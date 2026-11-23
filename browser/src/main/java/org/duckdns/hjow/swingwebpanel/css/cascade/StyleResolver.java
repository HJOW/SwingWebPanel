package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.CssDiagnostics;
import org.duckdns.hjow.swingwebpanel.css.cssom.ContainerCondition;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.UaStyleSheet;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.LogicalProps;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.selector.ElementState;
import org.duckdns.hjow.swingwebpanel.css.selector.MatchContext;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorMatcher;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomMutationListener;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The style engine of a document: it finds the rules that match an element, orders their declarations by the cascade (origin and importance, then
 * specificity, then source order), computes the values and caches the {@link ComputedStyle} of every element. It subscribes to the DOM mutations of the
 * document and drops exactly the cached styles a change can affect: an attribute change invalidates the subtree of the element (the whole parent subtree
 * if the style sheets use sibling combinators, the whole document if they use <code>:has()</code>), a change of the child list does so only when structural
 * selectors (<code>:first-child</code>, <code>:empty</code>, <code>+</code> ...) exist, and a change of the dynamic state (hover, focus, form state) only
 * recomputes the styles whose computation consulted that state. A change of the media environment or of the style sheets rebuilds the rule index.
 * <p>
 * <b>Cascade order</b> (ascending): user agent normal, user normal, presentational hints, author normal (the <code>style</code> attribute last),
 * author important, user important, user agent important. <b>Threading</b>: a resolver belongs to the thread that owns the document (the document thread
 * of the browser session); it is not synchronized.
 * <p>
 * 문서의 스타일 엔진: 요소와 일치하는 규칙을 찾고, 선언을 캐스케이드(출처와 중요도, 구체성, 소스 순서)로 정렬하고, 값을 계산하고, 요소마다 {@link ComputedStyle} 을 캐시한다.
 * 문서의 DOM 변경을 구독하여 변경이 영향을 줄 수 있는 캐시된 스타일만 버린다: 속성 변경은 요소의 서브트리를(스타일 시트가 형제 결합자를 쓰면 부모 서브트리 전체, <code>:has()</code>
 * 를 쓰면 문서 전체), 자식 목록 변경은 구조 선택자(<code>:first-child</code>, <code>:empty</code>, <code>+</code> 등)가 있을 때만, 동적 상태(hover, focus, 폼 상태) 변경은 계산에서 그
 * 상태를 조회한 스타일만 다시 계산한다. 미디어 환경이나 스타일 시트가 바뀌면 규칙 색인을 다시 만든다.
 * <p>
 * <b>캐스케이드 순서</b>(오름차순): 사용자 에이전트 일반, 사용자 일반, 표현 힌트, 작성자 일반(<code>style</code> 속성이 마지막), 작성자 중요, 사용자 중요, 사용자 에이전트 중요.
 * <b>스레드</b>: 리졸버는 문서를 소유한 스레드(브라우저 세션의 문서 스레드)에 속하며 동기화하지 않는다.
 */
public final class StyleResolver implements DomMutationListener {

    /** Tier: user agent normal declarations. / 층: 사용자 에이전트 일반 선언. */
    private static final int TIER_UA = 0;
    /** Tier: user normal declarations. / 층: 사용자 일반 선언. */
    private static final int TIER_USER = 1;
    /** Tier: presentational hints. / 층: 표현 힌트. */
    private static final int TIER_HINTS = 2;
    /** Tier: author normal declarations. / 층: 작성자 일반 선언. */
    private static final int TIER_AUTHOR = 3;
    /** Tier: author important declarations. / 층: 작성자 중요 선언. */
    private static final int TIER_AUTHOR_IMPORTANT = 5;
    /** Tier: user important declarations. / 층: 사용자 중요 선언. */
    private static final int TIER_USER_IMPORTANT = 6;
    /** Tier: user agent important declarations. / 층: 사용자 에이전트 중요 선언. */
    private static final int TIER_UA_IMPORTANT = 7;

    /**
     * One declaration block applied at a tier with its specificity and order.
     * <p>
     * 층, 구체성, 순서가 붙어 적용되는 선언 블록 하나.
     */
    private static final class App {
        /** The tier of this application. / 이 적용 정보의 층. */
        final int tier;
        /** The specificity. / 구체성. */
        final long spec;
        /** The source order. / 소스 순서. */
        final int order;
        /** The layer key within the tier: the layer rank for normal declarations, the reversed rank for important ones (higher wins). 층 안의 레이어 키: 일반 선언은 레이어 순위, 중요한 선언은 뒤집은 순위(높은 것이 이긴다). */
        final int layer;
        /** The declarations. / 선언. */
        final CssStyleDeclaration style;
        /** True to apply the important declarations. / 중요한 선언을 적용하면 true. */
        final boolean important;
        /** The scoping proximity key: higher is nearer; 0 for a rule that is not inside <code>@scope</code>. 범위 근접도 키: 클수록 가깝다. <code>@scope</code> 밖의 규칙은 0. */
        int proximity;

        /**
         * Creates an application.
         * <p>
         * 적용 정보를 만든다.
         *
         * @param tier      tier / 층
         * @param spec      specificity / 구체성
         * @param order     order / 순서
         * @param style     declarations / 선언
         * @param important important flag / 중요 플래그
         */
        App(int tier, long spec, int order, CssStyleDeclaration style, boolean important) {
            this(tier, spec, order, style, important, important ? 0 : Integer.MAX_VALUE);
        }

        /**
         * Creates an application that belongs to a cascade layer.
         * <p>
         * 캐스케이드 레이어에 속한 적용 정보를 만든다.
         *
         * @param tier      the tier / 층
         * @param spec      the specificity / 구체성
         * @param order     the source order / 소스 순서
         * @param style     the declarations / 선언
         * @param important true for the important declarations / 중요한 선언이면 true
         * @param layer     the layer key (see {@link #layer}) / 레이어 키({@link #layer} 참조)
         */
        App(int tier, long spec, int order, CssStyleDeclaration style, boolean important, int layer) {
            this.layer = layer;
            this.tier = tier;
            this.spec = spec;
            this.order = order;
            this.style = style;
            this.important = important;
        }
    }

    /** Orders applications by tier, specificity and source order. / 층, 구체성, 소스 순서로 적용 정보를 정렬한다. */
    private static final Comparator<App> ORDER = new Comparator<App>() {
        /** Compares two applications. 두 적용 정보를 비교한다. */
        public int compare(App a, App b) {
            if (a.tier != b.tier) {
                return a.tier < b.tier ? -1 : 1;
            }
            if (a.layer != b.layer) {
                return a.layer < b.layer ? -1 : 1;
            }
            if (a.spec != b.spec) {
                return a.spec < b.spec ? -1 : 1;
            }
            // 구체성이 같으면 범위 루트가 더 가까운 규칙이 이긴다(CSS Cascade 6 의 scoping proximity).
            if (a.proximity != b.proximity) {
                return a.proximity < b.proximity ? -1 : 1;
            }
            return a.order < b.order ? -1 : a.order > b.order ? 1 : 0;
        }
    };

    /**
     * The cached styles of one element.
     * <p>
     * 요소 하나의 캐시된 스타일.
     */
    private static final class Cached {
        /** The style of the element. / 요소의 스타일. */
        ComputedStyle self;
        /** Pseudo-element styles by name (null values are cached too). / 이름별 의사 요소 스타일(null 값도 캐시한다). */
        Map<String, ComputedStyle> pseudo;
    }

    /** The document. / 문서. */
    private final Document doc;

    /** The media environment (a private copy). / 미디어 환경(비공개 복사본). */
    private MediaEnvironment env;

    /** The author style sheets in document order. / 문서 순서의 작성자 스타일 시트. */
    private List<StyleSheet> author = new ArrayList<StyleSheet>();

    /** The user style sheets. / 사용자 스타일 시트. */
    private List<StyleSheet> user = new ArrayList<StyleSheet>();

    /** The dynamic element state provider. / 동적 요소 상태 제공자. */
    private ElementState state;

    /** The presentational hints. / 표현 힌트. */
    private PresentationalHints hints = new HtmlPresentationalHints();

    /** The rule index, or null if it must be rebuilt. / 규칙 색인. 다시 만들어야 하면 null. */
    private RuleIndex index;

    /** The cached styles. / 캐시된 스타일. */
    private final IdentityHashMap<Element, Cached> cache = new IdentityHashMap<Element, Cached>();

    /** The elements whose style computation consulted dynamic state. / 스타일 계산에서 동적 상태를 조회한 요소. */
    private final IdentityHashMap<Element, Boolean> dynamic = new IdentityHashMap<Element, Boolean>();

    /** The content box sizes {width, height} of the query containers, as the last layout measured them. 마지막 레이아웃이 잰 질의 컨테이너의 content box 크기 {너비, 높이}. */
    private final IdentityHashMap<Element, double[]> containerSizes = new IdentityHashMap<Element, double[]>();

    /** The listeners (copy on write). / 리스너(쓰기 시 복사). */
    private List<StyleChangeListener> listeners = new ArrayList<StyleChangeListener>();

    /** Counts invalidations. / 무효화 횟수. */
    private int version;

    /** True if a style or link element changed since the flag was last read. / 플래그를 마지막으로 읽은 뒤 style 이나 link 요소가 바뀌었으면 true. */
    private boolean sheetSourceChanged;

    /** True after {@link #dispose()}. / {@link #dispose()} 뒤에는 true. */
    private boolean disposed;

    /** The animator that makes the animated style of an element (P10-07), or null. 요소의 애니메이션 스타일을 만드는 애니메이터(P10-07). 없으면 null. */
    private StyleAnimator animator;

    /** Diagnostics for presentational hint declarations, or null. / 표현 힌트 선언의 진단. 없으면 null. */
    private CssDiagnostics diag;

    /**
     * Creates a resolver and subscribes to the DOM changes of the document.
     * <p>
     * 리졸버를 만들고 문서의 DOM 변경을 구독한다.
     *
     * @param doc the document / 문서
     * @param env the media environment (copied) / 미디어 환경(복사됨)
     */
    public StyleResolver(Document doc, MediaEnvironment env) {
        this.doc = doc;
        this.env = env == null ? new MediaEnvironment() : env.copy();
        doc.addMutationListener(this);
    }

    /**
     * Sets the author style sheets (in document order). The user agent sheet is always included.
     * <p>
     * 작성자 스타일 시트를 설정한다(문서 순서). 사용자 에이전트 시트는 항상 포함된다.
     *
     * @param sheets the sheets / 시트
     */
    public void setStyleSheets(List<StyleSheet> sheets) {
        author = new ArrayList<StyleSheet>(sheets);
        invalidate();
    }

    /**
     * Sets the user style sheets.
     * <p>
     * 사용자 스타일 시트를 설정한다.
     *
     * @param sheets the sheets / 시트
     */
    public void setUserStyleSheets(List<StyleSheet> sheets) {
        user = new ArrayList<StyleSheet>(sheets);
        invalidate();
    }

    /**
     * Sets the provider of the dynamic element state (hover, focus, form state).
     * <p>
     * 동적 요소 상태(hover, focus, 폼 상태)의 제공자를 설정한다.
     *
     * @param s the provider or null / 제공자 또는 null
     */
    public void setElementState(ElementState s) {
        this.state = s;
        invalidate();
    }

    /**
     * Sets the presentational hints (the default maps the presentational attributes of HTML).
     * <p>
     * 표현 힌트를 설정한다(기본 구현은 HTML 의 표현 속성을 대응시킨다).
     *
     * @param h the hints or null for none / 힌트. 없으면 null
     */
    public void setPresentationalHints(PresentationalHints h) {
        this.hints = h;
        invalidate();
    }

    /**
     * Sets a collector for the diagnostics of presentational hints.
     * <p>
     * 표현 힌트의 진단을 모을 수집기를 설정한다.
     *
     * @param d collector or null / 수집기 또는 null
     */
    public void setDiagnostics(CssDiagnostics d) {
        this.diag = d;
    }

    /**
     * Sets a new media environment; the media dependent rules are re-evaluated and the styles recomputed if it differs from the current one.
     * <p>
     * 새 미디어 환경을 설정한다. 현재와 다르면 미디어에 의존하는 규칙을 다시 평가하고 스타일을 다시 계산한다.
     *
     * @param e the environment (copied) / 환경(복사됨)
     */
    public void setMediaEnvironment(MediaEnvironment e) {
        if (!env.sameAs(e)) {
            env = e.copy();
            invalidate();
        }
    }

    /**
     * Returns a copy of the current media environment.
     * <p>
     * 현재 미디어 환경의 복사본을 반환한다.
     *
     * @return the environment / 환경
     */
    public MediaEnvironment getMediaEnvironment() {
        return env.copy();
    }

    /**
     * Registers a listener for invalidations.
     * <p>
     * 무효화 리스너를 등록한다.
     *
     * @param l the listener / 리스너
     */
    public void addChangeListener(StyleChangeListener l) {
        List<StyleChangeListener> copy = new ArrayList<StyleChangeListener>(listeners);
        copy.add(l);
        listeners = copy;
    }

    /**
     * Removes a listener.
     * <p>
     * 리스너를 제거한다.
     *
     * @param l the listener / 리스너
     */
    public void removeChangeListener(StyleChangeListener l) {
        List<StyleChangeListener> copy = new ArrayList<StyleChangeListener>(listeners);
        copy.remove(l);
        listeners = copy;
    }

    /**
     * Returns the number of invalidations so far (a cheap way to see whether styles may have changed).
     * <p>
     * 지금까지의 무효화 횟수를 반환한다(스타일이 바뀌었을 수 있는지 싸게 알아보는 방법).
     *
     * @return the version / 버전
     */
    public int getStyleVersion() {
        return version;
    }

    /**
     * Tells whether a <code>style</code> or <code>link</code> element changed since the last call, and clears the flag. The host then collects the
     * style sheets again and calls {@link #setStyleSheets(List)}.
     * <p>
     * 마지막 호출 뒤에 <code>style</code> 이나 <code>link</code> 요소가 바뀌었는지 알려 주고 플래그를 지운다. 그러면 호스트가 스타일 시트를 다시 모아
     * {@link #setStyleSheets(List)} 를 호출한다.
     *
     * @return true if the sources of the style sheets changed / 스타일 시트의 출처가 바뀌었으면 true
     */
    public boolean consumeSheetSourceChange() {
        boolean c = sheetSourceChanged;
        sheetSourceChanged = false;
        return c;
    }

    /**
     * Sets the animator that turns the computed style of an element into its animated style (transitions and animations, P10-07):
     * it is called whenever the style of an element is computed (not for pseudo-elements), the style it returns is what the element
     * and its descendants (which inherit from it) get.
     * <p>
     * 요소의 계산 스타일을 애니메이션이 적용된 스타일로 바꾸는 애니메이터를 정한다(전환과 애니메이션, P10-07): 요소의 스타일을 계산할 때마다(의사 요소는 제외) 불리고,
     * 돌려준 스타일이 그 요소와 그것을 상속하는 자손이 얻는 스타일이다.
     *
     * @param a the animator, null for none / 애니메이터. 없으면 null
     */
    public void setAnimator(StyleAnimator a) {
        this.animator = a;
    }

    /**
     * Returns the animator that was set (the script host reaches the Web Animations of the document through it).
     * <p>
     * 정해 둔 애니메이터를 반환한다(스크립트 호스트가 이것을 통해 문서의 Web Animations 에 닿는다).
     *
     * @return the animator or null / 애니메이터 또는 null
     */
    public StyleAnimator getAnimator() {
        return animator;
    }

    /**
     * Drops the cached style of an element and its descendants (an animation changed its value) and notifies the listeners.
     * <p>
     * 요소와 그 자손의 캐시한 스타일을 버리고(애니메이션이 값을 바꿨다) 리스너에 알린다.
     *
     * @param e the element / 요소
     */
    public void invalidateElement(Element e) {
        if (disposed) {
            return;
        }
        invalidateSubtree(e);
    }

    /**
     * Computes the values of declarations that are not part of the cascade (the keyframes of an animation) for an element: lengths
     * are relative to the font size of the element (the declared <code>font-size</code> if there is one), inheritance is from the parent
     * of the element. Only the declared properties of the result are meaningful.
     * <p>
     * 캐스케이드에 속하지 않는 선언(애니메이션의 키프레임)의 값을 요소를 기준으로 계산한다: 길이는 요소의 글꼴 크기(선언된 <code>font-size</code> 가 있으면 그것)를
     * 기준으로 하고 상속은 요소의 부모에서 온다. 결과에서는 선언한 속성만 의미가 있다.
     *
     * @param e        the element / 요소
     * @param declared the declared values by property id / 속성 id 별 선언 값
     * @param own      the computed style of the element (its font size is the base of relative lengths) / 요소의 계산 스타일(글꼴 크기가 상대 길이의 기준)
     * @return the computed style / 계산 스타일
     */
    public ComputedStyle computeDeclared(Element e, Map<Integer, CssValue> declared, ComputedStyle own) {
        CssValue[] cascaded = new CssValue[Prop.COUNT + 1];
        // 선언한 값을 그대로 지정 값으로 쓴다(없는 글꼴 크기는 요소의 값으로 채워 em 이 요소를 기준으로 계산되게 한다).
        String wm = own == null || own.keyword(Prop.WRITING_MODE) == null ? "horizontal-tb" : own.keyword(Prop.WRITING_MODE);
        String dr = own == null || own.keyword(Prop.DIRECTION) == null ? "ltr" : own.keyword(Prop.DIRECTION);
        for (Map.Entry<Integer, CssValue> d : declared.entrySet()) {
            // 논리 속성은 요소의 쓰기 모드와 방향이 가리키는 물리 속성의 값이 된다.
            cascaded[LogicalProps.physical(d.getKey().intValue(), wm, dr)] = d.getValue();
        }
        if (cascaded[Prop.FONT_SIZE] == null && own != null) {
            cascaded[Prop.FONT_SIZE] = own.get(Prop.FONT_SIZE);
        }
        Element parentEl = e.getParentElement();
        ComputedStyle parent = parentEl == null ? null : getComputedStyle(parentEl);
        LengthContext lc = new LengthContext(LengthContext.DEFAULT_FONT_SIZE, LengthContext.DEFAULT_FONT_SIZE, LengthContext.DEFAULT_FONT_SIZE * 1.2,
                env.getWidth(), env.getHeight()).withPrefersDark("dark".equals(env.getColorScheme()));
        return StyleComputer.compute(cascaded, new LinkedHashMap<String, List<org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue>>(), parent, lc,
                e == doc.getDocumentElement());
    }

    /**
     * Drops everything that was cached (also the rule index) and notifies the listeners that the whole document is stale.
     * <p>
     * 캐시한 모든 것(규칙 색인 포함)을 버리고 문서 전체가 낡았음을 리스너에 알린다.
     */
    public void invalidate() {
        index = null;
        cache.clear();
        dynamic.clear();
        version++;
        notifyListeners(null);
    }

    /**
     * Tells the resolver that the dynamic state of an element (hover, focus, checked ...) changed; the styles whose computation consulted such state are
     * recomputed. Call it from the input layer after changing the {@link ElementState}.
     * <p>
     * 요소의 동적 상태(hover, focus, checked 등)가 바뀌었음을 리졸버에 알린다. 계산에서 그런 상태를 조회한 스타일을 다시 계산한다. 입력 계층이
     * {@link ElementState} 를 바꾼 뒤 호출한다.
     *
     * @param e the element whose state changed (may be null) / 상태가 바뀐 요소(null 가능)
     */
    public void stateChanged(Element e) {
        if (dynamic.isEmpty()) {
            return;
        }
        List<Element> roots = new ArrayList<Element>(dynamic.keySet());
        dynamic.clear();
        // 동적 상태를 조회한 요소의 서브트리를 버린다.
        for (int i = 0; i < roots.size(); i++) {
            purgeSubtree(roots.get(i));
        }
        version++;
        // roots 의 항목을 하나씩 처리한다.
        for (int i = 0; i < roots.size(); i++) {
            notifyListeners(roots.get(i));
        }
    }

    /**
     * Unsubscribes from the document and drops all caches. The resolver cannot be used afterwards.
     * <p>
     * 문서 구독을 끊고 모든 캐시를 버린다. 이후에는 리졸버를 쓸 수 없다.
     */
    public void dispose() {
        if (!disposed) {
            disposed = true;
            doc.removeMutationListener(this);
            cache.clear();
            dynamic.clear();
            listeners = new ArrayList<StyleChangeListener>();
            index = null;
        }
    }

    /**
     * Returns the computed style of an element. The result is cached until something that can change it happens.
     * <p>
     * 요소의 계산 스타일을 반환한다. 결과는 그것을 바꿀 수 있는 일이 일어날 때까지 캐시된다.
     *
     * @param e the element, which must belong to the document / 요소. 문서에 속해야 한다
     * @return the computed style, never null / 계산 스타일. null 이 아니다
     */
    public ComputedStyle getComputedStyle(Element e) {
        check(e);
        Cached c = cache.get(e);
        if (c != null && c.self != null) {
            return c.self;
        }
        List<Element> chain = new ArrayList<Element>();
        Element cur = e;
        // 캐시된 조상을 만날 때까지 올라가며 계산할 요소를 모은다(재귀를 피한다).
        while (cur != null && (cache.get(cur) == null || cache.get(cur).self == null)) {
            chain.add(cur);
            cur = cur.getParentElement();
        }
        ComputedStyle parent = cur == null ? null : cache.get(cur).self;
        // 위에서 아래로 내려오며 계산해 캐시한다.
        for (int i = chain.size() - 1; i >= 0; i--) {
            Element el = chain.get(i);
            MatchContext ctx = new MatchContext(null, state);
            ComputedStyle s = compute(el, parent, null, ctx);
            // 애니메이터가 있으면 전환과 애니메이션이 적용된 스타일이 요소의 스타일이다(자손은 그것을 상속한다).
            if (animator != null) {
                s = animator.animate(el, s);
            }
            Cached ce = cache.get(el);
            if (ce == null) {
                ce = new Cached();
                cache.put(el, ce);
            }
            ce.self = s;
            if (ctx.isDynamicUsed()) {
                dynamic.put(el, Boolean.TRUE);
            }
            parent = s;
        }
        return cache.get(e).self;
    }

    /**
     * Tells whether the style sheets of the document contain a rule inside <code>@starting-style</code>.
     * <p>
     * 문서의 스타일 시트에 <code>@starting-style</code> 안의 규칙이 있는지 알려 준다.
     *
     * @return true if one exists / 있으면 true
     */
    public boolean hasStartingStyles() {
        ensureIndex();
        return index.hasStarting;
    }

    /**
     * Computes the starting style of an element (CSS Transitions Level 2, section 3.3): the style that the element would have if the rules inside
     * <code>@starting-style</code> applied in addition to the others, inheriting from the style that the parent has now. The animator compares it
     * with the style of the element to start transitions on an element that had no before-change style (a new element, or one that was not rendered).
     * The result is not cached.
     * <p>
     * 요소의 시작 스타일을 계산한다(CSS Transitions Level 2, 3.3 절): <code>@starting-style</code> 안의 규칙이 다른 규칙과 함께 적용될 때 요소가 가질 스타일이며,
     * 부모의 지금 스타일을 상속한다. 애니메이터는 이것을 요소의 스타일과 비교해 변경 전 스타일이 없던 요소(새 요소나 렌더링되지 않던 요소)의 전환을 시작한다.
     * 결과는 캐시하지 않는다.
     *
     * @param e the element, which must belong to the document / 요소. 문서에 속해야 한다
     * @return the starting style, or null if no rule inside <code>@starting-style</code> exists / 시작 스타일. <code>@starting-style</code> 안의 규칙이 없으면 null
     */
    public ComputedStyle getStartingStyle(Element e) {
        check(e);
        ensureIndex();
        // 시작 스타일 규칙이 없으면 변경 후 스타일과 같으므로 계산하지 않는다.
        if (!index.hasStarting) {
            return null;
        }
        Element p = e.getParentElement();
        ComputedStyle parent = p == null ? null : getComputedStyle(p);
        return compute(e, parent, null, new MatchContext(null, state), true);
    }

    /**
     * Returns the style of a pseudo-element of an element (<code>before</code>, <code>after</code>, <code>first-line</code>, <code>first-letter</code>,
     * <code>marker</code>, <code>selection</code>, <code>placeholder</code> ...).
     * <p>
     * 요소의 의사 요소 스타일을 반환한다(<code>before</code>, <code>after</code>, <code>first-line</code>, <code>first-letter</code>, <code>marker</code>,
     * <code>selection</code>, <code>placeholder</code> 등).
     *
     * @param e      the originating element / 원본 요소
     * @param pseudo the pseudo-element name without colons / 콜론 없는 의사 요소 이름
     * @return the style, or null if no rule targets that pseudo-element / 스타일. 그 의사 요소를 가리키는 규칙이 없으면 null
     */
    public ComputedStyle getPseudoStyle(Element e, String pseudo) {
        ComputedStyle parent = getComputedStyle(e);
        String p = lowerPseudo(pseudo);
        Cached c = cache.get(e);
        if (c.pseudo != null && c.pseudo.containsKey(p)) {
            return c.pseudo.get(p);
        }
        MatchContext ctx = new MatchContext(null, state);
        ctx.setPseudoElement(p);
        ComputedStyle s = compute(e, parent, p, ctx);
        // 애니메이터가 있으면 의사 요소의 전환 값이 적용된 스타일이 의사 요소의 스타일이다.
        if (animator != null && s != null) {
            s = animator.animatePseudo(e, p, s);
        }
        if (c.pseudo == null) {
            c.pseudo = new HashMap<String, ComputedStyle>();
        }
        c.pseudo.put(p, s);
        if (ctx.isDynamicUsed()) {
            dynamic.put(e, Boolean.TRUE);
        }
        return s;
    }

    /**
     * Receives the properties that the cascade of the next computation sets, or null (see {@link #getPseudoDeclared}).
     * <p>
     * 다음 계산의 계단식이 정하는 속성을 받는다. 없으면 null({@link #getPseudoDeclared} 참조).
     */
    private java.util.BitSet declaredSink;

    /**
     * Returns the properties that the rules of a pseudo-element of an element declare (set by the cascade, before inheritance and initial values): the
     * renderer of a part that the user agent draws itself (the button of a file control, <code>::file-selector-button</code>) keeps its own look for the
     * properties that the author did not set.
     * <p>
     * 요소의 의사 요소 규칙이 선언하는 속성(상속과 초기값 이전에 계단식이 정한 것)을 반환한다: 사용자 에이전트가 직접 그리는 부분(파일 컨트롤의 단추,
     * <code>::file-selector-button</code>)의 그리는 쪽은 작성자가 정하지 않은 속성에 대해 자신의 모양을 지킨다.
     *
     * @param e      the originating element / 원본 요소
     * @param pseudo the pseudo-element name / 의사 요소 이름
     * @return the set of property ids (empty if no rule targets the pseudo-element) / 속성 id 집합(그 의사 요소를 가리키는 규칙이 없으면 비어 있음)
     */
    public java.util.BitSet getPseudoDeclared(Element e, String pseudo) {
        ComputedStyle parent = getComputedStyle(e);
        String p = lowerPseudo(pseudo);
        java.util.BitSet out = new java.util.BitSet();
        MatchContext ctx = new MatchContext(null, state);
        ctx.setPseudoElement(p);
        declaredSink = out;
        try {
            compute(e, parent, p, ctx);
        } finally {
            declaredSink = null;
        }
        return out;
    }

    /**
     * Lower-cases the name of a pseudo-element but not the argument of a functional one (the custom highlight name of <code>highlight(Name)</code> is case
     * sensitive).
     * <p>
     * 의사 요소의 이름을 소문자로 바꾸되 함수형 의사 요소의 인수는 그대로 둔다(<code>highlight(Name)</code> 의 사용자 정의 하이라이트 이름은 대소문자를 구분한다).
     *
     * @param pseudo the name / 이름
     * @return the normalized name / 정규화한 이름
     */
    private static String lowerPseudo(String pseudo) {
        int paren = pseudo.indexOf('(');
        return paren < 0 ? pseudo.toLowerCase() : pseudo.substring(0, paren).toLowerCase() + pseudo.substring(paren);
    }

    /**
     * The properties that apply to the highlight pseudo-elements (CSS Pseudo-Elements 4 section 3.2): only these are taken from the cascade of
     * <code>::highlight()</code>, all the others are those of the originating element.
     * <p>
     * 하이라이트 의사 요소에 적용되는 속성(CSS Pseudo-Elements 4 3.2 절): <code>::highlight()</code> 의 계단식에서는 이것들만 가져오고 나머지는 원본 요소의 것이다.
     */
    private static final int[] HIGHLIGHT_PROPERTIES = { Prop.COLOR, Prop.BACKGROUND_COLOR, Prop.TEXT_DECORATION_LINE, Prop.TEXT_DECORATION_STYLE,
        Prop.TEXT_DECORATION_COLOR, Prop.TEXT_DECORATION_THICKNESS, Prop.TEXT_SHADOW, Prop.TEXT_UNDERLINE_POSITION, Prop.TEXT_UNDERLINE_OFFSET };

    /**
     * Returns the style of the custom highlight pseudo-element <code>::highlight(name)</code> of an element (CSS Custom Highlight API 1 section 4.2.3 and CSS
     * Pseudo-Elements 4 section 3.5): a highlight property that the cascade of the element's <code>::highlight(name)</code> does not set (or sets to
     * <code>inherit</code>/<code>unset</code>) inherits from the highlight of the parent element, whether or not the property is inherited, and for the root
     * element the inherited <code>color</code> is <code>currentcolor</code> (the computed value stays the keyword <code>currentcolor</code>, which the painter
     * resolves to the color of the text). The other properties are those of the element. The style is never null.
     * <p>
     * 요소의 사용자 정의 하이라이트 의사 요소 <code>::highlight(name)</code> 의 스타일을 반환한다(CSS Custom Highlight API 1 4.2.3 절, CSS Pseudo-Elements 4 3.5 절): 요소의
     * <code>::highlight(name)</code> 계단식이 정하지 않은(또는 <code>inherit</code>/<code>unset</code> 으로 정한) 하이라이트 속성은 상속 속성인지와 상관없이 부모 요소의 하이라이트에서
     * 물려받고, 루트 요소에서 물려받는 <code>color</code> 는 <code>currentcolor</code> 이다(계산 값은 키워드 <code>currentcolor</code> 로 남고 그리는 쪽이 텍스트의 색으로 푼다).
     * 다른 속성은 요소의 것이다. 스타일은 null 이 아니다.
     *
     * @param e    the originating element / 원본 요소
     * @param name the custom highlight name / 사용자 정의 하이라이트 이름
     * @return the style / 스타일
     */
    public ComputedStyle getHighlightStyle(Element e, String name) {
        String p = "highlight(" + name + ")";
        String key = "~" + p;
        List<Element> chain = new ArrayList<Element>();
        // 캐시된 하이라이트 스타일을 가진 가장 가까운 조상까지 올라간다(깊은 트리에서도 재귀하지 않는다).
        ComputedStyle above = null;
        for (Element a = e; a != null; a = a.getParentElement()) {
            Cached c = cache.get(a);
            if (c != null && c.pseudo != null && c.pseudo.containsKey(key)) {
                above = c.pseudo.get(key);
                break;
            }
            chain.add(a);
        }
        // 위에서 아래로 내려오며 각 요소의 하이라이트 스타일을 부모의 하이라이트를 물려받아 계산한다.
        for (int i = chain.size() - 1; i >= 0; i--) {
            Element a = chain.get(i);
            ComputedStyle origin = getComputedStyle(a);
            ComputedStyle parent = above != null ? above : origin.with(Prop.COLOR, org.duckdns.hjow.swingwebpanel.css.values.CssKeyword.of("currentcolor"));
            MatchContext ctx = new MatchContext(null, state);
            ctx.setPseudoElement(p);
            ComputedStyle h = compute(a, parent, p, ctx, false, null, true);
            ComputedStyle s = origin;
            // 하이라이트 속성만 하이라이트 계단식의 값을 쓰고 나머지는 원본 요소의 값을 쓴다.
            for (int k = 0; k < HIGHLIGHT_PROPERTIES.length; k++) {
                s = s.with(HIGHLIGHT_PROPERTIES[k], h.get(HIGHLIGHT_PROPERTIES[k]));
            }
            Cached c = cache.get(a);
            if (c.pseudo == null) {
                c.pseudo = new HashMap<String, ComputedStyle>();
            }
            c.pseudo.put(key, s);
            if (ctx.isDynamicUsed()) {
                dynamic.put(a, Boolean.TRUE);
            }
            above = s;
        }
        return above;
    }

    /**
     * Returns the computed style that <code>getComputedStyle(element, pseudo)</code> reports for a pseudo-element: the style of the pseudo-element
     * if a rule targets it, otherwise the style that a pseudo-element without any rule has (the properties that inherit come from the element, the
     * others have their initial value).
     * <p>
     * 의사 요소에 대해 <code>getComputedStyle(element, pseudo)</code> 가 알려 주는 계산 스타일을 반환한다: 규칙이 가리키는 의사 요소이면 그 스타일, 아니면 규칙이 전혀 없는
     * 의사 요소의 스타일(상속하는 속성은 요소에서 오고 나머지는 초기값).
     *
     * @param e      the originating element / 원본 요소
     * @param pseudo the pseudo-element name without colons / 콜론 없는 의사 요소 이름
     * @return the style, never null / 스타일. null 이 아니다
     */
    public ComputedStyle getPseudoComputedStyle(Element e, String pseudo) {
        ComputedStyle s = getPseudoStyle(e, pseudo);
        if (s != null) {
            return s;
        }
        LengthContext lc = new LengthContext(LengthContext.DEFAULT_FONT_SIZE, LengthContext.DEFAULT_FONT_SIZE, LengthContext.DEFAULT_FONT_SIZE * 1.2,
                env.getWidth(), env.getHeight());
        return StyleComputer.compute(new CssValue[Prop.COUNT + 1], new LinkedHashMap<String, List<ComponentValue>>(), getComputedStyle(e), lc, false);
    }

    /**
     * Checks that the resolver is usable and the element belongs to the document.
     * <p>
     * 리졸버를 쓸 수 있고 요소가 문서에 속하는지 확인한다.
     *
     * @param e the element / 요소
     */
    private void check(Element e) {
        if (disposed) {
            throw new IllegalStateException("the style resolver was disposed");
        }
        if (e.getOwnerDocument() != doc) {
            throw new IllegalArgumentException("the element belongs to another document");
        }
    }

    /**
     * Builds the rule index if it is missing.
     * <p>
     * 규칙 색인이 없으면 만든다.
     */
    private void ensureIndex() {
        if (index != null) {
            return;
        }
        RuleIndex idx = new RuleIndex(env, doc.getCompatibilityMode() == Document.QUIRKS_MODE);
        idx.addSheet(UaStyleSheet.standards());
        // 표 안의 폼 숨김 같은 HTML 문서 전용 규칙은 XML 문서에는 적용하지 않는다.
        if (doc.isHtmlDocument()) {
            idx.addSheet(UaStyleSheet.htmlOnly());
        }
        if (doc.getCompatibilityMode() == Document.QUIRKS_MODE) {
            idx.addSheet(UaStyleSheet.quirks());
        }
        // 사용자 시트와 작성자 시트를 차례로 색인에 넣는다.
        for (int i = 0; i < user.size(); i++) {
            idx.addSheet(user.get(i));
        }
        // author 의 항목을 하나씩 처리한다.
        for (int i = 0; i < author.size(); i++) {
            idx.addSheet(author.get(i));
        }
        index = idx;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 컨테이너 질의(CSS Containment 3, CSS Conditional 5)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Tells whether the style sheets contain a rule inside <code>@container</code>: then the styles depend on the size of the containers, which only
     * the layout knows, and the layout has to report the sizes with {@link #updateContainerSizes(Map)}.
     * <p>
     * 스타일 시트에 <code>@container</code> 안의 규칙이 있는지 알려 준다. 있으면 스타일이 컨테이너 크기에 달려 있는데 그것은 레이아웃만 알므로, 레이아웃이
     * {@link #updateContainerSizes(Map)} 로 크기를 알려야 한다.
     *
     * @return true if container queries are in use / 컨테이너 질의가 쓰이면 true
     */
    public boolean usesContainerQueries() {
        ensureIndex();
        return index.usesContainers;
    }

    /**
     * Returns the <code>@font-feature-values</code> rules of the style sheets that apply, in document order (CSS Fonts 4 section 6.9): the function
     * values of <code>font-variant-alternates</code> look their names up in them.
     * <p>
     * 적용되는 스타일 시트의 <code>@font-feature-values</code> 규칙을 문서 순서로 반환한다(CSS Fonts 4 6.9 절): <code>font-variant-alternates</code> 의 함수 값이 여기서 이름을 찾는다.
     *
     * @return a copy of the list / 목록의 복사본
     */
    public List<org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule> getFontFeatureValues() {
        ensureIndex();
        return new ArrayList<org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule>(index.fontFeatureValues());
    }

    /**
     * Returns the counter styles defined by the style sheets (<code>@counter-style</code>), by name.
     * <p>
     * 스타일 시트가 정의한 카운터 스타일(<code>@counter-style</code>)을 이름별로 반환한다.
     *
     * @return a copy of the definitions in the order of their first definition / 처음 정의한 순서의 정의 복사본
     */
    public Map<String, org.duckdns.hjow.swingwebpanel.css.cssom.CounterStyleRule> getCounterStyleRules() {
        ensureIndex();
        return new LinkedHashMap<String, org.duckdns.hjow.swingwebpanel.css.cssom.CounterStyleRule>(index.counterStyleRules());
    }

    /**
     * Returns the position option of a name (<code>@position-try</code>): the last rule of that name in the style sheets that apply to the environment
     * (CSS Anchor Positioning 1 section 6.4: rules of one name cascade like <code>@keyframes</code>).
     * <p>
     * 이름의 위치 대안(<code>@position-try</code>)을 반환한다: 환경에 적용되는 스타일 시트에서 그 이름의 마지막 규칙(CSS Anchor Positioning 1 6.4 절: 같은 이름의 규칙은
     * <code>@keyframes</code> 처럼 캐스케이드된다).
     *
     * @param name the dashed identifier / 대시 식별자
     * @return the rule or null / 규칙. 없으면 null
     */
    public org.duckdns.hjow.swingwebpanel.css.cssom.PositionTryRule getPositionTryRule(String name) {
        ensureIndex();
        return index.positionTryRules().get(name);
    }

    /**
     * Returns the page rules (<code>@page</code>) of the style sheets that apply to the environment, in the order of the sources. {@link PageStyles}
     * turns them into the page box of a page.
     * <p>
     * 환경에 적용되는 스타일 시트의 <code>@page</code> 규칙을 소스 순서로 반환한다. {@link PageStyles} 가 이것으로 쪽의 쪽 상자를 만든다.
     *
     * @return a copy of the rules / 규칙 복사본
     */
    public List<org.duckdns.hjow.swingwebpanel.css.cssom.PageRule> getPageRules() {
        ensureIndex();
        return new ArrayList<org.duckdns.hjow.swingwebpanel.css.cssom.PageRule>(index.pageRules());
    }

    /**
     * Returns the names of the registered custom properties (<code>@property</code>) that have a typed syntax, so that their values can be animated: a property
     * with the universal syntax <code>*</code> has no type to interpolate.
     * <p>
     * 형 있는 문법을 가진 등록된 사용자 정의 속성(<code>@property</code>)의 이름을 반환한다. 값을 애니메이션할 수 있도록 하기 위한 것이며 보편 문법 <code>*</code> 의
     * 속성은 보간할 형이 없다.
     *
     * @return the names in the order of the registration / 등록 순서의 이름
     */
    public List<String> registeredPropertyNames() {
        ensureIndex();
        List<String> out = new ArrayList<String>();
        // 등록된 속성 중 형 있는 문법을 가진 것의 이름을 모은다.
        for (RegisteredProperty p : index.registrations().values()) {
            if (p.isTyped()) {
                out.add(p.name);
            }
        }
        return out;
    }

    /**
     * Returns the typed value of a registered custom property in a style (a length, a number, a color ...), parsed from its computed text with the syntax of the
     * registration.
     * <p>
     * 스타일에 있는 등록된 사용자 정의 속성의 형 있는 값(길이, 숫자, 색 등)을 반환한다. 등록의 문법으로 계산된 텍스트를 파싱한 것이다.
     *
     * @param s    the style / 스타일
     * @param name the name of the property / 속성 이름
     * @return the typed value, or null if the property is not registered with a typed syntax or has no value / 형 있는 값. 형 있는 문법으로 등록되지 않았거나 값이 없으면 null
     */
    public CssValue typedCustomValue(ComputedStyle s, String name) {
        ensureIndex();
        RegisteredProperty p = index.registrations().get(name);
        org.duckdns.hjow.swingwebpanel.css.values.CssRaw raw = s.getCustomProperty(name);
        if (p == null || !p.isTyped() || raw == null) {
            return null;
        }
        return p.typed(raw.getTokens());
    }

    /**
     * Computes the typed value of a declaration of a registered property (in a keyframe, for example) against the font size of a style: the lengths become pixels.
     * <p>
     * 등록된 속성의 선언(예: 키프레임 안의 것)의 형 있는 값을 스타일의 글꼴 크기를 기준으로 계산한다: 길이는 픽셀이 된다.
     *
     * @param name   the name of the property / 속성 이름
     * @param tokens the declared tokens / 선언한 토큰
     * @param base   the style that gives the font size / 글꼴 크기를 주는 스타일
     * @return the computed typed value, or null if the declaration is not valid for the syntax / 계산된 형 있는 값. 문법에 맞지 않으면 null
     */
    public CssValue computeRegisteredValue(String name, List<ComponentValue> tokens, ComputedStyle base) {
        ensureIndex();
        RegisteredProperty p = index.registrations().get(name);
        CssValue v = p == null ? null : p.typed(tokens);
        if (v == null) {
            return null;
        }
        double fs = base.fontSizePx();
        LengthContext lc = new LengthContext(fs, base.rootFontSizePx(), fs * 1.2, env.getWidth(), env.getHeight()).withPrefersDark("dark".equals(env.getColorScheme()));
        return StyleComputer.map(v, lc);
    }

    /**
     * Computes the style of an element again with other values for some custom properties, as if the element had declared them last: the declarations that
     * use the properties with <code>var()</code> see the new values. This is how an animation of a registered custom property reaches the properties that
     * depend on it. The style is not cached and the animator is not asked.
     * <p>
     * 요소의 스타일을 일부 사용자 정의 속성의 다른 값으로 다시 계산한다. 요소가 그 값을 가장 나중에 선언한 것처럼 계산하므로 <code>var()</code> 로 그 속성을 쓰는 선언이
     * 새 값을 본다. 등록된 사용자 정의 속성의 애니메이션이 그에 의존하는 속성에 닿는 방법이다. 스타일은 캐시하지 않고 애니메이터에게 묻지도 않는다.
     *
     * @param e         the element, which must belong to the document / 요소. 문서에 속해야 한다
     * @param overrides the new values (CSS text) by custom property name / 사용자 정의 속성 이름별 새 값(CSS 문자열)
     * @return the style / 스타일
     */
    public ComputedStyle restyleWithCustom(Element e, Map<String, String> overrides) {
        check(e);
        ensureIndex();
        Element p = e.getParentElement();
        ComputedStyle parent = p == null ? null : getComputedStyle(p);
        return compute(e, parent, null, new MatchContext(null, state), false, overrides);
    }

    /**
     * Records the sizes of the query containers that a layout measured. A container whose size differs from the recorded one (or that is new, or
     * gone) has its descendants' styles dropped, because the <code>@container</code> rules they match may have changed; no listener is notified,
     * as this happens while the layout runs: the caller lays out again if the result is true.
     * <p>
     * 레이아웃이 잰 질의 컨테이너의 크기를 기록한다. 기록과 크기가 다른(또는 새로 생겼거나 사라진) 컨테이너는 자손의 스타일을 버린다. 자손이 일치하는 <code>@container</code>
     * 규칙이 바뀌었을 수 있기 때문이다. 레이아웃이 도는 중에 일어나는 일이므로 리스너에는 알리지 않으며, 결과가 true 이면 호출한 쪽이 다시 배치한다.
     *
     * @param measured the content box sizes {width, height} by container element / 컨테이너 요소별 content box 크기 {너비, 높이}
     * @return true if any recorded size changed / 기록한 크기가 하나라도 바뀌었으면 true
     */
    public boolean updateContainerSizes(Map<Element, double[]> measured) {
        boolean changed = false;
        List<Element> gone = new ArrayList<Element>();
        // 더는 컨테이너가 아닌(레이아웃에 없는) 요소를 찾는다.
        for (Element e : containerSizes.keySet()) {
            if (!measured.containsKey(e)) {
                gone.add(e);
            }
        }
        // 사라진 컨테이너의 기록과 서브트리 스타일을 지운다.
        for (int i = 0; i < gone.size(); i++) {
            containerSizes.remove(gone.get(i));
            purgeSubtree(gone.get(i));
            changed = true;
        }
        // 잰 크기를 기록과 비교해 바뀐 컨테이너의 서브트리 스타일을 버린다.
        for (Map.Entry<Element, double[]> m : measured.entrySet()) {
            double[] old = containerSizes.get(m.getKey());
            double[] now = m.getValue();
            if (old == null || Math.abs(old[0] - now[0]) > 0.01 || Math.abs(old[1] - now[1]) > 0.01) {
                containerSizes.put(m.getKey(), new double[] {now[0], now[1]});
                purgeSubtree(m.getKey());
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Tells whether all the container conditions of a rule hold for an element.
     * <p>
     * 규칙의 컨테이너 조건이 모두 요소에서 성립하는지 알려 준다.
     *
     * @param conds  the conditions from the outermost <code>@container</code> / 가장 바깥 <code>@container</code> 부터의 조건
     * @param el     the element / 요소
     * @param pseudo the pseudo-element being styled or null / 스타일을 계산하는 의사 요소. 없으면 null
     * @return true if every condition holds / 조건이 모두 성립하면 true
     */
    private boolean containersMatch(ContainerCondition[] conds, Element el, String pseudo) {
        // 조건마다 질의 컨테이너를 찾아 평가한다.
        for (int i = 0; i < conds.length; i++) {
            if (!containerMatches(conds[i], el, pseudo)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluates one container condition: finds the nearest ancestor that is a query container for it (it has the container name that the query names,
     * a container type, and size containment in the axes the query asks for; for a query of custom properties only the name matters, and the
     * nearest ancestor is the parent) and evaluates the condition against its content box size. A container that no layout has measured yet makes
     * the condition false.
     * <p>
     * 컨테이너 조건 하나를 평가한다: 그 조건의 질의 컨테이너인 가장 가까운 조상을 찾아(질의가 말하는 컨테이너 이름이 있고, 컨테이너 종류가 있으며, 질의가 요구하는 축에 크기
     * 격리가 있다. 사용자 정의 속성 질의는 이름만 보며 가장 가까운 조상은 부모이다) 그 content box 크기로 조건을 평가한다. 아직 레이아웃이 재지 않은 컨테이너는 조건을
     * 거짓으로 만든다.
     *
     * @param cond   the condition / 조건
     * @param el     the element / 요소
     * @param pseudo the pseudo-element name or null / 의사 요소 이름 또는 null
     * @return true if it holds / 성립하면 true
     */
    private boolean containerMatches(ContainerCondition cond, Element el, String pseudo) {
        if (!cond.isValid()) {
            return false;
        }
        boolean size = cond.usesSize();
        // 의사 요소는 원본 요소 자신도 컨테이너로 삼을 수 있고, 요소는 엄격한 조상만 가능하다.
        for (Element a = pseudo != null ? el : el.getParentElement(); a != null; a = a.getParentElement()) {
            ComputedStyle s = getComputedStyle(a);
            if (cond.getName() != null && !hasContainerName(s, cond.getName())) {
                continue;
            }
            if (size) {
                String type = s.keyword(Prop.CONTAINER_TYPE);
                if (type == null || "normal".equals(type)) {
                    continue;
                }
                String wm = s.keyword(Prop.WRITING_MODE);
                boolean vertical = wm != null && (wm.startsWith("vertical") || wm.startsWith("sideways"));
                int have = "size".equals(type) ? ContainerCondition.AXIS_WIDTH | ContainerCondition.AXIS_HEIGHT
                        : vertical ? ContainerCondition.AXIS_HEIGHT : ContainerCondition.AXIS_WIDTH;
                // 질의가 요구하는 축을 컨테이너가 격리하지 않으면 이 컨테이너는 쓸 수 없다.
                if ((cond.axes(vertical) & ~have) != 0) {
                    continue;
                }
                double[] sz = containerSizes.get(a);
                return sz != null && cond.matches(sz[0], sz[1], vertical, lookup(s));
            }
            return cond.matches(0, 0, false, lookup(s));
        }
        return false;
    }

    /**
     * Tells whether a container style names a container name.
     * <p>
     * 컨테이너 스타일이 컨테이너 이름 하나를 가졌는지 알려 준다.
     *
     * @param s    the computed style of the container / 컨테이너의 계산 스타일
     * @param name the name / 이름
     * @return true if <code>container-name</code> lists it / <code>container-name</code> 에 있으면 true
     */
    private static boolean hasContainerName(ComputedStyle s, String name) {
        CssValue v = s.get(Prop.CONTAINER_NAME);
        if (v == null) {
            return false;
        }
        String[] names = v.toCssText().split("\\s+");
        // 이름 목록에서 같은 이름(대소문자 구분)을 찾는다.
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Makes the custom property lookup of a container style for <code>style()</code> queries.
     * <p>
     * <code>style()</code> 질의를 위해 컨테이너 스타일의 사용자 정의 속성 조회를 만든다.
     *
     * @param s the computed style of the container / 컨테이너의 계산 스타일
     * @return the lookup / 조회
     */
    private static ContainerCondition.StyleLookup lookup(final ComputedStyle s) {
        return new ContainerCondition.StyleLookup() {
            /**
             * Returns the computed text of a custom property.
             * <p>
             * 사용자 정의 속성의 계산된 문자열을 반환한다.
             *
             * @param name the name / 이름
             * @return the text or null / 문자열 또는 null
             */
            public String customProperty(String name) {
                org.duckdns.hjow.swingwebpanel.css.values.CssRaw r = s.getCustomProperty(name);
                return r == null ? null : r.toCssText();
            }
        };
    }

    /**
     * Returns the tier of the normal declarations of an origin.
     * <p>
     * 출처의 일반 선언의 층을 반환한다.
     *
     * @param origin origin constant / 출처 상수
     * @return tier / 층
     */
    private static int normalTier(int origin) {
        return origin == StyleSheet.ORIGIN_USER_AGENT ? TIER_UA : origin == StyleSheet.ORIGIN_USER ? TIER_USER : TIER_AUTHOR;
    }

    /**
     * Returns the tier of the important declarations of an origin.
     * <p>
     * 출처의 중요한 선언의 층을 반환한다.
     *
     * @param origin origin constant / 출처 상수
     * @return tier / 층
     */
    private static int importantTier(int origin) {
        return origin == StyleSheet.ORIGIN_USER_AGENT ? TIER_UA_IMPORTANT : origin == StyleSheet.ORIGIN_USER ? TIER_USER_IMPORTANT : TIER_AUTHOR_IMPORTANT;
    }

    /**
     * Computes the style of an element or of one of its pseudo-elements.
     * <p>
     * 요소나 그 의사 요소의 스타일을 계산한다.
     *
     * @param el     the element / 요소
     * @param parent the parent's computed style (for a pseudo-element the style of the element) / 부모의 계산 스타일(의사 요소는 요소의 스타일)
     * @param pseudo the pseudo-element name or null / 의사 요소 이름 또는 null
     * @param ctx    the matching context / 매칭 컨텍스트
     * @return the style, or null for a pseudo-element no rule targets / 스타일. 어떤 규칙도 가리키지 않는 의사 요소이면 null
     */
    private ComputedStyle compute(Element el, ComputedStyle parent, String pseudo, MatchContext ctx) {
        return compute(el, parent, pseudo, ctx, false);
    }

    /**
     * Computes the style of an element or of one of its pseudo-elements, optionally as the <em>starting style</em> of CSS Transitions Level 2
     * (section 3.3): the after-change style with the rules inside <code>@starting-style</code> applied in addition.
     * <p>
     * 요소나 그 의사 요소의 스타일을 계산하며, 필요하면 CSS Transitions Level 2(3.3 절)의 <em>시작 스타일</em>로 계산한다: 변경 후 스타일에
     * <code>@starting-style</code> 안의 규칙을 더해 적용한 것.
     *
     * @param el       the element / 요소
     * @param parent   the parent's computed style / 부모의 계산 스타일
     * @param pseudo   the pseudo-element name or null / 의사 요소 이름 또는 null
     * @param ctx      the matching context / 매칭 컨텍스트
     * @param starting true to include the rules of <code>@starting-style</code> / <code>@starting-style</code> 의 규칙을 포함하면 true
     * @return the style, or null for a pseudo-element no rule targets / 스타일. 어떤 규칙도 가리키지 않는 의사 요소이면 null
     */
    private ComputedStyle compute(Element el, ComputedStyle parent, String pseudo, MatchContext ctx, boolean starting) {
        return compute(el, parent, pseudo, ctx, starting, null);
    }

    /**
     * Computes the style of an element or of one of its pseudo-elements, with values for some custom properties that win over every declaration
     * (the animated values of registered custom properties).
     * <p>
     * 요소나 그 의사 요소의 스타일을 계산하되, 일부 사용자 정의 속성의 값이 모든 선언을 이긴다(등록된 사용자 정의 속성의 애니메이션 값).
     *
     * @param el        the element / 요소
     * @param parent    the parent's computed style / 부모의 계산 스타일
     * @param pseudo    the pseudo-element name or null / 의사 요소 이름 또는 null
     * @param ctx       the matching context / 매칭 컨텍스트
     * @param starting  true to include the rules of <code>@starting-style</code> / <code>@starting-style</code> 의 규칙을 포함하면 true
     * @param overrides the values (CSS text) of custom properties that win, or null / 이기는 사용자 정의 속성의 값(CSS 문자열). 없으면 null
     * @return the style, or null for a pseudo-element no rule targets / 스타일. 어떤 규칙도 가리키지 않는 의사 요소이면 null
     */
    private ComputedStyle compute(Element el, ComputedStyle parent, String pseudo, MatchContext ctx, boolean starting, Map<String, String> overrides) {
        return compute(el, parent, pseudo, ctx, starting, overrides, false);
    }

    /**
     * Computes the style of an element or of one of its pseudo-elements; for a highlight pseudo-element every highlight property that the cascade does not set
     * inherits from the parent style (CSS Pseudo-Elements 4 section 3.5) and a style is made even when no rule targets the pseudo-element.
     * <p>
     * 요소나 그 의사 요소의 스타일을 계산한다. 하이라이트 의사 요소이면 계단식이 정하지 않은 하이라이트 속성은 모두 부모 스타일에서 물려받고(CSS Pseudo-Elements 4 3.5 절),
     * 의사 요소를 가리키는 규칙이 없어도 스타일을 만든다.
     *
     * @param el        the element / 요소
     * @param parent    the parent style / 부모 스타일
     * @param pseudo    the pseudo-element name or null / 의사 요소 이름 또는 null
     * @param ctx       the matching context / 매칭 컨텍스트
     * @param starting  true to include the rules of <code>@starting-style</code> / <code>@starting-style</code> 의 규칙을 포함하면 true
     * @param overrides the values of custom properties that win, or null / 이기는 사용자 정의 속성의 값. 없으면 null
     * @param highlight true for a highlight pseudo-element / 하이라이트 의사 요소이면 true
     * @return the style, or null for a pseudo-element no rule targets (unless it is a highlight) / 스타일. 어떤 규칙도 가리키지 않는 의사 요소이면(하이라이트가 아니면) null
     */
    private ComputedStyle compute(Element el, ComputedStyle parent, String pseudo, MatchContext ctx, boolean starting, Map<String, String> overrides,
            boolean highlight) {
        // 선언된 속성을 기록해 달라는 요청은 이 호출에만 적용한다(안에서 다른 스타일을 계산해도 섞이지 않게 먼저 비운다).
        java.util.BitSet sink = declaredSink;
        declaredSink = null;
        ensureIndex();
        List<RuleIndex.Entry> cands = new ArrayList<RuleIndex.Entry>();
        String local = el.getLocalName();
        index.candidates(local.toLowerCase(), el.getId(), el.getAttributeNS(null, "class"), cands);
        List<App> apps = new ArrayList<App>();
        // 후보 선택자를 하나씩 시험해 일치하는 것의 선언을 적용 목록에 넣는다.
        for (int i = 0; i < cands.size(); i++) {
            RuleIndex.Entry c = cands.get(i);
            if ((c.pseudo == null) != (pseudo == null) || (pseudo != null && !pseudo.equals(c.pseudo))) {
                continue;
            }
            // @starting-style 안의 규칙은 시작 스타일에만 참가한다.
            if (c.starting && !starting) {
                continue;
            }
            ctx.resetBudget();
            // @scope 안의 규칙이면 :scope 는 그 범위의 루트이다.
            if (c.scopes != null) {
                ctx.setScopeTest(c.scopes[c.scopes.length - 1]);
            }
            boolean matched = SelectorMatcher.matches(c.selector, el, ctx);
            if (c.scopes != null) {
                ctx.setScopeTest(null);
            }
            int proximity = 0;
            // 범위 안의 요소만 일치하고, 가까운 루트일수록 근접도 키가 크다.
            if (matched && c.scopes != null) {
                Element subject = el;
                boolean allowSelf = !c.scopeRelative;
                int d = -1;
                // 가장 안쪽 @scope 부터 바깥으로: 요소가 범위 안에 있어야 하고, 안쪽 범위의 루트는 바깥 범위의 루트 아래에 있어야 한다(중첩된 시작 선택자는 바깥 범위에 상대적이다).
                for (int k = c.scopes.length - 1; k >= 0; k--) {
                    Element root = c.scopes[k].findRoot(subject, allowSelf, ctx);
                    if (root == null) {
                        d = -1;
                        break;
                    }
                    if (k == c.scopes.length - 1) {
                        d = c.scopes[k].distance(el, !c.scopeRelative, ctx);
                    }
                    subject = root;
                    allowSelf = false;
                }
                if (d < 0) {
                    matched = false;
                } else {
                    proximity = Integer.MAX_VALUE - d;
                }
            }
            // @container 안의 규칙은 요소의 질의 컨테이너가 조건을 만족해야 한다.
            if (matched && c.containers != null && !containersMatch(c.containers, el, pseudo)) {
                matched = false;
            }
            if (matched) {
                boolean hasNormal = !c.style.normal().isEmpty() || !c.style.customNormal().isEmpty();
                boolean hasImportant = !c.style.important().isEmpty() || !c.style.customImportant().isEmpty();
                int rank = index.layerRank(c.layer);
                if (hasNormal) {
                    App app = new App(normalTier(c.origin), c.specificity, c.order, c.style, false, rank);
                    app.proximity = proximity;
                    apps.add(app);
                }
                // 중요한 선언은 레이어 순서가 뒤집힌다: 앞선 레이어가 이기고 레이어 없는 선언이 가장 약하다(CSS Cascade 5 6.4.2 절).
                if (hasImportant) {
                    App app = new App(importantTier(c.origin), c.specificity, c.order, c.style, true, Integer.MAX_VALUE - rank);
                    app.proximity = proximity;
                    apps.add(app);
                }
            }
        }
        if (pseudo != null && apps.isEmpty() && !highlight) {
            return null;
        }
        if (pseudo == null) {
            addElementLevel(el, apps, ctx);
        }
        Collections.sort(apps, ORDER);
        CssValue[] cascaded = new CssValue[Prop.COUNT + 1];
        CssValue[] base = null;
        CssValue wmDecl = null;
        CssValue dirDecl = null;
        // 논리 속성의 선언을 어느 물리 속성에 쓸지 정하려면 이기는 writing-mode 와 direction 을 먼저 알아야 한다.
        for (int i = 0; i < apps.size(); i++) {
            Map<Integer, CssValue> m = apps.get(i).important ? apps.get(i).style.important() : apps.get(i).style.normal();
            CssValue w = m.get(Integer.valueOf(Prop.WRITING_MODE));
            CssValue d = m.get(Integer.valueOf(Prop.DIRECTION));
            wmDecl = w != null ? w : wmDecl;
            dirDecl = d != null ? d : dirDecl;
        }
        String[] modes = StyleComputer.modes(wmDecl, dirDecl, parent);
        Map<String, List<ComponentValue>> custom = new LinkedHashMap<String, List<ComponentValue>>();
        // 층 순서대로 선언을 적용한다: 나중에 적용한 것이 앞의 것을 대체한다.
        for (int i = 0; i < apps.size(); i++) {
            App a = apps.get(i);
            if (a.tier >= TIER_HINTS && base == null) {
                base = cascaded.clone();
            }
            Map<Integer, CssValue> m = a.important ? a.style.important() : a.style.normal();
            // 항목 의 항목을 하나씩 처리한다.
            for (Map.Entry<Integer, CssValue> e : m.entrySet()) {
                CssValue v = e.getValue();
                // 논리 속성의 선언은 쓰기 모드와 방향이 가리키는 물리 속성에 쓴다(계단식 순서로 물리 선언과 겨룬다).
                int target = LogicalProps.physical(e.getKey().intValue(), modes[0], modes[1]);
                if (v.isKeyword("revert") || v.isKeyword("revert-layer")) {
                    v = base == null ? null : base[target];
                }
                cascaded[target] = v;
            }
            custom.putAll(a.important ? a.style.customImportant() : a.style.customNormal());
        }
        if (sink != null) {
            // 계단식이 값을 준 속성을 기록한다.
            for (int i = 0; i < cascaded.length; i++) {
                if (cascaded[i] != null) {
                    sink.set(i);
                }
            }
        }
        // 애니메이션이 정한 사용자 정의 속성의 값은 모든 선언을 이긴다.
        if (overrides != null) {
            // 이름마다 새 값을 토큰으로 파싱해 선언 맵에 덮어쓴다.
            for (Map.Entry<String, String> o : overrides.entrySet()) {
                custom.put(o.getKey(), org.duckdns.hjow.swingwebpanel.css.syntax.CssParser.parseComponentValues(o.getValue()));
            }
        }
        // 호환 모드에서 색이 정해지지 않은 table 은 조상이 아니라 본문의 색을 따른다(호환 모드 표준 3.12 의 quirk-inherit).
        if (pseudo == null && cascaded[Prop.COLOR] == null && doc.getCompatibilityMode() == Document.QUIRKS_MODE && LegacyAttributes.is(el, "table")) {
            Element body = bodyElement();
            if (body != null && body != el) {
                cascaded[Prop.COLOR] = getComputedStyle(body).get(Prop.COLOR);
            }
        }
        LengthContext lc = new LengthContext(LengthContext.DEFAULT_FONT_SIZE, LengthContext.DEFAULT_FONT_SIZE, LengthContext.DEFAULT_FONT_SIZE * 1.2,
                env.getWidth(), env.getHeight()).withPrefersDark("dark".equals(env.getColorScheme()));
        // 컨테이너 질의를 쓰는 문서에서는 cqw 같은 컨테이너 상대 길이를 위해 가장 가까운 컨테이너의 크기를 알려 준다.
        if (index.usesContainers) {
            double[] box = containerLengthBase(el, pseudo != null);
            lc = lc.withContainer(box[0], box[1]);
        }
        boolean root = pseudo == null && el == doc.getDocumentElement();
        if (highlight) {
            // 하이라이트 의사 요소에서 계단식이 정하지 않은 하이라이트 속성은 상속 속성이 아니어도 부모의 하이라이트에서 물려받는다.
            for (int i = 0; i < HIGHLIGHT_PROPERTIES.length; i++) {
                CssValue v = cascaded[HIGHLIGHT_PROPERTIES[i]];
                if (v == null || v.isKeyword("unset")) {
                    cascaded[HIGHLIGHT_PROPERTIES[i]] = org.duckdns.hjow.swingwebpanel.css.values.CssKeyword.of("inherit");
                }
            }
        }
        return StyleComputer.compute(cascaded, custom, parent, lc, root, index.registrations());
    }

    /**
     * Returns the body element of the document in the sense of the Quirks Mode Standard: the first child of the document element that is
     * an HTML <code>body</code> element.
     * <p>
     * 호환 모드 표준이 뜻하는 문서의 body 요소를 반환한다: 문서 요소의 첫 HTML <code>body</code> 자식.
     *
     * @return the body or null / body. 없으면 null
     */
    private Element bodyElement() {
        Element root = doc.getDocumentElement();
        if (root == null || !LegacyAttributes.is(root, "html")) {
            return null;
        }
        // 문서 요소의 자식 중 첫 body 를 찾는다.
        for (Node c = root.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof Element && LegacyAttributes.is((Element) c, "body")) {
                return (Element) c;
            }
        }
        return null;
    }

    /**
     * Finds the content box sizes that the container relative lengths of an element (<code>cqw</code>, <code>cqh</code> ...) refer to: for each
     * axis the nearest ancestor (or the element itself for a pseudo-element) that is a query container with containment in that axis and has been
     * measured. NaN for an axis with no such container (the viewport is used then).
     * <p>
     * 요소의 컨테이너 상대 길이(<code>cqw</code>, <code>cqh</code> 등)가 가리키는 content box 크기를 찾는다: 축마다 그 축에 격리가 있고 이미 잰 질의 컨테이너인 가장 가까운
     * 조상(의사 요소는 요소 자신도). 그런 컨테이너가 없는 축은 NaN(그러면 뷰포트를 쓴다).
     *
     * @param el     the element / 요소
     * @param pseudo true for a pseudo-element / 의사 요소이면 true
     * @return {width, height} / {너비, 높이}
     */
    private double[] containerLengthBase(Element el, boolean pseudo) {
        double w = Double.NaN;
        double h = Double.NaN;
        // 요소에서 위로 올라가며 축마다 처음 만나는 컨테이너의 크기를 쓴다.
        for (Element a = pseudo ? el : el.getParentElement(); a != null && (Double.isNaN(w) || Double.isNaN(h)); a = a.getParentElement()) {
            ComputedStyle s = getComputedStyle(a);
            String type = s.keyword(Prop.CONTAINER_TYPE);
            double[] sz = containerSizes.get(a);
            if (type == null || "normal".equals(type) || sz == null) {
                continue;
            }
            String wm = s.keyword(Prop.WRITING_MODE);
            boolean vertical = wm != null && (wm.startsWith("vertical") || wm.startsWith("sideways"));
            boolean both = "size".equals(type);
            if (Double.isNaN(w) && (both || !vertical)) {
                w = sz[0];
            }
            if (Double.isNaN(h) && (both || vertical)) {
                h = sz[1];
            }
        }
        return new double[] {w, h};
    }

    /**
     * Adds the declarations that belong to the element itself: the presentational hints and the <code>style</code> attribute.
     * <p>
     * 요소 자체에 속한 선언을 더한다: 표현 힌트와 <code>style</code> 속성.
     *
     * @param el   the element / 요소
     * @param apps receives the applications / 적용 정보를 받는다
     * @param context shared context for dynamic hint dependencies / 동적 힌트 의존성용 공용 문맥
     */
    private void addElementLevel(Element el, List<App> apps, MatchContext context) {
        String base = doc.getBaseUri() != null && doc.getBaseUri().length() > 0 ? doc.getBaseUri() : doc.getUrl();
        if (hints != null) {
            String h = hints instanceof StatefulPresentationalHints
                    ? ((StatefulPresentationalHints) hints).hintsFor(el, base, context) : hints.hintsFor(el, base);
            if (h != null) {
                apps.add(new App(TIER_HINTS, 0, -1, CssStyleDeclaration.parse(h, base, diag), false));
            }
        }
        String inline = el.getAttributeNS(null, "style");
        if (inline != null && inline.length() > 0) {
            // 호환 모드 문서의 style 속성도 호환 모드로 파싱한다.
            CssStyleDeclaration d = CssStyleDeclaration.parse(inline, base, diag, doc.getCompatibilityMode() == Document.QUIRKS_MODE);
            apps.add(new App(TIER_AUTHOR, Long.MAX_VALUE, Integer.MAX_VALUE, d, false));
            apps.add(new App(TIER_AUTHOR_IMPORTANT, Long.MAX_VALUE, Integer.MAX_VALUE, d, true));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 무효화
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Notifies the listeners.
     * <p>
     * 리스너에 알린다.
     *
     * @param root the stale subtree root or null for everything / 낡은 서브트리의 루트. 전체이면 null
     */
    private void notifyListeners(Element root) {
        List<StyleChangeListener> l = listeners;
        // 리스너마다 알리며 한 리스너의 예외가 다른 리스너를 막지 않게 한다.
        for (int i = 0; i < l.size(); i++) {
            try {
                l.get(i).stylesInvalidated(root);
            } catch (RuntimeException ex) {
                CssDiagnostics.report(diag, CssDiagnostics.RECOVERED, "StyleChangeListener", "a listener threw " + ex);
            }
        }
    }

    /**
     * Removes the cached styles of an element and of all its descendant elements.
     * <p>
     * 요소와 모든 자손 요소의 캐시된 스타일을 지운다.
     *
     * @param root the root of the subtree / 서브트리의 루트
     */
    private void purgeSubtree(Element root) {
        if (cache.isEmpty()) {
            return;
        }
        // 서브트리의 모든 요소를 캐시에서 지운다.
        for (Node n = root; n != null; n = Node.nextInTree(n, root)) {
            if (n instanceof Element) {
                cache.remove(n);
                dynamic.remove(n);
            }
        }
    }

    /**
     * Invalidates a subtree and notifies the listeners.
     * <p>
     * 서브트리를 무효화하고 리스너에 알린다.
     *
     * @param root the root of the stale subtree / 낡은 서브트리의 루트
     */
    private void invalidateSubtree(Element root) {
        purgeSubtree(root);
        version++;
        notifyListeners(root);
    }

    /**
     * Reacts to a DOM change by invalidating exactly the styles it can affect.
     * <p>
     * DOM 변경에 반응해 그 변경이 영향을 줄 수 있는 스타일만 무효화한다.
     *
     * @param record    the change / 변경
     * @param connected true if the target is connected to the document / 대상이 문서에 연결되어 있으면 true
     */
    public void mutated(MutationRecord record, boolean connected) {
        if (disposed) {
            return;
        }
        Node t = record.getTarget();
        int type = record.getType();
        if (type == MutationRecord.ATTRIBUTES && t instanceof Element) {
            Element el = (Element) t;
            noteSheetSource(el);
            if (cache.isEmpty()) {
                return;
            }
            // body의 링크 색은 본문 밖의 링크에도 적용되므로 문서 전체를 무효화한다.
            String attribute = record.getAttributeName();
            if (Namespaces.HTML.equals(el.getNamespaceURI()) && "body".equals(el.getLocalName())
                    && ("link".equals(attribute) || "vlink".equals(attribute) || "alink".equals(attribute))) {
                invalidate();
                return;
            }
            RuleIndex idx = index;
            if (idx != null && idx.usesHas) {
                invalidate();
            } else if (idx != null && idx.usesSiblings && el.getParentElement() != null) {
                invalidateSubtree(el.getParentElement());
            } else {
                invalidateSubtree(el);
            }
            return;
        }
        if (type == MutationRecord.CHILD_LIST) {
            List<Node> added = record.getAddedNodes();
            List<Node> removed = record.getRemovedNodes();
            // 본문 교체는 같은 문서의 모든 링크 표현 힌트에 영향을 줄 수 있다.
            if (t == doc || t == doc.getDocumentElement()) {
                invalidate();
            }
            if (t instanceof Element) {
                noteSheetSource((Element) t);
            }
            // 추가되거나 제거된 노드의 캐시를 지우고 style/link 요소가 있었는지 기록한다.
            for (int i = 0; i < added.size(); i++) {
                noteTree(added.get(i));
            }
            // removed 의 항목을 하나씩 처리한다.
            for (int i = 0; i < removed.size(); i++) {
                noteTree(removed.get(i));
            }
            if (t instanceof Element) {
                quirkStructureChanged((Element) t);
            }
            if (cache.isEmpty()) {
                return;
            }
            RuleIndex idx = index;
            if (idx != null && idx.usesHas) {
                invalidate();
            } else if (idx != null && idx.usesStructural && t instanceof Element) {
                invalidateSubtree((Element) t);
            }
            return;
        }
        if (type == MutationRecord.CHARACTER_DATA) {
            Element p = t.getParentElement();
            if (p != null) {
                noteSheetSource(p);
                quirkStructureChanged(p);
                RuleIndex idx = index;
                if (!cache.isEmpty() && idx != null && idx.usesStructural) {
                    invalidateSubtree(p);
                }
            }
        }
    }

    /**
     * Drops the cached styles that the margin quirks of quirks mode make depend on the children and texts of an element: the element
     * itself (its blank state) and, for <code>body</code>, <code>td</code> and <code>th</code>, its child elements (their substantial
     * siblings). See {@link QuirkHints}.
     * <p>
     * 호환 모드의 여백 quirk 가 요소의 자식과 텍스트에 의존하게 만드는 캐시된 스타일을 버린다: 요소 자신(비어 있는지)과 <code>body</code>, <code>td</code>,
     * <code>th</code> 의 자식 요소(실질적인 형제). {@link QuirkHints} 참조.
     *
     * @param el the element whose children or texts changed / 자식이나 텍스트가 바뀐 요소
     */
    private void quirkStructureChanged(Element el) {
        if (doc.getCompatibilityMode() != Document.QUIRKS_MODE || cache.isEmpty()) {
            return;
        }
        cache.remove(el);
        dynamic.remove(el);
        // body/td/th 의 자식 요소는 서로의 실질적인 형제 여부에 따라 스타일이 달라진다.
        if (QuirkHints.isQuirkParent(el)) {
            for (Node c = el.getFirstChild(); c != null; c = c.getNextSibling()) {
                if (c instanceof Element) {
                    cache.remove(c);
                    dynamic.remove(c);
                }
            }
        }
        version++;
        notifyListeners(el);
    }

    /**
     * Records whether an element is a source of style sheets.
     * <p>
     * 요소가 스타일 시트의 출처인지 기록한다.
     *
     * @param el the element / 요소
     */
    private void noteSheetSource(Element el) {
        if (Namespaces.HTML.equals(el.getNamespaceURI())) {
            String n = el.getLocalName();
            if (n.equalsIgnoreCase("style") || n.equalsIgnoreCase("link")) {
                sheetSourceChanged = true;
            }
        }
    }

    /**
     * Handles an added or removed node: its cached styles are dropped and style or link elements are noted.
     * <p>
     * 추가되거나 제거된 노드를 처리한다: 캐시된 스타일을 지우고 style 이나 link 요소를 기록한다.
     *
     * @param n the node / 노드
     */
    private void noteTree(Node n) {
        // 노드의 서브트리를 훑으며 요소의 캐시를 지우고 시트 출처를 기록한다.
        for (Node c = n; c != null; c = Node.nextInTree(c, n)) {
            if (c instanceof Element) {
                cache.remove(c);
                dynamic.remove(c);
                noteSheetSource((Element) c);
            }
        }
    }
}
