package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.CommandArgParser;
import org.dreamcat.common.util.ClassLoaderUtil;
import org.dreamcat.common.util.StringUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-04-27
 */
class BatchTypeTableHandlerTest {

    @Test
    void testFile() throws Exception {
        List<String> args = new ArrayList<>(Arrays.asList("batch-type-table", "my_table_$i"));
        File file = new File("src/test/resources/batch.txt");
        if (file.exists()) {
            String filename = file.getCanonicalPath();
            args.addAll(Arrays.asList("-f", filename));
        } else {
            String fileContent = ClassLoaderUtil.getResourceAsString("batch.txt");
            args.addAll(Arrays.asList("-F", "'" + StringUtil.escape(fileContent, '\'') + "'"));
        }
        CommandArgParser.run(Main.class, args);
    }

    @Test
    void testTypes() {
        // 7个类型中分别取3-7
        //             2           1   0
        // 99 = 2**7 - 7 * 6 / 2 - 7 - 1
        Main.main("batch-type-table", "t_table_${i+100}",
                "-t", "boolean", "int", "bigint", "double", "timestamp", "date", "string",
                "-n", "0", "-r", "3");
    }

    @Test
    @Tag("integration")
    void testRollingFile() {
        // 127 / 20 = 7
        Main.main(
                "batch-type-table", "t_table_${i+100}",
                "-t", "boolean", "int", "bigint", "double", "timestamp", "date", "string",
                "--compact", "-m", "1-7", "-n", "0",
                "-R", System.getenv("HOME") + "/Downloads/output_${i+100}.sql",
                "-M", "20");
    }
}
