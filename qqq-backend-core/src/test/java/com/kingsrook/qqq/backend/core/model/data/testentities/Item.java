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
import java.util.List;
import com.kingsrook.qqq.backend.core.model.data.QAssociation;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.data.QVirtualField;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;


/*******************************************************************************
 ** Sample of an entity that can be converted to & from a QRecord
 *******************************************************************************/
public class Item extends QRecordEntity
{
   public static final String TABLE_NAME = "item";

   public static final String ASSOCIATION_ITEM_ALTERNATES_NAME = "itemAlternates";

   @QField(isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, label = "SKU")
   private String sku;

   @QField()
   private String description;

   @QField(isEditable = false, displayFormat = DisplayFormat.COMMAS)
   private Integer quantity;

   @QField()
   private BigDecimal price;

   @QField(backendName = "is_featured")
   private Boolean featured;

   @QVirtualField
   private String displayName;

   @QAssociation(name = ASSOCIATION_ITEM_ALTERNATES_NAME)
   private List<Item> itemAlternates;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Item()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Item(QRecord qRecord)
   {
      populateFromQRecord(qRecord);
   }



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
   public Integer getQuantity()
   {
      return quantity;
   }



   /*******************************************************************************
    ** Setter for quantity
    **
    *******************************************************************************/
   public void setQuantity(Integer quantity)
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
   public Boolean getFeatured()
   {
      return featured;
   }



   /*******************************************************************************
    ** Setter for featured
    **
    *******************************************************************************/
   public void setFeatured(Boolean featured)
   {
      this.featured = featured;
   }



   /*******************************************************************************
    ** Fluent setter for sku
    *******************************************************************************/
   public Item withSku(String sku)
   {
      this.sku = sku;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public Item withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for quantity
    *******************************************************************************/
   public Item withQuantity(Integer quantity)
   {
      this.quantity = quantity;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for price
    *******************************************************************************/
   public Item withPrice(BigDecimal price)
   {
      this.price = price;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for featured
    *******************************************************************************/
   public Item withFeatured(Boolean featured)
   {
      this.featured = featured;
      return (this);
   }



   /*******************************************************************************
    ** Getter for displayName
    *******************************************************************************/
   public String getDisplayName()
   {
      return (this.displayName);
   }



   /*******************************************************************************
    ** Setter for displayName
    *******************************************************************************/
   public void setDisplayName(String displayName)
   {
      this.displayName = displayName;
   }



   /*******************************************************************************
    ** Fluent setter for displayName
    *******************************************************************************/
   public Item withDisplayName(String displayName)
   {
      this.displayName = displayName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for itemAlternates
    *******************************************************************************/
   public List<Item> getItemAlternates()
   {
      return (this.itemAlternates);
   }



   /*******************************************************************************
    ** Setter for itemAlternates
    *******************************************************************************/
   public void setItemAlternates(List<Item> itemAlternates)
   {
      this.itemAlternates = itemAlternates;
   }



   /*******************************************************************************
    ** Fluent setter for itemAlternates
    *******************************************************************************/
   public Item withItemAlternates(List<Item> itemAlternates)
   {
      this.itemAlternates = itemAlternates;
      return (this);
   }


   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public Item withId(Integer id)
   {
      this.id = id;
      return (this);
   }


}
