package com.zct.poc.controller;

import com.zct.poc.dto.ProductStatusRequestDTO;
import com.zct.poc.dto.ProductStatusResponseDTO;
import com.zct.poc.dto.BaseResponseDTO;
import com.zct.poc.service.ProductStatusService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 产品状态查询控制器
 * 参考toyou系统的TDeviceController实现
 */
@RestController
@RequestMapping("/api/product-status")
@CrossOrigin
@Api(tags = "产品状态查询API", description = "简化版产品状态查询接口")
@Slf4j
public class ProductStatusController {

    @Autowired
    private ProductStatusService productStatusService;

    /**
     * 根据LbsId查询产品状态
     * 参考toyou系统: /TDeviceFeign/TDeviceManage/queryByLbsId
     *
     * @param request 查询请求
     * @return 产品状态信息
     */
    @PostMapping("/queryByLbsId")
    @ApiOperation(value = "根据LbsId查询产品状态", notes = "根据基站ID查询设备的详细状态信息")
    public BaseResponseDTO<ProductStatusResponseDTO> queryByLbsId(@RequestBody ProductStatusRequestDTO request) {
        log.info("收到产品状态查询请求 - Controller层, lbsId: {}, 客户端IP: {}",
                request != null ? request.getLbsId() : "null",
                getClientIpAddress());

        long startTime = System.currentTimeMillis();

        try {
            // 记录请求详细信息
            log.debug("请求详细信息: {}", request);

            // 调用业务服务
            BaseResponseDTO<ProductStatusResponseDTO> response = productStatusService.queryProductStatusByLbsId(request);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 记录响应信息
            if (response.getSuccess()) {
                log.info("产品状态查询成功 - Controller层, lbsId: {}, 耗时: {}ms, 设备状态: {}",
                        request.getLbsId(),
                        duration,
                        response.getData() != null ? response.getData().getStatus() : "未知");
            } else {
                log.error("产品状态查询失败 - Controller层, lbsId: {}, 耗时: {}ms, 错误: {}",
                        request.getLbsId(),
                        duration,
                        response.getMessage());
            }

            return response;

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.error("产品状态查询异常 - Controller层, lbsId: {}, 耗时: {}ms",
                    request != null ? request.getLbsId() : "null",
                    duration, e);

            return BaseResponseDTO.error(500, "系统异常: " + e.getMessage());
        }
    }

    /**
     * 批量查询产品状态（扩展功能）
     *
     * @param lbsIds 基站ID列表，逗号分隔
     * @return 产品状态列表
     */
    @GetMapping("/batchQuery")
    @ApiOperation(value = "批量查询产品状态", notes = "根据多个基站ID批量查询设备状态")
    public BaseResponseDTO<java.util.List<ProductStatusResponseDTO>> batchQuery(@RequestParam String lbsIds) {
        log.info("收到批量产品状态查询请求 - Controller层, lbsIds: {}", lbsIds);

        long startTime = System.currentTimeMillis();

        try {
            if (lbsIds == null || lbsIds.trim().isEmpty()) {
                log.error("批量查询参数为空");
                return BaseResponseDTO.error(400, "lbsIds参数不能为空");
            }

            String[] lbsIdArray = lbsIds.split(",");
            java.util.List<ProductStatusResponseDTO> results = new java.util.ArrayList<>();

            log.debug("开始批量查询，数量: {}", lbsIdArray.length);

            for (String lbsId : lbsIdArray) {
                if (lbsId != null && !lbsId.trim().isEmpty()) {
                    ProductStatusRequestDTO request = new ProductStatusRequestDTO();
                    request.setLbsId(lbsId.trim());

                    BaseResponseDTO<ProductStatusResponseDTO> response = productStatusService.queryProductStatusByLbsId(request);

                    if (response.getSuccess() && response.getData() != null) {
                        results.add(response.getData());
                    } else {
                        log.warn("批量查询中单个lbsId查询失败: {}, 错误: {}", lbsId, response.getMessage());
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.info("批量产品状态查询完成 - Controller层, 查询数量: {}, 成功数量: {}, 耗时: {}ms",
                    lbsIdArray.length, results.size(), duration);

            return BaseResponseDTO.success(results);

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.error("批量产品状态查询异常 - Controller层, lbsIds: {}, 耗时: {}ms", lbsIds, duration, e);
            return BaseResponseDTO.error(500, "系统异常: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    @ApiOperation(value = "健康检查", notes = "检查服务是否正常运行")
    public BaseResponseDTO<String> health() {
        log.debug("收到健康检查请求");
        return BaseResponseDTO.success("产品状态查询服务运行正常");
    }

    /**
     * 获取客户端IP地址（用于日志记录）
     */
    private String getClientIpAddress() {
        // 简化实现，实际项目中可以从HttpServletRequest获取
        return "127.0.0.1";
    }
}