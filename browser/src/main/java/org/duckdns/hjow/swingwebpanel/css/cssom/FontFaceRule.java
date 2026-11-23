package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;

/**
 * An <code>@font-face</code> rule: the descriptors of a web font. The family, the sources (<code>url()</code> with an optional
 * <code>format()</code>, or <code>local()</code>) and the style, weight, stretch and unicode range are available; the font loading policy (plan item P10-05)
 * decides what to do with them.
 * <p>
 * <code>@font-face</code> 규칙: 웹 글꼴의 디스크립터. 글꼴 계열, 출처(<code>format()</code> 이 붙을 수 있는 <code>url()</code> 또는 <code>local()</code>), 스타일, 두께, 폭, 유니코드 범위를
 * 알 수 있으며, 글꼴을 읽는 정책(계획 P10-05)이 이들로 무엇을 할지 정한다.
 */
public final class FontFaceRule extends CssRule {

    /**
     * One source of a web font.
     * <p>
     * 웹 글꼴의 출처 하나.
     */
    public static final class Source {
        /** The URL as written, or null for local(). / 쓰인 그대로의 URL. local() 이면 null. */
        private final String url;
        /** The absolute URL or null. / 절대 URL 또는 null. */
        private final String resolved;
        /** The format hint (lower case) or null. / 형식 힌트(소문자) 또는 null. */
        private final String format;
        /** The local font name or null. / 로컬 글꼴 이름 또는 null. */
        private final String local;

        /**
         * Creates a source.
         * <p>
         * 출처를 만든다.
         *
         * @param url      URL as written / 쓰인 URL
         * @param resolved absolute URL / 절대 URL
         * @param format   format hint / 형식 힌트
         * @param local    local name / 로컬 이름
         */
        Source(String url, String resolved, String format, String local) {
            this.url = url;
            this.resolved = resolved;
            this.format = format;
            this.local = local;
        }

        /**
         * Returns the URL as written.
         * <p>
         * 쓰인 그대로의 URL 을 반환한다.
         *
         * @return the URL or null / URL 또는 null
         */
        public String getUrl() {
            return url;
        }

        /**
         * Returns the absolute URL, or the written one if the style sheet URL was not known.
         * <p>
         * 절대 URL 을 반환한다. 스타일 시트 URL 을 몰랐으면 쓰인 그대로.
         *
         * @return the URL or null / URL 또는 null
         */
        public String getResolvedUrl() {
            return resolved != null ? resolved : url;
        }

        /**
         * Returns the format hint.
         * <p>
         * 형식 힌트를 반환한다.
         *
         * @return for example woff2, truetype, or null / 예: woff2, truetype. 없으면 null
         */
        public String getFormat() {
            return format;
        }

        /**
         * Returns the local font name.
         * <p>
         * 로컬 글꼴 이름을 반환한다.
         *
         * @return the name or null / 이름 또는 null
         */
        public String getLocal() {
            return local;
        }
    }

    /** The descriptors by name with their component values. / 이름별 디스크립터와 그 컴포넌트 값. */
    private final Map<String, List<ComponentValue>> descriptors;

    /** The parsed sources. / 파싱한 출처. */
    private final List<Source> sources;

    /**
     * Creates a font face rule.
     * <p>
     * 글꼴 규칙을 만든다.
     *
     * @param descriptors the descriptors / 디스크립터
     * @param sources     the sources / 출처
     */
    public FontFaceRule(Map<String, List<ComponentValue>> descriptors, List<Source> sources) {
        this.descriptors = Collections.unmodifiableMap(new LinkedHashMap<String, List<ComponentValue>>(descriptors));
        this.sources = Collections.unmodifiableList(new ArrayList<Source>(sources));
    }

    /**
     * Returns a descriptor as text.
     * <p>
     * 디스크립터를 문자열로 반환한다.
     *
     * @param name descriptor name such as <code>font-weight</code> / <code>font-weight</code> 같은 디스크립터 이름
     * @return the text or null if the descriptor is not given / 문자열. 주어지지 않았으면 null
     */
    public String getDescriptor(String name) {
        List<ComponentValue> v = descriptors.get(name);
        return v == null ? null : CssBlock.join(v).trim();
    }

    /** The declaration block that the CSSOM <code>style</code> shows, built when it is first asked for. / CSSOM <code>style</code> 이 보여 주는 선언 블록. 처음 요청할 때 만든다. */
    private CssStyleDeclaration style;

    /**
     * Returns the descriptors as a declaration block (the CSSOM <code>style</code> of <code>CSSFontFaceRule</code>). The block holds the descriptors
     * that are also CSS properties (<code>font-family</code>, <code>font-style</code>, <code>font-weight</code> ...); a descriptor that is not a
     * property (<code>src</code>, <code>unicode-range</code>, <code>font-display</code>) is not in it. Changing the block does not change the rule: the
     * web font loader reads the descriptors that the rule was parsed with.
     * <p>
     * 디스크립터를 선언 블록으로 반환한다(<code>CSSFontFaceRule</code> 의 CSSOM <code>style</code>). 블록에는 CSS 속성이기도 한 디스크립터(<code>font-family</code>,
     * <code>font-style</code>, <code>font-weight</code> 등)가 들어 있고, 속성이 아닌 디스크립터(<code>src</code>, <code>unicode-range</code>, <code>font-display</code>)는
     * 없다. 블록을 바꿔도 규칙은 바뀌지 않는다: 웹 글꼴 로더는 규칙을 파싱한 디스크립터를 읽는다.
     *
     * @return the block / 블록
     */
    public CssStyleDeclaration getStyle() {
        if (style == null) {
            CssStyleDeclaration d = new CssStyleDeclaration();
            // 디스크립터마다 CSS 속성으로 읽히는 것만 블록에 넣는다.
            for (Map.Entry<String, List<ComponentValue>> e : descriptors.entrySet()) {
                d.add(new org.duckdns.hjow.swingwebpanel.css.syntax.SyntaxDeclaration(e.getKey(), e.getValue(), false),
                        new org.duckdns.hjow.swingwebpanel.css.values.ParseEnv(org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry.getInstance(), null), null);
            }
            d.setParentRule(this);
            style = d;
        }
        return style;
    }

    /**
     * Returns the names of the descriptors that the rule gives, in the order they were written.
     * <p>
     * 규칙이 주는 디스크립터의 이름을 쓰인 순서대로 반환한다.
     *
     * @return the names / 이름
     */
    public List<String> getDescriptorNames() {
        return new ArrayList<String>(descriptors.keySet());
    }

    /**
     * Returns the font family name without quotes.
     * <p>
     * 따옴표 없는 글꼴 계열 이름을 반환한다.
     *
     * @return the name or null / 이름 또는 null
     */
    public String getFamily() {
        List<ComponentValue> v = descriptors.get("font-family");
        if (v == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        // 문자열이면 그 내용을, 식별자 나열이면 공백으로 이은 이름을 쓴다.
        for (int i = 0; i < v.size(); i++) {
            ComponentValue c = v.get(i);
            if (c instanceof CssToken && (((CssToken) c).is(CssToken.STRING) || ((CssToken) c).is(CssToken.IDENT))) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(((CssToken) c).getValue());
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * Returns the sources in order of preference.
     * <p>
     * 선호하는 순서의 출처를 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<Source> getSources() {
        return sources;
    }

    /**
     * Returns the rule type.
     * <p>
     * 규칙 종류를 반환한다.
     *
     * @return {@link #FONT_FACE_RULE} / FONT_FACE_RULE
     */
    public int getType() {
        return FONT_FACE_RULE;
    }

    /**
     * Serializes the rule.
     * <p>
     * 규칙을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String getCssText() {
        StringBuilder sb = new StringBuilder("@font-face {");
        // 디스크립터를 차례로 이어 쓴다.
        for (Map.Entry<String, List<ComponentValue>> e : descriptors.entrySet()) {
            sb.append(' ').append(e.getKey()).append(": ").append(CssBlock.join(e.getValue()).trim()).append(';');
        }
        return sb.append(" }").toString();
    }

    /**
     * Parses the <code>src</code> descriptor into sources.
     * <p>
     * <code>src</code> 디스크립터를 출처 목록으로 파싱한다.
     *
     * @param value the descriptor's component values / 디스크립터의 컴포넌트 값
     * @param base  the style sheet URL used to resolve relative urls, or null / 상대 url 을 해석할 스타일 시트 URL 또는 null
     * @return the sources / 출처
     */
    static List<Source> parseSources(List<ComponentValue> value, org.duckdns.hjow.swingwebpanel.css.values.ParseEnv base) {
        List<Source> out = new ArrayList<Source>();
        List<List<ComponentValue>> parts = CssParser.splitCommas(value);
        // 쉼표로 나눈 출처마다 url() 또는 local() 과 선택적인 format() 을 읽는다.
        for (int i = 0; i < parts.size(); i++) {
            String url = null;
            String local = null;
            String format = null;
            List<ComponentValue> p = parts.get(i);
            // p 의 항목을 하나씩 처리한다.
            for (int j = 0; j < p.size(); j++) {
                ComponentValue c = p.get(j);
                if (c instanceof CssToken && ((CssToken) c).is(CssToken.URL)) {
                    url = ((CssToken) c).getValue();
                } else if (c instanceof CssFunction) {
                    CssFunction f = (CssFunction) c;
                    List<ComponentValue> a = CssParser.trim(f.getArguments());
                    String arg = a.size() >= 1 && a.get(0) instanceof CssToken ? ((CssToken) a.get(0)).getValue() : null;
                    if (f.isNamed("url") && arg != null) {
                        url = arg;
                    } else if (f.isNamed("local") && arg != null) {
                        local = arg;
                    } else if (f.isNamed("format") && arg != null) {
                        format = arg.toLowerCase();
                    }
                }
            }
            if (url != null || local != null) {
                out.add(new Source(url, url == null ? null : base.resolve(url), format, local));
            }
        }
        return out;
    }
}
