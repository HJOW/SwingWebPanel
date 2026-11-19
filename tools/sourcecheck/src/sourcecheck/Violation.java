package sourcecheck;

/**
 * One rule violation found by a check. It prints as <code>path:line: [rule] message</code>.
 * <p>
 * 검사에서 찾은 규칙 위반 하나. <code>경로:줄: [규칙] 메시지</code> 형식으로 출력된다.
 */
final class Violation {

    /**
     * File path shown to the user.
     * <p>
     * 사용자에게 보여 주는 파일 경로.
     */
    final String path;

    /**
     * 1-based line number, or 0 when the violation concerns the whole file.
     * <p>
     * 1 기반 줄 번호. 파일 전체에 대한 위반이면 0.
     */
    final int line;

    /**
     * Rule id such as <code>utf8</code> or <code>javadoc</code>.
     * <p>
     * <code>utf8</code>, <code>javadoc</code> 같은 규칙 식별자.
     */
    final String rule;

    /**
     * Human readable message.
     * <p>
     * 사람이 읽는 메시지.
     */
    final String message;

    /**
     * Creates a violation.
     * <p>
     * 위반을 만든다.
     *
     * @param path    file path / 파일 경로
     * @param line    line number or 0 / 줄 번호 또는 0
     * @param rule    rule id / 규칙 식별자
     * @param message message / 메시지
     */
    Violation(String path, int line, String rule, String message) {
        this.path = path;
        this.line = line;
        this.rule = rule;
        this.message = message;
    }

    /**
     * Formats the violation as one output line.
     * <p>
     * 위반을 출력 한 줄로 만든다.
     *
     * @return formatted text / 형식화한 문자열
     */
    public String toString() {
        return path + ":" + line + ": [" + rule + "] " + message;
    }
}
