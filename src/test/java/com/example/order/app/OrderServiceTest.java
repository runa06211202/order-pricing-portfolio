package com.example.order.app;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.order.domain.Product;
import com.example.order.dto.DiscountType;
import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderRequest.Line;
import com.example.order.dto.OrderResult;
import com.example.order.port.InventoryService;
import com.example.order.port.ProductRepository;
import com.example.order.port.TaxCalculator;

@ExtendWith(MockitoExtension.class)
/**
 * 関連ADR:
 *  - ADR-001 金額スケール正規化
 *  - ADR-003 Repository の findById は null を返さない（“存在しない”は Optional.empty）
 *  - ADR-004 DiscountType enum化
 */
class OrderServiceTest {

  @Mock ProductRepository products;
  @Mock InventoryService inventory;
  @Mock TaxCalculator tax;

  // SUTは後で実装
  OrderService sut;

  @BeforeEach
  void setUp() {
    sut = new OrderService(products, inventory, tax); // コンストラクタ実装はこれから
  }

  @Nested class Guards {
    @Test
    @DisplayName("G-1-1: linesが nullのとき IAEがThrowされる")
    void throwsWhenLinesIsNull() {
    	// Given: lines = null
    	OrderRequest req = new OrderRequest("JP", RoundingMode.HALF_UP, null);
    	//When: sut.placeOrder(req) Then: IAE
    	assertThrows(IllegalArgumentException.class, () -> sut.placeOrder(req));
    	verifyNoInteractions(products, inventory, tax);
    }

    @Test
    @DisplayName("G-1-2: linesが 空のとき IAEがThrowされる")
    void throwsWhenLinesIsEmpty() {
    	// Given: lines = null
    	OrderRequest req = new OrderRequest("JP", RoundingMode.HALF_UP, List.of());
    	//When: sut.placeOrder(req) Then: IAE
    	assertThatThrownBy(() -> sut.placeOrder(req))
    		.isInstanceOf(IllegalArgumentException.class)
    		.hasMessageContainingAll("lines");
    	verifyNoInteractions(products, inventory, tax);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("G-2-1: qtyが 0>=の時 IAEがThrowされる")
    void throwsWhenQtyIsNonPositive(int qty) {
    	// Given: qty == 0, qty <= 0 両方検証
    	OrderRequest req = new OrderRequest("JP", RoundingMode.HALF_UP, List.of(new Line("P01", qty)));
    	//When: sut.placeOrder(req) Then: IAE
    	assertThatThrownBy(() -> sut.placeOrder(req))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContainingAll("qty");
    	verifyNoInteractions(products, inventory, tax);
    }

    static Stream<String> blankStrings() {
        return Stream.of(
            null,
            "",
            " ",
            "   ",
            "\t",
            "\n"
        );
    }
    @ParameterizedTest
    @MethodSource("blankStrings")
    @DisplayName("G-3-1: regionが nullまたは空または空文字の時 IAEがThrowされる")
    void throwsWhenRegionIsBlank(String region) {
    	// Given: region = null or "" or " "etc.blank strings
    	OrderRequest req = new OrderRequest(region, RoundingMode.HALF_UP, List.of(new Line("P01", 5)));
    	//When: sut.placeOrder(req) Then: IAE
    	assertThatThrownBy(() -> sut.placeOrder(req))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContainingAll("region");
    	verifyNoInteractions(products, inventory, tax);
    }

    @Test
    @DisplayName("G-4-1: products.findById呼出結果が 未取得の時 IAEがThrowされる")
    void throwsWhenProductsIsNotFound() {
    	// Given: Product.findbyIdの返却値がOptional.empty
    	OrderRequest req = new OrderRequest("JP", RoundingMode.HALF_UP, List.of(new Line("A", 5), new Line("B", 5)));
    	when(products.findById("A")).thenReturn(Optional.of(new Product("A", new BigDecimal("100"))));
    	when(products.findById("B")).thenReturn(Optional.empty()); // これで「Bが存在しない」ケース

    	//When: findById("B") Then: IAE
    	assertThatThrownBy(() -> sut.placeOrder(req))
        	.isInstanceOf(IllegalArgumentException.class)
        	.hasMessageContaining("product not found: B");
    	verifyNoInteractions(inventory, tax);
    }
  }

  @Nested class Normal {
	@Test
	@DisplayName("N-1-1: 割引適用無し")
	void checkNoDiscount() {
		// Given: Line("A", 5)("B", 5)
		OrderRequest req = new OrderRequest("JP", RoundingMode.HALF_UP, List.of(new Line("A", 5), new Line("B", 5)));
		when(products.findById("A")).thenReturn(Optional.of(new Product("A", new BigDecimal("100"))));
		when(products.findById("B")).thenReturn(Optional.of(new Product("B", new BigDecimal("200"))));
		
		// モック呼出(税計算)呼出だけ確認するため任意値
		when(tax.addTax(any(), anyString(), any())).thenReturn(BigDecimal.TEN);
		when(tax.calcTaxAmount(any(), anyString(), any())).thenReturn(BigDecimal.ONE);
		
		//When: sut.placeOrder(req)
		OrderResult result = sut.placeOrder(req);

		//Then: totalNetBeforeDiscount = 1500.00, totalDiscount = 0.00
		assertThat(result.totalNetBeforeDiscount()).isEqualByComparingTo("1500.00");
		assertThat(result.totalDiscount()).isEqualByComparingTo("0.00");
		
		verify(tax).calcTaxAmount(any(), eq("JP"), eq(RoundingMode.HALF_UP));
		verify(tax).addTax(any(), eq("JP"), eq(RoundingMode.HALF_UP));

	}
	@Test
	@DisplayName("N-1-2: VOLUME割引適用")
	void checkVolumeDiscount() {
		// Given: Line("A", 15)("B", 5)
		OrderRequest req = new OrderRequest("JP", RoundingMode.HALF_UP, List.of(new Line("A", 15), new Line("B", 5)));
		when(products.findById("A")).thenReturn(Optional.of(new Product("A", new BigDecimal("100"))));
		when(products.findById("B")).thenReturn(Optional.of(new Product("B", new BigDecimal("200"))));

		// モック呼出(税計算)呼出だけ確認するため任意値
		when(tax.addTax(any(), anyString(), any())).thenReturn(BigDecimal.TEN);
		when(tax.calcTaxAmount(any(), anyString(), any())).thenReturn(BigDecimal.ONE);

		//When: sut.placeOrder(req)
		OrderResult result = sut.placeOrder(req);

		//Then: totalNetBeforeDiscount = 2500.00, totalDiscount = 75.00 appliedDiscounts = [VOLUME}
		assertThat(result.totalNetBeforeDiscount()).isEqualByComparingTo("2500.00");
		assertThat(result.totalDiscount()).isEqualByComparingTo("75.00");
		assertThat(result.appliedDiscounts()).isEqualTo(List.of(DiscountType.VOLUME));
		
		verify(tax).calcTaxAmount(any(), eq("JP"), eq(RoundingMode.HALF_UP));
		verify(tax).addTax(any(), eq("JP"), eq(RoundingMode.HALF_UP));

	}
    @Test @Disabled("skeleton")
    void endToEnd_happyPath_returnsExpectedTotalsAndLabels() {}
  }

  @Nested class Threshold {
	static Stream<Arguments> volumeDiscountThresholds() {
		return Stream.of(
				Arguments.of(9,  new BigDecimal("9000.00"),  new BigDecimal("0.00"), List.of()),
				Arguments.of(10, new BigDecimal("10000.00"), new BigDecimal ("500.00"), List.of(DiscountType.VOLUME)),
				Arguments.of(11, new BigDecimal("11000.00"), new BigDecimal ("550.00"), List.of(DiscountType.VOLUME))
		);
	}
	@ParameterizedTest
	@MethodSource("volumeDiscountThresholds")
    void volumeBoundary_qty9_10_11(int qty, BigDecimal expectedNet, BigDecimal expectedDiscount, List<DiscountType> expectedLabels) {
		when(products.findById("A"))
			.thenReturn(Optional.of(new Product("A", new BigDecimal("1000"))));

		// Given: qty = 9/10/11
		OrderRequest req = new OrderRequest("JP", RoundingMode.HALF_UP, List.of(new Line("A", qty)));

		//When: sut.placeOrder(req)
		OrderResult result = sut.placeOrder(req);

		//Then: totalNetBeforeDiscount = 9000.00/10000.00/11000.00, totalDiscount = 0.00/500.00/550.00 appliedDiscounts = []/[VOLUME}/[VOLUME}
		assertThat(result.totalNetBeforeDiscount()).isEqualByComparingTo(expectedNet);
		assertThat(result.totalDiscount()).isEqualByComparingTo(expectedDiscount);
		assertThat(result.appliedDiscounts()).containsExactlyElementsOf(expectedLabels);
	}
    @Test @Disabled("skeleton")
    void multiItemBoundary_kinds2_3_4() {}
    @Test @Disabled("skeleton")
    void highAmountBoundary_99999_100000_100001() {}
  }

  @Nested class VerifyCalls {
    @Test @Disabled("skeleton")
    void reservesInOrder_afterDiscounts_onlyOnceEach() {}
  }

  @Nested class Abnormal {
    @Test @Disabled("skeleton")
    void inventoryThrows_taxNotCalled() {}
  }
}
