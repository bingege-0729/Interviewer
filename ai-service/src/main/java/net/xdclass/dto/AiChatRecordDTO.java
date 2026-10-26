package net.xdclass.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * AI对话记录（会话+消息合并表）
 * </p>
 *
 * @author 小滴课堂-二当家小D,
 * @since 2026-01-14
 */
@Getter
@Setter
@Schema(name = "AiChatRecordDO", description = "AI对话记录（会话+消息合并表）")
public class AiChatRecordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "用户ID（租户ID）")
    private Long accountId;

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "角色（user/assistant/system）")
    private String role;

    @Schema(description = "会话标题或消息内容")
    private String content;

    @Schema(description = "消息序号（同会话内递增）")
    private Integer sequence;

    @Schema(description = "创建时间")
    private Date gmtCreate;

    @Schema(description = "内容类型（TEXT-纯文本，MARKDOWN-Markdown格式，IMAGE-图片）")
    private String contentType;

    @Schema(description = "意图类型（RESUME_ANALYSE/INTERVIEW/BENEFIT_QUERY/GENERAL_CHAT）")
    private String intentType;
}
