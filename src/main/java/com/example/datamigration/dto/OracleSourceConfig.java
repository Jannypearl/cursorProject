package com.example.datamigration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OracleSourceConfig {

    @NotBlank(message = "Oracle 主机不能为空")
    private String host;

    private int port = 1521;

    @NotBlank(message = "服务名/SID 不能为空")
    private String serviceNameOrSid;

    /** 若使用 SID，设为 true；否则按 serviceName 连接 */
    private boolean useSid = false;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    public String toJdbcUrl() {
        if (useSid) {
            return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, serviceNameOrSid);
        }
        return String.format("jdbc:oracle:thin:@//%s:%d/%s", host, port, serviceNameOrSid);
    }
}
