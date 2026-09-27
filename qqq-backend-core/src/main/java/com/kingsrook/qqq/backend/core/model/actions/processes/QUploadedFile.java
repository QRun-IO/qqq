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

package com.kingsrook.qqq.backend.core.model.actions.processes;


import java.io.Serializable;


/*******************************************************************************
 ** Model a file that a user uploaded (or otherwise submitted to the qqq backend).
 *******************************************************************************/
public class QUploadedFile implements Serializable
{
   public static final String DEFAULT_UPLOADED_FILE_FIELD_NAME = "uploadedFileKey";

   private String filename;
   private byte[] bytes;



   /*******************************************************************************
    ** Getter for filename
    **
    *******************************************************************************/
   public String getFilename()
   {
      return filename;
   }



   /*******************************************************************************
    ** Setter for filename
    **
    *******************************************************************************/
   public void setFilename(String filename)
   {
      this.filename = filename;
   }



   /*******************************************************************************
    ** Getter for bytes
    **
    *******************************************************************************/
   public byte[] getBytes()
   {
      return bytes;
   }



   /*******************************************************************************
    ** Setter for bytes
    **
    *******************************************************************************/
   public void setBytes(byte[] bytes)
   {
      this.bytes = bytes;
   }
}
