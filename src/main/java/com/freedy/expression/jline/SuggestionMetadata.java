package com.freedy.expression.jline;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jline.reader.ParsedLine;

import java.io.Serializable;
import java.util.List;

/**
 * @author Freedy
 * @date 2022/8/29 2:53
 */
@Data
@AllArgsConstructor
public class SuggestionMetadata implements ParsedLine, Serializable {
    private String word;
    private int wordCursor;

    @Override
    public String word() {
        return word;
    }

    @Override
    public int wordCursor() {
        return wordCursor;
    }

    @Override
    public int wordIndex() {
        return 0;
    }

    @Override
    public List<String> words() {
        return null;
    }

    @Override
    public String line() {
        return null;
    }

    @Override
    public int cursor() {
        return 0;
    }
}
