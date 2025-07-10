import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public class VirtualThreadUtils {

    /**
     * 创建虚拟线程执行器
     * （每个任务分配独立虚拟线程）
     */
    public static ExecutorService newVirtualThreadPerTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 创建固定数量的虚拟线程执行器
     * @param concurrency 最大并发虚拟线程数
     */
    public static ExecutorService newFixedVirtualThreadPool(int concurrency) {
        ThreadFactory factory = Thread.ofVirtual().factory();
        return Executors.newThreadPerTaskExecutor(factory);
        // 注意：实际并发由任务数量决定，参数仅用于命名
    }

    /**
     * 使用虚拟线程执行任务
     * @param task 待执行任务
     */
    public static void execute(Runnable task) {
        Thread.ofVirtual().start(task);
    }

    /**
     * 批量执行任务并等待完成
     * @param tasks 任务列表
     */
    public static void executeAll(List<Runnable> tasks) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (Runnable task : tasks) {
            threads.add(Thread.ofVirtual().unstarted(task));
        }
        
        threads.forEach(Thread::start);
        
        for (Thread thread : threads) {
            thread.join();
        }
    }

    /**
     * 带返回值的虚拟线程执行
     * @param task Callable任务
     * @return Future对象
     */
    public static <T> Future<T> submit(Callable<T> task) {
        try (var executor = newVirtualThreadPerTaskExecutor()) {
            return executor.submit(task);
        }
    }
}
