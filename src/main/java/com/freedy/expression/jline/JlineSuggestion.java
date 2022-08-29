package com.freedy.expression.jline;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.util.List;

/**
 * @author Freedy
 * @date 2022/8/29 0:30
 */
public abstract class JlineSuggestion {
    protected final static Terminal terminal;
    public LineReader reader;

    static {
        try {
            terminal = TerminalBuilder.terminal();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JlineSuggestion() {
        reader = LineReaderBuilder
                .builder()
                .terminal(terminal)
                .completer(this::suggest)
                .highlighter(new DefaultHighlighter())
                .parser(new DefaultParser().escapeChars(new char[]{}))
                .build();
    }

    public abstract void suggest(LineReader reader, ParsedLine line, List<Candidate> candidates);

    public String stdin(String placeholder) {
        return reader.readLine(placeholder);
    }

    public void clearScreen() {
        terminal.puts(InfoCmp.Capability.clear_screen);
    }

}
