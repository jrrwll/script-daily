package org.dreamcat.daily.script;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.dreamcat.common.argparse.SubcommandArgParser;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2023-04-27
 */
public class BatchTypeTableHandlerTest {

    @Test
    void testFile() throws Exception {
        // Arrays.asList(
        //         "-f", ClassPathUtil.getResource("batch.txt").toExternalForm());
        List<String> args = Arrays.asList(
                "batch-type-table", "-f",
                new File("src/test/resources/batch.txt").getCanonicalPath());
        SubcommandArgParser argParser = new SubcommandArgParser(App.class);
        argParser.run(args);
    }

    @Test
    void testTypes() {
        List<String> args = Arrays.asList(
                "batch-type-table",
                "-j", "jdbc:xxx:xxx", "-u", "myuser", "-p", "secret",
                "-t", "boolean", "int", "bigint", "double", "timestamp", "date", "string",
                "-o", "100", "-n", "0", "-r", "3"
        );
        SubcommandArgParser argParser = new SubcommandArgParser(App.class);
        argParser.run(args);
    }
}
