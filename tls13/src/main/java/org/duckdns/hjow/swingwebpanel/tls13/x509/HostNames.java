package org.duckdns.hjow.swingwebpanel.tls13.x509;

import java.util.List;
import java.util.Locale;

/**
 * Checks that a certificate names the host that the client wants to reach (RFC 6125 and RFC 9525). Only the subjectAltName extension
 * is used; the common name of the subject is ignored, as current browsers do. A wildcard is accepted only as the whole left-most label
 * (<code>*.example.com</code>), it matches exactly one label, and at least two labels must follow it. A host that is an IP address
 * literal is compared with the iPAddress entries. Example: <code>if (!HostNames.matches(cert, "www.example.com")) { reject(); }</code>.
 * <p>
 * 인증서가 클라이언트가 가려는 호스트의 이름을 갖는지 확인한다(RFC 6125, RFC 9525). subjectAltName 확장만 쓰고, 요즘 브라우저처럼 주체의 일반 이름(CN)은
 * 무시한다. 와일드카드는 맨 왼쪽 레이블 전체(<code>*.example.com</code>)일 때만 받아들이고, 정확히 레이블 하나에 맞으며, 그 뒤에 레이블이 둘 이상 있어야
 * 한다. 호스트가 IP 주소 리터럴이면 iPAddress 항목과 비교한다.
 */
public final class HostNames {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private HostNames() {
    }

    /**
     * Brings a host name to the form used for comparison: ASCII (IDNA A-labels), lower case, without a trailing dot and without the
     * brackets of an IPv6 literal.
     * <p>
     * 호스트 이름을 비교용 형식으로 바꾼다: ASCII(IDNA A-레이블), 소문자, 끝의 점 없음, IPv6 리터럴의 대괄호 없음.
     *
     * @param host the host as the application gave it / 응용 프로그램이 준 호스트
     * @return the normalized host, or null if it cannot be a host name / 정규화한 호스트. 호스트 이름이 될 수 없으면 null
     */
    public static String normalize(String host) {
        if (host == null) {
            return null;
        }
        String h = host.trim();
        if (h.startsWith("[") && h.endsWith("]")) {
            h = h.substring(1, h.length() - 1);
        }
        if (h.endsWith(".")) {
            h = h.substring(0, h.length() - 1);
        }
        if (h.length() == 0 || h.length() > 255) {
            return null;
        }
        boolean ascii = true;
        // ASCII 가 아닌 글자가 있는지 본다.
        for (int i = 0; i < h.length(); i++) {
            ascii &= h.charAt(i) < 0x80;
        }
        if (!ascii) {
            try {
                h = java.net.IDN.toASCII(h);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return h.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Parses an IPv4 literal of four decimal numbers.
     * <p>
     * 십진수 넷으로 된 IPv4 리터럴을 해석한다.
     *
     * @param host the text / 문자열
     * @return four bytes, or null if the text is not such a literal / 4바이트. 그런 리터럴이 아니면 null
     */
    private static byte[] ipv4(String host) {
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        byte[] out = new byte[4];
        // 마디마다 0~255 의 십진수여야 한다.
        for (int i = 0; i < 4; i++) {
            String p = parts[i];
            if (p.length() == 0 || p.length() > 3) {
                return null;
            }
            int v = 0;
            // 숫자만 허용한다.
            for (int k = 0; k < p.length(); k++) {
                char c = p.charAt(k);
                if (c < '0' || c > '9') {
                    return null;
                }
                v = v * 10 + (c - '0');
            }
            if (v > 255) {
                return null;
            }
            out[i] = (byte) v;
        }
        return out;
    }

    /**
     * Returns the address bytes if the host is an IP address literal. No name lookup is made.
     * <p>
     * 호스트가 IP 주소 리터럴이면 주소 바이트를 반환한다. 이름 조회는 하지 않는다.
     *
     * @param host the normalized host / 정규화한 호스트
     * @return 4 or 16 bytes, or null for a DNS name / 4 또는 16바이트. DNS 이름이면 null
     */
    public static byte[] ipLiteral(String host) {
        byte[] v4 = ipv4(host);
        if (v4 != null) {
            return v4;
        }
        if (host.indexOf(':') < 0) {
            return null;
        }
        // 콜론이 있는 문자열은 호스트 이름일 수 없으므로 주소 해석기가 이름을 조회하지 않는다.
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (!(c == ':' || c == '.' || Character.digit(c, 16) >= 0)) {
                return null;
            }
        }
        try {
            byte[] a = java.net.InetAddress.getByName(host).getAddress();
            return a.length == 16 ? a : null;
        } catch (java.net.UnknownHostException e) {
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Tells if a dNSName entry of a certificate matches a host name.
     * <p>
     * 인증서의 dNSName 항목이 호스트 이름에 맞는지 알려 준다.
     *
     * @param pattern the entry, possibly with a wildcard label / 항목. 와일드카드 레이블이 있을 수 있다
     * @param host    the normalized host name / 정규화한 호스트 이름
     * @return true if it matches / 맞으면 true
     */
    static boolean dnsMatches(String pattern, String host) {
        String p = pattern.toLowerCase(Locale.ENGLISH);
        if (p.endsWith(".")) {
            p = p.substring(0, p.length() - 1);
        }
        if (p.length() == 0) {
            return false;
        }
        if (p.indexOf('*') < 0) {
            return p.equals(host);
        }
        // 와일드카드는 맨 앞의 "*." 하나만 허용한다.
        if (!p.startsWith("*.") || p.indexOf('*', 1) >= 0) {
            return false;
        }
        String suffix = p.substring(1);
        // 와일드카드 뒤에 레이블이 둘 이상 있어야 한다(".example.com" 에는 점이 둘).
        if (suffix.indexOf('.', 1) < 0 || suffix.endsWith(".")) {
            return false;
        }
        if (!host.endsWith(suffix) || host.length() <= suffix.length()) {
            return false;
        }
        String first = host.substring(0, host.length() - suffix.length());
        return first.indexOf('.') < 0;
    }

    /**
     * Tells if a certificate names a host.
     * <p>
     * 인증서가 호스트의 이름을 갖는지 알려 준다.
     *
     * @param cert the certificate of the server / 서버의 인증서
     * @param host the host as the application gave it / 응용 프로그램이 준 호스트
     * @return true if a subjectAltName entry matches / subjectAltName 항목이 맞으면 true
     */
    public static boolean matches(X509Cert cert, String host) {
        String h = normalize(host);
        if (h == null) {
            return false;
        }
        byte[] ip = ipLiteral(h);
        if (ip != null) {
            List<byte[]> ips = cert.ipAddresses();
            // IP 리터럴은 iPAddress 항목과만 비교한다.
            for (int i = 0; i < ips.size(); i++) {
                if (java.util.Arrays.equals(ip, ips.get(i))) {
                    return true;
                }
            }
            return false;
        }
        List<String> names = cert.dnsNames();
        // dNSName 항목을 차례로 비교한다.
        for (int i = 0; i < names.size(); i++) {
            if (dnsMatches(names.get(i), h)) {
                return true;
            }
        }
        return false;
    }
}
