# BUILTIN-A 진행 기록 (P14-01 의 기본 객체 부분, P14-09 확인)

담당 경로: `src/org/duckdns/hjow/swingwebpanel/js/builtins/corelib/**`, `tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/corelib/**`, 이 문서.
진입점: `org.duckdns.hjow.swingwebpanel.js.builtins.corelib.CoreLibInstaller` (public, 인수 없는 생성자, `BuiltinInstaller`) — JS-CORE 의 `StandardBuiltins` 가 이름으로 불러온다.
설치 순서(클래스 안): Error 계열 → Object → Function.prototype 메서드 → Boolean → Symbol → 전역(NaN/Infinity/undefined/parseInt/parseFloat/isNaN/isFinite/URI) → Number → Math.
규격 가정: ECMA-262 6판 본문은 HTTP 403 으로 조회하지 못했다. ES2015 알고리즘은 작성자의 지식에 따랐고 클래스 Javadoc 에 가정을 적었다.
도구 주의: 도구 입력의 `\uXXXX` 는 실제 문자로 바뀐다(소스에서 특수 문자는 `(char) 0x....` 로).

## 시험 실행 방법

공식 명령 `scripts/pkgtest.sh bia "js/builtins/corelib" "js/builtins/corelib"` (JDK 6u45)이 JS-CORE 의 `StandardBuiltins` 와 설치기 클래스까지 컴파일하고 실행한다.
작업 초반에는 JS-CORE 코드가 미완성이라 컴파일이 깨질 수 있어, 그때는 `StandardBuiltins` 를 빈 스텁(스크래치 디렉터리, 저장소 밖)으로 대체한 비공개 스크립트로
corelib 만 컴파일/실행했다. 대부분의 시험은 `new JsRealm()` 에 `CoreLibInstaller` 만 설치한 realm(`LibEnv`)을 쓰므로 스텁과 무관하며,
`StandardRealmTest` 만 `JsRealm.create()`(완전한 표준 realm)를 쓴다.

---

## 작업 기록 (최신이 위)

### 2026-11-16 · P14-01(기본 객체) 시험 완료 · P14-09(속성/이름/length 표) 확인

```text
현재 작업 번호: P14-01 (Object/Function.prototype/Boolean/Symbol/Error/Number/Math/전역 함수 — 내 담당 부분 검증 완료), P14-09 (내 객체의 이름/length/속성 표 확인 완료)
완료한 내용: 시험 8개 클래스 63 사례를 추가했다(아래 표). 시험 중 발견해 고친 결함: Number.prototype.toLocaleString 이 0 근처 값에서 "0.000" 을 내던 문제
             (JRE 6 BigDecimal.stripTrailingZeros 가 0 을 처리하지 못함 → 0 은 따로 처리).
변경 파일: src/org/duckdns/hjow/swingwebpanel/js/builtins/corelib/{NumberLib,LibSupport,ObjectLib}.java (수정),
           tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/corelib/{LibEnv,ObjectLibTest,FunctionBooleanSymbolTest,ErrorLibTest,NumberGlobalTest,MathLibTest,
           PropertyAttributesTest,ScriptLevelTest,StandardRealmTest}.java
검증 결과: `scripts/pkgtest.sh bia "js/builtins/corelib" "js/builtins/corelib"` (JDK 6u45, 스텁 없이 공식 명령) → 70 passed, 0 failed, 1 skipped
           (기반 시설 7 + corelib 63 통과 + 1 건너뜀). tools/sourcecheck(encoding,forbidden,javadoc,loops) 0 위반.
  - ObjectLibTest 15: 생성자(new.target), 키 순서(정수→문자열→심볼), create/defineProperty(ies)(원자적 검증), assign(심볼/getter/엄격 실패), freeze/seal/preventExtensions
    와 판별(배열 포함), getOwnPropertyDescriptor 의 키 순서, get/setPrototypeOf(순환/비확장), is, prototype 메서드, toString 태그, __proto__ 및 레거시 접근자, ES2017 메서드 부재
  - FunctionBooleanSymbolTest 10: call/apply/bind(이름·length·Infinity·비숫자·바운드 생성자), toString, @@hasInstance, Boolean, Symbol(함수/레지스트리/잘 알려진 심볼 속성)
  - ErrorLibTest 4: Error 생성자/메시지 속성, 6개 NativeError 의 체인·이름·메시지, toString 조합, new.target 과 realm.createError
  - NumberGlobalTest 12: 상수, Number(), 정적 판별, toString(radix), toFixed, toExponential/toPrecision, toLocaleString, 수신 값 TypeError, parseInt/parseFloat, isNaN/isFinite,
    URI 함수(서로게이트, 과다 인코딩 오류), 전역 값 속성
  - MathLibTest 10: 상수/@@toStringTag, 반올림/부호(-0, round(0.49999999999999994)), clz32/imul/fround, 지수/로그(정확한 log2/log10), 삼각/쌍곡(직접 만든 역쌍곡),
    hypot(넘침/언더플로/Infinity>NaN), max/min(-0, 모든 인수 변환), pow 특수 경우, random, 인수 변환
  - PropertyAttributesTest 4: 표 기반으로 모든 객체의 함수 프로퍼티 집합(과 length/name/속성/비생성자 prototype 없음), 생성자 연결, 전역 프로퍼티 집합, __proto__ 접근자
  - ScriptLevelTest 6(+1 건너뜀): 실제 스크립트를 인터프리터로 실행(Object/Function/Boolean·Symbol/Error(class 상속)/Number·전역/Math). 각 사례는 새 realm
  - StandardRealmTest 2: `JsRealm.create()`(JS-CORE 의 최소 내장 객체 위에 이 설치기가 얹힌 실제 구성)에서 생성자 연결과 스크립트 12건
  - 건너뜀 1: ScriptLevelTest.testMinValueThroughEngineToString — 엔진의 `Ops.toString(Number.MIN_VALUE)` 가 4.9e-324 (JS-CORE 의 임시 `NumberText` 가 아직 js.num 으로 교체되지 않음).
    js.num 경로(`Number.prototype.toString`)는 NumberGlobalTest 가 "5e-324" 로 검증함.
미해결 사항:
  - JS-CORE 의 `NumberText` 를 `js.num.NumberConversion` 으로 교체해야 엔진 전체(`Ops.toString`, 템플릿 리터럴, 문자열 연결)에서 최단 왕복 표현이 된다(위 건너뜀 해소).
  - `Function` 생성자/`eval`, `escape`/`unescape`, `String`/`Array` 라이브러리는 이 패키지가 아니다. 시험 환경(LibEnv)은 맨 realm 에 `Function` 자리표시 생성자만 만들어 경로 조회를 돕는다.
  - 규격 본문(ECMA-262 6판)을 조회할 수 없어 알고리즘은 작성자의 지식 기준이다: toLocaleString(en-US 근사)은 구현 정의, Object.prototype.toString 의 내장 태그는 className() 기준이다.
  - Date/RegExp 객체 구현자는 `className()` 이 "Date"/"RegExp" 를 돌려주도록 해야 `Object.prototype.toString` 이 `[object Date]`/`[object RegExp]` 를 낸다.
다음 작업 번호: 내 담당 범위 끝(P14-01 의 Object/Function/Boolean/Symbol/Error/Number/Math/전역 부분). P14-01 전체는 String/Array 설치기(stringarray) 완료 후 조정자가 체크.
조정자 요청: (1) JS-CORE 의 NumberText 를 js.num 으로 교체 (2) 전체 P14-09(모든 내장 객체의 이름/length/속성 표)는 PropertyAttributesTest 와 같은 방식으로 다른 설치기에도 적용할 것.
체크박스 후보: 없음 — P14-01 은 String/Array 까지 있어야 완료, P14-09 는 모든 내장 메서드가 있어야 완료. "내 부분 검증 완료": Object, Function.prototype, Boolean, Symbol, Error 계열, Number, Math, 전역 함수/값.
```

### 2026-11-16 · P14-01(기본 객체 구현) + Object 시험

```text
현재 작업 번호: P14-01 (Object/Function.prototype/Boolean/Symbol/Error/Number/Math/전역 함수 구현), P14-09 (속성/이름/length 표 시험은 다음 단계)
완료한 내용: corelib 패키지 전체 구현(아래 표). 시험은 Object 부분(ObjectLibTest 15 사례)까지 작성하고 통과.
변경 파일: src/org/duckdns/hjow/swingwebpanel/js/builtins/corelib/{CoreLibInstaller,LibSupport,ObjectLib,FunctionLib,BooleanLib,SymbolLib,ErrorLib,NumberLib,MathLib,GlobalLib,package-info}.java,
           tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/corelib/{LibEnv,ObjectLibTest}.java
검증 결과: (스텁 StandardBuiltins 로) JDK 6u45 → 22 passed(기반 시설 6 + ObjectLibTest 15 + JarContentTest 건너뜀 없음), 0 failed. tools/sourcecheck 0 위반.
미해결 사항: 나머지 클래스의 시험(Function/Boolean/Symbol/Error/Number/Math/전역/속성 표)은 다음 작업 단위.
다음 작업 번호: P14-01 시험 확대, P14-09 표 시험
조정자 요청: 없음
체크박스 후보: 없음
```

### 구현 요약

| 클래스 | 내용 |
| --- | --- |
| `ObjectLib` | `Object` 생성자(call/construct, new.target 프로토타입), 정적: assign, create, defineProperties, defineProperty, freeze, getOwnPropertyDescriptor, getOwnPropertyNames, getOwnPropertySymbols, getPrototypeOf, is, isExtensible, isFrozen, isSealed, keys, preventExtensions, seal, setPrototypeOf. 프로토타입: hasOwnProperty, isPrototypeOf, propertyIsEnumerable, toLocaleString, toString(@@toStringTag), valueOf. 부속서 B: `__proto__` 접근자, `__defineGetter__`, `__defineSetter__`, `__lookupGetter__`, `__lookupSetter__`. ES2017 메서드(entries/values/getOwnPropertyDescriptors)는 의도적으로 없음. 기술자 변환은 JS-CORE 의 `DescriptorOps` 를 쓴다 |
| `FunctionLib` | `Function.prototype` 의 apply, bind(이름 `bound x`, length 조정, Infinity/비숫자 length 처리), call, toString, `[Symbol.hasInstance]`(writable/enumerable/configurable 모두 false). `Function` 생성자/`eval` 은 JS-CORE(P13-11) |
| `BooleanLib` | `Boolean`(call=ToBoolean, construct=래퍼), `toString`, `valueOf` |
| `SymbolLib` | `Symbol`(호출만 가능, `new Symbol` 은 TypeError), `Symbol.for/keyFor`, 11개 잘 알려진 심볼(읽기 전용), `toString`, `valueOf`, `[Symbol.toPrimitive]`, `[Symbol.toStringTag]` |
| `ErrorLib` | `Error` 와 NativeError 6종 생성자(`new` 없이도 동작, `message` 는 자체 비열거 프로퍼티, new.target 프로토타입), 프로토타입의 `name`/`message`, `Error.prototype.toString` |
| `NumberLib` | `Number` 상수 8개, isFinite/isInteger/isNaN/isSafeInteger, `Number.parseInt === parseInt`, `Number.parseFloat === parseFloat`, `toString(radix)`, `toFixed`, `toExponential`, `toPrecision`, `toLocaleString`(en-US 형식 근사), `valueOf` — 서식은 모두 `js.num.NumberConversion` |
| `MathLib` | 상수 8개, 함수 35개(ES2015 신규 포함), `@@toStringTag`. JRE 6 에 없는 acosh/asinh/atanh/log2 와 hypot/round 는 수치적으로 조심한 공식으로 구현 |
| `GlobalLib` | NaN/Infinity/undefined, parseInt/parseFloat/isNaN/isFinite, encodeURI/encodeURIComponent/decodeURI/decodeURIComponent(`js.text.UriCodec`, 오류는 URIError). `escape`/`unescape`(부속서 B)는 메타 라이브러리 담당 |

### 설계 메모와 가정

- 생성자는 `LibSupport.defineConstructor` 가 `prototype`(모두 false), `prototype.constructor`(writable, 비열거, configurable), 전역 바인딩, `%이름%` 내장 객체 등록을 한 번에 한다.
  JS-CORE 의 최소 구현이 이미 만든 전역 `Object`/`Error`/`Symbol` 등은 **새 생성자로 교체**한다(프로토타입 객체는 realm 의 것을 그대로 쓴다).
- `Number.prototype.toLocaleString` 은 ES2015 에서 구현 정의다. en-US 형식(쉼표, 소수 최대 3자리, 0에서 먼 쪽 반올림, `∞`)으로 만들었다.
- `Object.prototype.toString` 의 내장 태그는 `JsObject.className()` 이 Arguments/Error/Boolean/Number/String/Date/RegExp 이면 그 이름, 배열은 `Ops.isArray`, 호출 가능이면 Function 이다.
  Date/RegExp 객체를 만드는 다른 작업자는 `className()` 이 `"Date"`/`"RegExp"` 를 돌려주게 해야 한다.
