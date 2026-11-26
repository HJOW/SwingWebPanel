package org.duckdns.hjow.swingwebpanel.core.frames;

/**
 * The sandboxing flags of a browsing context, parsed from the <code>sandbox</code> attribute of an <code>iframe</code> (HTML5 REC 2014-10-28
 * 4.7.2). An absent attribute gives {@link #NONE}: nothing is restricted. A present attribute restricts scripts, forms, popups, navigation of
 * the top-level context and gives the document a unique origin, except for the <code>allow-*</code> tokens that it lists. The flags of nested
 * frames accumulate: a restriction of an ancestor cannot be lifted by a descendant (see {@link #inherit(SandboxFlags)}). Instances are immutable.
 * <p>
 * 브라우징 문맥의 샌드박스 플래그. <code>iframe</code> 의 <code>sandbox</code> 속성에서 해석한다(HTML5 REC 2014-10-28 4.7.2). 속성이 없으면
 * {@link #NONE}: 아무것도 제한하지 않는다. 속성이 있으면 나열한 <code>allow-*</code> 토큰을 뺀 스크립트, 폼, 팝업, 최상위 문맥 이동을 제한하고 문서에
 * 유일한 오리진을 준다. 중첩된 프레임의 플래그는 누적된다: 조상의 제한을 자손이 풀 수 없다({@link #inherit(SandboxFlags)} 참조). 인스턴스는 불변이다.
 */
public final class SandboxFlags {

    /**
     * No sandbox: nothing is restricted.
     * <p>
     * 샌드박스 없음: 아무것도 제한하지 않는다.
     */
    public static final SandboxFlags NONE = new SandboxFlags(false, true, true, true, true, true, true);

    /**
     * True if a <code>sandbox</code> attribute applies (even an empty one).
     * <p>
     * <code>sandbox</code> 속성이 적용되면(빈 속성이어도) true.
     */
    private final boolean sandboxed;

    /**
     * True if scripts may run (<code>allow-scripts</code>).
     * <p>
     * 스크립트를 실행할 수 있으면(<code>allow-scripts</code>) true.
     */
    private final boolean scripts;

    /**
     * True if the document keeps its real origin (<code>allow-same-origin</code>).
     * <p>
     * 문서가 실제 오리진을 유지하면(<code>allow-same-origin</code>) true.
     */
    private final boolean sameOrigin;

    /**
     * True if forms may be submitted (<code>allow-forms</code>).
     * <p>
     * 폼을 제출할 수 있으면(<code>allow-forms</code>) true.
     */
    private final boolean forms;

    /**
     * True if the document may navigate its top-level context (<code>allow-top-navigation</code>).
     * <p>
     * 문서가 최상위 문맥을 이동시킬 수 있으면(<code>allow-top-navigation</code>) true.
     */
    private final boolean topNavigation;

    /**
     * True if the document may request new browsing contexts (<code>allow-popups</code>).
     * <p>
     * 문서가 새 브라우징 문맥을 요청할 수 있으면(<code>allow-popups</code>) true.
     */
    private final boolean popups;

    /** Whether object URL links may start downloads. / 객체 URL 링크가 다운로드를 시작할 수 있는지 여부. */
    private final boolean downloads;

    /**
     * Creates a flag set.
     * <p>
     * 플래그 집합을 만든다.
     *
     * @param sandboxed     true if a sandbox applies / 샌드박스가 적용되면 true
     * @param scripts       true if scripts are allowed / 스크립트 허용이면 true
     * @param sameOrigin    true if the real origin is kept / 실제 오리진을 유지하면 true
     * @param forms         true if forms are allowed / 폼 허용이면 true
     * @param topNavigation true if top-level navigation is allowed / 최상위 이동 허용이면 true
     * @param popups        true if popups are allowed / 팝업 허용이면 true
     * @param downloads     true if object URL downloads are allowed / 객체 URL 다운로드 허용이면 true
     */
    private SandboxFlags(boolean sandboxed, boolean scripts, boolean sameOrigin, boolean forms, boolean topNavigation, boolean popups,
            boolean downloads) {
        this.sandboxed = sandboxed;
        this.scripts = scripts;
        this.sameOrigin = sameOrigin;
        this.forms = forms;
        this.topNavigation = topNavigation;
        this.popups = popups;
        this.downloads = downloads;
    }

    /**
     * Parses the value of a <code>sandbox</code> attribute: space-separated tokens, compared ASCII case-insensitively; unknown tokens are
     * ignored. A null value (no attribute) gives {@link #NONE}.
     * <p>
     * <code>sandbox</code> 속성 값을 해석한다: 공백으로 구분한 토큰이며 ASCII 대소문자를 구분하지 않고 비교한다. 모르는 토큰은 무시한다. null(속성 없음)이면
     * {@link #NONE}.
     *
     * @param attribute the attribute value or null / 속성 값 또는 null
     * @return the flags / 플래그
     */
    public static SandboxFlags parse(String attribute) {
        if (attribute == null) {
            return NONE;
        }
        boolean scripts = false;
        boolean sameOrigin = false;
        boolean forms = false;
        boolean top = false;
        boolean popups = false;
        boolean downloads = false;
        String[] tokens = attribute.trim().split("[ \\t\\n\\r\\f]+");
        // 토큰을 하나씩 보며 허용 키워드를 켠다.
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i].toLowerCase(java.util.Locale.ENGLISH);
            if ("allow-scripts".equals(t)) {
                scripts = true;
            } else if ("allow-same-origin".equals(t)) {
                sameOrigin = true;
            } else if ("allow-forms".equals(t)) {
                forms = true;
            } else if ("allow-top-navigation".equals(t)) {
                top = true;
            } else if ("allow-popups".equals(t)) {
                popups = true;
            } else if ("allow-downloads".equals(t)) {
                downloads = true;
            }
        }
        return new SandboxFlags(true, scripts, sameOrigin, forms, top, popups, downloads);
    }

    /**
     * Accumulates the flags of the parent context: a restriction that either of the two sets has applies. The flags of the top-level
     * document are {@link #NONE}.
     * <p>
     * 부모 문맥의 플래그를 누적한다: 둘 중 하나라도 가진 제한이 적용된다. 최상위 문서의 플래그는 {@link #NONE} 이다.
     *
     * @param parent the active flags of the parent document / 부모 문서의 활성 플래그
     * @return the effective flags / 실제 적용되는 플래그
     */
    public SandboxFlags inherit(SandboxFlags parent) {
        if (parent == null || !parent.sandboxed) {
            return this;
        }
        return new SandboxFlags(true, scripts && parent.scripts, sameOrigin && parent.sameOrigin, forms && parent.forms,
                topNavigation && parent.topNavigation, popups && parent.popups, downloads && parent.downloads);
    }

    /**
     * Tells whether the sandbox permits an object URL download.
     * <p>
     * 샌드박스가 객체 URL 다운로드를 허용하는지 알려 준다.
     *
     * @return true if allowed / 허용이면 true
     */
    public boolean allowsDownloads() {
        return downloads;
    }

    /**
     * Tells whether a sandbox applies.
     * <p>
     * 샌드박스가 적용되는지 알려 준다.
     *
     * @return true if sandboxed / 샌드박스이면 true
     */
    public boolean isSandboxed() {
        return sandboxed;
    }

    /**
     * Tells whether scripts may run.
     * <p>
     * 스크립트를 실행할 수 있는지 알려 준다.
     *
     * @return true if allowed / 허용이면 true
     */
    public boolean allowsScripts() {
        return scripts;
    }

    /**
     * Tells whether the document keeps its real origin (otherwise it has a unique, opaque origin).
     * <p>
     * 문서가 실제 오리진을 유지하는지(아니면 유일한 불투명 오리진) 알려 준다.
     *
     * @return true if the real origin is kept / 실제 오리진을 유지하면 true
     */
    public boolean allowsSameOrigin() {
        return sameOrigin;
    }

    /**
     * Tells whether forms may be submitted.
     * <p>
     * 폼을 제출할 수 있는지 알려 준다.
     *
     * @return true if allowed / 허용이면 true
     */
    public boolean allowsForms() {
        return forms;
    }

    /**
     * Tells whether the document may navigate the top-level browsing context.
     * <p>
     * 문서가 최상위 브라우징 문맥을 이동시킬 수 있는지 알려 준다.
     *
     * @return true if allowed / 허용이면 true
     */
    public boolean allowsTopNavigation() {
        return topNavigation;
    }

    /**
     * Tells whether the document may request new browsing contexts (<code>target="_blank"</code>, <code>window.open</code>).
     * <p>
     * 문서가 새 브라우징 문맥을 요청할 수 있는지(<code>target="_blank"</code>, <code>window.open</code>) 알려 준다.
     *
     * @return true if allowed / 허용이면 true
     */
    public boolean allowsPopups() {
        return popups;
    }

    /**
     * Describes the flags for diagnostics.
     * <p>
     * 진단용으로 플래그를 설명한다.
     *
     * @return the description / 설명
     */
    public String toString() {
        if (!sandboxed) {
            return "sandbox:none";
        }
        return "sandbox:" + (scripts ? " scripts" : "") + (sameOrigin ? " same-origin" : "") + (forms ? " forms" : "")
                + (topNavigation ? " top-navigation" : "") + (popups ? " popups" : "");
    }
}
