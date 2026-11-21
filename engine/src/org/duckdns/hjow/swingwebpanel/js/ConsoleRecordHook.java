package org.duckdns.hjow.swingwebpanel.js;

/**
 * A {@link ConsoleHook} that also wants the structured form of every message. The engine calls {@link #record(ConsoleRecord)} instead of
 * {@link #message(String, String)} for a hook of this type, so an implementation gets each message exactly once: it must implement
 * <code>message</code> too (for callers that still use the plain form) and normally forwards it as a record without a position.
 * <p>
 * 모든 메시지의 구조화된 형태까지 받으려는 {@link ConsoleHook}. 엔진은 이 타입의 연결에는 {@link #message(String, String)} 대신
 * {@link #record(ConsoleRecord)} 를 호출하므로 구현은 메시지를 정확히 한 번 받는다. 아직 일반 형태를 쓰는 호출자를 위해
 * <code>message</code> 도 구현해야 하며, 보통 위치 정보 없는 기록으로 바꿔 전달한다.
 */
public interface ConsoleRecordHook extends ConsoleHook {

    /**
     * Receives one console record. It runs on the thread that runs the script, inside the script task, so it must return quickly and must
     * not call back into the engine.
     * <p>
     * 콘솔 기록 하나를 받는다. 스크립트를 실행하는 스레드에서 스크립트 작업 안에서 실행되므로 빨리 반환해야 하며 엔진을 다시 호출하면 안 된다.
     *
     * @param record the record, not null / 기록(null 불가)
     */
    void record(ConsoleRecord record);
}
