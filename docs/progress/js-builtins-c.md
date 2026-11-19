# 진행 기록: BUILTIN-C (P14-02 Date, P14-03 RegExp, P14-04 JSON, P14-05 Map/Set/WeakMap/WeakSet, P14-09 해당 객체의 이름/length/속성 확인)

담당 경로: `src/org/duckdns/hjow/swingwebpanel/js/builtins/datajson/**`, `tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/datajson/**`, 이 문서.
진입 클래스: `org.duckdns.hjow.swingwebpanel.js.builtins.datajson.DataLibInstaller` (`BuiltinInstaller`, 인수 없는 public 생성자; `StandardBuiltins` 가 이름으로 불러온다).
시험 방법:
1. Java 수준(Ops/invoke 를 직접 호출하는 시험): JS-CORE 가 `JsRealm` 등을 편집 중이라 작업 트리 전체가 컴파일되지 않을 때가 있어, HEAD 스냅샷(`git archive HEAD src tests/src`)을 먼저
   소스 경로에 두고 우리 패키지만 작업 트리에서 컴파일하는 임시 스크립트(저장소 밖)로 JDK 6u45 에서 실행했다. 동등한 공식 명령은
   `scripts/pkgtest.sh bic "js/builtins/datajson js/date js/regexp js/text js/num" "js/builtins/datajson"` 이다. 이 방식은 Java 수준 시험용이다(`ScriptLevelTest` 는 아래 2. 방식으로 실행).
2. 스크립트 수준(`ScriptLevelTest`): `JsRealm.create()` 표준 realm 에서 JS-CORE 의 자체 인터프리터로 실행한다. 표준 realm 의 설치 묶음은 이름으로 불러오므로 `-sourcepath` 가 참조하지 않는 클래스는 컴파일되지 않는다 —
   `src/org/duckdns/hjow/swingwebpanel/js` 전체를 컴파일해야 한다(HEAD 스냅샷 방식 1. 로는 String/Array 내장 객체가 없어 6 사례가 실패하는데 이는 시험 방식의 한계이지 제품 오류가 아니다).
   최종 확인은 `git archive HEAD` 에 우리 패키지 파일을 덮어쓰고 `src/org/duckdns/hjow/swingwebpanel/js` 전체를 JDK 6u45 로 컴파일한 뒤 datajson 시험을 `TestRunner --scan` 으로 실행하는 것이다(65 사례 통과).
규격 가정: ECMA-262 6판 본문은 조회가 403 이어서 확인하지 못했고 ES2015 알고리즘은 작성자의 지식에 따랐다. 가정은 각 클래스 Javadoc 에 적었다.
도구 주의: 입력의 `\uXXXX` 는 실제 문자로 바뀐다(소스에는 `(char) 0x2028` 형태, Java 문자열에 이스케이프 텍스트를 넣을 때는 `"\\u0041"`).

---

## 2026-11-16 · P14-05 · Map / Set / WeakMap / WeakSet, P14-09 (담당 객체 전부)

- 현재 작업 번호: P14-05 (Map, Set, WeakMap, WeakSet) 와 P14-09 의 이름/length/속성 확인
- 완료한 내용:
  - `OrderedTable`: 삽입 순서 키/값 저장소(SameValueZero: NaN 은 NaN 과 같고 -0 은 +0 으로 저장). 규격의 항목 목록 인덱스 순회와 똑같이 동작하는 `Cursor`
    (순회 중 추가된 항목은 방문, 도달 전에 삭제된 항목은 건너뜀, `clear` 후에도 진행 중인 커서가 이후 추가 항목으로 계속). 삭제는 빈칸으로 두었다가 압축하며
    살아 있는 모든 커서(약한 참조 목록)의 위치를 다시 매긴다.
  - `CollectionBuiltins`: `Map`/`Set`/`WeakMap`/`WeakSet` 생성자(`new` 필수, 이터러블을 현재의 `set`/`add` 로 채우고 항목이 객체가 아니거나 adder 가 던지면 이터레이터를 닫음, `new.target` 프로토타입),
    프로토타입 메서드 전부(Map: clear delete entries forEach get has keys set size values; Set: add clear delete entries forEach has size values; WeakMap: delete get has set; WeakSet: add delete has),
    `Map.prototype[@@iterator] === entries`, `Set.prototype.keys === values === [@@iterator]`, `Map[@@species]`/`Set[@@species]` getter, `@@toStringTag`,
    `%MapIteratorPrototype%`/`%SetIteratorPrototype%`(`%IteratorPrototype%` 를 상속, `next`, 태그 "Map Iterator"/"Set Iterator"), 수신 객체 검사(다른 종류/프로토타입 객체는 TypeError),
    WeakMap/WeakSet 은 객체만 키로 허용(원시 값 `set`/`add` 는 TypeError, `get`/`has`/`delete` 는 false/undefined)하며 `WeakHashMap` 으로 키를 약하게 쥔다.
  - P14-09: `AttributeTest` 가 표로 Date/RegExp/JSON/Map/Set/WeakMap/WeakSet 과 두 이터레이터 프로토타입의 모든 함수/접근자의 이름·length·writable/enumerable/configurable 을 검사하고,
    소유 객체마다 표에 없는 프로퍼티가 없고 표의 프로퍼티가 모두 있는지(`ownKeys`)도 확인한다. 생성자 length(Date 7, RegExp 2, 나머지 0), `prototype` {false,false,false}, `constructor` {true,false,true}, 전역 바인딩 속성, `@@toStringTag` 속성, 공유 함수 객체(toGMTString, entries/@@iterator, keys/values)도 검사한다.
  - `ScriptLevelTest`: 표준 realm 에서 실제 JavaScript 로 Date/RegExp/JSON/Map/Set/WeakMap/WeakSet 을 실행(ES5 스타일과 클래스 상속·화살표 함수·전개·for-of 구조 분해·`Symbol.match` 사용자 객체 등 ES2015 사례).
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/builtins/datajson/{OrderedTable,CollectionBuiltins,DataLibInstaller}.java`, `tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/datajson/{CollectionTest,AttributeTest,ScriptLevelTest}.java`, 이 문서
- 검증 결과 (JDK 6u45, 작업 트리 전체 컴파일, `bic-wt` 임시 스크립트): datajson 65 사례 통과 / 실패 0 / 건너뜀 0 — DateTest 13, RegExpTest 16, JsonTest 12, CollectionTest 11, AttributeTest 5, ScriptLevelTest 8.
  `testWeakKeysAreNotRetained` 는 이 실행에서 3 초 안에 키가 수거되어 통과했다(수거 시점은 비결정적이라 수거되지 않으면 이유를 밝히고 건너뛰도록 되어 있음). sourcecheck 위반 0 (17 파일).
  HEAD 스냅샷 방식(`ScriptLevelTest` 추가 전 실행)에서도 Java 수준 시험 57 사례(+ 실행기 자체 시험 7)가 통과했다. `ScriptLevelTest` 는 그 방식으로는 컴파일되지 않는다.
- 가정/한계: ① WeakMap 의 **값**은 맵이 강하게 쥔다(`WeakHashMap` 은 값이 키를 참조하면 키가 수거되지 않는 순환을 해소하지 못함: 에페메론 의미 없음). 키 자체는 약해서 키가 다른 곳에서 도달 불가가 되면 항목이 사라진다.
  ② WeakSet/WeakMap 은 키 동일성으로 비교하는 `WeakHashMap<JsObject,…>` 이므로 `JsObject.hashCode/equals` 를 재정의하지 않는 것에 의존한다(재정의하면 동일성 의미가 깨짐).
  ③ 반복 중 변경 시 항목 위치는 규격과 같지만, 압축은 빈칸이 32 개를 넘고 목록의 절반 이상일 때만 일어난다(관찰 가능한 차이 없음).
  ④ `/./s` 처럼 `s` 플래그가 붙은 **정규식 리터럴**은 JS-CORE 토큰화기가 "Invalid regular expression flags" 로 거부한다(`new RegExp('.', 's')` 는 동작). 플래그 `s` 는 ES2015 밖의 확장이므로 거부가 맞을 수도 있다 — JS-CORE 와 조율 필요.
- 다음 작업 번호: 이 작업(BUILTIN-C)의 범위는 끝났다. 전체 P14-10(적합성 사례 목록)·P14-11(사례 수 집계)은 조정 세션이 맡는다.

### 조정 세션에 요청/전달 사항

1. `StandardBuiltins` 가 이미 `DataLibInstaller` 를 이름으로 불러오므로 별도 등록은 필요 없다. 설치 순서는 core/corelib/stringarray 다음이어야 한다(`JSON.stringify` 의 래퍼 풀기는 `Number.prototype.valueOf` 등이 필요).
2. 실행 제한 연결: 호스트/JS-CORE 가 `ExecutionLimits` 의 취소를 정규식 실행에도 반영하려면 realm 마다 `RegExpBuiltins.setCancelHook(realm, hook)`(hook 이 true 면 `ScriptAbortedException(CANCELLED)`)와
   필요하면 `RegExpBuiltins.setStepLimit(realm, n)` 을 호출해야 한다. 지금은 자동 연결되어 있지 않다. Date 시간대는 `DateBuiltins.setTimeZone(realm, tz)` 로 고정할 수 있다.
3. `ScriptLevelTest` 는 `JsRealm.create()`/`evaluateScript` 를 쓰며, JS-CORE 의 커밋(032)이 들어온 HEAD 에서 컴파일·통과를 확인했다.
4. 정규식 리터럴의 `s` 플래그(위 한계 ④) 처리 방침 결정.

### 완료 표시 후보 ("내 담당 부분 검증 완료")

- P14-02 Date: 구현과 검증(Java 수준 13 + 스크립트 수준) 완료. 시간대는 `TimeZone` 주입 시험으로 확인.
- P14-03 RegExp: 구현과 검증(16 + 스크립트) 완료. 한계: 후방 탐색/`\p{}`/이름 있는 그룹/`RegExp.$1` 미지원(js.regexp 한계 또는 ES2015 밖).
- P14-04 JSON: 구현과 검증(12 + 스크립트) 완료.
- P14-05 Map/Set/WeakMap/WeakSet: 구현과 검증(11 + 스크립트) 완료. 한계: WeakMap 값 강한 참조(위 ①).
- P14-09 중 담당 객체의 이름/length/프로퍼티 속성: `AttributeTest` 로 확인 완료. Annex B 의 담당 항목(getYear/setYear/toGMTString, RegExp.prototype.compile)도 구현·검증.
- P14-10/P14-11 은 이 작업의 범위가 아니다.

### 아직 실행하지 못한 사례

- 없음(스크립트 수준 시험 8 사례가 모두 실행되었고 건너뛴 것이 없음). 다만 ES2015 적합성 외부 자료(P14-10)로는 아직 검증하지 않았다. 정규식의 치명적 역추적 한계를 스크립트로 확인하는 사례는 기본 단계 제한 값이 커서 넣지 않았고,
  Java 수준에서 `setStepLimit` 으로 확인했다.

## 2026-11-16 · P14-04 · JSON

- 현재 작업 번호: P14-04 (JSON 객체)
- 완료한 내용: `JsonBuiltins`: `JSON.parse`(반복 방식 파서라 중첩 깊이 제한 없음, `JsonTokenizer` 로 토큰화, `NumberConversion.stringToNumber` 로 숫자 변환, CreateDataProperty 로 객체 생성이라 `__proto__` 키도 자체 데이터 프로퍼티,
  중복 키는 마지막 값과 처음 위치, 오류는 `Unexpected token X in JSON at position N` / `Unexpected end of JSON input`, reviver 의 InternalizeJSONProperty(아래에서 위로, 키 순서, undefined 는 삭제)),
  `JSON.stringify`(replacer 함수/배열 속성 목록(문자열·숫자·String/Number 래퍼, 중복 제거), space 숫자/문자열/래퍼 객체와 10 상한, `toJSON(key)`, Number/String/Boolean 래퍼 풀기,
  NaN/Infinity → null, undefined/함수/심볼 생략 또는 배열에서 null, 순환 → TypeError, 깊이 `JsonBuiltins.MAX_DEPTH`(1000) 초과 → RangeError, `JsonText.quote`(ES2015 모드)), `JSON[@@toStringTag]`.
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/builtins/datajson/{JsonBuiltins,DataLibInstaller}.java`, `tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/datajson/JsonTest.java`
- 검증 결과 (JDK 6u45): JsonTest 12 사례 통과(원시 값/escape/숫자 형태, 구조와 키 순서, 잘못된 텍스트 28종과 메시지, 100000 단계 중첩, reviver 순서/this/삭제/변환, stringify 원시 값/구조/들여쓰기/replacer/toJSON/래퍼/순환/깊이 제한, JSON 객체 속성). sourcecheck 위반 0.
- 가정/한계: ① 래퍼 객체 풀기는 규격대로 `ToNumber`/`ToString` 을 호출하므로 `Number.prototype.valueOf`, `String.prototype.toString` 이 필요하다(전체 realm 에서는 corelib/stringarray 가 제공, 시험은 래퍼 프로토타입에 직접 정의). ② stringify/reviver 재귀 깊이는 1000 으로 제한한다.
  ③ 이름 있는 큰 정수 등 BigInt 는 범위 밖.
- 다음 작업 번호: P14-05 (Map/Set/WeakMap/WeakSet) — 완료됨(위 항목)

## 2026-11-16 · P14-03 · RegExp

- 현재 작업 번호: P14-03 (RegExp 객체)
- 완료한 내용: `RegExpObject`([[OriginalSource]]/[[OriginalFlags]]/[[RegExpMatcher]] + 자체 데이터 프로퍼티 `lastIndex`), `RegExpBuiltins`:
  생성자(함수 호출/`new`, IsRegExp, 같은 생성자 단축 경로, 정규식 복사, `new.target` 프로토타입, SyntaxError), `source`(EscapeRegExpPattern: 빈 패턴 `(?:)`, 클래스 밖 `/`, 줄 끝 문자) /`flags`(범용, 순서 g i m s u y)/
  `global ignoreCase multiline sticky unicode` 접근자(+확장 `dotAll`; `RegExp.prototype` 자체는 undefined, 그 밖의 비정규식은 TypeError), `exec`(RegExpBuiltinExec 의 `lastIndex` 규약: 전역/고정/실패 시 0 으로 되돌림, 읽기 전용 lastIndex 는 TypeError),
  `test`(사용자 `exec` 프로퍼티 호출 규칙 포함), `toString`, 부속서 B `compile`, `[Symbol.match]`, `[Symbol.replace]`(GetSubstitution `$$ $& $\` $' $n $nn`, 함수 대체, 빈 일치 진행, 겹침 건너뜀),
  `[Symbol.search]`(lastIndex 복원), `[Symbol.split]`(species 생성자, 고정 플래그 splitter, 캡처 포함, limit, u 모드), `RegExp[Symbol.species]`.
  자원 제한: 엔진의 단계/역추적 제한 → `RangeError("Regular expression too complex: ...")`, 취소 훅이 true 이면 `ScriptAbortedException(CANCELLED)`.
  `RegExpBuiltins.setStepLimit(realm, n)`, `RegExpBuiltins.setCancelHook(realm, hook)` 는 호스트용 공개 API(JS-CORE 의 실행 제한이 준비되면 취소 훅에 `ExecutionLimits.isCancelled()` 를 연결할 것).
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/builtins/datajson/{RegExpObject,RegExpBuiltins,Support,DataLibInstaller}.java`, `tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/datajson/{RegExpTest,Env}.java`
- 검증 결과 (JDK 6u45): RegExpTest 15 사례 통과(생성/복사/IsRegExp, 문법 오류, source 이스케이프, exec 결과와 lastIndex 규약(전역/고정/비전역/범위 밖/읽기 전용), 접근자와 flags getter,
  test + 사용자 exec, match/replace/search/split 전 경로(빈 일치, 유니코드 전진, `$` 틀, 함수 인수, species), compile, 수신 객체 검사, 단계 제한 RangeError, 취소 훅, species/이름/length). sourcecheck 위반 0.
- 가정/한계: ① `$nn` 이 범위를 벗어나면 첫 자리 `$n` 으로 되돌아간다(모든 브라우저 동작; 6판 본문은 "치환 안 함"). ② 이름 있는 그룹은 엔진이 받지만 `groups`/`$<name>` 은 구현하지 않았다(ES2018). ③ 구형 정적 `RegExp.$1`/`lastMatch` 미구현.
  ④ 엔진은 후방 탐색과 `\p{}` 를 지원하지 않는다(js.regexp 한계). ⑤ 플래그 `s`(dotAll)와 `dotAll` 접근자는 ES2015 밖의 확장이다.
- 다음 작업 번호: P14-04 (JSON)

## 2026-11-16 · P14-02 · Date

- 현재 작업 번호: P14-02 (Date 객체)
- 완료한 내용: `DateObject`([[DateValue]]), `DateBuiltins`(생성자 전 형태: 인수 없음/숫자/문자열/Date/구성 요소, 함수 호출 시 문자열, `new.target` 프로토타입,
  `Date.now/parse/UTC`, 지역/UTC getter 16개와 `getTime/valueOf/getTimezoneOffset`, setter 14개와 `setTime`(인수 변환 순서와 NaN 규칙 포함),
  `toString/toDateString/toTimeString/toUTCString/toISOString/toLocale*String/toJSON`, `@@toPrimitive`, 부속서 B `getYear/setYear/toGMTString`(= `toUTCString` 과 같은 함수 객체)).
  시간대는 `DateBuiltins.setTimeZone(realm, tz)` 로 realm 에 고정할 수 있고 기본은 호출 시점의 `TimeZone.getDefault()`.
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/builtins/datajson/{package-info,Support,DateObject,DateBuiltins,DataLibInstaller}.java`,
  `tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/datajson/{Env,DateTest}.java`
- 검증 결과 (JDK 6u45): DateTest 13 사례 통과(UTC/Asia/Seoul/America/New_York, 서머타임 틈, setter 인수 변환 순서와 NaN 규칙, 수신 객체 검사, `Date.prototype` 이 Date 가 아님,
  서식과 Invalid Date, RangeError, toJSON 범용 동작, `@@toPrimitive`, 부속서 B). sourcecheck(encoding,forbidden,javadoc,loops) 위반 0.
- 가정/한계: ① `Date.UTC(year)` 는 6판 본문과 달리 월이 없으면 0 으로 본다(모든 브라우저와 이후 판의 동작). ② 지역 시간대 표시 이름(`GMT+0900 (KST)`)은 js.date 의 JRE 의존 동작을 따른다.
  ③ 스크립트 수준 시험은 P14-05 작업에서 실행했다(`ScriptLevelTest.testDate*`).
- 다음 작업 번호: P14-03 (RegExp)
