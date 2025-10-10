package org.dreamcat.daily.script.module;

import static org.dreamcat.common.util.StringUtil.isNotEmpty;

import java.net.URL;
import java.sql.Connection;
import java.util.List;
import java.util.Properties;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.function.IConsumer;
import org.dreamcat.common.sql.DriverUtil;
import org.dreamcat.daily.script.common.CliUtil;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@ArgParserType(allProperties = true)
public class JdbcModule {

    @ArgParserField(value = {"j"})
    public String jdbcUrl;
    @ArgParserField(value = {"u"})
    public String user;
    @ArgParserField(value = {"p"})
    public String password;
    @ArgParserField(value = {"dc"})
    public String driverClass;
    // driver directory
    @ArgParserField(value = {"dp"})
    public List<String> driverPaths;
    @ArgParserField({"D"})
    public Properties props;

    public void afterPropertySet() throws Exception {

    }

    public void run(IConsumer<Connection> f) throws Exception {
        if (jdbcUrl != null) {
            validateJdbc();
        }
        // jdbcUrl == null only if yes is false
        if (jdbcUrl == null) {
            f.accept(null);
            return;
        }
        if (isNotEmpty(user)) props.put("user", user);
        if (isNotEmpty(password)) props.put("password", password);

        List<URL> urls = DriverUtil.parseJarPaths(driverPaths);
        for (URL url : urls) {
            System.out.println("add url to classloader: " + url);
        }
        System.out.println("jdbcUrl=" + jdbcUrl);
        System.out.println("props=" + props);
        DriverUtil.runIsolated(jdbcUrl, props, urls, driverClass, c -> {
            f.accept(c);
            return null;
        });
    }

    public void validateJdbc() {
        // validate args
        CliUtil.checkParameter(jdbcUrl, "-j|--jdbc-url");
        CliUtil.checkParameter(driverPaths, "--dp|--driver-paths");
        CliUtil.checkParameter(driverClass, "--dc|--driver-class");
    }
}
