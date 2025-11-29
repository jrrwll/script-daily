package org.dreamcat.daily.script;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2025-11-29
 */
class BaseTest {

    final String homeDir = System.getProperty("user.home");
    final File gradleCacheDir = new File(homeDir, ".gradle/caches/modules-2/files-2.1");
    final File mavenCacheDir = new File(homeDir, ".m2/repository");

    List<String> mysqlDriverPath() {
        // implementation 'mysql:mysql-connector-java:8.0.29'
        return driverPath("mysql", "mysql-connector-java", "8.0.29");
    }

    List<String> postgresqlDriverPath() {
        // implementation 'org.postgresql:postgresql:42.6.0'
        return driverPath("org.postgresql", "postgresql", "42.6.0");
    }

    List<String> h2DriverPath() {
        // implementation 'com.h2database:h2:2.2.224'
        return driverPath("com.h2database", "h2", "2.2.224");
    }

    List<String> sqliteDriverPath() {
        // implementation 'org.xerial:sqlite-jdbc:3.50.3.0'
        return driverPath("org.xerial", "sqlite-jdbc", "3.50.3.0");
    }

    private List<String> driverPath(String groupId, String artifactId, String version) {
        File dir = new File(gradleCacheDir, groupId + "/" + artifactId + "/" + version);
        if (dir.exists()) {
            return Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());
        }

        dir = new File(mavenCacheDir, groupId.replace('.', '/') + "/" + artifactId + "/" + version);
        if (dir.exists()) {
            return Collections.singletonList(dir.getAbsolutePath());
        }
        return null;
    }
}
