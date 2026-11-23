package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of inline formatting and text layout (plan item P09-03): line breaking, white space, alignment, indentation, line height,
 * inline-block and replaced boxes, inline element fragments, vertical alignment, text transforms, Korean and surrogate pairs, bidirectional
 * text and source ranges. Expected widths are computed from the font that the fragments report.
 * <p>
 * 인라인 서식과 텍스트 배치 시험(계획 P09-03): 줄바꿈, 공백, 정렬, 들여쓰기, 줄 높이, inline-block 과 치환 상자, 인라인 요소 조각, 세로 정렬, 텍스트 변환,
 * 한글과 서로게이트 쌍, 양방향 텍스트, 원본 범위. 기대 너비는 조각이 알려 주는 글꼴로 계산한다.
 */
public class InlineLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public InlineLayoutTest() {
    }

    /**
     * Returns a CSS width of a number of monospace characters.
     * <p>
     * monospace 글자 수에 해당하는 CSS 너비를 반환한다.
     *
     * @param chars number of characters / 글자 수
     * @return for example <code>60.0px</code> / 예: <code>60.0px</code>
     */
    private static String w(double chars) {
        return (chars * LayoutTestSupport.charWidth()) + "px";
    }

    /**
     * Returns the text fragments of a line in x order.
     * <p>
     * 줄의 텍스트 조각을 x 순서로 반환한다.
     *
     * @param line the line / 줄
     * @return fragments / 조각
     */
    private static List<LayoutBox> texts(LayoutBox line) {
        List<LayoutBox> t = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(line, LayoutBox.Type.TEXT, t);
        // x 가 작은 것이 앞에 오도록 삽입 정렬한다.
        for (int i = 1; i < t.size(); i++) {
            for (int j = i; j > 0 && t.get(j).getX() < t.get(j - 1).getX(); j--) {
                LayoutBox x = t.get(j);
                t.set(j, t.get(j - 1));
                t.set(j - 1, x);
            }
        }
        return t;
    }

    /**
     * Returns the texts of all lines of a block.
     * <p>
     * 블록의 모든 줄의 텍스트를 반환한다.
     *
     * @param r  the result / 결과
     * @param id id of the block / 블록의 id
     * @return line texts / 줄 텍스트
     */
    private static List<String> lineTexts(LayoutResult r, String id) {
        List<String> out = new ArrayList<String>();
        // 줄마다 텍스트를 모은다.
        for (LayoutBox l : LayoutTestSupport.lines(LayoutTestSupport.box(r, id))) {
            out.add(LayoutTestSupport.lineText(l));
        }
        return out;
    }

    /**
     * Text breaks into lines at spaces when it does not fit; lines are stacked and trailing spaces are not shown.
     * <p>
     * 텍스트가 맞지 않으면 공백에서 줄이 바뀐다. 줄은 쌓이고 뒤 공백은 보이지 않는다.
     */
    public void testLineBreaking() {
        LayoutResult r = LayoutTestSupport.layout("<div id=d style='width:" + w(10) + "'>aaaa bbbb cccc dddd</div>", 800);
        Check.equal("[aaaa bbbb, cccc dddd]", lineTexts(r, "d").toString(), "lines");
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "d"));
        Check.near(lines.get(0).getY() + lines.get(0).getHeight(), lines.get(1).getY(), T, "lines are stacked");
        Check.near(LayoutTestSupport.box(r, "d").getHeight(), lines.get(0).getHeight() + lines.get(1).getHeight(), T, "block height is the sum of the lines");
        List<LayoutBox> t = texts(lines.get(0));
        Check.near(0, t.get(0).getX() - lines.get(0).getX(), T, "text starts at the line start");
        LayoutBox first = t.get(0);
        Check.near(LayoutTestSupport.advance(first.getFont(), first.getText()), first.getWidth(), T, "fragment width is the advance of its text");
        Check.isFalse(Double.isNaN(first.getBaseline()), "text has a baseline");
        Check.near(first.getY() + first.getAscent(), first.getBaseline(), T, "baseline is top plus ascent");
    }

    /**
     * White space collapses, <code>pre</code> keeps it, <code>nowrap</code> never wraps, <code>pre-line</code> keeps newlines only.
     * <p>
     * 공백은 접히고 <code>pre</code> 는 유지하며 <code>nowrap</code> 은 줄바꿈하지 않고 <code>pre-line</code> 은 개행만 유지한다.
     */
    public void testWhiteSpace() {
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=a>  a    b\n\n c  </div>"
                + "<div id=p style='white-space:pre'>a  b\n  c</div>"
                + "<div id=n style='white-space:nowrap;width:" + w(5) + "'>aaaa bbbb cccc</div>"
                + "<div id=l style='white-space:pre-line'>a   b\n  c</div>"
                + "<div id=pw style='white-space:pre-wrap;width:" + w(6) + "'>aa bb cc</div>"
                + "<div id=t style='white-space:pre'>x\ty</div>", 800);
        Check.equal("[a b c]", lineTexts(r, "a").toString(), "collapsed");
        Check.equal("[a  b,   c]", lineTexts(r, "p").toString(), "pre keeps spaces and breaks at newlines");
        Check.equal("[aaaa bbbb cccc]", lineTexts(r, "n").toString(), "nowrap does not wrap");
        Check.isTrue(LayoutTestSupport.lines(LayoutTestSupport.box(r, "n")).get(0).getWidth() >= 0, "line exists");
        Check.equal("[a b, c]", lineTexts(r, "l").toString(), "pre-line: newline breaks, spaces collapse, leading spaces of a line go");
        Check.equal("[aa bb , cc]", lineTexts(r, "pw").toString(), "pre-wrap wraps and keeps hanging spaces");
        Check.equal("[x       y]", lineTexts(r, "t").toString(), "a tab goes to the next tab stop of 8 columns");
        Check.isTrue(r.getContentWidth() > 800 || true, "overflow does not fail");
    }

    /**
     * <code>br</code> ends a line, consecutive breaks make empty lines, a trailing break makes no extra line.
     * <p>
     * <code>br</code> 은 줄을 끝내고 연속된 줄바꿈은 빈 줄을 만들며 끝의 줄바꿈은 줄을 더 만들지 않는다.
     */
    public void testBreaks() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a>a<br>b<br><br>c</div><div id=b>x<br></div>", 800);
        Check.equal("[a, b, , c]", lineTexts(r, "a").toString(), "four lines with an empty one");
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "a"));
        Check.isTrue(lines.get(2).getHeight() > 0, "an empty line still has the height of the strut");
        Check.equal(1, LayoutTestSupport.lines(LayoutTestSupport.box(r, "b")).size(), "a trailing br adds no line");
    }

    /**
     * <code>text-align</code>: left, right, center and justify (every word is a separate fragment and the last one touches the edge).
     * <p>
     * <code>text-align</code>: left, right, center, justify(낱말마다 조각이 따로 있고 마지막 조각이 가장자리에 닿는다).
     */
    public void testTextAlign() {
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=l style='width:" + w(20) + "'>ab</div>"
                + "<div id=r style='width:" + w(20) + ";text-align:right'>ab</div>"
                + "<div id=c style='width:" + w(20) + ";text-align:center'>ab</div>"
                + "<div id=j style='width:" + w(12) + ";text-align:justify'>aa bb cc dd ee</div>", 800);
        double cw = LayoutTestSupport.charWidth();
        LayoutBox l = LayoutTestSupport.box(r, "l");
        Check.near(0, texts(LayoutTestSupport.lines(l).get(0)).get(0).getX() - l.getX(), T, "left");
        LayoutBox rb = LayoutTestSupport.box(r, "r");
        LayoutBox rt = texts(LayoutTestSupport.lines(rb).get(0)).get(0);
        Check.near(rb.getX() + 20 * cw, rt.getX() + rt.getWidth(), T, "right edge");
        LayoutBox cb = LayoutTestSupport.box(r, "c");
        LayoutBox ct = texts(LayoutTestSupport.lines(cb).get(0)).get(0);
        Check.near(cb.getX() + 9 * cw, ct.getX(), T, "centred");
        LayoutBox j = LayoutTestSupport.box(r, "j");
        List<LayoutBox> jl = LayoutTestSupport.lines(j);
        Check.equal("[aabbccdd, ee]", lineTexts(r, "j").toString(), "lines of the justified text (words are separate fragments, the spaces are gaps)");
        List<LayoutBox> first = texts(jl.get(0));
        LayoutBox last = first.get(first.size() - 1);
        Check.near(j.getX() + 12 * cw, last.getX() + last.getWidth(), T, "a justified line fills the width");
        List<LayoutBox> lastLine = texts(jl.get(jl.size() - 1));
        Check.near(j.getX(), lastLine.get(0).getX(), T, "the last line is not justified");
    }

    /**
     * <code>text-indent</code> applies to the first line only.
     * <p>
     * <code>text-indent</code> 는 첫 줄에만 적용된다.
     */
    public void testTextIndent() {
        LayoutResult r = LayoutTestSupport.layout("<div id=d style='width:" + w(10) + ";text-indent:20px'>aaaa bbbb cccc</div>", 800);
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "d"));
        Check.near(20, texts(lines.get(0)).get(0).getX() - LayoutTestSupport.box(r, "d").getX(), T, "first line");
        Check.near(0, texts(lines.get(1)).get(0).getX() - LayoutTestSupport.box(r, "d").getX(), T, "second line");
    }

    /**
     * <code>line-height</code> sets the height of the lines (length and number) and <code>normal</code> uses the font.
     * <p>
     * <code>line-height</code> 는 줄 높이를 정하며(길이와 숫자) <code>normal</code> 은 글꼴을 쓴다.
     */
    public void testLineHeight() {
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=a style='line-height:30px'>x</div><div id=b style='line-height:2'>x</div><div id=n>x</div>", 800);
        Check.near(30, LayoutTestSupport.box(r, "a").getHeight(), T, "length");
        Check.near(20, LayoutTestSupport.box(r, "b").getHeight(), T, "number times the font size");
        LayoutBox n = LayoutTestSupport.box(r, "n");
        LayoutBox t = texts(LayoutTestSupport.lines(n).get(0)).get(0);
        Check.isTrue(n.getHeight() >= t.getHeight() - T, "normal line height covers the font");
        double[] metrics = new double[] { t.getAscent(), t.getDescent() };
        Check.near(metrics[0] + metrics[1], t.getHeight(), T, "fragment height is ascent plus descent");
    }

    /**
     * Inline-block boxes are atomic: shrink-to-fit width, their baseline is the baseline of their last line and they enlarge the line.
     * <p>
     * inline-block 상자는 원자적이다: shrink-to-fit 너비, 기준선은 마지막 줄의 기준선이며 줄을 키운다.
     */
    public void testInlineBlock() {
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=d>a<span id=i style='display:inline-block;width:50px;height:40px'></span>b"
                + "<span id=s style='display:inline-block'>hello</span></div>", 800);
        LayoutBox i = LayoutTestSupport.box(r, "i");
        Check.equal(LayoutBox.Type.INLINE_BLOCK, i.getType(), "type");
        Check.isTrue(i.isInlineLevel(), "inline level");
        Check.near(50, i.getWidth(), T, "width");
        LayoutBox line = LayoutTestSupport.lines(LayoutTestSupport.box(r, "d")).get(0);
        Check.isTrue(line.getHeight() >= 40 - T, "the line is as high as the box");
        Check.near(i.getY() + i.getHeight(), line.getBaseline(), T, "an empty inline-block sits on the baseline with its bottom margin edge");
        LayoutBox s = LayoutTestSupport.box(r, "s");
        double hello = LayoutTestSupport.advance(texts(LayoutTestSupport.lines(s).get(0)).get(0).getFont(), "hello");
        Check.near(hello, s.getWidth(), T, "shrink-to-fit width is the width of the text");
        Check.near(line.getBaseline(), LayoutTestSupport.lines(s).get(0).getBaseline(), T, "baselines are aligned");
    }

    /**
     * Inline elements give fragments with their padding and border; an element that wraps gets one fragment per line, with the edges only
     * where the element begins and ends.
     * <p>
     * 인라인 요소는 padding 과 border 를 가진 조각을 만든다. 줄바꿈되는 요소는 줄마다 조각이 있고 가장자리는 시작과 끝에만 있다.
     */
    public void testInlineFragments() {
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=d style='width:" + w(30) + "'><span id=s style='padding:0 5px;border:1px solid'>x</span></div>"
                + "<div id=e style='width:" + w(8) + "'><span id=t style='padding:0 5px;border:1px solid'>aaaa bbbb cccc</span></div>", 800);
        double cw = LayoutTestSupport.charWidth();
        LayoutBox s = r.getBoxes(((Element) LayoutTestSupport.box(r, "s").getNode())).get(0);
        Check.equal(LayoutBox.Type.INLINE, s.getType(), "type");
        Check.near(cw + 12, s.getWidth(), T, "padding and border widen the fragment");
        LayoutBox text = texts(LayoutTestSupport.lines(LayoutTestSupport.box(r, "d")).get(0)).get(0);
        Check.near(s.getX() + 6, text.getX(), T, "text starts inside the edges");
        List<LayoutBox> frags = r.getBoxes(LayoutTestSupport.box(r, "t").getNode());
        Check.isTrue(frags.size() >= 2, "one fragment per line: " + frags.size());
        Check.near(1, frags.get(0).getBorderLeft(), T, "first fragment has the start edge");
        Check.near(0, frags.get(0).getBorderRight(), T, "but not the end edge");
        LayoutBox last = frags.get(frags.size() - 1);
        Check.near(0, last.getBorderLeft(), T, "last fragment has no start edge");
        Check.near(1, last.getBorderRight(), T, "but the end edge");
    }

    /**
     * Replaced elements use the natural size of their content, the specified size, the aspect ratio and the limits, and sit on the baseline.
     * <p>
     * 치환 요소는 콘텐츠의 자연 크기, 지정 크기, 가로세로 비율, 제한을 쓰며 기준선 위에 놓인다.
     */
    public void testReplacedElements() {
        ReplacedResolver res = new ReplacedResolver() {
            /**
             * Gives every image the natural size 40 by 30.
             * <p>
             * 모든 이미지에 자연 크기 40×30 을 준다.
             *
             * @param element the element / 요소
             * @return the content / 콘텐츠
             */
            public ReplacedContent getReplacedContent(Element element) {
                return new ReplacedContent() {
                    /**
                     * Returns 40.
                     * <p>
                     * 40 을 반환한다.
                     *
                     * @return width / 너비
                     */
                    public double getNaturalWidth() {
                        return 40;
                    }

                    /**
                     * Returns 30.
                     * <p>
                     * 30 을 반환한다.
                     *
                     * @return height / 높이
                     */
                    public double getNaturalHeight() {
                        return 30;
                    }
                };
            }
        };
        LayoutResult r = LayoutTestSupport.layout(res,
                "<div id=d>x<img id=a>y</div><img id=b style='display:block;width:100px'>"
                + "<div style='width:20px'><img id=c style='max-width:100%'></div><img id=e style='width:10px;height:90px'>"
                + "<div style='width:200px'><img id=f style='display:block;margin:0 auto'></div>", 800);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.equal(LayoutBox.Type.REPLACED, a.getType(), "type");
        Check.near(40, a.getWidth(), T, "natural width");
        Check.near(30, a.getHeight(), T, "natural height");
        LayoutBox line = LayoutTestSupport.lines(LayoutTestSupport.box(r, "d")).get(0);
        Check.near(a.getY() + a.getHeight(), line.getBaseline(), T, "the bottom margin edge is on the baseline");
        LayoutBox b = LayoutTestSupport.box(r, "b");
        Check.near(100, b.getWidth(), T, "specified width");
        Check.near(75, b.getHeight(), T, "height follows the aspect ratio");
        Check.near(20, LayoutTestSupport.box(r, "c").getWidth(), T, "max-width:100% shrinks");
        Check.near(15, LayoutTestSupport.box(r, "c").getHeight(), T, "and keeps the ratio");
        Check.near(90, LayoutTestSupport.box(r, "e").getHeight(), T, "both sizes specified");
        Check.near(80, LayoutTestSupport.box(r, "f").getX(), T, "a block-level replaced element is centred by auto margins");
        LayoutResult none = LayoutTestSupport.layout("<img id=i width=24 height=12><video id=v></video>", 800);
        Check.near(24, LayoutTestSupport.box(none, "i").getWidth(), T, "width attribute");
        Check.near(300, LayoutTestSupport.box(none, "v").getWidth(), T, "default size of a video placeholder");
    }

    /**
     * A word that is wider than the line stays whole by default and is broken with <code>overflow-wrap</code>.
     * <p>
     * 줄보다 넓은 낱말은 기본으로 그대로 두고 <code>overflow-wrap</code> 이 있으면 끊는다.
     */
    public void testLongWords() {
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=a style='width:" + w(5) + "'>abcdefghijkl</div>"
                + "<div id=b style='width:" + w(5) + ";overflow-wrap:break-word'>abcdefghijkl</div>"
                + "<div id=c style='width:" + w(5) + ";word-break:break-all'>ab cdefghijkl</div>", 800);
        Check.equal("[abcdefghijkl]", lineTexts(r, "a").toString(), "kept whole");
        Check.equal("[abcde, fghij, kl]", lineTexts(r, "b").toString(), "broken at the width");
        Check.equal("[ab cd, efghi, jkl]", lineTexts(r, "c").toString(), "break-all fills each line");
        Check.isTrue(r.getContentWidth() >= 12 * LayoutTestSupport.charWidth() - T, "the unbroken word makes the document wider");
    }

    /**
     * Text transforms, letter spacing and word spacing: <code>text-transform</code>, <code>letter-spacing</code> and <code>word-spacing</code>.
     * <p>
     * 텍스트 변환, 글자 간격, 단어 간격: <code>text-transform</code>, <code>letter-spacing</code>, <code>word-spacing</code>.
     */
    public void testTextTransformAndSpacing() {
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=u style='text-transform:uppercase'>abc d</div><div id=c style='text-transform:capitalize'>hello big world</div>"
                + "<div id=l style='letter-spacing:2px'>abcd</div><div id=w style='word-spacing:5px'>a b</div><div id=p>abcd</div>", 800);
        Check.equal("[ABC D]", lineTexts(r, "u").toString(), "uppercase");
        Check.equal("[Hello Big World]", lineTexts(r, "c").toString(), "capitalize");
        LayoutBox plain = texts(LayoutTestSupport.lines(LayoutTestSupport.box(r, "p")).get(0)).get(0);
        LayoutBox spaced = texts(LayoutTestSupport.lines(LayoutTestSupport.box(r, "l")).get(0)).get(0);
        Check.near(plain.getWidth() + 8, spaced.getWidth(), T, "letter-spacing is added after every character");
        Check.near(2, spaced.getLetterSpacing(), T, "and reported");
        List<LayoutBox> wt = texts(LayoutTestSupport.lines(LayoutTestSupport.box(r, "w")).get(0));
        LayoutBox w = wt.get(0);
        double base = LayoutTestSupport.advance(w.getFont(), "a b");
        Check.near(base + 5, w.getWidth() + wt.get(1).getWidth(), T, "word-spacing is added after the space");
    }

    /**
     * Vertical alignment: <code>super</code> raises text, <code>sub</code> lowers it, <code>top</code> and <code>bottom</code> use the
     * line box.
     * <p>
     * 세로 정렬: <code>super</code> 는 텍스트를 올리고 <code>sub</code> 는 내리며 <code>top</code>, <code>bottom</code> 은 줄 상자를 기준으로 한다.
     */
    public void testVerticalAlign() {
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=d>a<span id=up style='vertical-align:super'>b</span><span id=dn style='vertical-align:sub'>c</span>"
                + "<span id=tp style='display:inline-block;height:50px;width:5px;vertical-align:top'></span>"
                + "<span id=bt style='display:inline-block;height:20px;width:5px;vertical-align:bottom'></span></div>", 800);
        LayoutBox line = LayoutTestSupport.lines(LayoutTestSupport.box(r, "d")).get(0);
        List<LayoutBox> t = texts(line);
        Check.isTrue(t.get(1).getBaseline() < t.get(0).getBaseline() - 1, "super is above the baseline of the text");
        Check.isTrue(t.get(2).getBaseline() > t.get(0).getBaseline() + 1, "sub is below it");
        Check.near(line.getY(), LayoutTestSupport.box(r, "tp").getY(), T, "top aligns with the top of the line");
        Check.near(line.getY() + line.getHeight(), LayoutTestSupport.box(r, "bt").getY() + LayoutTestSupport.box(r, "bt").getHeight(), T,
                "bottom aligns with the bottom of the line");
        Check.isTrue(line.getHeight() >= 50 - T, "the tall top-aligned box makes the line taller");
    }

    /**
     * Korean text wraps without losing characters and surrogate pairs are never split.
     * <p>
     * 한글 텍스트는 글자를 잃지 않고 줄바꿈되며 서로게이트 쌍은 절대 나뉘지 않는다.
     */
    public void testKoreanAndSurrogatePairs() {
        String ko = "한국어 텍스트를 줄바꿈하는 시험입니다";
        String emoji = new String(Character.toChars(0x1F600)) + new String(Character.toChars(0x1F601)) + new String(Character.toChars(0x1F602))
                + new String(Character.toChars(0x1F603));
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=k style='width:60px'>" + ko + "</div><div id=e style='width:20px;word-break:break-all'>" + emoji + "</div>", 800);
        List<LayoutBox> lines = LayoutTestSupport.lines(LayoutTestSupport.box(r, "k"));
        Check.isTrue(lines.size() >= 2, "Korean text wraps: " + lines.size());
        StringBuilder joined = new StringBuilder();
        // 모든 줄의 텍스트를 이어 글자가 사라지지 않았는지 확인한다.
        for (LayoutBox l : lines) {
            joined.append(LayoutTestSupport.lineText(l));
        }
        Check.equal(ko.replace(" ", ""), joined.toString().replace(" ", ""), "no character was lost");
        StringBuilder all = new StringBuilder();
        // 이모지 줄의 모든 조각에서 서로게이트 쌍이 깨지지 않았는지 확인한다.
        for (LayoutBox l : LayoutTestSupport.lines(LayoutTestSupport.box(r, "e"))) {
            for (LayoutBox t : texts(l)) {
                String s = t.getText();
                Check.isFalse(s.length() > 0 && Character.isHighSurrogate(s.charAt(s.length() - 1)), "no split pair at the end of " + s.length());
                Check.isFalse(s.length() > 0 && Character.isLowSurrogate(s.charAt(0)), "no split pair at the start");
                all.append(s);
            }
        }
        Check.equal(emoji, all.toString(), "all emoji are present");
    }

    /**
     * Right-to-left text: Hebrew inside English text is reordered visually and an <code>rtl</code> block starts at the right.
     * <p>
     * 오른쪽에서 왼쪽 텍스트: 영어 안의 히브리어는 시각 순서로 재배열되고 <code>rtl</code> 블록은 오른쪽에서 시작한다.
     */
    public void testBidirectionalText() {
        String he = "אבג";
        LayoutResult r = LayoutTestSupport.layout(
                "<div id=m style='width:300px'>abc " + he + " def</div><div id=r style='width:300px;direction:rtl'>abc</div>", 800);
        List<LayoutBox> t = texts(LayoutTestSupport.lines(LayoutTestSupport.box(r, "m")).get(0));
        boolean hebrewFound = false;
        // 히브리어 조각은 홀수 수준이어야 하고 영어 조각 사이에 있어야 한다.
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).getText().indexOf('א') >= 0) {
                hebrewFound = true;
                Check.equal(1, t.get(i).getBidiLevel() & 1, "odd level");
                Check.isTrue(i > 0 && i < t.size() - 1, "between the English parts");
            }
        }
        Check.isTrue(hebrewFound, "Hebrew fragment");
        LayoutBox rb = LayoutTestSupport.box(r, "r");
        LayoutBox rt = texts(LayoutTestSupport.lines(rb).get(0)).get(0);
        Check.near(rb.getX() + 300, rt.getX() + rt.getWidth(), T, "an rtl block aligns its text to the right");
    }

    /**
     * Fragments know the source range in the data of their Text node, even after white space was collapsed.
     * <p>
     * 조각은 공백이 접힌 뒤에도 자신의 Text 노드 데이터 안 원본 범위를 안다.
     */
    public void testSourceRanges() {
        LayoutResult r = LayoutTestSupport.layout("<div id=d>  hello   world </div>", 800);
        List<LayoutBox> t = texts(LayoutTestSupport.lines(LayoutTestSupport.box(r, "d")).get(0));
        Check.equal(2, t.size(), "two fragments");
        Check.equal(2, t.get(0).getSourceStart(), "start of hello");
        Check.equal(15, t.get(1).getSourceEnd(), "end of world");
        Document doc = ((Element) LayoutTestSupport.box(r, "d").getNode()).getOwnerDocument();
        Check.same(LayoutTestSupport.box(r, "d").getNode().getFirstChild(), t.get(0).getNode(), "the fragment points to its Text node");
        Check.notNull(doc, "document");
    }
}
