package org.duckdns.hjow.swingwebpanel.tls13.x509;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * A set of trusted root certificates. The default set is the list of roots that Mozilla trusts for web sites, taken from the "Mozilla
 * Included CA Certificate Report" of the Common CA Database (CCADB, licensed under CDLA-Permissive-2.0) and shipped inside this module.
 * <b>The list is not updated automatically</b>: it is the state of the day when the module was built (see {@link #source()}), so over
 * time new roots are missing and removals arrive late. A host can use its own roots instead. Usage:
 * <pre>
 * TrustStore roots = TrustStore.mozilla();          // the bundled list
 * TrustStore own = TrustStore.of(myKeyStore);       // or the certificates of a java.security.KeyStore
 * </pre>
 * <p>
 * 신뢰하는 루트 인증서의 집합. 기본 집합은 Mozilla 가 웹 사이트용으로 신뢰하는 루트 목록으로, Common CA Database(CCADB, CDLA-Permissive-2.0
 * 라이선스)의 "Mozilla Included CA Certificate Report" 에서 가져와 이 모듈 안에 넣은 것이다. <b>목록은 자동으로 갱신되지 않는다</b>: 모듈을 만든 날의
 * 상태이므로({@link #source()} 참고) 시간이 지나면 새 루트가 빠지고 삭제 반영이 늦어진다. 호스트는 자기 루트를 대신 쓸 수 있다.
 */
public final class TrustStore {

    /** The resource with the bundled roots. / 내장 루트가 든 자료. */
    private static final String RESOURCE = "mozilla-roots.txt";

    /** The bundled store, loaded on first use. / 내장 저장소. 처음 쓸 때 읽는다. */
    private static TrustStore bundled;

    /** The roots in order. / 순서대로의 루트. */
    private final List<TrustRoot> roots = new ArrayList<TrustRoot>();

    /** The roots by the hexadecimal text of their subject name. / 주체 이름의 16진수 문자열로 찾는 루트. */
    private final Map<String, List<TrustRoot>> bySubject = new HashMap<String, List<TrustRoot>>();

    /** A description of where the roots come from. / 루트의 출처 설명. */
    private final String source;

    /**
     * Creates an empty store.
     * <p>
     * 빈 저장소를 만든다.
     *
     * @param source the description of the origin / 출처 설명
     */
    private TrustStore(String source) {
        this.source = source;
    }

    /**
     * Returns the text key of a name.
     * <p>
     * 이름의 문자열 키를 반환한다.
     *
     * @param name the encoded name / 부호화된 이름
     * @return the key / 키
     */
    private static String keyOf(byte[] name) {
        return new java.math.BigInteger(1, name).toString(16);
    }

    /**
     * Adds a root.
     * <p>
     * 루트를 더한다.
     *
     * @param root the root / 루트
     */
    private void add(TrustRoot root) {
        roots.add(root);
        String key = keyOf(root.certificate().subject());
        List<TrustRoot> list = bySubject.get(key);
        if (list == null) {
            list = new ArrayList<TrustRoot>();
            bySubject.put(key, list);
        }
        list.add(root);
    }

    /**
     * Returns the bundled Mozilla root list.
     * <p>
     * 내장된 Mozilla 루트 목록을 반환한다.
     *
     * @return the store / 저장소
     * @throws IllegalStateException if the resource is missing or damaged / 자료가 없거나 손상되었으면
     */
    public static synchronized TrustStore mozilla() {
        if (bundled == null) {
            InputStream in = TrustStore.class.getResourceAsStream(RESOURCE);
            if (in == null) {
                throw new IllegalStateException("the bundled root list is missing");
            }
            try {
                bundled = read(in);
            } catch (IOException e) {
                throw new IllegalStateException("the bundled root list cannot be read: " + e);
            } catch (X509Exception e) {
                throw new IllegalStateException("the bundled root list is damaged: " + e.getMessage());
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    // 닫기 실패는 읽은 결과에 영향이 없다.
                    in = null;
                }
            }
        }
        return bundled;
    }

    /**
     * Reads a root list in the format written by <code>tools/cabundle</code>: comment lines that start with <code>#</code> (the lines
     * <code># source:</code> and <code># retrieved:</code> give the description) and one line per root with the fields
     * <code>root</code>, distrust date (<code>yyyy-mm-dd</code> or <code>-</code>), permitted DNS subtrees (comma separated or
     * <code>-</code>), a label and the Base64 of the certificate, separated by tabs.
     * <p>
     * <code>tools/cabundle</code> 이 쓴 형식의 루트 목록을 읽는다: <code>#</code> 로 시작하는 주석 줄(<code># source:</code>, <code># retrieved:</code>
     * 줄이 출처 설명이 된다)과, 루트마다 탭으로 나눈 필드 <code>root</code>, 불신 날짜(<code>yyyy-mm-dd</code> 또는 <code>-</code>), 허용 DNS 하위 트리(쉼표
     * 구분 또는 <code>-</code>), 레이블, 인증서의 Base64 로 된 한 줄.
     *
     * @param in the UTF-8 text / UTF-8 텍스트
     * @return the store / 저장소
     * @throws IOException   on a read failure / 읽기 실패 시
     * @throws X509Exception if a line or a certificate is malformed / 줄이나 인증서가 잘못되었으면
     */
    static TrustStore read(InputStream in) throws IOException, X509Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String from = "";
        String when = "";
        List<String> lines = new ArrayList<String>();
        String line;
        // 주석에서 출처를 읽고 루트 줄을 모은다.
        while ((line = r.readLine()) != null) {
            if (line.startsWith("# source:")) {
                from = line.substring(9).trim();
            } else if (line.startsWith("# retrieved:")) {
                when = line.substring(12).trim();
            } else if (line.length() > 0 && line.charAt(0) != '#') {
                lines.add(line);
            }
        }
        TrustStore store = new TrustStore("Mozilla root list from CCADB (" + from + "), retrieved " + when);
        // 루트 줄마다 필드를 해석한다.
        for (int i = 0; i < lines.size(); i++) {
            String[] f = lines.get(i).split("\t", -1);
            if (f.length != 5 || !"root".equals(f[0])) {
                throw Der.malformed("root list line " + (i + 1));
            }
            long distrust = "-".equals(f[1]) ? Long.MAX_VALUE : date(f[1]);
            NameConstraints applied = null;
            if (!"-".equals(f[2])) {
                applied = new NameConstraints(java.util.Arrays.asList(f[2].split(",")));
            }
            store.add(new TrustRoot(X509Cert.parse(base64(f[4])), distrust, applied));
        }
        if (store.roots.isEmpty()) {
            throw Der.malformed("the root list is empty");
        }
        return store;
    }

    /**
     * Parses a date <code>yyyy-mm-dd</code> as the end of that day in UTC.
     * <p>
     * 날짜 <code>yyyy-mm-dd</code> 를 UTC 로 그 날의 끝으로 해석한다.
     *
     * @param text the date / 날짜
     * @return milliseconds since 1970 / 1970년부터의 밀리초
     * @throws X509Exception if the text is not a date / 날짜가 아니면
     */
    private static long date(String text) throws X509Exception {
        String[] p = text.split("-");
        try {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            c.clear();
            c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 23, 59, 59);
            return c.getTimeInMillis();
        } catch (RuntimeException e) {
            throw Der.malformed("bad date " + text);
        }
    }

    /**
     * Decodes standard Base64 without line breaks.
     * <p>
     * 줄바꿈 없는 표준 Base64 를 푼다.
     *
     * @param text the text / 문자열
     * @return the bytes / 바이트
     * @throws X509Exception if a character is not Base64 / Base64 가 아닌 글자가 있으면
     */
    static byte[] base64(String text) throws X509Exception {
        int n = text.length();
        // 끝의 채움 문자를 뗀다.
        while (n > 0 && text.charAt(n - 1) == '=') {
            n--;
        }
        byte[] out = new byte[n * 3 / 4];
        int acc = 0;
        int bits = 0;
        int at = 0;
        // 글자마다 6비트를 모아 8비트가 차면 바이트를 낸다.
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            int v = c >= 'A' && c <= 'Z' ? c - 'A' : c >= 'a' && c <= 'z' ? c - 'a' + 26 : c >= '0' && c <= '9' ? c - '0' + 52 : c == '+' ? 62 : c == '/' ? 63 : -1;
            if (v < 0) {
                throw Der.malformed("not Base64");
            }
            acc = (acc << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out[at++] = (byte) (acc >> bits);
            }
        }
        return out;
    }

    /**
     * Creates a store from the certificate entries of a key store (for example the <code>cacerts</code> file of a JRE or a store with
     * the certificate of a test server). Entries that are not X.509 certificates or cannot be parsed are skipped.
     * <p>
     * 키 저장소의 인증서 항목으로 저장소를 만든다(예: JRE 의 <code>cacerts</code> 파일, 시험 서버의 인증서가 든 저장소). X.509 인증서가 아니거나 해석할
     * 수 없는 항목은 건너뛴다.
     *
     * @param keyStore the loaded key store / 읽어 둔 키 저장소
     * @return the store / 저장소
     * @throws KeyStoreException if the key store is not loaded / 키 저장소를 읽지 않았으면
     */
    public static TrustStore of(KeyStore keyStore) throws KeyStoreException {
        TrustStore store = new TrustStore("key store of the host");
        Enumeration<String> aliases = keyStore.aliases();
        // 별칭마다 인증서 항목을 더한다(개인 키 항목은 사슬의 첫 인증서를 쓴다).
        while (aliases.hasMoreElements()) {
            Certificate c = keyStore.getCertificate(aliases.nextElement());
            if (c != null && "X.509".equals(c.getType())) {
                try {
                    store.add(new TrustRoot(X509Cert.parse(c.getEncoded()), Long.MAX_VALUE, null));
                } catch (CertificateEncodingException e) {
                    // 부호화할 수 없는 항목은 건너뛴다.
                    continue;
                } catch (X509Exception e) {
                    // 해석할 수 없는 항목은 건너뛴다.
                    continue;
                }
            }
        }
        return store;
    }

    /**
     * Creates a store from encoded certificates.
     * <p>
     * 부호화된 인증서들로 저장소를 만든다.
     *
     * @param certificates DER encoded root certificates / DER 로 부호화된 루트 인증서들
     * @return the store / 저장소
     * @throws X509Exception if a certificate is malformed / 인증서가 잘못되었으면
     */
    public static TrustStore ofCertificates(List<byte[]> certificates) throws X509Exception {
        TrustStore store = new TrustStore("certificates of the host");
        // 인증서마다 제한 없는 루트로 더한다.
        for (int i = 0; i < certificates.size(); i++) {
            store.add(new TrustRoot(X509Cert.parse(certificates.get(i)), Long.MAX_VALUE, null));
        }
        return store;
    }

    /**
     * Returns a copy of this store in which a root has the given restrictions (used by tests and by hosts that want to restrict a root).
     * <p>
     * 루트 하나에 주어진 제한을 둔 이 저장소의 사본을 반환한다(시험과, 루트를 제한하려는 호스트가 쓴다).
     *
     * @param index         the index of the root in {@link #roots()} / {@link #roots()} 에서의 루트 번호
     * @param distrustAfter the distrust time or <code>Long.MAX_VALUE</code> / 불신 시각 또는 <code>Long.MAX_VALUE</code>
     * @param permittedDns  the permitted DNS subtrees or null / 허용 DNS 하위 트리 또는 null
     * @return the new store / 새 저장소
     */
    public TrustStore restrict(int index, long distrustAfter, List<String> permittedDns) {
        TrustStore store = new TrustStore(source);
        // 그 루트만 제한을 바꿔 옮긴다.
        for (int i = 0; i < roots.size(); i++) {
            TrustRoot r = roots.get(i);
            store.add(i == index ? new TrustRoot(r.certificate(), distrustAfter, permittedDns == null ? null : new NameConstraints(permittedDns)) : r);
        }
        return store;
    }

    /**
     * Returns the roots whose subject name equals a name.
     * <p>
     * 주체 이름이 그 이름과 같은 루트들을 반환한다.
     *
     * @param name an encoded name / 부호화된 이름
     * @return the roots, possibly empty / 루트 목록. 비어 있을 수 있다
     */
    List<TrustRoot> findBySubject(byte[] name) {
        List<TrustRoot> list = bySubject.get(keyOf(name));
        return list == null ? Collections.<TrustRoot>emptyList() : list;
    }

    /**
     * Returns all roots.
     * <p>
     * 모든 루트를 반환한다.
     *
     * @return the roots / 루트 목록
     */
    public List<TrustRoot> roots() {
        return Collections.unmodifiableList(roots);
    }

    /**
     * Returns the number of roots.
     * <p>
     * 루트 수를 반환한다.
     *
     * @return the count / 개수
     */
    public int size() {
        return roots.size();
    }

    /**
     * Describes where the roots come from; for the bundled list this names the source and the day it was retrieved.
     * <p>
     * 루트의 출처를 설명한다. 내장 목록이면 원본과 받은 날짜를 말한다.
     *
     * @return the description / 설명
     */
    public String source() {
        return source;
    }
}
