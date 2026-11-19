package org.duckdns.hjow.swingwebpanel.media.audio.testkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit.Out;

/**
 * Test-only Vorbis encoder written from the Vorbis I specification and independent of the decoder. It builds complete Ogg Vorbis files
 * with codebooks of three kinds of lookup (none, implicit grid, explicit table) in ordered, sparse and plain form, a floor 1 with
 * subclasses and prediction coding, residues 0, 1 and 2 with several passes, several submaps, channel coupling, short and long
 * blocks in any pattern, and granule positions with a trimmed end. Besides the file it computes, by direct (quadratic) formulas and a
 * time-line accumulation of the windowed blocks, the exact PCM that a correct decoder must produce from the chosen quantized
 * data (the <em>model</em>), so the decoder can be compared sample by sample.
 * <p>
 * Vorbis I 규격에서 디코더와 별개로 직접 작성한 시험 전용 Vorbis 인코더. 세 종류의 조회(없음, 암시적 격자, 명시적 표)를 ordered, sparse, 일반 형식으로 쓰는
 * 코드북, 서브클래스와 예측 부호화가 있는 플로어 1, 여러 패스의 잔차 0/1/2, 여러 하위 맵, 채널 결합, 임의 패턴의 짧은/긴 블록, 끝을 잘라 내는 그래뉼 위치를
 * 갖춘 완전한 Ogg Vorbis 파일을 만든다. 파일 외에, 선택한 양자화 데이터로부터 올바른 디코더가 만들어야 하는 정확한 PCM(<em>모델</em>)을 직접(제곱 시간)
 * 공식과 창을 씌운 블록의 시간축 누적으로 계산하므로 디코더와 샘플 단위로 비교할 수 있다.
 */
public final class VorbisWriter {

    /**
     * Encoder options.
     * <p>
     * 인코더 옵션.
     */
    public static final class Config {
        /**
         * Number of channels.
         * <p>
         * 채널 수.
         */
        public int channels = 1;

        /**
         * Sample rate.
         * <p>
         * 샘플링 주파수.
         */
        public int rate = 44100;

        /**
         * Short block size.
         * <p>
         * 짧은 블록 크기.
         */
        public int blockShort = 256;

        /**
         * Long block size.
         * <p>
         * 긴 블록 크기.
         */
        public int blockLong = 2048;

        /**
         * Cyclic pattern of block sizes, 'S' or 'L'.
         * <p>
         * 블록 크기의 반복 패턴. 'S' 또는 'L'.
         */
        public String pattern = "L";

        /**
         * Quantization profile: "vq" (two passes of vector quantization) or "fine" (one pass of scalar quantization).
         * <p>
         * 양자화 프로필: "vq"(벡터 양자화 두 패스) 또는 "fine"(스칼라 양자화 한 패스).
         */
        public String profile = "vq";

        /**
         * Residue type 0, 1 or 2.
         * <p>
         * 잔차 유형 0, 1, 2.
         */
        public int residueType = 1;

        /**
         * Couple channels 0 and 1.
         * <p>
         * 채널 0 과 1 을 결합한다.
         */
        public boolean couple;

        /**
         * Submap of every channel, or null for one submap.
         * <p>
         * 채널마다의 하위 맵. 하위 맵이 하나면 null.
         */
        public int[] mux;

        /**
         * Segments per Ogg page of the audio.
         * <p>
         * 오디오의 Ogg 페이지당 세그먼트 수.
         */
        public int segmentsPerPage = 255;

        /**
         * Use a sloped floor (else flat).
         * <p>
         * 기울어진 플로어를 쓴다(아니면 평평).
         */
        public boolean sloped = true;

        /**
         * Serial number of the stream.
         * <p>
         * 스트림의 시리얼 번호.
         */
        public int serial = 1;
    }

    /**
     * The encoded stream and the model of its decoding.
     * <p>
     * 인코딩된 스트림과 그 디코딩의 모델.
     */
    public static final class Result {
        /**
         * The Ogg file.
         * <p>
         * Ogg 파일.
         */
        public byte[] file;

        /**
         * All packets: three headers, then the audio packets.
         * <p>
         * 모든 패킷: 헤더 셋과 오디오 패킷들.
         */
        public byte[][] packets;

        /**
         * Granule position after every packet.
         * <p>
         * 패킷마다의 그래뉼 위치.
         */
        public long[] granules;

        /**
         * Block size of every audio packet.
         * <p>
         * 오디오 패킷마다의 블록 크기.
         */
        public int[] sizes;

        /**
         * Exact output of a correct decoder per channel, as floats (full scale 1.0), for the frames 0 to the length of the input.
         * <p>
         * 올바른 디코더의 정확한 출력(채널마다 float, 풀 스케일 1.0). 프레임 0 부터 입력 길이까지.
         */
        public float[][] model;

        /**
         * Number of frames of the input.
         * <p>
         * 입력의 프레임 수.
         */
        public int length;
    }

    /**
     * Floor points: positions (X list in setup order) and the target heights.
     * <p>
     * 플로어 점: 위치(설정 순서의 X 목록)와 목표 높이.
     */
    private static final int[] FLOOR_X = { 0, 1024, 24, 100, 300, 700 };

    /**
     * Target heights of a sloped floor (range 128, multiplier 2).
     * <p>
     * 기울어진 플로어의 목표 높이(범위 128, 곱셈 계수 2).
     */
    private static final int[] FLOOR_SLOPED = { 127, 121, 126, 125, 123, 122 };

    /**
     * Target heights of a flat floor.
     * <p>
     * 평평한 플로어의 목표 높이.
     */
    private static final int[] FLOOR_FLAT = { 126, 126, 126, 126, 126, 126 };

    /**
     * Cache of cosine tables by block size.
     * <p>
     * 블록 크기별 코사인 표의 캐시.
     */
    private static final Map<Integer, double[]> COS = new HashMap<Integer, double[]>();

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private VorbisWriter() {
    }

    /**
     * Returns the table cos(2 pi q / (4 n)) for q below 4n.
     * <p>
     * q 가 4n 미만일 때 cos(2 pi q / (4 n)) 표를 반환한다.
     *
     * @param n block size / 블록 크기
     * @return the table / 표
     */
    private static synchronized double[] cosTable(int n) {
        double[] t = COS.get(Integer.valueOf(n));
        if (t == null) {
            t = new double[4 * n];
            // 각도 인덱스마다 코사인 값을 계산한다.
            for (int q = 0; q < t.length; q++) {
                t[q] = Math.cos(2 * Math.PI * q / (4.0 * n));
            }
            COS.put(Integer.valueOf(n), t);
        }
        return t;
    }

    /**
     * The window value of sample i of a block, from the definition in the specification.
     * <p>
     * 규격의 정의에 따른 블록의 i 번째 샘플의 창 값.
     *
     * @param i         sample / 샘플
     * @param n         block size / 블록 크기
     * @param isLong    long block / 긴 블록
     * @param prevLong  previous block is long / 이전 블록이 길다
     * @param nextLong  next block is long / 다음 블록이 길다
     * @param shortSize short block size / 짧은 블록 크기
     * @return the window value / 창 값
     */
    private static double window(int i, int n, boolean isLong, boolean prevLong, boolean nextLong, int shortSize) {
        int ls = 0;
        int le = n / 2;
        if (isLong && !prevLong) {
            ls = n / 4 - shortSize / 4;
            le = n / 4 + shortSize / 4;
        }
        int rs = n / 2;
        int re = n;
        if (isLong && !nextLong) {
            rs = 3 * n / 4 - shortSize / 4;
            re = 3 * n / 4 + shortSize / 4;
        }
        if (i < ls || i >= re) {
            return 0;
        }
        if (i < le) {
            double v = Math.sin((i - ls + 0.5) / (le - ls) * Math.PI / 2);
            return Math.sin(Math.PI / 2 * v * v);
        }
        if (i < rs) {
            return 1;
        }
        double v = Math.sin((re - 1 - i + 0.5) / (re - rs) * Math.PI / 2);
        return Math.sin(Math.PI / 2 * v * v);
    }

    /**
     * Encodes PCM to Ogg Vorbis.
     * <p>
     * PCM 을 Ogg Vorbis 로 인코딩한다.
     *
     * @param x   samples per channel, full scale 1.0 / 채널마다의 샘플. 풀 스케일 1.0
     * @param cfg options / 옵션
     * @return the stream and its model / 스트림과 그 모델
     */
    public static Result encode(float[][] x, Config cfg) {
        int channels = cfg.channels;
        int len = x[0].length;
        Books b = new Books(cfg);
        int submaps = cfg.mux == null ? 1 : maxOf(cfg.mux) + 1;
        // 블록 크기 열을 정한다: 마지막 블록의 중심이 입력 길이에 닿을 때까지.
        List<Integer> sizeList = new ArrayList<Integer>();
        List<Long> centers = new ArrayList<Long>();
        long c = 0;
        int idx = 0;
        // 중심 간격은 이전 블록 크기의 4분의 1 + 이번 블록 크기의 4분의 1 이다.
        while (true) {
            int size = cfg.pattern.charAt(idx % cfg.pattern.length()) == 'L' ? cfg.blockLong : cfg.blockShort;
            if (idx > 0) {
                c += sizeList.get(idx - 1) / 4 + size / 4;
            }
            sizeList.add(Integer.valueOf(size));
            centers.add(Long.valueOf(c));
            idx++;
            if (c >= len && idx >= 2) {
                break;
            }
        }
        int packetsCount = sizeList.size();
        int[] sizes = new int[packetsCount];
        // 리스트를 배열로 옮긴다.
        for (int i = 0; i < packetsCount; i++) {
            sizes[i] = sizeList.get(i).intValue();
        }
        float[][] model = new float[channels][(int) c];
        byte[][] audio = new byte[packetsCount][];
        long[] granule = new long[packetsCount];
        // 블록마다 변환, 양자화, 부호화를 하고 모델을 시간축에 누적한다.
        for (int p = 0; p < packetsCount; p++) {
            int n = sizes[p];
            boolean isLong = isLongAt(cfg, p);
            boolean prevLong = p == 0 || isLongAt(cfg, p - 1);
            boolean nextLong = p == packetsCount - 1 || isLongAt(cfg, p + 1);
            audio[p] = encodePacket(x, cfg, b, submaps, p, n, centers.get(p).longValue(), isLong, prevLong, nextLong, model);
            granule[p] = p == 0 ? 0 : centers.get(p).longValue();
        }
        granule[packetsCount - 1] = Math.min(granule[packetsCount - 1], len);
        float[][] trimmed = new float[channels][len];
        // 모델을 입력 길이로 자른다.
        for (int ch = 0; ch < channels; ch++) {
            System.arraycopy(model[ch], 0, trimmed[ch], 0, Math.min(len, model[ch].length));
        }
        byte[] id = identification(cfg);
        byte[] comment = commentHeader();
        byte[] setup = setupHeader(cfg, b, submaps);
        Result r = new Result();
        r.packets = new byte[3 + packetsCount][];
        r.packets[0] = id;
        r.packets[1] = comment;
        r.packets[2] = setup;
        System.arraycopy(audio, 0, r.packets, 3, packetsCount);
        r.granules = new long[3 + packetsCount];
        System.arraycopy(granule, 0, r.granules, 3, packetsCount);
        r.sizes = sizes;
        r.model = trimmed;
        r.length = len;
        byte[] p0 = OggMux.mux(cfg.serial, 0, new byte[][] { id }, new long[] { 0 }, 255, true, false);
        byte[] p1 = OggMux.mux(cfg.serial, 1, new byte[][] { comment, setup }, new long[] { 0, 0 }, 255, false, false);
        int seq = 1 + OggMux.pageCount(p1);
        byte[] p2 = OggMux.mux(cfg.serial, seq, audio, granule, cfg.segmentsPerPage, false, true);
        r.file = OggMux.concat(p0, p1, p2);
        return r;
    }

    /**
     * Tells whether the packet at an index is a long block.
     * <p>
     * 위치의 패킷이 긴 블록인지 알려 준다.
     *
     * @param cfg options / 옵션
     * @param p   packet index / 패킷 번호
     * @return true for long / 길면 true
     */
    private static boolean isLongAt(Config cfg, int p) {
        return cfg.pattern.charAt(p % cfg.pattern.length()) == 'L';
    }

    /**
     * The largest value of an array.
     * <p>
     * 배열의 최댓값.
     *
     * @param a the array / 배열
     * @return the maximum / 최댓값
     */
    private static int maxOf(int[] a) {
        int m = a[0];
        // 값을 하나씩 비교한다.
        for (int v : a) {
            m = Math.max(m, v);
        }
        return m;
    }

    /**
     * The codebooks of a configuration.
     * <p>
     * 설정에 따른 코드북.
     */
    private static final class Books {
        /**
         * Floor value book (128 scalar entries).
         * <p>
         * 플로어 값 코드북(스칼라 항목 128개).
         */
        final VorbisBook floorValues = VorbisBook.flat(128, 7, false, 1);

        /**
         * Floor master book (4 entries).
         * <p>
         * 플로어 마스터 코드북(항목 4개).
         */
        final VorbisBook floorMaster = VorbisBook.flat(4, 2, true, 1);

        /**
         * Classification book of the residue.
         * <p>
         * 잔차의 분류 코드북.
         */
        final VorbisBook classBook;

        /**
         * First residue book (coarse).
         * <p>
         * 첫 번째 잔차 코드북(거친 양자화).
         */
        final VorbisBook coarse;

        /**
         * Second residue book (refinement), or null.
         * <p>
         * 두 번째 잔차 코드북(보정). 없으면 null.
         */
        final VorbisBook fine;

        /**
         * Number of classifications.
         * <p>
         * 분류 수.
         */
        final int classes;

        /**
         * Book numbers of every class and pass (-1 none), indices into {@link #all}.
         * <p>
         * 분류와 패스마다의 코드북 번호(-1 이면 없음). {@link #all} 의 인덱스.
         */
        final int[][] pass;

        /**
         * All books in setup order.
         * <p>
         * 설정 순서의 모든 코드북.
         */
        final List<VorbisBook> all = new ArrayList<VorbisBook>();

        /**
         * Builds the books.
         * <p>
         * 코드북을 만든다.
         *
         * @param cfg options / 옵션
         */
        Books(Config cfg) {
            all.add(floorValues);
            all.add(floorMaster);
            if (cfg.profile.equals("vq")) {
                classes = 3;
                classBook = VorbisBook.flat(9, 4, true, 2);
                float d = 0.5f;
                coarse = VorbisBook.grid(4, new float[] { -d, 0, d }, new VorbisBook.Weight() {
                    /**
                     * Entries with four non-zero components are unused; others are weighted by sparsity.
                     * <p>
                     * 성분 네 개가 모두 0 이 아닌 항목은 쓰지 않으며 나머지는 0 이 많을수록 가중치가 크다.
                     *
                     * @param v vector / 벡터
                     * @return weight / 가중치
                     */
                    public double of(float[] v) {
                        int nz = 0;
                        // 항목마다 반복해서 처리한다.
                        for (float f : v) {
                            if (f != 0) {
                                nz++;
                            }
                        }
                        return nz == 4 ? 0 : Math.exp(-1.2 * nz);
                    }
                });
                float[][] t = new float[64][2];
                // 두 성분이 각각 8단계(-3.5..3.5 곱하기 0.125)인 64개 벡터의 명시적 표를 만든다.
                for (int e = 0; e < 64; e++) {
                    t[e][0] = ((e % 8) - 3.5f) * 0.125f;
                    t[e][1] = ((e / 8) - 3.5f) * 0.125f;
                }
                fine = VorbisBook.table(t, new VorbisBook.Weight() {
                    /**
                     * Smaller vectors get shorter codes.
                     * <p>
                     * 작은 벡터일수록 짧은 부호를 받는다.
                     *
                     * @param v vector / 벡터
                     * @return weight / 가중치
                     */
                    public double of(float[] v) {
                        return Math.exp(-3 * (Math.abs(v[0]) + Math.abs(v[1])));
                    }
                }, false);
                all.add(classBook);
                all.add(coarse);
                all.add(fine);
                pass = new int[][] { { -1, -1 }, { 3, -1 }, { 3, 4 } };
            } else {
                classes = 2;
                classBook = VorbisBook.flat(4, 2, true, 2);
                float[] g = new float[257];
                // -128..128 곱하기 1/64 의 격자 값을 만든다.
                for (int i = 0; i < 257; i++) {
                    g[i] = (i - 128) / 64f;
                }
                coarse = VorbisBook.grid(1, g, new VorbisBook.Weight() {
                    /**
                     * Larger values get longer codes; values beyond 3.5 are unused.
                     * <p>
                     * 큰 값일수록 긴 부호를 받으며 3.5 를 넘는 값은 쓰지 않는다.
                     *
                     * @param v vector / 벡터
                     * @return weight / 가중치
                     */
                    public double of(float[] v) {
                        return Math.abs(v[0]) > 3.5 ? 0 : Math.exp(-Math.abs(v[0]));
                    }
                });
                float[] g2 = new float[33];
                // -16..16 곱하기 1/2048 의 보정용 격자 값을 만든다.
                for (int i = 0; i < 33; i++) {
                    g2[i] = (i - 16) / 2048f;
                }
                fine = VorbisBook.grid(1, g2, null);
                all.add(classBook);
                all.add(coarse);
                all.add(fine);
                pass = new int[][] { { -1, -1 }, { 3, 4 } };
            }
        }
    }

    /**
     * Writes the identification header.
     * <p>
     * 식별 헤더를 쓴다.
     *
     * @param cfg options / 옵션
     * @return packet / 패킷
     */
    private static byte[] identification(Config cfg) {
        Out o = new Out();
        o.u8(1).ascii("vorbis").le32(0).u8(cfg.channels).le32(cfg.rate).le32(0).le32(128000).le32(0);
        o.u8(VorbisBits.ilog(cfg.blockShort) - 1 | ((VorbisBits.ilog(cfg.blockLong) - 1) << 4)).u8(1);
        return o.toByteArray();
    }

    /**
     * Writes the comment header.
     * <p>
     * 주석 헤더를 쓴다.
     *
     * @return packet / 패킷
     */
    private static byte[] commentHeader() {
        Out o = new Out();
        o.u8(3).ascii("vorbis").le32(4).ascii("test").le32(0).u8(1);
        return o.toByteArray();
    }

    /**
     * Writes the setup header.
     * <p>
     * 설정 헤더를 쓴다.
     *
     * @param cfg     options / 옵션
     * @param b       codebooks / 코드북
     * @param submaps number of submaps / 하위 맵 수
     * @return packet / 패킷
     */
    private static byte[] setupHeader(Config cfg, Books b, int submaps) {
        VorbisBits w = new VorbisBits();
        w.put(5, 8);
        // 항목마다 반복해서 처리한다.
        for (char ch : "vorbis".toCharArray()) {
            w.put(ch, 8);
        }
        w.put(b.all.size() - 1, 8);
        // 코드북을 차례로 쓴다.
        for (VorbisBook book : b.all) {
            book.write(w);
        }
        w.put(0, 6);
        w.put(0, 16);
        w.put(0, 6);
        w.put(1, 16);
        w.put(2, 5);
        w.put(0, 4);
        w.put(0, 4);
        w.put(1, 3);
        w.put(1, 2);
        w.put(1, 8);
        w.put(0, 8);
        w.put(1, 8);
        w.put(1, 2);
        w.put(10, 4);
        // 파티션마다의 점 위치(10비트)를 쓴다.
        for (int i = 2; i < FLOOR_X.length; i++) {
            w.put(FLOOR_X[i], 10);
        }
        w.put(submaps - 1, 6);
        // 하위 맵마다 같은 설정의 잔차를 하나씩 쓴다.
        for (int s = 0; s < submaps; s++) {
            w.put(cfg.residueType, 16);
            w.put(0, 24);
            w.put(1024, 24);
            w.put(8 - 1, 24);
            w.put(b.classes - 1, 6);
            w.put(2, 8);
            // 분류마다 사용하는 패스(cascade)를 쓴다.
            for (int c = 0; c < b.classes; c++) {
                int cascade = 0;
                // 항목마다 반복해서 처리한다.
                for (int j = 0; j < 2; j++) {
                    if (b.pass[c][j] >= 0) {
                        cascade |= 1 << j;
                    }
                }
                w.put(cascade & 7, 3);
                w.put(0, 1);
            }
            // 항목마다 반복해서 처리한다.
            for (int c = 0; c < b.classes; c++) {
                // 항목마다 반복해서 처리한다.
                for (int j = 0; j < 8; j++) {
                    if (j < 2 && b.pass[c][j] >= 0) {
                        w.put(b.pass[c][j], 8);
                    }
                }
            }
        }
        w.put(0, 6);
        w.put(0, 16);
        w.put(submaps > 1 ? 1 : 0, 1);
        if (submaps > 1) {
            w.put(submaps - 1, 4);
        }
        w.put(cfg.couple ? 1 : 0, 1);
        if (cfg.couple) {
            w.put(0, 8);
            w.put(0, VorbisBits.ilog(cfg.channels - 1));
            w.put(1, VorbisBits.ilog(cfg.channels - 1));
        }
        w.put(0, 2);
        if (submaps > 1) {
            // 채널마다 하위 맵 번호를 쓴다.
            for (int c = 0; c < cfg.channels; c++) {
                w.put(cfg.mux[c], 4);
            }
        }
        // 하위 맵마다 시간(0), 플로어 0, 자기 잔차 번호를 쓴다.
        for (int s = 0; s < submaps; s++) {
            w.put(0, 8);
            w.put(0, 8);
            w.put(s, 8);
        }
        w.put(1, 6);
        w.put(0, 1);
        w.put(0, 16);
        w.put(0, 16);
        w.put(0, 8);
        w.put(1, 1);
        w.put(0, 16);
        w.put(0, 16);
        w.put(0, 8);
        w.put(1, 1);
        return w.bytes();
    }

    /**
     * The final floor heights as reconstructed by the decoder for the target heights (all points coded exactly), together with the
     * coded values of the points from the third on.
     * <p>
     * 목표 높이에 대해 디코더가 복원하는 최종 플로어 높이(모든 점을 정확히 부호화)와 세 번째 점부터의 부호화 값.
     *
     * @param target target heights in X list order / X 목록 순서의 목표 높이
     * @param coded  receives the coded values of points 2 and up / 2번 이후 점의 부호화 값을 받는다
     * @param step2  receives which points take part in the curve / 곡선에 쓰이는 점을 받는다
     */
    private static void floorCodes(int[] target, int[] coded, boolean[] step2) {
        int range = 128;
        int[] y = target.clone();
        step2[0] = true;
        step2[1] = true;
        // 점마다 앞선 점 중 가장 가까운 좌우 이웃으로 예측하고 차이를 부호화한다.
        for (int i = 2; i < FLOOR_X.length; i++) {
            int lo = 0;
            int hi = 1;
            // 항목마다 반복해서 처리한다.
            for (int j = 0; j < i; j++) {
                if (FLOOR_X[j] < FLOOR_X[i] && FLOOR_X[j] > FLOOR_X[lo]) {
                    lo = j;
                }
                if (FLOOR_X[j] > FLOOR_X[i] && FLOOR_X[j] < FLOOR_X[hi]) {
                    hi = j;
                }
            }
            int dy = y[hi] - y[lo];
            int adx = FLOOR_X[hi] - FLOOR_X[lo];
            int off = Math.abs(dy) * (FLOOR_X[i] - FLOOR_X[lo]) / adx;
            int predicted = dy < 0 ? y[lo] - off : y[lo] + off;
            int diff = y[i] - predicted;
            int highroom = range - predicted;
            int lowroom = predicted;
            int val;
            if (diff == 0) {
                val = 0;
            } else if (highroom > lowroom) {
                val = diff >= lowroom ? diff + lowroom : diff > 0 ? 2 * diff : -2 * diff - 1;
            } else {
                val = -diff > highroom ? -diff + highroom - 1 : diff > 0 ? 2 * diff : -2 * diff - 1;
            }
            coded[i] = val;
            step2[i] = val != 0;
            if (val != 0) {
                step2[lo] = true;
                step2[hi] = true;
            }
            if (val == 0) {
                y[i] = predicted;
            }
        }
    }

    /**
     * Renders the floor curve from the points that take part, with the integer line drawing of the specification.
     * <p>
     * 규격의 정수 선 그리기로, 쓰이는 점들에서 플로어 곡선을 만든다.
     *
     * @param y     final heights / 최종 높이
     * @param step2 points that take part / 쓰이는 점
     * @param half  length of the curve / 곡선의 길이
     * @return the curve (linear amplitudes) / 곡선(선형 진폭)
     */
    private static double[] floorCurve(int[] y, boolean[] step2, int half) {
        Integer[] order = new Integer[FLOOR_X.length];
        // 위치 순서로 점 번호를 정렬한다(삽입 정렬).
        for (int i = 0; i < order.length; i++) {
            int p = i;
            // 조건이 끝날 때까지 반복해서 처리한다.
            while (p > 0 && FLOOR_X[order[p - 1].intValue()] > FLOOR_X[i]) {
                order[p] = order[p - 1];
                p--;
            }
            order[p] = Integer.valueOf(i);
        }
        int[] h = new int[half];
        int lx = 0;
        int ly = y[order[0].intValue()] * 2;
        int hx = 0;
        int hy = 0;
        // 위치 순서대로 쓰이는 점을 이어 선분을 그린다.
        for (int k = 1; k < order.length; k++) {
            int pt = order[k].intValue();
            if (step2[pt]) {
                hy = y[pt] * 2;
                hx = FLOOR_X[pt];
                line(lx, ly, hx, hy, h);
                lx = hx;
                ly = hy;
            }
        }
        if (hx < half) {
            line(hx, hy, half, hy, h);
        }
        double[] c = new double[half];
        // 높이 값을 표의 진폭으로 바꾼다.
        for (int i = 0; i < half; i++) {
            c[i] = Math.pow(10.0, (h[i] - 255) * 7.0 / 256.0);
        }
        return c;
    }

    /**
     * Draws an integer line.
     * <p>
     * 정수 선을 그린다.
     *
     * @param x0 start / 시작
     * @param y0 start height / 시작 높이
     * @param x1 end (exclusive) / 끝(미포함)
     * @param y1 end height / 끝 높이
     * @param h  the heights / 높이들
     */
    private static void line(int x0, int y0, int x1, int y1, int[] h) {
        int dy = y1 - y0;
        int adx = x1 - x0;
        if (adx <= 0) {
            return;
        }
        int base = dy / adx;
        int sy = dy < 0 ? base - 1 : base + 1;
        int ady = Math.abs(dy) - Math.abs(base) * adx;
        int x = x0;
        int y = y0;
        int err = 0;
        if (x < h.length) {
            h[x] = y;
        }
        // 한 칸씩 가면서 오차를 누적해 높이를 정한다.
        while (++x < x1) {
            err += ady;
            if (err >= adx) {
                err -= adx;
                y += sy;
            } else {
                y += base;
            }
            if (x < h.length) {
                h[x] = y;
            }
        }
    }

    /**
     * Encodes one audio packet and adds the model of its decoding to the time line.
     * <p>
     * 오디오 패킷 하나를 인코딩하고 그 디코딩의 모델을 시간축에 더한다.
     *
     * @param x        input / 입력
     * @param cfg      options / 옵션
     * @param b        codebooks / 코드북
     * @param submaps  number of submaps / 하위 맵 수
     * @param p        packet number / 패킷 번호
     * @param n        block size / 블록 크기
     * @param center   center of the block on the time line / 시간축에서 블록의 중심
     * @param isLong   long block / 긴 블록
     * @param prevLong previous block long / 이전 블록이 길다
     * @param nextLong next block long / 다음 블록이 길다
     * @param model    the model time line / 모델 시간축
     * @return the packet / 패킷
     */
    private static byte[] encodePacket(float[][] x, Config cfg, Books b, int submaps, int p, int n, long center, boolean isLong, boolean prevLong,
            boolean nextLong, float[][] model) {
        int channels = cfg.channels;
        int half = n / 2;
        double[] cosT = cosTable(n);
        double[][] spec = new double[channels][half];
        boolean[] silent = new boolean[channels];
        // 채널마다 창을 씌운 입력을 MDCT 한다(순방향 스케일은 4/n).
        for (int c = 0; c < channels; c++) {
            double[] s = new double[n];
            double energy = 0;
            // 항목마다 반복해서 처리한다.
            for (int m = 0; m < n; m++) {
                long t = center - n / 2 + m;
                double v = t >= 0 && t < x[c].length ? x[c][(int) t] : 0;
                s[m] = v * window(m, n, isLong, prevLong, nextLong, cfg.blockShort);
                energy += Math.abs(s[m]);
            }
            silent[c] = energy < 1e-9;
            // 항목마다 반복해서 처리한다.
            for (int k = 0; k < half; k++) {
                double sum = 0;
                // 항목마다 반복해서 처리한다.
                for (int m = 0; m < n; m++) {
                    sum += s[m] * cosT[((2 * m + n / 2 + 1) * (2 * k + 1)) % (4 * n)];
                }
                spec[c][k] = sum * 4.0 / n;
            }
        }
        int[] heights = cfg.sloped ? FLOOR_SLOPED : FLOOR_FLAT;
        int[] coded = new int[FLOOR_X.length];
        boolean[] step2 = new boolean[FLOOR_X.length];
        floorCodes(heights, coded, step2);
        int[] finalY = heights.clone();
        double[] curve = floorCurve(finalY, step2, half);
        // 결합할 채널의 목표 잔차를 먼저 구하고 크기/각도로 바꾼다.
        double[][] target = new double[channels][half];
        for (int c = 0; c < channels; c++) {
            // 항목마다 반복해서 처리한다.
            for (int k = 0; k < half; k++) {
                target[c][k] = spec[c][k] / curve[k];
            }
        }
        if (cfg.couple) {
            // 두 채널의 값을 크기/각도로 바꾼다(역결합 규칙의 역).
            for (int k = 0; k < half; k++) {
                double pv = target[0][k];
                double qv = target[1][k];
                double mag;
                double ang;
                if (Math.abs(pv) >= Math.abs(qv)) {
                    mag = pv;
                    ang = pv > 0 ? pv - qv : qv - pv;
                } else {
                    mag = qv;
                    ang = qv > 0 ? pv - qv : qv - pv;
                }
                target[0][k] = mag;
                target[1][k] = ang;
            }
        }
        boolean[] noResidue = new boolean[channels];
        // 항목마다 반복해서 처리한다.
        for (int c = 0; c < channels; c++) {
            noResidue[c] = silent[c];
        }
        if (cfg.couple && (!noResidue[0] || !noResidue[1])) {
            noResidue[0] = false;
            noResidue[1] = false;
        }
        double[][] recon = new double[channels][half];
        VorbisBits w = new VorbisBits();
        w.put(0, 1);
        w.put(isLong ? 1 : 0, 1);
        if (isLong) {
            w.put(prevLong ? 1 : 0, 1);
            w.put(nextLong ? 1 : 0, 1);
        }
        // 채널마다 플로어를 쓴다(소리가 없는 채널은 사용하지 않음 표시).
        for (int c = 0; c < channels; c++) {
            if (noResidue[c]) {
                w.put(0, 1);
            } else {
                writeFloor(w, b, heights, coded);
            }
        }
        // 하위 맵마다 그 채널들의 잔차를 양자화해 쓴다.
        for (int s = 0; s < submaps; s++) {
            List<Integer> chans = new ArrayList<Integer>();
            // 항목마다 반복해서 처리한다.
            for (int c = 0; c < channels; c++) {
                int sub = cfg.mux == null ? 0 : cfg.mux[c];
                if (sub == s) {
                    chans.add(Integer.valueOf(c));
                }
            }
            if (!chans.isEmpty()) {
                writeResidue(w, b, cfg.residueType, chans, noResidue, target, recon, half);
            }
        }
        // 모델: 역결합하고 플로어를 곱해 스펙트럼을 만든다.
        double[][] spectrum = new double[channels][half];
        for (int c = 0; c < channels; c++) {
            System.arraycopy(recon[c], 0, spectrum[c], 0, half);
        }
        if (cfg.couple) {
            // 디코더의 역결합 규칙을 규격 그대로 적용한다.
            for (int k = 0; k < half; k++) {
                double m = recon[0][k];
                double a = recon[1][k];
                double outM;
                double outA;
                if (m > 0) {
                    if (a > 0) {
                        outM = m;
                        outA = m - a;
                    } else {
                        outA = m;
                        outM = m + a;
                    }
                } else {
                    if (a > 0) {
                        outM = m;
                        outA = m + a;
                    } else {
                        outA = m;
                        outM = m - a;
                    }
                }
                spectrum[0][k] = outM;
                spectrum[1][k] = outA;
            }
        }
        // 직접 IMDCT 한 뒤 창을 씌워 시간축에 누적한다.
        for (int c = 0; c < channels; c++) {
            boolean unused = silent[c] && !(cfg.couple && c < 2 && (!silent[0] || !silent[1]));
            if (unused) {
                continue;
            }
            // 항목마다 반복해서 처리한다.
            for (int m = 0; m < n; m++) {
                double sum = 0;
                // 항목마다 반복해서 처리한다.
                for (int k = 0; k < half; k++) {
                    sum += spectrum[c][k] * curve[k] * cosT[((2 * m + n / 2 + 1) * (2 * k + 1)) % (4 * n)];
                }
                long t = center - n / 2 + m;
                if (t >= 0 && t < model[c].length) {
                    model[c][(int) t] += (float) (sum * window(m, n, isLong, prevLong, nextLong, cfg.blockShort));
                }
            }
        }
        return w.bytes();
    }

    /**
     * Writes the floor data of a channel.
     * <p>
     * 채널의 플로어 데이터를 쓴다.
     *
     * @param w       bits / 비트
     * @param b       codebooks / 코드북
     * @param heights target heights / 목표 높이
     * @param coded   coded values of the points from the third on / 세 번째 점부터의 부호화 값
     */
    private static void writeFloor(VorbisBits w, Books b, int[] heights, int[] coded) {
        w.put(1, 1);
        w.put(heights[0], 7);
        w.put(heights[1], 7);
        // 파티션마다 두 점의 부호화 값을 서브클래스(0 이면 생략)와 함께 쓴다.
        for (int part = 0; part < 2; part++) {
            int v0 = coded[2 + part * 2];
            int v1 = coded[3 + part * 2];
            int cval = (v0 != 0 ? 1 : 0) | (v1 != 0 ? 2 : 0);
            b.floorMaster.writeEntry(w, cval);
            if (v0 != 0) {
                b.floorValues.writeEntry(w, v0);
            }
            if (v1 != 0) {
                b.floorValues.writeEntry(w, v1);
            }
        }
    }

    /**
     * Quantizes and writes the residue of the channels of one submap, and records what the decoder will reconstruct.
     * <p>
     * 하위 맵의 채널들의 잔차를 양자화해 쓰고 디코더가 복원할 값을 기록한다.
     *
     * @param w         bits / 비트
     * @param b         codebooks / 코드북
     * @param type      residue type / 잔차 유형
     * @param chans     the channels of the submap in order / 하위 맵의 채널(순서대로)
     * @param noResidue true for channels without residue / 잔차가 없는 채널이면 true
     * @param target    target residue vectors / 목표 잔차 벡터
     * @param recon     receives the reconstructed vectors / 복원한 벡터를 받는다
     * @param half      half block size / 블록 크기의 절반
     */
    private static void writeResidue(VorbisBits w, Books b, int type, List<Integer> chans, boolean[] noResidue, double[][] target, double[][] recon, int half) {
        int count = chans.size();
        boolean allSkip = true;
        // 모든 채널에 잔차가 없으면 아무것도 쓰지 않는다.
        for (Integer c : chans) {
            if (!noResidue[c.intValue()]) {
                allSkip = false;
            }
        }
        if (allSkip) {
            return;
        }
        int size = type == 2 ? half * count : half;
        int vectors = type == 2 ? 1 : count;
        double[][] tv = new double[vectors][size];
        boolean[] skip = new boolean[vectors];
        if (type == 2) {
            // 채널의 값을 한 벡터로 인터리브한다.
            for (int i = 0; i < half; i++) {
                for (int j = 0; j < count; j++) {
                    tv[0][i * count + j] = target[chans.get(j).intValue()][i];
                }
            }
        } else {
            // 채널마다 자기 벡터를 쓰며 잔차가 없는 채널은 건너뛴다.
            for (int j = 0; j < count; j++) {
                System.arraycopy(target[chans.get(j).intValue()], 0, tv[j], 0, half);
                skip[j] = noResidue[chans.get(j).intValue()];
            }
        }
        int partSize = 8;
        int parts = Math.min(1024, size) / partSize;
        double[][] rv = new double[vectors][size];
        int[][] cls = new int[vectors][parts];
        int[][][][] chosen = new int[vectors][parts][2][];
        // 벡터와 파티션마다 분류를 정하고 패스별로 가장 가까운 항목을 고른다.
        for (int v = 0; v < vectors; v++) {
            for (int pt = 0; pt < parts; pt++) {
                double max = 0;
                // 항목마다 반복해서 처리한다.
                for (int i = 0; i < partSize; i++) {
                    max = Math.max(max, Math.abs(tv[v][pt * partSize + i]));
                }
                int cl;
                if (b.classes == 3) {
                    cl = max < 0.12 ? 0 : max < 0.6 ? 1 : 2;
                } else {
                    cl = max < 0.0004 ? 0 : 1;
                }
                if (skip[v]) {
                    cl = 0;
                }
                cls[v][pt] = cl;
                quantizePartition(b, type, cl, tv[v], rv[v], pt * partSize, partSize, chosen[v][pt]);
            }
        }
        // 규격의 부호화 순서(패스, 파티션 묶음, 채널)대로 쓴다.
        int classWords = b.classBook.dims;
        for (int pass = 0; pass < 2; pass++) {
            int pc = 0;
            while (pc < parts) {
                if (pass == 0) {
                    // 첫 패스에서는 채널마다 분류 번호 묶음을 분류 코드북으로 쓴다.
                    for (int v = 0; v < vectors; v++) {
                        if (skip[v]) {
                            continue;
                        }
                        int entry = 0;
                        // 항목마다 반복해서 처리한다.
                        for (int i = 0; i < classWords; i++) {
                            entry = entry * b.classes + (pc + i < parts ? cls[v][pc + i] : 0);
                        }
                        b.classBook.writeEntry(w, entry);
                    }
                }
                // 항목마다 반복해서 처리한다.
                for (int i = 0; i < classWords && pc < parts; i++) {
                    // 항목마다 반복해서 처리한다.
                    for (int v = 0; v < vectors; v++) {
                        if (skip[v]) {
                            continue;
                        }
                        int book = b.pass[cls[v][pc]][pass];
                        if (book >= 0) {
                            VorbisBook bk = b.all.get(book);
                            // 이 패스에서 고른 항목들의 부호어를 쓴다.
                            for (int e : chosen[v][pc][pass]) {
                                bk.writeEntry(w, e);
                            }
                        }
                    }
                    pc++;
                }
            }
        }
        // 복원한 벡터를 채널 배열로 되돌린다.
        if (type == 2) {
            for (int i = 0; i < half; i++) {
                // 항목마다 반복해서 처리한다.
                for (int j = 0; j < count; j++) {
                    recon[chans.get(j).intValue()][i] = rv[0][i * count + j];
                }
            }
        } else {
            // 항목마다 반복해서 처리한다.
            for (int j = 0; j < count; j++) {
                System.arraycopy(rv[j], 0, recon[chans.get(j).intValue()], 0, half);
            }
        }
    }

    /**
     * Quantizes the vectors of one partition pass by pass (the nearest entry of each book to what is left), adds what the decoder will
     * reconstruct to <code>out</code> and keeps the chosen entries of every pass.
     * <p>
     * 파티션 하나의 벡터를 패스별로 양자화하고(각 코드북에서 남은 값에 가장 가까운 항목) 디코더가 복원할 값을 <code>out</code> 에 더하며 패스마다 고른
     * 항목을 보관한다.
     *
     * @param b      codebooks / 코드북
     * @param type   residue type / 잔차 유형
     * @param cl     class / 분류
     * @param tgt    target vector / 목표 벡터
     * @param out    receives the reconstruction / 복원 값을 받는다
     * @param offset first position of the partition / 파티션의 첫 위치
     * @param size   partition size / 파티션 크기
     * @param chosen receives the entries of every pass / 패스마다의 항목을 받는다
     */
    private static void quantizePartition(Books b, int type, int cl, double[] tgt, double[] out, int offset, int size, int[][] chosen) {
        double[] rem = new double[size];
        System.arraycopy(tgt, offset, rem, 0, size);
        // 사용하는 패스마다 남은 값에서 가장 가까운 항목을 빼 나간다.
        for (int pass = 0; pass < 2; pass++) {
            int book = b.pass[cl][pass];
            if (book < 0) {
                continue;
            }
            VorbisBook bk = b.all.get(book);
            int dims = bk.dims;
            int step = size / dims;
            chosen[pass] = new int[step];
            // 항목마다 반복해서 처리한다.
            for (int vi = 0; vi < step; vi++) {
                int e = nearest(bk, rem, vi, dims, step, type);
                chosen[pass][vi] = e;
                // 항목마다 반복해서 처리한다.
                for (int k = 0; k < dims; k++) {
                    int pos = type == 0 ? vi + k * step : vi * dims + k;
                    rem[pos] -= bk.value(e, k);
                    out[offset + pos] += bk.value(e, k);
                }
            }
        }
    }

    /**
     * Finds the used entry of a book that is nearest to the remaining values of one vector.
     * <p>
     * 코드북에서 벡터 하나의 남은 값에 가장 가까운 사용 항목을 찾는다.
     *
     * @param bk   the book / 코드북
     * @param rem  remaining values of the partition / 파티션의 남은 값
     * @param vi   vector number within the partition / 파티션 안의 벡터 번호
     * @param dims dimensions / 차원 수
     * @param step vectors per partition / 파티션당 벡터 수
     * @param type residue type (0 spreads the vector) / 잔차 유형(0 은 벡터를 펼친다)
     * @return the entry / 항목
     */
    private static int nearest(VorbisBook bk, double[] rem, int vi, int dims, int step, int type) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        // 사용하는 항목을 모두 비교해 거리가 가장 작은 것을 고른다.
        for (int e = 0; e < bk.entries; e++) {
            if (bk.lengths[e] == 0) {
                continue;
            }
            double d = 0;
            // 항목마다 반복해서 처리한다.
            for (int k = 0; k < dims; k++) {
                int pos = type == 0 ? vi + k * step : vi * dims + k;
                double diff = rem[pos] - bk.value(e, k);
                d += diff * diff;
            }
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }
}
