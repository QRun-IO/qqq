/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheOf;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheUseCase;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for CacheUtils (package-private methods, same-package test).
 **
 ** mapSourceRecordToCacheRecord — copies values, optionally strips primary key,
 ** and stamps the cachedDate field.
 **
 ** shouldCacheRecord — returns false when any use-case exclusion filter matches
 ** the record.
 *******************************************************************************/
class CacheUtilsTest extends BaseTest
{

   /*******************************************************************************
    ** All source values are copied to the cache record.
    *******************************************************************************/
   @Test
   void testMapSourceRecordToCacheRecord_copiesValues()
   {
      QTableMetaData table    = buildTable("cachedAt");
      CacheUseCase   useCase  = new CacheUseCase().withDoCopySourcePrimaryKeyToCache(true);
      QRecord        source   = new QRecord().withValue("id", 1).withValue("name", "Alice").withValue("cachedAt", null);

      QRecord result = CacheUtils.mapSourceRecordToCacheRecord(table, source, useCase);

      assertThat(result.getValueString("name")).isEqualTo("Alice");
   }



   /*******************************************************************************
    ** When doCopySourcePrimaryKeyToCache is false, the primary key field is removed
    ** from the mapped cache record.
    *******************************************************************************/
   @Test
   void testMapSourceRecordToCacheRecord_stripsPrimaryKeyWhenFlagFalse()
   {
      QTableMetaData table   = buildTable("cachedAt");
      CacheUseCase   useCase = new CacheUseCase().withDoCopySourcePrimaryKeyToCache(false);
      QRecord        source  = new QRecord().withValue("id", 99).withValue("name", "Bob").withValue("cachedAt", null);

      QRecord result = CacheUtils.mapSourceRecordToCacheRecord(table, source, useCase);

      assertFalse(result.getValues().containsKey("id"), "Primary key should be stripped");
      assertThat(result.getValueString("name")).isEqualTo("Bob");
   }



   /*******************************************************************************
    ** When doCopySourcePrimaryKeyToCache is true, the primary key value is retained.
    *******************************************************************************/
   @Test
   void testMapSourceRecordToCacheRecord_keepsPrimaryKeyWhenFlagTrue()
   {
      QTableMetaData table   = buildTable("cachedAt");
      CacheUseCase   useCase = new CacheUseCase().withDoCopySourcePrimaryKeyToCache(true);
      QRecord        source  = new QRecord().withValue("id", 7).withValue("name", "Carol").withValue("cachedAt", null);

      QRecord result = CacheUtils.mapSourceRecordToCacheRecord(table, source, useCase);

      assertThat(result.getValueInteger("id")).isEqualTo(7);
   }



   /*******************************************************************************
    ** When the table has a cachedDateFieldName, that field is stamped with a recent
    ** Instant on the mapped record.
    *******************************************************************************/
   @Test
   void testMapSourceRecordToCacheRecord_stampsCachedDateField()
   {
      Instant        before  = Instant.now().minusSeconds(2);
      QTableMetaData table   = buildTable("cachedAt");
      CacheUseCase   useCase = new CacheUseCase().withDoCopySourcePrimaryKeyToCache(false);
      QRecord        source  = new QRecord().withValue("id", 1).withValue("name", "Dave").withValue("cachedAt", null);

      QRecord result = CacheUtils.mapSourceRecordToCacheRecord(table, source, useCase);

      Instant stamped = result.getValueInstant("cachedAt");
      assertNotNull(stamped, "cachedAt should have been stamped");
      assertThat(stamped).isAfter(before);
   }



   /*******************************************************************************
    ** When the table has no cachedDateFieldName the field is not touched.
    *******************************************************************************/
   @Test
   void testMapSourceRecordToCacheRecord_noCachedDateField_doesNotStamp()
   {
      QTableMetaData table   = buildTable(null);
      CacheUseCase   useCase = new CacheUseCase().withDoCopySourcePrimaryKeyToCache(false);
      QRecord        source  = new QRecord().withValue("id", 1).withValue("name", "Eve");

      QRecord result = CacheUtils.mapSourceRecordToCacheRecord(table, source, useCase);

      assertNull(result.getValue("cachedAt"), "cachedAt should remain null when no cachedDateFieldName");
   }



   /*******************************************************************************
    ** A record that does NOT match any exclusion filter should be cached.
    *******************************************************************************/
   @Test
   void testShouldCacheRecord_noExclusionMatch_returnsTrue()
   {
      QTableMetaData table = buildTableWithExclusion("status", "inactive");
      QRecord        record = new QRecord().withValue("status", "active");

      assertTrue(CacheUtils.shouldCacheRecord(table, record));
   }



   /*******************************************************************************
    ** A record that matches an exclusion filter must NOT be cached.
    *******************************************************************************/
   @Test
   void testShouldCacheRecord_exclusionMatches_returnsFalse()
   {
      QTableMetaData table  = buildTableWithExclusion("status", "inactive");
      QRecord        record = new QRecord().withValue("status", "inactive");

      assertFalse(CacheUtils.shouldCacheRecord(table, record));
   }



   /*******************************************************************************
    ** When the table has no use cases at all, all records should be cached.
    *******************************************************************************/
   @Test
   void testShouldCacheRecord_noUseCases_returnsTrue()
   {
      QTableMetaData table  = new QTableMetaData()
         .withName("emptyCache")
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withCacheOf(new CacheOf());
      QRecord record = new QRecord().withValue("id", 1);

      assertTrue(CacheUtils.shouldCacheRecord(table, record));
   }



   /*******************************************************************************
    ** Builds a table with a CacheOf and a cachedDate field (may be null).
    *******************************************************************************/
   private QTableMetaData buildTable(String cachedDateFieldName)
   {
      QTableMetaData table = new QTableMetaData()
         .withName("cacheTable")
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withCacheOf(new CacheOf().withCachedDateFieldName(cachedDateFieldName));

      if(cachedDateFieldName != null)
      {
         table.withField(new QFieldMetaData(cachedDateFieldName, QFieldType.DATE_TIME));
      }
      return table;
   }



   /*******************************************************************************
    ** Builds a table with an exclusion filter: records where fieldName = fieldValue
    ** should be excluded from caching.
    *******************************************************************************/
   private QTableMetaData buildTableWithExclusion(String fieldName, String fieldValue)
   {
      QQueryFilter exclusionFilter = new QQueryFilter()
         .withCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, List.of(fieldValue)));

      CacheUseCase useCase = new CacheUseCase()
         .withType(CacheUseCase.Type.PRIMARY_KEY_TO_PRIMARY_KEY)
         .withExcludeRecordsMatching(List.of(exclusionFilter));

      return new QTableMetaData()
         .withName("cacheTable")
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData(fieldName, QFieldType.STRING))
         .withCacheOf(new CacheOf().withUseCase(useCase));
   }

}
