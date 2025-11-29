package org.dreamcat.daily.script;

import org.dreamcat.common.util.ClassLoaderUtil;
import org.dreamcat.common.util.IteratorUtil;
import org.junit.jupiter.api.Test;

import java.sql.Driver;
import java.sql.DriverManager;

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
        String jdbcUrl = "jdbc:sqlite:build/temp.sqlite";
        boolean accept = false;
        for (Driver driver : IteratorUtil.asIterable(DriverManager.getDrivers())) {
            if (driver.acceptsURL(jdbcUrl)) {
                System.out.println("accepted by " + driver.getClass().getName());
                accept = true;
                break;
            }
        }
        if (!accept) {
            System.out.println("no suitable driver found for " + jdbcUrl);
            return;
        }

        Main.main(
                "import-csv", "t_table_test", "-b", "3",
                "-F",ClassLoaderUtil.getResourceAsString("test.csv"),
                "-T", ClassLoaderUtil.getResourceAsString("mysql-text-types.txt"),
                "-j", "jdbc:sqlite:build/temp.sqlite");
    }
}
