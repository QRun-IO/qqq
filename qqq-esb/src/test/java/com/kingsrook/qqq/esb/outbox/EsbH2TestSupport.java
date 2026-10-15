/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.outbox;


import java.sql.Connection;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;


/*******************************************************************************
 * An H2 (transactional) backend for the outbox and dedupe tests, with an order
 * table, the outbox table, and the processed-message table on it.
 *******************************************************************************/
public final class EsbH2TestSupport
{
   public static final String BACKEND_NAME     = "h2";
   public static final String TABLE_NAME_ORDER = "h2Order";
   public static final String OUTBOX_TABLE     = "esb_outbox";
   public static final String PROCESSED_TABLE  = "esb_processed_message";



   /*******************************************************************************
    ** Static helpers only.
    *******************************************************************************/
   private EsbH2TestSupport()
   {
   }



   /*******************************************************************************
    ** The H2 backend (one in-memory database, kept for the JVM).
    *******************************************************************************/
   public static RDBMSBackendMetaData defineBackend()
   {
      RDBMSBackendMetaData backend = new RDBMSBackendMetaData()
         .withName(BACKEND_NAME)
         .withVendor("h2")
         .withHostName("mem")
         .withDatabaseName("esb_outbox_test")
         .withUsername("sa");
      backend.setJdbcUrl(ConnectionManager.getJdbcUrl(backend) + ";DATABASE_TO_UPPER=FALSE");
      return (backend);
   }



   /*******************************************************************************
    ** Add the backend and the three tables to the instance.
    *******************************************************************************/
   public static void addTo(QInstance qInstance)
   {
      qInstance.addBackend(defineBackend());
      qInstance.addTable(new QTableMetaData()
         .withName(TABLE_NAME_ORDER)
         .withBackendName(BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("orderNo", QFieldType.STRING).withBackendName("order_no"))
         .withField(new QFieldMetaData("status", QFieldType.STRING))
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("h2_order")));
      qInstance.addTable(EsbOutboxTable.defineTable(OUTBOX_TABLE, BACKEND_NAME));
      qInstance.addTable(EsbProcessedMessageTable.defineTable(PROCESSED_TABLE, BACKEND_NAME));
   }



   /*******************************************************************************
    ** Create the tables (if needed) and empty them.
    *******************************************************************************/
   public static void resetDatabase() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(defineBackend()))
      {
         QueryManager.executeUpdate(connection, """
            CREATE TABLE IF NOT EXISTS h2_order
            (
               id INT AUTO_INCREMENT PRIMARY KEY,
               order_no VARCHAR(100),
               status VARCHAR(100)
            )""");
         QueryManager.executeUpdate(connection, """
            CREATE TABLE IF NOT EXISTS esb_outbox
            (
               id BIGINT AUTO_INCREMENT PRIMARY KEY,
               uuid VARCHAR(36) NOT NULL UNIQUE,
               destination_name VARCHAR(250) NOT NULL,
               event_type VARCHAR(250),
               event_json VARCHAR(100000) NOT NULL,
               status VARCHAR(20) NOT NULL,
               attempts INT NOT NULL DEFAULT 0,
               next_attempt_at TIMESTAMP,
               lease_owner VARCHAR(250),
               lease_expires_at TIMESTAMP,
               last_error VARCHAR(100000),
               created_at TIMESTAMP NOT NULL,
               sent_at TIMESTAMP
            )""");
         QueryManager.executeUpdate(connection, """
            CREATE TABLE IF NOT EXISTS esb_processed_message
            (
               id BIGINT AUTO_INCREMENT PRIMARY KEY,
               trigger_name VARCHAR(250) NOT NULL,
               message_id VARCHAR(250) NOT NULL,
               processed_at TIMESTAMP NOT NULL,
               UNIQUE (trigger_name, message_id)
            )""");
         QueryManager.executeUpdate(connection, "DELETE FROM h2_order");
         QueryManager.executeUpdate(connection, "DELETE FROM esb_outbox");
         QueryManager.executeUpdate(connection, "DELETE FROM esb_processed_message");
      }
   }

}
