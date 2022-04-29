/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.OpenSearchExecutors;
import org.opensearch.node.Node;
import org.opensearch.threadpool.ExecutorBuilder;
import org.opensearch.threadpool.ThreadPool;

public class TestThreadPool extends ThreadPool {
    private final CountDownLatch blockingLatch;
    private volatile boolean returnRejectingExecutor;
    private volatile ThreadPoolExecutor rejectingExecutor;

    public TestThreadPool(String name, ExecutorBuilder<?>... customBuilders) {
        this(name, Settings.EMPTY, customBuilders);
    }

    public TestThreadPool(String name, Settings settings, ExecutorBuilder<?>... customBuilders) {
        super(Settings.builder().put(Node.NODE_NAME_SETTING.getKey(), name).put(settings).build(), customBuilders);
        this.blockingLatch = new CountDownLatch(1);
        this.returnRejectingExecutor = false;
    }

    public ExecutorService executor(String name) {
        return (ExecutorService) (this.returnRejectingExecutor ? this.rejectingExecutor : super.executor(name));
    }

    public void startForcingRejections() {
        if (this.rejectingExecutor == null) {
            this.createRejectingExecutor();
        }

        this.returnRejectingExecutor = true;
    }

    public void stopForcingRejections() {
        this.returnRejectingExecutor = false;
    }

    public void shutdown() {
        this.blockingLatch.countDown();
        if (this.rejectingExecutor != null) {
            this.rejectingExecutor.shutdown();
        }

        super.shutdown();
    }

    public void shutdownNow() {
        this.blockingLatch.countDown();
        if (this.rejectingExecutor != null) {
            this.rejectingExecutor.shutdownNow();
        }

        super.shutdownNow();
    }

    private synchronized void createRejectingExecutor() {
        if (this.rejectingExecutor == null) {
            ThreadFactory factory = OpenSearchExecutors.daemonThreadFactory("reject_thread");
            this.rejectingExecutor = OpenSearchExecutors.newFixed("rejecting", 1, 0, factory, this.getThreadContext());
            CountDownLatch startedLatch = new CountDownLatch(1);
            this.rejectingExecutor.execute(() -> {
                try {
                    startedLatch.countDown();
                    this.blockingLatch.await();
                } catch (InterruptedException var3) {
                    throw new RuntimeException(var3);
                }
            });

            try {
                startedLatch.await();
            } catch (InterruptedException var4) {
                throw new RuntimeException(var4);
            }
        }
    }
}
