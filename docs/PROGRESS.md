# SwingWebPanel 진행 현황 (인수인계 문서)

이 문서는 작업 간 인수인계와 진행 상황 공유가 목적이다. 새 작업을 시작하는 AI/사람은
`AGENTS.md`의 "개발 계획"과 이 문서를 먼저 읽고, 선행 단계가 끝난 첫 번째 미완료 항목을 고른다.
작업을 끝낼 때마다 아래 "현재 상태" 블록과 "작업 로그"를 갱신한 뒤 commit 한다. (push 는 사용자에게 맡긴다.)

작업 시작일: 2026-11-16
기준 환경: Windows 10 Pro, JDK 6u45(`C:\Program Files\Java\jdk1.6.0_45`), 기본 `java` = Zulu OpenJDK 25.
JDK 7/8 도 설치되어 있다(`jdk1.7.0_80`, `jdk1.8.0_202`). 제품 검증은 JDK 6 으로만 한다.

## 현재 상태

```text
=== P29-04-d IEEE 754 반정밀도 (2026-11-26) ===
현재 작업 번호: P29-04-d 완료.
완료한 내용: binary64에서 binary16으로 한 번만 반올림하는 순수 Java 변환을 추가했다. Math.f16round, Float16Array, DataView 접근이 이를 공유한다.
변경 파일: Float16.java, MathLib.java, ElementType.java, DataViewLib.java, TypedLibInstaller.java, Es2026Float16Test.java, BuiltinPropertiesTest.java, PropertyAttributesTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 740건 통과. sourcecheck 0 위반.
미해결 사항: 가변 ArrayBuffer와 Uint8Array 인코딩(P29-04-e~f). Float16 호스트 경계는 P29-10에서 대조한다.
다음 작업 번호: P29-04-e.
-----------------------------------------------------------------------------------------------
=== P29-04-c2 Array.fromAsync 순차 소비 (2026-11-26) ===
현재 작업 번호: P29-04-c 완료.
완료한 내용: 비동기·동기 이터레이터와 유사 배열을 입력으로 받는다. 규격에 따라 필요한 값과 매퍼를 기다리고 결과를 쓴 뒤 다음 요소를 요청한다. 생성자 this와 오류 발생 시 iterator.return의 대기 및 원래 거부 이유를 검증했다.
변경 파일: ArrayBuiltins.java, ArrayFromAsync.java, ArrayBuiltinsTest.java, PropertyTableTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 737건 통과. sourcecheck 0 위반.
미해결 사항: Float16, 가변 ArrayBuffer 및 Uint8Array 인코딩(P29-04-d~f).
다음 작업 번호: P29-04-d.
-----------------------------------------------------------------------------------------------
=== P29-04-c1 내장 Promise 대기와 동기 이터레이터 변환 (2026-11-26) ===
현재 작업 번호: P29-04-c1 완료.
완료한 내용: await는 intrinsic PromiseResolve와 PerformPromiseThen을 호출한다. 비동기 래퍼의 동기 next 메서드를 한 번 읽고 done 뒤에 value를 읽으며, 이행 전 값이 거부되면 열린 이터레이터를 닫고 원래 오류를 전달한다.
변경 파일: PromiseBuiltins.java, PromiseCapability.java, PromiseInterop.java, AsyncIteratorHelper.java, IteratorHelper.java, ModernAsyncTest.java, ModernAsyncIteratorTest.java, AGENTS.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 734건 통과. Maven으로 생성한 엔진·브라우저 JAR을 클래스패스로 사용한 JRE 6u45 문서 모듈·Worker·AudioWorklet 54건 통과. sourcecheck 0 위반.
미해결 사항: Array.fromAsync의 입력 선택과 순차 소비(P29-04-c2).
다음 작업 번호: P29-04-c2.
-----------------------------------------------------------------------------------------------
=== P29-04-b TypedArray 역방향 탐색과 복사 (2026-11-26) ===
현재 작업 번호: P29-04-b 완료.
완료한 내용: TypedArray의 findLast, findLastIndex, toReversed, toSorted, with를 추가했다. 복사 메서드는 내장 요소 형식을 유지하고 원본의 사용자 정의 생성자를 조회하지 않는다. 버퍼 분리 뒤의 인덱스 조회와 값 변환 순서를 확인했다.
변경 파일: TypedArrayMethods.java, TypedArrayFactory.java, TypedArrayMethodsTest.java, BuiltinPropertiesTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 732건 통과. sourcecheck 0 위반.
미해결 사항: Array.fromAsync, Float16, 가변 ArrayBuffer 및 Uint8Array 인코딩(P29-04-c~f). 가변 버퍼의 메서드 경계는 P29-04-e에서 추가 검증한다.
다음 작업 번호: P29-04-c.
-----------------------------------------------------------------------------------------------
=== P29-04-a Array 역방향 탐색과 복사 (2026-11-26) ===
현재 작업 번호: P29-04-a 완료.
완료한 내용: findLast와 findLastIndex, toReversed, toSorted, toSpliced, with를 추가했다. 프로퍼티 읽기 순서, 희소 배열의 밀집 복사, 원본 constructor 미조회, 범위와 타입 오류 및 ES2026의 @@unscopables 목록을 검증했다.
변경 파일: ArrayMethods.java, ArrayMethodsTest.java, PropertyTableTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 730건 통과. sourcecheck 0 위반.
미해결 사항: TypedArray 복사·탐색, Array.fromAsync, Float16, 가변 ArrayBuffer, Uint8Array 인코딩(P29-04-b~f).
다음 작업 번호: P29-04-b.
-----------------------------------------------------------------------------------------------
=== P29-03-e3 AudioWorklet의 JSON 선취득 (2026-11-26) ===
현재 작업 번호: P29-03 완료.
완료한 내용: AudioWorklet에서 모듈 타입별로 그래프를 렐름 잠금 밖에서 미리 가져온다. JSON을 정적 의존성으로 링크하며 동적 import는 저장된 모듈이 있어도 TypeError로 거부한다. credentials: omit는 동일 오리진에서도 쿠키를 보내지 않는다.
변경 파일: ModuleGraphFetcher.java, WorkletScope.java, WorkletApi.java, ModuleSources.java, WorkerApi.java, JsRealm.java, ModuleRequest.java, Interpreter.java, ModuleRecord.java, AudioWorkletComponentTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: Maven의 AudioWorklet 필터 실행 62건 통과, 실시간 타이밍 1건 실패. 동일 JRE 6u45로 문서·AudioWorklet 집중 시험 16건 통과, 힙을 -Xms256m -Xmx512m으로 설정한 AudioWorklet 전체 63건 통과. sourcecheck 0 위반.
미해결 사항: 기본 힙의 그룹 실행에서 AudioWorkletRealtimeTest.testABusyWorkletDoesNotHoldTheRenderingThread의 최소 40 quantum 조건이 두 차례 실패(9, 20 quantum)했다. P29-04 이후 미완료.
다음 작업 번호: P29-04-a.
-----------------------------------------------------------------------------------------------
=== P29-03-e2 Worker의 JSON 모듈 (2026-11-26) ===
현재 작업 번호: P29-03-e2 완료.
완료한 내용: 일반 Worker에서 동적 import를, 모듈 Worker와 SharedWorker에서 정적·동적 의존성 로드를 연결했다. 모듈 Worker의 자격 증명 모드와 진입 요청의 동일 오리진 제한을 적용했다.
변경 파일: WorkerApi.java, WorkerScope.java, WorkerGlobals.java, PageRuntime.java, ModuleScriptSource.java, ModuleSources.java, HttpFetcher.java, RequestPurpose.java, WorkerErrorTest.java, WorkerMessagingTest.java, WorkerSharedTest.java, AGENTS.md, docs/progress/worker.md, 이 문서.
검증 결과: mvn -B -o -pl browser -am test -Dtest.filter=Worker (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 브라우저 52건 통과. sourcecheck 0 위반.
미해결 사항: AudioWorklet의 타입별 모듈 선취득(P29-03-e3).
다음 작업 번호: P29-03-e3.
-----------------------------------------------------------------------------------------------
=== P29-03-e1 브라우저 문서의 JSON 모듈 (2026-11-26) ===
현재 작업 번호: P29-03-e1 완료.
완료한 내용: 문서 스크립트 로더가 모듈 타입별로 MIME을 검사하고 UTF-8로 읽는다. 캐시는 URL과 속성으로 구분하고, 같은 오리진 자격 증명 모드를 리디렉션의 각 구간에 적용한다.
변경 파일: PageRuntime.java, ModuleScriptSource.java, ModuleSources.java, PageScripts.java, ModuleScriptHttpTest.java, AGENTS.md, 이 문서.
검증 결과: mvn -B -o -pl browser -am test -Dtest.filter=ModuleScript (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 브라우저 13건 통과. sourcecheck 0 위반.
미해결 사항: Worker와 AudioWorklet의 모듈 타입별 요청(P29-03-e2~e3).
다음 작업 번호: P29-03-e2.
-----------------------------------------------------------------------------------------------
=== P29-03-d 엔진의 JSON 합성 모듈 (2026-11-26) ===
현재 작업 번호: P29-03-d 완료.
완료한 내용: ModuleLoader.WithAttributes가 제공한 JSON 본문을 공통 JSON 파서로 읽고 default 하나만 내보낸다. 해석된 URL과 속성을 키로 삼아 모듈과 JSON 구문 오류를 공유한다. 미완성 순환 그래프의 재시도와 export * as 이름의 네임스페이스 연결도 수정했다.
변경 파일: ModuleLoader.java, ModuleRequest.java, ModuleRecord.java, ModuleAsyncEvaluation.java, Interpreter.java, JsonBuiltins.java, Parser.java, ProgramInfo.java, ModernDynamicImportTest.java, ModuleTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 727건 통과. sourcecheck 0 위반.
미해결 사항: 문서·Worker·AudioWorklet의 네트워크 요청과 MIME·CORS 검증(P29-03-e1~e3).
다음 작업 번호: P29-03-e1.
-----------------------------------------------------------------------------------------------
=== P29-03-c 동적 가져오기 옵션 (2026-11-26) ===
현재 작업 번호: P29-03-c 완료.
완료한 내용: 동적 import의 두 번째 인수와 후행 쉼표를 파싱한다. 옵션 객체의 with에서 열거 가능한 자체 문자열 키의 값만 읽고 호스트의 속성 제약을 확인한다. 인수 표현식의 예외는 동기 예외로 전달하며 지정자 문자열 변환과 속성 조회 중 오류부터 Promise를 거부한다.
변경 파일: ExpressionParser.java, AstDump.java, Expressions.java, Interpreter.java, ModuleRecord.java, ParserExpressionTest.java, ModernDynamicImportTest.java, AGENTS.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 724건 통과. sourcecheck 0 위반.
미해결 사항: type 속성에 따른 JSON 모듈 로드와 캐시, 브라우저 MIME·CORS 검사(P29-03-d~e).
다음 작업 번호: P29-03-d.
-----------------------------------------------------------------------------------------------
=== P29-03-b 정적 모듈 요청 속성 (2026-11-26) ===
현재 작업 번호: P29-03-b 완료.
완료한 내용: 정적 import와 재내보내기의 with 절을 파싱해 모듈 지정자와 문자열 속성을 요청 키에 보존한다. 중복 키와 문자열 구문 오류를 거부하고, 속성 순서와 무관하게 요청을 비교하며, 기존 지정자 목록을 유지한다.
변경 파일: ModuleRequest.java, ProgramInfo.java, Parser.java, ModuleRecord.java, ParserStatementTest.java, ModuleTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 722건 통과. sourcecheck 0 위반.
미해결 사항: 아직 로더가 처리하지 않는 속성은 가져오기 전에 거부한다. 동적 옵션과 JSON 모듈의 로더 연결은 P29-03-c~e에서 진행한다.
다음 작업 번호: P29-03-c.
-----------------------------------------------------------------------------------------------
=== P29-03-a Hashbang 주석 (2026-11-26) ===
현재 작업 번호: P29-03-a 완료.
완료한 내용: Script와 Module의 오프셋 0에 있는 해시뱅을 줄 주석처럼 읽는다. 이후 토큰의 줄과 열을 추적하고 eval 및 Function 생성자와 private 식별자에 미치는 영향을 검증했다.
변경 파일: Lexer.java, LexerTest.java, DynamicCodeTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 719건 통과. sourcecheck 0 위반.
미해결 사항: 정적·동적 가져오기 속성과 JSON 모듈 연결(P29-03-b~e).
다음 작업 번호: P29-03-b.
-----------------------------------------------------------------------------------------------
=== P29-02-h2b 프레임 WindowProxy와 메시지 출처 (2026-11-26) ===
현재 작업 번호: P29-02 완료.
완료한 내용: 창 참조를 프레임 브라우징 문맥에 연결했다. 참조를 유지한 채 이동하면 현재 문서 기준으로 오리진을 검사한다. 이동 중 이전 문서가 새 load 이벤트를 보내던 문제를 수정했다. 프레임 메시지는 호출 스크립트의 realm을 기준으로 보낸 창을 전달하므로 수신자는 event.source로 응답할 수 있다.
변경 파일: FrameContext.java, WindowProxy.java, FrameApi.java, CrossOriginWindow.java, WindowGlobals.java, PageScripts.java, MessagingApi.java, RealmExecution 및 인터프리터 실행 코드, FrameWindowTest.java, JsEngineBasicsTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, docs/ARCHITECTURE.md, 이 문서.
검증 결과: mvn -B -o -pl browser -am test -Dtest.filter=FrameWindowTest (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 8건 통과. 제품 테스트 클래스패스로 JRE 6u45의 프레임·메시징·페이지·Worker·AudioWorklet 102건 통과. sourcecheck 0 위반.
미해결 사항: 전체 회귀의 Web Audio·Worker 다섯 실패는 개별 재실행에서도 재현되어 P22/P25 검증 기록에 남긴다. WindowProxy의 최상위 문서 교체 수명은 P29-10에서 호스트 경계와 함께 대조한다.
다음 작업 번호: P29-03-a.
-----------------------------------------------------------------------------------------------
=== P29-02-h2a 실행 중인 realm 식별 (2026-11-26) ===
현재 작업 번호: P29-02-h2a 완료.
완료한 내용: JavaScript 실행과 내장 함수 진입에서 실행 realm을 스레드에 지정하고 종료 때 복원한다. 서로 다른 realm의 꼬리 호출은 대상 인터프리터에서 실행한다. 일반·엄격 호출, 생성자, 제너레이터, 모듈 및 예외 이후 복원 상태를 직접 프로퍼티 접근으로 관찰했다.
변경 파일: RealmExecution.java, NativeFunction.java, 인터프리터·함수·모듈 실행 코드, JsEngineBasicsTest.java, AGENTS.md, 이 문서.
검증 결과: 변경 클래스를 JDK 6u45로 build/p29-02-h2a에 컴파일하고 TestRunner --filter ExecutingRealm을 JRE 6u45로 실행했다. 1건 통과, 실패 0건. sourcecheck 0 위반.
미해결 사항: 브라우저의 창 프록시 연결과 접근 검사(P29-02-h2b), P29-03~10.
다음 작업 번호: P29-02-h2b.
-----------------------------------------------------------------------------------------------
=== P29-02-h1 호스트 지정 전역 this 값 (2026-11-26) ===
현재 작업 번호: P29-02-h1 완료.
완료한 내용: JsRealm을 만들 때 호스트 전역 this 값을 지정할 수 있다. 일반 스크립트, eval과 비엄격 함수는 이 값을 사용한다. 전역 식별자와 선언을 처리하는 전역 객체는 각 realm에 속한다. 독립 실행과 모듈 최상위 this를 함께 검증했다.
변경 파일: JsRealm.java, GlobalBuiltins.java, Interpreter.java, JsEngineBasicsTest.java, AGENTS.md, 이 문서.
검증 결과: 바뀐 엔진 소스를 JDK 6u45 javac -encoding UTF-8 -source 1.6 -target 1.6으로 build/p29-02-h1에 컴파일하고 기존 엔진 빌드 경로보다 앞에 지정하여 TestRunner의 JsEngineBasicsTest를 JRE 6u45로 실행했다. 18건 통과, 실패 0건, 건너뜀 0건. sourcecheck 0 위반.
미해결 사항: P29-02-h2의 실제 브라우저 WindowProxy 연동과 P29-03~10.
다음 작업 번호: P29-02-h2.
-----------------------------------------------------------------------------------------------
=== P29-02-g2 Unicode 18.0.0 대소문자와 정규식 그룹 이름 (2026-11-26) ===
현재 작업 번호: P29-02-g 완료.
완료한 내용: 문자열의 기본 전체 대소문자 변환과 정규식의 단순 접기·레거시 정규형을 Unicode 18.0.0으로 갱신했다. 어말 시그마와 다중 코드 포인트 매핑을 검증했다. 정규식 그룹 이름의 Unicode 이스케이프를 플래그와 관계없이 처리한다.
변경 파일: UnicodeCase.java, UnicodeProperties.java, UnicodePropertyData.java, TextTransform.java, CaseFold.java, RegExpParser.java, Es2022RegExpTest.java, tools/unicodegen, AGENTS.md, docs/FEATURE_MATRIX.md, docs/ARCHITECTURE.md, docs/progress/es2026.md, 이 문서.
검증 결과: mvn -B -o verify의 JDK 6u45 엔진 단계 715건 통과, 실패 0건, 건너뜀 0건. 재생성 SHA-256 일치. 독립 엔진 JAR 1,102,421바이트. sourcecheck 0 위반.
미해결 사항: JRE에 위임한 String#normalize 판본 차이는 P29-10 대조에 남겼다. P29-02-h의 WindowProxy와 P29-03~10은 미완료이다.
다음 작업 번호: P29-02-h1.
-----------------------------------------------------------------------------------------------
=== P29-02-g1 Unicode 18.0.0 공통 분류와 정규식 속성 (2026-11-26) ===
현재 작업 번호: P29-02-g1 완료.
완료한 내용: UCD 18.0.0에서 생성한 표를 렉서와 정규식에 연결했다. 정확한 이름과 별칭, Unassigned/Unknown, Script_Extensions 기본값과 대체 범위, 이모지 속성 거부를 검증했다. 동일한 속성의 별칭에 범위 데이터를 공유하도록 생성했다.
변경 파일: JsChars.java, CharClass.java, UnicodeProperties.java, UnicodePropertyData.java, RegExpCharSet.java, LICENSE-unicode.txt, Es2022RegExpTest.java, tools/unicodegen, AGENTS.md, docs/FEATURE_MATRIX.md, docs/ARCHITECTURE.md, docs/progress/es2026.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 713건 통과, 실패 0건, 건너뜀 0건. UnicodeGen을 JDK 6u45로 두 번 실행해 같은 SHA-256을 확인했다. 독립 엔진 JAR은 1,102,629에서 1,082,342바이트로 줄었다. sourcecheck 0 위반.
미해결 사항: P29-02-g2 대소문자 데이터, P29-02-h WindowProxy, P29-03~10.
다음 작업 번호: P29-02-g2.
-----------------------------------------------------------------------------------------------
=== P29-02-f 실행 환경별 globalThis 기술자 (2026-11-26) ===
현재 작업 번호: P29-02-f 완료. P29-02-h에서 이동을 가로지르는 WindowProxy를 추적한다.
완료한 내용: 독립 엔진, 일반 페이지, 같은 오리진 iframe, 전용·공유 Worker, AudioWorklet의 globalThis 값과 기술자를 검증했다. 독립 엔진에서 globalThis 대입과 삭제 뒤에도 스크립트 최상위 this가 유지되는지 확인했다.
변경 파일: JsEngineBasicsTest.java, PageScriptsTest.java, FrameWindowTest.java, WorkerGlobalScopeTest.java, AudioWorkletModulesTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: mvn -B -o -pl browser -am test -Dtest.filter=testGlobalThis (JDK 6u45 컴파일·실행, Maven은 JDK 8u202) 엔진 1건과 브라우저 4건 통과, 실패 0건. sourcecheck 0 위반.
미해결 사항: FrameApi는 같은 오리진에 문서별 전역 객체를 직접 반환한다. WindowProxy가 문맥 이동 후에도 같은 객체여야 하는 범위를 P29-02-h로 분리했다. P29-02-g1/g2 및 P29-03~10도 열려 있다.
다음 작업 번호: P29-02-g1.
-----------------------------------------------------------------------------------------------
=== P29-02-e Array.prototype.sort의 비교와 쓰기·삭제 순서 (2026-11-26) ===
현재 작업 번호: P29-02-e 완료.
완료한 내용: 기본 정렬에서 비교할 때 두 값을 문자열로 변환하고, 정렬 후 남은 모든 인덱스에 삭제 연산을 적용한다. 희소·상속 프로퍼티를 포함한 프록시 연산 순서, 비교 함수나 문자열 변환 예외, 삭제 실패 전에 이미 반영된 쓰기를 검증했다.
변경 파일: ArrayMethods.java, ArraySorter.java, Es2022BuiltinsTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45로 컴파일·실행, Maven은 JDK 8u202) 709건 통과, 실패 0건, 건너뜀 0건. sourcecheck 0 위반.
미해결 사항: P29-02-f~g와 P29-03~10.
다음 작업 번호: P29-02-f.
-----------------------------------------------------------------------------------------------
=== P29-02-d for-in 속성 열거 (2026-11-26) ===
현재 작업 번호: P29-02-d 완료.
완료한 내용: for-in을 지연 이터레이터에 연결하여 자체 키와 자체 속성 기술자로 열거한다. 정수 키 순서, 프로토타입 가림, 심볼 제외 및 제너레이터 중단 중에 삭제한 키 처리를 검증했다. 프록시의 ownKeys와 getOwnPropertyDescriptor를 사용하며 has와 ES2015 enumerate 트랩은 for-in에서 호출하지 않는다.
변경 파일: EnumerateSupport.java, Interpreter.java, Statements.java, ControlFlowTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45로 컴파일·실행, Maven은 JDK 8u202) 707건 통과, 실패 0건, 건너뜀 0건. sourcecheck 0 위반.
미해결 사항: P29-02-e~g와 P29-03~10. ES2015 Reflect.enumerate 공개 진입점의 제거는 P29-09에 둔다.
다음 작업 번호: P29-02-e.
-----------------------------------------------------------------------------------------------
=== P29-02-c Function.prototype.toString 원문과 네이티브 표현 (2026-11-26) ===
현재 작업 번호: P29-02-c 완료.
완료한 내용: 정적 메서드의 원문이 static 수정자부터 시작하는 문제를 바로잡았다. 명시적 클래스 생성자의 원문 범위는 파싱 중에 정한다. 네이티브 함수는 공개 name 속성의 변경이나 접근자 호출 없이 최초 이름을 사용한다. 네이티브 구문에 맞지 않는 호스트 이름은 익명 형태로 표시한다.
변경 파일: ExpressionParser.java, Classes.java, JsFunction.java, NativeFunction.java, FunctionLib.java, FunctionTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45로 컴파일·실행, Maven은 JDK 8u202) 705건 통과, 실패 0건, 건너뜀 0건. sourcecheck 0 위반.
미해결 사항: P29-02-d~g와 P29-03~10.
다음 작업 번호: P29-02-d.
-----------------------------------------------------------------------------------------------
=== P29-02-b JSON 상위 집합 문자열과 JSON.stringify 서로게이트 (2026-11-26) ===
현재 작업 번호: P29-02-b 완료.
완료한 내용: 문자열 리터럴에 U+2028/U+2029를 그대로 보존하고 토큰 위치에 반영한다. CR/LF 거부와 이스케이프 줄 연속 처리를 검증했다. JSON.stringify는 문자열 값과 키의 고립 서로게이트만 이스케이프하고 유효한 쌍은 보존한다. JSON.parse 역변환을 확인했다.
변경 파일: Lexer.java, JsonBuiltins.java, LexerTest.java, JsonTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: mvn -B -o -pl engine test (JDK 6u45로 컴파일·실행, Maven은 JDK 8u202) 703건 통과, 실패 0건, 건너뜀 0건. sourcecheck 0 위반.
미해결 사항: P29-02-c~g와 P29-03~10. 이후 사용자 지정 순서에 따라 P27을 진행한다.
다음 작업 번호: P29-02-c.
-----------------------------------------------------------------------------------------------
=== P29-02-a 태그 템플릿의 realm별 객체 동일성 및 이스케이프 검증 (2026-11-26) ===
현재 작업 번호: P29-02-a 완료.
완료한 내용: 컴파일된 스크립트를 두 realm에서 번갈아 실행할 때 템플릿 객체가 바뀌는 문제를 고쳤다. 살아 있는 파싱 노드별 객체를 각 realm의 평가기에서 보관한다. 잘못된 이스케이프의 cooked/raw, 비태그 조기 오류, 조각별 값, 배열 동결 및 서로 다른 파싱 위치의 구분을 확인했다.
변경 파일: Expressions.java, Node.java, JsEngineBasicsTest.java, AGENTS.md, docs/FEATURE_MATRIX.md, 이 문서와 작업 로그.
검증 결과: engine\test.bat --filter JsEngineBasicsTest -o (JDK 6u45로 컴파일·실행, Maven은 JDK 8u202) 16건 통과, 실패 0건. sourcecheck 0 위반.
미해결 사항: P29-02-b~g와 P29-03~10. 사용자 지정 순서에 따라 P29 다음 P27을 진행하고 나머지는 AGENTS.md 순서로 확인한다.
다음 작업 번호: P29-02-b.
-----------------------------------------------------------------------------------------------
=== P29-01 ECMAScript 2026 기준 판본과 전체 격차·호환 목록 확정 (2026-11-25) ===
현재 작업 번호: P29-01 완료.
완료한 내용:
  1. 오프라인 정본 docs/es2026/(ECMA-262 17th edition, June 2026, 53,284줄, 1~29장·부속서 A~F)과 tools/webfetch(JRE 25.0.5.0.2)로 확인한 공식 스냅샷(https://tc39.es/ecma262/2026/ HTTP 200 7,530,216B ETag "6b0628b1-72e6e8", https://raw.githubusercontent.com/tc39/ecma262/es2026/spec.html HTTP 200 2,979,557B, https://262.ecma-international.org/ HTTP 403 무우회)을 docs/FEATURE_MATRIX.md 0절에 고정하고, 제13판(docs/es2022/) 대비 추가된 모든 문법 생성규칙 5개 영역과 전 장을 7.1~7.7절에 매핑했다.
  2. JDK 6u45(C:\Program Files\Java\jdk1.6.0_45)에서 현행 swingwebpanel-js.jar에 대해 44건의 실측 감사를 수행해 정확한 출발 상태를 확정했다:
     - ES2015 기반 중 숨은 격차: js.text.CharClass가 JRE 6 Character(Unicode 4.0)에 위임되어 Unicode 4.1+ 식별자를 거부함(정규식 \p{}는 Unicode 13.0 표라 불일치 → P29-02 통합), ES2016에서 제거된 Reflect.enumerate 잔존(→ P29-09/10 제거).
     - ES2016~ES2022 잔여분: 태그 템플릿 개정·캐싱(ESX-SYNTAX-2018)과 독립 JsRealm globalThis 기술자는 동작 확인(경계·다중 문맥 시험 P29-02), 문자열 생 U+2028/U+2029 허용과 JSON.stringify 고립 서로게이트 이스케이프는 미구현 확인(P29-02), SharedArrayBuffer/Atomics는 미구현(P29-05).
     - ES2023~ES2026(ES26-* 23행): Hashbang, Import Attributes·JSON 모듈, 배열/형식화 배열/Float16Array/Uint8Array Base64·Hex/가변 ArrayBuffer, Weak Symbol, groupBy/Set 7종/Map·WeakMap getOrInsert*/Iterator 헬퍼·concat, isWellFormed/정규식 /v·중복 그룹·인라인 수정자·escape, Promise.withResolvers·try/Error.isError(브라우저 DomExceptionObjects 오류 브랜드 연동 포함)/Math.sumPrecise/JSON.rawJSON·reviver context/Date 시간대 오프셋 연산의 구현 클래스와 검증 시험 계획을 연결했다.
     - 이전 판본 및 부속서 B·E·F(ES-COMPAT-* 3행): B.1.1·B.2·B.3.1~3.4 및 Annex E·F 핵심 항목 동작 확인, 비엄격 for(var x = init in obj)(B.3.5)와 함수 호출 좌변 런타임 ReferenceError(B.3.9) 미구현 확인(→ P29-09).
  3. ECMA-262 제17판 §2 Conformance의 조건부 적합성 조항 및 JRE 6 CLDR 부재·JAR 용량 제약을 근거로 전역 Intl 미제공을 명시적 예외 EXC-16으로 확정하고, 비-ECMA-402 기본 toLocaleString/localeCompare(정준 동치 포함) 준수 범위를 고정했다. 상세는 docs/progress/es2026.md에 기록했다.
변경 파일: AGENTS.md, docs/PROGRESS.md, docs/FEATURE_MATRIX.md, docs/progress/es2026.md.
검증 결과: tools/webfetch(JRE 25) 스냅샷 HTTP 200 확인, JDK 6u45 엔진 실측 프로브 44건 실행 완료, 상단 보호 단락 무변경 git diff 확인, tools/sourcecheck 0 위반.
미해결 사항: P29-01에서 식별·고정한 코드 격차(P29-02~P29-10)는 순서대로 구현·검증한다.
다음 작업 번호: P29-02.
-----------------------------------------------------------------------------------------------
=== JavaScript 규격 목표 ES2026 상향과 작업 계획·순서 개편 (2026-11-25) ===
현재 작업 번호: ES2026 목표 상향에 따른 계획·순서 및 문서 개편 완료.
완료한 내용: 상단 "목표 - 지원 기능"이 ES2026 및 이전 버전 ES 호환으로 변경됨에 따라 AGENTS.md 개발 계획 전반과 진행 순서를 개편하고 P29(P29-01~10: ES2016~ES2022 잔여 항목, ES2023~ES2026 문법·내장 객체·이진 데이터·컬렉션·정규식·JSON 모듈, ES1~ES5.1 및 제17판 Annex B 하위 호환, 호스트·독립 엔진 경계와 적합성 표 확장)를 신설했다. 진행 순서는 P29(ES2026 완성) → P28-06~09(웹 API·완성된 실행기 기준 성능 최적화·NetstormJava 구동) → P27(postMessage) → P22 마무리로 재조정했다. docs/es2026(ECMA-262 제17판)과 docs/es2022를 전수 비교하여 docs/FEATURE_MATRIX.md(0절 판본, 7.5절 필수 승격, 7.6절 ES26-* 23행, 7.7절 ES-COMPAT-* 3행, 10·11·13절)와 ARCHITECTURE/API_CONTRACT/progress 문서를 갱신했다.
변경 파일: AGENTS.md, docs/PROGRESS.md, docs/FEATURE_MATRIX.md, docs/ARCHITECTURE.md, docs/API_CONTRACT.md, docs/progress/netstorm.md, docs/progress/es2015-conformance.md, docs/progress/README.md.
검증 결과: 문서만 변경했다. 상단 보호 단락("프로젝트 목표", "목표 - 지원 기능", "주의사항") 무변경 git diff 확인 및 tools/sourcecheck UTF-8·규칙 검사 통과.
미해결 사항: 상단 "목표 - 지원 기능" 안내 줄에 적힌 docs/es2022 표기는 보호 단락 규칙에 따라 수정하지 않고, 실제 제17판 오프라인 사본 경로인 docs/es2026/과 함께 개발 계획·기능 표에 안내했다. P29 구현 전이므로 ES26-* 행과 일부 ESX-* 잔여 행은 미구현/부분 구현 상태이다.
다음 작업 번호: P29-01.
-----------------------------------------------------------------------------------------------
=== P28-05 ES2022 현대 내장 객체 완료 (2026-11-25) ===
현재 작업 번호: P28-05 완료(P28-05-a~c).
완료한 내용: Object/Array/String/TypedArray/Symbol/Promise/Error 확장, Error cause/AggregateError, named group·lookbehind·/d·matchAll, Unicode 13 일반 범주·Script/Script_Extensions·이진 속성, WeakRef/FinalizationRegistry와 ES2019 안정 sort를 구현했다. Unicode 표는 tools/unicodegen(Java 6)이 생성하며 사용자 지시로 Emoji 13 속성은 EXC-15로 제외했다. Netstorm 원본 경로는 H:\Workspace\git\NetstormReborn\webpj\www로 정정했다.
변경 파일: engine js core/builtins/regexp와 시험·적합성 표, UnicodePropertyData 생성 소스, tools/unicodegen, AGENTS/README/ARCHITECTURE/FEATURE_MATRIX/netstorm, 이 문서.
검증 결과: P28 집중 `Es2022` 15 passed/0 failed, 기존+신규 RegExp 36 passed/0 failed(실제 실행 묶음에서 35개 표), WeakRef GC 수거 시험 3회 연속 통과, 전체 엔진 699 passed/0 failed, Java 6 source/target 컴파일과 JAR 경계 시험 통과, P28 변경 영역 sourcecheck 0 violation/101 files. `test.bat -pl browser -am`은 엔진 699/0 뒤 브라우저 3685 passed/1 failed/14 skipped였고 실패는 기존 시간 민감 `AudioDecodeServiceTest.testCloseDuringADecoding`; 해당 시험을 3회 반복 실행하면 모두 통과했다.
미해결 사항: Emoji 전용 정규식 속성은 EXC-15(의도적 제외). 브라우저 전체 실행의 단일 시간 민감 시험은 재현 실패 없이 3회 단독 통과했으나 3685/1 기록은 남겨 둔다.
다음 작업 번호: P28-06.
-----------------------------------------------------------------------------------------------
=== P28-05-a 현대 내장 객체 1차 묶음 (2026-11-25) ===
현재 작업 번호: P28-05-a 완료. P28-05는 계속 진행.
완료한 내용: Object.entries/values/fromEntries/getOwnPropertyDescriptors/hasOwn, Array.flat/flatMap/at, String.at/replaceAll/matchAll 표면, TypedArray.at, Promise.allSettled/any/finally, Symbol.description/asyncIterator/matchAll, Error cause와 AggregateError를 구현했다.
변경 파일: engine js WellKnownSymbols/ErrorKind와 corelib ObjectLib/SymbolLib/ErrorLib, stringarray ArrayMethods/StringBuiltins, typed TypedArrayMethods, meta PromiseBuiltins, Es2022BuiltinsTest, AGENTS.md, 이 문서.
검증 결과: `.\engine\test.bat --filter js.interp.Es2022BuiltinsTest` JDK 6u45: 6 passed/0 failed. engine package JDK 6 source/target 컴파일 성공.
미해결 사항: String/RegExp matchAll의 실제 @@matchAll iterator·named groups·d 플래그·lookbehind·Unicode properties, WeakRef/FinalizationRegistry와 P28-05 전체 회귀는 다음 작업.
다음 작업 번호: P28-05-b.
-----------------------------------------------------------------------------------------------
=== P28-04 BigInt 완료 (2026-11-25) ===
현재 작업 번호: P28-04 완료(P28-04-a~d).
완료한 내용: BigInt 원시값·연산·래퍼/BigInt 내장 객체, BigInt64Array/BigUint64Array/DataView BigInt 접근, JSON TypeError, 구조화 복제, java.math.BigInteger/JSR 223 왕복과 문서를 구현했다.
변경 파일: engine js core/interp/typed/datajson/embed/script와 시험, browser StructuredClone와 시험, AGENTS.md, README.md, docs/FEATURE_MATRIX.md, docs/progress/netstorm.md, 이 문서.
검증 결과: BigIntCore 5, BigIntTypedArray 4, JsEngineValues 11, JsScriptEngine 14, StructuredClone 16건 통과. 전체 엔진 684/0. 전체 브라우저 실행 3,687 passed/1 failed/12 skipped였고 실패는 실행마다 다른 기존 시간 의존 시험(MetaRefresh, Web Audio thread close) 한 건이었으며 둘 다 단독 재실행 통과. sourcecheck 0 violation/1,964 files. netstormparse 번들 3,283,377자·module 1,059·import 2,486·syntax/missing 0.
미해결 사항: 실제 Netstorm 번들 실행은 P28-05~08에서 계속한다.
다음 작업 번호: P28-05.
-----------------------------------------------------------------------------------------------
=== P28-04-c BigInt JSON·구조화 복제·Java 변환 (2026-11-25) ===
현재 작업 번호: P28-04-c 완료.
완료한 내용: JSON BigInt TypeError와 replacer 변환, StructuredClone의 BigInt 원시/래퍼/BigInt 형식화 배열 복제, java.math.BigInteger ↔ script BigInt와 javax.script 경계 변환을 구현했다.
변경 파일: engine JsonBuiltins/JavaValues/JsEngine와 embed/script 시험, browser StructuredClone/ClonedValue/StructuredCloneTest, BigIntCoreTest, AGENTS.md/이 문서.
검증 결과: BigIntCoreTest 5, JsEngineValuesTest 11, JsScriptEngineTest 14, StructuredCloneTest 16건 JRE 6u45 통과. sourcecheck 0 위반.
미해결 사항: P28-04-d 전체 엔진/브라우저 회귀·외부 번들 구문 검사·기능 표/문서 최종 갱신.
다음 작업 번호: P28-04-d.
-----------------------------------------------------------------------------------------------
=== P28-04-b BigInt 형식화 배열과 DataView (2026-11-25) ===
현재 작업 번호: P28-04-b 완료.
완료한 내용: BigInt64Array/BigUint64Array, 64비트 감싸기·엔디언·형식 변환, DataView get/setBigInt64/get/setBigUint64와 모든 typed-array 메서드의 BigInt 보존을 구현했다.
변경 파일: engine/src/main/java/.../js/builtins/typed/{ElementType,TypedArrayObject,TypedArrayMethods,TypedArrayFactory,TypedArrayIterator,DataViewLib,TypedArrayLib,package-info}, BigIntTypedArrayTest, BuiltinPropertiesTest, AGENTS.md/이 문서.
검증 결과: BigIntTypedArrayTest 4건과 형식화 배열·DataView 전체 86건 JRE 6u45 통과. sourcecheck 0 위반.
미해결 사항: JSON/구조화 복제/Java 변환(P28-04-c), 전체 문서·회귀(P28-04-d).
다음 작업 번호: P28-04-c.
-----------------------------------------------------------------------------------------------
=== P28-04-a BigInt 원시값·연산·내장 객체 (2026-11-25) ===
현재 작업 번호: P28-04-a 완료.
완료한 내용: JsBigInt/JsType.BIGINT, typeof, ToNumeric/ToBigInt, 산술·비트·시프트·비교·갱신 연산, BigInt 래퍼와 BigInt()/asIntN/asUintN/toString/valueOf를 구현했다.
변경 파일: engine/src/main/java/.../js/JsBigInt·JsType·JsValue·Js·Ops·JsRealm, parser/ExpressionParser, ast/AstDump, interp/Expressions, corelib/BigIntLib·CoreLibInstaller, BigIntCoreTest와 PropertyAttributesTest, AGENTS.md/이 문서.
검증 결과: 진행 중 작업의 집중 시험 BigIntCoreTest 4건, 핵심 속성 표 PropertyAttributesTest 4건, 기존 parser 40건 통과. 전체 엔진·최종 sourcecheck는 P28-04-d에서 다시 실행한다.
미해결 사항: BigInt64Array/DataView(P28-04-b), JSON/구조화 복제/Java 변환(P28-04-c), 전체 문서·회귀(P28-04-d).
다음 작업 번호: P28-04-b.
-----------------------------------------------------------------------------------------------
=== P28-03 ES2016~ES2022 실행 의미 완료 (2026-11-25) ===
현재 작업 번호: P28-03 완료(P28-03-a~f).
완료한 내용: Number 지수·spread/rest·optional/nullish·논리 대입, class field/private/static block, async function/await, async generator/yield*/for-await, dynamic import/import.meta/top-level await, 페이지 dispose와 실행 제한에서의 재개 중단을 구현했다. BigInt 실행 의미는 P28-04로 분리했다.
변경 파일: AGENTS.md(P28-03 완료), engine/src/main/java js·interp, browser PageScripts, ModernExpression/Class/Async/AsyncIterator/DynamicImport 시험과 PageDisposal/ModuleScript 시험, docs/FEATURE_MATRIX.md, 이 문서.
검증 결과: `test.bat -pl browser -am` JDK 6u45 엔진 675 passed/0 failed/0 skipped, 브라우저 3,688 passed/0 failed/12 skipped. sourcecheck 0 위반. netstormparse 번들 3,283,377자, module 1,059, import 2,486, syntax/missing 0.
미해결 사항: 실제 Netstorm 게임 실행은 P28-04의 BigInt와 P28-08 통합 구동에서 계속한다.
다음 작업 번호: P28-04.
-----------------------------------------------------------------------------------------------
=== P28-02 ES2016~ES2022 파서 완료 (2026-11-25) ===
현재 작업 번호: P28-02 완료(P28-02-a~f).
완료한 내용: P28-02-a~e의 문법 구현을 마쳤고 P28-02-f용 Java 6 도구 `tools/netstormparse`를 추가했다. 외부 Netstorm 번들 3,283,377 UTF-16 문자, `debug.html` inline module 1개, 현재 원본 모듈 1,059개, static import 2,486건을 자체 파서로 구문 오류 0·missing module 0으로 확인했다. 계획 당시 1,057개에서 원본이 두 파일 늘어난 현재 개수를 기록한다. 외부 검증 중 단항 위치의 await, await 뒤 async function IIFE 호출, 정규식 `/d` 플래그 어휘 인식의 실제 파서 결함을 고쳤다.
변경 파일: AGENTS.md(P28-02/f 완료), engine/src/main/java/.../ast·parser, engine/src/test/java/.../parser, tools/netstormparse/{AGENTS,README,build/run,src}, docs/ARCHITECTURE.md, docs/FEATURE_MATRIX.md, docs/progress/netstorm.md, 이 문서.
검증 결과: `engine\test.bat --filter js.parser` JDK 6u45 40 passed/0 failed/0 skipped. `tools/netstormparse/run.bat H:\Workspace\git\NetstormReborn\webpj\www` JRE 6u45 syntax-errors 0, missing-modules 0. 제품과 도구 모두 Java 6 source/target 컴파일.
미해결 사항: 파서는 완료되었지만 async/optional chain/class field/BigInt 등의 실행 의미는 P28-03~05, 정규식 `/d`의 indices 결과는 P28-05이다.
다음 작업 번호: P28-03.
-----------------------------------------------------------------------------------------------
=== P28-01 ECMAScript 2022 기준 판본과 대상 목록 확정 (2026-11-25) ===
현재 작업 번호: P28-01 완료.
완료한 내용: ECMA-262 제13판(ECMAScript 2022, June 2022)을 P28 기준 판본으로 고정했다. 기능 표 7.5절을 ES2016~ES2022의 문법/실행 의미/내장 객체 행으로 다시 나누고 각 행에 NetstormJava 번들 사용 횟수나 사용 여부를 적었다. ES2023 이후 기능은 ESX-OUT-2023+로 분리했다. `**` 구문 오류와 이름 있는 정규식 캡처의 `groups`/`$<name>` 미구현 판정을 명시적으로 유지했다. NetstormReborn 원본 소스 위치를 사용자 제공 경로 `H:\Workspace\git\NetstormReborn\webpj\www`로 정정했다.
변경 파일: AGENTS.md(P28-01 완료 표시), docs/FEATURE_MATRIX.md(0절 판본 표, 7.5절 ESX 목록), docs/progress/netstorm.md(경로와 8절 판본 조회 기록), 이 문서.
검증 결과: 저장소의 [`docs/es2022`](es2022/) 오프라인 규격서 서문에서 제13판과 ES2016~ES2022 추가 기능 목록을 확인했다. `tools/webfetch`를 JRE 25.0.5.0.2로 실행해 공식 HTML/PDF URL은 모두 HTTP 403임을 확인했고(User-Agent 우회 안 함), TC39 공식 저장소 `es2022/spec.html`은 HTTP 200, 2,657,094바이트, ETag ffa4eed67e5c1aeee0f8bd4740f43a1ade7a8f0345195ac3b267ac110f7ef11a로 조회했다. 정정한 Netstorm 원본 경로와 `dist/netstorm.bundle.js`(3,445,839바이트)가 실제로 존재함을 확인했다. 문서 변경만 수행했다.
미해결 사항: P28-02부터 실제 렉서/파서 구현과 시험이 필요하다. ECMA 공식 URL은 계속 접근 제한되므로 직접 조회 성공 여부는 다음 조회에서도 장담하지 않는다.
다음 작업 번호: P28-02.
-----------------------------------------------------------------------------------------------
=== NetstormJava 게임 구동 점검과 P28 계획 추가 (2026-11-24) ===
현재 작업 번호: P28 계획 추가 완료. P28-01은 위 2026-11-25 기록에서 완료했다.
완료한 내용: 사용자 프로젝트 NetstormJava(`H:\Workspace\eclipse photon\NetstormJava`, `setPage("classpath://www/index.html")`)의 구동 오류를 JDK 7u80과 JDK 6u45에서 재현했다. 오류는 `classpath://www/dist/netstorm.bundle.js:1:782 Uncaught Invalid or unexpected token`이고, 원인은 ES2022 private 메서드 `#e`이다. 번들(webpack, 변환 없음)이 쓰는 ES2016~ES2022 문법을 집계했고, 엔진 단독과 실제 페이지에서 기능별로 판정했다(엔진 언어 판정 49개 중 14개 통과). 웹 API는 대부분 동작했고, 없는 것은 `addEventListener`의 `signal`, `ReadableStream`, `navigator.clipboard`, `Blob`/`FileReader`/`createObjectURL` 중 일부였다. 계획 P28(P28-01~09)을 AGENTS.md에 추가했고, 기능 표 7.5절에 `ESX-*` 행을 만들었다. ES-EXPR 행의 `**` 검증 완료 표기는 실제와 달라 바로잡았다.
변경 파일: AGENTS.md(P22-16 메모, 공통 사항 제목, P28, 중간 실행 목표 표), docs/FEATURE_MATRIX.md(ES-EXPR, 7.5절), docs/progress/netstorm.md(새 문서), 이 문서.
검증 결과: 문서만 변경했다. 판정에 쓴 임시 프로그램(헤드리스 페이지 실행기, 엔진 기능 판정기, acorn 집계 스크립트)은 저장소 밖의 세션 임시 디렉터리에서 실행했다. 재현 조건과 결과는 netstorm.md 1~4절에 있다.
미해결 사항: 사용자 지시로 첫 오류 뒤의 실행(그리기·소리·입력·성능)과 `debug.html` 경로는 확인하지 않았다(P28-08). `blob:` 주소 정책은 사용자 결정이 필요하다(P28-06).
다음 작업 번호: P27-01(기존 순서). P28은 P27에 의존하지 않으므로 사용자가 지시하면 P28-01부터 먼저 진행한다.
-----------------------------------------------------------------------------------------------
=== Maven Central 배포 설정 보완 (2026-11-24) ===
현재 작업 번호: P23 후속 배포 확인.
완료한 내용: release 프로필을 적용하는 Central 배포 명령과 업로드 없이 서명된 번들을 만드는 옵션을 문서화했다. Central 플러그인은 0.12.0을 사용한다.
변경 파일: pom.xml, README.md, docs/progress/maven.md, 이 문서.
검증 결과: 기본 배포 플러그인의 저장소 누락 오류를 재현했다. release 프로필의 로컬 번들 생성, 세 모듈의 GPG 서명 10개, JAR 시험 7건을 확인했다.
미해결 사항: 실제 Portal 토큰과 게시 계정으로 수행하는 Central 게시.
다음 작업 번호: P27-01.
-----------------------------------------------------------------------------------------------
=== Maven 전환 완료 (2026-11-24) ===
현재 작업 번호: P23-02~07 완료.
완료한 내용: Maven 부모 POM과 엔진·브라우저·예제 모듈을 연결했다. JDK 6 빌드와 자체 시험, 기존 제품 JAR 세 개, 배포 프로필을 연결했다.
변경 파일: pom.xml과 모듈 POM, Maven 표준 소스·리소스 디렉터리, 실행 스크립트, 도구의 경로 설정, README와 설계·진행 문서.
검증 결과: Windows에서 Maven 3.8.8/JDK 8u202로 mvn -o verify 실행. JDK 6u45 회귀 시험 4,334건 통과, 실패 0건, 선택 Opus 벡터 12건 건너뜀. 기존 JAR 세 개와 항목·MANIFEST 일치, 모든 클래스 major 50.
미해결 사항: 선택 Opus 원본 벡터가 있는 환경에서는 해당 12건을 추가로 실행할 수 있다. release 프로필은 소스·Javadoc 생성까지 검증했다.
다음 작업 번호: P27-01.
-----------------------------------------------------------------------------------------------
=== 이전 Maven 1단계 점검 (2026-11-24) ===
현재 작업 번호: P23 Maven 설정 1단계(`ceef525`) 점검. P23-01 완료, P23-02~07 미완료.
환경: Maven 3.8.8이 JDK 8u202로 실행된다. JDK 6u45도 설치되어 있고 사용자 toolchains.xml은 없다.
검증 결과: 커밋의 깨끗한 복사본에서 Maven validate는 성공했고 test와 package는 엔진 클래스 누락으로 컴파일에서 실패했다. 기존 JDK 6 스크립트의 JAR 관련 시험 7건은 통과했다.
다음 작업 번호: 기존 순서대로 P27-01. Maven 전환 재개 시 P23-02부터 진행한다.
-----------------------------------------------------------------------------------------------
=== 이전 요약 (2026-11-22 밤) ===
현재 작업 번호: P25(Web Audio)·P24(콘솔 이벤트)·P26(엔진 분리) 완료, P23-01(Maven 도입 방식 결정) 완료. **2026-11-22 사용자 요청으로 P27(Java 호스트와 페이지 스크립트 간 메시지 통신, `postMessage`/`message` 이벤트)을 계획에 추가했고 P23(Maven 전환)보다 먼저 한다.** 다음 작업은 P27-01(계약 확정; 기본안은 AGENTS.md P27, 계약 초안은 `docs/API_CONTRACT.md` 5.22)이다. P23-02(Maven 표준 구조 전환)는 P27 뒤에 하며 이 PC 에 `mvn` 이 없어 막혀 있다.
이 날(2026-11-22)에 끝낸 것: P24 전부, P26 전부(`engine/` 분리, `js.embed`, `javax.script` 제공자, JAR 셋), P25-01~06, P25-08(Web Audio: 그래프 엔진, 노드 전부, 디코딩, JavaScript 바인딩, 실시간 출력, `<audio>` 연결, `ScriptProcessorNode`, AudioWorklet, 실제 Chromium 156 과의 비교 시험), P23-01. 마지막 전체 회귀(JDK 6u45, `BUILD_DIR=build sh test.sh`): 4331 통과/1 실패/14 건너뜀(실패 `XhrTest.testTimeout` 은 시험을 고쳐 8회 연속 통과한 뒤 병합; 건너뜀 14건은 선택적 Opus 원본 벡터 12건과 Java Sound 믹서가 필요한 2건).
미해결 사항(AGENTS.md 의 `[ ]`):
  - AI 가 다음에 할 것: P27-01~06(Java 호스트 메시지. 계획과 계약 초안만 있고 코드는 없다; 기능 표 API-30 은 `미구현`).
  - 사람이 해야 하는 것: P25-07, P18-08, P22-10, P22-07(소리가 나는 장치가 있는 PC 에서 실제 소리 청취와 JRE 6 실제 실행 확인; `docs/progress/media-play.md` 미확인 표에 항목이 있다), P18-11(P18-08 뒤).
  - Maven 설치가 필요한 것: P23-02~07(Maven 3.9 이상을 JDK 8 로 실행, `~/.m2/toolchains.xml` 에 JDK 6u45 정의; 절차는 `docs/progress/maven.md` 5절).
  - 마지막에 할 것: P22-15(이 인수인계), P22-16(요구사항 최종 대조; 위 항목들이 끝나야 체크).
빌드/시험: `build.sh`/`test.sh`(엔진 단계 + 브라우저 단계, 시험 JVM 은 `-XX:MaxPermSize=256m`), 소규모는 `scripts/pkgtest.sh`(JavaScript/컴포넌트 시험은 소스 패키지에 `js dom html css net storage core content layout render download media @root`). 소스 규칙 검사 `sh tools/sourcecheck/run.sh <경로>`. 회귀가 전체에서만 가끔 실패하는 시험은 단독으로 3~6회 돌려 확인하고 시간 의존 구조면 고친다(이 PC 는 다른 프로그램이 함께 돌아 시간 시험이 흔들린다).
저장소 상태: `main`(HEAD `20261122-020`, P27 계획 추가), 미병합 브랜치 없음(작업자 worktree 브랜치 `worktree-agent-*` 는 모두 `main` 에 병합되었고 지워도 된다), push 는 하지 않았다.
-----------------------------------------------------------------------------------------------
현재 작업 번호: 2026-11-20 사용자 요청으로 AGENTS.md 개발 계획에 P23~P26 을 추가했다(구현은 시작하지 않음). 그 전 상태는 아래 "2026-11-19 재개 세션" 절.
완료한 내용:
  - 2026-11-20 계획 추가(문서만): P23 Maven 프로젝트 전환, P24 콘솔 출력 이벤트(console.log/console.error 리스너 통지), P25 Web Audio API, P26 JavaScript 엔진 분리와 Java Scripting API(javax.script) 지원. 진행 순서는 P23 → P26. P22-16 에 "P23~P26 도 끝나야 전체 개발 완료" 메모, "중간 실행 목표" 표에 행 추가. 상단 세 단락(프로젝트 목표/목표 - 지원 기능/주의사항)은 고치지 않았다.
  - 2026-11-20 주 패키지를 `swingwebpanel` 에서 `org.duckdns.hjow.swingwebpanel` 로 변경했다(소스 경로 `src/org/duckdns/hjow/swingwebpanel`). 자세한 내용과 미병합 브랜치에 미치는 영향은 아래 "2026-11-20 주 패키지명 변경" 절. 전체 회귀 3752 통과/0 실패/12 건너뜀(JDK 6u45).
  - CSS-L 마무리와 병합: JRE 의 Lucida 글꼴로 만든 시험 자료(WOFF2 네 개, Brotli 스트림 하나)를 지우고 브랜치 이력에서도 지운 뒤(filter-branch) 시험을 합성 글꼴과 시험 때 JRE 글꼴 변환으로 바꿨다. 병합 뒤 전체 3644 통과/0 실패/14 건너뜀.
  - MEDIA-D 마무리와 병합: MP3 탐색의 비트 저장소 사전 해독(WIP 수정이 실제로 필요함을 확인), 탐색 시험의 위치 계산 결함 수정, media-matrix 11절 작성. 병합 회귀 3658 íµê³¼/1 ì¤í¨(FontDisplayIntegrationTest.testOptionalFontThatArrivesAtOnceIsUsed ìê° ì´ê³¼, ë¨ë ì¬ì¤í 4/4 íµê³¼)/14 ê±´ëë.
  - P10-08-g 구현(font-display block 기간의 보이지 않는 텍스트와 기간 끝 다시 그리기), P10-08-e 보강(독립 인코더 fontTools 의 WOFF2 세 개). AGENTS 에서 P10-08-d/e/f/g 체크, P10-08-h(남은 CSS 항목) 신설.
  - FEATURE_MATRIX 13절 개수표 재집계, ARCHITECTURE 에 CSS-L 구성과 tools/brotlidict.
변경 파일: `git log` 의 20261119 커밋들.
검증 결과: 최종 전체 회귀(JDK 6u45, P10-08-g 와 MEDIA-D 를 모두 포함한 master) 3668 íµê³¼/0 ì¤í¨/14 ê±´ëë. 건너뜀은 선택적 Opus 원본 벡터 12건과 Java Sound 믹서가 필요한 실제 오디오 라인 시험 2건.
미해결 사항: 남은 `[ ]`: P10-08-h(와 상위 P10-08), P11-02/P11-10(한글 IME, 사람), P18-08(소리 청취, 사람), P18-11(P18 마지막 항목이라 P18-08 뒤에), P22-07/10/15/16(마무리). 미병합 브랜치 없음. 보존 WIP 브랜치: `wip/cssi-page-unverified`(지워도 됨), `wip/frames-forms-unverified`.
다음 작업 번호: P10-08-h(남은 CSS 항목을 구현하거나 근거 있는 예외로 확정) → P22-07/10/15/16 마무리 문서. 사람이 할 일은 "2026-11-19 재개 세션" 3번.
  (2026-11-20 갱신) AGENTS.md 기준으로 P10-08-h, P11-02, P11-10 은 이미 체크되어 있다. 남은 `[ ]` 는 P18-08, P18-11, P22-07/10/15/16 과 새 P23~P26 이다. 사람이 확인해야 하는 항목을 빼면 AI 가 진행할 수 있는 첫 항목은 P23-01(Maven 도입 방식 조사·결정)이다.
  (2026-11-22 갱신) **사용자 지시로 진행 순서를 바꿨다: P23(Maven 전환)은 최대한 나중으로 미룬다.** 순서는 P24 → P25 → P26 → P23(마지막)이며, 그 전까지 현재 `src`/`tests/src` 구조와 `build.sh`/`test.sh` 를 쓴다. 이 PC 에는 Maven 이 없고 사용자가 직접 설치하겠다고 했다(P23 을 시작할 때 `mvn` 이 PATH 에 있는지 먼저 확인하고 없으면 사용자에게 요청한다). P26 은 Maven 없이 `engine/` 디렉터리 + 기존 스크립트로 분리하고 P23 에서 그 디렉터리를 Maven 모듈로 옮긴다(`docs/progress/briefs/engine.md`).
  (2026-11-22) **P24 완료**(아래 작업 로그). 다음: P25(Web Audio)와 P26(엔진 분리)을 작업자 두 명이 병렬로(`docs/progress/briefs/webaudio.md`, `engine.md`), 조정자는 병합·전체 회귀를 맡는다.
  (2026-11-22) **P26 완료·main 병합**(커밋 `20261122-200`~`204`, 엔진 작업자). 엔진을 `engine/` 으로 분리하고(`engine/src`, `engine/tests`, `engine/resources`) 독립 API `js.embed`, `javax.script` 제공자 `js.script`, JAR 세 개를 만들었다. **빌드/시험 구조가 바뀌었다**: `build.sh|bat` 는 엔진→브라우저→합친 클래스 순서로 빌드하고 `test.sh|bat` 는 엔진 단계(엔진 클래스만, headless)와 브라우저 단계를 차례로 돌려 `=== TOTAL (engine + browser): ... ===` 를 출력한다. 엔진 소스 경로는 `src/.../js/{*.java,ast,parser,interp,builtins,date,num,regexp,text}` → `engine/src/...` (브라우저는 `js.host/dom/worker` 만 `src` 에 남음). 전체 회귀(조정자가 직접 재실행, JDK 6u45): **3823 통과/0 실패/14 건너뜀**(엔진 648 + 브라우저 3175). 상세 `docs/progress/engine.md`.
  (2026-11-22) **P25 A1 병합**(그래프 엔진·AudioParam·1차 노드, 커밋 `20261122-100`~`109`, `media/webaudio`). AGENTS 에서 P25-01, P25-03 체크, P25-02/04 는 미완료 유지(실시간 오디오 스레드는 C, 남은 노드는 A2). 진행 중 작업자 2명: **A2**(WaveShaper/Convolver/DynamicsCompressor/Analyser/Panner·HRTF/MediaElementAudioSource, 커밋 번호 110~139, `tools/hrtfgen` 추가 예정)와 **B**(AudioBuffer 마무리·`decodeAudioData` 순수 Java 쪽·리샘플러·비동기 디코딩 서비스, 140~169). 이후 C(JavaScript 바인딩·실시간 출력·`ScriptProcessorNode`), D(AudioWorklet·검증) → P23.
  (2026-11-22) **P25 B 병합**(`20261122-140`~`147`, `media/webaudio/decode`·`AudioBuffer`): 조정자 전체 회귀 **4005 통과/0 실패/14 건너뜀**(JDK 6u45). 이 시점 main = aad54f2 + 문서 커밋. B 작업자는 이어서 **B2**(P18 디코더에 원래 채널 수·float 정밀도 선택 경로 추가, 잘린 파일은 디코딩한 만큼 돌려주는 관대 정책; 커밋 148~)를 진행 중이고 A2 는 `tools/hrtfgen` 까지 커밋했다. P25-05 는 JavaScript 노출(C 단계)이 남아 미완료.
  (2026-11-22) **P25 A2 병합**(`20261122-110`~`117`, 나머지 노드): 조정자 전체 회귀 **4097 통과/0 실패/14 건너뜀**(JDK 6u45). main = fc209dc + 문서 커밋. 남은 것: B2(진행 중), C1(JavaScript 바인딩), C2(실시간 출력·`ScriptProcessorNode`·`<audio>` 어댑터·수명), D(AudioWorklet·검증). P25-04 체크는 C2 의 `<audio>` 어댑터 뒤.
  (2026-11-22) **P25 B2 병합**(`20261122-148`~`155`, P18 디코더 아홉 개의 넓은 경로 + 관대 손상 정책): 아래 로그 참조. 진행 중: **C1**(JavaScript 바인딩·OfflineAudioContext·decodeAudioData, 커밋 번호 170~199).
  (2026-11-22) **P25 C1 병합**(`20261122-170`~`175`): 전체 회귀 **4186 통과/0 실패/14 건너뜀**(JDK 6u45, 시험 안정화 뒤). AGENTS 에서 **P25-05 체크**. 진행 중: C2a 완료·회귀 중(실시간 출력 구동기, `ScriptProcessorNode` 엔진 노드, 커밋 300~303), 다음은 **C2b**(JS 연결: `OutputRenderDriver` 로 교체, `createScriptProcessor`, `createMediaElementSource` `<audio>` 어댑터, 수명)와 D(AudioWorklet·검증).
  (2026-11-22) **P25 C2a 병합**(`20261122-300`~`303`, 전체 회귀 4231/0/14)과 **C2b 병합**(`20261122-330`~`335`): 전체 회귀 **4270 통과/0 실패/14 건너뜀**. AGENTS 에서 P25-02, **P25-04 체크**. 남은 P25: P25-06(AudioWorklet, D1 작업자 진행 중, 커밋 번호 360~389), P25-07(실제 소리 장치 청취 확인, 사람), P25-08(최종 검증과 문서, D2). 그 뒤 P23(Maven, `mvn` 설치 필요).
  (2026-11-22) **P25 D1 병합**(`20261122-360`~`369`, AudioWorklet)과 **P25-08 체크**: **P25 의 남은 `[ ]` 는 P25-07 하나**(실제 소리 장치 출력과 청취 확인, 사람이 소리 장치가 있는 PC 에서; 코드와 자동 검증은 끝남). 전체 회귀 4331/1/14(실패는 P25 와 무관한 `XhrTest.testTimeout`, 고쳐서 8회 연속 통과 뒤 병합). P25-08 정리 문서 `docs/progress/webaudio-verify.md`, 실제 Chromium 156 과의 비교(`ChromeReferenceTest`) 결과는 아래 작업 로그. 다음: P23(Maven; `mvn` 설치 필요), 그 뒤 P22-15(인수인계)/P22-16(최종 대조).
```

## 2026-11-20 주 패키지명 변경: `swingwebpanel` → `org.duckdns.hjow.swingwebpanel`

사용자 요청으로 주 패키지명을 바꿨다. 기능 변경은 없고 이동과 이름 치환만 했다. 작업 번호가 있는 계획 항목이 아니므로 체크 상태는 바꾸지 않았다.

### 1. 한 일
- **디렉터리 이동**(`git mv`, 1857 파일이 rename 으로 잡힘): `src/swingwebpanel`, `tests/src/swingwebpanel`, `examples/src/swingwebpanel`, `tests/resources/swingwebpanel`, `examples/resources/swingwebpanel` 를 각각 `.../org/duckdns/hjow/swingwebpanel` 로 옮겼다. 클래스패스 자료(`tests/resources`, `examples/resources` 의 옛 `swingwebpanel/` 아래)도 같은 이유로 새 경로로 옮겼다.
- **내용 치환**(텍스트 파일 1783 개를 검사해 1779 개 변경): `package`/`import`/완전한 이름/Javadoc `{@link}`/`Class.forName` 문자열/`--filter` 문자열의 `swingwebpanel.` → `org.duckdns.hjow.swingwebpanel.`, 경로 문자열(`src/swingwebpanel`, `classpath://swingwebpanel/…`, `/swingwebpanel/js/…`, JAR 항목 이름 검사 `swingwebpanel/`) → `org/duckdns/hjow/swingwebpanel/`. 대상은 제품/시험/예제 소스, build.sh·bat, test.sh·bat, examples/run.*, scripts/*, tools/*(`UiCheck` 의 import 와 코드 생성기 `BrotliDict`/`EntityGen`/`VoGen` 이 내보내는 `package` 줄 포함), 모든 문서다.
- **바꾸지 않은 것**(패키지명이 아님): User-Agent 기본값과 시험의 기대값 `swingwebpanel/1.0`(AGENTS.md 목표 절이 정한 값), `swingwebpanel.jar`(JAR 이름), 스레드 이름 `swingwebpanel-…`, 임시 디렉터리 `<tmp>/swingwebpanel/localstorage-…`, 시스템 속성 `swingwebpanel.opus.vectors*`, WOFF2 시험 메타데이터의 `swingwebpanel.woff2.test`, `tools/webfetch` 의 UA `swingwebpanel-webfetch/1.0`.
- **문서**: AGENTS.md 는 "개발 계획" 절의 P01-01, P02-01, P07-07-b-3, P09-08-b 네 줄만 바꿨고(P01-01 에 변경 사실과 유지한 이름 메모 추가) "프로젝트 목표/목표 - 지원 기능/주의사항" 단락은 그대로다. README.md 사용법에 `import org.duckdns.hjow.swingwebpanel.SwingWebPanel;` 와 이전 이름에서 바뀌었다는 안내를 넣었다. ARCHITECTURE.md 의 구성 그림/패키지 표를 새 이름으로 고쳤다.
- 이동 중 `git mv src/swingwebpanel …` 가 "Permission denied" 로 한 번에 옮겨지지 않아 하위 항목을 하나씩 옮겼다(원인 불명, 다른 JVM/편집기가 디렉터리 핸들을 쥔 것으로 추정; 최종 결과는 동일).

### 2. 검증(JDK 6u45, 실제 실행)
- `sh test.sh`: 제품 빌드, 시험 컴파일, 전체 실행 **3752 통과 / 0 실패 / 12 건너뜀**(651 초). 이름 변경 전 master 의 마지막 기록(3752/0/12)과 같다. 건너뜀은 선택적 Opus 원본 벡터 12건이다. 실패 사례에서 실행기가 종료 코드 1 을 내는 자체 점검도 통과.
- 제품 JAR(`build/swingwebpanel.jar`): 항목 3280 개가 모두 `org/duckdns/hjow/swingwebpanel/` 와 `META-INF` 아래에 있고 옛 `swingwebpanel/` 항목이 없다. 진입 클래스의 class 파일 major 버전 50.
- `tools/sourcecheck`(인코딩, 금지 API, 영/한 Javadoc, 반복문 주석): 2796 파일 **0 위반**(문서 수정 뒤 다시 실행해도 같음).
- 예제: `sh examples/run.sh BlankPageExample --smoke`, `StaticPageExample --smoke`(페이지 `classpath://org/duckdns/hjow/swingwebpanel/examples/pages/index.html` 표시, renderedHeight=942), `examples\run.bat BlankPageExample --smoke`(Windows cmd) 모두 smoke OK.
- 도구 컴파일: `tools/{uicheck,brotlidict,entitygen,vogen,webfetch,sourcecheck}` 모두 JDK 6 으로 빌드 성공.
- 확인하지 못한 것: `test.bat`/`build.bat` 전체 실행(스크립트는 클래스 이름만 바뀌었고 `examples/run.bat` 으로 같은 형태를 확인), 실제 Swing 창 조작(`tools/uicheck` 실행)은 이번에 다시 하지 않았다.

### 3. 미병합 브랜치 세 개도 새 패키지로 옮겼다(같은 날 이어서 처리, commit 20261120-002~004)
master 에 병합하지 않은 브랜치 세 개는 옛 경로(`src/swingwebpanel/...`)와 옛 `package` 줄을 쓰고 있어, 그대로 두면 브랜치가 새로 만든 파일이 옛 경로에 놓이고 옛 패키지를 선언해 컴파일되지 않는다. 브랜치마다 가장 알맞은 방식으로 옮겼다.

| 브랜치 | 방식 | 결과 |
| --- | --- | --- |
| `worktree-agent-a3f9075cc7e969e4f`(`.claude/worktrees/agent-a3f9075cc7e969e4f`, P18-08 MEDIA-E 3 커밋) | master 를 병합(`merge.directoryRenames=true`). 이 브랜치가 새로 만든 파일 7개(제품 6, 시험 1)는 새 경로로 옮기고 `package`/`import` 를 같은 규칙으로 치환 | 병합 commit 20261120-004, 충돌 0. master 가 이 브랜치의 조상이라 `git merge --ff-only` 로 반영할 수 있다. master 대비 변경은 MEDIA-E 의 13개 파일뿐. 전체 회귀(JDK 6u45) **3757 통과 / 0 실패 / 12 건너뜀**(master 3752 + 이 브랜치의 새 시험 5) |
| `wip/cssi-page-unverified` | 브랜치 위에 이동·치환 commit 하나(20261120-002). master 내용은 섞지 않음 | JDK 6u45 로 제품과 시험이 컴파일된다(시험은 실행하지 않음) |
| `wip/frames-forms-unverified` | 같음(20261120-003) | 같음 |

왜 WIP 두 개에는 master 를 병합하지 않았나: 이름과 무관하게 master 와 실제로 갈라져 있다. 옛 tip 과 master 의 병합 충돌이 cssi 17건, frames 45건이다(master 가 이후에 같은 파일을 계속 고쳤고, frames 의 `FrameContext` 등은 master 에도 이미 있어 add/add 이다). 어느 쪽 구현을 택할지 정하는 일이라 임의로 해소하지 않았다. 이름 변경 commit 을 얹으면 cssi 의 충돌 수가 17건에서 74건으로 늘어난다(master 가 먼저 같은 `import` 줄을 바꿔 생기는 잡음). 그래서 이 두 브랜치는 `git merge master` 보다 필요한 변경만 골라 master 위에서 다시 적용하는 편이 낫다.

주의: 내용 치환 규칙(Perl)은 한 번만 적용한다. 옛 경로를 설명하는 문장도 새 경로로 바꾸기 때문에, 이 브랜치에서 두 번째로 돌렸을 때 이 절이 망가져 병합 commit 에서 master 의 원본으로 되돌렸다. 작업을 위해 만든 임시 worktree(`.claude/worktrees/port-cssi`, `port-frames`)는 끝난 뒤 지웠다.

### 4. 한계
- 호환용 옛 이름 클래스(`swingwebpanel.SwingWebPanel` 위임 클래스)는 만들지 않았다. 이 컴포넌트를 쓰던 코드는 `import` 를 바꿔야 한다.
- `docs/progress/*.md` 와 이 문서의 과거 기록 안의 경로와 클래스 이름도 기계적으로 새 이름으로 바꿨다. 그 기록이 적힌 당시에는 옛 경로였다. `--filter swingwebpanel` 처럼 부분 문자열인 시험 필터는 새 이름에도 들어 있어 그대로 유효하다.

## 2026-11-19 재개 세션 (조정자 혼자, 작업자 없음)

### 1. 한 일
- **CSS-L(P10-08-d)**: WIP 954 를 마무리했다. `Woff2Test` 의 실제 글꼴 시험을 둘로 나눴다(저장소의 합성 글꼴 기준 자료 `testReferenceFilesDecodeToTheSyntheticFont`, 시험 때 실행 중인 JRE 의 Lucida 세 글꼴을 `Woff2Builder` 로 바꿔 비교하는 `testJreFontsDecodeToTheOriginal`), `WebFontTest`/`WebFontIntegrationTest` 는 JRE 글꼴을 시험 때 WOFF2 로 바꿔 쓴다(없으면 건너뜀). 생성 스크립트 둘을 다시 실행하면 저장소의 자료와 바이트 단위로 같음을 확인했다. **Lucida 파생 파일 6개를 브랜치 이력에서 지웠다**(인수인계에 적힌 명령은 `woff2/` 디렉터리 전체를 지워 새 합성 자료까지 없앴을 것이므로 파일 6개만 지정했다). master 를 병합(속성 id 312~315 충돌 없음)한 뒤 전체 회귀 3644 통과/0 실패/14 건너뜀, master 빨리 감기(`.git` 소유자 문제로 `git fetch .` 대신 `git update-ref` 로 ff 만 확인하고 옮김). 기록: `docs/progress/cssg.md` 32.10~32.12.
- **WOFF2 독립 인코더(P10-08-e 보강)**: 임시 가상 환경에서 fontTools 4.68.0 + brotli 1.2.1 로 합성 글꼴을 세 가지로 인코딩해 시험 자료로 넣었다(`synthetic-fonttools-{xf,glyf,null}.woff2`, `generation/make-woff2-fonttools.py`). 우리 디코더가 그 파일들을 원본 합성 글꼴로 풀어 낸다(시각 필드 둘만 fontTools 가 바꿈). cssg 32.12.
- **MEDIA-D(P18-11)**: WIP 568 의 `Mp3Source.prerollStart`(작은 VBR 프레임 뒤에서 비트 저장소를 채울 만큼 앞에서부터 해독)가 맞음을 확인했다(옛 코드로는 `mp3_v2_44100_st_gap` 의 23152 프레임 탐색이 순차와 다름, SNR 12.8 dB). 시험의 새 탐색 위치 `frames*3/4+1100` 이 짧은 자료에서 끝을 넘던 결함(`NegativeArraySizeException`)을 고쳤다. `docs/progress/media-matrix.md` 11절(자료 117개+거부 4개, 허용 오차와 근거, 고친 결함 12가지, 미확인, P18-11 판단)을 썼다. 병합 회귀 3658 íµê³¼/1 ì¤í¨(FontDisplayIntegrationTest.testOptionalFontThatArrivesAtOnceIsUsed ìê° ì´ê³¼, ë¨ë ì¬ì¤í 4/4 íµê³¼)/14 ê±´ëë.
- **P10-08-g**: `font-display` 의 block 기간(`auto`/`block` 3초, `fallback`/`optional` 100ms, `swap` 없음)에 웹 글꼴을 기다리는 텍스트를 대체 글꼴 자리만 차지하고 그리지 않으며, 기간이 끝나면 페이지가 스스로 다시 렌더링한다. cssg 33절. 시험 `FontBlockPaintTest` 5, `FontBlockIntegrationTest` 3.
- **문서**: AGENTS P10-08-d/e/f/g 체크와 P10-08-h 신설, FEATURE_MATRIX 13절 재집계(집계 규칙은 13절 첫 문장), ARCHITECTURE(CSS-L 구성, `tools/brotlidict`).

### 2. P18-11 판단
기록 작업(형식별 입력, 기대 PCM/화소, 실패 사례)은 이미지·오디오 모두 끝났고 비디오 예외를 뺀 대상 표에 미구현 행이 없다. 그러나 P18-11 은 P18 의 마지막 항목이라 "단계의 마지막 완료 항목은 그 단계의 모든 작업과 검증이 끝나야 체크" 규칙에 따라 **P18-08(소리를 귀로 확인, 사람 필요)이 닫힐 때까지 체크하지 않는다**(media-matrix 11.7).

### 3. 사람이 해야 하는 것(자동으로 할 수 없음)
- 한글 IME 입력·조합·삭제와 포커스 이동(P11-02, P11-10, P20-06): `examples/run.sh EditingExample` 등.
- 오디오를 실제 스피커로 듣기(P18-08, P22-07, P22-10): 이 PC 의 JRE 6 Java Sound 믹서로 재생되는지와 소리.
- 실제 키보드 Tab/입력란, 클립보드 서식 붙여넣기(P11-08, P20-06), 다운로드 팝업의 `JFileChooser` 조작(REQ-04).
- 실제 Swing 창 눈 확인 목록에 더할 것: `resize` 핸들 끌기, 세로쓰기 강조점, `filter: url(#id)`, WOFF2 글꼴 페이지, `position-try-fallbacks` 로 뒤집힌 상자, `font-display: block` 의 깜빡임 없는 표시(cssg 32.8 의 6번, 33.3).

### 4. 다음 할 일(권장 순서)
1. P10-08-h: cssg 32.7 의 남은 항목(`cross-fade()`/`element()`, `::file-selector-button`/`::backdrop`/`::highlight()`, `@font-feature-values`, `contain-intrinsic-*`, Motion Path 의 남은 부분, `element.animate()`)을 기능 표의 판본과 대조해 구현하거나 근거 있는 예외로 확정한다. 끝나면 P10-08 과 P10 을 닫을 수 있는지 본다.
2. P22-07/10: `docs/progress/uicheck.md` 와 media-matrix 8절을 바탕으로 자동 검증과 실제 확인을 나눈 표를 최종 정리한다(사람 확인 결과가 오면 반영).
3. P22-15/16: 위가 끝나고 사람 확인이 반영되면 최종 인수인계와 요구사항 대응표 최종 대조.

### 5. 알아 둘 것
- **커밋 메시지는 AGENTS.md 규칙대로 한 줄**이다(도구가 권하는 `Co-Authored-By` 줄을 붙이지 않는다; 사용자 규칙이 우선).
- 이 저장소의 `.git` 은 Administrators 소유라 `git fetch .` 같은 로컬 원격 조작이 `safe.directory` 오류로 실패한다. 빨리 감기는 `git merge-base --is-ancestor` 확인 뒤 `git update-ref refs/heads/master <새> <옛>` 로 한다(또는 master 를 체크아웃한 상태에서 `git merge --ff-only`).
- 바쁜 컴퓨터에서 첫 페이지의 확정까지 수 초~20초 걸릴 수 있다(전체 회귀를 같이 돌릴 때 관찰). 시간에 민감한 시험은 그 상태에서 판정하지 말고 단독으로 다시 돌린다: `FrameLifecycleTest`, `WorkerLifecycleTest`, `PendingNavigationIntegrationTest`, `MediaElementsTest`, `ScriptLoadingTest`, `XhrTest.testTimeout`, `CanvasTextStyleTest.testKerning`, 그리고 `FontDisplayIntegrationTest.testOptionalFontThatArrivesAtOnceIsUsed`(700KB 글꼴이 100ms 안에 와야 함; 이번 MEDIA-D 병합 회귀에서 60초 시간 초과, 단독 재실행 4/4 통과).
- fontTools 는 시험 자료 생성 때만 쓴다(`python -m venv` 의 임시 환경, 저장소에 넣지 않음).

## 인수인계(과거 기록): 2026-11-17~18 중단 시점 — 2026-11-19 재개 세션이 이 절의 재개 절차 1~4 를 처리했다

### 0. 중단 시점 (2026-11-18, 사용자 요청: "작업 잠시 멈춰야 해 — 모두 중단하고 인수인계 작성, git 커밋 후 중단")

**멈춘 것**: 백그라운드 작업자 둘(CSS-L, MEDIA-D)을 `TaskStop` 으로 멈췄고, 그들이 돌리던 JDK 6 전체 회귀 java 프로세스도 종료했다(이 PC 의 `java.exe` 가 0 개인 것을 확인). 두 worktree 의 미커밋 작업은 **WIP 커밋으로 보존**했다(아래 표). master 는 깨끗하다(미커밋 없음), push 하지 않았다(사용자 몫).

**master 상태**: 마지막 코드 병합은 MEDIA-C(`5e9c5dd`, P18-03 체크, 통합 전체 3542 통과/0 실패/12 건너뜀). 이 세션(2026-11-17 오후~2026-11-18)에 master 로 들어간 것: CANVAS(P20-05/-b 체크), WEBAPI-B/C(P11-01, P20-13 체크), CSS-I/J/K(P10-08-a~c 체크), MEDIA-B/C(P18-03 체크), MEDIA-A 문서 반영(P18-01/02/10 체크), `tools/uicheck`(P09-08-b-2/P09-08-b/P09-08 체크, REQ-01 검증 완료, REQ-04 근거 보강), 결함 수정 둘(깨진 이미지 대체 상자의 긴 주소 줄바꿈 `DisplayCommands.Placeholder.wrapLine`, 다운로드 팝업 "Size:" 줄 갱신 `SwingDownloadDialog`). 마지막으로 master 전체를 돌린 회귀는 MEDIA-C 병합 상태이며 그 뒤 master 에는 문서 커밋뿐이다.

**미병합 브랜치 둘(둘 다 worktree 에 있고 커밋되어 있다)**

| 작업자 | 브랜치와 위치 | 상태 |
| --- | --- | --- |
| CSS-L (P10-08-d) | `worktree-agent-a6f38b8e380c3ad12`, `.claude/worktrees/agent-a6f38b8e380c3ad12`, 마지막 커밋 `75cde31`(954 WIP) | 커밋 940~953 은 완료·시험됨: `resize` 핸들, `shape-outside` 이미지 모양, SVG 요소 참조(`filter`/`clip-path`/`mask` 의 `url(#id)`), 세로쓰기 `text-emphasis`, 다단 `box-decoration-break: clone`, `calc()` 안 `anchor()`·암묵 앵커, `font-display` 기간 모델(block 기간의 보이지 않는 텍스트 제외), **Brotli 디코더(RFC 7932)와 WOFF2 디코더**(`media.font.{Brotli,BrotliData,Woff2}`, 도구 `tools/brotlidict`), `@position-try` 규칙·`position-try-fallbacks/order`·`position-try`·`position-visibility`·`anchor-scope`·`anchor-center`. 그 브랜치의 마지막 전체 회귀(952 기준): 3626 통과/1 실패/14 건너뜀(실패 `CanvasTextStyleTest.testKerning` 은 단독 재실행에서 통과하는 간헐 실패). **954 는 미완성 WIP 이며 이 브랜치의 일부 시험은 지금 깨져 있다**(아래 라이선스 정리 도중에 멈춤). 새 CSS 속성 id: `position-try-fallbacks` 312, `position-try-order` 313, `position-visibility` 314, `anchor-scope` 315(`Prop.COUNT` 315; master 의 마지막 id 는 `image-orientation` 311 이라 충돌하지 않을 것이다). 규칙 종류 `CssRule.POSITION_TRY_RULE` 106. 자세한 것은 `docs/progress/cssg.md` 32절. |
| MEDIA-D (P18-11) | `worktree-agent-a3245f3d8aed5ed68`, `.claude/worktrees/agent-a3245f3d8aed5ed68`, 마지막 커밋 `b1264bf`(568 WIP) | 오디오 디코더를 이 PC 의 ffmpeg 가 만든 참조 파일과 참조 PCM 으로 비교(`tests/resources/media/audio/**` + `generation/`, 시험 `media.audio.reference.ReferenceDecodeTest`). 이 과정에서 실제 인코더 파일에서만 드러나는 결함을 고쳤다(커밋 561~567): MP3 MPEG-2.5 8 kHz 짧은 블록 경계, AAC 샘플링 주파수 인덱스 12·채널 구성 11/12/14, Vorbis/Opus 다채널 섞기 순서, WAV RF64/BW64, AIFF-C `ima4` 추가, AAC 손상 블록의 예외 대신 무음 처리, 손상 사본 시험, 거부해야 하는 형식 시험 자료 4개. **WIP 568 에는 검증하지 않은 수정이 들어 있다**: `Mp3Source`(갭리스), `AiffDecoder`, `WavDecoder`, `make_audio_fixtures.py`, 참조 자료 재생성(`mp3_v2_44100_st_gap.mp3` 등), 문서 일부. **`docs/progress/media-matrix.md` 의 MEDIA-D 절(11절)과 "조정자 요청"은 아직 쓰지 않았다.** 마지막 전체 회귀 결과는 없다(작업자가 돌리던 것을 중단시켰다). |

**재개 절차(권장 순서)**
1. `git status`, `git log --oneline -5`, `git worktree list` 로 위 상태를 확인한다. 전체 회귀는 병합할 때마다 그 worktree 에서 `BUILD_DIR=build/coord sh test.sh > build/coord-full.log`(약 9~10 분, 백그라운드)로 돌린다. 충돌 표시 확인: `grep -rln '^<<<<<<<' src tests docs AGENTS.md tools`.
2. **CSS-L 마무리(먼저, 라이선스 문제)**: CSS-L 은 JRE 에 든 **Lucida 글꼴(Bigelow & Holmes, Java 용으로만 허락된 글꼴)을 변환한 파일을 저장소에 넣었다**(`tests/resources/org/duckdns/hjow/swingwebpanel/media/font/woff2/Lucida*.woff2` 4개와 `brotli/font-LucidaBrightDemiBold.br`·`.txt`). 954 WIP 는 그것들을 지우고 시험을 "시험 시간에 JRE 의 `java.home/lib/fonts/*.ttf` 를 읽어 `Woff2Builder`(시험용 Java 인코더)로 WOFF2 를 만들어 쓰기"로 바꾸다 중단되었다. 해야 할 일: `BrotliTest`(큰 입력은 글꼴이 아닌 합성/클래스 바이트 자료로), `Woff2Test`, `WebFontTest`, `WebFontIntegrationTest` 가 삭제된 파일을 더는 참조하지 않게 바꾸고 JDK 6u45 로 다시 돌리며, `cssg.md` 32.5 와 기능 표의 "저장소에 둔 Lucida WOFF2" 문구를 바로잡는다. **병합 전에 브랜치 이력에서 그 파일들을 지워야 한다**(미푸시이므로 가능): 그 worktree 의 작업을 모두 커밋한 상태에서 `git filter-branch -f --index-filter 'git rm -r --cached --ignore-unmatch -q tests/resources/org/duckdns/hjow/swingwebpanel/media/font/woff2 tests/resources/org/duckdns/hjow/swingwebpanel/media/font/brotli/font-LucidaBrightDemiBold.br tests/resources/org/duckdns/hjow/swingwebpanel/media/font/brotli/font-LucidaBrightDemiBold.txt' f37c11a..HEAD` 후 `git update-ref -d refs/original/refs/heads/worktree-agent-a6f38b8e380c3ad12`. (이 방식은 uicheck 스크린샷 정리 때 이미 한 번 썼다.) 그 다음 master 를 병합하고(`image-orientation` 311 이후에 312~315 가 붙었으므로 속성 id 충돌은 없을 것이다) 전체 회귀 → 기능 표/ARCHITECTURE/AGENTS 반영: ARCHITECTURE 에 `media.font.{Brotli,BrotliData,Woff2}`, `tools/brotlidict`, `layout.{Resizing,FloatShapes}`, `media.image.svg.SvgReference`, `layout.TextEmphasis`(세로쓰기), PositionTry 계열, `css.cssom.CSSPositionTryRule` 를 적고, AGENTS 의 P10-08-d 를 하위로 나눠(권장: `P10-08-d` 로 resize/shape-outside/SVG 참조/세로쓰기 text-emphasis/multicol clone/anchor 확장 완료 체크, `P10-08-e` WOFF2 체크, `P10-08-f` `position-try-*` 체크, `P10-08-g` `font-display` block 기간의 보이지 않는 텍스트 미구현으로 남김). `cssg.md` 32.7 의 체크 후보 판단을 읽고 P10-08 상위 항목은 **g 가 끝나거나 근거 있는 예외로 확정된 뒤** 체크한다.
3. **MEDIA-D 마무리**: 그 worktree 에서 WIP 568 의 변경(MP3 갭리스 `Mp3Source`, `AiffDecoder`, `WavDecoder`, 생성 스크립트와 자료 재생성)이 맞는지 `scripts/pkgtest.sh md "js dom html css net storage core content layout render download media @root" "js core render @root layout content download dom html css net storage media" --filter org.duckdns.hjow.swingwebpanel.media.audio` 로 확인한다(`ReferenceDecodeTest` 포함). 틀리면 WIP 변경을 줄이고 위 561~567 까지만 병합해도 된다. 그 뒤 `docs/progress/media-matrix.md` 에 MEDIA-D 절(11절)과 3절 표의 "시험용 인코더 기준" 표현 정정을 쓴다(형식별로 어떤 인코더/파일/참조로 어떤 오차까지 확인했는지, 확인하지 못한 것, **허용 오차와 근거**). 전체 회귀 후 병합하고, 새 정적 상태가 있으면 `core.StaticStateAuditTest` 의 REVIEWED 에 이유와 함께 추가한다. P18-11 은 보고를 읽고 판단한다(이미지 쪽은 대상 표에 미구현이 없다; P18-08 은 소리 청취가 사람 몫이라 체크 불가; 비디오 예외를 뺀 대상 표 전체 판정이 남는다).
4. **기능 표 13절 개수표 재집계**: 13절의 "축별 대조" 표와 "축별로 남은 행"을 현재 값으로 다시 센다. 행을 `|` 로 나눌 때 백틱 안의 `|` 를 건너뛰고, 상태 칸은 열 수가 표마다 달라 뒤에서부터 "검증 완료/부분 구현/미구현/예외" 로 시작하는 칸을 찾으며, 축은 ID 접두사로 정한다(ES→JavaScript 언어, DOM·HEI·WEB→DOM, UI·EVT→DOM 이벤트, HTML·LEG→HTML 표시, CSS→CSS 표시, IMG·AUD·VID→미디어, API·REQ·NET·STO·CNT→공개 API와 프로토콜). 2026-11-18 오후(CSS-K·MEDIA-B 병합 뒤 CSS-L/MEDIA-D 전)의 집계: ES 33/3/0/0, DOM 28/18/0/0, DOM 이벤트 5/8/0/0, HTML 표시 18/17/0/1, CSS 표시 14/32/0/0, 미디어 18/3/0/1, 공개 API 54/1/0/1(부분 구현 REQ-04 하나). 그 뒤 REQ-01 이 검증 완료가 되었고 MEDIA-C/CSS-L/MEDIA-D 가 행을 고쳤으므로 다시 세어야 한다(집계 스크립트는 `build/` 아래 임시 파일이라 저장소에 없다 — 위 규칙으로 다시 쓴다; 한글을 명령행 인수로 넘기지 말 것).
5. **P22 마무리**: P22-07(JRE 6 실제 실행: 이 PC 의 JRE 6 Java Sound 믹서는 3 개로 확인했으나 소리 청취는 사람 몫이라 미체크), P22-10(자동 검증/실제 확인의 구분 기록: `docs/progress/uicheck.md` 에 표가 있다. 한글 IME·소리·`JFileChooser` 가 사람 몫이라 미체크), P22-15(최종 인수인계), P22-16(요구사항 대응표 최종 대조: 기능 표 10절의 각 행이 구현·검증 사례에 연결되어 있고 `REQ-04` 같은 부분 구현 행이 정직한지 확인). 모든 선행 단계가 닫히기 전에는 체크하지 않는다.
6. **작업자는 한 번에 2명 이하**(사용자 지시, 메모리 규칙 `background-agent-limit`)이고 병합은 보고가 오는 대로 한다(`merge-workers-promptly`). 작업자 브리프의 공통 규칙은 `docs/progress/briefs/common.md`(거기 적힌 "최대 3명"은 2명으로 읽는다). 작업자 시작 명령/브리프 예는 이 세션의 `Agent` 호출(`isolation: worktree`, 커밋 번호 대역 지정, `AGENTS.md`/`PROGRESS.md`/`ARCHITECTURE.md`/`API_CONTRACT.md`/`scripts` 수정 금지)과 같다.

**알아 둘 것(이 세션에서 새로 알게 된 것)**
- 이 PC 에는 ffmpeg(libaom/libdav1d/librav1e/libsvtav1/libmp3lame/libvorbis/libopus 등)와 Node.js v26(`zlib.brotliCompressSync`)가 이미 있어 시험 자료(기준 스트림·참조 PCM·Brotli 스트림) 생성에 쓸 수 있다(내려받기 불필요; 제품·시험 실행에는 필요 없고 생성한 자료와 기대 값을 커밋한다). Python 에는 fontTools/brotli 가 없다.
- JRE 6u45 가 일부 문자(히브리어 등)를 글자 배치 API(`Font.layoutGlyphVector`/`TextLayout`)에 주면 `fontmanager.dll` 접근 위반으로 JVM 이 죽는 일이 있다. 전체 회귀에서 한두 번 간헐적으로 났고(`SvgDecoderTest.testTextInk` 에서 한 번) 같은 시험은 단독으로 통과했다. `hs_err_pid*.log` 는 `.gitignore` 대상이다.
- worktree 를 지울 때: 현재 디렉터리가 그 안이면 Windows 가 지우지 못한다(`Permission denied`). 다른 곳으로 옮긴 뒤 `git worktree remove -f -f <경로>` 와 `rm -rf <경로>`, `git worktree prune`.
- 실제 Swing 창 확인은 `tools/uicheck`(`tools/uicheck/AGENTS.md`): `--example`/`--window`/`--press`/`--click`/`--wheel`/`--dialogshot`/`--shot` 등. 스크린샷은 창 내용 영역만 찍는다(다른 창의 내용이 섞여 개인 정보가 저장소에 들어가는 것을 막기 위해). 다운로드 팝업 스크린샷은 사용자 계정 경로가 보여 저장소에 넣지 않았다.
- 한글을 perl 스크립트에 명령행 인수나 환경 변수로 넘기지 말 것(인코딩이 깨진다; 이번에 AGENTS.md 에서 한 번 발생해 고쳤다).

### 1. 저장소 상태
- 브랜치 `master`, 마지막 커밋 `20261117-060`(이 인수인계 커밋은 061). push 하지 않았다(사용자 몫).
- JDK 6: `C:\Program Files\Java\jdk1.6.0_45`(`JAVA6_HOME`). 제품 검증은 JDK 6 으로만 한다.
- 마지막 **전체** 회귀는 LAYOUT-F 병합 직후(스냅샷 `lfmerge`) 2405 통과/0 실패였다. 그 뒤 커밋 057(PageResources 종료 경쟁 수정), 058(전환/애니메이션 이벤트 스크립트 노출), 059(FEATURE_MATRIX), 060(a/area URL 유틸리티)은 `PageResourcesTest`, `AnimationIntegrationTest`, `css` 148건, `js.dom` 35건만 돌렸다. **새 세션의 첫 일은 전체 회귀이다**: `sh scripts/snaptest.sh full "src tests" "js dom html css net storage core content layout render download media @root" "js dom html css net storage core content layout render download media testing @root"` 를 백그라운드로 돌리고(10분 안팎) 결과 파일의 마지막 `===` 줄을 본다. 소스 규칙 점검은 `sh tools/sourcecheck/run.sh`(기본 경로 전체, 0 위반이어야 함).
- 제품 JAR: `./build.sh` → `build/swingwebpanel.jar`. 예제는 `examples/run.sh <클래스> --smoke`.

### 2. 작업자 (2026-11-18: CSS-L, MEDIA-D 실행 중; CSS-I, WEBAPI, CANVAS, MEDIA, WEBAPI-B, CANVAS-B, CSS-J, WEBAPI-C, MEDIA-B, CSS-K, MEDIA-C 는 병합 완료)

병합이 끝난 작업자(OPUS, FRAMES, WORKER, CSS-G, TEXTSEL, LEGACY, CSS-H, CSSOM, EDIT, CSS-I)의 worktree 는 지웠다. 현재 실행 중인 작업자(브리프와 규칙은 시작 때의 프롬프트에 있고 `docs/progress/briefs/` 가 보조 자료):

| 작업자 | 일 | 시작점 | 커밋 번호 대역 | 기록 |
| --- | --- | --- | --- | --- |
| MEDIA-D | P18-11: 오디오 디코더(WAV/AIFF/AU/MP3/AAC/Vorbis/Opus/FLAC)를 이 PC 의 ffmpeg 로 만든 참조 파일·참조 PCM 과 비교(시험용 인코더 기준만으로는 약함), 대상 표 최종 대조 | master(MEDIA-C 병합 뒤) | 560~ | `docs/progress/media-matrix.md` |
| CSS-L | P10-08-d: 세로쓰기 `text-emphasis`, `resize` 핸들, `shape-outside` 이미지 모양, SVG 요소 참조 `filter`/`clip-path`/`mask`, `anchor()` 확장, WOFF2(Brotli)·`font-display`, 다단 `box-decoration-break: clone` | master(CSS-K 병합 뒤) | 940~ | `docs/progress/cssg.md` |

작업자는 `git worktree list` 에서 `.claude/worktrees/agent-*` 로 찾는다(브랜치 `worktree-agent-<id>`). 작업자 보고는 모델 출력이다. **병합은 보고가 오는 대로 바로 한다**(사용자 요청): 해당 worktree 에서 `git rebase master`(충돌은 의미를 보고 양쪽을 합친다 — 같은 파일을 여러 작업자가 고쳤으면 병합 커밋 한 번이 낫다; 충돌 표시가 남은 채 `rebase --continue` 하지 않도록 `grep -rn '^<<<<<<<'` 로 확인; 마지막 커밋이 "WIP 미검증"이면 `wip/*` 브랜치로 빼고 `git reset --hard HEAD~1`) → 그 worktree 에서 `sh test.sh`(전체 회귀, 10분 이상; 컴파일 오류가 나면 병합 때문에 생긴 의미 충돌) → 실패는 단독 재실행으로 간헐 여부 판단 → master 에서 `git merge --ff-only <브랜치>` → 문서 반영(AGENTS 체크) → worktree 제거(`git worktree remove -f -f`). 여러 브랜치를 한 줄로 쌓아 회귀를 한 번만 돌릴 수도 있다. 동시 작업자는 **2명 이하**(사용자 지시 2026-11-17: 실행 중이던 CSS-I·WEBAPI·CANVAS 가 끝난 뒤부터 적용; 메모리 규칙 `background-agent-limit`), 병합은 즉시(`merge-workers-promptly`). 앞으로 시작할 작업자 후보: CSS-I(P10-08 남은 것: `@page`(`wip/cssi-page-unverified`), `backdrop-filter`, `text-emphasis` 등), MEDIA(P18-08/10 오디오 재생 나머지, P18-03 AVIF 인터 프레임), UIEVENTS(PointerEvent, 페이지 수명 이벤트 pageshow/pagehide/beforeunload/visibilitychange, `isComposing`), CANVAS(P20-05-b).

### 3. 열린 계획 항목(AGENTS.md 에서 `[ ]`)과 막힌 이유
- **작업자 진행 중**: LEGACY, TEXTSEL, CSS-H(위 2절). 병합 완료: P18-07(Opus), P20-09-b(WORKER), P20-02 의 구현(FRAMES; 체크는 시간 민감 시험 재확인 뒤), P10-08 의 일부(CSS-G). P22-09 의 프레임 부분은 아직.
- **P20-01**(HTML 요소 인터페이스): 이 세션에서 28개 인터페이스, 표 객체 모델, document 멤버, a/area URL 유틸리티를 했다. 남은 것: `HTMLMapElement.areas`, `HTMLDialogElement.show/showModal/close/returnValue`, `HTMLTrackElement.readyState/track`, `HTMLObjectElement.form/contentDocument`(설계 초안은 이 세션에서 작성했으나 시험 전이라 저장소에 넣지 않았다: 요소 상태 멤버를 `js/dom` 의 새 클래스에서 `defineAccessor`/`defineBuiltin` 으로 연결하면 된다), `document.elementFromPoint`, `rowSpan` 기본값 1(`uint1` 비슷한 새 반영 형식), 항목 단위 점검표(`docs/progress/htmlifaces-audit.md` 갱신).
- **P10-08**: 미구현 CSS 모듈 목록은 `docs/progress/layout-f.md` 의 표(`filter`, `mix-blend-mode`, `mask`, `shape-outside`, 논리 속성, `break-*`, `@layer`/`@container`, `::selection`/`::placeholder`, `@counter-style` 등). 새 작업자("CSS-G") 후보.
- **P11-01/02/08/10**: 텍스트 선택, 캐럿, 복사/붙여넣기, 페이지 확대(P11-08)와 실제 AWT 입력·한글 IME 확인(사람 필요). 선택/확대는 새 작업자("TEXTSEL") 후보.
- **P20-03**(Range 화면 좌표는 요소 단위 근사), **P20-05-b**(OffscreenCanvas, ImageBitmap, filter 등과 `crossorigin` 이미지의 `Origin`), **P20-06**(드래그/드롭, contenteditable, 데이터 전달), **P20-10**(WebSocket 등 스킴 정책 기록), **P20-11/12**(문서 모드·프레임·SVG/MathML 혼합 검증과 축별 대조).
- **P18-01/02/03/08/10/11**: 미디어 점검(AVIF 는 인트라 프레임만이라 P18-03 은 열림), 오디오 컨트롤 막대 조작과 상태(P18-08), 실제 장치 청취(사람 필요).
- **P15-08-c-2**(`<a download>`와 폼 제출): 구현과 시험(`LinkDownloadIntegrationTest`, `FormNavigationIntegrationTest`)은 있으나 AGENTS.md 체크가 빠져 있다 — 근거를 확인한 뒤 체크 가능성이 높다. P15-08/P15-08-c 상위도 확인한다.
- **사람이 필요한 것**: P09-08-b-2(실제 Swing 창), P11-02/10(IME), P22-07(오디오 장치 청취), P22-10(자동 검증과 실제 확인 구분 기록). 이 PC 의 JRE 6 Java Sound 는 믹서 0개라 소리를 낼 수 없다.
- **마무리**: P22-14(FEATURE_MATRIX 갱신), P22-15(PROGRESS 인수인계), P22-16(요구사항 대응표 최종 대조)은 모든 단계가 끝난 뒤.

### 4. 알려진 한계와 관찰(새로 알게 된 것)
- JRE 6u45 는 TLS 1.0 만 지원해 요즘 HTTPS 전용 사이트는 열 수 없다(https 실패 → http 재시도 → 사이트의 https 강제 리디렉션 → "Redirect loop" 오류 화면). JRE 8 에서는 같은 JAR 로 `https://www.wikipedia.org/` 가 열렸다(P22-07 기록).
- `Origin` 을 싣는 요청(CORS, 사전 요청, 오리진이 있는 POST/PUT/PATCH/DELETE)은 JVM 프록시가 없을 때 소켓 연결(`net.SocketHttp*`)로 보낸다. 이미지/스타일시트/글꼴 요청은 `Origin` 을 싣지 않고 교차 오리진 글꼴은 요청하지 않고 차단한다.
- HTTP 캐시는 요청 URL 키로 저장해서, 리디렉션을 거친 페이지의 최종 URL `refresh()` 는 서버에 다시 묻는다(계약 위반은 아님).
- 이미지 로딩이 아주 빨리 실패하면 페이지 뒤쪽 스크립트가 붙인 `error` 핸들러보다 이벤트가 먼저 보고될 수 있다(요소의 `onerror` 속성은 안전).
- `PageResourcesTest.testEntryLimitAndClose` 의 경쟁은 제품 쪽(close 뒤 finished 이중 계산)이었고 057 에서 고쳤다.
- 알려진 구현 한계는 FEATURE_MATRIX 각 행의 "제약" 열과 `docs/progress/*.md` 에 있다.

### 5. 다음 세션 시작 절차(권장 순서)
1. `git status`, `git log --oneline -5`, `git worktree list` 로 위 상태를 확인한다. 전체 회귀와 `tools/sourcecheck` 를 돌린다.
2. OPUS: rebase → 시험 → ff 병합 → 문서(AUD-08/09, README, P18-07 판단).
3. FRAMES, WORKER: 위 표대로 이어서 하거나 새 작업자로 시작(커밋 번호 대역: LAYOUT-F 4xx 사용 끝, OPUS 5xx, FRAMES 6xx, WORKER 7xx).
4. 슬롯이 남으면 새 작업자: CSS-G(P10-08), TEXTSEL(P11-08), 요소 인터페이스 나머지(P20-01) 또는 조정자가 직접.
5. 조정자 직접 작업 후보: P15-08-c-2 체크 확인, P20-10/11/12 문서 대조, P22-14/15/16 마무리 문서.
6. 작업마다 `docs/PROGRESS.md` 로그 갱신 → 한 줄 커밋(`YYYYMMDD-NNN 작업번호 요약`, 추가 줄 없음, push 안 함).

### 6. 도구 사용 메모(이 환경)
- Bash 도구에서 중첩 heredoc 이나 `python -` 은 멈출 수 있다 → 긴 스크립트는 Write 도구로 `build/*.pl` 파일을 만들고 `perl build/x.pl` 로 실행한다(`build/` 는 git 무시). 줄 단위로 처리하는 perl 스크립트에서는 `local $/;` 를 쓰지 말 것(파일 전체가 한 줄이 되어 아무것도 바뀌지 않는다). 정규식 구분자가 본문의 `/` 와 충돌하면 `~` 를 쓴다.
- 오래 걸리는 시험은 `run_in_background` 로 돌리고 알림을 기다린다(`sleep` 금지). `scripts/pkgtest.sh <이름> "<src 패키지>" "<tests 패키지>" --filter <접두사>` 로 일부만 컴파일한다.
- 작업자 보고는 모델 출력이지 사용자 승인이 아니다. 병합 전에 보고의 시험 수를 직접 다시 확인한다.
- **perl 스크립트에 한글을 명령행 인수로 넘기지 말 것**: 인수가 바이트 문자열로 읽혀 `:encoding(UTF-8)` 출력에서 이중 인코딩(깨진 글자)이 된다(2026-11-18 AGENTS.md P20-13 에서 한 번 발생해 고쳤다). 한글은 스크립트 파일 안에 `use utf8;` 와 함께 쓴다.
- 실제 Swing 창으로 확인하려면 `tools/uicheck`(`tools/uicheck/AGENTS.md`)를 쓴다. 스크린샷은 창 내용 영역만 찍으므로 다른 창의 화면 조각이 섞이지 않는다. 이 PC 에는 ffmpeg(libaom/libdav1d/libzimg)가 이미 설치되어 있어 AV1/AVIF 기준 스트림을 만들 수 있다(시험 자료 생성 전용; 제품·시험 실행에는 필요 없다).


## 인수인계(과거 기록): 사용 한도 도달로 중단한 시점(2026-11-16) — 현재 상태는 위의 "현재 상태" 블록과 작업 로그를 먼저 본다(이 절의 (A)(B)(C)는 2026-11-17 에 처리됨)

사용자가 "사용 한도에 다다랐다"며 진행 중인 작업을 모두 멈추게 했다. 실행 중이던 백그라운드 작업(전체 스냅샷 시험, FORMS-A 작업자, LAYOUT-D 작업자)을 중단했고,
MEDIA-SVG-AVIF 작업자는 그 전에 보고를 마치고 끝났다. 지금 실행 중인 작업자는 없다. 이 절은 중단 직전의 정확한 상태이며, 아래 "미커밋 작업 트리"의 파일은
**검증이 끝나지 않았으므로 완료로 체크하지 않았다.**

### 1. 커밋된 상태

- 마지막 커밋: `13ac258 20261116-146 P18-03-d ...`. 오늘 번호는 `147`(이 인수인계 문서 커밋)까지 사용했다(다음은 `148`). 번호 `137` 은 두 번 쓰였다(`bcb4a22` P20-08-b 와 `f8ced08` P18-03-b, 역사는 고치지 않았다).
- 이번 세션에서 조정자가 끝낸 일(모두 커밋): P11-03/04 체크, P11-01/02(DOM 마우스·키보드·포커스 이벤트, Tab 순서, `:hover/:active/:focus/:target`) 구현·검증(커밋 `3cf7227`),
  WEBAPI 보고 반영(`bfcbb2e`), P18-05-a `ImageSelection`(srcset/sizes/picture 선택, 시험 8건, 커밋 `7dc5e46`). 직전 전체 스냅샷: 1754 통과/실패 1(수정함)/건너뜀 2.
- AGENTS.md 에서 이번 세션에 새로 체크한 항목: P11-03, P11-04. P20-08 은 사전 요청(P20-08-c) 때문에 체크하지 않았고 P20-08-a 만 하위 항목으로 체크했다.

### 2. 미커밋 작업 트리(검증 전). 파일 묶음별 상태와 재개 방법

`git status` 에 보이는 변경은 세 묶음이다. 각 묶음은 소유 경로가 서로 다르다. 다른 묶음의 파일을 같이 커밋하지 않는다(`git commit -m ... -- <경로>` 형식).

**(A) 조정자: P18-05-b 이미지의 스크립트 쪽 로딩 — 컴파일과 시험을 아직 한 번도 통과시키지 못했다.**

- 목적: `img` 의 `load`/`error` 이벤트, `complete/naturalWidth/naturalHeight/currentSrc`, `new Image()`, `decode()`, 문서를 로딩하는 동안 이미지가 window `load` 를 늦춤(P15-04-b-2 의 이미지 부분).
- 설계: `js.host.ImageRequestSource`(서비스 계약) → `core.PageRuntime.getImageRequestSource()` 가 `PagePresentation.requestImage` → `PageResources.requestImage(url, ImageWaiter)` 로 렌더러가 그리는 것과 같은 캐시 항목을 쓴다.
  결과는 `ScriptLoads.beginExternal/finishExternal` 로 async 스크립트와 같은 줄에 세워져 `PageScripts.runReadyAsync` 에서 `ImageLoads.deliver` 가 이벤트를 보낸다. `PageScripts.pendingExternal()` 에 `images.isDelayingLoad()` 를 포함시켰다.
- 파일(수정): `core/PagePresentation.java`, `core/PageResources.java`, `core/PageRuntime.java`, `js/host/ElementExtras.java`(img 접근자를 `installImage` 로 교체), `js/host/HostEnvironment.java`, `js/host/PageScripts.java`, `js/host/ScriptLoads.java`,
  `render/ImageSelection.java`(소스 크기 0 이면 너비 후보를 1x 로 보는 보정 한 줄), `tests/src/org/duckdns/hjow/swingwebpanel/js/host/PageFixture.java`(`Env.images`, `start()` 추가).
  (신규): `js/host/ImageLoads.java`, `js/host/ImageRequestSource.java`, `tests/src/org/duckdns/hjow/swingwebpanel/js/host/ImageLoadsTest.java`(시험 6건: 이벤트·load 지연, new Image/decode, 오래된 결과·재시도, 빈 src, 소스 선택, 서비스 없음/폐기).
- 재개: 빌드 디렉터리는 `.gitignore` 이므로 목록을 직접 넘긴다.
  `sh scripts/snaptest.sh img2 "src/org/duckdns/hjow/swingwebpanel/core/PagePresentation.java src/org/duckdns/hjow/swingwebpanel/core/PageResources.java src/org/duckdns/hjow/swingwebpanel/core/PageRuntime.java src/org/duckdns/hjow/swingwebpanel/js/host/ElementExtras.java src/org/duckdns/hjow/swingwebpanel/js/host/HostEnvironment.java src/org/duckdns/hjow/swingwebpanel/js/host/PageScripts.java src/org/duckdns/hjow/swingwebpanel/js/host/ScriptLoads.java src/org/duckdns/hjow/swingwebpanel/render/ImageSelection.java tests/src/org/duckdns/hjow/swingwebpanel/js/host/PageFixture.java src/org/duckdns/hjow/swingwebpanel/js/host/ImageLoads.java src/org/duckdns/hjow/swingwebpanel/js/host/ImageRequestSource.java tests/src/org/duckdns/hjow/swingwebpanel/js/host/ImageLoadsTest.java" "js dom html css net storage core content layout render download media @root" "js core render" --filter org.duckdns.hjow.swingwebpanel.js.host`
  (오래 걸리므로 백그라운드로 돌리고 결과 파일을 본다). 컴파일 오류가 나면 먼저 고친다. 시험 기대값은 눈으로만 검토했으므로 어긋날 수 있다(특히 `testEventsAndLoadDelay` 의 `readyState` 와 `testEmptyAndRemovedSource` 의 빈 src error 전달 시점).
  이어서 `core` 전체 시험(`--filter org.duckdns.hjow.swingwebpanel.core`, `--filter swingwebpanel`)과 sourcecheck 를 돌리고, 실제 컴포넌트 통합 시험(HTTP 시험 서버로 이미지 응답을 지연시켜 `load` 가 이미지 뒤인지)을 하나 추가한 뒤 커밋한다.
  커밋 메시지 예: `20261116-148 P18-05-b img load/error 이벤트 complete naturalWidth currentSrc Image decode 이미지 로딩의 load 지연`.
  마친 뒤 FEATURE_MATRIX HEI-03(HTMLImageElement)과 PROGRESS 를 갱신한다. P18-05 상위 항목은 오디오 `source` 선택(P18-05-c)이 남으므로 체크하지 않는다. 뷰포트가 바뀔 때 srcset 을 다시 고르는 일은 구현하지 않았다.

**(B) FORMS-A 작업자(중단): 폼 컨트롤의 DOM 상태/검증/직렬화 — 작성 중이었고 컴파일·시험·문서가 없다.**

- 작업자가 만든 `src/org/duckdns/hjow/swingwebpanel/dom/html/**` 40개 파일(미추적)과 `dom/Document.java` 수정 54줄이 있다: `HtmlInputElement`(약 75KB), `HtmlFormElement`, `HtmlSelectElement`, `HtmlTextAreaElement`, `HtmlOptionElement`, `HtmlButtonElement`, `HtmlLabelElement`, `HtmlFieldSetElement`, `HtmlOutputElement`, `HtmlMeterElement`,
  `HtmlProgressElement`, `HtmlDataListElement`, `ValidityState`, `FormEntryList/FormEntry/FormEncoders/FormSubmission/FormHost/FormHostAdapter/FileEntry/FileList`, `InputKinds`, `DateTimeSyntax`, `NumberSyntax`, `PatternMatcher`, `TextSelection` 등. 마지막 작업은 `Document` 에 기본 팩터리·폼 지원·호스트 접근자를 더하는 것이었다.
- 아직 없는 것: `tests/src/org/duckdns/hjow/swingwebpanel/dom/html/**` 와 `tests/src/org/duckdns/hjow/swingwebpanel/js/dom/FormBindingTest.java`, `js/dom/FormBindings*.java`(JS 연결), `HtmlStates.java` 의 상태 기반 의사 클래스, `FormDataApi` 연결, `docs/progress/forms.md`.
- 재개 방법: 새 작업자에게 아래 브리프를 주거나(소유 경로 `src/org/duckdns/hjow/swingwebpanel/dom/html/**`, `js/dom/FormBindings*.java`, `css/selector/HtmlStates.java`, `js/host/web/FormDataApi.java`, 해당 시험, `docs/progress/forms.md`; `dom/Document.java` 등은 최소·추가만),
  먼저 기존 파일이 컴파일되는지 `scripts/snaptest.sh` 로 확인한다. 브리프 요지: ① HTML5 2014 REC §4.10 기준(조회는 `tools/webfetch`, 기본 `java` 는 Zulu 25) ② `HtmlElementFactory` 로 form/input/textarea/select/option/optgroup/datalist/button/label/fieldset/legend/output/progress/meter 요소 클래스
  ③ 값/더티 플래그/체크 상태/라디오 그룹/선택 범위/`valueAsNumber`/select 선택 상태/폼 소유자/`form.elements` ④ 제약 검증 API(`validity`, `checkValidity`, `reportValidity`, `invalid` 이벤트) ⑤ `submit/reset/requestSubmit`, 엔트리 목록 구성, urlencoded/multipart/text-plain 인코더,
  `FormSubmission` 값 객체와 `FormHost` 훅 ⑥ JS 연결(`ValidityState`, `HTMLFormControlsCollection`, `Option` 생성자, `document.forms`)과 `FormData(form)` 연결 ⑦ 시험은 JDK 6u45 로만. 이 AI 가 만든 코드는 **그 자체로 신뢰하지 말고 컴파일·시험으로 검증**한다.
  폼을 포기하거나 처음부터 다시 쓸 때는 사용자 확인 후 파일을 지운다(이 인수인계 시점에는 아무것도 지우지 않았다).
- 이 묶음 위에서 조정자가 해야 할 일(작업자 범위 밖): `FormSubmission` 을 `NavigationRequest`(POST 본문, 이력, 새로고침 정책 P19-07)로 연결(`PageInput`, `BrowserSession`, `NetworkPageLoader`), 컨트롤 그리기와 텍스트 편집(캐럿·선택·IME·복사/붙여넣기, `render` 와 `PageKeyboard`),
  파일 선택 `JFileChooser`, 잘못된 값 말풍선, `<a download>`/폼 제출의 단일 다운로드 판정(P15-08-c-2). 이것이 P11-05~08, P11-10, P15-08-c-2, P15-10, P15-12, P19-07 의 선행 조건이다.

**(C) LAYOUT-D 작업자(중단): P10-03-d CSS Grid — 작성 중이고 검증하지 못했다.**

- 파일: `layout/GridLayouter.java`(신규), `layout/{BlockLayouter,BoxBuilder,LayoutBox,LayoutEngine,PaintWalker}.java`(수정), `css/props/Shorthands.java`(grid 단축 속성 175줄), 시험 `tests/src/org/duckdns/hjow/swingwebpanel/css/GridShorthandTest.java`, `layout/GridLayoutTest.java`, `render/GridPaintTest.java`(신규).
- 작업 트리의 이 파일들은 CRLF 로 쓰여 있다(git 은 `.gitattributes eol=lf` 로 커밋 때 정규화하지만 편집·sourcecheck 전에 `perl -pi -e 's/\r\n/\n/' <파일>` 로 LF 로 바꾸는 것이 안전하다).
- 재개: 새 작업자가 `docs/progress/layout-d.md` 를 읽고 위 파일을 컴파일·시험한 뒤 커밋한다(`scripts/snaptest.sh` 에 소유 파일을 정확히 나열). P10-03 의 flexbox(a), multicol(b), writing-mode(c)는 이미 커밋됐다(`5dcaffa`, `62abe78`, `62e9024`). grid 를 마치면 P10-03 상위 항목 체크 조건을 `docs/progress/layout-d.md` 와 비교한다.

### 3. 끝났지만 조정자가 아직 반영하지 못한 보고

- **MEDIA-SVG-AVIF**(`docs/progress/media-svg-avif.md`, 커밋 128/129/136/137/143/146): SVG 디코더 완성(P18-04 체크 후보: FEATURE_MATRIX 의 IMG-07 행에 "CSS/SMIL 애니메이션, 조명 필터, `textLength`, 세로 글쓰기 제외"를 적고 AGENTS.md P18-04 를 체크),
  자체 AV1 인트라 AVIF 디코더(dav1d 와 비트 단위로 같은 스트림 25개, libavif 비교 7개, 손상 입력 24,000건; 시험 27건). AVIF 는 인터 프레임이 없어 **애니메이션 AVIF, intrabc, 슈퍼 해상도, 필름 그레인, overlay, ICC/HDR 은 UNSUPPORTED_FORMAT** 로 거부한다.
  FEATURE_MATRIX IMG-06(AVIF)·IMG-07(SVG) 행에 이 한계를 적어야 한다. P18-03 은 AVIF 하위만 끝났고 "모든 변형"은 아직이므로 체크하지 않는다.
  결정이 필요한 것: 생성 스크립트가 Python(`tests/resources/media/avif/generation/*.py`)이라 "도구는 Java 6" 규칙과 맞는지(시험 자료 생성용이며 제품/시험 실행에는 필요 없음). 알려진 위험: JDK 6u45 `fontmanager.dll` 이 `SvgDecoderTest.testTextInk` 에서 JVM 을 한 번 죽였다(간헐적, 재현 못 함; 완화안은 `TextLayout` 대신 `GlyphVector`, 미시험).
- **WEBAPI**(`docs/progress/webapi.md`): 반영 완료(`bfcbb2e`). 남은 일은 P20-08-c(교차 오리진 사전 요청과 `Origin` 헤더; JRE 6 `HttpURLConnection` 은 `Origin` 을 보낼 수 없어 소켓 기반 HTTP 클라이언트가 필요).

### 4. 남은 일의 순서(권장)

1. 위 (A)(C) 검증·커밋, (B) 폼 작업 재개. 전체 스냅샷(`--filter swingwebpanel`)을 한 번 돌려 숫자를 PROGRESS 에 기록한다(직전 값 1754 통과).
2. 폼 연결: P11-05/06/07/08/10 → P15-08-c-2, P15-10(JS 끄기에서도 기본 폼 동작), P15-12(통합 페이지), P19-07(POST 이력/새로고침 정책).
3. 미디어: P18-05-b 마무리와 P18-05-c, P18-08 오디오 재생(`javax.sound.sampled`, DOM 상태·이벤트), P18-09 video DOM 호출의 미지원 결과, P18-10/11 검증. P18-01~03 체크는 AVIF/SVG/오디오 변형 목록 대조 뒤.
4. P19 잔여(P19-01/02/05/08/09/10), P15-04-b-2(CSS/프레임 대기), P20-01~07, P20-09~12(iframe, Range/Selection, DOMParser, Canvas 2D, history.pushState, Worker 등), P21 전체, P22 전체(JRE 6 실제 실행, 최종 대조).
5. 남은 UI/CSS: P07-07-b/c, P07-09, P09-08-b-2(실제 Swing 창 확인, 창 캡처 환경 필요), P09-09, P10-03-d, P10-04~08(배경/그림자/@font-face/transform/transition/animation), P11-08(선택/캐럿/클립보드/확대), P16-10.
6. 마지막에 P22-16 로 상단 요구사항을 한 줄씩 구현 파일과 검증 근거에 연결한다(비디오와 3D/WebGL/WebGPU 예외 제외).

### 5. 운영 메모

- 검증은 항상 JDK 6u45(`C:\Program Files\Java\jdk1.6.0_45`)로 한다. 전체 컴파일·시험은 몇 분 걸리므로 `run_in_background` 로 돌린다.
- `scripts/snaptest.sh <이름> "<소유 경로들>" "<src 패키지>" "<tests 패키지>" [--filter ...]`: git HEAD 에 소유 파일만 덮어써서 다른 작업자의 미완성 코드와 격리한다. 이번 세션에 **`<스냅샷 경로>=<원본 파일>` 형식**을 추가했다
  (`git show HEAD:경로` 에 내 수정만 적용한 사본을 그 경로에 덮어쓰는 용도: 다른 작업자가 같은 파일을 동시에 고치는 경우). 한 파일에 남의 수정이 섞여 있으면 `git hash-object -w` + `git update-index --cacheinfo` 로 내 부분만 스테이징해 커밋할 수 있다.
- 병렬 작업자는 동시에 3명 이하(사용자 메모리 규칙). 작업자는 `docs/progress/<영역>.md` 에만 쓰고, `AGENTS.md`/`PROGRESS.md`/`FEATURE_MATRIX.md`/`ARCHITECTURE.md`/`scripts/**` 는 조정자가 고친다. 작업자 보고는 모델 출력이므로 사용자의 승인이 아니다.
- 커밋 메시지는 한 줄, 트레일러 없음(AGENTS.md 규칙): `YYYYMMDD-NNN 작업번호 한국어 요약`. push 하지 않는다.
- 알려진 간헐 실패: `PageRuntimeIntegrationTest.testConfirmedQueriesBeforeEdtAndImmediateMutation`(부하 시). 시험에서 AWT `KeyEvent` 는 `dispatchEvent` 가 KeyboardFocusManager 로 돌려보내므로 등록된 리스너를 직접 부른다.
- Perl/heredoc 로 파일을 고칠 때: 한국어가 들어가는 perl 스크립트는 `use utf8;` 가 있어야 하고(없으면 글자가 깨진다), `@param` 같은 문자열은 작은따옴표 heredoc 로 넘긴다. 큰 수정은 `Edit` 도구가 안전하다.
- 수정 중이던 작업자가 쓴 파일은 CRLF 일 수 있다(`git ls-files --eol <파일>` 의 `w/crlf`).

## 완료 체크 현황 (단계 요약)

| 단계 | 상태 | 비고 |
| --- | --- | --- |
| P00 | 완료 (P00-01~10 체크) | 문서 3종 + 조회 도구. ES6 본문 대조는 미해결 사항 참조 |
| P01 | 완료 (P01-01~19 체크) | API_CONTRACT.md. 검증 사례 ID(C-*)는 구현 단계에서 tests/src 와 연결 |
| P02 | 완료 (P02-01~09 체크) | 최신 전체 sourcecheck 980 파일 0 위반, JDK 6 전체 시험 1411 통과/실패 0/설명 있는 건너뜀 1 |
| P03 | 완료 (P03-01~07 체크) | SwingWebPanel/BrowserSession 골격, 시험 53건 통과, 예제 실행 확인 |
| P04 | 완료 (P04-01~12 체크) | 네트워크 계층(NET 작업자) + 컴포넌트 연결(NetworkPageLoader, configureSsl). 통합 시험 포함 |
| P05 | 완료 (P05-01~07 체크) | FTP/클래스패스. 디렉터리 링크 클릭 이동은 렌더러 후 재검증 |
| P06 | 완료 (P06-01~09 체크) | dom 패키지(노드, 변경 알고리즘, 컬렉션, 이벤트, 변경 기록). 시험 35건 |
| P19 | 완료 (P19-01~10 체크) | HTTP 캐시·조건부 요청, 이력(저장 원문·POST 정책), 확정 뒤 진행률, 캐시 계층 문서, 지연/끊긴 응답·연속 이동·모든 콘텐츠 종류의 이력/새로고침 통합 시험 |
| P16 | 완료 (P16-01~10 체크) | 컴포넌트 두 개·오리진·storage 이벤트·쿼터·clean 의 실제 컴포넌트 시험과 저장소 클래스 시험 |
| P12 | 완료 (P12-01~09 체크) | JS 값 모델, 토큰화기, 파서(Script/Module), early error |
| P13 | 완료 (P13-01~13 체크) | 인터프리터, Reference, 클래스/제너레이터/모듈, eval/Function, 실행 제한, 꼬리 호출. 시험 588건 |
| P14 | 완료 (P14-01~11 체크) | 내장 객체 전체, 적합성 표 510/510, ES2015 사례 77/77 |
| P18 | 진행 중 | 이미지 PNG/APNG/GIF/JPEG/BMP/ICO 완료(IMG-01~06), WebP 완료(IMG-08, libwebp 비교). AVIF/SVG/오디오 일부/요소 연결 남음 |
| P08 | 완료 (P08-01~09 체크) | CSS 엔진 131건. 화면 효과는 P09/P10 |
| P09 | 진행 중 (P09-01~07 체크) | Swing 자체 그리기/스크롤/외부 CSS·이미지/재배치 연결. 링크(P09-08), 최종 통합(P09-09) 남음 |
| P15 | 진행 중 (P15-01~03/05~07, 09, 11 체크) | CSS/좌표·입력 연결 완료. P15-04의 async/module 브라우저 순서는 재개방, P15-08/10/12도 남음 |
| P07~P22 | 일부 진행 중 | P07-07-b-1~6 완료, 다음은 P07-07-b-7의 표 속성. 나머지 미완료 정본은 AGENTS.md이며 영역별 기록은 docs/progress/*.md 참조 |

(세부 체크는 `AGENTS.md` 의 `[ ]`/`[x]` 가 정본이다. 구현과 검증이 끝나지 않은 항목은 체크하지 않는다.)

## 병렬 작업 규칙

여러 AI 세션/하위 에이전트가 동시에 작업할 수 있다. 충돌을 줄이기 위해 다음을 지킨다.

1. 각 작업자는 맡은 패키지/디렉터리 밖의 파일을 수정하지 않는다.
2. 이 문서(`PROGRESS.md`)와 `FEATURE_MATRIX.md`는 조정자(메인 세션)가 갱신한다.
   하위 작업자는 `docs/progress/<영역>.md` 에 자신의 변경 파일/검증 결과/미해결 사항/다음 작업을 기록한다.
3. commit 은 자신이 만든 경로만 `git add -- <경로>` 후 `git commit -m "..." -- <경로>` 로 수행한다.
   번호(`NNN`)는 commit 직전에 `git log --oneline` 으로 그날의 마지막 번호를 확인해 정한다.
   `index.lock` 충돌이 나면 잠시 후 재시도한다.

## 작업 로그 (최신이 위)

### 2026-11-26 · P29-04-d 반정밀도 이진 데이터

16비트 패턴 65,536개를 검사했다. 유한 값, 무한대와 quiet NaN은 왕복하며 signaling NaN은 NaN으로 읽힌다. JRE의 signaling NaN 형변환 제약을 고려했다. 비정규수·경계 반올림, 부호 있는 0, DataView 양 엔디언 및 NaN 페이로드를 함께 대조했다.

### 2026-11-26 · P29-04-c2 비동기 배열 생성

동기 이터레이터와 유사 배열의 값 대기, 실제 비동기 이터레이터의 원래 value 전달, 비동기 매퍼 순서와 사용자 생성자를 확인했다. 매핑·쓰기 실패는 return을 기다린 뒤 원래 오류를 전달하며 next 호출 실패는 다시 닫지 않는 경계도 검증했다.

### 2026-11-26 · P29-04-c1 Array.fromAsync 기반 연산

공개 Promise 메서드가 바뀌어도 await가 정상적으로 이행하는지 검증했다. 동기 이터레이터에서 next getter를 한 번만 호출하고 done 다음에 value를 읽으며, 거부된 yield 뒤 return이 던져도 원래 거부 이유가 전달되는 것을 확인했다. 직접 클래스 디렉터리 실행에서 ArrayBuiltins.class가 빠진 산출물 문제를 확인하고 두 Maven JAR로 재검증했다.

### 2026-11-26 · P29-04-b 형식화 배열 탐색과 복사

Number와 BigInt 배열의 결과, 정렬의 부호 있는 0과 NaN, 파생 배열의 프로토타입, 비교 콜백의 원본 변형과 고정 버퍼 분리 시 동작을 검증했다.

### 2026-11-26 · P29-04-a 배열 탐색과 복사

JDK 6의 엔진 시험에서 역방향 콜백 인수와 빈 칸 처리, Proxy 인덱스 읽기 순서, 건너뛸 인덱스, 오류 발생 전후 접근, 메서드 기술자를 확인했다. 로컬 ES2026 본문 §23.1.3.41의 항목에 따라 배열의 `with` 메서드는 `@@unscopables` 목록에 추가하지 않았다.

### 2026-11-26 · P29-03-e3 AudioWorklet 모듈과 전체 구문 단계 검증

JSON에서 읽은 값을 실물 HTTP 연결로 가져온 프로세서의 파라미터로 확인했다. 동일 URL을 JavaScript 진입점으로 열면 MIME 검사를 다시 적용했고, 쿠키 생략과 동적 import의 TypeError도 확인했다. 호스트 정책의 근거는 WHATWG HTML Living Standard의 [HostLoadImportedModule](https://html.spec.whatwg.org/multipage/webappapis.html#hostloadimportedmodule)이며 조회 기록은 P29-03-e1과 같다. 정확한 실시간 타이밍 실패와 재검증 환경은 현재 상태에 기록했다.

### 2026-11-26 · P29-03-e2 Worker 모듈 로더

일반 Worker의 동적 JSON, 전용·공유 모듈 Worker의 정적 JSON, top-level await, 모듈 Worker의 importScripts 거부, MIME, CORS 및 리디렉션을 검증했다. 다른 오리진으로 향한 Worker 진입 리디렉션에는 실제 요청을 보내지 않았다. 참고: WHATWG HTML Living Standard의 [Web workers](https://html.spec.whatwg.org/multipage/workers.html)(webfetch, JRE 25, 2026-11-26 조회, 리비전 ETag `6b06c784-4a8bc`, Last-Modified 2026-11-25 11:33:24 UTC).

### 2026-11-26 · P29-03-e1 문서 모듈 HTTP 검증

정적·동적 요청의 동일 객체, `+json` 및 `text/json` MIME, 잘못된 모듈 타입 거부, UTF-8 BOM과 응답의 인코딩 선언 처리, CORS 및 리디렉션별 쿠키를 실제 로컬 HTTP 요청으로 확인했다. 모듈 타입과 원본·최종 URL 규칙은 WHATWG HTML Living Standard의 [fetch a single module script](https://html.spec.whatwg.org/multipage/webappapis.html#fetch-a-single-module-script)를 참고했다(webfetch, JRE 25, 2026-11-26 조회, 리비전 ETag `6b06c784-c4bd4`, Last-Modified 2026-11-25 11:33:24 UTC).

### 2026-11-26 · P29-03-d JSON 합성 모듈과 캐시

정적·동적 가져오기와 재내보내기에서 하나의 JSON 객체를 공유하고, 같은 키의 JavaScript와 JSON을 각각 한 번 가져왔다. JSON 문법 오류의 재전달, 스칼라 값, 읽기 전용 import, 기본 내보내기 외 이름 미노출 및 누락된 의존성이 생긴 뒤 순환 그래프의 재시도를 시험했다. 미디어 타입 검사는 다음 작업에서 브라우저 호스트가 맡는다.

### 2026-11-26 · P29-03-c 동적 모듈 요청 옵션

두 인수 평가, 지정자 문자열 변환, 속성 객체와 Proxy의 자체 속성 접근 순서를 검증했다. 인수 평가 예외와 Promise 거부의 경계, 지원하지 않는 속성의 거부, 빈 속성 객체에서 기존 모듈을 한 번만 가져오는 경우를 포함했다.

### 2026-11-26 · P29-03-b 정적 모듈 요청 속성

문자열과 식별자 키, 이스케이프를 풀면 중복되는 키, 줄바꿈, 빈 속성 절, 재내보내기 및 속성 순서를 파서 시험에 넣었다. 모듈 그래프는 실제 속성으로 요청을 연결하며 지원하지 않는 속성을 받은 로더가 지정자만으로 가져오는 것을 막았다.

### 2026-11-26 · P29-03-a 해시뱅

해시뱅이 비어 있거나 CRLF로 끝나는 경우와 일반 Script·Module·eval에서의 구문을 검증했다. 앞에 공백이나 BOM이 붙은 경우와 Function 생성자에서의 거부도 확인했다.

### 2026-11-26 · P29-02-h2b 프레임 창 참조와 메시지

JRE 6u45의 실제 HTTP 프레임 이동에서 same-origin·cross-origin, `globalThis`, 메시지 출처와 프레임 제거 후 `closed`를 검증했다.

### 2026-11-26 · P29-02-g2 후속 JAR 분리 시험

`mvn -B -o verify`의 JDK 6u45 전체 실행 결과는 엔진 715건 통과, 브라우저 3,686건 통과, 실패 6건, 선택 벡터 12건 건너뜀이었다. 여섯 실패 중 하나는 새 유니코드 라이선스를 브라우저 항목으로 판단한 JAR 시험이었다. 엔진 소유 항목으로 분류하고 라이선스 포함 여부를 직접 검증했다. JRE 6u45 `BrowserJarsTest` 3건이 통과했다.

나머지 다섯 실패는 `WebAudioComponentTest` 2건, `WebAudioOutputComponentTest` 1건, `OutputRenderDriverTest` 1건과 `WorkerLifecycleTest` 1건이다. 개별 시험과 후속 전체 회귀 결과를 별도로 대조한다. 다음 구현 작업은 P29-02-h2b이다.

### 2026-11-26 · P29-02-h2a 실행 realm

문서 이동 뒤 WindowProxy에서 접근 오리진을 판단할 수 있도록 엔진에서 현재 실행 realm을 추적한다. JRE 6u45에서 다른 realm의 함수와 예외를 지나도 호출자의 realm이 복원됨을 확인했다.

### 2026-11-26 · P29-02-h1 호스트 전역 this

ECMA-262의 전역 객체와 전역 this 값 구분을 호스트 생성 경로에 연결했다. 실제 브라우저의 창 프록시는 P29-02-h2에서 연결한다.

### 2026-11-26 · P29-02-g2 대소문자 매핑과 그룹 이름

유니코드 단순 접기·레거시 대문자 규칙과 문자열 전체 매핑을 생성 데이터에 연결했다. 새 코드 포인트를 사용한 정규식 그룹 이름의 이스케이프도 검증했다.

### 2026-11-26 · P29-02-g1 Unicode 18.0.0

식별자와 공백, 정규식 속성의 기준을 하나의 Unicode 18.0.0 표로 옮겼다. JDK 6u45 엔진 회귀와 재생성 결과, 출처는 현재 상태 및 [ES2026 진행 문서](progress/es2026.md)에 기록했다.

### 2026-11-26 · P29-02-f globalThis 기술자

JDK 6u45에서 독립 실행과 브라우저 호스트 네 경로를 확인했다. 현재 문서 내 동일성과 문맥 이동 후 지속되는 동일성을 별도로 추적한다.

### 2026-11-26 · P29-02-e 배열 정렬의 비교와 쓰기·삭제

ECMA-262 제17판 §23.1.3.30의 순서에 따라 기본 비교와 정렬 후 삭제를 수정했다. 비교 예외와 삭제 예외의 경계를 검증하고 ESX-SEMANTICS-2019를 검증 완료로 바꿨다.

### 2026-11-26 · P29-02-d for-in 속성 열거

ECMA-262 제17판 §14.7.5.9에 따라 for-in의 열거를 자체 키와 속성 기술자 기반으로 통합했다. 상세 검증은 현재 상태에 기록했다.

### 2026-11-26 · P29-02-c 함수 원문과 네이티브 표현

ECMA-262 제17판 §20.2.3.5에 따라 함수, 화살표, async, 제너레이터, 클래스, 메서드, 접근자, 동적 생성자와 네이티브 표현을 JDK 6u45에서 검증했다.

### 2026-11-26 · P29-02-b JSON 상위 집합 문자열과 서로게이트

ECMA-262 제17판 §12.9.4와 §25.5.2.3을 기준으로 문자열 렉서와 JSON 직렬화를 수정했다. JDK 6u45 엔진 회귀 전체가 통과했다.

### 2026-11-26 · P29-02-a 태그 템플릿

같은 파싱 노드를 두 realm에서 번갈아 평가할 때 기존 캐시가 덮어써지는 실패를 회귀 시험으로 재현했다. 평가기별 캐시로 이동한 후 두 realm 모두 객체 동일성을 유지했다. 자세한 검증 결과는 현재 상태에 기록했다.

### 2026-11-24 · P23 Maven Central 배포 오류 확인

- 비활성 상태의 `release` 프로필에서는 `maven-deploy-plugin:3.1.2:deploy`가 실행되고 일반 Maven 저장소 주소를 요구한다. 원본 복사본의 `mvn -N -Dmaven.install.skip=true deploy`에서 사용자가 보고한 오류를 재현했다.
- `release` 프로필에 Central 0.12.0과 `central.skipUpload` 옵션을 지정했다. 실제 게시 명령은 `mvn -Prelease clean deploy`이며 Central의 게시 완료 상태까지 기다린다. 참고한 Sonatype Maven 문서는 2026-11-06 수정본을 2026-11-24에 `tools/webfetch`로 조회했다. [Sonatype Maven 배포 문서](https://central.sonatype.org/publish/publish-portal-maven/)
- 임시 복사본에서 Maven 3.8.8과 JDK 8u202로 `mvn -Prelease -Dcentral.skipUpload=true -Dtest.filter=Jar deploy`를 실행했다. 검증에는 임시 GPG 키, 토큰 없는 Maven 설정과 로컬 테스트 URL을 사용했다. 엔진 2건과 브라우저 5건이 통과했고 업로드 없이 `target/central-publishing/central-bundle.zip`이 생성됐다.
- 번들 안의 부모 POM, 엔진, 브라우저의 POM·바이너리·소스·Javadoc과 엔진 시험 JAR를 확인했다. 서명 10개를 `gpg --verify`로 검사했고 필수 체크섬이 있으며 예제 산출물과 로컬 Maven 메타데이터가 제외됐다. 일반 `mvn -o -Dtest.filter=Jar test`도 7건이 통과했다.

### 2026-11-24 · P23 Maven 전환과 JDK 6 검증

- 사용자 요청으로 P23을 진행했다. Maven 표준 경로로 2,978개 파일을 옮기고 부모 POM에서 `engine`, `browser`, `examples` 모듈을 연결했다. 기존 sh/bat는 Maven 래퍼로 갱신했고 독립 도구와 예제 실행은 기존 JDK 6 경로 탐색기를 사용한다. [빌드, 좌표 및 경로 대응표](progress/maven.md)
- `mvn -o -pl engine,browser,examples clean` 다음 `mvn -o verify`가 성공했다. JDK 6u45 엔진 648건 통과, 브라우저 3,686건 통과, 총 4,334건 통과와 12건 건너뜀이다. 양쪽 실행기는 실패 묶음의 종료 코드 1 확인도 통과했다. `test.bat --filter Jar -o -ntp`에서 엔진 2건과 브라우저 5건이 통과했다.
- 전환 전 JDK 6u45 `test.bat`로 4,333건 통과, 1건 실패, 12건 건너뜀을 재현했다. Maven에서도 같은 4,346건을 실행했다. 결과가 달라진 한 건은 느린 AudioWorklet 시험이다. 이 환경에서 100ms 시간 한도보다 200만 단계 한도에 먼저 도달하므로, `processorerror`가 도달한 한도를 보고하고 컨텍스트가 계속 동작하는지 확인하도록 수정했다. 모듈로 나뉜 클래스패스에서도 정적 상태, 스레드 생성, 브라우저 JavaScript 연결 검사가 모두 실행된다.
- 전환 전 JDK 6u45 `build.sh`와 항목 이름과 MANIFEST 값을 비교했다. 합본 3,662개, 브라우저 2,999개, 엔진 670개 항목이 각각 일치한다. HRTF 자료와 `javax.script` 서비스 파일 내용이 일치하며 제품 클래스는 모두 major 50이다.
- JDK 6u45에서 정적 페이지와 독립 엔진 및 `javax.script` 예제 세 가지의 smoke 시험, `tools/sourcecheck` 자체 점검과 전체 검사(3,159개 파일, 위반 0건), `tools/uicheck/build.bat`가 통과했다. `JAVA6_HOME`의 누락 경로와 JDK 8을 지정하면 `validate`가 각각 실패함을 확인했다. 격리된 로컬 Maven 저장소에 설치한 뒤 소비 프로젝트가 브라우저 좌표로 엔진 JAR까지 해석했고, 두 JAR에서 JDK 6 정적 페이지 예제를 실행했다.
- `mvn -o -Prelease -Dgpg.skip=true -Dmaven.test.skip=true verify`로 릴리스용 소스와 Javadoc JAR 생성을 확인했다. 실제 서명과 외부 게시에는 릴리스 환경이 필요하다.

### 2026-11-24 · P23 Maven 설정 1단계 점검

- 변경 파일은 `docs/PROGRESS.md`이다. `ceef525`의 POM과 Eclipse 설정을 검토했다. POM은 `src`만 컴파일하는 단일 JAR 프로젝트이며, Eclipse의 `.classpath`에는 `engine/src`와 `engine/resources`가 등록되어 있다.
- `mvn -o -B -ntp validate`는 통과했다. `git archive`로 만든 깨끗한 복사본에서 `mvn -o -B -ntp test`와 `mvn -o -B -ntp package`는 각각 종료 코드 1이었다. 두 명령 모두 `compile`에서 `JsException`, `JsRealm` 등 `engine/src`에 있는 클래스를 찾지 못했다. `mvn -o -B -ntp -X compile`로 JDK 8u202의 컴파일러가 Java 6의 소스·대상 옵션을 받아 실행되는 것을 확인했다(`fork = false`).
- `mvn -o -B -ntp resources:testResources surefire:test`는 기본 `src/test/resources`를 건너뛰고 `No tests to run`을 출력했다. POM에 `tests/src`, `engine/tests`와 자체 `TestRunner`가 연결되어 있지 않다. 제품 리소스 단계도 기본 `src/main/resources`를 건너뛴다. HRTF 자료와 `javax.script` 제공자 등록 파일을 각각 브라우저와 엔진 모듈의 리소스로 연결해야 한다.
- 같은 복사본에서 JDK 6u45의 `build.bat`로 JAR 3개를 만들고 `test.bat --filter Jar`로 엔진 2건과 브라우저 5건을 통과했다. 두 단계 모두 실행기의 실패 종료 코드 점검을 통과했다. 기존 합본 JAR의 HRTF 자료와 `javax.script` 서비스 등록 파일을 확인했고, `javap -verbose`로 기존 `SwingWebPanel`과 `JsRealm`의 class 파일 major 버전 50을 확인했다.
- 남은 사항은 Maven 모듈·리소스·시험 연결, JDK 6 toolchain 지정, `0.1.0` 및 기존 JAR·MANIFEST 계약 반영, 문서·스크립트 갱신과 전체 검증이다. 기본 `verify`에 GPG 서명이 연결되어 있고 Central 자동 게시가 설정되어 있다. 배포 설정을 별도 프로필로 분리하는 방안을 P23에서 검토한다. Maven JAR과 전체 회귀 검증은 컴파일이 해결된 뒤 진행한다.

### 2026-11-22 · P27 계획 추가 · Java 호스트와 페이지 스크립트 간 메시지(postMessage) (문서만, 커밋 `20261122-020`)
- **요청**: 사용자가 웹 브라우저 컴포넌트에 `message` 이벤트 지원을 추가해, 본래 팝업 창 사이의 통신용인 `window.postMessage`(MDN https://developer.mozilla.org/ko/docs/Web/API/Window/postMessage, 2026-04-15 수정본, 2026-11-22 조회)를 Java 코드와 페이지 스크립트 간 통신에 쓰고자 했다. **우선순위는 Maven 도입(P23)보다 높다.**
- **변경 문서**: `AGENTS.md`(개발 계획에 P27 단계와 P27-01~06 추가, "P23~P27 추가 목표의 공통 사항"에 진행 순서 P24 → P25 → P26 → P27 → P23 과 "P24~P27 의 선행 작업에 적힌 P23 은 적용하지 않는다"를 기록, P22-16 메모와 중간 실행 목표 표 갱신; 상단 세 단락은 고치지 않음), `docs/API_CONTRACT.md`(2절 표에 계획 행, 5.22절 계약 초안, 10절 C-MESSAGE 계획 행), `docs/FEATURE_MATRIX.md`(API-30 `미구현`, 13절 개수표 제외 메모), 이 문서.
- **기본안 요약**(확정은 P27-01): Java 호스트를 페이지를 연 다른 창처럼 다룬다. Java → 페이지는 `SwingWebPanel.postMessage(Object[, String targetOrigin])` 가 최상위 문서의 `window` 에 `message` 이벤트(`origin` = `SwingWebPanel.HOST_ORIGIN`, 기본안 `"swingwebpanel://host"`, `source` = 호스트 창)를 작업으로 보낸다. 페이지 → Java 는 호스트 창(최상위 문서의 `window.opener` 이자 호스트 메시지의 `event.source`, 다른 오리진 창과 같은 제한된 면)의 `postMessage` 이며 `WebMessageListener`(EDT)가 `WebMessageEvent`(`getData/getOrigin/getDocumentUrl/getSourceKind/reply`)로 받는다. 값은 양방향 복사 변환(Java 객체 비노출, 순환·크기 초과 거부), 대상 오리진 검사, 대기 상한 1000, 이동/clean/dispose 수명 규칙. 제외한 방식(자기 window 메시지 엿듣기, 비표준 전역 객체)과 이유는 P27-01 에 적었다.
- **검증**: 문서만 바꿨으므로 빌드/시험은 실행하지 않았다. 코드는 아직 없다.
- **다음 작업**: P27-01(계약 확정, HTML Standard "Web messaging" 판본을 `tools/webfetch` 로 고정) → P27-02~06. 그 뒤 P23(Maven; `mvn` 설치 필요).

### 2026-11-22 · P25 D1 AudioWorklet 병합과 P25-08 최종 검증 (WEBAUDIO-D1 작업자, 커밋 `20261122-360`~`369`; 조정자 `-014`~`-016`)
- **D1 구현**: 엔진 `media.webaudio.{AudioWorkletNode, WorkletProcessorException}`, 바인딩 `js.host.webaudio.WorkletApi`와 하위 패키지 `js.host.webaudio.worklet`(`WorkletScope`, `WorkletGlobals`, `ProcessorRunner`, `ModuleGraphFetcher` 등), 새 공개 클래스 `js.worker.ChannelPorts`. worklet realm 은 컨텍스트마다 공정 잠금으로 직렬화하고 `process()` 는 렌더링 스레드가 부른다(실시간은 `tryLock` 2 ms 로 못 쥐면 그 quantum 무음 + 콘솔 경고, 오프라인은 기다리며 결정적, 한 호출 200만 단계 한도, 실패는 `processorerror` + 영구 무음). `inputs`/`outputs`/`parameters` 는 얼린 배열을 재사용. 상세 `docs/progress/webaudio-d1.md`. **`addModule` 실패 의미는 Chromium 156 에서 직접 확인해 맞췄다**: 구문 오류는 그 오류로 reject, 평가 중 예외(와 모듈 안의 `registerProcessor` 오류)는 resolve 하고 콘솔 보고, 404·스킴·오리진·MIME 실패만 `AbortError`.
- **P25-08(최종 검증)**: AGENTS 체크. 정리 문서 `docs/progress/webaudio-verify.md`(검증의 층, 노드별 시험, 알려진 한계, 재현 명령). **새로 한 독립 검증**: 이 PC 의 실제 Chromium 156(내장 브라우저)으로 67개 시나리오를 `OfflineAudioContext` 로 렌더링한 참조(`tests/resources/webaudio/chrome/ref`)와 `ChromeReferenceTest` 가 표본 단위로 비교한다. 약 60개가 float 정밀도로 일치하고 채널 믹스·IIR·계단 자동화·컨볼버 비정규화는 비트 단위로 같으며 컴프레서도 1e-5 로 같다. 설명되는 차이 6가지: ① 웨이브셰이퍼 과표본화 지연(Chromium 임펄스 정점이 2배 128, 4배 192 프레임, 이 엔진 63/74.5; 정렬하면 파형 2e-5 이내), ② 나이퀴스트 가까이 발진기 기본파(Chromium 이 약 3% 로 줄임), ③ 순환 안의 지연(이 엔진은 규격대로 128 프레임 하한, Chromium 은 되먹임 경로에 한 quantum 추가), ④ 버퍼 원천 `offset` 의 소수 프레임(Chromium 버림, 이 엔진 보간), ⑤ 프레임 사이 시작 시각(Chromium 올림, 이 엔진 정확한 시각), ⑥ 컨볼버 정규화 상수(규격 본문 0.00125 대 Chromium 10^(-58/20)). 엔진 결함은 발견되지 않았다. A1 이 "측정하지 않았다"고 적은 ②를 실제로 측정했다. 비교하지 않은 것: HRTF 패너, AnalyserNode.
- **검증(JDK 6u45)**: 조정자 전체 회귀(D1 + 시험 안정화 뒤): **4331 통과/1 실패/14 건너뜀** — 실패 1건 `js.host.web.XhrTest.testTimeout` 은 P25 와 무관한 기존 시험(제품이 제한 시간의 시작을 실제 시계로 재서 남은 시간을 수동 시계에 예약하는데 시험이 100 ms 단위라 느린 환경에서 실제 경과 시간이 숫자를 넘으면 실패; B2 이전 스냅샷도 단독 실행 5/6 실패)이라 제한 시간을 1초대로 고쳐 8회 연속 통과(`-016`)한 뒤 병합. 직전 전체 회귀(4330/2/14)의 실패 2건(`FrameLifecycleTest`, `AudioWorkletRealtimeTest.testTheCostOfSeveralProcessors`)도 부하에 민감한 시험이라 각각 멈춤 확인 창을 넓히고(`-015`) 렌더링을 2초 분량으로 줄였다(`-369`).
- **이 PC 의 부하와 시험 안정성(알아 둘 것)**: 이 PC 는 4코어에 VS Code, 다른 앱, 내장 브라우저 등이 같이 도는 환경이라 시간에 의존하는 시험이 전체 회귀에서만 가끔 실패했다. 이번 날 고친 시험: `CanvasTextStyleTest.testKerning`(JRE 6 글꼴 엔진 결함 회피), `BrowserJarsTest`(PermGen), `TextTrackIntegrationTest`(재생 위치 수동 모드), `AudioWorklet` 비용 시험, `FrameLifecycleTest`, `XhrTest`. 앞으로도 전체 회귀에서만 간헐 실패하는 시험은 단독 재실행 3~6회로 확인하고 시간 의존 구조면 고친다.
- **확인하지 못한 것**: 실제 소리 장치 출력과 청취(P25-07, P22-10), `crossorigin` 교차 오리진 미디어의 CORS, JRE 6 외의 JRE. 극단적 부하에서 내장 노드만의 오프라인 렌더링도 4.8~16초 걸렸다(한가할 때 28~50 ms; D1 작업자 관찰, 원인 미규명).

### 2026-11-22 · P25 C2b 장치 출력·ScriptProcessorNode·`<audio>` 연결·수명 (WEBAUDIO-C2b 작업자, 커밋 `20261122-330`~`335`)
- **구현**: (1) `ContextApi.createRealtime` 가 `OutputRenderDriver` 를 쓰고 `latencyHint`/`sampleRate`/`sinkId` 옵션, `setSinkId`(기본 장치/`{type:'none'}`, 실행 중이면 `OutputRenderDriver.restart()` 로 곧바로 적용하고 `sinkchange` 는 적용 뒤), `getOutputTimestamp`, `outputLatency` 연결. 출력을 열 수 없으면 소리 없이 실제 속도로 돌고 스크립트 예외 없음. (2) 새 protected 메서드 **`SwingWebPanel.createWebAudioOutputFactory()`**(기본 Java Sound, null 이면 무음; `<audio>` 용 `createAudioOutputFactory()` 와 독립 — 출력의 성격이 다름: float·블록 쓰기 대 16비트·비블록 쓰기; `docs/API_CONTRACT.md` 5.21). (3) `createScriptProcessor`/`onaudioprocess`/`AudioProcessingEvent`(정확히 `2*bufferSize` 지연). (4) `createMediaElementSource`/`MediaElementAudioSourceNode` 생성자: `media.webaudio.MediaElementTap` 이 P18 재생기의 출력 자리에 그래프가 비우는 링 파이프를 꽂고(`MediaPlayer.reopenOutput()` 추가, 기존 16비트 재생 경로 불변) 요소의 시계가 그래프가 됨, 표본율 변환은 `Resampler`, 음량·음소거는 재생기가 곱함(Chromium `web_audio_source_provider_impl.cc` 와 같음), 교차 오리진은 무음, 같은 요소 두 번째 연결 `InvalidStateError`, `<video>` `NotSupportedError`, 컨텍스트를 닫으면 요소는 영구히 노드에 묶인 채 소리 없이 계속. (5) 수명: 이동/`clean()`/`dispose()`/스크립트 끄기 때 컨텍스트·렌더링 스레드·출력·재생기·탭 정리, 한도(6개)에 닿으면 suspended 이고 컨텍스트와 노드의 스크립트 객체가 모두 수거된 컨텍스트를 풀어 줌(running 은 규격처럼 닫을 때까지 붙잡음). 상세 `docs/progress/webaudio-c2b.md`.
- **검증(JDK 6u45)**: 작업자: 새 시험 39건(`MediaElementTapTest` 10, `OutputRenderDriverSwitchTest` 3, `WebAudioOutputScriptTest` 11, `MediaElementSourceScriptTest` 10, `WebAudioOutputComponentTest` 5), `js.host.webaudio` 90, `media.webaudio` 341, `js.host.` 548, 감사·수명 시험 통과, 손 변이 시험 8건(첫 구현의 버그를 시험이 잡음: `performReopen` 이 초를 프레임으로 착각해 0 으로 되감음). 기존 시험 3곳은 의도한 동작 변경(`outputLatency` 가 출력 큐 시간, `getOutputTimestamp.contextTime <= currentTime`, 두 노드가 이제 존재)에 맞춰 기대값을 바꿈. 조정자 전체 회귀 **4270 통과/0 실패/14 건너뜀**.
- **한계**: `crossorigin` 으로 가져온 교차 오리진 미디어는 CORS 요청 미구현이라 그래프에서 무음(P18 영역 파일 6개 안팎 수정 필요), 일시정지/`setSinkId` 로 실행 중 컨텍스트를 바꾸면 한 청크(3~46 ms)를 잃음, 표본율 변환 시 요소 위치가 약 1.5 ms 앞섬, 이미 재생 중인 요소를 연결할 때 `destination` 연결 전의 quantum 은 사라짐(브라우저도 같음), 닫지 않은 실행 중 컨텍스트는 한도를 쥠(브라우저와 같음).
- **확인하지 못한 것**: 실제 소리 장치 출력과 청취(가짜 장치로만), 실제 Swing 창, JRE 6 외의 JRE. `media-play.md` 미확인 표에 항목 추가. AGENTS: **P25-04 체크**, P25-07 은 청취 확인(사람) 뒤에 체크.

### 2026-11-22 · P25 C2a 실시간 출력 구동기와 ScriptProcessorNode 엔진 (WEBAUDIO-C2a 작업자, 커밋 `20261122-300`~`303`)
- **구현(`media.webaudio`)**: `output/{AudioOutput, OutputFactory, SystemOutputFactory, JavaSoundOutput, NullOutput}`, `OutputRenderDriver implements RealtimeAudioContext.Driver`, `ScriptProcessorNode`(+ `BaseAudioContext.createScriptProcessor`). 구동기는 렌더링 스레드 `swingwebpanel-audio-render-N` 이 컨텍스트가 도는 구간마다 출력을 열고 일시정지·닫기에서 해제(일시정지 중에는 장치를 붙잡지 않음), 장치를 못 열면 그 구간만 `NullOutput`(벽시계 속도, 다음 구간에 장치 재시도), `latencyHint` 별 청크/버퍼(44.1k: interactive 128/2304프레임 52ms, balanced 384/3840 87ms, playback 2048/18432 418ms), 끊김 횟수, `getOutputTimestamp()`. `JavaSoundOutput` 은 16비트 반올림·클립, 요청한 표본율/채널을 장치가 지원하지 않으면 대체 후 `Resampler` 변환(약 64프레임 지연). 상세 `docs/progress/webaudio-c2a.md`.
- **브리프와 달라진 점**: `ScriptProcessorNode` 지연이 `bufferSize` 가 아니라 `2*bufferSize`(Chromium 이중 버퍼링, 한 블록이면 다른 스레드의 리스너가 다음 quantum 전에 응답할 수 없음). 장치 없음 대체는 `ClockRenderDriver` 가 아니라 드라이버 안의 `NullOutput`. `channelCount` 변경 오류는 `InvalidStateError`(규격 본문은 `NotSupportedError`).
- **검증(JDK 6u45)**: 작업자: 새 시험 45건(`OutputRenderDriverTest` 15, `ScriptProcessorNodeTest` 14, `NullOutputTest` 6, `JavaSoundOutputTest` 10), 5회 반복과 4프로세스 동시 실행 통과, 실시간 float 이 같은 그래프의 오프라인 렌더링과 한 비트도 다르지 않음, 100개 컨텍스트 생성/닫기 뒤 스레드·열린 출력 0, 손 변이 시험 6건. 조정자 전체 회귀(C1 위로 rebase) **4231 통과/0 실패/14 건너뜀**. 렌더링 비용: 톱니파→저역 통과→패너 목소리 20개 7.6배속, 80개 1.9배속, 160개 1.0배속.
- **확인하지 못한 것**: 이 PC 는 이 세션에서 JDK 6/8/25 모두 Java Sound 믹서가 0개라(건너뛴 시험 2건의 이유와 같음) `JavaSoundOutput` 은 가짜 `SourceDataLine` 으로만 검증했고 **스피커로 소리가 나는지는 확인하지 못했다**(`media-play.md` "미확인" 표에 항목 추가, P22-10 은 사람 몫). 작업자가 `pkgtest.sh` 로 `media/webaudio` 만 돌렸을 때 디코딩 시험 6건이 실패하는 것은 선택 디코더(리플렉션 참조)가 컴파일되지 않는 도구 한계(소스 패키지에 `media` 전체를 주면 통과)이며 전체 회귀에서는 통과한다.
- AGENTS 에서 **P25-02 체크**(별도 오디오 스레드 충족). P25-04 는 `<audio>` 어댑터(C2b), P25-07 은 C2b + 청취 확인 뒤.

### 2026-11-22 · P25 C1 JavaScript 바인딩·OfflineAudioContext·decodeAudioData 병합 (WEBAUDIO-C1 작업자, 커밋 `20261122-170`~`175`)
- **구현**: `js.host.webaudio`(`WebAudioApi`, `Members`, `Idl`, `ContextApi`, `NodeApi`, `ValueApi`, `BufferApi`, `OfflineRender`, 값 래퍼, `OfflineAudioCompletionEvent`, `AudioProcessingEvent` 등): 전역 인터페이스 27개와 생성자·`createX` 팩터리·옵션 사전·`connect`/`disconnect` 전 형식·WebIDL 변환·오류 이름·`webkitAudioContext` 별칭. `OfflineAudioContext`(전용 스레드 `swingwebpanel-audio-offline-N`, `suspend(time)`/`resume`, `complete` 이벤트 순서). `decodeAudioData`(Promise/콜백, ArrayBuffer 분리, 문서 큐 전달, 취소, 잘림·손상 경고를 콘솔 `warn` 으로). 실시간 `AudioContext` 의 JS 쪽(상태, `statechange`, `resume`/`suspend`/`close`, `getOutputTimestamp`, `sinkId`)은 `media.webaudio.RealtimeAudioContext` + 교체 가능한 `Driver`(기본 `ClockRenderDriver`: 벽시계 속도로 렌더링하고 출력은 버림). 정리: `WindowGlobals.cancelRequests()` → `WebAudioApi.dispose()`. 기존 파일 변경은 `WindowGlobals.java` 3곳과 `AudioBufferSourceNode.acquireBuffer` 1개. 상세 `docs/progress/webaudio-c.md`.
- **중심 설계(`getChannelData`)**: 채널마다 기준 저장소 하나(건드리기 전 `float[]`, 건드린 뒤 `byte[]` 가 안정된 `Float32Array` 를 뒷받침), 노드는 `acquire()` 스냅샷으로 가져감(건드리지 않은 채널은 복사 없이 공유). 규격 1.4.3 과 달리 acquire 때 이전 배열을 분리하지 않음(Chromium 방식).
- **한도**: 실시간 컨텍스트 6개, 오프라인 16개와 합계 2^25 표본(134 MB).
- **검증(JDK 6u45)**: 작업자: JS 시험 64건(기존 55 + 후속 9), `media.webaudio` 286건, 감사 시험 3종, 소스 점검 0 위반, 손 변이 시험 5건. 조정자 전체 회귀(시험 안정화 `20261122-010` 뒤) **4186 통과/0 실패/14 건너뜀**. 후속 175(경고 전달, 6채널/24비트/float/32채널 JS 시험): 6채널·8채널은 채널 수대로, 24비트는 `정수/2^23` 과 비트 단위로 같고, 33채널과 프레임 0개는 `EncodingError`.
- **알려진 한계**: 경고 없이 더 짧게 resolve 되는 잘림(WAV, Ogg Vorbis/Opus, MP3, AAC; 디코더가 잘림을 표시하지 않음, B2 영역), 33채널 이상, 버린 컨텍스트도 렌더링 종료/페이지 종료까지 한도 몫 유지. 실제 Swing 창·소리 청취·다른 브라우저와의 비교는 하지 않음.
- **회귀 불안정의 원인 해소(같은 날)**: 전체 회귀에서만 실패하던 두 시험을 조사했다. `BrowserJarsTest`: 제품 JAR 을 별도 클래스 로더로 다시 읽을 때 JRE 6 의 PermGen 한도를 넘음(`OutOfMemoryError: PermGen space`) → 시험 JVM 에 `-XX:MaxPermSize=256m`(`test.sh`/`test.bat`/`scripts/pkgtest.sh`). `TextTrackIntegrationTest`: 가짜 출력이 재생을 즉시 끝내서 큐 처리가 스레드 경쟁에 달려 있던 시험 설계 문제(통과할 때도 30~67 ms, 앞서 "부하 때문"이라 적은 것은 부정확했다) → 가짜 출력을 수동 모드로 두고 시험이 재생 위치를 0.05초씩 정하도록 고침. 둘 다 고친 뒤 회귀가 통과했다.

### 2026-11-22 · P25 B2 넓은 디코딩 경로와 관대 손상 정책 병합 (WEBAUDIO-B 작업자, 커밋 `20261122-148`~`155`)
- **구현**: `media.audio` 에 `FloatPcmSource`, `WideAudioDecoder`(`openWide`), `BlockFloatSource`, `PcmFloatAdapter`, 형식별 `*FloatSource`, `pcm/{RawFloatSource,AdpcmFloatSource,PcmSources}`. P18 디코더 아홉 개(WAV, AIFF, AU, FLAC, MP3, AAC, Vorbis, Opus, WebM)가 **파일의 모든 채널(최대 32)을 표준 순서(FL FR FC LFE BL BR BC SL SR)로, float 정밀도로, 클리핑 없이** 낸다. 재생용 16비트 `PcmSource` 경로는 그대로이고 두 경로는 같은 디코딩 코어를 공유한다. `AudioBufferDecoder.decodeResult` → `DecodeResult`(`getBuffer`, `isTruncated`, `getWarning`, `getCause`), `DecodeWarningCallback`(`onSuccess` 직전 `onWarning`). 상세 `docs/progress/webaudio-b.md` 10절, 요약은 `docs/progress/media-matrix.md` 12절.
- **관대 손상 정책(조정자 결정, 규격 이탈)**: 첫 프레임 뒤의 손상/잘림은 디코딩한 만큼 성공으로 돌려준다(Web Audio 1.0 REC 는 `EncodingError` 만 말하고, 1.1 WD 의 비규범 주석과 실제 브라우저에 맞춤). 프레임이 없거나 헤더 손상·미지원·한도 초과·채널 32 초과는 `EncodingError`. 디코더가 만든 NaN/Infinity 는 0 으로 바꾸고 경고(작업자 결정, 규격에 없음). 표시 없이 짧게 디코딩되는 잘림(MP3, ADTS AAC, Ogg Vorbis/Opus, Ogg FLAC)이 있고 `truncated` 표시를 시험한 것은 FLAC 네이티브뿐.
- **검증(JDK 6u45)**: 작업자 `media` 전체 681 통과/0 실패/14 건너뜀(207초), `media.webaudio` 187, `media.audio.wide` 13, 감사 시험 3종, 소스 점검 0 위반. 16비트 일치(2채널 이하 시험 자료 전부, 손실 코덱 포함, 비트 단위), 다채널 자료 14개 vs ffmpeg 채널별 참조(정확한 형식 최대 차이 0, 손실 최대 0~1 LSB), 정수 정밀도(FLAC 4~32비트 포함 비트 단위), 손실 코덱 float 정밀도 rms 개선(MP3 20~42배, AAC 260~420배, Vorbis 137배, **Opus 1.03~1.08배**: 이 구현의 Opus 가 libopus 와 약 0.3 LSB 다르며 원인 미규명), 변이 시험 7건. 메모리: 230초 24비트 스테레오 WAV(입력 58 MB, 출력 77 MB)를 `-Xmx170m` 에서 디코딩, 140/150 MB 는 출력 버퍼 할당에서 OOM(한 번의 측정). 조정자 전체 회귀 결과는 아래 로그 갱신 줄.
- **조정자 회귀와 불안정한 시험 두 건**: rebase 한 스냅샷의 전체 회귀가 두 번 모두 100% 통과하지는 않았다: 1차 4113 통과/2 실패, 2차 4114 통과/1 실패. 실패는 `core.TextTrackIntegrationTest.testDefaultTrackIsFetchedAndItsCuesFire`(두 번 모두)와 `testing.selftest.BrowserJarsTest.testPageScriptsRunFromTheJars`(1차만). **B2 와 무관하다고 판단해 병합했다**: 두 시험 모두 단독 실행은 통과(각 3회, 부하 6개 프로세스를 걸고 8회씩 A2/B2 스냅샷 모두 0 실패), `core.` 전체를 한가한 상태에서 돌리면 A2/B2 모두 422/0, 부하를 걸고 돌릴 때만 B2 스냅샷에서도 TextTrack 시험이 실패. TextTrack 시험은 8 kHz 6000 프레임(0.75초) 클립을 재생하며 cue 1(0.1~0.25초)의 enter/exit 를 기다리는데, 재생 위치 갱신이 0.25초 이상 밀리면 규격의 time marches on("뒤처지지 않았다면 놓친 cue 를 발생")에 따라 놓친 cue 를 건너뛰므로 부하에 민감한 시험이다(제품의 규격 준수 동작, 결함 아님으로 판단). `BrowserJarsTest` 는 `dispose()` 가 던진 예외의 원인이 출력되지 않아 이번에 시험이 원인을 풀어서 던지도록 고쳤다(다음에 실패하면 원인이 보인다). 이 두 시험과 앞서 기록한 `ConsoleExample --smoke`(1초 창), 캔버스 커닝(JRE 6 JVM 종료, 완화함)이 **병렬 작업자가 PC 를 쓰는 동안 회귀가 불안정해지는 시험들**이다. 회귀 판정은 한가한 상태에서 단독 재실행으로 확인한다.
- **확인하지 못한 것**: 시험 자료 없는 채널 배치(Vorbis/Opus 4ch·5.0·6.1, AAC 구성 11/12, 2채널 초과 ADPCM, Opus 매핑 군 2/3/255), 트랙 여럿인 MP4/WebM 의 첫 트랙, 64 MB 한도 근처 입력, Chromium 의 동작(부분 결과·채널 순서·길이 `ceil`), JRE 6u45 이외의 JRE.

### 2026-11-22 · P25 A2 나머지 노드와 HRTF 병합 (WEBAUDIO 작업자, 커밋 `20261122-110`~`117`)
- **구현(`media.webaudio`, `.dsp`)**: `WaveShaperNode`(곡선 보간, 2x/4x 과표본화: Kaiser 반대역 필터, 지연 63/74.5 프레임), `ConvolverNode`(지연 없는 분할 FFT 합성곱, 임펄스 응답 1/2/4채널과 "true stereo", 규격 `normalize` 식), `DynamicsCompressorNode`(Chromium 설계를 옮김, BSD 고지 유지), `AnalyserNode`(Blackman 창, 시간 평활, dB/바이트 변환), `PannerNode`+`AudioListener`(equalpower, HRTF, 거리 모델 3종, 원뿔; 기하는 `Spatial`), `MediaElementAudioSourceNode`+`MediaElementPcmSource`(순수 Java 쪽, 교차 오리진 무음, `<video>` 거부; `<audio>` 어댑터는 C2). 새 팩터리와 `getListener()`/`isHrtfAvailable()`. 상세 `docs/progress/webaudio.md` 6절.
- **HRTF**: MIT KEMAR compact 자료(368 방향, `tools/webfetch` 로 `https://sound.media.mit.edu/resources/KEMAR/compact.zip` 231,461 바이트를 2026-11-22 에 받음)를 새 도구 `tools/hrtfgen`(Java 6)이 귀마다 최소 위상 커널 + 지연(150~1200 Hz 최소 제곱 적합)으로 변환해 자원 `media/webaudio/dsp/kemar-compact.hrtf`(381,183 바이트, **제품 JAR 의 첫 비자바 리소스**)를 만든다. 런타임은 쌍선형 보간 + 8점 라그랑주 분수 지연, 44.1 kHz 외 표본율은 윈도 sinc 변환, 이동 시 한 quantum 교차 페이드, 자료를 못 읽으면 equalpower. 이용 조건(저자 표기)은 README 에 반영. 원본 `compact.zip` 은 시험의 독립 기준으로 `tests/resources/kemar/` 에 둠. 원본 대비: 크기 최대 0.098 dB(200 Hz~10 kHz), 좌우 시간차 최대 0.93 표본(평균 0.30).
- **검증(JDK 6u45)**: 작업자 `media/webaudio` 213건(A1 121 + 새 92) 통과(`src` 를 클래스 경로에 더해서; `scripts/pkgtest.sh` 는 비자바 리소스를 복사하지 않아 HRTF 시험 15건이 실패했으므로 **조정자가 `pkgtest.sh` 가 `src/**`·`engine/src/**` 의 비자바 파일을 복사하도록 고침**), 노드별 손 변이 시험 7건 모두 잡힘, 속도(오프라인 44.1 kHz 스테레오, 공유 PC): 이동하는 HRTF 패너 20개 1.6배속, 컴프레서 20개 3.0배속, 4x 웨이브셰이퍼 20개 3.8배속, 컨볼버 13~30배속. 조정자 전체 회귀 **4097 통과/0 실패/14 건너뜀**.
- **규격/참조와의 차이**: 원뿔 이득은 소스→리스너 벡터 기준(Chromium 과 같음), 애널라이저는 모노 다운믹스 전에 노드 자신의 채널 설정 적용, 컴프레서 출력 채널 수는 입력과 같고 첫 신호에서 약 50 ms 이득이 내려감(참조와 같음), 스테레오 입력은 HRTF 에서 모노 평균으로 공간화(Blink 는 채널마다 자기 귀 커널).
- **확인하지 못한 것**: 실제 소리 출력과 HRTF/컴프레서/컨볼버 청취, 다른 엔진의 출력과 비교, 실제 `<audio>` 원천, JRE 6u45 이외의 JRE.

### 2026-11-22 · P25 B AudioBuffer·decodeAudioData 순수 Java 쪽 병합 (WEBAUDIO-B 작업자, 커밋 `20261122-140`~`147`)
- **구현**: `AudioBuffer` 복사 규칙을 규격 1.4.3(`max(0, min(N_b-k, N_f))`, `bufferOffset` 이 길이 이상이어도 오류 아님)으로 바로잡음(A1 의 `EngineStateTest` 어서션 1개 수정), `AudioBuffer.wrap`. `media.webaudio.decode`: `Resampler`(Kaiser 창 sinc 다상, 설계 정지 대역 100 dB·130 탭, 정확한 위상 표/1024행 보간 표, 길이 `ceil(N·out/in)`, 입력 분할과 무관하게 비트 단위 같은 결과), `AudioBufferDecoder`(P18 `AudioDecoders.open` 시그니처 판별 → float → 채널별 리샘플 → `AudioBuffer`, 모든 실패는 원인이 붙은 `EncodingError`), `AudioDecodeService`(데몬 스레드 풀, `Executor` 콜백, 취소·닫기·한도 64요청/256 MB·콜백 예외 격리). 상세 `docs/progress/webaudio-b.md`.
- **품질(시험이 출력한 실측)**: 통과 대역 이득 오차 최대 9.3e-5 dB, 95% 나이퀴스트에서 -6.02 dB, 정지 대역 가장 약한 것 -100.8 dB, 올릴 때 잔차 -104.7 dB, 사인 재현 오차 5.1e-6(진폭 0.5), 보간 표 대 정확한 위상 표 8.3e-7. ffmpeg(libsoxr VHQ)와 길이·시간 정렬이 같고 정확도가 같은 수준(참고 비교, 합격 기준은 해석식). 모노 60초 분량을 0.5~1.3초에 변환.
- **디코딩 검증**: P18 시험 자료 121개가 아홉 계열 모두 같은 경로로 디코딩, 무손실은 ffmpeg 참조 PCM 과 비트 단위 일치, 손상 사본 1089개는 703 디코딩/386 `EncodingError`/그 밖의 예외 0.
- **검증(JDK 6u45)**: 작업자 `media/webaudio` 182건(A1 121 + 새 61) 통과, 조정자 전체 회귀(rebase 한 스냅샷) **4005 통과/0 실패/14 건너뜀**.
- **한계와 결정**: (1) P18 디코더가 3채널 이상을 스테레오로 내리고 24비트 이상·float 를 16비트로 반올림 → **B2 승인**(원래 채널·float 선택 경로를 `media/audio/**` 에 추가, 기존 16비트 경로 출력은 비트 단위 불변). (2) 잘린 파일은 처음에 엄격(`EncodingError`)이었으나 **관대 정책으로 변경 결정**(디코딩한 프레임이 하나라도 있으면 그만큼 성공, 실제 브라우저·1.1 WD 비규범 주석에 맞춤). (3) 실제 64 MB 입력/2^25 표본 출력, 트랙이 여럿인 MP4/WebM, Chromium 의 길이 식(`ceil` 인지)은 확인하지 못함. (4) P25-05 는 `decodeAudioData` 의 Promise/콜백/ArrayBuffer 분리/문서 큐 전달(C 단계)이 남아 미완료.

### 2026-11-22 · P25 A1 Web Audio 그래프 엔진 병합 (WEBAUDIO 작업자, 커밋 `20261122-100`~`109`)
- **구현**: `media.webaudio`(+`.dsp`) 순수 Java. render quantum 128, 제어 메시지 모델, `connect/disconnect` 전 형태, 규격 4.4/4.5 채널 믹스(12쌍 행렬), 순환 규칙(Tarjan: DelayNode 든 순환만 허용, 아니면 무음), 꼬리 시간/수명, `OfflineAudioContext`, AudioParam 자동화 전부, 노드 11종(Destination, BufferSource, Oscillator+PeriodicWave, ConstantSource, Gain, Delay, Biquad, IIR, StereoPanner, ChannelSplitter/Merger)과 기본 `AudioBuffer`. 판본·결정·한계는 `docs/progress/webaudio.md`(기능 표 8.4절 WAU-01~09, 예외 EXC-14 로 옮김).
- **검증(JDK 6u45)**: 작업자: `media/webaudio` 시험 121건 통과(기대값은 닫힌 식/독립 계산, 변이 시험 3건), 속도 약 6배속 이상(공유 PC, 참고값). 조정자: 새 구조로 rebase 한 상태의 스냅샷에서 전체 회귀 **3943 통과/1 실패/14 건너뜀**(엔진 648 + 브라우저 3295). 1건은 `CanvasTextStyleTest.testKerning`(아래). 새 시험은 120건이라 전체 3823 → 3943.
- **`CanvasTextStyleTest.testKerning` 불안정의 원인(새로 확인)**: A1 과 무관. 단독 실행을 반복하면 가끔 **JRE 6u45 의 `fontmanager.dll` 접근 위반으로 JVM 이 죽는다**(`hs_err`: `sun.font.SunLayoutEngine.nativeLayout` ← `Font.layoutGlyphVector` ← `js.host.canvas.CanvasText.layout` ← `Context2D.measureText`, 5회 중 2회). 전체 회귀에서는 같은 시험이 단언 실패(`with a font that has kerning pairs normal kerning ...`)로 나타나기도 했다(P11-02 때 기록한 "간헐 실패"와 같은 시험). 코드는 이미 히브리어를 `layoutGlyphVector` 에 주지 않는 방어를 두었는데(`CanvasText` 22행), 커닝 경로(`TextAttribute.KERNING_ON` + `layoutGlyphVector`)에도 같은 종류의 JRE 6 결함이 있어 보인다. **제품 영향 가능성**: `ctx.fontKerning = 'normal'` + `measureText/fillText` 가 JRE 6 에서 호스트 JVM 을 죽일 수 있다. **조사와 조치(같은 날)**: 순수 Java 프로그램(제품 코드 없음)으로 재현했다. `new Font("SansSerif"|"Dialog",...)` 로 `layoutGlyphVector` 한 **뒤에** `new Font("Tahoma",...)` 로 `layoutGlyphVector` 하면 50~60회 중 약 2~5회 JVM 이 죽는다(커닝 속성이 없어도 재현, Tahoma 를 먼저 하거나 Arial 이면 0/60). 커닝을 켠 Tahoma 의 너비도 가끔 엉뚱했다(핵심 시험이 보인 "좁아짐" 대신 약 30% 넓어짐). 그래서 **JRE 6 에서는 `fontKerning = "normal"` 요청을 무시**하도록 했다(`CanvasText.KERNING_SUPPORTED`, 시험은 JRE 6 에서 "요청해도 너비가 같음"을 확인하고 더 새 Java 에서는 기존 엄격 검사). 변경 후 `CanvasTextStyleTest` 단독 25회 모두 통과, JVM 종료 0. **남은 위험**: 복합 문자(`isComplex`, 아랍·태국·인도 등)의 `layoutGlyphVector` 와 SVG 텍스트의 `TextLayout` 도 같은 배치 엔진을 쓰므로 같은 조건(논리 글꼴 배치 뒤의 Tahoma)에서는 JRE 6 에서 같은 위험이 있다(실제 페이지에서 드물 것으로 보이나 확인하지 않음). 재현 프로그램은 저장소에 넣지 않았다(스크래치).

### 2026-11-22 · P26 JavaScript 엔진 분리와 Java Scripting API 지원 (완료, 엔진 작업자, 브랜치 `worktree-agent-a8e4159237316313b`)
- **분리(P26-01)**: 엔진 핵심(`js` 루트, `ast parser interp builtins date num regexp text`, 새 `embed script`)을 `engine/` 으로 옮겼다(파일 275개, git 이 rename 으로 인식). 패키지 이름은 유지. 경계는 네 겹으로 강제한다: 엔진은 빈 클래스패스/소스패스로 컴파일, 브라우저는 컴파일된 엔진 클래스만 참조, `tools/sourcecheck` 새 규칙 `engine-boundary`, 클래스 상수 풀 시험과 headless 엔진 단계 시험. 작업자가 임시로 `java.awt` 를 쓰는 클래스를 넣어 각 장치가 실제로 실패함을 확인했다.
- **독립 API(P26-02/03)**: `js.embed.JsEngine`(eval/compile/모듈/전역/호출/제한·취소/콘솔 훅/모듈 로더), 살아 있는 핸들, `JsHostFunction`(스크립트가 Java 를 부르는 유일한 통로), `JsScriptError`(위치). Java 객체는 스크립트로 새지 않는다. 독립 모드 전역은 ES2015 내장 + `console`/`print`, 타이머는 켜지 않기로 결정. 엔진 쪽 보강: `JsRealm.parseScript`/`ParsedScript`(한 번 파싱해 여러 번 실행), 던진 위치 기록, 처리되지 않은 promise 거부 보고(속도 영향은 잡음 안).
- **`javax.script`(P26-04/05)**: 고유 이름(`swingwebpanel-js`)만 등록, 일반 이름은 `-Dswingwebpanel.js.genericNames=true`. ENGINE 범위별 realm, `Compilable`/`Invocable`, 컨텍스트 Writer 콘솔. `tools/sourcecheck` 의 `javax.script` 금지는 제공자·예제 패키지만 예외로 바꿨다. ARCHITECTURE 1.1 에 반영.
- **JAR(P26-06)**: `swingwebpanel.jar`(합친 단일, 기존 사용 그대로), `swingwebpanel-js.jar`(엔진), `swingwebpanel-browser.jar`(브라우저; 엔진 JAR 필요). 합친 JAR 의 최종 이름은 P23 에서 정한다.
- **검증(P26-07)**: 작업자 3820/0/14 보고 → 조정자 재실행 **3823 통과/0 실패/14 건너뜀**(엔진 648, 브라우저 3175; 작업자 보고 뒤 시험 3건이 늘었다). JRE 8(Nashorn)에서 `js.script` 20건 통과, 일반 이름이 이 엔진을 돌려주지 않음.
- **한계/미확인**: JRE 8 로 엔진 시험 전체를 돌리면 `DateTest.testRoundTrips` 1건 실패(1900년 이전 Asia/Seoul 시각의 JRE 6/8 시간대 데이터 차이로 보이며 JRE 6 에서는 통과). `ConsoleExample --smoke` 는 CPU 가 바쁠 때 1초 창 안에 이벤트가 오지 않아 `consoleEvents=0` 이 간헐적으로 나왔다(부하 의존으로 추정, 분리 전과 비교하지 않음). 최상위 let/const/class 는 `JsEngine.get` 으로 읽을 수 없음. 엔진 단독 모드를 JRE 7 이상(8 제외)에서는 돌리지 않음.
- **P23 때 할 일**: `engine/` → Maven 모듈 대응표는 `docs/progress/engine.md` D 절(`engine/src`→`src/main/java` 등, 시험 실행기를 test-jar 로 내보냄, 합친 JAR 은 shade/assembly).

### 2026-11-22 · P24 콘솔 출력 이벤트 (완료)
- **설계/계약**: `docs/API_CONTRACT.md` 5.20(통지 범위 표, 스레드·수명 규칙, 한계), 기능 표 API-27. 공개 타입 `ConsoleListener`/`ConsoleEvent`(루트 패키지)와 `SwingWebPanel.addConsoleListener/removeConsoleListener/getConsoleListeners`. 호출은 항상 EDT, 쓴 순서대로, 리스너 예외는 격리, 리스너가 없으면 이벤트를 만들지 않고 **표준 출력에도 쓰지 않는다**(이전에는 페이지 콘솔이 System.out/err 에 썼다: 동작 변경이며 계약과 README 에 적었다).
- **엔진 쪽 경계(P26 을 위해 브라우저 타입 없음)**: `js.ConsoleRecord`(불변: 메서드, 수준, 서식 적용 문자열, 인수 문자열, 스크립트 이름, 줄/열, 그룹 깊이, 종류), `js.ConsoleRecordHook extends ConsoleHook`. 기존 `ConsoleHook.message(level,text)` 는 그대로이며 기록을 모르는 연결은 이전과 같은 값을 받는다. 호출 위치는 `JsRealm.setCallSite`(인터프리터 `Expressions.callNode` 가 기본 함수 호출 직전에 기록, `Ctx.sourceName` 추가)로 안다.
- **브라우저 쪽**: `core.ConsoleDispatcher`(대기 1000개 상한, 100개씩 EDT 전달, 초과분은 세어 `MESSAGES_DROPPED` 알림 하나), `core.PageConsole`(페이지별 연결, 문서 URL 고정, 페이지를 떠나면 닫힘), `js.host.HostConsole`(worker 기록 전달), `PageRuntime` 이 realm 의 콘솔을 `PageConsole` 로 바꿈, `dispose()` 가 `ConsoleDispatcher.close()`. 프레임은 `FRAME`, Worker/SharedWorker 는 `WORKER`/`SHARED_WORKER`(`WorkerParent.console(ConsoleRecord)` 로 시그니처 변경).
- **P24-02 결정**: 모든 console 메서드 통지(`assert` 는 실패할 때만, 수준은 Console Standard), 잡히지 않은 예외(`UNCAUGHT_EXCEPTION`)와 처리되지 않은 promise 거부(`UNHANDLED_REJECTION`, 마이크로태스크 검사점 뒤 보고)는 ERROR 로 알리되 `kind` 로 `console.error` 와 구별, `alert/confirm/prompt` 는 `DIALOG`/INFO. 시험이 드러낸 결함: 파서가 실행한 스크립트 뒤의 검사점이 거부 보고를 빠뜨려 타이머 예외보다 늦게 보고됨 → `PageScripts.execute` 에 `reportUnhandledRejections()` 추가.
- **변경 파일**: `src/.../ConsoleEvent.java`, `ConsoleListener.java`, `SwingWebPanel.java`, `core/{ConsoleDispatcher,PageConsole,BrowserSession,PageRuntime}.java`, `js/{ConsoleRecord,ConsoleRecordHook,JsRealm}.java`, `js/interp/{Ctx,Interpreter,Expressions,ModuleRecord}.java`, `js/builtins/{ConsoleBuiltins,GlobalBuiltins}.java`, `js/host/{HostConsole,PageScripts,WindowGlobals}.java`, `js/worker/{WorkerParent,WorkerHandle,WorkerScope,SharedEntry,SharedHandle}.java`, 시험 `ConsoleEventIntegrationTest`(11), `js/interp/ConsoleRecordTest`(6), 예제 `examples/.../ConsoleExample.java`, 문서(API_CONTRACT, FEATURE_MATRIX, ARCHITECTURE, README, examples/README, AGENTS P24 체크).
- **검증(JDK 6u45)**: `ConsoleEventIntegrationTest` 11/11, `ConsoleRecordTest` 6/6, `ConsoleApiTest` 8/8; **전체 회귀(`BUILD_DIR=build-full ./test.sh`) 3771 통과/0 실패/14 건너뜀**(건너뜀은 선택적 Opus 원본 벡터 12건 + Java Sound 믹서가 필요한 시험 2건, 변경 전과 같음); `tools/sourcecheck` 변경 경로 0 위반; `examples/run.sh ConsoleExample --smoke` → `smoke OK consoleEvents=2 listeners=1`.
- **미확인/한계**: `unhandledrejection`/`rejectionhandled` 이벤트는 구현하지 않음(콘솔 보고만). 줄/열은 기본 함수의 직접 호출 지점(호출식 첫 토큰). 실제 Swing 창의 `ConsoleExample` 을 사람 눈으로 본 확인은 하지 않았다(smoke 는 창을 띄워 이벤트 수만 확인).
- **병렬 작업 번호 대역**(commit 메시지 `20261122-NNN`): 조정자 001~099, WebAudio 작업자 100~199, 엔진 작업자 200~299.

### 2026-11-20 · 계획 추가: P23~P26 (문서만)
- 사용자 요청으로 AGENTS.md "개발 계획" 끝에 추가 목표 네 단계를 넣었다. P23 Maven 전환(JDK 6 대상 빌드·시험, 표준 디렉터리, 부모 POM + 모듈 기본안), P24 콘솔 출력 이벤트(`ConsoleListener`/`ConsoleEvent` 기본안, 엔진 쪽 경계는 기존 `js.ConsoleHook`), P25 Web Audio API(그래프·자동화·노드·디코딩·AudioWorklet·Java Sound 출력), P26 엔진 분리와 `javax.script` 제공자(엔진 핵심 패키지가 브라우저 패키지를 import 하지 않음을 확인해 경계 기본안으로 기록, Rhino/Nashorn 이름 충돌과 `tools/sourcecheck` 의 `javax.script` 금지 예외 처리 포함).
- 변경 파일: `AGENTS.md`(개발 계획 절: P22-16 메모, 공통 사항, P23~P26, 중간 실행 목표 표), `docs/PROGRESS.md`.
- 검증: 문서만 바꿨으므로 빌드/시험은 실행하지 않았다. 상단 세 단락이 바뀌지 않았는지 `git diff` 로 확인했다.
- 다음 작업 번호: P23-01.

### 2026-11-19 · 재개 세션: CSS-L·MEDIA-D 마무리와 병합, P10-08-g, WOFF2 독립 인코더 자료

- 커밋 `20261119-001`~: CSS-L Lucida 파생 자료 제거와 이력 정리·병합(전체 3644 통과/0 실패/14 건너뜀), MEDIA-D 마무리·병합(회귀 3658 íµê³¼/1 ì¤í¨(FontDisplayIntegrationTest.testOptionalFontThatArrivesAtOnceIsUsed ìê° ì´ê³¼, ë¨ë ì¬ì¤í 4/4 íµê³¼)/14 ê±´ëë), P10-08-g, fontTools WOFF2, 문서(AGENTS P10-08-d~h, ARCHITECTURE, FEATURE_MATRIX 13절, cssg 32.10~33, media-matrix 11절).
- 최종 전체 회귀: 3668 íµê³¼/0 ì¤í¨/14 ê±´ëë(JDK 6u45). 자세한 것은 위 "2026-11-19 재개 세션" 절.

### 2026-11-17 · P07-07-b-4: 과거 UA 정적 표시·center 텍스트 정렬·nobr/wbr

- 구현 전에 b-4를 분할했다. HTML5 REC 2014-10-28 §10.3.1/3/4를 기존 `tools/webfetch` 조회 자료로 확인했다(조회일 2026-11-17). center의 정렬을 UA의 동작하지 않던 `-webkit-center`에서 작성자 표현 힌트 `center`로 옮겼다. 속성이 없는 요소에도 적용하고 일반 사용자/작성자 CSS 및 중요 사용자 CSS의 우선순위를 검증했다.
- blink 및 nobr wbr의 UA 규칙을 추가했다. wbr는 자기 계산 스타일을 가진 폭 없는 `InlineItem.Kind.OPPORTUNITY`로 전달하며 필요할 때만 줄을 나눈다. 빈 인라인 가장자리는 기회를 소모하지 않고, wbr 뒤 nowrap 덩어리는 `overflow-wrap:break-word`여도 내부를 나누지 않는다. 내재 너비 및 첫 줄 스타일과도 연결했다.
- `LegacyUaStylesTest` 13건: big/small/tt의 상대 크기·실제 글꼴·em 기하, strike/s/u 장식 화소, 정적 blink, HTML5 basefont 숨김, center의 힌트 출처·상자/픽셀, nobr/wbr의 자체 CSS·빈 줄·원자 상자·내재 너비·첫 줄 스타일·긴 nowrap 덩어리. 실제 Swing 경로 시험 1건을 더했다(JavaScript 비활성·가운데 빈 가장자리·원자 상자의 다음 줄·수신 원문 보존).
- 최초 8건은 수정 전 1통과/7실패로 문제를 재현했고, 추가 빈 가장자리 및 긴 nowrap 사례도 각각 실패 후 수정했다. 최종 집중 시험은 **15/0/0**(새 UA 13건 + 기존·신규 Swing Legacy 2건), **전체 `test.bat`: 2520 통과/0 실패/12 건너뜀**(296510 ms). JDK 6u45로 제품·시험 전체 컴파일 및 제품 JAR 생성/내용 검사, 실패 스위트 종료 코드 1 확인까지 통과했다. 건너뜀은 선택적 Opus 원본 벡터 두 판본의 파일 부재 12건으로 이유를 기록했다. 경계 수정 전에 중단한 첫 전체 실행은 합산하지 않았다.
- 전체 sourcecheck: **1849개 파일/0 위반**, `git diff --check` 통과. 상세 명령·변경 파일·남은 범위는 `docs/progress/legacy.md`, 구조 변경은 ARCHITECTURE §16, 기능 근거는 FEATURE_MATRIX LEG-01에 기록했다. **P07-07-b-4만 체크**하고 다음 P07-07-b-5의 입력·기대 결과·구현 위치·완료 조건을 추가했다. 상위 P07-07-b 및 P07은 미완료다.

### 2026-11-17 · P07-07-b-3: 과거 font의 색·크기·글꼴 힌트 수정

- 첫 미완료 P07-07-b를 읽고 구현 전에 b-3을 분할했다. 프로젝트의 `tools/webfetch`로 HTML5 REC 2014 §2.4.6/§10.3.4를 조회했다(조회용 JRE 25, 제품 시험은 JDK 6u45).
- `HtmlPresentationalHints`의 짧은 색 성분에서 발생하던 예외를 고쳤다. ASCII 공백·빈 원문과 공백만 있는 값·CSS3 색 이름·128자 제한을 구분한다. font 크기는 긴 선행 0·포화 숫자와 부호 바로 뒤 숫자를 해석하고, face는 단일 CSS 값으로 파싱·직렬화해 닫히지 않은 주석이 size를 삼키지 않게 한다.
- 수정 전 HEAD 전체 스냅샷(`resume1117`): **2496 통과/0 실패/13 건너뜀**(440833 ms). 13건의 이유는 선택적 Opus 원본 벡터 12건과 JAR 미빌드 1건이다. 새 `LegacyFontHintsTest` 8건은 수정 전 8건 실패를 확인했다. 수정 후 **8건 통과**, 관련 CSS/레이아웃/렌더링 전체 **558건 통과**(새 시험 포함), 실제 `SwingWebPanel.paint`의 JavaScript 비활성·48×24 상자·색·원문 보존 **1건 통과**. 수정 후 전체 회귀를 재실행한 것으로 기록하지 않는다.
- 소스 규칙 점검: **1848개 파일 / 0 위반**, `git diff --check` 통과. 변경 파일·재현 가능한 명령·남은 범위는 `docs/progress/legacy.md`에 기록하고 FEATURE_MATRIX LEG-01을 부분 구현으로 갱신했다. **P07-07-b-3만 체크**하며 P07-07-b/전체 P07은 미완료다.
- 기존 FRAMES/WORKER/CSS-G 브랜치와 worktree를 확인·보존했다. 미커밋 코드를 병합하거나 새 작업자를 시작하지 않았다. 다음은 P07-07-b의 나머지 LEG-01 UA 표시 규칙이다.

### 2026-11-17 · 두 번째 세션(조정자): OPUS 병합, API 표면 점검과 구현, 작업자 3명 시작

- **회귀와 병합**: master 전체 회귀 2412/0/1. OPUS rebase 후 2470/0/13 → fast-forward(`cc7ace4`). `docs/progress/media-opus.md` 에 커밋 502~504 의 내용(Ogg Opus, WebM `A_OPUS`, RFC 8251 수정, 견고성)을 조정자가 정리해 적었다. FEATURE_MATRIX AUD-08/09 와 README 오디오 문구 갱신, P18-07·P15-08-c·P15-08-c-2 체크(커밋 062).
- **API 표면 점검(P20-10, P20-12)**: `docs/progress/api-surface.md`. 구현 목록은 위 "현재 상태" 참조. 시험: `ElementStatesTest`(areas/dialog/track/object/rowSpan/insertAdjacent/replaceChildren/relList), `MediaQueriesTest`(matchMedia, resize/scroll 순서), `ScrollApiTest`, `WebSocketTest`, `NodeApiTest`, `PageRuntimeIntegrationTest`(elementFromPoint, 실제 JScrollPane 스크롤과 resize). 구현 중 발견·수정: `document.replaceChildren` 이 단일 노드를 지우던 오류, `matchMedia` 이벤트의 분류기 등록(`MediaQueryListEvent` 는 `dom` 패키지에 둬야 `DomInterfaces` 분류에 걸림).
- **발견(별도 처리 필요)**: `window.resize`/`scroll` 이벤트가 그동안 발생하지 않았고(이번에 구현), `scrollX/scrollY` 는 항상 0, `scrollTo` 는 아무 일도 하지 않았다(이번에 구현). 아직 발생하지 않는 이벤트: `pageshow`/`pagehide`/`beforeunload`/`unload`, window `focus`/`blur`/`online`/`offline`, `visibilitychange`, 포인터 이벤트.
- **한도 도달 직전 추가(커밋 065)**: `requestIdleCallback/cancelIdleCallback`, `window.origin/isSecureContext/status/defaultStatus/reportError`, `screen.avail*/orientation`, `document.createAttributeNS`, `PageTransitionEvent` 와 `pageshow`(load 뒤, persisted 항상 false)를 구현했다. 시험: `WindowBasicsTest` 5건과 `ElementStatesTest` 포함 `js.host` 224/`js.dom` 35 통과(JDK 6u45). `pageshow` 변경은 전체 스냅샷이 아니라 변경 클래스만 따로 컴파일해 `WindowBasicsTest` 로 확인했다 → **다음 세션 첫 일: 전체 회귀**(마지막 전체 회귀 2485/0/13 은 커밋 063 직전 상태).
- **작업자 3명(FRAMES, WORKER, CSS-G)은 한도 도달 시점에 백그라운드에서 돌고 있었다**: 각자 worktree 에 작은 단위로 commit 하고 있다(FRAMES 601~604, WORKER 701~706, CSS-G 801~803 확인). 새 세션은 `git worktree list` 와 각 worktree 의 `git log master..` 를 먼저 본다. 병합 전 `git rebase master` + 전체 회귀가 필요하다.
- **남은 조정자 후보**: IMG-09(AVIF 인터 프레임)와 P18-03, EventSource, IntersectionObserver/ResizeObserver, 페이지 수명 이벤트(pagehide/unload/beforeunload/focus/blur), CSSOM·LEGACY·TEXTSEL 작업자(브리프 `docs/progress/briefs/`), P22-14/15/16 마무리 문서.
- **도구 사용 메모**: 전체 컴파일된 스냅샷에서 시험만 다시 돌리려면 `sh build/rt.sh <스냅샷이름> <필터 접두사>`(`build/` 는 git 무시이므로 필요하면 다시 만든다: `java -cp build/snap-N/build/pkg-N/classes;…/test-classes org.duckdns.hjow.swingwebpanel.testing.TestRunner --scan …/test-classes --filter X`). CPU 가 4코어라 작업자가 돌고 있으면 전체 회귀가 5분에서 17분까지 걸린다.

### 2026-11-17 · 사용자 요청으로 중단, 인수인계 작성

- 사용자가 작업을 잠시 멈추라고 요청했다. 실행 중이던 작업자 OPUS/FRAMES/WORKER 를 중단했고(각자의 worktree 와 브랜치는 보존, FRAMES 와 WORKER 는 미커밋 작업이 남음), 이 문서의 "다음 세션 인수인계" 절에 상태·열린 항목·재개 절차를 적었다. 중단 직전의 조정자 작업: `a`/`area` 의 URL 유틸리티(커밋 060, `HtmlInterfacesTest` 포함 `js.dom` 35건 통과). 요소 상태 멤버(`areas`, 대화상자, track, object)의 초안 코드는 시험 전이라 저장소에 넣지 않았다.

### 2026-11-17 · LAYOUT-F 병합과 P10 체크, 표 객체 모델·document 멤버(P20-01 일부)

- **LAYOUT-F**(`26d9a42`~`7efc849`, 401~405) fast-forward 병합. 병합 후 전체 회귀(스냅샷 `lfmerge`, 작업 트리 포함): **2405 통과 / 실패 0 / 건너뜀 1**(제품 JAR 시험은 스냅샷에서 건너뜀, 실제 JAR 시험은 별도 통과). 구현: `::first-line`(P10-04-f), 2D `transform`·개별 `translate/rotate/scale`(P10-06, 3D 는 EXC-03), `transition`/`animation`/`@keyframes`/완화(P10-07, `AnimationEngine`), `@font-face`(TrueType/WOFF1/`local()`/`unicode-range`, P10-05), `clip-path` 기본 도형. **P10-04-f, P10-04, P10-05, P10-06, P10-07 체크.** P10-08 은 미구현 속성(`filter`, `mix-blend-mode`, `mask`, `shape-outside`, 논리 속성, `break-*`, `@layer`/`@container`, `::selection`/`::placeholder` 등, 목록은 `docs/progress/layout-f.md`)이 남아 체크하지 않았다. FEATURE_MATRIX CSS-13/24/25/26/27/28/35/42 와 ARCHITECTURE(웹 글꼴·애니메이션 소유)를 갱신했다. 작업자의 요청 처리: `StaticStateAuditTest` 의 `FontResolver#WEB` 항목을 확인했다. `PageResourcesTest.testEntryLimitAndClose` 의 드문 경쟁(close 가 finished 를 설정하고 중단된 작업자의 finished++ 가 단정문 앞에 실행됨)은 시험 쪽 문제로 남아 있다(원인 분석 완료, 수정 전). `TransitionEvent`/`AnimationEvent` 의 JS 속성 노출은 후속(js 소유).
- **P20-01 일부(조정자)**: `dom.html.TableOps` + `js.dom.TableBindings`: `HTMLTableElement` 의 `caption/tHead/tFoot`(설정자 포함)/`tBodies/rows`, `createCaption/deleteCaption/createTHead/deleteTHead/createTFoot/deleteTFoot/createTBody/insertRow/deleteRow`, 구역의 `rows/insertRow/deleteRow`, 행의 `rowIndex/sectionRowIndex/cells/insertCell/deleteCell`, 셀의 `cellIndex` 를 규격의 인덱스 규칙과 `IndexSizeError`/`HierarchyRequestError` 로 구현(`TableApiTest` 6건). document 의 `anchors`, `plugins`, `getElementsByName`(라이브), `lastModified`, `designMode`(끔 고정), `dir`, `execCommand`/`queryCommand*`(지원 없음)을 더했고 `document.links` 를 규격대로 href 가 있는 `a`+`area` 로 바로잡았다(`HtmlInterfacesTest.testDocumentMembers`). `js.dom` 시험 34건 통과. P20-01 은 `elementFromPoint`, `HTMLMapElement.areas`, `dialog.show/close`, `HTMLTrackElement`, `HTMLObjectElement` 의 상태, URL 유틸리티, `rowSpan` 기본값과 항목 단위 점검표가 남아 열려 있다.

### 2026-11-17 · P20-01 일부: 반영 속성만 있는 HTML 요소 인터페이스 28개(조정자)

- `js/dom/HtmlMoreInterfaces`(신규, `DomInterfaces` 에 두 줄 연결): `HTMLDListElement` `HTMLDataElement` `HTMLTimeElement` `HTMLModElement`(ins/del) `HTMLDetailsElement` `HTMLDialogElement` `HTMLMapElement` `HTMLAreaElement` `HTMLParamElement` `HTMLEmbedElement` `HTMLObjectElement` `HTMLTrackElement` `HTMLMenuElement` `HTMLMenuItemElement` `HTMLKeygenElement` `HTMLPictureElement` `HTMLTableCaptionElement` `HTMLTableColElement` `HTMLTableSectionElement` 과 과거 요소 `HTMLFontElement` `HTMLBaseFontElement` `HTMLDirectoryElement` `HTMLAppletElement` `HTMLMarqueeElement`. 이전에는 이 태그들이 `HTMLUnknownElement` 였다(감사 `docs/progress/htmlifaces-audit.md`). 반영 속성(문자열, URL, 불리언, 정수, 부호 없는 정수)과 새 반영 형식 `uint1`(0 보다 큰 수, 대체값 1: `col.span`, `td.colSpan` 의 기본값이 규격대로 1)을 `MemberInstaller` 에 더했다. `HtmlInterfacesTest` 3건, `js` 전체 810건 통과(JRE 6), sourcecheck 0 위반.
- **P20-01 은 열려 있다**: 메서드와 상태가 필요한 부분(`HTMLTableElement` 의 `caption/tHead/tFoot/tBodies/rows/createCaption/createTHead/insertRow/deleteRow`, 표 구역·행의 `rows/cells/insertRow/deleteRow`, `HTMLMapElement.areas`, `HTMLDialogElement.show/showModal/close/returnValue`, `HTMLTrackElement.readyState`/`track`, `HTMLObjectElement` 의 `contentDocument/form/validity`, `HTMLAreaElement`/`HTMLAnchorElement` 의 URL 유틸리티), `frame`/`frameset`(P20-02), document 의 `anchors` `plugins` `lastModified` `designMode` `dir` `getElementsByName` `elementFromPoint` `execCommand` `queryCommandSupported` 가 남았다.

### 2026-11-17 · P07-09, P09-09 근거 대조(조정자)

- **P07-09**: 고정 기대 DOM 트리 자료 `tests/resources/html/tree/*.dat` 7개 파일에 **256개 사례**(basics 40, body 39, document 44, foreign 29, fragments 32, special 37, tables 35)가 있고 `TreeConstructionTest` 가 읽는다. 속성 값 속 `>`(`<p title="a>b" data-x='1>2'>`), script 안의 비교식(`if (a<b && c>d)`), 잘못된 테이블, 문자 참조(`&amp;&lt;&notit;&#65;&#x42;`, `&copy &hearts;`), 조각 파싱, DOCTYPE/문서 모드, 외부 콘텐츠(SVG/MathML)가 포함되고 토큰화 시험(`HtmlTokenizerTest`)과 파서 시험(`HtmlParserTest`)이 따로 있다. 요소 동작 구현 상태는 FEATURE_MATRIX 의 HTML/HEI 표에서 따로 추적한다. **P07-09 체크**(P07 상위 항목은 P07-07-b/c 때문에 열려 있다).
- **P09-09**: 배치 좌표/크기 시험 256건(`layout/*Test`: 블록, 인라인 글자 배치, 부유, 위치 지정, 표, flex, grid, 다단, 세로쓰기, 생성 콘텐츠)과 그리기 시험 121건(`render/*Test`: 그림 순서, 클리핑, 쌓임 맥락, 배경/경계선/그림자, 이미지·컨트롤), 컴포넌트 수준 화소 시험(`IntegratedPageIntegrationTest` 외)이 상자·글자 위치·클리핑 기대값을 확인한다. 전체 화면 픽셀 비교만으로 합격을 정하지 않았다. **P09-09 체크**(P09 상위 항목은 P09-08-b-2 의 실제 창 확인 때문에 열려 있다).

### 2026-11-17 · P21-07, P21-08 점검(조정자)

- **P21-07**: `ResourceCountsIntegrationTest`(JRE 6, 실제 서버): 하위 리소스·아직 실행 중인 느린 요청·폴링이 있는 페이지로 컴포넌트를 15번 만들고 읽고(3번에 1번 clean) 폐기한 뒤 서버의 **열린 연결 0**(`TestHttpServer.getOpenConnections()` 추가), 제품 스레드(`swingwebpanel-`)가 기준 +2 이내로 돌아오고, 폐기 뒤 요청이 더 오지 않는다. 메모리 참조는 `ComponentLeakIntegrationTest`(약한 참조로 폐기/clean 한 컴포넌트 수거, finalizer 없음), 오디오 라인은 `AudioIntegrationTest`(이동·clean·dispose 가 출력을 닫음)가 근거이며 GC 시점은 보장하지 않는다.
- **P21-08**: 정리 뒤 상태를 `docs/API_CONTRACT.md` 5.13/5.14 와 대조했다: clean 은 빈 페이지·이력 1개·원문 빈 문자열·캐시와 sessionStorage 와 공용 localStorage 비움(`CacheIntegrationTest`, `StorageComponentIntegrationTest`, `LifecycleTest`)이고 User-Agent·추가 헤더·쿠키·JavaScript 설정·리스너를 유지(`PublicApiContractIntegrationTest`, `LifecycleTest.testCleanResetsPageAndKeepsSettings`)하며, dispose 는 반복 안전·변경 메서드 ISE·조회는 빈 상태(`SwingWebPanelTest.testDisposedComponentBehavior`, `LifecycleAuditIntegrationTest`)이다. **P21-07, P21-08 체크.** P21-03(프레임 정리)과 P21 상위 항목은 P20-02(FRAMES) 뒤에 확인한다.

### 2026-11-17 · P22-06 주소·타입·오류·다운로드 통합 시험(조정자)

- `ProtocolMatrixIntegrationTest` 7건(JRE 6, 보호 메서드 `configureSsl`/`createDownloadPopup`/`getDownloadDirectory` 를 재정의한 하위 클래스, 실제 HTTP/HTTPS/FTP 시험 서버, 두 번 연속 통과): http 의 네 콘텐츠 타입(원문 그대로, 제목으로 해석 확인, 대소문자·매개변수가 있는 타입), 서버의 404/500(서버 본문이 원문, 상태 `Error 404`/`Error 500`)과 거부된 연결과 `mailto:` 차단의 오류 화면, `about:blank`/`about:text`, 클래스패스 페이지와 없는 리소스의 404, 익명 ftp 목록(디렉터리 링크, 하위의 `../`, 실패는 404), https(재정의가 호출되고 시험 인증서를 신뢰)와 http 만 쓰는 포트로의 https(한 번의 http 재시도로 실제 http 주소 표시), WebGL/WebGL2/실험적 WebGL/WebGPU 컨텍스트 `null` 과 `navigator.gpu`·`WebGLRenderingContext` 미노출과 2D 동작, 한글 파일 이름 첨부와 `application/zip` 과 `<a download>`(페이지와 이력은 그대로, 팝업이 만들어지고 `COMPLETED` 통지, 파일이 안전한 이름으로 저장됨). 이미지/오디오 실패와 alt, 비디오 안내는 `MediaFallbackIntegrationTest`. **P22-06 체크.** 실제 Swing 팝업 조작은 사람의 확인이 필요하다(P11-10, P22-10).

### 2026-11-17 · P18-06, P18-09 컴포넌트 수준 확인(조정자)

- `MediaFallbackIntegrationTest`(JRE 6, 실제 컴포넌트·서버): 404 로 실패하는 `img`, 소스 없이 `alt` 만 있는 `img`, 404 인 `audio`(controls, alt), `video` 를 한 페이지에 두고 화면 밖 그리기로 확인했다 — 네 자리가 모두 비어 있지 않고(실패한 이미지/오디오는 미지원 URL 과 alt, 소스 없는 이미지는 alt, 비디오는 "지원하지 않는 비디오입니다 (Video is not supported)" 안내), `error` 이벤트(이미지 1, 오디오 코드 4)가 페이지에 닿고, `video.canPlayType()` 은 모든 형식에 빈 문자열, `video.play()` 는 명시적인 답(거부 또는 오류)이며 재생되지 않는다. 표시 목록 단계의 근거는 `ImagePaintTest`(실패 자리, 비디오 안내, 오디오 막대). 안내와 alt 는 텍스트로만 그려 HTML 로 실행하지 않는다. **P18-06, P18-09 체크.**
- 관찰: 이미지 오류가 아주 빨리 끝나면 페이지 뒤쪽의 스크립트가 핸들러를 붙이기 전에 `error` 가 먼저 보고될 수 있다(요소의 `onerror` 속성이 안전하다. 브라우저에서도 같은 경쟁이 있다). P18-08(컨트롤 막대 조작/재생 상태)과 P18-10, P18-11 은 남아 있다.

### 2026-11-17 · P22-09 한도 초과 점검(조정자, 체크하지 않음)

- `RobustnessIntegrationTest` 5건(JRE 6, 실제 컴포넌트와 서버, 사례마다 EDT 가 5초 안에 답하는지 계속 확인): 2만 단계 중첩 DOM(스택 오버플로 없이 로드·배치·그리기, 원문 유지), 문단 5만 개와 CSS 규칙 1만 개(전체 배치 높이 10만 px 초과, 화면 응답), 리디렉션 순환(4회 안에 끊고 오류 화면이 순환을 밝힘)과 끝없는 연쇄(한도에서 종료), 끝나지 않는 스크립트(`Script step limit exceeded`/`Script time limit exceeded`로 중단, 다음 이동 정상)와 메모리를 채우는 스크립트(중단), 20 MiB 응답(메모리 한도로 거부되어 오류 화면, OOM 없음), 20초 지연 응답 중 새 이동(새 이동이 이기고 늦은 페이지가 되돌아오지 않음). 모두 통과.
- 다른 근거: 모듈 순환(`ModuleTest`, `ModuleScriptHttpTest`), DOM 순환 삽입 거부(P06), 손상 이미지/오디오(디코더별 손상 입력 시험), 연속 이동과 지연 응답(`BrowserSessionTest`, `PendingNavigationIntegrationTest`). **프레임 순환/깊이 제한은 P20-02(FRAMES 작업자) 뒤에 확인해야 하므로 P22-09 는 체크하지 않았다.**

### 2026-11-17 · P22-07 JRE 6 실제 실행 기록(조정자, 체크하지 않음)

- 제품 JAR(`build/swingwebpanel.jar`)로 실제 네트워크를 조회했다(헤드리스, 스크래치 프로그램은 저장하지 않음).
  - **JRE 6u45**: `https://example.com/` → TLS 실패(JRE 6 은 TLS 1.0 만, SNI 없음) 뒤 규정대로 **http 재시도**로 `http://example.com/` 를 표시(`Done`, 제목 `Example Domain`, 원문 577자). `http://example.com/` 도 같은 결과. `https://www.wikipedia.org/` → https 실패, http 재시도가 사이트의 https 강제 리디렉션을 만나 다시 실패하여 **`Redirect loop` 오류 화면**(`Error 0`, 진행률 100)으로 끝났다. 요즘 HTTPS 강제 사이트는 JRE 6 에서 열 수 없다.
  - **JRE 8u202**(같은 JAR): `https://example.com/` 는 https 그대로 표시, `https://www.wikipedia.org/` 도 https 로 표시(원문 120,328자, 제목 `Wikipedia`). 즉 최소 대상 JRE 6 과 더 새 JRE 의 차이는 TLS 지원 범위(1.2/SNI)뿐이며 제품 코드는 같다.
  - 오디오: JRE 6 의 Java Sound 가 이 PC 에서 **믹서 0개**를 돌려주므로 `<audio>` 재생은 `playing` 뒤 `error`(코드 3)로 끝난다. 장치 없음 → `error` 이벤트라는 계약대로이며 예외나 정지는 없다. 실제 오디오 장치로 청취한 확인은 하지 못했다.
- 전체 시험은 JRE 6 에서 실행했다(최근 전체 회귀 2330 통과). 그러나 실제 오디오 장치 재생 확인이 없으므로 **P22-07 은 체크하지 않았다**(필요 환경: 오디오 장치가 있는 Windows 와 청취).

### 2026-11-17 · P22-08, P22-11~P22-13 점검(조정자)

- **P22-08**: `sh tools/sourcecheck/run.sh`(기본 경로 전체: src, tests, examples, tools, 문서) **0 위반 / 1727 파일**, `--selftest` OK. 검사 항목은 인코딩, `com.sun`/`sun`/Rhino/`javax.script`/외부 프로세스/JNI/Swing HTML 렌더러, 바이너리·네이티브 파일, Java 6 이후 구문, 영어·한국어 Javadoc, 반복문의 한국어 주석, 기본 인코딩 의존이다. API 수준의 Java 6 호환은 JDK 6u45 컴파일(`-source 1.6 -target 1.6`, 실제 JDK 6 의 rt.jar 기준)과 JRE 6 시험 실행으로 확인했다. 도구 네 개(entitygen, sourcecheck, vogen, webfetch)에 `AGENTS.md`/`README.md` 가 있다.
- **P22-13**: `./build.sh` 로 `build/swingwebpanel.jar`(3.6 MB, javac 1.6.0_45)를 만들어 `JarContentTest`(제품 파일만 포함)가 통과(이전에는 JAR 이 없어 건너뜀)하고, 예제를 JAR 클래스패스로만 실행했다: `java -cp build/swingwebpanel.jar;build/examples.jar org.duckdns.hjow.swingwebpanel.examples.StaticPageExample --smoke` → `smoke OK page=classpath://org/duckdns/hjow/swingwebpanel/examples/pages/index.html renderedHeight=942`(JAR 안의 HTML 과 상대 리소스), `BlankPageExample --smoke` → OK. 외부 라이브러리 없음.
- **P22-11/12**: README 에 CORS/소켓 연결/프록시 한계를 더했고(최소 Java 버전, 빌드/실행, JPanel/JScrollPane, 메서드, 비동기 이동, 상태, SSL 재정의, 다운로드 팝업과 대체 방법, 저장소와 clean, dispose, 비디오/3D/WebGL/WebGPU 미지원은 이미 있었다), 예제 세 개는 `dispose()` 를 보여 주고 소스 점검이 Javadoc 을 확인한다. 영어 README 는 없다.

### 2026-11-17 · P15-12, P22-05 통합 페이지 시험(조정자)

- `IntegratedPageIntegrationTest`(JRE 6, 실제 컴포넌트와 `TestHttpServer`): 외부 CSS/스크립트(`const`, 화살표 함수, Promise, 타이머), 이미지, 오디오 요소, 취소되는 링크와 이동하는 링크, GET 폼, 쿠키를 가진 한 페이지. 확인한 것: 하위 리소스 요청과 User-Agent·쿠키·Referer, `getContent()` 가 수신 원문 그대로, 스크립트 순서(`sync,micro,timer`), 실제 AWT 클릭 세 번이 바꾸는 글자·class·저장소와 그려진 화소(흰색→파랑→초록→파랑), 이미지 화소, 선호 높이(스크롤 범위), preventDefault 링크는 이동·요청 없음, 링크 이동·뒤로(원문 복원)·앞으로·`refresh(true)` 재요청·이력 불변, 폼 GET 이동, JavaScript 끄면 스크립트 정지하고 링크/폼/이미지는 동작, 폐기 뒤 요청 없음. **P15-12, P22-05 체크**(스크롤은 선호 높이와 Scrollable 정책으로 확인했고 사용자 휠 입력은 아니다. P15 상위 항목은 P15-04-b-2 때문에 열려 있다).

### 2026-11-17 · P22-01~P22-04 공개 API 점검(조정자)

- `PublicApiContractIntegrationTest` 8건(공개 메서드만, 실제 컴포넌트와 `TestHttpServer`, JRE 6): 초기값과 잘못된 인수, User-Agent 가 다음 요청부터 서버에 닿음, `setPage` 의 비동기 반환, 스크립트가 문서를 바꿔도 `getContent()` 는 수신 원문, JavaScript 끔/켬(다음 읽기부터), `setContent` 네 MIME(원문 그대로, `about:text`, 이력 복원), `back(n)/forward(n)` 경계(0·음수·부족 시 무동작)와 분기 이력과 복사본, 리디렉션의 이력 항목 하나, `refresh()` 의 캐시 사용과 `refresh(true)` 의 재요청과 이력 불변, 상태/진행률, 하이퍼링크 리스너 등록·제거 반복, clean 이 User-Agent/헤더를 유지, dispose 반복과 폐기 뒤 동작, `addHeaders` 덮어쓰기·UA 제외·CR/LF 거부·복사본, 쿠키 문자열, 저장소 복사본과 컴포넌트 간 공유/분리와 clean. 반복 실행 3회 모두 통과. **P22-01~P22-04 체크**(하이퍼링크 이벤트 통지 자체는 `SwingWebPanelTest`/`PageInputTest`, localStorage 손상 복구는 P16 시험이 근거).
- 관찰한 한계: 리디렉션을 거쳐 받은 페이지는 최종 URL 로 캐시에 저장되지 않아(요청 URL 키) 최종 URL 의 `refresh()` 가 서버에 다시 묻는다. 계약 위반은 아니지만 비효율이다. 후보: `ResourceFetcher` 가 최종 URL 로도 저장.

### 2026-11-17 · P21-05, P21-06 점검(조정자)

- `StaticStateAuditTest`(정적 상태를 가질 수 있는 필드 전부를 검토 목록과 대조, 새 항목이나 낡은 항목은 실패)와 `ThreadCreationAuditTest`(스레드를 만드는 소스 전부가 `setDaemon(true)` 와 `swingwebpanel-` 이름을 가짐)를 추가했고 JRE 6 에서 통과했다. 여기에 `ComponentLeakIntegrationTest`(폐기·clean 한 컴포넌트가 외부 루트 없이 수거됨, 스레드 비누적), `LifecycleAuditIntegrationTest`, P21-01 소유 표를 근거로 **P21-05, P21-06 체크**. `finalize()` 재정의는 제품 코드에 없다. 수거 시점은 보장하지 않는다(P21-07 의 근거 기록 방침 그대로). P21-07(소켓/오디오 라인 수 추적)과 P21-08(계약 대조)은 남았다.

### 2026-11-17 · 사용 한도 도달 직전: P21 점검(P21-02, P21-04 체크), 작업자 3명 중단

- `LifecycleAuditIntegrationTest` 4건(clean 중 늦은 응답/타이머/요청이 화면·원문·저장소를 되살리지 못함, 폴링이 clean·교체·dispose 뒤 멈춤, 3스레드 폭풍 뒤 일관 상태) JRE 6 통과 → **P21-02, P21-04 체크**. `StaticStateAuditTest`(정적 상태 보유 필드 검토 목록, 신규·낡은 항목은 실패)로 P21-05 의 정적 필드 점검을 수행했으나 스레드/타이머 점검과 localStorage 참조 검사를 마치지 못해 **P21-05 는 체크하지 않았다**. 점검 중 `FormBindingsCollections.FILES` 의 약한 키 맵 값이 키/realm 을 붙잡아 페이지가 새는 문제를 약한 값으로 고쳤다.
- 사용 한도 때문에 작업자 OPUS(`worktree-agent-a94b1fd99420a979a`), FRAMES(`worktree-agent-a43282243e908721a`), LAYOUT-F(`worktree-agent-a279d9b13b9f1a150`)를 중단했다. 각 worktree 브랜치에 커밋된 작업만 남아 있으며 미커밋 변경은 해당 worktree 에 그대로 있다(`.claude/worktrees/`). 다음 세션에서 각 브랜치의 `docs/progress/*.md` 와 `git log master..<브랜치>` 로 상태를 확인하고 이어서 맡기거나 병합한다. 마지막 병합 상태 전체 회귀 2330 통과(커밋 `51da8ed` 시점).

### 2026-11-17 · CANVAS-A·DOMAPI-B 병합, P20-08-c 소켓 HTTP 연결과 CORS 사전 요청(P20-08 체크)

- **CANVAS-A**(`5344452`~`96443bf`, 201~208) 병합: Canvas 2D(P20-05). 병합한 HEAD 전체 회귀 **2223 통과 / 실패 0 / 건너뜀 2**(스냅샷 `canvasmerge`). P20-05 를 `P20-05-a`(본문의 요구 전부, 체크)와 `P20-05-b`(OffscreenCanvas·ImageBitmap·filter·텍스트 확장 속성·`crossorigin` 이미지의 `Origin`, 미완료)로 나누어 a 만 체크했다. 실제 Swing 창 확인은 P22-10.
- **DOMAPI-B**(`3599d2e`~`be01ecd`, 301~306) 병합(fast-forward): 구조화 복제, history 상태(P20-07), Range/Selection/NodeIterator/TreeWalker(P20-03), DOMParser/XMLSerializer(P20-04), 메시징(`P20-09-a`). **P20-04, P20-07 체크.** P20-03 은 화면 좌표가 요소 단위 근사이고 화면 선택은 P11-08 이라 체크하지 않았다. P20-09 는 `P20-09-a`(체크)와 `P20-09-b`(Worker 등)로 나누었다. 작업자의 보고가 반복 전달되어 `TaskStop` 으로 끝냈다(작업은 모두 병합됨).
- **P20-08-c(조정자, 체크)**: JRE 의 `HttpURLConnection` 은 `Origin`/`Access-Control-Request-*` 같은 제한 요청 헤더를 조용히 버리고 `PATCH` 를 거부한다(1.6.0_45 에서 확인). 그래서 `net` 에 소켓 기반 HTTP/1.1 클라이언트를 만들었다.
  - `SocketHttpClient`: `Socket`/`SSLSocket` 위의 요청 하나(Connection: close). 모든 헤더·메서드 토큰 허용, CR/LF 주입 거부, 응답 머리 크기 제한, content-length/chunked/연결 종료 경계, HEAD/204/304 는 본문 없음, 짧은 본문은 `EOFException`, 어느 스레드에서나 `disconnect()`. TLS 는 일반 소켓 위에 `createSocket(plain, host, port, true)` 로 씌운다(SNI 는 JRE 가 지원할 때 보냄).
  - `SocketHttpURLConnection`/`SocketHttpsURLConnection`: 위를 `HttpURLConnection`/`HttpsURLConnection` API 로 감싼다. `HttpFetcher` 의 쿠키·리디렉션·압축·제한·취소와 **SSL 확장 지점(`configureSsl(HttpsURLConnection)`)** 이 그대로 동작한다(훅이 `setSSLSocketFactory`/`setHostnameVerifier` 를 호출하면 연결 시점에 적용). 인증서 사슬은 JRE 가 기본 신뢰 저장소로 확인하고 호스트 이름은 `HostnameMatcher`(RFC 2818: subjectAltName 우선, CN 대체, 와일드카드는 가장 왼쪽 한 레이블)가 확인한다. 연결의 검증기가 받아들이면 통과하지만 JVM 의 기본 검증기는 무시하여 JVM 전역 상태를 쓰지 않는다. 이름 불일치는 `SSLPeerUnverifiedException`(TLS 실패이므로 http 재시도 대상).
  - `HttpFetcher`: `Origin` 을 싣는 요청(CORS 목적의 교차 오리진 요청, 시작한 오리진이 있는 POST/PUT/DELETE)은 JVM 프록시(`ProxySelector`)가 적용되지 않는 한 소켓 연결로 보낸다. 안전 목록에 없는 메서드나 **페이지가 쓴 헤더**(`ResourceRequest.setCorsAuthorHeaders`; 컴포넌트의 추가 헤더는 사전 요청을 일으키지 않는다)가 있는 교차 오리진 요청은 먼저 `OPTIONS` 사전 요청(쿠키 없음, `Origin`, `Access-Control-Request-Method`, 정렬한 소문자 `Access-Control-Request-Headers`, Referer)을 보내고 `OriginPolicy.checkPreflight`(2xx, `Access-Control-Allow-Origin/Credentials` 가 본 요청과 같은 규칙, 메서드/헤더 허용 목록, `*` 는 자격 증명이 없을 때만 모든 것을, 단 `Authorization` 은 이름을 적어야 함)로 허락을 확인한다. 허락은 `Access-Control-Max-Age`(기본 5초, 최대 600초, 최대 256개)만큼 오리진·URL·자격 증명별로 다시 쓰고, 리디렉션 구간마다 다시 확인한다. 거부는 `FailureKind.CORS` 이며 본 요청은 보내지 않는다. 사전 요청의 연결 실패는 해당 종류(예: CONNECT)이고 https 실패는 http 재시도 대상이다.
  - `core.PageWebRequests`: "사전 요청 미지원" 오류를 없애고 페이지의 작성자 헤더를 요청에 실어 보낸다.
  - 검증(JDK 6u45, 실제 소켓): `SocketHttpClientTest` 19건(제한 헤더, 임의 메서드, POST 본문 길이, 응답 헤더, chunked/닫힐 때까지/길이 본문, HEAD, 오류 본문, 잘림, 헤더 주입 거부, 취소, 연결 실패/읽기 시간 초과, https 신뢰/불신/이름 불일치/연결 검증기, 와일드카드 규칙, 시험 인증서, `HttpFetcher` 의 POST Origin·SSL 훅·TLS 실패 시 http 재시도), `CorsPreflightTest` 9건(허용 PUT, 페이지 헤더 공지와 거부, 거부 4종, 자격 증명, 와일드카드와 Authorization, max-age 재사용, 사전 요청이 필요 없는 요청, 리디렉션 구간, 취소와 서버 다운), `WebRequestApiTest` 15건(페이지의 fetch/XHR 가 JSON POST·사용자 지정 헤더·PUT·DELETE 를 사전 요청 뒤 보내고 거부 서버는 `TypeError`), `HttpFetcherTest.testRefererOriginAndCacheHeaders` 의 건너뜀 해제. `net` 전체 137건 통과(`SocketHttpClientTest` 20건: gzip·chunked 응답과 POST 리디렉션 시험 추가). 병합한 코드 전체(CANVAS-A + DOMAPI-B + P20-08-c) 회귀(스냅샷 `p2008c`): **2330 통과 / 실패 0 / 건너뜀 1**(제품 JAR 미빌드). `tools/sourcecheck` 0 위반.
  - 한계: JVM 프록시가 적용되면 JRE 연결을 써서 `Origin` 이 사라진다(소켓 클라이언트는 프록시를 쓰지 못함). 이미지·스타일시트 요청은 `Origin` 을 싣지 않으므로 `Origin` 이 있어야 `Access-Control-Allow-Origin` 을 주는 서버의 `crossorigin` 이미지는 캔버스를 오염시킨다(P20-05-b). 불투명 오리진(`about:blank`/`setContent`)의 요청은 `Origin: null` 을 싣지만 응답이 `null` 이나 `*` 를 허용해도 기존 규칙(`corsAllows`)이 거부한다.

### 2026-11-17 · FORMS-B·LAYOUT-E 병합, 폼 제출 연결(P11-05/06/07, P15-10 체크), P10-03 체크

- **FORMS-B**(커밋 `09d1cfd`~`599dd83`, master 직접): 폼 컨트롤의 고유 크기·기준선·UA 스타일·그리기(체크박스/라디오/버튼/select 드롭다운·리스트박스/파일/진행/미터/슬라이더/색/fieldset+legend), 텍스트 필드·textarea 의 Swing 편집기(포커스 때 컨트롤 위에 나타나 텍스트/선택/`input`/`change`/조합 이벤트를 양방향 동기화),
  Enter 제출, 검증 말풍선, 파일 선택(`JFileChooser` 훅). 새 protected 확장점 `SwingWebPanel.submitForm`, `chooseUploadFiles`. 보고의 시험(50건)은 HEAD 전체에서 통과했다. 한계 13개는 `docs/progress/forms-b.md`와 FEATURE_MATRIX HTML-10(편집기는 항상 맨 위, keydown `preventDefault` 로 삽입을 막지 못함, 날짜/숫자 UI 없음 등). **실제 키보드·한글 IME·파일/색 대화상자·Tab 이동·클립보드는 확인하지 않았다.**
- **조정자 연결**(`9c632f8`, 035): `BrowserSession implements FormSubmitSink`(`submit` → `submitForm`). 시험 `core/FormSubmitWiringTest` 3건: 스크립트의 `requestSubmit()` 이 폼 호스트→세션을 거쳐 POST, **JavaScript 를 끈 채 제출 단추를 AWT 로 클릭하면 폼이 GET 으로 이동**, 인라인 핸들러는 JS 를 끄면 실행되지 않고 다시 켜도 멈춘 페이지는 되살아나지 않으며 다음 새로고침부터 실행된다.
  이로써 **P11-05, P11-06, P11-07, P15-10 체크**(자동 검증 기준. P11-02(IME), P11-08, P11-10 은 실제 입력 확인이 필요해 열려 있다).
- 시험 도구 결함 수정(`e7f67ea`, 036): `PageFixture.ManualScheduler` 의 `PriorityQueue` 가 플레이어/가져오기 스레드의 `post` 와 시험 스레드의 `advance` 로 경쟁해 `MediaElementsTest.testPagesWithoutServices` 가 드물게 NPE(FORMS-B 의 clean-HEAD 실행에서 1회). 잠금을 넣었고 `js.host` 113건을 두 번 연속 통과했다.
- **LAYOUT-E 병합**(worktree 브랜치 → master fast-forward, 커밋 `f763c54`(101) `be78a04`(102) `8a094b9`(103)): 보고를 master(FORMS-B 포함) 위로 rebase(충돌은 `render/DisplayListBuilder.border` 한 곳: 둥근 border 와 fieldset legend 갈래를 합침; 둥근 모서리와 legend 가 함께일 때는 둥근 고리를 우선)한 뒤 **전체 2165건 통과/실패 0/건너뜀 2**로 재검증했다.
  P10-03-e(`text-orientation: mixed/upright/sideways`, `text-combine-upright`, 표 생성 도구 `tools/vogen`, 한글 음절 줄바꿈 기회 추가로 가로 한국어 줄바꿈도 바뀜), P10-04-a~e(둥근 모서리, 그림자, 다중 배경, 그라디언트, 카운터/`::before/::after/::first-letter/::marker`) 구현·검증. **P10-03-e, P10-03 체크.** P10-04 는 하위 `P10-04-f`(`::first-line`, 이미지 늦게 도착 시 컴포넌트 수준 확인)가 남아 상위를 체크하지 않았다.
  FEATURE_MATRIX CSS-11/12/16/23/42 를 갱신했다. 워크트리 운영 메모: 별도 worktree 브랜치는 끝날 때 작업자가 master 위로 rebase 하고, 조정자는 공유 트리가 깨끗할 때(`git diff` 가 비고 CRLF 만 달라 보이는 파일은 `git checkout -- <경로>`)만 병합한다. `.claude/` 를 `.gitignore` 에 추가했다.
- 진행 중: CANVAS-A(Canvas 2D, 브랜치 커밋 번호 201~), DOMAPI-B(structured clone, `history.pushState`, Range/Selection, DOMParser, 브랜치 301~).

### 2026-11-17 · P15-08-c-2(`<a download>`), MEDIA-PLAY 보고 반영, P18-05 체크

- 커밋 `f42f9fb`(026). `PageInput.followLink` 가 `download` 속성이 있는 **같은 오리진** 링크를 `BrowserSession.downloadFromPage(url, name)` 으로 보낸다(다른 오리진은 속성을 무시하고 일반 이동). `NavigationRequest.getDownloadName()`, `DownloadRequiredException.Reason.LINK_DOWNLOAD`,
  `NetworkPageLoader` 가 상태 400 미만의 응답을 형식과 관계없이 다운로드로 판정(오류 응답은 오류 페이지), 속성 값은 서버가 파일 이름을 주지 않았을 때만 제안 이름(`attachment; filename=` 로 합성). 시험 `core/LinkDownloadIntegrationTest` 3건(실제 컴포넌트·HTTP·AWT 클릭).
  P15-08-c-2 는 폼 GET/POST(`FormNavigationIntegrationTest`)와 함께 근거가 갖춰졌으나, 컴포넌트의 실제 `FormHost` 설치(FORMS-B)가 남아 상위 P15-08-c 와 P15-08 은 체크하지 않았다.
- MEDIA-PLAY(오디오 재생) 보고를 **HEAD 스냅샷 전체(JDK 6u45, 2054건 통과/실패 0/건너뜀 2)**로 재검증했다(미디어 시험 43건 포함: `MediaPlayerTest` 17, `MediaElementsTest` 20, `AudioIntegrationTest` 6). 구현: `media/playback/**`(블록 디코딩 `MediaPlayer`, `AudioOutput`/Java Sound), `js/host/MediaElements`·`MediaBindings`,
  `SwingWebPanel.createAudioOutputFactory()`(protected), 미디어 요청 서비스. FEATURE_MATRIX AUD-10/HEI-08/WEB-09/VID-01/VID-02 갱신. **P18-05 체크**(img/picture/source/srcset/sizes 선택, 이벤트·크기, 오디오 `source` 목록 선택; 뷰포트 변화에 따른 srcset 재선택은 없음).
  **P18-08/09 는 체크하지 않았다**: 실제 장치로 소리를 듣지 못했고(가짜 출력으로만 검증), `controls` 막대의 입력/동적 그리기와 비디오의 "미지원" 화면 문구가 레이아웃/그리기 쪽에 남았다. 그 요청(막대 인터페이스, `controls` 없는 audio 의 공간)은 `docs/progress/media-play.md` 의 "조정자 요청"에 있고 `layout/render` 가 비는 시점에 처리한다.
  `docs/progress/media-play.md` 의 알려진 한계: `playbackRate` 는 1.0 만, JavaScript 를 끈 페이지의 미디어 동작 없음, 리소스를 통째로 받음(16 MiB), 요소당 데몬 스레드 하나, 트랙 미지원.

### 2026-11-17 · P11-06/P19-07 폼 제출의 이동 연결, P19 완료, 작업자 보고 반영(그리드·폼)

- 커밋 `6fe8192`(023). 새 클래스 `core.PostData`(콘텐츠 타입·본문 4 MiB 까지·참조 주소), `core.ResubmitConfirmer`; `PageSnapshot.getPost()/withPost`, `NavigationRequest.getPost()/isDownload()`, `BrowserSession.submitForm(FormSubmission)`(GET 은 쿼리가 붙은 일반 이동, POST 는 http/https 만, 링크와 같은 이동 정책),
  `NetworkPageLoader`(POST 전송·Referer, 이력 이동은 저장 원문으로 복원·리디렉션을 거치면 일반 페이지), `refresh` 는 폼이 만든 페이지에서 `SwingWebPanel.confirmResubmit`(protected, 기본 확인 대화상자·헤드리스는 아니오)에 물은 뒤 같은 본문으로 다시 보내며 항목을 교체, `DownloadRequest/DownloadTask` 는 POST 응답 저장 때(확인 뒤) 본문을 다시 보낸다.
  정책 전문은 `docs/API_CONTRACT.md` 5.8. 시험 `core/FormNavigationIntegrationTest` 5건(실제 컴포넌트·HTTP: GET 쿼리 인코딩(한글·`&`·`+`), POST 본문/Content-Type/Referer, 이력 이동이 폼을 다시 보내지 않음, 새로고침 확인(아니오/예, 항목 불변), multipart, POST→303 은 일반 페이지, 클래스패스 대상 차단, 첨부 응답 저장 확인).
  JDK 6u45 스냅샷(`build/snap-formnav`) 전체 1736건 통과/실패 0/건너뜀 2(Origin 헤더, 제품 JAR). 폼 호스트는 아직 시험용이다: 컴포넌트의 `FormHost` 설치(`FormSubmitSink` → `BrowserSession.submitForm`)는 FORMS-B 보고 뒤에 연결한다.
- **P19-01/02/07/09/10 체크 → P19 전 항목 완료.** 근거: 이력 항목이 URL/원문/MIME/기준 주소/POST 정보를 보관(`PageSnapshot`), 뒤/앞/새 이동/리디렉션/프래그먼트/폼 전송/오류 페이지의 이력 위치(`HistoryContentIntegrationTest`, `ScriptNavigationTest`, `FormNavigationIntegrationTest`, `NetworkIntegrationTest`),
  POST 정책 명시·구현, 지연/끊긴 응답과 연속 이동(`PendingNavigationIntegrationTest`), 모든 콘텐츠 종류의 뒤/앞/새로고침과 진행률(`HistoryContentIntegrationTest`, `LoadProgressIntegrationTest`). 한계: 폼 제출에는 `Origin` 헤더가 없다(JRE 6 제약, P20-08-c), `target` 은 무시, POST 응답을 저장하려면 폼을 한 번 더 보내야 한다(확인 필요).
- 작업자 보고 반영(모두 모델 출력이므로 HEAD 스냅샷 전체로 재검증): LAYOUT-D(그리드): 전체 1728건 중 실패 2건이 폼 쪽 기대값(아래)뿐이었고 그리드 시험은 모두 통과. P10-03-a~d 하위 항목을 AGENTS.md 에 추가하고 d 까지 체크, **P10-03-e(text-orientation upright/mixed)를 새로 두었으며 상위 P10-03 은 체크하지 않았다.**
  FORMS-A: 조정자 요청 4건 반영(`SelectorTest` `:valid` 기대값, `DomBindingTest` `input.type`, `SelectorMatcher` `:in-range/:out-of-range` 연결(시험 추가), `ElementExtras` 의 option `text` 덮어쓰기 제거) 후 css/js/dom 시험 통과(`141264f`). FEATURE_MATRIX HTML-10/EVT-07/HEI-04 는 "부분 구현"(DOM·검증·직렬화·JS 검증, 컨트롤 그리기·편집·파일 선택기·검증 말풍선은 FORMS-B).
- 병렬 작업자(진행 중): MEDIA-PLAY(오디오 재생·미디어 요소 DOM, 공유 트리), FORMS-B(폼 컨트롤 그리기·편집 오버레이·파일 선택기, 공유 트리, `layout/render` 소유), LAYOUT-E(P10-03-e, P10-04; **별도 worktree 브랜치**, 커밋 번호 101~ 사용, 끝나면 master 위로 rebase 후 조정자가 병합).

### 2026-11-17 · P19-05 캐시 계층 문서·시험, P21 수명 시험(누수·스레드)

- 커밋 `96385fa`(017), `ae4925f`(014). `docs/ARCHITECTURE.md` 마지막 절에 캐시 계층 표(수신 응답/디코딩 리소스/배치/글꼴: 범위·용량·제거 정책)와 확정 뒤 진행률 구조를 기록했다. 시험: `PageResourcesTest` +2건(활동 카운터는 실패도 끝난 것으로 세고 같은 주소는 한 번만 시작, 256 항목 한도 초과는 곧바로 실패하고 세지 않으며 `close()` 가 로딩 중이던 것을 끝난 것으로 셈),
  `PageReleaseIntegrationTest`(컴포넌트를 살려 둔 채 페이지만 교체해도 이전 페이지의 표현 상태와 DOM 이 약한 참조에서 지워짐 — 이력에는 이전 항목이 남아 있음). **P19-05 체크.**
- P21 근거 시험 `ComponentLeakIntegrationTest`(실제 컴포넌트 9개를 스크립트(interval/timeout/Promise/리스너/XHR)·`@import` CSS·이미지·두 저장소와 함께 쓰고 `clean()`+`dispose()` → 모두 수거됨, 놀고 있는 스레드가 끝난 뒤 라이브러리 스레드 6개 이하; `clean()` 만 한 컴포넌트도 수거됨). 결함은 발견되지 않았다.
  P21-01(소유 목록 표)·P21-03/04(Mouse/Key/Focus 리스너 제거, 반복 호출)·P21-07(소켓, 오디오 라인)은 남아 있어 P21 항목은 체크하지 않았다.
- 검토 메모(폼 작업자 보고 뒤 확인): `js/dom/FormBindingsCollections.FILES` 가 `WeakHashMap<FileEntry, JsObject>` 인데 값 `JsObject` 가 키를 강하게 가리키면 키가 수거되지 않는다(파일 선택이 있는 페이지만 해당).

### 2026-11-17 · P16-10 저장소 최종 검증(P16 완료)

- 새 시험 `StorageComponentIntegrationTest` 2건(실제 컴포넌트 3개·HTTP 서버 2개·스크립트): sessionStorage 는 컴포넌트에 남고 두 번째 이동에도 유지, localStorage 는 같은 오리진에서 공유되고 다른 오리진(포트)과 분리, 변경한 문서에는 `storage` 이벤트가 오지 않고 같은 오리진의 다른 문서가 정확히 한 번 받으며 다른 오리진의 변경은 이벤트가 없음,
  `getLocalStorage()` 가 모든 오리진을 나열, `clean()` 이 공유 localStorage 를 모든 컴포넌트에서 비우고 다른 컴포넌트의 sessionStorage 는 유지, 스크립트의 쿼터 초과는 `QuotaExceededError` 로 들어가지 않는 항목에서 던져지고 앞서 저장한 항목은 유지(JDK 6u45 통과).
  P16-10 의 나머지(파일 외부 삭제·손상·동시 쓰기·용량·다른 프로세스 파일 분리·수거 가능성)는 기존 `LocalStorageManagerTest`/`StorageIntegrationTest`/`SessionStorageTest` 가 이미 확인한다. 이로써 P16-01~10 이 모두 체크되어 P16 완료. 실제 컴포넌트의 GC 수거 확인은 P21-07 에서 한다.
- 참고: 시험 서버의 URL 호스트는 `localhost` 이다(오리진 비교 시 `127.0.0.1` 이 아님).

### 2026-11-17 · P19-08(확정 뒤 진행률 통합), P19-09/10 검증 시험, 이전 페이지의 늦은 보고 결함 수정

- 커밋 `355c98d`, `585a6c0`(009, 010), 시험만 `c8cb8db`(P19-10), `293f110`(P19-09). 번호 `005`/`008` 은 작업자 커밋과 겹쳤다(내 `005` 는 `006` 으로 고쳤다. 작업자 쪽 중복은 역사를 고치지 않았다).
- 구조: `PageResources` 가 시작/끝난 가져오기 수를 센다(`activity()`, 실패도 끝난 것으로 셈). `PageView.State` 가 배치할 때마다 `BrowserSession.pageLoadProgress(snapshot, finished, started, scriptsDone)` 로 보고하고,
  세션은 확정 때(`commit`) 스타일시트/이미지/스크립트가 남을 수 있는 문서(뷰가 붙은 `HtmlPageDocument`)라면 "Done" 대신 `Loading resources (0/0)` 50 에서 시작해 `Loading resources (n/m)` 50~94 로 가고,
  리소스와 스크립트가 모두 끝나면 `Done` 100 이 된다(오류 페이지는 확정 때 `Error N` 100). 스크립트 쪽은 `HostEnvironment.hasPendingResources()/pageLoaded()` 를 통해 window `load` 가 시작한 스타일시트·이미지(성공이든 실패든 끝날 때까지)를 기다리게 했고
  (`PageScripts.maybeFinishLoad`), 리소스가 끝나면 `PageScripts.resourcesChanged()` 가 문서 큐에 확인 작업을 올린다. JavaScript 를 끄거나 페이지가 닫히면 스크립트를 기다리지 않는다(`PageRuntime.isLoadComplete`).
- **발견·수정한 결함**: 이전 페이지의 늦은 로딩 보고(스타일시트/이미지 완료)가 이미 시작된 새 이동의 `Loading <url>`/0 을 `Done`/100 으로 덮어써 `NetworkIntegrationTest` 2건이 실패했다(전체 회귀에서 발견). 새 이동이 진행 중(`nav.currentId() != confirmedRequestId`)이면 보고를 무시하도록 고쳤고, 시험(`testResourcesOfTheOldPageDoNotTouchAPendingNavigation`)을 추가했다.
- 시험: `core/LoadProgressIntegrationTest` 6건(느린 스타일시트가 load 와 진행률을 붙잡음·진행률 단조 증가, 느린/실패 이미지, 스크립트 없는 페이지의 배치가 시작한 이미지, 오류·교체 페이지, 이전 페이지 리소스가 진행 중인 이동을 건드리지 않음, 본문 진행률 길이 선언 1~50/미선언 -1),
  `HistoryContentIntegrationTest` 2건(P19-10: setContent 4종·HTTP no-store/404/500·classpath 성공/404·FTP 목록/파일·연결 실패의 뒤로/앞으로/새로고침, 새로고침이 항목을 더하지 않음, 저장 원문 페이지의 새로고침은 네트워크를 쓰지 않음),
  `PendingNavigationIntegrationTest` 5건(P19-09: 늦은 응답/끊긴 응답/새로고침/연속 이동/clean 이 최신 페이지의 URL·원문·이력·상태·진행률을 바꾸지 않음). JDK 6u45 스냅샷(`build/snap-prog3`)에서 js/core/render/root/content/download 전체 1052건 통과/실패 0/건너뜀 1(제품 JAR 미빌드).
- 한계: 첫 배치 뒤에 발견되는 리소스(스크립트가 나중에 만드는 이미지 등)는 이미 `Done` 이 된 페이지의 진행률을 되돌리지 않는다. 프레임(P20-02), 오디오(P18-08)는 아직 진행률/`load` 대기에 포함되지 않는다(P15-04-b-2 의 나머지). CSS 배경 이미지 등 배치가 찾는 리소스는 JavaScript 가 켜진 페이지의 window `load` 를 늦추지 못한다(`load` 가 첫 배치보다 먼저 발생할 수 있음).
  Windows 에서 연결 거부 응답이 회당 약 2.4초 걸려 `HistoryContentIntegrationTest` 가 느리다(환경 지연). P19-08 을 체크했다. P19-01/02/05/07/10 은 폼 전송(POST)·디코딩/레이아웃 캐시가 남아 체크하지 않았다.
- 협업 메모: 작업자가 공유 파일(`HostEnvironment.java`)을 같은 시간에 고쳐 스냅샷이 깨졌다. 내 부분만 시험·커밋하려고 `git show HEAD:경로` 에 내 변경만 적용한 사본을 `snaptest.sh` 의 `<스냅샷경로>=<원본>` 형식으로 넣고, 커밋은 `git update-index --add --cacheinfo` 로 내 사본만 인덱스에 올린 뒤 경로 없이 `git commit` 했다.

### 2026-11-17 · P18-05-b(img 로딩) 검증·커밋, P18-04 체크, FEATURE_MATRIX 미디어 행 갱신

- 커밋 `9930426`(20261117-002). 인수인계 (A)의 미검증 코드를 컴파일·시험했다. 첫 실행에서 `ImageLoadsTest.testNewImageAndDecode` 1건이 실패했다: `PageScripts.fireImageEvent` 가 락·작업 한도·마이크로태스크 체크포인트 없이 `dispatchEvent` 만 불러서
  `load` 핸들러/`decode()` 가 만든 Promise 작업이 실행되지 않았다. 같은 클래스의 `fire(target, event)`(페이지 락, `beginTask/endTask`, `runJobs`, MutationObserver 전달)를 쓰도록 고쳤다.
- 새 통합 시험 `core/ImageLoadIntegrationTest`(실제 컴포넌트·HTTP, 2건): 느린 이미지에서 `DOMContentLoaded`/페이지 확정은 기다리지 않고 window `load` 는 이미지 `load` 이벤트 뒤에 오며, 레이아웃과 이벤트용 로드가 요청 1회를 공유(`server.count == 1`); 없는 파일(404)/이미지가 아닌 바이트는 `error` 를 내고 `load` 를 영원히 붙잡지 않는다.
- 검증(JDK 6u45, 스냅샷 `build/snap-img3`, 소유 파일 + git HEAD): `js.host` 90건 통과/실패 0, 통합 2건, js/core/render/root/content/download 전체 1040건 통과/실패 0/건너뜀 1(제품 JAR 미빌드, 기존 사유). sourcecheck 0 위반.
- 한계: 뷰포트 변경 시 srcset 재선택 없음. 이미지의 `loading=lazy`, `crossorigin`, `referrerpolicy`, `fetchpriority` 미반영. P18-05 상위 항목은 오디오 `source` 선택(P18-05-c)이 남아 체크하지 않았고 P15-04-b-2 는 CSS/프레임 대기가 남아 체크하지 않았다.
- 문서: FEATURE_MATRIX IMG-07(SVG 검증 완료+한계), IMG-09(AVIF 부분 구현: 인트라만), AUD-04/05(MP3, AAC 검증 완료: 시험용 인코더 기준), AUD-09(Vorbis 트랙만), HEI-03(HTMLImageElement 부분)을 작업자 보고대로 갱신했고 AGENTS.md 의 P18-04 를 체크했다
  (근거: `docs/progress/media-svg-avif.md` 의 Chrome 156 비교 사례 151개와 63건 시험). 결정이 필요한 것: AVIF 시험 자료 생성 스크립트가 Python(`tests/resources/media/avif/generation/*.py`)이다 — `tools/` 의 "Java 6 도구" 규칙의 대상은 아니라고 보고 두었으며, 원치 않으면 Java 로 바꾸거나 삭제한다.

### 2026-11-16 · P20-08-a/b · 비동기 웹 요청 API(WEBAPI 작업자 보고 처리)

- 커밋 `903700f`, `bcb4a22`(WEBAPI 작업자). 상세와 규격 조회 기록은 `docs/progress/webapi.md`. XHR/fetch/AbortController/FormData/Blob/File/FileReader/URL/URLSearchParams/TextEncoder/TextDecoder와 서비스 계층 구현, JDK 6u45 `js.host.web` 36건·`WebRequestApiTest` 15건 통과.
- 조정자 처리: `PageScriptsTest.testNavigatorAndGlobals` 의 `XMLHttpRequest` 기대값을 `function` 으로 고쳤고(P11 커밋에 포함), FEATURE_MATRIX DOM-12/WEB-06/WEB-07/WEB-12 와 ARCHITECTURE 를 갱신했다. 전체 스냅샷 1754 통과/실패 1(위 기대값, 수정 후 `PageScriptsTest` 18건 통과)/건너뜀 2(JRE 6 Origin 제한, 제품 JAR 미빌드).
- 미완료: P20-08-c(교차 오리진 사전 요청과 `Origin` 헤더 — 소켓 기반 HTTP 클라이언트 필요)가 남아 P20-08 상위 항목은 체크하지 않았다. `net.OriginPolicy` Javadoc 의 "preflight 는 P20-08 에서" 문구는 "JRE 6 제약으로 P20-08-c" 로 고쳐야 한다(net 소유 파일). `FormData(form)` 이 컨트롤 상태를 읽는 연결은 P11-05 이후.

### 2026-11-16 · P11-01~04(일부) · DOM UI 이벤트, 마우스/키보드/포커스 입력과 :hover/:active/:focus 상태

- 변경 파일: `AGENTS.md`, `dom/{UiEvent,MouseEvent,WheelEvent,KeyboardEvent,FocusEvent,InputEvent,CompositionEvent,FocusManager}.java`(신규), `dom/Document.java`, `js/dom/{DomInterfaces,DomBridge,MemberInstaller,DocumentExtras}.java`, `js/host/ElementExtras.java`, `core/{PageInput,PageKeyboard,KeyNames,PagePresentation,HtmlPageDocument}.java`, `SwingWebPanel.java`, 시험 4종(`UiEventBindingTest`, `DomMouseEventTest`, `KeyboardFocusTest`, `PageScriptsTest.testFocusApi`), 문서.
- DOM: UIEvent/MouseEvent/WheelEvent/KeyboardEvent/FocusEvent/InputEvent/CompositionEvent가 생성자(옵션 사전 포함)·멤버·`document.createEvent`·인터페이스 사슬로 스크립트에 노출된다. 인터페이스 상수(`Event.NONE`, `KeyboardEvent.DOM_KEY_LOCATION_LEFT` 등)가 생성자를 교체한 인터페이스에서 사라지던 기존 결함도 고쳤다(`DomBridge.setConstructor`가 상수를 새 생성자에 복사). `element.click()`은 MouseEvent를 보낸다.
- 마우스: `PageInput`이 AWT 포인터를 신뢰 DOM 이벤트로 바꾼다 — mousedown/mouseup/click/dblclick/auxclick/contextmenu/mousemove, 요소가 바뀔 때 mouseout→mouseleave(안쪽부터)→mouseover→mouseenter(바깥쪽부터), wheel(휠은 호스트 스크롤 창에도 그대로 전달). 좌표는 client(스크롤 반영)/page/offset(맞은 상자의 border 안쪽 기준)/screen, `buttons`는 눌린 버튼 비트마스크. 클릭의 기본 동작(링크)은 `followLink`로 분리해 키보드 Enter도 같은 경로를 쓴다.
- 포커스/키보드: `dom.FocusManager`가 포커스 가능 요소 판정(tabindex, href 링크, 사용 가능한 폼 컨트롤, hidden/disabled/fieldset 제외), Tab 순서(양수 tabindex 먼저), 이벤트 순서(blur→focusout→focus→focusin, relatedTarget)를 담당한다. `PageKeyboard`가 keydown/keypress/keyup(key/code/keyCode/location/repeat/보조 키), Tab/Shift+Tab 이동(마지막 요소 뒤에서는 컴포넌트를 떠나 호스트의 다음 컴포넌트로), 링크/단추의 Enter, 단추의 Space(keyup), 컴포넌트의 포커스 획득/상실을 처리한다. 클릭은 가장 가까운 포커스 가능 요소에 포커스를 주고 일반 영역에서는 거둔다(mousedown 취소 시 이동 없음). `focus()/blur()/document.activeElement/hasFocus()`가 동작한다.
- 스타일 상태: `PagePresentation`이 `SimpleElementState`를 스타일 리졸버에 연결하여 `:hover`, `:active`, `:focus`, `:target`(프래그먼트 이동)이 레이아웃에 반영된다.
- 검증(JDK 6u45, 스냅샷 `build/snap-kbd`): `DomMouseEventTest` 3건·`KeyboardFocusTest` 5건(실제 AWT MouseEvent/KeyEvent/FocusEvent 리스너 호출: 이벤트 순서와 값, Tab 순서·Shift+Tab, 취소, Enter, 클릭 포커스, 컴포넌트 포커스, 레이아웃 상자 크기로 확인한 :hover/:active/:focus)·`UiEventBindingTest` 5건·`PageScriptsTest.testFocusApi` 통과. 이 시험들 외 전체 묶음은 아래 확인 절차로 다시 실행한다.
- 미완료: EVT-04의 입력기 조합 중 키와 배열별 code, EVT-05의 input/change/composition 이벤트 발생(P11-05/P11-08 편집 컨트롤 이후), 창의 focus/blur 이벤트, 키 입력으로 `:focus-visible`/`:focus-within` 구분(`:focus-visible`은 항상 true), 단추 Space의 keydown 활성화 표시. P11-01/P11-02 체크박스는 편집 컨트롤·IME 연결 뒤에 체크한다. P11-03/P11-04는 완료로 체크했다.

### 2026-11-16 · P10-01/P10-02(LAYOUT-C 작업자 결과 반영)와 P11-09 다운로드 팝업의 컴포넌트 연결

- 변경 파일: `AGENTS.md`, `SwingWebPanel.java`, `core/BrowserSession.java`(setStatusMessage), `core/PageInput.java`(scroll 보정 히트 테스트), `DownloadIntegrationTest.java`(신규), `NetworkIntegrationTest.java`, 문서. 레이아웃 구현(`layout/**`, `render/**`)은 LAYOUT-C 작업자가 commit 118·124·131·132에서 했다(`docs/progress/layout-c.md`).
- 레이아웃(작업자 보고, 아래 확인 포함): float/clear/relative/absolute/fixed/쌓임 문맥/자르기/히트 테스트(P10-01)와 표(P10-02)를 구현했고 layout·render 시험 153건을 JRE 6에서 통과했다. 내가 snaptest로 core까지 합친 전체 묶음(js/core/content/dom/html/css/net/storage/download/layout/render/root)을 다시 돌려 1406 통과/실패 0/건너뜀 2를 확인했다. 표의 `visibility:collapse`·퍼센트 height는 미구현으로 기능 표에 남겼다.
- 작업자 요청 반영: `PageInput`이 `page.hitTestDocument(x,y)` 대신 뷰포트 스크롤을 반영한 `hitTest`를 호출하므로 스크롤된 페이지에서도 `position:fixed` 상자가 그려진 곳에서 맞는다.
- 다운로드: `SwingWebPanel`이 `SwingDownloadHandler`를 세션에 설치한다(컴포넌트의 요청 계층·쿠키를 공유). protected 확장 지점 `createDownloadPopup(DownloadRequest, DownloadControl)`(기본: 부모가 컴포넌트의 창인 비모달 `SwingDownloadDialog`)와 `getDownloadDirectory()`(기본 null=사용자의 Downloads)를 추가했다. `clean()`은 진행 중인 다운로드를 취소하고 팝업을 닫으며 `dispose()`는 관리자를 폐기한다. 상태 문구는 진행률을 바꾸지 않는 `BrowserSession.setStatusMessage`로 나간다.
- 검증(JDK 6u45): `DownloadIntegrationTest` 2건(실제 컴포넌트·HTTP: 첨부 파일과 보여 줄 수 없는 형식이 각각 팝업을 열고 호스트가 고른 디렉터리에 올바른 내용(5000바이트 비교)으로 저장되며 페이지/주소/이력/원문/진행률/부분 파일이 그대로, `clean()`이 진행 중인 전송의 팝업을 닫고 아무것도 저장하지 않음)과 기존 `NetworkIntegrationTest`의 핸들러 없는 다운로드 시험(세션 수준으로 옮김) 통과.
- 주의(발견·수정): 컴포넌트에 핸들러가 생기면서 핸들러를 따로 지정하지 않은 시험이 실제 `~/Downloads`에 `zip` 파일을 만든 것을 발견하여 지우고, 해당 시험을 핸들러 없는 세션 수준으로 바꿨다. 이후 다운로드를 일으키는 컴포넌트 시험은 `getDownloadDirectory()`를 재정의해야 한다. 실제 대화상자 조작(저장 위치 선택, 덮어쓰기 확인, 취소)은 수동 확인 대상으로 P11-10에 남긴다. `<a download>` 속성은 P15-08-c-2.

### 2026-11-16 · P15-08-b(완료) · 스크립트 문서 인코딩 상속과 오류/무한 반복/스위치의 컴포넌트 시험

- 변경 파일: `AGENTS.md`, `core/PageSnapshot.java`(encoding), `NetworkPageLoader.java`, `ParsingPageLoader.java`, `PageRuntime.java`, `ContentIntegrationTest.java`, `ScriptRobustnessTest.java`(신규), 문서.
- 외부 일반 스크립트의 디코딩 순서를 BOM → 응답 charset → 포함한 문서의 인코딩 → UTF-8로 맞췄다(HTML 표준의 `charset` 속성 없는 스크립트 규칙). 로더가 수신 바이트를 디코딩한 인코딩 이름을 `PageSnapshot.getEncoding()`에 담고 `ParsingPageLoader`가 파싱 전에 `Document.setCharacterSet`으로 지정하므로, 파싱 중 실행되는 스크립트도 EUC-KR 같은 문서 인코딩을 물려받는다(`document.characterSet`도 정확해짐). 스크립트 요소의 `charset` 속성은 아직 반영하지 않는다.
- 검증(JDK 6u45, 스냅샷 `build/snap-enc`): `ContentIntegrationTest.testExternalScriptsInheritTheDocumentEncoding`(EUC-KR 문서의 charset 없는 스크립트, 응답 charset이 이기는 경우), `ScriptRobustnessTest` 3건(예외는 `window.onerror`로 가고 뒤 스크립트 실행·원문 유지, 무한 반복은 한도로 중단되며 EDT 응답 지연 2초 미만, JavaScript 스위치: 끄면 타이머 정지·HTML 유지, 켜도 재시작 안 함, 다음 로드부터 실행, 꺼진 상태의 로드는 실행 안 함) 통과. 전체 묶음 1228 통과/실패 0/건너뜀 2(+ 로버스트니스 3건 추가 실행 통과).
- P15-08-b의 모든 하위 항목(b-1~3)과 완료 조건이 충족되어 체크했다. P15-04(b-2), P15-08(c-2), P15-10(기본 폼 기능은 P11-06), P15-12는 P15 단계 전체 완료 규칙에 따라 아직 체크하지 않는다.

### 2026-11-16 · P17-01/02/04~08 · 콘텐츠 계층(CONTENT-P17)의 core 연결

- 변경 파일: `AGENTS.md`, `core/NetworkPageLoader.java`, `DefaultPageLoader.java`, `ParsingPageLoader.java`, `BrowserSession.java`, `core/ErrorPages.java`(삭제), `ContentIntegrationTest.java`(신규), 문서. 콘텐츠 계층 자체(`content/**`)는 CONTENT-P17 작업자가 commit 120~125에서 구현했다(`docs/progress/content-p17.md`).
- 응답 분류: `NetworkPageLoader`가 `ContentClassifier`로 처리기를 고른다. 머리만 보고 첨부/다운로드 형식이면 요청을 중단해 다운로드로 넘기고, 타입이 없으면 본문 첫 4096바이트로 판별한다(HTML/텍스트/미디어/바이너리→다운로드). 스냅샷의 MIME은 `getDisplayMime()`(예: `text/css`→`text/plain`). `setContent`의 MIME 검사도 `ContentClassifier.requireSetContentMime`을 쓴다. 이전에 모든 `text/*`를 보이던 것과 달리 Chromium 목록의 일부 `text/*`(csv 등)는 다운로드이다.
- 일반 텍스트는 `PlainTextDocumentBuilder`, `application/xhtml+xml`은 `XhtmlParser`(XML 규칙, 외부 접근 차단)를 쓴다. 파서가 중단되지 않아도 스크립트는 SAX 콜백 안에서 가져오기를 기다리므로 같은 `PageScripts` 경로가 그대로 작동한다. well-formed가 아닌 XHTML은 HTML로 복구하지 않고 오류 위치를 보여 주는 페이지(스크립트 미실행)로 보이며 `getContent()`는 수신 원문을 유지한다. 오류 페이지는 모두 `ErrorPageBuilder`가 만든다(제목/설명은 이전과 같은 문구).
- 검증(JDK 6u45, 스냅샷 `build/snap-p17`): `ContentIntegrationTest` 7건(실제 컴포넌트·HTTP: 텍스트/CSS가 마크업으로 실행되지 않음, XHTML 스크립트가 XML 문서를 봄·원문/제목 유지·`setContent`, 잘못된 XHTML의 오류 페이지·원문 유지·뒤로 가기 복원, 처리기 선택(Markdown/XML→XHTML/타입 없는 HTML·텍스트)과 csv·octet-stream·타입 없는 바이너리의 다운로드 판정, 네 형식과 `setContent`의 이력 복원, 오류 페이지가 이전 페이지 타이머를 멈춤, 오류 페이지 다시 시도 링크의 클릭·리스너 통지·재요청) 포함 js/core/content/dom/html/css/net/storage/root 전체 묶음 1227 통과/실패 0/건너뜀 2(Origin 헤더 제한 1, 제품 JAR 없음 1). sourcecheck 0 위반.
- 알려진 한계: XHTML 본문에 잘못된 바이트가 있으면 로더가 대체 문자로 디코딩해 파서에 넘긴다(XML 파서가 보고하는 만큼만 오류가 된다). `nosniff`/text-or-binary 판별은 없다. `xml-stylesheet`·`xml:base`는 해석하지 않는다. XHTML의 `innerHTML`은 아직 HTML 구문이다(P20-04). 다운로드 팝업의 컴포넌트 연결은 P11-09.

### 2026-11-16 · P15-08-c-1 / P19-02(프래그먼트 부분) · 페이지 시작 이동과 같은 문서 안의 이동

- 변경 파일: `AGENTS.md`, `core/NavigationPolicy.java`(신규), `NavigationType.java`(REPLACE), `NavigationController.java`, `PageSnapshot.java`, `BrowserSession.java`, `PageRuntime.java`, `PageInput.java`, `HtmlPageDocument.java`, `PagePresentation.java`, `SessionListener.java`, `SwingWebPanel.java`, `js/host/PageScripts.java`(fireHashChange), `ScriptNavigationTest.java`(신규), `TestSupport.java`, 문서.
- 스크립트의 `location.assign/replace/href/hash`, `reload`, `history.go/back/forward`가 이전에는 호스트 기본 구현(아무 일도 하지 않음)에 연결되어 있었다. `PageRuntime`이 `NavigationPolicy`(허용 스킴, 네트워크 문서→classpath 차단)를 적용한 뒤 `BrowserSession.navigateFromPage/reloadFromPage/historyGoFromPage`로 전달한다. replace는 `NavigationType.REPLACE`로 이력 항목을 바꾼다. 링크 클릭의 기본 이동도 같은 `navigateFromPage`를 쓴다.
- 같은 문서 안의 이동: 대상이 현재 페이지와 프래그먼트만 다르면 재요청 없이 현재 스냅샷의 URL만 바꾸고(`PageSnapshot.changeFragment`) 이력에 항목을 더한다(replace/동일 URL이면 바꾼다). 이력 항목은 `documentId`로 같은 문서임을 알아 뒤/앞 이동도 재요청 없이 처리한다. 문서 URL(`Document.setUrl`)과 `location.href`는 스크립트 안에서 즉시 바뀌며, window에 `hashchange`(oldURL/newURL)가 가고, 컴포넌트가 JViewport 안이면 앵커(`id`, 없으면 `a[name]`)가 맨 위에 오도록 스크롤한다(빈 프래그먼트/`#top`은 맨 위). 이 이동은 페이지의 스크립트 수명(요청 토큰)을 끊지 않는다(`claimRequestLocked`). 파싱 중(확정 전)의 프래그먼트 이동은 확정 뒤에 적용한다.
- 검증(JDK 6u45, 스냅샷 `build/snap-nav`): `ScriptNavigationTest` 5건(실제 컴포넌트·HTTP: assign/href/location 대입은 항목 추가·replace는 교체·1회 요청, reload와 history.back, 차단 스킴 7종 무시·추가 요청 0, hashchange 시퀀스(`>two;two>;>two;`)와 back/forward 무재요청·`location.href` 즉시 반영, 앵커 링크 클릭이 뷰포트를 앵커로 스크롤하고 뒤로 가기도 무재요청) 통과. js/core/dom/html/css/net/storage/root 전체 묶음 1140 통과/실패 0/건너뜀 2(Origin 헤더 제한 1, 제품 JAR 없음 1).
- 미완료: `:target` 선택자 상태(P11-02), `popstate`/`pushState`(P20-07), 폼 제출·`<a download>`(P15-08-c-2, P11), 프래그먼트 이동 때 포커스 이동, setPage 로 같은 URL#frag 를 호출하면 여전히 전체 재요청(공개 API 계약 유지).

### 2026-11-16 · P15-04-c · 모듈 스크립트의 브라우저 호스트 규칙

- 변경 파일: `AGENTS.md`, `js/ModuleGraph.java`(신규), `js/JsRealm.java`, `js/interp/Interpreter.java`, `js/host/ModuleScriptSource.java`·`ScriptText.java`(신규), `ScriptLoads.java`, `PageScripts.java`, `net/RequestPurpose.java`, `OriginPolicy.java`, `HttpFetcher.java`, `core/PageRuntime.java`, `ModuleScriptTest.java`·`core/ModuleScriptHttpTest.java`(신규), 구조/기능/진행 문서.
- 규격: HTML 모듈 호스트 규칙은 HTML5 2014에 없으므로 WHATWG HTML scripting(조회 2026-11-16, Last-Modified 2026-11-16 10:39:02 GMT, `https://html.spec.whatwg.org/multipage/scripting.html`)을 별도 기준으로 하며 FEATURE_MATRIX ES-HOST-MOD에 기록했다.
- `RequestPurpose.MODULE`: 교차 오리진은 CORS 모드(`OriginPolicy.requiresCors`)이고 `PageRuntime.fetchModule`이 응답의 CORS 허용(`mayUse`), JavaScript MIME 타입(`PageScripts.isJavaScriptMimeType`), 최종 URL을 검사한다. 쿠키는 페이지와 같은 오리진에만 보내고 `crossorigin=use-credentials`일 때 모든 오리진에 보내며, 그때는 서버가 정확한 Origin과 `Access-Control-Allow-Credentials: true`를 줘야 한다(와일드카드 거부). 컴포넌트가 붙이는 추가 헤더 때문에 사전 요청(preflight)이 필요한 것으로 판정하지 않도록 MODULE은 사전 요청 실패 판정에서 제외했다.
- 모듈 그래프: `JsRealm.loadModule`이 그래프를 가져와 파싱만 하고 `ModuleGraph.evaluate`가 링크·평가한다. 가져오기/구문 오류는 script 요소의 `error`, 평가 중 오류는 보고만 하고 `load`를 보낸다. 인라인 모듈은 각각 고유 키를 갖고(이전에는 두 번째 인라인 모듈이 실행되지 않던 결함 수정) import 기준은 문서 기준 URL이다. 상대 import는 리디렉션 뒤 최종 URL 기준이다. 같은 모듈을 여러 script 요소·import가 요청해도 한 번만 가져오고(`ScriptLoads`의 module map과 텍스트 캐시) 한 번만 평가한다. nomodule 일반 스크립트는 건너뛴다.
- 검증(JDK 6u45, 스냅샷 `build/snap-p1504c`): `ModuleScriptTest` 5건(인라인 모듈 둘·nomodule·공유 의존, 중복·순환, 최종 URL 기준 해석, 그래프 실패 error/평가 오류 load, 동적 모듈)과 `core.ModuleScriptHttpTest` 4건(실제 컴포넌트·HTTP: MIME 강제, 교차 오리진 CORS, 쿠키와 자격 증명 모드, 리디렉션 import·1회 요청/평가) 통과. js/core/dom/html/css/net/storage 전체 묶음 1096 통과/실패 0/건너뜀 2(Origin 헤더 제한 1건, 제품 JAR 없음 1건). 변경 파일 sourcecheck 28파일 0위반.
- 미완료: 모듈 그래프의 병렬 가져오기(의존 모듈은 평가 시 순차로 가져옴), 교차 오리진 모듈의 사전 요청(preflight) 지원, 모듈의 `integrity`/CSP. ES2015 `import()`는 ES2020이라 범위 밖이다.

### 2026-11-16 · P15-04-a / P15-04-b-1 · 스크립트 병렬 요청, 준비 시 실행, 문서 큐 인계, 동적 스크립트

- 변경 파일: `AGENTS.md`, `js/host/ScriptLoads.java`(신규), `PageScripts.java`, `ElementExtras.java`, `html/ParserNodeData.java`, `html/TreeBuilderBase.java`, `ScriptLoadingTest.java`(신규), `core/ParallelScriptLoadingTest.java`(신규), `PageScriptsTest.java`, `PageFixture.java`, 구조/기능/진행 문서.
- `ScriptLoads`가 외부 스크립트 가져오기를 발굴 즉시 데몬 스레드에서 병렬로 시작한다(배경 가져오기 동시 6개 제한, 파서를 막는 스크립트는 제한 제외). 완료는 소유 스레드(파서 → 확정 뒤 문서 큐)에만 전달되고 가져오기 스레드에서는 스크립트를 실행하지 않는다. 닫으면 모든 늦은 응답이 보이지 않는다.
- 파서는 스크립트 경계와 파서 차단 스크립트 대기 중에 이미 가져온 async를 실행한다. defer/module은 파싱 뒤 문서 순서로 실행하며 가져오기는 이미 병렬 진행이다. 작업 큐(Scheduler)가 있으면 DOMContentLoaded 뒤 loader가 남은 async를 기다리지 않고 반환한다. 페이지는 먼저 확정되고, 남은 async는 가져오는 대로 문서 큐에서 실행하며 마지막 것이 끝나면 `readyState=complete`/load를 보낸다. 큐가 없는 호스트(시험)는 로더 스레드에서 기다린다.
- 동적 스크립트: 문서에 삽입된 파서 생성이 아닌 script를 삽입 시점에 준비한다. 인라인은 즉시, 외부는 즉시 가져오기를 시작하여 async(기본, force-async)이면 도착 순서로, `script.async=false`이면 삽입 순서로 실행한다. `async` IDL 프로퍼티는 force-async 기본값을 반영한다. 중첩 실행은 마이크로태스크 검사점을 가장 바깥 스크립트가 끝날 때로 미루고, 바깥 스크립트의 document.write 텍스트를 버리지 않는다.
- 검증(JDK 6u45, 스냅샷 `build/snap-p1504`): `ScriptLoadingTest` 13건(래치 장벽: 발굴 즉시 시작, 파서 대기 중 async 실행, defer 문서 순서, DCL이 async를 기다리지 않고 load는 기다림, 실패 async의 error+load, 동시 6개 제한, 닫을 때 대기 종료와 늦은 응답 억제, 큐 모드 확정·큐 스레드 실행, 동적/순서 있는 스크립트, 중첩 마이크로태스크, 중첩 document.write)과 `ParallelScriptLoadingTest` 5건(실제 컴포넌트·HTTP 집합 지점으로 세 요청 동시 진행, 느린 차단 스크립트 중 async 실행, 확정 후 느린 async와 load, 동적 외부 스크립트, 이동으로 교체된 페이지의 늦은 응답 무시) 통과. js/core/dom/html/css 전체 묶음(`--scan`) 951 통과/실패 0/건너뜀 1(제품 JAR 없음). 다른 작업자와 CPU를 공유하던 190초짜리 첫 실행에서는 `PageRuntimeIntegrationTest.testConfirmedQueriesBeforeEdtAndImmediateMutation`(5초 대기 초과)이 두 번 실패했으나, 같은 클래스 단독·core 패키지 단독·묶음 재실행(26초)에서는 통과했다. 부하에 민감한 시간 의존 시험으로 보지만 원인을 완전히 확정하지는 못했다.
- 미완료: P15-04-b-2(load가 CSS/이미지/프레임을 기다림 — P18-05/P19-08/P20-02 의존), P15-04-c(module 호스트 규칙: JavaScript MIME·CORS·credentials·리디렉션 기준 URL).

### 2026-11-16 · P15-08-b-3-b-2-b · 파서 CSS/import의 일반·defer 스크립트 차단과 취소

- 변경 파일: `AGENTS.md`, `core/PagePresentation.java`, `PageResources.java`, `PageRuntime.java`, `ParsingPageLoader.java`, `HtmlPageDocument.java`, `css/CssEngine.java`, `cssom/ContextualStyleSheetLoader.java`, `DocumentStyleSheets.java`, `StyleSheetBuilder.java`, `js/host/HostEnvironment.java`, `PageScripts.java`, `ParserStyleBlockingTest.java`, 구조/기능/진행 문서.
- styleReady/stylesheetFound에서 파서 시트를 비동기로 발굴하고 생성 당시 CSS type·활성·미디어·대체 시트 조건을 보관한다. ContextualStyleSheetLoader가 소유 요소와 중첩 import의 조건을 상속하므로 비활성/미일치 부모 시트의 import도 차단하지 않는다. 동적으로 생성한 링크는 파서 차단 집합에 넣지 않는다.
- 일반 파서/외부 defer 스크립트 실행 직전에 prepareParserScript가 적용 가능한 앞선 CSS/import를 기다린다. 대기는 스크립트 실행 한도/EDT/확정 뒤 렌더링 밖에서 한다. async는 이 차단 훅을 쓰지 않는다. CSS 오류 응답은 최종 실패 상태로 정리하여 스크립트 실행을 계속한다.
- JS 비활성화는 대기만 풀고 HTML/CSS를 유지한다. 새 이동/폐기는 CSS 네트워크와 대기 모두 취소한다. 단일 PagePresentation은 준비된 스타일을 파싱 중 조회/좌표와 확정 뒤 그림에 공유한다.
- 검증(JDK 6u45): 제어된 HTTP CSS 장벽으로 일반 외부·인라인/중첩 import·defer, async/미일치 media/비활성/동적·생성 후 type 변경 시트의 비차단, 404/500, 이동·비활성화·폐기를 검증했다. 전용 9건 통과(`build/parser-css-blocking-final-validation.log`), 관련 런타임/뷰/리소스/실제 렌더링/CssEngine 묶음 34건 통과(`build/parser-css-blocking-validation.log`, 마지막 type 조건 추가 전). 최신 전체 `test.bat` 회귀 1411 통과/실패 0/건너뜀 1, 실패 실행기 종료 코드 1 확인(`build/latest-validation.log`). 건너뜀은 기존 JRE 6 HttpURLConnection의 제한 Origin 헤더 사례이다. 최신 전체 sourcecheck 980파일 0위반(`build/latest-sourcecheck.log`). 제품 JAR도 최신 소스로 다시 생성했다.
- HTML5 2014-10-28의 scripting/document-metadata 조회 근거는 직전 로그와 같다. 적용 가능한 media와 생성 당시 type/활성 상태를 차단 범위로 삼고, import에서 부모 범위를 보존했다. module 스크립트의 CSS/그래프 실행은 다음 모듈 호스트 항목에서 검증한다.
- 정확성 재검토: P15-04의 체크를 되돌렸다. PageScripts.completeParsing은 async를 EOF/DCL 뒤 문서 순서의 목록으로 처리하고 외부 defer/async 요청도 발굴 시 시작하지 않는다. 기존 fake 소스 순서 시험은 특정 한 순서만 확인하므로 일반 async 순서 완료 근거가 아니다. ScriptSource도 classic/module을 구분하지 않아 module MIME/CORS 정책이 없다. ES2015 모듈 언어 실행 시험의 완료는 유지하되 브라우저 연결 완료로 표시하지 않는다. P15-04-a/b/c로 후속 작업을 나눴다.
- 모듈 호스트 규칙 조회: `https://html.spec.whatwg.org/multipage/scripting.html`, 조회 2026-11-16, Last-Modified 2026-11-16 10:39:02 GMT, JRE 25 webfetch, HTTP 200(`build/html-ls-scripting-20261116.txt|meta`). HTML5 2014와 별도 규칙으로 async의 준비 시 실행·module 교차 오리진 CORS·JavaScript MIME 요구를 확인했다. 아직 이 규칙의 구현 완료를 주장하지 않는다.
- 최신 제품 JAR 단독 클래스패스(`SWP_CLASSES=build\swingwebpanel.jar`)로 `examples\run.bat StaticPageExample --smoke` 통과: page=classpath://org/duckdns/hjow/swingwebpanel/examples/pages/index.html, renderedHeight=1020, sourceLength=757(`build/latest-example-smoke.log`). 실제 창 캡처 검증은 기존 환경 제약으로 여전히 미완료다.

### 2026-11-16 · P15-08-b-3-b-2-a · 파서/뷰의 단일 표현 상태와 내부 스타일 조회

- 변경 파일: `AGENTS.md`, `core/PagePresentation.java`, `PageCleanup.java`, `PageView.java`, `PageResources.java`, `PageRuntime.java`, `HtmlPageDocument.java`, `ParsingPageLoader.java`, `SwingWebPanel.java`, `PageRuntimeIntegrationTest.java`, 구조/기능/진행 문서.
- PagePresentation이 DOM 관찰/CssEngine/PageResources/불변 배치를 소유한다. 파싱 전 만들고 파서 소유 중 getComputedStyle/getBoundingClientRect가 같은 엔진을 사용하며, 확정 뒤 뷰도 그 상태를 이어받는다. 파싱 후 추가한 style과 DOM 변경은 같은 엔진을 무효화한다. 변경이 없으면 배치 결과를 재사용한다.
- JavaScript만 끈 경우 페이지 표현·대기 CSS는 유지한다. 이동 취소/문서 폐기는 리소스를 즉시 닫고, 공용 PageCleanup이 파서 반환 및 표현 잠금 뒤 관찰자를 해제한다. clean/dispose는 스크립트 중단을 먼저 요청한 뒤 뷰를 해제하여 실행 중 DOM/CSS 접근과의 경합을 피한다.
- 검증(JDK 6u45): 파싱 중 내부 스타일/좌표/width 변경·이후 마크업 및 style·확정 후 타이머/픽셀, JavaScript 비활성화 후 지연 CSS 표시를 추가했다. 런타임/뷰/리소스/실제 렌더링 22건 통과(`build/parser-presentation-validation.log`), 취소 훅 최종 수정 뒤 수명/입력/JS host까지 64건 통과/실패 0(`build/parser-presentation-final-validation.log`). 변경 범위 sourcecheck 10파일 0위반. 아직 최신 전체 회귀 숫자로 표시하지 않는다.
- 규격 조회(기존 Java 6 webfetch 도구를 JRE 25에서 실행, 2026-11-16): HTML5 2014-10-28 `https://www.w3.org/TR/2014/REC-html5-20141028/scripting-1.html`, `https://www.w3.org/TR/2014/REC-html5-20141028/document-metadata.html`, 둘 다 HTTP 200. 결과와 메타데이터는 `build/html5-scripting-20141028.txt|meta`, `build/html5-semantics-20141028.txt|meta`. 파서가 만든 준비되지 않은 활성 스타일시트의 스크립트 차단 규칙을 대조했다.
- 미완료: 외부 CSS/import 대기 및 media/비활성/동적 스타일별 차단 범위(b-2-b). 이번 항목의 내부 스타일 조회만으로 외부 스타일의 파싱 순서를 완료 처리하지 않는다. async/module·이동/폼/download와 기존 단계도 남는다.

### 2026-11-16 · P15-08-b-3-b-1 · 확정 뒤 실제 계산 스타일·사각형·뷰포트 조회

- 변경 파일: `AGENTS.md`, `core/PageRuntime.java`, `PageView.java`, `ParsingPageLoader.java`, `SwingWebPanel.java`, `PageRuntimeIntegrationTest.java`, 구조/기능/진행 문서.
- 파싱 뒤 확정한 DOM의 조회가 화면에 사용하는 동일 PageView/CssEngine/Layout 결과를 사용한다. style/사각형 조회 시 같은 문서 큐에서 연결 변경을 먼저 반영하므로 스크립트가 width/display를 바꾼 직후에도 새 값을 읽는다.
- 문서 큐의 확정 인계에서 PageView를 먼저 준비한 뒤 타이머를 활성화한다. EDT의 pageConfirmed 통지가 늦어도 지연 0 타이머가 계산 스타일/좌표를 읽을 수 있으며, 늦은 EDT 통지는 같은 표현 상태를 재생성하지 않는다.
- getBoundingClientRect는 비어 있지 않은 border box의 합집합에서 EDT가 복사한 JViewport 위치를 뺀다. innerWidth/innerHeight도 실제 뷰포트 extent를 사용한다. 스크립트/문서 큐에서 Swing 위젯을 직접 읽지 않는다. 부모 뷰포트 변경 시 리스너를 옮기며 dispose 후 EDT에서 연결을 제거한다.
- 검증(JDK 6u45): EDT 통지를 보류한 상태의 확정 후 타이머 조회, 같은 작업 안의 width/display 변경·border/padding 크기·숨김 사각형, 실제 JScrollPane 양축 스크롤/extent와 리스너 해제를 추가했다. 런타임/뷰/실제 렌더링 18건 통과(`build/presentation-validation.log`), 입력/JS host 35건 통과(`build/presentation-host-validation.log`), 변경 범위 sourcecheck 6파일 0위반. 실행 중인 전체 회귀와 분리한 `build/presentation-classes`, `presentation-test-classes`에서 검증했다.
- 최신 제품 전체를 `build.bat`로 다시 빌드하여 JAR 생성 성공(`build/presentation-build.log`). 새 `build/classes`로 위 관련 시험을 함께 다시 실행해 53건 통과/실패 0(`build/presentation-final-validation.log`). 최신 전체 sourcecheck도 976파일 0위반이다.
- 이전 113까지 전체 `test.bat` 검증도 완료: 1398 통과/실패 0/건너뜀 1, 실패 실행기 종료 코드 1 확인(`build/latest-validation.log`). 건너뜀은 기존 JRE 6 HttpURLConnection의 제한 Origin 헤더 사례이다. 전체 sourcecheck는 976파일 0위반(`build/latest-sourcecheck.log`). 이 전체 숫자는 114 조회 변경의 전체 회귀 결과로 표시하지 않는다.
- 미완료: 파서 소유 중 스타일·좌표 조회와 외부 CSS 스크립트 차단(b-3-b-2). offsetLeft/Top의 offsetParent 상대 계산, 변환/확장 배치와 분할 client rect 세부는 해당 입력/배치 단계에서도 계속 검증해야 한다. async/module·스크립트 이동/폼/download도 남는다.

### 2026-11-16 · P15-08-b-3-a · 실제 click의 신뢰 표시·작업 체크포인트·JS 중단 유지

- 변경 파일: `AGENTS.md`, `core/HtmlPageDocument.java`, `PageInput.java`, `js/host/PageScripts.java`, `js/dom/DomBridge.java`, `HandlerListener.java`, `JsListener.java`, `PageRuntimeIntegrationTest.java`, `PageDisposalTest.java`, 구조/진행 문서.
- 실제 AWT 주 버튼 click을 문서 큐에서 PageScripts 작업으로 전달한다. isTrusted를 유지하고 작업 끝에 Promise→MutationObserver 체크포인트를 수행한 뒤 타이머를 실행한다. 기본 링크는 DOM 취소 뒤 실행 여부를 판정한다.
- requestDisposal에서 DomBridge의 volatile 영구 콜백 차단을 세운다. 최종 dispose 전 직접 DOM 전달이 들어와도 인라인·함수·handleEvent 객체가 JS에 재진입하지 않는다. Java DOM 리스너와 기본 링크는 계속 작동하며 JS를 다시 켜도 중단된 페이지 핸들러는 재개하지 않는다.
- 검증(JDK 6u45): 실제 컴포넌트 const/화살표 클릭 카운터의 텍스트/class/style/픽셀 변경, 신뢰 표시, preventDefault, Promise/observer/timer 순서, 원문 보존, 비활성화 뒤 Java DOM/기본 HTTP 링크를 추가했다. 즉시 중단 직후 최종 해제 전 직접 이벤트 전달도 검증했다. 런타임·입력·JS host 42건 통과/실패 0(`build/page-runtime-input-validation.log`), 변경 범위 sourcecheck 9파일 0위반.
- 미완료: b-3-b 계산 스타일/좌표, 외부 CSS 차단·async/module, c 이동·폼·download. P15-10/12는 남은 폼/통합 항목 때문에 아직 상위 완료 체크하지 않는다.

### 2026-11-16 · P15-08-b-2 · 파싱 전 페이지 스크립트와 요청·저장소·취소 연결

- 변경 파일: `AGENTS.md`, `core/PageRuntime.java`, `ParsingPageLoader.java`, `HtmlPageDocument.java`, `BrowserSession.java`, `SwingWebPanel.java`, `js/host/PageScripts.java`, `tests/src/org/duckdns/hjow/swingwebpanel/core/PageRuntimeIntegrationTest.java`, 구조/기능 표/진행 문서.
- 표준 컴포넌트가 생성 시 ParsingPageLoader에 서비스를 연결한다. 새 DOM에 PageScripts를 설치한 뒤 파서를 실행하고, script 삽입 지점과 document.write를 전달한다. 수신 원문은 그대로 보관한다.
- 문서 확정 뒤 DocumentScheduler 활성화, 교체/clean/dispose/JS 비활성화는 즉시 requestDisposal·타이머/네트워크 취소. 현재뿐 아니라 파싱 중 런타임도 세션에 등록하여 무한 반복을 중단한다. 다시 켜도 이전 페이지는 재개하지 않는다.
- 최종 dispose는 파서 소유권 반환 뒤 별도 데몬에서 수행한다. 종료한 문서 실행기에서 해제 작업이 유실되지 않으며, 파서의 대기 script 목록과 동시 해제를 피한다. 취소된 외부 요청은 error/load 후속 이벤트도 보내지 않는다.
- 외부 스크립트는 같은 ResourceFetcher/UA/추가 헤더/쿠키/SSL/오리진/force 캐시 정책을 사용한다. BOM→HTTP charset→UTF-8로 해독하며 HTML meta 태그는 해석하지 않는다. 고전 스크립트의 문서 인코딩 상속과 module MIME/CORS/async 정확한 실행 순서는 아직 상위 b에 남는다.
- 검증(JDK 6u45): 실제 컴포넌트 파서/Promise/타이머/원문/HTTP 요청·캐시·저장소/파싱 중 비활성화/실행 중 타이머 교체·폐기/지연 외부 요청 취소 5건 통과. 기존 렌더링·네트워크·파서와의 묶음 32건(지연 요청 사례 추가 전), JS host와 최신 묶음 34건 통과/실패 0. `build/page-runtime-validation.log`, `build/page-runtime-host-validation.log`. 변경 범위 sourcecheck 8파일 0위반.
- 미완료: b-3 CSS/좌표/입력 연결, CSS 차단·async/module, c location/history/폼/download 공통 이동. 이 하위 연결만으로 P15 전체 지원을 표시하지 않는다. about:text는 base 요소가 있어도 opaque origin이라 Storage 접근이 SecurityError이며 취소 시험은 실제 HTTP 오리진으로 검증했다.

### 2026-11-16 · P15-08-b-1 · 잠금 없이 스크립트 폐기 요청

- 변경 파일: `js/host/PageScripts.java`, `tests/.../js/host/PageDisposalTest.java`, AGENTS/PROGRESS/ARCHITECTURE/progress/jsdom.
- `PageScripts.requestDisposal()`는 잠금 없이 disposed와 실행 제한 취소를 설정한다. 후속 작업/평가/타이머/이벤트 실행은 즉시 억제된다. 기존 dispose는 이 요청 후 페이지 잠금을 얻어 자원을 완전히 해제한다. **호스트는 EDT에서 requestDisposal을 호출한 뒤 정리 작업자에서 dispose해야 한다.** 요청만으로 전체 해제를 완료 처리하지 않는다.
- 검증(JDK 6u45): js.host 전체 `TestRunner --scan build/test-classes --filter js.host` → **29 passed / 0 failed / 0 skipped**(`build/script-disposal-tests.log`), 변경 Java 2 파일 sourcecheck 0 위반. 새 2건은 잠금을 의도적으로 소유한 작업이 끝나기 전 요청 완료, 요청 후 타이머/rAF/이벤트/평가 억제와 후속 반복 dispose를 확인한다.
- 현재 SwingWebPanel의 실제 스크립트 런타임 설치는 여전히 미구현이다(P15-08-b 상위 미체크). 다음 연결 순서와 기존 인계 문서의 ThreadScheduler/동기 dispose 안내를 `docs/progress/jsdom.md`에서 갱신했다.
- 배포 경계 확인: `SWP_CLASSES=build/swingwebpanel.jar`로 `examples/run.sh StaticPageExample --smoke` 실행 → `smoke OK ... renderedHeight=1020 sourceLength=757`. JDK 6에서 제품 JAR만 참조하여 예제를 컴파일/실행하고 예제 클래스패스의 상대 HTML/CSS도 불러왔다. 외부 엔진·테스트·도구 클래스 없이 성공했다.
- 최종 재검증: `test.bat`(JDK 6u45) → **1390 passed / 0 failed / 1 skipped**(`build/latest-validation.log`), 실행기 의도적 실패 종료 코드 1 확인. 건너뜀은 기존 JRE 6 Origin 헤더 제한 한 건이다. 전체 sourcecheck → **974 파일 0 위반**(`build/latest-sourcecheck.log`). 제품 JAR도 이 소스로 다시 생성했다. 전체 개발 완료를 뜻하지 않으며 남은 체크 항목은 AGENTS의 정본을 유지한다.

### 2026-11-16 · P15-08-a · 파싱 소유권 인계와 문서 큐 타이머 기반

- 변경 파일: `core/DocumentScheduler.java`, `tests/.../core/DocumentSchedulerTest.java`, AGENTS/PROGRESS/ARCHITECTURE. P15-08을 스케줄러 기반(a)/파싱 전 런타임·리소스 연결(b)/공통 이동 정책(c)으로 나눴다. 상위 P15-08은 미완료다.
- 파싱 중 타이머는 확정까지 보류하고 활성화 후 같은 BrowserSession 문서 큐에서 전달한다. 기한순/동일 기한 등록순을 지킨다. 한 페이지 대기 작업 상한 100000(기존 JS 타이머 한도와 동일). 취소/close/페이지 교체/폐기 후 콜백을 억제한다.
- 프로세스 공용 데몬 시계와 문서 큐의 전달 항목은 Pending을 약하게만 참조한다. 소유 페이지가 수거되면 ReferenceQueue 정리기가 먼 기한의 시계 항목도 제거한다(1초 주기). JRE 6에서 future 취소 시 큐를 명시적으로 제거한다. 공용 시계는 컴포넌트 폐기 후에도 프로세스 서비스로 남지만 페이지/세션 참조를 보관하지 않는다.
- 검증(JDK 6u45, headless): `TestRunner org.duckdns.hjow.swingwebpanel.core.DocumentSchedulerTest org.duckdns.hjow.swingwebpanel.core.LifecycleTest` → **12 passed / 0 failed / 0 skipped**(`build/scheduler-tests.log`); 변경 Java 2 파일 sourcecheck 0 위반. 새 5건은 확정 전 보류·문서 스레드, 기한/동일 기한 순서, 실제 문서 큐 전달 뒤 취소, 교체/close, dispose 없이 콜백→세션 순환 참조의 GC/ReferenceQueue 관측을 확인한다. 공용 시계의 후속 타이머를 다른 세션에서 관측해 큐 전달 시점을 확인했다.
- **아직 ParsingPageLoader에는 설치하지 않았다.** 런타임이 파싱 전 생성, 확정 시 activate, 교체 시 close를 호출하는 연결은 P15-08-b다. 다음 작업자가 기존 ThreadScheduler를 바로 붙여 파서/렌더러와 DOM을 동시에 수정하지 않도록 이 스케줄러를 사용한다.

### 2026-11-16 · P07-07-b-2 · body 여백 우선순위와 전체 회귀 확인

- 변경 파일: `css/cascade/HtmlPresentationalHints.java`, `tests/.../css/cascade/LegacyBodyMarginsTest.java`, AGENTS/PROGRESS/FEATURE_MATRIX/ARCHITECTURE.
- marginwidth/height가 존재하면 유효하지 않아도 개별 left/right/top/bottommargin으로 내려가지 않는다. 파싱 실패 시 UA 8px 또는 CSS 재정의를 따른다. HTML 비음수 정수의 ASCII 공백/부호/숫자 접두사/음의 0을 처리하고 고정 아홉 자리 잘림을 없앴다. 프레임 컨테이너의 여백은 P20-02에 남긴다.
- 규격: HTML5 2014 렌더링 §10.2/10.3.2와 infrastructure §2.4.4.1/2.4.4.2. `https://www.w3.org/TR/2014/REC-html5-20141028/infrastructure.html`을 tools/webfetch/JRE 25로 2026-11-16 조회(200, Last-Modified 2014-10-24; `build/specs/html/microsyntaxes-2014.txt[.meta]`). CSS 수치가 유한 double 범위를 벗어나면 힌트를 생략한다.
- 새 시험 **4/0/0**: 속성 우선순위/숫자 접두사와 큰 값/DOM 변경·CSS 우선/실제 상자·픽셀. 변경 Java 2 파일 sourcecheck 0 위반.
- 이후 현재 작업 트리 전체 `test.bat`(JDK 6u45) → **1383 passed / 0 failed / 1 skipped**, 의도적 실패 실행기 종료 코드 1 확인(`build/integration-after-rendering.log`). 건너뜀은 기존 JRE 6 Origin 요청 헤더 제한 1건이다. 전체 sourcecheck → **971 파일 0 위반**(`build/sourcecheck-after-rendering.log`). 제품 JAR도 이 빌드로 갱신했다.
- 다음: P15 컴포넌트 연결의 독립 기반(문서 큐 타이머와 페이지 수명)을 작은 하위 작업으로 나눈다. P07의 나머지 legacy/프레임·P09 실제 UI·P10 이후는 계속 미완료다.

### 2026-11-16 · P07-07-b-1 · body 링크 색과 상태 선택자 수정

- 변경 파일: `css/cascade/{HtmlPresentationalHints,StatefulPresentationalHints,StyleResolver}.java`, `css/selector/SelectorMatcher.java`, `tests/.../css/cascade/LegacyLinkHintsTest.java`, AGENTS/PROGRESS/FEATURE_MATRIX/ARCHITECTURE.
- 규격 조회: tools/webfetch로 `https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html`(HTML5 REC 2014-10-28 §10.3.2)을 2026-11-16 조회, 상태 200, Last-Modified 2014-10-24, 실행 JRE 25. 원문·메타데이터는 `build/specs/html/rendering-2014.txt[.meta]`에 보관하며 제품 판정 기준은 고정 2014 판본이다.
- body link/vlink/alink를 해당 상태의 링크에 구체성 0 작성자 힌트로 적용한다. MatchContext를 공유하여 동적 의존성을 기록하고 상태 변경 시 후손까지 무효화한다. 본문 속성/본문 교체는 본문 밖 링크도 갱신한다. 작성자 CSS/inline은 힌트를 덮어쓴다. :link가 방문 링크에도 맞던 오류와 :visited가 비링크에도 맞을 수 있던 오류를 고쳤다.
- 검증(JDK 6u45, headless): LegacyLinkHintsTest **5/0/0**, CSS 전체 `TestRunner --scan build/test-classes --filter css` → **129 passed / 0 failed / 0 skipped**(기반 실행기 자체 시험 제외), 변경 Java 5 파일 sourcecheck 0 위반. 계산 색·상속·속성 삭제/본문 교체·선택자 배타성과 currentColor 배경의 실제 픽셀을 확인했다.
- 남은 일: 실제 방문 기록/마우스 active 상태 공급은 P11, 기타 LEG-04 속성과 프레임 여백은 후속. P07-07-b/07 전체는 미완료 유지. 조회 중 발견한 body 여백의 잘못된 상위 속성 처리/기본값은 P07-07-b-2에서 수정한다.

### 2026-11-16 · P09-08-b-1 · 정적 예제와 HTTP/classpath 링크·이력 자동 검증

- 변경 파일: `examples/src/.../StaticPageExample.java`, `examples/resources/**`, `examples/run.sh|bat`, `examples/README.md`, `tests/.../SwingRenderingTest.java`, `tests/resources/.../static/**`, AGENTS와 인계 문서.
- 예제는 클래스패스 HTML/상대 CSS/상대 링크와 제목·문단·목록·표 내용·실패 이미지 대체 표시·스크롤을 제공한다. 기존 BlankPageExample의 주소/이력/상태/폐기 UI를 재사용한다. `--smoke`는 실제 paint 픽셀·원문·내용 높이를 검사하며 headless에서도 실행한다. 예제 자원은 제품 JAR에 포함하지 않는다.
- 검증(JDK 6u45): `java "-Djava.awt.headless=true" -cp "build/classes;build/test-classes" org.duckdns.hjow.swingwebpanel.testing.TestRunner org.duckdns.hjow.swingwebpanel.SwingRenderingTest` → **6 passed / 0 failed / 0 skipped**(`build/static-integration.log`). HTTP/classpath 양쪽에서 상대 CSS/클릭/뒤·앞 이동/force/원문·제목·이력을 확인했다. 공개 removeHyperlinkEvents 후에도 이동한다.
  `SWP_CLASSES=build/classes` 지정 후 `examples\\run.bat StaticPageExample --smoke` → `smoke OK ... renderedHeight=1020 sourceLength=757`. examples와 변경 시험 sourcecheck 10 파일 0 위반. sh 자원 복사는 추가했으나 이번 실행은 bat를 사용했다.
- 실제 UI: JRE 6 StaticPageExample을 실행하여 `SwingWebPanel` 창 생성 및 반환된 java.exe 창을 확인했다. Computer Use 캡처는 `FrameArrived timed out: timed out waiting on channel`, 재선택·활성화 후 `window capture timed out: timed out waiting on channel`로 실패했다. UI 조작/시각 확인은 수행하지 않았으며 시험용 JVM은 정리했다. **P09-08-b-2 미완료**. P09-08/09 및 P09 단계 전체를 체크하지 않는다.
- 초기 headless 실행에서 PowerShell이 따옴표 없는 `-Djava.awt.headless=true`를 분리해 main class 오류가 났으며, 인수를 따옴표로 감싸 재실행한 위 결과가 실제 검증이다. 제품 소스 실패는 아니다.
- 다음: 환경에 의존하지 않는 P07-07-b legacy 표현 속성/화면 동작을 계속 구현한다.

### 2026-11-16 · P09-08-a · 포인터 링크 입력과 DOM/Swing/기본 이동 순서

- 변경 파일: `core/PageInput.java`(신규), `core/BrowserSession.java`, `SwingWebPanel.java`, `tests/.../core/PageInputTest.java`, AGENTS와 진행/계약/구조 문서.
- 실제 컴포넌트 포인터를 문서 큐 hit test/DOM click → EDT Swing ACTIVATED → 기본 이동에 연결했다. DOM 취소·리스너의 직접 이동/clean/dispose를 확인하며 전달 중 바뀐 href도 반영한다. 사용자 통지 제거와 내부 기본 이동은 독립적이다. hover는 EXITED→ENTERED 순서, 특수 주소는 null URL/전체 설명이며 상태바는 로딩 상태를 보존하여 복원한다. 차단 스키마와 원격→classpath 기본 이동은 거부한다.
- 검증(JDK 6u45): 제품/시험을 JDK 6 javac로 컴파일하고 `TestRunner PageInputTest BrowserSessionTest SwingWebPanelTest SwingRenderingTest`(각 완전한 패키지 이름) → **35 passed / 0 failed / 0 skipped**(`build/input-regression.log`). 변경 Java 4 파일 sourcecheck 0 위반.
- 새 5건은 Component.dispatchEvent의 MouseEvent, DOM 이벤트 신뢰/취소·문서 큐, Swing EDT와 순서, DOM/Swing 직접 이동 우선, href 변경, 통지 전체 제거 후 한 번 이동, 금지 스키마, 호버 상태 복원을 검증한다.
- 한계: 이벤트별 MouseEvent/KeyboardEvent 데이터·hover/focus CSS·키보드·IME/폼/확대/변환 입력은 P11/P10 후속이다. JS 핸들러는 P15 core 연결 전이며 현재 Java DOM 리스너 수준 검증이다. P11-03/04 전체 체크는 후속 통합 후 판단한다. 다음 P09-08-b.

### 2026-11-16 · P09-07-b · 외부 CSS/이미지 비동기 연결과 force 정책

- 변경 파일: `core/{PageResources,PageRequestContext}.java`(신규), `core/{PageView,HtmlPageDocument,ParsingPageLoader}.java`, `SwingWebPanel.java`, `net/HttpCache.java`, `tests/.../{SwingRenderingTest,core/PageResourcesTest,net/HttpCacheTest}.java`, AGENTS와 진행/구조/기능 표.
- 페이지별 리소스 상태를 공통 ResourceFetcher/쿠키/SSL/HTTP 캐시에 연결했다. link/@import는 대기 시 문서 큐를 막지 않고 완료 후 다시 수집한다. 이미지 자연 크기와 픽셀은 자체 디코더 결과로 갱신한다. 이동 시 UA/헤더/force만 복사하여 이전 NavigationRequest/이력을 붙잡지 않는다. force는 CSS/import/이미지 전체에 전달된다.
- 제한: 컴포넌트당 데몬 작업자 4개(5초 유휴 종료), 큐 256개, 페이지별 URL 256개/디코딩 보관 64 MiB, 이미지 입력 8 MiB/한 프레임 8 Mi 픽셀/전체 16 Mi 픽셀/256프레임. 교체·clean·dispose는 요청/디코딩을 취소하고 오래된 완료를 게시하지 않는다. 취소된 응답의 HTTP 캐시 저장도 잠금 안에서 거부한다. 사용자 PageLoader에서도 clean이 뷰용 HTTP 캐시를 비운다.
- 검증(JDK 6u45): `test.bat --filter SwingRendering` 4/0/0; 이후 변경 포함 JDK 6 javac로 제품/관련 시험을 다시 컴파일하고 `TestRunner`에 PageViewTest, PageResourcesTest, SwingRenderingTest, HttpCacheTest, CacheIntegrationTest, SwingWebPanelTest를 지정 → **43 passed / 0 failed / 0 skipped**(`build/resource-regression.log`). 변경 영역 sourcecheck 33 파일 0 위반.
- 시험: CSS/import/이미지 픽셀, 공통 UA·추가 헤더·쿠키, 일반 캐시와 force 요청 수, 느린 CSS 도중 새 이동, 실패 URL/손상/차단 스키마·원격→classpath 차단, 취소 콜백 억제, 취소 후 캐시 재유입 방지.
- 남은 일: load/error DOM 이벤트·picture/srcset·애니메이션 시계·SVG/AVIF·오디오 재생은 P18, 하위 작업을 포함한 진행률 100 시점은 P19-08이다(현재 주 문서 확정 시 100). P09-08 링크 연결이 다음이다.

### 2026-11-16 · P09-07-a · Swing에 자체 레이아웃/렌더러 연결

- 변경 파일: `core/PageView.java`(신규), `core/BrowserSession.java`, `SwingWebPanel.java`, `tests/.../core/PageViewTest.java`, `tests/.../SwingRenderingTest.java`, `AGENTS.md`, 문서 4종(PROGRESS/ARCHITECTURE/FEATURE_MATRIX/progress/integ).
- PageView가 문서 큐에서 CSS·레이아웃·DisplayList를 만들고 EDT에서 크기 게시와 그리기를 수행한다. DOM 변경은 연결된 트리에만 재배치를 예약하고 같은 뷰포트 범위는 재배치하지 않는다. 가로 넘침 시에도 컴포넌트가 아닌 JViewport 범위를 기준으로 줄바꿈한다. 페이지 동일성을 작업 전/후·EDT 게시·그리기에서 확인하여 clean과 교체 뒤 낡은 결과가 그려지지 않게 한다.
- 검증(JDK 6u45): `scripts/pkgtest.sh view1 "core net storage dom html css layout render content media download @root" "core @root"` → **90 passed / 0 failed / 0 skipped**. 새 5건은 실제 JPanel.paint 픽셀·스크롤 크기, 넘치는 페이지의 창 폭 변경, DOM/CSS 갱신과 원문 보존, 분리된 노드 무시, 같은 폭 반복/EDT 통지/문서 큐·오래된 콜백/clean을 확인한다. 변경 Java 5 파일 sourcecheck 0 위반.
- 한계: 외부 CSS/이미지는 다음 P09-07-b, 클릭은 P09-08, JS 실행은 P15 core 연결 전이다. 실제 창 조작 검증은 아직 없으며 자동 오프스크린 결과와 구분한다. 로그의 `expected failure of a test listener` 스택은 기존 HyperlinkDispatcher 예외 격리 시험의 의도된 출력이다.
- P07-07은 세 하위 항목으로 분할했다. 파싱/문서 모드(a)는 확인했으며 과거 화면 규칙(b)·프레임/맵/폼(c)은 P09/P10/P11/P17/P20 후 검증한다. P07/P09 전체 완료는 표시하지 않는다.

### 2026-11-16 · P07-06-a · template 내용 소유권과 복제/가져오기/이동 수정

- 변경 파일: `dom/Element.java`, `dom/Node.java`, `html/ParserNodeData.java`, `tests/src/org/duckdns/hjow/swingwebpanel/html/TemplateOwnershipTest.java`, `AGENTS.md`, `docs/PROGRESS.md`, `docs/ARCHITECTURE.md`.
- template 내용을 전역 WeakHashMap 대신 해당 Element 필드에 보관한다. 이전 값은 Document를 통해 map 키에 다시 도달할 수 있어 약한 키만으로는 수거를 보장하지 못했다. deep clone/import는 중첩 내용까지 독립 복사하고 adopt는 조각·자손·속성의 소유 문서도 바꾼다. shallow clone은 빈 내용이다.
- 검증: JDK 6u45 `scripts/pkgtest.sh tmpl "html dom net" "html dom"` → **103 passed / 0 failed / 0 skipped**(새 회귀 5건). 해당 소스 sourcecheck 56 파일 0 위반. ReferenceQueue로 문서 수거를 관측했으며 GC의 특정 시점을 보장하지 않는다.
- 미해결: P07-07의 과거 요소 표시, P07-09 단계 최종 확인은 후속. 다음: P07-07을 파서/표현 속성/화면 동작으로 나누고 화면 통합을 진행한다.

### 2026-11-16 · P02-08/09 · 저장소 전체 Java 6 빌드와 규칙 검사

- 변경 파일: `AGENTS.md`, `docs/PROGRESS.md`, `docs/ARCHITECTURE.md`(entitygen 도구 목록 누락 수정), `SwingWebPanel.java`(쿠키 Javadoc의 오래된 미연결 설명 수정).
- 검증: `tools\\sourcecheck\\build.bat`, `tools\\sourcecheck\\run.bat --max 150` → 951 파일 0 위반; `run.bat --selftest` → 불량 샘플 종료 1/양호 샘플 종료 0 확인.
  `test.bat`(JDK 6u45) → **1352 passed / 0 failed / 1 skipped**, 의도적 실패 스위트 종료 코드 1 확인. 건너뜀은 `HttpFetcherTest.testRefererOriginAndCacheHeaders`(JRE 6의 제한된 Origin 헤더)이며 기존 사유를 유지한다.
- 검사는 Javadoc 존재/두 언어/반복문 주석을 기계적으로 확인한다. 익명 클래스 본문과 설명의 의미·중요 분기의 판정은 자동 검사 범위 밖이며 이후 소스 변경에도 규칙을 계속 적용한다. 제품 JAR/테스트/도구 분리와 금지 API 검사를 포함한다.
- 다음 작업: P07-06-a. 중첩 template 복제/가져오기/이동 및 전역 보관으로 인한 수거 방해를 수정한다. P07-07/09와 이후 단계는 미완료 유지.

### 2026-11-16 · P18-03(WebP) · 중단되어 있던 WebP 디코더(MEDIA-IMG-B) 완성

- 이어받은 상태: 커밋되지 않은 `media/image/webp/Vp8*.java` 7개(VP8 키 프레임 디코더 약 2700줄)만 있었고 컨테이너, VP8L, 알파, 애니메이션, 시험이 없었다. 소스 `TestHttpServer.java` 의 주석 3줄 변경도 커밋되지 않은 채였다(별도 commit).
- 추가: `WebpDecoder`(RIFF/VP8X/ALPH/ANIM/ANMF, libwebp `WebPAnimDecoder` 와 같은 합성), `Vp8lDecoder/Vp8lBits/Vp8lHuffman`(무손실), `WebpAlpha`, `WebpRaster`. 이어받은 VP8 코드는 시험 결과 수정 없이 맞았다(반복문 주석만 보강).
- 검증 (JDK 6u45, `scripts/pkgtest.sh webp "media/image" "media/image/webp"`): 24 passed / 0 failed(기반 7 + WebP 17). `scripts/pkgtest.sh mimg "media/image" "media/image"` 92 passed / 0 failed. sourcecheck 0 위반.
  기준은 **libwebp 의 실제 디코딩 결과**다: Chrome(WebGL 로 알파를 곱하지 않고 읽음) 11개 + libwebp 1.6.0(Pillow, ffmpeg 프리셋) 92개 파일의 모든 프레임이 바이트 단위로 같다. 자체 인코더로 만든 입력이 아니다.
  방법과 범위, 계측한 경로, 한계는 `docs/progress/media-img-b.md`. 무작위 손상 2.7만 회(범위 밖 임시 실행)와 시험 내 1600회에서 `MediaException` 이외의 실패와 멈춤은 없었다.
- 한계: 건너뜀 플래그, 단순 루프 필터, 루프 필터 변화량, 토큰 파티션 2개 이상은 libwebp 인코더가 만들지 않아 비교하지 못했다. ICC/EXIF 방향은 적용하지 않는다. P18-03 은 SVG/AVIF 후에 체크한다.
- 환경 메모: 이 개발 환경의 Bash 도구는 긴 here-document(`<<'EOF'`)를 실패시킨다(`unexpected EOF`). 긴 내용은 Write 로 파일을 만든 뒤 실행한다. Windows JDK 에 넘기는 클래스패스는 `cygpath -m` 을 쓰고 `MSYS2_ARG_CONV_EXCL="*"` 를 지정한다.

### 2026-11-16 · P09-04~06, P15(코어 연결 제외), P16-01/08 · 렌더러와 JS-DOM 연결 검증

- 렌더러 (`scripts/pkgtest.sh render "render layout css html dom core net storage content media @root" "render"`): 37 passed / 0 failed, sourcecheck 21 파일 0 위반. `DisplayListBuilder`(CSS 2.1 부록 E 정상 흐름 순서, 클립/불투명도 그룹), `PagePainter`(상태 복구, 클립 건너뛰기), `BorderPainter`(8 스타일), 텍스트·장식선·목록 표식·이미지 프레임·배경 이미지, 대체 그림(미지원 URL/alt/비디오 안내/오디오 막대), `HitTester`(요소/링크 URL/텍스트 오프셋/클라이언트 사각형). 복합 문자: JRE 6u45 에서 `TextLayout` 이 히브리어에 JVM 을 종료시키고 `drawString` 은 아무것도 그리지 못함 → 글리프 벡터 경로만 사용(재현 표 docs/progress/render.md).
  체크: P09-04~06. P09-07/08(SwingWebPanel 통합)과 P09-09 는 통합 후.
- JS-DOM (`scripts/pkgtest.sh jsd "js css html dom core net storage content media @root" "js dom html css core"`): 883 passed / 0 failed(js/dom/html/css/core 전체 포함). sourcecheck js/dom 312 파일 0 위반. `DomBridge/DomInterfaces`(명세 문자열 표 기반 바인딩, 동일 노드=동일 JS 객체), `PageScripts`(스크립트 모델 classic/defer/async/module, 이벤트 핸들러 속성, document.write, 이벤트 루프/타이머/rAF/queueMicrotask/MutationObserver, 쿠키, 저장소/storage 이벤트), `HostEnvironment/ScriptSource/Scheduler`.
  체크: P15-01~07, 09, 11, P16-01, P16-08. 미체크: P15-08(이동 정책 연동)/P15-10(JS 비활성)은 core 연결 후, P15-12 는 통합 페이지 시험 후.
- 후속: core 연결(ParsingPageLoader 에 PageScripts, 렌더러/스크롤/링크 클릭), `Element.cloneNode` 의 template 내용 복제(dom), XHR/fetch/Worker/DOMParser/Canvas2D 는 P20.

### 2026-11-16 · P09-01~03 · 레이아웃 엔진(블록/인라인/텍스트)

- 검증 (JDK 6u45, `scripts/pkgtest.sh lay "layout css html dom core net storage content media @root" "layout"`): 44 passed / 0 failed. sourcecheck 23 파일 0 위반. 시험은 실제 HtmlParser+CssEngine(UA 시트)로 문서를 만들고 기대 폭은 글꼴 advance 로 계산한다.
- API(상세 docs/progress/layout.md): `LayoutEngine.layout(doc, new CssStyleSource(cssEngine), replacedResolver, viewportW, viewportH)` → 변경되지 않는 `LayoutResult`(문서 좌표, border box, `LayoutBox.Type` BLOCK/ANONYMOUS_BLOCK/INLINE_BLOCK/REPLACED/LINE/INLINE/TEXT/MARKER, `getBoxes(node)`, `boxAt(x,y)`, 내용 크기). 전체 재배치, 문서 스레드에서 호출(내부 64 MiB 스택 보조 스레드).
- 주의: JDK 6u45 의 `Font.getStringBounds`/`TextLayout` 이 히브리어/아랍어 등 복합 문자에서 `fontmanager.dll` 접근 위반으로 JVM 을 종료시킴 → 레이아웃은 글자별 advance 합으로 우회. 렌더러가 같은 텍스트를 그릴 때 같은 충돌 가능성이 있어 확인 필요.
- 한계: float/절대·고정 배치/표/flex/grid 는 P10, 인라인 `position:relative` 미적용, 양방향 줄의 인라인 가로 margin/padding 미적용. 체크: P09-01~03(P09-09 는 그리기 검증 후).

### 2026-11-16 · P18-02/07(일부) · 오디오 디코더 WAV/AIFF/AU/FLAC/Ogg/Vorbis

- 검증 (JDK 6u45, `scripts/pkgtest.sh aud "media" "media/audio"`): 76 passed / 0 failed(기반 7 + 오디오 69). AUD-A 파일 sourcecheck 0 위반(mp3/webp 작업 중 파일 제외).
- `AudioDecoders` 등록부(시그니처 우선, MIME 힌트), `BlockPcmSource`, `ChannelMixer`, PCM 계열(WAV PCM/float/A-law/μ-law/IMA·MS ADPCM/EXTENSIBLE, AIFF/AIFC, AU), Ogg 컨테이너, FLAC(네이티브+Ogg), Vorbis I. 시험용 인코더로 입력 생성(외부 파일 없음).
- 한계: Vorbis 는 자체 인코더 스트림으로만 확인. P18-07 은 MP3/AAC/Opus/WebM(AUD-B) 후 체크.

### 2026-11-16 · P08-01~09 · CSS 엔진 검증

- 검증 (JDK 6u45, `scripts/pkgtest.sh css "css dom net" "css"`): 131 passed / 0 failed. sourcecheck 106 파일 0 위반. 92 소스 파일 약 22k 줄.
- 내용: 토크나이저/파서(오류 복구), 스타일시트 모델·@import(순환/깊이 8/개수 64 제한)·@media/@supports/@layer(파싱만)/@font-face/@keyframes, UA 기본 스타일시트(Java 문자열 상수), 선택자 3/4(:has, An+B)와 querySelector 엔진(P08-09),
  값 자료형(calc/min/max/clamp, 색), 176개 longhand + 29개 shorthand, 캐스케이드(origin/importance/specificity/order, 표현 속성, style 속성, var() 순환 검출), 미디어 질의, `StyleResolver` 증분 무효화, 퍼징(무작위 4000, 변형 600, 5000단 중첩, 20k 규칙).
- 한계(docs/progress/css.md): 34개 속성은 파싱·저장만(grid, border-image-*, filter, will-change, contain 등), 논리 속성·3D 변환·clip-path·mask·@container·@property·@page 미지원(진단 후 버림), `@layer` 순서 미적용, `ex/ch` 는 글꼴 크기 절반 근사.
- 호스트 후속: `StyleSheetLoader` 구현(P04 요청 계층), `CssEngine.loadStyleSheets()` 호출, `ElementState` 와 `stateChanged()` 연결(P11). 체크: P08-01~09(P08-07 은 기능 표에서 화면 확인 전까지 부분 구현으로 유지).

### 2026-11-16 · P07 통합 · HTML 파서를 페이지 파이프라인에 연결

- 변경 파일: `core/{HtmlPageDocument,ParsingPageLoader}`(신규), `core/NetworkPageLoader`(HTML 은 `EncodingSniffer` 로 디코딩), `SwingWebPanel`(`ParsingPageLoader` 로 감쌈), `tests/.../core/ParsingPageLoaderTest`(5건), ARCHITECTURE §14, FEATURE_MATRIX CNT-01~03.
- 검증 (JDK 6u45): `pkgtest integ "core net storage dom html css content download @root" "core net @root"` 187 passed / 1 skipped(기존), `ParsingPageLoaderTest` 5 passed. 수신 원문(CRLF, 한글)은 변하지 않는다.
- 후속(dom 쪽 요청, HTML 작업자): `Element` 에 이름 검증 없는 속성 추가 API(`@click` 등 XML Name 이 아닌 속성이 버려짐), `cloneNode` 의 template 내용 복제.

### 2026-11-16 · P07-01~06,08 / P17-03 / P11-09(변환기·관리자) · HTML 파서, Markdown 변환기, 다운로드 팝업 검증

- HTML (`scripts/pkgtest.sh html "html dom net" "html"`, JDK 6u45): 63 passed / 0 failed. 토큰화 70상태, 삽입 모드 23개, adoption agency, foster parenting, template, SVG/MathML, 조각 파싱, 인코딩 판정(BOM/HTTP/meta/UTF-8 추측), 문자 참조 2231항목(tools/entitygen, 출처·조회일 기록), 문서 모드, 직렬화, `ParserListener`(script/style/img/base/meta refresh 통지, document.write 삽입, 일시 정지/재개). 자체 사례 256건. 외부 적합성 자료는 사용하지 않았다.
  체크: P07-01~06, 08. 미체크: P07-07(옛 요소/속성의 표시 규칙은 UA 스타일시트(P08-02)/레이아웃(P09) 필요), P07-09(P07 마지막 항목이므로 P07-07 후).
  조정자 후속: `core.TextDecoder` 를 `EncodingSniffer/DecodedText` 로 교체하고 응답 파싱을 `HtmlParser`/`ParserListener` 로 연결(docs/progress/html.md 통합 순서), `dom.Element` 에 이름 검증 없는 속성 추가 API(`@click` 같은 속성), `cloneNode` 의 template 내용 복제.
- Markdown (`pkgtest cdl "content download html core net storage dom @root" "content download"`): 52 passed / 0 failed(Markdown + 다운로드). CommonMark 0.31.2 예제 652건 + GFM 22건 전수 통과, 안전 모드(원시 HTML 이스케이프, 위험 스킴 무력화), 입력 8 Mi 문자/중첩 100단계 제한. 체크: P17-03. P17-04 는 컴포넌트 연결 후.
- 다운로드(P11-09): `SwingDownloadHandler/DownloadTask/SwingDownloadDialog/SafeFiles`, 파일명 정리, `.part-<난수>` 임시 파일, 안전 교체, 취소/실패 정리, 동시 8건, 헤드리스 거부. 자동 검증 27건 통과. SwingWebPanel 설치(`protected createDownloadPopup(DownloadRequest, DownloadControl)`, clean 시 cancelAll, dispose)와 실제 팝업 수동 확인 후 P11-09 체크.

### 2026-11-16 · P18-01~03(일부) · 이미지 디코더 PNG/APNG/GIF/JPEG/BMP/ICO

- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/media/image/**`(PngDecoder 등, `ImageDecoders` 등록부), `media/MediaException`(CANCELLED 추가), `tests/src/org/duckdns/hjow/swingwebpanel/media/image/**`, `docs/progress/media-img-a.md`.
- 검증 (JDK 6u45, `scripts/pkgtest.sh mimg "media/image" "media/image"`): 75 passed / 0 failed(기반 7 + 이미지 68). 소스 검사는 IMG-A 파일 0 위반(WebP/오디오 작업 중 파일 제외).
- 결정/한계: 색 관리 없음(sRGB 로 간주), JPEG 산술 부호화/12비트/무손실은 UNSUPPORTED_FORMAT, 프레임 지연 10 ms 이하는 100 ms. 등록부는 webp/avif/svg 디코더를 리플렉션으로 찾는다.
- P18-01~03 은 WebP/AVIF/SVG/오디오가 끝날 때까지 미체크. `<img>` 연결, alt/URL 대체 표시는 P18-05/06.

### 2026-11-16 · P13-02~13, P14-11 체크 · JS 실행기 완료 검증

- 검증 (JDK 6u45, `scripts/pkgtest.sh jsall "js" "js"`): 580 passed / 0 failed / 0 skipped. ES2015 사례 목록 77/77(허용 목록 비어 있음), 적합성 표 510/510. `tools/sourcecheck` js/tests 0 위반.
- JS-CORE 완료 내용: eval/Function/strict 규칙(P13-11), 실행 제한·취소·RegExp 취소 훅 연결(P13-12), ES2015 체크리스트 시험(P13-13), 엄격 모드 꼬리 호출(14.6), `Function.prototype.caller/arguments` 제한 접근자, `NumberText`→`js.num`.
- 미체크: **P13-01** — 인터프리터/Environment/Binding/Ctx 는 분리되어 있으나 ES6 `Reference` 자료형이 별도 클래스가 아니다(식별자=환경+이름, 멤버=base+key 를 대입/갱신/delete/호출 코드 안에서 처리). 계획 문구와 문자 그대로 어긋나므로 `Reference` 클래스 도입을 후속 작업으로 남긴다.
- 알려진 차이(상세 docs/progress/js-core.md §9): 대입에서 프로퍼티 키 변환을 우변 평가 뒤에 수행(Chromium), `for (var x = 1 in o)` 는 SyntaxError, 반복자 `next` 를 단계마다 읽음. stringarray 의 `Support.poll()/checkStringLength` 는 `realm.getLimits()` 와 연결하는 것을 권장(BUILTIN-B 영역).

### 2026-11-16 · P12, P14-01~10 체크 · JS 값 모델/파서와 표준 내장 객체 통합 검증

- 통합 검증 (JDK 6u45, 작업 트리 전체 `scripts/pkgtest.sh jsall "js" "js"`): 577 passed / 0 failed / 0 skipped. 적합성 표 510개 항목 중 510 통과(`ConformanceTable`), ES2015 사례 목록 77건 중 76 통과 + 1건 허용 목록(꼬리 호출 14.6, 이유와 소유자 명시).
  `tools/sourcecheck --only forbidden` js/tests 0 위반(Rhino/javax.script/com.sun 없음 확인, P12-09).
- 체크: P12-01~09(JS-CORE 파서/값 모델), P14-01~10(BUILTIN-A~E 구현·검증). P14-11 은 꼬리 호출(14.6) 미구현 때문에 미체크(JS-CORE 에게 P13-12 와 함께 구현 요청 예정).
- 알려진 차이(상세는 docs/progress/es2015-conformance.md, js-builtins-*.md): ES2015 본문 403 으로 알고리즘은 지식 기반, 형식화 배열 길이 인수는 ToIndex(ES2017) 방식, WeakMap 값은 강한 참조(키만 약함), 정규식은 lookbehind/`\p{}`/이름 있는 그룹 미지원(ES2018),
  for-in 은 Proxy `enumerate` trap 을 호출하지 않음(ES2016 이후와 브라우저 동작).
- 남은 일: P13-11~13 (JS-CORE 재개 중), P13 체크박스는 JS-CORE 최종 보고 후 반영.

### 2026-11-16 · P19-03/04/06 · HTTP 캐시(조건부 요청과 304 처리)

- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/net/{HttpCache,CacheControl,HttpDates}.java`(신규), `net/ResourceFetcher`(setCache/getCache, 캐시 경유 GET, 안전하지 않은 메서드 무효화), `net/ResourceRequest`(`CacheMode.PREFER_CACHE`, `copy()`),
  `net/ResourceResponse`(`isFromCache/isRevalidated`), `core/CacheOwner`(신규), `core/NetworkPageLoader`(이력 이동=PREFER_CACHE, clearCache), `core/BrowserSession`(clean 시 캐시 삭제), `SwingWebPanel`(컴포넌트별 캐시 설치),
  `tests/src/org/duckdns/hjow/swingwebpanel/net/HttpCacheTest.java`(16건), `tests/src/org/duckdns/hjow/swingwebpanel/CacheIntegrationTest.java`(6건), `TestServersTest`(스레드 종료 대기 추가: CPU 부하에서 시간 의존 실패), 문서
- 검증 (JDK 6u45, `scripts/pkgtest.sh cache3 "net core storage dom @root" "core net @root"`): 187 passed / 0 failed / 1 skipped(JRE 6 의 Origin 헤더 제한, 기존). sourcecheck 0 위반.
  시험은 서버의 요청 수와 헤더로 일반 이동/새로고침/강제 새로고침/조건부 요청/이력 이동을 구분한다(P19-06).
- 설계: ARCHITECTURE §13. 리디렉션을 거친 응답, Vary: Cookie, 스트리밍 응답은 저장하지 않는다.
- 남은 일: P19-01/02/05/07/08/09/10(이력 복원 정보, 프래그먼트 이동, 디코딩·레이아웃 캐시, POST 정책, 진행률 통합, 연속 이동 검증).

### 2026-11-16 · P16-02~07, P16-09 · Web Storage Java 저장소와 임시 파일

- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/storage/*`(StorageLimits, StorageQuotaException, WebStorage, StorageListener(s), StorageEvent, StorageArea, SessionStorage, StorageFile, LocalStorageManager; StorageEntry 는 이전 커밋),
  `core/BrowserSession`(sessionStorage 필드, 공용 localStorage 관리자, clean/dispose 연결, 생성자 `(loader, listener, cookies, LocalStorageManager)`), `SwingWebPanel`(`getLocalStorage/getSessionStorage`),
  `tests/src/org/duckdns/hjow/swingwebpanel/storage/*`(StorageAreaTest, SessionStorageTest, LocalStorageManagerTest), `tests/src/org/duckdns/hjow/swingwebpanel/core/StorageIntegrationTest.java`, 문서(ARCHITECTURE §12, FEATURE_MATRIX, AGENTS 체크)
- 검증 (JDK 6u45, `scripts/pkgtest.sh storage "storage core net dom @root" "storage core @root"`): 100 passed / 0 failed / 0 skipped 수준(기반 시설 포함), sourcecheck 0 위반(storage/core/tests).
  시험 범위: 삽입 순서/key(i)/null·빈 문자열, 쿼터 정확성과 실패 시 불변, 컴포넌트별 sessionStorage 분리와 오리진 분리(스키마/포트 정규화), Java 조회 복사본, 통지(변경 주체 제외·값 동일 시 무통지·clear·리스너 제거),
  불투명 오리진 SecurityError, classpath 오리진 공유, 파일 형식(식별자/버전/UTF-8/CRC/잘림/제한), 영속성과 한글·서로게이트, 외부 삭제(빈 저장소로 처리·복구 안 함), 손상 파일 5종 초기화, 외부 교체 재로딩,
  동시 쓰기 6스레드×40항목 무손실, 임시/백업 파일 미잔존(JRE 6/Windows 의 rename 실패 경로 실행), 전체 쿼터, 다른 파일의 독립, clean 이 모든 컴포넌트의 공용 저장소를 비움, dispose 뒤 빈 조회, 약한 리스너/컴포넌트 수거 가능성.
- 설계 결정: 파일이 원본이며 매 연산 전에 (존재, 수정 시각, 길이)를 비교해 다시 읽는다(외부 삭제/교체/손상 대응). 쓰기 실패 시 메모리가 기준. 불투명 오리진은 SecurityError. 상세는 ARCHITECTURE §12.
- 남은 일(P15 연결 후): P16-01(JS `Storage` 객체와 named property 접근, `QuotaExceededError` 변환), P16-08(같은 오리진의 다른 문서로 DOM `storage` 이벤트 전달), P16-10(통합 최종 검증, 실제 별도 프로세스 분리는 파일 이름 고유성과 서로 다른 파일 독립 시험으로 대체 — 프로세스를 띄우지 않음).

### 2026-11-16 · P06-01~09 · DOM 자료 구조와 이벤트 모델

- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/dom/*`(36개 클래스: Node/Document/Element/Attr/CharacterData 계열, ElementCollection(s), ClassList, NamedAttributeMap, Event/CustomEvent/EventTarget/EventListener, MutationRecord/DomMutationListener, SelectorEngine/MarkupSupport/ElementFactory 훅, DomException/Namespaces),
  `tests/src/org/duckdns/hjow/swingwebpanel/dom/*`(TreeMutationTest, NodeApiTest, EventTest, DomTestSupport), 문서
- 검증 (JDK 6u45, `scripts/pkgtest.sh dom "dom" "dom"`): 42 passed / 0 failed (기반 시설 7건 포함, DOM 35건). 트리 연결, 삽입/교체/제거/이동, 순환·문서 규칙·조각, adopt/import/clone, 위치 비교, textContent(마크업 해석 없음), 문자 데이터(UTF-16),
  이름/네임스페이스 규칙(HTML 소문자·대문자 tagName, XML 대소문자 보존, NamespaceError), 속성/Attr/classList, 라이브 컬렉션(삽입·제거·속성 변경 뒤 갱신, 호환 모드), id 색인, 변경 기록(connected/detached),
  이벤트(캡처·대상·버블 순서와 단계, 취소/passive, stopPropagation, 중복/once/제거, 전달 중 추가·제거, 예외 격리, 재진입, 문서→window, 경로 고정) 포함. sourcecheck 0 위반.
- 설계: 선택자/마크업/HTML 요소 클래스는 `Document` 에 설치하는 서비스(없으면 NotSupportedError). 문서 변경 버전으로 라이브 컬렉션과 id 색인을 지연 갱신. 변경 리스너는 `connected` 플래그를 받아 분리된 하위 트리 변경에 레이아웃 작업을 하지 않게 한다.
- 알려진 한계: `Attr` 는 노드가 아님(DOM 표준 최신과 일치). Range/Selection/TreeWalker(P20-03), MutationObserver API(P15-11), `innerText`, 섀도 DOM 은 없음.

### 2026-11-16 · P04/P05 통합 · 네트워크 계층을 컴포넌트에 연결

- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/core/{NetworkPageLoader,ErrorPages,TextDecoder,DownloadRequiredException,DownloadHandler}.java`, `BrowserSession`(쿠키 저장소, 다운로드 핸들러, 다운로드 판정 처리),
  `SwingWebPanel`(표준 로더, `getCookies()`, 기본 `configureSsl` = `DefaultSslConfigurer`), `tests/src/org/duckdns/hjow/swingwebpanel/NetworkIntegrationTest.java`, `tests/resources/org/duckdns/hjow/swingwebpanel/testdata/hello.html`, 문서
- NET 작업자 결과 검증: `scripts/pkgtest.sh net "net" "net"` → 99 passed / 0 failed / 1 skipped(JRE 6u45 가 `Origin` 요청 헤더를 못 보냄, 시험이 이유를 출력), sourcecheck 0 위반.
- 통합 검증 (JDK 6u45, `scripts/pkgtest.sh core "core net storage @root" "core @root"`): 69 passed / 0 failed. 새 통합 사례 16건: HTTP 원문 보존(CRLF·한글), UA/추가 헤더 덮어쓰기와 전송, 404 서버 본문 유지/500 생성 페이지,
  리디렉션 최종 URL 한 건, 쿠키 저장·전송·clean 유지·dispose 삭제·컴포넌트 분리, `refresh(true)` 의 `Cache-Control: no-cache`, 금지 스킴이 네트워크에 닿지 않음, 연결 실패 오류 페이지,
  재정의한 `configureSsl` 호출과 HTTPS 성공, HTTPS 실패 시 평문 HTTP 재시도(최종 URL 이 http), FTP 목록/파일/404, classpath 페이지/404, 다운로드 판정(첨부/표시 불가 MIME → 페이지·URL·이력 불변, 핸들러 전달),
  이미지 주소의 생성 문서, Content-Type 없는 응답의 판별과 meta 문자셋(EUC-KR).
- JRE 6 확인된 제한(NET 작업자 실측): 지원 프로토콜 `[SSLv2Hello, SSLv3, TLSv1]`, 기본 선택 TLSv1, TLS 1.2/SNI 없음 → 요즘 HTTPS 사이트는 실패하고 http 재시도로 이어질 수 있다(P22-07 기록 필요).
- 설계 메모: `TextDecoder` 는 임시 해독기(P07-01 에서 정식 인코딩 판정으로 교체). `ErrorPages` 는 임시 생성기(P17-06 에서 content 계층으로 교체). 다운로드 팝업과 저장은 P11-09, 캐시는 P19.
- 미해결: 연결 수립 중 취소는 연결 제한 시간(15초)까지 걸릴 수 있음. 공개 접미사 목록이 없어 쿠키 Domain `co.kr` 거부 불가. FTP 디렉터리 링크 클릭 이동은 렌더러 후 재검증.

### 2026-11-16 · P03-01~07 · 컴포넌트 골격과 페이지 상태 관리

- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/SwingWebPanel.java`, `src/org/duckdns/hjow/swingwebpanel/core/*`(BrowserSession, NavigationController, History, PageSnapshot 등 15개), `src/org/duckdns/hjow/swingwebpanel/storage/StorageEntry.java`,
  `tests/src/org/duckdns/hjow/swingwebpanel/core/*`(FakeLoader, TestSupport, BrowserSessionTest, HistoryNavigationTest, LifecycleTest, HyperlinkDispatcherTest), `tests/src/org/duckdns/hjow/swingwebpanel/SwingWebPanelTest.java`,
  `tests/src/org/duckdns/hjow/swingwebpanel/testing/selftest/JarContentTest.java`, `examples/src/.../BlankPageExample.java`, `examples/run.sh`, `scripts/pkgtest.sh`(-sourcepath 로 필요한 클래스만 컴파일, `@root`), `test.sh/bat`(-Dswp.jar), 문서
- 설계: 읽기는 이동마다 독립 데몬 스레드(취소를 무시하는 로더가 새 이동을 막지 않음), 확정은 컴포넌트당 단일 문서 스레드에서 요청 번호 확인 후 수행, 통지는 EDT. `PageLoader` 전략 인터페이스로 이후 단계(P04 네트워크, P07~P09 파싱·렌더링)를 연결한다.
  기본 로더는 거짓 성공 없이 네트워크 주소에 501 "미구현" 오류 페이지를 준다. `getCookies()`/`getLocalStorage()`/`getSessionStorage()` 는 연결 전까지 빈 값이며 기능 표에는 미구현으로 둔다.
- 검증 (JDK 6u45, `scripts/pkgtest.sh core "core storage @root" "core @root"`): 53건 통과/0 실패/0 건너뜀 (기반 시설 7건 포함). 오래된 응답 폐기와 자원 해제, 이력 경계/분기/리디렉션/복원, 새로고침, clean/dispose 반복,
  8 스레드 동시 호출 일관성, 100개 컴포넌트 반복 생성·폐기 후 스레드 정리, EDT 호출, 속성 통지 순서/EDT, Scrollable 정책, 리스너 제거를 포함한다.
  `examples/run.sh BlankPageExample --smoke` 를 JDK 6 에서 실행: `smoke OK page=about:text status=Done progress=100 histories=[about:blank, about:text]` (실제 Swing 창 1초 표시).
- 실행 중 발견·수정한 결함: clean/dispose 시 현재 페이지 문서가 두 번 해제됨, 단일 읽기 스레드가 취소 무시 로더에 막힘(→ 이동별 스레드로 변경), 시험의 전역 카운터 경합.
- 주의: 머신 CPU 가 다른 작업자들 때문에 100% 가 될 때가 있어 시험 시간이 크게 흔들린다. 시간 가정이 있는 시험(스레드 정리 대기 등)은 넉넉한 제한을 둔다.
- 미완료: P02-06(NET 작업자 확인 후 체크), P02-08/09(저장소 전체 sourcecheck 0 위반일 때 체크).

### 2026-11-16 · P02-07/08/09 · 소스 규칙 검사 도구 sourcecheck

- 변경 파일: `tools/sourcecheck/**`(Java 6 소스 7개, build/run sh·bat, AGENTS.md, README.md), `tests/src/org/duckdns/hjow/swingwebpanel/testing/TestRunner.java`·`tools/webfetch/src/webfetch/WebFetch.java`(반복문 한국어 주석 추가), `docs/ARCHITECTURE.md`(도구 목록)
- 검증 결과 (JDK 6u45): `tools/sourcecheck/build.sh` 성공, `run.sh --selftest` → 불량 샘플(Javadoc 누락/영어만, 반복문 주석 누락, javax.script, Runtime.exec, native, 람다, 다이아몬드, FileReader, BOM, 깨진 UTF-8, sh CRLF, bat UTF-8, bat LF, .dll)이 모두 보고되고 종료 코드 1, 양호 샘플 종료 코드 0.
  내 소유 파일(tools, scripts, docs, AGENTS.md, 루트 스크립트, `tests/src/org/duckdns/hjow/swingwebpanel/testing`의 내 클래스, `src/org/duckdns/hjow/swingwebpanel/core`)에 대한 전체 검사: 0 위반.
- 발견한 환경 문제: 이 개발 환경의 Write/Edit/Bash 입력은 `\uXXXX`(서로게이트 제외)를 **실제 문자로 바꾸어 저장**한다(예: `﻿`, ` `, `\u000A`). Java 문자열에 이스케이프 텍스트를
  넣으려면 `"\\u0041"` 로 쓰고, 특수 문자는 `(char) 0x2028` 로 쓴다. `invisible-char`/`replacement-char` 규칙으로 감지한다. 작업자 3명에게 통보했다.
- 아직 확인하지 못한 것: 작업자가 작성 중인 소스(`js`, `net`, 시험 서버)는 검사기 기준을 맞추도록 안내했으나 최종 검사는 각 작업 완료 시 수행한다.

### 2026-11-16 · P02-01/04/05 · 패키지 구조, 빌드/테스트 스크립트, 자체 검증 실행기

- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/**/package-info.java`(13개), `src/org/duckdns/hjow/swingwebpanel/core/Version.java`, `scripts/jdk6.sh|bat`,
  `build.sh|bat`, `test.sh|bat`, `tests/src/org/duckdns/hjow/swingwebpanel/testing/*`(Check, TestRunner 등), `tests/src/.../selftest/*`, `examples/`, `tests/resources/`
- 검증 결과 (JDK 6u45 `javac 1.6.0_45`, 2026-11-16):
  - `./test.sh` → 빌드 OK, 6 passed / 0 failed, 의도적 실패 스위트 종료 코드 1 확인, 스크립트 종료 코드 0.
  - `test.bat` (cmd) → 동일하게 통과. `Version.class` major version 50 (Java 6).
  - `build/swingwebpanel.jar` 에는 `org/duckdns/hjow/swingwebpanel/**` 제품 클래스만 있고 테스트/도구 클래스 없음.
  - `JAVA6_HOME` 이 JDK 8 이거나 없는 경로이면 sh/bat 모두 종료 코드 1 (조용한 대체 없음). `SWP_ALLOW_NON_JDK6=1` 일 때만 경고와 함께 사용.
- 참고: JDK 8 로 `-source 1.6` 컴파일 시 `bootstrap class path not set` 경고가 난다. 다른 JDK 로 Java 6 API 호환을 검사하려면
  `-bootclasspath <JDK6>/jre/lib/rt.jar` 가 필요하다(target 옵션만으로 JRE 6 API 호환이 증명되지 않는다). JDK 25 는 `-source 6` 자체를 지원하지 않는다.
- 미완료: P02-06(시험 서버), P02-07/08/09(인코딩/금지 API/Javadoc 검사 도구). 

### 2026-11-16 · P00 시작 · 웹 조회 도구와 규격 판본 확인

- 변경 파일: `.gitignore`, `.gitattributes`, `tools/webfetch/**`, `docs/*.md`
- 실행한 검증:
  - `tools\webfetch\build.bat` (JDK 6u45) → 종료 코드 0, `WebFetch.class` major version 50 확인.
  - JRE 1.6.0_45 로 `https://www.w3.org/TR/2014/REC-html5-20141028/` 조회 →
    `SSLHandshakeException: Received fatal alert: handshake_failure`, 종료 코드 2. 평문 HTTP(`http://example.com/`)는 성공.
  - JRE Zulu25.28+85-CA 로 같은 URL 조회 → 상태 200 (HTTPS 조회는 새 JRE 로 실행).
  - 조회한 규격(조회일 2026-11-16, 새 JRE 사용):
    - W3C HTML5 Recommendation 2014-10-28 (`https://www.w3.org/TR/2014/REC-html5-20141028/`) 200.
    - CSS Snapshot 2026, W3C Group Note 2026-06-22 (`https://www.w3.org/TR/2026/NOTE-css-2026-20260622/`) 200.
    - DOM Standard, Living Standard — Last Updated 15 November 2026 (`https://dom.spec.whatwg.org/`;
      `https://www.w3.org/TR/dom/` 은 이곳으로 리디렉션됨) 200.
    - ECMA-262 6th edition: HTTP 403 (위 미해결 사항 참조).
- 확인하지 못한 것: ECMA-262 6.0 본문, Chromium 소스 열람 도구 동작(필요 시점에 확인).
