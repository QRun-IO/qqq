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

package com.kingsrook.qqq.backend.module.filesystem.local.actions;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;


/*******************************************************************************
 ** Relationship discovery must filter rows across every source file.
 *******************************************************************************/
class FilesystemAssociatedRecordDiscoveryTest extends FilesystemActionTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJsonFiltersParentMembership() throws Exception
   {
      assertMembers(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON, "lastName", "S", List.of(), List.of(3));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCsvFiltersMembershipAndRetainedKeysAcrossFiles() throws Exception
   {
      assertMembers(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_CSV, "lastName", "S", List.of(4), List.of(3, 5));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneRecordPerFileUsesFilenameKey() throws Exception
   {
      assertMembers(TestUtils.TABLE_NAME_BLOB_LOCAL_FS, "fileName", "BLOB-2.txt", List.of(), List.of("BLOB-2.txt"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHeavyContentsUsedByRelationshipAreRead() throws Exception
   {
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).getField("contents")
         .withType(QFieldType.STRING).withIsHeavy(true);
      assertMembers(TestUtils.TABLE_NAME_BLOB_LOCAL_FS, "contents", "Hi Bob", List.of(), List.of("BLOB-2.txt"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertMembers(String tableName, String childField, String value, List<Serializable> retained, List<Serializable> expected) throws Exception
   {
      var instance = QContext.getQInstance();
      var session = QContext.getQSession();
      QTableMetaData child = instance.getTable(tableName);
      child.withRecordSecurityLock(new RecordSecurityLock().withFieldName(childField).withSecurityKeyType("hidden"));
      var locks = child.getRecordSecurityLocks();
      QTableMetaData parent = new QTableMetaData().withName("family").withBackendName(TestUtils.BACKEND_NAME_MEMORY)
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("memberValue", QFieldType.STRING));
      Association association = new Association().withName("members").withAssociatedTableName(tableName).withJoinName("familyMembers");
      parent.withAssociation(association);
      instance.addTable(parent);
      instance.addJoin(new QJoinMetaData().withName("familyMembers").withLeftTable("family").withRightTable(tableName)
         .withJoinOn(new JoinOn("memberValue", childField)));

      assertThat(AssociatedRecordDiscovery.findPrimaryKeys(parent, association,
         List.of(new AssociatedRecordDiscovery.Parent(Map.of("memberValue", value), retained)), null)).containsExactlyInAnyOrderElementsOf(expected);
      assertSame(instance, QContext.getQInstance());
      assertSame(session, QContext.getQSession());
      assertSame(locks, child.getRecordSecurityLocks());
   }
}
