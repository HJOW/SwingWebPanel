package entitygen;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Build tool that turns the WHATWG <code>entities.json</code> and the Encoding Standard <code>encodings.json</code> into two Java
 * source files (<code>NamedCharacterData</code> and <code>EncodingLabelData</code>) of the product package
 * <code>org.duckdns.hjow.swingwebpanel.html</code>. The tables are emitted as chunked string constants (a class file constant is limited to 65535
 * bytes and the product build does not copy non-Java resources), so no resource loading is needed at run time.
 * <p>
 * Usage: <code>EntityGen entities.json encodings.json outputDirectory</code>. The <code>.meta</code> file written by the webfetch tool
 * next to each input (if present) provides the source URL, retrieval date and revision recorded in the generated Javadoc.
 * <p>
 * WHATWG <code>entities.json</code> 와 Encoding 표준 <code>encodings.json</code> 을 제품 패키지 <code>org.duckdns.hjow.swingwebpanel.html</code> 의 자바 소스 두 개
 * (<code>NamedCharacterData</code>, <code>EncodingLabelData</code>)로 바꾸는 빌드 도구이다. 표는 조각난 문자열 상수로 만든다(클래스 파일 상수 하나는
 * 65535 바이트로 제한되고 제품 빌드는 자바가 아닌 자원을 복사하지 않는다). 그래서 실행 중 자원을 읽을 필요가 없다.
 * <p>
 * 사용법: <code>EntityGen entities.json encodings.json 출력디렉터리</code>. 각 입력 옆에 webfetch 도구가 쓴 <code>.meta</code> 파일이 있으면
 * 출처 URL, 조회 날짜, 판본을 생성된 Javadoc 에 기록한다.
 */
public final class EntityGen {

    /**
     * Maximum characters per generated string constant (far below the 65535 byte class file limit).
     * <p>
     * 생성되는 문자열 상수 하나의 최대 글자 수(클래스 파일 한계 65535 바이트보다 훨씬 작다).
     */
    private static final int CHUNK = 6000;

    /**
     * Private constructor: static tool class.
     * <p>
     * 비공개 생성자: 정적 도구 클래스이다.
     */
    private EntityGen() {
    }

    /**
     * Entry point.
     * <p>
     * 진입점.
     *
     * @param args entities.json, encodings.json and the output directory / entities.json, encodings.json 과 출력 디렉터리
     * @throws IOException if reading or writing fails / 읽기나 쓰기가 실패할 때
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("usage: EntityGen entities.json encodings.json outputDirectory");
            System.exit(1);
            return;
        }
        File out = new File(args[2]);
        if (!out.isDirectory() && !out.mkdirs()) {
            System.err.println("cannot create " + out);
            System.exit(2);
            return;
        }
        String entityMeta = readMeta(new File(args[0] + ".meta"));
        String encodingMeta = readMeta(new File(args[1] + ".meta"));
        Object entities = MiniJson.parse(readText(new File(args[0])));
        Object encodings = MiniJson.parse(readText(new File(args[1])));
        writeText(new File(out, "NamedCharacterData.java"), generateEntities(entities, entityMeta));
        writeText(new File(out, "EncodingLabelData.java"), generateEncodings(encodings, encodingMeta));
        System.err.println("generated NamedCharacterData.java and EncodingLabelData.java in " + out);
    }

    /**
     * Reads the provenance lines (source-url, retrieved, last-modified, etag) of a meta file.
     * <p>
     * meta 파일에서 출처 줄(source-url, retrieved, last-modified, etag)을 읽는다.
     *
     * @param f meta file / meta 파일
     * @return a one line description, or "source unknown" / 한 줄 설명 또는 "source unknown"
     * @throws IOException if reading fails / 읽기 실패 시
     */
    private static String readMeta(File f) throws IOException {
        if (!f.isFile()) {
            return "source unknown (no .meta file)";
        }
        String[] lines = readText(f).split("\n");
        StringBuilder sb = new StringBuilder();
        // 필요한 항목만 골라 한 줄로 합친다.
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].trim();
            if (l.startsWith("# source-url:") || l.startsWith("# retrieved:") || l.startsWith("# last-modified:") || l.startsWith("# etag:")) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(l.substring(2));
            }
        }
        return sb.length() == 0 ? "source unknown" : sb.toString().replace("\"", "'");
    }

    /**
     * Generates <code>NamedCharacterData</code>.
     * <p>
     * <code>NamedCharacterData</code> 소스를 만든다.
     *
     * @param json parsed entities.json / 해석된 entities.json
     * @param meta provenance / 출처
     * @return Java source / 자바 소스
     */
    @SuppressWarnings("unchecked")
    private static String generateEntities(Object json, String meta) {
        Map<String, Object> map = (Map<String, Object>) json;
        List<String> entries = new ArrayList<String>();
        // 각 참조를 "이름:코드포인트,코드포인트" 형식의 항목으로 만든다(이름 앞의 앰퍼샌드는 뺀다).
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = e.getKey();
            if (!key.startsWith("&")) {
                throw new IllegalArgumentException("unexpected key " + key);
            }
            Map<String, Object> v = (Map<String, Object>) e.getValue();
            List<Object> cps = (List<Object>) v.get("codepoints");
            StringBuilder sb = new StringBuilder(key.substring(1)).append(':');
            // 코드 포인트를 쉼표로 이어 붙인다.
            for (int i = 0; i < cps.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(((Long) cps.get(i)).longValue());
            }
            entries.add(sb.toString());
        }
        StringBuilder src = new StringBuilder();
        header(src, "NamedCharacterData", "named character references of the HTML Standard (entities.json)",
                "HTML 표준의 이름 있는 문자 참조(entities.json)", meta, entries.size() + " entries / 항목");
        chunks(src, "CHUNKS", entries, ' ');
        src.append("}\n");
        return src.toString();
    }

    /**
     * Generates <code>EncodingLabelData</code>.
     * <p>
     * <code>EncodingLabelData</code> 소스를 만든다.
     *
     * @param json parsed encodings.json / 해석된 encodings.json
     * @param meta provenance / 출처
     * @return Java source / 자바 소스
     */
    @SuppressWarnings("unchecked")
    private static String generateEncodings(Object json, String meta) {
        List<Object> groups = (List<Object>) json;
        List<String> entries = new ArrayList<String>();
        // 그룹과 인코딩을 차례로 돌며 "이름=라벨,라벨" 항목을 만든다.
        for (int g = 0; g < groups.size(); g++) {
            Map<String, Object> group = (Map<String, Object>) groups.get(g);
            List<Object> encs = (List<Object>) group.get("encodings");
            // 인코딩마다 항목을 만든다.
            for (int i = 0; i < encs.size(); i++) {
                Map<String, Object> enc = (Map<String, Object>) encs.get(i);
                StringBuilder sb = new StringBuilder((String) enc.get("name")).append('=');
                List<Object> labels = (List<Object>) enc.get("labels");
                // 인코딩마다 라벨을 나열한다.
                for (int k = 0; k < labels.size(); k++) {
                    if (k > 0) {
                        sb.append(',');
                    }
                    sb.append((String) labels.get(k));
                }
                entries.add(sb.toString());
            }
        }
        StringBuilder src = new StringBuilder();
        header(src, "EncodingLabelData", "encoding names and labels of the Encoding Standard (encodings.json)",
                "Encoding 표준의 인코딩 이름과 라벨(encodings.json)", meta, entries.size() + " encodings / 인코딩");
        chunks(src, "CHUNKS", entries, '|');
        src.append("}\n");
        return src.toString();
    }

    /**
     * Writes the class header including the provenance Javadoc.
     * <p>
     * 출처 Javadoc 을 포함한 클래스 머리부를 쓴다.
     *
     * @param src    output / 출력
     * @param name   class name / 클래스 이름
     * @param en     English description / 영어 설명
     * @param ko     Korean description / 한국어 설명
     * @param meta   provenance / 출처
     * @param counts entry count text / 항목 수 문구
     */
    private static void header(StringBuilder src, String name, String en, String ko, String meta, String counts) {
        src.append("package org.duckdns.hjow.swingwebpanel.html;\n\n");
        src.append("/**\n");
        src.append(" * GENERATED FILE - do not edit by hand. Generated by <code>tools/entitygen</code> from ").append(en).append(".\n");
        src.append(" * Source: ").append(meta).append(". Contents: ").append(counts).append(".\n");
        src.append(" * <p>\n");
        src.append(" * 생성된 파일이므로 직접 고치지 않는다. <code>tools/entitygen</code> 이 ").append(ko).append("에서 만들었다.\n");
        src.append(" * 출처: ").append(meta).append(". 내용: ").append(counts).append(".\n");
        src.append(" */\n");
        src.append("final class ").append(name).append(" {\n\n");
        src.append("    /**\n     * Private constructor: constant holder.\n     * <p>\n     * 비공개 생성자: 상수 보관 클래스이다.\n     */\n");
        src.append("    private ").append(name).append("() {\n    }\n\n");
    }

    /**
     * Writes the chunk constants and the array that lists them.
     * <p>
     * 조각 상수와 그것들을 나열하는 배열을 쓴다.
     *
     * @param src     output / 출력
     * @param array   array name / 배열 이름
     * @param entries entries / 항목
     * @param sep     separator character between entries / 항목 사이 구분 문자
     */
    private static void chunks(StringBuilder src, String array, List<String> entries, char sep) {
        List<String> chunks = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        // 항목을 구분 문자로 이어 붙이다가 한 조각이 가득 차면 새 조각을 시작한다.
        for (int i = 0; i < entries.size(); i++) {
            String s = entries.get(i);
            if (cur.length() > 0 && cur.length() + s.length() + 1 > CHUNK) {
                chunks.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) {
                cur.append(sep);
            }
            cur.append(s);
        }
        if (cur.length() > 0) {
            chunks.add(cur.toString());
        }
        // 조각마다 문자열 상수를 쓴다.
        for (int i = 0; i < chunks.size(); i++) {
            src.append("    /**\n     * Data chunk ").append(i).append(" (entries separated by '").append(sep).append("').\n");
            src.append("     * <p>\n     * 데이터 조각 ").append(i).append("(항목은 '").append(sep).append("' 로 구분).\n     */\n");
            src.append("    private static final String C").append(i).append(" = \"").append(escape(chunks.get(i))).append("\";\n\n");
        }
        src.append("    /**\n     * All chunks in order; the chunks are separated from each other by the character '").append(sep).append("'.\n");
        src.append("     * <p>\n     * 모든 조각(순서대로). 조각 사이는 구분 문자 '").append(sep).append("' 로 이어진다.\n     */\n");
        src.append("    static final String[] ").append(array).append(" = {");
        // 조각 상수의 이름을 배열 초기화식에 나열한다.
        for (int i = 0; i < chunks.size(); i++) {
            if (i > 0) {
                src.append(", ");
            }
            src.append('C').append(i);
        }
        src.append("};\n");
    }

    /**
     * Escapes a string for a Java string literal (the data is ASCII).
     * <p>
     * 자바 문자열 리터럴용으로 이스케이프한다(데이터는 ASCII 이다).
     *
     * @param s text / 문자열
     * @return escaped text / 이스케이프된 문자열
     */
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        // 따옴표와 역슬래시를 이스케이프하고 비 ASCII 문자는 거부한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 0x20 || c > 0x7e) {
                throw new IllegalArgumentException("non ASCII data: " + (int) c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Reads a file as UTF-8 text.
     * <p>
     * 파일을 UTF-8 문자열로 읽는다.
     *
     * @param f file / 파일
     * @return text / 문자열
     * @throws IOException if reading fails / 읽기 실패 시
     */
    private static String readText(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 파일 끝까지 읽어 모은다.
            while ((n = in.read(buf)) >= 0) {
                bo.write(buf, 0, n);
            }
            return new String(bo.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    /**
     * Writes UTF-8 text.
     * <p>
     * UTF-8 문자열을 쓴다.
     *
     * @param f    file / 파일
     * @param text text / 문자열
     * @throws IOException if writing fails / 쓰기 실패 시
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
