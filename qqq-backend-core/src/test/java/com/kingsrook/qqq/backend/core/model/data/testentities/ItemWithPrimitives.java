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


import java.math.BigDecimal;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 ** Sample of an entity that can be converted to & from a QRecord
 *******************************************************************************/
public class ItemWithPrimitives extends QRecordEntity
{
   public static final String TABLE_NAME = "item";

   @QField()
   private String sku;
  
   @QField()
   private String description;

   @QField()
   private int quantity;

   @QField()
   private BigDecimal price;

   @QField()
   private boolean featured;



   /*******************************************************************************
    ** Getter for sku
    **
    *******************************************************************************/
   public String getSku()
   {
      return sku;
   }



   /*******************************************************************************
    ** Setter for sku
    **
    *******************************************************************************/
   public void setSku(String sku)
   {
      this.sku = sku;
   }



   /*******************************************************************************
    ** Getter for description
    **
    *******************************************************************************/
   public String getDescription()
   {
      return description;
   }



   /*******************************************************************************
    ** Setter for description
    **
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** Getter for quantity
    **
    *******************************************************************************/
   public int getQuantity()
   {
      return quantity;
   }



   /*******************************************************************************
    ** Setter for quantity
    **
    *******************************************************************************/
   public void setQuantity(int quantity)
   {
      this.quantity = quantity;
   }



   /*******************************************************************************
    ** Getter for price
    **
    *******************************************************************************/
   public BigDecimal getPrice()
   {
      return price;
   }



   /*******************************************************************************
    ** Setter for price
    **
    *******************************************************************************/
   public void setPrice(BigDecimal price)
   {
      this.price = price;
   }



   /*******************************************************************************
    ** Getter for featured
    **
    *******************************************************************************/
   public boolean getFeatured()
   {
      return featured;
   }



   /*******************************************************************************
    ** Setter for featured
    **
    *******************************************************************************/
   public void setFeatured(boolean featured)
   {
      this.featured = featured;
   }
}
