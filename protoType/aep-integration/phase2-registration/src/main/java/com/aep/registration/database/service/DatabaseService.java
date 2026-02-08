package com.aep.registration.database.service;

import com.aep.registration.database.DatabaseConfig;
import com.aep.registration.database.entity.AepProduct;
import com.aep.registration.database.entity.OperationLog;
import com.aep.registration.database.mapper.OperationLogMapper;
import com.aep.registration.database.mapper.ProductMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 数据库服务管理类
 *
 * 功能：
 * 1. 统一数据库访问接口
 * 2. 事务管理
 * 3. 连接管理
 * 4. 错误处理和重试
 *
 * 设计原则：
 * - 封装数据库操作细节
 * - 提供简洁的业务接口
 * - 统一异常处理
 * - 支持事务操作
 *
 * @author AEP Integration Team
 * @version 1.0.0
 * @since 2026-01-25
 */
public class DatabaseService {
    private static final Logger logger = Logger.getLogger(DatabaseService.class.getName());

    private static DatabaseService instance;
    private final DatabaseConfig databaseConfig;
    private final SqlSessionFactory sqlSessionFactory;

    private DatabaseService() {
        this.databaseConfig = DatabaseConfig.getInstance();
        this.sqlSessionFactory = databaseConfig.getSqlSessionFactory();
        logger.info("DatabaseService 初始化完成");
    }

    /**
     * 获取数据库服务实例（单例模式）
     */
    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    // ==================== 产品管理服务 ====================

    /**
     * 保存产品信息
     *
     * @param product 产品对象
     * @return 是否成功
     */
    public boolean saveProduct(AepProduct product) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);

            int result;
            if (product.getId() == null) {
                // 新增产品
                result = mapper.insert(product);
                logger.info("新增产品成功: " + product.getProductName() + ", ID: " + product.getId());
            } else {
                // 更新产品
                result = mapper.update(product);
                logger.info("更新产品成功: " + product.getProductName() + ", ID: " + product.getId());
            }

            session.commit();
            return result > 0;

        } catch (Exception e) {
            logger.severe("保存产品失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 根据AEP产品ID查询产品
     *
     * @param productId AEP产品ID
     * @return 产品对象
     */
    public Optional<AepProduct> findProductByAepId(Long productId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            return mapper.findByProductId(productId);
        } catch (Exception e) {
            logger.severe("查询产品失败，ID: " + productId + ", 错误: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 根据产品名称查询产品
     *
     * @param productName 产品名称
     * @return 产品对象
     */
    public Optional<AepProduct> findProductByName(String productName) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            return mapper.findByProductName(productName);
        } catch (Exception e) {
            logger.severe("查询产品失败，名称: " + productName + ", 错误: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 查询所有活跃产品
     *
     * @return 产品列表
     */
    public List<AepProduct> findAllActiveProducts() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            return mapper.findAllActive();
        } catch (Exception e) {
            logger.severe("查询活跃产品失败: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 检查产品名称是否已存在
     *
     * @param productName 产品名称
     * @return 是否存在
     */
    public boolean isProductNameExists(String productName) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            return mapper.existsByProductName(productName);
        } catch (Exception e) {
            logger.severe("检查产品名称存在性失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 软删除产品
     *
     * @param productId 产品ID
     * @return 是否成功
     */
    public boolean deleteProduct(Long productId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            int result = mapper.softDelete(productId);
            session.commit();

            if (result > 0) {
                logger.info("产品软删除成功，ID: " + productId);
                return true;
            } else {
                logger.warning("产品软删除失败，产品不存在，ID: " + productId);
                return false;
            }
        } catch (Exception e) {
            logger.severe("产品软删除失败，ID: " + productId + ", 错误: " + e.getMessage());
            return false;
        }
    }

    // ==================== 操作日志服务 ====================

    /**
     * 记录操作日志
     *
     * @param operationType 操作类型
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param resourceName 资源名称
     * @param operator 操作者
     * @return 操作ID
     */
    public String logOperation(String operationType, String resourceType,
                             String resourceId, String resourceName, String operator) {
        try {
            String operationId = UUID.randomUUID().toString();

            OperationLog log = OperationLog.builder()
                    .operationId(operationId)
                    .operationType(operationType)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .resourceName(resourceName)
                    .operator(operator)
                    .build();

            try (SqlSession session = sqlSessionFactory.openSession()) {
                OperationLogMapper mapper = session.getMapper(OperationLogMapper.class);
                mapper.insert(log);
                session.commit();

                logger.info("操作日志记录成功: " + log.getSummary());
                return operationId;
            }
        } catch (Exception e) {
            logger.severe("记录操作日志失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 更新操作日志状态为成功
     *
     * @param operationId 操作ID
     * @param responseData 响应数据
     * @return 是否成功
     */
    public boolean logOperationSuccess(String operationId, String responseData) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            OperationLogMapper mapper = session.getMapper(OperationLogMapper.class);
            Optional<OperationLog> logOpt = mapper.findByOperationId(operationId);

            if (logOpt.isPresent()) {
                OperationLog log = logOpt.get();
                log.markSuccess();
                log.setResponseData(responseData);

                mapper.update(log);
                session.commit();

                logger.info("操作成功日志更新: " + log.getSummary());
                return true;
            } else {
                logger.warning("未找到操作日志，ID: " + operationId);
                return false;
            }
        } catch (Exception e) {
            logger.severe("更新操作成功日志失败，ID: " + operationId + ", 错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 更新操作日志状态为失败
     *
     * @param operationId 操作ID
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @return 是否成功
     */
    public boolean logOperationFailure(String operationId, String errorCode, String errorMessage) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            OperationLogMapper mapper = session.getMapper(OperationLogMapper.class);
            Optional<OperationLog> logOpt = mapper.findByOperationId(operationId);

            if (logOpt.isPresent()) {
                OperationLog log = logOpt.get();
                log.markFailure(errorCode, errorMessage);

                mapper.update(log);
                session.commit();

                logger.warning("操作失败日志更新: " + log.getSummary());
                return true;
            } else {
                logger.warning("未找到操作日志，ID: " + operationId);
                return false;
            }
        } catch (Exception e) {
            logger.severe("更新操作失败日志失败，ID: " + operationId + ", 错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 查询最近的操作日志
     *
     * @param limit 限制数量
     * @return 操作日志列表
     */
    public List<OperationLog> findRecentOperationLogs(int limit) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            OperationLogMapper mapper = session.getMapper(OperationLogMapper.class);
            return mapper.findRecent(limit);
        } catch (Exception e) {
            logger.severe("查询最近操作日志失败: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 查询失败的操作日志
     *
     * @param limit 限制数量
     * @return 操作日志列表
     */
    public List<OperationLog> findFailedOperations(int limit) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            OperationLogMapper mapper = session.getMapper(OperationLogMapper.class);
            return mapper.findByStatus("FAILED", limit);
        } catch (Exception e) {
            logger.severe("查询失败操作日志失败: " + e.getMessage());
            return List.of();
        }
    }

    // ==================== 统计服务 ====================

    /**
     * 获取产品统计信息
     *
     * @return 统计信息
     */
    public ProductStats getProductStats() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);

            long activeCount = mapper.countActive();
            long sensorCount = mapper.countByDeviceType("SENSOR");
            long gatewayCount = mapper.countByDeviceType("GATEWAY");
            long deviceCount = mapper.countByDeviceType("DEVICE");

            return new ProductStats(activeCount, sensorCount, gatewayCount, deviceCount);
        } catch (Exception e) {
            logger.severe("获取产品统计信息失败: " + e.getMessage());
            return new ProductStats(0, 0, 0, 0);
        }
    }

    /**
     * 获取操作统计信息
     *
     * @return 统计信息
     */
    public OperationStats getOperationStats() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            OperationLogMapper mapper = session.getMapper(OperationLogMapper.class);

            long totalCount = mapper.count();
            long successCount = mapper.countByStatus("SUCCESS");
            long failedCount = mapper.countByStatus("FAILED");
            long pendingCount = mapper.countByStatus("PENDING");

            return new OperationStats(totalCount, successCount, failedCount, pendingCount);
        } catch (Exception e) {
            logger.severe("获取操作统计信息失败: " + e.getMessage());
            return new OperationStats(0, 0, 0, 0);
        }
    }

    // ==================== 健康检查 ====================

    /**
     * 检查数据库连接健康状态
     *
     * @return 健康状态
     */
    public boolean isHealthy() {
        return databaseConfig.isHealthy();
    }

    /**
     * 获取数据库连接池统计信息
     *
     * @return 连接池统计
     */
    public DatabaseConfig.DatabaseStats getDatabaseStats() {
        return databaseConfig.getStats();
    }

    // ==================== 维护操作 ====================

    /**
     * 清理过期操作日志
     *
     * @param retentionDays 保留天数
     * @return 清理的记录数
     */
    public int cleanupOldLogs(int retentionDays) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            OperationLogMapper mapper = session.getMapper(OperationLogMapper.class);
            int deletedCount = mapper.cleanupOldLogs(retentionDays);
            session.commit();

            logger.info("清理过期日志完成，删除记录数: " + deletedCount);
            return deletedCount;
        } catch (Exception e) {
            logger.severe("清理过期日志失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 关闭数据库服务
     */
    public void close() {
        if (databaseConfig != null) {
            databaseConfig.close();
            logger.info("DatabaseService 已关闭");
        }
    }

    // ==================== 内部类 - 统计信息 ====================

    /**
     * 产品统计信息
     */
    public static class ProductStats {
        private final long activeCount;
        private final long sensorCount;
        private final long gatewayCount;
        private final long deviceCount;

        public ProductStats(long activeCount, long sensorCount, long gatewayCount, long deviceCount) {
            this.activeCount = activeCount;
            this.sensorCount = sensorCount;
            this.gatewayCount = gatewayCount;
            this.deviceCount = deviceCount;
        }

        public long getActiveCount() { return activeCount; }
        public long getSensorCount() { return sensorCount; }
        public long getGatewayCount() { return gatewayCount; }
        public long getDeviceCount() { return deviceCount; }

        @Override
        public String toString() {
            return String.format("ProductStats{active=%d, sensor=%d, gateway=%d, device=%d}",
                    activeCount, sensorCount, gatewayCount, deviceCount);
        }
    }

    /**
     * 操作统计信息
     */
    public static class OperationStats {
        private final long totalCount;
        private final long successCount;
        private final long failedCount;
        private final long pendingCount;

        public OperationStats(long totalCount, long successCount, long failedCount, long pendingCount) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.pendingCount = pendingCount;
        }

        public long getTotalCount() { return totalCount; }
        public long getSuccessCount() { return successCount; }
        public long getFailedCount() { return failedCount; }
        public long getPendingCount() { return pendingCount; }

        public double getSuccessRate() {
            return totalCount > 0 ? (double) successCount / totalCount * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("OperationStats{total=%d, success=%d, failed=%d, pending=%d, successRate=%.1f%%}",
                    totalCount, successCount, failedCount, pendingCount, getSuccessRate());
        }
    }
}