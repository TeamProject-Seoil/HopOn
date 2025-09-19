package com.example.demo.repository;

import com.example.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByUserid(String userid);
    boolean existsByEmail(String email);
    Optional<UserEntity> findByUserid(String userid);
    Optional<UserEntity> findByUsernameAndTelAndEmail(String username, String tel, String email);
    Optional<UserEntity> findByUseridAndUsernameAndTelAndEmail(String userid, String username, String tel, String email);
}
