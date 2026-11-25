package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.text.CharClass;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.Uint8Base64.ChunkMode;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.Uint8Base64.DecodeResult;

/**
 * Text encodings on the Uint8Array constructor and its concrete prototype.
 * <p>
 * Uint8Array 생성자와 그 구체 프로토타입의 텍스트 인코딩 기능.
 */
final class Uint8ArrayConversions {

    /** The lowercase digits for hexadecimal output. / 소문자 16진 출력 숫자. */
    private static final String HEX = "0123456789abcdef";

    /** Uses static functions only. / 정적 함수만 사용한다. */
    private Uint8ArrayConversions() {
    }

    /**
     * Installs the conversion methods on the Uint8Array objects.
     * <p>
     * Uint8Array 객체에 변환 메서드를 설치한다.
     *
     * @param realm realm / realm
     * @param ctor Uint8Array constructor / Uint8Array 생성자
     * @param proto Uint8Array prototype / Uint8Array 프로토타입
     */
    static void install(JsRealm realm, JsObject ctor, JsObject proto) {
        installBase64(realm, ctor, proto);
        realm.method(ctor, "fromHex", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                String text = requireString(Js.arg(args, 0));
                int length = hexByteLength(text);
                TypedArrayObject result = newArray(r, length);
                decodeHex(r, text, result.getBuffer(), 0, length);
                return result;
            }
        });
        realm.method(proto, "setFromHex", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                TypedArrayObject target = requireUint8(receiver);
                String text = requireString(Js.arg(args, 0));
                TypedSupport.validate(target);
                int written = Math.min(hexByteLength(text), target.getLength());
                decodeHex(r, text, target.getBuffer(), target.getByteOffset(), written);
                return result(r, 2 * written, written);
            }
        });
        realm.method(proto, "toHex", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                TypedArrayObject source = TypedSupport.validate(requireUint8(receiver));
                int length = source.getLength();
                r.getLimits().checkStringLength(2L * length);
                StringBuilder output = new StringBuilder(2 * length);
                ArrayBufferObject bytes = source.getBuffer();
                int offset = source.getByteOffset();
                // 각 바이트의 높은 자리와 낮은 자리를 소문자로 출력한다.
                for (int i = 0; i < length; i++) {
                    int value = bytes.readByte(offset + i);
                    output.append(HEX.charAt(value >>> 4));
                    output.append(HEX.charAt(value & 15));
                    poll(r, i);
                }
                return Js.str(output.toString());
            }
        });
    }

    /**
     * Installs the Base64 codecs with option evaluation before reading the buffer bounds.
     * <p>
     * 버퍼 경계를 읽기 전에 옵션을 평가하는 Base64 변환 메서드를 설치한다.
     *
     * @param realm the realm / realm
     * @param ctor Uint8Array constructor / Uint8Array 생성자
     * @param proto Uint8Array prototype / Uint8Array 프로토타입
     */
    private static void installBase64(JsRealm realm, JsObject ctor, JsObject proto) {
        realm.method(ctor, "fromBase64", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                String input = requireString(Js.arg(args, 0));
                JsObject options = options(Js.arg(args, 1));
                String alphabet = alphabet(options);
                ChunkMode mode = chunkMode(options);
                ArrayBufferObject bytes = new ArrayBufferObject(null, TypedSupport.allocate(((input.length() + 3L) / 4L) * 3L));
                DecodeResult decoded = Uint8Base64.decode(r, input, alphabet, mode, bytes, 0, bytes.getByteLength());
                TypedArrayObject output = newArray(r, decoded.written);
                bytes.copyBytesTo(0, output.getBuffer(), 0, decoded.written);
                return output;
            }
        });
        realm.method(proto, "setFromBase64", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                TypedArrayObject target = requireUint8(receiver);
                String input = requireString(Js.arg(args, 0));
                JsObject options = options(Js.arg(args, 1));
                String alphabet = alphabet(options);
                ChunkMode mode = chunkMode(options);
                TypedSupport.validate(target);
                DecodeResult decoded = Uint8Base64.decode(r, input, alphabet, mode, target.getBuffer(),
                        target.getByteOffset(), target.getLength());
                return result(r, decoded.read, decoded.written);
            }
        });
        realm.method(proto, "toBase64", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                TypedArrayObject source = requireUint8(receiver);
                JsObject options = options(Js.arg(args, 0));
                String alphabet = alphabet(options);
                boolean omitPadding = Ops.toBoolean(option(options, "omitPadding"));
                TypedSupport.validate(source);
                return Js.str(Uint8Base64.encode(r, source.getBuffer(), source.getByteOffset(), source.getLength(), alphabet, omitPadding));
            }
        });
    }

    /**
     * Accepts an options object or omitted options.
     * <p>
     * 옵션 객체 또는 생략된 옵션을 받는다.
     *
     * @param value options argument / 옵션 인수
     * @return the options or null for omitted options / 옵션. 생략하면 null
     */
    private static JsObject options(JsValue value) {
        if (value.isUndefined()) {
            return null;
        }
        if (!(value instanceof JsObject)) {
            throw JsException.typeError("Base64 options must be an object");
        }
        return (JsObject) value;
    }

    /**
     * Reads the named option, defaulting to undefined.
     * <p>
     * 지정한 옵션을 읽는다. 기본값은 undefined다.
     *
     * @param options the options object or null / 옵션 객체 또는 null
     * @param name the option name / 옵션 이름
     * @return the property value or undefined / 프로퍼티 값 또는 undefined
     */
    private static JsValue option(JsObject options, String name) {
        return options == null ? Js.UNDEFINED : options.get(name);
    }

    /**
     * Reads the base64 or base64url alphabet selector.
     * <p>
     * base64 또는 base64url을 선택하는 옵션을 읽는다.
     *
     * @param options the options / 옵션
     * @return the selected encoding alphabet / 선택한 인코딩 알파벳
     */
    private static String alphabet(JsObject options) {
        JsValue value = option(options, "alphabet");
        if (value.isUndefined() || (value instanceof JsString && "base64".equals(((JsString) value).getValue()))) {
            return Uint8Base64.STANDARD;
        }
        if (value instanceof JsString && "base64url".equals(((JsString) value).getValue())) {
            return Uint8Base64.URL;
        }
        throw JsException.typeError("Invalid base64 alphabet");
    }

    /**
     * Reads the final chunk policy for a decoder.
     * <p>
     * 디코더의 마지막 묶음 처리 옵션을 읽는다.
     *
     * @param options the options / 옵션
     * @return final chunk policy / 마지막 묶음 처리 방법
     */
    private static ChunkMode chunkMode(JsObject options) {
        JsValue value = option(options, "lastChunkHandling");
        if (value.isUndefined()) {
            return ChunkMode.LOOSE;
        }
        if (value instanceof JsString) {
            String mode = ((JsString) value).getValue();
            if ("loose".equals(mode)) {
                return ChunkMode.LOOSE;
            }
            if ("strict".equals(mode)) {
                return ChunkMode.STRICT;
            }
            if ("stop-before-partial".equals(mode)) {
                return ChunkMode.STOP_BEFORE_PARTIAL;
            }
        }
        throw JsException.typeError("Invalid lastChunkHandling");
    }

    /**
     * Requires the concrete Uint8Array brand before observing arguments.
     * <p>
     * 인수를 읽기 전에 구체 Uint8Array 브랜드를 확인한다.
     *
     * @param value the receiver / 수신자
     * @return the byte array / 바이트 배열
     */
    private static TypedArrayObject requireUint8(JsValue value) {
        if (!(value instanceof TypedArrayObject) || ((TypedArrayObject) value).getType() != ElementType.UINT8) {
            throw JsException.typeError("Conversion receiver must be a Uint8Array");
        }
        return (TypedArrayObject) value;
    }

    /**
     * Gets the content of a primitive string.
     * <p>
     * 문자열 원시값의 내용을 얻는다.
     *
     * @param input the input / 입력
     * @return the string / 문자열
     */
    private static String requireString(JsValue input) {
        if (!(input instanceof JsString)) {
            throw JsException.typeError("Conversion input must be a string");
        }
        return ((JsString) input).getValue();
    }

    /**
     * Creates a byte array with the realm's intrinsic Uint8Array prototype.
     * <p>
     * realm의 내장 Uint8Array 프로토타입으로 바이트 배열을 만든다.
     *
     * @param realm realm / realm
     * @param length the byte length / 바이트 길이
     * @return the new array / 새 배열
     */
    private static TypedArrayObject newArray(JsRealm realm, int length) {
        return TypedArrayFactory.allocate(realm, ElementType.UINT8, realm.getIntrinsic("%Uint8ArrayPrototype%"), length);
    }

    /**
     * Checks hex input parity and returns its full decoded length.
     * <p>
     * 16진 입력의 길이가 짝수인지 검사한 뒤 전체 디코딩 길이를 반환한다.
     *
     * @param text the input / 입력
     * @return the number of encoded bytes / 인코딩된 바이트 수
     */
    private static int hexByteLength(String text) {
        if ((text.length() & 1) != 0) {
            throw JsException.syntaxError("Hex input must contain an even number of characters");
        }
        return text.length() / 2;
    }

    /**
     * Decodes the requested prefix into the destination, keeping any completed bytes on a syntax error.
     * <p>
     * 지정한 접두부를 대상에 디코딩하고 문법 오류가 나면 이미 쓴 바이트는 보존한다.
     *
     * @param realm realm / realm
     * @param text the input / 입력
     * @param bytes destination bytes / 대상 바이트
     * @param offset destination byte offset / 대상 바이트 위치
     * @param count the number of pairs to decode / 디코딩할 문자 쌍의 수
     */
    private static void decodeHex(JsRealm realm, String text, ArrayBufferObject bytes, int offset, int count) {
        // 대상이 수용할 수 있는 두 글자 묶음만 차례로 검사해 저장한다.
        for (int i = 0; i < count; i++) {
            int high = CharClass.hexValue(text.charAt(2 * i));
            int low = CharClass.hexValue(text.charAt(2 * i + 1));
            if (high < 0 || low < 0) {
                throw JsException.syntaxError("Invalid hexadecimal digit");
            }
            bytes.writeByte(offset + i, (high << 4) | low);
            poll(realm, i);
        }
    }

    /**
     * Returns the read/written result in property creation order.
     * <p>
     * 프로퍼티 생성 순서를 지켜 read/written 결과를 반환한다.
     *
     * @param realm realm / realm
     * @param read consumed input length / 소비한 입력 길이
     * @param written number of bytes written / 쓴 바이트 수
     * @return the counts object / 횟수 객체
     */
    private static JsObject result(JsRealm realm, int read, int written) {
        JsObject result = realm.newObject();
        result.createDataProperty("read", Js.num(read));
        result.createDataProperty("written", Js.num(written));
        return result;
    }

    /**
     * Checks the execution limits periodically while processing many bytes.
     * <p>
     * 바이트를 많이 처리할 때 실행 한도를 주기적으로 검사한다.
     *
     * @param realm realm / realm
     * @param index loop position / 반복 위치
     */
    static void poll(JsRealm realm, int index) {
        if ((index & 4095) == 0) {
            realm.getLimits().poll();
        }
    }
}
