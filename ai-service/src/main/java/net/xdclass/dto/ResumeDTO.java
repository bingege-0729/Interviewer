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
 * 
 * </p>
 *
 * @author 小滴课堂-二当家小D,
 * @since 2026-01-14
 */
@Getter
@Setter
public class ResumeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long accountId;

    private String filename;

    private String filePath;

    private String fileType;

    private String content;

    private String skillTags;

    @Schema(description = "AI简历分析")
    private String evaluation;

    @Schema(description = "解析状态")
    private String status;

    private Date gmtCreate;

    private Date gmtModified;
}
