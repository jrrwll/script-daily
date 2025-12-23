package org.dreamcat.daily.script;

import org.dreamcat.common.io.FileUtil;
import org.dreamcat.daily.script.model.TypeInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;

/**
 * @author Jerry Will
 * @version 2023-07-04
 */
class TypeInfoTest {

    @Test
    void test() {
        testType("varchar(%d)", "varchar");
        testType("varchar(100)", "varchar");
        testType("decimal(%d, %d)", "decimal");
        testType("decimal(16, 6)", "decimal");
    }

    private void testType(String type, String expectColumnName) {
        TypeInfo typeInfo = new TypeInfo(type, null);
        System.out.println(typeInfo);
        assert typeInfo.getColumnName().equals(expectColumnName);
    }

    @Test
    @Tag("integration")
    void testJdbc() throws Exception {
        FileUtil.loadDotEnvFile();
        String url = System.getProperty("DATABASE_URL");
        try (Connection connection = DriverManager.getConnection(url)) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet rs = metaData.getColumns(connection.getCatalog(), null, "t_type_test", "%");
            while (rs.next()) {
                String typeName = rs.getString("TYPE_NAME");
                System.out.println(typeName);
            }
        }
    }
}
