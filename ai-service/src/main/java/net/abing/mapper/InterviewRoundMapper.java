package net.abing.mapper;

import net.abing.model.InterviewRoundDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @since 2026-01-14
 */
public interface InterviewRoundMapper extends BaseMapper<InterviewRoundDO> {

    void insertBatch(@Param("list") List<InterviewRoundDO> interviewRoundDOS);
}
