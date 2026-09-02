# excel-exporter

애노테이션 기반으로 자바 객체 리스트를 엑셀(.xlsx) 파일로 변환해주는 Spring Boot 라이브러리/샘플 프로젝트입니다.
`@ExcelDocument`, `@ExcelColumn`, `@ExcelHeader` 애노테이션을 DTO에 붙이는 것만으로 시트 이름, 컬럼 순서/정렬/너비/포맷, 헤더 스타일이 적용된 엑셀 파일을 생성할 수 있습니다.

## 주요 기능

- **애노테이션 기반 매핑**: 필드에 `@ExcelColumn`을 붙이면 헤더명, 너비, 정렬, 표시 포맷을 지정할 수 있습니다.
- **자동 타입 변환**: `LocalDate`, `LocalDateTime`, `Date`, `BigDecimal`, `Number`, `Boolean` 등 필드 타입에 맞춰 셀 값과 기본 표시 포맷(날짜/일시)을 자동으로 적용합니다.
- **헤더 스타일 커스터마이징**: `@ExcelHeader`로 헤더 배경색·글자색·행 높이를 지정할 수 있습니다.
- **컬럼 순서 지정**: `order` 값을 지정한 컬럼끼리는 해당 값 기준으로, 지정하지 않은 컬럼은 필드 선언 순서로 정렬됩니다.
- **틀 고정(Freeze Pane)**: `@ExcelDocument`의 `colSplit`/`rowSplit`으로 틀 고정 영역을 설정할 수 있습니다.
- **대용량 처리**: Apache POI의 `SXSSFWorkbook`(스트리밍 API)을 사용해 대량의 행도 메모리 부담 없이 생성합니다.
- **컨트롤러 다운로드 지원**: `ExcelDownload` 반환 타입과 `ExcelDownloadReturnValueHandler`를 통해 컨트롤러에서 바로 파일 다운로드 응답을 내려줄 수 있습니다.

## 기술 스택

- Java 17
- Spring Boot 4.0.6 (`spring-boot-starter-webmvc`)
- Apache POI 5.5.1 (`poi`, `poi-ooxml`)
- Lombok
- H2 (런타임/콘솔용), JUnit 5

## 시작하기

### 빌드

```bash
./gradlew build
```

### 테스트

```bash
./gradlew test
```

`ExcelFileGeneratorTest`는 실제로 `build/excel-generator-test/members.xlsx` 파일을 생성하므로, 테스트 실행 후 해당 경로에서 결과물을 직접 열어 확인할 수 있습니다.

### 애플리케이션 실행

```bash
./gradlew bootRun
```

## 사용법

### 1. DTO에 애노테이션 적용

```java
@ExcelDocument(
    sheetName = "Members",
    rowSplit = 1, // 헤더 아래로 틀 고정
    excelHeader = @ExcelHeader(backgroundColor = "247270", textColor = "FFFFFF")
)
public class Member {

    @ExcelColumn(headerName = "이름", order = 1)
    private String name;

    @ExcelColumn(headerName = "나이", align = ExcelAlign.RIGHT, order = 2)
    private int age;

    // format을 지정하지 않으면 LocalDate/LocalDateTime 기본 포맷이 자동 적용됩니다.
    @ExcelColumn(headerName = "입사일", order = 3)
    private LocalDate joinedAt;
}
```

### 2. Workbook 생성

```java
List<Member> members = memberRepository.findAll();
Workbook workbook = ExcelFileGenerator.generate(members, Member.class);
```

### 3. 컨트롤러에서 바로 다운로드 응답 내려주기

`ExcelDownload`를 반환 타입으로 사용하려면 `ExcelDownloadReturnValueHandler`를 `WebMvcConfigurer`에 등록해야 합니다.

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
        handlers.add(new ExcelDownloadReturnValueHandler());
    }
}
```

```java
@GetMapping("/members/excel")
public ExcelDownload downloadMembers() {
    List<Member> members = memberRepository.findAll();
    Workbook workbook = ExcelFileGenerator.generate(members, Member.class);
    return ExcelDownload.of(workbook, "members.xlsx");
}
```

## 애노테이션 옵션

### `@ExcelDocument` (클래스 대상)

| 속성 | 기본값 | 설명 |
|---|---|---|
| `sheetName` | `Sheet1` | 시트 이름 |
| `excelHeader` | `@ExcelHeader()` | 헤더 행 스타일 |
| `colSplit` | `0` | 틀 고정 컬럼 수 |
| `rowSplit` | `0` | 틀 고정 행 수(헤더 아래 기준) |
| `isFooter` | `false` | 푸터 사용 여부 (현재 미구현) |

### `@ExcelColumn` (필드 대상)

| 속성 | 기본값 | 설명 |
|---|---|---|
| `headerName` | `""` | 헤더에 표시할 이름 |
| `format` | `""` | 셀 표시 포맷. 미지정 시 `LocalDate`/`Date`는 `yyyy-MM-dd`, `LocalDateTime`은 `yyyy-MM-dd HH:mm:ss`가 자동 적용 |
| `width` | `15` | 컬럼 너비(글자 수 기준) |
| `align` | `LEFT` | 가로 정렬(`LEFT`, `CENTER`, `RIGHT`) |
| `order` | `Integer.MAX_VALUE` | 컬럼 노출 순서. 지정한 컬럼끼리는 이 값 기준, 나머지는 필드 선언 순서 |

### `@ExcelHeader` (필드/애노테이션 값 대상)

| 속성 | 기본값 | 설명 |
|---|---|---|
| `backgroundColor` | `FFFFFF` | 헤더 배경색(RGB 헥스) |
| `textColor` | `000000` | 헤더 글자색(RGB 헥스) |
| `height` | `17` | 헤더 행 높이 |

## 프로젝트 구조

```
src/main/java/com/github/young
├── ExcelApplication.java              # Spring Boot 진입점
└── excel
    ├── ExcelDownload.java              # 컨트롤러 반환용 record (Workbook + 파일명)
    ├── annotation/                     # @ExcelDocument, @ExcelColumn, @ExcelHeader
    ├── constant/                       # 기본값 상수 (ExcelConstants)
    ├── enums/                          # 정렬 옵션 (ExcelAlign)
    ├── exception/                      # ExcelException
    ├── generator/                      # 리플렉션 기반 Workbook 생성 로직 (ExcelFileGenerator)
    ├── handler/                        # 컨트롤러 다운로드 응답 처리 (ExcelDownloadReturnValueHandler)
    └── style/                          # 헤더/컬럼 셀 스타일 적용 및 캐싱 (ExcelStyleApplier)
```

## 참고 문서

- [Apache POI 공식 문서](https://poi.apache.org/)
- [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.6/gradle-plugin)
- [Spring Web (MVC)](https://docs.spring.io/spring-boot/4.0.6/reference/web/servlet.html)
