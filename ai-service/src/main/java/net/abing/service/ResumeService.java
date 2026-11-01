package net.abing.service;

import net.abing.dto.ResumeDTO;
import net.abing.req.BenefitCheckReq;
import net.abing.util.JsonData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {
    ResumeDTO uploadAndParseResume(MultipartFile file);

    List<ResumeDTO> list();

    ResumeDTO detail(Long id);

    boolean delete(Long id);

    JsonData getResumePreviewUrl(Long id);

    JsonData analyse(BenefitCheckReq checkReq);
}
