package net.xdclass.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 套餐表
 * </p>
 *
 * @author 小滴课堂-二当家小D,
 * @since 2026-01-14
 */
@Getter
@Setter
@Schema(name = "ProductPackageDO", description = "套餐表")
public class ProductPackageDTO implements Serializable {


    @Schema(description = "套餐ID")
    private Long id;

    @Schema(description = "套餐名称")
    private String name;

    @Schema(description = "套餐描述")
    private String description;

    @Schema(description = "套餐价格")
    private BigDecimal price;

    @Schema(description = "有效期(天)")
    private Integer validDays;

    @Schema(description = "是否推荐")
    private Boolean isRecommended;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建时间")
    private Date gmtCreate;

    @Schema(description = "更新时间")
    private Date gmtModified;


    /**
     * 套餐下的权益
     */
    private List<BenefitDTO> benefits;


    /**
     * 套餐总权益数量
     */
    private Integer totalBenefitCount;


}
