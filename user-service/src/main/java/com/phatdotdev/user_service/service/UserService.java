package com.phatdotdev.user_service.service;

import com.phatdotdev.user_service.enity.User;
import com.phatdotdev.user_service.repository.UserRepository;
import org.springframework.stereotype.Service;

import com.phatdotdev.user_service.dto.UserDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public UserDto createUser(UserDto input) {
        log.info("Creating user: {}", input);
        final User createdUser = User.builder()
                .name(input.getName())
                .surname(input.getSurname())
                .email(input.getEmail())
                .address(input.getAddress())
                .alerting(input.isAlerting())
                .energyAlertingThreshold(input.getEnergyAlertingThreshold())
                .build();
        final User savedUser = userRepository.save(createdUser);
        return toDto(savedUser);
    }

    public UserDto getUserById(Long id){
        return userRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    public void updateUser(Long id, UserDto input){
        final User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setName(input.getName());
        user.setSurname(input.getSurname());
        user.setEmail(input.getEmail());
        user.setAddress(input.getAddress());
        user.setAlerting(input.isAlerting());
        user.setEnergyAlertingThreshold(input.getEnergyAlertingThreshold());

        userRepository.save(user);
    }

    public void deleteUser(Long id){
        final User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.delete(user);
    }

    private UserDto toDto(User user){
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .email(user.getEmail())
                .address(user.getAddress())
                .alerting(user.isAlerting())
                .energyAlertingThreshold(user.getEnergyAlertingThreshold())
                .build();
    }
}