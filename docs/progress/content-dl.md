# CONTENT-DL 작업 기록 (Markdown 변환기 P17-03/04, 다운로드 팝업 P11-09)

작업자: CONTENT-DL. 소유 경로: `src/org/duckdns/hjow/swingwebpanel/content/markdown/**`, `tests/src/org/duckdns/hjow/swingwebpanel/content/markdown/**`,
`tests/resources/org/duckdns/hjow/swingwebpanel/markdown/**`, `src/org/duckdns/hjow/swingwebpanel/download/**`, `tests/src/org/duckdns/hjow/swingwebpanel/download/**`,
`examples/src/org/duckdns/hjow/swingwebpanel/examples/DownloadPopupExample.java`, 이 문서.

## 2026-11-16 · JOB 2 — 다운로드 팝업 (P11-09)

현재 작업 번호: P11-09 (관리자/팝업/시험 완료, 컴포넌트 설치는 조정자 몫)
다음 작업 번호: 없음(CONTENT-DL 의 두 작업 모두 끝남). 조정자: SwingWebPanel 연결 → P11-09/P01-18/P22-06 의 통합 확인

### 완료한 내용 (`org.duckdns.hjow.swingwebpanel.download`)

| 클래스 | 책임 |
| --- | --- |
| `SwingDownloadHandler` (`core.DownloadHandler` 구현) | 다운로드 관리자. `downloadRequired(...)`/`start(DownloadRequest)` 로 시작, 팝업 하나당 전송 스레드 하나(동시 처리), `cancelAll()`, `dispose()`, `getActiveCount()`, `awaitIdle(ms)`. protected: `createDownloadPopup(DownloadRequest, DownloadControl)`, `isHeadless()`, `getDefaultDirectory()` |
| `DownloadTask` | 다운로드 하나: 부분 파일 `<이름>.part-<난수>`(대상 디렉터리)에 스트리밍, 진행 보고(약 100 ms 간격), 취소 토큰, 완료 시 교체 이동/실패·취소 시 부분 파일 삭제 |
| `DownloadRequest` | 불변 요청(최종 URL, MIME, Content-Disposition, 선언 크기, User-Agent, 추가 헤더 복사본, 안전한 파일 이름). `DownloadRequest.of(NavigationRequest, DownloadRequiredException)` |
| `DownloadFileNames` | 파일 이름 결정/정리: `filename*`(RFC 8187, 문자셋)·`filename`(UTF-8 바이트가 한 바이트씩 읽힌 경우 복구)·URL 마지막 구간·`download`; 경로 구분자/드라이브 문자/제어·서식 문자(RTL 덮어쓰기)/Windows 금지 문자/끝의 마침표·공백/예약 장치 이름(CON, PRN, AUX, NUL, COM1-9, LPT1-9)/길이 150(확장자 유지) |
| `DownloadHandle`, `DownloadControl`, `DownloadState`, `DownloadStatusSink` | 팝업 인터페이스(EDT 에서만 호출), 팝업이 관리자에게 하는 요청(취소/대상 변경), 상태 열거형, 상태 메시지 수신기 |
| `SwingDownloadDialog` (`JDialog`, 비모달) | 실제 팝업: 파일/출처/저장 위치/크기, 진행 막대(크기를 모르면 불확정), 받은 양과 속도, `Save as...`(`JFileChooser`), `Cancel`→`Close`, 결과 줄. 창 닫기 = 실행 중이면 취소 |
| `SafeFiles` | 고유 부분 파일 만들기와 안전한 교체(JRE 6/Windows 에서 기존 파일 위로 이름 바꾸기가 안 되므로 옛 파일을 옆으로 옮기고 새 파일을 옮기며 실패 시 되돌림, 볼륨이 다르면 복사 대체) |

- 전송은 컴포넌트의 요청 계층(`ResourceFetcher`: 스킴 제한, SSL 확장 지점, 제한 값 + 컴포넌트의 `CookieStore`, 이동 요청의 User-Agent/추가 헤더)을 그대로 쓴다(`RequestPurpose.DOWNLOAD`, 본문을 파일로 스트리밍).
- 기본 저장 위치는 `~/Downloads`(없으면 홈). 처음 대상으로 바로 내려받기 시작하며 `Save as...` 로 바꾼 대상은 완료 시 적용(부분 파일은 처음 대상 디렉터리에 있다가 완료 때 이동).
- 완료 시 대상이 이미 있으면 팝업에서 교체 확인 → 아니오면 `chooseTarget` → 취소하면 다운로드는 CANCELLED 로 끝나고 기존 파일은 그대로이다.
- 완료된 파일은 `cancelAll()`/`dispose()` 로도 지우지 않는다. 팝업은 사용자가 닫을 때까지 남고, `cancelAll()` 은 실행 중인 것의 팝업만 닫는다.
- 헤드리스(`GraphicsEnvironment.isHeadless()`): 전송하지 않고 `DownloadStatusSink` 로 "Download cancelled (no display available): <이름>" 만 알린다. `dispose()` 뒤에는 새 다운로드를 받지 않는다.

### 컴포넌트 연결 방법 (조정자가 `SwingWebPanel`/`BrowserSession` 에서 할 일)

```java
// SwingWebPanel 에서 (네트워크 로더를 만드는 곳과 같은 fetcher 를 쓴다)
protected DownloadHandle createDownloadPopup(DownloadRequest request, DownloadControl control) {   // API 계약 7절의 protected 확장 지점
    return new SwingDownloadDialog(SwingUtilities.getWindowAncestor(this), request, control);
}
...
final SwingDownloadHandler downloads = new SwingDownloadHandler(this, fetcher, session.getCookieStore(), statusSink) {
    protected DownloadHandle createDownloadPopup(DownloadRequest r, DownloadControl c) {
        return SwingWebPanel.this.createDownloadPopup(r, c);     // 호스트/시험이 SwingWebPanel 의 메서드를 재정의하면 그것을 쓴다
    }
};
session.setDownloadHandler(downloads);                        // BrowserSession.setDownloadHandler 가 이미 있다
```

- `clean()` → `downloads.cancelAll()`, `dispose()` → `downloads.dispose()` (그 뒤 `awaitIdle` 로 부분 파일 삭제를 기다릴 수 있음).
- `statusSink` 는 `BrowserSession` 의 상태 메시지(`statusText`)를 바꾸는 얇은 어댑터이다. 지금 코어에는 주 문서 요청과 무관한 상태 메시지 설정 API 가 없다면 `null` 을 넘겨도 동작한다(상태 메시지만 빠진다).
  다운로드는 `getPage()/getContent()/이력/진행률` 을 바꾸지 않으므로 상태 메시지는 `progress` 를 건드리지 않는 별도 경로여야 한다.
- API 계약 7절은 `protected DownloadHandle createDownloadPopup(DownloadRequest)` 로 적었으나 팝업이 취소/대상 변경을 요청할 수 있도록 `DownloadControl` 인수를 하나 더 받는다(계약 문장 갱신 요청).
- 팝업의 부모 창은 `SwingUtilities.getWindowAncestor(owner)` (컴포넌트가 창에 붙기 전이면 부모 없음).

### 변경 파일

`src/org/duckdns/hjow/swingwebpanel/download/`: `package-info.java`(기존), `SwingDownloadHandler.java`, `DownloadTask.java`, `DownloadRequest.java`, `DownloadFileNames.java`, `DownloadHandle.java`, `DownloadControl.java`, `DownloadState.java`, `DownloadStatusSink.java`, `SwingDownloadDialog.java`, `SafeFiles.java`;
`tests/src/org/duckdns/hjow/swingwebpanel/download/`: `DownloadFileNamesTest.java`, `SafeFilesTest.java`, `DownloadManagerTest.java`, `SwingDownloadDialogTest.java`, `HoldingServer.java`(시험 보조 서버);
`examples/src/org/duckdns/hjow/swingwebpanel/examples/DownloadPopupExample.java`(수동 확인과 `--smoke`).

### 검증 결과

- JDK 6u45: `scripts/pkgtest.sh cdl-dl "download net core storage dom" "download"` → **34 passed / 0 failed / 0 skipped** (기반 시설 7건 포함, 다운로드 시험 27건). Markdown 과 합쳐 `scripts/pkgtest.sh cdl-all ...` → 52 passed / 0 failed.
  - 파일 이름: Content-Disposition 14형태(한글 `filename*`, 문자셋, 잘못된 퍼센트 인코딩 대체, UTF-8 복구), URL 대체 10가지, 악의적 이름 24가지(`../../x`, `C:\x`, `C:x`, `CON.txt`, `NUL.tar.gz`, `aux.`, 제어/RTL 문자, 금지 문자, 끝의 점/공백 …), 긴 이름/서로게이트.
  - 관리자(가짜 팝업 + 실제 로컬 서버): 크기를 아는/모르는/빈 파일 다운로드와 상태 순서·진행·결과 줄·상태 메시지, User-Agent/추가 헤더/쿠키 저장·재전송, HTTP 404/500, 잘린 응답, 연결 거부, 차단 스킴(`file:`),
    **전송 도중 취소**(서버가 연결을 붙잡고 있고 클라이언트 연결이 닫힘을 서버가 확인, 부분 파일 삭제), `cancelAll`(두 다운로드 취소·팝업 닫힘·부분 파일 삭제·완료 파일 보존), 헤드리스 거부와 dispose 후 거부,
    덮어쓰기 확인 예/아니오+다른 이름/아니오+포기, 8개 동시 다운로드, 전송 중 대상 변경, 없는 대상 디렉터리 생성과 만들 수 없는 경우의 실패, 한글/악의적 이름 적용, `core.DownloadHandler` 인터페이스 경유.
  - 안전한 교체: 새 이름/기존 파일 위로(JRE 6 Windows 의 rename 실패 경로 실행), 실패 시 옛 파일 복원·원본 보존, 디렉터리 대상 거부, 없는 디렉터리 생성, 복사.
  - 실제 `SwingDownloadDialog` 를 EDT 에서 만들고 모든 상태로 갱신·해제(이 환경은 화면이 있어 실행됨; 헤드리스에서는 이유와 함께 건너뜀).
- `examples` 의 `DownloadPopupExample --smoke`(`-Djava.awt.headless=true`) → "OK (… 3145728 bytes)", 종료 코드 0.
- `tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/download tests/src/org/duckdns/hjow/swingwebpanel/download examples/src/org/duckdns/hjow/swingwebpanel/examples/DownloadPopupExample.java` → 0 위반.

### 확인하지 못한 것 (정직한 기록)

- 실제 팝업의 시각적 모양과 `JFileChooser`/교체 확인 대화상자의 조작은 자동으로 확인하지 못했다. `examples/run.sh DownloadPopupExample` 로 직접 확인해야 한다(P22-10 의 "실제 Swing 확인" 항목).
- `NavigationRequest` 를 실제 이동 흐름(`BrowserSession` → `DownloadHandler`)에서 받는 통합은 컴포넌트에 설치된 뒤 P11-09/P22-06 에서 확인해야 한다(여기서는 `DownloadRequest` 와 `downloadRequired(null, ...)` 로 확인).
- 이어받기(Range)와 다운로드 중 파일 이름 변경 감지는 구현하지 않았다.

### 미해결 사항

- 위 "컴포넌트 연결 방법"의 설치 코드와 상태 메시지 어댑터는 조정자가 `SwingWebPanel`/`core` 에 넣어야 한다.

### 체크박스 후보

- **P11-09** 의 구현과 자동 검증(파일 이름 정리, 부분 파일→완료 시 이동, 덮어쓰기 확인, 취소/실패 시 삭제, 여러 다운로드 동시 처리, 요청 계층 사용, EDT 갱신, 헤드리스 거부) 은 끝났다. 다만 "패널이 속한 창을 부모로 하는 JDialog" 와 `SwingWebPanel` 에서의 실제 호출은
  연결 뒤에야 확인되므로 **연결 후에** 체크하는 것을 권한다(그 전에는 후보 아님).
- P01-18/P21-03 의 다운로드 팝업 정리(`cancelAll`/`dispose`)도 연결 뒤 확인 항목이다.

### 조정자 요청

- `SwingWebPanel`/`BrowserSession` 에 위 설치 코드와 `clean()`/`dispose()` 연결, 다운로드 상태 메시지 경로를 추가해 주세요(필요하면 `DownloadStatusSink` 대신 코어 쪽 API 로 바꿔도 됩니다).
- `docs/API_CONTRACT.md` 7절의 `createDownloadPopup` 시그니처를 `(DownloadRequest, DownloadControl)` 로 갱신하고, FEATURE_MATRIX 의 다운로드 행(`org.duckdns.hjow.swingwebpanel.download.*`)을 "부분 구현(관리자/팝업 검증 완료, 컴포넌트 연결 전)" 으로 갱신해 주세요.

## 2026-11-16 · JOB 1 — Markdown → HTML 변환기 (P17-03, P17-04 변환기 부분)

현재 작업 번호: P17-03 (변환기와 시험 완료), P17-04 (변환기 쪽 계약 완료, 컴포넌트 연결은 조정자 몫)
다음 작업 번호: JOB 2 (P11-09 다운로드 팝업)

### 완료한 내용

- 기준 문법: **CommonMark 0.31.2** (https://spec.commonmark.org/0.31.2/, spec.json 조회일 2026-11-16, last-modified 2025-06-01)
  + **GFM 0.29** 확장(표, 취소선, 작업 목록, 확장 자동 링크, 태그 필터; https://github.github.com/gfm/ 의 소스 `cmark-gfm/test/spec.txt`, 조회일 2026-11-16).
- 자체 블록 파서(규격 부록 A 의 줄 단위 전략)와 인라인 파서(구분자 스택/대괄호 스택 알고리즘), 반복형(비재귀) 렌더러. 정규식을 쓰지 않는 직접 작성한 인식기
  (Java 정규식은 반복 그룹에서 재귀하여 긴 입력에서 스택이 넘칠 수 있다).
- 공개 API: `MarkdownConverter.toHtml(md)`, `toHtml(md, options)`, `toHtmlDocument(md, title)`, `toHtmlDocument(md, title, options)`;
  `MarkdownOptions`(`allowRawHtml`=false, `gfm`=true, `neutralizeUnsafeUrls`=true, 최대 입력 길이, 최대 블록/인라인 중첩 깊이), `MarkdownLimitException`.
- 변환은 입력을 바꾸지 않는다(문자열 불변). 컴포넌트는 `getContent()` 에 원래 Markdown 을 그대로 두고 이 클래스는 표시용 HTML 을 만드는 데에만 쓴다.
- 이름 있는 문자 참조 표는 HTML 작업자의 `org.duckdns.hjow.swingwebpanel.html.NamedCharacterReferences`(entitygen 이 생성, 2231개)를 `MdText.decodeEntity` 한 곳에서 호출한다.

### 정책(문서화한 결정)

- **원시 HTML**: 기본은 이스케이프하여 텍스트로 보여 준다. 인라인은 `&lt;b&gt;` 처럼, HTML 블록은 `<pre class="md-raw-html">` 안에 이스케이프하여 보여 준다
  (HTML 블록으로 인식되는 줄의 `*강조*` 같은 인라인 문법이 적용되지 않는 CommonMark 의 구문 구조는 그대로 따른다).
  `allowRawHtml=true` 이면 그대로 내보내되 GFM 이 켜져 있으면 태그 필터(`title textarea style xmp iframe noembed noframes script plaintext`)가 여는 `<` 를 `&lt;` 로 바꾼다.
- **위험한 URL 스킴**: `javascript:`, `vbscript:`, `data:`, `file:` 목적지의 링크는 `href` 를 생략하고 이미지는 `src=""` 로 둔다(엔티티/대소문자로 위장해도 정규화 뒤에 검사).
  `neutralizeUnsafeUrls=false` 로 끌 수 있다. 자동 링크 `<javascript:...>` 도 같다.
- **제한**: 입력 길이(기본 8 Mi 문자) 초과는 `MarkdownLimitException`. 컨테이너 블록 중첩(기본 100)을 넘는 `>`/목록 표식은 텍스트로 남는다.
  인라인 중첩(기본 100)을 넘으면 강조/링크가 되지 않고 글자 그대로 남는다. 참조 링크 정의로 내어 주는 목적지/제목 크기의 합계는 max(1,000,000자, 입력의 4배)로 제한하여
  긴 정의를 반복 사용해도 출력이 부풀지 않는다. 괄호가 있는 링크 목적지의 괄호 중첩은 32단계로 제한한다(cmark 와 같은 방식, 규격과의 의도된 차이).
- 닫는 탐색이 끝나지 않는 구성 요소(백틱 열, `<!--`, `<?`, `<![CDATA[`, `<!X`)는 실패한 시작 위치를 캐시하여 선형 시간을 유지한다.

### 규격과의 차이/한계 (정직한 기록)

- 유니코드 구두점/기호 판정은 JRE 6 의 `Character.getType`(유니코드 4.0)에 의존한다. 이후 버전에서 추가된 기호는 구두점으로 인식되지 않을 수 있다.
  대문자 ß(U+1E9E)는 JRE 6 표에 없어 참조 라벨 정규화에서 직접 `SS` 로 접는다.
- GFM 취소선은 규격 0.29 에 따라 물결표 **두 개**만 인식한다(github.com 은 하나도 허용하지만 따르지 않음).
- GFM 작업 목록의 규격 예제 2개는 `disabled` 표시(속성 순서가 달라 규격 자체가 시험에서 제외)이므로 직접 쓴 시험(`testTaskListItems`)으로 확인한다.
  출력은 `<input type="checkbox" disabled="" />` 형식(cmark-gfm 과 같은 속성 순서).
- GFM 확장 자동 링크의 일부 세부는 cmark-gfm 과 다르다: 끝의 `;` 는 엔티티처럼 보일 때만 제거(규격 문장대로), 이메일의 마지막 문자는 영숫자 허용(규격 문장대로), 따옴표 `'` `"` 는 끝에서 제거하지 않는다.
- `toHtmlDocument` 의 `<style>` 은 이 프로젝트의 CSS 엔진이 구현할 기본 속성만 쓴다.
- 줄 번호/소스 위치 정보는 만들지 않는다(필요하지 않음).

### 변경 파일

`src/org/duckdns/hjow/swingwebpanel/content/markdown/`: `package-info.java`, `MarkdownConverter.java`, `MarkdownOptions.java`, `MarkdownLimitException.java`,
`Node.java`, `NodeType.java`, `MdText.java`, `LinkRefs.java`, `HtmlTags.java`, `BlockParser.java`, `InlineParser.java`, `TableSupport.java`, `GfmAutolink.java`, `HtmlRenderer.java`;
`tests/src/org/duckdns/hjow/swingwebpanel/content/markdown/`: `CommonMarkSpecTest.java`, `MarkdownConverterTest.java`;
`tests/resources/org/duckdns/hjow/swingwebpanel/markdown/`: `commonmark-0.31.2-spec.json`, `gfm-0.29-spec.txt`, `README.md`.

### 검증 결과

- JDK 6u45: `scripts/pkgtest.sh cdl "content/markdown" "content/markdown"` → **25 passed / 0 failed / 0 skipped** (기반 시설 7건 포함, Markdown 시험 18건).
  - CommonMark 0.31.2 예제 **652/652 통과**(원시 HTML 통과, GFM 끔), GFM 확장 예제 **22/22 통과**(표 8, 취소선 2, 자동 링크 11, 태그 필터 1; `disabled` 2건 제외).
  - 보안/제한: 원시 HTML 이스케이프, 태그 필터, 위험 스킴 무력화(대소문자·엔티티·참조 정의·자동 링크·이미지), 속성/제목 탈출 방지, 옵션 기본값/검증, 입력 길이 제한,
    줄 끝 표기 3종·NUL, 한글/보충 문자/URL 인코딩, 블록 만 단계/목록 오천 단계/들여쓴 목록 삼천 단계 중첩, 강조 3만 쌍 중첩, 대괄호 5만 쌍, 이미지 참조 2만 겹,
    병적 입력 28종(각 10만 자)과 백틱 열 399종, 참조 확장 폭주, 8스레드 동시 변환. 가장 느린 시험이 약 1.5초(부하가 없을 때). 시험의 시간 제한은 30초.
- `tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/content/markdown tests/src/org/duckdns/hjow/swingwebpanel/content/markdown tests/resources/org/duckdns/hjow/swingwebpanel/markdown` → 0 위반.

### 미해결 사항

- `text/markdown` 콘텐츠 처리기(`content` 패키지의 MIME 분기)와 컴포넌트 연결: `getContent()` 는 원래 Markdown 을 반환하고 화면은 `toHtmlDocument` 결과를 일반 HTML 경로로 보여야 한다(P17-01/P17-04 의 연결 부분, 조정자).
- Markdown 안의 상대 링크/이미지는 문서의 기준 URL 로 해석된다(변환기는 건드리지 않음).

### 체크박스 후보

- **P17-03** (자체 블록/인라인 파서로 제목, 문단, 목록, 인용, 코드, 강조, 링크, 이미지, 이스케이프 구현, 코드 속 HTML 실행 없음, 원문 HTML 정책 명시) — 구현과 규격 예제 전수 검증 완료.
- P17-04 는 변환기 쪽(원문을 바꾸지 않음)만 끝났고 컴포넌트가 원문 반환/이력 복원을 하는 연결은 남아 있어 후보가 아니다.

### 조정자 요청

- 컴포넌트에서 MIME `text/markdown` 일 때: `String html = MarkdownConverter.toHtmlDocument(content, null)` 을 DOM/렌더링 경로에 넣고 `PageSnapshot.content` 는 원래 Markdown 으로 둔다.
  `MarkdownLimitException`/`IllegalArgumentException` 은 오류 페이지로 연결한다.
- FEATURE_MATRIX 의 text/markdown 행 상태를 "부분 구현(변환기 검증 완료, 컴포넌트 연결 전)"으로 갱신해 주세요.
