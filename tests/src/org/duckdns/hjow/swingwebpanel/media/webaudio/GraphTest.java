package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the audio graph and its rendering loop (Web Audio API 1.5 and 2, plan item P25-02): <code>connect</code> and the
 * <code>disconnect</code> forms with their errors, fan-in and fan-out, the number of channels of an input for the three modes, parameter
 * inputs, the rendering order of a long chain, cycles with and without a delay node, the application of control messages at the render
 * quantum boundary, the lifetime of started sources and the <code>ended</code> notification. Every expected signal is computed in the test
 * from the closed form of the graph.
 * <p>
 * 오디오 그래프와 렌더링 루프의 시험(Web Audio API 1.5와 2절, 계획 P25-02): <code>connect</code> 와 <code>disconnect</code> 의 형태와 오류, 팬인과 팬아웃,
 * 세 모드에서의 입력 채널 수, 파라미터 입력, 긴 사슬의 렌더링 순서, 지연 노드가 있는 순환과 없는 순환, render quantum 경계에서의 제어 메시지 적용, 시작한
 * 원천의 수명과 <code>ended</code> 통지. 기대 신호는 모두 시험이 그래프의 닫힌 식에서 계산한다.
 */
public class GraphTest {

    /**
     * The quantum size.
     * <p>
     * quantum 크기.
     */
    private static final int Q = 128;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public GraphTest() {
    }

    /**
     * Creates a constant source with an offset that is started at 0 and connected to a node.
     * <p>
     * 오프셋이 있는 상수 원천을 만들어 0 에 시작하고 노드에 연결한다.
     *
     * @param ctx    the context / 컨텍스트
     * @param value  the offset / 오프셋
     * @param target the node to connect to / 연결할 노드
     * @return the source / 원천
     */
    private static ConstantSourceNode constant(OfflineAudioContext ctx, float value, AudioNode target) {
        ConstantSourceNode c = ctx.createConstantSource();
        c.getOffset().setValue(value);
        c.connect(target);
        c.start(0);
        return c;
    }

    /**
     * A started source connected to the destination renders its signal; the context time advances by one quantum per quantum.
     * <p>
     * 시작한 원천을 목적지에 연결하면 그 신호가 렌더링된다. 컨텍스트 시간은 quantum 마다 한 quantum 씩 나아간다.
     */
    public void testSourceToDestination() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(Q * 3);
        constant(ctx, 0.5f, ctx.getDestination());
        Check.near(0.0, ctx.getCurrentTime(), 0, "time before rendering");
        float[] out = WebAudioTestKit.render(ctx);
        // 모든 프레임이 0.5 이다.
        for (int i = 0; i < out.length; i++) {
            Check.near(0.5, out[i], 0, "frame " + i);
        }
        Check.near(3.0 * Q / WebAudioTestKit.SR, ctx.getCurrentTime(), 1e-12, "time after 3 quanta");
        Check.equal(ContextState.CLOSED, ctx.getState(), "state after the rendering");
    }

    /**
     * Fan-in sums the connections of an input; fan-out gives every destination the same signal; a second connection with the same ends is
     * ignored.
     * <p>
     * 팬인은 입력의 연결을 합하고, 팬아웃은 모든 대상에 같은 신호를 주며, 끝이 같은 두 번째 연결은 무시된다.
     */
    public void testFanInFanOutAndDuplicates() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(Q);
        GainNode sum = ctx.createGain();
        sum.connect(ctx.getDestination());
        constant(ctx, 0.25f, sum);
        ConstantSourceNode b = constant(ctx, 0.5f, sum);
        b.connect(sum);
        b.connect(sum, 0, 0);
        float[] out = WebAudioTestKit.render(ctx);
        Check.near(0.75, out[0], 1e-7, "fan-in sum, duplicates ignored");
        Check.near(0.75, out[Q - 1], 1e-7, "fan-in sum at the end");
        OfflineAudioContext c2 = new OfflineAudioContext(2, Q, WebAudioTestKit.SR);
        ConstantSourceNode src = c2.createConstantSource();
        src.getOffset().setValue(0.5f);
        ChannelMergerNode merger = c2.createChannelMerger(2);
        GainNode left = c2.createGain();
        GainNode right = c2.createGain();
        right.getGain().setValue(2f);
        src.connect(left);
        src.connect(right);
        left.connect(merger, 0, 0);
        right.connect(merger, 0, 1);
        merger.connect(c2.getDestination());
        src.start(0);
        AudioBuffer r = c2.startRendering();
        Check.near(0.5, r.getChannelData(0)[10], 1e-7, "fan-out, first destination");
        Check.near(1.0, r.getChannelData(1)[10], 1e-7, "fan-out, second destination with gain 2");
    }

    /**
     * Nodes of other contexts, indexes out of range and null arguments are rejected.
     * <p>
     * 다른 컨텍스트의 노드, 범위를 벗어난 인덱스, null 인수는 거부된다.
     */
    public void testConnectErrors() {
        final OfflineAudioContext a = WebAudioTestKit.mono(Q);
        final OfflineAudioContext b = WebAudioTestKit.mono(Q);
        final GainNode ga = a.createGain();
        final GainNode gb = b.createGain();
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_ACCESS, new Runnable() {
            public void run() {
                ga.connect(gb);
            }
        }, "node of another context");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_ACCESS, new Runnable() {
            public void run() {
                ga.connect(gb.getGain(), 0);
            }
        }, "parameter of another context");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                ga.connect(a.getDestination(), 1, 0);
            }
        }, "output out of range");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                ga.connect(a.getDestination(), 0, 1);
            }
        }, "input out of range");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                ga.connect(ga.getGain(), 3);
            }
        }, "output of a parameter connection out of range");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                ga.connect((AudioNode) null);
            }
        }, "null destination");
        Check.equal(1, ga.getNumberOfInputs(), "inputs of a gain");
        Check.equal(1, ga.getNumberOfOutputs(), "outputs of a gain");
        Check.equal(0, a.getDestination().getNumberOfOutputs(), "the destination has no output");
    }

    /**
     * Every form of <code>disconnect</code> removes the matching connections and throws an <code>InvalidAccessError</code> if there is
     * none (except the forms without a destination), and an <code>IndexSizeError</code> for an index out of range.
     * <p>
     * <code>disconnect</code> 의 모든 형태는 맞는 연결을 지우고, 없으면 <code>InvalidAccessError</code> 를 던지며(대상이 없는 형태는 제외) 인덱스가 범위를
     * 벗어나면 <code>IndexSizeError</code> 를 던진다.
     */
    public void testDisconnectForms() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, Q, WebAudioTestKit.SR);
        final ChannelSplitterNode split = ctx.createChannelSplitter(2);
        final GainNode x = ctx.createGain();
        final GainNode y = ctx.createGain();
        final ChannelMergerNode merge = ctx.createChannelMerger(2);
        split.connect(x, 0, 0);
        split.connect(y, 1, 0);
        split.connect(merge, 0, 0);
        split.connect(merge, 1, 1);
        split.connect(x.getGain(), 1);
        // 다른 곳에 연결된 출력만 지우는 형태들.
        split.disconnect(x, 0, 0);
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_ACCESS, new Runnable() {
            public void run() {
                split.disconnect(x, 0, 0);
            }
        }, "the connection was removed already");
        split.disconnect(y);
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_ACCESS, new Runnable() {
            public void run() {
                split.disconnect(y);
            }
        }, "no connection to y any more");
        split.disconnect(merge, 0);
        split.disconnect(x.getGain());
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_ACCESS, new Runnable() {
            public void run() {
                split.disconnect(x.getGain(), 1);
            }
        }, "no parameter connection any more");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_ACCESS, new Runnable() {
            public void run() {
                split.disconnect(merge, 0);
            }
        }, "output 0 has no connection to merge any more");
        split.disconnect(1);
        split.disconnect();
        split.disconnect(1);
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                split.disconnect(2);
            }
        }, "output out of range");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                split.disconnect(merge, 5);
            }
        }, "output out of range with a destination");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                split.disconnect(merge, 0, 7);
            }
        }, "input out of range");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                split.disconnect((AudioNode) null);
            }
        }, "null destination");
    }

    /**
     * Control messages are applied at the start of the next render quantum: a connection made after a quantum was rendered is heard
     * from the next quantum on, and a disconnection silences the next quantum, with the exact frame of the boundary.
     * <p>
     * 제어 메시지는 다음 render quantum 시작에 적용된다: quantum 을 렌더링한 뒤 한 연결은 다음 quantum 부터 들리고, 연결을 끊으면 다음 quantum 이 무음이다.
     * 경계의 프레임이 정확하다.
     */
    public void testControlMessagesApplyAtQuantumBoundary() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(Q * 4);
        ConstantSourceNode c = ctx.createConstantSource();
        c.getOffset().setValue(0.5f);
        c.start(0);
        ctx.renderNextQuantum();
        c.connect(ctx.getDestination());
        ctx.renderNextQuantum();
        c.disconnect();
        ctx.renderNextQuantum();
        c.connect(ctx.getDestination());
        ctx.renderNextQuantum();
        float[] out = ctx.finishRendering().getChannelData(0);
        WebAudioTestKit.assertSilent("before the connection", out, 0, Q);
        // 연결한 quantum 은 0.5, 끊은 뒤의 quantum 은 무음, 다시 연결한 quantum 은 0.5 이다.
        for (int i = Q; i < 2 * Q; i++) {
            Check.near(0.5, out[i], 0, "connected quantum, frame " + i);
        }
        WebAudioTestKit.assertSilent("disconnected quantum", out, 2 * Q, 3 * Q);
        Check.near(0.5, out[3 * Q], 0, "reconnected quantum starts at the boundary");
        Check.near(0.5, out[4 * Q - 1], 0, "reconnected quantum ends at the boundary");
    }

    /**
     * Gain nodes in a chain multiply; a chain of thousands of nodes is ordered without recursion.
     * <p>
     * 사슬로 이은 게인 노드는 곱해진다. 수천 개 노드의 사슬도 재귀 없이 순서가 정해진다.
     */
    public void testChainAndLongChain() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(Q);
        GainNode g1 = ctx.createGain();
        GainNode g2 = ctx.createGain();
        GainNode g3 = ctx.createGain();
        g1.getGain().setValue(2f);
        g2.getGain().setValue(3f);
        g3.getGain().setValue(0.25f);
        constant(ctx, 1f, g3);
        g3.connect(g2);
        g2.connect(g1);
        g1.connect(ctx.getDestination());
        Check.near(0.25 * 3 * 2, WebAudioTestKit.render(ctx)[5], 1e-6, "product of three gains");
        OfflineAudioContext big = WebAudioTestKit.mono(Q);
        AudioNode previous = big.getDestination();
        // 게인 5000 개를 뒤에서부터 이어 붙인다.
        for (int i = 0; i < 5000; i++) {
            GainNode g = big.createGain();
            g.connect(previous);
            previous = g;
        }
        constant(big, 0.75f, previous);
        Check.near(0.75, WebAudioTestKit.render(big)[0], 0, "a chain of 5000 gain nodes");
    }

    /**
     * The output of a node is added to the value of a parameter: <code>computedValue = intrinsic + input</code>.
     * <p>
     * 노드의 출력은 파라미터의 값에 더해진다: <code>computedValue = 고유 값 + 입력</code>.
     */
    public void testParamInput() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(Q);
        GainNode g = ctx.createGain();
        g.getGain().setValue(0f);
        g.connect(ctx.getDestination());
        constant(ctx, 2f, g);
        ConstantSourceNode mod = ctx.createConstantSource();
        mod.getOffset().setValue(0.5f);
        mod.connect(g.getGain());
        mod.start(0);
        Check.near(1.0, WebAudioTestKit.render(ctx)[3], 1e-7, "signal 2 with gain 0 + 0.5");
        OfflineAudioContext c2 = WebAudioTestKit.mono(Q);
        GainNode h = c2.createGain();
        h.connect(c2.getDestination());
        constant(c2, 2f, h);
        ConstantSourceNode m2 = c2.createConstantSource();
        m2.getOffset().setValue(0.5f);
        m2.connect(h.getGain());
        m2.start(0);
        Check.near(3.0, WebAudioTestKit.render(c2)[3], 1e-7, "signal 2 with gain 1 + 0.5");
    }

    /**
     * A stereo output that is connected to a parameter is mixed down to mono (the mean) before it is added.
     * <p>
     * 파라미터에 연결된 스테레오 출력은 더하기 전에 모노(평균)로 다운믹스된다.
     */
    public void testParamInputIsMixedDownToMono() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(Q);
        GainNode g = ctx.createGain();
        g.getGain().setValue(0f);
        g.connect(ctx.getDestination());
        constant(ctx, 1f, g);
        AudioBuffer stereo = new AudioBuffer(2, Q, WebAudioTestKit.SR);
        java.util.Arrays.fill(stereo.getChannelData(0), 0.2f);
        java.util.Arrays.fill(stereo.getChannelData(1), 0.4f);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(stereo);
        src.connect(g.getGain());
        src.start(0);
        Check.near(0.3, WebAudioTestKit.render(ctx)[7], 1e-7, "the mean of the two channels");
    }

    /**
     * A computed value that is NaN becomes the default value; one that overflows is clamped to the nominal range.
     * <p>
     * 계산된 값이 NaN 이면 기본 값이 되고, 넘치면 명목 범위로 제한된다.
     */
    public void testParamInputNaNAndOverflow() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(Q);
        GainNode g = ctx.createGain();
        g.getGain().setValue(7f);
        g.connect(ctx.getDestination());
        constant(ctx, 1f, g);
        AudioBuffer bad = new AudioBuffer(1, Q, WebAudioTestKit.SR);
        java.util.Arrays.fill(bad.getChannelData(0), Float.NaN);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(bad);
        src.connect(g.getGain());
        src.start(0);
        Check.near(1.0, WebAudioTestKit.render(ctx)[3], 0, "NaN is replaced by the default value 1");
        OfflineAudioContext c2 = new OfflineAudioContext(2, Q, WebAudioTestKit.SR);
        StereoPannerNode p = c2.createStereoPanner();
        constant(c2, 1f, p);
        p.connect(c2.getDestination());
        ConstantSourceNode big = c2.createConstantSource();
        big.getOffset().setValue(5f);
        big.connect(p.getPan(), 0);
        big.start(0);
        c2.renderNextQuantum();
        Check.near(1.0, p.getPan().valueAt(0), 0, "the pan is limited to its range");
        // 팬이 오른쪽 끝(1)이면 모노 입력은 오른쪽으로만 간다.
        Check.near(0.0, c2.getDestination().getRenderedBus().channel(0)[5], 1e-6, "full right: no left");
        Check.near(1.0, c2.getDestination().getRenderedBus().channel(1)[5], 1e-6, "full right: all right");
    }

    /**
     * The number of channels of an input follows the mode: <code>max</code> takes the largest connection, <code>clamped-max</code> limits
     * it, <code>explicit</code> is fixed; an input without connection has one silent channel.
     * <p>
     * 입력의 채널 수는 모드를 따른다: <code>max</code> 는 가장 큰 연결, <code>clamped-max</code> 는 그것을 제한한 값, <code>explicit</code> 는 고정이다.
     * 연결이 없는 입력은 무음 채널 하나이다.
     */
    public void testInputChannelCounts() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, Q, WebAudioTestKit.SR);
        AudioBuffer quad = new AudioBuffer(4, Q, WebAudioTestKit.SR);
        // 쿼드 버퍼: L 0.1, R 0.2, SL 0.3, SR 0.4.
        for (int c = 0; c < 4; c++) {
            java.util.Arrays.fill(quad.getChannelData(c), 0.1f * (c + 1));
        }
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(quad);
        GainNode max = ctx.createGain();
        src.connect(max);
        src.start(0);
        max.connect(ctx.getDestination());
        // max 모드: 입력은 4 채널을 유지하고 목적지가 스테레오로 다운믹스한다(L = 0.5 (L + SL), R = 0.5 (R + SR)).
        ctx.renderNextQuantum();
        Check.equal(4, max.inputs[0].bus.getNumberOfChannels(), "max mode keeps 4 channels");
        Check.near(0.5 * (0.1 + 0.3), ctx.getDestination().getRenderedBus().channel(0)[0], 1e-6, "quad to stereo at the destination, left");
        Check.near(0.5 * (0.2 + 0.4), ctx.getDestination().getRenderedBus().channel(1)[0], 1e-6, "quad to stereo at the destination, right");
        OfflineAudioContext c2 = new OfflineAudioContext(2, Q * 6, WebAudioTestKit.SR);
        AudioBuffer quadLong = new AudioBuffer(4, Q * 6, WebAudioTestKit.SR);
        // 긴 쿼드 버퍼: 시험 동안 원천이 계속 재생되게 한다.
        for (int c = 0; c < 4; c++) {
            java.util.Arrays.fill(quadLong.getChannelData(c), 0.1f * (c + 1));
        }
        AudioBufferSourceNode s2 = c2.createBufferSource();
        s2.setBuffer(quadLong);
        GainNode clamped = c2.createGain();
        clamped.setChannelCountMode(ChannelCountMode.CLAMPED_MAX);
        clamped.setChannelCount(2);
        s2.connect(clamped);
        s2.start(0);
        clamped.connect(c2.getDestination());
        c2.renderNextQuantum();
        Check.equal(2, clamped.inputs[0].bus.getNumberOfChannels(), "clamped-max limits to channelCount");
        GainNode explicit = c2.createGain();
        explicit.setChannelCountMode(ChannelCountMode.EXPLICIT);
        explicit.setChannelCount(3);
        s2.connect(explicit);
        explicit.connect(c2.getDestination());
        c2.renderNextQuantum();
        Check.equal(3, explicit.inputs[0].bus.getNumberOfChannels(), "explicit is fixed");
        GainNode lonely = c2.createGain();
        lonely.setChannelCountMode(ChannelCountMode.EXPLICIT);
        lonely.setChannelCount(5);
        lonely.connect(c2.getDestination());
        ConstantSourceNode keep = c2.createConstantSource();
        keep.connect(lonely.getGain(), 0);
        keep.start(0);
        c2.renderNextQuantum();
        Check.equal(5, lonely.inputs[0].bus.getNumberOfChannels(), "explicit without connection has channelCount channels");
        Check.isTrue(lonely.inputs[0].bus.channel(2)[0] == 0f, "and they are silent");
    }

    /**
     * A mono source is up-mixed to the channels of a stereo destination; <code>discrete</code> puts it into the first channel only.
     * <p>
     * 모노 원천은 스테레오 목적지의 채널로 업믹스된다. <code>discrete</code> 는 첫 채널에만 넣는다.
     */
    public void testUpMixAtDestination() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, Q, WebAudioTestKit.SR);
        constant(ctx, 0.5f, ctx.getDestination());
        AudioBuffer r = ctx.startRendering();
        Check.near(0.5, r.getChannelData(0)[1], 0, "left");
        Check.near(0.5, r.getChannelData(1)[1], 0, "right");
        OfflineAudioContext d = new OfflineAudioContext(2, Q, WebAudioTestKit.SR);
        d.getDestination().setChannelInterpretation(ChannelInterpretation.DISCRETE);
        constant(d, 0.5f, d.getDestination());
        AudioBuffer q = d.startRendering();
        Check.near(0.5, q.getChannelData(0)[1], 0, "discrete: first channel");
        Check.near(0.0, q.getChannelData(1)[1], 0, "discrete: second channel is silent");
    }

    /**
     * The constraints of the channel attributes of the nodes: the general range, the nodes with fixed values and the destination.
     * <p>
     * 노드의 채널 속성 제약: 일반 범위, 값이 고정된 노드, 목적지.
     */
    public void testChannelAttributeRules() {
        final OfflineAudioContext ctx = new OfflineAudioContext(2, Q, WebAudioTestKit.SR);
        final GainNode g = ctx.createGain();
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                g.setChannelCount(0);
            }
        }, "channelCount 0");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                g.setChannelCount(33);
            }
        }, "channelCount above the maximum");
        g.setChannelCount(32);
        final StereoPannerNode p = ctx.createStereoPanner();
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                p.setChannelCount(3);
            }
        }, "panner channelCount 3");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                p.setChannelCountMode(ChannelCountMode.MAX);
            }
        }, "panner mode max");
        final ChannelSplitterNode s = ctx.createChannelSplitter(4);
        Check.equal(4, s.getChannelCount(), "splitter channelCount");
        Check.equal(ChannelCountMode.EXPLICIT, s.getChannelCountMode(), "splitter mode");
        Check.equal(ChannelInterpretation.DISCRETE, s.getChannelInterpretation(), "splitter interpretation");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                s.setChannelCount(2);
            }
        }, "splitter channelCount");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                s.setChannelCountMode(ChannelCountMode.MAX);
            }
        }, "splitter mode");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                s.setChannelInterpretation(ChannelInterpretation.SPEAKERS);
            }
        }, "splitter interpretation");
        final ChannelMergerNode m = ctx.createChannelMerger(3);
        Check.equal(1, m.getChannelCount(), "merger channelCount");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                m.setChannelCount(2);
            }
        }, "merger channelCount");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                m.setChannelCountMode(ChannelCountMode.MAX);
            }
        }, "merger mode");
        final AudioDestinationNode d = ctx.getDestination();
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                d.setChannelCount(1);
            }
        }, "offline destination channelCount");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                d.setChannelCountMode(ChannelCountMode.MAX);
            }
        }, "offline destination mode");
        d.setChannelCount(2);
        Check.equal(2, d.getMaxChannelCount(), "maxChannelCount of the offline destination");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                try {
                    new ChannelSplitterNode(ctx, 0);
                } catch (AudioException e) {
                    throw e;
                }
            }
        }, "splitter with 0 outputs");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                ctx.createChannelMerger(33);
            }
        }, "merger with 33 inputs");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                g.setChannelCountMode(null);
            }
        }, "null mode");
    }

    /**
     * A feedback loop through a delay node: with an impulse as input, a delay of 200 frames and a feedback gain of 0.5 the output is
     * the train of echoes <code>y[200 k] = 0.5^(k - 1)</code>; the delay breaks the cycle, so no node is muted.
     * <p>
     * 지연 노드를 지나는 피드백 루프: 입력이 임펄스이고 지연이 200 프레임, 피드백 이득이 0.5 이면 출력은 에코의 열 <code>y[200 k] = 0.5^(k - 1)</code> 이다.
     * 지연 노드가 순환을 끊으므로 무음 처리되는 노드는 없다.
     */
    public void testFeedbackLoopWithDelay() {
        int length = 200 * 6 + 50;
        // 샘플링 주파수 8192 에서 200 프레임은 200/8192 초로 float 에 정확히 표현된다.
        OfflineAudioContext ctx = new OfflineAudioContext(1, length, 8192f);
        AudioBuffer impulse = new AudioBuffer(1, 1, 8192f);
        impulse.getChannelData(0)[0] = 1f;
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(impulse);
        DelayNode delay = ctx.createDelay(1.0);
        delay.getDelayTime().setValue(200f / 8192f);
        GainNode feedback = ctx.createGain();
        feedback.getGain().setValue(0.5f);
        src.connect(delay);
        delay.connect(feedback);
        feedback.connect(delay);
        delay.connect(ctx.getDestination());
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        // 200 의 배수 위치에만 0.5^(k-1) 가 있다.
        for (int i = 0; i < length; i++) {
            double expected = (i > 0 && i % 200 == 0) ? Math.pow(0.5, i / 200 - 1) : 0.0;
            Check.near(expected, out[i], 1e-7, "frame " + i);
        }
        Check.isTrue(delay.isCycleBreaker(), "the delay breaks the cycle");
        Check.equal(0, ctx.getMutedNodeCount(), "nothing is muted");
    }

    /**
     * In a cycle the delay time is at least one render quantum: a delay of 64 frames behaves like 128 frames.
     * <p>
     * 순환 안에서 지연 시간은 최소 render quantum 하나이다: 64 프레임 지연은 128 프레임처럼 동작한다.
     */
    public void testDelayInCycleIsAtLeastOneQuantum() {
        int length = Q * 5;
        OfflineAudioContext ctx = new OfflineAudioContext(1, length, 8192f);
        AudioBuffer impulse = new AudioBuffer(1, 1, 8192f);
        impulse.getChannelData(0)[0] = 1f;
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(impulse);
        DelayNode delay = ctx.createDelay(1.0);
        delay.getDelayTime().setValue(64f / 8192f);
        GainNode feedback = ctx.createGain();
        feedback.getGain().setValue(0.5f);
        src.connect(delay);
        delay.connect(feedback);
        feedback.connect(delay);
        delay.connect(ctx.getDestination());
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        // 지연이 한 quantum 으로 제한되므로 에코는 128 의 배수 위치에 있다.
        for (int i = 0; i < length; i++) {
            double expected = (i > 0 && i % Q == 0) ? Math.pow(0.5, i / Q - 1) : 0.0;
            Check.near(expected, out[i], 1e-7, "frame " + i);
        }
    }

    /**
     * A cycle without a delay node is muted: the nodes of the cycle output silence and the rest of the graph is not affected.
     * <p>
     * 지연 노드가 없는 순환은 무음 처리된다: 순환의 노드는 무음을 출력하고 그래프의 나머지는 영향을 받지 않는다.
     */
    public void testCycleWithoutDelayIsMuted() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(Q);
        GainNode a = ctx.createGain();
        GainNode b = ctx.createGain();
        a.connect(b);
        b.connect(a);
        constant(ctx, 1f, a);
        a.connect(ctx.getDestination());
        constant(ctx, 0.25f, ctx.getDestination());
        float[] out = WebAudioTestKit.render(ctx);
        Check.near(0.25, out[0], 0, "only the independent source is heard");
        Check.equal(2, ctx.getMutedNodeCount(), "the two nodes of the cycle are muted");
        OfflineAudioContext self = WebAudioTestKit.mono(Q);
        GainNode loop = self.createGain();
        loop.connect(loop);
        constant(self, 1f, loop);
        loop.connect(self.getDestination());
        Check.near(0.0, WebAudioTestKit.render(self)[0], 0, "a node connected to itself is muted");
        Check.equal(1, self.getMutedNodeCount(), "one node is muted");
    }

    /**
     * A started source is held by the context until it has ended, and an <code>ended</code> notification is sent once through the task
     * sink when the stop time is reached; the nodes that no source reaches are not processed.
     * <p>
     * 시작한 원천은 끝날 때까지 컨텍스트가 붙잡고, 정지 시간에 이르면 작업 싱크를 통해 <code>ended</code> 통지가 한 번 간다. 어느 원천도 닿지 않는 노드는
     * 처리하지 않는다.
     */
    public void testPinningAndEnded() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(Q * 6);
        final List<String> events = new ArrayList<String>();
        ctx.setTaskSink(new BaseAudioContext.TaskSink() {
            public void post(Runnable task) {
                events.add("posted");
                task.run();
            }
        });
        OscillatorNode osc = ctx.createOscillator();
        osc.connect(ctx.getDestination());
        osc.setEndedListener(new Runnable() {
            public void run() {
                events.add("ended");
            }
        });
        Check.equal(0, ctx.getPinnedNodeCount(), "nothing is pinned before start");
        osc.start(0);
        osc.stop(2.5 * Q / WebAudioTestKit.SR);
        ctx.renderNextQuantum();
        Check.equal(1, ctx.getPinnedNodeCount(), "the started source is pinned");
        ctx.renderNextQuantum();
        Check.equal(0, events.size(), "not ended yet");
        ctx.renderNextQuantum();
        Check.equal(2, events.size(), "ended is posted once, at the quantum of the stop time");
        Check.equal("ended", events.get(1), "the listener ran");
        Check.equal(0, ctx.getPinnedNodeCount(), "the ended source is released");
        ctx.renderNextQuantum();
        ctx.renderNextQuantum();
        ctx.renderNextQuantum();
        Check.equal(2, events.size(), "no second notification");
        GainNode lonely = ctx.createGain();
        lonely.getGain().setValueAtTime(5f, 0);
        Check.near(1.0, lonely.getGain().getValue(), 0, "an unconnected node keeps its value (it is not processed)");
    }

    /**
     * Rendering with another render quantum size, and the limits of the context arguments.
     * <p>
     * 다른 render quantum 크기로 렌더링하기와 컨텍스트 인수의 제한.
     */
    public void testQuantumSizeAndArgumentLimits() {
        OfflineAudioContext ctx = new OfflineAudioContext(1, 100, 8000f, 16);
        Check.equal(16, ctx.getRenderQuantumSize(), "quantum size");
        constant(ctx, 0.5f, ctx.getDestination());
        AudioBuffer r = ctx.startRendering();
        Check.equal(100, r.getLength(), "result length");
        Check.near(0.5, r.getChannelData(0)[99], 0, "last frame");
        Check.near(112 / 8000.0, ctx.getCurrentTime(), 1e-12, "the length was rounded up to a whole number of quanta");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                new OfflineAudioContext(1, 100, 2999f);
            }
        }, "sample rate below 3000");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                new OfflineAudioContext(1, 100, 768001f);
            }
        }, "sample rate above 768000");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                new OfflineAudioContext(0, 100, 8000f);
            }
        }, "0 channels");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                new OfflineAudioContext(1, 0, 8000f);
            }
        }, "length 0");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                new OfflineAudioContext(1, 100, 8000f, 0);
            }
        }, "quantum size 0");
    }

    /**
     * <code>startRendering</code> can be called once; the state goes from suspended through running to closed.
     * <p>
     * <code>startRendering</code> 은 한 번만 부를 수 있다. 상태는 suspended 에서 running 을 거쳐 closed 가 된다.
     */
    public void testOfflineLifecycle() {
        final OfflineAudioContext ctx = WebAudioTestKit.mono(Q);
        Check.equal(ContextState.SUSPENDED, ctx.getState(), "new context");
        Check.equal(Q, ctx.getRenderQuantumSize(), "default quantum");
        ctx.renderNextQuantum();
        Check.equal(ContextState.RUNNING, ctx.getState(), "while rendering");
        Check.isTrue(ctx.isComplete(), "complete after one quantum");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                ctx.renderNextQuantum();
            }
        }, "render after completion");
        ctx.finishRendering();
        Check.equal(ContextState.CLOSED, ctx.getState(), "after finishing");
        final OfflineAudioContext used = WebAudioTestKit.mono(Q);
        used.startRendering();
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                used.startRendering();
            }
        }, "startRendering twice");
    }
}
