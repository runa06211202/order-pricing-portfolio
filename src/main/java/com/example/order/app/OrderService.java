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
  private static final String LINES = "lines";
  private static final String QTY = "qty";
  private static final String REGION = "region";
  

  public OrderService(ProductRepository products, InventoryService inventory, TaxCalculator tax) {
    this.products = products;
    this.inventory = inventory;
    this.tax = tax;
  }

  public OrderResult placeOrder(OrderRequest req) {
	  // 引数チェック
	  if(req == null || req.lines() == null || req.lines().isEmpty()) {
		  throw new IllegalArgumentException( LINES + "must not be null or empty");
	  }
	  for(Line line : req.lines()) {
		  if(line.qty() <= 0) {
			  throw new IllegalArgumentException(QTY + "must not be zero or minus");
		  }
	  }
	  if(req.region() == null || req.region().isBlank()) {
		  throw new IllegalArgumentException(REGION + "must not be null or blank strings");
	  }
	  for(Line line : req.lines()) {
		  products.findById(line.productId()).orElseThrow(() -> new IllegalArgumentException( "product not found: " + line.productId()));
	  }
    throw new UnsupportedOperationException("not implemented yet");
  }
}
