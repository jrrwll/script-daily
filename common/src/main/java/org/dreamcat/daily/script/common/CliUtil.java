package org.dreamcat.daily.script.common;

import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.util.ObjectUtil;

import java.io.IOException;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-03-24
 */
public class CliUtil {

    public static String requireFileOrContent(
            String file, String fileContent,
            String fileKey, String contentKey, String sinceKey)
            throws IOException {
        if (ObjectUtil.isNotBlank(file)) {
            return FileUtil.readAsString(file);
        } else if (ObjectUtil.isNotBlank(fileContent)) {
            return fileContent;
        } else {
            System.err.printf("required arg %s or %s since %s is pass%n",
                    fileKey, contentKey, sinceKey);
            System.exit(1);
            return null; // never happen
        }
    }

    public static void checkParameter(Object value, String names) {
        checkParameter(value, names, null);
    }

    public static void checkParameter(Object value, String names, String env) {
        if (value instanceof List) {
            if (ObjectUtil.isNotEmpty((List<?>) value)) return;
        } else {
            if (ObjectUtil.isNotBlank((String) value)) return;
        }
        String envString = "";
        if (ObjectUtil.isNotBlank(env)) {
            envString = String.format(", or you can define the environment variable %s", env);
        }

        System.err.printf("required arg %s is missing" + envString, names);
        System.exit(1);
    }
}
