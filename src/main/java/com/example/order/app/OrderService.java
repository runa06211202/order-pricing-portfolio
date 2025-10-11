package com.example.order.app;

import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderRequest.Line;
import com.example.order.dto.OrderResult;
import com.example.order.port.InventoryService;
import com.example.order.port.ProductRepository;
import com.example.order.port.TaxCalculator;

public class OrderService {
  private final ProductRepository products;
  private final InventoryService inventory;
  private final TaxCalculator tax;

  public OrderService(ProductRepository products, InventoryService inventory, TaxCalculator tax) {
    this.products = products;
    this.inventory = inventory;
    this.tax = tax;
  }

  public OrderResult placeOrder(OrderRequest req) {
	  // 引数チェック
	  if(req == null || req.lines() == null || req.lines().isEmpty()) {
		  throw new IllegalArgumentException("lines must not be null or empty");
	  }
	  for(Line line : req.lines()) {
		  if(line.qty() <= 0) {
			  throw new IllegalArgumentException("qty must not be zero or minus");
		  }
	  }
    throw new UnsupportedOperationException("not implemented yet");
  }
}
