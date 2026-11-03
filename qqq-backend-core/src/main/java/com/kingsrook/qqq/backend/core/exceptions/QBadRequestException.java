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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/*******************************************************************************
 ** User-facing exception for when user provided bad or missing data in their request
 ** - optionally naming the request fields that were bad (which a middleware can
 ** return to the client, e.g. as the UI API's fieldErrors).
 **
 *******************************************************************************/
public class QBadRequestException extends QUserFacingException
{
   private final ArrayList<FieldError> fieldErrors = new ArrayList<>();



   /*******************************************************************************
    ** An error about one field of the request.
    *******************************************************************************/
   public record FieldError(String field, String message) implements Serializable
   {
   }



   /*******************************************************************************
    ** Constructor of message
    **
    *******************************************************************************/
   public QBadRequestException(String message)
   {
      super(message);
   }



   /*******************************************************************************
    ** Constructor of message & cause
    **
    *******************************************************************************/
   public QBadRequestException(String message, Throwable cause)
   {
      super(message, cause);
   }



   /*******************************************************************************
    ** Fluent adder of an error about one field of the request.
    *******************************************************************************/
   public QBadRequestException withFieldError(String field, String message)
   {
      fieldErrors.add(new FieldError(field, message));
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldErrors (never null).
    *******************************************************************************/
   public List<FieldError> getFieldErrors()
   {
      return (fieldErrors);
   }

}
