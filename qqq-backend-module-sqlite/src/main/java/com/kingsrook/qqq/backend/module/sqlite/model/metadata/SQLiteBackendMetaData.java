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

package com.kingsrook.qqq.backend.module.sqlite.model.metadata;


import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.strategy.RDBMSActionStrategyInterface;
import com.kingsrook.qqq.backend.module.sqlite.SQLiteBackendModule;
import com.kingsrook.qqq.backend.module.sqlite.strategy.SQLiteRDBMSActionStrategy;
import org.sqlite.JDBC;


/*******************************************************************************
 ** Meta-data to provide details of an SQLite backend (e.g., path to the database file)
 *******************************************************************************/
public class SQLiteBackendMetaData extends RDBMSBackendMetaData
{
   private String path;

   // todo - overrides to setters for unsupported fields?
   // todo - or - change rdbms connection manager to not require an RDBMSBackendMetaData?



   /*******************************************************************************
    ** Default Constructor.
    *******************************************************************************/
   public SQLiteBackendMetaData()
   {
      super();
      setVendor("sqlite");
      setBackendType(SQLiteBackendModule.class);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String buildConnectionString()
   {
      return "jdbc:sqlite:" + this.path;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getJdbcDriverClassName()
   {
      return (JDBC.class.getName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public SQLiteBackendMetaData withName(String name)
   {
      setName(name);
      return (this);
   }



   /*******************************************************************************
    ** Getter for path
    *******************************************************************************/
   public String getPath()
   {
      return (this.path);
   }



   /*******************************************************************************
    ** Setter for path
    *******************************************************************************/
   public void setPath(String path)
   {
      this.path = path;
   }



   /*******************************************************************************
    ** Fluent setter for path
    *******************************************************************************/
   public SQLiteBackendMetaData withPath(String path)
   {
      this.path = path;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public RDBMSActionStrategyInterface getActionStrategy()
   {
      if(getActionStrategyField() == null)
      {
         if(getActionStrategyCodeReference() != null)
         {
            setActionStrategyField(QCodeLoader.getAdHoc(RDBMSActionStrategyInterface.class, getActionStrategyCodeReference()));
         }
         else
         {
            setActionStrategyField(new SQLiteRDBMSActionStrategy());
         }
      }

      return (getActionStrategyField());
   }
}
