package com.landit.common.exception;

import lombok.Getter;

/**
 * 业务异常基类
 * 用于封装业务逻辑中抛出的异常，携带错误码和错误信息
 *
 * @author Azir
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    /**
     * 创建资源不存在异常
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    /**
     * 创建参数错误异常
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }

    /**
     * 创建服务器内部错误异常
     */
    public static BusinessException serverError(String message) {
        return new BusinessException(500, message);
    }

}
