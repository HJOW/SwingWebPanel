package org.duckdns.hjow.swingwebpanel.content.markdown;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers of the GFM table extension: splitting a row into cells at unescaped pipes and recognizing the delimiter row
 * (<code>| --- | :-: |</code>) that decides the column alignments.
 * <p>
 * GFM 표 확장의 도우미: 행을 이스케이프되지 않은 파이프에서 셀로 나누고, 열 정렬을 정하는 구분 행(<code>| --- | :-: |</code>)을 인식한다.
 */
final class TableSupport {

    /** Private constructor: static helper class. 비공개 생성자: 정적 도우미 클래스이다. */
    private TableSupport() {
    }

    /**
     * Splits a table row into trimmed cell texts. A leading and a trailing pipe are optional. An escaped pipe <code>\|</code> does not
     * split and becomes a plain pipe in the cell text; other backslash sequences are kept for the inline parser.
     * <p>
     * 표의 행을 다듬은 셀 텍스트로 나눈다. 앞뒤의 파이프는 생략할 수 있다. 이스케이프된 파이프 <code>\|</code> 는 나누지 않으며 셀 텍스트에서는
     * 일반 파이프가 된다. 다른 백슬래시 열은 인라인 파서를 위해 그대로 둔다.
     *
     * @param row the row text / 행 텍스트
     * @return the cells / 셀 목록
     */
    static List<String> splitRow(String row) {
        String s = trimSpaces(row);
        List<String> cells = new ArrayList<String>();
        int n = s.length();
        int i = 0;
        if (i < n && s.charAt(i) == '|') {
            i++;
        }
        StringBuilder cell = new StringBuilder();
        boolean pendingCell = false;
        // 이스케이프되지 않은 파이프를 만날 때마다 셀 하나를 끝낸다.
        while (i < n) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < n) {
                if (s.charAt(i + 1) == '|') {
                    cell.append('|');
                } else {
                    cell.append(c).append(s.charAt(i + 1));
                }
                i += 2;
                pendingCell = true;
            } else if (c == '|') {
                cells.add(trimSpaces(cell.toString()));
                cell.setLength(0);
                pendingCell = false;
                i++;
            } else {
                cell.append(c);
                pendingCell = true;
                i++;
            }
        }
        // 끝 파이프로 끝나지 않았으면 마지막 셀이 남아 있다.
        if (pendingCell || (cell.length() > 0)) {
            cells.add(trimSpaces(cell.toString()));
        }
        return cells;
    }

    /**
     * Parses a delimiter row.
     * <p>
     * 구분 행을 해석한다.
     *
     * @param line the row text / 행 텍스트
     * @return the alignments (0 none, 1 left, 2 center, 3 right) per column, or null if the line is not a delimiter row / 열별 정렬(0 없음, 1 왼쪽, 2 가운데, 3 오른쪽). 구분 행이 아니면 null
     */
    static int[] parseDelimiterRow(String line) {
        String s = trimSpaces(line);
        int n = s.length();
        if (n == 0) {
            return null;
        }
        int i = 0;
        if (s.charAt(0) == '|') {
            i = 1;
        }
        List<Integer> aligns = new ArrayList<Integer>();
        // 파이프로 구분된 셀마다 :?-+:? 형태인지 확인한다.
        while (i < n) {
            while (i < n && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
                i++;
            }
            if (i >= n) {
                break;
            }
            boolean left = false;
            boolean right = false;
            if (s.charAt(i) == ':') {
                left = true;
                i++;
            }
            int dashes = 0;
            // 하이픈이 이어지는 동안 센다.
            while (i < n && s.charAt(i) == '-') {
                dashes++;
                i++;
            }
            if (dashes == 0) {
                return null;
            }
            if (i < n && s.charAt(i) == ':') {
                right = true;
                i++;
            }
            // 셀 안쪽 끝의 공백과 탭을 건너뛴다.
            while (i < n && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
                i++;
            }
            aligns.add(Integer.valueOf(left && right ? 2 : left ? 1 : right ? 3 : 0));
            if (i < n) {
                if (s.charAt(i) != '|') {
                    return null;
                }
                i++;
            }
        }
        if (aligns.isEmpty()) {
            return null;
        }
        int[] result = new int[aligns.size()];
        // 목록을 배열로 옮긴다.
        for (int k = 0; k < result.length; k++) {
            result[k] = aligns.get(k).intValue();
        }
        return result;
    }

    /**
     * Removes leading and trailing spaces and tabs.
     * <p>
     * 앞뒤의 공백과 탭을 지운다.
     *
     * @param s the text / 텍스트
     * @return trimmed text / 다듬은 텍스트
     */
    private static String trimSpaces(String s) {
        int b = 0;
        int e = s.length();
        // 앞쪽의 공백과 탭을 건너뛴다.
        while (b < e && (s.charAt(b) == ' ' || s.charAt(b) == '\t')) {
            b++;
        }
        // 뒤쪽의 공백과 탭을 건너뛴다.
        while (e > b && (s.charAt(e - 1) == ' ' || s.charAt(e - 1) == '\t')) {
            e--;
        }
        return s.substring(b, e);
    }
}
