package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.DatatypeConverter;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Compares the offline rendering of this project's Web Audio engine with the output of a real browser (plan item P25-08). The scenarios of
 * <code>tests/resources/webaudio/chrome/scenarios.js</code> (an oscillator of every kind, every filter type, delays, automation, buffer sources, a wave shaper, a
 * convolver, a compressor, panners, channel mixing and an amplitude modulation) are run by both engines with the same script. The reference was rendered by Chromium
 * (the version is in <code>ref/_meta.json</code>) with <code>OfflineAudioContext</code> and stored as Float32 samples; the test renders the same scenario on a page of the
 * component and compares sample by sample. The browser is an independent implementation, so a difference shows a defect of one of the two or a difference
 * that is explained. Most scenarios must agree to float precision (many are bit for bit the same: the mixing, the IIR filter, the steps of the automation). The
 * explained differences are listed with their reason in this class and are asserted too, so that a change of either behavior is noticed.
 * <p>
 * 이 프로젝트의 Web Audio 엔진의 오프라인 렌더링을 실제 브라우저의 출력과 비교한다(계획 P25-08). <code>tests/resources/webaudio/chrome/scenarios.js</code> 의 시나리오
 * (모든 종류의 발진기, 모든 필터 형식, 지연, 자동화, 버퍼 원천, 웨이브셰이퍼, 컨볼버, 컴프레서, 패너, 채널 믹스, 진폭 변조)를 두 엔진이 같은 스크립트로 실행한다.
 * 참조는 Chromium(버전은 <code>ref/_meta.json</code>)이 <code>OfflineAudioContext</code> 로 렌더링해 Float32 표본으로 저장한 것이고, 시험은 컴포넌트의 페이지에서
 * 같은 시나리오를 렌더링해 표본마다 비교한다. 브라우저는 독립적인 구현이므로 차이는 둘 중 하나의 결함이거나 설명되는 차이를 뜻한다. 대부분의 시나리오는 float 정밀도로
 * 일치해야 한다(믹스, IIR 필터, 자동화의 계단처럼 비트 단위로 같은 것이 많다). 설명되는 차이는 이유와 함께 이 클래스에 적고 그 차이도 단언해 어느 쪽 동작이
 * 바뀌어도 알 수 있게 한다.
 */
public class ChromeReferenceTest {

    /**
     * The default tolerance of the largest absolute error: the float arithmetic of two implementations of the same formula.
     * <p>
     * 가장 큰 절대 오차의 기본 허용치: 같은 식을 구현한 두 구현의 float 연산 차이.
     */
    static final double DEFAULT_TOLERANCE = 2e-5;

    /**
     * The tolerances of the scenarios that differ from the default (see {@link #tolerances()} for the reasons).
     * <p>
     * 기본값과 다른 시나리오의 허용 오차({@link #tolerances()} 에 이유가 있다).
     */
    private final Map<String, Double> tolerance = tolerances();

    /**
     * The scenarios whose delay differs (the oversampling filters of the wave shaper have a latency that the specification leaves to the implementation): they are
     * compared after the best alignment of the two outputs.
     * <p>
     * 지연이 다른 시나리오(웨이브셰이퍼의 과표본화 필터 지연은 규격이 구현에 맡긴다): 두 출력을 가장 잘 맞춘 뒤 비교한다.
     */
    private static final Set<String> ALIGNED = new HashSet<String>();

    /**
     * The scenarios with an explained difference that have their own assertions (see {@link #checkDifference(String, float[], float[])}).
     * <p>
     * 설명되는 차이가 있어 따로 단언하는 시나리오({@link #checkDifference(String, float[], float[])} 참고).
     */
    private static final Set<String> DIFFERENT = new HashSet<String>();

    static {
        ALIGNED.add("shaper_2x");
        ALIGNED.add("shaper_4x");
        DIFFERENT.add("osc_sine_nyq");
        DIFFERENT.add("osc_saw21k");
        DIFFERENT.add("delay_feedback");
        DIFFERENT.add("bufsrc_offset");
        DIFFERENT.add("osc_start_mid");
    }

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ChromeReferenceTest() {
    }

    /**
     * The tolerances that differ from the default, with the reason.
     * <p>
     * 기본값과 다른 허용 오차와 그 이유.
     *
     * @return the tolerances by scenario name / 시나리오 이름별 허용 오차
     */
    private static Map<String, Double> tolerances() {
        Map<String, Double> m = new HashMap<String, Double>();
        // 대역 제한 발진기: 두 구현이 옥타브별 표와 표 사이 보간으로 같은 파형을 만들지만 표의 점 수와 보간의 float 연산이 달라 몇 만분의 일의 차이가 난다.
        m.put("osc_saw", 1e-4);
        m.put("osc_saw5k", 1e-4);
        m.put("osc_sq880", 1e-4);
        m.put("osc_sq9k", 1e-4);
        // 분수 지연의 선형 보간: 한쪽은 float, 다른 쪽은 double 로 계산해 몇 만분의 일의 차이가 난다. 지연이 변하는 경우(delay_mod)는 조금 더 크다.
        m.put("delay_frac", 1e-4);
        m.put("delay_mod", 1.5e-4);
        // 곡선을 보간하는 웨이브셰이퍼(과표본화 없음): 곡선 점 사이의 보간 float 연산 차이.
        m.put("shaper_none", 5e-5);
        // 컨볼버(정규화 함): 규격 본문의 보정 상수는 0.00125 이고 Chromium 은 10^(-58/20)=0.0012589 를 쓴다. 그 비율(0.99293)만큼 이 엔진의 출력이 작다. 아래에서 참조를 그 비율로 맞춘 뒤 비교한다.
        m.put("conv_norm", 5e-6);
        return m;
    }

    /**
     * Reads a resource of the test classes as text.
     * <p>
     * 시험 클래스의 자원을 문자열로 읽는다.
     *
     * @param path the path / 경로
     * @return the text / 문자열
     * @throws IOException if it cannot be read / 읽을 수 없으면
     */
    private static String read(String path) throws IOException {
        InputStream in = ChromeReferenceTest.class.getResourceAsStream(path);
        Check.notNull(in, "the resource " + path);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 끝까지 읽는다.
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    /**
     * Decodes the Float32 samples (little endian) of a base64 text.
     * <p>
     * base64 문자열의 Float32 표본(리틀 엔디언)을 해독한다.
     *
     * @param text the base64 text / base64 문자열
     * @return the samples / 표본
     */
    private static float[] floats(String text) {
        byte[] b = DatatypeConverter.parseBase64Binary(text);
        float[] f = new float[b.length / 4];
        // 4바이트씩 float 로 바꾼다.
        for (int i = 0; i < f.length; i++) {
            int bits = (b[4 * i] & 0xff) | ((b[4 * i + 1] & 0xff) << 8) | ((b[4 * i + 2] & 0xff) << 16) | ((b[4 * i + 3] & 0xff) << 24);
            f[i] = Float.intBitsToFloat(bits);
        }
        return f;
    }

    /**
     * Parses the reference file of a scenario: the base64 text of every channel.
     * <p>
     * 시나리오의 참조 파일을 해석한다: 채널마다 base64 문자열.
     *
     * @param json the file / 파일
     * @return the samples by channel / 채널별 표본
     */
    private static float[][] reference(String json) {
        Matcher data = Pattern.compile("\"data\":\\[([^\\]]*)\\]").matcher(json);
        Check.isTrue(data.find(), "the reference has data");
        Matcher item = Pattern.compile("\"([A-Za-z0-9+/=]*)\"").matcher(data.group(1));
        List<float[]> channels = new ArrayList<float[]>();
        // 채널마다 해독한다.
        while (item.find()) {
            channels.add(floats(item.group(1)));
        }
        return channels.toArray(new float[channels.size()][]);
    }

    /**
     * Renders a scenario on a page of the component and returns the samples by channel.
     * <p>
     * 컴포넌트의 페이지에서 시나리오를 렌더링해 채널별 표본을 반환한다.
     *
     * @param page the page that has the scenarios / 시나리오가 있는 페이지
     * @param name the name of the scenario / 시나리오 이름
     * @param ch   the number of channels / 채널 수
     * @return the samples / 표본
     */
    private static float[][] render(WebAudioPage page, String name, int ch) {
        page.run("var result = {done: false}; (function () { var sc = SCENARIOS.filter(function (s) { return s.name === '" + name + "'; })[0];"
                + " var ctx = new OfflineAudioContext(sc.ch, sc.frames, sc.rate); sc.build(ctx);"
                + " ctx.startRendering().then(function (b) { result.buf = b; result.done = true; }, function (e) { result.err = e.name + ':' + e.message; result.done = true; }); })()");
        page.waitFor("result.done", 60000, "the rendering of " + name);
        Check.equal("undefined", page.run("String(result.err)"), "the rendering of " + name + " succeeded");
        float[][] out = new float[ch][];
        // 채널마다 표본을 문자열로 받아 해석한다.
        for (int c = 0; c < ch; c++) {
            String[] parts = page.run("Array.prototype.join.call(result.buf.getChannelData(" + c + "), ',')").split(",");
            out[c] = new float[parts.length];
            // 표본 문자열을 하나씩 해석한다.
            for (int i = 0; i < parts.length; i++) {
                out[c][i] = (float) Double.parseDouble(parts[i]);
            }
        }
        return out;
    }

    /**
     * Returns the largest absolute difference of two channels.
     * <p>
     * 두 채널의 가장 큰 절대 차이를 반환한다.
     *
     * @param a one channel / 한 채널
     * @param b the other channel / 다른 채널
     * @return the largest difference / 가장 큰 차이
     */
    private static double maxAbs(float[] a, float[] b) {
        double worst = 0;
        // 프레임마다 차이를 본다.
        for (int i = 0; i < a.length; i++) {
            worst = Math.max(worst, Math.abs((double) a[i] - b[i]));
        }
        return worst;
    }

    /**
     * Returns the root mean square of a channel.
     * <p>
     * 채널의 제곱 평균 제곱근을 반환한다.
     *
     * @param a the channel / 채널
     * @return the rms / 제곱 평균 제곱근
     */
    private static double rms(float[] a) {
        double e = 0;
        // 모든 프레임의 제곱을 더한다.
        for (int i = 0; i < a.length; i++) {
            e += (double) a[i] * a[i];
        }
        return Math.sqrt(e / a.length);
    }

    /**
     * Returns the index of the first sample whose size is over a small threshold, or -1.
     * <p>
     * 크기가 작은 문턱을 넘는 첫 표본의 위치를 반환한다. 없으면 -1.
     *
     * @param a the channel / 채널
     * @return the index / 위치
     */
    private static int firstSound(float[] a) {
        // 앞에서부터 처음으로 소리가 나는 프레임을 찾는다.
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i]) > 1e-9) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the root mean square of the difference of two channels after the best alignment (a delay of up to <code>maxLag</code> frames), over the middle part where
     * both are in the steady state.
     * <p>
     * 두 채널을 가장 잘 맞춘(<code>maxLag</code> 프레임까지의 지연) 뒤 둘 다 정상 상태인 가운데 부분에서의 차이의 제곱 평균 제곱근을 반환한다.
     *
     * @param ref    the reference / 참조
     * @param mine   this engine / 이 엔진
     * @param maxLag the largest delay to try / 시험할 가장 큰 지연
     * @return the smallest rms / 가장 작은 제곱 평균 제곱근
     */
    private static double alignedRms(float[] ref, float[] mine, int maxLag) {
        double best = Double.MAX_VALUE;
        int from = maxLag + 100;
        int to = ref.length - maxLag - 100;
        // 지연을 하나씩 바꿔 가며 차이가 가장 작은 것을 찾는다.
        for (int lag = -maxLag; lag <= maxLag; lag++) {
            double e = 0;
            // 가운데 구간의 프레임마다 차이의 제곱을 더한다.
            for (int i = from; i < to; i++) {
                double d = (double) ref[i] - mine[i + lag];
                e += d * d;
            }
            best = Math.min(best, Math.sqrt(e / (to - from)));
        }
        return best;
    }

    /**
     * Asserts the explained difference of a scenario: the reason is in the comment of each case, the numbers are what both engines produce.
     * <p>
     * 시나리오의 설명되는 차이를 단언한다: 이유는 각 사례의 주석에 있고 숫자는 두 엔진이 만드는 값이다.
     *
     * @param name the scenario / 시나리오
     * @param ref  the reference (first channel) / 참조(첫 채널)
     * @param mine this engine (first channel) / 이 엔진(첫 채널)
     */
    private static void checkDifference(String name, float[] ref, float[] mine) {
        if ("osc_sine_nyq".equals(name) || "osc_saw21k".equals(name)) {
            // 나이퀴스트 가까이의 기본파: Chromium 은 가장 높은 옥타브 표에서 기본파를 크게 줄인다(21900 Hz 사인파의 진폭이 이상값의 약 3%). 이 엔진은 규격대로
            // 나이퀴스트보다 낮은 기본파는 줄이지 않는다. 그래서 크기 비율이 작아야 하고, 이 엔진의 사인파는 이상적인 사인파와 같아야 한다.
            double ratio = rms(ref) / rms(mine);
            Check.isTrue(ratio < 0.2, name + ": the browser attenuates the fundamental near the Nyquist frequency (rms ratio " + ratio + ")");
            if ("osc_sine_nyq".equals(name)) {
                double worst = 0;
                // 이 엔진의 사인파가 이상적인 사인파와 같은지 본다.
                for (int i = 0; i < mine.length; i++) {
                    worst = Math.max(worst, Math.abs(mine[i] - Math.sin(2 * Math.PI * 21900.0 * i / 44100.0)));
                }
                Check.isTrue(worst < 2e-4, name + ": this engine keeps the sine of 21900 Hz at full size (error " + worst + ")");
            }
        } else if ("delay_feedback".equals(name)) {
            // 순환 안의 지연: 규격은 순환 안에서 delayTime 을 최소 한 render quantum(128 프레임)으로 제한한다. 이 엔진은 그대로 따르므로 첫 소리가 128 프레임에
            // 나온다. Chromium 은 직접 경로를 제한하지 않고(44 프레임) 되먹임 경로에 한 quantum 을 더한다(두 번째 소리가 44.1+128+44.1 프레임).
            Check.equal(128, firstSound(mine), name + ": this engine clamps the delay in a cycle to one render quantum");
            Check.equal(44, firstSound(ref), name + ": the browser does not clamp the direct path");
        } else if ("bufsrc_offset".equals(name)) {
            // offset 0.003 초는 132.3 프레임: Chromium 은 정수 프레임(132)에서 읽기 시작하고 이 엔진은 규격대로 132.3 프레임 위치를 보간한다. 잡음 입력이라
            // 소수 프레임의 차이가 크게 보인다(bufsrc_offset_int 는 정수 프레임에서 두 엔진이 같다).
            double d = maxAbs(ref, mine);
            Check.isTrue(d > 0.05 && d < 1.0, name + ": the fractional offset is interpolated by this engine and truncated by the browser (difference " + d + ")");
        } else if ("osc_start_mid".equals(name)) {
            // 시작 시각 100.5 프레임: Chromium 은 프레임으로 올려 101 에서 위상 0 으로 시작하고, 이 엔진은 규격 문장대로 프레임 사이의 정확한 시각에서 시작한다.
            // 1000 Hz 에서 반 프레임의 위상 차이는 2 pi 0.5 1000 / 44100 = 0.0712 라디안이다(osc_start_int 는 정수 프레임에서 두 엔진이 같다).
            double d = maxAbs(ref, mine);
            Check.isTrue(d > 0.06 && d < 0.08, name + ": half a frame of phase at 1000 Hz (difference " + d + ")");
        }
    }

    /**
     * Compares every scenario with the reference of the browser.
     * <p>
     * 모든 시나리오를 브라우저의 참조와 비교한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEveryScenarioAgainstTheBrowser() throws Exception {
        WebAudioPage page = new WebAudioPage();
        try {
            page.run(read("/webaudio/chrome/scenarios.js"));
            String[] names = page.run("SCENARIOS.map(function (s) { return s.name; }).join(',')").split(",");
            Check.isTrue(names.length >= 60, "the scenarios are all there: " + names.length);
            StringBuilder failures = new StringBuilder();
            // 시나리오마다 렌더링해 참조와 비교한다.
            for (int k = 0; k < names.length; k++) {
                String name = names[k];
                float[][] ref = reference(read("/webaudio/chrome/ref/" + name + ".json"));
                float[][] mine = render(page, name, ref.length);
                if ("conv_norm".equals(name)) {
                    // 규격 본문의 상수(0.00125)와 Chromium 의 상수(10^(-58/20))의 비율로 참조를 맞춘다.
                    double ratio = 0.00125 / Math.pow(10, -58.0 / 20.0);
                    for (int c = 0; c < ref.length; c++) {
                        // 참조의 표본마다 비율을 곱한다.
                        for (int i = 0; i < ref[c].length; i++) {
                            ref[c][i] = (float) (ref[c][i] * ratio);
                        }
                    }
                }
                if (DIFFERENT.contains(name)) {
                    checkDifference(name, ref[0], mine[0]);
                    System.out.println("REF " + name + " (explained difference) maxAbs=" + maxAbs(ref[0], mine[0]));
                    continue;
                }
                if (ALIGNED.contains(name)) {
                    // 과표본화 필터의 지연은 구현에 맡겨져 있다: 두 출력을 가장 잘 맞춘 뒤 파형이 같은지 본다(Chromium 은 2배에서 128, 4배에서 192 프레임 늦고 이 엔진은 63, 74.5 프레임).
                    double e = alignedRms(ref[0], mine[0], 250);
                    System.out.println("REF " + name + " aligned rms=" + e);
                    if (e > 5e-5) {
                        failures.append(' ').append(name).append(" aligned rms=").append(e);
                    }
                    continue;
                }
                double worst = 0;
                double energy = 0;
                double errorEnergy = 0;
                // 채널과 프레임마다 오차를 모은다.
                for (int c = 0; c < ref.length; c++) {
                    Check.equal(ref[c].length, mine[c].length, name + " channel " + c + " has the same length");
                    // 프레임마다 오차를 모은다.
                    for (int i = 0; i < ref[c].length; i++) {
                        double d = Math.abs((double) ref[c][i] - mine[c][i]);
                        worst = Math.max(worst, d);
                        energy += (double) ref[c][i] * ref[c][i];
                        errorEnergy += d * d;
                    }
                }
                double limit = tolerance.containsKey(name) ? tolerance.get(name).doubleValue() : DEFAULT_TOLERANCE;
                double snr = errorEnergy == 0 ? 999 : 10 * Math.log10(energy / errorEnergy);
                System.out.println("REF " + name + " maxAbs=" + worst + " snr=" + (Math.round(snr * 10) / 10.0) + "dB limit=" + limit);
                if (worst > limit) {
                    failures.append(' ').append(name).append('=').append(worst);
                }
            }
            Check.equal("", failures.toString(), "the scenarios whose error is over the tolerance");
        } finally {
            page.fixture.scripts.dispose();
        }
    }
}
