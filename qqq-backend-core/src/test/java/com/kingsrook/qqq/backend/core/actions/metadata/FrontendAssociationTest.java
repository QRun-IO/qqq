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

package com.kingsrook.qqq.backend.core.actions.metadata;


import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Full frontend metadata preserves declared association identity and joins.
 *******************************************************************************/
class FrontendAssociationTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFullMetadataPreservesNamedGroupsAndCompositeJoin() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER);
      QJoinMetaData join = QContext.getQInstance().getJoin("orderLineItem");
      join.getJoinOns().add(new JoinOn("orderNo", "sku"));
      table.setExposedJoins(List.of());
      table.setAssociations(List.of(
         new Association().withName("fulfillment-lines").withAssociatedTableName(TestUtils.TABLE_NAME_LINE_ITEM).withJoinName("orderLineItem"),
         new Association().withName("auditLines").withAssociatedTableName(TestUtils.TABLE_NAME_LINE_ITEM).withJoinName("orderLineItem")));
      String canonicalJoinBefore = JsonUtils.toJson(join);
      String canonicalAssociationsBefore = JsonUtils.toJson(table.getAssociations());

      TableMetaDataInput input = new TableMetaDataInput();
      input.setTableName(TestUtils.TABLE_NAME_ORDER);
      QFrontendTableMetaData full = new TableMetaDataAction().execute(input).getTable();
      JsonNode json = new ObjectMapper().readTree(JsonUtils.toJson(full));
      assertTrue(json.has("associations"), "Full metadata must identify exact association groups, including groups without exposed joins");
      assertEquals(2, json.get("associations").size());
      assertEquals("fulfillment-lines", json.at("/associations/0/name").asText());
      assertEquals("auditLines", json.at("/associations/1/name").asText());
      for(JsonNode association : json.get("associations"))
      {
         assertEquals(3, association.size());
         assertEquals("orderLine", association.get("associatedTableName").asText());
         assertEquals("orderLineItem", association.at("/join/name").asText());
         assertEquals("order", association.at("/join/leftTable").asText());
         assertEquals("orderLine", association.at("/join/rightTable").asText());
         assertEquals(2, association.at("/join/joinOns").size());
         assertEquals("id", association.at("/join/joinOns/0/leftField").asText());
         assertEquals("orderId", association.at("/join/joinOns/0/rightField").asText());
         assertEquals("orderNo", association.at("/join/joinOns/1/leftField").asText());
         assertEquals("sku", association.at("/join/joinOns/1/rightField").asText());
      }
      assertEquals(canonicalJoinBefore, JsonUtils.toJson(join));
      assertEquals(canonicalAssociationsBefore, JsonUtils.toJson(table.getAssociations()));
      full.getAssociations().get(0).getJoin().getJoinOns().get(0).setLeftField("changedFrontendField");
      assertEquals("id", full.getAssociations().get(1).getJoin().getJoinOns().get(0).getLeftField());
      assertEquals(canonicalJoinBefore, JsonUtils.toJson(join));

      QFrontendTableMetaData light = new QFrontendTableMetaData(new TableMetaDataInput(), QContext.getQInstance().getBackendForTable(table.getName()), table, false, true);
      assertFalse(new ObjectMapper().readTree(JsonUtils.toJson(light)).has("associations"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPersonalizedAssociationsPreserveIndependentInsertPermission() throws Exception
   {
      QTableMetaData parent = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER);
      parent.setExposedJoins(List.of());
      QTableMetaData child = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_LINE_ITEM);
      child.setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS).withDenyBehavior(DenyBehavior.DISABLED));
      QContext.getQSession().withPermission("orderLine.insert");
      QContext.getQInstance().addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(SelectAssociationPersonalizer.class));
      String canonicalBefore = JsonUtils.toJson(parent.getAssociations());

      TableMetaDataInput input = new TableMetaDataInput();
      input.setTableName(TestUtils.TABLE_NAME_ORDER);
      input.setInputSource(QInputSource.USER);
      QFrontendTableMetaData user = new TableMetaDataAction().execute(input).getTable();
      assertEquals(List.of("orderLine"), user.getAssociations().stream().map(association -> association.getName()).toList());
      assertEquals("orderLine", user.getAssociations().get(0).getAssociatedTableName());
      TableMetaDataInput childInput = new TableMetaDataInput();
      childInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM);
      childInput.setInputSource(QInputSource.USER);
      QFrontendTableMetaData childFrontend = new TableMetaDataAction().execute(childInput).getTable();
      assertFalse(childFrontend.getReadPermission());
      assertTrue(childFrontend.getInsertPermission());
      assertTrue(user.getReadPermission());

      input.setInputSource(QInputSource.SYSTEM);
      assertEquals(List.of("orderLine", "extrinsics"), new TableMetaDataAction().execute(input).getTable().getAssociations().stream().map(association -> association.getName()).toList());
      assertEquals(canonicalBefore, JsonUtils.toJson(parent.getAssociations()));

      child.setAssociations(List.of());
      JsonNode childJson = new ObjectMapper().readTree(JsonUtils.toJson(new TableMetaDataAction().execute(childInput).getTable()));
      assertTrue(childJson.get("associations").isArray());
      assertEquals(0, childJson.get("associations").size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class SelectAssociationPersonalizer implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(TestUtils.TABLE_NAME_ORDER.equals(input.getTableName()) && QInputSource.USER.equals(input.getInputSource()))
         {
            QTableMetaData clone = input.getTable().clone();
            clone.setAssociations(clone.getAssociations().stream().filter(association -> "orderLine".equals(association.getName())).toList());
            return clone;
         }
         return input.getTable();
      }
   }
}
