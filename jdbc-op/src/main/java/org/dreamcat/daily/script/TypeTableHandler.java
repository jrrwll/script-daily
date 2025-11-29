package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;
import static org.dreamcat.common.util.RandomUtil.uuid32;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.Pair;
import org.dreamcat.common.Triple;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.util.CollectionUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.model.TypeInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Jerry Will
 * @version 2023-03-30
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "type-table")
public class TypeTableHandler extends BaseDdlHandler {

    @ArgParserField(position = 1)
    private String tableName = "t_" + StringUtil.reverse(uuid32()).substring(0, 8);

    // one type per line: varchar(%d), decimal(%d, %d)
    @ArgParserField("f")
    private String file;
    @ArgParserField("F")
    private String fileContent;
    @ArgParserField("t")
    private List<String> types;
    @ArgParserField("P")
    private List<String> partitionTypes;
    @ArgParserField({"n"})
    int rowNum = randi(1, 76);

    @Override
    public void run() throws Exception {
        if (ObjectUtil.isBlank(file) && ObjectUtil.isEmpty(types) &&
                ObjectUtil.isEmpty(fileContent)) {
            throw new IllegalArgumentException(
                    "required arg: -f|--file <file> or -t|--types <t1> <t2>... "
                            + "or -F|--file-content <content>");
        }
        if (ObjectUtil.isNotBlank(file) || ObjectUtil.isNotBlank(fileContent)) {
            List<String> lines;
            if (ObjectUtil.isNotBlank(file)) {
                lines = FileUtil.readLines(file);
            } else {
                lines = Arrays.asList(fileContent.split("\n"));
            }
            types = lines.stream()
                    .filter(StringUtil::isNotBlank).map(String::trim)
                    .filter(it -> {
                        if (it.startsWith("@")) {
                            partitionTypes.add(it.substring(1));
                            return false;
                        }
                        return !it.startsWith("#");
                    }).map(String::trim).collect(Collectors.toList());
        }

        if (ObjectUtil.isEmpty(types)) {
            throw new IllegalArgumentException("at least one type is required in file " + file);
        }

        randomGen.reset(types.size());
        // debug
        if (debug) {
            Stream.concat(types.stream(), partitionTypes.stream()).distinct().forEach(type -> {
                type = new TypeInfo(type, setEnumValues).getTypeId();
                String raw = randomGen.generateLiteral(type);
                System.out.println(type + ": " + raw);
            });
        }
        jdbc.run(connection -> output(genSqlList(), connection));
    }

    public void reset() {
        randomGen.reset(types.size());
    }

    public List<String> genSqlList() {
        Pair<List<String>, List<String>> pair = genSql();
        return CollectionUtil.concatToList(pair.first(), pair.second());
    }

    // generate ddl & dml
    public Pair<List<String>, List<String>> genSql() {
        // generate
        Triple<List<String>, List<String>, List<String>> triple = genCreateTableSql(tableName,
                CollectionUtil.mapToList(types,
                        type -> new TypeInfo(type, setEnumValues)),
                CollectionUtil.mapToList(partitionTypes,
                        type -> new TypeInfo(type, setEnumValues)));

        List<String> ddlList = triple.first();
        List<String> columnNames = triple.second();
        List<String> partitionColumnNames = triple.third();

        // insert
        List<String> insertList = new ArrayList<>();
        String columnNameSql = StringUtil.join(",", columnNames, this::formatColumnName);
        String insertIntoSql = String.format(
                "insert into %s%s%%s values %%s;", tableName,
                CollectionUtil.isEmpty(partitionTypes) ? "(" + columnNameSql + ")" : "");
        while (rowNum > batchSize) {
            rowNum -= batchSize;
            insertList.add(String.format(insertIntoSql,
                    getPartitionValue(partitionColumnNames), getValues(batchSize)));
        }
        if (rowNum > 0) {
            insertList.add(String.format(insertIntoSql,
                    getPartitionValue(partitionColumnNames), getValues(rowNum)));
        }
        return Pair.of(ddlList, insertList);
    }

    private String getValues(int n) {
        return IntStream.range(0, n).mapToObj(i -> getOneValues())
                .collect(Collectors.joining(","));
    }

    private String getOneValues() {
        return "(" + types.stream().map(type -> {
            type = new TypeInfo(type, setEnumValues).getTypeId();
            return randomGen.generateLiteral(type);
        }).collect(Collectors.joining(",")) + ")";
    }

    private String getPartitionValue(List<String> partitionColumnNames) {
        if (CollectionUtil.isEmpty(partitionTypes)) return "";
        List<String> list = new ArrayList<>();
        for (int i = 0, size = partitionTypes.size(); i < size; i++) {
            TypeInfo typeInfo = new TypeInfo(partitionTypes.get(i), setEnumValues);
            String columnName = partitionColumnNames.get(i);
            list.add(columnName + "=" + randomGen.generateLiteral(typeInfo.getTypeId()));
        }
        return String.format(" partition(%s)", String.join(",", list));
    }
}
