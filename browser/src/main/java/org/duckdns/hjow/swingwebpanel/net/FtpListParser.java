package org.duckdns.hjow.swingwebpanel.net;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses FTP directory listings: the machine readable <code>MLSD</code> format (RFC 3659) and the common human readable
 * <code>LIST</code> formats (UNIX <code>ls -l</code> and DOS/IIS). Names may contain spaces and any non-ASCII characters.
 * A line that matches no known format is kept as an {@link FtpEntry.Type#UNKNOWN} entry with its original text and a
 * diagnostic message; it is never turned into a directory by guessing. The entries <code>.</code> and <code>..</code>, empty
 * lines and <code>total N</code> lines are skipped.
 * <p>
 * FTP 디렉터리 목록을 해석한다. 기계가 읽는 <code>MLSD</code> 형식(RFC 3659)과 흔한 사람용 <code>LIST</code> 형식(UNIX
 * <code>ls -l</code>, DOS/IIS)을 다룬다. 이름에는 공백과 비 ASCII 문자가 들어갈 수 있다. 알려진 형식에 맞지 않는 줄은 원문과
 * 진단 메시지를 가진 {@link FtpEntry.Type#UNKNOWN} 항목으로 보관하며 추측으로 디렉터리로 바꾸지 않는다. <code>.</code>,
 * <code>..</code>, 빈 줄, <code>total N</code> 줄은 건너뛴다.
 */
public final class FtpListParser {

    /**
     * UNIX style line: mode, link count, owner, optional group, size, date, name.
     * <p>
     * UNIX 형식 줄: 권한, 링크 수, 소유자, 선택적 그룹, 크기, 날짜, 이름.
     */
    private static final Pattern UNIX = Pattern.compile(
            "^([\\-dlbcpsDF?])([rwxsStTlL\\-?]{9})[+@.]?\\s+\\d+\\s+(\\S+)\\s+(?:(\\S+)\\s+)?(\\d+(?:,\\s*\\d+)?)\\s+"
            + "((?:[A-Za-z]{3}\\s+\\d{1,2}\\s+(?:\\d{4}|\\d{1,2}:\\d{2}))|(?:\\d{4}-\\d{2}-\\d{2}\\s+\\d{1,2}:\\d{2}(?::\\d{2})?))\\s(.*)$");

    /**
     * DOS/IIS style line: date, time, size or &lt;DIR&gt;, name.
     * <p>
     * DOS/IIS 형식 줄: 날짜, 시간, 크기 또는 &lt;DIR&gt;, 이름.
     */
    private static final Pattern DOS = Pattern.compile(
            "^(\\d{2}-\\d{2}-\\d{2,4})\\s+(\\d{1,2}:\\d{2}\\s*(?:[AaPp][Mm])?)\\s+(<DIR>|\\d+)\\s+(.*)$");

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FtpListParser() {
    }

    /**
     * Parses a whole listing; the format of every line is detected on its own (MLSD facts, UNIX or DOS).
     * <p>
     * 목록 전체를 해석한다. 줄마다 형식(MLSD 사실, UNIX, DOS)을 따로 판별한다.
     *
     * @param text        listing text / 목록 문자열
     * @param mlsd        true when the text came from MLSD / MLSD 에서 온 문자열이면 true
     * @param out         receives the entries / 항목을 받는다
     * @param diagnostics receives messages for unparsable lines / 해석할 수 없는 줄의 메시지를 받는다
     */
    public static void parse(String text, boolean mlsd, List<FtpEntry> out, List<String> diagnostics) {
        String[] lines = text.split("\r\n|\r|\n");
        // 줄마다 항목 하나로 해석한다.
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().length() == 0 || line.startsWith("total ") || line.startsWith("Total ")) {
                continue;
            }
            FtpEntry e = mlsd ? parseMlsdLine(line) : parseListLine(line);
            if (e == null) {
                continue;
            }
            if (e.getType() == FtpEntry.Type.UNKNOWN && e.getName() == null) {
                diagnostics.add("unparsable listing line: " + line);
            }
            out.add(e);
        }
    }

    /**
     * Parses one MLSD line (<code>fact=value;fact=value; name</code>); null for current/parent directory entries.
     * <p>
     * MLSD 줄 하나(<code>fact=value;fact=value; name</code>)를 해석한다. 현재/상위 디렉터리 항목은 null.
     *
     * @param line the line / 줄
     * @return the entry or null / 항목 또는 null
     */
    static FtpEntry parseMlsdLine(String line) {
        int sp = line.indexOf(' ');
        if (sp < 0) {
            return new FtpEntry(null, FtpEntry.Type.UNKNOWN, -1, null, null, line);
        }
        String facts = line.substring(0, sp);
        String name = line.substring(sp + 1);
        String type = "";
        long size = -1;
        String modify = null;
        String[] parts = facts.split(";");
        // 사실(fact)을 하나씩 보며 종류, 크기, 수정 시각만 사용한다.
        for (int i = 0; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = parts[i].substring(0, eq).toLowerCase();
            String val = parts[i].substring(eq + 1);
            if ("type".equals(key)) {
                type = val;
            } else if ("size".equals(key) || "sizd".equals(key)) {
                size = parseLong(val);
            } else if ("modify".equals(key)) {
                modify = val;
            }
        }
        String lower = type.toLowerCase();
        if ("cdir".equals(lower) || "pdir".equals(lower) || ".".equals(name) || "..".equals(name)) {
            return null;
        }
        if ("file".equals(lower)) {
            return new FtpEntry(name, FtpEntry.Type.FILE, size, modify, null, line);
        }
        if ("dir".equals(lower)) {
            return new FtpEntry(name, FtpEntry.Type.DIRECTORY, size, modify, null, line);
        }
        if (lower.startsWith("os.unix=slink") || lower.startsWith("os.unix=symlink")) {
            int colon = type.indexOf(':');
            return new FtpEntry(name, FtpEntry.Type.SYMLINK, size, modify, colon >= 0 ? type.substring(colon + 1) : null, line);
        }
        return new FtpEntry(name, FtpEntry.Type.UNKNOWN, size, modify, null, line);
    }

    /**
     * Parses one LIST line (UNIX or DOS style); null for "." and ".." entries.
     * <p>
     * LIST 줄 하나(UNIX 또는 DOS 형식)를 해석한다. "." 와 ".." 항목은 null.
     *
     * @param line the line / 줄
     * @return the entry or null / 항목 또는 null
     */
    static FtpEntry parseListLine(String line) {
        Matcher m = UNIX.matcher(line);
        if (m.matches()) {
            char kind = m.group(1).charAt(0);
            String name = m.group(7);
            long size = parseLong(m.group(5));
            String date = m.group(6);
            if (kind == 'l') {
                int arrow = name.indexOf(" -> ");
                String target = arrow >= 0 ? name.substring(arrow + 4) : null;
                String linkName = arrow >= 0 ? name.substring(0, arrow) : name;
                return new FtpEntry(linkName, FtpEntry.Type.SYMLINK, size, date, target, line);
            }
            if (".".equals(name) || "..".equals(name)) {
                return null;
            }
            if (kind == '-') {
                return new FtpEntry(name, FtpEntry.Type.FILE, size, date, null, line);
            }
            if (kind == 'd') {
                return new FtpEntry(name, FtpEntry.Type.DIRECTORY, -1, date, null, line);
            }
            return new FtpEntry(name, FtpEntry.Type.UNKNOWN, size, date, null, line);
        }
        m = DOS.matcher(line);
        if (m.matches()) {
            String name = m.group(4);
            if (".".equals(name) || "..".equals(name)) {
                return null;
            }
            String date = m.group(1) + " " + m.group(2);
            if ("<DIR>".equals(m.group(3))) {
                return new FtpEntry(name, FtpEntry.Type.DIRECTORY, -1, date, null, line);
            }
            return new FtpEntry(name, FtpEntry.Type.FILE, parseLong(m.group(3)), date, null, line);
        }
        return new FtpEntry(null, FtpEntry.Type.UNKNOWN, -1, null, null, line);
    }

    /**
     * Parses a long, returning -1 for invalid text.
     * <p>
     * long 값을 해석하며 잘못된 문자열이면 -1 을 돌려준다.
     *
     * @param s digits / 숫자 문자열
     * @return value or -1 / 값 또는 -1
     */
    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
