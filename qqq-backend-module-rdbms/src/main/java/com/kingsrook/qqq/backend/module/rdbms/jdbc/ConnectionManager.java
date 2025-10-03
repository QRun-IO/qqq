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

package com.kingsrook.qqq.backend.module.rdbms.jdbc;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.json.JSONArray;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Class to manage access to JDBC Connections.
 **
 ** Relies heavily on RDBMSBackendMetaData.
 *******************************************************************************/
public class ConnectionManager
{
   private static final QLogger LOG = QLogger.getLogger(ConnectionManager.class);

   private static final Map<String, ConnectionProviderInterface> connectionProviderMap = new ConcurrentHashMap<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Connection getConnection(RDBMSBackendMetaData backend) throws SQLException
   {
      try
      {
         ConnectionProviderInterface connectionProvider = getConnectionProvider(backend);
         return connectionProvider.getConnection();
      }
      catch(QException qe)
      {
         throw (new SQLException("Error getting connection", qe));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ConnectionProviderInterface getConnectionProvider(RDBMSBackendMetaData backend) throws QException
   {
      String name = Objects.requireNonNullElse(backend.getName(), "");
      if(!StringUtils.hasContent(name))
      {
         throw (new QException("RDBMSBackendMetaData is missing a name"));
      }

      if(!connectionProviderMap.containsKey(name))
      {
         synchronized(connectionProviderMap)
         {
            if(!connectionProviderMap.containsKey(name))
            {
               QCodeReference connectionProviderReference = backend.getConnectionProvider();
               boolean        usingDefaultSimpleProvider  = false;
               if(connectionProviderReference == null)
               {
                  connectionProviderReference = new QCodeReference(SimpleConnectionProvider.class);
                  usingDefaultSimpleProvider = true;
               }

               LOG.info("Initializing connection provider for RDBMS backend", logPair("backendName", name), logPair("connectionProvider", connectionProviderReference.getName()), logPair("usingDefaultSimpleProvider", usingDefaultSimpleProvider));
               ConnectionProviderInterface connectionProvider = QCodeLoader.getAdHoc(ConnectionProviderInterface.class, connectionProviderReference);
               connectionProvider.init(backend);

               connectionProviderMap.put(name, connectionProvider);
            }
         }
      }

      return (connectionProviderMap.get(name));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static JSONArray dumpConnectionProviderDebug()
   {
      try
      {
         JSONArray rs = new JSONArray();
         for(Map.Entry<String, ConnectionProviderInterface> entry : connectionProviderMap.entrySet())
         {
            JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
            jsonObject.put("backendName", entry.getKey());
            jsonObject.put("connectionProviderClass", entry.getValue().getClass().getName());
            jsonObject.put("values", entry.getValue().dumpDebug());
            rs.put(jsonObject);
         }

         return (rs);
      }
      catch(Exception e)
      {
         String message = "Error dumping debug data for connection providers";
         LOG.warn(message, e);
         return (new JSONArray(new JSONObject(Map.of("error", e.getMessage()))));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getJdbcDriverClassName(RDBMSBackendMetaData backend)
   {
      if(StringUtils.hasContent(backend.getJdbcDriverClassName()))
      {
         return backend.getJdbcDriverClassName();
      }

      return switch(backend.getVendor())
      {
         case RDBMSBackendMetaData.VENDOR_MYSQL, RDBMSBackendMetaData.VENDOR_AURORA_MYSQL -> "com.mysql.cj.jdbc.Driver";
         case RDBMSBackendMetaData.VENDOR_H2 -> "org.h2.Driver";
         default -> throw (new IllegalStateException("We do not know what jdbc driver to use for vendor name [" + backend.getVendor() + "].  Try setting jdbcDriverClassName in your backend meta data."));
      };
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getJdbcUrl(RDBMSBackendMetaData backend)
   {
      if(StringUtils.hasContent(backend.getJdbcUrl()))
      {
         return backend.getJdbcUrl();
      }

      return switch(backend.getVendor())
      {
         ////////////////////////////////////////////////////////////////
         // TODO aws-mysql-jdbc driver not working when running on AWS //
         ////////////////////////////////////////////////////////////////
         // jdbcURL = "jdbc:mysql:aws://" + backend.getHostName() + ":" + backend.getPort() + "/" + backend.getDatabaseName() + "?rewriteBatchedStatements=true&zeroDateTimeBehavior=CONVERT_TO_NULL";

         case RDBMSBackendMetaData.VENDOR_AURORA_MYSQL -> "jdbc:mysql://" + backend.getHostName() + ":" + backend.getPort() + "/" + backend.getDatabaseName() + "?rewriteBatchedStatements=true&zeroDateTimeBehavior=convertToNull&useSSL=false";
         case RDBMSBackendMetaData.VENDOR_MYSQL -> "jdbc:mysql://" + backend.getHostName() + ":" + backend.getPort() + "/" + backend.getDatabaseName() + "?rewriteBatchedStatements=true&zeroDateTimeBehavior=convertToNull";
         case RDBMSBackendMetaData.VENDOR_H2 -> "jdbc:h2:" + backend.getHostName() + ":" + backend.getDatabaseName() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
         default ->
         {
            String connectionString = backend.buildConnectionString();
            if(connectionString == null)
            {
               throw new IllegalArgumentException("Unsupported rdbms backend vendor: " + backend.getVendor() + " (and null returned by backendMetaData.buildConnectionString())");
            }
            else
            {
               yield (connectionString);
            }
         }
      };
   }



   /*******************************************************************************
    ** reset the map of connection providers - not necessarily meant to be useful
    ** in production code - written for use in qqq tests.
    *******************************************************************************/
   public static void resetConnectionProviders()
   {
      connectionProviderMap.clear();
   }
}
