package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.html.EncodingLabels;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;

/**
 * Installs <code>FileReader</code> (File API, section 6): the asynchronous reader of a <code>Blob</code> with <code>readAsArrayBuffer</code>,
 * <code>readAsBinaryString</code>, <code>readAsText</code>, <code>readAsDataURL</code> and <code>abort</code>, the states EMPTY, LOADING and DONE, the
 * <code>result</code>, and the events <code>loadstart</code>, <code>progress</code>, <code>load</code>, <code>abort</code>, <code>error</code>, <code>loadend</code>
 * with the <code>on...</code> handlers. A blob lives in memory, so a read has two page tasks (<code>loadstart</code>; then the result with
 * <code>progress</code>, <code>load</code>, <code>loadend</code>) and a script can still <code>abort()</code> in between. A file can only be read through
 * the blob that a script holds; the page never gets access to the local disk.
 * <p>
 * <code>FileReader</code> 를 설치한다(File API 6절): <code>readAsArrayBuffer</code>, <code>readAsBinaryString</code>, <code>readAsText</code>,
 * <code>readAsDataURL</code>, <code>abort</code> 를 가진 <code>Blob</code> 의 비동기 읽기 객체, EMPTY·LOADING·DONE 상태, <code>result</code>, 그리고
 * <code>on...</code> 핸들러가 있는 <code>loadstart</code>, <code>progress</code>, <code>load</code>, <code>abort</code>, <code>error</code>, <code>loadend</code> 이벤트.
 * 블롭은 메모리에 있으므로 읽기는 페이지 작업 둘(<code>loadstart</code>, 이어서 <code>progress</code>·<code>load</code>·<code>loadend</code> 와 결과)이며 그 사이에도
 * 스크립트가 <code>abort()</code> 할 수 있다. 파일은 스크립트가 쥔 블롭으로만 읽을 수 있으며 페이지는 로컬 디스크에 접근하지 못한다.
 */
final class FileReaderApi {

    /** State: no read started. / 상태: 시작한 읽기 없음. */
    static final int EMPTY = 0;

    /** State: a read is running. / 상태: 읽기 실행 중. */
    static final int LOADING = 1;

    /** State: the read ended. / 상태: 읽기가 끝났다. */
    static final int DONE = 2;

    /** The events of a FileReader. / FileReader 의 이벤트. */
    private static final String[] EVENTS = { "loadstart", "progress", "load", "abort", "error", "loadend" };

    /**
     * The Java side of a <code>FileReader</code>: an event target with the state of the read.
     * <p>
     * <code>FileReader</code> 의 Java 쪽: 읽기 상태를 가진 이벤트 대상.
     */
    static final class ReaderTarget extends EventTarget {
        /** The state. / 상태. */
        int state = EMPTY;

        /** The result or null. / 결과 또는 null. */
        JsValue result;

        /** The number of the current read; older tasks are stale. / 현재 읽기의 번호. 더 작은 번호의 작업은 오래된 것이다. */
        int epoch;

        /**
         * Creates a reader target.
         * <p>
         * 읽기 대상을 만든다.
         */
        ReaderTarget() {
            super();
        }
    }

    /** The page APIs. / 페이지 API. */
    private final WebApis api;

    /** The prototype. / 프로토타입. */
    private JsObject proto;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api the page APIs / 페이지 API
     */
    FileReaderApi(WebApis api) {
        this.api = api;
    }

    /**
     * Installs <code>FileReader</code>.
     * <p>
     * <code>FileReader</code> 를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final DomBridge bridge = api.bridge();
        final JsRealm realm = api.realm();
        bridge.addClassifier(new DomBridge.Classifier() {
            public String classify(Object target) {
                return target instanceof ReaderTarget ? "FileReader" : null;
            }
        });
        DomBridge.InterfaceInfo info = bridge.defineInterface("FileReader", "EventTarget", ReaderTarget.class, new String[0]);
        proto = info.getPrototype();
        bridge.getEventHandlers().define(proto, ReaderTarget.class, EVENTS);
        NativeFunction ctor = new NativeFunction(realm, realm.getFunctionPrototype(), "FileReader", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'FileReader': Please use the 'new' operator, this DOM object constructor "
                        + "cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                HostObject w = bridge.wrap(new ReaderTarget());
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, proto));
                return w;
            }
        });
        bridge.setConstructor("FileReader", ctor);
        String[] states = { "EMPTY", "LOADING", "DONE" };
        // 상태 상수를 인터페이스 객체와 프로토타입에 정의한다.
        for (int i = 0; i < states.length; i++) {
            ctor.defineData(states[i], Js.num(i), false, true, false);
            proto.defineData(states[i], Js.num(i), false, true, false);
        }
        Natives.getter(realm, proto, "readyState", this, "readyState");
        Natives.getter(realm, proto, "result", this, "result");
        Natives.getter(realm, proto, "error", this, "error");
        Natives.method(realm, proto, "readAsArrayBuffer", 1, this, "readAsArrayBuffer");
        Natives.method(realm, proto, "readAsBinaryString", 1, this, "readAsBinaryString");
        Natives.method(realm, proto, "readAsText", 1, this, "readAsText");
        Natives.method(realm, proto, "readAsDataURL", 1, this, "readAsDataUrl");
        Natives.method(realm, proto, "abort", 0, this, "abort");
    }

    /**
     * Returns the reader that a value stands for or throws the standard TypeError.
     * <p>
     * 값이 나타내는 읽기 객체를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the reader / 읽기 객체
     */
    private static ReaderTarget reader(JsValue self) {
        Object t = DomBridge.targetOf(self, ReaderTarget.class);
        if (t == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return (ReaderTarget) t;
    }

    /**
     * Returns <code>readyState</code>.
     * <p>
     * <code>readyState</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the state / 상태
     */
    JsValue readyState(JsValue self, JsValue[] args) {
        return Js.num(reader(self).state);
    }

    /**
     * Returns <code>result</code>.
     * <p>
     * <code>result</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the result or null / 결과 또는 null
     */
    JsValue result(JsValue self, JsValue[] args) {
        ReaderTarget r = reader(self);
        return r.result == null ? Js.NULL : r.result;
    }

    /**
     * Returns <code>error</code>: always null, because reading a blob in memory cannot fail.
     * <p>
     * <code>error</code> 를 반환한다: 메모리의 블롭을 읽는 일은 실패할 수 없으므로 항상 null.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return null / null
     */
    JsValue error(JsValue self, JsValue[] args) {
        reader(self);
        return Js.NULL;
    }

    /**
     * Implements <code>readAsArrayBuffer(blob)</code>.
     * <p>
     * <code>readAsArrayBuffer(blob)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue readAsArrayBuffer(JsValue self, JsValue[] args) {
        return start(self, args, 0, "readAsArrayBuffer");
    }

    /**
     * Implements <code>readAsBinaryString(blob)</code>.
     * <p>
     * <code>readAsBinaryString(blob)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue readAsBinaryString(JsValue self, JsValue[] args) {
        return start(self, args, 1, "readAsBinaryString");
    }

    /**
     * Implements <code>readAsText(blob, encoding)</code>.
     * <p>
     * <code>readAsText(blob, encoding)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue readAsText(JsValue self, JsValue[] args) {
        return start(self, args, 2, "readAsText");
    }

    /**
     * Implements <code>readAsDataURL(blob)</code>.
     * <p>
     * <code>readAsDataURL(blob)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue readAsDataUrl(JsValue self, JsValue[] args) {
        return start(self, args, 3, "readAsDataURL");
    }

    /**
     * The "read operation": checks the state and queues the two tasks of the read.
     * <p>
     * "읽기 연산": 상태를 검사하고 읽기의 두 작업을 줄 세운다.
     *
     * @param self   this value / this 값
     * @param args   arguments / 인수
     * @param kind   0 buffer, 1 binary string, 2 text, 3 data URL / 0 버퍼, 1 바이너리 문자열, 2 텍스트, 3 데이터 URL
     * @param method the method name for messages / 메시지용 메서드 이름
     * @return undefined / undefined
     */
    private JsValue start(JsValue self, JsValue[] args, final int kind, String method) {
        final ReaderTarget t = reader(self);
        Natives.requireArgs(args, 1, method, "FileReader");
        if (!(args[0] instanceof BlobApi.BlobObject)) {
            throw JsException.typeError("Failed to execute '" + method + "' on 'FileReader': parameter 1 is not of type 'Blob'.");
        }
        if (t.state == LOADING) {
            throw api.domError("InvalidStateError", "Failed to execute '" + method + "' on 'FileReader': The object is already busy reading Blobs.");
        }
        final BlobApi.BlobObject blob = (BlobApi.BlobObject) args[0];
        final String label = kind == 2 && args.length > 1 && !args[1].isUndefined() ? Ops.toString(args[1]) : null;
        t.state = LOADING;
        t.result = null;
        final int mine = ++t.epoch;
        api.post(new Runnable() {
            public void run() {
                if (t.epoch != mine) {
                    return;
                }
                fire(t, "loadstart", 0, blob.data.length);
                api.post(new Runnable() {
                    public void run() {
                        if (t.epoch != mine) {
                            return;
                        }
                        t.result = convert(blob, kind, label);
                        t.state = DONE;
                        fire(t, "progress", blob.data.length, blob.data.length);
                        if (t.epoch == mine) {
                            fire(t, "load", blob.data.length, blob.data.length);
                        }
                        if (t.epoch == mine) {
                            fire(t, "loadend", blob.data.length, blob.data.length);
                        }
                    }
                });
            }
        });
        return Js.UNDEFINED;
    }

    /**
     * Fires a progress event at a reader.
     * <p>
     * 읽기 객체에 진행 이벤트를 발생시킨다.
     *
     * @param t      the reader / 읽기 객체
     * @param type   the event type / 이벤트 종류
     * @param loaded the bytes read / 읽은 바이트
     * @param total  the size of the blob / 블롭 크기
     */
    private void fire(ReaderTarget t, String type, long loaded, long total) {
        t.dispatchTrustedEvent(new ProgressEvent(type, false, false, true, loaded, total));
    }

    /**
     * Converts the bytes of a blob to the result of a read.
     * <p>
     * 블롭의 바이트를 읽기의 결과로 바꾼다.
     *
     * @param blob  the blob / 블롭
     * @param kind  the kind of read / 읽기 종류
     * @param label the encoding label of <code>readAsText</code> or null / <code>readAsText</code> 의 인코딩 레이블 또는 null
     * @return the result / 결과
     */
    private JsValue convert(BlobApi.BlobObject blob, int kind, String label) {
        byte[] d = blob.data;
        if (kind == 0) {
            return TypedArrays.newArrayBufferCopy(api.realm(), d, 0, d.length);
        }
        if (kind == 1) {
            char[] c = new char[d.length];
            // 바이트 하나가 문자 하나(U+0000..U+00FF)이다.
            for (int i = 0; i < c.length; i++) {
                c[i] = (char) (d[i] & 255);
            }
            return Js.str(new String(c));
        }
        if (kind == 2) {
            String canonical = label == null ? null : EncodingLabels.canonicalName(label);
            // 바이트 순서 표시가 있으면 그것이 우선하고, 레이블이 없거나 모르면 UTF-8 이다.
            if (hasUtfBom(d) || canonical == null || EncodingLabels.UTF_8.equals(canonical)) {
                return Js.str(Encodings.decode(d, null));
            }
            return Js.str(EncodingLabels.decode(d, 0, canonical));
        }
        String type = blob.type.length() == 0 ? "application/octet-stream" : blob.type;
        return Js.str("data:" + type + ";base64," + Encodings.base64Encode(d));
    }

    /**
     * Tells whether bytes start with a UTF-8 or UTF-16 byte order mark.
     * <p>
     * 바이트가 UTF-8 이나 UTF-16 바이트 순서 표시로 시작하는지 알려 준다.
     *
     * @param d the bytes / 바이트
     * @return true if a BOM is present / BOM 이 있으면 true
     */
    private static boolean hasUtfBom(byte[] d) {
        return (d.length >= 3 && (d[0] & 255) == 0xEF && (d[1] & 255) == 0xBB && (d[2] & 255) == 0xBF)
                || (d.length >= 2 && (((d[0] & 255) == 0xFE && (d[1] & 255) == 0xFF) || ((d[0] & 255) == 0xFF && (d[1] & 255) == 0xFE)));
    }

    /**
     * Implements <code>abort()</code>.
     * <p>
     * <code>abort()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue abort(JsValue self, JsValue[] args) {
        ReaderTarget t = reader(self);
        if (t.state == EMPTY || t.state == DONE) {
            t.result = null;
            return Js.UNDEFINED;
        }
        t.state = DONE;
        t.result = null;
        t.epoch++;
        fire(t, "abort", 0, 0);
        fire(t, "loadend", 0, 0);
        return Js.UNDEFINED;
    }
}
