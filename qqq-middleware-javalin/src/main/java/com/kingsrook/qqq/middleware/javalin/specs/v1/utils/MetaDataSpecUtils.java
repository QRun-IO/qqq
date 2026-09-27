/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.utils;


import java.util.Comparator;
import java.util.TreeSet;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.instances.enrichment.plugins.QInstanceEnricherPluginInterface;
import com.kingsrook.qqq.backend.core.instances.validation.plugins.QInstanceValidatorPluginInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.utils.ListingHash;


/*******************************************************************************
 **
 *******************************************************************************/
public class MetaDataSpecUtils
{
   static QInstance exampleInstance;

   static
   {
      exampleInstance = defineExampleInstance();
      validateExampleInstance(exampleInstance);
   }

   /*******************************************************************************
    ** Getter for exampleInstance
    **
    *******************************************************************************/
   public static QInstance getExampleInstance()
   {
      return exampleInstance;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static QInstance defineExampleInstance()
   {
      QInstance exampleInstance = new QInstance();
      exampleInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withName("anonymous").withType(QAuthenticationType.FULLY_ANONYMOUS));

      QBackendMetaData exampleBackend = new QBackendMetaData()
         .withName("example")
         .withBackendType(MemoryBackendModule.class);
      exampleInstance.addBackend(exampleBackend);

      //////////////////////////////////////
      // create stable sorting of entries //
      //////////////////////////////////////
      TreeSet<Capability> capabilities = new TreeSet<>(Comparator.comparing((Capability c) -> c.name()));
      capabilities.addAll(Capability.allReadCapabilities());
      capabilities.addAll(Capability.allWriteCapabilities());

      QTableMetaData exampleTable = new QTableMetaData()
         .withName("person")
         .withLabel("Person")
         .withBackendName("example")
         .withPrimaryKeyField("id")
         .withIsHidden(false)
         .withIcon(new QIcon().withName("person_outline"))
         .withEnabledCapabilities(capabilities)
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
      exampleInstance.addTable(exampleTable);

      QProcessMetaData exampleProcess = new QProcessMetaData()
         .withName("samplePersonProcess")
         .withLabel("Sample Person Process")
         .withTableName("person")
         .withIsHidden(false)
         .withIcon(new QIcon().withName("person_add"))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withStep(new QFrontendStepMetaData().withName("example"));
      exampleInstance.addProcess(exampleProcess);

      QAppMetaData childApp = new QAppMetaData()
         .withName("childApp")
         .withLabel("Child App")
         .withIcon(new QIcon().withName("child_friendly"))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withChild(exampleProcess);
      exampleInstance.addApp(childApp);

      QAppMetaData exampleApp = new QAppMetaData()
         .withName("homeApp")
         .withLabel("Home App")
         .withIcon(new QIcon().withName("home"))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withChild(childApp)
         .withChild(exampleTable);
      exampleInstance.addApp(exampleApp);
      return exampleInstance;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void validateExampleInstance(QInstance exampleInstance)
   {
      ListingHash<Class<?>, QInstanceEnricherPluginInterface<?>> enricherPlugins = QInstanceEnricher.getEnricherPlugins();
      QInstanceEnricher.removeAllEnricherPlugins();

      ListingHash<Class<?>, QInstanceValidatorPluginInterface<?>> validatorPlugins = QInstanceValidator.getValidatorPlugins();
      QInstanceValidator.removeAllValidatorPlugins();

      try
      {
         new QInstanceValidator().validate(exampleInstance);
      }
      catch(QInstanceValidationException e)
      {
         System.err.println("Error validating example instance: " + e.getMessage());
      }

      enricherPlugins.values().forEach(l -> l.forEach(QInstanceEnricher::addEnricherPlugin));
      validatorPlugins.values().forEach(l -> l.forEach(QInstanceValidator::addValidatorPlugin));
   }
}
