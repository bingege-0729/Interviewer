package net.abing.service;

import net.abing.dto.BannerDTO;

import java.util.List;

/**
 * <p>
 * Banner 服务类
 * </p>
 *
 * @since 2026-01-19
 */
public interface BannerService {

    /**
     * 获取在线轮播图列表
     * @return
     */
    List<BannerDTO> list();

    /**
     * 获取轮播图详情
     * @param id
     * @return
     */
    BannerDTO detail(Long id);
}
