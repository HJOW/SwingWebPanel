package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the geometry of the spatialization (Web Audio API 6.2, 6.4, 6.5): the azimuth and the elevation against closed forms (for
 * the default listener, the azimuth of the horizontal projection is <code>atan2(x, -z)</code> and the elevation is
 * <code>asin(y / r)</code>), rotated and moved listeners, the degenerate cases, the three distance models against the formulas of the
 * specification written out in the test (with their special cases), and the sound cone (inside, between, outside, no cone).
 * <p>
 * 공간화의 기하(Web Audio API 6.2, 6.4, 6.5) 시험: 방위각과 고도를 닫힌 식과 비교(기본 청취자에서 수평 투영의 방위각은 <code>atan2(x, -z)</code>, 고도는
 * <code>asin(y / r)</code>), 회전하고 이동한 청취자, 퇴화한 경우, 세 거리 모델을 시험에 적은 규격의 식(특수한 경우 포함)과 비교, 음향 원뿔(안, 사이, 밖, 원뿔 없음).
 */
public class SpatialTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public SpatialTest() {
    }

    /**
     * Computes the azimuth and the elevation for the default listener (at the origin, looking along -z, y up).
     * <p>
     * 기본 청취자(원점, -z 를 봄, y 가 위)에 대한 방위각과 고도를 계산한다.
     *
     * @param x the x of the source / 음원 x
     * @param y the y of the source / 음원 y
     * @param z the z of the source / 음원 z
     * @return the azimuth and the elevation / 방위각과 고도
     */
    private static double[] defaultListener(double x, double y, double z) {
        double[] out = new double[2];
        Spatial.azimuthElevation(x, y, z, 0, 0, 0, 0, 0, -1, 0, 1, 0, out);
        return out;
    }

    /**
     * Returns the angle difference in degrees, wrapped to -180..180.
     * <p>
     * 각도 차이(도)를 -180 ~ 180 으로 접어 반환한다.
     *
     * @param a the first angle / 첫째 각
     * @param b the second angle / 둘째 각
     * @return the difference / 차이
     */
    private static double diff(double a, double b) {
        double d = (a - b) % 360.0;
        if (d > 180) {
            d -= 360;
        }
        if (d < -180) {
            d += 360;
        }
        return d;
    }

    /**
     * Sources on the horizontal circle: the azimuth is the angle from the front, positive to the right, at every distance, and the
     * elevation is 0.
     * <p>
     * 수평 원 위의 음원: 방위각은 정면에서의 각이고 오른쪽이 양수이며 모든 거리에서 같다. 고도는 0 이다.
     */
    public void testAzimuthOnTheHorizontalCircle() {
        double[] angles = {0, 15, 30, 45, 90, 120, 135, 170, -15, -45, -90, -135, -170};
        double[] radii = {0.5, 1, 7};
        // 각도와 거리마다 닫힌 식과 비교한다.
        for (int i = 0; i < angles.length; i++) {
            for (int j = 0; j < radii.length; j++) {
                double t = Math.toRadians(angles[i]);
                double[] ae = defaultListener(radii[j] * Math.sin(t), 0, -radii[j] * Math.cos(t));
                Check.near(0.0, diff(angles[i], ae[0]), 1e-9, "azimuth for " + angles[i] + " at " + radii[j]);
                Check.near(0.0, ae[1], 1e-9, "elevation for " + angles[i]);
            }
        }
        double[] behind = defaultListener(0, 0, 3);
        Check.near(180.0, Math.abs(behind[0]), 1e-9, "directly behind is +-180");
    }

    /**
     * Sources above and below: the elevation is <code>asin(y / r)</code> and the azimuth stays that of the horizontal projection; a
     * source straight above or below has the azimuth 0 and the elevation +-90; a general direction follows both closed forms.
     * <p>
     * 위와 아래의 음원: 고도는 <code>asin(y / r)</code> 이고 방위각은 수평 투영의 방위각 그대로이다. 바로 위나 아래의 음원은 방위각 0, 고도 +-90 이다. 일반적인 방향은
     * 두 닫힌 식을 따른다.
     */
    public void testElevation() {
        double[] elevations = {-80, -45, -10, 0, 20, 60, 85};
        // 고도마다 정면 방향 음원의 닫힌 식과 비교한다.
        for (int i = 0; i < elevations.length; i++) {
            double e = Math.toRadians(elevations[i]);
            double[] ae = defaultListener(0, Math.sin(e) * 2, -Math.cos(e) * 2);
            Check.near(elevations[i], ae[1], 1e-9, "elevation " + elevations[i]);
            Check.near(0.0, ae[0], 1e-9, "azimuth in front at " + elevations[i]);
        }
        double[] up = defaultListener(0, 5, 0);
        Check.near(0.0, up[0], 0.0, "straight above: azimuth 0");
        Check.near(90.0, up[1], 1e-9, "straight above: elevation 90");
        Check.near(-90.0, defaultListener(0, -5, 0)[1], 1e-9, "straight below: elevation -90");
        double[] g = defaultListener(3, 4, -5);
        double r = Math.sqrt(9 + 16 + 25);
        Check.near(Math.toDegrees(Math.atan2(3, 5)), g[0], 1e-9, "general azimuth");
        Check.near(Math.toDegrees(Math.asin(4 / r)), g[1], 1e-9, "general elevation");
        double[] rear = defaultListener(-2, 1, 3);
        Check.near(Math.toDegrees(Math.atan2(-2, -3)), rear[0], 1e-9, "azimuth behind and to the left");
        Check.near(Math.toDegrees(Math.asin(1 / Math.sqrt(14))), rear[1], 1e-9, "elevation behind");
    }

    /**
     * A listener that is moved and rotated: the positions are relative to the listener and the axes are its own axes. With the forward
     * vector +x (and y up) the right side is +z; with the forward vector -z and the up vector +x the right side is -y and the
     * elevation measures from +x.
     * <p>
     * 이동하고 회전한 청취자: 위치는 청취자에 상대적이고 축은 청취자 자신의 축이다. forward 가 +x(y 가 위)이면 오른쪽은 +z 이다. forward 가 -z 이고 up 이 +x 이면
     * 오른쪽은 -y 이고 고도는 +x 에서 잰다.
     */
    public void testMovedAndRotatedListener() {
        double[] ae = new double[2];
        Spatial.azimuthElevation(6, 1, 0.5, 5, 1, 0.5, 1, 0, 0, 0, 1, 0, ae);
        Check.near(0.0, ae[0], 1e-9, "a listener looking along +x sees +x in front");
        Spatial.azimuthElevation(0, 0, -1, 0, 0, 0, 1, 0, 0, 0, 1, 0, ae);
        Check.near(-90.0, ae[0], 1e-9, "with forward +x the -z side is the left");
        Spatial.azimuthElevation(0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, ae);
        Check.near(90.0, ae[0], 1e-9, "with forward +x the +z side is the right");
        Spatial.azimuthElevation(0, -1, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, ae);
        Check.near(90.0, ae[0], 1e-9, "forward -z with up +x: -y is the right");
        Spatial.azimuthElevation(1, 0, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, ae);
        Check.near(90.0, ae[1], 1e-9, "forward -z with up +x: +x is above");
        Spatial.azimuthElevation(5, 0, -1, 5, 0, 0, 0, 0, -3, 0, 2, 0, ae);
        Check.near(0.0, ae[0], 1e-9, "the vectors need not have unit length");
    }

    /**
     * The degenerate cases give azimuth 0 and elevation 0: a source at the listener, and a listener whose forward and up vectors are
     * parallel (or zero), for which the right side is undefined.
     * <p>
     * 퇴화한 경우는 방위각 0, 고도 0 이다: 청취자의 위치에 있는 음원, forward 와 up 벡터가 나란한(또는 0 인) 청취자는 오른쪽을 정할 수 없다.
     */
    public void testDegenerateCases() {
        double[] ae = new double[2];
        Spatial.azimuthElevation(2, 3, 4, 2, 3, 4, 0, 0, -1, 0, 1, 0, ae);
        Check.near(0.0, ae[0], 0.0, "source at the listener: azimuth");
        Check.near(0.0, ae[1], 0.0, "source at the listener: elevation");
        Spatial.azimuthElevation(1, 2, 3, 0, 0, 0, 0, 1, 0, 0, 2, 0, ae);
        Check.near(0.0, ae[0], 0.0, "parallel forward and up: azimuth");
        Check.near(0.0, ae[1], 0.0, "parallel forward and up: elevation");
        Spatial.azimuthElevation(1, 2, 3, 0, 0, 0, 0, 0, 0, 0, 1, 0, ae);
        Check.near(0.0, ae[0], 0.0, "zero forward vector");
        Check.near(5.0, Spatial.distance(3, 4, 0, 0, 0, 0), 1e-12, "distance");
        Check.near(Math.sqrt(3.0), Spatial.distance(1, 1, 1, 0, 0, 0), 1e-12, "diagonal distance");
    }

    /**
     * The distance models against the formulas of the specification written out here, for distances below, between and above the
     * reference and the maximum distance, and the special cases: a reference distance of 0 gives 0 for the inverse and exponential model;
     * equal reference and maximum distance give <code>1 - f</code> for the linear model; the rolloff factor of the linear model is
     * clamped to 0..1.
     * <p>
     * 거리 모델을 여기에 적은 규격의 식과 비교한다. 거리는 기준 거리와 최대 거리의 아래, 사이, 위로 하고, 특수한 경우도 본다. 기준 거리가 0 이면 역수와 지수 모델은
     * 0 이다. 기준 거리와 최대 거리가 같으면 선형 모델은 <code>1 - f</code> 이다. 선형 모델의 롤오프 계수는 0..1 로 제한한다.
     */
    public void testDistanceModels() {
        double[] distances = {0, 0.5, 1, 2, 5, 10, 50, 1000, 20000};
        // 거리마다 세 모델을 규격의 식과 비교한다.
        for (int i = 0; i < distances.length; i++) {
            double d = distances[i];
            double ref = 1;
            double max = 10000;
            double f = 0.7;
            double dref = Math.min(ref, max);
            double dmax = Math.max(ref, max);
            double linear = 1 - f * (Math.max(Math.min(d, dmax), dref) - dref) / (dmax - dref);
            Check.near(linear, Spatial.distanceGain(DistanceModelType.LINEAR, d, ref, max, f), 1e-12, "linear at " + d);
            double inverse = ref / (ref + f * (Math.max(d, ref) - ref));
            Check.near(inverse, Spatial.distanceGain(DistanceModelType.INVERSE, d, ref, max, f), 1e-12, "inverse at " + d);
            double exponential = Math.pow(Math.max(d, ref) / ref, -f);
            Check.near(exponential, Spatial.distanceGain(DistanceModelType.EXPONENTIAL, d, ref, max, f), 1e-12, "exponential at " + d);
        }
        Check.near(0.0, Spatial.distanceGain(DistanceModelType.INVERSE, 5, 0, 100, 1), 0.0, "inverse with a reference distance of 0");
        Check.near(0.0, Spatial.distanceGain(DistanceModelType.EXPONENTIAL, 5, 0, 100, 1), 0.0, "exponential with a reference distance of 0");
        Check.near(1 - 0.25, Spatial.distanceGain(DistanceModelType.LINEAR, 7, 3, 3, 0.25), 1e-12, "linear with equal reference and maximum distance");
        Check.near(1 - 1.0 * 0.5, Spatial.distanceGain(DistanceModelType.LINEAR, 5.5, 1, 10, 5.0), 1e-12, "the linear rolloff factor is clamped to 1");
        Check.near(0.5, Spatial.distanceGain(DistanceModelType.INVERSE, 3, 1, 100, 0.5), 1e-12, "inverse value: 1 / (1 + 0.5 * 2)");
        Check.near(1.0, Spatial.distanceGain(DistanceModelType.EXPONENTIAL, 0.2, 1, 100, 3), 1e-12, "closer than the reference: no attenuation");
        Check.near(1.0, Spatial.distanceGain(DistanceModelType.INVERSE, 3, 1, 100, 0), 1e-12, "rolloff factor 0: no attenuation");
        Check.near(Math.pow(4, -2), Spatial.distanceGain(DistanceModelType.EXPONENTIAL, 8, 2, 100, 2), 1e-12, "exponential value");
    }

    /**
     * The sound cone with the source at the origin pointing along +z and the listener at the angle <code>t</code> from that direction:
     * 1 inside half of the inner angle, the outer gain beyond half of the outer angle, a linear change in between; no cone (the angles
     * are both 360, or the orientation is zero) gives 1.
     * <p>
     * 원점에서 +z 를 향한 음원과 그 방향에서 각 <code>t</code> 에 있는 청취자의 음향 원뿔: 안쪽 각의 절반 안에서는 1, 바깥 각의 절반 밖에서는 바깥 게인, 그 사이는
     * 선형으로 바뀐다. 원뿔이 없으면(두 각이 모두 360 이거나 방향이 0) 1 이다.
     */
    public void testCone() {
        double inner = 60;
        double outer = 180;
        double outerGain = 0.2;
        double[] angles = {0, 20, 30, 45, 60, 89, 90, 120, 180};
        // 각도마다 규격의 식과 비교한다.
        for (int i = 0; i < angles.length; i++) {
            double t = Math.toRadians(angles[i]);
            double g = Spatial.coneGain(0, 0, 1, 0, 0, 0, 5 * Math.sin(t), 0, 5 * Math.cos(t), inner, outer, outerGain);
            double expected;
            if (angles[i] <= inner / 2) {
                expected = 1;
            } else if (angles[i] >= outer / 2) {
                expected = outerGain;
            } else {
                double x = (angles[i] - inner / 2) / (outer / 2 - inner / 2);
                expected = (1 - x) + outerGain * x;
            }
            Check.near(expected, g, 1e-9, "cone gain at " + angles[i] + " degrees");
        }
        Check.near(1.0, Spatial.coneGain(0, 0, 0, 0, 0, 0, 1, 2, 3, 30, 90, 0.1), 0.0, "a zero orientation gives no cone");
        Check.near(1.0, Spatial.coneGain(0, 0, 1, 0, 0, 0, 0, 0, -5, 360, 360, 0.1), 0.0, "both angles of 360 give no cone");
        Check.near(1.0, Spatial.coneGain(0, 0, 1, 1, 1, 1, 1, 1, 1, 30, 90, 0.3), 0.0, "a listener at the source");
        Check.near(0.3, Spatial.coneGain(0, 0, 1, 0, 0, 0, 0, 0, -5, 30, 90, 0.3), 1e-12, "behind the source: the outer gain");
    }
}
