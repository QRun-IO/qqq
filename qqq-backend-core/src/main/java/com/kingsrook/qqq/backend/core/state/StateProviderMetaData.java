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

package com.kingsrook.qqq.backend.core.state;


import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Where process and async job state lives (spec 2026-10-15-cluster-ha-design,
 ** section B). Without this supplemental meta data (or with neither a provider
 ** nor a table), state is in memory on each node, as before. With tableName,
 ** the built-in TableStateProvider shares state through that table, so any
 ** node can serve any process step, status poll or cancel.
 *******************************************************************************/
public class StateProviderMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "stateProvider";

   public static final String DEFAULT_TABLE_NAME     = "qqq_state";
   public static final String DEFAULT_ENCRYPTION_KEY = "${env.QQQ_STATE_ENCRYPTION_KEY}";

   @JsonIgnore
   private QCodeReference providerCode;

   private String       tableName;
   private Integer      ttlHours                    = 12;
   private String       encryptionKey               = DEFAULT_ENCRYPTION_KEY;
   private List<String> allowedClassPrefixes        = new ArrayList<>();
   private Integer      progressWriteIntervalMillis = 1000;
   private Integer      heartbeatSeconds            = 10;
   private Integer      lostAfterSeconds            = 60;
   private Integer      cleanIntervalMinutes        = 15;
   private Boolean      requireEncryption           = true;



   /*******************************************************************************
    ** The instance's state meta data, or null when none is configured.
    *******************************************************************************/
   public static StateProviderMetaData of(QInstance instance)
   {
      return (instance == null ? null : QSupplementalInstanceMetaData.of(instance, NAME));
   }



   /*******************************************************************************
    ** Whether the instance shares state across nodes.
    *******************************************************************************/
   public boolean isShared()
   {
      return (providerCode != null || StringUtils.hasContent(tableName));
   }



   /*******************************************************************************
    ** Supplemental meta data name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Validate: a configured table must exist; limits must be positive.
    *******************************************************************************/
   @Override
   public void validate(QInstance instance, QInstanceValidator validator)
   {
      if(StringUtils.hasContent(tableName))
      {
         validator.assertCondition(instance.getTable(tableName) != null, "State provider table [" + tableName + "] is not defined in the instance.");
      }
      validator.assertCondition(ttlHours != null && ttlHours > 0, "State provider ttlHours must be positive.");
      validator.assertCondition(progressWriteIntervalMillis != null && progressWriteIntervalMillis >= 0, "State provider progressWriteIntervalMillis may not be negative.");
      validator.assertCondition(heartbeatSeconds != null && heartbeatSeconds > 0, "State provider heartbeatSeconds must be positive.");
      validator.assertCondition(lostAfterSeconds != null && heartbeatSeconds != null && lostAfterSeconds > heartbeatSeconds * 2, "State provider lostAfterSeconds must be more than twice heartbeatSeconds.");
      validator.assertCondition(cleanIntervalMinutes != null && cleanIntervalMinutes > 0, "State provider cleanIntervalMinutes must be positive.");

      ///////////////////////////////////////////////////////////////////////////
      // shared state holds users' process data and one-time secrets: when it  //
      // is shared, an encryption key is required (fail fast at startup), unless //
      // explicitly waived (tests, single-tenant dev)                          //
      ///////////////////////////////////////////////////////////////////////////
      if(isShared() && !Boolean.FALSE.equals(requireEncryption))
      {
         String keyValue = StringUtils.hasContent(encryptionKey) ? new QMetaDataVariableInterpreter().interpret(encryptionKey) : null;
         validator.assertCondition(StringUtils.hasContent(keyValue) && !keyValue.startsWith("${"),
            "Shared state needs an encryption key: set QQQ_STATE_ENCRYPTION_KEY (base64, 32 bytes) or the state provider's encryptionKey.");
      }
   }



   /*******************************************************************************
    ** Getter for providerCode
    *******************************************************************************/
   public QCodeReference getProviderCode()
   {
      return (providerCode);
   }



   /*******************************************************************************
    ** Setter for providerCode
    *******************************************************************************/
   public void setProviderCode(QCodeReference providerCode)
   {
      this.providerCode = providerCode;
   }



   /*******************************************************************************
    ** Fluent setter for providerCode
    *******************************************************************************/
   public StateProviderMetaData withProviderCode(QCodeReference providerCode)
   {
      this.providerCode = providerCode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public StateProviderMetaData withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for ttlHours
    *******************************************************************************/
   public Integer getTtlHours()
   {
      return (ttlHours);
   }



   /*******************************************************************************
    ** Setter for ttlHours
    *******************************************************************************/
   public void setTtlHours(Integer ttlHours)
   {
      this.ttlHours = ttlHours;
   }



   /*******************************************************************************
    ** Fluent setter for ttlHours
    *******************************************************************************/
   public StateProviderMetaData withTtlHours(Integer ttlHours)
   {
      this.ttlHours = ttlHours;
      return (this);
   }



   /*******************************************************************************
    ** Getter for encryptionKey
    *******************************************************************************/
   public String getEncryptionKey()
   {
      return (encryptionKey);
   }



   /*******************************************************************************
    ** Setter for encryptionKey
    *******************************************************************************/
   public void setEncryptionKey(String encryptionKey)
   {
      this.encryptionKey = encryptionKey;
   }



   /*******************************************************************************
    ** Fluent setter for encryptionKey
    *******************************************************************************/
   public StateProviderMetaData withEncryptionKey(String encryptionKey)
   {
      this.encryptionKey = encryptionKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for allowedClassPrefixes
    *******************************************************************************/
   public List<String> getAllowedClassPrefixes()
   {
      return (allowedClassPrefixes);
   }



   /*******************************************************************************
    ** Setter for allowedClassPrefixes
    *******************************************************************************/
   public void setAllowedClassPrefixes(List<String> allowedClassPrefixes)
   {
      this.allowedClassPrefixes = allowedClassPrefixes;
   }



   /*******************************************************************************
    ** Fluent setter for allowedClassPrefixes
    *******************************************************************************/
   public StateProviderMetaData withAllowedClassPrefixes(List<String> allowedClassPrefixes)
   {
      this.allowedClassPrefixes = allowedClassPrefixes;
      return (this);
   }



   /*******************************************************************************
    ** Getter for progressWriteIntervalMillis
    *******************************************************************************/
   public Integer getProgressWriteIntervalMillis()
   {
      return (progressWriteIntervalMillis);
   }



   /*******************************************************************************
    ** Setter for progressWriteIntervalMillis
    *******************************************************************************/
   public void setProgressWriteIntervalMillis(Integer progressWriteIntervalMillis)
   {
      this.progressWriteIntervalMillis = progressWriteIntervalMillis;
   }



   /*******************************************************************************
    ** Fluent setter for progressWriteIntervalMillis
    *******************************************************************************/
   public StateProviderMetaData withProgressWriteIntervalMillis(Integer progressWriteIntervalMillis)
   {
      this.progressWriteIntervalMillis = progressWriteIntervalMillis;
      return (this);
   }



   /*******************************************************************************
    ** Getter for heartbeatSeconds
    *******************************************************************************/
   public Integer getHeartbeatSeconds()
   {
      return (heartbeatSeconds);
   }



   /*******************************************************************************
    ** Setter for heartbeatSeconds
    *******************************************************************************/
   public void setHeartbeatSeconds(Integer heartbeatSeconds)
   {
      this.heartbeatSeconds = heartbeatSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for heartbeatSeconds
    *******************************************************************************/
   public StateProviderMetaData withHeartbeatSeconds(Integer heartbeatSeconds)
   {
      this.heartbeatSeconds = heartbeatSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for lostAfterSeconds
    *******************************************************************************/
   public Integer getLostAfterSeconds()
   {
      return (lostAfterSeconds);
   }



   /*******************************************************************************
    ** Setter for lostAfterSeconds
    *******************************************************************************/
   public void setLostAfterSeconds(Integer lostAfterSeconds)
   {
      this.lostAfterSeconds = lostAfterSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for lostAfterSeconds
    *******************************************************************************/
   public StateProviderMetaData withLostAfterSeconds(Integer lostAfterSeconds)
   {
      this.lostAfterSeconds = lostAfterSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for cleanIntervalMinutes
    *******************************************************************************/
   public Integer getCleanIntervalMinutes()
   {
      return (cleanIntervalMinutes);
   }



   /*******************************************************************************
    ** Setter for cleanIntervalMinutes
    *******************************************************************************/
   public void setCleanIntervalMinutes(Integer cleanIntervalMinutes)
   {
      this.cleanIntervalMinutes = cleanIntervalMinutes;
   }



   /*******************************************************************************
    ** Fluent setter for cleanIntervalMinutes
    *******************************************************************************/
   public StateProviderMetaData withCleanIntervalMinutes(Integer cleanIntervalMinutes)
   {
      this.cleanIntervalMinutes = cleanIntervalMinutes;
      return (this);
   }



   /*******************************************************************************
    ** Getter for requireEncryption
    *******************************************************************************/
   public Boolean getRequireEncryption()
   {
      return (requireEncryption);
   }



   /*******************************************************************************
    ** Setter for requireEncryption
    *******************************************************************************/
   public void setRequireEncryption(Boolean requireEncryption)
   {
      this.requireEncryption = requireEncryption;
   }



   /*******************************************************************************
    ** Fluent setter for requireEncryption
    *******************************************************************************/
   public StateProviderMetaData withRequireEncryption(Boolean requireEncryption)
   {
      this.requireEncryption = requireEncryption;
      return (this);
   }
}
