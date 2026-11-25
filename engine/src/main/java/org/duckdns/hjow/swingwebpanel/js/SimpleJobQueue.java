package org.duckdns.hjow.swingwebpanel.js;

import java.util.LinkedList;

/**
 * A plain FIFO {@link JobQueue} with a drain loop, used as the default queue of a realm and in tests. Jobs enqueued while the
 * queue is being drained run in the same drain, as the microtask checkpoint of a browser does.
 * <p>
 * 비우기 반복을 가진 단순한 FIFO {@link JobQueue}. realm 의 기본 큐와 시험에서 쓴다. 큐를 비우는 중에 추가된 작업은 같은 비우기에서
 * 실행된다(브라우저의 마이크로태스크 체크포인트와 같다).
 */
public final class SimpleJobQueue implements JobQueue {

    /**
     * The pending jobs.
     * <p>
     * 대기 중인 작업들.
     */
    private final LinkedList<Runnable> jobs = new LinkedList<Runnable>();

    /**
     * Creates an empty queue.
     * <p>
     * 빈 큐를 만든다.
     */
    public SimpleJobQueue() {
    }

    /**
     * Adds a job.
     * <p>
     * 작업을 추가한다.
     *
     * @param job the job / 작업
     */
    public synchronized void enqueue(Runnable job) {
        jobs.addLast(job);
    }

    /**
     * Returns true if no job is pending.
     * <p>
     * 대기 중인 작업이 없으면 true.
     *
     * @return true if empty / 비어 있으면 true
     */
    public synchronized boolean isEmpty() {
        return jobs.isEmpty();
    }

    /**
     * Runs the oldest job.
     * <p>
     * 가장 오래된 작업을 실행한다.
     *
     * @return true if a job was run / 작업을 실행했으면 true
     */
    public boolean runOne() {
        Runnable job;
        synchronized (this) {
            job = jobs.pollFirst();
        }
        if (job == null) {
            return false;
        }
        job.run();
        return true;
    }

    /**
     * Runs jobs until the queue is empty (including jobs added by running jobs).
     * <p>
     * 큐가 빌 때까지(실행 중 추가된 작업 포함) 작업을 실행한다.
     */
    public void runAll() {
        // 큐가 빌 때까지 작업을 하나씩 실행한다.
        while (runOne()) {
            continue;
        }
    }
}
