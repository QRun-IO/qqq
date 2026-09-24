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

package com.kingsrook.qqq.backend.module.filesystem.s3.actions;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.s3.BaseS3Test;
import com.kingsrook.qqq.backend.module.filesystem.s3.S3BackendModule;
import com.kingsrook.qqq.backend.module.filesystem.s3.utils.S3Utils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Exercise the inherited discovery operation against the owned LocalStack bucket.
 *******************************************************************************/
public class S3AssociatedRecordDiscoveryTest extends BaseS3Test
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMembershipAcrossObjects() throws Exception
   {
      QBackendModuleDispatcher.registerBackendModule(new LocalDiscoveryBackend());
      QContext.getQInstance().getBackend(TestUtils.BACKEND_NAME_S3).setBackendType("discoveryS3Test");
      var instance = QContext.getQInstance();
      QTableMetaData parent = new QTableMetaData().withName("family").withBackendName(TestUtils.BACKEND_NAME_MEMORY)
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("memberValue", QFieldType.STRING));
      Association association = new Association().withName("members").withAssociatedTableName(TestUtils.TABLE_NAME_PERSON_S3).withJoinName("familyMembers");
      parent.withAssociation(association);
      instance.addTable(parent);
      instance.addJoin(new QJoinMetaData().withName("familyMembers").withLeftTable("family").withRightTable(TestUtils.TABLE_NAME_PERSON_S3)
         .withJoinOn(new JoinOn("memberValue", "lastName")));
      assertThat(AssociatedRecordDiscovery.findPrimaryKeys(parent, association,
         List.of(new AssociatedRecordDiscovery.Parent(Map.of("memberValue", "S"), List.of(4))), null)).containsExactlyInAnyOrder(3, 5);
   }



   /*******************************************************************************
    ** Change only the transport endpoint; retain the native query implementation.
    *******************************************************************************/
   public static class LocalDiscoveryBackend extends S3BackendModule
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getBackendType()
      {
         return "discoveryS3Test";
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QueryInterface getQueryInterface()
      {
         S3QueryAction action = new S3QueryAction();
         S3Utils utils = new S3Utils();
         utils.setAmazonS3(cloud.localstack.awssdkv1.TestUtils.getClientS3());
         action.setS3Utils(utils);
         return action;
      }
   }
}
