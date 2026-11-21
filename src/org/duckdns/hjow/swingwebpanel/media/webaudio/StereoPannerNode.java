package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * Positions its input in a stereo image with the low-cost equal power panning of the specification (Web Audio API 1.30, section 6.3.3).
 * The input has at most two channels (a mono input is panned across the image, a stereo input is balanced) and the output always has two.
 * With <code>pan</code> clamped to <code>[-1, 1]</code>, <code>x = (pan + 1) / 2</code> for a mono input and <code>x = pan + 1</code> or
 * <code>x = pan</code> for a stereo input, the gains are <code>cos(x pi / 2)</code> for the left and <code>sin(x pi / 2)</code> for the
 * right side.
 * <p>
 * 입력을 규격의 저비용 등전력 패닝으로 스테레오 이미지 안에 배치한다(Web Audio API 1.30, 6.3.3절). 입력은 채널이 최대 둘이고(모노 입력은 이미지 전체에
 * 패닝되고 스테레오 입력은 균형이 맞춰진다) 출력은 항상 둘이다. <code>pan</code> 을 <code>[-1, 1]</code> 로 제한하고, 모노 입력은
 * <code>x = (pan + 1) / 2</code>, 스테레오 입력은 <code>x = pan + 1</code> 또는 <code>x = pan</code> 이며, 이득은 왼쪽이 <code>cos(x pi / 2)</code>,
 * 오른쪽이 <code>sin(x pi / 2)</code> 이다.
 */
public final class StereoPannerNode extends AudioNode {

    /**
     * The position from -1 (left) to 1 (right) (a-rate, default 0).
     * <p>
     * -1(왼쪽)에서 1(오른쪽)까지의 위치(a-rate, 기본 0).
     */
    private final AudioParam pan;

    /**
     * Creates a stereo panner that is centred.
     * <p>
     * 가운데에 놓인 스테레오 패너를 만든다.
     *
     * @param context the context / 컨텍스트
     */
    public StereoPannerNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.CLAMPED_MAX, ChannelInterpretation.SPEAKERS);
        pan = registerParam(new AudioParam(context, this, "pan", 0f, -1f, 1f, AutomationRate.A_RATE, false));
    }

    /**
     * Returns the <code>pan</code> parameter.
     * <p>
     * <code>pan</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getPan() {
        return pan;
    }

    /**
     * Rejects a <code>channelCount</code> above 2.
     * <p>
     * 2 를 넘는 <code>channelCount</code> 를 거부한다.
     *
     * @param count the new value / 새 값
     */
    @Override
    void checkChannelCount(int count) {
        if (count > 2) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the channelCount of a StereoPannerNode is at most 2");
        }
    }

    /**
     * Rejects the mode <code>max</code>.
     * <p>
     * <code>max</code> 모드를 거부한다.
     *
     * @param mode the new value / 새 값
     */
    @Override
    void checkChannelCountMode(ChannelCountMode mode) {
        if (mode == ChannelCountMode.MAX) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the channelCountMode of a StereoPannerNode cannot be max");
        }
    }

    /**
     * Pans the input.
     * <p>
     * 입력을 패닝한다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBus in = inputBus(0);
        AudioBus out = outputBus(0);
        out.setNumberOfChannels(2);
        float[] left = out.channel(0);
        float[] right = out.channel(1);
        float[] inL = in.channel(0);
        boolean mono = in.getNumberOfChannels() == 1;
        float[] inR = mono ? inL : in.channel(1);
        // 프레임마다 pan 값으로 좌우 이득을 구해 규격의 식대로 출력한다.
        for (int i = 0; i < frames; i++) {
            double p = Math.max(-1.0, Math.min(1.0, pan.valueAt(i)));
            if (mono) {
                double x = (p + 1.0) / 2.0;
                float s = inL[i];
                left[i] = (float) (s * Math.cos(x * Math.PI / 2.0));
                right[i] = (float) (s * Math.sin(x * Math.PI / 2.0));
            } else {
                double x = p <= 0 ? p + 1.0 : p;
                double gl = Math.cos(x * Math.PI / 2.0);
                double gr = Math.sin(x * Math.PI / 2.0);
                float l = inL[i];
                float r = inR[i];
                if (p <= 0) {
                    left[i] = (float) (l + r * gl);
                    right[i] = (float) (r * gr);
                } else {
                    left[i] = (float) (l * gl);
                    right[i] = (float) (r + l * gr);
                }
            }
        }
    }
}
