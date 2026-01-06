/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.base;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.BlockStylesInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class BaseStyles implements BlockStylesInterface
{
   private Directional<String> padding;

   private String backgroundColor;



   /***************************************************************************
    **
    ***************************************************************************/
   public static class Directional<T extends Serializable> implements Serializable
   {
      private T top;
      private T bottom;
      private T left;
      private T right;



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public Directional()
      {
      }



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public Directional(T top, T right, T bottom, T left)
      {
         this.top = top;
         this.right = right;
         this.bottom = bottom;
         this.left = left;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      public static <T extends Serializable> Directional<T> of(T top, T right, T bottom, T left)
      {
         return (new Directional<>(top, right, bottom, left));
      }




      /***************************************************************************
       **
       ***************************************************************************/
      public static <T extends Serializable> Directional<T> of(T value)
      {
         return (new Directional<>(value, value, value, value));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      public static <T extends Serializable> Directional<T> ofTop(T top)
      {
         return (new Directional<>(top, null, null, null));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      public static <T extends Serializable> Directional<T> ofRight(T right)
      {
         return (new Directional<>(null, right, null, null));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      public static <T extends Serializable> Directional<T> ofBottom(T bottom)
      {
         return (new Directional<>(null, null, bottom, null));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      public static <T extends Serializable> Directional<T> ofLeft(T left)
      {
         return (new Directional<>(null, null, null, left));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      public static <T extends Serializable> Directional<T> ofX(T x)
      {
         return (new Directional<>(null, x, null, x));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      public static <T extends Serializable> Directional<T> ofY(T y)
      {
         return (new Directional<>(y, null, y, null));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      public static <T extends Serializable> Directional<T> ofXY(T x, T y)
      {
         return (new Directional<>(y, x, y, x));
      }



      /*******************************************************************************
       ** Getter for top
       **
       *******************************************************************************/
      public T getTop()
      {
         return top;
      }



      /*******************************************************************************
       ** Setter for top
       **
       *******************************************************************************/
      public void setTop(T top)
      {
         this.top = top;
      }



      /*******************************************************************************
       ** Fluent setter for top
       **
       *******************************************************************************/
      public Directional<T> withTop(T top)
      {
         this.top = top;
         return (this);
      }



      /*******************************************************************************
       ** Getter for bottom
       **
       *******************************************************************************/
      public T getBottom()
      {
         return bottom;
      }



      /*******************************************************************************
       ** Setter for bottom
       **
       *******************************************************************************/
      public void setBottom(T bottom)
      {
         this.bottom = bottom;
      }



      /*******************************************************************************
       ** Fluent setter for bottom
       **
       *******************************************************************************/
      public Directional<T> withBottom(T bottom)
      {
         this.bottom = bottom;
         return (this);
      }



      /*******************************************************************************
       ** Getter for left
       **
       *******************************************************************************/
      public T getLeft()
      {
         return left;
      }



      /*******************************************************************************
       ** Setter for left
       **
       *******************************************************************************/
      public void setLeft(T left)
      {
         this.left = left;
      }



      /*******************************************************************************
       ** Fluent setter for left
       **
       *******************************************************************************/
      public Directional<T> withLeft(T left)
      {
         this.left = left;
         return (this);
      }



      /*******************************************************************************
       ** Getter for right
       **
       *******************************************************************************/
      public T getRight()
      {
         return right;
      }



      /*******************************************************************************
       ** Setter for right
       **
       *******************************************************************************/
      public void setRight(T right)
      {
         this.right = right;
      }



      /*******************************************************************************
       ** Fluent setter for right
       **
       *******************************************************************************/
      public Directional<T> withRight(T right)
      {
         this.right = right;
         return (this);
      }

   }



   /*******************************************************************************
    ** Getter for padding
    *******************************************************************************/
   public Directional<String> getPadding()
   {
      return (this.padding);
   }



   /*******************************************************************************
    ** Setter for padding
    *******************************************************************************/
   public void setPadding(Directional<String> padding)
   {
      this.padding = padding;
   }



   /*******************************************************************************
    ** Fluent setter for padding
    *******************************************************************************/
   public BaseStyles withPadding(Directional<String> padding)
   {
      this.padding = padding;
      return (this);
   }



   /*******************************************************************************
    ** Getter for backgroundColor
    *******************************************************************************/
   public String getBackgroundColor()
   {
      return (this.backgroundColor);
   }



   /*******************************************************************************
    ** Setter for backgroundColor
    *******************************************************************************/
   public void setBackgroundColor(String backgroundColor)
   {
      this.backgroundColor = backgroundColor;
   }



   /*******************************************************************************
    ** Fluent setter for backgroundColor
    *******************************************************************************/
   public BaseStyles withBackgroundColor(String backgroundColor)
   {
      this.backgroundColor = backgroundColor;
      return (this);
   }

}
