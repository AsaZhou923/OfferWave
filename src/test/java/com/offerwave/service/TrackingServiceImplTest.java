package com.offerwave.service;

import com.offerwave.common.NotFoundException;
import com.offerwave.common.PrivilegeException;
import com.offerwave.dto.UpdateJobStatusDto;
import com.offerwave.entity.Job;
import com.offerwave.entity.Membership;
import com.offerwave.entity.User;
import com.offerwave.entity.UserJobStatus;
import com.offerwave.mapper.JobMapper;
import com.offerwave.mapper.MembershipMapper;
import com.offerwave.mapper.UserJobStatusMapper;
import com.offerwave.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceImplTest {

    @Mock
    private UserJobStatusMapper userJobStatusMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MembershipMapper membershipMapper;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private ContentModerationService contentModerationService;

    @Mock
    private MembershipAccessService membershipAccessService;

    @InjectMocks
    private TrackingServiceImpl trackingService;

    @Test
    void shouldThrowWhenJobDoesNotExist() {
        UpdateJobStatusDto dto = new UpdateJobStatusDto();
        dto.setIsCollected(true);

        when(jobMapper.selectById(100L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> trackingService.updateJobStatus(1L, 100L, dto));
        verify(userJobStatusMapper, never()).insert(any(UserJobStatus.class));
    }

    @Test
    void shouldCheckPrivilegesWhenExistingStatusBecomesTracked() {
        UpdateJobStatusDto dto = new UpdateJobStatusDto();
        dto.setIsCollected(true);

        Job job = new Job();
        job.setId(2L);
        when(jobMapper.selectById(2L)).thenReturn(job);

        UserJobStatus existingStatus = new UserJobStatus();
        existingStatus.setUserId(1L);
        existingStatus.setJobId(2L);
        existingStatus.setIsCollected(false);
        existingStatus.setDeliveryStatus(0);
        when(userJobStatusMapper.selectOne(any())).thenReturn(existingStatus);

        User user = new User();
        user.setId(1L);
        user.setMembershipId(1);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(membershipAccessService.ensureMembershipActive(user)).thenReturn(user);

        Membership membership = new Membership();
        membership.setId(1);
        membership.setPrivileges("{\"max_job_track\":1}");
        when(membershipMapper.selectById(1)).thenReturn(membership);
        when(userJobStatusMapper.selectCount(any())).thenReturn(1L);

        assertThrows(PrivilegeException.class, () -> trackingService.updateJobStatus(1L, 2L, dto));
    }

    @Test
    void shouldSkipPrivilegeCheckWhenAlreadyTracked() {
        UpdateJobStatusDto dto = new UpdateJobStatusDto();
        dto.setUserNote("follow up");
        when(contentModerationService.sanitizeUserNote(1L, "follow up")).thenReturn("follow up");

        Job job = new Job();
        job.setId(3L);
        when(jobMapper.selectById(3L)).thenReturn(job);

        UserJobStatus existingStatus = new UserJobStatus();
        existingStatus.setUserId(1L);
        existingStatus.setJobId(3L);
        existingStatus.setIsCollected(true);
        existingStatus.setDeliveryStatus(0);
        when(userJobStatusMapper.selectOne(any())).thenReturn(existingStatus);

        assertDoesNotThrow(() -> trackingService.updateJobStatus(1L, 3L, dto));
        verify(userMapper, never()).selectById(1L);
        verify(userJobStatusMapper).updateById(existingStatus);
    }
}
