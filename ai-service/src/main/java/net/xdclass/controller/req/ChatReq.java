package net.xdclass.controller.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 智能对话请求
 */
@Data
@Schema(name = "ChatReq", description = "智能对话请求")
public class ChatReq {

    /**
     * 用户消息内容
     */
    @Schema(description = "用户消息内容", required = true)
    private String message;

    /**
     * 会话ID（可选，用于多轮对话上下文，不传则自动生成）
     */
    @Schema(description = "会话ID（可选，不传则自动生成）")
    private Long sessionId;
}