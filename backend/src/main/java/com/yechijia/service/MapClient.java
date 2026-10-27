package com.yechijia.service;

import com.yechijia.model.Restaurant;

import java.util.List;

/**
 * 地图客户端接口。
 * 「出去吃」的周边美食推荐、「喝什么」的附近店铺搜索都依赖地图能力。
 * 生产环境实现：接入高德/腾讯地图开放平台（POI 周边搜索接口）。
 * 当前演示环境使用 StubMapClient 返回本地示例数据。
 */
public interface MapClient {

    /**
     * 搜索指定坐标周边的美食门店。
     *
     * @param latitude      用户纬度
     * @param longitude     用户经度
     * @param radiusKm      搜索半径（公里）
     * @param keyword       搜索关键词（如美食分类名，可为空）
     * @return 周边门店列表（按距离升序）
     */
    List<Restaurant> searchNearbyFood(double latitude, double longitude, double radiusKm, String keyword);

    /**
     * 当前地图客户端是否为示例数据实现。
     * 前端可据此展示「示例数据」提示，避免误导用户以为就是真实周边门店。
     *
     * @return true 表示返回的是本地示例数据
     */
    boolean isStubData();
}
