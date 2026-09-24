package com.desenvolvimento.logica.cashpilot_api.user.service;

import com.desenvolvimento.logica.cashpilot_api.shared.exception.UserNotFoundException;
import com.desenvolvimento.logica.cashpilot_api.shared.mapper.AddressMapper;
import com.desenvolvimento.logica.cashpilot_api.user.dto.UpdateUserProfileRequest;
import com.desenvolvimento.logica.cashpilot_api.user.entity.User;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void updateProfile(
            @NotNull UUID authenticatedUserId,
            @NotNull @Valid UpdateUserProfileRequest request) {

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(UserNotFoundException::new);

        user.updateProfile(
                request.name(),
                request.document(),
                request.phone(),
                AddressMapper.toEntity(request.address())
        );
    }
}
