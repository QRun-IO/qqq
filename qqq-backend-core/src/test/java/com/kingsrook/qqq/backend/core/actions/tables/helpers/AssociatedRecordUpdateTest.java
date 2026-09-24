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


import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryQueryAction;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Preparation rejects ambiguous targets and keeps stored relationship values private.
 *******************************************************************************/
class AssociatedRecordUpdateTest extends BaseTest
{
   private QTableMetaData parent;
   private static Integer reads;
   private static boolean refuseReads;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void seed() throws QException
   {
      resetProbe();
      QBackendModuleDispatcher.registerBackendModule(new PreparationModule());
      QContext.getQInstance().addBackend(new QBackendMetaData().withName("associationPreparation").withBackendType(PreparationModule.class));
      parent = new QTableMetaData().withName("preparationParent").withBackendName("associationPreparation").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("code", QFieldType.STRING).withIsHidden(true).withIsHeavy(true))
         .withField(new QFieldMetaData("region", QFieldType.INTEGER))
         .withField(new QFieldMetaData("payload", QFieldType.STRING))
         .withAssociation(new Association().withName("named children").withAssociatedTableName("preparationChild").withJoinName("preparationJoin"));
      QTableMetaData child = new QTableMetaData().withName("preparationChild").withBackendName("associationPreparation").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("parentCode", QFieldType.STRING))
         .withField(new QFieldMetaData("parentRegion", QFieldType.INTEGER));
      QContext.getQInstance().addTable(parent);
      QContext.getQInstance().addTable(child);
      QContext.getQInstance().addJoin(new QJoinMetaData().withName("preparationJoin").withLeftTable(parent.getName()).withRightTable(child.getName())
         .withJoinOn(new JoinOn("code", "parentCode")).withJoinOn(new JoinOn("region", "parentRegion")));
      new InsertAction().execute(new InsertInput(parent.getName()).withRecord(
         new QRecord().withValue("id", 1).withValue("code", "ALPHA").withValue("region", 7).withValue("payload", "stored")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void resetProbe()
   {
      reads = 0;
      refuseReads = true;
   }



   /*******************************************************************************
    ** Ordinary parent patches impose no native lookup requirement or new validation.
    *******************************************************************************/
   @Test
   void testPlainParentUpdateDoesNotReadOrChangeRecords() throws QException
   {
      List<QRecord> records = List.of(new QRecord().withValue("id", 1).withValue("payload", "new"),
         new QRecord().withValue("payload", "without id"),
         new QRecord().withValue("id", 2).withAssociatedRecords("not a declared association", List.of()));
      String before = JsonUtils.toJson(records);
      assertThat(AssociatedRecordUpdate.prepare(parent, records, null)).isEmpty();
      assertEquals(0, reads);
      assertEquals(before, JsonUtils.toJson(records));
   }



   /*******************************************************************************
    ** Typed duplicate targets are ambiguous even when only one carries replacement
    ** intent. Both fragments fail before native discovery; other targets stay intact.
    *******************************************************************************/
   @Test
   void testDuplicateTypedParentsFailBeforeLookup() throws QException
   {
      QRecord replacement = new QRecord().withValue("id", "1").withValue("payload", "first").withAssociatedRecords("named children", List.of());
      QRecord fragment = new QRecord().withValue("id", 1).withValue("payload", "second");
      QRecord unrelated = new QRecord().withValue("id", 2).withValue("payload", "other");
      assertThat(AssociatedRecordUpdate.prepare(parent, List.of(replacement, fragment, unrelated), null)).isEmpty();
      for(QRecord record : List.of(replacement, fragment))
      {
         assertThat(record.getErrors()).hasSize(1).allSatisfy(error -> assertThat(error).isInstanceOf(BadInputStatusMessage.class));
         assertThat(record.getErrorsAsString()).contains("only once");
      }
      assertEquals(Map.of("id", "1", "payload", "first"), replacement.getValues());
      assertEquals(Map.of("id", 1, "payload", "second"), fragment.getValues());
      assertThat(unrelated.getErrors()).isNullOrEmpty();
      assertEquals(0, reads);
   }



   /*******************************************************************************
    ** Missing parent keys are record-level bad input, not a provider error.
    *******************************************************************************/
   @Test
   void testMissingPrimaryKeyFailsBeforeLookup() throws QException
   {
      QRecord record = new QRecord().withValue("payload", "new").withAssociatedRecords("named children", List.of());
      assertThat(AssociatedRecordUpdate.prepare(parent, List.of(record), null)).isEmpty();
      assertThat(record.getErrors()).hasSize(1).allSatisfy(error -> assertThat(error).isInstanceOf(BadInputStatusMessage.class));
      assertThat(record.getErrorsAsString()).contains("Missing primary key");
      assertEquals(Map.of("payload", "new"), record.getValues());
      assertEquals(0, reads);
   }



   /*******************************************************************************
    ** A removed field is restored only for structural lookup. A forged value in
    ** that field cannot replace the stored old or prospective relationship tuple.
    *******************************************************************************/
   @Test
   void testRemovedFieldCannotOverridePrivateStoredTuple() throws QException
   {
      refuseReads = false;
      QTableMetaData active = parent.clone();
      active.getFields().remove("code");
      QRecord patch = new QRecord().withValue("id", 1).withValue("code", "FORGED").withValue("region", 0)
         .withValue("payload", "new").withAssociatedRecords("named children", List.of());
      String before = JsonUtils.toJson(patch);
      AssociatedRecordUpdate.Values values = AssociatedRecordUpdate.prepare(active, List.of(patch), null).get(patch);
      assertEquals(Map.of("id", 1, "code", "ALPHA", "region", 7), values.before().getValues());
      assertEquals(Map.of("id", 1, "code", "ALPHA", "region", 0), values.after().getValues());
      assertEquals(Set.of("code"), values.storedFields());
      assertEquals(before, JsonUtils.toJson(patch));
      assertFalse(active.getFields().containsKey("code"));
      assertEquals(QFieldType.STRING, parent.getField("code").getType());
      assertEquals(1, reads);

      QRecord allowed = new QRecord().withValue("id", 1).withValue("code", "NEW").withValue("region", 0)
         .withAssociatedRecords("named children", List.of());
      AssociatedRecordUpdate.Values allowedValues = AssociatedRecordUpdate.prepare(parent, List.of(allowed), null).get(allowed);
      assertEquals(Map.of("id", 1, "code", "ALPHA", "region", 7), allowedValues.before().getValues());
      assertEquals(Map.of("id", 1, "code", "NEW", "region", 0), allowedValues.after().getValues());
      assertThat(allowedValues.storedFields()).isEmpty();
      assertEquals(2, reads);
   }



   /*******************************************************************************
    ** Native reads are either counted or deliberately unavailable in these controls.
    *******************************************************************************/
   public static class PreparationModule extends MemoryBackendModule
   {
      @Override
      public String getBackendType()
      {
         return "associationPreparation";
      }



      @Override
      public QueryInterface getQueryInterface()
      {
         return new MemoryQueryAction()
         {
            @Override
            public List<QRecord> readAssociationValues(AssociatedRecordDiscovery.StoredValuesInput input) throws QException
            {
               reads++;
               if(refuseReads)
               {
                  throw new QException("Stored lookup is unavailable in this control");
               }
               return super.readAssociationValues(input);
            }
         };
      }
   }
}
