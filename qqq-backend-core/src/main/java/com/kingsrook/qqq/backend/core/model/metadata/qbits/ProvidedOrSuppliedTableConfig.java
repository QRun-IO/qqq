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

package com.kingsrook.qqq.backend.core.model.metadata.qbits;


/***************************************************************************
 ** Common (maybe)? qbit config pattern, where the qbit may be able to provide
 ** a particular table, or, the application may supply it itself.
 **
 ** If the qbit provides it, then we need to be told (by the application)
 ** what backendName to use for the table.
 **
 ** Else if the application supplies it, it needs to tell the qBit what the
 ** tableName is.
 ***************************************************************************/
public class ProvidedOrSuppliedTableConfig
{
   private boolean doProvideTable;
   private String  backendName;
   private String  tableName;



   /***************************************************************************
    **
    ***************************************************************************/
   public ProvidedOrSuppliedTableConfig(boolean doProvideTable, String backendName, String tableName)
   {
      this.doProvideTable = doProvideTable;
      this.backendName = backendName;
      this.tableName = tableName;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static ProvidedOrSuppliedTableConfig provideTableUsingBackendNamed(String backendName)
   {
      return (new ProvidedOrSuppliedTableConfig(true, backendName, null));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static ProvidedOrSuppliedTableConfig useSuppliedTaleNamed(String tableName)
   {
      return (new ProvidedOrSuppliedTableConfig(false, null, tableName));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public String getEffectiveTableName(String tableNameIfProviding)
   {
      if (getDoProvideTable())
      {
         return tableNameIfProviding;
      }
      else
      {
         return getTableName();
      }
   }



   /*******************************************************************************
    ** Getter for tableName
    **
    *******************************************************************************/
   public String getTableName()
   {
      return tableName;
   }



   /*******************************************************************************
    ** Getter for doProvideTable
    **
    *******************************************************************************/
   public boolean getDoProvideTable()
   {
      return doProvideTable;
   }



   /*******************************************************************************
    ** Getter for backendName
    **
    *******************************************************************************/
   public String getBackendName()
   {
      return backendName;
   }
}
