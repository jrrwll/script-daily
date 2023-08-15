package org.dreamcat.daily.script.module;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.json.YamlUtil;
import org.dreamcat.common.sql.SqlValueRandomGenerator;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ClassPathUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.model.ConverterInfo;
import org.dreamcat.daily.script.model.TypeInfo;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@ArgParserType(allProperties = true)
public class RandomGenModule {

    @ArgParserField("S")
    public String dataSourceType;
    String converterFile;
    // binary:cast($value as $type)
    Set<String> converters;
    @ArgParserField("A")
    boolean castAs;
    boolean enableNeg; // generate neg number for number types
    double nullRatio = 0;
    // like this 'ratio,rows', example: 0.5,10
    String rowNullRatio;

    transient final SqlValueRandomGenerator gen = new SqlValueRandomGenerator()
            .maxBitLength(1)
            .addEnumAlias("enum8") // clickhouse
            .addEnumAlias("enum16");
    transient RowNullRatioBasedGen rowNullRatioBasedGen;

    public void afterPropertySet() throws Exception {
        gen.enableNeg(enableNeg)
                .globalConvertor(this::convert);

        // builtin converters
        Map<String, List<ConverterInfo>> converterInfos = YamlUtil.fromJson(
                ClassPathUtil.getResourceAsString("converters.yaml"),
                new TypeReference<Map<String, List<ConverterInfo>>>() {
                });
        registerConvertors(converterInfos);
        // customer converters
        if (StringUtil.isNotEmpty(converterFile)) {
            converterInfos = YamlUtil.fromJson(new File(converterFile),
                    new TypeReference<Map<String, List<ConverterInfo>>>() {
                    });
            registerConvertors(converterInfos);
        }
        if (ObjectUtil.isNotEmpty(converters)) {
            for (String converter : converters) {
                String[] ss = converter.split(",", 2);
                if (ss.length != 2) {
                    throw new IllegalArgumentException("invalid converter: " + converter);
                }
                String type = ss[0], template = ss[1];
                registerConvertor(type, template);
            }
        }

        // value ratio
        if (ObjectUtil.isNotBlank(rowNullRatio)) {
            Pair<Double, Integer> pair = Pair.fromSep(rowNullRatio, ",",
                    Double::valueOf, Integer::valueOf);
            if (!pair.isFull()) {
                throw new IllegalArgumentException("invalid smartRowNullRatio: " + rowNullRatio);
            }
            double ratio = pair.first();
            int rows = pair.second();
            this.rowNullRatioBasedGen = new RowNullRatioBasedGen(ratio, rows);
        }
    }

    public void reset(int columns) {
        if (rowNullRatioBasedGen != null) {
            rowNullRatioBasedGen.reset(columns);
        }
    }

    public String generateLiteral(String typeName) {
        return gen.generateLiteral(typeName);
    }

    public String nullLiteral() {
        return gen.nullLiteral();
    }

    public String formatAsLiteral(Object value, TypeInfo typeInfo) {
        String literal = gen.formatAsLiteral(value);
        String convertedLiteral = gen.convertLiteral(literal, typeInfo.getTypeId());
        if (!Objects.equals(convertedLiteral, literal)) return convertedLiteral;
        if (castAs) return String.format("cast(%s as %s)", literal, typeInfo.getTypeName());
        return literal;
    }

    private String convert(String literal, String typeName) {
        if (nullRatio < 1 && nullRatio > 0) {
            if (Math.random() <= nullRatio) {
                return gen.nullLiteral();
            }
        }
        if (rowNullRatioBasedGen != null && rowNullRatioBasedGen.generate()) {
            return gen.nullLiteral();
        }
        return null;
    }

    private void registerConvertors(Map<String, List<ConverterInfo>> converterInfoMap) {
        Map<String, List<ConverterInfo>> map = new HashMap<>();
        converterInfoMap.forEach((k, v) -> {
            for (String ds : k.split(",")) {
                if (StringUtil.isNotEmpty(ds)) map.put(ds, v);
            }
        });
        List<ConverterInfo> converterInfos = map.get(dataSourceType);
        if (converterInfos == null) return;

        for (ConverterInfo converterInfo : converterInfos) {
            for (String type : converterInfo.getTypes()) {
                registerConvertor(type, converterInfo.getTemplate());
            }
        }
    }

    private void registerConvertor(String type, String template) {
        gen.registerConvertor((literal, typeName) -> InterpolationUtil.format(
                template, MapUtil.of("value", literal, "type", type)), type);
    }

    private static class RowNullRatioBasedGen {

        final int rows;
        final double ratio;
        int columns;
        int total;
        int offset;

        RowNullRatioBasedGen(double ratio, int rows) {
            this.ratio = ratio;
            this.rows = rows;
        }

        void reset(int columns) {
            this.columns = columns;
            this.total = columns * rows;
            this.offset = 0;
        }

        boolean generate() {
            if (total == 0) {
                throw new IllegalStateException("must call rest once before call generate");
            }
            if (offset >= total) {
                offset = 0;
            }
            if (offset++ < columns) {
                return Math.random() <= ratio;
            }
            return false;
        }
    }
}