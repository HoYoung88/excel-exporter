package com.github.young.excel.style;

import com.github.young.excel.annotation.ExcelColumn;
import com.github.young.excel.annotation.ExcelHeader;
import com.github.young.excel.constant.ExcelConstants;
import com.github.young.excel.enums.ExcelAlign;
import lombok.Getter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ExcelStyleApplier {

    private final Workbook workbook;
    private final Map<HeaderStyleKey, CellStyle> headerStyleCache = new HashMap<>();
    private final Map<ColumnStyleKey, CellStyle> columnStyleCache = new HashMap<>();

    private ExcelStyleApplier(Workbook workbook) {
        this.workbook = workbook;
    }

    public static ExcelStyleApplier getInstance(Workbook workbook) {
        return new ExcelStyleApplier(workbook);
    }

    private CellStyle defaultStyle() {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        return cellStyle;
    }

    public CellStyle applyHeaderStyle(ExcelHeader excelHeader) {
        HeaderStyleKey key = new HeaderStyleKey(excelHeader.textColor(), excelHeader.backgroundColor());
        return headerStyleCache.computeIfAbsent(key, k -> createHeaderStyle(excelHeader));
    }

    public CellStyle applyColumnStyle(ExcelColumn excelColumn, Class<?> fieldType) {
        String format = resolveFormat(excelColumn, fieldType);
        ColumnStyleKey key = new ColumnStyleKey(excelColumn.align(), format);
        return columnStyleCache.computeIfAbsent(key, k -> createColumnStyle(excelColumn.align(), format));
    }

    private CellStyle createHeaderStyle(ExcelHeader excelHeader) {
        CellStyle style = defaultStyle();

        XSSFColor textColor = new XSSFColor(hexToRgb(excelHeader.textColor()));
        Font font = workbook.createFont();
        font.setBold(true);
        ((XSSFFont) font).setColor(textColor);
        style.setFont(font);

        XSSFColor backgroundColor = new XSSFColor(hexToRgb(excelHeader.backgroundColor()));
        style.setFillForegroundColor(backgroundColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        applyHorizontalAlignment(style, ExcelAlign.CENTER);

        return style;
    }

    private String resolveFormat(ExcelColumn excelColumn, Class<?> fieldType) {
        if (!excelColumn.format().isBlank()) {
            return excelColumn.format();
        }

        if (fieldType == LocalDateTime.class) {
            return ExcelConstants.DEFAULT_DATETIME_FORMAT;
        }

        if (fieldType == LocalDate.class || fieldType == Date.class) {
            return ExcelConstants.DEFAULT_DATE_FORMAT;
        }

        return "";
    }

    private CellStyle createColumnStyle(ExcelAlign align, String format) {
        CellStyle style = defaultStyle();

        // format이 지정되면(자동 추론된 날짜 포맷 포함) 표시 형식이 우선이므로 가로 정렬은 엑셀 기본 정렬에 맡긴다.
        if (format.isBlank()) {
            applyHorizontalAlignment(style, align);
        } else {
            style.setDataFormat(workbook.createDataFormat().getFormat(format));
        }

        return style;
    }

    private void applyHorizontalAlignment(CellStyle style, ExcelAlign align) {
        switch (align) {
            case CENTER -> style.setAlignment(HorizontalAlignment.CENTER);
            case RIGHT -> style.setAlignment(HorizontalAlignment.RIGHT);
            default -> style.setAlignment(HorizontalAlignment.LEFT);
        }

    }

    private byte[] hexToRgb(String hex) {
        int r = Integer.valueOf(hex.substring(0, 2), 16);
        int g = Integer.valueOf(hex.substring(2, 4), 16);
        int b = Integer.valueOf(hex.substring(4, 6), 16);

        return new byte[]{(byte) r, (byte) g, (byte) b};
    }

    private record HeaderStyleKey(String textColor, String backgroundColor) {
    }

    private record ColumnStyleKey(ExcelAlign align, String format) {
    }

}
