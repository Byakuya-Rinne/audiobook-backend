package com.atguigu.tingshu.common.threadpool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
public class ThreadPoolConfig {


    public ThreadPoolExecutor getThreadPoolExecutor(){
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors(),
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        //提前创建线程池中的核心线程，而不是等拿到任务才创建核心线程
        threadPoolExecutor.prestartAllCoreThreads();

        return threadPoolExecutor;
    }


}
