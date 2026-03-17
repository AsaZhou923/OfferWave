package com.offernow.service;

import com.offernow.entity.Membership;
import com.offernow.entity.User;
import com.offernow.mapper.MembershipMapper;
import com.offernow.mapper.UserJobStatusMapper;
import com.offernow.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private MembershipMapper membershipMapper;

    @Mock
    private UserJobStatusMapper userJobStatusMapper;

    @Mock
    private MembershipAccessService membershipAccessService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldReturnEmailInProfileResponse() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setNickname("Test User");
        user.setRole(0);
        user.setMembershipId(2);
        user.setPrefIndustry("互联网,金融");
        user.setPrefCity("北京");
        user.setPrefJob("后端开发");

        Membership membership = new Membership();
        membership.setId(2);
        membership.setLevelName("VIP会员");
        membership.setPrivileges("{\"max_job_track\":999}");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(membershipAccessService.ensureMembershipActive(user)).thenReturn(user);
        when(membershipMapper.selectById(2)).thenReturn(membership);
        when(userJobStatusMapper.selectCount(any())).thenReturn(8L, 3L);

        Map<String, Object> response = userService.getUserInfo(1L);

        assertEquals("user@example.com", response.get("email"));
        assertEquals("Test User", response.get("nickname"));
        assertEquals(false, response.get("is_admin"));

        Object preferences = response.get("preferences");
        assertInstanceOf(Map.class, preferences);
        Map<?, ?> preferenceMap = (Map<?, ?>) preferences;
        assertEquals("北京", preferenceMap.get("city"));

        Object stats = response.get("stats");
        assertInstanceOf(Map.class, stats);
        Map<?, ?> statMap = (Map<?, ?>) stats;
        assertEquals(8L, statMap.get("tracked_count"));
        assertEquals(3L, statMap.get("delivered_count"));

        Object membershipInfo = response.get("membership");
        assertInstanceOf(Map.class, membershipInfo);
        Map<?, ?> membershipMap = (Map<?, ?>) membershipInfo;
        assertEquals("VIP会员", membershipMap.get("level_name"));
        assertTrue(membershipMap.containsKey("privileges"));
    }
}
