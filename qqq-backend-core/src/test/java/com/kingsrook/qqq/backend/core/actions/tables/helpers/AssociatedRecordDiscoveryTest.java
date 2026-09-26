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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CaseChangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.WhiteSpaceBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Structural discovery is constrained by metadata, not current row visibility.
 *******************************************************************************/
class AssociatedRecordDiscoveryTest extends BaseTest
{
   private QTableMetaData parent;
   private QTableMetaData child;
   private Association association;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void seed() throws QException
   {
      association = new Association().withName("arbitraryChildren").withAssociatedTableName("discoveryChild").withJoinName("discoveryJoin");
      parent = new QTableMetaData().withName("discoveryParent").withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("link", QFieldType.STRING)).withAssociation(association);
      child = new QTableMetaData().withName("discoveryChild").withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("parentLink", QFieldType.STRING).withBehavior(CaseChangeBehavior.TO_UPPER_CASE).withBehavior(WhiteSpaceBehavior.TRIM))
         .withField(new QFieldMetaData("tenant", QFieldType.INTEGER));
      QContext.getQInstance().addTable(parent);
      QContext.getQInstance().addTable(child);
      QContext.getQInstance().addJoin(new QJoinMetaData().withName("discoveryJoin").withLeftTable(parent.getName()).withRightTable(child.getName())
         .withJoinOn(new JoinOn("link", "parentLink")));
      new InsertAction().execute(new InsertInput(child.getName()).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("parentLink", "ALPHA").withValue("tenant", 99),
         new QRecord().withValue("id", 2).withValue("parentLink", "ALPHA").withValue("tenant", 99),
         new QRecord().withValue("id", 3).withValue("parentLink", "BETA").withValue("tenant", 99),
         new QRecord().withValue("id", 4).withValue("parentLink", null).withValue("tenant", 99))));
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("discoveryTenant"));
      child.withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("discoveryTenant").withFieldName("tenant"));
   }



   /*******************************************************************************
    ** Hidden candidates still need authorization; retained IDs and other parents do not.
    *******************************************************************************/
   @Test
   void testHiddenRowsAndNormalizedRelationshipValues() throws QException
   {
      assertThat(new QueryAction().execute(new QueryInput(child.getName())).getRecords()).isEmpty();
      Map<String, Serializable> values = new HashMap<>();
      values.put("link", " alpha ");
      AssociatedRecordDiscovery.Parent snapshot = new AssociatedRecordDiscovery.Parent(values, List.of("2"));
      values.put("link", "BETA");
      assertThat(AssociatedRecordDiscovery.findPrimaryKeys(parent, association, List.of(snapshot), null)).containsExactly(1);
      assertEquals(" alpha ", snapshot.values().get("link"));
      assertThat(new QueryAction().execute(new QueryInput(child.getName())).getRecords()).isEmpty();
      assertEquals(1, child.getRecordSecurityLocks().size());
   }



   /*******************************************************************************
    ** A null relation is a criterion, while no parent must never query every row.
    *******************************************************************************/
   @Test
   void testNullRelationshipAndEmptyParents() throws QException
   {
      Map<String, Serializable> values = new HashMap<>();
      values.put("link", null);
      assertThat(AssociatedRecordDiscovery.findPrimaryKeys(parent, association, List.of(new AssociatedRecordDiscovery.Parent(values, List.of())), null)).isEmpty();
      assertThat(AssociatedRecordDiscovery.findPrimaryKeys(parent, association, List.of(), null)).isEmpty();
   }



   /*******************************************************************************
    ** Association names and joins cannot be replaced by a generic arbitrary filter.
    *******************************************************************************/
   @Test
   void testRejectsUndeclaredAssociation() throws QException
   {
      Association invented = new Association().withName("other").withAssociatedTableName(child.getName()).withJoinName("discoveryJoin");
      assertThrows(QException.class, () -> AssociatedRecordDiscovery.findPrimaryKeys(parent, invented, List.of(), null));
      Association changed = new Association().withName(association.getName()).withAssociatedTableName(child.getName()).withJoinName("otherJoin");
      assertThrows(QException.class, () -> AssociatedRecordDiscovery.findPrimaryKeys(parent, changed, List.of(), null));
   }


   /*******************************************************************************
    ** Exclusions belong to each parent relationship, not the whole input page.
    *******************************************************************************/
   @Test
   void testMultipleParentsKeepIndependentRetainedSets() throws QException
   {
      assertThat(AssociatedRecordDiscovery.findPrimaryKeys(parent, association, List.of(
         new AssociatedRecordDiscovery.Parent(Map.of("link", "ALPHA"), List.of(2)),
         new AssociatedRecordDiscovery.Parent(Map.of("link", "BETA"), List.of(1))), null)).containsExactlyInAnyOrder(1, 3);
   }



   /*******************************************************************************
    ** Every declared join field constrains membership; malformed endpoints fail closed.
    *******************************************************************************/
   @Test
   void testCompositeRelationshipAndMalformedReverseJoinRejection() throws QException
   {
      QJoinMetaData join = QContext.getQInstance().getJoin(association.getJoinName());
      join.withJoinOn(new JoinOn("id", "tenant"));
      assertThat(AssociatedRecordDiscovery.findPrimaryKeys(parent, association,
         List.of(new AssociatedRecordDiscovery.Parent(Map.of("id", 99, "link", "ALPHA"), List.of())), null)).containsExactlyInAnyOrder(1, 2);
      assertThat(AssociatedRecordDiscovery.findPrimaryKeys(parent, association,
         List.of(new AssociatedRecordDiscovery.Parent(Map.of("id", 88, "link", "ALPHA"), List.of())), null)).isEmpty();
      join.setLeftTable(child.getName());
      join.setRightTable(parent.getName());
      assertThrows(QException.class, () -> AssociatedRecordDiscovery.findPrimaryKeys(parent, association,
         List.of(new AssociatedRecordDiscovery.Parent(Map.of("id", 99, "link", "ALPHA"), List.of())), null));
   }



   /*******************************************************************************
    ** A valid reversed declaration discovers the same hidden children.
    *******************************************************************************/
   @Test
   void testReverseRelationshipPreservesRegisteredMetadata() throws Exception
   {
      QJoinMetaData join = QContext.getQInstance().getJoin(association.getJoinName());
      join.setLeftTable(child.getName());
      join.setRightTable(parent.getName());
      join.setJoinOns(List.of(new JoinOn("parentLink", "link")));
      assertThat(AssociatedRecordDiscovery.findPrimaryKeys(parent, association,
         List.of(new AssociatedRecordDiscovery.Parent(Map.of("link", " alpha "), List.of(2))), null)).containsExactly(1);
      assertEquals(child.getName(), join.getLeftTable());
      assertEquals("parentLink", join.getJoinOns().get(0).getLeftField());
      assertThat(new QueryAction().execute(new QueryInput(child.getName())).getRecords()).isEmpty();
   }



   /*******************************************************************************
    ** The supplied personalized association graph, rather than the canonical graph,
    ** defines an Update's structural relationship.
    *******************************************************************************/
   @Test
   void testUsesSuppliedPersonalizedParentGraph() throws Exception
   {
      QTableMetaData personalized = parent.clone();
      parent.setAssociations(List.of());
      assertThat(AssociatedRecordDiscovery.findPrimaryKeys(personalized, association,
         List.of(new AssociatedRecordDiscovery.Parent(Map.of("link", "BETA"), List.of())), null)).containsExactly(3);
      assertThrows(QException.class, () -> AssociatedRecordDiscovery.findPrimaryKeys(parent, association,
         List.of(new AssociatedRecordDiscovery.Parent(Map.of("link", "BETA"), List.of())), null));
   }

}
