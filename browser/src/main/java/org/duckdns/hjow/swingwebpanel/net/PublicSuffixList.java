package org.duckdns.hjow.swingwebpanel.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.IDN;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves registrable domains for the schemeless same-site check in CORP.
 * <p>
 * CORP의 스킴을 무시한 동일 사이트 검사에 쓸 등록 가능 도메인을 찾는다.
 */
final class PublicSuffixList {
    /** Exact suffixes from the bundled Public Suffix List. / 포함한 Public Suffix List의 정확한 접미사. */
    private final Set<String> exact = new HashSet<String>();

    /** Suffixes with a wildcard in their first label. / 첫 레이블이 와일드카드인 접미사. */
    private final Set<String> wildcard = new HashSet<String>();

    /** Explicit exceptions to a wildcard rule. / 와일드카드 규칙의 명시적 예외. */
    private final Set<String> exceptions = new HashSet<String>();

    /**
     * Loads the table on its first same-site lookup.
     * <p>
     * 첫 동일 사이트 조회에서 표를 읽는다.
     */
    private static final class Table {
        /** The bundled rules, or null if the resource was unavailable. / 포함한 규칙. 리소스가 없으면 null. */
        static final PublicSuffixList RULES = load();
    }

    /**
     * Creates an empty rule table.
     * <p>
     * 빈 규칙 표를 만든다.
     */
    private PublicSuffixList() {
    }

    /**
     * Compares normalized hosts or their registrable domains without considering their scheme or port.
     * <p>
     * 스킴과 포트를 제외하고 정규화한 호스트 또는 등록 가능 도메인을 비교한다.
     *
     * @param a the first origin / 첫 오리진
     * @param b the other origin / 다른 오리진
     * @return true if the origins are schemelessly same-site / 스킴을 제외하고 동일 사이트이면 true
     */
    static boolean sameSite(Origin a, Origin b) {
        String first = a.getHost();
        String second = b.getHost();
        if (first == null || second == null || first.length() == 0 || second.length() == 0) {
            return false;
        }
        first = canonical(first);
        second = canonical(second);
        if (first.equals(second)) {
            return true;
        }
        PublicSuffixList table = Table.RULES;
        if (table == null) {
            return false;
        }
        String site = table.registrable(first);
        return site != null && site.equals(table.registrable(second));
    }

    /**
     * Reads one pinned Public Suffix List containing the ICANN and private sections.
     * <p>
     * ICANN 및 민간 구역을 담은 고정 Public Suffix List를 읽는다.
     *
     * @return the complete table or null if it could not be read / 완전한 표. 읽지 못했으면 null
     */
    private static PublicSuffixList load() {
        InputStream stream = PublicSuffixList.class.getResourceAsStream("public_suffix_list.dat");
        if (stream == null) {
            return null;
        }
        PublicSuffixList list = new PublicSuffixList();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            try {
                String line;
                // 주석을 건너뛰고 예외·와일드카드·정확한 접미사를 각각 담는다.
                while ((line = reader.readLine()) != null) {
                    String rule = line.trim();
                    if (rule.length() == 0 || rule.startsWith("//")) {
                        continue;
                    }
                    if (rule.startsWith("!")) {
                        list.exceptions.add(canonical(rule.substring(1)));
                    } else if (rule.startsWith("*.")) {
                        list.wildcard.add(canonical(rule.substring(2)));
                    } else {
                        list.exact.add(canonical(rule));
                    }
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            return null;
        }
        return list;
    }

    /**
     * Maps domains and list entries to lower-case ASCII where supported by the JVM.
     * <p>
     * JVM에서 지원하는 도메인과 목록 항목을 소문자 ASCII로 바꾼다.
     *
     * @param host the host or rule / 호스트 또는 규칙
     * @return normalized text / 정규화한 문자열
     */
    private static String canonical(String host) {
        try {
            return IDN.toASCII(host, IDN.ALLOW_UNASSIGNED).toLowerCase(Locale.ENGLISH);
        } catch (IllegalArgumentException e) {
            return host.toLowerCase(Locale.ENGLISH);
        }
    }

    /**
     * Finds the registrable domain, applying the longest rule with precedence for wildcard exceptions.
     * <p>
     * 와일드카드의 예외를 우선하고 가장 긴 규칙을 적용해 등록 가능 도메인을 찾는다.
     *
     * @param host normalized host / 정규화한 호스트
     * @return registrable domain or null for literals and public suffixes / 등록 가능 도메인. 리터럴이나 공용 접미사이면 null
     */
    private String registrable(String host) {
        if (host.indexOf(':') >= 0 || host.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")) {
            return null;
        }
        String name = host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
        int previous = -1;
        int start = 0;
        // 가장 긴 후보부터 하나의 레이블씩 제외해 공용 접미사를 찾는다.
        for (;;) {
            String suffix = name.substring(start);
            if (exceptions.contains(suffix)) {
                return host.substring(start);
            }
            int next = name.indexOf('.', start);
            if (exact.contains(suffix) || next < 0 || wildcard.contains(name.substring(next + 1))) {
                return previous < 0 ? null : host.substring(previous);
            }
            previous = start;
            start = next + 1;
        }
    }
}
