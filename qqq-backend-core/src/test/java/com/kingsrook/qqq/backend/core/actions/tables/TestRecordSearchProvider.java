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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Record search provider for tests: claims the tables named via claim(), and
 ** returns the results given via respondWith() (or fails, as set up), recording
 ** each input it is asked to search.  State is static (a new instance is loaded
 ** per search) - call reset() before each test.
 *******************************************************************************/
public class TestRecordSearchProvider implements RecordSearchProviderInterface
{
   private static Set<String>              claimedTableNames = new HashSet<>();
   private static List<RecordSearchResult> results           = new ArrayList<>();
   private static List<RecordSearchInput>  inputs            = new ArrayList<>();
   private static boolean                  failSearch        = false;
   private static boolean                  failClaim         = false;



   /*******************************************************************************
    ** Forget all set up and recorded state.
    *******************************************************************************/
   public static void reset()
   {
      claimedTableNames = new HashSet<>();
      results = new ArrayList<>();
      inputs = new ArrayList<>();
      failSearch = false;
      failClaim = false;
   }



   /*******************************************************************************
    ** Claim these tables.
    *******************************************************************************/
   public static void claim(String... tableNames)
   {
      claimedTableNames.addAll(List.of(tableNames));
   }



   /*******************************************************************************
    ** Respond to searches with a result for each of these table/id pairs.
    *******************************************************************************/
   public static void respondWith(String tableName, Serializable... recordIds)
   {
      for(Serializable recordId : recordIds)
      {
         results.add(new RecordSearchResult().withTableName(tableName).withRecordId(recordId).withRecordLabel("provider label"));
      }
   }



   /*******************************************************************************
    ** Make search (or claimsTable) throw.
    *******************************************************************************/
   public static void fail(boolean inSearch, boolean inClaim)
   {
      failSearch = inSearch;
      failClaim = inClaim;
   }



   /*******************************************************************************
    ** The inputs this provider was asked to search, in order.
    *******************************************************************************/
   public static List<RecordSearchInput> getInputs()
   {
      return (inputs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean claimsTable(QTableMetaData table)
   {
      if(failClaim)
      {
         throw (new IllegalStateException("test claim failure"));
      }
      return (claimedTableNames.contains(table.getName()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RecordSearchOutput search(RecordSearchInput input) throws QException
   {
      inputs.add(input);
      if(failSearch)
      {
         throw (new QException("test search index unavailable"));
      }
      return (new RecordSearchOutput().withResults(new ArrayList<>(results)));
   }

}
