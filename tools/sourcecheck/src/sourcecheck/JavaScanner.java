package sourcecheck;

import java.util.ArrayList;
import java.util.List;

/**
 * A small Java source scanner. It splits source text into tokens and comments, and builds a "code text" in which every
 * comment and the inside of every string/char literal is replaced by spaces (newlines are kept), so regular expressions
 * can search real code without being fooled by comments or string contents. It understands only the lexical level of
 * Java 6; it does not parse.
 * <p>
 * 작은 Java 소스 스캐너. 소스 텍스트를 토큰과 주석으로 나누고, 모든 주석과 문자열/문자 리터럴 내용을 공백으로 바꾼
 * "코드 텍스트"(줄바꿈은 유지)를 만들어 정규식이 주석이나 문자열 내용에 속지 않고 실제 코드만 검색하게 한다.
 * Java 6 의 어휘 수준만 이해하며 구문 분석은 하지 않는다.
 */
final class JavaScanner {

    /**
     * Token type: identifier or keyword.
     * <p>
     * 토큰 종류: 식별자 또는 키워드.
     */
    static final int IDENT = 0;

    /**
     * Token type: numeric literal.
     * <p>
     * 토큰 종류: 숫자 리터럴.
     */
    static final int NUM = 1;

    /**
     * Token type: string or char literal.
     * <p>
     * 토큰 종류: 문자열 또는 문자 리터럴.
     */
    static final int STR = 2;

    /**
     * Token type: a single punctuation character.
     * <p>
     * 토큰 종류: 구두점 한 글자.
     */
    static final int SYM = 3;

    /**
     * One token.
     * <p>
     * 토큰 하나.
     */
    static final class Tok {
        /**
         * Token type (IDENT, NUM, STR or SYM).
         * <p>
         * 토큰 종류(IDENT, NUM, STR, SYM).
         */
        final int type;

        /**
         * Token text.
         * <p>
         * 토큰 문자열.
         */
        final String text;

        /**
         * Start offset in the source.
         * <p>
         * 소스에서의 시작 위치.
         */
        final int pos;

        /**
         * End offset (exclusive) in the source.
         * <p>
         * 소스에서의 끝 위치(미포함).
         */
        final int end;

        /**
         * 1-based line number of the token start.
         * <p>
         * 토큰 시작의 1 기반 줄 번호.
         */
        final int line;

        /**
         * Creates a token.
         * <p>
         * 토큰을 만든다.
         *
         * @param type token type / 토큰 종류
         * @param text token text / 토큰 문자열
         * @param pos  start offset / 시작 위치
         * @param end  end offset / 끝 위치
         * @param line start line / 시작 줄
         */
        Tok(int type, String text, int pos, int end, int line) {
            this.type = type;
            this.text = text;
            this.pos = pos;
            this.end = end;
            this.line = line;
        }

        /**
         * Tests whether this token is the given punctuation character.
         * <p>
         * 이 토큰이 주어진 구두점 문자인지 확인한다.
         *
         * @param c punctuation character / 구두점 문자
         * @return true if it matches / 같으면 true
         */
        boolean isSym(char c) {
            return type == SYM && text.charAt(0) == c;
        }

        /**
         * Tests whether this token is the given identifier or keyword.
         * <p>
         * 이 토큰이 주어진 식별자/키워드인지 확인한다.
         *
         * @param s identifier text / 식별자 문자열
         * @return true if it matches / 같으면 true
         */
        boolean isIdent(String s) {
            return type == IDENT && text.equals(s);
        }
    }

    /**
     * One comment.
     * <p>
     * 주석 하나.
     */
    static final class Cmt {
        /**
         * True for a Javadoc comment (starts with slash-star-star).
         * <p>
         * Javadoc 주석(슬래시-별-별로 시작)이면 true.
         */
        final boolean doc;

        /**
         * Comment text including delimiters.
         * <p>
         * 구분자를 포함한 주석 문자열.
         */
        final String text;

        /**
         * First line of the comment.
         * <p>
         * 주석의 시작 줄.
         */
        final int startLine;

        /**
         * Last line of the comment.
         * <p>
         * 주석의 마지막 줄.
         */
        final int endLine;

        /**
         * Start offset.
         * <p>
         * 시작 위치.
         */
        final int pos;

        /**
         * End offset (exclusive).
         * <p>
         * 끝 위치(미포함).
         */
        final int end;

        /**
         * Creates a comment record.
         * <p>
         * 주석 기록을 만든다.
         *
         * @param doc       whether it is a Javadoc comment / Javadoc 주석 여부
         * @param text      comment text / 주석 문자열
         * @param startLine first line / 시작 줄
         * @param endLine   last line / 마지막 줄
         * @param pos       start offset / 시작 위치
         * @param end       end offset / 끝 위치
         */
        Cmt(boolean doc, String text, int startLine, int endLine, int pos, int end) {
            this.doc = doc;
            this.text = text;
            this.startLine = startLine;
            this.endLine = endLine;
            this.pos = pos;
            this.end = end;
        }
    }

    /**
     * The scanned source text.
     * <p>
     * 스캔한 소스 텍스트.
     */
    final String src;

    /**
     * Tokens in source order (comments excluded).
     * <p>
     * 소스 순서의 토큰 목록(주석 제외).
     */
    final List<Tok> toks = new ArrayList<Tok>();

    /**
     * Comments in source order.
     * <p>
     * 소스 순서의 주석 목록.
     */
    final List<Cmt> cmts = new ArrayList<Cmt>();

    /**
     * Source with comments and literal contents blanked out.
     * <p>
     * 주석과 리터럴 내용을 공백으로 바꾼 소스.
     */
    final String code;

    /**
     * Offsets of the first character of every line.
     * <p>
     * 각 줄 첫 문자의 위치.
     */
    private final int[] lineStart;

    /**
     * Scans the given source text.
     * <p>
     * 주어진 소스 텍스트를 스캔한다.
     *
     * @param source Java source text / Java 소스 텍스트
     */
    JavaScanner(String source) {
        this.src = source;
        // 줄 시작 위치 표를 먼저 만든다.
        List<Integer> starts = new ArrayList<Integer>();
        starts.add(Integer.valueOf(0));
        // 소스의 줄바꿈 위치에서 각 줄의 시작 위치를 모은다.
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                starts.add(Integer.valueOf(i + 1));
            }
        }
        lineStart = new int[starts.size()];
        // 목록을 배열로 옮겨 이분 탐색에 쓴다.
        for (int i = 0; i < lineStart.length; i++) {
            lineStart[i] = starts.get(i).intValue();
        }
        char[] blank = source.toCharArray();
        scan(blank);
        this.code = new String(blank);
    }

    /**
     * Converts an offset to a 1-based line number.
     * <p>
     * 위치를 1 기반 줄 번호로 바꾼다.
     *
     * @param pos offset in the source / 소스에서의 위치
     * @return line number / 줄 번호
     */
    int lineOf(int pos) {
        int lo = 0;
        int hi = lineStart.length - 1;
        // 이분 탐색으로 pos 가 속한 줄을 찾는다.
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (lineStart[mid] <= pos) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo + 1;
    }

    /**
     * Performs the scan, filling tokens, comments and the blanked code array.
     * <p>
     * 스캔을 수행하여 토큰, 주석, 공백 처리된 코드 배열을 채운다.
     *
     * @param blank character array that is blanked in place / 제자리에서 공백 처리되는 문자 배열
     */
    private void scan(char[] blank) {
        int n = src.length();
        int i = 0;
        // 문자 하나씩 진행하며 주석, 리터럴, 숫자, 식별자, 기호를 구분한다.
        while (i < n) {
            char c = src.charAt(i);
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
                int j = i;
                // 줄 끝까지를 한 줄 주석으로 읽는다.
                while (j < n && src.charAt(j) != '\n') {
                    j++;
                }
                addComment(false, i, j, blank);
                i = j;
            } else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
                int j = src.indexOf("*/", i + 2);
                j = j < 0 ? n : j + 2;
                boolean doc = src.startsWith("/**", i) && !src.startsWith("/**/", i);
                addComment(doc, i, j, blank);
                i = j;
            } else if (c == '"' || c == '\'') {
                int j = i + 1;
                // 닫는 따옴표까지 이스케이프를 건너뛰며 읽는다.
                while (j < n && src.charAt(j) != c && src.charAt(j) != '\n') {
                    if (src.charAt(j) == '\\') {
                        j++;
                    }
                    j++;
                }
                int e = Math.min(n, j + 1);
                toks.add(new Tok(STR, src.substring(i, e), i, e, lineOf(i)));
                // 문자열 내용을 공백으로 바꾸되 줄바꿈은 유지한다.
                for (int k = i + 1; k < e - 1; k++) {
                    if (blank[k] != '\n') {
                        blank[k] = ' ';
                    }
                }
                i = e;
            } else if (Character.isJavaIdentifierStart(c)) {
                int j = i + 1;
                // 식별자에 속하는 문자를 끝까지 읽는다.
                while (j < n && Character.isJavaIdentifierPart(src.charAt(j))) {
                    j++;
                }
                toks.add(new Tok(IDENT, src.substring(i, j), i, j, lineOf(i)));
                i = j;
            } else if (c >= '0' && c <= '9') {
                int j = i + 1;
                // 숫자 리터럴은 영문자, 숫자, 점, 밑줄을 한 덩어리로 읽는다.
                while (j < n && (Character.isLetterOrDigit(src.charAt(j)) || src.charAt(j) == '.' || src.charAt(j) == '_')) {
                    j++;
                }
                toks.add(new Tok(NUM, src.substring(i, j), i, j, lineOf(i)));
                i = j;
            } else if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == (char) 0xFEFF) {
                i++;
            } else {
                toks.add(new Tok(SYM, String.valueOf(c), i, i + 1, lineOf(i)));
                i++;
            }
        }
    }

    /**
     * Records a comment and blanks it in the code array.
     * <p>
     * 주석을 기록하고 코드 배열에서 공백 처리한다.
     *
     * @param doc   whether it is Javadoc / Javadoc 여부
     * @param from  start offset / 시작 위치
     * @param to    end offset (exclusive) / 끝 위치(미포함)
     * @param blank code array to blank / 공백 처리할 코드 배열
     */
    private void addComment(boolean doc, int from, int to, char[] blank) {
        cmts.add(new Cmt(doc, src.substring(from, to), lineOf(from), lineOf(Math.max(from, to - 1)), from, to));
        // 주석 전체를 공백으로 바꾸되 줄바꿈은 유지한다.
        for (int k = from; k < to; k++) {
            if (blank[k] != '\n') {
                blank[k] = ' ';
            }
        }
    }
}
