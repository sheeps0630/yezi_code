package com.yechijia.controller;

import com.yechijia.common.ApiResponse;
import com.yechijia.dto.DrinkRecommendRequest;
import com.yechijia.model.DrinkProduct;
import com.yechijia.service.DrinkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 「喝什么」接口。
 * 提供饮品种类列表与饮品推荐（附近店铺 / 随机生成，含会话内去重）。
 */
@RestController
@RequestMapping("/api/drink")
public class DrinkController {

    /** 喝什么业务服务 */
    private final DrinkService drinkService;

    /**
     * 构造控制器：注入业务服务。
     *
     * @param drinkService 喝什么服务
     */
    public DrinkController(DrinkService drinkService) {
        this.drinkService = drinkService;
    }

    /**
     * 获取饮品种类列表（奶茶/咖啡/其他）。
     *
     * @return 种类标识列表
     */
    @GetMapping("/categories")
    public ApiResponse<List<String>> listCategories() {
        return ApiResponse.ok(drinkService.listCategories());
    }

    /**
     * 推荐一款饮品（产品名 + 商家）。
     * 前端第一次调用为「推荐」；用户点「随机」换一个时，
     * 把已推荐的饮品标识放入排除列表再次调用即可。
     *
     * @param request 推荐请求（是否附近 / 种类 / 排除项）
     * @return 推荐的饮品
     */
    @PostMapping("/recommend")
    public ApiResponse<DrinkProduct> recommend(@RequestBody DrinkRecommendRequest request) {
        DrinkProduct product = drinkService.recommend(
                request.useNearby(), request.category(),
                request.latitude(), request.longitude(), request.excludedProductIds());
        return ApiResponse.ok(product);
    }
}
