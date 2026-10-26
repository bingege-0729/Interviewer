package net.xdclass.controller;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.util.JsonUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
public class AiTestController {

    private final ChatClient chatClient;

    private final ChatModel chatModel;


    @GetMapping("multi_agent1")
    public String multiAgent1(@RequestParam(name = "msg", defaultValue = "第一个关于春天的短文") String msg) throws Exception {

        // 第一个Agent：使用 {input} 获取用户输入
        ReactAgent writerAgent = ReactAgent.builder()
                .name("writer_agent")
                .model(chatModel)
                .instruction("你是一个知名的作家。请根据用户的提问进行回答：{input}。")
                .outputKey("article")
                .build();


        // 第二个Agent：使用 {article} 引用第一个Agent的输出
        ReactAgent reviewerAgent = ReactAgent.builder()
                .name("reviewer_agent")
                .model(chatModel)
                .instruction("请对文章进行评审修正：\n{article}，最终返回评审修正后的文章内容")
                .outputKey("reviewed_article")
                .build();


        SequentialAgent pipeline = SequentialAgent.builder()
                .name("sequential_agent")
                .description("从写作到评审修正的流水线")
                .subAgents(List.of(writerAgent, reviewerAgent))
                .build();

        log.info("开始执行流水线，用户输入：{}", msg);

        Optional<OverAllState> overAllState = pipeline.invoke(msg);


        if (overAllState.isEmpty()) {
            log.error("流水线执行失败");
            return "流水线执行失败";
        }

        OverAllState overAllStateResult = overAllState.get();

        //第一个agent的结果
        Optional<Object> article = overAllStateResult.value("article");

        String articleResult = article.map(String::valueOf).orElse("未获取到结果");
        log.info("流水线执行成功，结果：{}", articleResult);

        AtomicReference<String> reviewedArticleText = new AtomicReference<>();

        //第二个agent的结果
        overAllStateResult.value("reviewed_article").ifPresent(
                reviewedArticle -> {
                    if (reviewedArticle instanceof AssistantMessage) {
                        reviewedArticleText.set(((AssistantMessage) reviewedArticle).getText());
                    }
                });

        return reviewedArticleText.get();
    }


    @GetMapping("multi_agent2")
    public void multiAgent2() throws Exception {


        // 天气专业Agent
        ReactAgent weatherAgent = ReactAgent.builder()
                .name("WeatherAgent")
                .model(chatModel)
                .instruction("你专门负责查询和分析天气信息，请提供2020一整年的天气概况")
                .outputKey("weather_data")
                .build();

        // 新闻专业Agent
        ReactAgent newsAgent = ReactAgent.builder()
                .name("NewsAgent")
                .model(chatModel)
                .instruction("你专门负责收集和整理最新财经新闻，请提供2020一整年的财经要闻")
                .outputKey("news_data")
                .build();

        // 股票专业Agent
        ReactAgent stockAgent = ReactAgent.builder()
                .name("StockAgent")
                .model(chatModel)
                .instruction("你专门负责查询股票行情和分析走势，请提供2020一整年大盘概况")
                .outputKey("stock_data")
                .build();


        ParallelAgent parallelAgent = ParallelAgent.builder()
                .name("ParallelAgent")
                .description("市场监控系统，并行采集多源数据，并行执行多个Agent")
                .subAgents(List.of(weatherAgent, newsAgent, stockAgent))
                .build();


        Optional<OverAllState> result = parallelAgent.invoke("请提供2020年市场简报");


        if (result.isPresent()) {

            OverAllState overAllState = result.get();

            //分别获取3个agent输出
            overAllState.value("weather_data").ifPresent(weatherData -> {
                log.info("======WeatherData======: {}", weatherData);
            });

            overAllState.value("news_data").ifPresent(newsData -> {
                log.info("======NewsData======: {}", newsData);
            });

            overAllState.value("stock_data").ifPresent(stockData -> {
                log.info("======StockData======: {}", stockData);
            });

        }


    }


    @GetMapping(value = "/multi_agent3")
    public String multiAgent3(@RequestParam(name = "msg", defaultValue = "请提供2020年的市场简报") String msg) throws GraphRunnerException {

        // ========== 第一步：构建并行采集的三个子Agent ==========

        // 天气专业Agent
        ReactAgent weatherAgent = ReactAgent.builder()
                .name("WeatherAgent")
                .model(chatModel)
                .instruction("你专门负责查询和分析天气信息，请根据用户的问题提供当年重要天气概况：{input}")
                .outputKey("weather_data")
                .build();

        // 新闻专业Agent
        ReactAgent newsAgent = ReactAgent.builder()
                .name("NewsAgent")
                .model(chatModel)
                .instruction("你专门负责收集和整理最新财经新闻，请根据用户的问题提供当年重要财经要闻：{input}")
                .outputKey("news_data")
                .build();

        // 股票专业Agent
        ReactAgent stockAgent = ReactAgent.builder()
                .name("StockAgent")
                .model(chatModel)
                .instruction("你专门负责查询股票行情和分析走势，请根据用户的问题提供当年重要大盘概况：{input}")
                .outputKey("stock_data")
                .build();

        // ========== 第二步：组装ParallelAgent（并行采集） ==========
        ParallelAgent dataCollector = ParallelAgent.builder()
                .name("DataCollector")
                .description("并行采集天气、新闻、股票三类数据")
                .subAgents(List.of(weatherAgent, newsAgent, stockAgent))
                .build();


        //构建汇总的agent
        ReactAgent summaryAgent = ReactAgent.builder().name("SummaryAgent")
                .model(chatModel)
                .instruction("""
                        你是一位资深市场分析师，请根据以下多源数据生成一份综合市场简报。
                        
                                                【天气数据】
                                                {weather_data}
                        
                                                【财经新闻】
                                                {news_data}
                        
                                                【股票行情】
                                                {stock_data}
                        
                                                请综合以上信息，输出一份结构清晰的当年重要市场简报，包含：
                                                1. 天气对市场的潜在影响
                                                2. 重要财经新闻摘要
                                                3. 大盘走势分析
                                                4. 综合建议
                        """)
                .outputKey("market_report")
                .build();

        //构建流水线
        SequentialAgent pipeline = SequentialAgent.builder()
                .name("MarketReportPipeline")
                .description("从数据采集到市场简报生成的流水线")
                .subAgents(List.of(dataCollector, summaryAgent))
                .build();


        // ========== 第三步：执行流水线并获取结果 ==========
        Optional<OverAllState> result = pipeline.invoke(msg);
        if (result.isPresent()) {
            OverAllState overAllState = result.get();

            overAllState.value("weather_data").ifPresent(weatherData -> {
                log.info("======WeatherData=======: {}", weatherData);
            });
            overAllState.value("news_data").ifPresent(newsData -> {
                log.info("======NewsData=======: {}", newsData);
            });
            overAllState.value("stock_data").ifPresent(stockData -> {
                log.info("======StockData=======: {}", stockData);
            });

            overAllState.value("market_report").ifPresent(marketReport -> {
                log.info("======【MarketReport】======: {}", marketReport);
            });

            return overAllState.value("market_report").map(String::valueOf).orElse("未获取到结果");
        } else {
            return "流水线执行失败";
        }

    }



    @GetMapping(value = "/multi_agent4")
    public void multiAgent4(@RequestParam(name = "msg") String msg) throws GraphRunnerException {

        // 技术支持Agent
        ReactAgent techAgent = ReactAgent.builder()
                .name("TechSupportAgent")
                .model(chatModel)
                .description("技术支持专家，负责解决产品使用问题、Bug报告和技术咨询")
                .instruction("你是技术支持专家，专门解决产品使用问题、Bug报告和技术咨询")
                .build();

        // 订单服务Agent
        ReactAgent orderAgent = ReactAgent.builder()
                .name("OrderServiceAgent")
                .model(chatModel)
                .description("订单服务专家，负责查询订单状态、修改收货地址、处理发货问题")
                .instruction("你是订单服务专家，负责查询订单状态、修改收货地址、处理发货问题")
                .build();

        // 退款处理Agent
        ReactAgent refundAgent = ReactAgent.builder()
                .name("RefundAgent")
                .model(chatModel)
                .description("退款处理专家，负责审核退款申请、处理退货流程、解答退款政策")
                .instruction("你是退款处理专家，负责审核退款申请、处理退货流程、解答退款政策")
                .build();


        LlmRoutingAgent llmRoutingAgent = LlmRoutingAgent.builder()
                .name("LlmRoutingAgent")
                .model(chatModel)
                .instruction("""
                         根据用户的问题类型进行路由：
                                - 如果是技术问题、产品使用问题、Bug → 路由到 TechSupportAgent
                                - 如果是订单查询、物流、发货问题 → 路由到 OrderServiceAgent
                                - 如果是退款、退货申请 → 路由到 RefundAgent
                        """)
                .subAgents(List.of(techAgent, orderAgent, refundAgent))
                .build();

        Optional<OverAllState> overAllStateOptional = llmRoutingAgent.invoke(msg);


        overAllStateOptional.ifPresent(overAllState -> {

            List<Message> list = (List<Message>)overAllState.value("messages").orElse(List.of());

            list.stream()
                    .filter( m -> m instanceof  AssistantMessage)
                    .forEach(
                            m -> log.info("======AssistantMessage======: {}", ((AssistantMessage) m).getText())
                    );

        });
    }

    }
