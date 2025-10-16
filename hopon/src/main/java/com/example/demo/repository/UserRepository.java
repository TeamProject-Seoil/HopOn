package com.example.demo.repository;

import com.example.demo.entity.ApprovalStatus;
import com.example.demo.entity.Role;
import com.example.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByUserid(String userid);
    boolean existsByEmail(String email);

    Optional<UserEntity> findByUserid(String userid);
    Optional<UserEntity> findByUseridIgnoreCase(String userid);
    Optional<UserEntity> findByUsernameAndEmail(String username, String email);
    Optional<UserEntity> findByUseridAndEmail(String userid, String email);
    Optional<UserEntity> findByUseridAndEmailIgnoreCase(String userid, String email);
    List<UserEntity> findByRoleAndApprovalStatus(Role role, ApprovalStatus approvalStatus);

    // 권장: DB 콜레이션과 맞춰 대소문자 무시 버전으로 통일 사용
    boolean existsByUseridIgnoreCase(String userid);
    boolean existsByEmailIgnoreCase(String email);

    
    // userid + email 모두 대소문자 무시
    @Query("""
           select case when count(u) > 0 then true else false end
           from UserEntity u
           where lower(u.userid) = lower(:userid)
             and lower(u.email)  = lower(:email)
           """)
    boolean existsByUseridAndEmailIgnoreCaseBoth(@Param("userid") String userid,
                                                 @Param("email") String email);

    // 메서드명 기반 ignoreCase (파라미터 이름 정리)
    boolean existsByUseridIgnoreCaseAndEmailIgnoreCase(String userid, String email);

    // ===== 하드 삭제 (JPQL) =====
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserEntity u where u.userNum = :num")
    int hardDeleteByUserNum(@Param("num") Long num);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserEntity u where lower(u.userid) = lower(:userid)")
    int hardDeleteByUserid(@Param("userid") String userid);
}
