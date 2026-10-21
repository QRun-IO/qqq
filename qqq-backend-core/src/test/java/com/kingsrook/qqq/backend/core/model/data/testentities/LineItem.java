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


import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 ** Sample of an entity that can be converted to & from a QRecord
 *******************************************************************************/
public class LineItem extends QRecordEntity
{
   public static final String TABLE_NAME = "lineItem";

   @QField()
   private String sku;

   @QField()
   private Integer quantity;



   /*******************************************************************************
    ** Getter for sku
    *******************************************************************************/
   public String getSku()
   {
      return (this.sku);
   }



   /*******************************************************************************
    ** Setter for sku
    *******************************************************************************/
   public void setSku(String sku)
   {
      this.sku = sku;
   }



   /*******************************************************************************
    ** Fluent setter for sku
    *******************************************************************************/
   public LineItem withSku(String sku)
   {
      this.sku = sku;
      return (this);
   }



   /*******************************************************************************
    ** Getter for quantity
    *******************************************************************************/
   public Integer getQuantity()
   {
      return (this.quantity);
   }



   /*******************************************************************************
    ** Setter for quantity
    *******************************************************************************/
   public void setQuantity(Integer quantity)
   {
      this.quantity = quantity;
   }



   /*******************************************************************************
    ** Fluent setter for quantity
    *******************************************************************************/
   public LineItem withQuantity(Integer quantity)
   {
      this.quantity = quantity;
      return (this);
   }

}