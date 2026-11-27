package org.duckdns.hjow.swingwebpanel.tls13;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader of the line based test vector resources of the TLS module. A line is a list of words separated by single spaces; lines that
 * start with <code>#</code> and empty lines are skipped. Example:
 * <code>String ux = Vectors.load("/ec-vectors.txt").value("P-256", "Ux");</code>.
 * <p>
 * TLS 모듈의 줄 단위 시험 벡터 자료를 읽는다. 한 줄은 공백 하나로 나눈 낱말 목록이고, <code>#</code> 로 시작하는 줄과 빈 줄은 건너뛴다.
 */
public final class Vectors {

    /** The lines, split into words. / 낱말로 나눈 줄들. */
    private final List<String[]> lines = new ArrayList<String[]>();

    /**
     * Private constructor; use {@link #load(String)}.
     * <p>
     * 비공개 생성자. {@link #load(String)} 를 쓴다.
     */
    private Vectors() {
    }

    /**
     * Loads a resource from the test class path.
     * <p>
     * 시험 클래스패스에서 자료를 읽는다.
     *
     * @param resource the absolute resource name, for example <code>/ec-vectors.txt</code> / 절대 자료 이름. 예: <code>/ec-vectors.txt</code>
     * @return the vectors / 벡터
     */
    public static Vectors load(String resource) {
        Vectors v = new Vectors();
        try {
            InputStream in = Vectors.class.getResourceAsStream(resource);
            if (in == null) {
                throw new IllegalStateException("test resource " + resource + " is missing");
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            try {
                String line;
                // 주석과 빈 줄을 빼고 낱말로 나눠 모은다.
                while ((line = r.readLine()) != null) {
                    String s = line.trim();
                    if (s.length() > 0 && s.charAt(0) != '#') {
                        v.lines.add(s.split(" "));
                    }
                }
            } finally {
                r.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + resource + ": " + e);
        }
        return v;
    }

    /**
     * Returns the last word of the first line that starts with the given words.
     * <p>
     * 주어진 낱말들로 시작하는 첫 줄의 마지막 낱말을 반환한다.
     *
     * @param prefix the leading words / 앞쪽 낱말들
     * @return the value / 값
     */
    public String value(String... prefix) {
        List<String[]> found = all(prefix);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("no vector line starts with " + java.util.Arrays.toString(prefix));
        }
        String[] words = found.get(0);
        return words[words.length - 1];
    }

    /**
     * Returns all lines that start with the given words.
     * <p>
     * 주어진 낱말들로 시작하는 모든 줄을 반환한다.
     *
     * @param prefix the leading words / 앞쪽 낱말들
     * @return the lines as word arrays / 낱말 배열로 된 줄들
     */
    public List<String[]> all(String... prefix) {
        List<String[]> out = new ArrayList<String[]>();
        // 줄마다 앞쪽 낱말이 맞는지 본다.
        for (int i = 0; i < lines.size(); i++) {
            String[] words = lines.get(i);
            boolean match = words.length >= prefix.length;
            // 낱말을 차례로 비교한다.
            for (int k = 0; match && k < prefix.length; k++) {
                match = words[k].equals(prefix[k]);
            }
            if (match) {
                out.add(words);
            }
        }
        return out;
    }
}
