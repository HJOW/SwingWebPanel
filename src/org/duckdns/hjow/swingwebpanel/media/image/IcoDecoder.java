package org.duckdns.hjow.swingwebpanel.media.image;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * A pure Java decoder for Windows icon and cursor files (<code>.ico</code>, <code>.cur</code>; plan items P18-02, P18-03; FEATURE_MATRIX
 * IMG-06). A file holds several images of different sizes; every image is either an embedded PNG (decoded by {@link PngDecoder}) or a
 * device independent bitmap with a 1-bit AND mask (decoded by {@link BmpDecoder}; a 32-bit bitmap with alpha ignores the mask). The
 * {@link #decode} method returns the largest image (ties go to the first entry), {@link #decodeAll} returns all
 * of them. Directory entries whose image cannot be decoded are skipped; if none can be decoded the file is CORRUPT.
 * <p>
 * Windows 아이콘/커서 파일(<code>.ico</code>, <code>.cur</code>)을 위한 순수 Java 디코더(계획 P18-02, P18-03; FEATURE_MATRIX IMG-06).
 * 파일에는 크기가 다른 이미지가 여러 개 있으며 각 이미지는 내장 PNG({@link PngDecoder} 가 디코딩)이거나 1비트 AND 마스크가 있는 장치 독립
 * 비트맵({@link BmpDecoder} 가 디코딩. 알파가 있는 32비트 비트맵은 마스크를 무시)이다. {@link #decode} 는 가장 큰 이미지(같으면 앞선 항목)를 반환하고
 * {@link #decodeAll} 은 모두 반환한다. 이미지를 디코딩할 수 없는 디렉터리 항목은 건너뛰며 하나도 디코딩하지 못하면 CORRUPT 이다.
 */
public final class IcoDecoder implements ImageDecoder {

    /**
     * Creates a decoder.
     * <p>
     * 디코더를 만든다.
     */
    public IcoDecoder() {
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return <code>ico</code>
     */
    public String getFormatName() {
        return "ico";
    }

    /**
     * Returns the MIME types.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return MIME types / MIME 타입
     */
    public String[] getMimeTypes() {
        return new String[] { "image/x-icon", "image/vnd.microsoft.icon", "image/ico", "image/x-ico" };
    }

    /**
     * Checks the icon or cursor directory header (reserved 0, type 1 or 2, at least one image).
     * <p>
     * 아이콘/커서 디렉터리 헤더(예약 0, 유형 1 또는 2, 이미지 하나 이상)를 확인한다.
     *
     * @param head first bytes / 첫 바이트
     * @return true for an icon or cursor file / 아이콘/커서 파일이면 true
     */
    public boolean accepts(byte[] head) {
        return head != null && head.length >= 22 && head[0] == 0 && head[1] == 0 && (head[2] == 1 || head[2] == 2) && head[3] == 0
                && ((head[4] & 0xFF) | ((head[5] & 0xFF) << 8)) > 0;
    }

    /**
     * Decodes the largest image of an icon file.
     * <p>
     * 아이콘 파일에서 가장 큰 이미지를 디코딩한다.
     *
     * @param data   file data / 파일 데이터
     * @param limits limits / 제한
     * @return the largest image / 가장 큰 이미지
     * @throws MediaException on damaged, unsupported or too large data / 손상, 미지원, 과대 데이터일 때
     */
    public DecodedImage decode(byte[] data, DecodeLimits limits) throws MediaException {
        List<DecodedImage> all = decodeAll(data, limits);
        DecodedImage best = all.get(0);
        // 면적이 가장 큰 이미지를 고르고 같으면 앞선 것을 유지한다.
        for (int i = 1; i < all.size(); i++) {
            DecodedImage c = all.get(i);
            if ((long) c.getWidth() * c.getHeight() > (long) best.getWidth() * best.getHeight()) {
                best = c;
            }
        }
        return best;
    }

    /**
     * Decodes every image of an icon or cursor file in directory order. Entries that cannot be decoded are skipped.
     * <p>
     * 아이콘/커서 파일의 모든 이미지를 디렉터리 순서대로 디코딩한다. 디코딩할 수 없는 항목은 건너뛴다.
     *
     * @param d      file data / 파일 데이터
     * @param limits limits / 제한
     * @return the images, at least one / 이미지(하나 이상)
     * @throws MediaException CORRUPT if the header is invalid or no image can be decoded; TOO_LARGE or CANCELLED as they occur / 헤더가 잘못되었거나 디코딩할 이미지가 없으면 CORRUPT. TOO_LARGE, CANCELLED 는 발생 그대로
     */
    public static List<DecodedImage> decodeAll(byte[] d, DecodeLimits limits) throws MediaException {
        try {
            return decodeAllImpl(d, limits);
        } catch (RuntimeException e) {
            throw new MediaException(MediaException.Kind.CORRUPT, "damaged icon data: " + e, e);
        } catch (OutOfMemoryError e) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "not enough memory to decode the icon");
        }
    }

    /**
     * The actual decoding of all entries.
     * <p>
     * 모든 항목의 실제 디코딩.
     *
     * @param d      file data / 파일 데이터
     * @param limits limits / 제한
     * @return the images / 이미지
     * @throws MediaException on failure / 실패 시
     */
    private static List<DecodedImage> decodeAllImpl(byte[] d, DecodeLimits limits) throws MediaException {
        if (!new IcoDecoder().accepts(d)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "not an icon file");
        }
        if (d.length > limits.getMaxBytes()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "icon data is too large");
        }
        int count = PixelOps.u16le(d, 4);
        PixelOps.need(d, 6, 16L * count);
        PngDecoder png = new PngDecoder();
        List<DecodedImage> result = new ArrayList<DecodedImage>();
        // 디렉터리 항목마다 이미지를 디코딩한다.
        for (int i = 0; i < count; i++) {
            PixelOps.checkCancelled();
            int e = 6 + 16 * i;
            long size = PixelOps.s32le(d, e + 8) & 0xFFFFFFFFL;
            long offset = PixelOps.s32le(d, e + 12) & 0xFFFFFFFFL;
            if (offset >= d.length || size == 0) {
                continue;
            }
            int end = (int) Math.min(offset + size, d.length);
            int off = (int) offset;
            try {
                if (end - off >= 8 && png.accepts(java.util.Arrays.copyOfRange(d, off, off + 8))) {
                    DecodedImage img = png.decode(java.util.Arrays.copyOfRange(d, off, end), limits);
                    result.add(new DecodedImage(img.getWidth(), img.getHeight(), img.getFrames().get(0).getArgb()));
                } else {
                    result.add(BmpDecoder.decodeDib(d, off, -1, end, true, limits));
                }
            } catch (MediaException ex) {
                // 취소와 크기 초과는 그대로 전달하고 손상된 항목은 건너뛴다.
                if (ex.getKind() == MediaException.Kind.CANCELLED || ex.getKind() == MediaException.Kind.TOO_LARGE) {
                    throw ex;
                }
            }
        }
        if (result.isEmpty()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the icon file contains no decodable image");
        }
        return result;
    }
}
