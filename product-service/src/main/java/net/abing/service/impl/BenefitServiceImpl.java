package net.abing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.abing.dto.BenefitDTO;
import net.abing.enums.StatusEnum;
import net.abing.mapper.BenefitMapper;
import net.abing.model.BenefitDO;
import net.abing.service.BenefitService;
import net.abing.util.SpringBeanUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BenefitServiceImpl implements BenefitService {

    @Autowired
    private BenefitMapper benefitMapper;

    @Override
    public List<BenefitDTO> listBenefits(String status) {
        log.info("查询可用权益列表");

        LambdaQueryWrapper<BenefitDO> queryWrapper = new LambdaQueryWrapper<BenefitDO>()
                .eq(status!=null,BenefitDO::getStatus,status)
                .orderByAsc(BenefitDO::getSort);

        List<BenefitDO> benefitDOList = benefitMapper.selectList(queryWrapper);

        return SpringBeanUtil.copyProperties(benefitDOList, BenefitDTO.class);
    }

}
