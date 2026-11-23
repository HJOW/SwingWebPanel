/**
 * Web Workers and the <code>BroadcastChannel</code> (plan item P20-09-b): a dedicated worker runs a script in a realm of its own, on a thread
 * of its own, with a restricted set of objects (no window, no document, no DOM, no storage), and talks to the page only through messages that
 * are copied with the structured clone algorithm and delivered as tasks of the receiving side.
 * <p>
 * Web Worker 와 <code>BroadcastChannel</code>(계획 P20-09-b): 전용 worker 는 자기 realm 과 자기 스레드에서, 제한된 객체 집합(window, document,
 * DOM, 저장소 없음)으로 스크립트를 실행하며, 구조화 복제 알고리즘으로 복사해 받는 쪽의 작업으로 전달하는 메시지로만 페이지와 대화한다.
 */
package org.duckdns.hjow.swingwebpanel.js.worker;
