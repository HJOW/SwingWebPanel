package org.duckdns.hjow.swingwebpanel.js;

/**
 * The well-known symbols of ECMA-262 6th edition 6.1.5.1. They are shared by all realms.
 * <p>
 * ECMA-262 6판 6.1.5.1 의 잘 알려진 심볼. 모든 realm 이 공유한다.
 */
public final class WellKnownSymbols {

    /**
     * The well-known symbol Symbol.hasInstance.
     * <p>
     * 잘 알려진 심볼 Symbol.hasInstance 를 나타낸다.
     */
    public static final JsSymbol HAS_INSTANCE = new JsSymbol("Symbol.hasInstance");

    /**
     * The well-known symbol Symbol.isConcatSpreadable.
     * <p>
     * 잘 알려진 심볼 Symbol.isConcatSpreadable 를 나타낸다.
     */
    public static final JsSymbol IS_CONCAT_SPREADABLE = new JsSymbol("Symbol.isConcatSpreadable");

    /**
     * The well-known symbol Symbol.iterator.
     * <p>
     * 잘 알려진 심볼 Symbol.iterator 를 나타낸다.
     */
    public static final JsSymbol ITERATOR = new JsSymbol("Symbol.iterator");

    /**
     * The well-known symbol Symbol.asyncIterator.
     * <p>
     * 잘 알려진 심볼 Symbol.asyncIterator.
     */
    public static final JsSymbol ASYNC_ITERATOR = new JsSymbol("Symbol.asyncIterator");

    /**
     * The well-known symbol Symbol.match.
     * <p>
     * 잘 알려진 심볼 Symbol.match 를 나타낸다.
     */
    public static final JsSymbol MATCH = new JsSymbol("Symbol.match");

    /**
     * The well-known symbol Symbol.matchAll.
     * <p>
     * 잘 알려진 심볼 Symbol.matchAll.
     */
    public static final JsSymbol MATCH_ALL = new JsSymbol("Symbol.matchAll");

    /**
     * The well-known symbol Symbol.replace.
     * <p>
     * 잘 알려진 심볼 Symbol.replace 를 나타낸다.
     */
    public static final JsSymbol REPLACE = new JsSymbol("Symbol.replace");

    /**
     * The well-known symbol Symbol.search.
     * <p>
     * 잘 알려진 심볼 Symbol.search 를 나타낸다.
     */
    public static final JsSymbol SEARCH = new JsSymbol("Symbol.search");

    /**
     * The well-known symbol Symbol.species.
     * <p>
     * 잘 알려진 심볼 Symbol.species 를 나타낸다.
     */
    public static final JsSymbol SPECIES = new JsSymbol("Symbol.species");

    /**
     * The well-known symbol Symbol.split.
     * <p>
     * 잘 알려진 심볼 Symbol.split 를 나타낸다.
     */
    public static final JsSymbol SPLIT = new JsSymbol("Symbol.split");

    /**
     * The well-known symbol Symbol.toPrimitive.
     * <p>
     * 잘 알려진 심볼 Symbol.toPrimitive 를 나타낸다.
     */
    public static final JsSymbol TO_PRIMITIVE = new JsSymbol("Symbol.toPrimitive");

    /**
     * The well-known symbol Symbol.toStringTag.
     * <p>
     * 잘 알려진 심볼 Symbol.toStringTag 를 나타낸다.
     */
    public static final JsSymbol TO_STRING_TAG = new JsSymbol("Symbol.toStringTag");

    /**
     * The well-known symbol Symbol.unscopables.
     * <p>
     * 잘 알려진 심볼 Symbol.unscopables 를 나타낸다.
     */
    public static final JsSymbol UNSCOPABLES = new JsSymbol("Symbol.unscopables");

    /**
     * Private constructor: constants holder.
     * <p>
     * 비공개 생성자: 상수 보관용 클래스이다.
     */
    private WellKnownSymbols() {
    }
}
