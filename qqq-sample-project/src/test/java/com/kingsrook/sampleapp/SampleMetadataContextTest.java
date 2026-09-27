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

package com.kingsrook.sampleapp;


import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.FieldLabTableMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Canonical metadata and explicit context ownership in a reusable sample worker.
 *******************************************************************************/
class SampleMetadataContextTest
{
   private QInstance instance;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** The sample combines programmatic metadata, package discovery and bundled YAML.
    *******************************************************************************/
   @Test
   void testCanonicalMetadataEnrichment() throws Exception
   {
      QTableMetaData person = instance.getTable("person");
      QTableMetaData fields = instance.getTable(FieldLabTableMetaDataProducer.NAME);
      assertNotNull(instance.getTable("pet"));
      assertNotNull(fields);
      assertNotNull(instance.getPossibleValueSource("petSpecies"));
      person.setLabel(null);
      person.getField("firstName").setLabel(null);
      person.getSections().get(0).setLabel(null);
      new QInstanceValidator().validate(instance);
      assertTrue(instance.getHasBeenValidated());
      assertEquals("Person", person.getLabel());
      assertEquals("First Name", person.getField("firstName").getLabel());
      assertEquals("Identity", person.getSections().get(0).getLabel());
      assertEquals("first_name", person.getField("firstName").getBackendName());
      assertEquals("create_date", fields.getField("createDate").getBackendName());
      assertEquals(DynamicDefaultValueBehavior.CREATE_DATE, fields.getField("createDate").getBehaviorOrDefault(instance, DynamicDefaultValueBehavior.class));
      assertEquals(DynamicDefaultValueBehavior.MODIFY_DATE, fields.getField("modifyDate").getBehaviorOrDefault(instance, DynamicDefaultValueBehavior.class));
      QContext.init(instance, new QSession());
      assertEquals(5, CountAction.execute("person", null));
   }



   /*******************************************************************************
    ** Each malformed variant begins with the independently valid canonical instance.
    *******************************************************************************/
   @Test
   void testMissingMetadataReferencesAreRejected() throws Exception
   {
      List<Consumer<QInstance>> variants = List.of(
         value -> value.getTable("person").setBackendName("missingBackend"),
         value -> value.getTable("person").setPrimaryKeyField("missingField"),
         value -> value.getTable("person").getField("firstName").setPossibleValueSourceName("missingPvs"),
         value -> value.getJoin("personJoinPet").setRightTable("missingJoinTable"),
         value -> value.getTable("person").getSections().get(0).setFieldNames(List.of("missingSectionField")),
         value -> value.getTable("person").getAssociations().get(0).setAssociatedTableName("missingAssociationTable"));
      for(Consumer<QInstance> variant : variants)
      {
         QInstance candidate = SampleMetaDataProvider.defineTestInstance();
         new QInstanceValidator().validate(candidate);
         variant.accept(candidate);
         QInstanceValidationException failure = assertThrows(QInstanceValidationException.class, () -> new QInstanceValidator().revalidate(candidate));
         assertFalse(failure.getMessage().isBlank());
         assertFalse(candidate.getHasBeenValidated());
      }
      new QInstanceValidator().validate(instance);
      assertTrue(instance.getHasBeenValidated());
   }



   /*******************************************************************************
    ** Duplicate declarations and unresolved executable references cannot boot.
    *******************************************************************************/
   @Test
   void testDuplicateKeysAndInvalidCodeAreRejected() throws Exception
   {
      instance.getTable("person").withUniqueKey(new UniqueKey("email")).withUniqueKey(new UniqueKey("email"));
      QInstanceValidationException duplicate = assertThrows(QInstanceValidationException.class, () -> new QInstanceValidator().validate(instance));
      assertTrue(duplicate.getMessage().contains("more than one uniqueKey with the same set of fields"), duplicate.getMessage());
      QInstance invalidCode = SampleMetaDataProvider.defineTestInstance();
      invalidCode.getProcess(SampleMetaDataProvider.PROCESS_NAME_SIMPLE_THROW).getBackendStep(SampleMetaDataProvider.STEP_NAME_THROWER)
         .setCode(new QCodeReference("com.kingsrook.sampleapp.NoSuchBackendStep", QCodeType.JAVA));
      assertThrows(QInstanceValidationException.class, () -> new QInstanceValidator().validate(invalidCode));
   }



   /*******************************************************************************
    ** A plain worker has no inherited user; only explicit capture enables its query.
    *******************************************************************************/
   @Test
   void testCapturedContextAndWorkerCleanup() throws Exception
   {
      QSession caller = new QSession().withUser(new QUser().withIdReference("sample-caller"));
      QContext.init(instance, caller);
      CountInput action = new CountInput("person");
      QContext.pushAction(action);
      QContext.setObject("sampleMarker", "caller");
      var captured = QContext.capture();
      try(var worker = Executors.newSingleThreadExecutor())
      {
         worker.submit(() ->
         {
            assertNull(QContext.getQInstance());
            assertNull(QContext.getQSession());
            assertThrows(QException.class, () -> new CountAction().execute(action));
            try
            {
               QContext.init(captured);
               assertSame(instance, QContext.getQInstance());
               assertSame(caller, QContext.getQSession());
               assertSame(action, QContext.getFirstActionInStack().orElseThrow());
               assertEquals(5, CountAction.execute("person", null));
               QContext.setObject("sampleMarker", "worker");
            }
            finally
            {
               QContext.clear();
            }
            return null;
         }).get(10, TimeUnit.SECONDS);
         worker.submit(() ->
         {
            assertNull(QContext.getQInstance());
            assertNull(QContext.getQSession());
            assertNull(QContext.getQBackendTransaction());
            assertNull(QContext.getActionStack());
            assertNull(QContext.getObject("sampleMarker"));
            return null;
         }).get(10, TimeUnit.SECONDS);
      }
      assertSame(instance, QContext.getQInstance());
      assertSame(caller, QContext.getQSession());
      assertSame(action, QContext.getFirstActionInStack().orElseThrow());
      assertEquals("caller", QContext.getObject("sampleMarker"));
      assertEquals(5, CountAction.execute("person", null));
   }



   /*******************************************************************************
    ** Temporary scope restores the caller even when its work throws.
    *******************************************************************************/
   @Test
   void testTemporaryContextRestoresCallerAfterFailure() throws Exception
   {
      QSession temporary = new QSession().withUser(new QUser().withIdReference("sample-temporary"));
      QContext.init(instance, temporary);
      var captured = QContext.capture();
      QSession caller = new QSession().withUser(new QUser().withIdReference("sample-caller"));
      QContext.init(instance, caller);
      CountInput action = new CountInput("person");
      QContext.pushAction(action);
      IllegalStateException failure = assertThrows(IllegalStateException.class, () -> QContext.withTemporaryContext(captured, () ->
      {
         assertSame(temporary, QContext.getQSession());
         assertEquals(5, CountAction.execute("person", null));
         throw new IllegalStateException("Synthetic sample failure");
      }));
      assertEquals("Synthetic sample failure", failure.getMessage());
      assertSame(caller, QContext.getQSession());
      assertSame(instance, QContext.getQInstance());
      assertSame(action, QContext.getFirstActionInStack().orElseThrow());
      assertEquals(5, CountAction.execute("person", null));
   }



   /*******************************************************************************
    ** Reusing one thread does not retain the previous request's user or local data.
    *******************************************************************************/
   @Test
   void testWorkerReuseIsolatesUsersAfterFailure() throws Exception
   {
      try(var worker = Executors.newSingleThreadExecutor())
      {
         for(String user : List.of("sample-first", "sample-second"))
         {
            var failedRequest = worker.submit(() ->
            {
               assertNull(QContext.getQInstance());
               assertNull(QContext.getQSession());
               assertNull(QContext.getObject("sampleMarker"));
               try
               {
                  QContext.init(instance, new QSession().withUser(new QUser().withIdReference(user)));
                  QContext.setObject("sampleMarker", user);
                  assertEquals(user, QContext.getQSession().getUser().getIdReference());
                  assertEquals(5, CountAction.execute("person", null));
                  throw new IllegalArgumentException("Synthetic request failure for " + user);
               }
               finally
               {
                  QContext.clear();
               }
            });
            ExecutionException failure = assertThrows(ExecutionException.class, () -> failedRequest.get(10, TimeUnit.SECONDS));
            assertEquals("Synthetic request failure for " + user, failure.getCause().getMessage());
         }
      }
      assertNull(QContext.getQInstance());
      assertNull(QContext.getQSession());
   }
}
