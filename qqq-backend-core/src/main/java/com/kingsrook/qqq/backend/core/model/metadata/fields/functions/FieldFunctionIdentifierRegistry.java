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
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.ClassPathUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Singleton registry that auto-discovers all {@link FieldFunctionType} implementations
 * on the classpath and maps their identifier names to {@link FieldFunctionTypeIdentifier}
 * objects.
 *
 * <p>This allows non-Java contexts (e.g., JSON-deserialized filter criteria from a
 * frontend) to look up a FieldFunctionTypeIdentifier by its string name.  For
 * Java code, prefer referencing the IDENTIFIER constant on the implementation class
 * directly rather than going through this registry.</p>
 *
 * <p>Out-of-the-box, this registry will find all implementations of {@link FieldFunctionType}
 * that are in the same package as FieldFunctionType itself.  An application can
 * add custom functions and manually register them via {@link #register(FieldFunctionTypeIdentifier)}</p>
 *******************************************************************************/
public class FieldFunctionIdentifierRegistry
{
   private static final QLogger LOG = QLogger.getLogger(FieldFunctionIdentifierRegistry.class);



   /***************************************************************************
    * singleton holder - avoid double-lock in the constructor
    ***************************************************************************/
   private static final class SingletonHolder
   {
      private static final FieldFunctionIdentifierRegistry INSTANCE = new FieldFunctionIdentifierRegistry();
   }

   //////////////////////////////////////////
   // keys here are names from identifiers //
   //////////////////////////////////////////
   private Map<String, FieldFunctionTypeIdentifier> identifierRegistry = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** Singleton constructor
    *******************************************************************************/
   private FieldFunctionIdentifierRegistry()
   {
      scanClasspathForFunctionsAndRegisterThem();
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private void scanClasspathForFunctionsAndRegisterThem()
   {
      try
      {
         for(Class<?> c : ClassPathUtils.getClassesInPackage(FieldFunctionType.class.getPackageName()))
         {
            if(FieldFunctionType.class.isAssignableFrom(c) && !c.isInterface())
            {
               try
               {
                  FieldFunctionType fieldFunctionType = (FieldFunctionType) c.getConstructor().newInstance();
                  register(fieldFunctionType.getIdentifier());
               }
               catch(Exception e)
               {
                  LOG.info("Exception trying out possible field function type class: " + c.getName(), e);
               }
            }
         }
      }
      catch(Exception e)
      {
         LOG.error("Error registering field function identities", e);
      }
   }



   /***************************************************************************
    * Singleton accessor
    ***************************************************************************/
   public static FieldFunctionIdentifierRegistry getInstance()
   {
      return SingletonHolder.INSTANCE;
   }



   /***************************************************************************
    * Registers the given identifier. If a different identifier is already registered
    * under the same name, logs a warning and replaces it.
    ***************************************************************************/
   public void register(FieldFunctionTypeIdentifier identifier)
   {
      //////////////////////////////////////////////////////////////////////////////////
      // if a different value is already registered under this key, then log about it //
      //////////////////////////////////////////////////////////////////////////////////
      String key = identifier.getName();
      if(identifierRegistry.get(key) != null)
      {
         if(!identifierRegistry.get(key).equals(identifier))
         {
            LOG.info("Replacing FieldFunction type in identity registry.", logPair("identifier", key), logPair("old", identifierRegistry.get(key)), logPair("new", identifier));
         }
      }

      //////////////////////////////////////////////
      // put the code reference into the registry //
      //////////////////////////////////////////////
      identifierRegistry.put(key, identifier);
   }



   /***************************************************************************
    * Removes the identifier registered under the given name, if any.
    ***************************************************************************/
   public void unregister(String name)
   {
      identifierRegistry.remove(name);
   }



   /***************************************************************************
    * Returns the registered FieldFunctionTypeIdentifier for the given name, or
    * null if not found.
    ***************************************************************************/
   public FieldFunctionTypeIdentifier getFieldFunctionTypeIdentifier(String name)
   {
      return (identifierRegistry.get(name));
   }

}
