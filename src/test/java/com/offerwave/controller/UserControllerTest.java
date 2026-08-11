package com.offerwave.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    @Test
    void selfServiceMembershipUpgradeEndpointShouldReturnNotFound() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController()).build();

        mockMvc.perform(post("/api/v1/user/membership/upgrade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetLevelId\":2}"))
                .andExpect(status().isNotFound());
    }
}
