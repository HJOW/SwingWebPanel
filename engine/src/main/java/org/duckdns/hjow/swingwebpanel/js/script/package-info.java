/**
 * The Java Scripting API (JSR 223, <code>javax.script</code>) provider of the stand-alone JavaScript engine: a
 * {@link org.duckdns.hjow.swingwebpanel.js.script.JsScriptEngineFactory} (registered under the unique name <code>swingwebpanel-js</code>) that creates
 * {@link org.duckdns.hjow.swingwebpanel.js.script.JsScriptEngine}s, which implement <code>ScriptEngine</code>, <code>Compilable</code> and
 * <code>Invocable</code> on top of {@link org.duckdns.hjow.swingwebpanel.js.embed.JsEngine}. This is the only package of the project (apart from its tests and the
 * example that shows its use) that may use <code>javax.script</code>; the engine never delegates to the Rhino or Nashorn engine of the Java runtime
 * (plan items P26-04 and P26-05).
 * <p>
 * 독립 JavaScript 엔진의 Java Scripting API(JSR 223, <code>javax.script</code>) 제공자: 고유한 이름 <code>swingwebpanel-js</code> 로 등록된
 * {@link org.duckdns.hjow.swingwebpanel.js.script.JsScriptEngineFactory} 가 {@link org.duckdns.hjow.swingwebpanel.js.script.JsScriptEngine} 을 만들고, 이것이
 * {@link org.duckdns.hjow.swingwebpanel.js.embed.JsEngine} 위에서 <code>ScriptEngine</code>, <code>Compilable</code>, <code>Invocable</code> 을 구현한다. 이 프로젝트에서
 * <code>javax.script</code> 를 쓸 수 있는 유일한 패키지이다(시험과 그 사용을 보여 주는 예제는 제외). 엔진은 Java 런타임의 Rhino 나 Nashorn 엔진에 위임하지 않는다
 * (계획 항목 P26-04, P26-05).
 */
package org.duckdns.hjow.swingwebpanel.js.script;
