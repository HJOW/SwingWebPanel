package org.duckdns.hjow.swingwebpanel.js;

/**
 * An iterator and the next method captured when the iterator is acquired.
 * <p>
 * 이터레이터와 이터레이터를 획득할 때 읽은 next 메서드.
 */
public final class IteratorRecord {

    /** The iterator object. / 이터레이터 객체. */
    private final JsObject iterator;

    /** The next method read at acquisition time. / 획득할 때 읽은 next 메서드. */
    private final JsValue nextMethod;

    /** Whether iteration has finished or been closed. / 순회를 끝냈거나 이터레이터를 닫았는지 여부. */
    private boolean done;

    /**
     * Gets a record directly from an iterator object.
     * <p>
     * 이터레이터 객체로부터 직접 기록을 만든다.
     *
     * @param iterator the iterator object / 이터레이터 객체
     */
    public IteratorRecord(JsObject iterator) {
        this.iterator = iterator;
        this.nextMethod = iterator.get("next");
    }

    /**
     * Acquires the synchronous iterator of an iterable and captures its next method.
     * <p>
     * 반복 가능한 값의 동기 이터레이터를 획득하고 next 메서드를 읽어 둔다.
     *
     * @param realm the operating realm / 실행 realm
     * @param iterable source iterable / 원본 반복 가능 값
     * @return the iterator record / 이터레이터 기록
     */
    public static IteratorRecord from(JsRealm realm, JsValue iterable) {
        return new IteratorRecord(IteratorHelper.getIterator(realm, iterable));
    }

    /**
     * Advances the iterator and returns its value, or Java null when its done property is true.
     * <p>
     * 이터레이터의 다음 값을 반환한다. done 프로퍼티가 참이면 Java null을 반환한다.
     *
     * @return next ECMAScript value or Java null / 다음 ECMAScript 값 또는 Java null
     */
    public JsValue stepValue() {
        if (done) {
            return null;
        }
        try {
            JsObject result = IteratorHelper.next(iterator, nextMethod, null);
            if (IteratorHelper.isDone(result)) {
                done = true;
                return null;
            }
            return IteratorHelper.value(result);
        } catch (JsException e) {
            // 이터레이터 자체의 오류는 값을 소비하던 코드의 오류와 달리 이 자리에서 반복을 끝낸다.
            done = true;
            throw e;
        }
    }

    /**
     * Closes a live iterator when the surrounding operation has already thrown.
     * <p>
     * 바깥 연산이 이미 오류를 던진 경우 진행 중인 이터레이터를 닫는다.
     */
    public void closeOnError() {
        if (!done) {
            done = true;
            IteratorHelper.closeQuietly(iterator);
        }
    }

    /**
     * Closes the live iterator on early normal completion, propagating any return method error.
     * <p>
     * 정상적으로 앞서 끝난 경우 진행 중인 이터레이터를 닫고 return 메서드의 오류를 전파한다.
     */
    public void close() {
        if (!done) {
            done = true;
            IteratorHelper.close(iterator);
        }
    }
}
