# MEDIA-PLAY 진행 기록 (P18-08 오디오 재생, P18-05-c source 선택, P18-09 video DOM)

소유 경로: `src/org/duckdns/hjow/swingwebpanel/media/playback/**`, `src/org/duckdns/hjow/swingwebpanel/js/host/{MediaElements,MediaBindings,MediaRequestSource,MediaErrorValue,TimeRangesValue}.java`(신규),
`js/host/{ElementExtras,HostEnvironment,PageScripts}.java`·`core/{PageRuntime,PagePresentation,PageResources}.java`·`SwingWebPanel.java`(미디어 관련 최소 추가),
`tests/src/org/duckdns/hjow/swingwebpanel/media/playback/**`, `tests/.../js/host/{MediaElementsTest,PageFixture(미디어 필드 추가)}.java`, `tests/.../core/AudioIntegrationTest.java`, 이 문서.
소유 목록 밖에서 수정한 것: `core/PageView.java`(소리 출력 팩터리 보관 필드와 접근자 2개), `js/dom/DomInterfaces.java`(`HTMLMediaElement` 상수와 `defaultMuted` 반영 한 줄 —
이 hunk 는 FORMS 작업자의 커밋 `6d7137c` 에 함께 들어갔다). 둘 다 추가 위주의 작은 변경이다.
시험 실행(격리): `sh scripts/snaptest.sh <이름> "<소유 파일을 정확히 나열>" "js dom html css net storage core content layout render download media @root" "js core media render layout" --filter org.duckdns.hjow.swingwebpanel.`
(엔진만 반복할 때는 `scripts/pkgtest.sh <이름> "media" "media/playback media/audio/testkit" --filter org.duckdns.hjow.swingwebpanel.media.playback`). 모든 시험은 JDK 6u45 로만 했다.
린트: `tools/sourcecheck/run.sh <변경 경로들>` 0 위반.
참고 규격(조회 도구 `tools/webfetch`, 새 JRE = Zulu 25, 조회 2026-11-17):
- HTML5 W3C Recommendation 2014-10-28, 4.7.10 Media elements — `https://www.w3.org/TR/2014/REC-html5-20141028/embedded-content-0.html` (Last-Modified 2014-10-24 16:15:24 GMT, 본문 366,948 바이트, `build/html5-embedded-20141028.txt` 에 저장(build 는 커밋 대상 아님)).
  따른 절: 4.7.10.1 오류 코드, 4.7.10.3 MIME 타입(`canPlayType`), 4.7.10.4~5 네트워크 상태/로드 알고리즘/리소스 선택·가져오기, 4.7.10.6 오프셋(`currentTime`/`duration`/`loop`),
  4.7.10.7 준비 상태와 이벤트 순서, 4.7.10.8 재생(`play`/`pause`/`ended`/rate), 4.7.10.9 탐색, 4.7.10.14 TimeRanges, 4.7.10.16 이벤트 요약.
- 규격의 `play()` 는 `void` 이지만 이 구현은 이후 HTML 표준처럼 Promise 를 반환한다(요구).

## MEDIA-E 작업 기록 (2026-11-19, P18-08 남은 결함; 사용자 지시로 세션을 마무리하며 중단)

작업 위치: 격리 worktree 브랜치 `worktree-agent-a3f9075cc7e969e4f`(master `2da5a26` 에서 갈라짐). 커밋 `da69f0b`(301), `ebd2833`(302), 이 문서(303).

### 항목별 상태

| 항목 | 상태 | 근거 |
| --- | --- | --- |
| (조정자 추가) `play()` 프라미스가 `playing` 때 이행되지 않음 | **고침, 자동 검증** | `MediaElementsTest.testPlayPromiseFollowsPlayingNotTheDevice`: 고치기 전 소스(HEAD `2da5a26` 의 `MediaElements.java`)에서 실패("the promise is fulfilled although the device took nothing", 이벤트는 `a:play,a:playing`), 고친 뒤 통과 |
| 4. 재생 실패 뒤의 상태 | **고침, 자동 검증** | `MediaElementsTest.testPagesWithoutServices`(갱신), `AudioIntegrationTest.testNoSoundDevice`(갱신) |
| 1. 긴 오디오를 메모리에 통째로 두지 않기 | **기반만 완료(검증), 연결은 미완** | `ByteSourceTest` 4건. 디코더·재생기·페이지 요청은 아직 `byte[]` 와 16 MiB 메모리 한도 그대로 — 아래 "남은 일 1" |
| 2. JavaScript 를 꺼도 미디어 요소 동작 | **미착수** | 아래 "남은 일 2" |
| 3. `controls` 막대의 키보드·포커스·호버 | **미착수** | 아래 "남은 일 3" |
| 5. 기타 쉬운 미해결 | 없음 | |

### 고친 것 1: `play()` 프라미스 (조정자가 실제 창에서 찾은 결함)

- 원인: 프라미스를 재생기의 `PlaybackListener.started()`(출력 라인이 첫 데이터를 받아들인 때)에서 이행했다. RDP 세션의 Java Sound 처럼 `start()` 뒤 몇 초 동안 데이터를 받지 않는 라인에서는 `playing` 이벤트는 곧바로 오는데 프라미스는 몇 초 뒤에 이행되고, 그 사이 `pause()` 하면 `AbortError` 로 거부되었다.
- 고침(`js/host/MediaElements.java`): WHATWG HTML 의 "notify about playing the media element" 대로 `playing` 을 올릴 때 대기 중인 프라미스를 **가져와(take pending play promises)** 그 작업에 싣고, 작업이 `playing` 을 낸 직후 그 프라미스들을 이행한다(`notifyPlaying`, `Task.promises`). 장치 지연과 무관하다.
  - 이미 재생 중(`paused=false`)이고 `readyState >= HAVE_FUTURE_DATA` 일 때 부른 `play()` 는 대기 목록을 가져와 다음 작업(`K_RESOLVE`)에서 이행(전에는 소리 시작 전이면 계속 기다렸다).
  - 새 로드는 아직 실행되지 않은 작업에 실린 프라미스를 작업 순서대로 **곧바로 이행**하고(규격: "For each task in pending tasks that would resolve pending play promises ... immediately resolve"), 대기 목록에 남은 것은 `AbortError` 로 거부(기존).
  - 끝에 닿아 일시정지될 때 대기 중인 프라미스를 `AbortError` 로 거부(규격 "reaches the end" 단계; 전에는 하지 않았다).
  - `started()` 보고(`K_STARTED`)는 이제 기록만 하며 프라미스와 무관하다.
- 시험 장치: `FakeOutputs.holdWrites`(테스트 전용) 가 켜져 있으면 모든 가짜 출력이 `write` 에 0 을 돌려준다(첫 데이터를 받지 않는 느린 라인 흉내).
- 조회한 규격: WHATWG HTML `https://html.spec.whatwg.org/multipage/media.html`(tools/webfetch, Zulu 25, 조회 2026-11-19, Last-Modified `Thu, 19 Nov 2026 13:09:59 GMT`, ETag `"6afef527-ccc2c"`, 본문 247,210 바이트): "To notify about playing ...: Take pending play promises ... Queue a media element task ...: Fire an event named playing ... Resolve pending play promises", `play()` 의 "Otherwise, if ... HAVE_FUTURE_DATA or HAVE_ENOUGH_DATA, take pending play promises and queue a media element task ... to resolve", 로드 알고리즘의 "For each task in pending tasks that would resolve pending play promises ... immediately resolve or reject", 끝에 닿을 때 "Take pending play promises and reject ... AbortError".
- **실제 창(RDP + 실제 장치)에서 다시 확인하지 않았다**(가짜 출력으로만 재현·검증). 조정자가 `tools/uicheck` 로 다시 확인할 것.

### 고친 것 2: 재생 실패 뒤의 상태 (항목 4)

- 규격(두 판본 모두 조회해 대조, 2026-11-19): HTML5 W3C 권고안 2014-10-28 4.7.10.5 "If the media data is corrupted"(`https://www.w3.org/TR/2014/REC-html5-20141028/embedded-content-0.html`, Last-Modified 2014-10-24 16:15:24 GMT, 366,948 바이트)와 위 WHATWG 판의 같은 단계: 가져오기 취소, `error` = `MEDIA_ERR_DECODE`, `networkState` = `NETWORK_IDLE`, load 지연 끝, `error` 이벤트, 전체 리소스 선택 중단. **`paused` 를 바꾸지 않고 `pause` 이벤트도, 프라미스 거부도 없다.**
- 고침(`MediaElements.playbackFailed`): 위 단계만 한다. 전에는 요소를 다시 `paused` 로 만들고 `pause` 이벤트를 내고 프라미스를 `NotSupportedError`/`EncodingError` 로 거부했으며 `play()` 가 재생기에 다시 재생을 시켰다(규격과 다름). 이제 리소스는 죽은 것으로 보고 새 로드 전까지 재생기에 재생을 시키지 않는다(`startPlayback` 이 `error` 를 확인). 재생기는 위치·탐색 질의에만 남고, 요소는 `playing` 집합에서 빠져 참조가 없으면 수거될 수 있다. 같은 리소스의 두 번째 실패 보고는 무시한다.
- 소리 장치가 없을 때(출력 열기 실패, `ACCESS_FAILED`)도 같은 경로이다(아래 한계 1).

### 기반: 긴 오디오용 바이트 원천과 임시 파일 (항목 1의 일부)

새 클래스(`media/audio`, 모두 Java 6, 영어·한국어 Javadoc):
- `ByteSource`(길이, `get(pos)` 은 범위 밖이면 배열처럼 `IndexOutOfBoundsException`, `read(pos, buf, off, len)` 은 끝에서만 짧게 읽음, `close()`), `ByteSourceException`(입력 오류, `toMediaException()` 은 `ACCESS_FAILED`), `ArrayByteSource`, `FileByteSource`(`RandomAccessFile` 과 기본 64 KiB 창 하나; 창 크기 배수에 맞춘 창, 창보다 큰 읽기는 파일에서 바로; 모든 메서드 동기화).
- `MediaSpool`: 응답 본문을 받는 `OutputStream`. 1 MiB(`MEMORY_LIMIT`)까지는 메모리, 넘으면 `File.createTempFile("swingwebpanel-media-", ".part")` 로 옮겨 이어 쓴다. **디스크 한도 `MAX_BYTES` = `Integer.MAX_VALUE`(2 GiB − 1 바이트)**: 컨테이너 해석기의 모든 위치가 `int` 에 들어가도록. 넘으면 `IOException` 으로 요청 중단. `finish()` → `MediaData`, `discard()` 는 덜 받은 파일을 지운다.
- `MediaData`: 메모리 바이트나 임시 파일 하나를 소유. `open()` 이 원천을 주고, `release()` 는 아직 열린 원천을 모두 닫은 뒤(Windows 는 열린 파일을 지울 수 없다) 파일을 지운다. JVM 전역 상태(`deleteOnExit`)는 쓰지 않는다.
- `Bytes` 에 `ByteSource` 판 도우미(`has/matches/u8/le16/le32/le64/be16/be24/be32/ascii/copy/readFully`) 추가.
- 시험 `media/audio/spool/ByteSourceTest` 4건: 배열과 파일(창 7 바이트/4 KiB/64 KiB)이 무작위 위치·크기·역방향 읽기와 정수 도우미에서 같음, 범위 밖·닫힌 파일, 받는 쪽의 메모리/파일 문턱·조각 쓰기·한도 초과·버리기, 놓으면 열린 원천이 닫히고 파일이 지워짐.
- 이 클래스들은 **아직 어디에도 연결되지 않았다**(제품 동작은 바뀌지 않음).

### 검증 (JDK 6u45, 모두 직접 실행)

- `sh scripts/pkgtest.sh mediaold "media js" "js/host media/playback" --filter org.duckdns.hjow.swingwebpanel.js.host.MediaElementsTest.testPlayPromiseFollows`(작업 트리의 `MediaElements.java` 를 잠시 HEAD 판으로 바꿔 실행) → 0 통과 / 1 실패(위 메시지). 고친 판으로 되돌렸다.
- `sh scripts/pkgtest.sh mediae "media js dom html css layout render net storage core content download @root" "media/playback js/host core" --filter org.duckdns.hjow.swingwebpanel.core.Audio` → **12 통과 / 0 실패**(AudioIntegrationTest 6, AudioControlsIntegrationTest 1, AudioRobustnessIntegrationTest 5). 같은 빌드로 `--filter org.duckdns.hjow.swingwebpanel.js.host.MediaElementsTest` → **24 통과 / 0 실패**.
- `sh scripts/pkgtest.sh mediabs "media" "media/audio/spool" --filter org.duckdns.hjow.swingwebpanel.media.audio.spool` → **4 통과 / 0 실패**.
- `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/media/audio tests/src/org/duckdns/hjow/swingwebpanel/media/audio/spool src/org/duckdns/hjow/swingwebpanel/js/host/MediaElements.java` → 0 위반(89 파일). 커밋한 시험 파일들도 0 위반.
- 전체 회귀: 아래 "전체 회귀" 줄.

### 남은 일 (다음 작업자가 이어서 할 것)

1. **긴 오디오(항목 1)의 연결** — 조사한 설계(이번에 일부 시작했다가 끝낼 수 없어 되돌렸다):
   - `AudioDecoder` 를 `accepts(ByteSource)`/`open(ByteSource, DecodeLimits)` 로 바꾸고, 시험이 쓰는 `byte[]` 판은 공통 기반 클래스(예: `AbstractAudioDecoder` 가 `ArrayByteSource` 로 감쌈)에 두면 디코더 시험을 고치지 않아도 된다. `AudioDecoders.open(ByteSource, ...)` 은 `ByteSourceException` 을 `ACCESS_FAILED` 로, 기본 제한은 크기만 `MediaSpool.MAX_BYTES` 인 제한(정적 필드 이름에 `LIMITS` 를 쓰면 `StaticStateAuditTest` 가 잡는다)으로.
   - 디코더별 방법(바이트 단위 결과를 바꾸지 않는 것이 핵심; 기존 배열 경계 예외를 그대로 두려고 `get` 이 범위 밖에서 예외를 던지게 했다):
     - WAV/AIFF/AU: 헤더는 `Bytes` 의 `ByteSource` 판으로 그대로. `RawPcmSource.fill` 은 `count*inFrameBytes` 바이트를 블록 버퍼로 읽어 상대 위치로 변환, `AdpcmSource.decodeBlock` 은 블록(`blockAlign`)을 버퍼로 읽어 `decodeIma/Ms/ImaQt(0, len)`.
     - MP3: 프레임 색인은 `Mp3Header.parse` 의 `ByteSource` 판으로 만들고, 해독 때 프레임(`frameBytes`, 원천 끝에서 잘림)을 버퍼로 읽어 `decode(buf, 0, h, ...)`(해독기는 비트 저장소를 스스로 복사해 두므로 같다).
     - AAC: ADTS/MP4 색인은 그대로(위치만), `AacSource.fill` 은 블록을 재사용 버퍼로 읽어 `new AacBits(buf, 0, len)`(비트 정렬은 바이트 경계 기준이라 같다). `Mp4File` 의 `d[...]` 는 `ByteSource` 접근으로 기계적으로 바꾼다(박스 머리와 표만 읽고 `mdat` 은 건너뛴다).
     - Ogg(Vorbis/Opus/Ogg FLAC): `OggPage.parse`/`isChecksumValid` 의 `ByteSource` 판, `OggPacketStream.loadPage` 는 페이지 전체(최대 65,307 바이트)를 재사용 버퍼로 읽어 세그먼트와 페이로드를 그 버퍼에서.
     - FLAC: 프레임 길이를 미리 모르므로 창(예: STREAMINFO 최대 프레임 크기 또는 64 KiB)을 읽어 해독하고, `Truncated` 가 나는데 원천 끝이 아니면 창을 두 배로 늘려 다시 해독(해독은 프레임 바이트만의 결정적 함수이므로 결과가 같다). `findHeader`/`validPoint` 는 32 바이트 창이면 충분(헤더 최대 16 바이트).
     - WebM: `WebmFile` 은 기계적 변환. `VorbisInOgg/OpusInOgg.build` 가 메모리에 Ogg 전체를 만들므로 큰 파일은 임시 파일(파생 데이터, 반환한 PCM 소스가 닫힐 때 지움)이나 페이지를 필요할 때 만드는 가상 원천으로 바꿔야 한다.
   - 그 뒤: `MediaRequestSource.Callback.done(MediaData, mime, error)`(시험 가짜 `MediaElementsTest`, `TextTrackTest`, `AudioIntegrationTest` 도), `PageResources.requestMedia` 는 `ResponseHandler` 로 `MediaSpool` 에 받는다(스트림으로 받은 본문은 `HttpCache` 가 저장하지 않으므로 메모리에 다시 보관되지 않는다; 캐시가 이미 메모리에 가진 응답은 `applyHandler` 가 받는 쪽으로 써 준다). 페이지는 만든 `MediaData` 를 모두 기억했다가 취소 훅에서 놓고, 요소는 새 로드 때, 재생기는 `finish()` 에서 놓는다. `MediaPlayer.load(MediaData, mime)` 는 원천 하나를 열어 다시 열기(`rewindSource`)에도 쓰고 끝에서 닫는다.
   - 검증 계획: `ReferenceDecodeTest` 를 그대로 통과 + 같은 117 파일을 `FileByteSource`(작은 창)로 디코딩해 배열 결과와 바이트 단위 비교, 시험 중 만든 100 MB 넘는 WAV/MP3 를 `-Xmx64m` 으로 재생(시험 실행기 힙 옵션 확인 필요), 이동/`dispose()` 뒤 임시 파일 삭제 확인.
   - `buffered`/`progress`: 다운로드가 끝난 뒤 디코딩하는 설계에서는 메타데이터 전(`HAVE_NOTHING`)의 `buffered` 는 규격상 비고 끝난 뒤 전체이며, `progress` 는 이미 받은 바이트 수를 따른다(`HttpFetcher` 는 스트림 본문에도 진행을 알린다).
2. **JavaScript 를 꺼도 미디어(항목 2)**: 요소 상태 기계(`MediaElements`)가 페이지 스크립트 실행 환경(`PageScripts`) 안에 있어 스크립트가 꺼지면 만들어지지 않는다. 제안: 실행 환경은 만들되 스크립트 실행만 막는 모드(이벤트 디스패치는 하되 JS 리스너/핸들러 속성은 실행하지 않음; `setJavascriptEnabled(false)` 의 P15-10 계약 "이미 중단한 스크립트는 다시 실행하지 않음"과 충돌하지 않게 "미디어 전용 작업 큐"만 돌린다)를 `PageRuntime` 에 두거나, 상태 기계를 JS 와 무관한 core 쪽 객체로 옮기고 JS 바인딩을 그 위에 얹는다. 앞쪽이 변경이 작다.
3. **`controls` 막대의 키보드·포커스(항목 3)** 제안 키 표(Chromium 의 미디어 컨트롤과 비슷하게): Tab 으로 `controls` 가 있는 `audio` 에 포커스(포커스 고리), Space/Enter 재생·일시정지, ←/→ 5초 탐색, ↑/↓ 음량 0.1, `M` 음소거, Home/End 처음/끝. 입력 연결은 `core/PageInput`(키 이벤트가 DOM `keydown` 을 거쳐 취소되지 않았을 때)과 `render/MediaControls`(포커스·호버 상태 그리기).

### 숨기지 않는 한계 (이번 변경으로 생긴 것)

1. 소리 장치가 없는 컴퓨터는 규격상 오류가 아니다(브라우저는 소리 없이 시간만 흐르게 재생한다). 이 구현은 계속 `MEDIA_ERR_DECODE` 로 알린다. 이제는 요소가 일시정지되지 않고 `play()` 로 다시 시도할 수도 없다(새 로드 필요). 소리 없이 시계로 재생하는 출력(무음 출력)으로 바꾸는 것을 권한다.
2. 실패 뒤에도 `paused` 가 false 이므로 `controls` 막대는 일시정지 기호(재생 중 모양)를 그린다. 오류 상태를 막대에 따로 그리지 않는다.
3. 이미 이행된 `play()` 프라미스 뒤에 실패가 오면 스크립트는 `error` 이벤트로만 안다(규격과 같음).

### 조정자 요청

- **FEATURE_MATRIX AUD-10**: 제약에서 "재생 실패 뒤 `paused` 로 돌아가 재시도 가능(규격과 다름)" 과 "장치가 없으면 ... DOM `error`(code 3)" 를 "재생 실패는 HTML5 4.7.10.5 의 치명적 디코딩 오류(`MEDIA_ERR_DECODE`, 일시정지하지 않음, 새 로드 전 재시도 없음); 소리 장치가 없어도 같은 오류로 알림(규격상 오류가 아님, 한계)" 으로, "`play()` 프라미스는 `playing` 작업에서 이행(장치 지연과 무관)" 을 더하고, 검증 사례에 `MediaElementsTest.testPlayPromiseFollowsPlayingNotTheDevice` 를 넣을 것. "리소스는 통째로 받음(16 MiB 응답 한도)" 은 **그대로**(항목 1 연결 전).
- **ARCHITECTURE**: `media/audio` 에 `ByteSource`/`MediaSpool`/`MediaData`(긴 미디어의 임시 파일, 아직 연결 전)를 적을 것.
- **P18-08 체크 후보 아님**: 항목 1(긴 오디오 연결), 2(JS 꺼짐), 3(키보드)이 남았고 소리를 귀로 듣는 확인도 남았다.

### 전체 회귀

`BUILD_DIR=build/full sh test.sh > build/fullrun-e.txt 2>&1`(JDK 6u45, 커밋 `ebd2833` 의 작업 트리, 2026-11-19): **3684 통과 / 0 실패 / 12 건너뜀(774481 ms)**. 건너뜀 12건은 모두 선택적 Opus 원본 벡터(`SWP_OPUS_VECTORS`, `SWP_OPUS_VECTORS_8251` 없음). 제품 JAR 생성(`[OK] build/full/swingwebpanel.jar created (javac 1.6.0_45)`)과 실행기 종료 코드 자체 점검(`[OK] runner exit-code self-check passed`)도 통과. 시간 민감 시험의 간헐 실패는 없었다.

## 조정자 요청 (2026-11-17 2차 MEDIA 작업: P18-08 나머지, P18-10, P18-11, P18-01/02/03 점검)

- **체크박스 후보** (이 작업이 근거를 만든 것과 남은 것):
  - **P18-01**: 이미지·오디오 모두 시그니처 판별 + 실패 종류 구분(`MediaFailureKindsTest` 5건, `media-matrix.md` 4절) → **체크 후보**. 접근 실패는 네트워크 계층/소리 출력에서 따로 확인(`AudioRobustnessIntegrationTest`).
  - **P18-02**: JDK 6u45 의 `ImageIO.getReaderFormatNames()`, `AudioSystem.getAudioFileTypes()` 와 믹서를 실제 실행해 기록했고(`media-matrix.md` 1절), JRE 가 못 읽는 형식은 모두 자체 디코더로 구현됨 → **체크 후보**.
  - **P18-03**: AVIF 의 **오버레이(`iovl`)는 이 작업에서 구현·검증했다**(`AvifOverlayTest` 5건, 중첩만 `UNSUPPORTED_FORMAT`). AVIF 의 애니메이션(인터 프레임), 인트라 블록 복사, 초해상도, 필름 그레인, ICC/HDR 와 PNG/WebP 의 EXIF 방향, ICC 색 관리가
    **남아 있다**(`media-matrix.md` 5, 7절에 이유와 단계 계획) → **체크하지 말 것**. FEATURE_MATRIX IMG-09 의 제약에서 "오버레이" 를 빼고 `AvifOverlayTest` 를 검증 사례에 넣을 것. `AvifDecoderTest.testStructureErrors` 의 "오버레이는 UNSUPPORTED" 단언은 "설명이 맞지 않는 오버레이는 CORRUPT" 로 바꿨다.
  - **P18-08**: `playbackRate`(0.25~4배, `preservesPitch`, `ratechange`), `progress`/`stalled`, `preload=metadata`, `controls` 막대의 조작(마우스)이 구현·검증되었다. **소리를 귀로 확인하지 못했고**, 컨트롤 막대의 키보드/호버/포커스 표시와 JavaScript 를 끈 페이지에서의
    미디어 동작이 없다 → 사람이 확인하기 전에는 **체크하지 말 것**(P22-10 과 함께).
  - **P18-10**: 장치 없음(`MediaPlayerTest`, `JavaSoundDeviceTest`, `AudioIntegrationTest`), 디코더 실패(`MediaFailureKindsTest`), 손상/과대/잘린/거짓 헤더 파일, 접근 실패(404, 거절된 연결), 페이지 이동/clean/dispose 도중 재생, 여러 컴포넌트·여러 요소의 동시 재생
    (`AudioRobustnessIntegrationTest`, `AudioIntegrationTest`)이 자동 검증되었고 **디코딩/상태 검증과 실제 장치 청취를 분리**해 기록했다(아래 "미확인") → 청취 확인(사람)만 빼고 완료. 조정자 판단으로 **체크 후보**(P22-10 이 청취를 이어 받는다).
  - **P18-11**: 형식별 입력/기대/실패 사례 대응표 `media-matrix.md` → 비디오 예외를 뺀 대상 표에 미구현이 AVIF 변형 등으로 남으므로 **체크하지 말 것**.
- **FEATURE_MATRIX**: AUD-10 → "부분 구현" 유지, 구현 클래스에 `media/playback/RateConverter`, `render/{MediaControls,MediaControlState,MediaControlSource,MediaControlsPainter}`, 시험에
  `RateConverterTest` 9, `MediaPlayerTest` 20, `MediaElementsTest`, `AudioControlsIntegrationTest`, `AudioRobustnessIntegrationTest`, `JavaSoundDeviceTest`, `MediaControlsTest`, `MediaFailureKindsTest` 를 추가하고 제약을 아래 "미해결 사항(갱신)" 로 바꿀 것.
  WEB-09 도 같은 근거. IMG-*, AUD-* 의 "검증 사례" 열은 `media-matrix.md` 2, 3절의 시험 클래스·건수로 보강하고, IMG-09(AVIF)의 제약은 7절의 단계 계획으로 연결.
- 이 문서의 아래 "2026-11-17 2차" 기록이 변경 파일과 실행 결과이다.

## 2026-11-17 2차 MEDIA 작업 기록 (P18-08 나머지, P18-10, P18-11)

현재 작업 번호: P18-08(재생 속도, 컨트롤 막대, progress/stalled, preload=metadata), P18-10(오디오 실패·동시 재생 검증), P18-11/P18-01/02/03(형식 대응표와 점검 `media-matrix.md`).

완료한 내용:
1. **재생 속도** (`media/playback/RateConverter`, 새 클래스): 0.25~4 배. `preservePitch`(기본)이면 WSOLA(파형 유사 중첩 가산: 약 30 ms 창, 절반 겹침, 상관이 가장 높은 위치로 창을 옮김, 코사인 창, 스트리밍), 아니면 선형 보간 리샘플링(음높이도 속도를 따름).
   `MediaPlayer.setRate/getRate/setPreservesPitch/isRateSupported`; 위치 시계는 출력 프레임마다 속도만큼의 소스 프레임을 센다(`frameRatio`), 끝에서는 정확히 길이로 맞춘다. 속도는 블록 경계에서 적용된다(출력 버퍼의 약 150 ms 는 이전 속도).
   `HTMLMediaElement.playbackRate/defaultPlaybackRate/preservesPitch`, `ratechange` 이벤트(바뀔 때마다 하나, 같은 값은 없음), 새 로드는 `playbackRate` 를 `defaultPlaybackRate` 로 되돌리며 바뀌었으면 `ratechange`. 범위 밖(0, 음수, 4 초과, 0.25 미만)은 `NotSupportedError`, NaN 은 `TypeError`.
2. **`controls` 막대** (`render/MediaControls`, `MediaControlState`, `MediaControlSource`, `MediaControlsPainter`; `DisplayCommands.Placeholder` 에 상태 추가): 재생/일시정지 버튼(재생 중에는 일시정지 기호), `m:ss / m:ss` 시간 표시, 진행과 손잡이가 있는 탐색 막대, 스피커 버튼(음소거면 빗금), 음량 슬라이더.
   너비에 맞춰 부분을 뺀다(270 미만 음량 슬라이더, 190 미만 시간, 150 미만 음소거, 남는 폭이 24 미만이면 탐색 막대; 재생 버튼은 항상). 그리기와 마우스 처리가 같은 배치 함수를 쓴다.
   입력: `core/PageInput.pressMediaControls`(주 버튼 누름) → `HtmlPageDocument.controlMedia` → `PageScripts.controlMedia` → `MediaElements.control`(스크립트가 `play()`/`pause()`/`currentTime`/`muted`/`volume` 으로 하는 것과 같은 동작이므로 이벤트도 같다). 탐색/음량 슬라이더는 누른 채 끌 수 있다(막대 밖으로 나가도 계속, 놓으면 즉시 해제).
   취소된 `mousedown` 은 막대를 조작하지 않으며 DOM `click` 은 그대로 페이지에 도달한다. 상태가 바뀌면(`timeupdate` 를 포함한 미디어 이벤트마다) `HostEnvironment.mediaControlsChanged` → `PagePresentation.canvasChanged`(배치 없이 다시 그리기).
   로드 중인 요소는 흐린 탐색 막대(손잡이 없음)를 보인다.
3. **`progress`/`stalled`** (`MediaRequestSource.ProgressCallback`, `PageResources.requestMedia` 의 진행 리스너): 전송 중 바이트가 도착하면 `progress`(연속 보고는 하나로 합침), 3초 동안 데이터가 없으면 `stalled` 한 번(다음 진행이 타이머를 다시 시작, 스케줄러가 없는 호스트에서는 없음).
4. **`preload=metadata`**: `autoplay` 가 없고 재생 요청이 없으면 `HAVE_METADATA` 에서 멈춘다(`durationchange`, `loadedmetadata`, `progress`, `suspend`; `loadeddata`/`canplay`/`canplaythrough` 없음, 소리 출력 열지 않음, window `load` 는 기다리지 않음, 탐색은 가능).
   `play()` 가 `HAVE_ENOUGH_DATA` 로 올리며 `loadeddata`, `canplay`, `playing`, `canplaythrough` 를 낸다. **리소스는 여전히 통째로 가져온다**(전송량이 줄지는 않고 상태/이벤트가 규격을 따를 뿐).
5. **P18-10 검증**과 **P18-11 문서**: 아래 "검증 결과", `media-matrix.md`.

변경 파일:
- 신규 소스: `media/playback/RateConverter.java`, `render/{MediaControls,MediaControlState,MediaControlSource,MediaControlsPainter}.java`.
- 수정 소스: `media/playback/MediaPlayer.java`, `js/host/{MediaElements,MediaBindings,MediaRequestSource,PageScripts,HostEnvironment}.java`, `core/{PageInput,PageResources,PagePresentation,PageRuntime,HtmlPageDocument}.java`, `render/{DisplayCommands,DisplayListBuilder}.java`.
- 신규 시험: `media/playback/{RateConverterTest,JavaSoundDeviceTest}`, `render/MediaControlsTest`, `core/{AudioControlsIntegrationTest,AudioRobustnessIntegrationTest}`, `media/MediaFailureKindsTest`. 수정 시험: `media/playback/MediaPlayerTest`, `js/host/MediaElementsTest`, `core/AudioIntegrationTest`(로드 중인 요소와 로드된 요소가 이제 다르게 그려짐).
- 문서: 이 문서, `media-matrix.md`(새 문서).

검증 결과(JDK 6u45, `scripts/pkgtest.sh` 와 같은 컴파일 설정(`-source 1.6 -target 1.6`)의 증분 빌드 `build/pkg-play`, 격리 worktree, 다른 작업자와 CPU 공유; 모두 직접 실행한 결과):
- `org.duckdns.hjow.swingwebpanel.media.playback` 32 통과 / 0 실패 / 0 건너뜀: `MediaPlayerTest` 20(속도 시험 4건 추가: 범위, 길이와 위치 시계, 재생 중 변경, 탐색과 반복 뒤 유지), `RateConverterTest` 9(WSOLA/리샘플링의 길이, 음높이(영점 통과 주파수), 크기, 4배, 스테레오, 입력 나누기 무관, 아주 짧은 입력, 초기화),
  `JavaSoundDeviceTest` 3(지원하지 않는 형식 5종이 `ACCESS_FAILED`; **실제 Java Sound 라인**을 열어 무음 쓰기·정지·재개·비우기·닫기; `MediaPlayer` 가 실제 라인으로 무음 0.5초를 끝까지 재생 — 이 PC 에 믹서가 있어 건너뛰지 않고 실행되었다).
- `MediaElementsTest` 23 통과(추가 3: `testPlaybackRateEventsAndSound`, `testProgressAndStalledEvents`, `testPreloadMetadataStopsAtMetadataUntilPlayed`; `testVolumeMutedAndRate` 는 새 범위로 수정).
- `MediaControlsTest` 6 통과(너비별 배치, 부분 판정, 슬라이더 비율, 시간 문자열, 상태별 그려진 화소 차이).
- `AudioControlsIntegrationTest` 1 통과: 실제 컴포넌트 + `TestHttpServer` + 가짜 출력에 합성한 AWT `MouseEvent` 를 보내 재생 버튼(재생 → 위치 따라 막대 변화 → 일시정지), 탐색 막대 클릭(`seeked@2.5`), 음소거 켜고 끄기, 음량 슬라이더 클릭과 끌기(막대 밖까지, 놓은 뒤에는 움직이지 않음), 시간 표시 클릭은 무반응, DOM `click` 도달, `getContent()` 불변.
- `AudioIntegrationTest` 6 통과(기존, 로드 중인 요소와 로드된 요소의 막대가 달라진 것에 맞춰 한 단언을 고침).
- `AudioRobustnessIntegrationTest` 5 통과: 17 MiB 리소스(요청 한도 초과), 거절된 연결, 중간에 끊긴 응답, 글 페이지, 빈 파일이 모두 `error` 코드 4 로 끝나고 같은 페이지의 정상 오디오는 로드되며 window `load` 와 진행률 100 에 도달, 거짓 헤더(2 GiB 선언, 400 프레임) 는 있는 만큼만 재생,
  이동 중 늦게 도착한 응답은 출력을 열지 않음, 컴포넌트 둘/요소 둘의 동시 재생(각자의 출력에 각자의 PCM, 한쪽 `dispose()` 는 그 출력만 닫음).
- `MediaFailureKindsTest` 5 통과(형식별 실패 종류 표는 `media-matrix.md` 4절).
- `org.duckdns.hjow.swingwebpanel.media.image.avif` 32 통과 / 0 실패(`AvifOverlayTest` 5 + 기존 27; `AvifDecoderTest.testStructureErrors` 의 오버레이 단언은 "UNSUPPORTED" → "CORRUPT" 로 고침).
- `tools/sourcecheck` 0 위반(src/render, js/host, core, media 와 해당 시험 625 파일, AVIF 27 파일).
- **전체 회귀(`sh test.sh`, JDK 6u45, 이 worktree 의 최종 커밋 `eca636e` 기준): 2997 통과 / 0 실패 / 12 건너뜀(757217 ms)**. 건너뜀 12건은 모두 선택적 Opus 원본 벡터(`SWP_OPUS_VECTORS` 없음). 제품 JAR 검사(`JarContentTest`)와
  실행기 종료 코드 자체 점검(`[OK] runner exit-code self-check passed`)도 통과했다. 이 PC 의 다른 작업자와 CPU 를 나눠 쓰는 중이었고 시간 민감 시험 실패는 없었다.

미해결 사항(갱신: 위 "2026-11-17 P18-08 ..." 기록의 번호별 한계를 이렇게 바꾼다):
1. ~~재생 속도 1.0 만~~ → 0.25~4 배 지원. 4 배 근처에서는 WSOLA 의 품질이 떨어진다(창이 건너뛰므로 거칠다). 속도를 바꾼 직후 약 150 ms(출력 버퍼)는 이전 속도로 들린다. 0 이하/16 배 같은 극단은 `NotSupportedError`.
2. **JavaScript 가 꺼져 있으면 미디어 요소가 동작하지 않는다**(변경 없음): `controls` 막대도 그려지지만 조작해도 아무 일이 없다.
3. 리소스를 **통째로** 가져온다(16 MiB 한도, 초과하면 실패). `preload=metadata` 는 상태·이벤트만 규격대로이고 전송량은 `auto` 와 같다. 진행 중 `buffered` 는 비어 있다가 로드가 끝나면 전체가 된다(디코더가 데이터를 통째로 요구하므로).
   기본(속성 없음)은 `auto` 로 취급한다(규격은 UA 가 정함).
4. 로드한 요소마다 재생 스레드 1개(변경 없음).
5. 없는 것(변경 없음, 미지원으로 기록): 오디오/비디오/텍스트 트랙, `mediaGroup`/`controller`, `getStartDate`, `srcObject`, `captureStream()`, `setSinkId()`/`sinkId`, `fastSeek`, `crossOrigin` 의 CORS 효과. **정의하지 않았으므로 기능 검출(`'captureStream' in audio`)이 "없음" 으로 답한다.**
6.~10. 변경 없음(위 목록 참조).
11. **`autoplay`**: 브라우저의 사용자 제스처 정책(소리 있는 자동 재생 차단)이 없다 — 항상 재생한다.
12. **`controls` 막대**: 마우스만(키보드 조작, 호버/포커스 표시, 우클릭 메뉴, 다운로드 버튼, 재생 속도 메뉴, 버퍼링 범위 표시 없음). `controlsList`/`disableRemotePlayback` 무시. 시간 표시는 `m:ss` 로 소수 초를 버린다. 막대는 `audio` 에만 있고 `video` 는 미지원 안내를 그린다.
13. 재생 속도 변환 중 `seekable`/`duration` 은 원래 길이이고 변환 때문에 위치가 최대 한 창(약 30 ms) 틀릴 수 있다. 끝에서는 위치가 길이와 정확히 같다.

### 미확인 (자동 검증과 실제 확인의 분리, P18-10 / P22-10)

| 층 | 자동 검증(직접 실행해 통과) | 실제로 확인하지 못한 것 |
| --- | --- | --- |
| 디코딩, PCM | 형식별 디코더 시험, 가짜 출력에서 PCM 바이트 일치(`MediaPlayerTest`) | 실제 파일(libvorbis/lame/faac 가 만든 것)과의 상호 운용 |
| 상태, 이벤트 | `MediaElementsTest`, `AudioIntegrationTest`, `AudioRobustnessIntegrationTest`(실제 컴포넌트 + HTTP + 가짜 출력) | |
| 실제 Java Sound 라인 | `JavaSoundDeviceTest`: 이 PC 의 믹서(DirectSound 3개)로 실제 라인을 열고 **무음**을 쓰고 위치 시계가 끝에 닿는 것(JRE 6u45) | **소리가 귀에 들리는지**, 음량/음소거가 장치에서 실제로 작동하는지, 속도 변환된 소리의 음질 |
| 마우스 조작 | `AudioControlsIntegrationTest`: 합성한 AWT `MouseEvent` 를 실제 컴포넌트에 보내 재생/일시정지/탐색/음소거/음량(클릭과 끌기)과 그려진 막대의 변화 | 실제 마우스, 실제 Swing 창에서의 모양과 반응 |
| 동시 재생 | 컴포넌트 둘, 한 페이지의 요소 둘(각자의 출력에 각자의 PCM) | 두 소리가 실제 장치에서 섞여 들리는 것 |
| Web Audio 실시간 출력(P25 C2a, `OutputRenderDriver`) | 가짜 출력/가짜 `SourceDataLine` 에서 PCM 이 같은 그래프의 오프라인 렌더링과 float 로 한 비트도 다르지 않음(16비트 경로는 반올림한 정수와 정확히 같음), 일시정지의 장치 해제, 멈춘 장치가 일시정지/닫기를 붙잡지 않음, 100개 컨텍스트 생성/닫기 뒤 스레드·열린 출력 0, `NullOutput` 의 벽시계 속도(`OutputRenderDriverTest`, `JavaSoundOutputTest`, `NullOutputTest`) | **소리 장치가 있는 PC 에서 귀로**: 오실레이터/버퍼 소리가 나는지, 일시정지가 곧바로 조용한지, 끊김이 없는지, `latencyHint` 세 종류의 체감 지연, 장치가 44100 Hz 를 지원하지 않을 때 변환된 소리의 품질, 두 컨텍스트 동시 재생, 장치를 다른 프로그램이 쓸 때 무음으로 대체되는지, `setSinkId({type:'none'})` 뒤의 무음. 실제 `SourceDataLine` 의 `available()` 값, `write` 의 비블록 여부, 일부 장치의 버퍼 크기 조정 |
| Web Audio 요소 연결(P25 C2b, `createMediaElementSource`) | 시험 WAV 의 PCM 이 그래프(게인)를 거쳐 가짜 출력에 `표본/32768×게인` 과 비트 단위로 도착, 요소의 직접 출력은 없음, 요소 시계가 그래프 속도로 진행, 일시정지/탐색/반복/`playbackRate`, 변환 결과가 `Resampler.resample` 과 비트 단위로 같음, 이동/`clean()`/`dispose()`/스크립트 끄기의 스레드·출력·재생기 정리(`MediaElementTapTest`, `MediaElementSourceScriptTest`, `WebAudioOutputComponentTest`) | **소리 장치가 있는 PC 에서 귀로**: `<audio>` + `createMediaElementSource` + 게인으로 음량이 실제로 바뀌는지, 요소의 일시정지/탐색/반복이 소리에 맞는지, 재생 중인 요소를 연결하는 순간의 소리, 44100 Hz 파일이 48000 Hz 컨텍스트에서 변환된 소리의 품질, 두 컨텍스트·두 컴포넌트 동시 재생과 한쪽 `dispose()`, 실제 Swing 창에서 사용자 제스처로 시작하는 페이지 |
| Web Audio 오디오 worklet(P25 D1) | 프로세서(이득·사인·입력 복사)가 오프라인 렌더링에서 닫힌 식과 일치(결정적), 파라미터 a-rate/k-rate, 포트 메시지, 실패 시 영구 무음, 느린 프로세서의 건너뜀과 경고, 이웃 노드 무영향, 페이지 이동/`dispose()` 때 realm·스레드 정리(`AudioWorkletNodeTest`, `js.host.webaudio.worklet.*`) | **소리 장치가 있는 PC 에서 귀로**: worklet 프로세서(사인, 이득)가 실제 스피커로 들리는지, 프로세서가 바쁠 때 끊김이 어떻게 들리는지, 일곱 개 안팎의 프로세서에서 끊김 없이 도는지 |

## 작업 기록

### 2026-11-26 P18-08-c3a — 파일 레지스트리와 재생기 소유권

오디오 레지스트리는 MIME 힌트를 적용하되 실제 시그니처로 디코더를 선택한다. 파일을 여는 경로에서는 기본 코덱에 바이트 원천을 주고 배열 전용 등록 코덱은 최대 16 MiB까지 읽어서 전달한다. 재생기가 받은 인코딩 리소스는 탐색 불가 소스의 재열기에도 사용하고, 닫힘과 로드 실패에서 정리한다.

JRE 6u45의 32 MiB 힙에서 72 MiB WAV를 기존과 다른 MIME 힌트로 열어 마지막 두 표본을 출력했다. 로드 거부·탐색 불가 경로 재열기·임시 파일 삭제를 포함한 재생기 시험 22건을 통과했다.

### 2026-11-26 P18-08-c2d2 — WebM에서 Ogg 페이지 재포장

WebM의 Vorbis·Opus를 공통 페이지 작성기로 Ogg에 담는다. 패킷 크기의 임시 복사 대신 세그먼트 단위로 읽고, 세그먼트가 페이지를 넘을 때 이어짐·그라뉼·순서 번호를 유지한다. 파생 스트림이 1 MiB를 넘으면 임시 파일에 쓴다. WebM 원천은 재포장이 성공하면 닫히며, Ogg 임시 파일은 PCM 소스의 닫힘과 함께 삭제한다.

JRE 6u45의 32 MiB 힙에서 실제 Vorbis·Opus WebM의 배열/파일 PCM과 역탐색, 파생 파일의 처음과 끝 읽기 및 닫힘을 검증했다. 페이지·CRC·파일 원천·참조 오디오를 포함해 47건 통과했다.

### 2026-11-26 P18-08-c2d1 — WebM 파일 트랙 색인

WebM/Matroska의 EBML 판독기가 트랙·블록·레이싱 테이블을 파일 창에서 읽는다. 페이로드는 오프셋으로 참조하고 형식·코덱 식별자는 128바이트, 코덱 초기화 자료는 1 MiB까지 읽는다. 정수 범위를 넘는 레이싱을 거부하며, 잘린 블록 앞의 완전한 프레임은 계속 사용할 수 있다.

JRE 6u45에서 13바이트 파일 창을 사용해 레이싱 없는 블록과 Xiph·고정·EBML 레이싱의 트랙·패킷을 확인했다. 각각 잘린 끝을 추가로 확인했고 WebM·Opus 회귀 12건이 통과했다.

### 2026-11-26 P18-08-c2c2b — 네이티브·Ogg FLAC의 파일 읽기

네이티브 FLAC은 메타데이터, 프레임 헤더와 프레임 데이터를 따로 읽는다. 첫 창은 64 KiB이며 프레임이 잘리면 겹친 바이트를 재사용해 확장한다. 창 한도는 STREAMINFO가 표현하는 프레임 바이트 최대값이다. 헤더·프레임 CRC와 손상·탐색 정책을 보존한다. Ogg FLAC은 파일 패킷을 쓰며 두 경로 모두 PCM 소스가 원천을 닫는다.

JRE 6u45의 32 MiB 힙에서 15개 참조 파일과 80,000바이트보다 큰 프레임을 포함한 변형을 배열·파일로 읽고 16비트·float 출력과 탐색·닫힘을 확인했다. FLAC·Ogg·참조 오디오·다채널 회귀 44건이 통과했다.

### 2026-11-26 P18-08-c2c2a — Ogg Vorbis·Opus 디코더의 파일 패킷

두 디코더는 Ogg의 바이트 원천 색인과 패킷 읽기를 사용한다. 시작·끝 보정, 역탐색, 채널 보존 및 연쇄의 기존 규칙을 유지하며 16비트나 float PCM 출력을 닫을 때 파일 원천을 해제한다.

실제 Vorbis 9개·Opus 12개와 스테레오에서 모노로 이어지는 Opus 연쇄의 전체 출력 및 탐색 뒤 출력이 배열과 같았다. JRE 6u45에서 파일 닫힘·코덱·Ogg·WebM·참조 오디오 회귀 64건이 통과했다.

### 2026-11-26 P18-08-c2c1 — Ogg 페이지와 분할 패킷

Ogg 색인은 파일의 헤더와 세그먼트 표를 읽어 페이지의 위치·그라뉼·이어짐 플래그를 보관한다. 읽을 때는 한 페이지 버퍼에서 체크섬을 검증하고 분할 패킷을 재조립한다. 호출자가 원천을 소유하며 파일 읽기 실패는 `ACCESS_FAILED`로 전달한다.

JRE 6u45에서 17바이트 파일 창을 사용해 70,000바이트 분할 패킷, 페이지 사이의 잡음, 잘못된 CRC, 마지막 그라뉼과 닫힌 파일을 확인했다. 기존 Ogg와 참조·코덱 회귀를 포함해 70건을 통과했다.

### 2026-11-26 P18-08-c2b2 — ADTS·MP4 AAC 파일 블록

ADTS 프레임 헤더와 MP4 샘플 표로 원시 블록의 위치를 색인한다. 16비트 출력과 float 출력은 재사용 버퍼로 한 블록씩 읽는다. MP4 접근 단위는 1 MiB까지 버퍼에 읽으며, 범위를 넘는 손상 샘플은 기존과 같이 건너뛴다. 파일 읽기 오류는 디코딩 오류와 구분하고 PCM 소스가 닫힐 때 파일도 닫는다.

참조 AAC 25개와 CRC 필드를 추가한 ADTS의 파일·배열 PCM과 역탐색이 같았다. float 경로는 모든 채널을 끝까지 비교했다. JRE 6u45에서 AAC·MP4 및 참조 오디오 회귀 38건이 통과했다.

### 2026-11-26 P18-08-c2b1 — MP4 샘플 표와 파일 박스

MP4 역다중화기는 바이트 원천의 박스 헤더와 샘플 표를 읽고 `mdat`은 길이만 사용해 넘긴다. 잘못된 확장 크기와 조각 필드의 박스 경계를 확인한다. AAC/Opus 코덱 설정 복사에 64 KiB 한도를 둔다. 파서는 전달받은 파일 원천을 소유하지 않는다.

JRE 6u45에서 32 MiB 힙으로 72 MiB `mdat`이 있는 파일을 열었다. 일반·조각 파일의 표본 개수·위치·크기·시간과 설정을 배열과 비교했다. 확장 길이 초과도 거부했다. MP4 집중 5건 및 AAC·참조 오디오·float 회귀 포함 37건이 통과했다.

### 2026-11-26 P18-08-c2a — 바이트 원천의 MP3 프레임

MP3의 ID3와 Xing/Info/VBRI/LAME 정보 및 프레임 색인을 배열과 파일에 공통 적용한다. 색인에는 위치만 두고 재생 중에는 해당 프레임을 재사용 버퍼로 읽는다. 기존 MPEG 버전·채널·PCM 출력·탐색 프리롤을 유지하며, 소스가 닫힐 때 입력도 닫는다.

JRE 6u45에서 실제 libmp3lame 자료 16개와 끝을 자른 파일의 전체 PCM과 역탐색이 배열과 같았다. 기존 참조 오디오 및 float 회귀까지 32건 통과했다. 바이트 원천의 다운로드 연결은 P18-08-c3에 남아 있다.

### 2026-11-26 P18-08-c1b — WAV·AIFF·AU의 파일 헤더와 표본 읽기

`ByteAudioDecoder`의 배열 진입점이 공통 바이트 원천을 감싼다. WAV·AIFF·AU 디코더는 같은 헤더 파서로 배열과 파일을 읽고, 파일 열기에 성공하면 PCM 소스가 입력을 소유한다. 헤더가 잘렸으면 `CORRUPT`, 실제 읽기에 실패하면 `ACCESS_FAILED`를 반환한다.

JRE 6u45에 `-Xmx32m`을 적용해 72 MiB WAV의 첫·마지막 표본과 역탐색을 확인했다. 형식별 파일 비교를 포함한 작은 힙 시험 4건, 기본 힙의 PCM·ADPCM·참조 디코딩 시험 55건을 통과했다. 브라우저 요청의 임시 파일 연결은 P18-08-c3에 남겼다.

### 2026-11-26 P18-08-c1a — PCM과 ADPCM에 바이트 원천 연결

공통 PCM 소스는 배열이나 파일의 `ByteSource`를 소유한다. 16비트와 float PCM은 최대 2,048프레임을 재사용 버퍼로 읽고, IMA·QuickTime IMA·Microsoft ADPCM은 블록마다 읽는다. 이전 변환과 탐색 알고리즘을 재사용한다. 읽을 수 없는 파일은 `ACCESS_FAILED`를 반환하며, 결과 소스가 닫히면 입력 원천도 닫는다. 이 단계에서는 아직 파일 헤더나 페이지 요청을 연결하지 않았다.

JDK 6u45로 변경 클래스를 컴파일하고 JRE 6u45에서 `PcmByteSourceTest`, PCM 디코더, `WidePcmTest`, `ByteSourceTest`, `BlockPcmSourceTest`, `ReferenceDecodeTest` 총 53건을 통과했다. 새 사례는 작은 파일 창, 2,048프레임 경계, 마지막 ADPCM 블록, 탐색, 파일 원천 닫힘을 확인했다.

### 2026-11-26 P18-08-b3c — 스크립트 없는 오디오의 기본 컨트롤

문서가 스크립트를 실행하지 않을 때 `controls` 속성 없이도 오디오 컨트롤을 표시한다. CSS UA 스타일과 작성자 스타일의 `(scripting: none)` 질의가 같은 문서 상태를 읽는다. 속성을 추가하지 않으므로 `[controls]` 선택자와 수신 원문은 그대로다. 숨긴 오디오는 Tab 순서에 들어가지 않으며, 보이는 막대는 기존 키보드와 마우스 입력 경로를 이용한다.

JRE 6u45에서 기존 문서의 스크립트를 끄고 다시 그린 화면과 PCM 출력을 확인했다. 미디어·포커스·샌드박스·CSS 관련 집중 회귀는 92건 통과했다.

### 2026-11-26 P18-08-b3b — 열린 문서에서 JavaScript 비활성화

호스트가 스크립트를 끄면 기존 렐름·Web Audio·HTML 미디어 출력을 닫고 현재 문서의 컨트롤은 공통 미디어 상태 기계로 교체한다. 활성 문서의 HTML 오디오는 자동으로 재시작하지 않으며 사용자 조작 뒤 처음부터 시작한다. 파서에서 인계하기 전에 비활성화된 문서는 새 문서의 preload와 autoplay 정책을 적용한다. 문서가 이미 교체되었으면 오디오를 설치하지 않는다.

JRE 6u45에서 새 전환 및 파싱 중 비활성화 2건과 기존 Web Audio 장치·수명 5건이 통과했다. 문서·프레임·worker까지 포함한 실행은 72건 통과와 프레임 제거 사례 1건 실패였다. 그 프레임 시험은 단독 실행에서 통과했다. 실제 스피커 청취는 사용자의 샘플 게임 확인으로 이월했다.

### 2026-11-26 P18-08-b3a — 스크립트를 차단한 프레임의 오디오

프레임의 표시 크기와 문서 큐를 사용하는 미디어 호스트를 연결했다. [HTML5 REC §4.7.10.7](https://www.w3.org/TR/2014/REC-html5-20141028/embedded-content-0.html#ready-states)의 자동 재생 조건에 따라 `allow-scripts`가 없는 샌드박스에서 자동 시작을 막는다. `preload=none`은 사용자 재생까지 가져오기를 미룬다. 프레임을 버리면 그 출력과 콜백도 해제한다.

JRE 6u45의 `AudioControlsIntegrationTest`, `FrameSandboxTest`, `FrameNavigationTest`, `FrameRenderingTest`, `MediaElementsTest`가 총 57건 통과했다. 새 사례는 작성자 스크립트·오디오 요청이 자동으로 발생하지 않고, 클릭 뒤 PCM이 도착하며 프레임을 닫으면 출력이 해제됨을 확인했다.

### 2026-11-26 P18-08-b2 — 스크립트가 꺼진 새 최상위 문서의 오디오

`PageAudio`가 확정된 최상위 문서에서 공통 리소스와 Java Sound 출력을 연결한다. `UnscriptedMedia`가 별도 스크립트 렐름 없이 `MediaElements`의 상태·이벤트를 문서 큐에서 실행한다. `<audio controls preload=none>`은 사용자가 재생할 때 가져오고, `autoplay`는 입력 없이 재생한다. 오디오 적재가 끝나거나 실패하면 진행률이 끝날 수 있고 페이지 교체·clean·폐기에서는 해당 미디어와 스케줄러를 닫는다.

`AudioControlsIntegrationTest`에 스크립트를 끈 문서의 제어·가져오기·PCM·clean 및 404 응답과 자동 재생을 검증하는 사례 두 건을 추가했다. JRE 6u45의 미디어·텍스트 트랙·Web Audio·포커스·페이지 수명과 메타 갱신 집중 회귀 91건이 통과했다. 활성 문서에서 끄는 경우, 하위 프레임, `controls`가 없는 컨트롤 표시, 실제 출력 장치는 별도 작업이다.

### 2026-11-26 P18-08-b1 — 사용자 조작과 `play()`의 프라미스 분리

미디어 막대를 사용한 재생과 스크립트의 `play()`는 같은 내부 상태 전이를 사용한다. 막대 재생은 페이지의 프라미스를 만들거나 호출하지 않는다. 스크립트에는 기존 `playing` 시점의 이행과 실패 시 거부가 유지된다. `AudioControlsIntegrationTest`는 페이지가 `Promise.prototype.catch`를 예외를 던지는 함수로 바꾸어도 막대 재생이 이를 호출하지 않는지 확인한다.

JRE 6u45의 `AudioControlsIntegrationTest`, `AudioIntegrationTest`, `MediaElementsTest`, `TextTrackTest`, `MediaElementSourceScriptTest` 58건이 통과했다. preload의 `metadata`는 DOM에서 `HAVE_METADATA`로 멈추어 `play()`를 기다린다. 수신 데이터는 `auto`와 같이 통째로 가져오므로 네트워크 사용량과 보관 용량에는 여전히 한계가 있다.

### 2026-11-26 P18-08-a — 오디오 컨트롤 키보드 입력

`controls`가 있는 오디오 요소는 하나의 Tab 정지점이며 포커스를 기존 CSS 상태에 표시한다. Space와 Enter는 재생/일시정지를 전환한다. 좌우는 5초씩 탐색하고 위아래는 음량을 10%씩 바꾸며 M은 음소거를 전환한다. `preventDefault()`로 취소된 `keydown`에는 이 동작을 적용하지 않는다. 키를 눌러 반복해도 재생이나 음소거는 한 번만 전환한다. 제어 동작의 기준은 [HTML5 REC, 2014-10-28 §4.7.10.13](https://www.w3.org/TR/2014/REC-html5-20141028/embedded-content-0.html#user-interface)이며 2026-11-26에 webfetch로 조회했다. 해당 절에는 개별 키 배치가 정해져 있지 않다.

`AudioControlsIntegrationTest.testKeyboardOperatesFocusedAudioControls`가 실제 SwingWebPanel의 큐에 합성 AWT 키를 넣고 포커스·계산 스타일·미디어 DOM 이벤트를 확인한다. `AudioControlsIntegrationTest`, `AudioIntegrationTest`, `KeyboardFocusTest`, `EditingInputTest`로 JRE 6u45에서 28건을 통과했다. 실제 장치 청취와 JavaScript가 꺼진 문서에서의 재생은 남는다.

### 2026-11-17 P18-08, P18-05-c, P18-09(DOM) — 오디오 재생 엔진, `<audio>` 상태 기계, `<video>` 미지원 결과

현재 작업 번호: P18-08(오디오 재생·DOM 상태·이벤트), P18-05-c(`source` 목록의 형식 선택), P18-09(video DOM 부분). 체크박스 후보는 아래.

완료한 내용:
1. **재생 엔진** `media/playback`: `MediaPlayer`(압축 데이터를 한도 안에서 메모리에 두고 데몬 스레드에서 **블록 단위(2048 프레임)로 디코딩해 출력에 쓴다. 디코딩된 PCM 전체는 보관하지 않는다**),
   `AudioOutput`/`AudioOutputFactory`(출력 추상화), `JavaSoundOutputFactory`(`javax.sound.sampled` 의 `SourceDataLine`, 버퍼 약 150 ms, 장치가 없거나 형식을 열 수 없으면 `ACCESS_FAILED`),
   `PlaybackListener`, `MediaInfo`, `MediaRanges`(정규화된 시간 범위). 시계는 소리 자체: 위치 = (출력이 받은 마지막 소스 프레임) − (출력 큐에 남은 프레임)이며 일시정지 중에는 멈추고 타이머가 필요 없다.
   일시정지는 `pause()` 가 돌아오기 전에 출력을 멈추고, 탐색은 위치가 곧바로 새 값으로 읽히고 재생 스레드가 출력을 비운 뒤 디코더를 옮긴다(`seeked` 보고).
   반복(`loop`)은 출력을 비우지 않고 이어 쓴다(틈 없음; 탐색 불가 소스는 데이터를 다시 열어 읽는다). 음량/음소거는 16비트 샘플에 고정 소수점 이득(`(s*gain)>>15`)을 **소프트웨어로** 곱한다(장치의 `FloatControl` 에 의존하지 않음).
   길이를 미리 모르는 소스(`getFrameCount()==-1`)는 길이가 `Infinity` 였다가 끝에 닿으면 알게 되어 `durationKnown` 으로 보고한다. `played` 범위는 재생 구간마다 합쳐 정규화한다. 실패(장치 없음, 손상된 스트림 도중)는 `failed` 로 알리고 재생기는 로드된 채로 남아 `play()` 가 다시 시도한다.
   소유자가 사라지면(리스너를 약하게 참조) 재생 스레드가 스스로 출력을 닫고 끝난다(쉬는 동안 1초마다 확인). 출력은 재생 스레드에서 정확히 한 번 닫는다. `close()` 는 스레드를 최대 2초 기다린다.
2. **요청 서비스와 core 연결**: `js/host/MediaRequestSource`(절대 URL → 바이트+MIME 또는 실패, 취소 핸들) ← `core/PageRuntime.getMediaRequestSource()` ← `PagePresentation.requestMedia` ← `PageResources.requestMedia`
   (공통 요청 계층: User-Agent, 추가 헤더, 쿠키, SSL 훅, 스키마 제한, HTTP 캐시, 목적 `AUDIO`; 요청마다 전용 데몬 스레드; 페이지 취소/요청 취소 시 콜백 없음. 응답 크기는 요청 계층의 메모리 응답 한도 `FetchLimits.DEFAULT_MAX_MEMORY_BYTES`(16 MiB)를 따른다).
   소리 출력은 `HostEnvironment.getAudioOutputFactory()` ← `PageRuntime` ← `PageView.getAudioOutputFactory()` ← `SwingWebPanel` 이 만든 위임 팩터리 ← **`protected AudioOutputFactory createAudioOutputFactory()`**(기본 `JavaSoundOutputFactory`; 시험/호스트가 재정의; 첫 사용 때 한 번 호출; null 이면 "소리 출력 없음").
   렌더러 연결: `HostEnvironment.mediaStateChanged(url, error)` → `PageResources.mediaState` 가 **`getMedia(url)` 가 실제 상태(PENDING/AVAILABLE/FAILED)를 반환**하게 하고 다시 그리게 한다.
   상태는 렌더러(`DisplayListBuilder.paintAudioElement`)가 보는 주소, 즉 `src` 속성 또는 첫 `source` 자식의 `src` 에 기록한다(후보 하나가 실패해도 다른 후보가 재생되면 실패로 표시하지 않고, 쓸 후보가 없으면 FAILED).
3. **DOM/JS**: `MediaElements`(요소별 상태 기계; HTML5 4.7.10 의 로드 알고리즘, 리소스 선택(속성/자식 모드, `type`·`media` 검사, 실패하면 다음 후보, `source` 삽입을 기다림), 준비/네트워크 상태, 재생·일시정지·탐색 알고리즘, 이벤트 순서)와
   `MediaBindings`(JS 멤버). 멤버: 상수 `NETWORK_*`/`HAVE_*`(DomInterfaces), `error`(MediaError, `MEDIA_ERR_*`, `message`), `src/currentSrc/networkState/readyState/paused/ended/seeking/currentTime/duration/volume/muted/defaultMuted/loop/autoplay/preload/controls/crossOrigin`,
   `buffered/seekable/played`(TimeRanges, `IndexSizeError`), `play()`(Promise), `pause()`, `load()`, `canPlayType()`, `new Audio(url)`/`HTMLAudioElement`. 이벤트 15종 이상(loadstart, progress, suspend, abort, error, emptied, loadedmetadata, loadeddata, canplay, canplaythrough, playing, waiting, seeking, seeked, ended, durationchange, timeupdate, play, pause, ratechange(미사용: 아래), volumechange).
   이벤트는 문서 큐(페이지 결과 줄 `ScriptLoads` → `PageScripts.runReadyAsync` → 페이지 잠금 + 마이크로태스크 검사점)로 전달되고, 요소의 로드 세대(`epoch`)가 다른 작업(새 로드 뒤의 오래된 결과)과 폐기 뒤의 모든 작업은 버린다.
   요소는 리소스 선택을 시작할 때부터 `loadeddata` 까지 window `load` 이벤트를 늦춘다. 문서에서 제거된 재생 중인 요소는 일시정지된다. 재생 중인 요소는 스스로 도달 가능하게 유지(참조 없는 `new Audio(url).play()` 가 끝까지 재생)하고 그 밖의 요소는 약하게 잡는다.
   `play()` 의 Promise 는 **소리가 정말 시작되었을 때**(출력이 열리고 첫 데이터를 받음, `PlaybackListener.started`) 이행하고, 장치가 없으면 `NotSupportedError`, 손상이면 `EncodingError`, 새 로드는 `AbortError`, `pause()` 는 `AbortError` 로 거부한다.
4. **`autoplay`/`preload`**: `autoplay`(스크립트가 play/pause 를 부르지 않았으면 `canplay` 뒤 `play`,`playing`, `canplaythrough` 앞), `preload=none`(가져오기를 `play()` 까지 미루고 `suspend`, load 이벤트 지연 없음). `metadata`/`auto`/기본값은 모두 리소스 전체를 가져온다. `controls` 의 그림은 렌더러(`DisplayListBuilder.paintAudioElement`, 기본 300x54)가 이미 갖고 있으며 위의 상태 연결로 동작한다.
5. **P18-09 video**: `HTMLVideoElement` 는 있되 디코더·재생기·외부 프로그램 없이 명시적 미지원 결과를 준다: `canPlayType` 은 언제나 `""`, 자원을 요청하지 않으며, 리소스 선택은 `loadstart` → `error`(`MEDIA_ERR_SRC_NOT_SUPPORTED`, `networkState=NETWORK_NO_SOURCE`)로 끝난다
   (`source` 자식은 각각 `error`, 자식 모드와 자원 없는 요소도 마지막에 요소에 `error` 한 번), `load()`/`play()` 도 같은 결과, `play()` 는 `NotSupportedError` 로 거부, `videoWidth/videoHeight` 는 0.

변경 파일:
- 신규 소스: `src/org/duckdns/hjow/swingwebpanel/media/playback/{package-info,AudioOutput,AudioOutputFactory,JavaSoundOutputFactory,PlaybackListener,MediaInfo,MediaRanges,MediaPlayer}.java`,
  `src/org/duckdns/hjow/swingwebpanel/js/host/{MediaElements,MediaBindings,MediaRequestSource,MediaErrorValue,TimeRangesValue}.java`.
- 수정 소스: `js/host/{HostEnvironment(getMediaRequestSource, getAudioOutputFactory, mediaStateChanged),PageScripts(media 필드, 변경/결과/load 지연/폐기 연결),ElementExtras(1줄)}.java`,
  `core/{PageRuntime,PagePresentation,PageResources,PageView}.java`, `SwingWebPanel.java`(`createAudioOutputFactory` 훅과 위임 팩터리), `js/dom/DomInterfaces.java`(상수, `defaultMuted`).
- 시험: `tests/src/org/duckdns/hjow/swingwebpanel/media/playback/{FakeOutputs,RecordingListener,ScriptedDecoder,MediaPlayerTest}.java`, `tests/.../js/host/{MediaElementsTest,PageFixture}.java`, `tests/.../core/AudioIntegrationTest.java`.

검증 결과(JDK 6u45, 격리 스냅샷):
- `MediaPlayerTest` 17건(가짜 출력으로 PCM 바이트 일치, 위치=재생된 프레임, 일시정지/재개/탐색/반복/음량/음소거/장치 없음/손상·미지원 데이터/도중 손상/닫기·출력 close 횟수/길이 미상·탐색 불가 소스/버려진 재생기 자가 종료(GC 의존이라 수거되지 않으면 건너뜀으로 기록; 이 실행에서는 수거됨)/시간 범위),
  `MediaElementsTest` 20건(이벤트 순서와 상태, autoplay, play Promise, 일시정지·탐색, source 선택(type/media/실패 후보/디코딩 실패 후보/기다림), 실패 src, canPlayType, video 미지원, 음량·속도, TimeRanges, `new Audio`, preload=none, 새 로드가 이전 로드를 취소, 제거 시 일시정지, 폐기, 서비스 없음, loop, 이벤트 속성),
  `AudioIntegrationTest` 6건(실제 컴포넌트 + `TestHttpServer`: User-Agent/추가 헤더/쿠키가 요청에 실림, 이벤트 순서, 출력에 전체 PCM, `getContent()` 원문 불변, 실패/video, 장치 없음 → `error` 코드 3, 이동/`clean()`/`dispose()` 에서 출력 close, 페이지 리소스의 미디어 요청과 상태, 실제 그림에서 재생 가능/실패 오디오가 다르게 그려짐) — 모두 통과.
- 광범위 회귀(`org.duckdns.hjow.swingwebpanel.` 필터, js/core/media/render/layout 시험 전체): 마지막 실행 결과는 이 문서 하단 "회귀 결과" 에 적었다.
- 실제 장치(자동 검증이 아님): JRE 6u45 에서 `JavaSoundOutputFactory` 로 이 컴퓨터의 라인을 열고 **무음 PCM** 을 쓰고 flush/stop/close 하는 것과 `MediaPlayer` 로 1초 무음 WAV 를 끝까지 재생하는 것을 던져 버릴 프로그램으로 확인했다(열림/쓰기/종료 성공, 종료 보고는 재생 시작 지연+장치 버퍼 때문에 약 0.4초 늦음, 라인의 `available()` 갱신이 늦어 위치가 조금 뒤로 가는 것을 위치 보정으로 막음).
  **소리를 귀로 확인하지 않았다.** 자동 검증은 모두 가짜 출력이며 실제 장치 청취는 하지 않았다(P18-10, P22-10).

미해결 사항(한계, 숨기지 않고 기록):
1. **재생 속도는 1.0 만 지원**한다. `playbackRate`/`defaultPlaybackRate` 에 다른 값을 넣으면 `NotSupportedError`(조용히 무시하지 않음), `ratechange` 이벤트는 발생하지 않는다. 피치를 바꾸는 선형 보간 리샘플러로 확장할 수 있다(시도하지 않음).
2. **JavaScript 가 꺼져 있으면 미디어 요소가 동작하지 않는다**(요소 상태 기계가 페이지 런타임 안에 있다): `autoplay` 도, 로드도 일어나지 않는다. 렌더러는 PENDING 으로 보아 `controls` 막대를 그리지만 재생은 안 된다. P15-10 의 "JS 끄기에서도 HTML 기본 기능" 에 미디어를 넣을지는 조정자 판단.
3. 리소스를 **통째로** 가져온다(응답 한도 16 MiB 초과는 실패 → `MEDIA_ERR_SRC_NOT_SUPPORTED`). `preload=metadata/auto` 구분 없음, 진행 중 `progress`/`stalled` 없음(로드 끝에 `progress` 한 번), 데이터가 메모리에 있으므로 `HAVE_ENOUGH_DATA` 로 건너뛰고 `waiting` 은 `play()` 가 데이터보다 먼저 불렸을 때만 발생.
4. 로드한 요소마다 **재생 스레드 1개**(데몬)가 요소가 수거/`load()`/폐기될 때까지 산다(쉬는 동안 1초마다 소유자 확인). 오디오 요소를 아주 많이 로드하는 페이지는 스레드가 많아진다.
5. 없는 것: 오디오/비디오/텍스트 트랙(`audioTracks`, `textTracks`, `track` 요소), `mediaGroup`/`controller`, `getStartDate`, `crossOrigin` 의 CORS 효과(요청은 `AUDIO` 목적의 no-cors), `srcObject`, `fastSeek`, `preservesPitch`, `preload` IDL 값의 열거형 제한(원문 그대로 반환).
6. `currentTime` 은 안정된 "official playback position" 이 아니라 실시간 값이다(같은 작업 안에서 두 번 읽으면 재생 중에는 달라질 수 있음).
7. `volume` 변경은 출력 버퍼에 이미 있는 약 150 ms 와 디코딩 중인 블록(약 46 ms)에는 적용되지 않는다.
8. 소스 길이를 모르는 경우 `seekable` 은 비어 있고(탐색 불가로 취급) `buffered` 는 현재 위치까지로 근사한다. WebM(Vorbis)은 길이가 최대 한 블록 더 길 수 있다(AUD-B 기록).
9. 장치 없음/출력 열기 실패는 `MEDIA_ERR_DECODE`(3)와 메시지로 알리고 요소를 다시 `paused` 로 만든다(규격은 `paused` 를 그대로 둔다. `play()` 재시도를 위한 의도적 차이).
10. `HTMLMediaElement.prototype` 의 접근자는 `Illegal invocation` 검사를 하지만 `audio`/`video` 외의 요소가 프로토타입을 빌려 호출하면 TypeError 이다.

다음 작업 번호: P18-10(장치 부재/손상/과대/이동 중 재생/여러 컴포넌트 재생 검증과 실제 장치 청취 기록)과 P18-11, 재생 속도 확대(선택), `controls` 막대의 입력 처리(조정자 요청 참고).

조정자 요청:
- **FEATURE_MATRIX**: AUD-10 → "부분 구현(엔진 `media/playback/MediaPlayer` + `js/host/MediaElements`, 한계 1~10; 실제 장치 청취 확인 안 함)". HEI-08(`HTMLMediaElement HTMLAudioElement(Audio()) HTMLSourceElement HTMLVideoElement(EXC-01)`)은 "부분 구현(트랙/컨트롤러/속도 제외)", WEB-09 → 부분 구현, VID-01 의 DOM 부분(`canPlayType ""`, `error` 4, `play()` 거부) 검증 완료, VID-02 의 `<source>` 선택은 오디오 검증 완료(`MediaElementsTest`).
  구현 클래스: `media/playback/*`, `js/host/{MediaElements,MediaBindings,MediaRequestSource,MediaErrorValue,TimeRangesValue}`, `core/{PageResources.requestMedia,mediaState}`. 시험: `MediaPlayerTest`(17), `MediaElementsTest`(20), `AudioIntegrationTest`(6).
- **레이아웃/렌더(LAYOUT-D 종료 후, `layout/**`·`render/**` 는 건드리지 않았다)** — 렌더러에는 이미 `DisplayListBuilder.paintAudioElement`(controls 막대 `KIND_AUDIO`, 실패 시 `KIND_BROKEN` 으로 `UNSUPPORTED_URL`+alt)와 `layout/Replaced` 의 기본 크기 300x54 가 있고,
  **이제 `ImageSource.getMedia(url)` 이 실제 상태를 준다**(PENDING → 막대, AVAILABLE → 막대, FAILED → 미지원 URL+alt). 그래서 P18-06 의 오디오 실패 표시는 연결되었고 `AudioIntegrationTest.testRendererSeesThePlayableAndTheFailedAudio` 가 컴포넌트 수준에서 확인한다. 남은 요구:
  1. `audio` 는 `controls` 가 없으면 규격상 아무것도 그리지 않는다(`display:none` UA 기본값). 현재 `paintAudioElement` 는 `controls` 가 없고 실패하면 대체 그림을 그린다 — P18-06 요구("해당 위치에 미지원 URL 표시")에 맞다고 보고 유지했으나 `controls` 없는 정상 오디오가 자리를 차지하는지(레이아웃 크기 0 vs 300x54)는 렌더 담당 확인이 필요하다.
  2. **controls 막대의 입력 처리와 동적 그림**(재생/일시정지 버튼, 재생 위치 표시, 탐색 막대, 음량): 현재는 정적 `KIND_AUDIO` 그림이다. 필요한 인터페이스: 렌더러/입력 쪽이 요소의 상태(paused, currentTime, duration, volume, muted)를 읽고 동작을 부르는 훅. 제안: `HostEnvironment` 에 `MediaControls getMediaControls()` 같은 읽기 전용 상태 + `togglePlay(Element)`, `seek(Element,double)`, `setVolume(Element,double)` 을 두거나, 막대 클릭을 DOM `click` 이 아니라 `PageScripts.runTask` 로 `MediaElements.play/pause/setPosition` 을 부르게 한다
     (모두 문서 큐 스레드에서만 부를 것). 위치/재생 상태가 바뀔 때 다시 그리는 신호는 `timeupdate` 마다 `PageScripts.env.documentChanged()` 호출 또는 렌더러 쪽 타이머로 해결할 수 있다.
  3. `paintAudioElement` 가 `src`/첫 `source` 자식의 URL 만 본다. 이 구현은 같은 주소로 상태를 기록하므로 일관되지만, 렌더러가 선택된 후보(`currentSrc`)를 직접 알게 하려면 `ImageSource` 에 요소 단위 질의(`getMedia(Element)`)를 더하는 편이 정확하다.
  4. **미지원 비디오 문구(P18-09)**: 렌더러는 이미 미지원 비디오 안내를 그린다. DOM 쪽은 `error`(4)와 `networkState=3` 을 준다.
- **PROGRESS/AGENTS**: P18-08 은 "소리를 귀로 확인" 이 남아(실제 장치 청취는 하지 않았다) 체크하지 않는 것을 권한다. P18-05 상위 항목은 `picture` 등 이미지 쪽이 끝났고 `source` 오디오 선택(P18-05-c)이 이제 구현·검증되었으므로 P18-05-a/b/c 모두 완료 후보.
  P15-04-b-2(load 가 이미지/CSS/프레임 완료를 기다림) 중 미디어 부분: 요소가 `loadeddata` 까지(실패하면 즉시) load 이벤트를 늦춘다(`MediaElementsTest.testLoadEventsAndState`).
- **운영 메모**: 작업 중 FORMS 작업자가 부분 스테이징(`hash-object` + `update-index`)으로 커밋하는 바람에 작업 트리의 `ElementExtras.java` 에 그들이 커밋에서 뺀 줄(`textAccessor(HTMLOptionElement)`)이 남아 있었다. HEAD 와 같아지도록 작업 트리에서 그 줄을 지웠다.
  또한 FORMS 커밋 `6d7137c` 가 `DomInterfaces.java` 를 작업 트리째 커밋하여 내 `HTMLMediaElement` hunk 가 먼저 HEAD 에 들어갔다(그 커밋 시점의 HEAD 에서는 `muted` 접근자가 없는 상태였고 이번 커밋으로 맞춰진다).

체크박스 후보: 없음(위 한계 때문에 P18-08/P18-09 는 실제 장치 청취·`controls` 입력 처리 뒤에 판단). 구현·검증이 끝난 하위 근거: P18-05-c(오디오 `source` 선택: `type`/`media`/실패 후보/디코딩 실패 후보/기다림, `MediaElementsTest`),
P18-09 의 DOM 부분(video `canPlayType`/`error`/`play()` 거부/`videoWidth`).

## 회귀 결과

- 최종 광범위 회귀(JDK 6u45, 격리 스냅샷 = HEAD `add4c68` + 내 파일, `--filter org.duckdns.hjow.swingwebpanel.`, 패키지 js/core/media/render/layout): **1438 통과 / 실패 0 / 건너뜀 1**
  (건너뜀 1건은 `JarContentTest.testProductJarContainsOnlyProductFiles`, 제품 JAR 을 만들지 않아서). 이 중 내 시험은 `MediaPlayerTest` 17, `MediaElementsTest` 20, `AudioIntegrationTest` 6.
- 커밋 직전 HEAD `eae914c` 위에 내 파일을 덮은 스냅샷(`play6`)에서도 컴파일되고 `AudioIntegrationTest` 6, `MediaElementsTest` 20, `MediaPlayerTest` 17 이 통과했다.
- 이전 중간 실행(HEAD `be1b2c2` 기준) 1146 통과 / 실패 1(`DomBindingTest.testAttributesAndReflection`; FORMS 의 커밋 `141264f` 가 고친 기대값이며 미디어와 무관) / 건너뜀 1.
- 이 문서의 시험 수치는 `sh scripts/snaptest.sh play5 ...` 실행 기준이다. 스냅샷 컴파일/시험은 컴퓨터가 바쁠 때 첫 시험(실제 컴포넌트 첫 생성)이 20~30초 걸릴 수 있어 대기 상한을 30초로 두었다.
