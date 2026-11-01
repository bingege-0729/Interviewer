package net.abing.service;

import net.abing.dto.BenefitDTO;
import net.abing.dto.ProductPackageDTO;

import java.util.List;

public interface ProductPackageService {
    List<ProductPackageDTO> listPackages(String name);


    List<BenefitDTO> getPackageBenefits(Long packageId);

}
