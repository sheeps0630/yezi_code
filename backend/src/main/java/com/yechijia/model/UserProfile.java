package com.yechijia.model;

/**
 * 用户个人信息：头像、名称、手机号。
 * 演示阶段使用内存存储；生产环境接入登录体系与数据库。
 *
 * @param nickname  用户昵称
 * @param avatarUrl 头像图片地址
 * @param phone     手机号
 */
public record UserProfile(String nickname, String avatarUrl, String phone) {
}
