package com.atguigu.tingshu.album.config;

import com.atguigu.tingshu.common.execption.GuiguException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;

import java.util.concurrent.Executor;

/**
 * Spring Boot 异步任务全局配置
 * 实现 AsyncConfigurer 提供默认线程池 + 统一异常处理
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    /**
     * 核心线程数 = CPU 核心数 * 2 （适用于 I/O 密集型任务）
     * 可根据实际场景调整：CPU 密集型设为 CPU 核心数 + 1
     */
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 最大线程数：根据业务峰值设定，建议为核心线程数的 2~3 倍
     */
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 3;

    /**
     * 阻塞队列容量：防止无限制堆积导致 OOM
     */
    private static final int QUEUE_CAPACITY = 200;

    /**
     * 非核心线程空闲存活时间（秒）
     */
    private static final int KEEP_ALIVE_SECONDS = 60;

    /**
     * 线程名前缀：便于日志排查
     */
    private static final String THREAD_NAME_PREFIX = "async-service-";

    /**
     * 优雅停机：等待任务完成的最大时间（秒）
     */
    private static final int AWAIT_TERMINATION_SECONDS = 60;
    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);


    /**
     * 定义默认异步执行器（全局生效）
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(CORE_POOL_SIZE);
        // 最大线程数
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        // 阻塞队列容量
        executor.setQueueCapacity(QUEUE_CAPACITY);
        // 非核心线程空闲存活时间
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        // 线程名前缀
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);

        // 拒绝策略：由调用线程执行（避免任务丢失，同时减缓提交压力）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 是否允许核心线程超时（默认 false，保持核心线程常驻）
        executor.setAllowCoreThreadTimeOut(false);

        // 优雅停机：应用关闭时等待任务执行完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 最大等待时间（超时强制终止）
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);

        executor.initialize();
        return executor;
    }

    /**
     * 全局异步异常处理器（处理 @Async 方法未捕获的异常）
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            log.error("[Async Error] Method: %s, Args: %s, Exception: %s%n {}{}{}",
                    method.getName(),
                    Arrays.toString(objects),
                    throwable.getMessage());
        };
    }




}
