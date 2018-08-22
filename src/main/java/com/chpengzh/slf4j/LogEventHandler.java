package com.chpengzh.slf4j;

import com.lmax.disruptor.EventHandler;

import java.io.PrintWriter;

public class LogEventHandler implements EventHandler<LogEvent> {

    private final PrintWriter printer;

    public LogEventHandler(PrintWriter printer) {
        this.printer = printer;
    }

    @Override
    public void onEvent(LogEvent event, long sequence, boolean endOfBatch) throws Exception {
        System.out.printf("%d [%s] Thread %d-%s: %s%n",
                sequence,
                event.getLevel(),
                Thread.currentThread().getId(),
                Thread.currentThread().getName(),
                event);
        printer.printf("%d [%s] Thread %d-%s: %s%n",
                sequence,
                event.getLevel(),
                Thread.currentThread().getId(),
                Thread.currentThread().getName(),
                event);
        printer.flush();
    }

}
