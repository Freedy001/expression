package com.freedy.expression.jline;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

public class NormalJlineTerminal extends JlineTerminal{
    @Override
    public void suggest(LineReader reader, ParsedLine line, List<Candidate> candidates) {

    }
}
