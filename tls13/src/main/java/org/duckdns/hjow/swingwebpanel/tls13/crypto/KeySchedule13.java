package org.duckdns.hjow.swingwebpanel.tls13.crypto;

/**
 * The key schedule of TLS 1.3 without pre-shared keys (RFC 8446 section 7.1). The object walks through the secrets in order: early secret
 * (created by the constructor), handshake secret ({@link #addSharedSecret(byte[])}), master secret ({@link #toMaster()}). The traffic
 * secrets are derived from the current stage with the transcript hash at that point. Usage:
 * <pre>
 * KeySchedule13 ks = new KeySchedule13(HashAlg.SHA256);
 * ks.addSharedSecret(ecdhe);
 * byte[] serverHs = ks.serverHandshakeTraffic(hashOfClientHelloToServerHello);
 * ks.toMaster();
 * byte[] serverAp = ks.serverApplicationTraffic(hashUpToServerFinished);
 * </pre>
 * <p>
 * 사전 공유 키가 없는 TLS 1.3 키 스케줄(RFC 8446 7.1절). 객체는 비밀을 순서대로 지난다: early secret(생성자가 만든다), handshake secret
 * ({@link #addSharedSecret(byte[])}), master secret({@link #toMaster()}). 트래픽 비밀은 현재 단계에서 그 시점의 전사 해시로 유도한다.
 */
public final class KeySchedule13 {

    /** Stage: early secret. / 단계: early secret. */
    private static final int EARLY = 0;

    /** Stage: handshake secret. / 단계: handshake secret. */
    private static final int HANDSHAKE = 1;

    /** Stage: master secret. / 단계: master secret. */
    private static final int MASTER = 2;

    /** The hash of the cipher suite. / 암호 스위트의 해시. */
    private final HashAlg hash;

    /** The secret of the current stage. / 현재 단계의 비밀. */
    private byte[] secret;

    /** The current stage. / 현재 단계. */
    private int stage;

    /**
     * Creates the schedule at the early secret, computed with a pre-shared key of zeros.
     * <p>
     * 0 으로 채운 사전 공유 키로 계산한 early secret 단계의 스케줄을 만든다.
     *
     * @param hash the hash of the cipher suite / 암호 스위트의 해시
     */
    public KeySchedule13(HashAlg hash) {
        this.hash = hash;
        this.secret = Hkdf.extract(hash, null, new byte[hash.length()]);
        this.stage = EARLY;
    }

    /**
     * Returns the hash of the schedule.
     * <p>
     * 스케줄의 해시를 반환한다.
     *
     * @return the hash / 해시
     */
    public HashAlg hash() {
        return hash;
    }

    /**
     * Derives the salt of the next stage: <code>Derive-Secret(secret, "derived", Hash(""))</code>.
     * <p>
     * 다음 단계의 salt 를 유도한다: <code>Derive-Secret(secret, "derived", Hash(""))</code>.
     *
     * @return the salt / salt
     */
    private byte[] derived() {
        return Hkdf.deriveSecret(hash, secret, "derived", hash.digest());
    }

    /**
     * Moves from the early secret to the handshake secret with the (EC)DHE shared secret.
     * <p>
     * (EC)DHE 공유 비밀로 early secret 에서 handshake secret 으로 넘어간다.
     *
     * @param shared the shared secret of the key exchange / 키 교환의 공유 비밀
     */
    public void addSharedSecret(byte[] shared) {
        if (stage != EARLY) {
            throw new IllegalStateException("the shared secret was already added");
        }
        byte[] salt = derived();
        byte[] old = secret;
        secret = Hkdf.extract(hash, salt, shared);
        Bytes.wipe(old);
        Bytes.wipe(salt);
        stage = HANDSHAKE;
    }

    /**
     * Moves from the handshake secret to the master secret.
     * <p>
     * handshake secret 에서 master secret 으로 넘어간다.
     */
    public void toMaster() {
        if (stage != HANDSHAKE) {
            throw new IllegalStateException("not at the handshake secret");
        }
        byte[] salt = derived();
        byte[] old = secret;
        secret = Hkdf.extract(hash, salt, new byte[hash.length()]);
        Bytes.wipe(old);
        Bytes.wipe(salt);
        stage = MASTER;
    }

    /**
     * Derives a traffic secret of the required stage.
     * <p>
     * 요구하는 단계의 트래픽 비밀을 유도한다.
     *
     * @param need       the stage that the label belongs to / 레이블이 속한 단계
     * @param label      the label / 레이블
     * @param transcript the transcript hash / 전사 해시
     * @return the traffic secret / 트래픽 비밀
     */
    private byte[] traffic(int need, String label, byte[] transcript) {
        if (stage != need) {
            throw new IllegalStateException("wrong stage for " + label);
        }
        return Hkdf.deriveSecret(hash, secret, label, transcript);
    }

    /**
     * Derives <code>client_handshake_traffic_secret</code> from the hash of ClientHello...ServerHello.
     * <p>
     * ClientHello...ServerHello 의 해시로 <code>client_handshake_traffic_secret</code> 을 유도한다.
     *
     * @param transcript the transcript hash / 전사 해시
     * @return the secret / 비밀
     */
    public byte[] clientHandshakeTraffic(byte[] transcript) {
        return traffic(HANDSHAKE, "c hs traffic", transcript);
    }

    /**
     * Derives <code>server_handshake_traffic_secret</code> from the hash of ClientHello...ServerHello.
     * <p>
     * ClientHello...ServerHello 의 해시로 <code>server_handshake_traffic_secret</code> 을 유도한다.
     *
     * @param transcript the transcript hash / 전사 해시
     * @return the secret / 비밀
     */
    public byte[] serverHandshakeTraffic(byte[] transcript) {
        return traffic(HANDSHAKE, "s hs traffic", transcript);
    }

    /**
     * Derives <code>client_application_traffic_secret_0</code> from the hash of ClientHello...server Finished.
     * <p>
     * ClientHello...서버 Finished 의 해시로 <code>client_application_traffic_secret_0</code> 을 유도한다.
     *
     * @param transcript the transcript hash / 전사 해시
     * @return the secret / 비밀
     */
    public byte[] clientApplicationTraffic(byte[] transcript) {
        return traffic(MASTER, "c ap traffic", transcript);
    }

    /**
     * Derives <code>server_application_traffic_secret_0</code> from the hash of ClientHello...server Finished.
     * <p>
     * ClientHello...서버 Finished 의 해시로 <code>server_application_traffic_secret_0</code> 을 유도한다.
     *
     * @param transcript the transcript hash / 전사 해시
     * @return the secret / 비밀
     */
    public byte[] serverApplicationTraffic(byte[] transcript) {
        return traffic(MASTER, "s ap traffic", transcript);
    }

    /**
     * Wipes the current secret; the schedule cannot be used afterwards.
     * <p>
     * 현재 비밀을 지운다. 그 뒤에는 스케줄을 쓸 수 없다.
     */
    public void destroy() {
        Bytes.wipe(secret);
        stage = -1;
    }

    /**
     * Derives the record protection key of a traffic secret: <code>HKDF-Expand-Label(secret, "key", "", keyLength)</code>.
     * <p>
     * 트래픽 비밀의 레코드 보호 키를 유도한다: <code>HKDF-Expand-Label(secret, "key", "", keyLength)</code>.
     *
     * @param hash      the hash / 해시
     * @param traffic   the traffic secret / 트래픽 비밀
     * @param keyLength the key length of the AEAD / AEAD 의 키 길이
     * @return the key / 키
     */
    public static byte[] trafficKey(HashAlg hash, byte[] traffic, int keyLength) {
        return Hkdf.expandLabel(hash, traffic, "key", new byte[0], keyLength);
    }

    /**
     * Derives the 12 byte record protection IV of a traffic secret.
     * <p>
     * 트래픽 비밀의 12바이트 레코드 보호 IV 를 유도한다.
     *
     * @param hash    the hash / 해시
     * @param traffic the traffic secret / 트래픽 비밀
     * @return the IV / IV
     */
    public static byte[] trafficIv(HashAlg hash, byte[] traffic) {
        return Hkdf.expandLabel(hash, traffic, "iv", new byte[0], 12);
    }

    /**
     * Computes the <code>verify_data</code> of a Finished message: <code>HMAC(finished_key, transcriptHash)</code> where
     * <code>finished_key = HKDF-Expand-Label(baseKey, "finished", "", Hash.length)</code>.
     * <p>
     * Finished 메시지의 <code>verify_data</code> 를 계산한다: <code>HMAC(finished_key, transcriptHash)</code>. 여기서
     * <code>finished_key = HKDF-Expand-Label(baseKey, "finished", "", Hash.length)</code>.
     *
     * @param hash       the hash / 해시
     * @param baseKey    the handshake traffic secret of the sender / 보내는 쪽의 핸드셰이크 트래픽 비밀
     * @param transcript the transcript hash before the Finished message / Finished 메시지 앞까지의 전사 해시
     * @return the verify data / verify data
     */
    public static byte[] finishedMac(HashAlg hash, byte[] baseKey, byte[] transcript) {
        byte[] key = Hkdf.expandLabel(hash, baseKey, "finished", new byte[0], hash.length());
        byte[] mac = hash.hmac(key, transcript);
        Bytes.wipe(key);
        return mac;
    }

    /**
     * Derives the next application traffic secret after a KeyUpdate: <code>HKDF-Expand-Label(secret, "traffic upd", "", Hash.length)</code>.
     * <p>
     * KeyUpdate 뒤의 다음 응용 트래픽 비밀을 유도한다: <code>HKDF-Expand-Label(secret, "traffic upd", "", Hash.length)</code>.
     *
     * @param hash    the hash / 해시
     * @param traffic the current application traffic secret / 현재 응용 트래픽 비밀
     * @return the next secret / 다음 비밀
     */
    public static byte[] nextTrafficSecret(HashAlg hash, byte[] traffic) {
        return Hkdf.expandLabel(hash, traffic, "traffic upd", new byte[0], hash.length());
    }
}
