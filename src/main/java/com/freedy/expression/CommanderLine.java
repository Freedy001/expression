package com.freedy.expression;

import com.freedy.expression.stander.StanderEvaluationContext;
import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.util.Scanner;

/**
 * @author Freedy
 * @date 2022/1/5 20:40
 */
public class CommanderLine {

    public static ExpressionPasser passer = new ExpressionPasser();
    public static StanderEvaluationContext context = new StanderEvaluationContext(new Object());

    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        if (args.length == 1) {
            if (!args[0].endsWith(".fun")) {
                System.out.println("\033[91mscript file's name should end with .fun");
                System.exit(0);
            }
            try {
                FileInputStream stream = new FileInputStream(args[0]);
                evaluate(new String(stream.readAllBytes()), "");
            } catch (Exception e) {
                System.out.println("\033[91m"+e.getMessage()+"\033[0;39m");
            }
        }
        Scanner scanner = new Scanner(System.in);
        StringBuilder builder = new StringBuilder();
        int blockMode = 0;
        while (true) {
            System.out.print(blockMode != 0 ? "......................> " : "EL-commander-line@FNU # ");
            String line = scanner.nextLine();
            if (line.endsWith(".fun")) {
                main(new String[]{line});
            }
            builder.append(line).append("\n");
            blockMode += leftBracket(line);

            if (blockMode == 0) {
                try {
                    evaluate(builder.toString(), "\033[95mempty\033[0;39m");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("\n");
                builder = new StringBuilder();
            }
        }
    }

    private static void evaluate(String stream, String x) {
        Object value = passer.parseExpression(stream).getValue(context);
        System.out.println(value == null ? x : value);
    }

    private static int leftBracket(String expr) {
        char[] chars = expr.toCharArray();
        int l = 0;
        for (char aChar : chars) {
            if (aChar == '{') {
                l++;
                continue;
            }
            if (aChar == '}') {
                l--;
            }
        }
        return l;
    }
}
