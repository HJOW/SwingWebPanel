package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the channel splitter and the channel merger (Web Audio API 1.14 and 1.15, plan item P25-04): the channels are separated and
 * joined in order, outputs without a channel are silent and not active, an input of the merger that has no connection is a silent channel
 * that keeps its place, every input of the merger is mixed down to one channel, the output of the merger is one silent channel while no
 * input is active, and a splitter followed by a merger with the connections crossed swaps the channels.
 * <p>
 * 채널 스플리터와 채널 머저 시험(Web Audio API 1.14와 1.15, 계획 P25-04): 채널은 순서대로 분리되고 합쳐지며, 채널이 없는 출력은 무음이고 활성이 아니고, 연결이 없는
 * 머저 입력은 자리를 지키는 무음 채널이며, 머저의 모든 입력은 채널 하나로 다운믹스되고, 입력이 활성이 아닌 동안 머저의 출력은 무음 채널 하나이며, 연결을 엇갈리게
 * 한 스플리터와 머저는 채널을 맞바꾼다.
 */
public class ChannelSplitMergeTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ChannelSplitMergeTest() {
    }

    /**
     * Creates a stereo buffer with a constant on each channel.
     * <p>
     * 채널마다 상수가 든 스테레오 버퍼를 만든다.
     *
     * @param left  the value of channel 0 / 채널 0 의 값
     * @param right the value of channel 1 / 채널 1 의 값
     * @param n     the length / 길이
     * @return the buffer / 버퍼
     */
    private static AudioBuffer stereo(float left, float right, int n) {
        AudioBuffer b = new AudioBuffer(2, n, 8000f);
        java.util.Arrays.fill(b.getChannelData(0), left);
        java.util.Arrays.fill(b.getChannelData(1), right);
        return b;
    }

    /**
     * Splitting a stereo signal and merging the two outputs in the opposite order swaps the channels.
     * <p>
     * 스테레오 신호를 나눠 두 출력을 반대 순서로 합치면 채널이 바뀐다.
     */
    public void testSwapChannels() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 256, 8000f);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(stereo(0.25f, -0.5f, 256));
        ChannelSplitterNode split = ctx.createChannelSplitter(2);
        ChannelMergerNode merge = ctx.createChannelMerger(2);
        src.connect(split);
        split.connect(merge, 0, 1);
        split.connect(merge, 1, 0);
        merge.connect(ctx.getDestination());
        src.start(0);
        AudioBuffer out = ctx.startRendering();
        Check.near(-0.5, out.getChannelData(0)[100], 0, "the right input is now on the left");
        Check.near(0.25, out.getChannelData(1)[100], 0, "the left input is now on the right");
    }

    /**
     * The outputs of a splitter beyond the channels of the input are silent and not active; a splitter with six outputs on a stereo
     * input has two live outputs.
     * <p>
     * 입력의 채널보다 많은 스플리터 출력은 무음이고 활성이 아니다. 스테레오 입력에 출력이 여섯인 스플리터는 살아 있는 출력이 둘이다.
     */
    public void testUnusedSplitterOutputs() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 128, 8000f);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(stereo(0.25f, -0.5f, 128));
        ChannelSplitterNode split = ctx.createChannelSplitter(6);
        Check.equal(6, split.getNumberOfOutputs(), "six outputs");
        Check.equal(1, split.getNumberOfInputs(), "one input");
        src.connect(split);
        split.connect(ctx.getDestination(), 0, 0);
        src.start(0);
        ctx.renderNextQuantum();
        // 입력의 채널 수는 explicit 모드의 channelCount(출력 수 6)이므로 입력에는 채널이 6개 있고 둘째 채널 뒤는 무음이다.
        Check.equal(6, split.inputs[0].bus.getNumberOfChannels(), "the input has channelCount channels");
        Check.near(0.25, split.outputs[0].bus.channel(0)[7], 0, "output 0");
        Check.near(-0.5, split.outputs[1].bus.channel(0)[7], 0, "output 1");
        Check.near(0.0, split.outputs[2].bus.channel(0)[7], 0, "output 2 is silent");
        Check.near(0.0, split.outputs[5].bus.channel(0)[7], 0, "output 5 is silent");
        Check.equal(1, split.outputs[0].bus.getNumberOfChannels(), "every output has one channel");
    }

    /**
     * An input of the merger without a connection is a silent channel that keeps its place; the output has as many channels as the
     * merger has inputs; a stereo input is down-mixed to one channel.
     * <p>
     * 연결이 없는 머저 입력은 자리를 지키는 무음 채널이다. 출력의 채널 수는 머저의 입력 수와 같다. 스테레오 입력은 채널 하나로 다운믹스된다.
     */
    public void testMergerInputs() {
        OfflineAudioContext ctx = new OfflineAudioContext(4, 128, 8000f);
        ChannelMergerNode merge = ctx.createChannelMerger(4);
        Check.equal(4, merge.getNumberOfInputs(), "four inputs");
        Check.equal(1, merge.getNumberOfOutputs(), "one output");
        AudioBufferSourceNode a = ctx.createBufferSource();
        a.setBuffer(stereo(0.5f, 0.25f, 128));
        AudioBufferSourceNode b = ctx.createBufferSource();
        AudioBuffer mono = new AudioBuffer(1, 128, 8000f);
        java.util.Arrays.fill(mono.getChannelData(0), -0.5f);
        b.setBuffer(mono);
        a.connect(merge, 0, 1);
        b.connect(merge, 0, 3);
        merge.connect(ctx.getDestination());
        a.start(0);
        b.start(0);
        ctx.renderNextQuantum();
        Check.equal(4, merge.outputs[0].bus.getNumberOfChannels(), "four channels");
        Check.near(0.0, merge.outputs[0].bus.channel(0)[5], 0, "input 0 is not connected: a silent channel");
        Check.near(0.375, merge.outputs[0].bus.channel(1)[5], 1e-7, "input 1: the stereo input mixed down to the mean");
        Check.near(0.0, merge.outputs[0].bus.channel(2)[5], 0, "input 2 is not connected");
        Check.near(-0.5, merge.outputs[0].bus.channel(3)[5], 0, "input 3");
        Check.equal(1, merge.inputs[1].bus.getNumberOfChannels(), "each input has one channel");
    }

    /**
     * While no input of the merger is active its output is a single channel of silence.
     * <p>
     * 머저의 입력이 하나도 활성이 아닌 동안 그 출력은 무음 채널 하나이다.
     */
    public void testMergerWithoutActiveInput() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 384, 8000f);
        AudioBufferSourceNode a = ctx.createBufferSource();
        a.setBuffer(stereo(0.5f, 0.5f, 100));
        ChannelMergerNode merge = ctx.createChannelMerger(3);
        a.connect(merge, 0, 2);
        merge.connect(ctx.getDestination());
        a.start(0);
        ctx.renderNextQuantum();
        Check.equal(3, merge.outputs[0].bus.getNumberOfChannels(), "three channels while the source plays");
        ctx.renderNextQuantum();
        ctx.renderNextQuantum();
        Check.equal(1, merge.outputs[0].bus.getNumberOfChannels(), "one silent channel afterwards");
        Check.isFalse(merge.outputs[0].active, "and the output is not active");
    }

    /**
     * Gains between a splitter and a merger give independent control of every channel (matrix mixing).
     * <p>
     * 스플리터와 머저 사이의 게인은 채널마다 독립적인 제어를 준다(행렬 믹싱).
     */
    public void testMatrixMixing() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 128, 8000f);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(stereo(0.5f, 0.5f, 128));
        ChannelSplitterNode split = ctx.createChannelSplitter(2);
        ChannelMergerNode merge = ctx.createChannelMerger(2);
        GainNode g0 = ctx.createGain();
        GainNode g1 = ctx.createGain();
        g0.getGain().setValue(2f);
        g1.getGain().setValue(-1f);
        src.connect(split);
        split.connect(g0, 0, 0);
        split.connect(g1, 1, 0);
        g0.connect(merge, 0, 0);
        g1.connect(merge, 0, 1);
        merge.connect(ctx.getDestination());
        src.start(0);
        AudioBuffer out = ctx.startRendering();
        Check.near(1.0, out.getChannelData(0)[20], 1e-7, "left doubled");
        Check.near(-0.5, out.getChannelData(1)[20], 1e-7, "right inverted");
    }
}
