/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.picocli;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** CLI invocations own their authentication context and restore their caller.
 *******************************************************************************/
public class QPicoCliContextTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      TestUtils.primeTestDatabase();
      QContext.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** A standalone CLI must not depend on another entry point preparing QContext.
    *******************************************************************************/
   @Test
   void testFreshContextCanCountRecords()
   {
      CliResult result = run(new QPicoCliImplementation(TestUtils.defineInstance()), "person", "count");
      assertEquals(0, result.exitCode(), result.error());
      assertEquals(5, new JSONObject(result.output()).getInt("count"));
      assertNull(QContext.getQInstance());
      assertNull(QContext.getQSession());
      assertNull(QContext.getObjects());
   }



   /*******************************************************************************
    ** A caller's tenant key must not authorize the newly authenticated CLI user.
    *******************************************************************************/
   @Test
   void testAuthenticatedSessionReplacesPriorTenant()
   {
      QInstance instance = TestUtils.defineInstance();
      instance.addSecurityKeyType(new QSecurityKeyType().withName("tenant"));
      instance.getTable("person").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("tenant").withFieldName("id"));
      QSession caller = new QSession().withSecurityKeyValue("tenant", 1);
      QContext.init(instance, caller);
      QContext.setObject("caller-only", "preserve");
      CapturedContext original = QContext.capture();
      var originalObjects = QContext.getObjects();

      CliResult result = run(new QPicoCliImplementation(instance), "person", "count");
      assertEquals(0, result.exitCode(), result.error());
      assertEquals(0, new JSONObject(result.output()).getInt("count"));
      assertEquals(original, QContext.capture());
      assertSame(originalObjects, QContext.getObjects());
   }



   /*******************************************************************************
    ** Authentication customizers must see this CLI's instance before execution.
    *******************************************************************************/
   @Test
   void testCustomizerSeesCliContextAndRestoresCaller()
   {
      QInstance callerInstance = TestUtils.defineInstance();
      QContext.init(callerInstance, new QSession());
      QContext.setObject("caller-only", "preserve");
      CapturedContext original = QContext.capture();
      var originalObjects = QContext.getObjects();
      QInstance cliInstance = TestUtils.defineInstance();
      cliInstance.getAuthentication().withCustomizer(new QCodeReference(ContextCheckingCustomizer.class));
      CliResult result = run(new QPicoCliImplementation(cliInstance), "person", "count");
      assertEquals(0, result.exitCode(), result.error());
      assertEquals(5, new JSONObject(result.output()).getInt("count"));
      assertEquals(original, QContext.capture());
      assertSame(originalObjects, QContext.getObjects());
      assertNull(QContext.getObject("cli-only"));
   }



   /*******************************************************************************
    ** Failed authentication and bad arguments must also restore the caller.
    *******************************************************************************/
   @Test
   void testFailuresRestoreCaller()
   {
      QContext.init(TestUtils.defineInstance(), new QSession());
      QContext.setObject("caller-only", "preserve");
      CapturedContext original = QContext.capture();
      var originalObjects = QContext.getObjects();
      QInstance rejected = TestUtils.defineInstance();
      rejected.getAuthentication().withCustomizer(new QCodeReference(RejectingCustomizer.class));
      CliResult result = run(new QPicoCliImplementation(rejected), "person", "count");
      assertNotEquals(0, result.exitCode());
      assertTrue(result.error().contains("synthetic authentication rejection"));
      assertEquals(original, QContext.capture());
      assertSame(originalObjects, QContext.getObjects());
      assertNotEquals(0, run(new QPicoCliImplementation(TestUtils.defineInstance()), "no-such-command").exitCode());
      assertEquals(original, QContext.capture());
      assertSame(originalObjects, QContext.getObjects());
   }



   /*******************************************************************************
    ** Constructing another CLI must not replace an earlier CLI's instance.
    *******************************************************************************/
   @Test
   void testMultipleCliObjectsKeepTheirOwnInstances()
   {
      QInstance first = TestUtils.defineInstance();
      first.getTable("person").setLabel("First People");
      QPicoCliImplementation firstCli = new QPicoCliImplementation(first);
      QInstance second = TestUtils.defineInstance();
      second.getTable("person").setLabel("Second People");
      new QPicoCliImplementation(second);
      CliResult result = run(firstCli, "person", "meta-data");
      assertEquals(0, result.exitCode(), result.error());
      assertTrue(result.output().contains("First People"), result.output());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CliResult run(QPicoCliImplementation cli, String... args)
   {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      ByteArrayOutputStream error = new ByteArrayOutputStream();
      int exit = cli.runCli("context-test", args, new PrintStream(output), new PrintStream(error));
      return new CliResult(exit, output.toString(StandardCharsets.UTF_8), error.toString(StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private record CliResult(int exitCode, String output, String error)
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ContextCheckingCustomizer implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         assertSame(instance, QContext.getQInstance());
         assertNull(QContext.getQSession());
         assertNull(QContext.getObject("caller-only"));
         QContext.setObject("cli-only", "discard");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RejectingCustomizer implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         throw new IllegalStateException("synthetic authentication rejection");
      }
   }
}
