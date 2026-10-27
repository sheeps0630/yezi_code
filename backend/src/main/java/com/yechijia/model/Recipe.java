package com.yechijia.model;

import java.util.List;

/**
 * 菜谱：一道菜的真实做法。
 * 需求明确「禁止胡乱生成菜谱」，因此菜谱全部来自静态数据文件（data/recipes.json），
 * 只收录家常菜的经典做法，不动态编造。
 *
 * @param dishId      关联的菜品标识
 * @param dishName    菜名
 * @param ingredients 食材清单（含用量）
 * @param steps       烹饪步骤（有序）
 * @param tips        烹饪小贴士（可为空）
 * @param servings    适用人数
 */
public record Recipe(String dishId, String dishName, List<String> ingredients, List<String> steps, String tips,
                     int servings) {
    public Recipe {
        servings = servings < 1 ? 1 : servings;
    }
}
