package org.dreamcat.daily.script;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Predicate;
import org.dreamcat.common.util.ClassLoaderUtil;
import org.dreamcat.daily.script.poetry.ChinesePoetryFinder;

/**
 * @author Jerry Will
 * @version 2022-05-19
 */
public class App {

    public static final String USAGE;

    public static void main(String[] args) {
        int length = args.length;
        if (length < 1) {
            System.err.println(USAGE);
            System.exit(1);
        }

        Predicate<String[]> action;
        String command = args[0];
        switch (command) {
            case "-h":
            case "--help":
                System.out.println(USAGE);
                return;
            case "chinese-poetry":
            case "poetry":
                action = new ChinesePoetryFinder();
                break;
            default:
                System.err.printf("unsupported command: %s%n", command);
                System.err.println(USAGE);
                System.exit(1);
                return;
        }
        if (!action.test(Arrays.copyOfRange(args, 1, length))) {
            System.err.println(USAGE);
            System.exit(1);
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
