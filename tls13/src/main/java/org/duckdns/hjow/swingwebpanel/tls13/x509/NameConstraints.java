package org.duckdns.hjow.swingwebpanel.tls13.x509;

import java.util.ArrayList;
import java.util.List;

/**
 * The nameConstraints extension of a CA certificate (RFC 5280 section 4.2.1.10) for the name forms that matter for TLS servers: DNS
 * names, IP addresses and directory names. A certificate below the CA must have all its names of a constrained form inside a permitted
 * subtree (if any is given for that form) and outside every excluded subtree. Example:
 * <code>if (!ca.nameConstraints().allows(leaf)) { reject(); }</code>.
 * <p>
 * CA 인증서의 nameConstraints 확장(RFC 5280 4.2.1.10절) 가운데 TLS 서버에 의미가 있는 이름 형식: DNS 이름, IP 주소, 디렉터리 이름. CA 아래의 인증서는
 * 제약된 형식의 모든 이름이 허용 하위 트리(그 형식에 주어졌다면) 안에 있고 모든 제외 하위 트리 밖에 있어야 한다.
 */
public final class NameConstraints {

    /** Permitted DNS subtrees (lower case). / 허용 DNS 하위 트리(소문자). */
    private final List<String> permittedDns = new ArrayList<String>();

    /** Excluded DNS subtrees (lower case). / 제외 DNS 하위 트리(소문자). */
    private final List<String> excludedDns = new ArrayList<String>();

    /** Permitted IP ranges: address followed by mask (8 or 32 bytes). / 허용 IP 범위: 주소와 그 뒤의 마스크(8 또는 32바이트). */
    private final List<byte[]> permittedIp = new ArrayList<byte[]>();

    /** Excluded IP ranges. / 제외 IP 범위. */
    private final List<byte[]> excludedIp = new ArrayList<byte[]>();

    /** Permitted directory names (encoded Name elements). / 허용 디렉터리 이름(부호화된 Name 원소). */
    private final List<byte[]> permittedDir = new ArrayList<byte[]>();

    /** Excluded directory names. / 제외 디렉터리 이름. */
    private final List<byte[]> excludedDir = new ArrayList<byte[]>();

    /**
     * Parses the extension value: <code>SEQUENCE { permittedSubtrees [0], excludedSubtrees [1] }</code>.
     * <p>
     * 확장 값을 해석한다: <code>SEQUENCE { permittedSubtrees [0], excludedSubtrees [1] }</code>.
     *
     * @param value the extension value / 확장 값
     * @throws X509Exception if the value is malformed / 값이 잘못되었으면
     */
    NameConstraints(Der value) throws X509Exception {
        List<Der> parts = value.expect(Der.SEQUENCE).children(2);
        // 허용 [0] 과 제외 [1] 하위 트리 목록을 차례로 읽는다.
        for (int i = 0; i < parts.size(); i++) {
            Der p = parts.get(i);
            boolean permitted = p.tag() == 0xa0;
            if (!permitted && p.tag() != 0xa1) {
                throw Der.malformed("unexpected nameConstraints field");
            }
            List<Der> trees = p.children(1024);
            // GeneralSubtree ::= SEQUENCE { base GeneralName, minimum [0], maximum [1] } 의 base 만 쓴다.
            for (int k = 0; k < trees.size(); k++) {
                List<Der> tree = trees.get(k).expect(Der.SEQUENCE).children(3);
                if (tree.isEmpty()) {
                    throw Der.malformed("empty GeneralSubtree");
                }
                Der base = tree.get(0);
                if (base.tag() == 0x82) {
                    (permitted ? permittedDns : excludedDns).add(base.ascii().toLowerCase(java.util.Locale.ENGLISH));
                } else if (base.tag() == 0x87) {
                    if (base.length() != 8 && base.length() != 32) {
                        throw Der.malformed("iPAddress constraint of " + base.length() + " bytes");
                    }
                    (permitted ? permittedIp : excludedIp).add(base.content());
                } else if (base.tag() == 0xa4) {
                    (permitted ? permittedDir : excludedDir).add(base.inner().expect(Der.SEQUENCE).encoded());
                }
            }
        }
    }

    /**
     * Creates constraints that permit only DNS names inside the given subtrees (the "applied constraints" of a root program).
     * <p>
     * 주어진 하위 트리 안의 DNS 이름만 허용하는 제약을 만든다(루트 프로그램의 "적용 제약").
     *
     * @param dnsSubtrees the permitted subtrees, for example <code>gov.tr</code> / 허용 하위 트리. 예: <code>gov.tr</code>
     */
    NameConstraints(List<String> dnsSubtrees) {
        // 하위 트리를 소문자로, 앞의 "*." 과 점을 떼고 넣는다.
        for (int i = 0; i < dnsSubtrees.size(); i++) {
            String s = dnsSubtrees.get(i).trim().toLowerCase(java.util.Locale.ENGLISH);
            if (s.startsWith("*.")) {
                s = s.substring(2);
            }
            if (s.length() > 0) {
                permittedDns.add(s);
            }
        }
    }

    /**
     * Tells if a DNS name lies inside a DNS subtree: equal to it or below it. A constraint that starts with a dot matches only names
     * below it.
     * <p>
     * DNS 이름이 DNS 하위 트리 안에 있는지 알려 준다: 같거나 그 아래. 점으로 시작하는 제약은 그 아래의 이름에만 맞는다.
     *
     * @param name       the lower case DNS name; a leading wildcard label counts as a name below / 소문자 DNS 이름. 앞의 와일드카드 레이블은 그 아래 이름으로 본다
     * @param constraint the lower case constraint / 소문자 제약
     * @return true if inside / 안에 있으면 true
     */
    static boolean dnsWithin(String name, String constraint) {
        if (constraint.length() == 0) {
            return true;
        }
        if (constraint.charAt(0) == '.') {
            return name.endsWith(constraint);
        }
        return name.equals(constraint) || name.endsWith("." + constraint);
    }

    /**
     * Tells if an address lies inside a range.
     * <p>
     * 주소가 범위 안에 있는지 알려 준다.
     *
     * @param address the address of 4 or 16 bytes / 4 또는 16바이트 주소
     * @param range   address and mask of the same family / 같은 계열의 주소와 마스크
     * @return true if inside / 안에 있으면 true
     */
    private static boolean ipWithin(byte[] address, byte[] range) {
        if (range.length != address.length * 2) {
            return false;
        }
        // 마스크가 켜진 비트가 모두 같은지 본다.
        for (int i = 0; i < address.length; i++) {
            if (((address[i] ^ range[i]) & range[address.length + i]) != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells if a directory name lies inside a subtree: its first relative distinguished names are those of the base, byte for byte.
     * <p>
     * 디렉터리 이름이 하위 트리 안에 있는지 알려 준다: 앞쪽 상대 고유 이름들이 기준 이름의 것과 바이트 단위로 같다.
     *
     * @param name the encoded Name / 부호화된 Name
     * @param base the encoded base Name / 부호화된 기준 Name
     * @return true if inside / 안에 있으면 true
     */
    private static boolean dirWithin(byte[] name, byte[] base) {
        try {
            List<Der> n = Der.readExactly(name, 0, name.length).children(64);
            List<Der> b = Der.readExactly(base, 0, base.length).children(64);
            if (b.size() > n.size()) {
                return false;
            }
            // 기준의 RDN 을 차례로 비교한다.
            for (int i = 0; i < b.size(); i++) {
                if (!java.util.Arrays.equals(n.get(i).encoded(), b.get(i).encoded())) {
                    return false;
                }
            }
            return true;
        } catch (X509Exception e) {
            return false;
        }
    }

    /**
     * Tells if the names of a certificate satisfy these constraints.
     * <p>
     * 인증서의 이름들이 이 제약을 만족하는지 알려 준다.
     *
     * @param cert the certificate below the constrained CA / 제약된 CA 아래의 인증서
     * @return true if all DNS names, IP addresses and the subject name are allowed / DNS 이름, IP 주소, 주체 이름이 모두 허용되면 true
     */
    public boolean allows(X509Cert cert) {
        List<String> names = cert.dnsNames();
        // DNS 이름마다 제외 목록 밖이고(허용 목록이 있으면) 허용 목록 안인지 본다.
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i).toLowerCase(java.util.Locale.ENGLISH);
            if (name.startsWith("*.")) {
                name = "x" + name.substring(1);
            }
            boolean ok = permittedDns.isEmpty();
            // 허용 하위 트리 가운데 하나에 들어야 한다.
            for (int k = 0; !ok && k < permittedDns.size(); k++) {
                ok = dnsWithin(name, permittedDns.get(k));
            }
            // 제외 하위 트리에는 들면 안 된다.
            for (int k = 0; ok && k < excludedDns.size(); k++) {
                ok = !dnsWithin(name, excludedDns.get(k));
            }
            if (!ok) {
                return false;
            }
        }
        List<byte[]> ips = cert.ipAddresses();
        // IP 주소마다 같은 규칙을 적용한다.
        for (int i = 0; i < ips.size(); i++) {
            boolean ok = permittedIp.isEmpty();
            // 허용 범위 가운데 하나에 들어야 한다.
            for (int k = 0; !ok && k < permittedIp.size(); k++) {
                ok = ipWithin(ips.get(i), permittedIp.get(k));
            }
            // 제외 범위에는 들면 안 된다.
            for (int k = 0; ok && k < excludedIp.size(); k++) {
                ok = !ipWithin(ips.get(i), excludedIp.get(k));
            }
            if (!ok) {
                return false;
            }
        }
        byte[] subject = cert.subject();
        boolean empty = subject.length <= 2;
        boolean ok = permittedDir.isEmpty() || empty;
        // 주체 이름이 비어 있지 않으면 디렉터리 이름 제약을 적용한다.
        for (int k = 0; !ok && k < permittedDir.size(); k++) {
            ok = dirWithin(subject, permittedDir.get(k));
        }
        // 제외 디렉터리 이름에는 들면 안 된다.
        for (int k = 0; ok && !empty && k < excludedDir.size(); k++) {
            ok = !dirWithin(subject, excludedDir.get(k));
        }
        return ok;
    }
}
