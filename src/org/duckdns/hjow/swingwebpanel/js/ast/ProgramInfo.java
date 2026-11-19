package org.duckdns.hjow.swingwebpanel.js.ast;

import java.util.ArrayList;
import java.util.List;

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
            this.moduleRequest = moduleRequest;
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
            this.exportName = exportName;
            this.moduleRequest = moduleRequest;
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
     * Requested module specifiers in order of first appearance (modules only).
     * <p>
     * 처음 나타난 순서의 요청 모듈 지정자(모듈 전용).
     */
    public final List<String> requestedModules = new ArrayList<String>();

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
