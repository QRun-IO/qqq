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

package com.kingsrook.qqq.backend.core.cluster;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Which transport carries cluster control events (ClusterBus). Without it,
 ** events stay in this JVM (InMemoryClusterBus). Applications with qqq-esb use
 ** its EsbClusterBus (an Artemis topic, qqq.cluster.control).
 *******************************************************************************/
public class ClusterBusMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "clusterBus";

   @JsonIgnore
   private QCodeReference bus;



   /*******************************************************************************
    ** The instance's meta data, or null.
    *******************************************************************************/
   public static ClusterBusMetaData of(QInstance instance)
   {
      return (instance == null ? null : QSupplementalInstanceMetaData.of(instance, NAME));
   }



   /*******************************************************************************
    ** Supplemental meta data name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Getter for bus
    *******************************************************************************/
   public QCodeReference getBus()
   {
      return (bus);
   }



   /*******************************************************************************
    ** Setter for bus
    *******************************************************************************/
   public void setBus(QCodeReference bus)
   {
      this.bus = bus;
   }



   /*******************************************************************************
    ** Fluent setter for bus
    *******************************************************************************/
   public ClusterBusMetaData withBus(QCodeReference bus)
   {
      this.bus = bus;
      return (this);
   }
}
