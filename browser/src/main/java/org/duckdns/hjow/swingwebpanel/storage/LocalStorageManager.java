package org.duckdns.hjow.swingwebpanel.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.net.Origin;

/**
 * The process-wide localStorage: one store per origin shared by all components of the JVM process, kept in one temporary file that is
 * private to this process (the name contains the process id, a time stamp and a random number; files of other processes are never read,
 * changed or deleted). The file is the source of truth: before every operation the manager checks whether the file changed or vanished
 * since it last wrote or read it, and reloads it; a missing file simply means an empty store, and a file that cannot be parsed (damaged,
 * truncated, unknown version) resets the store (plan items P16-03 to P16-06). Nothing watches the file and nothing tries to restore data
 * that someone deleted. Writes go to a temporary file first and replace the real file in a way that never loses both versions on JRE 6
 * (where renaming over an existing file fails on Windows). The manager keeps no strong reference to components or documents (listeners are
 * weak), so it never prevents garbage collection of a component. The OS is expected to delete old temporary files.
 * <p>
 * 프로세스 공용 localStorage: JVM 프로세스의 모든 컴포넌트가 공유하는 오리진별 저장소이며, 이 프로세스 전용의 임시 파일 하나에 보관한다(이름에 프로세스
 * id, 시각, 난수가 들어 있어 다른 프로세스의 파일은 읽거나 바꾸거나 지우지 않는다). 파일이 원본이다: 매 연산 전에 파일이 마지막으로 쓰거나 읽은 뒤
 * 바뀌었거나 사라졌는지 확인하고 다시 읽는다. 파일이 없으면 빈 저장소로 보고, 해석할 수 없는 파일(손상, 잘림, 알 수 없는 버전)은 저장소를
 * 초기화한다(계획 P16-03~P16-06). 파일을 감시하지도, 누가 지운 데이터를 복구하려 하지도 않는다. 쓰기는 먼저 임시 파일에 한 뒤 실제 파일을
 * 교체하며, JRE 6(Windows 에서 기존 파일 위로 이름 바꾸기가 실패함)에서도 두 판을 모두 잃지 않는 방식을 쓴다. 관리자는 컴포넌트나 문서를 강한
 * 참조로 붙잡지 않으므로(리스너는 약한 참조) 컴포넌트의 가비지 컬렉션을 막지 않는다. 오래된 임시 파일은 OS 가 지우는 것으로 가정한다.
 */
public final class LocalStorageManager {

    /**
     * The manager of this process, created on first use.
     * <p>
     * 이 프로세스의 관리자. 처음 쓸 때 만든다.
     */
    private static LocalStorageManager instance;

    /**
     * The file that holds the store.
     * <p>
     * 저장소를 담는 파일.
     */
    private final File file;

    /**
     * Guards the state below and all file access.
     * <p>
     * 아래 상태와 모든 파일 접근을 보호한다.
     */
    private final Object lock = new Object();

    /**
     * The loaded store by serialized origin (only origins that hold items).
     * <p>
     * 불러온 저장소(직렬화된 오리진별, 항목이 있는 오리진만).
     */
    private Map<String, StorageArea> areas = new LinkedHashMap<String, StorageArea>();

    /**
     * True once the store was loaded or written at least once.
     * <p>
     * 저장소를 한 번이라도 불러오거나 썼으면 true.
     */
    private boolean loaded;

    /**
     * Whether the file existed when its state was last recorded.
     * <p>
     * 파일 상태를 마지막으로 기록할 때 파일이 있었는지 여부.
     */
    private boolean knownExists;

    /**
     * Modification time of the file when its state was last recorded.
     * <p>
     * 파일 상태를 마지막으로 기록할 때의 수정 시각.
     */
    private long knownModified;

    /**
     * Length of the file when its state was last recorded.
     * <p>
     * 파일 상태를 마지막으로 기록할 때의 길이.
     */
    private long knownLength;

    /**
     * True if the last attempt to write the file failed: the in-memory store is then authoritative until a write succeeds.
     * <p>
     * 마지막 파일 쓰기가 실패했으면 true. 이때는 쓰기가 성공할 때까지 메모리의 저장소가 기준이다.
     */
    private boolean persistFailed;

    /**
     * Listeners of documents (weak).
     * <p>
     * 문서의 리스너(약한 참조).
     */
    private final StorageListeners listeners = new StorageListeners(true);

    /**
     * Creates a manager that keeps its store in a given file (used by the process-wide instance and by tests).
     * <p>
     * 주어진 파일에 저장소를 두는 관리자를 만든다(프로세스 공용 인스턴스와 시험이 쓴다).
     *
     * @param file the store file; it need not exist / 저장소 파일. 없어도 된다
     */
    public LocalStorageManager(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        this.file = file;
    }

    /**
     * Returns the manager of this process; its file is created lazily in the temporary directory with a name that is unique to this
     * process.
     * <p>
     * 이 프로세스의 관리자를 반환한다. 파일은 임시 디렉터리에 이 프로세스에서 유일한 이름으로 필요할 때 만들어진다.
     *
     * @return the shared manager / 공용 관리자
     */
    public static synchronized LocalStorageManager getInstance() {
        if (instance == null) {
            instance = new LocalStorageManager(uniqueFile());
        }
        return instance;
    }

    /**
     * Builds a file name that no other process uses: process id, time and a random number.
     * <p>
     * 다른 프로세스가 쓰지 않는 파일 이름을 만든다: 프로세스 id, 시각, 난수.
     *
     * @return file / 파일
     */
    private static File uniqueFile() {
        String pid = "0";
        try {
            String name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            int at = name.indexOf('@');
            pid = at > 0 ? name.substring(0, at) : name;
        } catch (RuntimeException e) {
            pid = "x";
        }
        File dir = new File(System.getProperty("java.io.tmpdir"), "swingwebpanel");
        return new File(dir, "localstorage-" + pid.replaceAll("[^0-9A-Za-z]", "") + "-" + System.currentTimeMillis() + "-"
                + Integer.toHexString(new Random().nextInt()) + ".dat");
    }

    /**
     * Returns the number of live registered listeners (for tests and diagnostics).
     * <p>
     * 등록된 살아 있는 리스너의 수를 반환한다(시험과 진단용).
     *
     * @return count / 개수
     */
    int listenerCount() {
        return listeners.size();
    }

    /**
     * Returns the store file.
     * <p>
     * 저장소 파일을 반환한다.
     *
     * @return file / 파일
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the storage view of an origin.
     * <p>
     * 오리진의 저장소 뷰를 반환한다.
     *
     * @param origin origin of the document / 문서의 오리진
     * @param source the document that will use the view (excluded from change notifications), may be null / 뷰를 쓸 문서(변경 통지에서 제외). null 가능
     * @return the view / 뷰
     * @throws DomException SecurityError for opaque origins (about:blank, about:text) / 불투명 오리진이면 SecurityError
     */
    public WebStorage forOrigin(Origin origin, Object source) {
        if (origin == null || origin.isOpaque()) {
            throw DomException.security("this document has no storage (opaque origin)");
        }
        return new View(origin.serialize(), source);
    }

    /**
     * Registers a (weakly held) listener of a document.
     * <p>
     * 문서의 리스너를 등록한다(약한 참조로 보관).
     *
     * @param l listener / 리스너
     */
    public void addListener(StorageListener l) {
        listeners.add(l);
    }

    /**
     * Removes a listener.
     * <p>
     * 리스너를 제거한다.
     *
     * @param l listener / 리스너
     */
    public void removeListener(StorageListener l) {
        listeners.remove(l);
    }

    /**
     * Returns a read-only copy of all items of all origins, sorted by origin and key.
     * <p>
     * 모든 오리진의 모든 항목의 읽기 전용 복사본을 오리진, 키 순으로 정렬해 반환한다.
     *
     * @return entries / 항목
     */
    public List<StorageEntry> entries() {
        List<StorageEntry> list = new ArrayList<StorageEntry>();
        synchronized (lock) {
            refresh();
            // 오리진마다 항목을 모은다.
            for (Map.Entry<String, StorageArea> e : areas.entrySet()) {
                list.addAll(e.getValue().entries(e.getKey()));
            }
        }
        Collections.sort(list, new Comparator<StorageEntry>() {
            /**
             * Orders by origin, then by key.
             * <p>
             * 오리진, 키 순으로 정렬한다.
             *
             * @param a first / 첫 번째
             * @param b second / 두 번째
             * @return comparison / 비교 결과
             */
            public int compare(StorageEntry a, StorageEntry b) {
                int c = a.getOrigin().compareTo(b.getOrigin());
                return c != 0 ? c : a.getKey().compareTo(b.getKey());
            }
        });
        return list;
    }

    /**
     * Removes every item of every origin (the effect of <code>clean()</code> of any component) and reports a clear event for every origin
     * that had items.
     * <p>
     * 모든 오리진의 모든 항목을 제거하고(어느 컴포넌트의 <code>clean()</code> 이든 같은 효과) 항목이 있던 오리진마다 clear 이벤트를 보고한다.
     */
    public void clearAll() {
        List<String> cleared = new ArrayList<String>();
        synchronized (lock) {
            refresh();
            cleared.addAll(areas.keySet());
            boolean hadFile = knownExists;
            areas = new LinkedHashMap<String, StorageArea>();
            // 저장소가 이미 비어 있고 파일도 없으면 빈 파일을 새로 만들지 않는다.
            if (!cleared.isEmpty() || hadFile) {
                persist();
            }
        }
        // 항목이 있던 오리진마다 clear 이벤트를 알린다.
        for (String origin : cleared) {
            listeners.fire(origin, null, null, null, null);
        }
    }

    /**
     * Brings the in-memory store in line with the file: reloads it if it changed or vanished since the last access. A missing file is an
     * empty store; an invalid file resets the store.
     * <p>
     * 메모리의 저장소를 파일과 맞춘다: 마지막 접근 뒤에 파일이 바뀌었거나 사라졌으면 다시 읽는다. 파일이 없으면 빈 저장소이고 올바르지 않은
     * 파일은 저장소를 초기화한다. 호출자는 잠금을 잡고 있어야 한다.
     */
    private void refresh() {
        if (persistFailed) {
            return;
        }
        boolean exists = file.exists();
        long modified = exists ? file.lastModified() : 0;
        long length = exists ? file.length() : 0;
        if (loaded && exists == knownExists && modified == knownModified && length == knownLength) {
            return;
        }
        loaded = true;
        knownExists = exists;
        knownModified = modified;
        knownLength = length;
        if (!exists) {
            areas = new LinkedHashMap<String, StorageArea>();
            return;
        }
        try {
            areas = StorageFile.decode(readAll(file));
        } catch (StorageFile.FormatException e) {
            resetDamagedFile();
        } catch (IOException e) {
            resetDamagedFile();
        }
    }

    /**
     * Resets the store after the file turned out to be unusable: the in-memory store becomes empty and the file is replaced by an empty store.
     * <p>
     * 파일을 쓸 수 없는 것으로 드러난 뒤 저장소를 초기화한다: 메모리의 저장소가 비고 파일은 빈 저장소로 교체된다.
     */
    private void resetDamagedFile() {
        areas = new LinkedHashMap<String, StorageArea>();
        file.delete();
        persist();
    }

    /**
     * Reads a whole file.
     * <p>
     * 파일 전체를 읽는다.
     *
     * @param f file / 파일
     * @return bytes / 바이트
     * @throws IOException on read failure or if the file is unreasonably large / 읽기 실패나 파일이 지나치게 클 때
     */
    private static byte[] readAll(File f) throws IOException {
        long len = f.length();
        if (len > 64L * 1024 * 1024) {
            throw new IOException("the storage file is too large");
        }
        InputStream in = new FileInputStream(f);
        try {
            byte[] data = new byte[(int) len];
            int off = 0;
            // 파일 끝까지 채운다.
            while (off < data.length) {
                int n = in.read(data, off, data.length - off);
                if (n < 0) {
                    throw new IOException("unexpected end of file");
                }
                off += n;
            }
            return data;
        } finally {
            in.close();
        }
    }

    /**
     * Writes the store: to a temporary file first, then it replaces the real file. If renaming over the existing file fails (JRE 6 on
     * Windows), the old file is moved aside, the new file is moved in, and on failure the old file is moved back, so one complete version always
     * survives. If writing fails altogether, the in-memory store stays authoritative until a later write succeeds.
     * <p>
     * 저장소를 쓴다: 먼저 임시 파일에 쓰고 실제 파일을 교체한다. 기존 파일 위로 이름 바꾸기가 실패하면(Windows 의 JRE 6) 옛 파일을 옆으로 옮기고
     * 새 파일을 옮겨 넣으며, 실패하면 옛 파일을 되돌리므로 완전한 판 하나는 항상 남는다. 쓰기가 아예 실패하면 이후 쓰기가 성공할 때까지 메모리
     * 저장소가 기준이다. 호출자는 잠금을 잡고 있어야 한다.
     */
    private void persist() {
        File dir = file.getParentFile();
        File tmp = new File(file.getPath() + ".tmp");
        try {
            if (dir != null) {
                dir.mkdirs();
            }
            byte[] bytes = StorageFile.encode(areas);
            OutputStream out = new FileOutputStream(tmp);
            try {
                out.write(bytes);
                out.flush();
            } finally {
                out.close();
            }
            if (!tmp.renameTo(file)) {
                File bak = new File(file.getPath() + ".bak");
                bak.delete();
                boolean moved = !file.exists() || file.renameTo(bak);
                if (moved && tmp.renameTo(file)) {
                    bak.delete();
                } else {
                    // 새 파일을 옮기지 못했으면 옛 파일을 되돌려 완전한 판을 하나 남긴다.
                    if (moved && !file.exists()) {
                        bak.renameTo(file);
                    }
                    tmp.delete();
                    throw new IOException("cannot replace the storage file");
                }
            }
            persistFailed = false;
            loaded = true;
            knownExists = file.exists();
            knownModified = file.lastModified();
            knownLength = file.length();
        } catch (IOException e) {
            tmp.delete();
            persistFailed = true;
        }
    }

    /**
     * Returns the total size in code units of all origins.
     * <p>
     * 모든 오리진의 코드 단위 수 합계를 반환한다. 호출자는 잠금을 잡고 있어야 한다.
     *
     * @return total size / 전체 크기
     */
    private long totalChars() {
        long n = 0;
        // 모든 오리진의 크기를 더한다.
        for (StorageArea a : areas.values()) {
            n += a.getSize();
        }
        return n;
    }

    /**
     * The view of one origin on the shared store.
     * <p>
     * 공용 저장소에 대한 한 오리진의 뷰.
     */
    private final class View implements WebStorage {
        /**
         * Serialized origin.
         * <p>
         * 직렬화된 오리진.
         */
        private final String origin;

        /**
         * Document that uses the view.
         * <p>
         * 뷰를 쓰는 문서.
         */
        private final Object source;

        /**
         * Creates the view.
         * <p>
         * 뷰를 만든다.
         *
         * @param origin serialized origin / 직렬화된 오리진
         * @param source user of the view / 뷰의 사용자
         */
        View(String origin, Object source) {
            this.origin = origin;
            this.source = source;
        }

        /**
         * Returns the number of items.
         * <p>
         * 항목의 수를 반환한다.
         *
         * @return length / 길이
         */
        public int getLength() {
            synchronized (lock) {
                refresh();
                StorageArea a = areas.get(origin);
                return a == null ? 0 : a.getLength();
            }
        }

        /**
         * Returns the key at an index.
         * <p>
         * 위치의 키를 반환한다.
         *
         * @param index index / 위치
         * @return key or null / 키 또는 null
         */
        public String key(int index) {
            synchronized (lock) {
                refresh();
                StorageArea a = areas.get(origin);
                return a == null ? null : a.key(index);
            }
        }

        /**
         * Returns the value of an item.
         * <p>
         * 항목의 값을 반환한다.
         *
         * @param key key / 키
         * @return value or null / 값 또는 null
         */
        public String getItem(String key) {
            synchronized (lock) {
                refresh();
                StorageArea a = areas.get(origin);
                return a == null ? null : a.getItem(key);
            }
        }

        /**
         * Stores an item, persists the store and reports a real change.
         * <p>
         * 항목을 저장하고 저장소를 쓰며 실제 변경을 보고한다.
         *
         * @param key   key / 키
         * @param value value / 값
         * @throws StorageQuotaException if a limit would be exceeded / 제한을 넘게 되면
         */
        public void setItem(String key, String value) {
            if (key == null || value == null) {
                throw new IllegalArgumentException("key and value must not be null");
            }
            String old;
            synchronized (lock) {
                refresh();
                StorageArea a = areas.get(origin);
                boolean isNew = a == null;
                if (isNew) {
                    if (areas.size() >= StorageLimits.MAX_ORIGINS) {
                        throw new StorageQuotaException("too many origins use localStorage");
                    }
                    a = new StorageArea();
                }
                String previous = a.getItem(key);
                long delta = value.length() + (previous == null ? key.length() : -previous.length());
                if (totalChars() + delta > StorageLimits.MAX_TOTAL_CHARS) {
                    throw new StorageQuotaException("the total localStorage quota would be exceeded");
                }
                old = a.put(key, value);
                if (isNew) {
                    areas.put(origin, a);
                }
                if (old != null && old.equals(value)) {
                    return;
                }
                persist();
            }
            listeners.fire(origin, key, old, value, source);
        }

        /**
         * Removes an item, persists the store and reports a real change.
         * <p>
         * 항목을 제거하고 저장소를 쓰며 실제 변경을 보고한다.
         *
         * @param key key / 키
         */
        public void removeItem(String key) {
            String old;
            synchronized (lock) {
                refresh();
                StorageArea a = areas.get(origin);
                old = a == null ? null : a.remove(key);
                if (old == null) {
                    return;
                }
                if (a.getLength() == 0) {
                    areas.remove(origin);
                }
                persist();
            }
            listeners.fire(origin, key, old, null, source);
        }

        /**
         * Removes all items of the origin, persists the store and reports it if there were any.
         * <p>
         * 오리진의 모든 항목을 제거하고 저장소를 쓰며 항목이 있었으면 보고한다.
         */
        public void clear() {
            synchronized (lock) {
                refresh();
                StorageArea a = areas.remove(origin);
                if (a == null || a.getLength() == 0) {
                    return;
                }
                persist();
            }
            listeners.fire(origin, null, null, null, source);
        }
    }
}
