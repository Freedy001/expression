package com.freedy.expression.entrance.cmd;

import com.freedy.expression.jline.LocalJlineTerminal;
import com.freedy.expression.standard.StandardEvaluationContext;
import lombok.Cleanup;

import java.io.FileInputStream;
import java.util.Arrays;

import static com.freedy.expression.SysConstant.CHARSET;

/**
 * 用于以命令行的方式启动,直接运行会启动用于运行脚本的命令行。 <br/>
 * 可选参数:
 * <li>1.server:可以在其他机器上使用connect参数来连接此机器，并在此机器上执行脚本代码</li>
 * <li>2.connect:用于连接其他机器</li>
 * <li>3.exec:可以立即执行脚本</li>
 * <li>4.help:可以查看帮助选项</li>
 * <li>5.脚本文件+参数:可以直接执行脚本文件</li>
 *
 * @author Freedy
 * @date 2022/1/5 20:40
 */
public class TerminalStarter {

    private final static TerminalExpr EXPR = new TerminalExpr(new StandardEvaluationContext());
    private final static TerminalHandler TERMINAL_HANDLER = new TerminalHandler(new LocalJlineTerminal(EXPR.getContext()));

    public static void main(String[] args) throws Exception {
        //清屏
        TERMINAL_HANDLER.clearScreen();
        parseParameters(args);
        startLocalCmd();
    }


    private static void parseParameters(String[] args) throws Exception {
        if (args.length == 0) return;
        if (args[0].equals("exec")) {
            EXPR.eval(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            System.exit(0);
        }
        if (args[0].equals("help")) {
            System.out.println("""
                    \033[95mUsage:fun <command/script-file> [<args>]\033[0;39m
                    \033[93mThese are common fun commands:
                        exec       execute subsequent parameters as fun scripts
                        help       get help information\033[0;39m
                    \033[93mExample:
                        fun exec ls
                        fun script.fun\033[0;39m
                    """);
            System.exit(0);
        }
        if (!args[0].endsWith(".fun")) {
            System.out.println("\033[91munknown cmd,you can type <fun help> for more infomation\033[0;39m");
            System.exit(0);
        }
        @Cleanup FileInputStream scriptContent = null;
        try {
            scriptContent = new FileInputStream(args[0]);
        } catch (Exception e) {
            System.out.println("\033[91m" + e.getMessage() + "\033[0;39m");
        }
        if (scriptContent != null) {
            for (int i = 1; i < args.length; i++) {
                EXPR.getContext().setVariable("arg" + i, args[i]);
            }
            EXPR.eval(new String(scriptContent.readAllBytes(), CHARSET));
        }
        System.exit(0);
    }

    public static void startLocalCmd() {
        TERMINAL_HANDLER.collectScript(completeScript -> {
            EXPR.eval(completeScript);
            return true;
        }, TerminalStarter::resolveLocalCmd, "fun> ");
    }

    private static boolean resolveLocalCmd(String line) {
        try {
            if (line.endsWith(".fun")) {
                @Cleanup FileInputStream scriptContent = null;
                try {
                    scriptContent = new FileInputStream(line);
                } catch (Exception e) {
                    System.out.println("\033[91m" + e.getMessage() + "\033[0;39m");
                }
                if (scriptContent != null) EXPR.eval(new String(scriptContent.readAllBytes(), CHARSET));
                return false;
            }
            if (line.endsWith("cls")) {
                TERMINAL_HANDLER.clearScreen();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
