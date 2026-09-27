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

package com.kingsrook.qqq.backend.core.model.actions.processes;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.logging.LogPair;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** For processes that may show a review & result screen, this class provides a
 ** standard way to summarize information about the records in the process.
 **
 *******************************************************************************/
public class ProcessSummaryLine implements ProcessSummaryLineInterface
{
   private Status  status;
   private Integer count = 0;
   private String  message;

   private String singularFutureMessage;
   private String pluralFutureMessage;
   private String singularPastMessage;
   private String pluralPastMessage;
   private String messageSuffix;

   //////////////////////////////////////////////////////////////////////////
   // using ArrayList, because we need to be Serializable, and List is not //
   //////////////////////////////////////////////////////////////////////////
   private ArrayList<Serializable> primaryKeys;

   private ArrayList<String> bulletsOfText;


   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLine(Status status, Integer count, String message, ArrayList<Serializable> primaryKeys)
   {
      this.status = status;
      this.count = count;
      this.message = message;
      this.primaryKeys = primaryKeys;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLine(Status status, Integer count, String message)
   {
      this.status = status;
      this.count = count;
      this.message = message;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLine(Status status, String message)
   {
      this.status = status;
      this.message = message;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLine(Status status)
   {
      this.status = status;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return "ProcessSummaryLine{status=" + status + ", count=" + count + ", message='" + message + '\'' + '}';
   }



   /*******************************************************************************
    ** Getter for status
    **
    *******************************************************************************/
   public Status getStatus()
   {
      return status;
   }



   /*******************************************************************************
    ** Setter for status
    **
    *******************************************************************************/
   public void setStatus(Status status)
   {
      this.status = status;
   }



   /*******************************************************************************
    ** Getter for primaryKeys
    **
    *******************************************************************************/
   public List<Serializable> getPrimaryKeys()
   {
      return primaryKeys;
   }



   /*******************************************************************************
    ** Setter for primaryKeys
    **
    *******************************************************************************/
   public void setPrimaryKeys(ArrayList<Serializable> primaryKeys)
   {
      this.primaryKeys = primaryKeys;
   }



   /*******************************************************************************
    ** Getter for count
    **
    *******************************************************************************/
   public Integer getCount()
   {
      return count;
   }



   /*******************************************************************************
    ** Setter for count
    **
    *******************************************************************************/
   public void setCount(Integer count)
   {
      this.count = count;
   }



   /*******************************************************************************
    ** Getter for message
    **
    *******************************************************************************/
   @Override
   public String getMessage()
   {
      return message;
   }



   /*******************************************************************************
    ** Setter for message
    **
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void incrementCount()
   {
      incrementCount(1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void incrementCount(int amount)
   {
      if(count == null)
      {
         count = 0;
      }
      count += amount;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void incrementCountAndAddPrimaryKey(Serializable primaryKey)
   {
      incrementCount();

      if(primaryKeys == null)
      {
         primaryKeys = new ArrayList<>();
      }
      primaryKeys.add(primaryKey);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addSelfToListIfAnyCount(ArrayList<ProcessSummaryLineInterface> rs)
   {
      if(count != null && count > 0)
      {
         rs.add(this);
      }
   }



   /*******************************************************************************
    ** Getter for singularFutureMessage
    **
    *******************************************************************************/
   public String getSingularFutureMessage()
   {
      return singularFutureMessage;
   }



   /*******************************************************************************
    ** Setter for singularFutureMessage
    **
    *******************************************************************************/
   public void setSingularFutureMessage(String singularFutureMessage)
   {
      this.singularFutureMessage = singularFutureMessage;
   }



   /*******************************************************************************
    ** Fluent setter for singularFutureMessage
    **
    *******************************************************************************/
   public ProcessSummaryLine withSingularFutureMessage(String singularFutureMessage)
   {
      this.singularFutureMessage = singularFutureMessage;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pluralFutureMessage
    **
    *******************************************************************************/
   public String getPluralFutureMessage()
   {
      return pluralFutureMessage;
   }



   /*******************************************************************************
    ** Setter for pluralFutureMessage
    **
    *******************************************************************************/
   public void setPluralFutureMessage(String pluralFutureMessage)
   {
      this.pluralFutureMessage = pluralFutureMessage;
   }



   /*******************************************************************************
    ** Fluent setter for pluralFutureMessage
    **
    *******************************************************************************/
   public ProcessSummaryLine withPluralFutureMessage(String pluralFutureMessage)
   {
      this.pluralFutureMessage = pluralFutureMessage;
      return (this);
   }



   /*******************************************************************************
    ** Getter for singularPastMessage
    **
    *******************************************************************************/
   public String getSingularPastMessage()
   {
      return singularPastMessage;
   }



   /*******************************************************************************
    ** Setter for singularPastMessage
    **
    *******************************************************************************/
   public void setSingularPastMessage(String singularPastMessage)
   {
      this.singularPastMessage = singularPastMessage;
   }



   /*******************************************************************************
    ** Fluent setter for singularPastMessage
    **
    *******************************************************************************/
   public ProcessSummaryLine withSingularPastMessage(String singularPastMessage)
   {
      this.singularPastMessage = singularPastMessage;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pluralPastMessage
    **
    *******************************************************************************/
   public String getPluralPastMessage()
   {
      return pluralPastMessage;
   }



   /*******************************************************************************
    ** Setter for pluralPastMessage
    **
    *******************************************************************************/
   public void setPluralPastMessage(String pluralPastMessage)
   {
      this.pluralPastMessage = pluralPastMessage;
   }



   /*******************************************************************************
    ** Fluent setter for pluralPastMessage
    **
    *******************************************************************************/
   public ProcessSummaryLine withPluralPastMessage(String pluralPastMessage)
   {
      this.pluralPastMessage = pluralPastMessage;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void pickMessage(boolean isPast)
   {
      if(count != null)
      {
         String baseMessage;
         if(count.equals(1))
         {
            baseMessage = isPast ? getSingularPastMessage() : getSingularFutureMessage();
         }
         else
         {
            baseMessage = isPast ? getPluralPastMessage() : getPluralFutureMessage();
         }

         if(StringUtils.hasContent(baseMessage))
         {
            setMessage(baseMessage + ObjectUtils.requireConditionElse(messageSuffix, StringUtils::hasContent, ""));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void prepareForFrontend(boolean isForResultScreen)
   {
      if(!StringUtils.hasContent(getMessage()))
      {
         pickMessage(isForResultScreen);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public LogPair toLogPair()
   {
      return (logPair("ProcessSummary", logPair("status", status), logPair("count", count), logPair("message", message)));
   }



   /*******************************************************************************
    ** Getter for messageSuffix
    **
    *******************************************************************************/
   public String getMessageSuffix()
   {
      return messageSuffix;
   }



   /*******************************************************************************
    ** Setter for messageSuffix
    **
    *******************************************************************************/
   public void setMessageSuffix(String messageSuffix)
   {
      this.messageSuffix = messageSuffix;
   }



   /*******************************************************************************
    ** Fluent setter for messageSuffix
    **
    *******************************************************************************/
   public ProcessSummaryLine withMessageSuffix(String messageSuffix)
   {
      this.messageSuffix = messageSuffix;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLine withSingularMessage(String singularMessage)
   {
      singularFutureMessage = singularMessage;
      singularPastMessage = singularMessage;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLine withPluralMessage(String pluralMessage)
   {
      pluralFutureMessage = pluralMessage;
      pluralPastMessage = pluralMessage;
      return (this);
   }


   /*******************************************************************************
    ** Getter for bulletsOfText
    *******************************************************************************/
   public ArrayList<String> getBulletsOfText()
   {
      return (this.bulletsOfText);
   }



   /*******************************************************************************
    ** Setter for bulletsOfText
    *******************************************************************************/
   public void setBulletsOfText(ArrayList<String> bulletsOfText)
   {
      this.bulletsOfText = bulletsOfText;
   }



   /*******************************************************************************
    ** Fluent setter for bulletsOfText
    *******************************************************************************/
   public ProcessSummaryLine withBulletsOfText(ArrayList<String> bulletsOfText)
   {
      this.bulletsOfText = bulletsOfText;
      return (this);
   }


}
