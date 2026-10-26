package net.xdclass.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.dto.AccountDTO;
import net.xdclass.dto.BenefitCheckResultDTO;
import net.xdclass.dto.ResumeDTO;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.ResumeStatus;
import net.xdclass.feign.AccountBenefitFeign;
import net.xdclass.interceptor.AccountLoginInterceptor;
import net.xdclass.mapper.ResumeMapper;
import net.xdclass.model.ResumeDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import net.xdclass.req.BenefitCheckReq;
import net.xdclass.service.OssService;
import net.xdclass.service.ResumeService;
import net.xdclass.strategy.BenefitMessageStrategy;
import net.xdclass.strategy.BenefitMessageStrategyFactory;
import net.xdclass.util.FileUtil;
import net.xdclass.util.JsonData;
import net.xdclass.util.SpringBeanUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.fdf.FDFDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ResumeServiceImpl implements ResumeService {

    @Autowired
    private ResumeMapper resumeMapper;

    @Autowired
    private OssService ossService;


    @Autowired
    private AccountBenefitFeign accountBenefitFeign;


    @Autowired
    private BenefitMessageStrategyFactory strategyFactory;

    @Override
    public ResumeDTO detail(Long id) {
        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();
        Long accountId = accountDTO.getId();

        LambdaQueryWrapper<ResumeDO> queryWrapper = new LambdaQueryWrapper<ResumeDO>()
                .eq(ResumeDO::getId, id)
                .eq(ResumeDO::getAccountId, accountId);

        ResumeDO resumeDO = resumeMapper.selectOne(queryWrapper);
        if (resumeDO == null) {
            return null;
        }

        return SpringBeanUtil.copyProperties(resumeDO, ResumeDTO.class);
    }

    @Override
    public boolean delete(Long id) {
        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();
        Long accountId = accountDTO.getId();

        LambdaQueryWrapper<ResumeDO> queryWrapper = new LambdaQueryWrapper<ResumeDO>()
                .eq(ResumeDO::getId, id)
                .eq(ResumeDO::getAccountId, accountId);

        ResumeDO resumeDO = resumeMapper.selectOne(queryWrapper);

        // 2. 删除OSS文件
        ossService.deleteFile(resumeDO.getFilePath());

        int rows = resumeMapper.delete(queryWrapper);
        return rows > 0;
    }

    @Override
    public JsonData getResumePreviewUrl(Long id) {
        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();
        //查询简历
        ResumeDO resumeDO = resumeMapper.selectOne(new LambdaQueryWrapper<ResumeDO>()
                .eq(ResumeDO::getId, id)
                .eq(ResumeDO::getAccountId, accountDTO.getId()));
        if (resumeDO == null) {
            return JsonData.buildError("简历不存在");
        }
        String generatePreviewUrl = ossService.generatePreviewUrl(resumeDO.getFilePath());
        //构建返回数据
        return JsonData.buildSuccess(generatePreviewUrl);
    }

    /**
     * 查找简历
     *
     * 更新简历状态
     *
     * 检查权益扣减是否成功
     *
     * 发送MQ 业务消息
     *
     * @param id
     * @return
     */
    @Override
    public JsonData analyse(BenefitCheckReq checkReq) {

        //查找简历
        ResumeDO resumeDO = resumeMapper.selectOne(new LambdaQueryWrapper<ResumeDO>()
                .eq(ResumeDO::getId, checkReq.getBusinessId())
                .eq(ResumeDO::getAccountId, checkReq.getAccountId()));

        if (resumeDO == null) {
            return JsonData.buildError("简历不存在");
        }
        //检查权益扣减是否成功
        JsonData jsonData = accountBenefitFeign.checkAndDeductBenefit(checkReq);
        if(jsonData.getCode() != 0){
            return JsonData.buildResult(BizCodeEnum.BENEFIT_NOT_ENOUGH);
        }
        //更新简历状态
        resumeDO.setStatus(ResumeStatus.IN_PROCESS.name());
        resumeMapper.updateById(resumeDO);

        //解析扣减结果
        BenefitCheckResultDTO benefitCheckResultDTO = jsonData.getData(BenefitCheckResultDTO.class);
        log.info("权益检查结果：{}", benefitCheckResultDTO);

        //发送MQ业务消息
        try {

            BenefitMessageStrategy strategy = strategyFactory.getStrategy(checkReq.getBenefitCode());
            strategy.sendBusinessMessage(benefitCheckResultDTO.getMessageId(), checkReq.getBusinessId(), checkReq.getAccountId());
            log.info("发送MQ业务消息成功：{}", benefitCheckResultDTO.getMessageId());

        }catch (Exception e){
            log.error("发送MQ业务消息失败：{}", e.getMessage());
            //不要抛异常吗，权益是扣减，延迟检查机制会检查
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message_id", benefitCheckResultDTO.getMessageId());
        result.put("resume_id", resumeDO.getId());
        result.put("message","简历分析处理中，请稍等");
        return JsonData.buildSuccess(result);
    }

    @Override
    public List<ResumeDTO> list() {
        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();
        Long accountId = accountDTO.getId();

        LambdaQueryWrapper<ResumeDO> queryWrapper = new LambdaQueryWrapper<ResumeDO>()
                .eq(ResumeDO::getAccountId, accountId)
                .orderByDesc(ResumeDO::getGmtCreate);

        List<ResumeDO> resumeDOList = resumeMapper.selectList(queryWrapper);

        return SpringBeanUtil.copyProperties(resumeDOList, ResumeDTO.class);
    }

    @Override
    public ResumeDTO uploadAndParseResume(MultipartFile file) {

        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();

        //验证文件
        FileUtil.validateFile( file);

        //获取文件类型
        String fileType = FileUtil.getFileType(file.getOriginalFilename());
        String content = null;

        //解析文件
        try {
            content = extractTextFromMultipartFile(file,fileType);
        }catch (Exception e){
            log.error("文件解析失败：{}",e.getMessage());
            throw new RuntimeException("文件解析失败");
        }

        //上传oss
        String uploadSecureFileUrl = ossService.uploadSecureFile(file, "resume");
        log.info("上传文件成功：{}",uploadSecureFileUrl);

        //保存数据库
        ResumeDO resumeDO = ResumeDO.builder()
                .accountId(accountDTO.getId())
                .filename(file.getOriginalFilename())
                .filePath(uploadSecureFileUrl)
                .fileType(fileType)
                .content(content)
                .status(ResumeStatus.UPLOADED.name())
                .build();

        resumeMapper.insert(resumeDO);
        log.info("保存数据库成功：{}",resumeDO);
        return SpringBeanUtil.copyProperties(resumeDO,ResumeDTO.class);
    }

    private String extractTextFromMultipartFile(MultipartFile file, String fileType) {
        log.info("开始解析文件：{}",file.getOriginalFilename());

        try(InputStream inputStream = file.getInputStream()) {

            //通过switch和yield语法实现多文件解析
            return switch (fileType.toLowerCase()) {
                case "pdf"-> {
                    try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
                        if (document.isEncrypted()) {
                            throw new RuntimeException("PDF文件已加密，无法提取文本");
                        }
                        PDFTextStripper stripper = new PDFTextStripper();
                        yield stripper.getText(document);
                    }
                }
                case "doc"-> {
                    try (HWPFDocument document = new HWPFDocument(inputStream);
                         WordExtractor extractor = new WordExtractor(document)) {
                        yield extractor.getText();
                    }
                }
                case "docx"-> {
                    try (XWPFDocument document = new XWPFDocument(inputStream);
                         XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                        yield extractor.getText();
                    }
                }
                default -> throw new RuntimeException("不支持的文件类型");
            };

        }catch (Exception e){
            log.error("文件解析失败：{}",e.getMessage());
            throw new RuntimeException("文件解析失败");
        }
    }
}
