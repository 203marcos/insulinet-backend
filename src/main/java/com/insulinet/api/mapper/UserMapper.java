package com.insulinet.api.mapper;

import com.insulinet.api.model.dto.auth.UserResponse;
import com.insulinet.api.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
