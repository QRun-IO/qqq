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

package com.kingsrook.qqq.backend.core.model.metadata.branding;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


/*******************************************************************************
 ** element of BrandingMetaData - content to send to a frontend for showing a
 ** user across the whole UI - e.g., what environment you're in, or a message
 ** about your account - site announcements, etc.  
 *******************************************************************************/
public class Banner implements Serializable, Cloneable
{
   private Severity severity;
   private String   textColor;
   private String   backgroundColor;
   private String   messageText;
   private String   messageHTML;

   private Map<String, Serializable> additionalStyles;



   /***************************************************************************
    **
    ***************************************************************************/
   public enum Severity
   {
      INFO, WARNING, ERROR, SUCCESS
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Banner clone()
   {
      try
      {
         Banner clone = (Banner) super.clone();

         //////////////////////////////////////////////////////////////////////////////////////
         // copy mutable state here, so the clone can't change the internals of the original //
         //////////////////////////////////////////////////////////////////////////////////////
         if(additionalStyles != null)
         {
            clone.setAdditionalStyles(new LinkedHashMap<>(additionalStyles));
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }



   /*******************************************************************************
    ** Getter for textColor
    *******************************************************************************/
   public String getTextColor()
   {
      return (this.textColor);
   }



   /*******************************************************************************
    ** Setter for textColor
    *******************************************************************************/
   public void setTextColor(String textColor)
   {
      this.textColor = textColor;
   }



   /*******************************************************************************
    ** Fluent setter for textColor
    *******************************************************************************/
   public Banner withTextColor(String textColor)
   {
      this.textColor = textColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for backgroundColor
    *******************************************************************************/
   public String getBackgroundColor()
   {
      return (this.backgroundColor);
   }



   /*******************************************************************************
    ** Setter for backgroundColor
    *******************************************************************************/
   public void setBackgroundColor(String backgroundColor)
   {
      this.backgroundColor = backgroundColor;
   }



   /*******************************************************************************
    ** Fluent setter for backgroundColor
    *******************************************************************************/
   public Banner withBackgroundColor(String backgroundColor)
   {
      this.backgroundColor = backgroundColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for additionalStyles
    *******************************************************************************/
   public Map<String, Serializable> getAdditionalStyles()
   {
      return (this.additionalStyles);
   }



   /*******************************************************************************
    ** Setter for additionalStyles
    *******************************************************************************/
   public void setAdditionalStyles(Map<String, Serializable> additionalStyles)
   {
      this.additionalStyles = additionalStyles;
   }



   /*******************************************************************************
    ** Fluent setter for additionalStyles
    *******************************************************************************/
   public Banner withAdditionalStyles(Map<String, Serializable> additionalStyles)
   {
      this.additionalStyles = additionalStyles;
      return (this);
   }



   /*******************************************************************************
    ** Getter for messageText
    *******************************************************************************/
   public String getMessageText()
   {
      return (this.messageText);
   }



   /*******************************************************************************
    ** Setter for messageText
    *******************************************************************************/
   public void setMessageText(String messageText)
   {
      this.messageText = messageText;
   }



   /*******************************************************************************
    ** Fluent setter for messageText
    *******************************************************************************/
   public Banner withMessageText(String messageText)
   {
      this.messageText = messageText;
      return (this);
   }



   /*******************************************************************************
    ** Getter for messageHTML
    *******************************************************************************/
   public String getMessageHTML()
   {
      return (this.messageHTML);
   }



   /*******************************************************************************
    ** Setter for messageHTML
    *******************************************************************************/
   public void setMessageHTML(String messageHTML)
   {
      this.messageHTML = messageHTML;
   }



   /*******************************************************************************
    ** Fluent setter for messageHTML
    *******************************************************************************/
   public Banner withMessageHTML(String messageHTML)
   {
      this.messageHTML = messageHTML;
      return (this);
   }



   /*******************************************************************************
    ** Getter for severity
    *******************************************************************************/
   public Severity getSeverity()
   {
      return (this.severity);
   }



   /*******************************************************************************
    ** Setter for severity
    *******************************************************************************/
   public void setSeverity(Severity severity)
   {
      this.severity = severity;
   }



   /*******************************************************************************
    ** Fluent setter for severity
    *******************************************************************************/
   public Banner withSeverity(Severity severity)
   {
      this.severity = severity;
      return (this);
   }

}
