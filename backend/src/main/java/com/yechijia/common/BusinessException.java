package com.yechijia.common;

/**
 * 业务异常。
 * 业务规则不满足（如：菜谱不存在、可选项被全部排除）时抛出，
 * 由全局异常处理器统一转换为友好提示。
 */
public class BusinessException extends RuntimeException {

    /**
     * 构造业务异常。
     *
     * @param message 异常信息，将直接展示给用户
     */
    public BusinessException(String message) {
        super(message);
    }
}
