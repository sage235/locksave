// common/util/IdempotencyUtil.java

package com.LockSaveApplication.common.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IdempotencyUtil {

    /**
     * Generates a new idempotency key.
     * Call this when initiating a payment — store it with the transaction.
     */
    public String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * Builds a deterministic key from vault + amount + timestamp bucket.
     * Useful for deduplicating webhook callbacks from the same provider event.
     */
    public String buildWebhookKey(String providerReference, String eventType) {
        return String.format("webhook:%s:%s", eventType, providerReference);
    }
}
