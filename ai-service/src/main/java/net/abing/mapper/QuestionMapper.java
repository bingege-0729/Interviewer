package net.abing.mapper;

import net.abing.dto.QuestionDTO;
import net.abing.model.QuestionDO;
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
public interface QuestionMapper extends BaseMapper<QuestionDO> {

    void insertBatch(@Param("list") List<QuestionDO> questionDOList);

}
