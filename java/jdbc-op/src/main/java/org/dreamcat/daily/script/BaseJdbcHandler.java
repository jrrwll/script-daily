package org.dreamcat.daily.script;

import static org.dreamcat.common.util.StringUtil.isNotEmpty;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.function.IConsumer;
import org.dreamcat.common.javac.FileClassLoader;
import org.dreamcat.common.util.ReflectUtil;
import org.dreamcat.daily.script.common.CliUtil;

/**
 * @author Jerry Will
 * @version 2023-03-30
 */
public class BaseJdbcHandler implements Base {

    String jdbcUrl;
    String user;
    String password;
    @ArgParserField(value = {"C"})
    String driverClassName;
    @ArgParserField(value = {"P"})
    String driverPath;
    @ArgParserField({"D"})
    Properties props;
    boolean yes; // execute sql or not actually

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void run(IConsumer<Connection, ?> f) throws Exception {
        if (!yes) {
            f.accept(null);
            return;
        }
        // validate args
        CliUtil.checkParameter(jdbcUrl, "jdbcUrl", "-j|--jdbc-url");
        CliUtil.checkParameter(driverPath, "driverPath", "-p|--driver-path");
        CliUtil.checkParameter(driverClassName, "driverClassName", "-c|--driver-class-name");
        if (isNotEmpty(user) && isNotEmpty(password)) {
            props.put("user", user);
            props.put("password", password);
        }

        URL driverPathUrl = new File(driverPath).toURI().toURL();
        FileClassLoader driverClassLoader = new FileClassLoader(new URL[]{driverPathUrl},
                Thread.currentThread().getContextClassLoader());
        Class<? extends Driver> driverClass = (Class) driverClassLoader.loadClass(driverClassName);

        Driver driver = ReflectUtil.newInstance(driverClass);
        try (Connection connection = driver.connect(jdbcUrl, props)) {
            f.accept(connection);
        }
    }
}