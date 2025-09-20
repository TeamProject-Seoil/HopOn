package com.example.demo.repository;

import com.example.demo.entity.ApprovalStatus;
import com.example.demo.entity.Role;
import com.example.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByUserid(String userid);
    boolean existsByEmail(String email);
    Optional<UserEntity> findByUserid(String userid);
    Optional<UserEntity> findByUsernameAndEmail(String username, String email);
    Optional<UserEntity> findByUseridAndEmail(String userid, String email);
    List<UserEntity> findByRoleAndApprovalStatus(Role role, ApprovalStatus approvalStatus);
}
