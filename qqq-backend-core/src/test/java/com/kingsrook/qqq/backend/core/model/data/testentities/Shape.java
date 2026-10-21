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


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 **
 *******************************************************************************/
public class Shape extends QRecordEntity
{
   public static final String TABLE_NAME = "shape";

   @QField()
   private Integer id;

   @QField()
   private Instant createDate;

   @QField()
   private Instant modifyDate;

   @QField()
   private String name;

   @QField()
   private String type;

   @QField()
   private Integer noOfSides;

   @QField()
   private Boolean isPolygon;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Shape()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Shape(QRecord record)
   {
      populateFromQRecord(record);
   }



   /*******************************************************************************
    ** Getter for id
    **
    *******************************************************************************/
   public Integer getId()
   {
      return id;
   }



   /*******************************************************************************
    ** Setter for id
    **
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    **
    *******************************************************************************/
   public Shape withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    **
    *******************************************************************************/
   public Shape withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for createDate
    **
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return createDate;
   }



   /*******************************************************************************
    ** Setter for createDate
    **
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }



   /*******************************************************************************
    ** Fluent setter for createDate
    **
    *******************************************************************************/
   public Shape withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for modifyDate
    **
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return modifyDate;
   }



   /*******************************************************************************
    ** Setter for modifyDate
    **
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }



   /*******************************************************************************
    ** Fluent setter for modifyDate
    **
    *******************************************************************************/
   public Shape withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return type;
   }



   /*******************************************************************************
    ** Setter for type
    **
    *******************************************************************************/
   public void setType(String type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    **
    *******************************************************************************/
   public Shape withType(String type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for noOfSides
    **
    *******************************************************************************/
   public Integer getNoOfSides()
   {
      return noOfSides;
   }



   /*******************************************************************************
    ** Setter for noOfSides
    **
    *******************************************************************************/
   public void setNoOfSides(Integer noOfSides)
   {
      this.noOfSides = noOfSides;
   }



   /*******************************************************************************
    ** Fluent setter for noOfSides
    **
    *******************************************************************************/
   public Shape withNoOfSides(Integer noOfSides)
   {
      this.noOfSides = noOfSides;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isPolygon
    **
    *******************************************************************************/
   public Boolean getIsPolygon()
   {
      return isPolygon;
   }



   /*******************************************************************************
    ** Setter for isPolygon
    **
    *******************************************************************************/
   public void setIsPolygon(Boolean isPolygon)
   {
      this.isPolygon = isPolygon;
   }



   /*******************************************************************************
    ** Fluent setter for isPolygon
    **
    *******************************************************************************/
   public Shape withIsPolygon(Boolean isPolygon)
   {
      this.isPolygon = isPolygon;
      return (this);
   }

}
