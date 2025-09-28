package com.example.demo.repository;

import com.example.demo.entity.ApprovalStatus;
import com.example.demo.entity.Role;
import com.example.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByUserid(String userid);
    boolean existsByEmail(String email);
    Optional<UserEntity> findByUserid(String userid);
    Optional<UserEntity> findByUsernameAndEmail(String username, String email);
    Optional<UserEntity> findByUseridAndEmail(String userid, String email);
    Optional<UserEntity> findByUseridAndEmailIgnoreCase(String userid, String email);
    List<UserEntity> findByRoleAndApprovalStatus(Role role, ApprovalStatus approvalStatus);

    // ✅ 핵심: userid + email 둘 다 대소문자 무시 비교
    @Query("select case when count(u) > 0 then true else false end " +
           "from UserEntity u " +
           "where lower(u.userid) = lower(:userid) " +
           "and lower(u.email) = lower(:email)")
    boolean existsByUseridAndEmailIgnoreCaseBoth(@Param("userid") String userid,
                                                 @Param("email") String email);
	boolean existsByUseridIgnoreCaseAndEmailIgnoreCase(String trim, String trim2);

    Optional<UserEntity> findByUseridIgnoreCase(String userid);

    String userid(String userid);
}
