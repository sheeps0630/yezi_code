package com.yechijia.service;

import com.yechijia.model.Restaurant;
import com.yechijia.repository.StaticDataLoader;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 地图客户端的桩实现（示例数据版）。
 * 说明：演示环境没有真实的地图开放平台密钥，
 * 这里返回静态门店池中的示例数据以支撑完整流程演示。
 * 接入真实地图时，新增一个实现 MapClient 的类（如 AmapMapClient）并替换本 Bean 即可，
 * 上层业务代码不需要任何改动。
 */
@Component
public class StubMapClient implements MapClient {

    /** 示例门店池 */
    private final StaticDataLoader staticDataLoader;

    /**
     * 构造桩客户端：注入静态数据加载器获取示例门店。
     *
     * @param staticDataLoader 静态数据加载器
     */
    public StubMapClient(StaticDataLoader staticDataLoader) {
        this.staticDataLoader = staticDataLoader;
    }

    /**
     * 返回示例门店列表：按传入半径过滤距离、按关键词匹配类目，并按距离升序排列。
     *
     * @param latitude  用户纬度（示例实现中不参与计算，仅保留接口契约）
     * @param longitude 用户经度（示例实现中不参与计算）
     * @param radiusKm  搜索半径（公里）
     * @param keyword   类目关键词（可为空，空则返回全部）
     * @return 示例门店列表
     */
    @Override
    public List<Restaurant> searchNearbyFood(double latitude, double longitude, double radiusKm, String keyword) {
        double radiusMeters = radiusKm * 1000;
        return staticDataLoader.getRestaurants().stream()
                // 距离超出半径的门店剔除，模拟「附近 3km」
                .filter(restaurant -> restaurant.distanceMeters() <= radiusMeters)
                // 关键词匹配门店所属类目（如「火锅」）
                .filter(restaurant -> keyword == null || keyword.isBlank()
                        || restaurant.categoryName().contains(keyword))
                .sorted(Comparator.comparingInt(Restaurant::distanceMeters))
                .toList();
    }

    /**
     * 桩实现始终返回 true，前端据此展示「示例数据」角标。
     *
     * @return 固定为 true
     */
    @Override
    public boolean isStubData() {
        return true;
    }
}
