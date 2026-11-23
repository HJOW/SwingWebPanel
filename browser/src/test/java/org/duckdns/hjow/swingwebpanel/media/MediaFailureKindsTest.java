package org.duckdns.hjow.swingwebpanel.media;

import java.io.UnsupportedEncodingException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The three kinds of media failures for every format (plan item P18-01; FEATURE_MATRIX IMG-*, AUD-*): "the file is not a known format"
 * (<code>UNSUPPORTED_FORMAT</code>), "the file claims a known format but is damaged" (<code>CORRUPT</code>), and "it is too large"
 * (<code>TOO_LARGE</code>); the fourth kind, <code>ACCESS_FAILED</code> (the resource could not be reached), comes from the network layer and
 * the sound output and is checked there (<code>AudioRobustnessIntegrationTest</code>, <code>MediaFallbackIntegrationTest</code>,
 * <code>MediaPlayerTest</code>). Each format is given the start of a file (its signature) followed by nonsense. The kind that comes out is
 * fixed per format and never an exception of another type: <code>CORRUPT</code> where the signature decides the format (PNG, GIF, JPEG,
 * ICO, WebP, AVIF, SVG, WAV, AIFF, AU, FLAC), <code>UNSUPPORTED_FORMAT</code> where the decoder claims a file only when its structure is
 * found (BMP, Ogg, MP3, MP4/AAC, WebM). Text that is no media at all (an HTML page that is served as an image or as audio, as happens when
 * a server answers with its error page) is an unsupported format, not damage. The bytes are the signatures of the formats, so the checks
 * do not depend on the decoders' own test files. For a page both end as the same <code>error</code> event when the resource is loaded; the
 * kinds matter for the message and for damage that appears after the start.
 * <p>
 * 모든 형식에 대한 미디어 실패의 세 종류(계획 P18-01; FEATURE_MATRIX IMG-*, AUD-*): "알려진 형식이 아니다"(<code>UNSUPPORTED_FORMAT</code>), "알려진
 * 형식이라고 주장하지만 손상되었다"(<code>CORRUPT</code>), "너무 크다"(<code>TOO_LARGE</code>). 네 번째 종류 <code>ACCESS_FAILED</code>(리소스에 닿지
 * 못함)는 네트워크 계층과 소리 출력에서 나오며 거기서 확인한다(<code>AudioRobustnessIntegrationTest</code>, <code>MediaFallbackIntegrationTest</code>,
 * <code>MediaPlayerTest</code>). 각 형식에 파일의 시작(시그니처)과 이어지는 엉터리 데이터를 준다. 나오는 종류는 형식마다 정해져 있고 다른 종류의
 * 예외가 아니다: 시그니처가 형식을 정하는 형식(PNG, GIF, JPEG, ICO, WebP, AVIF, SVG, WAV, AIFF, AU, FLAC)은 <code>CORRUPT</code>, 구조가 확인되어야 디코더가
 * 파일을 받아들이는 형식(BMP, Ogg, MP3, MP4/AAC, WebM)은 <code>UNSUPPORTED_FORMAT</code> 이다. 미디어가 전혀 아닌 글(서버가 오류 페이지로 응답할 때처럼
 * 이미지나 오디오로 제공된 HTML 페이지)은 손상이 아니라 지원하지 않는 형식이다. 바이트는 형식의 시그니처이므로 확인은 디코더 자체의 시험 파일에 의존하지
 * 않는다. 페이지에서는 리소스를 로드할 때 둘 다 같은 <code>error</code> 이벤트로 끝나며, 종류는 메시지와 시작 뒤에 나타나는 손상에서 의미가 있다.
 */
public class MediaFailureKindsTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MediaFailureKindsTest() {
    }

    /**
     * The nonsense that follows a signature.
     * <p>
     * 시그니처 뒤에 오는 엉터리 데이터.
     */
    private static final int[] NONSENSE = { 0xDE, 0xAD, 0xBE, 0xEF, 0x00, 0x01, 0x02, 0x03, 0xFF, 0xFE, 0x7F, 0x80, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88,
        0x99, 0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x20, 0x30, 0x40 };

    /**
     * Joins a signature and the nonsense.
     * <p>
     * 시그니처와 엉터리 데이터를 잇는다.
     *
     * @param signature the start of the file / 파일의 시작
     * @return the bytes / 바이트
     */
    private static byte[] signaturePlusNonsense(int... signature) {
        byte[] b = new byte[signature.length + NONSENSE.length];
        // 시그니처를 먼저 쓰고 엉터리 바이트를 이어 붙인다.
        for (int i = 0; i < signature.length; i++) {
            b[i] = (byte) signature[i];
        }
        // 엉터리 바이트를 시그니처 뒤에 이어 붙인다.
        for (int i = 0; i < NONSENSE.length; i++) {
            b[signature.length + i] = (byte) NONSENSE[i];
        }
        return b;
    }

    /**
     * Returns the ASCII bytes of a text.
     * <p>
     * 문자열의 ASCII 바이트를 반환한다.
     *
     * @param text the text / 문자열
     * @return the bytes / 바이트
     */
    private static byte[] ascii(String text) {
        try {
            return text.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the kind of the failure when an image is decoded, or null when decoding worked (or threw a different exception).
     * <p>
     * 이미지를 디코딩할 때 실패의 종류를 반환한다. 디코딩이 되었거나 다른 예외가 나면 null.
     *
     * @param data the bytes / 바이트
     * @param mime the declared MIME type, or null / 선언된 MIME 타입. 없으면 null
     * @return the kind, or a description of the surprise / 종류. 또는 뜻밖의 결과의 설명
     */
    private static String imageKind(byte[] data, String mime) {
        try {
            ImageDecoders.decode(data, mime, DecodeLimits.DEFAULT);
            return "decoded";
        } catch (MediaException e) {
            return e.getKind().toString();
        } catch (RuntimeException e) {
            return "RuntimeException " + e;
        }
    }

    /**
     * Returns the kind of the failure when audio is opened, with the same conventions as {@link #imageKind(byte[], String)}.
     * <p>
     * 오디오를 열 때 실패의 종류를 반환한다. {@link #imageKind(byte[], String)} 와 같은 규칙이다.
     *
     * @param data the bytes / 바이트
     * @param mime the declared MIME type, or null / 선언된 MIME 타입. 없으면 null
     * @return the kind, or a description of the surprise / 종류. 또는 뜻밖의 결과의 설명
     */
    private static String audioKind(byte[] data, String mime) {
        try {
            AudioDecoders.open(data, mime, DecodeLimits.DEFAULT).close();
            return "opened";
        } catch (MediaException e) {
            return e.getKind().toString();
        } catch (RuntimeException e) {
            return "RuntimeException " + e;
        }
    }

    /**
     * Every image format: a signature followed by nonsense is CORRUPT (the format is known, the file is damaged) under the declared type and
     * without any type, except BMP, whose two-letter signature is only accepted when the header makes sense (UNSUPPORTED_FORMAT).
     * <p>
     * 모든 이미지 형식: 시그니처 뒤에 엉터리 데이터가 오면 선언된 타입이 있든 없든 CORRUPT(형식은 알지만 파일이 손상됨)이다. 단 BMP 는 두 글자 시그니처를
     * 헤더가 말이 될 때만 받아들이므로 UNSUPPORTED_FORMAT 이다.
     */
    public void testImageSignatureWithNonsense() {
        Object[][] cases = {
            { "PNG", signaturePlusNonsense(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A), "image/png", "CORRUPT" },
            { "GIF", signaturePlusNonsense('G', 'I', 'F', '8', '9', 'a'), "image/gif", "CORRUPT" },
            { "JPEG", signaturePlusNonsense(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00), "image/jpeg", "CORRUPT" },
            // BMP 의 시그니처는 두 글자뿐이므로 디코더는 헤더가 말이 될 때만 파일을 자기 것으로 받아들인다. 엉터리 헤더는 BMP 가 아닌 것이 된다.
            { "BMP", signaturePlusNonsense('B', 'M'), "image/bmp", "UNSUPPORTED_FORMAT" },
            { "ICO", signaturePlusNonsense(0x00, 0x00, 0x01, 0x00, 0x01, 0x00), "image/x-icon", "CORRUPT" },
            { "WebP", signaturePlusNonsense('R', 'I', 'F', 'F', 0x20, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P'), "image/webp", "CORRUPT" },
            { "AVIF", signaturePlusNonsense(0x00, 0x00, 0x00, 0x1C, 'f', 't', 'y', 'p', 'a', 'v', 'i', 'f', 0x00, 0x00, 0x00, 0x00), "image/avif", "CORRUPT" },
            { "SVG", ascii("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><rect width=\"5\" height=\"5\" fill=\"red\" <<<"), "image/svg+xml", "CORRUPT" },
        };
        StringBuilder problems = new StringBuilder();
        // 형식마다 선언된 타입이 있을 때와 없을 때 같은 종류로 실패하는지 확인한다.
        for (Object[] c : cases) {
            String name = (String) c[0];
            byte[] data = (byte[]) c[1];
            String expected = (String) c[3];
            String withType = imageKind(data, (String) c[2]);
            String without = imageKind(data, null);
            if (!expected.equals(withType)) {
                problems.append(name).append(" with its type: ").append(withType).append(" (expected ").append(expected).append("); ");
            }
            if (!expected.equals(without)) {
                problems.append(name).append(" without a type: ").append(without).append(" (expected ").append(expected).append("); ");
            }
        }
        Check.equal("", problems.toString(), "the kind of the failure for a known signature with nonsense behind it");
    }

    /**
     * Every audio format: for the formats that the signature decides (WAV, AIFF, AU, FLAC) a signature followed by nonsense is CORRUPT; for
     * the formats that are accepted only when their inner structure is found (Ogg, MP3, MP4, AAC, WebM) it is UNSUPPORTED_FORMAT; with or
     * without a declared type alike.
     * <p>
     * 모든 오디오 형식: 시그니처가 형식을 정하는 형식(WAV, AIFF, AU, FLAC)은 시그니처 뒤에 엉터리 데이터가 오면 CORRUPT 이고, 안쪽 구조가 확인되어야
     * 받아들이는 형식(Ogg, MP3, MP4, AAC, WebM)은 UNSUPPORTED_FORMAT 이다. 선언된 타입이 있든 없든 같다.
     */
    public void testAudioSignatureWithNonsense() {
        Object[][] cases = {
            // 시그니처가 형식을 정하는 컨테이너(WAV, AIFF, AU, FLAC)는 손상된 내용을 CORRUPT 로 보고한다.
            { "WAV", signaturePlusNonsense('R', 'I', 'F', 'F', 0x20, 0x00, 0x00, 0x00, 'W', 'A', 'V', 'E'), "audio/wav", "CORRUPT" },
            { "AIFF", signaturePlusNonsense('F', 'O', 'R', 'M', 0x00, 0x00, 0x00, 0x20, 'A', 'I', 'F', 'F'), "audio/aiff", "CORRUPT" },
            { "AU", signaturePlusNonsense('.', 's', 'n', 'd'), "audio/basic", "CORRUPT" },
            { "FLAC", signaturePlusNonsense('f', 'L', 'a', 'C'), "audio/flac", "CORRUPT" },
            // 그 안의 코덱이나 프레임 구조가 확인되어야 파일을 받아들이는 형식(Ogg, MP3, MP4, AAC ADTS, WebM)은 앞부분이 엉터리이면 그 형식으로 인정되지 않는다.
            { "Ogg", signaturePlusNonsense('O', 'g', 'g', 'S', 0x00, 0x02), "audio/ogg", "UNSUPPORTED_FORMAT" },
            { "MP3 with ID3", signaturePlusNonsense('I', 'D', '3', 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10), "audio/mpeg", "UNSUPPORTED_FORMAT" },
            { "MP3 frame", signaturePlusNonsense(0xFF, 0xFB, 0x90, 0x64), "audio/mpeg", "UNSUPPORTED_FORMAT" },
            { "MP4", signaturePlusNonsense(0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'M', '4', 'A', ' ', 0x00, 0x00, 0x00, 0x00), "audio/mp4", "UNSUPPORTED_FORMAT" },
            { "AAC ADTS", signaturePlusNonsense(0xFF, 0xF1, 0x50, 0x80), "audio/aac", "UNSUPPORTED_FORMAT" },
            { "WebM", signaturePlusNonsense(0x1A, 0x45, 0xDF, 0xA3), "audio/webm", "UNSUPPORTED_FORMAT" },
        };
        StringBuilder problems = new StringBuilder();
        // 형식마다 선언된 타입이 있을 때와 없을 때 같은 종류로 실패하는지 확인한다.
        for (Object[] c : cases) {
            String name = (String) c[0];
            byte[] data = (byte[]) c[1];
            String expected = (String) c[3];
            String withType = audioKind(data, (String) c[2]);
            String without = audioKind(data, null);
            if (!expected.equals(withType)) {
                problems.append(name).append(" with its type: ").append(withType).append(" (expected ").append(expected).append("); ");
            }
            if (!expected.equals(without)) {
                problems.append(name).append(" without a type: ").append(without).append(" (expected ").append(expected).append("); ");
            }
        }
        Check.equal("", problems.toString(), "the kind of the failure for a known signature with nonsense behind it");
    }

    /**
     * Text that is no media, as a server answers with an error page, is an unsupported format and not damage, for images and for audio,
     * with every declared type; an empty file is damage (there is nothing to decode).
     * <p>
     * 서버가 오류 페이지로 응답할 때처럼 미디어가 아닌 글은 모든 선언 타입에서, 이미지와 오디오 모두 손상이 아니라 지원하지 않는 형식이다. 빈 파일은
     * 손상이다(디코딩할 것이 없다).
     */
    public void testTextAndEmptyFiles() {
        byte[] page = ascii("<!doctype html><html><body><h1>404 Not Found</h1></body></html>");
        String[] imageTypes = { null, "image/png", "image/gif", "image/jpeg", "image/bmp", "image/webp", "image/avif", "image/x-icon" };
        StringBuilder problems = new StringBuilder();
        // 모든 선언된 타입에서 HTML 이 지원하지 않는 형식으로 보고되는지 확인한다.
        for (String type : imageTypes) {
            String kind = imageKind(page, type);
            if (!"UNSUPPORTED_FORMAT".equals(kind)) {
                problems.append("image ").append(type).append(": ").append(kind).append("; ");
            }
        }
        String[] audioTypes = { null, "audio/wav", "audio/mpeg", "audio/ogg", "audio/flac", "audio/mp4", "audio/aac", "audio/webm", "audio/basic", "audio/aiff" };
        // 오디오도 같은 방식으로 확인한다.
        for (String type : audioTypes) {
            String kind = audioKind(page, type);
            if (!"UNSUPPORTED_FORMAT".equals(kind)) {
                problems.append("audio ").append(type).append(": ").append(kind).append("; ");
            }
        }
        Check.equal("", problems.toString(), "an HTML page is an unsupported format for every declared type");
        Check.equal("CORRUPT", imageKind(new byte[0], "image/png"), "an empty image file");
        Check.equal("CORRUPT", audioKind(new byte[0], "audio/wav"), "an empty audio file");
        Check.equal("CORRUPT", imageKind(null, null), "no image bytes");
        Check.equal("CORRUPT", audioKind(null, null), "no audio bytes");
    }

    /**
     * A file over the byte limit is TOO_LARGE before any decoder looks at it, for images and for audio; the limit applies to every format
     * (it is checked by the registry), and a file within the limit is not reported as too large.
     * <p>
     * 바이트 제한을 넘는 파일은 어떤 디코더가 보기 전에 이미지와 오디오 모두 TOO_LARGE 이다. 제한은 모든 형식에 적용되며(등록부가 확인한다) 제한 안의
     * 파일은 너무 크다고 보고되지 않는다.
     */
    public void testByteLimitIsTooLarge() {
        DecodeLimits tiny = new DecodeLimits(DecodeLimits.DEFAULT_MAX_PIXELS, 16, DecodeLimits.DEFAULT_MAX_FRAMES, DecodeLimits.DEFAULT_MAX_TOTAL_PIXELS);
        byte[] png = signaturePlusNonsense(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
        byte[] wav = signaturePlusNonsense('R', 'I', 'F', 'F', 0x20, 0x00, 0x00, 0x00, 'W', 'A', 'V', 'E');
        try {
            ImageDecoders.decode(png, "image/png", tiny);
            Check.fail("an image over the byte limit was decoded");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.TOO_LARGE, e.getKind(), "an image over the byte limit");
        }
        try {
            AudioDecoders.open(wav, "audio/wav", tiny);
            Check.fail("audio over the byte limit was opened");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.TOO_LARGE, e.getKind(), "audio over the byte limit");
        }
        Check.isTrue(!"TOO_LARGE".equals(imageKind(png, "image/png")), "a file within the default limit is not too large");
        Check.isTrue(!"TOO_LARGE".equals(audioKind(wav, "audio/wav")), "audio within the default limit is not too large");
    }

    /**
     * The four kinds are different values and each has a description, so that a page can show the reason (access failure, unsupported
     * format, damage, size).
     * <p>
     * 종류는 모두 서로 다른 값이며 각각 이름이 있으므로 페이지가 이유(접근 실패, 지원하지 않는 형식, 손상, 크기)를 보여 줄 수 있다.
     */
    public void testKindsAreDistinct() {
        MediaException.Kind[] kinds = MediaException.Kind.values();
        java.util.Set<String> names = new java.util.HashSet<String>();
        // 종류의 이름이 모두 다른지 확인한다.
        for (MediaException.Kind k : kinds) {
            Check.isTrue(names.add(k.toString()), "a distinct kind: " + k);
        }
        Check.isTrue(names.contains("ACCESS_FAILED") && names.contains("UNSUPPORTED_FORMAT") && names.contains("CORRUPT") && names.contains("TOO_LARGE")
                && names.contains("CANCELLED"), "the kinds that the plan names: " + names);
    }
}
