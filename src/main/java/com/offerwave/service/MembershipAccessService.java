package com.offerwave.service;

import com.offerwave.entity.User;
import com.offerwave.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MembershipAccessService {

    @Autowired
    private UserMapper userMapper;

    public User ensureMembershipActive(User user) {
        if (user == null) {
            return null;
        }

        Integer membershipId = user.getMembershipId();
        if (membershipId == null || membershipId <= 1) {
            return user;
        }

        LocalDateTime expireAt = user.getMembershipExpireAt();
        if (expireAt == null || expireAt.isAfter(LocalDateTime.now())) {
            return user;
        }

        User patch = new User();
        patch.setId(user.getId());
        patch.setMembershipId(1);
        patch.setMembershipExpireAt(null);
        userMapper.updateById(patch);

        user.setMembershipId(1);
        user.setMembershipExpireAt(null);
        return user;
    }

    public boolean isVip(User user) {
        User effectiveUser = ensureMembershipActive(user);
        return effectiveUser != null
                && effectiveUser.getMembershipId() != null
                && effectiveUser.getMembershipId() > 1;
    }
}
