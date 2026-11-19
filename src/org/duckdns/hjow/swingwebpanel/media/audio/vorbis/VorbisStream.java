package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * The audio packet decoder of a Vorbis stream (Vorbis I specification, section 4.3): from the packet it reads the mode and the window
 * shape flags, decodes floor and residue of every channel, undoes the channel coupling, multiplies the spectrum by the floor, applies the
 * inverse MDCT and the power-sine window and overlaps the result with the second half of the previous block. The first packet after a
 * start or a reset only primes the overlap and produces no output; every later packet produces a quarter of the previous plus a
 * quarter of its own block size in frames.
 * <p>
 * Vorbis 스트림의 오디오 패킷 디코더(Vorbis I 규격 4.3절): 패킷에서 모드와 창 모양 플래그를 읽고, 채널마다 플로어와 잔차를 디코딩하며, 채널 결합을
 * 되돌리고, 스펙트럼에 플로어를 곱하고, 역 MDCT 와 power-sine 창을 적용해 이전 블록의 뒤쪽 절반과 겹친다. 시작이나 초기화 뒤의 첫 패킷은 겹침만
 * 준비하고 출력을 만들지 않으며, 이후 패킷마다 이전 블록 크기의 4분의 1과 자기 블록 크기의 4분의 1을 합한 만큼의 프레임이 나온다.
 */
final class VorbisStream {

    /**
     * The configuration.
     * <p>
     * 설정.
     */
    private final VorbisSetup setup;

    /**
     * Number of channels.
     * <p>
     * 채널 수.
     */
    private final int channels;

    /**
     * The transform of the short blocks.
     * <p>
     * 짧은 블록의 변환.
     */
    private final Imdct imdct0;

    /**
     * The transform of the long blocks.
     * <p>
     * 긴 블록의 변환.
     */
    private final Imdct imdct1;

    /**
     * Rising window slope of half a short block.
     * <p>
     * 짧은 블록 절반 길이의 상승 창 구간.
     */
    private final float[] slope0;

    /**
     * Rising window slope of half a long block.
     * <p>
     * 긴 블록 절반 길이의 상승 창 구간.
     */
    private final float[] slope1;

    /**
     * Spectrum of every channel.
     * <p>
     * 채널마다의 스펙트럼.
     */
    private final float[][] spectrum;

    /**
     * Floor curve of every channel.
     * <p>
     * 채널마다의 플로어 곡선.
     */
    private final float[][] floor;

    /**
     * Time samples of the block being decoded, per channel.
     * <p>
     * 디코딩 중인 블록의 채널마다의 시간 샘플.
     */
    private final float[][] block;

    /**
     * Second half of the previous block, per channel.
     * <p>
     * 이전 블록의 뒤쪽 절반(채널마다).
     */
    private final float[][] carry;

    /**
     * Output of the last packet, per channel; {@link #decode(byte[])} returns how many values are valid.
     * <p>
     * 마지막 패킷의 출력(채널마다). 유효한 개수는 {@link #decode(byte[])} 가 반환한다.
     */
    final float[][] output;

    /**
     * Block size of the previous packet, or 0 if there is none.
     * <p>
     * 이전 패킷의 블록 크기. 없으면 0.
     */
    private int previousSize;

    /**
     * Creates the decoder.
     * <p>
     * 디코더를 만든다.
     *
     * @param setup the stream configuration / 스트림 설정
     */
    VorbisStream(VorbisSetup setup) {
        this.setup = setup;
        this.channels = setup.channels;
        int half1 = setup.blockSize1 / 2;
        imdct0 = new Imdct(setup.blockSize0);
        imdct1 = new Imdct(setup.blockSize1);
        slope0 = slope(setup.blockSize0 / 2);
        slope1 = slope(half1);
        spectrum = new float[channels][half1];
        floor = new float[channels][half1];
        block = new float[channels][setup.blockSize1];
        carry = new float[channels][half1];
        output = new float[channels][half1];
    }

    /**
     * Computes the rising part of the Vorbis window: sin(pi/2 sin^2((k + 1/2) / L pi/2)) for k below L.
     * <p>
     * Vorbis 창의 상승 구간을 계산한다: k 가 L 미만일 때 sin(pi/2 sin^2((k + 1/2) / L pi/2)).
     *
     * @param len the length L / 길이 L
     * @return the slope / 상승 구간
     */
    private static float[] slope(int len) {
        float[] w = new float[len];
        // 구간의 샘플마다 창 값을 계산한다.
        for (int k = 0; k < len; k++) {
            double s = Math.sin((k + 0.5) / len * Math.PI / 2);
            w[k] = (float) Math.sin(Math.PI / 2 * s * s);
        }
        return w;
    }

    /**
     * Forgets the previous block, as after a seek: the next packet only primes the overlap.
     * <p>
     * 탐색 뒤처럼 이전 블록을 잊는다. 다음 패킷은 겹침만 준비한다.
     */
    void reset() {
        previousSize = 0;
    }

    /**
     * Returns the block size of an audio packet without decoding it.
     * <p>
     * 오디오 패킷을 디코딩하지 않고 블록 크기를 반환한다.
     *
     * @param packet the packet / 패킷
     * @return the block size, or -1 if the packet is not an audio packet / 블록 크기. 오디오 패킷이 아니면 -1
     */
    int blockSizeOf(byte[] packet) {
        VorbisBitReader br = new VorbisBitReader(packet);
        if (br.read(1) != 0) {
            return -1;
        }
        int mode = br.read(VorbisBitReader.ilog(setup.modeLong.length - 1));
        if (br.isEndOfPacket() || mode >= setup.modeLong.length) {
            return -1;
        }
        return setup.modeLong[mode] ? setup.blockSize1 : setup.blockSize0;
    }

    /**
     * Decodes one audio packet.
     * <p>
     * 오디오 패킷 하나를 디코딩한다.
     *
     * @param packet the packet / 패킷
     * @return the number of frames written to {@link #output}, 0 for a priming packet, -1 if the packet was ignored (not an audio packet) / {@link #output} 에 쓴 프레임 수. 겹침을 준비하는 패킷이면 0, 무시한 패킷(오디오 패킷이 아님)이면 -1
     * @throws MediaException CORRUPT for an invalid mode number / 모드 번호가 올바르지 않으면 CORRUPT
     */
    int decode(byte[] packet) throws MediaException {
        VorbisBitReader br = new VorbisBitReader(packet);
        if (br.read(1) != 0) {
            return -1;
        }
        int modes = setup.modeLong.length;
        int mode = br.read(VorbisBitReader.ilog(modes - 1));
        if (br.isEndOfPacket() || mode >= modes) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis mode number");
        }
        boolean isLong = setup.modeLong[mode];
        int n = isLong ? setup.blockSize1 : setup.blockSize0;
        boolean previousLong = true;
        boolean nextLong = true;
        if (isLong) {
            previousLong = br.read(1) != 0;
            nextLong = br.read(1) != 0;
        }
        int half = n / 2;
        VorbisSetup.Mapping mapping = setup.mappings[setup.modeMapping[mode]];
        boolean[] noResidue = new boolean[channels];
        // 채널마다 플로어를 디코딩한다(쓰지 않는 채널은 곡선을 0 으로 채운다).
        for (int c = 0; c < channels; c++) {
            VorbisFloor f = setup.floors[mapping.submapFloor[mapping.mux[c]]];
            boolean used = f.decode(br, floor[c], half);
            noResidue[c] = !used;
            if (!used) {
                java.util.Arrays.fill(floor[c], 0, half, 0f);
            }
            java.util.Arrays.fill(spectrum[c], 0, half, 0f);
        }
        // 결합된 두 채널 중 하나라도 소리가 있으면 둘 다 잔차를 디코딩해야 한다.
        for (int s = 0; s < mapping.magnitude.length; s++) {
            int mg = mapping.magnitude[s];
            int an = mapping.angle[s];
            if (!noResidue[mg] || !noResidue[an]) {
                noResidue[mg] = false;
                noResidue[an] = false;
            }
        }
        decodeResidues(br, mapping, noResidue, half);
        // 결합 단계를 거꾸로 되돌려 크기/각도를 left/right 로 복원한다.
        for (int s = mapping.magnitude.length - 1; s >= 0; s--) {
            float[] m = spectrum[mapping.magnitude[s]];
            float[] a = spectrum[mapping.angle[s]];
            // 항목마다 반복해서 처리한다.
            for (int i = 0; i < half; i++) {
                float mv = m[i];
                float av = a[i];
                if (mv > 0) {
                    if (av > 0) {
                        a[i] = mv - av;
                    } else {
                        a[i] = mv;
                        m[i] = mv + av;
                    }
                } else {
                    if (av > 0) {
                        a[i] = mv + av;
                    } else {
                        a[i] = mv;
                        m[i] = mv - av;
                    }
                }
            }
        }
        Imdct t = isLong ? imdct1 : imdct0;
        // 채널마다 스펙트럼에 플로어를 곱하고 역변환한 뒤 창을 씌운다.
        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < half; i++) {
                spectrum[c][i] *= floor[c][i];
            }
            t.inverse(spectrum[c], block[c]);
            window(block[c], n, isLong, previousLong, nextLong);
        }
        int outLen = 0;
        if (previousSize > 0) {
            outLen = previousSize / 4 + n / 4;
            // 이전 블록의 뒤쪽 절반과 이번 블록의 앞쪽을 겹쳐 더한다.
            for (int c = 0; c < channels; c++) {
                int prevHalf = previousSize / 2;
                int base = half - outLen;
                // 항목마다 반복해서 처리한다.
                for (int j = 0; j < outLen; j++) {
                    float v = j < prevHalf ? carry[c][j] : 0f;
                    int idx = base + j;
                    if (idx >= 0) {
                        v += block[c][idx];
                    }
                    output[c][j] = v;
                }
            }
        }
        // 이번 블록의 뒤쪽 절반을 다음 겹침을 위해 보관한다.
        for (int c = 0; c < channels; c++) {
            System.arraycopy(block[c], half, carry[c], 0, half);
        }
        previousSize = n;
        return outLen;
    }

    /**
     * Decodes the residues of all submaps into the spectrum arrays.
     * <p>
     * 모든 하위 맵의 잔차를 스펙트럼 배열에 디코딩한다.
     *
     * @param br         packet bits / 패킷 비트
     * @param mapping    the mapping / 매핑
     * @param noResidue  true for channels without residue / 잔차가 없는 채널이면 true
     * @param half       half of the block size / 블록 크기의 절반
     */
    private void decodeResidues(VorbisBitReader br, VorbisSetup.Mapping mapping, boolean[] noResidue, int half) {
        // 하위 맵마다 그 하위 맵에 속한 채널들의 잔차를 함께 디코딩한다.
        for (int s = 0; s < mapping.submapFloor.length; s++) {
            int count = 0;
            float[][] vec = new float[channels][];
            boolean[] skip = new boolean[channels];
            // 항목마다 반복해서 처리한다.
            for (int c = 0; c < channels; c++) {
                if (mapping.mux[c] == s) {
                    vec[count] = spectrum[c];
                    skip[count] = noResidue[c];
                    count++;
                }
            }
            if (count > 0) {
                setup.residues[mapping.submapResidue[s]].decode(br, vec, skip, count, half);
            }
        }
    }

    /**
     * Multiplies a block by the Vorbis window, whose left and right slopes depend on the neighbouring block sizes.
     * <p>
     * 블록에 Vorbis 창을 곱한다. 창의 왼쪽/오른쪽 구간은 이웃 블록의 크기에 따라 달라진다.
     *
     * @param y            the block / 블록
     * @param n            block size / 블록 크기
     * @param isLong       true for a long block / 긴 블록이면 true
     * @param previousLong true if the previous block is long / 이전 블록이 길면 true
     * @param nextLong     true if the next block is long / 다음 블록이 길면 true
     */
    private void window(float[] y, int n, boolean isLong, boolean previousLong, boolean nextLong) {
        int q = n / 4;
        int s4 = setup.blockSize0 / 4;
        int leftStart = 0;
        int leftEnd = n / 2;
        float[] leftSlope = isLong ? slope1 : slope0;
        if (isLong && !previousLong) {
            leftStart = q - s4;
            leftEnd = q + s4;
            leftSlope = slope0;
        }
        int rightStart = n / 2;
        int rightEnd = n;
        float[] rightSlope = isLong ? slope1 : slope0;
        if (isLong && !nextLong) {
            rightStart = 3 * q - s4;
            rightEnd = 3 * q + s4;
            rightSlope = slope0;
        }
        // 창 밖은 0, 안쪽 구간은 곡선, 가운데는 1 을 곱한다.
        for (int i = 0; i < n; i++) {
            float w;
            if (i < leftStart) {
                w = 0f;
            } else if (i < leftEnd) {
                w = leftSlope[i - leftStart];
            } else if (i < rightStart) {
                w = 1f;
            } else if (i < rightEnd) {
                w = rightSlope[rightEnd - 1 - i];
            } else {
                w = 0f;
            }
            y[i] *= w;
        }
    }
}
