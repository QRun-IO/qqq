/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.actions;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiOperation;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Concurrent first requests must safely publish each API version's table map.
 *******************************************************************************/
class ApiImplementationConcurrencyTest extends BaseTest
{
   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testConcurrentApiVersionLookups() throws Exception
   {
      var context = new CapturedContext(QContext.getQInstance(), QContext.getQSession());
      // Distinct API names with equal hashes exercise concurrent bucket insertions.
      List<ApiInstanceMetaData> apis = new ArrayList<>();
      for(String name : List.of("Aa", "BB"))
      {
         apis.add(new ApiInstanceMetaData().withName(name)
            .withSupportedVersions(List.of(new APIVersion(TestUtils.V2022_Q4), new APIVersion(TestUtils.V2023_Q1))));
         ApiTableMetaDataContainer.of(context.qInstance().getTable(TestUtils.TABLE_NAME_PERSON))
            .withApiTableMetaData(name, new ApiTableMetaData().withInitialVersion(TestUtils.V2022_Q4));
      }
      var executor = Executors.newFixedThreadPool(8);
      try
      {
         for(int round = 0; round < 1000; round++)
         {
            ApiImplementation.clearCaches();
            CountDownLatch ready = new CountDownLatch(8);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> requests = new ArrayList<>();
            for(int index = 0; index < 8; index++)
            {
               ApiInstanceMetaData api = apis.get(index % 2);
               String version = index % 4 < 2 ? TestUtils.V2022_Q4 : TestUtils.V2023_Q1;
               requests.add(executor.submit(() ->
               {
                  ready.countDown();
                  if(!start.await(5, TimeUnit.SECONDS))
                  {
                     throw new IllegalStateException("Concurrent requests did not start");
                  }
                  QContext.withTemporaryContext(context, () -> assertSame(context.qInstance().getTable(TestUtils.TABLE_NAME_PERSON),
                     ApiImplementation.validateTableAndVersion(api, version, TestUtils.TABLE_NAME_PERSON, ApiOperation.QUERY_BY_QUERY_STRING)));
                  return null;
               }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            for(Future<?> request : requests)
            {
               request.get(5, TimeUnit.SECONDS);
            }
         }
      }
      finally
      {
         executor.shutdownNow();
         ApiImplementation.clearCaches();
      }
   }
}
