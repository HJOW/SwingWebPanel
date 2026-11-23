package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

import java.util.HashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Vorbis floor type 0 (Vorbis I specification, section 6): the spectral envelope given by line spectral pair coefficients that are
 * coded with vector quantization. It is the old floor of Vorbis that encoders do not use any more, but decoders must understand it.
 * <p>
 * Vorbis 플로어 유형 0(Vorbis I 규격 6절): 벡터 양자화로 부호화한 선 스펙트럼 쌍(LSP) 계수로 주어지는 스펙트럼 포락선. 이제 인코더가 쓰지 않는
 * Vorbis 의 옛 플로어이지만 디코더는 이해해야 한다.
 */
final class VorbisFloor0 implements VorbisFloor {

    /**
     * The codebooks of the setup header.
     * <p>
     * 설정 헤더의 코드북.
     */
    private final VorbisCodebook[] books;

    /**
     * Order of the filter.
     * <p>
     * 필터의 차수.
     */
    private final int order;

    /**
     * Sample rate used for the Bark scale.
     * <p>
     * Bark 스케일에 쓰는 샘플링 주파수.
     */
    private final int rate;

    /**
     * Size of the Bark map.
     * <p>
     * Bark 지도의 크기.
     */
    private final int barkMapSize;

    /**
     * Bits of the amplitude value.
     * <p>
     * 진폭 값의 비트 수.
     */
    private final int amplitudeBits;

    /**
     * Amplitude offset (in dB).
     * <p>
     * 진폭 오프셋(dB).
     */
    private final int amplitudeOffset;

    /**
     * Codebook numbers the floor may use.
     * <p>
     * 이 플로어가 쓸 수 있는 코드북 번호.
     */
    private final int[] bookList;

    /**
     * Bark maps by half block size.
     * <p>
     * 블록 크기의 절반별 Bark 지도.
     */
    private final Map<Integer, int[]> maps = new HashMap<Integer, int[]>();

    /**
     * Parses the floor configuration.
     * <p>
     * 플로어 설정을 해석한다.
     *
     * @param br    packet bits / 패킷 비트
     * @param books the codebooks / 코드북
     * @throws MediaException CORRUPT for an invalid configuration / 올바르지 않은 설정이면 CORRUPT
     */
    VorbisFloor0(VorbisBitReader br, VorbisCodebook[] books) throws MediaException {
        this.books = books;
        order = br.read(8);
        rate = br.read(16);
        barkMapSize = br.read(16);
        amplitudeBits = br.read(6);
        amplitudeOffset = br.read(8);
        int count = br.read(4) + 1;
        bookList = new int[count];
        // 사용할 코드북 번호를 읽고 존재하는지 확인한다.
        for (int i = 0; i < count; i++) {
            bookList[i] = br.read(8);
            if (bookList[i] >= books.length || !books[bookList[i]].hasLookup()) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis floor 0 codebook");
            }
        }
        if (order < 1 || rate < 1 || barkMapSize < 1 || br.isEndOfPacket()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis floor 0 configuration");
        }
    }

    /**
     * Decodes the floor of a channel and renders the curve.
     * <p>
     * 채널의 플로어를 디코딩하고 곡선을 만든다.
     *
     * @param br   packet bits / 패킷 비트
     * @param out  receives the curve / 곡선을 받는다
     * @param half half of the block size / 블록 크기의 절반
     * @return true if the channel has a floor / 채널에 플로어가 있으면 true
     */
    public boolean decode(VorbisBitReader br, float[] out, int half) {
        int amplitude = br.read(amplitudeBits);
        if (amplitude == 0 || br.isEndOfPacket()) {
            return false;
        }
        int number = br.read(VorbisBitReader.ilog(bookList.length));
        if (number >= bookList.length || br.isEndOfPacket()) {
            return false;
        }
        VorbisCodebook book = books[bookList[number]];
        float[] coef = new float[order + book.dimensions];
        float[] tmp = new float[book.dimensions];
        int count = 0;
        float last = 0;
        // 벡터를 읽어 앞 벡터의 마지막 값을 더하며 차수만큼의 계수를 모은다.
        while (count < order) {
            if (!book.decodeVector(br, tmp, 0)) {
                return false;
            }
            // 항목마다 반복해서 처리한다.
            for (int i = 0; i < book.dimensions; i++) {
                coef[count + i] = tmp[i] + last;
            }
            last = coef[count + book.dimensions - 1];
            count += book.dimensions;
        }
        double[] lsp = new double[order];
        // 계수를 2cos 값으로 바꿔 둔다.
        for (int i = 0; i < order; i++) {
            lsp[i] = 2.0 * Math.cos(coef[i]);
        }
        int[] map = mapFor(half);
        double ampScaled = (double) amplitude * amplitudeOffset / (((1L << amplitudeBits) - 1));
        int prev = -1;
        float value = 0;
        // 샘플마다 Bark 지도의 위치가 같으면 이전 값을 재사용하고 다르면 곡선 값을 계산한다.
        for (int i = 0; i < half; i++) {
            if (map[i] != prev) {
                prev = map[i];
                value = (float) curve(lsp, Math.PI * map[i] / barkMapSize, ampScaled);
            }
            out[i] = value;
        }
        return true;
    }

    /**
     * Computes the linear floor value at one frequency from the line spectral pair coefficients.
     * <p>
     * 선 스펙트럼 쌍 계수로 한 주파수의 선형 플로어 값을 계산한다.
     *
     * @param lsp       the coefficients as 2cos(c) / 2cos(c) 로 바꾼 계수
     * @param omega     angle of the frequency / 주파수의 각도
     * @param ampScaled amplitude times offset divided by the largest amplitude value / 진폭 × 오프셋 ÷ 최대 진폭 값
     * @return the floor value / 플로어 값
     */
    private double curve(double[] lsp, double omega, double ampScaled) {
        double w = 2.0 * Math.cos(omega);
        double p = 0.5;
        double q = 0.5;
        int j = 1;
        // 계수 쌍마다 곱을 갱신한다.
        for (; j < order; j += 2) {
            q *= w - lsp[j - 1];
            p *= w - lsp[j];
        }
        if (j == order) {
            q *= w - lsp[j - 1];
            p = p * p * (4.0 - w * w);
            q = q * q;
        } else {
            p = p * p * (2.0 - w);
            q = q * q * (2.0 + w);
        }
        return Math.exp(0.11512925 * (ampScaled / Math.sqrt(p + q) - amplitudeOffset));
    }

    /**
     * Returns the Bark map for a half block size, computing it on first use.
     * <p>
     * 블록 크기의 절반에 대한 Bark 지도를 반환하며 처음 쓸 때 계산한다.
     *
     * @param half half of the block size / 블록 크기의 절반
     * @return the map / 지도
     */
    private int[] mapFor(int half) {
        Integer key = Integer.valueOf(half);
        int[] m = maps.get(key);
        if (m == null) {
            m = new int[half];
            double scale = barkMapSize / bark(0.5 * rate);
            // 샘플마다 Bark 값을 지도 크기에 맞게 바꿔 둔다.
            for (int i = 0; i < half; i++) {
                int v = (int) Math.floor(bark((double) rate * i / (2.0 * half)) * scale);
                m[i] = Math.min(barkMapSize - 1, v);
            }
            maps.put(key, m);
        }
        return m;
    }

    /**
     * The Bark scale.
     * <p>
     * Bark 스케일.
     *
     * @param x frequency in Hz / 주파수(Hz)
     * @return Bark value / Bark 값
     */
    private static double bark(double x) {
        return 13.1 * Math.atan(0.00074 * x) + 2.24 * Math.atan(0.0000000185 * x * x) + 0.0001 * x;
    }
}
