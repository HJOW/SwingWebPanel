# ES2015 적합성 사례 목록과 결과 (P14-10 사례 목록, P14-11 보고)

> **2026-11-25 갱신 메모**: 프로젝트의 최종 JavaScript 규격 목표가 ES6(ES2015)에서 **ES2026(ECMA-262 제17판, `docs/es2026/`) 및 이전 버전 ES 호환**으로 상향되었다. 이 문서는 1차 기반 단계(P14, 2026-11-16)의 ES2015 적합성 기록으로 보존하며, ES2016~ES2022(`P28`)와 ES2023~ES2026·하위 호환(`P29`)을 포함한 전체 내장 모양 표(`ConformanceTable`) 및 적합성 확장은 `docs/FEATURE_MATRIX.md` 7절과 `P29-10`에서 관리한다.

작성: BUILTIN-D, 2026-11-16. 기준 규격: ECMA-262 6판(ES2015). 규격 본문은 조회가 HTTP 403 이어서 읽지 못했고 사례의 기대 결과는 작성자의 지식과
브라우저(V8/SpiderMonkey) 동작에 따랐다(우회 시도 없음). 외부 적합성 자료(test262 등)는 쓰지 않았다. 기대 결과가 규격 본문과 다른 곳이 발견되면
이 문서의 해당 행과 `ConformanceTable`/`Es2015LanguageProbeTest` 의 기대 값을 고친다(그 경우 틀린 쪽은 표이지 라이브러리가 아닐 수 있다).

## 1. 사례 목록은 어디에 있고 어떻게 실행하는가

| 종류 | 위치 | 내용 |
| --- | --- | --- |
| 모양 표 (510 항목) | `tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/meta/ConformanceTable.java` + `MetaConformanceTest` | 18~26장과 부속서 B.2 의 전역 값/함수/생성자/프로토타입/접근자/심볼 키의 존재, 종류, 함수 `name`, `length`, 속성(writable/enumerable/configurable), 별칭 동일성(`Array.prototype[@@iterator] === values` 등), 프로토타입 체인, `@@toStringTag` |
| 동작 사례 (77건) | `Es2015LanguageProbeTest` | 라이브러리와 언어가 만나는 지점의 스크립트 사례: 부속서 B 구문, `with`/`Symbol.unscopables`, 잘 알려진 심볼을 쓰는 연산자, 제너레이터/이터레이터 닫기, 클래스/상속, eval/Function, Proxy를 다른 라이브러리가 쓰는 경우, Promise 조합 |
| 스크립트 수준 (5 메서드, 스크립트 38개 + 추적기 1건) | `ScriptLevelMetaTest` | Proxy/Reflect/Promise/escape 를 페이지가 쓰는 방식 그대로 인터프리터로 실행 |
| Java 수준 | `ProxyTest`, `ProxyInvariantTest`, `ReflectTest`, `PromiseTest`, `LegacyBuiltinsTest` (58 메서드) | 불변 조건, 작업 큐 순서, 추적기 등 |
| 다른 작업자 | `docs/progress/js-builtins-{a,b,c,e}.md` 와 `tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/{corelib,stringarray,datajson,typed}` | 각 내장 객체의 알고리즘 사례 |

모양 표가 실제로 어긋남을 잡는지는 `testWalkDetectsPlantedDeviations` 가 확인한다(없는 함수, 틀린 length, 잘못된 속성, 잘못된 프로토타입을 심어 정확히 그 4건만
보고되는지). 허용 목록(`PENDING`)은 두 사례 클래스에 있으며, 목록에 없는 어긋남은 시험을 실패시키고 더 이상 필요 없는 행은 `[stale ...]` 로 출력된다.

실행:

```text
sh scripts/pkgtest.sh <이름> "js" "js/builtins/meta"            # 이 문서의 시험 (내 몫)
sh scripts/pkgtest.sh <이름> "js" "js"                          # 엔진 전체 시험
```

**주의(함정)**: 소스 패키지 인수에 `js` 전체를 줘야 한다. `StandardBuiltins` 가 선택적 설치기(`CoreLibInstaller`, `StringArrayInstaller`, `DataLibInstaller`,
`TypedLibInstaller`, `MetaLibInstaller`)를 `Class.forName` 으로 적재하므로, `-sourcepath` 만으로는 그 클래스가 컴파일되지 않아 `JsRealm.create()` 가
조용히 Math/Date/Map/타입 배열 없는 realm 을 만든다(`js/builtins/meta` 만 주면 표 항목의 약 2/3 가 없다고 나온다).

## 2. 결과 (JDK 6u45, 2026-11-16, 작업 트리 전체 컴파일)

| 범위 | 통과 | 실패 | 건너뜀 | 비고 |
| --- | --- | --- | --- | --- |
| 이 작업자의 시험 (`js/builtins/meta`) | 71 (+ 기반 시설 7) | 0 | 0 | 모양 표 510 항목 모두 통과, 동작 사례 77 중 75 통과 + 2 보류(아래) |
| 엔진 전체 (`js`) | 570 | 1 | 1 | 실패 1건은 `interp.LimitsTest.testStringLengthLimit`(JS-CORE 가 작업 중인 P13-12 시험, 이 문서의 범위 밖), 건너뜀 1건은 `corelib.ScriptLevelTest.testMinValueThroughEngineToString`(코어의 임시 `NumberText`, 이유가 시험에 적혀 있음) |

모양 표 510 항목은 18장 전역 값/함수/생성자, 19~26장의 모든 생성자·정적 메서드·프로토타입 메서드·접근자·심볼 키 프로퍼티, 부속서 B.2 의 확장, 프로토타입 체인으로 이루어진다(`ConformanceTable.LINES`).

## 3. 절별 사례 목록과 상태

상태: **검증**(구현되어 있고 위 시험이 통과), **부분**(아래 차이 목록 참조), **보류**(허용 목록에 있음), **코어**(JS-CORE 영역으로 이 목록의 대상이 아님).

| ES2015 절 | 기능 | 모양 표 | 동작 사례 | 상태 |
| --- | --- | --- | --- | --- |
| 18.1 | NaN, Infinity, undefined | 통과 | `corelib.NumberGlobalTest.testGlobalValueProperties` | 검증 |
| 18.2 | eval, isFinite, isNaN, parseFloat, parseInt, URI 함수 | 통과 | `NumberGlobalTest.testParseFunctions/testIsNaNAndIsFinite/testUriFunctions`, `text.UriCodecTest`, probe `18.2.1/*` | 검증 (eval 3사례, Function 생성자 1사례) |
| 18.3~18.4 | 전역 생성자/Math/JSON/Reflect 의 속성 | 통과 | `PropertyAttributesTest.testGlobalObjectProperties`, `MetaConformanceTest` | 검증 |
| 19.1 | Object (+ B.2.2 `__proto__`, `__defineGetter__` 계열) | 통과 | `ObjectLibTest` 15, probe `9.1/*`, `B.3.1/*` | 검증 |
| 19.2 | Function (`call/apply/bind/toString/@@hasInstance`, 생성자) | 통과 | `FunctionBooleanSymbolTest` 10, probe `19.2.*`, `14.3/function-name-inference` | 부분: `Function.prototype.caller/arguments` 제한 접근자 없음 → 보류 |
| 19.3 | Boolean | 통과 | `FunctionBooleanSymbolTest.testBoolean` | 검증 |
| 19.4 | Symbol, 심볼 레지스트리, 잘 알려진 심볼 | 통과 | `FunctionBooleanSymbolTest.testSymbolFunction/testSymbolRegistryAndWellKnown`, probe `7.1.1/*`, `12.9.4/*`, `22.1.3.1/*`, `19.1.3.6/*` | 검증 |
| 19.5 | Error 와 NativeError 6종 | 통과 | `ErrorLibTest` 4, probe `14.5/extends-error` | 검증 |
| 20.1 | Number | 통과 | `NumberGlobalTest` 12, `num.NumberConversionTest` 11, probe `7.1.3.1/*` | 검증 |
| 20.2 | Math (함수 35개, 상수 8개) | 통과 | `MathLibTest` 10 | 검증 |
| 20.3 | Date (+ B.2.4 `getYear/setYear/toGMTString`) | 통과 | `datajson.DateTest` 13, `date.DateTest` 8, probe `20.3/*` | 검증 (차이 5 참조) |
| 21.1 | String (+ B.2.3 HTML 메서드, `substr`) | 통과 | `StringBuiltinsTest` 23, `PropertyTableTest`, `stringarray.ScriptLevelTest` | 검증 |
| 21.2 | RegExp (+ B.2.5 `compile`, B.1.4 확장 구문) | 통과 | `datajson.RegExpTest` 16, `regexp.RegExpTest` 15, `RegExpFuzzTest` 2, probe `B.1.4/*`, `21.2/*` | 부분: 차이 6 참조 |
| 22.1 | Array | 통과 | `ArrayBuiltinsTest` 10, `ArrayMethodsTest` 20, probe `9.4.2/*` | 검증 |
| 22.2 | %TypedArray%, 9개 생성자 | 통과 | `typed.*` 82 사례(`TypedArrayExoticTest` 등) | 부분: 차이 1~3 참조 |
| 23.1~23.4 | Map/Set/WeakMap/WeakSet | 통과 | `CollectionTest` 11, `datajson.ScriptLevelTest.testMapAndSet*` | 부분: 차이 7 참조 |
| 24.1~24.2 | ArrayBuffer, DataView | 통과 | `ArrayBufferTest` 10, `DataViewTest` 8 | 부분: 차이 1 참조 |
| 24.3 | JSON | 통과 | `JsonTest` 12, `text.JsonTextTest` 5 | 검증 |
| 25.1 | 이터레이션 인터페이스, %IteratorPrototype%, 배열/문자열/Map/Set 이터레이터 | 통과 | `IteratorHelperTest`, 배열/문자열/Map/Set 시험, probe `13.7.5/*`, `14.4/*` | 검증 |
| 25.2~25.3 | GeneratorFunction, 제너레이터 객체(next/return/throw) | 통과 | `interp.GeneratorTest` 7, probe `25.3/*`, `14.4/*` | 검증 |
| 25.4 | Promise (생성자, 해결 함수, then/catch, all/race/resolve/reject, @@species, 작업 큐) | 통과 | `PromiseTest` 19, `ScriptLevelMetaTest.testPromiseScripts`(13 스크립트), `testRejectionTrackerFromScript`, probe `25.4/*` | 검증 (큐를 브라우저 이벤트 루프에 연결하는 일은 P15-06) |
| 26.1 | Reflect (14함수, 그중 `enumerate` 는 ES2015 전용) | 통과 | `ReflectTest` 8, `ScriptLevelMetaTest.testReflectScripts`(9) | 검증 |
| 26.2 | Proxy (14 trap(`enumerate` 포함), 불변 조건, `Proxy.revocable`) | 통과 | `ProxyTest` 15, `ProxyInvariantTest` 12, `ScriptLevelMetaTest.testProxyScripts`(13), probe `26.2/*` | 부분: 차이 8 참조 |
| B.1 | 구문 확장(8진 리터럴/이스케이프, HTML 형태 주석, RegExp 확장 원자) | — | probe `B.1.1`~`B.1.4` | 검증 |
| B.2 | 내장 객체 확장(escape/unescape, `substr`, HTML 메서드, Date, RegExp.compile, `__proto__` 접근자와 `__define/lookup*__`) | 통과 | `LegacyBuiltinsTest` 4 외 위 시험 | 검증 |
| B.3 | 그 밖의 웹 호환 규칙(`__proto__` 리터럴, 라벨 함수, 블록/if 안의 함수, catch 의 var, `arguments.callee`, sloppy this, 암시적 전역) | — | probe `B.3/*`, `B.3.1`~`B.3.5` | 검증 |
| 6~15 | 언어 의미(타입, 연산자, 구문, 함수, 클래스, 모듈, 스크립트) | — | JS-CORE: `parser.*` 39, `interp.*`(ModuleTest 포함), `resources/interp/*.js`; 이 문서의 probe 의 7.1, 12.x, 13.x, 14.x 사례 | **코어** (이 목록은 경계 사례만 추가 확인) |

## 4. ES2015 본문과 다르거나 의도적으로 다르게 한 점 (브라우저 호환 선택 포함)

| # | 영역 | 차이 | 기록한 곳 |
| --- | --- | --- | --- |
| 1 | ArrayBuffer/DataView/형식화 배열 | 길이/위치 인수는 ES2017 `ToIndex`(소수 버림, NaN→0). ES2015 는 RangeError. 최대 크기 `Integer.MAX_VALUE-8` | js-builtins-e.md |
| 2 | 형식화 배열 | 범위 밖 쓰기는 오류 없이 무시, 분리된 버퍼의 요소 읽기는 undefined(ES2017+ 와 브라우저) | js-builtins-e.md |
| 3 | 형식화 배열 이터레이터 | `%ArrayIteratorPrototype%` 를 공유하지 않고 `%TypedArrayIteratorPrototype%` 를 쓴다(관찰 가능: `Object.getPrototypeOf(ta.values()) !== Object.getPrototypeOf([].values())`) | js-builtins-e.md |
| 4 | Array.prototype.sort | 비교 횟수/순서는 구현 정의, 안정 병합 정렬, 예외 시 배열 불변 | js-builtins-b.md |
| 5 | Date | `Date.UTC(year)` 는 월 생략 시 0(브라우저). 지역 시간대 이름은 JRE 의존 | js-builtins-c.md |
| 6 | RegExp / String | `$nn` 범위 밖은 `$n` 으로 되돌림(브라우저), 이름 있는 그룹/후방 탐색/`\p{}` 는 ES2015 밖이라 없음, `RegExp.$1` 등 정적 속성 없음, `dotAll` 확장 | js-builtins-c.md |
| 7 | WeakMap/WeakSet | 키는 약하지만 값은 맵이 강하게 쥔다(에페메론 아님: 값이 키를 참조하면 수거되지 않음) | js-builtins-c.md |
| 8 | Proxy | ownKeys 중복 키 TypeError 는 ES2017 의 엄격한 규칙, `[[GetOwnProperty]]` 의 writable 불변 조건(ES2020)·`[[Delete]]` 의 확장 불가 검사는 적용하지 않음. for-in 은 인터프리터가 `ownKeys`/`getOwnPropertyDescriptor`/`getPrototypeOf` 로 수행하며(ES2016 이후, 브라우저와 같음) ES2015 의 `[[Enumerate]]`/`enumerate` trap 은 `Reflect.enumerate` 직접 호출과 `EnumerateSupport` 로만 가능 | js-builtins-d.md |
| 9 | Promise | 미처리 거부 추적은 호스트가 `PromiseJobs.setRejectionTracker` 로 연결할 때만 동작(기본은 없음) | js-builtins-d.md |
| 10 | 14.6 꼬리 호출 | **해결(2026-11-16, JS-CORE)**: 엄격 모드 함수의 꼬리 위치 호출(return 의 직접/조건/논리/쉼표 식, 화살표 식 본문)은 트램펄린으로 실행해 호출 깊이를 쓰지 않는다. 바인드 함수 대상, try/for-in/for-of 안의 return, 제너레이터는 꼬리 호출이 아니다 | probe `14.6/proper-tail-calls`, `TailCallTest` |
| 11 | 19.2.4 | **해결(2026-11-16, JS-CORE)**: `Function.prototype.caller/arguments` 가 `%ThrowTypeError%` 접근자이다 | probe `19.2.4/function-caller-arguments`, `CoreIntegrationTest.testRestrictedFunctionProperties` |
| 12 | 숫자 → 문자열 | **해결(2026-11-16, JS-CORE)**: 코어 `NumberText` 가 `js.num.NumberConversion` 에 위임한다(`Number.MIN_VALUE` 는 `5e-324`) | `CoreIntegrationTest.testNumberTextIsExact` |

## 5. 확장 목록 (ES2015 에 없는 프로퍼티, ES6 완료 판정과 무관)

`MetaConformanceTest.testPropertiesBeyondTheTable` 가 모양 표에 나오는 모든 객체에 대해 표가 언급하지 않는 자체 프로퍼티를 출력한다(2026-11-16 기준 11건):

- 호스트: 전역 `globalThis`, `print`, `console`
- ES2016: `Array.prototype.includes`
- ES2017/2019(및 웹 호환 별칭): `String.prototype.padStart`, `padEnd`, `trimStart`, `trimEnd`, `trimLeft`, `trimRight`
- ES2018: `RegExp.prototype.dotAll`

`Packages`, `java`, `javax`, `JavaImporter`, `load`, `exit`, `System`, `Runtime` 같은 Java/셸 진입점이 전역에 없다는 것은 `testNoJavaOrShellGlobals` 가 확인한다(P15-02 의 전제).

## 6. 아직 지원하지 않는 것 / 확인하지 못한 것

- 보류(허용 목록, `Es2015LanguageProbeTest.PENDING`): 없음(꼬리 호출과 `Function.prototype.caller/arguments` 는 JS-CORE 가 구현해 허용 목록이 비었다).
- ES2015 의 모든 알고리즘 단계를 한 줄씩 대조한 것은 아니다. 규격 본문을 읽지 못했으므로 위 사례의 기대 결과는 지식 기반이며, 규격 본문에 접근할 수 있게 되면 `ConformanceTable` 과 probe 의 기대 값을 본문과 대조해야 한다.
- 모듈(15.2) 의 Script/Module 차이는 JS-CORE 의 `ModuleTest`(20사례)가 담당한다. 이 문서는 모듈 사례를 추가하지 않았다.
- 구현하지 않은 ES2015 이후 기능(async 함수, 지수 연산자 `**`, `Object.values/entries` 등)은 의도적으로 목록 밖이다.

## 7. P14-10 / P14-11 판단 (조정자용)

- **P14-10**: 사례 목록(위)과 실행 가능한 근거(표 510 + 동작 사례 77 + 스크립트 수준 38)가 있다. 외부 자료는 쓰지 않았다. → 체크 가능.
- **P14-11**: 성공/실패/건너뜀/미지원 수와 이유는 위 2, 3, 4, 6절에 기록했다. 이전에 걸리던 두 미구현(14.6 꼬리 호출, 19.2.4 `Function.prototype.caller/arguments`)은 JS-CORE 가 구현해 허용 목록이 비었다(2026-11-16). 남은 설명 있는 차이는 위 4절 표의 항목들이며 미구현은 없다.
