package org.duckdns.hjow.swingwebpanel.tls13;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader of the example traces of RFC 8448 ("Example Handshake Traces for TLS 1.3"). The text of the RFC is a test resource
 * (<code>rfc8448.txt</code>, fetched from https://www.rfc-editor.org/rfc/rfc8448.txt on 2026-11-27), so the test vectors are taken from the
 * original instead of being typed in. A trace is a list of steps such as <code>{server}  derive secret "tls13 c hs traffic"</code>, each
 * with named octet strings. Example:
 * <code>byte[] v = Rfc8448.load().step("3.", "{server}  derive secret \"tls13 c hs traffic\"", 0).get("expanded");</code>.
 * <p>
 * RFC 8448("Example Handshake Traces for TLS 1.3")의 예시 추적을 읽는다. RFC 원문이 시험 자료(<code>rfc8448.txt</code>, 2026-11-27 에
 * https://www.rfc-editor.org/rfc/rfc8448.txt 에서 받음)이므로 시험 벡터를 손으로 옮기지 않고 원문에서 가져온다. 추적은
 * <code>{server}  derive secret "tls13 c hs traffic"</code> 같은 단계의 목록이고, 단계마다 이름 붙은 옥텟 문자열이 있다.
 */
public final class Rfc8448 {

    /**
     * One step of a trace with its named octet strings.
     * <p>
     * 추적의 단계 하나와 그 이름 붙은 옥텟 문자열들.
     */
    public static final class Step {

        /** The number of the section that the step belongs to, for example <code>3.</code>. / 단계가 속한 절 번호. 예: <code>3.</code>. */
        final String section;

        /** The title without the trailing colon. / 끝의 콜론을 뺀 제목. */
        final String title;

        /** The names of the items in order. / 항목 이름(순서대로). */
        final List<String> names = new ArrayList<String>();

        /** The values of the items in order. / 항목 값(순서대로). */
        final List<byte[]> values = new ArrayList<byte[]>();

        /**
         * Creates a step.
         * <p>
         * 단계를 만든다.
         *
         * @param section the section number / 절 번호
         * @param title   the title / 제목
         */
        Step(String section, String title) {
            this.section = section;
            this.title = title;
        }

        /**
         * Returns the title of the step.
         * <p>
         * 단계의 제목을 반환한다.
         *
         * @return the title / 제목
         */
        public String title() {
            return title;
        }

        /**
         * Returns the first item with the name.
         * <p>
         * 그 이름의 첫 항목을 반환한다.
         *
         * @param name the item name, for example <code>expanded</code> / 항목 이름. 예: <code>expanded</code>
         * @return the octets / 옥텟
         */
        public byte[] get(String name) {
            return get(name, 0);
        }

        /**
         * Returns the n-th item with the name.
         * <p>
         * 그 이름의 n번째 항목을 반환한다.
         *
         * @param name the item name / 항목 이름
         * @param nth  zero based occurrence / 0부터 세는 순번
         * @return the octets / 옥텟
         */
        public byte[] get(String name, int nth) {
            int seen = 0;
            // 같은 이름의 항목을 순서대로 센다.
            for (int i = 0; i < names.size(); i++) {
                if (names.get(i).equals(name) && seen++ == nth) {
                    return values.get(i).clone();
                }
            }
            throw new IllegalArgumentException("no item " + name + " #" + nth + " in step " + title + " (items " + names + ")");
        }
    }

    /** The shared instance. / 공유 인스턴스. */
    private static Rfc8448 instance;

    /** All steps of the document in order. / 문서의 모든 단계(순서대로). */
    private final List<Step> steps = new ArrayList<Step>();

    /**
     * Parses the lines of the RFC.
     * <p>
     * RFC 의 줄을 해석한다.
     *
     * @param lines the lines without page headers and footers / 쪽 머리글과 바닥글을 뺀 줄
     */
    private Rfc8448(List<String> lines) {
        String section = "";
        Step step = null;
        int i = 0;
        // 줄을 차례로 보며 절, 단계, 항목을 모은다.
        while (i < lines.size()) {
            String line = lines.get(i++);
            String trimmed = line.trim();
            if (line.length() > 0 && Character.isDigit(line.charAt(0)) && line.indexOf(".  ") > 0) {
                section = line.substring(0, line.indexOf(".  ") + 1);
                step = null;
            } else if (trimmed.startsWith("{client}") || trimmed.startsWith("{server}")) {
                String title = trimmed;
                // 제목이 여러 줄이면 콜론으로 끝나는 줄까지 잇는다.
                while (!title.endsWith(":") && i < lines.size() && lines.get(i).trim().length() > 0) {
                    title = title + " " + lines.get(i++).trim();
                }
                if (title.endsWith(":")) {
                    title = title.substring(0, title.length() - 1);
                }
                step = new Step(section, title);
                steps.add(step);
            } else if (step != null) {
                int open = trimmed.indexOf(" (");
                int close = trimmed.indexOf(" octets):");
                if (open > 0 && close > open) {
                    String name = trimmed.substring(0, open);
                    int count = Integer.parseInt(trimmed.substring(open + 2, close));
                    byte[] value = new byte[count];
                    int have = 0;
                    String rest = trimmed.substring(close + " octets):".length());
                    // 선언한 옥텟 수를 채울 때까지 16진수 쌍을 읽는다(빈 줄은 건너뛴다).
                    while (true) {
                        String[] tokens = rest.trim().split("\\s+");
                        // 줄 안의 16진수 쌍을 차례로 넣는다.
                        for (int t = 0; t < tokens.length && have < count; t++) {
                            if (tokens[t].length() == 2 && Character.digit(tokens[t].charAt(0), 16) >= 0
                                    && Character.digit(tokens[t].charAt(1), 16) >= 0) {
                                value[have++] = (byte) Integer.parseInt(tokens[t], 16);
                            }
                        }
                        if (have >= count || i >= lines.size()) {
                            break;
                        }
                        rest = lines.get(i++);
                    }
                    if (have != count) {
                        throw new IllegalStateException("item " + name + " of " + step.title + " is truncated");
                    }
                    step.names.add(name);
                    step.values.add(value);
                }
            }
        }
    }

    /**
     * Loads and parses the RFC text once.
     * <p>
     * RFC 원문을 한 번 읽어 해석한다.
     *
     * @return the parsed document / 해석한 문서
     */
    public static synchronized Rfc8448 load() {
        if (instance == null) {
            try {
                InputStream in = Rfc8448.class.getResourceAsStream("/rfc8448.txt");
                if (in == null) {
                    throw new IllegalStateException("test resource rfc8448.txt is missing");
                }
                BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                List<String> lines = new ArrayList<String>();
                try {
                    String line;
                    // 쪽 머리글, 바닥글, 쪽 나눔 문자를 빼고 모은다.
                    while ((line = r.readLine()) != null) {
                        String s = line.replace("\f", "");
                        if (s.startsWith("Thomson ") || s.startsWith("RFC 8448 ")) {
                            continue;
                        }
                        lines.add(s);
                    }
                } finally {
                    r.close();
                }
                instance = new Rfc8448(lines);
            } catch (IOException e) {
                throw new IllegalStateException("cannot read rfc8448.txt: " + e);
            }
        }
        return instance;
    }

    /**
     * Returns the n-th step with the title in a section.
     * <p>
     * 절 안에서 그 제목을 가진 n번째 단계를 반환한다.
     *
     * @param section the section number with the dot, for example <code>3.</code> / 점을 포함한 절 번호. 예: <code>3.</code>
     * @param title   the title without the colon; runs of spaces count as one / 콜론을 뺀 제목. 연속한 공백은 하나로 본다
     * @param nth     zero based occurrence / 0부터 세는 순번
     * @return the step / 단계
     */
    public Step step(String section, String title, int nth) {
        String want = title.replaceAll("\\s+", " ");
        int seen = 0;
        // 절과 제목이 맞는 단계를 순서대로 센다.
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            if (s.section.equals(section) && s.title.replaceAll("\\s+", " ").equals(want) && seen++ == nth) {
                return s;
            }
        }
        throw new IllegalArgumentException("no step " + title + " #" + nth + " in section " + section);
    }

    /**
     * Returns all steps of a section in order.
     * <p>
     * 절의 모든 단계를 순서대로 반환한다.
     *
     * @param section the section number with the dot / 점을 포함한 절 번호
     * @return the steps / 단계 목록
     */
    public List<Step> steps(String section) {
        List<Step> out = new ArrayList<Step>();
        // 그 절의 단계만 고른다.
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).section.equals(section)) {
                out.add(steps.get(i));
            }
        }
        return out;
    }
}
