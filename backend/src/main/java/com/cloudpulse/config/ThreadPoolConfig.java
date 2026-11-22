package com.cloudpulse.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolConfig.class);

    @Value("${app.monitoring.core-pool-size:10}")
    private int corePoolSize;

    @Value("${app.monitoring.max-pool-size:30}")
    private int maxPoolSize;

    @Value("${app.monitoring.queue-capacity:500}")
    private int queueCapacity;

    @Bean(name = "monitoringTaskExecutor")
    public Executor monitoringTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("monitoring-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.error("Monitoring task rejected! Thread pool queue full: active={}, queueSize={}",
                    threadPoolExecutor.getActiveCount(), threadPoolExecutor.getQueue().size());
            new ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(runnable, threadPoolExecutor);
        });
        executor.initialize();
        return executor;
    }

    @Bean(name = "notificationTaskExecutor")
    public ThreadPoolTaskExecutor notificationTaskExecutor(
            @Value("${app.notifications.worker.core-pool-size:2}") int notifCorePoolSize,
            @Value("${app.notifications.worker.max-pool-size:5}") int notifMaxPoolSize,
            @Value("${app.notifications.worker.queue-capacity:100}") int notifQueueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(notifCorePoolSize);
        executor.setMaxPoolSize(notifMaxPoolSize);
        executor.setQueueCapacity(notifQueueCapacity);
        executor.setThreadNamePrefix("notification-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.warn("Notification worker queue full: active={}, queueSize={}",
                    threadPoolExecutor.getActiveCount(), threadPoolExecutor.getQueue().size());
            new ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(runnable, threadPoolExecutor);
        });
        executor.initialize();
        return executor;
    }
}
