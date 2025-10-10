package org.dreamcat.daily.script;

import java.io.IOException;
import org.dreamcat.common.util.ClassLoaderUtil;

/**
 * @author Jerry Will
 * @version 2021-06-15
 */
public class App {

    static final String USAGE;

    public static void main(String[] args) throws Exception {
        int length = args.length;
        if (length < 2) {
            if (length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
                System.out.println(USAGE);
                System.exit(0);
            }
            System.err.println("json-op: try 'json-op --help' for more information");
            System.exit(1);
        }

        String command = args[0];
        switch (command) {
            case "-h":
            case "--help":
                System.out.println(USAGE);
                return;
            case "compare":
            case "compare:print":
                CompareJsonHandler.print(args);
                break;
            case "compare:preJsonOneLine":
                CompareJsonHandler.preJsonOneLine(args);
                break;
            case "sort":
            case "sort:print":
                SortJsonFieldHandler.print(args);
                break;
            default:
                System.err.printf("unsupported command: %s%n", command);
                System.out.println(USAGE);
                System.exit(1);
                break;
        }
    }

    static {
        try {
            USAGE = ClassLoaderUtil.getResourceAsString("usage.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
