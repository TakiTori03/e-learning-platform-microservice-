package com.hust.mediaservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    /**
     * Specialized Worker Pool for heavy video transcoding operations.
     * By limiting maxPoolSize to 2, we guarantee that at most 2 CPU-intensive FFmpeg 
     * transcodings will run in parallel, protecting the microservice and system gateway 
     * from 100% CPU saturation denial-of-service conditions.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(1); // Keep 1 warm thread for processing
        executor.setMaxPoolSize(2);  // STRICT CAP: Allow at most 2 parallel transcodings
        executor.setQueueCapacity(100); // The next 100 requests wait in a safe memory queue
        
        executor.setThreadNamePrefix("VideoWorker-");
        
        // Ensure graceful shutdown when container is stopping
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
}
