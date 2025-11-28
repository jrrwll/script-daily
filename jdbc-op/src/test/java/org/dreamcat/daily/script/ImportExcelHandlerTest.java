package org.dreamcat.daily.script;

import static org.dreamcat.common.excel.ExcelBuilder.sheet;
import static org.dreamcat.common.util.DateUtil.addDay;
import static org.dreamcat.common.util.DateUtil.ofDate;
import static org.dreamcat.common.util.RandomUtil.choose36;
import static org.dreamcat.common.util.RandomUtil.rand;
import static org.dreamcat.common.util.RandomUtil.randi;
import static org.dreamcat.common.util.RandomUtil.uuid32;

import lombok.Data;
import lombok.SneakyThrows;
import org.dreamcat.common.Pair;
import org.dreamcat.common.excel.ExcelWorkbook;
import org.dreamcat.common.excel.annotation.XlsHeader;
import org.dreamcat.common.excel.annotation.XlsSheet;
import org.dreamcat.common.excel.annotation.XlsStyle;
import org.dreamcat.common.excel.callback.FitWidthWriteCallback;
import org.dreamcat.common.excel.callback.HeaderCellStyleWriteCallback;
import org.dreamcat.common.excel.map.SimpleSheet;
import org.dreamcat.common.util.ClassLoaderUtil;
import org.dreamcat.common.util.SystemUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;

/**
 * @author Jerry Will
 * @version 2023-06-30
 */
class ImportExcelHandlerTest {

    static String homeDir = SystemUtil.getPropertyOrEnv("user.dir", "HOME", ".");
    static File filename = new File(homeDir, "Downloads/all_type.xlsx");

    @Test
    void testHelp() {
        Main.main(
                "import-excel", "-h"
        );
    }

    @SneakyThrows
    @Test
    void test() {
        if (!filename.exists()) {
            System.err.println(filename + " doesn't exist, you may run main first");
            return;
        }
        Main.main(
                "import-excel",
                "-b", "3", "--cast-as",
                "-f", filename.getAbsolutePath(),
                "-T", ClassLoaderUtil.getResourceAsString("mysql-text-types.txt"),
                "--sn", "t_table_1,t_table_2",
                "--cn", "c_int,c_double,c_string,c_bool,c_date,c_local_date,c_local_date_time,c_null", "*");
    }

    // create a excel file
    @SneakyThrows
    public static void main(String[] args) {
        SimpleSheet sheet1 = new SimpleSheet(Pojo.class);
        for (int i = 0; i < randi(1, 17); i++) {
            sheet1.addRow(new Pojo());
        }
        sheet1.addWriteCallback(new HeaderCellStyleWriteCallback().overwrite(true));
        sheet1.addWriteCallback(new FitWidthWriteCallback());

        SimpleSheet sheet2 = new SimpleSheet(sheet("Sheet Two")
                .cell("cell_a", 0, 0)
                .cell("cell_b", 0, 1)
                .finish());
        for (int i = 0; i < randi(1, 17); i++) {
            sheet2.addRow(Pair.of(uuid32(), addDay(new Date(), -i - 1)));
        }
        sheet2.setSchemeConverter(row-> {
            Pair<?, ?> pair = (Pair<?, ?>) row;
            return Arrays.asList(pair.first(),pair.second());
        });
        new ExcelWorkbook<>().addSheet(sheet1).addSheet(sheet2).writeTo(filename);
    }

    @XlsSheet(name = "Sheet One")
    @Data
    static class Pojo {

        @XlsHeader(header = "Cell int", style = @XlsStyle(fgColor = 2))
        int a = randi(128);
        @XlsHeader(header = "Cell Double", style = @XlsStyle(fgColor = 3))
        Double b = rand();
        @XlsHeader(header = "Cell String", style = @XlsStyle(fgColor = 4))
        String c = choose36(randi(3, 7));
        @XlsHeader(header = "Cell boolean", style = @XlsStyle(fgColor = 5))
        boolean d = rand() > 0.5;
        @XlsHeader(header = "Cell Date", style = @XlsStyle(
                fgColor = 6, dataFormat = "yyyy-MM-dd hh:mm:ss"))
        Date e = new Date(System.currentTimeMillis() + randi(-3 * 24 * 3600L, 3 * 24 * 3600L));
        @XlsHeader(header = "Cell LocalDate", style = @XlsStyle(
                fgColor = 7, dataFormat = "yyyy-MM-dd"))
        LocalDate f = ofDate(
                new Date(System.currentTimeMillis() + randi(-3 * 24 * 3600L, 3 * 24 * 3600L))).toLocalDate();
        @XlsHeader(header = "Cell LocalDateTime", style = @XlsStyle(
                fgColor = 10, dataFormat = "yyyy-MM-dd hh:mm:ss"))
        LocalDateTime g = ofDate(new Date(System.currentTimeMillis() + randi(-3 * 24 * 3600L, 3 * 24 * 3600L)));
        @XlsHeader(header = "null", style = @XlsStyle(fgColor = 30))
        String _null; // null
    }
}
