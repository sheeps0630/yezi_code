package com.yechijia.dto;

import com.yechijia.model.Dish;

import java.util.List;

/**
 * 「在家吃」生成的一餐菜点结果。
 *
 * @param mainDish 主菜（可为 null，表示用户未指定主菜）
 * @param dishes   本餐包含的全部菜品（含主菜）
 */
public record MenuResponse(Dish mainDish, List<Dish> dishes) {
}
