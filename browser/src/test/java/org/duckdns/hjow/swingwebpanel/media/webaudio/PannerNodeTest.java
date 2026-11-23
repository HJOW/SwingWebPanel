package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the panner (Web Audio API 1.27) with the <code>equalpower</code> model and of the listener (1.11) through the audio graph:
 * the defaults, ranges and errors; the left and right gains against the algorithm of the specification written out in the test, for
 * sources around the listener (the folding of the azimuth into -90..90, mono and stereo input; the elevation is ignored); the distance
 * gain and the cone gain applied to the output; a source that moves at a-rate (the gains follow the position of every frame); a
 * listener that is moved and turned; the deprecated <code>setPosition</code> and <code>setOrientation</code> and their refusal when a
 * value curve is set; and the channel constraints.
 * <p>
 * 패너(Web Audio API 1.27)의 <code>equalpower</code> 모델과 청취자(1.11)를 오디오 그래프를 거쳐 시험한다: 기본값, 범위, 오류. 청취자 둘레의 음원에서 좌우 게인을
 * 시험에 적은 규격의 알고리즘과 비교(방위각을 -90..90 으로 접는 것, 모노와 스테레오 입력, 고도는 무시), 출력에 곱해지는 거리 게인과 원뿔 게인, a-rate 로 움직이는
 * 음원(게인이 프레임마다의 위치를 따른다), 이동하고 회전한 청취자, 폐기 예정인 <code>setPosition</code>, <code>setOrientation</code> 과 값 곡선이 있을 때의
 * 거부, 채널 제약.
 */
public class PannerNodeTest {

    /**
     * The sample rate of the contexts of the tests.
     * <p>
     * 시험 컨텍스트의 샘플링 주파수.
     */
    private static final float RATE = 8000f;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public PannerNodeTest() {
    }

    /**
     * Renders a constant source of 0.5 through a panner that is set up by a callback and returns the rendered stereo audio.
     * <p>
     * 콜백으로 설정한 패너를 거치는 0.5 의 일정한 원천을 렌더링해 스테레오 오디오를 반환한다.
     */
    private interface Setup {

        /**
         * Sets up the panner.
         * <p>
         * 패너를 설정한다.
         *
         * @param ctx the context / 컨텍스트
         * @param p   the panner / 패너
         */
        void apply(OfflineAudioContext ctx, PannerNode p);
    }

    /**
     * Renders a constant mono source through a panner.
     * <p>
     * 일정한 모노 원천을 패너를 거쳐 렌더링한다.
     *
     * @param level  the constant level / 일정한 레벨
     * @param frames the length / 길이
     * @param setup  the set-up of the panner / 패너 설정
     * @return the rendered stereo audio / 렌더링한 스테레오 오디오
     */
    private static AudioBuffer renderMono(float level, int frames, Setup setup) {
        OfflineAudioContext ctx = new OfflineAudioContext(2, frames, RATE);
        ConstantSourceNode c = ctx.createConstantSource();
        c.getOffset().setValue(level);
        PannerNode p = ctx.createPanner();
        setup.apply(ctx, p);
        c.connect(p);
        p.connect(ctx.getDestination());
        c.start(0);
        return ctx.startRendering();
    }

    /**
     * The left and right gains of the equal power algorithm of the specification for an azimuth, written out here: the azimuth is
     * clamped to -180..180, folded into -90..90, normalized to <code>x</code> (mono or stereo) and the gains are <code>cos</code> and
     * <code>sin</code> of <code>x pi / 2</code>.
     * <p>
     * 규격의 등전력 알고리즘의 방위각에 대한 좌우 게인(여기에 직접 적었다): 방위각을 -180..180 으로 제한하고 -90..90 으로 접고 <code>x</code> 로 정규화(모노 또는
     * 스테레오)하면 게인은 <code>x pi / 2</code> 의 <code>cos</code> 와 <code>sin</code> 이다.
     *
     * @param azimuth the azimuth in degrees / 방위각(도)
     * @param mono    true for a mono input / 모노 입력이면 true
     * @return the gains, left then right, and the folded azimuth / 게인(왼쪽, 오른쪽)과 접은 방위각
     */
    private static double[] gains(double azimuth, boolean mono) {
        double az = Math.max(-180, Math.min(180, azimuth));
        if (az < -90) {
            az = -180 - az;
        } else if (az > 90) {
            az = 180 - az;
        }
        double x;
        if (mono) {
            x = (az + 90) / 180;
        } else if (az <= 0) {
            x = (az + 90) / 90;
        } else {
            x = az / 90;
        }
        return new double[] {Math.cos(x * Math.PI / 2), Math.sin(x * Math.PI / 2), az};
    }

    /**
     * The defaults, the ranges and the errors of the attributes, and the channel constraints of the node.
     * <p>
     * 속성의 기본값, 범위, 오류와 노드의 채널 제약.
     */
    public void testDefaultsAndErrors() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(256);
        final PannerNode p = ctx.createPanner();
        Check.near(0.0, p.getPositionX().getValue(), 0.0, "positionX");
        Check.near(0.0, p.getPositionY().getValue(), 0.0, "positionY");
        Check.near(0.0, p.getPositionZ().getValue(), 0.0, "positionZ");
        Check.near(1.0, p.getOrientationX().getValue(), 0.0, "orientationX");
        Check.near(0.0, p.getOrientationY().getValue(), 0.0, "orientationY");
        Check.near(0.0, p.getOrientationZ().getValue(), 0.0, "orientationZ");
        Check.same(PanningModelType.EQUAL_POWER, p.getPanningModel(), "the default panning model");
        Check.same(DistanceModelType.INVERSE, p.getDistanceModel(), "the default distance model");
        Check.near(1.0, p.getRefDistance(), 0.0, "refDistance");
        Check.near(10000.0, p.getMaxDistance(), 0.0, "maxDistance");
        Check.near(1.0, p.getRolloffFactor(), 0.0, "rolloffFactor");
        Check.near(360.0, p.getConeInnerAngle(), 0.0, "coneInnerAngle");
        Check.near(360.0, p.getConeOuterAngle(), 0.0, "coneOuterAngle");
        Check.near(0.0, p.getConeOuterGain(), 0.0, "coneOuterGain");
        Check.equal(2L, (long) p.getChannelCount(), "channelCount");
        Check.same(ChannelCountMode.CLAMPED_MAX, p.getChannelCountMode(), "channelCountMode");
        Check.same(ChannelInterpretation.SPEAKERS, p.getChannelInterpretation(), "channelInterpretation");
        Check.same(AutomationRate.A_RATE, p.getPositionX().getAutomationRate(), "the position parameters are a-rate");
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                p.setRefDistance(-1);
            }
        }, "a negative refDistance");
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                p.setMaxDistance(0);
            }
        }, "a maxDistance of 0");
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                p.setRolloffFactor(-0.1);
            }
        }, "a negative rolloffFactor");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                p.setConeOuterGain(1.5);
            }
        }, "a coneOuterGain above 1");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                p.setConeOuterGain(-0.1);
            }
        }, "a coneOuterGain below 0");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                p.setPanningModel(null);
            }
        }, "a null panning model");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                p.setDistanceModel(null);
            }
        }, "a null distance model");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                p.setRefDistance(Double.NaN);
            }
        }, "a NaN refDistance");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                p.setChannelCount(3);
            }
        }, "channelCount 3");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                p.setChannelCountMode(ChannelCountMode.MAX);
            }
        }, "the mode max");
        p.setRefDistance(0);
        p.setRolloffFactor(0);
        p.setConeOuterGain(1.0);
        p.setConeInnerAngle(30);
        p.setConeOuterAngle(90);
        Check.near(90.0, p.getConeOuterAngle(), 0.0, "the cone angles are stored");
        Check.same(PanningModelType.HRTF, PanningModelType.fromScriptName("HRTF"), "script name HRTF");
        Check.same(DistanceModelType.EXPONENTIAL, DistanceModelType.fromScriptName("exponential"), "script name exponential");
        Check.isNull(PanningModelType.fromScriptName("hrtf"), "the script names are case sensitive");
    }

    /**
     * The defaults of the listener: at the origin, forward (0, 0, -1), up (0, 1, 0), all a-rate.
     * <p>
     * 청취자의 기본값: 원점, forward (0, 0, -1), up (0, 1, 0), 모두 a-rate.
     */
    public void testListenerDefaults() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(256);
        AudioListener l = ctx.getListener();
        Check.same(l, ctx.getListener(), "one listener per context");
        AudioParam[] ps = {l.getPositionX(), l.getPositionY(), l.getPositionZ(), l.getForwardX(), l.getForwardY(), l.getForwardZ(), l.getUpX(), l.getUpY(), l.getUpZ()};
        double[] defaults = {0, 0, 0, 0, 0, -1, 0, 1, 0};
        // 파라미터마다 기본값과 속도를 확인한다.
        for (int i = 0; i < ps.length; i++) {
            Check.near(defaults[i], ps[i].getValue(), 0.0, ps[i].getName());
            Check.same(AutomationRate.A_RATE, ps[i].getAutomationRate(), ps[i].getName() + " is a-rate");
        }
        l.setPosition(1, 2, 3);
        Check.near(2.0, l.getPositionY().getValue(), 0.0, "setPosition");
        l.setOrientation(1, 0, 0, 0, 0, 1);
        Check.near(1.0, l.getForwardX().getValue(), 0.0, "setOrientation forward");
        Check.near(1.0, l.getUpZ().getValue(), 0.0, "setOrientation up");
        l.getPositionX().setValueCurveAtTime(new float[] {0f, 1f}, 0.5, 1.0);
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                OfflineAudioContext c = WebAudioTestKit.mono(256);
                AudioListener x = c.getListener();
                x.getPositionY().setValueCurveAtTime(new float[] {0f, 1f}, 0.5, 1.0);
                x.setPosition(1, 1, 1);
            }
        }, "setPosition with a value curve");
    }

    /**
     * Sources around the listener at the distance 1: the output of a mono source is <code>0.5 cos</code> and <code>0.5 sin</code> of
     * <code>x pi / 2</code> with the folded azimuth (the sources behind sound like their mirror image in front); the elevation changes
     * nothing; the output has two channels.
     * <p>
     * 청취자 둘레 거리 1 의 음원: 모노 원천의 출력은 접은 방위각에 대한 <code>x pi / 2</code> 의 <code>0.5 cos</code>, <code>0.5 sin</code> 이다(뒤쪽 음원은 앞쪽의 거울상으로
     * 들린다). 고도는 아무것도 바꾸지 않는다. 출력은 채널이 둘이다.
     */
    public void testEqualPowerGainsAroundTheListener() {
        double[] angles = {-180, -135, -90, -45, -10, 0, 10, 45, 90, 135, 180};
        double[] elevations = {0, 40, -25};
        // 방위각과 고도마다 렌더링해 규격의 식과 비교한다.
        for (int i = 0; i < angles.length; i++) {
            for (int j = 0; j < elevations.length; j++) {
                final double t = Math.toRadians(angles[i]);
                final double e = Math.toRadians(elevations[j]);
                AudioBuffer out = renderMono(0.5f, 256, new Setup() {
                    public void apply(OfflineAudioContext ctx, PannerNode p) {
                        p.getPositionX().setValue((float) (Math.sin(t) * Math.cos(e)));
                        p.getPositionY().setValue((float) Math.sin(e));
                        p.getPositionZ().setValue((float) (-Math.cos(t) * Math.cos(e)));
                    }
                });
                double[] g = gains(angles[i], true);
                Check.near(0.5 * g[0], out.getChannelData(0)[200], 2e-5, "left at " + angles[i] + "/" + elevations[j]);
                Check.near(0.5 * g[1], out.getChannelData(1)[200], 2e-5, "right at " + angles[i] + "/" + elevations[j]);
            }
        }
    }

    /**
     * A stereo input is balanced: for a source to the right (azimuth 90) the left input is mixed into the right output and the left
     * output is silent; to the left (azimuth -90) the right input is mixed into the left output; in front the gains are those of
     * the stereo algorithm.
     * <p>
     * 스테레오 입력은 균형이 맞춰진다. 오른쪽(방위각 90) 음원이면 왼쪽 입력이 오른쪽 출력에 섞이고 왼쪽 출력은 무음이다. 왼쪽(방위각 -90)이면 오른쪽 입력이 왼쪽
     * 출력에 섞인다. 정면에서는 스테레오 알고리즘의 게인이다.
     */
    public void testStereoInputIsBalanced() {
        double[] angles = {90, -90, 0, 45, -45};
        // 방위각마다 스테레오 입력(왼쪽 0.5, 오른쪽 -0.25)을 규격의 식과 비교한다.
        for (int i = 0; i < angles.length; i++) {
            OfflineAudioContext ctx = new OfflineAudioContext(2, 256, RATE);
            AudioBuffer in = new AudioBuffer(2, 256, RATE);
            java.util.Arrays.fill(in.getChannelData(0), 0.5f);
            java.util.Arrays.fill(in.getChannelData(1), -0.25f);
            AudioBufferSourceNode src = ctx.createBufferSource();
            src.setBuffer(in);
            PannerNode p = ctx.createPanner();
            double t = Math.toRadians(angles[i]);
            p.getPositionX().setValue((float) Math.sin(t));
            p.getPositionZ().setValue((float) -Math.cos(t));
            src.connect(p);
            p.connect(ctx.getDestination());
            src.start(0);
            AudioBuffer out = ctx.startRendering();
            double[] g = gains(angles[i], false);
            double l = 0.5;
            double r = -0.25;
            double el = g[2] <= 0 ? l + r * g[0] : l * g[0];
            double er = g[2] <= 0 ? r * g[1] : r + l * g[1];
            Check.near(el, out.getChannelData(0)[200], 2e-5, "stereo left at " + angles[i]);
            Check.near(er, out.getChannelData(1)[200], 2e-5, "stereo right at " + angles[i]);
        }
    }

    /**
     * The distance gain multiplies the output: a source in front at the distance 4 with the inverse model (reference 1, rolloff 1) has
     * the gain 1/4, with the linear model (reference 1, maximum 11, rolloff 1) 0.7, with the exponential model (rolloff 2) 1/16; the sound
     * is the same in front for both channels.
     * <p>
     * 거리 게인이 출력에 곱해진다. 거리 4 의 정면 음원은 역수 모델(기준 1, 롤오프 1)에서 게인 1/4, 선형 모델(기준 1, 최대 11, 롤오프 1)에서 0.7, 지수 모델(롤오프 2)에서
     * 1/16 이다. 정면이므로 두 채널의 소리가 같다.
     */
    public void testDistanceGainIsApplied() {
        final DistanceModelType[] models = {DistanceModelType.INVERSE, DistanceModelType.LINEAR, DistanceModelType.EXPONENTIAL};
        final double[] expected = {0.25, 0.7, 1.0 / 16.0};
        // 모델마다 렌더링해 거리 게인을 곱한 값과 비교한다.
        for (int i = 0; i < models.length; i++) {
            final int k = i;
            AudioBuffer out = renderMono(0.8f, 256, new Setup() {
                public void apply(OfflineAudioContext ctx, PannerNode p) {
                    p.getPositionZ().setValue(-4f);
                    p.setDistanceModel(models[k]);
                    p.setMaxDistance(11);
                    p.setRolloffFactor(k == 2 ? 2 : 1);
                }
            });
            double e = 0.8 * expected[i] * Math.sqrt(0.5);
            Check.near(e, out.getChannelData(0)[100], 2e-5, "left with " + models[i]);
            Check.near(e, out.getChannelData(1)[100], 2e-5, "right with " + models[i]);
        }
    }

    /**
     * The cone gain multiplies the output: a source at the origin that points along +x with the cone angles 60 and 180 and the outer
     * gain 0.2, heard by a listener on the +x side (inside the inner cone: gain 1), at 60 degrees (between: 0.6) and on the -x side
     * (outside: 0.2); the listener is moved with its position parameter.
     * <p>
     * 원뿔 게인이 출력에 곱해진다. 원점에서 +x 를 향하고 원뿔 각 60, 180, 바깥 게인 0.2 인 음원을 +x 쪽의 청취자(안쪽 원뿔 안: 게인 1), 60도(사이: 0.6), -x 쪽(밖:
     * 0.2)의 청취자가 듣는다. 청취자는 위치 파라미터로 옮긴다.
     */
    public void testConeGainIsApplied() {
        final double[] angles = {0, 60, 180};
        final double[] expected = {1.0, 0.6, 0.2};
        // 청취자 위치(음원 둘레 반지름 1)마다 렌더링해 원뿔 게인을 곱한 값과 비교한다.
        for (int i = 0; i < angles.length; i++) {
            final double t = Math.toRadians(angles[i]);
            AudioBuffer out = renderMono(1f, 256, new Setup() {
                public void apply(OfflineAudioContext ctx, PannerNode p) {
                    // 음원은 원점에서 +x 를 향한다. 청취자는 방향에서 각 t 만큼 떨어진 곳(z 방향으로 돌린다)에 둔다.
                    p.getOrientationX().setValue(1f);
                    p.getOrientationY().setValue(0f);
                    p.getOrientationZ().setValue(0f);
                    p.setConeInnerAngle(60);
                    p.setConeOuterAngle(180);
                    p.setConeOuterGain(0.2);
                    ctx.getListener().getPositionX().setValue((float) Math.cos(t));
                    ctx.getListener().getPositionZ().setValue((float) Math.sin(t));
                }
            });
            double ex = Math.sqrt(out.getChannelData(0)[100] * out.getChannelData(0)[100] + out.getChannelData(1)[100] * out.getChannelData(1)[100]);
            Check.near(expected[i], ex, 3e-4, "cone gain at " + angles[i] + " degrees (the equal power pair has unit power)");
        }
    }

    /**
     * A source that moves at a-rate: the position goes from the left to the right in a ramp, and the gains of every frame follow the
     * azimuth of that frame (the closed form <code>atan2(x, 1)</code> for a source that passes in front at the distance 1 along the x
     * axis), so the first frames are heard on the left and the last on the right; the sum of the powers changes only with the distance
     * gain (the inverse model), not with the azimuth.
     * <p>
     * a-rate 로 움직이는 음원: 위치가 램프로 왼쪽에서 오른쪽으로 가고, 프레임마다의 게인이 그 프레임의 방위각을 따른다(거리 1 에서 x 축을 따라 정면을 지나는 음원의
     * 닫힌 식 <code>atan2(x, 1)</code>). 그래서 처음 프레임은 왼쪽에서, 마지막 프레임은 오른쪽에서 들린다. 출력 전력의 합은 방위각이 아니라 거리 게인(역수
     * 모델)에만 따라 바뀐다.
     */
    public void testMovingSourceFollowsEveryFrame() {
        AudioBuffer out = renderMono(1f, 1024, new Setup() {
            public void apply(OfflineAudioContext ctx, PannerNode p) {
                p.getPositionZ().setValue(-1f);
                p.getPositionX().setValueAtTime(-3f, 0.0);
                p.getPositionX().linearRampToValueAtTime(3f, 1024 / (double) RATE);
            }
        });
        // 프레임마다 위치 x 에서 방위각과 거리 게인을 구해 닫힌 식과 비교한다.
        for (int i = 0; i < 1024; i += 37) {
            double x = -3.0 + 6.0 * i / 1024.0;
            double az = Math.toDegrees(Math.atan2(x, 1.0));
            double[] g = gains(az, true);
            double dist = Math.sqrt(x * x + 1.0);
            double dg = 1.0 / (1.0 + (Math.max(dist, 1.0) - 1.0));
            Check.near(g[0] * dg, out.getChannelData(0)[i], 3e-5, "left at frame " + i);
            Check.near(g[1] * dg, out.getChannelData(1)[i], 3e-5, "right at frame " + i);
        }
        Check.isTrue(out.getChannelData(0)[5] > out.getChannelData(1)[5], "the start is on the left");
        Check.isTrue(out.getChannelData(0)[1020] < out.getChannelData(1)[1020], "the end is on the right");
    }

    /**
     * A listener that is moved and turned: with the listener at x = 3 a source at (3, 0, -1) is in front (equal gains); turning the
     * listener by 90 degrees to the right (forward +x) puts a source at (0, 0, -1) on its left, so the left channel is the louder one.
     * <p>
     * 이동하고 회전한 청취자: 청취자가 x = 3 에 있으면 (3, 0, -1) 의 음원은 정면이다(두 게인이 같다). 청취자를 오른쪽으로 90도 돌리면(forward +x) (0, 0, -1) 의 음원이 그의
     * 왼쪽에 놓이므로 왼쪽 채널이 더 크다.
     */
    public void testMovedAndTurnedListener() {
        AudioBuffer front = renderMono(1f, 256, new Setup() {
            public void apply(OfflineAudioContext ctx, PannerNode p) {
                ctx.getListener().getPositionX().setValue(3f);
                p.getPositionX().setValue(3f);
                p.getPositionZ().setValue(-1f);
            }
        });
        Check.near(front.getChannelData(0)[100], front.getChannelData(1)[100], 1e-6, "in front of the moved listener");
        AudioBuffer turned = renderMono(1f, 256, new Setup() {
            public void apply(OfflineAudioContext ctx, PannerNode p) {
                ctx.getListener().setOrientation(1f, 0f, 0f, 0f, 1f, 0f);
                p.getPositionZ().setValue(-1f);
            }
        });
        double[] g = gains(-90, true);
        Check.near(g[0], turned.getChannelData(0)[100], 2e-5, "the source is to the left of the turned listener: left");
        Check.near(g[1], turned.getChannelData(1)[100], 2e-5, "the source is to the left of the turned listener: right");
        Check.isTrue(turned.getChannelData(0)[100] > 0.99, "the left channel is the louder one");
    }

    /**
     * The deprecated <code>setPosition</code> and <code>setOrientation</code> set the parameters, and refuse (NotSupportedError) when a
     * parameter has a value curve; the equal power model has no tail time and the node is released after its source ended.
     * <p>
     * 폐기 예정인 <code>setPosition</code> 과 <code>setOrientation</code> 은 파라미터를 설정하고, 파라미터에 값 곡선이 있으면 거부한다(NotSupportedError). 등전력 모델은
     * 꼬리 시간이 없고 원천이 끝나면 노드가 풀린다.
     */
    public void testDeprecatedSettersAndRelease() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 1024, RATE);
        final PannerNode p = ctx.createPanner();
        p.setPosition(1f, 2f, 3f);
        Check.near(3.0, p.getPositionZ().getValue(), 0.0, "setPosition");
        p.setOrientation(0f, 1f, 0f);
        Check.near(1.0, p.getOrientationY().getValue(), 0.0, "setOrientation");
        p.getPositionX().setValueCurveAtTime(new float[] {0f, 1f}, 0.5, 1.0);
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                p.setPosition(0f, 0f, 0f);
            }
        }, "setPosition with a value curve");
        OfflineAudioContext ctx2 = new OfflineAudioContext(2, 2048, RATE);
        ConstantSourceNode c = ctx2.createConstantSource();
        PannerNode q = ctx2.createPanner();
        c.connect(q);
        q.connect(ctx2.getDestination());
        c.start(0);
        c.stop(0.05);
        ctx2.startRendering();
        Check.equal(0L, (long) ctx2.getPinnedNodeCount(), "nothing is pinned after the source ended (no tail)");
    }
}
