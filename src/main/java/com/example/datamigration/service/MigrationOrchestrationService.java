package com.example.datamigration.service;

import com.example.datamigration.dto.MigrationRequest;
import com.example.datamigration.dto.OracleSourceConfig;
import com.example.datamigration.dto.TdsqlTargetConfig;
import com.example.datamigration.entity.MigrationJob;
import com.example.datamigration.repository.MigrationJobRepository;
import com.example.datamigration.service.OracleCsvExportService.ExportResult;
import com.example.datamigration.service.TdsqlCsvImportService.ImportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 编排迁移流程：Oracle 导出 CSV -> TDSQL 导入 CSV。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationOrchestrationService {

    private final MigrationJobRepository jobRepository;
    private final OracleCsvExportService exportService;
    private final TdsqlCsvImportService importService;

    /**
     * 创建迁移任务并入库，返回任务信息。不执行迁移。
     */
    @Transactional
    public MigrationJob createJob(MigrationRequest request) {
        String jobName = request.getJobName() != null ? request.getJobName() : "migration-" + System.currentTimeMillis();
        TdsqlTargetConfig target = request.getTarget();
        MigrationJob job = MigrationJob.builder()
                .name(jobName)
                .sourceType("oracle")
                .targetType(target.getTargetType())
                .tableNames(String.join(",", request.getTableNames()))
                .status("PENDING")
                .build();
        return jobRepository.save(job);
    }

    /**
     * 异步执行迁移（导出 CSV -> 导入 TDSQL）。任务需已通过 createJob 创建。
     */
    @Async("migrationTaskExecutor")
    @Transactional
    public void runMigrationAsync(Long jobId, MigrationRequest request) {
        List<String> tables = request.getTableNames();
        OracleSourceConfig source = request.getSource();
        TdsqlTargetConfig target = request.getTarget();
        String jobIdStr = String.valueOf(jobId);

        try {
            updateJob(jobId, "EXPORTING", "正在从 Oracle 导出表到 CSV...", null);

            List<ExportResult> exportResults = exportService.exportToCsv(source, tables, jobIdStr);
            String exportStats = exportResults.stream()
                    .map(e -> e.getTableName() + ":" + e.getRowCount())
                    .collect(Collectors.joining("; "));
            updateJob(jobId, "EXPORTING", "导出完成", exportStats);

            updateJob(jobId, "IMPORTING", "正在将 CSV 导入 TDSQL...", null);
            List<ImportResult> importResults = importService.importFromCsv(target, exportResults);
            String importStats = importResults.stream()
                    .map(i -> i.getTableName() + ":" + i.getRowCount())
                    .collect(Collectors.joining("; "));

            updateJob(jobId, "SUCCESS", "迁移完成", exportStats, importStats, null);
            log.info("迁移任务 {} 执行成功", jobId);
        } catch (Exception e) {
            log.error("迁移任务 {} 执行失败", jobId, e);
            updateJob(jobId, "FAILED", null, null, null, e.getMessage());
        }
    }

    private void updateJob(Long id, String status, String currentStep, String exportStats) {
        updateJob(id, status, currentStep, exportStats, null, null);
    }

    @Transactional
    public void updateJob(Long id, String status, String currentStep, String exportStats, String importStats, String errorMessage) {
        jobRepository.findById(id).ifPresent(job -> {
            job.setStatus(status);
            if (currentStep != null) job.setCurrentStep(currentStep);
            if (exportStats != null) job.setExportStats(exportStats);
            if (importStats != null) job.setImportStats(importStats);
            if (errorMessage != null) job.setErrorMessage(errorMessage);
            jobRepository.save(job);
        });
    }

    public List<MigrationJob> listJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc();
    }

    public MigrationJob getJob(Long id) {
        return jobRepository.findById(id).orElse(null);
    }
}
