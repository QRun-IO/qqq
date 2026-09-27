/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.interfaces;


import java.io.InputStream;
import java.io.OutputStream;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;


/*******************************************************************************
 ** Interface for actions that a backend can perform, based on streaming data
 ** into the backend's storage.
 *******************************************************************************/
public interface QStorageInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   OutputStream createOutputStream(StorageInput storageInput) throws QException;


   /*******************************************************************************
    **
    *******************************************************************************/
   InputStream getInputStream(StorageInput storageInput) throws QException;


   /*******************************************************************************
    **
    *******************************************************************************/
   default void makePublic(StorageInput storageInput) throws QException
   {
      //////////
      // noop //
      //////////
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   default String getDownloadURL(StorageInput storageInput) throws QException
   {
      return (null);
   }

}
