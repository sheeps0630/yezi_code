package com.yechijia.controller;

import com.yechijia.common.ApiResponse;
import com.yechijia.dto.CookMenuRequest;
import com.yechijia.dto.MenuResponse;
import com.yechijia.model.Dish;
import com.yechijia.model.Recipe;
import com.yechijia.service.CookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 「在家吃」接口。
 * 提供种类选择、主菜候选列表、生成菜点与真实菜谱查询。
 */
@RestController
@RequestMapping("/api/cook")
public class CookController {

    /** 在家吃业务服务 */
    private final CookService cookService;

    /**
     * 构造控制器：注入业务服务。
     *
     * @param cookService 在家吃服务
     */
    public CookController(CookService cookService) {
        this.cookService = cookService;
    }

    /**
     * 获取在家吃饭的种类列表（素菜/肉菜/甜品）。
     *
     * @return 种类标识列表
     */
    @GetMapping("/categories")
    public ApiResponse<List<String>> listCategories() {
        return ApiResponse.ok(cookService.listCategories());
    }

    /**
     * 获取每种菜类对应的可选食材标签。
     *
     * @return 菜类到食材标签的映射
     */
    @GetMapping("/ingredient-types")
    public ApiResponse<Map<String, List<String>>> listIngredientTypes() {
        return ApiResponse.ok(cookService.listIngredientTypes());
    }

    /**
     * 获取某个种类下的全部菜品（供用户挑选主菜）。
     *
     * @param category 种类标识
     * @return 菜品列表
     */
    @GetMapping("/dishes")
    public ApiResponse<List<Dish>> listDishes(@RequestParam String category) {
        return ApiResponse.ok(cookService.listDishesByCategory(category));
    }

    /**
     * 根据所选种类（必选）与主菜（可选）生成一餐菜点。
     *
     * @param request 菜点生成请求
     * @return 本餐菜点组合
     */
    @PostMapping("/menu")
    public ApiResponse<MenuResponse> generateMenu(@RequestBody CookMenuRequest request) {
        return ApiResponse.ok(cookService.generateMenu(
                request.categories(), request.mainDishId(), request.ingredientFilters()));
    }

    /**
     * 查询一道菜的真实菜谱（禁止胡乱生成，全部来自静态数据）。
     *
     * @param dishId 菜品标识
     * @return 对应菜谱
     */
    @GetMapping("/recipes/{dishId}")
    public ApiResponse<Recipe> getRecipe(@PathVariable String dishId) {
        return ApiResponse.ok(cookService.getRecipe(dishId));
    }
}
