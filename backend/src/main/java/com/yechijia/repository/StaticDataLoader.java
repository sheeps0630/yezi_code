package com.yechijia.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yechijia.common.BusinessException;
import com.yechijia.model.Dish;
import com.yechijia.model.DrinkProduct;
import com.yechijia.model.Recipe;
import com.yechijia.model.Restaurant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import jakarta.annotation.PostConstruct;

/**
 * 静态数据加载器。
 * 启动时从 classpath 的 data 目录加载真实菜谱、示例门店与饮品数据到内存，
 * 供各业务服务读取。菜谱数据为人工整理的家常经典做法，禁止动态编造。
 */
@Component
public class StaticDataLoader {

    /** 日志对象：记录数据加载结果 */
    private static final Logger log = LoggerFactory.getLogger(StaticDataLoader.class);

    /** Jackson JSON 解析器 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 全部菜品（素菜/肉菜/甜品） */
    private List<Dish> dishes = Collections.emptyList();

    /** 全部菜谱（与菜品一一对应） */
    private List<Recipe> recipes = Collections.emptyList();

    /** 示例门店池（生产环境由地图客户端实时获取） */
    private List<Restaurant> restaurants = Collections.emptyList();

    /** 示例饮品池（产品名 + 商家） */
    private List<DrinkProduct> drinkProducts = Collections.emptyList();

    /**
     * 启动时加载全部静态数据文件；任一文件缺失或解析失败则直接启动失败，
     * 避免服务带着空数据对外提供推荐。
     */
    @PostConstruct
    public void loadAll() {
        this.dishes = loadList("data/dishes.json", new TypeReference<List<Dish>>() {
        });
        List<Dish> additionalDishes = loadList("data/dishes-extra.json", new TypeReference<List<Dish>>() {
        });
        List<Dish> allDishes = new ArrayList<>(this.dishes);
        allDishes.addAll(additionalDishes);
        this.dishes = List.copyOf(allDishes);
        List<Recipe> baseRecipes = loadList("data/recipes.json", new TypeReference<List<Recipe>>() {
        });
        List<Recipe> additionalRecipes = loadList("data/recipes-extra.json", new TypeReference<List<Recipe>>() {
        });
        List<Recipe> allRecipes = new ArrayList<>(baseRecipes);
        allRecipes.addAll(additionalRecipes);
        this.recipes = List.copyOf(allRecipes);
        this.restaurants = loadList("data/restaurants.json", new TypeReference<List<Restaurant>>() {
        });
        this.drinkProducts = loadList("data/drinks.json", new TypeReference<List<DrinkProduct>>() {
        });
        log.info("静态数据加载完成：菜品 {} 道，菜谱 {} 份，门店 {} 家，饮品 {} 款",
                dishes.size(), recipes.size(), restaurants.size(), drinkProducts.size());
    }

    /**
     * 从 classpath 读取一个 JSON 数组文件并反序列化为对象列表。
     *
     * @param path         classpath 路径（如 data/dishes.json）
     * @param type         Jackson 类型引用
     * @param <T>          目标元素类型
     * @return 反序列化后的列表
     */
    private <T> List<T> loadList(String path, TypeReference<List<T>> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            List<T> result = objectMapper.readValue(in, type);
            if (result == null || result.isEmpty()) {
                throw new BusinessException("静态数据文件为空：" + path);
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 数据文件缺失/格式错误属于启动级问题，直接失败并给出明确原因
            throw new IllegalStateException("静态数据加载失败：" + path + "，" + e.getMessage(), e);
        }
    }

    /** 获取全部菜品 */
    public List<Dish> getDishes() {
        return dishes;
    }

    /** 获取全部菜谱 */
    public List<Recipe> getRecipes() {
        return recipes;
    }

    /** 获取示例门店池 */
    public List<Restaurant> getRestaurants() {
        return restaurants;
    }

    /** 获取示例饮品池 */
    public List<DrinkProduct> getDrinkProducts() {
        return drinkProducts;
    }
}
