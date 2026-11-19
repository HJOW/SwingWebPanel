package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.duckdns.hjow.swingwebpanel.js.JobQueue;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.SimpleJobQueue;

/**
 * Where promise jobs go and who is told about unhandled rejections. Promise jobs (PromiseReactionJob, PromiseResolveThenableJob)
 * are never run synchronously: they are put on the job queue of the realm and run when the host drains the queue (the event loop
 * of the browser, or <code>runAll()</code> in tests).
 * <p>
 * The queue is chosen in this order: a queue installed with {@link #setQueue(JsRealm, JobQueue)}; the queue of the realm itself if
 * the realm class offers <code>getJobQueue()</code> (the engine core adds it; it is looked up by reflection so that this package does
 * not depend on it); otherwise a private {@link SimpleJobQueue} that the host must drain with {@link #drain(JsRealm)}.
 * The per-realm data lives in a hidden intrinsic, so it is released together with the realm.
 * <p>
 * promise 작업이 어디로 가는지, 처리되지 않은 거부를 누구에게 알리는지 정한다. promise 작업(PromiseReactionJob,
 * PromiseResolveThenableJob)은 절대 동기적으로 실행되지 않는다. realm 의 작업 큐에 넣어 두었다가 호스트가 큐를 비울 때(브라우저의
 * 이벤트 루프, 시험에서는 <code>runAll()</code>) 실행한다.
 * <p>
 * 큐는 다음 순서로 고른다: {@link #setQueue(JsRealm, JobQueue)} 로 설치한 큐, realm 클래스가 <code>getJobQueue()</code> 를 제공하면
 * realm 자신의 큐(엔진 코어가 추가하며 이 패키지가 코어에 의존하지 않도록 리플렉션으로 찾는다), 그 밖에는 호스트가
 * {@link #drain(JsRealm)} 으로 비워야 하는 비공개 {@link SimpleJobQueue}. realm 별 자료는 숨겨진 내장 객체에 있으므로 realm 과 함께 해제된다.
 */
public final class PromiseJobs {

    /**
     * Name of the hidden intrinsic that holds the per-realm data.
     * <p>
     * realm 별 자료를 담는 숨겨진 내장 객체의 이름.
     */
    private static final String INTRINSIC_NAME = "%PromiseRealmData%";

    /**
     * The <code>getJobQueue()</code> method of the realm class, or null if the realm has none (yet).
     * <p>
     * realm 클래스의 <code>getJobQueue()</code> 메서드. 없으면(아직) null.
     */
    private static final Method REALM_QUEUE_GETTER = findGetter();

    /**
     * Per-realm data stored as an intrinsic (a JsObject subclass so it fits the intrinsic registry).
     * <p>
     * 내장 객체로 저장하는 realm 별 자료(내장 객체 등록부에 맞도록 JsObject 하위 클래스).
     */
    private static final class RealmData extends JsObject {
        /**
         * Queue installed by the host, or null.
         * <p>
         * 호스트가 설치한 큐. 없으면 null.
         */
        JobQueue queue;

        /**
         * Private queue used when nothing else is available.
         * <p>
         * 다른 것이 없을 때 쓰는 비공개 큐.
         */
        SimpleJobQueue fallback;

        /**
         * Rejection observer, or null.
         * <p>
         * 거부 관찰자. 없으면 null.
         */
        PromiseRejectionTracker tracker;
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PromiseJobs() {
    }

    /**
     * Looks up <code>JsRealm.getJobQueue()</code> once.
     * <p>
     * <code>JsRealm.getJobQueue()</code> 를 한 번 찾는다.
     *
     * @return the method or null / 메서드 또는 null
     */
    private static Method findGetter() {
        try {
            return JsRealm.class.getMethod("getJobQueue");
        } catch (NoSuchMethodException e) {
            return null;
        } catch (SecurityException e) {
            return null;
        }
    }

    /**
     * Returns the per-realm data, creating it on first use.
     * <p>
     * realm 별 자료를 반환한다. 처음 쓸 때 만든다.
     *
     * @param realm the realm / realm
     * @return the data / 자료
     */
    private static synchronized RealmData data(JsRealm realm) {
        JsObject o = realm.getIntrinsic(INTRINSIC_NAME);
        if (o instanceof RealmData) {
            return (RealmData) o;
        }
        RealmData d = new RealmData();
        realm.setIntrinsic(INTRINSIC_NAME, d);
        return d;
    }

    /**
     * Installs the queue that receives the jobs of this realm.
     * <p>
     * 이 realm 의 작업을 받을 큐를 설치한다.
     *
     * @param realm the realm / realm
     * @param queue the queue, or null to go back to the default choice / 큐. 기본 선택으로 되돌리려면 null
     */
    public static void setQueue(JsRealm realm, JobQueue queue) {
        data(realm).queue = queue;
    }

    /**
     * Returns the queue that receives the jobs of this realm (see the class description for the order of choice).
     * <p>
     * 이 realm 의 작업을 받는 큐를 반환한다(선택 순서는 클래스 설명 참조).
     *
     * @param realm the realm / realm
     * @return the queue / 큐
     */
    public static JobQueue queueOf(JsRealm realm) {
        RealmData d = data(realm);
        if (d.queue != null) {
            return d.queue;
        }
        if (REALM_QUEUE_GETTER != null) {
            try {
                Object q = REALM_QUEUE_GETTER.invoke(realm);
                if (q instanceof JobQueue) {
                    return (JobQueue) q;
                }
            } catch (IllegalAccessException e) {
                // 접근할 수 없으면 비공개 큐로 대체한다.
                d.fallback = d.fallback == null ? new SimpleJobQueue() : d.fallback;
            } catch (InvocationTargetException e) {
                d.fallback = d.fallback == null ? new SimpleJobQueue() : d.fallback;
            }
        }
        if (d.fallback == null) {
            d.fallback = new SimpleJobQueue();
        }
        return d.fallback;
    }

    /**
     * Puts a job on the queue of the realm; it will run later, never inside this call.
     * <p>
     * realm 의 큐에 작업을 넣는다. 작업은 나중에 실행되며 이 호출 안에서 실행되지 않는다.
     *
     * @param realm the realm / realm
     * @param job   the job / 작업
     */
    public static void enqueue(JsRealm realm, Runnable job) {
        queueOf(realm).enqueue(job);
    }

    /**
     * Runs all queued jobs if the queue of the realm is a {@link SimpleJobQueue} (including the private default); other queues are
     * drained by their owner.
     * <p>
     * realm 의 큐가 {@link SimpleJobQueue}(비공개 기본 큐 포함)이면 대기 중인 모든 작업을 실행한다. 다른 큐는 소유자가 비운다.
     *
     * @param realm the realm / realm
     * @return true if a simple queue was drained / 단순 큐를 비웠으면 true
     */
    public static boolean drain(JsRealm realm) {
        JobQueue q = queueOf(realm);
        if (q instanceof SimpleJobQueue) {
            ((SimpleJobQueue) q).runAll();
            return true;
        }
        return false;
    }

    /**
     * Installs (or with null removes) the observer for unhandled rejections of this realm.
     * <p>
     * 이 realm 의 처리되지 않은 거부 관찰자를 설치한다(null 이면 제거).
     *
     * @param realm   the realm / realm
     * @param tracker the observer or null / 관찰자 또는 null
     */
    public static void setRejectionTracker(JsRealm realm, PromiseRejectionTracker tracker) {
        data(realm).tracker = tracker;
    }

    /**
     * Returns the rejection observer of the realm, or null.
     * <p>
     * realm 의 거부 관찰자를 반환한다. 없으면 null.
     *
     * @param realm the realm / realm
     * @return the observer or null / 관찰자 또는 null
     */
    public static PromiseRejectionTracker getRejectionTracker(JsRealm realm) {
        return data(realm).tracker;
    }
}
