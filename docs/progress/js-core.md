# JS-CORE 진행 기록 (P12~P13)

작업자: JS-CORE. 소유 경로: `src/org/duckdns/hjow/swingwebpanel/js/**`(js/regexp, js/num, js/date, js/text 제외), `tests/src/org/duckdns/hjow/swingwebpanel/js/**`(같은 하위 제외), 이 파일.
빌드/시험: `scripts/pkgtest.sh <이름> "js" "js"` (JDK 6u45). 코어 `NumberText` 가 JS-LIBS 의 `org.duckdns.hjow.swingwebpanel.js.num` 을 쓰므로 코어만 따로 컴파일할 수 없다.
선택적 라이브러리 설치기(corelib/stringarray/datajson/typed/meta)가 클래스패스에 없어도 코어와 `interp` 시험은 최소 내장 객체만으로 동작한다
(`StandardBuiltins` 가 설치기를 클래스 이름으로 찾음). `interp` 시험만: `scripts/pkgtest.sh jscoreall "js" "js" --filter interp`.

---

## Core API for built-ins  (P12-01~04 확정, 이후 변경 시 이 절에 변경 이력을 적는다)

P14 작업자(내장 객체 라이브러리)가 병렬로 시작할 수 있도록 안정적으로 공개하는 API 이다. 패키지는 모두 `org.duckdns.hjow.swingwebpanel.js`.

### 1. 값 모델

| 형식 | 클래스 | 만드는 법 |
| --- | --- | --- |
| undefined / null | `JsUndefined` / `JsNull` (싱글턴) | `Js.UNDEFINED`, `Js.NULL` |
| Boolean | `JsBoolean` | `Js.TRUE`, `Js.FALSE`, `Js.bool(b)` |
| Number | `JsNumber` (double, NaN/Infinity/-0 보존) | `Js.num(d)`, `JsNumber.of(int)` |
| String | `JsString` (UTF-16 코드 단위, Java String 래퍼) | `Js.str(s)` |
| Symbol | `JsSymbol` (인스턴스마다 고유) | `new JsSymbol("desc")`, `realm.symbolFor(key)` |
| Object | `JsObject` 와 하위 클래스 | `realm.newObject()`, `realm.newArray(...)` |

- 모든 값의 기반은 `JsValue`. **Java `null` 은 JavaScript 값이 아니다**(엔진 내부에서 "없음"만 뜻함). 내장 함수는 `Js.UNDEFINED` 를 반환한다.
- `value.type()`(enum `JsType`), `isUndefined()/isNull()/isNullOrUndefined()/isObject()/isCallable()/typeOf()`.
- 형 변환/비교/연산은 **반드시 `Ops`** 를 쓴다: `toPrimitive(v, hint)`, `toBoolean`, `toNumber`, `toInteger`, `toInt32`, `toUint32`, `toUint16`,
  `toString(v)`(Java String), `toStringValue`, `toObject(realm, v)`, `toPropertyKey(v)`, `toLength`, `strictEquals`, `looseEquals`, `sameValue`,
  `sameValueZero`, `lessThan(x, y, leftFirst)`(1/0/-1=undefined), `add`, `instanceOf`, `ordinaryHasInstance`, `call`, `construct`, `invoke`, `getV`,
  `getMethod`, `isArray`, `isConstructor`, `lengthOf(obj)`, `createListFromArrayLike`, `getPrototypeFromConstructor(realm, newTarget, fallback)`,
  `requireObjectCoercible`, `enumerableOwnKeys`.
- 숫자↔문자열은 `NumberText`(**임시 대체 구현**, 아래 7절)만 호출한다.

### 2. 프로퍼티 키와 내부 메서드

- 키는 Java `Object` 이고 **`java.lang.String` 또는 `JsSymbol`** 둘 중 하나. 숫자 인덱스는 `"0"`, `"1"` 같은 정규 문자열(`PropertyKeys.fromIndex(i)`, `PropertyKeys.arrayIndex(s)`).
- `JsObject` 가 ES6 §9.1 의 내부 메서드를 가진다. 이국적 객체는 이를 재정의한다:
  `getPrototypeOf/setPrototypeOf/isExtensible/preventExtensions/getOwnProperty(PropertyDescriptor)/getOwnPropertySlot(Property)/defineOwnProperty/hasProperty/get(key, receiver)/set(key, value, receiver)/delete/ownKeys/call/construct/isCallable/isConstructor/isArray/className`.
  - `ownKeys()` 순서: 배열 인덱스 오름차순 → 문자열(생성 순서) → 심볼(생성 순서).
  - `set` 은 거부되면 `false` 를 반환(예외 없음). 엄격 모드 의미는 `setStrict(key, v)`(TypeError).
- 편의 메서드(final): `get(key)`, `set(key, v)`, `setStrict`, `createDataProperty(key, v)`, `createDataPropertyOrThrow`, `definePropertyOrThrow`,
  `defineBuiltin(key, v)`(writable, **비열거**, configurable — 표준 메서드/생성자 속성), `defineData(key, v, w, e, c)`,
  `defineAccessor(key, getter, setter, e, c)`, `hasOwnProperty`, `getString(key)`.
- `PropertyDescriptor`: Java `null` 필드 = 지정 안 됨. 팩터리 `data(v,w,e,c)`, `accessor(g,s,e,c)`, `valueOnly(v)`, 분류 `isAccessor/isData/isGeneric/isEmpty`.
- `Proxy`(P14-07) 는 `JsObject` 를 상속해 위 내부 메서드를 모두 재정의하면 된다(`isArray()`, `isCallable()`, `isConstructor()` 포함).

### 3. 함수와 생성자

```java
// 일반 메서드 (Java 6 이므로 익명 클래스)
realm.method(proto, "charAt", 1, new NativeFunction.Call() {
    public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
        ...
        return Js.str("x");               // null 반환 금지(null 이면 undefined 로 간주됨)
    }
});
// 심볼 키 메서드: key 에 JsSymbol 을 넘기면 함수 이름은 "[설명]"
realm.method(proto, WellKnownSymbols.ITERATOR, 0, call);
// getter 만 있는 접근자 (예: Map.prototype.size)
realm.getter(proto, "size", call);
```

- `Js.arg(args, i)` 로 선택 인수를 안전하게 읽는다(모자라면 undefined).
- 생성자: `new NativeFunction(realm, "Map", 0, callBehaviour, new NativeFunction.Construct() { public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {...} })`.
  프로토타입은 `Ops.getPrototypeFromConstructor(r, newTarget, r.getIntrinsic("%MapPrototype%"))` 로 고른다(하위 클래스 지원).
  이어서 `ctor.defineData("prototype", proto, false, false, false)`, `proto.defineBuiltin("constructor", ctor)`, `global.defineBuiltin("Map", ctor)`.
- 호출 가능성은 `NativeFunction#isConstructor()` = Construct 구현체 유무. `new.target` 이 없으면(`newTarget == null`) 자기 자신으로 간주된다.
- 함수 객체의 `length`, `name` 은 자동 정의(writable 아님, 비열거, configurable).
- 바운드 함수 클래스 `BoundFunction(realm, target, boundThis, boundArgs)` — name/length 는 호출자(Function.prototype.bind)가 설정.

### 4. 오류 던지기

`throw JsException.typeError("...")`, `rangeError`, `referenceError`, `syntaxError`, `uriError`, 또는 `new JsException(ErrorKind.X, msg)`.
realm 이 없어도 던질 수 있으며, JavaScript 코드가 잡을 때 `JsException#getValue(realm)` 이 오류 객체를 만든다.
임의 값 던지기: `new JsException(jsValue)`. Java 스택 추적은 꺼져 있다. 오류 객체 직접 생성: `realm.createError(kind, message)`.

### 5. realm 과 내장 객체 등록

- `JsRealm` 접근자: `getGlobalObject()`, `getObjectPrototype()`, `getFunctionPrototype()`, `getArrayPrototype()`, `getStringPrototype()`, `getNumberPrototype()`,
  `getBooleanPrototype()`, `getSymbolPrototype()`, `getIteratorPrototype()`, `getErrorPrototype(ErrorKind)`.
- 임의 내장 객체 등록부: `realm.setIntrinsic("%MapPrototype%", obj)` / `realm.getIntrinsic("%MapPrototype%")`. 이름은 `%이름%` 형태.
  **코어가 등록하는 이름**(P13 완료 시점): `%IteratorPrototype%`(realm 생성 시 이미 등록), `%ArrayIteratorPrototype%`, `%GeneratorFunction%`, `%GeneratorFunction.prototype%`(=%Generator%),
  `%GeneratorPrototype%`, `%Object%`, `%Function%`, `%Array%`, `%Error%`, `%Symbol%`, `%ThrowTypeError%`.
  P14 작업자는 자신이 만드는 것(`%MapPrototype%`, `%PromisePrototype%`, `%RegExpPrototype%`, `%DatePrototype%` ...)을 같은 방식으로 등록한다.
- 설치 단위는 `BuiltinInstaller { void install(JsRealm realm); }`. P13 에서 `org.duckdns.hjow.swingwebpanel.js.builtins.StandardBuiltins.installAll(JsRealm)` 이 설치 순서를 정하며
  (Object → Function → Error → Symbol → Array → iterator/generator → …), **P14 작업자는 그 목록에 자기 설치기 한 줄을 추가**한다
  (충돌하면 조정자에게 요청). 설치기는 `realm.getGlobalObject().defineBuiltin("Math", math)` 로 전역에 붙인다.
- 전역 프로퍼티 속성: 표준 전역 함수/생성자는 `defineBuiltin`(writable, 비열거, configurable). `NaN`, `Infinity`, `undefined` 는 `defineData(k, v, false, false, false)`.

### 6. 이터레이터 도우미 (`IteratorHelper`)

`getIterator(realm, v)`, `next(it, value|null)`, `step(it)`(끝이면 null), `isDone(result)`, `value(result)`, `close(it)`, `closeQuietly(it)`,
`createResult(realm, value, done)`, `toList(realm, iterable)`. 조기 종료 시 `close`, 예외 중에는 `closeQuietly` 를 쓴다.
내장 이터레이터 객체는 프로토타입을 `%IteratorPrototype%`(또는 그 하위)로 두면 `[Symbol.iterator]() { return this }` 를 상속한다.

### 7. 숫자 변환 연결 지점 (JS-LIBS 용)

`org.duckdns.hjow.swingwebpanel.js.NumberText` 의 세 메서드 `toString(double)`, `toString(double, int radix)`, `parse(String)` 는 이제 JS-LIBS 의
`org.duckdns.hjow.swingwebpanel.js.num.NumberConversion`(`toString`, `toString(v, radix)`, `stringToNumber`)에 위임하는 얇은 외관이다(2026-11-16, P13 마무리).
따라서 `Ops.toString(Number.MIN_VALUE)` 는 `5e-324`, 0.1+0.2 는 `0.30000000000000004` 이다(`CoreIntegrationTest.testNumberTextIsExact`).
`JsChars` 가 공백/식별자 문자 판정을 제공한다. `parseInt/parseFloat/toFixed/toPrecision/toExponential` 은 코어에 없고 P14 작업자가 JS-LIBS 를 써서 만든다.

### 8. 코어가 이미 제공하는 객체 (P12 시점)

`JsObject`, `JsArray`(밀집/희소 자동 전환, 가상 length, `add`, `getOwnElement/setOwnElement` 빠른 경로, `getLength`), `JsStringObject`(문자열 이국적 객체),
`JsPrimitiveObject`(Boolean/Number/Symbol 래퍼, `getPrimitive()`), `JsErrorObject`, `NativeFunction`, `BoundFunction`, `WellKnownSymbols`(ES6 의 11개, 모든 realm 공유).
Array.prototype 의 최소 메서드(전개/구조 분해/for-of 에 필요한 것)와 Object/Function/Error/Symbol 의 최소 생성자는 P13 에서 코어가 만든다.
나머지 전체 라이브러리는 P14 작업자의 몫이며, `ArrayBuiltins` 의 최소 메서드를 확장할 때는 같은 클래스를 확장하지 말고 별도 설치기에서
`Array.prototype` 에 메서드를 추가/덮어쓴다(코어 메서드는 `defineBuiltin` 이므로 덮어쓸 수 있음).

### 9. 호스트 API, 설치기, 제한 (P13 에서 추가)

**realm 만들기와 실행** (모두 realm 을 소유한 한 스레드에서 호출. 스레드 사이에서 안전한 것은 `getLimits().cancel()` 뿐):

| API | 설명 |
| --- | --- |
| `JsRealm.create()` | 전체 표준 내장 객체가 설치된 realm. `new JsRealm()` 은 내장 객체가 하나도 없는 빈 realm(코어 시험용) |
| `evaluateScript(String source, String name)` | 전역 스코프에서 실행, 마지막 문장의 완료 값을 반환. 구문 오류/미처리 throw 는 `JsException`(메시지 `Name: message (name:줄:열)`), 한도/취소는 `ScriptAbortedException`(잡을 수 없음) |
| `evaluateModule(String key, String source)` | 모듈을 불러와 링크/평가하고 네임스페이스 객체를 반환. 같은 key 는 같은 모듈 인스턴스(재평가 없음). source 가 null 이면 `ModuleLoader.fetch(key)` |
| `setModuleLoader(ModuleLoader)` | `resolve(specifier, referrerKey)` → 키, `fetch(key)` → 소스. 모듈 주소 해석과 리소스 요청은 호스트(P15)의 몫 |
| `setJobQueue(JobQueue)`, `enqueueJob(Runnable)`, `runJobs()` | 마이크로태스크. 기본 `SimpleJobQueue` 는 FIFO, `runJobs()` 가 비움(작업마다 한도 예산을 새로 시작). 호스트 큐를 쓰면 호스트가 직접 비운다 |
| `setConsole(ConsoleHook)` | `console.log/info/warn/error/debug` 와 시험용 `print` 의 출력. `message(level, text)`. 기본값은 표준 출력/오류 |
| `getLimits()` | `ExecutionLimits` (아래) |
| `getInterpreter()` | `Interpreter.createDynamicFunction`, `evalIndirect`, `getRealm` 등 (P14 의 Function 생성자 등이 사용) |

호스트 객체: `realm.newObject()`, `realm.method(obj, key, length, NativeFunction.Call)`, `realm.getter(...)`, `realm.getGlobalObject().defineBuiltin(name, value)`.
네이티브 함수가 던진 `JsException` 은 스크립트가 잡을 수 있고, 그 밖의 Java 예외(`RuntimeException`)는 스크립트의 catch/finally 로 삼킬 수 없이 호스트로 전파된다.

**설치기 순서와 내장 객체 이름** (`StandardBuiltins.installAll`): Object, Function, Error, Symbol, Iterator(+Generator), Array, Primitive(Boolean/Number/String 최소), Global(globalThis, NaN, Infinity,
undefined, eval, print, console) 순으로 코어가 설치한 뒤, 선택적 설치기를 **이 순서로** 클래스 이름으로 찾아 설치한다: `builtins.corelib.CoreLibInstaller`,
`builtins.stringarray.StringArrayInstaller`, `builtins.datajson.DataLibInstaller`, `builtins.typed.TypedLibInstaller`, `builtins.meta.MetaLibInstaller` (없으면 건너뜀).
코어의 최소 메서드는 `defineBuiltin` 으로 정의되어 있어 라이브러리가 같은 이름으로 덮어쓸 수 있다.
realm 의 intrinsic 이름(`getIntrinsic`): `%Object%`, `%Function%`, `%Array%`, `%Error%`, `%Symbol%`, `%ThrowTypeError%`, `%ArrayProto_values%`, `%ArrayIteratorPrototype%`,
`%StringIteratorPrototype%`, `%GeneratorFunction%`, `%GeneratorFunction.prototype%`, `%GeneratorPrototype%`, `%eval%`; 정규식 리터럴은 `%RegExp%`(JS-LIBS 가 등록) 생성자를 쓴다.
`Function.prototype.caller/arguments` 는 `%ThrowTypeError%` 접근자이다(AddRestrictedFunctionProperties).

**`ExecutionLimits`** (realm 마다 하나, 작업 = 가장 바깥 `evaluateScript/evaluateModule` 호출 또는 작업 큐의 작업 하나마다 예산을 새로 시작):

| 한도 | 기본값 | 초과 시 |
| --- | --- | --- |
| 단계 수 `setMaxSteps` | 200,000,000 (0 이하는 무제한) | `ScriptAbortedException(STEP_LIMIT)` |
| 시간 `setMaxTimeMillis` | 0 = 무제한 (호스트가 정함) | `ScriptAbortedException(TIME_LIMIT)`, 1024 단계마다 시계를 확인 |
| 취소 `cancel()` / `reset()` | 어느 스레드에서나 호출 | 다음 단계에서 `ScriptAbortedException(CANCELLED)`. `reset()` 전에는 새 실행도 즉시 중단 |
| 호출 깊이 `setMaxCallDepth` | 400 (1 이상) | 스크립트가 잡을 수 있는 `RangeError: Maximum call stack size exceeded`. Java `StackOverflowError` 도 같은 오류로 바꾼다. 깊이를 올리면 큰 스택(예 16MB) 스레드에서 실행 |
| 문자열 길이 `setMaxStringLength` | 33,554,432 코드 단위 (1 이상) | `RangeError: Invalid string length` (`+`, 템플릿, 그리고 `checkStringLength` 를 부르는 내장 객체) |
| 배열 | 조밀 요소 5천만 개까지, 그보다 큰 인덱스는 희소 모드. `length` 는 2^32-1 | `RangeError: Invalid array length` |

`ScriptAbortedException` 은 스크립트의 try/catch/finally 가 삼키지 못한다(finally 안에서 다시 한도에 걸리면 즉시 같은 예외). 중단된 realm 의 전역 상태는 유지되고 이후 실행은 새 예산으로 가능하다
(`LimitsTest.testRealmIsUsableAfterAbort`). `isCancelled()`, `tick()` 은 인터프리터가 문장/반복/호출마다 부른다.

**긴 네이티브 코드를 위한 취소 API (BUILTIN-B/C 연결)**: `ExecutionLimits.isAbortRequested()`(예외 없는 조회: 취소 요청 또는 작업 시간 초과)와
`ExecutionLimits.poll()`(예외 버전: `ScriptAbortedException` CANCELLED/TIME_LIMIT). 
- **정규식**: `StandardBuiltins.installAll` 이 정규식 라이브러리가 있으면 `RegExpBuiltins.setCancelHook(realm, hook)` 을 리플렉션으로 연결한다(`hook.isCancelled()` →
  `limits.isAbortRequested()`; 훅은 `ExecutionLimits` 만 붙잡아 realm 이 약한 맵에서 수거될 수 있다). `CoreIntegrationTest.testRegExpMatchingIsCancellable`:
  `/^(a+)+$/` 의 파국적 역추적이 300ms 시간 제한에서 멈춘다.
- **`Support.poll()` (stringarray)**: 현재 스레드 인터럽트만 본다. **조정자 요청**: BUILTIN-B 가 realm 을 넘겨 `realm.getLimits().poll()` 을 부르도록 바꾸면 `cancel()`/시간 제한과 연결된다.
  또한 stringarray 의 `Support.checkStringLength` 는 상수(`DEFAULT_MAX_STRING_LENGTH`)를 쓰므로 `setMaxStringLength` 값을 따르지 않는다 → `realm.getLimits().checkStringLength(...)` 로 바꾸기를 권한다.

**정규식 리터럴**: 렉서는 플래그 `g i m s u y`(중복 불가)를 받는다(`s` 는 ES2018 이지만 JS-LIBS 의 정규식 엔진이 지원하므로 허용). 패턴의 조기 오류는 `Parser.setDefaultRegExpValidator` 로 연결한다.

**제너레이터 설계(스레드 없음)**: 제너레이터 본문을 AST 로 다시 걷는다. `yield` 를 포함한 노드(`hasYield`)마다 `NodeState`(이미 평가한 하위 값, 반복문/try 의 단계)를 `GenState` 에 저장하고,
재개하면 같은 AST 를 처음부터 다시 내려가며 저장된 값은 재사용(`memo/replay/save`)하고 중단 지점에서 이어서 평가한다. 중단은 `YieldSignal`(싱글턴), `return()` 은 `GeneratorReturn` 예외로 finally 를 거쳐 풀린다.
제너레이터마다 Java 스레드가 없고 중단된 제너레이터는 평범한 객체이다(`GeneratorTest`: 3000개 중단 시 스레드 증가 없음, 참조를 끊으면 GC 가능, 2만 번 재개, 한도/취소 적용).

**Reference (ES2015 6.2.3, `interp.Reference`)**: 식별자와 멤버 표현식은 `Expressions.evalReference` 가 `Reference` 로 평가한다(식별자 `resolved/identifier`, `base.name` `property`, `base[key]` `element`, `super.x` `superProperty`;
제너레이터를 위해 환경 조회와 하위 식은 저장/재생). 연산: `isPropertyReference/isUnresolvableReference/isSuperReference/isStrictReference`, `getBaseValue/getReferencedName/getThisValue`, `getValue`(GetValue),
`putValue`(PutValue: 해석 불가는 느슨한 모드에서 전역 프로퍼티 생성, 엄격 모드에서 ReferenceError), `delete`(12.5.3.2), `thisValueForCall`(호출의 this 값: 프로퍼티 참조의 기준, `with` 객체, 그 밖은 undefined).
대입/복합 대입, `++/--`, `delete`, `typeof`(해석 불가 식별자는 "undefined"), 호출과 꼬리 호출의 this 값 계산이 모두 이 클래스를 거친다. 요소 참조의 키 변환은 처음 쓸 때 한다(Chromium 동작).
시험: `ReferenceTest` 8건(종류별 직접 시험 + 인터프리터 사용 지점 + 제너레이터). 성능은 피보나치/프로퍼티 갱신/배열 반복 벤치마크에서 변경 전과 오차 범위 안이다.

**꼬리 호출(ES2015 14.6)**: 엄격 모드 함수가 `[[Call]]` 로 호출되었고 제너레이터가 아니면 `return` 의 식(직접 호출, `?:`, `&&`/`||`, 쉼표의 마지막, 괄호)과 화살표 함수의 식 본문 안의 호출이
꼬리 위치이다(`Expressions.evalTail`). 대상이 스크립트 함수(클래스 생성자 제외)이면 호출하지 않고 `Ctx.tailTarget/tailThis/tailArgs` 에 저장하고, `Interpreter.callFunction` 의 반복문(트램펄린)이
같은 호출 깊이에서 이어서 실행한다(한 번 돌 때마다 `limits.tick()` 이므로 단계 수/시간/취소는 그대로 적용). try 문(catch/finally 포함)과 for-in/for-of 안, 제너레이터, 생성자(`new`), 직접 eval,
바인드 함수/네이티브 대상은 꼬리 호출이 아니다(일반 호출). 호출자가 엄격 모드이면 피호출자는 느슨한 모드여도 된다. 시험: `TailCallTest`(70건: 10만~100만 번 재귀, 상호 재귀, 위치별, 의미, 비꼬리 위치는 깊이 제한 유지).

### 변경 이력 (Core API)

- 2026-11-16: 최초 공개 (P12-01~04).
- 2026-11-16 (P13): `JsRealm` 에 `create()`, `evaluateScript`, `evaluateModule`, `getLimits/getInterpreter`, `get/setJobQueue`, `enqueueJob`, `runJobs`, `get/setModuleLoader`, `get/setConsole`, `getIteratorPrototype` 추가.
  새 클래스: `ExecutionLimits`, `ScriptAbortedException`, `JobQueue`, `SimpleJobQueue`, `ModuleLoader`, `ConsoleHook`, `org.duckdns.hjow.swingwebpanel.js.interp.*`, `org.duckdns.hjow.swingwebpanel.js.builtins.*`. 기존 시그니처는 바뀌지 않았다.
  `JsException.getValue(JsRealm)` 은 P12 부터 realm 을 받는다(게으른 기본 오류 객체 생성).
- 2026-11-16 (P13 마무리): `NumberText` 가 `js.num.NumberConversion` 에 위임(임시 구현 폐기). `ExecutionLimits.isAbortRequested()/poll()` 추가. 정규식 취소 훅 자동 연결.
  `Function.prototype.caller/arguments` 제한 접근자. 렉서가 정규식 플래그 `s` 허용. 직접 eval 판정에서 `!paren` 조건 제거(`(eval)(x)` 도 직접 eval, 규격과 같음).
  `ExecutionLimits.setMaxCallDepth/setMaxStringLength/setMaxTimeMillis` 가 잘못된 값에 `IllegalArgumentException`.

---

## 작업 기록 (최신이 위)

### 2026-11-16 · P13-01~P13-13 · 인터프리터, 모듈, 동적 코드, 제한, 꼬리 호출

- 현재 작업 번호: P13-13 완료(P13 전체). 다음: P14(라이브러리)와 P15(DOM 연결)가 코어 API(9절)를 사용한다.
- 완료한 내용:
  - P13-01/02: `interp.Interpreter/Environment/Binding/Ctx/Completion`, 전역 선언 인스턴스화(CanDeclareGlobalFunction 포함), var/함수 호이스팅, let/const/class TDZ, 블록 함수(부속서 B).
  - P13-03/04: 연산자, 평가 순서, 참조 처리, 모든 문장과 Completion(finally 중첩, 라벨, 완료 값).
  - P13-05~08: 함수/클로저/this/arguments(mapped)/call/apply/bind/new.target, 리터럴/기본·나머지 매개변수/전개/구조 분해/템플릿/태그 템플릿, 화살표 함수, 클래스(super, 접근자, 정적, 내장 상속).
  - P13-09: 이터레이터/for-of/제너레이터(스레드 없는 재방문 방식, 위 9절), yield/yield*/return/throw/반복자 닫기.
  - P13-10: 모듈(링크/평가, live binding, 네임스페이스, 재수출, 순환, 인스턴스화 오류). 익명 `export default class` 의 이름은 `default`.
  - P13-11: 직접/간접 eval(부모 환경, var/let 충돌 SyntaxError, 엄격 eval 격리), Function/GeneratorFunction 생성자(파라미터/본문 분리 검증, toString), 엄격 모드 규칙.
    `(eval)(x)` 도 직접 eval(규격과 같음), 스프레드 인수 `eval(...a)` 도 직접 eval.
  - P13-12: `ExecutionLimits`(단계/시간/깊이/문자열/취소, `isAbortRequested()/poll()`), `ScriptAbortedException`, 정규식 취소 훅 자동 연결, 작업 큐 예산, 한도 문서(9절).
  - P13-13: `Es2015ChecklistTest`(6개 묶음 약 200 사례, 11~15절/18.2.1/9절/부속서 B 의 절 번호로 대조) + 영역별 시험 자원.
  - 추가(조정자 요청): 꼬리 호출(14.6), `Function.prototype.caller/arguments` 접근자, `NumberText` → `js.num` 위임, 정규식 플래그 `s`.
- 시험(JDK 6u45, `scripts/pkgtest.sh jscoreall "js" "js"`): 전체 js 트리 **580 passed, 0 failed, 0 skipped**(다른 작업자 시험 포함). JS-CORE 소유 시험 약 **260건**:
  `tests/src/org/duckdns/hjow/swingwebpanel/js/interp` 10개 클래스(ScopeTest 4, OperatorTest 3, ControlFlowTest 2, FunctionTest 2, SyntaxEs6Test 2, ClassTest 2, GeneratorTest 7, ModuleTest 20,
  DynamicCodeTest 4, LimitsTest 12, HostApiTest 11, Es2015ChecklistTest 6, CoreIntegrationTest 5, TailCallTest 3) + 스크립트 자원(`tests/resources/org/duckdns/hjow/swingwebpanel/js/interp/*.js` 약 1200 단언) + P12 시험 90건.
  `Es2015LanguageProbeTest` 77/77, `ConformanceTable` 510/510, 허용 목록 비어 있음. `tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/js tests/src/org/duckdns/hjow/swingwebpanel/js`: **0 violation**.
- 규격과 다르거나 Chromium 을 따른 선택(기능 표에 옮길 것):
  - 대입에서 프로퍼티 키의 ToPropertyKey 는 오른쪽 값 평가 뒤에 한다(Chromium 과 같음, ES2015 본문은 이전).
  - 반복자의 `next` 는 단계마다 읽는다(ES2015, 최신 규격은 한 번).
  - `for (var x = 1 in o)`(부속서 B, ES2017)는 SyntaxError. catch 매개변수와 같은 이름의 `for (var e of …)` 는 허용(ES2015, 최신 규격은 SyntaxError).
  - 식별자 문자는 JRE 6 의 유니코드 4.0 범위.
- 알려진 한계(정직한 기록):
  - (해결됨, 2026-11-16) `interp.Reference` 클래스를 도입해 P13-01 의 "Reference 자료형 분리"를 충족했다(아래 9절 "Reference").
  - 꼬리 호출은 위 9절의 위치에서만 동작한다(바인드 함수 대상은 일반 호출).
  - 라이브러리 쪽(내가 고치지 않음): `stringarray.Support.poll()` 은 스레드 인터럽트만 보고 `Support.checkStringLength` 는 `setMaxStringLength` 값을 따르지 않는다(`ExecutionLimits.poll()` 과 `checkStringLength` 사용을 권함).
  - 제너레이터는 재방문 방식이라 `yield` 를 포함한 식에서 하위 식의 값을 `NodeState` 에 보관한다. 매우 큰 제너레이터 본문을 매번 처음부터 내려가므로 중첩이 깊으면 재개 비용이 본문 크기에 비례한다.
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/{ExecutionLimits,ScriptAbortedException,JobQueue,SimpleJobQueue,ModuleLoader,ConsoleHook,NumberText,JsRealm}.java`, `src/org/duckdns/hjow/swingwebpanel/js/interp/*`,
  `src/org/duckdns/hjow/swingwebpanel/js/builtins/*.java`(최소 내장 객체, StandardBuiltins), `parser/{Lexer,ExpressionParser}.java`, `tests/src/org/duckdns/hjow/swingwebpanel/js/interp/*`, `tests/resources/org/duckdns/hjow/swingwebpanel/js/interp/*.js`,
  `tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/meta/Es2015LanguageProbeTest.java`(허용 목록 비움, 조정자 요청), `docs/progress/es2015-conformance.md`(해결 표시).
- 체크박스 후보: **P12-01~P12-09**, **P13-02~P13-13**, **P13-01**(Reference 클래스 도입으로 무조건 후보), **P14-11** 의 코어 쪽 조건(꼬리 호출과 caller/arguments 해결, `NumberText` 정확 변환).

### 2026-11-16 · P12-06~P12-09 · AST, 파서, 조기 오류

- 현재 작업 번호: P12-06~P12-09 완료. 다음: P13-01 (인터프리터 골격).
- 완료한 내용:
  - AST(`org.duckdns.hjow.swingwebpanel.js.ast`): 범용 `Node`(+ `NodeType` 상수별 배치 문서), `Op`, `ScopeInfo`, `FunctionInfo`, `TemplateParts`, `ProgramInfo`(모듈 import/export 표), `AstDump`(S-식 덤프).
  - 파서(`org.duckdns.hjow.swingwebpanel.js.parser`): `ParserBase`(토큰 커서, 스코프/재선언 검사, Annex B 함수 끌어올림), `ExpressionParser`(우선순위 상승법, 커버 문법 구조 분해,
    화살표 함수 추측 파싱, 템플릿, 객체/배열 리터럴, 함수/메서드/클래스, 바인딩 패턴), `Parser`(문장, 레이블/jump 검증, 지시문 서문과 엄격 모드, Script/Module,
    import/export, eval 코드, Function 생성자 소스 분리 검증), `RegExpValidator`(정규식 패턴 조기 오류 연결 지점).
  - P12-06: 정규식/나눗셈은 파서가 `rescanRegExp` 로 구분. ASI(restricted production: return, throw, break/continue, postfix, do-while 포함).
  - P12-07: 연산자 우선순위/결합, 위치 있는 오류, ES2015 에 없는 문법(`**`, `?.`, `??`, async, BigInt, 클래스 필드, `#`, 객체 spread, `import()`) 은 SyntaxError.
  - P12-08: Script/Module 목표 차이, 엄격 모드(8진, with, delete 식별자, eval/arguments, 예약어, 중복 매개변수, 지시문 뒤의 소급 검사), 선언 중복(let/const/class/function/var/catch),
    레이블/break/continue/return, 클래스(중복 constructor, super 위치, static prototype), 제너레이터(yield 매개변수/식별자), 접근자 매개변수, 구조 분해, 모듈 export 중복/미정의.
  - P12-09: 값 변환/객체 속성/연산자 우선순위/문자열/유니코드/세미콜론/오류 위치를 DOM 없이 검증. `EngineIndependenceTest` 가 컴파일된 엔진 클래스에서 javax.script, Rhino, Nashorn,
    GraalJS, com.sun, sun, ProcessBuilder, loadLibrary 참조가 없음을 확인한다.
- ES2017 호환 확장(허용): 인수/매개변수 목록의 마지막 쉼표. 그 밖에 ES2015 에 없는 문법은 모두 오류.
- 미구현/제약(정직한 기록):
  - 정규식 리터럴의 **패턴** 조기 오류는 `Parser.setDefaultRegExpValidator` 로 JS-LIBS 의 regexp 가 준비되면 연결한다(플래그는 렉서가 검증한다).
  - `for (var x = 1 in obj)` (부속서 B 의 for-in 초기값) 은 허용하지 않는다(오류).
  - 부속서 B 의 블록 함수 끌어올림은 구현했으나, `catch (f) { { function f(){} } }` 같은 catch 매개변수와의 세부 상호작용은 단순화했다.
  - 식별자의 유니코드 범위는 JRE 6 의 유니코드 4.0 이다.
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/ast/*`, `src/org/duckdns/hjow/swingwebpanel/js/parser/*`, `tests/src/org/duckdns/hjow/swingwebpanel/js/parser/{ParserExpressionTest,ParserStatementTest,EarlyErrorTest}.java`,
  `tests/src/org/duckdns/hjow/swingwebpanel/js/EngineIndependenceTest.java`, `JsChars.java`(공백 목록을 JRE 표 대신 명시).
- 검증 결과 (JDK 6u45, 2026-11-16): JS-CORE 영역 전체 **96 passed, 0 failed, 0 skipped**. `tools/sourcecheck` (encoding, forbidden, javadoc, loops) 내 소스 0건.
- 체크박스 후보: P12-06, P12-07, P12-08, P12-09 (P12 단계 전체).

### 2026-11-16 · P12-05 · 렉서

- 현재 작업 번호: P12-05 완료. 다음: P12-06 (정규식/나눗셈 구분, ASI — 파서와 함께).
- 완료한 내용: `org.duckdns.hjow.swingwebpanel.js.parser.{TokenType, Token, Lexer}`. 유니코드 식별자(이스케이프 `\uXXXX`, `\u{...}`, 보충 문자), 숫자(10진/0x/0o/0b/레거시 8진),
  문자열(모든 이스케이프, 줄 이음, 레거시 8진 표시), 템플릿 조각(cooked/raw, CR/CRLF 정규화, 잘못된 이스케이프는 cooked=null + 오류 기록),
  주석/줄 끝(LF, CR, CRLF, LS, PS)/`newlineBefore`, HTML 유사 주석(스크립트만), 구두점(최장 일치, `**` 미지원), 정규식 리터럴 재스캔(`rescanRegExp`), 템플릿 이어짐 재스캔,
  상태 저장/복원(`saveState/restoreState/resetAfter`), 위치(줄/열)와 SyntaxError 위치 정보.
- 알려진 JRE 6 한계(문서화): 식별자 문자(ID_Start/ID_Continue)는 `Character.isUnicodeIdentifierStart/Part` 로 판정하므로 JRE 6 의 유니코드 4.0 범위이다.
  공백은 JRE 표 대신 `JsChars.isWhiteSpace` 의 명시 목록을 쓴다(실제 결함 발견: JRE 6 은 U+200B 를 Zs 로 분류).
- 검증 결과 (JDK 6u45, 2026-11-16): JS-CORE 영역 전체 **69 passed, 0 failed** (LexerTest 15 사례 포함). `tools/sourcecheck` 0건.
- 체크박스 후보: P12-05.

### 2026-11-16 · P12-01~P12-04 · 값/객체/변환 모델

- 현재 작업 번호: P12-01~P12-04 구현 및 검증 완료. 다음: P12-05 (렉서).
- 완료한 내용:
  - P12-01: `JsValue` 계층(`JsUndefined`, `JsNull`, `JsBoolean`, `JsNumber`, `JsString`, `JsSymbol`, `JsObject`), `JsType`, `Js` 상수/팩터리. Java null 과 구분.
  - P12-02: UTF-16 코드 단위 문자열(`JsString`, `JsStringObject`), `codePointAt`, 심볼(고유성, 레지스트리, 문자열 키와 분리).
  - P12-03: `JsObject`(ES6 §9.1 내부 메서드 전부, ValidateAndApplyPropertyDescriptor, 키 순서), `Property`, `PropertyDescriptor`, `JsArray`(ArraySetLength, 희소 모드),
    `JsFunction`/`NativeFunction`/`BoundFunction`, `JsPrimitiveObject`, `JsErrorObject`, `JsException`/`ErrorKind`, `JsRealm`(골격).
  - P12-04: `Ops`(ToPrimitive, ToNumber, ToString, ToInt32/Uint32/Uint16, ToLength, ToObject, ToPropertyKey, ==/===/SameValue/SameValueZero, 관계 비교, +, instanceof, 호출 도우미),
    `NumberText`(임시), `JsChars`. 정수 비트 연산 자체(`& | ^ << >> >>>`)는 인터프리터(P13-03)가 `Ops.toInt32/toUint32` 를 사용해 구현한다.
  - 이터레이터 프로토콜 도우미 `IteratorHelper` 와 `%IteratorPrototype%`(P13-09 의 선행 부품).
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/*.java`(Js, JsType, JsValue, JsUndefined, JsNull, JsBoolean, JsNumber, JsString, JsSymbol, JsObject, Property, PropertyDescriptor, PropertyKeys,
  JsArray, JsFunction, NativeFunction, BoundFunction, JsPrimitiveObject, JsStringObject, JsErrorObject, JsException, ErrorKind, WellKnownSymbols, JsChars, NumberText, Ops, JsRealm, BuiltinInstaller, IteratorHelper),
  `tests/src/org/duckdns/hjow/swingwebpanel/js/{ValueModelTest,ObjectModelTest,ArrayModelTest,ConversionTest,IteratorHelperTest}.java`
- 검증 결과 (JDK 6u45 `javac 1.6.0_45`, 2026-11-16): JS-CORE 영역 컴파일 후 `TestRunner --scan` → **54 passed, 0 failed, 0 skipped**
  (테스트 클래스 5개 + 실행기 자체 시험 6개). `tools/sourcecheck --only encoding,forbidden,javadoc,loops` 내 소스 0건.
- 미해결 사항: `NumberText` 임시 대체 구현의 한계(위 7절). `JsRealm` 은 아직 평가/제한/모듈 API 가 없다(P13).
- 조정자 요청: 없음.
- 체크박스 후보: P12-01, P12-02, P12-03, P12-04 (P12-04 의 "정수 비트 연산" 은 변환 함수까지이며 연산자 구현은 P13-03 에서 검증).
