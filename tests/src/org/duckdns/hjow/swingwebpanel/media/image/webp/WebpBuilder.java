package org.duckdns.hjow.swingwebpanel.media.image.webp;

import java.io.ByteArrayOutputStream;

/**
 * Builds WebP containers for the tests: RIFF files with the extended header and animation chunks around bitstreams that were produced by
 * libwebp (taken out of the fixture files). The container code of the decoder is therefore tested with files that are made independently of it.
 * <p>
 * 시험용 WebP 컨테이너를 만든다. libwebp 가 만든 비트스트림(자료 파일에서 꺼낸 것)을 확장 헤더와 애니메이션 청크로 감싼 RIFF 파일이다. 따라서
 * 디코더의 컨테이너 코드를 디코더와 독립적으로 만든 파일로 시험한다.
 */
final class WebpBuilder {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private WebpBuilder() {
    }

    /**
     * Writes a little-endian integer.
     * <p>
     * 리틀 엔디언 정수를 쓴다.
     *
     * @param out   destination / 목적지
     * @param value value / 값
     * @param bytes number of bytes / 바이트 수
     */
    private static void le(ByteArrayOutputStream out, long value, int bytes) {
        // 낮은 자리 바이트부터 쓴다.
        for (int i = 0; i < bytes; i++) {
            out.write((int) (value >> (8 * i)) & 0xff);
        }
    }

    /**
     * Builds a chunk: four characters, the length, the data and a padding byte for an odd length.
     * <p>
     * 청크를 만든다: 문자 네 개, 길이, 데이터, 홀수 길이이면 패딩 바이트.
     *
     * @param type    four characters / 문자 네 개
     * @param payload data / 데이터
     * @return the chunk / 청크
     */
    static byte[] chunk(String type, byte[] payload) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(type.charAt(0));
        out.write(type.charAt(1));
        out.write(type.charAt(2));
        out.write(type.charAt(3));
        le(out, payload.length, 4);
        out.write(payload, 0, payload.length);
        if ((payload.length & 1) != 0) {
            out.write(0);
        }
        return out.toByteArray();
    }

    /**
     * Builds a RIFF WEBP file from chunks.
     * <p>
     * 청크들로 RIFF WEBP 파일을 만든다.
     *
     * @param chunks finished chunks / 만들어진 청크들
     * @return the file / 파일
     */
    static byte[] file(byte[]... chunks) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write('W');
        body.write('E');
        body.write('B');
        body.write('P');
        // 청크를 차례로 이어 붙인다.
        for (int i = 0; i < chunks.length; i++) {
            body.write(chunks[i], 0, chunks[i].length);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('R');
        out.write('I');
        out.write('F');
        out.write('F');
        le(out, body.size(), 4);
        out.write(body.toByteArray(), 0, body.size());
        return out.toByteArray();
    }

    /**
     * Builds a <code>VP8X</code> chunk.
     * <p>
     * <code>VP8X</code> 청크를 만든다.
     *
     * @param flags  flag byte / 플래그 바이트
     * @param width  canvas width / 캔버스 가로
     * @param height canvas height / 캔버스 세로
     * @return the chunk / 청크
     */
    static byte[] vp8x(int flags, int width, int height) {
        ByteArrayOutputStream p = new ByteArrayOutputStream();
        p.write(flags);
        p.write(0);
        p.write(0);
        p.write(0);
        le(p, width - 1, 3);
        le(p, height - 1, 3);
        return chunk("VP8X", p.toByteArray());
    }

    /**
     * Builds an <code>ANIM</code> chunk.
     * <p>
     * <code>ANIM</code> 청크를 만든다.
     *
     * @param background background colour (ignored by the decoder) / 배경색(디코더는 무시한다)
     * @param loops      loop count / 반복 횟수
     * @return the chunk / 청크
     */
    static byte[] anim(int background, int loops) {
        ByteArrayOutputStream p = new ByteArrayOutputStream();
        le(p, background, 4);
        le(p, loops, 2);
        return chunk("ANIM", p.toByteArray());
    }

    /**
     * Builds an <code>ANMF</code> chunk around image chunks.
     * <p>
     * 이미지 청크를 감싸는 <code>ANMF</code> 청크를 만든다.
     *
     * @param x        left offset (even) / 왼쪽 오프셋(짝수)
     * @param y        top offset (even) / 위쪽 오프셋(짝수)
     * @param width    frame width / 프레임 가로
     * @param height   frame height / 프레임 세로
     * @param duration delay in ms / 지연(ms)
     * @param flags    flag byte: bit 0 dispose to background, bit 1 do not blend / 플래그 바이트: 0번 비트 배경 지우기, 1번 비트 합성 안 함
     * @param sub      finished chunks of the frame / 프레임의 만들어진 청크들
     * @return the chunk / 청크
     */
    static byte[] anmf(int x, int y, int width, int height, int duration, int flags, byte[]... sub) {
        ByteArrayOutputStream p = new ByteArrayOutputStream();
        le(p, x / 2, 3);
        le(p, y / 2, 3);
        le(p, width - 1, 3);
        le(p, height - 1, 3);
        le(p, duration, 3);
        p.write(flags);
        // 프레임 안의 청크를 이어 붙인다.
        for (int i = 0; i < sub.length; i++) {
            p.write(sub[i], 0, sub[i].length);
        }
        return chunk("ANMF", p.toByteArray());
    }

    /**
     * Finds a chunk of a WebP file and returns it complete (header and padding included), so that it can be used inside another container.
     * <p>
     * WebP 파일의 청크를 찾아 헤더와 패딩까지 포함해 그대로 반환한다. 다른 컨테이너 안에서 쓸 수 있다.
     *
     * @param file the WebP file / WebP 파일
     * @param type four characters / 문자 네 개
     * @return the chunk, or null if there is none / 청크. 없으면 null
     */
    static byte[] extract(byte[] file, String type) {
        int p = 12;
        // 청크를 차례로 훑어 종류가 같은 첫 청크를 찾는다.
        while (p + 8 <= file.length) {
            int size = (file[p + 4] & 0xff) | ((file[p + 5] & 0xff) << 8) | ((file[p + 6] & 0xff) << 16) | ((file[p + 7] & 0xff) << 24);
            int total = 8 + size + (size & 1);
            if (file[p] == type.charAt(0) && file[p + 1] == type.charAt(1) && file[p + 2] == type.charAt(2) && file[p + 3] == type.charAt(3)) {
                byte[] c = new byte[total];
                System.arraycopy(file, p, c, 0, Math.min(total, file.length - p));
                return c;
            }
            p += total;
        }
        return null;
    }
}
