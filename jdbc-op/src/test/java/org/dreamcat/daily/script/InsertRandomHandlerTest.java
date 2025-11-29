package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.CommandArgParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2025-11-29
 */
class InsertRandomHandlerTest extends BaseTest {

    @Test
    void testHelp() {
        Main.main("import-random", "-h");
    }

    @Test
    void testSqlite() {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(
                "insert-random", "my_table", "-i", "deleted",
                "-P", "data_month", "-S", "postgres",
                "--column-quota", "-b", "5", "-n", "13", "--debug"
        ));
        args.addAll(Arrays.asList(
                "-j", "jdbc:sqlite:build/temp.sqlite",
                "--dc", "org.h2.Driver", "--dp"
        ));
        args.addAll(h2DriverPath());
        CommandArgParser.run(Main.class, args);
    }

}
