package com.phatdotdev.user_service.repository;

import com.phatdotdev.user_service.enity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
