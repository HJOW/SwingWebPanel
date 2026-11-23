package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>text-justify: inter-character</code> (and the legacy <code>distribute</code>) (plan item P10-08, CSS Text Level 3, section 6.4, https://www.w3.org/TR/css-text-3/#text-justify-property):
 * a justified line spreads its spare width between all of its characters, not only at the spaces, so that text without spaces reaches both edges. The last line is not
 * justified, a combining mark stays with its base character, <code>inter-word</code> still justifies at the spaces only, and <code>auto</code> spreads the CJK letters of a line that has no spaces.
 * <p>
 * <code>text-justify: inter-character</code>(와 옛 이름 <code>distribute</code>) 시험(계획 P10-08, CSS Text Level 3 6.4 절): 양쪽 정렬된 줄은 남는 너비를 공백뿐 아니라 모든 글자 사이에
 * 나누므로 공백이 없는 텍스트도 양 끝에 닿는다. 마지막 줄은 정렬하지 않고, 결합 부호는 기본 글자와 함께 있으며, <code>inter-word</code> 는 여전히 공백에서만 정렬하고, <code>auto</code> 는 공백이 없는 줄의 한중일 글자 사이를 늘인다.
 */
public class TextJustifyCharacterTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TextJustifyCharacterTest() {
    }

    /**
     * Lays out a paragraph of the given text in a box that is 12.5 characters wide, and returns its lines.
     * <p>
     * 주어진 텍스트의 문단을 글자 12.5 개 너비의 상자에 배치하고 그 줄을 반환한다.
     *
     * @param style the style of the paragraph / 문단의 스타일
     * @param text  the text / 텍스트
     * @return the lines / 줄
     */
    private static List<LayoutBox> lines(String style, String text) {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div style='width:" + 12.5 * cw + "px'><p id=p style='margin:0;text-align:justify;line-break:anywhere;" + style + "'>" + text + "</p></div>", 600);
        return LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"));
    }

    /**
     * The right edge of the last fragment of a line.
     * <p>
     * 줄의 마지막 조각의 오른쪽 가장자리.
     *
     * @param line the line / 줄
     * @return its right edge / 오른쪽 가장자리
     */
    private static double right(LayoutBox line) {
        LayoutBox last = line.getChildren().get(line.getChildren().size() - 1);
        return last.getX() + last.getWidth();
    }

    /**
     * Text without spaces: by default (<code>auto</code>) the line stays ragged; with <code>inter-character</code> every character is a fragment of its own, the spare width
     * is shared equally between them and the first line reaches the right edge; the last line is not justified.
     * <p>
     * 공백 없는 텍스트: 기본(<code>auto</code>)에서는 줄이 들쭉날쭉하고, <code>inter-character</code> 에서는 글자마다 조각이 되고 남는 너비가 똑같이 나뉘어 첫 줄이 오른쪽
     * 가장자리에 닿으며 마지막 줄은 정렬하지 않는다.
     */
    public void testSpreadBetweenCharacters() {
        double cw = LayoutTestSupport.charWidth();
        String text = "abcdefghijklmnopqrstuvwxyz";
        List<LayoutBox> auto = lines("", text);
        Check.equal(3, auto.size(), "26 letters in lines of 12 characters: three lines");
        Check.isTrue(right(auto.get(0)) < auto.get(0).getX() + 12.5 * cw - cw / 4, "auto: the line is ragged (no spaces to widen)");
        List<LayoutBox> chars = lines("text-justify:inter-character", text);
        Check.equal(3, chars.size(), "the same lines");
        LayoutBox first = chars.get(0);
        Check.equal(12, first.getChildren().size(), "inter-character: every character of the first line is a fragment");
        Check.near(first.getX() + 12.5 * cw, right(first), 0.6, "the first line reaches the right edge");
        double gap0 = first.getChildren().get(1).getX() - (first.getChildren().get(0).getX() + first.getChildren().get(0).getWidth());
        double gap7 = first.getChildren().get(8).getX() - (first.getChildren().get(7).getX() + first.getChildren().get(7).getWidth());
        Check.near(0.5 * cw / 11, gap0, 0.05, "the spare half character is shared by the eleven gaps");
        Check.near(gap0, gap7, 0.01, "all gaps are equal");
        Check.near(first.getX(), first.getChildren().get(0).getX(), 0.01, "the first character starts at the left edge");
        LayoutBox last = chars.get(2);
        Check.isTrue(right(last) < last.getX() + 12.5 * cw - cw, "the last line is not justified");
        Check.equal(12, lines("text-justify:distribute", text).get(0).getChildren().size(), "distribute is the same as inter-character");
        List<LayoutBox> word = lines("text-justify:inter-word", text);
        Check.isTrue(right(word.get(0)) < word.get(0).getX() + 12.5 * cw - cw / 4, "inter-word has no spaces to widen: the line stays ragged");
    }

    /**
     * The spaces take their share too: the spare width of a line with a space is shared between all characters, so the gap after a space is as wide as the others.
     * <p>
     * 공백도 몫을 가진다: 공백이 있는 줄의 남는 너비는 모든 글자에 나뉘므로 공백 뒤의 간격도 다른 간격과 같다.
     */
    public void testSpacesAreCharacters() {
        double cw = LayoutTestSupport.charWidth();
        // 첫 줄은 "abcd efgh ij" 처럼 공백이 있는 12 자이다.
        List<LayoutBox> lines = lines("text-justify:inter-character", "abcd efgh ijklmnopqrstu");
        LayoutBox first = lines.get(0);
        Check.near(first.getX() + 12.5 * cw, right(first), 0.6, "the line reaches the right edge");
        List<LayoutBox> kids = first.getChildren();
        double gapInside = kids.get(1).getX() - (kids.get(0).getX() + kids.get(0).getWidth());
        // 공백은 조각이 아니므로 d 와 e 사이는 공백 너비 + 간격 둘이다.
        double aroundSpace = kids.get(4).getX() - (kids.get(3).getX() + kids.get(3).getWidth());
        Check.near(2 * gapInside + cw, aroundSpace, 0.05, "the space is a character that gets spacing on both sides");
    }

    /**
     * A character is a cluster: a base letter with its combining mark, a surrogate pair and a joined sequence are not cut apart.
     * <p>
     * 글자는 클러스터이다: 기본 글자와 결합 부호, 서로게이트 쌍, 이어 붙인 열은 갈라지지 않는다.
     */
    public void testClusters() {
        Check.equal(1, InlineLayouter.nextCluster("a", 0), "a plain letter");
        Check.equal(2, InlineLayouter.nextCluster("éx", 0), "a letter and its acute accent");
        Check.equal(3, InlineLayouter.nextCluster("é̂x", 0), "two combining marks");
        Check.equal(2, InlineLayouter.nextCluster("😀x", 0), "a surrogate pair");
        Check.equal(5, InlineLayouter.nextCluster("" + (char) 0xD83D + (char) 0xDC68 + (char) 0x200D + (char) 0xD83D + (char) 0xDC69 + "x", 0), "two emoji joined by a zero width joiner");
        Check.equal(2, InlineLayouter.nextCluster("❤️x", 0), "a heart with its variation selector");
        Check.equal(1, InlineLayouter.nextCluster("가나", 0), "precomposed Hangul syllables are separate characters");
        Check.equal(3, InlineLayouter.nextCluster("한x", 0), "conjoining jamo form one syllable");
        Check.equal(3, InlineLayouter.nextCluster("ab́c", 1), "the helper starts at the given index");
    }

    /**
     * Characters of a script that does not use spaces (Hangul syllables, Han characters) are justified between characters as well.
     * <p>
     * 공백을 쓰지 않는 문자(한글 음절, 한자)도 글자 사이로 정렬된다.
     */
    public void testCjkText() {
        String text = "가나다라마바사아자차카타파하거너더러머버서어저처커터퍼허";
        double cw = LayoutTestSupport.charWidth();
        List<LayoutBox> chars = lines("text-justify:inter-character", text);
        Check.isTrue(chars.size() >= 2, "the text wraps");
        LayoutBox first = chars.get(0);
        Check.isTrue(first.getChildren().size() >= 3, "every syllable of the first line is a fragment");
        Check.near(first.getX() + 12.5 * cw, right(first), 0.6, "the first line reaches the right edge");
        // auto 는 공백이 없는 줄에서 한중일 글자 사이를 늘이고(CSS Text 3 6.4.3 의 예), inter-word 로 정하면 늘이지 않는다.
        List<LayoutBox> auto = lines("", text);
        Check.near(auto.get(0).getX() + 12.5 * cw, right(auto.get(0)), 0.6, "auto: a line of CJK letters without spaces is spread between the letters too");
        List<LayoutBox> word = lines("text-justify:inter-word", text);
        Check.isTrue(right(word.get(0)) < word.get(0).getX() + 12.5 * cw - 1, "inter-word: no spaces to widen, the line stays ragged");
        Check.isTrue(InlineLayouter.isCjk(0x4E2D) && InlineLayouter.isCjk(0x3042) && InlineLayouter.isCjk(0x30AB) && InlineLayouter.isCjk(0xAC00), "Han, Hiragana, Katakana and Hangul are CJK letters");
        Check.isFalse(InlineLayouter.isCjk('a') || InlineLayouter.isCjk(' ') || InlineLayouter.isCjk(0x0301), "a Latin letter, a space and a combining mark are not");
    }
}
