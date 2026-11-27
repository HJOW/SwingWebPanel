package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Saving the received text of a page into a file (plan item P31-05): the name that is suggested to the user and the bytes that are
 * written. The text is written as UTF-8. HTML and XHTML start with a byte order mark, because the text may declare another character set
 * (the one it arrived in) and the mark takes precedence over that declaration when the file is opened again.
 * <p>
 * 페이지의 수신 원문을 파일에 저장하는 일(계획 P31-05): 사용자에게 제안하는 이름과 쓰는 바이트. 원문은 UTF-8 로 쓴다. HTML 과 XHTML 은 바이트 순서
 * 표시로 시작한다. 원문이 다른 문자셋(도착했을 때의 문자셋)을 선언하고 있을 수 있는데, 파일을 다시 열 때 이 표시가 그 선언보다 우선하기 때문이다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * String name = PageFiles.suggestName(panel.getPage(), panel.getMimeType());
 * PageFiles.write(new File(dir, name), panel.getContent(), panel.getMimeType());
 * </pre>
 */
public final class PageFiles {

    /** The name that is suggested when the address gives none. / 주소에서 이름을 얻을 수 없을 때 제안하는 이름. */
    private static final String DEFAULT_NAME = "page";

    /** The characters that a file name must not contain on the common file systems. / 흔한 파일 시스템에서 파일 이름에 쓸 수 없는 문자. */
    private static final String FORBIDDEN = "\\/:*?\"<>|";

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private PageFiles() {
    }

    /**
     * Returns the file extension for a MIME type.
     * <p>
     * MIME 타입에 맞는 파일 확장자를 반환한다.
     *
     * @param mime the MIME type of the page, or null / 페이지의 MIME 타입 또는 null
     * @return the extension with its dot / 점이 붙은 확장자
     */
    public static String extensionOf(String mime) {
        String m = mime == null ? "" : mime.trim().toLowerCase();
        int semi = m.indexOf(';');
        if (semi >= 0) {
            m = m.substring(0, semi).trim();
        }
        // 컴포넌트가 표시하는 네 가지 문서 형식과 그 밖의 텍스트를 구분한다.
        if ("application/xhtml+xml".equals(m)) {
            return ".xhtml";
        }
        if ("text/markdown".equals(m)) {
            return ".md";
        }
        if ("text/plain".equals(m)) {
            return ".txt";
        }
        return ".html";
    }

    /**
     * Tells whether a page of this MIME type is written with a byte order mark.
     * <p>
     * 이 MIME 타입의 페이지를 바이트 순서 표시와 함께 쓰는지 알려 준다.
     *
     * @param mime the MIME type of the page, or null / 페이지의 MIME 타입 또는 null
     * @return true for HTML and XHTML / HTML 과 XHTML 이면 true
     */
    public static boolean needsByteOrderMark(String mime) {
        String ext = extensionOf(mime);
        return ".html".equals(ext) || ".xhtml".equals(ext);
    }

    /**
     * Suggests a file name for a page: the last part of the path of its address, without characters that file names cannot have, and
     * with an extension for the MIME type when that part has none.
     * <p>
     * 페이지의 파일 이름을 제안한다: 주소 경로의 마지막 부분에서 파일 이름에 쓸 수 없는 문자를 빼고, 그 부분에 확장자가 없으면 MIME 타입에 맞는
     * 확장자를 붙인다.
     *
     * @param url  the address of the page, or null / 페이지의 주소 또는 null
     * @param mime the MIME type of the page, or null / 페이지의 MIME 타입 또는 null
     * @return the suggested name; never empty / 제안하는 이름. 비어 있지 않다
     */
    public static String suggestName(String url, String mime) {
        String path = url == null ? "" : url;
        int cut = path.indexOf('#');
        if (cut >= 0) {
            path = path.substring(0, cut);
        }
        cut = path.indexOf('?');
        if (cut >= 0) {
            path = path.substring(0, cut);
        }
        // 스킴과 호스트를 떼어 경로만 남긴다. 경로가 없는 주소(about:blank 등)는 이름을 주지 못한다.
        int scheme = path.indexOf("://");
        if (scheme >= 0) {
            int slash = path.indexOf('/', scheme + 3);
            path = slash < 0 ? "" : path.substring(slash);
        } else if (path.indexOf(':') >= 0 && path.indexOf('/') < 0) {
            path = "";
        }
        int last = path.lastIndexOf('/');
        String name = clean(decode(last >= 0 ? path.substring(last + 1) : path));
        if (name.length() == 0) {
            name = DEFAULT_NAME;
        }
        // 확장자가 없는 이름에는 형식에 맞는 확장자를 붙인다.
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            name = (dot == name.length() - 1 && dot >= 0 ? name.substring(0, dot) : name) + extensionOf(mime);
        }
        return name;
    }

    /**
     * Decodes the percent escapes of a path segment as UTF-8; a segment with a broken escape is returned as it is.
     * <p>
     * 경로 조각의 퍼센트 이스케이프를 UTF-8 로 푼다. 이스케이프가 깨진 조각은 그대로 반환한다.
     *
     * @param segment the path segment / 경로 조각
     * @return the decoded text / 푼 글
     */
    private static String decode(String segment) {
        if (segment.indexOf('%') < 0) {
            return segment;
        }
        try {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            // 문자마다 이스케이프이면 바이트로, 아니면 그 문자의 UTF-8 바이트로 옮긴다.
            for (int i = 0; i < segment.length(); i++) {
                char c = segment.charAt(i);
                if (c == '%') {
                    if (i + 2 >= segment.length()) {
                        return segment;
                    }
                    bytes.write(Integer.parseInt(segment.substring(i + 1, i + 3), 16));
                    i += 2;
                } else {
                    bytes.write(String.valueOf(c).getBytes("UTF-8"));
                }
            }
            return new String(bytes.toByteArray(), "UTF-8");
        } catch (NumberFormatException e) {
            return segment;
        } catch (IOException e) {
            return segment;
        }
    }

    /**
     * Removes the characters that a file name cannot have.
     * <p>
     * 파일 이름에 쓸 수 없는 문자를 뺀다.
     *
     * @param name the raw name / 원래 이름
     * @return the cleaned name, possibly empty / 정리한 이름. 비어 있을 수 있다
     */
    private static String clean(String name) {
        StringBuilder out = new StringBuilder();
        // 제어 문자와 금지 문자를 빼고 나머지를 옮긴다.
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= ' ' && FORBIDDEN.indexOf(c) < 0) {
                out.append(c);
            }
        }
        String t = out.toString().trim();
        // 점만 있는 이름("." 과 "..")은 디렉터리를 가리키므로 버린다.
        while (t.startsWith(".")) {
            t = t.substring(1);
        }
        return t;
    }

    /**
     * Writes the text of a page into a file as UTF-8, with a byte order mark for HTML and XHTML. An existing file is replaced.
     * <p>
     * 페이지의 원문을 UTF-8 로 파일에 쓴다. HTML 과 XHTML 은 바이트 순서 표시를 붙인다. 이미 있는 파일은 바꾼다.
     *
     * @param file    the file to write / 쓸 파일
     * @param content the received text of the page / 페이지의 수신 원문
     * @param mime    the MIME type of the page, or null / 페이지의 MIME 타입 또는 null
     * @throws IOException if the file cannot be written / 파일을 쓸 수 없으면
     */
    public static void write(File file, String content, String mime) throws IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            String text = content == null ? "" : content;
            // 원문이 이미 표시 문자로 시작하면 두 번 쓰지 않는다.
            if (needsByteOrderMark(mime) && !(text.length() > 0 && text.charAt(0) == (char) 0xFEFF)) {
                out.write(0xEF);
                out.write(0xBB);
                out.write(0xBF);
            }
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }
}
