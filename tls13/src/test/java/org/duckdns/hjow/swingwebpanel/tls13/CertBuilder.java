package org.duckdns.hjow.swingwebpanel.tls13;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Bytes;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.HashAlg;

/**
 * Builds X.509 certificates for tests, with any combination of fields and deliberate defects. The defaults give a valid end-entity
 * certificate for one year around {@link #NOW}. Example:
 * <pre>
 * byte[] root = new CertBuilder("Root", rootKey).ca(-1).selfSigned().build();
 * byte[] leaf = new CertBuilder("server", leafKey).issuer("Root", rootKey).dns("www.example.test").build();
 * </pre>
 * <p>
 * 시험용 X.509 인증서를 만든다. 필드를 자유롭게 조합하고 일부러 결함을 넣을 수 있다. 기본값은 {@link #NOW} 앞뒤 1년 동안 유효한 최종 인증서이다.
 */
public final class CertBuilder {

    /** The fixed "current time" of certificate tests: 2026-11-28 00:00:00 UTC. / 인증서 시험의 고정된 "현재 시각": 2026-11-28 00:00:00 UTC. */
    public static final long NOW = 1795824000000L;

    /** One day in milliseconds. / 하루(밀리초). */
    public static final long DAY = 24L * 60 * 60 * 1000;

    /** The counter of serial numbers. / 일련번호 카운터. */
    private static long nextSerial = 1000;

    /** The encoded subject name. / 부호화된 주체 이름. */
    private byte[] subject;

    /** The key of the subject. / 주체의 키. */
    private final TestKey subjectKey;

    /** The encoded issuer name. / 부호화된 발급자 이름. */
    private byte[] issuer;

    /** The key that signs. / 서명하는 키. */
    private TestKey issuerKey;

    /** The start of the validity. / 유효 기간의 시작. */
    private long notBefore = NOW - 30 * DAY;

    /** The end of the validity. / 유효 기간의 끝. */
    private long notAfter = NOW + 335 * DAY;

    /** True to add basicConstraints with cA TRUE. / cA TRUE 인 basicConstraints 를 넣으려면 true. */
    private boolean ca;

    /** The path length constraint, or -1. / 경로 길이 제약. 없으면 -1. */
    private int pathLength = -1;

    /** The key usage bits, or -1 for no extension. / 키 용도 비트. 확장을 넣지 않으려면 -1. */
    private int keyUsage = -1;

    /** The extended key usage OIDs, or null for no extension. / 확장 키 용도 OID. 확장을 넣지 않으려면 null. */
    private List<String> extendedKeyUsage;

    /** The DNS names of subjectAltName. / subjectAltName 의 DNS 이름. */
    private final List<String> dnsNames = new ArrayList<String>();

    /** The IP addresses of subjectAltName. / subjectAltName 의 IP 주소. */
    private final List<byte[]> ipAddresses = new ArrayList<byte[]>();

    /** Encoded GeneralSubtree bases for permitted name constraints. / 허용 이름 제약의 부호화된 GeneralSubtree 기준. */
    private final List<byte[]> permitted = new ArrayList<byte[]>();

    /** Encoded GeneralSubtree bases for excluded name constraints. / 제외 이름 제약의 부호화된 GeneralSubtree 기준. */
    private final List<byte[]> excluded = new ArrayList<byte[]>();

    /** Extra extensions, already encoded. / 이미 부호화한 추가 확장. */
    private final List<byte[]> extra = new ArrayList<byte[]>();

    /** The signature kind of {@link TestKey}; 0 chooses by the issuer key. / {@link TestKey} 의 서명 종류. 0 이면 발급자 키에 따라 고른다. */
    private int signatureKind;

    /** The hash of the signature. / 서명의 해시. */
    private HashAlg signatureHash = HashAlg.SHA256;

    /** An OID that replaces the signature algorithm (for weak or unknown algorithms), or null. / 서명 알고리즘을 대신할 OID(약하거나 모르는 알고리즘용). 없으면 null. */
    private String signatureOidOverride;

    /** True to flip a bit of the signature. / 서명의 비트 하나를 뒤집으려면 true. */
    private boolean corrupt;

    /** The version field: 2 for v3, 0 to omit it (v1). / 버전 필드: v3 는 2, 생략(v1)하려면 0. */
    private int version = 2;

    /**
     * Starts a certificate for a subject with only a common name.
     * <p>
     * 일반 이름만 가진 주체의 인증서를 시작한다.
     *
     * @param commonName the common name of the subject / 주체의 일반 이름
     * @param key        the key of the subject / 주체의 키
     */
    public CertBuilder(String commonName, TestKey key) {
        this.subject = DerWriter.name(null, commonName);
        this.subjectKey = key;
    }

    /**
     * Sets the subject name with an organization in front of the common name.
     * <p>
     * 일반 이름 앞에 조직을 둔 주체 이름을 정한다.
     *
     * @param organization the organization / 조직
     * @param commonName   the common name / 일반 이름
     * @return this builder / 이 빌더
     */
    public CertBuilder subject(String organization, String commonName) {
        subject = DerWriter.name(organization, commonName);
        return this;
    }

    /**
     * Sets the issuer by common name and signing key.
     * <p>
     * 일반 이름과 서명 키로 발급자를 정한다.
     *
     * @param commonName the common name of the issuer / 발급자의 일반 이름
     * @param key        the key that signs / 서명하는 키
     * @return this builder / 이 빌더
     */
    public CertBuilder issuer(String commonName, TestKey key) {
        issuer = DerWriter.name(null, commonName);
        issuerKey = key;
        return this;
    }

    /**
     * Makes the certificate self-signed: issuer and signing key are those of the subject.
     * <p>
     * 인증서를 자체 서명으로 만든다: 발급자와 서명 키가 주체의 것이다.
     *
     * @return this builder / 이 빌더
     */
    public CertBuilder selfSigned() {
        issuer = subject;
        issuerKey = subjectKey;
        return this;
    }

    /**
     * Sets the validity.
     * <p>
     * 유효 기간을 정한다.
     *
     * @param from the start / 시작
     * @param to   the end / 끝
     * @return this builder / 이 빌더
     */
    public CertBuilder validity(long from, long to) {
        notBefore = from;
        notAfter = to;
        return this;
    }

    /**
     * Marks the certificate as a CA with key usage keyCertSign.
     * <p>
     * 인증서를 키 용도 keyCertSign 을 가진 CA 로 표시한다.
     *
     * @param pathLen the path length constraint, or -1 for none / 경로 길이 제약. 없으면 -1
     * @return this builder / 이 빌더
     */
    public CertBuilder ca(int pathLen) {
        ca = true;
        pathLength = pathLen;
        keyUsage = (1 << 5) | (1 << 6);
        return this;
    }

    /**
     * Sets the key usage bits (value <code>1 &lt;&lt; i</code> for bit i), or -1 to omit the extension.
     * <p>
     * 키 용도 비트를 정한다(i번 비트는 값 <code>1 &lt;&lt; i</code>). -1 이면 확장을 넣지 않는다.
     *
     * @param bits the bits / 비트
     * @return this builder / 이 빌더
     */
    public CertBuilder keyUsage(int bits) {
        keyUsage = bits;
        return this;
    }

    /**
     * Sets the extended key usage.
     * <p>
     * 확장 키 용도를 정한다.
     *
     * @param oids the key purpose OIDs / 키 목적 OID
     * @return this builder / 이 빌더
     */
    public CertBuilder eku(String... oids) {
        extendedKeyUsage = java.util.Arrays.asList(oids);
        return this;
    }

    /**
     * Adds DNS names to subjectAltName.
     * <p>
     * subjectAltName 에 DNS 이름을 더한다.
     *
     * @param names the names / 이름들
     * @return this builder / 이 빌더
     */
    public CertBuilder dns(String... names) {
        dnsNames.addAll(java.util.Arrays.asList(names));
        return this;
    }

    /**
     * Adds an IP address to subjectAltName.
     * <p>
     * subjectAltName 에 IP 주소를 더한다.
     *
     * @param address 4 or 16 bytes / 4 또는 16바이트
     * @return this builder / 이 빌더
     */
    public CertBuilder ip(byte[] address) {
        ipAddresses.add(address);
        return this;
    }

    /**
     * Adds a permitted DNS subtree to nameConstraints.
     * <p>
     * nameConstraints 에 허용 DNS 하위 트리를 더한다.
     *
     * @param subtree the subtree / 하위 트리
     * @return this builder / 이 빌더
     */
    public CertBuilder permitDns(String subtree) {
        permitted.add(DerWriter.ascii(0x82, subtree));
        return this;
    }

    /**
     * Adds an excluded DNS subtree to nameConstraints.
     * <p>
     * nameConstraints 에 제외 DNS 하위 트리를 더한다.
     *
     * @param subtree the subtree / 하위 트리
     * @return this builder / 이 빌더
     */
    public CertBuilder excludeDns(String subtree) {
        excluded.add(DerWriter.ascii(0x82, subtree));
        return this;
    }

    /**
     * Adds a permitted IP range to nameConstraints.
     * <p>
     * nameConstraints 에 허용 IP 범위를 더한다.
     *
     * @param addressAndMask address followed by mask / 주소와 그 뒤의 마스크
     * @return this builder / 이 빌더
     */
    public CertBuilder permitIp(byte[] addressAndMask) {
        permitted.add(DerWriter.tlv(0x87, addressAndMask));
        return this;
    }

    /**
     * Adds a permitted directory name subtree (organization only) to nameConstraints.
     * <p>
     * nameConstraints 에 허용 디렉터리 이름 하위 트리(조직만)를 더한다.
     *
     * @param organization the organization that subjects must start with / 주체가 시작해야 하는 조직
     * @return this builder / 이 빌더
     */
    public CertBuilder permitOrganization(String organization) {
        permitted.add(DerWriter.tlv(0xa4, DerWriter.seq(DerWriter.set(DerWriter.seq(DerWriter.oid("2.5.4.10"), DerWriter.utf8(organization))))));
        return this;
    }

    /**
     * Adds an extension with an arbitrary OID and an OCTET STRING of one NULL as value.
     * <p>
     * 임의의 OID 와, NULL 하나를 담은 OCTET STRING 값을 가진 확장을 더한다.
     *
     * @param oid      the extension OID / 확장 OID
     * @param critical the critical flag / critical 표시
     * @return this builder / 이 빌더
     */
    public CertBuilder extension(String oid, boolean critical) {
        extra.add(ext(oid, critical, DerWriter.nul()));
        return this;
    }

    /**
     * Chooses the signature algorithm.
     * <p>
     * 서명 알고리즘을 고른다.
     *
     * @param kind a kind of {@link TestKey} / {@link TestKey} 의 종류
     * @param hash the hash / 해시
     * @return this builder / 이 빌더
     */
    public CertBuilder signature(int kind, HashAlg hash) {
        signatureKind = kind;
        signatureHash = hash;
        return this;
    }

    /**
     * Declares another signature algorithm OID than the one really used (for example sha1WithRSAEncryption).
     * <p>
     * 실제로 쓴 것과 다른 서명 알고리즘 OID 를 선언한다(예: sha1WithRSAEncryption).
     *
     * @param oid the OID to declare / 선언할 OID
     * @return this builder / 이 빌더
     */
    public CertBuilder signatureOid(String oid) {
        signatureOidOverride = oid;
        return this;
    }

    /**
     * Flips a bit of the signature value.
     * <p>
     * 서명 값의 비트 하나를 뒤집는다.
     *
     * @return this builder / 이 빌더
     */
    public CertBuilder corruptSignature() {
        corrupt = true;
        return this;
    }

    /**
     * Builds a version 1 certificate (no version field, no extensions).
     * <p>
     * 버전 1 인증서를 만든다(버전 필드와 확장 없음).
     *
     * @return this builder / 이 빌더
     */
    public CertBuilder version1() {
        version = 0;
        return this;
    }

    /**
     * Encodes an Extension.
     * <p>
     * Extension 을 부호화한다.
     *
     * @param oid      the OID / OID
     * @param critical the critical flag / critical 표시
     * @param value    the encoded value / 부호화된 값
     * @return the element / 원소
     */
    private static byte[] ext(String oid, boolean critical, byte[] value) {
        if (critical) {
            return DerWriter.seq(DerWriter.oid(oid), DerWriter.bool(true), DerWriter.octets(value));
        }
        return DerWriter.seq(DerWriter.oid(oid), DerWriter.octets(value));
    }

    /**
     * Joins encoded elements.
     * <p>
     * 부호화된 원소들을 잇는다.
     *
     * @param parts the elements / 원소들
     * @return the concatenation / 이은 결과
     */
    private static byte[] join(List<byte[]> parts) {
        return Bytes.concat(parts.toArray(new byte[parts.size()][]));
    }

    /**
     * Wraps the bases of name constraints into GeneralSubtree elements.
     * <p>
     * 이름 제약의 기준들을 GeneralSubtree 원소로 감싼다.
     *
     * @param bases the encoded GeneralName bases / 부호화된 GeneralName 기준들
     * @return the subtrees / 하위 트리들
     */
    private static byte[] subtrees(List<byte[]> bases) {
        List<byte[]> out = new ArrayList<byte[]>();
        // 기준마다 SEQUENCE 로 감싼다.
        for (int i = 0; i < bases.size(); i++) {
            out.add(DerWriter.seq(bases.get(i)));
        }
        return join(out);
    }

    /**
     * Builds and signs the certificate.
     * <p>
     * 인증서를 만들고 서명한다.
     *
     * @return the DER encoding / DER 부호화
     */
    public byte[] build() {
        if (issuerKey == null) {
            throw new IllegalStateException("no issuer was set");
        }
        int kind = signatureKind != 0 ? signatureKind : issuerKey.isRsa() ? TestKey.PKCS1 : TestKey.ECDSA;
        byte[] alg;
        String h = signatureHash == HashAlg.SHA256 ? "1" : signatureHash == HashAlg.SHA384 ? "2" : "3";
        if (signatureOidOverride != null) {
            alg = DerWriter.seq(DerWriter.oid(signatureOidOverride), DerWriter.nul());
        } else if (kind == TestKey.PKCS1) {
            alg = DerWriter.seq(DerWriter.oid("1.2.840.113549.1.1.1" + h), DerWriter.nul());
        } else if (kind == TestKey.ECDSA) {
            alg = DerWriter.seq(DerWriter.oid("1.2.840.10045.4.3." + (Integer.parseInt(h) + 1)));
        } else {
            // RSASSA-PSS-params: 해시, 같은 해시의 MGF1, 해시 길이의 salt.
            byte[] hashAlg = DerWriter.seq(DerWriter.oid("2.16.840.1.101.3.4.2." + h), DerWriter.nul());
            alg = DerWriter.seq(DerWriter.oid("1.2.840.113549.1.1.10"), DerWriter.seq(DerWriter.tlv(0xa0, hashAlg),
                    DerWriter.tlv(0xa1, DerWriter.seq(DerWriter.oid("1.2.840.113549.1.1.8"), hashAlg)),
                    DerWriter.tlv(0xa2, DerWriter.integer(signatureHash.length()))));
        }
        List<byte[]> exts = new ArrayList<byte[]>();
        if (ca) {
            exts.add(ext("2.5.29.19", true, pathLength >= 0 ? DerWriter.seq(DerWriter.bool(true), DerWriter.integer(pathLength)) : DerWriter.seq(DerWriter.bool(true))));
        }
        if (keyUsage >= 0) {
            exts.add(ext("2.5.29.15", true, DerWriter.namedBits(keyUsage)));
        }
        if (extendedKeyUsage != null) {
            List<byte[]> ids = new ArrayList<byte[]>();
            // 키 목적 OID 를 차례로 넣는다.
            for (int i = 0; i < extendedKeyUsage.size(); i++) {
                ids.add(DerWriter.oid(extendedKeyUsage.get(i)));
            }
            exts.add(ext("2.5.29.37", false, DerWriter.seq(join(ids))));
        }
        if (!dnsNames.isEmpty() || !ipAddresses.isEmpty()) {
            List<byte[]> names = new ArrayList<byte[]>();
            // DNS 이름은 [2], IP 주소는 [7] 로 넣는다.
            for (int i = 0; i < dnsNames.size(); i++) {
                names.add(DerWriter.ascii(0x82, dnsNames.get(i)));
            }
            // IP 주소를 차례로 넣는다.
            for (int i = 0; i < ipAddresses.size(); i++) {
                names.add(DerWriter.tlv(0x87, ipAddresses.get(i)));
            }
            exts.add(ext("2.5.29.17", false, DerWriter.seq(join(names))));
        }
        if (!permitted.isEmpty() || !excluded.isEmpty()) {
            List<byte[]> nc = new ArrayList<byte[]>();
            if (!permitted.isEmpty()) {
                nc.add(DerWriter.tlv(0xa0, subtrees(permitted)));
            }
            if (!excluded.isEmpty()) {
                nc.add(DerWriter.tlv(0xa1, subtrees(excluded)));
            }
            exts.add(ext("2.5.29.30", true, DerWriter.seq(join(nc))));
        }
        exts.addAll(extra);
        List<byte[]> tbs = new ArrayList<byte[]>();
        if (version != 0) {
            tbs.add(DerWriter.tlv(0xa0, DerWriter.integer(version)));
        }
        long serial;
        synchronized (CertBuilder.class) {
            serial = nextSerial++;
        }
        tbs.add(DerWriter.integer(BigInteger.valueOf(serial)));
        tbs.add(alg);
        tbs.add(issuer);
        tbs.add(DerWriter.seq(DerWriter.time(notBefore), DerWriter.time(notAfter)));
        tbs.add(subject);
        tbs.add(subjectKey.spki());
        if (version != 0 && !exts.isEmpty()) {
            tbs.add(DerWriter.tlv(0xa3, DerWriter.seq(join(exts))));
        }
        byte[] tbsBytes = DerWriter.seq(join(tbs));
        byte[] sig = issuerKey.sign(kind, signatureHash, tbsBytes);
        if (corrupt) {
            sig[sig.length - 1] ^= 1;
        }
        return DerWriter.seq(tbsBytes, alg, DerWriter.bits(sig));
    }
}
