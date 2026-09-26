/*
 * Copyright 2026 QRun-IO.
 * SPDX-License-Identifier: Apache-2.0
 */

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
