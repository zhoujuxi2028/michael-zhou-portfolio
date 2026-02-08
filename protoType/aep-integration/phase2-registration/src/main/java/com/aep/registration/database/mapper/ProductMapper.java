package com.aep.registration.database.mapper;

import com.aep.registration.database.entity.AepProduct;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AEP产品数据访问映射器
 *
 * 功能：
 * - 产品的CRUD操作
 * - 复杂查询和统计
 * - 批量操作支持
 * - 事务管理
 *
 * 设计原则：
 * - 使用MyBatis注解方式定义SQL
 * - 支持分页查询
 * - 提供安全的参数绑定
 * - 优化查询性能
 *
 * @author AEP Integration Team
 * @version 1.0.0
 * @since 2026-01-25
 */
@Mapper
public interface ProductMapper {

    // ==================== 基础CRUD操作 ====================

    /**
     * 插入新产品
     *
     * @param product 产品对象
     * @return 影响的行数
     */
    @Insert({
        "INSERT INTO aep_products (",
        "  product_id, product_name, device_type, network_type, data_format,",
        "  industry_id, description, device_model, manufacturer, protocol_type,",
        "  max_device_count, enable_security, auto_create_device, master_key,",
        "  status, created_at, updated_at, created_by",
        ") VALUES (",
        "  #{productId}, #{productName}, #{deviceType}, #{networkType}, #{dataFormat},",
        "  #{industryId}, #{description}, #{deviceModel}, #{manufacturer}, #{protocolType},",
        "  #{maxDeviceCount}, #{enableSecurity}, #{autoCreateDevice}, #{masterKey},",
        "  #{status}, #{createdAt}, #{updatedAt}, #{createdBy}",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AepProduct product);

    /**
     * 根据ID查询产品
     *
     * @param id 主键ID
     * @return 产品对象，如果不存在返回null
     */
    @Select({
        "SELECT * FROM aep_products WHERE id = #{id}"
    })
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "productId", column = "product_id"),
        @Result(property = "productName", column = "product_name"),
        @Result(property = "deviceType", column = "device_type"),
        @Result(property = "networkType", column = "network_type"),
        @Result(property = "dataFormat", column = "data_format"),
        @Result(property = "industryId", column = "industry_id"),
        @Result(property = "description", column = "description"),
        @Result(property = "deviceModel", column = "device_model"),
        @Result(property = "manufacturer", column = "manufacturer"),
        @Result(property = "protocolType", column = "protocol_type"),
        @Result(property = "maxDeviceCount", column = "max_device_count"),
        @Result(property = "enableSecurity", column = "enable_security"),
        @Result(property = "autoCreateDevice", column = "auto_create_device"),
        @Result(property = "masterKey", column = "master_key"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "createdBy", column = "created_by")
    })
    Optional<AepProduct> findById(@Param("id") Long id);

    /**
     * 根据AEP产品ID查询
     *
     * @param productId AEP平台产品ID
     * @return 产品对象，如果不存在返回null
     */
    @Select({
        "SELECT * FROM aep_products WHERE product_id = #{productId}"
    })
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "productId", column = "product_id"),
        @Result(property = "productName", column = "product_name"),
        @Result(property = "deviceType", column = "device_type"),
        @Result(property = "networkType", column = "network_type"),
        @Result(property = "dataFormat", column = "data_format"),
        @Result(property = "industryId", column = "industry_id"),
        @Result(property = "description", column = "description"),
        @Result(property = "deviceModel", column = "device_model"),
        @Result(property = "manufacturer", column = "manufacturer"),
        @Result(property = "protocolType", column = "protocol_type"),
        @Result(property = "maxDeviceCount", column = "max_device_count"),
        @Result(property = "enableSecurity", column = "enable_security"),
        @Result(property = "autoCreateDevice", column = "auto_create_device"),
        @Result(property = "masterKey", column = "master_key"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "createdBy", column = "created_by")
    })
    Optional<AepProduct> findByProductId(@Param("productId") Long productId);

    /**
     * 根据产品名称查询（活跃状态）
     *
     * @param productName 产品名称
     * @return 产品对象，如果不存在返回null
     */
    @Select({
        "SELECT * FROM aep_products",
        "WHERE product_name = #{productName} AND status = 'ACTIVE'"
    })
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "productId", column = "product_id"),
        @Result(property = "productName", column = "product_name"),
        @Result(property = "deviceType", column = "device_type"),
        @Result(property = "networkType", column = "network_type"),
        @Result(property = "dataFormat", column = "data_format"),
        @Result(property = "industryId", column = "industry_id"),
        @Result(property = "description", column = "description"),
        @Result(property = "deviceModel", column = "device_model"),
        @Result(property = "manufacturer", column = "manufacturer"),
        @Result(property = "protocolType", column = "protocol_type"),
        @Result(property = "maxDeviceCount", column = "max_device_count"),
        @Result(property = "enableSecurity", column = "enable_security"),
        @Result(property = "autoCreateDevice", column = "auto_create_device"),
        @Result(property = "masterKey", column = "master_key"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "createdBy", column = "created_by")
    })
    Optional<AepProduct> findByProductName(@Param("productName") String productName);

    /**
     * 更新产品信息
     *
     * @param product 产品对象
     * @return 影响的行数
     */
    @Update({
        "UPDATE aep_products SET",
        "  product_name = #{productName},",
        "  device_type = #{deviceType},",
        "  network_type = #{networkType},",
        "  data_format = #{dataFormat},",
        "  industry_id = #{industryId},",
        "  description = #{description},",
        "  device_model = #{deviceModel},",
        "  manufacturer = #{manufacturer},",
        "  protocol_type = #{protocolType},",
        "  max_device_count = #{maxDeviceCount},",
        "  enable_security = #{enableSecurity},",
        "  auto_create_device = #{autoCreateDevice},",
        "  master_key = #{masterKey},",
        "  status = #{status},",
        "  updated_at = #{updatedAt}",
        "WHERE id = #{id}"
    })
    int update(AepProduct product);

    /**
     * 软删除产品（更新状态为DELETED）
     *
     * @param id 主键ID
     * @return 影响的行数
     */
    @Update({
        "UPDATE aep_products SET status = 'DELETED', updated_at = NOW()",
        "WHERE id = #{id}"
    })
    int softDelete(@Param("id") Long id);

    /**
     * 物理删除产品
     *
     * @param id 主键ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM aep_products WHERE id = #{id}")
    int delete(@Param("id") Long id);

    // ==================== 查询操作 ====================

    /**
     * 查询所有活跃产品
     *
     * @return 产品列表
     */
    @Select({
        "SELECT * FROM aep_products",
        "WHERE status = 'ACTIVE'",
        "ORDER BY created_at DESC"
    })
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "productId", column = "product_id"),
        @Result(property = "productName", column = "product_name"),
        @Result(property = "deviceType", column = "device_type"),
        @Result(property = "networkType", column = "network_type"),
        @Result(property = "dataFormat", column = "data_format"),
        @Result(property = "industryId", column = "industry_id"),
        @Result(property = "description", column = "description"),
        @Result(property = "deviceModel", column = "device_model"),
        @Result(property = "manufacturer", column = "manufacturer"),
        @Result(property = "protocolType", column = "protocol_type"),
        @Result(property = "maxDeviceCount", column = "max_device_count"),
        @Result(property = "enableSecurity", column = "enable_security"),
        @Result(property = "autoCreateDevice", column = "auto_create_device"),
        @Result(property = "masterKey", column = "master_key"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "createdBy", column = "created_by")
    })
    List<AepProduct> findAllActive();

    /**
     * 根据设备类型查询产品
     *
     * @param deviceType 设备类型
     * @return 产品列表
     */
    @Select({
        "SELECT * FROM aep_products",
        "WHERE device_type = #{deviceType} AND status = 'ACTIVE'",
        "ORDER BY created_at DESC"
    })
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "productId", column = "product_id"),
        @Result(property = "productName", column = "product_name"),
        @Result(property = "deviceType", column = "device_type"),
        @Result(property = "networkType", column = "network_type"),
        @Result(property = "dataFormat", column = "data_format"),
        @Result(property = "industryId", column = "industry_id"),
        @Result(property = "description", column = "description"),
        @Result(property = "deviceModel", column = "device_model"),
        @Result(property = "manufacturer", column = "manufacturer"),
        @Result(property = "protocolType", column = "protocol_type"),
        @Result(property = "maxDeviceCount", column = "max_device_count"),
        @Result(property = "enableSecurity", column = "enable_security"),
        @Result(property = "autoCreateDevice", column = "auto_create_device"),
        @Result(property = "masterKey", column = "master_key"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "createdBy", column = "created_by")
    })
    List<AepProduct> findByDeviceType(@Param("deviceType") String deviceType);

    /**
     * 分页查询产品
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 产品列表
     */
    @Select({
        "SELECT * FROM aep_products",
        "WHERE status = 'ACTIVE'",
        "ORDER BY created_at DESC",
        "LIMIT #{limit} OFFSET #{offset}"
    })
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "productId", column = "product_id"),
        @Result(property = "productName", column = "product_name"),
        @Result(property = "deviceType", column = "device_type"),
        @Result(property = "networkType", column = "network_type"),
        @Result(property = "dataFormat", column = "data_format"),
        @Result(property = "industryId", column = "industry_id"),
        @Result(property = "description", column = "description"),
        @Result(property = "deviceModel", column = "device_model"),
        @Result(property = "manufacturer", column = "manufacturer"),
        @Result(property = "protocolType", column = "protocol_type"),
        @Result(property = "maxDeviceCount", column = "max_device_count"),
        @Result(property = "enableSecurity", column = "enable_security"),
        @Result(property = "autoCreateDevice", column = "auto_create_device"),
        @Result(property = "masterKey", column = "master_key"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "createdBy", column = "created_by")
    })
    List<AepProduct> findWithPagination(@Param("offset") int offset, @Param("limit") int limit);

    // ==================== 统计操作 ====================

    /**
     * 统计活跃产品总数
     *
     * @return 产品数量
     */
    @Select("SELECT COUNT(*) FROM aep_products WHERE status = 'ACTIVE'")
    long countActive();

    /**
     * 根据设备类型统计产品数量
     *
     * @param deviceType 设备类型
     * @return 产品数量
     */
    @Select({
        "SELECT COUNT(*) FROM aep_products",
        "WHERE device_type = #{deviceType} AND status = 'ACTIVE'"
    })
    long countByDeviceType(@Param("deviceType") String deviceType);

    /**
     * 统计各状态的产品数量
     *
     * @return 状态统计结果 Map<status, count>
     */
    @Select({
        "SELECT status, COUNT(*) as count FROM aep_products",
        "GROUP BY status"
    })
    @MapKey("status")
    java.util.Map<String, Object> countByStatus();

    // ==================== 业务查询 ====================

    /**
     * 检查产品名称是否已存在（活跃状态）
     *
     * @param productName 产品名称
     * @return 是否存在
     */
    @Select({
        "SELECT COUNT(*) FROM aep_products",
        "WHERE product_name = #{productName} AND status = 'ACTIVE'"
    })
    boolean existsByProductName(@Param("productName") String productName);

    /**
     * 检查AEP产品ID是否已存在
     *
     * @param productId AEP产品ID
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) FROM aep_products WHERE product_id = #{productId}")
    boolean existsByProductId(@Param("productId") Long productId);

    /**
     * 查询最近创建的产品
     *
     * @param days 天数
     * @param limit 限制数量
     * @return 产品列表
     */
    @Select({
        "SELECT * FROM aep_products",
        "WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)",
        "AND status = 'ACTIVE'",
        "ORDER BY created_at DESC",
        "LIMIT #{limit}"
    })
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "productId", column = "product_id"),
        @Result(property = "productName", column = "product_name"),
        @Result(property = "deviceType", column = "device_type"),
        @Result(property = "networkType", column = "network_type"),
        @Result(property = "dataFormat", column = "data_format"),
        @Result(property = "industryId", column = "industry_id"),
        @Result(property = "description", column = "description"),
        @Result(property = "deviceModel", column = "device_model"),
        @Result(property = "manufacturer", column = "manufacturer"),
        @Result(property = "protocolType", column = "protocol_type"),
        @Result(property = "maxDeviceCount", column = "max_device_count"),
        @Result(property = "enableSecurity", column = "enable_security"),
        @Result(property = "autoCreateDevice", column = "auto_create_device"),
        @Result(property = "masterKey", column = "master_key"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "createdBy", column = "created_by")
    })
    List<AepProduct> findRecentlyCreated(@Param("days") int days, @Param("limit") int limit);

    // ==================== 批量操作 ====================

    /**
     * 批量插入产品
     *
     * @param products 产品列表
     * @return 影响的行数
     */
    @Insert({
        "<script>",
        "INSERT INTO aep_products (",
        "  product_id, product_name, device_type, network_type, data_format,",
        "  industry_id, description, device_model, manufacturer, protocol_type,",
        "  max_device_count, enable_security, auto_create_device, master_key,",
        "  status, created_at, updated_at, created_by",
        ") VALUES",
        "<foreach collection='products' item='product' separator=','>",
        "  (",
        "    #{product.productId}, #{product.productName}, #{product.deviceType},",
        "    #{product.networkType}, #{product.dataFormat}, #{product.industryId},",
        "    #{product.description}, #{product.deviceModel}, #{product.manufacturer},",
        "    #{product.protocolType}, #{product.maxDeviceCount}, #{product.enableSecurity},",
        "    #{product.autoCreateDevice}, #{product.masterKey}, #{product.status},",
        "    #{product.createdAt}, #{product.updatedAt}, #{product.createdBy}",
        "  )",
        "</foreach>",
        "</script>"
    })
    int batchInsert(@Param("products") List<AepProduct> products);

    /**
     * 批量更新产品状态
     *
     * @param ids 产品ID列表
     * @param status 目标状态
     * @return 影响的行数
     */
    @Update({
        "<script>",
        "UPDATE aep_products SET status = #{status}, updated_at = NOW()",
        "WHERE id IN",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
        "  #{id}",
        "</foreach>",
        "</script>"
    })
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);
}