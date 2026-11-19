package vogen;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Build tool that turns the Unicode data file <code>VerticalOrientation.txt</code> (property <code>Vertical_Orientation</code> of UAX #50)
 * into the Java source <code>VerticalOrientationData</code> of the product package <code>org.duckdns.hjow.swingwebpanel.layout</code>. The output lists the
 * code point ranges whose orientation is not <code>R</code> (that is <code>U</code>, <code>Tu</code> and <code>Tr</code>, which CSS Writing
 * Modes Level 3 section 5.1.2 typesets upright in mixed vertical text) as chunked string constants of hexadecimal <code>from-to</code>
 * pairs, so no resource loading is needed at run time.
 * <p>
 * The data file lists only the assigned code points. Its header says that the unassigned code points of some blocks (CJK, Hangul,
 * private use, the supplementary ideographic planes and so on) default to <code>U</code> and all other code points are <code>R</code>;
 * this tool applies that rule with the table {@link #UNASSIGNED_UPRIGHT}, which is a copy of the list in the header.
 * <p>
 * Usage: <code>VoGen VerticalOrientation.txt outputDirectory</code>. The <code>.meta</code> file written by the webfetch tool next to the
 * input (if present) provides the source URL, retrieval date and revision recorded in the generated Javadoc.
 * <p>
 * 유니코드 데이터 파일 <code>VerticalOrientation.txt</code>(UAX #50 의 <code>Vertical_Orientation</code> 속성)를 제품 패키지
 * <code>org.duckdns.hjow.swingwebpanel.layout</code> 의 자바 소스 <code>VerticalOrientationData</code> 로 바꾸는 빌드 도구이다. 출력은 방향이 <code>R</code> 이 아닌
 * (<code>U</code>, <code>Tu</code>, <code>Tr</code>: CSS Writing Modes Level 3 5.1.2 절이 혼합 세로쓰기에서 똑바로 세우는 값) 코드 포인트 범위를
 * 16 진수 <code>시작-끝</code> 쌍의 조각난 문자열 상수로 낸다. 그래서 실행 중 자원을 읽을 필요가 없다.
 * <p>
 * 데이터 파일은 배정된 코드 포인트만 나열한다. 머리말은 일부 블록(CJK, 한글, 사설 영역, 보조 한자 평면 등)의 미배정 코드 포인트가
 * <code>U</code> 가 기본값이고 나머지는 <code>R</code> 이라고 적는다. 이 도구는 머리말 목록의 복사본 {@link #UNASSIGNED_UPRIGHT} 로 그 규칙을 적용한다.
 * <p>
 * 사용법: <code>VoGen VerticalOrientation.txt 출력디렉터리</code>. 입력 옆에 webfetch 도구가 쓴 <code>.meta</code> 파일이 있으면 출처 URL, 조회 날짜,
 * 판본을 생성된 Javadoc 에 기록한다.
 */
public final class VoGen {

    /**
     * Maximum characters per generated string constant (far below the 65535 byte class file limit).
     * <p>
     * 생성되는 문자열 상수 하나의 최대 글자 수(클래스 파일 한계 65535 바이트보다 훨씬 작다).
     */
    private static final int CHUNK = 6000;

    /** Highest code point. 가장 큰 코드 포인트. */
    private static final int MAX = 0x10FFFF;

    /**
     * Ranges whose unassigned code points default to <code>U</code>, copied from the header of VerticalOrientation-18.0.0.txt.
     * <p>
     * 미배정 코드 포인트의 기본값이 <code>U</code> 인 범위. VerticalOrientation-18.0.0.txt 머리말에서 복사했다.
     */
    private static final int[][] UNASSIGNED_UPRIGHT = {
        {0x18B0, 0x18FF}, {0x2065, 0x2065}, {0x2150, 0x218F}, {0x2400, 0x245F}, {0x2BB8, 0x2BFF}, {0x2E80, 0xA4CF}, {0xA960, 0xA97F},
        {0xAC00, 0xD7FF}, {0xE000, 0xFAFF}, {0xFE10, 0xFE1F}, {0xFE50, 0xFE6F}, {0xFFE7, 0xFFE7}, {0xFFF0, 0xFFF8}, {0x11580, 0x115FF},
        {0x11A00, 0x11AAF}, {0x13000, 0x143FF}, {0x14400, 0x1467F}, {0x16FE0, 0x18AFF}, {0x18B00, 0x18D7F}, {0x18D80, 0x18DFF},
        {0x18E00, 0x191DF}, {0x1AFF0, 0x1AFFF}, {0x1B100, 0x1B16F}, {0x1B170, 0x1B2FF}, {0x1CF00, 0x1CFCF}, {0x1CEC0, 0x1CEFF},
        {0x1D000, 0x1D1FF}, {0x1D2E0, 0x1D2FF}, {0x1D300, 0x1D37F}, {0x1D800, 0x1DAAF}, {0x1F000, 0x1F0FF}, {0x1F100, 0x1F2FF},
        {0x1F680, 0x1F7FF}, {0x1F900, 0x1F9FF}, {0x1FA00, 0x1FAFF}, {0x20000, 0x2FFFD}, {0x30000, 0x3FFFD}, {0xF0000, 0xFFFFD},
        {0x100000, 0x10FFFD}
    };

    /**
     * Private constructor: static tool class.
     * <p>
     * 비공개 생성자: 정적 도구 클래스이다.
     */
    private VoGen() {
    }

    /**
     * Entry point.
     * <p>
     * 진입점.
     *
     * @param args the input file and the output directory / 입력 파일과 출력 디렉터리
     * @throws IOException if reading or writing fails / 읽기나 쓰기가 실패할 때
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("usage: VoGen VerticalOrientation.txt outputDirectory");
            System.exit(1);
            return;
        }
        File out = new File(args[1]);
        if (!out.isDirectory() && !out.mkdirs()) {
            System.err.println("cannot create " + out);
            System.exit(2);
            return;
        }
        String meta = readMeta(new File(args[0] + ".meta"));
        String text = readText(new File(args[0]));
        // 코드 포인트마다 방향 값을 둔다: 0 = R(기본), 1 = U/Tu/Tr(똑바로), 2 = 파일이 명시한 R.
        byte[] value = new byte[MAX + 1];
        String version = "";
        int listed = 0;
        String[] lines = text.split("\n");
        // 줄을 하나씩 읽어 범위와 값을 해석한다.
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("# VerticalOrientation-")) {
                version = line.substring(2);
            }
            int hash = line.indexOf('#');
            if (hash >= 0) {
                line = line.substring(0, hash).trim();
            }
            if (line.length() == 0) {
                continue;
            }
            String[] fields = line.split(";");
            if (fields.length != 2) {
                throw new IOException("bad line " + (i + 1) + ": " + lines[i]);
            }
            String range = fields[0].trim();
            String v = fields[1].trim();
            int dots = range.indexOf("..");
            int from = Integer.parseInt(dots < 0 ? range : range.substring(0, dots), 16);
            int to = dots < 0 ? from : Integer.parseInt(range.substring(dots + 2), 16);
            byte b;
            if ("R".equals(v)) {
                b = 2;
            } else if ("U".equals(v) || "Tu".equals(v) || "Tr".equals(v)) {
                b = 1;
            } else {
                throw new IOException("unknown value " + v + " at line " + (i + 1));
            }
            // 범위의 코드 포인트마다 값을 기록한다.
            for (int cp = from; cp <= to; cp++) {
                value[cp] = b;
            }
            listed++;
        }
        // 미배정 코드 포인트의 기본값 U 를 머리말의 범위에 적용한다.
        for (int r = 0; r < UNASSIGNED_UPRIGHT.length; r++) {
            for (int cp = UNASSIGNED_UPRIGHT[r][0]; cp <= UNASSIGNED_UPRIGHT[r][1]; cp++) {
                if (value[cp] == 0) {
                    value[cp] = 1;
                }
            }
        }
        List<String> pairs = new ArrayList<String>();
        int count = 0;
        int cp = 0;
        // 값이 1 인 코드 포인트의 연속 구간을 모은다.
        while (cp <= MAX) {
            if (value[cp] != 1) {
                cp++;
                continue;
            }
            int end = cp;
            // 값이 1 인 코드 포인트가 이어지는 동안 구간 끝을 늘린다.
            while (end + 1 <= MAX && value[end + 1] == 1) {
                end++;
            }
            pairs.add(Integer.toHexString(cp) + "-" + Integer.toHexString(end));
            count += end - cp + 1;
            cp = end + 1;
        }
        writeText(new File(out, "VerticalOrientationData.java"), generate(pairs, count, version, meta, listed));
        System.err.println("generated VerticalOrientationData.java in " + out + ": " + pairs.size() + " ranges, " + count + " upright code points");
    }

    /**
     * Reads the provenance lines (source-url, retrieved, last-modified, etag) of a meta file.
     * <p>
     * meta 파일에서 출처 줄(source-url, retrieved, last-modified, etag)을 읽는다.
     *
     * @param f the meta file / meta 파일
     * @return the lines joined by newlines, or an empty string if the file does not exist / 줄을 줄바꿈으로 이은 문자열. 파일이 없으면 빈 문자열
     * @throws IOException if reading fails / 읽기가 실패할 때
     */
    private static String readMeta(File f) throws IOException {
        if (!f.isFile()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String[] lines = readText(f).split("\n");
        // 출처에 필요한 키의 줄만 모은다.
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].trim();
            if (l.startsWith("# source-url:") || l.startsWith("# retrieved:") || l.startsWith("# last-modified:") || l.startsWith("# etag:")) {
                sb.append(l.substring(2)).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Builds the generated source.
     * <p>
     * 생성할 소스를 만든다.
     *
     * @param pairs   hexadecimal <code>from-to</code> pairs / 16 진수 <code>시작-끝</code> 쌍
     * @param count   number of upright code points / 똑바로 세우는 코드 포인트 수
     * @param version the version line of the input / 입력의 판본 줄
     * @param meta    provenance lines / 출처 줄
     * @param listed  number of data lines / 데이터 줄 수
     * @return the Java source / 자바 소스
     */
    private static String generate(List<String> pairs, int count, String version, String meta, int listed) {
        StringBuilder sb = new StringBuilder();
        sb.append("package org.duckdns.hjow.swingwebpanel.layout;\n\n");
        sb.append("/**\n");
        sb.append(" * GENERATED by tools/vogen from the Unicode data file VerticalOrientation.txt; do not edit by hand.\n");
        sb.append(" * Code point ranges whose Vertical_Orientation (UAX #50) is U, Tu or Tr, that is every code point that CSS Writing Modes\n");
        sb.append(" * Level 3 section 5.1.2 typesets upright in mixed vertical text (all other code points are R: sideways).\n");
        sb.append(" * Input: ").append(version.length() == 0 ? "(unknown)" : version).append(", ").append(listed).append(" data lines.\n");
        String[] m = meta.split("\n");
        // 출처 줄을 Javadoc 에 옮긴다.
        for (int i = 0; i < m.length; i++) {
            if (m[i].length() > 0) {
                sb.append(" * ").append(m[i]).append('\n');
            }
        }
        sb.append(" * Result: ").append(pairs.size()).append(" ranges, ").append(count).append(" upright code points.\n");
        sb.append(" * <p>\n");
        sb.append(" * 도구 tools/vogen 이 유니코드 데이터 파일 VerticalOrientation.txt 에서 생성한 소스이며 직접 고치지 않는다.\n");
        sb.append(" * Vertical_Orientation(UAX #50)이 U, Tu, Tr 인 코드 포인트 범위, 곧 CSS Writing Modes Level 3 5.1.2 절이 혼합 세로쓰기에서\n");
        sb.append(" * 똑바로 세우는 모든 코드 포인트이다(나머지는 R: 눕힘).\n");
        sb.append(" */\n");
        sb.append("final class VerticalOrientationData {\n\n");
        sb.append("    /** Private constructor: static data class. 비공개 생성자: 정적 데이터 클래스이다. */\n");
        sb.append("    private VerticalOrientationData() {\n    }\n\n");
        sb.append("    /** Space separated hexadecimal from-to pairs, in code point order. 코드 포인트 순서의 16 진수 시작-끝 쌍(공백으로 구분). */\n");
        sb.append("    static final String[] CHUNKS = {\n");
        StringBuilder chunk = new StringBuilder();
        boolean firstChunk = true;
        // 쌍을 한 상수 길이 한계까지 모아 문자열 상수로 낸다.
        for (int i = 0; i < pairs.size(); i++) {
            if (chunk.length() + pairs.get(i).length() + 1 > CHUNK) {
                appendChunk(sb, chunk, firstChunk);
                firstChunk = false;
                chunk.setLength(0);
            }
            if (chunk.length() > 0) {
                chunk.append(' ');
            }
            chunk.append(pairs.get(i));
        }
        if (chunk.length() > 0) {
            appendChunk(sb, chunk, firstChunk);
        }
        sb.append("\n    };\n}\n");
        return sb.toString();
    }

    /**
     * Appends one string constant of the table.
     * <p>
     * 표의 문자열 상수 하나를 덧붙인다.
     *
     * @param sb    the output / 출력
     * @param chunk the text of the constant / 상수의 텍스트
     * @param first true for the first constant / 첫 상수이면 true
     */
    private static void appendChunk(StringBuilder sb, StringBuilder chunk, boolean first) {
        if (!first) {
            sb.append(",\n");
        }
        sb.append("        \"").append(chunk).append('"');
    }

    /**
     * Reads a whole file as UTF-8 text.
     * <p>
     * 파일 전체를 UTF-8 텍스트로 읽는다.
     *
     * @param f the file / 파일
     * @return the text / 텍스트
     * @throws IOException if reading fails / 읽기가 실패할 때
     */
    private static String readText(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 파일 끝까지 읽어 모은다.
            while ((n = in.read(buf)) > 0) {
                bo.write(buf, 0, n);
            }
            return new String(bo.toByteArray(), "UTF-8").replace("\r\n", "\n");
        } finally {
            in.close();
        }
    }

    /**
     * Writes a text file as UTF-8 with LF line ends.
     * <p>
     * 텍스트 파일을 UTF-8 과 LF 줄 끝으로 쓴다.
     *
     * @param f    the file / 파일
     * @param text the text / 텍스트
     * @throws IOException if writing fails / 쓰기가 실패할 때
     */
    private static void writeText(File f, String text) throws IOException {
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }
}
