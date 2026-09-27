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
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.ReplaceAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Data contracts exercised by the sample's real H2-backed Person table.
 *******************************************************************************/
class SampleDataContractTest
{
   private static final String PERSON = SampleMetaDataProvider.TABLE_NAME_PERSON;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      QContext.init(SampleMetaDataProvider.defineTestInstance(), new QSession());
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
    **
    *******************************************************************************/
   @Test
   void testFilteredCountsAndInvalidFields() throws Exception
   {
      insertPerson("One", "count-fixture", "one@example.invalid");
      insertPerson("Two", "count-fixture", "two@example.invalid");
      QQueryFilter filter = equalsFilter("lastName", "count-fixture");
      assertEquals(2, CountAction.execute(PERSON, filter));
      assertEquals(QueryAction.execute(PERSON, filter).size(), CountAction.execute(PERSON, filter));
      assertEquals(0, CountAction.execute(PERSON, equalsFilter("lastName", "no-such-fixture")));
      assertThrows(QException.class, () -> CountAction.execute(PERSON, equalsFilter("noSuchField", "value")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTenantReadAndWriteIsolation() throws Exception
   {
      Integer allowedId = insertPerson("Allowed", "tenant-a", "a@example.invalid").getValueInteger("id");
      Integer deniedId = insertPerson("Denied", "tenant-b", "b@example.invalid").getValueInteger("id");
      QInstance instance = QContext.getQInstance();
      instance.addSecurityKeyType(new QSecurityKeyType().withName("tenant"));
      instance.getTable(PERSON).withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("tenant").withFieldName("lastName"));
      QContext.setQSession(new QSession().withSecurityKeyValue("tenant", "tenant-a"));

      assertEquals(1, CountAction.execute(PERSON, null));
      assertEquals(1, CountAction.execute(PERSON, equalsFilter("firstName", "Allowed")));
      assertEquals(0, CountAction.execute(PERSON, equalsFilter("firstName", "Denied")));
      assertEquals(List.of(allowedId), QueryAction.execute(PERSON, null).stream().map(record -> record.getValueInteger("id")).toList());
      assertNull(GetAction.execute(PERSON, deniedId));
      assertEquals("Allowed", GetAction.execute(PERSON, allowedId).getValueString("firstName"));
      QRecord deniedUpdate = new UpdateAction().execute(new UpdateInput(PERSON)
         .withRecord(new QRecord().withValue("id", deniedId).withValue("firstName", "Forbidden"))).getRecords().get(0);
      assertFalse(deniedUpdate.getErrors().isEmpty());

      QContext.setQSession(new QSession().withSecurityKeyValue("tenant", "tenant-b"));
      assertEquals("Denied", GetAction.execute(PERSON, deniedId).getValueString("firstName"));
      assertNull(GetAction.execute(PERSON, allowedId));
      QContext.setQSession(new QSession());
      assertEquals(0, CountAction.execute(PERSON, null));
      assertEquals(0, CountAction.execute(PERSON, equalsFilter("firstName", "Allowed")));
      assertEquals(0, CountAction.execute(PERSON, equalsFilter("firstName", "Denied")));
      assertEquals(0, QueryAction.execute(PERSON, null).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReplaceKeepsRecordsOutsideItsFilter() throws Exception
   {
      Integer keptId = insertPerson("Keep", "outside", "keep@example.invalid").getValueInteger("id");
      Integer updatedId = insertPerson("Original", "replace-fixture", "update@example.invalid").getValueInteger("id");
      Integer deletedId = insertPerson("Remove", "replace-fixture", "remove@example.invalid").getValueInteger("id");
      ReplaceInput input = new ReplaceInput();
      input.setTableName(PERSON);
      input.setKey(new UniqueKey("email"));
      input.setFilter(equalsFilter("lastName", "replace-fixture"));
      input.setRecords(List.of(
         person("Changed", "replace-fixture", "update@example.invalid"),
         person("New", "replace-fixture", "new@example.invalid")));
      ReplaceOutput output = new ReplaceAction().execute(input);
      assertEquals(1, output.getInsertOutput().getRecords().size());
      assertEquals(1, output.getUpdateOutput().getRecords().size());
      assertEquals(1, output.getDeleteOutput().getDeletedRecordCount());
      assertEquals("Keep", GetAction.execute(PERSON, keptId).getValueString("firstName"));
      assertEquals("Changed", GetAction.execute(PERSON, updatedId).getValueString("firstName"));
      assertNull(GetAction.execute(PERSON, deletedId));
      assertEquals(2, CountAction.execute(PERSON, equalsFilter("lastName", "replace-fixture")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insertPerson(String firstName, String lastName, String email) throws QException
   {
      QRecord inserted = new InsertAction().execute(new InsertInput(PERSON).withRecord(person(firstName, lastName, email))).getRecords().get(0);
      assertEquals(0, inserted.getErrors().size());
      return inserted;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord person(String firstName, String lastName, String email)
   {
      return new QRecord().withValue("firstName", firstName).withValue("lastName", lastName).withValue("email", email);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QQueryFilter equalsFilter(String field, String value)
   {
      return new QQueryFilter(new QFilterCriteria(field, QCriteriaOperator.EQUALS, value));
   }
}
