# NetstormJava 게임 페이지 구동 점검 (2026-11-24, 계획 P28)

사용자가 이 프로젝트로 웹 게임을 띄우려고 만든 별도 프로젝트에서, 구동하자마자 스크립트 오류가 났다.
이 문서는 그 원인과 SwingWebPanel 에 더 구현해야 할 사항을 정리한 점검 기록이다. 계획은 `AGENTS.md` 의 P28 이고,
기능 표의 추적 행은 `docs/FEATURE_MATRIX.md` 7.5절(`ESX-*`)이다.
사용자 지시("더 깊이 확인하지는 말 것")에 따라 첫 오류를 일으킨 언어 기능과, 짧은 판정으로 확인할 수 있는 내장 객체·웹 API 까지만 확인했다.
첫 오류 뒤의 실제 실행(그리기, 소리, 입력, 성능)은 확인하지 않았다(6절).

## 1. 대상과 재현

| 항목 | 내용 |
| --- | --- |
| 샘플 프로젝트 | `H:\Workspace\eclipse photon\NetstormJava` (Maven, `source/target 1.7`, 의존성 `swingwebpanel`·`swingwebpanel-js` 0.1.0) |
| 진입점 | `org.duckdns.hjow.netstorm.Netstorm`: `JFrame` 안의 `SwingWebPanel` 에 콘솔 리스너를 달고 `setPage("classpath://www/index.html")` |
| 페이지 | `www/index.html` 의 작은 인라인 로더가 `dist/netstorm.bundle.js` 를 `<script>` 로 넣는다(실패 시 세 번 재시도). `www/debug.html` 은 번들 없이 `<script type=module>` 로 `netstorm.js` 를 직접 불러온다 |
| 번들 | 원본 소스 `D:\Workspace\git\NetstormReborn\webpj\www`(ES 모듈 1,057개)를 webpack 5.113.0 과 terser-webpack-plugin 5.6.1 로 묶은 한 줄짜리 파일, 3,271,267 자(UTF-16 단위), 현재 파일 크기 3,445,839바이트. `webpack.config.js` 는 `target: 'web'` 이고 Babel 같은 하위 판본 변환이 없어 소스의 ES2022 문법이 그대로 들어 있다 |
| 재현 환경 | 이 저장소의 `browser/target/swingwebpanel.jar`(2026-11-24 빌드), 샘플의 `src/main/resources` 를 클래스패스에 두고 같은 주소를 연 헤드리스 실행. JDK 7u80(샘플의 실행 조건)과 JDK 6u45 |
| 결과(두 JDK 같음) | 콘솔 리스너로 `[ERROR] classpath://www/dist/netstorm.bundle.js:1:782 (UNCAUGHT_EXCEPTION) > Uncaught Invalid or unexpected token`. 페이지 자체는 확정(`Done`, 진행률 100)되지만 게임 코드는 한 줄도 실행되지 않는다 |
| 오류 위치 | 1행 782열은 `class AssetLoader { ... async#e(e){for(let t=0;;...` 의 `#e`, 즉 ES2022 클래스 비공개(private) 메서드이다. 엔진의 렉서가 `#` 를 토큰으로 읽지 못한다 |

클래스패스 주소 자체(상대 경로 `dist/netstorm.bundle.js`, `data/...` 와 구분자 `/`)는 정상으로 해석되었다. 번들은 정상적으로 받아졌고(2.의 집계는 같은 파일), 아래 4.의 판정 페이지에서도 `fetch('data.bin')`, `fetch('/www/data/...')` 같은 상대·루트 기준 주소가 클래스패스에서 읽혔다.

## 2. 번들이 쓰는 ES2015 이후 문법

번들과 원본 소스를 ECMAScript 최신 판 파서(원본 프로젝트의 `node_modules` 에 있는 acorn 8.19.0, `ecmaVersion: 'latest'`)로 파싱해 문법 노드를 센 결과이다.
(분석 스크립트는 저장소 밖 세션 임시 디렉터리에서 실행했고 저장소에 넣지 않았다.)

| 판본 | 문법 | 번들의 사용 수 |
| --- | --- | --- |
| ES2016 | `**` 연산자 | 199 |
| ES2017 | async 함수 / `await` | 26 / 34 |
| ES2018 | 객체 spread `{...x}` | 513 |
| ES2019 | optional catch binding `catch {` | 8 |
| ES2020 | optional chaining `?.` | 1,985 |
| ES2020 | nullish `??` | 714 |
| ES2020 | BigInt 리터럴 `10n` | 129 |
| ES2021 | 논리 대입 `??=` / `\|\|=` | 101 / 1 |
| ES2022 | 클래스 필드(private `#x` / public) | 3,797 / 2,053 |
| ES2022 | static 필드 / static 블록 | 2 / 5 |
| ES2022 | private 메서드·접근자 `#m()` | 1,570 |
| ES2022 | private 이름 사용(모든 위치) | 30,294 |

원본 소스에는 이 밖에 숫자 구분자 `1_000`(2곳)과 정규식 `d` 플래그(9곳)가 있으나 번들에는 나타나지 않았다.
async 제너레이터, `for await`, 객체 rest, 동적 `import()`, `import.meta`, 최상위 `await` 은 번들에 없다.
번들의 자유 전역 이름 중 ES2015 이후 것은 `BigInt`(307), `globalThis`(16), `WeakRef`(7) 이고, ES2016 이후 내장 메서드 이름으로는
`at`(44), `asIntN`/`asUintN`(83/44), `getBigUint64`/`setBigUint64`(14/14), `deref`(24), `hasOwn`(9), `fromEntries`(4), `replaceAll`(3), `flatMap`(1),
`description`(36), `groups`(2) 가 쓰인다(`entries`/`values` 같은 이름은 Map 등의 ES2015 메서드와 구분하지 않았다).
WebAssembly, Worker, WebSocket, IndexedDB 는 원본 소스에서 찾지 못했다. 음악 파일 `.mus` 는 일반 WAV(PCM 16비트 스테레오 22,050 Hz)이다.

## 3. 엔진 판정 (`js.embed.JsEngine`, 엔진 단독, JDK 7u80)

기능마다 짧은 코드를 새 엔진에서 `eval` 하고 `runJobs()` 한 결과이다. 49개 중 14개 통과.

| 결과 | 기능 |
| --- | --- |
| 구문 오류 | `**`/`**=`(ES2016), async 함수·화살표·메서드(ES2017), 객체 spread/rest, async 제너레이터/`for await`(ES2018), optional catch binding(ES2019), BigInt 리터럴, `?.`, `??`, 동적 `import()`, `import.meta`(ES2020), 논리 대입, 숫자 구분자(ES2021), 클래스 public/static 필드, private 필드·메서드·접근자, `#x in obj`, static 블록(ES2022), 정규식 후방 탐색 `(?<=`, `\p{L}`, `d` 플래그 |
| 없음(TypeError/ReferenceError 또는 `typeof` 가 `"undefined"`) | `Object.entries/values/fromEntries/hasOwn`, `Array.prototype.flat/flatMap/at/findLast/toSorted`, `String.prototype.replaceAll/matchAll`, `Promise.prototype.finally`, `Promise.allSettled/any`, `BigInt`, `BigInt64Array`, `WeakRef`, `FinalizationRegistry`, Error `cause`, `Symbol.prototype.description` |
| 결과가 규격과 다름 | 정규식 이름 있는 그룹: `/(?<y>\d+)/` 는 받지만 `exec()` 결과에 `groups` 가 없다(`RegExpBuiltins` 의 주석대로 `$<name>` 치환도 펼치지 않는다) |
| 통과 | `Array.prototype.includes`, `padStart/padEnd`, `trimStart/trimEnd`, 정규식 `s` 플래그, `globalThis` |

기능 표의 ES-EXPR 행은 `**`(ES2016 확장)를 검증 완료로 적고 있었으나 실제로는 구문 오류이다(`js.parser.Lexer` 의 클래스 주석도 "`**` 와 ES2016 이후 문법은 인식하지 않는다"고 한다). 이번 갱신에서 그 행을 고치고 7.5절로 옮겼다.

## 4. 실제 컴포넌트 페이지 판정 (`SwingWebPanel`, `classpath://` 페이지, JDK 7u80)

ES5 문법만 쓴 판정 페이지를 클래스패스에 두고 열어 콘솔 리스너로 결과를 받았다. 브라우저 realm 에도 3.의 "없음" 내장 객체는 모두 없었고(같은 결과), 게임이 쓰는 웹 API 는 다음과 같았다.

| 결과 | 웹 API |
| --- | --- |
| 동작 | `requestAnimationFrame/cancelAnimationFrame`, `ResizeObserver`, `matchMedia(...).addEventListener`, `devicePixelRatio`, `document.defaultView`, `document.baseURI`, `AbortController`, `PointerEvent`, `WheelEvent`, `ImageData`(두 생성자 형식), 캔버스 `getContext('2d', {alpha:false})`·`{willReadFrequently:true}`·`imageSmoothingEnabled`·`putImageData/getImageData`·`toDataURL('image/png')`·`setPointerCapture`, `style.cursor = 'url(data:...) x y, auto'`, `FontFace` 와 `document.fonts`(`add/delete/ready`), WOFF2 바이트로 만든 `FontFace.load()`(게임의 갈무리11 글꼴, `status=loaded`), `AudioContext({sampleRate: 22050})`, `decodeAudioData`(게임의 WAV), `structuredClone`, `navigator.languages/webdriver`, `TextDecoder('utf-8', {fatal:true})`, `TextEncoder`, `localStorage`, `performance.now`, `URLSearchParams`, 클래스패스 기준 `new URL(..., document.baseURI)`, `fetch().arrayBuffer()` |
| 없음 | `addEventListener` 옵션의 `signal`(중단해도 리스너가 지워지지 않음; 게임은 모든 입력·크기 리스너를 이 방식으로 등록하고 정리한다), `ReadableStream` 과 `fetch` 응답 `body.getReader()`(게임은 없으면 `arrayBuffer()` 로 대체하고 적재 진행률만 덜 세밀해진다), `navigator.clipboard`(채팅 글 복사·붙여넣기), `Blob`/`FileReader`/`URL.createObjectURL` 중 적어도 하나(개별 판정 안 함; 게임은 화면 저장 내려받기에만 쓰고 없으면 포기한다) |

## 5. 판단: 구현해야 할 사항 (계획 P28)

1. **언어 문법과 실행 의미(필수)**: 2.의 ES2016~ES2022 문법이 없으면 번들이 파싱되지 않으므로 게임은 시작조차 못 한다. 빠진 문법 중 하나라도 남으면 같은 증상이 반복된다(P28-02, P28-03).
2. **BigInt(필수)**: 리터럴·`BigInt()`·`asIntN/asUintN`·`DataView` 의 BigInt 접근이 게임 코드 곳곳에 있다(P28-04). 아래 7.처럼 게임 쪽에서 트랜스파일해도 BigInt 는 없앨 수 없다.
3. **내장 객체(필수)**: `at`, `hasOwn`, `fromEntries`, `replaceAll`, `flatMap`, `WeakRef`, Symbol `description`, 정규식 `groups` 등(P28-05).
4. **웹 API(일부 필수)**: `addEventListener` 의 `signal` 은 정리 동작에 필요하다. `ReadableStream`, `navigator.clipboard`, `URL.createObjectURL` 은 게임에 대체 경로가 있어 구동을 막지는 않는다. `blob:` 주소는 상단 요구사항의 허용 스키마와 충돌하므로 사용자 결정이 필요하다(P28-06).
5. **성능과 실행 제한(위험)**: 327만 자 번들을 AST 인터프리터로 파싱·실행하고 `requestAnimationFrame` 으로 매 프레임 그리므로, 문법을 지원한 뒤 파싱 시간·메모리·프레임 시간과 `ExecutionLimits` 의 한도를 측정해야 한다(P28-07).
6. **실제 구동 확인**: 문법 오류를 넘은 뒤 드러나는 실패를 계층별로 나누어 하위 항목으로 추가한다(P28-08).

## 6. 확인하지 않은 것

- 첫 구문 오류 뒤의 실제 실행: 캔버스 그리기 결과와 속도, 메뉴 음악·효과음 재생, 포인터·키보드 입력, 한국어 글꼴 래스터화, 게임 논리.
- `debug.html`(ES 모듈 직접 적재) 경로. 같은 ES2022 문법을 쓰므로 같은 오류가 날 것으로 보이나 실행하지 않았다.
- 4.의 `Blob`/`FileReader`/`URL.createObjectURL` 중 무엇이 없는지.
- JDK 6u45 에서는 1.의 첫 오류만 확인했다(3.과 4.의 판정은 JDK 7u80).

## 7. 참고: 게임 쪽 우회책과 한계

게임 프로젝트(webpj)의 webpack 설정에 Babel 등으로 ES2015 대상 변환을 넣으면 async/await, 클래스 필드·private 이름, `?.`, `??`, 객체 spread 같은 문법 대부분은 변환 코드(도우미 함수와 WeakMap)로 바뀌어 지금 엔진에서도 파싱될 수 있다.
그러나 BigInt 리터럴과 BigInt 연산은 변환할 수 없고, `WeakRef`·`Object.hasOwn`·`Array.prototype.at` 같은 내장 객체는 폴리필(core-js 등)을 따로 넣어야 한다.
또한 이 프로젝트의 목표는 실제 웹 페이지를 그대로 띄우는 것이므로, 계획 P28 은 엔진 쪽 구현을 기준으로 한다.

## 8. P28-01 판본과 대상 목록 확정 (2026-11-25)

P28의 기준 판본을 **ECMA-262 제13판, ECMAScript 2022 (June 2022)**로 고정했다.
공식 URL은 `https://262.ecma-international.org/13.0/` 이고 ECMA 공식 PDF는
`https://ecma-international.org/wp-content/uploads/ECMA-262_13th_edition_june_2022.pdf`이다.
`tools/webfetch`를 JRE 25.0.5.0.2로 실행한 조회에서는 두 공식 URL 모두 HTTP 403으로 거부되어
본문을 받지 못했다(2026-11-25). User-Agent 위장이나 인증서 검증 해제는 하지 않았다.

본문 대조는 사용자가 저장소에 둔 완전한 오프라인 HTML 사본
[`docs/es2022`](../es2022/)을 기준으로 했다. 사본의 링크는 `https://tc39.es/ecma262/2022/` 을 가리킨다.
TC39 공식 저장소의 같은 판 스냅샷
`https://raw.githubusercontent.com/tc39/ecma262/es2022/spec.html`을 보조 출처로 사용했다.
같은 도구·JRE에서 HTTP 200, 2,657,094바이트를 확인했고 ETag는
`ffa4eed67e5c1aeee0f8bd4740f43a1ade7a8f0345195ac3b267ac110f7ef11a`이다.
오프라인 사본과 스냅샷의 서문(HTML 95, 104~110행)은 이 판본이 "thirteenth edition"이며 ES2016~ES2022의
주요 추가 기능을 연도별로 정리한 본문임을 확인한다.
최종 판정의 `ESX-*` 목록은 `docs/FEATURE_MATRIX.md` 7.5절에 판본별 문법·실행 의미·내장 객체로 나누었고,
각 행에 NetstormJava 번들의 사용 여부 또는 관찰된 횟수를 기록했다.

P28-01에서 확인해 정정한 사실은 다음과 같다.

1. 기능 표의 ES-EXPR에서 `**`를 ES2016 기능으로 옮긴 기존 정정이 맞다. 엔진에서 실제 구문 오류이다.
2. 이름 있는 정규식 캡처는 구문만 받고, `exec()` 결과의 `groups`와 치환의 `$<name>`은 구현되어 있지 않다.
3. `Array`/`String`/`%TypedArray%`의 `at`은 ES2022 기능이며 번들에서 42회 사용된다. Netstorm 판정기에서 봤던 `findLast`와 `toSorted`는 ES2023 이후이므로 P28의 ES2022 완료 판정에는 포함하지 않는다.
4. ES2023 이후 기능은 `ESX-OUT-2023+`로 별도 기록했다.
