package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Serializes canvas pixels to a file for <code>toDataURL()</code> and <code>toBlob()</code> with the public <code>javax.imageio</code> API:
 * PNG (the default and the fallback for every other type) and JPEG (transparent pixels become black, as in browsers; the quality is a number
 * from 0 to 1, default 0.92). WebP and the other types that the canvas specification lists are not encoded: the encoder answers with PNG, as
 * the specification requires for unsupported types.
 * <p>
 * <code>toDataURL()</code> 과 <code>toBlob()</code> 을 위해 공개 <code>javax.imageio</code> API 로 캔버스 화소를 파일로 직렬화한다: PNG(기본이며 다른 모든
 * 형식의 대체)와 JPEG(투명한 화소는 브라우저처럼 검정이 된다. 품질은 0 에서 1 사이의 숫자이며 기본 0.92). WebP 와 캔버스 규격이 나열하는 다른 형식은 인코딩하지
 * 않고, 규격이 지원하지 않는 형식에 요구하는 대로 PNG 로 답한다.
 */
public final class CanvasEncoder {

    /**
     * The default JPEG quality.
     * <p>
     * 기본 JPEG 품질.
     */
    private static final float DEFAULT_JPEG_QUALITY = 0.92f;

    /**
     * The characters of Base64.
     * <p>
     * Base64 문자.
     */
    private static final String BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /**
     * The encoded file.
     * <p>
     * 인코딩된 파일.
     */
    public static final class Encoded {
        /**
         * The MIME type of the bytes.
         * <p>
         * 바이트의 MIME 타입.
         */
        public final String type;

        /**
         * The bytes of the file.
         * <p>
         * 파일의 바이트.
         */
        public final byte[] bytes;

        /**
         * Creates the value.
         * <p>
         * 값을 만든다.
         *
         * @param type  the MIME type / MIME 타입
         * @param bytes the bytes / 바이트
         */
        Encoded(String type, byte[] bytes) {
            this.type = type;
            this.bytes = bytes;
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CanvasEncoder() {
    }

    /**
     * Encodes pixels.
     * <p>
     * 화소를 인코딩한다.
     *
     * @param image   the pixels / 화소
     * @param type    the requested MIME type (may be null) / 요청한 MIME 타입(null 가능)
     * @param quality the requested quality, NaN if none / 요청한 품질. 없으면 NaN
     * @return the file, or null if the encoder failed / 파일. 인코더가 실패하면 null
     */
    public static Encoded encode(CanvasImage image, String type, double quality) {
        String t = type == null ? "" : type.trim().toLowerCase();
        boolean jpeg = "image/jpeg".equals(t) || "image/jpg".equals(t);
        try {
            BufferedImage bi = image.toBufferedImage();
            if (jpeg) {
                float q = quality >= 0 && quality <= 1 ? (float) quality : DEFAULT_JPEG_QUALITY;
                return new Encoded("image/jpeg", writeJpeg(bi, q));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(bi, "png", out)) {
                return null;
            }
            return new Encoded("image/png", out.toByteArray());
        } catch (IOException e) {
            return null;
        } catch (RuntimeException e) {
            // 이미지 저장소의 실패는 인코딩 실패로 알린다.
            return null;
        }
    }

    /**
     * Writes a JPEG of an image on a black background.
     * <p>
     * 이미지를 검은 배경 위의 JPEG 로 쓴다.
     *
     * @param src     the image / 이미지
     * @param quality the quality 0..1 / 품질 0..1
     * @return the bytes / 바이트
     * @throws IOException if writing fails / 쓰기에 실패하면
     */
    private static byte[] writeJpeg(BufferedImage src, float quality) throws IOException {
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpeg");
        if (!it.hasNext()) {
            throw new IOException("no JPEG writer");
        }
        ImageWriter writer = it.next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(bytes);
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgb, null, null), param);
            ios.flush();
        } finally {
            ios.close();
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    /**
     * Builds a <code>data:</code> URL.
     * <p>
     * <code>data:</code> URL 을 만든다.
     *
     * @param e the encoded file / 인코딩된 파일
     * @return the URL / URL
     */
    public static String dataUrl(Encoded e) {
        return "data:" + e.type + ";base64," + base64(e.bytes);
    }

    /**
     * Encodes bytes as Base64.
     * <p>
     * 바이트를 Base64 로 인코딩한다.
     *
     * @param data the bytes / 바이트
     * @return the text / 문자열
     */
    static String base64(byte[] data) {
        StringBuilder b = new StringBuilder((data.length + 2) / 3 * 4);
        // 세 바이트씩 네 문자로 바꾼다.
        for (int i = 0; i < data.length; i += 3) {
            int n = (data[i] & 0xFF) << 16;
            if (i + 1 < data.length) {
                n |= (data[i + 1] & 0xFF) << 8;
            }
            if (i + 2 < data.length) {
                n |= data[i + 2] & 0xFF;
            }
            b.append(BASE64.charAt((n >> 18) & 63)).append(BASE64.charAt((n >> 12) & 63));
            b.append(i + 1 < data.length ? BASE64.charAt((n >> 6) & 63) : '=');
            b.append(i + 2 < data.length ? BASE64.charAt(n & 63) : '=');
        }
        return b.toString();
    }
}
