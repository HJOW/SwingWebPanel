package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.media.webaudio.WebAudioLimits;

/**
 * The limits that the script interface of one page keeps, on top of the limits of the engine ({@link WebAudioLimits}, which bound one context): how many
 * contexts a page may have open at the same time and how much memory its offline contexts may hold together. A script that exceeds one gets a
 * <code>NotSupportedError</code>, the error that the specification names for "an implementation-defined maximum number of contexts".
 * <p>
 * Reasons. A real-time context owns a rendering thread and (once an output device is used) an audio line, which are scarce, and the browsers that limited the number
 * of contexts of a page also chose 6 (Chromium, for hardware contexts, in the versions that had a limit); a page that needs more than one context is rare, a page that
 * leaks contexts by creating one per sound without closing it is not, and the limit turns that into an error that the developer sees instead of a growing number of
 * threads. An offline context allocates its whole result when it is created (the length times the channels), so the sum is bounded by the largest buffer that the engine
 * allows ({@link WebAudioLimits#MAX_BUFFER_SAMPLES}); a context gives its share back when its rendering has finished or the context is disposed.
 * <p>
 * 엔진의 한도({@link WebAudioLimits}. 컨텍스트 하나를 제한한다)에 더해 한 페이지의 스크립트 인터페이스가 지키는 한도: 페이지가 동시에 열어 둘 수 있는 컨텍스트 수와
 * 오프라인 컨텍스트가 함께 쥘 수 있는 메모리. 한도를 넘는 스크립트는 <code>NotSupportedError</code> 를 받는다. 규격이 "구현이 정하는 컨텍스트 최대 수"에 대해
 * 정한 오류이다.
 * <p>
 * 이유. 실시간 컨텍스트는 렌더링 스레드와(출력 장치를 쓰게 되면) 오디오 라인을 가지며 이들은 귀하다. 페이지의 컨텍스트 수를 제한했던 브라우저도 6 을 택했다(Chromium 의 하드웨어
 * 컨텍스트, 한도가 있던 판). 컨텍스트가 둘 이상 필요한 페이지는 드물지만, 소리마다 하나를 만들고 닫지 않아 컨텍스트가 새는 페이지는 드물지 않고, 한도가 그것을 스레드가 늘어나는
 * 것 대신 개발자가 보는 오류로 바꾼다. 오프라인 컨텍스트는 만들 때 결과 전체(길이 곱하기 채널)를 할당하므로 합계는 엔진이 허용하는 가장 큰 버퍼
 * ({@link WebAudioLimits#MAX_BUFFER_SAMPLES})로 제한한다. 컨텍스트는 렌더링이 끝나거나 폐기되면 자기 몫을 돌려준다.
 */
public final class WebAudioHostLimits {

    /**
     * The largest number of real-time contexts (<code>AudioContext</code>) that a page has open (not closed) at the same time.
     * <p>
     * 페이지가 동시에 열어 둘(닫지 않은) 실시간 컨텍스트(<code>AudioContext</code>)의 최대 수.
     */
    public static final int MAX_REALTIME_CONTEXTS = 6;

    /**
     * The largest number of offline contexts (<code>OfflineAudioContext</code>) of a page that have not finished their rendering.
     * <p>
     * 렌더링을 끝내지 않은 페이지의 오프라인 컨텍스트(<code>OfflineAudioContext</code>)의 최대 수.
     */
    public static final int MAX_OFFLINE_CONTEXTS = 16;

    /**
     * The largest number of samples (all channels) that the offline contexts of a page may hold together.
     * <p>
     * 페이지의 오프라인 컨텍스트가 함께 쥘 수 있는 표본(모든 채널)의 최대 수.
     */
    public static final long MAX_OFFLINE_SAMPLES = WebAudioLimits.MAX_BUFFER_SAMPLES;

    /**
     * Private constructor: constants only.
     * <p>
     * 비공개 생성자: 상수만 있다.
     */
    private WebAudioHostLimits() {
    }
}
