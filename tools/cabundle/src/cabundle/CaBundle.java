package cabundle;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Converts the "Mozilla Included CA Certificate Report" of the Common CA Database (CCADB) into the root list resource of the TLS module
 * (plan item P32-04-c). Input: the CSV report (fetched with tools/webfetch from
 * <code>https://ccadb.my.salesforce-sites.com/mozilla/IncludedCACertificateReportPEMCSV</code>). Output: a UTF-8 text file with one line
 * per root that Mozilla trusts for web sites, with its distrust date and applied name constraints. The same input always gives the same
 * output. Run: <code>java -cp build/classes cabundle.CaBundle REPORT.csv OUTPUT.txt SOURCE_URL RETRIEVED_DATE</code>. Exit code:
 * 0 = written, 1 = usage error, 2 = the report cannot be read or has an unexpected form.
 * <p>
 * Common CA Database(CCADB)의 "Mozilla Included CA Certificate Report" 를 TLS 모듈의 루트 목록 자료로 바꾼다(계획 P32-04-c). 입력: CSV 보고서
 * (tools/webfetch 로 <code>https://ccadb.my.salesforce-sites.com/mozilla/IncludedCACertificateReportPEMCSV</code> 에서 받은 것). 출력: Mozilla 가 웹
 * 사이트용으로 신뢰하는 루트마다 한 줄씩, 불신 날짜와 적용 이름 제약을 함께 적은 UTF-8 텍스트 파일. 같은 입력은 항상 같은 출력을 낸다.
 * 실행: <code>java -cp build/classes cabundle.CaBundle 보고서.csv 출력.txt 원본URL 조회날짜</code>. 종료 코드: 0 = 기록함, 1 = 사용법 오류,
 * 2 = 보고서를 읽을 수 없거나 형식이 예상과 다름.
 */
public final class CaBundle {

    /** The column with the display name of the root. / 루트의 표시 이름 열. */
    private static final String COL_NAME = "Common Name or Certificate Name";

    /** The column with the SHA-256 fingerprint of the certificate. / 인증서의 SHA-256 지문 열. */
    private static final String COL_FINGERPRINT = "SHA-256 Fingerprint";

    /** The column with the trust bits. / 신뢰 비트 열. */
    private static final String COL_TRUST = "Trust Bits";

    /** The column with the distrust date for TLS. / TLS 불신 날짜 열. */
    private static final String COL_DISTRUST = "Distrust for TLS After Date";

    /** The column with the constraints that Mozilla applies. / Mozilla 가 적용하는 제약 열. */
    private static final String COL_CONSTRAINTS = "Mozilla Applied Constraints";

    /** The column with the certificate in PEM form. / PEM 형식 인증서 열. */
    private static final String COL_PEM = "PEM Info";

    /**
     * Private constructor: the class has only static methods.
     * <p>
     * 비공개 생성자: 이 클래스에는 정적 메서드만 있다.
     */
    private CaBundle() {
    }

    /**
     * Entry point.
     * <p>
     * 진입점.
     *
     * @param args report, output, source URL, retrieval date / 보고서, 출력, 원본 URL, 조회 날짜
     */
    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("usage: CaBundle REPORT.csv OUTPUT.txt SOURCE_URL RETRIEVED_DATE");
            System.exit(1);
        }
        try {
            int count = convert(args[0], args[1], args[2], args[3]);
            System.err.println("# roots: " + count);
        } catch (IOException e) {
            System.err.println("[ERROR] " + e.getMessage());
            System.exit(2);
        }
    }

    /**
     * Reads a whole file.
     * <p>
     * 파일 전체를 읽는다.
     *
     * @param path the file / 파일
     * @return the bytes / 바이트
     * @throws IOException on a read failure / 읽기 실패 시
     */
    private static byte[] readAll(String path) throws IOException {
        InputStream in = new FileInputStream(path);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 파일 끝까지 읽는다.
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Computes SHA-256 and returns it as upper case hexadecimal.
     * <p>
     * SHA-256 을 계산해 대문자 16진수로 반환한다.
     *
     * @param data the data / 데이터
     * @return the digest / 다이제스트
     * @throws IOException if the JRE has no SHA-256 / JRE 에 SHA-256 이 없으면
     */
    private static String sha256(byte[] data) throws IOException {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder();
            // 바이트마다 두 글자를 쓴다.
            for (int i = 0; i < d.length; i++) {
                sb.append(Character.toUpperCase(Character.forDigit((d[i] >> 4) & 15, 16))).append(Character.toUpperCase(Character.forDigit(d[i] & 15, 16)));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("no SHA-256: " + e);
        }
    }

    /**
     * Parses CSV text (RFC 4180): fields in double quotes may contain commas, line breaks and doubled quotes.
     * <p>
     * CSV 텍스트를 해석한다(RFC 4180): 큰따옴표 안의 필드에는 쉼표, 줄바꿈, 겹친 따옴표가 들어갈 수 있다.
     *
     * @param text the text / 텍스트
     * @return the records / 레코드 목록
     */
    static List<List<String>> parseCsv(String text) {
        List<List<String>> records = new ArrayList<List<String>>();
        List<String> record = new ArrayList<String>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean any = false;
        // 글자를 하나씩 보며 필드와 레코드를 나눈다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
                any = true;
            } else if (c == ',') {
                record.add(field.toString());
                field.setLength(0);
                any = true;
            } else if (c == '\n' || c == '\r') {
                if (any || field.length() > 0) {
                    record.add(field.toString());
                    records.add(record);
                    record = new ArrayList<String>();
                    field.setLength(0);
                    any = false;
                }
            } else {
                field.append(c);
                any = true;
            }
        }
        if (any || field.length() > 0) {
            record.add(field.toString());
            records.add(record);
        }
        return records;
    }

    /**
     * Decodes Base64 text, ignoring white space.
     * <p>
     * 공백을 무시하고 Base64 텍스트를 푼다.
     *
     * @param text the text / 텍스트
     * @return the bytes / 바이트
     * @throws IOException if a character is not Base64 / Base64 가 아닌 글자가 있으면
     */
    private static byte[] base64(String text) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int acc = 0;
        int bits = 0;
        // 글자마다 6비트를 모아 8비트가 차면 바이트를 낸다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '=' || Character.isWhitespace(c)) {
                continue;
            }
            int v = c >= 'A' && c <= 'Z' ? c - 'A' : c >= 'a' && c <= 'z' ? c - 'a' + 26 : c >= '0' && c <= '9' ? c - '0' + 52 : c == '+' ? 62 : c == '/' ? 63 : -1;
            if (v < 0) {
                throw new IOException("not Base64: " + c);
            }
            acc = (acc << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out.write((acc >> bits) & 0xff);
            }
        }
        return out.toByteArray();
    }

    /**
     * Encodes bytes as Base64 on one line.
     * <p>
     * 바이트를 한 줄의 Base64 로 부호화한다.
     *
     * @param data the bytes / 바이트
     * @return the text / 텍스트
     */
    private static String toBase64(byte[] data) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        StringBuilder sb = new StringBuilder();
        // 세 바이트씩 네 글자로 바꾼다.
        for (int i = 0; i < data.length; i += 3) {
            int b0 = data[i] & 0xff;
            int b1 = i + 1 < data.length ? data[i + 1] & 0xff : 0;
            int b2 = i + 2 < data.length ? data[i + 2] & 0xff : 0;
            sb.append(alphabet.charAt(b0 >> 2)).append(alphabet.charAt(((b0 & 3) << 4) | (b1 >> 4)));
            sb.append(i + 1 < data.length ? alphabet.charAt(((b1 & 15) << 2) | (b2 >> 6)) : '=');
            sb.append(i + 2 < data.length ? alphabet.charAt(b2 & 63) : '=');
        }
        return sb.toString();
    }

    /**
     * Converts the applied constraints of the report into a comma separated list of DNS subtrees, or a minus sign for none.
     * <p>
     * 보고서의 적용 제약을 쉼표로 나눈 DNS 하위 트리 목록으로 바꾼다. 없으면 빼기 기호.
     *
     * @param text the cell of the report / 보고서의 칸
     * @return the list / 목록
     * @throws IOException if the cell has a form that this tool does not know / 이 도구가 모르는 형식이면
     */
    static String constraints(String text) throws IOException {
        String t = text.trim();
        if (t.length() == 0 || t.toLowerCase().startsWith("no name constraints")) {
            return "-";
        }
        String[] parts = t.split("[;,\\s]+");
        StringBuilder sb = new StringBuilder();
        // 항목마다 "*.tr" 같은 DNS 하위 트리인지 확인한다.
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i].toLowerCase();
            if (p.length() == 0) {
                continue;
            }
            if (!p.matches("(\\*\\.)?[a-z0-9]([a-z0-9.-]*[a-z0-9])?")) {
                throw new IOException("unknown form of applied constraints: " + text);
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(p);
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    /**
     * Converts the report.
     * <p>
     * 보고서를 변환한다.
     *
     * @param report    the CSV file / CSV 파일
     * @param output    the output file / 출력 파일
     * @param sourceUrl the URL that the report came from / 보고서를 받은 URL
     * @param retrieved the day it was retrieved (yyyy-mm-dd) / 받은 날짜(yyyy-mm-dd)
     * @return the number of roots written / 기록한 루트 수
     * @throws IOException if the report cannot be read or has an unexpected form / 보고서를 읽을 수 없거나 형식이 예상과 다르면
     */
    static int convert(String report, String output, String sourceUrl, String retrieved) throws IOException {
        byte[] raw = readAll(report);
        List<List<String>> records = parseCsv(new String(raw, "UTF-8"));
        if (records.size() < 2) {
            throw new IOException("the report has no rows");
        }
        List<String> head = records.get(0);
        int name = head.indexOf(COL_NAME);
        int fingerprint = head.indexOf(COL_FINGERPRINT);
        int trust = head.indexOf(COL_TRUST);
        int distrust = head.indexOf(COL_DISTRUST);
        int applied = head.indexOf(COL_CONSTRAINTS);
        int pem = head.indexOf(COL_PEM);
        if (name < 0 || fingerprint < 0 || trust < 0 || distrust < 0 || applied < 0 || pem < 0) {
            throw new IOException("the report lacks an expected column: " + head);
        }
        List<String[]> roots = new ArrayList<String[]>();
        // 행마다 웹 사이트 신뢰 비트가 있는 루트를 고른다.
        for (int r = 1; r < records.size(); r++) {
            List<String> row = records.get(r);
            if (row.size() != head.size()) {
                throw new IOException("row " + r + " has " + row.size() + " fields instead of " + head.size());
            }
            if (row.get(trust).indexOf("Websites") < 0) {
                continue;
            }
            String p = row.get(pem);
            int begin = p.indexOf("-----BEGIN CERTIFICATE-----");
            int end = p.indexOf("-----END CERTIFICATE-----");
            if (begin < 0 || end < begin) {
                throw new IOException("row " + r + " has no PEM certificate");
            }
            byte[] der = base64(p.substring(begin + 27, end));
            String print = sha256(der);
            if (!print.equalsIgnoreCase(row.get(fingerprint).replace(":", "").trim())) {
                throw new IOException("row " + r + ": the certificate does not have the fingerprint of the report");
            }
            String d = row.get(distrust).trim();
            if (d.length() > 0) {
                if (!d.matches("\\d{4}\\.\\d{2}\\.\\d{2}")) {
                    throw new IOException("row " + r + ": unknown form of the distrust date: " + d);
                }
                d = d.replace('.', '-');
            } else {
                d = "-";
            }
            String label = row.get(name).replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
            roots.add(new String[] { print, d, constraints(row.get(applied)), label, toBase64(der) });
        }
        if (roots.isEmpty()) {
            throw new IOException("the report has no root with the Websites trust bit");
        }
        // 원본의 행 순서와 무관하게 지문 순으로 정렬한다.
        Collections.sort(roots, new Comparator<String[]>() {
            /**
             * Compares by fingerprint.
             * <p>
             * 지문으로 비교한다.
             *
             * @param a first root / 첫 루트
             * @param b second root / 둘째 루트
             * @return the order / 순서
             */
            public int compare(String[] a, String[] b) {
                return a[0].compareTo(b[0]);
            }
        });
        Writer w = new OutputStreamWriter(new FileOutputStream(output), "UTF-8");
        try {
            w.write("# Root certificates that Mozilla trusts for web sites (trust bit \"Websites\"), for the swingwebpanel TLS module.\n");
            w.write("# Generated by tools/cabundle. Do not edit; this list is NOT updated automatically.\n");
            w.write("# source: " + sourceUrl + "\n");
            w.write("# retrieved: " + retrieved + "\n");
            w.write("# source-sha256: " + sha256(raw).toLowerCase() + "\n");
            w.write("# roots: " + roots.size() + "\n");
            w.write("# license: This data comes from the Common CA Database (CCADB, https://www.ccadb.org/) and is licensed under the\n");
            w.write("#   Community Data License Agreement - Permissive, Version 2.0 (CDLA-Permissive-2.0, https://cdla.dev/permissive-2-0/).\n");
            w.write("#   The text of the license is in META-INF/LICENSE-ccadb.txt of this module.\n");
            w.write("# format: root <TAB> distrust for TLS after (yyyy-mm-dd or -) <TAB> permitted DNS subtrees (or -) <TAB> name <TAB> certificate (Base64 DER)\n");
            // 루트마다 한 줄을 쓴다.
            for (int i = 0; i < roots.size(); i++) {
                String[] r = roots.get(i);
                w.write("root\t" + r[1] + "\t" + r[2] + "\t" + r[3] + "\t" + r[4] + "\n");
            }
        } finally {
            w.close();
        }
        return roots.size();
    }
}
