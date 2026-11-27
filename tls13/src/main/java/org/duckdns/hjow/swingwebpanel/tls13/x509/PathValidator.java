package org.duckdns.hjow.swingwebpanel.tls13.x509;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and validates the certification path of a TLS server (RFC 5280 section 6, with the restrictions that browsers apply). The
 * server sends its certificate first and then intermediate certificates in any order; a path is searched from the server certificate up
 * to a root of the trust store. Checked: the host name, validity dates, signatures, CA flag, key usage, extended key usage, path length,
 * name constraints, key sizes (RSA of at least 2048 bits, EC on P-256, P-384 or P-521), no SHA-1 or MD5 signatures, no unknown critical
 * extensions, and the restrictions of the root program (distrust date, applied name constraints). Revocation (OCSP, CRL) is
 * <b>not</b> checked. Example:
 * <pre>
 * List&lt;X509Cert&gt; path = PathValidator.validate(chainFromServer, TrustStore.mozilla(), "www.example.com", System.currentTimeMillis());
 * SubjectKey serverKey = path.get(0).key();
 * </pre>
 * <p>
 * TLS 서버의 인증 경로를 구성하고 검증한다(RFC 5280 6절, 브라우저가 적용하는 제한 포함). 서버는 자기 인증서를 먼저 보내고 중간 인증서를 임의의 순서로
 * 보낸다. 서버 인증서에서 신뢰 저장소의 루트까지 경로를 찾는다. 검사 항목: 호스트 이름, 유효 기간, 서명, CA 표시, 키 용도, 확장 키 용도, 경로 길이,
 * 이름 제약, 키 크기(RSA 2048비트 이상, EC 는 P-256·P-384·P-521), SHA-1·MD5 서명 금지, 모르는 critical 확장 금지, 루트 프로그램의 제한(불신 날짜,
 * 적용 이름 제약). 폐기(OCSP, CRL)는 검사하지 <b>않는다</b>.
 */
public final class PathValidator {

    /** The largest number of certificates accepted from the server. / 서버에서 받아들이는 인증서의 최대 수. */
    public static final int MAX_CHAIN = 10;

    /** The largest number of certificates in a path, the root not counted. / 경로의 인증서 최대 수(루트 제외). */
    public static final int MAX_DEPTH = 8;

    /** The smallest RSA modulus accepted. / 받아들이는 가장 작은 RSA 법. */
    public static final int MIN_RSA_BITS = 2048;

    /** The largest number of signature checks in one search. / 탐색 한 번의 서명 검사 최대 횟수. */
    private static final int MAX_SIGNATURE_CHECKS = 48;

    /** The certificates sent by the server. / 서버가 보낸 인증서들. */
    private final List<X509Cert> certs;

    /** The trust store. / 신뢰 저장소. */
    private final TrustStore store;

    /** The time of the check. / 검사 시각. */
    private final long now;

    /** The failure of the deepest attempt, reported if no path is found. / 가장 깊이 간 시도의 실패. 경로를 찾지 못하면 이것을 보고한다. */
    private X509Exception best;

    /** The depth of that attempt. / 그 시도의 깊이. */
    private int bestDepth = -1;

    /** The number of signature checks made. / 지금까지 한 서명 검사 횟수. */
    private int signatureChecks;

    /**
     * Creates a search.
     * <p>
     * 탐색을 만든다.
     *
     * @param certs the parsed certificates of the server / 서버의 해석한 인증서들
     * @param store the trust store / 신뢰 저장소
     * @param now   the time of the check / 검사 시각
     */
    private PathValidator(List<X509Cert> certs, TrustStore store, long now) {
        this.certs = certs;
        this.store = store;
        this.now = now;
    }

    /**
     * Validates the certificates of a server.
     * <p>
     * 서버의 인증서들을 검증한다.
     *
     * @param chain the DER certificates as the server sent them, the server certificate first / 서버가 보낸 DER 인증서들. 서버 인증서가 먼저
     * @param store the trusted roots / 신뢰하는 루트
     * @param host  the host name or IP literal that the client connects to, or null to skip the name check (never null for TLS clients) /
     *              클라이언트가 연결하는 호스트 이름 또는 IP 리터럴. null 이면 이름 검사를 건너뛴다(TLS 클라이언트는 null 을 주지 않는다)
     * @param now   the current time in milliseconds since 1970 / 현재 시각(1970년부터의 밀리초)
     * @return the path from the server certificate up to the certificate signed by the root / 서버 인증서부터 루트가 서명한 인증서까지의 경로
     * @throws X509Exception with the reason of the rejection / 거부 이유를 가진 예외
     */
    public static List<X509Cert> validate(List<byte[]> chain, TrustStore store, String host, long now) throws X509Exception {
        if (chain.isEmpty()) {
            throw Der.malformed("the server sent no certificate");
        }
        if (chain.size() > MAX_CHAIN) {
            throw new X509Exception(X509Exception.Reason.TOO_LONG, chain.size() + " certificates");
        }
        List<X509Cert> certs = new ArrayList<X509Cert>();
        // 받은 인증서를 모두 해석한다.
        for (int i = 0; i < chain.size(); i++) {
            certs.add(X509Cert.parse(chain.get(i)));
        }
        X509Cert leaf = certs.get(0);
        PathValidator v = new PathValidator(certs, store, now);
        v.checkCommon(leaf);
        if (host != null && !HostNames.matches(leaf, host)) {
            throw new X509Exception(X509Exception.Reason.NAME_MISMATCH, "the certificate does not name " + host);
        }
        if (!leaf.allowsServerAuth()) {
            throw new X509Exception(X509Exception.Reason.EXTENDED_KEY_USAGE, "the certificate is not for server authentication");
        }
        if (!leaf.allowsKeyUsage(X509Cert.KU_DIGITAL_SIGNATURE)) {
            throw new X509Exception(X509Exception.Reason.KEY_USAGE, "the certificate key may not sign");
        }
        List<X509Cert> path = new ArrayList<X509Cert>();
        path.add(leaf);
        List<TrustRoot> same = store.findBySubject(leaf.subject());
        // 서버 인증서 자체가 신뢰 저장소에 있으면(호스트가 직접 신뢰한 인증서) 그것으로 끝난다.
        for (int i = 0; i < same.size(); i++) {
            if (java.util.Arrays.equals(same.get(i).certificate().encoded(), leaf.encoded())) {
                return path;
            }
        }
        if (v.search(path)) {
            return path;
        }
        if (v.best != null) {
            throw v.best;
        }
        throw new X509Exception(X509Exception.Reason.UNTRUSTED, "no path to a trusted root");
    }

    /**
     * Remembers the failure of an attempt if it went deeper than the ones before.
     * <p>
     * 시도의 실패를, 앞의 것보다 깊이 갔으면 기억한다.
     *
     * @param e     the failure / 실패
     * @param depth the number of certificates in the path at that point / 그 시점의 경로 인증서 수
     */
    private void remember(X509Exception e, int depth) {
        if (depth > bestDepth) {
            best = e;
            bestDepth = depth;
        }
    }

    /**
     * Checks what every certificate of a path must satisfy: validity, no unknown critical extension and a strong, supported key.
     * <p>
     * 경로의 모든 인증서가 만족해야 하는 것을 검사한다: 유효 기간, 모르는 critical 확장 없음, 강하고 지원하는 키.
     *
     * @param c the certificate / 인증서
     * @throws X509Exception if a check fails / 검사가 실패하면
     */
    private void checkCommon(X509Cert c) throws X509Exception {
        if (now < c.notBefore()) {
            throw new X509Exception(X509Exception.Reason.NOT_YET_VALID, "certificate is not valid before " + new java.util.Date(c.notBefore()));
        }
        if (now > c.notAfter()) {
            throw new X509Exception(X509Exception.Reason.EXPIRED, "certificate expired on " + new java.util.Date(c.notAfter()));
        }
        if (c.unknownCriticalExtension() != null) {
            throw new X509Exception(X509Exception.Reason.UNKNOWN_CRITICAL_EXTENSION, "extension " + c.unknownCriticalExtension());
        }
        SubjectKey k = c.key();
        if (!k.isRsa() && !k.isEc()) {
            throw new X509Exception(X509Exception.Reason.UNSUPPORTED_ALGORITHM, "key algorithm " + k.algorithm());
        }
        if (k.isRsa() && k.bits() < MIN_RSA_BITS) {
            throw new X509Exception(X509Exception.Reason.WEAK_KEY, "RSA key of " + k.bits() + " bits");
        }
    }

    /**
     * Verifies that a certificate was signed with a key.
     * <p>
     * 인증서가 그 키로 서명되었는지 검증한다.
     *
     * @param cert the certificate / 인증서
     * @param key  the key of the issuer / 발급자의 키
     * @throws X509Exception if the algorithm is weak or unsupported or the signature is wrong / 알고리즘이 약하거나 지원하지 않거나 서명이 틀리면
     */
    private void verifySignature(X509Cert cert, SubjectKey key) throws X509Exception {
        SignatureAlgorithm alg = cert.signatureAlgorithm();
        if (++signatureChecks > MAX_SIGNATURE_CHECKS) {
            throw new X509Exception(X509Exception.Reason.TOO_LONG, "too many candidate paths");
        }
        if (!alg.verify(key, cert.tbs(), cert.signature())) {
            throw new X509Exception(X509Exception.Reason.BAD_SIGNATURE, "the signature of a certificate is wrong");
        }
    }

    /**
     * Counts the CA certificates of a path that count for a path length constraint: all but the server certificate and self-issued ones.
     * <p>
     * 경로 길이 제약에 세는 경로의 CA 인증서 수를 센다: 서버 인증서와 자체 발급 인증서를 뺀 것.
     *
     * @param path the path so far / 지금까지의 경로
     * @return the count / 개수
     */
    private static int intermediates(List<X509Cert> path) {
        int n = 0;
        // 서버 인증서(0번)를 빼고 센다.
        for (int i = 1; i < path.size(); i++) {
            if (!path.get(i).isSelfIssued()) {
                n++;
            }
        }
        return n;
    }

    /**
     * Checks that a certificate may issue the certificate below it.
     * <p>
     * 인증서가 그 아래의 인증서를 발급할 수 있는지 검사한다.
     *
     * @param issuer the issuing certificate / 발급 인증서
     * @param path   the path below it / 그 아래의 경로
     * @throws X509Exception if a check fails / 검사가 실패하면
     */
    private void checkIssuer(X509Cert issuer, List<X509Cert> path) throws X509Exception {
        if (!issuer.isCa()) {
            throw new X509Exception(X509Exception.Reason.NOT_CA, "the issuing certificate is not a CA");
        }
        if (!issuer.allowsKeyUsage(X509Cert.KU_KEY_CERT_SIGN)) {
            throw new X509Exception(X509Exception.Reason.KEY_USAGE, "the issuing certificate may not sign certificates");
        }
        if (!issuer.allowsServerAuth()) {
            throw new X509Exception(X509Exception.Reason.EXTENDED_KEY_USAGE, "the issuing certificate is not for server authentication");
        }
        if (issuer.pathLength() >= 0 && intermediates(path) > issuer.pathLength()) {
            throw new X509Exception(X509Exception.Reason.PATH_LENGTH, "more CA certificates than the path length " + issuer.pathLength() + " allows");
        }
    }

    /**
     * Checks the constraints that need the whole path: the restrictions of the root and the name constraints of every CA.
     * <p>
     * 경로 전체가 있어야 하는 제약을 검사한다: 루트의 제한과 모든 CA 의 이름 제약.
     *
     * @param path the path below the root / 루트 아래의 경로
     * @param root the root / 루트
     * @throws X509Exception if a constraint is violated / 제약을 어기면
     */
    private void checkWholePath(List<X509Cert> path, TrustRoot root) throws X509Exception {
        X509Cert rootCert = root.certificate();
        if (path.get(0).notBefore() > root.distrustAfter()) {
            throw new X509Exception(X509Exception.Reason.DISTRUSTED, "the root is not trusted for certificates issued after "
                    + new java.util.Date(root.distrustAfter()));
        }
        if (rootCert.pathLength() >= 0 && intermediates(path) > rootCert.pathLength()) {
            throw new X509Exception(X509Exception.Reason.PATH_LENGTH, "more CA certificates than the root allows");
        }
        // 경로의 인증서마다, 그 위의 모든 CA 와 루트의 이름 제약을 만족해야 한다.
        for (int i = 0; i < path.size(); i++) {
            X509Cert below = path.get(i);
            // 위쪽 CA 의 제약을 차례로 본다.
            for (int k = i + 1; k < path.size(); k++) {
                NameConstraints nc = path.get(k).nameConstraints();
                if (nc != null && !nc.allows(below)) {
                    throw new X509Exception(X509Exception.Reason.NAME_CONSTRAINTS, "a name is outside the name constraints of an issuer");
                }
            }
            if (rootCert.nameConstraints() != null && !rootCert.nameConstraints().allows(below)) {
                throw new X509Exception(X509Exception.Reason.NAME_CONSTRAINTS, "a name is outside the name constraints of the root");
            }
            if (root.appliedConstraints() != null && !root.appliedConstraints().allows(below)) {
                throw new X509Exception(X509Exception.Reason.NAME_CONSTRAINTS, "a name is outside the constraints applied to the root");
            }
        }
    }

    /**
     * Extends a path upwards until a root signs its last certificate (depth first search over the certificates of the server).
     * <p>
     * 경로의 마지막 인증서를 루트가 서명할 때까지 경로를 위로 늘린다(서버가 보낸 인증서에 대한 깊이 우선 탐색).
     *
     * @param path the path so far; on success it holds the complete path / 지금까지의 경로. 성공하면 완성된 경로가 담긴다
     * @return true if a valid path was found / 유효한 경로를 찾았으면 true
     */
    private boolean search(List<X509Cert> path) {
        X509Cert current = path.get(path.size() - 1);
        int depth = path.size();
        List<TrustRoot> roots = store.findBySubject(current.issuer());
        // 먼저 신뢰하는 루트가 서명했는지 본다.
        for (int i = 0; i < roots.size(); i++) {
            try {
                verifySignature(current, roots.get(i).certificate().key());
                checkWholePath(path, roots.get(i));
                return true;
            } catch (X509Exception e) {
                remember(e, depth + 1);
            }
        }
        if (depth >= MAX_DEPTH) {
            remember(new X509Exception(X509Exception.Reason.TOO_LONG, "path of more than " + MAX_DEPTH + " certificates"), depth);
            return false;
        }
        // 서버가 보낸 인증서 가운데 발급자 후보를 차례로 시도한다.
        for (int i = 1; i < certs.size(); i++) {
            X509Cert candidate = certs.get(i);
            if (path.contains(candidate) || !current.isIssuedBy(candidate.subject())) {
                continue;
            }
            try {
                checkIssuer(candidate, path);
                checkCommon(candidate);
                verifySignature(current, candidate.key());
            } catch (X509Exception e) {
                remember(e, depth);
                continue;
            }
            path.add(candidate);
            if (search(path)) {
                return true;
            }
            path.remove(path.size() - 1);
        }
        return false;
    }
}
