package net.abing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <p>
 * Banner 传输对象
 * </p>
 *
 * @since 2026-01-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerDTO implements Serializable {

    private Long id;

    @Schema(description = "格式页面-位置-模块")
    private String location;

    @Schema(description = "中文名称")
    private String name;

    @Schema(description = "图片列表，逗号分隔")
    private String imgUrl;

    @Schema(description = "PC端跳转链接，逗号分隔")
    private String pcLink;

    @Schema(description = "文本")
    private String text;

    @Schema(description = "状态 ON在线，OFF下线")
    private String status;
}
