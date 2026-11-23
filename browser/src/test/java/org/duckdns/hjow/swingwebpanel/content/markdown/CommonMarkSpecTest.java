package org.duckdns.hjow.swingwebpanel.content.markdown;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Runs every example of the CommonMark 0.31.2 specification (<code>spec.json</code>, 652 examples) and the examples of the GFM extensions
 * (tables, strikethrough, extended autolinks, tag filter) of the GitHub Flavored Markdown specification 0.29 through the converter and
 * compares the HTML literally (plan items P17-03, P17-04). The data files are in <code>tests/resources/org/duckdns/hjow/swingwebpanel/markdown</code>.
 * <p>
 * CommonMark 0.31.2 규격의 모든 예제(<code>spec.json</code>, 652개)와 GitHub Flavored Markdown 규격 0.29 의 확장(표, 취소선, 확장 자동 링크,
 * 태그 필터) 예제를 변환기에 넣고 HTML 을 글자 그대로 비교한다(계획 P17-03, P17-04). 자료 파일은
 * <code>tests/resources/org/duckdns/hjow/swingwebpanel/markdown</code> 에 있다.
 */
public class CommonMarkSpecTest {

    /** One example of a specification. 규격의 예제 하나. */
    private static final class Example {
        /** Number within its file. 파일 안의 번호. */
        int number;

        /** Section name. 절 이름. */
        String section;

        /** Markdown input. Markdown 입력. */
        String markdown;

        /** Expected HTML. 기대하는 HTML. */
        String html;

        /** Tag of a GFM example such as table. GFM 예제의 태그(예: table). */
        String tag = "";
    }

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CommonMarkSpecTest() {
    }

    /**
     * All examples of CommonMark 0.31.2 give the expected HTML (plain CommonMark: no extensions, raw HTML passed through).
     * <p>
     * CommonMark 0.31.2 의 모든 예제가 기대한 HTML 을 낸다(순수 CommonMark: 확장 없음, 원시 HTML 통과).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAllCommonMarkExamples() throws Exception {
        List<Example> examples = loadJson("/org/duckdns/hjow/swingwebpanel/markdown/commonmark-0.31.2-spec.json");
        Check.isTrue(examples.size() == 652, "spec.json has 652 examples but " + examples.size() + " were read");
        MarkdownOptions o = new MarkdownOptions().setGfm(false).setAllowRawHtml(true).setNeutralizeUnsafeUrls(false);
        StringBuilder failures = new StringBuilder();
        int failed = 0;
        // 예제마다 변환하여 기대 출력과 비교한다.
        for (int i = 0; i < examples.size(); i++) {
            Example e = examples.get(i);
            String actual;
            try {
                actual = MarkdownConverter.toHtml(e.markdown, o);
            } catch (RuntimeException ex) {
                actual = "EXCEPTION " + ex;
            }
            if (!e.html.equals(actual)) {
                failed++;
                if (failed <= 12) {
                    failures.append("\n#").append(e.number).append(" [").append(e.section).append("]\n--- markdown:\n").append(show(e.markdown))
                            .append("--- expected:\n").append(show(e.html)).append("--- actual:\n").append(show(actual));
                }
            }
        }
        if (failed > 0) {
            Check.fail(failed + " of " + examples.size() + " CommonMark examples failed (first ones):" + failures);
        }
    }

    /**
     * The tagged GFM extension examples (tables, strikethrough, autolinks, tag filter) give the expected HTML.
     * <p>
     * 태그가 붙은 GFM 확장 예제(표, 취소선, 자동 링크, 태그 필터)가 기대한 HTML 을 낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGfmExtensionExamples() throws Exception {
        List<Example> examples = loadGfm("/org/duckdns/hjow/swingwebpanel/markdown/gfm-0.29-spec.txt");
        int run = 0;
        StringBuilder failures = new StringBuilder();
        int failed = 0;
        // 확장 태그가 붙은 예제(disabled 제외)만 GFM 옵션으로 변환해 비교한다.
        for (int i = 0; i < examples.size(); i++) {
            Example e = examples.get(i);
            if (e.tag.length() == 0 || "disabled".equals(e.tag)) {
                continue;
            }
            run++;
            MarkdownOptions o = new MarkdownOptions().setGfm(true).setAllowRawHtml("tagfilter".equals(e.tag));
            String actual = MarkdownConverter.toHtml(e.markdown, o);
            if (!e.html.equals(actual)) {
                failed++;
                failures.append("\n#").append(e.number).append(" [").append(e.section).append("/").append(e.tag).append("]\n--- markdown:\n")
                        .append(show(e.markdown)).append("--- expected:\n").append(show(e.html)).append("--- actual:\n").append(show(actual));
            }
        }
        Check.isTrue(run == 22, "22 tagged GFM examples expected but " + run + " were run");
        if (failed > 0) {
            Check.fail(failed + " of " + run + " GFM extension examples failed:" + failures);
        }
    }

    /**
     * Task list items (the GFM examples are marked disabled because their attribute order differs) render a disabled checkbox.
     * <p>
     * 작업 목록 항목(GFM 예제는 속성 순서가 달라 disabled 로 표시되어 있다)은 비활성 체크박스를 낸다.
     */
    public void testTaskListItems() {
        MarkdownOptions o = new MarkdownOptions();
        Check.equal("<ul>\n<li><input type=\"checkbox\" disabled=\"\" /> foo</li>\n<li><input type=\"checkbox\" checked=\"\" disabled=\"\" /> bar</li>\n</ul>\n",
                MarkdownConverter.toHtml("- [ ] foo\n- [x] bar\n", o), "simple task list");
        Check.equal("<ul>\n<li><input type=\"checkbox\" checked=\"\" disabled=\"\" /> foo\n<ul>\n<li><input type=\"checkbox\" disabled=\"\" /> bar</li>\n"
                + "<li><input type=\"checkbox\" checked=\"\" disabled=\"\" /> baz</li>\n</ul>\n</li>\n<li><input type=\"checkbox\" disabled=\"\" /> bim</li>\n</ul>\n",
                MarkdownConverter.toHtml("- [x] foo\n  - [ ] bar\n  - [x] baz\n- [ ] bim\n", o), "nested task list");
        Check.equal("<ul>\n<li>[ ]foo</li>\n<li>[y] foo</li>\n</ul>\n", MarkdownConverter.toHtml("- [ ]foo\n- [y] foo\n", o), "not a task marker");
        Check.equal("<ul>\n<li>[ ] foo</li>\n</ul>\n", MarkdownConverter.toHtml("- [ ] foo\n", new MarkdownOptions().setGfm(false)), "plain CommonMark keeps the text");
        Check.equal("<ol>\n<li>\n<p><input type=\"checkbox\" disabled=\"\" /> a</p>\n</li>\n<li>\n<p>b</p>\n</li>\n</ol>\n",
                MarkdownConverter.toHtml("1. [ ] a\n\n2. b\n", o), "loose list keeps the paragraph");
    }

    /**
     * Shows a text with visible line ends for failure messages.
     * <p>
     * 실패 메시지를 위해 줄 끝이 보이게 텍스트를 보여 준다.
     *
     * @param s the text / 텍스트
     * @return printable text / 출력용 텍스트
     */
    private static String show(String s) {
        return s.replace("\t", "<TAB>").replace("\n", "$\n") + "\n";
    }

    /**
     * Reads a resource completely as UTF-8.
     * <p>
     * 리소스 전체를 UTF-8 로 읽는다.
     *
     * @param name resource name / 리소스 이름
     * @return the text / 텍스트
     * @throws IOException if the resource is missing or unreadable / 리소스가 없거나 읽을 수 없으면
     */
    private static String readResource(String name) throws IOException {
        InputStream in = CommonMarkSpecTest.class.getResourceAsStream(name);
        if (in == null) {
            throw new IOException("missing test resource " + name);
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 리소스 끝까지 읽는다.
            while ((n = in.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    /**
     * Loads the examples of the CommonMark <code>spec.json</code> with a small purpose-made JSON reader.
     * <p>
     * CommonMark <code>spec.json</code> 의 예제를 이 용도로 만든 작은 JSON 읽기로 읽는다.
     *
     * @param name resource name / 리소스 이름
     * @return the examples / 예제
     * @throws IOException if unreadable / 읽을 수 없으면
     */
    private static List<Example> loadJson(String name) throws IOException {
        String s = readResource(name);
        List<Example> list = new ArrayList<Example>();
        int[] pos = { 0 };
        skipWs(s, pos);
        expect(s, pos, '[');
        // 배열의 객체를 하나씩 읽는다.
        while (true) {
            skipWs(s, pos);
            if (s.charAt(pos[0]) == ']') {
                break;
            }
            if (s.charAt(pos[0]) == ',') {
                pos[0]++;
                continue;
            }
            expect(s, pos, '{');
            Example e = new Example();
            // 객체의 키-값 쌍을 읽는다.
            while (true) {
                skipWs(s, pos);
                if (s.charAt(pos[0]) == '}') {
                    pos[0]++;
                    break;
                }
                if (s.charAt(pos[0]) == ',') {
                    pos[0]++;
                    continue;
                }
                String key = readString(s, pos);
                skipWs(s, pos);
                expect(s, pos, ':');
                skipWs(s, pos);
                if (s.charAt(pos[0]) == '"') {
                    String v = readString(s, pos);
                    if ("markdown".equals(key)) {
                        e.markdown = v;
                    } else if ("html".equals(key)) {
                        e.html = v;
                    } else if ("section".equals(key)) {
                        e.section = v;
                    }
                } else {
                    int start = pos[0];
                    // 숫자 문자를 읽는다.
                    while (pos[0] < s.length() && (Character.isDigit(s.charAt(pos[0])) || s.charAt(pos[0]) == '-')) {
                        pos[0]++;
                    }
                    if ("example".equals(key)) {
                        e.number = Integer.parseInt(s.substring(start, pos[0]));
                    }
                }
            }
            list.add(e);
        }
        return list;
    }

    /**
     * Skips JSON whitespace.
     * <p>
     * JSON 공백을 건너뛴다.
     *
     * @param s   text / 텍스트
     * @param pos position holder / 위치 보관
     */
    private static void skipWs(String s, int[] pos) {
        // 공백 문자를 건너뛴다.
        while (pos[0] < s.length() && Character.isWhitespace(s.charAt(pos[0]))) {
            pos[0]++;
        }
    }

    /**
     * Consumes an expected character.
     * <p>
     * 기대한 문자를 소비한다.
     *
     * @param s   text / 텍스트
     * @param pos position holder / 위치 보관
     * @param c   expected character / 기대한 문자
     * @throws IOException if another character is found / 다른 문자가 있으면
     */
    private static void expect(String s, int[] pos, char c) throws IOException {
        if (s.charAt(pos[0]) != c) {
            throw new IOException("expected '" + c + "' at " + pos[0]);
        }
        pos[0]++;
    }

    /**
     * Reads a JSON string at the position.
     * <p>
     * 위치의 JSON 문자열을 읽는다.
     *
     * @param s   text / 텍스트
     * @param pos position holder / 위치 보관
     * @return the string value / 문자열 값
     * @throws IOException if malformed / 형식이 잘못되었으면
     */
    private static String readString(String s, int[] pos) throws IOException {
        expect(s, pos, '"');
        StringBuilder sb = new StringBuilder();
        // 닫는 따옴표까지 문자를 읽으며 이스케이프를 해석한다.
        while (true) {
            char c = s.charAt(pos[0]++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char d = s.charAt(pos[0]++);
            switch (d) {
                case 'n':
                    sb.append('\n');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'u':
                    sb.append((char) Integer.parseInt(s.substring(pos[0], pos[0] + 4), 16));
                    pos[0] += 4;
                    break;
                default:
                    sb.append(d);
                    break;
            }
        }
    }

    /**
     * Loads the examples of the GFM <code>spec.txt</code>: blocks between a line that starts with 32 backticks and <code>example</code>
     * and the closing 32 backticks, the Markdown and the HTML separated by a line with a single period, and the arrow character standing
     * for a tab.
     * <p>
     * GFM <code>spec.txt</code> 의 예제를 읽는다: 백틱 32개와 <code>example</code> 로 시작하는 줄부터 닫는 백틱 32개까지의 블록이며, Markdown 과
     * HTML 은 마침표 한 글자뿐인 줄로 나뉘고 화살표 문자는 탭을 뜻한다.
     *
     * @param name resource name / 리소스 이름
     * @return the examples with their section name and tag / 절 이름과 태그가 있는 예제
     * @throws IOException if unreadable / 읽을 수 없으면
     */
    private static List<Example> loadGfm(String name) throws IOException {
        String text = readResource(name);
        BufferedReader r = new BufferedReader(new InputStreamReader(new java.io.ByteArrayInputStream(text.getBytes("UTF-8")), "UTF-8"));
        List<Example> list = new ArrayList<Example>();
        String fence = "````````````````````````````````";
        String section = "";
        String line;
        int number = 0;
        // 줄을 읽으며 절 제목과 예제 블록을 찾는다.
        while ((line = r.readLine()) != null) {
            if (line.startsWith("## ")) {
                section = line.substring(3);
            } else if (line.startsWith(fence + " example")) {
                Example e = new Example();
                e.number = ++number;
                e.section = section;
                e.tag = line.substring((fence + " example").length()).trim();
                StringBuilder md = new StringBuilder();
                StringBuilder html = new StringBuilder();
                boolean inHtml = false;
                // 닫는 울타리까지 Markdown 부분과 HTML 부분을 모은다.
                while ((line = r.readLine()) != null && !line.equals(fence)) {
                    if (!inHtml && line.equals(".")) {
                        inHtml = true;
                    } else {
                        (inHtml ? html : md).append(line.replace('→', '\t')).append('\n');
                    }
                }
                e.markdown = md.toString();
                e.html = html.toString();
                list.add(e);
            }
        }
        return list;
    }
}
