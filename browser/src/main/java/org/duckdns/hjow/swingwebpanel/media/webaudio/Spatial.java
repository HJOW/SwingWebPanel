package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The geometry of the spatialization (Web Audio API 6.2, 6.4 and 6.5): the azimuth and the elevation of a source as the listener
 * perceives them, the distance, the distance gain of the three distance models, and the gain of the sound cone. The methods are pure
 * functions in double precision; the coordinate system is right-handed (the default listener looks along -z with +y up, so +x is to
 * its right).
 * <p>
 * 공간화의 기하(Web Audio API 6.2, 6.4, 6.5): 청취자가 지각하는 음원의 방위각과 고도, 거리, 세 거리 모델의 거리 게인, 음향 원뿔의 게인. 메서드는 배정밀도의 순수
 * 함수이다. 좌표계는 오른손 좌표계이다(기본 청취자는 +y 가 위인 채로 -z 방향을 보므로 +x 가 오른쪽이다).
 */
public final class Spatial {

    /**
     * Private constructor: static functions only.
     * <p>
     * 비공개 생성자: 정적 함수만 있다.
     */
    private Spatial() {
    }

    /**
     * Computes the azimuth and the elevation of a source for a listener with the algorithm of the specification. The azimuth is in
     * degrees from -180 to 180 (0 in front, 90 to the right, -90 to the left, 180 behind), the elevation in degrees from -90 to 90. If
     * the source is at the listener, or the forward and up vectors of the listener are parallel (the right side is undefined), both are 0.
     * <p>
     * 규격의 알고리즘으로 청취자에 대한 음원의 방위각과 고도를 계산한다. 방위각은 -180 ~ 180도(0 정면, 90 오른쪽, -90 왼쪽, 180 뒤), 고도는 -90 ~ 90도이다. 음원이
     * 청취자의 위치에 있거나 청취자의 forward 와 up 벡터가 나란하면(오른쪽을 정할 수 없다) 둘 다 0 이다.
     *
     * @param px  the x of the source position / 음원 위치 x
     * @param py  the y of the source position / 음원 위치 y
     * @param pz  the z of the source position / 음원 위치 z
     * @param lx  the x of the listener position / 청취자 위치 x
     * @param ly  the y of the listener position / 청취자 위치 y
     * @param lz  the z of the listener position / 청취자 위치 z
     * @param fx  the x of the listener forward vector / 청취자 forward 벡터 x
     * @param fy  the y of the listener forward vector / 청취자 forward 벡터 y
     * @param fz  the z of the listener forward vector / 청취자 forward 벡터 z
     * @param ux  the x of the listener up vector / 청취자 up 벡터 x
     * @param uy  the y of the listener up vector / 청취자 up 벡터 y
     * @param uz  the z of the listener up vector / 청취자 up 벡터 z
     * @param out an array of two values that receives the azimuth and the elevation / 방위각과 고도를 받는 값 두 개의 배열
     */
    public static void azimuthElevation(double px, double py, double pz, double lx, double ly, double lz, double fx, double fy, double fz, double ux,
            double uy, double uz, double[] out) {
        double sx = px - lx;
        double sy = py - ly;
        double sz = pz - lz;
        double sm = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sm == 0.0 || Double.isNaN(sm)) {
            out[0] = 0.0;
            out[1] = 0.0;
            return;
        }
        sx /= sm;
        sy /= sm;
        sz /= sm;
        // 청취자의 오른쪽 = forward x up.
        double rx = fy * uz - fz * uy;
        double ry = fz * ux - fx * uz;
        double rz = fx * uy - fy * ux;
        double rm = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rm == 0.0 || Double.isNaN(rm)) {
            out[0] = 0.0;
            out[1] = 0.0;
            return;
        }
        rx /= rm;
        ry /= rm;
        rz /= rm;
        double fm = Math.sqrt(fx * fx + fy * fy + fz * fz);
        double nfx = fx / fm;
        double nfy = fy / fm;
        double nfz = fz / fm;
        // 오른쪽과 forward 에 직교하는 단위 벡터(청취자 기준의 위쪽).
        double upx = ry * nfz - rz * nfy;
        double upy = rz * nfx - rx * nfz;
        double upz = rx * nfy - ry * nfx;
        double upProjection = sx * upx + sy * upy + sz * upz;
        double qx = sx - upx * upProjection;
        double qy = sy - upy * upProjection;
        double qz = sz - upz * upProjection;
        double qm = Math.sqrt(qx * qx + qy * qy + qz * qz);
        double azimuth;
        if (qm == 0.0) {
            // 음원이 청취자의 바로 위나 아래이면 방위각을 정할 수 없으므로 0 으로 한다.
            azimuth = 0.0;
        } else {
            qx /= qm;
            qy /= qm;
            qz /= qm;
            double cosRight = Math.max(-1.0, Math.min(1.0, qx * rx + qy * ry + qz * rz));
            azimuth = 180.0 * Math.acos(cosRight) / Math.PI;
            double frontBack = qx * nfx + qy * nfy + qz * nfz;
            if (frontBack < 0) {
                azimuth = 360.0 - azimuth;
            }
            // 방위각을 오른쪽 기준에서 정면 기준으로 바꾼다.
            if (azimuth >= 0 && azimuth <= 270) {
                azimuth = 90.0 - azimuth;
            } else {
                azimuth = 450.0 - azimuth;
            }
        }
        double cosUp = Math.max(-1.0, Math.min(1.0, sx * upx + sy * upy + sz * upz));
        double elevation = 90.0 - 180.0 * Math.acos(cosUp) / Math.PI;
        if (elevation > 90) {
            elevation = 180 - elevation;
        } else if (elevation < -90) {
            elevation = -180 - elevation;
        }
        out[0] = azimuth;
        out[1] = elevation;
    }

    /**
     * Returns the distance between a source and the listener.
     * <p>
     * 음원과 청취자 사이의 거리를 반환한다.
     *
     * @param px the x of the source position / 음원 위치 x
     * @param py the y of the source position / 음원 위치 y
     * @param pz the z of the source position / 음원 위치 z
     * @param lx the x of the listener position / 청취자 위치 x
     * @param ly the y of the listener position / 청취자 위치 y
     * @param lz the z of the listener position / 청취자 위치 z
     * @return the distance / 거리
     */
    public static double distance(double px, double py, double pz, double lx, double ly, double lz) {
        double dx = px - lx;
        double dy = py - ly;
        double dz = pz - lz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Returns the gain of a distance model for a distance. The rolloff factor is clamped to the nominal range of the model (0 to 1 for
     * the linear model, 0 or more for the others).
     * <p>
     * 거리에 대한 거리 모델의 게인을 반환한다. 롤오프 계수는 모델의 명목 범위로 제한한다(선형 모델은 0 ~ 1, 나머지는 0 이상).
     *
     * @param model    the model / 모델
     * @param d        the distance / 거리
     * @param ref      the reference distance (<code>refDistance</code>) / 기준 거리
     * @param max      the largest distance (<code>maxDistance</code>) / 최대 거리
     * @param rolloff  the rolloff factor / 롤오프 계수
     * @return the gain / 게인
     */
    public static double distanceGain(DistanceModelType model, double d, double ref, double max, double rolloff) {
        if (model == DistanceModelType.LINEAR) {
            double f = Math.max(0.0, Math.min(1.0, rolloff));
            double dref = Math.min(ref, max);
            double dmax = Math.max(ref, max);
            if (dref == dmax) {
                return 1.0 - f;
            }
            return 1.0 - f * (Math.max(Math.min(d, dmax), dref) - dref) / (dmax - dref);
        }
        double f = Math.max(0.0, rolloff);
        if (ref == 0.0) {
            return 0.0;
        }
        if (model == DistanceModelType.INVERSE) {
            return ref / (ref + f * (Math.max(d, ref) - ref));
        }
        return Math.pow(Math.max(d, ref) / ref, -f);
    }

    /**
     * Returns the gain of the sound cone of a source for a listener: 1 inside the inner cone, the outer gain outside the outer cone,
     * and a linear change between them. 1 if the orientation vector is zero or both cone angles are 360 degrees. The angle is taken
     * between the orientation vector and the vector from the source to the listener (a source that points at the listener is heard fully),
     * as in Chromium; the pseudo code of the specification names that vector <code>sourceToListener</code> but builds it as the
     * listener-to-source difference of the positions, which would make a source that points at the listener silent.
     * <p>
     * 청취자에 대한 음원의 음향 원뿔 게인을 반환한다. 안쪽 원뿔 안에서는 1, 바깥 원뿔 밖에서는 바깥 게인이고 그 사이는 선형으로 바뀐다. 방향 벡터가 0 이거나 두 원뿔
     * 각이 모두 360도이면 1 이다. 각은 방향 벡터와 음원에서 청취자로 향하는 벡터 사이에서 잰다(청취자를 향한 음원은 온전히 들린다. Chromium 과 같다). 규격의
     * 의사 코드는 그 벡터를 <code>sourceToListener</code> 라고 부르지만 위치의 차이를 청취자에서 음원으로 만들어 청취자를 향한 음원이 무음이 되어 버린다.
     *
     * @param ox         the x of the source orientation / 음원 방향 x
     * @param oy         the y of the source orientation / 음원 방향 y
     * @param oz         the z of the source orientation / 음원 방향 z
     * @param px         the x of the source position / 음원 위치 x
     * @param py         the y of the source position / 음원 위치 y
     * @param pz         the z of the source position / 음원 위치 z
     * @param lx         the x of the listener position / 청취자 위치 x
     * @param ly         the y of the listener position / 청취자 위치 y
     * @param lz         the z of the listener position / 청취자 위치 z
     * @param innerAngle the inner cone angle in degrees (the whole angle) / 안쪽 원뿔 각(도, 전체 각)
     * @param outerAngle the outer cone angle in degrees (the whole angle) / 바깥 원뿔 각(도, 전체 각)
     * @param outerGain  the gain outside the outer cone / 바깥 원뿔 밖의 게인
     * @return the gain / 게인
     */
    public static double coneGain(double ox, double oy, double oz, double px, double py, double pz, double lx, double ly, double lz, double innerAngle,
            double outerAngle, double outerGain) {
        double om = Math.sqrt(ox * ox + oy * oy + oz * oz);
        if (om == 0.0 || (innerAngle == 360.0 && outerAngle == 360.0)) {
            return 1.0;
        }
        // 음원에서 청취자로 향하는 벡터(청취자가 원뿔 안에 있는지 알려면 음원이 청취자를 향한 방향과 방향 벡터를 비교해야 한다).
        double sx = lx - px;
        double sy = ly - py;
        double sz = lz - pz;
        double sm = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sm == 0.0) {
            return 1.0;
        }
        double cos = (sx * ox + sy * oy + sz * oz) / (sm * om);
        double angle = 180.0 * Math.acos(Math.max(-1.0, Math.min(1.0, cos))) / Math.PI;
        double absAngle = Math.abs(angle);
        double absInner = Math.abs(innerAngle) / 2.0;
        double absOuter = Math.abs(outerAngle) / 2.0;
        if (absAngle <= absInner) {
            return 1.0;
        }
        if (absAngle >= absOuter) {
            return outerGain;
        }
        double x = (absAngle - absInner) / (absOuter - absInner);
        return (1.0 - x) + outerGain * x;
    }
}
