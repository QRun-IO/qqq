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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.esb.publish.EsbProcessLifecycleListener;
import com.kingsrook.qqq.esb.publish.EsbRecordChangeListener;


/*******************************************************************************
 * Instance-level ESB meta-data: the providers (brokers) and destinations
 * (queues and topics) that tables and processes publish to and trigger from,
 * and the instance name that identifies this application in the source of the
 * events it publishes.
 *
 * Supplemental instance meta-data is included in the frontend meta-data
 * output, so the providers and destinations (with urls and credentials), and
 * the instance name, are excluded from JSON serialization.  The UI reads ESB
 * details through the permission-checked ESB endpoints instead.
 *******************************************************************************/
public class EsbInstanceMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "esb";

   private static final long serialVersionUID = 1L;

   //////////////////////////////////////////////////////////////////////
   // RFC 3986 unreserved characters - so qqq://instanceName/... is a  //
   // valid URI, with the instance name as its authority, unescaped    //
   //////////////////////////////////////////////////////////////////////
   private static final String INSTANCE_NAME_PATTERN = "[A-Za-z0-9._~-]+";

   private String                               instanceName;
   private Map<String, QEsbProviderMetaData>    providers    = new LinkedHashMap<>();
   private Map<String, QEsbDestinationMetaData> destinations = new LinkedHashMap<>();



   /*******************************************************************************
    ** Get the ESB meta-data on an instance - creating (and adding) it if the
    ** instance doesn't have any yet.
    *******************************************************************************/
   public static EsbInstanceMetaData of(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, EsbInstanceMetaData::new));
   }



   /*******************************************************************************
    ** Get the ESB meta-data on an instance, or null if it has none - without
    ** adding any (e.g., for use during validation).
    *******************************************************************************/
   static EsbInstanceMetaData ofOrNull(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.of(qInstance, NAME));
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
    ** Interpret ${env.*} (etc.) variables in each provider's connection fields,
    ** and register the listeners that publish table events and process
    ** lifecycle events (each once, however many times the instance is
    ** enriched).
    *******************************************************************************/
   @Override
   public void enrich(QInstance qInstance)
   {
      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();
      for(QEsbProviderMetaData provider : CollectionUtils.nonNullMap(providers).values())
      {
         provider.interpretVariables(interpreter);
      }

      if(!isRegistered(qInstance.getRecordChangeListeners(), EsbRecordChangeListener.class))
      {
         qInstance.withRecordChangeListener(new QCodeReference(EsbRecordChangeListener.class));
      }

      if(!isRegistered(qInstance.getProcessLifecycleListeners(), EsbProcessLifecycleListener.class))
      {
         qInstance.withProcessLifecycleListener(new QCodeReference(EsbProcessLifecycleListener.class));
      }
   }



   /*******************************************************************************
    ** Whether a list of listener code references includes the listener class.
    *******************************************************************************/
   private static boolean isRegistered(List<QCodeReference> listenerCodeReferences, Class<?> listenerClass)
   {
      return (CollectionUtils.nonNullList(listenerCodeReferences).stream()
         .anyMatch(codeReference -> codeReference != null && listenerClass.getName().equals(codeReference.getName())));
   }



   /*******************************************************************************
    ** Validate providers and destinations, and that topic triggers' subscription
    ** names are unique per provider.  (Publications and each trigger's own
    ** fields are validated by EsbTableMetaData and EsbProcessMetaData.)
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      if(instanceName != null)
      {
         validator.assertCondition(instanceName.matches(INSTANCE_NAME_PATTERN), "ESB instanceName " + instanceName + " may only contain letters, digits, and . _ ~ - (it is the authority of the events' qqq:// source URI).");
      }

      for(QEsbProviderMetaData provider : CollectionUtils.nonNullMap(providers).values())
      {
         String prefix = "ESB provider " + provider.getName() + " ";
         validator.assertCondition(StringUtils.hasContent(provider.getName()), "An ESB provider is missing a name.");
         validator.assertCondition(provider.getType() != null, prefix + "is missing a type.");
         validator.assertCondition(StringUtils.hasContent(provider.getUrl()), prefix + "is missing a url.");
      }

      for(QEsbDestinationMetaData destination : CollectionUtils.nonNullMap(destinations).values())
      {
         String prefix = "ESB destination " + destination.getName() + " ";
         validator.assertCondition(StringUtils.hasContent(destination.getName()), "An ESB destination is missing a name.");
         validator.assertCondition(destination.getType() != null, prefix + "is missing a type.");
         validator.assertCondition(getProvider(destination.getProviderName()) != null, prefix + "references an unknown provider: " + destination.getProviderName() + ".");
      }

      validateUniqueSubscriptionNames(qInstance, validator);
   }



   /*******************************************************************************
    ** Both brokers name a shared durable subscription's queue after the
    ** subscription, and (on Artemis) queue names are unique across the broker -
    ** so two topic triggers on one provider with the same effective subscription
    ** name would share (and split) one subscription.  Report each such name.
    *******************************************************************************/
   private void validateUniqueSubscriptionNames(QInstance qInstance, QInstanceValidator validator)
   {
      Map<String, Map<String, List<String>>> triggerNamesBySubscriptionByProvider = new LinkedHashMap<>();
      for(QProcessMetaData process : CollectionUtils.nonNullMap(qInstance.getProcesses()).values())
      {
         EsbProcessMetaData esbProcessMetaData = EsbProcessMetaData.of(process);
         if(esbProcessMetaData == null)
         {
            continue;
         }

         for(EsbTrigger trigger : CollectionUtils.nonNullList(esbProcessMetaData.getTriggers()))
         {
            /////////////////////////////////////////////////////////////////////
            // only topic triggers have subscriptions.  an unknown destination //
            // is reported by the trigger's own validation.                    //
            /////////////////////////////////////////////////////////////////////
            QEsbDestinationMetaData destination = getDestination(trigger.getDestinationName());
            if(destination == null || destination.getType() != EsbDestinationType.TOPIC)
            {
               continue;
            }

            String subscriptionName = trigger.getEffectiveSubscriptionName(process.getName(), destination);
            triggerNamesBySubscriptionByProvider
               .computeIfAbsent(destination.getProviderName(), k -> new LinkedHashMap<>())
               .computeIfAbsent(subscriptionName, k -> new ArrayList<>())
               .add(trigger.getName(process.getName()));
         }
      }

      for(Map.Entry<String, Map<String, List<String>>> providerEntry : triggerNamesBySubscriptionByProvider.entrySet())
      {
         for(Map.Entry<String, List<String>> subscriptionEntry : providerEntry.getValue().entrySet())
         {
            List<String> triggerNames = subscriptionEntry.getValue();
            validator.assertCondition(triggerNames.size() == 1, "ESB subscription name " + subscriptionEntry.getKey() + " is used by more than one topic trigger on provider " + providerEntry.getKey() + ": " + String.join(", ", triggerNames) + ".");
         }
      }
   }



   /*******************************************************************************
    ** Get a provider by name - or null if not found.
    *******************************************************************************/
   public QEsbProviderMetaData getProvider(String name)
   {
      return (providers == null || name == null ? null : providers.get(name));
   }



   /*******************************************************************************
    ** Fluent setter to add a provider (keyed by its name).
    **
    ** Throws IllegalArgumentException if a provider with that name was already
    ** added (as QInstance does for a second table, process, etc).
    *******************************************************************************/
   public EsbInstanceMetaData withProvider(QEsbProviderMetaData provider)
   {
      if(this.providers == null)
      {
         this.providers = new LinkedHashMap<>();
      }

      if(this.providers.containsKey(provider.getName()))
      {
         throw (new IllegalArgumentException("Attempted to add a second ESB provider with name: " + provider.getName()));
      }

      this.providers.put(provider.getName(), provider);
      return (this);
   }



   /*******************************************************************************
    ** Get a destination by name - or null if not found.
    *******************************************************************************/
   public QEsbDestinationMetaData getDestination(String name)
   {
      return (destinations == null || name == null ? null : destinations.get(name));
   }



   /*******************************************************************************
    ** Fluent setter to add a destination (keyed by its name).
    **
    ** Throws IllegalArgumentException if a destination with that name was
    ** already added (as QInstance does for a second table, process, etc).
    *******************************************************************************/
   public EsbInstanceMetaData withDestination(QEsbDestinationMetaData destination)
   {
      if(this.destinations == null)
      {
         this.destinations = new LinkedHashMap<>();
      }

      if(this.destinations.containsKey(destination.getName()))
      {
         throw (new IllegalArgumentException("Attempted to add a second ESB destination with name: " + destination.getName()));
      }

      this.destinations.put(destination.getName(), destination);
      return (this);
   }



   /*******************************************************************************
    ** Getter for instanceName: the name that identifies this application in the
    ** source of the events it publishes (qqq://instanceName/table/order).  When
    ** null, sources have an empty authority (qqq:///table/order).  Not
    ** serialized (see class comment).
    *******************************************************************************/
   @JsonIgnore
   public String getInstanceName()
   {
      return (this.instanceName);
   }



   /*******************************************************************************
    ** Setter for instanceName
    *******************************************************************************/
   public void setInstanceName(String instanceName)
   {
      this.instanceName = instanceName;
   }



   /*******************************************************************************
    ** Fluent setter for instanceName
    *******************************************************************************/
   public EsbInstanceMetaData withInstanceName(String instanceName)
   {
      this.instanceName = instanceName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for providers - not serialized (they hold urls and credentials).
    *******************************************************************************/
   @JsonIgnore
   public Map<String, QEsbProviderMetaData> getProviders()
   {
      return (this.providers);
   }



   /*******************************************************************************
    ** Setter for providers
    *******************************************************************************/
   public void setProviders(Map<String, QEsbProviderMetaData> providers)
   {
      this.providers = providers;
   }



   /*******************************************************************************
    ** Getter for destinations - not serialized (see class comment).
    *******************************************************************************/
   @JsonIgnore
   public Map<String, QEsbDestinationMetaData> getDestinations()
   {
      return (this.destinations);
   }



   /*******************************************************************************
    ** Setter for destinations
    *******************************************************************************/
   public void setDestinations(Map<String, QEsbDestinationMetaData> destinations)
   {
      this.destinations = destinations;
   }

}
