package com.example.datamigration.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源：根据迁移任务创建临时 Oracle / TDSQL 连接，不参与 Spring 主数据源。
 */
@Configuration
public class DataSourceConfig {

    /**
     * 缓存任务级别的数据源，任务结束后可关闭。简单实现中由调用方在用时创建、用毕关闭。
     */
    public static DataSource createOracleDataSource(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        return new HikariDataSource(config);
    }

    public static DataSource createMySQLDataSource(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        return new HikariDataSource(config);
    }

    public static DataSource createPostgresDataSource(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        return new HikariDataSource(config);
    }
}
