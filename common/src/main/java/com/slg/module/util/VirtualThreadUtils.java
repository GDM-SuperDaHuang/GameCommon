import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public class VirtualThreadUtils {

      // 创建固定数量平台线程的 executor，用于执行阻塞操作
    public static ExecutorService createFixedExecutor(int threadCount) {
        return Executors.newFixedThreadPool(threadCount);
    }

    // 创建基于 ForkJoinPool 的 executor
    public static ExecutorService createWorkStealingExecutor() {
        return Executors.newWorkStealingPool();
    }

    // 使用平台线程执行器创建虚拟线程
    public static Thread startVirtualThread(Runnable task, ExecutorService executor) {
        Thread virtualThread = Thread.ofVirtual()
                .name("virtual-thread-")
                .unstarted(() -> {
                    try (var ignored = executor) {
                        task.run();
                    }
                });
        virtualThread.start();
        return virtualThread;
    }

    // 创建虚拟线程执行器服务
    public static ExecutorService createVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    // 提交任务到虚拟线程执行器并获取 Future
    public static <T> Future<T> submitTask(ExecutorService executor, Supplier<T> task) {
        return executor.submit(task::get);
    }

    // 运行阻塞任务示例
    public static <T> CompletableFuture<T> runBlockingTask(Supplier<T> blockingTask, ExecutorService executor) {
        return CompletableFuture.supplyAsync(blockingTask, executor);
    }

    // 优雅关闭执行器
    public static void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // 模拟长时间运行的阻塞操作
    public static void simulateBlockingOperation(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
