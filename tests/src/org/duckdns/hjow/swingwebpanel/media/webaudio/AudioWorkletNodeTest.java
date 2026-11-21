package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the engine side of the audio worklet node (Web Audio API 1.32, plan item P25-06), with processors written in Java: the audio that a processor
 * produces reaches the graph (a gain on an input, a sine generator without inputs, the frame counter of the context), the active source flag (a processor that
 * returns false stops when no input is active and is released by the context, runs while an input is active, a processor that returns true is processed
 * although nothing is connected), the inputs and outputs it sees (<code>null</code> for an input without an active connection, the channels of the outputs by
 * <code>outputChannelCount</code> and by the input), the parameters (a-rate values per frame, one value for a constant block and for a k-rate parameter), the
 * validation of the constructor (1.32.3.3.2), the failure of a processor (silence for good, one notification, the node is released), a skipped quantum, and a
 * factory that is not ready for some quanta.
 * <p>
 * 오디오 worklet 노드의 엔진 쪽 시험(Web Audio API 1.32, 계획 P25-06). 프로세서는 Java 로 쓴다: 프로세서가 만든 오디오가 그래프에 닿는 것(입력에 이득, 입력 없는 사인 생성기, 컨텍스트의
 * 프레임 계수), active source flag(false 를 반환하는 프로세서는 활성 입력이 없으면 멈추고 컨텍스트가 놓아 준다. 입력이 활성인 동안은 실행되고, true 를 반환하는 프로세서는 아무것도
 * 연결되지 않아도 처리된다), 프로세서가 보는 입력과 출력(활성 연결이 없는 입력은 <code>null</code>, 출력의 채널은 <code>outputChannelCount</code> 와 입력에 따른다), 파라미터(a-rate 는
 * 프레임별 값, 일정한 블록과 k-rate 는 값 하나), 생성자의 검증(1.32.3.3.2), 프로세서의 실패(영구 무음, 통지 한 번, 노드를 놓아 준다), 건너뛴 quantum, 몇 quantum 동안 준비되지 않는 팩터리.
 */
public class AudioWorkletNodeTest {

    /**
     * The sample rate of the tests.
     * <p>
     * 시험의 샘플링 주파수.
     */
    private static final float RATE = 8000f;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public AudioWorkletNodeTest() {
    }

    /**
     * No parameters.
     * <p>
     * 파라미터 없음.
     *
     * @return an empty list / 빈 목록
     */
    private static List<AudioWorkletNode.ParamDescriptor> none() {
        return Collections.<AudioWorkletNode.ParamDescriptor> emptyList();
    }

    /**
     * Creates a node with a processor that is created right away.
     * <p>
     * 프로세서를 바로 만드는 노드를 만든다.
     *
     * @param ctx       the context / 컨텍스트
     * @param inputs    the number of inputs / 입력 수
     * @param outputs   the number of outputs / 출력 수
     * @param processor the processor / 프로세서
     * @return the node / 노드
     */
    private static AudioWorkletNode node(BaseAudioContext ctx, int inputs, int outputs, final AudioWorkletNode.Processor processor) {
        AudioWorkletNode n = new AudioWorkletNode(ctx, inputs, outputs, null, none());
        n.setProcessorFactory(new AudioWorkletNode.ProcessorFactory() {
            /**
             * Gives the processor.
             * <p>
             * 프로세서를 준다.
             *
             * @param node the node / 노드
             * @return the processor / 프로세서
             */
            public AudioWorkletNode.Processor create(AudioWorkletNode node) {
                return processor;
            }
        });
        return n;
    }

    /**
     * A processor that multiplies the first input into the first output.
     * <p>
     * 첫 입력에 곱해 첫 출력에 쓰는 프로세서.
     */
    private static final class Scaler implements AudioWorkletNode.Processor {

        /**
         * The factor.
         * <p>
         * 배율.
         */
        private final float factor;

        /**
         * The result to return.
         * <p>
         * 반환할 결과.
         */
        private final int result;

        /**
         * How often the input was null.
         * <p>
         * 입력이 null 이었던 횟수.
         */
        int nullInputs;

        /**
         * Creates the processor.
         * <p>
         * 프로세서를 만든다.
         *
         * @param factor the factor / 배율
         * @param result the result to return / 반환할 결과
         */
        Scaler(float factor, int result) {
            this.factor = factor;
            this.result = result;
        }

        /**
         * Scales the input.
         * <p>
         * 입력에 배율을 곱한다.
         *
         * @param node   the node / 노드
         * @param frames the frames / 프레임 수
         * @return the result / 결과
         */
        public int process(AudioWorkletNode node, int frames) {
            AudioBus in = node.getInput(0);
            AudioBus out = node.getOutput(0);
            if (in == null) {
                nullInputs++;
                return result;
            }
            // 채널마다 입력에 배율을 곱해 출력에 쓴다.
            for (int c = 0; c < out.getNumberOfChannels(); c++) {
                for (int i = 0; i < frames; i++) {
                    out.channel(c)[i] = in.channel(c)[i] * factor;
                }
            }
            return result;
        }
    }

    /**
     * The audio of a processor reaches the graph: a constant of 0.5 through a processor that doubles it is 1 at the destination, and the node follows the number of
     * channels of the input.
     * <p>
     * 프로세서의 오디오가 그래프에 닿는다: 0.5 의 상수가 두 배로 만드는 프로세서를 지나면 목적지에서 1 이고, 노드는 입력의 채널 수를 따른다.
     */
    public void testProcessorTransformsItsInput() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(512);
        ConstantSourceNode src = ctx.createConstantSource();
        src.getOffset().setValue(0.5f);
        Scaler scaler = new Scaler(2f, AudioWorkletNode.INACTIVE);
        AudioWorkletNode n = node(ctx, 1, 1, scaler);
        src.connect(n);
        n.connect(ctx.getDestination());
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        // 모든 프레임이 1 이어야 한다.
        for (int i = 0; i < out.length; i++) {
            Check.near(1.0, out[i], 0, "frame " + i);
        }
        Check.equal(4, n.getProcessCount(), "the processor runs in every quantum while the input is active, although it returns false");
        Check.equal(0, scaler.nullInputs, "the input was never null");
    }

    /**
     * The properties of a new node (1.32.3): <code>channelCount</code> 2, mode <code>max</code>, interpretation <code>speakers</code>.
     * <p>
     * 새 노드의 속성(1.32.3): <code>channelCount</code> 2, 모드 <code>max</code>, 해석 <code>speakers</code>.
     */
    public void testDefaultProperties() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(128);
        AudioWorkletNode n = new AudioWorkletNode(ctx, 2, 3, null, none());
        Check.equal(2, n.getNumberOfInputs(), "inputs");
        Check.equal(3, n.getNumberOfOutputs(), "outputs");
        Check.equal(2, n.getChannelCount(), "channelCount");
        Check.same(ChannelCountMode.MAX, n.getChannelCountMode(), "channelCountMode");
        Check.same(ChannelInterpretation.SPEAKERS, n.getChannelInterpretation(), "channelInterpretation");
    }

    /**
     * A processor without inputs that returns true is a source: a sine of the frame counter of the context is exactly the closed form, from the first frame.
     * <p>
     * 입력 없이 true 를 반환하는 프로세서는 원천이다: 컨텍스트의 프레임 계수로 만든 사인이 첫 프레임부터 닫힌 식과 같다.
     */
    public void testSineGeneratorWithoutInputs() {
        final int frames = 1024;
        OfflineAudioContext ctx = WebAudioTestKit.mono(frames);
        AudioWorkletNode n = node(ctx, 0, 1, new AudioWorkletNode.Processor() {
            /**
             * Writes a sine of 250 Hz.
             * <p>
             * 250 Hz 사인을 쓴다.
             *
             * @param node   the node / 노드
             * @param count  the frames / 프레임 수
             * @return true / true
             */
            public int process(AudioWorkletNode node, int count) {
                long f0 = node.getContext().getCurrentFrame();
                float[] o = node.getOutput(0).channel(0);
                // 프레임마다 닫힌 식의 사인을 쓴다.
                for (int i = 0; i < count; i++) {
                    o[i] = (float) Math.sin(2 * Math.PI * 250.0 * (f0 + i) / RATE);
                }
                return AudioWorkletNode.ACTIVE;
            }
        });
        n.connect(ctx.getDestination());
        float[] out = WebAudioTestKit.render(ctx);
        WebAudioTestKit.assertSignal("sine", out, 0, frames, new WebAudioTestKit.Signal() {
            /**
             * The expected sine.
             * <p>
             * 기대하는 사인.
             *
             * @param frame the frame / 프레임
             * @return the value / 값
             */
            public double at(int frame) {
                return Math.sin(2 * Math.PI * 250.0 * frame / RATE);
            }
        }, 1e-6);
        Check.equal(8, n.getProcessCount(), "one call per quantum");
    }

    /**
     * An input that has no active connection is <code>null</code> (zero channels for the script); a processor that returns false although nothing is connected plays
     * its first quantum, then it is silent and the context lets it go; a processor that returns true is processed even if no output is connected.
     * <p>
     * 활성 연결이 없는 입력은 <code>null</code> 이다(스크립트에는 채널 0 개). 아무것도 연결되지 않았는데 false 를 반환하는 프로세서는 첫 quantum 만 재생하고 그 뒤로는 무음이며 컨텍스트가
     * 놓아 준다. true 를 반환하는 프로세서는 출력이 연결되지 않아도 처리된다.
     */
    public void testActiveSourceFlag() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(1024);
        final int[] nulls = new int[1];
        final AudioWorkletNode stops = node(ctx, 1, 1, new AudioWorkletNode.Processor() {
            /**
             * Writes 0.25 and says false.
             * <p>
             * 0.25 를 쓰고 false 라고 한다.
             *
             * @param node   the node / 노드
             * @param frames the frames / 프레임 수
             * @return false / false
             */
            public int process(AudioWorkletNode node, int frames) {
                if (node.getInput(0) == null) {
                    nulls[0]++;
                }
                java.util.Arrays.fill(node.getOutput(0).channel(0), 0, frames, 0.25f);
                return AudioWorkletNode.INACTIVE;
            }
        });
        stops.connect(ctx.getDestination());
        AudioWorkletNode runs = node(ctx, 1, 1, new AudioWorkletNode.Processor() {
            /**
             * Says true and does nothing else.
             * <p>
             * true 라고 하고 다른 일은 하지 않는다.
             *
             * @param node   the node / 노드
             * @param frames the frames / 프레임 수
             * @return true / true
             */
            public int process(AudioWorkletNode node, int frames) {
                return AudioWorkletNode.ACTIVE;
            }
        });
        Check.equal(0, ctx.getPinnedNodeCount(), "nothing is pinned before the first quantum");
        float[] out = WebAudioTestKit.render(ctx);
        // 첫 quantum 은 0.25, 이후는 무음이어야 한다.
        WebAudioTestKit.assertSignal("first quantum", out, 0, 128, new WebAudioTestKit.Signal() {
            /**
             * The expected value.
             * <p>
             * 기대하는 값.
             *
             * @param frame the frame / 프레임
             * @return 0.25 / 0.25
             */
            public double at(int frame) {
                return 0.25;
            }
        }, 0);
        WebAudioTestKit.assertSilent("after the flag became false", out, 128, 1024);
        Check.equal(1, stops.getProcessCount(), "the processor ran once");
        Check.equal(1, nulls[0], "with nothing connected its input was null");
        Check.isFalse(stops.isActiveSource(), "the flag is false");
        Check.equal(8, runs.getProcessCount(), "a processor that says true is processed in every quantum although nothing is connected");
        Check.isTrue(runs.isActiveSource(), "the flag stays true");
        Check.equal(1, ctx.getPinnedNodeCount(), "only the node whose flag is true is held by the context");
    }

    /**
     * An input that is connected but silent (a source that has not started) is not an active input: the processor sees <code>null</code>.
     * <p>
     * 연결되어 있지만 무음인 입력(시작하지 않은 원천)은 활성 입력이 아니다: 프로세서는 <code>null</code> 을 본다.
     */
    public void testInputOfASourceThatDidNotStartIsNull() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(512);
        ConstantSourceNode src = ctx.createConstantSource();
        Scaler scaler = new Scaler(2f, AudioWorkletNode.ACTIVE);
        AudioWorkletNode n = node(ctx, 1, 1, scaler);
        src.connect(n);
        n.connect(ctx.getDestination());
        WebAudioTestKit.render(ctx);
        Check.equal(4, scaler.nullInputs, "a node that did not start gives no active input");
    }

    /**
     * Parameters: an a-rate parameter has a value per frame while it is automated and one value while the block is constant; a k-rate parameter always has one value
     * (the value at the start of the quantum); a linear ramp gives the closed form per frame.
     * <p>
     * 파라미터: a-rate 파라미터는 자동화되는 동안 프레임마다 값이 있고 블록이 일정하면 값이 하나이다. k-rate 파라미터는 항상 값이 하나이다(quantum 시작의 값). 선형 램프는 프레임마다 닫힌 식을 준다.
     */
    public void testParameters() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(1024);
        List<AudioWorkletNode.ParamDescriptor> d = new ArrayList<AudioWorkletNode.ParamDescriptor>();
        d.add(new AudioWorkletNode.ParamDescriptor("a", 0f, -10f, 10f, AutomationRate.A_RATE));
        d.add(new AudioWorkletNode.ParamDescriptor("k", 0.5f, -10f, 10f, AutomationRate.K_RATE));
        final AudioWorkletNode n = new AudioWorkletNode(ctx, 0, 1, null, d);
        final List<Integer> countsA = new ArrayList<Integer>();
        final List<Integer> countsK = new ArrayList<Integer>();
        final List<Float> kValues = new ArrayList<Float>();
        n.setProcessorFactory(new AudioWorkletNode.ProcessorFactory() {
            /**
             * Creates a processor that copies parameter a to the output.
             * <p>
             * 파라미터 a 를 출력에 복사하는 프로세서를 만든다.
             *
             * @param node the node / 노드
             * @return the processor / 프로세서
             */
            public AudioWorkletNode.Processor create(AudioWorkletNode node) {
                return new AudioWorkletNode.Processor() {
                    /**
                     * Copies the values of a.
                     * <p>
                     * a 의 값을 복사한다.
                     *
                     * @param self   the node / 노드
                     * @param frames the frames / 프레임 수
                     * @return true / true
                     */
                    public int process(AudioWorkletNode self, int frames) {
                        float[] a = new float[frames];
                        float[] k = new float[frames];
                        int na = self.copyParameter(0, a);
                        int nk = self.copyParameter(1, k);
                        countsA.add(Integer.valueOf(na));
                        countsK.add(Integer.valueOf(nk));
                        kValues.add(Float.valueOf(k[0]));
                        float[] o = self.getOutput(0).channel(0);
                        // 값이 하나이면 모든 프레임에 같은 값을, 아니면 프레임별 값을 쓴다.
                        for (int i = 0; i < frames; i++) {
                            o[i] = na == 1 ? a[0] : a[i];
                        }
                        return AudioWorkletNode.ACTIVE;
                    }
                };
            }
        });
        // a: 0 에서 시작해 512 프레임(0.064 초) 뒤 1. k: 0.5 에서 시작해 같은 시점에 4.5.
        n.getParameter("a").setValueAtTime(0f, 0.0);
        n.getParameter("a").linearRampToValueAtTime(1f, 512.0 / RATE);
        n.getParameter("k").setValueAtTime(0.5f, 0.0);
        n.getParameter("k").linearRampToValueAtTime(4.5f, 512.0 / RATE);
        n.connect(ctx.getDestination());
        float[] out = WebAudioTestKit.render(ctx);
        WebAudioTestKit.assertSignal("a ramp", out, 0, 512, new WebAudioTestKit.Signal() {
            /**
             * The linear ramp.
             * <p>
             * 선형 램프.
             *
             * @param frame the frame / 프레임
             * @return the value / 값
             */
            public double at(int frame) {
                return frame / 512.0;
            }
        }, 1e-6);
        WebAudioTestKit.assertSignal("a after the ramp", out, 512, 1024, new WebAudioTestKit.Signal() {
            /**
             * The held value.
             * <p>
             * 유지되는 값.
             *
             * @param frame the frame / 프레임
             * @return 1 / 1
             */
            public double at(int frame) {
                return 1.0;
            }
        }, 0);
        // 램프 중인 quantum 은 프레임마다, 램프가 끝난 뒤는 값 하나여야 한다.
        for (int q = 0; q < 8; q++) {
            Check.equal(q < 4 ? 128 : 1, countsA.get(q).intValue(), "values of the a-rate parameter in quantum " + q);
            Check.equal(1, countsK.get(q).intValue(), "values of the k-rate parameter in quantum " + q);
        }
        // k-rate 는 quantum 첫 프레임에서의 램프 값이다.
        for (int q = 0; q < 4; q++) {
            Check.near(0.5 + 4.0 * (q * 128) / 512.0, kValues.get(q).floatValue(), 1e-6, "k-rate value in quantum " + q);
        }
        Check.same(n.getParameters().get(0), n.getParameter("a"), "parameters are in the order of the descriptors");
        Check.isNull(n.getParameter("missing"), "no such parameter");
    }

    /**
     * The channels of the outputs (1.32.3.3.2): the options fix them; without them they are 1 each, except for a node with one input and one output, which follows the
     * input.
     * <p>
     * 출력의 채널(1.32.3.3.2): 옵션이 정한다. 옵션이 없으면 각각 1 이고, 입력과 출력이 하나씩인 노드만 입력을 따른다.
     */
    public void testOutputChannels() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 256, RATE);
        final List<Integer> seen = new ArrayList<Integer>();
        AudioWorkletNode.Processor probe = new AudioWorkletNode.Processor() {
            /**
             * Records the channels of every output.
             * <p>
             * 모든 출력의 채널 수를 기록한다.
             *
             * @param node   the node / 노드
             * @param frames the frames / 프레임 수
             * @return true / true
             */
            public int process(AudioWorkletNode node, int frames) {
                // 출력마다 채널 수를 기록한다.
                for (int j = 0; j < node.getNumberOfOutputs(); j++) {
                    seen.add(Integer.valueOf(node.getOutput(j).getNumberOfChannels()));
                }
                return AudioWorkletNode.ACTIVE;
            }
        };
        // 정해진 채널 수: 입력 하나 출력 하나인 노드도 옵션을 따른다.
        AudioWorkletNode fixed = new AudioWorkletNode(ctx, 1, 1, new int[] {3}, none());
        final AudioWorkletNode.Processor p = probe;
        fixed.setProcessorFactory(new AudioWorkletNode.ProcessorFactory() {
            /**
             * Gives the probe.
             * <p>
             * 탐침을 준다.
             *
             * @param node the node / 노드
             * @return the processor / 프로세서
             */
            public AudioWorkletNode.Processor create(AudioWorkletNode node) {
                return p;
            }
        });
        ctx.startRendering();
        Check.equal(2, seen.size(), "two quanta");
        Check.equal(3, seen.get(0).intValue(), "outputChannelCount fixes the channels");
        Check.equal(3, fixed.getFixedOutputChannels(0), "the fixed channels");

        // 입력이 둘, 출력이 둘이고 옵션이 없으면 출력은 각각 1 채널이다.
        seen.clear();
        OfflineAudioContext ctx2 = new OfflineAudioContext(2, 128, RATE);
        AudioWorkletNode two = node(ctx2, 2, 2, probe);
        ctx2.startRendering();
        Check.equal(2, seen.size(), "two outputs");
        Check.equal(1, seen.get(0).intValue(), "an output without a list is 1 channel (not one input and one output)");
        Check.equal(1, seen.get(1).intValue(), "the second output too");
        Check.equal(0, two.getFixedOutputChannels(0), "not fixed");

        // 입력 하나 출력 하나이고 옵션이 없으면 입력의 채널 수를 따른다(스테레오 원천).
        seen.clear();
        OfflineAudioContext ctx3 = new OfflineAudioContext(2, 128, RATE);
        AudioBufferSourceNode src = ctx3.createBufferSource();
        src.setBuffer(new AudioBuffer(2, 128, RATE));
        AudioWorkletNode follow = node(ctx3, 1, 1, probe);
        src.connect(follow);
        src.start(0);
        ctx3.startRendering();
        Check.equal(1, seen.size(), "one quantum");
        Check.equal(2, seen.get(0).intValue(), "the output follows the channels of the input");
    }

    /**
     * The validation of the constructor (1.32.3.3.2) and of the descriptors.
     * <p>
     * 생성자와 설명의 검증(1.32.3.3.2).
     */
    public void testValidation() {
        final OfflineAudioContext ctx = WebAudioTestKit.mono(128);
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            /**
             * Both numbers are zero.
             * <p>
             * 두 수가 모두 0 이다.
             */
            public void run() {
                new AudioWorkletNode(ctx, 0, 0, null, none());
            }
        }, "no inputs and no outputs");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            /**
             * The list does not fit the outputs.
             * <p>
             * 목록이 출력에 맞지 않는다.
             */
            public void run() {
                new AudioWorkletNode(ctx, 1, 2, new int[] {1}, none());
            }
        }, "outputChannelCount length");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            /**
             * A count of 0.
             * <p>
             * 0 개.
             */
            public void run() {
                new AudioWorkletNode(ctx, 1, 1, new int[] {0}, none());
            }
        }, "zero channels");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            /**
             * More channels than supported.
             * <p>
             * 지원하는 것보다 많은 채널.
             */
            public void run() {
                new AudioWorkletNode(ctx, 1, 1, new int[] {WebAudioLimits.MAX_CHANNELS + 1}, none());
            }
        }, "too many channels");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            /**
             * Too many inputs.
             * <p>
             * 입력이 너무 많다.
             */
            public void run() {
                new AudioWorkletNode(ctx, WebAudioLimits.MAX_CHANNELS + 1, 1, null, none());
            }
        }, "too many inputs");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            /**
             * Two parameters with one name.
             * <p>
             * 이름이 같은 파라미터 둘.
             */
            public void run() {
                List<AudioWorkletNode.ParamDescriptor> d = new ArrayList<AudioWorkletNode.ParamDescriptor>();
                d.add(new AudioWorkletNode.ParamDescriptor("x", 0f, -1f, 1f, AutomationRate.A_RATE));
                d.add(new AudioWorkletNode.ParamDescriptor("x", 0f, -1f, 1f, AutomationRate.K_RATE));
                new AudioWorkletNode(ctx, 1, 1, null, d);
            }
        }, "duplicate parameter names");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            /**
             * A default value outside the range.
             * <p>
             * 범위 밖의 기본 값.
             */
            public void run() {
                new AudioWorkletNode.ParamDescriptor("x", 2f, -1f, 1f, AutomationRate.A_RATE);
            }
        }, "default value outside the range");
    }

    /**
     * A processor that throws ends for good: the output of that quantum is silent although the processor had written to it, every later quantum is silent, the
     * listener is told exactly once with the exception, the processor is not called again, and the context lets the node go.
     * <p>
     * 예외를 던지는 프로세서는 영구히 끝난다: 프로세서가 출력에 썼더라도 그 quantum 의 출력은 무음이고, 이후 모든 quantum 도 무음이며, 리스너는 예외와 함께 정확히 한 번 통지받고,
     * 프로세서는 다시 불리지 않으며, 컨텍스트가 노드를 놓아 준다.
     */
    public void testFailureEndsTheProcessor() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(1024);
        AudioWorkletNode n = node(ctx, 0, 1, new AudioWorkletNode.Processor() {
            /**
             * The calls so far.
             * <p>
             * 지금까지의 호출.
             */
            private int calls;

            /**
             * Writes 0.9 and throws in the third quantum.
             * <p>
             * 0.9 를 쓰고 세 번째 quantum 에서 던진다.
             *
             * @param node   the node / 노드
             * @param frames the frames / 프레임 수
             * @return true / true
             */
            public int process(AudioWorkletNode node, int frames) {
                java.util.Arrays.fill(node.getOutput(0).channel(0), 0, frames, 0.9f);
                if (++calls == 3) {
                    throw new WorkletProcessorException("boom", "worklet.js", 7, 3);
                }
                return AudioWorkletNode.ACTIVE;
            }
        });
        final List<RuntimeException> told = new ArrayList<RuntimeException>();
        n.setErrorListener(new AudioWorkletNode.ErrorListener() {
            /**
             * Records the failure.
             * <p>
             * 실패를 기록한다.
             *
             * @param failure the failure / 실패
             */
            public void processorError(RuntimeException failure) {
                told.add(failure);
            }
        });
        n.connect(ctx.getDestination());
        float[] out = WebAudioTestKit.render(ctx);
        // 처음 두 quantum 은 0.9, 세 번째부터는 무음이어야 한다.
        for (int i = 0; i < 256; i++) {
            Check.near(0.9, out[i], 1e-7, "before the failure, frame " + i);
        }
        WebAudioTestKit.assertSilent("the failing quantum and after", out, 256, 1024);
        Check.equal(3, n.getProcessCount(), "the processor is not called again");
        Check.equal(1, told.size(), "the listener is told once");
        WorkletProcessorException w = (WorkletProcessorException) told.get(0);
        Check.equal("boom", w.getMessage(), "message");
        Check.equal("worklet.js", w.getFilename(), "filename");
        Check.equal(7, w.getLine(), "line");
        Check.equal(3, w.getColumn(), "column");
        Check.same(w, n.getFailure(), "the failure is kept");
        Check.equal(0, ctx.getPinnedNodeCount(), "the context lets a failed node go");
    }

    /**
     * A processor that cannot run in time (<code>SKIPPED</code>) leaves silence for that quantum only, and the flag stays.
     * <p>
     * 제때 실행하지 못한 프로세서(<code>SKIPPED</code>)는 그 quantum 에만 무음을 남기고 표시는 그대로이다.
     */
    public void testSkippedQuantumIsSilent() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(512);
        AudioWorkletNode n = node(ctx, 0, 1, new AudioWorkletNode.Processor() {
            /**
             * The calls so far.
             * <p>
             * 지금까지의 호출.
             */
            private int calls;

            /**
             * Writes 0.5; the second call says it was skipped.
             * <p>
             * 0.5 를 쓴다. 두 번째 호출은 건너뛰었다고 한다.
             *
             * @param node   the node / 노드
             * @param frames the frames / 프레임 수
             * @return the result / 결과
             */
            public int process(AudioWorkletNode node, int frames) {
                java.util.Arrays.fill(node.getOutput(0).channel(0), 0, frames, 0.5f);
                return ++calls == 2 ? AudioWorkletNode.SKIPPED : AudioWorkletNode.ACTIVE;
            }
        });
        n.connect(ctx.getDestination());
        float[] out = WebAudioTestKit.render(ctx);
        Check.near(0.5, out[0], 0, "quantum 0");
        WebAudioTestKit.assertSilent("the skipped quantum", out, 128, 256);
        Check.near(0.5, out[256], 0, "quantum 2");
        Check.near(0.5, out[511], 0, "quantum 3");
        Check.equal(1, n.getSkippedCount(), "one skipped quantum");
        Check.isTrue(n.isActiveSource(), "the flag stays true");
        Check.isNull(n.getFailure(), "no failure");
    }

    /**
     * A factory that is not ready (<code>null</code>) is asked again at the next quantum: the node is silent until it answers, then the processor runs from that
     * quantum on. A factory that throws makes the node fail and tells the listener.
     * <p>
     * 준비되지 않은(<code>null</code>) 팩터리는 다음 quantum 에 다시 묻는다: 답하기 전까지 노드는 무음이고, 그 quantum 부터 프로세서가 실행된다. 던지는 팩터리는 노드를 실패하게 하고 리스너에 알린다.
     */
    public void testFactoryThatIsNotReady() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(1024);
        final int[] asked = new int[1];
        final long[] firstFrame = {-1};
        AudioWorkletNode n = new AudioWorkletNode(ctx, 0, 1, null, none());
        n.setProcessorFactory(new AudioWorkletNode.ProcessorFactory() {
            /**
             * Answers at the fourth call.
             * <p>
             * 네 번째 호출에서 답한다.
             *
             * @param node the node / 노드
             * @return the processor, null for the first three calls / 프로세서. 처음 세 번은 null
             */
            public AudioWorkletNode.Processor create(AudioWorkletNode node) {
                if (++asked[0] < 4) {
                    return null;
                }
                return new AudioWorkletNode.Processor() {
                    /**
                     * Writes 0.5.
                     * <p>
                     * 0.5 를 쓴다.
                     *
                     * @param self   the node / 노드
                     * @param frames the frames / 프레임 수
                     * @return true / true
                     */
                    public int process(AudioWorkletNode self, int frames) {
                        if (firstFrame[0] < 0) {
                            firstFrame[0] = self.getContext().getCurrentFrame();
                        }
                        java.util.Arrays.fill(self.getOutput(0).channel(0), 0, frames, 0.5f);
                        return AudioWorkletNode.ACTIVE;
                    }
                };
            }
        });
        n.connect(ctx.getDestination());
        float[] out = WebAudioTestKit.render(ctx);
        Check.equal(4, asked[0], "asked at the quanta 0, 1, 2 and 3");
        Check.equal(384, firstFrame[0], "the processor runs from the quantum in which the factory answered");
        WebAudioTestKit.assertSilent("until the factory answered", out, 0, 384);
        Check.near(0.5, out[384], 0, "from quantum 3");

        // 던지는 팩터리는 노드를 실패하게 하고 리스너에 알린다.
        OfflineAudioContext ctx2 = WebAudioTestKit.mono(256);
        AudioWorkletNode bad = new AudioWorkletNode(ctx2, 0, 1, null, none());
        final List<RuntimeException> told = new ArrayList<RuntimeException>();
        bad.setErrorListener(new AudioWorkletNode.ErrorListener() {
            /**
             * Records the failure.
             * <p>
             * 실패를 기록한다.
             *
             * @param failure the failure / 실패
             */
            public void processorError(RuntimeException failure) {
                told.add(failure);
            }
        });
        bad.setProcessorFactory(new AudioWorkletNode.ProcessorFactory() {
            /**
             * Fails.
             * <p>
             * 실패한다.
             *
             * @param node the node / 노드
             * @return never / 반환하지 않는다
             */
            public AudioWorkletNode.Processor create(AudioWorkletNode node) {
                throw new WorkletProcessorException("no constructor", "w.js", 1, 1);
            }
        });
        bad.connect(ctx2.getDestination());
        float[] silent = WebAudioTestKit.render(ctx2);
        WebAudioTestKit.assertSilent("a node whose constructor failed", silent, 0, 256);
        Check.equal(1, told.size(), "told once");
        Check.equal(0, bad.getProcessCount(), "never processed");
    }

    /**
     * A factory that is never ready makes the node fail after {@link AudioWorkletNode#MAX_CONSTRUCTION_ATTEMPTS} quanta.
     * <p>
     * 끝내 준비되지 않는 팩터리는 {@link AudioWorkletNode#MAX_CONSTRUCTION_ATTEMPTS} quantum 뒤에 노드를 실패하게 한다.
     */
    public void testFactoryThatNeverGetsReady() {
        OfflineAudioContext ctx = new OfflineAudioContext(1, (AudioWorkletNode.MAX_CONSTRUCTION_ATTEMPTS + 8) * 128, RATE);
        AudioWorkletNode n = new AudioWorkletNode(ctx, 0, 1, null, none());
        final int[] asked = new int[1];
        n.setProcessorFactory(new AudioWorkletNode.ProcessorFactory() {
            /**
             * Never ready.
             * <p>
             * 끝내 준비되지 않는다.
             *
             * @param node the node / 노드
             * @return null / null
             */
            public AudioWorkletNode.Processor create(AudioWorkletNode node) {
                asked[0]++;
                return null;
            }
        });
        n.connect(ctx.getDestination());
        ctx.startRendering();
        Check.equal(AudioWorkletNode.MAX_CONSTRUCTION_ATTEMPTS, asked[0], "the factory is asked until the limit");
        Check.notNull(n.getFailure(), "the construction counts as failed");
    }

    /**
     * An output that connects to the parameter of another node drives it: a processor that writes a constant to a gain parameter scales a source.
     * <p>
     * 다른 노드의 파라미터에 연결한 출력이 그 파라미터를 구동한다: 이득 파라미터에 상수를 쓰는 프로세서가 원천의 크기를 조절한다.
     */
    public void testOutputDrivesAParameter() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(256);
        ConstantSourceNode src = ctx.createConstantSource();
        src.getOffset().setValue(0.5f);
        GainNode gain = ctx.createGain();
        gain.getGain().setValue(0f);
        AudioWorkletNode ctl = node(ctx, 0, 1, new AudioWorkletNode.Processor() {
            /**
             * Writes 2.
             * <p>
             * 2 를 쓴다.
             *
             * @param node   the node / 노드
             * @param frames the frames / 프레임 수
             * @return true / true
             */
            public int process(AudioWorkletNode node, int frames) {
                java.util.Arrays.fill(node.getOutput(0).channel(0), 0, frames, 2f);
                return AudioWorkletNode.ACTIVE;
            }
        });
        src.connect(gain);
        ctl.connect(gain.getGain());
        gain.connect(ctx.getDestination());
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        Check.near(1.0, out[0], 1e-6, "0.5 scaled by 0 + 2");
        Check.near(1.0, out[255], 1e-6, "the last frame");
    }
}
