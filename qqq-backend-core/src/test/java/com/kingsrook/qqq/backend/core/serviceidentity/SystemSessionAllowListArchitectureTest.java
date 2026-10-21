/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.serviceidentity;


import java.util.Map;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** docs/SERVICE-IDENTITY.md section 6.3 (owner decision D6): the system session
 ** (every permission, every all-access key) is for framework plumbing that
 ** touches no business data on anyone's behalf. Every core class that builds
 ** one is listed here with its reason; anything new must either use a service
 ** principal (ServiceIdentities) or be added here, in review, with a reason.
 *******************************************************************************/
class SystemSessionAllowListArchitectureTest
{
   /////////////////////////////////////////////////////////////////////////////
   // class (including its nested and anonymous classes) -> why it may build //
   // a system session                                                        //
   /////////////////////////////////////////////////////////////////////////////
   static final Map<String, String> ALLOWED = Map.ofEntries(
      Map.entry("com.kingsrook.qqq.backend.core.cluster.ClusterBus", "cluster cache/control event handlers (no business data)"),
      Map.entry("com.kingsrook.qqq.backend.core.health.HealthRunner", "health check bookkeeping (qqq#1220); checks that need authority use their own principal"),
      Map.entry("com.kingsrook.qqq.backend.core.instances.QInstanceHelpContentManager", "help content reload"),
      Map.entry("com.kingsrook.qqq.backend.core.jobs.SystemScope", "durable-jobs kernel bookkeeping; job work runs as the job's user or principal"),
      Map.entry("com.kingsrook.qqq.backend.core.jobs.progress.JobLogWriter", "append-only job log rows"),
      Map.entry("com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime", "worker/lease bookkeeping; chunks run as the job's session"),
      Map.entry("com.kingsrook.qqq.backend.core.model.metadata.QInstance", "metadata overlays"),
      Map.entry("com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles", "help bundle sync"),
      Map.entry("com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule", "authentication internals (session/token tables)"),
      Map.entry("com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2AuthenticationModule", "authentication internals (session/token tables, logout)"),
      Map.entry("com.kingsrook.qqq.backend.core.state.TableStateProvider", "framework state rows"));



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void onlyAllowListedClassesBuildSystemSessions()
   {
      JavaClasses classes = new ClassFileImporter()
         .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
         .importPackages("com.kingsrook.qqq.backend.core");
      assertThat(classes).isNotEmpty();

      ArchRule rule = noClasses()
         .that().doNotHaveFullyQualifiedName(QSystemUserSession.class.getName())
         .and(com.tngtech.archunit.base.DescribedPredicate.describe("are not allow-listed",
            (com.tngtech.archunit.core.domain.JavaClass javaClass) -> !ALLOWED.containsKey(outermost(javaClass.getName()))))
         .should().callConstructorWhere(target(owner(assignableTo(QSystemUserSession.class))))
         .because("background business work runs as a service principal (docs/SERVICE-IDENTITY.md); system sessions are for allow-listed plumbing only");
      rule.check(classes);
   }



   /*******************************************************************************
    ** The top-level class of a (possibly nested or anonymous) class name.
    *******************************************************************************/
   static String outermost(String className)
   {
      int dollar = className.indexOf('$');
      return (dollar < 0 ? className : className.substring(0, dollar));
   }
}
