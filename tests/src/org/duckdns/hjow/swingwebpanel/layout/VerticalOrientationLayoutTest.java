package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the orientation of the characters in vertical text (plan item P10-03-e, CSS Writing Modes Level 3 sections 5.1 and 9.1 and
 * the <code>digits</code> value of Level 4): the classification of characters by <code>Vertical_Orientation</code> (UAX #50), the
 * runs of one vertical line (upright Hangul, Han and kana, sideways Latin letters and digits, upright CJK punctuation), the one-em cell
 * of every upright character cluster, <code>text-orientation: upright</code> and <code>sideways</code>, letter spacing, line breaks
 * between upright characters, <code>text-combine-upright: all</code> and <code>digits</code>, the horizontal text that is left alone.
 * The layout creates a text fragment for every break opportunity unit; the tests join neighbouring fragments of the same kind into
 * runs ({@link Run}). The expected advances are computed from the specification (an upright cell is the font size) and from the font
 * that a fragment reports.
 * <p>
 * 세로쓰기에서 글자 방향 시험(계획 P10-03-e, CSS Writing Modes Level 3 5.1 절과 9.1 절, Level 4 의 <code>digits</code> 값): <code>Vertical_Orientation</code>(UAX #50)에 따른 글자
 * 분류, 세로 줄 하나의 구간(똑바로 선 한글, 한자, 가나, 눕힌 라틴 글자와 숫자, 똑바로 선 CJK 문장 부호), 똑바로 선 글자 클러스터마다 1em 한 칸,
 * <code>text-orientation: upright</code> 와 <code>sideways</code>, 글자 간격, 똑바로 선 글자 사이의 줄바꿈, <code>text-combine-upright: all</code> 과 <code>digits</code>,
 * 그대로 남는 가로쓰기 텍스트. 레이아웃은 줄바꿈 기회 단위마다 텍스트 조각을 만들므로 시험은 이웃한 같은 종류의 조각을 구간({@link Run})으로 합쳐 본다.
 * 기대 advance 는 규격(똑바로 선 한 칸은 글꼴 크기)과 조각이 알려 주는 글꼴로 계산한다.
 */
public class VerticalOrientationLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /**
     * A run: neighbouring fragments of one line with the same glyph mode (combined groups stay separate).
     * <p>
     * 구간: 한 줄에서 글리프 방식이 같은 이웃한 조각(조합 묶음은 따로 둔다).
     */
    private static final class Run {
        /** The text of the fragments. 조각의 텍스트. */
        String text = "";
        /** The glyph mode. 글리프 방식. */
        int mode;
        /** X of the line column. 줄 열의 x. */
        double x;
        /** Y of the first fragment. 첫 조각의 y. */
        double y;
        /** Length along the line. 줄을 따라 길이. */
        double height;
        /** Width across the line. 줄을 가로지르는 너비. */
        double width;
        /** First fragment. 첫 조각. */
        LayoutBox first;
        /** Last fragment. 마지막 조각. */
        LayoutBox last;
    }

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public VerticalOrientationLayoutTest() {
    }

    /**
     * Returns the text fragments below the box of an id in document order.
     * <p>
     * id 의 상자 아래 텍스트 조각을 문서 순서로 반환한다.
     *
     * @param r  the layout / 레이아웃
     * @param id the id / id
     * @return the fragments / 조각
     */
    private static List<LayoutBox> texts(LayoutResult r, String id) {
        List<LayoutBox> out = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(LayoutTestSupport.box(r, id), LayoutBox.Type.TEXT, out);
        return out;
    }

    /**
     * Joins neighbouring fragments with the same glyph mode in the same column and without a gap into runs.
     * <p>
     * 같은 열에서 틈 없이 이웃하고 글리프 방식이 같은 조각을 구간으로 합친다.
     *
     * @param r  the layout / 레이아웃
     * @param id the id of the paragraph / 문단의 id
     * @return the runs in document order / 문서 순서의 구간
     */
    private static List<Run> runs(LayoutResult r, String id) {
        List<Run> out = new ArrayList<Run>();
        List<LayoutBox> t = texts(r, id);
        // 조각을 차례로 훑으며 앞 구간에 이을 수 있으면 잇고 아니면 새 구간을 시작한다.
        for (int i = 0; i < t.size(); i++) {
            LayoutBox b = t.get(i);
            Run last = out.isEmpty() ? null : out.get(out.size() - 1);
            boolean join = last != null && last.mode == b.getGlyphMode() && b.getGlyphMode() != LayoutBox.GLYPHS_COMBINED
                    && Math.abs(last.x - b.getX()) < T && Math.abs(last.y + last.height - b.getY()) < T;
            if (join) {
                last.text += b.getText();
                last.height += b.getHeight();
                last.last = b;
            } else {
                Run n = new Run();
                n.text = b.getText();
                n.mode = b.getGlyphMode();
                n.x = b.getX();
                n.y = b.getY();
                n.height = b.getHeight();
                n.width = b.getWidth();
                n.first = b;
                n.last = b;
                out.add(n);
            }
        }
        return out;
    }

    /**
     * Lays out one vertical-rl paragraph with a 20 pixel font and a 30 pixel line height.
     * <p>
     * 20 픽셀 글꼴과 30 픽셀 줄 높이의 vertical-rl 문단 하나를 배치한다.
     *
     * @param style extra style / 추가 스타일
     * @param html  the content of the paragraph / 문단의 내용
     * @return the layout; the paragraph has the id <code>v</code> / 레이아웃. 문단의 id 는 <code>v</code>
     */
    private static LayoutResult vertical(String style, String html) {
        return LayoutTestSupport.layout("<div id=v style='writing-mode:vertical-rl;font:20px monospace;line-height:30px;height:300px;" + style + "'>"
                + html + "</div>", 400);
    }

    /**
     * Checks the kinds of the characters of a text.
     * <p>
     * 텍스트 글자의 종류를 확인한다.
     *
     * @param text     the text / 텍스트
     * @param orient   orientation mode / 방향 모드
     * @param combine  combine mode / 조합 모드
     * @param expected the expected kinds as a string of S (sideways), U (upright), C (combined) per UTF-16 unit; empty for null / 기대하는 종류. UTF-16 단위마다 S, U, C. null 이면 빈 문자열
     */
    private static void kinds(String text, int orient, int combine, String expected) {
        byte[] k = VerticalText.classify(text, orient, combine);
        StringBuilder sb = new StringBuilder();
        // 종류 배열을 문자열로 바꾼다.
        for (int i = 0; k != null && i < k.length; i++) {
            sb.append(k[i] == VerticalText.UP ? 'U' : (k[i] == VerticalText.COMBO ? 'C' : 'S'));
        }
        Check.equal(expected, sb.toString(), "kinds of \"" + text + "\"");
    }

    /**
     * Characters from vertical scripts (Hangul, Han, kana, fullwidth forms, CJK punctuation) are upright in mixed orientation and
     * horizontal-only characters (Latin letters, digits, ASCII punctuation, spaces) are sideways (Vertical_Orientation U, Tu, Tr and R).
     * <p>
     * 혼합 방향에서 세로 문자 체계(한글, 한자, 가나, 전각 형태, CJK 문장 부호)의 글자는 똑바로 서고 가로 전용 글자(라틴 글자, 숫자, ASCII 문장 부호, 공백)는 눕는다
     * (Vertical_Orientation U, Tu, Tr 과 R).
     */
    public void testMixedClassification() {
        kinds("한국어", VerticalText.ORIENT_MIXED, 0, "UUU");
        kinds("漢字あア", VerticalText.ORIENT_MIXED, 0, "UUUU");
        kinds("A1 z", VerticalText.ORIENT_MIXED, 0, "");
        kinds("한A1 漢", VerticalText.ORIENT_MIXED, 0, "USSSU");
        // U+300C 와 U+3001 은 Tr 과 Tu, U+FF21 은 U 이다. 모두 똑바로 선다.
        kinds("「、Ａ", VerticalText.ORIENT_MIXED, 0, "UUU");
        // 보조 평면의 한자(서로게이트 쌍)는 두 UTF-16 단위 모두 똑바로 선다.
        kinds("a𠀀", VerticalText.ORIENT_MIXED, 0, "SUU");
        // 결합 부호는 앞 글자를 따른다.
        kinds("é", VerticalText.ORIENT_MIXED, 0, "");
        kinds("が", VerticalText.ORIENT_MIXED, 0, "UU");
        Check.isTrue(VerticalText.isUpright(0xAC00) && VerticalText.isUpright(0x4E00) && VerticalText.isUpright(0xE000), "Hangul, Han, PUA are upright");
        Check.isTrue(VerticalText.isUpright(0x2FFFD), "an unassigned code point of the ideographic plane defaults to U");
        Check.isFalse(VerticalText.isUpright('a') || VerticalText.isUpright('0') || VerticalText.isUpright(' '), "Latin, digit, space are R");
        Check.isTrue(VerticalText.isUpright(0xA7) && !VerticalText.isUpright(0xA8), "the first entries of the table: section sign U, diaeresis R");
    }

    /**
     * <code>upright</code> stands every character but white space upright, <code>sideways</code> none.
     * <p>
     * <code>upright</code> 는 공백을 뺀 모든 글자를 똑바로 세우고 <code>sideways</code> 는 하나도 세우지 않는다.
     */
    public void testUprightAndSidewaysClassification() {
        kinds("AB c", VerticalText.ORIENT_UPRIGHT, 0, "UUSU");
        kinds("é", VerticalText.ORIENT_UPRIGHT, 0, "UU");
        kinds("한AB", VerticalText.ORIENT_SIDEWAYS, 0, "");
        Check.equal(1, VerticalText.clusterCount("é", 0, 2), "a base character with its mark is one cluster");
        Check.equal(3, VerticalText.clusterCount("a𠀀b", 0, 4), "a surrogate pair is one cluster");
        int[] o = VerticalText.clusterOffsets("a𠀀é");
        Check.equal(4, o.length, "three offsets and the end");
        Check.equal(0, o[0], "cluster 0");
        Check.equal(1, o[1], "cluster 1 starts after a");
        Check.equal(3, o[2], "cluster 2 starts after the pair");
        Check.equal(5, o[3], "the length");
    }

    /**
     * <code>text-combine-upright: all</code> combines the whole text run; <code>digits n</code> combines every maximal run of at most n
     * ASCII digits (n is 2 to 4, 2 if omitted).
     * <p>
     * <code>text-combine-upright: all</code> 은 텍스트 구간 전체를 조합하고 <code>digits n</code> 은 n 자리 이하의 ASCII 숫자가 이어진 최대 구간을 조합한다
     * (n 은 2~4, 생략하면 2).
     */
    public void testCombineClassification() {
        kinds("12ab", VerticalText.ORIENT_MIXED, VerticalText.COMBINE_ALL, "CCCC");
        kinds("1 12 123 1234", VerticalText.ORIENT_MIXED, 2, "CSCCSSSSSSSSS");
        kinds("1 12 123 1234", VerticalText.ORIENT_MIXED, 4, "CSCCSCCCSCCCC");
        kinds("平20年", VerticalText.ORIENT_MIXED, 2, "UCCU");
    }

    /**
     * The properties are parsed: <code>text-combine-upright</code> (none, all, digits, digits n) and the orientation keywords.
     * <p>
     * 속성을 해석한다: <code>text-combine-upright</code>(none, all, digits, digits n)와 방향 키워드.
     */
    public void testPropertiesAreComputed() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='text-combine-upright:digits 3'>x</div><div id=b style='text-combine-upright:digits'>x</div>"
                + "<div id=c style='text-combine-upright:all'>x</div><div id=d>x</div><div id=e style='text-combine-upright:digits 9'>x</div>", 400);
        Check.equal(3, VerticalText.combineOf(LayoutTestSupport.box(r, "a").style), "digits 3");
        Check.equal(2, VerticalText.combineOf(LayoutTestSupport.box(r, "b").style), "digits alone is digits 2");
        Check.equal(VerticalText.COMBINE_ALL, VerticalText.combineOf(LayoutTestSupport.box(r, "c").style), "all");
        Check.equal(VerticalText.COMBINE_NONE, VerticalText.combineOf(LayoutTestSupport.box(r, "d").style), "none");
        Check.equal(VerticalText.COMBINE_NONE, VerticalText.combineOf(LayoutTestSupport.box(r, "e").style), "digits 9 is invalid and ignored");
    }

    /**
     * Mixed text in one vertical line: the Hangul run is upright and takes one em per character along the line, the Latin run is
     * sideways; both are in the same line one after the other, and a fragment is as wide (across the line) as the font is high.
     * <p>
     * 세로 줄 하나의 혼합 텍스트: 한글 구간은 똑바로 서서 줄을 따라 글자마다 1em 을 차지하고 라틴 구간은 눕는다. 둘은 같은 줄에서 차례로 놓이며
     * 조각의 (줄을 가로지르는) 너비는 글꼴의 높이이다.
     */
    public void testMixedRuns() {
        LayoutResult r = vertical("", "한국어ABC12");
        List<Run> t = runs(r, "v");
        Check.equal(2, t.size(), "an upright run and a sideways run");
        Run up = t.get(0);
        Run side = t.get(1);
        Check.equal("한국어", up.text, "the Hangul run");
        Check.equal(LayoutBox.GLYPHS_UPRIGHT, up.mode, "upright");
        Check.equal(1, up.first.getTextRotation(), "the advance runs downwards");
        Check.near(60, up.height, T, "three characters of one em (20)");
        Check.near(0, up.y, T, "the line starts at the top");
        Check.equal("ABC12", side.text, "the Latin run");
        Check.equal(LayoutBox.GLYPHS_SIDEWAYS, side.mode, "sideways");
        Check.near(60, side.y, T, "the sideways run follows the upright run");
        Check.near(LayoutTestSupport.advance(side.first.getFont(), "ABC12"), side.height, 0.5, "advance of the sideways run");
        Check.near(up.first.getAscent() + up.first.getDescent(), up.width, T, "across the line a fragment is as wide as the font is high");
        Check.near(up.x, side.x, T, "both are in the same column");
        Check.equal(1, LayoutTestSupport.lines(LayoutTestSupport.box(r, "v")).size(), "one line");
        // 똑바로 선 조각은 글자마다 한 칸이다: 각 조각의 높이는 클러스터 수 곱하기 20.
        List<LayoutBox> frags = texts(r, "v");
        // 조각마다 클러스터 수와 높이를 대조한다.
        for (int i = 0; i < frags.size(); i++) {
            LayoutBox f = frags.get(i);
            if (f.getGlyphMode() == LayoutBox.GLYPHS_UPRIGHT) {
                Check.near((f.getClusterOffsets().length - 1) * 20, f.getHeight(), T, "an upright fragment takes a cell per cluster");
            }
        }
    }

    /**
     * The line length counts the upright cells: Hangul and punctuation upright, the Latin letters sideways, in the order of the text.
     * <p>
     * 줄 길이는 똑바로 선 칸을 센다: 한글과 문장 부호는 똑바로, 라틴 글자는 눕혀서, 텍스트 순서대로.
     */
    public void testRunsInTextOrder() {
        LayoutResult r = vertical("", "A한한B、C");
        List<Run> t = runs(r, "v");
        Check.equal(5, t.size(), "A, two Hangul, B, the comma (upright), C: five runs");
        Check.equal("A", t.get(0).text, "first run");
        Check.equal(LayoutBox.GLYPHS_SIDEWAYS, t.get(0).mode, "A sideways");
        Check.equal("한한", t.get(1).text, "second run");
        Check.near(40, t.get(1).height, T, "two cells");
        Check.equal("B", t.get(2).text, "third run");
        Check.equal("、", t.get(3).text, "the ideographic comma is upright");
        Check.equal(LayoutBox.GLYPHS_UPRIGHT, t.get(3).mode, "upright");
        Check.near(20, t.get(3).height, T, "one cell");
        Check.equal("C", t.get(4).text, "last run");
        double y = 0;
        // 구간이 줄을 따라 빈틈없이 이어지는지 확인한다.
        for (int i = 0; i < t.size(); i++) {
            Check.near(y, t.get(i).y, T, "run " + i + " follows the previous one");
            y += t.get(i).height;
        }
    }

    /**
     * <code>text-orientation: upright</code> stands Latin letters upright (one em each), the space stays sideways.
     * <p>
     * <code>text-orientation: upright</code> 는 라틴 글자를 똑바로 세우고(글자마다 1em) 공백은 눕힌 채로 둔다.
     */
    public void testUprightKeyword() {
        LayoutResult r = vertical("text-orientation:upright", "ABC d");
        List<Run> t = runs(r, "v");
        Check.equal(3, t.size(), "ABC, the space, d");
        Check.equal(LayoutBox.GLYPHS_UPRIGHT, t.get(0).mode, "ABC upright");
        Check.near(60, t.get(0).height, T, "three cells");
        Check.equal(LayoutBox.GLYPHS_SIDEWAYS, t.get(1).mode, "the space is sideways");
        Check.equal(LayoutBox.GLYPHS_UPRIGHT, t.get(2).mode, "d upright");
        Check.near(20, t.get(2).height, T, "one cell");
    }

    /**
     * <code>upright</code> forces the used direction to ltr: with <code>direction:rtl</code> the text still starts at the top, while
     * sideways text starts at the bottom.
     * <p>
     * <code>upright</code> 는 사용된 방향을 ltr 로 강제한다: <code>direction:rtl</code> 이어도 텍스트는 위에서 시작하고 눕힌 텍스트는 아래에서 시작한다.
     */
    public void testUprightForcesLtr() {
        LayoutResult up = vertical("direction:rtl;text-orientation:upright;height:100px", "ABC");
        Check.near(0, texts(up, "v").get(0).getY(), T, "upright text starts at the top although direction is rtl");
        LayoutResult side = vertical("direction:rtl;text-orientation:sideways;height:100px", "ABC");
        LayoutBox s = texts(side, "v").get(0);
        Check.near(100 - s.getHeight(), s.getY(), T, "sideways text starts at the bottom with direction:rtl");
    }

    /**
     * <code>sideways</code> keeps the previous behaviour: the whole text is rotated, also Hangul. <code>writing-mode: sideways-lr</code>
     * ignores <code>text-orientation</code>.
     * <p>
     * <code>sideways</code> 는 이전 동작을 유지한다: 한글도 텍스트 전체가 회전한다. <code>writing-mode: sideways-lr</code> 은
     * <code>text-orientation</code> 을 무시한다.
     */
    public void testSidewaysKeepsRotatedText() {
        LayoutResult r = vertical("text-orientation:sideways", "한국ABC");
        List<Run> t = runs(r, "v");
        Check.equal(1, t.size(), "one run");
        Check.equal(LayoutBox.GLYPHS_SIDEWAYS, t.get(0).mode, "sideways");
        LayoutResult slr = LayoutTestSupport.layout("<div id=v style='writing-mode:sideways-lr;text-orientation:upright;font:20px monospace;height:300px'>한국</div>", 400);
        List<LayoutBox> s = texts(slr, "v");
        Check.isFalse(s.isEmpty(), "has text");
        // 조각마다 눕힌 채 시계 반대 방향인지 확인한다(줄은 아래에서 위로 가므로 조각을 합치지 않는다).
        for (int i = 0; i < s.size(); i++) {
            Check.equal(LayoutBox.GLYPHS_SIDEWAYS, s.get(i).getGlyphMode(), "sideways-lr is always sideways");
            Check.equal(3, s.get(i).getTextRotation(), "counter-clockwise");
        }
    }

    /**
     * Letter spacing is added after every upright cell.
     * <p>
     * 글자 간격은 똑바로 선 칸마다 뒤에 더해진다.
     */
    public void testLetterSpacing() {
        LayoutResult r = vertical("letter-spacing:5px", "한국어");
        List<Run> t = runs(r, "v");
        Check.equal(1, t.size(), "one run");
        Check.near(75, t.get(0).height, T, "three cells of 20 + 5");
        Check.near(5, t.get(0).first.getLetterSpacing(), T, "the spacing is recorded");
    }

    /**
     * A paragraph of upright characters is broken between characters at the height of the box, and the lines stack from the right.
     * Hangul syllables may be broken between (UAX #14), which the line breaker of the JRE does not offer.
     * <p>
     * 똑바로 선 글자의 문단은 상자 높이에서 글자 사이로 줄이 나뉘고 줄은 오른쪽부터 쌓인다. 한글 음절 사이는 줄바꿈이 가능하다(UAX #14). JRE 의 줄바꿈 도구는
     * 이를 제공하지 않는다.
     */
    public void testLineBreaksBetweenUprightCharacters() {
        LayoutResult r = vertical("height:100px", "가나다라마바사아자차");
        LayoutBox v = LayoutTestSupport.box(r, "v");
        List<LayoutBox> lines = LayoutTestSupport.lines(v);
        Check.equal(2, lines.size(), "ten characters of 20 in a height of 100: two lines");
        Check.near(60, v.getWidth(), T, "two lines of 30");
        Check.near(30, lines.get(0).getX(), T, "the first line is at the right");
        Check.near(0, lines.get(1).getX(), T, "the second line is at the left");
        List<Run> t = runs(r, "v");
        Check.equal(2, t.size(), "two runs, one per line");
        Check.near(100, t.get(0).height, T, "five characters in the first line");
        Check.near(100, t.get(1).height, T, "five characters in the second line");
        Check.equal(5, t.get(0).text.length(), "five characters");
    }

    /**
     * Without <code>word-break: keep-all</code> horizontal Hangul text breaks between syllables as well, with it only at spaces.
     * <p>
     * <code>word-break: keep-all</code> 이 없으면 가로쓰기 한글 텍스트도 음절 사이에서 줄이 나뉘고, 있으면 공백에서만 나뉜다.
     */
    public void testHangulBreaksInHorizontalText() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='font:20px monospace;width:100px'>가나다라마바사아자차</div>"
                + "<div id=b style='font:20px monospace;width:100px;word-break:keep-all'>가나다라마바사아자차</div>", 400);
        Check.isTrue(LayoutTestSupport.lines(LayoutTestSupport.box(r, "a")).size() >= 2, "the syllables wrap");
        Check.equal(1, LayoutTestSupport.lines(LayoutTestSupport.box(r, "b")).size(), "keep-all keeps the word together");
    }

    /**
     * An element with <code>text-combine-upright: all</code> becomes one combined fragment that is one em long along the line, the text
     * before and after it stays upright.
     * <p>
     * <code>text-combine-upright: all</code> 인 요소는 줄을 따라 1em 길이의 조합 조각 하나가 되고 앞뒤 텍스트는 똑바로 선 채로 남는다.
     */
    public void testCombineAll() {
        LayoutResult r = vertical("", "平成<span style='text-combine-upright:all'>2025</span>年");
        List<Run> t = runs(r, "v");
        Check.equal(3, t.size(), "upright, combined, upright");
        Check.equal(LayoutBox.GLYPHS_UPRIGHT, t.get(0).mode, "before");
        Check.near(40, t.get(0).height, T, "two cells");
        Run c = t.get(1);
        Check.equal("2025", c.text, "the combined text");
        Check.equal(LayoutBox.GLYPHS_COMBINED, c.mode, "combined");
        Check.near(20, c.height, T, "one em along the line regardless of the number of digits");
        Check.near(40, c.y, T, "it follows the first run");
        Check.near(c.first.getAscent() + c.first.getDescent(), c.width, T, "across the line it is as wide as the font is high");
        Check.equal(LayoutBox.GLYPHS_UPRIGHT, t.get(2).mode, "after");
        Check.near(60, t.get(2).y, T, "the last run follows the combined cell");
    }

    /**
     * <code>digits</code> combines short digit sequences only: with 2 the sequences of one or two digits, not the year 2010.
     * <p>
     * <code>digits</code> 는 짧은 숫자 연속만 조합한다: 2 이면 한두 자리 연속을 조합하고 2010 년은 조합하지 않는다.
     */
    public void testCombineDigits() {
        LayoutResult r = vertical("text-combine-upright:digits 2", "平成20年4月16日2010");
        List<Run> t = runs(r, "v");
        String[] texts = { "平成", "20", "年", "4", "月", "16", "日", "2010" };
        Check.equal(texts.length, t.size(), "eight runs");
        // 구간마다 텍스트를 확인한다.
        for (int i = 0; i < texts.length; i++) {
            Check.equal(texts[i], t.get(i).text, "run " + i);
        }
        Check.equal(LayoutBox.GLYPHS_COMBINED, t.get(1).mode, "20 is combined");
        Check.equal(LayoutBox.GLYPHS_COMBINED, t.get(3).mode, "4 is combined");
        Check.equal(LayoutBox.GLYPHS_COMBINED, t.get(5).mode, "16 is combined");
        Check.equal(LayoutBox.GLYPHS_SIDEWAYS, t.get(7).mode, "2010 has four digits: sideways");
        Check.near(20, t.get(1).height, T, "a combined cell is one em");
        Check.near(80, t.get(3).y, T, "the cells follow each other: 2 + 1 + 1 cells of 20 before the 4");
        LayoutResult r4 = vertical("text-combine-upright:digits 4", "2010");
        Check.equal(LayoutBox.GLYPHS_COMBINED, runs(r4, "v").get(0).mode, "digits 4 combines 2010");
    }

    /**
     * A line is not broken inside a combined group: in a box that is too short for the group it stays whole, and the group is a break
     * opportunity on both sides.
     * <p>
     * 조합 묶음 안에서는 줄을 나누지 않는다: 묶음에 비해 너무 낮은 상자에서도 묶음은 그대로 남고, 묶음의 앞뒤는 줄바꿈 기회이다.
     */
    public void testNoBreakInsideCombinedGroup() {
        LayoutResult r = vertical("height:25px", "平<span style='text-combine-upright:all'>2025</span>年");
        List<Run> t = runs(r, "v");
        int combined = 0;
        // 조합 구간이 나뉘지 않고 하나뿐인지 센다.
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).mode == LayoutBox.GLYPHS_COMBINED) {
                combined++;
                Check.equal("2025", t.get(i).text, "the group is whole");
            }
        }
        Check.equal(1, combined, "exactly one combined fragment");
        Check.equal(3, LayoutTestSupport.lines(LayoutTestSupport.box(r, "v")).size(), "each part takes a line of its own at 25px");
    }

    /**
     * Horizontal text is not touched: Hangul in horizontal lines has no glyph mode and no rotation, and
     * <code>text-combine-upright</code> and <code>text-orientation</code> have no effect there.
     * <p>
     * 가로쓰기 텍스트는 건드리지 않는다: 가로 줄의 한글은 글리프 방식도 회전도 없고 <code>text-combine-upright</code> 과 <code>text-orientation</code> 은 효과가 없다.
     */
    public void testHorizontalTextIsUnchanged() {
        LayoutResult r = LayoutTestSupport.layout("<div id=h style='font:20px monospace;text-combine-upright:all;text-orientation:upright'>한국ABC 2025</div>", 400);
        List<LayoutBox> t = texts(r, "h");
        Check.isFalse(t.isEmpty(), "has text");
        // 조각마다 평범한 가로 텍스트인지 확인한다.
        for (int i = 0; i < t.size(); i++) {
            LayoutBox f = t.get(i);
            Check.equal(LayoutBox.GLYPHS_SIDEWAYS, f.getGlyphMode(), "no special glyph mode");
            Check.equal(0, f.getTextRotation(), "not rotated");
            Check.near(LayoutTestSupport.advance(f.getFont(), f.getText()), f.getWidth(), T, "the width of the string");
        }
    }

    /**
     * The upright text of an inline element of its own is placed in the line like other text: the inline box has the extent of its
     * characters.
     * <p>
     * 인라인 요소 자신의 똑바로 선 텍스트도 다른 텍스트처럼 줄에 놓인다: 인라인 상자는 자기 글자의 범위를 갖는다.
     */
    public void testInlineElementAroundUprightText() {
        LayoutResult r = vertical("", "A<b id=b>한국</b>C");
        LayoutBox b = LayoutTestSupport.box(r, "b");
        List<Run> t = runs(r, "v");
        Check.equal(3, t.size(), "A, the bold Hangul, C");
        Check.near(t.get(1).y, b.getY(), T, "the inline box starts with its text");
        Check.near(40, b.getHeight(), T, "and is as long as its two cells");
    }

    /**
     * The fragments report the source offsets of their characters, also for several runs of one text node.
     * <p>
     * 조각은 자기 글자의 원본 위치를 알려 준다. 텍스트 노드 하나의 여러 구간도 마찬가지이다.
     */
    public void testSourceOffsetsOfRuns() {
        LayoutResult r = vertical("", "한국AB어");
        List<Run> t = runs(r, "v");
        Check.equal(3, t.size(), "three runs");
        Check.equal(0, t.get(0).first.getSourceStart(), "first run start");
        Check.equal(2, t.get(0).last.getSourceEnd(), "first run end");
        Check.equal(2, t.get(1).first.getSourceStart(), "second run start");
        Check.equal(4, t.get(1).last.getSourceEnd(), "second run end");
        Check.equal(4, t.get(2).first.getSourceStart(), "third run start");
        Check.equal(5, t.get(2).last.getSourceEnd(), "third run end");
        Check.equal(2, t.get(0).first.getClusterOffsets().length, "the first fragment is one Hangul syllable: one cluster and the end offset");
    }
}
