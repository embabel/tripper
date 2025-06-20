/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
