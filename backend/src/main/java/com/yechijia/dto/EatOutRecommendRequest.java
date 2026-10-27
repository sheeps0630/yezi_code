package com.yechijia.dto;

import java.util.List;

/**
 * 「出去吃」推荐请求。
 *
 * @param mode                 推荐模式：custom-自建食用类加权抽签 / nearby-地图周边美食
 * @param latitude             用户纬度（nearby 模式使用）
 * @param longitude            用户经度（nearby 模式使用）
 * @param excludedCategoryIds  不想吃的类目（先筛选再随机）
 * @param preferredCategoryIds 心动的类目（附近推荐时优先）
 * @param excludedCategoryNames 附近模式不喜欢的门店类型名称
 * @param preferredCategoryNames 附近模式喜欢的门店类型名称
 * @param excludedRestaurantIds 本会话已推荐过、不想再看到的门店（随机时去重）
 */
public record EatOutRecommendRequest(
        String mode,
        Double latitude,
        Double longitude,
        List<String> excludedCategoryIds,
        List<String> preferredCategoryIds,
        List<String> excludedCategoryNames,
        List<String> preferredCategoryNames,
        List<String> excludedRestaurantIds) {
}
