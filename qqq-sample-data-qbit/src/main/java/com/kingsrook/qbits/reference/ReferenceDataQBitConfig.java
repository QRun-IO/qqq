/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

/* Copyright 2026 QRun-IO. */

package com.kingsrook.qbits.reference;

import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;

/** Host backend and table prefix adapted from the data-QBit template config. */
public final class ReferenceDataQBitConfig implements QBitConfig
{
   private static final long serialVersionUID = 1L;

   private final String backendName;
   private final String tableNamePrefix;

   /** Construct a configuration with a safe metadata and SQL identifier prefix. */
   public ReferenceDataQBitConfig(String backendName, String tableNamePrefix)
   {
      if(backendName == null || backendName.isBlank())
      {
         throw new IllegalArgumentException("Reference-data backend is required");
      }
      if(tableNamePrefix == null || !tableNamePrefix.matches("[A-Za-z][A-Za-z0-9_]*"))
      {
         throw new IllegalArgumentException("Invalid reference-data prefix");
      }
      this.backendName = backendName;
      this.tableNamePrefix = tableNamePrefix;
   }

   /** Get the configured host backend name. */
   public String getBackendName()
   {
      return backendName;
   }

   /** Get the configured prefix for generated metadata. */
   public String getTableNamePrefix()
   {
      return tableNamePrefix;
   }

   /** Scope a template metadata name to this consumer instance. */
   public String applyPrefix(String name)
   {
      return tableNamePrefix + "_" + name;
   }

   /** Require the selected backend to exist in the host. */
   @Override
   public void validate(QInstance instance, List<String> errors)
   {
      if(instance.getBackend(backendName) == null)
      {
         errors.add("Unknown reference-data backend");
      }
   }
}
