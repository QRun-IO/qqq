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


package com.kingsrook.qqq.backend.core.instances;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QMetaDataVariableInterpreter's trusted vs. untrusted input mode:
 ** untrusted interpreters never resolve env, prop or file references.
 *******************************************************************************/
class QMetaDataVariableInterpreterUntrustedInputTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   private QMetaDataVariableInterpreter withEnv()
   {
      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();
      interpreter.setEnvironmentOverrides(Map.of("QQQ_UNTRUSTED_TEST_VAR", "server-secret"));
      return (interpreter);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTrustedByDefaultWithoutValueMaps()
   {
      QMetaDataVariableInterpreter interpreter = withEnv();
      assertTrue(interpreter.isTrustedInput());
      assertEquals("server-secret", interpreter.interpret("${env.QQQ_UNTRUSTED_TEST_VAR}"));
      assertEquals(System.getProperty("user.home"), interpreter.interpret("${prop.user.home}"));
      assertEquals("server-secret", interpreter.interpret("${prop.nope.nope}??${env.QQQ_UNTRUSTED_TEST_VAR}"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValueMapsMakeUntrustedByDefault()
   {
      QMetaDataVariableInterpreter interpreter = withEnv();
      interpreter.addValueMap("input", Map.of("x", "y"));
      assertFalse(interpreter.isTrustedInput());

      assertEquals("${env.QQQ_UNTRUSTED_TEST_VAR}", interpreter.interpret("${env.QQQ_UNTRUSTED_TEST_VAR}"));
      assertEquals("${prop.user.home}", interpreter.interpret("${prop.user.home}"));
      assertEquals("${env.QQQ_UNTRUSTED_TEST_VAR}??${prop.user.home}", interpreter.interpret("${env.QQQ_UNTRUSTED_TEST_VAR}??${prop.user.home}"));

      ////////////////////////////////////
      // input references are unchanged //
      ////////////////////////////////////
      assertEquals("y", interpreter.interpret("${input.x}"));
      assertEquals("y", interpreter.interpret("${env.QQQ_UNTRUSTED_TEST_VAR}??${input.x}"));
      assertEquals("plain", interpreter.interpret("plain"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExplicitModes()
   {
      assertEquals("${env.QQQ_UNTRUSTED_TEST_VAR}", withEnv().withTrustedInput(false).interpret("${env.QQQ_UNTRUSTED_TEST_VAR}"));

      QMetaDataVariableInterpreter interpreter = withEnv().withTrustedInput(true);
      interpreter.addValueMap("input", Map.of("x", "y"));
      assertEquals("server-secret", interpreter.interpret("${env.QQQ_UNTRUSTED_TEST_VAR}"));
   }



   /*******************************************************************************
    ** A user-supplied filter value is treated as literal text.
    *******************************************************************************/
   @Test
   void testQueryFilterValuesStayLiteral() throws QException
   {
      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("a", QCriteriaOperator.EQUALS, "${env.PATH}"))
         .withCriteria(new QFilterCriteria("b", QCriteriaOperator.EQUALS, "${prop.user.home}"))
         .withCriteria(new QFilterCriteria("c", QCriteriaOperator.EQUALS, "${input.x}"));

      filter.interpretValues(Map.<String, Serializable>of("x", "y"));

      List<QFilterCriteria> criteria = filter.getCriteria();
      assertEquals(List.of("${env.PATH}"), criteria.get(0).getValues());
      assertEquals(List.of("${prop.user.home}"), criteria.get(1).getValues());
      assertEquals(List.of("y"), criteria.get(2).getValues());
   }

}
