package org.duckdns.hjow.swingwebpanel.tls13;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * A minimal DER encoder for building test certificates and keys. Every method returns a complete element (tag, length, value).
 * Example: <code>byte[] alg = DerWriter.seq(DerWriter.oid("1.2.840.113549.1.1.11"), DerWriter.nul());</code>.
 * <p>
 * 시험용 인증서와 키를 만드는 최소한의 DER 부호화기. 모든 메서드는 온전한 원소(태그, 길이, 값)를 반환한다.
 */
public final class DerWriter {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DerWriter() {
    }

    /**
     * Builds an element with the concatenation of the parts as its content.
     * <p>
     * 조각들을 이어 붙인 것을 내용으로 하는 원소를 만든다.
     *
     * @param tag   the tag byte / 태그 바이트
     * @param parts the content / 내용
     * @return the element / 원소
     */
    public static byte[] tlv(int tag, byte[]... parts) {
        int len = 0;
        // 내용 길이를 센다.
        for (int i = 0; i < parts.length; i++) {
            len += parts[i].length;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        if (len < 0x80) {
            out.write(len);
        } else if (len < 0x100) {
            out.write(0x81);
            out.write(len);
        } else if (len < 0x10000) {
            out.write(0x82);
            out.write(len >> 8);
            out.write(len);
        } else {
            out.write(0x83);
            out.write(len >> 16);
            out.write(len >> 8);
            out.write(len);
        }
        // 내용을 차례로 쓴다.
        for (int i = 0; i < parts.length; i++) {
            out.write(parts[i], 0, parts[i].length);
        }
        return out.toByteArray();
    }

    /**
     * Builds a SEQUENCE.
     * <p>
     * SEQUENCE 를 만든다.
     *
     * @param parts the elements / 원소들
     * @return the element / 원소
     */
    public static byte[] seq(byte[]... parts) {
        return tlv(0x30, parts);
    }

    /**
     * Builds a SET.
     * <p>
     * SET 을 만든다.
     *
     * @param parts the elements / 원소들
     * @return the element / 원소
     */
    public static byte[] set(byte[]... parts) {
        return tlv(0x31, parts);
    }

    /**
     * Builds an INTEGER.
     * <p>
     * INTEGER 를 만든다.
     *
     * @param v the value / 값
     * @return the element / 원소
     */
    public static byte[] integer(BigInteger v) {
        return tlv(0x02, v.toByteArray());
    }

    /**
     * Builds an INTEGER from a long.
     * <p>
     * long 값으로 INTEGER 를 만든다.
     *
     * @param v the value / 값
     * @return the element / 원소
     */
    public static byte[] integer(long v) {
        return integer(BigInteger.valueOf(v));
    }

    /**
     * Builds a NULL.
     * <p>
     * NULL 을 만든다.
     *
     * @return the element / 원소
     */
    public static byte[] nul() {
        return new byte[] { 5, 0 };
    }

    /**
     * Builds a BOOLEAN.
     * <p>
     * BOOLEAN 을 만든다.
     *
     * @param v the value / 값
     * @return the element / 원소
     */
    public static byte[] bool(boolean v) {
        return new byte[] { 1, 1, (byte) (v ? 0xff : 0) };
    }

    /**
     * Builds an OCTET STRING.
     * <p>
     * OCTET STRING 을 만든다.
     *
     * @param content the bytes / 바이트
     * @return the element / 원소
     */
    public static byte[] octets(byte[] content) {
        return tlv(0x04, content);
    }

    /**
     * Builds a BIT STRING of whole bytes.
     * <p>
     * 온전한 바이트로 된 BIT STRING 을 만든다.
     *
     * @param content the bytes / 바이트
     * @return the element / 원소
     */
    public static byte[] bits(byte[] content) {
        return tlv(0x03, new byte[] { 0 }, content);
    }

    /**
     * Builds a BIT STRING of named bits from a mask in which the value <code>1 &lt;&lt; i</code> stands for bit i of the ASN.1 definition.
     * <p>
     * 값 <code>1 &lt;&lt; i</code> 가 ASN.1 정의의 i번 비트를 뜻하는 마스크로 이름 붙은 비트의 BIT STRING 을 만든다.
     *
     * @param mask the bits / 비트들
     * @return the element / 원소
     */
    public static byte[] namedBits(int mask) {
        int high = 31 - Integer.numberOfLeadingZeros(mask);
        if (mask == 0) {
            return new byte[] { 3, 1, 0 };
        }
        byte[] data = new byte[high / 8 + 1];
        // 비트마다 바이트의 높은 쪽부터 채운다.
        for (int i = 0; i <= high; i++) {
            if ((mask & (1 << i)) != 0) {
                data[i / 8] |= (byte) (0x80 >> (i % 8));
            }
        }
        return tlv(0x03, new byte[] { (byte) (7 - (high % 8)) }, data);
    }

    /**
     * Builds an OBJECT IDENTIFIER from dotted text.
     * <p>
     * 점으로 나눈 문자열로 OBJECT IDENTIFIER 를 만든다.
     *
     * @param dotted the identifier, for example <code>2.5.29.19</code> / 식별자. 예: <code>2.5.29.19</code>
     * @return the element / 원소
     */
    public static byte[] oid(String dotted) {
        String[] arcs = dotted.split("\\.");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 앞의 두 마디는 합쳐 하나로 쓰고, 나머지는 7비트 묶음으로 쓴다.
        for (int i = 1; i < arcs.length; i++) {
            long v = Long.parseLong(arcs[i]);
            if (i == 1) {
                v += 40 * Long.parseLong(arcs[0]);
            }
            int groups = 1;
            // 필요한 7비트 묶음 수를 센다.
            for (long t = v >>> 7; t != 0; t >>>= 7) {
                groups++;
            }
            // 높은 묶음부터 쓰고 마지막 묶음만 최상위 비트를 끈다.
            for (int g = groups - 1; g >= 0; g--) {
                out.write((int) ((v >>> (7 * g)) & 0x7f) | (g > 0 ? 0x80 : 0));
            }
        }
        return tlv(0x06, out.toByteArray());
    }

    /**
     * Builds a UTF8String.
     * <p>
     * UTF8String 을 만든다.
     *
     * @param text the text / 문자열
     * @return the element / 원소
     */
    public static byte[] utf8(String text) {
        try {
            return tlv(0x0c, text.getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    /**
     * Builds an element with one-byte characters under any tag (IA5String, or an implicitly tagged name).
     * <p>
     * 임의의 태그 아래 한 바이트 문자들의 원소를 만든다(IA5String, 또는 암시적 태그의 이름).
     *
     * @param tag  the tag / 태그
     * @param text the ASCII text / ASCII 문자열
     * @return the element / 원소
     */
    public static byte[] ascii(int tag, String text) {
        byte[] b = new byte[text.length()];
        // 문자 값을 그대로 옮긴다.
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) text.charAt(i);
        }
        return tlv(tag, b);
    }

    /**
     * Builds a time as RFC 5280 requires: UTCTime for the years 1950 to 2049, GeneralizedTime otherwise.
     * <p>
     * RFC 5280 이 요구하는 대로 시간을 만든다: 1950~2049년은 UTCTime, 그 밖은 GeneralizedTime.
     *
     * @param millis milliseconds since 1970 / 1970년부터의 밀리초
     * @return the element / 원소
     */
    public static byte[] time(long millis) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(millis);
        int year = c.get(Calendar.YEAR);
        String rest = two(c.get(Calendar.MONTH) + 1) + two(c.get(Calendar.DAY_OF_MONTH)) + two(c.get(Calendar.HOUR_OF_DAY)) + two(c.get(Calendar.MINUTE))
                + two(c.get(Calendar.SECOND)) + "Z";
        if (year >= 1950 && year <= 2049) {
            return ascii(0x17, two(year % 100) + rest);
        }
        return ascii(0x18, year + rest);
    }

    /**
     * Formats a number with two digits.
     * <p>
     * 수를 두 자리로 쓴다.
     *
     * @param v the number / 수
     * @return the text / 문자열
     */
    private static String two(int v) {
        return (v < 10 ? "0" : "") + v;
    }

    /**
     * Builds a Name with a common name and optionally an organization in front of it.
     * <p>
     * 일반 이름(CN)과, 선택적으로 그 앞의 조직(O)을 가진 Name 을 만든다.
     *
     * @param organization the organization or null / 조직 또는 null
     * @param commonName   the common name / 일반 이름
     * @return the element / 원소
     */
    public static byte[] name(String organization, String commonName) {
        byte[] cn = set(seq(oid("2.5.4.3"), utf8(commonName)));
        if (organization == null) {
            return seq(cn);
        }
        return seq(set(seq(oid("2.5.4.10"), utf8(organization))), cn);
    }
}
