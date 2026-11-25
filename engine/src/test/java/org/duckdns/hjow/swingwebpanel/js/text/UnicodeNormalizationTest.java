package org.duckdns.hjow.swingwebpanel.js.text;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.BitSet;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Regression and official corpus tests for Unicode 18 normalization.
 * <p>
 * Unicode 18 정규화의 회귀 시험과 공식 전체 입력 검증.
 */
public class UnicodeNormalizationTest {
    /** The corpus columns corresponding to these forms begin at index one. / 이 형식의 시험 자료 열은 인덱스 1부터 시작한다. */
    private static final String[] FORMS = { "NFC", "NFD", "NFKC", "NFKD" };

    /**
     * Checks a supplementary decomposition, Hangul, blocked pairs and lone UTF-16 surrogates.
     * <p>
     * 보충 평면 분해, 한글, 차단된 조합 쌍과 짝 없는 UTF-16 서로게이트를 확인한다.
     */
    public void testCanonicalAndCompatibilityBoundaries() {
        String akuru = chars("11938");
        String separated = chars("11935 11930");
        Check.equal(separated, TextTransform.normalize(akuru, "NFD"), "a supplementary decomposition newer than JRE 6");
        Check.equal(akuru, TextTransform.normalize(separated, null), "composes the supplementary pair in default NFC");
        Check.equal("\u1100\u1161\u11A8", TextTransform.normalize("\uAC01", "NFD"), "algorithmic Hangul decomposition");
        Check.equal("\uAC01", TextTransform.normalize("\uAC00\u11A8", "NFC"), "an LV syllable expands and recomposes");
        Check.equal("\u0308\u0301", TextTransform.normalize("\u0344", "NFC"), "a composition exclusion stays decomposed");
        Check.equal("A\u0305\u030A", TextTransform.normalize("A\u0305\u030A", "NFC"), "an equal combining class blocks composition");
        Check.equal("\uFB03", TextTransform.normalize("\uFB03", "NFC"), "canonical forms leave compatibility characters alone");
        Check.equal("ffi", TextTransform.normalize("\uFB03", "NFKD"), "compatibility decomposition expands a ligature");
        Check.equal("\uD800e\u0301\uDC00", TextTransform.normalize("\uD800\u00E9\uDC00", "NFD"), "lone surrogates stay in place");
    }

    /**
     * Checks both the short and long combining-run ordering and equal-class stability.
     * <p>
     * 짧고 긴 결합 구간의 정렬과 같은 클래스의 순서 보존을 확인한다.
     */
    public void testCombiningRunsAreStable() {
        Check.equal("a\u0301\u0300\u0315", TextTransform.normalize("a\u0315\u0301\u0300", "NFD"), "short canonical ordering");
        StringBuilder unordered = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        // 서로 다른 세 결합 문자 중 뒤의 두 문자는 같은 클래스이다.
        for (int i = 0; i < 100; i++) {
            unordered.append("\u0315\u0301\u0300");
            expected.append("\u0301\u0300");
        }
        // 가장 큰 클래스의 문자는 구간 끝으로 모인다.
        for (int i = 0; i < 100; i++) {
            expected.append('\u0315');
        }
        Check.equal(expected.toString(), TextTransform.normalize(unordered.toString(), "NFD"), "long leading combining run");
    }

    /**
     * Runs all stated invariants of the original Unicode 18 NormalizationTest.txt.
     * <p>
     * 원본 Unicode 18 NormalizationTest.txt가 정한 모든 불변 조건을 검사한다.
     *
     * @param args path to the pinned NormalizationTest.txt / 고정 NormalizationTest.txt의 경로
     * @throws IOException when the corpus cannot be read / 전체 입력을 읽을 수 없을 때
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: UnicodeNormalizationTest /path/to/NormalizationTest.txt");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));
        BitSet listed = new BitSet();
        int rows = 0;
        try {
            Check.equal("# NormalizationTest-18.0.0.txt", reader.readLine(), "the corpus version");
            String line;
            boolean partOne = false;
            // 보이는 모든 자료 행을 읽고 첫 부분의 단일 문자 항목을 기록한다.
            for (int lineNumber = 2; (line = reader.readLine()) != null; lineNumber++) {
                int comment = line.indexOf('#');
                String data = (comment < 0 ? line : line.substring(0, comment)).trim();
                if (data.length() == 0) {
                    continue;
                }
                if (data.startsWith("@")) {
                    partOne = "@Part1".equals(data);
                    continue;
                }
                String[] columns = data.split(";");
                Check.equal(5, columns.length, "five columns at line " + lineNumber);
                // 16진수 열을 Unicode 문자열로 바꾼다.
                for (int i = 0; i < columns.length; i++) {
                    columns[i] = chars(columns[i]);
                }
                if (partOne && CodePoints.codePointCount(columns[0]) == 1) {
                    listed.set(CodePoints.codePointAt(columns[0], 0));
                }
                checkForms(columns, lineNumber);
                rows++;
            }
        } finally {
            reader.close();
        }
        int unchanged = 0;
        // Part 1에 없는 단일 코드 포인트는 네 형식에서 모두 그대로여야 한다.
        for (int cp = listed.nextClearBit(0); cp <= CodePoints.MAX_CODE_POINT; cp = listed.nextClearBit(cp + 1)) {
            String scalar = new String(Character.toChars(cp));
            // 각 형식에서 전체 입력 길이와 코드 단위가 같아야 한다.
            for (int f = 0; f < FORMS.length; f++) {
                if (!scalar.equals(TextTransform.normalize(scalar, FORMS[f]))) {
                    throw new AssertionError(FORMS[f] + " unexpectedly changed U+" + Integer.toHexString(cp));
                }
            }
            unchanged++;
        }
        System.out.println(rows + " Unicode 18 normalization rows and " + unchanged + " unchanged code points passed");
    }

    /**
     * Checks all five source forms against the NFC, NFD, NFKC and NFKD conformance columns.
     * <p>
     * 원본 형식 다섯 가지를 NFC, NFD, NFKC, NFKD 적합성 열과 대조한다.
     *
     * @param columns the normalization vector / 정규화 시험 자료 행
     * @param line the source line number / 원본 줄 번호
     */
    private static void checkForms(String[] columns, int line) {
        // 정준 형식은 호환 분해 이전과 이후의 세부 기대값이 다르다.
        for (int src = 0; src < columns.length; src++) {
            for (int f = 0; f < FORMS.length; f++) {
                int expected = f + 1 + (f < 2 && src > 2 ? 2 : 0);
                Check.equal(columns[expected], TextTransform.normalize(columns[src], FORMS[f]),
                        "line " + line + ", input " + (src + 1) + ", " + FORMS[f]);
            }
        }
    }

    /**
     * Decodes the space-separated hexadecimal notation of a corpus field.
     * <p>
     * 시험 자료 열에서 공백으로 구분한 16진수 표기를 해석한다.
     *
     * @param codes the field text / 열의 텍스트
     * @return the UTF-16 string / UTF-16 문자열
     */
    private static String chars(String codes) {
        StringBuilder value = new StringBuilder();
        String[] points = codes.trim().split(" +");
        // 각 코드 포인트를 UTF-16 단위로 붙인다.
        for (int i = 0; i < points.length; i++) {
            value.appendCodePoint(Integer.parseInt(points[i], 16));
        }
        return value.toString();
    }
}
