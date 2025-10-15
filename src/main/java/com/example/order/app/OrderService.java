package com.example.order.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.example.order.domain.model.Product;
import com.example.order.domain.policy.DiscountCapPolicy;
import com.example.order.domain.policy.PercentCapPolicy;
import com.example.order.dto.DiscountType;
import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderRequest.Line;
import com.example.order.dto.OrderResult;
import com.example.order.port.outbound.InventoryService;
import com.example.order.port.outbound.ProductRepository;
import com.example.order.port.outbound.TaxCalculator;



public class OrderService {
  private final ProductRepository products;
  private final InventoryService inventory;
  private final TaxCalculator tax;
  private final DiscountCapPolicy capPolicy;
  private static final String LINES = "lines";
  private static final String QTY = "qty";
  private static final String REGION = "region";
  private static final BigDecimal VOLUME_DISCOUNT_RATE = new BigDecimal("0.05");
  private static final BigDecimal MULTI_ITEM_DISCOUNT_RATE = new BigDecimal("0.02");
  private static final BigDecimal HIGH_AMOUNT_DISCOUNT_RATE = new BigDecimal("0.03");
  private static final int MULTI_ITEM_DISCOUNT_NUMBER_OF_LINES = 3;
  private static final BigDecimal HIGH_AMOUNT_DISCOUNT_APPLY_NET = new BigDecimal("100000");

  public OrderService(ProductRepository products, InventoryService inventory, TaxCalculator tax, DiscountCapPolicy capPolicy) {
    this.products = products;
    this.inventory = inventory;
    this.tax = tax;
    this.capPolicy = capPolicy;
  }
  
  public OrderService(ProductRepository products, InventoryService inventory, TaxCalculator tax) {
	  this(products, inventory, tax, new PercentCapPolicy(new BigDecimal("0.30"))); // デフォルト30%
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
	  
	  BigDecimal orderNetBeforeDiscount = BigDecimal.ZERO;
	  BigDecimal volumeDiscount = BigDecimal.ZERO;
	  List<DiscountType> appliedDiscounts = new ArrayList<DiscountType>();
	  for(Line line : req.lines()) {
		  // Optional<Product>をここでunwrap
		  Product product = products.findById(line.productId())
				  .orElseThrow(() -> new IllegalArgumentException( "product not found: " + line.productId()));

		  BigDecimal lineSubtotal = product.unitPrice()
				  .multiply(BigDecimal.valueOf(line.qty()))
				  .setScale(2, RoundingMode.HALF_UP);

		  orderNetBeforeDiscount = orderNetBeforeDiscount.add(lineSubtotal);
		  
		  // VOLUME割引
		  if(line.qty() >= 10) {
			  volumeDiscount = volumeDiscount.add(lineSubtotal
					  .multiply(VOLUME_DISCOUNT_RATE))
					  .setScale(2);
		  }		  
	  }

	  if(volumeDiscount.compareTo(BigDecimal.ZERO) == 1) {
		  appliedDiscounts.add(DiscountType.VOLUME);
	  }
	  BigDecimal subtotalVolumeDiscount = orderNetBeforeDiscount.subtract(volumeDiscount).setScale(2);
	  
	  BigDecimal multiItemDisctount = BigDecimal.ZERO;
	  BigDecimal subtotalMultiItemDiscount = BigDecimal.ZERO;
	  // MULTI_ITEM割引
	  if(req.lines().size() >= MULTI_ITEM_DISCOUNT_NUMBER_OF_LINES) {
		  multiItemDisctount = subtotalVolumeDiscount.multiply(MULTI_ITEM_DISCOUNT_RATE);
	  }
	  if(multiItemDisctount.compareTo(BigDecimal.ZERO) == 1) {
		  appliedDiscounts.add(DiscountType.MULTI_ITEM);
	  }
	  subtotalMultiItemDiscount = subtotalVolumeDiscount.subtract(multiItemDisctount).setScale(2);;

	  BigDecimal highAmountDisctount = BigDecimal.ZERO;
	  
	  // HIGH_AMOUNT割引
	  if(subtotalMultiItemDiscount.compareTo(HIGH_AMOUNT_DISCOUNT_APPLY_NET) >= 0) {
		  highAmountDisctount = subtotalMultiItemDiscount.multiply(HIGH_AMOUNT_DISCOUNT_RATE);
	  }
	  if(highAmountDisctount.compareTo(BigDecimal.ZERO) == 1) {
		  appliedDiscounts.add(DiscountType.HIGH_AMOUNT);
	  }

	  BigDecimal totalNetBeforeDiscount = BigDecimal.ZERO;
	  BigDecimal totalDiscount  = BigDecimal.ZERO;
	  BigDecimal totalNetAfterDiscount = BigDecimal.ZERO;
	  BigDecimal totalTax = BigDecimal.ZERO;
	  BigDecimal totalGross = BigDecimal.ZERO;

	  totalNetBeforeDiscount = orderNetBeforeDiscount;
	  BigDecimal rawDiscount  = BigDecimal.ZERO.add(volumeDiscount).add(multiItemDisctount).add(highAmountDisctount).setScale(2);

	  // Cap適用
	  BigDecimal cappedDiscount = capPolicy.apply(totalNetBeforeDiscount, rawDiscount);
	  totalDiscount = cappedDiscount;
	  totalNetAfterDiscount = orderNetBeforeDiscount.subtract(cappedDiscount);	  
	  
	  //税計算
	  totalTax = totalTax.add(tax.calcTaxAmount(totalNetBeforeDiscount, req.region(), req.mode()));
	  totalGross = totalGross.add(tax.addTax(totalNetBeforeDiscount, req.region(), req.mode()));
	  
	  //在庫確認(仮)
	  for(Line line : req.lines()) {
		  inventory.reserve(line.productId(), line.qty());
	  }
	  
	  OrderResult orderResult = new OrderResult(orderNetBeforeDiscount.setScale(2), totalDiscount.setScale(2), totalNetAfterDiscount.setScale(2), totalTax.setScale(2), totalGross.setScale(0), appliedDiscounts);

	  return orderResult;
  }
}
