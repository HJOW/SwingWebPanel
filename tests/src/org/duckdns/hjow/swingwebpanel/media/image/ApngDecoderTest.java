package org.duckdns.hjow.swingwebpanel.media.image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the APNG part of {@link PngDecoder} (plan items P18-02, P18-03; FEATURE_MATRIX IMG-02): frame composition with every dispose and
 * blend operation, the default image inside or outside the animation, delays, play counts, and the fall back to the default image for
 * inconsistent animations.
 * <p>
 * {@link PngDecoder} 의 APNG 부분 시험(계획 P18-02, P18-03; FEATURE_MATRIX IMG-02): 모든 처리/합성 연산을 쓴 프레임 합성, 애니메이션 안팎의 기본
 * 이미지, 지연, 재생 횟수, 일관되지 않은 애니메이션에서 기본 이미지로의 대체.
 */
public class ApngDecoderTest {

    /**
     * Opaque red.
     * <p>
     * 불투명 빨강.
     */
    private static final int RED = 0xFFFF0000;

    /**
     * Opaque green.
     * <p>
     * 불투명 초록.
     */
    private static final int GREEN = 0xFF00FF00;

    /**
     * Opaque blue.
     * <p>
     * 불투명 파랑.
     */
    private static final int BLUE = 0xFF0000FF;

    /**
     * Opaque white.
     * <p>
     * 불투명 흰색.
     */
    private static final int WHITE = 0xFFFFFFFF;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ApngDecoderTest() {
    }

    /**
     * Builds RGBA 8-bit samples of a solid rectangle.
     * <p>
     * 단색 사각형의 RGBA 8비트 샘플을 만든다.
     *
     * @param w    width / 가로
     * @param h    height / 세로
     * @param argb colour / 색
     * @return samples / 샘플
     */
    private static int[] solid(int w, int h, int argb) {
        int[] s = new int[w * h * 4];
        // 픽셀마다 R, G, B, A 샘플을 채운다.
        for (int i = 0; i < w * h; i++) {
            s[i * 4] = (argb >> 16) & 0xFF;
            s[i * 4 + 1] = (argb >> 8) & 0xFF;
            s[i * 4 + 2] = argb & 0xFF;
            s[i * 4 + 3] = argb >>> 24;
        }
        return s;
    }

    /**
     * Creates a 4 x 4 RGBA description with a solid default image.
     * <p>
     * 단색 기본 이미지를 가진 4 x 4 RGBA 설명을 만든다.
     *
     * @param argb colour of the default image / 기본 이미지의 색
     * @return description / 설명
     */
    private static PngBuilder.Spec canvas(int argb) {
        PngBuilder.Spec s = new PngBuilder.Spec(4, 4);
        s.samples = solid(4, 4, argb);
        return s;
    }

    /**
     * Creates a frame.
     * <p>
     * 프레임을 만든다.
     *
     * @param w       width / 가로
     * @param h       height / 세로
     * @param x       x offset / 가로 위치
     * @param y       y offset / 세로 위치
     * @param argb    colour / 색
     * @param dispose dispose operation / 처리 방법
     * @param blend   blend operation / 합성 방법
     * @return frame / 프레임
     */
    private static PngBuilder.FrameSpec frame(int w, int h, int x, int y, int argb, int dispose, int blend) {
        PngBuilder.FrameSpec f = new PngBuilder.FrameSpec(w, h, x, y, solid(w, h, argb));
        f.dispose = dispose;
        f.blend = blend;
        return f;
    }

    /**
     * Fills a rectangle of an expected canvas.
     * <p>
     * 기대 캔버스의 사각형을 채운다.
     *
     * @param c    canvas 4 x 4 / 4 x 4 캔버스
     * @param x    left / 왼쪽
     * @param y    top / 위쪽
     * @param w    width / 가로
     * @param h    height / 세로
     * @param argb colour / 색
     */
    private static void fill(int[] c, int x, int y, int w, int h, int argb) {
        // 사각형의 행과 열을 차례로 채운다.
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                c[yy * 4 + xx] = argb;
            }
        }
    }

    /**
     * Decodes with default limits.
     * <p>
     * 기본 제한으로 디코딩한다.
     *
     * @param d data / 데이터
     * @return image / 이미지
     * @throws MediaException on failure / 실패 시
     */
    private static DecodedImage decode(byte[] d) throws MediaException {
        return new PngDecoder().decode(d, DecodeLimits.DEFAULT);
    }

    /**
     * Composition with the default image as the first frame: SOURCE and OVER blending, BACKGROUND disposal, delays and play count.
     * <p>
     * 기본 이미지가 첫 프레임인 합성: SOURCE 와 OVER 합성, BACKGROUND 처리, 지연, 재생 횟수.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBasicAnimation() throws Exception {
        List<PngBuilder.FrameSpec> frames = new ArrayList<PngBuilder.FrameSpec>();
        PngBuilder.FrameSpec f0 = frame(4, 4, 0, 0, RED, 0, 0);
        f0.delayNum = 1;
        f0.delayDen = 10;
        PngBuilder.FrameSpec f1 = frame(2, 2, 1, 1, GREEN, 1, 0);
        f1.delayNum = 20;
        f1.delayDen = 100;
        PngBuilder.FrameSpec f2 = frame(2, 2, 0, 0, 0x800000FF, 0, 1);
        f2.delayNum = 0;
        f2.delayDen = 0;
        frames.add(f0);
        frames.add(f1);
        frames.add(f2);
        DecodedImage img = decode(PngBuilder.encodeApng(canvas(WHITE), 3, true, frames, false));
        Check.equal(4, img.getWidth(), "width");
        Check.equal(3, img.getFrames().size(), "frame count");
        Check.isTrue(img.isAnimated(), "animated");
        Check.equal(3, img.getLoopCount(), "play count");
        int[] e0 = new int[16];
        Arrays.fill(e0, RED);
        ImageAsserts.framePixels(img.getFrames().get(0), e0, "frame 0");
        int[] e1 = e0.clone();
        fill(e1, 1, 1, 2, 2, GREEN);
        ImageAsserts.framePixels(img.getFrames().get(1), e1, "frame 1");
        // 프레임 1 은 BACKGROUND 처리이므로 2번 프레임 전에 그 영역이 투명해진다.
        int[] e2 = e0.clone();
        fill(e2, 1, 1, 2, 2, 0);
        fill(e2, 0, 0, 2, 2, RED);
        int[] f2Frame = img.getFrames().get(2).getArgb();
        Check.equal(16, f2Frame.length, "frame 2 size");
        ImageAsserts.near(0x7F8000FF + 0x01000000 * 0, f2Frame[0] | 0, 255, "pixel 0 only checks it is defined");
        // (0,0), (1,0), (0,1): 반투명 파랑이 빨강 위에 합성된다.
        int blended = 0xFF7F0080;
        ImageAsserts.near(blended, f2Frame[0], 2, "blue over red at (0,0)");
        ImageAsserts.near(blended, f2Frame[1], 2, "blue over red at (1,0)");
        ImageAsserts.near(blended, f2Frame[4], 2, "blue over red at (0,1)");
        // (1,1): 지워진 투명 위에 합성하면 원본 픽셀 그대로이다.
        Check.equal(0x800000FF, f2Frame[5], "blue over transparent keeps the pixel");
        // 지워진 나머지 영역은 투명하고 그 밖은 빨강이다.
        Check.equal(0, f2Frame[6], "cleared (2,1)");
        Check.equal(0, f2Frame[9], "cleared (1,2)");
        Check.equal(0, f2Frame[10], "cleared (2,2)");
        Check.equal(RED, f2Frame[3], "untouched (3,0)");
        Check.equal(RED, f2Frame[15], "untouched (3,3)");
        Check.equal(100, img.getFrames().get(0).getDelayMillis(), "delay 1/10 s");
        Check.equal(200, img.getFrames().get(1).getDelayMillis(), "delay 20/100 s");
        Check.equal(100, img.getFrames().get(2).getDelayMillis(), "a zero delay becomes 100 ms");
        Check.isTrue(e2.length == 16, "expected canvas built");
    }

    /**
     * The default image is not part of the animation when no fcTL comes before IDAT: the frames come only from fdAT.
     * <p>
     * IDAT 앞에 fcTL 이 없으면 기본 이미지는 애니메이션에 포함되지 않는다: 프레임은 fdAT 에서만 온다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDefaultImageOutsideTheAnimation() throws Exception {
        List<PngBuilder.FrameSpec> frames = new ArrayList<PngBuilder.FrameSpec>();
        frames.add(frame(2, 2, 2, 2, 0xFFFFFF00, 0, 0));
        frames.add(frame(4, 4, 0, 0, 0xFF00FFFF, 0, 0));
        DecodedImage img = decode(PngBuilder.encodeApng(canvas(WHITE), 0, false, frames, false));
        Check.equal(2, img.getFrames().size(), "two frames, the default image is not one of them");
        Check.equal(0, img.getLoopCount(), "play count 0 means forever");
        int[] e0 = new int[16];
        fill(e0, 2, 2, 2, 2, 0xFFFFFF00);
        ImageAsserts.framePixels(img.getFrames().get(0), e0, "frame 0 on a transparent canvas");
        int[] e1 = new int[16];
        Arrays.fill(e1, 0xFF00FFFF);
        ImageAsserts.framePixels(img.getFrames().get(1), e1, "frame 1");
    }

    /**
     * Dispose operation PREVIOUS restores the canvas to its state before the frame; on the first frame it acts like BACKGROUND.
     * <p>
     * 처리 방법 PREVIOUS 는 캔버스를 프레임 이전 상태로 되돌린다. 첫 프레임에서는 BACKGROUND 처럼 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDisposePrevious() throws Exception {
        List<PngBuilder.FrameSpec> frames = new ArrayList<PngBuilder.FrameSpec>();
        frames.add(frame(4, 4, 0, 0, RED, 0, 0));
        frames.add(frame(2, 2, 0, 0, GREEN, 2, 0));
        frames.add(frame(2, 2, 2, 2, BLUE, 0, 0));
        DecodedImage img = decode(PngBuilder.encodeApng(canvas(WHITE), 1, true, frames, false));
        int[] e2 = new int[16];
        Arrays.fill(e2, RED);
        fill(e2, 2, 2, 2, 2, BLUE);
        ImageAsserts.framePixels(img.getFrames().get(2), e2, "the green frame was undone");
        int[] e1 = new int[16];
        Arrays.fill(e1, RED);
        fill(e1, 0, 0, 2, 2, GREEN);
        ImageAsserts.framePixels(img.getFrames().get(1), e1, "the green frame itself is shown");
        // 첫 프레임의 PREVIOUS 는 BACKGROUND 와 같다.
        List<PngBuilder.FrameSpec> first = new ArrayList<PngBuilder.FrameSpec>();
        first.add(frame(4, 4, 0, 0, RED, 2, 0));
        first.add(frame(1, 1, 3, 3, GREEN, 0, 0));
        DecodedImage img2 = decode(PngBuilder.encodeApng(canvas(WHITE), 1, true, first, false));
        int[] e = new int[16];
        e[15] = GREEN;
        ImageAsserts.framePixels(img2.getFrames().get(1), e, "first frame PREVIOUS acts like BACKGROUND");
    }

    /**
     * Blend operation SOURCE replaces the pixels including their alpha; OVER composites.
     * <p>
     * 합성 방법 SOURCE 는 알파를 포함해 픽셀을 바꾸고 OVER 는 합성한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlendOperations() throws Exception {
        List<PngBuilder.FrameSpec> frames = new ArrayList<PngBuilder.FrameSpec>();
        frames.add(frame(4, 4, 0, 0, RED, 0, 0));
        frames.add(frame(2, 2, 0, 0, 0x800000FF, 0, 0));
        frames.add(frame(2, 2, 2, 2, 0x00000000, 0, 1));
        DecodedImage img = decode(PngBuilder.encodeApng(canvas(WHITE), 1, true, frames, false));
        int[] e1 = new int[16];
        Arrays.fill(e1, RED);
        fill(e1, 0, 0, 2, 2, 0x800000FF);
        ImageAsserts.framePixels(img.getFrames().get(1), e1, "SOURCE replaces red with half transparent blue");
        // 완전 투명한 OVER 프레임은 아무것도 바꾸지 않는다.
        ImageAsserts.framePixels(img.getFrames().get(2), e1, "OVER with a transparent frame changes nothing");
    }

    /**
     * Frame delays: numerator over denominator seconds, a zero denominator means 100, and 10 ms or less becomes 100 ms.
     * <p>
     * 프레임 지연: 분자/분모 초, 분모 0 은 100 으로 보고, 10 ms 이하는 100 ms 가 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDelays() throws Exception {
        int[][] cases = { { 1, 10, 100 }, { 1, 4, 250 }, { 3, 1000, 100 }, { 0, 0, 100 }, { 50, 100, 500 }, { 7, 0, 70 }, { 2, 1, 2000 } };
        List<PngBuilder.FrameSpec> frames = new ArrayList<PngBuilder.FrameSpec>();
        // 사례마다 프레임 하나를 만든다.
        for (int[] c : cases) {
            PngBuilder.FrameSpec f = frame(4, 4, 0, 0, RED, 0, 0);
            f.delayNum = c[0];
            f.delayDen = c[1];
            frames.add(f);
        }
        DecodedImage img = decode(PngBuilder.encodeApng(canvas(WHITE), 2, true, frames, false));
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < cases.length; i++) {
            Check.equal(cases[i][2], img.getFrames().get(i).getDelayMillis(), "delay " + cases[i][0] + "/" + cases[i][1]);
        }
    }

    /**
     * An animation with one frame is a still image.
     * <p>
     * 프레임이 하나인 애니메이션은 정지 이미지이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSingleFrameIsStill() throws Exception {
        List<PngBuilder.FrameSpec> frames = new ArrayList<PngBuilder.FrameSpec>();
        frames.add(frame(4, 4, 0, 0, GREEN, 0, 0));
        DecodedImage img = decode(PngBuilder.encodeApng(canvas(WHITE), 1, true, frames, false));
        Check.isFalse(img.isAnimated(), "one frame is not animated");
        int[] e = new int[16];
        Arrays.fill(e, GREEN);
        ImageAsserts.pixels(img, 4, 4, e, "the single frame");
    }

    /**
     * Interlaced frames and frames in other positions compose correctly (random pixels, alpha 0 or 255).
     * <p>
     * 인터레이스 프레임과 다른 위치의 프레임도 올바르게 합성된다(임의 픽셀, 알파 0 또는 255).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInterlacedFrames() throws Exception {
        PngBuilder.Spec s = new PngBuilder.Spec(11, 9);
        s.interlace = true;
        s.samples = new int[11 * 9 * 4];
        java.util.Random rnd = new java.util.Random(7);
        // 기본 이미지와 프레임의 픽셀은 알파가 0 또는 255 가 되게 만든다.
        for (int i = 0; i < 11 * 9; i++) {
            for (int c = 0; c < 3; c++) {
                s.samples[i * 4 + c] = rnd.nextInt(256);
            }
            s.samples[i * 4 + 3] = 255;
        }
        List<PngBuilder.FrameSpec> frames = new ArrayList<PngBuilder.FrameSpec>();
        PngBuilder.FrameSpec f0 = new PngBuilder.FrameSpec(11, 9, 0, 0, s.samples);
        int[] sub = new int[5 * 4 * 4];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < 5 * 4; i++) {
            sub[i * 4] = rnd.nextInt(256);
            sub[i * 4 + 1] = rnd.nextInt(256);
            sub[i * 4 + 2] = rnd.nextInt(256);
            sub[i * 4 + 3] = (i % 3 == 0) ? 0 : 255;
        }
        PngBuilder.FrameSpec f1 = new PngBuilder.FrameSpec(5, 4, 3, 2, sub);
        f1.blend = 1;
        frames.add(f0);
        frames.add(f1);
        DecodedImage img = decode(PngBuilder.encodeApng(s, 1, true, frames, false));
        int[] e1 = new int[11 * 9];
        // 기대값: 첫 프레임을 깔고 두 번째 프레임의 불투명 픽셀만 덮어쓴다.
        for (int i = 0; i < e1.length; i++) {
            e1[i] = 0xFF000000 | (s.samples[i * 4] << 16) | (s.samples[i * 4 + 1] << 8) | s.samples[i * 4 + 2];
        }
        int[] e0 = e1.clone();
        // 각 항목을 차례로 처리한다.
        for (int y = 0; y < 4; y++) {
            // 각 항목을 차례로 처리한다.
            for (int x = 0; x < 5; x++) {
                int i = y * 5 + x;
                if (sub[i * 4 + 3] == 255) {
                    e1[(2 + y) * 11 + 3 + x] = 0xFF000000 | (sub[i * 4] << 16) | (sub[i * 4 + 1] << 8) | sub[i * 4 + 2];
                }
            }
        }
        ImageAsserts.framePixels(img.getFrames().get(0), e0, "interlaced frame 0");
        ImageAsserts.framePixels(img.getFrames().get(1), e1, "interlaced frame 1");
    }

    /**
     * Inconsistent animations fall back to the default image as a still image: a wrong sequence number, a frame outside the canvas, damaged
     * frame data.
     * <p>
     * 일관되지 않은 애니메이션은 기본 이미지를 정지 이미지로 반환한다: 틀린 순서 번호, 캔버스 밖의 프레임, 손상된 프레임 데이터.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFallbackToDefaultImage() throws Exception {
        int[] white = new int[16];
        Arrays.fill(white, WHITE);
        List<PngBuilder.FrameSpec> frames = new ArrayList<PngBuilder.FrameSpec>();
        frames.add(frame(2, 2, 0, 0, RED, 0, 0));
        frames.add(frame(2, 2, 1, 1, GREEN, 0, 0));
        frames.add(frame(2, 2, 2, 2, BLUE, 0, 0));
        DecodedImage bad = decode(PngBuilder.encodeApng(canvas(WHITE), 1, false, frames, true));
        Check.isFalse(bad.isAnimated(), "wrong sequence number: still image");
        ImageAsserts.pixels(bad, 4, 4, white, "wrong sequence number: default image");
        List<PngBuilder.FrameSpec> outside = new ArrayList<PngBuilder.FrameSpec>();
        outside.add(frame(2, 2, 3, 3, RED, 0, 0));
        outside.add(frame(2, 2, 0, 0, GREEN, 0, 0));
        DecodedImage out = decode(PngBuilder.encodeApng(canvas(WHITE), 1, false, outside, false));
        Check.isFalse(out.isAnimated(), "frame outside the canvas: still image");
        ImageAsserts.pixels(out, 4, 4, white, "frame outside the canvas: default image");
        // 마지막 fdAT 청크 중간에서 잘라 프레임 데이터를 손상시킨다.
        byte[] ok = PngBuilder.encodeApng(canvas(WHITE), 1, false, frames, false);
        int last = lastIndexOf(ok, "fdAT");
        DecodedImage cut = decode(ImageAsserts.prefix(ok, last + 6));
        Check.isFalse(cut.isAnimated(), "truncated frame data: still image");
        ImageAsserts.pixels(cut, 4, 4, white, "truncated frame data: default image");
        // 기본 이미지 자체가 손상되면 대체할 수 없으므로 CORRUPT 이다.
        List<PngBuilder.FrameSpec> full = new ArrayList<PngBuilder.FrameSpec>();
        full.add(frame(4, 4, 0, 0, RED, 0, 0));
        full.add(frame(2, 2, 1, 1, GREEN, 0, 0));
        final byte[] noDefault = ImageAsserts.prefix(PngBuilder.encodeApng(canvas(WHITE), 1, true, full, false), 120);
        ImageAsserts.fails(MediaException.Kind.CORRUPT, new Block() {
            /**
             * Decodes a file whose default image is cut off.
             * <p>
             * 기본 이미지가 잘린 파일을 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                decode(noDefault);
            }
        }, "damaged default image");
    }

    /**
     * Finds the last occurrence of a four letter chunk type.
     * <p>
     * 네 글자 청크 유형의 마지막 위치를 찾는다.
     *
     * @param d    data / 데이터
     * @param type chunk type / 청크 유형
     * @return position of the type letters / 유형 글자의 위치
     */
    private static int lastIndexOf(byte[] d, String type) {
        byte[] t = new byte[4];
        // 각 항목을 차례로 처리한다.
        for (int k = 0; k < 4; k++) {
            t[k] = (byte) type.charAt(k);
        }
        // 뒤에서부터 유형 글자가 일치하는 위치를 찾는다.
        for (int i = d.length - 4; i >= 0; i--) {
            if (d[i] == t[0] && d[i + 1] == t[1] && d[i + 2] == t[2] && d[i + 3] == t[3]) {
                return i;
            }
        }
        throw new IllegalStateException("type not found");
    }

    /**
     * Frame and pixel limits are enforced and reported as TOO_LARGE (not as a fall back).
     * <p>
     * 프레임과 픽셀 제한은 지켜지며 TOO_LARGE 로 보고된다(대체되지 않는다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnimationLimits() throws Exception {
        List<PngBuilder.FrameSpec> frames = new ArrayList<PngBuilder.FrameSpec>();
        // 프레임 다섯 개를 만든다.
        for (int i = 0; i < 5; i++) {
            frames.add(frame(4, 4, 0, 0, RED, 0, 0));
        }
        final byte[] apng = PngBuilder.encodeApng(canvas(WHITE), 1, true, frames, false);
        ImageAsserts.fails(MediaException.Kind.TOO_LARGE, new Block() {
            /**
             * Decodes five frames with a limit of three.
             * <p>
             * 제한이 3 일 때 프레임 다섯 개를 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                new PngDecoder().decode(apng, new DecodeLimits(1000, 1 << 20, 3, 100000));
            }
        }, "frame limit");
        ImageAsserts.fails(MediaException.Kind.TOO_LARGE, new Block() {
            /**
             * Decodes five 16-pixel frames with a total limit of 60 pixels.
             * <p>
             * 전체 제한이 60 픽셀일 때 16 픽셀 프레임 다섯 개를 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                new PngDecoder().decode(apng, new DecodeLimits(1000, 1 << 20, 100, 60));
            }
        }, "total pixel limit");
        Check.equal(5, new PngDecoder().decode(apng, DecodeLimits.DEFAULT).getFrames().size(), "within the limits");
    }

    /**
     * An acTL chunk after the image data is ignored (the file is a plain PNG).
     * <p>
     * 이미지 데이터 뒤의 acTL 청크는 무시한다(일반 PNG 로 본다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLateActlIsIgnored() throws Exception {
        PngBuilder.Spec s = canvas(GREEN);
        byte[] png = PngBuilder.encode(s);
        // IEND 앞(끝에서 12바이트 앞)에 acTL 과 fcTL 을 끼워 넣는다.
        byte[] actl = PngBuilder.chunk("acTL", new Bytes().be32(2).be32(0).toBytes());
        byte[] fctl = PngBuilder.chunk("fcTL", new Bytes().be32(0).be32(4).be32(4).be32(0).be32(0).be16(1).be16(10).u8(0).u8(0).toBytes());
        Bytes b = new Bytes();
        b.put(ImageAsserts.prefix(png, png.length - 12)).put(actl).put(fctl).put(ImageAsserts.prefix(Arrays.copyOfRange(png, png.length - 12, png.length), 12));
        DecodedImage img = decode(b.toBytes());
        Check.isFalse(img.isAnimated(), "still image");
        int[] e = new int[16];
        Arrays.fill(e, GREEN);
        ImageAsserts.pixels(img, 4, 4, e, "plain PNG");
    }
}
