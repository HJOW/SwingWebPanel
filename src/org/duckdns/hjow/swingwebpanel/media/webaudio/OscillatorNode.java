package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * A source that generates a periodic waveform (Web Audio API 1.26): a sine, square, sawtooth or triangle wave, or a {@link PeriodicWave}.
 * The output has one channel. The instantaneous phase is the integral of the computed frequency
 * <code>frequency(t) * 2^(detune(t) / 1200)</code> (both parameters are a-rate) and is zero at the exact start time of the node; the
 * computed frequency is limited to the range of the Nyquist frequency. Changing the type keeps the phase. The waveform is read from
 * band-limited wave tables, so a high frequency does not alias.
 * <p>
 * 주기 파형을 만드는 원천(Web Audio API 1.26): 사인파, 사각파, 톱니파, 삼각파 또는 {@link PeriodicWave}. 출력은 채널 하나이다. 순간 위상은 계산된 주파수
 * <code>frequency(t) * 2^(detune(t) / 1200)</code> (두 파라미터 모두 a-rate)의 적분이며 노드의 정확한 시작 시간에 0 이다. 계산된 주파수는 나이퀴스트
 * 주파수 범위로 제한한다. 종류를 바꿔도 위상은 유지된다. 파형은 대역 제한된 파형 표에서 읽으므로 높은 주파수에서도 에일리어싱이 없다.
 */
public final class OscillatorNode extends AudioScheduledSourceNode {

    /**
     * The frequency in Hz, from minus to plus the Nyquist frequency (a-rate, default 440).
     * <p>
     * 주파수(Hz). 나이퀴스트 주파수의 음수에서 양수까지(a-rate, 기본 440).
     */
    private final AudioParam frequency;

    /**
     * The detune in cents (a-rate, default 0).
     * <p>
     * 디튠(센트, a-rate, 기본 0).
     */
    private final AudioParam detune;

    /**
     * The type of the waveform.
     * <p>
     * 파형의 종류.
     */
    private volatile OscillatorType type = OscillatorType.SINE;

    /**
     * The wave that is played: the shared basic wave of the type or the custom wave.
     * <p>
     * 재생하는 파형: 종류에 맞는 공유 기본 파형 또는 사용자 파형.
     */
    private volatile PeriodicWave wave;

    /**
     * The phase as an index into the wave table (rendering thread).
     * <p>
     * 파형 표의 인덱스로 나타낸 위상(렌더링 스레드).
     */
    private double phase;

    /**
     * True after the phase was set from the start time (rendering thread).
     * <p>
     * 시작 시간에서 위상을 정한 뒤 true(렌더링 스레드).
     */
    private boolean phaseInitialized;

    /**
     * The table choice of the oscillator (rendering thread).
     * <p>
     * 발진기의 표 선택(렌더링 스레드).
     */
    private final PeriodicWave.Selection selection = new PeriodicWave.Selection();

    /**
     * Creates an oscillator of the type <code>sine</code> with a frequency of 440 Hz.
     * <p>
     * 주파수 440 Hz 의 <code>sine</code> 발진기를 만든다.
     *
     * @param context the context / 컨텍스트
     */
    public OscillatorNode(BaseAudioContext context) {
        super(context);
        float nyquist = context.getSampleRate() / 2f;
        frequency = registerParam(new AudioParam(context, this, "frequency", 440f, -nyquist, nyquist, AutomationRate.A_RATE, false));
        detune = registerParam(new AudioParam(context, this, "detune", 0f, -153600f, 153600f, AutomationRate.A_RATE, false));
        wave = context.getBasicWave(OscillatorType.SINE);
    }

    /**
     * Returns the <code>frequency</code> parameter.
     * <p>
     * <code>frequency</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getFrequency() {
        return frequency;
    }

    /**
     * Returns the <code>detune</code> parameter.
     * <p>
     * <code>detune</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getDetune() {
        return detune;
    }

    /**
     * Returns the type of the waveform.
     * <p>
     * 파형의 종류를 반환한다.
     *
     * @return the type / 종류
     */
    public OscillatorType getType() {
        return type;
    }

    /**
     * Sets the type of the waveform to a basic type; the phase is kept.
     * <p>
     * 파형의 종류를 기본 종류로 정한다. 위상은 유지된다.
     *
     * @param newType the type / 종류
     * @throws AudioException <code>InvalidStateError</code> for <code>custom</code> (use {@link #setPeriodicWave(PeriodicWave)}) / <code>custom</code> 이면
     *                        <code>InvalidStateError</code>({@link #setPeriodicWave(PeriodicWave)} 를 쓴다)
     */
    public void setType(OscillatorType newType) {
        if (newType == null) {
            throw new AudioException(AudioException.Kind.TYPE, "type is null");
        }
        if (newType == OscillatorType.CUSTOM) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the type custom is set with setPeriodicWave()");
        }
        wave = getContext().getBasicWave(newType);
        type = newType;
    }

    /**
     * Sets a custom waveform; the type becomes <code>custom</code>.
     * <p>
     * 사용자 파형을 정한다. 종류는 <code>custom</code> 이 된다.
     *
     * @param periodicWave the waveform / 파형
     */
    public void setPeriodicWave(PeriodicWave periodicWave) {
        if (periodicWave == null) {
            throw new AudioException(AudioException.Kind.TYPE, "periodicWave is null");
        }
        wave = periodicWave;
        type = OscillatorType.CUSTOM;
    }

    /**
     * Renders the waveform of the quantum into the single output channel.
     * <p>
     * quantum 의 파형을 하나뿐인 출력 채널에 렌더링한다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBus bus = outputBus(0);
        bus.setNumberOfChannels(1);
        float[] out = bus.channel(0);
        long frame0 = getQuantumStart();
        int first = firstPlayFrame(frame0, frames);
        int end = endPlayFrame(frame0, frames);
        // 재생하지 않는 구간은 0 으로 채운다.
        for (int i = 0; i < first; i++) {
            out[i] = 0f;
        }
        for (int i = end; i < frames; i++) {
            out[i] = 0f;
        }
        PeriodicWave w = wave;
        double size = w.getSize();
        double rateScale = w.getRateScale();
        double nyquist = getContext().getSampleRate() / 2.0;
        boolean detuneConstant = detune.isConstantBlock();
        double constantDetune = detuneConstant ? Math.pow(2.0, detune.valueAt(0) / 1200.0) : 1.0;
        if (!phaseInitialized) {
            // 정확한 시작 시간에서 위상이 0 이도록, 첫 프레임이 시작 시간보다 늦은 만큼 위상을 앞으로 보낸다.
            double sr = getContext().getSampleRate();
            double effectiveStart = Math.max(getStartTime(), frame0 / sr);
            double late = (frame0 + first) / sr - effectiveStart;
            double f0 = clampFrequency(frequency.valueAt(first) * (detuneConstant ? constantDetune : Math.pow(2.0, detune.valueAt(first) / 1200.0)), nyquist);
            phase = wrap(f0 * late * size, size);
            phaseInitialized = true;
        }
        // 프레임마다 계산된 주파수로 파형 표를 읽고 위상을 나아가게 한다.
        for (int i = first; i < end; i++) {
            double f = frequency.valueAt(i) * (detuneConstant ? constantDetune : Math.pow(2.0, detune.valueAt(i) / 1200.0));
            f = clampFrequency(f, nyquist);
            w.select(Math.abs(f), selection);
            out[i] = w.sample(selection, phase);
            phase += f * rateScale;
            if (phase >= size || phase < 0) {
                phase = wrap(phase, size);
            }
        }
    }

    /**
     * Limits a frequency to the range of the Nyquist frequency; a NaN becomes 0.
     * <p>
     * 주파수를 나이퀴스트 주파수 범위로 제한한다. NaN 은 0 이 된다.
     *
     * @param f       the frequency / 주파수
     * @param nyquist the Nyquist frequency / 나이퀴스트 주파수
     * @return the limited frequency / 제한된 주파수
     */
    private static double clampFrequency(double f, double nyquist) {
        if (f != f) {
            return 0.0;
        }
        return Math.max(-nyquist, Math.min(nyquist, f));
    }

    /**
     * Wraps a phase into the range from 0 to the table size.
     * <p>
     * 위상을 0 이상 표 크기 미만으로 되감는다.
     *
     * @param p    the phase / 위상
     * @param size the table size / 표 크기
     * @return the wrapped phase / 되감은 위상
     */
    private static double wrap(double p, double size) {
        double r = p - Math.floor(p / size) * size;
        return r >= size ? 0.0 : r;
    }
}
