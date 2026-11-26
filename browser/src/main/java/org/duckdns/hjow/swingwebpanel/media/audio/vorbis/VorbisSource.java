package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

import java.util.LinkedList;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmConverter;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggPacket;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggPacketStream;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggStream;

/**
 * Source of a Vorbis stream in Ogg: it reads the audio packets of the logical stream, decodes them one at a time and converts the
 * float output to 16 bits (more than two channels are mixed down). The end of the stream is trimmed to its granule position, so
 * the padding of the last block is not played. Seeking starts at the page before the wanted frame, learns the exact frame position
 * of its packets from the granule position of the page and the block sizes of the packets, decodes one packet to prime the overlap
 * and then discards frames up to the wanted position, so the position is exact.
 * <p>
 * Ogg 안의 Vorbis 스트림의 소스: 논리 스트림의 오디오 패킷을 읽어 하나씩 디코딩하고 float 출력을 16비트로 바꾼다(채널이 둘보다 많으면 다운믹스).
 * 스트림의 끝은 그래뉼 위치에 맞춰 잘라 내므로 마지막 블록의 패딩은 재생하지 않는다. 탐색은 원하는 프레임 앞의 페이지에서 시작해, 페이지의 그래뉼
 * 위치와 패킷의 블록 크기로 패킷들의 정확한 프레임 위치를 알아내고, 패킷 하나를 디코딩해 겹침을 준비한 뒤 원하는 위치까지 프레임을 버리므로 위치가
 * 정확하다.
 */
final class VorbisSource extends BlockPcmSource {

    /** Encoded input owned by the PCM source. / PCM 소스가 소유하는 인코딩 입력. */
    private final ByteSource data;

    /**
     * The logical stream.
     * <p>
     * 논리 스트림.
     */
    private final OggStream stream;

    /**
     * Number of the first page that holds audio packets.
     * <p>
     * 오디오 패킷이 있는 첫 페이지의 번호.
     */
    private final int audioStartPage;

    /**
     * The packet decoder.
     * <p>
     * 패킷 디코더.
     */
    private final VorbisStream core;

    /**
     * The channels of the stream.
     * <p>
     * 스트림의 채널 수.
     */
    private final int channels;

    /**
     * Mixer for more than two channels, else null.
     * <p>
     * 채널이 둘보다 많을 때의 믹서. 아니면 null.
     */
    private final ChannelMixer mixer;

    /**
     * The packet reader.
     * <p>
     * 패킷 읽기 객체.
     */
    private OggPacketStream packets;

    /**
     * Packets read ahead while seeking.
     * <p>
     * 탐색하며 미리 읽어 둔 패킷.
     */
    private final LinkedList<OggPacket> queue = new LinkedList<OggPacket>();

    /**
     * Number of frames that the decoder has produced since the start of the stream (in the timeline of the granule positions).
     * <p>
     * 스트림 시작 이후 디코더가 만들어 낸 프레임 수(그래뉼 위치의 시간축 기준).
     */
    private long produced;

    /**
     * Scratch for one frame.
     * <p>
     * 프레임 하나의 작업 공간.
     */
    private final float[] frameBuf;

    /**
     * Scratch for the mixed frame.
     * <p>
     * 섞은 프레임의 작업 공간.
     */
    private final float[] mixed = new float[2];

    /**
     * Creates the source.
     * <p>
     * 소스를 만든다.
     *
     * @param data           the encoded input / 인코딩 입력
     * @param stream         the logical stream / 논리 스트림
     * @param setup          the configuration / 설정
     * @param audioStartPage first page with audio packets / 오디오 패킷이 있는 첫 페이지
     */
    VorbisSource(ByteSource data, OggStream stream, VorbisSetup setup, int audioStartPage) {
        super(setup.sampleRate, Math.min(setup.channels, 2), stream.getLastGranule(), Math.max(4096, setup.blockSize1 / 2));
        this.data = data;
        this.stream = stream;
        this.audioStartPage = audioStartPage;
        this.core = new VorbisStream(setup);
        this.channels = setup.channels;
        // Vorbis I 의 채널 순서(규격 4.3.9절)는 WAV 와 달리 중앙이 오른쪽보다 앞선다.
        this.mixer = channels > 2 ? ChannelMixer.forVorbisOrder(channels) : null;
        this.frameBuf = new float[channels];
        this.packets = stream.openPackets(audioStartPage);
    }

    /**
     * Returns the next packet.
     * <p>
     * 다음 패킷을 반환한다.
     *
     * @return the packet or null at the end / 패킷. 끝이면 null
     * @throws MediaException on a reading failure / 읽기 실패 시
     */
    private OggPacket nextPacket() throws MediaException {
        if (!queue.isEmpty()) {
            return queue.removeFirst();
        }
        return packets.next();
    }

    /**
     * Decodes packets until one produces output and converts it to 16 bits.
     * <p>
     * 출력을 만드는 패킷이 나올 때까지 디코딩하고 16비트로 바꾼다.
     *
     * @param out output buffer / 출력 버퍼
     * @return bytes written or -1 at the end / 쓴 바이트 수. 끝이면 -1
     * @throws MediaException CORRUPT for invalid packets / 패킷이 올바르지 않으면 CORRUPT
     */
    protected int fill(byte[] out) throws MediaException {
        int n = nextBlock();
        return n < 0 ? -1 : convert(n, out);
    }

    /**
     * Returns the decoded channels of the block that {@link #nextBlock()} produced, one array per channel (the wide path reads them directly).
     * <p>
     * {@link #nextBlock()} 이 만든 블록의 디코딩된 채널을 채널마다 배열 하나로 반환한다(넓은 경로가 직접 읽는다).
     *
     * @return the decoded samples / 디코딩된 표본
     */
    float[][] blockOutput() {
        return core.output;
    }

    /**
     * Returns the number of channels of the stream (the 16 bit path outputs at most two).
     * <p>
     * 스트림의 채널 수를 반환한다(16비트 경로는 최대 둘을 출력한다).
     *
     * @return channels / 채널 수
     */
    int sourceChannels() {
        return channels;
    }

    /**
     * Decodes packets until one produces output; the samples are then in {@link #blockOutput()}. Both the 16 bit path and the wide path read
     * blocks through this method.
     * <p>
     * 출력을 만드는 패킷이 나올 때까지 디코딩한다. 샘플은 그다음 {@link #blockOutput()} 에 있다. 16비트 경로와 넓은 경로가 모두 이 메서드로 블록을 읽는다.
     *
     * @return the number of frames of the block, or -1 at the end / 블록의 프레임 수. 끝이면 -1
     * @throws MediaException CORRUPT for invalid packets / 패킷이 올바르지 않으면 CORRUPT
     */
    int nextBlock() throws MediaException {
        // 출력이 나오는 패킷을 찾을 때까지 패킷을 디코딩한다.
        for (;;) {
            OggPacket p = nextPacket();
            if (p == null) {
                return -1;
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "audio decoding was interrupted");
            }
            int n = core.decode(p.getData());
            if (n <= 0) {
                continue;
            }
            if (p.isLast() && p.getGranule() >= 0) {
                n = (int) Math.max(0, Math.min(n, p.getGranule() - produced));
            }
            produced += n;
            if (n == 0) {
                continue;
            }
            return n;
        }
    }

    /**
     * Converts the decoder output to 16-bit interleaved samples.
     * <p>
     * 디코더 출력을 16비트 인터리브 샘플로 바꾼다.
     *
     * @param n   number of frames / 프레임 수
     * @param out output buffer / 출력 버퍼
     * @return bytes written / 쓴 바이트 수
     */
    private int convert(int n, byte[] out) {
        int o = 0;
        // 프레임마다 채널을 16비트로 바꿔 쓴다(둘보다 많으면 믹서를 거친다).
        for (int i = 0; i < n; i++) {
            if (mixer == null) {
                // 항목마다 반복해서 처리한다.
                for (int c = 0; c < channels; c++) {
                    short s = PcmConverter.clip16(core.output[c][i]);
                    out[o++] = (byte) s;
                    out[o++] = (byte) (s >> 8);
                }
            } else {
                // 항목마다 반복해서 처리한다.
                for (int c = 0; c < channels; c++) {
                    frameBuf[c] = core.output[c][i];
                }
                mixer.mix(frameBuf, mixed);
                // 항목마다 반복해서 처리한다.
                for (int c = 0; c < 2; c++) {
                    short s = PcmConverter.clip16(mixed[c]);
                    out[o++] = (byte) s;
                    out[o++] = (byte) (s >> 8);
                }
            }
        }
        return o;
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
     * Positions the decoder at a page before the wanted frame.
     * <p>
     * 원하는 프레임 앞의 페이지에 디코더를 둔다.
     *
     * @param frame the wanted frame / 원하는 프레임
     * @return the frame at which decoding resumes / 디코딩을 다시 시작하는 프레임
     * @throws MediaException on a reading failure / 읽기 실패 시
     */
    protected long seekTo(long frame) throws MediaException {
        queue.clear();
        core.reset();
        int k = stream.findPageAfterGranule(frame);
        int start = Math.max(audioStartPage, Math.min(k, stream.getPageCount() - 1) - 1);
        // 이어지는 페이지에서 시작하면 첫 패킷의 앞부분이 없으므로 이어지지 않는 페이지까지 거슬러 올라간다.
        while (start > audioStartPage && stream.isPageContinued(start)) {
            start--;
        }
        if (start <= audioStartPage) {
            return restart();
        }
        OggPacketStream reader = stream.openPackets(start);
        java.util.ArrayList<OggPacket> group = new java.util.ArrayList<OggPacket>();
        long granule = -1;
        // 그래뉼 위치를 아는 패킷이 나올 때까지 패킷을 모은다.
        for (;;) {
            OggPacket p = reader.next();
            if (p == null) {
                break;
            }
            group.add(p);
            if (p.getGranule() >= 0) {
                granule = p.getGranule();
                break;
            }
        }
        long sum = 0;
        int previous = -1;
        boolean valid = granule >= 0 && !group.isEmpty();
        // 패킷마다 블록 크기를 읽어 첫 패킷 뒤에 나오는 프레임 수의 합을 구한다.
        for (int i = 0; valid && i < group.size(); i++) {
            int size = core.blockSizeOf(group.get(i).getData());
            if (size < 0) {
                valid = false;
            } else {
                if (i > 0) {
                    sum += previous / 4 + size / 4;
                }
                previous = size;
            }
        }
        if (!valid || granule - sum < 0) {
            return restart();
        }
        packets = reader;
        // 첫 패킷은 겹침을 준비하는 데 쓰고 나머지는 큐에 넣어 이어서 디코딩하게 한다.
        core.decode(group.get(0).getData());
        for (int i = 1; i < group.size(); i++) {
            queue.add(group.get(i));
        }
        produced = granule - sum;
        return produced;
    }

    /**
     * Restarts decoding at the first audio packet.
     * <p>
     * 첫 오디오 패킷에서 디코딩을 다시 시작한다.
     *
     * @return 0, the frame at which decoding resumes / 디코딩을 다시 시작하는 프레임인 0
     */
    private long restart() {
        queue.clear();
        core.reset();
        packets = stream.openPackets(audioStartPage);
        produced = 0;
        return 0;
    }

    /** Closes the input and releases retained packets. / 입력을 닫고 보관한 패킷을 해제한다. */
    protected void release() {
        queue.clear();
        packets = null;
        data.close();
    }
}
