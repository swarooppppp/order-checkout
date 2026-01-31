package com.example.ordermanagement.service;

import com.example.ordermanagement.entity.Coupon;
import com.example.ordermanagement.entity.CouponType;
import com.example.ordermanagement.exception.ResourceNotFoundException;
import com.example.ordermanagement.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon validPercentageCoupon;
    private Coupon validFixedCoupon;

    @BeforeEach
    void setUp() {
        // Valid PERCENTAGE coupon
        validPercentageCoupon = new Coupon();
        validPercentageCoupon.setId(1L);
        validPercentageCoupon.setCode("SAVE20PC");
        validPercentageCoupon.setType(CouponType.PERCENTAGE);
        validPercentageCoupon.setValue(new BigDecimal("20.00"));
        validPercentageCoupon.setMinOrderAmount(BigDecimal.ZERO);
        validPercentageCoupon.setMaxUses(100);
        validPercentageCoupon.setUsedCount(5);
        validPercentageCoupon.setValidFrom(LocalDateTime.now().minusDays(1));
        validPercentageCoupon.setValidUntil(LocalDateTime.now().plusDays(30));
        validPercentageCoupon.setActive(true);

        // Valid FIXED coupon
        validFixedCoupon = new Coupon();
        validFixedCoupon.setId(2L);
        validFixedCoupon.setCode("FLAT50OF");
        validFixedCoupon.setType(CouponType.FIXED);
        validFixedCoupon.setValue(new BigDecimal("50.00"));
        validFixedCoupon.setMinOrderAmount(new BigDecimal("100.00"));
        validFixedCoupon.setMaxUses(50);
        validFixedCoupon.setUsedCount(10);
        validFixedCoupon.setValidFrom(LocalDateTime.now().minusDays(1));
        validFixedCoupon.setValidUntil(LocalDateTime.now().plusDays(30));
        validFixedCoupon.setActive(true);
    }

    @Nested
    @DisplayName("Create Coupon Validation Tests")
    class CreateCouponValidationTests {

        @Test
        @DisplayName("Should throw exception when PERCENTAGE value exceeds 50%")
        void shouldThrowExceptionWhenPercentageExceeds50() {
            Coupon invalidCoupon = new Coupon();
            invalidCoupon.setType(CouponType.PERCENTAGE);
            invalidCoupon.setValue(new BigDecimal("60.00")); // Invalid: > 50%
            invalidCoupon.setMinOrderAmount(BigDecimal.ZERO);
            invalidCoupon.setMaxUses(100);
            invalidCoupon.setValidFrom(LocalDateTime.now());
            invalidCoupon.setValidUntil(LocalDateTime.now().plusDays(30));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> couponService.createCoupon(invalidCoupon)
            );

            assertTrue(exception.getMessage().contains("50%"));
        }

        @Test
        @DisplayName("Should allow PERCENTAGE value exactly at 50%")
        void shouldAllowPercentageExactly50() {
            Coupon coupon = new Coupon();
            coupon.setType(CouponType.PERCENTAGE);
            coupon.setValue(new BigDecimal("50.00"));
            coupon.setMinOrderAmount(BigDecimal.ZERO);
            coupon.setMaxUses(100);
            coupon.setValidFrom(LocalDateTime.now());
            coupon.setValidUntil(LocalDateTime.now().plusDays(30));

            when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

            assertDoesNotThrow(() -> couponService.createCoupon(coupon));
        }

        @Test
        @DisplayName("Should allow any value for FIXED type")
        void shouldAllowAnyValueForFixedType() {
            Coupon coupon = new Coupon();
            coupon.setType(CouponType.FIXED);
            coupon.setValue(new BigDecimal("100.00")); // Any value allowed for FIXED
            coupon.setMinOrderAmount(BigDecimal.ZERO);
            coupon.setMaxUses(100);
            coupon.setValidFrom(LocalDateTime.now());
            coupon.setValidUntil(LocalDateTime.now().plusDays(30));

            when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

            assertDoesNotThrow(() -> couponService.createCoupon(coupon));
        }

        @Test
        @DisplayName("Should throw exception when validUntil is before validFrom")
        void shouldThrowExceptionWhenValidUntilBeforeValidFrom() {
            Coupon invalidCoupon = new Coupon();
            invalidCoupon.setType(CouponType.PERCENTAGE);
            invalidCoupon.setValue(new BigDecimal("20.00"));
            invalidCoupon.setMinOrderAmount(BigDecimal.ZERO);
            invalidCoupon.setMaxUses(100);
            invalidCoupon.setValidFrom(LocalDateTime.now().plusDays(30)); // Later
            invalidCoupon.setValidUntil(LocalDateTime.now()); // Earlier - Invalid!

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> couponService.createCoupon(invalidCoupon)
            );

            assertTrue(exception.getMessage().contains("Valid until date"));
        }
    }

    @Nested
    @DisplayName("Get Coupon Tests")
    class GetCouponTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when ID not found")
        void shouldThrowExceptionWhenIdNotFound() {
            when(couponRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> couponService.getCouponById(999L));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when code not found")
        void shouldThrowExceptionWhenCodeNotFound() {
            when(couponRepository.findByCode("INVALID")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> couponService.getCouponByCode("INVALID"));
        }
    }

    @Nested
    @DisplayName("Calculate Discount - PERCENTAGE Type Tests")
    class CalculateDiscountPercentageTests {

        @Test
        @DisplayName("Should calculate 20% discount correctly")
        void shouldCalculate20PercentDiscount() {
            when(couponRepository.findByCode("SAVE20PC")).thenReturn(Optional.of(validPercentageCoupon));

            BigDecimal discount = couponService.calculateDiscount("SAVE20PC", new BigDecimal("100.00"));

            assertEquals(new BigDecimal("20.00"), discount);
        }

        @Test
        @DisplayName("Should round percentage discount to 2 decimal places")
        void shouldRoundPercentageDiscount() {
            when(couponRepository.findByCode("SAVE20PC")).thenReturn(Optional.of(validPercentageCoupon));

            // 20% of 33.33 = 6.666 -> should round to 6.67
            BigDecimal discount = couponService.calculateDiscount("SAVE20PC", new BigDecimal("33.33"));

            assertEquals(new BigDecimal("6.67"), discount);
        }

        @Test
        @DisplayName("Should calculate discount for very small order amount")
        void shouldCalculateDiscountForSmallAmount() {
            when(couponRepository.findByCode("SAVE20PC")).thenReturn(Optional.of(validPercentageCoupon));

            BigDecimal discount = couponService.calculateDiscount("SAVE20PC", new BigDecimal("1.00"));

            assertEquals(new BigDecimal("0.20"), discount);
        }

        @Test
        @DisplayName("Should calculate discount for very large order amount")
        void shouldCalculateDiscountForLargeAmount() {
            when(couponRepository.findByCode("SAVE20PC")).thenReturn(Optional.of(validPercentageCoupon));

            BigDecimal discount = couponService.calculateDiscount("SAVE20PC", new BigDecimal("10000.00"));

            assertEquals(new BigDecimal("2000.00"), discount);
        }
    }

    @Nested
    @DisplayName("Calculate Discount - FIXED Type Tests")
    class CalculateDiscountFixedTests {

        @Test
        @DisplayName("Should return full discount when order exceeds minimum")
        void shouldReturnFullDiscountWhenOrderExceedsMinimum() {
            when(couponRepository.findByCode("FLAT50OF")).thenReturn(Optional.of(validFixedCoupon));

            BigDecimal discount = couponService.calculateDiscount("FLAT50OF", new BigDecimal("200.00"));

            assertEquals(new BigDecimal("50.00"), discount);
        }

        @Test
        @DisplayName("Should throw exception when order is below minimum for FIXED coupon")
        void shouldThrowExceptionWhenOrderBelowMinimum() {
            when(couponRepository.findByCode("FLAT50OF")).thenReturn(Optional.of(validFixedCoupon));

            // Order amount 50 is below minimum 100
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> couponService.calculateDiscount("FLAT50OF", new BigDecimal("50.00"))
            );

            assertTrue(exception.getMessage().contains("minimum requirement"));
        }

        @Test
        @DisplayName("Should cap discount to order amount when FIXED discount exceeds order")
        void shouldCapDiscountToOrderAmount() {
            // Coupon value is 50, but order is only 30
            validFixedCoupon.setMinOrderAmount(BigDecimal.ZERO); // No minimum for this test
            when(couponRepository.findByCode("FLAT50OF")).thenReturn(Optional.of(validFixedCoupon));

            BigDecimal discount = couponService.calculateDiscount("FLAT50OF", new BigDecimal("30.00"));

            // Discount should be capped to 30 (the order amount)
            assertEquals(new BigDecimal("30.00"), discount);
        }

        @Test
        @DisplayName("Should allow order exactly at minimum amount")
        void shouldAllowOrderExactlyAtMinimum() {
            when(couponRepository.findByCode("FLAT50OF")).thenReturn(Optional.of(validFixedCoupon));

            // Order amount exactly equals minimum (100)
            BigDecimal discount = couponService.calculateDiscount("FLAT50OF", new BigDecimal("100.00"));

            assertEquals(new BigDecimal("50.00"), discount);
        }
    }

    @Nested
    @DisplayName("Coupon Validity Tests")
    class CouponValidityTests {

        @Test
        @DisplayName("Should throw exception for expired coupon")
        void shouldThrowExceptionForExpiredCoupon() {
            Coupon expiredCoupon = new Coupon();
            expiredCoupon.setCode("EXPIRED");
            expiredCoupon.setType(CouponType.PERCENTAGE);
            expiredCoupon.setValue(new BigDecimal("20.00"));
            expiredCoupon.setMinOrderAmount(BigDecimal.ZERO);
            expiredCoupon.setMaxUses(100);
            expiredCoupon.setUsedCount(5);
            expiredCoupon.setValidFrom(LocalDateTime.now().minusDays(30));
            expiredCoupon.setValidUntil(LocalDateTime.now().minusDays(1)); // Expired yesterday
            expiredCoupon.setActive(true);

            when(couponRepository.findByCode("EXPIRED")).thenReturn(Optional.of(expiredCoupon));

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> couponService.calculateDiscount("EXPIRED", new BigDecimal("100.00"))
            );

            assertTrue(exception.getMessage().contains("not valid"));
        }

        @Test
        @DisplayName("Should throw exception for inactive coupon")
        void shouldThrowExceptionForInactiveCoupon() {
            Coupon inactiveCoupon = new Coupon();
            inactiveCoupon.setCode("INACTIVE");
            inactiveCoupon.setType(CouponType.PERCENTAGE);
            inactiveCoupon.setValue(new BigDecimal("20.00"));
            inactiveCoupon.setMinOrderAmount(BigDecimal.ZERO);
            inactiveCoupon.setMaxUses(100);
            inactiveCoupon.setUsedCount(5);
            inactiveCoupon.setValidFrom(LocalDateTime.now().minusDays(1));
            inactiveCoupon.setValidUntil(LocalDateTime.now().plusDays(30));
            inactiveCoupon.setActive(false); // Inactive!

            when(couponRepository.findByCode("INACTIVE")).thenReturn(Optional.of(inactiveCoupon));

            assertThrows(IllegalStateException.class,
                    () -> couponService.calculateDiscount("INACTIVE", new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("Should throw exception for coupon not yet valid")
        void shouldThrowExceptionForFutureCoupon() {
            Coupon futureCoupon = new Coupon();
            futureCoupon.setCode("FUTURE");
            futureCoupon.setType(CouponType.PERCENTAGE);
            futureCoupon.setValue(new BigDecimal("20.00"));
            futureCoupon.setMinOrderAmount(BigDecimal.ZERO);
            futureCoupon.setMaxUses(100);
            futureCoupon.setUsedCount(0);
            futureCoupon.setValidFrom(LocalDateTime.now().plusDays(1)); // Starts tomorrow
            futureCoupon.setValidUntil(LocalDateTime.now().plusDays(30));
            futureCoupon.setActive(true);

            when(couponRepository.findByCode("FUTURE")).thenReturn(Optional.of(futureCoupon));

            assertThrows(IllegalStateException.class,
                    () -> couponService.calculateDiscount("FUTURE", new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("Should throw exception when coupon reached max uses")
        void shouldThrowExceptionWhenMaxUsesReached() {
            Coupon exhaustedCoupon = new Coupon();
            exhaustedCoupon.setCode("MAXED");
            exhaustedCoupon.setType(CouponType.PERCENTAGE);
            exhaustedCoupon.setValue(new BigDecimal("20.00"));
            exhaustedCoupon.setMinOrderAmount(BigDecimal.ZERO);
            exhaustedCoupon.setMaxUses(100);
            exhaustedCoupon.setUsedCount(100); // Max reached!
            exhaustedCoupon.setValidFrom(LocalDateTime.now().minusDays(1));
            exhaustedCoupon.setValidUntil(LocalDateTime.now().plusDays(30));
            exhaustedCoupon.setActive(true);

            when(couponRepository.findByCode("MAXED")).thenReturn(Optional.of(exhaustedCoupon));

            assertThrows(IllegalStateException.class,
                    () -> couponService.calculateDiscount("MAXED", new BigDecimal("100.00")));
        }
    }

    @Nested
    @DisplayName("Increment Used Count Tests")
    class IncrementUsedCountTests {

        @Test
        @DisplayName("Should increment used count successfully")
        void shouldIncrementUsedCountSuccessfully() {
            validPercentageCoupon.setUsedCount(5);
            when(couponRepository.findById(1L)).thenReturn(Optional.of(validPercentageCoupon));
            when(couponRepository.save(any(Coupon.class))).thenReturn(validPercentageCoupon);

            Coupon result = couponService.incrementUsedCount(1L);

            assertEquals(6, validPercentageCoupon.getUsedCount());
        }

        @Test
        @DisplayName("Should throw exception when trying to exceed max uses")
        void shouldThrowExceptionWhenExceedingMaxUses() {
            validPercentageCoupon.setUsedCount(100); // Already at max
            validPercentageCoupon.setMaxUses(100);
            when(couponRepository.findById(1L)).thenReturn(Optional.of(validPercentageCoupon));

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> couponService.incrementUsedCount(1L)
            );

            assertTrue(exception.getMessage().contains("maximum uses"));
        }

        @Test
        @DisplayName("Should allow increment when at max-1 uses")
        void shouldAllowIncrementAtMaxMinusOne() {
            validPercentageCoupon.setUsedCount(99); // One below max
            validPercentageCoupon.setMaxUses(100);
            when(couponRepository.findById(1L)).thenReturn(Optional.of(validPercentageCoupon));
            when(couponRepository.save(any(Coupon.class))).thenReturn(validPercentageCoupon);

            Coupon result = couponService.incrementUsedCount(1L);

            assertEquals(100, validPercentageCoupon.getUsedCount());
        }
    }

    @Nested
    @DisplayName("Deactivate Coupon Tests")
    class DeactivateCouponTests {

        @Test
        @DisplayName("Should deactivate active coupon")
        void shouldDeactivateActiveCoupon() {
            validPercentageCoupon.setActive(true);
            when(couponRepository.findById(1L)).thenReturn(Optional.of(validPercentageCoupon));
            when(couponRepository.save(any(Coupon.class))).thenReturn(validPercentageCoupon);

            Coupon result = couponService.deactivateCoupon(1L);

            assertFalse(validPercentageCoupon.getActive());
        }

        @Test
        @DisplayName("Should throw exception when deactivating non-existent coupon")
        void shouldThrowExceptionWhenDeactivatingNonExistentCoupon() {
            when(couponRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> couponService.deactivateCoupon(999L));
        }
    }
}
