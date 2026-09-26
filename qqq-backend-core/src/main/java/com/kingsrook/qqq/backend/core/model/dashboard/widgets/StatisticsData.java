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


/*******************************************************************************
 ** Model containing datastructure expected by frontend statistics widget
 **
 *******************************************************************************/
public class StatisticsData extends QWidgetData
{
   private Serializable count;
   private String       countFontSize;
   private String       countURL;
   private String       countContext;
   private Number       percentageAmount;
   private String       percentageLabel;
   private String       percentageURL;
   private boolean      isCurrency     = false;
   private boolean      increaseIsGood = true;



   /*******************************************************************************
    **
    *******************************************************************************/
   public StatisticsData()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public StatisticsData(Serializable count, Number percentageAmount, String percentageLabel)
   {
      this.count = count;
      this.percentageLabel = percentageLabel;
      this.percentageAmount = percentageAmount;
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return "statistics";
   }



   /*******************************************************************************
    ** Getter for countURL
    **
    *******************************************************************************/
   public String getCountURL()
   {
      return countURL;
   }



   /*******************************************************************************
    ** Setter for countURL
    **
    *******************************************************************************/
   public void setCountURL(String countURL)
   {
      this.countURL = countURL;
   }



   /*******************************************************************************
    ** Fluent setter for countURL
    **
    *******************************************************************************/
   public StatisticsData withCountURL(String countURL)
   {
      this.countURL = countURL;
      return (this);
   }



   /*******************************************************************************
    ** Getter for countFontSize
    **
    *******************************************************************************/
   public String getCountFontSize()
   {
      return countFontSize;
   }



   /*******************************************************************************
    ** Setter for countFontSize
    **
    *******************************************************************************/
   public void setCountFontSize(String countFontSize)
   {
      this.countFontSize = countFontSize;
   }



   /*******************************************************************************
    ** Fluent setter for countFontSize
    **
    *******************************************************************************/
   public StatisticsData withCountFontSize(String countFontSize)
   {
      this.countFontSize = countFontSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for count
    **
    *******************************************************************************/
   public Serializable getCount()
   {
      return count;
   }



   /*******************************************************************************
    ** Setter for count
    **
    *******************************************************************************/
   public void setCount(Serializable count)
   {
      this.count = count;
   }



   /*******************************************************************************
    ** Fluent setter for count
    **
    *******************************************************************************/
   public StatisticsData withCount(Serializable count)
   {
      this.count = count;
      return (this);
   }



   /*******************************************************************************
    ** Getter for percentageAmount
    **
    *******************************************************************************/
   public Number getPercentageAmount()
   {
      return percentageAmount;
   }



   /*******************************************************************************
    ** Setter for percentageAmount
    **
    *******************************************************************************/
   public void setPercentageAmount(Number percentageAmount)
   {
      this.percentageAmount = percentageAmount;
   }



   /*******************************************************************************
    ** Fluent setter for percentageAmount
    **
    *******************************************************************************/
   public StatisticsData withPercentageAmount(Number percentageAmount)
   {
      this.percentageAmount = percentageAmount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for percentageLabel
    **
    *******************************************************************************/
   public String getPercentageLabel()
   {
      return percentageLabel;
   }



   /*******************************************************************************
    ** Setter for percentageLabel
    **
    *******************************************************************************/
   public void setPercentageLabel(String percentageLabel)
   {
      this.percentageLabel = percentageLabel;
   }



   /*******************************************************************************
    ** Fluent setter for percentageLabel
    **
    *******************************************************************************/
   public StatisticsData withPercentageLabel(String percentageLabel)
   {
      this.percentageLabel = percentageLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isCurrency
    **
    *******************************************************************************/
   public boolean getIsCurrency()
   {
      return isCurrency;
   }



   /*******************************************************************************
    ** Setter for isCurrency
    **
    *******************************************************************************/
   public void setIsCurrency(boolean isCurrency)
   {
      this.isCurrency = isCurrency;
   }



   /*******************************************************************************
    ** Fluent setter for isCurrency
    **
    *******************************************************************************/
   public StatisticsData withIsCurrency(boolean isCurrency)
   {
      this.isCurrency = isCurrency;
      return (this);
   }



   /*******************************************************************************
    ** Getter for increaseIsGood
    **
    *******************************************************************************/
   public boolean getIncreaseIsGood()
   {
      return increaseIsGood;
   }



   /*******************************************************************************
    ** Setter for increaseIsGood
    **
    *******************************************************************************/
   public void setIncreaseIsGood(boolean increaseIsGood)
   {
      this.increaseIsGood = increaseIsGood;
   }



   /*******************************************************************************
    ** Fluent setter for increaseIsGood
    **
    *******************************************************************************/
   public StatisticsData withIncreaseIsGood(boolean increaseIsGood)
   {
      this.increaseIsGood = increaseIsGood;
      return (this);
   }



   /*******************************************************************************
    ** Getter for countContext
    *******************************************************************************/
   public String getCountContext()
   {
      return (this.countContext);
   }



   /*******************************************************************************
    ** Setter for countContext
    *******************************************************************************/
   public void setCountContext(String countContext)
   {
      this.countContext = countContext;
   }



   /*******************************************************************************
    ** Fluent setter for countContext
    *******************************************************************************/
   public StatisticsData withCountContext(String countContext)
   {
      this.countContext = countContext;
      return (this);
   }



   /*******************************************************************************
    ** Getter for percentageURL
    *******************************************************************************/
   public String getPercentageURL()
   {
      return (this.percentageURL);
   }



   /*******************************************************************************
    ** Setter for percentageURL
    *******************************************************************************/
   public void setPercentageURL(String percentageURL)
   {
      this.percentageURL = percentageURL;
   }



   /*******************************************************************************
    ** Fluent setter for percentageURL
    *******************************************************************************/
   public StatisticsData withPercentageURL(String percentageURL)
   {
      this.percentageURL = percentageURL;
      return (this);
   }

}
