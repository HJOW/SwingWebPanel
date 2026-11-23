package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.io.UnsupportedEncodingException;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;
import org.duckdns.hjow.swingwebpanel.net.ContentType;

/**
 * The body support that <code>Request</code> and <code>Response</code> share (Fetch Standard, sections 5.2 and 5.3): extracting the bytes and the
 * content type from a <code>BodyInit</code> value (string, <code>Blob</code>, buffer, <code>FormData</code>, <code>URLSearchParams</code>), and the
 * <em>Body mixin</em> (<code>bodyUsed</code>, <code>text()</code>, <code>json()</code>, <code>arrayBuffer()</code>, <code>bytes()</code>, <code>blob()</code>,
 * <code>formData()</code>) that reads the body once, as a promise. The whole body is in memory, so there are no streams: <code>body</code> is
 * always <code>null</code> and there is no <code>ReadableStream</code>.
 * <p>
 * <code>Request</code> 와 <code>Response</code> 가 공유하는 본문 지원(Fetch 표준 5.2절, 5.3절): <code>BodyInit</code> 값(문자열, <code>Blob</code>, 버퍼,
 * <code>FormData</code>, <code>URLSearchParams</code>)에서 바이트와 콘텐츠 타입을 뽑는 일, 본문을 프라미스로 한 번 읽는 <em>Body mixin</em>
 * (<code>bodyUsed</code>, <code>text()</code>, <code>json()</code>, <code>arrayBuffer()</code>, <code>bytes()</code>, <code>blob()</code>, <code>formData()</code>).
 * 본문 전체가 메모리에 있으므로 스트림은 없다: <code>body</code> 는 항상 <code>null</code> 이며 <code>ReadableStream</code> 은 없다.
 */
final class BodyApi {

    /**
     * Bytes and content type of an extracted body.
     * <p>
     * 뽑아낸 본문의 바이트와 콘텐츠 타입.
     */
    static final class BodyData {
        /** The bytes. / 바이트. */
        final byte[] bytes;

        /** The content type or null. / 콘텐츠 타입 또는 null. */
        final String type;

        /**
         * Creates the pair.
         * <p>
         * 쌍을 만든다.
         *
         * @param bytes the bytes / 바이트
         * @param type  the type or null / 타입 또는 null
         */
        BodyData(byte[] bytes, String type) {
            this.bytes = bytes;
            this.type = type;
        }
    }

    /**
     * The common base of <code>Request</code> and <code>Response</code> objects: their headers and the body that can be read once.
     * <p>
     * <code>Request</code> 와 <code>Response</code> 객체의 공통 기반: 헤더와 한 번 읽을 수 있는 본문.
     */
    static class BodyObject extends JsObject {
        /** The body bytes, or null if there is no body. / 본문 바이트. 본문이 없으면 null. */
        byte[] body;

        /** True after the body was read. / 본문을 읽은 뒤에는 true. */
        boolean used;

        /** The headers. / 헤더. */
        HeadersApi.HeadersObject headers;

        /**
         * Creates the object.
         * <p>
         * 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         */
        BodyObject(JsObject proto) {
            super(proto);
        }

        /**
         * Tells whether the body cannot be read any more.
         * <p>
         * 본문을 더 읽을 수 없는지 알려 준다.
         *
         * @return true if a body exists and was used / 본문이 있고 이미 사용했으면 true
         */
        boolean unusable() {
            return body != null && used;
        }
    }

    /** The page APIs. / 페이지 API. */
    private final WebApis api;

    /**
     * Creates the helper.
     * <p>
     * 도우미를 만든다.
     *
     * @param api the page APIs / 페이지 API
     */
    BodyApi(WebApis api) {
        this.api = api;
    }

    /**
     * Extracts the bytes and the content type of a body value: a string (also any other value, converted to a string), a
     * <code>Blob</code>, an <code>ArrayBuffer</code>, a typed array, a <code>DataView</code>, a <code>FormData</code> or a <code>URLSearchParams</code>.
     * <p>
     * 본문 값에서 바이트와 콘텐츠 타입을 뽑는다: 문자열(다른 값도 문자열로 변환), <code>Blob</code>, <code>ArrayBuffer</code>, 형식화 배열,
     * <code>DataView</code>, <code>FormData</code>, <code>URLSearchParams</code>.
     *
     * @param v the value, neither null nor undefined / 값. null 과 undefined 가 아니다
     * @return the data / 데이터
     */
    BodyData extract(JsValue v) {
        if (v instanceof BlobApi.BlobObject) {
            BlobApi.BlobObject b = (BlobApi.BlobObject) v;
            return new BodyData(b.data, b.type.length() == 0 ? null : b.type);
        }
        TypedArrays.ByteView view = TypedArrays.bytesOf(v);
        if (view != null) {
            byte[] copy = new byte[view.length];
            System.arraycopy(view.array, view.offset, copy, 0, view.length);
            return new BodyData(copy, null);
        }
        if (v instanceof FormDataApi.FormDataObject) {
            String boundary = api.forms.newBoundary();
            return new BodyData(FormDataApi.multipart(((FormDataApi.FormDataObject) v).entries, boundary), "multipart/form-data; boundary=" + boundary);
        }
        if (v instanceof SearchParamsApi.ParamsObject) {
            return new BodyData(Encodings.utf8(Encodings.formSerialize(((SearchParamsApi.ParamsObject) v).list)),
                    "application/x-www-form-urlencoded;charset=UTF-8");
        }
        return new BodyData(Encodings.utf8(Natives.usv(v)), "text/plain;charset=UTF-8");
    }

    /**
     * Installs the body mixin members on a prototype.
     * <p>
     * 프로토타입에 body mixin 멤버를 설치한다.
     *
     * @param proto the prototype of <code>Request</code> or <code>Response</code> / <code>Request</code> 나 <code>Response</code> 의 프로토타입
     */
    void installMixin(JsObject proto) {
        Natives.getter(api.realm(), proto, "body", this, "bodyStream");
        Natives.getter(api.realm(), proto, "bodyUsed", this, "bodyUsed");
        Natives.method(api.realm(), proto, "arrayBuffer", 0, this, "arrayBuffer");
        Natives.method(api.realm(), proto, "blob", 0, this, "blob");
        Natives.method(api.realm(), proto, "bytes", 0, this, "bytes");
        Natives.method(api.realm(), proto, "formData", 0, this, "formData");
        Natives.method(api.realm(), proto, "json", 0, this, "json");
        Natives.method(api.realm(), proto, "text", 0, this, "text");
    }

    /**
     * Returns <code>body</code>: always null because the component has no streams.
     * <p>
     * <code>body</code> 를 반환한다: 컴포넌트에 스트림이 없으므로 항상 null.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return null / null
     */
    JsValue bodyStream(JsValue self, JsValue[] args) {
        Natives.self(self, BodyObject.class);
        return Js.NULL;
    }

    /**
     * Returns <code>bodyUsed</code>.
     * <p>
     * <code>bodyUsed</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return true if the body was read / 본문을 읽었으면 true
     */
    JsValue bodyUsed(JsValue self, JsValue[] args) {
        BodyObject o = Natives.self(self, BodyObject.class);
        return Js.bool(o.body != null && o.used);
    }

    /**
     * Implements <code>arrayBuffer()</code>.
     * <p>
     * <code>arrayBuffer()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return a promise / 프라미스
     */
    JsValue arrayBuffer(JsValue self, JsValue[] args) {
        return consume(self, 2);
    }

    /**
     * Implements <code>blob()</code>.
     * <p>
     * <code>blob()</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return a promise / 프라미스
     */
    JsValue blob(JsValue self, JsValue[] args) {
        return consume(self, 3);
    }

    /**
     * Implements <code>bytes()</code>.
     * <p>
     * <code>bytes()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return a promise / 프라미스
     */
    JsValue bytes(JsValue self, JsValue[] args) {
        return consume(self, 4);
    }

    /**
     * Implements <code>formData()</code>.
     * <p>
     * <code>formData()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return a promise / 프라미스
     */
    JsValue formData(JsValue self, JsValue[] args) {
        return consume(self, 5);
    }

    /**
     * Implements <code>json()</code>.
     * <p>
     * <code>json()</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return a promise / 프라미스
     */
    JsValue json(JsValue self, JsValue[] args) {
        return consume(self, 1);
    }

    /**
     * Implements <code>text()</code>.
     * <p>
     * <code>text()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return a promise / 프라미스
     */
    JsValue text(JsValue self, JsValue[] args) {
        return consume(self, 0);
    }

    /**
     * The "consume body" algorithm: reads the body once and converts it.
     * <p>
     * "consume body" 알고리즘: 본문을 한 번 읽어 변환한다.
     *
     * @param self this value / this 값
     * @param kind 0 text, 1 json, 2 arrayBuffer, 3 blob, 4 bytes, 5 formData / 0 text, 1 json, 2 arrayBuffer, 3 blob, 4 bytes, 5 formData
     * @return the promise / 프라미스
     */
    JsValue consume(JsValue self, int kind) {
        if (!(self instanceof BodyObject)) {
            return Promises.rejectedTypeError(api.realm(), "Illegal invocation");
        }
        BodyObject o = (BodyObject) self;
        if (o.unusable()) {
            return Promises.rejectedTypeError(api.realm(), "Failed to execute body method: body stream already read");
        }
        if (o.body != null) {
            o.used = true;
        }
        byte[] bytes = o.body == null ? new byte[0] : o.body;
        try {
            return Promises.resolved(api.realm(), convert(o, bytes, kind));
        } catch (JsException e) {
            return Promises.rejected(api.realm(), e.getValue(api.realm()));
        }
    }

    /**
     * Converts body bytes to the requested form.
     * <p>
     * 본문 바이트를 요청한 형태로 바꾼다.
     *
     * @param o     the object (for its content type) / 객체(콘텐츠 타입용)
     * @param bytes the bytes / 바이트
     * @param kind  the kind of {@link #consume} / {@link #consume} 의 종류
     * @return the script value / 스크립트 값
     */
    private JsValue convert(BodyObject o, byte[] bytes, int kind) {
        switch (kind) {
        case 0:
            return Js.str(Encodings.utf8Decode(bytes));
        case 1:
            return parseJson(Js.str(Encodings.utf8Decode(bytes)));
        case 2:
            return TypedArrays.newArrayBufferCopy(api.realm(), bytes, 0, bytes.length);
        case 3:
            return api.blobs.newBlob(bytes, mimeOf(o));
        case 4:
            return TypedArrays.newUint8Array(api.realm(), bytes.clone());
        default:
            return formDataOf(o, bytes);
        }
    }

    /**
     * Parses JSON text with the realm's own <code>JSON.parse</code>.
     * <p>
     * realm 자신의 <code>JSON.parse</code> 로 JSON 텍스트를 해석한다.
     *
     * @param text the text / 텍스트
     * @return the parsed value / 해석한 값
     */
    private JsValue parseJson(JsValue text) {
        return Ops.invoke(api.realm(), api.realm().getIntrinsic("%JSON%"), "parse", new JsValue[] { text });
    }

    /**
     * Returns the MIME type for <code>blob()</code>: the Content-Type of the headers in the serialized form, or "".
     * <p>
     * <code>blob()</code> 의 MIME 타입을 반환한다: 헤더의 Content-Type 을 직렬화한 형태 또는 "".
     *
     * @param o the object / 객체
     * @return the type / 타입
     */
    static String mimeOf(BodyObject o) {
        String raw = o.headers == null ? null : o.headers.get("content-type");
        if (raw == null) {
            return "";
        }
        ContentType ct = ContentType.parse(raw);
        if (ct.getMime() == null || ct.getMime().length() == 0) {
            return "";
        }
        return ct.getCharset() == null ? ct.getMime() : ct.getMime() + ";charset=" + ct.getCharset().toLowerCase();
    }

    /**
     * Parses a body as form data according to its content type.
     * <p>
     * 본문을 콘텐츠 타입에 따라 폼 데이터로 해석한다.
     *
     * @param o     the object / 객체
     * @param bytes the bytes / 바이트
     * @return a <code>FormData</code> object / <code>FormData</code> 객체
     */
    private JsValue formDataOf(BodyObject o, byte[] bytes) {
        String raw = o.headers == null ? null : o.headers.get("content-type");
        ContentType ct = ContentType.parse(raw == null ? "" : raw);
        FormDataApi.FormDataObject f = api.forms.create();
        if ("multipart/form-data".equals(ct.getMime())) {
            String boundary = boundaryOf(raw);
            if (boundary == null || !parseMultipart(bytes, boundary, f.entries)) {
                throw JsException.typeError("Failed to fetch: could not parse the multipart body");
            }
        } else if ("application/x-www-form-urlencoded".equals(ct.getMime())) {
            List<String[]> pairs = Encodings.formParse(Encodings.utf8Decode(bytes));
            // 해석한 쌍을 문자열 항목으로 옮긴다.
            for (int i = 0; i < pairs.size(); i++) {
                f.entries.add(new Object[] { pairs.get(i)[0], pairs.get(i)[1] });
            }
        } else {
            throw JsException.typeError("Failed to fetch: the content type is neither multipart/form-data nor "
                    + "application/x-www-form-urlencoded");
        }
        return f;
    }

    /**
     * Extracts the <code>boundary</code> parameter of a multipart content type.
     * <p>
     * multipart 콘텐츠 타입의 <code>boundary</code> 매개변수를 뽑는다.
     *
     * @param contentType the header value / 헤더 값
     * @return the boundary or null / 경계 또는 null
     */
    private static String boundaryOf(String contentType) {
        String lower = contentType.toLowerCase();
        int at = lower.indexOf("boundary=");
        if (at < 0) {
            return null;
        }
        String rest = contentType.substring(at + "boundary=".length()).trim();
        if (rest.startsWith("\"")) {
            int end = rest.indexOf('"', 1);
            return end < 0 ? null : rest.substring(1, end);
        }
        int semi = rest.indexOf(';');
        String b = semi < 0 ? rest : rest.substring(0, semi);
        return b.trim().length() == 0 ? null : b.trim();
    }

    /**
     * Parses a <code>multipart/form-data</code> body into entries.
     * <p>
     * <code>multipart/form-data</code> 본문을 항목으로 해석한다.
     *
     * @param bytes    the body / 본문
     * @param boundary the boundary / 경계
     * @param out      receives the entries / 항목을 받는다
     * @return false if the body is malformed / 본문이 올바르지 않으면 false
     */
    private boolean parseMultipart(byte[] bytes, String boundary, List<Object[]> out) {
        String all;
        try {
            all = new String(bytes, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return false;
        }
        String delimiter = "--" + boundary;
        int pos = all.indexOf(delimiter);
        if (pos < 0) {
            return false;
        }
        // 구분자마다 그 뒤의 부분을 읽는다. 마지막 구분자("--")를 만나면 정상 종료이다.
        while (true) {
            pos += delimiter.length();
            if (all.startsWith("--", pos)) {
                return true;
            }
            if (!all.startsWith("\r\n", pos)) {
                return false;
            }
            pos += 2;
            int headerEnd = all.indexOf("\r\n\r\n", pos);
            if (headerEnd < 0) {
                return false;
            }
            String headerText = all.substring(pos, headerEnd);
            int contentStart = headerEnd + 4;
            int next = all.indexOf("\r\n" + delimiter, contentStart);
            if (next < 0) {
                return false;
            }
            addPart(headerText, bytes, contentStart, next, out);
            pos = next + 2;
        }
    }

    /**
     * Adds one multipart part as an entry.
     * <p>
     * multipart 부분 하나를 항목으로 추가한다.
     *
     * @param headerText the part headers / 부분의 헤더
     * @param bytes      the whole body / 전체 본문
     * @param from       the first content byte / 내용의 첫 바이트
     * @param to         the end of the content / 내용의 끝
     * @param out        receives the entry / 항목을 받는다
     */
    private void addPart(String headerText, byte[] bytes, int from, int to, List<Object[]> out) {
        String name = null;
        String filename = null;
        String type = null;
        String[] lines = headerText.split("\r\n");
        // 부분의 헤더 줄에서 Content-Disposition 의 name, filename 과 Content-Type 을 찾는다.
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String h = line.substring(0, colon).trim().toLowerCase();
            String v = line.substring(colon + 1).trim();
            if ("content-disposition".equals(h)) {
                name = param(v, "name");
                filename = param(v, "filename");
            } else if ("content-type".equals(h)) {
                type = v;
            }
        }
        if (name == null) {
            return;
        }
        byte[] content = new byte[to - from];
        System.arraycopy(bytes, from, content, 0, content.length);
        String decodedName = Encodings.utf8Raw(latin1Bytes(name));
        if (filename != null) {
            out.add(new Object[] { decodedName, api.blobs.newFile(content, type == null ? "text/plain" : type,
                    Encodings.utf8Raw(latin1Bytes(filename)), System.currentTimeMillis()) });
        } else {
            out.add(new Object[] { decodedName, Encodings.utf8Raw(content) });
        }
    }

    /**
     * Converts a string of Latin-1 characters back to the bytes that it stands for.
     * <p>
     * Latin-1 문자로 된 문자열을 그것이 나타내는 바이트로 되돌린다.
     *
     * @param s the string / 문자열
     * @return the bytes / 바이트
     */
    private static byte[] latin1Bytes(String s) {
        byte[] b = new byte[s.length()];
        // 문자 하나가 바이트 하나이다.
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) s.charAt(i);
        }
        return b;
    }

    /**
     * Reads a quoted parameter of a <code>Content-Disposition</code> value.
     * <p>
     * <code>Content-Disposition</code> 값의 따옴표 매개변수를 읽는다.
     *
     * @param value the header value / 헤더 값
     * @param key   the parameter name / 매개변수 이름
     * @return the unescaped value or null / 이스케이프를 푼 값 또는 null
     */
    private static String param(String value, String key) {
        String needle = key + "=\"";
        int at = -1;
        int from = 0;
        // 앞에 ';' 나 공백이 붙은 이름을 찾아 filename 안의 name 같은 오탐을 피한다.
        while (true) {
            int i = value.toLowerCase().indexOf(needle, from);
            if (i < 0) {
                break;
            }
            if (i == 0 || value.charAt(i - 1) == ' ' || value.charAt(i - 1) == ';') {
                at = i;
                break;
            }
            from = i + 1;
        }
        if (at < 0) {
            return null;
        }
        int start = at + needle.length();
        int end = value.indexOf('"', start);
        if (end < 0) {
            return null;
        }
        return value.substring(start, end).replace("%22", "\"").replace("%0D", "\r").replace("%0A", "\n");
    }
}
