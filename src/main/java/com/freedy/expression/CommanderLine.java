package com.freedy.expression;

import com.freedy.expression.stander.StanderEvaluationContext;
import com.freedy.expression.utils.PlaceholderParser;
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

import java.io.FileInputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Freedy
 * @date 2022/1/5 20:40
 */
public class CommanderLine {

    public static LogRecorder logRecorder = new LogRecorder(System.out);

    static {
        System.setOut(new PrintStream(logRecorder));
    }

    @Setter
    @Getter
    private static EvaluationContext context = new StanderEvaluationContext();
    private final static ExpressionPasser passer = new ExpressionPasser();
    private final static Scanner scanner = new Scanner(System.in);
    private final static String charset = System.getProperty("file.encoding") == null ? "UTF-8" : System.getProperty("file.encoding");
    private static Channel pc;

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
                evaluate(new String(stream.readAllBytes(),charset), "");
            }
        }
        while (true) {
            cmdInvoke(str -> evaluate(str, "\033[95mempty\033[0;39m"), str -> {
                try {
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
        while (true) {
            System.out.print(blockMode != 0 ? "......................> " : "EL-commander-line@FNU # ");
            String line = scanner.nextLine();
            if (!cmd.apply(line)) {
                return;
            }
            builder.append(line).append("\n");
            blockMode += leftBracket(line);

            if (blockMode == 0) {
                try {
                    consumer.accept(builder.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("\n");
                builder = new StringBuilder();
            }
        }
    }

    private static void evaluate(String stream, String x) {
        try {
            Object value = passer.parseExpression(stream).getValue(context);
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
            } else if (value.getClass().getName().startsWith("[")){
                int length = Array.getLength(value);
                StringJoiner joiner = new StringJoiner(",", "[", "]");
                for (int i = 0; i < length; i++) {
                    joiner.add(String.valueOf(Array.get(value,i)));
                }
                System.out.println(joiner);
            }else {
                System.out.println(value);
            }
        } catch (Exception e) {
            if (Objects.requireNonNull(CommanderLine.class.getResource("")).getProtocol().equals("jar")) {
                System.out.println(e.getMessage());
            } else {
                e.printStackTrace();
            }
        }
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


    public static void startRemote(int port, String aesKey, byte[] auth) throws InterruptedException {
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
                                        System.out.println(new PlaceholderParser("one client? connect", ctx.channel().remoteAddress()).configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_BLUE));
                                    }

                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String s) {
                                        if (s.trim().toLowerCase(Locale.ROOT).equals("::shutdown")) {
                                            ctx.channel().close();
                                            pc.close();
                                            return;
                                        }
                                        //清空日志缓存
                                        logRecorder.getLog();
                                        evaluate(s, "\033[95mempty\033[0;39m");
                                        ctx.channel().writeAndFlush(logRecorder.getLog());
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

    private static void startRemote() throws InterruptedException {
        System.out.print("port:");
        String line = scanner.nextLine();
        int port = Integer.parseInt(line);
        System.out.print("aes-key:");
        String aesKey;
        while (true) {
            aesKey = scanner.nextLine();
            if (aesKey.length() != 16) {
                System.out.println("\033[95maes-key'length must 16\033[0;39m");
                System.out.print("aes-key:");
            } else {
                break;
            }
        }
        System.out.print("auth-key:");
        String authKey = scanner.nextLine();
        byte[] bytes = EncryptUtil.stringToMD5(authKey).getBytes(StandardCharsets.UTF_8);
        startRemote(port, aesKey, bytes);
    }


    private static void startClient() throws InterruptedException {
        System.out.print("address(ip:port):");
        String line = scanner.nextLine();
        String ip = line.split(":")[0];
        int port = Integer.parseInt(line.split(":")[1]);
        System.out.print("aes-key:");
        String aesKey;
        while (true) {
            aesKey = scanner.nextLine();
            if (aesKey.length() != 16) {
                System.out.println("\033[95maes-key'length must 16\033[0;39m");
                System.out.print("aes-key:");
            } else {
                break;
            }
        }
        System.out.print("auth-key:");
        String authKey = scanner.nextLine();
        byte[] bytes = EncryptUtil.stringToMD5(authKey).getBytes(StandardCharsets.UTF_8);
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
                            channel.writeAndFlush(new String(stream.readAllBytes(),charset));
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


}
