package net.abing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 对话会话摘要DTO
 * 用于展示会话列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDTO {

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 会话标题（取第一条用户消息的前30字）
     */
    private String title;

    /**
     * 最后一条消息内容摘要
     */
    private String lastMessage;

    /**
     * 最后活跃时间
     */
    private Date lastActiveTime;

    /**
     * 消息数量
     */
    private Integer messageCount;
}
