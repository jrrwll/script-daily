package org.dreamcat.daily.script.module;

import java.io.IOException;
import java.util.List;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ObjectUtil;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@ArgParserType(allProperties = true)
public class OutputModule {

    public boolean compact;

    @ArgParserField({"R"})
    String rollingFile; // such as: output_${i+100}.sql
    @ArgParserField({"M"})
    int rollingFileMaxSqlCount = Integer.MAX_VALUE;

    transient int sqlCountCounter;
    transient int rollingFileIndex = 1;

    public void run(List<String> sqlList) throws IOException {
        int size = sqlList.size();
        for (int i = 0; i < size; i++) {
            String sql = sqlList.get(i);
            if (compact) {
                if (i > 0) System.out.print(" ");
                System.out.print(sql);
            } else {
                System.out.println(sql);
            }
        }
        if (compact) System.out.println();

        if (ObjectUtil.isEmpty(rollingFile)) return;

        int expect = rollingFileMaxSqlCount - sqlCountCounter;
        int offset = 0;
        while (expect < size) {
            List<String> subSqlList = sqlList.subList(offset, expect);
            String blockFile = InterpolationUtil.formatEl(rollingFile, "i", rollingFileIndex);
            FileUtil.write(blockFile, String.join("\n", subSqlList) + "\n", true);
            offset += expect;
            size -= expect;
            expect = rollingFileMaxSqlCount;
            sqlCountCounter = 0;
            rollingFileIndex++;
        }
        if (size > 0) {
            sqlCountCounter += size;
            List<String> subSqlList = sqlList.subList(offset, sqlList.size());
            String blockFile = InterpolationUtil.formatEl(rollingFile, "i", rollingFileIndex);
            FileUtil.write(blockFile, String.join("\n", subSqlList) + "\n", true);
        }
    }
}
