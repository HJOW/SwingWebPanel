package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The head-related transfer functions (HRTF) that the <code>HRTF</code> panning model of the <code>PannerNode</code> uses: the
 * measurements of the MIT KEMAR dummy head microphone, loaded from the resource <code>kemar-compact.hrtf</code> that sits next to this
 * class (it is part of the product JAR and is read from the class path).
 * <p>
 * <b>Credit.</b> HRTF Measurements of a KEMAR Dummy-Head Microphone, Bill Gardner and Keith Martin, MIT Media Lab Machine Listening
 * Group, 1994 (the compact data set; <a href="https://sound.media.mit.edu/resources/KEMAR.html">https://sound.media.mit.edu/resources/KEMAR.html</a>,
 * <code>compact.zip</code>, retrieved 2026-11-22, last modified 2000-07-14). Copyright 1994 by the MIT Media Laboratory. The data is
 * provided free with no restrictions on use, provided the authors are cited when the data is used in any research or commercial
 * application. An application that uses the HRTF panning of SwingWebPanel must keep this credit.
 * <p>
 * The resource was made by <code>tools/hrtfgen</code> from the 368 measured directions (elevations -40 to 90 degrees, azimuths 0 to 180
 * degrees to the right; the left side follows from the symmetry of the head): for every direction and ear a 128 tap minimum phase
 * kernel at 44100 Hz and a delay in samples (the interaural time difference is the difference of the two delays). The format is
 * big-endian: the magic <code>SWPHRTF1</code>, the version 1, the sample rate, the number of taps, the number of elevations, for every
 * elevation its angle and its azimuths, then for every direction the two delays and the two kernels (floats), and the credit text.
 * <p>
 * 오디오 엔진의 <code>PannerNode</code> 가 <code>HRTF</code> 패닝 모델에 쓰는 머리전달함수(HRTF): MIT KEMAR 더미 헤드 마이크의 측정 자료이며, 이 클래스 옆의
 * 자원 <code>kemar-compact.hrtf</code> 에서 읽는다(제품 JAR 에 들어 있고 클래스 경로에서 읽는다).
 * <p>
 * <b>저자 표기.</b> HRTF Measurements of a KEMAR Dummy-Head Microphone, Bill Gardner and Keith Martin, MIT Media Lab Machine Listening Group, 1994(compact 자료.
 * <a href="https://sound.media.mit.edu/resources/KEMAR.html">https://sound.media.mit.edu/resources/KEMAR.html</a>, <code>compact.zip</code>, 2026-11-22 조회,
 * 최종 수정 2000-07-14). 저작권은 MIT Media Laboratory(1994)에 있다. 이 자료는 연구나 상업 용도에 쓸 때 저자를 표기하는 조건으로 제한 없이 자유롭게 쓸 수 있다.
 * SwingWebPanel 의 HRTF 패닝을 쓰는 응용 프로그램은 이 저자 표기를 유지해야 한다.
 * <p>
 * 자원은 <code>tools/hrtfgen</code> 이 측정한 368 방향(고도 -40 ~ 90도, 오른쪽 방위각 0 ~ 180도. 왼쪽은 머리의 대칭에서 나온다)으로 만들었다. 방향과 귀마다 44100 Hz 의
 * 128탭 최소 위상 커널과 지연(샘플)이 있다(두 귀 사이의 시간차는 두 지연의 차이). 형식은 빅 엔디언이다: 매직 <code>SWPHRTF1</code>, 버전 1, 샘플링 주파수, 탭 수, 고도
 * 수, 고도마다 각도와 방위각, 방향마다 지연 둘과 커널 둘(float), 출처 표기 문장.
 */
public final class HrtfDatabase {

    /**
     * The name of the resource, relative to this package.
     * <p>
     * 자원 이름(이 패키지 기준 상대 경로).
     */
    public static final String RESOURCE = "kemar-compact.hrtf";

    /**
     * The sanity limit for the number of taps of a kernel.
     * <p>
     * 커널 탭 수의 안전 한도.
     */
    private static final int MAX_TAPS = 4096;

    /**
     * The sanity limit for the number of directions.
     * <p>
     * 방향 수의 안전 한도.
     */
    private static final int MAX_DIRECTIONS = 100000;

    /**
     * The sample rate of the data in Hz.
     * <p>
     * 자료의 샘플링 주파수(Hz).
     */
    private final int sampleRate;

    /**
     * The number of taps of every kernel.
     * <p>
     * 모든 커널의 탭 수.
     */
    private final int taps;

    /**
     * The elevations in degrees, ascending.
     * <p>
     * 고도(도), 오름차순.
     */
    private final int[] elevations;

    /**
     * The azimuths in degrees of every elevation, ascending.
     * <p>
     * 고도마다의 방위각(도), 오름차순.
     */
    private final int[][] azimuths;

    /**
     * The minimum phase kernels, [elevation][azimuth][ear][tap], ear 0 is the left ear.
     * <p>
     * 최소 위상 커널 [고도][방위각][귀][탭]. 귀 0 이 왼쪽 귀.
     */
    private final float[][][][] kernels;

    /**
     * The delays in samples at the sample rate of the data, [elevation][azimuth][ear].
     * <p>
     * 자료의 샘플링 주파수에서의 지연(샘플) [고도][방위각][귀].
     */
    private final float[][][] delays;

    /**
     * The credit text of the resource.
     * <p>
     * 자원의 출처 표기 문장.
     */
    private final String credit;

    /**
     * Creates a database from parsed data.
     * <p>
     * 읽어 낸 자료로 데이터베이스를 만든다.
     *
     * @param sampleRate the sample rate / 샘플링 주파수
     * @param taps       the number of taps / 탭 수
     * @param elevations the elevations / 고도
     * @param azimuths   the azimuths per elevation / 고도마다의 방위각
     * @param kernels    the kernels / 커널
     * @param delays     the delays / 지연
     * @param credit     the credit text / 출처 표기 문장
     */
    private HrtfDatabase(int sampleRate, int taps, int[] elevations, int[][] azimuths, float[][][][] kernels, float[][][] delays, String credit) {
        this.sampleRate = sampleRate;
        this.taps = taps;
        this.elevations = elevations;
        this.azimuths = azimuths;
        this.kernels = kernels;
        this.delays = delays;
        this.credit = credit;
    }

    /**
     * Loads the data of the product from the resource next to this class.
     * <p>
     * 이 클래스 옆의 자원에서 제품의 자료를 읽는다.
     *
     * @return the database / 데이터베이스
     * @throws IOException if the resource is missing or damaged / 자원이 없거나 손상되었으면
     */
    public static HrtfDatabase load() throws IOException {
        InputStream in = HrtfDatabase.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            throw new IOException("the resource " + RESOURCE + " was not found next to " + HrtfDatabase.class.getName());
        }
        return read(in);
    }

    /**
     * Reads the data from a stream in the format of the resource; the stream is closed.
     * <p>
     * 자원 형식의 스트림에서 자료를 읽는다. 스트림은 닫는다.
     *
     * @param stream the stream / 스트림
     * @return the database / 데이터베이스
     * @throws IOException if the data is not valid / 자료가 올바르지 않으면
     */
    public static HrtfDatabase read(InputStream stream) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(stream, 65536));
        try {
            byte[] magic = new byte[8];
            in.readFully(magic);
            if (!"SWPHRTF1".equals(new String(magic, "US-ASCII"))) {
                throw new IOException("not an HRTF resource: bad magic");
            }
            int version = in.readInt();
            if (version != 1) {
                throw new IOException("unknown HRTF resource version " + version);
            }
            int rate = in.readInt();
            int taps = in.readInt();
            int count = in.readInt();
            if (rate < 8000 || rate > 192000 || taps < 8 || taps > MAX_TAPS || count < 1 || count > 1000) {
                throw new IOException("implausible HRTF header: rate " + rate + ", taps " + taps + ", elevations " + count);
            }
            int[] elevations = new int[count];
            int[][] azimuths = new int[count][];
            int total = 0;
            // 고도마다 각도와 방위각 목록을 읽는다.
            for (int e = 0; e < count; e++) {
                elevations[e] = in.readShort();
                int n = in.readShort();
                if (n < 1 || n > 1000 || (e > 0 && elevations[e] <= elevations[e - 1])) {
                    throw new IOException("bad elevation entry " + e);
                }
                azimuths[e] = new int[n];
                // 방위각은 오름차순이어야 한다.
                for (int a = 0; a < n; a++) {
                    azimuths[e][a] = in.readShort();
                    if (a > 0 && azimuths[e][a] <= azimuths[e][a - 1]) {
                        throw new IOException("the azimuths of elevation " + elevations[e] + " are not ascending");
                    }
                }
                total += n;
                if (total > MAX_DIRECTIONS) {
                    throw new IOException("too many directions");
                }
            }
            float[][][][] kernels = new float[count][][][];
            float[][][] delays = new float[count][][];
            // 방향마다 지연 둘과 커널 둘을 읽는다.
            for (int e = 0; e < count; e++) {
                int n = azimuths[e].length;
                kernels[e] = new float[n][2][taps];
                delays[e] = new float[n][2];
                // 이 고도의 방위각마다 지연 둘과 커널 둘을 읽는다.
                for (int a = 0; a < n; a++) {
                    delays[e][a][0] = readFinite(in);
                    delays[e][a][1] = readFinite(in);
                    // 두 귀의 커널 탭을 읽는다.
                    for (int ear = 0; ear < 2; ear++) {
                        for (int t = 0; t < taps; t++) {
                            kernels[e][a][ear][t] = readFinite(in);
                        }
                    }
                }
            }
            int length = in.readInt();
            if (length < 0 || length > 100000) {
                throw new IOException("bad credit length");
            }
            byte[] text = new byte[length];
            in.readFully(text);
            return new HrtfDatabase(rate, taps, elevations, azimuths, kernels, delays, new String(text, "UTF-8"));
        } finally {
            in.close();
        }
    }

    /**
     * Reads a float that must be finite.
     * <p>
     * 유한해야 하는 float 를 읽는다.
     *
     * @param in the stream / 스트림
     * @return the value / 값
     * @throws IOException if the value is NaN or infinite or the stream ends / 값이 NaN 이나 무한대이거나 스트림이 끝나면
     */
    private static float readFinite(DataInputStream in) throws IOException {
        float v = in.readFloat();
        if (Float.isNaN(v) || Float.isInfinite(v)) {
            throw new IOException("the HRTF data has a value that is not finite");
        }
        return v;
    }

    /**
     * Returns the sample rate of the data in Hz.
     * <p>
     * 자료의 샘플링 주파수(Hz)를 반환한다.
     *
     * @return the sample rate / 샘플링 주파수
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Returns the number of taps of every kernel.
     * <p>
     * 모든 커널의 탭 수를 반환한다.
     *
     * @return the number of taps / 탭 수
     */
    public int getTaps() {
        return taps;
    }

    /**
     * Returns the number of elevations.
     * <p>
     * 고도의 수를 반환한다.
     *
     * @return the number / 수
     */
    public int getElevationCount() {
        return elevations.length;
    }

    /**
     * Returns an elevation in degrees.
     * <p>
     * 고도(도)를 반환한다.
     *
     * @param e the index of the elevation / 고도의 인덱스
     * @return the elevation / 고도
     */
    public int getElevation(int e) {
        return elevations[e];
    }

    /**
     * Returns the number of measured azimuths of an elevation.
     * <p>
     * 고도의 측정한 방위각 수를 반환한다.
     *
     * @param e the index of the elevation / 고도의 인덱스
     * @return the number / 수
     */
    public int getAzimuthCount(int e) {
        return azimuths[e].length;
    }

    /**
     * Returns a measured azimuth in degrees (0 is the front, 90 is to the right, 180 is the back).
     * <p>
     * 측정한 방위각(도)을 반환한다(0 은 정면, 90 은 오른쪽, 180 은 뒤).
     *
     * @param e the index of the elevation / 고도의 인덱스
     * @param a the index of the azimuth / 방위각의 인덱스
     * @return the azimuth / 방위각
     */
    public int getAzimuth(int e, int a) {
        return azimuths[e][a];
    }

    /**
     * Returns the total number of measured directions.
     * <p>
     * 측정한 방향의 총 수를 반환한다.
     *
     * @return the number / 수
     */
    public int getDirectionCount() {
        int n = 0;
        // 고도마다의 방위각 수를 더한다.
        for (int e = 0; e < azimuths.length; e++) {
            n += azimuths[e].length;
        }
        return n;
    }

    /**
     * Returns the minimum phase kernel of a direction and an ear. The array belongs to the database and must not be changed.
     * <p>
     * 방향과 귀의 최소 위상 커널을 반환한다. 배열은 데이터베이스의 것이므로 바꾸면 안 된다.
     *
     * @param e   the index of the elevation / 고도의 인덱스
     * @param a   the index of the azimuth / 방위각의 인덱스
     * @param ear 0 for the left ear, 1 for the right ear / 0 이면 왼쪽 귀, 1 이면 오른쪽 귀
     * @return the kernel / 커널
     */
    public float[] getKernel(int e, int a, int ear) {
        return kernels[e][a][ear];
    }

    /**
     * Returns the delay of a direction and an ear in samples at {@link #getSampleRate()}. The delay that all directions share was
     * removed; the difference of the two ears is the interaural time difference (positive for the ear that is farther from the source).
     * <p>
     * 방향과 귀의 지연을 {@link #getSampleRate()} 의 샘플로 반환한다. 모든 방향에 공통인 지연은 뺐다. 두 귀의 차이가 두 귀 사이의 시간차이다(음원에서 먼 귀가 양수).
     *
     * @param e   the index of the elevation / 고도의 인덱스
     * @param a   the index of the azimuth / 방위각의 인덱스
     * @param ear 0 for the left ear, 1 for the right ear / 0 이면 왼쪽 귀, 1 이면 오른쪽 귀
     * @return the delay / 지연
     */
    public float getDelay(int e, int a, int ear) {
        return delays[e][a][ear];
    }

    /**
     * Returns the credit text of the resource (the authors, the source, the retrieval date).
     * <p>
     * 자원의 출처 표기 문장(저자, 출처, 조회 날짜)을 반환한다.
     *
     * @return the text / 문장
     */
    public String getCredit() {
        return credit;
    }
}
