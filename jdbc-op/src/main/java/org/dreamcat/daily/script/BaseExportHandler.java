package org.dreamcat.daily.script;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.function.IConsumer;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.sql.JdbcUtil;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.FunctionUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.common.BaseHandler;
import org.dreamcat.daily.script.module.SqlGenModule;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Setter
@Accessors(fluent = true)
public abstract class BaseExportHandler extends BaseHandler {

    private boolean useShow;
    private boolean useDesc;
    private String showDatabases = "show databases";
    private String showTables = "show tables from $database";
    private String descTable = "desc $database.$table";
    private String select = "select * from $database.$table";
    private String select_without_database = "select * from $table";

    private String catalog;
    private String databasePattern;
    @ArgParserField({"d"})
    private List<String> databases;
    private String tablePattern; // .* for sql, or % for metadata
    private List<String> tables;
    @ArgParserField({"b"})
    private int batchSize = 1000;
    boolean verbose;
    private boolean abort;

    protected abstract void readSource(IConsumer<Connection> f) throws Exception;

    protected void writeTarget(IConsumer<Connection> f) throws Exception {
        f.accept(null);
    }

    protected abstract void handleRows(String database, String table,
            List<Map<String, Object>> rows, Map<String, JdbcColumnDef> columnMap,
            Connection targetConnection);

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
        if (ObjectUtil.isEmpty(databases) && ObjectUtil.isBlank(databasePattern)) {
            throw new IllegalArgumentException("require arg: --databases or --database-pattern");
        }
    }

    @Override
    public void run() throws Exception {
        readSource(this::handle);
    }

    private void handle(Connection connection) throws Exception {
        List<String> matchedDatabases = new ArrayList<>();
        if (ObjectUtil.isEmpty(databases)) {
            List<String> allDatabases = getDatabases(connection);
            if (allDatabases == null) {
                matchedDatabases.add(null);// database-less db
            } else if (ObjectUtil.isEmpty(allDatabases)) {
                throw new IllegalArgumentException("no databases found in catalog: " + catalog);
            } else {
                for (String database : allDatabases) {
                    if (!database.matches(databasePattern)) {
                        System.out.println(database + " is unmatched by " + databasePattern);
                        continue;
                    }
                    matchedDatabases.add(database);
                }
            }
        } else {
            matchedDatabases = databases;
            if (ObjectUtil.isNotBlank(databasePattern)) {
                System.out.println("already pass --databases so --database-pattern is ignored");
            }
            if (ObjectUtil.isNotBlank(databasePattern)) {
                System.out.println("already pass --databases so --all-database is ignored");
            }
        }

        System.out.println("matched databases: " + matchedDatabases);
        if (matchedDatabases.isEmpty()) return;
        List<String> dbs = matchedDatabases;
        writeTarget(targetConnection -> {
            handle(connection, dbs, targetConnection);
        });
    }

    private void handle(Connection connection, List<String> matchedDatabases,
            Connection targetConnection) throws SQLException {
        outer:
        for (String database : matchedDatabases) {
            List<String> matchedTables = getTables(connection, database);
            if (ObjectUtil.isEmpty(matchedTables)) {
                System.out.println("no tables found in database " + database);
                continue;
            }
            System.out.printf("matched tables in %s: %s%n", database, matchedTables);
            for (String table : matchedTables) {
                if (ObjectUtil.isNotEmpty(this.tables) && !this.tables.contains(table)) {
                    System.out.println(table + " is not in " + this.tables);
                    continue;
                }

                try {
                    handle(connection, database, table, targetConnection);
                } catch (Exception e) {
                    System.err.printf("failed to migrate %s.%s: %s%n",
                            database, table, e.getMessage());
                    if (verbose) e.printStackTrace();
                    if (abort) break outer;
                }
            }
        }
    }

    private void handle(Connection connection, String database, String table,
            Connection targetConnection) throws Exception {
        // schema
        List<JdbcColumnDef> columns = getColumns(connection, database, table);
        if (verbose) {
            System.out.println("columns: " + columns.stream()
                    .map(c -> Pair.of(c.getName(), c.getType()))
                    .collect(Collectors.toList()));
        }
        Map<String, JdbcColumnDef> columnMap = MapUtil.toMap(columns, JdbcColumnDef::getName);

        // query
        String sql = InterpolationUtil.format(
                database == null ? select_without_database : select,
                "database", database, "db", database, "table", table, "tb", table);
        System.out.println("extract: " + sql);
        try (Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(sql)) {
                JdbcUtil.getRows(rs, batchSize, rows -> {
                    System.out.printf("handling %d rows on %s.%s%n",
                            rows.size(), database, table);
                    if (!rows.isEmpty()) {
                        handleRows(database, table, rows, columnMap, targetConnection);
                    }
                });
            }
        }
    }

    private List<String> getDatabases(Connection connection) throws SQLException {
        if (!useShow) {
            System.out.printf("getDatabases: catalog=%s%n", catalog);
            return JdbcUtil.getDatabases(connection, catalog);
        }
        System.out.printf("getDatabases: %s%n", showDatabases);
        return JdbcUtil.getRows(connection, showDatabases).stream()
                .map(map -> new ArrayList<>(map.values()).get(0).toString())
                .collect(Collectors.toList());
    }

    private List<String> getTables(Connection connection, String database)
            throws SQLException {
        if (!useShow) {
            if (ObjectUtil.isNotBlank(tablePattern)) {
                System.out.printf("getTables: catalog=%s, database=%s, tablePattern=%s%n",
                        catalog, database, tablePattern);
                return JdbcUtil.getTableLike(connection, catalog, database, tablePattern);
            } else {
                System.out.printf("getTables: catalog=%s, database=%s%n", catalog, database);
                return JdbcUtil.getTables(connection, catalog, database);
            }
        }

        if (database == null) {
            throw new IllegalArgumentException("useShow is unsupported for database-less db");
        }
        String sql = InterpolationUtil.format(showTables,
                "database", database, "db", database);
        System.out.printf("getTables: %s%n", sql);
        return JdbcUtil.getRows(connection, sql).stream()
                .map(map -> new ArrayList<>(map.values()).get(0).toString())
                .filter(name -> ObjectUtil.isBlank(tablePattern) || name.matches(tablePattern))
                .collect(Collectors.toList());
    }

    private List<JdbcColumnDef> getColumns(Connection connection, String database, String table)
            throws SQLException {
        if (!useDesc) {
            System.out.printf("getColumns: catalog=%s, database=%s, table=%s%n",
                    catalog, database, table);
            return JdbcUtil.getColumns(
                    connection, catalog, database, table);
        }

        String sql = InterpolationUtil.format(descTable,
                "database", database, "db", database, "table", table, "tb", table);
        System.out.printf("getColumns: %s%n", sql);
        return JdbcUtil.getRows(connection, sql).stream()
                .map(map -> {
                    Object name = FunctionUtil.getByIgnoreCaseKey(map::get,
                            "Field", "Column", "Name");
                    Object type = FunctionUtil.getByIgnoreCaseKey(map::get, "Type");
                    if (name == null || type == null) {
                        throw new RuntimeException("unexpect desc result: " + map);
                    }
                    return JdbcColumnDef.builder().name(name.toString())
                            .type(type.toString()).build();
                })
                .collect(Collectors.toList());
    }
}