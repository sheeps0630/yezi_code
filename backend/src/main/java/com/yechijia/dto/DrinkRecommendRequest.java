package com.yechijia.dto;

import java.util.List;

/**
 * 「喝什么」推荐请求。
 *
 * @param useNearby         是否选择附近店铺：true-地图搜附近 3km / false-随机生成
 * @param category          饮品种类（可选）：milk-tea / coffee / others
 * @param latitude          用户纬度（useNearby 为 true 时使用）
 * @param longitude         用户经度（useNearby 为 true 时使用）
 * @param excludedProductIds 本会话已推荐过、不想再看到的饮品（随机时去重）
 */
public record DrinkRecommendRequest(
        Boolean useNearby,
        String category,
        Double latitude,
        Double longitude,
        List<String> excludedProductIds) {
}
