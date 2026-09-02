package com.github.young.excel;

import org.apache.poi.ss.usermodel.Workbook;

public record ExcelDownload(Workbook workbook, String fileName) {

    public static ExcelDownload of(Workbook workbook, String fileName) {
        return new ExcelDownload(workbook, fileName);
    }
}
