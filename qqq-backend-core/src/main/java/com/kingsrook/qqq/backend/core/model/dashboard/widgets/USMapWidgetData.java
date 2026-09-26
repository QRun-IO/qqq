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


import java.math.BigDecimal;
import java.util.List;


/*******************************************************************************
 ** Model containing datastructure expected by frontend USA map
 **
 *******************************************************************************/
public class USMapWidgetData extends QWidgetData
{
   private String          height;
   private List<MapMarker> mapMarkerList;



   /*******************************************************************************
    **
    *******************************************************************************/
   public USMapWidgetData()
   {
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return WidgetType.USA_MAP.getType();
   }



   /*******************************************************************************
    ** Getter for height
    **
    *******************************************************************************/
   public String getHeight()
   {
      return height;
   }



   /*******************************************************************************
    ** Setter for height
    **
    *******************************************************************************/
   public void setHeight(String height)
   {
      this.height = height;
   }



   /*******************************************************************************
    ** Fluent setter for height
    **
    *******************************************************************************/
   public USMapWidgetData withHeight(String height)
   {
      this.height = height;
      return (this);
   }



   /*******************************************************************************
    ** Getter for mapMarkerList
    **
    *******************************************************************************/
   public List<MapMarker> getMapMarkerList()
   {
      return mapMarkerList;
   }



   /*******************************************************************************
    ** Setter for mapMarkerList
    **
    *******************************************************************************/
   public void setMapMarkerList(List<MapMarker> mapMarkerList)
   {
      this.mapMarkerList = mapMarkerList;
   }



   /*******************************************************************************
    ** Fluent setter for mapMarkerList
    **
    *******************************************************************************/
   public USMapWidgetData withMapMarkerList(List<MapMarker> mapMarkerList)
   {
      this.mapMarkerList = mapMarkerList;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class MapMarker
   {
      private String     name;
      private BigDecimal latitude;
      private BigDecimal longitude;



      /*******************************************************************************
       ** default constructor
       **
       *******************************************************************************/
      public MapMarker()
      {
      }



      /*******************************************************************************
       ** useful constructor
       **
       *******************************************************************************/
      public MapMarker(String name, BigDecimal latitude, BigDecimal longitude)
      {
         this.name = name;
         this.latitude = latitude;
         this.longitude = longitude;
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
      public MapMarker withName(String name)
      {
         this.name = name;
         return (this);
      }



      /*******************************************************************************
       ** Getter for latitude
       **
       *******************************************************************************/
      public BigDecimal getLatitude()
      {
         return latitude;
      }



      /*******************************************************************************
       ** Setter for latitude
       **
       *******************************************************************************/
      public void setLatitude(BigDecimal latitude)
      {
         this.latitude = latitude;
      }



      /*******************************************************************************
       ** Fluent setter for latitude
       **
       *******************************************************************************/
      public MapMarker withLatitude(BigDecimal latitude)
      {
         this.latitude = latitude;
         return (this);
      }



      /*******************************************************************************
       ** Getter for longitude
       **
       *******************************************************************************/
      public BigDecimal getLongitude()
      {
         return longitude;
      }



      /*******************************************************************************
       ** Setter for longitude
       **
       *******************************************************************************/
      public void setLongitude(BigDecimal longitude)
      {
         this.longitude = longitude;
      }



      /*******************************************************************************
       ** Fluent setter for longitude
       **
       *******************************************************************************/
      public MapMarker withLongitude(BigDecimal longitude)
      {
         this.longitude = longitude;
         return (this);
      }

   }
}
