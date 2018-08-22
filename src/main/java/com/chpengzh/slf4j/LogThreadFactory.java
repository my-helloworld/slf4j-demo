package com.chpengzh.slf4j;

import java.util.concurrent.ThreadFactory;

/**
 * 日志落盘线程池工厂类
 */
public class LogThreadFactory implements ThreadFactory {

    private final String file;

    public LogThreadFactory(String file) {
        this.file = file;
    }

    /**
     * 创建日志落盘线程，日志的异步落盘将在这个线程中执行
     *
     * @param disruptorRunnable disruptor 消费任务
     * @return 线程实例
     */
    @Override
    public Thread newThread(Runnable disruptorRunnable) {
        return new Thread("demo-log") {
            @Override
            public void run() {
                System.err.printf("disruptor thread is started, output log file: %s%n", file);
                disruptorRunnable.run();
                System.err.println("disruptor thread is terminated");
            }
        };
    }
}
