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

package com.kingsrook.qqq.backend.core.model.data.testentities;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.data.QAssociation;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 ** Sample of an entity that can be converted to & from a QRecord
 *******************************************************************************/
public class Order extends QRecordEntity
{
   public static final String TABLE_NAME = "order";

   @QField()
   private String orderNo;

   @QAssociation(name = "lineItems")
   private List<LineItem> lineItems;



   /*******************************************************************************
    ** Getter for orderNo
    *******************************************************************************/
   public String getOrderNo()
   {
      return (this.orderNo);
   }



   /*******************************************************************************
    ** Setter for orderNo
    *******************************************************************************/
   public void setOrderNo(String orderNo)
   {
      this.orderNo = orderNo;
   }



   /*******************************************************************************
    ** Fluent setter for orderNo
    *******************************************************************************/
   public Order withOrderNo(String orderNo)
   {
      this.orderNo = orderNo;
      return (this);
   }



   /*******************************************************************************
    ** Getter for lineItems
    *******************************************************************************/
   public List<LineItem> getLineItems()
   {
      return (this.lineItems);
   }



   /*******************************************************************************
    ** Setter for lineItems
    *******************************************************************************/
   public void setLineItems(List<LineItem> lineItems)
   {
      this.lineItems = lineItems;
   }



   /*******************************************************************************
    ** Fluent setter for lineItems
    *******************************************************************************/
   public Order withLineItems(List<LineItem> lineItems)
   {
      this.lineItems = lineItems;
      return (this);
   }

}