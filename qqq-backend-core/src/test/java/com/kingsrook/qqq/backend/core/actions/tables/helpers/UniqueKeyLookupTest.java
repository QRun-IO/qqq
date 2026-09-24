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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.UpdateInterface;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryQueryAction;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryUpdateAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native lookups accept two private schema-bound shapes and fail closed.
 *******************************************************************************/
class UniqueKeyLookupTest extends BaseTest
{
   private QTableMetaData table;
   private UniqueKey key;
   private static UniqueKeyLookup.Input captured;
   private static boolean returnError;
   private static boolean omitProjection;
   private static boolean refuseStoredLookup;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void seed() throws QException
   {
      QBackendModuleDispatcher.registerBackendModule(new CapturingModule());
      QContext.getQInstance().addBackend(new QBackendMetaData().withName("uniqueProbe").withBackendType(CapturingModule.class));
      key = new UniqueKey("amount", "binary");
      table = new QTableMetaData().withName("uniqueProbe").withBackendName("uniqueProbe").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsHidden(true).withIsHeavy(true))
         .withField(new QFieldMetaData("amount", QFieldType.DECIMAL))
         .withField(new QFieldMetaData("binary", QFieldType.BLOB).withIsHeavy(true))
         .withField(new QFieldMetaData("private", QFieldType.STRING)).withUniqueKey(key);
      QContext.getQInstance().addTable(table);
      new InsertAction().execute(new InsertInput(table.getName()).withSkipUniqueKeyCheck(true).withRecord(
         candidate().withValue("id", 1).withValue("private", "never projected")));
      captured = null;
      returnError = false;
      omitProjection = false;
      refuseStoredLookup = false;
   }



   /*******************************************************************************
    ** Native matches use typed values, preserve all projection fields, and copy arrays.
    *******************************************************************************/
   @Test
   void testTypedProjectionSnapshotsAndPrivateConstruction() throws QException
   {
      QRecord result = UniqueKeyLookup.findConflicts(table, key, candidate(), null).get(0);
      assertThat(result.getValues().keySet()).containsExactlyInAnyOrder("id", "amount", "binary");
      assertEquals(1, result.getValue("id"));
      assertTrue(captured.matches(candidate().withValue("amount", "2.000")));
      assertFalse(captured.matches(candidate().withValue("amount", null)));
      assertFalse(captured.matches(candidate().withValue("binary", new byte[] { 2, 1 })));
      QueryInput copy = captured.newQueryInput();
      assertTrue(copy.getShouldFetchHeavyFields());
      assertFalse(copy.getShouldOmitHiddenFields());
      assertFalse(copy.getShouldMaskPasswords());
      copy.getTable().getField("amount").setType(QFieldType.STRING);
      copy.getFilter().getCriteria().clear();
      copy.getFieldNamesToInclude().clear();
      assertEquals(QFieldType.DECIMAL, captured.newQueryInput().getTable().getField("amount").getType());
      assertEquals(2, captured.newQueryInput().getFilter().getCriteria().size());
      assertEquals(Set.of("id", "amount", "binary"), captured.newQueryInput().getFieldNamesToInclude());
      ((byte[]) result.getValue("binary"))[0] = 9;
      assertEquals(1, ((byte[]) UniqueKeyLookup.findConflicts(table, key, candidate(), null).get(0).getValue("binary"))[0]);
      assertThat(UniqueKeyLookup.Input.class.getDeclaredConstructors()).allSatisfy(constructor -> assertTrue(Modifier.isPrivate(constructor.getModifiers())));
   }



   /*******************************************************************************
    ** A table without a PK can still reject an INSERT using conflict existence.
    *******************************************************************************/
   @Test
   void testPrimaryKeylessInsertExistenceAndNullDistinctness() throws QException
   {
      table.setPrimaryKeyField(null);
      QRecord duplicate = candidate();
      new InsertAction().performValidations(new InsertInput(table.getName()).withRecord(duplicate), false, false);
      assertThat(duplicate.getErrorsAsString()).contains("Another record");
      assertThat(captured.newQueryInput().getFieldNamesToInclude()).containsExactlyInAnyOrder("amount", "binary");
      QRecord nullValue = candidate().withValue("binary", null);
      new InsertAction().performValidations(new InsertInput(table.getName()).withRecord(nullValue), false, false);
      assertThat(nullValue.getErrors()).isNullOrEmpty();
      QRecord skipped = candidate();
      new InsertAction().performValidations(new InsertInput(table.getName()).withRecord(skipped).withSkipUniqueKeyCheck(true), false, false);
      assertThat(skipped.getErrors()).isNullOrEmpty();
   }



   /*******************************************************************************
    ** Non-schema fields and unsupported backends cannot act as an empty result.
    *******************************************************************************/
   @Test
   void testConstrainedRequestsAndUnsupportedCapability() throws QException
   {
      assertThrows(QException.class, () -> UniqueKeyLookup.findConflicts(table, new UniqueKey("private"), new QRecord().withValue("private", "value"), null));
      assertThat(UniqueKeyLookup.readStoredComponents(table, List.of(), null)).isEmpty();
      assertEquals(null, captured);
      assertThat(UniqueKeyLookup.readStoredComponents(table, List.of("1"), null)).hasSize(1);
      QueryInterface unsupported = input -> new QueryOutput(input);
      assertThrows(QException.class, () -> unsupported.lookupUniqueKey(captured));
      assertThrows(QException.class, () -> UniqueKeyLookup.readStoredComponents(table, java.util.Collections.singletonList(null), null));
      QRecord invalid = candidate().withValue("amount", "not numeric");
      new InsertAction().performValidations(new InsertInput(table.getName()).withRecord(invalid), false, false);
      assertThat(invalid.getErrorsAsString()).contains("Invalid value");
   }



   /*******************************************************************************
    ** Partial native rows and native errors are not successful integrity evidence.
    *******************************************************************************/
   @Test
   void testRejectsFailedAndIncompleteNativeRecords()
   {
      returnError = true;
      assertThrows(QException.class, () -> UniqueKeyLookup.findConflicts(table, key, candidate(), null));
      returnError = false;
      omitProjection = true;
      assertThrows(QException.class, () -> UniqueKeyLookup.findConflicts(table, key, candidate(), null));
   }



   /*******************************************************************************
    ** A complete key needs no hydration on a backend without ordinary prefetch.
    ** Sparse input still requires native stored components and must fail closed.
    *******************************************************************************/
   @Test
   void testCompleteKeyWithoutStoredLookup() throws QException
   {
      refuseStoredLookup = true;
      QRecord full = candidate().withValue("id", "1").withValue("amount", new BigDecimal("3.00"));
      assertThat(new UpdateAction().execute(new UpdateInput(table.getName()).withRecord(full)).getRecords().get(0).getErrors()).isNullOrEmpty();
      assertThat(UniqueKeyLookup.findConflicts(table, key, full, null)).hasSize(1);
      assertThat(UniqueKeyLookup.findConflicts(table, key, candidate(), null)).isEmpty();
      assertThrows(QException.class, () -> new UpdateAction().execute(new UpdateInput(table.getName()).withRecord(
         new QRecord().withValue("id", 1).withValue("amount", new BigDecimal("4.00")))));
      assertThat(UniqueKeyLookup.findConflicts(table, key, full, null)).hasSize(1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord candidate()
   {
      return new QRecord().withValue("amount", new BigDecimal("2.00")).withValue("binary", new byte[] { 1, 2 });
   }



   /*******************************************************************************
    ** Capture at the native boundary while still using actual Memory storage.
    *******************************************************************************/
   public static class CapturingModule extends MemoryBackendModule
   {
      @Override
      public String getBackendType()
      {
         return "uniqueProbe";
      }



      @Override
      public UpdateInterface getUpdateInterface()
      {
         return new MemoryUpdateAction()
         {
            @Override
            public boolean supportsPreFetchQuery()
            {
               return false;
            }
         };
      }



      @Override
      public QueryInterface getQueryInterface()
      {
         return new MemoryQueryAction()
         {
            @Override
            public List<QRecord> lookupUniqueKey(UniqueKeyLookup.Input input) throws QException
            {
               captured = input;
               if(refuseStoredLookup && input.newQueryInput().getFilter().getCriteria().get(0).getOperator() == QCriteriaOperator.IN)
               {
                  throw new QException("This configured provider supports only conflict existence");
               }
               List<QRecord> records = super.lookupUniqueKey(input);
               if(returnError)
               {
                  records.get(0).addError(new com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage("failed native row"));
               }
               if(omitProjection)
               {
                  records.get(0).getValues().remove("binary");
               }
               return records;
            }
         };
      }
   }
}
