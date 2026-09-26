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

package com.kingsrook.qqq.backend.core.logging;


import org.apache.logging.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;


/*******************************************************************************
 ** A log message, which can be "collected" by the QCollectingLogger.
 *******************************************************************************/
public class CollectedLogMessage
{
   private Level     level;
   private String    message;
   private Throwable exception;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public CollectedLogMessage()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return "CollectedLogMessage{level=" + level + ", message='" + message + '\'' + ", exception=" + exception + '}';
   }



   /*******************************************************************************
    ** Getter for message
    *******************************************************************************/
   public String getMessage()
   {
      return (this.message);
   }



   /*******************************************************************************
    ** Setter for message
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    ** Fluent setter for message
    *******************************************************************************/
   public CollectedLogMessage withMessage(String message)
   {
      this.message = message;
      return (this);
   }



   /*******************************************************************************
    ** Getter for exception
    *******************************************************************************/
   public Throwable getException()
   {
      return (this.exception);
   }



   /*******************************************************************************
    ** Setter for exception
    *******************************************************************************/
   public void setException(Throwable exception)
   {
      this.exception = exception;
   }



   /*******************************************************************************
    ** Fluent setter for exception
    *******************************************************************************/
   public CollectedLogMessage withException(Throwable exception)
   {
      this.exception = exception;
      return (this);
   }



   /*******************************************************************************
    ** Getter for level
    **
    *******************************************************************************/
   public Level getLevel()
   {
      return level;
   }



   /*******************************************************************************
    ** Setter for level
    **
    *******************************************************************************/
   public void setLevel(Level level)
   {
      this.level = level;
   }



   /*******************************************************************************
    ** Fluent setter for level
    **
    *******************************************************************************/
   public CollectedLogMessage withLevel(Level level)
   {
      this.level = level;
      return (this);
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   public JSONObject getMessageAsJSONObject() throws JSONException
   {
      return (new JSONObject(getMessage()));
   }
}
