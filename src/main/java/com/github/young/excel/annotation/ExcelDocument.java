package com.github.young.excel.annotation;


import com.github.young.excel.constant.ExcelConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelDocument {

    String sheetName() default ExcelConstants.DEFAULT_SHEET_NAME;

    ExcelHeader excelHeader() default @ExcelHeader();

    int colSplit() default 0;

    int rowSplit() default 0;

    boolean isFooter() default false;
}
