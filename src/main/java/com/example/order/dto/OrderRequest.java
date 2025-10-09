package com.example.order.dto;

import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public record OrderRequest(String region, RoundingMode mode, List<Line> lines) {
  public record Line(String productId, int qty) {}

  public OrderRequest {
    // nullはテストで検証するのでここでは厳格バリデーションはしない（Guardsテストの対象）
    // ただしNPEを避ける最低限の防御だけ
    Objects.requireNonNull(lines, "lines must not be null");
  }
}
