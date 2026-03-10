package com.example.datamigration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MigrationRequest {

    /** 任务名称 */
    private String jobName;

    /** 源库 Oracle 配置 */
    @Valid
    private OracleSourceConfig source;

    /** 目标库 TDSQL 配置 */
    @Valid
    private TdsqlTargetConfig target;

    /** 要迁移的表名列表 */
    @NotEmpty(message = "至少指定一张表")
    private List<String> tableNames;
}
