/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


package com.kingsrook.qqq.middleware.javalin.executors;


import java.io.InputStream;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRefs;
import com.kingsrook.qqq.middleware.javalin.executors.io.UploadsInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.UploadsOutputInterface;


/*******************************************************************************
 ** Stream the request's file into the uploads storage table as the caller.
 *******************************************************************************/
public class UploadsExecutor extends AbstractMiddlewareExecutor<UploadsInput, UploadsOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(UploadsInput input, UploadsOutputInterface output) throws QException
   {
      try(InputStream content = input.getContentSupplier().open())
      {
         output.setUploadRef(UploadRefs.store(input.getFileName(), input.getContentType(), content));
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException("Could not read the upload", e));
      }
   }

}
