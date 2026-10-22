/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.tracing;


import java.io.Serializable;


/*******************************************************************************
 ** Basic class that can be passed in to ProcessTracerInterface.handleMessage.
 ** This class just provides for a string message.  We anticipate subclasses
 ** that may have more specific data, that specific tracer implementations may
 ** be aware of.  
 *******************************************************************************/
public class ProcessTracerMessage implements Serializable
{
   private String message;


   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ProcessTracerMessage()
   {
   }


   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ProcessTracerMessage(String message)
   {
      this.message = message;
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
   public ProcessTracerMessage withMessage(String message)
   {
      this.message = message;
      return (this);
   }

}
