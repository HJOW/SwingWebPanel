package org.duckdns.hjow.swingwebpanel.core.frames;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The address rules of <code>iframe</code> attributes (HTML5 REC 2014-10-28 4.7.2 "process the iframe attributes"): an empty or unresolvable
 * <code>src</code> means <code>about:blank</code>, and an address that an ancestor browsing context already shows (ignoring the fragment)
 * must not be loaded again, which stops pages that embed themselves.
 * <p>
 * <code>iframe</code> 속성의 주소 규칙(HTML5 REC 2014-10-28 4.7.2 "process the iframe attributes"): 비어 있거나 해석할 수 없는 <code>src</code> 는
 * <code>about:blank</code> 이고, 조상 브라우징 문맥이 이미 보여 주는 주소(프래그먼트 제외)는 다시 로드하지 않는다. 자기 자신을 임베드하는 페이지를 막는다.
 */
public final class FrameUrls {

    /**
     * The address of an empty frame.
     * <p>
     * 빈 프레임의 주소.
     */
    public static final String ABOUT_BLANK = "about:blank";

    /**
     * The address of the document of a <code>srcdoc</code> attribute.
     * <p>
     * <code>srcdoc</code> 속성 문서의 주소.
     */
    public static final String ABOUT_SRCDOC = "about:srcdoc";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FrameUrls() {
    }

    /**
     * Resolves the <code>src</code> attribute against the base URL of the document of the frame element: a missing or empty value, a base
     * that cannot resolve it (for example an <code>about:</code> document) or a malformed value give {@link #ABOUT_BLANK}.
     * <p>
     * <code>src</code> 속성을 프레임 요소가 속한 문서의 기준 URL 로 해석한다. 없거나 빈 값, 해석할 수 없는 기준(예: <code>about:</code> 문서), 잘못된 값이면
     * {@link #ABOUT_BLANK} 이다.
     *
     * @param base the base URL of the document / 문서의 기준 URL
     * @param src  the attribute value or null / 속성 값 또는 null
     * @return the absolute address / 절대 주소
     */
    public static String resolveSrc(String base, String src) {
        if (src == null || src.trim().length() == 0) {
            return ABOUT_BLANK;
        }
        try {
            String resolved = UrlUtil.resolve(base, src.trim());
            return resolved == null || resolved.length() == 0 ? ABOUT_BLANK : resolved;
        } catch (RuntimeException e) {
            // 기준 URL 이 없거나 값이 잘못되어 해석하지 못하면 빈 프레임이다.
            return ABOUT_BLANK;
        }
    }

    /**
     * Returns an address without its fragment, for comparisons of documents.
     * <p>
     * 문서 비교를 위해 프래그먼트를 뺀 주소를 반환한다.
     *
     * @param url the address / 주소
     * @return the address without fragment / 프래그먼트를 뺀 주소
     */
    public static String withoutFragment(String url) {
        if (url == null) {
            return "";
        }
        int hash = url.indexOf('#');
        return hash < 0 ? url : url.substring(0, hash);
    }

    /**
     * Tells whether an ancestor shows the address (ignoring fragments): then the navigation of the frame is cancelled. The special addresses
     * <code>about:blank</code> and <code>about:srcdoc</code> never count as a loop.
     * <p>
     * 조상이 그 주소(프래그먼트 제외)를 보여 주는지 알려 준다. 그러면 프레임 이동은 취소된다. 특수 주소 <code>about:blank</code> 와
     * <code>about:srcdoc</code> 는 순환으로 세지 않는다.
     *
     * @param url       the address that the frame would load / 프레임이 로드할 주소
     * @param ancestors the addresses of the active documents of all ancestors, nearest first / 모든 조상의 활성 문서 주소(가까운 것부터)
     * @return true if it is a loop / 순환이면 true
     */
    public static boolean isLoop(String url, List<String> ancestors) {
        if (url == null || url.startsWith("about:")) {
            return false;
        }
        String wanted = withoutFragment(url);
        // 조상의 주소를 하나씩 비교한다.
        for (int i = 0; i < ancestors.size(); i++) {
            if (wanted.equals(withoutFragment(ancestors.get(i)))) {
                return true;
            }
        }
        return false;
    }
}
