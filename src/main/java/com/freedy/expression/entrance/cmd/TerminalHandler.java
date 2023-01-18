package com.freedy.expression.entrance.cmd;

import com.freedy.expression.jline.ExpressionTerminal;

import java.util.function.Function;

public class TerminalHandler implements ExpressionTerminal {
    private final ExpressionTerminal suggestion;

    public TerminalHandler(ExpressionTerminal suggestion) {
        this.suggestion = suggestion;
    }

    public String stdin(String placeholder) {
        return suggestion.stdin(placeholder);
    }

    public void clearScreen() {
        suggestion.clearScreen();
    }

    /**
     * 启动命令行，用于收集用户输入的脚本。
     *
     * @param completeScriptAct 当用户输入了一个完整的脚本后会回调该lambda。返回true表示继续收集，返回false表示结束收集并退出循环。
     * @param lineInterceptor   每行输入的拦截器，返回true表示继续执行，返回false表示跳过该行输入。
     */
    public void collectScript(Function<String, Boolean> completeScriptAct, Function<String, Boolean> lineInterceptor, String placeholder) {
        StringBuilder builder = new StringBuilder();
        int blockMode = 0;
        int bracketMode = 0;
        boolean quote = false;
        boolean bigQuote = false;
        while (true) {
            String line = stdin(blockMode != 0 || bracketMode != 0 || quote || bigQuote ? "...  " : placeholder);
            if (!lineInterceptor.apply(line)) {
                builder = new StringBuilder();
                blockMode = 0;
                bracketMode = 0;
                quote = false;
                bigQuote = false;
                continue;
            }
            builder.append(line).append("\n");
            char[] chars = line.toCharArray();
            for (char aChar : chars) {
                if (!quote && aChar == '"') {
                    bigQuote = !bigQuote;
                }
                if (bigQuote) continue;
                if (aChar == '\'') {
                    quote = !quote;
                }
                if (quote) continue;

                if (aChar == '{') {
                    blockMode++;
                    continue;
                }
                if (aChar == '}') {
                    blockMode--;
                }
                if (aChar == '(') {
                    bracketMode++;
                    continue;
                }
                if (aChar == ')') {
                    bracketMode--;
                }
            }
            if (blockMode < 0 || bracketMode < 0) {
                System.out.println("\033[91mnot pared bracket " + (blockMode < 0 ? '}' : ')') + " close!\033[0;30m");
                builder = new StringBuilder();
                blockMode = 0;
                bracketMode = 0;
                continue;
            }
            if (blockMode == 0 && bracketMode == 0 && !quote && !bigQuote) {
                if (!completeScriptAct.apply(builder.toString())) {
                    return;
                }
                builder = new StringBuilder();
            }
        }
    }

}
