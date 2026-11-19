package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;

/**
 * Installs <code>Blob</code> and <code>File</code> (File API): an immutable block of bytes with a MIME type. A blob is created from a list of
 * parts (strings, other blobs, <code>ArrayBuffer</code>s, typed arrays, <code>DataView</code>s), can be sliced, and is read as text, as an
 * <code>ArrayBuffer</code> or as a <code>Uint8Array</code> through promises. Blobs hold their bytes in memory; there are no streams, no
 * <code>FileReader</code> and no <code>blob:</code> URLs (the component does not load that scheme). A <code>File</code> adds a name and a
 * modification time; the Java side never lets a page create a <code>File</code> for a path of the local disk.
 * <p>
 * <code>Blob</code> 과 <code>File</code> 을 설치한다(File API). MIME 타입을 가진 바뀌지 않는 바이트 덩어리이다. 블롭은 부분 목록(문자열, 다른 블롭,
 * <code>ArrayBuffer</code>, 형식화 배열, <code>DataView</code>)으로 만들고 자를 수 있으며 프라미스로 텍스트, <code>ArrayBuffer</code>, <code>Uint8Array</code>
 * 로 읽는다. 블롭은 바이트를 메모리에 둔다. 스트림, <code>FileReader</code>, <code>blob:</code> URL(컴포넌트가 그 스킴을 읽지 않는다)은 없다.
 * <code>File</code> 은 이름과 수정 시간을 더하며 Java 쪽은 페이지가 로컬 디스크 경로의 <code>File</code> 을 만들게 하지 않는다.
 */
final class BlobApi {

    /**
     * The script object of a <code>Blob</code>.
     * <p>
     * <code>Blob</code> 의 스크립트 객체.
     */
    static class BlobObject extends JsObject {
        /** The bytes (shared, never changed). / 바이트(공유하며 바꾸지 않음). */
        final byte[] data;

        /** The MIME type in lower case, or "". / 소문자 MIME 타입 또는 "". */
        final String type;

        /**
         * Creates a blob object.
         * <p>
         * 블롭 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         * @param data  the bytes / 바이트
         * @param type  the type / 타입
         */
        BlobObject(JsObject proto, byte[] data, String type) {
            super(proto);
            this.data = data;
            this.type = type;
        }
    }

    /**
     * The script object of a <code>File</code>.
     * <p>
     * <code>File</code> 의 스크립트 객체.
     */
    static final class FileObject extends BlobObject {
        /** The file name. / 파일 이름. */
        final String name;

        /** The modification time in milliseconds since the epoch. / 수정 시간(에포크 이후 밀리초). */
        final double lastModified;

        /**
         * Creates a file object.
         * <p>
         * 파일 객체를 만든다.
         *
         * @param proto        the prototype / 프로토타입
         * @param data         the bytes / 바이트
         * @param type         the type / 타입
         * @param name         the name / 이름
         * @param lastModified the modification time / 수정 시간
         */
        FileObject(JsObject proto, byte[] data, String type, String name, double lastModified) {
            super(proto, data, type);
            this.name = name;
            this.lastModified = lastModified;
        }
    }

    /** The page APIs. / 페이지 API. */
    private final WebApis api;

    /** The prototype of blobs. / 블롭의 프로토타입. */
    private JsObject blobProto;

    /** The prototype of files. / 파일의 프로토타입. */
    private JsObject fileProto;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api the page APIs / 페이지 API
     */
    BlobApi(WebApis api) {
        this.api = api;
    }

    /**
     * Installs <code>Blob</code> and <code>File</code>.
     * <p>
     * <code>Blob</code> 과 <code>File</code> 을 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final JsRealm realm = api.realm();
        Natives.ClassDef b = Natives.defineClass(realm, g, "Blob", 0, null, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return constructBlob(r, args, newTarget);
            }
        });
        blobProto = b.prototype;
        Natives.getter(realm, b.prototype, "size", this, "blobSize");
        Natives.getter(realm, b.prototype, "type", this, "blobType");
        Natives.method(realm, b.prototype, "slice", 0, this, "blobSlice");
        Natives.method(realm, b.prototype, "text", 0, this, "blobText");
        Natives.method(realm, b.prototype, "arrayBuffer", 0, this, "blobArrayBuffer");
        Natives.method(realm, b.prototype, "bytes", 0, this, "blobBytes");
        Natives.ClassDef f = Natives.defineClass(realm, g, "File", 2, b.prototype, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return constructFile(r, args, newTarget);
            }
        });
        f.constructor.setPrototypeOf(b.constructor);
        fileProto = f.prototype;
        Natives.getter(realm, f.prototype, "name", this, "fileName");
        Natives.getter(realm, f.prototype, "lastModified", this, "fileLastModified");
    }

    /**
     * Normalizes a Blob type: a type with a character outside U+0020..U+007E becomes "", the rest is lower-cased.
     * <p>
     * 블롭 타입을 정규화한다. U+0020..U+007E 밖의 문자가 있으면 ""이 되고 나머지는 소문자로 바꾼다.
     *
     * @param t the type text / 타입 문자열
     * @return the normalized type / 정규화한 타입
     */
    static String normalizeType(String t) {
        // 모든 문자가 출력 가능한 ASCII 이어야 하며 하나라도 벗어나면 빈 타입이다.
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c < 0x20 || c > 0x7E) {
                return "";
            }
        }
        return t.toLowerCase();
    }

    /**
     * Creates a blob object.
     * <p>
     * 블롭 객체를 만든다.
     *
     * @param data the bytes (kept, not copied) / 바이트(복사하지 않고 보관)
     * @param type the type (normalized here) / 타입(여기서 정규화)
     * @return the blob / 블롭
     */
    BlobObject newBlob(byte[] data, String type) {
        return new BlobObject(blobProto, data, normalizeType(type == null ? "" : type));
    }

    /**
     * Creates a file object.
     * <p>
     * 파일 객체를 만든다.
     *
     * @param data         the bytes / 바이트
     * @param type         the type / 타입
     * @param name         the name / 이름
     * @param lastModified the modification time / 수정 시간
     * @return the file / 파일
     */
    FileObject newFile(byte[] data, String type, String name, double lastModified) {
        return new FileObject(fileProto, data, normalizeType(type == null ? "" : type), name, lastModified);
    }

    /**
     * Converts the <code>blobParts</code> argument to bytes.
     * <p>
     * <code>blobParts</code> 인수를 바이트로 바꾼다.
     *
     * @param parts   the sequence value, or undefined / 시퀀스 값 또는 undefined
     * @param endings true if line endings are converted to the platform's ("native") / 줄 끝을 플랫폼 것으로 바꾸면("native") true
     * @param realm   the realm / realm
     * @return the bytes / 바이트
     */
    static byte[] partsToBytes(JsRealm realm, JsValue parts, boolean endings) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (parts.isUndefined()) {
            return out.toByteArray();
        }
        if (!parts.isObject()) {
            throw JsException.typeError("The provided value cannot be converted to a sequence.");
        }
        List<JsValue> items = IteratorHelper.toList(realm, parts);
        // 각 부분을 바이트로 바꿔 순서대로 이어 붙인다.
        for (int i = 0; i < items.size(); i++) {
            JsValue p = items.get(i);
            if (p instanceof BlobObject) {
                byte[] d = ((BlobObject) p).data;
                out.write(d, 0, d.length);
                continue;
            }
            TypedArrays.ByteView view = TypedArrays.bytesOf(p);
            if (view != null) {
                out.write(view.array, view.offset, view.length);
                continue;
            }
            String s = Natives.usv(Ops.toString(p));
            if (endings) {
                s = nativeEndings(s);
            }
            byte[] d = Encodings.utf8(s);
            out.write(d, 0, d.length);
        }
        return out.toByteArray();
    }

    /**
     * Converts every line ending of a string to the platform's line separator ("native" endings).
     * <p>
     * 문자열의 모든 줄 끝을 플랫폼의 줄 구분자로 바꾼다("native" 줄 끝).
     *
     * @param s the text / 텍스트
     * @return the converted text / 변환한 텍스트
     */
    private static String nativeEndings(String s) {
        String sep = System.getProperty("line.separator", "\n");
        StringBuilder out = new StringBuilder(s.length() + 8);
        // CR LF, 단독 CR, 단독 LF 를 모두 같은 구분자로 바꾼다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                }
                out.append(sep);
            } else if (c == '\n') {
                out.append(sep);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Implements <code>new Blob(parts, options)</code>.
     * <p>
     * <code>new Blob(parts, options)</code> 를 구현한다.
     *
     * @param r         the realm / realm
     * @param args      the arguments / 인수
     * @param newTarget the new target / new 대상
     * @return the blob / 블롭
     */
    private JsObject constructBlob(JsRealm r, JsValue[] args, JsObject newTarget) {
        JsValue opts = Js.arg(args, 1);
        boolean nativeEndings = false;
        String type = "";
        if (!opts.isNullOrUndefined()) {
            if (!opts.isObject()) {
                throw JsException.typeError("Failed to construct 'Blob': The provided value is not of type 'BlobPropertyBag'.");
            }
            JsValue e = ((JsObject) opts).get("endings");
            if (!e.isUndefined()) {
                String es = Ops.toString(e);
                if ("native".equals(es)) {
                    nativeEndings = true;
                } else if (!"transparent".equals(es)) {
                    throw JsException.typeError("Failed to construct 'Blob': The provided value '" + es
                            + "' is not a valid enum value of type EndingType.");
                }
            }
            JsValue t = ((JsObject) opts).get("type");
            if (!t.isUndefined()) {
                type = normalizeType(Natives.usv(t));
            }
        }
        byte[] data = partsToBytes(r, Js.arg(args, 0), nativeEndings);
        return new BlobObject(Ops.getPrototypeFromConstructor(r, newTarget, blobProto), data, type);
    }

    /**
     * Implements <code>new File(bits, name, options)</code>.
     * <p>
     * <code>new File(bits, name, options)</code> 를 구현한다.
     *
     * @param r         the realm / realm
     * @param args      the arguments / 인수
     * @param newTarget the new target / new 대상
     * @return the file / 파일
     */
    private JsObject constructFile(JsRealm r, JsValue[] args, JsObject newTarget) {
        Natives.requireArgs(args, 2, "constructor", "File");
        JsValue opts = Js.arg(args, 2);
        String type = "";
        double modified = System.currentTimeMillis();
        boolean nativeEndings = false;
        if (!opts.isNullOrUndefined()) {
            if (!opts.isObject()) {
                throw JsException.typeError("Failed to construct 'File': The provided value is not of type 'FilePropertyBag'.");
            }
            JsValue t = ((JsObject) opts).get("type");
            if (!t.isUndefined()) {
                type = normalizeType(Natives.usv(t));
            }
            JsValue m = ((JsObject) opts).get("lastModified");
            if (!m.isUndefined()) {
                modified = Ops.toInteger(m);
            }
            JsValue e = ((JsObject) opts).get("endings");
            nativeEndings = !e.isUndefined() && "native".equals(Ops.toString(e));
        }
        byte[] data = partsToBytes(r, args[0], nativeEndings);
        // File 이름의 '/' 는 ':' 로 바꾸는 것이 표준이다.
        String name = Natives.usv(args[1]).replace('/', ':');
        return new FileObject(Ops.getPrototypeFromConstructor(r, newTarget, fileProto), data, type, name, modified);
    }

    /**
     * Returns <code>blob.size</code>.
     * <p>
     * <code>blob.size</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the size / 크기
     */
    JsValue blobSize(JsValue self, JsValue[] args) {
        return Js.num(Natives.self(self, BlobObject.class).data.length);
    }

    /**
     * Returns <code>blob.type</code>.
     * <p>
     * <code>blob.type</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the type / 타입
     */
    JsValue blobType(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, BlobObject.class).type);
    }

    /**
     * Implements <code>blob.slice(start, end, contentType)</code>.
     * <p>
     * <code>blob.slice(start, end, contentType)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the new blob / 새 블롭
     */
    JsValue blobSlice(JsValue self, JsValue[] args) {
        BlobObject b = Natives.self(self, BlobObject.class);
        int size = b.data.length;
        int start = relative(Js.arg(args, 0), 0, size);
        int end = relative(Js.arg(args, 1), size, size);
        int len = Math.max(end - start, 0);
        byte[] part = new byte[len];
        System.arraycopy(b.data, start, part, 0, len);
        JsValue t = Js.arg(args, 2);
        return new BlobObject(blobProto, part, t.isUndefined() ? "" : normalizeType(Natives.usv(t)));
    }

    /**
     * Converts a relative slice position to an absolute one.
     * <p>
     * 상대 자르기 위치를 절대 위치로 바꾼다.
     *
     * @param v    the argument / 인수
     * @param dflt the value for undefined / undefined 일 때 값
     * @param size the blob size / 블롭 크기
     * @return a position in 0..size / 0..size 의 위치
     */
    private static int relative(JsValue v, int dflt, int size) {
        if (v.isUndefined()) {
            return dflt;
        }
        double d = Ops.toInteger(v);
        if (d < 0) {
            return (int) Math.max(size + d, 0);
        }
        return (int) Math.min(d, size);
    }

    /**
     * Implements <code>blob.text()</code>.
     * <p>
     * <code>blob.text()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return a promise of the text / 텍스트의 프라미스
     */
    JsValue blobText(JsValue self, JsValue[] args) {
        if (!(self instanceof BlobObject)) {
            return Promises.rejectedTypeError(api.realm(), "Illegal invocation");
        }
        return Promises.resolved(api.realm(), Js.str(Encodings.utf8Decode(((BlobObject) self).data)));
    }

    /**
     * Implements <code>blob.arrayBuffer()</code>.
     * <p>
     * <code>blob.arrayBuffer()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return a promise of an ArrayBuffer / ArrayBuffer 의 프라미스
     */
    JsValue blobArrayBuffer(JsValue self, JsValue[] args) {
        if (!(self instanceof BlobObject)) {
            return Promises.rejectedTypeError(api.realm(), "Illegal invocation");
        }
        byte[] d = ((BlobObject) self).data;
        return Promises.resolved(api.realm(), TypedArrays.newArrayBufferCopy(api.realm(), d, 0, d.length));
    }

    /**
     * Implements <code>blob.bytes()</code>.
     * <p>
     * <code>blob.bytes()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return a promise of a Uint8Array / Uint8Array 의 프라미스
     */
    JsValue blobBytes(JsValue self, JsValue[] args) {
        if (!(self instanceof BlobObject)) {
            return Promises.rejectedTypeError(api.realm(), "Illegal invocation");
        }
        byte[] d = ((BlobObject) self).data.clone();
        return Promises.resolved(api.realm(), TypedArrays.newUint8Array(api.realm(), d));
    }

    /**
     * Returns <code>file.name</code>.
     * <p>
     * <code>file.name</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the name / 이름
     */
    JsValue fileName(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, FileObject.class).name);
    }

    /**
     * Returns <code>file.lastModified</code>.
     * <p>
     * <code>file.lastModified</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the time / 시간
     */
    JsValue fileLastModified(JsValue self, JsValue[] args) {
        return Js.num(Natives.self(self, FileObject.class).lastModified);
    }

}
