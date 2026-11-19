package org.duckdns.hjow.swingwebpanel.css.anim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.css.props.LogicalProps;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssRaw;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * A keyframe effect of Web Animations Level 1 (sections 5.3 and 6.6, <code>KeyframeEffect</code>): an effect target (an element and an optional
 * pseudo-element such as <code>::before</code>), the timing properties ({@link EffectTiming}), the composite operation of the effect and the list
 * of keyframes. A {@link Keyframe} holds its offset (null for automatic spacing), its computed offset, its timing function, its keyframe-specific
 * composite operation (null for <code>auto</code>) and its property values as specified (CSS names and serialized values, what
 * <code>getKeyframes()</code> reports) together with the parsed declarations. {@link #apply} computes the effect value of every target property for
 * an iteration progress (section 5.3.4: property-specific keyframes, the implicit keyframes at 0 and 1 with the underlying value, the interval,
 * the keyframe timing function, interpolation by {@link Interpolator} and composition by {@link Compositor}) and returns the style with these values.
 * The values of the keyframes are computed against the target element (relative lengths use its font size, logical properties map to physical ones)
 * and cached while the keyframes, the font size, the color and the writing mode of the target do not change. The iteration composite operation of
 * Web Animations Level 2 is stored and reported but not applied.
 * <p>
 * Web Animations Level 1 의 키프레임 효과(5.3 절과 6.6 절, <code>KeyframeEffect</code>): 효과 대상(요소와 <code>::before</code> 같은 선택적 의사 요소), 시간 속성
 * ({@link EffectTiming}), 효과의 합성 연산, 키프레임 목록. {@link Keyframe} 는 오프셋(자동 간격이면 null), 계산된 오프셋, 완화 함수, 키프레임별 합성 연산
 * (<code>auto</code> 이면 null), 지정한 속성 값(CSS 이름과 직렬화한 값, <code>getKeyframes()</code> 가 알려 주는 것)과 해석한 선언을 가진다. {@link #apply} 는 반복
 * 진행도에 대한 모든 대상 속성의 효과 값을 계산하고(5.3.4 절: 속성별 키프레임, 기저 값을 가진 0 과 1 의 암묵 키프레임, 구간, 키프레임 완화 함수,
 * {@link Interpolator} 의 보간, {@link Compositor} 의 합성) 그 값을 가진 스타일을 반환한다. 키프레임의 값은 대상 요소를 기준으로 계산하며(상대 길이는 그 글꼴 크기,
 * 논리 속성은 물리 속성으로) 키프레임, 대상의 글꼴 크기, 색, 쓰기 모드가 바뀌지 않는 동안 캐시한다. Web Animations Level 2 의 반복 합성 연산은 보관하고 알려 주지만
 * 적용하지 않는다.
 */
public final class KeyframeEffectModel {

    /**
     * The properties that keyframes never animate (their animation type is "not animatable"): the properties of animations and transitions themselves,
     * <code>direction</code>, <code>unicode-bidi</code>, <code>writing-mode</code>, <code>text-orientation</code>, <code>will-change</code> and <code>all</code>.
     * <p>
     * 키프레임이 결코 애니메이션하지 않는 속성(애니메이션 종류가 "not animatable"): 애니메이션과 전환 자체의 속성, <code>direction</code>, <code>unicode-bidi</code>,
     * <code>writing-mode</code>, <code>text-orientation</code>, <code>will-change</code>, <code>all</code>.
     */
    private static final Set<String> NOT_ANIMATABLE = Collections.unmodifiableSet(new HashSet<String>(java.util.Arrays.asList(new String[] { "all",
        "animation", "transition", "direction", "unicode-bidi", "writing-mode", "text-orientation", "will-change", "transition-behavior" })));

    /** The target element, or null. 대상 요소. 없으면 null. */
    private Element target;
    /** The target pseudo-selector (<code>::before</code>), or null. 대상 의사 선택자(<code>::before</code>). 없으면 null. */
    private String pseudo;
    /** The timing properties. 시간 속성. */
    private final EffectTiming timing;
    /** The composite operation of the effect. 효과의 합성 연산. */
    private String composite = "replace";
    /** The iteration composite operation (Web Animations Level 2, stored only). 반복 합성 연산(Web Animations Level 2, 보관만). */
    private String iterationComposite = "replace";
    /** The keyframes, never null. 키프레임. null 이 아니다. */
    private List<Keyframe> keyframes = new ArrayList<Keyframe>();
    /** The animation that this effect is associated with, or null. 이 효과가 연결된 애니메이션. 없으면 null. */
    WebAnimation animation;
    /** Changes with every change of the keyframes or the target (drops the cached computed keyframes). 키프레임이나 대상이 바뀔 때마다 바뀐다(캐시한 계산 키프레임을 버린다). */
    private int version;
    /** The cached computed keyframes, or null. 캐시한 계산 키프레임. 없으면 null. */
    private Frames frames;

    /**
     * Creates an effect without target, keyframes or delays, with the given timing.
     * <p>
     * 대상, 키프레임, 지연이 없는 효과를 주어진 시간으로 만든다.
     *
     * @param timing the timing, not null / 시간(null 불가)
     */
    public KeyframeEffectModel(EffectTiming timing) {
        if (timing == null) {
            throw new IllegalArgumentException("timing must not be null");
        }
        this.timing = timing;
    }

    /**
     * Creates a copy of an effect (the <code>KeyframeEffect(source)</code> constructor): the target, the keyframes, the composite operation and
     * the specified timing are copied; the copy has no animation.
     * <p>
     * 효과의 복사본을 만든다(<code>KeyframeEffect(source)</code> 생성자): 대상, 키프레임, 합성 연산, 지정한 시간을 복사한다. 복사본에는 애니메이션이 없다.
     *
     * @param source the effect to copy / 복사할 효과
     * @return the copy / 복사본
     */
    public static KeyframeEffectModel copyOf(KeyframeEffectModel source) {
        KeyframeEffectModel e = new KeyframeEffectModel(source.timing.copy());
        e.target = source.target;
        e.pseudo = source.pseudo;
        e.composite = source.composite;
        e.iterationComposite = source.iterationComposite;
        e.keyframes = new ArrayList<Keyframe>(source.keyframes);
        return e;
    }

    // ------------------------------------------------------------------------------------------------ keyframes

    /**
     * One keyframe as the script specified it, with its parsed declarations. Instances are immutable except for the computed offset, which
     * {@link #computeMissingOffsets(List)} sets.
     * <p>
     * 스크립트가 지정한 키프레임 하나와 그 해석한 선언. 인스턴스는 계산된 오프셋을 빼면 바뀌지 않으며 계산된 오프셋은 {@link #computeMissingOffsets(List)} 가 정한다.
     */
    public static final class Keyframe {
        /** The offset, null for automatic spacing. 오프셋. 자동 간격이면 null. */
        private final Double offset;
        /** The computed offset. 계산된 오프셋. */
        private double computedOffset;
        /** The timing function as serialized text. 직렬화한 완화 함수. */
        private final String easingText;
        /** The timing function. 완화 함수. */
        private final Easing easing;
        /** The keyframe-specific composite operation, null for <code>auto</code>. 키프레임별 합성 연산. <code>auto</code> 이면 null. */
        private final String composite;
        /** The specified values by CSS property name (serialized), in the order of the IDL names. CSS 속성 이름별 지정 값(직렬화), IDL 이름 순서. */
        private final Map<String, String> values;
        /** The declarations by longhand property id (shorthands expanded). 개별 속성 id 별 선언(단축 속성은 펼침). */
        private final Map<Integer, CssValue> declared;
        /** The custom property declarations by name. 이름별 사용자 정의 속성 선언. */
        private final Map<String, List<ComponentValue>> custom;

        /**
         * Creates a keyframe.
         * <p>
         * 키프레임을 만든다.
         *
         * @param offset     the offset or null / 오프셋 또는 null
         * @param easingText the serialized easing / 직렬화한 완화
         * @param easing     the easing / 완화
         * @param composite  the composite operation or null / 합성 연산 또는 null
         * @param values     the specified values / 지정 값
         * @param declared   the parsed longhand declarations / 해석한 개별 속성 선언
         * @param custom     the custom property declarations / 사용자 정의 속성 선언
         */
        private Keyframe(Double offset, String easingText, Easing easing, String composite, Map<String, String> values, Map<Integer, CssValue> declared,
                Map<String, List<ComponentValue>> custom) {
            this.offset = offset;
            this.computedOffset = offset == null ? Double.NaN : offset.doubleValue();
            this.easingText = easingText;
            this.easing = easing;
            this.composite = composite;
            this.values = values;
            this.declared = declared;
            this.custom = custom;
        }

        /**
         * Returns the offset.
         * <p>
         * 오프셋을 반환한다.
         *
         * @return the offset, null for automatic spacing / 오프셋. 자동 간격이면 null
         */
        public Double getOffset() {
            return offset;
        }

        /**
         * Returns the computed offset.
         * <p>
         * 계산된 오프셋을 반환한다.
         *
         * @return the offset / 오프셋
         */
        public double getComputedOffset() {
            return computedOffset;
        }

        /**
         * Returns the timing function as serialized text.
         * <p>
         * 완화 함수를 직렬화한 문자열로 반환한다.
         *
         * @return the text / 문자열
         */
        public String getEasing() {
            return easingText;
        }

        /**
         * Returns the keyframe-specific composite operation.
         * <p>
         * 키프레임별 합성 연산을 반환한다.
         *
         * @return the operation, null for <code>auto</code> / 연산. <code>auto</code> 이면 null
         */
        public String getComposite() {
            return composite;
        }

        /**
         * Returns the specified values by CSS property name.
         * <p>
         * CSS 속성 이름별 지정 값을 반환한다.
         *
         * @return an unmodifiable map / 수정할 수 없는 맵
         */
        public Map<String, String> getValues() {
            return Collections.unmodifiableMap(values);
        }
    }

    /**
     * Creates a keyframe from the values of a script (the result of "process a keyframe-like object" for one keyframe): the property-value pairs
     * whose value does not parse for the property are discarded (section 6.6.3). When several pairs name the same longhand, a longhand wins over a
     * shorthand, a shorthand with fewer longhands over one with more, and among equal shorthands the one whose IDL name sorts first (5.3.3).
     * <p>
     * 스크립트의 값으로 키프레임을 만든다(키프레임 하나에 대한 "process a keyframe-like object" 의 결과): 속성에 대해 해석되지 않는 값을 가진 쌍은 버린다(6.6.3 절). 여러 쌍이
     * 같은 개별 속성을 가리키면 개별 속성이 단축 속성을, 개별 속성이 적은 단축 속성이 많은 것을, 같은 단축 속성끼리는 IDL 이름이 먼저 오는 것이 이긴다(5.3.3).
     *
     * @param offset    the offset or null / 오프셋 또는 null
     * @param easing    the easing text, which must parse (see {@link EffectTiming#parseEasing(String)}) / 완화 문자열. 해석되어야 한다
     * @param composite <code>replace</code>, <code>add</code>, <code>accumulate</code> or null for <code>auto</code> / 합성 연산. <code>auto</code> 이면 null
     * @param pairs     the property values by CSS property name, in the order of their IDL names / IDL 이름 순서의 CSS 속성 이름별 값
     * @return the keyframe / 키프레임
     */
    public static Keyframe newKeyframe(Double offset, String easing, String composite, Map<String, String> pairs) {
        CssValue e = EffectTiming.parseEasing(easing);
        if (e == null) {
            throw new IllegalArgumentException("invalid easing " + easing);
        }
        Map<String, String> values = new LinkedHashMap<String, String>();
        List<String> valid = new ArrayList<String>();
        // 쌍마다 그 속성만 가진 선언 블록으로 해석해 보고, 해석되면 직렬화한 지정 값을 기록한다.
        for (Map.Entry<String, String> p : pairs.entrySet()) {
            CssStyleDeclaration one = new CssStyleDeclaration();
            if (!one.setProperty(p.getKey(), p.getValue(), "")) {
                continue;
            }
            String text = one.getPropertyValue(p.getKey());
            values.put(p.getKey(), text.length() > 0 ? text : p.getValue().trim());
            valid.add(p.getKey());
        }
        // 충돌 규칙에 맞게 지는 쪽을 먼저, 이기는 쪽을 나중에 적용한다.
        Collections.sort(valid, new java.util.Comparator<String>() {
            /**
             * Orders the names so that the name that wins a conflict comes last.
             * <p>
             * 충돌에서 이기는 이름이 마지막에 오도록 정렬한다.
             *
             * @param a the first name / 첫 이름
             * @param b the second name / 둘째 이름
             * @return the order / 순서
             */
            public int compare(String a, String b) {
                int ca = longhandCount(a);
                int cb = longhandCount(b);
                if (ca != cb) {
                    return ca > cb ? -1 : 1;
                }
                return idlName(b).compareTo(idlName(a));
            }
        });
        CssStyleDeclaration all = new CssStyleDeclaration();
        // 정렬한 순서대로 하나의 블록에 적용해 개별 속성 선언을 모은다.
        for (String name : valid) {
            all.setProperty(name, pairs.get(name), "");
        }
        Map<Integer, CssValue> declared = new LinkedHashMap<Integer, CssValue>(all.normal());
        // 애니메이션할 수 없는 개별 속성은 단축 속성을 통해서도 들어가지 않게 뺀다.
        for (Integer id : new ArrayList<Integer>(declared.keySet())) {
            if (!animatableId(id.intValue())) {
                declared.remove(id);
            }
        }
        Map<String, List<ComponentValue>> custom = new LinkedHashMap<String, List<ComponentValue>>(all.customNormal());
        return new Keyframe(offset, e.toCssText(), Easing.of(e), composite, values, declared, custom);
    }

    /**
     * Returns the number of longhands of a property name (1 for a longhand and for a custom property).
     * <p>
     * 속성 이름의 개별 속성 수를 반환한다(개별 속성과 사용자 정의 속성은 1).
     *
     * @param name the CSS name / CSS 이름
     * @return the count / 수
     */
    private static int longhandCount(String name) {
        if (PropertyRegistry.isCustomProperty(name)) {
            return 1;
        }
        int[] ids = PropertyRegistry.getInstance().longhandsOf(name);
        return ids == null ? 1 : ids.length;
    }

    /**
     * Tells whether a longhand property id may be animated by keyframes.
     * <p>
     * 개별 속성 id 를 키프레임이 애니메이션할 수 있는지 알려 준다.
     *
     * @param id the property id / 속성 id
     * @return true if animatable / 애니메이션할 수 있으면 true
     */
    private static boolean animatableId(int id) {
        if (id >= Prop.TRANSITION_PROPERTY && id <= Prop.ANIMATION_PLAY_STATE) {
            return false;
        }
        return id != Prop.TRANSITION_BEHAVIOR && id != Prop.DIRECTION && id != Prop.UNICODE_BIDI && id != Prop.WRITING_MODE;
    }

    /**
     * Converts an IDL attribute name to a CSS property name (6.6.2 "IDL attribute name to animation property name"): custom property names stay,
     * <code>cssFloat</code> is <code>float</code>, <code>cssOffset</code> is <code>offset</code>, the other names lose their capitals for dashes
     * (<code>webkitX</code> becomes <code>-webkit-x</code>).
     * <p>
     * IDL 속성 이름을 CSS 속성 이름으로 바꾼다(6.6.2 "IDL attribute name to animation property name"): 사용자 정의 속성 이름은 그대로, <code>cssFloat</code> 는
     * <code>float</code>, <code>cssOffset</code> 는 <code>offset</code>, 나머지는 대문자를 대시로 바꾼다(<code>webkitX</code> 는 <code>-webkit-x</code>).
     *
     * @param idl the IDL name / IDL 이름
     * @return the CSS name / CSS 이름
     */
    public static String cssName(String idl) {
        if (PropertyRegistry.isCustomProperty(idl)) {
            return idl;
        }
        if ("cssFloat".equals(idl)) {
            return "float";
        }
        if ("cssOffset".equals(idl)) {
            return "offset";
        }
        StringBuilder sb = new StringBuilder();
        // 대문자마다 대시와 소문자로 바꾼다.
        for (int i = 0; i < idl.length(); i++) {
            char c = idl.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.append('-').append((char) (c + 32));
            } else {
                sb.append(c);
            }
        }
        String s = sb.toString();
        return s.startsWith("webkit-") ? "-" + s : s;
    }

    /**
     * Converts a CSS property name to its IDL attribute name (6.6.2 "animation property name to IDL attribute name").
     * <p>
     * CSS 속성 이름을 IDL 속성 이름으로 바꾼다(6.6.2 "animation property name to IDL attribute name").
     *
     * @param css the CSS name / CSS 이름
     * @return the IDL name / IDL 이름
     */
    public static String idlName(String css) {
        if (PropertyRegistry.isCustomProperty(css)) {
            return css;
        }
        if ("float".equals(css)) {
            return "cssFloat";
        }
        if ("offset".equals(css)) {
            return "cssOffset";
        }
        String s = css.startsWith("-webkit-") ? css.substring(1) : css;
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        // 대시 다음 글자를 대문자로 바꾸고 대시는 지운다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '-') {
                up = true;
            } else {
                sb.append(up && c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
                up = false;
            }
        }
        return sb.toString();
    }

    /**
     * Tells whether a key of a keyframe object names a property that keyframes can animate: a custom property, or the IDL name of a longhand or
     * shorthand that this engine knows and that is animatable.
     * <p>
     * 키프레임 객체의 키가 키프레임이 애니메이션할 수 있는 속성을 가리키는지 알려 준다: 사용자 정의 속성, 또는 이 엔진이 알고 애니메이션할 수 있는 개별·단축 속성의 IDL 이름.
     *
     * @param key the key / 키
     * @return the CSS name, or null if the key is not such a property / CSS 이름. 그런 속성이 아니면 null
     */
    public static String animatableProperty(String key) {
        if (key == null || key.length() == 0) {
            return null;
        }
        if (PropertyRegistry.isCustomProperty(key)) {
            return key;
        }
        String css = cssName(key);
        // IDL 이름이 아닌 키(대시가 든 이름, 대소문자가 다른 이름)는 거부한다.
        if (!idlName(css).equals(key) || NOT_ANIMATABLE.contains(css)) {
            return null;
        }
        PropertyRegistry reg = PropertyRegistry.getInstance();
        if (reg.byName(css) == null && reg.getShorthand(css) == null) {
            return null;
        }
        if (!css.equals(reg.canonicalName(css)) && !css.startsWith("-webkit-")) {
            return null;
        }
        int[] ids = reg.longhandsOf(css);
        // 개별 속성 중 하나라도 애니메이션할 수 있어야 한다.
        for (int i = 0; ids != null && i < ids.length; i++) {
            if (animatableId(ids[i])) {
                return css;
            }
        }
        return null;
    }

    /**
     * Checks a list of keyframes before it is used (6.6.3): the offsets that are not null must be loosely sorted and between 0 and 1.
     * <p>
     * 키프레임 목록을 쓰기 전에 검사한다(6.6.3): null 이 아닌 오프셋은 느슨하게 정렬되어 있고 0 과 1 사이여야 한다.
     *
     * @param list the keyframes / 키프레임
     * @return null if valid, otherwise the message of the TypeError / 올바르면 null. 아니면 TypeError 의 메시지
     */
    public static String checkOffsets(List<Keyframe> list) {
        List<Double> offsets = new ArrayList<Double>();
        // 키프레임의 오프셋을 모은다.
        for (Keyframe k : list) {
            offsets.add(k.offset);
        }
        return checkOffsetValues(offsets);
    }

    /**
     * Checks a list of keyframe offsets (6.6.3): the values that are not null must be loosely sorted and between 0 and 1.
     * <p>
     * 키프레임 오프셋 목록을 검사한다(6.6.3): null 이 아닌 값은 느슨하게 정렬되어 있고 0 과 1 사이여야 한다.
     *
     * @param offsets the offsets, null for automatic spacing / 오프셋. 자동 간격이면 null
     * @return null if valid, otherwise the message of the TypeError / 올바르면 null. 아니면 TypeError 의 메시지
     */
    public static String checkOffsetValues(List<Double> offsets) {
        double last = Double.NEGATIVE_INFINITY;
        // null 이 아닌 오프셋마다 순서와 범위를 확인한다(순서 위반을 먼저 알린다).
        for (Double k : offsets) {
            if (k == null) {
                continue;
            }
            double o = k.doubleValue();
            if (o < last) {
                return "Offsets must be monotonically non-decreasing.";
            }
            last = o;
        }
        // 범위를 확인한다.
        for (Double k : offsets) {
            if (k == null) {
                continue;
            }
            double o = k.doubleValue();
            if (Double.isNaN(o) || o < 0 || o > 1) {
                return "Offsets must be null or in the range [0,1].";
            }
        }
        return null;
    }

    /**
     * Computes the missing keyframe offsets (5.3.3 "compute missing keyframe offsets"): a null first offset is 0 when there are several keyframes, a
     * null last offset is 1, and the keyframes between two known offsets are spaced evenly.
     * <p>
     * 빠진 키프레임 오프셋을 계산한다(5.3.3 "compute missing keyframe offsets"): 키프레임이 여럿일 때 null 인 첫 오프셋은 0, null 인 마지막 오프셋은 1 이며, 알려진 두 오프셋
     * 사이의 키프레임은 고르게 놓는다.
     *
     * @param list the keyframes / 키프레임
     */
    public static void computeMissingOffsets(List<Keyframe> list) {
        int n = list.size();
        // 우선 지정한 오프셋을 계산된 오프셋으로 쓴다.
        for (Keyframe k : list) {
            k.computedOffset = k.offset == null ? Double.NaN : k.offset.doubleValue();
        }
        if (n == 0) {
            return;
        }
        if (n > 1 && Double.isNaN(list.get(0).computedOffset)) {
            list.get(0).computedOffset = 0;
        }
        if (Double.isNaN(list.get(n - 1).computedOffset)) {
            list.get(n - 1).computedOffset = 1;
        }
        int a = 0;
        // 알려진 오프셋 사이의 null 오프셋을 고르게 채운다.
        while (a < n) {
            if (Double.isNaN(list.get(a).computedOffset)) {
                a++;
                continue;
            }
            int b = a + 1;
            // 다음으로 알려진 오프셋을 가진 키프레임 B 를 찾는다.
            while (b < n && Double.isNaN(list.get(b).computedOffset)) {
                b++;
            }
            if (b >= n) {
                break;
            }
            double oa = list.get(a).computedOffset;
            double ob = list.get(b).computedOffset;
            int span = b - a;
            // A 와 B 사이의 키프레임마다 위치에 비례한 오프셋을 준다.
            for (int i = a + 1; i < b; i++) {
                list.get(i).computedOffset = oa + (ob - oa) * (i - a) / span;
            }
            a = b;
        }
    }

    /**
     * Replaces the keyframes (<code>setKeyframes()</code>); the list must have passed {@link #checkOffsets(List)}.
     * <p>
     * 키프레임을 바꾼다(<code>setKeyframes()</code>). 목록은 {@link #checkOffsets(List)} 를 통과해야 한다.
     *
     * @param list the keyframes / 키프레임
     */
    public void setKeyframes(List<Keyframe> list) {
        List<Keyframe> copy = new ArrayList<Keyframe>(list);
        computeMissingOffsets(copy);
        keyframes = copy;
        changed();
    }

    /**
     * Returns the keyframes with their computed offsets (<code>getKeyframes()</code>).
     * <p>
     * 계산된 오프셋과 함께 키프레임을 반환한다(<code>getKeyframes()</code>).
     *
     * @return an unmodifiable list / 수정할 수 없는 목록
     */
    public List<Keyframe> getKeyframes() {
        return Collections.unmodifiableList(keyframes);
    }

    // ------------------------------------------------------------------------------------------------ properties

    /**
     * Returns the timing properties.
     * <p>
     * 시간 속성을 반환한다.
     *
     * @return the timing / 시간
     */
    public EffectTiming getTiming() {
        return timing;
    }

    /**
     * Returns the animation that the effect is associated with.
     * <p>
     * 효과가 연결된 애니메이션을 반환한다.
     *
     * @return the animation or null / 애니메이션 또는 null
     */
    public WebAnimation getAnimation() {
        return animation;
    }

    /**
     * Returns the target element.
     * <p>
     * 대상 요소를 반환한다.
     *
     * @return the element or null / 요소 또는 null
     */
    public Element getTarget() {
        return target;
    }

    /**
     * Returns the target pseudo-selector.
     * <p>
     * 대상 의사 선택자를 반환한다.
     *
     * @return <code>::before</code> and the like, or null / <code>::before</code> 등. 없으면 null
     */
    public String getPseudoElement() {
        return pseudo;
    }

    /**
     * Normalizes a pseudo-element selector for {@link #setTarget(Element, String)} (6.6 "pseudoElement"): the legacy single-colon forms of
     * <code>:before</code>, <code>:after</code>, <code>:first-letter</code> and <code>:first-line</code> get two colons; the selectors that this
     * engine animates are <code>::before</code>, <code>::after</code> and <code>::marker</code>.
     * <p>
     * {@link #setTarget(Element, String)} 를 위해 의사 요소 선택자를 정규화한다(6.6 "pseudoElement"): <code>:before</code>, <code>:after</code>,
     * <code>:first-letter</code>, <code>:first-line</code> 의 옛 한 콜론 형식은 두 콜론이 된다. 이 엔진이 애니메이션하는 선택자는 <code>::before</code>,
     * <code>::after</code>, <code>::marker</code> 이다.
     *
     * @param text the selector / 선택자
     * @return the normalized selector, or null if it is not a valid pseudo-element selector (a SyntaxError) / 정규화한 선택자. 올바른 의사 요소 선택자가 아니면(SyntaxError) null
     */
    public static String normalizePseudo(String text) {
        String t = text.toLowerCase(java.util.Locale.ENGLISH);
        if (":before".equals(t) || ":after".equals(t) || ":first-letter".equals(t) || ":first-line".equals(t)) {
            return ":" + t;
        }
        if ("::before".equals(t) || "::after".equals(t) || "::marker".equals(t)) {
            return t;
        }
        return null;
    }

    /**
     * Changes the effect target; the old and the new target are restyled.
     * <p>
     * 효과 대상을 바꾼다. 이전 대상과 새 대상의 스타일을 다시 계산한다.
     *
     * @param element the element or null / 요소 또는 null
     * @param pseudoSelector the normalized pseudo-selector or null / 정규화한 의사 선택자 또는 null
     */
    public void setTarget(Element element, String pseudoSelector) {
        Element old = target;
        target = element;
        pseudo = pseudoSelector;
        frames = null;
        version++;
        WebAnimation a = animation;
        if (a != null) {
            a.set.invalidate(old);
            a.set.changed(a);
        }
    }

    /**
     * Returns the composite operation of the effect.
     * <p>
     * 효과의 합성 연산을 반환한다.
     *
     * @return <code>replace</code>, <code>add</code> or <code>accumulate</code> / 연산
     */
    public String getComposite() {
        return composite;
    }

    /**
     * Sets the composite operation of the effect.
     * <p>
     * 효과의 합성 연산을 정한다.
     *
     * @param op <code>replace</code>, <code>add</code> or <code>accumulate</code> / 연산
     */
    public void setComposite(String op) {
        composite = op;
        changed();
    }

    /**
     * Returns the iteration composite operation (Web Animations Level 2; stored only).
     * <p>
     * 반복 합성 연산을 반환한다(Web Animations Level 2. 보관만 한다).
     *
     * @return <code>replace</code> or <code>accumulate</code> / 연산
     */
    public String getIterationComposite() {
        return iterationComposite;
    }

    /**
     * Sets the iteration composite operation (stored only, not applied).
     * <p>
     * 반복 합성 연산을 정한다(보관만 하고 적용하지 않는다).
     *
     * @param op <code>replace</code> or <code>accumulate</code> / 연산
     */
    public void setIterationComposite(String op) {
        iterationComposite = op;
    }

    /**
     * Tells the animation of the effect that the timing or the keyframes changed (the finished state and the style follow).
     * <p>
     * 효과의 애니메이션에 시간이나 키프레임이 바뀌었음을 알린다(끝남 상태와 스타일이 따라간다).
     */
    public void changed() {
        version++;
        frames = null;
        WebAnimation a = animation;
        if (a != null) {
            a.updateFinishedState(false, false);
            a.set.changed(a);
        }
    }

    /**
     * Returns the longhand property ids that the keyframes declare (the target properties, logical ones as declared).
     * <p>
     * 키프레임이 선언한 개별 속성 id(대상 속성, 논리 속성은 선언한 그대로)를 반환한다.
     *
     * @return the ids / id
     */
    public Set<Integer> getTargetProperties() {
        Set<Integer> out = new LinkedHashSet<Integer>();
        // 키프레임마다 선언한 속성을 모은다.
        for (Keyframe k : keyframes) {
            out.addAll(k.declared.keySet());
        }
        return out;
    }

    /**
     * Returns the custom properties that the keyframes declare.
     * <p>
     * 키프레임이 선언한 사용자 정의 속성을 반환한다.
     *
     * @return the names / 이름
     */
    public Set<String> getTargetCustomProperties() {
        Set<String> out = new LinkedHashSet<String>();
        // 키프레임마다 선언한 사용자 정의 속성을 모은다.
        for (Keyframe k : keyframes) {
            out.addAll(k.custom.keySet());
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ effect values

    /**
     * The computed keyframes: the values of the keyframes computed against the target, by physical property id.
     * <p>
     * 계산된 키프레임: 대상을 기준으로 계산한 키프레임의 값(물리 속성 id 별).
     */
    private static final class Frames {
        /** The version of the effect that they were computed for. 계산할 때의 효과 버전. */
        int version;
        /** The font size, the color and the writing mode of the style that they were computed against. 계산 기준 스타일의 글꼴 크기, 색, 쓰기 모드. */
        String key;
        /** The values per keyframe by physical property id. 키프레임별, 물리 속성 id 별 값. */
        final List<Map<Integer, CssValue>> values = new ArrayList<Map<Integer, CssValue>>();
        /** The custom values per keyframe by name (typed for registered properties, raw otherwise). 키프레임별, 이름별 사용자 정의 값(등록된 속성은 형 있는 값, 아니면 원문). */
        final List<Map<String, CssValue>> custom = new ArrayList<Map<String, CssValue>>();
    }

    /**
     * Returns the key that tells whether the computed keyframes are still valid for a style.
     * <p>
     * 계산된 키프레임이 스타일에 대해 아직 유효한지 알려 주는 키를 반환한다.
     *
     * @param s the style / 스타일
     * @return the key / 키
     */
    private static String frameKey(ComputedStyle s) {
        return s.fontSizePx() + "|" + s.rootFontSizePx() + "|" + s.get(Prop.COLOR) + "|" + s.keyword(Prop.WRITING_MODE) + "|" + s.keyword(Prop.DIRECTION);
    }

    /**
     * Computes (or returns the cached) computed keyframes for a target style.
     * <p>
     * 대상 스타일에 대한 계산 키프레임을 계산한다(또는 캐시한 것을 반환한다).
     *
     * @param resolver the style resolver / 스타일 리졸버
     * @param e        the target element / 대상 요소
     * @param own      the style of the target under the effect / 효과 아래의 대상 스타일
     * @return the computed keyframes / 계산 키프레임
     */
    private Frames frames(StyleResolver resolver, Element e, ComputedStyle own) {
        String key = frameKey(own);
        if (frames != null && frames.version == version && frames.key.equals(key)) {
            return frames;
        }
        Frames f = new Frames();
        f.version = version;
        f.key = key;
        String wm = own.keyword(Prop.WRITING_MODE) == null ? "horizontal-tb" : own.keyword(Prop.WRITING_MODE);
        String dr = own.keyword(Prop.DIRECTION) == null ? "ltr" : own.keyword(Prop.DIRECTION);
        // 키프레임마다 선언을 대상 기준으로 계산해 물리 속성 id 별 값으로 둔다.
        for (Keyframe k : keyframes) {
            Map<Integer, CssValue> vals = new LinkedHashMap<Integer, CssValue>();
            Map<Integer, CssValue> rest = new LinkedHashMap<Integer, CssValue>(k.declared);
            CssValue fontSize = rest.remove(Integer.valueOf(Prop.FONT_SIZE));
            // font-size 를 뺀 선언은 대상의 (애니메이션된) 글꼴 크기를 기준으로 계산한다(5.3.2 의 순서 의존: em 은 효과 값의 글꼴 크기를 따른다).
            if (!rest.isEmpty()) {
                ComputedStyle c = resolver.computeDeclared(e, rest, own);
                // 선언한 속성마다(논리 속성은 물리 속성으로) 계산 값을 꺼낸다.
                for (Integer id : rest.keySet()) {
                    int phys = LogicalProps.physical(id.intValue(), wm, dr);
                    vals.put(Integer.valueOf(phys), c.get(phys));
                }
            }
            // font-size 자신은 부모의 글꼴 크기를 기준으로 따로 계산한다.
            if (fontSize != null) {
                Map<Integer, CssValue> one = new LinkedHashMap<Integer, CssValue>();
                one.put(Integer.valueOf(Prop.FONT_SIZE), fontSize);
                vals.put(Integer.valueOf(Prop.FONT_SIZE), resolver.computeDeclared(e, one, own).get(Prop.FONT_SIZE));
            }
            f.values.add(vals);
            Map<String, CssValue> cv = new LinkedHashMap<String, CssValue>();
            // 사용자 정의 속성은 등록되어 있으면 형 있는 값으로, 아니면 원문으로 둔다.
            for (Map.Entry<String, List<ComponentValue>> c : k.custom.entrySet()) {
                CssValue typed = resolver.computeRegisteredValue(c.getKey(), c.getValue(), own);
                cv.put(c.getKey(), typed != null ? typed : new CssRaw(c.getValue()));
            }
            f.custom.add(cv);
        }
        frames = f;
        return f;
    }

    /**
     * Applies the effect values at an iteration progress on a style (5.3.4 and 5.4.3): for every target property the value of the keyframes is
     * combined with the value of the style, which is the underlying value, and replaces it.
     * <p>
     * 반복 진행도에서의 효과 값을 스타일에 적용한다(5.3.4 와 5.4.3): 대상 속성마다 키프레임의 값을 스타일의 값(기저 값)과 결합해 그것을 바꾼다.
     *
     * @param resolver the style resolver / 스타일 리졸버
     * @param e        the target element / 대상 요소
     * @param style    the underlying style / 기저 스타일
     * @param progress the iteration progress / 반복 진행도
     * @return the style with the effect values / 효과 값을 가진 스타일
     */
    public ComputedStyle apply(StyleResolver resolver, Element e, ComputedStyle style, double progress) {
        if (keyframes.isEmpty() || Double.isNaN(progress)) {
            return style;
        }
        Frames f = frames(resolver, e, style);
        ComputedStyle result = style;
        Integer fs = Integer.valueOf(Prop.FONT_SIZE);
        // font-size 를 먼저 적용하고, 다른 속성은 그 글꼴 크기로 다시 계산한 키프레임에서 구한다.
        if (hasValue(f, fs)) {
            CssValue v = effectValue(Prop.FONT_SIZE, f.values, progress, style.get(Prop.FONT_SIZE));
            if (v != null) {
                result = result.with(Prop.FONT_SIZE, v);
                f = frames(resolver, e, result);
            }
        }
        Set<Integer> ids = new LinkedHashSet<Integer>();
        // 키프레임에 나오는 물리 속성을 모은다.
        for (Map<Integer, CssValue> m : f.values) {
            ids.addAll(m.keySet());
        }
        ids.remove(fs);
        // 속성마다 효과 값을 계산해 적용한다.
        for (Integer id : ids) {
            CssValue v = effectValue(id.intValue(), f.values, progress, style.get(id.intValue()));
            if (v != null) {
                result = result.with(id.intValue(), v);
            }
        }
        return result;
    }

    /**
     * Tells whether a keyframe of the computed keyframes has a value for a property.
     * <p>
     * 계산 키프레임 중 속성의 값을 가진 키프레임이 있는지 알려 준다.
     *
     * @param f  the computed keyframes / 계산 키프레임
     * @param id the property id / 속성 id
     * @return true if one has / 있으면 true
     */
    private static boolean hasValue(Frames f, Integer id) {
        // 키프레임마다 속성이 있는지 본다.
        for (Map<Integer, CssValue> m : f.values) {
            if (m.containsKey(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Collects the effect values of the custom properties at an iteration progress (as CSS text, for a restyle with them).
     * <p>
     * 반복 진행도에서 사용자 정의 속성의 효과 값을 모은다(그것으로 스타일을 다시 계산하도록 CSS 문자열로).
     *
     * @param resolver the style resolver / 스타일 리졸버
     * @param e        the target element / 대상 요소
     * @param style    the underlying style / 기저 스타일
     * @param progress the iteration progress / 반복 진행도
     * @param out      receives the values by name / 이름별 값을 받는다
     */
    public void collectCustom(StyleResolver resolver, Element e, ComputedStyle style, double progress, Map<String, String> out) {
        if (keyframes.isEmpty() || Double.isNaN(progress) || getTargetCustomProperties().isEmpty()) {
            return;
        }
        Frames f = frames(resolver, e, style);
        // 사용자 정의 속성마다 효과 값을 계산한다.
        for (String name : getTargetCustomProperties()) {
            CssValue under = resolver.typedCustomValue(style, name);
            if (under == null) {
                under = style.getCustomProperty(name);
            }
            CssValue v = customEffectValue(name, f.custom, progress, under);
            if (v != null) {
                out.put(name, v.toCssText());
            }
        }
    }

    /**
     * Computes the effect value of a custom property.
     * <p>
     * 사용자 정의 속성의 효과 값을 계산한다.
     *
     * @param name       the property name / 속성 이름
     * @param custom     the custom values per keyframe / 키프레임별 사용자 정의 값
     * @param progress   the iteration progress / 반복 진행도
     * @param underlying the underlying value or null / 기저 값 또는 null
     * @return the value or null / 값 또는 null
     */
    private CssValue customEffectValue(String name, List<Map<String, CssValue>> custom, double progress, CssValue underlying) {
        List<Double> offs = new ArrayList<Double>();
        List<CssValue> vals = new ArrayList<CssValue>();
        List<Keyframe> kfs = new ArrayList<Keyframe>();
        // 이 속성의 값을 가진 키프레임만 고른다.
        for (int i = 0; i < keyframes.size(); i++) {
            CssValue v = custom.get(i).get(name);
            if (v != null) {
                offs.add(Double.valueOf(keyframes.get(i).computedOffset));
                vals.add(v);
                kfs.add(keyframes.get(i));
            }
        }
        return interval(0, offs, vals, kfs, progress, underlying);
    }

    /**
     * Computes the effect value of a property (5.3.4).
     * <p>
     * 속성의 효과 값을 계산한다(5.3.4).
     *
     * @param id         the physical property id / 물리 속성 id
     * @param values     the computed values per keyframe / 키프레임별 계산 값
     * @param progress   the iteration progress / 반복 진행도
     * @param underlying the underlying value / 기저 값
     * @return the value or null / 값 또는 null
     */
    private CssValue effectValue(int id, List<Map<Integer, CssValue>> values, double progress, CssValue underlying) {
        List<Double> offs = new ArrayList<Double>();
        List<CssValue> vals = new ArrayList<CssValue>();
        List<Keyframe> kfs = new ArrayList<Keyframe>();
        // 이 속성의 값을 가진 키프레임만 고른다(속성별 키프레임).
        for (int i = 0; i < keyframes.size(); i++) {
            CssValue v = values.get(i).get(Integer.valueOf(id));
            if (v != null) {
                offs.add(Double.valueOf(keyframes.get(i).computedOffset));
                vals.add(v);
                kfs.add(keyframes.get(i));
            }
        }
        return interval(id, offs, vals, kfs, progress, underlying);
    }

    /**
     * Finds the interval of the property-specific keyframes for a progress and interpolates (the second half of 5.3.4).
     * <p>
     * 진행도에 대한 속성별 키프레임의 구간을 찾아 보간한다(5.3.4 의 뒷부분).
     *
     * @param id         the property id (0 for a custom property) / 속성 id(사용자 정의 속성이면 0)
     * @param offs       the computed offsets / 계산된 오프셋
     * @param vals       the values / 값
     * @param kfs        the keyframes / 키프레임
     * @param progress   the iteration progress / 반복 진행도
     * @param underlying the underlying value or null / 기저 값 또는 null
     * @return the value or null / 값 또는 null
     */
    private CssValue interval(int id, List<Double> offs, List<CssValue> vals, List<Keyframe> kfs, double progress, CssValue underlying) {
        if (offs.isEmpty()) {
            return null;
        }
        // 0 과 1 에 키프레임이 없으면 기저 값(중립 값에 add 를 한 결과)을 가진 키프레임을 더한다.
        if (offs.get(0).doubleValue() != 0) {
            offs.add(0, Double.valueOf(0));
            vals.add(0, underlying);
            kfs.add(0, null);
        }
        if (offs.get(offs.size() - 1).doubleValue() != 1) {
            offs.add(Double.valueOf(1));
            vals.add(underlying);
            kfs.add(null);
        }
        int zeros = 0;
        int ones = 0;
        // 오프셋 0 과 1 의 키프레임 수를 센다.
        for (Double o : offs) {
            if (o.doubleValue() == 0) {
                zeros++;
            }
            if (o.doubleValue() == 1) {
                ones++;
            }
        }
        int first;
        int second;
        if (progress < 0 && zeros > 1) {
            first = 0;
            second = -1;
        } else if (progress >= 1 && ones > 1) {
            first = offs.size() - 1;
            second = -1;
        } else {
            first = -1;
            // 진행도 이하이며 1 보다 작은 오프셋을 가진 마지막 키프레임을 찾는다.
            for (int i = 0; i < offs.size(); i++) {
                double o = offs.get(i).doubleValue();
                if (o <= progress && o < 1) {
                    first = i;
                }
            }
            if (first < 0) {
                // 없으면(진행도가 음수) 오프셋이 0 인 마지막 키프레임을 쓴다.
                for (int i = 0; i < offs.size(); i++) {
                    if (offs.get(i).doubleValue() == 0) {
                        first = i;
                    }
                }
            }
            second = first + 1 < offs.size() ? first + 1 : -1;
        }
        CssValue start = combined(id, vals.get(first), kfs.get(first), underlying);
        if (second < 0) {
            return start;
        }
        CssValue end = combined(id, vals.get(second), kfs.get(second), underlying);
        double lo = offs.get(first).doubleValue();
        double hi = offs.get(second).doubleValue();
        double distance = hi > lo ? (progress - lo) / (hi - lo) : 0;
        Easing ease = kfs.get(first) == null ? Easing.LINEAR : kfs.get(first).easing;
        double t = ease.at(distance);
        if (start == null || end == null) {
            return t < 0.5 ? start : end;
        }
        CssValue v = Interpolator.interpolate(id, start, end, t);
        if (v == null) {
            // 보간할 수 없는 값(이산)은 구간의 절반에서 바뀐다.
            return t < 0.5 ? start : end;
        }
        return v;
    }

    /**
     * Combines the value of a keyframe with the underlying value according to its composite operation (the implicit keyframes, null, have the
     * underlying value already).
     * <p>
     * 키프레임의 값을 합성 연산에 따라 기저 값과 결합한다(암묵 키프레임(null)은 이미 기저 값을 가진다).
     *
     * @param id         the property id / 속성 id
     * @param value      the value of the keyframe / 키프레임의 값
     * @param k          the keyframe or null / 키프레임 또는 null
     * @param underlying the underlying value / 기저 값
     * @return the value to interpolate / 보간할 값
     */
    private CssValue combined(int id, CssValue value, Keyframe k, CssValue underlying) {
        if (k == null) {
            return value;
        }
        String op = k.composite != null ? k.composite : composite;
        if ("replace".equals(op)) {
            return value;
        }
        CssValue c = Compositor.combine(id, underlying, value, op);
        return c != null ? c : value;
    }
}
