package com.freedy.expression.jline;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jline.reader.Candidate;

import java.io.Serializable;

/**
 * @author Freedy
 * @date 2022/8/29 3:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SerializableCandidate implements Serializable {
    private String value;
    private String displ;
    private String group;
    private String descr;
    private String suffix;
    private String key;
    private boolean complete;
    private int sort;

    public Candidate toCandidate(){
        return new Candidate(value, displ, group, descr, suffix, key, complete, sort);
    }
}