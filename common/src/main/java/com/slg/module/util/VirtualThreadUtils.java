package com.slg.module.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 虚拟线程工具类 (Java 21+)
 */
public class VirtualThreadUtils {

    // 默认虚拟线程执行器
    private static final ExecutorService VIRTUAL_EXECUTOR =
            Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 使用虚拟线程执行任务
     *
     * @param task 要执行的任务
     * @return 提交的任务Future
     */
    public static Future<?> execute(Runnable task) {
        return VIRTUAL_EXECUTOR.submit(task);
    }

    /**
     * 批量执行任务并等待所有任务完成
     *
     * @param tasks 任务列表
     * @throws InterruptedException 如果等待过程被中断
     * @throws ExecutionException   如果任何任务抛出异常
     */
    public static void executeAllAndWait(List<Runnable> tasks)
            throws InterruptedException, ExecutionException {

        List<Future<?>> futures = new ArrayList<>();
        for (Runnable task : tasks) {
            futures.add(execute(task));
        }

        for (Future<?> future : futures) {
            future.get();
        }
    }

    /**
     * 执行任务并获取结果（带超时）
     *
     * @param task    可调用任务
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 任务结果
     * @throws TimeoutException 如果任务超时
     */
    public static <T> T executeWithTimeout(Callable<T> task, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<T> future = executor.submit(task);
            return future.get(timeout, unit);
        }
    }

    /**
     * 创建带名称的虚拟线程
     *
     * @param name 线程名称
     * @param task 要执行的任务
     * @return 启动的线程
     */
    public static Thread startNamedThread(String name, Runnable task) {
        return Thread.ofVirtual()
                .name(name)
                .start(task);
    }

    /**
     * 创建虚拟线程（虚拟线程总是守护线程）
     *
     * @param task 要执行的任务
     * @return 启动的线程
     */
    public static Thread startThread(Runnable task) {
        return Thread.ofVirtual().start(task);
    }

    /**
     * 带异常处理的虚拟线程执行
     *
     * @param task             要执行的任务
     * @param exceptionHandler 异常处理器
     */
    public static void executeWithHandler(Runnable task, Consumer<Throwable> exceptionHandler) {
        execute(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                exceptionHandler.accept(t);
            }
        });
    }

    /**
     * 带资源清理的安全执行
     *
     * @param task 需要资源清理的任务
     */
    public static void executeWithCleanup(AutoCloseableTask task) {
        execute(() -> {
            try (task) {
                task.run();
            } catch (Exception e) {
                Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), e);
            }
        });
    }

    /**
     * 自动关闭任务接口
     */
    @FunctionalInterface
    public interface AutoCloseableTask extends Runnable, AutoCloseable {
        @Override
        default void close() throws Exception {
            // 默认空实现，可按需重写
        }
    }

    /**
     * 监控虚拟线程状态
     */
    public static void monitorVirtualThreads() {
        Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.isVirtual() && thread.isAlive())
                .forEach(thread -> System.out.println(
                        "VirtualThread[%s] - State: %s".formatted(thread.getName(), thread.getState())
                ));
    }

    /**
     * 自定义虚拟线程工厂
     *
     * @param namePrefix 线程名前缀
     * @return 线程工厂
     */
    public static ThreadFactory virtualThreadFactory(String namePrefix) {
        AtomicInteger count = new AtomicInteger(1);
        return r -> Thread.ofVirtual()
                .name(namePrefix + "-" + count.getAndIncrement())
                .uncaughtExceptionHandler((t, e) ->
                        System.err.println("Exception in thread " + t.getName() + ": " + e)
                ).factory().newThread(r);
    }

    /**
     * 关闭线程池并等待终止
     */
    public static void shutdown() {
        VIRTUAL_EXECUTOR.shutdown();
        try {
            if (!VIRTUAL_EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
                VIRTUAL_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            VIRTUAL_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    //import java.util.List;
    //import java.util.concurrent.*;
    //import java.util.function.Consumer;
    //
    //public class VirtualThreadUtilsExamples {
    //
    //    public static void main(String[] args) throws Exception {
    //        // 1. 基础任务执行
    //        executeExample();
    //
    //        // 2. 批量执行并等待
    //        executeAllAndWaitExample();
    //
    //        // 3. 带超时执行
    //        executeWithTimeoutExample();
    //
    //        // 4. 具名线程
    //        startNamedThreadExample();
    //
    //        // 5. 直接启动线程
    //        startThreadExample();
    //
    //        // 6. 带异常处理
    //        executeWithHandlerExample();
    //
    //        // 7. 带资源清理
    //        executeWithCleanupExample();
    //
    //        // 8. 监控线程状态
    //        monitorVirtualThreadsExample();
    //
    //        // 9. 自定义线程工厂
    //        virtualThreadFactoryExample();
    //
    //        // 10. 关闭线程池
    //        shutdownExample();
    //    }
    //
    //    // 1. execute 示例
    //    private static void executeExample() {
    //        System.out.println("\n=== execute 示例 ===");
    //        VirtualThreadUtils.execute(() -> {
    //            System.out.println("任务在虚拟线程中执行: " + Thread.currentThread());
    //        });
    //    }
    //
    //    // 2. executeAllAndWait 示例
    //    private static void executeAllAndWaitExample() throws Exception {
    //        System.out.println("\n=== executeAllAndWait 示例 ===");
    //        List<Runnable> tasks = List.of(
    //            () -> System.out.println("任务1 - " + Thread.currentThread()),
    //            () -> System.out.println("任务2 - " + Thread.currentThread()),
    //            () -> System.out.println("任务3 - " + Thread.currentThread())
    //        );
    //
    //        VirtualThreadUtils.executeAllAndWait(tasks);
    //        System.out.println("所有任务已完成");
    //    }
    //
    //    // 3. executeWithTimeout 示例
    //    private static void executeWithTimeoutExample() {
    //        System.out.println("\n=== executeWithTimeout 示例 ===");
    //        try {
    //            String result = VirtualThreadUtils.executeWithTimeout(() -> {
    //                Thread.sleep(500);
    //                return "操作成功";
    //            }, 1, TimeUnit.SECONDS);
    //
    //            System.out.println("结果: " + result);
    //        } catch (TimeoutException e) {
    //            System.err.println("任务超时");
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //        }
    //    }
    //
    //    // 4. startNamedThread 示例
    //    private static void startNamedThreadExample() {
    //        System.out.println("\n=== startNamedThread 示例 ===");
    //        VirtualThreadUtils.startNamedThread("文件处理线程", () -> {
    //            System.out.println("具名线程执行: " + Thread.currentThread().getName());
    //        });
    //    }
    //
    //    // 5. startThread 示例
    //    private static void startThreadExample() {
    //        System.out.println("\n=== startThread 示例 ===");
    //        VirtualThreadUtils.startThread(() -> {
    //            System.out.println("直接启动的虚拟线程: " + Thread.currentThread());
    //            System.out.println("是否为守护线程: " + Thread.currentThread().isDaemon());
    //        });
    //    }
    //
    //    // 6. executeWithHandler 示例
    //    private static void executeWithHandlerExample() {
    //        System.out.println("\n=== executeWithHandler 示例 ===");
    //        VirtualThreadUtils.executeWithHandler(() -> {
    //            if (Math.random() > 0.5) {
    //                throw new RuntimeException("随机错误");
    //            }
    //            System.out.println("任务成功执行");
    //        }, e -> System.err.println("捕获异常: " + e.getMessage()));
    //    }
    //
    //    // 7. executeWithCleanup 示例
    //    private static void executeWithCleanupExample() {
    //        System.out.println("\n=== executeWithCleanup 示例 ===");
    //        VirtualThreadUtils.executeWithCleanup(new VirtualThreadUtils.AutoCloseableTask() {
    //            @Override
    //            public void run() {
    //                System.out.println("使用资源执行任务");
    //                // 模拟资源使用
    //            }
    //
    //            @Override
    //            public void close() throws Exception {
    //                System.out.println("自动清理资源");
    //            }
    //        });
    //    }
    //
    //    // 8. monitorVirtualThreads 示例
    //    private static void monitorVirtualThreadsExample() throws InterruptedException {
    //        System.out.println("\n=== monitorVirtualThreads 示例 ===");
    //        VirtualThreadUtils.startNamedThread("监控线程-1", () -> {
    //            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
    //        });
    //
    //        VirtualThreadUtils.startNamedThread("监控线程-2", () -> {
    //            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
    //        });
    //
    //        // 给线程启动时间
    //        Thread.sleep(500);
    //
    //        System.out.println("当前虚拟线程状态:");
    //        VirtualThreadUtils.monitorVirtualThreads();
    //    }
    //
    //    // 9. virtualThreadFactory 示例
    //    private static void virtualThreadFactoryExample() {
    //        System.out.println("\n=== virtualThreadFactory 示例 ===");
    //        ThreadFactory factory = VirtualThreadUtils.virtualThreadFactory("db-worker");
    //
    //        // 使用自定义工厂创建线程
    //        Thread thread1 = factory.newThread(() -> System.out.println("数据库操作1"));
    //        Thread thread2 = factory.newThread(() -> System.out.println("数据库操作2"));
    //
    //        thread1.start();
    //        thread2.start();
    //
    //        // 测试异常处理
    //        Thread thread3 = factory.newThread(() -> {
    //            throw new RuntimeException("数据库连接失败");
    //        });
    //        thread3.start();
    //    }
    //
    //    // 10. shutdown 示例
    //    private static void shutdownExample() throws InterruptedException {
    //        System.out.println("\n=== shutdown 示例 ===");
    //        // 提交一些任务
    //        for (int i = 0; i < 5; i++) {
    //            VirtualThreadUtils.execute(() -> {
    //                try {
    //                    Thread.sleep(100);
    //                    System.out.println("任务完成");
    //                } catch (InterruptedException e) {
    //                    System.out.println("任务被中断");
    //                }
    //            });
    //        }
    //
    //        // 关闭线程池
    //        VirtualThreadUtils.shutdown();
    //        System.out.println("线程池已关闭");
    //    }
    //}
}
