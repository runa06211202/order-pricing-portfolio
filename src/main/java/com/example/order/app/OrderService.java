package com.example.order.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.example.order.domain.Product;
import com.example.order.dto.DiscountType;
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
	  
	  BigDecimal subtotalBase = BigDecimal.ZERO;
	  for(Line line : req.lines()) {
		  // Optional<Product>をここでunwrap
		  Product product = products.findById(line.productId())
				  .orElseThrow(() -> new IllegalArgumentException( "product not found: " + line.productId()));

		  BigDecimal lineSubtotal = product.unitPrice()
				  .multiply(BigDecimal.valueOf(line.qty()))
				  .setScale(2, RoundingMode.HALF_UP);

		  subtotalBase = subtotalBase.add(lineSubtotal);
	  }

	  BigDecimal totalNetBeforeDiscount = BigDecimal.ZERO;
	  BigDecimal totalDiscount = BigDecimal.ZERO;
	  BigDecimal totalTax = BigDecimal.ZERO;
	  BigDecimal totalGross = BigDecimal.ZERO;
	  List<DiscountType> appliedDiscounts = new ArrayList<DiscountType>();

	  totalNetBeforeDiscount = subtotalBase;
	  
	  //税計算(仮)
	  totalTax = tax.calcTaxAmount(totalNetBeforeDiscount, "JP", RoundingMode.HALF_UP);
	  totalGross = tax.addTax(totalNetBeforeDiscount, "JP", RoundingMode.HALF_UP);
	  
	  OrderResult orderResult = new OrderResult(totalNetBeforeDiscount, totalDiscount, totalTax, totalGross, appliedDiscounts);

	  return orderResult;
  }
}
