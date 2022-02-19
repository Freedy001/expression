package com.freedy.expression;

import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.stander.StanderEvaluationContext;
import com.freedy.expression.utils.PlaceholderParser;
import com.freedy.expression.utils.ReflectionUtils;
import com.freedy.expression.utils.StringUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Freedy
 * @date 2022/1/5 20:40
 */
public class CommanderLine {

    public final static boolean JAR_ENV = System.getProperty("disableJline") == null && Objects.requireNonNull(CommanderLine.class.getResource("")).getProtocol().equals("jar");
    public final static LogRecorder LOG_RECORDER = new LogRecorder(System.out);
    private final static ExpressionPasser PARSER = new ExpressionPasser();
    public final static Scanner SCANNER = new Scanner(System.in);
    public final static String CHARSET = System.getProperty("file.encoding") == null ? "UTF-8" : System.getProperty("file.encoding");
    private final static Terminal TERMINAL;
    public final static LineReader READER;
    @Setter
    @Getter
    private static StanderEvaluationContext context = new StanderEvaluationContext();
    private static Channel pc;

    static {
        System.setOut(new PrintStream(LOG_RECORDER));
        try {
            TERMINAL = TerminalBuilder.terminal();
            TERMINAL.puts(InfoCmp.Capability.clear_screen);
            READER = LineReaderBuilder.builder().terminal(TERMINAL).completer((reader1, line, candidates) -> {
                String lineStr = line.word();
                if (StringUtils.isEmpty(lineStr)) {
                    candidates.addAll(getDefaultTipCandidate("", ""));
                } else {
                    String subLine = lineStr.substring(0, line.wordCursor());
                    String suffix = lineStr.substring(line.wordCursor());
                    int[] str = findEvaluateStr(subLine);
                    if (str == null) {
                        return;
                    }
                    if (str.length == 1) {
                        Set<Candidate> set = getDefaultTipCandidate(subLine.substring(0, str[0]), "");
                        candidates.addAll(set);
                    } else {
                        String resultStr = subLine.substring(str[0], str[1]);
                        String baseStr = lineStr.substring(0, str[0]) + resultStr;
                        String[] varArr = resultStr.split("\\.");
                        Object variable = context.getVariable(varArr[0]);
                        if (variable == null) return;
                        Class<?> varType = variable.getClass();
                        int len = varArr.length;
                        if (len == 1) {
                            candidates.addAll(ReflectionUtils.getFieldsRecursion(varType).stream().map(field -> {
                                String tip = baseStr + "." + field.getName() + suffix;
                                return new Candidate(tip, tip, "variable", null, null, null, false, 0);
                            }).collect(Collectors.toSet()));
                            candidates.addAll(ReflectionUtils.getMethodsRecursion(varType).stream().map(method -> {
                                int count = method.getParameterCount();
                                String tip = baseStr + "." + method.getName() + "(" + ",".repeat(count <= 1 ? 0 : count - 1) + ")" + suffix;
                                return new Candidate(tip, tip, "function", null, null, null, true, 1);
                            }).collect(Collectors.toSet()));
                            return;
                        }
                        for (int i = 1; i < len; i++) {
                            Field field = ReflectionUtils.getFieldRecursion(varType, varArr[i]);
                            if (field == null) return;
                            varType = field.getType();
                        }
                        candidates.addAll(ReflectionUtils.getFieldsRecursion(varType).stream().map(field -> {
                            String tip = baseStr + "." + field.getName() + suffix;
                            return new Candidate(tip, tip, "variable", null, null, null, false, 0);
                        }).collect(Collectors.toSet()));
                        candidates.addAll(ReflectionUtils.getMethodsRecursion(varType).stream().map(method -> {
                            int count = method.getParameterCount();
                            String tip = baseStr + "." + method.getName() + "(" + ",".repeat(count <= 1 ? 0 : count - 1) + ")" + suffix;
                            return new Candidate(tip, tip, "function", null, null, null, true, 1);
                        }).collect(Collectors.toSet()));
                    }
                }
            }).highlighter(new DefaultHighlighter()).parser(new DefaultParser().escapeChars(new char[]{})).build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }


    public static Set<Candidate> getDefaultTipCandidate(String base, String suffix) {
        Set<Candidate> set = context.getVariableMap().keySet().stream().map(var -> new Candidate(base + var + suffix, base + var + suffix, "_variable", null, null, null, false, 0)).collect(Collectors.toSet());
        context.getFunMap().forEach((k, v) -> {
            int params = v.getClass().getDeclaredMethods()[0].getParameterCount();
            String funStr = base + k + "(" + ",".repeat(params <= 1 ? 0 : params - 1) + ")" + suffix;
            set.add(new Candidate(funStr, funStr, "function", null, null, null, true, 1));
        });
        for (Field field : ReflectionUtils.getFieldsRecursion(context.getRoot().getClass())) {
            set.add(new Candidate(field.getName(), field.getName(), "root", null, null, null, false, 0));
        }
        set.addAll(List.of(
                new Candidate("for()", "for", "keyword", null, null, null, false, 0),
                new Candidate("if()", "if", "keyword", null, null, null, false, 0),
                new Candidate("def", "def", "keyword", null, null, null, false, 0),
                new Candidate("T()", "T()", "keyword", null, null, null, false, 0),
                new Candidate("[]", "[]", "keyword", null, null, null, false, 0),
                new Candidate("{}", "{}", "keyword", null, null, null, false, 0),
                new Candidate("@block{", "@block{", "keyword", null, null, null, false, 0),
                new Candidate("continue;", "continue", "keyword", null, null, null, false, 0),
                new Candidate("break;", "break", "keyword", null, null, null, false, 0),
                new Candidate("return;", "return", "keyword", null, null, null, false, 0)
        ));
        return set;
    }


    public static int[] findEvaluateStr(String line) {
        char[] chars = line.toCharArray();
        int len = chars.length;
        int lastDot = -1;
        int i = len - 1;
        for (; i >= 0; i--) {
            if (chars[i] == ')' || chars[i] == ',') {
                return null;
            }
            if (chars[i] == '(' || chars[i] == '=') {
                break;
            }
            if (chars[i] == '.' && lastDot == -1) {
                lastDot = i;
            }
        }
        return lastDot == -1 ? new int[]{i + 1} : new int[]{i + 1, lastDot};
    }


    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        cmd:
        if (args.length == 1) {
            if (args[0].equals("server")) {
                startRemote();
                break cmd;
            }
            if (args[0].equals("connect")) {
                startClient();
                break cmd;
            }
            if (!args[0].endsWith(".fun")) {
                System.out.println("\033[91mscript file's name should end with .fun");
                System.exit(0);
            }
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(args[0]);
            } catch (Exception e) {
                System.out.println("\033[91m" + e.getMessage() + "\033[0;39m");
            }
            if (stream != null) {
                evaluate(new String(stream.readAllBytes(), CHARSET), "");
            }
        }
        while (true) {
            cmdInvoke(str -> evaluate(str, "\033[95munknown\033[0;39m"), str -> {
                try {
                    if (str.equals("cls") && JAR_ENV) {
                        TERMINAL.puts(InfoCmp.Capability.clear_screen);
                        return false;
                    }
                    if (str.endsWith(".fun")) {
                        main(new String[]{str});
                        return false;
                    } else if (str.equals("::shutdown")) {
                        if (pc != null) {
                            pc.close();
                            System.out.println("ok!");
                        }
                        return false;
                    } else if (str.equals("::server")) {
                        if (pc != null && pc.isActive()) {
                            System.out.println("server has start!");
                        } else {
                            startRemote();
                        }
                        return false;
                    } else if (str.equals("::connect")) {
                        startClient();
                        return false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            });
        }

    }


    private static void cmdInvoke(Consumer<String> consumer, Function<String, Boolean> cmd) {
        StringBuilder builder = new StringBuilder();
        int blockMode = 0;
        int bracketMode = 0;
        while (true) {
            String line;
            if (JAR_ENV) {
                line = READER.readLine(blockMode != 0 || bracketMode != 0 ? "...... " : "fun> ");
            } else {
                System.out.print(blockMode != 0 || bracketMode != 0 ? "...... " : "fun> ");
                line = SCANNER.nextLine();
            }
            if (!cmd.apply(line)) {
                return;
            }
            builder.append(line).append("\n");
            blockMode += leftBracket(line, '{', '}');
            bracketMode += leftBracket(line, '(', ')');
            if (blockMode < 0) {
                System.out.println("\033[91mnot pared bracket '}' close!\033[0;30m");
                builder = new StringBuilder();
                continue;
            }
            if (bracketMode < 0) {
                System.out.println("\033[91mnot pared ')' close!\033[0;30m");
                builder = new StringBuilder();
                continue;
            }

            if (blockMode == 0 && bracketMode == 0) {
                try {
                    consumer.accept(builder.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                builder = new StringBuilder();
            }
        }
    }

    private static void evaluate(String stream, String x) {
        try {
            Object value = PARSER.parseExpression(stream).getValue(context);
            if (value == null) {
                System.out.println(x);
                return;
            }
            if (value instanceof Collection<?> collection) {
                System.out.println("\033[95mCollection:\033[0;39m");
                for (Object o : collection) {
                    System.out.println("\t" + o);
                }
            } else if (value instanceof Map<?, ?> map) {
                System.out.println("\033[95mMap:\033[0;39m");
                map.forEach((k, v) -> System.out.println("\t" + k + " --- " + v));
            } else if (value.getClass().getName().startsWith("[")) {
                int length = Array.getLength(value);
                StringJoiner joiner = new StringJoiner(",", "[", "]");
                for (int i = 0; i < length; i++) {
                    joiner.add(String.valueOf(Array.get(value, i)));
                }
                System.out.println(joiner);
            } else {
                System.out.println(value);
            }
        } catch (Throwable e) {
            if (JAR_ENV) {
                System.out.println(e.getMessage().strip());
                List<String> msg = new ArrayList<>();
                Throwable cause = e.getCause();
                while (cause != null) {
                    msg.add("\t\033[31m" + cause.getClass().getSimpleName() + "\033[0;39m -> " + cause.getMessage());
                    cause = cause.getCause();
                }
                if (!msg.isEmpty()) {
                    System.out.println("\033[31m----------------------------------------------------------------------------------------------");
                    System.out.println("cause:\033[0;39m");
                    msg.forEach(System.out::println);
                }
            } else {
                e.printStackTrace();
            }
        }
    }

    private static int leftBracket(String expr, char c1, char c2) {
        char[] chars = expr.toCharArray();
        int l = 0;
        boolean quote = false;
        boolean bigQuote = false;
        for (char aChar : chars) {
            if (!quote && aChar == '"') {
                bigQuote = !bigQuote;
            }
            if (bigQuote) continue;
            if (aChar == '\'') {
                quote = !quote;
            }
            if (quote) continue;

            if (aChar == c1) {
                l++;
                continue;
            }
            if (aChar == c2) {
                l--;
            }
        }
        return l;
    }

    private static void startRemote() throws InterruptedException {
        int port = Integer.parseInt(stdin("port:"));
        String aesKey;
        while (true) {
            aesKey = stdin("aes-key:");
            if (aesKey.length() != 16) {
                System.out.println("\033[95maes-key'length must 16\033[0;39m");
            } else {
                break;
            }
        }
        startRemote(port, aesKey, EncryptUtil.stringToMD5(stdin("auth-key:")).getBytes(StandardCharsets.UTF_8));
    }

    public static void startRemote(int port, String aesKey, byte[] auth) throws InterruptedException {
        startRemote(port,aesKey,auth,ctx-> System.out.println(new PlaceholderParser("one client? connect", ctx.channel().remoteAddress()).configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_BLUE)));
    }

    public static void startRemote(int port, String aesKey, byte[] auth,Consumer<ChannelHandlerContext> interceptor) throws InterruptedException {
        pc = new ServerBootstrap().option(ChannelOption.SO_BACKLOG, 10240)
                .channel(NioServerSocketChannel.class)
                .group(new NioEventLoopGroup(1), new NioEventLoopGroup())
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline().addLast(
                                new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                                new LengthFieldPrepender(4),
                                new AuthenticAndEncrypt(aesKey, auth),
                                new AuthenticAndDecrypt(aesKey, auth),
                                new StringEncoder(),
                                new StringDecoder(),
                                new SimpleChannelInboundHandler<String>() {

                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) {
                                        interceptor.accept(ctx);
                                    }

                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String s) {
                                        if (s.trim().toLowerCase(Locale.ROOT).equals("::shutdown")) {
                                            ctx.channel().close();
                                            pc.close();
                                            return;
                                        }
                                        //清空日志缓存
                                        LOG_RECORDER.getLog();
                                        evaluate(s, "\033[95mempty\033[0;39m");
                                        ctx.channel().writeAndFlush(LOG_RECORDER.getLog());
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                        cause.printStackTrace();
                                        ctx.channel().writeAndFlush(cause.getMessage());
                                    }
                                }
                        );
                    }
                }).bind(port).sync().channel();
        System.out.println("\033[95mserver start success\033[0;39m");
    }

    private static void startClient() throws InterruptedException {
        String line = stdin("address(ip:port):");
        String ip = line.split(":")[0];
        int port = Integer.parseInt(line.split(":")[1]);
        String aesKey;
        while (true) {
            aesKey = stdin("aes-key:");
            if (aesKey.length() != 16) {
                System.out.println("\033[95maes-key'length must 16\033[0;39m");
            } else {
                break;
            }
        }
        byte[] bytes = EncryptUtil.stringToMD5(stdin("auth-key:")).getBytes(StandardCharsets.UTF_8);
        boolean[] shutDown = new boolean[]{false};
        String finalAesKey = aesKey;
        Channel channel = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline().addLast(
                                new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                                new LengthFieldPrepender(4),
                                new AuthenticAndEncrypt(finalAesKey, bytes),
                                new AuthenticAndDecrypt(finalAesKey, bytes),
                                new StringEncoder(),
                                new StringDecoder(),
                                new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) {
                                        synchronized (channel) {
                                            System.out.print(s);
                                            channel.notifyAll();
                                        }
                                    }

                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) {
                                        System.out.println("\033[92mserver stopped!\033[0;39m");
                                        shutDown[0] = true;
                                        channel.notifyAll();
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                        if (cause.getMessage().equals("Connection reset")) {
                                            System.exit(0);
                                        }
                                        System.out.println(new PlaceholderParser("EXCEPTION ?", cause.getMessage()).configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_RED));
                                    }
                                }
                        );
                    }
                }).connect(ip, port).sync().channel();
        while (!shutDown[0]) {
            synchronized (channel) {
                cmdInvoke(str -> {
                    channel.writeAndFlush(str);
                    try {
                        channel.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }, cmd -> {
                    if (cmd.endsWith(".fun")) {
                        try {
                            FileInputStream stream = new FileInputStream(cmd);
                            channel.writeAndFlush(new String(stream.readAllBytes(), CHARSET));
                            channel.wait();
                        } catch (Exception e) {
                            System.out.println("\033[91m" + e.getMessage() + "\033[0;39m");
                        }
                        return false;
                    }
                    return true;
                });
            }
        }
    }

    private static String stdin(String placeholder){
        String line;
        if (JAR_ENV) {
            line = READER.readLine(placeholder);
        } else {
            System.out.print(placeholder);
            line = SCANNER.nextLine();
        }
        return line;
    }

}
