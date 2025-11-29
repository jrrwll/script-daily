package org.dreamcat.daily.script.module;

import org.dreamcat.common.MutableInt;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.text.TextValueType;
import org.dreamcat.common.util.ObjectUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2023-08-15
 */
@ArgParserType(allProperties = true)
public class TextTypeModule {

    @ArgParserField("t")
    private String textTypeFile;
    @ArgParserField("T")
    private String textTypeFileContent; // line sep by \n or ;, and field sep by |
    private String nullType;

    transient EnumMap<TextValueType, List<String>> textTypeMap;
    transient Map<TextValueType, MutableInt> textTypeUsedIndexMap = new HashMap<>();

    public void afterPropertySet() throws IOException {
        if (ObjectUtil.isEmpty(textTypeFile) && ObjectUtil.isBlank(textTypeFileContent)) {
            System.err.println("require args: -t|--text-type-file <file> or -T|--text-type-file-content <content>");
            System.exit(1);
        }
        this.textTypeMap = getTextTypeMap();
        if (!textTypeMap.containsKey(TextValueType.NULL)) {
            List<String> all = textTypeMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
            textTypeMap.put(TextValueType.NULL, all);
        }
        Set<TextValueType> both = new HashSet<>(Arrays.asList(TextValueType.values()));
        both.removeAll(textTypeMap.keySet());
        if (!both.isEmpty()) {
            System.err.println("miss text type: " + both);
            System.exit(1);
        }
    }

    public String computeCandidateType(Object value) {
        TextValueType textValueType = TextValueType.detectObject(value);
        return computeCandidateType(textValueType);
    }

    public String computeCandidateType(TextValueType textValueType) {
        if (TextValueType.NULL.equals(textValueType) && nullType != null) {
            return nullType;
        }

        List<String> candidateList = textTypeMap.get(textValueType);
        int index = textTypeUsedIndexMap.computeIfAbsent(textValueType, k -> new MutableInt(0)).getAndIncr();
        index %= candidateList.size();
        return candidateList.get(index);
    }

    private EnumMap<TextValueType, List<String>> getTextTypeMap() throws IOException {
        List<String> lines;
        if (ObjectUtil.isNotEmpty(textTypeFile)) {
            lines = FileUtil.readLines(textTypeFile);
        } else {
            lines = Arrays.asList(textTypeFileContent.split("\n"));
        }
        return lines.stream()
                .map(String::trim).filter(ObjectUtil::isNotBlank)
                .map(line -> {
                    String[] pair = line.split(":", 2);
                    if (pair.length != 2) {
                        System.err.println("invalid format line in your text-type-file: " + line);
                        System.exit(1);
                    }
                    String textType = pair[0].trim();
                    TextValueType textValueType = null;
                    for (TextValueType valueType : TextValueType.values()) {
                        if (valueType.name().equalsIgnoreCase(textType)) {
                            textValueType = valueType;
                            break;
                        }
                    }
                    if (textValueType == null) {
                        System.err.println("invalid format line in your text-type-file: " + line);
                        System.exit(1);
                    }
                    List<String> types = Arrays.stream(pair[1].trim().split("\\|")).map(String::trim)
                            .collect(Collectors.toList());
                    return Pair.of(textValueType, types);
                }).collect(Collectors.toMap(Pair::getKey, Pair::getValue,
                        (a, b) -> a, () -> new EnumMap<>(TextValueType.class)));
    }
}
