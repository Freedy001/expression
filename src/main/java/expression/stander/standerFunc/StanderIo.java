package com.freedy.expression.stander.standerFunc;

import com.freedy.expression.JavaAdapter;
import com.freedy.expression.ScriptStarter;
import com.freedy.expression.stander.ExpressionFunc;
import lombok.Cleanup;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.freedy.expression.SysConstant.CHARSET;
import static com.freedy.expression.SysConstant.SEPARATOR;

/**
 * @author Freedy
 * @date 2022/3/6 0:10
 */
public class StanderIo extends AbstractStanderFunc {

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
        return ScriptStarter.stdin("");
    }

    @SneakyThrows
    @ExpressionFunc("read all byte from giving file")
    public String cat(String path) {
        @Cleanup FileInputStream inputStream = new FileInputStream(file(path));
        return new String(JavaAdapter.readAllBytes(inputStream), CHARSET);
    }

    @ExpressionFunc("get file obj from your give path")
    public File file(String path) {
        File file;
        if (path.startsWith("/")) {
            file = new File(path);
        } else {
            file = new File(context.getCurrentPath() + "/" + path);
        }
        return file;
    }

    @ExpressionFunc("change dir")
    public void cd(String arg) {
        File file = file(arg);
        if (file.exists()) {
            String path = purePath(file.getAbsolutePath());
            context.setCurrentPath(path);
            System.out.println("switch dir to " + path);
        } else {
            System.out.println("No such file or directory");
        }
    }

    @ExpressionFunc("list all files under relative path")
    public File[] ls(String path) {
        return new File(path).listFiles();
    }

    @SneakyThrows
    @ExpressionFunc("write string to dest file")
    public void write(String content, String path) {
        writeByte(content.getBytes("UTF-8"), path);
    }

    @SneakyThrows
    @ExpressionFunc("write bytes to dest file")
    public void writeByte(byte[] bytes, String path) {
        @Cleanup FileOutputStream outputStream = new FileOutputStream(file(path));
        outputStream.write(bytes);
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
