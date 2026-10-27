package com.yechijia.service;

import com.yechijia.common.BusinessException;
import com.yechijia.model.FoodCategory;
import com.yechijia.model.Restaurant;
import com.yechijia.repository.InMemoryUserStore;
import com.yechijia.repository.StaticDataLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Comparator;

/**
 * 「出去吃」服务。
 * 核心流程：推荐 → 用户排除不想吃的类目 → 点随机换一个（会话内去重）→ 确定。
 * 两种推荐模式：
 * 1. custom：从用户自建食用类中按权重抽签选类目，再在该类目门店中选一家；
 * 2. nearby：调用地图客户端搜索周边美食并推荐最近的一家。
 */
@Service
public class EatOutService {

    /** 地图客户端：搜索周边美食 */
    private final MapClient mapClient;

    /** 示例门店池（custom 模式下按类目匹配时使用） */
    private final StaticDataLoader staticDataLoader;

    /** 用户数据存储（自建食用类） */
    private final InMemoryUserStore userStore;

    /** 附近搜索半径（公里），从配置读取，需求要求 3km */
    @Value("${yechijia.nearby-radius-km:3}")
    private double nearbyRadiusKm;

    /**
     * 构造服务：注入地图客户端、静态数据与用户存储。
     *
     * @param mapClient       地图客户端
     * @param staticDataLoader 静态数据加载器
     * @param userStore       用户数据存储
     */
    public EatOutService(MapClient mapClient, StaticDataLoader staticDataLoader, InMemoryUserStore userStore) {
        this.mapClient = mapClient;
        this.staticDataLoader = staticDataLoader;
        this.userStore = userStore;
    }

    /**
     * 获取演示门店池中实际存在的美食类型。
     *
     * @return 按名称排序的去重类型列表
     */
    public List<String> listNearbyTypes() {
        return staticDataLoader.getRestaurants().stream()
                .map(Restaurant::categoryName)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    /**
     * 推荐一家门店（出去吃的入口推荐）。
     *
     * @param mode                 推荐模式：custom / nearby；空值按 custom 处理
     * @param latitude             用户纬度（nearby 模式使用）
     * @param longitude            用户经度（nearby 模式使用）
     * @param excludedCategoryIds  用户不想吃的类目（先排除再推荐）
     * @param preferredCategoryIds 用户心动的类目（nearby 模式优先推荐）
     * @param excludedRestaurantIds 已推荐过不想再看到的门店（去重）
     * @return 推荐的门店
     */
    public Restaurant recommend(String mode, Double latitude, Double longitude,
                                 List<String> excludedCategoryIds, List<String> preferredCategoryIds,
                                 List<String> excludedCategoryNames, List<String> preferredCategoryNames,
                                 List<String> excludedRestaurantIds) {
        List<String> excludedCategories = safeList(excludedCategoryIds);
        List<String> preferredCategories = safeList(preferredCategoryIds);
        List<String> excludedRestaurants = safeList(excludedRestaurantIds);
        if ("nearby".equalsIgnoreCase(mode)) {
            return recommendNearby(latitude, longitude, excludedCategories,
                    preferredCategories, safeList(excludedCategoryNames),
                    safeList(preferredCategoryNames), excludedRestaurants);
        }
        return recommendByCustomCategory(excludedCategories, excludedRestaurants);
    }

    /**
     * 周边模式：调用地图搜索周边美食，排除已看过的门店后取距离最近的一家。
     *
     * @param latitude              用户纬度
     * @param longitude             用户经度
     * @param excludedRestaurantIds 已看过的门店标识
     * @return 推荐门店
     */
    private Restaurant recommendNearby(Double latitude, Double longitude, List<String> excludedCategoryIds,
                                       List<String> preferredCategoryIds,
                                       List<String> excludedCategoryNames,
                                       List<String> preferredCategoryNames,
                                       List<String> excludedRestaurantIds) {
        // 缺少经纬度时直接报错，避免静默以 (0,0) 为圆心搜索导致结果失真
        if (latitude == null || longitude == null) {
            throw new BusinessException("周边模式需要定位信息，请允许定位或改用自建类抽签");
        }
        double lat = latitude;
        double lng = longitude;
        Map<String, String> categoryNamesById = new HashMap<>();
        userStore.findAllCategories().forEach(category ->
                categoryNamesById.put(category.id(), category.name()));
        List<String> excludedCategoryNamesFromIds = excludedCategoryIds.stream()
                .map(categoryNamesById::get)
                .filter(name -> name != null)
                .toList();
        List<String> preferredCategoryNamesFromIds = preferredCategoryIds.stream()
                .map(categoryNamesById::get)
                .filter(name -> name != null)
                .toList();
        List<String> allExcludedCategoryNames = java.util.stream.Stream
                .concat(excludedCategoryNamesFromIds.stream(), excludedCategoryNames.stream())
                .distinct()
                .toList();
        List<String> allPreferredCategoryNames = java.util.stream.Stream
                .concat(preferredCategoryNamesFromIds.stream(), preferredCategoryNames.stream())
                .distinct()
                .toList();
        List<Restaurant> candidates = mapClient.searchNearbyFood(lat, lng, nearbyRadiusKm, null)
                .stream()
                .filter(r -> !allExcludedCategoryNames.contains(r.categoryName()))
                .filter(r -> !excludedRestaurantIds.contains(r.id()))
                .toList();
        if (candidates.isEmpty()) {
            throw new BusinessException("附近没有符合当前筛选条件的美食，调整讨厌类型或清空本轮已看后再试");
        }
        // 心动类型优先于距离；没有心动类型可用时仍从其余候选中按距离推荐
        return candidates.stream()
                .sorted((first, second) -> {
                    boolean firstPreferred = allPreferredCategoryNames.contains(first.categoryName());
                    boolean secondPreferred = allPreferredCategoryNames.contains(second.categoryName());
                    if (firstPreferred != secondPreferred) {
                        return firstPreferred ? -1 : 1;
                    }
                    return Integer.compare(first.distanceMeters(), second.distanceMeters());
                })
                .findFirst()
                .orElseThrow(() -> new BusinessException("附近没有找到可推荐的美食"));
    }

    /**
     * 自建类目模式：按权重抽签选类目，再从该类目的门店中随机选一家。
     *
     * @param excludedCategoryIds   用户排除的类目
     * @param excludedRestaurantIds 已看过的门店
     * @return 推荐门店
     */
    private Restaurant recommendByCustomCategory(List<String> excludedCategoryIds, List<String> excludedRestaurantIds) {
        // 第一步：先按用户「不想吃」的排除过滤类目
        List<FoodCategory> notExcludedCategories = userStore.findAllCategories().stream()
                .filter(c -> !excludedCategoryIds.contains(c.id()))
                .toList();
        if (notExcludedCategories.isEmpty()) {
            throw new BusinessException("类目都被排除了，放开一个试试吧");
        }
        // 第二步：剔除「排除已看门店后已无可用门店」的类目再抽签，
        // 保证抽签池里每个类目都有门店可选，不会再出现抽中空类目后无法回退的失败
        Map<String, List<Restaurant>> availableByCategoryId = new HashMap<>();
        for (FoodCategory category : notExcludedCategories) {
            List<Restaurant> restaurantCandidates = staticDataLoader.getRestaurants().stream()
                    .filter(r -> r.categoryName().equals(category.name()))
                    .filter(r -> !excludedRestaurantIds.contains(r.id()))
                    .toList();
            if (!restaurantCandidates.isEmpty()) {
                availableByCategoryId.put(category.id(), restaurantCandidates);
            }
        }
        if (availableByCategoryId.isEmpty()) {
            throw new BusinessException("所有类目的门店都看过了，清空本轮已看记录或放开排除再试吧");
        }
        // 第三步：在仍有可用门店的类目中按权重抽签，并从该类目可用门店中随机选一家
        List<FoodCategory> drawableCategories = notExcludedCategories.stream()
                .filter(c -> availableByCategoryId.containsKey(c.id()))
                .toList();
        FoodCategory picked = pickWeightedRandom(drawableCategories);
        return pickRandom(availableByCategoryId.get(picked.id()));
    }

    /**
     * 加权抽签：权重越大的类目被抽中的概率越高。
     * 算法：把权重累加成区间，随机落点定位到某个类目。
     *
     * @param categories 候选类目列表（权重必须为正数）
     * @return 被抽中的类目
     */
    private FoodCategory pickWeightedRandom(List<FoodCategory> categories) {
        int totalWeight = categories.stream()
                .mapToInt(c -> Math.max(c.weight(), 1))
                .sum();
        Random random = ThreadLocalRandom.current();
        int point = random.nextInt(totalWeight);
        int accumulated = 0;
        for (FoodCategory category : categories) {
            accumulated += Math.max(category.weight(), 1);
            if (point < accumulated) {
                return category;
            }
        }
        // 理论上不会走到这里，兜底返回最后一个
        return categories.get(categories.size() - 1);
    }

    /**
     * 从列表中均匀随机取一个元素。
     *
     * @param list 候选列表
     * @param <T>  元素类型
     * @return 随机选中的元素
     */
    private <T> T pickRandom(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /**
     * 空列表保护：把可能为 null 的入参统一成空列表，简化上层处理。
     *
     * @param list 原始列表
     * @param <T>  元素类型
     * @return 非 null 列表
     */
    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }
}
