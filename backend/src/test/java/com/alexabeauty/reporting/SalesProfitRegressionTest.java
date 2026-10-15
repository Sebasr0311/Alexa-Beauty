package com.alexabeauty.reporting;

import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.order.domain.*;
import com.alexabeauty.order.repository.OrderRepository;
import com.alexabeauty.reporting.dto.SalesSummaryDto;
import com.alexabeauty.reporting.excel.ExcelSalesReportGenerator;
import com.alexabeauty.reporting.service.SalesServiceImpl;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SalesProfitRegressionTest {
 private static final LocalDate DATE=LocalDate.of(2026,10,15);
 private Order order(BigDecimal discount) {
  var known=OrderItem.builder().sku("KNOWN").productName("Known historical cost").quantity(2).unitPrice(new BigDecimal("50")).subtotal(new BigDecimal("100")).costSnapshot(new BigDecimal("20")).build();
  var free=OrderItem.builder().sku("ZERO").productName("Confirmed zero cost").quantity(1).unitPrice(new BigDecimal("30")).subtotal(new BigDecimal("30")).costSnapshot(BigDecimal.ZERO).build();
  var unknown=OrderItem.builder().sku("UNKNOWN").productName("Unknown historical cost").quantity(1).unitPrice(new BigDecimal("70")).subtotal(new BigDecimal("70")).variant(ProductVariant.builder().referenceCost(new BigDecimal("5")).build()).build();
  return Order.builder().reference("PROFIT-TEST").customerName("Test customer").status(OrderStatus.PAGADO).subtotal(new BigDecimal("200")).shippingFee(BigDecimal.TEN).discount(discount).total(new BigDecimal("210").subtract(discount)).items(List.of(known,free,unknown)).build();
 }
 private SalesServiceImpl service(Order order) {
  var repository=mock(OrderRepository.class);
  when(repository.findPaidOrdersList(eq(OrderRepository.VERIFIED_PAID_STATUSES),any(),any(),isNull())).thenReturn(List.of(order));
  return new SalesServiceImpl(repository,new ExcelSalesReportGenerator());
 }
 @Test void profitUsesOnlyFrozenKnownCostsIncludingZero() {
  SalesSummaryDto result=service(order(BigDecimal.ZERO)).getAnalytics(DATE,DATE).summary();
  assertThat(result.knownCostTotal()).isEqualByComparingTo("40");
  assertThat(result.coveredMerchandiseRevenue()).isEqualByComparingTo("130");
  assertThat(result.grossProfit()).isEqualByComparingTo("90");
  assertThat(result.unitsWithoutCost()).isEqualTo(1);
 }
 @Test void proportionalDiscountExcludesShippingAndNeverReconstructsUnknownCost() {
  var order=order(new BigDecimal("40"));var service=service(order);
  var result=service.getAnalytics(DATE,DATE).summary();
  assertThat(result.totalSales()).isEqualByComparingTo("170");
  assertThat(result.merchandiseRevenue()).isEqualByComparingTo("160");
  assertThat(result.coveredMerchandiseRevenue()).isEqualByComparingTo("104");
  assertThat(result.knownCostTotal()).isEqualByComparingTo("40");
  assertThat(result.grossProfit()).isEqualByComparingTo("64");
  assertThat(result.grossMarginPercentage()).isEqualByComparingTo("61.54");
  assertThat(result.unitsWithoutCost()).isEqualTo(1);
  order.getItems().get(2).getVariant().setReferenceCost(new BigDecimal("999"));
  assertThat(service.getAnalytics(DATE,DATE).summary()).isEqualTo(result);
 }
 @Test void excelExportsUnknownCostAsTextZeroAsNumericAndMatchingDiscountedProfit() throws Exception {
  byte[] bytes=service(order(new BigDecimal("40"))).exportSalesReport(DATE,DATE,null,null);
  try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
   var details=workbook.getSheet("DETALLE_PRODUCTOS");
   assertThat(details.getRow(5).getCell(8).getNumericCellValue()).isEqualTo(20);
   assertThat(details.getRow(6).getCell(8).getCellType()).isEqualTo(CellType.NUMERIC);
   assertThat(details.getRow(6).getCell(8).getNumericCellValue()).isZero();
   assertThat(details.getRow(7).getCell(8).getCellType()).isEqualTo(CellType.STRING);
   assertThat(details.getRow(7).getCell(8).getStringCellValue()).isEqualTo("Sin costo histórico");
   var summary=workbook.getSheet("RESUMEN");
   assertThat(summary.getRow(16).getCell(1).getNumericCellValue()).isEqualTo(104);
   assertThat(summary.getRow(17).getCell(1).getNumericCellValue()).isEqualTo(40);
   assertThat(summary.getRow(18).getCell(1).getNumericCellValue()).isEqualTo(64);
   assertThat(summary.getRow(20).getCell(1).getNumericCellValue()).isEqualTo(1);
  }
 }
}
