# 진행 기록: js-libs (JS 값 모델과 무관한 독립 라이브러리)

담당 경로: `src/org/duckdns/hjow/swingwebpanel/js/{regexp,num,date,text}/**`, `tests/src/org/duckdns/hjow/swingwebpanel/js/{regexp,num,date,text}/**`, 이 문서.
검증 명령: `scripts/pkgtest.sh jslibs "js/regexp js/num js/date js/text" "js/regexp js/num js/date js/text"` (JDK 6u45 `javac 1.6.0_45`).
규격 가정: ECMA-262 6th edition 본문은 조회가 403 이어서 확인하지 못했다. ES2015 알고리즘은 작성자의 지식에 따랐고 각 클래스 Javadoc 에 가정을 적었다.
도구 주의: 도구 입력의 `\uXXXX` 는 실제 문자로 바뀐다. 소스에는 `(char) 0x2028` 형태를 쓰고, 정규식 시험 문자열은 `"\u0041"` 로 쓴다.

---

## 2026-11-16 · P14-03(라이브러리 부분) · js.regexp 완료

- 현재 작업 번호: P14-03 (RegExp 라이브러리 부분), P12-05/P12-08 의 정규식 리터럴 조기 오류 검사
- 완료한 내용: 처음부터 만든 ES2015 정규식 엔진(`java.util.regex` 미사용).
  `RegExpParser`(재귀 하강, 부속서 B 호환 구문, u 모드 엄격 오류, 이름 있는 그룹은 ES2018 확장) →
  `RegExpNode`(AST) → `RegExpCompiler`(명령어 생성, 단일 문자 원자 반복은 REPEAT 하나로, 그 밖은 횟수 반복문 + 빈 반복 검사 + 반복마다 캡처 초기화) →
  `RegExpProgram`/`RegExpMatcher`(명시적 스택을 쓰는 역추적 VM: 재귀 없음, 캡처/레지스터 되돌림 기록, 원자적 선행 탐색, 단계 한도/스택 한도/취소 훅).
  `CaseFold`(ES2015 Canonicalize, u 모드는 단순 접기 근사), `RegExpCharSet`, `RegExpFlags`, 예외 `RegExpSyntaxException`(위치 포함)/`RegExpLimitException`, `RegExpCancel`.
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/regexp/*`, `tests/src/org/duckdns/hjow/swingwebpanel/js/regexp/{RegExpTest,RegExpFuzzTest,ReferenceMatcher}.java`
- 검증 결과: `scripts/pkgtest.sh jslibs ...` → regexp 17 사례 통과(규격 예제, 구문 오류 40여 건, 대소문자/유니코드, 한도/취소, 100만 글자 입력),
  **차분 시험**: 규격 방식 재귀 참조 구현(`ReferenceMatcher`)과 5000 패턴 x 6 입력(캡처 안의 수량자, 빈 본문, 역참조, 선행 탐색, m/i 플래그)이 모두 일치,
  `java.util.regex` 와 3000 패턴 x 6 입력 비교(불일치는 참조 구현으로 판정, Java 편차 1% 이하 확인). sourcecheck(encoding,forbidden,javadoc,loops) 위반 0.
- 발견한 사실: JRE 6 `java.util.regex` 는 일부 패턴(빈 선택지가 있는 복잡한 패턴)에서 잘못된 "일치 없음"을 반환한다 → 오라클로 단독 사용하지 않음.
- 미해결 사항(알려진 한계): ① 뒷쪽 탐색(lookbehind, ES2018) 미지원 — `(?<=`, `(?<!` 는 "Invalid group" 오류. ② 유니코드 속성 이스케이프 `\p{..}` 미지원(ES2018).
  ③ u 모드 대소문자 접기는 `toLowerCase(toUpperCase)` 근사이며 JRE 6 의 유니코드 4.0 데이터에 한정. ④ `\w` 의 u+i 확장(U+017F, U+212A 포함)은 구현하지 않음(ES2015 의 \w 는 ASCII).
  ⑤ 접두 최적화는 리터럴 첫 문자와 시작 고정(^)뿐이다. 입력이 매우 길고 시작 위치가 많은 패턴(예: `(?:ab)*c` 를 전체 탐색)은 이차 시간이라 단계 한도(기본 1억)에서 RegExpLimitException 이 난다.
  ⑥ ES2015 RegExpBuiltinExec 의 lastIndex 전진은 u 모드에서 코드 포인트 단위로 구현했다(ES2017 이후 동작).
- 다음 작업 번호: 이 라이브러리를 JS 값 모델에 연결하는 P14-03(RegExp 객체, String.prototype.match/replace/search/split, Symbol.match 등), P14-02(Date 객체), P14-01(Number/String 내장).
- 체크박스 후보: 없음. **라이브러리 부분 검증 완료**: P14-02(DateMath/DateParser/DateFormatter), P14-03(RegExp 엔진), P14-01 의 숫자/문자열 변환, P14-04 의 JSON 텍스트 도우미, P14-09 의 escape/unescape.
  (P12-05 의 정규식 리터럴 조기 오류는 `RegExpCompiler.validate(source, flags)` 를 렉서/파서가 호출하면 된다.)

## 공개 API 요약 (JS 연결 코드 작성자용)

| 패키지 | 클래스 | 용도 |
| --- | --- | --- |
| `js.num` | `NumberConversion` | `toString(double)`, `toString(double, radix)`, `toFixed`, `toExponential`, `toPrecision`, `stringToNumber`, `parseInt(String,int)`, `parseFloat`, `toInteger/toInt32/toUint32/toUint16/toLength`, `isInteger/isSafeInteger/sameValue/sameValueZero`; RangeError 는 `NumberRangeException` |
| `js.num` | `DecimalDigits`, `ExactDecimal` | 최단 왕복 숫자열, 정확한 10진 → double |
| `js.text` | `CharClass` | WhiteSpace/LineTerminator/식별자 분류 (렉서, `trim`, `\s` 가 공유) |
| `js.text` | `UriCodec`, `LegacyEscape` | encodeURI* / decodeURI* (`UriFormatException`=URIError), escape/unescape |
| `js.text` | `TextTransform`, `CodePoints` | normalize/대소문자/trim/repeat, codePointAt/fromCodePoint/fromCharCode/raw (`TextRangeException`=RangeError) |
| `js.text` | `JsonText`, `JsonTokenizer` | JSON.stringify 인용(ES2015/well-formed), JSON.parse 토큰화(`JsonSyntaxException`=SyntaxError), `validate` |
| `js.date` | `DateMath`, `DateParser`, `DateFormatter` | 시간값 연산/지역시간(TimeZone 주입), Date.parse, toString 계열/toISOString(`DateRangeException`=RangeError) |
| `js.regexp` | `RegExpCompiler.compile(src, flags)` → `RegExpProgram` | `exec(String,int)`(앞쪽 탐색)/`matchAt`(고정 위치) 은 `int[]` 캡처 위치(없으면 null), `groupCount()`, `getGroupName(i)`, 한도 `setStepLimit/setMaxBacktrackEntries`, 취소 `RegExpCancel`; 조기 오류는 `RegExpSyntaxException`(메시지 "Invalid regular expression: /src/: 사유"), `RegExpCompiler.validate`, 한도 초과는 `RegExpLimitException` |


## 2026-11-16 · P14-02(라이브러리 부분) · js.date 완료

- 현재 작업 번호: P14-02 (Date 라이브러리 부분)
- 완료한 내용: `DateMath`(Day/TimeWithinDay/DaysInYear/DayFromYear/YearFromTime/MonthFromTime/DateFromTime/WeekDay/Hour..ms/MakeTime/MakeDay/MakeDate/TimeClip,
  LocalTZA/LocalTime/UTC: 전환 이전 오프셋 규칙, 시간대 주입), `DateParser`(ES2015 ISO 형식 + toString/toUTCString/RFC 2822/미국식 등 대체 형식),
  `DateFormatter`(toString/toDateString/toTimeString/toUTCString/toISOString/toJSON 도우미/toLocale* 영어 대체), `DateRangeException`.
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/date/*`, `tests/src/org/duckdns/hjow/swingwebpanel/js/date/DateTest.java`
- 검증 결과: `scripts/pkgtest.sh jslibs ...` → date 8 사례 통과(고정 시간대 UTC/Seoul/New_York, DST 전환 틈/반복 시각, 무작위 1500 왕복, GregorianCalendar 와 3000 비교), sourcecheck 위반 0.
- 가정/미해결: 대체 해석기는 구현 정의(연도 없는 문자열은 NaN, 두 자리 연도 50 기준). 시간대 이름 "(KST)" 는 JRE 짧은 표시 이름이라 JRE/로캘에 따라 다를 수 있음. 오프셋 전환은 앞뒤 이틀 안에 한 번만 있다고 가정.
- 다음 작업 번호: js.regexp (P14-03)
- 체크박스 후보: 없음(JS Date 객체 연결 필요).

## 2026-11-16 · P14-01(text 부분) · js.text 완료

- 현재 작업 번호: P14-01 (문자열 도우미), P14-09 일부(Annex B escape/unescape)
- 완료한 내용: `UriCodec`(encodeURI/encodeURIComponent/decodeURI/decodeURIComponent, 엄격한 UTF-8 검사), `LegacyEscape`(escape/unescape),
  `TextTransform`(normalize, toUpperCase/toLowerCase(+Locale), trim/trimStart/trimEnd, repeat), `CodePoints`(codePointAt, fromCodePoint, fromCharCode, raw, isWellFormed),
  `JsonText`(ES2015 quote, ES2019 well-formed quote), `JsonTokenizer`(엄격한 JSON 토큰화기 + 반복식 validate), 예외 `UriFormatException`/`TextRangeException`/`JsonSyntaxException`.
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/text/*`, `tests/src/org/duckdns/hjow/swingwebpanel/js/text/*`
- 검증 결과: `scripts/pkgtest.sh jslibs ...` → text 14 사례 통과, sourcecheck(encoding,forbidden,javadoc,loops) 위반 0.
- 발견한 JRE 6 차이: `String.toLowerCase(Locale.ROOT)` 가 U+0130 을 "i" 로만 바꿈 → `TextTransform.toLowerCase` 가 "i"+U+0307 로 보정. 대문자 특수 매핑(ß, ﬁ, ŉ)과 어말 시그마는 JRE 6 에서 올바름.
- 미해결 사항: 유니코드 4.0 데이터(JRE 6)라 이후 문자는 대소문자/정규화/식별자 분류가 안 됨(문서화). `localeCompare` 는 미구현(Collator 정책 필요).
- 다음 작업 번호: js.date → js.regexp
- 체크박스 후보: 없음(JS 연결 객체가 필요).

## 2026-11-16 · P14-01(num 부분) · js.num 완료, js.text.CharClass

- 현재 작업 번호: P14-01 (숫자 변환 도우미), P12-04 의 숫자 변환 부분
- 완료한 내용:
  - `js.text.CharClass`: WhiteSpace/LineTerminator/숫자/ID_Start/ID_Continue/IdentifierStart/IdentifierPart 분류 (U+180E 는 공백에서 제외).
  - `js.num.DecimalDigits`: Burger-Dybvig 최단 왕복 숫자열(BigInteger 정확 연산).
  - `js.num.ExactDecimal`: 임의 정밀도 10진수/분수 → double (동점 짝수 반올림, 비정규 수, 넘침 처리).
  - `js.num.NumberConversion`: toString(10)/toString(radix, V8 알고리즘), toFixed, toExponential(+undefined), toPrecision(+undefined),
    StringToNumber(10/16/8/2진), parseInt, parseFloat, ToInteger/ToInt32/ToUint32/ToUint16/ToLength, isInteger/isSafeInteger/SameValue/SameValueZero.
  - `js.num.NumberRangeException`: RangeError 대응 예외.
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/num/*`, `src/org/duckdns/hjow/swingwebpanel/js/text/{package-info,CharClass}.java`, `tests/src/org/duckdns/hjow/swingwebpanel/js/num/NumberConversionTest.java`
- 검증 결과: `scripts/pkgtest.sh jslibs ...` → num 11 사례 통과(알려진 결과 + 20000 무작위 비트 왕복/최단성 + 3000 정확 전개 + 500 동점 경계). `tools/sourcecheck --only encoding,forbidden,javadoc,loops` 위반 0.
- 미해결 사항: toString(radix) 는 규격이 구현 정의로 둔 부분이라 V8 알고리즘 결과와의 일치만 확인(소수 진법 값 일부는 기억 기반 기대값). 
- 다음 작업 번호: js.text 나머지 → js.date → js.regexp
- 조정자 요청: 없음. (`src/org/duckdns/hjow/swingwebpanel/js/NumberText.java` 가 다른 작업자 소유로 존재함: 중복 구현이 되지 않도록 P12/P13 작업자가 `js.num.NumberConversion` 을 쓰도록 안내 바람.)
- 체크박스 후보: 없음(P14-01 은 JS 연결 객체가 있어야 완료). 라이브러리 부분 검증 완료: 숫자 변환.
