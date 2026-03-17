package com.aep.registration.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 数据库配置管理类
 *
 * 功能：
 * 1. 数据库连接池配置
 * 2. MyBatis会话工厂创建
 * 3. 数据库迁移管理
 * 4. 连接健康检查
 *
 * 设计原则：
 * - 单例模式确保全局唯一配置
 * - 支持环境变量和配置文件双重配置
 * - 集成Flyway自动数据库迁移
 * - 提供连接池监控和健康检查
 *
 * @author AEP Integration Team
 * @version 1.0.0
 * @since 2026-01-25
 */
public class DatabaseConfig {
    private static final Logger logger = Logger.getLogger(DatabaseConfig.class.getName());

    private static DatabaseConfig instance;
    private HikariDataSource dataSource;
    private SqlSessionFactory sqlSessionFactory;

    // 数据库配置常量
    private static final String DEFAULT_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final int DEFAULT_POOL_SIZE = 10;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30000L;
    private static final long DEFAULT_IDLE_TIMEOUT = 600000L;
    private static final long DEFAULT_MAX_LIFETIME = 1800000L;

    private DatabaseConfig() {
        initializeDataSource();
        initializeSqlSessionFactory();
        runDatabaseMigration();
    }

    /**
     * 获取数据库配置实例（单例模式）
     */
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    /**
     * 初始化数据源配置
     */
    private void initializeDataSource() {
        try {
            logger.info("初始化数据库连接池配置...");

            HikariConfig config = new HikariConfig();

            // 基础连接配置
            config.setDriverClassName(getConfigValue("DB_DRIVER_CLASS", DEFAULT_DRIVER_CLASS));
            config.setJdbcUrl(buildJdbcUrl());
            config.setUsername(getConfigValue("DB_USERNAME", "aep_user"));
            config.setPassword(getConfigValue("DB_PASSWORD", ""));

            // 连接池配置
            config.setMaximumPoolSize(Integer.parseInt(getConfigValue("DB_POOL_SIZE", String.valueOf(DEFAULT_POOL_SIZE))));
            config.setMinimumIdle(Integer.parseInt(getConfigValue("DB_MIN_IDLE", "2")));
            config.setConnectionTimeout(Long.parseLong(getConfigValue("DB_CONNECTION_TIMEOUT", String.valueOf(DEFAULT_CONNECTION_TIMEOUT))));
            config.setIdleTimeout(Long.parseLong(getConfigValue("DB_IDLE_TIMEOUT", String.valueOf(DEFAULT_IDLE_TIMEOUT))));
            config.setMaxLifetime(Long.parseLong(getConfigValue("DB_MAX_LIFETIME", String.valueOf(DEFAULT_MAX_LIFETIME))));

            // 连接池名称和测试查询
            config.setPoolName("AEP-Integration-Pool");
            config.setConnectionTestQuery("SELECT 1");

            // MySQL特定配置
            config.addDataSourceProperty("useUnicode", "true");
            config.addDataSourceProperty("characterEncoding", "utf8mb4");
            config.addDataSourceProperty("useSSL", getConfigValue("DB_USE_SSL", "false"));
            config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
            config.addDataSourceProperty("serverTimezone", "Asia/Shanghai");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");

            // 性能优化配置
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            this.dataSource = new HikariDataSource(config);

            logger.info("数据库连接池初始化完成 - 最大连接数: " + config.getMaximumPoolSize());

        } catch (Exception e) {
            logger.severe("数据库连接池初始化失败: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }

    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl() {
        String host = getConfigValue("DB_HOST", "localhost");
        String port = getConfigValue("DB_PORT", "3306");
        String database = getConfigValue("DB_NAME", "aep_integration");

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("jdbc:mysql://")
                  .append(host)
                  .append(":")
                  .append(port)
                  .append("/")
                  .append(database)
                  .append("?useUnicode=true")
                  .append("&characterEncoding=utf8mb4")
                  .append("&serverTimezone=Asia/Shanghai")
                  .append("&allowPublicKeyRetrieval=true");

        // 如果启用SSL
        if ("true".equals(getConfigValue("DB_USE_SSL", "false"))) {
            urlBuilder.append("&useSSL=true");
            urlBuilder.append("&requireSSL=true");
        } else {
            urlBuilder.append("&useSSL=false");
        }

        return urlBuilder.toString();
    }

    /**
     * 初始化MyBatis会话工厂
     */
    private void initializeSqlSessionFactory() {
        try {
            logger.info("初始化MyBatis会话工厂...");

            // 加载MyBatis配置文件
            InputStream configStream = DatabaseConfig.class.getClassLoader()
                    .getResourceAsStream("mybatis-config.xml");

            if (configStream == null) {
                // 如果没有配置文件，使用编程式配置
                this.sqlSessionFactory = createProgrammaticSqlSessionFactory();
            } else {
                // 使用配置文件
                this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(configStream);
                configStream.close();
            }

            logger.info("MyBatis会话工厂初始化完成");

        } catch (Exception e) {
            logger.severe("MyBatis会话工厂初始化失败: " + e.getMessage());
            throw new RuntimeException("Failed to initialize SqlSessionFactory", e);
        }
    }

    /**
     * 编程式创建SqlSessionFactory（当没有mybatis-config.xml时）
     */
    private SqlSessionFactory createProgrammaticSqlSessionFactory() {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();

        // 基础配置
        configuration.setEnvironment(new org.apache.ibatis.mapping.Environment(
            "development",
            new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),
            dataSource
        ));

        // MyBatis行为配置
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
        configuration.setCacheEnabled(true);
        configuration.setLazyLoadingEnabled(true);
        configuration.setAggressiveLazyLoading(false);

        // 注册Mapper接口（后续添加）
        // configuration.addMapper(ProductMapper.class);
        // configuration.addMapper(OperationLogMapper.class);

        return new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(configuration);
    }

    /**
     * 运行数据库迁移
     */
    private void runDatabaseMigration() {
        try {
            String enableMigration = getConfigValue("DB_ENABLE_MIGRATION", "true");
            if (!"true".equalsIgnoreCase(enableMigration)) {
                logger.info("数据库迁移已禁用，跳过迁移步骤");
                return;
            }

            logger.info("开始数据库迁移...");

            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .validateOnMigrate(true)
                    .cleanDisabled(true)  // 禁用clean操作以保护生产数据
                    .load();

            // 执行迁移
            flyway.migrate();

            logger.info("数据库迁移完成");

        } catch (Exception e) {
            logger.warning("数据库迁移失败，但不影响应用启动: " + e.getMessage());
            // 迁移失败不影响应用启动，只记录警告
        }
    }

    /**
     * 获取配置值（环境变量优先，然后是配置文件）
     */
    private String getConfigValue(String key, String defaultValue) {
        // 1. 优先从环境变量获取
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }

        // 2. 从系统属性获取
        String propValue = System.getProperty(key);
        if (propValue != null && !propValue.trim().isEmpty()) {
            return propValue.trim();
        }

        // 3. 从配置文件获取（database.properties）
        try {
            Properties props = loadDatabaseProperties();
            String fileValue = props.getProperty(key);
            if (fileValue != null && !fileValue.trim().isEmpty()) {
                return fileValue.trim();
            }
        } catch (Exception e) {
            // 配置文件不存在或读取失败，使用默认值
        }

        // 4. 返回默认值
        return defaultValue;
    }

    /**
     * 加载数据库配置文件
     */
    private Properties loadDatabaseProperties() throws IOException {
        Properties props = new Properties();
        InputStream stream = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("database.properties");
        if (stream != null) {
            props.load(stream);
            stream.close();
        }
        return props;
    }

    /**
     * 获取数据源
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * 获取SqlSessionFactory
     */
    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    /**
     * 检查数据库连接健康状态
     */
    public boolean isHealthy() {
        try {
            if (dataSource == null || dataSource.isClosed()) {
                return false;
            }

            // 测试连接
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement("SELECT 1")) {
                statement.execute();
                return true;
            }
        } catch (Exception e) {
            logger.warning("数据库健康检查失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取连接池统计信息
     */
    public DatabaseStats getStats() {
        if (dataSource == null) {
            return new DatabaseStats();
        }

        return new DatabaseStats(
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }

    /**
     * 关闭数据源
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("关闭数据库连接池...");
            dataSource.close();
        }
    }

    /**
     * 数据库统计信息类
     */
    public static class DatabaseStats {
        private final int totalConnections;
        private final int activeConnections;
        private final int idleConnections;
        private final int waitingThreads;

        public DatabaseStats() {
            this(0, 0, 0, 0);
        }

        public DatabaseStats(int totalConnections, int activeConnections,
                           int idleConnections, int waitingThreads) {
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.waitingThreads = waitingThreads;
        }

        public int getTotalConnections() { return totalConnections; }
        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getWaitingThreads() { return waitingThreads; }

        @Override
        public String toString() {
            return String.format(
                "DatabaseStats{total=%d, active=%d, idle=%d, waiting=%d}",
                totalConnections, activeConnections, idleConnections, waitingThreads
            );
        }
    }
}