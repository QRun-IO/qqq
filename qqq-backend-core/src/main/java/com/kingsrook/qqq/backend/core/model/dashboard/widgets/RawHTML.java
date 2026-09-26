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


/*******************************************************************************
 ** Model containing datastructure expected by frontend bar raw html widget
 **
 *******************************************************************************/
public class RawHTML extends QWidgetData
{
   private String title;
   private String html;



   /*******************************************************************************
    **
    *******************************************************************************/
   public RawHTML()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RawHTML(String title, String html)
   {
      setTitle(title);
      setHtml(html);
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return WidgetType.HTML.getType();
   }



   /*******************************************************************************
    ** Getter for title
    **
    *******************************************************************************/
   public String getTitle()
   {
      return title;
   }



   /*******************************************************************************
    ** Setter for title
    **
    *******************************************************************************/
   public void setTitle(String title)
   {
      this.title = title;
   }



   /*******************************************************************************
    ** Fluent setter for title
    **
    *******************************************************************************/
   public RawHTML withTitle(String title)
   {
      this.title = title;
      return (this);
   }



   /*******************************************************************************
    ** Getter for html
    **
    *******************************************************************************/
   public String getHtml()
   {
      return html;
   }



   /*******************************************************************************
    ** Setter for html
    **
    *******************************************************************************/
   public void setHtml(String html)
   {
      this.html = html;
   }



   /*******************************************************************************
    ** Fluent setter for html
    **
    *******************************************************************************/
   public RawHTML withHtml(String html)
   {
      this.html = html;
      return (this);
   }

}
