package com.example.datamigration.controller;

import com.example.datamigration.dto.MigrationRequest;
import com.example.datamigration.entity.MigrationJob;
import com.example.datamigration.service.MigrationOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据迁移 REST API：提交迁移任务、查询任务列表与状态。
 */
@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationOrchestrationService orchestrationService;

    /**
     * 提交迁移任务（异步执行）。
     * 流程：Oracle 表 -> 导出 CSV -> 导入到 TDSQL-MySQL 或 TDSQL-PG。
     */
    @PostMapping("/run")
    public ResponseEntity<MigrationJob> runMigration(@Valid @RequestBody MigrationRequest request) {
        MigrationJob job = orchestrationService.createJob(request);
        orchestrationService.runMigrationAsync(job.getId(), request);
        return ResponseEntity.accepted().body(job);
    }

    /**
     * 查询所有迁移任务（按创建时间倒序）。
     */
    @GetMapping("/jobs")
    public ResponseEntity<List<MigrationJob>> listJobs() {
        return ResponseEntity.ok(orchestrationService.listJobs());
    }

    /**
     * 根据 ID 查询单个迁移任务状态。
     */
    @GetMapping("/jobs/{id}")
    public ResponseEntity<MigrationJob> getJob(@PathVariable Long id) {
        MigrationJob job = orchestrationService.getJob(id);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(job);
    }
}
