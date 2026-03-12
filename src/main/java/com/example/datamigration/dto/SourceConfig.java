package com.example.datamigration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SourceConfig {

    /**
     * 源库类型：oracle / mysql / postgresql
     */
    @NotBlank(message = "sourceType 不能为空")
    private String sourceType;

    @NotBlank(message = "源库主机不能为空")
    private String host;

    /**
     * oracle 默认 1521；mysql 默认 3306；postgresql 默认 5432
     */
    private int port;

    /**
     * MySQL/PostgreSQL 数据库名
     */
    private String database;

    /**
     * Oracle 服务名或 SID
     */
    private String serviceNameOrSid;

    /**
     * Oracle：若使用 SID，设为 true；否则按 serviceName 连接
     */
    private boolean useSid = false;

    @NotBlank(message = "源库用户名不能为空")
    private String username;

    @NotBlank(message = "源库密码不能为空")
    private String password;

    public int getPort() {
        if (port > 0) return port;
        if ("postgresql".equalsIgnoreCase(sourceType)) return 5432;
        if ("mysql".equalsIgnoreCase(sourceType)) return 3306;
        return 1521;
    }

    public String toJdbcUrl() {
        if ("mysql".equalsIgnoreCase(sourceType)) {
            if (database == null || database.isBlank()) {
                throw new IllegalArgumentException("MySQL 源库 database 不能为空");
            }
            return String.format(
                    "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                    host, getPort(), database
            );
        }
        if ("postgresql".equalsIgnoreCase(sourceType)) {
            if (database == null || database.isBlank()) {
                throw new IllegalArgumentException("PostgreSQL 源库 database 不能为空");
            }
            return String.format("jdbc:postgresql://%s:%d/%s", host, getPort(), database);
        }
        // oracle
        if (serviceNameOrSid == null || serviceNameOrSid.isBlank()) {
            throw new IllegalArgumentException("Oracle 源库 serviceNameOrSid 不能为空");
        }
        if (useSid) {
            return String.format("jdbc:oracle:thin:@%s:%d:%s", host, getPort(), serviceNameOrSid);
        }
        return String.format("jdbc:oracle:thin:@//%s:%d/%s", host, getPort(), serviceNameOrSid);
    }
}

