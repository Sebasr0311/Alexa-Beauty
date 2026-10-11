package com.alexabeauty;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AlexaBeautyApplicationTests {

    @Test
    @DisplayName("Context loads successfully with test profile")
    void contextLoads() {
        // Verifies Spring application context boots without error
    }
}
