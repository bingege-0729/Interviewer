package net.abing.service;

import net.abing.dto.BenefitDTO;

import java.util.List;

public interface BenefitService {

    /**
     * 查询所有可用的权益列表
     * @return 权益列表
     */
    List<BenefitDTO> listBenefits(String status);
}
