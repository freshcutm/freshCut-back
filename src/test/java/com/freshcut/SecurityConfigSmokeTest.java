package com.freshcut;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SecurityConfigSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void securityFilterChainBeanExists() {
        SecurityFilterChain chain = applicationContext.getBean(SecurityFilterChain.class);
        assertNotNull(chain, "SecurityFilterChain bean should be present");
    }
}