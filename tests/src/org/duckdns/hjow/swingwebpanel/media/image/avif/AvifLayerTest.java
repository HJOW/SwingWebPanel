package org.duckdns.hjow.swingwebpanel.media.image.avif;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of layered AV1 images (plan item P18-03; FEATURE_MATRIX IMG-09): the streams of <code>/media/avif/layers</code> hold two shown key frames
 * per temporal unit, one in the spatial layer 0 and one in the layer 1, and two operating points (0: both layers, 1: the layer 0). The reference
 * pictures of every layer come from libdav1d (see <code>layers/index.txt</code>). The properties <code>a1op</code> (operating point) and
 * <code>lsel</code> (spatial layer to render) of the AVIF files choose which picture the decoder returns. The streams were assembled from key
 * frames of an encoder (no available encoder makes spatial layers), so the inter-layer prediction is not covered.
 * <p>
 * 계층이 있는 AV1 이미지(계획 P18-03; FEATURE_MATRIX IMG-09)의 시험: <code>/media/avif/layers</code> 의 스트림은 시간 단위마다 공간 계층 0 과 1 에 각각
 * 표시되는 키 프레임 하나씩과 두 동작 지점(0: 두 계층, 1: 계층 0)을 가진다. 계층별 기준 화면은 libdav1d 에서 얻었다(<code>layers/index.txt</code>).
 * AVIF 파일의 속성 <code>a1op</code>(동작 지점)와 <code>lsel</code>(그릴 공간 계층)이 디코더가 돌려줄 화면을 정한다. 스트림은 인코더의 키
 * 프레임으로 조립했으므로(공간 계층을 만드는 인코더가 없다) 계층 간 예측은 다루지 않는다.
 */
public class AvifLayerTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AvifLayerTest() {
    }

    /**
     * Reads a line of the index of the layer fixtures: the md5 of the pictures of the layer 0 (index 3) and the layer 1 (index 4).
     * <p>
     * 계층 시험 자료 색인의 줄을 읽는다: 계층 0 화면의 md5(3번)와 계층 1 화면의 md5(4번).
     *
     * @param name the name of the stream / 스트림 이름
     * @return the fields of the line / 줄의 필드
     * @throws Exception on a failure / 실패 시
     */
    private static String[] index(String name) throws Exception {
        // 색인에서 이름이 같은 줄을 찾는다.
        for (String line : AvifTestKit.indexLines("/media/avif/layers/index.txt")) {
            String[] f = line.split("\\|");
            if (f[0].equals(name)) {
                return f;
            }
        }
        throw new IllegalStateException(name);
    }

    /**
     * The pictures of the operating points and layers equal the pictures of libdav1d, and a layer that the operating point does not contain or
     * an operating point that does not exist is refused.
     * <p>
     * 동작 지점과 계층별 화면이 libdav1d 의 화면과 같으며, 동작 지점에 들어 있지 않은 계층이나 없는 동작 지점은 거부된다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testOperatingPointsAndLayersMatchDav1d() throws Exception {
        final byte[] data = AvifTestKit.resource("/media/avif/layers/layers_1tu.obu");
        String[] f = index("layers_1tu");
        DecodeLimits lim = DecodeLimits.DEFAULT;
        Check.equal(f[4], AvifTestKit.md5(Av1Decoder.decodeLayer(data, 0, data.length, lim, null, 0, -1)), "op 0, highest layer is the layer 1");
        Check.equal(f[3], AvifTestKit.md5(Av1Decoder.decodeLayer(data, 0, data.length, lim, null, 0, 0)), "op 0, layer 0");
        Check.equal(f[4], AvifTestKit.md5(Av1Decoder.decodeLayer(data, 0, data.length, lim, null, 0, 1)), "op 0, layer 1");
        Check.equal(f[3], AvifTestKit.md5(Av1Decoder.decodeLayer(data, 0, data.length, lim, null, 1, -1)), "op 1, highest layer is the layer 0");
        Check.equal(f[3], AvifTestKit.md5(Av1Decoder.decodeLayer(data, 0, data.length, lim, null, 1, 0)), "op 1, layer 0");
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "layer 1 is not in the operating point 1", new Block() {
            public void run() throws Exception {
                Av1Decoder.decodeLayer(data, 0, data.length, DecodeLimits.DEFAULT, null, 1, 1);
            }
        });
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "layer 2 does not exist", new Block() {
            public void run() throws Exception {
                Av1Decoder.decodeLayer(data, 0, data.length, DecodeLimits.DEFAULT, null, 0, 2);
            }
        });
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "operating point 2 does not exist", new Block() {
            public void run() throws Exception {
                Av1Decoder.decodeLayer(data, 0, data.length, DecodeLimits.DEFAULT, null, 2, -1);
            }
        });
    }

    /**
     * A stream with two temporal units shows all layers of the operating point 0 in the order of decoding (the pictures of libdav1d with all
     * layers enabled).
     * <p>
     * 시간 단위가 둘인 스트림은 동작 지점 0 의 모든 계층의 화면을 디코딩 순서대로 보여 준다(모든 계층을 켠 libdav1d 의 화면).
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testAllLayersOfASequence() throws Exception {
        byte[] data = AvifTestKit.resource("/media/avif/layers/layers_2tu.obu");
        String[] f = index("layers_2tu");
        String[] l0 = f[3].split(",");
        String[] l1 = f[4].split(",");
        List<Av1Picture> pics = Av1Decoder.decodeAll(data, 0, data.length, DecodeLimits.DEFAULT, null);
        Check.equal(4, pics.size(), "two layers in each of two temporal units");
        // 시간 단위마다 계층 0, 계층 1 의 순서로 화면이 나온다.
        for (int i = 0; i < 2; i++) {
            Check.equal(l0[i], AvifTestKit.md5(pics.get(2 * i)), "layer 0 of the temporal unit " + i);
            Check.equal(l1[i], AvifTestKit.md5(pics.get(2 * i + 1)), "layer 1 of the temporal unit " + i);
        }
    }

    /**
     * Decodes an AVIF item of the layer fixtures.
     * <p>
     * 계층 시험 자료의 AVIF 항목을 디코딩한다.
     *
     * @param name the name without extension / 확장자 없는 이름
     * @return the image / 이미지
     * @throws Exception on a failure / 실패 시
     */
    private static DecodedImage item(String name) throws Exception {
        return new AvifDecoder().decode(AvifTestKit.resource("/media/avif/layers/" + name + ".avif"), DecodeLimits.DEFAULT);
    }

    /**
     * The pixels of the picture of a layer of the stream (converted like a still image).
     * <p>
     * 스트림의 계층 하나의 화면의 픽셀(정지 이미지처럼 변환).
     *
     * @param operatingPoint the operating point / 동작 지점
     * @param layer          the layer / 계층
     * @return the pixels / 픽셀
     * @throws Exception on a failure / 실패 시
     */
    private static int[] pixels(int operatingPoint, int layer) throws Exception {
        byte[] data = AvifTestKit.resource("/media/avif/layers/layers_1tu.obu");
        return AvifColor.toArgb(Av1Decoder.decodeLayer(data, 0, data.length, DecodeLimits.DEFAULT, null, operatingPoint, layer), null, false);
    }

    /**
     * The properties <code>a1op</code> and <code>lsel</code> of an AVIF item choose the picture: without them the highest layer of the operating
     * point 0, with <code>a1op</code> the highest layer of that operating point, with <code>lsel</code> the layer (0xFFFF: the highest).
     * <p>
     * AVIF 항목의 속성 <code>a1op</code>, <code>lsel</code> 이 화면을 고른다: 없으면 동작 지점 0 의 가장 높은 계층, <code>a1op</code> 가 있으면 그 동작
     * 지점의 가장 높은 계층, <code>lsel</code> 이 있으면 그 계층(0xFFFF 는 가장 높은 계층).
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testItemPropertiesChooseThePicture() throws Exception {
        int[] layer0 = pixels(0, 0);
        int[] layer1 = pixels(0, 1);
        Check.isFalse(java.util.Arrays.equals(layer0, layer1), "the two layers show different pictures");
        Check.isTrue(java.util.Arrays.equals(layer1, item("item_default").getFrames().get(0).getArgb()), "no property: the highest layer of op 0");
        Check.isTrue(java.util.Arrays.equals(layer1, item("item_op0").getFrames().get(0).getArgb()), "a1op 0");
        Check.isTrue(java.util.Arrays.equals(layer0, item("item_op1").getFrames().get(0).getArgb()), "a1op 1: the only layer of op 1");
        Check.isTrue(java.util.Arrays.equals(layer0, item("item_lsel0").getFrames().get(0).getArgb()), "lsel 0");
        Check.isTrue(java.util.Arrays.equals(layer1, item("item_lsel1").getFrames().get(0).getArgb()), "lsel 1");
        Check.isTrue(java.util.Arrays.equals(layer1, item("item_lselall").getFrames().get(0).getArgb()), "lsel 0xFFFF: the highest layer");
        Check.isTrue(java.util.Arrays.equals(layer0, item("item_op1_lsel0").getFrames().get(0).getArgb()), "a1op 1 and lsel 0");
        Check.equal(96, item("item_lsel0").getWidth(), "width");
        Check.equal(64, item("item_lsel0").getHeight(), "height");
    }

    /**
     * Invalid selections fail with CORRUPT: a layer that the stream or the operating point does not have, a layer id that is no spatial layer,
     * an operating point that does not exist.
     * <p>
     * 잘못된 선택은 CORRUPT 로 실패한다: 스트림이나 동작 지점에 없는 계층, 공간 계층이 아닌 계층 번호, 없는 동작 지점.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testInvalidSelections() throws Exception {
        String[] names = { "item_lsel2", "item_lsel9", "item_op5", "item_op1_lsel1" };
        // 항목마다 디코딩이 CORRUPT 로 실패하는지 확인한다.
        for (final String name : names) {
            AvifTestKit.fails(MediaException.Kind.CORRUPT, name, new Block() {
                public void run() throws Exception {
                    item(name);
                }
            });
        }
    }

    /**
     * The picture of an animated AVIF sample is the picture of the highest spatial layer of its temporal unit.
     * <p>
     * 애니메이션 AVIF 샘플의 화면은 그 시간 단위의 가장 높은 공간 계층의 화면이다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testLayeredImageSequence() throws Exception {
        DecodedImage img = new AvifDecoder().decode(AvifTestKit.resource("/media/avif/layers/avis_layers.avif"), DecodeLimits.DEFAULT);
        Check.equal(2, img.getFrames().size(), "two samples");
        byte[] raw = AvifTestKit.resource("/media/avif/layers/layers_2tu.obu");
        List<Av1Picture> pics = Av1Decoder.decodeAll(raw, 0, raw.length, DecodeLimits.DEFAULT, null);
        // 샘플마다 그 시간 단위의 마지막(가장 높은 계층) 화면과 같은지 본다.
        for (int i = 0; i < 2; i++) {
            int[] expected = AvifColor.toArgb(pics.get(2 * i + 1), null, false);
            Check.isTrue(java.util.Arrays.equals(expected, img.getFrames().get(i).getArgb()), "frame " + i + " is the highest layer");
        }
    }
}
