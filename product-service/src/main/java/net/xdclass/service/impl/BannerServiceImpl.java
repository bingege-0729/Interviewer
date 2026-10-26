package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dto.BannerDTO;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.exception.BizException;
import net.xdclass.mapper.BannerMapper;
import net.xdclass.model.BannerDO;
import net.xdclass.service.BannerService;
import net.xdclass.util.SpringBeanUtil;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * <p>
 * Banner 服务实现类
 * </p>
 *
 * @author 小滴课堂-二当家小D
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
