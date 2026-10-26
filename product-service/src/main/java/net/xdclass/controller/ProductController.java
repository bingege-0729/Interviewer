package net.xdclass.controller;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.dto.BenefitDTO;
import net.xdclass.dto.ProductPackageDTO;
import net.xdclass.enums.StatusEnum;
import net.xdclass.model.ProductPackageDO;
import net.xdclass.service.BenefitService;
import net.xdclass.service.ProductPackageService;
import net.xdclass.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product")
@Slf4j
public class ProductController {


    @Autowired
    private BenefitService benefitService;

    @Autowired
    private ProductPackageService productPackageService;

    /**
     * 查询可用权益列表
     * @return 权益列表
     */
    @GetMapping("/benefits")
    public JsonData listBenefits() {
        log.info("查询权益列表请求");
        List<BenefitDTO> benefitList = benefitService.listBenefits(StatusEnum.ON.name());
        return JsonData.buildSuccess(benefitList);
    }


    /**
     * 套餐列表
     */
    @GetMapping("/packages")
    public JsonData listPackages() {
        log.info("查询套餐列表请求");
        List<ProductPackageDTO> productPackageDTOS = productPackageService.listPackages(StatusEnum.ON.name());
        return JsonData.buildSuccess(productPackageDTOS);
    }



}
