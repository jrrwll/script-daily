package org.dreamcat.daily.script;

import org.dreamcat.daily.script.model.TypeInfo;
import org.junit.jupiter.api.Test;

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
}
