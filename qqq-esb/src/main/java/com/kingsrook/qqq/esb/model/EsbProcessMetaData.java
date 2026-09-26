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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QSupplementalProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 * Process-level ESB meta-data: the destinations a process publishes its
 * lifecycle events to, and the triggers (queues and topics) that run it.
 *
 * Not included in frontend meta-data; the UI reads it through the
 * permission-checked ESB endpoints.
 *******************************************************************************/
public class EsbProcessMetaData extends QSupplementalProcessMetaData
{
   public static final String TYPE = "esb";

   private List<EsbProcessPublication> publications;
   private List<EsbTrigger>            triggers;



   /*******************************************************************************
    ** Get the ESB meta-data on a process - or null if it has none.
    *******************************************************************************/
   public static EsbProcessMetaData of(QProcessMetaData process)
   {
      return ((EsbProcessMetaData) process.getSupplementalMetaData(TYPE));
   }



   /*******************************************************************************
    ** Get the ESB meta-data on a process - creating (and adding) it if the
    ** process doesn't have any yet.
    *******************************************************************************/
   public static EsbProcessMetaData ofOrWithNew(QProcessMetaData process)
   {
      EsbProcessMetaData esbProcessMetaData = of(process);
      if(esbProcessMetaData == null)
      {
         esbProcessMetaData = new EsbProcessMetaData();
         process.withSupplementalMetaData(esbProcessMetaData);
      }
      return (esbProcessMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getType()
   {
      return (TYPE);
   }



   /*******************************************************************************
    ** Each publication must be to a known destination; each trigger must be
    ** valid, and there can be only one trigger per destination.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QProcessMetaData process, QInstanceValidator qInstanceValidator)
   {
      super.validate(qInstance, process, qInstanceValidator);

      EsbInstanceMetaData esbInstanceMetaData = EsbInstanceMetaData.ofOrNull(qInstance);
      for(EsbProcessPublication publication : CollectionUtils.nonNullList(publications))
      {
         boolean destinationExists = esbInstanceMetaData != null && esbInstanceMetaData.getDestination(publication.getDestinationName()) != null;
         qInstanceValidator.assertCondition(destinationExists, "ESB publication on process " + process.getName() + " references an unknown destination: " + publication.getDestinationName() + ".");
      }

      Set<String> triggerDestinationNames = new HashSet<>();
      for(EsbTrigger trigger : CollectionUtils.nonNullList(triggers))
      {
         trigger.validate(process.getName(), esbInstanceMetaData, qInstanceValidator);
         qInstanceValidator.assertCondition(triggerDestinationNames.add(trigger.getDestinationName()), "Process " + process.getName() + " has more than one ESB trigger on destination " + trigger.getDestinationName() + ".");
      }
   }



   /*******************************************************************************
    ** Getter for publications
    *******************************************************************************/
   public List<EsbProcessPublication> getPublications()
   {
      return (this.publications);
   }



   /*******************************************************************************
    ** Setter for publications
    *******************************************************************************/
   public void setPublications(List<EsbProcessPublication> publications)
   {
      this.publications = publications;
   }



   /*******************************************************************************
    ** Fluent setter for publications
    *******************************************************************************/
   public EsbProcessMetaData withPublications(List<EsbProcessPublication> publications)
   {
      this.publications = publications;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter to add a single publication
    *******************************************************************************/
   public EsbProcessMetaData withPublication(EsbProcessPublication publication)
   {
      if(this.publications == null)
      {
         this.publications = new ArrayList<>();
      }
      this.publications.add(publication);
      return (this);
   }



   /*******************************************************************************
    ** Getter for triggers
    *******************************************************************************/
   public List<EsbTrigger> getTriggers()
   {
      return (this.triggers);
   }



   /*******************************************************************************
    ** Setter for triggers
    *******************************************************************************/
   public void setTriggers(List<EsbTrigger> triggers)
   {
      this.triggers = triggers;
   }



   /*******************************************************************************
    ** Fluent setter for triggers
    *******************************************************************************/
   public EsbProcessMetaData withTriggers(List<EsbTrigger> triggers)
   {
      this.triggers = triggers;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter to add a single trigger
    *******************************************************************************/
   public EsbProcessMetaData withTrigger(EsbTrigger trigger)
   {
      if(this.triggers == null)
      {
         this.triggers = new ArrayList<>();
      }
      this.triggers.add(trigger);
      return (this);
   }

}
