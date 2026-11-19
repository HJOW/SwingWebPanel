package org.duckdns.hjow.swingwebpanel.content.markdown;

import java.util.concurrent.atomic.AtomicInteger;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Markdown converter beyond the specification examples (plan items P17-03, P17-04): the safe defaults (escaped raw HTML,
 * neutralized URL schemes), the options, the limits and the behavior on pathological input, the generated page and thread safety.
 * <p>
 * 규격 예제 밖의 Markdown 변환기 시험(계획 P17-03, P17-04): 안전한 기본값(이스케이프된 원시 HTML, 무력화된 URL 스킴), 옵션, 제한, 병적인 입력에서의
 * 동작, 생성되는 페이지, 스레드 안전성.
 */
public class MarkdownConverterTest {

    /** Time allowed for one pathological input, generous because the test machine may be busy. 병적인 입력 하나에 허용하는 시간. 시험 기계가 바쁠 수 있어 넉넉하다. */
    private static final long LIMIT_MILLIS = 30000;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MarkdownConverterTest() {
    }

    /**
     * Raw HTML is escaped and shown as text by default, in inline and block form.
     * <p>
     * 원시 HTML 은 기본적으로 인라인과 블록 모두 이스케이프하여 텍스트로 보여 준다.
     */
    public void testRawHtmlIsEscapedByDefault() {
        Check.equal("<p>a &lt;script&gt;alert(1)&lt;/script&gt; b</p>\n", MarkdownConverter.toHtml("a <script>alert(1)</script> b"), "inline script");
        Check.equal("<pre class=\"md-raw-html\">&lt;div onclick=&quot;x()&quot;&gt;\n*foo*\n&lt;/div&gt;</pre>\n",
                MarkdownConverter.toHtml("<div onclick=\"x()\">\n*foo*\n</div>"), "HTML block");
        Check.equal("<p>x &lt;!-- c --&gt; &lt;?pi?&gt;</p>\n", MarkdownConverter.toHtml("x <!-- c --> <?pi?>"), "comment and processing instruction");
        Check.equal("<pre class=\"md-raw-html\">&lt;!-- c --&gt; &lt;?pi?&gt;</pre>\n", MarkdownConverter.toHtml("<!-- c --> <?pi?>"), "comment starting a block");
        String h = MarkdownConverter.toHtml("[<img src=x onerror=alert(1)>](/u)\n\n<style>p{}</style>\n\n`<b>` and <b>x</b>");
        Check.isFalse(h.indexOf("<img") >= 0 || h.indexOf("<style") >= 0 || h.indexOf("<b>x") >= 0, "no raw tag survives: " + h);
    }

    /**
     * With allowRawHtml the HTML is passed through; the GFM tag filter still disables the dangerous tags.
     * <p>
     * allowRawHtml 이면 HTML 을 그대로 내보내며 GFM 태그 필터는 위험한 태그를 여전히 막는다.
     */
    public void testAllowRawHtml() {
        MarkdownOptions o = new MarkdownOptions().setAllowRawHtml(true);
        Check.equal("<p>a <b>x</b> b</p>\n", MarkdownConverter.toHtml("a <b>x</b> b", o), "inline HTML passes");
        Check.equal("<div>\n*foo*\n</div>\n", MarkdownConverter.toHtml("<div>\n*foo*\n</div>", o), "block HTML passes");
        Check.equal("<p>t &lt;script>x&lt;/script> <em>ok</em></p>\n", MarkdownConverter.toHtml("t <script>x</script> *ok*", o), "tag filter");
        Check.equal("&lt;script>x&lt;/script> *ok*\n", MarkdownConverter.toHtml("<script>x</script> *ok*", o), "a line that starts an HTML block is raw (filtered)");
        Check.equal("<p>t <script>x</script></p>\n", MarkdownConverter.toHtml("t <script>x</script>", o.copy().setGfm(false)), "no tag filter without GFM");
    }

    /**
     * Dangerous URL schemes lose their destination in links, images and autolinks, also when disguised by entities or case.
     * <p>
     * 위험한 URL 스킴은 링크, 이미지, 자동 링크에서 목적지를 잃으며 엔티티나 대소문자로 위장해도 마찬가지이다.
     */
    public void testUnsafeUrlsAreNeutralized() {
        Check.equal("<p><a>x</a></p>\n", MarkdownConverter.toHtml("[x](javascript:alert(1))"), "javascript link");
        Check.equal("<p><a>x</a></p>\n", MarkdownConverter.toHtml("[x](JaVaScRiPt:alert(1))"), "mixed case");
        Check.equal("<p><a>x</a></p>\n", MarkdownConverter.toHtml("[x](&#106;avascript:alert(1))"), "entity-encoded");
        Check.equal("<p><a>x</a></p>\n", MarkdownConverter.toHtml("[x](vbscript:msgbox)"), "vbscript");
        Check.equal("<p><a>x</a></p>\n", MarkdownConverter.toHtml("[x](data:text/html;base64,AAAA)"), "data link");
        Check.equal("<p><a>x</a></p>\n", MarkdownConverter.toHtml("[x](file:///etc/passwd)"), "file link");
        Check.equal("<p><img src=\"\" alt=\"x\" /></p>\n", MarkdownConverter.toHtml("![x](data:image/png;base64,AAAA)"), "data image");
        Check.equal("<p><a>javascript:alert(1)</a></p>\n", MarkdownConverter.toHtml("<javascript:alert(1)>"), "autolink");
        Check.equal("<p><a>x</a></p>\n", MarkdownConverter.toHtml("[x]\n\n[x]: javascript:alert(1)"), "reference definition");
        Check.equal("<p><a href=\"http://example.com/a\">x</a> <a href=\"foo/bar\">y</a> <a href=\"#frag\">z</a></p>\n",
                MarkdownConverter.toHtml("[x](http://example.com/a) [y](foo/bar) [z](#frag)"), "safe URLs are untouched");
        String off = MarkdownConverter.toHtml("[x](javascript:alert(1))", new MarkdownOptions().setNeutralizeUnsafeUrls(false));
        Check.equal("<p><a href=\"javascript:alert(1)\">x</a></p>\n", off, "can be switched off");
    }

    /**
     * Text, attribute values and titles are escaped, and a title or text cannot break out of an attribute.
     * <p>
     * 텍스트, 속성값, 제목은 이스케이프되며 제목이나 텍스트가 속성 밖으로 빠져나올 수 없다.
     */
    public void testEscaping() {
        Check.equal("<p><a href=\"/u?a=1&amp;b=2\" title=\"t&quot;&gt;&lt;script&gt;\">&lt;x&gt; &amp;</a></p>\n",
                MarkdownConverter.toHtml("[<x> &amp;](/u?a=1&b=2 \"t\\\"><script>\")"), "attributes");
        Check.equal("<p><img src=\"/p.png\" alt=\"a &quot; &lt; b\" title=\"q&quot;\" /></p>\n", MarkdownConverter.toHtml("![a \" < b](/p.png \"q\\\"\")"), "image alt");
        Check.equal("<pre><code class=\"language-a&quot;b\">x\n</code></pre>\n", MarkdownConverter.toHtml("```a\"b\nx\n```"), "code block language");
    }

    /**
     * Options have safe defaults, are copied by the converter and validate their values.
     * <p>
     * 옵션의 기본값은 안전하며 변환기가 복사해 쓰고 값을 검증한다.
     */
    public void testOptions() {
        MarkdownOptions o = new MarkdownOptions();
        Check.isFalse(o.isAllowRawHtml(), "raw HTML off");
        Check.isTrue(o.isGfm(), "GFM on");
        Check.isTrue(o.isNeutralizeUnsafeUrls(), "URLs neutralized");
        Check.equal(MarkdownOptions.DEFAULT_MAX_INPUT_LENGTH, o.getMaxInputLength(), "input limit");
        Check.equal("<p>a ~~b~~</p>\n", MarkdownConverter.toHtml("a ~~b~~", new MarkdownOptions().setGfm(false)), "no strikethrough without GFM");
        Check.equal("<p>a <del>b</del></p>\n", MarkdownConverter.toHtml("a ~~b~~"), "strikethrough with GFM");
        Check.equal("<p>x</p>\n", MarkdownConverter.toHtml("x", null), "null options mean defaults");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Converts null.
             * <p>
             * null 을 변환한다.
             */
            public void run() {
                MarkdownConverter.toHtml(null);
            }
        }, "null text");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Sets an invalid limit.
             * <p>
             * 잘못된 제한을 지정한다.
             */
            public void run() {
                new MarkdownOptions().setMaxBlockDepth(0);
            }
        }, "invalid depth");
    }

    /**
     * Input longer than the limit throws MarkdownLimitException; input at the limit is converted.
     * <p>
     * 제한보다 긴 입력은 MarkdownLimitException 을 던지고 제한과 같은 길이는 변환한다.
     */
    public void testInputLimit() {
        final MarkdownOptions o = new MarkdownOptions().setMaxInputLength(10);
        Check.equal("<p>0123456789</p>\n", MarkdownConverter.toHtml("0123456789", o), "at the limit");
        Check.throwsException(MarkdownLimitException.class, new Block() {
            /**
             * Converts an eleven character text with a limit of ten.
             * <p>
             * 제한이 10 일 때 11자 텍스트를 변환한다.
             */
            public void run() {
                MarkdownConverter.toHtml("01234567890", o);
            }
        }, "over the limit");
    }

    /**
     * Line endings are equivalent, NUL becomes the replacement character, and the empty text gives an empty fragment.
     * <p>
     * 줄 끝 표기는 동등하고 NUL 은 대체 문자가 되며 빈 텍스트는 빈 조각을 낸다.
     */
    public void testLineEndingsAndNul() {
        String unix = MarkdownConverter.toHtml("# a\n\n- b\n- c\n\ntext\nmore\n");
        Check.equal(unix, MarkdownConverter.toHtml("# a\r\n\r\n- b\r\n- c\r\n\r\ntext\r\nmore\r\n"), "CRLF");
        Check.equal(unix, MarkdownConverter.toHtml("# a\r\r- b\r- c\r\rtext\rmore\r"), "CR");
        Check.equal("<p>a" + (char) 0xFFFD + "b</p>\n", MarkdownConverter.toHtml("a" + (char) 0 + "b"), "NUL");
        Check.equal("", MarkdownConverter.toHtml(""), "empty");
        Check.equal("", MarkdownConverter.toHtml("\n\n  \n"), "blank lines");
    }

    /**
     * Korean text, supplementary characters and emphasis next to CJK punctuation convert correctly.
     * <p>
     * 한글, 보충 문자, CJK 구두점 옆의 강조가 올바르게 변환된다.
     */
    public void testKoreanAndUnicode() {
        String emoji = new String(Character.toChars(0x1F600));
        Check.equal("<p><strong>" + "한글" + "</strong> " + emoji + " <del>x</del></p>\n",
                MarkdownConverter.toHtml("**" + "한글" + "** " + emoji + " ~~x~~"), "Hangul and emoji");
        Check.equal("<p><a href=\"/%ED%95%9C%EA%B8%80\">link</a> <a href=\"/" + "%F0%9F%98%80" + "\">e</a></p>\n",
                MarkdownConverter.toHtml("[link](/" + "한글" + ") [e](/" + emoji + ")"), "URL encoding of non-ASCII");
    }

    /**
     * Very deep block nesting is limited instead of overflowing: the markers beyond the limit stay text.
     * <p>
     * 아주 깊은 블록 중첩은 스택을 넘치게 하지 않고 제한된다: 제한을 넘는 표식은 텍스트로 남는다.
     */
    public void testDeepBlockNesting() {
        StringBuilder q = new StringBuilder();
        // 인용 표식을 만 개 쌓는다.
        for (int i = 0; i < 10000; i++) {
            q.append('>');
        }
        q.append(" x");
        String h = timed(q.toString(), new MarkdownOptions());
        Check.equal(MarkdownOptions.DEFAULT_MAX_BLOCK_DEPTH, count(h, "<blockquote>"), "block quotes stop at the limit");
        Check.isTrue(h.indexOf("&gt;&gt;&gt;") >= 0 && h.indexOf("x") >= 0, "the rest stays text");
        StringBuilder l = new StringBuilder();
        // 목록 표식을 오천 개 쌓는다.
        for (int i = 0; i < 5000; i++) {
            l.append("- ");
        }
        l.append("x");
        h = timed(l.toString(), new MarkdownOptions().setMaxBlockDepth(20));
        Check.isTrue(count(h, "<ul>") <= 20 && count(h, "<ul>") >= 5, "nested lists are limited: " + count(h, "<ul>"));
        StringBuilder m = new StringBuilder();
        // 줄마다 들여쓴 중첩 목록을 삼천 단계 쌓는다.
        for (int i = 0; i < 3000; i++) {
            for (int k = 0; k < i; k++) {
                m.append(' ');
            }
            m.append("- a\n");
        }
        timed(m.toString(), new MarkdownOptions());
    }

    /**
     * Deeply nested emphasis and links are limited by the inline depth and do not overflow the stack.
     * <p>
     * 깊게 중첩된 강조와 링크는 인라인 깊이로 제한되며 스택을 넘치게 하지 않는다.
     */
    public void testDeepInlineNesting() {
        StringBuilder e = new StringBuilder();
        // 열고 닫는 강조를 각각 삼만 번 쌓는다.
        for (int i = 0; i < 30000; i++) {
            e.append("*a ");
        }
        // 닫는 강조 구분자를 이어 붙인다.
        for (int i = 0; i < 30000; i++) {
            e.append("a* ");
        }
        String h = timed(e.toString(), new MarkdownOptions());
        Check.isTrue(count(h, "<em>") <= MarkdownOptions.DEFAULT_MAX_INLINE_DEPTH + 1, "emphasis depth is limited: " + count(h, "<em>"));
        StringBuilder b = new StringBuilder();
        // 대괄호 열기/닫기를 오만 번 쌓는다.
        for (int i = 0; i < 50000; i++) {
            b.append('[');
        }
        // 대괄호를 닫는다.
        for (int i = 0; i < 50000; i++) {
            b.append(']');
        }
        timed(b.toString(), new MarkdownOptions());
        StringBuilder d = new StringBuilder("[a]: /u\n\n");
        // 참조 링크를 이중으로 감싼 것을 이만 번 쌓는다.
        for (int i = 0; i < 20000; i++) {
            d.append("![");
        }
        d.append("a");
        // 닫는 쪽 참조 링크를 이어 붙인다.
        for (int i = 0; i < 20000; i++) {
            d.append("][a]");
        }
        timed(d.toString(), new MarkdownOptions());
    }

    /**
     * Inputs that make naive implementations quadratic or worse finish quickly.
     * <p>
     * 단순한 구현이라면 제곱 시간 이상이 드는 입력도 빨리 끝난다.
     */
    public void testPathologicalInputs() {
        String[] units = { "[a](", "[a](<", "`a ``b ```c ", "<!--", "<?", "<![CDATA[", "<a b=\"", "<a ", "***a ", "_a_ ", "[a][", "&a", "&#1", "![",
                "a  \n", "\\", "> ", "- ", "1. ", "* * ", "| a |\n", "~~a ", "www.a.b/(", "http://", "a@b.c.", "    ", "\t", "<http://a>" };
        // 단위마다 십만 자 안팎으로 이어 붙인 입력을 시간 제한 안에 변환한다.
        for (int i = 0; i < units.length; i++) {
            StringBuilder sb = new StringBuilder();
            // 단위를 이어 붙여 목표 길이를 채운다.
            while (sb.length() < 100000) {
                sb.append(units[i]);
            }
            timed(sb.toString(), new MarkdownOptions());
        }
        // 서로 다른 길이의 백틱 열을 늘려 가며 닫히지 않게 한다.
        StringBuilder ticks = new StringBuilder();
        for (int i = 1; i < 400; i++) {
            // 백틱 열을 길이만큼 붙인다.
            for (int k = 0; k < i; k++) {
                ticks.append('`');
            }
            ticks.append(" x ");
        }
        timed(ticks.toString(), new MarkdownOptions());
    }

    /**
     * One long reference definition used many times cannot blow the output up: later uses stay plain text.
     * <p>
     * 긴 참조 정의 하나를 여러 번 써도 출력이 부풀지 않는다: 나중의 사용은 일반 텍스트로 남는다.
     */
    public void testReferenceExpansionIsLimited() {
        StringBuilder sb = new StringBuilder("[a]: /");
        // 길이 오만의 목적지를 만든다.
        for (int i = 0; i < 50000; i++) {
            sb.append('x');
        }
        sb.append("\n\n");
        // 같은 참조를 십만 번 쓴다.
        for (int i = 0; i < 100000; i++) {
            sb.append("[a] ");
        }
        String h = timed(sb.toString(), new MarkdownOptions());
        int links = count(h, "<a href=");
        Check.isTrue(links > 0 && links < 100000, "some uses are links, not all: " + links);
        Check.isTrue(h.length() < 8 * 1000 * 1000, "output stays bounded: " + h.length());
    }

    /**
     * The generated page is a complete HTML5 document with a UTF-8 declaration, an escaped title and a style block.
     * <p>
     * 생성되는 페이지는 UTF-8 선언, 이스케이프된 제목, 스타일 블록을 가진 완전한 HTML5 문서이다.
     */
    public void testDocument() {
        String d = MarkdownConverter.toHtmlDocument("# Hello <b> & `x`\n\ntext", null);
        Check.isTrue(d.startsWith("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n"), "head start: " + d);
        Check.contains(d, "<title>Hello &amp; x</title>", "title from the first heading");
        Check.contains(d, "<style>", "style block");
        Check.contains(d, "<body>\n<h1>Hello &lt;b&gt; &amp; <code>x</code></h1>\n<p>text</p>\n</body>\n</html>\n", "body");
        Check.contains(MarkdownConverter.toHtmlDocument("text", "A \"title\" <i>"), "<title>A &quot;title&quot; &lt;i&gt;</title>", "explicit title is escaped");
        Check.contains(MarkdownConverter.toHtmlDocument("text", null), "<title></title>", "no heading: empty title");
    }

    /**
     * Tables escape cell content, honour alignment and escaped pipes, and render inline content.
     * <p>
     * 표는 셀 내용을 이스케이프하고 정렬과 이스케이프된 파이프를 지키며 인라인 내용을 변환한다.
     */
    public void testTables() {
        Check.equal("<table>\n<thead>\n<tr>\n<th align=\"left\">a</th>\n<th align=\"right\">&lt;b&gt;</th>\n</tr>\n</thead>\n<tbody>\n<tr>\n"
                + "<td align=\"left\"><em>x</em></td>\n<td align=\"right\">y|z</td>\n</tr>\n</tbody>\n</table>\n",
                MarkdownConverter.toHtml("| a | <b> |\n|:--|--:|\n| *x* | y\\|z |\n"), "table with alignment");
        Check.equal("<p>| a |\n| b |</p>\n", MarkdownConverter.toHtml("| a |\n| b |"), "no delimiter row: no table");
        Check.equal("<p>a</p>\n<table>\n<thead>\n<tr>\n<th>b</th>\n</tr>\n</thead>\n</table>\n", MarkdownConverter.toHtml("a\n| b |\n|---|"),
                "the header is the last line of the paragraph");
    }

    /**
     * Converting from many threads at once gives identical results (no shared mutable state).
     * <p>
     * 여러 스레드가 동시에 변환해도 결과가 같다(공유되는 가변 상태가 없다).
     *
     * @throws Exception if a thread is interrupted / 스레드가 중단되면
     */
    public void testThreadSafety() throws Exception {
        final String md = "# T\n\n* a\n* **b** [c](http://x.test/?a=1&b=2)\n\n| h |\n|---|\n| v |\n\nwww.example.com and a@b.cd\n";
        final String expected = MarkdownConverter.toHtml(md);
        final AtomicInteger bad = new AtomicInteger();
        Thread[] ts = new Thread[8];
        // 스레드마다 같은 문서를 여러 번 변환해 결과를 비교한다.
        for (int t = 0; t < ts.length; t++) {
            ts[t] = new Thread(new Runnable() {
                /**
                 * Converts the document repeatedly.
                 * <p>
                 * 문서를 되풀이하여 변환한다.
                 */
                public void run() {
                    // 같은 문서를 되풀이해 변환한다.
                    for (int i = 0; i < 200; i++) {
                        if (!expected.equals(MarkdownConverter.toHtml(md))) {
                            bad.incrementAndGet();
                        }
                    }
                }
            });
            ts[t].start();
        }
        // 모든 스레드가 끝나기를 기다린다.
        for (int t = 0; t < ts.length; t++) {
            ts[t].join(60000);
        }
        Check.equal(0, bad.get(), "all conversions gave the same result");
    }

    /**
     * Converts a text and fails if it takes too long.
     * <p>
     * 텍스트를 변환하며 너무 오래 걸리면 실패한다.
     *
     * @param md the Markdown / Markdown
     * @param o  the options / 옵션
     * @return the HTML / HTML
     */
    private static String timed(String md, MarkdownOptions o) {
        long t0 = System.currentTimeMillis();
        String h = MarkdownConverter.toHtml(md, o);
        long ms = System.currentTimeMillis() - t0;
        Check.isTrue(ms < LIMIT_MILLIS, "conversion took " + ms + " ms for an input of " + md.length() + " characters starting with "
                + md.substring(0, Math.min(20, md.length())));
        return h;
    }

    /**
     * Counts the occurrences of a substring.
     * <p>
     * 부분 문자열이 나타나는 횟수를 센다.
     *
     * @param s   the text / 텍스트
     * @param sub the substring / 부분 문자열
     * @return the count / 횟수
     */
    private static int count(String s, String sub) {
        int n = 0;
        int i = 0;
        // 겹치지 않게 부분 문자열을 계속 찾는다.
        while ((i = s.indexOf(sub, i)) >= 0) {
            n++;
            i += sub.length();
        }
        return n;
    }
}
