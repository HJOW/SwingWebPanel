package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;

/**
 * An input of an audio node: the list of outputs that are connected to it and the bus into which they are mixed (rendering thread only).
 * Before a node processes, the input computes the number of channels from the connections and the <code>channelCountMode</code> /
 * <code>channelCount</code> of the node, up-mixes or down-mixes every active connection with the <code>channelInterpretation</code>, and
 * adds the connections together (Web Audio API section 4). An input without a connection has one silent channel, or
 * <code>channelCount</code> silent channels in the <code>explicit</code> mode.
 * <p>
 * 오디오 노드의 입력: 연결된 출력의 목록과 그것들을 섞는 버스(렌더링 스레드 전용). 노드가 처리하기 전에 입력은 연결과 노드의
 * <code>channelCountMode</code> / <code>channelCount</code> 로 채널 수를 계산하고, 활성인 연결마다 <code>channelInterpretation</code> 으로 업믹스 또는
 * 다운믹스한 뒤 합친다(Web Audio API 4절). 연결이 없는 입력은 무음 채널 한 개이며, <code>explicit</code> 모드에서는 <code>channelCount</code> 개이다.
 */
final class AudioNodeInput {

    /**
     * The node that owns the input.
     * <p>
     * 입력을 가진 노드.
     */
    final AudioNode node;

    /**
     * The index of the input in the node.
     * <p>
     * 노드 안에서의 입력 인덱스.
     */
    final int index;

    /**
     * The mixed audio.
     * <p>
     * 섞은 오디오.
     */
    final AudioBus bus;

    /**
     * The outputs connected to this input.
     * <p>
     * 이 입력에 연결된 출력.
     */
    final ArrayList<AudioNodeOutput> sources = new ArrayList<AudioNodeOutput>();

    /**
     * True if at least one connected output was actively processing in the current quantum.
     * <p>
     * 연결된 출력 중 하나라도 현재 quantum 에 활성으로 처리했으면 true.
     */
    boolean anyActive;

    /**
     * Creates an input with a silent single channel bus.
     * <p>
     * 무음 한 채널 버스를 가진 입력을 만든다.
     *
     * @param node   the owning node / 가진 노드
     * @param index  the index / 인덱스
     * @param frames the render quantum size / render quantum 크기
     */
    AudioNodeInput(AudioNode node, int index, int frames) {
        this.node = node;
        this.index = index;
        this.bus = new AudioBus(1, frames);
    }

    /**
     * Computes the number of channels of the input (<code>computedNumberOfChannels</code>).
     * <p>
     * 입력의 채널 수(<code>computedNumberOfChannels</code>)를 계산한다.
     *
     * @param serial the render serial of the current quantum / 현재 quantum 의 렌더링 일련번호
     * @return the number of channels / 채널 수
     */
    private int computeChannels(long serial) {
        ChannelCountMode mode = node.getChannelCountMode();
        int count = node.getChannelCount();
        if (mode == ChannelCountMode.EXPLICIT) {
            return count;
        }
        int max = 1;
        // 이번 quantum 에 만들어진 연결 출력 중 채널 수가 가장 많은 것을 찾는다.
        for (int i = 0; i < sources.size(); i++) {
            AudioNodeOutput src = sources.get(i);
            if (src.stamp == serial) {
                max = Math.max(max, src.bus.getNumberOfChannels());
            }
        }
        return mode == ChannelCountMode.CLAMPED_MAX ? Math.min(max, count) : max;
    }

    /**
     * Mixes the active connections into the bus.
     * <p>
     * 활성인 연결을 버스로 섞는다.
     *
     * @param serial the render serial of the current quantum / 현재 quantum 의 렌더링 일련번호
     */
    void pull(long serial) {
        bus.setNumberOfChannels(computeChannels(serial));
        bus.zero();
        anyActive = false;
        ChannelInterpretation interpretation = node.getChannelInterpretation();
        // 활성인 연결만 더한다. 비활성 출력은 무음이므로 건너뛴다.
        for (int i = 0; i < sources.size(); i++) {
            AudioNodeOutput src = sources.get(i);
            if (src.stamp == serial && src.active) {
                anyActive = true;
                ChannelMixing.mix(src.bus, bus, interpretation, true);
            }
        }
    }
}
