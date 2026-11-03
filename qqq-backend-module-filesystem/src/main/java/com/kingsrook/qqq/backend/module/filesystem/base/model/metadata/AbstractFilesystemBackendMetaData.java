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

package com.kingsrook.qqq.backend.module.filesystem.base.model.metadata;


import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Base class for all BackendMetaData for all filesystem-style backend modules.
 *******************************************************************************/
public class AbstractFilesystemBackendMetaData extends QBackendMetaData
{
   private String basePath;



   /*******************************************************************************
    ** Default Constructor.
    *******************************************************************************/
   public AbstractFilesystemBackendMetaData()
   {
      super();
   }



   /*******************************************************************************
    ** Getter for basePath
    **
    *******************************************************************************/
   public String getBasePath()
   {
      return (basePath);
   }



   /*******************************************************************************
    ** Setter for basePath
    **
    *******************************************************************************/
   public void setBasePath(String basePath)
   {
      this.basePath = basePath;
   }



   /*******************************************************************************
    ** Fluent setter for basePath
    **
    *******************************************************************************/
   public AbstractFilesystemBackendMetaData withBasePath(String basePath)
   {
      this.basePath = basePath;
      return (this);
   }



   /*******************************************************************************
    ** Every table on a filesystem backend needs filesystem table backend details
    ** (cardinality, record format or contents field, glob, etc.): the actions
    ** read them unconditionally, so a table without them would fail on every
    ** query rather than at startup.
    *******************************************************************************/
   @Override
   public void performTableValidation(QInstance qInstance, QTableMetaData table, QInstanceValidator qInstanceValidator)
   {
      if(qInstanceValidator.assertCondition(table.getBackendDetails() != null, "Table " + table.getName() + " on filesystem backend " + getName() + " has no backend details: give it filesystem table backend details (cardinality, record format, glob) with withBackendDetails."))
      {
         qInstanceValidator.assertCondition(table.getBackendDetails() instanceof AbstractFilesystemTableBackendDetails, "Table " + table.getName() + " on filesystem backend " + getName() + " has backend details of type " + table.getBackendDetails().getClass().getSimpleName() + ", but filesystem table backend details are required.");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean requiresPrimaryKeyOnTables()
   {
      return (false);
   }

}
