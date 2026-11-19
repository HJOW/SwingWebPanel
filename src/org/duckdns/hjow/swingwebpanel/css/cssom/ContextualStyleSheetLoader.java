package org.duckdns.hjow.swingwebpanel.css.cssom;

import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Optional stylesheet loading policy scoped to an owning element and nested import media. A child policy inherits the
 * parent's conditions, allowing parser-blocking loads to wait only for applicable parser-created sheets.
 * <p>소유 요소와 중첩 import 미디어에 범위를 둔 선택적 스타일시트 로딩 정책. 자식 정책은 부모 조건을 상속하여 파서를 막는 로딩이
 * 적용 가능한 파서 생성 시트만 기다리게 한다. 일반 StyleSheetLoader 구현에는 영향을 주지 않는다.
 */
public interface ContextualStyleSheetLoader extends StyleSheetLoader {
    /** Creates a policy for one top-level owning element. / 최상위 소유 요소 하나의 정책을 만든다.
     * @param owner style or link element / style 또는 link 요소
     * @return scoped loader / 범위가 정해진 로더 */
    StyleSheetLoader forOwner(Element owner);

    /** Creates a policy inheriting the parent's conditions and this import's media. / 부모 조건과 이 import의 미디어를 상속하는 정책을 만든다.
     * @param media import media / import 미디어
     * @return scoped loader / 범위가 정해진 로더 */
    StyleSheetLoader forMedia(MediaQueryList media);
}
