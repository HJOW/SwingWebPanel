package org.duckdns.hjow.swingwebpanel.media.vtt;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the WebVTT cue text parser (plan item P20-13; FEATURE_MATRIX VID-02): the spans of the tags <code>c i b u ruby rt v lang</code>,
 * classes and annotations, timestamps, the character references, the tags that are ignored, the end tags that do not match, the languages,
 * and the chapter title. The trees are written as text for the comparison.
 * <p>
 * WebVTT cue 텍스트 파서의 시험(계획 P20-13, FEATURE_MATRIX VID-02): 태그 <code>c i b u ruby rt v lang</code> 의 span, 클래스와 주석, 타임스탬프, 문자 참조,
 * 무시하는 태그, 맞지 않는 종료 태그, 언어, 장 제목. 트리는 비교하려고 텍스트로 쓴다.
 */
public class VttCueTextTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public VttCueTextTest() {
    }

    /**
     * Writes a tree as text: <code>name(classes|value|lang: children)</code>, a text as a quoted string and a timestamp as
     * <code>@seconds</code>.
     * <p>
     * 트리를 텍스트로 쓴다: <code>이름(클래스|값|lang: 자식)</code>, 텍스트는 따옴표 문자열, 타임스탬프는 <code>@초</code>.
     *
     * @param n the node / 노드
     * @return the text / 텍스트
     */
    private static String dump(VttNode n) {
        if (n.kind == VttNode.TEXT) {
            return "'" + n.value + "'";
        }
        if (n.kind == VttNode.TIMESTAMP) {
            return "@" + n.time;
        }
        String[] names = { "list", "c", "i", "b", "u", "ruby", "rt", "v", "lang" };
        StringBuilder sb = new StringBuilder(names[n.kind]);
        if (n.classes.size() > 0) {
            sb.append('.').append(n.classes);
        }
        if (n.kind == VttNode.VOICE) {
            sb.append('[').append(n.value).append(']');
        }
        if (n.language != null) {
            sb.append('{').append(n.language).append('}');
        }
        sb.append('(');
        // 자식을 쉼표로 이어 쓴다.
        for (int i = 0; i < n.children.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(dump(n.children.get(i)));
        }
        return sb.append(')').toString();
    }

    /**
     * Parses a text and dumps the tree.
     * <p>
     * 텍스트를 해석해 트리를 쓴다.
     *
     * @param s the cue text / cue 텍스트
     * @return the dump / 덤프
     */
    private static String tree(String s) {
        return dump(VttCueText.parse(s, null));
    }

    /**
     * Plain text and the tags that make spans, with nesting and classes.
     * <p>
     * 일반 텍스트와 span 을 만드는 태그(중첩과 클래스 포함).
     */
    public void testSpans() {
        Check.equal("list('hello')", tree("hello"), "plain text");
        Check.equal("list()", tree(""), "empty text");
        Check.equal("list('a ',i('b'),' c')", tree("a <i>b</i> c"), "italic");
        Check.equal("list(b(i(u('x'))))", tree("<b><i><u>x</u></i></b>"), "nesting");
        Check.equal("list(c.[red, big]('x'))", tree("<c.red.big>x</c>"), "classes");
        Check.equal("list(c.[a]('x'))", tree("<c.a.>x</c>"), "an empty class is dropped");
        Check.equal("list(v[Fred Smith](i('hi')))", tree("<v Fred Smith><i>hi</i></v>"), "voice with a name");
        Check.equal("list(v[Mary Jane]('x'))", tree("<v   Mary \t  Jane  >x"), "annotation white space is collapsed and trimmed, the span is closed by the end");
        Check.equal("list(v.[loud][Bob]('x'))", tree("<v.loud Bob>x</v>"), "voice with a class");
        Check.equal("list(v[]('x'))", tree("<v>x</v>"), "voice without a name");
        Check.equal("list(ruby('base',rt('text')))", tree("<ruby>base<rt>text</rt></ruby>"), "ruby");
        Check.equal("list(ruby('base',rt('text')),' after')", tree("<ruby>base<rt>text</ruby> after"), "a ruby end tag inside the ruby text closes both");
        Check.equal("list('a','x')", tree("a<rt>x</rt>"), "rt outside a ruby is ignored together with its end tag");
        Check.equal("list(i('x'),'y')", tree("<i>x</i></i>y"), "an end tag without a start tag is ignored");
        Check.equal("list(i(b('x','y')))", tree("<i><b>x</i>y"), "an end tag that does not match the current node is ignored");
        Check.equal("list('a','b')", tree("a<foo>b</foo>"), "unknown tags are ignored");
        Check.equal("list('a','b')", tree("a<>b"), "an empty tag is ignored");
        Check.equal("list('a',b())", tree("a<b"), "an unfinished tag is a tag (the end of the input closes it)");
    }

    /**
     * Languages: the <code>lang</code> span sets the applicable language of what it contains and the language stack is restored after it; a
     * fallback language applies to the root.
     * <p>
     * 언어: <code>lang</code> span 은 내용에 적용되는 언어를 정하고 언어 스택은 그 뒤에 복원된다. 대체 언어는 루트에 적용된다.
     */
    public void testLanguages() {
        Check.equal("list(lang{en}(i{en}('x'),'y'),i('z'))", tree("<lang en><i>x</i>y</lang><i>z</i>"), "a language span");
        Check.equal("list{ko}(i{ko}('a'),lang{ja}(b{ja}('b')),i{ko}('c'))", dump(VttCueText.parse("<i>a</i><lang ja><b>b</b></lang><i>c</i>", "ko")),
                "the fallback language and the stack");
        Check.equal("list(lang{}('x'))", tree("<lang>x</lang>"), "no annotation is the empty language");
    }

    /**
     * Timestamps: the tag whose text starts with a digit is a timestamp if the whole text is one, else it is ignored.
     * <p>
     * 타임스탬프: 숫자로 시작하는 태그는 텍스트 전체가 타임스탬프일 때 타임스탬프이며 아니면 무시한다.
     */
    public void testTimestamps() {
        Check.equal("list('a ',@1.5,' b')", tree("a <00:01.500> b"), "a timestamp");
        Check.equal("list(@3723.5)", tree("<01:02:03.500>"), "the hours form");
        Check.equal("list('x')", tree("<00:01.500x>x"), "a trailing character: ignored");
        Check.equal("list('x')", tree("<12>x"), "not a timestamp: ignored");
        Check.equal("list('x')", tree("<00:61.000>x"), "a wrong timestamp: ignored");
    }

    /**
     * Character references: named (with and without a semicolon, the legacy ones), numeric (decimal, hexadecimal, replacements), and text
     * that only looks like a reference. They also work in an annotation.
     * <p>
     * 문자 참조: 이름 있는 것(세미콜론 있음/없음, 옛 이름), 숫자(10진, 16진, 대체), 참조처럼 보일 뿐인 텍스트. 주석 안에서도 동작한다.
     */
    public void testCharacterReferences() {
        Check.equal("list('a&b')", tree("a&amp;b"), "amp");
        Check.equal("list('<tag>')", tree("&lt;tag&gt;"), "lt and gt do not make tags");
        Check.equal("list('a" + (char) 0xA0 + "b')", tree("a&nbsp;b"), "nbsp");
        Check.equal("list('&b')", tree("&ampb"), "a legacy reference that needs no semicolon, the rest is text");
        Check.equal("list('&;x')", tree("&;x"), "an ampersand alone");
        Check.equal("list('&zzz;')", tree("&zzz;"), "an unknown name stays");
        Check.equal("list('AA')", tree("&#65;&#x41;"), "numeric");
        Check.equal("list('A')", tree("&#65"), "a numeric reference without a semicolon");
        Check.equal("list('" + (char) 0x20AC + "')", tree("&#x80;"), "a C1 reference is replaced");
        Check.equal("list('" + (char) 0xFFFD + "')", tree("&#0;"), "zero becomes the replacement character");
        Check.equal("list('&#;')", tree("&#;"), "no digits");
        Check.equal("list('" + new String(Character.toChars(0x1F600)) + "')", tree("&#x1F600;"), "outside the BMP");
        Check.equal("list(v[A&B]('x'))", tree("<v A&amp;B>x"), "a reference in an annotation");
        Check.equal("list('1 &lt 2')", tree("1 &amp;lt 2"), "the replaced text is not scanned again");
    }

    /**
     * The chapter title is the text without the ruby text.
     * <p>
     * 장 제목은 루비 텍스트를 뺀 텍스트이다.
     */
    public void testChapterTitle() {
        Check.equal("Intro: the start", VttCueText.chapterTitle("Intro: <i>the</i> start"), "tags are removed");
        Check.equal("Aa", VttCueText.chapterTitle("<ruby>A<rt>ignored</rt></ruby>&#97;"), "ruby text is excluded");
        Check.equal("", VttCueText.chapterTitle(""), "empty");
    }

    /**
     * Timestamps are written with all components when the cue text becomes DOM nodes.
     * <p>
     * cue 텍스트가 DOM 노드가 될 때 타임스탬프는 모든 구성 요소를 갖춰 쓴다.
     */
    public void testTimestampFormat() {
        Check.equal("00:00:01.500", VttTimestamps.format(1.5), "a short time");
        Check.equal("01:02:03.456", VttTimestamps.format(3723.456), "hours");
        Check.equal("100:00:00.000", VttTimestamps.format(360000), "more than two digits of hours");
        Check.equal("00:00:00.007", VttTimestamps.format(0.007), "milliseconds with zeros");
    }
}
