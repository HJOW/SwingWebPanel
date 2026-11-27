package org.duckdns.hjow.swingwebpanel.tls13.x509;

import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.CertBuilder;
import org.duckdns.hjow.swingwebpanel.tls13.Hex;
import org.duckdns.hjow.swingwebpanel.tls13.TestKey;

/**
 * Host name verification of the TLS module (plan item P32-04-b): exact names, the limits of wildcards, IP address literals, and the
 * normalization of the host given by the application.
 * <p>
 * TLS 모듈의 호스트 이름 검증(계획 P32-04-b): 정확한 이름, 와일드카드의 경계, IP 주소 리터럴, 응용 프로그램이 준 호스트의 정규화.
 */
public class HostNamesTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HostNamesTest() {
    }

    /**
     * Exact names match without regard to case; other names do not.
     * <p>
     * 정확한 이름은 대소문자와 무관하게 맞고 다른 이름은 맞지 않는다.
     */
    public void testExactNames() {
        Check.isTrue(HostNames.dnsMatches("www.example.com", "www.example.com"), "equal");
        Check.isTrue(HostNames.dnsMatches("WWW.Example.COM", "www.example.com"), "upper case in the certificate");
        Check.isTrue(HostNames.dnsMatches("www.example.com.", "www.example.com"), "trailing dot in the certificate");
        Check.isFalse(HostNames.dnsMatches("example.com", "www.example.com"), "parent domain");
        Check.isFalse(HostNames.dnsMatches("www.example.com", "example.com"), "child domain");
        Check.isFalse(HostNames.dnsMatches("www.example.com", "wwwxexample.com"), "a dot is not a wildcard");
        Check.isFalse(HostNames.dnsMatches("", "www.example.com"), "empty name");
    }

    /**
     * A wildcard is the whole left-most label, matches exactly one label and needs two more labels.
     * <p>
     * 와일드카드는 맨 왼쪽 레이블 전체이고, 정확히 레이블 하나에 맞으며, 뒤에 레이블이 둘 더 있어야 한다.
     */
    public void testWildcardBoundaries() {
        Check.isTrue(HostNames.dnsMatches("*.example.com", "www.example.com"), "one label");
        Check.isTrue(HostNames.dnsMatches("*.EXAMPLE.com", "a.example.com"), "upper case pattern");
        Check.isFalse(HostNames.dnsMatches("*.example.com", "example.com"), "the wildcard does not match zero labels");
        Check.isFalse(HostNames.dnsMatches("*.example.com", "a.b.example.com"), "the wildcard does not match two labels");
        Check.isFalse(HostNames.dnsMatches("*.example.com", ".example.com"), "the wildcard does not match an empty label");
        Check.isFalse(HostNames.dnsMatches("*.example.com", "www.example.org"), "another domain");
        Check.isFalse(HostNames.dnsMatches("*.example.com", "wwwexample.com"), "the dot after the wildcard is required");
        Check.isFalse(HostNames.dnsMatches("*.com", "example.com"), "a wildcard directly below a top level domain");
        Check.isFalse(HostNames.dnsMatches("*", "example"), "a bare wildcard");
        Check.isFalse(HostNames.dnsMatches("*.", "example"), "a wildcard with an empty rest");
        Check.isFalse(HostNames.dnsMatches("w*.example.com", "www.example.com"), "partial wildcard at the end of a label");
        Check.isFalse(HostNames.dnsMatches("*w.example.com", "www.example.com"), "partial wildcard at the start of a label");
        Check.isFalse(HostNames.dnsMatches("www.*.com", "www.example.com"), "wildcard in the middle");
        Check.isFalse(HostNames.dnsMatches("*.*.example.com", "a.b.example.com"), "two wildcards");
        Check.isFalse(HostNames.dnsMatches("**.example.com", "www.example.com"), "double asterisk");
    }

    /**
     * Hosts are normalized: case, trailing dot, brackets, internationalized names; IP literals are recognized without a name lookup.
     * <p>
     * 호스트를 정규화한다: 대소문자, 끝의 점, 대괄호, 국제화 이름. IP 리터럴은 이름 조회 없이 알아본다.
     */
    public void testNormalizationAndIpLiterals() {
        Check.equal("www.example.com", HostNames.normalize(" WWW.Example.com. "), "case, spaces and trailing dot");
        Check.equal("::1", HostNames.normalize("[::1]"), "brackets of an IPv6 literal");
        Check.equal("xn--bj0bj06e.kr", HostNames.normalize("한글.kr"), "internationalized name becomes an A-label");
        Check.isNull(HostNames.normalize(""), "empty host");
        Check.isNull(HostNames.normalize(null), "null host");
        Check.equal("7f000001", Hex.encode(HostNames.ipLiteral("127.0.0.1")), "IPv4 literal");
        Check.equal("00000000000000000000000000000001", Hex.encode(HostNames.ipLiteral("::1")), "IPv6 literal");
        Check.equal("20010db8000000000000000000000001", Hex.encode(HostNames.ipLiteral("2001:db8::1")), "IPv6 literal with a prefix");
        Check.isNull(HostNames.ipLiteral("www.example.com"), "a DNS name");
        Check.isNull(HostNames.ipLiteral("1.2.3"), "three numbers");
        Check.isNull(HostNames.ipLiteral("1.2.3.256"), "a number above 255");
        Check.isNull(HostNames.ipLiteral("1.2.3.4.5"), "five numbers");
        Check.isNull(HostNames.ipLiteral("a.b.c.d"), "letters");
        Check.isNull(HostNames.ipLiteral("host:name"), "a colon in something that is not an address");
    }

    /**
     * A certificate is matched through subjectAltName only: DNS names for host names, IP entries for IP literals.
     * <p>
     * 인증서는 subjectAltName 으로만 대조한다: 호스트 이름은 DNS 이름과, IP 리터럴은 IP 항목과.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCertificateMatching() throws Exception {
        TestKey key = TestKey.rsa("leaf");
        X509Cert c = X509Cert.parse(new CertBuilder("cn.example.com", key).selfSigned().dns("www.example.com", "*.api.example.com")
                .ip(new byte[] { 127, 0, 0, 1 }).ip(Hex.decode("00000000000000000000000000000001")).build());
        Check.isTrue(HostNames.matches(c, "www.example.com"), "DNS name");
        Check.isTrue(HostNames.matches(c, "WWW.EXAMPLE.COM."), "DNS name in another form");
        Check.isTrue(HostNames.matches(c, "v1.api.example.com"), "wildcard name");
        Check.isFalse(HostNames.matches(c, "api.example.com"), "the wildcard does not match its parent");
        Check.isFalse(HostNames.matches(c, "cn.example.com"), "the common name is ignored");
        Check.isTrue(HostNames.matches(c, "127.0.0.1"), "IPv4 entry");
        Check.isTrue(HostNames.matches(c, "[::1]"), "IPv6 entry");
        Check.isTrue(HostNames.matches(c, "0:0:0:0:0:0:0:1"), "IPv6 entry in the long form");
        Check.isFalse(HostNames.matches(c, "127.0.0.2"), "another address");
        Check.isFalse(HostNames.matches(c, null), "no host");
        X509Cert dnsOnly = X509Cert.parse(new CertBuilder("x", key).selfSigned().dns("127.0.0.1").build());
        Check.isFalse(HostNames.matches(dnsOnly, "127.0.0.1"), "an IP literal is not matched against DNS names");
    }
}
