package com.github.young.excel.generator;

import com.github.young.excel.annotation.ExcelColumn;
import com.github.young.excel.annotation.ExcelDocument;
import com.github.young.excel.annotation.ExcelHeader;
import com.github.young.excel.enums.ExcelAlign;
import com.github.young.excel.exception.ExcelException;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelFileGeneratorTest {

    @ExcelDocument(sheetName = "Members",
            colSplit = 0,
            rowSplit = 1,
            excelHeader = @ExcelHeader(backgroundColor = "247270", textColor = "FFFFFF"))
    @AllArgsConstructor
    static class Member {

        @ExcelColumn(headerName = "이름")
        private String name;

        @ExcelColumn(headerName = "나이", align = ExcelAlign.RIGHT)
        private int age;

        // format을 지정하지 않아도 날짜 기본 포맷이 자동 적용되는지 확인용
        @ExcelColumn(headerName = "입사일")
        private LocalDate joinedAt;
    }

    @Test
    void createWorkbook_writesExcelFileToGivenLocalPath() throws IOException {
        // 원하는 로컬 경로로 바꿔서 실행하면 생성된 엑셀을 직접 열어 확인할 수 있습니다.
        Path outputPath = Path.of(System.getProperty("user.dir"), "build", "excel-generator-test", "members.xlsx");
        Files.createDirectories(outputPath.getParent());

        List<Member> members = List.of(
            new Member("홍길동", 30, LocalDate.of(2024, 3, 2)),
            new Member("김철수", 25, LocalDate.of(2022, 11, 15))
        );

        try (Workbook workbook = ExcelFileGenerator.generate(members, Member.class);
             OutputStream out = Files.newOutputStream(outputPath)) {
            workbook.write(out);
        }

        assertTrue(Files.exists(outputPath));
        assertTrue(Files.size(outputPath) > 0);

        System.out.println("Generated excel file: " + outputPath.toAbsolutePath());
    }

    static class NoAnnotation {
        private String name;
    }

    @Test
    void generate_throwsExcelException_whenExcelDocumentAnnotationMissing() {
        List<NoAnnotation> rows = List.of(new NoAnnotation());

        assertThrows(ExcelException.class, () -> ExcelFileGenerator.generate(rows, NoAnnotation.class));
    }

    @ExcelDocument(sheetName = "DefaultHeader")
    @AllArgsConstructor
    static class DefaultHeaderStyle {

        @ExcelColumn(headerName = "이름")
        private String name;
    }

    @Test
    void generate_usesWhiteBackgroundAndBlackText_whenExcelHeaderNotSpecified() throws IOException {
        List<DefaultHeaderStyle> rows = List.of(new DefaultHeaderStyle("홍길동"));

        try (Workbook workbook = ExcelFileGenerator.generate(rows, DefaultHeaderStyle.class)) {
            assertTrue(workbook.getSheetAt(0).getRow(0).getCell(0) != null);
        }
    }

    @ExcelDocument(sheetName = "Ordered",
            excelHeader = @ExcelHeader(backgroundColor = "247270", textColor = "FFFFFF"))
    @AllArgsConstructor
    static class OrderedColumns {

        // 필드 선언 순서(c, a, b)와 반대로 order를 지정해서, 실제 출력 순서가 order를 따르는지 확인
        @ExcelColumn(headerName = "C", order = 3)
        private String c;

        @ExcelColumn(headerName = "A", order = 1)
        private String a;

        @ExcelColumn(headerName = "B", order = 2)
        private String b;
    }

    @Test
    void generate_ordersColumnsByExplicitOrderAttribute() {
        List<OrderedColumns> rows = List.of(new OrderedColumns("c", "a", "b"));

        try (Workbook workbook = ExcelFileGenerator.generate(rows, OrderedColumns.class)) {
            Row headerRow = workbook.getSheetAt(0).getRow(0);

            assertEquals("A", headerRow.getCell(0).getStringCellValue());
            assertEquals("B", headerRow.getCell(1).getStringCellValue());
            assertEquals("C", headerRow.getCell(2).getStringCellValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ExcelDocument(sheetName = "Prices")
    @AllArgsConstructor
    static class Price {

        @ExcelColumn(headerName = "상품명")
        private String productName;

        @ExcelColumn(headerName = "가격")
        private BigDecimal amount;
    }

    @Test
    void generate_writesBigDecimalAsNumericCellValue() {
        List<Price> rows = List.of(new Price("노트북", new BigDecimal("1299999.99")));

        try (Workbook workbook = ExcelFileGenerator.generate(rows, Price.class)) {
            Cell cell = workbook.getSheetAt(0).getRow(1).getCell(1);

            assertEquals(CellType.NUMERIC, cell.getCellType());
            assertEquals(1299999.99, cell.getNumericCellValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
