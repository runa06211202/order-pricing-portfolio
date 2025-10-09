package com.example.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderResult(
    BigDecimal totalNetBeforeDiscount,
    BigDecimal totalDiscount,
    BigDecimal totalNetAfterDiscount,
    BigDecimal totalGross,
    List<String> appliedDiscounts
) {}
