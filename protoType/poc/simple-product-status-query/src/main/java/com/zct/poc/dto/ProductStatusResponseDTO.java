package com.zct.poc.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;

/**
 * 产品状态查询响应DTO
 * 参考toyou系统的TDeviceRespDTO实现，简化版本
 */
@Data
@ApiModel("产品状态查询响应")
public class ProductStatusResponseDTO {

    @ApiModelProperty("设备ID")
    private String id;

    @ApiModelProperty("基站ID")
    private String lbsId;

    @ApiModelProperty("设备名称")
    private String deviceName;

    @ApiModelProperty("设备型号")
    private String model;

    @ApiModelProperty("设备类型")
    private String deviceType;

    @ApiModelProperty("项目ID")
    private String projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("公司ID")
    private String companyId;

    @ApiModelProperty("公司名称")
    private String companyName;

    @ApiModelProperty("设备状态(11位状态码)")
    private String status;

    @ApiModelProperty("设备状态描述")
    private String statusDescription;

    @ApiModelProperty("在线状态 1-在线 0-离线")
    private Integer onlineStatus;

    @ApiModelProperty("最后上报时间")
    private Date lastReportTime;

    @ApiModelProperty("设备流程状态")
    private String processStatus;

    @ApiModelProperty("设备流程状态名称")
    private String processStatusName;

    @ApiModelProperty("升级状态")
    private String upgradeStatus;

    @ApiModelProperty("升级状态名称")
    private String upgradeStatusName;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("更新时间")
    private Date updateTime;

    @ApiModelProperty("备注")
    private String remarks;
}