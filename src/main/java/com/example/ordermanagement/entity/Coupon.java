package com.example.ordermanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @NotNull(message = "Coupon type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType type;

    @NotNull(message = "Value is required")
    @Positive(message = "Value must be positive")
    @Column(name = "discount_value", nullable = false)
    private BigDecimal value;

    @NotNull(message = "Minimum order amount is required")
    @PositiveOrZero(message = "Minimum order amount cannot be negative")
    @Column(name = "min_order_amount", nullable = false)
    private BigDecimal minOrderAmount;

    @NotNull(message = "Max uses is required")
    @Positive(message = "Max uses must be positive")
    @Column(name = "max_uses", nullable = false)
    private Integer maxUses;

    @PositiveOrZero(message = "Used count cannot be negative")
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @NotNull(message = "Valid from date is required")
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @NotNull(message = "Valid until date is required")
    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(nullable = false)
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        if (code == null || code.isEmpty()) {
            code = generateRandomCode();
        }
        validatePercentageValue();
    }

    @PreUpdate
    protected void onUpdate() {
        validatePercentageValue();
    }

    private void validatePercentageValue() {
        if (type == CouponType.PERCENTAGE && value != null) {
            if (value.compareTo(BigDecimal.valueOf(50)) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot exceed 50%");
            }
        }
    }

    private String generateRandomCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active
                && usedCount < maxUses
                && now.isAfter(validFrom)
                && now.isBefore(validUntil);
    }

    public boolean canApplyToOrder(BigDecimal orderAmount) {
        if (!isValid()) {
            return false;
        }
        // Only check minOrderAmount for FIXED type coupons
        if (type == CouponType.FIXED) {
            return orderAmount.compareTo(minOrderAmount) >= 0;
        }
        return true;
    }
}
