package org.dreamcat.daily.script;

import org.dreamcat.common.util.ExceptionUtil;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * @author Jerry Will
 * @version 2025-10-11
 */
public class JDBCDriverTest {

    static final List<String> jdbcUrls = Arrays.asList(
            "jdbc:mysql://localhost/",
            "jdbc:postgresql://localhost/",
            "jdbc:sqlite:build/temp.sqlite",
            "jdbc:h2:./build/temp.h2",
            "jdbc:sqlserver://localhost/"
    );

    @Test
    void test() {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            System.out.println(driver.getClass().getName());
        }
        System.out.println();

        for (String jdbcUrl : jdbcUrls) {
            try (Connection connection = DriverManager.getConnection(
                    jdbcUrl, new Properties())) {
                System.out.println(connection);
                if (connection != null) {
                    System.out.println(connection.getClass().getName());
                }
            } catch (Exception e) {
                System.err.println(ExceptionUtil.getRootCause(e).getMessage());
            }
        }
    }
}
