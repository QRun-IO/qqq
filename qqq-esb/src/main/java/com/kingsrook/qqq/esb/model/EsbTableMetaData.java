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
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QSupplementalTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 * Table-level ESB meta-data: the destinations a table publishes its record
 * change events to.
 *
 * Not included in frontend meta-data; the UI reads it through the
 * permission-checked ESB endpoints.
 *******************************************************************************/
public class EsbTableMetaData extends QSupplementalTableMetaData
{
   public static final String TYPE = "esb";

   private List<EsbTablePublication> publications;



   /*******************************************************************************
    ** Get the ESB meta-data on a table - or null if it has none.
    *******************************************************************************/
   public static EsbTableMetaData of(QTableMetaData table)
   {
      return (QSupplementalTableMetaData.of(table, TYPE));
   }



   /*******************************************************************************
    ** Get the ESB meta-data on a table - creating (and adding) it if the table
    ** doesn't have any yet.
    *******************************************************************************/
   public static EsbTableMetaData ofOrWithNew(QTableMetaData table)
   {
      return (QSupplementalTableMetaData.ofOrWithNew(table, TYPE, EsbTableMetaData::new));
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
    **
    *******************************************************************************/
   @Override
   public boolean includeInPartialFrontendMetaData()
   {
      return (false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean includeInFullFrontendMetaData()
   {
      return (false);
   }



   /*******************************************************************************
    ** Each publication must be to a known destination.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QTableMetaData tableMetaData, QInstanceValidator qInstanceValidator)
   {
      super.validate(qInstance, tableMetaData, qInstanceValidator);

      EsbInstanceMetaData esbInstanceMetaData = EsbInstanceMetaData.ofOrNull(qInstance);
      for(EsbTablePublication publication : CollectionUtils.nonNullList(publications))
      {
         boolean destinationExists = esbInstanceMetaData != null && esbInstanceMetaData.getDestination(publication.getDestinationName()) != null;
         qInstanceValidator.assertCondition(destinationExists, "ESB publication on table " + tableMetaData.getName() + " references an unknown destination: " + publication.getDestinationName() + ".");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected EsbTableMetaData finishClone(QSupplementalTableMetaData abstractClone)
   {
      EsbTableMetaData clone = (EsbTableMetaData) abstractClone;
      if(publications != null)
      {
         clone.publications = new ArrayList<>();
         for(EsbTablePublication publication : publications)
         {
            clone.publications.add(publication.clone());
         }
      }
      return (clone);
   }



   /*******************************************************************************
    ** Getter for publications
    *******************************************************************************/
   public List<EsbTablePublication> getPublications()
   {
      return (this.publications);
   }



   /*******************************************************************************
    ** Setter for publications
    *******************************************************************************/
   public void setPublications(List<EsbTablePublication> publications)
   {
      this.publications = publications;
   }



   /*******************************************************************************
    ** Fluent setter for publications
    *******************************************************************************/
   public EsbTableMetaData withPublications(List<EsbTablePublication> publications)
   {
      this.publications = publications;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter to add a single publication
    *******************************************************************************/
   public EsbTableMetaData withPublication(EsbTablePublication publication)
   {
      if(this.publications == null)
      {
         this.publications = new ArrayList<>();
      }
      this.publications.add(publication);
      return (this);
   }

}
