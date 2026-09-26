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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** Model containing datastructure expected by frontend process widget
 **
 *******************************************************************************/
public class ProcessWidgetData extends QWidgetData
{
   private QProcessMetaData          processMetaData;
   private Map<String, Serializable> defaultValues;



   /*******************************************************************************
    ** Getter for processMetaData
    **
    *******************************************************************************/
   public QProcessMetaData getProcessMetaData()
   {
      return processMetaData;
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return WidgetType.PROCESS.getType();
   }



   /*******************************************************************************
    ** Setter for processMetaData
    **
    *******************************************************************************/
   public void setProcessMetaData(QProcessMetaData processMetaData)
   {
      this.processMetaData = processMetaData;
   }



   /*******************************************************************************
    ** Fluent setter for processMetaData
    **
    *******************************************************************************/
   public ProcessWidgetData withProcessMetaData(QProcessMetaData processMetaData)
   {
      this.processMetaData = processMetaData;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultValues
    **
    *******************************************************************************/
   public Map<String, Serializable> getDefaultValues()
   {
      return defaultValues;
   }



   /*******************************************************************************
    ** Setter for defaultValues
    **
    *******************************************************************************/
   public void setDefaultValues(Map<String, Serializable> defaultValues)
   {
      this.defaultValues = defaultValues;
   }



   /*******************************************************************************
    ** Fluent setter for defaultValues
    **
    *******************************************************************************/
   public ProcessWidgetData withDefaultValues(Map<String, Serializable> defaultValues)
   {
      this.defaultValues = defaultValues;
      return (this);
   }

}
