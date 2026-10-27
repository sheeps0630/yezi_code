package com.yechijia.model;

/**
 * 家庭成员：家庭组内的一位成员。
 *
 * @param id       成员唯一标识
 * @param name     成员名称
 * @param relation 与当前用户的关系（如：爸爸、妹妹）
 * @param phone    联系手机号（演示环境只记录联系方式，不关联其他账号）
 */
public record FamilyMember(String id, String name, String relation, String phone) {
}
