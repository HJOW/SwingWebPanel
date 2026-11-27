package org.duckdns.hjow.swingwebpanel.tls13.x509;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * A parsed X.509 version 3 certificate (RFC 5280) with the fields that TLS server authentication needs: names, validity, key, signature
 * and the extensions basicConstraints, keyUsage, extendedKeyUsage, subjectAltName and nameConstraints. The object is immutable.
 * A damaged encoding gives an {@link X509Exception} with the reason MALFORMED and never another exception. Example:
 * <pre>
 * X509Cert cert = X509Cert.parse(der);
 * List&lt;String&gt; names = cert.dnsNames();
 * </pre>
 * <p>
 * 해석한 X.509 버전 3 인증서(RFC 5280). TLS 서버 인증에 필요한 필드를 가진다: 이름, 유효 기간, 키, 서명, 그리고 확장 basicConstraints, keyUsage,
 * extendedKeyUsage, subjectAltName, nameConstraints. 객체는 변경할 수 없다. 손상된 부호화는 이유가 MALFORMED 인 {@link X509Exception} 이 되고
 * 다른 예외는 나오지 않는다.
 */
public final class X509Cert {

    /** KeyUsage bit digitalSignature. / KeyUsage 비트 digitalSignature. */
    public static final int KU_DIGITAL_SIGNATURE = 1;

    /** KeyUsage bit keyCertSign. / KeyUsage 비트 keyCertSign. */
    public static final int KU_KEY_CERT_SIGN = 1 << 5;

    /** The object identifier of the extended key usage serverAuth. / 확장 키 용도 serverAuth 의 객체 식별자. */
    public static final String EKU_SERVER_AUTH = "1.3.6.1.5.5.7.3.1";

    /** The object identifier of anyExtendedKeyUsage. / anyExtendedKeyUsage 의 객체 식별자. */
    public static final String EKU_ANY = "2.5.29.37.0";

    /** The largest certificate that is parsed. / 해석하는 인증서의 최대 크기. */
    private static final int MAX_SIZE = 64 * 1024;

    /** The whole certificate. / 인증서 전체. */
    private final byte[] encoded;

    /** The encoded TBSCertificate (the signed part). / 부호화된 TBSCertificate(서명 대상). */
    private final byte[] tbs;

    /** The serial number. / 일련번호. */
    private final BigInteger serial;

    /** The encoded issuer name. / 부호화된 발급자 이름. */
    private final byte[] issuer;

    /** The encoded subject name. / 부호화된 주체 이름. */
    private final byte[] subject;

    /** The start of the validity in milliseconds since 1970. / 유효 기간의 시작(1970년부터의 밀리초). */
    private final long notBefore;

    /** The end of the validity in milliseconds since 1970. / 유효 기간의 끝(1970년부터의 밀리초). */
    private final long notAfter;

    /** The public key. / 공개 키. */
    private final SubjectKey key;

    /** The signature algorithm, or null if it is weak or unsupported. / 서명 알고리즘. 약하거나 지원하지 않으면 null. */
    private final SignatureAlgorithm signatureAlgorithm;

    /** The reason why the signature algorithm cannot be used, or null. / 서명 알고리즘을 쓸 수 없는 이유. 없으면 null. */
    private final X509Exception signatureProblem;

    /** The signature value. / 서명 값. */
    private final byte[] signature;

    /** True if basicConstraints says cA. / basicConstraints 가 cA 이면 true. */
    private boolean ca;

    /** The pathLenConstraint, or -1 if absent. / pathLenConstraint. 없으면 -1. */
    private int pathLength = -1;

    /** The KeyUsage bits, or -1 if the extension is absent. / KeyUsage 비트. 확장이 없으면 -1. */
    private int keyUsage = -1;

    /** The extended key usage OIDs, or null if the extension is absent. / 확장 키 용도 OID. 확장이 없으면 null. */
    private List<String> extendedKeyUsage;

    /** The dNSName entries of subjectAltName. / subjectAltName 의 dNSName 항목. */
    private final List<String> dnsNames = new ArrayList<String>();

    /** The iPAddress entries of subjectAltName (4 or 16 bytes each). / subjectAltName 의 iPAddress 항목(각 4 또는 16바이트). */
    private final List<byte[]> ipAddresses = new ArrayList<byte[]>();

    /** The name constraints, or null if the extension is absent. / 이름 제약. 확장이 없으면 null. */
    private NameConstraints nameConstraints;

    /** The OID of a critical extension that is not understood, or null. / 이해하지 못하는 critical 확장의 OID. 없으면 null. */
    private String unknownCritical;

    /**
     * Parses a certificate.
     * <p>
     * 인증서를 해석한다.
     *
     * @param der the DER encoding / DER 부호화
     * @throws X509Exception if the encoding is not a well-formed certificate / 부호화가 올바른 인증서가 아니면
     */
    private X509Cert(byte[] der) throws X509Exception {
        if (der.length > MAX_SIZE) {
            throw Der.malformed("certificate of " + der.length + " bytes");
        }
        encoded = der.clone();
        List<Der> top = Der.readExactly(encoded, 0, encoded.length).expect(Der.SEQUENCE).children(3);
        if (top.size() != 3) {
            throw Der.malformed("Certificate must have three elements");
        }
        tbs = top.get(0).expect(Der.SEQUENCE).encoded();
        signature = top.get(2).bitStringBytes();
        List<Der> f = top.get(0).children(10);
        int i = 0;
        // 버전: [0] EXPLICIT INTEGER. 확장을 쓰려면 v3(값 2)이어야 한다. 없으면 v1 이다.
        boolean v3 = false;
        if (i < f.size() && f.get(i).tag() == 0xa0) {
            BigInteger version = f.get(i).inner().integer();
            if (version.signum() < 0 || version.compareTo(BigInteger.valueOf(2)) > 0) {
                throw Der.malformed("certificate version " + version);
            }
            v3 = version.intValue() == 2;
            i++;
        }
        if (f.size() < i + 6) {
            throw Der.malformed("TBSCertificate is too short");
        }
        serial = f.get(i++).integer();
        Der innerAlg = f.get(i++);
        if (!java.util.Arrays.equals(innerAlg.encoded(), top.get(1).encoded())) {
            throw Der.malformed("the two signature algorithm fields differ");
        }
        SignatureAlgorithm alg = null;
        X509Exception problem = null;
        try {
            alg = SignatureAlgorithm.parse(innerAlg);
        } catch (X509Exception e) {
            // 약하거나 모르는 알고리즘은 이 인증서의 서명을 검증해야 할 때에만 문제가 된다(루트의 자체 서명은 검증하지 않는다).
            if (e.reason() == X509Exception.Reason.MALFORMED) {
                throw e;
            }
            problem = e;
        }
        signatureAlgorithm = alg;
        signatureProblem = problem;
        issuer = f.get(i++).expect(Der.SEQUENCE).encoded();
        List<Der> validity = f.get(i++).expect(Der.SEQUENCE).children(2);
        if (validity.size() != 2) {
            throw Der.malformed("Validity must have two times");
        }
        notBefore = time(validity.get(0));
        notAfter = time(validity.get(1));
        subject = f.get(i++).expect(Der.SEQUENCE).encoded();
        key = new SubjectKey(f.get(i++));
        // 남은 선택 필드를 차례로 본다: issuerUniqueID [1], subjectUniqueID [2], extensions [3].
        while (i < f.size()) {
            Der opt = f.get(i++);
            boolean last = i == f.size();
            if (opt.tag() == 0xa3 && v3 && last) {
                extensions(opt.inner().expect(Der.SEQUENCE).children(64));
            } else if (opt.tag() != 0x81 && opt.tag() != 0x82 && opt.tag() != 0xa1 && opt.tag() != 0xa2) {
                throw Der.malformed("unexpected TBSCertificate field " + Integer.toHexString(opt.tag()));
            }
        }
    }

    /**
     * Parses a certificate.
     * <p>
     * 인증서를 해석한다.
     *
     * @param der the DER encoding / DER 부호화
     * @return the certificate / 인증서
     * @throws X509Exception with the reason MALFORMED if the encoding is damaged / 부호화가 손상되었으면 이유가 MALFORMED 인 예외
     */
    public static X509Cert parse(byte[] der) throws X509Exception {
        try {
            return new X509Cert(der);
        } catch (RuntimeException e) {
            // 방어: 해석기의 범위 검사를 빠져나간 입력도 검증 실패로 끝낸다.
            throw Der.malformed("unexpected " + e);
        }
    }

    /**
     * Reads a UTCTime or GeneralizedTime in the form that RFC 5280 requires (seconds present, time zone Z).
     * <p>
     * RFC 5280 이 요구하는 형식(초 포함, 시간대 Z)의 UTCTime 또는 GeneralizedTime 을 읽는다.
     *
     * @param t the element / 원소
     * @return milliseconds since 1970 / 1970년부터의 밀리초
     * @throws X509Exception if the time is malformed / 시간이 잘못되었으면
     */
    private static long time(Der t) throws X509Exception {
        String s = t.ascii();
        int digits;
        if (t.tag() == Der.UTC_TIME && s.length() == 13) {
            digits = 12;
        } else if (t.tag() == Der.GENERALIZED_TIME && s.length() == 15) {
            digits = 14;
        } else {
            throw Der.malformed("bad time " + s);
        }
        // 마지막 글자는 Z 이고 나머지는 숫자여야 한다.
        for (int i = 0; i < digits; i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                throw Der.malformed("bad time " + s);
            }
        }
        if (s.charAt(digits) != 'Z') {
            throw Der.malformed("time without Z " + s);
        }
        int year;
        int at;
        if (digits == 12) {
            int yy = Integer.parseInt(s.substring(0, 2));
            year = yy >= 50 ? 1900 + yy : 2000 + yy;
            at = 2;
        } else {
            year = Integer.parseInt(s.substring(0, 4));
            at = 4;
        }
        int month = Integer.parseInt(s.substring(at, at + 2));
        int day = Integer.parseInt(s.substring(at + 2, at + 4));
        int hour = Integer.parseInt(s.substring(at + 4, at + 6));
        int minute = Integer.parseInt(s.substring(at + 6, at + 8));
        int second = Integer.parseInt(s.substring(at + 8, at + 10));
        if (month < 1 || month > 12 || day < 1 || day > 31 || hour > 23 || minute > 59 || second > 59) {
            throw Der.malformed("bad time " + s);
        }
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(year, month - 1, day, hour, minute, second);
        return c.getTimeInMillis();
    }

    /**
     * Reads the extensions.
     * <p>
     * 확장들을 읽는다.
     *
     * @param list the Extension elements / Extension 원소들
     * @throws X509Exception if an extension is malformed or repeated / 확장이 잘못되었거나 되풀이되면
     */
    private void extensions(List<Der> list) throws X509Exception {
        List<String> seen = new ArrayList<String>();
        // 확장마다 읽는다: Extension ::= SEQUENCE { extnID, critical BOOLEAN DEFAULT FALSE, extnValue OCTET STRING }
        for (int i = 0; i < list.size(); i++) {
            List<Der> e = list.get(i).expect(Der.SEQUENCE).children(3);
            if (e.size() < 2) {
                throw Der.malformed("Extension is too short");
            }
            String oid = e.get(0).oid();
            if (seen.contains(oid)) {
                throw Der.malformed("extension " + oid + " appears twice");
            }
            seen.add(oid);
            boolean critical = e.size() == 3 && e.get(1).bool();
            byte[] value = e.get(e.size() - 1).expect(Der.OCTET_STRING).content();
            Der v = value.length == 0 ? null : Der.readExactly(value, 0, value.length);
            if (v == null) {
                throw Der.malformed("empty extension value");
            }
            if ("2.5.29.19".equals(oid)) {
                basicConstraints(v);
            } else if ("2.5.29.15".equals(oid)) {
                keyUsage = v.namedBits();
            } else if ("2.5.29.37".equals(oid)) {
                List<Der> ids = v.expect(Der.SEQUENCE).children(64);
                List<String> usages = new ArrayList<String>();
                // KeyPurposeId 를 차례로 모은다.
                for (int k = 0; k < ids.size(); k++) {
                    usages.add(ids.get(k).oid());
                }
                extendedKeyUsage = usages;
            } else if ("2.5.29.17".equals(oid)) {
                subjectAltName(v);
            } else if ("2.5.29.30".equals(oid)) {
                nameConstraints = new NameConstraints(v);
            } else if (critical && !"2.5.29.32".equals(oid) && !"2.5.29.36".equals(oid) && !"2.5.29.54".equals(oid) && !"2.5.29.33".equals(oid)) {
                // 정책 관련 확장(certificatePolicies, policyConstraints, inhibitAnyPolicy, policyMappings)은 critical 이어도 받아들인다(정책은 강제하지 않는다).
                if (unknownCritical == null) {
                    unknownCritical = oid;
                }
            }
        }
    }

    /**
     * Reads basicConstraints: <code>SEQUENCE { cA BOOLEAN DEFAULT FALSE, pathLenConstraint INTEGER OPTIONAL }</code>.
     * <p>
     * basicConstraints 를 읽는다: <code>SEQUENCE { cA BOOLEAN DEFAULT FALSE, pathLenConstraint INTEGER OPTIONAL }</code>.
     *
     * @param v the extension value / 확장 값
     * @throws X509Exception if the value is malformed / 값이 잘못되었으면
     */
    private void basicConstraints(Der v) throws X509Exception {
        List<Der> parts = v.expect(Der.SEQUENCE).children(2);
        int at = 0;
        if (at < parts.size() && parts.get(at).tag() == Der.BOOLEAN) {
            ca = parts.get(at++).bool();
        }
        if (at < parts.size()) {
            BigInteger len = parts.get(at++).integer();
            if (len.signum() < 0) {
                throw Der.malformed("negative path length");
            }
            pathLength = len.bitLength() > 16 ? 0xffff : len.intValue();
        }
        if (at != parts.size()) {
            throw Der.malformed("unexpected basicConstraints content");
        }
    }

    /**
     * Reads subjectAltName and keeps the dNSName and iPAddress entries.
     * <p>
     * subjectAltName 을 읽고 dNSName 과 iPAddress 항목을 보관한다.
     *
     * @param v the extension value / 확장 값
     * @throws X509Exception if the value is malformed / 값이 잘못되었으면
     */
    private void subjectAltName(Der v) throws X509Exception {
        List<Der> names = v.expect(Der.SEQUENCE).children(2048);
        // GeneralName 마다 종류를 본다: [2] dNSName, [7] iPAddress.
        for (int i = 0; i < names.size(); i++) {
            Der n = names.get(i);
            if (n.tag() == 0x82) {
                dnsNames.add(n.ascii());
            } else if (n.tag() == 0x87) {
                if (n.length() != 4 && n.length() != 16) {
                    throw Der.malformed("iPAddress of " + n.length() + " bytes");
                }
                ipAddresses.add(n.content());
            }
        }
    }

    /**
     * Returns a copy of the DER encoding.
     * <p>
     * DER 부호화의 사본을 반환한다.
     *
     * @return the encoding / 부호화
     */
    public byte[] encoded() {
        return encoded.clone();
    }

    /**
     * Returns the signed part (TBSCertificate).
     * <p>
     * 서명 대상(TBSCertificate)을 반환한다.
     *
     * @return the encoding / 부호화
     */
    public byte[] tbs() {
        return tbs.clone();
    }

    /**
     * Returns the serial number.
     * <p>
     * 일련번호를 반환한다.
     *
     * @return the serial number / 일련번호
     */
    public BigInteger serial() {
        return serial;
    }

    /**
     * Returns the encoded issuer name.
     * <p>
     * 부호화된 발급자 이름을 반환한다.
     *
     * @return the Name element / Name 원소
     */
    public byte[] issuer() {
        return issuer.clone();
    }

    /**
     * Returns the encoded subject name.
     * <p>
     * 부호화된 주체 이름을 반환한다.
     *
     * @return the Name element / Name 원소
     */
    public byte[] subject() {
        return subject.clone();
    }

    /**
     * Tells if the issuer name of this certificate is the subject name of another one (byte for byte).
     * <p>
     * 이 인증서의 발급자 이름이 다른 인증서의 주체 이름과 같은지(바이트 단위) 알려 준다.
     *
     * @param subjectName an encoded subject name / 부호화된 주체 이름
     * @return true if equal / 같으면 true
     */
    public boolean isIssuedBy(byte[] subjectName) {
        return java.util.Arrays.equals(issuer, subjectName);
    }

    /**
     * Tells if issuer and subject are the same name.
     * <p>
     * 발급자와 주체가 같은 이름인지 알려 준다.
     *
     * @return true if self-issued / 자체 발급이면 true
     */
    public boolean isSelfIssued() {
        return java.util.Arrays.equals(issuer, subject);
    }

    /**
     * Returns the start of the validity.
     * <p>
     * 유효 기간의 시작을 반환한다.
     *
     * @return milliseconds since 1970 / 1970년부터의 밀리초
     */
    public long notBefore() {
        return notBefore;
    }

    /**
     * Returns the end of the validity.
     * <p>
     * 유효 기간의 끝을 반환한다.
     *
     * @return milliseconds since 1970 / 1970년부터의 밀리초
     */
    public long notAfter() {
        return notAfter;
    }

    /**
     * Returns the public key.
     * <p>
     * 공개 키를 반환한다.
     *
     * @return the key / 키
     */
    public SubjectKey key() {
        return key;
    }

    /**
     * Returns the signature algorithm.
     * <p>
     * 서명 알고리즘을 반환한다.
     *
     * @return the algorithm / 알고리즘
     * @throws X509Exception WEAK_SIGNATURE or UNSUPPORTED_ALGORITHM if the algorithm cannot be used / 알고리즘을 쓸 수 없으면
     */
    public SignatureAlgorithm signatureAlgorithm() throws X509Exception {
        if (signatureAlgorithm == null) {
            throw new X509Exception(signatureProblem.reason(), signatureProblem.getMessage());
        }
        return signatureAlgorithm;
    }

    /**
     * Returns the signature value.
     * <p>
     * 서명 값을 반환한다.
     *
     * @return the signature / 서명
     */
    public byte[] signature() {
        return signature.clone();
    }

    /**
     * Tells if basicConstraints marks a certification authority.
     * <p>
     * basicConstraints 가 인증 기관으로 표시하는지 알려 준다.
     *
     * @return true for a CA / CA 이면 true
     */
    public boolean isCa() {
        return ca;
    }

    /**
     * Returns the path length constraint.
     * <p>
     * 경로 길이 제약을 반환한다.
     *
     * @return the constraint, or -1 if absent / 제약. 없으면 -1
     */
    public int pathLength() {
        return pathLength;
    }

    /**
     * Tells if the key usage allows a use. A certificate without the extension allows every use.
     * <p>
     * 키 용도가 그 사용을 허락하는지 알려 준다. 확장이 없는 인증서는 모든 사용을 허락한다.
     *
     * @param bit {@link #KU_DIGITAL_SIGNATURE} or {@link #KU_KEY_CERT_SIGN} / 용도 비트
     * @return true if allowed / 허락하면 true
     */
    public boolean allowsKeyUsage(int bit) {
        return keyUsage < 0 || (keyUsage & bit) != 0;
    }

    /**
     * Tells if the extended key usage allows TLS server authentication. A certificate without the extension allows it.
     * <p>
     * 확장 키 용도가 TLS 서버 인증을 허락하는지 알려 준다. 확장이 없는 인증서는 허락한다.
     *
     * @return true if allowed / 허락하면 true
     */
    public boolean allowsServerAuth() {
        return extendedKeyUsage == null || extendedKeyUsage.contains(EKU_SERVER_AUTH) || extendedKeyUsage.contains(EKU_ANY);
    }

    /**
     * Returns the dNSName entries of subjectAltName.
     * <p>
     * subjectAltName 의 dNSName 항목을 반환한다.
     *
     * @return the names / 이름 목록
     */
    public List<String> dnsNames() {
        return Collections.unmodifiableList(dnsNames);
    }

    /**
     * Returns the iPAddress entries of subjectAltName.
     * <p>
     * subjectAltName 의 iPAddress 항목을 반환한다.
     *
     * @return the addresses of 4 or 16 bytes / 4 또는 16바이트 주소 목록
     */
    public List<byte[]> ipAddresses() {
        List<byte[]> out = new ArrayList<byte[]>();
        // 배열은 바꿀 수 있으므로 사본을 준다.
        for (int i = 0; i < ipAddresses.size(); i++) {
            out.add(ipAddresses.get(i).clone());
        }
        return out;
    }

    /**
     * Returns the name constraints.
     * <p>
     * 이름 제약을 반환한다.
     *
     * @return the constraints, or null if absent / 제약. 없으면 null
     */
    public NameConstraints nameConstraints() {
        return nameConstraints;
    }

    /**
     * Returns the OID of the first critical extension that is not understood.
     * <p>
     * 이해하지 못하는 첫 critical 확장의 OID 를 반환한다.
     *
     * @return the OID, or null if there is none / OID. 없으면 null
     */
    public String unknownCriticalExtension() {
        return unknownCritical;
    }
}
