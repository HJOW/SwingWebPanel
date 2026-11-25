package org.duckdns.hjow.swingwebpanel.js.ast;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ModuleRequest;

/**
 * Information about a whole script or module, stored in the value field of the PROGRAM node: strictness, the goal, the
 * source text and, for modules, the import and export entries (ECMA-262 6th edition 15.2.1.16 Source Text Module Records).
 * <p>
 * 스크립트 또는 모듈 전체에 대한 정보. PROGRAM 노드의 value 필드에 저장된다: 엄격 여부, 파싱 목표, 소스 텍스트, 모듈의
 * import/export 항목(ECMA-262 6판 15.2.1.16 Source Text Module Record).
 */
public final class ProgramInfo {

    /**
     * An import entry: <code>import { importName as localName } from 'moduleRequest'</code>.
     * <p>
     * import 항목: <code>import { importName as localName } from 'moduleRequest'</code>.
     */
    public static final class ImportEntry {
        /**
         * The module specifier.
         * <p>
         * 모듈 지정자.
         */
        public final String moduleRequest;
        /** The request including attributes. / 속성을 포함한 요청. */
        public final ModuleRequest request;
        /**
         * The imported name; "default" for default imports and "*" for namespace imports.
         * <p>
         * 가져오는 이름. 기본 가져오기는 "default", 네임스페이스 가져오기는 "*".
         */
        public final String importName;
        /**
         * The local binding name.
         * <p>
         * 로컬 바인딩 이름.
         */
        public final String localName;

        /**
         * Creates an entry.
         * <p>
         * 항목을 만든다.
         *
         * @param moduleRequest module specifier / 모듈 지정자
         * @param importName    imported name / 가져오는 이름
         * @param localName     local name / 로컬 이름
         */
        public ImportEntry(String moduleRequest, String importName, String localName) {
            this(moduleRequest == null ? null : new ModuleRequest(moduleRequest), importName, localName);
        }

        /**
         * Creates an entry referring to an attributed module request.
         * <p>
         * 속성이 있는 모듈 요청을 가리키는 항목을 만든다.
         *
         * @param request requesting specifier and attributes / 요청 지정자와 속성
         * @param importName imported name / 가져오는 이름
         * @param localName local binding name / 지역 바인딩 이름
         * @return import entry / 가져오기 항목
         */
        public static ImportEntry forRequest(ModuleRequest request, String importName, String localName) {
            return new ImportEntry(request, importName, localName);
        }

        /**
         * Stores a request and the names it binds.
         * <p>
         * 요청과 요청이 바인딩하는 이름을 저장한다.
         *
         * @param request module request / 모듈 요청
         * @param importName imported name / 가져오는 이름
         * @param localName local binding name / 지역 바인딩 이름
         */
        private ImportEntry(ModuleRequest request, String importName, String localName) {
            this.request = request;
            this.moduleRequest = request == null ? null : request.getSpecifier();
            this.importName = importName;
            this.localName = localName;
        }
    }

    /**
     * An export entry. A local export has a localName and no moduleRequest; an indirect export has moduleRequest and importName
     * (importName "*" with exportName null is <code>export * from</code>).
     * <p>
     * export 항목. 로컬 export 는 localName 이 있고 moduleRequest 가 없다. 간접 export 는 moduleRequest 와 importName 이 있다
     * (exportName 이 null 이고 importName 이 "*" 이면 <code>export * from</code>).
     */
    public static final class ExportEntry {
        /**
         * The exported name or null for <code>export *</code>.
         * <p>
         * 내보내는 이름. <code>export *</code> 이면 null.
         */
        public final String exportName;
        /**
         * The module specifier for re-exports, otherwise null.
         * <p>
         * 재내보내기의 모듈 지정자. 아니면 null.
         */
        public final String moduleRequest;
        /** The re-export request including attributes, or null for a local export. / 속성을 포함한 재내보내기 요청. 지역 내보내기면 null. */
        public final ModuleRequest request;
        /**
         * The name imported from the other module for re-exports.
         * <p>
         * 재내보내기에서 다른 모듈로부터 가져오는 이름.
         */
        public final String importName;
        /**
         * The local binding name for local exports.
         * <p>
         * 로컬 export 의 로컬 바인딩 이름.
         */
        public final String localName;

        /**
         * Creates an entry.
         * <p>
         * 항목을 만든다.
         *
         * @param exportName    exported name or null / 내보내는 이름 또는 null
         * @param moduleRequest module specifier or null / 모듈 지정자 또는 null
         * @param importName    imported name or null / 가져오는 이름 또는 null
         * @param localName     local name or null / 로컬 이름 또는 null
         */
        public ExportEntry(String exportName, String moduleRequest, String importName, String localName) {
            this(exportName, moduleRequest == null ? null : new ModuleRequest(moduleRequest), importName, localName);
        }

        /**
         * Creates an indirect export with a full module request.
         * <p>
         * 전체 모듈 요청을 포함한 간접 내보내기 항목을 만든다.
         *
         * @param exportName exported name or null for a star export / 내보내는 이름. 별표 내보내기면 null
         * @param request re-export request / 재내보내기 요청
         * @param importName imported name / 가져오는 이름
         * @return export entry / 내보내기 항목
         */
        public static ExportEntry forRequest(String exportName, ModuleRequest request, String importName) {
            return new ExportEntry(exportName, request, importName, null);
        }

        /**
         * Stores local or indirect export data.
         * <p>
         * 지역 또는 간접 내보내기 정보를 저장한다.
         *
         * @param exportName exported name or null / 내보내는 이름 또는 null
         * @param request re-export request or null / 재내보내기 요청 또는 null
         * @param importName imported name or null / 가져오는 이름 또는 null
         * @param localName local binding name or null / 지역 바인딩 이름 또는 null
         */
        private ExportEntry(String exportName, ModuleRequest request, String importName, String localName) {
            this.exportName = exportName;
            this.request = request;
            this.moduleRequest = request == null ? null : request.getSpecifier();
            this.importName = importName;
            this.localName = localName;
        }
    }

    /**
     * True for the Module goal.
     * <p>
     * Module 목표이면 true.
     */
    public boolean module;

    /**
     * True if the program is strict mode code (always true for modules).
     * <p>
     * 프로그램이 엄격 모드 코드이면 true(모듈은 항상 true).
     */
    public boolean strict;

    /**
     * The full source text.
     * <p>
     * 전체 소스 텍스트.
     */
    public String source;

    /**
     * The source name.
     * <p>
     * 소스 이름.
     */
    public String sourceName;

    /**
     * Requested specifiers in order of first appearance. Kept for clients reading the original parser model.
     * <p>
     * 처음 나타난 순서의 요청 지정자. 기존 파서 모델을 읽는 호출자용이다.
     */
    public final List<String> requestedModules = new ArrayList<String>();

    /** Unique module requests in first appearance order. / 처음 나타난 순서의 고유 모듈 요청. */
    public final List<ModuleRequest> moduleRequests = new ArrayList<ModuleRequest>();

    /**
     * Records a request in both the full and specifier-only views.
     * <p>
     * 전체 모듈 요청 목록과 지정자만의 목록에 요청을 기록한다.
     *
     * @param request parsed module request / 파싱한 모듈 요청
     */
    public void addModuleRequest(ModuleRequest request) {
        if (!moduleRequests.contains(request)) {
            moduleRequests.add(request);
        }
        if (!requestedModules.contains(request.getSpecifier())) {
            requestedModules.add(request.getSpecifier());
        }
    }

    /**
     * Import entries (modules only).
     * <p>
     * import 항목들(모듈 전용).
     */
    public final List<ImportEntry> imports = new ArrayList<ImportEntry>();

    /**
     * Local export entries (modules only).
     * <p>
     * 로컬 export 항목들(모듈 전용).
     */
    public final List<ExportEntry> localExports = new ArrayList<ExportEntry>();

    /**
     * Indirect export entries (re-exports, modules only).
     * <p>
     * 간접 export 항목들(재내보내기, 모듈 전용).
     */
    public final List<ExportEntry> indirectExports = new ArrayList<ExportEntry>();

    /**
     * Star export entries (<code>export * from</code>, modules only); the exportName is null.
     * <p>
     * 별표 export 항목들(<code>export * from</code>, 모듈 전용). exportName 은 null.
     */
    public final List<ExportEntry> starExports = new ArrayList<ExportEntry>();
}
