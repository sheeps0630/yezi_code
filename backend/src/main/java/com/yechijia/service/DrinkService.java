package com.yechijia.service;

import com.yechijia.common.BusinessException;
import com.yechijia.model.DrinkProduct;
import com.yechijia.repository.StaticDataLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 「喝什么」服务。
 * 流程：询问用户是否选附近店铺 → 选种类（奶茶/咖啡/其他）→ 推荐「商家 + 产品名」。
 * 附近模式：调用地图客户端搜索 3km 内相关门店，优先推荐这些门店在售（示例池中）的饮品；
 * 随机模式：从示例饮品池随机推荐。
 * 说明：需求中「当前买得到的」依赖真实在售数据，演示版使用静态示例数据并在接口响应中标注。
 */
@Service
public class DrinkService {

    /** 合法的饮品种类：奶茶 / 咖啡 / 其他 */
    private static final List<String> VALID_CATEGORIES = List.of("milk-tea", "coffee", "others");

    /** 种类对应的搜索关键词（用于附近门店搜索） */
    private static final List<String> CATEGORY_KEYWORDS = List.of("奶茶", "咖啡", "饮品");

    /** 静态数据加载器：示例饮品池 */
    private final StaticDataLoader staticDataLoader;

    /** 地图客户端：附近店铺搜索 */
    private final MapClient mapClient;

    /** 附近搜索半径（公里），需求要求 3km */
    @Value("${yechijia.nearby-radius-km:3}")
    private double nearbyRadiusKm;

    /**
     * 构造服务：注入数据加载器与地图客户端。
     *
     * @param staticDataLoader 静态数据加载器
     * @param mapClient         地图客户端
     */
    public DrinkService(StaticDataLoader staticDataLoader, MapClient mapClient) {
        this.staticDataLoader = staticDataLoader;
        this.mapClient = mapClient;
    }

    /**
     * 获取饮品种类列表（供前端渲染选择器）。
     *
     * @return 种类标识列表
     */
    public List<String> listCategories() {
        return VALID_CATEGORIES;
    }

    /**
     * 推荐一款饮品：按用户选择走「附近店铺」或「随机生成」。
     *
     * @param useNearby          是否使用附近店铺模式（null 按 false 处理）
     * @param category           饮品种类（可选，空则不限种类）
     * @param latitude           用户纬度（附近模式使用）
     * @param longitude          用户经度（附近模式使用）
     * @param excludedProductIds 已推荐过不想再看到的饮品（会话内去重）
     * @return 推荐的饮品（产品名 + 商家）
     */
    public DrinkProduct recommend(Boolean useNearby, String category, Double latitude, Double longitude,
                                   List<String> excludedProductIds) {
        validateCategory(category);
        List<String> excluded = excludedProductIds == null ? List.of() : excludedProductIds;
        List<DrinkProduct> baseCandidates = filterCandidates(category, excluded);
        if (baseCandidates.isEmpty()) {
            throw new BusinessException("该种类的推荐都用完啦，换个种类或清空排除试试吧");
        }
        // 附近店铺模式：优先匹配附近 3km 内真实（示例）门店的商家
        if (Boolean.TRUE.equals(useNearby)) {
            DrinkProduct nearbyProduct = pickFromNearbyStores(category, latitude, longitude, baseCandidates);
            if (nearbyProduct != null) {
                return nearbyProduct;
            }
        }
        // 随机生成 / 附近未命中：从候选池均匀随机推荐
        return pickRandom(baseCandidates);
    }

    /**
     * 在附近门店中寻找可推荐的饮品：搜到的门店商家若在饮品池中有产品，则优先推荐。
     *
     * @param category       饮品种类（可为空）
     * @param latitude       用户纬度
     * @param longitude      用户经度
     * @param baseCandidates 过滤后的候选饮品
     * @return 附近门店对应的饮品；附近没有相关门店时返回 null
     */
    private DrinkProduct pickFromNearbyStores(String category, Double latitude, Double longitude,
                                               List<DrinkProduct> baseCandidates) {
        double lat = latitude == null ? 0 : latitude;
        double lng = longitude == null ? 0 : longitude;
        // 按种类选取搜索关键词：不限种类时关键词留空，搜索全部门店，
        // 避免只命中类目名含「饮品」的少数门店（蜜雪/喜茶/瑞幸等品牌门店也能进入附近匹配）
        String keyword = switch (category == null ? "" : category) {
            case "milk-tea" -> "奶茶";
            case "coffee" -> "咖啡";
            case "others" -> "饮品";
            default -> null;
        };
        List<com.yechijia.model.Restaurant> nearbyStores =
                mapClient.searchNearbyFood(lat, lng, nearbyRadiusKm, keyword);
        if (nearbyStores.isEmpty()) {
            return null;
        }
        // 收集附近门店名称集合（小写比对，避免大小写差异）
        Set<String> nearbyStoreNames = nearbyStores.stream()
                .map(store -> store.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        // 候选中优先选「商家就在附近」的饮品
        List<DrinkProduct> matched = baseCandidates.stream()
                .filter(p -> nearbyStoreNames.contains(p.brand().toLowerCase(Locale.ROOT)))
                .toList();
        if (matched.isEmpty()) {
            return null;
        }
        return pickRandom(matched);
    }

    /**
     * 按种类与排除列表过滤候选饮品池。
     *
     * @param category 种类（可为空）
     * @param excluded 需排除的饮品标识
     * @return 过滤后的候选列表
     */
    private List<DrinkProduct> filterCandidates(String category, List<String> excluded) {
        return staticDataLoader.getDrinkProducts().stream()
                .filter(p -> category == null || category.isBlank() || p.category().equals(category))
                .filter(p -> !excluded.contains(p.id()))
                .toList();
    }

    /**
     * 从候选列表中均匀随机选一款。
     *
     * @param candidates 候选饮品列表
     * @return 随机选中的饮品
     */
    private DrinkProduct pickRandom(List<DrinkProduct> candidates) {
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    /**
     * 校验饮品种类是否合法（空值放行，表示不限种类）。
     *
     * @param category 种类标识
     */
    private void validateCategory(String category) {
        if (category != null && !category.isBlank() && !VALID_CATEGORIES.contains(category)) {
            throw new BusinessException("无效的饮品种类：" + category);
        }
    }
}
