package com.yechijia.model;

/**
 * 用户自建的「食用类」，如火锅、烧烤、日料。
 * 用户可以给不同类设置权重，权重越高在「出去吃」随机抽签时越容易命中。
 *
 * @param id           类目唯一标识
 * @param name         类目名称
 * @param weight       抽签权重（正整数，默认 1）
 * @param builtIn      是否内置类目（内置类目不可删除，用户自建的可删）
 */
public record FoodCategory(String id, String name, int weight, boolean builtIn) {
}
