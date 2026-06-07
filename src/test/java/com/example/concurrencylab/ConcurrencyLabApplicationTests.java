package com.example.concurrencylab;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local-h2")
class ConcurrencyLabApplicationTests {

    @Test
    void contextLoads() {
    }
}
