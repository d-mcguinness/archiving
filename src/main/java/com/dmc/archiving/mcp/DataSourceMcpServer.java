package com.dmc.archiving.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * MCP Server for PostgreSQL Datasource Operations
 * Provides tools for AI assistants to interact with the PostgreSQL database
 * Only active in local profile
 */
@RestController
@RequestMapping("/mcp/datasource")
@Profile("local")
public class DataSourceMcpServer {

    private static final Logger log = LoggerFactory.getLogger(DataSourceMcpServer.class);

    private final DataSource dataSource;
    private final String jdbcUrl;
    private final String username;

    public DataSourceMcpServer(
            DataSource dataSource,
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username) {
        this.dataSource = dataSource;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        log.info("DataSource MCP Server initialized for database: {}", jdbcUrl);
    }

    /**
     * Get MCP server information and available tools
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "datasource");
        info.put("version", "1.0.0");
        info.put("description", "MCP server for PostgreSQL database operations");
        info.put("jdbcUrl", jdbcUrl);
        info.put("username", username);

        List<Map<String, Object>> tools = new ArrayList<>();

        // List tables tool
        tools.add(Map.of(
            "name", "list_tables",
            "description", "List all tables in the database",
            "parameters", Map.of(
                "schema", Map.of("type", "string", "description", "Schema name (default: public)", "required", false)
            )
        ));

        // Describe table tool
        tools.add(Map.of(
            "name", "describe_table",
            "description", "Get the schema/structure of a specific table",
            "parameters", Map.of(
                "tableName", Map.of("type", "string", "description", "Name of the table", "required", true),
                "schema", Map.of("type", "string", "description", "Schema name (default: public)", "required", false)
            )
        ));

        // Execute query tool
        tools.add(Map.of(
            "name", "execute_query",
            "description", "Execute a SELECT query (read-only)",
            "parameters", Map.of(
                "query", Map.of("type", "string", "description", "SQL SELECT query to execute", "required", true),
                "maxRows", Map.of("type", "integer", "description", "Maximum rows to return (default: 100)", "required", false)
            )
        ));

        // Get database info tool
        tools.add(Map.of(
            "name", "get_database_info",
            "description", "Get database metadata and connection information",
            "parameters", Map.of()
        ));

        // Get table count tool
        tools.add(Map.of(
            "name", "get_table_count",
            "description", "Get the row count for a specific table",
            "parameters", Map.of(
                "tableName", Map.of("type", "string", "description", "Name of the table", "required", true),
                "schema", Map.of("type", "string", "description", "Schema name (default: public)", "required", false)
            )
        ));

        info.put("tools", tools);

        return ResponseEntity.ok(info);
    }

    /**
     * List all tables in the database
     */
    @PostMapping("/tools/list_tables")
    public ResponseEntity<Map<String, Object>> listTables(
            @RequestBody(required = false) Map<String, String> params) {
        String schema = params != null && params.containsKey("schema")
            ? params.get("schema") : "public";

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, schema, "%", new String[]{"TABLE"});

            List<Map<String, Object>> tables = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> table = new HashMap<>();
                table.put("schema", rs.getString("TABLE_SCHEM"));
                table.put("name", rs.getString("TABLE_NAME"));
                table.put("type", rs.getString("TABLE_TYPE"));
                table.put("remarks", rs.getString("REMARKS"));
                tables.add(table);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("schema", schema);
            result.put("tables", tables);
            result.put("count", tables.size());

            return ResponseEntity.ok(result);

        } catch (SQLException e) {
            log.error("Error listing tables: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Describe table structure
     */
    @PostMapping("/tools/describe_table")
    public ResponseEntity<Map<String, Object>> describeTable(
            @RequestBody Map<String, String> params) {
        String tableName = params.get("tableName");
        String schema = params.getOrDefault("schema", "public");

        if (tableName == null || tableName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Parameter 'tableName' is required"
            ));
        }

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Get columns
            ResultSet columnsRs = metaData.getColumns(null, schema, tableName, "%");
            List<Map<String, Object>> columns = new ArrayList<>();

            while (columnsRs.next()) {
                Map<String, Object> column = new HashMap<>();
                column.put("name", columnsRs.getString("COLUMN_NAME"));
                column.put("type", columnsRs.getString("TYPE_NAME"));
                column.put("size", columnsRs.getInt("COLUMN_SIZE"));
                column.put("nullable", columnsRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.put("defaultValue", columnsRs.getString("COLUMN_DEF"));
                column.put("remarks", columnsRs.getString("REMARKS"));
                columns.add(column);
            }

            // Get primary keys
            ResultSet pkRs = metaData.getPrimaryKeys(null, schema, tableName);
            List<String> primaryKeys = new ArrayList<>();

            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }

            // Get foreign keys
            ResultSet fkRs = metaData.getImportedKeys(null, schema, tableName);
            List<Map<String, Object>> foreignKeys = new ArrayList<>();

            while (fkRs.next()) {
                Map<String, Object> fk = new HashMap<>();
                fk.put("column", fkRs.getString("FKCOLUMN_NAME"));
                fk.put("referencedTable", fkRs.getString("PKTABLE_NAME"));
                fk.put("referencedColumn", fkRs.getString("PKCOLUMN_NAME"));
                foreignKeys.add(fk);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("schema", schema);
            result.put("tableName", tableName);
            result.put("columns", columns);
            result.put("primaryKeys", primaryKeys);
            result.put("foreignKeys", foreignKeys);

            return ResponseEntity.ok(result);

        } catch (SQLException e) {
            log.error("Error describing table: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Execute a SELECT query (read-only)
     */
    @PostMapping("/tools/execute_query")
    public ResponseEntity<Map<String, Object>> executeQuery(
            @RequestBody Map<String, Object> params) {
        String query = (String) params.get("query");
        Integer maxRows = params.containsKey("maxRows") && params.get("maxRows") != null
            ? ((Number) params.get("maxRows")).intValue() : 100;

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Parameter 'query' is required"
            ));
        }

        // Security: Only allow SELECT queries
        String trimmedQuery = query.trim().toLowerCase();
        if (!trimmedQuery.startsWith("select")) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Only SELECT queries are allowed"
            ));
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.setMaxRows(maxRows);
            ResultSet rs = stmt.executeQuery(query);

            // Get column names
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int columnCount = rsMetaData.getColumnCount();

            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(rsMetaData.getColumnName(i));
            }

            // Get rows
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(columnNames.get(i - 1), rs.getObject(i));
                }
                rows.add(row);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("columns", columnNames);
            result.put("rows", rows);
            result.put("rowCount", rows.size());
            result.put("maxRows", maxRows);

            return ResponseEntity.ok(result);

        } catch (SQLException e) {
            log.error("Error executing query: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get database information
     */
    @PostMapping("/tools/get_database_info")
    public ResponseEntity<Map<String, Object>> getDatabaseInfo() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            Map<String, Object> info = new HashMap<>();
            info.put("success", true);
            info.put("databaseProductName", metaData.getDatabaseProductName());
            info.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            info.put("driverName", metaData.getDriverName());
            info.put("driverVersion", metaData.getDriverVersion());
            info.put("jdbcUrl", jdbcUrl);
            info.put("username", username);
            info.put("maxConnections", metaData.getMaxConnections());
            info.put("supportsTransactions", metaData.supportsTransactions());

            return ResponseEntity.ok(info);

        } catch (SQLException e) {
            log.error("Error getting database info: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get row count for a table
     */
    @PostMapping("/tools/get_table_count")
    public ResponseEntity<Map<String, Object>> getTableCount(
            @RequestBody Map<String, String> params) {
        String tableName = params.get("tableName");
        String schema = params.getOrDefault("schema", "public");

        if (tableName == null || tableName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Parameter 'tableName' is required"
            ));
        }

        // Sanitize table name to prevent SQL injection
        if (!tableName.matches("[a-zA-Z0-9_]+")) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Invalid table name"
            ));
        }

        String query = String.format("SELECT COUNT(*) FROM %s.%s", schema, tableName);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                long count = rs.getLong(1);

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "schema", schema,
                    "tableName", tableName,
                    "count", count
                ));
            }

            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "No result returned"
            ));

        } catch (SQLException e) {
            log.error("Error getting table count: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try (Connection conn = dataSource.getConnection()) {
            boolean isValid = conn.isValid(5); // 5 second timeout

            if (isValid) {
                return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "service", "postgresql",
                    "jdbcUrl", jdbcUrl
                ));
            } else {
                return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "service", "postgresql",
                    "error", "Connection validation failed"
                ));
            }

        } catch (SQLException e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(503).body(Map.of(
                "status", "DOWN",
                "service", "postgresql",
                "error", e.getMessage()
            ));
        }
    }
}

