package net.abing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.abing.controller.req.ChatReq;
import net.abing.dto.AccountDTO;
import net.abing.dto.AiChatRecordDTO;
import net.abing.dto.ChatSessionDTO;
import net.abing.enums.ChatIntentEnum;
import net.abing.interceptor.AccountLoginInterceptor;
import net.abing.mapper.AiChatRecordMapper;
import net.abing.model.AiChatRecordDO;
import net.abing.service.ChatService;
import net.abing.strategy.IntentToolRouter;
import net.abing.util.JsonUtil;
import net.abing.util.SpringBeanUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatModel chatModel;          // 意图识别用
    private final ChatClient chatClient;        // 工具调用+流式用
    private final AiChatRecordMapper aiChatRecordMapper;
    private final IntentToolRouter intentToolRouter;

    private static final String INTENT_RECOGNITION_PROMPT = """
    你是一个意图识别系统，根据用户最新消息和对话上下文判断其最终意图类型。
        
    意图分类规则：
    - RESUME_ANALYSE: 用户想要分析简历、查看简历、上传简历等
    - INTERVIEW: 用户想要创建面试、开始面试、模拟面试等
    - BENEFIT_QUERY: 用户想要查询权益、购买套餐、充值、查看订单等
    - GENERAL_CHAT: 以上都不符合的通用对话、闲聊、问候等
        
    判断规则（按优先级）：
    1. 如果用户最新消息明确表达了新需求，直接按新需求分类
    2. 如果用户在回应上一轮AI的问题（补充信息、确认操作），保持当前意图
    3. 如果无法确定，归为 GENERAL_CHAT
        
    请只返回意图编码，不要返回其他内容。
    """;
    private ChatReq chatReq;

    @Override
    public Flux<ServerSentEvent<String>> chat(ChatReq chatReq) {
        this.chatReq = chatReq;
        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();
        Long accountId = accountDTO.getId();
        Long sessionId = chatReq.getSessionId();

        if(sessionId == null) {
            sessionId = IdWorker.getId();
            log.info("创建会话ID：{},accountId:{}", sessionId,accountId);
        } else {
            sessionId = chatReq.getSessionId();
        }

        //1、加载历史 + 意图识别
        List<AiChatRecordDO> historyList = SpringBeanUtil.copyProperties(getHistoryBySessionId(sessionId), AiChatRecordDO.class);
        String activeIntent = getActiveIntent(historyList);
        String intentType = recognizeIntent(chatReq.getMessage(),activeIntent,historyList);
        log.info("意图识别结果：{}", intentType);

        //2、保存用户消息
        AiChatRecordDO userRecord = saveMessage(accountId, sessionId, chatReq.getMessage(), "user", "TEXT", intentType);
        historyList.add(userRecord);

        //3、根据意图获取tool工具 和 系统提示词
        Object toolBean = intentToolRouter.getToolBean(intentType);
        String systemPrompt = intentToolRouter.getSystemPrompt(intentType);


        //4、构建历史消息列表
        List<Message> messages = buildHistoryMessages(historyList);


        //5、调用AI和工具
        StringBuilder fullResponse = new StringBuilder();

        final Long finalSessionId = sessionId;

        var promptSpec = chatClient.prompt().system(systemPrompt)
                .messages(messages)
                .user(chatReq.getMessage());

        //有工具注册的时候，恢复上下文
        if(toolBean !=null){
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            promptSpec.tools(toolBean)
                    .toolContext(Map.of("accountId",accountDTO.getId(),
                            "requestAttributes",requestAttributes,
                            "currentAccount",accountDTO));
        }

        return promptSpec.stream().content()
                // 过滤空内容
                .filter(content -> content != null && !content.isEmpty())
                // 累积内容 + 构建SSE
                .map(content -> {
                    fullResponse.append(content);
                    String jsonData = "{\"content\":" + JsonUtil.obj2Json(content) + ",\"contentType\":\"MARKDOWN\"}";
                    return ServerSentEvent.<String>builder().data(jsonData).build();
                })
                // 切换到弹性线程池：doOnComplete内有DB插入，reactor-nio线程禁止阻塞
                .publishOn(Schedulers.boundedElastic())
                // 流结束后保存AI回复
                .doOnComplete(() -> {
                    String aiResponse = fullResponse.toString();
                    if (!aiResponse.isEmpty()) {
                        saveMessage(accountId, finalSessionId, aiResponse, "assistant", "MARKDOWN", intentType);
                        log.info("AI回复保存成功, accountId:{}, responseLength:{}", accountId, aiResponse.length());
                    }
                })
                .doOnError(error -> log.error("流式对话异常, accountId:{}", accountId, error));
    }

    private List<Message> buildHistoryMessages(List<AiChatRecordDO> historyList) {
        if (historyList == null || historyList.isEmpty()) {
            return List.of();
        }
        // 取最近10条，避免Token超限
        int start = Math.max(0, historyList.size() - 10);
        List<Message> messages = new ArrayList<>();
        for (int i = start; i < historyList.size(); i++) {
            AiChatRecordDO record = historyList.get(i);
            if ("user".equals(record.getRole())) {
                messages.add(new UserMessage(record.getContent()));
            } else if ("assistant".equals(record.getRole())) {
                messages.add(new AssistantMessage(record.getContent()));
            }
        }
        return messages;
    }

    /**
     * 统一意图识别（上下文感知）
     * 根据用户消息 + 当前活跃意图 + 历史对话，综合判断用户意图
     * 识别失败时降级返回GENERAL_CHAT，保证系统不崩
     *
     * @param userMessage  用户当前发送的消息
     * @param activeIntent 当前会话的活跃意图（可能为null，表示首轮对话）
     * @param historyList  历史对话记录列表
     * @return 识别出的意图Code（如 BENEFIT_QUERY、INTERVIEW 等）
     */
    private String recognizeIntent(String userMessage, String activeIntent, List<AiChatRecordDO> historyList) {
        try {
            // 1. 构建带上下文的意图识别提示词
            String systemPrompt = buildIntentPrompt(activeIntent, historyList);
            // 2. 组装Prompt：系统提示词 + 用户消息，调用AI模型判断意图
            Prompt prompt = new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)));
            ChatResponse response = chatModel.call(prompt);
            String result = response.getResult().getOutput().getText();
            // 3. 解析AI返回的意图字符串，转为枚举Code
            if (result != null) {
                return ChatIntentEnum.of(result.trim().toUpperCase()).getCode();
            }
        } catch (Exception e) {
            log.error("意图识别失败, message:{}", userMessage, e);
        }
        // 降级策略：识别失败或异常时，默认返回闲聊意图，保证系统可用
        return ChatIntentEnum.GENERAL_CHAT.getCode();
    }

    /**
     * 构建意图识别提示词（根据是否有活跃意图附加上下文）
     * - 首轮对话：仅用基础提示词，让AI纯粹根据用户消息判断
     * - 多轮对话：附加“当前活跃意图” + “最近6条对话摘要”，防止意图跳变
     *
     * @param activeIntent 当前活跃意图（null表示无历史意图）
     * @param historyList  历史对话记录
     * @return 完整的意图识别SystemPrompt
     */
    private String buildIntentPrompt(String activeIntent, List<AiChatRecordDO> historyList) {
        // 无历史意图时，直接返回基础提示词（首轮对话场景）
        if (activeIntent == null || historyList == null || historyList.isEmpty()) {
            return INTENT_RECOGNITION_PROMPT;
        }
        // 获取当前意图的中文描述（如"权益查询与购买"）
        String intentDesc = ChatIntentEnum.of(activeIntent).getDesc();
        // 取最近6条历史记录，每条截断100字，控制Token消耗
        String context = historyList.stream()
                .skip(Math.max(0, historyList.size() - 6))
                .map(r -> ("user".equals(r.getRole()) ? "用户" : "AI") + ": " +
                        (r.getContent().length() > 100 ? r.getContent().substring(0, 100) + "..." : r.getContent()))
                .collect(Collectors.joining("\n"));
        // 拼接：基础提示词 + 意图粘性提示 + 对话上下文
        return INTENT_RECOGNITION_PROMPT + "\n当前对话正处于【" + intentDesc + "】意图的多轮交互中。\n\n最近对话上下文：\n" + context;
    }


    /**
     * 获取会话当前活跃意图
     * 从历史记录中找到最后一条assistant消息的intentType，作为当前活跃意图
     * 用于告诉AI"当前正在哪个意图的多轮交互中"，防止意图跳变
     *
     * @param historyList 历史对话记录
     * @return 活跃意图Code，无历史时返回null
     */
    private String getActiveIntent(List<AiChatRecordDO> historyList) {
        if (historyList == null || historyList.isEmpty()) {
            return null;
        }
        // 取最后一条有意图标记的assistant消息（reduce取最后一个匹配项）
        return historyList.stream()
                .filter(r -> "assistant".equals(r.getRole()) && r.getIntentType() != null)
                .reduce((first, second) -> second)
                .map(AiChatRecordDO::getIntentType)
                .orElse(null);
    }

    @Override
    public List<AiChatRecordDTO> getHistoryBySessionId(Long sessionId) {

        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();

        log.info("查询用户：{}的会话ID：{}的聊天记录",accountDTO.getId(),sessionId);
        LambdaQueryWrapper<AiChatRecordDO> queryWrapper = new LambdaQueryWrapper<AiChatRecordDO>()
                .eq(AiChatRecordDO::getSessionId, sessionId)
                .eq(AiChatRecordDO::getAccountId, accountDTO.getId())
                .orderByAsc(AiChatRecordDO::getSequence);
        List<AiChatRecordDO> aiChatRecordDOS = aiChatRecordMapper.selectList(queryWrapper);
        return SpringBeanUtil.copyProperties(aiChatRecordDOS, AiChatRecordDTO.class);
    }

    @Override
    public List<ChatSessionDTO> listSessions() {
        Long accountId = AccountLoginInterceptor.threadLocal.get().getId();
        List<AiChatRecordDO> allRecords = aiChatRecordMapper.selectList(
                new LambdaQueryWrapper<AiChatRecordDO>()
                        .eq(AiChatRecordDO::getAccountId, accountId)
                        .orderByAsc(AiChatRecordDO::getGmtCreate));

        if (allRecords.isEmpty()) return List.of();

        return allRecords.stream()
                .collect(Collectors.groupingBy(AiChatRecordDO::getSessionId))  // 按session分组
                .entrySet().stream()
                .map(entry -> {
                    List<AiChatRecordDO> records = entry.getValue();
                    // 标题：第一条用户消息前30字
                    String title = records.stream()
                            .filter(r -> "user".equals(r.getRole()))
                            .findFirst()
                            .map(r -> r.getContent().length() > 30
                                    ? r.getContent().substring(0, 30) + "..." : r.getContent())
                            .orElse("新对话");
                    // 最后一条消息摘要
                    AiChatRecordDO lastRecord = records.get(records.size() - 1);
                    String lastMessage = lastRecord.getContent();
                    if (lastMessage != null && lastMessage.length() > 50) {
                        lastMessage = lastMessage.substring(0, 50) + "...";
                    }
                    return ChatSessionDTO.builder()
                            .sessionId(entry.getKey())
                            .title(title)
                            .lastMessage(lastMessage)
                            .lastActiveTime(lastRecord.getGmtCreate())
                            .messageCount(records.size())
                            .build();
                })
                .sorted((a, b) -> b.getLastActiveTime().compareTo(a.getLastActiveTime()))  // 最近活跃排前面
                .collect(Collectors.toList());
    }

    @Override
    public Long createSession() {
        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();
        long id = IdWorker.getId();// 生成会话ID
        log.info("创建会话ID：{}, accountId:{}", id,accountDTO.getId());
        return id;
    }


    private AiChatRecordDO saveMessage(Long accountId, Long sessionId,
                                       String content, String role, String contentType, String intentType) {
        try {
            AiChatRecordDO record = AiChatRecordDO.builder()
                    .accountId(accountId)
                    .sessionId(sessionId)
                    .role(role)
                    .content(content)
                    .sequence(getNextSequence(accountId, sessionId))
                    .contentType(contentType)
                    .intentType(intentType)
                    .build();
            aiChatRecordMapper.insert(record);
            return record;
        } catch (Exception e) {
            log.error("保存{}消息失败, accountId:{}", role, accountId, e);
            return null;
        }
    }

    private int getNextSequence(Long accountId, Long sessionId) {
        if (sessionId == null) return 1;
        AiChatRecordDO lastRecord = aiChatRecordMapper.selectOne(
                new LambdaQueryWrapper<AiChatRecordDO>()
                        .eq(AiChatRecordDO::getAccountId, accountId)
                        .eq(AiChatRecordDO::getSessionId, sessionId)
                        .orderByDesc(AiChatRecordDO::getSequence)
                        .last("LIMIT 1"));
        return (lastRecord != null && lastRecord.getSequence() != null)
                ? lastRecord.getSequence() + 1 : 1;
    }

}
