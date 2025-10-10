package org.dreamcat.daily.script;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.daily.script.common.BaseHandler;
import org.dreamcat.daily.script.common.CliUtil;

/**
 * @author Jerry Will
 * @version 2021-06-15
 */
@ArgParserType(command = "compare-lines")
public class CompareLinesOp extends BaseHandler {

    @ArgParserField(position = 2)
    private File inputFile;
    @ArgParserField(position = 2)
    private File outputLeftFile;
    @ArgParserField(position = 1)
    private File outputRightFile;

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
        CliUtil.checkParameter(inputFile, "<inputFile>");
        CliUtil.checkParameter(outputLeftFile, "<outputLeftFile>");
        CliUtil.checkParameter(outputRightFile, "<outputRightFile>");
    }

    @Override
    public void run() throws Exception {
        List<String> jsons = FileUtil.readLines(inputFile);
        int size = jsons.size();
        if (size < 1) return;

        Map<String, Object> prev = JsonUtil.fromJsonObject(jsons.get(0)), next;
        for (int i = 1; i < size; i++) {
            next = JsonUtil.fromJsonObject(jsons.get(i));
            Map<String, Object> leftDiff = new LinkedHashMap<>();
            Map<String, Object> rightDiff = new LinkedHashMap<>();
            MapUtil.compare(prev, next, leftDiff, rightDiff);
            FileUtil.write(outputLeftFile, JsonUtil.toJson(leftDiff) + '\n', true);
            FileUtil.write(outputRightFile, JsonUtil.toJson(rightDiff) + '\n', true);
            prev = next;
        }
    }
}
