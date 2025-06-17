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
class AsyncWrapper(
    @Qualifier("virtualThreadTaskExecutor") private val executor: Executor
) {

    // For functions with no parameters
    fun <T> async(block: () -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync(block, executor)
    }

    // For functions with one parameter
    fun <P, T> async(param: P, block: (P) -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync({ block(param) }, executor)
    }

    // For functions with two parameters
    fun <P1, P2, T> async(param1: P1, param2: P2, block: (P1, P2) -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync({ block(param1, param2) }, executor)
    }

    // For functions with three parameters
    fun <P1, P2, P3, T> async(
        param1: P1,
        param2: P2,
        param3: P3,
        block: (P1, P2, P3) -> T
    ): CompletableFuture<T> {
        return CompletableFuture.supplyAsync({ block(param1, param2, param3) }, executor)
    }

    // Generic vararg version for any number of parameters
    fun <T> asyncVararg(vararg params: Any?, block: (Array<out Any?>) -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync({ block(params) }, executor)
    }

    // With error handling and callbacks
    fun <T> asyncWithCallback(
        block: () -> T,
        onSuccess: (T) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ): CompletableFuture<T> {
        return async(block).whenComplete { result, throwable ->
            when {
                throwable != null -> onError(throwable)
                result != null -> onSuccess(result)
            }
        }
    }

    // For suspend-like behavior without coroutines
    fun <T> asyncThen(
        block: () -> T,
        then: (T) -> Unit
    ): CompletableFuture<Void> {
        return async(block).thenAccept(then)
    }

    // Chain multiple async operations
    fun <T, U> asyncThenAsync(
        block: () -> T,
        then: (T) -> U
    ): CompletableFuture<U> {
        return async(block).thenCompose { result ->
            async { then(result) }
        }
    }
}