package brotlidict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * Reads the tables of the Brotli compressed data format out of the text of RFC 7932 (https://www.rfc-editor.org/rfc/rfc7932.txt) and writes the Java source
 * of the data class <code>org.duckdns.hjow.swingwebpanel.media.font.BrotliData</code>: the static dictionary of 122,784 bytes (Appendix A), the 121 word transformations
 * (Appendix B) and the three context lookup tables (section 7.1), each as Base64 text, and the bit depths of the dictionary (section 8). The RFC states the
 * length and the CRC-32 of every table; the tool checks them and stops with an error if one does not match, so that a wrong extraction can never be written.
 * <p>
 * RFC 7932(https://www.rfc-editor.org/rfc/rfc7932.txt)의 텍스트에서 Brotli 압축 형식의 표를 읽어 데이터 클래스 <code>org.duckdns.hjow.swingwebpanel.media.font.BrotliData</code> 의 자바 소스를
 * 만든다: 122,784 바이트의 정적 사전(부록 A), 121 개의 단어 변환(부록 B), 세 개의 문맥 조회표(7.1 절)를 Base64 문자열로, 사전의 비트 깊이(8 절)를 정수 배열로 쓴다.
 * RFC 는 표마다 길이와 CRC-32 를 적어 두었으므로 도구가 이를 확인하며, 하나라도 다르면 오류로 끝나 잘못 추출한 표는 쓰이지 않는다.
 */
public final class BrotliDict {

    /** The length of the static dictionary (Appendix A). 정적 사전의 길이(부록 A). */
    private static final int DICT_LENGTH = 122784;
    /** The CRC-32 of the static dictionary (Appendix A). 정적 사전의 CRC-32(부록 A). */
    private static final long DICT_CRC = 0x5136cb04L;
    /** The length of the byte form of the transformations (Appendix B). 변환의 바이트 형식의 길이(부록 B). */
    private static final int TRANSFORMS_LENGTH = 648;
    /** The CRC-32 of the byte form of the transformations (Appendix B). 변환의 바이트 형식의 CRC-32(부록 B). */
    private static final long TRANSFORMS_CRC = 0x3d965f81L;
    /** The CRC-32 of the three context tables (section 7.1). 세 문맥표의 CRC-32(7.1 절). */
    private static final long[] LUT_CRC = { 0x8e91efb7L, 0xd01a32f4L, 0x0dd7a0d6L };
    /** The number of characters of one chunk of Base64 text (a multiple of four). Base64 문자열 한 조각의 글자 수(4 의 배수). */
    private static final int CHUNK = 8192;

    /**
     * Private constructor: static main class.
     * <p>
     * 비공개 생성자: 정적 main 클래스이다.
     */
    private BrotliDict() {
    }

    /**
     * Runs the tool.
     * <p>
     * 도구를 실행한다.
     *
     * @param args the text of the RFC and the output directory / RFC 텍스트와 출력 디렉터리
     * @throws Exception on failure / 실패 시
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("usage: brotlidict <rfc7932.txt> <output directory>");
            System.exit(1);
        }
        List<String> lines = readLines(new File(args[0]));
        byte[] dict = dictionary(lines);
        int[] ndbits = bitDepths(lines);
        byte[] transforms = transforms(lines);
        byte[] luts = lookupTables(lines);
        File meta = new File(args[0] + ".meta");
        String metaText = meta.isFile() ? new String(readAll(meta), "UTF-8").trim().replace('\r', ' ').replace('\n', ' ') : "(no .meta file)";
        File outDir = new File(args[1]);
        if (!outDir.isDirectory() && !outDir.mkdirs()) {
            System.err.println("[ERROR] cannot create " + outDir);
            System.exit(2);
        }
        Writer w = new OutputStreamWriter(new FileOutputStream(new File(outDir, "BrotliData.java")), "UTF-8");
        try {
            write(w, dict, ndbits, transforms, luts, metaText);
        } finally {
            w.close();
        }
        System.err.println("# BrotliData.java: dictionary " + dict.length + " bytes, transforms " + transforms.length + " bytes, lookup tables " + luts.length + " bytes");
    }

    /**
     * Reads the lines of a text file as ISO-8859-1.
     * <p>
     * 텍스트 파일의 줄을 ISO-8859-1 로 읽는다.
     *
     * @param f the file / 파일
     * @return the lines / 줄
     * @throws IOException if the file cannot be read / 파일을 읽을 수 없으면
     */
    private static List<String> readLines(File f) throws IOException {
        List<String> out = new ArrayList<String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "ISO-8859-1"));
        try {
            String line;
            // 파일의 줄을 끝까지 읽는다.
            while ((line = r.readLine()) != null) {
                out.add(line);
            }
        } finally {
            r.close();
        }
        return out;
    }

    /**
     * Reads a whole file.
     * <p>
     * 파일 전체를 읽는다.
     *
     * @param f the file / 파일
     * @return the bytes / 바이트
     * @throws IOException if the file cannot be read / 파일을 읽을 수 없으면
     */
    private static byte[] readAll(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 파일을 끝까지 읽는다.
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Returns the index of the first line that starts with a text.
     * <p>
     * 어떤 텍스트로 시작하는 첫 줄의 위치를 반환한다.
     *
     * @param lines the lines / 줄
     * @param start the first index to look at / 처음 볼 위치
     * @param text  the text / 텍스트
     * @return the index / 위치
     */
    private static int find(List<String> lines, int start, String text) {
        // 줄을 차례로 보며 텍스트로 시작하는 줄을 찾는다.
        for (int i = start; i < lines.size(); i++) {
            if (lines.get(i).startsWith(text)) {
                return i;
            }
        }
        throw new IllegalStateException("not found: " + text);
    }

    /**
     * Computes the CRC-32 of bytes.
     * <p>
     * 바이트의 CRC-32 를 계산한다.
     *
     * @param b the bytes / 바이트
     * @return the CRC-32 / CRC-32
     */
    private static long crc(byte[] b) {
        CRC32 c = new CRC32();
        c.update(b, 0, b.length);
        return c.getValue();
    }

    /**
     * Checks the length and the CRC-32 of a table, or stops.
     * <p>
     * 표의 길이와 CRC-32 를 확인하고 다르면 멈춘다.
     *
     * @param name   the name of the table / 표 이름
     * @param b      the bytes / 바이트
     * @param length the length that the RFC gives / RFC 가 주는 길이
     * @param crc    the CRC-32 that the RFC gives / RFC 가 주는 CRC-32
     */
    private static void verify(String name, byte[] b, int length, long crc) {
        long actual = crc(b);
        if (b.length != length || actual != crc) {
            throw new IllegalStateException(name + ": length " + b.length + " (expected " + length + "), CRC-32 " + Long.toHexString(actual) + " (expected "
                    + Long.toHexString(crc) + ")");
        }
    }

    /**
     * Reads the hexadecimal dump of the static dictionary (Appendix A).
     * <p>
     * 정적 사전의 16진 덤프를 읽는다(부록 A).
     *
     * @param lines the lines of the RFC / RFC 의 줄
     * @return the dictionary / 사전
     */
    private static byte[] dictionary(List<String> lines) {
        int i = find(lines, 0, "Appendix A.  Static Dictionary Data");
        int end = find(lines, i, "   The number of words for each length");
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        Pattern hex = Pattern.compile("^\\s+((?:[0-9a-f]{2})+)\\s*$");
        // 덤프의 줄마다 16진 숫자를 바이트로 바꾼다.
        for (int k = i; k < end; k++) {
            Matcher m = hex.matcher(lines.get(k));
            if (m.matches()) {
                String h = m.group(1);
                // 두 글자씩 바이트 하나이다.
                for (int p = 0; p < h.length(); p += 2) {
                    out.write(Integer.parseInt(h.substring(p, p + 2), 16));
                }
            }
        }
        byte[] b = out.toByteArray();
        verify("dictionary", b, DICT_LENGTH, DICT_CRC);
        return b;
    }

    /**
     * Reads the bit depths of the static dictionary (section 8, NDBITS).
     * <p>
     * 정적 사전의 비트 깊이를 읽는다(8 절, NDBITS).
     *
     * @param lines the lines of the RFC / RFC 의 줄
     * @return NDBITS[0..24] / NDBITS[0..24]
     */
    private static int[] bitDepths(List<String> lines) {
        int i = find(lines, find(lines, 0, "Appendix A.  Static Dictionary Data"), "      NDBITS :=");
        int[] out = new int[25];
        int n = 0;
        // NDBITS 줄과 그 뒤의 숫자 줄에서 25 개를 읽는다.
        for (int k = i; k < lines.size() && n < 25; k++) {
            String t = lines.get(k);
            if (k == i) {
                t = t.substring(t.indexOf(":=") + 2);
            }
            Matcher m = Pattern.compile("\\d+").matcher(t);
            // 줄의 숫자를 차례로 읽는다.
            while (m.find() && n < 25) {
                out[n++] = Integer.parseInt(m.group());
            }
        }
        if (n != 25) {
            throw new IllegalStateException("NDBITS has " + n + " values");
        }
        return out;
    }

    /**
     * Reads the list of the word transformations (Appendix B) and makes the byte form that the RFC describes: the prefix and a zero, the number of the
     * transformation, the suffix and a zero.
     * <p>
     * 단어 변환 목록(부록 B)을 읽어 RFC 가 설명하는 바이트 형식으로 만든다: 접두사와 0, 변환 번호, 접미사와 0.
     *
     * @param lines the lines of the RFC / RFC 의 줄
     * @return the bytes / 바이트
     */
    private static byte[] transforms(List<String> lines) {
        int i = find(lines, find(lines, 0, "Appendix A.  Static Dictionary Data"), "Appendix B.  List of Word Transformations");
        int end = find(lines, i, "Appendix C.");
        Pattern row = Pattern.compile("^\\s+(\\d+)\\s+\"((?:[^\"\\\\]|\\\\.)*)\"\\s+(\\w+)\\s+\"((?:[^\"\\\\]|\\\\.)*)\"\\s*$");
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int expected = 0;
        // 변환 표의 줄마다 번호가 차례대로인지 확인하며 바이트로 바꾼다.
        for (int k = i; k < end; k++) {
            Matcher m = row.matcher(lines.get(k));
            if (!m.matches()) {
                continue;
            }
            if (Integer.parseInt(m.group(1)) != expected) {
                throw new IllegalStateException("transform " + m.group(1) + " is out of order (expected " + expected + ")");
            }
            expected++;
            byte[] prefix = unescape(m.group(2));
            byte[] suffix = unescape(m.group(4));
            out.write(prefix, 0, prefix.length);
            out.write(0);
            out.write(kindOf(m.group(3)));
            out.write(suffix, 0, suffix.length);
            out.write(0);
        }
        if (expected != 121) {
            throw new IllegalStateException("found " + expected + " transformations, expected 121");
        }
        byte[] b = out.toByteArray();
        verify("transforms", b, TRANSFORMS_LENGTH, TRANSFORMS_CRC);
        return b;
    }

    /**
     * Returns the number that identifies an elementary transform: 0 for Identity, 1 for FermentFirst, 2 for FermentAll, 3 to 11 for OmitFirst1 to OmitFirst9 and
     * 12 to 20 for OmitLast1 to OmitLast9.
     * <p>
     * 기본 변환을 가리키는 번호를 반환한다: Identity 0, FermentFirst 1, FermentAll 2, OmitFirst1~9 는 3~11, OmitLast1~9 는 12~20.
     *
     * @param name the name in the table / 표의 이름
     * @return the number / 번호
     */
    private static int kindOf(String name) {
        if (name.equals("Identity")) {
            return 0;
        }
        if (name.equals("FermentFirst")) {
            return 1;
        }
        if (name.equals("FermentAll")) {
            return 2;
        }
        if (name.startsWith("OmitFirst")) {
            return 2 + Integer.parseInt(name.substring("OmitFirst".length()));
        }
        if (name.startsWith("OmitLast")) {
            return 11 + Integer.parseInt(name.substring("OmitLast".length()));
        }
        throw new IllegalStateException("unknown transform " + name);
    }

    /**
     * Turns a string literal of the table (C format) into bytes.
     * <p>
     * 표의 문자열 리터럴(C 형식)을 바이트로 바꾼다.
     *
     * @param s the text between the quotes / 따옴표 사이의 텍스트
     * @return the bytes / 바이트
     */
    private static byte[] unescape(String s) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        // 글자마다 이스케이프 시퀀스를 풀어 바이트로 쓴다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\') {
                out.write(c);
                continue;
            }
            char e = s.charAt(++i);
            if (e == 'n') {
                out.write('\n');
            } else if (e == 't') {
                out.write('\t');
            } else if (e == 'r') {
                out.write('\r');
            } else if (e == '0') {
                out.write(0);
            } else if (e == 'x') {
                out.write(Integer.parseInt(s.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                out.write(e);
            }
        }
        return out.toByteArray();
    }

    /**
     * Reads the three context lookup tables (section 7.1) one after the other.
     * <p>
     * 세 문맥 조회표(7.1 절)를 차례로 읽는다.
     *
     * @param lines the lines of the RFC / RFC 의 줄
     * @return the 768 bytes of Lut0, Lut1 and Lut2 / Lut0, Lut1, Lut2 의 768 바이트
     */
    private static byte[] lookupTables(List<String> lines) {
        byte[] out = new byte[768];
        int from = find(lines, 0, "7.1.  Context Modes and Context ID Lookup for Literals");
        // 표마다 이름 줄을 찾아 256 개의 숫자를 읽고 CRC-32 를 확인한다.
        for (int t = 0; t < 3; t++) {
            int i = find(lines, from, "      Lut" + t + " :=");
            byte[] one = new byte[256];
            int n = 0;
            Pattern numbers = Pattern.compile("^\\s*\\d+(?:\\s*,\\s*\\d+)*\\s*,?\\s*$");
            // 이름 줄 뒤에서 숫자만 있는 줄의 숫자를 모은다(쪽 머리말 줄은 건너뛴다).
            for (int k = i + 1; k < lines.size() && n < 256; k++) {
                if (!numbers.matcher(lines.get(k)).matches()) {
                    continue;
                }
                Matcher m = Pattern.compile("\\d+").matcher(lines.get(k));
                // 줄의 숫자를 차례로 읽는다.
                while (m.find() && n < 256) {
                    one[n++] = (byte) Integer.parseInt(m.group());
                }
            }
            if (n != 256) {
                throw new IllegalStateException("Lut" + t + " has " + n + " values");
            }
            verify("Lut" + t, one, 256, LUT_CRC[t]);
            System.arraycopy(one, 0, out, t * 256, 256);
            from = i + 1;
        }
        return out;
    }

    /**
     * Encodes bytes as Base64 text without line breaks.
     * <p>
     * 바이트를 줄바꿈 없는 Base64 텍스트로 인코딩한다.
     *
     * @param b the bytes / 바이트
     * @return the text / 텍스트
     */
    private static String base64(byte[] b) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        StringBuilder sb = new StringBuilder();
        // 세 바이트마다 네 글자를 쓴다(마지막은 = 로 채운다).
        for (int i = 0; i < b.length; i += 3) {
            int v = (b[i] & 0xFF) << 16;
            v |= i + 1 < b.length ? (b[i + 1] & 0xFF) << 8 : 0;
            v |= i + 2 < b.length ? b[i + 2] & 0xFF : 0;
            sb.append(alphabet.charAt((v >> 18) & 63));
            sb.append(alphabet.charAt((v >> 12) & 63));
            sb.append(i + 1 < b.length ? alphabet.charAt((v >> 6) & 63) : '=');
            sb.append(i + 2 < b.length ? alphabet.charAt(v & 63) : '=');
        }
        return sb.toString();
    }

    /**
     * Writes the source of the data class.
     * <p>
     * 데이터 클래스의 소스를 쓴다.
     *
     * @param w          the destination / 출력 대상
     * @param dict       the dictionary / 사전
     * @param ndbits     NDBITS / NDBITS
     * @param transforms the transformations / 변환
     * @param luts       the lookup tables / 조회표
     * @param meta       the origin of the input as the webfetch tool wrote it / webfetch 도구가 쓴 입력의 출처
     * @throws IOException if writing fails / 쓰기가 실패하면
     */
    private static void write(Writer w, byte[] dict, int[] ndbits, byte[] transforms, byte[] luts, String meta) throws IOException {
        w.write("package org.duckdns.hjow.swingwebpanel.media.font;\n\n");
        w.write("/**\n");
        w.write(" * The tables of the Brotli compressed data format (RFC 7932, https://www.rfc-editor.org/rfc/rfc7932.txt, July 2016), generated by the tool\n");
        w.write(" * <code>tools/brotlidict</code> from the text of the RFC (origin of the input: " + meta.replace("*/", "* /") + "): the static dictionary of "
                + dict.length + " bytes\n");
        w.write(" * (Appendix A, CRC-32 0x5136cb04), the 121 word transformations (Appendix B, 648 bytes, CRC-32 0x3d965f81), the context lookup tables Lut0, Lut1 and Lut2\n");
        w.write(" * (section 7.1, 256 bytes each, CRC-32 0x8e91efb7, 0xd01a32f4 and 0x0dd7a0d6) and the bit depths of the dictionary (section 8). The tool checked every length\n");
        w.write(" * and CRC-32 against the values that the RFC states. Do not edit this file: run the tool again.\n");
        w.write(" * <p>\n");
        w.write(" * Brotli 압축 데이터 형식(RFC 7932, https://www.rfc-editor.org/rfc/rfc7932.txt, 2016 년 7 월)의 표. <code>tools/brotlidict</code> 도구가 RFC 텍스트에서 만들었다:\n");
        w.write(" * 정적 사전 " + dict.length + " 바이트(부록 A), 단어 변환 121 개(부록 B), 문맥 조회표 Lut0, Lut1, Lut2(7.1 절), 사전의 비트 깊이(8 절). 도구가 길이와 CRC-32 를\n");
        w.write(" * RFC 가 적은 값과 대조했다. 이 파일은 고치지 말고 도구를 다시 실행한다.\n");
        w.write(" */\n");
        w.write("final class BrotliData {\n\n");
        w.write("    /**\n     * The static dictionary as Base64 text in chunks (the bytes of DICT in order).\n     * <p>\n     * 정적 사전의 Base64 텍스트 조각(DICT 의 바이트를 순서대로).\n     */\n");
        w.write("    static final String[] DICTIONARY = {\n");
        String text = base64(dict);
        // 정해진 글자 수마다 한 조각으로 쓴다.
        for (int p = 0; p < text.length(); p += CHUNK) {
            w.write("        \"" + text.substring(p, Math.min(text.length(), p + CHUNK)) + "\"" + (p + CHUNK < text.length() ? "," : "") + "\n");
        }
        w.write("    };\n\n");
        w.write("    /**\n     * The bit depths of the dictionary for the lengths 0 to 24 (NDBITS).\n     * <p>\n     * 길이 0~24 의 사전 비트 깊이(NDBITS).\n     */\n");
        StringBuilder nd = new StringBuilder();
        // 비트 깊이를 쉼표로 이어 쓴다.
        for (int i = 0; i < ndbits.length; i++) {
            nd.append(i == 0 ? "" : ", ").append(ndbits[i]);
        }
        w.write("    static final int[] NDBITS = { " + nd + " };\n\n");
        w.write("    /**\n     * The word transformations as Base64 text: for each of the 121 transformations the prefix and a zero, the number of the elementary transform\n"
                + "     * (0 Identity, 1 FermentFirst, 2 FermentAll, 3..11 OmitFirst1..9, 12..20 OmitLast1..9), the suffix and a zero.\n     * <p>\n"
                + "     * 단어 변환의 Base64 텍스트: 121 개의 변환마다 접두사와 0, 기본 변환의 번호(0 Identity, 1 FermentFirst, 2 FermentAll, 3~11 OmitFirst1~9, 12~20 OmitLast1~9), 접미사와 0.\n     */\n");
        w.write("    static final String TRANSFORMS = \"" + base64(transforms) + "\";\n\n");
        w.write("    /**\n     * The context lookup tables Lut0, Lut1 and Lut2 (256 bytes each, one after the other) as Base64 text.\n     * <p>\n"
                + "     * 문맥 조회표 Lut0, Lut1, Lut2(각 256 바이트, 차례로)의 Base64 텍스트.\n     */\n");
        w.write("    static final String CONTEXT_LUTS = \"" + base64(luts) + "\";\n\n");
        w.write("    /**\n     * Not instantiable.\n     * <p>\n     * 인스턴스를 만들 수 없다.\n     */\n");
        w.write("    private BrotliData() {\n    }\n}\n");
    }
}
