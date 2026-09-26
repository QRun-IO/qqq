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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets;


import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;


/*******************************************************************************
 **
 *******************************************************************************/
public class ChartSubheaderData
{
   private Number  mainNumber;
   private Number  vsPreviousPercent;
   private Number  vsPreviousNumber;
   private Boolean isUpVsPrevious;
   private Boolean isGoodVsPrevious;
   private String  vsDescription = "vs prev period";

   private String mainNumberUrl;
   private String previousNumberUrl;



   /*******************************************************************************
    ** Getter for mainNumber
    *******************************************************************************/
   public Number getMainNumber()
   {
      return (this.mainNumber);
   }



   /*******************************************************************************
    ** Setter for mainNumber
    *******************************************************************************/
   public void setMainNumber(Number mainNumber)
   {
      this.mainNumber = mainNumber;
   }



   /*******************************************************************************
    ** Fluent setter for mainNumber
    *******************************************************************************/
   public ChartSubheaderData withMainNumber(Number mainNumber)
   {
      this.mainNumber = mainNumber;
      return (this);
   }



   /*******************************************************************************
    ** Getter for vsPreviousNumber
    *******************************************************************************/
   public Number getVsPreviousNumber()
   {
      return (this.vsPreviousNumber);
   }



   /*******************************************************************************
    ** Setter for vsPreviousNumber
    *******************************************************************************/
   public void setVsPreviousNumber(Number vsPreviousNumber)
   {
      this.vsPreviousNumber = vsPreviousNumber;
   }



   /*******************************************************************************
    ** Fluent setter for vsPreviousNumber
    *******************************************************************************/
   public ChartSubheaderData withVsPreviousNumber(Number vsPreviousNumber)
   {
      this.vsPreviousNumber = vsPreviousNumber;
      return (this);
   }



   /*******************************************************************************
    ** Getter for vsDescription
    *******************************************************************************/
   public String getVsDescription()
   {
      return (this.vsDescription);
   }



   /*******************************************************************************
    ** Setter for vsDescription
    *******************************************************************************/
   public void setVsDescription(String vsDescription)
   {
      this.vsDescription = vsDescription;
   }



   /*******************************************************************************
    ** Fluent setter for vsDescription
    *******************************************************************************/
   public ChartSubheaderData withVsDescription(String vsDescription)
   {
      this.vsDescription = vsDescription;
      return (this);
   }



   /*******************************************************************************
    ** Getter for vsPreviousPercent
    *******************************************************************************/
   public Number getVsPreviousPercent()
   {
      return (this.vsPreviousPercent);
   }



   /*******************************************************************************
    ** Setter for vsPreviousPercent
    *******************************************************************************/
   public void setVsPreviousPercent(Number vsPreviousPercent)
   {
      this.vsPreviousPercent = vsPreviousPercent;
   }



   /*******************************************************************************
    ** Fluent setter for vsPreviousPercent
    *******************************************************************************/
   public ChartSubheaderData withVsPreviousPercent(Number vsPreviousPercent)
   {
      this.vsPreviousPercent = vsPreviousPercent;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isUpVsPrevious
    *******************************************************************************/
   public Boolean getIsUpVsPrevious()
   {
      return (this.isUpVsPrevious);
   }



   /*******************************************************************************
    ** Setter for isUpVsPrevious
    *******************************************************************************/
   public void setIsUpVsPrevious(Boolean isUpVsPrevious)
   {
      this.isUpVsPrevious = isUpVsPrevious;
   }



   /*******************************************************************************
    ** Fluent setter for isUpVsPrevious
    *******************************************************************************/
   public ChartSubheaderData withIsUpVsPrevious(Boolean isUpVsPrevious)
   {
      this.isUpVsPrevious = isUpVsPrevious;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isGoodVsPrevious
    *******************************************************************************/
   public Boolean getIsGoodVsPrevious()
   {
      return (this.isGoodVsPrevious);
   }



   /*******************************************************************************
    ** Setter for isGoodVsPrevious
    *******************************************************************************/
   public void setIsGoodVsPrevious(Boolean isGoodVsPrevious)
   {
      this.isGoodVsPrevious = isGoodVsPrevious;
   }



   /*******************************************************************************
    ** Fluent setter for isGoodVsPrevious
    *******************************************************************************/
   public ChartSubheaderData withIsGoodVsPrevious(Boolean isGoodVsPrevious)
   {
      this.isGoodVsPrevious = isGoodVsPrevious;
      return (this);
   }



   /*******************************************************************************
    ** Getter for mainNumberUrl
    *******************************************************************************/
   public String getMainNumberUrl()
   {
      return (this.mainNumberUrl);
   }



   /*******************************************************************************
    ** Setter for mainNumberUrl
    *******************************************************************************/
   public void setMainNumberUrl(String mainNumberUrl)
   {
      this.mainNumberUrl = mainNumberUrl;
   }



   /*******************************************************************************
    ** Fluent setter for mainNumberUrl
    *******************************************************************************/
   public ChartSubheaderData withMainNumberUrl(String mainNumberUrl)
   {
      this.mainNumberUrl = mainNumberUrl;
      return (this);
   }



   /*******************************************************************************
    ** Getter for previousNumberUrl
    *******************************************************************************/
   public String getPreviousNumberUrl()
   {
      return (this.previousNumberUrl);
   }



   /*******************************************************************************
    ** Setter for previousNumberUrl
    *******************************************************************************/
   public void setPreviousNumberUrl(String previousNumberUrl)
   {
      this.previousNumberUrl = previousNumberUrl;
   }



   /*******************************************************************************
    ** Fluent setter for previousNumberUrl
    *******************************************************************************/
   public ChartSubheaderData withPreviousNumberUrl(String previousNumberUrl)
   {
      this.previousNumberUrl = previousNumberUrl;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void calculatePercentsEtc(boolean isUpGood)
   {
      if(mainNumber != null && vsPreviousNumber != null && vsPreviousNumber.doubleValue() > 0)
      {
         /////////////////////////////////////////////////////////////////
         // these are the results we're going for:                      //
         // current: 10, previous: 20 = -50%                            //
         // current: 15, previous: 20 = -25%                            //
         // current: 20, previous: 10 = +100%                           //
         // current: 15, previous: 10 = +50%                            //
         // this formula gets us that:  (current - previous) / previous //
         // (with a *100 in there to make it a percent-looking value)   //
         /////////////////////////////////////////////////////////////////
         BigDecimal current    = new BigDecimal(String.valueOf(mainNumber));
         BigDecimal previous   = new BigDecimal(String.valueOf(vsPreviousNumber));
         BigDecimal difference = current.subtract(previous);
         BigDecimal ratio      = difference.divide(previous, new MathContext(3, RoundingMode.HALF_UP));
         BigDecimal percentBD  = ratio.multiply(new BigDecimal(100));
         BigDecimal percent    = percentBD.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO.subtract(percentBD) : percentBD;
         if(mainNumber.doubleValue() < vsPreviousNumber.doubleValue())
         {
            setIsUpVsPrevious(false);
            setIsGoodVsPrevious(isUpGood ? false : true);
            setVsPreviousPercent(percent);
         }
         else // note - equal is being considered here in the good.
         {
            setIsUpVsPrevious(true);
            setIsGoodVsPrevious(isUpGood ? true : false);
            setVsPreviousPercent(percent);
         }
      }
   }
}
