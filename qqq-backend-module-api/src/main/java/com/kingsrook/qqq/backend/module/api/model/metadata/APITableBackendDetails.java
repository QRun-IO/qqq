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

package com.kingsrook.qqq.backend.module.api.model.metadata;


import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;
import com.kingsrook.qqq.backend.module.api.APIBackendModule;


/*******************************************************************************
 ** Extension of QTableBackendDetails, with details specific to an API table.
 *******************************************************************************/
public class APITableBackendDetails extends QTableBackendDetails
{
   private String tablePath;
   private String tableWrapperObjectName;



   /*******************************************************************************
    ** Default Constructor.
    *******************************************************************************/
   public APITableBackendDetails()
   {
      super();
      setBackendType(APIBackendModule.class);
   }



   /*******************************************************************************
    ** Getter for tablePath
    **
    *******************************************************************************/
   public String getTablePath()
   {
      return tablePath;
   }



   /*******************************************************************************
    ** Setter for tablePath
    **
    *******************************************************************************/
   public void setTablePath(String tablePath)
   {
      this.tablePath = tablePath;
   }



   /*******************************************************************************
    ** Fluent Setter for tablePath
    **
    *******************************************************************************/
   public APITableBackendDetails withTablePath(String tablePath)
   {
      this.tablePath = tablePath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableWrapperObjectName
    **
    *******************************************************************************/
   public String getTableWrapperObjectName()
   {
      return tableWrapperObjectName;
   }



   /*******************************************************************************
    ** Setter for tableWrapperObjectName
    **
    *******************************************************************************/
   public void setTableWrapperObjectName(String tableWrapperObjectName)
   {
      this.tableWrapperObjectName = tableWrapperObjectName;
   }



   /*******************************************************************************
    ** Fluent setter for tableWrapperObjectName
    **
    *******************************************************************************/
   public APITableBackendDetails withTableWrapperObjectName(String tableWrapperObjectName)
   {
      this.tableWrapperObjectName = tableWrapperObjectName;
      return (this);
   }



   /***************************************************************************
    * finish the cloning operation started in the base class. copy all state
    * from the subclass into the input clone (which can be safely casted to
    * the subclass's type, as it was obtained by super.clone())
    ***************************************************************************/
   @Override
   protected QTableBackendDetails finishClone(QTableBackendDetails abstractClone)
   {
      APITableBackendDetails clone = (APITableBackendDetails) abstractClone;
      clone.tablePath = tablePath;
      clone.tableWrapperObjectName = tableWrapperObjectName;
      return (clone);
   }

}
