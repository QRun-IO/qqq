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

package com.kingsrook.qqq.backend.module.filesystem.sftp.actions;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.sftp.BaseSFTPTest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Exercise native discovery against the owned SFTP server.
 *******************************************************************************/
class SFTPAssociatedRecordDiscoveryTest extends BaseSFTPTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFilenameMembership() throws Exception
   {
      var instance = QContext.getQInstance();
      QTableMetaData parent = new QTableMetaData().withName("family").withBackendName(TestUtils.BACKEND_NAME_MEMORY)
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("memberValue", QFieldType.STRING));
      Association association = new Association().withName("members").withAssociatedTableName(TestUtils.TABLE_NAME_SFTP_FILE).withJoinName("familyMembers");
      parent.withAssociation(association);
      instance.addTable(parent);
      instance.addJoin(new QJoinMetaData().withName("familyMembers").withLeftTable("family").withRightTable(TestUtils.TABLE_NAME_SFTP_FILE)
         .withJoinOn(new JoinOn("memberValue", "fileName")));
      assertThat(AssociatedRecordDiscovery.findPrimaryKeys(parent, association,
         List.of(new AssociatedRecordDiscovery.Parent(Map.of("memberValue", "testfile-1.txt"), List.of())), null)).containsExactly("testfile-1.txt");
   }
}
