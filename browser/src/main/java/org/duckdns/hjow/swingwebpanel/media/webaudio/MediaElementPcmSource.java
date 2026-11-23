package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The supplier of the audio of a media element (an <code>&lt;audio&gt;</code> element of the page) for a
 * {@link MediaElementAudioSourceNode}. The pure Java audio engine knows nothing about HTML elements and players: the host (the binding
 * of the page, stage C of the plan) implements this interface on top of the player of the element, and the node pulls the decoded
 * samples of the playing media from it, one render quantum at a time, on the rendering thread.
 * <p>
 * Contract: the methods are called from the rendering thread and must return quickly without blocking (no waiting for the network, the
 * decoder or a lock that a slow thread may hold). The media keeps playing with the same timing as without the node; only its output is
 * taken from the speakers to the graph. A video element is not a supported origin (the factory of the host refuses it).
 * <p>
 * 미디어 요소(페이지의 <code>&lt;audio&gt;</code> 요소)의 오디오를 {@link MediaElementAudioSourceNode} 에 공급하는 쪽. 순수 Java 오디오 엔진은 HTML 요소와
 * 재생기를 모른다. 호스트(페이지 바인딩, 계획의 C 단계)가 요소의 재생기 위에서 이 인터페이스를 구현하고, 노드가 렌더링 스레드에서 render quantum 하나씩
 * 재생 중인 미디어의 디코딩된 샘플을 가져간다.
 * <p>
 * 계약: 메서드는 렌더링 스레드에서 불리며 막히지 않고 빠르게 반환해야 한다(네트워크, 디코더, 느린 스레드가 쥘 수 있는 잠금을 기다리지 않는다). 미디어는 노드가
 * 없을 때와 같은 타이밍으로 계속 재생되며 출력만 스피커에서 그래프로 옮겨진다. 비디오 요소는 지원하는 원천이 아니다(호스트의 팩터리가 거부한다).
 */
public interface MediaElementPcmSource {

    /**
     * Returns the number of channels of the media (1 to 32), or 0 if the media has no audio (the node then outputs one silent channel).
     * <p>
     * 미디어의 채널 수(1 ~ 32)를 반환한다. 미디어에 오디오가 없으면 0 이며 그때 노드는 무음 채널 하나를 출력한다.
     *
     * @return the number of channels / 채널 수
     */
    int getChannelCount();

    /**
     * Returns the sample rate of the samples that {@link #pull} delivers, in Hz. If it differs from the sample rate of the context the
     * node resamples (with a linear interpolation).
     * <p>
     * {@link #pull} 이 주는 샘플의 샘플링 주파수(Hz)를 반환한다. 컨텍스트의 샘플링 주파수와 다르면 노드가 (선형 보간으로) 리샘플링한다.
     *
     * @return the sample rate / 샘플링 주파수
     */
    float getSampleRate();

    /**
     * Tells whether the audio may be heard through the graph. It is false for media from another origin that was not fetched with
     * the permission of the server (CORS): the specification makes the node output silence then.
     * <p>
     * 오디오를 그래프로 들려줘도 되는지 알려 준다. 서버의 허가(CORS) 없이 가져온 다른 오리진의 미디어이면 false 이며, 그러면 규격에 따라 노드가 무음을
     * 출력한다.
     *
     * @return true if the audio may pass / 오디오가 지나가도 되면 true
     */
    boolean isOutputAllowed();

    /**
     * Pulls the next samples of the playing media. The method fills the first frames of <code>dest[c]</code> for every channel
     * <code>c</code> below {@link #getChannelCount()} and returns how many frames it delivered: fewer than requested (down to 0) while
     * the media is paused, has ended, is buffering or has no data yet. The node fills the rest with silence.
     * <p>
     * 재생 중인 미디어의 다음 샘플을 가져온다. {@link #getChannelCount()} 보다 작은 모든 채널 <code>c</code> 에 대해 <code>dest[c]</code> 의 앞쪽 프레임을
     * 채우고 몇 프레임을 주었는지 반환한다. 미디어가 일시 정지, 종료, 버퍼링 중이거나 아직 데이터가 없으면 요청보다 적게(0 까지) 준다. 나머지는 노드가 무음으로
     * 채운다.
     *
     * @param dest   the destination arrays, one per channel, each with room for at least <code>frames</code> frames / 대상 배열. 채널마다 하나이고 각각
     *               <code>frames</code> 프레임 이상이 들어간다
     * @param frames the number of frames that are wanted / 원하는 프레임 수
     * @return the number of frames delivered, from 0 to <code>frames</code> / 준 프레임 수(0 ~ <code>frames</code>)
     */
    int pull(float[][] dest, int frames);
}
