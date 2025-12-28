/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.actions.io;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 * Input wrapper for the methods in
 * {@link com.kingsrook.qqq.api.actions.QRecordApiAdapter} that adapt from
 * QRecords To API objects.
 *
 * Originally, the methods in that class just took records and table/apiName/version
 * - but - when we wanted to start adding optional behaviors (e.g., whether to
 * includeExposedJoins), then this class was introduced.
 *******************************************************************************/
public class QRecordApiAdapterToApiInput implements Serializable
{
   private List<QRecord> inputRecords;
   private String        tableName;
   private String        apiName;
   private String        apiVersion;

   private boolean includeExposedJoins = false;



   /*******************************************************************************
    * Getter for inputRecords
    * @see #withInputRecords(List)
    *******************************************************************************/
   public List<QRecord> getInputRecords()
   {
      return (this.inputRecords);
   }



   /*******************************************************************************
    * Setter for inputRecords
    * @see #withInputRecords(List)
    *******************************************************************************/
   public void setInputRecords(List<QRecord> inputRecords)
   {
      this.inputRecords = inputRecords;
   }



   /*******************************************************************************
    * Fluent setter for inputRecords
    *
    * @param inputRecords
    * list of records to be adapted to api objects
    * @return this
    *******************************************************************************/
   public QRecordApiAdapterToApiInput withInputRecords(List<QRecord> inputRecords)
   {
      this.inputRecords = inputRecords;
      return (this);
   }



   /*******************************************************************************
    * Getter for tableName
    * @see #withTableName(String)
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    * Setter for tableName
    * @see #withTableName(String)
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    * Fluent setter for tableName
    *
    * @param tableName
    * name of the table that input records are from (main table, in the case joined records)
    * @return this
    *******************************************************************************/
   public QRecordApiAdapterToApiInput withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    * Getter for apiName
    * @see #withApiName(String)
    *******************************************************************************/
   public String getApiName()
   {
      return (this.apiName);
   }



   /*******************************************************************************
    * Setter for apiName
    * @see #withApiName(String)
    *******************************************************************************/
   public void setApiName(String apiName)
   {
      this.apiName = apiName;
   }



   /*******************************************************************************
    * Fluent setter for apiName
    *
    * @param apiName
    * api name of the output records
    * @return this
    *******************************************************************************/
   public QRecordApiAdapterToApiInput withApiName(String apiName)
   {
      this.apiName = apiName;
      return (this);
   }



   /*******************************************************************************
    * Getter for apiVersion
    * @see #withApiVersion(String)
    *******************************************************************************/
   public String getApiVersion()
   {
      return (this.apiVersion);
   }



   /*******************************************************************************
    * Setter for apiVersion
    * @see #withApiVersion(String)
    *******************************************************************************/
   public void setApiVersion(String apiVersion)
   {
      this.apiVersion = apiVersion;
   }



   /*******************************************************************************
    * Fluent setter for apiVersion
    *
    * @param apiVersion
    * api version for the output records
    * @return this
    *******************************************************************************/
   public QRecordApiAdapterToApiInput withApiVersion(String apiVersion)
   {
      this.apiVersion = apiVersion;
      return (this);
   }



   /*******************************************************************************
    * Getter for includeExposedJoins
    * @see #withIncludeExposedJoins(boolean)
    *******************************************************************************/
   public boolean getIncludeExposedJoins()
   {
      return (this.includeExposedJoins);
   }



   /*******************************************************************************
    * Setter for includeExposedJoins
    * @see #withIncludeExposedJoins(boolean)
    *******************************************************************************/
   public void setIncludeExposedJoins(boolean includeExposedJoins)
   {
      this.includeExposedJoins = includeExposedJoins;
   }



   /*******************************************************************************
    * Fluent setter for includeExposedJoins
    *
    * @param includeExposedJoins
    * boolean to control if exposedJoins should be supported.  By default, they are
    * not - but a QQQ middleware implementation may want them, so it can set this flag
    * to true to get them
    * @return this
    *******************************************************************************/
   public QRecordApiAdapterToApiInput withIncludeExposedJoins(boolean includeExposedJoins)
   {
      this.includeExposedJoins = includeExposedJoins;
      return (this);
   }

}
