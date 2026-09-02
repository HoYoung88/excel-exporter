package com.github.young.excel.annotation;


import com.github.young.excel.constant.ExcelConstants;
import com.github.young.excel.enums.ExcelAlign;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumn {

    String headerName() default "";

    String format() default "";

    int width() default ExcelConstants.DEFAULT_COLUMN_WIDTH;

    ExcelAlign align() default ExcelAlign.LEFT;

    /**
     * 컬럼 노출 순서. 지정하지 않으면 선언 순서를 따르되, 값을 지정한 컬럼들 사이에서는 이 값 기준으로 정렬된다.
     */
    int order() default Integer.MAX_VALUE;

}
