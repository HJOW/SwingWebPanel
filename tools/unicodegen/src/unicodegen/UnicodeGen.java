package unicodegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Builds the ECMAScript identifier and RegExp Unicode property tables from the versioned Unicode Character Database.
 * <p>
 * 판본이 고정된 Unicode Character Database에서 ECMAScript 식별자와 정규식의 유니코드 속성 표를 생성한다.
 */
public final class UnicodeGen {

    /** First code point outside Unicode. / 유니코드의 범위를 벗어나는 첫 코드 포인트. */
    private static final int LIMIT = 0x110000;

    /** Maximum size of one generated string literal. / 생성한 문자열 리터럴 하나의 최대 크기. */
    private static final int CHUNK = 5000;

    /** Binary properties in ECMA-262 Table 65, subject to EXC-15. / ECMA-262 표 65의 이진 속성 중 EXC-15 대상 외 목록. */
    private static final Set<String> BINARY = new HashSet<String>(Arrays.asList(
            "ASCII", "ASCII_Hex_Digit", "Alphabetic", "Any", "Assigned", "Bidi_Control", "Bidi_Mirrored",
            "Case_Ignorable", "Cased", "Changes_When_Casefolded", "Changes_When_Casemapped", "Changes_When_Lowercased",
            "Changes_When_NFKC_Casefolded", "Changes_When_Titlecased", "Changes_When_Uppercased",
            "Dash", "Default_Ignorable_Code_Point", "Deprecated", "Diacritic", "Extender", "Grapheme_Base",
            "Grapheme_Extend", "Hex_Digit", "IDS_Binary_Operator", "IDS_Trinary_Operator", "ID_Continue", "ID_Start",
            "Ideographic", "Join_Control", "Logical_Order_Exception", "Lowercase", "Math", "Noncharacter_Code_Point",
            "Pattern_Syntax", "Pattern_White_Space", "Quotation_Mark", "Radical", "Sentence_Terminal", "Soft_Dotted",
            "Terminal_Punctuation", "Unified_Ideograph", "Uppercase", "Variation_Selector", "White_Space",
            "XID_Continue", "XID_Start"));

    /** Code points for each canonical property. / 표준 속성별 코드 포인트. */
    private final Map<String, BitSet> tables = new TreeMap<String, BitSet>();

    /** Accepted exact alias spellings and their canonical table keys. / 허용하는 정확한 별칭과 표준 표 키. */
    private final Map<String, String> aliases = new TreeMap<String, String>();

    /** UCD script name or alias to its ISO 15924 code. / UCD 스크립트 이름이나 별칭과 ISO 15924 코드의 대응. */
    private final Map<String, String> scripts = new HashMap<String, String>();

    /** Code points with explicitly supplied Script_Extensions. / Script_Extensions가 명시된 코드 포인트. */
    private final BitSet extensionOverrides = new BitSet();

    /** Full default uppercase mappings. / 기본 전체 대문자 매핑. */
    private final Map<Integer, String> upperCase = new TreeMap<Integer, String>();

    /** Full default lowercase mappings without contextual rules. / 문맥 규칙을 제외한 기본 전체 소문자 매핑. */
    private final Map<Integer, String> lowerCase = new TreeMap<Integer, String>();

    /** Simple case folding entries. / 단순 대소문자 접기 매핑. */
    private final Map<Integer, String> simpleFold = new TreeMap<Integer, String>();

    /** Creates one generator. / 생성기 하나를 만든다. */
    private UnicodeGen() {
    }

    /**
     * Generates a UTF-8 Java source file. Exits with 1 for usage or 2 for invalid input or failed I/O.
     * <p>
     * UTF-8 Java 소스를 생성한다. 사용법 오류면 1, 잘못된 입력이나 입출력 실패면 2로 종료한다.
     *
     * @param args UCD directory and output Java file / UCD 디렉터리와 출력 Java 파일
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("usage: UnicodeGen ucd-directory output-java");
            System.exit(1);
            return;
        }
        File output = new File(args[1]);
        try {
            new UnicodeGen().generate(new File(args[0]), output);
            System.err.println("generated " + output);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    /**
     * Loads input tables, resolves UCD defaults, and writes the data and aliases.
     * <p>
     * 입력 표를 읽고 UCD 기본값을 적용한 뒤 데이터와 별칭을 쓴다.
     *
     * @param input UCD directory / UCD 디렉터리
     * @param output Java file / Java 파일
     * @throws IOException on invalid input or failed I/O / 잘못된 입력 또는 입출력 실패 시
     */
    private void generate(File input, File output) throws IOException {
        checkVersion(new File(input, "ReadMe.txt"));
        readValueAliases(new File(input, "PropertyValueAliases.txt"));
        readUnicodeData(new File(input, "UnicodeData.txt"));
        readSpecialCasing(new File(input, "SpecialCasing.txt"));
        readCaseFolding(new File(input, "CaseFolding.txt"));
        readRanges(new File(input, "Scripts.txt"), "sc=");
        readRanges(new File(input, "ScriptExtensions.txt"), "scx=");
        finishScripts();
        readRanges(new File(input, "PropList.txt"), "");
        readRanges(new File(input, "DerivedCoreProperties.txt"), "");
        readRanges(new File(input, "DerivedNormalizationProps.txt"), "");
        readRanges(new File(input, "DerivedBinaryProperties.txt"), "");
        bits("ASCII").set(0, 0x80);
        bits("Any").set(0, LIMIT);
        readBinaryAliases(new File(input, "PropertyAliases.txt"));
        // 규격이 받는 각 이진 속성이 실제 데이터에 있는지 확인한다.
        for (String property : BINARY) {
            if (!tables.containsKey(property)) {
                throw new IOException("Missing UCD binary property: " + property);
            }
            aliases.put(property, property);
        }
        write(output);
    }

    /**
     * Rejects inputs from a different Unicode release.
     * <p>
     * 다른 유니코드 판본의 입력을 거부한다.
     *
     * @param file UCD ReadMe.txt / UCD ReadMe.txt
     * @throws IOException on a wrong release or failed read / 판본이 다르거나 읽기에 실패한 경우
     */
    private static void checkVersion(File file) throws IOException {
        BufferedReader in = reader(file);
        try {
            String line;
            // UCD 배포본을 식별하는 판본 표기를 찾는다.
            while ((line = in.readLine()) != null) {
                if (line.contains("version 18.0.0")) {
                    return;
                }
            }
        } finally {
            in.close();
        }
        throw new IOException("Expected Unicode 18.0.0 UCD: " + file);
    }

    /**
     * Reads General_Category and Script values and accepts only their exact UCD aliases.
     * <p>
     * General_Category와 Script 값 및 UCD 별칭의 정확한 철자를 읽는다.
     *
     * @param file UCD value aliases / UCD 값 별칭
     * @throws IOException on failed read / 읽기 실패 시
     */
    private void readValueAliases(File file) throws IOException {
        BufferedReader in = reader(file);
        try {
            String line;
            // 각 값의 정식 코드로 정규식이 받는 별칭을 연결한다.
            while ((line = in.readLine()) != null) {
                String[] fields = fields(line);
                if (fields.length < 3) {
                    continue;
                }
                String code = fields[1];
                if ("gc".equals(fields[0])) {
                    bits("gc=" + code);
                    // 일반 범주는 단독 이름이나 접두어 형식으로 지정할 수 있다.
                    for (int i = 1; i < fields.length; i++) {
                        aliases.put(fields[i], "gc=" + code);
                        aliases.put("gc=" + fields[i], "gc=" + code);
                        aliases.put("General_Category=" + fields[i], "gc=" + code);
                    }
                } else if ("sc".equals(fields[0])) {
                    bits("sc=" + code);
                    bits("scx=" + code);
                    // 스크립트는 Script 또는 Script_Extensions 접두어로 지정한다.
                    for (int i = 1; i < fields.length; i++) {
                        scripts.put(fields[i], code);
                        aliases.put("sc=" + fields[i], "sc=" + code);
                        aliases.put("Script=" + fields[i], "sc=" + code);
                        aliases.put("scx=" + fields[i], "scx=" + code);
                        aliases.put("Script_Extensions=" + fields[i], "scx=" + code);
                    }
                }
            }
        } finally {
            in.close();
        }
    }

    /**
     * Reads assigned Unicode categories and constructs the default unassigned category.
     * <p>
     * 유니코드에서 배정된 범주를 읽고 기본 미배정 범주를 만든다.
     *
     * @param file UnicodeData.txt / UnicodeData.txt 파일
     * @throws IOException on malformed input or failed read / 입력 형식 오류나 읽기 실패 시
     */
    private void readUnicodeData(File file) throws IOException {
        BitSet assigned = bits("Assigned");
        BufferedReader in = reader(file);
        try {
            String line;
            int first = -1;
            String firstCategory = null;
            // First/Last 행 한 쌍은 사이의 모든 코드 포인트를 나타낸다.
            while ((line = in.readLine()) != null) {
                String[] fields = line.split(";", -1);
                if (fields.length != 15) {
                    throw new IOException("Invalid UnicodeData row: " + line);
                }
                int cp = Integer.parseInt(fields[0], 16);
                String category = fields[2];
                if (fields[1].endsWith(", First>") && first < 0) {
                    first = cp;
                    firstCategory = category;
                    continue;
                }
                if (fields[1].endsWith(", Last>")) {
                    if (first < 0 || !category.equals(firstCategory)) {
                        throw new IOException("Unmatched UnicodeData range end: " + line);
                    }
                } else if (first >= 0) {
                    throw new IOException("Missing UnicodeData range end: " + line);
                } else {
                    first = cp;
                }
                assigned.set(first, cp + 1);
                bits("gc=" + category).set(first, cp + 1);
                bits("gc=" + category.charAt(0)).set(first, cp + 1);
                if ("Lu".equals(category) || "Ll".equals(category) || "Lt".equals(category)) {
                    bits("gc=LC").set(first, cp + 1);
                }
                addMapping(upperCase, cp, fields[12]);
                addMapping(lowerCase, cp, fields[13]);
                first = -1;
            }
            if (first >= 0) {
                throw new IOException("Unterminated UnicodeData range");
            }
        } finally {
            in.close();
        }
        BitSet unassigned = bits("gc=Cn");
        unassigned.set(0, LIMIT);
        unassigned.andNot(assigned);
        bits("gc=C").or(unassigned);
    }

    /**
     * Overrides simple UnicodeData mappings with unconditional full mappings.
     * <p>
     * UnicodeData의 단순 매핑을 무조건 적용하는 전체 매핑으로 덮어쓴다.
     *
     * @param file SpecialCasing.txt / SpecialCasing.txt 파일
     * @throws IOException on failed read / 읽기 실패 시
     */
    private void readSpecialCasing(File file) throws IOException {
        BufferedReader in = reader(file);
        try {
            String line;
            // 네 필드인 무조건 매핑만 저장한다. 로케일 독립 문맥 규칙은 엔진에서 적용한다.
            while ((line = in.readLine()) != null) {
                String[] fields = fields(line);
                if (fields.length == 4) {
                    int codePoint = Integer.parseInt(fields[0], 16);
                    addMapping(lowerCase, codePoint, fields[1]);
                    addMapping(upperCase, codePoint, fields[3]);
                }
            }
        } finally {
            in.close();
        }
    }

    /**
     * Reads simple Unicode case folding using status C or S.
     * <p>
     * 유니코드 단순 대소문자 접기에 쓰는 C와 S 상태를 읽는다.
     *
     * @param file CaseFolding.txt / CaseFolding.txt 파일
     * @throws IOException on failed read / 읽기 실패 시
     */
    private void readCaseFolding(File file) throws IOException {
        BufferedReader in = reader(file);
        try {
            String line;
            // 전체 접기(F)와 터키어용 접기(T)는 단순 접기 표에서 제외한다.
            while ((line = in.readLine()) != null) {
                String[] fields = fields(line);
                if (fields.length == 3 && ("C".equals(fields[1]) || "S".equals(fields[1]))) {
                    addMapping(simpleFold, Integer.parseInt(fields[0], 16), fields[2]);
                }
            }
        } finally {
            in.close();
        }
    }

    /**
     * Stores a nonidentity code point mapping from a data column.
     * <p>
     * 데이터 열에서 원래 코드 포인트를 바꾸는 매핑을 저장한다.
     *
     * @param map source-to-target map / 원본과 대상 매핑
     * @param cp source code point / 원본 코드 포인트
     * @param target hexadecimal point sequence / 16진수 코드 포인트 열
     */
    private static void addMapping(Map<Integer, String> map, int cp, String target) {
        if (target.length() != 0) {
            if (target.indexOf(' ') < 0 && Integer.parseInt(target, 16) == cp) {
                map.remove(Integer.valueOf(cp));
            } else {
                map.put(Integer.valueOf(cp), target.replace(' ', ','));
            }
        }
    }

    /**
     * Reads script, script extension or supported binary ranges from a UCD data file.
     * <p>
     * UCD 데이터 파일에서 스크립트, 스크립트 확장 또는 지원하는 이진 범위를 읽는다.
     *
     * @param file UCD range file / UCD 범위 파일
     * @param prefix sc=, scx= or empty for a binary table / sc=, scx= 또는 이진 표일 때 빈 문자열
     * @throws IOException on failed read or unknown script / 읽기 실패나 알 수 없는 스크립트 값
     */
    private void readRanges(File file, String prefix) throws IOException {
        BufferedReader in = reader(file);
        try {
            String line;
            // 이름과 범위를 읽는다. 이진 파일에 섞인 열거형 속성은 제외한다.
            while ((line = in.readLine()) != null) {
                String[] fields = fields(line);
                if (fields.length < 2 || (prefix.length() == 0 && !BINARY.contains(fields[1]))) {
                    continue;
                }
                int dot = fields[0].indexOf("..");
                int first = Integer.parseInt(dot < 0 ? fields[0] : fields[0].substring(0, dot), 16);
                int end = dot < 0 ? first + 1 : Integer.parseInt(fields[0].substring(dot + 2), 16) + 1;
                if (prefix.length() == 0) {
                    bits(fields[1]).set(first, end);
                } else {
                    if ("scx=".equals(prefix)) {
                        extensionOverrides.set(first, end);
                    }
                    // Script_Extensions 값은 스크립트 이름 하나 이상을 포함한다.
                    for (String name : fields[1].split("\\s+")) {
                        String code = scripts.get(name);
                        if (code == null) {
                            throw new IOException("Unknown UCD script: " + name + " in " + file);
                        }
                        bits(prefix + code).set(first, end);
                    }
                }
            }
        } finally {
            in.close();
        }
    }

    /**
     * Defaults uncovered script values to Unknown and uncovered extension values to the regular script.
     * <p>
     * 스크립트가 배정되지 않은 값은 Unknown으로, 확장이 없는 값은 기본 스크립트로 처리한다.
     */
    private void finishScripts() {
        Set<String> codes = new TreeSet<String>(scripts.values());
        BitSet unknown = bits("sc=Zzzz");
        unknown.set(0, LIMIT);
        // Script 기본값 Unknown에서 명시적으로 배정된 범위를 뺀다.
        for (String code : codes) {
            if (!"Zzzz".equals(code)) {
                unknown.andNot(bits("sc=" + code));
            }
        }
        // 명시된 확장 범위는 기본 스크립트에 덧붙이는 대신 값을 대체한다.
        for (String code : codes) {
            BitSet inherited = (BitSet) bits("sc=" + code).clone();
            inherited.andNot(extensionOverrides);
            bits("scx=" + code).or(inherited);
        }
    }

    /**
     * Reads exact binary property aliases declared by the UCD and allowed by ECMAScript.
     * <p>
     * ECMAScript가 허용하는 이진 속성의 UCD 별칭을 정확한 철자로 읽는다.
     *
     * @param file UCD property aliases / UCD 속성 별칭
     * @throws IOException on failed read / 읽기 실패 시
     */
    private void readBinaryAliases(File file) throws IOException {
        BufferedReader in = reader(file);
        try {
            String line;
            // 표준 이진 속성에 해당하는 행만 별칭으로 연결한다.
            while ((line = in.readLine()) != null) {
                String[] fields = fields(line);
                if (fields.length >= 2 && BINARY.contains(fields[1])) {
                    // 짧은 표기와 추가 별칭을 정확한 철자로 저장한다.
                    for (String alias : fields) {
                        aliases.put(alias, fields[1]);
                    }
                }
            }
        } finally {
            in.close();
        }
    }

    /**
     * Returns the bit set for a canonical table key, creating it as needed.
     * <p>
     * 표준 키에 맞는 비트 집합을 반환하며 필요하면 새로 만든다.
     *
     * @param key canonical key / 표준 키
     * @return mutable code point set / 변경 가능한 코드 포인트 집합
     */
    private BitSet bits(String key) {
        BitSet b = tables.get(key);
        if (b == null) {
            b = new BitSet();
            tables.put(key, b);
        }
        return b;
    }

    /**
     * Splits a commented UCD row into trimmed semicolon-delimited fields.
     * <p>
     * UCD 행의 주석을 제거하고 세미콜론으로 나눈 필드의 공백을 제거한다.
     *
     * @param line input row / 입력 행
     * @return columns / 열
     */
    private static String[] fields(String line) {
        int comment = line.indexOf('#');
        return (comment < 0 ? line : line.substring(0, comment)).trim().split("\\s*;\\s*");
    }

    /**
     * Writes sorted generated rows, sharing one table across all accepted aliases.
     * <p>
     * 정렬된 생성 행을 쓰고 허용하는 별칭은 한 범위 표를 공유하게 한다.
     *
     * @param output Java file / Java 파일
     * @throws IOException on failed write / 쓰기 실패 시
     */
    private void write(File output) throws IOException {
        File parent = output.getAbsoluteFile().getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create output directory: " + parent);
        }
        StringBuilder source = new StringBuilder();
        source.append("package org.duckdns.hjow.swingwebpanel.js.text;\n\n");
        source.append("/**\n * Generated by tools/unicodegen from Unicode 18.0.0 (2026-11-26). See META-INF/LICENSE-unicode.txt.\n");
        source.append(" * <p>\n * Unicode 18.0.0에서 tools/unicodegen으로 생성했다(2026-11-26). META-INF/LICENSE-unicode.txt 참조.\n */\n");
        source.append("final class UnicodePropertyData {\n\n");
        source.append("    /** Canonical ranges in hexadecimal. / 16진수 표준 범위. */\n");
        source.append("    static final String[] RANGES = {\n");
        List<String> rows = new ArrayList<String>();
        // 같은 이름의 여러 행을 허용하여 각 문자열의 길이를 제한한다.
        for (Map.Entry<String, BitSet> entry : tables.entrySet()) {
            String prefix = entry.getKey().replace('=', '~') + "=";
            StringBuilder row = new StringBuilder(prefix);
            BitSet data = entry.getValue();
            // 연속된 코드 포인트를 닫힌 16진수 범위로 기록한다.
            for (int first = data.nextSetBit(0); first >= 0;) {
                int end = data.nextClearBit(first);
                String range = Integer.toHexString(first) + "-" + Integer.toHexString(end - 1);
                if (row.length() + range.length() + 1 > CHUNK) {
                    rows.add(row.toString());
                    row = new StringBuilder(prefix);
                }
                if (row.length() > prefix.length()) {
                    row.append(',');
                }
                row.append(range);
                first = data.nextSetBit(end);
            }
            rows.add(row.toString());
        }
        appendChunks(source, rows);
        source.append("    };\n\n    /** Exact names and their table keys. / 정확한 이름과 해당 표의 키. */\n");
        source.append("    static final String[] ALIASES = {\n");
        rows.clear();
        // 정규식에서 받는 이름을 해당 표의 키에 연결한다.
        for (Map.Entry<String, String> alias : aliases.entrySet()) {
            rows.add(alias.getKey().replace('=', '~') + "=" + alias.getValue().replace('=', '~'));
        }
        appendChunks(source, rows);
        source.append("    };\n");
        appendMappings(source, "UPPER_CASE", upperCase);
        appendMappings(source, "LOWER_CASE", lowerCase);
        appendMappings(source, "SIMPLE_FOLD", simpleFold);
        source.append("\n    /** Static data holder. / 정적 자료 보관 클래스. */\n    private UnicodePropertyData() { }\n}\n");
        OutputStream out = new FileOutputStream(output);
        try {
            out.write(source.toString().getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    /**
     * Emits a sorted array of hexadecimal case mapping rows.
     * <p>
     * 16진수 대소문자 매핑 행을 정렬한 배열을 출력한다.
     *
     * @param source Java source builder / Java 소스 작성기
     * @param name generated field name / 생성 필드 이름
     * @param mappings Unicode mapping table / 유니코드 매핑 표
     */
    private static void appendMappings(StringBuilder source, String name, Map<Integer, String> mappings) {
        source.append("\n    /** Unicode case mappings. / 유니코드 대소문자 매핑. */\n");
        source.append("    static final String[] ").append(name).append(" = {\n");
        List<String> rows = new ArrayList<String>();
        // 코드 포인트 하나에 대응하는 코드 포인트 열을 쓴다.
        for (Map.Entry<Integer, String> entry : mappings.entrySet()) {
            rows.add(Integer.toHexString(entry.getKey().intValue()) + "=" + entry.getValue());
        }
        appendChunks(source, rows);
        source.append("    };\n");
    }

    /**
     * Emits pipe-separated data rows using bounded Java string constants.
     * <p>
     * 파이프로 구분한 데이터 행을 길이가 제한된 Java 문자열 상수로 출력한다.
     *
     * @param source output source / 출력 소스
     * @param rows data rows / 데이터 행
     */
    private static void appendChunks(StringBuilder source, List<String> rows) {
        StringBuilder chunk = new StringBuilder();
        // 상수 길이 제한 안에 들어가는 만큼 범위 행을 합친다.
        for (String row : rows) {
            if (chunk.length() + row.length() + 1 > CHUNK && chunk.length() > 0) {
                source.append("        \"").append(chunk).append("\",\n");
                chunk.setLength(0);
            }
            if (chunk.length() > 0) {
                chunk.append('|');
            }
            chunk.append(row);
        }
        if (chunk.length() > 0) {
            source.append("        \"").append(chunk).append("\",\n");
        }
    }

    /**
     * Opens a UTF-8 UCD file.
     * <p>
     * UTF-8 UCD 파일을 연다.
     *
     * @param file input / 입력 파일
     * @return the reader / 읽기 객체
     * @throws IOException on failed open / 파일 열기 실패 시
     */
    private static BufferedReader reader(File file) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
    }
}
