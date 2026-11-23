package org.duckdns.hjow.swingwebpanel.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the HTML page that shows an FTP directory. Directories come first and are links (with a trailing slash) that lead to
 * the sub directory; the very first entry is a <code>../</code> link to the parent directory except at the FTP root, where
 * nothing links above the root. Files are links too (so that a click downloads or shows them). Every name is HTML-escaped, so
 * a file name can never become a tag, and every link is a percent-encoded relative URL that starts with <code>./</code>
 * (a name such as <code>a:b</code> can therefore not be mistaken for a scheme). Lines that could not be parsed are listed
 * as plain escaped text without a link.
 * <p>
 * FTP 디렉터리를 보여 주는 HTML 페이지를 만든다. 디렉터리를 먼저 나열하고 하위 디렉터리로 가는 링크(끝에 슬래시)로 만들며,
 * FTP 루트가 아니면 가장 먼저 상위 디렉터리로 가는 <code>../</code> 링크를 둔다(루트 위로 가는 링크는 없다). 파일도 링크이다(클릭하면
 * 내려받거나 표시). 모든 이름은 HTML 이스케이프하므로 파일 이름이 태그가 될 수 없고, 링크는 모두 <code>./</code> 로 시작하는 퍼센트
 * 인코딩된 상대 URL 이다(<code>a:b</code> 같은 이름이 스킴으로 오해되지 않는다). 해석하지 못한 줄은 링크 없이 이스케이프한
 * 평문으로 나열한다.
 */
public final class FtpListing {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FtpListing() {
    }

    /**
     * Escapes text for HTML element content and attribute values; control characters become a question mark.
     * <p>
     * HTML 요소 내용과 속성 값을 위해 문자열을 이스케이프한다. 제어 문자는 물음표가 된다.
     *
     * @param s text / 문자열
     * @return escaped text / 이스케이프된 문자열
     */
    public static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        // 문자마다 HTML 특수 문자는 문자 참조로, 제어 문자는 '?' 로 바꾼다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '&':
                sb.append("&amp;");
                break;
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '"':
                sb.append("&quot;");
                break;
            case '\'':
                sb.append("&#39;");
                break;
            default:
                sb.append(c < 0x20 || c == 0x7F ? '?' : c);
            }
        }
        return sb.toString();
    }

    /**
     * Builds the page.
     * <p>
     * 페이지를 만든다.
     *
     * @param decodedPath directory path as shown to the user (decoded, starts and ends with '/') / 사용자에게 보여 줄 디렉터리 경로(디코딩됨, '/' 로 시작하고 끝남)
     * @param entries     entries of the directory / 디렉터리의 항목들
     * @return HTML text / HTML 문자열
     */
    public static String toHtml(String decodedPath, List<FtpEntry> entries) {
        List<FtpEntry> sorted = new ArrayList<FtpEntry>(entries);
        Collections.sort(sorted, new Comparator<FtpEntry>() {
            /**
             * Directories first, then links, files and others; each group by name ignoring case.
             * <p>
             * 디렉터리, 링크, 파일, 기타 순이며 각 묶음은 대소문자 무시 이름순이다.
             *
             * @param a first entry / 첫 항목
             * @param b second entry / 두 번째 항목
             * @return comparison / 비교 결과
             */
            public int compare(FtpEntry a, FtpEntry b) {
                int ra = rank(a);
                int rb = rank(b);
                if (ra != rb) {
                    return ra - rb;
                }
                String na = a.getName() == null ? a.getRaw() : a.getName();
                String nb = b.getName() == null ? b.getRaw() : b.getName();
                return na.compareToIgnoreCase(nb);
            }
        });
        String title = "Index of " + decodedPath;
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n<title>").append(escape(title))
                .append("</title>\n</head>\n<body>\n<h1>").append(escape(title)).append("</h1>\n<ul class=\"ftp-listing\">\n");
        if (!"/".equals(decodedPath)) {
            sb.append("<li class=\"up\"><a href=\"../\">../</a></li>\n");
        }
        // 정렬된 항목을 하나씩 목록 줄로 만든다.
        for (int i = 0; i < sorted.size(); i++) {
            FtpEntry e = sorted.get(i);
            if (e.getName() == null) {
                sb.append("<li class=\"unparsed\">").append(escape(e.getRaw())).append("</li>\n");
                continue;
            }
            String href = "./" + UrlUtil.percentEncodeComponent(e.getName());
            String label = escape(e.getName());
            switch (e.getType()) {
            case DIRECTORY:
                sb.append("<li class=\"dir\"><a href=\"").append(href).append("/\">").append(label).append("/</a></li>\n");
                break;
            case SYMLINK:
                sb.append("<li class=\"link\"><a href=\"").append(href).append("\">").append(label).append("</a> -&gt; ")
                        .append(escape(e.getLinkTarget() == null ? "?" : e.getLinkTarget())).append("</li>\n");
                break;
            case FILE:
                sb.append("<li class=\"file\"><a href=\"").append(href).append("\">").append(label).append("</a>");
                if (e.getSize() >= 0) {
                    sb.append(" <span class=\"size\">").append(e.getSize()).append(" bytes</span>");
                }
                sb.append("</li>\n");
                break;
            default:
                sb.append("<li class=\"other\">").append(label).append("</li>\n");
            }
        }
        sb.append("</ul>\n</body>\n</html>\n");
        return sb.toString();
    }

    /**
     * Sort group of an entry.
     * <p>
     * 항목의 정렬 묶음 번호.
     *
     * @param e entry / 항목
     * @return group number / 묶음 번호
     */
    private static int rank(FtpEntry e) {
        if (e.getName() == null) {
            return 4;
        }
        switch (e.getType()) {
        case DIRECTORY:
            return 0;
        case SYMLINK:
            return 1;
        case FILE:
            return 2;
        default:
            return 3;
        }
    }
}
