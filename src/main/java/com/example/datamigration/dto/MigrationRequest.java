package com.example.datamigration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MigrationRequest {

    /** 任务名称 */
    private String jobName;

    /** 源库配置：oracle/mysql/postgresql */
    @Valid
    @NotNull(message = "源库配置不能为空")
    private SourceConfig source;

    /** 目标库 TDSQL 配置 */
    @Valid
    @NotNull(message = "目标库配置不能为空")
    private TdsqlTargetConfig target;

    /** 要迁移的表名列表 */
    @NotEmpty(message = "至少指定一张表")
    private List<String> tableNames;
}
