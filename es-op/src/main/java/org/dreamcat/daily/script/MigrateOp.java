package org.dreamcat.daily.script;

import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.elasticsearch.EsQueryClient.ScrollIter;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.script.DelegateScriptEngine;
import org.dreamcat.common.text.PatternUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.common.BaseHandler;
import org.dreamcat.daily.script.common.CliUtil;

import javax.script.ScriptException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Create by tuke on 2021/3/22
 */
@ArgParserType(allProperties = true, command = "migrate")
public class MigrateOp extends BaseHandler {

    private String indexConverter;
    private String indexSettings;
    private String indexSettingsContent;
    private boolean exclude;
    @ArgParserField(firstChar = true)
    private List<Pattern> patterns;
    @ArgParserField(firstChar = true)
    private boolean merge;
    @ArgParserField(firstChar = true)
    private boolean force;
    private boolean abort;
    @ArgParserField({"y", "yes"})
    private boolean effect; // actually modify or not
    @ArgParserField("V")
    private boolean verbose;
    @ArgParserField(firstChar = true)
    private int batchSize = 1024;
    private int keepAlive = 60; // seconds

    @ArgParserField(nested = true, nestedPrefix = "source.")
    private EsModule source;
    @ArgParserField(nested = true, nestedPrefix = "target.")
    private EsModule target;

    private final DelegateScriptEngine scriptEngine = new DelegateScriptEngine();

    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
        source.afterPropertySet();
        target.afterPropertySet();

        if (effect) {
            indexSettingsContent = CliUtil.requireFileOrContent(
                    indexSettings, indexSettingsContent,
                    "--index-settings", "--index-settings-content", "--effect");
        }
    }

    @Override
    public void run() throws Exception {
        List<String> schemas = source.esIndexClient.getAllIndex();
        for (String schema : schemas) {
            if (!PatternUtil.match(schema, patterns, exclude)) {
                if (verbose) {
                    System.out.printf("skip unmatched index %s%n", schema);
                }
                continue;
            }
            try {
                handle(schema);
            } catch (Exception e) {
                System.err.printf("handle index %s, error occurred: %s%n", schema, e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                if (abort) {
                    System.out.println("operation is aborted by exception");
                    break;
                }
            }
        }
    }

    private void handle(String sourceSchema) {
        String targetSchema = this.getTargetSchema(sourceSchema);
        if (targetSchema == null) return;

        if (target.esIndexClient.existsIndex(targetSchema)) {
            // schema already exist
            if (force) {
                // force delete existing schemas
                System.out.printf("drop target index %s%n", targetSchema);
                if (effect) {
                    target.esIndexClient.deleteIndex(targetSchema);
                }
            } else {
                System.out.printf("index %s already exists in the target%n", targetSchema);
                // ignore when no-merge for existing schemas
                if (!merge) return;
            }
        } else {
            // schema doesn't exist, so create it
            Pair<String, String> pair = source.esIndexClient.getIndex(sourceSchema);
            String mappings = pair.first();

            if (verbose) {
                System.out.printf("create index %s, mappings=%s, settings=%s%n",
                        targetSchema, mappings, indexSettingsContent);
            } else {
                System.out.printf("create index %s%n", targetSchema);
            }
            if (effect) {
                target.esIndexClient.createIndex(targetSchema, mappings, indexSettingsContent);
            }
        }
        migrateSchema(sourceSchema, targetSchema);
    }

    protected void migrateSchema(String sourceSchema, String targetSchema) {
        long total = source.esQueryClient.count(sourceSchema);
        System.out.printf("migrate index %s to %s, find %d records%n",
                sourceSchema, targetSchema, total);
        if (total == 0) return;

        // huge index, then
        try (ScrollIter<Map<String, Object>> scrollIter = source.esQueryClient.scrollIter(
                sourceSchema, batchSize, keepAlive)) {
            while (scrollIter.hasNext()) {
                List<Map<String, Object>> list = scrollIter.next();
                if (list.isEmpty()) continue;

                Map<String, String> idJsonMap = list.stream().collect(Collectors.toMap(
                        it -> it.get("id").toString(), JsonUtil::toJson, (a, b) -> a));
                System.out.printf("migrate index %s to %s, bulk records: %s%n",
                        sourceSchema, targetSchema, idJsonMap);
                if (effect) {
                    target.esDocClient.bulkSave(targetSchema, idJsonMap);
                }
            }
        }
    }

    private String getTargetSchema(String sourceSchema) {
        if (ObjectUtil.isNotBlank(indexConverter)) {
            try {
                return scriptEngine.evalWith(indexConverter, sourceSchema);
            } catch (ScriptException e) {
                String message = String.format(
                        "failed to format schema %s with converter `%s`, %s",
                        sourceSchema, indexConverter, e.getMessage());
                throw new RuntimeException(message, e);
            }
        }
        return sourceSchema;
    }
}
