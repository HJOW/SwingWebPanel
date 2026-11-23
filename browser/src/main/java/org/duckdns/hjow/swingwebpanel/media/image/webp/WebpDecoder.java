package org.duckdns.hjow.swingwebpanel.media.image.webp;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedFrame;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.ExifOrientation;
import org.duckdns.hjow.swingwebpanel.media.image.ImageDecoder;
import org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfile;

/**
 * The decoder of WebP images (plan item P18-03; FEATURE_MATRIX IMG-05; "WebP Container Specification" and RFC 6386 for the lossy VP8 key
 * frames, "WebP Lossless Bitstream Specification" for VP8L). It reads the RIFF container and supports the simple files (a single
 * <code>VP8 </code> or <code>VP8L</code> chunk) and the extended files (<code>VP8X</code>) with the alpha plane (<code>ALPH</code>) of lossy
 * images and with animation (<code>ANIM</code>, <code>ANMF</code>); the EXIF chunk gives the orientation (applied to the pixels, see {@link ExifOrientation}); the ICCP chunk gives the colour profile (a matrix/TRC or gray ICC profile converts the pixels to sRGB, see {@link ColorProfile}; other profiles are ignored); XMP and unknown chunks are skipped.
 * Animation frames are composed on a transparent canvas with the blending and disposal methods of each frame, so every {@link DecodedFrame}
 * is a complete canvas. The background colour of the animation header is ignored, as browsers do. Frame delays of 10 ms or less are shown for
 * 100 ms, the policy of the other animated formats of this project.
 * <p>
 * WebP 이미지의 디코더(계획 P18-03; FEATURE_MATRIX IMG-05; 손실 VP8 키 프레임은 RFC 6386, VP8L 은 "WebP Lossless Bitstream Specification",
 * 컨테이너는 "WebP Container Specification"). RIFF 컨테이너를 읽고 단순 파일(<code>VP8 </code> 또는 <code>VP8L</code> 청크 하나)과 손실 영상의
 * 알파 평면(<code>ALPH</code>), 애니메이션(<code>ANIM</code>, <code>ANMF</code>)을 가진 확장 파일(<code>VP8X</code>)을 지원한다.
 * EXIF 청크는 방향을 주고({@link ExifOrientation} 참조, 픽셀에 적용한다) ICCP 청크는 색 프로파일을 준다(행렬/TRC 나 회색조 ICC 프로파일은 픽셀을
 * sRGB 로 바꾸고 {@link ColorProfile} 참조, 다른 프로파일은 무시한다). XMP 청크와 알 수 없는 청크는 건너뛴다. 애니메이션 프레임은 투명한 캔버스에
 * 프레임별 합성/처리 방법으로 합성하므로 모든 {@link DecodedFrame} 이 완전한 캔버스이다. 애니메이션 헤더의 배경색은 브라우저처럼 무시한다.
 * 지연 10 ms 이하인 프레임은 이 프로젝트의 다른 애니메이션 형식과 같은 정책으로 100 ms 동안 보인다.
 */
public final class WebpDecoder implements ImageDecoder {

    /**
     * Chunk type <code>VP8 </code> (lossy bitstream) as a little-endian integer.
     * <p>
     * 청크 종류 <code>VP8 </code>(손실 비트스트림)의 리틀 엔디언 정수.
     */
    private static final int CHUNK_VP8 = fourCc('V', 'P', '8', ' ');

    /**
     * Chunk type <code>VP8L</code> (lossless bitstream).
     * <p>
     * 청크 종류 <code>VP8L</code>(무손실 비트스트림).
     */
    private static final int CHUNK_VP8L = fourCc('V', 'P', '8', 'L');

    /**
     * Chunk type <code>VP8X</code> (extended header).
     * <p>
     * 청크 종류 <code>VP8X</code>(확장 헤더).
     */
    private static final int CHUNK_VP8X = fourCc('V', 'P', '8', 'X');

    /**
     * Chunk type <code>ALPH</code> (alpha plane of a lossy image).
     * <p>
     * 청크 종류 <code>ALPH</code>(손실 영상의 알파 평면).
     */
    private static final int CHUNK_ALPH = fourCc('A', 'L', 'P', 'H');

    /**
     * Chunk type <code>ANIM</code> (animation header).
     * <p>
     * 청크 종류 <code>ANIM</code>(애니메이션 헤더).
     */
    private static final int CHUNK_ANIM = fourCc('A', 'N', 'I', 'M');

    /**
     * Chunk type <code>ANMF</code> (animation frame).
     * <p>
     * 청크 종류 <code>ANMF</code>(애니메이션 프레임).
     */
    private static final int CHUNK_ANMF = fourCc('A', 'N', 'M', 'F');

    /**
     * Chunk type <code>EXIF</code> (Exif metadata: a TIFF structure that carries the orientation).
     * <p>
     * 청크 종류 <code>EXIF</code>(Exif 메타데이터: 방향을 담은 TIFF 구조).
     */
    private static final int CHUNK_EXIF = fourCc('E', 'X', 'I', 'F');

    /**
     * Chunk type <code>ICCP</code> (an embedded ICC colour profile).
     * <p>
     * 청크 종류 <code>ICCP</code>(내장 ICC 색 프로파일).
     */
    private static final int CHUNK_ICCP = fourCc('I', 'C', 'C', 'P');

    /**
     * Flag of the <code>VP8X</code> chunk that marks an animation.
     * <p>
     * 애니메이션을 나타내는 <code>VP8X</code> 청크의 플래그.
     */
    private static final int FLAG_ANIMATION = 0x02;

    /**
     * Frame delays up to this value (milliseconds) are treated as {@link #SLOW_DELAY}.
     * <p>
     * 이 값(밀리초) 이하의 프레임 지연은 {@link #SLOW_DELAY} 로 본다.
     */
    private static final int FAST_DELAY = 10;

    /**
     * The delay that replaces a frame delay of {@link #FAST_DELAY} or less.
     * <p>
     * {@link #FAST_DELAY} 이하인 프레임 지연을 대신하는 지연.
     */
    private static final int SLOW_DELAY = 100;

    /**
     * Creates the decoder (also used by the registry through reflection).
     * <p>
     * 디코더를 만든다(등록부가 리플렉션으로도 쓴다).
     */
    public WebpDecoder() {
    }

    /**
     * Packs four characters into a little-endian integer as they appear in a file.
     * <p>
     * 문자 네 개를 파일에 놓인 순서대로 리틀 엔디언 정수로 묶는다.
     *
     * @param a first character / 첫 문자
     * @param b second character / 둘째 문자
     * @param c third character / 셋째 문자
     * @param d fourth character / 넷째 문자
     * @return the packed value / 묶은 값
     */
    private static int fourCc(char a, char b, char c, char d) {
        return a | (b << 8) | (c << 16) | (d << 24);
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return <code>webp</code>
     */
    public String getFormatName() {
        return "webp";
    }

    /**
     * Returns the MIME types of the format.
     * <p>
     * 형식의 MIME 타입을 반환한다.
     *
     * @return <code>image/webp</code>
     */
    public String[] getMimeTypes() {
        return new String[] { "image/webp" };
    }

    /**
     * Tells whether the first bytes are <code>RIFF</code>, a size and <code>WEBP</code>.
     * <p>
     * 첫 바이트가 <code>RIFF</code>, 크기, <code>WEBP</code> 인지 알려 준다.
     *
     * @param head the first bytes of the data / 데이터의 첫 바이트
     * @return true if the signature matches / 시그니처가 맞으면 true
     */
    public boolean accepts(byte[] head) {
        if (head == null || head.length < 12) {
            return false;
        }
        return head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F' && head[8] == 'W' && head[9] == 'E' && head[10] == 'B'
                && head[11] == 'P';
    }

    /**
     * Reads a little-endian unsigned integer of 1 to 4 bytes.
     * <p>
     * 1~4 바이트의 리틀 엔디언 부호 없는 정수를 읽는다.
     *
     * @param d   data / 데이터
     * @param pos position / 위치
     * @param n   number of bytes / 바이트 수
     * @return the value / 값
     */
    private static long le(byte[] d, int pos, int n) {
        long v = 0;
        // 높은 자리 바이트부터 모은다.
        for (int i = n - 1; i >= 0; i--) {
            v = (v << 8) | (d[pos + i] & 0xff);
        }
        return v;
    }

    /**
     * Decodes a WebP file completely; an animation yields all composed frames.
     * <p>
     * WebP 파일 전체를 디코딩한다. 애니메이션이면 합성된 프레임을 모두 담는다.
     *
     * @param data   file data / 파일 데이터
     * @param limits limits / 제한
     * @return decoded image / 디코딩된 이미지
     * @throws MediaException CORRUPT for damaged or truncated data, UNSUPPORTED_FORMAT for another format, TOO_LARGE over a limit, CANCELLED if the thread is interrupted / 손상되거나 잘린 데이터는 CORRUPT, 다른 형식은 UNSUPPORTED_FORMAT, 제한 초과는 TOO_LARGE, 스레드가 인터럽트되면 CANCELLED
     */
    public DecodedImage decode(byte[] data, DecodeLimits limits) throws MediaException {
        if (Thread.currentThread().isInterrupted()) {
            throw new MediaException(MediaException.Kind.CANCELLED, "decoding was cancelled");
        }
        if (!accepts(data)) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "not a WebP file");
        }
        long declared = le(data, 4, 4) + 8;
        if (declared < 20) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the RIFF size of the WebP file is too small");
        }
        if (declared > data.length) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the WebP file is truncated");
        }
        int end = (int) declared;
        Chunks chunks = new Chunks(data, 12, end);
        if (!chunks.next()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the WebP file has no chunks");
        }
        if (chunks.type == CHUNK_VP8 || chunks.type == CHUNK_VP8L) {
            // 단순 파일: 청크 하나가 곧 이미지이다.
            WebpRaster r = decodeBitstream(data, chunks.type, chunks.offset, chunks.length, -1, 0, limits);
            return new DecodedImage(r.width, r.height, r.argb);
        }
        if (chunks.type != CHUNK_VP8X) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unknown first chunk of the WebP file");
        }
        return decodeExtended(data, chunks, limits);
    }

    /**
     * Decodes an extended file: the header chunk is the current chunk of the cursor.
     * <p>
     * 확장 파일을 디코딩한다. 헤더 청크가 커서의 현재 청크이다.
     *
     * @param data   data / 데이터
     * @param chunks the cursor at the <code>VP8X</code> chunk / <code>VP8X</code> 청크에 있는 커서
     * @param limits limits / 제한
     * @return the image / 이미지
     * @throws MediaException on failure / 실패 시
     */
    private DecodedImage decodeExtended(byte[] data, Chunks chunks, DecodeLimits limits) throws MediaException {
        if (chunks.length < 10) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the VP8X chunk is too short");
        }
        int flags = data[chunks.offset] & 0xff;
        long canvasW = le(data, chunks.offset + 4, 3) + 1;
        long canvasH = le(data, chunks.offset + 7, 3) + 1;
        limits.checkImageSize(canvasW, canvasH);
        boolean animated = (flags & FLAG_ANIMATION) != 0;
        int alphaOff = -1;
        int alphaLen = 0;
        boolean sawAnim = false;
        int loops = 0;
        Animator animator = animated ? new Animator((int) canvasW, (int) canvasH, limits) : null;
        // 정지 이미지는 이미지 청크 뒤에 오는 EXIF 청크(방향)까지 읽기 위해 끝까지 훑는다.
        WebpRaster still = null;
        int orientation = 1;
        boolean sawExif = false;
        byte[] icc = null;
        // 나머지 청크를 차례로 보며 필요한 것만 처리한다.
        while (true) {
            boolean more;
            try {
                more = chunks.next();
            } catch (MediaException e) {
                // 이미지를 이미 읽은 정지 파일의 뒤쪽 손상은 이미지를 해치지 않는다.
                if (still != null) {
                    break;
                }
                throw e;
            }
            if (!more) {
                break;
            }
            if (chunks.type == CHUNK_EXIF && !sawExif) {
                // 방향은 첫 EXIF 청크의 것만 쓴다.
                sawExif = true;
                orientation = ExifOrientation.read(data, chunks.offset, chunks.length);
            } else if (chunks.type == CHUNK_ICCP && icc == null) {
                // 색 프로파일도 첫 ICCP 청크의 것만 쓴다.
                icc = java.util.Arrays.copyOfRange(data, chunks.offset, chunks.offset + chunks.length);
            } else if (chunks.type == CHUNK_ALPH && !animated) {
                alphaOff = chunks.offset;
                alphaLen = chunks.length;
            } else if ((chunks.type == CHUNK_VP8 || chunks.type == CHUNK_VP8L) && !animated) {
                if (still != null) {
                    continue;
                }
                WebpRaster r = decodeBitstream(data, chunks.type, chunks.offset, chunks.length, alphaOff, alphaLen, limits);
                if (r.width != canvasW || r.height != canvasH) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "the image size differs from the canvas size");
                }
                still = r;
            } else if (chunks.type == CHUNK_ANIM && animated) {
                if (chunks.length < 6) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "the ANIM chunk is too short");
                }
                loops = (int) le(data, chunks.offset + 4, 2);
                sawAnim = true;
            } else if (chunks.type == CHUNK_ANMF && animated) {
                if (!sawAnim) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "an animation frame comes before the ANIM chunk");
                }
                animator.addFrame(data, chunks.offset, chunks.length);
            }
        }
        if (!animated) {
            if (still == null) {
                throw new MediaException(MediaException.Kind.CORRUPT, "the WebP file has no image data");
            }
            return ExifOrientation.apply(toSrgb(new DecodedImage(still.width, still.height, still.argb), icc), orientation);
        }
        if (animator.frames.isEmpty()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the animated WebP file has no frames");
        }
        return ExifOrientation.apply(toSrgb(new DecodedImage((int) canvasW, (int) canvasH, animator.frames, loops), icc), orientation);
    }

    /**
     * Converts the pixels of an image to sRGB with the profile of an <code>ICCP</code> chunk. Without a profile, and with a profile that
     * is not of a supported kind, the image is returned as it is (the samples are taken as sRGB).
     * <p>
     * <code>ICCP</code> 청크의 프로파일로 이미지의 픽셀을 sRGB 로 바꾼다. 프로파일이 없거나 지원하지 않는 종류이면 이미지를 그대로 돌려준다(샘플을
     * sRGB 로 본다).
     *
     * @param image the decoded image / 디코딩된 이미지
     * @param icc   the bytes of the profile, or null / 프로파일의 바이트. 없으면 null
     * @return the image / 이미지
     */
    private static DecodedImage toSrgb(DecodedImage image, byte[] icc) {
        ColorProfile profile = icc == null ? null : ColorProfile.fromIcc(icc);
        return profile == null ? image : profile.forImage(false).convert(image);
    }

    /**
     * Decodes a lossy or lossless bitstream chunk, with the alpha plane of a lossy one.
     * <p>
     * 손실 또는 무손실 비트스트림 청크를 디코딩한다. 손실이면 알파 평면도 적용한다.
     *
     * @param data     data / 데이터
     * @param type     chunk type / 청크 종류
     * @param off      start of the chunk data / 청크 데이터 시작
     * @param len      length of the chunk data / 청크 데이터 길이
     * @param alphaOff start of an <code>ALPH</code> chunk, or -1 / <code>ALPH</code> 청크 시작. 없으면 -1
     * @param alphaLen length of the <code>ALPH</code> chunk / <code>ALPH</code> 청크 길이
     * @param limits   limits / 제한
     * @return the raster / 래스터
     * @throws MediaException on failure / 실패 시
     */
    private static WebpRaster decodeBitstream(byte[] data, int type, int off, int len, int alphaOff, int alphaLen, DecodeLimits limits)
            throws MediaException {
        if (type == CHUNK_VP8L) {
            return Vp8lDecoder.decode(data, off, len, limits);
        }
        int[] size = Vp8Decoder.readSize(data, off, len);
        limits.checkImageSize(size[0], size[1]);
        Vp8Frame frame = Vp8Decoder.decode(data, off, len, limits);
        byte[] alpha = null;
        if (alphaOff >= 0) {
            alpha = WebpAlpha.decode(data, alphaOff, alphaLen, frame.width, frame.height);
        }
        return new WebpRaster(frame.width, frame.height, frame.toArgb(alpha), alphaOff >= 0);
    }

    /**
     * A cursor over the chunks of a RIFF container.
     * <p>
     * RIFF 컨테이너의 청크를 훑는 커서.
     */
    private static final class Chunks {

        /**
         * The data.
         * <p>
         * 데이터.
         */
        private final byte[] data;

        /**
         * Position of the next chunk header.
         * <p>
         * 다음 청크 헤더의 위치.
         */
        private int pos;

        /**
         * End of the container (exclusive).
         * <p>
         * 컨테이너의 끝(포함하지 않음).
         */
        private final int end;

        /**
         * Type of the current chunk (little-endian four characters).
         * <p>
         * 현재 청크의 종류(리틀 엔디언 문자 네 개).
         */
        int type;

        /**
         * Start of the data of the current chunk.
         * <p>
         * 현재 청크 데이터의 시작.
         */
        int offset;

        /**
         * Length of the data of the current chunk (without padding).
         * <p>
         * 현재 청크 데이터의 길이(패딩 제외).
         */
        int length;

        /**
         * Creates a cursor.
         * <p>
         * 커서를 만든다.
         *
         * @param data  data / 데이터
         * @param start position of the first chunk / 첫 청크의 위치
         * @param end   end of the container / 컨테이너의 끝
         */
        Chunks(byte[] data, int start, int end) {
            this.data = data;
            this.pos = start;
            this.end = end;
        }

        /**
         * Moves to the next chunk.
         * <p>
         * 다음 청크로 옮긴다.
         *
         * @return false if there are no more chunks / 더 이상 청크가 없으면 false
         * @throws MediaException CORRUPT if a chunk does not fit into the container / 청크가 컨테이너에 들어가지 않으면 CORRUPT
         */
        boolean next() throws MediaException {
            if (pos + 8 > end) {
                return false;
            }
            long size = le(data, pos + 4, 4);
            if (pos + 8 + size > end) {
                throw new MediaException(MediaException.Kind.CORRUPT, "a chunk of the WebP file is truncated");
            }
            type = (int) le(data, pos, 4);
            offset = pos + 8;
            length = (int) size;
            // 홀수 길이 청크 뒤에는 패딩 바이트가 한 개 있다.
            pos = offset + length + (length & 1);
            return true;
        }
    }

    /**
     * Composes the frames of an animation on a canvas and collects the finished canvases. The composition follows the animation decoder of
     * libwebp (<code>WebPAnimDecoder</code>) exactly, so that the pixels are the same as the ones the reference implementation produces: a
     * "key frame" (the first frame, a frame that covers the whole canvas without blending, or the frame after a disposal to background of the
     * whole canvas or of a key frame) starts on a transparent canvas and is copied; any other frame starts on the previous canvas, where the
     * rectangle of the previous frame is cleared first if that frame asks for disposal to background, and the pixels with partial alpha are
     * blended over the previous canvas with the integer arithmetic of libwebp, except inside the rectangle that was just cleared.
     * <p>
     * 애니메이션의 프레임을 캔버스에 합성하고 완성된 캔버스를 모은다. 합성은 libwebp 의 애니메이션 디코더(<code>WebPAnimDecoder</code>)를 그대로
     * 따르므로 참조 구현이 만드는 픽셀과 같다. "키 프레임"(첫 프레임, 합성 없이 캔버스 전체를 덮는 프레임, 캔버스 전체 또는 키 프레임을 배경으로
     * 지운 다음 프레임)은 투명한 캔버스에서 시작해 그대로 복사한다. 다른 프레임은 이전 캔버스에서 시작하며, 이전 프레임이 배경 지우기를 요청했으면
     * 먼저 그 사각형을 지우고, 알파가 일부인 픽셀은 방금 지운 사각형 안을 뺀 곳에서 libwebp 의 정수 연산으로 이전 캔버스 위에 합성한다.
     */
    private static final class Animator {

        /**
         * Canvas width.
         * <p>
         * 캔버스 가로.
         */
        private final int width;

        /**
         * Canvas height.
         * <p>
         * 캔버스 세로.
         */
        private final int height;

        /**
         * The decoding limits.
         * <p>
         * 디코딩 제한.
         */
        private final DecodeLimits limits;

        /**
         * The canvas: the picture of the previous frame until the next frame is drawn on it.
         * <p>
         * 캔버스. 다음 프레임을 그리기 전까지는 이전 프레임의 그림이다.
         */
        private final int[] canvas;

        /**
         * The finished frames.
         * <p>
         * 완성된 프레임.
         */
        final List<DecodedFrame> frames = new ArrayList<DecodedFrame>();

        /**
         * Whether the previous frame asked for disposal to background.
         * <p>
         * 이전 프레임이 배경 지우기를 요청했는지 여부.
         */
        private boolean previousDispose;

        /**
         * Whether the previous frame was a key frame.
         * <p>
         * 이전 프레임이 키 프레임이었는지 여부.
         */
        private boolean previousKey;

        /**
         * Rectangle of the previous frame: x, y, width, height.
         * <p>
         * 이전 프레임의 사각형: x, y, 가로, 세로.
         */
        private final int[] previousRect = new int[4];

        /**
         * Creates the composer; the canvas starts fully transparent.
         * <p>
         * 합성기를 만든다. 캔버스는 완전히 투명하게 시작한다.
         *
         * @param width  canvas width / 캔버스 가로
         * @param height canvas height / 캔버스 세로
         * @param limits limits / 제한
         */
        Animator(int width, int height, DecodeLimits limits) {
            this.width = width;
            this.height = height;
            this.limits = limits;
            this.canvas = new int[width * height];
        }

        /**
         * Decodes one <code>ANMF</code> chunk and draws it.
         * <p>
         * <code>ANMF</code> 청크 하나를 디코딩해 그린다.
         *
         * @param data data / 데이터
         * @param off  start of the chunk data / 청크 데이터 시작
         * @param len  length of the chunk data / 청크 데이터 길이
         * @throws MediaException on failure or if a limit is exceeded / 실패하거나 제한을 넘으면
         */
        void addFrame(byte[] data, int off, int len) throws MediaException {
            if (len < 16) {
                throw new MediaException(MediaException.Kind.CORRUPT, "the ANMF chunk is too short");
            }
            if (frames.size() >= limits.getMaxFrames()) {
                throw new MediaException(MediaException.Kind.TOO_LARGE, "the animation has too many frames");
            }
            if ((frames.size() + 1L) * width * height > limits.getMaxTotalPixels()) {
                throw new MediaException(MediaException.Kind.TOO_LARGE, "the animation has too many pixels");
            }
            long x = 2 * le(data, off, 3);
            long y = 2 * le(data, off + 3, 3);
            long fw = le(data, off + 6, 3) + 1;
            long fh = le(data, off + 9, 3) + 1;
            int duration = (int) le(data, off + 12, 3);
            int flags = data[off + 15] & 0xff;
            boolean dispose = (flags & 1) != 0;
            boolean noBlend = (flags & 2) != 0;
            if (x + fw > width || y + fh > height) {
                throw new MediaException(MediaException.Kind.CORRUPT, "an animation frame lies outside the canvas");
            }
            WebpRaster r = decodeFrameData(data, off + 16, len - 16);
            if (r.width != fw || r.height != fh) {
                throw new MediaException(MediaException.Kind.CORRUPT, "the frame size differs from its bitstream");
            }
            boolean full = fw == width && fh == height;
            boolean previousFull = previousRect[2] == width && previousRect[3] == height;
            boolean key = frames.isEmpty() || ((!r.hasAlpha || noBlend) && full) || (previousDispose && (previousFull || previousKey));
            if (key) {
                // 키 프레임은 투명한 캔버스에서 시작한다.
                java.util.Arrays.fill(canvas, 0);
            } else if (previousDispose) {
                // 이전 프레임의 처리 방법이 배경 지우기이면 그 사각형을 투명하게 되돌린다.
                clear(previousRect[0], previousRect[1], previousRect[2], previousRect[3]);
            }
            draw(r, (int) x, (int) y, !noBlend && !key);
            frames.add(new DecodedFrame(canvas.clone(), duration <= FAST_DELAY ? SLOW_DELAY : duration));
            previousKey = key;
            previousDispose = dispose;
            previousRect[0] = (int) x;
            previousRect[1] = (int) y;
            previousRect[2] = (int) fw;
            previousRect[3] = (int) fh;
        }

        /**
         * Decodes the image data inside an <code>ANMF</code> chunk (an optional <code>ALPH</code> and a <code>VP8 </code> or
         * <code>VP8L</code> chunk; other chunks are skipped).
         * <p>
         * <code>ANMF</code> 청크 안의 이미지 데이터(선택적 <code>ALPH</code> 와 <code>VP8 </code> 또는 <code>VP8L</code> 청크, 다른 청크는 건너뜀)를
         * 디코딩한다.
         *
         * @param data data / 데이터
         * @param off  start of the frame data / 프레임 데이터 시작
         * @param len  length of the frame data / 프레임 데이터 길이
         * @return the frame image / 프레임 이미지
         * @throws MediaException on failure / 실패 시
         */
        private WebpRaster decodeFrameData(byte[] data, int off, int len) throws MediaException {
            Chunks sub = new Chunks(data, off, off + len);
            int alphaOff = -1;
            int alphaLen = 0;
            // 프레임 안의 청크를 차례로 훑어 이미지 청크를 찾는다.
            while (sub.next()) {
                if (sub.type == CHUNK_ALPH) {
                    alphaOff = sub.offset;
                    alphaLen = sub.length;
                } else if (sub.type == CHUNK_VP8 || sub.type == CHUNK_VP8L) {
                    return decodeBitstream(data, sub.type, sub.offset, sub.length, alphaOff, alphaLen, limits);
                }
            }
            throw new MediaException(MediaException.Kind.CORRUPT, "an animation frame has no image data");
        }

        /**
         * Clears a rectangle of the canvas to transparent.
         * <p>
         * 캔버스의 사각형을 투명으로 지운다.
         *
         * @param x left / 왼쪽
         * @param y top / 위
         * @param w width / 가로
         * @param h height / 세로
         */
        private void clear(int x, int y, int w, int h) {
            // 사각형의 행마다 투명으로 채운다.
            for (int row = 0; row < h; row++) {
                java.util.Arrays.fill(canvas, (y + row) * width + x, (y + row) * width + x + w, 0);
            }
        }

        /**
         * Draws a frame image onto the canvas.
         * <p>
         * 프레임 이미지를 캔버스에 그린다.
         *
         * @param r     the frame image / 프레임 이미지
         * @param x     left / 왼쪽
         * @param y     top / 위
         * @param blend true to blend the pixels with partial alpha over the canvas, false to replace the pixels / true 이면 알파가 일부인 픽셀을 캔버스 위에 합성, false 이면 픽셀을 덮어쓴다
         */
        private void draw(WebpRaster r, int x, int y, boolean blend) {
            // 프레임의 행마다 캔버스의 해당 위치에 쓴다.
            for (int row = 0; row < r.height; row++) {
                int dst = (y + row) * width + x;
                int src = row * r.width;
                if (!blend) {
                    System.arraycopy(r.argb, src, canvas, dst, r.width);
                    continue;
                }
                boolean rowInPrevious = previousDispose && y + row >= previousRect[1] && y + row < previousRect[1] + previousRect[3];
                // 합성할 때는 불투명하지 않은 픽셀만 계산한다(방금 지운 사각형 안은 그대로 복사한다).
                for (int col = 0; col < r.width; col++) {
                    int s = r.argb[src + col];
                    boolean cleared = rowInPrevious && x + col >= previousRect[0] && x + col < previousRect[0] + previousRect[2];
                    canvas[dst + col] = (s >>> 24) == 255 || cleared ? s : blendPixel(s, canvas[dst + col]);
                }
            }
        }

        /**
         * Blends a non-premultiplied pixel over another one with the integer arithmetic of libwebp (<code>BlendPixelNonPremult</code>), which
         * approximates the formula of the container specification.
         * <p>
         * libwebp 의 정수 연산(<code>BlendPixelNonPremult</code>)으로 알파를 미리 곱하지 않은 픽셀을 다른 픽셀 위에 합성한다. 컨테이너 규격의
         * 공식을 근사한 것이다.
         *
         * @param src the pixel that is drawn / 그리는 픽셀
         * @param dst the pixel on the canvas / 캔버스의 픽셀
         * @return the blended pixel / 합성된 픽셀
         */
        private static int blendPixel(int src, int dst) {
            int srcA = src >>> 24;
            if (srcA == 0) {
                return dst;
            }
            int dstA = dst >>> 24;
            int dstFactorA = (dstA * (256 - srcA)) >> 8;
            int blendA = srcA + dstFactorA;
            long scale = (1L << 24) / blendA;
            int out = blendA << 24;
            // 세 색 채널을 각각 가중합에 배율을 곱해 계산한다.
            for (int shift = 0; shift <= 16; shift += 8) {
                long unscaled = ((src >> shift) & 0xff) * (long) srcA + ((dst >> shift) & 0xff) * (long) dstFactorA;
                out |= (int) ((unscaled * scale) >> 24) << shift;
            }
            return out;
        }
    }
}
