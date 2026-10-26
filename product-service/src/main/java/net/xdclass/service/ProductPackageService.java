package net.xdclass.service;

import net.xdclass.dto.BenefitDTO;
import net.xdclass.dto.ProductPackageDTO;

import java.util.List;

public interface ProductPackageService {
    List<ProductPackageDTO> listPackages(String name);


    List<BenefitDTO> getPackageBenefits(Long packageId);

}
