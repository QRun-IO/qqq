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

package com.kingsrook.qqq.backend.core.health;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;


/*******************************************************************************
 ** Static and dynamic checks. Resolution creates a fresh validated snapshot.
 *******************************************************************************/
public class HealthChecksMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "healthChecks";
   private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9]++(?:[._-][a-z0-9]++)*+");
   private final List<HealthCheckMetaData> checks = new ArrayList<>();
   private final List<HealthCheckSupplierMetaData> suppliers = new ArrayList<>();



   /*******************************************************************************
    ** Get or install the registry.
    *******************************************************************************/
   public static HealthChecksMetaData ofOrWithNew(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, HealthChecksMetaData::new));
   }



   /*******************************************************************************
    ** Supplemental metadata name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Register a static definition.
    *******************************************************************************/
   public HealthChecksMetaData withCheck(HealthCheckMetaData check)
   {
      checks.add(check);
      return (this);
   }



   /*******************************************************************************
    ** Register a supplier.
    *******************************************************************************/
   public HealthChecksMetaData withSupplier(HealthCheckSupplierMetaData supplier)
   {
      suppliers.add(supplier);
      return (this);
   }



   /*******************************************************************************
    ** Snapshot static definitions.
    *******************************************************************************/
   public List<HealthCheckMetaData> getChecks()
   {
      return (List.copyOf(checks));
   }



   /*******************************************************************************
    ** Snapshot supplier definitions.
    *******************************************************************************/
   public List<HealthCheckSupplierMetaData> getSuppliers()
   {
      return (List.copyOf(suppliers));
   }



   /*******************************************************************************
    ** Resolve dynamic checks without letting a supplier escape its namespace.
    *******************************************************************************/
   public List<HealthCheckMetaData> resolveAll(QInstance qInstance) throws QException
   {
      List<HealthCheckMetaData> resolved = new ArrayList<>(checks);
      for(HealthCheckSupplierMetaData supplier : suppliers)
      {
         if(!validName(supplier.getNamePrefix()) || supplier.getComponent() == null || supplier.getComponent().isBlank() || supplier.getSupplier() == null)
         {
            throw new QException("Health supplier requires a valid prefix, component and code reference");
         }
         for(HealthCheckMetaData check : QCodeLoader.getAdHoc(HealthCheckSupplier.class, supplier.getSupplier()).supply(qInstance))
         {
            if(check.getName() == null || !check.getName().startsWith(supplier.getNamePrefix() + "."))
            {
               throw new QException("Health supplier check must be beneath its namePrefix");
            }
            if(check.getComponent() == null)
            {
               check.setComponent(supplier.getComponent());
            }
            resolved.add(check);
         }
      }
      Set<String> names = new HashSet<>();
      for(HealthCheckMetaData check : resolved)
      {
         String error = definitionError(check);
         if(error != null || !names.add(check.getName()))
         {
            throw new QException(error == null ? "Duplicate health check name" : error);
         }
      }
      return (List.copyOf(resolved));
   }



   /*******************************************************************************
    ** Structural validation does not invoke dynamic suppliers or perform I/O.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      Set<String> names = new HashSet<>();
      for(HealthCheckMetaData check : checks)
      {
         String error = definitionError(check);
         validator.assertCondition(error == null, "Health check " + (check == null ? "(null)" : "[" + check.getName() + "]") + ": " + error);
         if(check != null)
         {
            validator.assertCondition(names.add(check.getName()), "Duplicate health check name: " + check.getName());
            if(check.getCheck() != null)
            {
               validator.validateSimpleCodeReference("Health check: ", check.getCheck(), HealthCheck.class);
            }
         }
      }
      for(HealthCheckSupplierMetaData supplier : suppliers)
      {
         validator.assertCondition(validName(supplier.getNamePrefix()), "Health supplier requires a valid namePrefix.");
         validator.assertCondition(supplier.getComponent() != null && !supplier.getComponent().isBlank(), "Health supplier requires a component.");
         validator.validateSimpleCodeReference("Health supplier: ", supplier.getSupplier(), HealthCheckSupplier.class);
      }
   }



   /*******************************************************************************
    ** Turn an arbitrary identifier (a backend, provider or record name) into a
    ** valid check-name segment: camelCase becomes snake_case, everything is lower
    ** case, and characters outside [a-z0-9._-] become '_'.  For example
    ** "websiteDatabase" becomes "website_database".
    *******************************************************************************/
   public static String toNamePart(String raw)
   {
      if(raw == null)
      {
         return ("unnamed");
      }
      String part = raw.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT)
         .replaceAll("[^a-z0-9._-]+", "_")
         .replaceAll("([._-])[._-]+", "$1")
         .replaceAll("^[._-]+|[._-]+$", "");
      return (part.isEmpty() ? "unnamed" : part);
   }



   /*******************************************************************************
    ** Shared validation also protects newly resolved dynamic checks.
    *******************************************************************************/
   static String definitionError(HealthCheckMetaData check)
   {
      if(check == null || !validName(check.getName()))
      {
         return ("invalid name");
      }
      if(check.getComponent() == null || check.getComponent().isBlank())
      {
         return ("component is required");
      }
      if(check.getIntervalSeconds() < 10 || check.getTimeoutMs() <= 0 || check.getTimeoutMs() >= check.getIntervalSeconds() * 1000L)
      {
         return ("interval must be at least 10 seconds and timeout must be positive and below interval");
      }
      if(check.getKind() == HealthCheckKind.LIVENESS && check.getTimeoutMs() > 1000)
      {
         return ("LIVENESS timeout must be at most 1000ms");
      }
      if(check.getFailureThreshold() < 1 || check.getRecoveryThreshold() < 1 || check.getKind() == null || check.getCategory() == null || check.getSeverity() == null)
      {
         return ("thresholds must be positive; kind, category and severity are required");
      }
      if((check.getCheck() == null) == (check.getCheckInstance() == null))
      {
         return ("exactly one check code reference or check instance is required");
      }
      return (null);
   }



   /*******************************************************************************
    ** Stable names are safe in identifiers, filters and alert dedup keys.
    *******************************************************************************/
   private static boolean validName(String name)
   {
      return (name != null && name.length() <= 255 && NAME_PATTERN.matcher(name).matches());
   }
}
