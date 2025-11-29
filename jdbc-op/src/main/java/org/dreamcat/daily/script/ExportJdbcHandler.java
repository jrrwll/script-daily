package org.dreamcat.daily.script;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.function.IConsumer;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.common.CliUtil;
import org.dreamcat.daily.script.module.JdbcModule;
import org.dreamcat.daily.script.module.RandomGenModule;

import java.sql.Connection;
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
@ArgParserType(allProperties = true, command = "export-jdbc")
public class ExportJdbcHandler extends BaseExportHandler {

    boolean columnQuota;
    boolean doubleQuota; // "c1" or `c2`
    @ArgParserField(value = {"y"})
    boolean yes;

    @ArgParserField(nested = true, nestedSuffix = "1")
    JdbcModule jdbc1;
    @ArgParserField(nested = true, nestedSuffix = "2")
    JdbcModule jdbc2;

    @ArgParserField(nested = true)
    RandomGenModule randomGen;

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
        randomGen.afterPropertySet();

        CliUtil.checkParameter(jdbc1.jdbcUrl, "-j1|--jdbc-url1");
        CliUtil.checkParameter(jdbc1.driverPaths, "--dp1|--driver-paths1");
        CliUtil.checkParameter(jdbc1.driverClass, "--dc1|--driver-class1");
        if (ObjectUtil.isEmpty(jdbc2.driverPaths)) jdbc2.driverPaths = jdbc1.driverPaths;
        if (ObjectUtil.isEmpty(jdbc2.driverClass)) jdbc2.driverClass = jdbc1.driverClass;
        if (yes) {
            CliUtil.checkParameter(jdbc2.jdbcUrl, "-j2|--jdbc-url2");
        }
    }

    @Override
    protected void readSource(IConsumer<Connection> f) throws Exception {
        jdbc1.run(f);
    }

    @Override
    protected void writeTarget(IConsumer<Connection> f) throws Exception {
        if (!yes) {
            super.writeTarget(f);
            return;
        }
        jdbc2.run(f);
    }

    @SneakyThrows
    @Override
    protected void handleRows(String database, String table,
            List<Map<String, Object>> rows, Map<String, JdbcColumnDef> columnMap,
            Connection targetConnection) {
        List<List<Object>> list = rows.stream().map(this::mapToRow)
                .collect(Collectors.toList());

        List<String> columnNames = new ArrayList<>(rows.get(0).keySet());
        List<String> typeNames = new ArrayList<>();
        for (String columnName : columnNames) {
            typeNames.add(columnMap.get(columnName).getType().toLowerCase());
        }

        String columnNameSql = StringUtil.join(",", columnNames, this::formatColumnName);
        String insertIntoSql;
        if (database != null) {
            insertIntoSql = String.format(
                    "insert into %s.%s(%s) values ", database, table, columnNameSql);
        } else {
            insertIntoSql = String.format(
                    "insert into %s(%s) values ", table, columnNameSql);
        }

        String sql = insertIntoSql + randomGen.generateValues(list, typeNames);
        if (verbose) System.out.println("write sql: " + sql);
        if (!yes) return;

        try (Statement statement = targetConnection.createStatement()) {
            long t = System.currentTimeMillis();
            int rowEffect = statement.executeUpdate(sql);
            System.out.printf("%d row effect, cost %.2fs%n",
                    rowEffect, (System.currentTimeMillis() - t) / 1000.);
        }
    }

    private List<Object> mapToRow(Map<String, Object> map) {
        List<Object> row = new ArrayList<>(map.size());
        row.addAll(map.values());
        return row;
    }

    public String formatColumnName(String columnName) {
        if (!columnQuota) return columnName;
        return StringUtil.escape(columnName, doubleQuota ? "\"" : "`");
    }
}