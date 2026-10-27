package com.yechijia.controller;

import com.yechijia.common.ApiResponse;
import com.yechijia.dto.EatOutRecommendRequest;
import com.yechijia.dto.FoodCategoryRequest;
import com.yechijia.model.FoodCategory;
import com.yechijia.model.Restaurant;
import com.yechijia.service.EatOutService;
import com.yechijia.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 「出去吃」接口。
 * 提供自建食用类管理与门店推荐（推荐 → 排除 → 随机 → 确定）。
 */
@RestController
@RequestMapping("/api/eat-out")
public class EatOutController {

    /** 出去吃业务服务 */
    private final EatOutService eatOutService;

    /** 用户服务（自建食用类 CRUD） */
    private final UserService userService;

    /**
     * 构造控制器：注入业务服务。
     *
     * @param eatOutService 出去吃服务
     * @param userService   用户服务
     */
    public EatOutController(EatOutService eatOutService, UserService userService) {
        this.eatOutService = eatOutService;
        this.userService = userService;
    }

    /**
     * 查询用户的全部自建食用类（含权重）。
     *
     * @return 食用类列表
     */
    @GetMapping("/categories")
    public ApiResponse<List<FoodCategory>> listCategories() {
        return ApiResponse.ok(userService.listCategories());
    }

    /**
     * 查询附近门店实际支持的类型。
     *
     * @return 去重后的门店类型名称
     */
    @GetMapping("/types")
    public ApiResponse<List<String>> listNearbyTypes() {
        return ApiResponse.ok(eatOutService.listNearbyTypes());
    }

    /**
     * 新建自建食用类。
     *
     * @param request 类目名称与权重
     * @return 新建的食用类
     */
    @PostMapping("/categories")
    public ApiResponse<FoodCategory> createCategory(@RequestBody FoodCategoryRequest request) {
        return ApiResponse.ok(userService.createCategory(request.name(), request.weight()));
    }

    /**
     * 更新食用类（改名或调整权重）。
     *
     * @param id      类目标识
     * @param request 新的名称与权重
     * @return 更新后的食用类
     */
    @PutMapping("/categories/{id}")
    public ApiResponse<FoodCategory> updateCategory(@PathVariable String id,
                                                     @RequestBody FoodCategoryRequest request) {
        return ApiResponse.ok(userService.updateCategory(id, request.name(), request.weight()));
    }

    /**
     * 删除自建食用类（内置类目不可删）。
     *
     * @param id 类目标识
     * @return 空数据成功响应
     */
    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable String id) {
        userService.deleteCategory(id);
        return ApiResponse.ok(null);
    }

    /**
     * 推荐一家门店。
     * nearby 模式按心动类型优先、讨厌类型排除，并排除本轮已看门店；
     * 前端使用一个按钮完成首次推荐和后续换一家。
     *
     * @param request 推荐请求（模式 / 坐标 / 排除项）
     * @return 推荐的门店
     */
    @PostMapping("/recommend")
    public ApiResponse<Restaurant> recommend(@RequestBody EatOutRecommendRequest request) {
        Restaurant restaurant = eatOutService.recommend(
                request.mode(), request.latitude(), request.longitude(),
                request.excludedCategoryIds(), request.preferredCategoryIds(),
                request.excludedCategoryNames(), request.preferredCategoryNames(),
                request.excludedRestaurantIds());
        return ApiResponse.ok(restaurant);
    }
}
