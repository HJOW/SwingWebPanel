package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the buffer source (Web Audio API 1.9, plan item P25-04): the playback of a ramp buffer against its closed form (a ramp is
 * interpolated exactly by linear interpolation, so the expected values are plain formulas of the frame), the start time, the offset and
 * the duration, the playback rate and the detune (k-rate), the sample rate of the buffer, a negative rate, the loop with its end points
 * and the entry into the loop, the stop time, the notification at the end, the output channels and the rules of <code>buffer</code> and
 * <code>start</code>.
 * <p>
 * 버퍼 원천 시험(Web Audio API 1.9, 계획 P25-04): 램프 버퍼의 재생을 닫힌 식과 비교하고(램프는 선형 보간으로 정확히 보간되므로 기대값은 프레임의 단순한 식이다),
 * 시작 시간, offset, duration, 재생 속도와 detune(k-rate), 버퍼의 샘플링 주파수, 음수 속도, 끝점이 있는 루프와 루프 진입, 정지 시간, 끝의 통지, 출력 채널,
 * <code>buffer</code> 와 <code>start</code> 의 규칙을 확인한다.
 */
public class AudioBufferSourceNodeTest {

    /**
     * The sample rate.
     * <p>
     * 샘플링 주파수.
     */
    private static final double SR = 8000.0;

    /**
     * The tolerance for the values of the ramp buffer: the buffer holds floats, the expected values are doubles.
     * <p>
     * 램프 버퍼 값의 허용 오차: 버퍼는 float 를 담고 기대값은 double 이다.
     */
    private static final double TOL = 1e-7;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public AudioBufferSourceNodeTest() {
    }

    /**
     * Creates a context with a source that plays a ramp buffer of a length, connected to the destination.
     * <p>
     * 주어진 길이의 램프 버퍼를 재생하는 원천이 목적지에 연결된 컨텍스트를 만든다.
     *
     * @param frames the length of the rendering / 렌더링 길이
     * @param length the length of the buffer / 버퍼 길이
     * @param rate   the sample rate of the buffer / 버퍼의 샘플링 주파수
     * @param ctxOut receives the context at index 0 / 0번에 컨텍스트를 받는다
     * @return the source / 원천
     */
    private static AudioBufferSourceNode source(int frames, int length, float rate, OfflineAudioContext[] ctxOut) {
        OfflineAudioContext ctx = WebAudioTestKit.mono(frames);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(WebAudioTestKit.ramp(1, length, rate));
        src.connect(ctx.getDestination());
        ctxOut[0] = ctx;
        return src;
    }

    /**
     * Returns the value of the ramp buffer at a position as the engine reads it: the position as a float from <code>i / 1000</code>.
     * <p>
     * 위치에서 램프 버퍼의 값을 반환한다: 위치 <code>i / 1000</code>.
     *
     * @param pos the position in frames / 위치(프레임)
     * @return the value / 값
     */
    private static double ramp(double pos) {
        return pos / 1000.0;
    }

    /**
     * The buffer is played from its first frame and the output is silent after its end; the node ends once.
     * <p>
     * 버퍼는 첫 프레임부터 재생되고 끝난 뒤의 출력은 무음이다. 노드는 한 번 끝난다.
     */
    public void testPlaysTheBufferOnce() {
        OfflineAudioContext[] c = new OfflineAudioContext[1];
        AudioBufferSourceNode src = source(512, 300, 8000f, c);
        final List<Integer> ended = new ArrayList<Integer>();
        final OfflineAudioContext ctx = c[0];
        src.setEndedListener(new Runnable() {
            public void run() {
                ended.add(Integer.valueOf((int) ctx.getCurrentFrame()));
            }
        });
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        WebAudioTestKit.assertSignal("buffer", out, 0, 300, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(i);
            }
        }, TOL);
        WebAudioTestKit.assertSilent("after the end", out, 300, 512);
        Check.equal(1, ended.size(), "one ended notification");
        Check.equal(256, ended.get(0).intValue(), "the notification comes in the quantum that contains the last frame (frames 256 to 383, whose first frame is the current frame)");
    }

    /**
     * The start time delays the playback; the offset moves the playhead; both are exact.
     * <p>
     * 시작 시간은 재생을 늦추고 offset 은 재생 헤드를 옮긴다. 둘 다 정확하다.
     */
    public void testStartTimeAndOffset() {
        OfflineAudioContext[] c = new OfflineAudioContext[1];
        AudioBufferSourceNode src = source(512, 300, 8000f, c);
        src.start(80 / SR, 40 / SR);
        float[] out = WebAudioTestKit.render(c[0]);
        WebAudioTestKit.assertSilent("before the start time", out, 0, 80);
        WebAudioTestKit.assertSignal("from the offset", out, 80, 340, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(40 + (i - 80));
            }
        }, TOL);
        WebAudioTestKit.assertSilent("after the buffer", out, 340, 512);
    }

    /**
     * A duration limits the content that is played, measured in the time of the buffer and not in the time of the output: at half
     * speed 80 frames of content take 160 frames of output.
     * <p>
     * duration 은 재생할 내용을 제한하며 출력의 시간이 아니라 버퍼의 시간으로 잰다: 절반 속도에서는 내용 80프레임이 출력 160프레임이다.
     */
    public void testDuration() {
        OfflineAudioContext[] c = new OfflineAudioContext[1];
        AudioBufferSourceNode src = source(512, 300, 8000f, c);
        src.start(0, 0, 80 / SR);
        float[] out = WebAudioTestKit.render(c[0]);
        WebAudioTestKit.assertSignal("80 frames", out, 0, 80, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(i);
            }
        }, TOL);
        WebAudioTestKit.assertSilent("after the duration", out, 80, 512);
        OfflineAudioContext[] d = new OfflineAudioContext[1];
        AudioBufferSourceNode half = source(512, 300, 8000f, d);
        half.getPlaybackRate().setValue(0.5f);
        half.start(0, 0, 80 / SR);
        float[] slow = WebAudioTestKit.render(d[0]);
        WebAudioTestKit.assertSignal("half speed", slow, 0, 160, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(i * 0.5);
            }
        }, 1e-7);
        WebAudioTestKit.assertSilent("after the duration at half speed", slow, 160, 512);
    }

    /**
     * The computed playback rate is <code>playbackRate * 2^(detune / 1200)</code>; between sample frames the signal is interpolated
     * linearly.
     * <p>
     * 계산된 재생 속도는 <code>playbackRate * 2^(detune / 1200)</code> 이다. 샘플 프레임 사이의 신호는 선형 보간한다.
     */
    public void testPlaybackRateAndDetune() {
        OfflineAudioContext[] c = new OfflineAudioContext[1];
        AudioBufferSourceNode fast = source(512, 1000, 8000f, c);
        fast.getPlaybackRate().setValue(2f);
        fast.start(0);
        WebAudioTestKit.assertSignal("rate 2", WebAudioTestKit.render(c[0]), 0, 400, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(2.0 * i);
            }
        }, 1e-6);
        OfflineAudioContext[] d = new OfflineAudioContext[1];
        AudioBufferSourceNode slow = source(512, 1000, 8000f, d);
        slow.getPlaybackRate().setValue(0.5f);
        slow.start(0);
        WebAudioTestKit.assertSignal("rate 0.5", WebAudioTestKit.render(d[0]), 0, 400, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(0.5 * i);
            }
        }, 1e-6);
        OfflineAudioContext[] e = new OfflineAudioContext[1];
        AudioBufferSourceNode detuned = source(512, 1000, 8000f, e);
        detuned.getPlaybackRate().setValue(1.5f);
        detuned.getDetune().setValue(-1200f);
        detuned.start(0);
        WebAudioTestKit.assertSignal("rate 1.5 detuned by an octave down", WebAudioTestKit.render(e[0]), 0, 400, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(0.75 * i);
            }
        }, 1e-6);
    }

    /**
     * The playback rate is k-rate: the value at the first frame of a quantum is used for the whole quantum, so a ramp of the rate
     * from 1 to 2 over 256 frames gives a rate of 1 in the first quantum and 1.5 in the second.
     * <p>
     * 재생 속도는 k-rate 이다: quantum 첫 프레임의 값을 quantum 전체에 쓰므로 256프레임에 걸친 1 에서 2 로의 속도 램프는 첫 quantum 에서 1, 둘째 quantum 에서
     * 1.5 이다.
     */
    public void testPlaybackRateIsKRate() {
        OfflineAudioContext[] c = new OfflineAudioContext[1];
        AudioBufferSourceNode src = source(384, 1000, 8000f, c);
        src.getPlaybackRate().setValueAtTime(1f, 0);
        src.getPlaybackRate().linearRampToValueAtTime(2f, 256 / SR);
        src.start(0);
        float[] out = WebAudioTestKit.render(c[0]);
        WebAudioTestKit.assertSignal("first quantum", out, 0, 128, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(i);
            }
        }, 1e-6);
        WebAudioTestKit.assertSignal("second quantum", out, 128, 256, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(128 + 1.5 * (i - 128));
            }
        }, 1e-6);
    }

    /**
     * A buffer with another sample rate is played at its own speed: a buffer of 4000 Hz is stretched to twice its length, a buffer of
     * 16000 Hz is squeezed to half.
     * <p>
     * 샘플링 주파수가 다른 버퍼는 자기 속도로 재생된다: 4000 Hz 버퍼는 두 배 길이로 늘어나고 16000 Hz 버퍼는 절반으로 줄어든다.
     */
    public void testBufferSampleRate() {
        OfflineAudioContext[] c = new OfflineAudioContext[1];
        AudioBufferSourceNode low = source(512, 100, 4000f, c);
        low.start(0);
        float[] a = WebAudioTestKit.render(c[0]);
        WebAudioTestKit.assertSignal("4000 Hz buffer", a, 0, 199, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(0.5 * i);
            }
        }, 1e-6);
        WebAudioTestKit.assertSilent("after the stretched buffer", a, 200, 512);
        OfflineAudioContext[] d = new OfflineAudioContext[1];
        AudioBufferSourceNode high = source(512, 100, 16000f, d);
        high.start(0);
        float[] b = WebAudioTestKit.render(d[0]);
        WebAudioTestKit.assertSignal("16000 Hz buffer", b, 0, 50, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(2.0 * i);
            }
        }, 1e-6);
        WebAudioTestKit.assertSilent("after the squeezed buffer", b, 50, 512);
    }

    /**
     * A sub-frame offset is interpolated: an offset of half a frame reads between the first two frames.
     * <p>
     * 프레임 이하의 offset 은 보간한다: 반 프레임의 offset 은 첫 두 프레임 사이를 읽는다.
     */
    public void testSubFrameOffset() {
        OfflineAudioContext[] c = new OfflineAudioContext[1];
        AudioBufferSourceNode src = source(256, 300, 8000f, c);
        src.start(0, 0.5 / SR);
        WebAudioTestKit.assertSignal("half a frame", WebAudioTestKit.render(c[0]), 0, 250, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(i + 0.5);
            }
        }, 1e-6);
    }

    /**
     * A negative rate plays backwards from the offset and the node ends at the start of the buffer.
     * <p>
     * 음수 속도는 offset 에서 거꾸로 재생하고 노드는 버퍼의 시작에서 끝난다.
     */
    public void testNegativeRate() {
        OfflineAudioContext[] c = new OfflineAudioContext[1];
        AudioBufferSourceNode src = source(512, 300, 8000f, c);
        src.getPlaybackRate().setValue(-1f);
        src.start(0, 199 / SR);
        float[] out = WebAudioTestKit.render(c[0]);
        WebAudioTestKit.assertSignal("backwards", out, 0, 200, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(199 - i);
            }
        }, TOL);
        WebAudioTestKit.assertSilent("after the start of the buffer", out, 200, 512);
    }

    /**
     * Looping the whole buffer repeats it without a gap.
     * <p>
     * 버퍼 전체를 루프하면 틈 없이 반복된다.
     */
    public void testLoopWholeBuffer() {
        OfflineAudioContext[] c = new OfflineAudioContext[1];
        AudioBufferSourceNode src = source(512, 100, 8000f, c);
        src.setLoop(true);
        src.start(0);
        WebAudioTestKit.assertSignal("loop", WebAudioTestKit.render(c[0]), 0, 512, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(i % 100);
            }
        }, TOL);
    }

    /**
     * Loop end points select a region; the playhead wraps from the end to the start of the region in either direction; end points that
     * are not valid mean the whole buffer.
     * <p>
     * 루프 끝점은 구간을 정하며 재생 헤드는 어느 방향으로든 구간의 끝에서 시작으로 되감긴다. 올바르지 않은 끝점은 버퍼 전체를 뜻한다.
     */
    public void testLoopPoints() {
        OfflineAudioContext[] c = new OfflineAudioContext[1];
        AudioBufferSourceNode src = source(512, 100, 8000f, c);
        src.setLoop(true);
        src.setLoopStart(20 / SR);
        src.setLoopEnd(60 / SR);
        src.start(0);
        WebAudioTestKit.assertSignal("region 20 to 60", WebAudioTestKit.render(c[0]), 0, 512, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(i < 60 ? i : 20 + (i - 20) % 40);
            }
        }, 1e-6);
        OfflineAudioContext[] d = new OfflineAudioContext[1];
        AudioBufferSourceNode back = source(512, 100, 8000f, d);
        back.setLoop(true);
        back.setLoopStart(20 / SR);
        back.setLoopEnd(60 / SR);
        back.getPlaybackRate().setValue(-1f);
        back.start(0, 50 / SR);
        WebAudioTestKit.assertSignal("backwards in the region", WebAudioTestKit.render(d[0]), 0, 300, new WebAudioTestKit.Signal() {
            public double at(int i) {
                int p = 50 - i;
                return ramp(20 + (((p - 20) % 40) + 40) % 40);
            }
        }, 1e-6);
        OfflineAudioContext[] e = new OfflineAudioContext[1];
        AudioBufferSourceNode invalid = source(300, 100, 8000f, e);
        invalid.setLoop(true);
        invalid.setLoopStart(60 / SR);
        invalid.setLoopEnd(30 / SR);
        invalid.start(0);
        WebAudioTestKit.assertSignal("invalid end points: the whole buffer", WebAudioTestKit.render(e[0]), 0, 300, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(i % 100);
            }
        }, TOL);
    }

    /**
     * Looping does not change the offset: playback starts at the offset and enters the loop when the playhead is inside it; an offset
     * after the loop end plays the rest of the buffer without looping.
     * <p>
     * 루프는 offset 을 바꾸지 않는다: 재생은 offset 에서 시작하고 재생 헤드가 루프 안에 있으면 루프에 들어간다. 루프 끝 뒤의 offset 은 루프 없이 버퍼의
     * 나머지를 재생한다.
     */
    public void testLoopEntry() {
        OfflineAudioContext[] c = new OfflineAudioContext[1];
        AudioBufferSourceNode inside = source(300, 100, 8000f, c);
        inside.setLoop(true);
        inside.setLoopStart(20 / SR);
        inside.setLoopEnd(60 / SR);
        inside.start(0, 30 / SR);
        WebAudioTestKit.assertSignal("offset inside the loop", WebAudioTestKit.render(c[0]), 0, 300, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(20 + ((30 + i) - 20) % 40);
            }
        }, 1e-6);
        OfflineAudioContext[] d = new OfflineAudioContext[1];
        AudioBufferSourceNode after = source(300, 100, 8000f, d);
        after.setLoop(true);
        after.setLoopStart(20 / SR);
        after.setLoopEnd(60 / SR);
        after.start(0, 70 / SR);
        float[] out = WebAudioTestKit.render(d[0]);
        WebAudioTestKit.assertSignal("offset after the loop end starts at the loop end", out, 0, 40, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(60 + i);
            }
        }, 1e-6);
        WebAudioTestKit.assertSilent("and the rest of the buffer is played without looping", out, 40, 300);
    }

    /**
     * The stop time ends the playback and the node.
     * <p>
     * 정지 시간은 재생과 노드를 끝낸다.
     */
    public void testStopTime() {
        OfflineAudioContext[] c = new OfflineAudioContext[1];
        AudioBufferSourceNode src = source(512, 400, 8000f, c);
        final List<String> ended = new ArrayList<String>();
        src.setEndedListener(new Runnable() {
            public void run() {
                ended.add("ended");
            }
        });
        src.start(0);
        src.stop(100 / SR);
        float[] out = WebAudioTestKit.render(c[0]);
        WebAudioTestKit.assertSignal("until the stop time", out, 0, 100, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return ramp(i);
            }
        }, TOL);
        WebAudioTestKit.assertSilent("after the stop time", out, 100, 512);
        Check.equal(1, ended.size(), "ended once");
    }

    /**
     * A source without a buffer is silent; a buffer that is set after <code>start</code> is played; a buffer can be set once but
     * null can always be set; the rules of <code>start</code>.
     * <p>
     * 버퍼가 없는 원천은 무음이다. <code>start</code> 뒤에 정한 버퍼도 재생된다. 버퍼는 한 번만 정할 수 있지만 null 은 언제나 정할 수 있다.
     * <code>start</code> 의 규칙.
     */
    public void testBufferAndStartRules() {
        final OfflineAudioContext ctx = WebAudioTestKit.mono(256);
        final AudioBufferSourceNode src = ctx.createBufferSource();
        src.connect(ctx.getDestination());
        src.start(0);
        src.setBuffer(WebAudioTestKit.ramp(1, 50, 8000f));
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                src.setBuffer(WebAudioTestKit.ramp(1, 50, 8000f));
            }
        }, "second buffer");
        src.setBuffer(null);
        float[] silent = WebAudioTestKit.render(ctx);
        WebAudioTestKit.assertSilent("a null buffer", silent, 0, 256);
        final OfflineAudioContext c2 = WebAudioTestKit.mono(256);
        final AudioBufferSourceNode s2 = c2.createBufferSource();
        s2.connect(c2.getDestination());
        s2.start(0);
        s2.setBuffer(WebAudioTestKit.ramp(1, 50, 8000f));
        Check.near(0.049, WebAudioTestKit.render(c2)[49], TOL, "a buffer set after start is played");
        final AudioBufferSourceNode s3 = ctx.createBufferSource();
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                s3.start(0, -1.0);
            }
        }, "negative offset");
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                s3.start(0, 0.0, -1.0);
            }
        }, "negative duration");
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                s3.start(-1.0);
            }
        }, "negative start time");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                s3.start(0, Double.NaN);
            }
        }, "offset NaN");
        s3.start(0);
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                s3.start(0);
            }
        }, "start twice");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                s3.setLoopStart(Double.POSITIVE_INFINITY);
            }
        }, "loopStart infinite");
    }

    /**
     * The output has the channels of the buffer; after the buffer has ended the output is a single channel of silence.
     * <p>
     * 출력은 버퍼의 채널을 가진다. 버퍼가 끝난 뒤의 출력은 무음 채널 하나이다.
     */
    public void testOutputChannels() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 384, 8000f);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(WebAudioTestKit.ramp(2, 100, 8000f));
        src.connect(ctx.getDestination());
        src.start(0);
        ctx.renderNextQuantum();
        Check.equal(2, src.outputs[0].bus.getNumberOfChannels(), "the output has the 2 channels of the buffer");
        Check.near(0.05, ctx.getDestination().getRenderedBus().channel(0)[50], TOL, "left");
        Check.near(-0.05, ctx.getDestination().getRenderedBus().channel(1)[50], TOL, "right");
        ctx.renderNextQuantum();
        Check.isFalse(src.outputs[0].active, "the ended source is not active");
        Check.equal(1, src.outputs[0].bus.getNumberOfChannels(), "after the end the output is a single channel");
        Check.equal(0, ctx.getPinnedNodeCount(), "and the source was released");
    }
}
