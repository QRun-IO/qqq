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

package com.kingsrook.qqq.backend.core.model.developer;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Instance-level registry of the {@link DeveloperPanelProviderInterface}s
 ** that contribute panels to records' developer views.  It is supplemental
 ** meta-data, so any module or qbit can add to it; it is not sent to frontends
 ** (the panels themselves are, by the record developer panels endpoint).
 *******************************************************************************/
public class DeveloperPanelsMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "developerPanels";

   /** Code references to the provider classes, in registration order. */
   private List<QCodeReference> providers = new ArrayList<>();



   /*******************************************************************************
    ** Get the instance's registry, or null when nothing registered a provider.
    *******************************************************************************/
   public static DeveloperPanelsMetaData of(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.of(qInstance, NAME));
   }



   /*******************************************************************************
    ** Get the instance's registry, adding an empty one if it has none.
    *******************************************************************************/
   public static DeveloperPanelsMetaData ofOrWithNew(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, DeveloperPanelsMetaData::new));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Register a provider class.  Registering the same class again is a no-op,
    ** so modules may register idempotently.
    *******************************************************************************/
   public DeveloperPanelsMetaData withProvider(QCodeReference provider)
   {
      if(provider != null && providers.stream().noneMatch(existing -> Objects.equals(existing.getName(), provider.getName())))
      {
         providers.add(provider);
      }
      return (this);
   }



   /*******************************************************************************
    ** Getter for providers
    *******************************************************************************/
   public List<QCodeReference> getProviders()
   {
      return (this.providers);
   }



   /*******************************************************************************
    ** Setter for providers
    *******************************************************************************/
   public void setProviders(List<QCodeReference> providers)
   {
      this.providers = providers == null ? new ArrayList<>() : new ArrayList<>(providers);
   }



   /*******************************************************************************
    ** Every provider must name a class that implements the provider interface.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      for(QCodeReference provider : providers)
      {
         validator.validateSimpleCodeReference("Developer panel provider " + provider.getName() + ": ", provider, DeveloperPanelProviderInterface.class);
      }
   }

}
