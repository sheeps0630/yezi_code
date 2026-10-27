package com.yechijia.model;

import java.time.LocalDateTime;

/**
 * 收藏条目：用户收藏的一道家（或门店/饮品）。
 *
 * @param id        收藏唯一标识
 * @param targetType 收藏对象类型：dish-菜品 / restaurant-门店 / drink-饮品
 * @param targetId  收藏对象标识
 * @param title     收藏对象标题（菜名/店名/产品名，便于列表直接展示）
 * @param createdAt 收藏时间
 * @param pinned    是否置顶
 * @param pinnedAt  最近一次置顶时间
 */
public record FavoriteItem(String id, String targetType, String targetId, String title,
                           LocalDateTime createdAt, boolean pinned, LocalDateTime pinnedAt) {
}
