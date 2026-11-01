package net.abing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeAnalyseMessageDTO {


    private Long resumeId;

    private Long accountId;

    private String messageId;

}
