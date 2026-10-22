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


import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;


/*******************************************************************************
 **
 *******************************************************************************/
public abstract class AbstractWidgetOutput
{
   protected QFilterCriteria condition;
   protected String          type;



   /*******************************************************************************
    **
    *******************************************************************************/
   public abstract String render(Map<String, Object> context) throws QException;



   /*******************************************************************************
    ** Getter for condition
    *******************************************************************************/
   public QFilterCriteria getCondition()
   {
      return (this.condition);
   }



   /*******************************************************************************
    ** Setter for condition
    *******************************************************************************/
   public void setCondition(QFilterCriteria condition)
   {
      this.condition = condition;
   }



   /*******************************************************************************
    ** Fluent setter for condition
    *******************************************************************************/
   public AbstractWidgetOutput withCondition(QFilterCriteria condition)
   {
      this.condition = condition;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public String getType()
   {
      return (this.type);
   }



   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   public void setType(String type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    *******************************************************************************/
   public AbstractWidgetOutput withType(String type)
   {
      this.type = type;
      return (this);
   }

}
