package org.dreamcat.daily.script;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.MutableInt;
import org.dreamcat.common.Pair;
import org.dreamcat.common.Triple;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.CsvUtil;
import org.dreamcat.common.text.TextValueType;
import org.dreamcat.common.util.CollectionUtil;
import org.dreamcat.common.util.ListUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.model.TypeInfo;
import org.dreamcat.daily.script.module.TextTypeModule;

import java.io.File;
import java.io.StringReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2023-06-17
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "import-csv")
public class ImportCsvHandler extends BaseDdlHandler {

    @ArgParserField(position = 1)
    private String tableName;

    // partition is unsupported
    @ArgParserField("f")
    private String file;
    @ArgParserField("F")
    private String fileContent;
    @ArgParserField("E")
    private boolean existing;
    @ArgParserField("nc")
    private List<String> nullColumns;

    private boolean tsv;
    private boolean emptyStringAsNull;

    @ArgParserField(nested = true)
    TextTypeModule textType;

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
        if (ObjectUtil.isEmpty(file) && ObjectUtil.isBlank(fileContent)) {
            throw new IllegalArgumentException(
                    "required arg: -f|--file <file> or -F|--file-content <content>");
        }
        if (!existing) {
            if (debug) {
                System.out.println("I will require the textTypes args since --existing isn't passed");
            }
            textType.afterPropertySet();
        }
    }

    protected String getDefaultColumnName() {
        return "$name";
    }

    protected String getDefaultPartitionColumnName() {
        return "$name";
    }

    @Override
    protected boolean isNullableColumn(String columnName) {
        return ObjectUtil.isNotEmpty(nullColumns) && nullColumns.contains(columnName);
    }

    @Override
    public void run() throws Exception {
        List<List<String>> rows;
        if (ObjectUtil.isNotEmpty(file)) {
            if (tsv) {
                rows = CsvUtil.readTsv(new File(file));
            } else {
                rows = CsvUtil.read(new File(file));
            }
        } else {
            if (tsv) {
                rows = CsvUtil.readTsv(new StringReader(fileContent));
            } else {
                rows = CsvUtil.read(new StringReader(fileContent));
            }
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("require some data in your csf file");
        }
        if (rows.size() < 2) {
            throw new IllegalArgumentException("require at least two rows in your data file");
        }
        int headerWidth = rows.get(0).size();
        if (rows.stream().anyMatch(row -> row.size() < headerWidth)) {
            throw new IllegalArgumentException("require same column width in your data file");
        }

        jdbc.run(connection -> this.handle(connection, rows));
    }

    protected void handle(Connection connection, List<List<String>> rows) throws Exception {
        List<String> header = rows.get(0);
        rows = rows.subList(1, rows.size());

        List<TypeInfo> typeInfos;
        if (!existing) {
            List<String> types = getTypesByData(rows, header);
            typeInfos = types.stream().map(type -> new TypeInfo(type, setEnumValues))
                    .collect(Collectors.toList());
        } else {
            // table already exists
            Pair<List<String>, List<String>> pair = TypeInfo.getTypes(connection, tableName);
            List<String> types = pair.first();
            Map<String, TypeInfo> typeInfoMap = types.stream().map(type -> new TypeInfo(type, setEnumValues))
                    .collect(Collectors.toMap(TypeInfo::getColumnName, Function.identity()));
            typeInfos = new ArrayList<>();
            for (String columnName : header) {
                TypeInfo typeInfo = typeInfoMap.get(columnName);
                if (typeInfo == null) {
                    throw new IllegalArgumentException("column `" + columnName + "` doesn't exist in table " + tableName);
                }
                typeInfos.add(typeInfo);
            }
        }

        List<String> sqlList = genSqlList(rows, typeInfos);
        output(sqlList, connection);
    }

    public List<String> genSqlList(List<List<String>> rows, List<TypeInfo> typeInfos) {
        Pair<List<String>, List<String>> pair = genSql(rows, typeInfos);
        return CollectionUtil.concatToList(pair.first(), pair.second());
    }

    public Pair<List<String>, List<String>> genSql(List<List<String>> rows, List<TypeInfo> typeInfos) {
        // create
        Triple<List<String>, List<String>, List<String>> triple = genCreateTableSql(tableName, typeInfos,
                Collections.emptyList());
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
        List<List<String>> list;
        while (!(list = ListUtil.subList(rows, pageNum++, batchSize)).isEmpty()) {
            insertList.add(String.format(insertIntoSql, getValues(list, typeInfos)));
        }

        return Pair.of(ddlList, insertList);
    }

    // (%s,%s,%s),(%s,%s,%s),(%s,%s,%s)
    private String getValues(List<List<String>> rows, List<TypeInfo> typeInfos) {
        int count = typeInfos.size();
        List<String> valueSqlList = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> value = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String cell = row.get(i);
                String literal = getOneValue(cell, typeInfos.get(i));
                value.add(literal);
            }
            valueSqlList.add("(" + String.join(",", value) + ")");
        }
        return String.join(",", valueSqlList);
    }

    private String getOneValue(String value, TypeInfo typeInfo) {
        if (emptyStringAsNull && ObjectUtil.isEmpty(value)) {
            return sqlGenModule.nullLiteral();
        }
        Object sqlValue = TextValueType.parse(value);
        return sqlGenModule.formatAsLiteral(sqlValue, typeInfo);
    }

    // data detect
    private List<String> getTypesByData(List<List<String>> rows, List<String> header) {
        int headerWidth = header.size();
        List<Map<TextValueType, MutableInt>> typeCounter = new ArrayList<>(headerWidth);
        header.forEach(it -> {
            typeCounter.add(new LinkedHashMap<>());
        });

        for (List<String> row : rows) {
            for (int i = 0; i < headerWidth; i++) {
                String value = row.get(i);
                TextValueType textValueType = TextValueType.detect(value);
                typeCounter.get(i).computeIfAbsent(textValueType, k -> new MutableInt(0))
                        .incrAndGet();
            }
        }

        // compute top text-type
        List<String> types = new ArrayList<>();
        for (int i = 0; i < typeCounter.size(); i++) {
            Map<TextValueType, MutableInt> m = typeCounter.get(i);
            Entry<TextValueType, MutableInt> topEntry = null;
            for (Entry<TextValueType, MutableInt> entry : m.entrySet()) {
                if (topEntry == null || entry.getValue().compareTo(topEntry.getValue()) > 0) {
                    topEntry = entry;
                }
            }
            if (topEntry == null) {
                throw new IllegalArgumentException("no enough data to detect text-type for column " + (i + 1));
            }
            TextValueType textValueType = topEntry.getKey();
            String type = textType.computeCandidateType(textValueType);

            String alias = header.get(i);
            types.add(type + ": " + alias);
        }
        return types;
    }
}
