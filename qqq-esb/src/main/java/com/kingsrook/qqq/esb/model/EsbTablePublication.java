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

package com.kingsrook.qqq.esb.model;


import java.util.ArrayList;
import java.util.List;


/*******************************************************************************
 * A table's publication to an ESB destination: which record change events
 * (insert, update, delete) are published there.
 *******************************************************************************/
public class EsbTablePublication implements Cloneable
{
   private String              destinationName;
   private List<EsbTableEvent> events;



   /*******************************************************************************
    ** Copy this publication (including its own copy of the events list).
    *******************************************************************************/
   @Override
   public EsbTablePublication clone()
   {
      try
      {
         EsbTablePublication clone = (EsbTablePublication) super.clone();
         if(events != null)
         {
            clone.events = new ArrayList<>(events);
         }
         return (clone);
      }
      catch(CloneNotSupportedException e)
      {
         throw (new AssertionError(e));
      }
   }



   /*******************************************************************************
    ** Getter for destinationName
    *******************************************************************************/
   public String getDestinationName()
   {
      return (this.destinationName);
   }



   /*******************************************************************************
    ** Setter for destinationName
    *******************************************************************************/
   public void setDestinationName(String destinationName)
   {
      this.destinationName = destinationName;
   }



   /*******************************************************************************
    ** Fluent setter for destinationName
    *******************************************************************************/
   public EsbTablePublication withDestinationName(String destinationName)
   {
      this.destinationName = destinationName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for events
    *******************************************************************************/
   public List<EsbTableEvent> getEvents()
   {
      return (this.events);
   }



   /*******************************************************************************
    ** Setter for events
    *******************************************************************************/
   public void setEvents(List<EsbTableEvent> events)
   {
      this.events = events;
   }



   /*******************************************************************************
    ** Fluent setter for events
    *******************************************************************************/
   public EsbTablePublication withEvents(List<EsbTableEvent> events)
   {
      this.events = events;
      return (this);
   }

}
