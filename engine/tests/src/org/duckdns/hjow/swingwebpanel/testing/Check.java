package org.duckdns.hjow.swingwebpanel.testing;

import java.util.Arrays;

/**
 * Explicit assertion helpers for the self-made test runner. Every check throws {@link AssertionFailure}
 * instead of relying on Java <code>assert</code>, so it works regardless of the <code>-ea</code> JVM option.
 * Use it as <code>Check.equal("a", actual, "name should be a")</code>.
 * <p>
 * 자체 시험 실행기용 명시적 검사 도구 모음. 모든 검사는 Java <code>assert</code> 대신 {@link AssertionFailure} 를 던지므로
 * <code>-ea</code> JVM 옵션과 무관하게 동작한다. 사용 예: <code>Check.equal("a", actual, "이름은 a 여야 한다")</code>.
 */
public final class Check {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Check() {
    }

    /**
     * Fails unconditionally.
     * <p>
     * 무조건 실패시킨다.
     *
     * @param message failure description / 실패 설명
     */
    public static void fail(String message) {
        throw new AssertionFailure(message);
    }

    /**
     * Marks the current test as skipped with a mandatory reason.
     * <p>
     * 현재 시험을 건너뜀으로 표시한다. 이유는 필수이다.
     *
     * @param reason why the test cannot run (must not be empty) / 시험을 실행할 수 없는 이유(비어 있으면 안 됨)
     */
    public static void skip(String reason) {
        if (reason == null || reason.trim().length() == 0) {
            throw new AssertionFailure("skip() requires a non-empty reason");
        }
        throw new SkipException(reason);
    }

    /**
     * Checks that a condition is true.
     * <p>
     * 조건이 참인지 검사한다.
     *
     * @param condition value to check / 검사할 값
     * @param message   failure description / 실패 설명
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionFailure(message);
        }
    }

    /**
     * Checks that a condition is false.
     * <p>
     * 조건이 거짓인지 검사한다.
     *
     * @param condition value to check / 검사할 값
     * @param message   failure description / 실패 설명
     */
    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionFailure(message);
        }
    }

    /**
     * Checks that a reference is not null.
     * <p>
     * 참조가 null 이 아닌지 검사한다.
     *
     * @param value   value to check / 검사할 값
     * @param message failure description / 실패 설명
     */
    public static void notNull(Object value, String message) {
        if (value == null) {
            throw new AssertionFailure(message + " (was null)");
        }
    }

    /**
     * Checks that a reference is null.
     * <p>
     * 참조가 null 인지 검사한다.
     *
     * @param value   value to check / 검사할 값
     * @param message failure description / 실패 설명
     */
    public static void isNull(Object value, String message) {
        if (value != null) {
            throw new AssertionFailure(message + " (was " + describe(value) + ")");
        }
    }

    /**
     * Checks equality with <code>equals</code> (null-safe, arrays compared by content).
     * <p>
     * <code>equals</code> 로 같음을 검사한다(null 안전, 배열은 내용 비교).
     *
     * @param expected expected value / 기대 값
     * @param actual   actual value / 실제 값
     * @param message  failure description / 실패 설명
     */
    public static void equal(Object expected, Object actual, String message) {
        if (!sameValue(expected, actual)) {
            throw new AssertionFailure(message + " (expected " + describe(expected) + " but was " + describe(actual) + ")");
        }
    }

    /**
     * Checks equality of two long values.
     * <p>
     * long 값 두 개의 같음을 검사한다.
     *
     * @param expected expected value / 기대 값
     * @param actual   actual value / 실제 값
     * @param message  failure description / 실패 설명
     */
    public static void equal(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionFailure(message + " (expected " + expected + " but was " + actual + ")");
        }
    }

    /**
     * Checks that two doubles differ by at most a tolerance.
     * <p>
     * double 값 두 개의 차이가 허용 오차 이하인지 검사한다.
     *
     * @param expected  expected value / 기대 값
     * @param actual    actual value / 실제 값
     * @param tolerance maximum allowed difference / 허용 오차
     * @param message   failure description / 실패 설명
     */
    public static void near(double expected, double actual, double tolerance, String message) {
        if (Double.isNaN(expected) ? !Double.isNaN(actual) : Math.abs(expected - actual) > tolerance) {
            throw new AssertionFailure(message + " (expected " + expected + " +/- " + tolerance + " but was " + actual + ")");
        }
    }

    /**
     * Checks that two references are the same object.
     * <p>
     * 두 참조가 같은 객체인지 검사한다.
     *
     * @param expected expected object / 기대 객체
     * @param actual   actual object / 실제 객체
     * @param message  failure description / 실패 설명
     */
    public static void same(Object expected, Object actual, String message) {
        if (expected != actual) {
            throw new AssertionFailure(message + " (not the same instance)");
        }
    }

    /**
     * Checks that a string contains a fragment.
     * <p>
     * 문자열이 조각을 포함하는지 검사한다.
     *
     * @param text     text to search in / 검색 대상 문자열
     * @param fragment expected fragment / 기대 조각
     * @param message  failure description / 실패 설명
     */
    public static void contains(String text, String fragment, String message) {
        if (text == null || text.indexOf(fragment) < 0) {
            throw new AssertionFailure(message + " (expected to contain " + describe(fragment) + " but was " + describe(text) + ")");
        }
    }

    /**
     * Checks that a block throws an exception of the given type (or a subclass).
     * <p>
     * 코드 조각이 주어진 형식(또는 하위 형식)의 예외를 던지는지 검사한다.
     *
     * @param type    expected exception class / 기대 예외 클래스
     * @param block   code to run / 실행할 코드
     * @param message failure description / 실패 설명
     * @return the thrown exception, for further checks / 추가 검사를 위해 던져진 예외를 반환
     */
    public static Throwable throwsException(Class<? extends Throwable> type, Block block, String message) {
        try {
            block.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) {
                return t;
            }
            throw new AssertionFailure(message + " (expected " + type.getName() + " but " + t + " was thrown)");
        }
        throw new AssertionFailure(message + " (expected " + type.getName() + " but nothing was thrown)");
    }

    /**
     * Compares two values, treating arrays by content.
     * <p>
     * 두 값을 비교한다. 배열은 내용으로 비교한다.
     *
     * @param a first value / 첫 번째 값
     * @param b second value / 두 번째 값
     * @return true if equal / 같으면 true
     */
    private static boolean sameValue(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof Object[] && b instanceof Object[]) {
            return Arrays.deepEquals((Object[]) a, (Object[]) b);
        }
        if (a instanceof byte[] && b instanceof byte[]) {
            return Arrays.equals((byte[]) a, (byte[]) b);
        }
        if (a instanceof int[] && b instanceof int[]) {
            return Arrays.equals((int[]) a, (int[]) b);
        }
        if (a instanceof char[] && b instanceof char[]) {
            return Arrays.equals((char[]) a, (char[]) b);
        }
        return a.equals(b);
    }

    /**
     * Formats a value for a failure message.
     * <p>
     * 실패 메시지용으로 값을 문자열로 만든다.
     *
     * @param v value / 값
     * @return readable text / 읽기 쉬운 문자열
     */
    private static String describe(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof String) {
            return "\"" + v + "\"";
        }
        if (v instanceof Object[]) {
            return Arrays.deepToString((Object[]) v);
        }
        if (v instanceof byte[]) {
            return Arrays.toString((byte[]) v);
        }
        if (v instanceof int[]) {
            return Arrays.toString((int[]) v);
        }
        return String.valueOf(v);
    }
}
