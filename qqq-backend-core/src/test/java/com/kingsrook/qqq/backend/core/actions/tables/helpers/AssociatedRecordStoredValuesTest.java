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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryQueryAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Exact stored parent projections stay private and reject incomplete providers.
 *******************************************************************************/
class AssociatedRecordStoredValuesTest extends BaseTest
{
   private QTableMetaData parent;
   private Association association;
   private static AssociatedRecordDiscovery.StoredValuesInput captured;
   private static boolean overrideResults;
   private static List<QRecord> providerResults;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void seed() throws QException
   {
      clearCapture();
      QBackendModuleDispatcher.registerBackendModule(new CapturingModule());
      QContext.getQInstance().addBackend(new QBackendMetaData().withName("associationValuesProbe").withBackendType(CapturingModule.class));
      association = new Association().withName("selected children").withAssociatedTableName("storedChild").withJoinName("storedJoin");
      parent = new QTableMetaData().withName("storedParent").withBackendName("associationValuesProbe").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsHidden(true).withIsHeavy(true))
         .withField(new QFieldMetaData("code", QFieldType.STRING).withIsHidden(true).withIsHeavy(true).withBackendName("stored_code"))
         .withField(new QFieldMetaData("binary", QFieldType.BLOB).withIsHidden(true).withIsHeavy(true))
         .withField(new QFieldMetaData("tenant", QFieldType.INTEGER))
         .withField(new QFieldMetaData("private", QFieldType.STRING)).withAssociation(association);
      QTableMetaData child = new QTableMetaData().withName("storedChild").withBackendName("associationValuesProbe").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("parentCode", QFieldType.STRING))
         .withField(new QFieldMetaData("parentBinary", QFieldType.BLOB));
      QContext.getQInstance().addTable(parent);
      QContext.getQInstance().addTable(child);
      QContext.getQInstance().addJoin(new QJoinMetaData().withName("storedJoin").withLeftTable(parent.getName()).withRightTable(child.getName())
         .withJoinOn(new JoinOn("code", "parentCode")).withJoinOn(new JoinOn("binary", "parentBinary")));
      new InsertAction().execute(new InsertInput(parent.getName()).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("code", "ALPHA").withValue("binary", new byte[] { 1, 2 }).withValue("tenant", 99).withValue("private", "never projected"),
         new QRecord().withValue("id", 2).withValue("code", null).withValue("binary", null).withValue("tenant", 99))));
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("storedTenant"));
      parent.withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("storedTenant").withFieldName("tenant"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void clearCapture()
   {
      captured = null;
      overrideResults = false;
      providerResults = null;
   }



   /*******************************************************************************
    ** Removed structural fields are restored only on private metadata, while the
    ** supplied association graph, active field mapping and row locks stay intact.
    *******************************************************************************/
   @Test
   void testPrivatePhysicalMetadataAndTypedProjection() throws QException
   {
      QTableMetaData active = parent.clone();
      active.getFields().remove("id");
      active.getFields().remove("code");
      active.getFields().remove("binary");
      active.getField("tenant").setBackendName("active_tenant");
      parent.setAssociations(List.of());
      Object session = QContext.getQSession();
      Object locks = parent.getRecordSecurityLocks();
      assertThat(new QueryAction().execute(new QueryInput(parent.getName())).getRecords()).isEmpty();
      QBackendTransaction transaction = new QBackendTransaction();
      List<QRecord> records = AssociatedRecordDiscovery.readParentValues(active, List.of(association), List.of("1", 1, "2", 999), transaction);
      assertThat(records).hasSize(2);
      QRecord first = records.stream().filter(record -> record.getValue("id").equals(1)).findFirst().orElseThrow();
      QRecord second = records.stream().filter(record -> record.getValue("id").equals(2)).findFirst().orElseThrow();
      assertEquals(Set.of("id", "code", "binary"), first.getValues().keySet());
      assertEquals(Set.of("id", "code", "binary"), second.getValues().keySet());
      assertEquals("ALPHA", first.getValue("code"));
      assertNull(second.getValue("code"));
      assertNull(second.getValue("binary"));
      QueryInput query = captured.newQueryInput();
      assertSame(transaction, query.getTransaction());
      assertEquals(QCriteriaOperator.IN, query.getFilter().getCriteria().get(0).getOperator());
      assertThat(query.getFilter().getCriteria().get(0).getValues()).containsExactly(1, 2, 999);
      assertTrue(query.getShouldFetchHeavyFields());
      assertFalse(query.getShouldOmitHiddenFields());
      assertFalse(query.getShouldMaskPasswords());
      assertEquals("stored_code", query.getTable().getField("code").getBackendName());
      assertEquals("active_tenant", query.getTable().getField("tenant").getBackendName());
      assertThat(query.getTable().getAssociations()).hasSize(1);
      assertNotSame(parent.getField("code"), query.getTable().getField("code"));
      query.getTable().getField("code").setBackendName("changed");
      query.getFieldNamesToInclude().clear();
      query.getFilter().getCriteria().clear();
      assertEquals("stored_code", captured.newQueryInput().getTable().getField("code").getBackendName());
      assertEquals(Set.of("id", "code", "binary"), captured.newQueryInput().getFieldNamesToInclude());
      assertEquals(1, captured.newQueryInput().getFilter().getCriteria().size());
      assertFalse(active.getFields().containsKey("id"));
      assertFalse(active.getFields().containsKey("code"));
      assertFalse(active.getFields().containsKey("binary"));
      assertThat(parent.getAssociations()).isEmpty();
      ((byte[]) first.getValue("binary"))[0] = 9;
      assertEquals(1, ((byte[]) AssociatedRecordDiscovery.readParentValues(active, List.of(association), List.of(1), null).get(0).getValue("binary"))[0]);
      assertThat(new QueryAction().execute(new QueryInput(parent.getName())).getRecords()).isEmpty();
      assertSame(session, QContext.getQSession());
      assertSame(locks, parent.getRecordSecurityLocks());
   }



   /*******************************************************************************
    ** Reversed pairs select parent fields, and binary keys are copied at both
    ** caller and adapter boundaries instead of depending on array identity.
    *******************************************************************************/
   @Test
   void testReverseRelationshipAndBinaryKeySnapshots() throws QException
   {
      QJoinMetaData join = QContext.getQInstance().getJoin(association.getJoinName());
      join.setLeftTable("storedChild");
      join.setRightTable(parent.getName());
      join.setJoinOns(List.of(new JoinOn("parentCode", "code"), new JoinOn("parentBinary", "binary")));
      parent.setPrimaryKeyField("binary");
      byte[] key = new byte[] { 1, 2 };
      List<Serializable> keys = new ArrayList<>();
      keys.add(key);
      QRecord record = AssociatedRecordDiscovery.readParentValues(parent, List.of(association), keys, null).get(0);
      assertEquals(Set.of("binary", "code"), record.getValues().keySet());
      key[0] = 9;
      QueryInput query = captured.newQueryInput();
      ((byte[]) query.getFilter().getCriteria().get(0).getValues().get(0))[0] = 8;
      assertTrue(captured.matches(new QRecord().withValue("binary", new byte[] { 1, 2 })));
      assertFalse(captured.matches(new QRecord().withValue("binary", new byte[] { 8, 2 })));
      assertEquals(1, ((byte[]) captured.newQueryInput().getFilter().getCriteria().get(0).getValues().get(0))[0]);
      assertEquals("storedChild", join.getLeftTable());
      assertEquals("parentCode", join.getJoinOns().get(0).getLeftField());
      assertThat(AssociatedRecordDiscovery.StoredValuesInput.class.getDeclaredConstructors())
         .allSatisfy(constructor -> assertTrue(Modifier.isPrivate(constructor.getModifiers())));
   }



   /*******************************************************************************
    ** Empty key requests never dispatch, but invalid graphs and keys fail closed.
    *******************************************************************************/
   @Test
   void testConstrainedRequestsAndUnsupportedProvider() throws QException
   {
      assertThat(AssociatedRecordDiscovery.readParentValues(parent, List.of(association), List.of(), null)).isEmpty();
      assertNull(captured);
      Association invented = new Association().withName("not declared").withAssociatedTableName("storedChild").withJoinName("storedJoin");
      assertThrows(QException.class, () -> AssociatedRecordDiscovery.readParentValues(parent, List.of(invented), List.of(), null));
      QTableMetaData removed = parent.clone().withAssociations(List.of());
      assertThrows(QException.class, () -> AssociatedRecordDiscovery.readParentValues(removed, List.of(association), List.of(1), null));
      assertThrows(QException.class, () -> AssociatedRecordDiscovery.physicalParentTable(parent.clone().withBackendName("another")));
      assertThrows(QException.class, () -> AssociatedRecordDiscovery.physicalParentTable(parent.clone().withPrimaryKeyField("code")));
      assertThrows(QException.class, () -> AssociatedRecordDiscovery.readParentValues(parent, List.of(association), Collections.singletonList(null), null));
      assertThrows(QException.class, () -> AssociatedRecordDiscovery.readParentValues(parent, List.of(association), List.of("invalid integer"), null));
      assertThat(AssociatedRecordDiscovery.readParentValues(parent, List.of(association), List.of(1), null)).hasSize(1);
      QueryInterface unsupported = input -> new QueryOutput(input);
      assertEquals("Backend does not implement stored association-value lookup", assertThrows(QException.class, () -> unsupported.readAssociationValues(captured)).getMessage());
   }



   /*******************************************************************************
    ** Provider failures, omitted columns, invalid values and unrequested/duplicate
    ** owners cannot become successful structural evidence.
    *******************************************************************************/
   @Test
   void testRejectsIncompleteOrUnexpectedProviderResults() throws QException
   {
      QRecord valid = AssociatedRecordDiscovery.readParentValues(parent, List.of(association), List.of(1), null).get(0);
      QRecord incomplete = new QRecord(valid);
      incomplete.getValues().remove("code");
      QRecord failed = new QRecord(valid);
      failed.addError(new BadInputStatusMessage("failed native row"));
      QRecord unexpected = new QRecord(valid).withValue("id", 99);
      QRecord invalid = new QRecord(valid).withValue("id", "bad integer");
      QRecord nullKey = new QRecord(valid).withValue("id", null);
      overrideResults = true;
      for(List<QRecord> records : List.of(List.of(incomplete), List.of(failed), List.of(unexpected), List.of(invalid), List.of(nullKey), List.of(valid, new QRecord(valid).withValue("id", "1")), Collections.<QRecord>singletonList(null)))
      {
         providerResults = records;
         assertThrows(QException.class, () -> AssociatedRecordDiscovery.readParentValues(parent, List.of(association), List.of(1), null));
      }
      providerResults = null;
      assertThrows(QException.class, () -> AssociatedRecordDiscovery.readParentValues(parent, List.of(association), List.of(1), null));
      overrideResults = false;
      assertEquals("ALPHA", AssociatedRecordDiscovery.readParentValues(parent, List.of(association), List.of(1), null).get(0).getValue("code"));
   }



   /*******************************************************************************
    ** Real Memory storage with capture and deliberately malformed provider outputs.
    *******************************************************************************/
   public static class CapturingModule extends MemoryBackendModule
   {
      @Override
      public String getBackendType()
      {
         return "associationValuesProbe";
      }



      @Override
      public QueryInterface getQueryInterface()
      {
         return new MemoryQueryAction()
         {
            @Override
            public List<QRecord> readAssociationValues(AssociatedRecordDiscovery.StoredValuesInput input) throws QException
            {
               captured = input;
               return overrideResults ? providerResults : super.readAssociationValues(input);
            }
         };
      }
   }
}
