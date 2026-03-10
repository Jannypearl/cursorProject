package com.example.datamigration.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "migration_job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务名称 */
    @Column(nullable = false, length = 255)
    private String name;

    /** 源库类型：oracle */
    @Column(nullable = false, length = 32)
    private String sourceType;

    /** 目标库类型：tdsql_mysql / tdsql_pg */
    @Column(nullable = false, length = 32)
    private String targetType;

    /** 要迁移的表名（多表逗号分隔） */
    @Column(nullable = false, length = 2000)
    private String tableNames;

    /** 状态：PENDING, EXPORTING, IMPORTING, SUCCESS, FAILED */
    @Column(nullable = false, length = 32)
    private String status;

    /** 当前步骤描述 */
    @Column(length = 500)
    private String currentStep;

    /** 错误信息（失败时） */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /** 导出行数统计（JSON 或简单数字） */
    @Column(length = 1000)
    private String exportStats;

    /** 导入行数统计 */
    @Column(length = 1000)
    private String importStats;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
