package com.yechijia.dto;

/**
 * 更新收藏置顶状态的请求体。
 *
 * @param pinned 是否置顶
 */
public record FavoritePinRequest(boolean pinned) {
}
