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

   /** Old package names and a removed zero-argument API must fail compilation. */
   @Test
   void testRemovedMigrationApisDoNotCompile()
   {
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
      return success;
   }

   /** Return the loaded library jar name for version checks. */
   private String artifact(Class<?> type) throws Exception
   {
      return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).getFileName().toString();
   }
}
