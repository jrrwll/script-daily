package org.dreamcat.daily.script;

import org.dreamcat.common.util.ClassLoaderUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2023-06-27
 */
class ImportCsvHandlerTest {

    @Test
    void test() throws Exception {
        Main.main(
                "import-csv", "t_table_test", "-b", "3",
                "-F",ClassLoaderUtil.getResourceAsString("test.csv"),
                "-T", ClassLoaderUtil.getResourceAsString("mysql-text-types.txt"));
    }

    @Test
    void testJdbc() throws Exception {
        Main.main(
                "import-csv", "t_table_test", "-b", "3",
                "-F",ClassLoaderUtil.getResourceAsString("test.csv"),
                "-T", ClassLoaderUtil.getResourceAsString("mysql-text-types.txt"),
                "-j", "jdbc:sqlite:build/temp.sqlite");
    }
}
