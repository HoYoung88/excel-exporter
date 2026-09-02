package com.github.young.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.github.young.excel.constant.ExcelConstants.DEFAULT_ROW_HEIGHT;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelHeader {

    String backgroundColor() default "FFFFFF";

    String textColor() default "000000";

    int height() default DEFAULT_ROW_HEIGHT;


}
