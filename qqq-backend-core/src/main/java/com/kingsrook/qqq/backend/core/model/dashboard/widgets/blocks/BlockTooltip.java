/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks;


import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CompositeWidgetData;


/*******************************************************************************
 ** A tooltip used within a (widget) block.
 **
 *******************************************************************************/
public class BlockTooltip
{
   private CompositeWidgetData blockData;
   private String              title;
   private Placement           placement = Placement.BOTTOM;



   /***************************************************************************
    **
    ***************************************************************************/
   public enum Placement
   {BOTTOM, LEFT, RIGHT, TOP}



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BlockTooltip()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BlockTooltip(String title)
   {
      this.title = title;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BlockTooltip(CompositeWidgetData blockData)
   {
      this.blockData = blockData;
   }



   /*******************************************************************************
    ** Getter for title
    *******************************************************************************/
   public String getTitle()
   {
      return (this.title);
   }



   /*******************************************************************************
    ** Setter for title
    *******************************************************************************/
   public void setTitle(String title)
   {
      this.title = title;
   }



   /*******************************************************************************
    ** Fluent setter for title
    *******************************************************************************/
   public BlockTooltip withTitle(String title)
   {
      this.title = title;
      return (this);
   }



   /*******************************************************************************
    ** Getter for placement
    *******************************************************************************/
   public Placement getPlacement()
   {
      return (this.placement);
   }



   /*******************************************************************************
    ** Setter for placement
    *******************************************************************************/
   public void setPlacement(Placement placement)
   {
      this.placement = placement;
   }



   /*******************************************************************************
    ** Fluent setter for placement
    *******************************************************************************/
   public BlockTooltip withPlacement(Placement placement)
   {
      this.placement = placement;
      return (this);
   }



   /*******************************************************************************
    ** Getter for blockData
    *******************************************************************************/
   public CompositeWidgetData getBlockData()
   {
      return (this.blockData);
   }



   /*******************************************************************************
    ** Setter for blockData
    *******************************************************************************/
   public void setBlockData(CompositeWidgetData blockData)
   {
      this.blockData = blockData;
   }



   /*******************************************************************************
    ** Fluent setter for blockData
    *******************************************************************************/
   public BlockTooltip withBlockData(CompositeWidgetData blockData)
   {
      this.blockData = blockData;
      return (this);
   }

}
