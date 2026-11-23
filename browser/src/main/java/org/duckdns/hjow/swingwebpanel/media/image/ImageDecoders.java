package org.duckdns.hjow.swingwebpanel.media.image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.MediaTypes;

/**
 * The registry and entry point of all image decoders (plan items P18-01, P18-02, P18-03). {@link #decode} looks at the first bytes of the
 * data (file signature) to choose the decoder; a MIME type given by the caller is only a hint that breaks ties between decoders that accept the
 * same bytes, so a wrong extension or Content-Type does not decide the format. The built-in decoders are PNG/APNG, GIF, JPEG, BMP and ICO/CUR.
 * Decoders written elsewhere are found by name through reflection when their class is present: <code>org.duckdns.hjow.swingwebpanel.media.image.webp.WebpDecoder</code>,
 * <code>org.duckdns.hjow.swingwebpanel.media.image.avif.AvifDecoder</code> and <code>org.duckdns.hjow.swingwebpanel.media.image.svg.SvgDecoder</code>; a missing or broken class
 * is skipped silently. More decoders can be added with {@link #register}. All methods are thread-safe.
 * <p>
 * 모든 이미지 디코더의 등록부이자 진입점(계획 P18-01, P18-02, P18-03). {@link #decode} 는 데이터의 첫 바이트(파일 시그니처)로 디코더를 고르며,
 * 호출자가 준 MIME 타입은 같은 바이트를 받아들이는 디코더가 여럿일 때 승부를 가르는 힌트로만 쓰므로 잘못된 확장자나 Content-Type 이 형식을 결정하지
 * 못한다. 내장 디코더는 PNG/APNG, GIF, JPEG, BMP, ICO/CUR 이다. 다른 곳에서 만든 디코더는 클래스가 있으면 이름으로 리플렉션해 찾는다:
 * <code>org.duckdns.hjow.swingwebpanel.media.image.webp.WebpDecoder</code>, <code>org.duckdns.hjow.swingwebpanel.media.image.avif.AvifDecoder</code>,
 * <code>org.duckdns.hjow.swingwebpanel.media.image.svg.SvgDecoder</code>. 클래스가 없거나 깨져 있으면 조용히 건너뛴다. {@link #register} 로 디코더를 더 추가할 수 있다.
 * 모든 메서드는 스레드 안전하다.
 */
public final class ImageDecoders {

    /**
     * Number of leading bytes handed to {@link ImageDecoder#accepts}.
     * <p>
     * {@link ImageDecoder#accepts} 에 넘기는 앞부분 바이트 수.
     */
    private static final int HEAD_SIZE = 1024;

    /**
     * Names of optional decoder classes written by other parts of the project.
     * <p>
     * 프로젝트의 다른 부분이 작성한 선택적 디코더 클래스의 이름.
     */
    private static final String[] OPTIONAL_DECODERS = { "org.duckdns.hjow.swingwebpanel.media.image.webp.WebpDecoder",
            "org.duckdns.hjow.swingwebpanel.media.image.avif.AvifDecoder", "org.duckdns.hjow.swingwebpanel.media.image.svg.SvgDecoder" };

    /**
     * The registered decoders in the order of registration.
     * <p>
     * 등록된 디코더(등록 순서).
     */
    private static final List<ImageDecoder> DECODERS = new CopyOnWriteArrayList<ImageDecoder>();

    static {
        DECODERS.add(new PngDecoder());
        DECODERS.add(new GifDecoder());
        DECODERS.add(new JpegDecoder());
        DECODERS.add(new BmpDecoder());
        DECODERS.add(new IcoDecoder());
        // 선택적 디코더는 클래스가 있을 때만 이름으로 찾아 등록한다.
        for (int i = 0; i < OPTIONAL_DECODERS.length; i++) {
            try {
                Object o = Class.forName(OPTIONAL_DECODERS[i]).newInstance();
                if (o instanceof ImageDecoder) {
                    DECODERS.add((ImageDecoder) o);
                }
            } catch (ClassNotFoundException e) {
                // 아직 없는 디코더이다.
                continue;
            } catch (LinkageError e) {
                // 일부만 컴파일된 디코더는 건너뛴다.
                continue;
            } catch (InstantiationException e) {
                continue;
            } catch (IllegalAccessException e) {
                continue;
            } catch (RuntimeException e) {
                continue;
            }
        }
    }

    /**
     * Private constructor: static facade.
     * <p>
     * 비공개 생성자: 정적 진입점이다.
     */
    private ImageDecoders() {
    }

    /**
     * Adds a decoder to the registry. It is tried after the decoders that are already registered.
     * <p>
     * 등록부에 디코더를 추가한다. 이미 등록된 디코더 다음에 시도된다.
     *
     * @param decoder the decoder, not null / 디코더. null 이 아니어야 한다
     */
    public static void register(ImageDecoder decoder) {
        if (decoder == null) {
            throw new IllegalArgumentException("decoder must not be null");
        }
        DECODERS.add(decoder);
    }

    /**
     * Returns the decoder whose file signature matches the first bytes, or null.
     * <p>
     * 첫 바이트의 파일 시그니처와 맞는 디코더를 반환하며 없으면 null 이다.
     *
     * @param head first bytes of the data (up to 1024 are used) / 데이터의 첫 바이트(최대 1024바이트를 쓴다)
     * @return the decoder or null / 디코더 또는 null
     */
    public static ImageDecoder find(byte[] head) {
        return pick(head, null);
    }

    /**
     * Chooses a decoder: the signature decides; among several matches the one that lists the hinted MIME type wins, otherwise the first.
     * <p>
     * 디코더를 고른다: 시그니처가 결정하며 여럿이 맞으면 힌트 MIME 타입을 가진 것이, 없으면 첫 번째가 이긴다.
     *
     * @param data data or its first bytes / 데이터 또는 그 앞부분
     * @param mime normalized MIME hint or null / 정규화된 MIME 힌트 또는 null
     * @return the decoder or null / 디코더 또는 null
     */
    private static ImageDecoder pick(byte[] data, String mime) {
        if (data == null || data.length == 0) {
            return null;
        }
        byte[] head = data.length > HEAD_SIZE ? Arrays.copyOf(data, HEAD_SIZE) : data;
        ImageDecoder first = null;
        // 시그니처가 맞는 디코더를 모으고 힌트와 같은 형식이 있으면 그것을 고른다.
        for (ImageDecoder d : DECODERS) {
            boolean ok;
            try {
                ok = d.accepts(head);
            } catch (RuntimeException e) {
                ok = false;
            }
            if (ok) {
                if (mime != null && Arrays.asList(d.getMimeTypes()).contains(mime)) {
                    return d;
                }
                if (first == null) {
                    first = d;
                }
            }
        }
        return first;
    }

    /**
     * Decodes image data. The format is found from the data, not from the MIME type.
     * <p>
     * 이미지 데이터를 디코딩한다. 형식은 MIME 타입이 아니라 데이터에서 찾는다.
     *
     * @param data     the encoded data / 인코딩된 데이터
     * @param mimeHint the Content-Type of the resource (parameters allowed) or null; only a hint / 리소스의 Content-Type(매개변수 허용) 또는 null. 힌트일 뿐이다
     * @param limits   the limits, or null for {@link DecodeLimits#DEFAULT} / 제한. null 이면 {@link DecodeLimits#DEFAULT}
     * @return the decoded image / 디코딩된 이미지
     * @throws MediaException CORRUPT for empty or damaged data, UNSUPPORTED_FORMAT for an unknown signature, TOO_LARGE over a limit, CANCELLED if the thread is interrupted / 빈 데이터나 손상은 CORRUPT, 알 수 없는 시그니처는 UNSUPPORTED_FORMAT, 제한 초과는 TOO_LARGE, 스레드가 인터럽트되면 CANCELLED
     */
    public static DecodedImage decode(byte[] data, String mimeHint, DecodeLimits limits) throws MediaException {
        DecodeLimits lim = limits == null ? DecodeLimits.DEFAULT : limits;
        if (data == null || data.length == 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the image data is empty");
        }
        if (data.length > lim.getMaxBytes()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "the image data is larger than " + lim.getMaxBytes() + " bytes");
        }
        PixelOps.checkCancelled();
        ImageDecoder decoder = pick(data, MediaTypes.normalize(mimeHint));
        if (decoder == null) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unknown image format");
        }
        DecodedImage img;
        try {
            img = decoder.decode(data, lim);
        } catch (RuntimeException e) {
            throw new MediaException(MediaException.Kind.CORRUPT, "damaged " + decoder.getFormatName() + " data: " + e, e);
        } catch (OutOfMemoryError e) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "not enough memory to decode the image");
        }
        // 어떤 디코더가 돌려준 결과든 제한을 넘지 않는지 한 번 더 확인한다.
        lim.checkImageSize(img.getWidth(), img.getHeight());
        if (img.getFrames().size() > lim.getMaxFrames()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "the image has too many frames");
        }
        return img;
    }

    /**
     * Tells whether a MIME type is decoded by one of the registered decoders (parameters and letter case are ignored).
     * <p>
     * MIME 타입을 등록된 디코더 중 하나가 디코딩하는지 알려 준다(매개변수와 대소문자는 무시한다).
     *
     * @param mime a Content-Type value / Content-Type 값
     * @return true if supported / 지원하면 true
     */
    public static boolean isSupportedMime(String mime) {
        String m = MediaTypes.normalize(mime);
        if (m == null) {
            return false;
        }
        // 등록된 디코더의 MIME 목록에서 찾는다.
        for (ImageDecoder d : DECODERS) {
            if (Arrays.asList(d.getMimeTypes()).contains(m)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the MIME types of all registered decoders without duplicates, in registration order.
     * <p>
     * 등록된 모든 디코더의 MIME 타입을 중복 없이 등록 순서대로 반환한다.
     *
     * @return unmodifiable list of MIME types / 수정할 수 없는 MIME 타입 목록
     */
    public static List<String> getSupportedMimeTypes() {
        Set<String> all = new LinkedHashSet<String>();
        // 디코더마다 MIME 타입을 모은다.
        for (ImageDecoder d : DECODERS) {
            all.addAll(Arrays.asList(d.getMimeTypes()));
        }
        return Collections.unmodifiableList(new ArrayList<String>(all));
    }

    /**
     * Returns a value for the <code>Accept</code> request header of image requests, built from the canonical MIME type (the first one) of each
     * registered decoder, for example <code>image/png,image/gif,image/jpeg,image/bmp,image/x-icon,*&#47;*;q=0.8</code>.
     * <p>
     * 이미지 요청의 <code>Accept</code> 헤더 값을 반환한다. 등록된 디코더마다 대표 MIME 타입(첫 번째)으로 만든다.
     * 예: <code>image/png,image/gif,image/jpeg,image/bmp,image/x-icon,*&#47;*;q=0.8</code>.
     *
     * @return header value / 헤더 값
     */
    public static String getAcceptHeader() {
        StringBuilder sb = new StringBuilder();
        // 디코더마다 대표 MIME 타입 하나를 이어 붙인다.
        for (ImageDecoder d : DECODERS) {
            String[] t = d.getMimeTypes();
            if (t.length > 0) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(t[0]);
            }
        }
        sb.append(",*/*;q=0.8");
        return sb.toString();
    }
}
