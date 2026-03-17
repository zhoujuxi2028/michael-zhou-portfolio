package com.zct.poc.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 产品状态查询请求DTO
 * 参考vendor-b系统的TDeviceReqDTO实现
 */
@Data
@ApiModel("产品状态查询请求")
public class ProductStatusRequestDTO {

    @ApiModelProperty("基站ID/设备ID")
    private String lbsId;

    @ApiModelProperty("项目ID")
    private String projectId;

    @ApiModelProperty("公司ID")
    private String companyId;

    @ApiModelProperty("设备类型")
    private String deviceType;

    @ApiModelProperty("设备型号")
    private String model;

    @ApiModelProperty("是否删除 0-未删除 1-已删除")
    private Integer isDelete = 0;
}