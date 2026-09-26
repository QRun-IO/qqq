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

package com.kingsrook.qqq.backend.module.rdbms.jdbc;


import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.mchange.v2.c3p0.ConnectionCustomizer;


/*******************************************************************************
 ** Basic version of a C3P0 Connection Customizer used by QQQ - that does things
 ** expected for an RDBMS backend - specifically:
 ** - runs queriesForNewConnections, if they are set.
 *******************************************************************************/
public class BaseC3P0ConnectionCustomizer implements ConnectionCustomizer
{
   private static final QLogger LOG = QLogger.getLogger(BaseC3P0ConnectionCustomizer.class);

   private static Map<String, List<String>> queriesForNewConnections = new HashMap<>();



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void onAcquire(Connection connection, String dataSourceIdentityToken) throws Exception
   {
      try
      {
         List<String> queries = queriesForNewConnections.get(dataSourceIdentityToken);
         if(CollectionUtils.nullSafeHasContents(queries))
         {
            for(String sql : queries)
            {
               Statement statement = connection.createStatement();
               statement.execute(sql);
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Exception on a query-for-new-connection", e);
         throw (e);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void onDestroy(Connection connection, String dataSourceIdentityToken) throws Exception
   {
      //////////
      // noop //
      //////////
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void onCheckOut(Connection connection, String dataSourceIdentityToken) throws Exception
   {
      //////////
      // noop //
      //////////
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void onCheckIn(Connection connection, String dataSourceIdentityToken) throws Exception
   {
      //////////
      // noop //
      //////////
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static void setQueriesForNewConnections(String backendName, List<String> queriesForNewConnections)
   {
      BaseC3P0ConnectionCustomizer.queriesForNewConnections.put(backendName, queriesForNewConnections);
   }

}
