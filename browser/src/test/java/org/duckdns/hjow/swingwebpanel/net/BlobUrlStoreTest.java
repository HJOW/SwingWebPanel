package org.duckdns.hjow.swingwebpanel.net;

import java.util.Collections;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Checks object URL partitioning and the common request layer's local responses.
 * <p>
 * 객체 URL 파티션과 공통 요청 계층의 로컬 응답을 검사한다.
 */
public final class BlobUrlStoreTest {
    /** Creates the suite. / 시험 묶음을 만든다. */
    public BlobUrlStoreTest() {
    }

    /**
     * Verifies byte isolation, request metadata, fragments and ranges.
     * <p>
     * 바이트 격리, 요청 메타데이터, 프래그먼트와 바이트 범위를 검증한다.
     */
    public void testLocalResponseAndByteRange() {
        ResourceFetcher fetcher = new ResourceFetcher(null, FetchLimits.DEFAULT, null);
        BlobUrlStore.Scope owner = fetcher.getBlobUrlStore().open("https://example.test/page");
        byte[] bytes = { 1, 2, 3 };
        String url = owner.create(bytes, "application/test");
        bytes[1] = 41;
        RequestHeaders headers = new RequestHeaders();
        headers.addAll(Collections.singletonMap("Range", "bytes=1-"));
        ResourceResponse range = fetcher.fetch(new ResourceRequest(url + "#part").setBlobUrlScope(owner).setExtraHeaders(headers));
        Check.equal(206, range.getStatus(), "a range is a partial response");
        Check.equal("bytes 1-2/3", range.getHeader("Content-Range"), "range metadata");
        Check.equal("application/test", range.getMimeType(), "Blob MIME type");
        Check.equal(2, range.getBody()[0], "registration snapshots the bytes");
        Check.equal("https://example.test/page", range.getBlobUrlEntry().getOriginUrl(), "creator metadata survives response filtering");
        Check.equal("https://example.test", Origin.of(url).serialize(), "tuple origin is serialized into a blob URL");
        Check.equal(FailureKind.NOT_FOUND, fetcher.fetch(new ResourceRequest(url + "?different").setBlobUrlScope(owner)).getFailureKind(),
                "query is part of the identifier");
        CancelToken cancelled = new CancelToken();
        cancelled.cancel();
        Check.equal(FailureKind.CANCELLED, fetcher.fetch(new ResourceRequest(url).setBlobUrlScope(owner).setCancelToken(cancelled)).getFailureKind(),
                "the local request is cancellable");
    }

    /**
     * Checks same-origin reads, revocation and a request that retained an entry before revocation.
     * <p>
     * 동일 오리진 읽기, 해제 및 해제 전에 항목을 보관한 요청을 검사한다.
     */
    public void testOriginAndRevocation() {
        ResourceFetcher fetcher = new ResourceFetcher(null, FetchLimits.DEFAULT, null);
        BlobUrlStore store = fetcher.getBlobUrlStore();
        BlobUrlStore.Scope creator = store.open("https://example.test/a");
        BlobUrlStore.Scope same = store.open("https://example.test/b");
        BlobUrlStore.Scope other = store.open("https://different.test/a");
        String url = creator.create(new byte[] { 7 }, "");
        BlobUrlStore.Entry accepted = store.resolve(url, same, null, false);
        Check.notNull(accepted, "a same-origin scope can load the entry");
        other.revoke(url);
        Check.isNull(store.resolve(url, other, null, false), "a different origin cannot read or revoke the entry");
        Check.same(accepted, store.resolve(url, other, null, true), "a top-level navigation can resolve a valid URL in the component");
        same.revoke(url + "#part");
        Check.same(accepted, store.resolve(url, same, null, false), "revocation serializes the supplied address");
        same.revoke(url);
        Check.isNull(store.resolve(url, creator, null, false), "revocation affects future lookups");
        Check.equal(7, fetcher.fetch(new ResourceRequest(url).setBlobUrlEntry(accepted)).getBody()[0], "a started request keeps its bytes");
        ResourceFetcher otherPanel = new ResourceFetcher(null, FetchLimits.DEFAULT, null);
        Check.equal(FailureKind.NOT_FOUND, otherPanel.fetch(new ResourceRequest(url).setBlobUrlEntry(accepted)).getFailureKind(),
                "an accepted entry stays within its component");
    }

    /**
     * Checks opaque identity and independent worker lifetimes.
     * <p>
     * 불투명 동일성과 독립 워커 수명을 검사한다.
     */
    public void testOpaqueWorkerScopeAndLifetime() {
        BlobUrlStore store = new BlobUrlStore();
        BlobUrlStore.Scope creator = store.open("about:text");
        BlobUrlStore.Scope worker = creator.fork();
        BlobUrlStore.Scope unrelated = store.open("about:text");
        String a = creator.create(new byte[] { 5 }, "");
        String b = worker.create(new byte[] { 9 }, "");
        Check.isTrue(a.startsWith("blob:null/"), "opaque origins serialize as null");
        Check.notNull(store.resolve(a, worker, null, false), "the worker shares its owner's opaque storage key");
        Check.isNull(store.resolve(a, unrelated, null, false), "unrelated opaque documents have different keys");
        worker.close();
        Check.isNull(store.resolve(b, creator, null, false), "worker termination releases only worker URLs");
        Check.notNull(store.resolve(a, creator, null, false), "the document remains active");
        creator.close();
        Check.isNull(store.resolve(a, null, null, true), "even navigation fails after unloading the creator");
    }
}
