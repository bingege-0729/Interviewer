package net.abing.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
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
 * 
 * </p>
 *
 * @since 2026-01-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("banner")
@Schema(name = "BannerDO", description = "")
public class BannerDO implements Serializable {

    private static final long serialVersionUID = 1L;

      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "格式页面-位置-模块")
    @TableField("location")
    private String location;

    @Schema(description = "中文名称")
    @TableField("name")
    private String name;

    @Schema(description = "图片列表，逗号分隔")
    @TableField("img_url")
    private String imgUrl;

    @Schema(description = "PC端跳转链接，逗号分隔")
    @TableField("pc_link")
    private String pcLink;

    @Schema(description = "文本")
    @TableField("text")
    private String text;

    @Schema(description = "状态 ON在线，OFF下线")
    @TableField("status")
    private String status;

    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private Date gmtCreate;

    @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private Date gmtModified;
}
