package org.duckdns.hjow.swingwebpanel.css.anim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the effect values of script animations on the computed style (Web Animations Level 1 sections 5.3 and 5.4) through the real style
 * resolver and {@link AnimationEngine}: interpolation of the computed keyframe values, the implicit keyframes with the underlying value, explicit
 * offsets, relative lengths against the animated font size (the example of section 5.3.2), the composite operations <code>add</code> and
 * <code>accumulate</code>, discrete values, the fill and cancel, the composite order with CSS transitions and CSS animations, pseudo-element targets
 * and <code>commitStyles()</code>.
 * <p>
 * 실제 스타일 리졸버와 {@link AnimationEngine} 으로 계산 스타일에 대한 스크립트 애니메이션의 효과 값을 시험한다(Web Animations Level 1 5.3 절과 5.4 절): 계산 키프레임
 * 값의 보간, 기저 값을 가진 암묵 키프레임, 명시 오프셋, 애니메이션된 글꼴 크기를 기준으로 한 상대 길이(5.3.2 절의 예), 합성 연산 <code>add</code>,
 * <code>accumulate</code>, 이산 값, 채움과 취소, CSS 전환·CSS 애니메이션과의 합성 순서, 의사 요소 대상, <code>commitStyles()</code>.
 */
public class WebAnimationStyleTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public WebAnimationStyleTest() {
    }

    /**
     * A document with its style engine, its animation engine and a fake script host.
     * <p>
     * 스타일 엔진, 애니메이션 엔진, 가짜 스크립트 호스트를 가진 문서.
     */
    private static final class Page {
        /** The clock of the CSS animations in milliseconds. CSS 애니메이션의 시계(밀리초). */
        final long[] cssTime = new long[1];
        /** The document. 문서. */
        final Document doc;
        /** The style engine. 스타일 엔진. */
        final CssEngine css;
        /** The animation engine. 애니메이션 엔진. */
        final AnimationEngine engine;
        /** The script animations. 스크립트 애니메이션. */
        final WebAnimationSet set;
        /** The host. 호스트. */
        final FakeAnimationHost host = new FakeAnimationHost();

        /**
         * Creates the page.
         * <p>
         * 페이지를 만든다.
         *
         * @param style the style sheet / 스타일 시트
         * @param body  the body / 본문
         */
        Page(String style, String body) {
            doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>" + style + "</style></head><body>" + body + "</body></html>");
            css = new CssEngine(doc, null, new MediaEnvironment(400, 300));
            css.loadStyleSheets();
            engine = new AnimationEngine(css, new AnimationClock() {
                /**
                 * Returns the test clock.
                 * <p>
                 * 시험 시계를 반환한다.
                 *
                 * @return milliseconds / 밀리초
                 */
                public long nowMillis() {
                    return cssTime[0];
                }
            });
            set = engine.getWebAnimations();
            set.attach(host, host);
        }

        /**
         * Returns an element by id.
         * <p>
         * id 로 요소를 반환한다.
         *
         * @param id the id / id
         * @return the element / 요소
         */
        Element el(String id) {
            return doc.getElementById(id);
        }

        /**
         * Returns the computed value of a property as text.
         * <p>
         * 속성의 계산 값을 문자열로 반환한다.
         *
         * @param id   the element id / 요소 id
         * @param prop the property id / 속성 id
         * @return the text / 문자열
         */
        String value(String id, int prop) {
            CssValue v = css.getComputedStyle(el(id)).get(prop);
            return v == null ? "null" : v.toCssText();
        }

        /**
         * Returns the computed pixels of a property.
         * <p>
         * 속성의 계산 픽셀을 반환한다.
         *
         * @param id   the element id / 요소 id
         * @param prop the property id / 속성 id
         * @return pixels / 픽셀
         */
        double px(String id, int prop) {
            return css.getComputedStyle(el(id)).px(prop);
        }

        /**
         * Returns a computed number.
         * <p>
         * 계산된 숫자를 반환한다.
         *
         * @param id   the element id / 요소 id
         * @param prop the property id / 속성 id
         * @return the number / 숫자
         */
        double number(String id, int prop) {
            return css.getComputedStyle(el(id)).number(prop);
        }

        /**
         * Creates and plays an animation, then runs the first frame at time 1000 (so the start time is 1000).
         * <p>
         * 애니메이션을 만들어 재생하고 시각 1000 의 첫 프레임을 실행한다(시작 시간이 1000 이 된다).
         *
         * @param target    the target id / 대상 id
         * @param frames    the keyframes, each as alternating IDL names and values (<code>offset</code>, <code>composite</code> and <code>easing</code> too) / 키프레임. 각각 IDL 이름과 값을 번갈아(<code>offset</code>, <code>composite</code>, <code>easing</code> 포함)
         * @param duration  the duration / 지속 시간
         * @param fill      the fill / 채움
         * @param composite the composite operation of the effect / 효과의 합성 연산
         * @return the animation / 애니메이션
         */
        WebAnimation animate(String target, String[][] frames, double duration, String fill, String composite) {
            EffectTiming t = new EffectTiming();
            t.setDuration(duration);
            t.setFill(fill);
            KeyframeEffectModel fx = new KeyframeEffectModel(t);
            fx.setComposite(composite);
            List<KeyframeEffectModel.Keyframe> list = new ArrayList<KeyframeEffectModel.Keyframe>();
            // 키프레임마다 이름과 값의 쌍을 읽는다.
            for (String[] f : frames) {
                Map<String, String> pairs = new LinkedHashMap<String, String>();
                Double offset = null;
                String easing = "linear";
                String comp = null;
                // 이름과 값을 둘씩 읽어 특수 멤버와 속성으로 나눈다.
                for (int i = 0; i + 1 < f.length; i += 2) {
                    if ("offset".equals(f[i])) {
                        offset = Double.valueOf(f[i + 1]);
                    } else if ("easing".equals(f[i])) {
                        easing = f[i + 1];
                    } else if ("composite".equals(f[i])) {
                        comp = f[i + 1];
                    } else {
                        pairs.put(KeyframeEffectModel.animatableProperty(f[i]), f[i + 1]);
                    }
                }
                list.add(KeyframeEffectModel.newKeyframe(offset, easing, comp, pairs));
            }
            fx.setKeyframes(list);
            WebAnimation a = new WebAnimation(set);
            a.setTimeline(set.getTimeline());
            a.setEffect(fx);
            fx.setTarget(el(target), null);
            host.time = 1000;
            a.play();
            host.frame(set, 1000);
            return a;
        }

        /**
         * Runs a frame at a time.
         * <p>
         * 시각에 프레임을 실행한다.
         *
         * @param at the time / 시각
         */
        void at(double at) {
            host.frame(set, at);
        }
    }

    /**
     * Two keyframes interpolate their computed values: opacity at half way, lengths in <code>em</code> of the element.
     * <p>
     * 두 키프레임은 계산 값을 보간한다: 절반의 opacity, 요소의 <code>em</code> 길이.
     */
    public void testKeyframesInterpolate() {
        Page p = new Page("#d{font-size:10px}", "<div id=d></div>");
        p.animate("d", new String[][] { { "opacity", "0", "marginLeft", "1em" }, { "opacity", "1", "marginLeft", "3em" } }, 1000, "auto", "replace");
        Check.near(0, p.number("d", Prop.OPACITY), 1e-6, "the first keyframe at the start");
        p.at(1500);
        Check.near(0.5, p.number("d", Prop.OPACITY), 1e-6, "half way");
        Check.near(20, p.px("d", Prop.MARGIN_LEFT), 1e-6, "1em to 3em of 10px");
        p.at(1750);
        Check.near(0.75, p.number("d", Prop.OPACITY), 1e-6, "three quarters");
        p.at(2500);
        Check.near(1, p.number("d", Prop.OPACITY), 1e-6, "no fill: the cascade value afterwards");
        Check.near(0, p.px("d", Prop.MARGIN_LEFT), 1e-6, "the cascade margin afterwards");
    }

    /**
     * A keyframe list without a keyframe at 0 or 1 uses the underlying value there, and explicit offsets place the keyframes.
     * <p>
     * 0 이나 1 에 키프레임이 없는 목록은 거기서 기저 값을 쓰고, 명시 오프셋은 키프레임의 위치를 정한다.
     */
    public void testImplicitKeyframesAndOffsets() {
        Page p = new Page("#d{margin-left:20px} #e{margin-left:20px}", "<div id=d></div><div id=e></div>");
        p.animate("d", new String[][] { { "marginLeft", "100px" } }, 1000, "auto", "replace");
        p.animate("e", new String[][] { { "marginLeft", "0px", "offset", "0" }, { "marginLeft", "100px", "offset", "0.5" } }, 1000, "auto", "replace");
        p.at(1500);
        Check.near(60, p.px("d", Prop.MARGIN_LEFT), 1e-6, "from the underlying 20px to 100px");
        Check.near(100, p.px("e", Prop.MARGIN_LEFT), 1e-6, "the keyframe at 0.5");
        p.at(1750);
        Check.near(60, p.px("e", Prop.MARGIN_LEFT), 1e-6, "from 100px at 0.5 back to the underlying 20px at 1");
    }

    /**
     * The example of section 5.3.2: the keyframes of <code>width</code> in <code>em</code> are computed against the animated font size, so at half
     * way the font size is 15px and the width 225px (not 250px).
     * <p>
     * 5.3.2 절의 예: <code>em</code> 단위의 <code>width</code> 키프레임은 애니메이션된 글꼴 크기를 기준으로 계산되므로 절반에서 글꼴 크기는 15px, 폭은 225px(250px 가
     * 아님)이다.
     */
    public void testLengthsFollowTheAnimatedFontSize() {
        Page p = new Page("", "<div id=d></div>");
        p.animate("d", new String[][] { { "fontSize", "10px", "width", "10em" }, { "fontSize", "20px", "width", "20em" } }, 1000, "auto", "replace");
        p.at(1500);
        Check.near(15, p.px("d", Prop.FONT_SIZE), 1e-6, "font size");
        Check.near(225, p.px("d", Prop.WIDTH), 1e-6, "width against the animated font size");
    }

    /**
     * The composite operations: <code>add</code> adds the keyframe values to the underlying value, a keyframe-specific composite overrides the one of
     * the effect, <code>accumulate</code> of matching transform functions adds their arguments and <code>add</code> appends the functions.
     * <p>
     * 합성 연산: <code>add</code> 는 키프레임 값을 기저 값에 더하고, 키프레임별 합성은 효과의 것보다 우선하며, 같은 transform 함수의 <code>accumulate</code> 는 인수를
     * 더하고 <code>add</code> 는 함수를 붙인다.
     */
    public void testCompositeOperations() {
        Page p = new Page("#d,#e{margin-left:10px} #t,#u{transform:translateX(10px)}", "<div id=d></div><div id=e></div><div id=t></div><div id=u></div>");
        p.animate("d", new String[][] { { "marginLeft", "0px" }, { "marginLeft", "100px" } }, 1000, "auto", "add");
        p.animate("e", new String[][] { { "marginLeft", "0px", "composite", "replace" }, { "marginLeft", "100px", "composite", "add" } }, 1000, "auto", "add");
        p.animate("t", new String[][] { { "transform", "translateX(20px)" }, { "transform", "translateX(20px)" } }, 1000, "auto", "accumulate");
        p.animate("u", new String[][] { { "transform", "rotate(0deg)" }, { "transform", "rotate(0deg)" } }, 1000, "auto", "add");
        p.at(1500);
        Check.near(60, p.px("d", Prop.MARGIN_LEFT), 1e-6, "10px + (0..100px at half)");
        Check.near(55, p.px("e", Prop.MARGIN_LEFT), 1e-6, "0px replaced to 110px added: 55px");
        Check.equal("translatex(30px)", p.value("t", Prop.TRANSFORM).toLowerCase(), "accumulated arguments");
        Check.contains(p.value("u", Prop.TRANSFORM).toLowerCase(), "translatex(10px) rotate(", "appended functions");
    }

    /**
     * Custom properties in keyframes: a registered property of a typed syntax is interpolated and the property that uses it with <code>var()</code>
     * follows; an unregistered one changes half way.
     * <p>
     * 키프레임의 사용자 정의 속성: 형 있는 문법으로 등록된 속성은 보간되고 그것을 <code>var()</code> 로 쓰는 속성이 따라가며, 등록되지 않은 것은 절반에서 바뀐다.
     */
    public void testCustomProperties() {
        Page p = new Page("@property --w { syntax: '<length>'; inherits: true; initial-value: 100px } #d{--w:100px;width:var(--w)} #e{--k:a}",
                "<div id=d></div><div id=e></div>");
        p.animate("d", new String[][] { { "--w", "100px" }, { "--w", "200px" } }, 1000, "auto", "replace");
        p.animate("e", new String[][] { { "--k", "x" }, { "--k", "y" } }, 1000, "auto", "replace");
        p.at(1400);
        Check.equal("x", p.css.getComputedStyle(p.el("e")).getCustomProperty("--k").toCssText().trim(), "before half way");
        p.at(1500);
        Check.near(150, p.px("d", Prop.WIDTH), 1e-6, "the width that uses the registered property follows its interpolation");
        p.at(1600);
        Check.equal("y", p.css.getComputedStyle(p.el("e")).getCustomProperty("--k").toCssText().trim(), "after half way");
    }

    /**
     * A value that cannot be interpolated changes half way through the interval.
     * <p>
     * 보간할 수 없는 값은 구간의 절반에서 바뀐다.
     */
    public void testDiscreteValues() {
        Page p = new Page("", "<div id=d></div>");
        p.animate("d", new String[][] { { "textAlign", "left" }, { "textAlign", "right" } }, 1000, "auto", "replace");
        p.at(1400);
        Check.equal("left", p.value("d", Prop.TEXT_ALIGN), "before half way");
        p.at(1600);
        Check.equal("right", p.value("d", Prop.TEXT_ALIGN), "after half way");
    }

    /**
     * Fill forwards keeps the last value after the end, pausing keeps the value of the moment, and canceling takes the effect away.
     * <p>
     * forwards 채움은 끝 뒤에도 마지막 값을 유지하고, 일시정지는 그 순간의 값을 유지하며, 취소하면 효과가 사라진다.
     */
    public void testFillPauseAndCancel() {
        Page p = new Page("#d{opacity:0.9}", "<div id=d></div>");
        WebAnimation a = p.animate("d", new String[][] { { "opacity", "0" }, { "opacity", "0.4" } }, 1000, "forwards", "replace");
        p.at(3000);
        Check.near(0.4, p.number("d", Prop.OPACITY), 1e-6, "the fill keeps the last keyframe");
        a.setCurrentTime(500);
        Check.near(0.2, p.number("d", Prop.OPACITY), 1e-6, "seeking changes the style at once (no frame needed)");
        a.pause();
        p.at(3100);
        Check.near(0.24, p.number("d", Prop.OPACITY), 1e-6, "the pause took the time of the next frame");
        p.at(4000);
        Check.near(0.24, p.number("d", Prop.OPACITY), 1e-6, "paused");
        a.cancel();
        Check.near(0.9, p.number("d", Prop.OPACITY), 1e-6, "cancel takes the effect away at once");
    }

    /**
     * The composite order of the effect stack (Web Animations 5.4.2, CSS Transitions 2): a script animation is above a CSS animation, which is above
     * a CSS transition; canceling the script animation shows the CSS animation again.
     * <p>
     * 효과 스택의 합성 순서(Web Animations 5.4.2, CSS Transitions 2): 스크립트 애니메이션은 CSS 애니메이션 위에, CSS 애니메이션은 CSS 전환 위에 있다. 스크립트
     * 애니메이션을 취소하면 CSS 애니메이션이 다시 보인다.
     */
    public void testCompositeOrderWithCss() {
        Page p = new Page("@keyframes k{from{opacity:0.2}to{opacity:0.2}} #d{animation:k 100s} #e{opacity:1;transition:opacity 100s linear}"
                + " #e.x{opacity:0} #e.y{animation:k 100s}", "<div id=d></div><div id=e></div>");
        Check.near(0.2, p.number("d", Prop.OPACITY), 1e-6, "the CSS animation");
        WebAnimation a = p.animate("d", new String[][] { { "opacity", "0.7" }, { "opacity", "0.7" } }, 100000, "auto", "replace");
        Check.near(0.7, p.number("d", Prop.OPACITY), 1e-6, "the script animation is on top");
        a.cancel();
        Check.near(0.2, p.number("d", Prop.OPACITY), 1e-6, "the CSS animation again");
        Check.near(1, p.number("e", Prop.OPACITY), 1e-6, "before the transition");
        p.el("e").setAttribute("class", "x");
        Check.near(1, p.number("e", Prop.OPACITY), 1e-6, "the transition starts");
        p.cssTime[0] = 50000;
        p.engine.tick();
        Check.near(0.5, p.number("e", Prop.OPACITY), 1e-6, "the transition half way");
        p.el("e").setAttribute("class", "x y");
        Check.near(0.2, p.number("e", Prop.OPACITY), 1e-6, "a CSS animation is above the running transition");
    }

    /**
     * A pseudo-element target animates the style of the pseudo-element only.
     * <p>
     * 의사 요소 대상은 의사 요소의 스타일만 애니메이션한다.
     */
    public void testPseudoElementTarget() {
        Page p = new Page("#d::before{content:'x';opacity:1}", "<div id=d></div>");
        EffectTiming t = new EffectTiming();
        t.setDuration(1000);
        KeyframeEffectModel fx = new KeyframeEffectModel(t);
        Map<String, String> pairs = new LinkedHashMap<String, String>();
        pairs.put("opacity", "0");
        List<KeyframeEffectModel.Keyframe> list = new ArrayList<KeyframeEffectModel.Keyframe>();
        list.add(KeyframeEffectModel.newKeyframe(null, "linear", null, pairs));
        list.add(KeyframeEffectModel.newKeyframe(null, "linear", null, pairs));
        fx.setKeyframes(list);
        fx.setTarget(p.el("d"), "::before");
        WebAnimation a = new WebAnimation(p.set);
        a.setTimeline(p.set.getTimeline());
        a.setEffect(fx);
        a.play();
        p.at(1000);
        p.at(1200);
        Check.near(0, p.css.getResolver().getPseudoStyle(p.el("d"), "before").number(Prop.OPACITY), 1e-6, "the pseudo-element is animated");
        Check.near(1, p.number("d", Prop.OPACITY), 1e-6, "the element is not");
        AnimationException x = null;
        try {
            p.set.commitStyles(a);
        } catch (AnimationException e) {
            x = e;
        }
        Check.notNull(x, "commitStyles on a pseudo-element");
        Check.equal(AnimationException.NO_MODIFICATION, x.getKind(), "NoModificationAllowedError");
    }

    /**
     * <code>commitStyles()</code> writes the animated values into the <code>style</code> attribute, so the value stays after <code>cancel()</code>;
     * a target that is not rendered gives an InvalidStateError.
     * <p>
     * <code>commitStyles()</code> 는 애니메이션 값을 <code>style</code> 속성에 쓰므로 <code>cancel()</code> 뒤에도 값이 남는다. 렌더링되지 않는 대상은
     * InvalidStateError 를 준다.
     */
    public void testCommitStyles() {
        Page p = new Page("#h{display:none}", "<div id=d style='color:red'></div><div id=h></div>");
        WebAnimation a = p.animate("d", new String[][] { { "opacity", "0" }, { "opacity", "0.3", "marginTop", "7px" } }, 1000, "forwards", "replace");
        p.at(2500);
        p.set.commitStyles(a);
        String style = p.el("d").getAttribute("style");
        Check.contains(style, "opacity: 0.3", "opacity committed");
        Check.contains(style, "margin-top: 7px", "margin-top committed");
        Check.contains(style, "color: red", "the other inline declarations stay");
        a.cancel();
        Check.near(0.3, p.number("d", Prop.OPACITY), 1e-6, "the committed value stays after cancel");
        WebAnimation h = p.animate("h", new String[][] { { "opacity", "0" } }, 1000, "forwards", "replace");
        AnimationException x = null;
        try {
            p.set.commitStyles(h);
        } catch (AnimationException e) {
            x = e;
        }
        Check.notNull(x, "not rendered");
        Check.equal(AnimationException.INVALID_STATE, x.getKind(), "InvalidStateError");
    }
}
