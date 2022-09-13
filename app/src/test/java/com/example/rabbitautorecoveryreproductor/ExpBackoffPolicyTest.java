package com.example.rabbitautorecoveryreproductor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExpBackoffPolicyTest {

    @Test
    void testBackoff() {

        ExpBackoffPolicy b = new ExpBackoffPolicy(2000, 2);
        Assertions.assertEquals(2000L, b.getSleepTimeMs(1));
        Assertions.assertEquals(4000L, b.getSleepTimeMs(2));
        Assertions.assertEquals(8000L, b.getSleepTimeMs(3));
    }

}