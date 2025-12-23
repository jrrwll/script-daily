package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;
import static org.dreamcat.common.util.RandomUtil.uuid32;

import lombok.SneakyThrows;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.common.BaseHandler;
import org.dreamcat.daily.script.common.CliUtil;
import org.dreamcat.daily.script.model.TypeInfo;
import org.dreamcat.daily.script.module.JdbcModule;
import org.dreamcat.daily.script.module.OutputModule;
import org.dreamcat.daily.script.module.SqlGenModule;

import java.sql.Connection;
import java.util.List;
import java.util.Set;

/**
 * @author Jerry Will
 * @version 2023-03-22
 */
@ArgParserType(allProperties = true, command = "insert-random")
public class InsertRandomHandler extends BaseHandler {

    @ArgParserField(position = 1)
    private String tableName = "t_" + StringUtil.reverse(uuid32()).substring(0, 8);
    @ArgParserField("i")
    private Set<String> ignoredColumns;
    @ArgParserField("P")
    private Set<String> partitionColumns;

    boolean columnQuota;
    boolean doubleQuota; // "c1" or `c2`
    String setEnumValues = "a,b,c,d";

    @ArgParserField({"b"})
    int batchSize = 1;
    @ArgParserField({"n"})
    int rowNum = randi(1, 76);
    @ArgParserField(value = {"y"})
    boolean yes; // execute sql or not actually
    boolean debug;
    @ArgParserField(firstChar = true)
    boolean help;

    @ArgParserField(nested = true)
    JdbcModule jdbc;
    @ArgParserField(nested = true)
    OutputModule output;
    @ArgParserField(nested = true)
    SqlGenModule randomGen;

    transient TypeTableHandler typeTableHandler;

    @SneakyThrows
    @Override
    public void run() throws Exception {
        CliUtil.checkParameter(tableName, "<tableName>");
        jdbc.run(this::handle);
    }

    protected void afterPropertySet() throws Exception {
        this.typeTableHandler = (TypeTableHandler) new TypeTableHandler()
                .rowNum(rowNum)
                .tableName(tableName)
                .columnQuota(columnQuota)
                .doubleQuota(doubleQuota)
                .setEnumValues(setEnumValues)
                .jdbc(jdbc)
                .output(output)
                .sqlGenModule(randomGen)
                .batchSize(batchSize)
                .yes(yes)
                .debug(debug);
        typeTableHandler.afterPropertySet();
    }

    void handle(Connection connection) throws Exception {
        Pair<List<String>, List<String>> pair = TypeInfo.getTypes(connection, tableName, ignoredColumns,
                partitionColumns);
        List<String> types = pair.first(), partitionTypes = pair.second();

        typeTableHandler
                .types(types)
                .partitionTypes(partitionTypes);

        List<String> sqlList = typeTableHandler.genSql().second();
        typeTableHandler.output(sqlList, connection);
    }
}