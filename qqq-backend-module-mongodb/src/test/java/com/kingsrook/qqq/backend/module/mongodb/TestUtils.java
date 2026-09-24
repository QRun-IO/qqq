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

package com.kingsrook.qqq.backend.module.mongodb;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PVSValueFormatAndFields;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBTableBackendDetails;


/*******************************************************************************
 ** Test Utils class for this module
 **
 ** Note - tons of copying from RDMBS... wouldn't it be nice to share??
 *******************************************************************************/
public class TestUtils
{
   public static final String DEFAULT_BACKEND_NAME = "default";

   public static final String TABLE_NAME_PERSON = "personTable";

   public static final String TABLE_NAME_STORE               = "store";
   public static final String TABLE_NAME_ORDER               = "order";
   public static final String TABLE_NAME_ORDER_INSTRUCTIONS  = "orderInstructions";
   public static final String TABLE_NAME_ITEM                = "item";
   public static final String TABLE_NAME_ORDER_LINE          = "orderLine";
   public static final String TABLE_NAME_LINE_ITEM_EXTRINSIC = "orderLineExtrinsic";
   public static final String TABLE_NAME_WAREHOUSE           = "warehouse";
   public static final String TABLE_NAME_WAREHOUSE_STORE_INT = "warehouseStoreInt";

   public static final String SECURITY_KEY_STORE_ALL_ACCESS = "storeAllAccess";

   public static final String  MONGO_USERNAME = "mongoUser";
   public static final String  MONGO_PASSWORD = "password";
   public static final Integer MONGO_PORT     = 27017;
   public static final String  MONGO_DATABASE = "testDatabase";



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QInstance defineInstance()
   {
      QInstance qInstance = new QInstance();
      qInstance.addBackend(defineBackend());
      qInstance.addTable(defineTablePerson());
      qInstance.addPossibleValueSource(definePvsPerson());
      addOmsTablesAndJoins(qInstance);
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), defineAuthentication());
      return (qInstance);
   }



   /*******************************************************************************
    ** Define the authentication used in standard tests - using 'mock' type.
    **
    *******************************************************************************/
   public static QAuthenticationMetaData defineAuthentication()
   {
      return new QAuthenticationMetaData()
         .withName("mock")
         .withType(QAuthenticationType.MOCK);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static MongoDBBackendMetaData defineBackend()
   {
      return (new MongoDBBackendMetaData()
         .withName(DEFAULT_BACKEND_NAME)
         .withHost("localhost")
         .withPort(TestUtils.MONGO_PORT)
         .withUsername(TestUtils.MONGO_USERNAME)
         .withPassword(TestUtils.MONGO_PASSWORD)
         .withAuthSourceDatabase("admin")
         .withDatabaseName(TestUtils.MONGO_DATABASE)
         .withTransactionsSupported(false));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineTablePerson()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_PERSON)
         .withLabel("Person")
         .withRecordLabelFormat("%s %s")
         .withRecordLabelFields("firstName", "lastName")
         .withBackendName(DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.STRING).withBackendName("_id"))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("metaData.createDate"))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withBackendName("metaData.modifyDate"))
         .withField(new QFieldMetaData("seqNo", QFieldType.INTEGER))
         .withField(new QFieldMetaData("firstName", QFieldType.STRING))
         .withField(new QFieldMetaData("lastName", QFieldType.STRING))
         .withField(new QFieldMetaData("birthDate", QFieldType.DATE))
         .withField(new QFieldMetaData("email", QFieldType.STRING))
         .withField(new QFieldMetaData("isEmployed", QFieldType.BOOLEAN))
         .withField(new QFieldMetaData("annualSalary", QFieldType.DECIMAL))
         .withField(new QFieldMetaData("daysWorked", QFieldType.INTEGER))
         .withField(new QFieldMetaData("homeTown", QFieldType.STRING))
         .withBackendDetails(new MongoDBTableBackendDetails()
            .withTableName(TABLE_NAME_PERSON));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QPossibleValueSource definePvsPerson()
   {
      return (new QPossibleValueSource()
         .withName(TABLE_NAME_PERSON)
         .withType(QPossibleValueSourceType.TABLE)
         .withTableName(TABLE_NAME_PERSON)
         .withValueFormatAndFields(PVSValueFormatAndFields.LABEL_ONLY));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void addOmsTablesAndJoins(QInstance qInstance)
   {
      qInstance.addTable(defineBaseTable(TABLE_NAME_STORE, "store")
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("name")
         .withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType(TABLE_NAME_STORE).withFieldName("key"))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_ORDER, "order")
         .withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType(TABLE_NAME_STORE).withFieldName("storeKey"))
         .withAssociation(new Association().withName("orderLine").withAssociatedTableName(TABLE_NAME_ORDER_LINE).withJoinName("orderJoinOrderLine"))
         .withExposedJoin(new ExposedJoin().withJoinTable(TABLE_NAME_ITEM).withJoinPath(List.of("orderJoinOrderLine", "orderLineJoinItem")))
         .withField(new QFieldMetaData("storeKey", QFieldType.INTEGER).withPossibleValueSourceName(TABLE_NAME_STORE))
         .withField(new QFieldMetaData("billToPersonId", QFieldType.STRING).withPossibleValueSourceName(TABLE_NAME_PERSON))
         .withField(new QFieldMetaData("shipToPersonId", QFieldType.STRING).withPossibleValueSourceName(TABLE_NAME_PERSON))
         .withField(new QFieldMetaData("currentOrderInstructionsId", QFieldType.STRING).withPossibleValueSourceName(TABLE_NAME_PERSON))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_ORDER_INSTRUCTIONS, "order_instructions")
         .withRecordSecurityLock(new RecordSecurityLock()
            .withSecurityKeyType(TABLE_NAME_STORE)
            .withFieldName("order.storeKey")
            .withJoinNameChain(List.of("orderInstructionsJoinOrder")))
         .withField(new QFieldMetaData("orderId", QFieldType.STRING))
         .withField(new QFieldMetaData("instructions", QFieldType.STRING))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_ITEM, "item")
         .withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType(TABLE_NAME_STORE).withFieldName("storeKey"))
         .withExposedJoin(new ExposedJoin().withJoinTable(TABLE_NAME_ORDER).withJoinPath(List.of("orderLineJoinItem", "orderJoinOrderLine")))
         .withField(new QFieldMetaData("sku", QFieldType.STRING))
         .withField(new QFieldMetaData("description", QFieldType.STRING))
         .withField(new QFieldMetaData("storeKey", QFieldType.INTEGER).withPossibleValueSourceName(TABLE_NAME_STORE))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_ORDER_LINE, "order_line")
         .withRecordSecurityLock(new RecordSecurityLock()
            .withSecurityKeyType(TABLE_NAME_STORE)
            .withFieldName("order.storeKey")
            .withJoinNameChain(List.of("orderJoinOrderLine")))
         .withAssociation(new Association().withName("extrinsics").withAssociatedTableName(TABLE_NAME_LINE_ITEM_EXTRINSIC).withJoinName("orderLineJoinLineItemExtrinsic"))
         .withField(new QFieldMetaData("orderId", QFieldType.STRING))
         .withField(new QFieldMetaData("sku", QFieldType.STRING))
         .withField(new QFieldMetaData("storeKey", QFieldType.INTEGER).withPossibleValueSourceName(TABLE_NAME_STORE))
         .withField(new QFieldMetaData("quantity", QFieldType.INTEGER))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_LINE_ITEM_EXTRINSIC, "line_item_extrinsic")
         .withRecordSecurityLock(new RecordSecurityLock()
            .withSecurityKeyType(TABLE_NAME_STORE)
            .withFieldName("order.storeKey")
            .withJoinNameChain(List.of("orderJoinOrderLine", "orderLineJoinLineItemExtrinsic")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("orderLineId", QFieldType.STRING))
         .withField(new QFieldMetaData("key", QFieldType.STRING))
         .withField(new QFieldMetaData("value", QFieldType.STRING))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_WAREHOUSE_STORE_INT, "warehouse_store_int")
         .withField(new QFieldMetaData("warehouseId", QFieldType.STRING))
         .withField(new QFieldMetaData("storeKey", QFieldType.INTEGER))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_WAREHOUSE, "warehouse")
         .withRecordSecurityLock(new RecordSecurityLock()
            .withSecurityKeyType(TABLE_NAME_STORE)
            .withFieldName(TABLE_NAME_WAREHOUSE_STORE_INT + ".storeKey")
            .withJoinNameChain(List.of(QJoinMetaData.makeInferredJoinName(TestUtils.TABLE_NAME_WAREHOUSE, TestUtils.TABLE_NAME_WAREHOUSE_STORE_INT)))
         )
         .withField(new QFieldMetaData("name", QFieldType.STRING))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withType(JoinType.ONE_TO_MANY)
         .withLeftTable(TestUtils.TABLE_NAME_WAREHOUSE)
         .withRightTable(TestUtils.TABLE_NAME_WAREHOUSE_STORE_INT)
         .withInferredName()
         .withJoinOn(new JoinOn("id", "warehouseId"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderJoinStore")
         .withLeftTable(TABLE_NAME_ORDER)
         .withRightTable(TABLE_NAME_STORE)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("storeKey", "key"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderJoinBillToPerson")
         .withLeftTable(TABLE_NAME_ORDER)
         .withRightTable(TABLE_NAME_PERSON)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("billToPersonId", "id"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderJoinShipToPerson")
         .withLeftTable(TABLE_NAME_ORDER)
         .withRightTable(TABLE_NAME_PERSON)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("shipToPersonId", "id"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("itemJoinStore")
         .withLeftTable(TABLE_NAME_ITEM)
         .withRightTable(TABLE_NAME_STORE)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("storeKey", "key"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderJoinOrderLine")
         .withLeftTable(TABLE_NAME_ORDER)
         .withRightTable(TABLE_NAME_ORDER_LINE)
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "orderId"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderLineJoinItem")
         .withLeftTable(TABLE_NAME_ORDER_LINE)
         .withRightTable(TABLE_NAME_ITEM)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("sku", "sku"))
         .withJoinOn(new JoinOn("storeKey", "storeKey"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderLineJoinLineItemExtrinsic")
         .withLeftTable(TABLE_NAME_ORDER_LINE)
         .withRightTable(TABLE_NAME_LINE_ITEM_EXTRINSIC)
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "orderLineId"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderJoinCurrentOrderInstructions")
         .withLeftTable(TABLE_NAME_ORDER)
         .withRightTable(TABLE_NAME_ORDER_INSTRUCTIONS)
         .withType(JoinType.ONE_TO_ONE)
         .withJoinOn(new JoinOn("currentOrderInstructionsId", "id"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderInstructionsJoinOrder")
         .withLeftTable(TABLE_NAME_ORDER_INSTRUCTIONS)
         .withRightTable(TABLE_NAME_ORDER)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("orderId", "id"))
      );

      qInstance.addPossibleValueSource(new QPossibleValueSource()
         .withName("store")
         .withType(QPossibleValueSourceType.TABLE)
         .withTableName(TABLE_NAME_STORE)
         .withValueFormatAndFields(PVSValueFormatAndFields.LABEL_ONLY)
      );

      qInstance.addSecurityKeyType(new QSecurityKeyType()
         .withName(TABLE_NAME_STORE)
         .withAllAccessKeyName(SECURITY_KEY_STORE_ALL_ACCESS)
         .withPossibleValueSourceName(TABLE_NAME_STORE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QTableMetaData defineBaseTable(String tableName, String backendTableName)
   {
      return new QTableMetaData()
         .withName(tableName)
         .withBackendName(DEFAULT_BACKEND_NAME)
         .withBackendDetails(new MongoDBTableBackendDetails().withTableName(backendTableName))
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.STRING))
         .withField(new QFieldMetaData("key", QFieldType.INTEGER));
   }

}
