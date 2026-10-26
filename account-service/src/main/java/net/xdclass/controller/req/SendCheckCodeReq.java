package net.xdclass.controller.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送验证码请求参数
 */
@Data
@Schema(description = "发送验证码请求参数")
public class SendCheckCodeReq {

    @Schema(description = "手机号或邮箱", example = "13800138000")
    @NotBlank(message = "接收终端不能为空")
    private String identifier;

    @Schema(description = "图形验证码", example = "8264")
    @NotBlank(message = "图形验证码不能为空")
    private String captcha;

    @Schema(description = "验证码类型:LOGIN-登录", example = "LOGIN")
    @NotBlank(message = "验证码类型不能为空")
    private String type;
}