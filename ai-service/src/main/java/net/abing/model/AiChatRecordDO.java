package net.abing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * <p>
 * AI对话记录（会话+消息合并表）
 * </p>
 *
 * @since 2026-01-14
 */
@Getter
@Setter
@TableName("ai_chat_record")
@Schema(name = "AiChatRecordDO", description = "AI对话记录（会话+消息合并表）")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRecordDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID（租户ID）")
    @TableField("account_id")
    private Long accountId;

    @Schema(description = "会话ID")
    @TableField("session_id")
    private Long sessionId;

    @Schema(description = "角色（user/assistant/system）")
    @TableField("role")
    private String role;

    @Schema(description = "会话标题或消息内容")
    @TableField("content")
    private String content;

    @Schema(description = "消息序号（同会话内递增）")
    @TableField("sequence")
    private Integer sequence;

    @Schema(description = "创建时间")
    @TableField("gmt_create")
    private Date gmtCreate;

    @Schema(description = "内容类型（TEXT-纯文本，MARKDOWN-Markdown格式，IMAGE-图片）")
    @TableField("content_type")
    private String contentType;

    @Schema(description = "意图类型（RESUME_ANALYSE/INTERVIEW/BENEFIT_QUERY/GENERAL_CHAT）")
    @TableField("intent_type")
    private String intentType;
}
