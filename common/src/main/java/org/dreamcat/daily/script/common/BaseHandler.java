package org.dreamcat.daily.script.common;

import lombok.SneakyThrows;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.CommandArgParser;
import org.dreamcat.common.argparse.CommandHelpInfo;
import org.dreamcat.common.json.YamlUtil;
import org.dreamcat.common.util.ObjectUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * @author Jerry Will
 * @version 2023-06-27
 */
public abstract class BaseHandler implements ArgParserEntrypoint {

    @ArgParserField(firstChar = true)
    private boolean help;

    public static void run(Class<? extends BaseHandler> clazz, String[] args) {
        if (ObjectUtil.isNotBlank(System.getenv("DEBUG")) && !"0".equals(System.getenv("DEBUG"))) {
            System.out.println(Arrays.toString(args));
        }
        CommandArgParser argParser = new CommandArgParser(clazz);
        // help info
        try (InputStream file = clazz.getClassLoader().getResourceAsStream("usage.yaml")) {
            CommandHelpInfo helpInfo = YamlUtil.fromJson(file, CommandHelpInfo.class);
            argParser.setSubcommandHelpInfo(helpInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        argParser.run(args);
    }

    protected void afterPropertySet() throws Exception {
        // nop
    }

    public abstract void run() throws Exception;

    @SneakyThrows
    @Override
    public final void run(ArgParserContext context) {
        if (help) {
            System.out.println(context.getHelp());
            return;
        }

        this.afterPropertySet();
        this.run();
    }
}
