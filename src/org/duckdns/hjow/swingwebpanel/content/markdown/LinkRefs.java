package org.duckdns.hjow.swingwebpanel.content.markdown;

import java.util.HashMap;
import java.util.Map;

/**
 * The link reference definitions of one document (<code>[label]: destination "title"</code>). The first definition of a label wins. To keep
 * a small input from producing a huge output (one long definition used thousands of times), the total size of all destinations and
 * titles handed out is limited to the larger of 1,000,000 characters and four times the length of the input; after that no reference matches.
 * <p>
 * 문서 하나의 링크 참조 정의(<code>[label]: destination "title"</code>). 같은 라벨은 처음 정의가 이긴다. 작은 입력이 거대한 출력을 만들지 못하도록
 * (긴 정의 하나를 수천 번 쓰는 경우) 내어 주는 목적지와 제목 크기의 합계를 1,000,000자와 입력 길이의 4배 중 큰 값으로 제한하며, 그 뒤로는 어떤
 * 참조도 일치하지 않는다.
 */
final class LinkRefs {

    /** A definition: destination and title. 정의 하나: 목적지와 제목. */
    static final class Ref {
        /** Normalized destination. 정규화한 목적지. */
        final String destination;

        /** Title, empty if none. 제목. 없으면 빈 문자열. */
        final String title;

        /**
         * Creates a definition.
         * <p>
         * 정의를 만든다.
         *
         * @param destination normalized destination / 정규화한 목적지
         * @param title       title / 제목
         */
        Ref(String destination, String title) {
            this.destination = destination;
            this.title = title;
        }
    }

    /** Definitions by normalized label. 정규화한 라벨별 정의. */
    private final Map<String, Ref> map = new HashMap<String, Ref>();

    /** Characters that may still be handed out. 앞으로 내어 줄 수 있는 문자 수. */
    private long budget;

    /**
     * Creates an empty set of definitions.
     * <p>
     * 빈 정의 집합을 만든다.
     *
     * @param inputLength length of the Markdown input / Markdown 입력의 길이
     */
    LinkRefs(int inputLength) {
        this.budget = Math.max(1000000L, 4L * inputLength);
    }

    /**
     * Adds a definition unless the label is already defined.
     * <p>
     * 라벨이 아직 정의되지 않았으면 정의를 추가한다.
     *
     * @param label       normalized label / 정규화한 라벨
     * @param destination normalized destination / 정규화한 목적지
     * @param title       title / 제목
     */
    void add(String label, String destination, String title) {
        if (!map.containsKey(label)) {
            map.put(label, new Ref(destination, title));
        }
    }

    /**
     * Looks up a definition and charges its size to the budget.
     * <p>
     * 정의를 찾고 그 크기를 예산에서 차감한다.
     *
     * @param label normalized label / 정규화한 라벨
     * @return the definition, or null if undefined or the budget is used up / 정의. 정의가 없거나 예산이 다 쓰였으면 null
     */
    Ref get(String label) {
        Ref r = map.get(label);
        if (r == null) {
            return null;
        }
        long cost = r.destination.length() + r.title.length() + 1;
        if (cost > budget) {
            return null;
        }
        budget -= cost;
        return r;
    }
}
