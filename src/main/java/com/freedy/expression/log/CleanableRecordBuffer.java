package com.freedy.expression.log;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.ref.Cleaner;

/**
 * @author Freedy
 * @date 2021/12/1 22:53
 */
class CleanableRecordBuffer implements LogRecord {
    private final int size;
    private final byte[] buffer;
    private int rIndex, wIndex = 0;

    private final PrintStream std;
    private final PrintStream err;
    private Cleaner cleaner=Cleaner.create();

    CleanableRecordBuffer(int bufferSize, LogRecorder proxyLogRecord) {
        this.size = bufferSize;
        this.buffer = new byte[bufferSize];
        this.std = System.out;
        this.err = System.err;
        System.setOut(new PrintStream(new LogInterceptor(std)));
        System.setErr(new PrintStream(new LogInterceptor(err)));
        cleaner.register(proxyLogRecord,()->{
            System.setOut(std);
            System.setErr(err);
            cleaner=null;
        });
    }

    private class LogInterceptor extends OutputStream {

        private final PrintStream printStream;

        public LogInterceptor(PrintStream printStream) {
            this.printStream = printStream;
        }

        @Override
        public void write(int b) {
            write(new byte[]{(byte) b});
        }


        @Override
        public void write(byte[] b) {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            printStream.write(b, off, len);
            synchronized (buffer) {
                //只取b后BUFFER_SIZE长度的数组
                if (len > size) {
                    off += len - size;
                    int remain = size - wIndex;
                    System.arraycopy(b, off, buffer, wIndex, remain);
                    System.arraycopy(b, off + remain, buffer, 0, size - remain);
                    if (rIndex < wIndex) {
                        rIndex = wIndex;
                    }
                    return;
                }
                int exceed = wIndex + len - size;
                if (exceed > 0) {
                    int remain = size - wIndex;
                    System.arraycopy(b, off, buffer, wIndex, remain);
                    System.arraycopy(b, off + remain, buffer, 0, exceed);
                    wIndex = exceed;
                    if (rIndex < wIndex) {
                        rIndex = wIndex;
                    }
                    return;
                }

                System.arraycopy(b, off, buffer, wIndex, len);
                wIndex += len;
            }
        }
    }


    public String getLog() {
        if (rIndex == wIndex) return null;
        String log;
        synchronized (buffer) {
            log = rIndex < wIndex ? new String(buffer, rIndex, wIndex - rIndex) : new String(buffer, rIndex, size - rIndex) + new String(buffer, 0, wIndex);
            rIndex = wIndex;
        }
        return log;
    }

    public String getLog(int expectLength) {
        String log;
        synchronized (buffer) {
            if (rIndex < wIndex) {
                int min = Math.min(expectLength, wIndex - rIndex);
                log = new String(buffer, rIndex, min);
                rIndex += min;
            } else {
                int remain = size - rIndex;
                if (expectLength < remain) {
                    log = new String(buffer, rIndex, expectLength);
                    rIndex += expectLength;
                } else {
                    String s = new String(buffer, rIndex, remain);
                    int min = Math.min(wIndex, expectLength - remain);
                    log = s + new String(buffer, 0, min);
                    rIndex += min;
                }
            }
        }
        return log;
    }

    public void clear() {
        synchronized (buffer) {
            rIndex = 0;
            wIndex = 0;
        }
    }
}
