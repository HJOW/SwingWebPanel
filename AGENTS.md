# 프로젝트 목표

java Swing 에서 사용할 수 있는 웹브라우저 컴포넌트 개발.
외부 라이브러리 의존성 없이 구현.

# 목표 - 지원 기능

* HTML5 지원 (이전 버전 모두 지원)
* CSS3, ES2026 지원 (DOM 객체 및 DOM 이벤트 모두 지원해야 함, 이전 버전의 ES 호환 필요), 하이퍼링크 지원
  자바스크립트 엔진 및 DOM 및 렌더링 엔진의 자체 개발이 필요
  ES 2026 규격은 docs/es2026 내 문서 참고
* HTML 웹 표준에서 지원하는 이미지, 오디오 모두 지원, 비디오는 일딘 미지원 (해당 위치에 미지원 비디오임을 표시해야 함)
* 이미지, 오디오 URL 접근 실패 시 해당 위치에 미지원 URL 표시 및 alt 태그 존재 시 alt 내용 출력
* 메소드 지원
- setUserAgent(String agent)
  User-Agent 값 지정 (이 메소드를 호출하지 않은 경우 "swingwebpanel/1.0" 사용)
- getUserAgent()
  User-Agent 값 반환
- setPage(String url)
  해당 URL 페이지 호출, URL스키마 "classpath://" 사용 시 클래스패스 경로 내 html 파일 호출
  "about:blank" 입력 시 빈 페이지 호출
- getPage()
  현재의 URL 반환. setContent 메소드로 컨텐츠를 임의로 입력한 경우 "about:text" 반환
- setJavascriptEnabled(boolean enabled)
  자바스크립트 활성화 여부 지정
- back()
  히스토리 1칸 뒤로가기 호출 (더 이상 뒤로 갈 페이지가 없는 경우 아무 동작 안 함)
- back(int n)
  히스토리 n칸 뒤로가기 호출 (더 이상 뒤로 갈 페이지가 없는 경우 아무 동작 안 함)
- forward()
  히스토리 1칸 앞으로 가기 호출 (더 이상 앞으로 갈 페이지가 없는 경우 아무 동작 안 함)
- forward(int n)
  히스토리 n칸 앞으로 가기 호출 (더 이상 앞으로 갈 페이지가 없는 경우 아무 동작 안 함)
- getHistories()
  페이지 이동 히스토리를 Vector 에 담아 반환 (원소는 String)
- refresh()
  페이지 새로 고침
- refresh(boolean force)
  페이지 새로 고침, force 를 true 입력 시 캐싱된 컨텐츠도 강제로 새로 고침
- getContent()
  웹브라우저 컴포넌트에 현재 출력된 컨텐츠 텍스트 반환 (String)
  화면에 표시되는 내용이 아닌 브라우저가 받는 컨텐츠 내용을 반환해야 함. (웹페이지 출력 중인 경우 수신받은 HTML문을 반환)
- setContent(String content, String contentType)
  웹브라우저 컴포넌트에 컨텐츠 출력 (contentType 에는 Mime Type 입력)
  text/html, text/plain, text/markdown, application/xhtml+xml 지원
- getStatusText()
  상태바 메시지를 문자열로 반환
- getProgress()
  상태바의 진행상태바 값을 정수로 반환, 단 indeterminates 인 경우 -1 반환
- addHyperlinkListener(HyperlinkListener listener)
  하이퍼링크 이벤트 리스너 등록, javax.swing.event.HyperlinkListener 타입 이용
- removeHyperlinkListener(HyperlinkListener listener)
  하이퍼링크 이벤트 리스너 제거
- removeHyperlinkEvents()
  이 객체에 연결된 하이퍼링크 이벤트 모두 제거
- clean()
  "about:blank" 호출 (페이지 비우기) 및 캐시 전부 삭제
  Storage API 로 저장된 컨텐츠도 모두 삭제
- dispose()
  위 clean 메소드 호출 후 연결된 하이퍼링크 이벤트를 비롯해 이 객체에 연결된 모든 이벤트 제거
- addHeaders(Map headers)
  추가 헤더값 지정 (서버에 요청 시 헤더 포함)
  Map 은 java.util.Map 타입이며 키와 값 모두 String 타입으로 지정
  이 메소드 사용 시, 이미 추가된 헤더값을 삭제하지 않고 덮어쓰기로 적용해야 함
  User Agent 값은 이 메소드로 변경할 수 없음
- getHeaders()
  추가된 헤더값 정보를 Map 타입으로 반환 (키와 값 모두 String)
  User Agent 값은 이 메소드로 조회할 수 없음
- getCookies()
  현재 쿠키값 전부 하나의 문자열로 반환
- getLocalStorage()
  Storage API 의 localStorage 로 저장된 컨텐츠를 Collection 으로 반환
- getSessionStorage()
  Storage API 의 sessionStorage 로 저장된 컨텐츠를 Collection 으로 반환
* JScrollPane 을 통한 스크롤 지원
* Storage API 지원
- sessionStorage 는 이 웹브라우저 컴포넌트 하나 당 개별로 메모리 상에 저장
  이 컴포넌트 사용 중지 (GC될 때) 시 같이 사라져야 함
- localStorage 는 프로세스 당 개별로 저장 (임시 파일 경로상에 저장)
  따로 삭제할 필요는 없음 (OS의 임시 파일 삭제 기능으로 삭제되도록)
  외부에서 삭제되더라도 별다른 조치 하지 않음 
  단, 이 객체 사용으로 localStorage 를 불러와야 할 때 파싱 실패 시에는 초기화 후 사용해야 함
* 404, 500 등 오류 코드 발생을 대비하여 오류 페이지 구현
* ftp:// 프로토콜 호출 시 해당 주소로 ftp 접근 (anonymous 사용), 접근 실패 시에는 404 처리, 성공 시에는 파일 목록 출력, 디렉토리는 하이퍼링크로 출력하여 클릭 시 해당 디렉토리로 접근할 수 있어야 함. 디렉토리 접근 시 최상위에 "../" 하이퍼링크 출력하여 상위 디렉토리로 이동도 지원해야 함
* http://, https:// 및 ftp:// 스키마만 지원, mailto 및 기타 스키마는 미지원
* SSL은 현재의 JRE 에서 지원하는 버전을 사용하며, 단 SSL 처리 메소드를 별도로 분리 및 protected 로 지정하여 오버라이드할 수 있도록 구현. https 접근 실패 시 http 접근 시도.
* 3D 가속 기능 미지원, webgl 및 webgpu 미지원, 2D 캔버스는 지원.
* 링크 클릭 시 파일 다운로드가 필요한 경우를 위해 Swing 기반 다운로드 팝업이 구현되어야 한다.

# 주의사항

* Java 6 이상 버전 지원해야 하며, JRE 6 에서 동작해야 함.
* com.sun 관련 패키지는 사용하지 말 것
* 클래스, 메소드, 필드, 상수에 javadoc 주석이 영어와 한국어로 달려야 함. 
  이 때, 해당 객체의 설명과 사용 방법이 주 내용이 되어야 함.
* 반복문 및 중요 분기문에 한국어 주석으로 설명이 달려야 함. (이 때는 영어 주석은 달 필요 없음)
* java 소스, md 문서, sh 파일은 UTF-8 캐릭터셋 사용, bat 파일은 MSWIN949 사용
* 네이티브 코드 (C/C++ 등) 는 사용하지 않는다.
* 작업 단위마다 git 에 commit 을 수행한다. (push 는 사용자에게 맡긴다.)
  commit 메시지는 한 줄로, 앞에 YYYYMMDD 형식 날짜를 붙이고 3자리 번호를 붙인 후 그 뒤 한 줄로 요약 내용을 적는다.
* 웹 브라우징 등 특수 기능이 필요한 경우, tools 디렉토리 하위에 도구를 만들고 활용한다.
  이 때 도구는 Java 6 언어로 작성한다.
  도구 작성 시 해당 디렉토리에 AGENTS.md 및 README.md 문서를 작성해 도구 사용법을 설명해야 한다.
  AGENTS.md 는 다른 AI를 위해, README.md 는 사용자를 위해 작성한다. (README.md 는 한국어로 작성한다.)
* 이 문서에서 "프로젝트 목표", "목표 - 지원 기능", "주의사항" 단락은 수정하지 않는다.

# 참고 자료

* Chromium Project - https://github.com/chromium/chromium.git
  (소스 코드 전체 Clone 시 중간에 접속이 끊긴다. 웹 브라우저로 접속하여 내용을 봐야 할 것 같다.)

* JDK 1.6 (Java 6 지원) 설치 경로는 보편적으로 "C:\Program Files\Java\jdk1.6.0_45" 에 설치된 경우가 많다.

# 개발 계획

이 계획은 외부 라이브러리, Rhino 등 기존 엔진, 네이티브 코드(C/C++, JNI 등) 없이 JavaScript, DOM, HTML 파서, CSS 처리기와 렌더링 엔진을 순수 Java로 직접 개발하는 것을 전제로 한다. Java 6은 이 엔진을 실행하는 Java 코드의 호환성 기준이며, JavaScript의 지원 목표는 **ES2026(ECMA-262 제17판, June 2026) 및 이전 버전의 ECMAScript(ES1~ES5.1, ES2015~ES2025) 하위 호환**이다(2026-11-25 상단 목표를 ES6에서 ES2026으로 상향). 개발은 1차 기반 단계(P12~P14: ES2015/ES6)와 2차 확장 단계(P28-01~05: ES2016~ES2022) 위에 3차 완성 단계(P29: ES2023~ES2026 전체 규격·잔여 항목·이전 판본 하위 호환)를 쌓는 순서로 진행한다. 단계별 부분 구현은 허용하지만, 부분 구현을 HTML5/CSS3/ES2026 전체 지원으로 표시해서는 안 된다.

## 계획 사용 방법과 완료 표시

작업 번호 `P00-01` 등의 번호를 유지한다. `[ ]`는 미완료이며, 구현과 해당 검증이 모두 끝났을 때만 `[x]`로 바꾼다. 계획을 읽거나 클래스의 빈 틀만 작성한 상태는 완료가 아니다. 단계의 마지막 완료 항목은 그 단계의 모든 작업과 검증이 끝나야 체크한다.

후속 AI는 매 작업 시작 시 이 문서와 `docs/PROGRESS.md`를 읽고, 선행 단계가 완료된 첫 번째 미완료 항목을 선택한다. 해당 항목에서 사용하는 규격과 관련 클래스, 기존 검증 자료를 읽은 뒤 작은 단위로 구현한다. 작업 후 변경한 파일, 검증 명령과 결과, 아직 실패하는 사례, 다음 작업 번호를 `docs/PROGRESS.md`에 기록하고, 그 작업 단위를 아래 공통 작업 규칙에 따라 git에 commit한다. 검증 환경이 없으면 그 항목은 미완료로 남기고 필요한 환경을 기록한다.

한 항목이 여러 클래스나 많은 규격 기능으로 나뉘면 구현 전에 `P13-09-a` 같은 하위 번호로 체크리스트를 추가한다. 각 하위 항목에도 입력 사례, 기대 결과, 구현 파일과 완료 조건을 적는다. 상위 항목은 모든 하위 항목을 검증한 뒤 체크한다. 클래스 이름과 패키지는 이 계획의 기본안이며, 변경할 때는 구조 문서와 기능 표의 경로를 함께 갱신한다.

검증 환경이 없거나 요구사항 충돌로 진행할 수 없는 항목은 필요한 조건을 기록하고 미완료로 유지한다. 같은 실패를 반복하지 말고, 그 항목에 의존하지 않으며 선행 조건이 충족된 다음 작업을 진행한다. 일부 항목을 먼저 구현해도 단계/전체 완료 조건은 생략하지 않는다.

상단 요구사항은 유지한다. 이 계획에서 정한 기본 동작은 문서에 정의되지 않은 부분에 대한 구현 계약이다. 요구사항과 실제 구현이 충돌하면 해당 기능을 완료 처리하거나 요구사항을 임의로 축소하지 말고, 충돌 원인과 수정 방안을 인계 문서에 기록한다.

### 공통 작업 규칙

상단 "주의사항"과 "참고 자료"를 작업 절차에 적용하는 방법이다. 모든 단계의 모든 항목에 적용한다.

**git commit**

- 작업 번호(또는 하위 번호) 하나를 끝냈을 때, 검증 가능한 중간 상태에 도달했을 때, 문서만 고쳤을 때를 각각 하나의 작업 단위로 보고 `docs/PROGRESS.md`를 갱신한 뒤 commit한다. push는 하지 않고 사용자에게 맡긴다.
- commit 메시지는 본문이나 추가 줄 없이 한 줄로 쓴다. `YYYYMMDD` 날짜, 3자리 번호, 요약 순서로 적는다. 번호는 같은 날짜 안에서 `001`부터 1씩 늘리며, 그날의 마지막 번호는 `git log --oneline`으로 확인한다. 예: `20261116-001 P00-01 작업 추적 문서 작성`.
- 요약에는 가능하면 작업 번호를 넣는다. 검증이 실패한 상태를 commit할 때는 PROGRESS의 미해결 사항에 기록하고 해당 항목을 완료로 체크하지 않는다.

**네이티브 코드 금지**

- C/C++ 등의 네이티브 소스, JNI(`native` 메서드, `System.loadLibrary`/`System.load`), JNA 같은 네이티브 연결 라이브러리, `.dll`/`.so`/`.dylib` 파일을 제품, 테스트, 도구 어디에도 넣지 않는다.
- 미디어 디코더, 글꼴 처리, 네트워크, 렌더링 중 JRE 표준 공개 API가 제공하지 않는 부분은 순수 Java로 직접 구현한다. Java2D, ImageIO, Java Sound, JSSE 등 JRE 표준 공개 API의 호출은 허용하며, 그 API를 구현하는 JRE 내부는 이 금지의 대상이 아니다.
- 제품 코드는 `Runtime.exec`/`ProcessBuilder`로 OS 프로그램, 외부 브라우저, 코덱 실행 파일을 호출하여 기능을 대신하지 않는다.

**도구(`tools/`)**

- 웹 페이지와 규격 문서 조회, Chromium 소스 열람, 규격 자료로부터 표 생성(예: P07-04 문자 참조 표), 인코딩/금지 API/Javadoc 검사(예: P02-07, P02-09, P22-08) 등 제품 기능이 아닌 특수 기능이 필요하면 `tools/<도구 이름>/` 아래에 도구를 만들어 사용한다. 규격 URL과 판본을 확인해야 하는 P00-02~P00-07에 웹 조회가 필요한데 도구가 없으면 먼저 웹 조회 도구를 작성하고, 그 작업을 PROGRESS에 기록한다.
- 도구는 Java 6 언어로 작성하여 JDK 6의 `javac -source 1.6 -target 1.6`으로 컴파일되게 한다. 외부 라이브러리, `com.sun.*`/`sun.*`, 네이티브 코드를 사용하지 않으며, Javadoc, 한국어 주석, 파일 인코딩 규칙도 제품 코드와 같게 적용한다.
- 각 도구 디렉터리에 `AGENTS.md`와 `README.md`를 둔다. `AGENTS.md`는 다른 AI를 위해 목적, 빌드/실행 명령, 입력/출력 형식, 종료 코드, 제약, 실제로 실행해 본 JRE를 적는다. `README.md`는 사용자를 위해 한국어로 목적, 사용법, 실행 예시를 적는다. 도구 동작을 바꾸면 두 문서를 함께 갱신한다.
- 새 도구를 만들기 전에 `tools/` 아래 기존 도구의 `AGENTS.md`를 읽고, 가능하면 기존 도구를 재사용하거나 확장한다. 도구 목록과 용도는 `docs/ARCHITECTURE.md`에 기록한다.
- 도구 소스는 `tools/<도구 이름>/src`에, 빌드/실행 스크립트는 같은 디렉터리에 sh(UTF-8)와 bat(MSWIN949)로 둔다. 도구 클래스는 제품 소스(`src`)와 제품 JAR에 포함하지 않으며, 제품과 테스트 코드는 도구 클래스에 의존하지 않는다.
- 도구로 얻은 규격 발췌와 참고 자료에는 원본 URL, 판본 또는 리비전, 조회 날짜를 함께 기록한다. 도구 출력은 보조 자료이며 제품 기능의 검증을 대신하지 않는다.
- 공개 업데이트의 마지막 판인 JDK 6u45의 JSSE는 TLS 1.2와 클라이언트 SNI를 지원하지 않는 것으로 알려져 있어 현대 HTTPS 사이트 조회가 실패할 수 있다. 첫 웹 조회 도구 작성 시 이를 실제로 확인한다. 실패하면 도구 소스는 Java 6 문법/API로 유지한 채 더 새 JRE에서 실행할 수 있으며, 실행한 JRE 버전을 도구 문서와 PROGRESS에 기록한다.

**참고 자료 활용**

- 기능 판정의 기준은 P00에서 고정한 규격 판본이다. Chromium은 실제 브라우저의 구조, 오류 복구, 경계 사례를 확인하는 참고 구현으로 사용한다. Chromium 동작이 기준 판본과 다르면 그 차이와 선택한 동작을 기능 표에 기록한다.
- Chromium 저장소는 전체를 clone하지 않는다. 필요한 파일만 `tools/`의 웹 조회 도구로 읽는다. 예: GitHub 미러 원문 `https://raw.githubusercontent.com/chromium/chromium/main/<경로>`, 또는 Gitiles `https://chromium.googlesource.com/chromium/src/+/refs/heads/main/<경로>?format=TEXT`(Base64 본문).
- 참고 위치 예: HTML 파서 `third_party/blink/renderer/core/html/parser`, DOM `third_party/blink/renderer/core/dom`, CSS `third_party/blink/renderer/core/css`, 레이아웃 `third_party/blink/renderer/core/layout`. 경로는 리비전에 따라 바뀔 수 있으므로 조회 시 확인한다. JavaScript 엔진 V8은 별도 저장소이므로 ECMAScript(ES2026 및 이전 판본 호환) 구현은 ECMA-262 본문을 우선 기준으로 한다. 오프라인 규격 자료는 저장소의 `docs/es2026/`(ECMA-262 제17판, June 2026 HTML 사본)을 주 정본으로 삼고, 판본 간 변천과 이전 판본 대조에는 `docs/ECMA-262_16th_edition_june_2025.pdf`(제16판, ES2025)와 `docs/es2022/`(제13판, ES2022)를 함께 참고한다.
- 참고한 Chromium 파일 경로와 리비전(커밋 해시 또는 조회 날짜)을 해당 기능의 기능 표나 설계 문서에 남긴다. Chromium 코드의 구조를 Java로 옮겨 적을 정도로 차용하면 Chromium 라이선스(BSD 계열) 고지를 보존하고 해당 Java 파일을 기록한다. Chromium의 C++ 코드를 빌드하거나 연결하지 않는다.
- JDK 6은 기본적으로 `C:\Program Files\Java\jdk1.6.0_45`에서 찾는다. 빌드/검증 스크립트는 환경 변수(예: `JAVA6_HOME`)로 경로를 바꿀 수 있게 하고, JDK 6을 찾지 못하면 다른 JDK로 조용히 대체하지 말고 오류로 종료한다. 다른 JDK는 명시적으로 지정했을 때만 사용하며, 그 실행 결과를 JRE 6 검증으로 기록하지 않는다.

### P00. 작업 추적과 표준 범위 정리

선행 작업: 없음. 산출물: `docs/PROGRESS.md`, `docs/FEATURE_MATRIX.md`, `docs/ARCHITECTURE.md`, `docs/API_CONTRACT.md`.

- [x] P00-01. 위 네 문서를 UTF-8로 만들고, 작업을 시작한 날짜와 현재 소스 상태를 기록한다. 진행 문서에는 `현재 작업 번호`, `완료한 내용`, `변경 파일`, `검증 결과`, `미해결 사항`, `다음 작업 번호` 항목을 둔다.
- [x] P00-02. 기능 표의 각 행에 `기능 ID`, `요구사항`, `규격 URL 및 판본`, `구현 클래스`, `검증 사례`, `지원 상태`, `제약`을 기록한다. 지원 상태는 미구현/부분 구현/검증 완료로 구분한다. 상단의 모든 공개 메서드와 프로토콜, 미디어, Storage 요구사항을 빠짐없이 행으로 옮긴다.
- [x] P00-03. HTML5의 출발 기준을 [W3C HTML5 2014 권고안](https://www.w3.org/TR/2014/REC-html5-20141028/)으로 기록한다. HTML 이전 판본의 요소, 속성, 문자 참조, 프레임, 문서 모드와 폐기된 기능을 별도 목록으로 작성한다. 이전 버전 지원을 오래된 태그 몇 개의 표시만으로 완료 처리하지 않는다.
- [x] P00-04. ES6 기준을 [ECMA-262 제6판, ECMAScript 2015](https://262.ecma-international.org/6.0/)로 기록한다. 언어 문법, 실행 의미, 표준 내장 객체, 모듈을 별도 영역으로 나눈다. ES2015 이후 기능은 별도 확장 목록에 기록하며, ES6의 미구현 기능을 확장 기능으로 분류하지 않는다. (2026-11-25 목표 상향 갱신: 최종 JavaScript 기준을 **ECMA-262 제17판, ECMAScript 2026(June 2026)** 과 이전 ES 판본 하위 호환으로 상향했다. 본문 정본은 저장소의 `docs/es2026/` 사본이며 `docs/ECMA-262_16th_edition_june_2025.pdf`·`docs/es2022/`를 대조 자료로 함께 사용한다. P12~P14의 ES2015 기반과 P28의 ES2016~ES2022 구현 위에 P29에서 ES2026 전체 범위와 이전 판본 호환을 추적·완성하며, ES2026 이하의 미구현 기능을 범위 밖 확장으로 분류하지 않는다.)
- [x] P00-05. CSS3는 여러 모듈로 구성되므로 [W3C CSS 규격 목록](https://www.w3.org/TR/CSS/)을 확인하여 모듈 이름과 정확한 판본을 기능 표에 고정한다. CSS 2.1 기본 레이아웃과 선택자, 색상, 배경, 경계선, 글꼴, 텍스트, 변환, 애니메이션, 미디어 질의 등의 모듈을 각각 추적한다. 작업 중 목록이 바뀌면 이유와 영향도 함께 기록한다. 변환은 2D 변환만 다루는 CSS Transforms Level 1을 기본 대상 판본으로 하고, 3D 변환(Level 2의 perspective, transform-style, backface-visibility, 3D 변환 함수)은 상단의 3D 미지원 요구에 따라 명시적 예외로 기록하는 것을 기본으로 한다.
- [x] P00-06. DOM 노드/조회/변경, DOM 이벤트, HTML 요소별 인터페이스, 브라우저 API를 분리한다. HTML 규격에서 다른 규격을 참조하는 기능은 참조 규격과 판본도 기록한다. 단순히 태그가 화면에 보인다는 이유로 관련 DOM API를 지원 완료로 표시하지 않는다.
- [x] P00-07. 이미지와 오디오의 컨테이너, 코덱, MIME 타입을 각각 조사하여 지원 대상 표를 작성한다. JRE가 기본 제공하는 형식과 자체 디코더가 필요한 형식을 구분한다. HTML 표준이 모든 코덱의 지원을 강제하는 것으로 가정하지 말고, 프로젝트의 '모두 지원' 목표를 판정할 대상 목록을 명시한다.
- [x] P00-08. 비디오는 표시 영역에 미지원 문구를 출력하는 명시적 예외로 기록한다. 3D 가속 기능, WebGL, WebGPU도 명시적 미지원 예외로 기록하고, 이를 요청하는 페이지에 대한 동작(컨텍스트 요청의 null 반환, 관련 전역 객체 미노출)을 정한다. 2D 캔버스(Canvas 2D)는 예외가 아니라 지원 대상으로 기능 표에 넣는다. 외부 플러그인이 필요한 과거 HTML 기능, 허용되지 않은 URL 스키마 등은 상단 제약과 연결하여 처리 정책과 남은 호환성 차이를 기록한다.
- [x] P00-09. Rhino, Nashorn, GraalJS, JRE의 기존 JavaScript 제공자, JavaFX WebView, JCEF, OS 브라우저 실행을 엔진 대체 수단으로 사용하지 않는다고 설계 문서에 명시한다. `javax.script.ScriptEngineManager`를 통한 내장 Rhino의 우회 사용도 금지한다. C/C++ 등 네이티브 코드, JNI/JNA, 네이티브 라이브러리를 사용하지 않는다는 원칙과 JRE 표준 공개 API의 허용 범위도 함께 명시한다.
- [x] P00-10. 전체 목표를 축소하지 않고 중간 실행 가능한 단계를 구분했는지 검토한다. P00 산출물과 요구사항 대응표가 완성되면 이 단계를 완료 처리한다.

### P01. 공개 API와 기본 동작 계약 확정

선행 작업: P00. 산출물: `docs/API_CONTRACT.md`의 메서드별 입력, 반환값, 예외, 상태 변화, 검증 사례.

- [x] P01-01. (2026-11-19 사용자 요청으로 주 패키지를 `swingwebpanel` 에서 `org.duckdns.hjow.swingwebpanel` 로 변경했다. 소스 경로는 `src/org/duckdns/hjow/swingwebpanel`, 클래스패스 자료 경로는 `org/duckdns/hjow/swingwebpanel/` 이다. User-Agent 기본값 `swingwebpanel/1.0`, JAR 이름 `swingwebpanel.jar`, 스레드 이름 접두사 `swingwebpanel-` 는 패키지명이 아니므로 그대로 둔다.) 공개 진입점을 `org.duckdns.hjow.swingwebpanel.SwingWebPanel`로 정하고 `JPanel`을 상속하며 `Scrollable`을 구현하도록 설계한다. 호출자가 `new JScrollPane(panel)`로 감쌀 수 있게 하고, 내부에 또 다른 스크롤 창을 강제하지 않는다. 렌더링 부품과 페이지 상태는 별도 클래스에 둔다.
- [x] P01-02. 생성 직후 URL은 `about:blank`, 원문은 빈 문자열, 이력은 `about:blank` 하나, User-Agent는 `swingwebpanel/1.0`, 추가 헤더와 쿠키는 빈 상태, JavaScript는 활성 상태, 진행률은 100으로 정한다. JavaScript 기본값과 초기 상태를 사용자 문서에도 기록한다.
- [x] P01-03. 이동/콘텐츠 설정 메서드는 반환형 `void`, URL/원문/상태/쿠키/User-Agent 조회는 `String`, 진행률은 `int`로 정한다. 이력은 `Vector<String>`, 헤더는 `Map<String,String>`, 저장소 조회는 `Collection<StorageEntry>`로 정한다. `StorageEntry`에는 origin, key, value를 포함하고 읽기 전용 복사본을 반환한다.
- [x] P01-04. 모든 공개 메서드를 EDT와 일반 스레드에서 호출할 수 있도록 계약을 정한다. 입력 검증은 호출 시 수행하고, 화면 상태 변경은 EDT에 요청한다. 네트워크와 긴 파싱/실행은 EDT를 막지 않는다. 조회 메서드는 마지막 확정 상태의 복사본이나 불변 값을 반환한다.
- [x] P01-05. `setPage`는 비동기 이동으로 정한다. 로딩 중 `getPage()`와 `getContent()`는 이전 확정 페이지를 반환하고, 새 페이지가 확정되면 URL/원문/문서/이력 위치를 함께 교체한다. 네트워크 실패는 오류 페이지로 확정하며, 잘못된 메서드 인수는 `IllegalArgumentException`으로 처리한다.
- [x] P01-06. `getContent()`는 문자셋으로 디코딩한 수신 원문을 보관하여 반환한다. DOM 변경이나 Markdown의 HTML 변환으로 값을 바꾸지 않는다. `setContent`는 입력 문자열을 그대로 보관한다. HTTP 오류 응답 본문이 있으면 그 원문을 유지하고, FTP 목록/본문 없는 로컬 오류 페이지는 내부 생성 문서 원문을 반환하도록 기록한다.
- [x] P01-07. 이력에는 확정된 주 문서 이동을 기록한다. 리디렉션은 최종 URL 한 건, 하위 리소스 요청은 이력 제외로 정한다. `setContent`도 이력에 넣고 원문과 MIME 타입을 함께 보관하여 복원한다. 뒤로 이동한 뒤 새 이동을 하면 앞쪽 이력을 삭제하고, 연속된 동일 URL 이동은 별도 항목으로 보관한다.
- [x] P01-08. `back()`과 `forward()`는 각각 한 칸 이동한다. `n <= 0`이거나 정확히 n칸 이동할 수 없으면 아무 동작도 하지 않는다. 가능한 범위로 n을 자동 보정하지 않는다. 이동과 새로고침은 이력 항목을 추가하지 않으며, `getHistories()`는 전체 이력 URL의 복사본을 반환한다.
- [x] P01-09. `refresh(false)`는 현재 원문/리소스의 캐시 정책을 따르고, `refresh(true)`는 주 문서와 그 이동에서 요청하는 하위 리소스의 캐시를 우회한다. `about:text`는 저장한 입력 원문으로 다시 구성하고 `about:blank`는 빈 상태를 유지한다. force가 다른 컴포넌트의 캐시까지 삭제하는 것으로 구현하지 않는다.
- [x] P01-10. 헤더 이름은 대소문자를 구분하지 않고 기존 값을 덮어쓴다. 어떠한 대소문자의 `User-Agent`도 `addHeaders`에서 받지 않고 무시하며 조회 결과에서도 제외한다. null, String 이외의 키/값, 헤더의 CR/LF 입력은 거부한다. `getHeaders()`는 복사본을 반환한다.
- [x] P01-11. 쿠키 저장소는 컴포넌트별로 분리한다. `getCookies()`는 해당 저장소의 만료되지 않은 쿠키 전부를 `name=value; name=value` 형식으로 반환한다. 도메인/경로/이름 순으로 정렬하고 동명 쿠키도 보존한다. JavaScript의 `document.cookie`는 현재 URL에 적용되는 쿠키만 반환하고 HttpOnly를 숨긴다.
- [x] P01-12. 외부 페이지 이동은 HTTP/HTTPS/FTP만 허용하고, `classpath://`, `about:blank`, 내부 `about:text`는 명시적 특수 주소로 처리한다. `setPage("about:text")`로 임의 원문을 만들지는 않는다. `file`, `jar`, `mailto`, `javascript`, `data`, `blob` 등은 허용 스키마에 추가하지 않고 차단 정책을 기록한다.
- [x] P01-13. `classpath://` 경로는 클래스패스 루트 기준으로 해석하고 클래스 로더의 스트림으로 읽는다. 공백과 퍼센트 인코딩, 상대 리소스, 루트 경로를 정규화한다. `about:text`의 상대 주소에는 외부 기준 URL이 없으므로 `<base>`가 없으면 해석 실패로 처리한다.
- [x] P01-14. sessionStorage는 컴포넌트별/오리진별로, localStorage는 같은 JVM 프로세스의 컴포넌트들이 공유하되 오리진별로 분리한다. Java 조회 메서드는 각 저장소의 모든 오리진 항목을 복사하여 반환한다. 웹 스크립트에는 현재 오리진 영역만 노출한다.
- [x] P01-15. `clean()`은 진행 중 작업을 취소하고 빈 페이지로 전환하며 이력을 `about:blank` 하나로 초기화한다. 이 컴포넌트의 모든 캐시와 sessionStorage 및 프로세스 공용 localStorage 전체를 비운다. User-Agent, 추가 헤더, 쿠키, JavaScript 설정, 사용자 리스너는 유지하도록 정한다. 공유 localStorage 삭제의 다른 컴포넌트 영향도 문서화한다.
- [x] P01-16. `dispose()`는 한 번만 자원을 해제하며 반복 호출은 안전하게 끝낸다. clean 처리 후 이 객체에 등록된 모든 사용자 리스너와 자체 생성 이벤트 연결, 타이머, 작업, 미디어, 하위 프레임을 제거한다. Swing UI 위임 계층의 필수 연결과 사용자 등록 연결을 구분하여 제거 대상 표를 만든다. 폐기 후 변경 메서드는 `IllegalStateException`, 조회는 마지막 빈 상태를 반환하도록 정한다.
- [x] P01-17. null/빈 URL, null 콘텐츠, null MIME, 미지원 MIME, null 리스너, 빈 User-Agent의 동작을 메서드별로 적는다. null 콘텐츠는 거부하고 빈 콘텐츠는 허용한다. MIME은 매개변수를 분리하고 타입 부분을 대소문자 무관하게 처리한다. 리스너 중복 등록/제거는 Swing의 일반적인 등록 단위 동작과 일치시킨다.
- [x] P01-18. 파일 다운로드 계약을 정한다. 주 문서 이동의 응답이 `Content-Disposition: attachment`이거나 동일 오리진의 `<a download>` 링크이거나, P17에서 정하는 표시 가능 형식이 아닌 MIME이면 페이지를 교체하지 않고 Swing 다운로드 팝업을 띄운다. 링크 클릭을 기준으로 하되 폼 전송, 스크립트 이동, `setPage`도 같은 판정을 사용하는 것을 기본으로 한다. 다운로드는 `getPage()`/`getContent()`/이력/페이지 진행률을 바꾸지 않는다. 팝업은 파일명/출처 URL/크기, 저장 위치 선택, 진행률(크기를 모르면 indeterminate), 취소, 완료/실패 결과를 제공한다. 팝업 생성은 호스트와 시험 코드가 대체할 수 있도록 protected 메서드로 분리한다. 헤드리스 환경에서는 다운로드를 취소하고 상태 메시지로 알린다. clean/dispose는 진행 중 다운로드를 취소하고 팝업을 닫으며, 완료된 파일은 지우지 않는다.
- [x] P01-19. 상태 메시지와 진행률 0~100/-1의 상태표를 작성한다. 전체 크기를 모를 때는 -1, 로딩 시작은 0, 문서와 필수 하위 작업의 성공/실패 정리가 끝나면 100으로 정한다. '연결 성공'만으로 100을 반환하지 않는다. P01 계약과 상단 요구사항의 대응을 검토한 뒤 이 단계를 완료 처리한다.

### P02. Java 6 빌드와 프로젝트 구조

선행 작업: P01. 산출물: Java 6 빌드/검증 스크립트, 소스 패키지, 자체 검증 실행기.

- [x] P02-01. `src/org/duckdns/hjow/swingwebpanel` 아래에 `core`, `net`, `html`, `dom`, `css`, `layout`, `render`, `js`, `storage`, `media`, `content`, `download` 패키지를 만든다. 테스트는 `tests/src`, 입력 자료는 `tests/resources`, 예제는 `examples`, 개발 보조 도구는 `tools/<도구 이름>`, 생성물은 `build`로 분리한다. 생성물과 IDE 출력(`build`, `bin`)은 `.gitignore`로 commit 대상에서 제외한다. 클래스 하나에 여러 엔진을 몰아넣지 않는다.
- [x] P02-02. 네트워크 결과 `ResourceResponse`, 원문과 문서 상태 `PageSnapshot`, DOM의 `Document`, 계산 스타일 `ComputedStyle`, 레이아웃 결과 `LayoutBox`, 그림 명령 `DisplayList`, JavaScript 실행 환경 `JsRealm`의 책임과 데이터 흐름을 정한다. 이름은 구현 중 조정할 수 있지만 책임 분리는 유지한다.
- [x] P02-03. Java 코드에 람다, 다이아몬드 문법, try-with-resources, NIO.2, `StandardCharsets`, Java 8 이후 API를 사용하지 않는다. JavaScript 엔진의 ES6~ES2026 처리를 Java 소스의 Java 8 이후 문법으로 대신 구현하지 않는다.
- [x] P02-04. JDK 6에서 `javac -encoding UTF-8 -source 1.6 -target 1.6`으로 빌드하는 스크립트를 sh(UTF-8)와 bat(MSWIN949)로 만든다. JDK 경로는 공통 작업 규칙에 따라 기본값 `C:\Program Files\Java\jdk1.6.0_45`와 환경 변수 재지정을 지원하고, 찾지 못하면 오류로 종료한다. 더 새 JDK를 사용할 경우 해당 JDK의 Java 6 대상 컴파일 가능 여부와 JRE 6 API 검사 방법을 별도로 기록한다. target 옵션만으로 JRE 6 API 호환성이 증명된다고 적지 않는다.
- [x] P02-05. 검증 실행기를 표준 Java만으로 만든다. `main`에서 사례를 실행하고 실패 시 0이 아닌 종료 코드를 반환한다. JVM 옵션에 따라 비활성화되는 `assert`에만 의존하지 말고 명시적인 실패 판정을 사용한다. 제품 JAR에 테스트 클래스, 입력 자료와 `tools/`의 도구를 포함하지 않는다.
- [x] P02-06. 로컬 HTTP 시험 서버는 `java.net.ServerSocket`, HTTPS 시험 서버는 `SSLServerSocket` 등 공개 API로 작성한다. 시험용 키/인증서/신뢰 저장소는 시험 범위에만 적용하고, JVM 전역 신뢰 설정을 바꾸지 않는다. 금지된 `com.sun.net.httpserver`를 테스트에서도 사용하지 않는다. 서버 종료 시 소켓과 스레드를 정리하도록 한다.
- [x] P02-07. java/md/sh는 UTF-8, bat는 MSWIN949로 저장하는 규칙을 스크립트와 편집기 설정에 반영한다. 읽기/쓰기 시 플랫폼 기본 인코딩에 기대지 않는다. bat에 UTF-8 한국어가 섞이지 않는지 별도로 확인한다. 인코딩 검사는 `tools/` 아래 Java 6 도구로 만들어 `src`, `tests`, `examples`, `tools`, 문서 전체에 반복 실행할 수 있게 한다.
- [x] P02-08. 모든 클래스/메서드/필드/상수에 영어와 한국어 Javadoc을 작성한다. 중요한 분기와 반복문은 한국어 주석을 넣는다. 설명에는 책임, 입력 조건, 반환값, 예외, 스레드 조건, 간단한 사용 방법을 포함한다.
- [x] P02-09. 공개 문서화된 `java.*`, `javax.*` API만 사용한다. `com.sun.*`, `sun.*`, JRE 내부 Rhino 클래스, 외부 JAR, 네이티브 코드(C/C++ 소스, `native` 메서드, `System.loadLibrary`/`System.load`, JNA, `.dll`/`.so`/`.dylib`), 제품 코드의 `Runtime.exec`/`ProcessBuilder` 사용이 없는지 검사하는 도구를 `tools/`에 만든다. 검사 대상에는 `src`, `tests`, `examples`, `tools`를 모두 포함한다. 빈 실행기 빌드와 실패 사례의 종료 코드까지 확인하고 P02를 완료 처리한다.

### P03. 컴포넌트 골격과 페이지 상태 관리

선행 작업: P02. 산출물: `SwingWebPanel`, `BrowserSession`, 페이지 이동 제어기와 빈 페이지 예제.

- [x] P03-01. 공개 클래스와 상단의 모든 공개 메서드를 P01 계약대로 선언한다. 미구현 기능의 메서드가 거짓 성공을 반환하지 않게 상태를 명시하고, 기능 표에는 미구현으로 기록한다.
- [x] P03-02. 페이지의 URL, 원문, MIME, DOM, 상태 메시지, 진행률, 이력 위치를 별도 모델로 관리한다. 원문을 DOM 직렬화 결과로 덮어쓰지 않는다. 조회 값의 일관성을 위해 페이지 확정 스냅샷을 한 번에 교체한다.
- [x] P03-03. 이동 요청마다 증가하는 요청 번호를 부여한다. 새로운 이동이 시작되면 이전 요청을 취소하고, 번호가 오래된 완료 콜백은 화면/이력/원문에 반영하지 않는다.
- [x] P03-04. 백그라운드 작업과 EDT 반영을 분리한다. 문서와 JavaScript 상태의 변경은 한 소유 스레드에서 직렬 처리하고, 렌더러에는 변경되지 않는 스냅샷을 전달한다. 서로 다른 스레드가 DOM을 동시에 수정하지 않도록 한다.
- [x] P03-05. `Scrollable`의 뷰포트 크기, 스크롤 단위, 블록 단위, 폭 추적 정책을 구현한다. 뷰포트 폭 변경 시 재배치하고, 내용이 넓어야 하는 문서에서는 가로 스크롤을 허용할 기준을 문서화한다.
- [x] P03-06. `about:blank`, 빈 콘텐츠, 상태 메시지와 진행률 조회, User-Agent/헤더 설정을 먼저 연결한다. 상태 변경을 호스트 애플리케이션이 관찰할 수 있도록 `page`, `statusText`, `progress` 등의 속성 변경 알림을 EDT에서 발생시킨다.
- [x] P03-07. 반복 생성/폐기, EDT와 일반 스레드의 호출, 오래된 응답 취소, 이력 복사본 변경이 내부 상태에 영향을 주지 않는 사례를 확인한다. 빈 페이지를 `JScrollPane`으로 감싼 예제가 실행되면 P03을 완료 처리한다.

### P04. URL, HTTP, HTTPS와 리소스 요청

선행 작업: P03. 산출물: URL 처리기, HTTP 요청기, SSL 확장 지점, 리소스 응답 모델.

- [x] P04-01. `URI` 기반의 URL 해석/상대 주소 결합/경로 정규화/프래그먼트 분리를 구현한다. 퍼센트 인코딩된 경로를 중복 디코딩하지 않고, URL 경로의 `+`를 폼의 공백처럼 처리하지 않는다. 상대 주소와 `<base>`를 공통 경로에서 처리한다.
- [x] P04-02. 허용 스키마 검사를 최초 주소, 리디렉션 주소, 하위 리소스 주소에 적용한다. 차단된 주소는 서버에 연결하지 않는다. 클래스패스 주소가 로컬 파일 URL이나 다른 허용되지 않은 스키마로 변환되어 우회되지 않게 한다.
- [x] P04-03. `HttpURLConnection`으로 GET/HEAD와 폼 전송에 필요한 POST를 구현한다. 연결/읽기 제한 시간, 리디렉션 횟수, 최대 응답 크기는 이름 있는 상수로 두고 기본값과 초과 시 동작을 문서화한다. 다운로드 응답은 메모리에 모으지 않고 파일로 스트리밍 저장하므로, 메모리 응답 크기 제한과 다운로드 크기 정책을 따로 둔다. 취소와 예외 경로에서도 스트림과 연결을 닫는다.
- [x] P04-04. User-Agent와 추가 헤더를 주 문서/이미지/오디오/CSS/스크립트 요청에 동일한 요청 계층에서 적용한다. 리디렉션으로 오리진이 바뀔 때 인증 관련 헤더를 전달할 범위를 정하고 검증한다. 쿠키 헤더는 별도 쿠키 정책으로 계산한다.
- [x] P04-05. 리디렉션 응답을 직접 처리하고 상대 Location, 순환, 제한 초과, POST의 메서드 전환, HTTPS/HTTP 이동을 구분한다. 최종 유효 URL을 문서 기준 주소와 `getPage()`에 사용한다.
- [x] P04-06. 응답 코드, 헤더, MIME, 문자셋, 압축 해제한 본문, 최종 URL을 응답 모델에 저장한다. 오류 본문은 `getErrorStream()`으로 읽는다. gzip/deflate 등의 응답을 지원할 때는 실제 해제 가능 형식만 요청 헤더에 알린다.
- [x] P04-07. SSL 설정을 `protected void configureSsl(HttpsURLConnection connection) throws IOException` 같은 별도 메서드로 분리하고 모든 HTTPS 요청에서 호출한다. JRE 기본 신뢰 저장소와 호스트 이름 검증을 사용하며, 전역 SSL 설정을 변경하거나 인증서 검증을 무조건 통과시키지 않는다.
- [x] P04-08. HTTPS의 연결/TLS 처리 실패 시에만 같은 주소의 HTTP 접근을 한 번 시도한다. HTTP 404/500 응답은 HTTPS 접근 자체의 실패로 간주하지 않는다. 기본 443 포트는 HTTP의 기본 포트로 바꾸고 명시한 다른 포트는 유지한다. 재시도 성공 시 실제 HTTP URL과 상태를 표시하며 순환 재시도를 막는다.
- [x] P04-09. HTTPS에서 HTTP로 바뀌면 Secure 쿠키를 보내지 않는다. 평문 전환이 발생했다는 사실과 추가 헤더 전달 정책을 문서에 적는다. 보호 메서드를 재정의한 시험 하위 클래스에서 호출 여부와 적용 범위를 확인한다.
- [x] P04-10. 쿠키의 Domain/Path/Expires/Max-Age/Secure/HttpOnly와 삭제를 처리하고, 응답별로 갱신한다. `CookieManager`를 활용해도 컴포넌트별로 호출하며 `CookieHandler.setDefault`나 `Authenticator.setDefault` 등 JVM 전역 상태를 바꾸지 않는다.
- [x] P04-11. 오리진을 스키마/호스트/유효 포트로 계산한다. 동일 오리진 정책과 교차 오리진 허용 조건을 요청 목적별로 구분한다. 이미지 표시가 가능하다는 이유로 교차 오리진 응답 본문을 스크립트에 노출하지 않는다.
- [x] P04-12. 로컬 시험 서버로 200/404/500, 리디렉션, 지연/취소, 잘린 응답, 압축, 쿠키, 헤더, SSL 실패 후 HTTP 재시도를 확인한다. 네트워크 없는 문자열 검사만으로 요청 계층을 완료 표시하지 않는다. P04 사례가 통과하면 이 단계를 완료 처리한다.

### P05. FTP와 클래스패스 접근

선행 작업: P04. 산출물: 익명 FTP 목록 처리기, 클래스패스 로더, 링크가 있는 목록 문서.

- [x] P05-01. FTP의 제어 연결과 데이터 연결을 표준 소켓 API로 구현한다. 익명 사용자, 응답 코드, 여러 줄 응답, 로그인, 종료, 제한 시간과 취소를 처리한다. 주소에 인증 정보가 있어도 임의 사용자 로그인으로 바꾸지 않고 익명 정책을 지킨다. 디코딩된 경로에 CR/LF가 있으면 FTP 명령에 삽입하지 않고 거부한다. JRE 내부 FTP 클래스를 가져오지 않는다.
- [x] P05-02. EPSV/PASV 순서와 실패 대체 규칙을 정한다. 데이터 연결 주소와 포트의 유효성을 검사하고, 디렉터리 목록 수신 전후의 응답을 모두 처리한다. 데이터 소켓만 열고 완료 응답을 무시하지 않는다.
- [x] P05-03. MLSD 지원을 확인하고 우선 사용하며, 지원하지 않으면 LIST로 전환한다. UNIX/DOS 등의 LIST 형식, 공백/비ASCII 파일명, 디렉터리, 심볼릭 링크를 판별한다. 판별할 수 없는 행을 디렉터리로 잘못 표시하지 않도록 원문과 진단을 보관한다.
- [x] P05-04. 목록의 디렉터리는 URL 인코딩한 링크로, 파일은 파일 항목으로 표시한다. 상위 이동 `../` 링크를 목록 맨 위에 만들고 FTP 루트에서는 루트보다 위로 이동하지 않게 한다. 파일명이 HTML 태그처럼 해석되지 않도록 이스케이프한다.
- [x] P05-05. FTP 주소가 파일을 가리킬 때의 수신/표시 MIME 정책과 디렉터리 여부 판정 규칙을 기록한다. 주 문서로 표시할 수 없는 FTP 파일은 P01의 다운로드 계약에 따라 다운로드 팝업으로 연결한다. 접근/인증/목록 해석 실패는 프로젝트 요구에 따라 404 화면으로 처리하고 연결을 모두 닫는다.
- [x] P05-06. 클래스 로더 스트림으로 HTML과 상대 리소스를 읽고, 없는 리소스는 오류 페이지/대체 표시로 연결한다. JAR 안의 클래스패스 리소스도 파일 경로로 변환하지 않고 읽는다.
- [x] P05-07. 자체 로컬 FTP 시험 서버로 하위/상위 디렉터리, 루트, MLSD 미지원, 여러 줄 응답, 한글/공백 이름, 실패와 취소를 검증한다. 디렉터리 링크가 실제 이동을 일으키는 것은 렌더러 연결 후 다시 검증한다. 현재 검증 범위를 기록하고 P05를 완료 처리한다.

### P06. DOM 자료 구조와 이벤트 모델

선행 작업: P02, P03. P07부터 이 DOM을 사용한다. 산출물: DOM 트리와 JavaScript에 독립적인 이벤트 처리기.

- [x] P06-01. Document/Element/Text/Comment/DocumentType/DocumentFragment와 부모/자식/형제/소유 문서 관계를 구현한다. HTML과 XML의 이름 대소문자, 네임스페이스와 속성 정책을 구분한다.
- [x] P06-02. appendChild/insertBefore/removeChild/replaceChild/cloneNode와 문서 간 이동을 구현한다. 조상 아래에 자신을 삽입하는 순환, 잘못된 부모, 없는 자식 등의 오류를 검출한다. 모든 변경을 공통 변경 경로에 모은다.
- [x] P06-03. getAttribute/setAttribute/removeAttribute, id/className/classList, textContent를 구현한다. 텍스트 노드와 속성의 이스케이프를 별도 처리하며 textContent 입력을 HTML로 실행하지 않는다.
- [x] P06-04. getElementById/getElementsByTagName/getElementsByClassName을 구현한다. live 컬렉션과 고정 스냅샷의 차이를 정하고 DOM 변경 후 반환 결과가 규격에 맞게 바뀌는지 확인한다.
- [x] P06-05. querySelector/querySelectorAll/matches가 사용할 선택자 조회 인터페이스와 노드 순회 규칙을 설계한다. 선택자 해석기와 실제 조회 구현/검증은 P08-09에서 수행하며, 그때까지 해당 기능은 기능 표에서 미구현으로 유지한다. DOM 기반 단계가 후속 CSS 단계의 완료를 기다리지 않도록 한다.
- [x] P06-06. EventTarget/Event에 addEventListener/removeEventListener/dispatchEvent를 구현한다. 캡처/대상/버블 순서, target/currentTarget, cancelable, preventDefault, stopPropagation, stopImmediatePropagation을 처리한다.
- [x] P06-07. 이벤트 처리 중 리스너 추가/제거, 같은 리스너의 중복, capture 인수 차이, 한 리스너의 예외가 다른 리스너에 미치는 영향을 규격에 따라 처리한다. 기본 동작은 이벤트 처리 후 취소 여부를 보고 수행한다.
- [x] P06-08. DOM 변경을 스타일/레이아웃/그리기 무효화와 연결할 수 있게 변경 종류를 구분한다. 분리된 노드의 변경 때문에 전체 화면을 항상 다시 구성하지 않도록 한다.
- [x] P06-09. DOM 순환 방지, 노드 이동, 문자열과 속성 변경, live 컬렉션, 이벤트 전파/취소의 기대 결과를 자동 검증한다. JavaScript 없이 DOM과 이벤트가 독립적으로 검증되면 P06을 완료 처리한다.

### P07. HTML5 파서와 이전 HTML 호환 처리

선행 작업: P04, P06. 산출물: HTML 토큰화기, 트리 생성기, 문서 모드와 오류 복구 처리.

- [x] P07-01. 네트워크 본문 인코딩 판정과 HTML 파싱용 입력 정규화를 분리한다. BOM/응답 charset/meta charset/기본값의 우선순위를 문서화한다. meta 판정으로 재디코딩해야 하는 경우를 위해 필요한 원본 바이트를 보관한다. `getContent()`용 원문은 줄바꿈/공백을 보존하고, 파서에 필요한 정규화는 별도 입력에서 한다.
- [x] P07-02. 토큰 종류를 시작 태그/종료 태그/문자/주석/DOCTYPE/EOF로 정의하고 위치 정보를 보관한다. 속성 이름/값, 따옴표 유무, 자기 닫기 표기, 중복 속성과 문자 참조를 처리한다.
- [x] P07-03. data, tag, attribute, comment, RCDATA, RAWTEXT, script data 등의 상태를 명시적인 상태 기계로 구현한다. HTML을 하나의 정규식이나 XML 파서 호출로 처리하지 않는다.
- [x] P07-04. HTML5의 이름 있는 문자 참조와 숫자 참조, 세미콜론 누락, 잘못된 코드 포인트와 속성 안의 참조를 처리한다. 직접 작성한 표와 처리기의 출처/생성 방법을 문서화한다. 표를 규격 자료에서 생성한다면 생성 도구를 `tools/`에 두고 원본 판본을 기록한다.
- [x] P07-05. 트리 생성의 삽입 모드와 열린 요소 스택을 구현한다. 생략된 html/head/body, p/li 종료, 잘못 중첩된 서식 요소, 테이블 안의 잘못된 내용 등 대표 복구 사례를 먼저 추가하고 규격 전체로 확장한다.
- [x] P07-06. template, SVG/MathML 외부 콘텐츠, 네임스페이스 전환, 문서 조각 파싱을 구현한다. innerHTML용 조각 파서는 요소 문맥에 따라 다른 결과가 나오는 사례를 처리한다.
  - [x] P07-06-a. template 내용의 전역 보관을 요소 소유로 바꾸고 복제/가져오기/이동을 처리한다. 입력: 중첩 template의 shallow/deep clone, 다른 Document의 importNode/adoptNode와 문서 해제. 기대: shallow 내용은 비고 deep 내용은 독립 복제되며 소유 문서가 일관되고 전역 참조가 문서 수거를 막지 않는다. 구현 파일: `dom/Element.java`, `dom/Node.java`, `html/ParserNodeData.java`; 완료 조건: 위 사례와 HTML/DOM 회귀 검증 통과.
- [x] P07-07. DOCTYPE에 따른 표준/제한 호환/호환 모드를 구분한다. 과거 HTML 요소/속성의 표시 규칙을 기능 표대로 처리하고, 오래된 문서를 HTML5 태그 목록만으로 누락하지 않는다.
  - [x] P07-07-a. DOCTYPE 및 과거 요소의 파싱을 검증한다. 입력: HTML 2/3.2/4.01/XHTML DOCTYPE, 폐기 요소와 속성; 기대: 세 문서 모드와 고정 DOM; 구현: `html/DocumentModes.java`, `tests/.../html/{DocumentModesTest,LegacyHtmlTest,TreeConstructionTest}`; 완료 조건: HTML/DOM 회귀 103건 통과(2026-11-16).
  - [x] P07-07-b. LEG-01/03~06/08/11의 UA 스타일·표현 속성·목록·문서 모드별 화면 규칙을 구현한다. 입력: 해당 LEG 표의 요소/속성을 섞은 페이지; 기대: 계산 스타일 및 상자·표식·픽셀; 구현: `css/cascade`, `layout`, `render`; 완료 조건: 각 LEG 행의 스타일/화면 검증 근거 연결. P09/P10 통합 후 확인한다.
    - [x] P07-07-b-1. body link/vlink/alink 표현 힌트를 상태 선택자와 연결한다. 입력: 미방문/방문/활성 링크, 속성 변경/본문 교체, 작성자 CSS 우선순위; 기대: HTML5 2014 §10.3.2 색·캐시 무효화와 :link/:visited 구분; 구현: `css/cascade/HtmlPresentationalHints.java`, `StyleResolver.java`, `css/selector/SelectorMatcher.java`; 완료 조건: 계산 스타일/선택자 및 기존 CSS 회귀 통과. 실제 방문/활성 입력 상태 연결은 P11에 별도 유지한다.
    - [x] P07-07-b-2. body 여백의 첫 존재 속성 우선순위와 실패 기본값을 구현한다. 입력: marginwidth/height와 left/right/top/bottommargin 동시 지정, 잘못된 상위 값, 부호·공백·선행 0·긴 숫자, DOM 속성 변경; 기대: HTML5 2014 §10.3.2와 §2.4.4의 해석·기본 8px, 작성자 CSS 우선, 상자/픽셀; 구현: `css/cascade/HtmlPresentationalHints.java`; 완료 조건: 계산 스타일/화면 및 CSS 회귀 통과. 컨테이너 frame/iframe 속성 상속은 P20-02 후속이다.
    - [x] P07-07-b-3. font의 color/face/size 표현 힌트를 규격에 맞게 처리한다. 입력: 한 자리·빈 공백·긴 색, CSS3 색 이름, 절대/상대 크기와 긴 선행 0·부호 뒤 공백, 글꼴 목록·주석·선언 구분자, 속성 변경과 작성자 CSS; 기대: HTML5 2014 §2.4.6 및 §10.3.4의 계산 값, 예외 없는 색 해석, 독립된 속성 파싱과 상자/픽셀; 구현: `css/cascade/HtmlPresentationalHints.java`, `tests/src/org/duckdns/hjow/swingwebpanel/css/cascade/LegacyFontHintsTest.java`, `tests/src/org/duckdns/hjow/swingwebpanel/SwingRenderingTest.java`; 완료 조건: 해당 8건·Swing 오프스크린 1건 및 CSS/레이아웃/렌더링 회귀 558건 JRE 6 통과(2026-11-17). basefont 및 다른 LEG-01 요소는 후속으로 유지한다.
    - [x] P07-07-b-4. LEG-01의 정적 UA 표시와 center 텍스트 정렬을 검증·보완한다. 입력: big/small/tt/strike/s/u/blink/basefont/center/nobr, nobr 안팎의 wbr, 작성자·사용자 CSS 및 변경; 기대: HTML5 2014 §10.3.1/3/4의 계산 값·크기·장식 화소, center 힌트의 캐스케이드, nowrap 안의 명시적 줄바꿈 기회와 실제 Swing 표시; 구현: `css/cssom/UaStyleSheet.java`, `css/cascade/HtmlPresentationalHints.java`, `layout/{BoxBuilder,InlineItem,InlineLayouter}.java`, 해당 CSS/렌더링 및 Swing 시험; 완료 조건: JRE 6u45 집중 15건(새 UA 13건·Swing 2건) 및 전체 2520 통과/0 실패/선택적 Opus 원본 벡터 12건 건너뜀, 제품 JAR·실행기 종료 코드·소스 규칙 검증 통과(2026-11-17). marquee 정적 상자, basefont의 과거 판본 효과, center의 자손 블록 정렬은 별도 후속으로 유지한다.
    - [x] P07-07-b-5. center 및 div align의 자손 블록 정렬을 구현한다. 입력: 고정 폭 자손, 중첩 center/div align, auto 여백·넘치는 폭·자손 자신의 유효한 align, 작성자 text-align 재정의; 기대: HTML5 2014 §10.2 align-descendants의 적용 조건·가장 가까운 정렬 조상·사용 여백 보정이며 텍스트 정렬과 독립적으로 동작; 구현: `layout/BlockLayouter.java` 및 필요한 정렬 도우미와 CSS/레이아웃/렌더링 시험; 완료 조건: 조건별 상자·픽셀·실제 Swing 오프스크린 검증의 JRE 6 통과. 완료(2026-11-17): `layout/AlignDescendants.java`(정렬 요소 판정과 가장 가까운 조상), `BlockLayouter.resolveMargins`(margin 상자 중앙/오른쪽 보정, 안쪽 float 문맥 x 보정), 시험 `AlignDescendantsLayoutTest` 16건·`SwingRenderingTest.testAlignDescendantsPaintsAlignedBlocks` 1건, 전체 2537 통과/0 실패/선택적 Opus 원본 벡터 12건 건너뜀. marquee·basefont의 남은 판본별 동작은 별도 후속으로 유지한다.
    - [x] P07-07-b-6. LEG-03의 목록 속성을 규격대로 처리한다. 입력: `ol/ul/li type`의 대소문자·알 수 없는 값·부모와 다른 종류의 li, `ol start`/`li value`의 앞 공백·부호·뒤따르는 글자·큰 수·빈 값, `reversed`, `dir/menu`, 작성자 CSS 우선순위; 기대: HTML5 2014 §4.5.5~4.5.8 및 §10.3.8의 표식 문자열과 순서 값; 구현: `css/cascade/HtmlPresentationalHints.java`, `layout/BoxBuilder.java`; 완료 조건: `LegacyListsTest` 및 레이아웃/CSS 회귀의 JRE 6 통과. 완료(2026-11-17): `LegacyListsTest` 6건(수정 전 소스에서는 4건 실패), 전체 2543 통과/0 실패/선택적 Opus 원본 벡터 12건 건너뜀. `li value` 는 `ol` 이외의 부모에서도 적용한다(현행 WHATWG 동작; HTML5 2014 와의 차이는 docs/progress/legacy.md).
    - [x] P07-07-b-7. LEG-06의 표 표현 속성을 점검·구현한다. 입력: `table border/frame/rules/cellpadding/cellspacing/hspace/vspace/width/height/bgcolor/background/align`, `colgroup/col span/width`, `tr height`, 셀 `align=absmiddle/valign/nowrap/width/height`, 표 안 `form`, quirks 모드 표의 글꼴 초기화; 기대: HTML5 2014 §10.3.9의 UA 규칙과 표현 힌트별 계산 스타일·경계선·상자·화소; 구현: `css/cascade/HtmlPresentationalHints.java`, `css/cssom/UaStyleSheet.java`, `layout/TableLayouter.java`; 완료 조건: 규칙별 시험과 표 회귀의 JRE 6 통과. 완료(2026-11-17): `css/cascade/{LegacyAttributes,TableHints}`, 시험 `LegacyTablesTest` 21건·`SwingRenderingTest.testLegacyTableAttributesPaint`(수정 전 소스에서는 15건 실패 확인), 병합 뒤 전체 2803 통과. 숨겨진 표 부품은 규격의 `visibility:collapse` 가 아닌 `display:none`(한계는 docs/progress/legacy.md).
    - [x] P07-07-b-8. LEG-05의 나머지 정렬·크기 속성을 점검·구현한다. 입력: `hr color/noshade/size/width/align`, `img/object/embed/iframe align hspace vspace border width height`, `br clear`, `p/h1~h6 align`, `legend align`, `fieldset`; 기대: HTML5 2014 §10.3.12, §10.3.13, §10.4의 UA 규칙과 힌트; 구현: `css/cascade/HtmlPresentationalHints.java`, `css/cssom/UaStyleSheet.java`, `layout`; 완료 조건: 규칙별 계산 스타일/상자/화소 시험의 JRE 6 통과. 완료(2026-11-17): hr·삽입/치환 요소·`br clear`·fieldset/legend·문단/제목 align, 시험 `LegacyEmbeddedTest` 17건(수정 전 11건 실패 확인), 병합 뒤 전체 2803 통과.
    - [x] P07-07-b-9. LEG-08의 `plaintext/listing/xmp/isindex`와 LEG-01의 `marquee/basefont` 남은 동작을 처리한다. 입력: 각 요소의 파싱·표시·작성자 CSS; 기대: HTML5 2014의 파서 상태와 §10.3의 UA 스타일 및 프로젝트의 정적 marquee 정책; 구현: `html/**`, `css/cssom/UaStyleSheet.java`, `layout`; 완료 조건: 파싱 트리와 표시 시험의 JRE 6 통과. 완료(2026-11-17): isindex 확장(HTML5 2014 §8.2.5.4.7), marquee 정적 상자(애니메이션 제외), `pre[wrap]`, 숨김 요소(`source`/`track`/`embed[hidden]`), plaintext/listing/xmp/basefont 시험, `LegacyElementsTest` 12건(수정 전 7건 실패 확인), 병합 뒤 전체 2803 통과. marquee 애니메이션은 프로젝트의 정적 정책으로 미구현.
    - [x] P07-07-b-10. LEG-11의 문서 모드(quirks/limited-quirks/no-quirks)별 화면 차이를 구현한다. 입력: quirks 모드의 `form margin-bottom`, 표 글꼴 초기화, 단위 없는 길이, 해시 없는 색, body 높이, 이미지 아래 간격 등 규격에 있는 차이와 모드 판정; 기대: WHATWG Quirks Mode Standard 및 HTML5 2014 §10의 차이를 판본과 함께 고정한 결과; 구현: `html/DocumentModes.java`, `css/**`, `layout/**`; 완료 조건: 모드별 계산 스타일·상자 시험의 JRE 6 통과. 완료(2026-11-17, 일부): DOCTYPE 모드 판정, form margin-bottom, 표 글꼴 초기화·body 색 상속, 여백 병합 quirk, 단위 없는 길이·# 없는 16진수 색, `:hover/:active` quirk, html/body 뷰포트 채우기(가로쓰기), 줄 높이 quirk(이미지 아래 간격), collapsing table, `LegacyQuirksTest` 13건(수정 전 10건 실패 확인). 남은 호환 모드 규칙은 b-11.
    - [x] P07-07-b-11. 호환 모드의 남은 화면 규칙을 구현한다. 입력: Quirks Mode Standard 3.5(백분율 높이), 3.8(셀 안 이미지 줄바꿈), 3.9(nowrap 셀의 최소 너비), 3.13(셀 높이 box-sizing), HTML5 2014 §10.4.2(호환 모드 이미지 대체 상자), `clip: rect()` quirk, `element.style` 스크립트 설정의 quirk, html/body 채우기의 세로쓰기와 자손 재배치; 기대: 각 규격 절의 계산 값과 상자; 구현: `css/cascade/QuirkHints`, `css/props/QuirkyValues`, `layout/**`; 완료 조건: 모드별 계산 스타일·상자 시험의 JRE 6 통과. 완료(2026-11-17): Quirks 3.5/3.8/3.9/3.13(`LegacyQuirksBoxesTest` 4), §10.4.2 이미지 대체 텍스트(`BrokenImageFallbackTest` 4), `clip: rect()`·`element.style`/`rule.style`/`insertRule`/`@supports` quirk(`QuirksStyleScriptTest` 4, `LegacyQuirksTest` 16), html/body 채우기의 세로쓰기·자손 재배치. 병합 뒤 전체 3092 통과. 한계는 docs/progress/legacy.md.
  - [x] P07-07-c. LEG-02/07/09/12~15의 프레임·이미지 맵·플러그인 대체·XHTML·폼·이동 동작을 검증한다. 입력: 기능 표 각 LEG 행의 페이지; 기대: 별도 문맥/기본 동작과 금지 스키마 차단; 구현: `core`, `content`, `dom`, `render`; 완료 조건: P11/P17/P20 구현 및 각 LEG 검증 근거 연결. 완료(2026-11-17): LEG-02 는 P20-02, LEG-09 는 P20-11, LEG-07/12/13/14/15 는 `ImageMapTest`·`ImageMapIntegrationTest`, `LegacyXhtmlPageTest`·`XmlStylesheetInstructionTest`, `LegacyScriptCompatTest`, `LegacyFormCompatTest`·`ImageButtonIntegrationTest`, `MetaRefreshTest`·`MetaRefreshIntegrationTest`·`LegacyLink*Test`(새 시험 62건)로 검증했다. 한계는 docs/progress/legacy.md 와 기능 표 LEG 행.
- [x] P07-08. script/style의 원문과 외부 주소, async/defer/기본 실행 순서를 문서 로딩 모델에 등록한다. 파싱 중 스크립트 실행과 document.write는 P15에서 연결하고, JavaScript 비활성 시에도 HTML 파싱은 정상 진행한다.
- [x] P07-09. 정상/깨진 HTML 입력별 기대 DOM 트리를 고정 자료로 만든다. 속성 속 `>` 문자, script 안의 비교식, 잘못된 테이블, 문자 참조, 조각 파싱 등을 검증한다. 태그 이름을 인식하는 것과 요소 동작 구현 상태는 별도로 기록하고 P07을 완료 처리한다.

### P08. CSS 파서, 선택자와 계산 스타일

선행 작업: P06, P07. 산출물: CSS 구문 트리, 선택자 일치기, 스타일 계산기.

- [x] P08-01. CSS 토큰화에서 주석, 문자열, 이스케이프, 식별자, 숫자/단위, 함수, URL을 처리한다. 잘못된 선언 하나 때문에 전체 스타일시트를 폐기하지 않도록 규격의 오류 복구 경계를 구현한다.
- [x] P08-02. 외부 stylesheet, style 요소, style 속성, 사용자 에이전트 기본 스타일을 읽는다. @import의 주소 기준, 순환, 가져오기 순서와 로딩 완료를 처리한다. 외부 CSS도 공통 요청기/캐시/헤더를 사용한다.
- [x] P08-03. 타입/id/class/속성 선택자와 자식/자손/인접/일반 형제 조합자를 구현한다. 의사 클래스와 의사 요소는 기능 표에 따라 추가하고, HTML 대소문자/네임스페이스 처리도 검증한다.
- [x] P08-04. 중요도, 출처, 구체성, 소스 순서로 우선순위를 계산한다. 단축 속성의 개별 속성 확장, !important, 상속, initial/inherit/unset의 적용 범위를 처리한다.
- [x] P08-05. 길이/백분율/색/글꼴/문자열/URL 등의 값 자료형을 둔다. 지정 값/계산 값/실제 레이아웃 값을 구분하고, 백분율을 너무 일찍 픽셀로 확정하지 않는다.
- [x] P08-06. 미디어 질의와 뷰포트/상태 의존 선택자를 연결한다. 창 크기, hover/focus/active, class/속성/DOM 변경 시 해당 스타일을 다시 계산한다.
- [x] P08-07. display, position, margin/padding/border, font, color, background, text, overflow 등 기본 속성부터 구현하고 기능 표의 모듈별 속성으로 확장한다. 미지원 선언은 브라우저식 무시 처리와 진단을 제공하되 지원 완료로 표시하지 않는다.
- [x] P08-08. 상속/구체성/중요도/단축 속성, 잘못된 CSS 복구, 상대 URL, 미디어 질의, DOM 변경 후 계산 값의 기대 결과를 검증한다.
- [x] P08-09. P06-05의 인터페이스에 선택자 엔진을 연결하여 querySelector/querySelectorAll/matches를 구현한다. 잘못된 선택자 오류, 첫 일치/전체 일치, 문서 순서와 querySelectorAll의 고정 결과를 검증한다. 계산 스타일과 DOM 선택자 조회 검증이 통과하면 P08을 완료 처리한다.

### P09. 기본 레이아웃과 자체 렌더링

선행 작업: P03, P07, P08. 산출물: 레이아웃 트리, 그림 명령, Java2D 렌더러, 정적 HTML 실행 예제.

- [x] P09-01. DOM 트리와 레이아웃 트리를 분리한다. display:none, 익명 상자, 인라인 텍스트, 치환 요소를 처리하고 DOM 노드와 표시 영역 사이의 연결 정보를 보관한다.
- [x] P09-02. 블록/인라인 레이아웃, 가로/세로 크기, margin/padding/border, box-sizing, 백분율, 최소/최대 크기를 구현한다. margin collapsing과 정상 흐름의 상자 배치를 검증한다.
- [x] P09-03. 글꼴 선택/대체, 문자 측정, 줄바꿈, 공백 축약, white-space, 줄 높이, 정렬, 목록 표식과 텍스트 장식을 구현한다. 한글, 영문, 결합 문자, 서로게이트 쌍과 양방향 문자를 검증 자료에 넣는다.
- [x] P09-04. Java2D의 Graphics2D/TextLayout 등 공개 API로 계산된 내용만 그린다. JEditorPane/HTMLEditorKit나 외부 브라우저에 실제 HTML/CSS 렌더링을 위임하지 않는다. GPU/OpenGL/Direct3D 등 3D 가속에 의존하지 않으며, JVM 전역 Java2D 파이프라인 속성을 설정하지 않는다.
- [x] P09-05. 배경/경계선/텍스트/이미지/대체 문구의 그림 순서를 구현한다. Graphics2D 상태를 저장/복구하고 클리핑, 투명도, 화면 축척과 스크롤 위치를 반영한다.
- [x] P09-06. 레이아웃 상자에 대한 좌표 조회를 구현하여 링크/요소 hit testing에 사용한다. 그림과 입력이 같은 변환/스크롤 좌표계를 사용하도록 한다.
- [x] P09-07. 내용 높이에 따라 preferredSize를 갱신하고 revalidate/repaint를 EDT에서 호출한다. 창 폭 변경과 CSS 변경에 따른 재배치가 반복 갱신 루프를 만들지 않게 한다.
  - [x] P09-07-a. 문서 스레드에서 레이아웃/그림 명령을 만들고 EDT에서 게시·그린다. 입력: 긴 문단, inline CSS, 폭/높이 변경, DOM 변경, 연속 이동과 clean/dispose; 기대: 실제 픽셀·스크롤 크기 갱신과 오래된 결과 폐기, 같은 폭에서 반복 재배치 없음; 구현: `core/PageView.java`, `SwingWebPanel.java`; 완료 조건: Swing 오프스크린 통합 시험과 기존 core 회귀 통과.
  - [x] P09-07-b. 외부 스타일시트/이미지 완료를 재배치에 연결한다. 입력: 느린 CSS/@import/이미지, 오류 URL, 새로고침 force와 이동 중 완료; 기대: EDT/문서 큐를 막지 않는 로딩, 완료 후 최신 페이지에만 게시, 공통 헤더/캐시 사용; 구현: `core/PageResources.java`, `core/PageView.java`; 완료 조건: 시험 서버 요청/픽셀/취소 검증 통과.
- [x] P09-08. HTTP/클래스패스의 제목, 문단, 목록, 링크, 표, 이미지 대체 문구가 있는 정적 페이지를 표시한다. hit testing과 공통 이동 제어기로 기본 href 이동을 연결하고 뒤/앞 이동, 새로고침과 원문 조회를 확인한다. DOM 클릭 취소와 Swing 링크 통지의 전체 처리/검증은 P11에서 연결한다.
  - [x] P09-08-a. 기본 링크 입력을 연결한다. 입력: 링크 진입/이탈/클릭, DOM 취소, Swing 리스너 직접 이동/전체 제거, 금지 스키마; 기대: EDT 링크 통지와 취소되지 않은 단일 이동, 통지 제거 후 기본 이동 유지; 구현: `core/PageInput.java`, `BrowserSession.java`, `SwingWebPanel.java`; 완료 조건: 실제 컴포넌트 MouseEvent 및 문서 큐 회귀 검증 통과.
  - [x] P09-08-b. 정적 페이지 예제와 HTTP/classpath 이력·새로고침을 검증한다. 입력: 제목/문단/목록/링크/표/이미지 대체 문구와 상대 리소스를 포함한 페이지; 기대: 표시·뒤/앞 이동·원문 보존; 구현: `examples/src`, `examples/resources`, `tests/src/org/duckdns/hjow/swingwebpanel`; 완료 조건: JRE 6 오프스크린 검증 및 Swing 예제 실행 결과 기록. 표의 완전한 배치는 P10-02에 남긴다.
    - [x] P09-08-b-1. 자동 검증과 실행 예제. 입력: 동일 HTML/CSS의 HTTP/classpath 페이지, 상대 링크, 이력/force; 기대: 제목·픽셀·원문·이력 복원과 예제 smoke 성공; 구현: `SwingRenderingTest`, `StaticPageExample`, `examples/run.sh|bat`; 완료 조건: JRE 6 headless 통합 6건 및 smoke 통과(2026-11-16).
    - [x] P09-08-b-2. 실제 Swing 창 확인. 입력: StaticPageExample의 링크 클릭/스크롤/뒤·앞 이동/닫기; 기대: 눈으로 확인한 페이지 전환·스크롤·대체 문구와 정상 dispose; 구현: 동일 예제/입력 계층; 완료 조건: Windows 창 캡처·입력이 가능한 환경에서 결과 기록. 이전에는 Computer Use의 FrameArrived/window capture timeout으로 시각 검증을 완료하지 못했다(2026-11-16). 완료(2026-11-18): `tools/uicheck`(Java 6, `java.awt.Robot` 화면 캡처)로 이 PC(Windows 10, JDK 6u45)에서 **실제 StaticPageExample 창**을 확인했다 — 한글/English 문단·목록·표·단추·스크롤 막대·미지원 URL과 대체 문구 표시, 실제 OS 마우스 클릭(Robot)으로 링크 이동, 도구 모음의 `<` `>` `R` 이동, 스크롤, `dispose()` 후 예외 없이 종료. 스크린샷과 한계(첫 클릭 경쟁 관찰, 실제 키보드/IME/휠 미확인; 긴 URL 문구가 상자에서 잘리던 결함은 발견해 고침)는 `docs/progress/uicheck.md`.
- [x] P09-09. 레이아웃 좌표/크기와 그리기 결과를 검증한다. 글꼴/OS 차이가 있는 전체 화면 픽셀 비교만으로 합격을 판정하지 말고 글자 배치/상자/클리핑 기대값을 함께 확인한다. 기본 정적 페이지의 통합 검증이 끝나면 P09를 완료 처리한다.

### P10. CSS 레이아웃과 시각 효과 확대

선행 작업: P09. 산출물: CSS 기능 표의 나머지 레이아웃/시각 모듈과 회귀 입력.

- [x] P10-01. float/clear, 상대/절대/고정 배치, 포함 블록과 stacking context, z-index, overflow/clip을 구현한다. 화면 밖 요소의 표시와 클릭 좌표도 함께 검증한다.
- [x] P10-02. table의 열 폭/행 높이/셀 병합/경계선 모델과 caption을 구현한다. 단순 고정 폭 격자로 처리한 상태를 HTML 테이블 전체 지원으로 표시하지 않는다.
- [x] P10-03. 기능 표의 flexbox, 다단, writing-mode 등 레이아웃 모듈을 모듈별로 구현한다. grid 등 추가 대상으로 정한 모듈은 독립 항목으로 추적한다. 각 모듈의 크기 결정과 배치 사례를 먼저 검증한 뒤 함께 사용한 문서를 검증한다.
  - [x] P10-03-a. Flexible Box Layout Level 1. 입력: 방향/줄바꿈/grow·shrink/정렬/order/gap/기준선/중첩 컨테이너; 기대: 규격 9장 좌표와 화소; 구현: `layout/FlexLayouter.java`, `BoxBuilder`, `PaintWalker`; 완료 조건: `FlexLayoutTest`/`FlexEdgeLayoutTest`/`FlexPaintTest` JRE 6 통과(2026-11-16). 한계는 FEATURE_MATRIX CSS-19.
  - [x] P10-03-b. Multi-column Layout Level 1. 입력: column-count/width/gap/rule/fill/span, 줄·블록 사이 끊기; 기대: 열 배치와 상자 조각; 구현: `layout/MulticolLayouter.java`; 완료 조건: `MulticolLayoutTest`/`MulticolPaintTest` JRE 6 통과(2026-11-16). 한계는 CSS-20.
  - [x] P10-03-c. Writing Modes Level 3 세로쓰기(눕힌 글자). 입력: vertical-rl/lr, sideways-lr, 가로/세로 혼합, 직교 흐름; 기대: 물리 좌표 변환과 글리프 회전; 구현: `layout/OrthogonalLayouter.java`, `render/DisplayCommands.RotatedText`; 완료 조건: `WritingModeLayoutTest`/`VerticalTextPaintTest` JRE 6 통과(2026-11-16). 똑바로 선 CJK 글자는 P10-03-e.
  - [x] P10-03-d. Grid Layout Level 1. 입력: 트랙 정의/자동 배치/이름 있는 선·영역/정렬/절대 배치 자식/단축 속성; 기대: 규격 11장 트랙 크기와 배치; 구현: `layout/GridLayouter.java`, `css/props/Shorthands.java`; 완료 조건: `GridLayoutTest`/`GridSizingTest`/`GridPaintTest`/`GridShorthandTest` JRE 6 통과(2026-11-17, 조정자가 HEAD 스냅샷 전체로 재확인). subgrid·baseline 정렬 등 한계는 CSS-21.
  - [x] P10-03-e. `text-orientation: mixed/upright` 로 CJK 글자를 똑바로 세우고 `text-combine-upright`, 세로쓰기 줄 안의 글자 방향 판정(글리프 세로 advance)을 구현한다. 입력: 한글/한자/가나와 라틴/숫자가 섞인 vertical-rl/lr 문단, `upright` 지정; 기대: 규격 Writing Modes 3 의 mixed(기본) 방향 규칙대로 CJK 는 똑바로(advance = 1em), 라틴은 눕힘, 줄 길이와 줄바꿈과 히트가 일치; 구현: `layout/InlineLayouter`(글자 단위 방향), `render/*`; 완료 조건: 위 입력의 좌표/화소 시험 JRE 6 통과. 이 하위 항목이 끝나야 P10-03 상위 항목을 체크한다(그리드 `baseline` 정렬은 별도 한계로 기능 표에 둔다).
- [x] P10-04. 배경 이미지/다중 배경/그라디언트, border-radius, box-shadow, opacity와 의사 요소를 구현한다. 이미지 로딩 완료 후 필요한 영역을 다시 배치하거나 그린다.
  - [x] P10-04-a. 둥근 모서리(`border-radius`: 타원, 겹침 축소, 둥근 border 고리, `overflow` 둥근 클립). 입력: 네 모서리 반지름/타원/겹침, 둥근 border 와 배경, overflow 클립; 기대: 규격 backgrounds-3 5장 모양과 화소; 구현: `render/RoundedBox.java`, `BoxCommands.java`; 완료 조건: `BoxDecorationPaintTest` JRE 6 통과(2026-11-17).
  - [x] P10-04-b. `box-shadow`(바깥, inset, 블러, 퍼짐, 여러 개, opacity 그룹 안). 구현: `render/BoxShadows.java`; 완료 조건: 같은 시험 통과. 한계는 CSS-11.
  - [x] P10-04-c. 다중 배경 레이어와 `background-*` 6 속성(origin/clip/size/position 1~4 값/repeat `space`·`round`). 구현: `render/BackgroundLayers.java`, `css/props`; 완료 조건: 같은 시험 통과.
  - [x] P10-04-d. 그라디언트(linear/radial/conic/repeating, 색 힌트, 프리멀티플라이드 보간). 구현: `render/GradientSpec.java`; 완료 조건: 같은 시험 통과.
  - [x] P10-04-e. 생성 콘텐츠(CSS 카운터, `::before/::after`, `::first-letter`, `::marker`). 구현: `layout/GeneratedContent.java`; 완료 조건: `PseudoElementLayoutTest` 8 JRE 6 통과(2026-11-17).
  - [x] P10-04-f. `::first-line`(첫 줄에만 적용되는 스타일), 그리고 `opacity`/이미지 로딩 후 재배치를 컴포넌트 수준으로 확인한다. 입력: `::first-line` 의 글꼴/색/배경이 첫 줄에만 적용되는 문단과 줄바꿈이 바뀌는 폭 변경, 배경 이미지가 늦게 도착하는 실제 컴포넌트 페이지; 기대: 규격 pseudo-4 의 첫 줄 규칙, 이미지 도착 후 레이어가 나타남; 구현: `layout/InlineLayouter`, `render/BackgroundLayers`; 완료 조건: 좌표/화소 시험과 `TestHttpServer` 통합 시험 JRE 6 통과. 이 하위 항목이 끝나야 P10-04 상위 항목을 체크한다.
- [x] P10-05. @font-face의 대상 글꼴 형식과 글꼴 로딩/대체 정책을 정의한다. JRE가 읽을 수 없는 웹 글꼴 형식은 필요한 자체 처리를 추적하고, 글꼴 URL도 허용 스키마와 공통 요청 정책을 적용한다.
- [x] P10-06. transform과 transform-origin을 구현하고 입력 좌표에는 역변환을 적용한다. 3D 변환은 P00-05에서 정한 예외 정책을 따른다. 시각적으로 이동한 요소가 원래 위치에서만 클릭되는 오류를 막는다.
- [x] P10-07. transition과 animation의 시간 진행, 반복/지연/종료, 스타일 갱신과 관련 이벤트를 구현한다. 페이지 이동/폐기 시 관련 작업을 정리한다. JavaScript 비활성 설정은 JavaScript 이벤트 핸들러에 적용하고 CSS 자체 애니메이션은 계속 동작하게 한다.
- [x] P10-08. 기능 표의 CSS3 모듈별 미구현 속성/선택자/규칙을 다시 확인하고 각각 구현/검증한다. 기본 속성 검증만 통과한 상태를 CSS3 전체 완료로 표시하지 않는다. 모듈별 근거가 모두 기록되면 P10을 완료 처리한다. 완료(2026-11-19): 하위 항목 a~h 를 모두 구현·검증하고 모듈별 근거를 `docs/progress/cssg.md`(19~34절)와 `webanim.md` 에 기록했다(구조적으로 불가능한 것은 명시적 예외 EXC-09~13 으로 확정). 이로써 P10 단계를 완료 처리한다. 이는 CSS3 전체 완료 표시가 아니다: 기능 표의 CSS 행 다수가 남은 한계 때문에 `부분 구현` 이며 전체 판정은 P22-16 의 대조에서 한다.
  - [x] P10-08-a. CSS-I 의 모듈: `@page`, `backdrop-filter`, 글꼴 기능(`font-variant`), `text-emphasis`, 어두운 색 구성표의 폼 컨트롤, 스크롤 속성(계산 값), 등록된 사용자 정의 속성의 전환·애니메이션, Motion Path, `hanging-punctuation`, `mask-border`, `initial-letter`. 완료(2026-11-17): `docs/progress/cssg.md` 19~29절.
  - [x] P10-08-b. CSS-J 의 모듈: `border-image`, `font-synthesis-*`, `text-justify`, `contain: style`, `image-set()`, CSS `cursor`, Anchor Positioning 기본 형태, 명시적 예외 EXC-09~13. 완료(2026-11-17): `docs/progress/cssg.md` 30절.
  - [x] P10-08-c. CSS-K 의 모듈: `position: sticky`, `background-attachment: fixed/local`, `background-clip: text`, `line-break` loose/normal/strict·`text-wrap: pretty`, `text-decoration-skip-ink`·`text-underline-offset`, `position-area`, `transition-behavior: allow-discrete`와 의사 요소 전환, 교차 오리진 웹 글꼴(CORS), `box-decoration-break: clone`(인라인 조각). 완료(2026-11-18): `docs/progress/cssg.md` 31절.
  - [x] P10-08-d. 남은 후속 중 세로쓰기 텍스트의 `text-emphasis`, `resize` 핸들, `shape-outside` 이미지 모양과 `shape-image-threshold`, `filter`/`clip-path`/`mask` 의 SVG 요소 참조(`url(#id)`), `anchor()` 의 `calc()` 안 사용·암묵 앵커, 다단 조각의 `box-decoration-break: clone`, `font-display` 의 기간 모델(늦게 온 글꼴의 사용 판정). 구현: `layout/**`, `render/**`, `css/**`, `core/PageResize`; 완료 조건: 화소/좌표 시험 JRE 6 통과. 완료(2026-11-19, CSS-L): `docs/progress/cssg.md` 32절, 시험 `ResizingLayoutTest`·`ResizeIntegrationTest`, `ShapeImageLayoutTest`·`ShapeImageIntegrationTest`, `SvgReferencePaintTest`, `VerticalEmphasisPaintTest`, `MulticolCloneLayoutTest`, `AnchorCalcImplicitLayoutTest`, `FontDisplayLateTest`·`FontDisplayIntegrationTest`, 병합 뒤 통합 전체 3644 íµê³¼/0 ì¤í¨/14 ê±´ëë(JDK 6u45). 한계는 cssg 32.4.
  - [x] P10-08-e. WOFF2(`@font-face` 의 `format('woff2')`): Brotli 디코더(RFC 7932, 정적 사전과 변환 표는 `tools/brotlidict` 가 RFC 부록에서 CRC-32 를 대조하며 생성)와 WOFF2 컨테이너·변환된 `glyf`/`loca`/`hmtx` 복원. 입력: 참조 Brotli 인코더(Node.js)의 스트림, 합성 글꼴과 실제 JRE 글꼴(시험 때 변환)의 WOFF2, 독립 인코더 fontTools 가 만든 WOFF2, 규격이 거부하라는 위반·잘림·무작위 변조; 기대: 바이트 단위 복원, 원본과 테이블별·글리프별·Java2D 윤곽선이 같음, 손상은 `IOException` 으로만 끝남; 구현: `media/font/{Brotli,BrotliData,Woff2}`, `layout/WebFontSet`; 완료(2026-11-19): `BrotliTest`, `Woff2Test`(fontTools 파일 포함), `WebFontTest.testWoff2SourceIsDecoded`, `WebFontIntegrationTest.testWoff2FontIsUsedByThePage` JRE 6 통과. 한계: 브라우저가 만든 WOFF2 로는 확인하지 못함, JRE 의 Lucida 글꼴로 만든 파일은 라이선스 때문에 저장소에 두지 않음(cssg 32.5, 32.10, 32.12).
  - [x] P10-08-f. Anchor Positioning 의 위치 대안: `@position-try`(CSSOM 포함), `position-try-fallbacks`/`-order`, 단축 속성 `position-try`, `position-visibility`, `anchor-scope`, `anchor-center`. 완료(2026-11-19, CSS-L): `PositionTryLayoutTest`, `PositionTryPropertiesTest`, `CssomModernRulesTest.testPositionTryRule` JRE 6 통과. 한계(마지막으로 성공한 대안을 기억하지 않음, 대안 최대 16개 등)는 cssg 32.4, 32.6.
  - [x] P10-08-g. `font-display` 의 block 기간에 보이지 않는 텍스트: 웹 글꼴이 오는 동안 block 기간(`auto`/`block` 3초, `fallback`/`optional` 100ms, `swap` 없음)에는 대체 글꼴의 크기로 자리를 잡되 글자를 그리지 않고, 기간이 끝나면 다시 그려 대체 글꼴을 보인다. 입력: 지연 응답 글꼴의 각 `font-display` 값, 기간 중·뒤의 화소, 앞의 쓸 수 있는 패밀리, 기간 중 폐기; 기대: CSS Fonts 4 4.8 절의 기간별 표시, 타이머가 페이지 수명을 넘지 않음; 구현: `layout/{WebFontSet,WebFontSource,FontResolver,InlineLayouter,LayoutBox}`, `render/DisplayListBuilder`, `core/PagePresentation`; 완료(2026-11-19): `FontBlockPaintTest` 5, `FontBlockIntegrationTest` 3(다시 그리기 예약을 뺀 변형으로 실패함을 확인), 병합 뒤 통합 전체 3668 íµê³¼/0 ì¤í¨/14 ê±´ëë(JDK 6u45). 한계(목록 표식·줄임표·폼 컨트롤 글자는 막지 않음, 실제 창 미확인)는 cssg 33.3.
  - [x] P10-08-h. 32.7 이 건드리지 않았다고 기록한 나머지: `cross-fade()`/`element()`, `::file-selector-button`/`::backdrop`/`::highlight()`, `@font-feature-values`, `contain-intrinsic-*`, Motion Path 의 남은 부분, `element.animate()`(Web Animations). 각 항목을 기능 표의 판본에 대조해 구현·검증하거나 근거 있는 명시적 예외(기능 표 3절)로 확정한다. 구현: `css/**`, `layout/**`, `render/**`, `js/**`; 완료 조건: 항목별 계산 값·화소/좌표 시험 JRE 6 통과 또는 예외 근거 기록. 이 하위 항목이 끝나야 P10-08 상위 항목을 체크한다(모듈별 근거가 모두 기록되면 P10 을 닫는다). 완료(2026-11-19): CSS-M 이 `cross-fade()`/`image()`/`element()`(`ImageFunctionsPaintTest` 9), `::file-selector-button`/`::backdrop`(모달 dialog 최상위 계층)/`::highlight()` 와 Custom Highlight API(`HighlightPaintTest` 5, `HighlightApiTest` 4, `PseudoElementsMorePaintTest` 3, `ModalDialogTest` 1), `@font-feature-values` 와 웹 글꼴의 GSUB 대체 글리프(`FontFeatureValuesLayoutTest` 2, `FontFeatureValuesCssomTest` 3), `contain-intrinsic-*` 의 `auto`·논리 속성·기억한 크기(`ContainIntrinsicSizeTest` 3), Motion Path 의 `coord-box`·둥근 상자 경로·`url(#id)`·보간(`MotionPathMoreLayoutTest` 4)을 구현했고(`docs/progress/cssg.md` 34절), WEBANIM 이 Web Animations Level 1(`element.animate()`, `getAnimations`, `Animation`, `KeyframeEffect`, `document.timeline`; 시험 38건, `docs/progress/webanim.md`, 기능 표 CSS-47)을 구현했다. 병합 상태 전체 회귀 3752 통과/0 실패/12 건너뜀(JDK 6u45). 새 명시적 예외는 없고 남은 한계(`content: <image>`, 안쪽 표식 이미지, `CSS.elementSources`, `CSSAnimation`/`CSSTransition` 객체 노출, 하이라이트의 세로쓰기 등)는 기능 표 해당 행의 제약으로 둔다.

### P11. 마우스, 키보드, 링크와 폼

선행 작업: P06, P09. 시각 효과에 관련된 검증은 P10도 필요하다. 산출물: 입력을 DOM 이벤트와 기본 동작으로 연결하는 처리기.

- [x] P11-01. Mouse/Keyboard/Focus/Input/Composition 등의 이벤트 종류, 좌표, 키 값, 보조 키, 버튼, relatedTarget과 전파 여부를 기능 표로 작성한다. AWT 이벤트를 DOM 이벤트로 바꿀 때 스크롤/변환 좌표를 반영한다. 완료(2026-11-18): 기능 표 EVT-03(마우스·휠: 좌표 screen/client/page/offset, button/buttons, 보조 키, detail, relatedTarget, 전파), EVT-04(키보드: key/code/location/repeat/keyCode, 보조 키, `isComposing`), EVT-05(포커스·입력·조합), EVT-11(포인터 이벤트)로 작성했고 실제 AWT 이벤트로 `DomMouseEventTest`, `PageInputTest`, `PointerEventIntegrationTest`, `KeyboardFocusTest`, `KeyboardCodeTest` 를 확인했으며 스크롤/배율/변환 좌표는 `HitTestTest.testScrollAndScaleConversions` 와 변환 hit 시험(P10-06)이 확인한다. 실제 한글 IME 확인은 P11-02/P11-10 에 남는다.
- [x] P11-02. hover/active/focus와 탭 이동을 구현하고 상태 변경을 CSS에 전달한다. IME의 조합 중 입력을 확정된 문자 입력으로 중복 처리하지 않는다. 한글 입력/삭제/조합/포커스 이동을 실제 Swing 환경에서 확인한다. 완료(2026-11-19): hover/active/focus/Tab 은 기존 구현(`KeyboardFocusTest` 등)에 더해 **실제 Swing 창**에서 실제 OS 포인터·키로 `:hover`·`:active`·`:focus`·Tab/Shift+Tab 이동을 확인했다. **실제 Windows 한국어 입력기**(Robot 의 실제 키를 OS 입력기가 조합; 한글 모드 전환만 `InputContext` 공개 API)로 폼 입력란·텍스트 영역·contenteditable 에서 입력/조합 중 삭제/확정/포커스 이동을 확인했고 확정 글이 중복되지 않았다. 이때 찾은 결함(폼 컨트롤의 `compositionend` data 빔·`isComposing` false·`beforeinput` 없음, Space 확정의 두 이벤트)을 고쳤다(`FormImeEventsTest` 5, `EditingInputTest.testSpaceCommitInTwoEventsEndsTheCompositionOnce`). 병합 상태 전체 회귀 3716 통과/1 실패(간헐 `CanvasTextStyleTest.testKerning`, 단독 4/4 통과)/12 건너뜀(JDK 6u45). 기록 `docs/progress/uicheck.md` 2026-11-19, 도구 `tools/uicheck`. 남은 차이: 입력기가 소비한 키의 `keydown`(`Process`/229) 없음, 한/영 키 자체와 한자 후보 창은 미확인(FEATURE_MATRIX EVT-05).
- [x] P11-03. 링크 진입/이탈/활성화를 `HyperlinkEvent.EventType.ENTERED/EXITED/ACTIVATED`로 알린다. HTTP/HTTPS/FTP는 URL과 설명을 제공하고 특수 주소는 URL 객체를 만들 수 없으면 null URL과 유효한 주소 설명을 제공한다.
- [x] P11-04. DOM click 이벤트와 Swing 리스너 통지 후 취소되지 않은 기본 동작으로 링크를 이동한다. 리스너가 직접 새 페이지 이동을 시작했으면 오래된 기본 이동을 추가 실행하지 않는다. `removeHyperlinkEvents()` 후에도 브라우저 자체의 링크 이동은 작동하도록 사용자 통지와 내부 이동 처리를 분리한다.
- [x] P11-05. input/textarea/select/button 등의 상태, 속성과 현재 값, 선택/체크/비활성/읽기 전용, 라디오 그룹, label 연결, tabIndex를 구현한다. 렌더링 엔진이 HTML 요소 상태를 소유하고 편집용 Swing 부품을 사용하더라도 상태와 좌표를 동기화한다.
- [x] P11-06. form의 action/method/enctype/기준 URL, 이름 있는 필드, 체크된 항목과 submit 버튼을 규격대로 직렬화한다. GET/POST, URL 인코딩과 multipart, 문자셋, 파일 선택과 취소를 처리한다.
- [x] P11-07. 폼 검증, submit/reset 이벤트와 기본 동작 취소를 DOM 모델에 연결한다. 프로그램에서 DOM 값이 바뀌면 폼 표시/전송도 갱신되게 하고, JavaScript 객체 연결은 P15에서 수행한다. 파일 읽기는 사용자가 선택한 파일에 한정하고 페이지 스크립트가 임의의 로컬 경로를 열지 못하게 한다.
- [x] P11-08. 텍스트 선택, 캐럿, 복사/붙여넣기와 페이지 확대의 범위를 정하고 구현한다. 클립보드와 파일 선택 같은 OS 기능은 실제 UI 확인 결과를 별도 기록한다. 완료(2026-11-17): 범위와 규칙은 `docs/progress/textsel.md` 1~2절. 마우스·키보드 선택(드래그, 더블/트리플 클릭, Shift, Ctrl+A, 자동 스크롤, `user-select`), 선택 강조, 복사(`copy` 이벤트, 시스템 클립보드 `text/plain`+`text/html`), 페이지 확대(`setZoom/getZoom`, Ctrl+휠/키)를 구현했고 일반 페이지의 캐럿 브라우징은 하지 않기로 정했다(폼 컨트롤 캐럿은 기존 편집기, `contenteditable` 은 P20-06). 시험 `SelectionListenerTest` 6, `TextSelectionTest` 11, `TextSelectionIntegrationTest` 8, `TextZoomTest` 6, 병합 뒤 전체 2740 통과/0 실패. **실제 UI 확인(실제 마우스, 서식 있는 붙여넣기, 한글 IME, 한글 자판의 Ctrl+`+`)은 하지 못했고** textsel.md 4.2 절에 미확인으로 기록했다(P11-02/P11-10 의 IME 확인과 P22-10 에서 사람이 확인).
- [x] P11-09. 링크 활성화로 시작한 이동이 P01의 다운로드 판정에 해당하면 Swing 다운로드 팝업을 띄운다. 팝업은 패널이 속한 창을 부모로 하는 `JDialog`와 `JFileChooser`로 구현하고, 전송은 백그라운드 스레드에서, 화면 갱신은 EDT에서 수행한다. 전송에는 공통 요청 계층(User-Agent, 추가 헤더, 쿠키, SSL 확장 지점, 스키마 제한)을 사용한다. 파일명은 Content-Disposition의 `filename`/`filename*` 또는 URL 경로에서 정하되, 경로 구분자와 OS 예약 이름을 제거하여 선택한 디렉터리 밖에 쓰지 않게 한다. 부분 파일은 임시 이름으로 저장한 뒤 완료 시 대상 파일로 옮기고, 같은 이름의 파일은 덮어쓰기 확인을 거친다. 취소/실패 시 부분 파일을 지우며, 여러 다운로드를 동시에 처리할 수 있게 한다.
- [x] P11-10. 링크 이벤트 순서/취소/등록·제거, 키보드 이동, 한글 편집, 폼 요청 본문을 검증한다. 다운로드 판정, 팝업 표시, 저장, 취소, 실패, 한글 파일명, 크기 불명 응답도 검증한다. 팝업은 대체한 protected 메서드로 자동 검증하고 실제 Swing 팝업 조작은 별도로 확인한다. DOM 검증과 실제 AWT 입력 확인을 각각 기록하고 P11을 완료 처리한다. 완료(2026-11-19): DOM·자동 검증(링크 `PageInputTest`·`HyperlinkDispatcherTest`·`PublicApiContractIntegrationTest`(등록·제거·`removeHyperlinkEvents`), 폼 `FormSubmissionTest`/`FormNavigationIntegrationTest`, 다운로드 `DownloadManagerTest`/`DownloadFileNamesTest`/`DownloadIntegrationTest`/`LinkDownloadIntegrationTest`, 팝업은 대체한 protected 메서드)과 **실제 AWT/OS 입력 확인**을 따로 기록했다(`docs/progress/uicheck.md` 2026-11-18/19): 실제 포인터의 링크 `ENTERED/EXITED/ACTIVATED`(EDT)와 DOM 이벤트 순서, 취소된 링크(이동 없음), 차단 스킴, Tab/Enter 링크 활성화, 실제 IME 한글 입력 뒤 Enter 의 암묵적 제출과 서버가 받은 요청 본문(UTF-8 퍼센트 인코딩, 기본 단추 제출자), 실제 `JFileChooser` 로 고른 파일의 multipart 본문, 다운로드 팝업의 한글 파일 이름·크기 불명·끊긴 응답·덮어쓰기 확인·`Save as`·취소. 찾은 결함(Save as 선택 무시 경쟁, 크기 불명 0%, 실패 때 낡은 진행, 폼 붙여넣기의 `text/html` 누락)을 고쳤다(`DownloadManagerTest.testChoiceWhileTheTransferFinishes`, `SwingDownloadDialogTest.testUnknownSizeBarHasNoPercentage`, `FormClipboardTest.testPasteShowsTheHtmlOfTheClipboard`). 이로써 P11 단계를 완료 처리한다.

### P12. JavaScript 값, 객체, 토큰과 구문 트리

선행 작업: P02. DOM과 분리하여 먼저 개발한다. 산출물: 자체 JavaScript 값 모델, 토큰화기, 구문 분석기.

- [x] P12-01. `JsValue` 계층으로 Undefined/Null/Boolean/Number/String/Symbol/Object를 구분한다. Number는 double 의미를 사용하고 NaN, Infinity, 음의 0을 보존한다. Java의 null 하나로 JavaScript의 null과 undefined를 함께 표현하지 않는다.
- [x] P12-02. JavaScript String은 UTF-16 코드 단위 의미를 유지한다. 코드 포인트 단위 API와 문자열 인덱스/length의 차이를 구분한다. Symbol은 설명이 같은 문자열이어도 서로 다른 값이며 문자열 프로퍼티 키와 분리한다.
- [x] P12-03. 객체의 자체 프로퍼티, 프로토타입, 데이터/접근자 프로퍼티와 writable/enumerable/configurable를 구현한다. 프로퍼티 조회/설정/삭제/열거의 공통 연산을 만들고 모든 내장 객체와 DOM 연결이 이를 사용하게 한다.
- [x] P12-04. 규격의 형 변환, ToPrimitive, 동등 비교/엄격 비교/SameValue 계열과 정수 비트 연산을 공통 함수로 구현한다. Java의 equals, int 변환, 문자열 연결만으로 JavaScript 의미를 대신하지 않는다.
- [x] P12-05. 토큰화에서 Unicode 식별자, 숫자 표기, 문자열/이스케이프, 주석, 줄 끝, 연산자, 템플릿과 정규식 리터럴을 처리한다. 위치/줄/열 정보를 SyntaxError에 제공한다.
- [x] P12-06. 나눗셈과 정규식 리터럴을 구문 문맥으로 구분하고 자동 세미콜론 삽입 규칙을 구현한다. return 뒤 줄바꿈, postfix 연산, 문자열 속 구분 문자 등을 검증한다.
- [x] P12-07. 표현식/문장의 명시적 AST 노드와 연산자 우선순위/결합법칙을 구현한다. 잘못된 토큰과 아직 미구현인 문법은 위치가 있는 오류로 보고하며 빈 문장으로 무시하지 않는다.
- [x] P12-08. Script와 Module 파싱 목표를 구분하고 strict mode, 선언 중복과 기타 early error를 검사한다. 실행 가능한 문법의 범위를 기능 표에 표시한다.
- [x] P12-09. 값 변환/객체 속성/연산자 우선순위/문자열/Unicode/세미콜론/오류 위치를 DOM 없이 검증한다. Rhino 또는 다른 엔진 실행을 제품의 평가 경로에 호출하지 않았는지 확인하고 P12를 완료 처리한다.

### P13. 자체 JavaScript 실행기와 ES6 언어 의미

선행 작업: P12. 산출물: 인터프리터, 실행 환경, 함수/선언/제어 흐름과 ES6 문법 구현.

- [x] P13-01. 우선 AST 인터프리터 방식으로 실행기를 구현한다. LexicalEnvironment/EnvironmentRecord, 실행 문맥, 변수 참조와 Reference 자료형을 분리한다. 최적화나 Java 바이트코드 생성은 정확한 의미 검증 후 별도 작업으로 둔다.
- [x] P13-02. var/함수 선언의 생성과 hoisting, let/const의 블록 스코프와 초기화 전 접근 오류, 재선언과 재대입 제한을 구현한다. Java Map에 이름을 넣기만 하는 방식으로 모든 스코프를 처리하지 않는다.
- [x] P13-03. 산술/비교/논리/비트/조건/할당, short-circuit, 프로퍼티 참조, delete/typeof/in/instanceof와 호출을 구현한다. 평가 순서와 getter/setter의 호출 횟수를 확인한다.
- [x] P13-04. if/switch/for/while/do/for-in, break/continue/label, return/throw/try/catch/finally를 구현한다. 정상 종료/return/throw/break/continue를 명시적인 Completion 자료형으로 표현하여 finally와 중첩 반복문에서 보존한다.
- [x] P13-05. 함수, 클로저, this, arguments, call/apply/bind, new와 프로토타입 연결을 구현한다. strict/non-strict의 this와 arguments 차이를 테스트한다. 함수가 종료되어도 참조되는 상위 스코프는 유지한다.
- [x] P13-06. 객체/배열 리터럴, 축약 프로퍼티/메서드, 계산된 키, 기본 매개변수, rest/spread, 배열/객체 분해 할당, 템플릿 문자열과 태그 템플릿을 구현한다.
- [x] P13-07. 화살표 함수의 this/arguments 캡처와 생성자 호출 제한을 구현한다. 일반 함수를 화살표 표기로 바꾸는 단순 문자열 치환으로 처리하지 않는다.
- [x] P13-08. class/extends/constructor/super, 정적/인스턴스 메서드, getter/setter, 상속과 new.target을 구현한다. 클래스 실행 의미와 초기 오류 조건을 객체 리터럴과 구분한다.
- [x] P13-09. iterator/for-of와 generator/yield/yield*를 구현한다. 중단된 실행 위치와 환경을 명시적으로 보관하고 next/return/throw 및 반복 중 조기 종료의 iterator 정리를 검증한다. generator마다 Java 스레드를 만드는 방식은 사용하지 않는다.
- [x] P13-10. ES6 import/export, 모듈 환경, live binding, namespace, 재수출, 순환 의존성, 인스턴스화와 평가를 구현한다. 모듈 주소 해석과 실제 리소스 요청은 브라우저 연결 계층으로 분리한다.
- [x] P13-11. eval/Function 생성자와 strict mode의 스코프 규칙을 구현한다. 동적 코드도 동일 파서/실행기와 실행 제한을 사용한다. Java 코드나 셸 명령 실행으로 우회하지 않는다.
- [x] P13-12. 실행 단계 수/시간/호출 깊이/배열·문자열 크기 등의 제한과 취소 지점을 구현한다. 무한 반복과 재귀가 EDT를 멈추지 않게 하고, 페이지 이동/폐기 시 실행을 종료할 수 있게 한다. 제한 값과 제한 초과 오류는 문서화한다.
- [x] P13-13. 클로저/TDZ/this/finally/클래스/분해 할당/이터레이터/generator/순환 모듈/동적 코드/취소의 기대 결과를 검증한다. ES2015 문법 목록과 실행 의미 목록을 모두 대조한 뒤 P13을 완료 처리한다.

### P14. ES6 표준 내장 객체와 실행 검증

선행 작업: P13. 산출물: ES2015의 내장 객체/함수와 언어 적합성 결과.

- [x] P14-01. Object/Function/Boolean/Number/String/Array/Error 계열, Math, 전역 상수와 함수의 프로퍼티/생성자/프로토타입 메서드를 규격 목록으로 추적한다. 배열의 빈 슬롯, length 조정과 프로퍼티 순서를 처리한다.
- [x] P14-02. Date의 시간값, 날짜 변환, 파싱, 타임존 경계와 JSON 직렬화를 구현한다. Java Date/Calendar를 사용해도 JavaScript의 범위/변환 규칙을 별도로 검증한다.
- [x] P14-03. RegExp의 구문, 상태, 플래그, lastIndex, 그룹과 관련 String API를 구현한다. Java Pattern과 다른 문법/Unicode/치환 의미를 명시적으로 처리한다. Java Pattern에 입력을 그대로 넘긴 상태를 ES6 정규식 지원 완료로 표시하지 않는다.
- [x] P14-04. JSON parse/stringify의 숫자/이스케이프/순환/undefined/replacer/reviver 규칙을 구현한다. JSON을 eval로 읽거나 Java 객체 직렬화 형식을 웹에 노출하지 않는다.
- [x] P14-05. Map/Set/WeakMap/WeakSet, Symbol과 well-known symbol을 구현한다. 키 비교/삽입 순서/반복 동작을 규격대로 처리하고 WeakMap/WeakSet 때문에 키가 강하게 유지되지 않게 한다.
- [x] P14-06. ArrayBuffer/DataView/TypedArray의 바이트 표현/경계/정렬/엔디언/숫자 변환과 관련 생성자를 구현한다. 타입 배열을 일반 JavaScript Array와 구분한다.
- [x] P14-07. Proxy/Reflect의 동작과 프로퍼티 불변 조건을 구현한다. 원래 객체가 허용하지 않는 결과를 trap이 반환하는 사례의 오류를 검증한다.
- [x] P14-08. Promise의 상태/then 연결/thenable 처리/예외/resolve/reject/all/race와 독립적인 microtask 큐를 구현한다. Promise 콜백을 동기 호출하지 않고, DOM 없는 시험 실행기에서 큐를 소비하여 순서를 검증한다. P15에서는 이 큐를 브라우저 event loop에 연결한다.
- [x] P14-09. ES2015의 Annex B 등 웹 호환 동작을 기능 표에 따로 추적하고 Script/Module 및 strict mode에 따른 차이를 검증한다. 구현한 내장 메서드마다 이름/인수 개수/프로퍼티 속성도 확인한다.
- [x] P14-10. 자체 사례 외에 ES2015 적합성 사례 목록을 작성한다. 외부 검증 자료를 사용할 경우 `tools/` 도구로 내려받고 판본과 이용 조건을 기록하며 허용된 자료만 검증 입력으로 사용한다. 외부 엔진을 실행 의존성으로 넣지 않는다.
- [x] P14-11. 성공/실패/건너뜀/아직 지원하지 않는 사례 수와 이유를 기능별로 기록한다. 전체 ES6 완료는 P13/P14의 대상 목록에 미구현과 설명 없는 건너뜀이 없을 때만 체크한다. 이 조건을 만족하면 P14를 완료 처리한다. (2026-11-25 갱신: 이 단계는 ES2015(ES6) 1차 기반 완료 판정이며, 상향된 프로젝트 최종 목표인 ES2026 및 이전 판본 호환의 전체 완료는 P28(ES2016~ES2022)과 P29(ES2023~ES2026·잔여 항목·이전 ES 호환)의 대상 목록에 미구현과 설명 없는 건너뜀이 없을 때 판정한다.)

### P15. JavaScript와 DOM, 브라우저 작업 흐름 연결

선행 작업: P06~P09, P11, P13, P14. 산출물: 웹 스크립트가 자체 DOM과 렌더러를 제어하는 페이지.

- [x] P15-01. 페이지마다 분리된 JsRealm/Window/Document와 허용된 브라우저 객체를 만든다. DOM을 wrapping하는 객체의 동일성을 유지하여 같은 노드를 두 번 조회해도 JavaScript의 동일 객체 비교가 맞게 한다.
- [x] P15-02. DOM 노드/속성/메서드/컬렉션/Event/EventTarget을 JavaScript 객체 모델에 연결한다. Java 클래스/리플렉션/파일/프로세스/임의 Java 메서드 호출 기능을 자동으로 노출하지 않는다.
- [x] P15-03. innerHTML/outerHTML, createElement/createTextNode, querySelector, classList, style, 요소별 HTML 인터페이스를 연결한다. DOM/스타일 변경 후 필요한 재배치와 repaint가 일어나도록 한다. `getContent()`는 수신 원문을 유지한다.
- [x] P15-04. 인라인 script, 외부 script, 이벤트 핸들러 속성, on... 프로퍼티, addEventListener를 연결한다. 기본/async/defer 스크립트와 DOMContentLoaded/load의 실행 순서를 처리한다. ES6 모듈을 페이지에서 불러오는 방식의 규격과 일반 script와의 차이도 기능 표에 기록한다.
  - [x] P15-04-a. script 발굴 시 취소 가능한 외부 소스 요청을 시작하고 완료 결과를 DOM 소유자에게 전달한다. 입력: 지연된 일반/async/defer 요청과 페이지 교체·비활성화; 기대: 병렬 가져오기와 소유권을 벗어난 DOM 실행 금지, 늦은 콜백 억제; 구현: js.host의 소스 요청 계약, core 스크립트 요청 상태와 파서 제어; 완료 조건: JRE 6 요청 시작/완료 순서·스레드·취소 통합 시험 통과.
  - [x] P15-04-b. async를 준비된 시점에 실행하고 파서 대기/EOF 이후에도 이벤트 루프를 진행한다. 입력: 서로 다른 응답 순서의 async, 지연 일반 script/CSS, defer와 DOMContentLoaded/load; 기대: async를 문서 순서나 EOF 뒤로 고정하지 않음, DOMContentLoaded는 async 완료를 기다리지 않음, load는 필요한 작업을 기다림; 구현: PageScripts/파서 소유자 루프/페이지 로딩 단계; 완료 조건: JRE 6 제어된 HTTP 장벽으로 가능한 순서와 금지 순서를 검증. 현재 completeParsing의 async 목록 순차 실행은 완료 근거가 아니다.
    - [x] P15-04-b-1. 스크립트 범위의 준비 시 실행·문서 큐 인계·동적 스크립트. 입력: 응답 순서를 바꾼 async/defer/차단 스크립트(HTTP 장벽), 느린 async 중 DOMContentLoaded/확정/load, 스크립트가 삽입한 인라인·외부·`async=false` 스크립트, 이동·비활성화; 기대: async는 가져오는 대로(파서 대기 중이거나 확정 뒤 문서 큐에서) 실행, DOMContentLoaded는 기다리지 않고 페이지는 확정되며 load는 스크립트 완료 뒤, 동적 스크립트 순서·중첩 마이크로태스크 지연, 늦은 응답 억제; 구현: `js/host/PageScripts.java`, `ScriptLoads.java`, `ElementExtras.java`, `html/ParserNodeData.java`; 완료 조건: `ScriptLoadingTest` 및 `ParallelScriptLoadingTest`(실제 컴포넌트·HTTP 장벽) JRE 6 통과(2026-11-16).
    - [x] P15-04-b-2. load가 CSS/이미지/프레임 같은 하위 리소스 로딩도 기다린다. 입력: 지연 이미지·스타일시트·iframe 이 있는 페이지의 load/`onload`; 기대: 모든 필수 리소스가 끝나거나 실패한 뒤 load; 구현: 리소스 대기 훅(`PageScripts` 로드 차단자 + `PageResources`/미디어/프레임 완료); 완료 조건: P18-05(이미지 load/error 이벤트), P19-08(진행률 통합), P20-02(프레임) 연결 뒤 JRE 6 HTTP 장벽 시험. 완료(2026-11-17): 지연 이미지 `ImageLoadIntegrationTest.testWindowLoadWaitsForSlowImage`, 지연 스타일시트 `LoadProgressIntegrationTest.testSlowStylesheetKeepsTheLoadOpen`, 지연 iframe `FrameRenderingTest.testParentLoadAndProgressWaitForASlowFrame`(모두 실제 컴포넌트와 HTTP 장벽, 이미지·프레임 실패 시 load 가 영구히 막히지 않음은 `testBrokenImagesFireErrorAndDoNotBlockLoad`, `testFailedFrameShowsErrorInsideTheFrameOnly`), FRAMES 병합 뒤 전체 회귀(2643 통과)에서 통과.
  - [x] P15-04-c. ES2015 모듈 언어와 브라우저 로딩 규칙을 분리하여 module graph/async/defer 및 MIME/CORS/credentials를 연결한다. 입력: 상대/리디렉션 의존성·순환·중복 모듈, 잘못된 MIME·다른 오리진·쿠키; 기대: 그래프별 올바른 1회 평가/순서와 모듈 전용 요청 정책; 구현: ModuleLoader/ScriptSource, 페이지 요청·오리진 정책과 script 준비; 완료 조건: 조회 판본을 기능 표에 고정하고 JRE 6 실제 HTTP 모듈 통합 시험 통과. HTML5 2014에 없는 브라우저 module 호스트 규칙은 WHATWG HTML 조회 판본을 별도로 기록한다.
- [x] P15-05. 파싱 중 script가 DOM을 변경하거나 document.write로 입력을 추가할 때 HTML 파서가 이어서 처리하게 한다. 단순히 문서 파싱 후 모든 script를 한꺼번에 실행하는 방식으로 기본 script를 완료 처리하지 않는다.
- [x] P15-06. task/microtask 큐와 event loop를 구현하고 한 문서의 DOM/JavaScript 변경을 직렬화한다. 타이머/입력/네트워크 완료/Promise 처리 순서와 microtask 실행 지점을 검증한다. EDT 대기와 엔진 대기가 서로 순환하지 않게 한다.
- [x] P15-07. setTimeout/clearTimeout/setInterval/clearInterval과 대상 목록의 animation frame API를 연결한다. 페이지가 교체되면 이전 페이지의 작업을 취소하고 오래된 콜백이 새 페이지 DOM을 바꾸지 못하게 한다.
- [x] P15-08. location/document.URL/document.baseURI, 브라우저 history와 링크/폼 기본 동작을 공통 이동 제어기에 연결한다. `<a download>`와 스크립트 이동의 다운로드 판정도 같은 제어기에서 처리한다. 임의 스키마/다른 오리진/프레임 접근이 이동 정책을 우회하지 않게 한다. 완료(2026-11-17): 하위 a·b·c(스크립트 이동, 링크·폼·`<a download>`) 외에 프레임의 이동도 같은 정책(`NavigationPolicy`/`FrameNavigator`)을 거친다(`FrameNavigationTest` 11건: 차단 스킴 `testBlockedSchemesStayBlockedInFrames`, target `_top`·이름·`window.open`, 프레임 지역 이력, 폼 target, 프래그먼트; `FrameSandboxTest` 7건: allow-forms/popups/top-navigation 없는 샌드박스의 이동 제한), FRAMES 병합 뒤 전체 회귀에서 통과.
  - [x] P15-08-a. 페이지 확정 전 타이머를 보류하고 확정 뒤 문서 큐에서 실행하는 스케줄러를 만든다. 이 하위 기반의 선행 범위는 P03의 문서 큐와 P15-06/07의 Scheduler 계약이다. 입력: 확정 전/후 타이머, 취소·교체·폐기·참조 해제; 기대: 직렬 실행과 늦은 콜백 억제, 공용 시계가 페이지를 강하게 붙잡지 않음; 구현: `core/DocumentScheduler.java`; 완료 조건: JRE 6 큐/수명/ReferenceQueue 시험 통과. P15 단계 전체의 선행·완료 조건은 유지한다.
  - [x] P15-08-b. 파싱 전 PageScripts/HostEnvironment를 만들고 수명·리소스·Storage·CSS를 연결한다. 입력: 파싱 중 script/document.write, 외부/defer/async/module, 타이머/Promise/DOM 변경; 기대: 동일 문서 소유권과 원문 보존, 공통 요청 정책 및 이동 중 취소; 구현: `core/ParsingPageLoader.java`, `HtmlPageDocument.java`, 페이지 런타임; 완료 조건: 실제 SwingWebPanel 통합 페이지 시험 통과.
    - [x] P15-08-b-1. 실행 중 잠금을 기다리지 않는 스크립트 폐기 요청을 분리한다. 입력: 페이지 작업이 잠금을 소유한 동안 중단 요청, 대기 타이머/이벤트와 후속 해제; 기대: 즉시 disposed/실행 취소 표시, 새 실행 억제, 별도 스레드에서 완전 해제 가능; 구현: `js/host/PageScripts.java`; 완료 조건: JRE 6 동시성·콜백/해제 및 기존 JS host 회귀 통과. 런타임 설치는 상위 b의 남은 작업이다.
    - [x] P15-08-b-2. 표준 로더가 HTML 파싱 전에 페이지 실행 환경을 설치하고 컴포넌트의 쿠키·Storage·스크립트 요청·수명과 연결한다. 입력: 파싱 중 DOM 관측/document.write, 외부 script, Promise/타이머, 교체·비활성화·폐기 중 실행; 기대: 원문 보존, 확정 전 타이머 보류, 취소 후 실행 억제와 세션별 저장소; 구현: `core/PageRuntime.java`, `ParsingPageLoader.java`, `HtmlPageDocument.java`, `BrowserSession.java`, `SwingWebPanel.java`; 완료 조건: JRE 6 실제 컴포넌트 통합과 수명 시험 통과.
    - [x] P15-08-b-3. 페이지 CSS/레이아웃 조회와 실제 입력 이벤트를 실행 환경에 연결한다. 입력: getComputedStyle/getBoundingClientRect와 스크롤, 신뢰 click/Promise/취소; 기대: 같은 문서 큐에서 정확한 스타일·뷰포트 좌표와 작업 종료 체크포인트; 구현: `core/PageRuntime.java`, `PageView.java`, `PageInput.java`, `js/host/PageScripts.java`; 완료 조건: 실제 컴포넌트 스타일·좌표·클릭 통합 시험 통과. 외부 CSS 차단과 async/module 실행 순서도 상위 b 완료 전에 검증한다.
      - [x] P15-08-b-3-a. 실제 포인터 click을 신뢰 이벤트와 페이지 작업 종료 체크포인트로 전달하고 중단 직후 JS 리스너를 차단한다. 입력: const/화살표 클릭 카운터, Promise/MutationObserver/타이머, preventDefault, 비활성화 직후 Java DOM/기본 링크; 기대: JS 작업 순서와 신뢰성 유지, 중단된 JS는 재개하지 않고 기본 이동 유지; 구현: PageScripts/DomBridge/JS 리스너, HtmlPageDocument/PageInput; 완료 조건: JRE 6 실제 AWT 전달·그리기·기본 링크 통합 시험 통과.
      - [x] P15-08-b-3-b. 파싱 중 및 확정 뒤 CSS·배치·뷰포트 좌표 조회를 연결한다. 입력: 외부/내부 스타일과 DOM 변경 직후 조회, JScrollPane 이동; 기대: 같은 문서의 최신 계산 스타일과 실제 뷰포트 좌표; 구현: PageRuntime/PageView 및 연결 서비스; 완료 조건: JRE 6 파싱·그리기·스크롤 좌표 통합 시험 통과.
        - [x] P15-08-b-3-b-1. 확정 뒤 동일 페이지 CSS/레이아웃 엔진 조회를 연결하고 DOM 변경 직후 레이아웃을 동기 반영한다. 입력: 타이머/클릭 중 계산 스타일·사각형, 표시/숨김, JViewport 이동·크기; 기대: 문서 큐의 최신 스타일과 뷰포트 기준 border-box 합집합, EDT 선행 통지 여부에 독립적인 확정 후 조회; 구현: PageRuntime/PageView/ParsingPageLoader/SwingWebPanel; 완료 조건: JRE 6 실제 컴포넌트 조회·재배치·스크롤 시험 통과. 파싱 중 조회·외부 CSS 차단은 다음 하위 항목이다.
        - [x] P15-08-b-3-b-2. 파서 소유 중에도 CSS/배치 조회를 제공하고 앞선 차단 스타일시트를 반영한다. 입력: 파싱 중 내부/외부/import 스타일과 조회, 지연 CSS와 이동 취소; 기대: 올바른 파서 순서·문서 소유권·스크립트 차단, 다음 이동을 막지 않는 취소; 구현: 페이지 표현 상태와 로더/뷰 인계; 완료 조건: JRE 6 파싱 시 스타일·좌표·네트워크/취소 통합 시험 통과.
          - [x] P15-08-b-3-b-2-a. 파서와 확정 후 뷰가 동일 페이지 표현 상태를 소유권 인계하도록 분리한다. 입력: 파싱 중 내부 스타일·좌표 조회와 이후 변경/타이머/뷰포트, 파싱 실패·비활성화·폐기; 기대: 단일 CSS/리소스/배치 상태, 원문 보존, EDT 대기 없는 해제와 중단 후 HTML/CSS 유지; 구현: PagePresentation/PageCleanup, PageView/PageRuntime/HtmlPageDocument/ParsingPageLoader; 완료 조건: JRE 6 파싱 중 조회·인계·기존 컴포넌트 회귀 통과. 외부 CSS 대기는 다음 항목에서 검증한다.
          - [x] P15-08-b-3-b-2-b. 앞선 적용 가능한 파서 스타일시트/import 완료를 일반·defer 스크립트 실행 전에 기다린다. 입력: 지연 CSS/import와 내부·외부 script, 미일치 media/비활성/동적 스타일, 교체·JS 비활성화 중 대기; 기대: 실행 시 올바른 스타일/좌표, 불필요한 차단 없음, 즉시 취소 및 다음 이동 보장; 구현: PageResources/페이지 표현과 contextual CSS loader/HostEnvironment/PageScripts; 완료 조건: JRE 6 요청·차단·비차단·취소 통합 시험 통과, 조회한 HTML5 2014 규칙 기록.
  - [x] P15-08-c. location/history/링크/폼과 다운로드를 공통 이동 정책으로 연결한다. 입력: 스크립트 assign/replace/reload/go, 링크 download와 금지 주소/오리진; 기대: 올바른 이력 및 단일 다운로드 판정, 정책 우회 없음; 구현: `core/BrowserSession.java`, 페이지 호스트, `core/PageInput.java`; 완료 조건: 요청·이력·다운로드 통합 시험 통과.
    - [x] P15-08-c-1. 스크립트·링크의 페이지 시작 이동을 공통 이동 정책으로 연결한다(같은 문서 안의 프래그먼트 이동 포함). 입력: `location.assign/replace/href/hash`, `reload`, `history.go/back/forward`, 차단 스킴·네트워크 문서의 classpath 주소, 같은 문서의 앵커 링크 클릭과 뒤/앞 이동; 기대: 허용된 주소만 이동하고 replace는 이력 항목을 더하지 않으며 프래그먼트만 바뀌면 재요청 없이 URL/이력/hashchange/스크롤이 처리됨, 스크립트가 바로 읽는 `location.href`가 새 주소; 구현: `core/NavigationPolicy.java`, `BrowserSession.java`, `PageRuntime.java`, `PageInput.java`, `PageSnapshot.java`, `HtmlPageDocument.java`, `SwingWebPanel.java`; 완료 조건: `ScriptNavigationTest`(실제 컴포넌트·HTTP) JRE 6 통과(2026-11-16). `:target` 선택자 상태는 P11-02 연동으로 남김.
    - [x] P15-08-c-2. `<a download>` 링크와 폼 제출을 같은 정책·다운로드 판정으로 연결한다. 입력: 동일 오리진/교차 오리진 download 링크, 폼 GET/POST 이동과 금지 주소; 기대: 단일 다운로드 판정, 폼 본문 직렬화는 P11-06 에서; 구현: `PageInput`, `BrowserSession`, `NavigationRequest`, `NetworkPageLoader`; 완료 조건: P11-06/P11-09 와 함께 요청·이력·다운로드 통합 시험 통과.
- [x] P15-09. document.cookie를 P04 쿠키 정책에 연결한다. 사용자 에이전트 객체, DOMException, console 및 대상 브라우저 API의 오류를 JavaScript 측에서 올바른 객체로 전달한다.
- [x] P15-10. `setJavascriptEnabled(false)`에서 script, 인라인 핸들러, 타이머와 후속 script 실행을 중지한다. HTML/CSS/링크/기본 폼 기능은 계속 동작한다. true로 되돌려도 이미 중단한 페이지 script를 자동 재실행하지 않고, 다음 콘텐츠 설정/이동/새로고침부터 정상 실행하도록 정한다.
- [x] P15-11. MutationObserver 등 대상 DOM API의 변경 통지를 이벤트 작업 흐름과 연결한다. 원래 콜백을 호출한 뒤 발생한 변경도 규격 순서에 맞게 처리한다.
- [x] P15-12. const/화살표 함수로 클릭 횟수를 바꾸고 요소 텍스트/class/style을 수정하는 페이지를 실행한다. 이벤트 취소, Promise/타이머 순서, JavaScript 비활성, 실행 오류/무한 반복, 이동 중 취소와 원문 보존을 확인하고 P15를 완료 처리한다.

### P16. Storage API와 임시 파일 저장소

선행 작업: P03, P15. 산출물: StorageEntry, 오리진별 Storage와 프로세스 전용 임시 파일.

- [x] P16-01. length/key/getItem/setItem/removeItem/clear와 문자열 변환/존재하지 않는 키 반환을 구현한다. DOM Storage 객체를 일반 Java Map처럼 그대로 공개하지 않는다. 객체 프로퍼티 방식 접근도 규격대로 연결한다.
- [x] P16-02. sessionStorage는 BrowserSession의 인스턴스 필드에 두고 같은 컴포넌트의 오리진별 영역으로 나눈다. JVM 전역 Map에 컴포넌트나 sessionStorage를 강한 참조로 등록하지 않는다.
- [x] P16-03. localStorage는 프로세스별 하나의 관리 객체로 공유하되 저장 데이터만 보관한다. 프로세스마다 구별되는 임시 경로를 만들고 다른 프로세스의 저장 파일을 읽거나 지우지 않는다. 오리진에는 명시/기본 포트 정규화를 적용한다.
- [x] P16-04. 저장 파일에 형식 식별자, 버전, 오리진/키/값과 길이 정보를 명시한다. 문자열은 UTF-8로 읽고 쓰며 길이/항목 수 제한을 검사한다. Java 객체 역직렬화로 파일을 읽지 않는다.
- [x] P16-05. 읽기 시 파일 부재는 빈 저장소로 처리한다. 외부 삭제를 감시하거나 즉시 복구하지 않고, 다음 실제 읽기에서 부재를 처리한다. 파싱 실패/잘린 데이터/알 수 없는 버전은 해당 프로세스 저장소를 초기화한 뒤 사용한다.
- [x] P16-06. 같은 프로세스의 읽기/쓰기/clean을 동기화한다. 임시 파일에 먼저 기록한 뒤 교체하고, Java 6의 rename 동작이 실패할 때 원본과 새 파일을 잃지 않는 복구 절차를 구현한다. 임시 경로 밖으로 쓰기/삭제하지 않는다.
- [x] P16-07. 저장 용량 제한과 초과 오류를 문서화한다. 현재 오리진을 계산할 수 없는 내부 주소의 저장소 정책을 정하고, 다른 웹 오리진에 접근 권한을 주지 않는다. classpath 문서끼리 공유할 범위도 명시한다.
- [x] P16-08. localStorage 변경을 같은 오리진의 다른 활성 문서에 storage 이벤트로 알린다. 변경을 수행한 문서에 같은 이벤트를 중복 전달하지 않는다. 컴포넌트별 sessionStorage의 통지 범위는 별도로 구현한다.
- [x] P16-09. `getLocalStorage`/`getSessionStorage`의 복사본과 `clean()`을 연결한다. 한 컴포넌트의 clean이 프로세스 공용 localStorage를 비우는 동작과 다른 컴포넌트의 조회/통지를 확인한다. 자동 종료 파일 삭제를 요구하지 않는다.
- [x] P16-10. 컴포넌트 두 개의 session 분리/local 공유, 오리진 분리, 다른 프로세스 분리, 파일 외부 삭제, 파일 손상, 동시 쓰기, 용량 초과와 clean을 검증한다. GC 시점 자체를 보장한다고 주장하지 말고 강한 외부 참조 때문에 수거가 막히지 않는지 확인한다. P16을 완료 처리한다.

### P17. MIME별 콘텐츠와 오류 화면

선행 작업: P07~P09, P15. 산출물: HTML/텍스트/Markdown/XHTML 처리기와 오류 페이지 생성기.

- [x] P17-01. Content-Type의 타입/매개변수/charset을 분리하고 대소문자/공백을 처리한다. 네트워크 응답과 `setContent`가 같은 콘텐츠 처리 경로를 사용하게 한다. MIME이 없거나 미지원일 때의 표시 정책도 기록한다. 네트워크 응답 중 주 문서로 표시할 수 없는 것은 P01의 다운로드 계약으로 넘기고, `setContent`의 미지원 MIME은 다운로드 대상이 아니라 P01-17의 입력 오류 정책을 따른다.
- [x] P17-02. text/plain은 태그/스크립트로 해석하지 않고 줄바꿈과 원문을 보존하여 표시한다. HTML처럼 보이는 문자열을 전달해도 스크립트가 실행되지 않는지 확인한다.
- [x] P17-03. text/markdown의 기준 문법과 판본을 정한다. 자체 블록/인라인 파서로 제목, 문단, 목록, 인용, 코드, 강조, 링크, 이미지와 이스케이프를 구현한다. 코드 속 HTML을 실행하지 않으며, 원문 HTML을 허용하는 문법의 처리 정책은 명시한다.
- [x] P17-04. Markdown에서 생성한 HTML은 동일 DOM/렌더링 경로를 사용하고 `getContent()`는 원래 Markdown을 반환한다. 변환된 HTML로 원문과 이력 내용을 덮어쓰지 않는다.
- [x] P17-05. application/xhtml+xml은 XML의 대소문자/네임스페이스/문법 오류를 처리한다. Java 표준 XML 파서를 사용한다면 외부 엔티티와 외부 DTD 접근을 막고 결과를 자체 DOM으로 변환한다. XML 오류를 관대한 HTML 복구로 숨기지 않는다.
- [x] P17-06. 4xx/5xx, DNS/연결/제한 시간, 차단 스키마, 클래스패스 누락, FTP 실패에 대한 오류 화면을 만든다. 응답 코드/주소/재시도 링크를 표시하고, 오류 주소/메시지를 HTML에 넣을 때 이스케이프한다.
- [x] P17-07. 서버 오류 본문과 표시용 오류 DOM을 분리하고 P01의 원문 반환 계약을 지킨다. 오류 페이지 내부 링크도 일반 이동/이벤트 정책을 사용하게 한다.
- [x] P17-08. 네 MIME의 표시/원문/이력 복원/스크립트 동작, 잘못된 XHTML, Markdown 코드, MIME 매개변수, 오류 페이지를 검증한다. 오류 화면이 이전 정상 페이지의 JavaScript나 타이머를 계속 실행하지 않는지 확인하고 P17을 완료 처리한다.

### P18. 이미지, 오디오와 비디오 대체 표시

선행 작업: P04, P09, P11, P15. 산출물: 대상 형식별 디코더, 미디어 상태와 DOM 이벤트, 대체 표시.

- [x] P18-01. 이미지/오디오의 가져오기, 형식 판별, 디코딩, 크기/시간 정보, 표시/재생 상태를 분리한다. 확장자만 믿지 않고 MIME과 파일 시그니처를 함께 검사한다. 실패 종류를 접근 실패/형식 미지원/손상으로 구분한다. 완료(2026-11-17): 이미지·오디오 모두 시그니처 판별과 실패 종류 구분 `MediaFailureKindsTest` 5건(대응표 `docs/progress/media-matrix.md` 4절), 접근 실패는 네트워크 계층·소리 출력에서 `AudioRobustnessIntegrationTest` 로 확인.
- [x] P18-02. ImageIO/Java Sound의 공개 API로 읽을 수 있는 형식을 먼저 연결한다. 실제 JRE 6의 지원을 확인하고, JRE가 제공하지 않는 형식은 자체 디코더 작업으로 기능 표에 남긴다. 자체 디코더는 순수 Java로 작성하고 OS 코덱, 네이티브 디코더, 외부 실행 파일을 호출하지 않는다. 최신 개발 JRE에서 읽힌다는 이유로 JRE 6 지원으로 표시하지 않는다. 완료(2026-11-17): JDK 6u45 의 `ImageIO.getReaderFormatNames()`/`AudioSystem.getAudioFileTypes()`/믹서를 실제 실행해 기록했고(`media-matrix.md` 1절) JRE 가 못 읽는 형식은 모두 자체 디코더로 구현했다.
- [x] P18-03. 이미지 대상 표의 PNG/APNG/GIF/JPEG/SVG/WebP/AVIF 등 조사된 형식과 변형을 각각 구현한다. 이 예시 목록만으로 '모두'를 판정하지 말고 P00의 전체 목록과 대조한다. 애니메이션/투명도/진행형 JPEG/방향 정보/색 처리도 독립 검증한다. 완료(2026-11-18): 방향 정보(JPEG EXIF, PNG `eXIf`, WebP `EXIF`, AVIF `irot`/`imir`/`clap`, CSS `image-orientation`), 색 관리(ICC 행렬/TRC·회색조 프로필, PNG gAMA/cHRM/cICP/iCCP, JPEG/WebP/BMP/AVIF 프로필, CICP), AV1 필름 그레인·인트라 블록 복사·초해상도·인터 프레임 전체(단일/복합 참조, OBMC, 워프, 마스크 복합)·애니메이션 AVIF(`avis`)·계층 이미지(`a1op`/`lsel`)·중첩 오버레이를 libdav1d(이 PC 의 ffmpeg)와 프레임별 비트 단위로 비교해 구현·검증했다(정지 77개 스트림, 시퀀스 39개 321프레임, 애니메이션 파일 11개, 계층 11개; `Av1StreamTest`, `Av1SequenceTest`, `AvifSequenceTest`, `AvifLayerTest`). 병합 뒤 통합 전체 3542 passed, 0 failed, 12 skipped (StaticStateAuditTest was the only failure and was fixed and rerun alone). **남은 한계(기능 표 IMG-* 행에 기록, 대상 판정은 조정자 결정)**: 이중 필터·실제 공간 계층 간 예측·다른 도구가 만든 애니메이션 AVIF 는 비트 단위로 확인하지 못함, ICC 룩업 테이블/CMYK 프로필과 PQ/HLG 톤 매핑 곡선은 적용하지 않음, JPEG 산술/무손실/12비트, BMP BI_JPEG/BI_PNG/RLE24, SVG 의 CSS/SMIL 애니메이션은 브라우저(Chromium)도 일부 지원하지 않거나 별도 기능이라 명시적 한계로 둠(`docs/progress/media-matrix.md` 10.5절).
- [x] P18-04. SVG의 자체 DOM/좌표/도형/경로/문자/변환/스타일 처리와 외부 리소스 제한을 구현한다. SVG 파일을 빈 이미지로 읽은 상태를 지원 완료로 표시하지 않는다.
- [x] P18-05. img/picture/source의 크기, 자연 크기, src/srcset/sizes, 오디오 source 목록의 형식 선택을 처리한다. 로딩 완료/실패 이벤트를 DOM에 알리고 배치에 필요한 크기가 바뀌면 재배치한다.
- [x] P18-06. 이미지/오디오 URL 실패 시 해당 위치에 미지원 URL과 alt가 있으면 그 내용을 표시한다. audio의 alt는 프로젝트 확장 속성으로 처리하고 HTML 자체의 대체 자식 콘텐츠도 고려한다. 깨진 파일명/alt를 HTML로 실행하지 않는다.
- [x] P18-07. 오디오 대상 표의 PCM/WAV/AIFF/AU/MP3/AAC/Ogg 계열/FLAC 등 조사된 코덱·컨테이너를 각각 구현한다. 자체 디코딩 결과를 Java Sound에 전달할 PCM으로 바꾸며, 확장자가 같아도 실제 코덱이 다른 사례를 처리한다.
- [ ] P18-08. audio의 재생/일시정지/종료/탐색/반복/음량/음소거, preload/autoplay/controls와 Media 관련 DOM 상태/이벤트를 대상 규격대로 구현한다. 버퍼와 오디오 라인을 닫고, 긴 오디오 전체를 항상 메모리에 적재하지 않도록 한다.
- [x] P18-09. video 요소는 원래 위치/크기를 확보하여 미지원 비디오 문구를 표시한다. 비디오 디코더나 외부 재생기를 실행하지 않고, 비디오 관련 DOM 호출에는 명시적인 미지원 결과를 제공한다.
- [x] P18-10. 오디오 출력 장치 부재, 디코더 실패, 손상/과대 파일, 접근 실패, 페이지 이동/clean/dispose 도중 재생, 여러 컴포넌트 재생을 검증한다. 디코딩/상태 검증과 실제 장치 청취를 분리하여 결과를 기록한다. 완료(2026-11-17, 자동 검증): 장치 없음 `MediaPlayerTest`/`JavaSoundDeviceTest`/`AudioIntegrationTest`, 디코더 실패 `MediaFailureKindsTest`, 손상/과대/잘린/거짓 헤더 파일·접근 실패·이동/clean/dispose 도중 재생·여러 컴포넌트 동시 재생 `AudioRobustnessIntegrationTest`/`AudioIntegrationTest`. **소리를 귀로 확인하지 못했고** `docs/progress/media-play.md` "미확인" 표에 분리해 기록했다(P22-10 에서 사람이 확인).
- [ ] P18-11. 형식별 입력 자료/기대 픽셀 또는 PCM/상태 변화와 실패 사례를 기록한다. '하나의 이미지와 WAV가 재생됨'으로 전체 미디어 목표를 완료 처리하지 않는다. 비디오 예외를 제외한 대상 표에 미구현이 없으면 P18을 완료 처리한다. 기록(2026-11-19): 오디오는 `docs/progress/media-matrix.md` 11절(MEDIA-D: 실제 인코더 ffmpeg/libmp3lame/AAC/libvorbis/libopus/flac 파일 117개와 ffmpeg 참조 PCM, 무손실 비트 단위·손실 ISO/IEC 11172-4 허용 오차, 거부 형식 4개, 손상 사본, 탐색; `ReferenceDecodeTest` 15건), 이미지는 5·9·10절. 비디오 예외를 뺀 대상 표에 미구현 행은 없으나 P18 의 마지막 항목이므로 P18-08(소리 청취, 사람 필요)이 닫힌 뒤 체크한다(11.7절).

### P19. 이력 복원, 캐시, 새로고침과 진행률 통합

선행 작업: P04, P05, P15~P18. 산출물: 모든 콘텐츠에 동일하게 적용되는 페이지 수명과 캐시 처리.

- [x] P19-01. 이력 항목에 URL/원문/MIME/기준 주소와 필요한 복원 정보를 보관한다. `about:text` 원문을 URL 문자열만으로 복원하지 않는다. 일반 URL의 이력 이동은 캐시 정책을 따르며 캐시가 없으면 다시 요청하도록 한다.
- [x] P19-02. 뒤/앞 이동, 새 이동, 리디렉션, 프래그먼트, 폼 전송, 오류 페이지에서 이력 위치를 검증한다. 프래그먼트 이동은 원문/문서를 불필요하게 다시 내려받지 않도록 별도 처리한다.
- [x] P19-03. 요청 메서드/URL/오리진/응답의 Vary와 관련 헤더를 고려한 캐시 키를 만든다. 다른 사용자/컴포넌트의 응답이나 다른 요청 본문을 같은 URL 캐시로 공유하지 않는다.
- [x] P19-04. Cache-Control/Expires/ETag/Last-Modified와 no-store/no-cache, 조건부 요청/304 처리를 구현한다. 304를 빈 페이지로 표시하지 않고 기존 본문과 새 메타데이터를 결합한다.
- [x] P19-05. 원문, 이미지/CSS/스크립트/오디오, 디코딩 데이터와 레이아웃 캐시의 범위/용량/제거 정책을 분리한다. 캐시에서 제거했어도 화면이 참조 중인 데이터는 안전하게 유지하고, 화면 교체 후 해제한다.
- [x] P19-06. force 새로고침의 이동 번호에 강제 재요청 정책을 붙여 주 문서와 하위 요청에 전달한다. 일반 새로고침, 캐시 사용, 조건부 요청, force 요청을 시험 서버의 요청 횟수와 헤더로 구분해 확인한다.
- [x] P19-07. POST 등 자동 반복에 영향을 주는 요청의 이력/새로고침 정책을 명시한다. 재전송이 필요한 경우 사용자 결정이 필요한 동작과 GET 재요청을 구분하여 구현한다.
- [x] P19-08. 상태/진행률을 주 문서 가져오기/파싱/리소스/스크립트/배치/완료에 연결한다. Content-Length가 없거나 총량이 불명확하면 -1을 반환한다. 이미지를 하나 실패했다는 이유로 로딩 완료 처리를 영원히 막지 않는다.
- [x] P19-09. 로딩 중 취소/오류/새로고침/연속 이동 시 원문/URL/이력/상태가 같은 요청에 속하는지 확인한다. 지연 응답 뒤에 이전 페이지가 다시 나타나는 사례를 검증한다.
- [x] P19-10. 모든 MIME와 FTP/클래스패스/오류 페이지에 대한 뒤/앞 이동과 새로고침을 검증한다. 이력·캐시·진행률이 실제 요청과 연결되어 일관되게 동작하면 P19를 완료 처리한다.

### P20. HTML5와 DOM의 나머지 API 및 문서 호환성

선행 작업: P10~P19. 산출물: P00의 전체 대상 목록에서 아직 구현되지 않은 웹 기능의 구현과 근거.

- [x] P20-01. HTML 요소별 인터페이스/속성 반영 규칙/메서드/기본 동작을 기능 표에 대조한다. document의 제목/준비 상태, 링크/이미지/폼 컬렉션, HTML 문서 생성·변경 등 공통 API를 빠짐없이 추적한다. 완료(2026-11-17): HTML5 2014 REC 의 모든 요소가 전용 인터페이스를 가지고(`HTMLUnknownElement` 로 남는 것은 REC 가 인터페이스를 정하지 않은 과거·비표준 태그뿐) 반영 속성·상태가 구현됐다. 이번에 `console` 전체, `window.event`, `elementsFromPoint`, `clientTop/offsetParent`, `Node.lookupNamespaceURI`, 폐기된 IDL 반영 속성, `details/summary`, 이미지 맵, `meta refresh`, 인라인 SVG, `object` 대체 내용을 구현·시험했다. 남은 것(`HTMLObjectElement.validity`, `HTMLImageElement.x/y`, SVG DOM 등)과 근거는 `docs/progress/htmlifaces-audit.md`.
- [x] P20-02. iframe, srcdoc와 과거 frame/frameset을 별도 브라우징 문맥으로 구현한다. 부모/자식 Window 관계, target 이동, 기준 주소, 오리진, sandbox, 재귀 프레임 제한과 자원 해제를 처리한다. 하위 프레임 이동을 주 문서 이력과 연결할 규칙을 정한다. 완료(2026-11-17): `docs/progress/frames.md`(설계, 단계 b~g). 시험 `core.frames` 패키지 59건(iframe 렌더링·입력·로딩·진행률, 창 관계와 교차 오리진 `SecurityError`, target 이동·`window.open`·프레임 지역 이력, sandbox 토큰, frameset/frame/noframes 배치, 한도(깊이 8, 프레임 64, 자기 자신 포함 취소), 수명·누수). 병합 뒤 TEXTSEL 포함 전체 회귀 2740 통과/0 실패에서 프레임 수명 시험까지 모두 통과(부하가 큰 실행에서는 `FrameLifecycleTest` 가 간헐적으로 시간 초과).
- [x] P20-03. Range/Selection/TreeWalker/NodeIterator와 DOM의 남은 인터페이스를 구현한다. live 상태와 노드 삭제/이동 시의 보정, 선택 영역의 화면 좌표와 텍스트 추출을 검증한다. 완료(2026-11-17): `render/TextSelections.rects/caretRectAt` 로 `Range.getBoundingClientRect/getClientRects`·`Element.getClientRects` 가 실제 글자·상자 기하를 돌려주고(CSSOM View), `DOMRect/DOMRectReadOnly/DOMRectList/DOMPoint*` 가 `instanceof` 가능하며, `Node.normalize()` 의 live range 보정과 `Range.insertNode` 사전 검사, NodeIterator 제거 보정(`replaceChildren` 시험 보강), `Selection.toString()` 의 렌더링 텍스트를 구현했다. 시험 `RangeRectsTest`, `RangeNormalizeTest`, `RangeGeometryIntegrationTest` 외, 통합 후 전체 2921 통과/0 실패(제품 JAR 검사 1건은 이름 변경으로 수정). 상세·한계는 `docs/progress/edit.md`.
- [x] P20-04. DOMParser/XMLSerializer, HTML 조각과 직렬화 기능을 구현한다. 이는 JavaScript에서 사용하는 기능이며 `getContent()`가 변경된 DOM을 반환하는 동작으로 연결하지 않는다.
- [x] P20-05. Canvas 2D의 상태/변환/경로/색/선/그리기/문자/이미지/픽셀 API를 자체 객체와 Java2D로 구현한다. 이미지의 교차 오리진 상태와 픽셀 조회 제한, 크기 변경 시 상태 초기화, TypedArray와의 연결을 검증한다. 상단 요구사항(2D 캔버스 지원, WebGL/WebGPU 미지원)에 따라 `getContext`는 `2d`를 지원하고, `webgl`/`webgl2`/`experimental-webgl`/`webgpu` 요청에는 null을 반환한다. `navigator.gpu`와 WebGL 인터페이스 객체는 노출하지 않는다.
  - [x] P20-05-a. 본문의 요구 전부: 상태/변환/경로/색/선/그리기/문자/이미지/픽셀 API, 교차 오리진 이미지의 오염과 픽셀 조회 제한, 크기 변경 시 초기화, TypedArray 연결, `getContext('2d')` 지원과 webgl/webgl2/experimental-webgl/webgpu 요청의 null, `navigator.gpu`/WebGL 인터페이스 미노출. 구현: `js/host/canvas/**`, `render/CanvasSource`, `core/PagePresentation`; 완료 조건: `CanvasBasicsTest`/`CanvasPathsTest`/`CanvasPaintTest`/`CanvasPixelsTest`/`CanvasPath2DTest` 45건과 `CanvasIntegrationTest`/`CanvasRepaintTest` JRE 6 통과(2026-11-17). 화면 밖 그리기만 확인했으므로 실제 Swing 창 확인은 P22-10 에 남긴다. 한계는 `docs/progress/canvas.md`.
  - [x] P20-05-b. `OffscreenCanvas`, `ImageBitmap`/`createImageBitmap`, `createConicGradient`, `filter`, 텍스트 확장 속성(`letterSpacing` `wordSpacing` `fontKerning` `fontStretch` `fontVariantCaps` `textRendering`), `DOMMatrix`, 히트 영역, `crossorigin` 이미지 요청의 `Origin` 헤더(P20-08-c 의 소켓 연결로 가능). 입력: 각 API 를 쓰는 페이지; 기대: 규격 결과와 화소; 구현: `js/host/canvas/**`, `core/PageResources`; 완료 조건: 화소/상태 시험 JRE 6 통과. 완료(2026-11-17): `OffscreenCanvas`/`ImageBitmap`/`createImageBitmap`/`transferControlToOffscreen`(`CanvasOffscreenTest`), `createConicGradient`(`CanvasExtrasTest`), `DOMMatrix`/`DOMMatrixReadOnly` 와 캔버스 연결(`DomMatrixTest` 9, `CanvasMatrixTest` 4), 텍스트 확장 속성(`CanvasTextStyleTest`: `letterSpacing`/`wordSpacing`/`fontKerning` 은 효과가 있고 `fontStretch`/`fontVariantCaps`/`textRendering`/`lang` 은 상태만 보관), `ctx.filter`(`CanvasFilterTest` 5), `crossorigin` 이미지의 `Origin`·CORS 검사와 캔버스 오염(`CrossOriginImageIntegrationTest`), `drawFocusIfNeeded`/`isContextLost`/컨텍스트 이벤트 핸들러 속성, 히트 영역은 현행 WHATWG 규격에서 삭제되어 구현하지 않고 근거를 기록(EXC-08). 병합 뒤 전체 회귀로 확인. 한계는 `docs/progress/canvas.md` 와 기능 표 WEB-05f~h(실제 Swing 창 확인은 P22-10).
- [x] P20-06. 드래그/드롭, 요소 편집, 데이터 전달, 포커스/입력과 관련된 남은 HTML API와 DOM 이벤트를 구현한다. 사용자 입력을 필요로 하는 기능은 프로그램 호출만으로 실제 입력 검증을 대체하지 않는다. 완료(2026-11-17, 자동 검증): 편집 엔진을 화면 입력에 연결(캐럿·키·마우스·서식 단축키·되돌리기, 한글 조합 `PageComposition`), `cut`/`paste`(편집 호스트와 폼 컨트롤), HTML 끌어다 놓기(`PageDragDrop`, `DataTransfer` 전체). 시험은 실제 AWT 이벤트를 컴포넌트에 보내는 방식으로 `EditingInputTest` 14, `EditingClipboardTest` 7, `FormClipboardTest` 3, `DragDropIntegrationTest` 12, `DataTransferApiTest` 6, 병합 뒤 전체 3030 통과. **실제 입력 확인(한글 IME, 실제 키보드·마우스 드래그, 시스템 클립보드 서식)은 하지 못했다**: `docs/progress/edit.md` 6절에 미확인으로 기록했고 P11-02/P11-10/P22-10 에서 사람이 확인한다. 사람이 해 볼 수 있는 예제 `examples/run.sh EditingExample`.
- [x] P20-07. history.pushState/replaceState, popstate/hashchange와 상태 직렬화의 규칙을 구현한다. 이동 API의 이력과 두 종류의 상태 변경을 연결하고, 오리진을 변경하는 잘못된 주소를 거부한다.
- [x] P20-08. 대상 표의 비동기 웹 요청 API를 공통 요청 계층에 연결한다. 요청 상태/헤더/응답 형식/취소/오류/교차 오리진 정책을 구현한다. 외부 요청 API가 User-Agent, 쿠키, SSL 확장 지점과 스키마 제한을 우회하지 않게 한다.
  - [x] P20-08-a. XMLHttpRequest/fetch/Request/Response/Headers/AbortController/FormData/Blob/File/FileReader/URL/URLSearchParams/TextEncoder/TextDecoder와 공통 요청 서비스 계층(`core/PageWebRequests`). 입력: 같은 오리진/교차 오리진 GET/POST, 쿠키 자격 증명 모드, 금지 헤더/스킴, 취소와 폐기; 기대: User-Agent·추가 헤더·쿠키·SSL 확장 지점·스키마 제한 적용, 늦은 응답은 스크립트를 실행하지 않음; 구현: `js/host/web/**`, `core/PageWebRequests.java`; 완료 조건: JRE 6 `js.host.web` 36건과 `WebRequestApiTest` 15건 통과(2026-11-16). 조회 판본과 제약은 `docs/progress/webapi.md`.
  - [x] P20-08-c. 교차 오리진 사전 요청(preflight)과 `Origin` 헤더. 입력: 안전하지 않은 메서드/헤더(`Content-Type: application/json` 등)의 교차 오리진 fetch/XHR, `Origin` 이 있어야 `Access-Control-Allow-Origin` 을 내는 서버; 기대: OPTIONS 사전 요청과 `Access-Control-Request-*`/`Origin` 전송, 허용 시 본 요청 실행·거부 시 네트워크 오류; 구현: JRE 6 `HttpURLConnection` 이 제한 헤더를 보낼 수 없으므로 `net` 에 `Socket`/`SSLSocket` 기반 HTTP/1.1 클라이언트가 필요(`configureSsl` 확장 지점과의 관계 포함); 완료 조건: 로컬 HTTP/HTTPS 두 오리진 서버로 허용/거부/자격 증명 사전 요청 검증, 이후 P20-08 상위 항목 체크. 완료(2026-11-17): `net/SocketHttpClient`·`SocketHttpURLConnection`·`SocketHttpsURLConnection`·`HostnameMatcher`, `HttpFetcher` 의 사전 요청과 `OriginPolicy.checkPreflight`; 시험 `SocketHttpClientTest` 19건·`CorsPreflightTest` 9건·`WebRequestApiTest` 15건 JRE 6 통과. 한계: JVM 프록시가 적용되면 JRE 연결을 써서 `Origin` 이 사라지고, 이미지/스타일시트 요청은 `Origin` 을 싣지 않는다(docs/progress/net.md). `PATCH` 도 소켓 연결로 보낸다(`SocketHttpClientTest.testFetcherSendsPatch`).
- [x] P20-09. 대상 규격의 Worker/message 관련 기능은 별도 실행 문맥과 제한된 객체 집합으로 구현한다. 메시지의 구조화 복사/전달, 큐, 오류, 종료를 처리하고 Worker에서 DOM을 직접 변경하지 못하게 한다.
  - [x] P20-09-a. 구조화 복제와 창/채널 메시징: `structuredClone`, `window.postMessage`, `MessageChannel`, `MessagePort`, `MessageEvent`, `ArrayBuffer` 전달, 비동기 전달·큐·`start()`/`onmessage`. 구현: `js/host/structured/**`, `js/host/MessagingApi`; 완료 조건: 구조화 복제 16건·메시징 7건 JRE 6 통과(2026-11-17, `docs/progress/domapi-b.md`).
  - [x] P20-09-b. `Worker`/`SharedWorker` 별도 실행 문맥(DOM 없는 제한된 전역), `BroadcastChannel`, `MessagePort` 의 전달(transfer), `targetOrigin` 필터. 입력: 워커 스크립트와 메시지 왕복·오류·종료; 기대: 별도 realm 의 직렬 실행, DOM 접근 불가, 종료 뒤 콜백 없음; 구현: `js/worker/**`; 완료 조건: 시험 JRE 6 통과 후 P20-09 상위 항목 체크. 완료(2026-11-17): `js/worker/**`, 시험 `js.worker` 45건(Broadcast 3, Error 6, GlobalScope 4, Lifecycle 12, Messaging 6, Port 5, Request 3, Shared 6), master 병합 뒤 전체 2588 통과/0 실패/선택적 Opus 원본 벡터 12건 건너뜀. 한계는 `docs/progress/worker.md`.
- [x] P20-10. HTML 규격과 참조 규격에 포함된 나머지 기능을 표에서 확인한다. WebSocket 등의 기능이 추가 스키마를 요구하면 현재 스키마 제한과의 관계를 명시하고 차단/지원 결과를 기록한다. 규격 대상과 제약의 충돌이 남으면 전체 호환 완료 항목은 미완료로 둔다. 완료(2026-11-17): FEATURE_MATRIX 11절이 HTML5 2014 REC 의 참조 규격을 모두 열거해 구현함/스킴 정책으로 차단·실패(WebSocket: EXC-06 연결 실패)/범위 밖(근거)/미구현으로 분류했고 마이크로데이터를 구현했다. 미구현으로 남은 것은 새 항목 P20-13 에 모았다.
- [x] P20-11. SVG/MathML, 각종 문서 모드, 오래된 HTML 속성, 프레임, 폼/표, 현대 문서를 섞어 검증한다. 미지원 플러그인 콘텐츠는 대체 내용을 표시하고 실제 플러그인 실행을 지원했다고 기록하지 않는다. 완료(2026-11-17): `MixedDocumentsIntegrationTest` 6건(SVG+MathML+표+폼+iframe+object+video 혼합 HTML5 페이지, 호환 모드 구식 페이지, XHTML+SVG, DOCTYPE 8종 `compatMode`, frameset, 플러그인 대체 내용)과 `DetailsMapRefreshIntegrationTest` 3건. 시험이 드러낸 결함(닫힌 `details`, 이미지 맵 클릭, `meta refresh`, 인라인 SVG, `object` 대체 내용)은 고쳤다.
- [x] P20-12. JavaScript 언어/DOM/DOM 이벤트/HTML 표시/CSS 표시/미디어의 각 축별로 미구현·부분 구현·검증 완료를 대조한다. 명시적 예외(비디오, 3D 가속/WebGL/WebGPU)는 미구현과 구분하여 예외 근거와 함께 적는다. 이 단계의 대상 규격에서 남은 항목을 모두 처리한 뒤 P20을 완료 처리한다. 대조 완료(2026-11-17): FEATURE_MATRIX 13절에 축별 개수표(검증 완료/부분/미구현/예외: ES 33/3/0/0, DOM 23/16/1/0, DOM 이벤트 5/7/1/0, HTML 표시 23/18/0/1, CSS 표시 14/29/2/0, 미디어 18/3/0/1, API·프로토콜 47/2/0/1)와 남은 행 ID 목록, 명시적 예외 EXC-01..07 을 근거와 함께 적었다. 이 항목은 대조 자체이며 P20 단계 전체는 미구현·부분 구현이 남아 있어 완료로 표시하지 않는다(P20-05-b, P20-13 등 개별 항목에서 처리).
- [x] P20-13. 기능 표 11~13절이 미구현으로 남긴 웹 플랫폼 기능을 구현한다. 입력: `EventSource`(서버 전송 이벤트: 스트리밍 응답 읽기, 재연결, `Last-Event-ID`), `PointerEvent`(마우스형 포인터의 `pointerdown/move/up/enter/leave/over/out/cancel`, `setPointerCapture`), 페이지 수명 이벤트(`pageshow/pagehide/beforeunload/unload/visibilitychange`, 창 `focus/blur/online/offline`), `IntersectionObserver`/`ResizeObserver`, XPath(`document.evaluate`), 텍스트 트랙/WebVTT(`track`, `TextTrack`, `TextTrackCue`), `performance.mark/measure/timing`, 인터페이스 객체 이름(`Navigator/Location/History/Screen/Performance`, `DOMStringMap`, `MutationRecord`), 키보드 `code`/`isComposing`; 기대: 각 규격의 상태·이벤트 순서와 스킴 정책(요청 계층)·수명 정리; 구현: `js/host/**`, `core/**`, `dom/**`; 완료 조건: 기능별 JRE 6 시험 통과와 기능 표 갱신. Custom Elements/Shadow DOM 은 HTML5 2014 REC 이후 규격이므로 이 항목의 대상이 아니다(확장 목록 EXT 로 기록). 완료(2026-11-18): `EventSource`(`EventSourceTest` 14, `EventSourceIntegrationTest` 7), `PointerEvent`(`PointerApiTest` 5, `PointerEventIntegrationTest` 5; 끌기 시작 때 대기 중인 포인터 캡처 결함을 고침), 페이지 수명 이벤트(`PageLifecycleTest` 9, `PageLifecycleIntegrationTest` 9; 수명 결함 3건 수정), `IntersectionObserver`/`ResizeObserver`(`RenderObserversTest` 18, `RenderObserversIntegrationTest` 3), `performance`(`PagePerformanceTest` 6, `PerformanceIntegrationTest` 1), 인터페이스 객체 이름(`InterfaceNamesTest` 3), 키보드 `code`/`isComposing`(`KeyboardCodeTest` 2), XPath(`XPathApiTest` 13), 텍스트 트랙/WebVTT(`VttParserTest` 7, `VttCueTextTest` 6, `TextTrackTest` 16, `TextTrackIntegrationTest` 2) 를 구현·검증했다. 병합 뒤 통합 전체 3439 통과/0 실패/12 건너뜀. 한계(자막을 그리지 않음, 비디오 트랙은 구조만, 실제 한글 IME 미확인 등)는 기능 표 VID-02/HEI-08/EVT-04 와 `docs/progress/webapi2.md`.

### P21. clean, dispose, GC와 자원 정리

선행 작업: P16, P18, P19. P20에서 구현한 기능의 자원도 함께 검토한다. 산출물: 페이지 교체/정리/폐기 시 누수가 없는 수명 관리. P20 기능이 이후 추가되면 해당 자원의 정리 검증을 다시 수행한다.

- [x] P21-01. 페이지가 소유하는 요청/스트림/DOM/JsRealm/리스너/작업 큐/타이머/미디어/프레임/캐시 참조와 컴포넌트가 소유하는 다운로드 작업/팝업의 목록을 작성한다. 페이지 교체, clean, dispose, 취소에서 각 항목을 정리하는 담당 클래스를 표로 정한다.
- [x] P21-02. clean은 모든 진행 중 작업의 요청 번호를 무효화한 뒤 빈 페이지/이력을 확정하고 대상 캐시/Storage를 지운다. cleanup 도중 완료된 비동기 작업이 이전 화면/원문/저장소를 복원하지 못하게 한다.
- [x] P21-03. dispose는 clean 처리를 수행하고 모든 사용자 이벤트 등록, 자체 DOM 이벤트, 타이머/작업/오디오/하위 프레임, 진행 중 다운로드와 다운로드 팝업을 해제한다. Hyperlink뿐 아니라 Mouse/Key/Focus/PropertyChange 등 이 객체의 사용자 등록 리스너도 P01의 대상 표에 따라 제거한다. 완료(2026-11-17): 사용자 AWT/PropertyChange/Hyperlink 리스너 제거 `SwingWebPanelTest.testDisposeRemovesAttachedListeners`, 타이머 `PageRuntimeIntegrationTest.testNavigationAndDisposeCancelRunningTimer`, 오디오 `AudioIntegrationTest.testNavigationCleanAndDisposeCloseTheOutput`, 진행 중 다운로드와 팝업 `DownloadIntegrationTest.testDisposeCancelsARunningDownload`(새로 추가), 하위 프레임 `FrameLifecycleTest`(dispose 도중 로딩, 프레임 스크립트 정지, 수거), worker `WorkerLifecycleTest.testDisposeTerminatesAllWorkers`, 반복 호출 `LifecycleTest.testDisposeIsIdempotentAndLeavesEmptyState`.
- [x] P21-04. clean/dispose를 반복하거나 로딩/이벤트 처리/스크립트 실행/오디오 재생 도중 호출해도 예외 누수/교착/이중 해제가 발생하지 않게 한다. 폐기 완료 뒤 도착한 콜백은 상태를 바꾸지 못하게 한다.
- [x] P21-05. static 저장소, executor, AWT/Swing 타이머, 백그라운드 스레드가 컴포넌트를 영구히 붙잡지 않는지 검사한다. 필요한 비동기 연결은 WeakReference와 수명 토큰으로 끊고, 프로세스 공용 localStorage가 BrowserSession을 참조하지 않게 한다.
- [x] P21-06. finalizer의 호출 시점이나 명시적 dispose만으로 sessionStorage의 GC 요구를 처리하지 않는다. 컴포넌트와 그 세션이 외부 루트 없이 수거 가능한 구조를 만들고, 남은 작업도 유한하게 종료되도록 한다.
- [x] P21-07. 반복 생성/이동/clean/dispose에서 열린 소켓/스레드/오디오 라인 수와 메모리 참조를 확인한다. WeakReference/ReferenceQueue 확인은 GC의 비결정성을 감안하여 근거를 기록하고, 정확한 특정 시간에 GC가 실행된다고 보장하지 않는다.
- [x] P21-08. 정리 후 캐시/Storage/원문/이력/이벤트와 유지 대상 설정의 상태를 P01 계약에 대조한다. 자원 정리 검증과 모든 호출 경로의 처리가 끝나면 P21을 완료 처리한다.

### P22. 최종 통합 검증, 사용 문서와 배포 준비

선행 작업: P00~P21. 산출물: 기능별 검증 근거, 사용 예제, 빌드 가능한 독립 JAR, 완료/미완료 인계.

- [x] P22-01. 공개 API 대응표에서 `setUserAgent/getUserAgent`, `setPage/getPage`, `setJavascriptEnabled`, `getContent/setContent`의 초기값/정상/오류/호출 시점/JavaScript 변경 영향 사례를 확인한다.
- [x] P22-02. `back/back(int)/forward/forward(int)/getHistories`, `refresh/refresh(boolean)`의 경계/복원/분기 이력/리디렉션/force와 캐시 사례를 확인한다.
- [x] P22-03. `getStatusText/getProgress`, `addHyperlinkListener/removeHyperlinkListener/removeHyperlinkEvents`, `clean/dispose`의 상태 전환/통지/취소/반복 호출 사례를 확인한다.
- [x] P22-04. `addHeaders/getHeaders/getCookies`, `getLocalStorage/getSessionStorage`의 복사본/대소문자/헤더 덮어쓰기/User-Agent 제외/쿠키 범위/저장소 공유·분리/손상 복구 사례를 확인한다. 상단 메서드에 검증 근거가 누락된 것이 없는지 대조한다.
- [x] P22-05. 로컬 시험 페이지 하나에 외부 CSS/스크립트/이미지/오디오, const/화살표 함수/Promise, DOM 이벤트/변경, Storage, 링크와 폼을 넣는다. 실제 요청/쿠키/원문 반환/화면 갱신/스크롤/뒤·앞 이동/새로고침/폐기를 끝까지 확인한다.
- [x] P22-06. HTTP/HTTPS/FTP/클래스패스/특수 주소와 네 MIME, 404/500, 리소스 실패/alt, 비디오 미지원 화면, WebGL/WebGPU를 요청하는 페이지, 다운로드 팝업(첨부 응답, 표시할 수 없는 MIME, `<a download>`)의 통합 사례를 실행한다. SSL 메서드의 하위 클래스 재정의도 함께 확인한다.
- [ ] P22-07. JRE 6(기본 `C:\Program Files\Java\jdk1.6.0_45` 또는 지정한 JDK 6)에서 실제 실행하여 연결/파싱/렌더링/JavaScript/오디오/저장소를 확인한다. JRE 6의 TLS 지원 범위 때문에 실제 HTTPS 사이트가 실패할 수 있으므로 그 결과와 HTTP 재시도 동작을 따로 기록한다. 더 새 JRE와 최소 대상 JRE의 차이를 기록한다. class 파일 버전 50 확인과 컴파일 통과만으로 JRE 6 실행 검증을 대체하지 않는다.
- [x] P22-08. `com.sun`/`sun`/Rhino/외부 엔진·JAR, 네이티브 코드(C/C++ 소스, JNI/JNA, 네이티브 라이브러리)와 외부 프로그램 실행 의존성, Java 6 이후 API/문법, 빠진 영어·한국어 Javadoc, 중요한 분기/반복문의 한국어 주석과 파일 인코딩을 검사한다. `tools/`의 각 도구도 같은 기준으로 검사하고, `AGENTS.md`/`README.md`가 있으며 현재 동작과 맞는지 확인한다.
- [x] P22-09. 크거나 깨진 HTML/CSS/JavaScript/미디어, 순환 DOM/리디렉션/모듈/프레임, 무한 스크립트, 지연 응답과 연속 이동을 확인한다. 각 제한 초과가 명시적인 오류/취소로 끝나고 UI가 반응하는지 검증한다. 완료(2026-11-17), 근거는 모두 JDK 6u45 통합 시험이다: 매우 깊은 중첩·엄청난 요소/규칙 수·리디렉션 순환과 끝없는 사슬·끝없는/욕심 많은 스크립트·메모리 한도를 넘는 응답·느린 응답 뒤의 다음 이동 `RobustnessIntegrationTest`(위 5건; 각 사례마다 EDT 왕복 감시), 깨지거나 빈 파일·종류가 틀린 파일·10만 x 10만 화소를 약속하는 PNG 머리·잘린 PNG·잡음 오디오 `RobustnessIntegrationTest.testBrokenAndOversizedMedia`(새로 추가: 이미지 5개와 오디오 1개가 error 이벤트로 끝나고 올바른 이미지는 load), 디코더 손상 입력(`WebpRobustnessTest`, AVIF 손상 24,000건, `OpusRobustnessTest` 등), 순환 모듈 `ModuleScriptTest.testDuplicateModulesAndCycles`·`ModuleTest.testCyclicDependencies`, 순환 DOM 삽입 거부 `TreeMutationTest.testHierarchyAndNotFoundErrors`, 순환/깊은/많은 프레임 `FrameRenderingTest.testPageThatEmbedsItselfStopsAtTheFirstFrame/testNestingStopsAtTheDepthLimit/testPageHasAtMostSixtyFourFrames`, 지연 응답과 연속 이동 `PendingNavigationIntegrationTest.testRefreshAndQuickNavigationsWinOverALoad/testLateResponseNeverBringsTheOldPageBack`, worker 무한 반복 `WorkerLifecycleTest.testEndlessLoopIsAbortedByTheTaskLimitAndTheWorkerLivesOn`.
- [ ] P22-10. 렌더링/입력/오디오의 자동 검증 결과와 실제 Swing/OS/오디오 장치에서 확인한 결과를 구분하여 기록한다. 한 계층의 성공을 다른 계층의 실제 동작 근거로 사용하지 않는다.
- [x] P22-11. README와 사용 예제에 최소 Java 버전, 빌드/실행 명령, JPanel/JScrollPane 사용, 공개 메서드, 비동기 이동, 상태 관찰, SSL 재정의, 다운로드 팝업과 그 대체 방법, 저장소/clean 영향, dispose와 원문 반환 동작, 비디오/3D 가속/WebGL/WebGPU 미지원을 작성한다. 영어 문서를 추가하면 한국어 문서의 동작 설명과 함께 갱신한다.
- [x] P22-12. 사용자 예제의 모든 메서드/이벤트/타이머도 영어·한국어 Javadoc과 필요한 한국어 설명을 작성한다. 예제가 폐기 동작까지 보여주도록 하고, 예제 실행 때문에 외부 엔진을 필요로 하지 않게 한다.
- [x] P22-13. 제품 JAR에 실행에 필요한 자체 클래스/리소스만 포함하고 테스트와 `tools/`의 도구는 넣지 않는다. 별도 외부 라이브러리가 없어도 최소 예제가 빌드되고 실행되는지 확인한다. JAR의 클래스패스 HTML과 상대 리소스도 확인한다.
- [x] P22-14. FEATURE_MATRIX에 실제 검증된 판본/환경/사례와 남은 호환성 차이를 적는다. 미구현이나 설명 없는 건너뜀, 상단 제약과 충돌하는 기능이 남으면 전체 목표는 완료로 표시하지 않는다. 기록 완료(2026-11-17): 기능 표의 계획용 번호(`T-...`/`C-...`)를 실제 시험 클래스 이름으로 바꾸고 인용한 시험 430개가 존재하고 통과하는지, 인용한 제품 클래스가 `src` 에 있는지 대조했으며 낡은 `미구현` 행 74개를 현실에 맞췄다. 14절에 환경·시험 수·남은 호환성 차이를 적었다. 남은 미구현·부분 구현이 있으므로 **전체 목표 완료(P22-16)는 표시하지 않는다**. 기능 표는 구현이 진행될 때마다 계속 갱신한다.
- [ ] P22-15. PROGRESS에 최종 변경 파일/검증 명령/결과/미해결 작업 번호/필요 환경을 인계한다. 변경 내용을 검토한 뒤 공통 작업 규칙의 형식으로 commit하고 push는 사용자에게 맡긴다.
- [ ] P22-16. 상단 요구사항의 각 행이 구현 파일과 성공한 검증 사례에 연결되고, 명시된 비디오, 3D 가속/WebGL/WebGPU 예외와 런타임 제약 외의 미완료 목표가 없는지 최종 대조한다. 이 조건과 모든 선행 단계가 충족될 때만 P22 및 전체 개발 완료를 표시한다. (2026-11-20 추가, 2026-11-22·2026-11-24·2026-11-25 갱신) 상단 ES2026 및 이전 버전 ES 호환 목표를 완성하는 P29를 포함해 아래 P23~P29 목표가 모두 완료되어야 P22 및 전체 개발 완료를 표시한다.

### P23~P29 목표의 공통 사항과 진행 순서

P23~P26은 2026-11-20, P27은 2026-11-22, P28은 2026-11-24 사용자 요청으로 추가했고, **P29는 2026-11-25 상단 JavaScript 규격 목표가 `ES6`에서 `ES2026`(및 이전 버전 ES 호환)으로 상향됨에 따라 신설한 필수 규격 완성 단계**이다.

지금까지 완료한 단계 순서는 `P24`(콘솔 출력 이벤트) → `P25`(Web Audio API, 사람 청취 확인 `P25-07` 제외 완료) → `P26`(독립 JavaScript 엔진 분리·`javax.script`) → `P23`(Maven 표준 구조 전환, 2026-11-24 완료) → `P28-01~05`(ES2016~ES2022 파서·실행 의미·BigInt·내장 객체, 2026-11-25 완료)이다.

**2026-11-25 기준 남은 작업의 진행 순서**는 다음과 같이 재조정한다.

1. **`P29`(`P29-01` ~ `P29-10`, ES2026 규격 완성 및 이전 ES 호환 검증) — 최우선 진행**:
   - 상단 핵심 요구사항인 ES2026 및 이전 판본 호환을 먼저 충족한다.
   - 직전 `P28-01~05`에서 ES2022까지 확장한 엔진(`js.parser`, `js.ast`, `js.interp`, `js.builtins`, `js.regexp`)의 흐름을 끊지 않고 ES2016~ES2022 잔여 항목과 ES2023~ES2026 문법·실행 의미·이진 데이터·컬렉션·정규식·JSON 모듈·이전 판본 호환을 한 번에 완성한다.
   - P13-01 원칙("최적화는 정확한 의미 검증 후 별도 작업으로 둔다")에 따라 가변 길이 `ArrayBuffer` witness record, `Iterator` 헬퍼, JSON 모듈, 정규식 `/v`·인라인 수정자 등 ES2026 언어 의미를 모두 확정한 뒤에 `P28-07`의 인터프리터 캐시·성능 최적화를 수행한다.
2. **`P28` 잔여 항목(`P28-06` ~ `P28-09`, 부족한 웹 API·대규모 스크립트 성능 최적화·게임 구동 검증)**:
   - `P29`로 완성된 ES2026 엔진 위에서 부족한 웹 API(`P28-06`: `AbortSignal` 리스너 옵션, `ReadableStream` — ES2026 `Array.fromAsync`/`for await` 연동 포함, `navigator.clipboard`, `URL.createObjectURL` 정책)를 보강하고, 대규모 스크립트 성능 측정·최적화(`P28-07`) 및 NetstormJava 번들·ES 모듈 구동 검증(`P28-08`), 문서 정리(`P28-09`)를 수행한다.
3. **`P27`(`P27-01` ~ `P27-06`, Java 호스트와 페이지 스크립트 간 `postMessage` 통신)**:
   - `P28-04`(`BigInt`)와 `P29`(`Float16Array`, 가변 `ArrayBuffer`, `SharedArrayBuffer` 정책 등)로 확정된 ES2026 값 모델과 구조화 복제 규칙 위에서 호스트↔페이지 양방향 값 변환과 메시지 전달을 한 번에 구현·검증한다.
4. **`P22` 및 실환경/사람 확인 마무리(`P18-08`/`P18-11`, `P25-07`, `P22-07`/`P22-10`/`P22-14` 재집계/`P22-15`/`P22-16`)**:
   - Maven 빌드 산출물을 기준으로 실제 오디오 청취·JRE 6 실환경 확인과 기능 표 전수 대조를 마친 뒤 전체 개발 완료를 판정한다.

상단 "프로젝트 목표", "목표 - 지원 기능", "주의사항" 단락은 수정하지 않으므로, 공개 API와 세부 규격 항목은 이 계획과 `docs/API_CONTRACT.md`, `docs/FEATURE_MATRIX.md`에 행을 추가하여 추적한다. 상단 제약(Java 6/JRE 6 동작, 외부 라이브러리·네이티브 코드·`com.sun`/`sun` 금지, 영어·한국어 Javadoc과 한국어 주석, 파일 인코딩, commit 규칙)과 공통 작업 규칙은 P23~P29의 모든 항목에도 그대로 적용한다. P22의 사람 확인 항목(`P22-07`/`10`)과 최종 마감(`P22-15`/`16`)은 P29·P28·P27 진행을 막지 않는다.

### P23. Maven 프로젝트 전환

선행 작업: P02, P22-13. 산출물: `pom.xml`(부모/모듈), Maven 표준 디렉터리 구조, JDK 6 대상 빌드·시험·패키징, 갱신된 스크립트·도구·문서.

2026-11-24 사용자 요청으로 P23 Maven 전환을 먼저 진행했다. P02와 기존 작업 항목의 옛 경로는 [Maven 전환 기록](docs/progress/maven.md#모듈과-이전-경로)의 대응표에서 확인한다. 도구는 각자 기존 디렉터리와 독립 빌드를 사용한다.

- [x] P23-01. Maven 도입 방식을 조사하여 결정하고 기록한다. JDK 6에서 실행 가능한 마지막 Maven(3.2.5)은 JDK 6u45의 JSSE가 TLS 1.2를 지원하지 않아 HTTPS 전용인 Maven Central 접근이 실패할 수 있으므로 이를 실제로 확인한다. 기본안은 Maven 자체는 더 새 JDK에서 실행하고, 컴파일은 `maven-toolchains-plugin`(toolchains.xml) 또는 `maven-compiler-plugin`의 `fork`/`executable`로 JDK 6의 `javac`를 사용하며(`-encoding UTF-8 -source 1.6 -target 1.6 -g`), 시험 실행도 JDK 6의 `java`로 하는 것이다. 사용할 Maven/플러그인 판본과 각 판본이 요구하는 실행 JDK를 기록한다. 대상 JDK 6을 찾지 못하면 다른 JDK로 조용히 대체하지 않고 빌드가 오류로 끝나게 한다. Maven 플러그인은 빌드 도구일 뿐 제품 런타임 의존성이 아니며, 제품 POM의 `dependencies`는 비워 외부 라이브러리 의존성 없음 원칙을 지킨다. 완료(2026-11-22, 조사·결정만; 빌드 검증은 P23-02 이후): `tools/webfetch` 로 Maven Central 에 JDK 6u45(`SSLHandshakeException handshake_failure`), JDK 7u80(`PKIX path building failed`)로는 접속하지 못하고 JDK 8u202 로는 접속됨을 실제로 확인했다. 결정: Maven 3.10.1(3.9 이상, JDK 8 이상으로 실행)을 JDK 8 로 실행하고 컴파일은 toolchains 의 JDK 6 `javac`(`-source 1.6 -target 1.6`), 시험은 `exec-maven-plugin`(3.7.0)으로 JDK 6 의 `java` 에서 자체 실행기를 실행, 합친 JAR 은 `maven-shade-plugin`(3.6.2), 플러그인 판본(compiler 3.16.0, toolchains 3.3.0, jar 3.5.1, resources 3.5.0)과 요구 JDK(모두 Maven 3.6.3+ / JDK 1.8)를 기록했다. JDK 6 을 찾지 못하면 빌드는 오류로 끝난다. 모듈 구성은 부모 POM + `swingwebpanel-js`(`engine/`) + `swingwebpanel` + 예제. **이 PC 에는 Maven 이 설치되어 있지 않아 P23-02 이후는 진행하지 못한다**(사용자가 설치하기로 함). 기록 `docs/progress/maven.md`.
- [x] P23-02. 디렉터리를 Maven 표준 구조로 옮긴다. 제품 소스는 `src/main/java`, Java가 아닌 제품 리소스(UA 스타일시트, 템플릿, 생성 표 등)는 `src/main/resources`, 시험은 `src/test/java`, 시험 자료는 `src/test/resources`로 옮긴다. 예제(`examples`)는 제품 JAR에 넣지 않는 별도 모듈로 둔다. `tools/`는 각 도구의 sh/bat 빌드를 유지하고 제품 reactor와 산출물에서 분리하는 것을 기본으로 한다. P26의 엔진 분리 때 전체 소스를 두 번 옮기지 않도록 처음부터 부모 POM(집계, `packaging=pom`)과 하위 모듈 구조로 시작하는 것을 기본안으로 한다(예: 제품 모듈 `swingwebpanel`, 예제 모듈 `swingwebpanel-examples`; P26에서 엔진 모듈 추가). 이동은 `git mv`로 이력을 보존한다. 미병합 작업 브랜치가 있으면 전환 전에 병합할지 전환 후 새 경로로 옮길지 정한다.
- [x] P23-03. POM을 설정한다. 기본 좌표는 groupId `org.duckdns.hjow`, artifactId `swingwebpanel`, 판은 현재 MANIFEST의 `Implementation-Version`(0.1.0)과 맞춘다. `project.build.sourceEncoding`/`project.reporting.outputEncoding`을 UTF-8로 하고, 제품 JAR 이름(`swingwebpanel.jar`)과 MANIFEST 항목은 현재 `build.sh` 결과와 같게 한다. Java가 아닌 리소스가 빠짐없이 JAR에 들어가고 시험 클래스·시험 자료·도구가 들어가지 않게 한다.
- [x] P23-04. 시험 실행을 연결한다. 자체 실행기 `org.duckdns.hjow.swingwebpanel.testing.TestRunner`(외부 시험 라이브러리 없음, P02-05)를 유지하고 `mvn test`/`verify` 단계에서 JDK 6의 `java`로 실행한다(예: `exec-maven-plugin`에 JDK 6 실행 파일 지정. surefire 등 다른 방식은 JDK 6 지원 여부를 확인한 뒤 결정). `--filter` 같은 실행기 인수를 Maven 속성으로 넘길 수 있게 하고, 시험 실패와 실행기 자체 점검(실패 묶음이 종료 코드 1) 실패가 빌드 실패가 되게 한다. 시험이 쓰는 시스템 속성(`swp.jar` 등)과 시험 자료 경로를 새 구조에 맞춘다.
- [x] P23-05. 기존 스크립트를 정리한다. `build.sh/bat`, `test.sh/bat`, `examples/run.sh/bat`, `scripts/jdk6.*`, `scripts/pkgtest.sh`, `scripts/snaptest.sh`를 Maven 호출 래퍼로 바꾸거나 제거한다. Maven을 실행할 수 없는(JDK 6만 있는) 환경을 위해 javac 직접 호출 경로를 남길지 결정하여 문서화한다. sh는 UTF-8, bat는 MSWIN949로 저장하고, `JAVA6_HOME` 재지정과 "JDK 6을 찾지 못하면 오류" 규칙을 Maven 경로에도 적용한다.
- [x] P23-06. 경로를 참조하는 도구와 문서를 갱신한다. `tools/sourcecheck`의 기본 검사 경로, 제품 클래스 경로를 쓰는 도구(`tools/uicheck` 등), `.gitignore`(`target/`), `docs/ARCHITECTURE.md`의 디렉터리/패키지 표와 도구 목록, `docs/FEATURE_MATRIX.md`와 `docs/progress/*`의 구현 경로, README의 빌드·사용 방법(Maven 좌표와 `mvn` 명령, Maven 없이 JAR을 쓰는 방법)을 고친다. 이 계획의 옛 경로 표기(P02-01 등)는 항목의 의미를 바꾸지 않고 새 경로를 알리는 메모를 덧붙인다.
- [x] P23-07. 전환 결과를 검증한다. JDK 6 대상 Maven 빌드로 만든 제품 JAR의 class 파일 major 버전이 50이고 JAR 항목이 전환 전 `build.sh` 결과와 같은지(리소스 포함, 시험·도구 미포함) 비교한다. 전체 회귀가 Maven 경로에서 전환 전과 같은 통과/실패/건너뜀 수로 끝나는지, 예제 smoke, `tools/sourcecheck` 0 위반, 플러그인을 받은 뒤의 오프라인(`-o`) 빌드를 확인한다. JRE 6 실행 검증은 JDK 6의 `java`로 실행한 결과만 인정한다. 결과를 PROGRESS에 기록하고 P23을 완료 처리한다.

### P24. 콘솔 출력 이벤트

선행 작업: P15, P20-02, P20-09, P21, P23. 산출물: 콘솔 이벤트/리스너 공개 API, 주 문서·프레임·Worker의 콘솔 출력 전달, 계약 문서와 시험.

- [x] P24-01. 공개 API 계약을 `docs/API_CONTRACT.md`에 정한다. 기본안은 리스너 인터페이스 `org.duckdns.hjow.swingwebpanel.ConsoleListener`(`java.util.EventListener` 상속, 메서드 `consoleMessage(ConsoleEvent e)`), 이벤트 클래스 `ConsoleEvent`(`java.util.EventObject` 상속, 원천은 `SwingWebPanel`), `SwingWebPanel`의 `addConsoleListener`/`removeConsoleListener`/`getConsoleListeners`이다. 이벤트에는 수준(적어도 `log`, `error`), 서식 문자열 처리 후의 메시지, 원래 인수들의 문자열 표현(JavaScript 객체 참조를 Java에 노출하지 않음), 발생 문서 URL, 가능한 경우 스크립트 URL과 줄 번호, 발생 위치 종류(주 문서/하위 프레임/Worker), 발생 시각을 담는다. null 리스너와 중복 등록·제거는 P01-17과 같이 Swing의 일반 등록 단위 동작을 따른다. 이름은 구현 중 조정할 수 있으나 계약 문서와 기능 표를 함께 갱신한다.
- [x] P24-02. 통지 범위를 정한다. `console.log`와 `console.error` 호출은 반드시 이벤트를 발생시킨다. 나머지 console 메서드(`info`/`warn`/`debug`/`trace`/`table`/`dir`/`group`/`count`/`time*`, 실패한 `assert` 등)의 통지 여부와 수준 값을 계약에 정한다(기본안: 출력을 만드는 모든 메서드가 Console Standard의 수준대로 통지하고 `assert`는 실패할 때만 error로 통지). 잡히지 않은 스크립트 예외와 처리되지 않은 Promise 거부를 error 수준으로 알릴지 정하고, 알린다면 `console.error` 호출과 구분할 수 있는 필드를 둔다. `alert/confirm/prompt`가 현재 콘솔에만 쓰는 동작(FEATURE_MATRIX WEB-01)과의 관계도 정한다.
- [x] P24-03. 엔진 쪽 경계를 유지하며 구현한다. 엔진의 realm별 출력 연결점 `js.ConsoleHook`을 경계로 사용하고, 브라우저 쪽 어댑터가 이를 `ConsoleEvent`로 바꾼다. P26에서 엔진만 사용할 때도 같은 훅으로 콘솔 출력을 받을 수 있도록 엔진 쪽에 Swing/브라우저 타입을 넣지 않는다. 수준·인수·위치 정보를 넘기려고 `ConsoleHook`을 확장할 때는 기존 시험의 출력 형식을 유지한다. 주 문서, iframe/frame, Worker/SharedWorker의 출력을 해당 컴포넌트의 리스너로 모은다.
- [x] P24-04. 스레드와 수명 규칙을 구현한다. 리스너는 HyperlinkListener와 같이 EDT에서 호출하는 것을 기본안으로 한다. 스크립트 실행 스레드는 EDT를 기다리지 않고 문서 큐와 EDT 사이에 순환 대기를 만들지 않으며, 한 컴포넌트 안의 발생 순서를 보존한다. 리스너가 던진 예외는 다른 리스너와 스크립트 실행에 전파하지 않는다. 반복문 안의 대량 출력이 EDT 큐와 메모리를 고갈시키지 않도록 대기 이벤트 수의 상한과 초과 시 정책(버림/합침과 버린 개수 알림)을 정하고 문서화한다. 이동 뒤 늦게 도착한 이전 페이지의 메시지가 새 페이지의 것으로 보이지 않도록 이벤트에 발생 페이지 정보를 고정한다. `setJavascriptEnabled(false)`에서는 스크립트가 실행되지 않으므로 이벤트도 없다. `clean()`은 콘솔 리스너를 유지하고(P01-15), `dispose()`는 콘솔 리스너도 제거하며(P01-16의 제거 대상 표에 추가), `removeHyperlinkEvents()`는 콘솔 리스너를 제거하지 않는다.
- [x] P24-05. 실제 `SwingWebPanel` 페이지로 검증한다. 인라인/외부 스크립트, 이벤트 핸들러, 타이머, Promise, 모듈, iframe, Worker의 `console.log`/`console.error`가 정확한 수준, 메시지(서식 문자열 `%s %d %o` 포함), 순서, 스레드(EDT)로 도착하는지 확인한다. 등록 전/제거 뒤 호출의 미통지, 리스너 예외 격리, 대량 출력 상한, 이동/clean/dispose 전후, JavaScript 비활성 사례를 JRE 6에서 검증한다. 기능 표에 행을 추가하고 README와 예제에 사용법을 적은 뒤 P24를 완료 처리한다. 완료(2026-11-22): `ConsoleListener`/`ConsoleEvent`/`ConsoleDispatcher`(EDT, 대기 1000개 상한)/`PageConsole`, 엔진 쪽 `js.ConsoleRecord`/`ConsoleRecordHook`(호출 위치 추적 포함), 처리되지 않은 promise 거부 보고, 시험 `ConsoleEventIntegrationTest` 11 · `ConsoleRecordTest` 6, 예제 `ConsoleExample`, 병합 전 전체 회귀 3771 통과/0 실패/14 건너뜀(JDK 6u45). 계약 `docs/API_CONTRACT.md` 5.20. 한계: `unhandledrejection` 이벤트 없음.

### P25. Web Audio API

선행 작업: P15, P18, P20-09, P21, P24. 산출물: `AudioContext`/`OfflineAudioContext`와 오디오 노드 그래프, 오디오 렌더링 스레드와 Java Sound 출력, 기능 표와 시험.

- [x] P25-01. 기준 판본과 대상 목록을 정한다. W3C Web Audio API 1.0 권고안을 출발 기준으로 하되 `tools/webfetch`로 현재 판본(이후 판 포함)을 조회하여 판본·URL·조회 날짜를 기능 표에 고정한다. BaseAudioContext, AudioContext, OfflineAudioContext, AudioNode/AudioParam, AudioBuffer, 각 노드, AudioWorklet, AudioListener, PeriodicWave 등 인터페이스별로 행을 나누고, Chromium의 `third_party/blink/renderer/modules/webaudio`를 참고 구현으로 기록한다. Web Audio는 HTML5 2014 REC 이후의 규격이므로 기능 표의 기존 분류(확장 목록 등)를 이 추가 목표에 맞게 갱신한다. PannerNode의 공간화는 CPU 계산이므로 3D 가속 예외에 해당하지 않는다. `MediaElementAudioSourceNode`의 `<video>` 원천은 비디오 예외에 따른 처리를 정하고, 장치 입력이 필요한 `MediaStream` 계열(`getUserMedia`, `MediaStreamAudioSourceNode`/`MediaStreamAudioDestinationNode`)의 대상 여부를 근거와 함께 결정한다. 완료(2026-11-22, A1 작업자): 기준 판본(1.0 REC 2021-06-17 이 기능 기준, 현재 TR 주소는 1.1 WD 2026-09-22), 인터페이스별 대상 표, Chromium 참고 구현(리비전 `8d101571406d3ef5bda7446b34568192ad99b3b5`), 결정: MediaStream 계열은 명시적 예외(EXC-14), `<video>` 원천 거부, `ScriptProcessorNode` 는 C 단계에서 구현, HRTF 는 MIT KEMAR 자료(저자 표기 조건), `sinkId` 는 기본 장치만·자동 재생 정책 없음. 기록 `docs/progress/webaudio.md` 1~3절, 기능 표 8.4절.
- [x] P25-02. 오디오 그래프와 렌더링 엔진을 구현한다. 128 프레임 render quantum, 모든 형태의 `connect`/`disconnect`(AudioParam 입력 연결 포함), `channelCount`/`channelCountMode`/`channelInterpretation`에 따른 업·다운 믹스, DelayNode가 포함된 순환만 허용하는 순환 규칙, 연결이 끊긴 노드의 꼬리 시간과 수명, `currentTime`/`sampleRate`/`baseLatency`/`outputLatency`를 처리한다. 렌더링은 별도 오디오 스레드에서 하고, JavaScript 스레드와는 규격의 제어 메시지 모델로 상태를 주고받으며, 렌더링 중 버퍼를 재사용하여 GC 부담을 줄인다. 완료(2026-11-22, A1/C1/C2a 작업자): 128 프레임 render quantum, `connect`/`disconnect` 전 형식, 규격 4.4/4.5절 채널 믹스, DelayNode 순환 규칙(Tarjan), 꼬리 시간과 수명, 제어 메시지 모델, 오프라인 렌더링, 렌더링 중 버퍼 재사용, **별도 오디오 렌더링 스레드**(`OutputRenderDriver`, 스레드 `swingwebpanel-audio-render-N`; 장치로 쓰고 장치 없으면 `NullOutput` 으로 벽시계 속도)와 `currentTime`/`sampleRate`/`baseLatency`/`outputLatency`. 시험 `GraphTest`, `ChannelMixingTest`, `EngineStateTest`, `RealtimeAudioContextTest`, `OutputRenderDriverTest`. 실제 소리 장치 출력과 청취는 확인하지 못했다(P22-10, P25-07). 기록 `docs/progress/webaudio.md`, `webaudio-c2a.md`.
- [x] P25-03. AudioParam 자동화를 구현한다. `value`, `setValueAtTime`, `linearRampToValueAtTime`, `exponentialRampToValueAtTime`, `setTargetAtTime`, `setValueCurveAtTime`, `cancelScheduledValues`, `cancelAndHoldAtTime`, a-rate/k-rate와 `automationRate`, 명목 범위 제한, 자동화 이벤트 겹침 등의 오류 규칙을 처리한다. 완료(2026-11-22, A1 작업자): `AudioParamTimeline`/`AudioParam` 에 모든 자동화 메서드(`cancelAndHold` 포함), a-rate/k-rate, 명목 범위, 오류 규칙, 오래된 이벤트 접기. 시험 `AudioParamTest` 22건(식으로 만든 기대값)과 `GraphTest`.
- [x] P25-04. 원천/처리 노드를 구현한다. `AudioBufferSourceNode`(loop, playbackRate, detune, start/stop 오프셋), `OscillatorNode`와 `PeriodicWave`(대역 제한 파형), `ConstantSourceNode`, `GainNode`, `DelayNode`, `BiquadFilterNode`(`getFrequencyResponse` 포함), `IIRFilterNode`, `WaveShaperNode`(oversample), `ConvolverNode`(normalize, 긴 임펄스 응답의 분할 FFT 합성곱), `DynamicsCompressorNode`, `StereoPannerNode`, `PannerNode`/`AudioListener`(equalpower와 HRTF), `ChannelSplitterNode`/`ChannelMergerNode`, `AnalyserNode`(FFT, 창 함수, 평활, 주파수/시간 영역 데이터), `MediaElementAudioSourceNode`(P18의 `<audio>` 출력을 그래프로 연결, 교차 오리진 원천은 무음)를 대상으로 한다. HRTF는 순수 Java로 쓸 수 있는 임펄스 응답 데이터의 출처와 이용 조건을 확인하고, 사용할 수 없으면 근거를 기록한다. 규격에서 폐기 예정인 `ScriptProcessorNode`의 처리도 결정한다. 완료(2026-11-22, A1/A2/C2a/C2b 작업자): 노드 전부를 엔진 수준과 JavaScript 경로에서 닫힌 식·독립 계산으로 검증했다: `AudioBufferSourceNode`, `OscillatorNode`+`PeriodicWave`, `ConstantSourceNode`, `GainNode`, `DelayNode`, `BiquadFilterNode`(`getFrequencyResponse`), `IIRFilterNode`, `WaveShaperNode`(2x/4x 과표본화), `ConvolverNode`(분할 FFT, normalize), `DynamicsCompressorNode`(Chromium 설계를 옮김, BSD 고지), `StereoPannerNode`, `PannerNode`/`AudioListener`(equalpower, MIT KEMAR 자료의 HRTF, 자료를 못 읽으면 equalpower), `ChannelSplitterNode`/`ChannelMergerNode`, `AnalyserNode`, `MediaElementAudioSourceNode`(`<audio>` 의 소리를 그래프로, 교차 오리진은 무음; 비디오는 `NotSupportedError`), `ScriptProcessorNode`(폐기 예정 API 의 호환 구현). `ScriptProcessorNode` 결정은 구현(결정 c). 한계: `crossorigin` CORS 로 가져온 교차 오리진 미디어는 아직 무음. 실제 소리 청취는 확인하지 못했다(P22-10). 기록 `docs/progress/webaudio.md`, `webaudio-c2a.md`, `webaudio-c2b.md`.
- [x] P25-05. AudioBuffer와 디코딩을 구현한다. `createBuffer`/`AudioBuffer` 생성자, `getChannelData`/`copyFromChannel`/`copyToChannel`, `decodeAudioData`(Promise와 콜백 형식, 전달된 ArrayBuffer의 분리, 실패 시 `EncodingError`)를 처리한다. 디코딩은 P18의 자체 디코더를 재사용하고 컨텍스트의 sampleRate로 리샘플링하며, 리샘플러의 방식과 품질 근거를 기록한다. 디코딩은 백그라운드에서 하고 결과는 문서 큐에서 전달한다. 완료(2026-11-22, B/B2/C1 작업자): `AudioBuffer`(규격 1.4.3 복사 규칙, `getChannelData` 의 같은 `Float32Array` 와 acquire 스냅샷, 생성자 한계), `decodeAudioData`(Promise·콜백, ArrayBuffer 분리와 `DataCloneError`, 실패 `EncodingError`, 문서 큐 전달, 취소), 기존 디코더 아홉 개를 파일의 모든 채널(최대 32, 표준 순서)·float 정밀도로 읽는 넓은 경로(`openWide`), Kaiser 창 sinc 다상 리샘플러(정지 대역 약 -100 dB), 백그라운드 디코딩 서비스. 규격과 다른 점: 첫 프레임 뒤의 잘림/손상은 디코딩한 만큼 성공으로 돌려주고 경고를 콘솔로 보낸다(규격 1.0 은 `EncodingError`; 1.1 WD 비규범 주석과 실제 브라우저에 맞춤), acquire 때 이전 배열을 분리하지 않음(Chromium 방식). 시험 `AudioBufferTest`, `ResamplerTest`, `AudioBufferDecoderTest`, `AudioDecodeServiceTest`, `TruncatedDataTest`, `media.audio.wide.*`, `js.host.webaudio.{AudioBufferScriptTest,DecodeAudioDataTest,DecodeDamagedDataTest}`. 기록 `docs/progress/webaudio-b.md`, `webaudio-c.md`.
- [x] P25-06. AudioWorklet을 구현한다. `audioWorklet.addModule`(공통 요청 계층과 모듈 로더 사용), `AudioWorkletGlobalScope`(`registerProcessor`, `currentFrame`/`currentTime`/`sampleRate`), `AudioWorkletNode`/`AudioWorkletProcessor`(`process` 호출, `parameterDescriptors`, `port` 메시지)를 DOM이 없는 별도 realm에서 실행한다(P20-09의 Worker 구조 재사용). 렌더링 스레드에서 JavaScript를 실행하므로 실행 제한/중단과 처리 지연 시 무음 처리 규칙을 정한다. 완료(2026-11-22, D1 작업자): `audioWorklet.addModule`(공통 요청 계층과 모듈 로더 사용, 같은 URL 은 한 번만 평가, 실패 의미는 HTML Standard 와 Chromium 156 에 맞춤: 구문 오류는 그 오류로 reject, 평가 중 예외는 resolve 하고 콘솔 보고, 네트워크·스킴·오리진·MIME 실패만 `AbortError`), DOM 없는 별도 realm 의 `AudioWorkletGlobalScope`(`registerProcessor`, `currentFrame`/`currentTime`/`sampleRate`), `AudioWorkletNode`(옵션 규칙, `AudioParamMap`, `port`, `onprocessorerror`)와 `AudioWorkletProcessor`(`process`, `parameterDescriptors`). realm 은 컨텍스트마다 공정 잠금으로 직렬화하고 `process()` 는 렌더링 스레드가 부른다(실시간은 `tryLock` 2 ms 로 못 쥐면 그 quantum 무음, 한 호출 200만 단계 한도). 시험 `AudioWorkletNodeTest` 와 `js.host.webaudio.worklet.*`. 한계: 인터프리터 비용 때문에 단순한 프로세서 일곱 개 안팎이 48 kHz 실시간의 한계, 실제 장치 청취는 확인하지 못했다(P22-10). 기록 `docs/progress/webaudio-d1.md`.
- [ ] P25-07. 출력과 수명을 처리한다. 실시간 `AudioContext`는 Java Sound(`SourceDataLine`)로 출력한다. 출력 장치가 없거나 열 수 없을 때의 상태와 시간 진행 규칙, `state`(`suspended`/`running`/`closed`)와 `statechange`, `resume`/`suspend`/`close`, 사용자 활성화 전 시작을 막는 자동 재생 정책 여부, `latencyHint`, 출력 장치 선택(`sinkId`)의 대상 여부를 정하여 기록한다. `OfflineAudioContext`는 실시간과 무관하게 렌더링하고 `startRendering`/`complete` 이벤트/`suspend(time)`/`resume`을 처리한다. 여러 컨텍스트·여러 컴포넌트의 동시 출력, 페이지 이동/clean/dispose/JavaScript 비활성화 시 컨텍스트 종료와 오디오 라인·렌더링 스레드 정리(P21 대상 표에 추가), 버퍼 크기와 노드 수의 상한을 처리한다.
- [x] P25-08. 검증한다. 노드별 기대 PCM(합성 신호의 해석적 기대값 또는 독립 계산과 허용 오차 근거), 자동화 곡선의 표본값, 오프라인 렌더링 결과를 자동 검증한다. 다른 브라우저로 만든 참조 출력을 비교에 쓰는 경우 `tools/` 도구와 판본·조건을 기록하고 외부 엔진을 실행 의존성에 넣지 않는다. 실시간 출력의 스레드/수명/누수와 장치 없음 경로를 JRE 6에서 검증하고, 실제 스피커 청취 확인은 자동 검증과 분리하여 기록한다(P22-10). 기능 표와 README를 갱신하고 P25를 완료 처리한다. 완료(2026-11-22, 조정자 정리): 노드별 기대 PCM(닫힌 식·독립 계산), 자동화 곡선 표본값, 오프라인 렌더링 결과를 자동 검증했고, **실제 Chromium 156 이 같은 스크립트로 렌더링한 67개 시나리오와 표본 단위로 비교**했다(`ChromeReferenceTest`, 참조 자료와 만드는 방법은 `tests/resources/webaudio/chrome/`; 약 60개가 float 정밀도로 일치하고 믹스·IIR·계단 자동화 등은 비트 단위로 같으며, 설명되는 차이 6가지(웨이브셰이퍼 과표본화 지연, 나이퀴스트 가까이 발진기, 순환 안의 지연, 소수 프레임의 offset/시작 시각, 컨볼버 정규화 상수)를 시험이 단언한다). 실시간 출력의 스레드/수명/누수와 장치 없음 경로를 JRE 6u45 에서 가짜 장치로 검증했다. **실제 스피커 청취는 확인하지 못했고 자동 검증과 분리해 기록했다**(P22-10, P25-07; `docs/progress/media-play.md` 미확인 표). 기능 표 8.4절과 README 갱신. 정리 문서 `docs/progress/webaudio-verify.md`.

### P26. JavaScript 엔진 분리와 Java Scripting API 지원

선행 작업: P23, P24, P25. 산출물: 브라우저 없이 쓸 수 있는 독립 JavaScript 엔진 모듈(JAR)과 공개 API, `javax.script`(JSR 223) 제공자, 엔진 모듈에 의존하는 브라우저 컴포넌트.

- [x] P26-01. 엔진과 브라우저의 경계를 확정한다. 현재 엔진 핵심(`js` 루트, `js.ast`, `js.parser`, `js.interp`, `js.builtins`, `js.date`, `js.num`, `js.regexp`, `js.text`)은 브라우저 패키지, `js.host`/`js.dom`/`js.worker`, AWT/Swing을 import하지 않는다(2026-11-20 확인). 이 범위를 엔진 모듈로, `js.host`/`js.dom`/`js.worker`와 나머지 패키지를 브라우저 모듈로 나누는 것을 기본안으로 한다(예: 엔진 `swingwebpanel-js`, 브라우저 `swingwebpanel`). 엔진 모듈은 AWT/Swing/네트워크/파일/DOM에 의존하지 않고 JRE 6 표준 API만 쓰며, 의존 방향(브라우저 → 엔진, 역방향 금지)을 빌드로 강제한다. 엔진 패키지 이름의 유지/변경과 그 영향, Worker의 DOM 없는 범용 부분과 모듈 로더(`ModuleLoader`)를 둘 위치를 결정한다. 완료(2026-11-22): 엔진을 `engine/`(`engine/src`, `engine/tests`, `engine/resources`)으로 분리했고 패키지 이름은 유지했다(근거: 브라우저 코드 수백 개와 시험 전체 영향). 엔진은 빈 클래스패스로 컴파일되고 브라우저는 컴파일된 엔진 클래스만 참조하며, `tools/sourcecheck` 의 `engine-boundary` 규칙, 클래스 상수 풀 시험, AWT 없는(headless) 엔진 단계 시험으로 경계를 강제한다. Worker 의 범용 부분은 DOM 의존이라 브라우저에, `ModuleLoader` 는 엔진 루트에 있다. 기록 `docs/progress/engine.md` 1절.
- [x] P26-02. 엔진 독립 공개 API를 설계·구현한다. 브라우저 없이 엔진을 생성·설정·실행하는 진입점, Script/Module 평가, 전역 변수 읽기/쓰기, JavaScript 함수 호출, Java 값과 JavaScript 값의 변환 규칙(문자열/숫자/불리언/null·undefined/배열/객체/함수, `JsValue`의 노출 범위), 예외 변환(`JsException`, SyntaxError의 위치), 실행 제한(`ExecutionLimits`)과 중단, 작업/마이크로태스크 큐(`JobQueue`) 소비 방법, 콘솔 출력 훅(P24의 `ConsoleHook`), 모듈 해석 훅, 스레드 모델(realm 하나는 한 번에 한 스레드에서 사용, 동시 호출 시 동작)을 정한다. P15-02의 원칙대로 Java 클래스/리플렉션/파일/프로세스를 자동 노출하지 않고 호스트가 명시적으로 등록한 함수와 객체만 노출한다. 완료(2026-11-22): `js.embed`(`JsEngine`, 살아 있는 핸들 `JsObjectHandle/JsArrayHandle/JsFunctionHandle`, `JsHostFunction`, 위치가 있는 `JsScriptError`)와 값 변환·스레드·제한·큐·모듈·콘솔 규칙(engine.md 2절). Java 객체는 스크립트로 새지 않고 호스트가 등록한 함수만 부를 수 있다. 시험 `JsEngineBasicsTest` 13 · `JsEngineValuesTest` 12.
- [x] P26-03. 독립 모드의 전역 객체 범위를 정한다. ECMAScript 표준 내장 객체는 모두 제공하고 `window`, `document`, DOM, `fetch`, Storage 등 브라우저 전용 객체는 제공하지 않는다. `console`은 제공하여 훅으로 보낸다. 타이머(`setTimeout` 등), `TextEncoder`, `URL`, `structuredClone` 같은 호스트 API를 독립 모드에서 선택적으로 켤 수 있게 할지, 켠다면 이벤트 루프를 누가 돌리는지 결정하여 문서화한다. 완료(2026-11-22): 독립 모드 전역은 ES2015 내장 객체 전부와 `console`/`print` 이며 window/DOM/fetch/Storage 와 타이머는 없다. 타이머 등을 켜는 선택지는 만들지 않기로 결정했다(독립 엔진에는 이벤트 루프가 없고 호스트가 자기 함수를 등록해 자기 루프에서 `call`/`runJobs` 를 부른다; engine.md 2.4, 예제 `EngineExample`).
- [x] P26-04. `javax.script` 제공자를 구현한다. `ScriptEngineFactory`와 `ScriptEngine`(`AbstractScriptEngine` 상속 가능)을 만들고 엔진 JAR의 `META-INF/services/javax.script.ScriptEngineFactory`로 등록한다. `Compilable`(`CompiledScript`), `Invocable`(`invokeFunction`/`invokeMethod`/`getInterface`; `getInterface`는 `java.lang.reflect.Proxy`로 연결하되 P26-02의 노출 원칙을 지킴), `ScriptContext`의 ENGINE/GLOBAL 범위 `Bindings`와 JavaScript 전역의 연결 규칙, `Reader` 입력, `getWriter`/`getErrorWriter`와 `console.log`/`console.error` 출력의 연결, 파일 이름·줄·열이 있는 `ScriptException`, `ScriptEngine.FILENAME` 등 표준 키, 팩토리의 `getParameter`(`THREADING` 포함)와 `getMethodCallSyntax`/`getOutputStatement`/`getProgram`을 구현한다. 완료(2026-11-22): `js.script`(`JsScriptEngineFactory`, `JsScriptEngine`(Compilable, Invocable, Proxy 기반 `getInterface`), `JsCompiledScript`, `JsGlobalBindings`, 컨텍스트 Writer 연결 콘솔)와 엔진 JAR 의 서비스 파일 등록. ENGINE/GLOBAL 범위 규칙과 위치가 있는 `ScriptException`(engine.md 3절). 시험 `js.script` 20건.
- [x] P26-05. 엔진 이름 충돌과 기존 금지 규칙을 정리한다. JRE 6~7의 내장 Rhino와 JRE 8~14의 Nashorn이 `js`/`javascript`/`JavaScript`/`ECMAScript` 등의 이름과 MIME으로 등록되어 있어 `getEngineByName("javascript")`가 어느 엔진을 돌려줄지 보장되지 않는다. 이 엔진의 고유 이름(예: `swingwebpanel-js`)을 반드시 등록하고, 일반 이름/MIME/확장자의 등록 여부와 그 한계를 문서화한다. 시험은 반환된 엔진 클래스가 이 프로젝트의 것임을 확인하며 Rhino/Nashorn으로 평가된 결과를 합격으로 인정하지 않는다. P00-09와 `docs/ARCHITECTURE.md` 1.1절의 금지(다른 엔진과 `ScriptEngineManager`를 통한 내장 Rhino 우회 사용 금지)는 유지하되, 자체 엔진을 `javax.script` 제공자로 공개하는 것은 허용됨을 명시한다. `tools/sourcecheck`의 `javax.script` 금지 규칙은 제공자 구현 패키지와 해당 시험에만 예외를 두도록 고치고, 제품 코드의 `ScriptEngineManager` 사용 금지는 유지한다. 완료(2026-11-22): 팩터리는 고유 이름 `swingwebpanel-js`(와 `swingwebpanel-javascript`, 확장자 `swjs`, MIME `application/x-swingwebpanel-javascript`)만 등록하고 일반 이름은 JVM 옵션 `-Dswingwebpanel.js.genericNames=true` 로만 켠다(Rhino/Nashorn 사용자가 조용히 이 엔진을 받지 않게). `tools/sourcecheck` 는 `javax.script` 를 제공자 패키지와 `examples/**/script` 에서만 허용한다. 자체 엔진을 `javax.script` 제공자로 공개하는 것은 허용되며 다른 엔진 호출 금지는 유지한다(ARCHITECTURE 1.1). `ScriptEngineDiscoveryTest` 가 JRE 6 과 JRE 8(Nashorn)에서 일반 이름이 이 엔진이 아님을 확인한다.
- [x] P26-06. 브라우저 컴포넌트가 엔진 모듈의 공개 API만 사용하도록 정리한다. `SwingWebPanel` 사용자는 지금처럼 브라우저 모듈 하나에 의존하면 엔진이 함께 동작해야 하며, JavaScript 동작·성능·공개 API가 바뀌지 않아야 한다. Maven을 쓰지 않는 사용자를 위한 JAR 배포 형태(엔진 JAR과 브라우저 JAR, 둘을 합친 단일 JAR 제공 여부)를 정하고, 제품 JAR 이름을 검사하는 시험과 문서(P22-13)를 갱신한다. 완료(2026-11-22): 브라우저는 컴파일된 엔진 클래스의 공개 API 만 사용한다(컴파일러가 강제). JAR 세 개: `swingwebpanel.jar`(합친 단일 JAR, 기존 사용 방식 유지), `swingwebpanel-js.jar`(엔진만, 서비스 파일 포함), `swingwebpanel-browser.jar`(브라우저만, 엔진 JAR 필요). 시험 `EngineJarTest`, `BrowserJarsTest`, `JarContentTest`. 합친 JAR 이름은 P23 에서 다시 정한다.
- [x] P26-07. 검증한다. (1) 엔진 모듈만 클래스패스에 두고(AWT/Swing 미사용, `java.awt.headless=true`) ES6 회귀 시험 전체와 독립 API 시험을 JRE 6에서 통과한다. (2) `ScriptEngineManager`로 고유 이름을 조회하여 `eval`, `Bindings`, `Compilable`, `Invocable`, `getInterface`, `ScriptException` 위치, Writer 연결, 엔진 인스턴스 간 분리, 스레드 정책을 JRE 6과 Nashorn이 있는 JRE 8에서 확인하고 다른 엔진이 선택되지 않음을 검증한다. (3) 브라우저 모듈의 전체 회귀가 분리 전과 같은 통과/실패/건너뜀 수로 끝난다. (4) 엔진 모듈이 브라우저/AWT/Swing을 참조하지 않는지 도구나 빌드 단계로 자동 검사한다. README에 독립 엔진 사용법과 `javax.script` 예제를 추가하고 예제 코드를 작성한 뒤 P26을 완료 처리한다. 완료(2026-11-22): 엔진 단계(엔진 클래스만, headless) 648 통과, 엔진 JAR 하나만으로 645 통과/3 건너뜀(상수 풀 검사), 전체 회귀 3823 통과/0 실패/14 건너뜀(JDK 6u45, 조정자가 직접 재실행), JRE 8(Nashorn)에서 `js.script` 20건과 `javax.script` 예제 통과, 경계 자동 검사 3종(sourcecheck, 상수 풀 시험, 빈 클래스패스 컴파일), README 와 예제 `EngineExample`/`script.ScriptEngineExample`. 한계: JRE 8 에서 `DateTest.testRoundTrips`(1900 이전 시간대 데이터) 1건 실패, 엔진 단독을 JRE 7 이상에서 전체 실행하지 않음, 독립 모드에는 타이머 없음(engine.md 7절).

### P27. Java 호스트와 페이지 스크립트 간 메시지 통신(postMessage)

선행 작업: P15, P20-02, P20-09, P21, P23, P24, P26, P28-04, P29(진행 순서상 P29와 P28 잔여 항목 완료 뒤 수행: 위 공통 사항 참조). 산출물: Java → 페이지 `message` 이벤트 전달 API, 페이지 → Java 메시지 리스너 API, 페이지 안의 호스트 창 객체, 양방향 값 변환 규칙, 계약 문서·기능 표·예제와 시험.

2026-11-22 사용자 요청으로 추가했으며, 2026-11-25 목표 상향에 따른 순서 재조정으로 ES2026 값 모델과 구조화 복제 규칙이 확정되는 P29 및 P28 잔여 작업 다음에 진행한다. `window.postMessage`(참고: MDN 한국어 문서 https://developer.mozilla.org/ko/docs/Web/API/Window/postMessage, 2026-04-15 수정본, 2026-11-22 조회. 규격은 HTML Standard "Web messaging" 절과 W3C Web Messaging)는 본래 팝업 창과 프레임 같은 창 사이의 통신을 위한 것이지만, 이 단계에서는 이 컴포넌트를 사용하는 Java 코드(호스트)를 페이지를 연 다른 창처럼 다루어 Java 코드와 페이지 스크립트가 같은 `message` 이벤트 관용구로 통신하게 한다. 페이지 안에 이미 있는 `postMessage`/`MessageChannel`/구조화 복제(P20-09, FEATURE_MATRIX WEB-08)를 재사용하고, 페이지·프레임·Worker 사이의 기존 동작은 바꾸지 않는다.

- [ ] P27-01. 공개 API 계약을 `docs/API_CONTRACT.md` 5.22절에 확정한다. 기본안은 다음과 같다. (1) Java → 페이지: `SwingWebPanel.postMessage(Object message, String targetOrigin)`과 `postMessage(Object message)`(대상 오리진 `"*"`와 같음). (2) 페이지 → Java: 리스너 인터페이스 `org.duckdns.hjow.swingwebpanel.WebMessageListener`(`java.util.EventListener` 상속, 메서드 `messageReceived(WebMessageEvent e)`), 이벤트 클래스 `WebMessageEvent`(`java.util.EventObject` 상속, 원천은 `SwingWebPanel`), `SwingWebPanel`의 `addWebMessageListener`/`removeWebMessageListener`/`getWebMessageListeners`. (3) 페이지 안에서는 Java 호스트를 "호스트 창" 객체 하나로 나타낸다. 최상위 문서의 `window.opener`와 호스트가 보낸 `message` 이벤트의 `event.source`가 이 객체이므로(같은 페이지 안에서 `window.opener === event.source`), 팝업이 자신을 연 창과 통신하는 관용구(`window.opener.postMessage(data, targetOrigin)`, 받은 메시지에 `event.source.postMessage(reply, event.origin)`으로 답하기)를 그대로 쓸 수 있다. 호스트 창의 오리진은 상수 `SwingWebPanel.HOST_ORIGIN`(기본안 값 `"swingwebpanel://host"`, 웹 오리진과 겹치지 않는 값)으로 고정한다. 이름과 상수 값은 구현 중 조정할 수 있으나 계약 문서와 기능 표를 함께 갱신한다. 검토했으나 기본안에서 제외한 방식과 그 이유도 계약에 적는다: 페이지가 자기 `window`로 보낸 메시지를 Java가 엿듣는 방식(페이지 자신의 `message` 리스너에도 전달되고, 라이브러리가 작업 예약용으로 자신에게 보내는 메시지가 섞인다), `window.swingwebpanel` 같은 비표준 전역 객체(표준 관용구를 쓸 수 없고 페이지에 새 전역 이름이 생긴다). null 리스너와 중복 등록·제거는 P01-17과 같이 Swing의 일반 등록 단위 동작을 따른다. `postMessage`의 null 대상 오리진과 `"*"`·절대 URL 이외의 값(`"/"` 포함; 호스트에는 같은 오리진의 문서가 없다)은 `IllegalArgumentException`, 폐기 뒤 호출은 `IllegalStateException`으로 한다. 참조 규격의 판본(HTML Standard "Web messaging" 절)을 `tools/webfetch`로 다시 조회하여 기능 표에 고정한다.
- [ ] P27-02. 양방향 값 변환 규칙을 정한다. Java → 페이지는 호출한 스레드에서 즉시 깊은 복사하여(호출 뒤 Java 객체를 바꿔도 전달할 값은 바뀌지 않는다) 구조화 복제 결과와 같은 중간 형태로 만들고, 받는 문서의 realm에서 새 값으로 만든다. 기본안: `null`→null, `Boolean`, `java.math.BigInteger`→BigInt(P28-04), 그 밖의 `Number`→number(double; `long`이 2^53을 넘으면 정밀도 손실), `String`/`CharSequence`/`Character`→string, 문자열 키의 `Map`→일반 객체(삽입 순서), `List`/`Collection`/`Object[]`와 `byte[]`를 뺀 기본형 배열→배열, `byte[]`→`ArrayBuffer`, `java.util.Date`→`Date`. 그 밖의 Java 객체, 문자열이 아닌 키, P26의 살아 있는 핸들과 호스트 함수(`JsHandle`, `JsHostFunction`, `JsValue`)는 `IllegalArgumentException`으로 거부하여 P15-02 원칙대로 Java 객체를 스크립트에 노출하지 않는다. 페이지 → Java는 보내는 쪽 realm에서 구조화 복제 알고리즘(P20-09, P28-04, P29)을 먼저 적용하고(함수, DOM 노드, Symbol 등은 보내는 스크립트에 `DataCloneError`) 문서 스레드에서 스크립트 객체 참조가 없는 Java 복사본으로 바꾼다. 기본안: undefined→P26의 `JsEngine.UNDEFINED`, null→`null`, boolean→`Boolean`, number→항상 `Double`(NaN/무한대/-0 유지), bigint→`java.math.BigInteger`, string→`String`, 배열→수정할 수 없는 `List<Object>`(빈 슬롯은 undefined), 일반 객체→삽입 순서를 지키는 수정할 수 없는 `Map<String,Object>`, `Date`→`java.util.Date`, `ArrayBuffer`(가변 길이 포함 현재 유효 바이트)와 모든 `TypedArray`(`BigInt64Array`/`BigUint64Array`/`Float16Array` 포함)/`DataView`→그 바이트의 `byte[]` 복사본(`SharedArrayBuffer`의 호스트 경계 허용/거부 정책은 P29-05와 일치시킴), `Map`/`Set`→순서를 지키는 수정할 수 없는 Java `Map`/`Set`, 원시값 래퍼 객체→그 원시값의 Java 값. 같은 객체를 여러 번 가리키면 같은 Java 객체로 유지한다. 순환 참조는 Java 컬렉션의 `equals`/`hashCode`/`toString`이 끝나지 않으므로 기본안에서 거부한다(Java → 페이지는 `IllegalArgumentException`, 페이지 → Java는 `DataCloneError`). 메시지 하나의 크기 상한(문자 수와 원소 수의 합)을 이름 있는 상수로 두고 초과 시 같은 오류로 거부한다. P26 `js.embed`의 변환 규칙(`docs/progress/engine.md` 2.2)과 같은 부분은 같게 하고, 다른 부분(살아 있는 핸들 대신 복사본, `byte[]`/`ArrayBuffer`, `Date`)은 계약에 표로 적는다.
- [ ] P27-03. Java → 페이지 전달을 구현한다. 메시지는 호출 시점에 스크립트를 실행하는 최상위 문서(새 이동의 실행 환경이 설치되었으면 확정 전이라도 그 문서, 아니면 확정된 문서)에 묶는다. `targetOrigin`이 `"*"`가 아니면 그 문서 오리진의 스킴/호스트/포트가 모두 같을 때만 전달하고 다르면 조용히 버린다(페이지가 이동한 뒤 데이터가 다른 사이트로 새는 것을 막는 웹 규칙과 같다). 전달은 그 문서의 작업 큐에 넣는 작업이며 호출 중에 동기 실행하지 않는다. 확정 전 문서의 작업은 P15-08-a의 보류 규칙을 따르고, Java가 보낸 순서를 지킨다. 이벤트는 `window`에서 발생하는 `MessageEvent`로 `data`(P27-02 변환 결과), `origin` = `HOST_ORIGIN`, `source` = 호스트 창, `ports` = 빈 배열, `lastEventId` = 빈 문자열, `isTrusted` = true이고 버블링·취소되지 않으며, `addEventListener('message', ...)`와 `onmessage` 모두로 받는다. 묶인 문서가 전달 전에 교체/폐기되면 메시지를 버리고 다음 페이지로 넘기지 않는다. 스크립트를 실행하지 않는 문서(JavaScript 비활성, `text/plain` 등)로 가는 메시지도 버린다. 하위 프레임과 Worker에는 직접 보내지 않는다(페이지가 중계하거나 P27-04의 `reply`를 쓴다). 메서드는 `void`이며 버린 메시지를 알리지 않으므로, 페이지가 준비되면 먼저 호스트에 알리는 관용구(예: `window.opener.postMessage({type: 'ready'}, '*')`를 받은 뒤 Java가 보냄)를 README와 Javadoc에 안내한다.
- [ ] P27-04. 페이지 → Java 전달을 구현한다. 호스트 창 객체는 다른 오리진 창(P20-02의 `CrossOriginWindow`)과 같은 제한된 면만 가진다: `postMessage(message, targetOrigin [, transfer])`와 옵션 객체 형식 `postMessage(message, {targetOrigin, transfer})`, `closed`(페이지를 떠났거나 컴포넌트가 폐기되면 true), 아무 동작도 하지 않는 `close()`/`focus()`/`blur()`이며 그 밖의 속성 접근은 `SecurityError`이다. 호스트 창은 페이지마다 하나이며, 페이지가 `window.opener = null`로 끊어도 이후 호스트 메시지의 `event.source`로는 계속 받는다. 하위 프레임의 `opener`는 지금처럼 null이고, 프레임은 최상위 창을 거쳐(`top.opener.postMessage(...)`; `opener`는 다른 오리진에서도 읽을 수 있는 창 속성이다) 보낼 수 있으며 이벤트에는 보낸 프레임의 오리진과 출처 종류가 담긴다. Worker 전역에는 호스트 창이 없다. 대상 오리진이 `"*"` 또는 `HOST_ORIGIN`이면 전달하고, `"/"`(보낸 쪽과 같은 오리진이라는 뜻이므로 호스트와 맞지 않는다)와 그 밖의 유효한 URL은 조용히 버리며, URL로 해석할 수 없으면 `SyntaxError`(DOMException)이다. 전달 목록의 `ArrayBuffer`는 보내는 쪽에서 분리되고 `byte[]`로 전달되며, `MessagePort` 전달은 기본안에서 `DataCloneError`로 거부한다. `WebMessageEvent`에는 변환한 `getData()`, 보낸 문서의 직렬화된 오리진 `getOrigin()`(불투명이면 `"null"`), 보낸 시점에 고정한 `getDocumentUrl()`, 출처 종류 `getSourceKind()`(`DOCUMENT`/`FRAME`), 일련번호 `getSequence()`, 시각 `getTimestamp()`, 보낸 창에 답하는 `reply(Object message)`(대상 오리진은 이벤트의 오리진이며, 보낸 문서가 이미 떠났으면 버린다)를 둔다. 리스너가 없으면 메시지를 버리고 나중에 등록한 리스너를 위해 쌓아 두지 않는다.
- [ ] P27-05. 스레드·수명·보안 규칙을 구현한다. `postMessage`와 `reply`는 어느 스레드에서나 호출할 수 있고 검증·변환은 호출 스레드에서 하며, EDT나 문서 스레드를 기다리지 않으므로 `WebMessageListener` 안(EDT)에서 다시 보내도 교착하지 않는다. 리스너는 P24 콘솔 리스너와 같이 EDT에서 보낸 순서대로 호출하고, 리스너가 던진 예외는 다른 리스너와 스크립트 실행에 전파하지 않는다. 반복문 안의 대량 전송이 EDT 큐와 메모리를 고갈시키지 않도록 대기 이벤트 수의 상한(기본안: 콘솔과 같은 1000개)과 초과 시 정책(버리고 그 개수를 다음 이벤트나 콘솔 `HOST_ERROR`로 알림)을 정하며, 리스너가 느려도 스크립트 스레드는 막히지 않는다. 이동하면 이전 페이지의 호스트 창은 `closed`가 되고, 늦게 도착한 이전 페이지의 메시지와 이전 문서로 보낸 `reply`는 새 페이지로 가지 않는다. `setJavascriptEnabled(false)`이면 양방향 모두 메시지가 없다. `clean()`은 메시지 리스너를 유지하고(P01-15) 대기 중인 메시지는 버린다. `dispose()`는 메시지 리스너와 대기 메시지를 제거하며(P01-16의 제거 대상 표에 추가) 이후 `postMessage`와 리스너 등록은 `IllegalStateException`이다. `removeHyperlinkEvents()`는 메시지 리스너에 영향이 없다. `WebMessageEvent`와 호스트 창은 페이지와 컴포넌트를 약한 참조나 수명 토큰으로만 가리켜 GC를 막지 않는다(P21). 보안: 네트워크에서 불러온 어떤 페이지(와 그 안의 다른 오리진 프레임)도 호스트에 메시지를 보낼 수 있으므로 Java 쪽은 먼저 `getOrigin()`을 검사하고 받은 데이터의 형식을 검증해야 하며, Java가 보낼 때는 `"*"` 대신 정확한 대상 오리진을 쓰도록 README와 Javadoc에 적는다(MDN의 보안 권고와 같다).
- [ ] P27-06. 실제 `SwingWebPanel` 페이지로 검증하고 문서화한다. 서로 다른 오리진의 로컬 HTTP 시험 서버와 classpath 페이지로 다음을 JRE 6에서 검증한다: 왕복(Java → 페이지 → `event.source.postMessage(reply, event.origin)` → Java), 페이지가 먼저 보내는 준비 알림과 파싱 중 전송, `window.opener === event.source`, 양방향의 대상 오리진(`"*"`, 일치, 불일치, `"/"`, 잘못된 값), 값 변환 표 전체와 거부 사례(함수, DOM 노드, 순환, 크기 초과, 지원하지 않는 Java 형식), `ArrayBuffer` 전달과 분리, 클릭 처리기 안에서 보낸 메시지가 그 처리기와 마이크로태스크 뒤에 도착하는 순서, 하위 프레임(같은/다른 오리진)의 전송과 `reply`, Worker에 호스트 창이 없음, 이동 경쟁(이전 페이지의 늦은 메시지와 `reply`를 버림, `closed`), JavaScript 비활성, clean/dispose 전후, 리스너가 EDT에서 호출됨, 리스너 예외 격리, 대량 전송 상한, 폐기 뒤 수거(누수 시험). 기존 페이지 사이 메시징 시험(`MessagingApiTest`, `js.worker.*`, 프레임 시험)이 그대로 통과하는지도 확인한다. `docs/API_CONTRACT.md`(2절 표, 5.13/5.14 표, 5.22절, 10절 사례), `docs/FEATURE_MATRIX.md`(API-30 행, WEB-08/EVT-06 제약), `docs/ARCHITECTURE.md`, README, 예제(`MessageExample`)를 갱신한 뒤 P27을 완료 처리한다.

### P28. ES2016~ES2022 언어 확장과 게임 페이지 구동

선행 작업: P13, P14, P15, P20-08, P23, P24, P26(또한 `P28-06~09`는 `P29` 완료 뒤 진행: 위 공통 사항 참조). 산출물: ES2016~ES2022 언어 문법과 실행 의미, BigInt, 추가 내장 객체, 게임이 쓰는 부족한 웹 API, 큰 스크립트의 실행 성능 측정과 개선, NetstormJava 게임 페이지 구동 기록.

2026-11-24 사용자 요청으로 추가했다. 사용자가 이 프로젝트로 웹 게임(NetStorm 웹 클론의 webpack 번들)을 띄우려고 만든 별도 프로젝트 `H:\Workspace\eclipse photon\NetstormJava`(Java 7 대상, `setPage("classpath://www/index.html")`)에서 번들 첫 부분의 스크립트 오류 `classpath://www/dist/netstorm.bundle.js:1:782 Uncaught Invalid or unexpected token`이 났다. 원인은 번들이 ES2022 문법(클래스 비공개 메서드 `#e`)을 변환 없이 그대로 쓰는데 당시 엔진이 ES2015(와 일부 확장)만 읽었기 때문이다. 번들은 이 밖에도 `?.`, `??`, 클래스 필드, `async`/`await`, BigInt 등을 많이 쓴다. 점검 내용과 판정 근거는 `docs/progress/netstorm.md`에 있다. (2026-11-25 목표 상향 갱신) 상단 "목표 - 지원 기능"의 JavaScript 목표가 ES6에서 **ES2026(및 이전 버전 ES 호환)**으로 상향됨에 따라, `P28-01~05`에서 구현한 ES2016~ES2022 기능은 더 이상 부가 확장이 아니라 프로젝트 필수 규격 범위의 2차 달성 단계가 되었다. ES2016~ES2022의 미완 잔여 항목과 ES2023~ES2026 규격 및 이전 ES 하위 호환은 아래 **`P29`에서 먼저 완성**하며, 그 뒤 `P28-06`(부족한 웹 API) → `P28-07`(완성된 ES2026 실행기 기준 성능 측정·최적화) → `P28-08`(게임 구동 검증) → `P28-09`(문서 정리) 순서로 마무리한다. 게임 자산(원작 자료, 글꼴, 음악)은 이 저장소의 시험 자료로 복사하지 않는다. 게임 구동 확인은 외부 프로젝트 경로를 지정하여 실행한다. 제품은 계속 JRE 6에서 동작해야 하므로 JDK 6u45와 샘플의 실행 조건인 JDK 7u80(`C:\Program Files\Java\jdk1.7.0_80`) 모두에서 검증한다.

- [x] P28-01. 기준 판본과 대상 목록을 확정한다. ECMA-262 제13판(ECMAScript 2022)을 이 단계의 기준 판본으로 하고, `tools/webfetch`로 URL·판본·조회 날짜를 기능 표에 고정한다. ES2016~ES2022의 문법·실행 의미·내장 객체를 기능 표 7.5절에 `ESX-*` 행으로 나누어 적고, 각 행에 NetstormJava 번들의 사용 여부를 표시한다. ES2023 이후 기능은 대상 밖 확장 목록에 기록한다. 2026-11-24 점검에서 실제와 다르게 나온 두 곳을 바로잡는다. 기능 표 ES-EXPR 행의 `**`는 엔진에서 구문 오류이다(7.5절로 옮김). 정규식 이름 있는 그룹은 파서만 받고 `exec()` 결과에 `groups`가 없으며 `$<name>` 치환도 없다. 완료(2026-11-25): 저장소의 `docs/es2022` 사본과 TC39 보조 스냅샷을 대조하여 `docs/FEATURE_MATRIX.md` 0절과 7.5절, `docs/progress/netstorm.md` 8절에 공식 판본 URL(접근 제한 포함), 판본별 대상 행, 게임 사용 횟수, ES2023 이후 제외 목록을 기록했다.
- [x] P28-02. 렉서와 파서에 ES2016~ES2022 문법을 추가한다. 대상은 `**`/`**=`, async 함수·화살표·메서드·제너레이터와 `await`(문맥 예약어, 모듈 최상위 `await` 포함), `for await`, 객체 spread/rest, optional catch binding, BigInt 리터럴(모든 진법), 숫자 구분자 `_`이다. `?.`(프로퍼티·계산된 키·호출과 단락 범위), `??`(괄호 없이 `||`/`&&`와 섞으면 SyntaxError), `||=`/`&&=`/`??=`도 포함한다. 클래스 public/private/static 필드, private 메서드·접근자, static 블록, `#x in obj`, `import.meta`, 동적 `import()`도 추가한다. 중복되거나 선언되지 않은 private 이름, `delete this.#x`, 필드 초기화 식 안의 `arguments`, async 함수 안의 `await` 식별자 같은 조기 오류도 처리한다. 구현: `js.parser.*`, `js.ast.*`. 완료 조건: 문법별 성공 사례와 조기 오류 사례의 파서 시험이 통과한다. NetstormJava의 번들(약 327만 자)과 `debug.html`이 불러오는 모듈 소스도 구문 오류 없이 파싱되어야 한다. 완료(2026-11-25): 하위 a~f 모두 구현했다. JRE 6 parser 40 passed/0 failed이며 `tools/netstormparse`가 현재 번들 3,283,377 UTF-16 문자, inline module 1개, 원본 모듈 1,059개, static import 2,486건을 구문 오류 0·missing 0으로 확인했다. 실행 의미는 P28-03~05의 후속 대상이다.
  - [x] P28-02-a. 토큰·숫자·연산자 문법을 추가한다. 입력: `2 ** 3 ** 2`, `2 ** -2`, `-2 ** 2`(오류), `**=`, `1_000`, `0xCA_FE`, `123n`/`0xffn`; 기대: 오른쪽 결합·단항 피연산자 제한, 숫자 구분자의 조기 오류, 모든 BigInt 진법 토큰; 구현: `parser/Lexer.java`, `parser/Token.java`, `ast/Op.java`; 완료 조건: Lexer/표현식 파서 시험 JRE 6 통과. 완료(2026-11-25): BigInt는 실행기의 `JsValue` 변환 전인 파서 AST에서 `BigInteger`로 보존하고, JRE 6 focused parser 39 passed/0 failed.
  - [x] P28-02-b. 논리·선택 문법을 추가한다. 입력: `a ?? b`, `a?.b.c?.[x]?.()`, `??=`/`&&=`/`||=`, `a ?? b || c`(오류), `a?.3:0`; 기대: optional chain 범위 보존, nullish와 `&&`/`||` 혼합 오류, 숫자 프로퍼티 오인식 방지; 구현: `ExpressionParser.java`, `ast/Node.java`, `ast/NodeType.java`; 완료 조건: 성공 AST와 실패 위치 시험 통과. 완료(2026-11-25): `OPTIONAL_CHAIN` 범위와 선택적 호출/접근 AST, nullish/논리 대입, 혼합 조기 오류를 구현했고 parser 39 passed/0 failed.
  - [x] P28-02-c. async 문법을 추가한다. 입력: async 함수·화살표·메서드·제너레이터, `await`, 모듈 최상위 `await`, `for await`, 줄바꿈 제한과 async 매개변수의 `await`; 기대: 문맥별 await, 줄바꿈 뒤 일반 식별자, 조기 오류; 구현: `FunctionInfo.java`, `Parser.java`, `ExpressionParser.java`; 완료 조건: Script/Module/early-error 시험 통과. 완료(2026-11-25): `FunctionInfo.async`, `AWAIT`, `for-await`, async 함수/메서드/화살표/제너레이터와 module top-level await를 파서 AST에 보존하고 parser 39 passed/0 failed.
  - [x] P28-02-d. 객체·catch·import 문법을 추가한다. 입력: `{...x}`, `({...x} = y)`, `const {...rest} = x`, `catch {}`, `import.meta`, `import(x, y)`(오류); 기대: object rest가 마지막, duplicate proto와 별개, 빈 catch, 동적 import 인수 검증; 구현: `ExpressionParser.java`, `Parser.java`, `ast/NodeType.java`; 완료 조건: 관련 파서·모듈 시험 통과. 완료(2026-11-25): object spread/rest, binding 없는 catch, `IMPORT_CALL`/`IMPORT_META`, `export * as ns`와 정적/동적 import 라우팅을 구현했고 parser 39 passed/0 failed.
  - [x] P28-02-e. 클래스 요소 문법을 추가한다. 입력: public/private/static field, computed field, private method/get/set, static block, `#x in obj`, 초기화 식의 `arguments` 오류; 기대: 중복/미선언 private name과 `delete this.#x` 조기 오류, static block AST; 구현: `ExpressionParser.java`, `ast/NodeType.java`, `ast/Op.java`; 완료 조건: 클래스·조기 오류 시험 통과. 완료(2026-11-25): private 토큰/환경, public-private-static-computed field, private 메서드·getter/setter, static block, `#x in`, forward/nested 참조, 중복·미선언·delete·arguments 조기 오류를 구현했고 parser 40 passed/0 failed.
  - [x] P28-02-f. Netstorm 파싱과 문서화를 검증한다. 입력: `H:\Workspace\git\NetstormReborn\webpj\www\dist\netstorm.bundle.js` 와 `debug.html`이 불러오는 `webpj\www` 모듈 전체; 기대: 구문 오류 0(실행 의미 실패는 P28-03 이후 대상); 구현: 파서 시험/도구 경로, `docs/progress/netstorm.md`; 완료 조건: JRE 6 파서 시험과 외부 모듈 1,057개 파싱 기록. 이 하위 항목과 a~e가 모두 끝나면 P28-02 상위 항목을 체크한다. 완료(2026-11-25): `tools/netstormparse`를 JDK 6u45로 실행했다. 현재 외부 트리는 2026-11-24 계획 당시보다 두 파일이 늘어 1,059개 모듈이며 번들 3,283,377 문자, inline module 1, static import 2,486건이다. syntax-errors 0, missing-modules 0. P28-02 실행 중 발견한 단항 위치의 await와 async function IIFE, `/d` 플래그 어휘 검사를 고쳤다.
- [x] P28-03. 실행 의미를 구현한다. async 함수와 `await`는 P13-09의 제너레이터처럼 Java 스레드 없이 중단 지점을 보관하는 방식으로 구현하고, 재개는 Promise 작업 큐로 한다. `await`의 마이크로태스크 순서는 기준 판본(ES2022의 await 최적화 포함)을 따른다. 다음 의미도 구현한다.
  - async 제너레이터와 요청 큐, `for await`와 AsyncFromSyncIterator.
  - 객체 spread/rest(CopyDataProperties).
  - optional chaining, nullish, 논리 대입의 단락 평가. 논리 대입은 단락되면 대입하지 않는다.
  - 클래스 필드의 정의 순서. 기반 클래스는 생성 직후, 파생 클래스는 `super()` 반환 직후에 정의한다.
  - PrivateEnvironment와 brand 검사(위반하면 TypeError), static 블록.
  - 동적 `import()`를 브라우저 모듈 로더(P15-04-c)에 연결한다.

  페이지 이동·폐기·JavaScript 비활성화 뒤에는 대기 중인 async 재개가 실행되지 않게 한다(P15-07, P21). 구현: `js.interp.*`, `js.host.*`. 완료 조건: 평가 순서, 예외, 마이크로태스크 순서를 확인하는 실행 시험이 통과한다. 완료(2026-11-25): a~f 구현·집중 시험이 통과했고 전체 엔진 675/0, 브라우저 3,688/0(선택 12), sourcecheck 0 위반이다. 실제 Netstorm 게임 실행과 BigInt 값 의미는 P28-04/08에 남는다.
  - [x] P28-03-a. P28-02의 일반 실행 의미를 연결한다. 입력: 우결합 `**`/`**=`, 배열·호출 spread, 객체 spread/rest(CopyDataProperties), optional member/call chain, `??`, `&&=`/`||=`/`??=`; 기대: 평가 횟수·getter·단락·짧은 멈춤, 문자열/symbol 키 열거와 setter 순서, 대입 대상은 한 번만 평가; 구현: `interp/Expressions.java`, `interp/Patterns.java`, `interp/Interpreter.java`; 완료 조건: 값·부작용·오류 사례의 JRE 6 시험 통과. 완료(2026-11-25): optional chain 내부 신호, Number 지수, CopyDataProperties/rest의 symbol·getter 순서, `??`, 논리 대입의 단락/단일 참조를 구현했고 `ModernExpressionTest` 6 passed.
  - [x] P28-03-b. 클래스 필드와 private 의미를 연결한다. 입력: instance/static/public/private/computed field, private method/get/set, getter/setter pair, `#x in obj`, `delete this.#x`, class static block, base/derived 초기화 순서; 기대: field 정의 순서와 `super()` 뒤 초기화, private brand 검사, static block return/await 제한, 객체별 격리; 구현: `interp/Classes.java`, `interp/ScriptFunction.java`, `Interpreter.java`; 완료 조건: 클래스 값·초기화·brand 오류의 JRE 6 시험 통과. 완료(2026-11-25): WeakHashMap private 슬롯과 class brand, field 초기화, static block, private method/accessor/reference/in 연산을 구현했고 `ModernClassTest` 4 passed.
  - [x] P28-03-c. async 함수와 `await` 실행을 구현한다. 입력: async 함수/화살표/메서드, fulfilled/rejected promise, thenable, throw/return, 중첩 await; 기대: Promise 반환, ES2022 PromiseResolve 순서, 동기 실행 후 첫 작업 큐 재개, Java 스레드 추가 없음. 모듈 최상위 await는 e에서 연결한다; 구현: `interp/AsyncFunction.java`, `Expressions.java`, `ScriptFunction.java`, `builtins/meta/Promise*.java`; 완료 조건: microtask 순서·resolve/reject·스택·정지 시험 통과. 완료(2026-11-25): GenState 재진입 기반 AsyncFunction, AwaitSignal, PromiseInterop, rejection/thenable/constructor 제한을 구현했고 ModernAsyncTest 5 passed.
  - [x] P28-03-d. async generator와 비동기 반복을 구현한다. 입력: `async function*`, `yield`/`yield*`/`await`, `next`/`return`/`throw` 직렬 요청, sync/async iterable의 `for await`, 조기 break의 iterator close; 기대: 요청 큐 순서, AsyncFromSyncIterator, rejection 전파와 close; 구현: `interp/AsyncGeneratorObject.java`, `Statements.java`, `AsyncIteratorHelper.java`; 완료 조건: 비동기 반복의 값·오류·정리 시험 통과. 완료(2026-11-25): AsyncGenerator request queue, Promise yield/await 재개, async generator prototype, Symbol.asyncIterator, AsyncFromSyncIterator, async/sync yield*와 throw/close 전달, for-await 단계/close를 구현했고 ModernAsyncIteratorTest 6 passed.
  - [x] P28-03-e. 동적 import와 비동기 모듈 평가를 연결한다. 입력: `import(specifier)`의 Promise, string coercion과 SyntaxError, 순환/중복 모듈, 성공/실패, module-only `import.meta`, 모듈 최상위 `await`; 기대: Script/Module 모두에서 Promise 로 반환하고 기존 `ModuleRecord` 그래프를 공유, 늦은 완료와 수명 토큰을 지킴; 구현: `interp/ModuleRecord.java`, `Expressions.java`, browser `js.host.PageScripts` 연결; 완료 조건: engine 독립 모듈과 실제 browser 요청/취소 시험 통과. 완료(2026-11-25): ModuleAsyncEvaluation/ModuleGraph.evaluateAsync, Promise dynamic import, import.meta.url, TLA 의존성 순서·중복·cycle을 구현했고 engine 4, browser ModuleScript 6 passed.
  - [x] P28-03-f. P28-03 수명과 통합을 검증한다. 입력: await·import가 대기 중인 페이지 이동/clean/dispose/JavaScript 비활성화, 실행 제한 초과, Promise rejection 관찰, 이벤트 루프 작업 순서; 기대: 오래된 문서 작업은 새 DOM·새 realm을 바꾸지 않고 강한 참조를 남기지 않음; 구현: `js.host.*`, `JsRealm`, 관련 수명 시험; 완료 조건: a~e 시험·엔진/브라우저 회귀·Netstorm 실행 전 smoke까지 통과하고 P28-03 상위 항목 체크. 완료(2026-11-25): `PageDisposalTest`가 requestDisposal 전에 queued await continuation이 DOM/global을 바꾸지 않음을 확인했고, ModernAsync limit가 await 뒤 단계 제한을 다시 적용함을 확인했다. 전체 엔진 675/0, 브라우저 3,688/0/12 skipped, sourcecheck 0, netstormparse syntax 0. 실제 게임 실행은 BigInt(P28-04)와 P28-08로 넘긴다.
- [x] P28-04. BigInt를 구현한다. 다음을 포함한다.
  - 원시 자료형(`typeof`가 `"bigint"`, `JsType`에 새 종류)과 래퍼 객체.
  - 산술·비트·시프트·비교·동등 연산, Number와 섞으면 TypeError, ToBigInt/ToNumeric.
  - `BigInt()`(생성자로 부르면 TypeError), `BigInt.asIntN/asUintN`, `toString(radix)`.
  - `BigInt64Array`/`BigUint64Array`, `DataView`의 `get/setBigInt64`·`get/setBigUint64`.
  - `JSON.stringify`의 TypeError, 구조화 복제(P20-09).
  - `js.embed`/`javax.script`의 Java 값 변환(`java.math.BigInteger`).

  번들이 BigInt 리터럴 129곳, `asIntN`/`asUintN` 127곳, `get/setBigUint64` 28곳을 쓰므로 게임 구동에 필수이다. 게임 쪽에서 하위 판본으로 트랜스파일해도 BigInt는 없앨 수 없다. 구현: `js.*`, `js.builtins.*`, `js.embed`. 완료 조건: 연산, 변환, 타입 배열, DataView 시험이 통과한다. 완료(2026-11-25): P28-04-a~d에서 원시값/연산/래퍼, BigInt64Array/BigUint64Array/DataView, JSON/구조화 복제/Java 경계와 문서를 구현했다. JRE 6u45 엔진 684/0, 브라우저 3,687/1/12(시간 의존 기존 시험 한 건이 전체에서 실패했고 `MetaRefresh`와 Web Audio 시험 각각 단독 재실행 통과), sourcecheck 0/1,964, Netstorm syntax/missing 0이다.
  - [x] P28-04-a. BigInt 원시값·연산·래퍼와 `BigInt` 내장 객체를 구현한다. 입력: 진법 변환, 산술/비트/시프트/비교/갱신 연산, Number 혼합, `asIntN/asUintN`, `new BigInt`, radix 문자열; 기대: ES2022 TypeError/RangeError/SyntaxError와 2의 보수 결과; 구현: `JsBigInt`, `Ops`, `Expressions`, `BigIntLib`; 완료 조건: BigInt 의미 시험과 전체 엔진 회귀 JRE 6 통과. 완료(2026-11-25): 원시 형식과 typeof, ToNumeric/ToBigInt, 산술·비트·정확한 Number 비교, 래퍼·toString/valueOf·asIntN/asUintN, 생성자 금지를 구현했고 `BigIntCoreTest` 4건과 핵심 속성 표 4건이 통과한다.
  - [x] P28-04-b. `BigInt64Array`/`BigUint64Array`와 DataView BigInt 접근자를 구현한다. 입력: 생성/복사/인덱스 읽기·쓰기, 엔디언, ToBigInt 변환, 다른 형식화 배열과 혼합, 정렬; 기대: 64비트 2의 보수와 Number/BigInt 변환 오류; 구현: `builtins.typed.*`; 완료 조건: typed-array/DataView 시험과 기존 9종 회귀 JRE 6 통과. 완료(2026-11-25): ElementType 두 종류와 JsValue 기반 원소 접근, 형식별 ToNumber/ToBigInt, copy/sort/fill/set/iterator, DataView get/setBigInt64/get/setBigUint64 를 구현했다. BigIntTypedArrayTest 4건과 형식화 배열 전체 86건이 통과한다.
  - [x] P28-04-c. JSON, 구조화 복제와 Java 경계의 BigInt를 연결한다. 입력: BigInt JSON 오류, wrapper toJSON, structured clone, Java `BigInteger` 왕복과 Java Script API; 기대: 복사된 정확한 값, JSON TypeError, 임의 Java 객체 비노출; 구현: `JsonBuiltins`, `StructuredClone`, `embed.JavaValues`, `js.script`; 완료 조건: 관련 엔진/브라우저 시험 JRE 6 통과. 완료(2026-11-25): JSON 원시/래퍼/배열의 TypeError와 replacer/toJSON 변환을 연결했고, 구조화 복제는 원시 BigInt·래퍼·BigInt 형식화 배열과 공유 버퍼를 정확히 복사한다. `java.math.BigInteger`는 Java→script BigInt, script→Java BigInteger, `javax.script` eval/binding 양방향으로 변환되며 관련 시험 5+11+14+16건이 통과한다.
  - [x] P28-04-d. P28-04 전체를 대조·검증·문서화한다. 입력: a~c의 시험과 Netstorm 번들의 BigInt 사용; 기대: 기능 표의 `ESX-BIGINT` 검증 완료와 실행 제한/지원 차이 기록; 구현: `docs/*`; 완료 조건: 전체 엔진/브라우저 회귀, sourcecheck, 외부 번들 구문 검사와 P28-04 기록 통과. 완료(2026-11-25): 기능 표·README·Netstorm 인계 문서를 갱신했고 BigInt 연산/typed array/DataView/JSON/clone/Java Scripting API 집중 시험이 통과했다. 두 전체 브라우저 실행에서는 서로 다른 기존 시간 의존 시험(MetaRefresh, Web Audio 스레드 종료)이 한 건씩 실패했으며 둘 다 즉시 단독 통과했다. Netstorm 번들 실행은 P28-08에서 계속한다.
- [x] P28-05. ES2016~ES2022 내장 객체를 추가한다. 대상은 다음과 같다.
  - [x] P28-05-a. Object/Array/String/Symbol/Promise/Error/TypedArray의 ES2017~ES2022 내장 메서드와 Error cause/AggregateError를 구현한다. 완료(2026-11-25): `Object.entries/values/fromEntries/getOwnPropertyDescriptors/hasOwn`, `Array.flat/flatMap/at`, String `at/replaceAll/matchAll` 표면, `%TypedArray%.at`, `Promise.allSettled/any/finally`, `Symbol.description`/`Symbol.matchAll`, Error `cause`, `AggregateError` 를 설치했다. `Es2022BuiltinsTest` 6건이 JDK 6u45 에서 통과한다. `String.matchAll` 과 RegExp 프로토콜 자체는 P28-05-b에서 연결한다.
  - [x] P28-05-b. 정규식 확장을 완성한다. 완료(2026-11-25): `groups`, `$<name>`, positive/negative lookbehind, `/d`/`hasIndices`/`indices`, `RegExp.prototype[Symbol.matchAll]`, Unicode 13.0 `General_Category`/`Script`/`Script_Extensions`와 이진 속성 escape 를 구현했다. `tools/unicodegen` 이 공식 Unicode 13.0 UCD 파일에서 `UnicodePropertyData.java` 를 만든다. 사용자 결정으로 Emoji 13 속성은 JAR 용량 때문에 생성하지 않고 `EXC-15`로 기록한다.
  - [x] P28-05-c. WeakRef/FinalizationRegistry와 ES2019 안정 정렬을 구현한다. 완료(2026-11-25): Java `WeakReference`/`ReferenceQueue`, realm 작업 큐의 cleanup 콜백, 현재 작업의 `deref` KeepDuringJob, unregister/token 의미와 GC 회수 시험을 추가했다. Array sort 는 기존 안정 병합 정렬을 ES2019 의미로 확인했다.
  - `Object.entries/values/getOwnPropertyDescriptors/fromEntries/hasOwn`.
  - `Array.prototype.flat/flatMap/at`, `%TypedArray%.prototype.at`.
  - `String.prototype.at/replaceAll/matchAll`과 `RegExp.prototype[@@matchAll]`.
  - `Promise.prototype.finally`, `Promise.allSettled/any`, `AggregateError`.
  - `Symbol.prototype.description`, `Symbol.asyncIterator`.
  - `WeakRef`와 `FinalizationRegistry`(Java `WeakReference`/`ReferenceQueue` 사용). `deref` 결과는 현재 작업이 끝날 때까지 유지하고, 정리 콜백은 작업으로 예약하며 페이지 수명을 넘지 않게 한다.
  - Error의 `cause` 옵션.
  - 정규식: 이름 있는 그룹의 결과(`groups`, `$<name>`), 후방 탐색 `(?<=`/`(?<!`, 유니코드 속성 이스케이프 `\p{}`, `d` 플래그(`hasIndices`, `indices`). `\p{}`의 속성 표는 `tools/` 도구로 UCD에서 생성하고 판본을 기록한다.
  - ES2019의 안정 정렬을 확인한다.

  구현: `js.builtins.*`, `js.regexp.*`. 완료 조건: P14-09와 같이 메서드별 이름·인수 개수·프로퍼티 속성과 동작 시험이 통과한다. 완료(2026-11-25): P28 집중 `Es2022BuiltinsTest`/`Es2022RegExpTest` 15건, 기존 RegExp 회귀 35건, WeakRef GC 시험 3회 연속, 엔진 전체 699/0, 브라우저와 엔진 전체 회귀는 `docs/PROGRESS.md` 에 기록한 결과로 검증했다.
- [ ] P28-06. 게임이 쓰는데 빠진 웹 API를 구현한다.
  1. `addEventListener`의 `signal` 옵션(DOM Standard). 이미 중단된 신호이면 등록하지 않고, 중단되면 리스너를 지운다. 게임은 모든 입력·크기 리스너를 이 방식으로 등록하고 정리한다.
  2. `fetch` 응답의 `body`(`ReadableStream`과 기본 리더의 `getReader/read/cancel/releaseLock`, Streams Standard 판본 기록). 게임은 `getReader`가 없으면 `arrayBuffer()`로 대신 읽으므로 구동이 막히지는 않는다. 다만 적재 진행률이 덜 세밀해진다.
  3. `navigator.clipboard.readText/writeText`(Clipboard API). 보안 문맥과 사용자 활성화 조건을 정하고, P11-08의 시스템 클립보드 연결을 재사용한다.
  4. `URL.createObjectURL/revokeObjectURL`. 2026-11-24 판정에서 `Blob`/`FileReader`/`URL.createObjectURL` 중 적어도 하나가 없었다(개별 판정은 하지 않음). `blob:` 주소는 상단의 허용 스키마(http/https/ftp) 및 P01-12의 차단 정책과 충돌하므로, 구현하기 전에 사용자 결정을 받아 기록한다. 이 기능이 없으면 게임은 화면 저장 내려받기만 하지 않는다.

  완료 조건: 실제 컴포넌트 페이지 시험이 통과하고 기능 표 5.4절을 갱신한다.
- [ ] P28-07. 큰 스크립트의 실행 성능과 실행 제한을 측정하고 개선한다. 번들 하나가 약 327만 자이고, private 이름 3만여 곳과 클래스 필드 6천여 곳을 쓰며, `requestAnimationFrame` 프레임 루프로 돈다. JDK 6u45와 7u80에서 다음을 측정해 기록한다.
  - 파싱 시간과 메모리(JRE 6/7 기본 힙 포함).
  - 프레임당 스크립트 시간.
  - 자산 적재(바이트 배열 처리) 시간.

  `ExecutionLimits`의 단계·시간 한도가 정상적인 게임 작업을 중단시키지 않는지 확인하고, 필요하면 페이지별 한도 정책을 조정한다(P13-12의 무한 반복 보호는 유지). 병목은 P29에서 ES2026 실행 의미가 모두 확정된 뒤 P13-01이 별도 작업으로 남긴 최적화(식별자 해석 캐시, 프로퍼티 조회 캐시 등)로 개선하고, ES2015~ES2026 의미 시험 전체를 다시 통과시킨다. 완료 조건: 측정 결과와 목표(예: 타이틀 화면까지의 시간, 메인 메뉴의 프레임 유지)를 문서에 기록하고 그 목표를 만족한다.
- [ ] P28-08. NetstormJava 게임 페이지의 구동을 확인한다. 외부 프로젝트의 `src/main/resources`를 클래스패스에 두고, JDK 7u80과 JDK 6u45에서 다음 두 페이지를 연다.
  - `classpath://www/index.html`(번들)
  - `classpath://www/debug.html`(ES 모듈 직접 적재)

  콘솔 리스너(P24)로 잡히지 않은 오류가 없는지 확인한다. 로딩 안내 → 자산 적재 → "클릭하여 시작" → 메인 메뉴(원본 그림, 한국어 글꼴, 버튼 입력, 효과음, 메뉴 음악)까지 진행되는지도 확인한다. 문법 오류를 넘은 뒤 드러나는 실패는 원인 계층(파서/실행기/내장 객체/DOM·웹 API/렌더링/오디오/성능)을 나누어 하위 항목(`P28-08-a` …)으로 추가하고, 해당 계층의 시험과 함께 고친다. 게임 자산을 이 저장소에 복사하지 않으므로 자동 회귀에는 같은 기능을 쓰는 자체 시험 페이지를 둔다. 실제 화면과 소리는 사람이 확인하는 항목으로 따로 기록한다(P22-10). 완료 조건: 메인 메뉴까지 오류 없이 진행됨을 기록한다.
- [ ] P28-09. 다음 문서를 갱신한 뒤 P28을 완료 처리한다.
  - 기능 표 7.5절(`ESX-*`)과 5.4절(웹 API 행)
  - README(지원하는 ECMAScript 범위)
  - `docs/progress/netstorm.md`
  - PROGRESS

### P29. ECMAScript 2026 규격 완성 및 이전 버전 ES 호환성 검증

선행 작업: P12, P13, P14, P15, P20-09, P23, P26, P28-01~05. 진행 순서상 **`P28-05` 직후(`P28-06~09` 및 `P27`보다 먼저)** 수행한다. 산출물: ES2016~ES2022 잔여 규격 보완, ES2023~ES2026 언어 문법·실행 의미·내장 객체·이진 데이터·컬렉션·정규식·JSON 모듈 구현, 이전 버전 ES(ES1~ES5.1, ES2015~ES2025) 및 웹 호환(Annex B) 검증, 호스트·독립 엔진 경계 갱신과 ES2026 적합성 표.

2026-11-25 상단 "목표 - 지원 기능"의 JavaScript 지원 기준이 `ES6`에서 **`ES2026`(및 이전 버전의 ES 호환)**으로 상향됨에 따라 신설했다. 본문 정본은 저장소의 오프라인 규격서 `docs/es2026/`(ECMA-262 제17판, June 2026 HTML 사본)이고, 판본별 변천 대조에는 `docs/ECMA-262_16th_edition_june_2025.pdf`(제16판, ES2025)와 `docs/es2022/`(제13판, ES2022)를 함께 참고한다. 각 항목이 여러 클래스나 많은 규격 절로 나뉘면 구현 전에 `P29-04-a` 같은 하위 번호로 세분화하여 작은 단위로 검증·commit한다.

- [x] P29-01. 기준 판본과 전체 격차·호환 대상 목록을 확정한다. `docs/es2026/`(ECMA-262 17th edition, June 2026)의 장·절 구성과 규격 요구사항(2절 Conformance 포함)을 대조하여 `docs/FEATURE_MATRIX.md` 0절과 7절(7.1~7.7)에 고정한다. ES2016~ES2022 중 `P28-05` 시점에 미구현·부분 구현·판정 필요로 남은 항목(`ESX-SHARED`, `ESX-SYNTAX-2018`, `ESX-SEMANTICS-2019`, `ESX-GLOBALTHIS`)과 ES2023(제14판)~ES2026(제17판)의 모든 신규 문법·추상 연산·내장 객체를 빠짐없이 기능 표 행(`ES26-*`)으로 정리한다. ECMA-262 2절이 조건부로 규정하는 국제화 API(ECMA-402 `Intl`: 제공하는 경우에만 해당 판본 준수 요구, 미제공 시 ECMA-262 본문의 `toLocaleString`/`localeCompare` 기본 의미 적용)의 지원 범위/예외 정책도 근거와 함께 명확히 기록한다. 완료 조건: 기능 표 7절에 ES2026 이하의 누락 조항이 없고 항목별 구현 패키지·검증 계획이 연결된다. 완료(2026-11-25): 오프라인 정본 `docs/es2026/`(53,284줄, 1~29장·부속서 A~F)과 `tools/webfetch` 온라인 스냅샷(`tc39.es/ecma262/2026/` HTTP 200·7,530,216B, `raw.githubusercontent.com/tc39/ecma262/es2026/spec.html` HTTP 200·2,979,557B)을 고정하고, JDK 6u45 현행 엔진 44건 실측 감사로 `CharClass` Unicode 4.0 격차·`Reflect.enumerate` 잔존·`ESX-*` 잔여분·`ES26-*` 23행·`ES-COMPAT-*` 3행(`B.3.5`/`B.3.9` 포함)·`DomExceptionObjects` 오류 브랜드 연동 대상을 확정했다. `Intl`은 §2 조건부 적합성과 JRE 6/JAR 용량 근거로 명시적 예외 `EXC-16`으로 확정하고 비-ECMA-402 기본 메서드 준수 범위를 연결했다(`docs/FEATURE_MATRIX.md` 0·3·7·13절, `docs/progress/es2026.md`).
- [x] P29-02. ES2016~ES2022 잔여 문법·실행 의미와 최신 Unicode 정합성을 보완한다. 다음을 구현·검증하여 7.5절의 부분 구현·판정 필요 항목을 해소한다.
  - 태그 있는 템플릿 리터럴에서 잘못된 이스케이프 시퀀스의 cooked 값을 `undefined`로 전달하고 raw 문자열은 보존하며, 태그 없는 템플릿 리터럴에서만 조기 `SyntaxError`를 내는 규칙(ES2018 Template Literal Revision)과 템플릿 객체의 Parse Node(소스 위치)별 캐싱(ES2018 Annex F).
  - 문자열 리터럴 안의 U+2028/U+2029 직접 허용(ES2019 JSON superset), `JSON.stringify`의 고립 서로게이트 `\uXXXX` 이스케이프(well-formed UTF-16 반환, ES2019), `Function.prototype.toString`의 원본 소스 텍스트 보존 또는 표준 네이티브 자리표시자 반환(ES2019), `for-in` 열거 순서 표준화(ES2020), `Array.prototype.sort`의 정밀 정렬 순서와 예외/변경 경계(ES2021), `globalThis` 속성 기술자(`writable: true, enumerable: false, configurable: true`)와 브라우저 `WindowProxy`·Worker·독립 엔진 일관성(ES2020).
  - ES2026 2절이 요구하는 최신 Unicode 기준에 맞춰 식별자(`ID_Start`/`ID_Continue`), 공백(`U+180E` 비공백 처리 등), 정규식 `\p{}` 표 생성기(`tools/unicodegen`)의 UCD 판본을 점검·갱신한다(JAR 크기 제약에 따른 Emoji 속성 `EXC-15` 유지 여부는 용량 측정 결과와 함께 기록).
  구현: `js.parser.*`, `js.interp.*`, `js.builtins.*`, `js.regexp.*`, `tools/unicodegen`. 완료 조건: 각 항목의 경계 사례 시험이 JDK 6에서 통과하고 7.5절 해당 행이 `검증 완료`로 갱신된다.
  - [x] P29-02-a. 태그 템플릿의 잘못된 8진·16진·유니코드 이스케이프를 `Lexer`와 `Expressions`에서 확인한다. 조각별 cooked/raw와 태그 없는 경우의 조기 오류, 같은 위치·다른 위치·다른 realm의 템플릿 객체 동일성을 JDK 6에서 검증한다.
  - [x] P29-02-b. `Lexer`에서 인용 문자열의 U+2028/U+2029 허용과 CR/LF 거부를 확인한다. `JsonBuiltins`에서 값과 키의 고립 서로게이트를 이스케이프하고 유효한 쌍을 보존하며, 둘 다 JDK 6에서 검증한다.
  - [x] P29-02-c. `FunctionLib`, 파서와 인터프리터에서 함수·메서드·클래스·동적 생성자의 원문과 바운드·내장·프록시의 네이티브 구문을 JDK 6에서 검증한다.
  - [x] P29-02-d. `EnumerateSupport`와 `Statements`에서 정수 키 정렬, 프로토타입 가림, 순회 중 삭제, 프록시의 자체 키 순서와 심볼 제외를 JDK 6에서 검증한다.
  - [x] P29-02-e. `ArrayMethods`에서 희소·상속 배열의 수집·정렬·쓰기·삭제 순서, 비교 함수와 프록시 예외 경계를 JDK 6에서 검증한다.
  - [x] P29-02-f. `JsRealm`, 현재 문서의 브라우저 창·Worker·AudioWorklet의 `globalThis` 동일성과 프로퍼티 기술자를 각 실행 환경의 JDK 6 시험으로 검증한다.
  - [x] P29-02-g. `CharClass`, 정규식 속성 표와 `tools/unicodegen`에서 최신 UCD 판본을 사용한다. 새 식별자, U+180E, 비이모지 속성 및 이모지 속성 거부를 JDK 6에서 검증하고 JAR 크기를 측정한다.
    - [x] P29-02-g1. `tools/unicodegen`의 Unicode 18.0.0 입력과 공유 표를 엔진에 연결한다. 새 식별자와 공백 분류, 정확한 속성 별칭, Script_Extensions 기본값과 대체 범위, 생성 파일 재현 및 JAR 크기를 JDK 6에서 검증한다.
    - [x] P29-02-g2. Unicode 판본 영향을 받는 정규식 식별자와 대소문자 데이터의 소비 지점을 대조한다. 엔진이 JRE 6의 문자 데이터에 기대는 경계를 확인하고 새 코드 포인트와 Annex B 차이를 검증한다.
  - [x] P29-02-h. `FrameApi`, 브라우징 문맥과 `JsRealm`에서 이동 전후 WindowProxy와 `globalThis`의 동일성을 검증한다. 이전부터 창을 가리키던 부모의 참조가 같은 오리진의 새 문서를 보고, 오리진이 바뀌면 같은 참조에 접근 제한이 적용되는지 JDK 6의 실제 프레임 이동으로 확인한다.
    - [x] P29-02-h1. `JsRealm`과 인터프리터에서 호스트가 지정한 별도 전역 this 값으로 `globalThis`와 스크립트의 최상위 this를 연결한다. 전역 변수와 식별자 해석은 전역 객체에 남으며 비엄격 호출, eval, 모듈, 독립 엔진의 차이를 JDK 6에서 검증한다.
    - [x] P29-02-h2. 프레임 브라우징 문맥별 WindowProxy를 브라우저에서 사용한다. 같은 오리진 사이의 동작, 이동 후 기존 참조의 현재 문서 연결, 교차 오리진 재판정과 폐기, 메시지 출처와 window 별칭을 실제 HTTP 프레임의 JDK 6 시험으로 확인한다.
      - [x] P29-02-h2a. 현재 실행 realm을 추적한다. 다른 realm의 함수·생성자·제너레이터·모듈로 들어갔다가 예외로 끝나도 호출자의 realm이 복원되어야 한다. JDK 6 엔진 시험에서 속성 접근으로 관찰한다.
      - [x] P29-02-h2b. `PageScripts`, `WindowGlobals`, `FrameApi`의 창 진입점을 문맥별 WindowProxy에 연결한다. 보관한 참조의 오리진 재검사와 창 별칭, 프레임 메시징을 JDK 6 통합 시험으로 검증한다.
- [x] P29-03. ES2023~ES2025 어휘·구문·모듈 확장(Hashbang, Import Attributes, JSON 모듈)을 구현한다.
  - Hashbang 주석(`12.5 Hashbang Comments`): Script와 Module 시작 오프셋 0의 `#!...` 한 줄 주석을 허용하고(`InputElementHashbangOrRegExp`), 공백·BOM 뒤나 함수 본문 등 허용되지 않는 위치의 조기 오류 및 `eval`/`Function` 처리 규칙을 규격대로 검증한다.
  - Import Attributes(`16.2.2`, `13.3.10`): 정적 `import ... with { key: "value" }`, `export ... from ... with { ... }`, 동적 `import(specifier, options)` 두 번째 인수(`options.with`), `ModuleRequest Record`와 속성 동등성(`ModuleRequestsEqual`), 중복 키 조기 오류와 호스트 미지원 속성 거부(`AllImportAttributesSupported`/`HostGetSupportedImportAttributes`)를 구현한다.
  - JSON 모듈(`16.2.1.8 Synthetic Module Records`, `ParseJSONModule`): `with { type: "json" }`으로 요청한 JSON 리소스를 파싱하여 `default` 내보내기 하나만 가진 합성 모듈로 평가한다. 독립 엔진 `ModuleLoader`와 브라우저 `<script type="module">`·동적 `import()`·Worker·AudioWorklet 요청 계층에 연결하며, 브라우저에서는 JSON MIME 검사·CORS·모듈 캐시 키(지정자+속성) 분리를 적용한다(`type: "json"` 없이 JSON MIME이 오거나 반대의 경우 실패).
  구현: `js.parser.*`, `js.ast.*`, `js.interp.*`, `js.ModuleLoader`, `js.host.*`, `js.worker.*`. 완료 조건: 엔진 단독 모듈 시험과 브라우저 HTTP 통합 시험(정적/동적 JSON import, MIME 거부, CORS, 캐시 분리)이 JDK 6에서 통과한다.
  - [x] P29-03-a. `Lexer`에서 첫 문자부터 시작한 해시뱅을 한 줄 주석으로 읽는다. Script, Module, eval 허용과 공백·BOM·함수 인수 뒤 거부, 탐색 위치와 기존 private 식별자를 JDK 6에서 검증한다.
  - [x] P29-03-b. `Parser`, `ProgramInfo`와 모듈 레코드에 정적 가져오기·재내보내기의 속성을 포함한다. 문자열·식별자 키, 중복과 오류, 키 순서에 독립적인 요청 동일성을 JDK 6에서 검증한다.
  - [x] P29-03-c. 표현식 파서와 인터프리터에서 동적 `import()` 옵션을 읽고 `ModuleLoader`로 넘기기 전에 호스트 속성을 검사한다. 옵션 평가 순서와 실패의 Promise 전달을 JDK 6에서 검증한다.
  - [x] P29-03-d. 엔진 모듈 레코드에서 JSON 합성 모듈의 `default`와 속성별 캐시를 구현한다. 정적·동적 가져오기, 재내보내기, 값과 오류 수명을 JDK 6에서 검증한다.
  - [x] P29-03-e. 브라우저 페이지·Worker·AudioWorklet 로더에 JSON 모듈 요청을 전달한다. HTTP 서버로 MIME 및 CORS 검사와 캐시 분리를 JDK 6에서 검증한다.
    - [x] P29-03-e1. `PageRuntime`, `PageScripts`의 문서 모듈 요청에 타입을 전달한다. 정적·동적 JSON 가져오기에서 UTF-8, MIME과 CORS, 같은 URL의 타입별 캐시 분리를 `ModuleScriptHttpTest`에서 검증한다.
    - [x] P29-03-e2. `WorkerScope`와 Worker 설정에 모듈 로더를 연결한다. Worker 메시지로 받은 JSON 값, 기존 스크립트와 오리진 검사를 실제 HTTP 시험에서 대조한다.
    - [x] P29-03-e3. `ModuleGraphFetcher`와 `WorkletScope`의 선취득 키에 타입을 포함한다. JSON 의존성에서 등록된 프로세서의 값, MIME 오류, 기존 AudioWorklet 시험을 검증한다.
- [x] P29-04. ES2023~ES2026 배열·형식화 배열·이진 데이터(`Float16Array`, `Uint8Array` Base64/Hex, 가변 `ArrayBuffer`)를 구현한다.
  - 복사 기반 배열 변경과 역방향 탐색(ES2023): `Array.prototype`의 `findLast`, `findLastIndex`, `toReversed`, `toSorted`, `toSpliced`, `with`(희소 배열 빈 슬롯을 `undefined`로 채우고 `@@unscopables`는 §23.1.3.41의 이름을 따름) 및 `%TypedArray%.prototype`의 `findLast`, `findLastIndex`, `toReversed`, `toSorted`, `with`(species를 보지 않는 `TypedArrayCreateSameType` 적용).
  - 비동기 배열 생성(ES2026, `23.1.2.2`): `Array.fromAsync(items [, mapper [, thisArg]])` — async iterable·sync iterable·array-like 입력의 순차 소비, 입력 종류에 따른 값의 `await`와 매퍼 결과 대기, 매핑·쓰기 오류 시 열린 이터레이터 닫기(`IfAbruptCloseAsyncIterator`), 생성자 `this` 처리.
  - 반정밀도 부동소수점(ES2025): 순수 Java IEEE 754 binary16 변환기(짝수 방향 반올림 ties-to-even, 비정규수, 부호 있는 0, 무한대, NaN 비트 보존)로 `Math.f16round(x)`, `Float16Array`, `DataView.prototype.getFloat16(byteOffset [, littleEndian])`·`setFloat16(byteOffset, value [, littleEndian])`을 구현한다.
  - `Uint8Array` Base64·Hex 메서드(ES2026, `23.3`): `Uint8Array.fromBase64(string [, options])`, `Uint8Array.fromHex(string)`, `Uint8Array.prototype.toBase64([options])`, `toHex()`, `setFromBase64(string [, options])`, `setFromHex(string)`을 구현한다(`alphabet`: `"base64"`/`"base64url"`, `lastChunkHandling`: `"loose"`/`"strict"`/`"stop-before-partial"`, `omitPadding`, ASCII 공백 건너뛰기, `{ read, written }` 결과, 분리·가변 버퍼 경계 검사).
  - 가변 길이·전송 가능 `ArrayBuffer`(ES2024, `25.1`·`10.4.5`): `new ArrayBuffer(length, { maxByteLength })`, `ArrayBuffer.prototype.maxByteLength`·`resizable`·`resize(newLength)`·`detached`·`transfer([newLength])`·`transferToFixedLength([newLength])`를 구현하고, 가변 버퍼를 바라보는 `TypedArray`/`DataView`의 자동 길이 추적과 경계 이탈 동작(`TypedArray/DataView With Buffer Witness Records`, `IsTypedArrayOutOfBounds`, `IsViewOutOfBounds`)을 전 메서드에 반영한다.
  구현: `js.num.*`, `js.builtins.stringarray.*`, `js.builtins.typed.*`, `js.builtins.corelib.MathLib`. 완료 조건: 각 메서드의 이름·`length`·속성 기술자·경계값(희소 배열, IEEE 754 half 경계, Base64 패딩/오류, 버퍼 축소 중 콜백 부작용, 분리 후 접근) 시험이 JDK 6에서 통과한다.
  - [x] P29-04-a. `ArrayMethods`에 역방향 탐색과 복사 메서드를 추가한다. 희소 배열의 밀집 복사, 인덱스 읽기 순서, 변경 대상 슬롯의 getter 회피와 범위 오류를 JDK 6에서 검증한다.
  - [x] P29-04-b. `TypedArrayMethods`에 역방향 탐색과 복사 메서드를 추가한다. 일반·BigInt 배열의 결과, 수치 정렬, 동일 타입 생성과 species 접근 회피를 JDK 6에서 검증한다.
  - [x] P29-04-c. `ArrayBuiltins`와 비동기 반복 경로에 `Array.fromAsync`를 추가한다. 반복자 종류, await 순서, 매퍼 예외와 반복자 닫기를 JDK 6에서 검증한다.
    - [x] P29-04-c1. `PromiseInterop`와 `AsyncIteratorHelper`에서 내장 Promise 대기, 동기 반복자의 next·done 조회 순서와 거부 시 닫기를 검증한다.
    - [x] P29-04-c2. `ArrayBuiltins`와 `ArrayFromAsync`에서 반복자 또는 유사 배열의 순차 변환과 생성자 호출을 구현한다. 부작용 순서, 오류별 닫기 및 결과 Promise를 검증한다.
  - [x] P29-04-d. 반정밀도 변환과 `Math.f16round`, `Float16Array`, DataView 접근을 추가한다. 짝수 반올림, 부호 있는 0 및 경계 비트열을 JDK 6에서 검증한다.
  - [x] P29-04-e. `ArrayBufferLib`와 View 연산에 가변 길이, 전송, 자동 길이와 경계 검사 지원을 추가한다. 버퍼 축소 후 접근과 콜백 부작용을 JDK 6에서 검증한다.
    - [x] P29-04-e1. `ArrayBufferObject`와 `ArrayBufferLib`에 최대 길이와 재할당을 추가한다. 옵션 변환, 길이 접근자, 축소·확대와 바이트 보존을 JDK 6에서 검증한다.
    - [x] P29-04-e2. `TypedArrayObject`, 생성자와 메서드에 자동 길이와 경계 이탈을 반영한다. 축소·확대, 경계 복구, 콜백과 형식화 배열의 복사·쓰기 순서를 검증한다.
    - [x] P29-04-e3. `DataViewObject`, 생성자 및 접근자에서 버퍼의 현재 경계를 사용한다. 자동·고정 길이, 엔디언, 값 변환 중 축소·분리와 결과 오류를 검증한다.
    - [x] P29-04-e4. `ArrayBuffer.prototype`의 resize와 transfer 2종을 연결한다. 범위 실패, 재할당의 0 채움, 전송 성공 뒤 분리와 기존 뷰의 동작을 스크립트에서 검증한다.
  - [x] P29-04-f. `Uint8Array`의 Base64·Hex 변환을 추가한다. 알파벳, 패딩, 접두부 처리, 버퍼 길이·분리 경계 및 read/written을 JDK 6에서 검증한다.
    - [x] P29-04-f1. `Uint8ArrayConversions`를 등록하고 Hex 3종을 구현한다. 입력·출력 형식, 제한 길이, 불완전 입력, 오류 전 접두부 저장, 분리·가변 버퍼를 시험한다.
    - [x] P29-04-f2. Base64 3종을 구현한다. 알파벳, ASCII 공백, 패딩, 마지막 조각 처리, 출력 범위와 관찰 가능한 옵션 접근을 시험한다.
- [x] P29-05. `SharedArrayBuffer`와 `Atomics`(ES2017·ES2024, `25.2`·`25.4`·`29`)를 구현한다.
  - `SharedArrayBuffer` 생성자(`{ maxByteLength }` 성장 옵션 포함), `byteLength`, `maxByteLength`, `growable`, `grow(newLength)`(축소 금지·원자적 확장), `slice`, `@@species`/`@@toStringTag`를 구현한다.
  - Java 6 표준 공개 API(`java.util.concurrent.atomic`, 모니터/잠금 등 — `sun.misc.Unsafe` 및 네이티브 코드 금지)만으로 공유 바이트 블록과 8/16/32/64비트 정수 `TypedArray`(`Int8`~`Uint32`, `BigInt64`/`BigUint64`)에 대한 원자적 읽기·쓰기·산술·비트·교환 연산(`Atomics.load`, `store`, `add`, `sub`, `and`, `or`, `xor`, `exchange`, `compareExchange`, `isLockFree`)을 구현한다.
  - 대기·통지(`Atomics.wait`, `Atomics.notify`, ES2024 `Atomics.waitAsync`): 에이전트 차단 허용 규칙(`AgentCanSuspend`: 브라우저 주 문서/EDT 연결 스레드에서는 `Atomics.wait`가 즉시 `TypeError`, Worker 스레드에서는 대기 허용하되 Worker 종료·페이지 폐기·실행 제한 취소 시 즉시 깨어나 정리)과 `waitAsync`의 `{ async, value }` 반환 및 타임아웃/해결 작업 큐 연동을 구현한다.
  - 브라우저 `postMessage`/Worker 사이의 `SharedArrayBuffer` 공유 복제 정책(동일 에이전트 클러스터 공유, 교차 오리진/호스트 경계 거부 규칙)을 확정하고 문서화한다.
  구현: `js.builtins.typed.*`, `js.host.structured.*`, `js.worker.*`. 완료 조건: 다중 스레드/Worker 원자성·경합·`wait`/`notify`/`waitAsync`·성장 버퍼·폐기 시 대기 해제 시험이 JDK 6에서 통과한다.
  - [x] P29-05-a. 공유 바이트 저장소를 Java 6 원자 배열로 구현한다. 같은 워드의 서로 다른 크기 접근과 확장 중 기존 바이트 보존을 다중 스레드 시험으로 검증한다.
  - [x] P29-05-b. `SharedArrayBuffer`의 생성·조회·grow·slice와 TypedArray/DataView 연결을 구현한다. 고정/성장 길이, 공유 범위 복사, 엔디언과 뷰 경계를 검증한다.
    - [x] P29-05-b1. `ElementType`, `ArrayBufferObject`와 공유 버퍼에서 공통 요소·바이트 접근을 구현한다. 일반 버퍼와 공유 뷰의 엔디언, 복사, 겹침을 검증한다.
    - [x] P29-05-b2. `SharedArrayBufferLib`를 설치하고 크기와 병행 성장, slice·species 및 일반 ArrayBuffer 메서드의 거부를 스크립트에서 검증한다.
  - [x] P29-05-c. `Atomics`의 읽기·쓰기·읽기-수정-쓰기와 isLockFree를 구현한다. 정수·BigInt, 범위와 인수 변환, 스레드 경합을 검증한다.
  - [x] P29-05-d. `wait`, `notify`, `waitAsync`와 호스트의 차단 허용·취소 처리를 구현한다. 대기열 순서, 무한·유한 대기, 타임아웃과 폐기를 검증한다.
    - [x] P29-05-d1. `AtomicsWait`, `SharedWaiters`, `ExecutionLimits`에서 대기 위치별 FIFO, 동기 대기와 비동기 Promise, 기한·취소를 구현한다. 독립 realm으로 즉시 결과, 서로 다른 위치, 통지 개수·순서, 시간 초과와 대기 해제를 검사한다.
    - [x] P29-05-d2. `PageScripts`, `WorkerScope`, `WorkletScope`에 차단 허용 설정과 비동기 결과를 호스트 작업에 올리는 경로를 연결한다. 페이지와 AudioWorklet은 차단을 거부하고 Worker는 차단할 수 있으며, 새 스크립트 실행 없이도 타임아웃 결과가 전달되는지 검증한다.
  - [x] P29-05-e. 브라우저의 동일 에이전트 클러스터 구조화 복제를 연결한다. Worker와 원본 뷰가 공유 바이트를 관찰하는 사례, 다른 오리진/클러스터의 거부와 전송 목록 검사, 종료 시 대기 해제를 통합 검증한다.
    - [x] P29-05-e1. `StructuredClone`, `ClonedValue`, `SharedBufferReference`를 연결한다. 같은 클러스터·오리진의 래퍼와 길이 추적 뷰만 복원하고, 다른 오리진·클러스터 및 일반 저장용 직렬화와 transfer 항목은 거부하는 것을 검증한다.
    - [x] P29-05-e2. Window, Worker, MessagePort, BroadcastChannel, AudioWorklet의 메시징에 클러스터 키와 역직렬화 실패 처리를 연결한다. 로컬 HTTP로 공유 바이트 왕복, Worker 대기 해제·폐기, 다른 오리진에서의 `messageerror`를 검증한다.
    - [x] P29-05-e3. 공유 뷰를 받는 Web IDL 바이트 경계를 확인한다. `[AllowShared]` 대상인 텍스트 변환과 공유를 거부하는 Blob·요청 바디 처리를 검증한다.
      - [x] P29-05-e3-a. `TypedArrays`, `EncodingApi`, `BlobApi`, `BodyApi`, `XhrApi` 및 `LegacyWebApi`에서 공유 여부와 크기 변경 가능 여부를 각 IDL 타입에 맞게 판정한다. 텍스트 변환의 공유 바이트 읽기·쓰기, Blob·요청 바디의 공유 뷰 거부와 합집합 문자열 변환, Crypto 입력 경계를 검증한다.
      - [x] P29-05-e3-b. `js.host.webaudio`와 `js.host.canvas`의 실시간 바이트 접근에서 IDL 경계를 적용한다. 입력 검증이 공유 버퍼의 사본에 쓰거나 공유 버퍼를 분리하기 전에 실행되는지 관련 회귀 시험으로 검증한다.
- [x] P29-06. ES2023~ES2026 컬렉션·약한 참조·이터레이터 헬퍼(`Iterator`, `Set`, `Map`/`WeakMap`, Weak Symbol 키)를 구현한다.
  - 약한 참조의 Symbol 키 허용(ES2023, `9.13 CanBeHeldWeakly`): `Symbol.for`로 등록되지 않은 모든 Symbol(`KeyForSymbol(sym) === undefined`, well-known symbol 포함)을 `WeakMap`, `WeakSet`, `WeakRef`, `FinalizationRegistry`(대상 및 unregisterToken)에서 허용하고 등록 심볼은 `TypeError`로 거부한다.
  - 데이터 그룹화(ES2024): `Object.groupBy(items, callback)`(null 프로토타입 객체, 프로퍼티 키 강제 변환)과 `Map.groupBy(items, callback)`(`CanonicalizeKeyedCollectionKey` 적용)을 구현한다.
  - `Set` 집합 연산 7종(ES2025, `24.2`): `intersection`, `union`, `difference`, `symmetricDifference`, `isSubsetOf`, `isSupersetOf`, `isDisjointFrom`을 `GetSetRecord` 프로토콜(`size`, `has`, `keys` 검증과 집합 크기 기반 순회 선택, 순회 중 변경 안전성)대로 구현한다.
  - `Map`/`WeakMap` Upsert(ES2026, `24.1.3`·`24.3.3`): `Map.prototype.getOrInsert(key, value)`, `getOrInsertComputed(key, callback)`, `WeakMap.prototype.getOrInsert(key, value)`, `getOrInsertComputed(key, callback)`을 구현한다(`-0`→`+0` 정규화, 콜백 호출 시점 재조회·삽입, 약한 키 유효성 선행 검사).
  - 전역 `Iterator`와 이터레이터 헬퍼(ES2025·ES2026, `27.1.2`~`27.1.3`): 전역 `Iterator` 생성자(직접 `new Iterator()`는 `TypeError`, 서브클래스 `super()` 허용, 기존 `%IteratorPrototype%`와 동일 객체로 통합), `Iterator.from(O)`(`%WrapForValidIteratorPrototype%`), **ES2026 `Iterator.concat(...items)`**, `%IteratorHelperPrototype%` 지연 헬퍼(`map`, `filter`, `take`, `drop`, `flatMap`)와 즉시 소비 헬퍼(`reduce`, `toArray`, `forEach`, `some`, `every`, `find`), 조기 종료·예외 시 내부 이터레이터 닫기(`IteratorClose`/`IteratorCloseAll`), `Iterator.prototype.constructor`·`@@toStringTag` 접근자(`SetterThatIgnoresPrototypeProperties`)를 구현한다.
  구현: `js.builtins.corelib.*`, `js.builtins.datajson.*`, `js.builtins.meta.*`, `js.interp.*`. 완료 조건: 컬렉션·약한 심볼 GC·이터레이터 지연 평가/정리/서브클래싱 시험이 JDK 6에서 통과한다.
  - [x] P29-06-a. `Ops.canBeHeldWeakly`, `CollectionBuiltins`, `WeakReferenceBuiltins`, `JsRealm`에 약한 키·대상·토큰 규칙을 적용한다. 비등록·등록·well-known Symbol의 각 메서드 결과, WeakRef의 현재 작업 보관, Java 6 수거 뒤 정리와 해제를 검증한다.
  - [x] P29-06-b. `Object.groupBy`, `Map.groupBy`의 공통 그룹 순회를 구현한다. 순서, 프로퍼티 키, 음의 0과 NaN, 콜백·키 변환 예외 후 이터레이터 닫기를 검증한다.
  - [x] P29-06-c. `CollectionBuiltins`와 `OrderedTable`을 사용해 Set 프로토콜과 7개 집합 연산을 구현한다. 크기 분기, 이터레이터 닫기, 실행 중 원본 변경과 순서를 검증한다.
    - [x] P29-06-c1. `SetOperations`의 GetSetRecord와 결과 Set을 생성하는 4개 연산을 구현한다. 상대 크기, getter·keys의 관찰 순서, 중복 키, 순회 중 삽입·삭제, 내장 Set 결과를 검증한다.
    - [x] P29-06-c2. `SetOperations`의 집합 조건 검사 3개 연산과 `IteratorRecord`의 정상 닫기를 구현한다. 조기 반환이 상대 이터레이터의 return을 호출하는지, 원본 변경에 따른 멤버 판정과 크기 기반 분기를 검증한다.
  - [x] P29-06-d. Map/WeakMap upsert를 구현한다. 원래 값이 undefined인 키, 콜백 중 콜렉션 변경, 제로 정규화와 약한 키 검사를 검증한다.
  - [x] P29-06-e. `IteratorBuiltins`와 `IteratorHelper`의 전역 Iterator, 래핑, 지연·즉시 소비 및 concat을 구현한다. 서브클래싱, 작업 순서, 닫기·오류 전파, 기술자를 검증한다.
    - [x] P29-06-e1. `IteratorConstructor`와 `IteratorRecord`를 사용해 전역 Iterator와 from, 래퍼 next·return, 프로토타입 접근자를 설치한다. 직접 생성 거부, 서브클래싱, 기존 이터레이터 동일성과 기술자를 검증한다.
    - [x] P29-06-e2. `IteratorEagerMethods`에서 reduce, toArray, forEach, some, every, find를 설치한다. 단락, 빈 이터레이터, 다음 메서드 캡처와 오류 시 닫기를 검증한다.
    - [x] P29-06-e3. `IteratorLazyMethods`와 공통 helper 프로토타입에서 map, filter, take, drop을 설치한다. 평가 지연, 0·무한·음수 한계, 시작 전 return, 재진입과 중단을 검증한다.
    - [x] P29-06-e4. flatMap과 Iterator.concat에서 현재 안쪽 이터레이터와 바깥 이터레이터의 수명과 순서를 관리한다. 접근자 평가·실제 열기 시점, 내부·외부 닫기 순서, 오류 우선순위를 검증한다.
- [x] P29-07. ES2024~ES2026 문자열·정규식(`isWellFormed`/`toWellFormed`, `v` 플래그, 중복 이름 그룹, 인라인 수정자, `RegExp.escape`)을 구현한다.
  - 유니코드 정합 문자열(ES2024): `String.prototype.isWellFormed()`와 `toWellFormed()`(고립 상위/하위 서로게이트 판정 및 `U+FFFD` 치환)를 구현한다.
  - 정규식 `v`(`unicodeSets`) 플래그(ES2024, `22.2`): `RegExp.prototype.unicodeSets` 접근자, `u`·`v` 동시 지정 조기 `SyntaxError`, 중첩 문자 클래스(`[...]`), 집합 교집합(`&&`)·차집합(`--`), 문자열 리터럴 대안(`\q{abc|d}`), 다중 문자 집합 매칭 우선순위, `v` 모드의 예약 구두점 이스케이프 규칙·단순 대소문자 폴딩(`MaybeSimpleCaseFolding`)·문자 보완 집합(`CharacterComplement`)을 구현한다(Emoji 문자열 속성은 `EXC-15` 정책과의 관계를 문서화하고 일관되게 처리).
  - 정규식 중복 이름 캡처 그룹(ES2025): 서로 다른 대안 분기(`(?<x>a)|(?<x>b)`)에서 같은 그룹 이름을 허용하고 같은 분기 내 중복은 조기 `SyntaxError`로 유지하며(`MightBothParticipate`), `exec().groups`, `indices.groups`, 역참조 `\k<x>`, `$<x>` 치환이 일치한 분기의 캡처 값을 사용하게 한다.
  - 정규식 인라인 패턴 수정자(ES2025): `(?ims:...)`, `(?-ims:...)`, `(?ims-ims:...)` 그룹 내부에서만 `i`/`m`/`s` 플래그를 변경·해제하는 구문과 조기 오류(중복 플래그, 빈 수정자 `(?-:...)`, 허용되지 않는 플래그 `g`/`u`/`v`/`y`/`d`), 런타임 매칭(`UpdateModifiers`)을 구현한다.
  - 정규식 이스케이프(ES2025): `RegExp.escape(S)`(문자열 비강제 `TypeError`, 선두 영숫자 `\x..` 이스케이프, 구문 문자·공백·제어 문자·서로게이트 이스케이프)를 구현한다.
  구현: `js.builtins.stringarray.StringBuiltins`, `js.builtins.datajson.RegExpBuiltins`, `js.regexp.*`. 완료 조건: 구문 조기 오류·집합 연산·다중 문자 매칭·인라인 플래그·중복 그룹·이스케이프 왕복 시험이 JDK 6에서 통과한다.
  - [x] P29-07-a. `CodePoints`와 `StringBuiltins`에서 짝 없는 서로게이트 판정과 치환을 구현한다. 유효 쌍, 위치별 짝 없는 코드 유닛, 수신 객체의 문자열 변환과 기술자를 검증한다.
  - [x] P29-07-b. `RegExpFlags`, 정규식 파서·컴파일러·매처와 스크립트 내장 객체에 Unicode Sets를 구현한다.
    - [x] P29-07-b1. v, unicodeSets, u·v 충돌 및 전체 Unicode 실행 경로를 연결한다. 리터럴·생성자, 플래그 조회 순서, 전역·sticky·matchAll의 서로게이트 이동을 검증한다.
    - [x] P29-07-b2. v 클래스의 코드 포인트 집합, 중첩·교집합·차집합, 예약 문자의 문법을 구현한다. 대소문자 폴딩과 보완 집합 차이를 검증한다.
    - [x] P29-07-b3. `\q{...}`와 문자·문자열 집합의 결합 및 매칭을 구현한다. 긴 대안 우선과 백트래킹, 빈 대안·보완 제한과 EXC-15의 속성 거부를 검증한다.
  - [x] P29-07-c. 서로 배타적인 대안의 중복 캡처 이름을 허용한다. 동일 분기 조기 오류, 이름 역참조와 치환, 결과의 groups와 indices를 검증한다.
  - [x] P29-07-d. i·m·s 인라인 패턴 수정자를 구현한다. 플래그 조기 오류, 중첩 변경·복원, 범위와 백트래킹을 검증한다.
  - [x] P29-07-e. RegExp.escape를 구현한다. 타입, 첫 문자, 구문 및 예약 구두점, 제어·공백·서로게이트와 리터럴 왕복을 검증한다.
- [x] P29-08. ES2024~ES2026 Promise·Error·Math·JSON·Date 확장을 구현한다.
  - Promise 확장: `Promise.withResolvers()`(ES2024, 서브클래스 `NewPromiseCapability(this)` 지원), `Promise.try(callback, ...args)`(ES2025, 동기 반환·동기 throw·thenable 반환 모두 Promise로 귀결, 서브클래스 지원).
  - Error 확장(ES2026, `20.5.2.1`): `Error.isError(arg)` — 프로토타입 체인이 아닌 내부 오류 브랜드(`[[ErrorData]]`) 존재 여부를 판정하여 교차 realm `Error`는 `true`, `Object.create(Error.prototype)` 및 일반 객체·원시값·Proxy는 `false`를 반환한다(브라우저 `DOMException`에 대한 명세 요구도 함께 검증).
  - Math 확장(ES2026, `21.3.2.34`): `Math.sumPrecise(items)` — 이터러블에서 숫자를 읽어(비숫자는 즉시 `TypeError` + 이터레이터 닫기) 중간 반올림 손실 없이 무한 정밀도 합을 구한 뒤 IEEE 754 배정밀도로 한 번만 반올림한다(빈 입력 `-0`, 모두 `-0`이면 `-0`, `+Infinity`와 `-Infinity` 공존 시 `NaN`, `NaN` 포함 시 `NaN`, 실행 제한 단계 수 반영).
  - JSON 원문 접근(ES2026, `25.5`): `JSON.rawJSON(text)`(공백을 제외한 단일 원시 JSON 토큰만 허용하고 객체·배열·빈 문자열·잘못된 숫자/서로게이트는 `SyntaxError`, 동결된 null 프로토타입 원시 JSON 객체 반환), `JSON.isRawJSON(O)`, `JSON.stringify`의 rawJSON 원문 삽입, `JSON.parse` reviver 콜백의 3번째 인수 `context`(원시 토큰 노드에 `{ source }` 전달, 배열·객체이거나 상위 reviver가 교체한 값에는 빈 `{}` 전달)를 구현한다.
  - Date·시간대 오프셋 문자열 규격(ES2026, `21.4`·부속서 A.7): 시간대 오프셋 문자열 형식(`±HH:MM`, 초/소수점 등 규격 A.7)과 `Date.parse`/시간대 처리 규칙을 ES2026 본문과 대조·보완한다.
  구현: `js.builtins.meta.PromiseBuiltins`, `js.builtins.corelib.{ErrorLib,MathLib}`, `js.builtins.datajson.{JsonBuiltins,DateBuiltins}`, `js.date.*`, `js.text.*`. 완료 조건: 각 기능의 정밀도·경계·오류·이터레이터 정리 시험이 JDK 6에서 통과한다.
  - [x] P29-08-a. `PromiseBuiltins`에서 NewPromiseCapability를 사용해 withResolvers 및 try를 제공한다. 일반·하위·사용자 생성자, 반환·throw·thenable, 함수 호출 순서와 기술자를 검증한다.
  - [x] P29-08-b. `ErrorLib`, 기본 `ErrorBuiltins`, 브라우저 `DomExceptionObjects`에서 오류 내부 브랜드를 연결한다. 다른 realm, 원시값, 프로토타입 변조, 모방 객체, 프록시 및 Web IDL DOMException을 검증한다.
  - [x] P29-08-c. `MathLib`와 수치 연산 계층에 sumPrecise를 추가한다. 반올림·상쇄·오버플로우, 부호 있는 0, 비유한 값, 오류 시 이터레이터 닫기와 실행 제한을 검증한다.
  - [x] P29-08-d. `JsonBuiltins`와 JSON 텍스트 파서에서 원문 JSON 및 reviver 문맥을 구현한다.
    - [x] P29-08-d1. rawJSON, isRawJSON, stringify에 내부 브랜드와 토큰 원문 출력을 연결한다. 단일 원시 토큰과 구문 오류, 프로토타입·동결 상태, replacer·toJSON 호출 순서를 검증한다.
    - [x] P29-08-d2. JSON.parse에서 원문 위치가 있는 reviver context를 전달한다. 복합 값, 중복 키, 정밀 수치, 형제 속성의 교체·삭제, 빈 context와 기술자를 검증한다.
  - [x] P29-08-e. `js.date`에서 제17판의 시간대 오프셋과 Date 파싱을 대조한다.
    - [x] P29-08-e1. `TimeZoneOffsets`에서 A.7 문자열 판정과 나노초 오프셋을 구현한다. 콜론, 초·소수, 부호 및 초과 범위를 검증한다.
    - [x] P29-08-e2. `DateParser`와 `DateBuiltins`에 Date Time String Format과 호스트 시간대 적용을 연결한다. 날짜 전용, 현지 시간, 명시 오프셋, 경계와 기존 표준·비표준 형식을 검증한다.
- [x] P29-09. 이전 버전 ECMAScript(ES1~ES5.1, ES2015~ES2025) 하위 호환성 및 웹 호환(Annex B)을 전수 검증한다.
  - 상단 요구사항 `이전 버전의 ES 호환 필요`를 충족하기 위해 ES3/ES5 비엄격(sloppy) 코드(`var`/함수 선언 호이스팅, 매핑된 `arguments`와 `arguments.callee`, `with` 문과 `@@unscopables`, 생성자 프로토타입 체인, `eval`의 호출 환경 바인딩, ES3/ES5 표준 내장 메서드 전체)와 ES2015~ES2025 기능들이 ES2026 엔진 위에서 회귀 없이 함께 동작하는지 검증한다.
  - ECMA-262 제17판 2.2절 `Legacy` 조항 및 웹 브라우저 호환 부속서 B(`B.1` HTML 주석·느슨한 정규식 패턴, `B.2` `escape`/`unescape`·`String#substr`/HTML 메서드·`Date#getYear/setYear/toGMTString`·`RegExp#compile`·`__proto__`·`__defineGetter__` 계열, `B.3.1`~`B.3.5` 라벨/블록/if 함수 선언·catch `var`·`for-in` 초기화식, `B.3.6` `[[IsHTMLDDA]]` 정책, `B.3.7`~`B.3.9` 호스트 콜백·함수 호출 좌변 런타임 `ReferenceError`)를 본문과 대조하고 미비점을 보완한다.
  - 부속서 E·F에 명시된 구판 대비 표준 변경 사항(예: `ToNumber("0b10")`, `U+180E` 비공백, 함수 `length` configurable, `Object.keys` 등의 원시값 `ToObject` 강제 변환)은 최신 정본인 ES2026 의미를 따름을 시험과 문서로 고정한다.
  구현: `js.parser.*`, `js.interp.*`, `js.builtins.meta.LegacyBuiltins*`, 호환성 회귀 시험. 완료 조건: 구판(ES3/ES5)·부속서 B·판본 전이 적합성 시험 묶음이 JDK 6에서 통과한다.
  - [x] P29-09-a. `js.parser`와 `js.interp`의 Annex B 문법과 평가를 보완한다.
    - [x] P29-09-a1. B.3.5의 비엄격 for-in var 초기화식을 허용한다. 초기화·우변 평가 순서, 단일 평가, null/undefined, 중단과 재개, 엄격 코드와 잘못된 선언의 조기 오류를 검증한다.
    - [x] P29-09-a2. B.3.9의 함수 호출 대입 대상에 대한 구문과 런타임 ReferenceError를 연결한다. 대입·복합 대입·증감과 반복문 대상, 호출 부작용, 예외 순서와 엄격 코드의 조기 오류를 검증한다.
  - [x] P29-09-b. `ReflectBuiltins`에서 최신 판본에 없는 Reflect.enumerate를 제거하고 `EditionTransitionTest`에서 Annex E·F의 숫자 변환, 공백, 공개 프로퍼티, Date·RegExp 프로토타입 동작을 검증한다.
  - [x] P29-09-c. `js.regexp`에서 B.1.2의 느슨한 문법과 이후 판본의 정규식 동작을 검증한다.
    - [x] P29-09-c1. 초기 확장 문법, 리터럴과 생성자의 조기 오류, 다양한 플래그의 검색 시작·실패 시 인덱스 갱신을 확인한다.
    - [x] P29-09-c2. 뒤쪽 탐색을 역방향 평가와 대조한다. 반복·분기 우선순위, 역참조, 기존 캡처 및 단언과 섞인 경우를 검증한다.
  - [x] P29-09-d. B.3.6~8의 호스트 동작과 HTML 레거시 상태를 대조한다. 독립 엔진과 브라우저의 전역 객체·콜백 정책 및 오류 전파를 확인한다.
    - [x] P29-09-d1. B.3.6과 HTML의 `document.all`을 연결한다. 독립 엔진과 프록시에는 브랜드가 없고, 브라우저 컬렉션은 불리언·느슨한 비교·typeof, 호출, 이름과 인덱스 및 DOM 변경을 확인한다.
    - [x] P29-09-d2. B.3.8의 호스트 private 요소 추가 제한을 연결한다. 일반 객체에는 추가할 수 있고 브라우저 `WindowProxy`와 `Location`에는 예외가 발생하는지 검증한다.
    - [x] P29-09-d3. B.3.7의 Promise 작업 콜백이 등록 시 활성 스크립트와 호스트 설정을 유지하는지 확인한다. 독립 엔진의 기본 동작과 브라우저 모듈 해석, 오류 전파를 검증한다.
      - [x] P29-09-d3a. 실행 범위에 활성 스크립트·모듈 키를 연결한다. eval과 함수·모듈 재개에서 상대 import가 정의 위치를 사용하고 종료 시 범위가 복원되는지 확인한다.
      - [x] P29-09-d3b. 브라우저의 Promise 반응·thenable 콜백 기록에 등록 시 실행 범위와 incumbent settings를 보존한다. 지연 실행, 예외, 문서 기준 URL 대체를 검증한다.
  - [x] P29-09-e. `EsLegacyCompatTest`와 `AnnexB17ConformanceTest`에서 구판 문법·내장·부속서 B.1/B.2/B.3과 최신 기능의 혼합 실행을 검증하고, 발견한 차이와 보완 결과를 기능 표에 반영한다.
- [ ] P29-10. 호스트·독립 엔진 경계 갱신과 ES2026 전체 적합성 대조를 완료한다.
  - 브라우저 `StructuredClone`(P20-09), Worker/AudioWorklet, 독립 엔진 `js.embed.JsEngine` 및 `javax.script`(P26)에서 ES2026의 모든 자료형(`Float16Array`, 가변/분리 `ArrayBuffer`, `SharedArrayBuffer`, `Error` cause/AggregateError, JSON 모듈 등)이 일관되게 동작하고 엔진 모듈의 무의존 경계(`engine-boundary`)가 유지되는지 검증한다.
  - `ConformanceTable`(전역·생성자·프로토타입·정적 메서드·접근자·심볼 프로퍼티의 존재, `name`, `length`, 속성 기술자, 프로토타입 사슬)을 ECMA-262 제17판(19~28장 및 부속서 B.2) 전체 목록으로 확장하고 허용 목록(`PENDING`)에 설명 없는 누락이 없게 한다.
  - JDK 6u45에서 엔진 단독 시험 전체, 브라우저 전체 회귀 시험, 예제 smoke, `tools/sourcecheck` 0 위반을 확인한다. `docs/FEATURE_MATRIX.md`(7절 및 13절 재집계), `docs/ARCHITECTURE.md`, `README.md`, `docs/PROGRESS.md`를 갱신한 뒤 P29를 완료 처리한다.
  - [x] P29-10-a. 내장 객체 표와 자체 검사기를 제17판 기준으로 갱신하고 발견한 구현 차이를 보완한다.
    - [x] P29-10-a1. 19~22장 및 B.2의 전역, 기본 객체, 숫자·날짜, 텍스트를 대조한다. 함수 이름·길이, 접근자 및 속성 기술자, 별칭 정체성과 프로토타입을 검사한다.
    - [x] P29-10-a2. 23~25장의 인덱스·키 컬렉션과 이진 데이터, Atomics, JSON을 대조한다. 신규·기존 생성자와 각 프로토타입의 전체 속성 표를 검증한다.
    - [x] P29-10-a3. 26~28장의 약한 참조, 이터레이터·제너레이터·Promise·Reflect·Proxy를 대조한다. 내부 프로토타입, 심볼과 제약 목록을 검증한다.
      - [x] P29-10-a3a. WeakRef·FinalizationRegistry·Promise·Reflect·Proxy의 프로퍼티와 생성자 계층을 대조한다. 선택 또는 확장 프로퍼티는 표에서 근거와 함께 관리한다.
      - [x] P29-10-a3b. Iterator와 헬퍼, Array·TypedArray·String·RegExp·Map·Set 이터레이터의 내부 프로토타입을 대조한다. 같은 프로토타입과 메서드가 공유되어야 하는 수신자를 검사한다.
      - [x] P29-10-a3c. 제너레이터·async 함수·async 제너레이터의 생성자와 프로토타입을 대조한다. Function 계열의 동적 생성, newTarget 및 중단·재개의 실행 결과를 검증한다.
      - [x] P29-10-a3d. Async-from-Sync 어댑터를 공용 내장 프로토타입에 연결한다. for await와 yield*가 next를 한 번 읽고 return·throw를 호출 시점에 조회하는지 확인한다.
  - [x] P29-10-b. 구조화 복제와 독립 엔진 경계를 보완한다. 가변 버퍼·뷰, Float16Array, 공유 버퍼 정책, Error cause·AggregateError, JSON 모듈을 호스트별 정상 진입점과 JSR 223에서 확인한다.
    - [x] P29-10-b1. 구조화 복제·전송에서 가변 버퍼의 최대 길이와 형식화 배열·DataView의 길이 추적을 보존한다. 분리·범위 이탈, 전송 전 재진입, 창·Worker·AudioWorklet 메시지를 검증한다.
    - [x] P29-10-b2. Error의 cause, AggregateError의 errors, 수신 렐름 프로토타입 및 공유·순환 참조를 구조화 복제에서 검증한다. 잘못된 호스트 객체는 DataCloneError를 확인한다.
    - [x] P29-10-b3. 독립 엔진 및 JSR 223의 최신 값과 JSON 모듈 로더 경계를 검증한다. Java 값 변환, 바이트·생명 주기 및 패키지 분리를 점검한다.
  - [ ] P29-10-c. 브라우저 렐름 경계를 대조한다. 전역 `WindowProxy`의 정체성과 반사 연산, 타 렐름 eval이 사용하는 활성 모듈 레코드의 소유 렐름, 이벤트 속성의 중첩 실행, COOP/COEP와 공유 메모리 조건을 확인한다.
    - [x] P29-10-c1. `RealmExecution`과 `Interpreter`에서 활성 레코드의 소유 렐름을 모듈 로더와 연결한다. 타 렐름 eval과 Promise 콜백은 원문 소유 렐름에서 모듈을 불러오고, `PageScripts`의 이벤트 속성은 컴파일 문맥을 복제하지 않는지 확인한다.
    - [x] P29-10-c2. `WindowProxy`와 브라우징 문맥을 실제 주 문서 이동 및 프레임 시험으로 대조한다. 정체성, 동일·교차 오리진에서 프로토타입과 키·기술자, 이전 참조의 동작을 확인한다.
      - [x] P29-10-c2a. `WindowProxy`, `CrossOriginWindow`, `FrameApi`에서 숫자 프레임 인덱스와 교차 오리진 속성의 키·기술자를 확인한다. 실제 하위 프레임에서 허용·거부 및 프로토타입 변경 결과를 시험한다.
      - [x] P29-10-c2b. `TopContext`의 최상위 창 정체성을 `BrowserSession` 범위로 유지한다. 이동 전후 같은 프록시가 현재 전역 객체에 연결되는지, 파싱 중 새 문서 자체 접근과 취소된 이동을 검증한다.
    - [ ] P29-10-c3. 문서와 Worker의 COOP/COEP 설정, `crossOriginIsolated`, 공유 메모리 노출과 전송을 헤더에 따라 시험한다. 응답 메타데이터, 페이지 및 Worker 설정과 `SharedMemoryScope`에 정책을 적용한다.
      - [x] P29-10-c3a. 최종 응답의 COOP·COEP 토큰과 신뢰 가능한 오리진을 검사해 페이지 및 스크립트 로더에 전달한다. 리디렉션·잘못된 토큰·헤더 중복·루프백 및 안전하지 않은 오리진을 검증한다.
      - [x] P29-10-c3b. 문서 요청에 COEP·CORP 제한을 적용한다. 교차 오리진의 no-cors 하위 리소스와 프레임, CORS 접근, credentialless의 쿠키와 캐시·리디렉션을 검증한다.
      - [ ] P29-10-c3c. 창·Worker·AudioWorklet의 격리 권한과 공유 버퍼 유효 범위를 연결한다. 두 헤더를 가진 신뢰 가능한 문서와 Worker의 왕복, 헤더 누락·격리 불가 창의 생성자와 복제 오류, 독립 엔진 동작을 검증한다.
  - [ ] P29-10-d. Java 6에서 정본의 Unicode 정규화와 ECMA-402 없는 기본 지역화 동작을 확인한다. Unicode 18 입력, 정규화 형식과 문자열·수치·날짜의 경계를 검증한다.
  - [ ] P29-10-e. JDK 6u45 엔진·브라우저 전체 회귀, 예제 smoke, 엔진 분리, sourcecheck를 실행하고 7절과 13절 및 사용자·구조·진행 문서를 갱신한다.

## 중간 실행 목표와 다음 작업 선택

| 실행 목표 | 필요한 단계 | 확인해야 하는 동작 | 완료 판정의 범위 |
| --- | --- | --- | --- |
| 빈 Swing 컴포넌트 | P00~P03 | 생성, JScrollPane, 빈 페이지, 기본 설정과 상태 조회 | 컴포넌트 골격 |
| 정적 HTML 페이지 | P04~P09 및 P11의 링크 처리 | HTTP/클래스패스, 자체 파싱·표시, 링크, 원문과 이력 | 실제 구현한 HTML/CSS 기능 |
| 자체 JavaScript 1차 기반(ES2015) | P11~P15 및 필요한 선행 단계 | ES2015 실행, DOM 변경, 입력/이벤트, 원문 보존 | 실제 검증한 ES2015·DOM 기능 |
| 저장소·미디어 통합 | P16~P19 | 저장소 공유·분리, MIME, 이미지/오디오, 캐시와 새로고침 | 대상 표에서 검증한 기능 |
| Maven 빌드 | P23 | JDK 6 대상 Maven 빌드·시험·패키징, 기존 회귀 결과 유지 | 빌드 구조 |
| 콘솔 이벤트·Web Audio | P24~P25 | 콘솔 리스너 통지, 오디오 그래프 렌더링과 출력 | 실제 검증한 추가 기능 |
| 독립 JavaScript 엔진 | P26 | 엔진 단독 실행, `javax.script` 사용, 브라우저 동작 유지 | 엔진 분리와 JSR 223 |
| ES2016~ES2022 핵심 확장 | P28-01~P28-05 | ES2016~ES2022 문법·실행 의미·BigInt·내장 객체 | 실제 검증한 ES2022 핵심 기능 |
| **ES2026 완성 및 이전 ES 호환** | **P29** | **ES2023~ES2026 전체 문법·내장 객체·이진 데이터·JSON 모듈, ES2016~ES2022 잔여 항목, ES1~ES5.1 및 Annex B 하위 호환** | **실제 검증한 ES2026 및 이전 판본 호환** |
| 웹 API 보강·성능·게임 구동 | P28-06~P28-09 | 부족한 웹 API, ES2026 엔진 기준 대규모 스크립트 성능 목표, NetstormJava 메인 메뉴 구동 | 실제 검증한 웹 API·성능·게임 구동 |
| Java↔스크립트 메시지 | P27 | Java가 보낸 `message` 이벤트 수신, `window.opener`/`event.source`로 Java에 전송·응답, ES2026 값 변환과 오리진 검사 | 실제 검증한 호스트 메시지 기능 |
| 요구사항 전체 완료 | P00~P29 | 표준 기능 표(HTML5/CSS3/ES2026/이전 호환), JRE 6 실제 실행과 모든 공개 API | 상단 목표 전체, P23~P29 목표와 명시적 예외 |

다음 AI가 작업을 시작할 때는 실행 목표만 보고 앞 단계를 생략하지 않는다. 예를 들어 `P15-12`가 미완료이고 P15의 이전 항목이 끝났다면, 새 기능을 더하기 전에 통합 페이지의 오류 원인을 확인한다. 파서 문제인지 실행기 문제인지 DOM 연결 문제인지 입력/그리기 문제인지 분리된 검증으로 확인하고, 수정 후 해당 계층과 통합 사례를 다시 검증한다.

`docs/PROGRESS.md`에는 다음 형태로 기록한다. 아래 예시 값은 개발 완료 근거가 아니다.

```text
현재 작업 번호: P00-01
완료한 내용: 아직 구현 작업을 시작하지 않음
변경 파일: 실제 변경한 파일을 기록
검증 결과: 실제 실행한 명령, 환경, 성공/실패와 확인하지 못한 내용을 기록
미해결 사항: 실패 입력, 기대 결과, 실제 결과, 원인과 필요한 작업을 기록
다음 작업 번호: 선행 조건이 충족된 첫 미완료 번호를 기록
```
