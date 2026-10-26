package net.xdclass.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.controller.req.ChatReq;
import net.xdclass.dto.AiChatRecordDTO;
import net.xdclass.dto.ChatSessionDTO;
import net.xdclass.model.AiChatRecordDO;
import net.xdclass.service.ChatService;
import net.xdclass.util.JsonData;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {


    private final ChatService chatService;


    /**
     * SSE 接口
     */

    @PostMapping(value = "/stream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestBody  ChatReq chatReq){
        log.info("chatReq:{}", chatReq);
        return chatService.chat(chatReq);
    }



    /**
     * 查询对话历史记录
     */
    @GetMapping("/history")
    @Operation(summary = "查询对话历史")
    public JsonData history(@RequestParam("sessionId") Long sessionId) {
        List<AiChatRecordDTO> historyList = chatService.getHistoryBySessionId(sessionId);
        return JsonData.buildSuccess(historyList);
    }

    /**
     * 获取当前用户的对话会话列表
     */
    @GetMapping("/sessions")
    @Operation(summary = "会话列表")
    public JsonData sessions() {
        List<ChatSessionDTO> sessionList = chatService.listSessions();
        return JsonData.buildSuccess(sessionList);
    }

    /**
     * 创建新会话
     */
    @PostMapping("/session/create")
    @Operation(summary = "创建新会话")
    public JsonData createSession() {
        Long sessionId = chatService.createSession();
        return JsonData.buildSuccess(sessionId);
    }


}
