package unicodegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Java 6 build tool that converts the Unicode 13.0 UCD range files into the regular-expression property's generated
 * Java table. The generated source is used by the engine without reading files or depending on the host JRE's Unicode
 * database.
 * <p>
 * Unicode 13.0 UCD 구간 파일을 정규식 엔진의 생성 Java 표로 바꾸는 Java 6 빌드 도구이다. 생성된 소스는 실행 중 파일을 읽거나
 * 호스트 JRE 의 유니코드 데이터베이스에 의존하지 않는다.
 */
public final class UnicodeGen {

    /**
     * Maximum characters in one generated Java string constant.
     * <p>
     * 생성 Java 문자열 상수 하나의 최대 글자 수.
     */
    private static final int CHUNK = 5000;

    /**
     * Property range table.
     * <p>
     * 속성별 구간 표.
     */
    private final Map<String, List<Range>> tables = new HashMap<String, List<Range>>();

    /**
     * General category alias to short canonical value.
     * <p>
     * General_Category 별칭과 짧은 표준 값의 대응표.
     */
    private final Map<String, String> categoryAliases = new HashMap<String, String>();

    /**
     * Script alias to canonical long name.
     * <p>
     * Script 별칭과 표준 긴 이름의 대응표.
     */
    private final Map<String, String> scriptAliases = new HashMap<String, String>();

    /**
     * Private constructor: one generator instance owns the parsed maps.
     * <p>
     * 비공개 생성자: 생성기 하나가 해석한 표를 소유한다.
     */
    private UnicodeGen() {
    }

    /**
     * Entry point.
     * <p>
     * 진입점.
     *
     * @param args input UCD directory and output Java source / UCD 입력 디렉터리와 출력 Java 소스
     * @throws IOException if a file operation fails / 파일 작업이 실패하면
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("usage: UnicodeGen ucd-directory output-java");
            System.exit(1);
            return;
        }
        File dir = new File(args[0]);
        File output = new File(args[1]);
        if (!dir.isDirectory()) {
            System.err.println("not a directory: " + dir);
            System.exit(2);
            return;
        }
        File parent = output.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            System.err.println("cannot create output directory: " + parent);
            System.exit(2);
            return;
        }
        UnicodeGen generator = new UnicodeGen();
        generator.readAliases(new File(dir, "PropertyValueAliases.txt"));
        generator.readUnicodeData(new File(dir, "UnicodeData.txt"));
        generator.readRangeFile(new File(dir, "Scripts.txt"), true);
        generator.readRangeFile(new File(dir, "ScriptExtensions.txt"), true);
        generator.readRangeFile(new File(dir, "PropList.txt"), false);
        generator.readRangeFile(new File(dir, "DerivedCoreProperties.txt"), false);
        generator.addAliases();
        generator.write(output);
        System.err.println("generated " + output + " (" + generator.tables.size() + " property aliases)");
    }

    /**
     * Reads UnicodeData.txt category assignments.
     * <p>
     * UnicodeData.txt 의 범주 배정을 읽는다.
     *
     * @param file input / 입력
     * @throws IOException on read failure / 읽기 실패 시
     */
    private void readUnicodeData(File file) throws IOException {
        BufferedReader in = reader(file);
        try {
            String line;
            int pendingFirst = -1;
            // 각 UCD 행의 첫 코드 포인트와 세 번째 필드(일반 범주)를 읽는다.
            while ((line = in.readLine()) != null) {
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split(";", -1);
                if (fields.length < 3) {
                    throw new IOException("bad UnicodeData row: " + line);
                }
                int cp = Integer.parseInt(fields[0], 16);
                String category = fields[2].toLowerCase();
                int first = cp;
                if (fields[1].endsWith(", First>")) {
                    pendingFirst = cp;
                    continue;
                }
                if (fields[1].endsWith(", Last>")) {
                    if (pendingFirst < 0) {
                        throw new IOException("UnicodeData Last without First: " + line);
                    }
                    first = pendingFirst;
                    pendingFirst = -1;
                } else if (pendingFirst >= 0) {
                    throw new IOException("UnicodeData First not followed by Last: " + line);
                }
                add("gc=" + category, first, cp);
                add("gc=" + category.substring(0, 1), first, cp);
                if ("lu".equals(category) || "ll".equals(category) || "lt".equals(category)) {
                    add("gc=lc", first, cp);
                }
            }
            if (pendingFirst >= 0) {
                throw new IOException("UnicodeData First has no Last");
            }
        } finally {
            in.close();
        }
    }

    /**
     * Reads a UCD range file whose payload is a property name.
     * <p>
     * 본문이 속성 이름인 UCD 구간 파일을 읽는다.
     *
     * @param file input / 입력
     * @param script true for script files / Script 파일이면 true
     * @throws IOException on read failure / 읽기 실패 시
     */
    private void readRangeFile(File file, boolean script) throws IOException {
        BufferedReader in = reader(file);
        try {
            String line;
            // 구간 파일의 모든 속성 행을 읽어 코드 포인트 범위를 추가한다.
            while ((line = in.readLine()) != null) {
                int comment = line.indexOf('#');
                if (comment >= 0) {
                    line = line.substring(0, comment);
                }
                line = line.trim();
                if (line.length() == 0 || line.indexOf(';') < 0) {
                    continue;
                }
                String[] fields = line.split(";", 2);
                int[] bounds = bounds(fields[0].trim());
                String property = fields[1].trim();
                if (script) {
                    String source = file.getName().toLowerCase().indexOf("extension") >= 0 ? "scx=" : "sc=";
                    String[] values = property.split("\\s+");
                    // ScriptExtensions 항목 하나가 여러 Script 값을 가질 수 있다.
                    for (int i = 0; i < values.length; i++) {
                        String alias = compact(values[i]);
                        String canonical = scriptAliases.get(alias);
                        add(source + (canonical == null ? alias : canonical), bounds[0], bounds[1]);
                    }
                } else {
                    add(compact(property), bounds[0], bounds[1]);
                }
            }
        } finally {
            in.close();
        }
    }

    /**
     * Reads PropertyValueAliases.txt.
     * <p>
     * PropertyValueAliases.txt 를 읽는다.
     *
     * @param file input / 입력
     * @throws IOException on read failure / 읽기 실패 시
     */
    private void readAliases(File file) throws IOException {
        BufferedReader in = reader(file);
        try {
            String line;
            // 별칭 파일의 모든 값 행을 읽어 표준값과 별칭을 연결한다.
            while ((line = in.readLine()) != null) {
                int comment = line.indexOf('#');
                if (comment >= 0) {
                    line = line.substring(0, comment);
                }
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                String[] fields = line.split(";");
                if (fields.length < 3) {
                    continue;
                }
                String kind = compact(fields[0]);
                String canonical = compact(fields[1]);
                // 같은 행에 있는 짧은 이름과 긴 이름을 모두 별칭으로 기록한다.
                for (int i = 1; i < fields.length; i++) {
                    String alias = compact(fields[i]);
                    if ("gc".equals(kind)) {
                        categoryAliases.put(alias, canonical);
                    } else if ("sc".equals(kind)) {
                        scriptAliases.put(alias, canonical);
                    }
                }
            }
        } finally {
            in.close();
        }
    }

    /**
     * Adds General_Category and Script aliases to their canonical tables.
     * <p>
     * General_Category 와 Script 별칭을 표준 표에 추가한다.
     */
    private void addAliases() {
        addAliasFamily("gc=", categoryAliases);
        addAliasFamily("sc=", scriptAliases);
        addAliasFamily("script=", scriptAliases);
        addAliasFamily("scx=", scriptAliases);
        addRawCategoryAliases();
        List<String> scriptKeys = new ArrayList<String>();
        // Script 표 키를 모아 Script_Extensions가 없는 항목에 기본 표를 연결한다.
        for (Iterator<String> it = tables.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            if (key.startsWith("sc=")) {
                scriptKeys.add(key);
            }
        }
        // 모은 Script 키마다 같은 범위를 scx 키로 복사한다.
        for (int i = 0; i < scriptKeys.size(); i++) {
            String key = scriptKeys.get(i);
            String scx = "scx=" + key.substring(3);
            if (tables.get(scx) == null) {
                copy(key, scx);
            }
        }
    }

    /**
     * Adds aliases with a property prefix.
     * <p>
     * 속성 접두사가 붙은 별칭을 추가한다.
     *
     * @param prefix key prefix / 키 접두사
     * @param aliases aliases / 별칭
     */
    private void addAliasFamily(String prefix, Map<String, String> aliases) {
        List<String> values = new ArrayList<String>(aliases.keySet());
        // 각 별칭이 가리키는 표준 속성 표를 새 이름으로 복사한다.
        for (int i = 0; i < values.size(); i++) {
            String alias = values.get(i);
            String canonical = aliases.get(alias);
            String from = prefix + canonical;
            String to = prefix + alias;
            if (tables.get(from) != null) {
                copy(from, to);
            }
        }
    }

    /**
     * Adds lone General_Category aliases accepted by Unicode property escapes.
     * <p>
     * 유니코드 속성 escape 가 받는 단독 General_Category 별칭을 추가한다.
     */
    private void addRawCategoryAliases() {
        List<String> values = new ArrayList<String>(categoryAliases.keySet());
        // 단독으로 쓸 수 있는 General_Category 별칭을 추가한다.
        for (int i = 0; i < values.size(); i++) {
            String alias = values.get(i);
            copy("gc=" + categoryAliases.get(alias), alias);
        }
    }

    /**
     * Copies one table.
     * <p>
     * 표 하나를 복사한다.
     *
     * @param from source key / 원본 키
     * @param to target key / 대상 키
     */
    private void copy(String from, String to) {
        List<Range> values = tables.get(from);
        if (values == null) {
            return;
        }
        tables.put(to, new ArrayList<Range>(values));
    }

    /**
     * Adds a code point range.
     * <p>
     * 코드 포인트 구간을 추가한다.
     *
     * @param key property key / 속성 키
     * @param first first code point / 첫 코드 포인트
     * @param last last code point / 마지막 코드 포인트
     */
    private void add(String key, int first, int last) {
        List<Range> list = tables.get(key);
        if (list == null) {
            list = new ArrayList<Range>();
            tables.put(key, list);
        }
        list.add(new Range(first, last));
    }

    /**
     * Parses a UCD code point or code point range.
     * <p>
     * UCD 코드 포인트 또는 구간을 해석한다.
     *
     * @param text range text / 구간 문자열
     * @return first and last / 첫 값과 끝 값
     */
    private static int[] bounds(String text) {
        int dot = text.indexOf("..");
        if (dot < 0) {
            int one = Integer.parseInt(text, 16);
            return new int[] { one, one };
        }
        return new int[] { Integer.parseInt(text.substring(0, dot), 16), Integer.parseInt(text.substring(dot + 2), 16) };
    }

    /**
     * Normalizes a Unicode property name for generated lookup.
     * <p>
     * 생성 표 조회용으로 유니코드 속성 이름을 정규화한다.
     *
     * @param text property / 속성
     * @return normalized value / 정규화한 값
     */
    private static String compact(String text) {
        String value = text.trim().toLowerCase();
        StringBuilder out = new StringBuilder();
        // 밑줄·공백·하이픈을 빼 ECMAScript 속성 이름 비교 형태로 만든다.
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '_' && c != ' ' && c != '\t' && c != '-') {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Merges overlapping and adjacent ranges.
     * <p>
     * 겹치거나 이웃한 구간을 합친다.
     *
     * @param ranges ranges / 구간
     * @return merged ranges / 합친 구간
     */
    private static List<Range> merge(List<Range> ranges) {
        Collections.sort(ranges, new Comparator<Range>() {
            public int compare(Range a, Range b) {
                return a.first < b.first ? -1 : (a.first > b.first ? 1 : (a.last - b.last));
            }
        });
        List<Range> out = new ArrayList<Range>();
        // 정렬된 구간을 합쳐 겹치거나 맞닿은 범위를 하나로 만든다.
        for (int i = 0; i < ranges.size(); i++) {
            Range next = ranges.get(i);
            if (out.isEmpty() || out.get(out.size() - 1).last + 1 < next.first) {
                out.add(new Range(next.first, next.last));
            } else if (next.last > out.get(out.size() - 1).last) {
                out.get(out.size() - 1).last = next.last;
            }
        }
        return out;
    }

    /**
     * Writes the generated Java source.
     * <p>
     * 생성 Java 소스를 쓴다.
     *
     * @param output output source / 출력 소스
     * @throws IOException on write failure / 쓰기 실패 시
     */
    private void write(File output) throws IOException {
        List<String> keys = new ArrayList<String>(tables.keySet());
        Collections.sort(keys);
        List<String> entries = new ArrayList<String>();
        // 속성 이름 순서로 범위를 항목 문자열로 만든다.
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String encodedKey = key.replace("=", "~");
            List<Range> ranges = merge(tables.get(key));
            StringBuilder entry = new StringBuilder(encodedKey).append('=');
            StringBuilder current = entry;
            // 긴 속성은 같은 이름의 여러 항목으로 나눠 클래스 상수 한도를 지킨다.
            for (int r = 0; r < ranges.size(); r++) {
                String token = Integer.toHexString(ranges.get(r).first) + "-" + Integer.toHexString(ranges.get(r).last);
                if (current.length() > key.length() + 1 && current.length() + token.length() + 1 > CHUNK) {
                    entries.add(current.toString());
                    current = new StringBuilder(encodedKey).append('=');
                }
                if (current.charAt(current.length() - 1) != '=') {
                    current.append(',');
                }
                current.append(token);
            }
            entries.add(current.toString());
        }
        List<String> chunks = new ArrayList<String>();
        StringBuilder chunk = new StringBuilder();
        // 항목을 5000자 이하의 조각에 담아 상수로 만든다.
        for (int i = 0; i < entries.size(); i++) {
            String entry = entries.get(i);
            if (chunk.length() + entry.length() + 1 > CHUNK && chunk.length() > 0) {
                chunks.add(chunk.toString());
                chunk = new StringBuilder();
            }
            if (chunk.length() > 0) {
                chunk.append('|');
            }
            chunk.append(entry);
        }
        if (chunk.length() > 0) {
            chunks.add(chunk.toString());
        }
        StringBuilder source = new StringBuilder();
        source.append("package org.duckdns.hjow.swingwebpanel.js.regexp;\n\n");
        source.append("import java.util.HashMap;\nimport java.util.Map;\n\n");
        source.append("/**\n * GENERATED FILE - do not edit. Generated by tools/unicodegen from Unicode 13.0.0 UCD\n");
        source.append(" * files (UnicodeData.txt, Scripts.txt, ScriptExtensions.txt, PropList.txt, DerivedCoreProperties.txt and\n");
        source.append(" * PropertyValueAliases.txt), retrieved 2026-11-25 from unicode.org. Emoji 13 properties are intentionally excluded.\n * <p>\n");
        source.append(" * 생성 파일이다. Unicode 13.0.0 UCD 파일에서 tools/unicodegen 이 만들었다. 이모지 13 속성은 의도적으로 뺐다.\n */\n");
        source.append("final class UnicodePropertyData {\n");
        source.append("    /** Ranges by compact property name. / 축약 속성 이름별 구간. */\n");
        source.append("    private static final Map<String, int[]> RANGES = new HashMap<String, int[]>();\n\n");
        // 각 조각을 영어와 한국어 설명이 있는 문자열 상수로 쓴다.
        for (int i = 0; i < chunks.size(); i++) {
            source.append("    /** Generated data chunk ").append(i).append(". / 생성 데이터 조각 ").append(i).append(". */\n");
            source.append("    private static final String C").append(i).append(" = \"").append(escape(chunks.get(i))).append("\";\n\n");
        }
        source.append("    static { buildTables(); }\n\n");
        source.append("    /** Private constructor. / 비공개 생성자. */\n    private UnicodePropertyData() { }\n\n");
        source.append("    /** Returns copied ranges, or null. / 복사한 구간 또는 null 을 반환한다. @param name name / 이름 @return ranges / 구간 */\n");
        source.append("    static int[] ranges(String name) { int[] r = RANGES.get(name.replace('=', '~')); return r == null ? null : r.clone(); }\n\n");
        source.append("    /** Decodes generated chunks. / 생성 조각을 해석한다. */\n    private static void buildTables() {\n");
        // 생성된 조각 상수를 순서대로 해석기에 넘긴다.
        for (int i = 0; i < chunks.size(); i++) {
            source.append("        addChunk(C").append(i).append(");\n");
        }
        source.append("    }\n\n");
        source.append("    /** Decodes one chunk. / 조각 하나를 해석한다. @param chunk chunk / 조각 */\n");
        source.append("    private static void addChunk(String chunk) {\n");
        source.append("        int from = 0;\n");
        source.append("        // 조각 안의 속성 항목을 '|' 경계로 차례로 읽는다.\n");
        source.append("        while (from <= chunk.length()) {\n");
        source.append("            int separator = chunk.indexOf('|', from);\n");
        source.append("            String entry = chunk.substring(from, separator < 0 ? chunk.length() : separator);\n");
        source.append("            int eq = entry.indexOf('=');\n");
        source.append("            String key = entry.substring(0, eq);\n");
        source.append("            String[] pieces = entry.substring(eq + 1).split(\",\");\n");
        source.append("            int[] old = RANGES.get(key);\n");
        source.append("            int oldLength = old == null ? 0 : old.length;\n");
        source.append("            int[] next = new int[oldLength + pieces.length * 2];\n");
        source.append("            if (old != null) System.arraycopy(old, 0, next, 0, oldLength);\n");
        source.append("            // 각 시작·끝 구간을 16진 정수로 해석해 앞 조각 뒤에 이어 붙인다.\n");
        source.append("            for (int p = 0; p < pieces.length; p++) {\n");
        source.append("                int dash = pieces[p].indexOf('-');\n");
        source.append("                if (dash < 1) throw new IllegalArgumentException(\"bad range \" + pieces[p] + \" in \" + entry);\n");
        source.append("                next[oldLength + 2 * p] = Integer.parseInt(pieces[p].substring(0, dash), 16);\n");
        source.append("                next[oldLength + 2 * p + 1] = Integer.parseInt(pieces[p].substring(dash + 1), 16);\n");
        source.append("            }\n");
        source.append("            RANGES.put(key, next);\n");
        source.append("            if (separator < 0) break;\n");
        source.append("            from = separator + 1;\n");
        source.append("        }\n");
        source.append("    }\n");
        source.append("}\n");
        OutputStream out = new FileOutputStream(output);
        try {
            out.write(source.toString().getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    /**
     * Escapes a generated Java literal.
     * <p>
     * 생성 Java 문자열 리터럴을 이스케이프한다.
     *
     * @param text text / 문자열
     * @return escaped text / 이스케이프한 문자열
     */
    private static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        // Java 문자열 리터럴의 따옴표와 역슬래시를 이스케이프한다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' || c == '"') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * Opens UTF-8 text.
     * <p>
     * UTF-8 텍스트를 연다.
     *
     * @param file file / 파일
     * @return reader / reader
     * @throws IOException on open failure / 열기 실패 시
     */
    private static BufferedReader reader(File file) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
    }

    /**
     * Inclusive integer range.
     * <p>
     * 닫힌 정수 구간.
     */
    private static final class Range {
        /**
         * First code point.
         * <p>
         * 첫 코드 포인트.
         */
        int first;
        /**
         * Last code point.
         * <p>
         * 마지막 코드 포인트.
         */
        int last;

        /**
         * Creates a range.
         * <p>
         * 구간을 만든다.
         *
         * @param a first / 첫 값
         * @param b last / 마지막 값
         */
        Range(int a, int b) {
            first = a;
            last = b;
        }
    }
}
