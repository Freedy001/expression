package com.freedy.expression.stander.standerFunc;

import com.freedy.expression.CommanderLine;
import com.freedy.expression.stander.ExpressionFunc;
import lombok.Cleanup;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * @author Freedy
 * @date 2022/3/6 0:10
 */
public class StanderIo extends AbstractStanderFunc{
    private final static String SEPARATOR = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "\\\\" : "/";


    @ExpressionFunc("same as System.out.println()")
    public void print(Object o) {
        System.out.println(o);
    }

    @ExpressionFunc("same as System.out.print()")
    public void printInline(Object o) {
        System.out.print(o);
    }

    @ExpressionFunc("stander input same as new Scanner(System.in).nextLine()")
    public String stdin() {
        if (CommanderLine.JAR_ENV) {
            return CommanderLine.READER.readLine();
        } else {
            return CommanderLine.SCANNER.nextLine();
        }
    }

    @SneakyThrows
    @ExpressionFunc("read all byte from giving file")
    public String cat(String path){
        File file;
        if (path.startsWith("/")) {
            file = new File(path);
        } else {
            file = new File(context.getCurrentPath() + "/" + path);
        }
        @Cleanup FileInputStream inputStream = new FileInputStream(file);
        return new String(inputStream.readAllBytes(), CHARSET);
    }

    @ExpressionFunc("get file obj from your give path")
    public File file(String path){
        File file;
        if (path.startsWith("/")) {
            file = new File(path);
        } else {
            file = new File(context.getCurrentPath() + "/" + path);
        }
        return file;
    }

    @ExpressionFunc("change dir")
    public void cd(String arg){
        File file;
        if (arg.startsWith("/")) {
            file = new File(arg);
        } else {
            file = new File(context.getCurrentPath() + "/" + arg);
        }
        if (file.exists()) {
            String path = purePath(file.getAbsolutePath());
            context.setCurrentPath(path);
            System.out.println("switch dir to " + path);
        } else {
            System.out.println("No such file or directory");
        }
    }

    @ExpressionFunc("list all files under relative path")
    public String ls(String path){
        return String.join("\n", Objects.requireNonNull(new File(path).list()));
    }

    private String purePath(String path) {
        ArrayList<String> pathList = new ArrayList<>();
        for (String s : path.split(SEPARATOR)) {
            if (s.equals(".")) {
                continue;
            }
            if (s.equals("..")) {
                pathList.remove(pathList.size() - 1);
                continue;
            }
            pathList.add(s);
        }
        StringJoiner builder = new StringJoiner(SEPARATOR);
        pathList.forEach(builder::add);
        return pathList.size() == 1 ? builder + SEPARATOR : builder.toString();
    }

}
