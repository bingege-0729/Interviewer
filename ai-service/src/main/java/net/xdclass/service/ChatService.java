package net.xdclass.service;

import net.xdclass.controller.req.ChatReq;
import net.xdclass.dto.AiChatRecordDTO;
import net.xdclass.dto.ChatSessionDTO;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {

    Flux<ServerSentEvent<String>> chat(ChatReq chatReq);

    List<AiChatRecordDTO> getHistoryBySessionId(Long sessionId);

    List<ChatSessionDTO> listSessions();

    Long createSession();

}
