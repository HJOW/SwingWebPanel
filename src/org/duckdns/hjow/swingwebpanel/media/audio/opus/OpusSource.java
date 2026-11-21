package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmConverter;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggPacket;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggPacketStream;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggStream;

/**
 * Source of Ogg Opus audio (RFC 7845): it decodes the audio packets of every logical stream of the file in turn (a chained file
 * plays all its Opus links one after the other), discards the pre-skip at the start of each link, trims the end of the link to the
 * granule position of its last page, applies the output gain of the header, mixes more than two channels down to stereo and
 * delivers 16-bit PCM at 48 kHz (mono only if all links are mono). A packet that cannot be decoded is concealed like a lost packet.
 * Seeking starts decoding at a page about 80 ms before the wanted sample, as RFC 7845 section 4.6 recommends, so that the
 * decoder state has converged when the wanted position is reached, and the base class discards the output before it, so the
 * position is exact.
 * <p>
 * Ogg Opus 오디오의 소스(RFC 7845): 파일의 모든 논리 스트림의 오디오 패킷을 차례로 디코딩하고(체인 파일은 Opus 고리를 모두 이어서 재생),
 * 고리마다 시작의 사전 건너뛰기를 버리고, 고리의 끝을 마지막 페이지의 그래뉼 위치에 맞춰 자르며, 헤더의 출력 이득을 적용하고, 채널이 둘보다 많으면
 * 스테레오로 다운믹스하여 48 kHz 16비트 PCM 으로 전달한다(모든 고리가 모노일 때만 모노). 디코딩할 수 없는 패킷은 손실된 패킷처럼 은닉한다.
 * 탐색은 RFC 7845 4.6 이 권하는 대로 원하는 샘플 약 80 ms 앞의 페이지에서 디코딩을 시작해 원하는 위치에 닿을 때 디코더 상태가 수렴하게 하고,
 * 그 앞의 출력은 기반 클래스가 버리므로 위치는 정확하다.
 */
final class OpusSource extends BlockPcmSource {

    /**
     * Number of samples that a seek decodes before the wanted position so that the decoder state converges (80 ms).
     * <p>
     * 디코더 상태가 수렴하도록 탐색이 원하는 위치보다 먼저 디코딩하는 샘플 수(80 ms).
     */
    private static final int PREROLL = 3840;

    /**
     * Distance of the seek anchors in samples (an anchor is kept only if it is at least this far from the previous one).
     * <p>
     * 탐색 기준점의 간격(샘플). 이전 기준점에서 이만큼 이상 떨어진 것만 보관한다.
     */
    private static final int ANCHOR_SPACING = 24000;

    /**
     * Largest number of consecutive undecodable packets that are concealed before the stream is reported as damaged.
     * <p>
     * 스트림이 손상되었다고 보고하기 전까지 은닉하는, 연속으로 디코딩할 수 없는 패킷의 최대 수.
     */
    private static final int MAX_CONCEALED = 200;

    /**
     * One logical Opus stream of the file with the data that playback and seeking need.
     * <p>
     * 재생과 탐색에 필요한 정보를 가진 파일의 논리 Opus 스트림 하나.
     */
    private static final class Link {

        /**
         * The logical stream.
         * <p>
         * 논리 스트림.
         */
        OggStream stream;

        /**
         * Its identification header.
         * <p>
         * 식별 헤더.
         */
        OpusHead head;

        /**
         * Number of the first page that holds audio packets.
         * <p>
         * 오디오 패킷이 있는 첫 페이지의 번호.
         */
        int audioStartPage;

        /**
         * Number of decoded samples to discard at the start (pre-skip plus the excess of the first page).
         * <p>
         * 시작에서 버릴 디코딩 샘플 수(사전 건너뛰기에 첫 페이지의 초과분을 더한 값).
         */
        long skip;

        /**
         * Number of output frames of the link.
         * <p>
         * 고리의 출력 프레임 수.
         */
        long frames;

        /**
         * Number of the first output frame of the link in the whole file.
         * <p>
         * 파일 전체에서 고리의 첫 출력 프레임 번호.
         */
        long startFrame;

        /**
         * Decoded sample index at which the stream ends according to the granule position of the last page, or -1.
         * <p>
         * 마지막 페이지의 그래뉼 위치로 정해지는, 스트림이 끝나는 디코딩 샘플 번호. 모르면 -1.
         */
        long endLimit;

        /**
         * Pages at which decoding can start for a seek.
         * <p>
         * 탐색 때 디코딩을 시작할 수 있는 페이지.
         */
        int[] anchorPage = new int[16];

        /**
         * Decoded sample index at the start of each anchor.
         * <p>
         * 기준점마다 시작 위치의 디코딩 샘플 번호.
         */
        long[] anchorSample = new long[16];

        /**
         * Number of anchors.
         * <p>
         * 기준점의 수.
         */
        int anchors;

        /**
         * The decoder of this link (created on first use).
         * <p>
         * 이 고리의 디코더(처음 쓸 때 만든다).
         */
        OpusMultistream core;

        /**
         * Mixer of this link for more than two channels, else null.
         * <p>
         * 이 고리의 채널이 둘보다 많을 때의 믹서. 아니면 null.
         */
        ChannelMixer mixer;

        /**
         * Adds a seek anchor.
         * <p>
         * 탐색 기준점을 더한다.
         *
         * @param page   the page at which decoding can start / 디코딩을 시작할 수 있는 페이지
         * @param sample the decoded sample index at the start of the page / 페이지 시작 위치의 디코딩 샘플 번호
         */
        void addAnchor(int page, long sample) {
            if (anchors == anchorPage.length) {
                anchorPage = java.util.Arrays.copyOf(anchorPage, anchors * 2);
                anchorSample = java.util.Arrays.copyOf(anchorSample, anchors * 2);
            }
            anchorPage[anchors] = page;
            anchorSample[anchors] = sample;
            anchors++;
        }
    }

    /**
     * The links in file order.
     * <p>
     * 파일 순서의 고리.
     */
    private final List<Link> links;

    /**
     * Number of output channels (1 or 2).
     * <p>
     * 출력 채널 수(1 또는 2).
     */
    private final int outChannels;

    /**
     * Index of the link that is being played.
     * <p>
     * 재생 중인 고리의 번호.
     */
    private int current;

    /**
     * The packet reader of the current link.
     * <p>
     * 현재 고리의 패킷 읽기 객체.
     */
    private OggPacketStream packets;

    /**
     * Decoded sample index (counted from the start of the link, pre-skip included) of the next packet.
     * <p>
     * 다음 패킷의 디코딩 샘플 번호(고리 처음부터 센 값. 사전 건너뛰기 포함).
     */
    private long produced;

    /**
     * Duration of the last packet in samples; a lost or damaged packet is concealed with this duration.
     * <p>
     * 마지막 패킷의 길이(샘플). 손실되었거나 손상된 패킷은 이 길이로 은닉한다.
     */
    private int lastDuration = 960;

    /**
     * True once a packet of the current link decoded properly.
     * <p>
     * 현재 고리의 패킷이 한 번이라도 제대로 디코딩되면 true.
     */
    private boolean everDecoded;

    /**
     * Number of consecutive packets that had to be concealed.
     * <p>
     * 연속으로 은닉해야 했던 패킷 수.
     */
    private int concealed;

    /**
     * Decoded samples per output channel of the current link.
     * <p>
     * 현재 고리의 출력 채널별 디코딩 샘플.
     */
    private final float[][] chan = new float[8][OpusPacket.MAX_PACKET_SAMPLES];

    /**
     * Scratch for one frame of more than two channels.
     * <p>
     * 채널이 둘보다 많은 프레임 하나의 작업 공간.
     */
    private final float[] frameBuf = new float[8];

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
     * @param links       the links in file order / 파일 순서의 고리
     * @param outChannels number of output channels / 출력 채널 수
     * @param frames      total number of output frames / 전체 출력 프레임 수
     */
    private OpusSource(List<Link> links, int outChannels, long frames) {
        super(OpusFrameDecoder.FS, outChannels, frames, OpusPacket.MAX_PACKET_SAMPLES);
        this.links = links;
        this.outChannels = outChannels;
    }

    /**
     * Opens the Opus links of an Ogg file: reads and checks the headers, indexes the packets (for the length and for seeking) and
     * positions the source at the start.
     * <p>
     * Ogg 파일의 Opus 고리를 연다: 헤더를 읽고 확인하며, 패킷을 색인하고(길이와 탐색을 위해) 소스를 시작 위치에 둔다.
     *
     * @param streams the logical Opus streams of the file in order / 파일의 논리 Opus 스트림(순서대로)
     * @return the source / 소스
     * @throws MediaException CORRUPT or UNSUPPORTED_FORMAT for damaged or unsupported headers; CANCELLED if the thread is interrupted / 헤더가 손상되었거나 지원하지 않으면 CORRUPT 또는 UNSUPPORTED_FORMAT. 스레드가 인터럽트되면 CANCELLED
     */
    static OpusSource open(List<OggStream> streams) throws MediaException {
        List<Link> list = new ArrayList<Link>();
        boolean allMono = true;
        long total = 0;
        // 스트림마다 헤더를 읽고 패킷을 색인한다.
        for (int i = 0; i < streams.size(); i++) {
            OggStream st = streams.get(i);
            Link l = new Link();
            l.stream = st;
            OggPacketStream ps = st.openPackets(0);
            OggPacket first = ps.next();
            OggPacket tags = ps.next();
            if (first == null || tags == null) {
                if (i == 0) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "the Opus stream has fewer than two header packets");
                }
                continue;
            }
            byte[] hd = first.getData();
            l.head = OpusHead.parse(hd, 0, hd.length);
            byte[] tg = tags.getData();
            if (tg.length < 8 || tg[0] != 'O' || tg[1] != 'p' || tg[2] != 'u' || tg[3] != 's' || tg[4] != 'T' || tg[5] != 'a' || tg[6] != 'g' || tg[7] != 's') {
                throw new MediaException(MediaException.Kind.CORRUPT, "the second packet of the Opus stream is not OpusTags");
            }
            l.audioStartPage = tags.getPageIndex() + 1;
            indexLink(l);
            allMono &= l.head.channels == 1;
            l.startFrame = total;
            total += l.frames;
            list.add(l);
        }
        if (list.isEmpty()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "no usable Opus stream in the Ogg file");
        }
        OpusSource s = new OpusSource(list, allMono ? 1 : 2, total);
        s.startLink(0);
        return s;
    }

    /**
     * Duration of a packet in samples, from its table-of-contents byte.
     * <p>
     * 패킷의 길이(샘플). 목차 바이트로 구한다.
     *
     * @param d        the packet / 패킷
     * @param fallback the duration to assume if the packet is empty or invalid / 패킷이 비었거나 올바르지 않을 때 가정할 길이
     * @return the duration / 길이
     */
    static int duration(byte[] d, int fallback) {
        if (d.length == 0) {
            return fallback;
        }
        int toc = d[0] & 0xFF;
        int per = OpusPacket.getSamplesPerFrame(toc);
        int c = toc & 3;
        int count;
        if (c == 0) {
            count = 1;
        } else if (c != 3) {
            count = 2;
        } else if (d.length < 2) {
            return fallback;
        } else {
            count = d[1] & 0x3F;
        }
        int n = per * count;
        return count < 1 || n > OpusPacket.MAX_PACKET_SAMPLES ? fallback : n;
    }

    /**
     * Reads all packets of a link once to learn its length, the trimming at its start and its end, and the anchors for seeking.
     * <p>
     * 고리의 모든 패킷을 한 번 읽어 길이, 시작과 끝의 자르기, 탐색용 기준점을 알아낸다.
     *
     * @param l the link / 고리
     * @throws MediaException CANCELLED if the thread is interrupted / 스레드가 인터럽트되면 CANCELLED
     */
    private static void indexLink(Link l) throws MediaException {
        OggPacketStream ps = l.stream.openPackets(l.audioStartPage);
        long sample = 0;
        int dur = 960;
        int lastEndPage = -1;
        long extraTrim = 0;
        boolean firstGranuleSeen = false;
        // 첫 오디오 패킷은 첫 오디오 페이지에서 시작하므로 처음 위치는 언제나 기준점이다.
        l.addAnchor(l.audioStartPage, 0);
        long lastAnchor = 0;
        int counter = 0;
        // 패킷마다 길이를 더하며 페이지의 첫 패킷 위치를 기준점으로 기록한다.
        for (;;) {
            OggPacket p = ps.next();
            if (p == null) {
                break;
            }
            if ((++counter & 0xFFF) == 0 && Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "Opus indexing was interrupted");
            }
            int e = p.getPageIndex();
            if (e != lastEndPage && !l.stream.isPageContinued(e) && sample - lastAnchor >= ANCHOR_SPACING) {
                l.addAnchor(e, sample);
                lastAnchor = sample;
            }
            lastEndPage = e;
            dur = duration(p.getData(), dur);
            sample += dur;
            if (!firstGranuleSeen && p.getGranule() >= 0) {
                firstGranuleSeen = true;
                // 첫 페이지가 마지막 페이지(스트림 끝 표시)이면 끝을 자르는 것이므로 시작 자르기로 보지 않는다.
                if (!p.isLast() && p.getGranule() < sample) {
                    extraTrim = sample - p.getGranule();
                }
            }
        }
        l.skip = l.head.preSkip + extraTrim;
        long lastGranule = l.stream.getLastGranule();
        long byGranule = lastGranule >= 0 ? lastGranule - l.head.preSkip : -1;
        long bySamples = sample - l.skip;
        l.frames = Math.max(0, byGranule >= 0 ? Math.min(byGranule, bySamples) : bySamples);
        l.endLimit = lastGranule >= 0 ? lastGranule + extraTrim : -1;
    }

    /**
     * Makes the decoder of a link ready and positions the packet reader at the first audio packet.
     * <p>
     * 고리의 디코더를 준비하고 패킷 읽기 객체를 첫 오디오 패킷에 둔다.
     *
     * @param index number of the link / 고리의 번호
     */
    private void startLink(int index) {
        current = index;
        Link l = links.get(index);
        if (l.core == null) {
            l.core = new OpusMultistream(l.head);
            // 매핑 군 1 의 채널 순서는 Vorbis 와 같다(RFC 7845 5.1.1.2절). 다른 군은 순서가 정해져 있지 않아 일반 배치를 쓴다.
            l.mixer = l.head.channels > 2 ? (l.head.family == 1 ? ChannelMixer.forVorbisOrder(l.head.channels) : ChannelMixer.forChannels(l.head.channels)) : null;
        } else {
            l.core.reset();
        }
        packets = l.stream.openPackets(l.audioStartPage);
        produced = 0;
        everDecoded = false;
        concealed = 0;
    }

    /**
     * Returns the next audio packet, moving on to the next link at the end of a link.
     * <p>
     * 다음 오디오 패킷을 반환한다. 고리의 끝에서는 다음 고리로 넘어간다.
     *
     * @return the packet, or null at the end of the file / 패킷. 파일의 끝이면 null
     * @throws MediaException on a reading failure / 읽기 실패 시
     */
    private OggPacket nextPacket() throws MediaException {
        // 고리가 끝나면 다음 고리로 넘어가며 패킷이 나올 때까지 반복한다.
        for (;;) {
            OggPacket p = packets.next();
            if (p != null) {
                return p;
            }
            if (current + 1 >= links.size()) {
                return null;
            }
            startLink(current + 1);
        }
    }

    /**
     * Decodes packets until one produces output and converts it to 16 bits.
     * <p>
     * 출력이 나오는 패킷이 나올 때까지 디코딩하고 16비트로 바꾼다.
     *
     * @param out output buffer / 출력 버퍼
     * @return bytes written or -1 at the end / 쓴 바이트 수. 끝이면 -1
     * @throws MediaException CORRUPT for a stream that cannot be decoded / 디코딩할 수 없는 스트림이면 CORRUPT
     */
    protected int fill(byte[] out) throws MediaException {
        int n = nextBlock();
        return n < 0 ? -1 : convert(blockLink, blockFrom, blockTo, out);
    }

    /**
     * The link of the block that {@link #nextBlock()} produced.
     * <p>
     * {@link #nextBlock()} 이 만든 블록의 고리.
     */
    private Link blockLink;

    /**
     * First frame of the block in {@link #blockChannels()}.
     * <p>
     * {@link #blockChannels()} 안에서 블록의 첫 프레임.
     */
    private int blockFrom;

    /**
     * End of the block in {@link #blockChannels()}.
     * <p>
     * {@link #blockChannels()} 안에서 블록의 끝.
     */
    private int blockTo;

    /**
     * Returns the decoded samples of the block that {@link #nextBlock()} produced: the frames from {@link #blockFrom()} on, one array per channel
     * (the wide path reads them directly).
     * <p>
     * {@link #nextBlock()} 이 만든 블록의 디코딩된 표본을 반환한다: {@link #blockFrom()} 부터의 프레임. 채널마다 배열 하나(넓은 경로가 직접 읽는다).
     *
     * @return the decoded samples / 디코딩된 표본
     */
    float[][] blockChannels() {
        return chan;
    }

    /**
     * Returns the index of the first frame of the block in {@link #blockChannels()}.
     * <p>
     * {@link #blockChannels()} 안에서 블록의 첫 프레임의 인덱스를 반환한다.
     *
     * @return the first frame / 첫 프레임
     */
    int blockFrom() {
        return blockFrom;
    }

    /**
     * Returns the channel count of every link if it is the same in all of them and every link uses the same channel mapping family, else -1 (a
     * chained stream that changes its layout cannot be given with a fixed number of channels).
     * <p>
     * 모든 고리의 채널 수와 채널 매핑 군이 같으면 그 채널 수를, 아니면 -1 을 반환한다(배치가 바뀌는 연결된 스트림은 고정된 채널 수로 줄 수 없다).
     *
     * @return the channels or -1 / 채널 수 또는 -1
     */
    int uniformChannels() {
        int ch = links.get(0).head.channels;
        int family = links.get(0).head.family;
        // 모든 고리가 같은 채널 수와 매핑 군을 가졌는지 확인한다.
        for (Link l : links) {
            if (l.head.channels != ch || l.head.family != family) {
                return -1;
            }
        }
        return ch;
    }

    /**
     * Returns the channel mapping family of the first link.
     * <p>
     * 첫 고리의 채널 매핑 군을 반환한다.
     *
     * @return the family / 매핑 군
     */
    int family() {
        return links.get(0).head.family;
    }

    /**
     * Decodes packets until one produces output; the samples are then in {@link #blockChannels()} from {@link #blockFrom()}. Both the 16 bit path and
     * the wide path read blocks through this method.
     * <p>
     * 출력이 나오는 패킷이 나올 때까지 디코딩한다. 샘플은 그다음 {@link #blockChannels()} 의 {@link #blockFrom()} 부터에 있다. 16비트 경로와 넓은 경로가 모두 이
     * 메서드로 블록을 읽는다.
     *
     * @return the number of frames of the block, or -1 at the end / 블록의 프레임 수. 끝이면 -1
     * @throws MediaException CORRUPT for a stream that cannot be decoded / 디코딩할 수 없는 스트림이면 CORRUPT
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
            Link l = links.get(current);
            byte[] d = p.getData();
            int n;
            if (d.length == 0) {
                n = l.core.decode(null, 0, 0, lastDuration, chan);
            } else {
                n = l.core.decode(d, 0, d.length, lastDuration, chan);
            }
            if (n < 0) {
                // 디코딩할 수 없는 패킷은 앞서 한 번이라도 성공했다면 손실로 보고 은닉한다.
                concealed++;
                if (!everDecoded || concealed > MAX_CONCEALED) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "the Opus packet cannot be decoded");
                }
                int dur = duration(d, lastDuration);
                n = l.core.decode(null, 0, 0, dur, chan);
                if (n < 0) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "the Opus decoder failed to conceal a damaged packet");
                }
            } else {
                everDecoded = true;
                concealed = 0;
                lastDuration = n;
            }
            long start = produced;
            produced += n;
            int from = 0;
            int to = n;
            if (start < l.skip) {
                from = (int) Math.min(n, l.skip - start);
            }
            if (p.isLast() && p.getGranule() >= 0 && l.endLimit >= 0) {
                to = (int) Math.max(from, Math.min(n, l.endLimit - start));
            }
            if (to <= from) {
                continue;
            }
            blockLink = l;
            blockFrom = from;
            blockTo = to;
            return to - from;
        }
    }

    /**
     * Converts decoded samples to 16-bit interleaved output.
     * <p>
     * 디코딩한 샘플을 16비트 인터리브 출력으로 바꾼다.
     *
     * @param l    the link / 고리
     * @param from first sample / 첫 샘플
     * @param to   one past the last sample / 마지막 샘플의 다음
     * @param out  the output buffer / 출력 버퍼
     * @return bytes written / 쓴 바이트 수
     */
    private int convert(Link l, int from, int to, byte[] out) {
        int o = 0;
        int channels = l.head.channels;
        // 프레임마다 출력 채널 수에 맞춰 16비트로 바꿔 쓴다.
        for (int i = from; i < to; i++) {
            float left;
            float right = 0;
            if (channels == 1) {
                left = chan[0][i];
                right = left;
            } else if (channels == 2) {
                left = chan[0][i];
                right = chan[1][i];
            } else {
                // 채널이 많으면 믹서로 스테레오로 섞는다.
                for (int c = 0; c < channels; c++) {
                    frameBuf[c] = chan[c][i];
                }
                l.mixer.mix(frameBuf, mixed);
                left = mixed[0];
                right = mixed[1];
            }
            short s = PcmConverter.clip16(left);
            out[o++] = (byte) s;
            out[o++] = (byte) (s >> 8);
            if (outChannels == 2) {
                s = PcmConverter.clip16(right);
                out[o++] = (byte) s;
                out[o++] = (byte) (s >> 8);
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
     * Positions the decoder at an anchor about 80 ms before the wanted frame.
     * <p>
     * 원하는 프레임 약 80 ms 앞의 기준점에 디코더를 둔다.
     *
     * @param frame the wanted frame / 원하는 프레임
     * @return the frame at which decoding resumes / 디코딩을 다시 시작하는 프레임
     * @throws MediaException never in practice / 실제로는 발생하지 않는다
     */
    protected long seekTo(long frame) throws MediaException {
        int li = 0;
        // 프레임이 속한 고리를 찾는다.
        for (int i = 0; i < links.size(); i++) {
            if (links.get(i).startFrame <= frame) {
                li = i;
            }
        }
        Link l = links.get(li);
        long target = frame - l.startFrame + l.skip - PREROLL;
        int best = 0;
        // 목표보다 앞선 기준점 중 가장 뒤의 것을 고른다.
        for (int i = 0; i < l.anchors; i++) {
            if (l.anchorSample[i] <= target) {
                best = i;
            }
        }
        startLink(li);
        packets = l.stream.openPackets(l.anchorPage[best]);
        produced = l.anchorSample[best];
        return l.startFrame + Math.max(0, produced - l.skip);
    }
}
