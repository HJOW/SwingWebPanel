package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * Decoder of the table text that the generated table classes {@link AvifCdfTables} and {@link AvifTables} embed: each table is a comma
 * separated list of decimal integers (split into several string constants because one constant must stay under 64 KiB).
 * <p>
 * 생성된 표 클래스 {@link AvifCdfTables}, {@link AvifTables} 가 담고 있는 표 텍스트의 해독기: 표 하나는 쉼표로 구분한 십진 정수 목록이며
 * (문자열 상수 하나가 64 KiB 를 넘을 수 없으므로 여러 상수로 나눈다).
 */
final class AvifTableData {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private AvifTableData() {
    }

    /**
     * Parses the parts of a table into an integer array.
     * <p>
     * 표의 조각을 정수 배열로 해석한다.
     *
     * @param parts the pieces of the comma separated list (a piece may end inside a number) / 쉼표로 구분한 목록의 조각(숫자 중간에서 끊길 수 있다)
     * @return the numbers / 수 배열
     */
    static int[] ints(String... parts) {
        StringBuilder sb = new StringBuilder();
        // 조각을 하나의 텍스트로 이어 붙인다(조각은 숫자 중간에서 끊길 수 있다).
        for (int p = 0; p < parts.length; p++) {
            sb.append(parts[p]);
        }
        String s = sb.toString();
        int count = s.length() == 0 ? 0 : 1;
        // 쉼표 개수로 원소 수를 센다.
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ',') {
                count++;
            }
        }
        int[] out = new int[count];
        int n = 0;
        int i = 0;
        // 숫자를 차례로 읽어 배열에 채운다.
        while (i < s.length()) {
            int sign = 1;
            if (s.charAt(i) == '-') {
                sign = -1;
                i++;
            }
            int v = 0;
            // 연속된 숫자 문자를 모두 읽는다.
            while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                v = v * 10 + (s.charAt(i) - '0');
                i++;
            }
            out[n++] = sign * v;
            if (i < s.length() && s.charAt(i) == ',') {
                i++;
            }
        }
        if (n != count) {
            throw new IllegalStateException("table text is inconsistent");
        }
        return out;
    }
}
