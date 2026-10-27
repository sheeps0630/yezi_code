package com.yechijia.model;

/**
 * 一家可去的门店（出去吃场景）。
 * 当前数据来自示例门店池；生产环境由地图客户端（高德/腾讯）实时返回。
 *
 * @param id             门店唯一标识
 * @param name           门店名称
 * @param categoryName   所属食用类名称（火锅/烧烤/日料等，与 FoodCategory.name 对应）
 * @param address        门店地址
 * @param distanceMeters 距用户的直线距离（米，示例数据）
 * @param rating         门店评分（0-5）
 */
public record Restaurant(String id, String name, String categoryName, String address, int distanceMeters, double rating) {
}
