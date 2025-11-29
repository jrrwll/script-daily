package org.dreamcat.daily.script;

import org.dreamcat.common.util.ClassLoaderUtil;
import org.dreamcat.common.util.StringUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2023-04-27
 */
class BatchTypeTableHandlerTest {

    @Test
    void testHelp() {
        Main.main("batch-type-table", "-h");
    }

    @Test
    void testFile1() {
        Main.main("batch-type-table", "my_table_$i",
            "-f", "src/test/resources/batch.txt"
        );
    }

    @Test
    void testFile2() throws Exception {
        String fileContent = ClassLoaderUtil.getResourceAsString("batch.txt");
        Main.main(
                "batch-type-table", "my_table_$i",
                "-F", "'" + StringUtil.escape(fileContent, '\'') + "'"
        );
    }

    @Test
    void testTypes() {
        // 7个类型中分别取3-7
        //             2           1   0
        // 99 = 2**7 - 7 * 6 / 2 - 7 - 1
        Main.main(
                "batch-type-table", "t_table_${i+100}",
                "-t", "boolean", "int", "bigint", "double", "timestamp", "date", "string",
                "-n", "0", "-r", "3"
        );
    }

    @Test
    @Tag("integration")
    void testRollingFile() {
        // 127 / 20 = 7
        Main.main(
                "batch-type-table", "t_table_${i+100}",
                "-t", "boolean", "int", "bigint", "double", "timestamp", "date", "string",
                "--compact", "-m", "1-7", "-n", "0",
                "-R", "/build/output_${i+100}.sql",
                "-M", "20"
        );
    }
}
