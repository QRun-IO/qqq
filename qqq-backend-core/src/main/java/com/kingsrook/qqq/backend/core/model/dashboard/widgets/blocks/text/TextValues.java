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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.text;


import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.BlockValuesInterface;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;


/*******************************************************************************
 **
 *******************************************************************************/
public class TextValues implements BlockValuesInterface
{
   private String text;

   private QIcon startIcon;
   private QIcon endIcon;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TextValues()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TextValues(String text)
   {
      setText(text);
   }



   /*******************************************************************************
    ** Getter for text
    *******************************************************************************/
   public String getText()
   {
      return (this.text);
   }



   /*******************************************************************************
    ** Setter for text
    *******************************************************************************/
   public void setText(String text)
   {
      this.text = text;
   }



   /*******************************************************************************
    ** Fluent setter for text
    *******************************************************************************/
   public TextValues withText(String text)
   {
      this.text = text;
      return (this);
   }


   /*******************************************************************************
    ** Getter for startIcon
    *******************************************************************************/
   public QIcon getStartIcon()
   {
      return (this.startIcon);
   }



   /*******************************************************************************
    ** Setter for startIcon
    *******************************************************************************/
   public void setStartIcon(QIcon startIcon)
   {
      this.startIcon = startIcon;
   }



   /*******************************************************************************
    ** Fluent setter for startIcon
    *******************************************************************************/
   public TextValues withStartIcon(QIcon startIcon)
   {
      this.startIcon = startIcon;
      return (this);
   }



   /*******************************************************************************
    ** Getter for endIcon
    *******************************************************************************/
   public QIcon getEndIcon()
   {
      return (this.endIcon);
   }



   /*******************************************************************************
    ** Setter for endIcon
    *******************************************************************************/
   public void setEndIcon(QIcon endIcon)
   {
      this.endIcon = endIcon;
   }



   /*******************************************************************************
    ** Fluent setter for endIcon
    *******************************************************************************/
   public TextValues withEndIcon(QIcon endIcon)
   {
      this.endIcon = endIcon;
      return (this);
   }


}
