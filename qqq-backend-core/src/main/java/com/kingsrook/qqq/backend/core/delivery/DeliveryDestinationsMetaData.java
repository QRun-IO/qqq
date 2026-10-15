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

package com.kingsrook.qqq.backend.core.delivery;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;


/*******************************************************************************
 ** Instance-local destinations and delivery policy.
 *******************************************************************************/
public class DeliveryDestinationsMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "deliveryDestinations";
   private final List<QDeliveryDestinationMetaData> destinations = new ArrayList<>();
   private List<Integer> retryMinutes = List.of(1, 5, 15, 60, 240);
   private String emailProviderName;
   private String emailFromAddress;
   private Integer linkTtlHours = 72;
   private Integer logRetentionDays = 90;



   /*******************************************************************************
    ** Find the installed registry.
    *******************************************************************************/
   public static DeliveryDestinationsMetaData of(QInstance instance)
   {
      return (QSupplementalInstanceMetaData.of(instance, NAME));
   }



   /*******************************************************************************
    ** Install a registry when absent.
    *******************************************************************************/
   public static DeliveryDestinationsMetaData ofOrWithNew(QInstance instance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(instance, NAME, DeliveryDestinationsMetaData::new));
   }



   /*******************************************************************************
    ** Registry name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Keep duplicate registrations visible to validation.
    *******************************************************************************/
   public DeliveryDestinationsMetaData withDestination(QDeliveryDestinationMetaData destination)
   {
      destinations.add(destination);
      return (this);
   }



   /*******************************************************************************
    ** Registered destinations.
    *******************************************************************************/
   public List<QDeliveryDestinationMetaData> getDestinations()
   {
      return (destinations);
   }



   /*******************************************************************************
    ** Retry delays in minutes.
    *******************************************************************************/
   public List<Integer> getRetryMinutes()
   {
      return (retryMinutes);
   }



   /*******************************************************************************
    ** Configure retry delays.
    *******************************************************************************/
   public DeliveryDestinationsMetaData withRetryMinutes(List<Integer> retryMinutes)
   {
      this.retryMinutes = List.copyOf(retryMinutes);
      return (this);
   }



   /*******************************************************************************
    ** Default email provider.
    *******************************************************************************/
   public String getEmailProviderName()
   {
      return (emailProviderName);
   }



   /*******************************************************************************
    ** Configure the default email provider.
    *******************************************************************************/
   public DeliveryDestinationsMetaData withEmailProviderName(String emailProviderName)
   {
      this.emailProviderName = emailProviderName;
      return (this);
   }



   /*******************************************************************************
    ** Reject ambiguous names and invalid implementations during startup.
    *******************************************************************************/
   @Override
   public void validate(QInstance instance, QInstanceValidator validator)
   {
      validator.assertCondition(linkTtlHours != null && linkTtlHours > 0, "Delivery linkTtlHours must be positive");
      validator.assertCondition(logRetentionDays != null && logRetentionDays > 0, "Delivery logRetentionDays must be positive");
      Set<String> names = new HashSet<>();
      for(QDeliveryDestinationMetaData destination : destinations)
      {
         String path = "Delivery destination [" + destination.getName() + "]: ";
         validator.assertCondition(destination.getName() != null && !destination.getName().isBlank(), path + "name is required");
         validator.assertCondition(names.add(destination.getName()), path + "name must be unique");
         validator.assertCondition(destination.getLabel() != null && !destination.getLabel().isBlank(), path + "label is required");
         validator.assertCondition(destination.getMaxClassification() != null, path + "maxClassification is required");
         validator.assertCondition(destination.getAboveMaxClassification() != null, path + "aboveMaxClassification is required");
         validator.assertCondition(destination.getMaxAttachmentBytes() == null || destination.getMaxAttachmentBytes() >= 0, path + "maxAttachmentBytes must be nonnegative");
         try
         {
            validator.assertCondition(destination.getCodeReference() != null && QCodeLoader.getAdHoc(Object.class, destination.getCodeReference()) instanceof DeliveryDestinationInterface,
               path + "codeReference must implement DeliveryDestinationInterface");
         }
         catch(Exception | LinkageError e)
         {
            validator.assertCondition(false, path + "codeReference must implement DeliveryDestinationInterface");
         }
      }
      validator.assertCondition(retryMinutes.stream().allMatch(value -> value != null && value > 0), "Delivery retryMinutes must be positive");
   }



   /*******************************************************************************
    ** Get emailFromAddress.
    *******************************************************************************/
   public String getEmailFromAddress()
   {
      return (emailFromAddress);
   }



   /*******************************************************************************
    ** Configure emailFromAddress.
    *******************************************************************************/
   public DeliveryDestinationsMetaData withEmailFromAddress(String emailFromAddress)
   {
      this.emailFromAddress = emailFromAddress;
      return (this);
   }



   /*******************************************************************************
    ** Get linkTtlHours.
    *******************************************************************************/
   public Integer getLinkTtlHours()
   {
      return (linkTtlHours);
   }



   /*******************************************************************************
    ** Configure linkTtlHours.
    *******************************************************************************/
   public DeliveryDestinationsMetaData withLinkTtlHours(Integer linkTtlHours)
   {
      this.linkTtlHours = linkTtlHours;
      return (this);
   }



   /*******************************************************************************
    ** Get logRetentionDays.
    *******************************************************************************/
   public Integer getLogRetentionDays()
   {
      return (logRetentionDays);
   }



   /*******************************************************************************
    ** Configure logRetentionDays.
    *******************************************************************************/
   public DeliveryDestinationsMetaData withLogRetentionDays(Integer logRetentionDays)
   {
      this.logRetentionDays = logRetentionDays;
      return (this);
   }



   /*******************************************************************************
    ** Schedule retries only when both persistence and a scheduler are installed.
    *******************************************************************************/
   @Override
   public void enrich(QInstance instance)
   {
      var process = instance.getProcess(DeliveryRetryProcess.NAME);
      if(process != null && process.getSchedule() == null && instance.getTable(DeliveryMetaDataProvider.DELIVERY_LOG) != null
         && instance.getSchedulers() != null && !instance.getSchedulers().isEmpty())
      {
         process.withSchedule(new com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData()
            .withRepeatSeconds(60).withSchedulerName(instance.getSchedulers().keySet().iterator().next()));
      }
   }
}
