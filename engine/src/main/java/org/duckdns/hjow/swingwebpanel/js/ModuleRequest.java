package org.duckdns.hjow.swingwebpanel.js;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * A module specifier and its immutable import attributes. Attribute order does not affect request equality.
 * <p>
 * 모듈 지정자와 변경할 수 없는 가져오기 속성. 속성의 순서는 요청의 동일성에 영향을 주지 않는다.
 */
public final class ModuleRequest {

    /** The module specifier or resolved key. / 모듈 지정자 또는 해석된 키. */
    private final String specifier;

    /** Import attributes ordered by name. / 이름순으로 정렬한 가져오기 속성. */
    private final Map<String, String> attributes;

    /**
     * Creates a request with no attributes.
     * <p>
     * 속성이 없는 요청을 만든다.
     *
     * @param specifier module specifier / 모듈 지정자
     */
    public ModuleRequest(String specifier) {
        this(specifier, Collections.<String, String>emptyMap());
    }

    /**
     * Creates a request and copies its attributes.
     * <p>
     * 요청을 만들며 속성을 복사한다.
     *
     * @param specifier module specifier / 모듈 지정자
     * @param attributes string-valued import attributes / 문자열 값을 가진 가져오기 속성
     */
    public ModuleRequest(String specifier, Map<String, String> attributes) {
        if (specifier == null || attributes == null) {
            throw new IllegalArgumentException("Module specifier and attributes must be non-null");
        }
        Map<String, String> copy = new TreeMap<String, String>();
        // 호출자가 제공하는 Map의 구현에 의존하지 않고 키와 값을 검사한다.
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("Module attribute names and values must be non-null");
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        this.specifier = specifier;
        this.attributes = Collections.unmodifiableMap(copy);
    }

    /**
     * Gets the specifier or a resolved host key.
     * <p>
     * 지정자 또는 해석된 호스트 키를 반환한다.
     *
     * @return module specifier / 모듈 지정자
     */
    public String getSpecifier() {
        return specifier;
    }

    /**
     * Returns the read-only attributes of this request.
     * <p>
     * 이 요청의 읽기 전용 속성을 반환한다.
     *
     * @return immutable attributes / 변경할 수 없는 속성
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Checks module request equality by specifier and attribute key/value pairs.
     * <p>
     * 지정자와 속성의 키·값 쌍으로 요청의 동일성을 검사한다.
     *
     * @param other another value / 다른 값
     * @return true for the same request / 같은 요청이면 true
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ModuleRequest)) {
            return false;
        }
        ModuleRequest request = (ModuleRequest) other;
        return specifier.equals(request.specifier) && attributes.equals(request.attributes);
    }

    /**
     * Returns a hash consistent with request equality.
     * <p>
     * 요청의 동일성에 맞는 해시를 반환한다.
     *
     * @return request hash / 요청 해시
     */
    @Override
    public int hashCode() {
        return 31 * specifier.hashCode() + attributes.hashCode();
    }

    /**
     * Formats this request for diagnosis.
     * <p>
     * 진단에 사용할 요청 문자열을 반환한다.
     *
     * @return specifier and optional attributes / 지정자와 선택적인 속성
     */
    @Override
    public String toString() {
        return attributes.isEmpty() ? specifier : specifier + " with " + attributes;
    }
}
