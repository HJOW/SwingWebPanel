package hrtfgen;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Converts the compact HRTF data set of the MIT KEMAR dummy head measurements (Bill Gardner and Keith Martin, MIT Media Lab, 1994,
 * <code>compact.zip</code>) into the resource file that the Web Audio engine of SwingWebPanel reads for the <code>HRTF</code> panning
 * model of the <code>PannerNode</code>. The tool reads the 16-bit stereo WAV files of the archive (128 frames at 44100 Hz: the left and
 * the right ear impulse response of one source direction), splits every impulse response into a minimum phase kernel and a delay (the
 * minimum phase kernel has the magnitude response of the measurement; the delay is the low frequency group delay of the remaining excess
 * phase), removes the delay that is common to all measurements, and writes everything as one binary file. The format is described in
 * the AGENTS.md of the tool and in <code>HrtfDatabase</code> of the engine.
 * <p>
 * Usage: <code>HrtfGen compact.zip output.hrtf [--retrieved YYYY-MM-DD]</code>. Exit codes: 0 success, 1 usage error, 2 failure to read
 * the archive or to write the file.
 * <p>
 * MIT KEMAR 더미 헤드 측정(Bill Gardner, Keith Martin, MIT Media Lab, 1994)의 compact HRTF 자료(<code>compact.zip</code>)를 SwingWebPanel 의 Web Audio
 * 엔진이 <code>PannerNode</code> 의 <code>HRTF</code> 패닝 모델에 쓰는 자원 파일로 바꾼다. 도구는 압축 파일의 16비트 스테레오 WAV 파일(44100 Hz, 128 프레임.
 * 음원 방향 하나의 왼쪽과 오른쪽 귀 임펄스 응답)을 읽고, 임펄스 응답마다 최소 위상 커널과 지연으로 나눈 뒤(최소 위상 커널은 측정의 크기 응답을 가지며, 지연은 나머지
 * 초과 위상의 저주파 군지연이다) 모든 측정에 공통인 지연을 빼고 전부를 하나의 이진 파일로 쓴다. 형식은 도구의 AGENTS.md 와 엔진의 <code>HrtfDatabase</code> 에
 * 설명되어 있다.
 * <p>
 * 사용법: <code>HrtfGen compact.zip output.hrtf [--retrieved YYYY-MM-DD]</code>. 종료 코드: 0 성공, 1 사용법 오류, 2 압축 파일을 읽지 못하거나 파일을 쓰지 못함.
 */
public final class HrtfGen {

    /**
     * The number of taps of every impulse response of the data set.
     * <p>
     * 자료의 모든 임펄스 응답의 탭 수.
     */
    static final int TAPS = 128;

    /**
     * The sample rate of the data set in Hz.
     * <p>
     * 자료의 샘플링 주파수(Hz).
     */
    static final int RATE = 44100;

    /**
     * The size of the FFTs of the minimum phase computation (the impulse response is zero padded to it).
     * <p>
     * 최소 위상 계산의 FFT 크기(임펄스 응답을 이 크기까지 0 으로 채운다).
     */
    static final int FFT_SIZE = 16384;

    /**
     * The number of FFT bins (from 1) over which the low frequency group delay is averaged: 512 bins at 16384 points are 1378 Hz.
     * <p>
     * 저주파 군지연을 평균하는 FFT 빈 수(1 번부터). 16384 점에서 512 빈은 1378 Hz 이다.
     */
    static final int DELAY_BINS = 512;

    /**
     * The magic bytes at the start of the output file.
     * <p>
     * 출력 파일 맨 앞의 매직 바이트.
     */
    static final String MAGIC = "SWPHRTF1";

    /**
     * The version of the output format.
     * <p>
     * 출력 형식의 버전.
     */
    static final int VERSION = 1;

    /**
     * The pattern of the names of the WAV files in the archive, for example <code>elev-10/H-10e020a.wav</code>: the elevation and the
     * azimuth in degrees.
     * <p>
     * 압축 파일 안 WAV 파일 이름의 패턴. 예: <code>elev-10/H-10e020a.wav</code>. 고도와 방위각(도).
     */
    private static final Pattern NAME = Pattern.compile("elev(-?\\d+)/H(-?\\d+)e(\\d+)a\\.wav");

    /**
     * One measured direction: the impulse responses of the two ears and what the analysis made of them.
     * <p>
     * 측정한 방향 하나: 두 귀의 임펄스 응답과 분석 결과.
     */
    static final class Measurement {

        /**
         * The elevation in degrees.
         * <p>
         * 고도(도).
         */
        final int elevation;

        /**
         * The azimuth in degrees, positive towards the right ear.
         * <p>
         * 방위각(도). 오른쪽 귀 쪽이 양수.
         */
        final int azimuth;

        /**
         * The original impulse responses, [ear][tap], ear 0 is the left ear (scaled to +-1).
         * <p>
         * 원래 임펄스 응답 [귀][탭]. 귀 0 이 왼쪽 귀(+-1 로 맞춘 값).
         */
        final double[][] original = new double[2][TAPS];

        /**
         * The minimum phase kernels, [ear][tap].
         * <p>
         * 최소 위상 커널 [귀][탭].
         */
        final double[][] minPhase = new double[2][TAPS];

        /**
         * The delay of every ear in samples at 44100 Hz.
         * <p>
         * 귀마다의 지연(44100 Hz 샘플).
         */
        final double[] delay = new double[2];

        /**
         * Creates a measurement.
         * <p>
         * 측정을 만든다.
         *
         * @param elevation the elevation / 고도
         * @param azimuth   the azimuth / 방위각
         */
        Measurement(int elevation, int azimuth) {
            this.elevation = elevation;
            this.azimuth = azimuth;
        }
    }

    /**
     * Private constructor: the class only has static members.
     * <p>
     * 비공개 생성자: 이 클래스는 정적 멤버만 가진다.
     */
    private HrtfGen() {
    }

    /**
     * Entry point.
     * <p>
     * 진입점.
     *
     * @param args the arguments: the archive, the output file and the optional <code>--retrieved</code> date / 인수: 압축 파일, 출력 파일, 선택 사항인
     *             <code>--retrieved</code> 날짜
     */
    public static void main(String[] args) {
        System.exit(run(args));
    }

    /**
     * Runs the tool.
     * <p>
     * 도구를 실행한다.
     *
     * @param args the arguments / 인수
     * @return the exit code / 종료 코드
     */
    static int run(String[] args) {
        String input = null;
        String output = null;
        String retrieved = "unknown";
        // 인수를 차례로 읽는다.
        for (int i = 0; i < args.length; i++) {
            if ("--retrieved".equals(args[i]) && i + 1 < args.length) {
                retrieved = args[++i];
            } else if (input == null) {
                input = args[i];
            } else if (output == null) {
                output = args[i];
            } else {
                return usage();
            }
        }
        if (input == null || output == null) {
            return usage();
        }
        try {
            long crc = crcOf(new File(input));
            List<Measurement> list = readArchive(new File(input));
            analyse(list);
            writeFile(new File(output), list, input, crc, retrieved);
            return 0;
        } catch (IOException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return 2;
        }
    }

    /**
     * Prints the usage.
     * <p>
     * 사용법을 출력한다.
     *
     * @return the exit code 1 / 종료 코드 1
     */
    private static int usage() {
        System.err.println("usage: HrtfGen <compact.zip> <output.hrtf> [--retrieved YYYY-MM-DD]");
        return 1;
    }

    /**
     * Computes the CRC-32 of a file (recorded in the output as the identity of the source).
     * <p>
     * 파일의 CRC-32 를 계산한다(출력에 원본의 식별로 기록한다).
     *
     * @param f the file / 파일
     * @return the checksum / 체크섬
     * @throws IOException if the file cannot be read / 파일을 읽지 못하면
     */
    private static long crcOf(File f) throws IOException {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        try {
            CRC32 crc = new CRC32();
            byte[] buf = new byte[8192];
            int n;
            // 파일 끝까지 읽으며 체크섬을 갱신한다.
            while ((n = in.read(buf)) > 0) {
                crc.update(buf, 0, n);
            }
            return crc.getValue();
        } finally {
            in.close();
        }
    }

    /**
     * Reads the measurements from the archive, in memory (nothing is extracted to disk).
     * <p>
     * 압축 파일에서 측정을 읽는다. 메모리에서만 읽고 디스크에 풀지 않는다.
     *
     * @param zip the archive / 압축 파일
     * @return the measurements sorted by elevation and azimuth / 고도와 방위각으로 정렬한 측정
     * @throws IOException if the archive is not the expected data set / 압축 파일이 기대한 자료가 아니면
     */
    static List<Measurement> readArchive(File zip) throws IOException {
        ZipFile z = new ZipFile(zip);
        try {
            TreeMap<Long, Measurement> sorted = new TreeMap<Long, Measurement>();
            Enumeration<? extends ZipEntry> en = z.entries();
            // 항목을 하나씩 보며 이름이 맞는 WAV 파일만 읽는다.
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) {
                    continue;
                }
                Matcher m = NAME.matcher(e.getName());
                if (!m.matches()) {
                    throw new IOException("unexpected entry in the archive: " + e.getName());
                }
                int elevation = Integer.parseInt(m.group(1));
                int azimuth = Integer.parseInt(m.group(3));
                if (elevation != Integer.parseInt(m.group(2))) {
                    throw new IOException("the elevation of the directory and of the file differ: " + e.getName());
                }
                Measurement meas = new Measurement(elevation, azimuth);
                readWav(z.getInputStream(e), meas, e.getName());
                long key = (long) (elevation + 1000) * 100000L + azimuth;
                if (sorted.put(key, meas) != null) {
                    throw new IOException("two files for one direction: " + e.getName());
                }
            }
            if (sorted.isEmpty()) {
                throw new IOException("the archive has no measurements");
            }
            return new ArrayList<Measurement>(sorted.values());
        } finally {
            z.close();
        }
    }

    /**
     * Reads one WAV file: a RIFF file with a PCM format of two channels of 16 bits at 44100 Hz and a data chunk of 128 frames.
     * <p>
     * WAV 파일 하나를 읽는다. 44100 Hz 16비트 2 채널 PCM 형식과 128 프레임의 data 청크를 가진 RIFF 파일이다.
     *
     * @param in   the stream of the entry / 항목의 스트림
     * @param meas the measurement that receives the samples / 샘플을 받는 측정
     * @param name the name of the entry for messages / 메시지에 쓸 항목 이름
     * @throws IOException if the file is not what is expected / 파일이 기대한 것이 아니면
     */
    private static void readWav(InputStream in, Measurement meas, String name) throws IOException {
        byte[] b;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            // 항목 전체를 메모리로 읽는다.
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            b = bos.toByteArray();
        } finally {
            in.close();
        }
        if (b.length != 44 + TAPS * 4 || b[0] != 'R' || b[1] != 'I' || b[2] != 'F' || b[3] != 'F' || b[8] != 'W' || b[9] != 'A' || b[10] != 'V'
                || b[11] != 'E') {
            throw new IOException("not the expected WAV file: " + name);
        }
        int format = le16(b, 20);
        int channels = le16(b, 22);
        int rate = le32(b, 24);
        int bits = le16(b, 34);
        int dataSize = le32(b, 40);
        if (format != 1 || channels != 2 || rate != RATE || bits != 16 || dataSize != TAPS * 4 || b[36] != 'd' || b[37] != 'a' || b[38] != 't' || b[39] != 'a') {
            throw new IOException("unexpected WAV format in " + name);
        }
        // 프레임마다 왼쪽과 오른쪽 귀의 16비트 샘플을 +-1 로 맞춰 읽는다.
        for (int t = 0; t < TAPS; t++) {
            meas.original[0][t] = (short) le16(b, 44 + t * 4) / 32768.0;
            meas.original[1][t] = (short) le16(b, 46 + t * 4) / 32768.0;
        }
    }

    /**
     * Reads a little-endian 16-bit unsigned value.
     * <p>
     * 리틀 엔디언 16비트 부호 없는 값을 읽는다.
     *
     * @param b   the bytes / 바이트
     * @param pos the position / 위치
     * @return the value / 값
     */
    private static int le16(byte[] b, int pos) {
        return (b[pos] & 0xFF) | ((b[pos + 1] & 0xFF) << 8);
    }

    /**
     * Reads a little-endian 32-bit value.
     * <p>
     * 리틀 엔디언 32비트 값을 읽는다.
     *
     * @param b   the bytes / 바이트
     * @param pos the position / 위치
     * @return the value / 값
     */
    private static int le32(byte[] b, int pos) {
        return le16(b, pos) | (le16(b, pos + 2) << 16);
    }

    /**
     * Splits every impulse response into the minimum phase kernel and the delay, removes the common delay and prints the statistics.
     * <p>
     * 임펄스 응답마다 최소 위상 커널과 지연으로 나누고, 공통 지연을 빼고, 통계를 출력한다.
     *
     * @param list the measurements / 측정
     */
    static void analyse(List<Measurement> list) {
        double minDelay = Double.MAX_VALUE;
        double worstDb = 0.0;
        double worstEnergy = 0.0;
        // 측정과 귀마다 최소 위상 커널과 지연을 구한다.
        for (int i = 0; i < list.size(); i++) {
            Measurement m = list.get(i);
            // 두 귀를 차례로 처리한다.
            for (int ear = 0; ear < 2; ear++) {
                double[] result = new double[3];
                m.minPhase[ear] = minPhase(m.original[ear], result);
                m.delay[ear] = result[0];
                worstDb = Math.max(worstDb, result[1]);
                worstEnergy = Math.max(worstEnergy, result[2]);
                minDelay = Math.min(minDelay, m.delay[ear]);
            }
        }
        // 모든 측정에 공통인 지연(측정계의 전파 지연)을 뺀다.
        for (int i = 0; i < list.size(); i++) {
            Measurement m = list.get(i);
            m.delay[0] -= minDelay;
            m.delay[1] -= minDelay;
        }
        double maxDelay = 0.0;
        // 가장 큰 지연을 찾는다.
        for (int i = 0; i < list.size(); i++) {
            maxDelay = Math.max(maxDelay, Math.max(list.get(i).delay[0], list.get(i).delay[1]));
        }
        System.err.println("# measurements: " + list.size());
        System.err.println("# common delay removed: " + minDelay + " samples; largest remaining delay: " + maxDelay + " samples");
        System.err.println("# largest magnitude deviation of the truncated minimum phase kernel (100 Hz - 16 kHz, bins within 40 dB of the peak): " + worstDb + " dB");
        System.err.println("# largest energy fraction cut off by the truncation: " + worstEnergy);
    }

    /**
     * Computes the minimum phase kernel of an impulse response with the real cepstrum and estimates its delay. The result array holds
     * the delay in samples, the largest deviation of the magnitude response of the truncated kernel in decibels between 100 Hz and
     * 16 kHz, and the energy fraction that the truncation cuts off.
     * <p>
     * 실수 켑스트럼으로 임펄스 응답의 최소 위상 커널을 구하고 지연을 추정한다. 결과 배열은 지연(샘플), 자른 커널의 크기 응답이 100 Hz ~ 16 kHz 에서 벗어나는
     * 최대량(dB), 자르기로 잘려 나가는 에너지 비율을 담는다.
     *
     * @param h      the impulse response / 임펄스 응답
     * @param result the array of three values that receives the delay, the deviation and the energy fraction / 지연, 벗어남, 에너지 비율을 받는 값 3개의 배열
     * @return the minimum phase kernel with the same number of taps / 탭 수가 같은 최소 위상 커널
     */
    static double[] minPhase(double[] h, double[] result) {
        int n = FFT_SIZE;
        double[] re = new double[n];
        double[] im = new double[n];
        System.arraycopy(h, 0, re, 0, h.length);
        fft(re, im, false);
        double[] hr = re.clone();
        double[] hi = im.clone();
        double peak = 0.0;
        // 스펙트럼의 최대 크기를 찾는다.
        for (int k = 0; k < n; k++) {
            peak = Math.max(peak, Math.hypot(hr[k], hi[k]));
        }
        double floor = peak * 1e-5;
        // 로그 크기 스펙트럼을 만든다. 너무 작은 값은 바닥으로 막는다.
        for (int k = 0; k < n; k++) {
            re[k] = Math.log(Math.max(Math.hypot(hr[k], hi[k]), floor));
            im[k] = 0.0;
        }
        fft(re, im, true);
        // 실수 켑스트럼을 접어 인과적인 켑스트럼으로 만든다.
        for (int k = 0; k < n; k++) {
            double c = re[k] / n;
            if (k == 0 || k == n / 2) {
                re[k] = c;
            } else if (k < n / 2) {
                re[k] = 2.0 * c;
            } else {
                re[k] = 0.0;
            }
            im[k] = 0.0;
        }
        fft(re, im, false);
        double[] mr = new double[n];
        double[] mi = new double[n];
        // 접은 켑스트럼의 스펙트럼을 지수 함수로 바꿔 최소 위상 스펙트럼을 만든다.
        for (int k = 0; k < n; k++) {
            double mag = Math.exp(re[k]);
            mr[k] = mag * Math.cos(im[k]);
            mi[k] = mag * Math.sin(im[k]);
        }
        double[] fr = mr.clone();
        double[] fi = mi.clone();
        fft(fr, fi, true);
        double[] kernel = new double[h.length];
        double total = 0.0;
        double kept = 0.0;
        // 앞쪽 탭만 남기고 잘려 나가는 에너지를 합한다.
        for (int t = 0; t < n; t++) {
            double v = fr[t] / n;
            total += v * v;
            if (t < kernel.length) {
                kernel[t] = v;
                kept += v * v;
            }
        }
        result[2] = total > 0 ? (total - kept) / total : 0.0;
        // 초과 위상의 저주파 군지연(샘플)을 구한다: 이웃한 빈 사이의 위상 차이를 가중 평균한다.
        double weighted = 0.0;
        double weights = 0.0;
        // 낮은 주파수 빈마다 이웃 빈과의 위상 차이에서 군지연을 구해 크기로 가중한다.
        for (int k = 1; k < DELAY_BINS; k++) {
            double ar = hr[k] * mr[k] + hi[k] * mi[k];
            double ai = hi[k] * mr[k] - hr[k] * mi[k];
            double br = hr[k + 1] * mr[k + 1] + hi[k + 1] * mi[k + 1];
            double bi = hi[k + 1] * mr[k + 1] - hr[k + 1] * mi[k + 1];
            double dr = br * ar + bi * ai;
            double di = bi * ar - br * ai;
            double dphase = Math.atan2(di, dr);
            double w = Math.hypot(hr[k], hi[k]) * Math.hypot(hr[k + 1], hi[k + 1]);
            weighted += w * (-dphase / (2.0 * Math.PI / n));
            weights += w;
        }
        result[0] = weights > 0 ? weighted / weights : 0.0;
        // 잘린 커널의 크기 응답이 원래 응답과 얼마나 다른지 100 Hz ~ 16 kHz 에서 잰다.
        double[] kr = new double[n];
        double[] ki = new double[n];
        System.arraycopy(kernel, 0, kr, 0, kernel.length);
        fft(kr, ki, false);
        double worst = 0.0;
        int lo = (int) Math.ceil(100.0 * n / RATE);
        int hiBin = (int) Math.floor(16000.0 * n / RATE);
        // 범위의 빈마다 크기 응답의 차이(dB)를 구해 가장 큰 값을 보관한다.
        for (int k = lo; k <= hiBin; k++) {
            double a = Math.hypot(hr[k], hi[k]);
            double b = Math.hypot(kr[k], ki[k]);
            if (a > peak * 0.01) {
                worst = Math.max(worst, Math.abs(20.0 * Math.log10(b / a)));
            }
        }
        result[1] = worst;
        return kernel;
    }

    /**
     * The cosine table of the last FFT size (the tool uses one size).
     * <p>
     * 마지막 FFT 크기의 코사인 표(도구는 한 가지 크기만 쓴다).
     */
    private static double[] cosCache;

    /**
     * The sine table of the last FFT size.
     * <p>
     * 마지막 FFT 크기의 사인 표.
     */
    private static double[] sinCache;

    /**
     * Returns the table of <code>cos(2 pi k / n)</code> for <code>k</code> below <code>n / 2</code>, building it when the size changed.
     * <p>
     * <code>k</code> 가 <code>n / 2</code> 미만인 <code>cos(2 pi k / n)</code> 표를 반환한다. 크기가 바뀌었으면 새로 만든다.
     *
     * @param n the FFT size / FFT 크기
     * @return the table / 표
     */
    private static double[] cosTable(int n) {
        if (cosCache == null || cosCache.length != n / 2) {
            buildTables(n);
        }
        return cosCache;
    }

    /**
     * Returns the table of <code>sin(2 pi k / n)</code> for <code>k</code> below <code>n / 2</code>, building it when the size changed.
     * <p>
     * <code>k</code> 가 <code>n / 2</code> 미만인 <code>sin(2 pi k / n)</code> 표를 반환한다. 크기가 바뀌었으면 새로 만든다.
     *
     * @param n the FFT size / FFT 크기
     * @return the table / 표
     */
    private static double[] sinTable(int n) {
        if (sinCache == null || sinCache.length != n / 2) {
            buildTables(n);
        }
        return sinCache;
    }

    /**
     * Builds the cosine and sine tables of a size.
     * <p>
     * 크기에 맞는 코사인과 사인 표를 만든다.
     *
     * @param n the FFT size / FFT 크기
     */
    private static void buildTables(int n) {
        cosCache = new double[n / 2];
        sinCache = new double[n / 2];
        // 표의 각 점을 계산한다.
        for (int k = 0; k < n / 2; k++) {
            cosCache[k] = Math.cos(2.0 * Math.PI * k / n);
            sinCache[k] = Math.sin(2.0 * Math.PI * k / n);
        }
    }

    /**
     * An in-place complex FFT of the size {@link #FFT_SIZE}; the inverse transform is not divided by the size.
     * <p>
     * 크기 {@link #FFT_SIZE} 의 제자리 복소 FFT. 역변환은 크기로 나누지 않는다.
     *
     * @param re      the real parts / 실수부
     * @param im      the imaginary parts / 허수부
     * @param inverse true for the inverse transform / 역변환이면 true
     */
    static void fft(double[] re, double[] im, boolean inverse) {
        int n = re.length;
        int bits = Integer.numberOfTrailingZeros(n);
        // 비트 반전 순서로 재배열한다.
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> (32 - bits);
            if (j > i) {
                double t = re[i];
                re[i] = re[j];
                re[j] = t;
                t = im[i];
                im[i] = im[j];
                im[j] = t;
            }
        }
        double sign = inverse ? 1.0 : -1.0;
        // 길이 2, 4, 8, ... 의 나비 연산을 차례로 한다.
        for (int len = 2; len <= n; len <<= 1) {
            int step = n / len;
            for (int start = 0; start < n; start += len) {
                // 구간 안의 나비 연산을 계산한다.
                for (int k = 0; k < len / 2; k++) {
                    double wr = cosTable(n)[k * step];
                    double wi = sign * sinTable(n)[k * step];
                    int a = start + k;
                    int bIdx = a + len / 2;
                    double tr = re[bIdx] * wr - im[bIdx] * wi;
                    double ti = re[bIdx] * wi + im[bIdx] * wr;
                    re[bIdx] = re[a] - tr;
                    im[bIdx] = im[a] - ti;
                    re[a] += tr;
                    im[a] += ti;
                }
            }
        }
    }

    /**
     * The credit that is stored in the file: the citation that the terms of the data require, and where the data came from.
     * <p>
     * 파일에 저장하는 출처 표기: 자료의 이용 조건이 요구하는 인용과 자료의 출처.
     *
     * @param input     the path of the archive / 압축 파일 경로
     * @param crc       the CRC-32 of the archive / 압축 파일의 CRC-32
     * @param retrieved the date of the download / 내려받은 날짜
     * @return the text / 문장
     */
    private static String credit(String input, long crc, String retrieved) {
        return "HRTF Measurements of a KEMAR Dummy-Head Microphone, Bill Gardner and Keith Martin, MIT Media Lab Machine Listening Group, "
                + "May 1994 (compact data set, last revised 2000). Copyright 1994 by the MIT Media Laboratory. The data is provided free with no "
                + "restrictions on use, provided the authors are cited when the data is used in any research or commercial application. "
                + "Source: https://sound.media.mit.edu/resources/KEMAR.html (compact.zip, retrieved " + retrieved + ", CRC-32 "
                + Long.toHexString(crc) + "). Converted by tools/hrtfgen of SwingWebPanel: minimum phase kernels of 128 taps at 44100 Hz "
                + "and a delay per ear.";
    }

    /**
     * Writes the output file.
     * <p>
     * 출력 파일을 쓴다.
     *
     * @param out       the output file / 출력 파일
     * @param list      the measurements / 측정
     * @param input     the path of the archive / 압축 파일 경로
     * @param crc       the CRC-32 of the archive / 압축 파일의 CRC-32
     * @param retrieved the date of the download / 내려받은 날짜
     * @throws IOException if the file cannot be written / 파일을 쓰지 못하면
     */
    static void writeFile(File out, List<Measurement> list, String input, long crc, String retrieved) throws IOException {
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(out)));
        try {
            os.write(MAGIC.getBytes("US-ASCII"));
            os.writeInt(VERSION);
            os.writeInt(RATE);
            os.writeInt(TAPS);
            List<Integer> elevations = new ArrayList<Integer>();
            // 고도 목록을 만든다.
            for (int i = 0; i < list.size(); i++) {
                Integer e = Integer.valueOf(list.get(i).elevation);
                if (!elevations.contains(e)) {
                    elevations.add(e);
                }
            }
            os.writeInt(elevations.size());
            // 고도마다 고도, 방위각 수, 방위각 목록을 쓴다.
            for (int e = 0; e < elevations.size(); e++) {
                int elev = elevations.get(e).intValue();
                int count = 0;
                // 이 고도의 측정 수를 센다.
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).elevation == elev) {
                        count++;
                    }
                }
                os.writeShort(elev);
                os.writeShort(count);
                // 이 고도의 방위각을 쓴다.
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).elevation == elev) {
                        os.writeShort(list.get(i).azimuth);
                    }
                }
            }
            // 측정마다 지연 둘과 커널 둘을 순서대로 쓴다.
            for (int i = 0; i < list.size(); i++) {
                Measurement m = list.get(i);
                os.writeFloat((float) m.delay[0]);
                os.writeFloat((float) m.delay[1]);
                // 귀마다 최소 위상 커널의 탭을 쓴다.
                for (int ear = 0; ear < 2; ear++) {
                    // 커널의 각 탭을 float 로 쓴다.
                    for (int t = 0; t < TAPS; t++) {
                        os.writeFloat((float) m.minPhase[ear][t]);
                    }
                }
            }
            byte[] credit = credit(input, crc, retrieved).getBytes("UTF-8");
            os.writeInt(credit.length);
            os.write(credit);
        } finally {
            os.close();
        }
    }
}
