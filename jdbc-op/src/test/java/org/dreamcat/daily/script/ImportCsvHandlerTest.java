package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.CommandArgParser;
import org.dreamcat.common.util.ClassLoaderUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-06-27
 */
class ImportCsvHandlerTest extends BaseTest {

    @Test
    void testHelp() {
        Main.main("import-csv", "-h");
    }

    @Test
    void test() throws Exception {
        Main.main(
                "import-csv", "t_table_test", "-b", "3",
                "-F", ClassLoaderUtil.getResourceAsString("test.csv"),
                "-T", ClassLoaderUtil.getResourceAsString("mysql-text-types.txt")
        );
    }

    @Test
    void testJdbc1() throws Exception {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(
                "import-csv", "t_table_test", "-b", "3",
                "-F", ClassLoaderUtil.getResourceAsString("test.csv"),
                "-T", ClassLoaderUtil.getResourceAsString("sqlite-text-types.txt")
        ));
        args.addAll(Arrays.asList(
                "-j", "jdbc:sqlite:build/temp.sqlite",
                "--dc", "org.sqlite.JDBC", "--dp"
        ));
        args.addAll(sqliteDriverPath());
        CommandArgParser.run(Main.class, args);
    }

    @Test
    void testJdbc2() {
        Main.main("import-csv", "t_table_test", "-b", "3",
                "-f", "src/test/resources/test.csv",
                "-t", "src/test/resources/mysql-text-types.txt"
        );
    }
}
