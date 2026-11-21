package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;

/**
 * An output of an audio node: the bus that the node fills for every render quantum and the connections to the inputs and the parameters
 * that read it (rendering thread only; the control thread works with the links of the node and the control messages change these lists
 * at a render quantum boundary).
 * <p>
 * 오디오 노드의 출력: 노드가 render quantum 마다 채우는 버스와 그것을 읽는 입력과 파라미터로의 연결(렌더링 스레드 전용. 제어 스레드는 노드의 링크를
 * 다루고, 제어 메시지가 render quantum 경계에서 이 목록을 바꾼다).
 */
final class AudioNodeOutput {

    /**
     * The node that owns the output.
     * <p>
     * 출력을 가진 노드.
     */
    final AudioNode node;

    /**
     * The index of the output in the node.
     * <p>
     * 노드 안에서의 출력 인덱스.
     */
    final int index;

    /**
     * The audio of the last rendered quantum.
     * <p>
     * 마지막으로 렌더링한 quantum 의 오디오.
     */
    final AudioBus bus;

    /**
     * The render serial of the quantum in which the bus was produced; a reader ignores an output whose stamp is not the current serial.
     * <p>
     * 버스를 만든 quantum 의 렌더링 일련번호. 읽는 쪽은 도장이 현재 일련번호가 아닌 출력을 무시한다.
     */
    long stamp = -1;

    /**
     * True if the node was actively processing in the quantum; an inactive node produces a single channel of silence.
     * <p>
     * 노드가 그 quantum 에서 활성으로 처리했으면 true. 활성이 아닌 노드는 무음 채널 하나를 만든다.
     */
    boolean active;

    /**
     * The inputs that read this output.
     * <p>
     * 이 출력을 읽는 입력.
     */
    final ArrayList<AudioNodeInput> inputTargets = new ArrayList<AudioNodeInput>();

    /**
     * The parameters that read this output.
     * <p>
     * 이 출력을 읽는 파라미터.
     */
    final ArrayList<AudioParam> paramTargets = new ArrayList<AudioParam>();

    /**
     * Creates an output with a silent single channel bus.
     * <p>
     * 무음 한 채널 버스를 가진 출력을 만든다.
     *
     * @param node   the owning node / 가진 노드
     * @param index  the index / 인덱스
     * @param frames the render quantum size / render quantum 크기
     */
    AudioNodeOutput(AudioNode node, int index, int frames) {
        this.node = node;
        this.index = index;
        this.bus = new AudioBus(1, frames);
    }
}
