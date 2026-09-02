package com.github.young.excel.generator;

import com.github.young.excel.annotation.ExcelColumn;
import com.github.young.excel.annotation.ExcelDocument;
import com.github.young.excel.annotation.ExcelHeader;
import com.github.young.excel.constant.ExcelConstants;
import com.github.young.excel.exception.ExcelException;
import com.github.young.excel.style.ExcelStyleApplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;

@Slf4j(topic = "excel")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExcelFileGenerator<T> {

    private List<T> excelRowData = new ArrayList<>();
    private Class<T> type = null;
    private int EXCEL_HEADER_ROW_INDEX = 0;

    private ExcelFileGenerator(List<T> excelRowData, Class<T> type) {
        this.excelRowData = excelRowData;
        this.type = type;
    }

    public static <T> Workbook generate(List<T> excelRowData, Class<T> type) {
        ExcelFileGenerator<T> excelFileGenerator = new ExcelFileGenerator<>(excelRowData, type);
        return excelFileGenerator.createWorkbook();
    }

    private Workbook createWorkbook() {
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        renderSheet(workbook);
        return workbook;
    }

    public List<T> getExcelRowData() {
        return Collections.unmodifiableList(excelRowData);
    }

    private void renderSheet(Workbook workbook) {

        ExcelDocument excelDocument = getExcelDocument();
        Sheet sheet = workbook.createSheet(excelDocument.sheetName());

        ExcelStyleApplier excelStyleApplier = ExcelStyleApplier.getInstance(workbook);
        List<ExcelColumnMeta> columnMetas = prepareColumnMetas(excelStyleApplier);

        applyFreezePane(sheet, excelDocument, EXCEL_HEADER_ROW_INDEX);

        renderHeader(sheet, excelStyleApplier, columnMetas, excelDocument);
        renderColumn(sheet, columnMetas);
        renderFooter(sheet, columnMetas);

    }

    private void renderHeader(Sheet sheet, ExcelStyleApplier excelStyleApplier, List<ExcelColumnMeta> columnMetas, ExcelDocument excelDocument) {
        Row headerRow = sheet.createRow(EXCEL_HEADER_ROW_INDEX);
        ExcelHeader excelHeader = excelDocument.excelHeader();
        int colIndex = 0;

        for (ExcelColumnMeta columnMeta : columnMetas) {

            sheet.setColumnWidth(colIndex, columnMeta.excelColumn().width() * ExcelConstants.COLUMN_WIDTH_UNIT);
            headerRow.setHeightInPoints(excelHeader.height());

            Cell cell = headerRow.createCell(colIndex);
            cell.setCellValue(columnMeta.excelColumn().headerName());
            cell.setCellStyle(excelStyleApplier.applyHeaderStyle(excelHeader));

            colIndex++;
        }

    }

    private void renderColumn(Sheet sheet, List<ExcelColumnMeta> columnMetas) {
        int rowIndex = EXCEL_HEADER_ROW_INDEX + 1;

        for (Object data : getExcelRowData()) {
            Row row = sheet.createRow(rowIndex);
            int colIndex = 0;

            for (ExcelColumnMeta columnMeta : columnMetas) {
                Cell cell = row.createCell(colIndex);

                try {
                    Object value = columnMeta.field().get(data);
                    applyValueWithStyle(cell, value, columnMeta.columnStyle());
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("필드 접근 실패: " + columnMeta.field().getName(), e);
                }

                colIndex++;
            }

            rowIndex++;
        }

    }

    private void renderFooter(Sheet sheet,  List<ExcelColumnMeta> columnMetas) {

    }

    private ExcelDocument getExcelDocument() {
        ExcelDocument excelDocument = type.getAnnotation(ExcelDocument.class);

        if (excelDocument == null) {
            throw new ExcelException(type.getName() + "에 @ExcelDocument 애노테이션이 없습니다.");
        }

        return excelDocument;
    }

    private void applyFreezePane(Sheet sheet, ExcelDocument excelDocument, int headerRowIndex) {
        int colSplit = excelDocument.colSplit();
        int rowSplit = excelDocument.rowSplit();

        if (colSplit <= 0 && rowSplit <= 0) {
            return;
        }

        int effectiveRowSplit = headerRowIndex > 0 ? headerRowIndex + 1 + rowSplit : rowSplit;

        sheet.createFreezePane(colSplit, effectiveRowSplit);
    }

    // 새 타입을 지원하려면 이 목록에 항목만 추가하면 되고, 기존 항목은 건드릴 필요가 없다.
    private static final List<CellValueWriter> CELL_VALUE_WRITERS = List.of(
            new CellValueWriter(LocalDateTime.class, (cell, value) -> cell.setCellValue((LocalDateTime) value)),
            new CellValueWriter(LocalDate.class, (cell, value) -> cell.setCellValue((LocalDate) value)),
            new CellValueWriter(Date.class, (cell, value) -> cell.setCellValue((Date) value)),
            new CellValueWriter(BigDecimal.class, (cell, value) -> cell.setCellValue(((BigDecimal) value).doubleValue())),
            new CellValueWriter(Number.class, (cell, value) -> cell.setCellValue(((Number) value).doubleValue())),
            new CellValueWriter(Boolean.class, (cell, value) -> cell.setCellValue((Boolean) value))
    );

    private void applyValueWithStyle(Cell cell, Object value, CellStyle style) {
        Object cellValue = value == null ? "" : value;

        CELL_VALUE_WRITERS.stream()
                .filter(writer -> writer.supports(cellValue))
                .findFirst()
                .ifPresentOrElse(
                        writer -> writer.write(cell, cellValue),
                        () -> cell.setCellValue(cellValue.toString())
                );

        cell.setCellStyle(style);
    }

    private record CellValueWriter(Class<?> type, BiConsumer<Cell, Object> writer) {
        boolean supports(Object value) {
            return type.isInstance(value);
        }

        void write(Cell cell, Object value) {
            writer.accept(cell, value);
        }
    }

    private List<ExcelColumnMeta> prepareColumnMetas(ExcelStyleApplier excelStyleApplier) {
        List<ExcelColumnMeta> columnMetas = new ArrayList<>();

        for (Field field : type.getDeclaredFields()) {
            ExcelColumn columnAnno = field.getAnnotation(ExcelColumn.class);

            if (columnAnno != null) {
                field.setAccessible(true);
                CellStyle columnStyle = excelStyleApplier.applyColumnStyle(columnAnno, field.getType());

                columnMetas.add(new ExcelColumnMeta(field, columnAnno, columnStyle));
            }
        }

        columnMetas.sort(Comparator.comparingInt(meta -> meta.excelColumn().order()));

        return columnMetas;
    }

    private record ExcelColumnMeta(
            Field field,
            ExcelColumn excelColumn,
            CellStyle columnStyle
    ) {
    }


}
