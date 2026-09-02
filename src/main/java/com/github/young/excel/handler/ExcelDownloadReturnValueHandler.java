package com.github.young.excel.handler;

import com.github.young.excel.ExcelDownload;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@Slf4j
public class ExcelDownloadReturnValueHandler implements HandlerMethodReturnValueHandler {

    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return ExcelDownload.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public void handleReturnValue(
        Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest
    ) throws IOException {
        mavContainer.setRequestHandled(true);

        if (returnValue == null) {
            return;
        }

        ExcelDownload excelDownload = (ExcelDownload) returnValue;
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
        if (response == null) {
            throw new IllegalStateException("HttpServletResponse를 가져올 수 없습니다.");
        }

        // ExcelGenerator.create(...)는 성공 경로에서 워크북을 닫지 않으므로(멀티시트 지원 시 write() 전에
        // 임시파일이 사라지지 않게 하기 위함) write() 이후 여기서 닫는다.
        try (Workbook workbook = excelDownload.workbook()) {
            String encodedFileName = URLEncoder.encode(excelDownload.fileName(), StandardCharsets.UTF_8);

            response.setContentType(EXCEL_CONTENT_TYPE);
            response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName
            );
            workbook.write(response.getOutputStream());
        }
    }
}

