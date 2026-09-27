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

package com.kingsrook.qqq.backend.module.filesystem.local.model.metadata;


import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.AbstractFilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.local.FilesystemBackendModule;


/*******************************************************************************
 ** (local) Filesystem specific Extension of QTableBackendDetails
 *******************************************************************************/
public class FilesystemTableBackendDetails extends AbstractFilesystemTableBackendDetails
{

   /*******************************************************************************
    ** Default Constructor.
    *******************************************************************************/
   public FilesystemTableBackendDetails()
   {
      super();
      setBackendType(FilesystemBackendModule.class);
   }



   /***************************************************************************
    * finish the cloning process in a subclass of AbstractFilesystemTableBackendDetails
    ***************************************************************************/
   @Override
   protected FilesystemTableBackendDetails finishFilesystemSubclassClone(QTableBackendDetails cloned)
   {
      return ((FilesystemTableBackendDetails) super.finishFilesystemSubclassClone(cloned));
   }

}
