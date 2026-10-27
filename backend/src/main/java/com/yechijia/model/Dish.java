package com.yechijia.model;

import java.util.List;

/**
 * 一道菜（在家做饭场景）。
 * 按需求分为三大类：素菜 vegetable / 肉菜 meat / 甜品 dessert，
 * 每道菜都关联一份真实菜谱（见 Recipe）。
 *
 * @param id          菜品唯一标识
 * @param name        菜名
 * @param category    所属种类：vegetable / meat / dessert
 * @param difficulty   烹饪难度：easy / medium / hard
 * @param cookMinutes 预计烹饪时长（分钟）
 * @param ingredientTags 食材筛选标签
 */
public record Dish(String id, String name, String category, String difficulty, int cookMinutes,
                   List<String> ingredientTags) {
    public Dish {
        ingredientTags = ingredientTags == null ? List.of() : List.copyOf(ingredientTags);
    }
}
