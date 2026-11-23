package org.duckdns.hjow.swingwebpanel.media.image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the registry {@link ImageDecoders} (plan items P18-01, P18-02, P18-03): the format comes from the file signature and not from the
 * MIME type, errors and limits of the facade, MIME helpers, registration, thread safety and the optional decoders.
 * <p>
 * 등록부 {@link ImageDecoders} 시험(계획 P18-01, P18-02, P18-03): 형식은 MIME 타입이 아니라 파일 시그니처에서 오고, 진입점의 오류와 제한, MIME 도우미,
 * 등록, 스레드 안전성, 선택적 디코더.
 */
public class ImageDecodersTest {

    /**
     * Counts how many decoders of the tests were asked to decode.
     * <p>
     * 시험용 디코더가 디코딩 요청을 받은 횟수를 센다.
     */
    private static final AtomicInteger CALLS = new AtomicInteger();

    /**
     * A decoder for tests that accepts bytes starting with a magic text and does what its mode says.
     * <p>
     * 매직 문자열로 시작하는 바이트를 받아들이고 모드에 따라 동작하는 시험용 디코더.
     */
    private static final class FakeDecoder implements ImageDecoder {
        /**
         * The MIME type this decoder lists.
         * <p>
         * 이 디코더가 가진 MIME 타입.
         */
        private final String mime;

        /**
         * The magic text.
         * <p>
         * 매직 문자열.
         */
        private final String magic;

        /**
         * Colour of the pixel it returns.
         * <p>
         * 반환하는 픽셀의 색.
         */
        private final int argb;

        /**
         * Size of the square image it returns; negative means throw a RuntimeException.
         * <p>
         * 반환하는 정사각형 이미지의 크기. 음수이면 RuntimeException 을 던진다.
         */
        private final int size;

        /**
         * Creates a decoder.
         * <p>
         * 디코더를 만든다.
         *
         * @param magic magic text / 매직 문자열
         * @param mime  MIME type / MIME 타입
         * @param argb  colour / 색
         * @param size  size or -1 to fail / 크기. -1 이면 실패
         */
        FakeDecoder(String magic, String mime, int argb, int size) {
            this.magic = magic;
            this.mime = mime;
            this.argb = argb;
            this.size = size;
        }

        /**
         * Returns the format name.
         * <p>
         * 형식 이름을 반환한다.
         *
         * @return name / 이름
         */
        public String getFormatName() {
            return "fake";
        }

        /**
         * Returns the MIME types.
         * <p>
         * MIME 타입을 반환한다.
         *
         * @return the one MIME type / MIME 타입 하나
         */
        public String[] getMimeTypes() {
            return new String[] { mime };
        }

        /**
         * Checks the magic text.
         * <p>
         * 매직 문자열을 확인한다.
         *
         * @param head first bytes / 첫 바이트
         * @return true if the magic text matches / 매직 문자열이 맞으면 true
         */
        public boolean accepts(byte[] head) {
            if (head.length < magic.length()) {
                return false;
            }
            // 매직 문자열의 문자를 하나씩 비교한다.
            for (int i = 0; i < magic.length(); i++) {
                if (head[i] != (byte) magic.charAt(i)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns an image of the configured colour and size, or fails.
         * <p>
         * 설정한 색과 크기의 이미지를 반환하거나 실패한다.
         *
         * @param data   data / 데이터
         * @param limits limits / 제한
         * @return image / 이미지
         * @throws MediaException never / 던지지 않음
         */
        public DecodedImage decode(byte[] data, DecodeLimits limits) throws MediaException {
            CALLS.incrementAndGet();
            if (size < 0) {
                throw new IllegalStateException("the fake decoder fails");
            }
            int[] px = new int[size * size];
            Arrays.fill(px, argb);
            return new DecodedImage(size, size, px);
        }
    }

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ImageDecodersTest() {
    }

    /**
     * One small valid file per built-in format.
     * <p>
     * 내장 형식마다 작고 올바른 파일 하나.
     *
     * @return files in the order PNG, GIF, JPEG, BMP, ICO / PNG, GIF, JPEG, BMP, ICO 순서의 파일
     * @throws Exception on failure / 실패 시
     */
    private static byte[][] samples() throws Exception {
        PngBuilder.Spec s = new PngBuilder.Spec(3, 2);
        s.samples = new int[3 * 2 * 4];
        Arrays.fill(s.samples, 200);
        byte[] png = PngBuilder.encode(s);
        byte[] gif = new GifBuilder(3, 2, new int[] { 0x112233, 0xFFFFFF }).frame(0, 0, 3, 2, new byte[6], null, false, 0, 0, -1, 2).build();
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(3, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpeg", bos);
        byte[] jpeg = bos.toByteArray();
        byte[] bmp = BmpBuilder.build(true, 40, 3, 2, 24, 0, 0, null, null, BmpBuilder.pack(3, 2, 24, new int[6], false));
        List<byte[]> l = new ArrayList<byte[]>();
        l.add(BmpBuilder.iconDib(3, 2, 32, null, new int[6], new int[6]));
        byte[] ico = BmpBuilder.ico(1, l, new int[] { 3 }, new int[] { 2 });
        return new byte[][] { png, gif, jpeg, bmp, ico };
    }

    /**
     * The decoder is chosen by the file signature; a wrong, missing or odd MIME type does not matter.
     * <p>
     * 디코더는 파일 시그니처로 고르며 틀리거나 없거나 이상한 MIME 타입은 상관없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFormatComesFromTheSignature() throws Exception {
        byte[][] files = samples();
        String[] names = { "png", "gif", "jpeg", "bmp", "ico" };
        String[] hints = { "image/jpeg", "text/html", null, "image/png; charset=binary", "IMAGE/GIF", "", "application/octet-stream" };
        // 형식마다 여러 가지 (틀린) MIME 힌트로 디코딩해 본다.
        for (int i = 0; i < files.length; i++) {
            Check.equal(names[i], ImageDecoders.find(files[i]).getFormatName(), names[i] + ": decoder found by signature");
            // 각 항목을 차례로 처리한다.
            for (String hint : hints) {
                DecodedImage img = ImageDecoders.decode(files[i], hint, DecodeLimits.DEFAULT);
                Check.equal(3, img.getWidth(), names[i] + " with hint " + hint + ": width");
                Check.equal(2, img.getHeight(), names[i] + " with hint " + hint + ": height");
            }
        }
        Check.isNull(ImageDecoders.find(new byte[] { 1, 2, 3 }), "no decoder for garbage");
        Check.isNull(ImageDecoders.find(null), "null head");
        Check.isNull(ImageDecoders.find(new byte[0]), "empty head");
    }

    /**
     * Errors of the facade: empty data, unknown formats, data over the byte limit; null limits mean the defaults.
     * <p>
     * 진입점의 오류: 빈 데이터, 알 수 없는 형식, 바이트 제한을 넘는 데이터. 제한이 null 이면 기본값이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testErrors() throws Exception {
        final byte[][] files = samples();
        fails(MediaException.Kind.CORRUPT, new byte[0], null, DecodeLimits.DEFAULT, "empty data");
        fails(MediaException.Kind.CORRUPT, null, null, DecodeLimits.DEFAULT, "null data");
        fails(MediaException.Kind.UNSUPPORTED_FORMAT, "<html><body>not an image</body></html>".getBytes("US-ASCII"), "image/png",
                DecodeLimits.DEFAULT, "an HTML page");
        fails(MediaException.Kind.UNSUPPORTED_FORMAT, new byte[] { 0x50, 0x4B, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, "image/png", null, "a zip file");
        fails(MediaException.Kind.TOO_LARGE, files[0], null, new DecodeLimits(100000, 10, 10, 100000), "more bytes than the byte limit");
        fails(MediaException.Kind.CORRUPT, Arrays.copyOf(files[0], 20), "image/png", DecodeLimits.DEFAULT, "truncated PNG");
        Check.equal(3, ImageDecoders.decode(files[0], null, null).getWidth(), "null limits use the defaults");
    }

    /**
     * Checks that the facade fails with a kind.
     * <p>
     * 진입점이 주어진 종류로 실패하는지 검사한다.
     *
     * @param kind   expected kind / 기대하는 종류
     * @param data   data / 데이터
     * @param mime   MIME hint / MIME 힌트
     * @param limits limits / 제한
     * @param what   description / 설명
     */
    private static void fails(MediaException.Kind kind, final byte[] data, final String mime, final DecodeLimits limits, String what) {
        ImageAsserts.fails(kind, new Block() {
            /**
             * Decodes through the facade.
             * <p>
             * 진입점으로 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                ImageDecoders.decode(data, mime, limits);
            }
        }, what);
    }

    /**
     * MIME helpers: support check with parameters and letter case, the list without duplicates, and the Accept header.
     * <p>
     * MIME 도우미: 매개변수와 대소문자를 무시한 지원 확인, 중복 없는 목록, Accept 헤더.
     */
    public void testMimeHelpers() {
        String[] yes = { "image/png", "IMAGE/PNG", " image/gif ; q=0.5", "image/jpeg", "image/jpg", "image/pjpeg", "image/bmp", "image/x-icon",
                "image/vnd.microsoft.icon", "image/apng", "image/x-png" };
        // 지원하는 형식을 모두 확인한다.
        for (String m : yes) {
            Check.isTrue(ImageDecoders.isSupportedMime(m), m + " is supported");
        }
        String[] no = { null, "", "image", "image/", "text/html", "image/tiff", "image/foo", "video/mp4", "/png" };
        // 지원하지 않는 형식을 모두 확인한다.
        for (String m : no) {
            Check.isFalse(ImageDecoders.isSupportedMime(m), m + " is not supported");
        }
        final List<String> all = ImageDecoders.getSupportedMimeTypes();
        Check.isTrue(all.contains("image/png") && all.contains("image/gif") && all.contains("image/jpeg") && all.contains("image/bmp")
                && all.contains("image/x-icon"), "the list has the built-in types");
        Check.equal(all.size(), new java.util.HashSet<String>(all).size(), "no duplicates");
        Check.throwsException(UnsupportedOperationException.class, new Block() {
            /**
             * Tries to change the returned list.
             * <p>
             * 반환된 목록을 바꿔 본다.
             */
            public void run() {
                all.add("image/x-test");
            }
        }, "the list cannot be changed");
        String accept = ImageDecoders.getAcceptHeader();
        Check.isTrue(accept.startsWith("image/png,"), "Accept starts with PNG: " + accept);
        Check.isTrue(accept.endsWith(",*/*;q=0.8"), "Accept ends with the wildcard: " + accept);
        Check.isTrue(accept.contains("image/gif") && accept.contains("image/jpeg"), "Accept names GIF and JPEG");
    }

    /**
     * Registration: a registered decoder is used, a MIME hint breaks the tie between decoders with the same signature, a decoder that throws
     * a RuntimeException gives CORRUPT, and an image over the limits is TOO_LARGE even if its decoder did not notice.
     * <p>
     * 등록: 등록한 디코더가 쓰이고 같은 시그니처를 가진 디코더 사이에서는 MIME 힌트가 승부를 가르며 RuntimeException 을 던지는 디코더는 CORRUPT 이고
     * 제한을 넘는 이미지는 디코더가 몰라도 TOO_LARGE 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRegistration() throws Exception {
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Registers null.
             * <p>
             * null 을 등록한다.
             */
            public void run() {
                ImageDecoders.register(null);
            }
        }, "register(null)");
        ImageDecoders.register(new FakeDecoder("SWPFAKE1", "image/x-fake-a", 0xFF0000AA, 1));
        ImageDecoders.register(new FakeDecoder("SWPFAKE1", "image/x-fake-b", 0xFF0000BB, 1));
        byte[] data = "SWPFAKE1-data".getBytes("US-ASCII");
        int before = CALLS.get();
        Check.equal(0xFF0000AA, ImageDecoders.decode(data, null, null).getFrames().get(0).getArgb()[0], "the first registered decoder wins without a hint");
        Check.equal(0xFF0000BB, ImageDecoders.decode(data, "image/x-fake-b", null).getFrames().get(0).getArgb()[0], "the hint breaks the tie");
        Check.equal(0xFF0000AA, ImageDecoders.decode(data, "IMAGE/X-FAKE-A; x=y", null).getFrames().get(0).getArgb()[0], "hint with parameters and case");
        Check.equal(before + 3, CALLS.get(), "each call used one decoder");
        Check.isTrue(ImageDecoders.isSupportedMime("image/x-fake-b"), "the new MIME type is supported");
        ImageDecoders.register(new FakeDecoder("SWPFAIL1", "image/x-fake-fail", 0, -1));
        fails(MediaException.Kind.CORRUPT, "SWPFAIL1 data".getBytes("US-ASCII"), null, null, "a decoder that throws a RuntimeException");
        ImageDecoders.register(new FakeDecoder("SWPBIG01", "image/x-fake-big", 0xFF000000, 5));
        fails(MediaException.Kind.TOO_LARGE, "SWPBIG01 data".getBytes("US-ASCII"), null, new DecodeLimits(10, 1000, 10, 100),
                "an image over the pixel limit that its decoder accepted");
    }

    /**
     * Many threads decode different formats at the same time and every result equals the single-thread result.
     * <p>
     * 여러 스레드가 동시에 여러 형식을 디코딩하며 모든 결과가 단일 스레드 결과와 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testThreadSafety() throws Exception {
        final byte[][] files = samples();
        final int[][] expected = new int[files.length][];
        // 먼저 단일 스레드로 기대 결과를 얻는다.
        for (int i = 0; i < files.length; i++) {
            expected[i] = ImageDecoders.decode(files[i], null, null).getFrames().get(0).getArgb();
        }
        final AtomicInteger failures = new AtomicInteger();
        Thread[] ts = new Thread[8];
        // 스레드마다 모든 형식을 여러 번 디코딩한다.
        for (int t = 0; t < ts.length; t++) {
            ts[t] = new Thread(new Runnable() {
                /**
                 * Decodes all samples repeatedly and compares them.
                 * <p>
                 * 모든 샘플을 반복해서 디코딩하고 비교한다.
                 */
                public void run() {
                    try {
                        // 각 항목을 차례로 처리한다.
                        for (int round = 0; round < 40; round++) {
                            // 각 항목을 차례로 처리한다.
                            for (int i = 0; i < files.length; i++) {
                                int[] got = ImageDecoders.decode(files[i], null, null).getFrames().get(0).getArgb();
                                if (!Arrays.equals(expected[i], got)) {
                                    failures.incrementAndGet();
                                }
                            }
                        }
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    }
                }
            });
            ts[t].start();
        }
        // 모든 스레드가 끝나기를 기다린다.
        for (Thread t : ts) {
            t.join(60000);
        }
        Check.equal(0, failures.get(), "no thread saw a wrong result");
    }

    /**
     * An interrupted thread makes the facade fail with CANCELLED before any decoder is used.
     * <p>
     * 인터럽트된 스레드는 디코더를 쓰기 전에 진입점이 CANCELLED 로 실패하게 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancellation() throws Exception {
        final byte[][] files = samples();
        Thread.currentThread().interrupt();
        try {
            ImageAsserts.fails(MediaException.Kind.CANCELLED, new Block() {
                /**
                 * Decodes while the thread is interrupted.
                 * <p>
                 * 스레드가 인터럽트된 상태에서 디코딩한다.
                 *
                 * @throws Exception always MediaException / 항상 MediaException
                 */
                public void run() throws Exception {
                    ImageDecoders.decode(files[0], null, null);
                }
            }, "interrupted facade");
            Check.isTrue(Thread.currentThread().isInterrupted(), "the interrupt flag is still set");
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * Builds a PNG of the given size whose pixels are all transparent black, without allocating the sample arrays (the zero rows are fed
     * straight into the compressor).
     * <p>
     * 픽셀이 모두 투명한 검정인 주어진 크기의 PNG 를 만든다. 샘플 배열을 할당하지 않고 0 행을 압축기에 바로 넣는다.
     *
     * @param w width / 가로
     * @param h height / 세로
     * @return PNG bytes / PNG 바이트
     */
    private static byte[] blankPng(int w, int h) {
        java.util.zip.Deflater d = new java.util.zip.Deflater(1);
        java.io.ByteArrayOutputStream z = new java.io.ByteArrayOutputStream();
        byte[] row = new byte[1 + w * 4];
        byte[] buf = new byte[8192];
        // 행마다 0 으로 채운 행을 압축기에 넣고 나온 데이터를 모은다.
        for (int y = 0; y < h; y++) {
            d.setInput(row);
            // 각 항목을 차례로 처리한다.
            while (!d.needsInput()) {
                z.write(buf, 0, d.deflate(buf));
            }
        }
        d.finish();
        // 각 항목을 차례로 처리한다.
        while (!d.finished()) {
            z.write(buf, 0, d.deflate(buf));
        }
        d.end();
        Bytes b = new Bytes();
        b.put(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A });
        b.put(PngBuilder.chunk("IHDR", new Bytes().be32(w).be32(h).u8(8).u8(6).u8(0).u8(0).u8(0).toBytes()));
        b.put(PngBuilder.chunk("IDAT", z.toByteArray()));
        b.put(PngBuilder.chunk("IEND", new byte[0]));
        return b.toBytes();
    }

    /**
     * Large images decode in reasonable time (the times are printed), and an interrupt that arrives in the middle of a long decode stops it
     * with CANCELLED.
     * <p>
     * 큰 이미지가 적절한 시간에 디코딩되며(시간을 출력한다) 긴 디코딩 중간에 들어온 인터럽트가 CANCELLED 로 그것을 멈춘다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLargeImagesAndMidDecodeCancellation() throws Exception {
        long t0 = System.currentTimeMillis();
        DecodedImage big = ImageDecoders.decode(blankPng(3000, 2000), null, null);
        long t1 = System.currentTimeMillis();
        Check.equal(3000, big.getWidth(), "large PNG width");
        Check.equal(2000, big.getHeight(), "large PNG height");
        System.out.println("[info] 3000 x 2000 RGBA PNG decoded in " + (t1 - t0) + " ms (including building the file)");
        Check.isTrue(t1 - t0 < 60000, "a 6 megapixel PNG decodes within a minute even on a busy machine");
        final byte[] huge = blankPng(6000, 3000);
        final Object[] result = new Object[2];
        final boolean[] started = new boolean[1];
        Thread worker = new Thread(new Runnable() {
            /**
             * Decodes the large image and records the result.
             * <p>
             * 큰 이미지를 디코딩하고 결과를 기록한다.
             */
            public void run() {
                synchronized (started) {
                    started[0] = true;
                    started.notifyAll();
                }
                try {
                    result[0] = ImageDecoders.decode(huge, null, null);
                } catch (MediaException e) {
                    result[1] = e;
                }
            }
        });
        worker.start();
        synchronized (started) {
            // 작업 스레드가 시작할 때까지 기다린다.
            while (!started[0]) {
                started.wait(1000);
            }
        }
        Thread.sleep(30);
        worker.interrupt();
        worker.join(60000);
        Check.isFalse(worker.isAlive(), "the worker ended after the interrupt");
        if (result[0] != null) {
            Check.skip("the decode finished before the interrupt arrived (very fast machine)");
        }
        Check.equal(MediaException.Kind.CANCELLED, ((MediaException) result[1]).getKind(), "the interrupt stopped the decode");
    }

    /**
     * Optional decoders of other parts of the project: when their class exists it is registered and its first MIME type is supported; when
     * it does not exist the registry works without it.
     * <p>
     * 프로젝트의 다른 부분이 만든 선택적 디코더: 클래스가 있으면 등록되고 첫 MIME 타입이 지원되며 없으면 등록부는 그것 없이 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOptionalDecoders() throws Exception {
        String[] names = { "org.duckdns.hjow.swingwebpanel.media.image.webp.WebpDecoder", "org.duckdns.hjow.swingwebpanel.media.image.avif.AvifDecoder",
                "org.duckdns.hjow.swingwebpanel.media.image.svg.SvgDecoder" };
        int present = 0;
        // 클래스가 있는 선택적 디코더만 확인한다.
        for (String n : names) {
            try {
                Object o = Class.forName(n).newInstance();
                ImageDecoder d = (ImageDecoder) o;
                present++;
                Check.isTrue(ImageDecoders.isSupportedMime(d.getMimeTypes()[0]), n + " is registered");
            } catch (ClassNotFoundException absent) {
                Check.isTrue(ImageDecoders.isSupportedMime("image/png"), "the registry works without " + n);
            }
        }
        System.out.println("[info] optional image decoders present in this build: " + present + " of " + names.length);
    }
}
