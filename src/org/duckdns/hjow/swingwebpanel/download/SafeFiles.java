package org.duckdns.hjow.swingwebpanel.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

/**
 * File operations of the download manager that must never lose a file: creating a uniquely named partial file next to the target and
 * moving a finished file over an existing one. JRE 6 cannot rename over an existing file on Windows and renames can fail between
 * volumes, so the replacement moves the old file aside first, falls back to copying, and puts the old file back if anything fails.
 * <p>
 * 파일을 잃어서는 안 되는 다운로드 관리자의 파일 작업: 대상 옆에 고유한 이름의 부분 파일 만들기와 끝난 파일을 기존 파일 위로 옮기기.
 * JRE 6 은 Windows 에서 기존 파일 위로 이름을 바꿀 수 없고 이름 바꾸기는 볼륨 사이에서 실패할 수 있으므로, 교체는 옛 파일을 먼저 옆으로
 * 옮기고, 복사로 대신하며, 무엇이든 실패하면 옛 파일을 되돌린다.
 */
final class SafeFiles {

    /** Source of the random part of file names. 파일 이름의 난수 부분의 원천. */
    private static final Random RANDOM = new Random();

    /** Private constructor: static helper class. 비공개 생성자: 정적 도우미 클래스이다. */
    private SafeFiles() {
    }

    /**
     * Creates a new empty partial file <code>name.part-xxxxxxxx</code> in a directory (the directory is created if needed).
     * <p>
     * 디렉터리에 새 빈 부분 파일 <code>name.part-xxxxxxxx</code> 를 만든다(디렉터리가 없으면 만든다).
     *
     * @param dir  the target directory / 대상 디렉터리
     * @param name the target file name / 대상 파일 이름
     * @return the created file / 만든 파일
     * @throws IOException if the file cannot be created / 파일을 만들 수 없으면
     */
    static File newPartFile(File dir, String name) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("cannot create the directory " + dir);
        }
        // 같은 이름이 이미 있으면 다른 난수로 다시 시도한다.
        for (int attempt = 0; attempt < 100; attempt++) {
            String hex;
            synchronized (RANDOM) {
                hex = Integer.toHexString(RANDOM.nextInt());
            }
            File f = new File(dir, name + ".part-" + hex);
            if (f.createNewFile()) {
                return f;
            }
        }
        throw new IOException("cannot create a partial file in " + dir);
    }

    /**
     * Moves a finished file to its target, replacing an existing file without ever leaving the target missing: on failure the old
     * file is restored and the source stays where it is.
     * <p>
     * 끝난 파일을 대상으로 옮기며 기존 파일을 교체한다. 대상이 사라진 채로 남지 않으며, 실패하면 옛 파일을 되돌리고 원본은 그대로 둔다.
     *
     * @param src the finished file / 끝난 파일
     * @param dst the target file / 대상 파일
     * @throws IOException if the file could not be moved / 옮길 수 없으면
     */
    static void moveReplacing(File src, File dst) throws IOException {
        File parent = dst.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("cannot create the directory " + parent);
        }
        if (dst.isDirectory()) {
            throw new IOException(dst + " is a directory");
        }
        File old = null;
        if (dst.exists()) {
            old = new File(dst.getPath() + ".old-" + Integer.toHexString(RANDOM.nextInt()));
            // 기존 파일을 옆으로 옮겨 두면 대상 자리가 비어 이름 바꾸기가 된다.
            if (!dst.renameTo(old)) {
                throw new IOException("cannot move the existing file " + dst + " aside");
            }
        }
        boolean moved = src.renameTo(dst);
        if (!moved) {
            // 볼륨이 다르면 이름 바꾸기가 실패하므로 같은 디렉터리에 복사한 뒤 이름을 바꾼다.
            File copy = null;
            try {
                copy = newPartFile(parent == null ? new File(".") : parent, dst.getName());
                copyFile(src, copy);
                moved = copy.renameTo(dst);
            } catch (IOException e) {
                moved = false;
            } finally {
                if (!moved && copy != null) {
                    copy.delete();
                }
            }
            if (moved) {
                src.delete();
            }
        }
        if (!moved) {
            // 실패하면 옛 파일을 되돌린다.
            if (old != null) {
                old.renameTo(dst);
            }
            throw new IOException("cannot move " + src + " to " + dst);
        }
        if (old != null) {
            old.delete();
        }
    }

    /**
     * Copies the content of a file.
     * <p>
     * 파일 내용을 복사한다.
     *
     * @param src source / 원본
     * @param dst destination (overwritten) / 대상(덮어씀)
     * @throws IOException on failure / 실패 시
     */
    static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                byte[] buf = new byte[64 * 1024];
                int n;
                // 원본을 끝까지 읽어 대상에 쓴다.
                while ((n = in.read(buf)) >= 0) {
                    out.write(buf, 0, n);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}
