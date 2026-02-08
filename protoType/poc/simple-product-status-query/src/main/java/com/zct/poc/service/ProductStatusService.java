package com.zct.poc.service;

import com.zct.poc.dto.ProductStatusRequestDTO;
import com.zct.poc.dto.ProductStatusResponseDTO;
import com.zct.poc.dto.BaseResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 产品状态查询服务
 * 参考toyou系统的TDeviceService.listByLbsId()方法实现
 * 简化版本，包含详细日志记录
 */
@Service
@Slf4j
public class ProductStatusService {

    /**
     * 根据LbsId查询产品状态
     * 参考toyou系统实现逻辑
     *
     * @param request 查询请求
     * @return 产品状态信息
     */
    public BaseResponseDTO<ProductStatusResponseDTO> queryProductStatusByLbsId(ProductStatusRequestDTO request) {
        log.info("开始执行产品状态查询，请求参数: {}", request);

        try {
            // 1. 参数校验
            if (!validateRequest(request)) {
                log.error("参数校验失败，lbsId为空或无效");
                return BaseResponseDTO.error(400, "lbsId不能为空");
            }

            // 2. 模拟数据库查询 - 基础设备信息
            log.debug("开始查询基础设备信息，lbsId: {}", request.getLbsId());
            ProductStatusResponseDTO deviceInfo = queryDeviceBasicInfo(request);

            if (deviceInfo == null) {
                log.warn("未找到对应的设备信息，lbsId: {}", request.getLbsId());
                return BaseResponseDTO.error(404, "未找到对应的设备信息");
            }

            // 3. 查询设备状态信息
            log.debug("开始查询设备状态信息，deviceId: {}", deviceInfo.getId());
            enrichDeviceStatus(deviceInfo);

            // 4. 查询项目信息
            log.debug("开始查询项目信息，projectId: {}", deviceInfo.getProjectId());
            enrichProjectInfo(deviceInfo);

            // 5. 查询公司信息
            log.debug("开始查询公司信息，companyId: {}", deviceInfo.getCompanyId());
            enrichCompanyInfo(deviceInfo);

            // 6. 查询升级状态
            log.debug("开始查询升级状态，deviceId: {}", deviceInfo.getId());
            enrichUpgradeStatus(deviceInfo);

            // 7. 查询流程状态名称
            log.debug("开始查询流程状态名称，processStatus: {}", deviceInfo.getProcessStatus());
            enrichProcessStatusName(deviceInfo);

            log.info("产品状态查询完成，设备: {}, 状态: {}", deviceInfo.getDeviceName(), deviceInfo.getStatus());
            return BaseResponseDTO.success(deviceInfo);

        } catch (Exception e) {
            log.error("查询产品状态时发生异常，lbsId: {}", request.getLbsId(), e);
            return BaseResponseDTO.error(500, "系统内部错误: " + e.getMessage());
        }
    }

    /**
     * 参数校验
     */
    private boolean validateRequest(ProductStatusRequestDTO request) {
        log.debug("执行参数校验");

        if (request == null) {
            log.error("请求参数为null");
            return false;
        }

        if (!StringUtils.hasText(request.getLbsId())) {
            log.error("lbsId为空或空字符串");
            return false;
        }

        log.debug("参数校验通过");
        return true;
    }

    /**
     * 查询设备基础信息
     * 模拟toyou系统中的数据库查询
     */
    private ProductStatusResponseDTO queryDeviceBasicInfo(ProductStatusRequestDTO request) {
        log.debug("模拟查询数据库 - 基础设备信息");

        // 模拟数据库查询延迟
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 模拟查询结果 - 根据lbsId生成不同的测试数据
        ProductStatusResponseDTO response = new ProductStatusResponseDTO();
        response.setId("device_" + request.getLbsId().hashCode());
        response.setLbsId(request.getLbsId());
        response.setDeviceName("测试设备_" + request.getLbsId());
        response.setModel("ZC-001");
        response.setDeviceType("4G设备");
        response.setProjectId("project_001");
        response.setCompanyId("company_001");
        response.setProcessStatus("3"); // 调试成功
        response.setCreateTime(new Date());
        response.setUpdateTime(new Date());

        log.info("查询到设备基础信息 - ID: {}, 名称: {}, 型号: {}",
                response.getId(), response.getDeviceName(), response.getModel());

        return response;
    }

    /**
     * 补充设备状态信息
     * 参考toyou系统的状态处理逻辑
     */
    private void enrichDeviceStatus(ProductStatusResponseDTO deviceInfo) {
        log.debug("开始补充设备状态信息");

        // 模拟状态计算 - toyou系统中有复杂的状态码逻辑
        String statusCode = calculateDeviceStatus(deviceInfo.getLbsId());
        deviceInfo.setStatus(statusCode);
        deviceInfo.setStatusDescription(parseStatusDescription(statusCode));

        // 模拟在线状态判断
        deviceInfo.setOnlineStatus(isDeviceOnline(statusCode) ? 1 : 0);

        // 模拟最后上报时间
        deviceInfo.setLastReportTime(new Date(System.currentTimeMillis() - 30000)); // 30秒前

        log.info("设备状态信息补充完成 - 状态码: {}, 在线状态: {}, 最后上报: {}",
                deviceInfo.getStatus(), deviceInfo.getOnlineStatus(), deviceInfo.getLastReportTime());
    }

    /**
     * 计算设备状态码
     * 参考toyou系统的状态计算逻辑
     */
    private String calculateDeviceStatus(String lbsId) {
        log.debug("计算设备状态码，lbsId: {}", lbsId);

        // 模拟toyou系统中的11位状态码计算
        // 根据lbsId的hash值模拟不同的状态
        int hash = Math.abs(lbsId.hashCode()) % 3;

        String statusCode;
        switch (hash) {
            case 0:
                statusCode = "11111111111"; // 全部正常
                log.debug("生成正常状态码: {}", statusCode);
                break;
            case 1:
                statusCode = "11110111111"; // 部分模块异常
                log.debug("生成部分异常状态码: {}", statusCode);
                break;
            default:
                statusCode = "00000000000"; // 设备离线
                log.debug("生成离线状态码: {}", statusCode);
                break;
        }

        return statusCode;
    }

    /**
     * 解析状态描述
     */
    private String parseStatusDescription(String statusCode) {
        log.debug("解析状态描述，状态码: {}", statusCode);

        if ("11111111111".equals(statusCode)) {
            return "设备运行正常";
        } else if ("00000000000".equals(statusCode)) {
            return "设备离线";
        } else {
            return "设备部分功能异常";
        }
    }

    /**
     * 判断设备是否在线
     */
    private boolean isDeviceOnline(String statusCode) {
        // 如果第一位或第二位是0，则认为离线
        boolean online = !(statusCode.substring(0, 1).equals("0") || statusCode.substring(1, 2).equals("0"));
        log.debug("设备在线状态判断结果: {}, 状态码: {}", online, statusCode);
        return online;
    }

    /**
     * 补充项目信息
     */
    private void enrichProjectInfo(ProductStatusResponseDTO deviceInfo) {
        log.debug("开始补充项目信息");

        if (StringUtils.hasText(deviceInfo.getProjectId())) {
            // 模拟项目查询
            deviceInfo.setProjectName("测试项目_" + deviceInfo.getProjectId());
            log.info("项目信息补充完成 - 项目名称: {}", deviceInfo.getProjectName());
        } else {
            log.warn("项目ID为空，跳过项目信息查询");
        }
    }

    /**
     * 补充公司信息
     */
    private void enrichCompanyInfo(ProductStatusResponseDTO deviceInfo) {
        log.debug("开始补充公司信息");

        if (StringUtils.hasText(deviceInfo.getCompanyId())) {
            // 模拟公司查询
            deviceInfo.setCompanyName("测试公司_" + deviceInfo.getCompanyId());
            log.info("公司信息补充完成 - 公司名称: {}", deviceInfo.getCompanyName());
        } else {
            log.warn("公司ID为空，跳过公司信息查询");
        }
    }

    /**
     * 补充升级状态信息
     */
    private void enrichUpgradeStatus(ProductStatusResponseDTO deviceInfo) {
        log.debug("开始补充升级状态信息");

        // 模拟升级状态查询
        int statusRandom = Math.abs(deviceInfo.getId().hashCode()) % 4;

        switch (statusRandom) {
            case 0:
                deviceInfo.setUpgradeStatus("0");
                deviceInfo.setUpgradeStatusName("未升级");
                break;
            case 1:
                deviceInfo.setUpgradeStatus("1");
                deviceInfo.setUpgradeStatusName("升级中");
                break;
            case 2:
                deviceInfo.setUpgradeStatus("2");
                deviceInfo.setUpgradeStatusName("升级成功");
                break;
            default:
                deviceInfo.setUpgradeStatus("3");
                deviceInfo.setUpgradeStatusName("升级失败");
                break;
        }

        log.info("升级状态补充完成 - 状态: {}, 描述: {}",
                deviceInfo.getUpgradeStatus(), deviceInfo.getUpgradeStatusName());
    }

    /**
     * 补充流程状态名称
     */
    private void enrichProcessStatusName(ProductStatusResponseDTO deviceInfo) {
        log.debug("开始补充流程状态名称");

        if (StringUtils.hasText(deviceInfo.getProcessStatus())) {
            // 模拟字典查询 - 参考toyou系统的DictConstant
            switch (deviceInfo.getProcessStatus()) {
                case "1":
                    deviceInfo.setProcessStatusName("待采购");
                    break;
                case "2":
                    deviceInfo.setProcessStatusName("已采购");
                    break;
                case "3":
                    deviceInfo.setProcessStatusName("调试成功");
                    break;
                case "4":
                    deviceInfo.setProcessStatusName("调试失败");
                    break;
                default:
                    deviceInfo.setProcessStatusName("未知状态");
                    break;
            }

            log.info("流程状态名称补充完成 - 状态: {}, 名称: {}",
                    deviceInfo.getProcessStatus(), deviceInfo.getProcessStatusName());
        } else {
            log.warn("流程状态为空，跳过状态名称补充");
        }
    }
}