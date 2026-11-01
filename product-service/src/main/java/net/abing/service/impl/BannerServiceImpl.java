package net.abing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.abing.dto.BannerDTO;
import net.abing.enums.BizCodeEnum;
import net.abing.exception.BizException;
import net.abing.mapper.BannerMapper;
import net.abing.model.BannerDO;
import net.abing.service.BannerService;
import net.abing.util.SpringBeanUtil;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * <p>
 * Banner 服务实现类
 * </p>
 *
 * @since 2026-01-19
 */
@Service
@Slf4j
public class BannerServiceImpl implements BannerService {

    @Resource
    private BannerMapper bannerMapper;

    @Override
    public List<BannerDTO> list() {
        log.info("获取在线轮播图列表");
        // 查询在线的轮播图
        List<BannerDO> bannerDOList = bannerMapper.selectList(new LambdaQueryWrapper<BannerDO>()
                .eq(BannerDO::getStatus, "ON")
        );

        return SpringBeanUtil.copyProperties(bannerDOList, BannerDTO.class);
    }

    @Override
    public BannerDTO detail(Long id) {
        log.info("获取轮播图详情, id:{}", id);
        BannerDO bannerDO = bannerMapper.selectById(id);
        if (bannerDO == null) {
            throw new BizException(BizCodeEnum.BANNER_NOT_EXIST);
        }
        return SpringBeanUtil.copyProperties(bannerDO, BannerDTO.class);
    }
}
