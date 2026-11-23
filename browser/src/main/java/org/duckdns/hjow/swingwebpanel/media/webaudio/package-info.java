/**
 * The audio graph engine of the Web Audio API (plan items P25-01 to P25-04): the nodes, their connections, the parameters with their
 * automation timeline, the channel mixing rules and the rendering loop that processes one render quantum at a time. The package is pure
 * Java: it knows nothing about JavaScript, the DOM or Swing, so the script bindings (<code>js.host.webaudio</code>) are a thin layer on top
 * of it. Control methods may be called from any thread; they validate at once and the rendering thread applies the change at the next
 * render quantum boundary. {@link org.duckdns.hjow.swingwebpanel.media.webaudio.OfflineAudioContext} renders on the calling thread.
 * <p>
 * Web Audio API 의 오디오 그래프 엔진(계획 P25-01~P25-04): 노드, 연결, 자동화 타임라인을 가진 파라미터, 채널 믹스 규칙, render quantum 한 개씩
 * 처리하는 렌더링 루프. 이 패키지는 순수 Java 이며 JavaScript, DOM, Swing 을 모르므로 스크립트 바인딩(<code>js.host.webaudio</code>)은 이 위의
 * 얇은 층이다. 제어 메서드는 어느 스레드에서나 부를 수 있고 즉시 검증하며, 렌더링 스레드가 다음 render quantum 경계에서 변경을 적용한다.
 * {@link org.duckdns.hjow.swingwebpanel.media.webaudio.OfflineAudioContext} 는 호출한 스레드에서 렌더링한다.
 */
package org.duckdns.hjow.swingwebpanel.media.webaudio;
