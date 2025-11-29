package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.daily.script.common.BaseHandler;

/**
 * @author Jerry Will
 * @version 2021-06-15
 */
@ArgParserType(allProperties = true,
        firstChar = true,
        subcommands = {
                CompareOp.class,
                CompareLinesOp.class,
                SortOp.class,
        })
public class Main extends BaseHandler {

    public static void main(String[] args) {
        run(Main.class, args);
    }

    @Override
    public void run() {
        System.err.println("require a subcommand");
        System.err.println("json-op: try 'json-op --help' for more information");
    }
}
