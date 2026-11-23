package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import org.duckdns.hjow.swingwebpanel.html.EncodingLabels;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;

/**
 * Installs <code>TextEncoder</code> and <code>TextDecoder</code> (Encoding Standard, section 8): the UTF-8 encoder with
 * <code>encode()</code> and <code>encodeInto()</code>, and the decoder for every encoding that the component knows by label (the labels of the
 * Encoding Standard, decoded with the JRE charsets and the component's own decoders). A decoder can be <code>fatal</code> (malformed data
 * throws a <code>TypeError</code>), can keep the byte order mark (<code>ignoreBOM</code>) and can decode a stream in pieces
 * (<code>decode(chunk, {stream: true})</code>); the bytes of an incomplete sequence wait for the next piece.
 * <p>
 * <code>TextEncoder</code> 와 <code>TextDecoder</code> 를 설치한다(Encoding 표준 8절): <code>encode()</code> 와 <code>encodeInto()</code> 를 가진 UTF-8 인코더,
 * 컴포넌트가 레이블로 아는 모든 인코딩(Encoding 표준의 레이블을 JRE charset 과 컴포넌트 자체 디코더로 해석)의 디코더. 디코더는 <code>fatal</code>(잘못된
 * 데이터가 <code>TypeError</code> 를 던짐)일 수 있고 바이트 순서 표시를 둘 수 있으며(<code>ignoreBOM</code>) 조각으로 나누어 스트림을 디코딩할 수 있다
 * (<code>decode(chunk, {stream: true})</code>). 불완전한 시퀀스의 바이트는 다음 조각을 기다린다.
 */
final class EncodingApi {

    /**
     * The script object of a <code>TextDecoder</code>.
     * <p>
     * <code>TextDecoder</code> 의 스크립트 객체.
     */
    static final class DecoderObject extends JsObject {
        /** The canonical encoding name (Encoding Standard). / 표준 인코딩 이름(Encoding 표준). */
        final String canonical;

        /** True if malformed data throws. / 잘못된 데이터가 예외를 던지면 true. */
        final boolean fatal;

        /** True if a byte order mark is kept. / 바이트 순서 표시를 두면 true. */
        final boolean ignoreBom;

        /** The streaming decoder for charsets of the JRE, or null for the encodings that this component decodes itself. / JRE charset 의 스트리밍 디코더. 컴포넌트가 직접 디코딩하는 인코딩이면 null. */
        final CharsetDecoder decoder;

        /** Bytes of an incomplete sequence from the previous piece. / 이전 조각의 불완전한 시퀀스의 바이트. */
        byte[] pending = new byte[0];

        /** True once the start of the stream was passed (the BOM is looked for only there). / 스트림 시작을 지났으면 true(BOM 은 거기서만 찾는다). */
        boolean started;

        /**
         * Creates a decoder object.
         * <p>
         * 디코더 객체를 만든다.
         *
         * @param proto     the prototype / 프로토타입
         * @param canonical the canonical name / 표준 이름
         * @param fatal     the fatal flag / fatal 표시
         * @param ignoreBom the ignoreBOM flag / ignoreBOM 표시
         * @param decoder   the charset decoder or null / charset 디코더 또는 null
         */
        DecoderObject(JsObject proto, String canonical, boolean fatal, boolean ignoreBom, CharsetDecoder decoder) {
            super(proto);
            this.canonical = canonical;
            this.fatal = fatal;
            this.ignoreBom = ignoreBom;
            this.decoder = decoder;
        }
    }

    /**
     * The script object of a <code>TextEncoder</code>.
     * <p>
     * <code>TextEncoder</code> 의 스크립트 객체.
     */
    private static final class EncoderObject extends JsObject {
        /**
         * Creates an encoder object.
         * <p>
         * 인코더 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         */
        EncoderObject(JsObject proto) {
            super(proto);
        }
    }

    /** The page APIs. / 페이지 API. */
    private final WebApis api;

    /** The prototype of encoders. / 인코더의 프로토타입. */
    private JsObject encoderProto;

    /** The prototype of decoders. / 디코더의 프로토타입. */
    private JsObject decoderProto;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api the page APIs / 페이지 API
     */
    EncodingApi(WebApis api) {
        this.api = api;
    }

    /**
     * Installs both interfaces.
     * <p>
     * 두 인터페이스를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        JsRealm realm = api.realm();
        Natives.ClassDef e = Natives.defineClass(realm, g, "TextEncoder", 0, null, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return new EncoderObject(Ops.getPrototypeFromConstructor(r, newTarget, encoderProto));
            }
        });
        encoderProto = e.prototype;
        Natives.getter(realm, encoderProto, "encoding", this, "encoderEncoding");
        Natives.method(realm, encoderProto, "encode", 0, this, "encode");
        Natives.method(realm, encoderProto, "encodeInto", 2, this, "encodeInto");
        Natives.ClassDef d = Natives.defineClass(realm, g, "TextDecoder", 0, null, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return newDecoder(Js.arg(args, 0), Js.arg(args, 1), Ops.getPrototypeFromConstructor(r, newTarget, decoderProto));
            }
        });
        decoderProto = d.prototype;
        Natives.getter(realm, decoderProto, "encoding", this, "decoderEncoding");
        Natives.getter(realm, decoderProto, "fatal", this, "decoderFatal");
        Natives.getter(realm, decoderProto, "ignoreBOM", this, "decoderIgnoreBom");
        Natives.method(realm, decoderProto, "decode", 0, this, "decode");
    }

    /**
     * Implements <code>new TextDecoder(label, options)</code>.
     * <p>
     * <code>new TextDecoder(label, options)</code> 를 구현한다.
     *
     * @param label   the label argument / label 인수
     * @param options the options argument / options 인수
     * @param proto   the prototype / 프로토타입
     * @return the decoder / 디코더
     */
    private DecoderObject newDecoder(JsValue label, JsValue options, JsObject proto) {
        String name = label.isUndefined() ? "utf-8" : Ops.toString(label);
        String canonical = EncodingLabels.canonicalName(name);
        if (canonical == null || EncodingLabels.REPLACEMENT.equals(canonical) || !EncodingLabels.isDecodable(canonical)) {
            throw JsException.rangeError("Failed to construct 'TextDecoder': The encoding label provided ('" + name + "') is invalid.");
        }
        boolean fatal = false;
        boolean ignoreBom = false;
        if (!options.isNullOrUndefined()) {
            if (!options.isObject()) {
                throw JsException.typeError("Failed to construct 'TextDecoder': The provided value is not of type 'TextDecoderOptions'.");
            }
            fatal = Ops.toBoolean(((JsObject) options).get("fatal"));
            ignoreBom = Ops.toBoolean(((JsObject) options).get("ignoreBOM"));
        }
        CharsetDecoder dec = null;
        String java = EncodingLabels.javaCharsetName(canonical);
        if (java != null && !EncodingLabels.WINDOWS_1252.equals(canonical) && !EncodingLabels.X_USER_DEFINED.equals(canonical)) {
            CodingErrorAction action = fatal ? CodingErrorAction.REPORT : CodingErrorAction.REPLACE;
            dec = Charset.forName(java).newDecoder().onMalformedInput(action).onUnmappableCharacter(action);
        }
        return new DecoderObject(proto, canonical, fatal, ignoreBom, dec);
    }

    /**
     * Returns <code>encoder.encoding</code>.
     * <p>
     * <code>encoder.encoding</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return "utf-8" / "utf-8"
     */
    JsValue encoderEncoding(JsValue self, JsValue[] args) {
        Natives.self(self, EncoderObject.class);
        return Js.str("utf-8");
    }

    /**
     * Implements <code>encoder.encode(input)</code>.
     * <p>
     * <code>encoder.encode(input)</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return a Uint8Array / Uint8Array
     */
    JsValue encode(JsValue self, JsValue[] args) {
        Natives.self(self, EncoderObject.class);
        String s = args.length == 0 || args[0].isUndefined() ? "" : Natives.usv(args[0]);
        return TypedArrays.newUint8Array(api.realm(), Encodings.utf8(s));
    }

    /**
     * Implements <code>encoder.encodeInto(source, destination)</code>: encodes as many whole code points as fit.
     * <p>
     * <code>encoder.encodeInto(source, destination)</code> 를 구현한다: 들어가는 만큼의 완전한 코드 포인트를 인코딩한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an object with read and written / read 와 written 을 가진 객체
     */
    JsValue encodeInto(JsValue self, JsValue[] args) {
        Natives.self(self, EncoderObject.class);
        Natives.requireArgs(args, 2, "encodeInto", "TextEncoder");
        String s = Natives.usv(args[0]);
        TypedArrays.ByteView view = args[1] instanceof org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrayObject ? TypedArrays.bytesOf(args[1]) : null;
        if (view == null) {
            throw JsException.typeError("Failed to execute 'encodeInto' on 'TextEncoder': parameter 2 is not of type 'Uint8Array'.");
        }
        int read = 0;
        int written = 0;
        // 코드 포인트를 하나씩 인코딩하여 남은 공간에 완전히 들어갈 때까지만 쓴다.
        while (read < s.length()) {
            int cp = s.codePointAt(read);
            byte[] b = Encodings.utf8(new String(Character.toChars(cp)));
            if (written + b.length > view.length) {
                break;
            }
            System.arraycopy(b, 0, view.array, view.offset + written, b.length);
            written += b.length;
            read += Character.charCount(cp);
        }
        JsObject result = api.realm().newObject();
        result.createDataProperty("read", Js.num(read));
        result.createDataProperty("written", Js.num(written));
        return result;
    }

    /**
     * Returns <code>decoder.encoding</code>.
     * <p>
     * <code>decoder.encoding</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the lower-case encoding name / 소문자 인코딩 이름
     */
    JsValue decoderEncoding(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, DecoderObject.class).canonical.toLowerCase());
    }

    /**
     * Returns <code>decoder.fatal</code>.
     * <p>
     * <code>decoder.fatal</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the flag / 표시
     */
    JsValue decoderFatal(JsValue self, JsValue[] args) {
        return Js.bool(Natives.self(self, DecoderObject.class).fatal);
    }

    /**
     * Returns <code>decoder.ignoreBOM</code>.
     * <p>
     * <code>decoder.ignoreBOM</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the flag / 표시
     */
    JsValue decoderIgnoreBom(JsValue self, JsValue[] args) {
        return Js.bool(Natives.self(self, DecoderObject.class).ignoreBom);
    }

    /**
     * Implements <code>decoder.decode(input, options)</code>.
     * <p>
     * <code>decoder.decode(input, options)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the decoded text / 디코딩한 텍스트
     */
    JsValue decode(JsValue self, JsValue[] args) {
        DecoderObject d = Natives.self(self, DecoderObject.class);
        byte[] bytes = new byte[0];
        JsValue input = Js.arg(args, 0);
        if (!input.isUndefined()) {
            TypedArrays.ByteView v = TypedArrays.bytesOf(input);
            if (v == null) {
                throw JsException.typeError("Failed to execute 'decode' on 'TextDecoder': The provided value is not of type '(ArrayBuffer or "
                        + "ArrayBufferView)'.");
            }
            bytes = new byte[v.length];
            System.arraycopy(v.array, v.offset, bytes, 0, v.length);
        }
        JsValue opts = Js.arg(args, 1);
        boolean stream = opts.isObject() && Ops.toBoolean(((JsObject) opts).get("stream"));
        String text = decodeBytes(d, bytes, stream);
        // BOM 은 스트림의 맨 앞에서 한 번만 걷어 낸다(ignoreBOM 이면 그대로 둔다).
        if (!d.started && text.length() > 0) {
            d.started = true;
            if (!d.ignoreBom && text.charAt(0) == 0xFEFF && !EncodingLabels.X_USER_DEFINED.equals(d.canonical)) {
                text = text.substring(1);
            }
        }
        if (!stream) {
            d.started = false;
            d.pending = new byte[0];
            if (d.decoder != null) {
                d.decoder.reset();
            }
        }
        return Js.str(text);
    }

    /**
     * Decodes one piece of bytes with the decoder of the object.
     * <p>
     * 객체의 디코더로 바이트 조각 하나를 디코딩한다.
     *
     * @param d      the decoder object / 디코더 객체
     * @param bytes  the new bytes / 새 바이트
     * @param stream true if more pieces follow / 조각이 더 이어지면 true
     * @return the text / 텍스트
     */
    private String decodeBytes(DecoderObject d, byte[] bytes, boolean stream) {
        if (d.decoder == null) {
            return EncodingLabels.decode(bytes, 0, d.canonical);
        }
        byte[] all = new byte[d.pending.length + bytes.length];
        System.arraycopy(d.pending, 0, all, 0, d.pending.length);
        System.arraycopy(bytes, 0, all, d.pending.length, bytes.length);
        ByteBuffer in = ByteBuffer.wrap(all);
        CharBuffer out = CharBuffer.allocate(all.length * 2 + 16);
        StringBuilder sb = new StringBuilder();
        try {
            // 입력이 없어질 때까지 디코딩한다. 출력 공간이 모자라면 비우고 계속한다.
            while (true) {
                CoderResult r = d.decoder.decode(in, out, !stream);
                if (r.isOverflow()) {
                    out.flip();
                    sb.append(out);
                    out.clear();
                    continue;
                }
                if (r.isError()) {
                    d.decoder.reset();
                    d.pending = new byte[0];
                    throw JsException.typeError("Failed to execute 'decode' on 'TextDecoder': The encoded data was not valid for encoding "
                            + d.canonical.toLowerCase());
                }
                break;
            }
            if (!stream) {
                // 마지막 조각이면 디코더 내부에 남은 상태를 비운다.
                CoderResult f = d.decoder.flush(out);
                if (f.isOverflow()) {
                    out.flip();
                    sb.append(out);
                    out.clear();
                    d.decoder.flush(out);
                }
            }
        } finally {
            out.flip();
            sb.append(out);
        }
        int left = in.remaining();
        d.pending = new byte[left];
        in.get(d.pending);
        return sb.toString();
    }
}
