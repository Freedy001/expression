package com.freedy.expression.jline;

import com.freedy.expression.utils.Color;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.SneakyThrows;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Freedy
 * @date 2022/8/29 0:55
 */
public class ClientJlineSuggestion extends JlineSuggestion {

    private final Channel clientChannel;
    private final BlockingQueue<List<SerializableCandidate>> blockingQueue = new LinkedBlockingQueue<>();

    public ClientJlineSuggestion(Channel channel) {
        clientChannel = channel;
        try {
            clientChannel.pipeline().remove(processCandidateResp.class);
        } catch (NoSuchElementException ignore) {}
        clientChannel.pipeline().addLast(new processCandidateResp());
    }

    @Override
    @SneakyThrows
    public void suggest(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        clientChannel.writeAndFlush(new SuggestionMetadata(line.word(),line.wordCursor()));
        List<SerializableCandidate> take = blockingQueue.poll(5, TimeUnit.SECONDS);
        if (take == null) {
            System.out.println("\n" + Color.dRed("request tip timeout!"));
            return;
        }
        candidates.addAll(take.stream().map(SerializableCandidate::toCandidate).toList());
    }

    private class processCandidateResp extends SimpleChannelInboundHandler<List<SerializableCandidate>> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, List<SerializableCandidate> msg) throws Exception {
            blockingQueue.put(msg);
        }
    }
}
