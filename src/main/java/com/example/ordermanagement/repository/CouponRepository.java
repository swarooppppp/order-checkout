package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.Coupon;
import com.example.ordermanagement.entity.CouponType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    List<Coupon> findByActive(Boolean active);

    List<Coupon> findByType(CouponType type);

    @Query("SELECT c FROM Coupon c WHERE c.active = true AND c.usedCount < c.maxUses " +
           "AND c.validFrom <= :now AND c.validUntil >= :now")
    List<Coupon> findAllValidCoupons(@Param("now") LocalDateTime now);

    boolean existsByCode(String code);
}
