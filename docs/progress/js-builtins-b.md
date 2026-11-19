# BUILTIN-B 진행 기록 (P14-01 String·Array, P14-09 확인)

담당 경로: `src/org/duckdns/hjow/swingwebpanel/js/builtins/stringarray/**`, `tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/stringarray/**`, 이 문서.
진입점: `org.duckdns.hjow.swingwebpanel.js.builtins.stringarray.StringArrayInstaller` (JS-CORE 의 `StandardBuiltins` 가 이름으로 적재: corelib 다음, datajson 앞).
빌드/시험: 표준 realm 전체(코어, corelib, RegExp 등)와 함께 실행하려면 `scripts/pkgtest.sh bib "js/builtins" "js/builtins/stringarray"` (JDK 6, `-sourcepath` 로 필요한 코어 클래스를 컴파일하고 `js/builtins` 아래 모든 설치기를 함께 컴파일해야 `JsRealm.create()` 의 이름 적재가 동작한다).

## 작업 기록 (최신이 위)

### 2026-11-16 · P14-01 (String, Array) · 스크립트 수준 시험과 표준 realm 확인

```text
현재 작업 번호: P14-01 의 String/Array 부분 검증 완료, P14-09 의 String/Array 확인 완료
완료한 내용: `ScriptLevelTest`(JavaScript 소스를 프로젝트 인터프리터 `JsRealm.create()` + `evaluateScript` 로 평가) 추가. 표준 realm 의 String.prototype/Array.prototype 프로퍼티 집합이 문서화한 표와
             정확히 일치함을 확인(다른 묶음이 예상 밖 프로퍼티를 더하거나 남기지 않음).
변경 파일: tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/stringarray/{ScriptLevelTest,PropertyTableTest}.java, docs/progress/js-builtins-b.md
검증 결과 (JDK 6u45, 2026-11-16, 작업 트리 코어 + `js/builtins` 전체 설치기 컴파일): `scripts/pkgtest.sh bib "js/builtins" "js/builtins/stringarray"` → 75 passed, 0 failed, 0 skipped
  (내 시험 68건 + 기반 시설 7건). tools/sourcecheck → 0 violations (16 파일).
  - 스크립트 수준 시험(실제 인터프리터): String 기본 메서드, 유니코드(서로게이트, normalize, 대소문자, 한글, trim 의 공백 범위), 오류 종류(RangeError/TypeError 와 e.name), 정규식과 함께 쓰는
    replace/match/search/split(실제 RegExp 내장 객체, 문자열 인수 match 포함), Array 기본 메서드, 구멍, 길이 한계(4294967295), 이터레이션 객체(entries/keys/values, toString 태그, @@iterator 동일성),
    정렬 관찰(안정성, 비교 함수 예외 시 불변, undefined/구멍), species(생성자 함수), ES2015 구문 연동(spread, for-of, 구조 분해, 화살표 함수, rest 인수, 템플릿 String.raw, class extends Array 의
    species/Array.from/Array.of 상속).
  - 불완전한 realm(예: corelib 설치기를 컴파일하지 않은 실행)에서는 `ScriptLevelTest` 가 이유를 밝히고 건너뛴다(`setUp` 의 SkipException).
미해결 사항:
  - ES2015 본문(403)을 읽지 못해 알고리즘은 지식 기반이다(위 "가정과 결정").
  - 스크립트 수준에서 확인하지 못한 것: Proxy 수신자(`Array.prototype.* .call(new Proxy(...))`), typed array 수신자(다른 작업자 영역이 준비된 뒤 재확인 필요).
  - JS-LIBS `CharClass.isWhiteSpace`/`TextTransform.trim*` 결함(U+200B/U+180E) 조정자 요청은 그대로 유효하다.
다음 작업 번호: 없음(담당 범위 완료). 후속: P14-10/P14-11 의 ES2015 적합성 목록에 이 묶음의 사례 수를 연결, Proxy/typed array 수신자 재확인.
조정자 요청: 위 항목 (CharClass 결함, %ArrayIteratorPrototype% 교체 사실, Support.poll 과 ExecutionLimits 연결)
체크박스 후보: 없음 (P14-01/P14-09 는 모든 객체 묶음이 끝나야 완료. String/Array 부분은 구현·검증 완료로 표시: "my part verified")
```

### 2026-11-16 · P14-01 (String, Array) · Java 수준 구현과 시험

```text
현재 작업 번호: P14-01 의 String/Array 부분, P14-09 의 String/Array 확인
완료한 내용:
  - String(21.1): 생성자(호출/생성, 심볼 규칙), fromCharCode, fromCodePoint(RangeError), raw, 프로토타입 전체(charAt, charCodeAt, codePointAt, concat, endsWith, includes,
    indexOf, lastIndexOf, localeCompare, match, normalize, repeat, replace, search, slice, split, startsWith, substring, toLocale*Case, to*Case, toString, trim, valueOf, @@iterator),
    부속서 B(substr, anchor, big, blink, bold, fixed, fontcolor, fontsize, italics, link, small, strike, sub, sup), String Iterator(%StringIteratorPrototype%, 코드 포인트 단위)
  - Array(22.1): 생성자(길이/요소, RangeError, new.target 프로토타입), isArray, of, from(이터러블/유사 배열/매핑/이터레이터 닫기/사용자 생성자), @@species,
    ArraySpeciesCreate, 프로토타입 전체(concat+@@isConcatSpreadable, copyWithin, entries, every, fill, filter, find, findIndex, forEach, indexOf, join, keys, lastIndexOf, map, pop, push,
    reduce, reduceRight, reverse, shift, slice, some, sort(안정 병합 정렬), splice, toLocaleString, toString, unshift, values, @@iterator(=values 같은 함수 객체), @@unscopables),
    Array Iterator(%ArrayIteratorPrototype%)
  - 문서화한 ES2015 이후 확장(별도 목록, 아래 "확장 목록"): String.prototype.trimStart/trimEnd(+별칭 trimLeft/trimRight), padStart/padEnd, Array.prototype.includes
  - 공개 도우미: `Substitution.expand(matched, str, position, captures, replacement)` (GetSubstitution; RegExp 내장 객체가 같은 구현을 쓸 수 있음),
    `ArrayIteratorObject.create(realm, obj, KEYS|VALUES|ENTRIES)` / `StringIteratorObject.create(realm, string)` (타입 배열 등 다른 내장 객체가 같은 이터레이터 프로토타입을 쓰게 하는 팩터리)
변경 파일: src/org/duckdns/hjow/swingwebpanel/js/builtins/stringarray/{package-info,StringArrayInstaller,Support,Substitution,StringBuiltins,StringIteratorObject,ArrayBuiltins,ArrayMethods,ArraySorter,ArrayIteratorObject}.java,
           tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/stringarray/{Env,StringBuiltinsTest,ArrayBuiltinsTest,ArrayMethodsTest,PropertyTableTest}.java
검증 결과 (JDK 6u45, 2026-11-16, 커밋된 코어 스냅샷 기준): `build/bib-run.sh` → 64 passed, 0 failed, 0 skipped (내 시험 57건 + 기반 시설 7건).
  tools/sourcecheck (encoding, forbidden, javadoc, loops) → 0 violations (15 파일).
  - String 시험 22건: 생성자/심볼, fromCharCode/fromCodePoint/raw, 문자 접근과 서로게이트, 자르기, 검색, 정규식 인수 거부(IsRegExp), localeCompare, normalize, repeat, 대소문자(ß, 마지막 시그마, İ),
    trim(WhiteSpace/LineTerminator 전체, U+200B 비포함), split(문자열/제한/빈 구분자/서로게이트), replace(패턴 $$ $& $` $' $n, 함수 치환, 예외 전파), Substitution(캡처), 심볼 메서드 위임(match/search/replace/split),
    문자열 인수의 RegExpCreate(%RegExp% 가상 생성자), HTML 메서드, 일반 this 와 null/undefined 거부, String 이터레이터, pad, 길이 제한
  - Array 시험: 생성자 형태/RangeError/new.target, isArray, of, from(이터레이터 닫기, 사용자 생성자), species(생성자, null, 비생성자), push/pop/shift/unshift/splice/reverse(구멍 4조합), indexOf 계열(NaN, -0, 구멍),
    join/toString/toLocaleString(순환), fill/copyWithin(겹침, 구멍), 콜백 메서드(구멍 건너뜀, 추가 요소 비방문), find 계열, reduce 계열, sort(기본/숫자/undefined/구멍/안정성/NaN/예외 시 불변/5000개),
    반복자/@@unscopables, 일반 수신자와 null/undefined 거부, 한계값(2^53-1 TypeError, 인터럽트로 중단), 희소 배열
  - PropertyTableTest(P14-09): 설치한 모든 함수의 name/length/속성(writable, 비열거, configurable), 함수 객체 자체의 name/length 속성, 생성자 없음/프로토타입 없음, 정확한 프로퍼티 집합(예상 밖 프로퍼티 없음),
    생성자 length/prototype 속성, Array[@@species] 접근자, 이터레이터 프로토타입
미해결 사항(정직한 기록):
  - (해소됨: 스크립트 수준 시험은 위 항목에서 실행했다.)
  - ES2015 본문(ecma-international)이 HTTP 403 이라 알고리즘은 지식 기반이다. 가정은 이 문서와 Javadoc 에 적었다(아래 "가정과 결정").
  - 실제 RegExp 내장 객체(다른 작업자)와의 연동은 가짜 `%RegExp%` 생성자로만 시험했다. 실제 연동 시험은 RegExp 가 설치된 realm 에서 따로 필요하다.
  - 비교 함수 호출 횟수/순서는 구현 정의이며 ToString 은 요소마다 한 번만 한다(규격은 비교마다). `localeCompare` 는 JRE 루트 콜레이터라 ICU 와 결과가 다를 수 있다.
  - 매우 긴 희소 배열(예: 길이 2^32-1)에서 모든 인덱스를 방문하는 메서드는 선형 시간이다(인터럽트로 중단 가능). 희소 최적화는 하지 않았다.
  - JS-LIBS 의 `org.duckdns.hjow.swingwebpanel.js.text.CharClass.isWhiteSpace` 는 JRE 6 에서 U+200B/U+180E 를 공백으로 분류한다(코어의 `JsChars` 는 명시 목록으로 이미 피함).
    `TextTransform.trim/trimStart/trimEnd` 도 같은 결함이 있어 이 패키지는 `JsChars.isSpace` 로 직접 구현했다(`Support.trim`). → 조정자 요청(JS-LIBS 쪽 수정 필요).
다음 작업 번호: 스크립트 수준 시험(인터프리터 준비 후), RegExp 설치된 realm 에서의 String 연동 시험, P14-09 최종 확인
조정자 요청:
  - JS-LIBS `CharClass.isWhiteSpace`/`TextTransform.trim*` 의 U+200B, U+180E 결함(위) 수정 또는 `JsChars` 사용.
  - 코어의 최소 `%ArrayIteratorPrototype%` 를 이 패키지가 **교체**한다: 이 패키지가 만든 이터레이터 객체만 이 프로토타입의 `next` 가 받기 때문이다. 코어가 자체 배열 이터레이터 객체를
    만든다면 `ArrayIteratorObject.create(realm, obj, kind)` 를 쓰게 해야 한다(타입 배열/arguments 도 같은 팩터리 사용).
  - `Support.poll()` 은 현재 스레드 인터럽트만 확인한다. `JsRealm` 이 `ExecutionLimits` 를 노출하면 `Support.poll()` 한 곳에서 `limits.tick()` 을 호출하도록 연결하면 된다.
체크박스 후보: 없음 (P14-01/P14-09 는 모든 내장 객체 묶음이 있어야 완료. String/Array 부분의 Java 수준 검증만 끝남)
```

## 가정과 결정 (ES2015 본문을 읽지 못해 지식에 따른 부분)

- **GetSubstitution `$nn`**: 두 자리 `nn` 이 1..m(캡처 수)이면 그 캡처, 아니면 `$n`(한 자리) 뒤에 숫자 문자가 이어진 것으로 해석(웹 호환). 없는 캡처 참조는 `$` 포함 그대로 둔다.
- **`String.prototype.match/search` 의 문자열 인수**: 규격의 RegExpCreate(P, undefined) 에 해당하며 realm 의 `%RegExp%`(또는 전역 `RegExp`) 생성자를 쓴다. 없으면 TypeError("RegExp is not available").
- **IsRegExp**: 객체이고 `@@match` 가 정의되어 있으면 그 불리언 값, 아니면 `className() == "RegExp"`.
- **`Array.prototype.join/toString/toLocaleString` 의 순환**: 이미 join 중인 객체는 빈 문자열(스택 오버플로를 피하기 위한 실제 엔진 동작).
- **`Array.prototype.sort`**: 있는 요소를 모아(undefined 는 개수만) 안정 병합 정렬한 뒤 정렬 값 → undefined → 남은 구멍 삭제 순으로 쓴다. 비교 함수 예외 시 배열은 바뀌지 않는다.
  `toString` 은 요소마다 한 번만 호출한다.
- **길이 상한**: 늘어난 길이가 2^53-1 을 넘으면 TypeError(규격 후속 판과 같음). 문자열은 `Support.MAX_STRING_LENGTH`(= `ExecutionLimits.DEFAULT_MAX_STRING_LENGTH`, 33,554,432 코드 단위)를 넘으면 RangeError("Invalid string length").
- **`localeCompare`**: `Collator.getInstance(Locale.ROOT)`, 정규 분해, TERTIARY 강도. 결과는 -1/0/1.
- **`toLocaleUpperCase/LowerCase`**: 로케일에 독립적인 `toUpperCase/toLowerCase` 와 같다.
- **`%ArrayIteratorPrototype%`**: 코어가 등록한 것이 있어도 이 패키지가 새로 만들어 교체한다(위 조정자 요청 참조). `%StringIteratorPrototype%`, `%ArrayProto_values%`, `%String%`, `%Array%` 도 등록한다.
- **`Array` 생성자**: 코어의 최소 생성자가 있으면 교체한다(전역 `Array` 와 `%Array%`).
- **인터럽트**: 길이에 비례하는 반복은 4096 번마다 `Thread.isInterrupted()` 를 보고 `ScriptAbortedException(CANCELLED)` 를 던진다.

## 확장 목록 (ES2015 에 없는 것, ES6 완료 판정과 무관)

`String.prototype.trimStart`, `trimEnd`(별칭 `trimLeft`, `trimRight` 는 같은 함수 객체), `padStart`, `padEnd`(ES2017/2019), `Array.prototype.includes`(ES2016).

## 스크립트 수준 시험

`ScriptLevelTest` 참조(위 작업 기록). 인터프리터와 표준 realm 은 작업 트리의 JS-CORE 구현을 쓰며, 시험은 `JsRealm.create()` 와 `evaluateScript(String, String)` 만 사용한다.

## 의존성 주의

`ScriptLevelTest` 는 JS-CORE 가 작성한 `JsRealm.create()` 와 `JsRealm.evaluateScript(String, String)` 에 의존한다. 이 둘이 커밋되기 전의 저장소 상태에서는 이 시험 클래스가 컴파일되지 않는다
(JS-CORE 의 P13 커밋이 들어오면 해소된다). 나머지 시험(Java 수준)은 코어의 커밋된 API 만 쓴다.
