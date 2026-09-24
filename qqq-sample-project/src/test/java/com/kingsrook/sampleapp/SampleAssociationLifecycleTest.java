/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.sampleapp;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Canonical Person/pets replacement and cascade behavior under row-lock changes.
 ** Native SQL verifies storage independently of protected QQQ read results.
 *******************************************************************************/
class SampleAssociationLifecycleTest
{
   private static final String PERSON = SampleMetaDataProvider.TABLE_NAME_PERSON;
   private static final String PET = SampleMetaDataProvider.TABLE_NAME_PET;
   private String originalAssociation;
   private String originalJoin;



   /*******************************************************************************
    ** Use the same bundled schema, seed data and Person metadata as the sample.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      QContext.init(SampleMetaDataProvider.defineTestInstance(), new QSession());
      assertEquals(1, QContext.getQInstance().getTable(PERSON).getAssociations().size());
      assertEquals("pets", QContext.getQInstance().getTable(PERSON).getAssociations().get(0).getName());
      originalAssociation = JsonUtils.toJson(QContext.getQInstance().getTable(PERSON).getAssociations());
      originalJoin = JsonUtils.toJson(QContext.getQInstance().getJoin("personJoinPet"));
      assertEquals(4, rows("SELECT id FROM pet WHERE person_id=1").size());
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
    ** Omitted pets are deleted before changing the name used by their owner lock.
    ** A protected empty read alone is not evidence that the SQL rows were deleted.
    *******************************************************************************/
   @Test
   void testOmittedPetsDeletedBeforeOwnerNameChanges() throws Exception
   {
      protectPetsByOwnerName();
      Map<String, Object> before = snapshot();
      assertEquals(List.of(1, 2, 3, 4), visibleTargetPetIds());
      QRecord output = new UpdateAction().execute(replacement(null)).getRecords().get(0);
      assertTrue(output.getErrors().isEmpty());
      assertEquals("Renamed", rows("SELECT first_name FROM person WHERE id=1").get(0).get("FIRST_NAME"));
      assertTrue(rows("SELECT * FROM pet WHERE person_id=1").isEmpty());
      assertTrue(rows("SELECT * FROM pet_note WHERE pet_id IN(1,2,3,4)").isEmpty());
      assertEquals(List.of(), visibleTargetPetIds());
      assertUnrelatedUnchanged(before);
      assertCanonicalRelationshipUnchanged();
   }



   /*******************************************************************************
    ** Pets remain writable while their owner exists, so the cascade must delete
    ** them before deleting the Person referenced by their row-security join.
    *******************************************************************************/
   @Test
   void testPetsDeletedBeforeProtectedOwnerDisappears() throws Exception
   {
      protectPetsByOwnerName();
      Map<String, Object> before = snapshot();
      assertEquals(List.of(1, 2, 3, 4), visibleTargetPetIds());
      DeleteOutput output = new DeleteAction().execute(deletePerson(null));
      assertEquals(1, output.getDeletedRecordCount());
      assertTrue(output.getRecordsWithErrors() == null || output.getRecordsWithErrors().isEmpty());
      assertTrue(rows("SELECT * FROM person WHERE id=1").isEmpty());
      assertTrue(rows("SELECT * FROM pet WHERE person_id=1").isEmpty());
      assertTrue(rows("SELECT * FROM pet_note WHERE pet_id IN(1,2,3,4)").isEmpty());
      assertUnrelatedUnchanged(before);
      assertCanonicalRelationshipUnchanged();
   }



   /*******************************************************************************
    ** Readable pets can still deny DELETE; neither their owner nor the pets change.
    *******************************************************************************/
   @Test
   void testWriteDeniedPetsPreservePersonAndReplacement() throws Exception
   {
      assertDeniedPetsPreservePerson(RecordSecurityLock.LockScope.WRITE);
   }



   /*******************************************************************************
    ** Structural discovery cannot reinterpret unavailable pets as no members.
    *******************************************************************************/
   @Test
   void testUnreadablePetsPreservePersonAndReplacement() throws Exception
   {
      assertDeniedPetsPreservePerson(RecordSecurityLock.LockScope.READ_AND_WRITE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCallerRollbackRestoresOmittedPetsAndOwner() throws Exception
   {
      assertCallerRollback(true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCallerRollbackRestoresDeletedPersonAndPets() throws Exception
   {
      assertCallerRollback(false);
   }



   /*******************************************************************************
    ** These are record-security denials, independent of middleware table grants.
    *******************************************************************************/
   private void assertDeniedPetsPreservePerson(RecordSecurityLock.LockScope scope) throws Exception
   {
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("lifecycleSpecies"));
      QContext.getQInstance().getTable(PET).withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("lifecycleSpecies")
         .withFieldName("speciesId").withLockScope(scope));
      QContext.setQSession(new QSession().withSecurityKeyValue("lifecycleSpecies", 2));
      new QInstanceValidator().revalidate(QContext.getQInstance());
      Map<String, Object> before = snapshot();
      assertEquals(scope == RecordSecurityLock.LockScope.WRITE ? List.of(1, 2, 3, 4) : List.of(), visibleTargetPetIds());
      DeleteOutput deleted = new DeleteAction().execute(deletePerson(null));
      assertEquals(0, deleted.getDeletedRecordCount());
      assertEquals(1, deleted.getRecordsWithErrors().size());
      assertParentFailure(deleted.getRecordsWithErrors().get(0));
      assertEquals(before, snapshot());
      QRecord updated = new UpdateAction().execute(replacement(null)).getRecords().get(0);
      assertParentFailure(updated);
      assertEquals(before, snapshot());
      assertCanonicalRelationshipUnchanged();
   }



   /*******************************************************************************
    ** The caller owns rollback. During its transaction the intended mutations
    ** must actually occur, while another connection still sees the original rows.
    *******************************************************************************/
   private void assertCallerRollback(boolean updating) throws Exception
   {
      protectPetsByOwnerName();
      Map<String, Object> before = snapshot();
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend())))
      {
         if(updating)
         {
            QRecord output = new UpdateAction().execute(replacement(transaction)).getRecords().get(0);
            assertTrue(output.getErrors().isEmpty());
            assertEquals("Renamed", rows(transaction.getConnection(), "SELECT first_name FROM person WHERE id=1").get(0).get("FIRST_NAME"));
         }
         else
         {
            DeleteOutput output = new DeleteAction().execute(deletePerson(transaction));
            assertEquals(1, output.getDeletedRecordCount());
            assertTrue(output.getRecordsWithErrors() == null || output.getRecordsWithErrors().isEmpty());
            assertTrue(rows(transaction.getConnection(), "SELECT * FROM person WHERE id=1").isEmpty());
         }
         assertTrue(rows(transaction.getConnection(), "SELECT * FROM pet WHERE person_id=1").isEmpty());
         assertTrue(rows(transaction.getConnection(), "SELECT * FROM pet_note WHERE pet_id IN(1,2,3,4)").isEmpty());
         assertEquals(before.get("unrelatedNotes"), rows(transaction.getConnection(), "SELECT * FROM pet_note WHERE pet_id NOT IN(1,2,3,4) ORDER BY id"));
         assertEquals(before.get("unrelatedPeople"), rows(transaction.getConnection(), "SELECT * FROM person WHERE id<>1 ORDER BY id"));
         assertEquals(before.get("unrelatedPets"), rows(transaction.getConnection(), "SELECT * FROM pet WHERE person_id<>1 ORDER BY id"));
         assertFalse(transaction.getConnection().isClosed());
         assertEquals(before, snapshot());
         transaction.rollback();
      }
      assertEquals(before, snapshot());
      assertCanonicalRelationshipUnchanged();
   }



   /*******************************************************************************
    ** A scoped metadata variation uses the existing Person/Pet join and data.
    *******************************************************************************/
   private void protectPetsByOwnerName() throws Exception
   {
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("lifecycleOwnerName"));
      QContext.getQInstance().getTable(PET).withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("lifecycleOwnerName")
         .withFieldName("person.firstName").withJoinNameChain(List.of("personJoinPet")));
      QContext.setQSession(new QSession().withSecurityKeyValue("lifecycleOwnerName", "Avery"));
      new QInstanceValidator().revalidate(QContext.getQInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private UpdateInput replacement(RDBMSTransaction transaction)
   {
      return new UpdateInput(PERSON).withInputSource(QInputSource.USER).withTransaction(transaction)
         .withRecord(new QRecord().withValue("id", 1).withValue("firstName", "Renamed").withAssociatedRecords("pets", List.of()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private DeleteInput deletePerson(RDBMSTransaction transaction)
   {
      return new DeleteInput(PERSON).withInputSource(QInputSource.USER).withTransaction(transaction).withPrimaryKeys(List.of(1));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Integer> visibleTargetPetIds() throws Exception
   {
      return new QueryAction().execute(new QueryInput(PET).withInputSource(QInputSource.USER)
         .withFilter(new QQueryFilter(new QFilterCriteria("personId", QCriteriaOperator.EQUALS, 1)))).getRecords().stream()
         .map(record -> record.getValueInteger("id")).sorted().toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertParentFailure(QRecord record)
   {
      assertEquals(1, record.getValueInteger("id"));
      assertFalse(record.getErrors().isEmpty());
      for(String privatePetName : List.of("Charlie", "Coco", "Louie", "Barkley"))
      {
         assertFalse(record.getErrors().toString().contains(privatePetName));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertCanonicalRelationshipUnchanged()
   {
      assertEquals(originalAssociation, JsonUtils.toJson(QContext.getQInstance().getTable(PERSON).getAssociations()));
      assertEquals(originalJoin, JsonUtils.toJson(QContext.getQInstance().getJoin("personJoinPet")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertUnrelatedUnchanged(Map<String, Object> before) throws Exception
   {
      assertEquals(before.get("unrelatedPeople"), rows("SELECT * FROM person WHERE id<>1 ORDER BY id"));
      assertEquals(before.get("unrelatedPets"), rows("SELECT * FROM pet WHERE person_id<>1 ORDER BY id"));
      assertEquals(before.get("unrelatedNotes"), rows("SELECT * FROM pet_note WHERE pet_id NOT IN(1,2,3,4) ORDER BY id"));
   }



   /*******************************************************************************
    ** Retain nulls and native types in snapshots, including untouched timestamps.
    *******************************************************************************/
   private Map<String, Object> snapshot() throws Exception
   {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("people", rows("SELECT * FROM person ORDER BY id"));
      result.put("pets", rows("SELECT * FROM pet ORDER BY id"));
      result.put("notes", rows("SELECT * FROM pet_note ORDER BY id"));
      result.put("unrelatedNotes", rows("SELECT * FROM pet_note WHERE pet_id NOT IN(1,2,3,4) ORDER BY id"));
      result.put("unrelatedPeople", rows("SELECT * FROM person WHERE id<>1 ORDER BY id"));
      result.put("unrelatedPets", rows("SELECT * FROM pet WHERE person_id<>1 ORDER BY id"));
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Map<String, Object>> rows(String query) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()))
      {
         return rows(connection, query);
      }
   }



   /*******************************************************************************
    ** The caller retains ownership of its transaction connection.
    *******************************************************************************/
   private List<Map<String, Object>> rows(Connection connection, String query) throws Exception
   {
      List<Map<String, Object>> rows = new ArrayList<>();
      try(Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query))
      {
         while(result.next())
         {
            Map<String, Object> row = new LinkedHashMap<>();
            for(int i = 1; i <= result.getMetaData().getColumnCount(); i++)
            {
               row.put(result.getMetaData().getColumnLabel(i).toUpperCase(Locale.ROOT), result.getObject(i));
            }
            rows.add(row);
         }
      }
      return rows;
   }
}
