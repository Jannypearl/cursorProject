# 数据迁移服务 (Data Migration)

基于 **Spring Boot 3** 的后端服务：应用自身使用 **MySQL** 存储迁移任务与状态，支持从 **Oracle / MySQL / PostgreSQL** 将表数据导出为 CSV，再导入到 **TDSQL-MySQL** 或 **TDSQL-PG**。

## 迁移流程

1. **导出**：连接源库（Oracle/MySQL/PostgreSQL），将指定表的数据查询并写入 CSV 文件（UTF-8）。
2. **导入**：读取 CSV 文件，通过 JDBC 批量插入到已配置的 TDSQL（MySQL 或 PostgreSQL）目标库。

## 技术栈

- Java 17、Spring Boot 3.2.x
- 应用库：MySQL + JPA
- 源库：Oracle（ojdbc11）/ MySQL（mysql-connector-j）/ PostgreSQL（postgresql）
- 目标库：TDSQL-MySQL（mysql-connector-j）、TDSQL-PG（postgresql）

## 配置说明

### 1. 应用自身 MySQL

在 `application.yml` 或环境变量中配置本应用使用的 MySQL：

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| `MYSQL_HOST` | MySQL 主机 | localhost |
| `MYSQL_PORT` | 端口 | 3306 |
| `MYSQL_DATABASE` | 数据库名 | data_migration |
| `MYSQL_USER` | 用户名 | root |
| `MYSQL_PASSWORD` | 密码 | 空 |

首次启动时会自动建表（`ddl-auto: update`）。

### 2. CSV 输出目录

| 配置/环境变量 | 说明 | 默认值 |
|---------------|------|--------|
| `migration.csv.base-dir` / `MIGRATION_CSV_DIR` | 导出 CSV 存放根目录 | `./migration-csv` |

每个迁移任务会在此目录下创建以任务 ID 命名的子目录，表名会作为文件名（如 `USER_INFO.csv`）。

## API 说明

### 提交迁移任务（异步）

```http
POST /api/migration/run
Content-Type: application/json
```

请求体示例（Oracle → TDSQL-MySQL）：

```json
{
  "jobName": "my-first-migration",
  "source": {
    "sourceType": "oracle",
    "host": "oracle-host",
    "port": 1521,
    "serviceNameOrSid": "ORCL",
    "useSid": false,
    "username": "scott",
    "password": "tiger"
  },
  "target": {
    "targetType": "tdsql_mysql",
    "host": "tdsql-mysql-host",
    "port": 3306,
    "database": "mydb",
    "username": "admin",
    "password": "secret"
  },
  "tableNames": ["USER_INFO", "ORDER_MAIN"]
}
```

请求体示例（MySQL → TDSQL-PG）：

```json
{
  "jobName": "mysql-to-tdsqlpg",
  "source": {
    "sourceType": "mysql",
    "host": "mysql-host",
    "port": 3306,
    "database": "sourcedb",
    "username": "root",
    "password": "rootpwd"
  },
  "target": {
    "targetType": "tdsql_pg",
    "host": "tdsql-pg-host",
    "port": 5432,
    "database": "targetdb",
    "username": "pguser",
    "password": "pgpwd"
  },
  "tableNames": ["user_info"]
}
```

目标为 TDSQL-PG 时，将 `target.targetType` 设为 `tdsql_pg`，并设置 `port`（默认 5432）和对应数据库名即可。

响应：`202 Accepted`，并在 body 中返回刚创建的迁移任务信息（含 `id`）。任务在后台异步执行。

### 查询任务列表

```http
GET /api/migration/jobs
```

返回所有迁移任务，按创建时间倒序。

### 查询单个任务状态

```http
GET /api/migration/jobs/{id}
```

任务状态：`PENDING` → `EXPORTING` → `IMPORTING` → `SUCCESS` / `FAILED`。  
`exportStats` / `importStats` 中会记录各表的导出行数、导入行数。

## 运行方式

1. 确保本机或远程有 MySQL，并已创建数据库（如 `data_migration`）。
2. 配置好上述环境变量或 `application.yml`。
3. 执行：

```bash
mvn spring-boot:run
```

或打包后运行：

```bash
mvn package -DskipTests
java -jar target/data-migration-1.0.0-SNAPSHOT.jar
```

## 使用注意

- **Oracle 连接**：`source.sourceType=oracle` 时，`useSid: true` 使用 SID；`false` 使用服务名（推荐）。
- **MySQL/PostgreSQL 源库**：`source.sourceType=mysql/postgresql` 时请填写 `database`。
- **目标表**：导入前请在 TDSQL 中预先创建好与源表结构一致（或兼容）的表，本服务只做数据插入，不自动建表。
- **CSV 编码**：导出与解析均使用 UTF-8；若源库有特殊字符，请保证 Oracle 端字符集与导出一致。
- **大批量**：单表数据量极大时，可考虑按表分批提交多个任务，或后续扩展分页导出/导入。

## 项目结构概览

```
src/main/java/com/example/datamigration/
├── DataMigrationApplication.java
├── config/
│   ├── AsyncConfig.java
│   └── DataSourceConfig.java
├── controller/
│   └── MigrationController.java
├── dto/
│   ├── MigrationRequest.java
│   ├── OracleSourceConfig.java
│   └── TdsqlTargetConfig.java
├── entity/
│   └── MigrationJob.java
├── repository/
│   └── MigrationJobRepository.java
└── service/
    ├── MigrationOrchestrationService.java
    ├── OracleCsvExportService.java
    └── TdsqlCsvImportService.java
```
