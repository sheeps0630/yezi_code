package com.yechijia.dto;

import java.util.List;
import java.util.Map;

/**
 * 「在家吃」生成菜点请求。
 *
 * @param categories 选择的种类（必选，可多选）：vegetable / meat / dessert / seafood / soup
 * @param mainDishId 主菜菜品标识（可选；不传则不指定主菜）
 * @param ingredientFilters 各种类中选择的食材标签（多个标签按或匹配）
 */
public record CookMenuRequest(List<String> categories, String mainDishId,
                              Map<String, List<String>> ingredientFilters) {
}
