# 웹 플랫폼 API 표면 점검 (P20-10, P20-12 입력 자료)

조정자가 2026-11-17 에 실제 페이지(`PageFixture`)에서 `typeof` 로 약 600개 이름의 존재를 조사한 결과를 MATRIX 작업자가 같은 날 다시 조사해 갱신했다(HTML Standard 의 Window/Document/Element/폼/미디어 멤버와 자주 쓰이는 인접 규격의 이름, 조사 클래스는 저장하지 않았다).
**"있다"는 것이 규격대로 동작한다는 뜻은 아니다.** 이 문서는 없는 것만 분류한다. 새 이름을 더하면 이 문서를 갱신한다.

분류: **구현**(구현함), **후보**(구현할 수 있고 가치가 있어 후속 작업 후보), **제외**(HTML5 2014 REC·CSS3·ES6·"DOM 이벤트" 목표 밖의 별도 규격이라 구현 대상이 아님, 근거를 적음), **예외**(AGENTS.md 의 명시적 미지원).

## 구현한 것

| 이름 | 위치 | 시험 |
| --- | --- | --- |
| `Element.insertAdjacentElement/insertAdjacentText`, `ParentNode.replaceChildren` | `dom.Element`, `dom.Node`, `js.dom.DomInterfaces` | `NodeApiTest`, `ElementStatesTest` |
| `HTMLMapElement.areas`, `HTMLDialogElement.show/showModal/close/returnValue`(+`close` 이벤트), `HTMLTrackElement.readyState/track`, `HTMLObjectElement.form/contentDocument/contentWindow/getSVGDocument/willValidate/checkValidity…`, `rowSpan` 기본값 1 | `js.host.ElementStates`, `js.dom.HtmlMoreInterfaces` | `ElementStatesTest` |
| `document.elementFromPoint`, `scrollingElement`, 창 스크롤(`scrollX/scrollY/pageXOffset/pageYOffset/scroll/scrollTo/scrollBy`), `Element.scrollIntoView/scroll*`, `matchMedia`, `resize`/`scroll` 이벤트 | `js.host.{WindowGlobals,ElementExtras,MediaQueries}` | `ScrollApiTest`, `MediaQueriesTest`, `PageRuntimeIntegrationTest` |
| `draggable`, `spellcheck`, `translate`, `contentEditable`, `isContentEditable`, `outerText`, `accessKeyLabel`, `inputMode`, `enterKeyHint`, `autocapitalize`, `nonce`, `inert`, `role`/`aria*`, `relList`, `referrerPolicy`, 이미지의 `loading/decoding/lowsrc/hspace/vspace/border/longDesc` | `js.host.ElementStates`, `js.dom.DomInterfaces` | `ElementStatesTest` |
| 핸들러 프로퍼티 이름(`onpointer*`, `oncopy/oncut/onpaste`, `onselectstart`, `ontouch*`, `ontoggle`, `onclose`, `oncancel`, 미디어 `onwaiting` 등) | `js.dom.EventHandlers` | 이름 등록만(발생은 각 기능) |
| `WebSocket`+`CloseEvent`(연결 거부로 실패, EXC-06), `Worker`/`SharedWorker`/`BroadcastChannel`/`ErrorEvent`, `window.length/frameElement/opener`, `status/defaultStatus`, `reportError`, `isSecureContext`, `origin`, `screen.avail*`, `requestIdleCallback`, `PageTransitionEvent`(`pageshow`) | `js.host.web.WebSocketApi`, `js.worker.*`, `js.host.FrameApi`, `js.host.WindowGlobals` | `WebSocketTest`, `js.worker.*`, `core.frames.*`, `WindowBasicsTest` |
| `DOMRect/DOMRectReadOnly/DOMRectList/DOMPoint*`, `Range.getBoundingClientRect/getClientRects`, CSSOM(`document.styleSheets`, `CSSStyleSheet`, `CSSRule` 계열, `document.fonts`, `FontFace`, `CSS.supports/escape`) | `js.host.RangeGeometry`, `js.host.Cssom*`, `js.host.CssFontApi` | `RangeRectsTest`, `Cssom*Test`, `CssomFontsTest` |
| **(MATRIX 작업자)** `console` 전체(`assert count dir group table time trace` 등)와 서식 문자열 | `js.builtins.ConsoleBuiltins` | `js.interp.ConsoleApiTest` 8 |
| **(MATRIX 작업자)** `window.event`, `external`, `moveTo/moveBy/resizeTo/resizeBy`, `locationbar` 등 막대, `document.elementsFromPoint/applets/clear/captureEvents/releaseEvents/fgColor 계열`, `Node.lookupNamespaceURI/lookupPrefix/isDefaultNamespace`, `Element.clientTop/clientLeft/offsetParent/scrollIntoViewIfNeeded/setAttributeNodeNS`, `window.applicationCache`, `navigator.taintEnabled/registerProtocolHandler 계열/yieldForStorageUpdates/webdriver`, `crypto.getRandomValues/randomUUID` | `js.host.LegacyWebApi`, `dom.NamespaceLookup`, `js.dom.DomBridge` | `js.host.LegacyWebApiTest` 7, `core.LegacyWebApiIntegrationTest` 3 |
| **(MATRIX 작업자)** 폐기된 IDL 의 반영 속성(표·행·셀의 `align/bgColor/vAlign/…`, `a.coords/rev/shape`, `body.text/link/…`, `br.clear`, `hr.*`, `script.event/htmlFor`, `link.charset/rev/target/sizes`, `meta.scheme/media`, `input.align/useMap`, `iframe.allow/loading/referrerPolicy`, `form.rel/relList`, `details.name`, `ul/ol.compact`, `legend.form`, `video.playsInline`, `img.fetchPriority` 등) | `js.dom.DomInterfaces`, `js.dom.FormBindings`, `js.dom.HtmlMoreInterfaces`, `js.host.ElementStates` | `LegacyWebApiTest.testObsoleteReflectedAttributes` |
| **(MATRIX 작업자)** 마이크로데이터: `document.getItems`, `itemScope/itemType/itemId/itemProp/itemRef/properties/itemValue`, `HTMLPropertiesCollection`, `PropertyNodeList` | `dom.Microdata`, `js.host.MicrodataApi` | `js.host.MicrodataTest` 5 |
| **(MATRIX 작업자)** `details`/`summary`(닫힌 내용 숨김, 클릭 토글, `toggle` 이벤트), 클라이언트 이미지 맵(`area` 클릭), `meta http-equiv=refresh`, 인라인 `<svg>` 그리기, `object` 대체 내용과 `embed`/`object` 안내 | `layout.BoxBuilder`, `dom.html.FormActivation`, `render.{ImageMaps,HitTester,InlineSvg}`, `js.host.MetaRefresh`, `layout.Replaced` | `core.DetailsMapRefreshIntegrationTest` 3, `MixedDocumentsIntegrationTest` 6 |

## 아직 없는 것

### Window / Navigator / Screen / Performance
| 이름 | 분류 | 비고 |
| --- | --- | --- |
| `Navigator`, `Location`, `History`, `Screen`, `Performance` 인터페이스 객체 이름 | 후보(작음) | 객체는 있지만 `instanceof Navigator` 가 안 된다 |
| `performance.mark/measure/getEntries*/timing/navigation/toJSON`, `PerformanceObserver` | 제외 | User Timing/Navigation Timing/Performance Timeline 은 별도 규격. `performance.now/timeOrigin` 만 있음 |
| `navigator.sendBeacon`, `doNotTrack`, `permissions`, `share`, `clipboard`, `geolocation`, `mediaDevices`, `serviceWorker`, `userAgentData`, `deviceMemory`, `connection` | 제외 | Beacon/Clipboard API/Geolocation/MediaDevices/Service Worker/UA Client Hints 는 별도 규격 |
| `IntersectionObserver`, `ResizeObserver` | 후보 | 별도 규격이지만 현대 페이지가 널리 쓴다. 레이아웃 상자 조회로 구현할 수 있다(문서 큐에서 배치 뒤 비교) |
| `EventSource` | **미구현**(설계) | W3C REC 2015-02-03. FEATURE_MATRIX 11절에 설계(`ResourceFetcher.fetch` 의 스트리밍 `ResponseHandler` 이용)를 적었다 |
| `customElements`, `attachShadow/shadowRoot/slot/assignedSlot`, `ShadowRoot`, `HTMLSlotElement` | 제외(범위 밖) | HTML5 2014 REC 이후의 DOM/HTML Living Standard. 렌더링 트리 합성이 필요한 대형 작업 |
| `indexedDB`, `caches`, `Notification`, `speechSynthesis`, `visualViewport`, `trustedTypes`, `Intl`, `WebAssembly`, `SharedArrayBuffer`, `Atomics`, `BigInt`, `crypto.subtle` | 제외 | Web Crypto 의 암호 연산/IndexedDB/Cache/Notifications/Web Speech/Visual Viewport/Trusted Types/ECMA-402/WebAssembly/ES2017~ES2020 |

### 이벤트와 인터페이스
| 이름 | 분류 | 비고 |
| --- | --- | --- |
| `pagehide`/`unload`/`beforeunload` 발생, `BeforeUnloadEvent`, window `focus/blur/online/offline/visibilitychange` 발생 | 후보 | 페이지를 떠날 때 스크립트에 알리는 경로가 없다(이동·clean·dispose 때 페이지를 폐기). `pageshow` 는 발생한다 |
| `PointerEvent` 와 `pointer*` 발생, `TouchEvent`, `DragEvent`/드래그 앤 드롭 | 후보(`pointer*`, 드래그), 제외(Touch: 터치 입력이 없는 Swing) | `core.PageInput` 의 마우스 전달에 병행해 `pointerdown/up/move/over/out/enter/leave` 를 발생시키는 설계가 필요(`pointerdown` 을 취소하면 호환 마우스 이벤트 생략) |
| `TextTrack/TextTrackCue/TextTrackList/VTTCue`, `audioTracks/textTracks/addTextTrack`, `srcObject`, `captureStream`, `setSinkId`, `controlsList`, `preservesPitch` | 부분(`track.track` 객체만) | WebVTT 가져오기와 큐 표시는 미구현(VID-02) |
| `ImageBitmap/createImageBitmap/OffscreenCanvas/DOMMatrix/DOMQuad`, `canvas.captureStream/transferControlToOffscreen` | 후보(P20-05-b) | |
| `MutationRecord`, `DOMStringMap`, `DOMStringList`, `HTMLAllCollection`, `XMLDocument`, `MathMLElement`, `FormDataEvent` 전역 이름 | 후보(작음) | 객체는 있으나 생성자/전역 이름이 없다 |

### Document와 Element
| 이름 | 분류 | 비고 |
| --- | --- | --- |
| `document.evaluate/createExpression/createNSResolver`, `XPathResult`, `XPathEvaluator` | 미구현 | DOM Level 3 XPath. XPath 1.0 해석기가 필요 |
| `caretRangeFromPoint`, `caretPositionFromPoint`, `rootElement`(SVG 문서) | 후보 | |
| `document.fullscreen*`, `exitFullscreen`, `pointerLockElement`, `timeline/getAnimations`, `startViewTransition`, `hasStorageAccess/requestStorageAccess`, `Element.requestFullscreen/requestPointerLock/set·release·hasPointerCapture/checkVisibility/popover/showPopover/animate/getAnimations/setHTMLUnsafe/getHTML/computedStyleMap` | 제외 | Fullscreen/Pointer Lock/Web Animations/View Transitions/Popover 등 별도 규격 |
| `HTMLObjectElement.validity`, `HTMLImageElement.x/y`, `input.showPicker`, `dialog.requestClose`, `script.supports`, `style.blocking`, SVG 요소의 `width/height/viewBox/getBBox` | 제외·후보 | HTML Living Standard 에만 있는 멤버와 SVG DOM(HEI-09) |
| `video.textTracks/audioTracks/videoTracks/mediaGroup/controller` 등 | 제외 | EXC-01 과 REC 4.8.10 의 삭제된 기능 |
