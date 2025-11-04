package com.freshcut;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityUnauthorizedTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminSchedulesWithoutAuthReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/schedules"))
                .andExpect(status().isForbidden());
    }
}