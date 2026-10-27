package com.yechijia.dto;

/**
 * 加入家庭菜谱的请求体。
 *
 * @param dishId  菜品标识
 * @param addedBy 添加人名称（可空，空时后端取当前用户昵称）
 */
public record FamilyRecipeRequest(String dishId, String addedBy) {
}
