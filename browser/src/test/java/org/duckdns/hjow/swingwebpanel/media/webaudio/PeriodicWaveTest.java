package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the periodic wave (Web Audio API 1.28): the argument rules of <code>createPeriodicWave</code>, the table size per sample rate,
 * the number of band-limited tables, and the fact that higher tables have fewer partials.
 * <p>
 * 주기파 시험(Web Audio API 1.28): <code>createPeriodicWave</code> 의 인수 규칙, 샘플링 주파수에 따른 표 크기, 대역 제한 표의 수, 높은 표일수록 부분음이
 * 적다는 사실.
 */
public class PeriodicWaveTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public PeriodicWaveTest() {
    }

    /**
     * The arrays must have the same length of at least 2; a missing array is taken as zeros; no array at all makes a sine; the
     * coefficients must be finite.
     * <p>
     * 배열은 길이가 같고 2 이상이어야 한다. 없는 배열은 0 으로 본다. 배열이 둘 다 없으면 사인파이다. 계수는 유한해야 한다.
     */
    public void testArgumentRules() {
        final OfflineAudioContext ctx = WebAudioTestKit.mono(128);
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                ctx.createPeriodicWave(new float[] {0f, 1f}, new float[] {0f, 1f, 2f}, false);
            }
        }, "different lengths");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                ctx.createPeriodicWave(new float[] {0f}, new float[] {0f}, false);
            }
        }, "fewer than 2 elements");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                ctx.createPeriodicWave(new float[] {1f}, null, false);
            }
        }, "only real with 1 element");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                ctx.createPeriodicWave(new float[] {0f, Float.NaN}, new float[] {0f, 0f}, false);
            }
        }, "NaN coefficient");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                ctx.createPeriodicWave(new float[WebAudioLimits.MAX_PERIODIC_WAVE_LENGTH + 1], new float[WebAudioLimits.MAX_PERIODIC_WAVE_LENGTH + 1], false);
            }
        }, "too many terms");
        Check.notNull(ctx.createPeriodicWave(null, new float[] {0f, 1f}, false), "only imag");
        Check.notNull(ctx.createPeriodicWave(new float[] {0f, 1f}, null, false), "only real");
        Check.notNull(ctx.createPeriodicWave(new float[8192], new float[8192], false), "8192 terms are supported");
        OscillatorNode osc = ctx.createOscillator();
        osc.setPeriodicWave(ctx.createPeriodicWave(null, null, false));
        osc.getFrequency().setValue(1000f);
        osc.connect(ctx.getDestination());
        osc.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        Check.near(Math.sin(2 * Math.PI * 1000 * 3 / 8000.0), out[3], 1e-6, "no arrays make a sine");
    }

    /**
     * The table size follows the sample rate (2048 up to 24 kHz, 4096 up to 88.2 kHz, 16384 above) and the number of tables is three
     * per octave of the table size.
     * <p>
     * 표 크기는 샘플링 주파수를 따르고(24 kHz 까지 2048, 88.2 kHz 까지 4096, 그 위는 16384) 표의 수는 표 크기의 옥타브당 세 개이다.
     */
    public void testTableSizes() {
        PeriodicWave a = new PeriodicWave(8000f, new float[] {0f, 0f}, new float[] {0f, 1f}, false);
        Check.equal(2048, a.getSize(), "size at 8 kHz");
        Check.equal(33, a.getTableCount(), "tables at 8 kHz: 3 * log2(2048)");
        PeriodicWave b = new PeriodicWave(44100f, new float[] {0f, 0f}, new float[] {0f, 1f}, false);
        Check.equal(4096, b.getSize(), "size at 44.1 kHz");
        Check.equal(36, b.getTableCount(), "tables at 44.1 kHz: 3 * log2(4096)");
        PeriodicWave c = new PeriodicWave(96000f, new float[] {0f, 0f}, new float[] {0f, 1f}, false);
        Check.equal(16384, c.getSize(), "size at 96 kHz");
        Check.equal(42, c.getTableCount(), "tables at 96 kHz: 3 * log2(16384)");
        Check.near(4096 / 44100.0, b.getRateScale(), 1e-12, "table increment of 1 Hz");
    }

    /**
     * Higher frequencies use tables with fewer partials: a wave with a fundamental and a 20th partial keeps the 20th partial at a low
     * frequency and loses it near the Nyquist frequency.
     * <p>
     * 높은 주파수는 부분음이 적은 표를 쓴다: 기본파와 20번째 부분음이 있는 파형은 낮은 주파수에서는 20번째 부분음을 유지하고 나이퀴스트 주파수 근처에서는
     * 잃는다.
     */
    public void testHigherTablesHaveFewerPartials() {
        float[] real = new float[21];
        float[] imag = new float[21];
        imag[1] = 1f;
        imag[20] = 1f;
        OfflineAudioContext ctx = WebAudioTestKit.mono(4000);
        OscillatorNode low = ctx.createOscillator();
        low.setPeriodicWave(ctx.createPeriodicWave(real, imag, true));
        low.getFrequency().setValue(100f);
        low.connect(ctx.getDestination());
        low.start(0);
        float[] outLow = WebAudioTestKit.render(ctx);
        Check.near(1.0, WebAudioTestKit.amplitudeAt(outLow, 0, 4000, 2000, 8000.0), 0.02, "the 20th partial of 100 Hz is 2000 Hz and is kept");
        OfflineAudioContext c2 = WebAudioTestKit.mono(4000);
        OscillatorNode high = c2.createOscillator();
        high.setPeriodicWave(c2.createPeriodicWave(real, imag, true));
        high.getFrequency().setValue(3100f);
        high.connect(c2.getDestination());
        high.start(0);
        float[] outHigh = WebAudioTestKit.render(c2);
        Check.near(1.0, WebAudioTestKit.amplitudeAt(outHigh, 0, 4000, 3100, 8000.0), 0.02, "the fundamental of 3100 Hz is kept");
        // 20번째 부분음 62000 Hz 는 8000 Hz 로 샘플링하면 2000 Hz 로 접힌다. 표가 그것을 이미 지웠으므로 거기에는 아무것도 없어야 한다.
        Check.isTrue(WebAudioTestKit.amplitudeAt(outHigh, 0, 4000, 2000, 8000.0) < 0.02, "the 20th partial (62 kHz) is removed, not aliased to 2000 Hz");
    }
}
