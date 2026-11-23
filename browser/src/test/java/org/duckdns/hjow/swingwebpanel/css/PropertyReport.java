package org.duckdns.hjow.swingwebpanel.css;

import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.Property;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.props.Shorthand;

/**
 * Prints the table of supported properties as markdown (used to keep <code>docs/progress/css.md</code> in step with the property table). Run with
 * <code>java -cp build/pkg-css/classes:build/pkg-css/test-classes org.duckdns.hjow.swingwebpanel.css.PropertyReport</code>.
 * <p>
 * 지원하는 속성의 표를 마크다운으로 출력한다(<code>docs/progress/css.md</code> 를 속성 표와 맞추는 데 쓴다). 실행:
 * <code>java -cp build/pkg-css/classes:build/pkg-css/test-classes org.duckdns.hjow.swingwebpanel.css.PropertyReport</code>.
 */
public final class PropertyReport {

    /**
     * Private constructor: only the main method is used.
     * <p>
     * 비공개 생성자: main 메서드만 쓴다.
     */
    private PropertyReport() {
    }

    /**
     * Prints the report.
     * <p>
     * 보고서를 출력한다.
     *
     * @param args unused / 쓰지 않음
     */
    public static void main(String[] args) {
        PropertyRegistry reg = PropertyRegistry.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("| 속성 | 종류 | 상속 | 초기값 | 상태 |\n| --- | --- | --- | --- | --- |\n");
        int applied = 0;
        int parsedOnly = 0;
        // 개별 속성을 id 순서로 나열한다.
        for (int id = 1; id <= Prop.COUNT; id++) {
            Property p = reg.byId(id);
            sb.append("| `").append(p.getName()).append("` | 개별 | ").append(p.isInherited() ? "예" : "아니오").append(" | `").append(p.getInitial().toCssText())
                    .append("` | ").append(p.isParsedOnly() ? "파싱만(렌더링 영향 없음)" : "파싱+계산 값").append(" |\n");
            if (p.isParsedOnly()) {
                parsedOnly++;
            } else {
                applied++;
            }
        }
        List<String> names = reg.names();
        int shorthands = 0;
        // 단축 속성을 나열한다.
        for (int i = 0; i < names.size(); i++) {
            String n = names.get(i);
            Shorthand s = reg.getShorthand(n);
            if (s != null) {
                shorthands++;
                sb.append("| `").append(n).append("` | 단축(").append(s.getLonghands().length).append("개 개별 속성) | - | - | 전개 구현 |\n");
            }
        }
        sb.append("| `all` | 단축(CSS 전역 키워드만) | - | - | 전개 구현 |\n");
        sb.append("\n개별 속성 ").append(Prop.COUNT).append("개(파싱+계산 값 ").append(applied).append(", 파싱만 ").append(parsedOnly).append("), 단축 속성 ")
                .append(shorthands + 1).append("개.\n");
        sb.append("\n별칭(접두사 붙은 이름 등): ").append(reg.aliasNames().size()).append("개. 예: ");
        int shown = 0;
        // 별칭 몇 개를 예로 보인다.
        for (Map.Entry<String, String> e : reg.aliasNames().entrySet()) {
            if (shown++ < 6) {
                sb.append('`').append(e.getKey()).append("`→`").append(e.getValue()).append("` ");
            }
        }
        sb.append("\n\n알아보지만 지원하지 않는 이름(선언을 버리고 `unsupported` 진단): ").append(reg.unsupportedNames().size()).append("개 — ");
        List<String> un = reg.unsupportedNames();
        // 지원하지 않는 이름을 나열한다.
        for (int i = 0; i < un.size(); i++) {
            sb.append(i > 0 ? ", " : "").append('`').append(un.get(i)).append('`');
        }
        sb.append('\n');
        System.out.print(sb);
    }
}
