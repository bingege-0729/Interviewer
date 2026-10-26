package net.xdclass.mapper;

import net.xdclass.dto.QuestionDTO;
import net.xdclass.model.QuestionDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 小滴课堂-二当家小D,
 * @since 2026-01-14
 */
public interface QuestionMapper extends BaseMapper<QuestionDO> {

    void insertBatch(@Param("list") List<QuestionDO> questionDOList);

}
