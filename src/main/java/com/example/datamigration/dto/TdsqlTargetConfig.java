package com.example.datamigration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TdsqlTargetConfig {

    /** tdsql_mysql / tdsql_pg */
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    @NotBlank(message = "主机不能为空")
    private String host;

    private int port;

    @NotBlank(message = "数据库名不能为空")
    private String database;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** MySQL 默认 3306，PG 默认 5432 */
    public int getPort() {
        if (port > 0) {
            return port;
        }
        return "tdsql_pg".equalsIgnoreCase(targetType) ? 5432 : 3306;
    }

    public String toJdbcUrl() {
        if ("tdsql_pg".equalsIgnoreCase(targetType)) {
            return String.format("jdbc:postgresql://%s:%d/%s", host, getPort(), database);
        }
        return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                host, getPort(), database);
    }
}
