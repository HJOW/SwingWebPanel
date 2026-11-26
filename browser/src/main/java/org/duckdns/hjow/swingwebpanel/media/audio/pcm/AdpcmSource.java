package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ArrayByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;
import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmConverter;

/**
 * Decoder of the two block-based ADPCM codecs of WAV files: IMA/DVI ADPCM (format tag 0x0011) and Microsoft ADPCM (format tag 0x0002).
 * Every block is self-contained (it starts with the predictor state of every channel), so decoding happens one block at a time and
 * seeking jumps to a block and discards the frames before the wanted one. The last block may be shorter than the others; only the
 * complete groups of samples in it are decoded.
 * <p>
 * WAV 파일의 블록 기반 ADPCM 코덱 둘의 디코더: IMA/DVI ADPCM(형식 태그 0x0011)과 Microsoft ADPCM(형식 태그 0x0002). 각 블록은 자체로 완결되며(모든
 * 채널의 예측기 상태로 시작) 블록 하나씩 디코딩하고, 탐색은 블록으로 점프한 뒤 원하는 프레임 앞을 버린다. 마지막 블록은 다른 블록보다 짧을 수 있으며
 * 그 안에서는 완전한 샘플 묶음만 디코딩한다.
 */
public final class AdpcmSource extends BlockPcmSource {

    /**
     * Kind of codec: IMA ADPCM.
     * <p>
     * 코덱 종류: IMA ADPCM.
     */
    public static final int IMA = 0;

    /**
     * Kind of codec: Microsoft ADPCM.
     * <p>
     * 코덱 종류: Microsoft ADPCM.
     */
    public static final int MS = 1;

    /**
     * Codec kind: QuickTime IMA ADPCM (AIFF-C <code>ima4</code>): per channel a packet of 34 bytes (9-bit predictor, 7-bit step index, 64 samples of 4 bits)
     * for every 64 frames. As in the decoders of Apple and ffmpeg, the predictor of a packet header replaces the running predictor only when the step index
     * differs or the predictors are more than 127 apart, so the state flows from packet to packet (seeking decodes from the start).
     * <p>
     * 코덱 종류: QuickTime IMA ADPCM(AIFF-C <code>ima4</code>). 64프레임마다 채널별로 34바이트 패킷(9비트 예측값, 7비트 스텝 인덱스, 4비트 샘플 64개)이
     * 있다. Apple 과 ffmpeg 의 디코더처럼 패킷 머리의 예측값은 스텝 인덱스가 다르거나 예측값이 127보다 많이 다를 때만 진행 중인 예측값을 대체하므로 상태가
     * 패킷에서 패킷으로 이어진다(탐색할 때는 처음부터 디코딩한다).
     */
    public static final int IMA_QT = 2;

    /**
     * The default coefficient pairs of Microsoft ADPCM.
     * <p>
     * Microsoft ADPCM 의 기본 계수 쌍.
     */
    public static final int[] MS_DEFAULT_COEFS = { 256, 0, 512, -256, 0, 0, 192, 64, 240, 0, 460, -208, 392, -232 };

    /**
     * IMA step sizes.
     * <p>
     * IMA 스텝 크기 표.
     */
    static final int[] STEPS = { 7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60, 66, 73, 80, 88,
            97, 107, 118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449, 494, 544, 598, 658, 724, 796, 876, 963, 1060,
            1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845,
            8630, 9493, 10442, 11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767 };

    /**
     * IMA step index adjustments for the low three bits of a nibble.
     * <p>
     * 니블의 하위 3비트에 따른 IMA 스텝 인덱스 조정 값.
     */
    static final int[] INDEX_ADJUST = { -1, -1, -1, -1, 2, 4, 6, 8 };

    /**
     * Microsoft ADPCM delta adaptation table.
     * <p>
     * Microsoft ADPCM 의 델타 적응 표.
     */
    static final int[] MS_ADAPT = { 230, 230, 230, 230, 307, 409, 512, 614, 768, 614, 512, 409, 307, 230, 230, 230 };

    /**
     * Scratch for one encoded block.
     * <p>
     * 인코딩 블록 하나의 작업 공간.
     */
    private final byte[] data;
    /** The encoded input owned by this decoder. / 디코더가 소유하는 인코딩 입력. */
    private final ByteSource source;

    /**
     * Offset of the first block.
     * <p>
     * 첫 블록의 위치.
     */
    private final int start;

    /**
     * End offset (exclusive) of the blocks.
     * <p>
     * 블록들의 끝 위치(미포함).
     */
    private final int end;

    /**
     * Codec kind, {@link #IMA} or {@link #MS}.
     * <p>
     * 코덱 종류. {@link #IMA} 또는 {@link #MS}.
     */
    private final int kind;

    /**
     * Channels in the data.
     * <p>
     * 데이터의 채널 수.
     */
    private final int channels;

    /**
     * Bytes of a block.
     * <p>
     * 블록의 바이트 수.
     */
    private final int blockAlign;

    /**
     * Frames of a complete block.
     * <p>
     * 완전한 블록의 프레임 수.
     */
    private final int samplesPerBlock;

    /**
     * Coefficient pairs of Microsoft ADPCM.
     * <p>
     * Microsoft ADPCM 의 계수 쌍.
     */
    private final int[] coefs;

    /**
     * Mixer for more than two channels, else null.
     * <p>
     * 채널이 둘보다 많을 때의 믹서. 아니면 null.
     */
    private final ChannelMixer mixer;

    /**
     * Running predictor of every channel of a QuickTime IMA ADPCM stream, valid for the state before block {@link #qtNext}.
     * <p>
     * QuickTime IMA ADPCM 스트림에서 채널별로 진행 중인 예측값. {@link #qtNext} 블록 앞의 상태에 대한 값이다.
     */
    private final int[] qtPred;

    /**
     * Running step index of every channel of a QuickTime IMA ADPCM stream.
     * <p>
     * QuickTime IMA ADPCM 스트림에서 채널별로 진행 중인 스텝 인덱스.
     */
    private final int[] qtIdx;

    /**
     * Index of the next block whose packets the running state of a QuickTime IMA ADPCM stream expects.
     * <p>
     * QuickTime IMA ADPCM 스트림의 진행 상태가 다음에 기대하는 블록의 번호.
     */
    private int qtNext;

    /**
     * Decoded samples of the current block, interleaved.
     * <p>
     * 현재 블록의 디코딩된 샘플(인터리브).
     */
    private final short[] block;

    /**
     * Frames in the current block (0 when none is decoded).
     * <p>
     * 현재 블록의 프레임 수(디코딩된 것이 없으면 0).
     */
    private int blockFrames;

    /**
     * Next frame of the current block to output.
     * <p>
     * 현재 블록에서 다음에 출력할 프레임.
     */
    private int blockPos;

    /**
     * Index of the next block to decode.
     * <p>
     * 다음에 디코딩할 블록 번호.
     */
    private int nextBlock;

    /**
     * Number of blocks, the last one possibly incomplete.
     * <p>
     * 블록 수. 마지막 블록은 불완전할 수 있다.
     */
    private final int blocks;

    /**
     * Creates the source.
     * <p>
     * 소스를 만든다.
     *
     * @param data            encoded data / 인코딩된 데이터
     * @param start           offset of the first block / 첫 블록의 위치
     * @param end             end of the blocks (exclusive) / 블록들의 끝(미포함)
     * @param kind            {@link #IMA} or {@link #MS} / {@link #IMA} 또는 {@link #MS}
     * @param channels        channels, 1..8 / 채널 수(1..8)
     * @param sampleRate      sample rate / 샘플링 주파수
     * @param blockAlign      bytes per block / 블록의 바이트 수
     * @param samplesPerBlock frames per complete block / 완전한 블록의 프레임 수
     * @param coefs           coefficient pairs for MS ADPCM, may be null for IMA / MS ADPCM 의 계수 쌍. IMA 면 null 가능
     * @param mixer           mixer for more than two channels, may be null / 채널이 둘보다 많을 때의 믹서. null 가능
     */
    public AdpcmSource(byte[] data, int start, int end, int kind, int channels, int sampleRate, int blockAlign, int samplesPerBlock,
            int[] coefs, ChannelMixer mixer) {
        this(new ArrayByteSource(data), start, end, kind, channels, sampleRate, blockAlign, samplesPerBlock, coefs, mixer);
    }

    /**
     * Creates an ADPCM decoder from an owned byte source. / 소유하는 바이트 원천으로 ADPCM 디코더를 만든다.
     * @param source encoded blocks / 인코딩된 블록
     * @param start first block position / 첫 블록 위치
     * @param end exclusive end of the blocks / 블록 끝 위치. 미포함
     * @param kind ADPCM encoding / ADPCM 인코딩
     * @param channels input channels / 입력 채널 수
     * @param sampleRate frames per second / 초당 프레임 수
     * @param blockAlign byte count of each full block / 완전한 블록의 바이트 수
     * @param samplesPerBlock frames per full block / 완전한 블록의 프레임 수
     * @param coefs Microsoft ADPCM coefficients or null / Microsoft ADPCM 계수 또는 null
     * @param mixer optional stereo mixer / 선택적 스테레오 믹서
     */
    AdpcmSource(ByteSource source, int start, int end, int kind, int channels, int sampleRate, int blockAlign, int samplesPerBlock,
            int[] coefs, ChannelMixer mixer) {
        super(sampleRate, Math.min(channels, 2), countFrames(end - start, kind, channels, blockAlign, samplesPerBlock), 4096);
        this.source = source;
        this.data = new byte[Math.min(blockAlign, end - start)];
        this.start = start;
        this.end = end;
        this.kind = kind;
        this.channels = channels;
        this.blockAlign = blockAlign;
        this.samplesPerBlock = samplesPerBlock;
        this.coefs = coefs;
        this.mixer = channels > 2 ? (mixer != null ? mixer : ChannelMixer.forChannels(channels)) : null;
        this.block = new short[samplesPerBlock * channels];
        this.qtPred = new int[channels];
        this.qtIdx = new int[channels];
        this.blocks = (int) (((long) (end - start) + blockAlign - 1) / blockAlign);
    }

    /**
     * Counts the frames of the whole data: complete blocks plus the complete groups of the last short block.
     * <p>
     * 전체 데이터의 프레임 수를 센다: 완전한 블록과 마지막 짧은 블록의 완전한 묶음.
     *
     * @param length          bytes of block data / 블록 데이터의 바이트 수
     * @param kind            codec kind / 코덱 종류
     * @param channels        channels / 채널 수
     * @param blockAlign      bytes per block / 블록의 바이트 수
     * @param samplesPerBlock frames per block / 블록의 프레임 수
     * @return frames / 프레임 수
     */
    static long countFrames(int length, int kind, int channels, int blockAlign, int samplesPerBlock) {
        long full = length / blockAlign;
        return full * samplesPerBlock + Math.min(partialFrames(length % blockAlign, kind, channels), samplesPerBlock);
    }

    /**
     * Counts the frames of an incomplete last block.
     * <p>
     * 불완전한 마지막 블록의 프레임 수를 센다.
     *
     * @param bytes    bytes of the block / 블록의 바이트 수
     * @param kind     codec kind / 코덱 종류
     * @param channels channels / 채널 수
     * @return frames / 프레임 수
     */
    private static int partialFrames(int bytes, int kind, int channels) {
        if (kind == IMA_QT) {
            return bytes >= 34 * channels ? 64 : 0;
        }
        if (kind == IMA) {
            if (bytes < 4 * channels) {
                return 0;
            }
            return 1 + (bytes - 4 * channels) / (4 * channels) * 8;
        }
        if (bytes < 7 * channels) {
            return 0;
        }
        return 2 + (bytes - 7 * channels) * 2 / channels;
    }

    /**
     * Decodes the next block for the wide path ({@link AdpcmFloatSource}): the samples are then in {@link #blockSamples()}. The 16 bit path
     * does not use this method.
     * <p>
     * 넓은 경로({@link AdpcmFloatSource})를 위해 다음 블록을 디코딩한다. 샘플은 그다음 {@link #blockSamples()} 에 있다. 16비트 경로는 이 메서드를 쓰지 않는다.
     *
     * @return frames of the block (0 for a block without samples), or -1 at the end / 블록의 프레임 수(샘플이 없는 블록은 0). 끝이면 -1
     * @throws MediaException if a block is damaged or the thread was interrupted / 블록이 손상되었거나 스레드가 인터럽트되었으면
     */
    int decodeNextBlock() throws MediaException {
        if (nextBlock >= blocks) {
            return -1;
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new MediaException(MediaException.Kind.CANCELLED, "audio decoding was interrupted");
        }
        return decodeBlock(nextBlock++);
    }

    /**
     * Returns the samples of the block that {@link #decodeNextBlock()} decoded: interleaved 16 bit values, {@link #sourceChannels()} per frame.
     * <p>
     * {@link #decodeNextBlock()} 가 디코딩한 블록의 샘플을 반환한다: 인터리브된 16비트 값. 프레임마다 {@link #sourceChannels()} 개.
     *
     * @return the samples / 샘플
     */
    short[] blockSamples() {
        return block;
    }

    /**
     * Returns the number of channels of the file (the 16 bit path outputs at most two).
     * <p>
     * 파일의 채널 수를 반환한다(16비트 경로는 최대 둘을 출력한다).
     *
     * @return channels / 채널 수
     */
    int sourceChannels() {
        return channels;
    }

    /**
     * Returns the largest number of frames of a block.
     * <p>
     * 블록의 최대 프레임 수를 반환한다.
     *
     * @return frames / 프레임 수
     */
    int maxBlockFrames() {
        return samplesPerBlock;
    }

    /**
     * Decodes block after block and converts them to output PCM.
     * <p>
     * 블록을 차례로 디코딩해 출력 PCM 으로 바꾼다.
     *
     * @param out output buffer / 출력 버퍼
     * @return bytes written or -1 at the end / 쓴 바이트 수. 끝이면 -1
     * @throws MediaException if a block is damaged or the thread was interrupted / 블록이 손상되었거나 스레드가 인터럽트되었으면
     */
    protected int fill(byte[] out) throws MediaException {
        int outCh = getChannels();
        int cap = out.length / (outCh * 2);
        int o = 0;
        int produced = 0;
        float[] frame = new float[channels];
        float[] mixed = new float[2];
        // 출력 버퍼가 찰 때까지 블록의 프레임을 변환한다.
        while (produced < cap) {
            if (blockPos >= blockFrames) {
                if (nextBlock >= blocks) {
                    break;
                }
                if (Thread.currentThread().isInterrupted()) {
                    throw new MediaException(MediaException.Kind.CANCELLED, "audio decoding was interrupted");
                }
                blockFrames = decodeBlock(nextBlock++);
                blockPos = 0;
                continue;
            }
            int base = blockPos * channels;
            if (channels <= 2) {
                // 채널마다 16비트 샘플을 쓴다.
                for (int c = 0; c < channels; c++) {
                    short s = block[base + c];
                    out[o++] = (byte) s;
                    out[o++] = (byte) (s >> 8);
                }
            } else {
                // 채널마다 float 로 바꿔 믹서에 넣는다.
                for (int c = 0; c < channels; c++) {
                    frame[c] = block[base + c] / 32768f;
                }
                mixer.mix(frame, mixed);
                // 채널마다 값을 출력한다.
                for (int c = 0; c < 2; c++) {
                    short s = PcmConverter.clip16(mixed[c]);
                    out[o++] = (byte) s;
                    out[o++] = (byte) (s >> 8);
                }
            }
            blockPos++;
            produced++;
        }
        return produced == 0 ? -1 : o;
    }

    /**
     * Returns whether this source can seek.
     * <p>
     * 이 소스가 탐색할 수 있는지 반환한다.
     *
     * @return always true / 항상 true
     */
    protected boolean canSeek() {
        return true;
    }

    /**
     * Jumps to the block that contains a frame.
     * <p>
     * 프레임이 들어 있는 블록으로 점프한다.
     *
     * @param frame frame index / 프레임 번호
     * @return the first frame of that block / 그 블록의 첫 프레임
     */
    protected long seekTo(long frame) {
        long b = Math.min(frame / samplesPerBlock, blocks);
        nextBlock = (int) b;
        blockFrames = 0;
        blockPos = 0;
        return b * samplesPerBlock;
    }

    /**
     * Decodes one block into {@link #block}.
     * <p>
     * 블록 하나를 {@link #block} 에 디코딩한다.
     *
     * @param index block number / 블록 번호
     * @return frames decoded / 디코딩한 프레임 수
     * @throws MediaException if the block cannot be read or its header is invalid / 블록을 읽을 수 없거나 헤더가 올바르지 않으면
     */
    private int decodeBlock(int index) throws MediaException {
        if (kind == IMA_QT) {
            // 진행 상태가 이 블록 앞에 있도록 필요하면 처음부터 앞선 블록을 디코딩해 상태만 이어 간다.
            if (index < qtNext) {
                java.util.Arrays.fill(qtPred, 0);
                java.util.Arrays.fill(qtIdx, 0);
                qtNext = 0;
            }
            // 건너뛴 블록의 패킷도 상태를 바꾸므로 차례로 디코딩해 버린다.
            while (qtNext < index) {
                decodeImaQt(0, readEncodedBlock(qtNext));
                qtNext++;
            }
            int frames = decodeImaQt(0, readEncodedBlock(index));
            qtNext = index + 1;
            return frames;
        }
        int len = readEncodedBlock(index);
        return kind == IMA ? decodeIma(0, len) : decodeMs(0, len);
    }

    /**
     * Reads the possibly short block into the scratch buffer. / 짧을 수 있는 블록을 작업 공간으로 읽는다.
     * @param index block number / 블록 번호
     * @return bytes read / 읽은 바이트 수
     * @throws MediaException if the input cannot be read / 입력을 읽을 수 없으면
     */
    private int readEncodedBlock(int index) throws MediaException {
        int offset = start + index * blockAlign;
        int length = Math.min(blockAlign, end - offset);
        PcmSources.readBlock(source, offset, data, length);
        return length;
    }

    /** Releases the encoded input with the decoder. / 디코더와 함께 인코딩 입력을 해제한다. */
    protected void release() {
        source.close();
    }

    /**
     * Decodes an IMA ADPCM block.
     * <p>
     * IMA ADPCM 블록을 디코딩한다.
     *
     * @param pos offset of the block / 블록의 위치
     * @param len bytes available in the block / 블록에서 쓸 수 있는 바이트 수
     * @return frames / 프레임 수
     * @throws MediaException if a step index is out of range / 스텝 인덱스가 범위를 벗어나면
     */
    private int decodeIma(int pos, int len) throws MediaException {
        int ch = channels;
        int frames = Math.min(partialFrames(len, IMA, ch), samplesPerBlock);
        if (frames == 0) {
            return 0;
        }
        int[] pred = new int[ch];
        int[] idx = new int[ch];
        // 채널마다 예측값과 스텝 인덱스를 읽고 첫 프레임을 채운다.
        for (int c = 0; c < ch; c++) {
            pred[c] = (short) Bytes.le16(data, pos + 4 * c);
            idx[c] = data[pos + 4 * c + 2] & 0xFF;
            if (idx[c] > 88) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid IMA ADPCM step index");
            }
            block[c] = (short) pred[c];
        }
        int groups = (frames - 1) / 8;
        int src = pos + 4 * ch;
        // 8 샘플 묶음마다 채널별 4바이트를 읽어 디코딩한다.
        for (int g = 0; g < groups; g++) {
            for (int c = 0; c < ch; c++) {
                // 묶음 안의 네 바이트를 차례로 읽는다.
                for (int k = 0; k < 4; k++) {
                    int b = data[src++] & 0xFF;
                    // 한 바이트의 두 니블(낮은 쪽이 먼저)을 처리한다.
                    for (int h = 0; h < 2; h++) {
                        int nib = (b >> (4 * h)) & 0xF;
                        int step = STEPS[idx[c]];
                        int diff = step >> 3;
                        if ((nib & 1) != 0) {
                            diff += step >> 2;
                        }
                        if ((nib & 2) != 0) {
                            diff += step >> 1;
                        }
                        if ((nib & 4) != 0) {
                            diff += step;
                        }
                        pred[c] += (nib & 8) != 0 ? -diff : diff;
                        pred[c] = Math.max(-32768, Math.min(32767, pred[c]));
                        idx[c] = Math.max(0, Math.min(88, idx[c] + INDEX_ADJUST[nib & 7]));
                        block[(1 + g * 8 + k * 2 + h) * ch + c] = (short) pred[c];
                    }
                }
            }
        }
        return frames;
    }

    /**
     * Decodes a QuickTime IMA ADPCM block: for every channel a packet of 34 bytes with a 16-bit header (9-bit predictor, 7-bit step index) and 32 bytes of
     * 64 nibbles (the low nibble first). The running state is kept in {@link #qtPred} and {@link #qtIdx}.
     * <p>
     * QuickTime IMA ADPCM 블록을 디코딩한다: 채널마다 16비트 머리(9비트 예측값, 7비트 스텝 인덱스)와 니블 64개(낮은 쪽이 먼저)의 32바이트로 된 34바이트
     * 패킷이다. 진행 상태는 {@link #qtPred}, {@link #qtIdx} 에 있다.
     *
     * @param pos offset of the block / 블록의 위치
     * @param len bytes available in the block / 블록에서 쓸 수 있는 바이트 수
     * @return frames, 0 if the block does not hold a packet of every channel / 프레임 수. 모든 채널의 패킷이 없으면 0
     * @throws MediaException if a step index is out of range / 스텝 인덱스가 범위를 벗어나면
     */
    private int decodeImaQt(int pos, int len) throws MediaException {
        int ch = channels;
        if (len < 34 * ch) {
            return 0;
        }
        // 채널마다 패킷의 머리를 읽고 니블 64개를 디코딩한다.
        for (int c = 0; c < ch; c++) {
            int p = pos + 34 * c;
            int header = (short) Bytes.be16(data, p);
            int stepIndex = header & 0x7F;
            int predictor = header & ~0x7F;
            if (stepIndex > 88) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid IMA ADPCM step index");
            }
            // 머리의 예측값은 스텝 인덱스가 다르거나 진행 중인 값과 크게 다를 때만 쓴다.
            if (qtIdx[c] != stepIndex || Math.abs(predictor - qtPred[c]) > 0x7F) {
                qtIdx[c] = stepIndex;
                qtPred[c] = predictor;
            }
            int pred = qtPred[c];
            int idx = qtIdx[c];
            // 32바이트의 니블 64개를 차례로 복원한다.
            for (int k = 0; k < 32; k++) {
                int b = data[p + 2 + k] & 0xFF;
                for (int h = 0; h < 2; h++) { // 한 바이트의 두 니블(낮은 쪽이 먼저)을 처리한다
                    int nib = (b >> (4 * h)) & 0xF;
                    int step = STEPS[idx];
                    int diff = step >> 3;
                    if ((nib & 1) != 0) {
                        diff += step >> 2;
                    }
                    if ((nib & 2) != 0) {
                        diff += step >> 1;
                    }
                    if ((nib & 4) != 0) {
                        diff += step;
                    }
                    pred += (nib & 8) != 0 ? -diff : diff;
                    pred = Math.max(-32768, Math.min(32767, pred));
                    idx = Math.max(0, Math.min(88, idx + INDEX_ADJUST[nib & 7]));
                    block[(2 * k + h) * ch + c] = (short) pred;
                }
            }
            qtPred[c] = pred;
            qtIdx[c] = idx;
        }
        return 64;
    }

    /**
     * Decodes a Microsoft ADPCM block.
     * <p>
     * Microsoft ADPCM 블록을 디코딩한다.
     *
     * @param pos offset of the block / 블록의 위치
     * @param len bytes available in the block / 블록에서 쓸 수 있는 바이트 수
     * @return frames / 프레임 수
     * @throws MediaException if a predictor index is out of range / 예측기 인덱스가 범위를 벗어나면
     */
    private int decodeMs(int pos, int len) throws MediaException {
        int ch = channels;
        int frames = Math.min(partialFrames(len, MS, ch), samplesPerBlock);
        if (frames == 0) {
            return 0;
        }
        int pairs = coefs.length / 2;
        int[] c1 = new int[ch];
        int[] c2 = new int[ch];
        int[] delta = new int[ch];
        int[] s1 = new int[ch];
        int[] s2 = new int[ch];
        // 채널마다 헤더(예측기 번호, 델타, 직전 두 샘플)를 읽는다.
        for (int c = 0; c < ch; c++) {
            int p = data[pos + c] & 0xFF;
            if (p >= pairs) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid MS ADPCM predictor index");
            }
            c1[c] = coefs[2 * p];
            c2[c] = coefs[2 * p + 1];
            delta[c] = (short) Bytes.le16(data, pos + ch + 2 * c);
            s1[c] = (short) Bytes.le16(data, pos + 3 * ch + 2 * c);
            s2[c] = (short) Bytes.le16(data, pos + 5 * ch + 2 * c);
            block[c] = (short) s2[c];
            block[ch + c] = (short) s1[c];
        }
        int src = pos + 7 * ch;
        int total = (frames - 2) * ch;
        // 니블마다 채널을 돌아가며 한 샘플씩 복원한다(상위 니블이 먼저).
        for (int i = 0; i < total; i++) {
            int b = data[src + (i >> 1)] & 0xFF;
            int nib = (i & 1) == 0 ? b >> 4 : b & 0xF;
            int c = i % ch;
            int signed = nib >= 8 ? nib - 16 : nib;
            int pred = (s1[c] * c1[c] + s2[c] * c2[c]) / 256 + signed * delta[c];
            pred = Math.max(-32768, Math.min(32767, pred));
            block[(2 + i / ch) * ch + c] = (short) pred;
            s2[c] = s1[c];
            s1[c] = pred;
            delta[c] = Math.max(16, (MS_ADAPT[nib] * delta[c]) >> 8);
        }
        return frames;
    }
}
