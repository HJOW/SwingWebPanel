package org.duckdns.hjow.swingwebpanel.media.audio.testkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Test-only description and writer of a Vorbis codebook. Codeword lengths come from a Huffman construction over weights; the
 * codewords are then assigned by the rule of the Vorbis specification (the lowest unused value of the codeword length, entry by entry)
 * with an interval method that is independent of the decoder. The vector lookup is stored explicitly as the float value of every entry
 * and dimension; the writer chooses the implicit (type 1) or explicit (type 2) representation.
 * <p>
 * 시험 전용의 Vorbis 코드북 설명 및 쓰기 객체. 부호어 길이는 가중치에 대한 허프만 구성으로 구하고, 부호어는 Vorbis 규격의 규칙(항목 순서대로 부호어 길이의
 * 가장 낮은 미사용 값)에 따라 디코더와 별개의 구간 방식으로 배정한다. 벡터 조회는 항목과 차원마다의 float 값으로 명시적으로 저장하며 쓰기 객체가 암시적(유형
 * 1) 또는 명시적(유형 2) 표현을 고른다.
 */
public final class VorbisBook {

    /**
     * Dimensions of the vectors.
     * <p>
     * 벡터의 차원 수.
     */
    public final int dims;

    /**
     * Number of entries.
     * <p>
     * 항목 수.
     */
    public final int entries;

    /**
     * Codeword length of every entry, 0 for unused entries.
     * <p>
     * 항목마다의 부호어 길이. 쓰지 않는 항목은 0.
     */
    public final int[] lengths;

    /**
     * Codeword of every used entry.
     * <p>
     * 쓰는 항목마다의 부호어.
     */
    public final long[] codes;

    /**
     * The vector of every entry (null for books without lookup), [entry][dimension].
     * <p>
     * 항목마다의 벡터(조회가 없는 코드북이면 null). [항목][차원].
     */
    public final float[][] vectors;

    /**
     * Lookup type to write: 0 none, 1 implicit grid, 2 explicit table.
     * <p>
     * 쓸 조회 유형: 0 없음, 1 암시적 격자, 2 명시적 표.
     */
    public final int lookupType;

    /**
     * Grid values per dimension of a type 1 lookup.
     * <p>
     * 유형 1 조회의 차원당 격자 값.
     */
    private final float[] grid;

    /**
     * Whether the lengths are written in the ordered form.
     * <p>
     * 길이를 ordered 형식으로 쓰는지 여부.
     */
    private final boolean ordered;

    /**
     * Whether unused entries are allowed (sparse form).
     * <p>
     * 쓰지 않는 항목을 허용하는지(sparse 형식) 여부.
     */
    private final boolean sparse;

    /**
     * Creates a book.
     * <p>
     * 코드북을 만든다.
     *
     * @param dims       dimensions / 차원 수
     * @param lengths    codeword lengths (0 = unused) / 부호어 길이(0 이면 쓰지 않음)
     * @param vectors    vectors of the entries or null / 항목의 벡터 또는 null
     * @param lookupType 0, 1 or 2 / 0, 1, 2
     * @param grid       the values of one dimension for type 1 / 유형 1 의 한 차원 값
     * @param ordered    write the ordered form (lengths must not decrease) / ordered 형식으로 쓴다(길이가 줄어들면 안 됨)
     * @param sparse     write the sparse form / sparse 형식으로 쓴다
     */
    private VorbisBook(int dims, int[] lengths, float[][] vectors, int lookupType, float[] grid, boolean ordered, boolean sparse) {
        this.dims = dims;
        this.entries = lengths.length;
        this.lengths = lengths;
        this.vectors = vectors;
        this.lookupType = lookupType;
        this.grid = grid;
        this.ordered = ordered;
        this.sparse = sparse;
        this.codes = assign(lengths);
    }

    /**
     * Creates a book without a vector lookup (scalar entries) and with equal codeword lengths.
     * <p>
     * 벡터 조회가 없고 부호어 길이가 모두 같은 코드북을 만든다.
     *
     * @param entries number of entries / 항목 수
     * @param length  the common length / 공통 길이
     * @param ordered write the ordered form / ordered 형식으로 쓴다
     * @param dims    the dimension field (the number of class words for a classification book) / 차원 필드(분류 코드북이면 분류 단어 수)
     * @return the book / 코드북
     */
    public static VorbisBook flat(int entries, int length, boolean ordered, int dims) {
        int[] l = new int[entries];
        java.util.Arrays.fill(l, length);
        return new VorbisBook(dims, l, null, 0, null, ordered, false);
    }

    /**
     * Creates a book with a type 1 lookup: every entry is a vector whose components are values of a grid, entry number digits in base
     * <code>grid.length</code>, first component least significant.
     * <p>
     * 유형 1 조회를 가진 코드북을 만든다: 항목은 격자 값으로 이루어진 벡터이며 항목 번호를 <code>grid.length</code> 진수로 나눈 자릿수가 성분이고 첫 성분이
     * 최하위이다.
     *
     * @param dims   dimensions / 차원 수
     * @param grid   grid values (ascending multiples of a step) / 격자 값(어떤 간격의 배수, 오름차순)
     * @param weight weight of an entry from its vector (larger = shorter code), or null for equal weights; entries with weight 0 are unused / 벡터로부터 구한 항목의 가중치(클수록 짧은 부호). null 이면 모두 같음. 가중치 0 인 항목은 쓰지 않음
     * @return the book / 코드북
     */
    public static VorbisBook grid(int dims, float[] grid, Weight weight) {
        int base = grid.length;
        int entries = 1;
        // 격자 값의 거듭제곱만큼 항목이 있다.
        for (int i = 0; i < dims; i++) {
            entries *= base;
        }
        float[][] v = new float[entries][dims];
        double[] w = new double[entries];
        // 항목 번호를 자릿수로 나눠 벡터를 만들고 가중치를 구한다.
        for (int e = 0; e < entries; e++) {
            int idx = e;
            // 항목마다 반복해서 처리한다.
            for (int d = 0; d < dims; d++) {
                v[e][d] = grid[idx % base];
                idx /= base;
            }
            w[e] = weight == null ? 1.0 : weight.of(v[e]);
        }
        return new VorbisBook(dims, huffman(w), v, 1, grid, false, true);
    }

    /**
     * Creates a book with a type 2 lookup from an explicit table of vectors.
     * <p>
     * 벡터의 명시적 표로 유형 2 조회를 가진 코드북을 만든다.
     *
     * @param vectors the vectors / 벡터
     * @param weight  weight function or null / 가중치 함수 또는 null
     * @param sparse  allow unused entries (weight 0) / 쓰지 않는 항목(가중치 0)을 허용
     * @return the book / 코드북
     */
    public static VorbisBook table(float[][] vectors, Weight weight, boolean sparse) {
        double[] w = new double[vectors.length];
        // 항목마다 가중치를 구한다.
        for (int e = 0; e < w.length; e++) {
            w[e] = weight == null ? 1.0 : weight.of(vectors[e]);
        }
        return new VorbisBook(vectors[0].length, huffman(w), vectors, 2, null, false, sparse);
    }

    /**
     * Weight function of an entry.
     * <p>
     * 항목의 가중치 함수.
     */
    public interface Weight {
        /**
         * Returns the weight of a vector.
         * <p>
         * 벡터의 가중치를 반환한다.
         *
         * @param v the vector / 벡터
         * @return the weight, 0 for unused / 가중치. 쓰지 않으면 0
         */
        double of(float[] v);
    }

    /**
     * Huffman code lengths for weights; zero weights get length 0. A single used entry gets length 1.
     * <p>
     * 가중치에 대한 허프만 부호 길이. 가중치 0 은 길이 0. 쓰는 항목이 하나뿐이면 길이 1.
     *
     * @param w weights / 가중치
     * @return lengths / 길이
     */
    static int[] huffman(double[] w) {
        int n = w.length;
        int[] len = new int[n];
        double max = 0;
        // 가장 큰 가중치를 구해 너무 작은 가중치를 끌어올려 부호가 너무 길어지지 않게 한다.
        for (double d : w) {
            max = Math.max(max, d);
        }
        List<double[]> nodes = new ArrayList<double[]>();
        List<List<Integer>> members = new ArrayList<List<Integer>>();
        // 항목마다 반복해서 처리한다.
        for (int i = 0; i < n; i++) {
            if (w[i] > 0) {
                nodes.add(new double[] { Math.max(w[i], max / 512) });
                List<Integer> m = new ArrayList<Integer>();
                m.add(Integer.valueOf(i));
                members.add(m);
            }
        }
        if (nodes.size() == 1) {
            len[members.get(0).get(0).intValue()] = 1;
            return len;
        }
        // 가장 가벼운 두 노드를 합치며 속한 항목의 길이를 1씩 늘린다.
        while (nodes.size() > 1) {
            int a = 0;
            int b = 1;
            if (nodes.get(b)[0] < nodes.get(a)[0]) {
                a = 1;
                b = 0;
            }
            // 항목마다 반복해서 처리한다.
            for (int i = 2; i < nodes.size(); i++) {
                double v = nodes.get(i)[0];
                if (v < nodes.get(a)[0]) {
                    b = a;
                    a = i;
                } else if (v < nodes.get(b)[0]) {
                    b = i;
                }
            }
            double sum = nodes.get(a)[0] + nodes.get(b)[0];
            List<Integer> merged = new ArrayList<Integer>(members.get(a));
            merged.addAll(members.get(b));
            // 항목마다 반복해서 처리한다.
            for (Integer m : merged) {
                len[m.intValue()]++;
            }
            int hi = Math.max(a, b);
            int lo = Math.min(a, b);
            nodes.remove(hi);
            members.remove(hi);
            nodes.remove(lo);
            members.remove(lo);
            nodes.add(new double[] { sum });
            members.add(merged);
        }
        return len;
    }

    /**
     * Assigns codewords by the rule of the Vorbis specification: entry by entry, the numerically lowest codeword of the entry's length
     * that is not a prefix of an assigned codeword and has no assigned codeword as a prefix. Codewords are kept as intervals of the
     * 32-bit code space.
     * <p>
     * Vorbis 규격의 규칙으로 부호어를 배정한다: 항목 순서대로, 이미 배정된 부호어의 접두어가 아니고 배정된 부호어를 접두어로 갖지도 않는, 그 항목 길이의
     * 수치상 가장 낮은 부호어. 부호어는 32비트 부호 공간의 구간으로 관리한다.
     *
     * @param len lengths / 길이
     * @return codewords (as len-bit numbers, first bit most significant) / 부호어(len 비트 수, 먼저 읽는 비트가 최상위)
     */
    static long[] assign(int[] len) {
        long[] code = new long[len.length];
        List<long[]> used = new ArrayList<long[]>();
        // 항목마다 구간이 겹치지 않는 가장 낮은 후보를 찾는다.
        for (int i = 0; i < len.length; i++) {
            if (len[i] == 0) {
                continue;
            }
            long size = 1L << (32 - len[i]);
            long cand = 0;
            // 항목마다 반복해서 처리한다.
            for (long[] u : used) {
                if (cand + size <= u[0]) {
                    break;
                }
                if (cand < u[1]) {
                    cand = ((u[1] + size - 1) / size) * size;
                }
            }
            if (cand + size > (1L << 32)) {
                throw new IllegalStateException("overspecified codebook");
            }
            code[i] = cand >>> (32 - len[i]);
            used.add(new long[] { cand, cand + size });
            Collections.sort(used, new Comparator<long[]>() {
                /**
                 * Orders intervals by their start.
                 * <p>
                 * 구간을 시작 위치순으로 정렬한다.
                 *
                 * @param a first / 첫째
                 * @param b second / 둘째
                 * @return comparison / 비교 결과
                 */
                public int compare(long[] a, long[] b) {
                    return a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0;
                }
            });
        }
        return code;
    }

    /**
     * Writes the codebook to the setup header.
     * <p>
     * 코드북을 설정 헤더에 쓴다.
     *
     * @param w bit writer / 비트 쓰기 객체
     */
    public void write(VorbisBits w) {
        w.put(0x564342, 24);
        w.put(dims, 16);
        w.put(entries, 24);
        w.put(ordered ? 1 : 0, 1);
        if (!ordered) {
            boolean anyUnused = false;
            // 항목마다 반복해서 처리한다.
            for (int l : lengths) {
                if (l == 0) {
                    anyUnused = true;
                }
            }
            w.put(anyUnused && sparse ? 1 : 0, 1);
            // 항목마다 부호어 길이를 쓴다(sparse 형식에서는 사용 여부 플래그가 앞선다).
            for (int l : lengths) {
                if (anyUnused && sparse) {
                    w.put(l == 0 ? 0 : 1, 1);
                    if (l != 0) {
                        w.put(l - 1, 5);
                    }
                } else {
                    w.put(l - 1, 5);
                }
            }
        } else {
            int cur = 0;
            int curLen = lengths[0];
            w.put(curLen - 1, 5);
            // 같은 길이의 항목 묶음마다 개수를 쓰며 길이를 하나씩 늘려 간다.
            while (cur < entries) {
                int count = 0;
                // 조건이 끝날 때까지 반복해서 처리한다.
                while (cur + count < entries && lengths[cur + count] == curLen) {
                    count++;
                }
                w.put(count, VorbisBits.ilog(entries - cur));
                cur += count;
                curLen++;
            }
        }
        w.put(lookupType, 4);
        if (lookupType == 0) {
            return;
        }
        float step;
        float min;
        int[] mult;
        if (lookupType == 1) {
            step = grid.length > 1 ? grid[1] - grid[0] : 1f;
            min = grid[0];
            mult = new int[grid.length];
            // 항목마다 반복해서 처리한다.
            for (int i = 0; i < grid.length; i++) {
                mult[i] = i;
            }
        } else {
            float lo = Float.MAX_VALUE;
            float hi = -Float.MAX_VALUE;
            // 항목마다 반복해서 처리한다.
            for (float[] v : vectors) {
                // 항목마다 반복해서 처리한다.
                for (float f : v) {
                    lo = Math.min(lo, f);
                    hi = Math.max(hi, f);
                }
            }
            int levels = 16;
            step = hi > lo ? (hi - lo) / (levels - 1) : 1f;
            min = lo;
            mult = new int[entries * dims];
            // 표의 값마다 가장 가까운 격자 단계를 곱셈 계수로 쓴다.
            for (int e = 0; e < entries; e++) {
                for (int d = 0; d < dims; d++) {
                    mult[e * dims + d] = Math.round((vectors[e][d] - lo) / step);
                }
            }
        }
        int maxMult = 0;
        // 곱셈 계수의 최댓값으로 필요한 비트 수를 구한다.
        for (int m : mult) {
            maxMult = Math.max(maxMult, m);
        }
        int valueBits = Math.max(1, VorbisBits.ilog(maxMult));
        w.put(VorbisBits.pack(min), 32);
        w.put(VorbisBits.pack(step), 32);
        w.put(valueBits - 1, 4);
        w.put(0, 1);
        // 곱셈 계수를 쓴다.
        for (int m : mult) {
            w.put(m, valueBits);
        }
    }

    /**
     * Writes the codeword of an entry.
     * <p>
     * 항목의 부호어를 쓴다.
     *
     * @param w     bit writer / 비트 쓰기 객체
     * @param entry entry number / 항목 번호
     */
    public void writeEntry(VorbisBits w, int entry) {
        if (lengths[entry] == 0) {
            throw new IllegalArgumentException("unused entry " + entry);
        }
        // 부호어를 최상위 비트부터 한 비트씩 쓴다.
        for (int b = lengths[entry] - 1; b >= 0; b--) {
            w.put((int) ((codes[entry] >> b) & 1), 1);
        }
    }

    /**
     * Returns the value of a vector component as the decoder derives it from the lookup table (the value after the quantization of the
     * stored representation).
     * <p>
     * 디코더가 조회 표에서 구하는 벡터 성분 값(저장 표현으로 양자화된 뒤의 값)을 반환한다.
     *
     * @param entry entry / 항목
     * @param d     dimension / 차원
     * @return the value / 값
     */
    public float value(int entry, int d) {
        float lo;
        float step;
        if (lookupType == 1) {
            lo = grid[0];
            step = grid.length > 1 ? grid[1] - grid[0] : 1f;
        } else {
            lo = Float.MAX_VALUE;
            float hi = -Float.MAX_VALUE;
            // 표의 최솟값과 최댓값으로 격자 간격을 구한다.
            for (float[] v : vectors) {
                for (float x : v) {
                    lo = Math.min(lo, x);
                    hi = Math.max(hi, x);
                }
            }
            step = hi > lo ? (hi - lo) / 15f : 1f;
        }
        float packedStep = VorbisBits.unpack(VorbisBits.pack(step));
        float packedMin = VorbisBits.unpack(VorbisBits.pack(lo));
        return Math.round((vectors[entry][d] - lo) / step) * packedStep + packedMin;
    }
}
