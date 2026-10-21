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

package com.kingsrook.qqq.backend.core.model.metadata.fields.functions;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Registry of {@link BackendFieldFunctionAdapterInterface} implementations, keyed
 * by function type identifier name.  These objects are, by design, stored as
 * properties of {@link QBackendMetaData} subclasses (which is why the keys inside
 * the registry are just function identifiers - each backend has 1 adapter
 * implementation per function type).
 *
 * <p>Backend modules (e.g., RDBMS) register adapters here so that when a query
 * is executed, the appropriate backend-specific adapter can be retrieved by
 * function type identifier to generate the correct SQL or other backend syntax.</p>
 *******************************************************************************/
public class BackendFieldFunctionAdapterRegistry
{
   private static final QLogger LOG = QLogger.getLogger(BackendFieldFunctionAdapterRegistry.class);

   /////////////////////////////////////////////////////////////
   // map of:  FieldFunctionIdentifier.name -> code reference //
   /////////////////////////////////////////////////////////////
   private Map<String, QCodeReference> registry = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public BackendFieldFunctionAdapterRegistry()
   {

   }



   /***************************************************************************
    * Registers an adapter code reference for the given function type identifier
    * and backend type. Validates that the code reference can be instantiated
    * before registering.
    ***************************************************************************/
   public void register(FieldFunctionTypeIdentifier identifier, String backendTypeName, QCodeReference adapterCodeReference)
   {
      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure the code reference is valid - that is - that it can be instantiated as the expected type //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      try
      {
         QCodeLoader.getAdHoc(BackendFieldFunctionAdapterInterface.class, adapterCodeReference);
      }
      catch(Exception e)
      {
         LOG.error("Error registering FieldFunctionAdapter for function type [" + identifier.getName() + "], backend type [" + backendTypeName + "] code reference [" + adapterCodeReference + "]", e);
         return;
      }

      //////////////////////////////////////////////////////////////////////////////////
      // if a different value is already registered under this key, then log about it //
      //////////////////////////////////////////////////////////////////////////////////
      String key = identifier.getName();
      if(registry.get(key) != null)
      {
         if(!registry.get(key).equals(adapterCodeReference))
         {
            LOG.info("Replacing FieldFunction type in registry.", logPair("identifier", key), logPair("old", registry.get(key)), logPair("new", adapterCodeReference));
         }
      }

      //////////////////////////////////////////////
      // put the code reference into the registry //
      //////////////////////////////////////////////
      registry.put(key, adapterCodeReference);
   }



   /***************************************************************************
    * Returns an instantiated BackendFieldFunctionAdapterInterface for the given
    * identifier, or null if no adapter is registered.
    ***************************************************************************/
   public BackendFieldFunctionAdapterInterface getFieldFunctionAdapter(FieldFunctionTypeIdentifier identifier)
   {
      String         key           = identifier.getName();
      QCodeReference codeReference = registry.get(key);
      if(codeReference != null)
      {
         BackendFieldFunctionAdapterInterface adapter = QCodeLoader.getAdHoc(BackendFieldFunctionAdapterInterface.class, codeReference);
         return (adapter);
      }

      LOG.info("No fieldFunctionAdapter is registered for requested name", logPair("key", key));
      return (null);
   }

}
