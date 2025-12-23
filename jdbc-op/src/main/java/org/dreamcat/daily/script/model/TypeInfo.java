package org.dreamcat.daily.script.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dreamcat.common.MutableInt;
import org.dreamcat.common.Pair;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.sql.JdbcUtil;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.CollectionUtil;
import org.dreamcat.common.util.ObjectUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2023-05-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeInfo {

    private String columnName; // sql column
    private String typeName; // sql type
    private String typeId; // generator
    private boolean notNull;

    // type: such as `$type` or `$type: $columnName`
    public TypeInfo(String type, String setEnumValues) {
        String[] tt = type.split(":");
        if (tt.length > 1) {
            type = tt[0].trim();
            this.columnName = tt[1].trim();
        }
        if (Arrays.asList("set", "enum", "enum8", "enum16").contains(type.toLowerCase())) {
            if (ObjectUtil.isEmpty(setEnumValues)) {
                this.typeName = this.typeId = type;
            } else {
                this.typeName = this.typeId = String.format("%s(%s)", type,
                        Arrays.stream(setEnumValues.split(","))
                                .map(it -> "'" + it + "'")
                                .collect(Collectors.joining(",")));
            }
            if (this.columnName == null) this.columnName = type;
            return;
        }

        int d = type.split("%d").length;
        if (d == 1) {
            this.typeName = type;
        } else if (d == 2) {
            this.typeName = type.replaceAll("%d", "16");
        } else if (d == 3) {
            this.typeName = type.replaceFirst("%d", "16")
                    .replaceFirst("%d", "6");
        } else {
            throw new IllegalArgumentException("invalid type: " + type);
        }
        this.typeId = typeName.toLowerCase()
                .replaceAll("\\(\\d+\\)", "")
                .replaceAll("\\(\\d+, *\\d+\\)", "")
                .replace("(", "")
                .replace(")", "")
                .replaceAll(",[ ]*", "");
        if (this.columnName == null) {
            this.columnName = this.typeId
                    .replace(' ', '_')
                    .replace(',', '_')
                    .replace("(", "_")
                    .replace(")", "")
                    .replace("'", "");
        }
    }

    public String computeColumnName(String columnNameTemplate, Map<String, MutableInt> columnNameCounter) {
        int index = columnNameCounter.computeIfAbsent(columnName, k -> new MutableInt(0)).incrAndGet();
        return InterpolationUtil.format(columnNameTemplate,
                "name", columnName, "type", typeName,
                "i", index + "", "index", index + "");
    }

    public static Pair<List<String>, List<String>> getTypes(
            Connection connection, String tableName)
            throws SQLException {
        return getTypes(connection, tableName,
                Collections.emptySet(), Collections.emptySet());
    }

    public static Pair<List<String>, List<String>> getTypes(
            Connection connection, String tableName,
            Set<String> ignoredColumns, Set<String> partitionColumns) throws SQLException {
        String s = null, t = tableName;
        if (tableName.contains(".")) {
            String[] ss = tableName.split("\\.", 2);
            s = ss[0];
            t = ss[1];
        }
        List<JdbcColumnDef> columns = JdbcUtil.getColumns(connection, s, t);
        columns = columns.stream().filter(column -> !ignoredColumns.contains(column.getName()))
                .collect(Collectors.toList());

        Pair<List<JdbcColumnDef>, List<JdbcColumnDef>> pair = CollectionUtil.partitioningBy(
                columns, column -> !partitionColumns.contains(column.getName()));

        List<String> types = CollectionUtil.mapToList(pair.first(), column -> {
            String columnName = column.getName();
            String type = column.getType();
            return type + ":" + columnName;
        });
        List<String> partitionTypes = CollectionUtil.mapToList(pair.second(), column -> {
            String columnName = column.getName();
            String type = column.getType();
            return type + ":" + columnName;
        });
        return Pair.of(types, partitionTypes);
    }
}
