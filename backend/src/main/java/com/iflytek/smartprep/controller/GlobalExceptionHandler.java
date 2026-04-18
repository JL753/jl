package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.dto.ApiResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> illegal(IllegalArgumentException e) {
        return ApiResponse.fail(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> valid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() == null ? "参数错误" : e.getBindingResult().getFieldError().getDefaultMessage();
        return ApiResponse.fail(msg);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> ex(Exception e) {
        return ApiResponse.fail("系统异常：" + e.getMessage());
    }
}
