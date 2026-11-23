package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.KemarReference;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the HRTF data of the product (plan item P25-04): the resource is found on the class path and has the structure of the KEMAR
 * compact data set (14 elevations from -40 to 90 degrees, 368 directions, 128 taps at 44100 Hz) and the credit text of the authors;
 * every measured direction agrees with the original impulse responses that the test reads from the unchanged archive of the test
 * resources (the minimum phase kernel has the magnitude response of the original within 0.01 dB wherever the original is within 40 dB
 * of its peak, and the difference of the two delays is the interaural time difference that the cross-correlation of the original
 * finds); a damaged resource is refused with an <code>IOException</code>.
 * <p>
 * 제품의 HRTF 자료 시험(계획 P25-04): 자원이 클래스 경로에서 발견되고 KEMAR compact 자료의 구조(-40 ~ 90도의 고도 14개, 368 방향, 44100 Hz 128탭)와 저자의 출처 표기
 * 문장을 가진다. 측정한 모든 방향이 시험이 시험 자원의 바뀌지 않은 압축 파일에서 읽는 원본 임펄스 응답과 맞는다(최소 위상 커널의 크기 응답이 원본이 피크보다 40 dB 이내인
 * 곳에서 0.01 dB 안으로 원본과 같고, 두 지연의 차이는 원본의 상호 상관이 찾는 두 귀 사이의 시간차이다). 손상된 자원은 <code>IOException</code> 으로 거부한다.
 */
public class HrtfDatabaseTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public HrtfDatabaseTest() {
    }

    /**
     * Loads the database of the product.
     * <p>
     * 제품의 데이터베이스를 읽는다.
     *
     * @return the database / 데이터베이스
     */
    static HrtfDatabase db() {
        try {
            return HrtfDatabase.load();
        } catch (IOException e) {
            Check.fail("the HRTF resource cannot be loaded: " + e.getMessage());
            return null;
        }
    }

    /**
     * The structure of the data and its credit.
     * <p>
     * 자료의 구조와 출처 표기.
     */
    public void testStructureAndCredit() {
        HrtfDatabase db = db();
        Check.equal(44100L, (long) db.getSampleRate(), "sample rate");
        Check.equal(128L, (long) db.getTaps(), "taps");
        Check.equal(14L, (long) db.getElevationCount(), "elevations");
        Check.equal(368L, (long) db.getDirectionCount(), "directions");
        Check.equal(368L, (long) KemarReference.count(), "the original archive has the same number of measurements");
        Check.equal(-40L, (long) db.getElevation(0), "the lowest elevation");
        Check.equal(90L, (long) db.getElevation(13), "the highest elevation");
        // 고도는 10도 간격으로 오름차순이다.
        for (int e = 1; e < 14; e++) {
            Check.equal(10L, (long) (db.getElevation(e) - db.getElevation(e - 1)), "elevation step " + e);
            Check.equal(0L, (long) db.getAzimuth(e, 0), "every ring starts at azimuth 0");
        }
        Check.equal(37L, (long) db.getAzimuthCount(4), "the horizontal ring has 37 azimuths (0 to 180 in steps of 5)");
        Check.equal(180L, (long) db.getAzimuth(4, 36), "the horizontal ring ends at 180");
        Check.equal(1L, (long) db.getAzimuthCount(13), "the top has one direction");
        String credit = db.getCredit();
        Check.contains(credit, "Bill Gardner and Keith Martin", "the credit names the authors");
        Check.contains(credit, "MIT Media Lab", "the credit names the laboratory");
        Check.contains(credit, "KEMAR", "the credit names the data set");
        Check.contains(credit, "cited", "the credit states the condition of use");
        Check.contains(credit, "https://sound.media.mit.edu/resources/KEMAR.html", "the credit names the source");
    }

    /**
     * Every direction against the original: the magnitude response of the kernel matches the original at frequencies from 100 Hz to
     * 16 kHz wherever the original is within 40 dB of its largest magnitude, within 0.01 dB (the kernel is the minimum phase version of
     * the original, which has the same magnitude).
     * <p>
     * 모든 방향을 원본과 비교한다. 100 Hz ~ 16 kHz 의 주파수에서 원본이 가장 큰 크기보다 40 dB 이내인 곳마다 커널의 크기 응답이 원본과 0.01 dB 안으로 같다(커널은
     * 원본의 최소 위상 버전이며 크기가 같다).
     */
    public void testKernelsHaveTheMagnitudeOfTheOriginal() {
        HrtfDatabase db = db();
        double worst = 0;
        // 모든 고도, 방위각, 귀에 대해 크기 응답을 비교한다.
        for (int e = 0; e < db.getElevationCount(); e++) {
            for (int a = 0; a < db.getAzimuthCount(e); a++) {
                double[][] original = KemarReference.get(db.getElevation(e), db.getAzimuth(e, a));
                // 두 귀를 차례로 처리한다.
                for (int ear = 0; ear < 2; ear++) {
                    float[] k = db.getKernel(e, a, ear);
                    double[] kd = new double[k.length];
                    // 커널의 탭마다 처리한다.
                    for (int t = 0; t < k.length; t++) {
                        kd[t] = k[t];
                    }
                    double peak = -1000;
                    // 원본의 가장 큰 크기(100 Hz ~ 16 kHz 안)를 찾는다.
                    for (double f = 100; f <= 16000; f += 100) {
                        peak = Math.max(peak, KemarReference.magnitudeDb(original[ear], f, KemarReference.RATE));
                    }
                    // 100 Hz 간격의 주파수마다 원본과 커널의 크기를 비교한다.
                    for (double f = 100; f <= 16000; f += 100) {
                        double o = KemarReference.magnitudeDb(original[ear], f, KemarReference.RATE);
                        if (o < peak - 40) {
                            continue;
                        }
                        double d = Math.abs(KemarReference.magnitudeDb(kd, f, KemarReference.RATE) - o);
                        worst = Math.max(worst, d);
                        Check.isTrue(d < 0.01, "elevation " + db.getElevation(e) + " azimuth " + db.getAzimuth(e, a) + " ear " + ear + " at " + f + " Hz differs by " + d + " dB");
                    }
                }
            }
        }
        Check.isTrue(worst < 0.01, "the largest deviation is " + worst + " dB");
    }

    /**
     * The shape of the delays: they are not negative, the ear that is closer to the source (the right ear for the azimuths from 5 to
     * 175 degrees) has the smaller delay, the delays at the front and the back are equal for both ears, the interaural delay grows from
     * the front to about 90 degrees and has more than 20 samples (0.45 ms) there. The delay is only the pure delay part of the
     * response; the total interaural time difference of the rendering also contains the delay of the minimum phase kernels and is
     * compared with the original in {@link HrtfSetTest}.
     * <p>
     * 지연의 모양: 음수가 아니고, 음원에 가까운 귀(방위각 5 ~ 175도에서는 오른쪽 귀)의 지연이 더 작고, 정면과 뒤에서는 두 귀의 지연이 같고, 두 귀의 지연 차이는 정면에서
     * 90도 근처까지 커지며 거기서 20 샘플(0.45 ms) 이상이다. 지연은 응답 중 순수 지연 부분일 뿐이다. 렌더링의 전체 두 귀 시간차에는 최소 위상 커널의 지연도 들어 있고
     * 그것은 {@link HrtfSetTest} 에서 원본과 비교한다.
     */
    public void testDelaysAreOrderedLikeTheHead() {
        HrtfDatabase db = db();
        int e = 4;
        Check.equal(0L, (long) db.getElevation(e), "ring 4 is the horizontal ring");
        double previous = -1;
        // 수평 고리의 방위각마다 지연의 크기와 순서를 확인한다.
        for (int a = 0; a < db.getAzimuthCount(e); a++) {
            int az = db.getAzimuth(e, a);
            Check.isTrue(db.getDelay(e, a, 0) >= 0 && db.getDelay(e, a, 1) >= 0, "no negative delay at " + az);
            double itd = db.getDelay(e, a, 0) - db.getDelay(e, a, 1);
            if (az >= 5 && az <= 175) {
                Check.isTrue(itd > 0, "the right ear is the closer one at azimuth " + az);
            }
            if (az <= 90) {
                Check.isTrue(itd >= previous - 0.5, "the interaural delay does not fall before 90 degrees: azimuth " + az);
                previous = itd;
            }
        }
        Check.near(db.getDelay(e, 0, 0), db.getDelay(e, 0, 1), 1e-6, "the front is symmetric");
        Check.near(db.getDelay(e, 36, 0), db.getDelay(e, 36, 1), 1e-6, "the back is symmetric");
        Check.isTrue(db.getDelay(e, 18, 0) - db.getDelay(e, 18, 1) > 20, "the interaural delay at 90 degrees is more than 20 samples (0.45 ms)");
    }

    /**
     * A damaged resource is refused with an <code>IOException</code>: bad magic, an unknown version, a truncated file, and a value that
     * is not finite.
     * <p>
     * 손상된 자원은 <code>IOException</code> 으로 거부한다: 잘못된 매직, 모르는 버전, 잘린 파일, 유한하지 않은 값.
     */
    public void testDamagedResourcesAreRefused() throws Exception {
        java.io.InputStream in = HrtfDatabase.class.getResourceAsStream(HrtfDatabase.RESOURCE);
        Check.notNull(in, "the resource is on the class path");
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        // 자원 전체를 메모리로 읽는다.
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        in.close();
        final byte[] good = bos.toByteArray();
        Check.equal(368L, (long) HrtfDatabase.read(new ByteArrayInputStream(good)).getDirectionCount(), "the unchanged bytes load");
        byte[] badMagic = good.clone();
        badMagic[0] = 'X';
        expectFailure(badMagic, "bad magic");
        byte[] badVersion = good.clone();
        badVersion[11] = 9;
        expectFailure(badVersion, "unknown version");
        byte[] truncated = new byte[good.length / 2];
        System.arraycopy(good, 0, truncated, 0, truncated.length);
        expectFailure(truncated, "truncated file");
        byte[] nan = good.clone();
        // 첫 측정의 첫 지연(머리 뒤 첫 float)을 NaN 으로 바꾼다. 머리의 크기는 고도와 방위각 목록의 길이에 따른다.
        int header = 8 + 4 * 4;
        int elevations = (nan[header - 1] & 0xFF);
        int pos = header;
        // 고도 목록(각도, 개수, 방위각들)을 건너뛴다.
        for (int e = 0; e < elevations; e++) {
            int count = ((nan[pos + 2] & 0xFF) << 8) | (nan[pos + 3] & 0xFF);
            pos += 4 + 2 * count;
        }
        nan[pos] = 0x7F;
        nan[pos + 1] = (byte) 0xC0;
        nan[pos + 2] = 0;
        nan[pos + 3] = 0;
        expectFailure(nan, "a NaN value");
    }

    /**
     * Checks that reading the bytes fails with an <code>IOException</code>.
     * <p>
     * 바이트를 읽을 때 <code>IOException</code> 이 나는지 검사한다.
     *
     * @param bytes the bytes / 바이트
     * @param what  the description / 설명
     */
    private static void expectFailure(byte[] bytes, String what) {
        try {
            HrtfDatabase.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return;
        }
        Check.fail("a damaged resource (" + what + ") was accepted");
    }
}
