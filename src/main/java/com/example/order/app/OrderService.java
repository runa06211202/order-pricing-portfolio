package com.example.order.app;

import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderResult;
import com.example.order.port.*;

public class OrderService {
  private final ProductRepository products;
  private final InventoryService inventory;
  private final TaxCalculator tax;

  public OrderService(ProductRepository products, InventoryService inventory, TaxCalculator tax) {
    this.products = products;
    this.inventory = inventory;
    this.tax = tax;
  }

  public OrderResult place(OrderRequest req) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
