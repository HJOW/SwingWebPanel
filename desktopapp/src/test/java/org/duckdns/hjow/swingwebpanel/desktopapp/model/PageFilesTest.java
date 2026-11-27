package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Saving a page into a file and the zoom steps (plan item P31-05): the suggested file names, the bytes that are written, and the steps
 * and percentages of the zoom commands.
 * <p>
 * 페이지를 파일에 저장하는 일과 확대 단계(계획 P31-05): 제안하는 파일 이름, 쓰는 바이트, 확대 명령의 단계와 백분율.
 */
public class PageFilesTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PageFilesTest() {
    }

    /**
     * The suggested name is the last part of the path, cleaned, with an extension for the MIME type when it has none.
     * <p>
     * 제안하는 이름은 경로의 마지막 부분을 정리한 것이고, 확장자가 없으면 MIME 타입에 맞는 확장자를 붙인다.
     */
    public void testSuggestedNames() {
        Check.equal("index.html", PageFiles.suggestName("http://example.com/a/index.html?x=1#top", "text/html"), "the last path part");
        Check.equal("page.html", PageFiles.suggestName("http://example.com/", "text/html"), "a directory address");
        Check.equal("page.html", PageFiles.suggestName("http://example.com", null), "an address without a path");
        Check.equal("page.html", PageFiles.suggestName("about:blank", "text/html"), "the empty page");
        Check.equal("page.txt", PageFiles.suggestName(null, "text/plain; charset=utf-8"), "no address");
        Check.equal("notes.md", PageFiles.suggestName("https://example.com/docs/notes", "Text/Markdown"), "an extension for markdown");
        Check.equal("doc.xhtml", PageFiles.suggestName("https://example.com/doc.", "application/xhtml+xml"), "a trailing dot is replaced");
        Check.equal("report.php", PageFiles.suggestName("https://example.com/report.php", "text/html"), "an existing extension is kept");
        Check.equal("한글 문서.html", PageFiles.suggestName("file:///C:/docs/%ED%95%9C%EA%B8%80%20%EB%AC%B8%EC%84%9C.html", "text/html"),
                "percent escapes are decoded");
        Check.equal("ab.html", PageFiles.suggestName("http://example.com/a%3Ab", "text/html"), "forbidden characters are removed");
        Check.equal("100%.html", PageFiles.suggestName("http://example.com/100%", "text/html"), "a broken escape is kept");
        Check.equal("index.html", PageFiles.suggestName("classpath://www/index.html", "text/html"), "a classpath address");
    }

    /**
     * HTML and XHTML are written with a byte order mark, other text without; a mark that the text already has is not doubled.
     * <p>
     * HTML 과 XHTML 은 바이트 순서 표시와 함께, 그 밖의 글은 표시 없이 쓴다. 원문에 이미 있는 표시는 두 번 쓰지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWrittenBytes() throws Exception {
        File f = File.createTempFile("swp-pagefiles", ".tmp");
        try {
            PageFiles.write(f, "<p>가</p>", "text/html");
            Check.equal("EFBBBF3C703EEAB0803C2F703E", hex(f), "HTML starts with the mark");
            PageFiles.write(f, "\uFEFF<p>", "application/xhtml+xml");
            Check.equal("EFBBBF3C703E", hex(f), "a mark of the text is not doubled");
            PageFiles.write(f, "가", "text/plain");
            Check.equal("EAB080", hex(f), "plain text has no mark");
            PageFiles.write(f, null, "text/markdown");
            Check.equal("", hex(f), "no text gives an empty file");
        } finally {
            f.delete();
        }
        Check.isTrue(PageFiles.needsByteOrderMark(null), "an unknown type is saved as HTML");
        Check.isFalse(PageFiles.needsByteOrderMark("text/markdown"), "markdown has no mark");
    }

    /**
     * Returns the bytes of a file as hexadecimal digits.
     * <p>
     * 파일의 바이트를 16진수 숫자로 반환한다.
     *
     * @param f the file / 파일
     * @return the digits / 숫자
     * @throws Exception on failure / 실패 시
     */
    private static String hex(File f) throws Exception {
        InputStream in = new FileInputStream(f);
        try {
            StringBuilder out = new StringBuilder();
            int c;
            // 바이트마다 두 자리 16진수로 옮긴다.
            while ((c = in.read()) >= 0) {
                out.append(Integer.toHexString(0x100 | c).substring(1).toUpperCase());
            }
            return out.toString();
        } finally {
            in.close();
        }
    }

    /**
     * The zoom commands step through fixed factors and stop at the ends; percentages are read and written.
     * <p>
     * 확대 명령은 정해진 배율을 거치고 끝에서 멈춘다. 백분율을 읽고 쓴다.
     */
    public void testZoomSteps() {
        Check.near(1.1, ZoomSteps.larger(1.0), 0.0001, "one step up from 100%");
        Check.near(0.9, ZoomSteps.smaller(1.0), 0.0001, "one step down from 100%");
        Check.near(1.25, ZoomSteps.larger(1.2), 0.0001, "the next step above a value between steps");
        Check.near(1.1, ZoomSteps.smaller(1.2), 0.0001, "the next step below a value between steps");
        Check.near(5.0, ZoomSteps.larger(5.0), 0.0001, "the largest step stays");
        Check.near(0.25, ZoomSteps.smaller(0.25), 0.0001, "the smallest step stays");
        Check.near(1.25, ZoomSteps.parsePercent(" 125% ", 1.0), 0.0001, "a percentage with a sign");
        Check.near(0.5, ZoomSteps.parsePercent("50", 1.0), 0.0001, "a percentage without a sign");
        Check.near(5.0, ZoomSteps.parsePercent("900", 1.0), 0.0001, "too large is cut");
        Check.near(1.0, ZoomSteps.parsePercent("big", 1.0), 0.0001, "text gives the fallback");
        Check.near(1.0, ZoomSteps.parsePercent("-5", 1.0), 0.0001, "a negative value gives the fallback");
        Check.near(1.0, ZoomSteps.parsePercent(null, 1.0), 0.0001, "nothing gives the fallback");
        Check.equal("125", ZoomSteps.toPercent(1.25), "a factor as a percentage");
        Check.equal("33", ZoomSteps.toPercent(0.33), "a rounded percentage");
    }
}
