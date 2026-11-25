package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;

/**
 * Base64 and Base64url operations for the Uint8Array text conversions.
 * <p>
 * Uint8Array 텍스트 변환의 Base64와 Base64url 연산.
 */
final class Uint8Base64 {

    /** RFC 4648 base64 alphabet. / RFC 4648 base64 알파벳. */
    static final String STANDARD = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /** RFC 4648 base64url alphabet. / RFC 4648 base64url 알파벳. */
    static final String URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    /**
     * Treatment of the final group of base64 digits.
     * <p>
     * 마지막 base64 글자 묶음의 처리 방법.
     */
    enum ChunkMode {
        /** Accept a short final group. / 짧은 마지막 묶음을 허용한다. */
        LOOSE,
        /** Require correct padding and zero unused bits. / 올바른 패딩과 0인 남는 비트를 요구한다. */
        STRICT,
        /** Return before an incomplete final group. / 불완전한 마지막 묶음 앞에서 반환한다. */
        STOP_BEFORE_PARTIAL
    }

    /**
     * Counts consumed code units and written bytes.
     * <p>
     * 소비한 코드 단위와 쓴 바이트의 수.
     */
    static final class DecodeResult {
        /** The consumed code units. / 소비한 코드 단위 수. */
        final int read;

        /** The written bytes. / 쓴 바이트 수. */
        final int written;

        /**
         * Stores the result of a successful decode.
         * <p>
         * 성공한 디코딩의 결과를 저장한다.
         *
         * @param read the consumed code units / 소비한 코드 단위 수
         * @param written the written bytes / 쓴 바이트 수
         */
        DecodeResult(int read, int written) {
            this.read = read;
            this.written = written;
        }
    }

    /** Uses static operations only. / 정적 연산만 사용한다. */
    private Uint8Base64() {
    }

    /**
     * Decodes a prefix, preserving completed output bytes on a syntax error.
     * <p>
     * 접두부를 디코딩하며 문법 오류가 생기면 완성한 출력 바이트를 보존한다.
     *
     * @param realm the current realm / 현재 realm
     * @param input the base64 text / base64 텍스트
     * @param alphabet the selected alphabet / 선택한 알파벳
     * @param mode final group behavior / 마지막 묶음의 처리 방법
     * @param bytes destination array / 대상 배열
     * @param offset starting byte position / 시작 바이트 위치
     * @param capacity maximum number of bytes to write / 쓸 수 있는 최대 바이트 수
     * @return consumed and written lengths / 소비한 길이와 쓴 길이
     */
    static DecodeResult decode(JsRealm realm, String input, String alphabet, ChunkMode mode, byte[] bytes, int offset, int capacity) {
        int read = 0;
        int written = 0;
        int chunk = 0;
        int digits = 0;
        int index = 0;
        int length = input.length();
        // 완성한 묶음 단위로 read를 앞으로 옮기고 출력 용량까지만 읽는다.
        while (written < capacity) {
            index = skipWhitespace(realm, input, index);
            if (index == length) {
                if (digits == 0) {
                    return new DecodeResult(length, written);
                }
                if (mode == ChunkMode.STOP_BEFORE_PARTIAL) {
                    return new DecodeResult(read, written);
                }
                if (mode == ChunkMode.STRICT || digits == 1) {
                    throw JsException.syntaxError("Incomplete base64 chunk");
                }
                writeChunk(bytes, offset + written, chunk, digits);
                return new DecodeResult(length, written + digits - 1);
            }
            char c = input.charAt(index++);
            // 패딩이 나오면 남은 입력이 마지막 묶음을 정확히 끝내는지 확인한다.
            if (c == '=') {
                int end = skipWhitespace(realm, input, index);
                if (digits < 2) {
                    throw JsException.syntaxError("Unexpected base64 padding");
                }
                if (digits == 2) {
                    if (end == length && mode == ChunkMode.STOP_BEFORE_PARTIAL) {
                        return new DecodeResult(read, written);
                    }
                    if (end == length || input.charAt(end) != '=') {
                        throw JsException.syntaxError("Missing base64 padding");
                    }
                    end = skipWhitespace(realm, input, end + 1);
                }
                if (end != length) {
                    throw JsException.syntaxError("Unexpected character after base64 padding");
                }
                int unused = digits == 2 ? 15 : 3;
                if (mode == ChunkMode.STRICT && (chunk & unused) != 0) {
                    throw JsException.syntaxError("Nonzero unused bits in a base64 chunk");
                }
                writeChunk(bytes, offset + written, chunk, digits);
                return new DecodeResult(length, written + digits - 1);
            }
            int digit = digit(c, alphabet);
            if (digit < 0) {
                throw JsException.syntaxError("Invalid base64 digit");
            }
            int remaining = capacity - written;
            // 확인한 글자가 더해지면 묶음이 남은 용량을 넘는다. 완성한 앞부분만 반환한다.
            if (remaining < 3 && digits == remaining + 1) {
                return new DecodeResult(read, written);
            }
            chunk = (chunk << 6) | digit;
            digits++;
            if (digits == 4) {
                writeChunk(bytes, offset + written, chunk, digits);
                written += 3;
                read = index;
                digits = 0;
                chunk = 0;
            }
        }
        return new DecodeResult(read, written);
    }

    /**
     * Skips the ASCII whitespace code units used by the Base64 decoder.
     * <p>
     * Base64 디코더가 허용하는 ASCII 공백 코드 단위를 건너뛴다.
     *
     * @param realm the current realm / 현재 realm
     * @param input input string / 입력 문자열
     * @param index starting position / 시작 위치
     * @return index after the whitespace / 공백 뒤 인덱스
     */
    private static int skipWhitespace(JsRealm realm, String input, int index) {
        // 규격의 ASCII 공백 다섯 개를 건너뛴다.
        while (index < input.length()) {
            Uint8ArrayConversions.poll(realm, index);
            char c = input.charAt(index);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '\f') {
                return index;
            }
            index++;
        }
        return index;
    }

    /**
     * Returns the numeric value of a digit in the selected alphabet.
     * <p>
     * 선택한 알파벳에서 글자의 숫자 값을 반환한다.
     *
     * @param c the digit / 글자
     * @param alphabet the selected alphabet / 선택한 알파벳
     * @return a value between 0 and 63, or -1 / 0~63의 값. 유효하지 않으면 -1
     */
    private static int digit(char c, String alphabet) {
        if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        }
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 26;
        }
        if (c >= '0' && c <= '9') {
            return c - '0' + 52;
        }
        if (c == alphabet.charAt(62)) {
            return 62;
        }
        return c == alphabet.charAt(63) ? 63 : -1;
    }

    /**
     * Writes the high bytes represented by two, three or four six-bit digits.
     * <p>
     * 6비트 숫자 두 개, 세 개 또는 네 개가 나타내는 상위 바이트를 쓴다.
     *
     * @param bytes destination / 대상
     * @param offset byte position / 바이트 위치
     * @param chunk packed six-bit digits / 모은 6비트 숫자
     * @param digits digit count / 숫자 수
     */
    private static void writeChunk(byte[] bytes, int offset, int chunk, int digits) {
        int packed = chunk << (6 * (4 - digits));
        bytes[offset] = (byte) (packed >>> 16);
        if (digits >= 3) {
            bytes[offset + 1] = (byte) (packed >>> 8);
        }
        if (digits == 4) {
            bytes[offset + 2] = (byte) packed;
        }
    }

    /**
     * Encodes the selected byte range using the selected alphabet and padding policy.
     * <p>
     * 지정한 바이트 범위를 선택한 알파벳과 패딩 정책으로 인코딩한다.
     *
     * @param realm the current realm / 현재 realm
     * @param bytes source data / 원본 데이터
     * @param offset starting byte position / 시작 바이트 위치
     * @param length byte count / 바이트 수
     * @param alphabet the selected alphabet / 선택한 알파벳
     * @param omitPadding true to omit padding / 패딩을 생략하려면 true
     * @return base64 text / base64 텍스트
     */
    static String encode(JsRealm realm, byte[] bytes, int offset, int length, String alphabet, boolean omitPadding) {
        int extra = length % 3;
        long chars = 4L * (length / 3) + (extra == 0 ? 0 : omitPadding ? extra + 1 : 4);
        realm.getLimits().checkStringLength(chars);
        StringBuilder output = new StringBuilder((int) chars);
        int complete = length - extra;
        // 바이트 세 개를 24비트로 모아 글자 네 개로 쓴다.
        for (int i = 0; i < complete; i += 3) {
            int packed = ((bytes[offset + i] & 255) << 16) | ((bytes[offset + i + 1] & 255) << 8) | (bytes[offset + i + 2] & 255);
            output.append(alphabet.charAt(packed >>> 18));
            output.append(alphabet.charAt((packed >>> 12) & 63));
            output.append(alphabet.charAt((packed >>> 6) & 63));
            output.append(alphabet.charAt(packed & 63));
            Uint8ArrayConversions.poll(realm, i);
        }
        if (extra > 0) {
            int packed = (bytes[offset + complete] & 255) << 16;
            if (extra == 2) {
                packed |= (bytes[offset + complete + 1] & 255) << 8;
            }
            output.append(alphabet.charAt(packed >>> 18));
            output.append(alphabet.charAt((packed >>> 12) & 63));
            if (extra == 2) {
                output.append(alphabet.charAt((packed >>> 6) & 63));
            } else if (!omitPadding) {
                output.append('=');
            }
            if (!omitPadding) {
                output.append('=');
            }
        }
        return output.toString();
    }
}
