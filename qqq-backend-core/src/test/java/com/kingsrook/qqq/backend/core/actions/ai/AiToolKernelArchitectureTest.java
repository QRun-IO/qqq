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

package com.kingsrook.qqq.backend.core.actions.ai;


import com.kingsrook.qqq.backend.core.model.session.QSession;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Architecture rules for the AI tool kernel (spec 3.1 / 7.4): kernel code
 ** never uses a system session, and never constructs a session of its own -
 ** every tool runs as the requesting user's session.
 *******************************************************************************/
class AiToolKernelArchitectureTest
{
   public static final String KERNEL_PACKAGE = "com.kingsrook.qqq.backend.core.actions.ai";

   private static final ArchRule NO_SYSTEM_SESSION = noClasses()
      .should().dependOnClassesThat().areAssignableTo(QSystemUserSession.class)
      .because("the AI tool kernel must only ever run as the requesting user's session");

   private static final ArchRule NO_SESSION_CONSTRUCTION = noClasses()
      .should().callConstructorWhere(target(owner(assignableTo(QSession.class))))
      .because("the AI tool kernel must never create a session - it only uses the one it is given");



   /*******************************************************************************
    ** The kernel's main (non-test) classes.
    *******************************************************************************/
   private static JavaClasses kernelClasses()
   {
      return (new ClassFileImporter()
         .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
         .importPackages(KERNEL_PACKAGE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testKernelNeverUsesSystemSession()
   {
      JavaClasses classes = kernelClasses();
      assertThat(classes).isNotEmpty();
      assertThat(classes.contain(AiToolKernel.class)).isTrue();
      NO_SYSTEM_SESSION.check(classes);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testKernelNeverConstructsSessions()
   {
      NO_SESSION_CONSTRUCTION.check(kernelClasses());
   }



   /*******************************************************************************
    ** The rules do catch violations (so they can't pass vacuously).
    *******************************************************************************/
   @Test
   void testRulesDetectViolations()
   {
      JavaClasses violators = new ClassFileImporter().importClasses(ViolatingClass.class);
      assertThatThrownBy(() -> NO_SYSTEM_SESSION.check(violators)).isInstanceOf(AssertionError.class);
      assertThatThrownBy(() -> NO_SESSION_CONSTRUCTION.check(violators)).isInstanceOf(AssertionError.class);
   }



   /*******************************************************************************
    ** A class that breaks both rules - only for testRulesDetectViolations.
    *******************************************************************************/
   static class ViolatingClass
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      QSession makeSessions()
      {
         new QSystemUserSession();
         return (new QSession());
      }
   }
}
