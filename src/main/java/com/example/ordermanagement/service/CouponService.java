package com.example.ordermanagement.service;

import com.example.ordermanagement.entity.Coupon;
import com.example.ordermanagement.entity.CouponType;
import com.example.ordermanagement.exception.ResourceNotFoundException;
import com.example.ordermanagement.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;

    public List<Coupon> getAllCoupons() {
        log.info("Fetching all coupons");
        List<Coupon> coupons = couponRepository.findAll();
        log.debug("Found {} coupons", coupons.size());
        return coupons;
    }

    public Coupon getCouponById(Long id) {
        log.info("Fetching coupon with id: {}", id);
        return couponRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Coupon not found with id: {}", id);
                    return new ResourceNotFoundException("Coupon not found with id: " + id);
                });
    }

    public Coupon getCouponByCode(String code) {
        log.info("Fetching coupon with code: {}", code);
        return couponRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.error("Coupon not found with code: {}", code);
                    return new ResourceNotFoundException("Coupon not found with code: " + code);
                });
    }

    public Coupon createCoupon(Coupon coupon) {
        log.info("Creating new coupon of type: {}", coupon.getType());
        validateCoupon(coupon);
        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("Coupon created successfully with id: {} and code: {}", savedCoupon.getId(), savedCoupon.getCode());
        return savedCoupon;
    }

    public Coupon updateCoupon(Long id, Coupon couponDetails) {
        log.info("Updating coupon with id: {}", id);
        Coupon coupon = getCouponById(id);
        validateCoupon(couponDetails);

        coupon.setType(couponDetails.getType());
        coupon.setValue(couponDetails.getValue());
        coupon.setMinOrderAmount(couponDetails.getMinOrderAmount());
        coupon.setMaxUses(couponDetails.getMaxUses());
        coupon.setValidFrom(couponDetails.getValidFrom());
        coupon.setValidUntil(couponDetails.getValidUntil());
        coupon.setActive(couponDetails.getActive());

        Coupon updatedCoupon = couponRepository.save(coupon);
        log.info("Coupon updated successfully with id: {}", updatedCoupon.getId());
        return updatedCoupon;
    }

    public void deleteCoupon(Long id) {
        log.info("Deleting coupon with id: {}", id);
        Coupon coupon = getCouponById(id);
        couponRepository.delete(coupon);
        log.info("Coupon deleted successfully with id: {}", id);
    }

    public Coupon deactivateCoupon(Long id) {
        log.info("Deactivating coupon with id: {}", id);
        Coupon coupon = getCouponById(id);
        coupon.setActive(false);
        Coupon deactivatedCoupon = couponRepository.save(coupon);
        log.info("Coupon deactivated successfully with id: {}", id);
        return deactivatedCoupon;
    }

    public List<Coupon> getActiveCoupons() {
        log.info("Fetching all active coupons");
        List<Coupon> coupons = couponRepository.findByActive(true);
        log.debug("Found {} active coupons", coupons.size());
        return coupons;
    }

    public List<Coupon> getValidCoupons() {
        log.info("Fetching all valid coupons");
        List<Coupon> coupons = couponRepository.findAllValidCoupons(LocalDateTime.now());
        log.debug("Found {} valid coupons", coupons.size());
        return coupons;
    }

    public Coupon incrementUsedCount(Long id) {
        log.info("Incrementing used count for coupon id: {}", id);
        Coupon coupon = getCouponById(id);
        if (coupon.getUsedCount() >= coupon.getMaxUses()) {
            log.warn("Coupon id: {} has reached maximum uses ({}/{})", id, coupon.getUsedCount(), coupon.getMaxUses());
            throw new IllegalStateException("Coupon has reached maximum uses");
        }
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        Coupon updatedCoupon = couponRepository.save(coupon);
        log.info("Coupon usage incremented to {}/{} for coupon id: {}", updatedCoupon.getUsedCount(), updatedCoupon.getMaxUses(), id);
        return updatedCoupon;
    }

    /**
     * Calculates the discount amount for a given coupon and order amount.
     * - FIXED: Validates minOrderAmount, returns coupon value as discount (capped at order amount)
     * - PERCENTAGE: No minOrderAmount validation, returns percentage of order amount as discount
     *
     * @param code        the coupon code
     * @param orderAmount the original order amount
     * @return the discount amount to subtract from the order
     */
    public BigDecimal calculateDiscount(String code, BigDecimal orderAmount) {
        log.info("Calculating discount for coupon code: {} with order amount: {}", code, orderAmount);
        Coupon coupon = getCouponByCode(code);

        if (!coupon.isValid()) {
            log.warn("Coupon code: {} is not valid", code);
            throw new IllegalStateException("Coupon is not valid");
        }

        BigDecimal discount;
        if (coupon.getType() == CouponType.FIXED) {
            // Validate minOrderAmount for FIXED type coupons
            if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
                log.warn("Order amount {} does not meet minimum requirement {} for coupon code: {}",
                        orderAmount, coupon.getMinOrderAmount(), code);
                throw new IllegalStateException("Order amount does not meet minimum requirement for this coupon");
            }
            // Return discount (capped so final amount doesn't go negative)
            discount = coupon.getValue();
            if (discount.compareTo(orderAmount) > 0) {
                discount = orderAmount;
                log.debug("FIXED discount capped to order amount: {}", discount);
            }
        } else {
            // PERCENTAGE type - no minOrderAmount validation
            discount = orderAmount.multiply(coupon.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        log.info("Calculated discount: {} for coupon code: {} (type: {})", discount, code, coupon.getType());
        return discount;
    }

    private void validateCoupon(Coupon coupon) {
        if (coupon.getType() == CouponType.PERCENTAGE
                && coupon.getValue().compareTo(BigDecimal.valueOf(50)) > 0) {
            log.error("Validation failed: Percentage discount {} exceeds maximum 50%", coupon.getValue());
            throw new IllegalArgumentException("Percentage discount cannot exceed 50%");
        }

        if (coupon.getValidUntil().isBefore(coupon.getValidFrom())) {
            log.error("Validation failed: validUntil {} is before validFrom {}", coupon.getValidUntil(), coupon.getValidFrom());
            throw new IllegalArgumentException("Valid until date must be after valid from date");
        }
        log.debug("Coupon validation passed");
    }
}
