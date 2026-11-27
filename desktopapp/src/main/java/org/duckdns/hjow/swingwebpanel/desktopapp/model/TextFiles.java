package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Reading and writing the UTF-8 text files of the user data (settings, bookmarks, session). A file is written into a temporary file of
 * the same directory first and then put in the place of the old one, so that a failure in the middle leaves the old file as it was. A
 * file that cannot be understood is kept under another name, so that the user can look at it, before the application starts with the
 * defaults.
 * <p>
 * 사용자 데이터(설정, 북마크, 세션)의 UTF-8 텍스트 파일을 읽고 쓰는 일. 파일은 먼저 같은 디렉터리의 임시 파일에 쓴 다음 이전 파일의 자리에 놓으므로,
 * 도중에 실패해도 이전 파일이 그대로 남는다. 해석할 수 없는 파일은 응용 프로그램이 기본값으로 시작하기 전에 사용자가 볼 수 있도록 다른 이름으로
 * 보존한다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * TextFiles.write(new File(dir, "settings.properties"), text);
 * String back = TextFiles.read(new File(dir, "settings.properties"));
 * </pre>
 */
public final class TextFiles {

    /** The ending of the name under which a file that cannot be understood is kept. / 해석할 수 없는 파일을 보존하는 이름의 끝부분. */
    public static final String BROKEN_SUFFIX = ".broken";

    /** The ending of the name of the temporary file that is written first. / 먼저 쓰는 임시 파일 이름의 끝부분. */
    private static final String TEMP_SUFFIX = ".tmp";

    /** The largest file that is read, in bytes; user data files are far smaller. / 읽는 파일의 최대 크기(바이트). 사용자 데이터 파일은 훨씬 작다. */
    private static final long MAX_BYTES = 16L * 1024 * 1024;

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private TextFiles() {
    }

    /**
     * Reads a whole file as UTF-8 text.
     * <p>
     * 파일 전체를 UTF-8 텍스트로 읽는다.
     *
     * @param file the file / 파일
     * @return the text, or null if the file does not exist / 텍스트. 파일이 없으면 null
     * @throws IOException if the file cannot be read or is too large / 파일을 읽을 수 없거나 너무 크면
     */
    public static String read(File file) throws IOException {
        if (!file.isFile()) {
            return null;
        }
        if (file.length() > MAX_BYTES) {
            throw new IOException("the file is too large: " + file);
        }
        InputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 파일 끝까지 읽는다.
            while ((n = in.read(buf)) > 0) {
                bytes.write(buf, 0, n);
            }
            String text = new String(bytes.toByteArray(), "UTF-8");
            // 다른 편집기가 붙인 바이트 순서 표시는 내용이 아니다.
            if (text.length() > 0 && text.charAt(0) == (char) 0xFEFF) {
                text = text.substring(1);
            }
            return text;
        } finally {
            in.close();
        }
    }

    /**
     * Writes text into a file as UTF-8: first into a temporary file of the same directory, which then takes the place of the file. The
     * directory is created if it does not exist. If the replacement fails, the old file stays as it was.
     * <p>
     * 텍스트를 UTF-8 로 파일에 쓴다: 먼저 같은 디렉터리의 임시 파일에 쓰고, 그 파일이 원래 파일의 자리를 차지한다. 디렉터리가 없으면 만든다.
     * 교체에 실패하면 이전 파일이 그대로 남는다.
     *
     * @param file the file / 파일
     * @param text the text / 텍스트
     * @throws IOException if the file cannot be written / 파일을 쓸 수 없으면
     */
    public static void write(File file, String text) throws IOException {
        File dir = file.getAbsoluteFile().getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("cannot create the directory " + dir);
        }
        File temp = new File(dir, file.getName() + TEMP_SUFFIX);
        OutputStream out = new FileOutputStream(temp);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
        // 이름 바꾸기가 기존 파일을 덮어쓰지 못하는 플랫폼(Windows)에서는 이전 파일을 잠시 다른 이름으로 옮긴다.
        if (temp.renameTo(file)) {
            return;
        }
        File old = new File(dir, file.getName() + ".old");
        old.delete();
        boolean moved = !file.exists() || file.renameTo(old);
        if (moved && temp.renameTo(file)) {
            old.delete();
            return;
        }
        // 교체에 실패했다. 옮겨 둔 이전 파일을 되돌리고 임시 파일을 지운다.
        if (moved && old.exists() && !file.exists()) {
            old.renameTo(file);
        }
        temp.delete();
        throw new IOException("cannot replace " + file);
    }

    /**
     * Keeps a file that cannot be understood under the name <code>name.broken</code> (replacing an older kept file), so that the
     * application can write a new file in its place.
     * <p>
     * 해석할 수 없는 파일을 <code>이름.broken</code> 으로 보존한다(이전에 보존한 파일은 바꾼다). 응용 프로그램이 그 자리에 새 파일을 쓸 수 있게
     * 한다.
     *
     * @param file the file / 파일
     * @return the kept file, or null if it could not be moved / 보존한 파일. 옮길 수 없었으면 null
     */
    public static File keepBroken(File file) {
        File kept = new File(file.getAbsoluteFile().getParentFile(), file.getName() + BROKEN_SUFFIX);
        kept.delete();
        return file.renameTo(kept) ? kept : null;
    }
}
