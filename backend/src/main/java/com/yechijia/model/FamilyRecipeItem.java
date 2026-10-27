package com.yechijia.model;

import java.time.LocalDateTime;

/**
 * 家庭菜谱条目：加入家庭菜谱库的一道菜，供全家成员共同查看与做饭时选用。
 *
 * @param id        条目唯一标识
 * @param dishId    关联菜品标识
 * @param dishName  菜名
 * @param addedBy   添加人名称
 * @param createdAt 添加时间
 */
public record FamilyRecipeItem(String id, String dishId, String dishName, String addedBy, LocalDateTime createdAt) {
}
