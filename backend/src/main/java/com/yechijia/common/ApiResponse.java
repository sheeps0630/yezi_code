package com.yechijia.common;

/**
 * 统一接口响应包装类。
 * 所有接口都返回 { code, message, data } 结构，前端按同一格式解析。
 *
 * @param <T> 业务数据的类型
 */
public class ApiResponse<T> {

    /** 响应码：0 表示成功，非 0 表示失败 */
    private final int code;

    /** 提示信息：失败时给出可直接展示给用户的原因 */
    private final String message;

    /** 业务数据：成功时携带，失败时为 null */
    private final T data;

    /**
     * 全参构造：内部使用，外部请用 ok / fail 工厂方法。
     *
     * @param code    响应码
     * @param message 提示信息
     * @param data    业务数据
     */
    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 构造成功响应。
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 包装好的成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    /**
     * 构造失败响应。
     *
     * @param message 失败原因（可直接展示给用户）
     * @param <T>     业务数据类型
     * @return 包装好的失败响应
     */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(1, message, null);
    }

    /** 获取响应码 */
    public int getCode() {
        return code;
    }

    /** 获取提示信息 */
    public String getMessage() {
        return message;
    }

    /** 获取业务数据 */
    public T getData() {
        return data;
    }
}
