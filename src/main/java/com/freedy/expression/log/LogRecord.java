package com.freedy.expression.log;

/**
 * @author Freedy
 * @date 2022/8/8 4:00
 */
public interface LogRecord {
    String getLog();

    String getLog(int expectLength);

    void clear();
}