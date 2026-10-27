package com.yechijia.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器。
 * 把各类异常统一转换成 ApiResponse 结构，避免把堆栈信息泄露给前端。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 日志对象：记录服务端异常，便于排查问题 */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常：返回用户可读的失败信息。
     *
     * @param ex 捕获到的业务异常
     * @return 统一失败响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.fail(ex.getMessage());
    }

    /**
     * 处理请求体不可读：空请求体或畸形 JSON 属于客户端错误，给出可读提示。
     *
     * @param ex 请求体解析异常
     * @return 统一失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        log.warn("请求体缺失或格式不正确：{}", ex.getMessage());
        return ApiResponse.fail("请求体缺失或格式不正确，请检查后重试");
    }

    /**
     * 处理参数类型不匹配：如路径参数 / 查询参数类型传错。
     *
     * @param ex 参数类型不匹配异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("参数类型不匹配：{}", ex.getMessage());
        return ApiResponse.fail("参数「" + ex.getName() + "」的类型不正确，请检查后重试");
    }

    /**
     * 处理缺少必填参数：如查询参数缺失。
     *
     * @param ex 缺少参数异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("缺少必填参数：{}", ex.getMessage());
        return ApiResponse.fail("缺少必填参数「" + ex.getParameterName() + "」");
    }

    /**
     * 处理未预期的系统异常：隐藏内部细节，只提示服务异常。
     *
     * @param ex 捕获到的未知异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpected(Exception ex) {
        log.error("接口发生未预期异常", ex);
        return ApiResponse.fail("服务开小差了，请稍后再试");
    }
}
