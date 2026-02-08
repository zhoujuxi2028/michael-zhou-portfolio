package com.zct.poc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zct.poc.dto.ProductStatusRequestDTO;
import com.zct.poc.service.ProductStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 产品状态查询控制器测试
 * 参考toyou系统的测试结构
 */
@WebMvcTest(ProductStatusController.class)
public class ProductStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductStatusService productStatusService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductStatusRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ProductStatusRequestDTO();
        validRequest.setLbsId("station001");
        validRequest.setProjectId("project_001");
        validRequest.setCompanyId("company_001");
    }

    @Test
    void testQueryByLbsId_Success() throws Exception {
        // 执行测试
        mockMvc.perform(post("/api/product-status/queryByLbsId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testQueryByLbsId_InvalidRequest() throws Exception {
        // 测试无效请求
        ProductStatusRequestDTO invalidRequest = new ProductStatusRequestDTO();
        // lbsId为空

        mockMvc.perform(post("/api/product-status/queryByLbsId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testBatchQuery_Success() throws Exception {
        mockMvc.perform(get("/api/product-status/batchQuery")
                        .param("lbsIds", "station001,station002,station003"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/product-status/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("产品状态查询服务运行正常"));
    }
}