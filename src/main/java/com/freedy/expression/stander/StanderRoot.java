package com.freedy.expression.stander;

import com.freedy.expression.JavaAdapter;
import com.freedy.expression.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Freedy
 * @date 2022/7/1 1:37
 */
@Getter
@Setter
@SuppressWarnings("unused")
public class StanderRoot {
    private final static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private final StanderEvaluationContext ctx;
    private final Properties env = System.getProperties();
    private String pwd;
    private String ls;
    private Date time;
    private String help;
    private Set<String> allVar;


    public StanderRoot(StanderEvaluationContext ctx) {
        this.ctx = ctx;
    }

    public Date getTime() {
        return new Date();
    }

    public String getHelp() {
        if (StringUtils.hasText(help)) {
            return help;
        }
        StringBuilder builder = new StringBuilder();
        new TreeMap<>(ctx.getSelfFuncHelp()).forEach((k, v) -> builder.append("\033[95m").append(k).append("\033[0;39m").append(50 - k.length() < 0 ? "\n\t---" : JavaAdapter.repeat(" ",50 - k.length())).append(v.contains("\n") ? v.substring(0, v.indexOf("\n")) : v).append("\n"));
        help = builder.toString();
        return help;
    }

    public Set<String> getAllVar() {
        return ctx.allVariables();
    }

    public String getLs() {
        String[] fileArr = Arrays.stream(Objects.requireNonNull(new File(ctx.getCurrentPath()).listFiles())).map(f -> {
            StringBuilder builder = new StringBuilder();
            builder.append(f.isDirectory() ? "d" : "-");
            builder.append(f.canRead() ? "r" : "-");
            builder.append(f.canWrite() ? "w" : "-");
            builder.append(f.canExecute() ? "x" : "-");
            builder.append("\t");


            BasicFileAttributeView basicview = Files.getFileAttributeView(f.toPath(), BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            BasicFileAttributes attr = null;
            try {
                attr = basicview.readAttributes();
            } catch (IOException e) {
                e.printStackTrace();
            }

            builder.append(attr != null ? SIMPLE_DATE_FORMAT.format(new Date(attr.creationTime().toMillis())) : "????-??-?? ??:??:??");
            builder.append("\t\t");

            StringBuilder b = new StringBuilder();
            char[] chars = (f.length() + "").toCharArray();
            for (int i = chars.length - 1, r = 1; i >= 0; i--, r++) {
                b.append(r % 3 == 0 && i != 0 ? chars[i] + "," : chars[i]);
            }
            b.reverse();
            if (b.length() < 20) {
                b.append(JavaAdapter.repeat(" ",20 - b.length()));
            }
            builder.append(b);
            builder.append("\t");
            builder.append(f.isDirectory() ? "\033[91m" : f.canExecute() ? "\033[93m" : "").append(f.getName()).append("\033[0;39m");

            return builder.toString();
        }).toArray(String[]::new);
        return String.join("\n", fileArr);
    }

    public String getPwd() {
        return new File(ctx.getCurrentPath()).getAbsolutePath();
    }
}
