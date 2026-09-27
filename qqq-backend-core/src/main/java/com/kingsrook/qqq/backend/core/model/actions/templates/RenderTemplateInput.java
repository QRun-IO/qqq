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

package com.kingsrook.qqq.backend.core.model.actions.templates;


import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.templates.TemplateType;


/*******************************************************************************
 **
 *******************************************************************************/
public class RenderTemplateInput extends AbstractActionInput
{
   private String       templateIdentifier;
   private String       code; // todo - TemplateReference, like CodeReference??
   private TemplateType templateType;

   private Map<String, ? extends Object> context;



   /*******************************************************************************
    **
    *******************************************************************************/
   public RenderTemplateInput()
   {
   }



   /*******************************************************************************
    ** Getter for code
    **
    *******************************************************************************/
   public String getCode()
   {
      return code;
   }



   /*******************************************************************************
    ** Setter for code
    **
    *******************************************************************************/
   public void setCode(String code)
   {
      this.code = code;
   }



   /*******************************************************************************
    ** Fluent setter for code
    **
    *******************************************************************************/
   public RenderTemplateInput withCode(String code)
   {
      this.code = code;
      return (this);
   }



   /*******************************************************************************
    ** Getter for templateType
    **
    *******************************************************************************/
   public TemplateType getTemplateType()
   {
      return templateType;
   }



   /*******************************************************************************
    ** Setter for templateType
    **
    *******************************************************************************/
   public void setTemplateType(TemplateType templateType)
   {
      this.templateType = templateType;
   }



   /*******************************************************************************
    ** Fluent setter for templateType
    **
    *******************************************************************************/
   public RenderTemplateInput withTemplateType(TemplateType templateType)
   {
      this.templateType = templateType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for context
    **
    *******************************************************************************/
   public Map<String, ? extends Object> getContext()
   {
      return context;
   }



   /*******************************************************************************
    ** Setter for context
    **
    *******************************************************************************/
   public void setContext(Map<String, ? extends Object> context)
   {
      this.context = context;
   }



   /*******************************************************************************
    ** Fluent setter for context
    **
    *******************************************************************************/
   public RenderTemplateInput withContext(Map<String, Object> context)
   {
      this.context = context;
      return (this);
   }



   /*******************************************************************************
    ** Getter for templateIdentifier
    *******************************************************************************/
   public String getTemplateIdentifier()
   {
      return (this.templateIdentifier);
   }



   /*******************************************************************************
    ** Setter for templateIdentifier
    *******************************************************************************/
   public void setTemplateIdentifier(String templateIdentifier)
   {
      this.templateIdentifier = templateIdentifier;
   }



   /*******************************************************************************
    ** Fluent setter for templateIdentifier
    *******************************************************************************/
   public RenderTemplateInput withTemplateIdentifier(String templateIdentifier)
   {
      this.templateIdentifier = templateIdentifier;
      return (this);
   }

}
