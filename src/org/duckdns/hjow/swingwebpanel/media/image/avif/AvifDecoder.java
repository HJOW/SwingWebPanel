package org.duckdns.hjow.swingwebpanel.media.image.avif;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedFrame;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.ImageDecoder;
import org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfile;

/**
 * The AVIF still image decoder (plan item P18-03): it reads the HEIF structure of an AVIF file, decodes the primary image (a single AV1 intra
 * frame or a grid of AV1 frames) and an optional alpha image with the own AV1 decoder of this package, converts the colours to sRGB ARGB (the
 * matrix coefficients to RGB, then the embedded ICC profile or the colour primaries and transfer characteristics to sRGB, see
 * {@link org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfile}) and applies the clean aperture, rotation and mirroring properties. It is registered
 * by name in {@link org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders}.
 * <p>
 * Supported: AV1 key frames and intra-only frames of 8, 10 and 12 bits in 4:0:0, 4:2:0, 4:2:2 and 4:4:4 with all intra tools (palette, filter
 * intra, chroma from luma, all transform types and sizes, tiles, segmentation, delta quantizer and loop filter levels), the deblocking filter,
 * CDEF, loop restoration and quantizer matrices; derived grid images; derived overlay images (<code>iovl</code>: a canvas with a fill colour
 * and layers at offsets, composited with their alpha; overlays may be nested to three levels); auxiliary alpha images; the properties ispe, colr, pixi, av1C, auxC, clap, irot and imir;
 * intra block copy and super resolution; image sequences (animated AVIF: the <code>avis</code> brand with a track of AV1 samples, inter frames with
 * all tools, an optional alpha track, durations and the repeat count of the edit list); layered images (the properties <code>a1op</code> and
 * <code>lsel</code>: the operating point that is decoded and the spatial layer that is rendered).
 * Not supported, with UNSUPPORTED_FORMAT: the matrix coefficients with constant luminance.
 * <p>
 * AVIF 정지 영상 디코더(계획 P18-03): AVIF 파일의 HEIF 구조를 읽고, 이 패키지의 자체 AV1 디코더로 기본 이미지(AV1 인트라 프레임 하나 또는 AV1 프레임
 * 격자)와 선택적 알파 이미지를 디코딩하여 색을 sRGB ARGB 로 바꾸고(행렬 계수로 RGB 를, 이어서 내장 ICC 프로파일이나 색 원색과 전달 특성으로
 * sRGB 를. {@link org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfile} 참조) clean aperture, 회전, 거울 속성을 적용한다.
 * {@link org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders} 에 이름으로 등록된다.
 * <p>
 * 지원: 8/10/12비트의 4:0:0, 4:2:0, 4:2:2, 4:4:4 AV1 키 프레임과 인트라 전용 프레임(팔레트, 필터 인트라, 휘도로부터의 색차, 모든 변환 종류와 크기, 타일,
 * 분할, 델타 양자화와 루프 필터 수준 포함 모든 인트라 도구), 블록 경계 제거 필터, CDEF, 루프 복원, 양자화 행렬, 파생 격자 이미지, 파생 오버레이 이미지
 * (<code>iovl</code>: 채우기 색이 있는 캔버스와 오프셋에 놓인 계층, 알파를 반영해 합성, 세 단계까지 중첩 가능), 보조 알파 이미지, ispe, colr,
 * pixi, av1C, auxC, clap, irot, imir 속성, 인트라 블록 복사와 초해상도, 이미지 시퀀스(애니메이션 AVIF: AV1 샘플 트랙이 있는 <code>avis</code>
 * 브랜드, 모든 도구를 쓰는 인터 프레임, 선택적 알파 트랙, 지속 시간과 편집 목록의 반복 횟수), 계층 이미지(속성 <code>a1op</code>,
 * <code>lsel</code>: 디코딩할 동작 지점과 그릴 공간 계층). 지원하지 않는 것은 UNSUPPORTED_FORMAT 으로 실패한다: 상수 휘도 행렬 계수.
 */
public final class AvifDecoder implements ImageDecoder {

    /**
     * The auxiliary type of the alpha image defined by MPEG.
     * <p>
     * MPEG 가 정의한 알파 이미지의 보조 종류.
     */
    private static final String ALPHA_MPEG = "urn:mpeg:mpegB:cicp:systems:auxiliary:alpha";

    /**
     * The auxiliary type of the alpha image defined by the HEVC specification (also used by AVIF files).
     * <p>
     * HEVC 규격이 정의한 알파 이미지의 보조 종류(AVIF 파일에서도 쓴다).
     */
    private static final String ALPHA_HEVC = "urn:mpeg:hevc:2015:auxid:1";

    /**
     * The deepest nesting of overlays that is rendered: an overlay may have overlays as layers, three levels below the primary item. The limit
     * also ends reference cycles (an overlay that is its own layer).
     * <p>
     * 그리는 오버레이의 최대 중첩 깊이: 오버레이의 계층이 다시 오버레이일 수 있으며 기본 항목 아래로 세 단계까지이다. 이 한도가 참조 순환(자기 자신을 계층으로
     * 가진 오버레이)도 끝낸다.
     */
    static final int MAX_OVERLAY_DEPTH = 3;

    /**
     * Creates the decoder.
     * <p>
     * 디코더를 만든다.
     */
    public AvifDecoder() {
    }

    /**
     * Returns the format name <code>avif</code>.
     * <p>
     * 형식 이름 <code>avif</code> 를 반환한다.
     *
     * @return the name / 이름
     */
    public String getFormatName() {
        return "avif";
    }

    /**
     * Returns the MIME type <code>image/avif</code>.
     * <p>
     * MIME 타입 <code>image/avif</code> 를 반환한다.
     *
     * @return the types / 타입
     */
    public String[] getMimeTypes() {
        return new String[] { "image/avif" };
    }

    /**
     * Checks the file type box for an AVIF brand (<code>avif</code> or <code>avis</code>).
     * <p>
     * 파일 종류 상자에 AVIF 브랜드(<code>avif</code> 또는 <code>avis</code>)가 있는지 검사한다.
     *
     * @param head the first bytes of the data / 데이터의 첫 바이트
     * @return true for an AVIF file / AVIF 파일이면 true
     */
    public boolean accepts(byte[] head) {
        if (head == null || head.length < 16) {
            return false;
        }
        if (head[4] != 'f' || head[5] != 't' || head[6] != 'y' || head[7] != 'p') {
            return false;
        }
        long size = ((head[0] & 0xFFL) << 24) | ((head[1] & 0xFF) << 16) | ((head[2] & 0xFF) << 8) | (head[3] & 0xFF);
        int end = (int) Math.min(size == 0 ? head.length : size, head.length);
        // 주 브랜드(8)와 호환 브랜드(16부터)를 4바이트씩 검사한다.
        for (int p = 8; p + 4 <= end; p += (p == 8 ? 8 : 4)) {
            if (head[p] == 'a' && head[p + 1] == 'v' && head[p + 2] == 'i' && (head[p + 3] == 'f' || head[p + 3] == 's')) {
                return true;
            }
        }
        return false;
    }

    /**
     * Decodes an AVIF file.
     * <p>
     * AVIF 파일을 디코딩한다.
     *
     * @param data   the file / 파일
     * @param limits the limits / 제한
     * @return the image / 이미지
     * @throws MediaException the failure kind: CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE or CANCELLED / 실패 종류: CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE 또는 CANCELLED
     */
    public DecodedImage decode(byte[] data, DecodeLimits limits) throws MediaException {
        if (data == null || data.length > limits.getMaxBytes()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "AVIF: the file is too large");
        }
        AvifContainer c = AvifContainer.parse(data);
        AvifContainer.Track color = c.colorTrack();
        // 'avis' 브랜드의 파일(또는 기본 항목이 없는 파일)은 트랙의 이미지 시퀀스를 애니메이션으로 디코딩한다.
        if (color != null && (c.hasBrand("avis") || c.primaryItem < 0)) {
            try {
                return decodeSequence(c, color, limits);
            } catch (MediaException e) {
                // 지원하지 않는 시퀀스라도 기본 항목(보통 첫 프레임)이 있으면 정지 이미지로 보여 준다.
                if (e.getKind() != MediaException.Kind.UNSUPPORTED_FORMAT || c.primaryItem < 0) {
                    throw e;
                }
            }
        }
        if (c.primaryItem < 0) {
            if (c.hasMovie) {
                throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "AVIF: the movie box has no track with AV1 images");
            }
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the file has no primary item");
        }
        AvifContainer.Item primary = c.items.get(Integer.valueOf(c.primaryItem));
        if (primary == null) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the primary item is missing");
        }
        int[] rendered = renderItem(c, primary, limits, 0);
        return new DecodedImage(rendered[0], rendered[1], java.util.Arrays.copyOfRange(rendered, 2, rendered.length));
    }

    /**
     * The frame delay (milliseconds) that replaces a frame duration of 0 in an image sequence.
     * <p>
     * 이미지 시퀀스에서 지속 시간 0 인 프레임을 대신하는 프레임 지연(밀리초).
     */
    private static final int ZERO_DURATION_DELAY = 100;

    /**
     * Decodes an image sequence (animated AVIF): the samples of the colour track are decoded one after another with one AV1 decoder (so that
     * the inter frames find their reference frames), each sample is a temporal unit; the optional alpha track is decoded in step with it. The
     * pictures are converted to sRGB like a still image; every frame shows for the duration of its sample. The edit list decides the number of
     * plays: forever with the repeat flag, otherwise the duration of the edit divided by the duration of the samples (at least one play).
     * <p>
     * 이미지 시퀀스(애니메이션 AVIF)를 디코딩한다: 색 트랙의 샘플을 하나의 AV1 디코더로 차례로 디코딩하며(인터 프레임이 참조 프레임을 찾도록)
     * 샘플 하나가 시간 단위 하나이다. 알파 트랙이 있으면 보조를 맞추어 디코딩한다. 화면은 정지 이미지처럼 sRGB 로 바꾸고 프레임마다 샘플의
     * 지속 시간 동안 보여 준다. 재생 횟수는 편집 목록이 정한다: 반복 플래그가 있으면 무한, 아니면 편집의 지속 시간을 샘플의 지속 시간으로 나눈 값
     * (최소 한 번).
     *
     * @param c      the container / 컨테이너
     * @param color  the colour track / 색 트랙
     * @param limits the limits / 제한
     * @return the animated image / 애니메이션 이미지
     * @throws MediaException CORRUPT for damaged data, UNSUPPORTED_FORMAT for frames of different sizes, TOO_LARGE or CANCELLED / 손상된 데이터이면 CORRUPT, 크기가
     *                        다른 프레임은 UNSUPPORTED_FORMAT, TOO_LARGE 또는 CANCELLED
     */
    private static DecodedImage decodeSequence(AvifContainer c, AvifContainer.Track color, DecodeLimits limits) throws MediaException {
        AvifContainer.Track alphaTrack = c.alphaTrack(color);
        int n = color.sampleCount;
        if (n > limits.getMaxFrames()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "AVIF: the image sequence has too many frames");
        }
        if (alphaTrack != null && alphaTrack.sampleCount != n) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the alpha sequence has another number of frames than the colour sequence");
        }
        if (color.timescale <= 0 || (alphaTrack != null && alphaTrack.timescale <= 0)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the time scale of a track is missing");
        }
        boolean premultiplied = alphaTrack != null && color.premRef == alphaTrack.trackId;
        Av1Decoder colorDecoder = sequenceDecoder(c, color, limits);
        Av1Decoder alphaDecoder = alphaTrack == null ? null : sequenceDecoder(c, alphaTrack, limits);
        List<DecodedFrame> frames = new ArrayList<DecodedFrame>(n);
        int width = 0;
        int height = 0;
        long sumDuration = 0;
        ColorProfile profile = null;
        Av1Picture profilePic = null;
        // 샘플(시간 단위)마다 색과 알파를 디코딩하여 프레임으로 만든다.
        for (int i = 0; i < n; i++) {
            Av1Picture pic = sequencePicture(c, colorDecoder, color, i);
            Av1Picture alpha = alphaDecoder == null ? null : sequencePicture(c, alphaDecoder, alphaTrack, i);
            applyColr(c, color.entry, pic);
            if (i == 0) {
                width = pic.width;
                height = pic.height;
                limits.checkImageSize(width, height);
            } else if (pic.width != width || pic.height != height) {
                throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "AVIF: the frames of the image sequence have different sizes");
            }
            if ((i + 1L) * width * height > limits.getMaxTotalPixels()) {
                throw new MediaException(MediaException.Kind.TOO_LARGE, "AVIF: the image sequence is too large");
            }
            int[] argb = AvifColor.toArgb(pic, alpha, premultiplied);
            // 색 변환 정보가 앞 프레임과 같으면 같은 변환을 다시 쓴다(ICC 프로파일 해석을 프레임마다 되풀이하지 않는다).
            if (profile == null || !sameColor(profilePic, pic)) {
                profile = colorProfile(pic);
                profilePic = pic;
            }
            profile.convert(argb);
            long duration = color.sampleDurations[i];
            sumDuration += duration;
            long ms = (duration * 1000 + color.timescale / 2) / color.timescale;
            int delay = (int) Math.min(ms <= 0 ? ZERO_DURATION_DELAY : ms, Integer.MAX_VALUE);
            frames.add(new DecodedFrame(argb, n == 1 ? 0 : delay));
        }
        return new DecodedImage(width, height, frames, loopCount(c, color, sumDuration));
    }

    /**
     * Tells if two pictures need the same conversion to sRGB.
     * <p>
     * 두 화면이 sRGB 로 같은 변환을 필요로 하는지 알려 준다.
     *
     * @param a the first picture / 첫 화면
     * @param b the second picture / 둘째 화면
     * @return true if the colour description is the same / 색 설명이 같으면 true
     */
    private static boolean sameColor(Av1Picture a, Av1Picture b) {
        return a.colorPrimaries == b.colorPrimaries && a.transferCharacteristics == b.transferCharacteristics
                && (a.numPlanes == 1) == (b.numPlanes == 1) && java.util.Arrays.equals(a.iccProfile, b.iccProfile);
    }

    /**
     * Computes the number of plays of an image sequence from the edit list of its track.
     * <p>
     * 이미지 시퀀스의 재생 횟수를 트랙의 편집 목록에서 구한다.
     *
     * @param c           the container / 컨테이너
     * @param t           the colour track / 색 트랙
     * @param sumDuration the sum of the sample durations in time units of the track / 샘플 지속 시간의 합(트랙의 시간 단위)
     * @return 0 to repeat forever, else the number of plays (at least 1) / 무한 반복이면 0, 아니면 재생 횟수(최소 1)
     */
    private static int loopCount(AvifContainer c, AvifContainer.Track t, long sumDuration) {
        if (t.repeat) {
            return 0;
        }
        // 편집 목록의 지속 시간(무비 시간 단위)이 샘플 전체보다 길면 그 비율만큼 반복한다.
        if (c.movieTimescale > 0 && t.editDuration > 0 && sumDuration > 0) {
            double editSeconds = (double) t.editDuration / c.movieTimescale;
            double mediaSeconds = (double) sumDuration / t.timescale;
            long plays = Math.round(editSeconds / mediaSeconds);
            return (int) Math.max(1, Math.min(plays, Integer.MAX_VALUE));
        }
        return 1;
    }

    /**
     * Creates the AV1 decoder of a track and gives it the sequence header of the sample entry.
     * <p>
     * 트랙의 AV1 디코더를 만들고 샘플 항목의 시퀀스 헤더를 준다.
     *
     * @param c      the container / 컨테이너
     * @param t      the track / 트랙
     * @param limits the limits / 제한
     * @return the decoder / 디코더
     * @throws MediaException CORRUPT for a damaged configuration / 설정이 손상되었으면 CORRUPT
     */
    private static Av1Decoder sequenceDecoder(AvifContainer c, AvifContainer.Track t, DecodeLimits limits) throws MediaException {
        Av1Decoder d = new Av1Decoder(limits);
        AvifContainer.Property cfg = c.property(t.entry, "av1C");
        // av1C 의 뒤쪽(4바이트 다음)에는 시퀀스 헤더 OBU 가 들어 있을 수 있다.
        if (cfg != null && cfg.length > 4) {
            byte[] configObu = new byte[cfg.length - 4];
            System.arraycopy(c.data, cfg.offset + 4, configObu, 0, configObu.length);
            d.configure(configObu);
        }
        return d;
    }

    /**
     * Decodes one sample of a track and returns the picture that it shows (with spatial layers, the last one: the highest layer).
     * <p>
     * 트랙의 샘플 하나를 디코딩하고 그 샘플이 표시하는 화면을 반환한다(공간 계층이 있으면 마지막 것: 가장 높은 계층).
     *
     * @param c     the container / 컨테이너
     * @param d     the decoder of the track / 트랙의 디코더
     * @param t     the track / 트랙
     * @param index the index of the sample / 샘플 번호
     * @return the picture / 화면
     * @throws MediaException CORRUPT if the sample shows no picture / 샘플이 화면을 표시하지 않으면 CORRUPT
     */
    private static Av1Picture sequencePicture(AvifContainer c, Av1Decoder d, AvifContainer.Track t, int index) throws MediaException {
        List<Av1Picture> shown = d.decodeTemporalUnit(c.data, (int) t.sampleOffsets[index], t.sampleSizes[index]);
        if (shown.isEmpty()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: sample " + index + " of the image sequence shows no frame");
        }
        return shown.get(shown.size() - 1);
    }

    /**
     * Renders an image item to 8-bit ARGB pixels: an AV1 image or a grid with its alpha image, colour conversion and the transformative
     * properties (clean aperture, rotation, mirroring), or an overlay of such images. The result starts with the width and the height.
     * <p>
     * 이미지 항목을 8비트 ARGB 픽셀로 만든다: 알파 이미지, 색 변환, 변환 속성(깨끗한 개구, 회전, 뒤집기)이 있는 AV1 이미지나 격자, 또는 그런 이미지들의
     * 오버레이. 결과의 첫 두 값은 너비와 높이이다.
     *
     * @param c      the container / 컨테이너
     * @param primary the item / 항목
     * @param limits the limits / 제한
     * @param depth  the nesting depth of overlays (0 for the primary item) / 오버레이의 중첩 깊이(기본 항목은 0)
     * @return the width, the height and the pixels / 너비, 높이, 픽셀
     * @throws MediaException CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE or CANCELLED / CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE 또는 CANCELLED
     */
    private static int[] renderItem(AvifContainer c, AvifContainer.Item primary, DecodeLimits limits, int depth) throws MediaException {
        if (primary.type.equals("iovl")) {
            return renderOverlay(c, primary, limits, depth);
        }
        Av1Picture pic = decodeItem(c, primary, limits, 0);
        Av1Picture alpha = null;
        boolean premultiplied = false;
        int[] auxItems = c.referencing("auxl", primary.id);
        int[] premItems = c.referencedBy("prem", primary.id);
        // 기본 이미지를 참조하는 보조 항목 중 알파 종류인 첫 항목을 디코딩한다.
        for (int i = 0; i < auxItems.length && alpha == null; i++) {
            AvifContainer.Item ai = c.items.get(Integer.valueOf(auxItems[i]));
            if (ai != null && isAlpha(c, ai)) {
                alpha = decodeItem(c, ai, limits, 0);
                // 알파 항목이 미리 곱한 알파로 표시되었는지 확인한다.
                for (int k = 0; k < premItems.length; k++) {
                    if (premItems[k] == ai.id) {
                        premultiplied = true;
                    }
                }
            }
        }
        int[] argb = AvifColor.toArgb(pic, alpha, premultiplied);
        // 색 프로파일이나 코드 값이 sRGB 가 아니면 알파를 미리 곱하지 않은 픽셀을 sRGB 로 바꾼다(회전과 오려내기 전에 한 번).
        colorProfile(pic).convert(argb);
        return transformed(c, primary, argb, new int[] { pic.width, pic.height }, limits);
    }

    /**
     * Applies the transformative properties of an item (clean aperture, rotation, mirroring) in the order of the file.
     * <p>
     * 항목의 변환 속성(깨끗한 개구, 회전, 뒤집기)을 파일에 쓰인 순서대로 적용한다.
     *
     * @param c      the container / 컨테이너
     * @param item   the item whose properties apply / 속성을 적용할 항목
     * @param argb   the pixels / 픽셀
     * @param size   the width and the height / 너비와 높이
     * @param limits the limits / 제한
     * @return the width, the height and the pixels of the result / 결과의 너비, 높이, 픽셀
     * @throws MediaException CORRUPT for an invalid property, TOO_LARGE / 속성이 잘못되면 CORRUPT, TOO_LARGE
     */
    private static int[] transformed(AvifContainer c, AvifContainer.Item item, int[] argb, int[] size, DecodeLimits limits) throws MediaException {
        int[] result = argb;
        // 항목에 연결된 변환 속성을 파일에 쓰인 순서대로 적용한다.
        for (int[] a : item.properties) {
            if (a[0] < 1 || a[0] > c.properties.size()) {
                continue;
            }
            AvifContainer.Property pr = c.properties.get(a[0] - 1);
            if (pr.type.equals("clap")) {
                result = applyClap(c, pr, result, size);
            } else if (pr.type.equals("irot")) {
                result = rotate(result, size, c.propU8(pr, 0) & 3);
            } else if (pr.type.equals("imir")) {
                result = mirror(result, size, c.propU8(pr, 0) & 1);
            }
        }
        limits.checkImageSize(size[0], size[1]);
        int[] out = new int[2 + result.length];
        out[0] = size[0];
        out[1] = size[1];
        System.arraycopy(result, 0, out, 2, result.length);
        return out;
    }

    /**
     * Renders an overlay item (<code>iovl</code>, ISO/IEC 23008-12 6.6.2.3): a canvas of the output size that is filled with a colour and
     * receives the referenced images in the order of the references, each at its own offset (negative offsets and parts outside of the canvas
     * are cut off). The layers are composited with their alpha over the canvas (straight alpha, source-over). A layer may be an overlay itself, down to {@link #MAX_OVERLAY_DEPTH} levels (deeper chains and reference cycles are refused).
     * <p>
     * 오버레이 항목(<code>iovl</code>, ISO/IEC 23008-12 6.6.2.3)을 만든다: 출력 크기의 캔버스를 한 색으로 채우고 참조한 이미지를 참조 순서대로 각자의
     * 오프셋에 놓는다(음수 오프셋과 캔버스 밖 부분은 잘려 나간다). 계층은 알파를 반영해 캔버스 위에 합성한다(직선 알파, source-over). 계층이 다시 오버레이일 수 있으며 {@link #MAX_OVERLAY_DEPTH} 단계까지 허용한다(더 깊은 사슬과 참조 순환은 거부한다).
     *
     * @param c      the container / 컨테이너
     * @param it     the overlay item / 오버레이 항목
     * @param limits the limits / 제한
     * @param depth  the nesting depth / 중첩 깊이
     * @return the width, the height and the pixels / 너비, 높이, 픽셀
     * @throws MediaException CORRUPT for a damaged description, UNSUPPORTED_FORMAT for a chain of overlays that is too deep or a cycle, TOO_LARGE / 설명이 손상되면 CORRUPT, 너무 깊은 오버레이 사슬이나 순환은
     *                        UNSUPPORTED_FORMAT, TOO_LARGE
     */
    private static int[] renderOverlay(AvifContainer c, AvifContainer.Item it, DecodeLimits limits, int depth) throws MediaException {
        if (depth > MAX_OVERLAY_DEPTH) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "AVIF: overlays are nested too deeply (or reference themselves)");
        }
        checkEssential(c, it);
        byte[] d = c.itemData(it, 1 << 16);
        int[] ids = c.referencedBy("dimg", it.id);
        if (d.length < 2 || d[0] != 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: invalid overlay description");
        }
        int field = (d[1] & 1) != 0 ? 4 : 2;
        int need = 2 + 8 + 2 * field + ids.length * 2 * field;
        if (ids.length == 0 || ids.length > limits.getMaxFrames() || d.length != need) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the overlay description does not match its " + ids.length + " references");
        }
        int fill = 0;
        // 캔버스를 채우는 색은 16비트 R, G, B, A 이며 8비트로 줄여 ARGB 로 만든다.
        for (int i = 0; i < 4; i++) {
            int v = ((d[2 + i * 2] & 0xFF) << 8) | (d[3 + i * 2] & 0xFF);
            int ch = v >> 8;
            fill |= ch << (i == 3 ? 24 : 16 - i * 8);
        }
        long outW = readUnsigned(d, 10, field);
        long outH = readUnsigned(d, 10 + field, field);
        limits.checkImageSize(outW, outH);
        int w = (int) outW;
        int h = (int) outH;
        int[] canvas = new int[w * h];
        java.util.Arrays.fill(canvas, fill);
        // 참조한 이미지를 순서대로(먼저 나온 것이 아래) 오프셋에 합성한다.
        for (int i = 0; i < ids.length; i++) {
            AvifContainer.Item layer = c.items.get(Integer.valueOf(ids[i]));
            if (layer == null) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: an overlay layer is missing");
            }
            int[] img = renderItem(c, layer, limits, depth + 1);
            int offX = readSigned(d, 10 + 2 * field + i * 2 * field, field);
            int offY = readSigned(d, 10 + 2 * field + i * 2 * field + field, field);
            compose(canvas, w, h, img, offX, offY);
        }
        return transformed(c, it, canvas, new int[] { w, h }, limits);
    }

    /**
     * Reads a big-endian unsigned number.
     * <p>
     * 빅 엔디언 부호 없는 수를 읽는다.
     *
     * @param d      the data / 데이터
     * @param offset the first byte / 첫 바이트
     * @param bytes  the number of bytes (2 or 4) / 바이트 수(2 또는 4)
     * @return the value / 값
     */
    private static long readUnsigned(byte[] d, int offset, int bytes) {
        long v = 0;
        // 바이트를 위에서부터 이어 붙인다.
        for (int i = 0; i < bytes; i++) {
            v = (v << 8) | (d[offset + i] & 0xFF);
        }
        return v;
    }

    /**
     * Reads a big-endian signed number, clamped to the range of an int.
     * <p>
     * 빅 엔디언 부호 있는 수를 읽는다. int 범위로 제한한다.
     *
     * @param d      the data / 데이터
     * @param offset the first byte / 첫 바이트
     * @param bytes  the number of bytes (2 or 4) / 바이트 수(2 또는 4)
     * @return the value / 값
     */
    private static int readSigned(byte[] d, int offset, int bytes) {
        long v = readUnsigned(d, offset, bytes);
        long sign = 1L << (bytes * 8 - 1);
        return (int) (v >= sign ? v - (sign << 1) : v);
    }

    /**
     * Composites an image over the canvas at an offset with straight alpha (source-over); the parts outside of the canvas are cut off.
     * <p>
     * 이미지를 오프셋에서 캔버스 위에 직선 알파(source-over)로 합성한다. 캔버스 밖의 부분은 잘려 나간다.
     *
     * @param canvas the canvas pixels / 캔버스 픽셀
     * @param cw     the canvas width / 캔버스 너비
     * @param ch     the canvas height / 캔버스 높이
     * @param img    the width, the height and the pixels of the layer / 계층의 너비, 높이, 픽셀
     * @param offX   the horizontal offset of the layer / 계층의 가로 오프셋
     * @param offY   the vertical offset of the layer / 계층의 세로 오프셋
     */
    private static void compose(int[] canvas, int cw, int ch, int[] img, int offX, int offY) {
        int iw = img[0];
        int ih = img[1];
        // 계층의 행 중 캔버스 안에 들어오는 것만 합성한다.
        for (int y = 0; y < ih; y++) {
            long cy = (long) offY + y;
            if (cy < 0 || cy >= ch) {
                continue;
            }
            // 행의 픽셀 중 캔버스 안에 들어오는 것만 합성한다.
            for (int x = 0; x < iw; x++) {
                long cx = (long) offX + x;
                if (cx < 0 || cx >= cw) {
                    continue;
                }
                int src = img[2 + y * iw + x];
                int idx = (int) (cy * cw + cx);
                int sa = src >>> 24;
                // 불투명하면 덮어쓰고, 완전히 투명하면 그대로 두며, 반투명이면 직선 알파로 섞는다.
                if (sa == 255) {
                    canvas[idx] = src;
                } else if (sa != 0) {
                    canvas[idx] = blend(src, canvas[idx], sa);
                }
            }
        }
    }

    /**
     * Blends a translucent source pixel over a destination pixel (straight alpha, source-over).
     * <p>
     * 반투명한 원본 픽셀을 대상 픽셀 위에 섞는다(직선 알파, source-over).
     *
     * @param src the source pixel / 원본 픽셀
     * @param dst the destination pixel / 대상 픽셀
     * @param sa  the alpha of the source, 1 to 254 / 원본의 알파(1에서 254)
     * @return the result / 결과
     */
    private static int blend(int src, int dst, int sa) {
        int da = dst >>> 24;
        // 결과 알파: sa + da * (255 - sa) / 255
        int outA = sa + (da * (255 - sa) + 127) / 255;
        if (outA == 0) {
            return 0;
        }
        int result = outA << 24;
        // 색 채널마다 알파 가중 평균을 구한다.
        for (int shift = 16; shift >= 0; shift -= 8) {
            int s = (src >>> shift) & 255;
            int dd = (dst >>> shift) & 255;
            int num = s * sa * 255 + dd * da * (255 - sa);
            int v = (int) Math.min(255, (num + (long) outA * 255 / 2) / ((long) outA * 255));
            result |= v << shift;
        }
        return result;
    }

    /**
     * Tells if an item is an alpha plane (an auxiliary image with the alpha auxiliary type).
     * <p>
     * 항목이 알파 평면(알파 보조 종류의 보조 이미지)인지 알려 준다.
     *
     * @param c  the container / 컨테이너
     * @param it the item / 항목
     * @return true if it is an alpha image / 알파 이미지이면 true
     * @throws MediaException CORRUPT for a damaged property / 속성이 손상되었으면 CORRUPT
     */
    private static boolean isAlpha(AvifContainer c, AvifContainer.Item it) throws MediaException {
        AvifContainer.Property pr = c.property(it, "auxC");
        if (pr == null || pr.length < 5) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        // 전체 상자의 4바이트 다음부터 널 문자 전까지가 보조 종류 문자열이다.
        for (int i = 4; i < pr.length; i++) {
            int b = c.propU8(pr, i);
            if (b == 0) {
                break;
            }
            sb.append((char) b);
        }
        String t = sb.toString();
        return t.equals(ALPHA_MPEG) || t.equals(ALPHA_HEVC) || t.startsWith("urn:mpeg:mpegB:cicp:systems:auxiliary:alpha");
    }

    /**
     * Checks that the essential properties of an item are understood.
     * <p>
     * 항목의 필수 속성을 이해하는지 확인한다.
     *
     * @param c  the container / 컨테이너
     * @param it the item / 항목
     * @throws MediaException UNSUPPORTED_FORMAT for an essential property that is not supported / 지원하지 않는 필수 속성이면 UNSUPPORTED_FORMAT
     */
    private static void checkEssential(AvifContainer c, AvifContainer.Item it) throws MediaException {
        // 필수로 표시된 속성이 아는 종류인지 확인한다.
        for (int[] a : it.properties) {
            if (a[1] == 0 || a[0] < 1 || a[0] > c.properties.size()) {
                continue;
            }
            AvifContainer.Property pr = c.properties.get(a[0] - 1);
            String t = pr.type;
            boolean known = t.equals("ispe") || t.equals("pixi") || t.equals("colr") || t.equals("av1C") || t.equals("auxC")
                    || t.equals("clap") || t.equals("irot") || t.equals("imir") || t.equals("pasp") || t.equals("clli")
                    || t.equals("mdcv") || t.equals("cclv") || t.equals("a1lx");
            // 계층 이미지의 동작 지점과 계층 선택 속성도 이해한다(디코더가 동작 지점을 고르고 계층의 화면을 고른다).
            if (t.equals("a1op") || t.equals("lsel")) {
                known = true;
            }
            if (!known) {
                throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "AVIF: unsupported essential property " + t);
            }
        }
    }

    /**
     * Decodes an item that holds an AV1 image or a grid of AV1 images.
     * <p>
     * AV1 이미지 또는 AV1 이미지 격자를 담은 항목을 디코딩한다.
     *
     * @param c      the container / 컨테이너
     * @param it     the item / 항목
     * @param limits the limits / 제한
     * @param depth  the nesting depth (0 for the top level) / 중첩 깊이(최상위는 0)
     * @return the picture / 화면
     * @throws MediaException CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE or CANCELLED / CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE 또는 CANCELLED
     */
    private static Av1Picture decodeItem(AvifContainer c, AvifContainer.Item it, DecodeLimits limits, int depth) throws MediaException {
        checkEssential(c, it);
        if (it.type.equals("av01")) {
            byte[] payload = c.itemData(it, limits.getMaxBytes());
            AvifContainer.Property cfg = c.property(it, "av1C");
            byte[] configObu = null;
            // av1C 의 뒤쪽(4바이트 다음)에는 시퀀스 헤더 OBU 가 들어 있을 수 있다.
            if (cfg != null && cfg.length > 4) {
                configObu = new byte[cfg.length - 4];
                System.arraycopy(c.data, cfg.offset + 4, configObu, 0, configObu.length);
            }
            int operatingPoint = 0;
            int layerId = -1;
            AvifContainer.Property a1op = c.property(it, "a1op");
            AvifContainer.Property lsel = c.property(it, "lsel");
            // 동작 지점 선택 속성(a1op)이 있으면 그 번호를, 계층 선택 속성(lsel)이 0xFFFF 가 아니면 그 공간 계층을 쓴다.
            if (a1op != null) {
                operatingPoint = c.propU8(a1op, 0);
            }
            if (lsel != null) {
                int id = c.propU16(lsel, 0);
                if (id != 0xFFFF && id > 3) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the layer selector " + id + " is not a spatial layer");
                }
                layerId = id == 0xFFFF ? -1 : id;
            }
            Av1Picture pic = Av1Decoder.decodeLayer(payload, 0, payload.length, limits, configObu, operatingPoint, layerId);
            applyColr(c, it, pic);
            AvifContainer.Property ispe = c.property(it, "ispe");
            if (ispe != null) {
                int iw = (int) c.propU32(ispe, 4);
                int ih = (int) c.propU32(ispe, 8);
                if (iw > pic.width || ih > pic.height || iw <= 0 || ih <= 0) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the image size of the container does not match the AV1 frame");
                }
                if (iw < pic.width || ih < pic.height) {
                    Av1Picture cropped = new Av1Picture(iw, ih, pic.bitDepth, pic.numPlanes, pic.subX, pic.subY, pic.planes, pic.stride);
                    copyColor(pic, cropped);
                    pic = cropped;
                }
            }
            return pic;
        }
        if (it.type.equals("grid")) {
            if (depth > 0) {
                throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "AVIF: nested grids are not supported");
            }
            return decodeGrid(c, it, limits);
        }
        throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "AVIF: unsupported item type '" + it.type + "'");
    }

    /**
     * Applies the colour description of a <code>colr</code> property of the type <code>nclx</code> (colour primaries, transfer characteristics,
     * matrix coefficients and range) to a picture; it takes precedence over the values of the AV1 sequence header, as in libavif. A profile
     * property (<code>rICC</code> or <code>prof</code>) is kept in the picture for the conversion to sRGB ({@link #colorProfile(Av1Picture)}).
     * <p>
     * <code>nclx</code> 형식의 <code>colr</code> 속성의 색 설명(색 원색, 전달 특성, 행렬 계수, 범위)을 화면에 적용한다. libavif 처럼 AV1 시퀀스
     * 헤더의 값보다 우선한다. 프로파일 속성(<code>rICC</code>, <code>prof</code>)은 sRGB 로의 변환({@link #colorProfile(Av1Picture)})을 위해 화면에 보관한다.
     *
     * @param c   the container / 컨테이너
     * @param it  the item / 항목
     * @param pic the picture to update / 갱신할 화면
     * @throws MediaException CORRUPT for a truncated property / 속성이 잘렸으면 CORRUPT
     */
    private static void applyColr(AvifContainer c, AvifContainer.Item it, Av1Picture pic) throws MediaException {
        boolean sawNclx = false;
        // 항목에 연결된 모든 colr 속성을 본다(nclx 와 ICC 가 함께 있을 수 있으며 같은 종류는 처음 것만 쓴다).
        for (int[] a : it.properties) {
            if (a[0] < 1 || a[0] > c.properties.size()) {
                continue;
            }
            AvifContainer.Property colr = c.properties.get(a[0] - 1);
            if (!colr.type.equals("colr") || colr.length < 4) {
                continue;
            }
            // 첫 4바이트가 colour_type 이다: nclx 는 색 설명, prof 와 rICC 는 ICC 프로파일.
            String type = "" + (char) c.propU8(colr, 0) + (char) c.propU8(colr, 1) + (char) c.propU8(colr, 2) + (char) c.propU8(colr, 3);
            if (type.equals("nclx") && colr.length >= 11 && !sawNclx) {
                sawNclx = true;
                pic.colorPrimaries = c.propU16(colr, 4);
                pic.transferCharacteristics = c.propU16(colr, 6);
                pic.matrixCoefficients = c.propU16(colr, 8);
                pic.fullRange = (c.propU8(colr, 10) & 0x80) != 0;
            } else if ((type.equals("prof") || type.equals("rICC")) && pic.iccProfile == null && colr.length > 4) {
                pic.iccProfile = java.util.Arrays.copyOfRange(c.data, colr.offset + 4, colr.offset + colr.length);
            }
        }
    }

    /**
     * Chooses the conversion of the samples of a picture to sRGB: the embedded ICC profile if there is one that can be used (matrix/TRC and
     * gray profiles), else the colour primaries and transfer characteristics of the <code>nclx</code> property or of the AV1 sequence header
     * (unspecified values are sRGB). A gray profile only fits a monochrome picture.
     * <p>
     * 화면의 샘플을 sRGB 로 바꾸는 변환을 고른다: 쓸 수 있는 내장 ICC 프로파일(행렬/TRC 와 회색조 프로파일)이 있으면 그것, 아니면 <code>nclx</code>
     * 속성이나 AV1 시퀀스 헤더의 색 원색과 전달 특성(지정되지 않은 값은 sRGB). 회색조 프로파일은 단색 화면에만 맞는다.
     *
     * @param pic the picture / 화면
     * @return the conversion, never null / 변환. null 이 아님
     */
    private static ColorProfile colorProfile(Av1Picture pic) {
        ColorProfile p = pic.iccProfile == null ? null : ColorProfile.fromIcc(pic.iccProfile);
        if (p == null) {
            p = ColorProfile.fromCicp(pic.colorPrimaries, pic.transferCharacteristics);
        }
        return p == null ? ColorProfile.SRGB : p.forImage(pic.numPlanes == 1);
    }

    /**
     * Copies the colour description of a picture.
     * <p>
     * 화면의 색 설명을 복사한다.
     *
     * @param from the source / 원본
     * @param to   the target / 대상
     */
    private static void copyColor(Av1Picture from, Av1Picture to) {
        to.matrixCoefficients = from.matrixCoefficients;
        to.colorPrimaries = from.colorPrimaries;
        to.transferCharacteristics = from.transferCharacteristics;
        to.fullRange = from.fullRange;
        to.iccProfile = from.iccProfile;
    }

    /**
     * Decodes the tiles of a grid item and joins them in the YUV domain.
     * <p>
     * 격자 항목의 타일을 디코딩하여 YUV 영역에서 이어 붙인다.
     *
     * @param c      the container / 컨테이너
     * @param it     the grid item / 격자 항목
     * @param limits the limits / 제한
     * @return the picture / 화면
     * @throws MediaException CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE or CANCELLED / CORRUPT, UNSUPPORTED_FORMAT, TOO_LARGE 또는 CANCELLED
     */
    private static Av1Picture decodeGrid(AvifContainer c, AvifContainer.Item it, DecodeLimits limits) throws MediaException {
        byte[] g = c.itemData(it, 1024);
        if (g.length < 8 || g[0] != 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: invalid grid description");
        }
        int fieldSize = (g[1] & 1) != 0 ? 4 : 2;
        int rows = (g[2] & 0xFF) + 1;
        int cols = (g[3] & 0xFF) + 1;
        if (g.length < 4 + 2 * fieldSize) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: truncated grid description");
        }
        long outW = 0;
        long outH = 0;
        // 출력 너비와 높이를 빅 엔디언으로 읽는다.
        for (int i = 0; i < fieldSize; i++) {
            outW = (outW << 8) | (g[4 + i] & 0xFF);
            outH = (outH << 8) | (g[4 + fieldSize + i] & 0xFF);
        }
        limits.checkImageSize(outW, outH);
        int[] ids = c.referencedBy("dimg", it.id);
        if (ids.length != rows * cols || ids.length > limits.getMaxFrames()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the grid does not reference " + rows * cols + " tiles");
        }
        short[][] planes = null;
        int[] stride = new int[3];
        Av1Picture first = null;
        int tileW = 0;
        int tileH = 0;
        // 타일을 차례로 디코딩하여 캔버스의 제 위치에 복사한다.
        for (int t = 0; t < ids.length; t++) {
            AvifContainer.Item ti = c.items.get(Integer.valueOf(ids[t]));
            if (ti == null) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: a grid tile is missing");
            }
            Av1Picture tile = decodeItem(c, ti, limits, 1);
            if (first == null) {
                first = tile;
                tileW = tile.width;
                tileH = tile.height;
                if ((tileW & ((1 << tile.subX) - 1)) != 0 || (tileH & ((1 << tile.subY) - 1)) != 0) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the tile size does not fit the chroma subsampling");
                }
                if ((long) tileW * cols < outW || (long) tileH * rows < outH) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the grid tiles do not cover the output image");
                }
                limits.checkImageSize((long) tileW * cols, (long) tileH * rows);
                planes = new short[3][];
                // 평면마다 캔버스를 할당한다.
                for (int p = 0; p < tile.numPlanes; p++) {
                    int sx = p == 0 ? 0 : tile.subX;
                    int sy = p == 0 ? 0 : tile.subY;
                    stride[p] = (tileW * cols) >> sx;
                    planes[p] = new short[stride[p] * ((tileH * rows) >> sy)];
                }
            } else if (tile.width != tileW || tile.height != tileH || tile.bitDepth != first.bitDepth || tile.numPlanes != first.numPlanes
                    || tile.subX != first.subX || tile.subY != first.subY) {
                throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the grid tiles differ in size or format");
            }
            int col = t % cols;
            int row = t / cols;
            // 평면마다 타일의 행을 캔버스에 복사한다.
            for (int p = 0; p < tile.numPlanes; p++) {
                int sx = p == 0 ? 0 : tile.subX;
                int sy = p == 0 ? 0 : tile.subY;
                int pw = tileW >> sx;
                int ph = tileH >> sy;
                int dx = col * pw;
                int dy = row * ph;
                // 타일의 행을 캔버스로 한 줄씩 복사한다.
                for (int y = 0; y < ph; y++) {
                    System.arraycopy(tile.planes[p], y * tile.stride[p], planes[p], (dy + y) * stride[p] + dx, pw);
                }
            }
        }
        Av1Picture out = new Av1Picture((int) outW, (int) outH, first.bitDepth, first.numPlanes, first.subX, first.subY, planes, stride);
        copyColor(first, out);
        applyColr(c, it, out);
        return out;
    }

    /**
     * Applies the clean aperture property (a crop around a centre).
     * <p>
     * 깨끗한 개구 속성(중심 주위의 잘라내기)을 적용한다.
     *
     * @param c    the container / 컨테이너
     * @param pr   the clap property / clap 속성
     * @param src  the pixels / 픽셀
     * @param size the width and height, updated / 너비와 높이(갱신된다)
     * @return the cropped pixels / 잘라낸 픽셀
     * @throws MediaException CORRUPT for an invalid aperture / 잘못된 개구면 CORRUPT
     */
    private static int[] applyClap(AvifContainer c, AvifContainer.Property pr, int[] src, int[] size) throws MediaException {
        int w = size[0];
        int h = size[1];
        double cw = frac(c.propU32(pr, 0), c.propU32(pr, 4));
        double ch = frac(c.propU32(pr, 8), c.propU32(pr, 12));
        double ho = frac((int) c.propU32(pr, 16), c.propU32(pr, 20));
        double vo = frac((int) c.propU32(pr, 24), c.propU32(pr, 28));
        int nw = (int) Math.floor(cw + 0.5);
        int nh = (int) Math.floor(ch + 0.5);
        int x0 = (int) Math.floor(ho + (w - cw) / 2.0 + 0.5);
        int y0 = (int) Math.floor(vo + (h - ch) / 2.0 + 0.5);
        if (nw <= 0 || nh <= 0 || x0 < 0 || y0 < 0 || x0 + nw > w || y0 + nh > h) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the clean aperture lies outside the image");
        }
        int[] out = new int[nw * nh];
        // 잘라낼 사각형의 행을 복사한다.
        for (int y = 0; y < nh; y++) {
            System.arraycopy(src, (y0 + y) * w + x0, out, y * nw, nw);
        }
        size[0] = nw;
        size[1] = nh;
        return out;
    }

    /**
     * Divides two numbers of the clean aperture property (the denominator 0 gives 0).
     * <p>
     * 깨끗한 개구 속성의 두 수를 나눈다(분모 0 은 0 을 준다).
     *
     * @param n the numerator / 분자
     * @param d the denominator / 분모
     * @return the quotient / 몫
     */
    private static double frac(long n, long d) {
        return d == 0 ? 0 : (double) n / (double) d;
    }

    /**
     * Rotates the pixels counter-clockwise by a multiple of 90 degrees (the property irot).
     * <p>
     * 픽셀을 반시계 방향으로 90도의 배수만큼 회전한다(irot 속성).
     *
     * @param src   the pixels / 픽셀
     * @param size  the width and height, updated / 너비와 높이(갱신된다)
     * @param angle the number of quarter turns (0..3) / 90도 회전 횟수(0~3)
     * @return the rotated pixels / 회전한 픽셀
     */
    private static int[] rotate(int[] src, int[] size, int angle) {
        int w = size[0];
        int h = size[1];
        if (angle == 0) {
            return src;
        }
        int[] out = new int[src.length];
        if (angle == 2) {
            // 180도: 순서를 뒤집는다.
            for (int i = 0; i < src.length; i++) {
                out[src.length - 1 - i] = src[i];
            }
            return out;
        }
        int nw = h;
        int nh = w;
        // 90도(1)는 오른쪽 위가 왼쪽 위로, 270도(3)는 왼쪽 위가 오른쪽 위로 간다.
        for (int ny = 0; ny < nh; ny++) {
            for (int nx = 0; nx < nw; nx++) {
                if (angle == 1) {
                    out[ny * nw + nx] = src[nx * w + (w - 1 - ny)];
                } else {
                    out[ny * nw + nx] = src[(h - 1 - nx) * w + ny];
                }
            }
        }
        size[0] = nw;
        size[1] = nh;
        return out;
    }

    /**
     * Mirrors the pixels (the property imir): the axis 0 swaps the top and the bottom rows, the axis 1 swaps the left and right columns.
     * <p>
     * 픽셀을 뒤집는다(imir 속성): 축 0 은 위아래 행을, 축 1 은 좌우 열을 맞바꾼다.
     *
     * @param src  the pixels / 픽셀
     * @param size the width and height / 너비와 높이
     * @param axis the axis 0 or 1 / 축 0 또는 1
     * @return the mirrored pixels / 뒤집은 픽셀
     */
    private static int[] mirror(int[] src, int[] size, int axis) {
        int w = size[0];
        int h = size[1];
        int[] out = new int[src.length];
        // 축에 따라 행 또는 열의 순서를 뒤집어 복사한다.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (axis == 0) {
                    out[(h - 1 - y) * w + x] = src[y * w + x];
                } else {
                    out[y * w + (w - 1 - x)] = src[y * w + x];
                }
            }
        }
        return out;
    }
}
