package com.github.young.excel.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExcelConstants {

    public static final String DEFAULT_SHEET_NAME = "Sheet1";

    /**
     * POI Column Width 단위 (1/256th of a character width)
     */
    public static final int COLUMN_WIDTH_UNIT = 256;

    /**
     * 기본 컬럼 너비 (글자 수 기준)
     */
    public static final int DEFAULT_COLUMN_WIDTH = 15;

    /**
     * 기본 로우 높이
     */
    public static final int DEFAULT_ROW_HEIGHT = 17;

    /**
     * format 미지정 시 날짜 컬럼(LocalDate, Date)에 적용되는 기본 표시 형식
     */
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * format 미지정 시 날짜시간 컬럼(LocalDateTime)에 적용되는 기본 표시 형식
     */
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

}
