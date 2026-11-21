/**
 * The audio worklet of the Web Audio API (plan item P25-06; Web Audio API 1.0 1.32 and the HTML Worklets model): a script realm of its own,
 * {@link org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet.WorkletScope}, that has no DOM and no window, loads ES2015 modules through the request layer of the component
 * and runs the <code>process()</code> method of the <code>AudioWorkletProcessor</code> classes that the modules register, on the rendering thread of the audio context, through the
 * engine node {@link org.duckdns.hjow.swingwebpanel.media.webaudio.AudioWorkletNode}. The page side (<code>AudioWorklet</code>, <code>AudioWorkletNode</code>) is in
 * {@code org.duckdns.hjow.swingwebpanel.js.host.webaudio}.
 * <p>
 * Web Audio API 의 오디오 worklet(계획 P25-06. Web Audio API 1.0 1.32 와 HTML Worklets 모델): DOM 도 window 도 없는 자기만의 스크립트 realm
 * {@link org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet.WorkletScope}. 컴포넌트의 요청 계층으로 ES2015 모듈을 불러오고, 모듈이 등록한 <code>AudioWorkletProcessor</code> 클래스의
 * <code>process()</code> 메서드를 엔진 노드 {@link org.duckdns.hjow.swingwebpanel.media.webaudio.AudioWorkletNode} 를 통해 오디오 컨텍스트의 렌더링 스레드에서 실행한다. 페이지 쪽
 * (<code>AudioWorklet</code>, <code>AudioWorkletNode</code>)은 {@code org.duckdns.hjow.swingwebpanel.js.host.webaudio} 에 있다.
 */
package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;
