package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.css.cssom.CounterStyleRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRuleList;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.css.cssom.FontFaceRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.GroupRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.ImportRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.KeyframesRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.MarginRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.MediaList;
import org.duckdns.hjow.swingwebpanel.css.cssom.NamespaceRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.PageRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.PositionTryRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.PropertyRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The script interfaces of the CSS Object Model that wrap the objects of the CSS engine (CSSOM, W3C Working Draft 26 August 2021,
 * <code>https://www.w3.org/TR/cssom-1/</code>): <code>StyleSheet</code> and <code>CSSStyleSheet</code>, <code>MediaList</code>, <code>CSSRuleList</code>,
 * and the rule interfaces <code>CSSRule</code>, <code>CSSStyleRule</code>, <code>CSSGroupingRule</code>, <code>CSSConditionRule</code>,
 * <code>CSSMediaRule</code>, <code>CSSSupportsRule</code>, <code>CSSLayerBlockRule</code>, <code>CSSLayerStatementRule</code>,
 * <code>CSSImportRule</code>, <code>CSSFontFaceRule</code>, <code>CSSKeyframesRule</code>, <code>CSSKeyframeRule</code> and
 * <code>CSSNamespaceRule</code>, and the interfaces of the newer rules <code>CSSContainerRule</code>, <code>CSSScopeRule</code>,
 * <code>CSSStartingStyleRule</code>, <code>CSSPropertyRule</code>, <code>CSSCounterStyleRule</code>, <code>CSSPageRule</code> and
 * <code>CSSMarginRule</code>. A script reaches the very objects that the style resolver reads, so inserting a rule, changing a declaration, the
 * selector, the media or the disabled flag changes the page at once. A rule kind that has no interface of its own (<code>@font-feature-values</code>
 * ...) is a plain <code>CSSRule</code> with its <code>type</code> and <code>cssText</code>. Whether a
 * script may read the rules of a style sheet that came from another origin is decided by the {@link Guard} that the page gives (the CSSOM makes such
 * a sheet unreadable). The page-specific parts (<code>document.styleSheets</code>, constructed style sheets, <code>sheet</code> of the elements) are
 * installed by the page.
 * <p>
 * CSS 엔진의 객체를 감싸는 CSS 객체 모델(CSSOM, W3C 작업 초안 2021-08-26, <code>https://www.w3.org/TR/cssom-1/</code>)의 스크립트 인터페이스:
 * <code>StyleSheet</code> 와 <code>CSSStyleSheet</code>, <code>MediaList</code>, <code>CSSRuleList</code>, 규칙 인터페이스 <code>CSSRule</code>,
 * <code>CSSStyleRule</code>, <code>CSSGroupingRule</code>, <code>CSSConditionRule</code>, <code>CSSMediaRule</code>, <code>CSSSupportsRule</code>,
 * <code>CSSLayerBlockRule</code>, <code>CSSLayerStatementRule</code>, <code>CSSImportRule</code>, <code>CSSFontFaceRule</code>,
 * <code>CSSKeyframesRule</code>, <code>CSSKeyframeRule</code>, <code>CSSNamespaceRule</code>. 스크립트는 스타일 리졸버가 읽는 바로 그 객체에 닿으므로 규칙을 넣거나
 * 선언, 선택자, 미디어, 비활성 플래그를 바꾸면 페이지가 즉시 바뀐다. 새 규칙의 인터페이스 <code>CSSContainerRule</code>, <code>CSSScopeRule</code>,
 * <code>CSSStartingStyleRule</code>, <code>CSSPropertyRule</code>, <code>CSSCounterStyleRule</code>, <code>CSSPageRule</code>, <code>CSSMarginRule</code> 도 있다.
 * 자신의 인터페이스가 없는 규칙 종류(<code>@font-feature-values</code> 등)는 <code>type</code> 과 <code>cssText</code> 만 가진 일반 <code>CSSRule</code> 이다. 다른 오리진에서 온 스타일 시트의 규칙을 스크립트가 읽을 수
 * 있는지는 페이지가 주는 {@link Guard} 가 정한다(CSSOM 은 그런 시트를 읽을 수 없게 한다). 페이지에 따라 달라지는 부분(<code>document.styleSheets</code>, 생성 스타일 시트,
 * 요소의 <code>sheet</code>)은 페이지가 설치한다.
 */
public final class CssomInterfaces {

    /**
     * Decides whether a script may read and change the rules of a style sheet.
     * <p>
     * 스크립트가 스타일 시트의 규칙을 읽고 바꿀 수 있는지 정한다.
     */
    public interface Guard {
        /**
         * Checks the access to the rules of a sheet.
         * <p>
         * 시트의 규칙에 대한 접근을 검사한다.
         *
         * @param sheet the sheet / 시트
         * @throws DomException <code>SecurityError</code> if the rules must not be read / 규칙을 읽을 수 없으면 <code>SecurityError</code>
         */
        void check(StyleSheet sheet);
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CssomInterfaces() {
    }

    /**
     * Returns the interface name of an object of the CSS engine, or null if it is not one.
     * <p>
     * CSS 엔진 객체의 인터페이스 이름을 반환한다. 해당하지 않으면 null.
     *
     * @param o the object / 객체
     * @return the interface name or null / 인터페이스 이름 또는 null
     */
    static String classify(Object o) {
        if (o instanceof StyleSheet) {
            return "CSSStyleSheet";
        } else if (o instanceof MediaList) {
            return "MediaList";
        } else if (o instanceof CssRuleList) {
            return "CSSRuleList";
        } else if (o instanceof CssStyleDeclaration) {
            return "CSSStyleDeclaration";
        } else if (o instanceof StyleRule) {
            return "CSSStyleRule";
        } else if (o instanceof ImportRule) {
            return "CSSImportRule";
        } else if (o instanceof GroupRule) {
            GroupRule g = (GroupRule) o;
            // 그룹 규칙은 종류에 따라 인터페이스가 다르다.
            if (g.getType() == CssRule.MEDIA_RULE) {
                return "CSSMediaRule";
            } else if (g.getType() == CssRule.SUPPORTS_RULE) {
                return "CSSSupportsRule";
            } else if (g.getType() == CssRule.LAYER_RULE) {
                return g.isLayerStatement() ? "CSSLayerStatementRule" : "CSSLayerBlockRule";
            } else if (g.getType() == CssRule.CONTAINER_RULE) {
                return "CSSContainerRule";
            } else if (g.getType() == CssRule.SCOPE_RULE) {
                return "CSSScopeRule";
            } else if (g.getType() == CssRule.STARTING_STYLE_RULE) {
                return "CSSStartingStyleRule";
            }
            return "CSSGroupingRule";
        } else if (o instanceof FontFaceRule) {
            return "CSSFontFaceRule";
        } else if (o instanceof KeyframesRule) {
            return "CSSKeyframesRule";
        } else if (o instanceof KeyframesRule.Keyframe) {
            return "CSSKeyframeRule";
        } else if (o instanceof NamespaceRule) {
            return "CSSNamespaceRule";
        } else if (o instanceof PageRule) {
            return "CSSPageRule";
        } else if (o instanceof MarginRule) {
            return "CSSMarginRule";
        } else if (o instanceof PropertyRule) {
            return "CSSPropertyRule";
        } else if (o instanceof CounterStyleRule) {
            return "CSSCounterStyleRule";
        } else if (o instanceof PositionTryRule) {
            return "CSSPositionTryRule";
        } else if (FontFeatureValuesBindings.classify(o) != null) {
            return FontFeatureValuesBindings.classify(o);
        } else if (o instanceof CssRule) {
            return "CSSRule";
        }
        return null;
    }

    /**
     * Returns an argument as text (a missing argument is undefined, so the text <code>undefined</code>, as WebIDL converts it).
     * <p>
     * 인수를 텍스트로 반환한다(없는 인수는 undefined 이므로 WebIDL 변환대로 <code>undefined</code> 텍스트).
     *
     * @param args the arguments / 인수
     * @param i    the position / 위치
     * @return the text / 텍스트
     */
    private static String text(JsValue[] args, int i) {
        return Ops.toString(Js.arg(args, i));
    }

    /**
     * Returns an optional integer argument, or a default if it is missing or undefined.
     * <p>
     * 선택 정수 인수를 반환한다. 없거나 undefined 이면 기본값.
     *
     * @param args the arguments / 인수
     * @param i    the position / 위치
     * @param dflt the default / 기본값
     * @return the integer / 정수
     */
    private static int integer(JsValue[] args, int i, int dflt) {
        return i < args.length && !args[i].isUndefined() ? Ops.toInt32(args[i]) : dflt;
    }

    /**
     * Throws the standard TypeError for a missing required argument.
     * <p>
     * 필수 인수가 없을 때의 표준 TypeError 를 던진다.
     *
     * @param args  the arguments / 인수
     * @param need  the number of required arguments / 필수 인수 수
     * @param name  the operation name / 연산 이름
     * @param iface the interface name / 인터페이스 이름
     */
    private static void require(JsValue[] args, int need, String name, String iface) {
        if (args.length < need) {
            throw JsException.typeError("Failed to execute '" + name + "' on '" + iface + "': " + need + " argument" + (need == 1 ? "" : "s")
                    + " required, but only " + args.length + " present.");
        }
    }

    /**
     * Installs the interfaces.
     * <p>
     * 인터페이스를 설치한다.
     *
     * @param bridge the bridge of the page / 페이지의 브리지
     * @param guard  decides which style sheets a script may read / 스크립트가 읽을 수 있는 스타일 시트를 정한다
     */
    public static void install(final DomBridge bridge, final Guard guard) {
        bridge.addClassifier(new DomBridge.Classifier() {
            /** Chooses the CSSOM interface of an object. / 객체의 CSSOM 인터페이스를 고른다. */
            public String classify(Object target) {
                return CssomInterfaces.classify(target);
            }
        });
        installLists(bridge);
        installStyleSheet(bridge, guard);
        installRules(bridge, guard);
    }

    /**
     * Installs <code>MediaList</code> and <code>CSSRuleList</code>.
     * <p>
     * <code>MediaList</code> 와 <code>CSSRuleList</code> 를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private static void installLists(DomBridge bridge) {
        DomBridge.InterfaceInfo media = bridge.defineInterface("MediaList", null, MediaList.class, new String[] { "a:mediaText=?", "a:length",
            "m:item(int)", "m:appendMedium(String)", "m:deleteMedium(String)" });
        CssomMembers.method(bridge, media, "toString", 0, new CssomMembers.Op() {
            /** The stringifier returns the media text. / 문자열화는 미디어 텍스트를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return Js.str(((MediaList) target).getMediaText());
            }
        });
        bridge.defineInterface("CSSRuleList", null, CssRuleList.class, new String[] { "a:length", "m:item(int)" });
        bridge.setIndexed("CSSRuleList", new IndexedObject.Source() {
            /** Returns the number of rules. / 규칙 수를 반환한다. */
            public int length(Object target) {
                return ((CssRuleList) target).getLength();
            }

            /** Returns a rule. / 규칙을 반환한다. */
            public Object item(Object target, int index) {
                return ((CssRuleList) target).item(index);
            }

            /** A rule list has no named items. / 규칙 목록에는 이름 있는 항목이 없다. */
            public Object named(Object target, String name) {
                return null;
            }
        });
    }

    /**
     * Installs <code>StyleSheet</code> and <code>CSSStyleSheet</code>.
     * <p>
     * <code>StyleSheet</code> 와 <code>CSSStyleSheet</code> 를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param guard  the access guard / 접근 검사
     */
    private static void installStyleSheet(DomBridge bridge, final Guard guard) {
        DomBridge.InterfaceInfo base = bridge.defineInterface("StyleSheet", null, StyleSheet.class, new String[] { "a:type@MimeType", "a:href",
            "a:ownerNode", "a:parentStyleSheet", "a:title", "a:disabled=" });
        forwardMedia(bridge, base);
        DomBridge.InterfaceInfo sheet = bridge.defineInterface("CSSStyleSheet", "StyleSheet", StyleSheet.class, new String[] { "a:ownerRule" });
        CssomMembers.accessor(bridge, sheet, "cssRules", new CssomMembers.Op() {
            /** Returns the rule list of the sheet. / 시트의 규칙 목록을 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                guard.check((StyleSheet) target);
                return b.toJs(((StyleSheet) target).getCssRules());
            }
        }, null);
        CssomMembers.accessor(bridge, sheet, "rules", new CssomMembers.Op() {
            /** The legacy name of <code>cssRules</code>. / <code>cssRules</code> 의 옛 이름. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                guard.check((StyleSheet) target);
                return b.toJs(((StyleSheet) target).getCssRules());
            }
        }, null);
        CssomMembers.method(bridge, sheet, "insertRule", 1, new CssomMembers.Op() {
            /** Inserts a rule given as text. / 텍스트로 주어진 규칙을 넣는다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                require(args, 1, "insertRule", "CSSStyleSheet");
                guard.check((StyleSheet) target);
                return Js.num(((StyleSheet) target).insertRule(text(args, 0), integer(args, 1, 0)));
            }
        });
        CssomMembers.method(bridge, sheet, "deleteRule", 1, new CssomMembers.Op() {
            /** Deletes a rule. / 규칙을 지운다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                require(args, 1, "deleteRule", "CSSStyleSheet");
                guard.check((StyleSheet) target);
                ((StyleSheet) target).deleteRule(Ops.toInt32(args[0]));
                return Js.UNDEFINED;
            }
        });
        CssomMembers.method(bridge, sheet, "addRule", 0, new CssomMembers.Op() {
            /** The legacy <code>addRule(selector, style, index)</code>; the index defaults to the end. / 옛 <code>addRule(selector, style, index)</code>. 위치의 기본값은 끝. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                StyleSheet s = (StyleSheet) target;
                guard.check(s);
                return Js.num(s.addRule(text(args, 0), text(args, 1), integer(args, 2, s.getRules().size())));
            }
        });
        CssomMembers.method(bridge, sheet, "removeRule", 0, new CssomMembers.Op() {
            /** The legacy <code>removeRule(index)</code>; the index defaults to 0. / 옛 <code>removeRule(index)</code>. 위치의 기본값은 0. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                guard.check((StyleSheet) target);
                ((StyleSheet) target).deleteRule(integer(args, 0, 0));
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs <code>media</code> on an interface whose target has a media list: reading gives the list, assigning sets its <code>mediaText</code>
     * (<code>[PutForwards=mediaText]</code>).
     * <p>
     * 대상이 미디어 목록을 가진 인터페이스에 <code>media</code> 를 설치한다: 읽으면 목록을 주고, 대입하면 그 <code>mediaText</code> 를 설정한다
     * (<code>[PutForwards=mediaText]</code>).
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface (<code>StyleSheet</code>, <code>CSSMediaRule</code> or <code>CSSImportRule</code>) / 인터페이스
     */
    private static void forwardMedia(DomBridge bridge, DomBridge.InterfaceInfo info) {
        CssomMembers.accessor(bridge, info, "media", new CssomMembers.Op() {
            /** Returns the media list. / 미디어 목록을 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return b.toJs(mediaListOf(target));
            }
        }, new CssomMembers.Op() {
            /** Sets the media text of the list. / 목록의 미디어 텍스트를 설정한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                mediaListOf(target).setMediaText(text(args, 0));
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Returns the media list of a style sheet, an <code>@media</code> rule or an <code>@import</code> rule.
     * <p>
     * 스타일 시트, <code>@media</code> 규칙, <code>@import</code> 규칙의 미디어 목록을 반환한다.
     *
     * @param target the Java object / Java 객체
     * @return the media list / 미디어 목록
     */
    private static MediaList mediaListOf(Object target) {
        if (target instanceof StyleSheet) {
            return ((StyleSheet) target).getMediaList();
        } else if (target instanceof GroupRule) {
            return ((GroupRule) target).getMediaList();
        }
        return ((ImportRule) target).getMediaList();
    }

    /**
     * Installs <code>style</code> on a rule interface: reading gives the declaration block, assigning sets its <code>cssText</code>
     * (<code>[PutForwards=cssText]</code>).
     * <p>
     * 규칙 인터페이스에 <code>style</code> 을 설치한다: 읽으면 선언 블록을 주고, 대입하면 그 <code>cssText</code> 를 설정한다(<code>[PutForwards=cssText]</code>).
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface of a rule that has a declaration block / 선언 블록을 가진 규칙의 인터페이스
     */
    private static void forwardStyle(DomBridge bridge, DomBridge.InterfaceInfo info) {
        CssomMembers.accessor(bridge, info, "style", new CssomMembers.Op() {
            /** Returns the declaration block. / 선언 블록을 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return b.toJs(styleOf(target));
            }
        }, new CssomMembers.Op() {
            /** Replaces the declarations by the text. / 선언을 텍스트로 바꾼다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                styleOf(target).setCssText(text(args, 0));
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Returns the declaration block of a style rule, a keyframe or a font face rule.
     * <p>
     * 스타일 규칙, 키프레임, 글꼴 규칙의 선언 블록을 반환한다.
     *
     * @param target the Java object / Java 객체
     * @return the block / 블록
     */
    private static CssStyleDeclaration styleOf(Object target) {
        if (target instanceof StyleRule) {
            return ((StyleRule) target).getStyle();
        } else if (target instanceof KeyframesRule.Keyframe) {
            return ((KeyframesRule.Keyframe) target).getStyle();
        } else if (target instanceof PageRule) {
            return ((PageRule) target).getStyle();
        } else if (target instanceof MarginRule) {
            return ((MarginRule) target).getStyle();
        } else if (target instanceof PositionTryRule) {
            return ((PositionTryRule) target).getStyle();
        }
        return ((FontFaceRule) target).getStyle();
    }

    /**
     * Installs the rule interfaces.
     * <p>
     * 규칙 인터페이스를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param guard  the access guard / 접근 검사
     */
    private static void installRules(final DomBridge bridge, Guard guard) {
        bridge.defineInterface("CSSRule", null, CssRule.class, new String[] { "c:STYLE_RULE=1", "c:CHARSET_RULE=2", "c:IMPORT_RULE=3", "c:MEDIA_RULE=4",
            "c:FONT_FACE_RULE=5", "c:PAGE_RULE=6", "c:KEYFRAMES_RULE=7", "c:KEYFRAME_RULE=8", "c:MARGIN_RULE=9", "c:NAMESPACE_RULE=10",
            "c:COUNTER_STYLE_RULE=11", "c:SUPPORTS_RULE=12", "c:FONT_FEATURE_VALUES_RULE=14", "a:type@CssomType", "a:cssText=", "a:parentRule",
            "a:parentStyleSheet" });
        DomBridge.InterfaceInfo style = bridge.defineInterface("CSSStyleRule", "CSSRule", StyleRule.class, new String[] { "a:selectorText=" });
        forwardStyle(bridge, style);
        bridge.defineInterface("CSSGroupingRule", "CSSRule", GroupRule.class, new String[] { "a:cssRules", "m:insertRule(String,int*)", "m:deleteRule(int)" });
        bridge.defineInterface("CSSConditionRule", "CSSGroupingRule", GroupRule.class, new String[] { "a:conditionText" });
        DomBridge.InterfaceInfo media = bridge.defineInterface("CSSMediaRule", "CSSConditionRule", GroupRule.class, new String[0]);
        forwardMedia(bridge, media);
        bridge.defineInterface("CSSSupportsRule", "CSSConditionRule", GroupRule.class, new String[0]);
        bridge.defineInterface("CSSLayerBlockRule", "CSSGroupingRule", GroupRule.class, new String[] { "a:name" });
        bridge.defineInterface("CSSLayerStatementRule", "CSSRule", GroupRule.class, new String[] { "a:nameList" });
        DomBridge.InterfaceInfo imp = bridge.defineInterface("CSSImportRule", "CSSRule", ImportRule.class, new String[] { "a:href@ResolvedHref",
            "a:styleSheet", "a:layerName" });
        forwardMedia(bridge, imp);
        DomBridge.InterfaceInfo font = bridge.defineInterface("CSSFontFaceRule", "CSSRule", FontFaceRule.class, new String[0]);
        CssomMembers.accessor(bridge, font, "style", new CssomMembers.Op() {
            /** Returns the descriptors as a declaration block. / 디스크립터를 선언 블록으로 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return b.toJs(((FontFaceRule) target).getStyle());
            }
        }, null);
        bridge.defineInterface("CSSKeyframesRule", "CSSRule", KeyframesRule.class, new String[] { "a:name=", "a:cssRules", "m:appendRule(String)",
            "m:deleteRule(String)", "m:findRule(String)" });
        CssomMembers.accessor(bridge, bridge.getInterface("CSSKeyframesRule"), "length", new CssomMembers.Op() {
            /** Returns the number of keyframes (the legacy <code>length</code>). / 키프레임 수를 반환한다(옛 <code>length</code>). */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return Js.num(((KeyframesRule) target).getKeyframes().size());
            }
        }, null);
        DomBridge.InterfaceInfo keyframe = bridge.defineInterface("CSSKeyframeRule", "CSSRule", KeyframesRule.Keyframe.class, new String[] { "a:keyText=" });
        forwardStyle(bridge, keyframe);
        bridge.defineInterface("CSSNamespaceRule", "CSSRule", NamespaceRule.class, new String[] { "a:namespaceURI@NamespaceUri", "a:prefix" });
        // CSS Conditional 5 / Cascade 6 / Properties and Values API / Counter Styles 의 규칙 인터페이스.
        bridge.defineInterface("CSSContainerRule", "CSSConditionRule", GroupRule.class, new String[] { "a:containerName", "a:containerQuery" });
        bridge.defineInterface("CSSScopeRule", "CSSGroupingRule", GroupRule.class, new String[] { "a:start@ScopeStart", "a:end@ScopeEnd" });
        bridge.defineInterface("CSSStartingStyleRule", "CSSGroupingRule", GroupRule.class, new String[0]);
        // CSS Paged Media 3: @page 와 그 안의 쪽 여백 규칙. CSSPageRule 은 규칙 목록(cssRules)을 가지지만 CSSGroupingRule 이 아니다(삽입/삭제 없음).
        DomBridge.InterfaceInfo page = bridge.defineInterface("CSSPageRule", "CSSRule", PageRule.class, new String[] { "a:selectorText=", "a:cssRules" });
        forwardStyle(bridge, page);
        DomBridge.InterfaceInfo marginRule = bridge.defineInterface("CSSMarginRule", "CSSRule", MarginRule.class, new String[] { "a:name" });
        forwardStyle(bridge, marginRule);
        bridge.defineInterface("CSSPropertyRule", "CSSRule", PropertyRule.class, new String[] { "a:name", "a:syntax", "a:inherits", "a:initialValue" });
        bridge.defineInterface("CSSCounterStyleRule", "CSSRule", CounterStyleRule.class, new String[] { "a:name=", "a:system=", "a:symbols=",
            "a:additiveSymbols=", "a:negative=", "a:prefix=", "a:suffix=", "a:range=", "a:pad=", "a:speakAs=", "a:fallback=" });
        // CSS Anchor Positioning 1 8.1 절: @position-try 규칙(이름과 위치 서술자 블록).
        DomBridge.InterfaceInfo positionTry = bridge.defineInterface("CSSPositionTryRule", "CSSRule", PositionTryRule.class, new String[] { "a:name" });
        forwardStyle(bridge, positionTry);
        // CSS Fonts 4 9.1 절: @font-feature-values 규칙과 그 기능 값 맵.
        FontFeatureValuesBindings.install(bridge);
    }
}
