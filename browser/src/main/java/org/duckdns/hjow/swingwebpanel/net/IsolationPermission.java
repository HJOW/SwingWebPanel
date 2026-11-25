package org.duckdns.hjow.swingwebpanel.net;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The cross-origin-isolated feature of a document or an iframe's permissions policy.
 * <p>
 * 문서 또는 iframe의 권한 정책에 있는 cross-origin-isolated 기능.
 */
final class IsolationPermission {
    /** The single feature that affects shared memory exposure. / 공유 메모리 노출에 영향을 주는 기능. */
    private static final String FEATURE = "cross-origin-isolated";

    /** An allowlist item carrying self or an origin. / self 또는 오리진을 가진 허용 목록 항목. */
    private static final String ITEM = "(?:self|\"[^\"\\\\ ]+\")";

    /** One declared allowlist, using the header's structured syntax. / 구조화 헤더 문법에 따르는 선언 허용 목록. */
    private static final Pattern DECLARATION = Pattern.compile("cross-origin-isolated[ \\t]*=[ \\t]*"
            + "(\\*|\\( *(?:" + ITEM + "(?: +" + ITEM + ")*+)? *\\))");

    /** Static permission evaluation. / 정적 권한 판정. */
    private IsolationPermission() {
    }

    /**
     * Reads the applicable allowlist with quote-aware header member boundaries.
     * <p>
     * 따옴표를 고려해 헤더 항목을 구분하고 적용할 허용 목록을 읽는다.
     *
     * @param headers the Permissions-Policy field lines / Permissions-Policy 헤더 줄
     * @return the declared allowlist or null if none / 선언 허용 목록. 없으면 null
     */
    static String read(List<String> headers) {
        String result = null;
        // 여러 줄에 있는 구조화 사전 항목을 헤더 순서대로 처리한다.
        for (int h = 0; h < headers.size(); h++) {
            String line = headers.get(h);
            boolean quoted = false;
            boolean escaped = false;
            int start = 0;
            // 인용 문자열 안의 쉼표로는 항목을 나누지 않는다.
            for (int i = 0; i <= line.length(); i++) {
                char c = i == line.length() ? ',' : line.charAt(i);
                if (c == ',' && !quoted) {
                    Matcher matcher = DECLARATION.matcher(line.substring(start, i).trim());
                    if (matcher.matches()) {
                        result = matcher.group(1);
                    }
                    start = i + 1;
                } else if (escaped) {
                    escaped = false;
                } else if (c == '"') {
                    quoted = !quoted;
                } else if (quoted && c == '\\') {
                    escaped = true;
                }
            }
        }
        return result;
    }

    /**
     * Determines whether a declared response header permits the feature for the target.
     * <p>
     * 응답에 선언한 헤더가 대상에 이 기능을 허용하는지 판정한다.
     *
     * @param declaration the declared field or null / 선언 목록 또는 null
     * @param owner the response's origin / 응답의 오리진
     * @param target the origin asking for the feature / 기능을 요청하는 오리진
     * @return true if the declaration does not disable the feature / 선언이 기능을 차단하지 않으면 true
     */
    static boolean declaredAllows(String declaration, Origin owner, Origin target) {
        if (declaration == null || "*".equals(declaration)) {
            return true;
        }
        String[] values = declaration.substring(1, declaration.length() - 1).trim().split(" +");
        // self 토큰과 명시된 오리진 중 하나가 대상에 해당해야 한다.
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if ("self".equals(value) && owner.isSameOrigin(target)) {
                return true;
            }
            if (value.length() >= 2 && value.charAt(0) == '"' && originMatches(value.substring(1, value.length() - 1), target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Applies an iframe's container policy and the default allowlist of self.
     * <p>
     * iframe의 컨테이너 정책과 기본 허용 목록 self를 적용한다.
     *
     * @param allow the iframe's allow attribute / iframe의 allow 속성
     * @param owner embedding document origin / 컨테이너 문서 오리진
     * @param declaredSrc iframe's declared source origin / iframe에 지정한 src 오리진
     * @param target the navigating document's origin / 이동 중인 문서의 오리진
     * @return true when the child inherits permission / 자식이 권한을 이어받으면 true
     */
    static boolean containerAllows(String allow, Origin owner, Origin declaredSrc, Origin target) {
        if (allow != null) {
            String[] directives = allow.split(";");
            // 처음 선언한 기능 항목을 사용한다.
            for (int i = 0; i < directives.length; i++) {
                String[] tokens = directives[i].trim().split("\\s+");
                if (!FEATURE.equals(tokens[0])) {
                    continue;
                }
                if (tokens.length == 1) {
                    return declaredSrc.isSameOrigin(target);
                }
                // 컨테이너의 토큰은 self·src·공용 허용 및 명시적 오리진을 지원한다.
                for (int j = 1; j < tokens.length; j++) {
                    String value = tokens[j];
                    if ("*".equals(value) || ("'self'".equals(value) && owner.isSameOrigin(target))
                            || ("'src'".equals(value) && declaredSrc.isSameOrigin(target)) || originMatches(value, target)) {
                        return true;
                    }
                }
                return false;
            }
        }
        return owner.isSameOrigin(target);
    }

    /**
     * Compares an origin token including a wildcard subdomain or port.
     * <p>
     * 와일드카드 하위 도메인이나 포트를 포함할 수 있는 오리진 토큰을 비교한다.
     *
     * @param value the token / 토큰
     * @param target the target origin / 대상 오리진
     * @return true if the token matches / 일치하면 true
     */
    private static boolean originMatches(String value, Origin target) {
        int scheme = value.indexOf("://");
        if (scheme < 0 || target.isOpaque()) {
            return false;
        }
        boolean anyHost = value.startsWith("*.", scheme + 3);
        boolean anyPort = value.endsWith(":*");
        String text = anyHost ? value.substring(0, scheme + 3) + value.substring(scheme + 5) : value;
        Origin origin = Origin.of(anyPort ? text.substring(0, text.length() - 2) : text);
        if (origin.isOpaque() || !value.substring(0, scheme).equals(origin.getScheme()) || !origin.getScheme().equals(target.getScheme())) {
            return false;
        }
        // 오리진 토큰에는 경로나 쿼리가 없으며 와일드카드는 하위 도메인에만 적용된다.
        String authority = text.substring(scheme + 3);
        if (authority.indexOf('/') >= 0 || authority.indexOf('?') >= 0 || authority.indexOf('#') >= 0 || authority.indexOf('@') >= 0) {
            return false;
        }
        if (origin.getHost() == null || target.getHost() == null) {
            return false;
        }
        boolean hostMatches = anyHost ? target.getHost().endsWith("." + origin.getHost()) : origin.getHost().equals(target.getHost());
        return hostMatches && (anyPort || origin.getPort() == target.getPort());
    }
}
