package org.duckdns.hjow.swingwebpanel.media.image.webp;

import java.io.IOException;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link WebpDecoder} with files made by the WebP encoder of Chrome (plan item P18-03; FEATURE_MATRIX IMG-05): lossy and lossless
 * images, the alpha plane, and odd sizes are compared row by row with the pixels that Chrome decoded from the same files (see
 * {@link WebpFixtures}). The result must be identical to libwebp bit for bit.
 * <p>
 * Chrome 의 WebP 인코더가 만든 파일로 하는 {@link WebpDecoder} 시험(계획 P18-03; FEATURE_MATRIX IMG-05): 손실/무손실 영상, 알파 평면, 홀수 크기를
 * 같은 파일에서 Chrome 이 디코딩한 픽셀과 행 단위로 비교한다({@link WebpFixtures} 참조). 결과는 libwebp 와 비트 단위로 같아야 한다.
 */
public class WebpDecoderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebpDecoderTest() {
    }

    /**
     * Decodes every fixture whose name starts with a prefix and compares all rows with the reference.
     * <p>
     * 이름이 접두사로 시작하는 모든 자료를 디코딩해 모든 행을 기준과 비교한다.
     *
     * @param prefix name prefix / 이름 접두사
     * @throws IOException        if the fixtures cannot be read / 자료를 읽을 수 없을 때
     * @throws MediaException     if a fixture cannot be decoded / 자료를 디코딩할 수 없을 때
     */
    private static void compareAll(String prefix) throws IOException, MediaException {
        List<WebpFixtures.Fixture> all = WebpFixtures.load();
        int count = 0;
        StringBuilder failures = new StringBuilder();
        // 접두사가 맞는 자료마다 디코딩해 행 해시를 비교한다.
        for (int i = 0; i < all.size(); i++) {
            WebpFixtures.Fixture f = all.get(i);
            if (!f.name.startsWith(prefix)) {
                continue;
            }
            count++;
            DecodedImage img = new WebpDecoder().decode(f.data, DecodeLimits.DEFAULT);
            Check.equal(f.width, img.getWidth(), f.name + ": width");
            Check.equal(f.height, img.getHeight(), f.name + ": height");
            Check.equal(1, img.getFrames().size(), f.name + ": frame count");
            long[] got = WebpFixtures.rowHashes(img.getFrames().get(0), f.width, f.height);
            int bad = 0;
            int first = -1;
            // 해시가 다른 행을 센다.
            for (int y = 0; y < f.height; y++) {
                if (got[y] != f.rowHashes[y]) {
                    bad++;
                    if (first < 0) {
                        first = y;
                    }
                }
            }
            if (bad > 0) {
                failures.append(f.name).append(": ").append(bad).append(" of ").append(f.height).append(" rows differ, first row ").append(first).append('\n');
            }
        }
        Check.isTrue(count > 0, "fixtures with prefix " + prefix);
        if (failures.length() > 0) {
            Check.fail(failures.toString());
        }
    }

    /**
     * Lossy images (VP8 key frames: all prediction modes, segments, loop filter, odd sizes) equal the pixels that Chrome decodes.
     * <p>
     * 손실 영상(VP8 키 프레임: 모든 예측 모드, 세그먼트, 루프 필터, 홀수 크기)이 Chrome 이 디코딩한 픽셀과 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLossyMatchesChrome() throws Exception {
        compareAll("lossy_");
    }

    /**
     * Lossless images (VP8L) equal the pixels that Chrome decodes.
     * <p>
     * 무손실 영상(VP8L)이 Chrome 이 디코딩한 픽셀과 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLosslessMatchesChrome() throws Exception {
        compareAll("lossless_");
    }
}
