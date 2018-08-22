package org.slf4j.impl;

import com.chpengzh.slf4j.Level;
import com.chpengzh.slf4j.LogEvent;
import com.lmax.disruptor.dsl.Disruptor;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * 异步日志实例
 */
public class DemoAsyncLogger implements Logger {

    /**
     * 日志名称
     */
    private final String name;

    /**
     * 日志消息的异步执行线程
     */
    private final Disruptor<LogEvent> disruptor;

    DemoAsyncLogger(String name, Disruptor<LogEvent> disruptor) {
        this.name = name;
        this.disruptor = disruptor;
    }

    public String getName() {
        return name;
    }

    public boolean isTraceEnabled() {
        return DemoAsyncLoggerFactory.INSTANCE.getLevel().ordinal() <= Level.TRACE.ordinal();
    }

    @Override
    public void trace(String msg) {
        if (isTraceEnabled()) {
            asyncLog(Level.TRACE, msg);
        }
    }

    @Override
    public void trace(String format, Object arg) {
        if (isTraceEnabled()) {
            asyncLog(Level.TRACE, format, arg);
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            asyncLog(Level.TRACE, format, arg1, arg2);
        }
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (isTraceEnabled()) {
            asyncLog(Level.TRACE, format, arguments);
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (isTraceEnabled()) {
            asyncLog(Level.TRACE, null, t);
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled();
    }

    @Override
    public void trace(Marker marker, String msg) {
        trace(msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        trace(format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        trace(format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        trace(format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        trace(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return DemoAsyncLoggerFactory.INSTANCE.getLevel().ordinal() <= Level.DEBUG.ordinal();
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled()) {
            asyncLog(Level.DEBUG, msg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if (isDebugEnabled()) {
            asyncLog(Level.DEBUG, format, arg);
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            asyncLog(Level.DEBUG, format, arg1, arg2);
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (isDebugEnabled()) {
            asyncLog(Level.DEBUG, format, arguments);
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (isDebugEnabled()) {
            asyncLog(Level.DEBUG, null, t);
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public void debug(Marker marker, String msg) {
        debug(msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        debug(format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        debug(format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        debug(format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return DemoAsyncLoggerFactory.INSTANCE.getLevel().ordinal() <= Level.INFO.ordinal();
    }

    @Override
    public void info(String msg) {
        if (isInfoEnabled()) {
            asyncLog(Level.INFO, msg);
        }
    }

    @Override
    public void info(String format, Object arg) {
        if (isInfoEnabled()) {
            asyncLog(Level.INFO, format, arg);
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            asyncLog(Level.INFO, format, arg1, arg2);
        }
    }

    @Override
    public void info(String format, Object... arguments) {
        if (isInfoEnabled()) {
            asyncLog(Level.INFO, format, arguments);
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        if (isInfoEnabled()) {
            asyncLog(Level.INFO, null, t);
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    @Override
    public void info(Marker marker, String msg) {
        info(msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        info(format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        info(format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        info(format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        info(msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return DemoAsyncLoggerFactory.INSTANCE.getLevel().ordinal() <= Level.WARN.ordinal();
    }

    @Override
    public void warn(String msg) {
        if (isWarnEnabled()) {
            asyncLog(Level.WARN, msg);
        }
    }

    @Override
    public void warn(String format, Object arg) {
        if (isWarnEnabled()) {
            asyncLog(Level.WARN, format, arg);
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (isWarnEnabled()) {
            asyncLog(Level.WARN, format, arguments);
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            asyncLog(Level.WARN, format, arg1, arg2);
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (isWarnEnabled()) {
            asyncLog(Level.WARN, msg, t);
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    @Override
    public void warn(Marker marker, String msg) {
        warn(msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        warn(format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        warn(format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        warn(format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return DemoAsyncLoggerFactory.INSTANCE.getLevel().ordinal() <= Level.ERROR.ordinal();
    }

    @Override
    public void error(String msg) {
        if (isErrorEnabled()) {
            asyncLog(Level.ERROR, msg);
        }
    }

    @Override
    public void error(String format, Object arg) {
        if (isErrorEnabled()) {
            asyncLog(Level.ERROR, format, arg);
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            asyncLog(Level.ERROR, format, arg1, arg2);
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        if (isErrorEnabled()) {
            asyncLog(Level.ERROR, format, arguments);
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        if (isErrorEnabled()) {
            asyncLog(Level.ERROR, msg, t);
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }

    @Override
    public void error(Marker marker, String msg) {
        error(msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        error(format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        error(format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        error(format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        error(msg, t);
    }

    private void asyncLog(Level level, String format, Object arg) {
        FormattingTuple formatter = MessageFormatter.format(format, arg);
        asyncLog(level, formatter.getMessage(), formatter.getThrowable());
    }

    private void asyncLog(Level level, String format, Object arg1, Object arg2) {
        FormattingTuple formatter = MessageFormatter.format(format, arg1, arg2);
        asyncLog(level, formatter.getMessage(), formatter.getThrowable());
    }

    private void asyncLog(Level level, String format, Object[] args) {
        FormattingTuple formatter = MessageFormatter.arrayFormat(format, args);
        asyncLog(level, formatter.getMessage(), formatter.getThrowable());
    }

    private void asyncLog(Level level, String msg, Throwable err) {
        if (msg == null && err == null) {
            throw new IllegalArgumentException("both message and error are null");
        }
        StringBuilder msgBuilder = new StringBuilder();
        if (msg != null) {
            msgBuilder.append(msg).append("\n");
        }
        if (err != null) {
            msgBuilder.append(err.toString());
            for (StackTraceElement stackTrace : err.getStackTrace()) {
                msgBuilder.append(stackTrace).append("\n");
            }
        }
        msgBuilder.setLength(msgBuilder.length() - 1);
        asyncLog(level, msgBuilder.toString());
    }

    private void asyncLog(Level level, String msg) {
        long sequence = disruptor.getRingBuffer().next();
        try {
            LogEvent event = disruptor.getRingBuffer().get(sequence);
            event.setLevel(level);
            event.setMsgSupplier(() -> String.format("%s\t%s", name, msg));
        } finally {
            disruptor.getRingBuffer().publish(sequence);
        }
    }
}
