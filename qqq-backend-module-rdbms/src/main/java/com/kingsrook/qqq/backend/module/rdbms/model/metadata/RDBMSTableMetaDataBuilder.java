/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.model.metadata;


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;


/*******************************************************************************
 ** note - this class is pretty mysql-specific
 *******************************************************************************/
public class RDBMSTableMetaDataBuilder
{
   private static final QLogger LOG = QLogger.getLogger(RDBMSTableMetaDataBuilder.class);

   private boolean useCamelCaseNames = true;

   private static Map<String, QFieldType> typeMap = new HashMap<>();

   static
   {
      ////////////////////////////////////////////////
      // these are types as returned by mysql       //
      // let null in here mean unsupported QQQ type //
      ////////////////////////////////////////////////
      typeMap.put("TEXT", QFieldType.TEXT);
      typeMap.put("BINARY", QFieldType.BLOB);
      typeMap.put("SET", null);
      typeMap.put("VARBINARY", QFieldType.BLOB);
      typeMap.put("MEDIUMBLOB", QFieldType.BLOB);
      typeMap.put("NUMERIC", QFieldType.INTEGER);
      typeMap.put("INTEGER", QFieldType.INTEGER);
      typeMap.put("BIGINT UNSIGNED", QFieldType.LONG);
      typeMap.put("MEDIUMINT UNSIGNED", QFieldType.INTEGER);
      typeMap.put("SMALLINT UNSIGNED", QFieldType.INTEGER);
      typeMap.put("TINYINT UNSIGNED", QFieldType.INTEGER);
      typeMap.put("BIT", null);
      typeMap.put("FLOAT", null);
      typeMap.put("REAL", null);
      typeMap.put("VARCHAR", QFieldType.STRING);
      typeMap.put("BOOL", QFieldType.BOOLEAN);
      typeMap.put("YEAR", null);
      typeMap.put("TIME", QFieldType.TIME);
      typeMap.put("TIMESTAMP", QFieldType.DATE_TIME);
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData buildTableMetaData(RDBMSBackendMetaData backendMetaData, String tableName) throws QException
   {
      try(Connection connection = new ConnectionManager().getConnection(backendMetaData))
      {
         List<QFieldMetaData> fieldMetaDataList = new ArrayList<>();
         String               primaryKey        = null;

         DatabaseMetaData     databaseMetaData  = connection.getMetaData();
         Map<Integer, String> dataTypeMap       = new HashMap<>();
         ResultSet            typeInfoResultSet = databaseMetaData.getTypeInfo();
         while(typeInfoResultSet.next())
         {
            String  name = typeInfoResultSet.getString("TYPE_NAME");
            Integer id   = typeInfoResultSet.getInt("DATA_TYPE");
            dataTypeMap.put(id, name);
         }

         // todo - for h2, uppercase both db & table names...
         String databaseName = backendMetaData.getDatabaseName(); // these work for mysql - unclear about other vendors.
         String schemaName   = null;
         String tableNameForMetaDataQueries = tableName;

         try(ResultSet tableResultSet = databaseMetaData.getTables(databaseName, schemaName, tableNameForMetaDataQueries, null))
         {
            if(!tableResultSet.next())
            {
               throw (new QException("Table: " + tableName + " was not found in backend: " + backendMetaData.getName()));
            }
         }

         try(ResultSet columnsResultSet = databaseMetaData.getColumns(databaseName, schemaName, tableNameForMetaDataQueries, null))
         {
            while(columnsResultSet.next())
            {
               String  columnName      = columnsResultSet.getString("COLUMN_NAME");
               String  columnSize      = columnsResultSet.getString("COLUMN_SIZE");
               Integer dataTypeId      = columnsResultSet.getInt("DATA_TYPE");
               String  isNullable      = columnsResultSet.getString("IS_NULLABLE");
               String  isAutoIncrement = columnsResultSet.getString("IS_AUTOINCREMENT");

               String     dataTypeName = dataTypeMap.get(dataTypeId);
               QFieldType type         = typeMap.get(dataTypeName);
               if(type == null)
               {
                  LOG.info("Table " + tableName + " column " + columnName + " has an unmapped type: " + dataTypeId + ".  Field will not be added to QTableMetaData");
                  continue;
               }

               String qqqFieldName = QInstanceEnricher.inferNameFromBackendName(columnName);

               QFieldMetaData fieldMetaData = new QFieldMetaData(qqqFieldName, type)
                  // todo - what string? .withIsRequired(!isNullable)
                  .withBackendName(columnName);

               fieldMetaDataList.add(fieldMetaData);

               if("YES".equals(isAutoIncrement))
               {
                  primaryKey = qqqFieldName;
               }
            }
         }

         if(fieldMetaDataList.isEmpty())
         {
            throw (new QException("Could not find any usable fields in table: " + tableName));
         }

         if(primaryKey == null)
         {
            throw (new QException("Could not find primary key in table: " + tableName));
         }

         String qqqTableName = QInstanceEnricher.inferNameFromBackendName(tableName);

         QTableMetaData tableMetaData = new QTableMetaData()
            .withBackendName(backendMetaData.getName())
            .withName(qqqTableName)
            .withBackendDetails(new RDBMSTableBackendDetails().withTableName(tableName))
            .withFields(fieldMetaDataList)
            .withPrimaryKeyField(primaryKey);

         return (tableMetaData);
      }
      catch(Exception e)
      {
         throw (new QException("Error automatically building table meta data for table: " + tableName, e));
      }
   }


   /*******************************************************************************
    ** Getter for useCamelCaseNames
    *******************************************************************************/
   public boolean getUseCamelCaseNames()
   {
      return (this.useCamelCaseNames);
   }



   /*******************************************************************************
    ** Setter for useCamelCaseNames
    *******************************************************************************/
   public void setUseCamelCaseNames(boolean useCamelCaseNames)
   {
      this.useCamelCaseNames = useCamelCaseNames;
   }



   /*******************************************************************************
    ** Fluent setter for useCamelCaseNames
    *******************************************************************************/
   public RDBMSTableMetaDataBuilder withUseCamelCaseNames(boolean useCamelCaseNames)
   {
      this.useCamelCaseNames = useCamelCaseNames;
      return (this);
   }


}
