/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.model;


import java.util.List;


/*******************************************************************************
 * A process's publication to an ESB destination: which lifecycle events
 * (started, completed, failed) are published there.
 *******************************************************************************/
public class EsbProcessPublication
{
   private String                destinationName;
   private List<EsbProcessEvent> events;



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
   public EsbProcessPublication withDestinationName(String destinationName)
   {
      this.destinationName = destinationName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for events
    *******************************************************************************/
   public List<EsbProcessEvent> getEvents()
   {
      return (this.events);
   }



   /*******************************************************************************
    ** Setter for events
    *******************************************************************************/
   public void setEvents(List<EsbProcessEvent> events)
   {
      this.events = events;
   }



   /*******************************************************************************
    ** Fluent setter for events
    *******************************************************************************/
   public EsbProcessPublication withEvents(List<EsbProcessEvent> events)
   {
      this.events = events;
      return (this);
   }

}
