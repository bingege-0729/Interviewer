package net.xdclass.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.xdclass.service.BannerService;
import net.xdclass.util.JsonData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * <p>
 * Banner 前端控制器
 * </p>
 *
 * @author 小滴课堂-二当家小D
 * @since 2026-01-19
 */
@Tag(name = "轮播图模块", description = "轮播图相关接口")
@RestController
@RequestMapping("/api/v1/banner")
public class BannerController {

    @Resource
    private BannerService bannerService;

    @Operation(summary = "获取轮播图列表")
    @GetMapping("/list")
    public JsonData list() {
        return JsonData.buildSuccess(bannerService.list());
    }

    @Operation(summary = "获取轮播图详情")
    @GetMapping("/detail")
    public JsonData detail(@RequestParam("id") Long id) {
        return JsonData.buildSuccess(bannerService.detail(id));
    }
}
