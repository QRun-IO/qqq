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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryQueryAction;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** DML prefetch retains only actual native identities while preserving ordinary
 ** presentation and public Query customizer behavior.
 *******************************************************************************/
class QueryActionDmlIdentityTest extends BaseTest
{
   private static final String TABLE = "dmlIdentity";
   private QTableMetaData table;



   /*******************************************************************************
    ** Real Memory rows provide deterministic native identities and ordering.
    *******************************************************************************/
   @BeforeEach
   void seed() throws QException
   {
      resetCustomizer();
      table = new QTableMetaData().withName(TABLE).withBackendName(TestUtils.MEMORY_BACKEND_NAME).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("owner", QFieldType.INTEGER));
      QContext.getQInstance().addTable(table);
      List<QRecord> inserted = new InsertAction().execute(new InsertInput(TABLE).withOmitDmlAudit(true).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "First").withValue("owner", 1),
         new QRecord().withValue("id", 2).withValue("name", "Second").withValue("owner", 2)))).getRecords();
      assertEquals(List.of(1, 2), inserted.stream().map(record -> record.getValue("id")).toList());
      assertTrue(inserted.stream().allMatch(record -> record.getErrors().isEmpty()));
      table.getField("id").setIsHidden(true);
      table.withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(IdentityCustomizer.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void resetCustomizer()
   {
      IdentityCustomizer.behavior = Behavior.KEEP;
      IdentityCustomizer.replacementId = 2;
   }



   /*******************************************************************************
    ** A fresh object cannot present a READ-excluded requested row as visible.
    *******************************************************************************/
   @Test
   void testFreshReplacementCannotInventNativeIdentity() throws Exception
   {
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("identityOwner"));
      QContext.getQSession().withSecurityKeyValue("identityOwner", 1);
      table.withRecordSecurityLock(new RecordSecurityLock().withFieldName("owner").withSecurityKeyType("identityOwner")
         .withLockScope(RecordSecurityLock.LockScope.READ));
      assertEquals(List.of(1), new MemoryQueryAction().execute(input(1, 2)).getRecords().stream().map(record -> record.getValue("id")).toList());

      IdentityCustomizer.behavior = Behavior.FRESH_OTHER;
      QException failure = assertThrows(QException.class, () -> new QueryAction().executeForDml(input(1, 2)));
      assertTrue(failure.getMessage().contains("outside the native result"));
      assertEquals(List.of(1), new MemoryQueryAction().execute(input(1, 2)).getRecords().stream().map(record -> record.getValue("id")).toList());
   }



   /*******************************************************************************
    ** Reordered immutable lists work for both copied and reconstructed records.
    *******************************************************************************/
   @Test
   void testSameKeyReconstructionAndReorderedClonesRemainPrivate() throws Exception
   {
      String metadata = JsonUtils.toJson(table);
      for(Behavior behavior : List.of(Behavior.COPY_REVERSED, Behavior.FRESH_REVERSED))
      {
         IdentityCustomizer.behavior = behavior;
         QueryInput input = input(1, 2);
         List<QRecord> records = new QueryAction().executeForDml(input).getRecords();
         assertAll(
            () -> assertEquals(List.of("Second", "First"), records.stream().map(record -> record.getValueString("name")).toList()),
            () -> assertEquals(List.of(2, 1), resolvedKeys(records)),
            () -> assertTrue(records.stream().allMatch(record -> !record.getValues().containsKey("id"))),
            () -> assertFalse(JsonUtils.toJson(records).contains("primaryKeyIdentity")),
            () -> assertSame(table, input.getTable()),
            () -> assertEquals(metadata, JsonUtils.toJson(table)));
      }
   }



   /*******************************************************************************
    ** A copied record cannot change its key even to another native returned key.
    *******************************************************************************/
   @Test
   void testCopiedChangedKeyRejectsEvenWithinNativeSet()
   {
      IdentityCustomizer.behavior = Behavior.CHANGED_COPY;
      QException failure = assertThrows(QException.class, () -> new QueryAction().executeForDml(input(1, 2)));
      assertTrue(failure.getMessage().contains("presentation changed"));
   }



   /*******************************************************************************
    ** Reconstructing a sanitized record without identity cannot invent a match.
    *******************************************************************************/
   @Test
   void testReconstructionWithoutKeyRejects()
   {
      IdentityCustomizer.behavior = Behavior.WITHOUT_KEY;
      assertThrows(QException.class, () -> new QueryAction().executeForDml(input(1)));
   }



   /*******************************************************************************
    ** Public calls on the same action do not retain the internal identity policy.
    *******************************************************************************/
   @Test
   void testPublicExecuteResetsInternalModeOnReuse() throws Exception
   {
      QueryAction action = new QueryAction();
      QRecord internal = action.executeForDml(input(1)).getRecords().get(0);
      assertEquals(1, internal.resolvePrimaryKey(table));
      assertFalse(internal.getValues().containsKey("id"));
      QRecord ordinary = action.execute(input(1)).getRecords().get(0);
      assertAll(
         () -> assertNull(ordinary.resolvePrimaryKey(table)),
         () -> assertFalse(ordinary.getValues().containsKey("id")),
         () -> assertEquals(JsonUtils.toJson(internal), JsonUtils.toJson(ordinary)));

      IdentityCustomizer.behavior = Behavior.FRESH_OTHER;
      QRecord publicReplacement = action.execute(input(1)).getRecords().get(0);
      assertEquals("Reconstructed", publicReplacement.getValueString("name"));
      assertFalse(publicReplacement.getValues().containsKey("id"));
      assertNull(publicReplacement.resolvePrimaryKey(table));
   }



   /*******************************************************************************
    ** Prior native keys cannot authorize a later invocation's fabricated record.
    *******************************************************************************/
   @Test
   void testNativeIdentitySetResetsBetweenInternalQueries() throws Exception
   {
      QueryAction action = new QueryAction();
      assertEquals(List.of(1), resolvedKeys(action.executeForDml(input(1)).getRecords()));
      IdentityCustomizer.behavior = Behavior.FRESH_OTHER;
      IdentityCustomizer.replacementId = 1;
      assertThrows(QException.class, () -> action.executeForDml(input(2)));
   }



   /*******************************************************************************
    ** The internal materialized-prefetch entry cannot publish into a record pipe.
    *******************************************************************************/
   @Test
   void testInternalPipeEntryRejectsBeforePublication()
   {
      RecordPipe pipe = new RecordPipe(3);
      try
      {
         QException failure = assertThrows(QException.class, () -> new QueryAction().executeForDml(input(1, 2).withRecordPipe(pipe)));
         assertTrue(failure.getMessage().contains("materialized record list"));
         assertEquals(0, pipe.getTotalRecordCount());
         assertTrue(pipe.consumeAvailableRecords().isEmpty());
      }
      finally
      {
         pipe.terminate();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QueryInput input(Integer... keys)
   {
      return new QueryInput(TABLE).withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, List.of(keys)))
         .withOrderBy(new QFilterOrderBy("id", true)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Serializable> resolvedKeys(List<QRecord> records) throws QException
   {
      List<Serializable> keys = new ArrayList<>();
      for(QRecord record : records)
      {
         keys.add(record.resolvePrimaryKey(table));
      }
      return keys;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private enum Behavior
   {
      KEEP, COPY_REVERSED, FRESH_REVERSED, FRESH_OTHER, CHANGED_COPY, WITHOUT_KEY
   }



   /*******************************************************************************
    ** Real table customizer returns fresh or copied records, without provider mocks.
    *******************************************************************************/
   public static class IdentityCustomizer implements TableCustomizerInterface
   {
      private static Behavior behavior = Behavior.KEEP;
      private static Integer replacementId = 2;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         if(behavior == Behavior.FRESH_OTHER)
         {
            return List.of(new QRecord().withValue("id", replacementId).withValue("name", "Reconstructed").withValue("owner", 1));
         }
         if(behavior == Behavior.WITHOUT_KEY)
         {
            return List.of(new QRecord().withValue("name", "Reconstructed"));
         }
         if(behavior == Behavior.CHANGED_COPY)
         {
            return List.of(new QRecord(records.get(0)).withValue("id", 2));
         }
         if(behavior == Behavior.COPY_REVERSED || behavior == Behavior.FRESH_REVERSED)
         {
            List<QRecord> result = new ArrayList<>();
            for(QRecord record : records)
            {
               result.add(behavior == Behavior.COPY_REVERSED ? new QRecord(record)
                  : new QRecord().withValue("id", record.getValue("id")).withValue("name", record.getValue("name")).withValue("owner", record.getValue("owner")));
            }
            Collections.reverse(result);
            return List.copyOf(result);
         }
         return records;
      }
   }
}
