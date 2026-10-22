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

package com.kingsrook.qqq.backend.core.exceptions;


/*******************************************************************************
 ** Exception thrown while executing custom code in QQQ.
 **
 ** Context field is meant to give the user "context" for where the error occurred
 ** - e.g., a line number or word that was bad.
 *******************************************************************************/
public class QCodeException extends QException
{
   private String context;



   /*******************************************************************************
    ** Constructor of message
    **
    *******************************************************************************/
   public QCodeException(String message)
   {
      super(message);
   }



   /*******************************************************************************
    ** Constructor of message & cause
    **
    *******************************************************************************/
   public QCodeException(String message, Throwable cause)
   {
      super(message, cause);
   }



   /*******************************************************************************
    ** Getter for context
    **
    *******************************************************************************/
   public String getContext()
   {
      return context;
   }



   /*******************************************************************************
    ** Setter for context
    **
    *******************************************************************************/
   public void setContext(String context)
   {
      this.context = context;
   }

}
