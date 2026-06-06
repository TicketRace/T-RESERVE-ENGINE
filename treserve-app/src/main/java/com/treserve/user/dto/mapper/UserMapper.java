package com.treserve.user.dto.mapper;

import com.treserve.user.dto.UserProfileResponse;
import com.treserve.user.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserProfileResponse toResponse(User entity);

    List<UserProfileResponse> toResponseList(List<User> entities);
}