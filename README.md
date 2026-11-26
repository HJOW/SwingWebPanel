# SwingWebPanel

English · [한국어](README.ko.md)

## Introduction

SwingWebPanel embeds a web browser component in Java Swing. The browser and JavaScript engine are implemented in Java and run on Java 6 or later.

```java
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;

public class JavaBrowser extends JFrame {
    private final SwingWebPanel web;

    public JavaBrowser() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout());

        web = new SwingWebPanel();
        add(new JScrollPane(web));
        web.setPage("https://www.w3.org/");
    }

    public static void main(String[] args) {
        new JavaBrowser().setVisible(true);
    }
}
```

For the Swing component, add this Maven dependency. The coordinates match this checkout.

```xml
<dependency>
    <groupId>org.duckdns.hjow</groupId>
    <artifactId>swingwebpanel</artifactId>
    <version>0.1.2</version>
</dependency>
```

For the standalone JavaScript engine, use the following dependency.

```xml
<dependency>
    <groupId>org.duckdns.hjow</groupId>
    <artifactId>swingwebpanel-js</artifactId>
    <version>0.1.2</version>
</dependency>
```

Use of the component is subject to its open-source license. The project recommends using it with pages you develop yourself or bundle with an application. The project uses AI extensively in development.

## Implementation and compatibility

The project implements HTML parsing, CSS, layout and rendering, the DOM, the JavaScript engine, media decoding and network requests. Runtime dependencies are limited to Java. The implementation runs without Rhino, JavaFX WebView, JNI or an installed browser.

The JavaScript target is ECMA-262, 17th edition (ECMAScript 2026), with earlier editions covered by compatibility tests. The feature matrix lists tested behavior and remaining differences in HTML, CSS, JavaScript and browser APIs. [Feature matrix](docs/FEATURE_MATRIX.md) · [Development plan](AGENTS.md)

## Requirements

- Run applications on Java 6 or later. The library uses Java 6 language features and APIs and is tested on JRE 6u45.
- JRE 6u45 JSSE lacks TLS 1.2 and client SNI. Modern HTTPS sites may require a newer JRE. On a connection or TLS failure, the component tries the corresponding HTTP URL once.

## Build and test

Run Maven 3.8.8 or later from the repository root with JDK 8 or later in `JAVA_HOME`. The build invokes a separate JDK 6 for compilation and regression tests. The default JDK 6 paths are `C:\Program Files\Java\jdk1.6.0_45` on Windows and `/usr/lib/jvm/java-6-openjdk` on Linux. Set `JAVA6_HOME` if your installation is elsewhere.

| Task | Command |
| --- | --- |
| Run the regression suite and package the project | `mvn verify` |
| Build the JARs | `mvn -Dmaven.test.skip=true package` |
| Run engine tests | `mvn -pl engine test` |
| Run matching test suites | `mvn "-Dtest.filter=BrowserJarsTest" test` |
| Verify offline after caching the plugins | `mvn -o verify` |
| Install the checkout in the local Maven repository | `mvn install` |

The root and engine `build` and `test` scripts invoke Maven. The test scripts also accept `--filter TEXT`. Run `examples/run.sh BlankPageExample` or `examples\run.bat BlankPageExample` for a minimal example. In Eclipse, import the repository root as **Existing Maven Projects**. [Example list](examples/README.md)

### JARs

| File | Contents |
| --- | --- |
| `browser/target/swingwebpanel.jar` | Browser and engine in one standalone JAR |
| `browser/target/swingwebpanel-browser.jar` | Browser module. Add the engine JAR to the runtime classpath |
| `engine/target/swingwebpanel-js.jar` | Standalone JavaScript engine and JSR 223 provider |

When you use Maven, the browser artifact brings in the engine. For manual setup, `browser/target/swingwebpanel.jar` contains both. Examples are built separately under `examples/target/`. A browser application built on SwingWebPanel (work in progress) is built as `desktopapp/target/swingwebpanel-desktopapp.jar` and started with `java -jar`; see [desktopapp/README.md](desktopapp/README.md).

### Publishing to Maven Central

The `release` profile builds source and Javadoc JARs, signs the artifacts with GPG and deploys them to Central.

```sh
mvn -Prelease clean deploy
```

Use `mvn -Prelease "-Dcentral.skipUpload=true" clean deploy` to create a signed bundle locally. [Release configuration](docs/progress/maven.md)

## Using the Swing component

`SwingWebPanel` extends `JPanel` and implements `Scrollable`. Wrap it in a `JScrollPane` to add scrollbars. The package name is `org.duckdns.hjow.swingwebpanel`.

```java
SwingWebPanel panel = new SwingWebPanel();
panel.addPropertyChangeListener(new PropertyChangeListener() {
    public void propertyChange(PropertyChangeEvent event) {
        System.out.println(event.getPropertyName() + " = " + event.getNewValue());
    }
});
frame.add(new JScrollPane(panel));
panel.setPage("http://example.com/");
```

Call `panel.dispose()` when closing the window. Property notifications, including `page`, `statusText`, `progress` and `title`, run on the event dispatch thread (EDT). You can call public methods from the EDT or application threads. Reads return immutable values or copies of the last committed state.

### Public methods

| Method | Behavior |
| --- | --- |
| `setPage(String)`, `getPage()` | Navigate asynchronously and read the committed URL. Supported addresses include HTTP, HTTPS, anonymous FTP, `classpath://`, local `file:`, registered `blob:` and `about:blank`. |
| `setContent(String, String)`, `getContent()` | Display HTML, plain text, Markdown or XHTML. `getContent()` returns the source after DOM edits or Markdown conversion. The URL for supplied content is `about:text`. |
| `back()`, `back(int)`, `forward()`, `forward(int)`, `getHistories()` | Move by the exact number of history entries when possible. The history getter returns a `Vector<String>` copy of the URLs. |
| `refresh()`, `refresh(boolean)` | Reload the page. Pass `true` to bypass the main document and subresource caches for that load. |
| `setUserAgent(String)`, `getUserAgent()` | Set or read the User-Agent. The default is `swingwebpanel/1.0`. |
| `addHeaders(Map<String,String>)`, `getHeaders()` | Merge headers by case-insensitive name. Set User-Agent through its own method. |
| `getCookies()` | Return unexpired cookies from the component's cookie jar as one string of name-value pairs. |
| `setJavascriptEnabled(boolean)`, `isJavascriptEnabled()` | JavaScript starts enabled. Disabling it stops scripts, inline handlers and timers. HTML, CSS, links and forms keep working. Re-enabling takes effect on the next load. |
| `getStatusText()`, `getProgress()` | Read status text and navigation progress. |
| `addHyperlinkListener`, `removeHyperlinkListener`, `removeHyperlinkEvents()` | Manage Swing listeners for link enter, exit and activation. Link navigation works without listeners. |
| `addConsoleListener`, `removeConsoleListener`, `getConsoleListeners()` | Manage listeners for page console output and script errors. |
| `postMessage(Object[, String])`, `addWebMessageListener`, `removeWebMessageListener`, `getWebMessageListeners()` | Send cloned data to the top-level document and receive messages on the EDT. |
| `setRootWindowClosingEnabled(boolean)` / `isRootWindowClosingEnabled()` | Allow the page of the main window (not a popup) to close it with `window.close()`. Off by default (the call is ignored); when on, the default action ends the program with `System.exit(0)`. See "Letting the page close the window". |
| `getLocalStorage()`, `getSessionStorage()` | Return read-only copies of the origin, key and value of each storage entry. |
| `clean()` | Cancel current work, return to `about:blank` and clear cache, session storage and all process-wide local storage. Preserve cookies, headers, User-Agent, scripting settings and listeners. |
| `dispose()` | Clean up the component and release user listeners, timers, media, downloads and other resources. Repeated calls are safe. |
| Additional methods | `getTitle()`, `getMimeType()`, `getHistoryIndex()`, `stop()`, `isDisposed()` |

A new panel starts at `about:blank` with an empty source, one history entry, empty cookies and headers, JavaScript enabled and progress `100`. [Method arguments, return values and lifecycle](docs/API_CONTRACT.md)

### Status and progress

| State | Status | Progress |
| --- | --- | --- |
| Initial state or after `clean()` | Empty string | 100 |
| Navigation begins | `Loading <URL>` | 0 |
| Receiving the main response | `Loading <URL>` | 1–50 with a known length, −1 if indeterminate |
| Waiting for document resources or scripts | `Loading resources (n/m)` | 50–94 |
| Required tasks have settled | `Done` | 100 |
| Error page | For example, `Error 404` | 100 |

Hovering over a link shows its address. During navigation, callers see the last committed URL and source. A commit updates the URL, source, document and history index together. A later navigation cancels pending work from the earlier one. A failed request displays an error page.

### Navigation, forms and URL schemes

Redirects produce one history entry for the final address. Refresh and history traversal reuse existing entries. Navigating to a new page after going back clears the forward history. A form `GET` uses the query string. History restores a `POST` response from its stored source. Refreshing or downloading that response may require submitting the form again. Override `protected boolean confirmResubmit(String url)` to replace the default confirmation dialog. [Navigation contract](docs/API_CONTRACT.md)

To open a local file from Java, use `panel.setPage(new File(path).toURI().toASCIIString())`. Local documents can use relative links and read files through Fetch and XHR. Network documents, opaque origins, HTTP redirects and UNC addresses cannot access local files.

Pages and workers register objects with `URL.createObjectURL`. Their `blob:` URLs work within the component. Frames, resources, Fetch/XHR and explicit download links follow the creator's origin. Calling `URL.revokeObjectURL` or closing the creator's context removes the registration. Downloads that already captured the bytes can finish. Unsupported schemes include `mailto:`, `javascript:`, `data:` and `jar:`. [Scheme policy](docs/API_CONTRACT.md)

### HTTPS and SSL

HTTPS uses the JRE trust store and hostname verification. Override `configureSsl` to customize the individual connection.

```java
SwingWebPanel panel = new SwingWebPanel() {
    protected void configureSsl(HttpsURLConnection connection) throws IOException {
        super.configureSsl(connection);
        connection.setSSLSocketFactory(myFactory);
    }
};
```

The HTTP fallback runs once for an HTTPS connection or TLS failure. HTTP error responses such as 404 and 500 do not trigger a retry. Secure cookies are withheld during fallback.

Fetch and XHR share the component's request layer and honor CORS and preflight. The component uses its socket client for requests that must send restricted headers such as `Origin`. If the JVM proxy applies, the JRE connection handles the request and may omit those headers. [Network and CORS coverage](docs/FEATURE_MATRIX.md)

### Web APIs and script time limits

An `AbortSignal` can remove registered DOM event listeners. Fetch responses expose a readable body with `getReader()` and async iteration. A classpath Fetch stream can carry up to 32 MiB. Whole-body methods such as `arrayBuffer()` have a 16 MiB limit.

`navigator.clipboard.readText()` and `writeText()` handle plain text in HTTPS, loopback and host-provided classpath documents. Writing requires a trusted user input action. Reading is allowed from Ctrl+V or Shift+Insert. [Web API coverage](docs/FEATURE_MATRIX.md)

The default browser script task time limit is 10 seconds. A host can override `protected long getScriptTaskTimeLimitMillis(String url)` for an app it supplies. The loader calls it with the top-level document's final URL before scripts run. An override returning zero or a negative number uses the default. Frames use the default. The step limit and cancellation still apply. [Script contract](docs/API_CONTRACT.md)

### Downloads

An attachment, an undisplayable MIME type or a same-origin `<a download>` opens a Swing download dialog and leaves the page and history in place. A same-origin object URL can also be saved from a detached anchor's `click()`. The dialog shows the filename, source, destination, progress and result, and allows cancellation. Headless mode cancels the download and reports it in the status text.

Override `protected DownloadHandle createDownloadPopup(DownloadRequest, DownloadControl)` to supply a UI, or `protected File getDownloadDirectory()` to choose the default destination. [Download popup example](examples/src/main/java/org/duckdns/hjow/swingwebpanel/examples/DownloadPopupExample.java)

### Audio, video and Canvas

The `<audio>` element decodes PCM/WAV, AIFF, AU, MP3, AAC, FLAC and Ogg Vorbis/Opus, including Vorbis or Opus in WebM. It outputs through Java Sound and reports an error if no usable device is available. An inaccessible image or audio address is shown in its place with alternative text when available. `<video>` shows an unsupported-video placeholder.

Tab to focus `<audio controls>`. Use Space or Enter to play or pause, Left and Right to seek by five seconds, Up and Down to adjust volume in 10% steps, and M to mute or unmute.

Web Audio includes `AudioContext`, `OfflineAudioContext`, media element sources for same-origin audio, node graphs, decoding and `AudioWorkletNode`. A real-time context advances silently if Java Sound cannot open an output. Override `createAudioOutputFactory()` for media elements or `createWebAudioOutputFactory()` for Web Audio. Automated tests verify PCM and playback state with a test output. Physical-device playback depends on the environment. [Media and Web Audio coverage](docs/FEATURE_MATRIX.md)

2D Canvas includes drawing, image data and exports through `toDataURL()` and `toBlob()`. Video playback, 3D acceleration, CSS 3D transforms, WebGL, WebGPU, emoji rendering and emoji-specific regular expression properties are unsupported. `getContext()` returns `null` for unsupported graphics contexts. External plugins such as Flash and Java applets display fallback content. [Feature coverage and exceptions](docs/FEATURE_MATRIX.md)

### Receiving console output

Listeners run on the EDT in message order. `ConsoleEvent` includes the level, kind, formatted message, source document, script location and original arguments.

```java
panel.addConsoleListener(new ConsoleListener() {
    public void consoleMessage(ConsoleEvent event) {
        System.out.println("[" + event.getLevel() + "] " + event.getMessage()
                + " (" + event.getScriptUrl() + ":" + event.getLineNumber() + ")");
    }
});
```

Uncaught exceptions and unhandled promise rejections have `ERROR` level and can be distinguished using `getKind()`. Calls to `alert`, `confirm` and `prompt` produce `DIALOG` records. If the listener queue overflows, `MESSAGES_DROPPED` reports the number of discarded records. The component discards console messages when it has no listeners. `clean()` retains listeners and `dispose()` removes them. [Console example](examples/README.md)

### Messaging between Java and the page

Java `postMessage(message, targetOrigin)` dispatches a `MessageEvent` to the current top-level document when the origin matches. The page sends data with `window.opener.postMessage(data, "swingwebpanel://host")` and can reply to Java using `event.source.postMessage(reply, event.origin)`.

```java
panel.addWebMessageListener(new WebMessageListener() {
    public void messageReceived(WebMessageEvent event) {
        if ("https://app.example".equals(event.getOrigin())
                && event.getSourceKind() == WebMessageEvent.SourceKind.DOCUMENT
                && "ready".equals(event.getData())) {
            event.reply("hello");
        }
    }
});
```

Check the origin and data shape when a document or frame sends a message. `reply()` is associated with the original document. Use `examples\run.bat MessageExample` or `examples/run.sh MessageExample` and append `--smoke` for a headless round trip. [Messaging contract](docs/API_CONTRACT.md)

### Letting the page close the window

By default `window.close()` from the page of the main window is ignored and only leaves a warning in the console of the page. After `setRootWindowClosingEnabled(true)`, a call from the top-level document makes the component call `closeRootWindow()` once on the EDT, right after the calling script task has ended. The default implementation ends the program with `System.exit(0)`.

```java
SwingWebPanel panel = new SwingWebPanel() {
    protected void closeRootWindow() {
        frame.dispose();   // close only this window instead of ending the program
        dispose();
    }
};
panel.setRootWindowClosingEnabled(true);
```

**Warning:** with this setting on, the page that is shown can end the program. Turn it on only for pages whose content you control. `top.close()` from a frame of another origin, or from a sandboxed frame without `allow-top-navigation`, is ignored even when the setting is on. A host action before the closing is performed (`setPage`, `clean()` and so on) cancels it. [Closing contract](docs/API_CONTRACT.md)

### Standalone JavaScript and JSR 223

Put `swingwebpanel-js.jar` on the classpath to embed the engine without Swing. Tested features include JSON modules and import attributes, resizable and shared buffers, `Atomics.waitAsync`, `Float16Array`, `Uint8Array` Base64 and hex methods, iterator helpers, regex `/v` and Unicode 18 normalization. The engine uses ECMA-262 defaults for `localeCompare` and `toLocaleString`. `Intl` is unavailable. [ECMAScript feature coverage](docs/FEATURE_MATRIX.md)

```java
import org.duckdns.hjow.swingwebpanel.js.embed.*;

JsEngine engine = new JsEngine();
engine.getLimits().setMaxTimeMillis(2000);
engine.put("name", "world");
Object text = engine.eval("('hello ' + name).toUpperCase()");
engine.eval("function twice(x) { return x * 2; }");
Object answer = engine.call("twice", Integer.valueOf(21)); // Double 42.0
```

Java can supply scalar values, `BigInteger`, maps, collections, arrays, engine handles or `JsHostFunction`. Unrecognized Java objects raise `IllegalArgumentException`. JS Number and BigInt values return as `Double` and `BigInteger`. Arrays and ordinary objects have live `List` and `Map` views, and functions implement `JsCallable`. Use `toPlain` for copies and `TypedArrays.copyBytes(handle.getJsObject())` to copy bytes from a typed array or buffer handle.

`JsScriptError` carries the script name, line and column for syntax and runtime errors. `ScriptAbortedException` reports execution limits and cancellation. The engine serializes operations and permits `cancel()` from another thread. Promise jobs run at the end of a top-level call and can also be processed by `runJobs()`. The standalone engine provides `console` and `print`. Browser objects and timers are available in the Swing component.

The JSR 223 provider registers the name `swingwebpanel-js` and reports language version `17`.

```java
ScriptEngine engine = new ScriptEngineManager().getEngineByName("swingwebpanel-js");
engine.put("x", 20);
Object result = engine.eval("x + 22"); // Double 42.0
Invocable invocable = (Invocable) engine;
```

The provider also registers `swingwebpanel-javascript`, the `swjs` extension and the `application/x-swingwebpanel-javascript` MIME type. Start the JVM with `-Dswingwebpanel.js.genericNames=true` to register generic names. Resolution then depends on the order of installed engines.

`ENGINE_SCOPE` bindings provide a live view of the global object. Other bindings give the evaluation a separate global scope. `print` and console messages use the context writers. The provider implements `Invocable` and `Compilable` and reports `MULTITHREADED`. `JsScriptEngine.getJsEngine()` exposes engine limits and cancellation. Register a `ModuleLoader.WithAttributes` using `JsEngine.setModuleLoader` to load JSON modules. [Standalone engine examples](examples/README.md)

### Storage and lifetime

`sessionStorage` is in memory per component and origin. `localStorage` is shared within the JVM, separated by origin and stored in a process-specific temporary file. A missing storage file starts empty and an invalid file is reset. `clean()` clears shared local storage across components in that process.

## Documentation

| Document | Contents |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Requirements, development rules and checklist |
| [API contract](docs/API_CONTRACT.md) | Public API, status changes, errors and policies |
| [Feature matrix](docs/FEATURE_MATRIX.md) | Standards, implementation, tests and compatibility limits |
| [Architecture](docs/ARCHITECTURE.md) | Packages, data flow, caching, lifetime and progress |
| [Progress](docs/PROGRESS.md) | Verification results and remaining work |
| [NetStorm](docs/progress/netstorm.md) | Sample compatibility and JDK measurements |

## Third-party notices

- The `PannerNode` HRTF resource uses the MIT KEMAR compact measurements by Bill Gardner and Keith Martin, MIT Media Lab Machine Listening Group, 1994. Copyright 1994 MIT Media Laboratory. The project converts the data with `tools/hrtfgen` and packages the converted HRTF resource. [MIT KEMAR measurements](https://sound.media.mit.edu/resources/KEMAR.html)
- `DynamicsCompressorKernel.java` retains Chromium's 3-clause BSD notice for the compressor design.
- The packaged Public Suffix List and Unicode data ship with their respective licenses. [Public Suffix List notice](browser/src/main/resources/org/duckdns/hjow/swingwebpanel/net/public_suffix_list.dat) · [Unicode license](engine/src/main/resources/META-INF/LICENSE-unicode.txt)

## License

Copyright 2026 HJOW

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
