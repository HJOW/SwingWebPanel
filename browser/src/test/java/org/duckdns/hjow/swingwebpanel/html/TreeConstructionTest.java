package org.duckdns.hjow.swingwebpanel.html;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Data driven tests of the tree construction (plan items P07-05, P07-06, P07-07, P07-09): every case of the files
 * <code>tests/resources/html/tree/*.dat</code> is parsed and its DOM dump ({@link TreeDump}) is compared with the expected tree. The
 * case files use the notation of the html5lib tree construction tests, written for this project: a case is
 * <pre>
 * #data
 * the markup on ONE line (\n = line feed, \t = tab, \0 = NUL, \\ = backslash)
 * #fragment
 * context element name, for example tr or "svg path" (optional)
 * #script-off
 * (optional line: scripting disabled, so noscript is parsed as markup)
 * #document
 * | &lt;html&gt;
 * |   &lt;head&gt;
 * ...
 * </pre>
 * A blank line ends a case. All failures of a file are collected and reported together.
 * <p>
 * 트리 생성의 자료 기반 시험(계획 P07-05, P07-06, P07-07, P07-09): <code>tests/resources/html/tree/*.dat</code> 의 모든 사례를 파싱해 DOM 덤프
 * ({@link TreeDump})를 기대하는 트리와 비교한다. 사례 파일은 이 프로젝트를 위해 쓴 html5lib 트리 생성 시험 표기를 쓴다. 빈 줄이 사례를 끝낸다. 한 파일의
 * 모든 실패를 모아 한꺼번에 보고한다.
 */
public class TreeConstructionTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TreeConstructionTest() {
    }

    /**
     * Reads a resource file as UTF-8 lines.
     * <p>
     * 자원 파일을 UTF-8 줄로 읽는다.
     *
     * @param name resource name below <code>/html/tree/</code> / <code>/html/tree/</code> 아래의 자원 이름
     * @return the lines / 줄
     * @throws IOException if the resource is missing or unreadable / 자원이 없거나 읽을 수 없을 때
     */
    static List<String> readLines(String name) throws IOException {
        InputStream in = TreeConstructionTest.class.getResourceAsStream("/html/tree/" + name);
        if (in == null) {
            throw new IOException("missing test resource /html/tree/" + name);
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        List<String> lines = new ArrayList<String>();
        try {
            String line;
            // 파일의 모든 줄을 읽는다.
            while ((line = r.readLine()) != null) {
                lines.add(line);
            }
        } finally {
            r.close();
        }
        return lines;
    }

    /**
     * Decodes the escapes of a data line.
     * <p>
     * 데이터 줄의 이스케이프를 푼다.
     *
     * @param s data line / 데이터 줄
     * @return the markup / 마크업
     */
    static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        // 역슬래시 뒤의 문자를 해당 제어 문자로 바꾼다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char d = s.charAt(++i);
                sb.append(d == 'n' ? '\n' : d == 't' ? '\t' : d == '0' ? (char) 0 : d == 'r' ? '\r' : d == 'f' ? '\f' : d);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Runs all cases of a file and fails once with the list of all wrong cases.
     * <p>
     * 파일의 모든 사례를 실행하고 틀린 사례의 목록과 함께 한 번 실패한다.
     *
     * @param name file name / 파일 이름
     * @return the number of cases / 사례 수
     * @throws IOException if the file cannot be read / 파일을 읽을 수 없을 때
     */
    static int runFile(String name) throws IOException {
        List<String> lines = readLines(name);
        StringBuilder failures = new StringBuilder();
        int cases = 0;
        int i = 0;
        // 줄을 훑으며 #data 로 시작하는 사례를 찾아 실행한다.
        while (i < lines.size()) {
            if (!lines.get(i).equals("#data")) {
                i++;
                continue;
            }
            String rawData = lines.get(i + 1);
            String data = unescape(rawData);
            i += 2;
            String fragment = null;
            boolean scripting = true;
            // #document 가 나올 때까지 선택 지시어를 읽는다.
            while (!lines.get(i).equals("#document")) {
                if (lines.get(i).equals("#fragment")) {
                    fragment = lines.get(i + 1);
                    i++;
                } else if (lines.get(i).equals("#script-off")) {
                    scripting = false;
                }
                i++;
            }
            i++;
            StringBuilder expected = new StringBuilder();
            // 빈 줄이나 파일 끝까지가 기대하는 트리이다.
            while (i < lines.size() && lines.get(i).startsWith("|")) {
                expected.append(lines.get(i)).append('\n');
                i++;
            }
            cases++;
            String actual;
            try {
                actual = parse(data, fragment, scripting);
            } catch (RuntimeException ex) {
                actual = "EXCEPTION " + ex;
            }
            if (!actual.equals(expected.toString())) {
                failures.append("---- case ").append(cases).append(" data: ").append(rawData).append('\n').append("expected:\n").append(expected)
                        .append("actual:\n").append(actual);
            }
        }
        if (failures.length() > 0) {
            Check.fail(name + ": wrong cases (of " + cases + "):\n" + failures);
        }
        return cases;
    }

    /**
     * Parses markup and returns the dump of the result.
     * <p>
     * 마크업을 파싱해 결과의 덤프를 반환한다.
     *
     * @param data      markup / 마크업
     * @param fragment  context description or null for a whole document / 컨텍스트 설명. 문서 전체이면 null
     * @param scripting parser scripting flag / parser scripting 플래그
     * @return the dump / 덤프
     */
    static String parse(String data, String fragment, boolean scripting) {
        if (fragment == null) {
            Document doc = HtmlParser.parseDocument(data, null, null, scripting);
            return TreeDump.dumpChildren(doc);
        }
        Document doc = Document.createHtmlDocument();
        String ns = Namespaces.HTML;
        String name = fragment;
        if (fragment.startsWith("svg ")) {
            ns = Namespaces.SVG;
            name = fragment.substring(4);
        } else if (fragment.startsWith("math ")) {
            ns = Namespaces.MATHML;
            name = fragment.substring(5);
        }
        Element context = doc.createElementInternal(ns, null, name);
        DocumentFragment f = HtmlParser.parseFragment(context, data, scripting);
        return TreeDump.dumpChildren(f);
    }

    /**
     * The basic document structure and the implied elements.
     * <p>
     * 기본 문서 구조와 암시적 요소.
     *
     * @throws IOException on read errors / 읽기 오류 시
     */
    public void testBasics() throws IOException {
        Check.isTrue(runFile("basics.dat") > 20, "number of cases");
    }

    /**
     * Misnested formatting elements, the adoption agency algorithm, paragraphs, lists and other body rules.
     * <p>
     * 잘못 중첩된 서식 요소, adoption agency 알고리즘, 문단, 목록, 그 밖의 body 규칙.
     *
     * @throws IOException on read errors / 읽기 오류 시
     */
    public void testBody() throws IOException {
        Check.isTrue(runFile("body.dat") > 30, "number of cases");
    }

    /**
     * Tables: implied sections, foster parenting, captions, column groups, cells and forms in tables.
     * <p>
     * 표: 암시적 구역, foster parenting, caption, column group, 셀, 표 안의 폼.
     *
     * @throws IOException on read errors / 읽기 오류 시
     */
    public void testTables() throws IOException {
        Check.isTrue(runFile("tables.dat") > 25, "number of cases");
    }

    /**
     * Select, template, frameset, noscript, raw text elements and the head modes.
     * <p>
     * select, template, frameset, noscript, 원문 텍스트 요소, head 모드.
     *
     * @throws IOException on read errors / 읽기 오류 시
     */
    public void testSpecialElements() throws IOException {
        Check.isTrue(runFile("special.dat") > 25, "number of cases");
    }

    /**
     * SVG and MathML (foreign content): namespaces, attribute and tag name adjustment, integration points and breakout.
     * <p>
     * SVG 와 MathML(외부 콘텐츠): 네임스페이스, 속성과 태그 이름 보정, 통합 지점, 이탈.
     *
     * @throws IOException on read errors / 읽기 오류 시
     */
    public void testForeign() throws IOException {
        Check.isTrue(runFile("foreign.dat") > 20, "number of cases");
    }

    /**
     * Fragment parsing with different context elements.
     * <p>
     * 여러 컨텍스트 요소로 조각 파싱.
     *
     * @throws IOException on read errors / 읽기 오류 시
     */
    public void testFragments() throws IOException {
        Check.isTrue(runFile("fragments.dat") > 20, "number of cases");
    }

    /**
     * DOCTYPE, comments, character references, legacy markup and other document level cases.
     * <p>
     * DOCTYPE, 주석, 문자 참조, 옛 마크업, 그 밖의 문서 수준 사례.
     *
     * @throws IOException on read errors / 읽기 오류 시
     */
    public void testDocumentLevel() throws IOException {
        Check.isTrue(runFile("document.dat") > 20, "number of cases");
    }
}
