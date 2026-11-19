package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.ImageDecoder;

/**
 * The decoder of SVG images (plan item P18-04; FEATURE_MATRIX IMG-07): the registry {@link org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders} loads this
 * class by name. It recognizes an SVG document by its first bytes (an optional byte order mark, an XML declaration, comments or DOCTYPE and
 * then an <code>svg</code> start tag; gzip compressed <code>svgz</code> data whose content starts like that is accepted too), parses it with
 * {@link SvgImage} and rasterizes it at the natural size of the document with Java2D. The result is a single non-animated frame. Use
 * {@link SvgImage#parse} directly to draw the same document at other sizes without losing sharpness. External resources are never loaded
 * and scripts never run.
 * <p>
 * SVG 이미지의 디코더(계획 P18-04; FEATURE_MATRIX IMG-07). 등록부 {@link org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders} 가 이 클래스를 이름으로 읽어 온다.
 * 첫 바이트(선택적 바이트 순서 표시, XML 선언, 주석이나 DOCTYPE, 그리고 <code>svg</code> 시작 태그)로 SVG 문서를 알아보며(내용이 그렇게 시작하는 gzip
 * 압축 <code>svgz</code> 도 받는다), {@link SvgImage} 로 해석해 문서의 자연 크기로 Java2D 로 래스터화한다. 결과는 애니메이션 없는 프레임 하나이다.
 * 같은 문서를 선명도를 잃지 않고 다른 크기로 그리려면 {@link SvgImage#parse} 를 직접 쓴다. 외부 자원은 읽지 않고 스크립트는 실행하지 않는다.
 */
public final class SvgDecoder implements ImageDecoder {

    /**
     * Creates the decoder (the registry needs a public constructor without arguments).
     * <p>
     * 디코더를 만든다(등록부는 인수 없는 공개 생성자를 필요로 한다).
     */
    public SvgDecoder() {
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return <code>svg</code>
     */
    public String getFormatName() {
        return "svg";
    }

    /**
     * Returns the MIME types.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return <code>image/svg+xml</code>
     */
    public String[] getMimeTypes() {
        return new String[] { "image/svg+xml" };
    }

    /**
     * Tells whether the first bytes look like an SVG document (or a gzip compressed one).
     * <p>
     * 첫 바이트가 SVG 문서(또는 gzip 으로 압축된 문서)처럼 보이는지 알려 준다.
     *
     * @param head the first bytes / 첫 바이트
     * @return true for a probable SVG / SVG 로 보이면 true
     */
    public boolean accepts(byte[] head) {
        if (head == null || head.length < 4) {
            return false;
        }
        byte[] h = head;
        if ((h[0] & 0xFF) == 0x1F && (h[1] & 0xFF) == 0x8B) {
            h = gunzipHead(head);
            if (h == null) {
                return false;
            }
        }
        return looksLikeSvg(SvgXmlParser.decode(h));
    }

    /**
     * Decompresses the first bytes of gzip data (a truncated stream is fine).
     * <p>
     * gzip 데이터의 첫 바이트를 푼다(잘린 스트림이어도 된다).
     *
     * @param head compressed bytes / 압축된 바이트
     * @return up to 2048 decompressed bytes, or null if nothing could be read / 최대 2048바이트의 푼 데이터. 읽지 못하면 null
     */
    private static byte[] gunzipHead(byte[] head) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(head));
            byte[] buf = new byte[512];
            int n;
            // 잘린 스트림이므로 읽을 수 있는 만큼(최대 2048바이트)만 읽는다.
            while (out.size() < 2048 && (n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        } catch (IOException e) {
            // 압축 데이터가 잘려 끝난 경우에도 앞부분은 쓸 수 있다.
            if (out.size() == 0) {
                return null;
            }
        }
        return out.size() == 0 ? null : out.toByteArray();
    }

    /**
     * Checks that the text begins like an SVG document: a prolog (white space, comments, processing instructions, DOCTYPE) then
     * <code>&lt;svg</code> (with an optional namespace prefix).
     * <p>
     * 텍스트가 SVG 문서처럼 시작하는지 확인한다: 머리말(공백, 주석, 처리 명령, DOCTYPE) 뒤에 <code>&lt;svg</code>(네임스페이스 접두사 가능).
     *
     * @param s the decoded start of the data / 데이터 앞부분의 디코딩 결과
     * @return true if it looks like SVG / SVG 로 보이면 true
     */
    private static boolean looksLikeSvg(String s) {
        int i = 0;
        int n = s.length();
        // 머리말 항목을 건너뛰며 첫 요소 시작을 찾는다.
        while (i < n) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == (char) 0xFEFF) {
                i++;
            } else if (s.startsWith("<?", i)) {
                int e = s.indexOf("?>", i);
                if (e < 0) {
                    return false;
                }
                i = e + 2;
            } else if (s.startsWith("<!--", i)) {
                int e = s.indexOf("-->", i);
                if (e < 0) {
                    return false;
                }
                i = e + 3;
            } else if (s.startsWith("<!DOCTYPE", i) || s.startsWith("<!doctype", i)) {
                int e = skipDoctype(s, i);
                if (e < 0) {
                    // 머리 부분(1024바이트)에서 DOCTYPE 이 끝나지 않으면 이름으로 판단한다.
                    return s.regionMatches(true, i + 9, " svg", 0, 4) || s.regionMatches(true, i + 9, "\tsvg", 0, 4)
                            || s.regionMatches(true, i + 9, "\nsvg", 0, 4);
                }
                i = e;
            } else if (c == '<') {
                int j = i + 1;
                // 요소 이름(접두사 포함)을 읽고 지역 이름이 svg 인지 본다.
                while (j < n && s.charAt(j) != ' ' && s.charAt(j) != '>' && s.charAt(j) != '/' && s.charAt(j) != '\t' && s.charAt(j) != '\n'
                        && s.charAt(j) != '\r') {
                    j++;
                }
                String name = s.substring(i + 1, j);
                int colon = name.indexOf(':');
                return (colon < 0 ? name : name.substring(colon + 1)).equals("svg");
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * Returns the index after a DOCTYPE declaration starting at a position, or -1 if it does not end in the text.
     * <p>
     * 위치에서 시작하는 DOCTYPE 선언 뒤의 위치를 반환한다. 텍스트 안에서 끝나지 않으면 -1.
     *
     * @param s     the text / 텍스트
     * @param start the index of <code>&lt;!DOCTYPE</code> / <code>&lt;!DOCTYPE</code> 의 위치
     * @return the index after the closing <code>&gt;</code> or -1 / 닫는 <code>&gt;</code> 뒤의 위치. 없으면 -1
     */
    private static int skipDoctype(String s, int start) {
        int i = start + 9;
        int bracket = 0;
        // 내부 부분집합의 대괄호 깊이를 세며 선언 끝 '>' 를 찾는다.
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                int e = s.indexOf(c, i + 1);
                if (e < 0) {
                    return -1;
                }
                i = e + 1;
                continue;
            }
            if (c == '[') {
                bracket++;
            } else if (c == ']') {
                bracket--;
            } else if (c == '>' && bracket <= 0) {
                return i + 1;
            }
            i++;
        }
        return -1;
    }

    /**
     * Decodes SVG data into a still image at the natural size of the document.
     * <p>
     * SVG 데이터를 문서의 자연 크기의 정지 이미지로 디코딩한다.
     *
     * @param data   the SVG document (optionally gzip compressed) / SVG 문서(gzip 압축 가능)
     * @param limits the limits / 제한
     * @return the rasterized image / 래스터화한 이미지
     * @throws MediaException CORRUPT for malformed data, TOO_LARGE over a limit, CANCELLED on interrupt / 손상은 CORRUPT, 제한 초과는 TOO_LARGE, 인터럽트는 CANCELLED
     */
    public DecodedImage decode(byte[] data, DecodeLimits limits) throws MediaException {
        DecodeLimits lim = limits == null ? DecodeLimits.DEFAULT : limits;
        byte[] raw = data;
        if (data.length >= 2 && (data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B) {
            raw = gunzip(data, lim);
        }
        return SvgImage.parse(raw, lim).toDecodedImage();
    }

    /**
     * Decompresses gzip data with a limit on the decompressed size.
     * <p>
     * gzip 데이터를 푼 크기 제한 아래에서 푼다.
     *
     * @param data compressed data / 압축 데이터
     * @param lim  limits (the decompressed size may not exceed the byte limit) / 제한(푼 크기는 바이트 제한을 넘을 수 없다)
     * @return decompressed bytes / 푼 바이트
     * @throws MediaException CORRUPT for damaged data, TOO_LARGE for a decompression bomb / 손상은 CORRUPT, 압축 폭탄은 TOO_LARGE
     */
    private static byte[] gunzip(byte[] data, DecodeLimits lim) throws MediaException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = null;
        try {
            in = new GZIPInputStream(new ByteArrayInputStream(data));
            byte[] buf = new byte[8192];
            int n;
            // 푼 크기가 제한을 넘지 않는 동안 읽는다.
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > lim.getMaxBytes()) {
                    throw new MediaException(MediaException.Kind.TOO_LARGE, "the decompressed SVG is too large");
                }
            }
        } catch (IOException e) {
            throw new MediaException(MediaException.Kind.CORRUPT, "damaged gzip data: " + e, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // 닫기 실패는 결과에 영향이 없다.
                    in = null;
                }
            }
        }
        return out.toByteArray();
    }
}
