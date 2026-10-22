/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.assessment.QInstanceAssessor;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.rdbms.actions.AbstractRDBMSAction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;


/*******************************************************************************
 **
 *******************************************************************************/
public class RDBMSBackendAssessor
{
   private QInstanceAssessor    assessor;
   private RDBMSBackendMetaData backendMetaData;
   private List<QTableMetaData> tables;

   private Map<String, QFieldType> typeMap = new HashMap<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public RDBMSBackendAssessor(QInstanceAssessor assessor, RDBMSBackendMetaData backendMetaData, List<QTableMetaData> tables)
   {
      this.assessor = assessor;
      this.backendMetaData = backendMetaData;
      this.tables = tables;

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
      typeMap.put("BIGINT UNSIGNED", QFieldType.INTEGER);
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
   public void assess()
   {
      try(Connection connection = new ConnectionManager().getConnection(backendMetaData))
      {
         ////////////////////////////////////////////////////////////////////
         // read data type ids (integers) to names, for field-type mapping //
         ////////////////////////////////////////////////////////////////////
         DatabaseMetaData     databaseMetaData;
         Map<Integer, String> dataTypeMap = new HashMap<>();
         try
         {
            databaseMetaData = connection.getMetaData();
            ResultSet typeInfoResultSet = databaseMetaData.getTypeInfo();
            while(typeInfoResultSet.next())
            {
               String  name = typeInfoResultSet.getString("TYPE_NAME");
               Integer id   = typeInfoResultSet.getInt("DATA_TYPE");
               dataTypeMap.put(id, name);
            }
         }
         catch(Exception e)
         {
            assessor.addError("Error loading metaData from RDBMS for backendName: " + backendMetaData.getName() + " - assessment cannot be completed.", e);
            return;
         }

         ///////////////////////////////////////
         // process each table in the backend //
         ///////////////////////////////////////
         for(QTableMetaData table : tables)
         {
            String tableName = AbstractRDBMSAction.getTableName(table);

            try
            {
               ///////////////////////////////
               // check if the table exists //
               ///////////////////////////////
               String databaseName = backendMetaData.getDatabaseName(); // these work for mysql - unclear about other vendors.
               String schemaName   = null;
               try(ResultSet tableResultSet = databaseMetaData.getTables(databaseName, schemaName, tableName, null))
               {
                  if(!tableResultSet.next())
                  {
                     assessor.addError("Table: " + table.getName() + " was not found in backend: " + backendMetaData.getName());
                     assessor.addSuggestion(suggestCreateTable(table));
                     continue;
                  }

                  //////////////////////////////
                  // read the table's columns //
                  //////////////////////////////
                  Map<String, QFieldMetaData> columnMap            = new HashMap<>();
                  String                      primaryKeyColumnName = null;
                  try(ResultSet columnsResultSet = databaseMetaData.getColumns(databaseName, schemaName, tableName, null))
                  {
                     while(columnsResultSet.next())
                     {
                        String  columnName      = columnsResultSet.getString("COLUMN_NAME");
                        String  columnSize      = columnsResultSet.getString("COLUMN_SIZE");
                        Integer dataTypeId      = columnsResultSet.getInt("DATA_TYPE");
                        String  isNullable      = columnsResultSet.getString("IS_NULLABLE");
                        String  isAutoIncrement = columnsResultSet.getString("IS_AUTOINCREMENT");

                        String         dataTypeName   = dataTypeMap.get(dataTypeId);
                        QFieldMetaData columnMetaData = new QFieldMetaData(columnName, typeMap.get(dataTypeName));
                        columnMap.put(columnName, columnMetaData);

                        if("YES".equals(isAutoIncrement))
                        {
                           primaryKeyColumnName = columnName;
                        }
                     }
                  }

                  /////////////////////////////////
                  // diff the columns and fields //
                  /////////////////////////////////
                  for(QFieldMetaData column : columnMap.values())
                  {
                     boolean fieldExists = table.getFields().values().stream().anyMatch(f -> column.getName().equals(AbstractRDBMSAction.getColumnName(f)));
                     if(!fieldExists)
                     {
                        assessor.addWarning("Table: " + table.getName() + " has a column which was not found in the metaData: " + column.getName());
                        assessor.addSuggestion("// in QTableMetaData.withName(\"" + table.getName() + "\")\n"
                           + ".withField(new QFieldMetaData(\"" + column.getName() + "\", QFieldType." + column.getType() + ").withBackendName(\"" + column.getName() + "\")"); // todo - column_name to fieldName
                     }
                  }

                  for(QFieldMetaData field : table.getFields().values())
                  {
                     String  columnName   = AbstractRDBMSAction.getColumnName(field);
                     boolean columnExists = columnMap.values().stream().anyMatch(c -> c.getName().equals(columnName));
                     if(!columnExists)
                     {
                        assessor.addError("Table: " + table.getName() + " has a field which was not found in the database: " + field.getName());
                        assessor.addSuggestion("/* For table [" + tableName + "] in backend [" + table.getBackendName() + " (database " + databaseName + ")]: */\n"
                           + "ALTER TABLE " + tableName + " ADD " + QInstanceEnricher.inferBackendName(columnName) + " " + getDatabaseTypeForField(table, field) + ";");
                     }
                  }

                  ///////////////////////////////////////////////
                  // read unique constraints from the database //
                  ///////////////////////////////////////////////
                  Map<String, Set<String>> uniqueIndexMap = new HashMap<>();
                  try(ResultSet indexInfoResultSet = databaseMetaData.getIndexInfo(databaseName, schemaName, tableName, true, true))
                  {
                     while(indexInfoResultSet.next())
                     {
                        String indexName  = indexInfoResultSet.getString("INDEX_NAME");
                        String columnName = indexInfoResultSet.getString("COLUMN_NAME");
                        uniqueIndexMap.computeIfAbsent(indexName, k -> new HashSet<>());
                        uniqueIndexMap.get(indexName).add(columnName);
                     }
                  }

                  //////////////////////////
                  // diff the unique keys //
                  //////////////////////////
                  for(UniqueKey uniqueKey : CollectionUtils.nonNullList(table.getUniqueKeys()))
                  {
                     Set<String> fieldNames = uniqueKey.getFieldNames().stream().map(fieldName -> AbstractRDBMSAction.getColumnName(table.getField(fieldName))).collect(Collectors.toSet());
                     if(!uniqueIndexMap.containsValue(fieldNames))
                     {
                        assessor.addWarning("Table: " + table.getName() + " specifies a uniqueKey which was not found in the database: " + uniqueKey.getFieldNames());
                        assessor.addSuggestion("/* For table [" + tableName + "] in backend [" + table.getBackendName() + " (database " + databaseName + ")]: */\n"
                           + "ALTER TABLE " + tableName + " ADD UNIQUE (" + StringUtils.join(", ", fieldNames) + ");");
                     }
                  }

                  for(Set<String> uniqueIndex : uniqueIndexMap.values())
                  {
                     //////////////////////////
                     // skip the primary key //
                     //////////////////////////
                     if(uniqueIndex.size() == 1 && uniqueIndex.contains(primaryKeyColumnName))
                     {
                        continue;
                     }

                     boolean foundInTableMetaData = false;
                     for(UniqueKey uniqueKey : CollectionUtils.nonNullList(table.getUniqueKeys()))
                     {
                        Set<String> fieldNames = uniqueKey.getFieldNames().stream().map(fieldName -> AbstractRDBMSAction.getColumnName(table.getField(fieldName))).collect(Collectors.toSet());
                        if(uniqueIndex.equals(fieldNames))
                        {
                           foundInTableMetaData = true;
                           break;
                        }
                     }

                     if(!foundInTableMetaData)
                     {
                        assessor.addWarning("Table: " + table.getName() + " has a unique index which was not found in the metaData: " + uniqueIndex);
                        assessor.addSuggestion("// in QTableMetaData.withName(\"" + table.getName() + "\")\n"
                           + ".withUniqueKey(new UniqueKey(\"" + StringUtils.join("\", \"", uniqueIndex) + "\"))");
                     }
                  }

               }
            }
            catch(Exception e)
            {
               assessor.addError("Error assessing table: " + table.getName() + " in backend: " + backendMetaData.getName(), e);
            }
         }
      }
      catch(Exception e)
      {
         assessor.addError("Error connecting to RDBMS for backendName: " + backendMetaData.getName(), e);
         return;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String suggestCreateTable(QTableMetaData table)
   {
      StringBuilder rs = new StringBuilder("/* For table [" + table.getName() + "] in backend [" + table.getBackendName() + " (database " + (backendMetaData.getDatabaseName()) + ")]: */\n");
      rs.append("CREATE TABLE ").append(AbstractRDBMSAction.getTableName(table)).append("\n");
      rs.append("(\n");

      List<String> fields = new ArrayList<>();
      for(QFieldMetaData field : table.getFields().values())
      {
         fields.add("   " + AbstractRDBMSAction.getColumnName(field) + " " + getDatabaseTypeForField(table, field));
      }

      rs.append(StringUtils.join(",\n", fields));

      rs.append("\n);");
      return (rs.toString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getDatabaseTypeForField(QTableMetaData table, QFieldMetaData field)
   {
      return switch(field.getType())
      {
         case STRING ->
         {
            int n = Objects.requireNonNullElse(field.getMaxLength(), 250);
            yield ("VARCHAR(" + n + ")");
         }
         case INTEGER ->
         {
            String suffix = table.getPrimaryKeyField().equals(field.getName()) ? " AUTO_INCREMENT PRIMARY KEY" : "";
            yield ("INTEGER" + suffix);
         }
         case LONG ->
         {
            String suffix = table.getPrimaryKeyField().equals(field.getName()) ? " AUTO_INCREMENT PRIMARY KEY" : "";
            yield ("BIGINT" + suffix);
         }
         case DECIMAL -> "DECIMAL(10,2)";
         case BOOLEAN -> "BOOLEAN";
         case DATE -> "DATE";
         case TIME -> "TIME";
         case DATE_TIME -> "TIMESTAMP";
         case TEXT -> "TEXT";
         case HTML -> "TEXT";
         case PASSWORD -> "VARCHAR(40)";
         case BLOB -> "BLOB";
      };
   }
}
