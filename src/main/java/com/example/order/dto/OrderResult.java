package com.example.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderResult(
    BigDecimal totalNetBeforeDiscount,
    BigDecimal totalDiscount,
    BigDecimal totalTax,
    BigDecimal totalGross,
    List<DiscountType> appliedDiscounts // ADR-004
) {}
