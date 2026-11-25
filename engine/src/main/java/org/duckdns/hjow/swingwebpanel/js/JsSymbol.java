package org.duckdns.hjow.swingwebpanel.js;

/**
 * The ECMAScript Symbol value. Every instance is unique even when descriptions are equal, so compare with <code>==</code>.
 * A symbol is a property key distinct from any string key. Symbols created by <code>Symbol.for</code> carry a registry key.
 * <p>
 * ECMAScript 의 Symbol 값. 설명이 같아도 인스턴스마다 고유하므로 <code>==</code> 로 비교한다.
 * 심볼은 어떤 문자열 키와도 구별되는 프로퍼티 키이다. <code>Symbol.for</code> 로 만든 심볼은 레지스트리 키를 가진다.
 */
public final class JsSymbol extends JsWeakKey {

    /**
     * Optional description (null if undefined).
     * <p>
     * 선택적 설명(없으면 null).
     */
    private final String description;

    /**
     * Global registry key for symbols created by Symbol.for, otherwise null.
     * <p>
     * Symbol.for 로 만든 심볼의 전역 레지스트리 키. 아니면 null.
     */
    private final String registryKey;

    /** Whether this is one of the engine's well-known symbols. / 엔진의 well-known 심볼인지 여부. */
    private final boolean wellKnown;

    /**
     * Creates a new unique symbol.
     * <p>
     * 새 고유 심볼을 만든다.
     *
     * @param description description or null / 설명 또는 null
     */
    public JsSymbol(String description) {
        this(description, null);
    }

    /**
     * Creates a symbol with a registry key (used by the Symbol.for registry).
     * <p>
     * 레지스트리 키가 있는 심볼을 만든다(Symbol.for 레지스트리가 사용).
     *
     * @param description description or null / 설명 또는 null
     * @param registryKey registry key or null / 레지스트리 키 또는 null
     */
    public JsSymbol(String description, String registryKey) {
        this(description, registryKey, false);
    }

    /**
     * Creates a symbol for the engine's well-known symbol table.
     * <p>
     * 엔진의 well-known 심볼 표에 넣을 심볼을 만든다.
     *
     * @param description the symbol description / 심볼 설명
     * @param wellKnown true for a well-known symbol / well-known 심볼이면 true
     */
    JsSymbol(String description, boolean wellKnown) {
        this(description, null, wellKnown);
    }

    /**
     * Stores the description, registry key and well-known status.
     * <p>
     * 설명, 레지스트리 키, well-known 여부를 보관한다.
     *
     * @param description the symbol description / 심볼 설명
     * @param registryKey registry key or null / 레지스트리 키 또는 null
     * @param wellKnown whether the symbol is well-known / well-known 심볼 여부
     */
    private JsSymbol(String description, String registryKey, boolean wellKnown) {
        this.description = description;
        this.registryKey = registryKey;
        this.wellKnown = wellKnown;
    }

    /**
     * Returns the description, or null if the symbol was created without one.
     * <p>
     * 설명을 반환한다. 설명 없이 만들었으면 null.
     *
     * @return description or null / 설명 또는 null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the global registry key, or null if the symbol is not in the registry.
     * <p>
     * 전역 레지스트리 키를 반환한다. 레지스트리에 없으면 null.
     *
     * @return registry key or null / 레지스트리 키 또는 null
     */
    public String getRegistryKey() {
        return registryKey;
    }

    /**
     * Reports whether the engine permanently holds this well-known symbol.
     * <p>
     * 엔진이 이 well-known 심볼을 영구적으로 유지하는지 알려 준다.
     *
     * @return whether this is a well-known symbol / well-known 심볼 여부
     */
    public boolean isWellKnown() {
        return wellKnown;
    }

    /**
     * Returns {@link JsType#SYMBOL}.
     * <p>
     * {@link JsType#SYMBOL} 을 반환한다.
     *
     * @return the type / 형식
     */
    public JsType type() {
        return JsType.SYMBOL;
    }

    /**
     * Returns the SymbolDescriptiveString, for example <code>Symbol(foo)</code>.
     * <p>
     * SymbolDescriptiveString 을 반환한다. 예: <code>Symbol(foo)</code>.
     *
     * @return descriptive string / 설명 문자열
     */
    public String toString() {
        return "Symbol(" + (description == null ? "" : description) + ")";
    }
}
