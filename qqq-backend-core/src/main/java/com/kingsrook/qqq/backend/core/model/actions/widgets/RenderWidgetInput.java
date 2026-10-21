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

package com.kingsrook.qqq.backend.core.model.actions.widgets;


import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** Input data container for the RenderWidget action
 **
 *******************************************************************************/
public class RenderWidgetInput extends AbstractActionInput
{
   private InputSource              inputSource = QInputSource.SYSTEM;
   private QSession                 session;
   private QWidgetMetaDataInterface widgetMetaData;
   private Map<String, String>      queryParams = new HashMap<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public RenderWidgetInput()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getActionIdentity()
   {
      return (getClass().getSimpleName() + ":" + widgetMetaData.getName());
   }



   /*******************************************************************************
    ** Getter for inputSource
    *******************************************************************************/
   public InputSource getInputSource()
   {
      return (this.inputSource);
   }



   /*******************************************************************************
    ** Setter for inputSource
    *******************************************************************************/
   public void setInputSource(InputSource inputSource)
   {
      this.inputSource = inputSource;
   }



   /*******************************************************************************
    ** Fluent setter for inputSource
    *******************************************************************************/
   public RenderWidgetInput withInputSource(InputSource inputSource)
   {
      setInputSource(inputSource);
      return (this);
   }



   /*******************************************************************************
    ** Getter for widgetMetaData
    **
    *******************************************************************************/
   public QWidgetMetaDataInterface getWidgetMetaData()
   {
      return widgetMetaData;
   }



   /*******************************************************************************
    ** Setter for widgetMetaData
    **
    *******************************************************************************/
   public void setWidgetMetaData(QWidgetMetaDataInterface widgetMetaData)
   {
      this.widgetMetaData = widgetMetaData;
   }



   /*******************************************************************************
    ** Fluent setter for widgetMetaData
    **
    *******************************************************************************/
   public RenderWidgetInput withWidgetMetaData(QWidgetMetaDataInterface widgetMetaData)
   {
      this.widgetMetaData = widgetMetaData;
      return (this);
   }



   /*******************************************************************************
    ** Getter for urlParams
    **
    *******************************************************************************/
   public Map<String, String> getQueryParams()
   {
      return queryParams;
   }



   /*******************************************************************************
    ** Setter for urlParams
    **
    *******************************************************************************/
   public void setQueryParams(Map<String, String> queryParams)
   {
      this.queryParams = queryParams;
   }



   /*******************************************************************************
    ** Fluent setter for urlParams
    **
    *******************************************************************************/
   public RenderWidgetInput withUrlParams(Map<String, String> urlParams)
   {
      this.queryParams = urlParams;
      return (this);
   }



   /*******************************************************************************
    ** adds a query param value
    **
    *******************************************************************************/
   public void addQueryParam(String name, String value)
   {
      if(this.queryParams == null)
      {
         this.queryParams = new HashMap<>();
      }

      this.queryParams.put(name, value);
   }

}
