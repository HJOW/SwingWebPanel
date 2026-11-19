package org.duckdns.hjow.swingwebpanel.download;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the file name choice of downloads (plan item P11-09): Content-Disposition forms, the URL fallback, Korean names, and the
 * removal of everything that could make a download write outside the chosen directory.
 * <p>
 * 다운로드 파일 이름 결정 시험(계획 P11-09): Content-Disposition 의 여러 형태, URL 대체, 한글 이름, 다운로드가 선택한 디렉터리 밖에 쓰게 만들 수 있는
 * 모든 것의 제거.
 */
public class DownloadFileNamesTest {

    /** Korean text "hangul" used in several cases. 여러 사례에서 쓰는 한글 텍스트. */
    private static final String KOREAN = "\uD55C\uAE00";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DownloadFileNamesTest() {
    }

    /**
     * Checks one choice.
     * <p>
     * 선택 하나를 확인한다.
     *
     * @param expected the expected name / 기대하는 이름
     * @param header   the Content-Disposition value / Content-Disposition 값
     * @param url      the URL / URL
     */
    private static void is(String expected, String header, String url) {
        Check.equal(expected, DownloadFileNames.choose(header, url), "choose(" + header + ", " + url + ")");
    }

    /**
     * The forms of the Content-Disposition header.
     * <p>
     * Content-Disposition 헤더의 여러 형태.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testContentDispositionForms() throws Exception {
        is("report.pdf", "attachment; filename=\"report.pdf\"", "http://h/x");
        is("report.pdf", "attachment; filename=report.pdf", "http://h/x");
        is("report.pdf", "ATTACHMENT; FileName = \"report.pdf\" ; size=3", "http://h/x");
        is("a;b.txt", "attachment; filename=\"a;b.txt\"; size=1", "http://h/x");
        is("a_b.txt", "attachment; filename=\"a\\\"b.txt\"", "http://h/x");
        is(KOREAN + ".txt", "attachment; filename*=UTF-8''%ED%95%9C%EA%B8%80.txt", "http://h/x");
        is(KOREAN + ".txt", "attachment; filename=\"fallback.txt\"; filename*=UTF-8'ko'%ED%95%9C%EA%B8%80.txt", "http://h/x");
        is("\u00E9.txt", "attachment; filename*=iso-8859-1''%E9.txt", "http://h/x");
        is("fallback.txt", "attachment; filename=\"fallback.txt\"; filename*=UTF-8''%ZZ.txt", "http://h/x");
        is("fallback.txt", "attachment; filename=\"fallback.txt\"; filename*=nonsense''x.txt", "http://h/x");
        is("fallback.txt", "attachment; filename=\"fallback.txt\"; filename*=UTF-8''%ED%95.txt", "http://h/x");
        String mojibake = new String((KOREAN + ".txt").getBytes("UTF-8"), "ISO-8859-1");
        is(KOREAN + ".txt", "attachment; filename=\"" + mojibake + "\"", "http://h/x");
        is("caf\u00E9.txt", "attachment; filename=\"caf\u00E9.txt\"", "http://h/x");
        is("file name.txt", "inline; filename=\"file name.txt\"", "http://h/x");
    }

    /**
     * Without a usable header name the last segment of the URL path is used.
     * <p>
     * 쓸 수 있는 헤더 이름이 없으면 URL 경로의 마지막 구간을 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUrlFallback() throws Exception {
        is("some file.txt", null, "http://h/dir/some%20file.txt?x=1#frag");
        is(KOREAN + ".bin", "attachment", "http://h/" + "%ED%95%9C%EA%B8%80" + ".bin");
        is("real.bin", "attachment; filename=\"..\"", "http://h/d/real.bin");
        is("real.bin", "attachment; filename=\"\"", "http://h/d/real.bin");
        is("file.zip", null, "ftp://h/pub/file.zip");
        is("download", null, "http://h/dir/");
        is("download", null, "http://h");
        is("download", null, "http://h/?a=b");
        is("a%zz.txt", null, "http://h/a%zz.txt");
        is("download", "attachment; filename=\"..\"", "http://h/dir/");
    }

    /**
     * Names that try to leave the directory, to use device names or to disguise the type are made harmless.
     * <p>
     * 디렉터리를 벗어나거나 장치 이름을 쓰거나 형식을 위장하려는 이름은 무해하게 바뀐다.
     */
    public void testMaliciousNames() {
        Check.equal("x", DownloadFileNames.sanitize("../../x"), "parent directories");
        Check.equal("x.txt", DownloadFileNames.sanitize("..\\..\\x.txt"), "Windows parent directories");
        Check.equal("x.txt", DownloadFileNames.sanitize("C:\\x.txt"), "drive letter with backslash");
        Check.equal("C_x.txt", DownloadFileNames.sanitize("C:x.txt"), "drive letter without separator");
        Check.equal("passwd", DownloadFileNames.sanitize("/etc/passwd"), "absolute path");
        Check.equal("x.txt", DownloadFileNames.sanitize("a/b/../x.txt"), "last segment");
        Check.equal("_CON.txt", DownloadFileNames.sanitize("CON.txt"), "device name with extension");
        Check.equal("_con", DownloadFileNames.sanitize("con"), "device name in lower case");
        Check.equal("_NUL.tar.gz", DownloadFileNames.sanitize("NUL.tar.gz"), "device name with two extensions");
        Check.equal("_COM1", DownloadFileNames.sanitize("COM1"), "COM1");
        Check.equal("_LPT9.x", DownloadFileNames.sanitize("LPT9.x"), "LPT9");
        Check.equal("COM10", DownloadFileNames.sanitize("COM10"), "COM10 is not reserved");
        Check.equal("CONSOLE.txt", DownloadFileNames.sanitize("CONSOLE.txt"), "names that only start with a device name");
        Check.equal("_aux", DownloadFileNames.sanitize("aux."), "device name with trailing dot");
        Check.equal("download", DownloadFileNames.sanitize(".."), "dots only");
        Check.equal("download", DownloadFileNames.sanitize("..."), "more dots");
        Check.equal("download", DownloadFileNames.sanitize(""), "empty");
        Check.equal("download", DownloadFileNames.sanitize("   "), "spaces");
        Check.equal("download", DownloadFileNames.sanitize(null), "null");
        Check.equal("a_b_c_d_e_f_g.txt", DownloadFileNames.sanitize("a<b>c:d|e?f*g.txt"), "characters Windows forbids");
        Check.equal("a_b", DownloadFileNames.sanitize("a" + (char) 1 + "b"), "control character");
        Check.equal("evil_fdp.exe", DownloadFileNames.sanitize("evil" + (char) 0x202E + "fdp.exe"), "right-to-left override");
        Check.equal("x.txt", DownloadFileNames.sanitize("x.txt. ."), "trailing dots and spaces");
        Check.equal("x.txt", DownloadFileNames.sanitize("  x.txt"), "leading spaces");
        Check.equal(".hidden", DownloadFileNames.sanitize(".hidden"), "a leading dot is allowed");
        Check.equal("a b.txt", DownloadFileNames.sanitize("a b.txt"), "inner spaces");
    }

    /**
     * Long names are shortened but keep their extension and are never cut inside a surrogate pair.
     * <p>
     * 긴 이름은 줄어들지만 확장자를 유지하고 서로게이트 쌍 가운데에서 잘리지 않는다.
     */
    public void testLongNames() {
        StringBuilder sb = new StringBuilder();
        // 300자의 이름을 만든다.
        for (int i = 0; i < 300; i++) {
            sb.append('a');
        }
        String n = DownloadFileNames.sanitize(sb + ".tar.gz");
        Check.equal(DownloadFileNames.MAX_LENGTH, n.length(), "the length is limited");
        Check.isTrue(n.endsWith(".gz"), "the extension is kept: " + n);
        StringBuilder emoji = new StringBuilder();
        // 서로게이트 쌍으로 이루어진 이름을 만든다.
        for (int i = 0; i < 100; i++) {
            emoji.appendCodePoint(0x1F600);
        }
        String e = DownloadFileNames.sanitize(emoji.toString());
        Check.isTrue(e.length() <= DownloadFileNames.MAX_LENGTH, "emoji name is limited");
        Check.isFalse(Character.isHighSurrogate(e.charAt(e.length() - 1)), "not cut inside a surrogate pair");
    }

    /**
     * The names of requests are derived the same way when a request is created.
     * <p>
     * 요청을 만들 때에도 이름은 같은 방식으로 정해진다.
     */
    public void testRequestUsesTheChoice() {
        DownloadRequest r = new DownloadRequest("http://h/a/b.bin", null, "application/octet-stream", "attachment; filename=\"../x.bin\"", 5, null, null);
        Check.equal("x.bin", r.getFileName(), "file name of the request");
        Check.equal("http://h/a/b.bin", r.getRequestedUrl(), "requested URL defaults to the final URL");
        Check.equal(5L, r.getLength(), "length");
    }
}
