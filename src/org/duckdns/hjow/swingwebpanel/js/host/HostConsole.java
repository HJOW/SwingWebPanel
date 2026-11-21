package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.js.ConsoleRecord;
import org.duckdns.hjow.swingwebpanel.js.ConsoleRecordHook;

/**
 * The console hook of a page that also knows where a message comes from when it was not written by the page's own scripts: a worker of the
 * page. The page host ({@link PageScripts}) forwards the output of the page's workers through it; a plain hook just prints the record.
 * <p>
 * 페이지의 스크립트가 직접 쓰지 않은 메시지의 출처, 곧 페이지의 worker 까지 아는 페이지의 콘솔 연결. 페이지 호스트({@link PageScripts})가
 * 페이지 worker 의 출력을 이것을 통해 전달하며, 일반 연결은 기록을 그대로 출력할 뿐이다.
 */
public interface HostConsole extends ConsoleRecordHook {

    /**
     * Receives a record that a worker of the page wrote. It is called on the thread of the worker that wrote it.
     * <p>
     * 페이지의 worker 가 쓴 기록을 받는다. 그것을 쓴 worker 의 스레드에서 호출된다.
     *
     * @param record    the record the worker's console made / worker 의 콘솔이 만든 기록
     * @param workerUrl the script URL of the worker / worker 의 스크립트 URL
     * @param shared    true for a shared worker / 공유 worker 이면 true
     */
    void workerRecord(ConsoleRecord record, String workerUrl, boolean shared);
}
