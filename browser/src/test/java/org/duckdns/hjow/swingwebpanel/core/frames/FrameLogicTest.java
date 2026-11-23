package org.duckdns.hjow.swingwebpanel.core.frames;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.css.cascade.FramesetTracks;
import org.duckdns.hjow.swingwebpanel.render.FrameScroll;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The pure rules of nested browsing contexts, without a component: the address rules of <code>iframe</code> attributes, the sandbox tokens and
 * their accumulation, the target names, the track lists of a <code>frameset</code> and the scroll arithmetic of the frame scrollbars (plan item
 * P20-02).
 * <p>
 * 컴포넌트 없이 확인하는 중첩 브라우징 문맥의 순수 규칙: <code>iframe</code> 속성의 주소 규칙, 샌드박스 토큰과 누적, 대상 이름, <code>frameset</code> 의
 * 트랙 목록, 프레임 스크롤바의 스크롤 계산(계획 P20-02).
 */
public class FrameLogicTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FrameLogicTest() {
    }

    /**
     * An empty, missing or unresolvable <code>src</code> is <code>about:blank</code>; others resolve against the base.
     * <p>
     * 비었거나 없거나 해석할 수 없는 <code>src</code> 는 <code>about:blank</code> 이고 나머지는 기준 URL 로 해석한다.
     */
    public void testSrcResolution() {
        Check.equal("about:blank", FrameUrls.resolveSrc("http://h/a/b.html", null), "no src");
        Check.equal("about:blank", FrameUrls.resolveSrc("http://h/a/b.html", "  "), "blank src");
        Check.equal("http://h/a/c.html", FrameUrls.resolveSrc("http://h/a/b.html", "c.html"), "relative src");
        Check.equal("http://x/y", FrameUrls.resolveSrc("http://h/a/b.html", " http://x/y "), "absolute src with spaces");
        Check.equal("about:blank", FrameUrls.resolveSrc("about:blank", "c.html"), "a base that cannot resolve");
    }

    /**
     * An ancestor that shows an address (ignoring the fragment) makes the navigation a loop; the special addresses never do.
     * <p>
     * 조상이 보여 주는 주소(프래그먼트 제외)로의 이동은 순환이고, 특수 주소는 순환이 아니다.
     */
    public void testLoopDetection() {
        java.util.List<String> ancestors = Arrays.asList("http://h/inner.html#x", "http://h/top.html");
        Check.isTrue(FrameUrls.isLoop("http://h/top.html", ancestors), "the top document is an ancestor");
        Check.isTrue(FrameUrls.isLoop("http://h/inner.html", ancestors), "fragments are ignored");
        Check.isFalse(FrameUrls.isLoop("http://h/other.html", ancestors), "another address");
        Check.isFalse(FrameUrls.isLoop("about:blank", Arrays.asList("about:blank")), "about:blank never loops");
        Check.isFalse(FrameUrls.isLoop("about:srcdoc", Arrays.asList("about:srcdoc")), "about:srcdoc never loops");
    }

    /**
     * Sandbox tokens are ASCII case-insensitive, unknown tokens are ignored, and the flags accumulate down the tree.
     * <p>
     * 샌드박스 토큰은 ASCII 대소문자를 구분하지 않고 모르는 토큰은 무시하며 플래그는 트리 아래로 누적된다.
     */
    public void testSandboxFlags() {
        Check.isFalse(SandboxFlags.parse(null).isSandboxed(), "no attribute");
        SandboxFlags empty = SandboxFlags.parse("");
        Check.isTrue(empty.isSandboxed(), "an empty attribute sandboxes");
        Check.isFalse(empty.allowsScripts() || empty.allowsSameOrigin() || empty.allowsForms() || empty.allowsTopNavigation() || empty.allowsPopups(),
                "nothing is allowed");
        SandboxFlags some = SandboxFlags.parse("  ALLOW-Scripts\tallow-forms bogus-token ");
        Check.isTrue(some.allowsScripts() && some.allowsForms(), "tokens are case-insensitive");
        Check.isFalse(some.allowsSameOrigin() || some.allowsTopNavigation() || some.allowsPopups(), "the other flags stay off");
        SandboxFlags child = SandboxFlags.parse("allow-scripts allow-same-origin allow-popups").inherit(some);
        Check.isTrue(child.allowsScripts(), "allowed by both");
        Check.isFalse(child.allowsSameOrigin(), "an ancestor forces a unique origin");
        Check.isFalse(child.allowsPopups(), "an ancestor forbids popups");
        Check.isTrue(SandboxFlags.parse(null).inherit(some).allowsForms(), "no attribute keeps what the parent allows");
        Check.isFalse(SandboxFlags.parse(null).inherit(empty).allowsScripts(), "a sandboxed parent restricts a frame without attribute");
        Check.isTrue(SandboxFlags.NONE.inherit(SandboxFlags.NONE).allowsSameOrigin(), "no sandbox anywhere");
    }

    /**
     * The keywords are ASCII case-insensitive and a valid context name does not start with an underscore.
     * <p>
     * 키워드는 ASCII 대소문자를 구분하지 않고 유효한 문맥 이름은 밑줄로 시작하지 않는다.
     */
    public void testTargetNames() {
        Check.equal(FrameTargets.SELF, FrameTargets.classify(null), "null");
        Check.equal(FrameTargets.SELF, FrameTargets.classify(""), "empty");
        Check.equal(FrameTargets.SELF, FrameTargets.classify("_SELF"), "_SELF");
        Check.equal(FrameTargets.PARENT, FrameTargets.classify("_Parent"), "_Parent");
        Check.equal(FrameTargets.TOP, FrameTargets.classify("_top"), "_top");
        Check.equal(FrameTargets.BLANK, FrameTargets.classify("_blank"), "_blank");
        Check.equal(FrameTargets.NAMED, FrameTargets.classify("main"), "a name");
        Check.equal(FrameTargets.NAMED, FrameTargets.classify("_other"), "an underscore name that is no keyword");
        Check.isTrue(FrameTargets.isValidName("main"), "valid name");
        Check.isFalse(FrameTargets.isValidName("_x"), "underscore");
        Check.isFalse(FrameTargets.isValidName(""), "empty");
    }

    /**
     * The lists of a frameset become grid track lists: pixels, percentages and relative lengths that share the rest.
     * <p>
     * frameset 의 목록은 그리드 트랙 목록이 된다: 픽셀, 백분율, 나머지를 나누는 상대 길이.
     */
    public void testFramesetTracks() {
        Check.equal("30% minmax(0,1fr) 100px", FramesetTracks.toTracks("30%, * ,100"), "mixed list");
        Check.equal("minmax(0,2fr) minmax(0,1fr)", FramesetTracks.toTracks("2*,*"), "relative lengths");
        Check.equal("minmax(0,1fr)", FramesetTracks.toTracks(null), "missing attribute");
        Check.equal("minmax(0,1fr)", FramesetTracks.toTracks("   "), "blank attribute");
        Check.equal("minmax(0,1fr) 50px", FramesetTracks.toTracks("junk,50px"), "an unreadable item fills");
        Check.equal("12.5%", FramesetTracks.toTracks("12.5%"), "fractional percentage");
        Check.equal(3, FramesetTracks.count("1,2,3"), "count");
        Check.equal(1, FramesetTracks.count(null), "count of nothing");
    }

    /**
     * The thumb of a scrollbar is proportional to the visible part, never shorter than the minimum, and maps back to its offset.
     * <p>
     * 스크롤바의 썸은 보이는 부분에 비례하고 최소 길이보다 짧지 않으며 오프셋으로 되돌아간다.
     */
    public void testScrollArithmetic() {
        Check.equal(0, FrameScroll.max(100, 200), "content smaller than the view");
        Check.equal(300, FrameScroll.max(500, 200), "max offset");
        Check.equal(300, FrameScroll.clamp(999, 500, 200), "clamped to the end");
        Check.equal(0, FrameScroll.clamp(-5, 500, 200), "clamped to the start");
        double[] top = FrameScroll.thumb(100, 1000, 0);
        Check.near(0, top[0], 0.001, "thumb at the start");
        Check.near(FrameScroll.MIN_THUMB, top[1], 0.001, "a short thumb is stretched to the minimum");
        double[] half = FrameScroll.thumb(100, 200, 50);
        Check.near(50, half[1], 0.001, "half the content is visible");
        Check.near(25, half[0], 0.001, "scrolled half way: the thumb is half way too");
        // 썸 위치에서 되돌린 오프셋은 썸을 구한 오프셋과 같다.
        double[] t = FrameScroll.thumb(100, 1000, 450);
        Check.near(450, FrameScroll.offsetForThumb(100, 1000, t[0]), 10, "the offset maps back");
        Check.equal(900, FrameScroll.offsetForThumb(100, 1000, 100), "the end of the track is the end of the content");
    }
}
