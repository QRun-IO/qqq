/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.backends;


import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheOf;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheUseCase;


/*******************************************************************************
 ** Provides meta data for the QQQBackend table, PVS, and a cache table.
 *******************************************************************************/
public class QQQBackendsMetaDataProvider
{
   public static final String QQQ_BACKEND_CACHE_TABLE_NAME = QQQBackend.TABLE_NAME + "Cache";



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineAll(QInstance instance, String persistentBackendName, String cacheBackendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      instance.addTable(defineQQQBackend(persistentBackendName, backendDetailEnricher));
      instance.addTable(defineQQQBackendCache(cacheBackendName, backendDetailEnricher));
      instance.addPossibleValueSource(defineQQQBackendPossibleValueSource());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData defineQQQBackend(String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      QTableMetaData table = new QTableMetaData()
         .withName(QQQBackend.TABLE_NAME)
         .withLabel("Backend")
         .withBackendName(backendName)
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE))
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("label")
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("name"))
         .withFieldsFromEntity(QQQBackend.class)
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }

      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData defineQQQBackendCache(String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      QTableMetaData table = new QTableMetaData()
         .withName(QQQ_BACKEND_CACHE_TABLE_NAME)
         .withBackendName(backendName)
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE))
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("label")
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("name"))
         .withFieldsFromEntity(QQQBackend.class)
         .withCacheOf(new CacheOf()
            .withSourceTable(QQQBackend.TABLE_NAME)
            .withUseCase(new CacheUseCase()
               .withType(CacheUseCase.Type.UNIQUE_KEY_TO_UNIQUE_KEY)
               .withCacheSourceMisses(false)
               .withCacheUniqueKey(new UniqueKey("name"))
               .withSourceUniqueKey(new UniqueKey("name"))
               .withDoCopySourcePrimaryKeyToCache(true)
            )
         );

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }

      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QPossibleValueSource defineQQQBackendPossibleValueSource()
   {
      return (new QPossibleValueSource()
         .withType(QPossibleValueSourceType.TABLE)
         .withName(QQQBackend.TABLE_NAME)
         .withTableName(QQQBackend.TABLE_NAME))
         .withOrderByField("label");
   }

}
