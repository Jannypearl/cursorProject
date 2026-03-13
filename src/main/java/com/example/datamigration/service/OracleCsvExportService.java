package com.example.datamigration.service;

import com.example.datamigration.config.DataSourceConfig;
import com.example.datamigration.dto.SourceConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 从源库（Oracle/MySQL/PostgreSQL）导出表数据到 CSV 文件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OracleCsvExportService {

    @Value("${migration.csv.base-dir:./migration-csv}")
    private String csvBaseDir;

    /**
     * 导出多张表到指定目录，返回每张表对应的 CSV 路径及行数。
     */
    public List<ExportResult> exportToCsv(SourceConfig sourceConfig, List<String> tableNames, String jobId) throws Exception {
        Path jobDir = Path.of(csvBaseDir, jobId);
        Files.createDirectories(jobDir);

        DataSource ds = null;
        try {
            ds = createSourceDataSource(sourceConfig);
            List<ExportResult> results = new ArrayList<>();
            for (String tableName : tableNames) {
                ExportResult r = exportTable(ds, tableName.trim(), jobDir);
                results.add(r);
            }
            return results;
        } finally {
            if (ds instanceof AutoCloseable c) {
                try {
                    c.close();
                } catch (Exception e) {
                    log.warn("关闭源库数据源异常", e);
                }
            }
        }
    }

    private DataSource createSourceDataSource(SourceConfig config) {
        String t = config.getSourceType() == null ? "" : config.getSourceType().toLowerCase();
        return switch (t) {
            case "mysql" -> DataSourceConfig.createMySQLDataSource(
                    config.toJdbcUrl(), config.getUsername(), config.getPassword());
            case "postgresql", "postgres" -> DataSourceConfig.createPostgresDataSource(
                    config.toJdbcUrl(), config.getUsername(), config.getPassword());
            case "oracle" -> DataSourceConfig.createOracleDataSource(
                    config.toJdbcUrl(), config.getUsername(), config.getPassword());
            default -> throw new IllegalArgumentException("不支持的 sourceType: " + config.getSourceType());
        };
    }

    private ExportResult exportTable(DataSource ds, String tableName, Path jobDir) throws SQLException, IOException {
        Path csvPath = jobDir.resolve(sanitizeFileName(tableName) + ".csv");
        long rows = 0;

        try (Connection conn = ds.getConnection()) {
            String sql = "SELECT * FROM " + quoteIdentifier(conn, tableName);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                List<String> headers = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    headers.add(meta.getColumnLabel(i));
                }

                try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
                    writer.write(headers.stream().map(this::escapeCsv).collect(Collectors.joining(",")));
                    writer.newLine();

                    while (rs.next()) {
                        List<String> row = new ArrayList<>();
                        for (int i = 1; i <= colCount; i++) {
                            Object v = rs.getObject(i);
                            row.add(escapeCsv(valueToString(v)));
                        }
                        writer.write(String.join(",", row));
                        writer.newLine();
                        rows++;
                    }
                }
            }
        }

        log.info("导出表 {} 到 {} 完成，行数: {}", tableName, csvPath, rows);
        return new ExportResult(tableName, csvPath.toString(), rows);
    }

    private String quoteIdentifier(Connection conn, String name) throws SQLException {
        String quote = conn.getMetaData().getIdentifierQuoteString();
        if (quote == null || quote.isEmpty()) {
            quote = "\"";
        }
        return quote + name + quote;
    }

    private String sanitizeFileName(String tableName) {
        return tableName.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String valueToString(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Timestamp ts) {
            return ts.toString();
        }
        if (v instanceof Date d) {
            return d.toString();
        }
        return v.toString();
    }

    private String escapeCsv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    @lombok.Data
    public static class ExportResult {
        private final String tableName;
        private final String csvPath;
        private final long rowCount;
    }
}
