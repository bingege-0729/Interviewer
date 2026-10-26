package net.xdclass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WechatQrCodeDTO {

    private String sceneId;

    private String qrCodeUrl;

    private Integer expiresTime;

    /**
     * 二维码状态 WAITING SCANNED EXPIRED CONFIRMED
     */
    private String status;

    private Long createTime;
}
