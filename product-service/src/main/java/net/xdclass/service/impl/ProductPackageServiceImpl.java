package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dto.BenefitDTO;
import net.xdclass.dto.ProductPackageDTO;
import net.xdclass.mapper.BenefitMapper;
import net.xdclass.mapper.PackageBenefitMapper;
import net.xdclass.mapper.ProductPackageMapper;
import net.xdclass.model.BenefitDO;
import net.xdclass.model.PackageBenefitDO;
import net.xdclass.model.ProductPackageDO;
import net.xdclass.service.ProductPackageService;
import net.xdclass.util.SpringBeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductPackageServiceImpl implements ProductPackageService {

    @Autowired
    private BenefitMapper benefitMapper;

    @Autowired
    private ProductPackageMapper productPackageMapper;

    @Autowired
    private PackageBenefitMapper packageBenefitMapper;


    @Override
    public List<ProductPackageDTO> listPackages(String status) {

        LambdaQueryWrapper<ProductPackageDO> queryWrapper = new LambdaQueryWrapper<ProductPackageDO>();
        if(status != null){
            queryWrapper.eq(ProductPackageDO::getStatus,status);
        }
        queryWrapper.orderByAsc(ProductPackageDO::getSort);
        List<ProductPackageDO> productPackageDOS = productPackageMapper.selectList(queryWrapper);

        //将套餐填充权益信息
        return productPackageDOS.stream().map(productPackageDO -> {
            ProductPackageDTO productPackageDTO = SpringBeanUtil.copyProperties(productPackageDO, ProductPackageDTO.class);

            List<BenefitDTO> benefitDTOList = getPackageBenefits(productPackageDO.getId());
            productPackageDTO.setBenefits(benefitDTOList);
            productPackageDTO.setTotalBenefitCount(benefitDTOList.size());
            //获取权益
            return productPackageDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<BenefitDTO> getPackageBenefits(Long packageId) {
        LambdaQueryWrapper<PackageBenefitDO> queryWrapper = new LambdaQueryWrapper<PackageBenefitDO>();
        queryWrapper.eq(PackageBenefitDO::getProductPackageId,packageId);
        List<PackageBenefitDO> packageBenefitDOS = packageBenefitMapper.selectList(queryWrapper);
        return packageBenefitDOS.stream().map(packageBenefitDO -> {

            BenefitDO benefitDO = benefitMapper.selectOne(new LambdaQueryWrapper<BenefitDO>()
                    .eq(BenefitDO::getBenefitCode, packageBenefitDO.getBenefitCode()));

            return SpringBeanUtil.copyProperties(benefitDO, BenefitDTO.class);
        }).collect(Collectors.toList());
    }
}
