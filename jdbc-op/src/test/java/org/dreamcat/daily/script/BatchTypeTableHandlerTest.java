package org.dreamcat.daily.script;

import java.io.File;
import org.dreamcat.common.argparse.SubcommandArgParser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2023-04-27
 */
@Tag("integration")
class BatchTypeTableHandlerTest {

    @Test
    void testFile() throws Exception {
        new SubcommandArgParser(Main.class).run(
                "batch-type-table", "my_table_$i","-f",
                new File("src/test/resources/batch.txt").getCanonicalPath());
    }

    @Test
    void testTypes() {
        // 7个类型中分别取3-7
        //             2           1   0
        // 99 = 2**7 - 7 * 6 / 2 - 7 - 1
        new SubcommandArgParser(Main.class).run(
                "batch-type-table", "t_table_${i+100}",
                "-t", "boolean", "int", "bigint", "double", "timestamp", "date", "string",
                "-n", "0", "-r", "3");
    }

    @Test
    void testRollingFile() {
        // 127 / 20 = 7
        new SubcommandArgParser(Main.class).run(
                "batch-type-table", "t_table_${i+100}",
                "-t", "boolean", "int", "bigint", "double", "timestamp", "date", "string",
                "--compact", "-m", "1-7","-n", "0",
                "-R", System.getenv("HOME") + "/Downloads/output_${i+100}.sql",
                "-M", "20");
    }
}
