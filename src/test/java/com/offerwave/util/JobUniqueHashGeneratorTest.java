package com.offerwave.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobUniqueHashGeneratorTest {

    @Test
    void shouldNormalizeCaseAndSurroundingWhitespace() {
        assertEquals(
                JobUniqueHashGenerator.generate("OfferWave", "Java工程师", "上海"),
                JobUniqueHashGenerator.generate(" offerwave ", "JAVA工程师 ", " 上海 "));
    }
}
