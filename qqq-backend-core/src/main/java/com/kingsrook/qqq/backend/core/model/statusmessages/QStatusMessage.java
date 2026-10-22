/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.statusmessages;


import java.io.Serializable;


/*******************************************************************************
 ** Abstract Base class for status messages (errors or warnings) that can be
 ** attached to QRecords.
 **
 ** They look like exceptions, but they aren't throwable, and they are meant
 ** to just be put in a record's error or warning list.  Those lists were originally
 ** just Strings, but we wanted to have some type information communicated with
 ** them, e.g., for marking an error as caused by bad-data (e.g., from a user, e.g.,
 ** for an HTTP 400) vs. a server-side error, etc.
 *******************************************************************************/
public abstract class QStatusMessage implements Serializable
{
   private String message;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QStatusMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    ** Getter for message
    **
    *******************************************************************************/
   public String getMessage()
   {
      return message;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return (message);
   }
}
