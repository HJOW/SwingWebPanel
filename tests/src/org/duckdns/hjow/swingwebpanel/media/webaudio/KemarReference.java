package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The independent reference of the HRTF tests: the original impulse responses of the MIT KEMAR compact data set (Bill Gardner and Keith
 * Martin, MIT Media Lab, 1994), read straight from the unchanged <code>kemar/compact.zip</code> of the test resources with its own
 * parser, and a few measurements that the tests make on them (the magnitude response at a frequency by a direct sum, the interaural
 * lag by the cross-correlation). Nothing here uses the converted resource or the classes of the engine. The class is not a suite.
 * <p>
 * HRTF 시험의 독립적인 기준: MIT KEMAR compact 자료(Bill Gardner, Keith Martin, MIT Media Lab, 1994)의 원본 임펄스 응답. 시험 자원의 바뀌지 않은
 * <code>kemar/compact.zip</code> 에서 자체 파서로 곧바로 읽는다. 그 위에서 시험이 하는 측정(직접 합으로 잰 한 주파수의 크기 응답, 상호 상관으로 잰 두 귀의 시차)도
 * 있다. 여기서는 변환한 자원도 엔진의 클래스도 쓰지 않는다. 이 클래스는 스위트가 아니다.
 */
public final class KemarReference {

    /**
     * The sample rate of the data.
     * <p>
     * 자료의 샘플링 주파수.
     */
    public static final double RATE = 44100.0;

    /**
     * The original responses by the key <code>elevation * 1000 + azimuth</code>: <code>[ear][tap]</code> with ear 0 the left ear.
     * <p>
     * 키 <code>고도 * 1000 + 방위각</code> 에 대한 원본 응답: <code>[귀][탭]</code>, 귀 0 이 왼쪽 귀.
     */
    private static Map<Integer, double[][]> data;

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 있다.
     */
    private KemarReference() {
    }

    /**
     * Reads the archive once.
     * <p>
     * 압축 파일을 한 번 읽는다.
     *
     * @return the responses / 응답
     * @throws IOException if the archive cannot be read / 압축 파일을 읽지 못하면
     */
    private static synchronized Map<Integer, double[][]> load() throws IOException {
        if (data != null) {
            return data;
        }
        InputStream res = KemarReference.class.getResourceAsStream("/kemar/compact.zip");
        if (res == null) {
            throw new IOException("the test resource kemar/compact.zip was not found");
        }
        java.io.File tmp = java.io.File.createTempFile("kemar-compact", ".zip");
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(tmp);
            try {
                byte[] buf = new byte[8192];
                int n;
                // 자원을 임시 파일로 복사한다(ZipFile 이 파일을 필요로 한다).
                while ((n = res.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            } finally {
                out.close();
                res.close();
            }
            Map<Integer, double[][]> map = new HashMap<Integer, double[][]>();
            ZipFile z = new ZipFile(tmp);
            try {
                Pattern name = Pattern.compile("elev(-?\\d+)/H(-?\\d+)e(\\d+)a\\.wav");
                Enumeration<? extends ZipEntry> en = z.entries();
                // 항목마다 WAV 의 16비트 스테레오 샘플을 읽는다.
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    if (e.isDirectory()) {
                        continue;
                    }
                    Matcher m = name.matcher(e.getName());
                    if (!m.matches()) {
                        throw new IOException("unexpected entry " + e.getName());
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    InputStream in = z.getInputStream(e);
                    byte[] buf = new byte[4096];
                    int n;
                    // 항목 전체를 읽는다.
                    while ((n = in.read(buf)) > 0) {
                        bos.write(buf, 0, n);
                    }
                    in.close();
                    byte[] b = bos.toByteArray();
                    double[][] h = new double[2][128];
                    // 44 바이트 머리 뒤의 128 프레임을 왼쪽, 오른쪽 순서로 읽는다.
                    for (int t = 0; t < 128; t++) {
                        h[0][t] = (short) ((b[44 + 4 * t] & 0xFF) | (b[45 + 4 * t] << 8)) / 32768.0;
                        h[1][t] = (short) ((b[46 + 4 * t] & 0xFF) | (b[47 + 4 * t] << 8)) / 32768.0;
                    }
                    map.put(Integer.valueOf(Integer.parseInt(m.group(1)) * 1000 + Integer.parseInt(m.group(3))), h);
                }
            } finally {
                z.close();
            }
            data = map;
            return map;
        } finally {
            tmp.delete();
        }
    }

    /**
     * Returns the original impulse responses of a measured direction.
     * <p>
     * 측정한 방향의 원본 임펄스 응답을 반환한다.
     *
     * @param elevation the elevation in degrees / 고도(도)
     * @param azimuth   the azimuth in degrees (0 to 180, to the right) / 방위각(도, 0 ~ 180, 오른쪽)
     * @return the responses, [ear][tap] / 응답 [귀][탭]
     */
    public static double[][] get(int elevation, int azimuth) {
        try {
            double[][] h = load().get(Integer.valueOf(elevation * 1000 + azimuth));
            Check.notNull(h, "the original measurement of " + elevation + "/" + azimuth);
            return h;
        } catch (IOException e) {
            Check.fail("cannot read the original KEMAR data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the number of original measurements.
     * <p>
     * 원본 측정의 수를 반환한다.
     *
     * @return the number / 수
     */
    public static int count() {
        try {
            return load().size();
        } catch (IOException e) {
            Check.fail("cannot read the original KEMAR data: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Returns the magnitude response of an impulse response at a frequency in decibels by a direct sum.
     * <p>
     * 임펄스 응답의 한 주파수에서의 크기 응답(dB)을 직접 합으로 반환한다.
     *
     * @param h    the impulse response / 임펄스 응답
     * @param freq the frequency in Hz / 주파수(Hz)
     * @param rate the sample rate in Hz / 샘플링 주파수(Hz)
     * @return the magnitude in decibels / 크기(dB)
     */
    public static double magnitudeDb(double[] h, double freq, double rate) {
        double re = 0;
        double im = 0;
        double w = 2 * Math.PI * freq / rate;
        // 탭마다 복소 지수와 곱해 더한다.
        for (int k = 0; k < h.length; k++) {
            re += h[k] * Math.cos(w * k);
            im -= h[k] * Math.sin(w * k);
        }
        return 20 * Math.log10(Math.sqrt(re * re + im * im) + 1e-30);
    }

    /**
     * Low-pass filters a response with a Hamming windowed sinc of 63 taps (the group delay of the filter is the same for every input and
     * cancels in a comparison of two responses), so that a time lag can be measured from the low frequency part, where the delay of the
     * ear is not disturbed by the head shadow and the pinna.
     * <p>
     * 응답을 63탭 Hamming 창 sinc 로 저역 통과 필터링한다(필터의 군지연은 모든 입력에 같아 두 응답을 비교할 때 상쇄된다). 귀의 지연이 머리 그림자와 귓바퀴에
     * 방해받지 않는 저주파 부분으로 시차를 잴 수 있게 한다.
     *
     * @param h      the response / 응답
     * @param cutoff the cutoff frequency in Hz / 차단 주파수(Hz)
     * @return the filtered response, as long as the input / 입력과 같은 길이의 필터링한 응답
     */
    public static double[] lowpass(double[] h, double cutoff) {
        int taps = 63;
        int half = taps / 2;
        double fc = cutoff / RATE;
        double[] f = new double[taps];
        double sum = 0;
        // 창을 씌운 sinc 탭을 만든다.
        for (int i = 0; i < taps; i++) {
            double x = i - half;
            double s = x == 0 ? 2 * fc : Math.sin(2 * Math.PI * fc * x) / (Math.PI * x);
            f[i] = s * (0.54 - 0.46 * Math.cos(2 * Math.PI * i / (taps - 1)));
            sum += f[i];
        }
        double[] out = new double[h.length + taps - 1];
        // 합성곱한다(길이 h + taps - 1).
        for (int n = 0; n < out.length; n++) {
            double s = 0;
            // 커널의 탭마다 처리한다.
            for (int k = 0; k < taps; k++) {
                int j = n - k;
                if (j >= 0 && j < h.length) {
                    s += f[k] * h[j];
                }
            }
            out[n] = s / sum;
        }
        return out;
    }

    /**
     * Returns how many samples the left response is later than the right one, found as the peak of the cross-correlation, refined
     * with a parabola through the peak and its neighbours.
     * <p>
     * 왼쪽 응답이 오른쪽보다 몇 샘플 늦은지 상호 상관의 정점으로 찾아 반환한다. 정점과 이웃을 지나는 포물선으로 다듬는다.
     *
     * @param left  the left response / 왼쪽 응답
     * @param right the right response / 오른쪽 응답
     * @return the lag in samples (positive if the left ear is later) / 시차(샘플, 왼쪽 귀가 늦으면 양수)
     */
    public static double lagLeftLater(double[] left, double[] right) {
        int range = 60;
        double[] c = new double[2 * range + 1];
        int best = 0;
        // 시차마다 상호 상관을 구한다.
        for (int l = -range; l <= range; l++) {
            double s = 0;
            // 커널의 탭마다 처리한다.
            for (int k = 0; k < right.length; k++) {
                int j = k + l;
                if (j >= 0 && j < left.length) {
                    s += right[k] * left[j];
                }
            }
            c[l + range] = s;
            if (s > c[best]) {
                best = l + range;
            }
        }
        double offset = 0;
        if (best > 0 && best < c.length - 1) {
            double a = c[best - 1];
            double b = c[best];
            double d = c[best + 1];
            double denom = a - 2 * b + d;
            if (denom != 0) {
                offset = 0.5 * (a - d) / denom;
            }
        }
        return best - range + offset;
    }
}
