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

package com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.templates.RenderTemplateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.templates.RenderTemplateInput;
import com.kingsrook.qqq.backend.core.model.actions.templates.RenderTemplateOutput;
import com.kingsrook.qqq.backend.core.model.templates.TemplateType;


/*******************************************************************************
 **
 *******************************************************************************/
public class WidgetHtmlLine extends AbstractWidgetOutput
{
   private List<HtmlWrapper> wrappers;
   private String            velocityTemplate;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public WidgetHtmlLine()
   {
      setType(getClass().getSimpleName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String render(Map<String, Object> context) throws QException
   {
      RenderTemplateInput renderTemplateInput = new RenderTemplateInput();
      renderTemplateInput.setTemplateType(TemplateType.VELOCITY);
      renderTemplateInput.setCode(velocityTemplate);
      renderTemplateInput.setContext(context);

      RenderTemplateOutput renderTemplateOutput = new RenderTemplateAction().execute(renderTemplateInput);
      String               content              = renderTemplateOutput.getResult();

      if(wrappers != null)
      {
         for(int i = wrappers.size() - 1; i >= 0; i--)
         {
            content = wrappers.get(i).wrap(content);
         }
      }

      return (content);
   }



   /*******************************************************************************
    ** Getter for velocityTemplate
    *******************************************************************************/
   public String getVelocityTemplate()
   {
      return (this.velocityTemplate);
   }



   /*******************************************************************************
    ** Setter for velocityTemplate
    *******************************************************************************/
   public void setVelocityTemplate(String velocityTemplate)
   {
      this.velocityTemplate = velocityTemplate;
   }



   /*******************************************************************************
    ** Fluent setter for velocityTemplate
    *******************************************************************************/
   public WidgetHtmlLine withVelocityTemplate(String velocityTemplate)
   {
      this.velocityTemplate = velocityTemplate;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for condition
    *******************************************************************************/
   public WidgetHtmlLine withCondition(QFilterCriteria condition)
   {
      this.condition = condition;
      return (this);
   }



   /*******************************************************************************
    ** Getter for wrappers
    *******************************************************************************/
   public List<HtmlWrapper> getWrappers()
   {
      return (this.wrappers);
   }



   /*******************************************************************************
    ** Setter for wrappers
    *******************************************************************************/
   public void setWrappers(List<HtmlWrapper> wrappers)
   {
      this.wrappers = wrappers;
   }



   /*******************************************************************************
    ** Fluent setter for wrappers
    *******************************************************************************/
   public WidgetHtmlLine withWrappers(List<HtmlWrapper> wrappers)
   {
      this.wrappers = wrappers;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter to add 1 wrapper
    *******************************************************************************/
   public WidgetHtmlLine withWrapper(HtmlWrapper wrapper)
   {
      if(this.wrappers == null)
      {
         this.wrappers = new ArrayList<>();
      }
      this.wrappers.add(wrapper);
      return (this);
   }

}
