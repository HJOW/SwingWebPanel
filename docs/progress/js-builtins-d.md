# BUILTIN-D 진행 기록 (P14-07 Proxy/Reflect, P14-08 Promise, P14-09 Annex B, P14-10/11 적합성 목록)

담당 경로: `src/org/duckdns/hjow/swingwebpanel/js/builtins/meta/**`, `tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/meta/**`, 이 문서, `docs/progress/es2015-conformance.md`.
진입점: `org.duckdns.hjow.swingwebpanel.js.builtins.meta.MetaLibInstaller`(public 인수 없는 생성자, `StandardBuiltins` 가 이름으로 마지막에 적재).
빌드/시험: 코어의 작업 트리가 컴파일되지 않는 동안에는 마지막 commit 의 스냅샷 위에 내 경로만 덮어 컴파일하는 비공개 스크립트(`build/bid-sync.sh`, 저장소에 넣지 않음)를 썼다.
같은 내용을 `scripts/pkgtest.sh bid "js" "js/builtins/meta"` 로 시험한다(소스 패키지는 "js" 전체: 선택적 설치기가 Class.forName 으로 적재되므로).
규격 본문은 ECMA 사이트 HTTP 403 으로 읽지 못해 ES2015 지식에 의존했고 가정은 Javadoc 에 적었다(우회 시도 없음).

## 작업 단위 기록 (최신이 위)

### P14-09 이름/length/속성 점검 표 · P14-10 적합성 사례 목록 · P14-11 보고 · 스크립트 수준 시험 — 2026-11-16

```text
현재 작업 번호: P14-09(전체 내장 객체의 이름/length/속성 표), P14-10, P14-11, 그리고 내 영역의 스크립트 수준 시험
완료한 내용:
  - ConformanceTable(ES2015 18~26장과 부속서 B.2 의 전역/생성자/프로토타입/접근자/심볼 키 510 항목) + MetaConformanceTest: 표를 JsRealm.create() 의 realm 에 대해 훑어
    존재, 종류, name, length, 속성, 별칭 동일성, 프로토타입 체인, @@toStringTag 를 확인한다. 결과: 510 항목 모두 통과(없음 0, 틀림 0). 허용 목록(PENDING)은 비어 있다.
    표가 실제로 잡아내는지는 심은 어긋남 4건으로 확인(testWalkDetectsPlantedDeviations). 표에 없는 자체 프로퍼티(확장)와 Java/셸 전역 이름 부재도 확인한다.
  - Es2015LanguageProbeTest: 언어/라이브러리 경계의 ES2015 동작 77 사례(부속서 B 구문과 웹 호환 규칙, with/unscopables, 제너레이터/이터레이터 닫기, 클래스, eval/Function,
    Proxy 를 다른 라이브러리가 쓰는 경우, Promise 조합). 75 통과 + 2 보류(14.6 꼬리 호출, Function.prototype.caller/arguments)
  - ScriptLevelMetaTest: Proxy 13, Reflect 9, Promise 13, escape/unescape 3 스크립트와 호스트 거부 추적기 1건을 실제 인터프리터로 실행(모두 통과)
  - docs/progress/es2015-conformance.md: 사례 목록, 절별 상태, ES2015 와 다른 점(12건), 확장 목록, 미지원/보류, P14-10/11 판단
변경 파일: tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/meta/{ConformanceTable,MetaConformanceTest,Es2015LanguageProbeTest,ScriptLevelMetaTest}.java,
           docs/progress/{es2015-conformance,js-builtins-d}.md
검증 결과 (JDK 6u45, 작업 트리 전체 컴파일 `scripts/pkgtest.sh bid "js" "js/builtins/meta"`): 78 passed(내 71 + 기반 시설 7), 0 failed, 0 skipped. 엔진 전체 `"js" "js"`: 570 passed,
  1 failed(interp.LimitsTest.testStringLengthLimit, JS-CORE 의 P13-12 작업 중), 1 skipped(corelib.ScriptLevelTest, 이유 있음). tools/sourcecheck 내 24개 파일 0 위반.
미해결 사항:
  - 위 보류 2건(꼬리 호출, Function.prototype.caller/arguments 접근자). 소유자: JS-CORE/BUILTIN-A. 표와 사례는 구현되는 즉시 허용 목록에서 지우면 된다(낡은 행은 출력에 표시됨).
  - for-in 은 인터프리터가 ownKeys/getOwnPropertyDescriptor/getPrototypeOf 로 수행하므로 프록시의 enumerate trap 은 호출되지 않는다(ES2016 이후와 브라우저 동작). ES2015 원문을 따르려면
    JS-CORE 가 EnumerateSupport.enumerate(realm, obj) 를 쓰게 해야 한다(현재 설계상 필요 없음).
  - ES2015 본문(HTTP 403)을 읽지 못해 표와 사례의 기대 값은 지식 기반이다.
함정(다른 작업자도 겪음): `scripts/pkgtest.sh` 의 소스 패키지에 "js/builtins/xxx" 만 주면 선택적 설치기가 `Class.forName` 으로만 적재되어 컴파일되지 않고 realm 이 Math/Date/Map 등 없이
  만들어진다. 표준 realm 이 필요한 시험은 소스 패키지에 "js" 전체를 줄 것.
다음 작업 번호: 내 몫 끝. 조정자: P14-07, P14-08, P14-09, P14-10 체크 판단(아래), P15-06 에서 PromiseJobs/JsRealm.getJobQueue() 를 이벤트 루프에 연결
조정자 요청: (1) 위 보류 2건의 소유자 지정 (2) 통합 시 `JsRealm.runJobs()`/호스트 큐가 PromiseJobs 의 작업을 모두 처리하는지 확인(이미 같은 큐를 쓰도록 되어 있음: JsRealm.getJobQueue())
체크박스 후보: P14-07 (Proxy/Reflect 구현, Java 수준 35 사례(ProxyTest 15 + ProxyInvariantTest 12 + ReflectTest 8) + 스크립트 22 사례 통과), P14-08 (Promise 구현, 큐 순서 Java 19 + 스크립트 13 사례 통과),
  P14-09 (표 510 항목의 이름/length/속성 통과 + Annex B 사례; 모듈 관련 차이는 JS-CORE ModuleTest 의 몫), P14-10 (사례 목록과 실행 가능한 근거).
  P14-11 은 조건("미구현과 설명 없는 건너뜀 없음")에 걸리는 보류 2건 때문에 조정자 판단 필요.
```

### P14-07 / P14-08 / P14-09(escape·unescape) · Proxy, Reflect, Promise, 전역 escape/unescape — 2026-11-16

```text
현재 작업 번호: P14-07, P14-08, P14-09(내 몫인 Annex B.2.1)
완료한 내용:
  - Proxy: ProxyObject(JsObject 하위 클래스, [[GetPrototypeOf]]~[[Construct]] 와 [[Enumerate]] 전부 재정의, 9.5.1~9.5.14 의 불변 조건 검사,
    해제(revoke) 후 모든 연산 TypeError, IsArray 전달), 생성자(length 2, new 전용, prototype 없음), Proxy.revocable(length 2, revoke 는 이름 "" length 0)
  - Reflect: apply, construct(newTarget), defineProperty, deleteProperty, enumerate(ES2015 전용), get/set(receiver), getOwnPropertyDescriptor,
    getPrototypeOf, has, isExtensible, ownKeys, preventExtensions, setPrototypeOf. 이름/length/속성은 ES2015 표대로
  - Descriptors: ToPropertyDescriptor/FromPropertyDescriptor/CompletePropertyDescriptor/IsCompatiblePropertyDescriptor (public 이므로 다른 설치기가 재사용 가능)
  - EnumerateSupport: 일반 객체 [[Enumerate]]/for-in 이터레이터(%ForInIteratorPrototype%, 가려진 키·삭제된 키 처리). 인터프리터가 for-in 에 쓸 수 있는 공개 API
  - Promise: 생성자(length 1), 프로토타입(then 2, catch 1, @@toStringTag), all/race/resolve/reject, @@species getter, 해결 함수([[AlreadyResolved]] 공유),
    PromiseReactionJob/PromiseResolveThenableJob 은 작업 큐에 넣고 절대 동기 실행하지 않음, NewPromiseCapability/SpeciesConstructor, 반복자 오류 시 이터레이터 닫기
  - PromiseJobs: 작업 큐 선택(PromiseJobs.setQueue → realm 의 getJobQueue()(리플렉션, 코어가 추가하면 자동 사용) → 비공개 SimpleJobQueue 와 drain), 호스트의 처리되지 않은 거부 추적
    인터페이스 PromiseRejectionTracker(setRejectionTracker)
  - 전역 escape/unescape(Annex B.2.1, js.text.LegacyEscape 사용), 둘 다 length 1
변경 파일: src/org/duckdns/hjow/swingwebpanel/js/builtins/meta/{package-info,Descriptors,EnumerateSupport,LegacyBuiltins,MetaLibInstaller,ProxyBuiltins,ProxyObject,PromiseBuiltins,
           PromiseCapability,PromiseJobs,PromiseObject,PromiseReaction,PromiseRejectionTracker,ReflectBuiltins}.java,
           tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/meta/{MetaTestSupport,ProxyTest,ProxyInvariantTest,ReflectTest,PromiseTest,LegacyBuiltinsTest}.java
검증 결과 (JDK 6u45, 위 방식): 64 passed, 0 failed, 1 skipped(JarContentTest: 제품 JAR 없음, 이 작업과 무관한 기반 시설 사례). 내 사례 58건:
  ProxyTest 15 (생성자 모양, 전달, trap 인수/receiver, 호출·생성 trap, 해제, GetMethod 규칙, enumerate), ProxyInvariantTest 12 (trap 11종의 위반=TypeError 와 가장 가까운 적법한 답),
  ReflectTest 8, PromiseTest 19 (순서 검증 a1,a2,a3,b,a4 포함), LegacyBuiltinsTest 4.
  tools/sourcecheck (encoding, forbidden, javadoc, loops): 내 20개 파일 0 위반.
미해결 사항 / 가정:
  - ownKeys 의 중복 키 TypeError 는 후속 판(ES2017)의 규칙을 ES2015 보다 엄격하게 적용했다. [[GetOwnProperty]] 의 writable 불변 조건(ES2020)과 [[Delete]] 의 확장 불가 검사는 적용하지 않았다.
  - for-in 의 [[Enumerate]] 연결은 인터프리터(JS-CORE)가 `EnumerateSupport.enumerate(realm, obj)` 를 호출해야 한다. 코어에 훅이 없어 프록시의 enumerate trap 은 이 호출을 통해서만 동작한다.
  - 코어의 `JsException.describe()` 는 객체의 name/message 를 `getOwnPropertySlot`/`getPrototypeOf` 로 읽으므로 프록시이면 trap 이 호출될 수 있다(오류 메시지 만드는 중 부작용). 해제된 프록시는
    getOwnPropertySlot 이 null 을 돌려주어 예외가 나지 않게 했다. 코어가 이국적 객체를 건드리지 않게 바꾸면 더 안전하다.
  - JsRealm 에 getJobQueue()/작업 큐 API 가 아직 없어 PromiseJobs 가 리플렉션으로 찾는다(없으면 비공개 큐). 코어 변경 이력에 getJobQueue() 가 생기면 자동으로 쓰인다.
다음 작업 번호: P14-09 이름/length/속성 점검 표와 P14-10 conformance 목록 → P14-11 보고
조정자 요청:
  - StandardBuiltins 에 meta 설치기를 마지막에 한 줄로 적재(이미 합의됨). `Realm.evaluateScript` 가 생기면 스크립트 수준 시험(ScriptLevelMetaTest)이 자동으로 실행된다.
  - 통합 시 `PromiseJobs.setQueue(realm, queue)` 또는 `JsRealm.getJobQueue()` 를 호스트 이벤트 루프의 microtask 큐에 연결할 것.
체크박스 후보: (아직 없음. P14-07/P14-08 은 스크립트 수준 시험(인터프리터)이 통과해야 완료. 라이브러리 부분은 위 시험으로 검증됨)
```
