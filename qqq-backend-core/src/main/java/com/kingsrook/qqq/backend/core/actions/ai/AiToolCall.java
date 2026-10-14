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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.util.Map;


/*******************************************************************************
 ** One call of an AI tool, as requested by a model: the provider's tool-call
 ** id, the tool's wire name, and its (parsed JSON) input object.  The input is
 ** untrusted, and is validated by the kernel before anything runs.
 *******************************************************************************/
public class AiToolCall
{
   private String              id;
   private String              name;
   private Map<String, Object> input;


   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public String getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(String id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public AiToolCall withId(String id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public AiToolCall withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for input
    *******************************************************************************/
   public Map<String, Object> getInput()
   {
      return (this.input);
   }



   /*******************************************************************************
    ** Setter for input
    *******************************************************************************/
   public void setInput(Map<String, Object> input)
   {
      this.input = input;
   }



   /*******************************************************************************
    ** Fluent setter for input
    *******************************************************************************/
   public AiToolCall withInput(Map<String, Object> input)
   {
      this.input = input;
      return (this);
   }
}
