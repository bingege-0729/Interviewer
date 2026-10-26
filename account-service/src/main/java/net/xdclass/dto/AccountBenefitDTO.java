package net.xdclass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBenefitDTO {

    private Long id;

    private Long accountId;

    private String benefitCode;

    private String benefitName;

    private String benefitDescription;

    private Long productOrderId;

    private Integer remainingCount;

    private Integer totalCount;

    private Date startTime;

    private Date endTime;

    private Date gmtCreate;

    private Date gmtModified;
}
