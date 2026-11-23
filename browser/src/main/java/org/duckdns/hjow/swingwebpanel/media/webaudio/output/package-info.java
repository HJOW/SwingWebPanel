/**
 * The sound output of a real-time Web Audio context (plan item P25-07): the {@link org.duckdns.hjow.swingwebpanel.media.webaudio.output.AudioOutput}
 * that the rendering thread writes float audio to, the {@link org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory} that opens
 * it, and two implementations: {@link org.duckdns.hjow.swingwebpanel.media.webaudio.output.JavaSoundOutput} (a Java Sound line, public API
 * <code>javax.sound.sampled</code> only) and {@link org.duckdns.hjow.swingwebpanel.media.webaudio.output.NullOutput} (no device: the sound
 * is discarded but the writes are paced to wall-clock time, so the context clock runs at real speed). The package is pure Java and knows
 * nothing about the graph; the OutputRenderDriver in the parent package owns the rendering thread.
 * <p>
 * 실시간 Web Audio 컨텍스트의 소리 출력(계획 P25-07): 렌더링 스레드가 float 오디오를 쓰는
 * {@link org.duckdns.hjow.swingwebpanel.media.webaudio.output.AudioOutput}, 그것을 여는
 * {@link org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory}, 그리고 구현 둘:
 * {@link org.duckdns.hjow.swingwebpanel.media.webaudio.output.JavaSoundOutput}(Java Sound 라인, 공개 API <code>javax.sound.sampled</code> 만 사용)과
 * {@link org.duckdns.hjow.swingwebpanel.media.webaudio.output.NullOutput}(장치 없음: 소리는 버리지만 쓰기를 벽시계 속도로 늦추므로 컨텍스트 시계가
 * 실제 속도로 진행한다). 이 패키지는 순수 Java 이며 그래프를 모른다. 렌더링 스레드는 상위 패키지의 OutputRenderDriver 가 가진다.
 */
package org.duckdns.hjow.swingwebpanel.media.webaudio.output;
