package com.yechijia.model;

/**
 * 一款可购买的饮品（喝什么场景）。
 * 按「产品名 + 商家」成对推荐，只收录连锁品牌中常见、通常可以买到的产品。
 * 注意：真实「当前买得到」需接入外卖/地图在售数据，当前为静态示例数据。
 *
 * @param id          产品唯一标识
 * @param productName 产品名称（如：珍珠奶茶、生椰拿铁）
 * @param brand       商家名称（如：蜜雪冰城、瑞幸咖啡）
 * @param category     饮品种类：milk-tea / coffee / others
 * @param priceYuan    参考价格（元）
 * @param note         推荐备注（如少冰三分甜）
 */
public record DrinkProduct(String id, String productName, String brand, String category, double priceYuan, String note) {
}
