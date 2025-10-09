package com.example.order.app;

import com.example.order.dto.*;
import com.example.order.port.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    @Test @Disabled("skeleton")
    void throwsWhenLinesNull() {}
    @Test @Disabled("skeleton")
    void throwsWhenLinesEmpty() {}
    @Test @Disabled("skeleton")
    void throwsWhenQtyNonPositive() {}
    @Test @Disabled("skeleton")
    void throwsWhenRegionBlank() {}
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
