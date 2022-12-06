package com.freedy.expression.utils;

/**
 * @author Freedy
 * @date 2022/6/30 2:03
 */
@SuppressWarnings("unused")
public class Color {
    public static final String RED = "\033[31m";
    public static final String GREEN = "\033[32m";
    public static final String YELLOW = "\033[33m";
    public static final String BLUE = "\033[34m";
    public static final String PINK = "\033[35m";
    public static final String CYAN = "\033[36m";
    public static final String WHITE = "\033[37m";
    public static final String GRAY = "\033[38m";
    public static final String D_RED = "\033[91m";
    public static final String D_GREEN = "\033[92m";
    public static final String D_YELLOW = "\033[93m";
    public static final String D_BLUE = "\033[94m";
    public static final String D_PINK = "\033[95m";
    public static final String D_CYAN = "\033[96m";
    public static final String D_WHITE = "\033[97m";
    public static final String D_GRAY = "\033[98m";

    public static final String END = "\033[0;39m";

    public void test(){}

    public static String red(Object str) {
        if (str == null) return null;
        return RED + str + END;
    }

    public static String green(Object str) {
        if (str == null) return null;
        return GREEN + str + END;
    }

    public static String yellow(Object str) {
        if (str == null) return null;
        return YELLOW + str + END;
    }

    public static String blue(Object str) {
        if (str == null) return null;
        return BLUE + str + END;
    }

    public static String pink(Object str) {
        if (str == null) return null;
        return PINK + str + END;
    }

    public static String cyan(Object str) {
        if (str == null) return null;
        return CYAN + str + END;
    }

    public static String white(Object str) {
        if (str == null) return null;
        return WHITE + str + END;
    }

    public static String gray(Object str) {
        if (str == null) return null;
        return GRAY + str + END;
    }

    public static String dRed(Object str) {
        if (str == null) return null;
        return D_RED + str + END;
    }

    public static String dGreen(Object str) {
        if (str == null) return null;
        return D_GREEN + str + END;
    }

    public static String dYellow(Object str) {
        if (str == null) return null;
        return D_YELLOW + str + END;
    }

    public static String dBlue(Object str) {
        if (str == null) return null;
        return D_BLUE + str + END;
    }

    public static String dPink(Object str) {
        if (str == null) return null;
        return D_PINK + str + END;
    }

    public static String dCyan(Object str) {
        if (str == null) return null;
        return D_CYAN + str + END;
    }

    public static String dWhite(Object str) {
        if (str == null) return null;
        return D_WHITE + str + END;
    }
}