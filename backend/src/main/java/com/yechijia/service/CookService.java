package com.yechijia.service;

import com.yechijia.common.BusinessException;
import com.yechijia.dto.MenuResponse;
import com.yechijia.model.Dish;
import com.yechijia.model.Recipe;
import com.yechijia.repository.StaticDataLoader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 「在家吃」服务。
 * 流程：选择种类（素菜/肉菜/甜品，必选）→ 可选主菜 → 生成对应菜点 → 查看真实菜谱。
 * 菜谱全部来自静态数据文件，禁止动态编造。
 */
@Service
public class CookService {

    /** 合法的种类标识：素菜 / 肉菜 / 甜品 */
    private static final List<String> VALID_CATEGORIES = List.of(
            "vegetable", "meat", "dessert", "seafood", "soup");

    /** 每个菜类可选的食材标签 */
    private static final Map<String, List<String>> INGREDIENT_TYPES = Map.of(
            "vegetable", List.of("西兰花", "白菜", "土豆", "番茄", "茄子", "豆腐", "菌菇", "鸡蛋"),
            "meat", List.of("猪肉", "牛肉", "羊肉", "鸡肉", "鸭肉"),
            "dessert", List.of("水果", "豆类", "奶制品", "银耳莲子", "薯类"),
            "seafood", List.of("鱼", "虾", "蟹", "贝类"),
            "soup", List.of("蔬菜汤", "菌菇汤", "肉汤", "海鲜汤"));

    /** 静态数据加载器：菜品与菜谱数据源 */
    private final StaticDataLoader staticDataLoader;

    /**
     * 构造服务：注入静态数据加载器。
     *
     * @param staticDataLoader 静态数据加载器
     */
    public CookService(StaticDataLoader staticDataLoader) {
        this.staticDataLoader = staticDataLoader;
    }

    /**
     * 获取在家吃饭的三大种类（供前端渲染选择器）。
     *
     * @return 种类标识列表
     */
    public List<String> listCategories() {
        return VALID_CATEGORIES;
    }

    /**
     * 获取每个菜类对应的可选食材标签。
     *
     * @return 菜类到标签的映射
     */
    public Map<String, List<String>> listIngredientTypes() {
        return INGREDIENT_TYPES;
    }

    /**
     * 列出某个种类下的全部菜品，供用户挑选主菜。
     *
     * @param category 种类标识（vegetable / meat / dessert）
     * @return 该种类的菜品列表
     */
    public List<Dish> listDishesByCategory(String category) {
        validateCategory(category);
        return staticDataLoader.getDishes().stream()
                .filter(dish -> dish.category().equals(category))
                .toList();
    }

    /**
     * 根据用户选择生成一餐菜点。
     * 规则：每个所选种类随机生成 1-2 道菜；若指定了主菜，主菜一定包含在内。
     *
     * @param categories 用户选择的种类（必选，至少一个）
     * @param mainDishId 主菜标识（可选）
     * @return 一餐的菜点组合
     */
    public MenuResponse generateMenu(List<String> categories, String mainDishId,
                                     Map<String, List<String>> ingredientFilters) {
        // 校验：种类必选
        if (categories == null || categories.isEmpty()) {
            throw new BusinessException("请至少选择一个种类（素菜 / 肉菜 / 甜品）");
        }
        categories.forEach(this::validateCategory);
        if (ingredientFilters != null) {
            ingredientFilters.forEach((category, filters) -> {
                validateCategory(category);
                if (!categories.contains(category)) {
                    throw new BusinessException("食材筛选类别不在已选菜类中");
                }
                if (filters != null && !INGREDIENT_TYPES.get(category).containsAll(filters)) {
                    throw new BusinessException("食材筛选包含无效选项，请重新选择");
                }
            });
        }

        Dish mainDish = null;
        // 校验并解析主菜（可选）：主菜必须属于所选种类之一
        if (mainDishId != null && !mainDishId.isBlank()) {
            mainDish = staticDataLoader.getDishes().stream()
                    .filter(d -> d.id().equals(mainDishId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("指定的主菜不存在，请重新选择"));
            if (!categories.contains(mainDish.category())) {
                throw new BusinessException("主菜「" + mainDish.name() + "」不在所选种类内");
            }
        }

        List<Dish> result = new ArrayList<>();
        // 主菜优先入列
        if (mainDish != null) {
            result.add(mainDish);
        }
        // 每个种类随机生成 1-2 道菜
        for (String category : categories) {
            List<String> selectedIngredients = ingredientFilters == null
                    ? List.of()
                    : ingredientFilters.getOrDefault(category, List.of());
            List<Dish> pool = listDishesByCategory(category).stream()
                    .filter(dish -> selectedIngredients.isEmpty()
                            || dish.ingredientTags().stream().anyMatch(selectedIngredients::contains))
                    .filter(d -> !result.contains(d))
                    .toList();
            if (pool.isEmpty()) {
                throw new BusinessException("“" + categoryLabel(category)
                        + "”没有符合已选食材的菜谱，请调整食材选择");
            }
            int count = Math.min(pool.size(), ThreadLocalRandom.current().nextInt(1, 3));
            List<Dish> shuffledPool = new ArrayList<>(pool);
            java.util.Collections.shuffle(shuffledPool);
            for (int i = 0; i < count; i++) {
                result.add(shuffledPool.get(i));
            }
        }
        if (result.isEmpty()) {
            throw new BusinessException("没有生成任何菜点，请重新选择");
        }
        return new MenuResponse(mainDish, result);
    }

    /**
     * 获取菜类的中文名称。
     *
     * @param category 菜类标识
     * @return 中文名称
     */
    private String categoryLabel(String category) {
        return switch (category) {
            case "vegetable" -> "素菜";
            case "meat" -> "肉菜";
            case "dessert" -> "甜品";
            case "seafood" -> "海鲜";
            case "soup" -> "汤羹";
            default -> category;
        };
    }

    /**
     * 查询一道菜的真实菜谱。
     * 菜谱来自人工整理的静态数据，找不到时给出明确提示而不是临时编造。
     *
     * @param dishId 菜品标识
     * @return 对应菜谱
     */
    public Recipe getRecipe(String dishId) {
        return staticDataLoader.getRecipes().stream()
                .filter(recipe -> recipe.dishId().equals(dishId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("该菜品暂时没有菜谱"));
    }

    /**
     * 校验种类标识是否合法。
     *
     * @param category 种类标识
     */
    private void validateCategory(String category) {
        if (!VALID_CATEGORIES.contains(category)) {
            throw new BusinessException("无效的种类：" + category);
        }
    }
}
