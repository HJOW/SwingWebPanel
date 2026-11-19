package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the strictness of <code>line-break</code> for Japanese text (CSS Text Module Level 3 section 5.3, https://www.w3.org/TR/css-text-3/#line-break-property:
 * <code>loose</code> allows a break before a small kana, <code>normal</code> and <code>loose</code> allow a break before the wave dash, <code>strict</code> does
 * not) and of <code>text-wrap: pretty</code> (CSS Text Module Level 4, https://www.w3.org/TR/css-text-4/#text-wrap-style: a short last line, an orphan, is
 * avoided by moving a word down from the line before it, without adding a line, while a paragraph whose last line is long enough is left alone).
 * <p>
 * 일본어 텍스트에 대한 <code>line-break</code> 엄격도 시험(CSS Text Module Level 3 5.3 절: <code>loose</code> 는 작은 가나 앞의 줄바꿈을 허용하고,
 * <code>normal</code> 과 <code>loose</code> 는 물결 대시 앞의 줄바꿈을 허용하며, <code>strict</code> 는 허용하지 않는다)와 <code>text-wrap: pretty</code> 시험(CSS Text Module Level 4:
 * 짧은 마지막 줄(고아)은 줄을 늘리지 않고 앞줄의 단어를 내려서 피하며, 마지막 줄이 충분히 긴 문단은 그대로 둔다).
 */
public class LineBreakStrictnessLayoutTest {

    /** The first five hiragana: a i u e o. 처음 다섯 히라가나. */
    private static final String FIVE = "あいうえお";

    /** The small ya. 작은 야. */
    private static final String SMALL_YA = "ゃ";

    /** The rest of the text: ka ki ku ke. 나머지 텍스트. */
    private static final String REST = "かきくけ";

    /** The wave dash. 물결 대시. */
    private static final String WAVE_DASH = "〜";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LineBreakStrictnessLayoutTest() {
    }

    /**
     * Measures the width of a hiragana character in the test font.
     * <p>
     * 시험 글꼴에서 히라가나 한 글자의 너비를 잰다.
     *
     * @return the width in px / 너비(px)
     */
    private static double kanaWidth() {
        LayoutResult r = LayoutTestSupport.layout("<p id=p style='margin:0'>" + FIVE + "</p>", 600);
        List<LayoutBox> l = LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"));
        double w = 0;
        // 첫 줄의 텍스트 상자 너비를 더한다.
        for (LayoutBox c : l.get(0).getChildren()) {
            w += c.getWidth();
        }
        return w / 5;
    }

    /**
     * Lays out a paragraph in a container that holds five and a half kana.
     * <p>
     * 가나 다섯 글자 반을 담는 컨테이너에 문단을 배치한다.
     *
     * @param style the style of the paragraph / 문단의 스타일
     * @param text  the text / 텍스트
     * @return the lines / 줄
     */
    private static List<LayoutBox> kanaLines(String style, String text) {
        double w = kanaWidth() * 5.5;
        LayoutResult r = LayoutTestSupport.layout("<div style='width:" + w + "px'><p id=p style='margin:0;" + style + "'>" + text + "</p></div>", 600);
        return LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"));
    }

    /**
     * <code>normal</code> (and <code>auto</code>) and <code>strict</code> do not break before a small kana, <code>loose</code> does.
     * <p>
     * <code>normal</code>(과 <code>auto</code>), <code>strict</code> 는 작은 가나 앞에서 줄을 바꾸지 않고 <code>loose</code> 는 바꾼다.
     */
    public void testSmallKana() {
        String text = FIVE + SMALL_YA + REST;
        List<LayoutBox> normal = kanaLines("line-break:normal", text);
        Check.equal(FIVE.substring(0, 4), LayoutTestSupport.lineText(normal.get(0)), "normal: the line ends before the letter that the small kana follows");
        Check.isTrue(LayoutTestSupport.lineText(normal.get(1)).startsWith(FIVE.substring(4) + SMALL_YA), "normal: the small kana stays with its letter");
        List<LayoutBox> auto = kanaLines("", text);
        Check.equal(FIVE.substring(0, 4), LayoutTestSupport.lineText(auto.get(0)), "auto is like normal");
        List<LayoutBox> strict = kanaLines("line-break:strict", text);
        Check.equal(FIVE.substring(0, 4), LayoutTestSupport.lineText(strict.get(0)), "strict does not break before it either");
        List<LayoutBox> loose = kanaLines("line-break:loose", text);
        Check.equal(FIVE, LayoutTestSupport.lineText(loose.get(0)), "loose fills the line");
        Check.isTrue(LayoutTestSupport.lineText(loose.get(1)).startsWith(SMALL_YA), "loose: the small kana starts the next line");
    }

    /**
     * The wave dash may start a line in <code>normal</code> and <code>loose</code> but not in <code>strict</code>; the prolonged sound mark is like a
     * small kana.
     * <p>
     * 물결 대시는 <code>normal</code> 과 <code>loose</code> 에서는 줄을 시작할 수 있고 <code>strict</code> 에서는 시작할 수 없다. 장음 부호는 작은 가나와 같다.
     */
    public void testWaveDashAndProlongedSoundMark() {
        String text = FIVE + WAVE_DASH + REST;
        Check.equal(FIVE, LayoutTestSupport.lineText(kanaLines("line-break:normal", text).get(0)), "normal: a break before the wave dash");
        Check.equal(FIVE, LayoutTestSupport.lineText(kanaLines("line-break:loose", text).get(0)), "loose: a break before the wave dash");
        Check.equal(FIVE.substring(0, 4), LayoutTestSupport.lineText(kanaLines("line-break:strict", text).get(0)), "strict: no break before the wave dash");
        String dash = FIVE + "ー" + REST;
        Check.equal(FIVE.substring(0, 4), LayoutTestSupport.lineText(kanaLines("line-break:normal", dash).get(0)), "normal: none before the prolonged sound mark");
        Check.equal(FIVE, LayoutTestSupport.lineText(kanaLines("line-break:loose", dash).get(0)), "loose: a break before the prolonged sound mark");
    }

    /**
     * An opening bracket never ends a line and a closing bracket never starts one, whatever the strictness.
     * <p>
     * 여는 괄호는 엄격도와 상관없이 줄을 끝내지 않고 닫는 괄호는 줄을 시작하지 않는다.
     */
    public void testBracketsAreKept() {
        String closing = FIVE + "」" + REST;
        Check.equal(FIVE.substring(0, 4), LayoutTestSupport.lineText(kanaLines("line-break:loose", closing).get(0)), "loose: no break before a closing bracket");
        String opening = FIVE.substring(0, 4) + "「" + FIVE.substring(4) + REST;
        String first = LayoutTestSupport.lineText(kanaLines("line-break:loose", opening).get(0));
        Check.isFalse(first.endsWith("「"), "loose: no break after an opening bracket");
    }

    /**
     * A last line of one short word is avoided: the second last line gives up a word, the line count stays; a long last line is left alone.
     * <p>
     * 짧은 낱말 하나뿐인 마지막 줄은 피한다: 끝에서 둘째 줄이 낱말 하나를 내주고 줄 수는 그대로이다. 긴 마지막 줄은 그대로 둔다.
     */
    public void testPrettyAvoidsAnOrphan() {
        double w = 14 * LayoutTestSupport.charWidth();
        String text = "aaaa bbbb cccc dddd eeee ffff gg";
        LayoutResult plain = LayoutTestSupport.layout("<div style='width:" + w + "px'><p id=p style='margin:0'>" + text + "</p></div>", 600);
        List<LayoutBox> p = LayoutTestSupport.lines(LayoutTestSupport.box(plain, "p"));
        Check.equal(3, p.size(), "normal: three lines");
        Check.equal("gg", LayoutTestSupport.lineText(p.get(2)), "normal: the last line is an orphan");
        LayoutResult pretty = LayoutTestSupport.layout("<div style='width:" + w + "px'><p id=p style='margin:0;text-wrap:pretty'>" + text + "</p></div>", 600);
        List<LayoutBox> q = LayoutTestSupport.lines(LayoutTestSupport.box(pretty, "p"));
        Check.equal(3, q.size(), "pretty: still three lines");
        Check.equal("aaaa bbbb cccc", LayoutTestSupport.lineText(q.get(0)), "pretty: the first line is full");
        Check.equal("dddd eeee", LayoutTestSupport.lineText(q.get(1)), "pretty: the second last line gives up a word");
        Check.equal("ffff gg", LayoutTestSupport.lineText(q.get(2)), "pretty: the last line has two words");
        String longLast = "aaaa bbbb cccc dddd eeee ffff gggg hhhh";
        LayoutResult same = LayoutTestSupport.layout("<div style='width:" + w + "px'><p id=p style='margin:0;text-wrap:pretty'>" + longLast + "</p></div>", 600);
        List<LayoutBox> r = LayoutTestSupport.lines(LayoutTestSupport.box(same, "p"));
        Check.equal("dddd eeee ffff", LayoutTestSupport.lineText(r.get(1)), "pretty: a long last line leaves the paragraph alone");
        Check.equal("gggg hhhh", LayoutTestSupport.lineText(r.get(2)), "pretty: and the last line is unchanged");
        LayoutResult stable = LayoutTestSupport.layout("<div style='width:" + w + "px'><p id=p style='margin:0;text-wrap:stable'>" + text + "</p></div>", 600);
        Check.equal("gg", LayoutTestSupport.lineText(LayoutTestSupport.lines(LayoutTestSupport.box(stable, "p")).get(2)), "stable breaks like normal text");
    }
}
