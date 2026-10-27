package com.yechijia.dto;

/**
 * 新增家庭成员的请求体。
 *
 * @param name     成员名称
 * @param relation 与当前用户的关系
 * @param phone    成员手机号
 */
public record FamilyMemberRequest(String name, String relation, String phone) {
}
