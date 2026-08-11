package com.offerwave.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerwave.common.R;
import com.offerwave.dto.AdminUserResponseDto;
import com.offerwave.entity.User;
import com.offerwave.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController();
        ReflectionTestUtils.setField(controller, "adminService", adminService);
    }

    @Test
    void userListShouldUseSafeResponseDto() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin-visible");
        user.setEmail("user@example.com");
        user.setPasswordHash("$2a$10$must-never-leak");
        Page<User> users = new Page<>(1, 20, 1);
        users.setRecords(List.of(user));
        when(adminService.listUsers(any(Page.class), isNull(), isNull())).thenReturn(users);

        R<Page<AdminUserResponseDto>> response = controller.listUsers(1, 20, null, null);

        assertEquals(AdminUserResponseDto.class, response.getData().getRecords().get(0).getClass());
        String json = new ObjectMapper().writeValueAsString(response);
        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("must-never-leak"));
    }

    @Test
    void userListShouldRejectUnboundedPageSize() {
        R<Page<AdminUserResponseDto>> response = controller.listUsers(1, 1000, null, null);

        assertEquals(400, response.getCode());
        verify(adminService, never()).listUsers(any(), any(), any());
    }

    @Test
    void importFailureShouldNotExposeParserDetails() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "jobs.txt",
                "text/plain",
                "untrusted-content".getBytes(StandardCharsets.UTF_8));

        R<?> response = controller.importJobs(file);

        assertEquals(400, response.getCode());
        assertEquals("导入文件格式或内容不符合要求", response.getMessage());
        assertFalse(response.getMessage().contains("jobs.txt"));
    }
}
