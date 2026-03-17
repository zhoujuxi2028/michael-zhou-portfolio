package com.zct.poc.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 通用响应DTO
 * 参考vendor-b系统的BaseResponseDTO实现
 */
@Data
@ApiModel("通用响应")
public class BaseResponseDTO<T> {

    @ApiModelProperty("响应码 200-成功")
    private Integer code;

    @ApiModelProperty("响应消息")
    private String message;

    @ApiModelProperty("是否成功")
    private Boolean success;

    @ApiModelProperty("响应数据")
    private T data;

    @ApiModelProperty("时间戳")
    private Long timestamp;

    public BaseResponseDTO() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> BaseResponseDTO<T> success(T data) {
        BaseResponseDTO<T> response = new BaseResponseDTO<>();
        response.setCode(200);
        response.setMessage("success");
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    public static <T> BaseResponseDTO<T> error(Integer code, String message) {
        BaseResponseDTO<T> response = new BaseResponseDTO<>();
        response.setCode(code);
        response.setMessage(message);
        response.setSuccess(false);
        response.setData(null);
        return response;
    }

    public static <T> BaseResponseDTO<T> error(String message) {
        return error(500, message);
    }
}