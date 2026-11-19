package org.duckdns.hjow.swingwebpanel.net;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

/**
 * Checks whether the certificate of a TLS server was issued for the host name that the client connected to (RFC 2818 section 3.1): the
 * <code>dNSName</code> (or <code>iPAddress</code>) entries of <code>subjectAltName</code> are used, and only when the certificate has no
 * such entry for the kind of the host the most specific <code>CN</code> of the subject. A wildcard is accepted only as a whole left-most
 * label and covers exactly one label (<code>*.example.com</code> matches <code>www.example.com</code> but neither
 * <code>example.com</code> nor <code>a.b.example.com</code>). Only public APIs are used.
 * <p>
 * TLS 서버의 인증서가 클라이언트가 연결한 호스트 이름을 위해 발급되었는지 확인한다(RFC 2818 3.1절): <code>subjectAltName</code> 의
 * <code>dNSName</code>(또는 <code>iPAddress</code>) 항목을 쓰고, 그 종류의 항목이 없을 때만 주체의 가장 구체적인 <code>CN</code> 을 쓴다.
 * 와일드카드는 가장 왼쪽 레이블 전체로만 받아들이며 레이블 정확히 하나를 대신한다(<code>*.example.com</code> 은 <code>www.example.com</code> 에
 * 일치하지만 <code>example.com</code> 이나 <code>a.b.example.com</code> 에는 일치하지 않는다). 공개 API 만 쓴다.
 */
final class HostnameMatcher {

    /**
     * The type code of the <code>dNSName</code> entry of <code>subjectAltName</code>.
     * <p>
     * <code>subjectAltName</code> 의 <code>dNSName</code> 항목의 종류 번호.
     */
    private static final int ALT_DNS = 2;

    /**
     * The type code of the <code>iPAddress</code> entry of <code>subjectAltName</code>.
     * <p>
     * <code>subjectAltName</code> 의 <code>iPAddress</code> 항목의 종류 번호.
     */
    private static final int ALT_IP = 7;

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private HostnameMatcher() {
    }

    /**
     * Tells whether a certificate is valid for a host.
     * <p>
     * 인증서가 호스트에 유효한지 알려 준다.
     *
     * @param host the host name or IP address text that the client connected to / 클라이언트가 연결한 호스트 이름 또는 IP 주소 문자열
     * @param cert the certificate of the server / 서버의 인증서
     * @return true if the certificate names the host / 인증서가 그 호스트를 이름으로 가지면 true
     */
    static boolean matches(String host, X509Certificate cert) {
        if (host == null || cert == null) {
            return false;
        }
        String h = host.toLowerCase();
        // 대괄호로 감싼 IPv6 주소는 대괄호를 벗긴다.
        if (h.startsWith("[") && h.endsWith("]")) {
            h = h.substring(1, h.length() - 1);
        }
        boolean ip = looksLikeIp(h);
        int wanted = ip ? ALT_IP : ALT_DNS;
        boolean sawEntry = false;
        try {
            Collection<List<?>> alt = cert.getSubjectAlternativeNames();
            if (alt != null) {
                // subjectAltName 의 항목을 하나씩 보며 같은 종류의 이름이 호스트와 일치하는지 확인한다.
                for (List<?> entry : alt) {
                    if (entry.size() < 2 || !(entry.get(0) instanceof Integer) || !(entry.get(1) instanceof String)) {
                        continue;
                    }
                    int type = ((Integer) entry.get(0)).intValue();
                    if (type != wanted) {
                        continue;
                    }
                    sawEntry = true;
                    String name = ((String) entry.get(1)).toLowerCase();
                    if (ip ? name.equals(h) : nameMatches(h, name)) {
                        return true;
                    }
                }
            }
        } catch (CertificateParsingException e) {
            return false;
        }
        if (sawEntry || ip) {
            return false;
        }
        String cn = commonName(cert);
        return cn != null && nameMatches(h, cn.toLowerCase());
    }

    /**
     * Compares a host name with a (possibly wildcard) name of a certificate; both are lower case.
     * <p>
     * 호스트 이름을 인증서의 (와일드카드일 수 있는) 이름과 비교한다. 둘 다 소문자이다.
     *
     * @param host the host name / 호스트 이름
     * @param name the name of the certificate / 인증서의 이름
     * @return true if they match / 일치하면 true
     */
    static boolean nameMatches(String host, String name) {
        String n = name.endsWith(".") ? name.substring(0, name.length() - 1) : name;
        String h = host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
        if (n.length() == 0 || h.length() == 0) {
            return false;
        }
        if (!n.startsWith("*.")) {
            return n.equals(h);
        }
        // 와일드카드는 가장 왼쪽 레이블 전체이며 정확히 한 레이블(점이 없는 비어 있지 않은 문자열)만 대신한다.
        String suffix = n.substring(1);
        if (suffix.indexOf('*') >= 0 || suffix.indexOf('.', 1) < 0) {
            return false;
        }
        int dot = h.indexOf('.');
        return dot > 0 && h.substring(dot).equals(suffix);
    }

    /**
     * Tells whether a host text is an IP address (IPv4 dotted decimal or an IPv6 text with colons).
     * <p>
     * 호스트 문자열이 IP 주소(점으로 구분한 IPv4 또는 콜론이 있는 IPv6)인지 알려 준다.
     *
     * @param h the host text / 호스트 문자열
     * @return true for an IP address / IP 주소이면 true
     */
    private static boolean looksLikeIp(String h) {
        if (h.indexOf(':') >= 0) {
            return true;
        }
        boolean digitsAndDots = h.length() > 0;
        // 숫자와 점만으로 이루어졌는지 확인한다.
        for (int i = 0; i < h.length(); i++) {
            char c = h.charAt(i);
            if ((c < '0' || c > '9') && c != '.') {
                digitsAndDots = false;
                break;
            }
        }
        return digitsAndDots && h.indexOf('.') > 0;
    }

    /**
     * Returns the most specific common name of the subject of a certificate.
     * <p>
     * 인증서 주체의 가장 구체적인 common name 을 반환한다.
     *
     * @param cert the certificate / 인증서
     * @return the name or null / 이름. 없으면 null
     */
    private static String commonName(X509Certificate cert) {
        try {
            LdapName dn = new LdapName(cert.getSubjectX500Principal().getName(X500Principal.RFC2253));
            // RFC 2253 문자열은 가장 구체적인 항목이 맨 앞이므로 앞쪽에서부터 첫 CN 을 찾는다.
            for (int i = dn.size() - 1; i >= 0; i--) {
                Rdn rdn = dn.getRdn(i);
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    Object v = rdn.getValue();
                    return v == null ? null : v.toString();
                }
            }
        } catch (InvalidNameException e) {
            return null;
        }
        return null;
    }
}
