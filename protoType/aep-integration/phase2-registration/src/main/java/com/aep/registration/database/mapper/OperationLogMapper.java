package com.aep.registration.database.mapper;

import com.aep.registration.database.entity.OperationLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 操作日志数据访问映射器
 *
 * 功能：
 * - 操作日志的CRUD操作
 * - 审计查询和统计
 * - 性能监控数据
 * - 日志清理维护
 *
 * @author AEP Integration Team
 * @version 1.0.0
 * @since 2026-01-25
 */
@Mapper
public interface OperationLogMapper {

    // ==================== 基础CRUD操作 ====================

    /**
     * 插入操作日志
     *
     * @param log 操作日志对象
     * @return 影响的行数
     */
    @Insert({
        "INSERT INTO aep_operation_logs (",
        "  operation_id, operation_type, resource_type, resource_id, resource_name,",
        "  operation_status, error_code, error_message, request_params, response_data,",
        "  execution_time_ms, start_time, end_time, operator, client_ip, user_agent",
        ") VALUES (",
        "  #{operationId}, #{operationType}, #{resourceType}, #{resourceId}, #{resourceName},",
        "  #{operationStatus}, #{errorCode}, #{errorMessage}, #{requestParams}, #{responseData},",
        "  #{executionTimeMs}, #{startTime}, #{endTime}, #{operator}, #{clientIp}, #{userAgent}",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationLog log);

    /**
     * 根据ID查询操作日志
     *
     * @param id 主键ID
     * @return 操作日志对象
     */
    @Select("SELECT * FROM aep_operation_logs WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "operationId", column = "operation_id"),
        @Result(property = "operationType", column = "operation_type"),
        @Result(property = "resourceType", column = "resource_type"),
        @Result(property = "resourceId", column = "resource_id"),
        @Result(property = "resourceName", column = "resource_name"),
        @Result(property = "operationStatus", column = "operation_status"),
        @Result(property = "errorCode", column = "error_code"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "requestParams", column = "request_params"),
        @Result(property = "responseData", column = "response_data"),
        @Result(property = "executionTimeMs", column = "execution_time_ms"),
        @Result(property = "startTime", column = "start_time"),
        @Result(property = "endTime", column = "end_time"),
        @Result(property = "operator", column = "operator"),
        @Result(property = "clientIp", column = "client_ip"),
        @Result(property = "userAgent", column = "user_agent")
    })
    Optional<OperationLog> findById(@Param("id") Long id);

    /**
     * 根据操作ID查询
     *
     * @param operationId 操作唯一标识
     * @return 操作日志对象
     */
    @Select("SELECT * FROM aep_operation_logs WHERE operation_id = #{operationId}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "operationId", column = "operation_id"),
        @Result(property = "operationType", column = "operation_type"),
        @Result(property = "resourceType", column = "resource_type"),
        @Result(property = "resourceId", column = "resource_id"),
        @Result(property = "resourceName", column = "resource_name"),
        @Result(property = "operationStatus", column = "operation_status"),
        @Result(property = "errorCode", column = "error_code"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "requestParams", column = "request_params"),
        @Result(property = "responseData", column = "response_data"),
        @Result(property = "executionTimeMs", column = "execution_time_ms"),
        @Result(property = "startTime", column = "start_time"),
        @Result(property = "endTime", column = "end_time"),
        @Result(property = "operator", column = "operator"),
        @Result(property = "clientIp", column = "client_ip"),
        @Result(property = "userAgent", column = "user_agent")
    })
    Optional<OperationLog> findByOperationId(@Param("operationId") String operationId);

    /**
     * 更新操作日志
     *
     * @param log 操作日志对象
     * @return 影响的行数
     */
    @Update({
        "UPDATE aep_operation_logs SET",
        "  operation_status = #{operationStatus},",
        "  error_code = #{errorCode},",
        "  error_message = #{errorMessage},",
        "  response_data = #{responseData},",
        "  execution_time_ms = #{executionTimeMs},",
        "  end_time = #{endTime}",
        "WHERE id = #{id}"
    })
    int update(OperationLog log);

    /**
     * 删除操作日志
     *
     * @param id 主键ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM aep_operation_logs WHERE id = #{id}")
    int delete(@Param("id") Long id);

    // ==================== 查询操作 ====================

    /**
     * 查询最近的操作日志
     *
     * @param limit 限制数量
     * @return 操作日志列表
     */
    @Select({
        "SELECT * FROM aep_operation_logs",
        "ORDER BY start_time DESC",
        "LIMIT #{limit}"
    })
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "operationId", column = "operation_id"),
        @Result(property = "operationType", column = "operation_type"),
        @Result(property = "resourceType", column = "resource_type"),
        @Result(property = "resourceId", column = "resource_id"),
        @Result(property = "resourceName", column = "resource_name"),
        @Result(property = "operationStatus", column = "operation_status"),
        @Result(property = "errorCode", column = "error_code"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "requestParams", column = "request_params"),
        @Result(property = "responseData", column = "response_data"),
        @Result(property = "executionTimeMs", column = "execution_time_ms"),
        @Result(property = "startTime", column = "start_time"),
        @Result(property = "endTime", column = "end_time"),
        @Result(property = "operator", column = "operator"),
        @Result(property = "clientIp", column = "client_ip"),
        @Result(property = "userAgent", column = "user_agent")
    })
    List<OperationLog> findRecent(@Param("limit") int limit);

    /**
     * 根据操作类型查询
     *
     * @param operationType 操作类型
     * @param limit 限制数量
     * @return 操作日志列表
     */
    @Select({
        "SELECT * FROM aep_operation_logs",
        "WHERE operation_type = #{operationType}",
        "ORDER BY start_time DESC",
        "LIMIT #{limit}"
    })
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "operationId", column = "operation_id"),
        @Result(property = "operationType", column = "operation_type"),
        @Result(property = "resourceType", column = "resource_type"),
        @Result(property = "resourceId", column = "resource_id"),
        @Result(property = "resourceName", column = "resource_name"),
        @Result(property = "operationStatus", column = "operation_status"),
        @Result(property = "errorCode", column = "error_code"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "requestParams", column = "request_params"),
        @Result(property = "responseData", column = "response_data"),
        @Result(property = "executionTimeMs", column = "execution_time_ms"),
        @Result(property = "startTime", column = "start_time"),
        @Result(property = "endTime", column = "end_time"),
        @Result(property = "operator", column = "operator"),
        @Result(property = "clientIp", column = "client_ip"),
        @Result(property = "userAgent", column = "user_agent")
    })
    List<OperationLog> findByOperationType(@Param("operationType") String operationType, @Param("limit") int limit);

    /**
     * 根据操作状态查询
     *
     * @param operationStatus 操作状态
     * @param limit 限制数量
     * @return 操作日志列表
     */
    @Select({
        "SELECT * FROM aep_operation_logs",
        "WHERE operation_status = #{operationStatus}",
        "ORDER BY start_time DESC",
        "LIMIT #{limit}"
    })
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "operationId", column = "operation_id"),
        @Result(property = "operationType", column = "operation_type"),
        @Result(property = "resourceType", column = "resource_type"),
        @Result(property = "resourceId", column = "resource_id"),
        @Result(property = "resourceName", column = "resource_name"),
        @Result(property = "operationStatus", column = "operation_status"),
        @Result(property = "errorCode", column = "error_code"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "requestParams", column = "request_params"),
        @Result(property = "responseData", column = "response_data"),
        @Result(property = "executionTimeMs", column = "execution_time_ms"),
        @Result(property = "startTime", column = "start_time"),
        @Result(property = "endTime", column = "end_time"),
        @Result(property = "operator", column = "operator"),
        @Result(property = "clientIp", column = "client_ip"),
        @Result(property = "userAgent", column = "user_agent")
    })
    List<OperationLog> findByStatus(@Param("operationStatus") String operationStatus, @Param("limit") int limit);

    /**
     * 根据时间范围查询
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作日志列表
     */
    @Select({
        "SELECT * FROM aep_operation_logs",
        "WHERE start_time >= #{startTime} AND start_time <= #{endTime}",
        "ORDER BY start_time DESC"
    })
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "operationId", column = "operation_id"),
        @Result(property = "operationType", column = "operation_type"),
        @Result(property = "resourceType", column = "resource_type"),
        @Result(property = "resourceId", column = "resource_id"),
        @Result(property = "resourceName", column = "resource_name"),
        @Result(property = "operationStatus", column = "operation_status"),
        @Result(property = "errorCode", column = "error_code"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "requestParams", column = "request_params"),
        @Result(property = "responseData", column = "response_data"),
        @Result(property = "executionTimeMs", column = "execution_time_ms"),
        @Result(property = "startTime", column = "start_time"),
        @Result(property = "endTime", column = "end_time"),
        @Result(property = "operator", column = "operator"),
        @Result(property = "clientIp", column = "client_ip"),
        @Result(property = "userAgent", column = "user_agent")
    })
    List<OperationLog> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 分页查询操作日志
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 操作日志列表
     */
    @Select({
        "SELECT * FROM aep_operation_logs",
        "ORDER BY start_time DESC",
        "LIMIT #{limit} OFFSET #{offset}"
    })
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "operationId", column = "operation_id"),
        @Result(property = "operationType", column = "operation_type"),
        @Result(property = "resourceType", column = "resource_type"),
        @Result(property = "resourceId", column = "resource_id"),
        @Result(property = "resourceName", column = "resource_name"),
        @Result(property = "operationStatus", column = "operation_status"),
        @Result(property = "errorCode", column = "error_code"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "requestParams", column = "request_params"),
        @Result(property = "responseData", column = "response_data"),
        @Result(property = "executionTimeMs", column = "execution_time_ms"),
        @Result(property = "startTime", column = "start_time"),
        @Result(property = "endTime", column = "end_time"),
        @Result(property = "operator", column = "operator"),
        @Result(property = "clientIp", column = "client_ip"),
        @Result(property = "userAgent", column = "user_agent")
    })
    List<OperationLog> findWithPagination(@Param("offset") int offset, @Param("limit") int limit);

    // ==================== 统计操作 ====================

    /**
     * 统计总日志数量
     *
     * @return 日志数量
     */
    @Select("SELECT COUNT(*) FROM aep_operation_logs")
    long count();

    /**
     * 根据操作状态统计数量
     *
     * @param operationStatus 操作状态
     * @return 日志数量
     */
    @Select("SELECT COUNT(*) FROM aep_operation_logs WHERE operation_status = #{operationStatus}")
    long countByStatus(@Param("operationStatus") String operationStatus);

    /**
     * 根据操作类型统计数量
     *
     * @param operationType 操作类型
     * @return 日志数量
     */
    @Select("SELECT COUNT(*) FROM aep_operation_logs WHERE operation_type = #{operationType}")
    long countByOperationType(@Param("operationType") String operationType);

    /**
     * 统计指定时间范围内的操作数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志数量
     */
    @Select({
        "SELECT COUNT(*) FROM aep_operation_logs",
        "WHERE start_time >= #{startTime} AND start_time <= #{endTime}"
    })
    long countByTimeRange(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    /**
     * 获取操作统计概要
     *
     * @return 统计结果 Map
     */
    @Select({
        "SELECT",
        "  operation_type,",
        "  operation_status,",
        "  COUNT(*) as count,",
        "  AVG(execution_time_ms) as avg_execution_time,",
        "  MAX(execution_time_ms) as max_execution_time,",
        "  MIN(execution_time_ms) as min_execution_time",
        "FROM aep_operation_logs",
        "WHERE start_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)",
        "GROUP BY operation_type, operation_status",
        "ORDER BY operation_type, operation_status"
    })
    List<java.util.Map<String, Object>> getOperationSummary();

    /**
     * 获取性能统计数据
     *
     * @param hours 小时数
     * @return 性能统计结果
     */
    @Select({
        "SELECT",
        "  operation_type,",
        "  COUNT(*) as total_operations,",
        "  AVG(execution_time_ms) as avg_time,",
        "  MAX(execution_time_ms) as max_time,",
        "  MIN(execution_time_ms) as min_time,",
        "  SUM(CASE WHEN operation_status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count,",
        "  SUM(CASE WHEN operation_status = 'FAILED' THEN 1 ELSE 0 END) as failed_count",
        "FROM aep_operation_logs",
        "WHERE start_time >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR)",
        "GROUP BY operation_type",
        "ORDER BY total_operations DESC"
    })
    List<java.util.Map<String, Object>> getPerformanceStats(@Param("hours") int hours);

    // ==================== 维护操作 ====================

    /**
     * 清理指定日期之前的日志
     *
     * @param beforeDate 截止日期
     * @return 删除的记录数
     */
    @Delete({
        "DELETE FROM aep_operation_logs",
        "WHERE start_time < #{beforeDate}"
    })
    int deleteBeforeDate(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * 清理指定天数之前的日志
     *
     * @param days 天数
     * @return 删除的记录数
     */
    @Delete({
        "DELETE FROM aep_operation_logs",
        "WHERE start_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)"
    })
    int cleanupOldLogs(@Param("days") int days);

    // ==================== 批量操作 ====================

    /**
     * 批量插入操作日志
     *
     * @param logs 日志列表
     * @return 影响的行数
     */
    @Insert({
        "<script>",
        "INSERT INTO aep_operation_logs (",
        "  operation_id, operation_type, resource_type, resource_id, resource_name,",
        "  operation_status, error_code, error_message, request_params, response_data,",
        "  execution_time_ms, start_time, end_time, operator, client_ip, user_agent",
        ") VALUES",
        "<foreach collection='logs' item='log' separator=','>",
        "  (",
        "    #{log.operationId}, #{log.operationType}, #{log.resourceType},",
        "    #{log.resourceId}, #{log.resourceName}, #{log.operationStatus},",
        "    #{log.errorCode}, #{log.errorMessage}, #{log.requestParams},",
        "    #{log.responseData}, #{log.executionTimeMs}, #{log.startTime},",
        "    #{log.endTime}, #{log.operator}, #{log.clientIp}, #{log.userAgent}",
        "  )",
        "</foreach>",
        "</script>"
    })
    int batchInsert(@Param("logs") List<OperationLog> logs);

    /**
     * 根据操作ID批量查询
     *
     * @param operationIds 操作ID列表
     * @return 操作日志列表
     */
    @Select({
        "<script>",
        "SELECT * FROM aep_operation_logs",
        "WHERE operation_id IN",
        "<foreach collection='operationIds' item='operationId' open='(' separator=',' close=')'>",
        "  #{operationId}",
        "</foreach>",
        "ORDER BY start_time DESC",
        "</script>"
    })
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "operationId", column = "operation_id"),
        @Result(property = "operationType", column = "operation_type"),
        @Result(property = "resourceType", column = "resource_type"),
        @Result(property = "resourceId", column = "resource_id"),
        @Result(property = "resourceName", column = "resource_name"),
        @Result(property = "operationStatus", column = "operation_status"),
        @Result(property = "errorCode", column = "error_code"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "requestParams", column = "request_params"),
        @Result(property = "responseData", column = "response_data"),
        @Result(property = "executionTimeMs", column = "execution_time_ms"),
        @Result(property = "startTime", column = "start_time"),
        @Result(property = "endTime", column = "end_time"),
        @Result(property = "operator", column = "operator"),
        @Result(property = "clientIp", column = "client_ip"),
        @Result(property = "userAgent", column = "user_agent")
    })
    List<OperationLog> findByOperationIds(@Param("operationIds") List<String> operationIds);
}