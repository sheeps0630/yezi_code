package com.yechijia.dto;

/**
 * 新增收藏的请求体。
 *
 * @param targetType 对象类型：dish / restaurant / drink
 * @param targetId   对象标识
 * @param title      对象标题（菜名/店名/产品名）
 */
public record FavoriteRequest(String targetType, String targetId, String title) {
}
