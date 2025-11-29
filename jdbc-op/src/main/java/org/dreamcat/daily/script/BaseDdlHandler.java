package org.dreamcat.daily.script;

import static org.dreamcat.common.util.ListUtil.getOrNull;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.MutableInt;
import org.dreamcat.common.Pair;
import org.dreamcat.common.Triple;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ListUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.common.BaseHandler;
import org.dreamcat.daily.script.model.TypeInfo;
import org.dreamcat.daily.script.module.JdbcModule;
import org.dreamcat.daily.script.module.OutputModule;
import org.dreamcat.daily.script.module.RandomGenModule;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2023-06-27
 */
@Setter
@Accessors(fluent = true)
public abstract class BaseDdlHandler extends BaseHandler {

    @ArgParserField("cnt")
    String columnNameTemplate;
    @ArgParserField("pcnt")
    String partitionColumnNameTemplate;
    boolean columnQuota;
    // comment on column, or c int comment
    // default use mysql column comment style
    boolean commentAlone;
    // such as: comment on column $table.$column is 'Column Type: $type'
    // or 'Column Type: $type' if commentAlone is ture
    @ArgParserField("c")
    String columnCommentSql;
    // postgresql, mysql, oracle, db2, sqlserver, and so on
    String tableSuffixSql;
    String extraColumnSql;
    boolean doubleQuota; // "c1" or `c2`
    String setEnumValues = "a,b,c,d";
    @ArgParserField(value = {"y"})
    boolean yes; // execute sql or not actually
    boolean debug;
    @ArgParserField(firstChar = true)
    boolean help;
    @ArgParserField({"b"})
    int batchSize = 1;

    @ArgParserField(nested = true)
    JdbcModule jdbc;
    @ArgParserField(nested = true)
    OutputModule output;
    @ArgParserField(nested = true)
    RandomGenModule randomGen;

    transient Map<String, MutableInt> columnNameCounter = new HashMap<>();
    transient Map<String, MutableInt> partitionColumnNameCounter = new HashMap<>();

    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
        randomGen.afterPropertySet();
        if (ObjectUtil.isBlank(columnNameTemplate)) {
            columnNameTemplate = getDefaultColumnName();
        }
        if (ObjectUtil.isBlank(partitionColumnNameTemplate)) {
            partitionColumnNameTemplate = getDefaultPartitionColumnName();
        }

        if (Arrays.asList("pg", "postgres", "postgresql").contains(randomGen.dataSourceType)) {
            if (StringUtil.isNotBlank(columnCommentSql) && !columnCommentSql.trim().startsWith("comment on column")) {
                columnCommentSql = String.format("comment on column $table.$column is '%s'", columnCommentSql);
                commentAlone = true; // since use postgres style
                doubleQuota = true;
            }
        }

    }

    protected String getDefaultColumnName() {
        return "c_$name";
    }

    protected String getDefaultPartitionColumnName() {
        return "p_$name";
    }

    public String formatColumnName(String columnName) {
        if (!columnQuota) return columnName;
        return StringUtil.escape(columnName, doubleQuota ? "\"" : "`");
    }

    // return createTableSql, columnNames, partitionColumnNames
    public Triple<List<String>, List<String>, List<String>> genCreateTableSql(
            String tableName, List<TypeInfo> typeInfos,
            List<TypeInfo> partitionTypeInfos) {
        List<String> ddlList = new ArrayList<>();

        String sep = output.compact ? " " : "\n";
        StringBuilder createTableSql = new StringBuilder();
        createTableSql.append("create table ").append(tableName).append(" (").append(sep);
        if (StringUtil.isNotBlank(extraColumnSql)) {
            createTableSql.append("    ").append(extraColumnSql).append(",").append(sep);
        }

        // column
        int columnCount = typeInfos.size();
        List<String> columnDefSqlList = new ArrayList<>(columnCount);
        List<String> columnCommentSqlList = new ArrayList<>(columnCount);

        List<String> columnNames = new ArrayList<>(columnCount);
        for (TypeInfo typeInfo : typeInfos) {
            String columnDefSql = getColumnDefSqlAndFillComment(
                    typeInfo, tableName, columnNameTemplate,
                    columnNames, columnCommentSqlList, columnNameCounter);
            columnDefSqlList.add(columnDefSql);
        }
        createTableSql.append(String.join("," + sep, columnDefSqlList))
                .append(sep).append(")");

        // partition
        int partitionColumnCount = partitionTypeInfos.size();
        List<String> partitionColumnDefSqlList = new ArrayList<>(partitionColumnCount);

        List<String> partitionColumnNames = new ArrayList<>(partitionColumnCount);
        for (TypeInfo partitionTypeInfo : partitionTypeInfos) {
            String partitionColumnDefSql = getColumnDefSqlAndFillComment(
                    partitionTypeInfo, tableName, partitionColumnNameTemplate,
                    partitionColumnNames, columnCommentSqlList, partitionColumnNameCounter);
            partitionColumnDefSqlList.add(partitionColumnDefSql);
        }
        if (ObjectUtil.isNotEmpty(partitionColumnDefSqlList)) {
            createTableSql.append(" partitioned by (").append(sep)
                    .append(String.join("," + sep, partitionColumnDefSqlList))
                    .append(sep).append(")");
        }

        // suffix
        if (StringUtil.isNotBlank(tableSuffixSql)) {
            createTableSql.append(" ").append(tableSuffixSql);
        }
        createTableSql.append(";");
        ddlList.add(createTableSql.toString());

        for (String s : columnCommentSqlList) {
            if (!s.endsWith(";")) s += ";";
            ddlList.add(s);
        }

        return Triple.of(ddlList, columnNames, partitionColumnNames);
    }

    private String getColumnDefSqlAndFillComment(
            TypeInfo typeInfo, String tableName, String columnTemplate,
            List<String> columnNames, List<String> columnCommentSqlList,
            Map<String, MutableInt> counter) {
        String col = typeInfo.computeColumnName(columnTemplate, counter);
        columnNames.add(col);

        String columnDefSql = formatColumnName(col) + " " + typeInfo.getTypeName();
        if (!output.compact) columnDefSql = "    " + columnDefSql;

        if (StringUtil.isNotBlank(columnCommentSql)) {
            Map<String, Object> ctx = MapUtil.of(
                    "table", tableName, "column", col, "type", typeInfo.getTypeName());
            String comment = InterpolationUtil.format(columnCommentSql, ctx);
            comment = StringUtil.escape(comment, "'");
            if (commentAlone) {
                ctx.put("comment", comment);
                columnCommentSqlList.add(InterpolationUtil.format(columnCommentSql, ctx));
            } else {
                columnDefSql = String.format("%s comment '%s'", columnDefSql, comment);
            }
        }
        return columnDefSql;
    }

    public void output(List<String> sqlList, Connection connection) throws Exception {
        if (!yes) {
            output.run(sqlList);
            return;
        }

        try (Statement statement = connection.createStatement()) {
            output.run(sqlList);
            for (String sql : sqlList) {
                statement.execute(sql);
            }
        }
    }

    public Pair<List<String>, List<String>> genSql(String tableName, List<List<Object>> rows,
            List<TypeInfo> typeInfos) {
        // create
        Triple<List<String>, List<String>, List<String>> triple = genCreateTableSql(
                tableName, typeInfos, Collections.emptyList());
        List<String> ddlList = triple.first();
        List<String> columnNames = triple.second();

        // insert
        List<String> insertList = new ArrayList<>();
        String columnNameSql = StringUtil.join(
                ",", columnNames,
                this::formatColumnName);
        String insertIntoSql = String.format(
                "insert into %s(%s) values %%s;", tableName, columnNameSql);

        int pageNum = 1;
        List<List<Object>> list;
        while (!(list = ListUtil.subList(rows, pageNum++, batchSize)).isEmpty()) {
            String valueSql = getValues(list, typeInfos);
            if (valueSql == null) continue;
            insertList.add(String.format(insertIntoSql, valueSql));
        }

        return Pair.of(ddlList, insertList);
    }

    // (%s,%s,%s),(%s,%s,%s),(%s,%s,%s)
    private String getValues(List<List<Object>> rows, List<TypeInfo> typeInfos) {
        int count = typeInfos.size();
        List<String> valueSqlList = new ArrayList<>();
        for (List<Object> row : rows) {
            if (row == null) {
                System.err.println("found a null row");
                continue;
            }
            List<String> value = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Object cell = getOrNull(row, i);
                String literal = randomGen.formatAsLiteral(cell, typeInfos.get(i));
                value.add(literal);
            }
            valueSqlList.add("(" + String.join(",", value) + ")");
        }
        if (valueSqlList.isEmpty()) return null;
        else return String.join(",", valueSqlList);
    }
}
