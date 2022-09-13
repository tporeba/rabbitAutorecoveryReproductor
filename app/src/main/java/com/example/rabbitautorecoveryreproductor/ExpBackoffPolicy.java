package com.example.rabbitautorecoveryreproductor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rabbitmq.client.impl.recovery.BackoffPolicy;

import io.github.resilience4j.core.IntervalFunction;

public class ExpBackoffPolicy implements BackoffPolicy {

    private static final Logger log = LogManager.getLogger();

    private final long initialIntervalMs;
    private final int multiplier;

    ExpBackoffPolicy(long initialDelayMs, int multiplier) {
        this.initialIntervalMs = initialDelayMs;
        this.multiplier = multiplier;
    }

    @Override
    public void backoff(int attemptNumber) throws InterruptedException {
        long sleepTimeMs = getSleepTimeMs(attemptNumber);

        log.debug("Backoff attempt {}, will sleep {} ms", attemptNumber, sleepTimeMs);
        Thread.sleep((long) sleepTimeMs);
    }

    public long getSleepTimeMs(int attemptNumber) {
        IntervalFunction intervalFunction = IntervalFunction.ofExponentialBackoff(initialIntervalMs, multiplier);
        long sleepTimeMs = intervalFunction.apply(attemptNumber);

        // logX(Y) = log10(Y) / log10(X), so basically we calculate here log with base=multiplier of Long.MAX_VALUE
        //        double maxAttemptNr = (Math.log10(Long.MAX_VALUE) / Math.log10(multiplier)) - 1;
        //        int effectiveAttemptNr = attemptNumber;
        //        if (attemptNumber > maxAttemptNr) {
        //            effectiveAttemptNr = (int) Math.floor(maxAttemptNr);
        //        }
        //        long sleepTimeMs = (long) (initialIntervalMs * Math.pow(multiplier, effectiveAttemptNr-1));
        return sleepTimeMs;
    }
}
