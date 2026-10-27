package com.yechijia.dto;

/**
 * 新建/更新自建食用类的请求体。
 *
 * @param name   类目名称
 * @param weight 抽签权重
 */
public record FoodCategoryRequest(String name, Integer weight) {
}
