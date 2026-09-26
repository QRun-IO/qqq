/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.instances.loaders;


import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.loaders.implementations.QTableMetaDataLoader;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.YamlUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for QTableMetaDataLoader 
 *******************************************************************************/
class QTableMetaDataLoaderTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @Disabled("Not quite yet passing - is a good goal to get to though!")
   void testToYaml() throws QMetaDataLoaderException
   {
      QTableMetaData expectedTable = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      String expectedYaml = YamlUtils.toYaml(expectedTable);
      QTableMetaData actualTable = new QTableMetaDataLoader().fileToMetaDataObject(new QInstance(), IOUtils.toInputStream(expectedYaml, StandardCharsets.UTF_8), "person.yaml");
      String actualYaml = YamlUtils.toYaml(actualTable);
      assertEquals(expectedYaml, actualYaml);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testYaml() throws QMetaDataLoaderException
   {
      QTableMetaData table = new QTableMetaDataLoader().fileToMetaDataObject(new QInstance(), IOUtils.toInputStream("""
         class: QTableMetaData
         version: 1.0
         name: myTable
         icon:
            name: account_tree
         fields:
            id:
               name: id
               type: INTEGER
            name:
               name: name
               type: STRING
         uniqueKeys:
         -  label: Name
            fieldNames:
            - name
         associations:
         -  name: A1
            associatedTableName: yourTable
            joinName: myTableJoinYourTable
         -  name: A2
            associatedTableName: theirTable
            joinName: myTableJoinTheirTable
         permissionRules:
            level: READ_WRITE_PERMISSIONS
            denyBehavior: HIDDEN
            permissionBaseName: myTablePermissions
            customPermissionChecker:
               name: com.kingsrook.SomeChecker
               codeType: JAVA
         ## todo recordSecurityLocks
         ## todo auditRules
         ## todo backendDetails
         ## todo automationDetails
         sections:
         -  name: identity
            label: Identity
            icon:
               name: badge
            tier: T1
            fieldNames:
            -  id
            -  firstName
            -  lastName
         customizers:
            postQueryRecord:
               name: com.kingsrook.SomePostQuery
               codeType: JAVA
            preDeleteRecord:
               name: com.kingsrook.SomePreDelete
               codeType: JAVA
         disabledCapabilities:
         -   TABLE_COUNT
         -   QUERY_STATS
         """, StandardCharsets.UTF_8), "myTable.yaml");

      assertEquals("myTable", table.getName());

      assertEquals(2, table.getFields().size());
      // assertEquals("id", table.getFields().get("id").getName());
      assertEquals(QFieldType.INTEGER, table.getFields().get("id").getType());
      // assertEquals("name", table.getFields().get("name").getName());
      assertEquals(QFieldType.STRING, table.getFields().get("name").getType());

      assertNotNull(table.getIcon());
      assertEquals("account_tree", table.getIcon().getName());

      assertEquals(1, table.getUniqueKeys().size());
      assertEquals(List.of("name"), table.getUniqueKeys().get(0).getFieldNames());
      assertEquals("Name", table.getUniqueKeys().get(0).getLabel());

      assertEquals(2, table.getAssociations().size());
      assertEquals("A1", table.getAssociations().get(0).getName());
      assertEquals("theirTable", table.getAssociations().get(1).getAssociatedTableName());

      assertNotNull(table.getPermissionRules());
      assertEquals(PermissionLevel.READ_WRITE_PERMISSIONS, table.getPermissionRules().getLevel());
      assertEquals(DenyBehavior.HIDDEN, table.getPermissionRules().getDenyBehavior());
      assertEquals("myTablePermissions", table.getPermissionRules().getPermissionBaseName());
      assertNotNull(table.getPermissionRules().getCustomPermissionChecker());
      assertEquals("com.kingsrook.SomeChecker", table.getPermissionRules().getCustomPermissionChecker().getName());
      assertEquals(QCodeType.JAVA, table.getPermissionRules().getCustomPermissionChecker().getCodeType());

      assertEquals(1, table.getSections().size());
      assertEquals("identity", table.getSections().get(0).getName());
      assertEquals(Tier.T1, table.getSections().get(0).getTier());
      assertEquals(List.of("id", "firstName", "lastName"), table.getSections().get(0).getFieldNames());

      assertEquals(2, table.getCustomizers().size());
      assertEquals("com.kingsrook.SomePostQuery", table.getCustomizers().get(TableCustomizers.POST_QUERY_RECORD.getRole()).getName());
      assertEquals("com.kingsrook.SomePreDelete", table.getCustomizers().get(TableCustomizers.PRE_DELETE_RECORD.getRole()).getName());

      assertEquals(Set.of(Capability.TABLE_COUNT, Capability.QUERY_STATS), table.getDisabledCapabilities());
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSimpleJson() throws QMetaDataLoaderException
   {
      QTableMetaData table = new QTableMetaDataLoader().fileToMetaDataObject(new QInstance(), IOUtils.toInputStream("""
         {
            "class": "QTableMetaData",
            "version": "1.0",
            "name": "myTable",
            "fields":
            {
               "id": {"name": "id", "type": "INTEGER"},
               "name": {"name": "name", "type": "STRING"}
            }
         }
         """, StandardCharsets.UTF_8), "myTable.json");

      assertEquals("myTable", table.getName());
      assertEquals(2, table.getFields().size());
      assertEquals("id", table.getFields().get("id").getName());
      assertEquals(QFieldType.INTEGER, table.getFields().get("id").getType());
      assertEquals("name", table.getFields().get("name").getName());
      assertEquals(QFieldType.STRING, table.getFields().get("name").getType());
   }


}