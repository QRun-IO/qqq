/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableAudienceType;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.AutomationStatusTracking;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.QTableAutomationDetails;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TableAutomationAction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheOf;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheUseCase;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QTableMetaData
 *******************************************************************************/
class QTableMetaDataTest extends BaseTest
{

   /*******************************************************************************
    ** Backend exclusions must disable operations when supplied as a set.
    *******************************************************************************/
   @Test
   void testBackendWithoutCapabilitiesSet()
   {
      QBackendMetaData backend = new QBackendMetaData()
         .withCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE)
         .withoutCapabilities(Set.of(Capability.TABLE_INSERT, Capability.TABLE_DELETE));
      QTableMetaData table = new QTableMetaData();

      assertFalse(table.isCapabilityEnabled(backend, Capability.TABLE_INSERT));
      assertFalse(table.isCapabilityEnabled(backend, Capability.TABLE_DELETE));
      assertFalse(table.isCapabilityEnabled(backend, Capability.QUERY_STATS));
      assertTrue(table.isCapabilityEnabled(backend, Capability.TABLE_UPDATE));
      assertFalse(backend.getEnabledCapabilities().contains(Capability.TABLE_INSERT));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIsCapabilityEnabled()
   {
      Capability capability = Capability.TABLE_GET;

      // table:null & backend:null = true
      assertTrue(new QTableMetaData().isCapabilityEnabled(new QBackendMetaData(), capability));
      assertTrue(new QTableMetaData().isCapabilityEnabled(null, capability));

      // table:null & backend:true = true
      assertTrue(new QTableMetaData().isCapabilityEnabled(new QBackendMetaData().withCapability(capability), capability));

      // table:null & backend:false = false
      assertFalse(new QTableMetaData().isCapabilityEnabled(new QBackendMetaData().withoutCapability(capability), capability));

      // table:true & backend:null = true
      assertTrue(new QTableMetaData().withCapability(capability).isCapabilityEnabled(new QBackendMetaData(), capability));
      assertTrue(new QTableMetaData().withCapability(capability).isCapabilityEnabled(null, capability));

      // table:false & backend:null = false
      assertFalse(new QTableMetaData().withoutCapability(capability).isCapabilityEnabled(new QBackendMetaData(), capability));
      assertFalse(new QTableMetaData().withoutCapability(capability).isCapabilityEnabled(null, capability));

      // table:true & backend:true = true
      assertTrue(new QTableMetaData().withCapability(capability).isCapabilityEnabled(new QBackendMetaData().withCapability(capability), capability));

      // table:true & backend:false = true
      assertTrue(new QTableMetaData().withCapability(capability).isCapabilityEnabled(new QBackendMetaData().withoutCapability(capability), capability));

      // table:false & backend:true = false
      assertFalse(new QTableMetaData().withoutCapability(capability).isCapabilityEnabled(new QBackendMetaData().withCapability(capability), capability));

      // table:false & backend:false = false
      assertFalse(new QTableMetaData().withoutCapability(capability).isCapabilityEnabled(new QBackendMetaData().withoutCapability(capability), capability));

      // backend false, but then true = true
      assertTrue(new QTableMetaData().isCapabilityEnabled(new QBackendMetaData().withoutCapability(capability).withCapability(capability), capability));

      // backend true, but then false = false
      assertFalse(new QTableMetaData().isCapabilityEnabled(new QBackendMetaData().withCapability(capability).withoutCapability(capability), capability));

      // table true, but then false = true
      assertFalse(new QTableMetaData().withCapability(capability).withoutCapability(capability).isCapabilityEnabled(new QBackendMetaData(), capability));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testClone()
   {
      //////////////////////////////////////////////
      // lambda to set several strings in a table //
      //////////////////////////////////////////////
      BiConsumer<QTableMetaData, String> setAllStrings = (table, s) ->
      {
         table.setName(s);
         table.setLabel(s);
         table.setBackendName(s);
         table.setPrimaryKeyField(s);
         table.setIsHidden(true);
         table.setFields(MapBuilder.of(s, new QFieldMetaData(s, QFieldType.INTEGER)));
         table.setAutomationDetails(new QTableAutomationDetails()
            .withActions(ListBuilder.of(new TableAutomationAction().withName(s).withFilter(new QQueryFilter(new QFilterCriteria(s, QCriteriaOperator.IS_BLANK)))))
            .withStatusTracking(new AutomationStatusTracking().withFieldName(s)));
         table.setCustomizers(MapBuilder.of(TableCustomizers.PRE_INSERT_RECORD.getRole(), new QCodeReference(s, QCodeType.JAVA)));
         table.setIcon(new QIcon(s));
         table.setRecordLabelFormat(s);
         table.setRecordLabelFields(ListBuilder.of(s));
         table.setSections(ListBuilder.of(
            new QFieldSection(s, new QIcon(s), Tier.T1, ListBuilder.of(s))));
         table.setAssociatedScripts(ListBuilder.of(
            new AssociatedScript().withFieldName(s)));
         table.setUniqueKeys(ListBuilder.of(new UniqueKey(s)));
         table.setEnabledCapabilities(Set.of(Capability.TABLE_COUNT));
         table.setDisabledCapabilities(Set.of(Capability.TABLE_DELETE));
         table.setCacheOf(new CacheOf().withUseCases(ListBuilder.of(
            new CacheUseCase().withExcludeRecordsMatching(ListBuilder.of(new QQueryFilter(new QFilterCriteria(s, QCriteriaOperator.IS_BLANK)))))));
         table.setRecordSecurityLocks(ListBuilder.of(
            new RecordSecurityLock().withSecurityKeyType(s).withJoinNameChain(ListBuilder.of(s))));
         table.setPermissionRules(new QPermissionRules().withPermissionBaseName(s).withCustomPermissionChecker(new QCodeReference(s, QCodeType.JAVA)));
         table.setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE));
         table.setAssociations(ListBuilder.of(new Association().withName(s)));
         table.setExposedJoins(ListBuilder.of(new ExposedJoin().withJoinTable(s).withJoinPath(ListBuilder.of(s))));
         table.setShareableTableMetaData(new ShareableTableMetaData().withSharedRecordTableName(s).withAudienceTypes(MapBuilder.of(s, new ShareableAudienceType().withName(s))));
         table.setHelpContent(Map.of(s, ListBuilder.of(new QHelpContent().withContent(s))));
         table.setSourceQBitName(s);
         // hmm! table.setBackendDetails();
         // hmm! table.setSupplementalMetaData();
      };

      /////////////////////////////////////////////////////////////////////////////////
      // lambda to assert against the several strings in a table by the lambda above //
      /////////////////////////////////////////////////////////////////////////////////
      BiConsumer<QTableMetaData, String> assertAllStrings = (table, s) ->
      {
         assertEquals(s, table.getName());
         assertEquals(s, table.getLabel());
         assertEquals(s, table.getBackendName());
         assertEquals(s, table.getPrimaryKeyField());
         assertTrue(table.getIsHidden());
         assertEquals(s, table.getFields().get(s).getName());
         assertEquals(s, table.getAutomationDetails().getActions().get(0).getName());
         assertEquals(s, table.getAutomationDetails().getActions().get(0).getFilter().getCriteria().get(0).getFieldName());
         assertEquals(s, table.getAutomationDetails().getStatusTracking().getFieldName());
         assertEquals(s, table.getCustomizers().get(TableCustomizers.PRE_INSERT_RECORD.getRole()).getName());
         assertEquals(s, table.getIcon().getName());
         assertEquals(s, table.getRecordLabelFormat());
         assertEquals(s, table.getRecordLabelFields().get(0));
         assertEquals(s, table.getSections().get(0).getName());
         assertEquals(s, table.getSections().get(0).getIcon().getName());
         assertEquals(s, table.getSections().get(0).getFieldNames().get(0));
         assertEquals(s, table.getAssociatedScripts().get(0).getFieldName());
         assertEquals(s, table.getUniqueKeys().get(0).getFieldNames().get(0));
         assertTrue(table.getEnabledCapabilities().contains(Capability.TABLE_COUNT));
         assertTrue(table.getDisabledCapabilities().contains(Capability.TABLE_DELETE));
         assertEquals(s, table.getCacheOf().getUseCases().get(0).getExcludeRecordsMatching().get(0).getCriteria().get(0).getFieldName());
         assertEquals(s, table.getRecordSecurityLocks().get(0).getSecurityKeyType());
         assertEquals(s, table.getRecordSecurityLocks().get(0).getJoinNameChain().get(0));
         assertEquals(s, table.getPermissionRules().getPermissionBaseName());
         assertEquals(s, table.getPermissionRules().getCustomPermissionChecker().getName());
         assertEquals(AuditLevel.NONE, table.getAuditRules().getAuditLevel());
         assertEquals(s, table.getAssociations().get(0).getName());
         assertEquals(s, table.getExposedJoins().get(0).getJoinTable());
         assertEquals(s, table.getExposedJoins().get(0).getJoinPath().get(0));
         assertEquals(s, table.getShareableTableMetaData().getSharedRecordTableName());
         assertEquals(s, table.getShareableTableMetaData().getAudienceTypes().get(s).getName());
         assertEquals(s, table.getHelpContent().get(s).get(0).getContent());
         assertEquals(s, table.getSourceQBitName());
         // hmm! table.setBackendDetails();
         // hmm! table.setSupplementalMetaData();
      };

      //////////////////////////////////////////////////////
      // make a table with its several-strings to set "a" //
      //////////////////////////////////////////////////////
      QTableMetaData table = new QTableMetaData();
      setAllStrings.accept(table, "a");
      assertAllStrings.accept(table, "a");

      /////////////////////////////////////////////////////////////////////////////
      // make a clone - assert everything started as "a", then change all to "b" //
      /////////////////////////////////////////////////////////////////////////////
      QTableMetaData clone = table.clone();
      assertAllStrings.accept(table, "a");
      setAllStrings.accept(clone, "b");

      /////////////////////////////////////////////////////
      // assert that the original didn't change anywhere //
      /////////////////////////////////////////////////////
      assertAllStrings.accept(table, "a");
      assertAllStrings.accept(clone, "b");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetFieldOrVirtualFieldReturnsRegularField()
   {
      QTableMetaData table = new QTableMetaData()
         .withName("test")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING));

      QFieldMetaData field = table.getFieldOrVirtualField("name");
      assertNotNull(field);
      assertEquals("name", field.getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetFieldOrVirtualFieldFallsBackToVirtualField()
   {
      QTableMetaData table = new QTableMetaData()
         .withName("test")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withVirtualField(new QVirtualFieldMetaData("nameLength", QFieldType.INTEGER));

      QFieldMetaData field = table.getFieldOrVirtualField("nameLength");
      assertNotNull(field);
      assertEquals("nameLength", field.getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetFieldOrVirtualFieldThrowsForUnknown()
   {
      QTableMetaData table = new QTableMetaData()
         .withName("test")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));

      assertThatThrownBy(() -> table.getFieldOrVirtualField("noSuchField"))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("noSuchField")
         .hasMessageContaining("not found");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetFieldOrVirtualFieldThrowsWhenFieldsNull()
   {
      QTableMetaData table = new QTableMetaData().withName("test");

      assertThatThrownBy(() -> table.getFieldOrVirtualField("anything"))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("does not have its fields defined");
   }

}