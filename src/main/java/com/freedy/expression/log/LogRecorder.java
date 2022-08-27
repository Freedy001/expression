package com.freedy.expression.log;

/**
 * 会记录程序的标准输出与错误输出,其数据结构是一个环状队列。<br/>
 * 如果生产者产生的日志没被消费者及时处理，则生产者早期产生的日志将会被自动丢弃。
 * @author Freedy
 * @date 2022/8/8 3:56
 */
public class LogRecorder implements LogRecord{

    private final CleanableRecordBuffer recordBuffer;

    public LogRecorder() {
        this(10 * 1024 * 1024);
    }

    public LogRecorder(int bufferSize) {
        recordBuffer=new CleanableRecordBuffer(bufferSize,this);
    }


    @Override
    public String getLog() {
        return recordBuffer.getLog();
    }

    @Override
    public String getLog(int expectLength) {
        return recordBuffer.getLog(expectLength);
    }

    @Override
    public void clear() {
        recordBuffer.clear();
    }
}
