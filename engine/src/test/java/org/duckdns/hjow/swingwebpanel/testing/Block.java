package org.duckdns.hjow.swingwebpanel.testing;

/**
 * A piece of code that may throw, used with {@link Check#throwsException(Class, Block, String)}.
 * Java 6 has no lambdas, so tests pass an anonymous class implementing this interface.
 * <p>
 * 예외를 던질 수 있는 코드 조각. {@link Check#throwsException(Class, Block, String)} 에서 사용한다.
 * Java 6 에는 람다가 없으므로 시험 코드는 이 인터페이스를 구현한 익명 클래스를 넘긴다.
 */
public interface Block {

    /**
     * Runs the code.
     * <p>
     * 코드를 실행한다.
     *
     * @throws Exception any failure / 모든 실패
     */
    void run() throws Exception;
}
