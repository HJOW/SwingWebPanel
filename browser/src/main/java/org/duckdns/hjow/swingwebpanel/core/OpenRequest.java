package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.js.host.BrowsingContext;

/**
 * What a page asked for when it needs a new window (plan item P31-08): a call of <code>window.open</code>, a link or form whose target
 * names no existing browsing context, or a link that the user chose to open elsewhere (Ctrl, Shift or the middle button). The session
 * builds the request and gives it to the {@link WindowOpener} of its component, which decides whether and where the window appears.
 * <p>
 * 페이지가 새 창을 필요로 할 때 요청한 것(계획 P31-08): <code>window.open</code> 호출, 대상이 기존 브라우징 문맥을 가리키지 않는 링크나 폼,
 * 사용자가 다른 곳에서 열기로 고른 링크(Ctrl, Shift, 가운데 단추). 세션이 요청을 만들어 컴포넌트의 {@link WindowOpener} 에 주고, 그것이 창을
 * 보일지와 어디에 보일지를 정한다.
 */
public final class OpenRequest {

    /** A call of <code>window.open</code>. / <code>window.open</code> 호출. */
    public static final int SCRIPT = 0;

    /** A link or a form whose target needs a new window. / 대상이 새 창을 필요로 하는 링크나 폼. */
    public static final int LINK = 1;

    /** A link that the user chose to open in a new tab or window. / 사용자가 새 탭이나 창에서 열기로 고른 링크. */
    public static final int USER = 2;

    /** The user asked for a new tab (Ctrl+click, middle click). / 사용자가 새 탭을 요청했다(Ctrl+클릭, 가운데 클릭). */
    public static final String PLACE_TAB = "tab";

    /** The user asked for a new window (Shift+click). / 사용자가 새 창을 요청했다(Shift+클릭). */
    public static final String PLACE_WINDOW = "window";

    /** One of {@link #SCRIPT}, {@link #LINK} and {@link #USER}. / {@link #SCRIPT}, {@link #LINK}, {@link #USER} 중 하나. */
    private final int kind;

    /** The absolute address to open, or the empty string for the empty page. / 열 절대 주소. 빈 페이지이면 빈 문자열. */
    private final String url;

    /** The target name, or the empty string for <code>_blank</code>. / 대상 이름. <code>_blank</code> 이면 빈 문자열. */
    private final String target;

    /** The window features text of <code>window.open</code>, or the empty string. / <code>window.open</code> 의 창 기능 문자열. 없으면 빈 문자열. */
    private final String features;

    /** The address of the document that asked. / 요청한 문서의 주소. */
    private final String documentUrl;

    /** The context of the document that asked, or null. / 요청한 문서의 문맥. 없으면 null. */
    private final BrowsingContext opener;

    /** The origin address that the first empty document of the window inherits. / 창의 처음 빈 문서가 물려받는 오리진 주소. */
    private final String originUrl;

    /** The base address that the first empty document of the window inherits. / 창의 처음 빈 문서가 물려받는 기준 주소. */
    private final String baseUri;

    /** The body of a form submission that goes to the new window, or null. / 새 창으로 가는 폼 제출의 본문. 없으면 null. */
    private final PostData post;

    /** For {@link #USER}: {@link #PLACE_TAB} or {@link #PLACE_WINDOW}; otherwise the empty string. / {@link #USER} 일 때: {@link #PLACE_TAB} 또는 {@link #PLACE_WINDOW}. 그 밖에는 빈 문자열. */
    private final String place;

    /** True if the feature <code>noopener</code> (or <code>noreferrer</code>) was given, or the request is not a script call. / <code>noopener</code>(또는 <code>noreferrer</code>) 기능이 있었거나 스크립트 호출이 아니면 true. */
    private final boolean noOpener;

    /** The requested width, height, left and top in CSS pixels; -1 where none was given. / 요청한 폭, 높이, 왼쪽, 위쪽(CSS 픽셀). 주지 않은 것은 -1. */
    private final int[] box;

    /**
     * Creates a request.
     * <p>
     * 요청을 만든다.
     *
     * @param kind        {@link #SCRIPT}, {@link #LINK} or {@link #USER} / {@link #SCRIPT}, {@link #LINK}, {@link #USER} 중 하나
     * @param url         the absolute address or the empty string / 절대 주소 또는 빈 문자열
     * @param target      the target name / 대상 이름
     * @param features    the window features text / 창 기능 문자열
     * @param documentUrl the address of the asking document / 요청한 문서의 주소
     * @param opener      the context of the asking document or null / 요청한 문서의 문맥 또는 null
     * @param originUrl   the origin address to inherit / 물려줄 오리진 주소
     * @param baseUri     the base address to inherit / 물려줄 기준 주소
     * @param post        the form body or null / 폼 본문 또는 null
     * @param place       the place that the user chose, or the empty string / 사용자가 고른 장소 또는 빈 문자열
     */
    public OpenRequest(int kind, String url, String target, String features, String documentUrl, BrowsingContext opener, String originUrl,
            String baseUri, PostData post, String place) {
        this.kind = kind;
        this.url = url == null ? "" : url;
        String t = target == null ? "" : target;
        this.target = "_blank".equalsIgnoreCase(t) ? "" : t;
        this.features = features == null ? "" : features;
        this.documentUrl = documentUrl == null ? "" : documentUrl;
        this.opener = opener;
        this.originUrl = originUrl;
        this.baseUri = baseUri;
        this.post = post;
        this.place = place == null ? "" : place;
        this.box = new int[] { number("width", "innerwidth"), number("height", "innerheight"), number("left", "screenx"), number("top", "screeny") };
        this.noOpener = kind != SCRIPT || flag("noopener") || flag("noreferrer");
    }

    /** False if a script asked without an action of the user, so that the popup blocker applies. / 스크립트가 사용자의 조작 없이 요청했으면 false. 그러면 팝업 차단이 적용된다. */
    private boolean activated = true;

    /**
     * Notes whether an action of the user stands behind the request (a form that a script submits into a new window has none).
     * <p>
     * 요청 뒤에 사용자의 조작이 있는지 기록한다(스크립트가 새 창으로 제출한 폼에는 없다).
     *
     * @param value true if the user acted / 사용자가 조작했으면 true
     * @return this request / 이 요청
     */
    public OpenRequest withActivation(boolean value) {
        activated = value;
        return this;
    }

    /**
     * Tells whether an action of the user stands behind the request.
     * <p>
     * 요청 뒤에 사용자의 조작이 있는지 알려 준다.
     *
     * @return false if the popup blocker applies to it / 팝업 차단이 적용되면 false
     */
    public boolean isActivated() {
        return activated;
    }

    /**
     * Returns the kind of the request.
     * <p>
     * 요청의 종류를 반환한다.
     *
     * @return {@link #SCRIPT}, {@link #LINK} or {@link #USER} / {@link #SCRIPT}, {@link #LINK}, {@link #USER} 중 하나
     */
    public int getKind() {
        return kind;
    }

    /**
     * Returns the address to open.
     * <p>
     * 열 주소를 반환한다.
     *
     * @return the absolute address, or the empty string for the empty page / 절대 주소. 빈 페이지이면 빈 문자열
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the target name.
     * <p>
     * 대상 이름을 반환한다.
     *
     * @return the name, or the empty string for <code>_blank</code> / 이름. <code>_blank</code> 이면 빈 문자열
     */
    public String getTarget() {
        return target;
    }

    /**
     * Returns the window features text.
     * <p>
     * 창 기능 문자열을 반환한다.
     *
     * @return the text as the script gave it, or the empty string / 스크립트가 준 그대로의 글. 없으면 빈 문자열
     */
    public String getFeatures() {
        return features;
    }

    /**
     * Returns the address of the document that asked.
     * <p>
     * 요청한 문서의 주소를 반환한다.
     *
     * @return the address / 주소
     */
    public String getDocumentUrl() {
        return documentUrl;
    }

    /**
     * Returns the context of the document that asked.
     * <p>
     * 요청한 문서의 문맥을 반환한다.
     *
     * @return the context, or null / 문맥. 없으면 null
     */
    public BrowsingContext getOpener() {
        return opener;
    }

    /**
     * Returns the origin address that the first empty document of the window inherits.
     * <p>
     * 창의 처음 빈 문서가 물려받는 오리진 주소를 반환한다.
     *
     * @return the address, or null / 주소. 없으면 null
     */
    public String getOriginUrl() {
        return originUrl;
    }

    /**
     * Returns the base address that the first empty document of the window inherits.
     * <p>
     * 창의 처음 빈 문서가 물려받는 기준 주소를 반환한다.
     *
     * @return the address, or null / 주소. 없으면 null
     */
    public String getBaseUri() {
        return baseUri;
    }

    /**
     * Returns the body of the form submission that goes to the new window.
     * <p>
     * 새 창으로 가는 폼 제출의 본문을 반환한다.
     *
     * @return the body, or null for a plain address / 본문. 보통 주소이면 null
     */
    public PostData getPost() {
        return post;
    }

    /**
     * Returns the place that the user chose.
     * <p>
     * 사용자가 고른 장소를 반환한다.
     *
     * @return {@link #PLACE_TAB} or {@link #PLACE_WINDOW} for {@link #USER}, otherwise the empty string / {@link #USER} 이면
     *         {@link #PLACE_TAB} 또는 {@link #PLACE_WINDOW}. 그 밖에는 빈 문자열
     */
    public String getPlace() {
        return place;
    }

    /**
     * Tells whether the new window has no connection to the window that asked: it runs on its own document thread, has no opener, and
     * <code>window.open</code> returns null.
     * <p>
     * 새 창이 요청한 창과 관계가 없는지 알려 준다: 자기 문서 스레드에서 실행되고, opener 가 없으며, <code>window.open</code> 은 null 을 돌려준다.
     *
     * @return true without a connection / 관계가 없으면 true
     */
    public boolean isNoOpener() {
        return noOpener;
    }

    /**
     * Returns the requested width of the content area.
     * <p>
     * 요청한 내용 영역의 폭을 반환한다.
     *
     * @return CSS pixels, or -1 if none was given / CSS 픽셀. 주지 않았으면 -1
     */
    public int getWidth() {
        return box[0];
    }

    /**
     * Returns the requested height of the content area.
     * <p>
     * 요청한 내용 영역의 높이를 반환한다.
     *
     * @return CSS pixels, or -1 if none was given / CSS 픽셀. 주지 않았으면 -1
     */
    public int getHeight() {
        return box[1];
    }

    /**
     * Returns the requested left edge on the screen.
     * <p>
     * 요청한 화면의 왼쪽 가장자리를 반환한다.
     *
     * @return pixels, or -1 if none was given / 픽셀. 주지 않았으면 -1
     */
    public int getLeft() {
        return box[2];
    }

    /**
     * Returns the requested top edge on the screen.
     * <p>
     * 요청한 화면의 위쪽 가장자리를 반환한다.
     *
     * @return pixels, or -1 if none was given / 픽셀. 주지 않았으면 -1
     */
    public int getTop() {
        return box[3];
    }

    /**
     * Finds the value of a feature in the features text. The text is a list of <code>name=value</code> items separated by commas; the
     * names are compared without regard to case and white space around names and values is ignored.
     * <p>
     * 창 기능 문자열에서 기능의 값을 찾는다. 문자열은 쉼표로 구분한 <code>이름=값</code> 항목의 목록이다. 이름은 대소문자를 가리지 않고 비교하며
     * 이름과 값 둘레의 공백은 무시한다.
     *
     * @param name the feature name in lower case / 소문자 기능 이름
     * @return the value (the empty string for a name without one), or null if the feature is absent / 값(값 없는 이름이면 빈 문자열).
     *         기능이 없으면 null
     */
    private String feature(String name) {
        String[] items = features.split(",");
        // 항목마다 이름을 비교한다.
        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();
            int eq = item.indexOf('=');
            String key = (eq < 0 ? item : item.substring(0, eq)).trim().toLowerCase(java.util.Locale.ENGLISH);
            if (name.equals(key)) {
                return eq < 0 ? "" : item.substring(eq + 1).trim();
            }
        }
        return null;
    }

    /**
     * Tells whether a feature is turned on: present without a value, or with <code>yes</code>, <code>true</code> or a number other than 0.
     * <p>
     * 기능이 켜져 있는지 알려 준다: 값 없이 있거나, 값이 <code>yes</code>, <code>true</code>, 또는 0 이 아닌 수이다.
     *
     * @param name the feature name in lower case / 소문자 기능 이름
     * @return true if on / 켜져 있으면 true
     */
    private boolean flag(String name) {
        String v = feature(name);
        if (v == null) {
            return false;
        }
        if (v.length() == 0 || "yes".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v)) {
            return true;
        }
        try {
            return Integer.parseInt(v) != 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Reads a feature as a whole number that is not negative.
     * <p>
     * 기능을 음수가 아닌 정수로 읽는다.
     *
     * @param name  the feature name in lower case / 소문자 기능 이름
     * @param alias another name of the same feature / 같은 기능의 다른 이름
     * @return the number, or -1 if the feature is absent or not such a number / 수. 기능이 없거나 그런 수가 아니면 -1
     */
    private int number(String name, String alias) {
        String v = feature(name);
        if (v == null) {
            v = feature(alias);
        }
        if (v == null) {
            return -1;
        }
        int end = 0;
        // 앞쪽의 숫자만 읽는다("400px" 같은 값의 단위는 버린다).
        while (end < v.length() && end < 6 && v.charAt(end) >= '0' && v.charAt(end) <= '9') {
            end++;
        }
        return end == 0 ? -1 : Integer.parseInt(v.substring(0, end));
    }
}
