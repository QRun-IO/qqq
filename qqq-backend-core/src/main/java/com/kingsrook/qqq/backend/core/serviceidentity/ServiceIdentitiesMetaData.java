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

package com.kingsrook.qqq.backend.core.serviceidentity;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.alerts.QAlertTypeMetaData;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;


/*******************************************************************************
 ** The service principals an instance declares (docs/SERVICE-IDENTITY.md).
 ** Qbits and hosts add the principals their background work runs as.
 *******************************************************************************/
public class ServiceIdentitiesMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "serviceIdentities";

   public static final String ALERT_TYPE_UNAVAILABLE = "service_identity.unavailable";

   public static final Pattern PRINCIPAL_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{0,62}$");

   private Map<String, QServicePrincipalMetaData> principals = new LinkedHashMap<>();
   private List<String>                           duplicates = new ArrayList<>();



   /*******************************************************************************
    ** The instance's service identities meta-data, or null when none declared.
    *******************************************************************************/
   public static ServiceIdentitiesMetaData of(QInstance qInstance)
   {
      return (qInstance == null ? null : QSupplementalInstanceMetaData.of(qInstance, NAME));
   }



   /*******************************************************************************
    ** The instance's service identities meta-data, adding an empty one if needed.
    *******************************************************************************/
   public static ServiceIdentitiesMetaData ofOrWithNew(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, ServiceIdentitiesMetaData::new));
   }



   /*******************************************************************************
    ** A declared principal, or null.
    *******************************************************************************/
   public static QServicePrincipalMetaData getPrincipal(QInstance qInstance, String name)
   {
      ServiceIdentitiesMetaData metaData = of(qInstance);
      return (metaData == null || name == null ? null : metaData.principals.get(name));
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
    ** Declare a principal. Declaring the same name twice fails validation.
    *******************************************************************************/
   public ServiceIdentitiesMetaData withPrincipal(QServicePrincipalMetaData principal)
   {
      if(principals.containsKey(principal.getName()))
      {
         duplicates.add(principal.getName());
      }
      principals.put(principal.getName(), principal);
      return (this);
   }



   /*******************************************************************************
    ** Getter for principals
    *******************************************************************************/
   public Map<String, QServicePrincipalMetaData> getPrincipals()
   {
      return (this.principals);
   }



   /*******************************************************************************
    ** Declare a principal on an instance, with the unavailable-principal alert
    ** type. The way qbits and hosts add principals.
    *******************************************************************************/
   public static QServicePrincipalMetaData declare(QInstance qInstance, QServicePrincipalMetaData principal)
   {
      ofOrWithNew(qInstance).withPrincipal(principal);
      AlertsInstanceMetaData alerts = AlertsInstanceMetaData.ofOrWithNew(qInstance);
      if(alerts.getAlertTypes() == null || !alerts.getAlertTypes().containsKey(ALERT_TYPE_UNAVAILABLE))
      {
         alerts.withAlertType(new QAlertTypeMetaData().withName(ALERT_TYPE_UNAVAILABLE).withLabel("Service identity unavailable")
            .withDefaultSeverity(AlertSeverity.ERROR));
      }
      return (principal);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      for(String duplicate : duplicates)
      {
         validator.assertCondition(false, "Service principal " + duplicate + " is declared more than once.");
      }
      for(Map.Entry<String, QServicePrincipalMetaData> entry : principals.entrySet())
      {
         validator.assertCondition(entry.getKey() != null && PRINCIPAL_NAME_PATTERN.matcher(entry.getKey()).matches(),
            "Service principal name [" + entry.getKey() + "] must be lowercase letters, digits and dashes.");
      }
      if(!principals.isEmpty() && qInstance.getAuthentication() instanceof OAuth2AuthenticationMetaData oauth2)
      {
         validator.assertCondition(Boolean.TRUE.equals(oauth2.getBearerTokensEnabled()),
            "Service principals under OAuth2 need bearerTokensEnabled: their sessions are bearer-token sessions.");
      }
   }
}
