package com.minecraft.launcher.download;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 低频快照流发布器：保留最新值，新订阅者立即收到当前快照（SubmissionPublisher 无重放）。
 * 面向 UI 消费（订阅即请求无界），非高吞吐场景。
 */
public final class SnapshotPublisher<T> implements Flow.Publisher<T> {

    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();
    private volatile T latest;

    /** 发布新快照并缓存为最新值。 */
    public void publish(T value) {
        latest = value;
        for (Subscription s : subscriptions) {
            s.deliver(value);
        }
    }

    public T latest() {
        return latest;
    }

    public void close() {
        for (Subscription s : subscriptions) {
            s.subscriber.onComplete();
        }
        subscriptions.clear();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        Subscription s = new Subscription(subscriber);
        subscriptions.add(s);
        subscriber.onSubscribe(s);
        T current = latest;
        if (current != null) {
            s.deliver(current);
        }
    }

    private final class Subscription implements Flow.Subscription {
        private final Flow.Subscriber<? super T> subscriber;
        private final AtomicLong requested = new AtomicLong();
        private final AtomicBoolean cancelled = new AtomicBoolean();

        Subscription(Flow.Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                for (; ; ) {
                    long cur = requested.get();
                    long next = cur == Long.MAX_VALUE - n ? Long.MAX_VALUE : Math.min(Long.MAX_VALUE, cur + n);
                    if (requested.compareAndSet(cur, next)) {
                        return;
                    }
                }
            }
        }

        @Override
        public void cancel() {
            cancelled.set(true);
            subscriptions.remove(this);
        }

        void deliver(T value) {
            if (cancelled.get()) {
                return;
            }
            if (requested.get() > 0) {
                requested.decrementAndGet();
            }
            // 低频快照流：即使请求配额耗尽也投递最新值（消费端 trySend 缓冲，丢中间值可接受）
            subscriber.onNext(value);
        }
    }
}
