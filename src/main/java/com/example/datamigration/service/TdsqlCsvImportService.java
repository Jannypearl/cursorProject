package com.example.datamigration.service;

import com.example.datamigration.config.DataSourceConfig;
import com.example.datamigration.dto.TdsqlTargetConfig;
import com.example.datamigration.service.OracleCsvExportService.ExportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 将 CSV 文件导入 TDSQL-MySQL 或 TDSQL-PG。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TdsqlCsvImportService {

    private static final int BATCH_SIZE = 1000;

    /**
     * 根据 ExportResult 列表，将对应 CSV 导入到目标库对应表中。
     * 约定：目标库中表结构已准备好（本服务仅负责数据写入）。
     */
    public List<ImportResult> importFromCsv(TdsqlTargetConfig targetConfig, List<ExportResult> exportResults) throws Exception {
        DataSource ds = createDataSource(targetConfig);
        try {
            List<ImportResult> results = new ArrayList<>();
            for (ExportResult er : exportResults) {
                ImportResult ir = importTable(ds, targetConfig.getTargetType(), er.getTableName(), Path.of(er.getCsvPath()));
                results.add(ir);
            }
            return results;
        } finally {
            if (ds instanceof AutoCloseable c) {
                try {
                    c.close();
                } catch (Exception e) {
                    log.warn("关闭目标库数据源异常", e);
                }
            }
        }
    }
    private DataSource createDataSource(TdsqlTargetConfig config) {
        if ("tdsql_pg".equalsIgnoreCase(config.getTargetType())) {
            return DataSourceConfig.createPostgresDataSource(
                    config.toJdbcUrl(), config.getUsername(), config.getPassword());
        }
        return DataSourceConfig.createMySQLDataSource(
                config.toJdbcUrl(), config.getUsername(), config.getPassword());
    }

    private ImportResult importTable(DataSource ds, String targetType, String tableName, Path csvPath) throws SQLException, IOException {
        if (!Files.exists(csvPath)) {
            throw new IOException("CSV 文件不存在: " + csvPath);
        }

        List<String> columns;
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                return new ImportResult(tableName, 0, "空文件");
            }
            columns = parseCsvLine(header);
        }

        String quote = "tdsql_pg".equalsIgnoreCase(targetType) ? "\"" : "`";
        String quotedTable = quote + tableName + quote;
        String quotedColumns = columns.stream().map(c -> quote + c + quote).collect(Collectors.joining(","));
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(","));
        String insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)", quotedTable, quotedColumns, placeholders);

        long rows = 0;
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql);
             BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {

            conn.setAutoCommit(false);
            reader.readLine(); // skip header

            int batch = 0;
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    List<String> values = parseCsvLine(line);
                    for (int i = 0; i < columns.size(); i++) {
                        String v = i < values.size() ? values.get(i) : "";
                        ps.setObject(i + 1, v.isEmpty() ? null : v);
                    }
                    ps.addBatch();
                    batch++;
                    if (batch >= BATCH_SIZE) {
                        ps.executeBatch();
                        rows += batch;
                        batch = 0;
                    }
                }
                if (batch > 0) {
                    ps.executeBatch();
                    rows += batch;
                }
                conn.commit();
            } catch (Exception e) {
                // 关键：批处理异常时必须显式回滚，避免部分数据落库
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.addSuppressed(e);
                    throw rollbackEx;
                }
                throw e;
            }
        }

        log.info("导入表 {} 完成，行数: {}", tableName, rows);
        return new ImportResult(tableName, rows, null);
    }
    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    cell.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (inQuotes) {
                cell.append(c);
            } else if (c == ',') {
                result.add(cell.toString().trim());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        result.add(cell.toString().trim());
        return result;
    }

    @lombok.Data
    public static class ImportResult {
        private final String tableName;
        private final long rowCount;
        private final String errorMessage;
    }
}

