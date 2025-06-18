package com.embabel.example.travel

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean("virtualThreadTaskExecutor")
    fun virtualThreadTaskExecutor(): TaskExecutor {
        val executor = SimpleAsyncTaskExecutor()
        executor.setVirtualThreads(true)
        executor.setTaskDecorator { runnable ->
            // Optional: add context propagation or logging
            runnable
        }
        return executor
    }
}

@Service
class Asyncer(
    @Qualifier("virtualThreadTaskExecutor") private val executor: Executor
) {

    fun <T> async(block: () -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync(block, executor)
    }

}