package org.duckdns.hjow.swingwebpanel.net;

import java.util.HashMap;
import java.util.Map;

/**
 * Guesses a media type from a file name extension. Used for FTP files and class path resources, which have no
 * <code>Content-Type</code> header. Unknown extensions give <code>application/octet-stream</code>, which the content layer
 * treats as "not displayable" (download).
 * <p>
 * 파일 이름 확장자로 미디어 타입을 추정한다. <code>Content-Type</code> 헤더가 없는 FTP 파일과 클래스패스 리소스에 쓴다.
 * 알 수 없는 확장자는 <code>application/octet-stream</code> 이며 콘텐츠 계층은 이를 "표시할 수 없음"(다운로드)으로 본다.
 */
public final class MimeTypes {

    /**
     * Media type used when nothing is known.
     * <p>
     * 아무것도 알 수 없을 때 쓰는 미디어 타입.
     */
    public static final String OCTET_STREAM = "application/octet-stream";

    /**
     * Extension (lower case, without dot) to media type table.
     * <p>
     * 확장자(소문자, 점 없음)에서 미디어 타입으로 가는 표.
     */
    private static final Map<String, String> TABLE = new HashMap<String, String>();

    static {
        String[][] rows = {
            { "html", "text/html" }, { "htm", "text/html" }, { "xhtml", "application/xhtml+xml" }, { "xht", "application/xhtml+xml" },
            { "txt", "text/plain" }, { "text", "text/plain" }, { "md", "text/markdown" }, { "markdown", "text/markdown" },
            { "css", "text/css" }, { "js", "text/javascript" }, { "mjs", "text/javascript" }, { "json", "application/json" },
            { "xml", "application/xml" }, { "svg", "image/svg+xml" }, { "csv", "text/csv" },
            { "png", "image/png" }, { "apng", "image/apng" }, { "gif", "image/gif" }, { "jpg", "image/jpeg" }, { "jpeg", "image/jpeg" },
            { "bmp", "image/bmp" }, { "ico", "image/x-icon" }, { "webp", "image/webp" }, { "avif", "image/avif" },
            { "wav", "audio/wav" }, { "mp3", "audio/mpeg" }, { "ogg", "audio/ogg" }, { "oga", "audio/ogg" }, { "opus", "audio/ogg" },
            { "flac", "audio/flac" }, { "aac", "audio/aac" }, { "m4a", "audio/mp4" }, { "au", "audio/basic" }, { "snd", "audio/basic" },
            { "aif", "audio/aiff" }, { "aiff", "audio/aiff" }, { "weba", "audio/webm" },
            { "mp4", "video/mp4" }, { "webm", "video/webm" }, { "ogv", "video/ogg" },
            { "woff", "font/woff" }, { "woff2", "font/woff2" }, { "ttf", "font/ttf" }, { "otf", "font/otf" },
            { "pdf", "application/pdf" }, { "zip", "application/zip" }, { "gz", "application/gzip" }, { "jar", "application/java-archive" }
        };
        // 표의 각 행을 맵에 넣는다.
        for (int i = 0; i < rows.length; i++) {
            TABLE.put(rows[i][0], rows[i][1]);
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private MimeTypes() {
    }

    /**
     * Guesses the media type from a file name (or URL path). Query and fragment parts are ignored.
     * <p>
     * 파일 이름(또는 URL 경로)으로 미디어 타입을 추정한다. 질의와 프래그먼트 부분은 무시한다.
     *
     * @param name file name or path / 파일 이름 또는 경로
     * @return media type, {@link #OCTET_STREAM} if unknown / 미디어 타입, 모르면 {@link #OCTET_STREAM}
     */
    public static String fromFileName(String name) {
        if (name == null) {
            return OCTET_STREAM;
        }
        String n = name;
        int cut = n.indexOf('#');
        if (cut >= 0) {
            n = n.substring(0, cut);
        }
        cut = n.indexOf('?');
        if (cut >= 0) {
            n = n.substring(0, cut);
        }
        int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        n = n.substring(slash + 1);
        int dot = n.lastIndexOf('.');
        if (dot < 0 || dot == n.length() - 1) {
            return OCTET_STREAM;
        }
        String type = TABLE.get(n.substring(dot + 1).toLowerCase());
        return type == null ? OCTET_STREAM : type;
    }
}
