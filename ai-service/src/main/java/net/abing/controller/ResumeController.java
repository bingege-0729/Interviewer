package net.abing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import net.abing.dto.AccountDTO;
import net.abing.dto.ResumeDTO;
import net.abing.enums.BenefitEnum;
import net.abing.interceptor.AccountLoginInterceptor;
import net.abing.req.BenefitCheckReq;
import net.abing.service.ResumeService;
import net.abing.util.JsonData;
import org.apache.ibatis.annotations.Delete;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resume")
@RequiredArgsConstructor
public class ResumeController {


    private final ResumeService resumeService;


    /**
     * 简历详情查询
     */
    @GetMapping("detail")
    @Operation(summary = "简历详情")
    public JsonData detail(@RequestParam("id") Long id) {
        ResumeDTO resumeDTO = resumeService.detail(id);
        return JsonData.buildSuccess(resumeDTO);
    }

    /**
     * 简历列表查询
     */
    @GetMapping("list")
    @Operation(summary = "简历列表")
    public JsonData list() {
        List<ResumeDTO> list = resumeService.list();
        return JsonData.buildSuccess(list);
    }

    /**
     * 简历上传
     */
    @PostMapping(value = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传简历")
    public JsonData upload(
            @Parameter(name = "file")
            @RequestPart("file") MultipartFile file) {
        ResumeDTO resumeDTO = resumeService.uploadAndParseResume(file);
        return JsonData.buildSuccess(resumeDTO);
    }

    /**
     * 删除简历
     */
    @DeleteMapping("delete")
    @Operation(summary = "删除简历")
    public JsonData delete(@RequestParam("id") Long id) {
        boolean result = resumeService.delete(id);
        return JsonData.buildSuccess(result);
    }


    /**
     * 预览简历
     */
    @GetMapping("preview")
    @Operation(summary = "预览简历")
    public JsonData preview(@RequestParam("id") Long id) {
        return resumeService.getResumePreviewUrl(id);
    }


    /**
     * 简历分析
     */
    @GetMapping("analyse")
    @Operation(summary = "简历分析")
    public JsonData analyse(@RequestParam("id") Long id) {

        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();

        BenefitCheckReq checkReq = BenefitCheckReq.builder()
                .benefitCode(BenefitEnum.RESUME_ANALYSE.name())
                .businessId(id.toString())
                .accountId(accountDTO.getId())
                .count(1)
                .build();

        return resumeService.analyse(checkReq);

    }



}
