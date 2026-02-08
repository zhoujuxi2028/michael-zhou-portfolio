package com.zct.poc.service;

import com.zct.poc.dto.BaseResponseDTO;
import com.zct.poc.dto.ProductStatusRequestDTO;
import com.zct.poc.dto.ProductStatusResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 产品状态服务测试
 * 验证核心业务逻辑
 */
@SpringBootTest
public class ProductStatusServiceTest {

    @Autowired
    private ProductStatusService productStatusService;

    private ProductStatusRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ProductStatusRequestDTO();
        validRequest.setLbsId("test_station_001");
        validRequest.setProjectId("test_project");
        validRequest.setCompanyId("test_company");
    }

    @Test
    void testQueryProductStatusByLbsId_Success() {
        // 执行查询
        BaseResponseDTO<ProductStatusResponseDTO> response =
                productStatusService.queryProductStatusByLbsId(validRequest);

        // 验证响应
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());

        // 验证设备信息
        ProductStatusResponseDTO deviceInfo = response.getData();
        assertEquals(validRequest.getLbsId(), deviceInfo.getLbsId());
        assertNotNull(deviceInfo.getId());
        assertNotNull(deviceInfo.getDeviceName());
        assertNotNull(deviceInfo.getStatus());
        assertNotNull(deviceInfo.getStatusDescription());
    }

    @Test
    void testQueryProductStatusByLbsId_InvalidRequest_NullLbsId() {
        // 测试空lbsId
        ProductStatusRequestDTO invalidRequest = new ProductStatusRequestDTO();
        invalidRequest.setLbsId(null);

        BaseResponseDTO<ProductStatusResponseDTO> response =
                productStatusService.queryProductStatusByLbsId(invalidRequest);

        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals(400, response.getCode());
        assertNull(response.getData());
    }

    @Test
    void testQueryProductStatusByLbsId_InvalidRequest_EmptyLbsId() {
        // 测试空字符串lbsId
        ProductStatusRequestDTO invalidRequest = new ProductStatusRequestDTO();
        invalidRequest.setLbsId("");

        BaseResponseDTO<ProductStatusResponseDTO> response =
                productStatusService.queryProductStatusByLbsId(invalidRequest);

        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals(400, response.getCode());
    }

    @Test
    void testQueryProductStatusByLbsId_NullRequest() {
        // 测试null请求
        BaseResponseDTO<ProductStatusResponseDTO> response =
                productStatusService.queryProductStatusByLbsId(null);

        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals(400, response.getCode());
    }

    @Test
    void testStatusCodeGeneration() {
        // 测试不同lbsId生成的状态码
        String[] testLbsIds = {"station001", "station002", "station003", "station004", "station005"};

        for (String lbsId : testLbsIds) {
            ProductStatusRequestDTO request = new ProductStatusRequestDTO();
            request.setLbsId(lbsId);

            BaseResponseDTO<ProductStatusResponseDTO> response =
                    productStatusService.queryProductStatusByLbsId(request);

            assertTrue(response.getSuccess());
            ProductStatusResponseDTO deviceInfo = response.getData();

            // 验证状态码格式
            assertNotNull(deviceInfo.getStatus());
            assertEquals(11, deviceInfo.getStatus().length()); // 11位状态码
            assertTrue(deviceInfo.getStatus().matches("[01]+"));  // 只包含0和1

            // 验证在线状态逻辑
            boolean expectedOnline = !(deviceInfo.getStatus().substring(0, 1).equals("0") ||
                    deviceInfo.getStatus().substring(1, 2).equals("0"));
            assertEquals(expectedOnline ? 1 : 0, deviceInfo.getOnlineStatus());

            System.out.println("LbsId: " + lbsId +
                    ", Status: " + deviceInfo.getStatus() +
                    ", Online: " + deviceInfo.getOnlineStatus() +
                    ", Description: " + deviceInfo.getStatusDescription());
        }
    }

    @Test
    void testDeviceInfoEnrichment() {
        // 测试设备信息补充逻辑
        BaseResponseDTO<ProductStatusResponseDTO> response =
                productStatusService.queryProductStatusByLbsId(validRequest);

        ProductStatusResponseDTO deviceInfo = response.getData();

        // 验证基础信息
        assertNotNull(deviceInfo.getDeviceName());
        assertNotNull(deviceInfo.getModel());
        assertNotNull(deviceInfo.getDeviceType());

        // 验证项目信息
        assertNotNull(deviceInfo.getProjectName());

        // 验证公司信息
        assertNotNull(deviceInfo.getCompanyName());

        // 验证升级状态
        assertNotNull(deviceInfo.getUpgradeStatus());
        assertNotNull(deviceInfo.getUpgradeStatusName());

        // 验证时间戳
        assertNotNull(deviceInfo.getLastReportTime());
        assertNotNull(deviceInfo.getCreateTime());
        assertNotNull(deviceInfo.getUpdateTime());
    }
}