package com.epipoli.starter.security;

import com.epipoli.commons.annotation.HWAttribute;
import com.epipoli.commons.annotation.HWEntity;
import com.epipoli.commons.interfaces.IEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@HWEntity("api_user")
public class ApiUser implements IEntity<Long> {

    @HWAttribute
    private Long id;

    @HWAttribute
    private String accessId;

    @HWAttribute
    private String role;

    @HWAttribute
    private String secret;

}