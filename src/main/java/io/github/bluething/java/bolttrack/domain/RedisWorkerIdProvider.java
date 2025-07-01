package io.github.bluething.java.bolttrack.domain;

import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
class RedisWorkerIdProvider implements WorkerIdProvider, SmartLifecycle {
    private static final int MAX_WORKER_ID = 1023;
    private static final Duration LEASE_TTL = Duration.ofSeconds(60);
    private final StringRedisTemplate redis;
    private String workerKey;
    private int workerId;
    private ScheduledFuture<?> renewal;

    public RedisWorkerIdProvider(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void start() {
        String instanceId = UUID.randomUUID().toString();
        for (int i = 0; i <= MAX_WORKER_ID; i++) {
            String key = "tracking:worker:" + i;
            Boolean ok = redis.opsForValue()
                    .setIfAbsent(key, instanceId, LEASE_TTL);
            if (Boolean.TRUE.equals(ok)) {
                this.workerKey = key;
                this.workerId  = i;
                // schedule periodic TTL refresh so we don’t lose the lease
                this.renewal = Executors.newSingleThreadScheduledExecutor()
                        .scheduleAtFixedRate(() ->
                                        redis.expire(workerKey, LEASE_TTL),
                                LEASE_TTL.getSeconds() / 2,
                                LEASE_TTL.getSeconds() / 2,
                                TimeUnit.SECONDS);
                return;
            }
        }
        throw new IllegalStateException("no free worker ID");
    }

    @Override
    public int getWorkerId() {
        return workerId;
    }

    @Override
    public void stop() {
        if (renewal   != null) renewal.cancel(false);
        if (workerKey != null) redis.delete(workerKey);
    }
    @Override public boolean isRunning()    { return workerKey != null; }
    @Override public boolean isAutoStartup(){ return true; }
    @Override public int getPhase()        { return Integer.MAX_VALUE; }
    @Override public void stop(Runnable callback) {
        stop(); callback.run();
    }
}
