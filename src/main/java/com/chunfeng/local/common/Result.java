package com.chunfeng.local.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一交互结果
 *
 * @author 13994
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {
    /**
     * 状态码
     */
    private Integer code;
    /**
     * 提示信息
     */
    private String message;
    /**
     * 返回数据
     */
    private T data;

    /**
     * 成功响应
     *
     * @param data 数据
     * @param <T>  泛型类型
     * @return 响应结果
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    public static <T> Result<T> success() {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .build();
    }

    /**
     * 失败响应
     *
     * @param message 消息
     * @param <T>     泛型类型
     * @return 响应结果
     */
    public static <T> Result<T> error(String message) {
        return Result.<T>builder()
                .code(400)
                .message(message)
                .build();
    }

    /**
     * 自定义响应
     *
     * @param code    状态码
     * @param message 消息
     * @param data    数据
     * @param <T>     泛型类型
     * @return 响应结果
     */
    public static <T> Result<T> error(Integer code, String message, T data) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}