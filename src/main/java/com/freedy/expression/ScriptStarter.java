package com.freedy.expression;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.freedy.expression.core.Expression;
import com.freedy.expression.jline.*;
import com.freedy.expression.log.LogRecorder;
import com.freedy.expression.stander.StanderEvaluationContext;
import com.freedy.expression.utils.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.*;
import org.jline.reader.Candidate;
import org.jline.reader.ParsedLine;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

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
public class ScriptStarter {

    public static boolean isJlineMode = false;
    private static Channel pc;
    @Setter
    @Getter
    // TODO: 2022/8/29 启动检查
    private static StanderEvaluationContext context = new StanderEvaluationContext();
    private static JlineSuggestion suggestion = new LocalJlineSuggestion(context);
    public final static Scanner scanner = new Scanner(System.in);
    public final static String CHARSET = System.getProperty("file.encoding") == null ? "UTF-8" : System.getProperty("file.encoding");
    private final static boolean DEV = Boolean.parseBoolean(Optional.ofNullable(System.getProperty("dev")).orElse("false"));


    public static void main(String[] args) throws Exception {
        parseParameters(args);
        isJlineMode = System.getProperty("disableJline") == null && Objects.requireNonNull(ScriptStarter.class.getResource("")).getProtocol().equals("jar");
        startLocalCmd();
    }

    public static void startLocalCmd() {
        collectScript(completeScript -> {
            evaluate(completeScript);
            return true;
        }, ScriptStarter::resolveLocalCmd, "fun> ");
    }

    private static void parseParameters(String[] args) throws Exception {
        if (args.length == 0) return;
        if (args[0].equals("server")) {
            startRemote();
            return;
        }
        if (args[0].equals("connect")) {
            startClient();
            System.exit(0);
        }
        if (args[0].equals("exec")) {
            evaluate(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            System.exit(0);
        }
        if (args[0].equals("help")) {
            System.out.println("""
                    \033[95mUsage:fun <command/script-file> [<args>]\033[0;39m
                    \033[93mThese are common fun commands:
                        server     start a netty server and act as a remote script runner
                        connect    connect to a remote server and run script on remote server
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
                context.setVariable("arg" + i, args[i]);
            }
            evaluate(new String(scriptContent.readAllBytes(), CHARSET));
        }
        System.exit(0);
    }


    private static boolean resolveLocalCmd(String line) {
        try {
            if (line.equals("cls") && isJlineMode) {
                suggestion.clearScreen();
                return false;
            }
            if (line.endsWith(".fun")) {
                @Cleanup FileInputStream scriptContent = null;
                try {
                    scriptContent = new FileInputStream(line);
                } catch (Exception e) {
                    System.out.println("\033[91m" + e.getMessage() + "\033[0;39m");
                }
                if (scriptContent != null) evaluate(new String(scriptContent.readAllBytes(), CHARSET));
                return false;
            } else if (line.equals("::shutdown")) {
                if (pc != null) {
                    pc.close();
                    System.out.println("ok!");
                }
                return false;
            } else if (line.equals("::server")) {
                if (pc != null && pc.isActive()) {
                    System.out.println("server has start!");
                } else {
                    startRemote();
                }
                return false;
            } else if (line.equals("::connect")) {
                startClient();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 启动命令行，用于收集用户输入的脚本。
     *
     * @param completeScriptAct 当用户输入了一个完整的脚本后会回调该lambda。返回true表示继续收集，返回false表示结束收集并退出循环。
     * @param lineInterceptor   每行输入的拦截器，返回true表示继续执行，返回false表示跳过该行输入。
     */
    private static void collectScript(Function<String, Boolean> completeScriptAct, Function<String, Boolean> lineInterceptor, String placeholder) {
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
                try {
                    if (!completeScriptAct.apply(builder.toString())) {
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                builder = new StringBuilder();
            }
        }
    }

    private static void evaluate(String completeScript) {
        evaluate(completeScript, "\033[95munknown\033[0;39m");
    }

    private static void evaluate(String completeScript, String nullValTips) {
        try {
            Object value = new Expression(completeScript).getValue(context);
            context.setVariable("_lastReturn", value);
            if (value == null) {
                System.out.println(nullValTips);
                return;
            }
            if (value instanceof Collection<?> collection) {
                System.out.println("\033[95mCollection:\033[0;39m");
                for (Object o : collection) {
                    System.out.println("\t" + toString(o));
                }
            } else if (value instanceof Map<?, ?> map) {
                System.out.println("\033[95mMap:\033[0;39m");
                map.forEach((k, v) -> System.out.println("\t" + toString(k) + " --- " + toString(v)));
            } else if (value.getClass().getName().startsWith("[")) {
                int length = Array.getLength(value);
                StringJoiner joiner = new StringJoiner(",", "[", "]");
                for (int i = 0; i < length; i++) {
                    joiner.add(toString(Array.get(value, i), true));
                }
                System.out.println(joiner);
            } else {
                System.out.println(toString(value, true));
            }
        } catch (Throwable e) {
            if (DEV) {
                e.printStackTrace();
                return;
            }
            System.out.println(e.getMessage().strip());
            Throwable cause = e.getCause();
            System.out.println("\033[31m----------------------------------------------------------------------------------------------");
            System.out.println("cause:\033[0;39m");
            while (cause != null) {
                System.out.println("\t\033[31m" + cause.getClass().getSimpleName() + "\033[0;39m -> " + cause.getMessage());
                cause = cause.getCause();
            }
        }
    }

    private static String toString(Object o) {
        return toString(o, false);
    }

    private static String toString(Object o, boolean pretty) {
        Object format = context.getVariable("jsonFormat");
        if (format instanceof Boolean f && f) {
            return o.toString().equals(o.getClass().getName() + "@" + Integer.toHexString(o.hashCode())) ?
                    pretty ? JSON.toJSONString(o, SerializerFeature.PrettyFormat) : JSON.toJSONString(o) : o.toString();
        } else return o.toString();
    }

    private static void startRemote() {
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
        startRemote(port, aesKey, stdin("auth-key:"));
    }


    public static void startRemote(int port, String aesKey, String md5AuthStr) {
        startRemote(port, aesKey, md5AuthStr, ctx -> System.out.println(new PlaceholderParser("one client? connect", ctx.channel().remoteAddress()).configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_BLUE)));
    }

    @SneakyThrows
    public static void startRemote(int port, String aesKey, String md5AuthStr, Consumer<ChannelHandlerContext> interceptor) {
        LogRecorder LOG_RECORDER = new LogRecorder();
        if (checkSuggestion()) return;
        byte[] auth = EncryptUtil.stringToMD5(md5AuthStr).getBytes(StandardCharsets.UTF_8);
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
                                new ObjectEncoder(),
                                new ObjectDecoder(ClassResolvers.cacheDisabled(ScriptStarter.class.getClassLoader())),
                                new SimpleChannelInboundHandler<String>() {

                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) {
                                        interceptor.accept(ctx);
                                    }

                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String s) {
                                        String cmd = s.trim().toLowerCase(Locale.ROOT);
                                        if (cmd.equals("::shutdown::")) {
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
                                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                        System.out.println(new PlaceholderParser("client[?] disconnected!", ctx.channel().remoteAddress()));
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                        cause.printStackTrace();
                                        ctx.channel().writeAndFlush(cause.getMessage());
                                    }
                                }, new SimpleChannelInboundHandler<SuggestionMetadata>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, SuggestionMetadata msg) throws Exception {
                                        if (msg == null) return;
                                        ArrayList<Candidate> list = new ArrayList<>();
                                        suggestion.suggest(null, msg, list);
                                        ctx.channel().writeAndFlush(list.stream().map(ca -> ReflectionUtils.copyReference(ca, SerializableCandidate.class)).toList());
                                    }
                                }
                        );
                    }
                }).bind(port).sync().channel();
        System.out.println("\033[95mserver start success on port" + port + "\033[0;39m");
    }

    private static boolean checkSuggestion() {
        if (suggestion instanceof LocalJlineSuggestion l){
            if (l.getContext()!=context){
                l.setContext(context);
            }
        }else {
            System.out.println(Color.dRed("wrong suggestion type!"));
            return true;
        }
        return false;
    }

    //127.0.0.1:21;abcdsawerfsasxcs;asdasdas
    @SneakyThrows
    private static void startClient() {
        File file = new File("./encrypt.txt");
        String ip, aesKey;
        int port;
        byte[] bytes;

        if (file.exists()) {
            @Cleanup FileInputStream is = new FileInputStream(file);
            String strip = new String(is.readAllBytes()).trim().strip();
            List<ConnectConfig> list = Arrays.stream(strip.split("\n")).map(ConnectConfig::new).toList();
            ConnectConfig connectConfig;
            if (list.size() == 1) {
                connectConfig = list.get(0);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    ConnectConfig config = list.get(i);
                    System.out.println(i + ": " + config.getTagName() + "(" + config.getIp() + ":" + config.getPort() + ")");
                }
                String configNum = null;
                do {
                    if (configNum != null) System.out.println("\033[95millegal input!\033[0;39m");
                    configNum = stdin("\033[95mplease select one config:\033[0;39m");
                } while (!configNum.matches("\\d+"));
                connectConfig = list.get(Integer.parseInt(configNum));
            }
            ip = connectConfig.ip;
            port = connectConfig.port;
            aesKey = connectConfig.aesKey;
            bytes = EncryptUtil.stringToMD5(connectConfig.md5Key).getBytes(StandardCharsets.UTF_8);
        } else {
            String line = stdin("address(ip:port):");
            ip = line.split(":")[0];
            port = Integer.parseInt(line.split(":")[1]);
            while (true) {
                aesKey = stdin("aes-key:");
                if (aesKey.length() != 16) {
                    System.out.println("\033[95maes-key'length must 16\033[0;39m");
                } else {
                    break;
                }
            }
            bytes = EncryptUtil.stringToMD5(stdin("auth-key:")).getBytes(StandardCharsets.UTF_8);
        }
        boolean[] shutDown = new boolean[]{true};
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
                                new ObjectEncoder(),
                                new ObjectDecoder(ClassResolvers.cacheDisabled(ScriptStarter.class.getClassLoader())),
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
                                        synchronized (channel) {
                                            System.out.println("\033[92mserver stopped!\033[0;39m");
                                            shutDown[0] = false;
                                            channel.notifyAll();
                                        }
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                        if (cause.getMessage().equals("Connection reset")) {
                                            System.exit(0);
                                        }
                                        System.out.println(new PlaceholderParser("EXCEPTION ?", cause.getMessage()).configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_RED));
                                    }
                                });
                    }
                }).connect(ip, port).sync().channel();
        JlineSuggestion replicate = suggestion;
        suggestion = new ClientJlineSuggestion(channel);
        synchronized (channel) {
            collectScript(completeScript -> {
                channel.writeAndFlush(completeScript);
                try {
                    channel.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return shutDown[0];
            }, line -> {
                if (line.endsWith(".fun")) {
                    try {
                        @Cleanup FileInputStream stream = new FileInputStream(line);
                        channel.writeAndFlush(new String(stream.readAllBytes(), CHARSET));
                        channel.wait();
                    } catch (Exception e) {
                        System.out.println("\033[91m" + e.getMessage() + "\033[0;39m");
                    }
                    return false;
                }
                return true;
            }, "fun@" + ip + ":" + port + "> ");
        }
        suggestion = replicate;
    }

    @Data
    private static class ConnectConfig {
        String tagName;
        String ip;
        int port;
        String aesKey;
        String md5Key;

        public ConnectConfig(String str) {
            String[] item = str.split(";");
            if (item.length != 4) throw new java.lang.IllegalArgumentException("illegal pattern str");
            this.tagName = item[0];
            String[] ipInfo = item[1].split(":");
            if (ipInfo.length != 2) throw new java.lang.IllegalArgumentException("illegal pattern str");
            this.ip = ipInfo[0];
            this.port = Integer.parseInt(ipInfo[1]);
            this.aesKey = item[2];
            this.md5Key = item[3];
        }
    }


    public static String stdin(String placeholder) {
        String line;
        if (isJlineMode) {
            line = suggestion.stdin(placeholder);
        } else {
            System.out.print(placeholder);
            line = scanner.nextLine();
        }
        return line;
    }

}
