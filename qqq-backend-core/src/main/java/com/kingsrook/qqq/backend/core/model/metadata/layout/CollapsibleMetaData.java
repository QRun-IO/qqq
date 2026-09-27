/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.layout;


import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;


/*******************************************************************************
 * meta-data class to represent collapsible sections in a layout.
 *
 * <p>Used to define if an element (e.g., a table section or a widget) is
 * collapsible, and if so, what it's default state is (opened or closed).</p>
 *******************************************************************************/
public class CollapsibleMetaData implements QMetaDataObject, Cloneable
{
   private final boolean isCollapsible;
   private final boolean initiallyOpen;

   ///////////////////////////////////////////////////////////////////////////////////////////////
   // define the 3 commonly used states (the 4th combination (false, false) doesn't make sense) //
   ///////////////////////////////////////////////////////////////////////////////////////////////
   public static CollapsibleMetaData NOT_COLLAPSIBLE  = new CollapsibleMetaData(false, true);
   public static CollapsibleMetaData INITIALLY_OPEN   = new CollapsibleMetaData(true, true);
   public static CollapsibleMetaData INITIALLY_CLOSED = new CollapsibleMetaData(true, false);



   /***************************************************************************
    *
    ***************************************************************************/
   public CollapsibleMetaData(boolean isCollapsible, boolean initiallyOpen)
   {
      this.isCollapsible = isCollapsible;
      this.initiallyOpen = initiallyOpen;
   }



   /*******************************************************************************
    * Getter for isCollapsible
    *
    * <p>Specifies if the element is collapsible or not.  Null should be interpreted
    * as false (not collapsible).</p>
    *******************************************************************************/
   public boolean getIsCollapsible()
   {
      return isCollapsible;
   }



   /*******************************************************************************
    * Getter for initiallyOpen
    *
    * <p>Specifies if the element is initially open or not.  Null should be interpreted
    * as false (not open).</p>
    *******************************************************************************/
   public boolean getInitiallyOpen()
   {
      return initiallyOpen;
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public CollapsibleMetaData clone()
   {
      try
      {
         CollapsibleMetaData clone = (CollapsibleMetaData) super.clone();
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }
}

