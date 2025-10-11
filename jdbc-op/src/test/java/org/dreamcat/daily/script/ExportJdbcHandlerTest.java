package org.dreamcat.daily.script;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2023-08-21
 */
class ExportJdbcHandlerTest {

    private static final String home = System.getenv("HOME");

    @Test
    void testHelp() throws Exception {
        Main.main(new String[]{
                "export-jdbc", "-h"
        });
    }

    @Test
    @Tag("integration")
    void testMysql() throws Exception {
        Main.main(new String[]{
                "export-jdbc", "--databases", "test",
                "--j1", "jdbc:mysql://127.0.0.1:3306", "--u1", "root", "--p1", "root",
                "--dp1",
                home + "/.m2/repository/mysql/mysql-connector-java/8.0.27",
                "--dc1", "com.mysql.cj.jdbc.Driver",
                "--j2", "jdbc:mysql://127.0.0.1:3307", "--u2", "root", "--p2", "root",
                "-S", "mysql", "--use-show", "--verbose"
                // , "-y"
        });
    }
}
