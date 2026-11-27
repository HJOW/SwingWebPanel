package org.duckdns.hjow.swingwebpanel.tls13.x509;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * One element (tag, length, value) of a DER encoding, as certificates use it (ITU-T X.690). Reading is strict: one-byte tags, definite
 * and minimal lengths, and every element must lie inside its parent. An element does not copy the data; it points into the array.
 * Example:
 * <pre>
 * Der cert = Der.readExactly(bytes, 0, bytes.length);
 * List&lt;Der&gt; parts = cert.expect(Der.SEQUENCE).children(3);
 * </pre>
 * <p>
 * 인증서가 쓰는 DER 부호화(ITU-T X.690)의 원소(태그, 길이, 값) 하나. 엄격하게 읽는다: 한 바이트 태그, 확정적이고 최소인 길이, 모든 원소는 부모 안에
 * 있어야 한다. 원소는 데이터를 복사하지 않고 배열 안을 가리킨다.
 */
public final class Der {

    /** Tag BOOLEAN. / BOOLEAN 태그. */
    public static final int BOOLEAN = 0x01;

    /** Tag INTEGER. / INTEGER 태그. */
    public static final int INTEGER = 0x02;

    /** Tag BIT STRING. / BIT STRING 태그. */
    public static final int BIT_STRING = 0x03;

    /** Tag OCTET STRING. / OCTET STRING 태그. */
    public static final int OCTET_STRING = 0x04;

    /** Tag NULL. / NULL 태그. */
    public static final int NULL = 0x05;

    /** Tag OBJECT IDENTIFIER. / OBJECT IDENTIFIER 태그. */
    public static final int OID = 0x06;

    /** Tag UTCTime. / UTCTime 태그. */
    public static final int UTC_TIME = 0x17;

    /** Tag GeneralizedTime. / GeneralizedTime 태그. */
    public static final int GENERALIZED_TIME = 0x18;

    /** Tag SEQUENCE (constructed). / SEQUENCE 태그(구성형). */
    public static final int SEQUENCE = 0x30;

    /** Tag SET (constructed). / SET 태그(구성형). */
    public static final int SET = 0x31;

    /** The array that holds the encoding. / 부호화가 든 배열. */
    private final byte[] buf;

    /** The tag byte. / 태그 바이트. */
    private final int tag;

    /** The position of the tag byte. / 태그 바이트의 위치. */
    private final int tlvStart;

    /** The position of the first content byte. / 내용 첫 바이트의 위치. */
    private final int start;

    /** The position after the last content byte. / 내용 마지막 바이트 다음 위치. */
    private final int end;

    /**
     * Creates an element.
     * <p>
     * 원소를 만든다.
     *
     * @param buf      the array / 배열
     * @param tag      the tag / 태그
     * @param tlvStart the position of the tag / 태그 위치
     * @param start    the start of the content / 내용의 시작
     * @param end      the end of the content / 내용의 끝
     */
    private Der(byte[] buf, int tag, int tlvStart, int start, int end) {
        this.buf = buf;
        this.tag = tag;
        this.tlvStart = tlvStart;
        this.start = start;
        this.end = end;
    }

    /**
     * Creates the exception for a malformed encoding.
     * <p>
     * 잘못된 부호화에 대한 예외를 만든다.
     *
     * @param message the detail / 상세 설명
     * @return the exception / 예외
     */
    static X509Exception malformed(String message) {
        return new X509Exception(X509Exception.Reason.MALFORMED, message);
    }

    /**
     * Reads the element that starts at a position.
     * <p>
     * 그 위치에서 시작하는 원소를 읽는다.
     *
     * @param buf   the array / 배열
     * @param off   the position of the tag / 태그 위치
     * @param limit the position that the element must not pass / 원소가 넘으면 안 되는 위치
     * @return the element / 원소
     * @throws X509Exception if the encoding is not strict DER or passes the limit / 부호화가 엄격한 DER 이 아니거나 한계를 넘으면
     */
    public static Der read(byte[] buf, int off, int limit) throws X509Exception {
        if (off < 0 || limit > buf.length || limit - off < 2) {
            throw malformed("truncated element");
        }
        int tag = buf[off] & 0xff;
        if ((tag & 0x1f) == 0x1f) {
            throw malformed("multi-byte tag");
        }
        int first = buf[off + 1] & 0xff;
        int at = off + 2;
        int len;
        if (first < 0x80) {
            len = first;
        } else {
            int count = first & 0x7f;
            // 긴 형식: 길이 옥텟은 1~4개이고 맨 앞이 0 이면 안 되며 짧은 형식으로 쓸 수 있는 값이면 안 된다.
            if (count == 0 || count > 4 || limit - at < count || buf[at] == 0) {
                throw malformed("bad length");
            }
            long value = 0;
            // 길이 옥텟을 빅 엔디언으로 모은다.
            for (int i = 0; i < count; i++) {
                value = (value << 8) | (buf[at + i] & 0xff);
            }
            at += count;
            if (value < 0x80 || value > Integer.MAX_VALUE) {
                throw malformed("length is not minimal or too large");
            }
            len = (int) value;
        }
        if (len > limit - at) {
            throw malformed("element is longer than its parent");
        }
        return new Der(buf, tag, off, at, at + len);
    }

    /**
     * Reads an element that must fill a range exactly.
     * <p>
     * 범위를 정확히 채워야 하는 원소를 읽는다.
     *
     * @param buf   the array / 배열
     * @param off   the start of the range / 범위의 시작
     * @param limit the end of the range / 범위의 끝
     * @return the element / 원소
     * @throws X509Exception if the encoding is wrong or data follows the element / 부호화가 틀리거나 원소 뒤에 데이터가 있으면
     */
    public static Der readExactly(byte[] buf, int off, int limit) throws X509Exception {
        Der d = read(buf, off, limit);
        if (d.end != limit) {
            throw malformed("trailing data");
        }
        return d;
    }

    /**
     * Returns the tag byte.
     * <p>
     * 태그 바이트를 반환한다.
     *
     * @return the tag / 태그
     */
    public int tag() {
        return tag;
    }

    /**
     * Returns the length of the content.
     * <p>
     * 내용의 길이를 반환한다.
     *
     * @return the length / 길이
     */
    public int length() {
        return end - start;
    }

    /**
     * Returns the position after the element in its array.
     * <p>
     * 배열에서 원소 다음 위치를 반환한다.
     *
     * @return the end position / 끝 위치
     */
    public int endPosition() {
        return end;
    }

    /**
     * Checks the tag and returns this element.
     * <p>
     * 태그를 확인하고 이 원소를 반환한다.
     *
     * @param expected the expected tag / 기대하는 태그
     * @return this element / 이 원소
     * @throws X509Exception if the tag differs / 태그가 다르면
     */
    public Der expect(int expected) throws X509Exception {
        if (tag != expected) {
            throw malformed("expected tag " + Integer.toHexString(expected) + " but found " + Integer.toHexString(tag));
        }
        return this;
    }

    /**
     * Returns a copy of the content bytes.
     * <p>
     * 내용 바이트의 사본을 반환한다.
     *
     * @return the content / 내용
     */
    public byte[] content() {
        byte[] out = new byte[end - start];
        System.arraycopy(buf, start, out, 0, out.length);
        return out;
    }

    /**
     * Returns a copy of the whole element with tag and length.
     * <p>
     * 태그와 길이를 포함한 원소 전체의 사본을 반환한다.
     *
     * @return the encoding / 부호화
     */
    public byte[] encoded() {
        byte[] out = new byte[end - tlvStart];
        System.arraycopy(buf, tlvStart, out, 0, out.length);
        return out;
    }

    /**
     * Reads the elements inside a constructed element.
     * <p>
     * 구성형 원소 안의 원소들을 읽는다.
     *
     * @param max the largest number of children allowed / 허용하는 자식의 최대 수
     * @return the children in order / 순서대로의 자식
     * @throws X509Exception if a child is malformed, the element is not constructed, or there are too many / 자식이 잘못되었거나 구성형이 아니거나 너무 많으면
     */
    public List<Der> children(int max) throws X509Exception {
        if ((tag & 0x20) == 0) {
            throw malformed("element is not constructed");
        }
        List<Der> out = new ArrayList<Der>();
        int at = start;
        // 내용 끝까지 자식을 차례로 읽는다.
        while (at < end) {
            if (out.size() >= max) {
                throw malformed("too many elements");
            }
            Der child = read(buf, at, end);
            out.add(child);
            at = child.end;
        }
        return out;
    }

    /**
     * Reads the single element inside an explicitly tagged element.
     * <p>
     * 명시적 태그 원소 안의 원소 하나를 읽는다.
     *
     * @return the inner element / 안쪽 원소
     * @throws X509Exception if the content is not exactly one element / 내용이 정확히 원소 하나가 아니면
     */
    public Der inner() throws X509Exception {
        return readExactly(buf, start, end);
    }

    /**
     * Reads an INTEGER with the minimal encoding that DER requires.
     * <p>
     * DER 이 요구하는 최소 부호화의 INTEGER 를 읽는다.
     *
     * @return the value (may be negative) / 값(음수일 수 있다)
     * @throws X509Exception if the element is not a minimal INTEGER / 원소가 최소 부호화의 INTEGER 가 아니면
     */
    public BigInteger integer() throws X509Exception {
        expect(INTEGER);
        int n = end - start;
        if (n == 0) {
            throw malformed("empty INTEGER");
        }
        if (n > 1 && ((buf[start] == 0 && (buf[start + 1] & 0x80) == 0) || (buf[start] == -1 && (buf[start + 1] & 0x80) != 0))) {
            throw malformed("INTEGER is not minimal");
        }
        return new BigInteger(content());
    }

    /**
     * Reads a BOOLEAN; DER allows only 00 and FF.
     * <p>
     * BOOLEAN 을 읽는다. DER 은 00 과 FF 만 허용한다.
     *
     * @return the value / 값
     * @throws X509Exception if the encoding is wrong / 부호화가 틀리면
     */
    public boolean bool() throws X509Exception {
        expect(BOOLEAN);
        if (end - start != 1 || (buf[start] != 0 && buf[start] != -1)) {
            throw malformed("bad BOOLEAN");
        }
        return buf[start] != 0;
    }

    /**
     * Reads an OBJECT IDENTIFIER as dotted text, for example <code>1.2.840.113549.1.1.11</code>.
     * <p>
     * OBJECT IDENTIFIER 를 점으로 나눈 문자열로 읽는다. 예: <code>1.2.840.113549.1.1.11</code>.
     *
     * @return the identifier / 식별자
     * @throws X509Exception if the encoding is wrong / 부호화가 틀리면
     */
    public String oid() throws X509Exception {
        expect(OID);
        if (end == start || (buf[end - 1] & 0x80) != 0 || end - start > 32) {
            throw malformed("bad OBJECT IDENTIFIER");
        }
        StringBuilder sb = new StringBuilder();
        long value = 0;
        boolean first = true;
        // 7비트 묶음으로 된 하위 식별자를 차례로 읽는다.
        for (int i = start; i < end; i++) {
            int b = buf[i] & 0xff;
            if (value == 0 && b == 0x80) {
                throw malformed("OBJECT IDENTIFIER arc is not minimal");
            }
            value = (value << 7) | (b & 0x7f);
            if (value > 0xffffffffL) {
                throw malformed("OBJECT IDENTIFIER arc too large");
            }
            if ((b & 0x80) == 0) {
                if (first) {
                    // 첫 하위 식별자는 앞의 두 마디를 합친 값이다.
                    long top = value < 40 ? 0 : value < 80 ? 1 : 2;
                    sb.append(top).append('.').append(value - 40 * top);
                    first = false;
                } else {
                    sb.append('.').append(value);
                }
                value = 0;
            }
        }
        return sb.toString();
    }

    /**
     * Reads a BIT STRING that must hold whole bytes (no unused bits), as keys and signatures do.
     * <p>
     * 키와 서명처럼 온전한 바이트만 담아야 하는(안 쓰는 비트가 없는) BIT STRING 을 읽는다.
     *
     * @return the bytes / 바이트
     * @throws X509Exception if the encoding is wrong or bits are unused / 부호화가 틀리거나 안 쓰는 비트가 있으면
     */
    public byte[] bitStringBytes() throws X509Exception {
        expect(BIT_STRING);
        if (end - start < 1 || buf[start] != 0) {
            throw malformed("BIT STRING with unused bits");
        }
        byte[] out = new byte[end - start - 1];
        System.arraycopy(buf, start + 1, out, 0, out.length);
        return out;
    }

    /**
     * Reads a BIT STRING of named bits (such as KeyUsage) as a mask: bit 0 of the ASN.1 definition becomes the value 1.
     * <p>
     * 이름 붙은 비트의 BIT STRING(KeyUsage 등)을 마스크로 읽는다: ASN.1 정의의 0번 비트가 값 1 이 된다.
     *
     * @return the mask of the first 16 named bits / 앞쪽 16개 비트의 마스크
     * @throws X509Exception if the encoding is wrong / 부호화가 틀리면
     */
    public int namedBits() throws X509Exception {
        expect(BIT_STRING);
        int n = end - start;
        if (n < 1 || (buf[start] & 0xff) > 7 || (n == 1 && buf[start] != 0)) {
            throw malformed("bad BIT STRING");
        }
        int mask = 0;
        // 앞쪽 두 바이트의 비트를 마스크로 옮긴다(바이트의 최상위 비트가 번호가 가장 낮은 비트이다).
        for (int i = 0; i < 16 && i < (n - 1) * 8; i++) {
            if ((buf[start + 1 + i / 8] & (0x80 >> (i % 8))) != 0) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    /**
     * Reads the content as text of one-byte characters (IA5String and similar ASCII strings).
     * <p>
     * 내용을 한 바이트 문자의 문자열로 읽는다(IA5String 과 비슷한 ASCII 문자열).
     *
     * @return the text / 문자열
     */
    public String ascii() {
        StringBuilder sb = new StringBuilder(end - start);
        // 바이트를 문자로 그대로 옮긴다.
        for (int i = start; i < end; i++) {
            sb.append((char) (buf[i] & 0xff));
        }
        return sb.toString();
    }
}
