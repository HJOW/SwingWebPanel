package org.duckdns.hjow.swingwebpanel.media.vtt;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * The WebVTT file parser (WebVTT 6.1 "WebVTT file parsing", 6.2 region settings, 6.3 cue timings and settings), after the W3C Candidate
 * Recommendation of 2026-05-20. {@link #parse(byte[])} decodes UTF-8 (a byte order mark is dropped, a malformed sequence becomes U+FFFD) and
 * {@link #parse(String)} reads text. The parser works as the specification says: the file must start with <code>WEBVTT</code> (otherwise the
 * result is not {@link VttFile#valid}), the header block is skipped, and the blocks that follow are cues, <code>REGION</code> blocks,
 * <code>STYLE</code> blocks, comments (<code>NOTE</code>, which is a block that no rule accepts) or garbage; a cue block whose timing line is
 * wrong is dropped without hurting the cues after it.
 * <p>
 * WebVTT 파일 파서(WebVTT 6.1 "WebVTT file parsing", 6.2 region 설정, 6.3 cue 타이밍과 설정. 2026-05-20 W3C 후보 권고안). {@link #parse(byte[])} 는 UTF-8 을
 * 해독하고(바이트 순서 표시는 버리고 잘못된 열은 U+FFFD) {@link #parse(String)} 은 텍스트를 읽는다. 파서는 규격대로 동작한다: 파일은
 * <code>WEBVTT</code> 로 시작해야 하며(아니면 결과가 {@link VttFile#valid} 가 아니다) 머리 블록은 건너뛰고, 이어지는 블록은 cue, <code>REGION</code> 블록,
 * <code>STYLE</code> 블록, 주석(<code>NOTE</code>: 어떤 규칙도 받아들이지 않는 블록) 또는 쓰레기이다. 타이밍 줄이 틀린 cue 블록은 뒤의 cue 를 해치지 않고
 * 버려진다.
 */
public final class VttParser {

    /**
     * The input after the normalization of line ends.
     * <p>
     * 줄 끝을 정규화한 뒤의 입력.
     */
    private String input;

    /**
     * The position in the input.
     * <p>
     * 입력 안의 위치.
     */
    private int pos;

    /**
     * True once a cue was seen (a <code>STYLE</code> or <code>REGION</code> block after the first cue is not one).
     * <p>
     * cue 를 한 번이라도 봤으면 true(첫 cue 뒤의 <code>STYLE</code>, <code>REGION</code> 블록은 그런 블록이 아니다).
     */
    private boolean seenCue;

    /**
     * The regions that were defined so far (a cue refers to the last one with its identifier).
     * <p>
     * 지금까지 정의된 region(cue 는 식별자가 같은 마지막 것을 참조한다).
     */
    private final List<VttRegion> regions = new ArrayList<VttRegion>();

    /**
     * The result.
     * <p>
     * 결과.
     */
    private final VttFile result = new VttFile();

    /**
     * Private constructor: use the static methods.
     * <p>
     * 비공개 생성자: 정적 메서드를 쓴다.
     */
    private VttParser() {
    }

    /**
     * Parses a WebVTT file given as bytes: the bytes are UTF-8 (a leading byte order mark is removed).
     * <p>
     * 바이트로 주어진 WebVTT 파일을 해석한다: 바이트는 UTF-8 이다(앞의 바이트 순서 표시는 제거한다).
     *
     * @param data the bytes of the file / 파일의 바이트
     * @return the result / 결과
     */
    public static VttFile parse(byte[] data) {
        String text;
        try {
            // 자바의 UTF-8 해독기는 잘못된 열을 U+FFFD 로 바꾼다.
            text = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported", e);
        }
        return parse(text);
    }

    /**
     * Parses a WebVTT file given as text.
     * <p>
     * 텍스트로 주어진 WebVTT 파일을 해석한다.
     *
     * @param text the file; a leading U+FEFF is removed / 파일. 앞의 U+FEFF 는 제거한다
     * @return the result / 결과
     */
    public static VttFile parse(String text) {
        VttParser p = new VttParser();
        String s = text.length() > 0 && text.charAt(0) == (char) 0xFEFF ? text.substring(1) : text;
        p.input = normalize(s);
        p.run();
        return p.result;
    }

    /**
     * Normalizes the line ends and the NUL characters (WebVTT 6.1).
     * <p>
     * 줄 끝과 NUL 문자를 정규화한다(WebVTT 6.1).
     *
     * @param s the text / 텍스트
     * @return the normalized text / 정규화한 텍스트
     */
    static String normalize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        // 글자마다 NUL 은 U+FFFD 로, CRLF 와 CR 은 LF 로 바꾼다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == 0) {
                sb.append((char) 0xFFFD);
            } else if (c == '\r') {
                sb.append('\n');
                if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Runs the parser algorithm of WebVTT 6.1.
     * <p>
     * WebVTT 6.1 의 파서 알고리즘을 실행한다.
     */
    private void run() {
        int n = input.length();
        // 파일 서명을 확인한다: "WEBVTT" 뒤에는 입력의 끝, 스페이스, 탭, 줄 바꿈 중 하나가 와야 한다.
        if (n < 6 || !input.startsWith("WEBVTT")) {
            return;
        }
        if (n > 6 && input.charAt(6) != ' ' && input.charAt(6) != '\t' && input.charAt(6) != '\n') {
            return;
        }
        result.valid = true;
        collectLine();
        if (pos >= n) {
            return;
        }
        pos++;
        if (pos >= n) {
            return;
        }
        // 머리 블록: 줄이 하나 더 있으면 그 블록을 읽어 버리고, 빈 줄이면 건너뛴다.
        if (input.charAt(pos) != '\n') {
            collectBlock(true);
        } else {
            pos++;
        }
        skipLineFeeds();
        // 블록 반복: 입력의 끝까지 블록을 읽어 종류별로 모은다.
        while (pos < n) {
            Object block = collectBlock(false);
            if (block instanceof VttCue) {
                result.cues.add((VttCue) block);
            } else if (block instanceof VttRegion) {
                regions.add((VttRegion) block);
                result.regions.add((VttRegion) block);
            } else if (block instanceof String) {
                result.stylesheets.add((String) block);
            }
            skipLineFeeds();
        }
    }

    /**
     * Collects the characters up to the next line feed (the position stops at the line feed or at the end).
     * <p>
     * 다음 줄 바꿈 앞까지의 문자를 모은다(위치는 줄 바꿈이나 끝에서 멈춘다).
     *
     * @return the line / 줄
     */
    private String collectLine() {
        int start = pos;
        int n = input.length();
        // 줄 바꿈이 나올 때까지 위치를 옮긴다.
        while (pos < n && input.charAt(pos) != '\n') {
            pos++;
        }
        return input.substring(start, pos);
    }

    /**
     * Skips a run of line feeds.
     * <p>
     * 줄 바꿈의 연속을 건너뛴다.
     */
    private void skipLineFeeds() {
        // 줄 바꿈인 동안 위치를 옮긴다.
        while (pos < input.length() && input.charAt(pos) == '\n') {
            pos++;
        }
    }

    /**
     * Tells whether the rest of a string after a prefix is only ASCII whitespace.
     * <p>
     * 문자열에서 접두사 뒤가 모두 ASCII 공백인지 알려 준다.
     *
     * @param s      the string / 문자열
     * @param prefix the length of the prefix / 접두사 길이
     * @return true if only whitespace follows / 공백만 이어지면 true
     */
    private static boolean onlySpaceAfter(String s, int prefix) {
        // 접두사 뒤의 글자가 모두 공백인지 확인한다.
        for (int i = prefix; i < s.length(); i++) {
            if (!VttTimestamps.isSpace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Collects one WebVTT block (WebVTT 6.1 "collect a WebVTT block").
     * <p>
     * WebVTT 블록 하나를 모은다(WebVTT 6.1 "collect a WebVTT block").
     *
     * @param inHeader true while the header block is read / 머리 블록을 읽는 동안 true
     * @return a {@link VttCue}, a {@link VttRegion}, the text of a style sheet (a String) or null for anything else / {@link VttCue},
     *         {@link VttRegion}, 스타일 시트의 텍스트(String), 그 밖에는 null
     */
    private Object collectBlock(boolean inHeader) {
        int lineCount = 0;
        int previous = pos;
        String buffer = "";
        boolean seenEof = false;
        boolean seenArrow = false;
        VttCue cue = null;
        boolean stylesheet = false;
        VttRegion region = null;
        // 블록의 줄을 하나씩 읽는다: 빈 줄이나 입력의 끝에서 블록이 끝난다.
        while (true) {
            String line = collectLine();
            lineCount++;
            if (pos >= input.length()) {
                seenEof = true;
            } else {
                pos++;
            }
            if (line.indexOf("-->") >= 0) {
                // 머리가 아니고 블록의 첫 줄이거나(둘째 줄이며 아직 화살표가 없는 경우) 타이밍 줄이다.
                if (!inHeader && (lineCount == 1 || (lineCount == 2 && !seenArrow))) {
                    seenArrow = true;
                    previous = pos;
                    cue = new VttCue();
                    cue.id = buffer;
                    if (!collectTimingsAndSettings(line, cue)) {
                        cue = null;
                    } else {
                        buffer = "";
                        seenCue = true;
                    }
                } else {
                    // 그 밖의 화살표 줄은 다음 블록의 시작이다: 위치를 되돌리고 이 블록을 끝낸다.
                    pos = previous;
                    break;
                }
            } else if (line.length() == 0) {
                break;
            } else {
                // 둘째 줄이면 첫 줄이 STYLE 이나 REGION 이었는지 본다(첫 cue 보다 앞에 있을 때만).
                if (!inHeader && lineCount == 2) {
                    if (!seenCue && buffer.startsWith("STYLE") && onlySpaceAfter(buffer, 5)) {
                        stylesheet = true;
                        buffer = "";
                    } else if (!seenCue && buffer.startsWith("REGION") && onlySpaceAfter(buffer, 6)) {
                        region = new VttRegion();
                        buffer = "";
                    }
                }
                if (buffer.length() > 0) {
                    buffer = buffer + "\n";
                }
                buffer = buffer + line;
                previous = pos;
                if (seenEof) {
                    break;
                }
            }
        }
        if (cue != null) {
            cue.text = buffer;
            return cue;
        }
        if (stylesheet) {
            return buffer;
        }
        if (region != null) {
            collectRegionSettings(buffer, region);
            return region;
        }
        return null;
    }

    /**
     * Splits a settings string on ASCII whitespace.
     * <p>
     * 설정 문자열을 ASCII 공백으로 나눈다.
     *
     * @param s the string / 문자열
     * @return the non-empty tokens / 비어 있지 않은 토큰
     */
    private static List<String> tokens(String s) {
        List<String> out = new ArrayList<String>();
        int start = -1;
        // 공백이 아닌 구간마다 토큰 하나를 만든다.
        for (int i = 0; i <= s.length(); i++) {
            boolean space = i == s.length() || VttTimestamps.isSpace(s.charAt(i));
            if (!space && start < 0) {
                start = i;
            } else if (space && start >= 0) {
                out.add(s.substring(start, i));
                start = -1;
            }
        }
        return out;
    }

    /**
     * Collects the region settings (WebVTT 6.2).
     * <p>
     * region 설정을 모은다(WebVTT 6.2).
     *
     * @param text   the settings / 설정
     * @param region the region that receives them / 설정을 받는 region
     */
    private static void collectRegionSettings(String text, VttRegion region) {
        List<String> list = tokens(text);
        // 설정 토큰마다 이름:값 으로 나누어 알려진 이름만 적용한다.
        for (int i = 0; i < list.size(); i++) {
            String setting = list.get(i);
            int colon = setting.indexOf(':');
            if (colon <= 0 || colon == setting.length() - 1) {
                continue;
            }
            String name = setting.substring(0, colon);
            String value = setting.substring(colon + 1);
            if ("id".equals(name)) {
                region.id = value;
            } else if ("width".equals(name)) {
                double p = VttTimestamps.parsePercentage(value);
                if (!Double.isNaN(p)) {
                    region.width = p;
                }
            } else if ("lines".equals(name)) {
                if (allDigits(value)) {
                    region.lines = value.length() > 18 ? Long.MAX_VALUE : Long.parseLong(value);
                }
            } else if ("regionanchor".equals(name)) {
                double[] xy = pair(value);
                if (xy != null) {
                    region.regionAnchorX = xy[0];
                    region.regionAnchorY = xy[1];
                }
            } else if ("viewportanchor".equals(name)) {
                double[] xy = pair(value);
                if (xy != null) {
                    region.viewportAnchorX = xy[0];
                    region.viewportAnchorY = xy[1];
                }
            } else if ("scroll".equals(name) && "up".equals(value)) {
                region.scroll = "up";
            }
        }
    }

    /**
     * Tells whether a string consists only of ASCII digits (and is not empty).
     * <p>
     * 문자열이 ASCII 숫자로만 이루어졌는지(비어 있지 않은지) 알려 준다.
     *
     * @param s the string / 문자열
     * @return true if so / 그러면 true
     */
    private static boolean allDigits(String s) {
        if (s.length() == 0) {
            return false;
        }
        // 글자마다 숫자인지 확인한다.
        for (int i = 0; i < s.length(); i++) {
            if (!VttTimestamps.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses a pair of percentages separated by a comma (<code>x,y</code>).
     * <p>
     * 쉼표로 나뉜 백분율 쌍(<code>x,y</code>)을 해석한다.
     *
     * @param value the text / 텍스트
     * @return the two numbers, or null if it is not a pair of percentages / 두 수. 백분율 쌍이 아니면 null
     */
    private static double[] pair(String value) {
        int comma = value.indexOf(',');
        if (comma < 0) {
            return null;
        }
        double x = VttTimestamps.parsePercentage(value.substring(0, comma));
        double y = VttTimestamps.parsePercentage(value.substring(comma + 1));
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return null;
        }
        return new double[] { x, y };
    }

    /**
     * Skips the whitespace at a position.
     * <p>
     * 위치의 공백을 건너뛴다.
     *
     * @param s   the text / 텍스트
     * @param pos the position, one element / 위치(원소 하나)
     */
    private static void skipSpace(String s, int[] pos) {
        // 공백인 동안 위치를 옮긴다.
        while (pos[0] < s.length() && VttTimestamps.isSpace(s.charAt(pos[0]))) {
            pos[0]++;
        }
    }

    /**
     * Collects the cue timings and settings of a timing line (WebVTT 6.3).
     * <p>
     * 타이밍 줄의 cue 시각과 설정을 모은다(WebVTT 6.3).
     *
     * @param line the line / 줄
     * @param cue  the cue that receives them / 값을 받는 cue
     * @return true if the timings were correct / 시각이 올바르면 true
     */
    private boolean collectTimingsAndSettings(String line, VttCue cue) {
        int[] p = { 0 };
        skipSpace(line, p);
        double start = VttTimestamps.collectTimestamp(line, p);
        if (Double.isNaN(start)) {
            return false;
        }
        cue.startTime = start;
        skipSpace(line, p);
        if (!expect(line, p, "-->")) {
            return false;
        }
        skipSpace(line, p);
        double end = VttTimestamps.collectTimestamp(line, p);
        if (Double.isNaN(end)) {
            return false;
        }
        cue.endTime = end;
        parseCueSettings(line.substring(p[0]), cue);
        return true;
    }

    /**
     * Consumes a literal at a position.
     * <p>
     * 위치의 리터럴을 소비한다.
     *
     * @param s       the text / 텍스트
     * @param pos     the position, one element / 위치(원소 하나)
     * @param literal the literal / 리터럴
     * @return true if the text had it there / 텍스트에 있었으면 true
     */
    private static boolean expect(String s, int[] pos, String literal) {
        if (!s.startsWith(literal, pos[0])) {
            return false;
        }
        pos[0] += literal.length();
        return true;
    }

    /**
     * Parses the cue settings that follow the timings (WebVTT 6.3 "parse the WebVTT cue settings"): a setting that is wrong is ignored and
     * the rest still applies.
     * <p>
     * 타이밍 뒤의 cue 설정을 해석한다(WebVTT 6.3 "parse the WebVTT cue settings"): 잘못된 설정은 무시하고 나머지는 그대로 적용한다.
     *
     * @param text the settings / 설정
     * @param cue  the cue / cue
     */
    private void parseCueSettings(String text, VttCue cue) {
        List<String> list = tokens(text);
        // 설정 토큰마다 이름:값 으로 나누어 알려진 이름만 적용한다.
        for (int i = 0; i < list.size(); i++) {
            String setting = list.get(i);
            int colon = setting.indexOf(':');
            if (colon <= 0 || colon == setting.length() - 1) {
                continue;
            }
            String name = setting.substring(0, colon);
            String value = setting.substring(colon + 1);
            if ("region".equals(name)) {
                cue.region = findRegion(value);
            } else if ("vertical".equals(name)) {
                if ("rl".equals(value) || "lr".equals(value)) {
                    cue.vertical = value;
                }
                // 세로 cue 는 region 에 들어갈 수 없다.
                if (cue.vertical.length() > 0) {
                    cue.region = null;
                }
            } else if ("line".equals(name)) {
                applyLine(value, cue);
            } else if ("position".equals(name)) {
                applyPosition(value, cue);
            } else if ("size".equals(name)) {
                double p = VttTimestamps.parsePercentage(value);
                if (!Double.isNaN(p)) {
                    cue.size = p;
                    // 크기를 정한 cue 는 region 에서 빠진다.
                    if (p != 100) {
                        cue.region = null;
                    }
                }
            } else if ("align".equals(name)) {
                if ("start".equals(value) || "center".equals(value) || "end".equals(value) || "left".equals(value) || "right".equals(value)) {
                    cue.align = value;
                }
            }
        }
    }

    /**
     * Finds the last region with an identifier.
     * <p>
     * 식별자가 같은 마지막 region 을 찾는다.
     *
     * @param id the identifier / 식별자
     * @return the region or null / region. 없으면 null
     */
    private VttRegion findRegion(String id) {
        // 마지막에 정의된 것이 우선하므로 뒤에서부터 찾는다.
        for (int i = regions.size() - 1; i >= 0; i--) {
            if (regions.get(i).id.equals(id)) {
                return regions.get(i);
            }
        }
        return null;
    }

    /**
     * Applies a <code>line</code> setting (<code>number</code>, <code>number%</code>, either with <code>,start|center|end</code>).
     * <p>
     * <code>line</code> 설정을 적용한다(<code>숫자</code>, <code>숫자%</code>, 둘 다 <code>,start|center|end</code> 가 붙을 수 있다).
     *
     * @param value the value / 값
     * @param cue   the cue / cue
     */
    private static void applyLine(String value, VttCue cue) {
        int comma = value.indexOf(',');
        String linepos = comma < 0 ? value : value.substring(0, comma);
        String linealign = comma < 0 ? null : value.substring(comma + 1);
        boolean digit = false;
        // 숫자가 하나 이상 있어야 한다.
        for (int i = 0; i < linepos.length(); i++) {
            if (VttTimestamps.isDigit(linepos.charAt(i))) {
                digit = true;
            }
        }
        if (!digit) {
            return;
        }
        boolean percent = linepos.endsWith("%");
        double number;
        if (percent) {
            number = VttTimestamps.parsePercentage(linepos);
            if (Double.isNaN(number)) {
                return;
            }
        } else {
            if (!isPlainNumber(linepos)) {
                return;
            }
            number = Double.parseDouble(linepos);
        }
        if (linealign != null) {
            if (!"start".equals(linealign) && !"center".equals(linealign) && !"end".equals(linealign)) {
                return;
            }
            cue.lineAlign = linealign;
        }
        cue.line = number;
        cue.snapToLines = !percent;
        // 줄을 정한 cue 는 region 에서 빠진다.
        cue.region = null;
    }

    /**
     * Tells whether a text is a number with an optional leading minus and an optional fraction (<code>-1</code>, <code>2.5</code>).
     * <p>
     * 텍스트가 맨 앞의 마이너스와 소수부가 선택적인 숫자(<code>-1</code>, <code>2.5</code>)인지 알려 준다.
     *
     * @param s the text / 텍스트
     * @return true if it is / 그러면 true
     */
    private static boolean isPlainNumber(String s) {
        int dots = 0;
        // 글자마다 확인한다: 마이너스는 맨 앞에만, 점은 하나만, 점의 앞뒤는 숫자.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '-') {
                if (i != 0) {
                    return false;
                }
            } else if (c == '.') {
                dots++;
                if (dots > 1 || i == 0 || i == s.length() - 1 || !VttTimestamps.isDigit(s.charAt(i - 1)) || !VttTimestamps.isDigit(s.charAt(i + 1))) {
                    return false;
                }
            } else if (!VttTimestamps.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies a <code>position</code> setting (<code>number%</code> with an optional <code>,line-left|center|line-right</code>).
     * <p>
     * <code>position</code> 설정을 적용한다(<code>숫자%</code>, <code>,line-left|center|line-right</code> 가 붙을 수 있다).
     *
     * @param value the value / 값
     * @param cue   the cue / cue
     */
    private static void applyPosition(String value, VttCue cue) {
        int comma = value.indexOf(',');
        String colpos = comma < 0 ? value : value.substring(0, comma);
        String colalign = comma < 0 ? null : value.substring(comma + 1);
        double number = VttTimestamps.parsePercentage(colpos);
        if (Double.isNaN(number)) {
            return;
        }
        if (colalign != null) {
            if (!"line-left".equals(colalign) && !"center".equals(colalign) && !"line-right".equals(colalign)) {
                return;
            }
            cue.positionAlign = colalign;
        }
        cue.position = number;
    }
}
