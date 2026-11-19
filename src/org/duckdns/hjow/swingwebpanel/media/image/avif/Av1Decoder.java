package org.duckdns.hjow.swingwebpanel.media.image.avif;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * The AV1 decoder (plan item P18-03): it reads the OBUs (open bitstream units) of temporal units, decodes the frames with all tiles, runs the
 * post filters (deblocking, CDEF, super resolution upscaling, loop restoration), keeps the reference frames of the sequence and returns the
 * pictures that are shown, in the coded colour space and with the film grain of the frame added ({@link Av1FilmGrain}). A still picture is one
 * temporal unit; an image sequence (animated AVIF) is a series of temporal units that are decoded with the same decoder object.
 * <p>
 * AV1 디코더(계획 P18-03): 시간 단위의 OBU(개방형 비트스트림 단위)를 읽어 프레임을 모든 타일과 함께 디코딩하고 후처리 필터(블록 경계 제거, CDEF,
 * 초해상도 확대, 루프 복원)를 거치며, 시퀀스의 참조 프레임을 보관하고, 표시되는 화면을 부호화된 색 공간에서 프레임의 필름 그레인을 더해
 * ({@link Av1FilmGrain}) 반환한다. 정지 화면은 시간 단위 하나이고, 이미지 시퀀스(애니메이션 AVIF)는 같은 디코더 객체로 디코딩하는 시간 단위의 열이다.
 */
final class Av1Decoder {

    /**
     * The OBU type of a sequence header.
     * <p>
     * 시퀀스 헤더의 OBU 종류.
     */
    private static final int OBU_SEQUENCE_HEADER = 1;

    /**
     * The OBU type of a temporal delimiter.
     * <p>
     * 시간 구분자의 OBU 종류.
     */
    private static final int OBU_TEMPORAL_DELIMITER = 2;

    /**
     * The OBU type of a frame header.
     * <p>
     * 프레임 헤더의 OBU 종류.
     */
    private static final int OBU_FRAME_HEADER = 3;

    /**
     * The OBU type of a tile group.
     * <p>
     * 타일 그룹의 OBU 종류.
     */
    private static final int OBU_TILE_GROUP = 4;

    /**
     * The OBU type of a frame (header and tile group).
     * <p>
     * 프레임(헤더와 타일 그룹)의 OBU 종류.
     */
    private static final int OBU_FRAME = 6;

    /**
     * The OBU type of a redundant frame header.
     * <p>
     * 중복 프레임 헤더의 OBU 종류.
     */
    private static final int OBU_REDUNDANT_FRAME_HEADER = 7;

    /**
     * The limits of this decoding.
     * <p>
     * 이 디코딩의 제한.
     */
    private final DecodeLimits limits;

    /**
     * The reference frame store: eight slots (null for an empty slot).
     * <p>
     * 참조 프레임 저장소: 슬롯 여덟 개(빈 슬롯은 null).
     */
    private final Av1RefFrame[] refs = new Av1RefFrame[8];

    /**
     * The sequence header that was read.
     * <p>
     * 읽은 시퀀스 헤더.
     */
    private Av1Sequence seq;

    /**
     * The frame header of the frame that is being decoded.
     * <p>
     * 디코딩 중인 프레임의 프레임 헤더.
     */
    private Av1FrameHeader hdr;

    /**
     * The frame that is being decoded (null between frames).
     * <p>
     * 디코딩 중인 프레임(프레임 사이에는 null).
     */
    private Av1Frame frame;

    /**
     * The initial distributions of the frame.
     * <p>
     * 프레임의 초기 분포.
     */
    private Av1Cdfs frameCdfs;

    /**
     * The distributions at the end of the tile whose CDFs the following frames start from (null before it was decoded).
     * <p>
     * 이후 프레임이 이어받을 CDF 를 가진 타일이 끝났을 때의 분포(그 타일을 디코딩하기 전에는 null).
     */
    private Av1Cdfs savedCdfs;

    /**
     * The number of tiles that were decoded so far in the frame.
     * <p>
     * 지금까지 디코딩한 프레임의 타일 수.
     */
    private int tilesDecoded;

    /**
     * True after a frame header was read until the frame is finished (further frame headers are copies).
     * <p>
     * 프레임 헤더를 읽은 뒤 프레임이 끝날 때까지 true(그 사이의 프레임 헤더는 복사본이다).
     */
    private boolean seenFrameHeader;

    /**
     * The temporal layer of the OBU that is being read.
     * <p>
     * 읽고 있는 OBU 의 시간 계층.
     */
    private int obuTemporalId;

    /**
     * The spatial layer of the OBU that is being read.
     * <p>
     * 읽고 있는 OBU 의 공간 계층.
     */
    private int obuSpatialId;

    /**
     * The order hints of the references of the last frame (kept for the frames that do not set them).
     * <p>
     * 마지막 프레임의 참조 순서 힌트(순서 힌트를 정하지 않는 프레임을 위해 유지한다).
     */
    private final int[] orderHints = new int[8];

    /**
     * The operating point that is decoded (the index into the operating points of the sequence header).
     * <p>
     * 디코딩하는 동작 지점(시퀀스 헤더의 동작 지점 번호).
     */
    private int operatingPoint;

    /**
     * The spatial layer whose picture is wanted by a still picture decode (-1 for the highest layer of the operating point).
     * <p>
     * 정지 화면 디코딩이 원하는 공간 계층의 화면(-1 은 동작 지점의 가장 높은 계층).
     */
    private int wantedLayer = -1;

    /**
     * True if the decoding stops at the first picture that is shown (a still picture).
     * <p>
     * 처음 표시되는 화면에서 디코딩을 멈추면 true(정지 화면).
     */
    private boolean stopAtFirstShown;

    /**
     * The pictures that are shown, with the spatial layer of the frame, in the order of the display.
     * <p>
     * 표시되는 화면과 프레임의 공간 계층, 표시 순서대로.
     */
    private final List<Av1Picture> outputs = new ArrayList<Av1Picture>();

    /**
     * The spatial layers of the pictures of {@link #outputs}.
     * <p>
     * {@link #outputs} 에 있는 화면의 공간 계층.
     */
    private final List<Integer> outputLayers = new ArrayList<Integer>();

    /**
     * Creates a decoder.
     * <p>
     * 디코더를 만든다.
     *
     * @param limits the limits / 제한
     */
    Av1Decoder(DecodeLimits limits) {
        this.limits = limits;
    }

    /**
     * Decodes the first picture that is shown in a sequence of OBUs (a still picture).
     * <p>
     * OBU 열에서 처음 표시되는 화면을 디코딩한다(정지 화면).
     *
     * @param data      the data / 데이터
     * @param offset    the first byte / 첫 바이트
     * @param length    the number of bytes / 바이트 수
     * @param limits    the limits / 제한
     * @param configObu the OBUs of the codec configuration (may be null) / 코덱 설정의 OBU(null 일 수 있다)
     * @return the picture / 화면
     * @throws MediaException CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE or CANCELLED / CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE 또는 CANCELLED
     */
    static Av1Picture decode(byte[] data, int offset, int length, DecodeLimits limits, byte[] configObu) throws MediaException {
        return decodeLayer(data, offset, length, limits, configObu, 0, -1);
    }

    /**
     * Decodes a still picture of a layered image: the OBUs of the operating point are decoded (the OBUs of other layers are dropped) and the
     * picture of one spatial layer is returned. A non-scalable stream has one layer, and its first picture that is shown is returned.
     * <p>
     * 계층 이미지의 정지 화면을 디코딩한다: 동작 지점의 OBU 를 디코딩하고(다른 계층의 OBU 는 버린다) 공간 계층 하나의 화면을 반환한다. 계층 부호화가
     * 아닌 스트림은 계층이 하나이며 처음 표시되는 화면을 반환한다.
     *
     * @param data           the data / 데이터
     * @param offset         the first byte / 첫 바이트
     * @param length         the number of bytes / 바이트 수
     * @param limits         the limits / 제한
     * @param configObu      the OBUs of the codec configuration (may be null) / 코덱 설정의 OBU(null 일 수 있다)
     * @param operatingPoint the index of the operating point to decode / 디코딩할 동작 지점 번호
     * @param layerId        the spatial_id of the layer to return, -1 for the highest layer that the operating point has and the data contains / 반환할
     *                       계층의 spatial_id. -1 이면 동작 지점에 있고 데이터에 들어 있는 가장 높은 계층
     * @return the picture / 화면
     * @throws MediaException CORRUPT for damaged data, an unknown operating point or a layer that shows no picture, UNSUPPORTED_FORMAT, TOO_LARGE or
     *                        CANCELLED / 손상된 데이터, 없는 동작 지점, 화면이 없는 계층이면 CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE 또는 CANCELLED
     */
    static Av1Picture decodeLayer(byte[] data, int offset, int length, DecodeLimits limits, byte[] configObu, int operatingPoint, int layerId)
            throws MediaException {
        Av1Decoder d = new Av1Decoder(limits);
        d.stopAtFirstShown = true;
        d.operatingPoint = operatingPoint;
        d.wantedLayer = layerId;
        if (configObu != null && configObu.length > 0) {
            d.parseObus(configObu, 0, configObu.length, true);
        }
        d.parseObus(data, offset, length, false);
        if (d.outputs.isEmpty()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AV1: the data contains no complete frame");
        }
        if (layerId < 0) {
            return d.outputs.get(d.outputs.size() - 1);
        }
        // 원하는 공간 계층의 화면을 뒤에서부터 찾는다(같은 계층이 여럿이면 마지막 것).
        for (int i = d.outputs.size() - 1; i >= 0; i--) {
            if (d.outputLayers.get(i).intValue() == layerId) {
                return d.outputs.get(i);
            }
        }
        throw new MediaException(MediaException.Kind.CORRUPT, "AV1: the spatial layer " + layerId + " shows no picture");
    }

    /**
     * Tells if a still picture decode has what it wants: the picture of the wanted layer, or (no layer wanted) the picture of the highest
     * spatial layer of the operating point (the first picture if the operating point has no layers).
     * <p>
     * 정지 화면 디코딩이 원하는 것을 얻었는지 알려 준다: 원하는 계층의 화면, 또는(계층을 지정하지 않으면) 동작 지점의 가장 높은 공간 계층의 화면(동작
     * 지점에 계층이 없으면 첫 화면).
     *
     * @return true to stop reading OBUs / OBU 읽기를 멈추려면 true
     */
    private boolean stillPictureDone() {
        if (!stopAtFirstShown || outputs.isEmpty()) {
            return false;
        }
        int last = outputLayers.get(outputLayers.size() - 1).intValue();
        if (wantedLayer >= 0) {
            return last == wantedLayer;
        }
        int spatialMask = (operatingPointIdc() >> 8) & 15;
        // 계층이 없는 동작 지점은 첫 화면에서, 계층이 있으면 가장 높은 계층의 화면이 나오면 멈춘다.
        return spatialMask == 0 || last >= 31 - Integer.numberOfLeadingZeros(spatialMask);
    }

    /**
     * Decodes all frames of a sequence of OBUs (any number of temporal units) and returns the pictures that are shown, in the order of the
     * display.
     * <p>
     * OBU 열의 모든 프레임(시간 단위 개수 무관)을 디코딩하고 표시되는 화면을 표시 순서대로 반환한다.
     *
     * @param data      the data / 데이터
     * @param offset    the first byte / 첫 바이트
     * @param length    the number of bytes / 바이트 수
     * @param limits    the limits / 제한
     * @param configObu the OBUs of the codec configuration (may be null) / 코덱 설정의 OBU(null 일 수 있다)
     * @return the pictures / 화면
     * @throws MediaException CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE or CANCELLED / CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE 또는 CANCELLED
     */
    static List<Av1Picture> decodeAll(byte[] data, int offset, int length, DecodeLimits limits, byte[] configObu) throws MediaException {
        Av1Decoder d = new Av1Decoder(limits);
        d.configure(configObu);
        d.parseObus(data, offset, length, false);
        return new ArrayList<Av1Picture>(d.outputs);
    }

    /**
     * Reads the sequence header OBUs of the codec configuration of a container.
     * <p>
     * 컨테이너의 코덱 설정에 있는 시퀀스 헤더 OBU 를 읽는다.
     *
     * @param configObu the OBUs / OBU
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    void configure(byte[] configObu) throws MediaException {
        if (configObu != null && configObu.length > 0) {
            parseObus(configObu, 0, configObu.length, true);
        }
    }

    /**
     * Decodes one temporal unit of a sequence and returns the pictures that are shown (usually one).
     * <p>
     * 시퀀스의 시간 단위 하나를 디코딩하고 표시되는 화면(보통 하나)을 반환한다.
     *
     * @param data   the data / 데이터
     * @param offset the first byte / 첫 바이트
     * @param length the number of bytes / 바이트 수
     * @return the pictures that are shown / 표시되는 화면
     * @throws MediaException CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE or CANCELLED / CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE 또는 CANCELLED
     */
    List<Av1Picture> decodeTemporalUnit(byte[] data, int offset, int length) throws MediaException {
        outputs.clear();
        outputLayers.clear();
        seenFrameHeader = false;
        parseObus(data, offset, length, false);
        return new ArrayList<Av1Picture>(outputs);
    }

    /**
     * Reads the OBUs of a byte range.
     * <p>
     * 바이트 범위의 OBU 를 읽는다.
     *
     * @param data      the data / 데이터
     * @param offset    the first byte / 첫 바이트
     * @param length    the number of bytes / 바이트 수
     * @param configure true if the OBUs only configure the sequence (frames are ignored) / OBU 가 시퀀스 설정만 하면 true(프레임은 무시)
     * @throws MediaException CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE or CANCELLED / CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE 또는 CANCELLED
     */
    private void parseObus(byte[] data, int offset, int length, boolean configure) throws MediaException {
        int pos = offset;
        int end = offset + length;
        // 데이터 끝까지 OBU 를 하나씩 읽는다. 정지 화면은 첫 화면이 나오면 멈춘다.
        while (pos < end && !stillPictureDone()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "AV1: decoding was cancelled");
            }
            int h = data[pos++] & 0xFF;
            int type = (h >> 3) & 15;
            boolean ext = (h & 4) != 0;
            boolean hasSize = (h & 2) != 0;
            obuTemporalId = 0;
            obuSpatialId = 0;
            if (ext) {
                if (pos >= end) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "AV1: truncated OBU extension");
                }
                int e = data[pos++] & 0xFF;
                obuTemporalId = e >> 5;
                obuSpatialId = (e >> 3) & 3;
            }
            long size;
            if (hasSize) {
                Av1BitReader r = new Av1BitReader(data, pos, end - pos);
                size = r.leb128();
                pos = (int) (r.position() >> 3);
            } else {
                size = end - pos;
            }
            if (size < 0 || pos + size > end) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AV1: OBU size exceeds the data");
            }
            int payload = pos;
            int payloadEnd = pos + (int) size;
            pos = payloadEnd;
            if (type != OBU_SEQUENCE_HEADER && type != OBU_TEMPORAL_DELIMITER && ext && seq != null && operatingPointIdc() != 0) {
                int idc = operatingPointIdc();
                boolean inTemporal = ((idc >> obuTemporalId) & 1) != 0;
                boolean inSpatial = ((idc >> (obuSpatialId + 8)) & 1) != 0;
                if (!inTemporal || !inSpatial) {
                    continue;
                }
            }
            if (type == OBU_SEQUENCE_HEADER) {
                seq = Av1Sequence.parse(new Av1BitReader(data, payload, payloadEnd - payload));
                if (operatingPoint > seq.operatingPointsCntMinus1) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "AV1: the operating point " + operatingPoint + " does not exist (the sequence has "
                            + (seq.operatingPointsCntMinus1 + 1) + ")");
                }
            } else if (configure) {
                continue;
            } else if (type == OBU_TEMPORAL_DELIMITER) {
                seenFrameHeader = false;
            } else if (type == OBU_FRAME_HEADER || type == OBU_REDUNDANT_FRAME_HEADER) {
                if (!seenFrameHeader) {
                    frameHeader(new Av1BitReader(data, payload, payloadEnd - payload));
                }
            } else if (type == OBU_FRAME) {
                Av1BitReader r = new Av1BitReader(data, payload, payloadEnd - payload);
                frameHeader(r);
                if (frame != null) {
                    r.byteAlign();
                    int tgStart = (int) (r.position() >> 3);
                    tileGroup(data, tgStart, payloadEnd - tgStart);
                }
            } else if (type == OBU_TILE_GROUP) {
                if (frame == null) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "AV1: a tile group appears before a frame header");
                }
                tileGroup(data, payload, payloadEnd - payload);
            }
        }
    }

    /**
     * Returns the operating point idc of the selected operating point (0 if the stream is not scalable).
     * <p>
     * 선택한 동작 지점의 idc 를 반환한다(계층 부호화가 아니면 0).
     *
     * @return the idc / idc
     */
    private int operatingPointIdc() {
        return seq.operatingPointIdc[Math.min(operatingPoint, seq.operatingPointsCntMinus1)];
    }

    /**
     * Parses a frame header and prepares the frame (or shows an existing frame).
     * <p>
     * 프레임 헤더를 해석하고 프레임을 준비한다(또는 기존 프레임을 표시한다).
     *
     * @param r the reader at the start of the frame header / 프레임 헤더 시작의 읽기
     * @throws MediaException CORRUPT, UNSUPPORTED_FORMAT or TOO_LARGE / CORRUPT, UNSUPPORTED_FORMAT 또는 TOO_LARGE
     */
    private void frameHeader(Av1BitReader r) throws MediaException {
        if (seq == null) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AV1: a frame header appears before the sequence header");
        }
        hdr = Av1FrameHeader.parse(r, seq, refs, obuTemporalId, obuSpatialId);
        if (hdr.showExistingFrame) {
            showExistingFrame();
            frame = null;
            seenFrameHeader = false;
            return;
        }
        seenFrameHeader = true;
        limits.checkImageSize(hdr.upscaledWidth, hdr.frameHeight);
        // 순서 힌트는 프레임 사이에 이어진다(인터 프레임이 정하고 표시되는 키 프레임이 0 으로 되돌린다).
        if (hdr.frameType == Av1FrameHeader.KEY_FRAME && hdr.showFrame) {
            java.util.Arrays.fill(orderHints, 0);
        } else if (hdr.frameIsIntra) {
            System.arraycopy(orderHints, 0, hdr.orderHints, 0, 8);
        } else {
            System.arraycopy(hdr.orderHints, 0, orderHints, 0, 8);
        }
        frame = new Av1Frame(seq, hdr);
        frame.allocateRestoration();
        // 인터 프레임은 참조 프레임 일곱 개를 모으고 형식이 같은지, 크기 비율이 허용 범위 안인지 확인한다.
        if (!hdr.frameIsIntra) {
            for (int i = 0; i < 7; i++) {
                Av1RefFrame ref = refs[hdr.refFrameIdx[i]];
                if (ref.bitDepth != seq.bitDepth || ref.subX != seq.subsamplingX || ref.subY != seq.subsamplingY || ref.numPlanes != seq.numPlanes) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "AV1: a reference frame has another format");
                }
                if (2 * hdr.frameWidth < ref.upscaledWidth || 2 * hdr.frameHeight < ref.frameHeight || hdr.frameWidth > 16 * ref.upscaledWidth
                        || hdr.frameHeight > 16 * ref.frameHeight) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "AV1: a reference frame has a size that cannot be scaled");
                }
                frame.refList[1 + i] = ref;
            }
        }
        // 이전 프레임의 분할 맵을 불러온다(같은 크기이고 분할을 쓸 때만, 아니면 0).
        Av1RefFrame primary = hdr.primaryRef;
        if (primary != null && hdr.segmentationEnabled && primary.savedSegmentIds != null && primary.miCols == hdr.miCols
                && primary.miRows == hdr.miRows) {
            frame.prevSegmentIds = new byte[frame.segmentId.length];
            // 저장된 압축 배열을 프레임의 행 간격으로 펼친다.
            for (int row = 0; row < hdr.miRows; row++) {
                System.arraycopy(primary.savedSegmentIds, row * hdr.miCols, frame.prevSegmentIds, row * frame.miStride, hdr.miCols);
            }
        }
        if (hdr.useRefFrameMvs) {
            Av1MotionField.estimate(frame);
        }
        frameCdfs = hdr.primaryRef != null ? hdr.primaryRef.cdfs.copyForLoad() : new Av1Cdfs(hdr.baseQIdx);
        savedCdfs = null;
        tilesDecoded = 0;
    }

    /**
     * Decodes the tiles of a tile group and finishes the frame after the last tile.
     * <p>
     * 타일 그룹의 타일을 디코딩하고 마지막 타일 뒤에 프레임을 마무리한다.
     *
     * @param data   the data / 데이터
     * @param offset the first byte of the tile group / 타일 그룹의 첫 바이트
     * @param size   the size of the tile group / 타일 그룹의 크기
     * @throws MediaException CORRUPT, UNSUPPORTED_FORMAT or CANCELLED / CORRUPT, UNSUPPORTED_FORMAT 또는 CANCELLED
     */
    private void tileGroup(byte[] data, int offset, int size) throws MediaException {
        int numTiles = hdr.tileCols * hdr.tileRows;
        Av1BitReader r = new Av1BitReader(data, offset, size);
        boolean startEnd = false;
        if (numTiles > 1) {
            startEnd = r.flag();
        }
        int tgStart = 0;
        int tgEnd = numTiles - 1;
        if (startEnd) {
            int tileBits = hdr.tileColsLog2 + hdr.tileRowsLog2;
            tgStart = r.bits(tileBits);
            tgEnd = r.bits(tileBits);
        }
        r.byteAlign();
        int pos = (int) (r.position() >> 3);
        int end = offset + size;
        if (tgEnd >= numTiles || tgStart > tgEnd) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AV1: invalid tile group range");
        }
        // 타일 그룹의 타일을 순서대로 디코딩한다. 마지막 타일을 제외하면 앞에 크기가 있다.
        for (int tileNum = tgStart; tileNum <= tgEnd; tileNum++) {
            int tileRow = tileNum / hdr.tileCols;
            int tileCol = tileNum % hdr.tileCols;
            int tileSize;
            if (tileNum == tgEnd) {
                tileSize = end - pos;
            } else {
                if (pos + hdr.tileSizeBytes > end) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "AV1: truncated tile size");
                }
                long v = 0;
                // 타일 크기를 리틀 엔디언 바이트로 조립한다.
                for (int i = 0; i < hdr.tileSizeBytes; i++) {
                    v |= (long) (data[pos + i] & 0xFF) << (8 * i);
                }
                tileSize = (int) (v + 1);
                pos += hdr.tileSizeBytes;
            }
            if (tileSize < 0 || pos + tileSize > end) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AV1: the tile exceeds the tile group");
            }
            Av1Cdfs tileCdfs = frameCdfs.copy();
            Av1TileDecoder td = new Av1TileDecoder(frame, tileCdfs, data, pos, tileSize, hdr.miRowStarts[tileRow], hdr.miRowStarts[tileRow + 1],
                    hdr.miColStarts[tileCol], hdr.miColStarts[tileCol + 1]);
            td.decode();
            // 프레임 끝에서 이어받을 CDF 는 지정된 타일이 끝났을 때의 것이다.
            if (!hdr.disableFrameEndUpdateCdf && tileNum == hdr.contextUpdateTileId) {
                savedCdfs = tileCdfs;
            }
            pos += tileSize;
            tilesDecoded++;
        }
        if (tgEnd == numTiles - 1) {
            finishFrame();
        }
    }

    /**
     * Runs the post filters, updates the reference frames and outputs the picture if the frame is shown.
     * <p>
     * 후처리 필터를 실행하고 참조 프레임을 갱신하며 프레임이 표시되면 화면을 출력한다.
     *
     * @throws MediaException CANCELLED if interrupted / 인터럽트되면 CANCELLED
     */
    private void finishFrame() throws MediaException {
        if (Thread.currentThread().isInterrupted()) {
            throw new MediaException(MediaException.Kind.CANCELLED, "AV1: decoding was cancelled");
        }
        Av1LoopFilter.apply(frame);
        short[][] cdef = Av1Cdef.apply(frame);
        short[][] curr = frame.planes;
        // 초해상도: 블록 경계를 제거한 프레임과 CDEF 뒤의 프레임을 모두 실제 너비로 늘린 다음 루프 복원을 적용한다.
        if (hdr.useSuperres) {
            int[] up = Av1Upscale.strides(frame);
            short[][] upCdef = Av1Upscale.apply(frame, cdef, up);
            short[][] upCurr = cdef == curr ? upCdef : (hdr.usesLr ? Av1Upscale.apply(frame, curr, up) : upCdef);
            // 확대된 평면의 행 간격을 프레임에 반영한다(이후 단계가 같은 간격을 쓴다).
            for (int p = 0; p < frame.numPlanes; p++) {
                frame.stride[p] = up[p];
            }
            cdef = upCdef;
            curr = upCurr;
        }
        short[][] lr = Av1LoopRestoration.apply(frame, curr, cdef);
        Av1Cdfs finalCdfs = !hdr.disableFrameEndUpdateCdf && savedCdfs != null ? savedCdfs : frameCdfs;
        Av1RefFrame ref = null;
        // 이후 프레임이 참조할 수 있도록 상태를 저장한다.
        if (hdr.refreshFrameFlags != 0 || hdr.showFrame) {
            ref = new Av1RefFrame();
            ref.frameId = hdr.currentFrameId;
            ref.upscaledWidth = hdr.upscaledWidth;
            ref.frameWidth = hdr.frameWidth;
            ref.frameHeight = hdr.frameHeight;
            ref.renderWidth = hdr.renderWidth;
            ref.renderHeight = hdr.renderHeight;
            ref.miCols = hdr.miCols;
            ref.miRows = hdr.miRows;
            ref.frameType = hdr.frameType;
            ref.subX = seq.subsamplingX;
            ref.subY = seq.subsamplingY;
            ref.bitDepth = seq.bitDepth;
            ref.numPlanes = seq.numPlanes;
            ref.orderHint = hdr.orderHint;
            ref.showable = hdr.showableFrame;
            System.arraycopy(hdr.orderHints, 0, ref.savedOrderHints, 0, 8);
            ref.planes = lr;
            // 이후 프레임이 투영할 움직임 벡터와 분할 맵을 저장한다.
            Av1MotionField.store(frame, seq, ref);
            ref.miStride = hdr.miCols;
            ref.savedSegmentIds = new byte[hdr.miRows * hdr.miCols];
            byte[] segSource = hdr.segmentationEnabled && !hdr.segmentationUpdateMap ? frame.prevSegmentIds : frame.segmentId;
            int srcStride = hdr.segmentationEnabled && !hdr.segmentationUpdateMap ? frame.miStride : frame.miStride;
            // 분할 맵이 없으면(0) 그대로 두고, 있으면 행마다 압축해 복사한다.
            if (segSource != null && hdr.segmentationEnabled) {
                for (int row = 0; row < hdr.miRows; row++) {
                    System.arraycopy(segSource, row * srcStride, ref.savedSegmentIds, row * hdr.miCols, hdr.miCols);
                }
            }
            ref.stride = frame.stride.clone();
            ref.savedGmParams = copyGm(hdr.gmParams);
            ref.cdfs = finalCdfs;
            ref.filmGrain = hdr.filmGrain;
            System.arraycopy(hdr.loopFilterRefDeltas, 0, ref.lfRefDeltas, 0, 8);
            System.arraycopy(hdr.loopFilterModeDeltas, 0, ref.lfModeDeltas, 0, 2);
            // 분할 기능을 복사해 저장한다.
            for (int i = 0; i < 8; i++) {
                System.arraycopy(hdr.featureEnabled[i], 0, ref.featureEnabled[i], 0, 8);
                System.arraycopy(hdr.featureData[i], 0, ref.featureData[i], 0, 8);
            }
        }
        // 새 프레임을 표시 플래그가 가리키는 모든 슬롯에 넣는다.
        for (int i = 0; i < 8; i++) {
            if (((hdr.refreshFrameFlags >> i) & 1) != 0) {
                refs[i] = ref;
            }
        }
        if (hdr.showFrame) {
            output(ref, hdr.filmGrain, hdr.spatialId);
        }
        frame = null;
        seenFrameHeader = false;
    }

    /**
     * Copies the global motion parameters.
     * <p>
     * 전역 움직임 매개변수를 복사한다.
     *
     * @param gm the parameters / 매개변수
     * @return the copy / 복사본
     */
    private static int[][] copyGm(int[][] gm) {
        int[][] c = new int[gm.length][];
        // 참조 프레임마다 복사한다.
        for (int i = 0; i < gm.length; i++) {
            c[i] = gm[i].clone();
        }
        return c;
    }

    /**
     * Shows a frame of the reference store again (<code>show_existing_frame</code>); a key frame refreshes all slots with it.
     * <p>
     * 참조 저장소의 프레임을 다시 표시한다(<code>show_existing_frame</code>). 키 프레임이면 모든 슬롯을 그 프레임으로 갱신한다.
     *
     * @throws MediaException CORRUPT for damaged data / 손상된 데이터이면 CORRUPT
     */
    private void showExistingFrame() throws MediaException {
        Av1RefFrame ref = refs[hdr.frameToShowMapIdx];
        if (ref == null) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AV1: show_existing_frame of an empty reference slot");
        }
        output(ref, hdr.filmGrain, hdr.spatialId);
        // 키 프레임을 다시 표시하면 모든 참조 슬롯이 그 프레임으로 바뀐다.
        if (hdr.refreshFrameFlags == 0xFF) {
            for (int i = 0; i < 8; i++) {
                refs[i] = ref;
            }
        }
    }

    /**
     * Adds the picture of a frame to the pictures that are shown (the film grain is added to a copy of the planes).
     * <p>
     * 프레임의 화면을 표시되는 화면에 더한다(필름 그레인은 평면의 복사본에 더한다).
     *
     * @param ref       the frame / 프레임
     * @param grain     the film grain parameters (null if none) / 필름 그레인 매개변수(없으면 null)
     * @param spatialId the spatial layer of the frame / 프레임의 공간 계층
     */
    private void output(Av1RefFrame ref, Av1FilmGrain grain, int spatialId) {
        short[][] planes = ref.planes;
        // 그레인을 더하는 화면은 참조 프레임을 건드리지 않도록 복사본을 쓴다.
        if (grain != null) {
            planes = new short[3][];
            // 그레인을 더할 평면을 하나씩 복사한다.
            for (int p = 0; p < ref.numPlanes; p++) {
                planes[p] = ref.planes[p].clone();
            }
        }
        Av1Picture pic = new Av1Picture(ref.upscaledWidth, ref.frameHeight, ref.bitDepth, ref.numPlanes, ref.subX, ref.subY, planes, ref.stride);
        pic.matrixCoefficients = seq.matrixCoefficients;
        pic.colorPrimaries = seq.colorPrimaries;
        pic.transferCharacteristics = seq.transferCharacteristics;
        pic.fullRange = seq.colorRange != 0;
        // 필름 그레인은 출력 화면에만 더한다(참조 프레임은 그레인 없이 남는다).
        if (grain != null) {
            grain.apply(pic);
        }
        outputs.add(pic);
        outputLayers.add(Integer.valueOf(spatialId));
    }
}
