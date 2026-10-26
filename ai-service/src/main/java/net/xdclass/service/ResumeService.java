package net.xdclass.service;

import net.xdclass.dto.ResumeDTO;
import net.xdclass.req.BenefitCheckReq;
import net.xdclass.util.JsonData;
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
