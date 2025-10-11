package com.example.order.app;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderRequest.Line;
import com.example.order.port.InventoryService;
import com.example.order.port.ProductRepository;
import com.example.order.port.TaxCalculator;

@ExtendWith(MockitoExtension.class)
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
    void throwsWhenLinesNull() {
    	// Given: lines = null
    	OrderRequest req = new OrderRequest("JP", RoundingMode.HALF_UP, null);
    	//When: sut.placeOrder(req) Then: IAE
    	assertThrows(IllegalArgumentException.class, () -> sut.placeOrder(req));
    	verifyNoInteractions(products, inventory, tax);
    }

    @Test
    @DisplayName("G-1-2: linesが 空のとき IAEがThrowされる")
    void throwsWhenLinesEmpty() {
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
    void throwsWhenQtyNonPositive(int qty) {
    	// Given: qty <= 0
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
    void throwsWhenRegionBlank(String region) {
    	// Given: region = null or "" or " "etc.blank strings
    	OrderRequest req = new OrderRequest(region, RoundingMode.HALF_UP, List.of(new Line("P01", 5)));
    	//When: sut.placeOrder(req) Then: IAE
    	assertThatThrownBy(() -> sut.placeOrder(req))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContainingAll("region");
    	verifyNoInteractions(products, inventory, tax);
    }
  }

  @Nested class Normal {
    @Test @Disabled("skeleton")
    void endToEnd_happyPath_returnsExpectedTotalsAndLabels() {}
  }

  @Nested class Threshold {
    @Test @Disabled("skeleton")
    void volumeBoundary_qty9_10_11() {}
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
