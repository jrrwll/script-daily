package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;

import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.math.CombinationUtil;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ArrayUtil;
import org.dreamcat.common.util.CollectionUtil;
import org.dreamcat.common.util.NumberUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Jerry Will
 * @version 2023-06-06
 */
@ArgParserType(allProperties = true, command = "batch-type-table")
public class BatchTypeTableHandler extends BaseDdlHandler {

    @ArgParserField(position = 1)
    private String tableName = "t_table_$i";

    @ArgParserField("f")
    private String file;
    @ArgParserField("F")
    private String fileContent;
    @ArgParserField("t")
    List<String> combinationTypes;
    @ArgParserField({"m"})
    Set<String> combinationMode = Collections.singleton("3-16");
    @ArgParserField({"r"})
    int combinationRepeat = 1;

    boolean ignoreError; // ignore error when any TypeTableHandler failed
    @ArgParserField({"n"})
    int rowNum = randi(1, 76);

    transient TypeTableHandler typeTableHandler;
    transient List<List<String>> typesList;

    @Override
    public void run() throws Exception {
        this.afterPropertySet();
        jdbc.run(this::handle);
    }

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();

        this.typeTableHandler = (TypeTableHandler) new TypeTableHandler()
                .rowNum(rowNum)
                .columnNameTemplate(columnNameTemplate)
                .partitionColumnNameTemplate(partitionColumnNameTemplate)
                .columnQuota(columnQuota)
                .doubleQuota(doubleQuota)
                .commentAlone(commentAlone)
                .columnCommentSql(columnCommentSql)
                .tableSuffixSql(tableSuffixSql)
                .extraColumnSql(extraColumnSql)
                .setEnumValues(setEnumValues)
                .jdbc(jdbc)
                .output(output)
                .sqlGenModule(sqlGenModule)
                .debug(debug)
                .batchSize(batchSize)
                .yes(yes);
        typeTableHandler.afterPropertySet();

        if (ObjectUtil.isEmpty(file) && ObjectUtil.isBlank(fileContent) &&
                ObjectUtil.isEmpty(combinationTypes)) {
            throw new IllegalArgumentException(
                    "required arg: -f|--file <file> or -F|--file-content <content> or -t|--types <t1> <t2>...");
        }

        if (ObjectUtil.isNotEmpty(file) || ObjectUtil.isNotBlank(fileContent)) {
            List<String> lines;
            if (ObjectUtil.isNotEmpty(file)) {
                lines = FileUtil.readLines(file);
            } else {
                lines = Arrays.asList(fileContent.split("\n"));
            }
            typesList = lines.stream()
                    .filter(StringUtil::isNotBlank)
                    .map(String::trim)
                    .filter(it -> !it.startsWith("#"))
                    .map(line -> ArrayUtil.mapToList(line.split(";"), String::trim))
                    .collect(Collectors.toList());
        } else {
            int typeCount = combinationTypes.size();
            if (typeCount > 30) {
                throw new IllegalArgumentException("types is too much, must <= 30, but: " + typeCount);
            }
            Set<Integer> combinationNums = new HashSet<>();
            for (String c : combinationMode) {
                String[] ss = c.split("-");
                if (ss.length > 1) {
                    IntStream.rangeClosed(Integer.parseInt(ss[0]), Integer.parseInt(ss[1]))
                            .forEach(combinationNums::add);
                } else {
                    combinationNums.add(Integer.parseInt(c));
                }
            }

            typesList = new ArrayList<>();
            for (int combinationNum : combinationNums) {
                if (combinationNum > typeCount) continue;
                List<int[]> all = CombinationUtil.all(combinationNum, typeCount);
                for (int[] indexes : all) {
                    List<String> types = new ArrayList<>(combinationNum);
                    for (int i : indexes) {
                        types.add(combinationTypes.get(i));
                    }
                    // repeat it
                    int repeat = NumberUtil.limitRange(combinationRepeat, 1, 100);
                    for (int i = 0; i < repeat; i++) {
                        typesList.add(types);
                    }
                }
            }
        }
    }

    void handle(Connection connection) {
        int size = typesList.size();
        for (int i = 1; i <= size; i++) {
            List<String> types = typesList.get(i - 1);
            try {
                Pair<List<String>, List<String>> pair = CollectionUtil.partitioningBy(
                        types, type -> !type.startsWith("@"));
                handleOne(connection, pair.first(), pair.second(), i);
            } catch (Exception e) {
                if (!ignoreError) {
                    System.err.printf("error handle %s: %s%n", types, e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    private void handleOne(Connection connection, List<String> types, List<String> partitionTypes, int index)
            throws Exception {
        String name = InterpolationUtil.formatEl(tableName, "i", index, "index", index);
        typeTableHandler.tableName(name)
                .types(types)
                .partitionTypes(partitionTypes)
                .columnNameCounter(new HashMap<>())
                .partitionColumnNameCounter(new HashMap<>());
        typeTableHandler.reset();
        List<String> sqlList = typeTableHandler.genSqlList();
        typeTableHandler.output(sqlList, connection);
    }
}
