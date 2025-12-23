package org.dreamcat.daily.script;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.function.IConsumer;
import org.dreamcat.common.io.CsvUtil;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.daily.script.module.JdbcModule;

import java.io.File;
import java.sql.Connection;
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
@ArgParserType(allProperties = true, command = "export-csv")
public class ExportCsvHandler extends BaseExportHandler {

    @ArgParserField("o")
    private String outputFile = "$database/$table.csv";
    @ArgParserField(nested = true)
    JdbcModule jdbc;

    @Override
    protected void readSource(IConsumer<Connection> f) throws Exception {
        jdbc.run(f);
    }

    @SneakyThrows
    @Override
    protected void handleRows(String database, String table,
            List<Map<String, Object>> rows, Map<String, JdbcColumnDef> columnMap,
            Connection targetConnection) {
        String outputName = InterpolationUtil.format(outputFile,
                "database", database, "db", database,
                "table", table, "tb", table);
        File output = new File(outputName);

        List<List<String>> list = rows.stream().map(this::mapToRow)
                .collect(Collectors.toList());
        CsvUtil.write(list, output, true);
    }

    private List<String> mapToRow(Map<String, Object> map) {
        List<String> row = new ArrayList<>(map.size());
        for (Object value : map.values()) {
            if (value != null) {
                row.add(value.toString());
            } else {
                row.add("");
            }
        }
        return row;
    }
}