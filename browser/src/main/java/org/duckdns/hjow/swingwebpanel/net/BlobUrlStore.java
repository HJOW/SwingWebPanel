package org.duckdns.hjow.swingwebpanel.net;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Component-owned File API blob URL store. Entries hold immutable bytes and the creating environment's origin and lifetime token.
 * The component is one top-level storage partition. Opaque origins within it have distinct keys, and child workers inherit the creator's key.
 * <p>
 * 컴포넌트가 소유한 File API blob URL 저장소. 항목은 변경 불가 바이트, 생성 환경의 오리진과 수명 토큰을 보관한다.
 * 컴포넌트는 최상위 저장소 파티션 하나이다. 내부의 불투명 오리진은 키가 서로 다르며 하위 워커는 생성자의 키를 상속한다.
 */
public final class BlobUrlStore {
    /** Live object URLs indexed by their serialized addresses. / 직렬화한 주소로 찾는 유효 객체 URL. */
    private final Map<String, Entry> entries = new HashMap<String, Entry>();

    /** Creates an empty store for one component. / 컴포넌트 하나의 빈 저장소를 만든다. */
    public BlobUrlStore() {
    }

    /**
     * One environment's registrations, released together when the environment is destroyed.
     * <p>
     * 환경 하나의 등록. 환경을 폐기하면 함께 해제한다.
     */
    public static final class Scope {
        /** Storage partition that owns this scope. / 이 범위를 보유한 저장소 파티션. */
        private final BlobUrlStore store;
        /** Document URL used to establish the origin. / 오리진을 확정하는 문서 URL. */
        private final String originUrl;
        /** Origin under which the bytes were created. / 바이트가 생성된 오리진. */
        private final Origin origin;
        /** Equality key shared only by related opaque environments. / 관련된 불투명 환경만 공유하는 동일성 키. */
        private final Object opaqueKey;
        /** Whether this environment was destroyed, guarded by the store. / 환경 폐기 여부. 저장소로 보호한다. */
        private boolean closed;

        /**
         * Creates a scope with the specified origin and opaque identity.
         * <p>
         * 지정한 오리진과 불투명 동일성으로 범위를 만든다.
         *
         * @param store owning store / 소유 저장소
         * @param originUrl creating document address / 생성 문서 주소
         * @param origin creating origin / 생성 오리진
         * @param opaqueKey opaque environment identity / 불투명 환경 동일성
         */
        private Scope(BlobUrlStore store, String originUrl, Origin origin, Object opaqueKey) {
            this.store = store;
            this.originUrl = originUrl;
            this.origin = origin;
            this.opaqueKey = opaqueKey;
        }

        /**
         * Opens a distinct lifetime for a worker that shares this scope's origin.
         * <p>
         * 이 범위의 오리진을 공유하는 워커에 별도 수명을 연다.
         *
         * @return a new worker scope / 새 워커 범위
         */
        public Scope fork() {
            synchronized (store) {
                Scope child = new Scope(store, originUrl, origin, opaqueKey);
                child.closed = closed;
                return child;
            }
        }

        /**
         * Creates a URL for immutable blob data, copying the caller's byte array.
         * <p>
         * 호출자의 바이트 배열을 복사해 변경 불가 blob 데이터의 URL을 만든다.
         *
         * @param bytes blob contents / blob 내용
         * @param mime blob media type / blob 미디어 타입
         * @return the generated URL / 생성한 URL
         */
        public String create(byte[] bytes, String mime) {
            return store.create(this, bytes.clone(), mime);
        }

        /**
         * Revokes a URL in the same partition and origin. Unknown URLs have no effect.
         * <p>
         * 같은 파티션과 오리진의 URL을 해제한다. 알 수 없는 URL에는 효과가 없다.
         *
         * @param url serialized URL / 직렬화한 URL
         */
        public void revoke(String url) {
            store.revoke(this, url);
        }

        /** Removes this environment's URLs. / 이 환경의 URL을 제거한다. */
        public void close() {
            store.close(this);
        }
    }

    /**
     * A resolved URL whose immutable bytes can survive revocation during an already started request.
     * <p>
     * 조회한 URL. 이미 시작한 요청에서는 해제 뒤에도 변경 불가 바이트를 사용할 수 있다.
     */
    public static final class Entry {
        /** Owning environment and origin. / 소유 환경과 오리진. */
        private final Scope owner;
        /** URL without a fragment. / 프래그먼트가 없는 URL. */
        private final String url;
        /** Immutable blob contents. / 변경 불가 blob 내용. */
        private final byte[] bytes;
        /** Blob media type. / blob 미디어 타입. */
        private final String mime;

        /**
         * Creates an entry for one object URL.
         * <p>
         * 객체 URL 하나의 항목을 만든다.
         *
         * @param owner creating environment / 생성 환경
         * @param url assigned URL / 배정한 URL
         * @param bytes owned data / 소유하는 데이터
         * @param mime media type / 미디어 타입
         */
        private Entry(Scope owner, String url, byte[] bytes, String mime) {
            this.owner = owner;
            this.url = url;
            this.bytes = bytes;
            this.mime = mime == null ? "" : mime;
        }

        /** Returns the creator's document address for origin isolation. / 오리진 격리의 기준인 생성자 문서 주소를 반환한다. */
        public String getOriginUrl() {
            return owner.originUrl;
        }

        /** Returns the blob media type. / blob 미디어 타입을 반환한다. */
        public String getMimeType() {
            return mime;
        }

        /** Returns the exact byte count. / 정확한 바이트 수를 반환한다. */
        public int length() {
            return bytes.length;
        }

        /**
         * Returns independent bytes for a buffered consumer.
         * <p>
         * 버퍼 소비자용 독립 바이트를 반환한다.
         *
         * @param start first byte offset / 첫 바이트 위치
         * @param count number of bytes / 바이트 수
         * @return independent bytes / 독립 바이트
         */
        byte[] copy(int start, int count) {
            byte[] copy = new byte[count];
            System.arraycopy(bytes, start, copy, 0, count);
            return copy;
        }

        /**
         * Writes a byte interval in chunks without exposing the stored bytes to the output stream.
         * <p>
         * 저장 바이트를 출력 스트림에 노출하지 않고 바이트 구간을 청크로 쓴다.
         *
         * @param out output sink / 출력 목적지
         * @param start first byte offset / 첫 바이트 위치
         * @param count number of bytes / 바이트 수
         * @param cancel request cancellation / 요청 취소
         * @throws IOException if cancelled or writing fails / 취소 또는 쓰기 실패 시
         */
        void write(OutputStream out, int start, int count, CancelToken cancel) throws IOException {
            int end = start + count;
            byte[] buffer = new byte[Math.min(count, 8192)];
            // 목적지가 오래 기다리거나 배열을 저장해도 등록 바이트는 영향을 받지 않는다.
            for (int position = start; position < end; ) {
                cancel.throwIfCancelled();
                int n = Math.min(end - position, buffer.length);
                System.arraycopy(bytes, position, buffer, 0, n);
                out.write(buffer, 0, n);
                position += n;
            }
        }
    }

    /**
     * Opens a new document environment using its effective origin address.
     * <p>
     * 유효 오리진 주소를 사용하여 새 문서 환경을 연다.
     *
     * @param originUrl document origin address / 문서 오리진 주소
     * @return scope for the document lifetime / 문서 수명 범위
     */
    public Scope open(String originUrl) {
        return new Scope(this, originUrl, Origin.of(originUrl), new Object());
    }

    /**
     * Looks up bytes for an origin-checked resource fetch or for a navigation with a valid URL.
     * <p>
     * 오리진을 검사하는 리소스 요청이나 유효 주소의 이동에 쓸 바이트를 찾는다.
     *
     * @param url requested address / 요청 주소
     * @param scope requesting environment, if available / 요청 환경. 알 수 있으면 지정
     * @param initiator requesting origin when no scope is available / 환경이 없을 때 요청 오리진
     * @param navigation true for a navigation allowed to resolve other origins / 다른 오리진 조회가 허용된 이동이면 true
     * @return resolved entry or null / 조회한 항목 또는 null
     */
    public synchronized Entry resolve(String url, Scope scope, Origin initiator, boolean navigation) {
        Entry entry = entries.get(key(url, true));
        if (entry == null || (scope != null && (scope.closed || scope.store != this))) {
            return null;
        }
        return navigation || mayRead(entry, scope, initiator) ? entry : null;
    }

    /**
     * Confirms that an in-flight entry belongs to this store and this exact request.
     * <p>
     * 시작한 요청의 항목이 이 저장소와 지정한 요청의 것인지 확인한다.
     *
     * @param entry resolved entry / 조회한 항목
     * @param url requested address / 요청 주소
     * @return whether the entry may be reused / 항목을 재사용해도 되는지 여부
     */
    boolean owns(Entry entry, String url) {
        return entry != null && entry.owner.store == this && entry.url.equals(key(url, true));
    }

    /** Generates and registers a URL within the owning scope. / 소유 범위에 URL을 생성하여 등록한다. */
    private synchronized String create(Scope scope, byte[] bytes, String mime) {
        if (scope.closed) {
            throw new IllegalStateException("Object URL environment is closed");
        }
        String url;
        // 저장소에 같은 UUID가 이미 있으면 다시 생성한다.
        do {
            url = "blob:" + scope.origin.serialize() + "/" + UUID.randomUUID().toString();
        } while (entries.containsKey(url));
        entries.put(url, new Entry(scope, url, bytes, mime));
        return url;
    }

    /** Revokes the serialized address if the scope may access it. / 범위에 접근 권한이 있으면 직렬화한 주소를 해제한다. */
    private synchronized void revoke(Scope scope, String url) {
        String key = key(url, false);
        Entry entry = entries.get(key);
        if (!scope.closed && entry != null && mayRead(entry, scope, null)) {
            entries.remove(key);
        }
    }

    /** Removes entries created by the closing scope. / 닫는 범위가 생성한 항목을 지운다. */
    private synchronized void close(Scope scope) {
        if (scope.closed) {
            return;
        }
        scope.closed = true;
        // 다른 문서나 워커의 등록은 보존하고 현재 소유 범위만 정리한다.
        for (Iterator<Entry> i = entries.values().iterator(); i.hasNext(); ) {
            if (i.next().owner == scope) {
                i.remove();
            }
        }
    }

    /** Compares tuple origins or their opaque storage keys. / 튜플 오리진이나 불투명 저장소 키를 비교한다. */
    private boolean mayRead(Entry entry, Scope scope, Origin initiator) {
        Origin origin = scope == null ? initiator : scope.origin;
        return entry.owner.origin.isSameOrigin(origin) || (scope != null && entry.owner.opaqueKey == scope.opaqueKey);
    }

    /** Normalizes a blob URL and optionally excludes its fragment. / blob URL을 정규화하고 필요한 경우 프래그먼트를 제외한다. */
    private static String key(String url, boolean excludeFragment) {
        if (!"blob".equals(UrlUtil.getScheme(url))) {
            return null;
        }
        try {
            return UrlUtil.normalize(excludeFragment ? UrlUtil.stripFragment(url) : url);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
