package org.duckdns.hjow.swingwebpanel.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link LocalStorageManager} and {@link StorageFile} (plan items P16-03 to P16-10): sharing between components, origins, the
 * file format, external deletion, damaged files, concurrent writes, quotas, clear and notifications. Every test uses its own temporary
 * file, never the file of the process-wide instance.
 * <p>
 * {@link LocalStorageManager} 와 {@link StorageFile} 시험(계획 P16-03~P16-10): 컴포넌트 사이의 공유, 오리진, 파일 형식, 외부 삭제, 손상된 파일,
 * 동시 쓰기, 쿼터, clear, 통지. 시험마다 자신의 임시 파일을 쓰며 프로세스 공용 인스턴스의 파일은 쓰지 않는다.
 */
public class LocalStorageManagerTest {

    /**
     * Records notifications for tests.
     * <p>
     * 시험용으로 통지를 기록한다.
     */
    private static final class Recorder implements StorageListener {
        /**
         * Origin the recorder listens for.
         * <p>
         * 기록기가 듣는 오리진.
         */
        private final String origin;

        /**
         * Received notifications as text (guarded by itself).
         * <p>
         * 받은 통지(문자열, 자기 자신으로 보호).
         */
        final List<String> events = new ArrayList<String>();

        /**
         * Creates a recorder.
         * <p>
         * 기록기를 만든다.
         *
         * @param origin origin to listen for / 들을 오리진
         */
        Recorder(String origin) {
            this.origin = origin;
        }

        /**
         * Returns the origin.
         * <p>
         * 오리진을 반환한다.
         *
         * @return origin / 오리진
         */
        public String getOrigin() {
            return origin;
        }

        /**
         * Records a change.
         * <p>
         * 변경을 기록한다.
         *
         * @param key      key / 키
         * @param oldValue old value / 이전 값
         * @param newValue new value / 새 값
         * @param source   source document / 출처 문서
         */
        public void storageChanged(String key, String oldValue, String newValue, Object source) {
            synchronized (events) {
                events.add(key + ":" + oldValue + "->" + newValue);
            }
        }
    }

    /**
     * Temporary files created by the current test, deleted afterwards.
     * <p>
     * 현재 시험이 만든 임시 파일. 시험이 끝나면 지운다.
     */
    private final List<File> created = new ArrayList<File>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LocalStorageManagerTest() {
    }

    /**
     * Returns a path for a store file inside a fresh temporary directory (the file itself does not exist yet).
     * <p>
     * 새 임시 디렉터리 안의 저장소 파일 경로를 반환한다(파일 자체는 아직 없다).
     *
     * @return file / 파일
     * @throws IOException if the directory cannot be created / 디렉터리를 만들 수 없으면
     */
    private File newStoreFile() throws IOException {
        File dir = File.createTempFile("swp-storage-", ".d");
        dir.delete();
        dir.mkdirs();
        created.add(dir);
        return new File(dir, "store.dat");
    }

    /**
     * Removes the temporary files of the case (called by the runner after every case).
     * <p>
     * 사례의 임시 파일을 지운다(실행기가 사례마다 끝난 뒤 호출한다).
     */
    public void tearDown() {
        // 만든 디렉터리마다 안의 파일을 지운 뒤 디렉터리를 지운다.
        for (File dir : created) {
            File[] children = dir.listFiles();
            if (children != null) {
                // 디렉터리 안의 파일을 하나씩 지운다.
                for (File c : children) {
                    c.delete();
                }
            }
            dir.delete();
        }
        created.clear();
    }

    /**
     * Reads a whole file.
     * <p>
     * 파일 전체를 읽는다.
     *
     * @param f file / 파일
     * @return bytes / 바이트
     * @throws IOException on failure / 실패 시
     */
    private static byte[] read(File f) throws IOException {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            // 파일 끝까지 읽는다.
            while ((n = in.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Writes bytes to a file, replacing it, and gives it a newer modification time so that the manager notices the change.
     * <p>
     * 파일을 바이트로 교체해 쓰며 관리자가 변화를 알아차리도록 수정 시각을 더 나중으로 둔다.
     *
     * @param f    file / 파일
     * @param data bytes / 바이트
     * @throws IOException on failure / 실패 시
     */
    private static void write(File f, byte[] data) throws IOException {
        long before = f.exists() ? f.lastModified() : 0;
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(data);
        } finally {
            out.close();
        }
        if (f.lastModified() <= before) {
            f.setLastModified(before + 5000);
        }
    }

    /**
     * P16-03/P16-10: two components share localStorage; origins stay separate.
     * <p>
     * P16-03/P16-10: 두 컴포넌트는 localStorage 를 공유하고 오리진은 분리된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSharedAcrossComponentsSeparatedByOrigin() throws Exception {
        LocalStorageManager m = new LocalStorageManager(newStoreFile());
        WebStorage c1 = m.forOrigin(Origin.of("http://a.test/p"), null);
        WebStorage c2 = m.forOrigin(Origin.of("http://a.test/q"), null);
        WebStorage other = m.forOrigin(Origin.of("http://b.test/"), null);
        c1.setItem("k", "v");
        Check.equal("v", c2.getItem("k"), "a second component of the same origin sees the item");
        Check.isNull(other.getItem("k"), "another origin does not");
        other.setItem("k", "other");
        Check.equal("v", c1.getItem("k"), "no interference");
        List<StorageEntry> all = m.entries();
        Check.equal(2, all.size(), "the Java view lists every origin");
        Check.equal("http://a.test", all.get(0).getOrigin(), "sorted by origin");
        Check.equal("http://b.test", all.get(1).getOrigin(), "second origin");
        Check.isNull(m.forOrigin(Origin.of("http://a.test:8080/"), null).getItem("k"), "another port");
        Check.equal("v", m.forOrigin(Origin.of("http://a.test:80/"), null).getItem("k"), "the default port is normalized");
    }

    /**
     * P16-03/P16-04: data written by one manager can be read by another on the same file (persistence), including non-ASCII text and
     * surrogate pairs.
     * <p>
     * P16-03/P16-04: 한 관리자가 쓴 데이터를 같은 파일의 다른 관리자가 읽을 수 있다(영속성). 비 ASCII 문자와 서로게이트 쌍을 포함한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPersistenceAndUnicode() throws Exception {
        File f = newStoreFile();
        String key = "한글 키";
        String value = "값 " + new String(Character.toChars(0x1F600)) + " é";
        new LocalStorageManager(f).forOrigin(Origin.of("http://a.test/"), null).setItem(key, value);
        Check.isTrue(f.exists(), "the file exists after a write");
        WebStorage again = new LocalStorageManager(f).forOrigin(Origin.of("http://a.test/"), null);
        Check.equal(value, again.getItem(key), "value read back by a new manager");
        Check.equal(1, again.getLength(), "length");
        Check.equal(key, again.key(0), "key");
    }

    /**
     * P16-04: the file starts with the format identifier and version, is never a Java serialization stream, and validates lengths.
     * <p>
     * P16-04: 파일은 형식 식별자와 버전으로 시작하고 Java 직렬화 스트림이 아니며 길이를 검증한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFileFormat() throws Exception {
        File f = newStoreFile();
        new LocalStorageManager(f).forOrigin(Origin.of("http://a.test/"), null).setItem("k", "v");
        byte[] data = read(f);
        Check.equal("SWPLSTOR", new String(data, 0, 8, "US-ASCII"), "identifier");
        Check.equal(1, ((data[8] & 0xFF) << 24) | ((data[9] & 0xFF) << 16) | ((data[10] & 0xFF) << 8) | (data[11] & 0xFF), "version");
        Check.isFalse((data[0] & 0xFF) == 0xAC && (data[1] & 0xFF) == 0xED, "not a Java serialization stream");
        Check.equal(1, StorageFile.decode(data).size(), "the file decodes");
        byte[] cut = new byte[data.length - 3];
        System.arraycopy(data, 0, cut, 0, cut.length);
        try {
            StorageFile.decode(cut);
            Check.fail("a truncated file must be rejected");
        } catch (StorageFile.FormatException expected) {
            Check.isTrue(true, "truncated");
        }
        byte[] flipped = data.clone();
        flipped[flipped.length - 20] ^= 0x55;
        try {
            StorageFile.decode(flipped);
            Check.fail("a changed byte must be rejected by the checksum");
        } catch (StorageFile.FormatException expected) {
            Check.isTrue(true, "checksum");
        }
    }

    /**
     * P16-05: a file deleted from outside is noticed on the next read as an empty store; the manager does not restore it and later writes
     * work again.
     * <p>
     * P16-05: 외부에서 지운 파일은 다음 읽기에서 빈 저장소로 처리되며 관리자는 복구하지 않고 이후 쓰기는 다시 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testExternalDeletion() throws Exception {
        File f = newStoreFile();
        LocalStorageManager m = new LocalStorageManager(f);
        WebStorage w = m.forOrigin(Origin.of("http://a.test/"), null);
        w.setItem("k", "v");
        Check.isTrue(f.delete(), "delete the file from outside");
        Check.equal(0, w.getLength(), "the next read sees an empty store");
        Check.isNull(w.getItem("k"), "the item is gone");
        Check.isFalse(f.exists(), "the manager did not restore the file by itself");
        Check.equal(0, m.entries().size(), "the Java view is empty too");
        w.setItem("n", "1");
        Check.isTrue(f.exists(), "a new write creates the file again");
        Check.equal("1", w.getItem("n"), "and the store works");
        Check.isNull(w.getItem("k"), "old data stays lost");
    }

    /**
     * P16-05: a damaged file (garbage, truncated, wrong version, bad checksum) resets the store instead of failing.
     * <p>
     * P16-05: 손상된 파일(쓰레기, 잘림, 잘못된 버전, 체크섬 불일치)은 오류 대신 저장소를 초기화한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDamagedFiles() throws Exception {
        File f = newStoreFile();
        LocalStorageManager m = new LocalStorageManager(f);
        WebStorage w = m.forOrigin(Origin.of("http://a.test/"), null);
        w.setItem("k", "v");
        byte[] good = read(f);

        write(f, "this is not a storage file at all".getBytes("US-ASCII"));
        Check.equal(0, w.getLength(), "garbage resets the store");
        w.setItem("k", "v");
        Check.equal("v", w.getItem("k"), "and the store is usable afterwards");

        byte[] good2 = read(f);
        byte[] cut = new byte[good2.length / 2];
        System.arraycopy(good2, 0, cut, 0, cut.length);
        write(f, cut);
        Check.equal(0, w.getLength(), "a truncated file resets the store");

        w.setItem("k", "v");
        byte[] versioned = read(f);
        versioned[11] = 9;
        write(f, versioned);
        Check.equal(0, w.getLength(), "an unknown version (or its checksum) resets the store");

        w.setItem("k", "v");
        byte[] bad = read(f);
        bad[bad.length - 1] ^= 1;
        write(f, bad);
        Check.equal(0, w.getLength(), "a wrong checksum resets the store");

        write(f, new byte[0]);
        Check.equal(0, w.getLength(), "an empty file resets the store");
        w.setItem("after", "ok");
        Check.equal("ok", new LocalStorageManager(f).forOrigin(Origin.of("http://a.test/"), null).getItem("after"), "valid file again");
        Check.isTrue(good.length > 0, "the first good file had content");
    }

    /**
     * P16-05: an external replacement by another valid file is picked up (the file is the source of truth).
     * <p>
     * P16-05: 다른 올바른 파일로의 외부 교체는 반영된다(파일이 원본이다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testExternalChangeIsReloaded() throws Exception {
        File f = newStoreFile();
        LocalStorageManager m1 = new LocalStorageManager(f);
        WebStorage w1 = m1.forOrigin(Origin.of("http://a.test/"), null);
        w1.setItem("k", "one");
        LocalStorageManager m2 = new LocalStorageManager(f);
        m2.forOrigin(Origin.of("http://a.test/"), null).setItem("k", "two-changed-longer");
        Check.equal("two-changed-longer", w1.getItem("k"), "the first manager reloads after a change by another manager of the file");
    }

    /**
     * P16-06: many threads writing at the same time lose no item and leave a valid file.
     * <p>
     * P16-06: 여러 스레드가 동시에 써도 항목을 잃지 않고 올바른 파일을 남긴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConcurrentWrites() throws Exception {
        final File f = newStoreFile();
        final LocalStorageManager m = new LocalStorageManager(f);
        final int threads = 6;
        final int perThread = 40;
        final AtomicInteger failures = new AtomicInteger();
        Thread[] ts = new Thread[threads];
        // 스레드마다 자신의 키를 반복해서 쓴다.
        for (int t = 0; t < threads; t++) {
            final int id = t;
            ts[t] = new Thread(new Runnable() {
                /**
                 * Writes this thread's keys.
                 * <p>
                 * 이 스레드의 키를 쓴다.
                 */
                public void run() {
                    try {
                        WebStorage w = m.forOrigin(Origin.of("http://a.test/"), null);
                        // 이 스레드의 키를 하나씩 쓴다.
                        for (int i = 0; i < perThread; i++) {
                            w.setItem("t" + id + "-" + i, "value" + i);
                        }
                    } catch (RuntimeException e) {
                        failures.incrementAndGet();
                    }
                }
            });
            ts[t].start();
        }
        // 모든 스레드가 끝나기를 기다린다.
        for (int t = 0; t < threads; t++) {
            ts[t].join(60000);
        }
        Check.equal(0, failures.get(), "no write failed");
        Check.equal(threads * perThread, m.entries().size(), "no item was lost");
        Check.equal(threads * perThread, new LocalStorageManager(f).entries().size(), "the file holds every item");
        Check.isFalse(new File(f.getPath() + ".tmp").exists(), "no temporary file is left");
    }

    /**
     * P16-06: writes go through a temporary file; a failing replacement keeps one complete version.
     * <p>
     * P16-06: 쓰기는 임시 파일을 거치며 교체가 실패해도 완전한 판 하나는 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWriteLeavesNoTemporaryFile() throws Exception {
        File f = newStoreFile();
        LocalStorageManager m = new LocalStorageManager(f);
        WebStorage w = m.forOrigin(Origin.of("http://a.test/"), null);
        // 쓰기를 여러 번 되풀이해도 임시/백업 파일이 남지 않는다.
        for (int i = 0; i < 20; i++) {
            w.setItem("k", "value-" + i);
        }
        Check.isFalse(new File(f.getPath() + ".tmp").exists(), "no .tmp file");
        Check.isFalse(new File(f.getPath() + ".bak").exists(), "no .bak file");
        Check.equal("value-19", new LocalStorageManager(f).forOrigin(Origin.of("http://a.test/"), null).getItem("k"), "last value");
    }

    /**
     * P16-07: the total quota is enforced across origins and the old data survives a failed write.
     * <p>
     * P16-07: 전체 쿼터는 오리진을 가로질러 적용되며 실패한 쓰기는 옛 데이터를 유지한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testQuotas() throws Exception {
        LocalStorageManager m = new LocalStorageManager(newStoreFile());
        final WebStorage w = m.forOrigin(Origin.of("http://a.test/"), null);
        char[] big = new char[StorageLimits.MAX_ORIGIN_CHARS - 2];
        java.util.Arrays.fill(big, 'x');
        w.setItem("k", new String(big));
        Check.throwsException(StorageQuotaException.class, new Block() {
            /**
             * Adds data beyond the per-origin quota.
             * <p>
             * 오리진 쿼터를 넘는 데이터를 추가한다.
             */
            public void run() {
                w.setItem("more", "data");
            }
        }, "over the quota of one origin");
        Check.equal(1, w.getLength(), "nothing was added");
        Check.equal(StorageLimits.MAX_ORIGIN_CHARS - 2, w.getItem("k").length(), "the old value is intact");
        // 오리진마다 한도까지 채워 전체 쿼터에 도달한다.
        // 이미 한 오리진이 거의 한도까지 찼으므로 그만큼을 뺀 수만큼만 더 채워 전체 쿼터 바로 아래에 둔다.
        int fillable = StorageLimits.MAX_TOTAL_CHARS / StorageLimits.MAX_ORIGIN_CHARS - 1;
        for (int i = 0; i < fillable; i++) {
            m.forOrigin(Origin.of("http://fill" + i + ".test/"), null).setItem("k", new String(big));
        }
        final WebStorage last = m.forOrigin(Origin.of("http://last.test/"), null);
        Check.throwsException(StorageQuotaException.class, new Block() {
            /**
             * Adds data beyond the total quota.
             * <p>
             * 전체 쿼터를 넘는 데이터를 추가한다.
             */
            public void run() {
                last.setItem("k", "x" + new String(new char[StorageLimits.MAX_ORIGIN_CHARS / 2]));
            }
        }, "over the total quota");
        Check.equal(0, last.getLength(), "the failed origin stays empty");
    }

    /**
     * P16-07: opaque origins (about:blank and about:text) have no localStorage; classpath documents share one origin.
     * <p>
     * P16-07: 불투명 오리진(about:blank, about:text)에는 localStorage 가 없고 classpath 문서끼리는 한 오리진을 공유한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOpaqueAndClasspathOrigins() throws Exception {
        final LocalStorageManager m = new LocalStorageManager(newStoreFile());
        Check.throwsException(DomException.class, new Block() {
            /**
             * Asks for the storage of an opaque origin.
             * <p>
             * 불투명 오리진의 저장소를 요청한다.
             */
            public void run() {
                m.forOrigin(Origin.of("about:blank"), null);
            }
        }, "about:blank");
        Check.throwsException(DomException.class, new Block() {
            /**
             * Asks for the storage of a blocked scheme.
             * <p>
             * 차단된 스키마의 저장소를 요청한다.
             */
            public void run() {
                m.forOrigin(Origin.of("file://nonlocal.invalid/path"), null);
            }
        }, "blocked scheme");
        m.forOrigin(Origin.of("classpath:///docs/a.html"), null).setItem("k", "v");
        Check.equal("v", m.forOrigin(Origin.of("classpath:///other/b.html"), null).getItem("k"), "classpath documents share one origin");
        Check.isNull(m.forOrigin(Origin.of("http://docs/"), null).getItem("k"), "and it is not a web origin");
    }

    /**
     * P16-08: other documents of the origin get change events, the changing document does not, clear is reported per origin, and a failing
     * listener does not stop the others.
     * <p>
     * P16-08: 오리진의 다른 문서는 변경 이벤트를 받고 변경한 문서는 받지 않으며 clear 는 오리진마다 보고되고 실패한 리스너가 다른 리스너를 막지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNotifications() throws Exception {
        LocalStorageManager m = new LocalStorageManager(newStoreFile());
        Recorder r1 = new Recorder("http://a.test");
        Recorder r2 = new Recorder("http://a.test");
        Recorder other = new Recorder("http://b.test");
        m.addListener(r1);
        m.addListener(r2);
        m.addListener(other);
        WebStorage w = m.forOrigin(Origin.of("http://a.test/"), r2);
        w.setItem("k", "v");
        Check.equal(1, r1.events.size(), "another document is told");
        Check.equal("k:null->v", r1.events.get(0), "content");
        Check.equal(0, r2.events.size(), "the changing document is not told");
        Check.equal(0, other.events.size(), "another origin is not told");
        w.setItem("k", "v");
        Check.equal(1, r1.events.size(), "same value: no event");
        m.forOrigin(Origin.of("http://b.test/"), null).setItem("x", "y");
        Check.equal(1, other.events.size(), "events of the other origin");
        m.clearAll();
        Check.equal(2, r1.events.size(), "clear event for origin a");
        Check.equal("null:null->null", r1.events.get(1), "clear event content");
        Check.equal(2, other.events.size(), "clear event for origin b");
        Check.equal(1, r2.events.size(), "a clearAll has no source document, so the document that wrote earlier is told too");
        m.clearAll();
        Check.equal(2, r1.events.size(), "clearing an empty store is silent");
        m.removeListener(r1);
        w.setItem("again", "1");
        Check.equal(2, r1.events.size(), "a removed listener hears nothing");
    }

    /**
     * P16-09: clearAll empties the store for every component, and every component sees it.
     * <p>
     * P16-09: clearAll 은 모든 컴포넌트의 저장소를 비우고 모든 컴포넌트가 그것을 본다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClearAllAffectsEveryComponent() throws Exception {
        LocalStorageManager m = new LocalStorageManager(newStoreFile());
        WebStorage c1 = m.forOrigin(Origin.of("http://a.test/"), null);
        WebStorage c2 = m.forOrigin(Origin.of("http://a.test/"), null);
        c1.setItem("k", "v");
        c2.setItem("k2", "v2");
        List<StorageEntry> before = m.entries();
        m.clearAll();
        Check.equal(0, c1.getLength(), "first component");
        Check.equal(0, c2.getLength(), "second component");
        Check.equal(0, m.entries().size(), "Java view");
        Check.equal(2, before.size(), "an earlier copy is not changed");
        Check.equal(0, new LocalStorageManager(m.getFile()).entries().size(), "the file is empty as well");
    }

    /**
     * P16-09: clearAll on a store that never wrote anything creates no file.
     * <p>
     * P16-09: 아무것도 쓴 적이 없는 저장소의 clearAll 은 파일을 만들지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClearAllOnEmptyStoreCreatesNoFile() throws Exception {
        File f = newStoreFile();
        new LocalStorageManager(f).clearAll();
        Check.isFalse(f.exists(), "no file was created");
    }

    /**
     * P16-03: the process-wide file has a name unique to this process, lives under the temporary directory and is not touched by tests.
     * <p>
     * P16-03: 프로세스 공용 파일은 이 프로세스 고유의 이름이며 임시 디렉터리 아래에 있고 시험이 건드리지 않는다.
     */
    public void testProcessFileName() {
        LocalStorageManager m = LocalStorageManager.getInstance();
        Check.same(m, LocalStorageManager.getInstance(), "one instance per process");
        String tmp = new File(System.getProperty("java.io.tmpdir")).getAbsoluteFile().getPath();
        Check.isTrue(m.getFile().getAbsolutePath().startsWith(tmp), "under the temporary directory");
        Check.isTrue(m.getFile().getName().startsWith("localstorage-"), "name pattern");
        Check.isTrue(m.getFile().getName().endsWith(".dat"), "extension");
    }

    /**
     * P16-03/P16-10: managers with different files (as two processes have) never see, change or delete each other's data.
     * <p>
     * P16-03/P16-10: 파일이 다른 관리자(두 프로세스가 그렇다)는 서로의 데이터를 보거나 바꾸거나 지우지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDifferentFilesAreIndependent() throws Exception {
        File f1 = newStoreFile();
        File f2 = newStoreFile();
        LocalStorageManager m1 = new LocalStorageManager(f1);
        LocalStorageManager m2 = new LocalStorageManager(f2);
        Origin o = Origin.of("http://a.test/");
        m1.forOrigin(o, null).setItem("k", "one");
        Check.isNull(m2.forOrigin(o, null).getItem("k"), "the other file has no such item");
        m2.forOrigin(o, null).setItem("k", "two");
        m1.clearAll();
        Check.equal("two", m2.forOrigin(o, null).getItem("k"), "clearing one store does not touch the other file");
        Check.isTrue(f2.exists(), "the other file still exists");
    }

    /**
     * P16-02/P21-05: the manager holds listeners weakly, so a document that is no longer referenced is collected, and the manager does
     * not keep a component alive.
     * <p>
     * P16-02/P21-05: 관리자는 리스너를 약한 참조로 보관하므로 더 이상 참조되지 않는 문서는 수거되며 관리자가 컴포넌트를 붙잡지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testListenersAreWeak() throws Exception {
        LocalStorageManager m = new LocalStorageManager(newStoreFile());
        Recorder r = new Recorder("http://a.test");
        m.addListener(r);
        Check.equal(1, m.listenerCount(), "registered");
        WeakReference<Recorder> ref = new WeakReference<Recorder>(r);
        r = null;
        long end = System.currentTimeMillis() + 30000;
        // 수거될 때까지 GC 를 반복 요청하며 기다린다.
        while (ref.get() != null && System.currentTimeMillis() < end) {
            System.gc();
            Thread.sleep(50);
        }
        Check.isNull(ref.get(), "the listener was collected although the manager still exists");
        m.forOrigin(Origin.of("http://a.test/"), null).setItem("k", "v");
        Check.equal(0, m.listenerCount(), "dead references are dropped");
    }

    /**
     * P16-04: limits are validated when reading (a file claiming too many origins or items is rejected).
     * <p>
     * P16-04: 읽을 때 제한을 검증한다(오리진이나 항목이 너무 많다고 주장하는 파일은 거부된다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFileLimitsAreValidated() throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream out = new java.io.DataOutputStream(bos);
        out.write("SWPLSTOR".getBytes("US-ASCII"));
        out.writeInt(1);
        out.writeInt(StorageLimits.MAX_ORIGINS + 1);
        out.flush();
        byte[] body = bos.toByteArray();
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(body);
        java.io.ByteArrayOutputStream full = new java.io.ByteArrayOutputStream();
        full.write(body);
        new java.io.DataOutputStream(full).writeLong(crc.getValue());
        try {
            StorageFile.decode(full.toByteArray());
            Check.fail("too many origins must be rejected");
        } catch (StorageFile.FormatException expected) {
            Check.isTrue(true, "origin count limit");
        }
    }
}
