/**
 * The script interface of the Web Audio API (plan item P25): <code>AudioContext</code>, <code>OfflineAudioContext</code>, <code>AudioNode</code> and the nodes,
 * <code>AudioParam</code>, <code>AudioBuffer</code>, <code>AudioListener</code>, <code>PeriodicWave</code> and the events, over the pure Java engine
 * {@code org.duckdns.hjow.swingwebpanel.media.webaudio}. {@link org.duckdns.hjow.swingwebpanel.js.host.webaudio.WebAudioApi} installs it on the global object of a page
 * and disposes everything with the page. A real-time context plays to the output that the host chose ({@code SwingWebPanel.createWebAudioOutputFactory()}), the deprecated
 * <code>ScriptProcessorNode</code> runs its handlers as tasks of the page, and <code>createMediaElementSource</code> routes the sound of an <code>audio</code> element into the graph.
 * <p>
 * Web Audio API 의 스크립트 인터페이스(계획 P25): <code>AudioContext</code>, <code>OfflineAudioContext</code>, <code>AudioNode</code> 와 노드, <code>AudioParam</code>,
 * <code>AudioBuffer</code>, <code>AudioListener</code>, <code>PeriodicWave</code>, 이벤트를 순수 Java 엔진 {@code org.duckdns.hjow.swingwebpanel.media.webaudio} 위에 만든다.
 * {@link org.duckdns.hjow.swingwebpanel.js.host.webaudio.WebAudioApi} 가 페이지의 전역 객체에 설치하고 페이지와 함께 모든 것을 폐기한다. 실시간 컨텍스트는 호스트가 고른 출력
 * ({@code SwingWebPanel.createWebAudioOutputFactory()})으로 재생하고, 폐기 예정인 <code>ScriptProcessorNode</code> 는 핸들러를 페이지의 작업으로 실행하며,
 * <code>createMediaElementSource</code> 는 <code>audio</code> 요소의 소리를 그래프로 돌린다.
 */
package org.duckdns.hjow.swingwebpanel.js.host.webaudio;
