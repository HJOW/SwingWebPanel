package org.duckdns.hjow.swingwebpanel.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * The binary file format of the process-wide localStorage (plan item P16-04): the 8-byte identifier <code>SWPLSTOR</code>, a format
 * version, the number of origins and for each origin its name and its items, every string stored as a length followed by UTF-8 bytes, and
 * a CRC-32 over everything before it. Reading validates identifier, version, all lengths against the remaining bytes, the limits of
 * {@link StorageLimits} and the checksum; Java object serialization is never used. A file that fails validation is reported as a
 * {@link FormatException} so that the manager can reset the store.
 * <p>
 * 프로세스 공용 localStorage 의 이진 파일 형식(계획 P16-04): 8 바이트 식별자 <code>SWPLSTOR</code>, 형식 버전, 오리진 수, 오리진마다 이름과 항목들,
 * 모든 문자열은 길이 뒤에 UTF-8 바이트로 저장, 그리고 그 앞 전체에 대한 CRC-32. 읽을 때 식별자, 버전, 남은 바이트에 대한 모든 길이,
 * {@link StorageLimits} 의 제한, 체크섬을 검증하며 Java 객체 직렬화는 쓰지 않는다. 검증에 실패한 파일은 {@link FormatException} 으로 보고되어
 * 관리자가 저장소를 초기화할 수 있다.
 */
final class StorageFile {

    /**
     * Identifier at the start of the file.
     * <p>
     * 파일 첫머리의 식별자.
     */
    private static final byte[] MAGIC = { 'S', 'W', 'P', 'L', 'S', 'T', 'O', 'R' };

    /**
     * Current format version.
     * <p>
     * 현재 형식 버전.
     */
    static final int VERSION = 1;

    /**
     * The file is invalid (damaged, truncated, unknown version, over the limits).
     * <p>
     * 파일이 올바르지 않다(손상, 잘림, 알 수 없는 버전, 제한 초과).
     */
    static final class FormatException extends Exception {
        /**
         * Serialization version id.
         * <p>
         * 직렬화 버전 식별자.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Creates the exception.
         * <p>
         * 예외를 만든다.
         *
         * @param message description / 설명
         */
        FormatException(String message) {
            super(message);
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private StorageFile() {
    }

    /**
     * Encodes the stores of all origins.
     * <p>
     * 모든 오리진의 저장소를 인코딩한다.
     *
     * @param areas areas by serialized origin / 직렬화된 오리진별 영역
     * @return file bytes / 파일 바이트
     * @throws IOException never in practice (memory streams) / 실제로는 발생하지 않음(메모리 스트림)
     */
    static byte[] encode(Map<String, StorageArea> areas) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        out.write(MAGIC);
        out.writeInt(VERSION);
        out.writeInt(areas.size());
        // 오리진마다 이름과 항목 수, 항목을 쓴다.
        for (Map.Entry<String, StorageArea> e : areas.entrySet()) {
            writeString(out, e.getKey());
            java.util.List<StorageEntry> items = e.getValue().entries(e.getKey());
            out.writeInt(items.size());
            // 항목마다 키와 값을 쓴다.
            for (StorageEntry item : items) {
                writeString(out, item.getKey());
                writeString(out, item.getValue());
            }
        }
        out.flush();
        byte[] body = bos.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(body);
        ByteArrayOutputStream result = new ByteArrayOutputStream(body.length + 8);
        result.write(body);
        new DataOutputStream(result).writeLong(crc.getValue());
        return result.toByteArray();
    }

    /**
     * Writes a string as length and UTF-8 bytes.
     * <p>
     * 문자열을 길이와 UTF-8 바이트로 쓴다.
     *
     * @param out output / 출력
     * @param s   string / 문자열
     * @throws IOException on write failure / 쓰기 실패 시
     */
    private static void writeString(DataOutputStream out, String s) throws IOException {
        byte[] b = s.getBytes("UTF-8");
        out.writeInt(b.length);
        out.write(b);
    }

    /**
     * Decodes and validates file bytes.
     * <p>
     * 파일 바이트를 디코딩하고 검증한다.
     *
     * @param data file bytes / 파일 바이트
     * @return the stores by serialized origin / 직렬화된 오리진별 저장소
     * @throws FormatException if the bytes are not a valid store / 올바른 저장소가 아니면
     */
    static Map<String, StorageArea> decode(byte[] data) throws FormatException {
        if (data.length < MAGIC.length + 4 + 4 + 8) {
            throw new FormatException("file is too short");
        }
        CRC32 crc = new CRC32();
        crc.update(data, 0, data.length - 8);
        long stored;
        DataInputStream tail = new DataInputStream(new ByteArrayInputStream(data, data.length - 8, 8));
        try {
            stored = tail.readLong();
        } catch (IOException e) {
            throw new FormatException("cannot read the checksum");
        }
        if (stored != crc.getValue()) {
            throw new FormatException("checksum mismatch");
        }
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data, 0, data.length - 8));
        Map<String, StorageArea> result = new LinkedHashMap<String, StorageArea>();
        try {
            byte[] magic = new byte[MAGIC.length];
            in.readFully(magic);
            // 식별자의 모든 바이트를 비교한다.
            for (int i = 0; i < MAGIC.length; i++) {
                if (magic[i] != MAGIC[i]) {
                    throw new FormatException("not a storage file");
                }
            }
            int version = in.readInt();
            if (version != VERSION) {
                throw new FormatException("unknown version " + version);
            }
            int origins = in.readInt();
            if (origins < 0 || origins > StorageLimits.MAX_ORIGINS) {
                throw new FormatException("invalid origin count");
            }
            long total = 0;
            // 오리진마다 이름과 항목을 읽고 제한을 확인한다.
            for (int o = 0; o < origins; o++) {
                String origin = readString(in, data.length);
                int count = in.readInt();
                if (count < 0 || count > StorageLimits.MAX_ITEMS_PER_ORIGIN || result.containsKey(origin)) {
                    throw new FormatException("invalid item count or duplicate origin");
                }
                StorageArea area = new StorageArea();
                // 항목마다 키와 값을 읽고 전체 크기 제한을 확인한다.
                for (int i = 0; i < count; i++) {
                    String key = readString(in, data.length);
                    String value = readString(in, data.length);
                    total += key.length() + value.length();
                    if (total > StorageLimits.MAX_TOTAL_CHARS) {
                        throw new FormatException("the store is larger than the limit");
                    }
                    area.put(key, value);
                }
                result.put(origin, area);
            }
            if (in.available() != 0) {
                throw new FormatException("unexpected trailing bytes");
            }
        } catch (IOException e) {
            throw new FormatException("truncated data: " + e.getMessage());
        } catch (StorageQuotaException e) {
            throw new FormatException("limits exceeded: " + e.getMessage());
        }
        return result;
    }

    /**
     * Reads a length-prefixed UTF-8 string, rejecting impossible lengths and malformed bytes.
     * <p>
     * 길이가 앞선 UTF-8 문자열을 읽으며 불가능한 길이와 잘못된 바이트를 거부한다.
     *
     * @param in       input / 입력
     * @param fileSize size of the file in bytes (upper bound for any length) / 파일 크기(바이트). 모든 길이의 상한
     * @return the string / 문자열
     * @throws IOException     on short data / 데이터가 모자랄 때
     * @throws FormatException on an invalid length or encoding / 길이나 인코딩이 올바르지 않을 때
     */
    private static String readString(DataInputStream in, int fileSize) throws IOException, FormatException {
        int len = in.readInt();
        if (len < 0 || len > fileSize || len > in.available()) {
            throw new FormatException("invalid string length " + len);
        }
        byte[] b = new byte[len];
        in.readFully(b);
        try {
            return Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(b)).toString();
        } catch (CharacterCodingException e) {
            throw new FormatException("invalid UTF-8");
        }
    }
}
