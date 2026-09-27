/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.sampleapp;


import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.picocli.QPicoCliImplementation;
import io.javalin.Javalin;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/** Compiles the 4.0 migration guide against the sample's resolved candidate modules. */
class SampleMigration40ContractTest
{
   @TempDir
   Path classes;
   private String compileDiagnostics;

   /** The guide's two Javalin 7 registration snippets compile with observer and replacement APIs. */
   @Test
   void testDocumentedAfterExamplesCompile() throws Exception
   {
      String guide = Files.readString(Path.of(System.getProperty("basedir"), "../docs/migration/4.0.adoc"));
      String provider = afterBlock(guide, 0);
      String customizer = afterBlock(guide, 1);
      assertTrue(guide.contains("import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;"));
      assertTrue(guide.contains("import com.kingsrook.qqq.middleware.picocli.QPicoCliImplementation;"));
      assertTrue(guide.contains("input.getValueString(\"method\")"));
      assertTrue(guide.contains("entity.toQRecordOnlyChangedFields(false)"));

      String source = """
         package migration;
         import java.util.concurrent.atomic.AtomicReference;
         import com.kingsrook.sampleapp.SampleJavalinServer;
         import com.kingsrook.qqq.backend.core.context.QContext;
         import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
         import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
         import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
         import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
         import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
         import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
         import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
         import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
         import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
         import com.kingsrook.qqq.backend.core.model.session.QSession;
         import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
         import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
         import com.kingsrook.qqq.middleware.javalin.QJavalinProcessHandler;
         import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
         import com.kingsrook.qqq.middleware.picocli.QPicoCliImplementation;
         import com.kingsrook.qqq.middleware.picocli.QCommandBuilder;
         import com.kingsrook.qqq.openapi.model.In;
         import com.kingsrook.qqq.openapi.model.Parameter;
         import com.kingsrook.qqq.openapi.model.Schema;
         import com.kingsrook.qqq.openapi.model.Type;
         import io.javalin.Javalin;
         import io.javalin.config.JavalinConfig;
         class MigrationExamples implements QJavalinRouteProviderInterface {
            public void setQInstance(QInstance instance) {}
         """ + provider + """
            void configure(SampleJavalinServer server, AtomicReference<Javalin> serviceReference) {
         """ + customizer + """
               server.withJavalinConfigurationCustomizer(serviceReference::set);
            }
            String method(RunBackendStepInput input) { return input.getValueString("method"); }
            QInstance instance() { return QContext.getQInstance(); }
            QSession session() { return QContext.getQSession(); }
            void changed(QRecordEntity entity) { entity.toQRecordOnlyChangedFields(false); }
            void replacements(QInstance instance, QAuthenticationMetaData authMetaData,
               Parameter parameter, Schema schema, QProcessMetaData process,
               QBackendStepMetaData step, QFunctionInputMetaData inputMetaData,
               QFieldMetaData field, SampleJavalinServer server,
               QJavalinRouteProviderInterface routeProvider) {
               instance.registerAuthenticationProvider(AuthScope.instanceDefault(), authMetaData);
               parameter.setIn(In.QUERY);
               schema.setType(Type.STRING);
               process.withStep(step);
               process.withOptionalStep(step);
               inputMetaData.withField(field);
               server.withAdditionalRouteProviders(java.util.List.of(routeProvider));
            }
            Class<?>[] renamed() { return new Class<?>[] { QJavalinImplementation.class,
               QJavalinMetaData.class, QJavalinProcessHandler.class,
               QPicoCliImplementation.class, QCommandBuilder.class }; }
         }
         """;
      assertTrue(compile("migration.MigrationExamples", source), source);
   }

   /*******************************************************************************
    ** Compile the guide's remaining concrete Java replacements.
    *******************************************************************************/
   @Test
   void testBreak03And04ReplacementExamplesCompile() throws Exception
   {
      String guide = Files.readString(Path.of(System.getProperty("basedir"), "../docs/migration/4.0.adoc"));
      Map<String, Integer> examples = Map.ofEntries(
         Map.entry("BREAK-03-A", 1), Map.entry("BREAK-03-B", 1), Map.entry("BREAK-03-C", 1),
         Map.entry("BREAK-04-06", 2), Map.entry("BREAK-04-07", 1), Map.entry("BREAK-04-08", 1),
         Map.entry("BREAK-04-09", 1), Map.entry("BREAK-04-10", 1), Map.entry("BREAK-04-11", 1),
         Map.entry("BREAK-04-12", 1), Map.entry("BREAK-04-13", 1), Map.entry("BREAK-04-14", 1),
         Map.entry("BREAK-04-15", 1), Map.entry("BREAK-04-16", 1), Map.entry("BREAK-04-17", 1),
         Map.entry("BREAK-04-18", 1), Map.entry("BREAK-04-20", 1), Map.entry("BREAK-04-21", 1),
         Map.entry("BREAK-04-24", 1), Map.entry("BREAK-04-25", 1), Map.entry("BREAK-04-26", 1));
      for(var entry : examples.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList())
      {
         for(int index = 0; index < entry.getValue(); index++)
         {
            String snippet = migrationAfterBlock(guide, entry.getKey(), index)
               .replaceAll("(?m)^import [^;]+;\\R", "");
            if("BREAK-04-25".equals(entry.getKey()))
            {
               snippet = "var input = queryInput;\n" + snippet;
            }
            String source = """
               package migration;
               import java.io.Serializable;
               import java.util.*;
               import java.time.temporal.ChronoUnit;
               import com.fasterxml.jackson.databind.*;
               import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
               import com.kingsrook.qqq.backend.core.actions.dashboard.AbstractHTMLWidgetRenderer;
               import com.kingsrook.qqq.backend.core.actions.customizers.RecordCustomizerUtilityInterface;
               import com.kingsrook.qqq.backend.core.actions.metadata.DefaultNoopMetaDataActionCustomizer;
               import com.kingsrook.qqq.backend.core.actions.templates.RenderTemplateAction;
               import com.kingsrook.qqq.backend.core.context.QContext;
               import com.kingsrook.qqq.backend.core.model.data.QRecord;
               import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
               import com.kingsrook.qqq.backend.core.model.actions.tables.query.*;
               import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.NowWithOffset;
               import com.kingsrook.qqq.backend.core.model.metadata.*;
               import com.kingsrook.qqq.backend.core.model.metadata.authentication.*;
               import com.kingsrook.qqq.backend.core.model.metadata.branding.*;
               import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
               import com.kingsrook.qqq.backend.core.model.metadata.fields.*;
               import com.kingsrook.qqq.backend.core.model.metadata.processes.*;
               import com.kingsrook.qqq.backend.core.model.metadata.variants.*;
               import com.kingsrook.qqq.backend.core.model.session.QSession;
               import com.kingsrook.qqq.backend.core.utils.*;
               import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
               import com.kingsrook.qqq.backend.module.api.model.metadata.APIBackendVariantSetting;
               import com.kingsrook.qqq.api.actions.GetTableApiFieldsAction;
               import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsInput;
               import com.kingsrook.qqq.api.utils.ApiQueryFilterUtils;
               import com.kingsrook.qqq.frontend.materialdashboard.model.metadata.MaterialDashboardBannerSlots;
               import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
               import com.kingsrook.sampleapp.SampleJavalinServer;
               import com.kingsrook.qqq.openapi.model.*;
               class MyMetaDataCustomizer extends DefaultNoopMetaDataActionCustomizer {}
               class ReplacementExample {
                  void run(QBackendMetaData metaData, QRecordEntity entity, Parameter parameter, Schema schema,
                     Object myObject, DefaultSerializerProvider customProvider, QQueryFilter filter,
                     Map<String, Serializable> inputValues, QInstance qInstance,
                     QAuthenticationMetaData authMetaData, QCodeReference codeRef,
                     QBrandingMetaData qBrandingMetaData, QProcessMetaData qProcessMetaData,
                     QBackendStepMetaData step, QFunctionInputMetaData inputMetaData, QFieldMetaData field,
                     String tableName, Serializable id, String processName, String fieldName,
                     QRecord record, Serializable pkey, Optional<Map<Serializable, QRecord>> oldRecordMap,
                     Map<String, Object> context, String code, SampleJavalinServer server,
                     QJavalinRouteProviderInterface routeProvider, Map<String, QFieldMetaData> tableApiFields,
                     List<String> badRequestMessages, String apiName, String apiVersion,
                     com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput queryInput,
                     String tableNameForApi) throws Exception {
               """ + snippet + """
                  }
               }
               """;
            assertTrue(compile("migration.ReplacementExample", source), entry.getKey() + " #" + index + "\n" + compileDiagnostics);
         }
      }
   }

   /*******************************************************************************
    ** Extract a replacement from its BREAK section so guide drift fails.
    *******************************************************************************/
   private String migrationAfterBlock(String guide, String id, int ordinal)
   {
      Matcher section = Pattern.compile("(?ms)^=== " + Pattern.quote(id) + ":[^\\n]*\\R(.*?)(?=^=== |^== |\\z)").matcher(guide);
      assertTrue(section.find(), "Missing migration section " + id);
      Matcher block = Pattern.compile("(?ms)^\\.After[^\\n]*\\R----\\R(.*?)\\R----").matcher(section.group(1));
      for(int index = 0; block.find(); index++)
      {
         if(index == ordinal)
         {
            return block.group(1) + "\n";
         }
      }
      throw new AssertionError("Missing replacement " + id + " #" + ordinal);
   }

   /*******************************************************************************
    ** Compile replacements needing a subclass or guide placeholder.
    *******************************************************************************/
   @Test
   void testSubclassReplacementContractsCompile() throws Exception
   {
      String guide = Files.readString(Path.of(System.getProperty("basedir"), "../docs/migration/4.0.adoc"));
      assertTrue(guide.contains("Override `customizeInputPreQuery(RunBackendStepInput, QueryInput)`"));
      assertTrue(guide.contains("Override `customizeFilterCriteriaForQueryOrCount`"));
      assertTrue(compile("migration.CoreReplacements", """
         package migration;
         import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
         import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
         import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
         import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
         import com.kingsrook.qqq.backend.core.model.metadata.processes.QRecordListMetaData;
         class CoreReplacements extends ExtractViaQueryStep {
            @Override protected void customizeInputPreQuery(RunBackendStepInput step, QueryInput query) {
               super.customizeInputPreQuery(step, query);
            }
            void fields(QRecordListMetaData list, QFieldMetaData field) { list.withField(field); }
         }
         """), compileDiagnostics);
      assertTrue(compile("migration.ApiReplacement", """
         package migration;
         import com.kingsrook.qqq.api.model.actions.ApiFieldCustomValueMapper;
         import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
         import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
         import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
         import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrCountInputInterface;
         class ApiReplacement extends ApiFieldCustomValueMapper {
            @Override public void customizeFilterCriteriaForQueryOrCount(QueryOrCountInputInterface input,
               QQueryFilter filter, QFilterCriteria criteria, String name, ApiFieldMetaData field) {}
         }
         """), compileDiagnostics);
      String handler = migrationAfterBlock(guide, "BREAK-04-19", 0).replace("...", "");
      assertTrue(compile("migration.MyHandler", """
         package migration;
         import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationHandlerInterface;
         import com.kingsrook.qqq.backend.core.model.automation.RecordAutomationInput;
         import com.kingsrook.qqq.backend.core.exceptions.QException;
         """ + handler), compileDiagnostics);
      assertTrue(compile("migration.TokenReplacement", """
         package migration;
         import com.kingsrook.qqq.backend.module.api.actions.BaseAPIActionUtil;
         import org.apache.http.impl.client.CloseableHttpClient;
         import org.apache.http.client.methods.HttpRequestBase;
         class TokenReplacement extends BaseAPIActionUtil {
            void run(CloseableHttpClient httpClient, HttpRequestBase httpRequest) throws Exception {
         """ + migrationAfterBlock(guide, "BREAK-04-22", 0) + """
            }
         }
         """), compileDiagnostics);
      assertTrue(compile("migration.FileReplacement", """
         package migration;
         import com.kingsrook.qqq.backend.module.filesystem.base.actions.AbstractBaseFilesystemAction;
         import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
         import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
         import com.kingsrook.qqq.backend.core.model.data.QRecord;
         abstract class FileReplacement extends AbstractBaseFilesystemAction<Object> {
            void run(QBackendMetaData backend, QTableMetaData table, QRecord record,
               String path, byte[] contents) throws Exception {
         """ + migrationAfterBlock(guide, "BREAK-04-23", 0) + """
            }
         }
         """), compileDiagnostics);
   }

   /*******************************************************************************
    ** Match non-Java migration entries to the candidate BOM and toolchain.
    *******************************************************************************/
   @Test
   void testBuildMigrationEntriesMatchCandidate() throws Exception
   {
      Path root = Path.of(System.getProperty("basedir")).getParent();
      String guide = Files.readString(root.resolve("docs/migration/4.0.adoc"));
      String bom = Files.readString(root.resolve("qqq-bom/pom.xml"));
      for(String module : List.of("qqq-backend-module-sqlite", "qqq-backend-module-postgres",
         "qqq-middleware-lambda", "qqq-middleware-health", "qqq-utility-lambdas"))
      {
         assertTrue(bom.contains("<artifactId>" + module + "</artifactId>"), module);
         assertTrue(guide.contains(module), module);
      }
      String lambdas = Files.readString(root.resolve("qqq-utility-lambdas/pom.xml"));
      assertTrue(lambdas.contains("<maven.compiler.source>21</maven.compiler.source>"));
      assertTrue(lambdas.contains("<maven.compiler.target>21</maven.compiler.target>"));
      String javalin = Files.readString(root.resolve("qqq-middleware-javalin/pom.xml"));
      assertTrue(javalin.contains("<artifactId>openapi-annotation-processor</artifactId>"));
      assertFalse(javalin.contains("<source>11</source>"));
      assertFalse(javalin.contains("<target>11</target>"));
      assertTrue(guide.contains("annotation processor inherits Java 21"));
      assertTrue(Files.readString(root.resolve("qodana.yaml")).contains("projectJDK: 21"));
   }

   /** Old package names and a removed zero-argument API must fail compilation. */
   @Test
   void testRemovedMigrationApisDoNotCompile()
   {
      assertFalse(compile("migration.OldVariant", """
         package migration;
         import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
         class OldVariant { void run(QBackendMetaData metadata) { metadata.setVariantOptionsTableName("old"); } }
         """));
      assertFalse(compile("migration.OldJson", """
         package migration;
         import com.kingsrook.qqq.backend.core.utils.JsonUtils;
         class OldJson { void run() { JsonUtils.toJson(new Object(), mapper -> {}); } }
         """));
      assertFalse(compile("migration.OldProcess", """
         package migration;
         import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
         import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
         class OldProcess { void run(QProcessMetaData process, QBackendStepMetaData step) { process.addStep(step); } }
         """));
      assertFalse(compile("migration.OldApiSession", """
         package migration;
         import com.kingsrook.qqq.backend.module.api.actions.BaseAPIActionUtil;
         import com.kingsrook.qqq.backend.core.model.session.QSession;
         class OldApiSession { void run(BaseAPIActionUtil api, QSession session) { api.setSession(session); } }
         """));
      assertFalse(compile("migration.OldSchema", """
         package migration;
         import com.kingsrook.qqq.openapi.model.Schema;
         class OldSchema { void run(Schema schema) { schema.setType("string"); } }
         """));
      assertFalse(compile("migration.OldJavalin", """
         package migration;
         import com.kingsrook.qqq.backend.javalin.QJavalinImplementation;
         class OldJavalin { QJavalinImplementation value; }
         """));
      assertFalse(compile("migration.OldPicoCli", """
         package migration;
         import com.kingsrook.qqq.frontend.picocli.QPicoCliImplementation;
         class OldPicoCli { QPicoCliImplementation value; }
         """));
      assertFalse(compile("migration.OldEntity", """
         package migration;
         import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
         class OldEntity {
            void run(QRecordEntity entity) { entity.toQRecordOnlyChangedFields(); }
         }
         """));
      assertFalse(compile("migration.OldOpenApi", """
         package migration;
         import com.kingsrook.qqq.openapi.model.Parameter;
         class OldOpenApi { void run(Parameter parameter) { parameter.setIn("query"); } }
         """));
      assertFalse(compile("migration.OldJavalinProvider", """
         package migration;
         import com.kingsrook.sampleapp.SampleJavalinServer;
         import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
         class OldJavalinProvider {
            void run(SampleJavalinServer server, QJavalinRouteProviderInterface provider) {
               server.withAdditionalRouteProvider(provider);
            }
         }
         """));
   }

   /** Sample and generated Java must not retain the selected removed migration APIs. */
   @Test
   void testSampleSourceHasNoSelectedRemovedApis() throws Exception
   {
      Path base = Path.of(System.getProperty("basedir"));
      for(Path sourceRoot : List.of(base.resolve("src/main/java"), base.resolve("target/generated-sources")))
      {
         if(!Files.exists(sourceRoot))
         {
            continue;
         }
         try(var files = Files.walk(sourceRoot))
         {
            for(Path path : files.filter(file -> file.toString().endsWith(".java")).toList())
            {
               String source = Files.readString(path);
               for(String removed : List.of("com.kingsrook.qqq.backend.javalin", "com.kingsrook.qqq.frontend.picocli",
                  ".toQRecordOnlyChangedFields()", ".withAdditionalRouteProvider(", ".setAuthentication("))
               {
                  assertFalse(source.contains(removed), path + " retains " + removed);
               }
            }
         }
      }
   }

   /** The resolved test classpath uses one Javalin and aligned Jetty core/EE10 versions. */
   @Test
   void testResolvedCandidateRuntimeVersions() throws Exception
   {
      assertTrue(Runtime.version().feature() >= 21);
      assertEquals("javalin-7.2.3.jar", artifact(Javalin.class));
      assertEquals("jetty-server-12.1.13.jar", artifact(Server.class));
      assertEquals("jetty-ee10-servlet-12.1.13.jar", artifact(ServletContextHandler.class));
      List<String> jettyJars = List.of(System.getProperty("surefire.test.class.path").split(Pattern.quote(File.pathSeparator)))
         .stream().filter(path -> path.contains("/org/eclipse/jetty/") && path.endsWith(".jar")).toList();
      assertTrue(jettyJars.size() >= 5, jettyJars.toString());
      for(String path : jettyJars)
      {
         assertTrue(path.endsWith("-12.1.13.jar"), path);
      }
      String root = Files.readString(Path.of(System.getProperty("basedir"), "../pom.xml"));
      String sample = Files.readString(Path.of(System.getProperty("basedir"), "pom.xml"));
      Matcher revision = Pattern.compile("<revision>([^<]+)</revision>").matcher(root);
      assertTrue(revision.find());
      String candidateVersion = revision.group(1);
      assertTrue(root.contains("<artifactId>jetty-bom</artifactId>"));
      assertTrue(root.contains("<artifactId>jetty-ee10-bom</artifactId>"));
      for(Class<?> module : List.of(QInstance.class, QJavalinImplementation.class, QPicoCliImplementation.class))
      {
         assertTrue(artifact(module).contains(candidateVersion), module.getName());
      }
      assertTrue(candidateModuleCount(sample) >= 5);
      assertEquals(List.of(), mismatchedCandidates(sample));
      assertEquals(List.of("qqq-middleware-javalin:4.0.0"), mismatchedCandidates(
         "<artifactId>qqq-middleware-javalin</artifactId><version>4.0.0</version>"));
   }

   /** Extract the guide's configuration snippet without paraphrasing it. */
   private String afterBlock(String guide, int ordinal)
   {
      Matcher matcher = Pattern.compile("(?s)\\.After \\(Javalin 7, QQQ 4\\.0\\)\\R----\\R(.*?)\\R----").matcher(guide);
      for(int index = 0; matcher.find(); index++)
      {
         if(index == ordinal)
         {
            return matcher.group(1) + "\n";
         }
      }
      throw new AssertionError("Missing Javalin 7 migration example " + ordinal);
   }

   /** Count versioned QQQ library dependencies in the sample POM. */
   private int candidateModuleCount(String pom)
   {
      Matcher dependency = candidateModules(pom);
      int count = 0;
      while(dependency.find())
      {
         count++;
      }
      return count;
   }

   /** Report direct QQQ dependencies that do not use the candidate revision. */
   private List<String> mismatchedCandidates(String pom)
   {
      List<String> mismatches = new ArrayList<>();
      Matcher dependency = candidateModules(pom);
      while(dependency.find())
      {
         if(!"${revision}".equals(dependency.group(2)))
         {
            mismatches.add(dependency.group(1) + ":" + dependency.group(2));
         }
      }
      return mismatches;
   }

   /** Match direct backend and middleware module declarations. */
   private Matcher candidateModules(String pom)
   {
      return Pattern.compile("<artifactId>(qqq-(?:backend|middleware)[^<]+)</artifactId>\\s*<version>([^<]+)</version>").matcher(pom);
   }

   /** Compile an in-memory consumer against the exact resolved test classpath. */
   private boolean compile(String name, String source)
   {
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
      JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///" + name.replace('.', '/') + ".java"), JavaFileObject.Kind.SOURCE)
      {
         @Override
         public CharSequence getCharContent(boolean ignoreEncodingErrors)
         {
            return source;
         }
      };
      String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
      boolean success = compiler.getTask(null, null, diagnostics,
         List.of("--release", "21", "-proc:none", "-classpath", classpath, "-d", classes.toString()), null, List.of(file)).call();
      compileDiagnostics = diagnostics.getDiagnostics().toString();
      return success;
   }

   /** Return the loaded library jar name for version checks. */
   private String artifact(Class<?> type) throws Exception
   {
      return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).getFileName().toString();
   }
}
