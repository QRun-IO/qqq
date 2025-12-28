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

package com.kingsrook.qqq.middleware.health.indicators;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.middleware.health.HealthIndicator;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Health indicator that checks database connectivity and connection pool health.
 **
 ** This indicator:
 ** - Executes a simple query (SELECT 1) to verify database is reachable
 ** - Checks connection pool statistics if available
 ** - Reports status as UP if query succeeds, DOWN if it fails
 *******************************************************************************/
public class DatabaseHealthIndicator implements HealthIndicator
{
   private static final QLogger LOG = QLogger.getLogger(DatabaseHealthIndicator.class);

   private String  backendName;
   private Integer timeoutMs = 3000;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public DatabaseHealthIndicator()
   {
   }



   /*******************************************************************************
    ** Get the name of this health indicator
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("database");
   }



   /*******************************************************************************
    ** Execute the database health check
    *******************************************************************************/
   @Override
   public HealthCheckResult check(QInstance qInstance) throws QException
   {
      long startTime = System.currentTimeMillis();

      if(backendName == null)
      {
         return new HealthCheckResult()
            .withStatus(HealthStatus.UNKNOWN)
            .withDurationMs(System.currentTimeMillis() - startTime)
            .withDetail("error", "No backend name configured");
      }

      QBackendMetaData backend = qInstance.getBackend(backendName);
      if(backend == null)
      {
         return new HealthCheckResult()
            .withStatus(HealthStatus.DOWN)
            .withDurationMs(System.currentTimeMillis() - startTime)
            .withDetail("error", "Backend not found")
            .withDetail("backendName", backendName);
      }

      if(!(backend instanceof RDBMSBackendMetaData rdbmsBackend))
      {
         return new HealthCheckResult()
            .withStatus(HealthStatus.UNKNOWN)
            .withDurationMs(System.currentTimeMillis() - startTime)
            .withDetail("error", "Backend is not an RDBMS backend")
            .withDetail("backendType", backend.getClass().getSimpleName());
      }

      try
      {
         return checkDatabaseConnection(rdbmsBackend, startTime);
      }
      catch(Exception e)
      {
         LOG.warn("Database health check failed", logPair("backendName", backendName), e);
         return new HealthCheckResult()
            .withStatus(HealthStatus.DOWN)
            .withDurationMs(System.currentTimeMillis() - startTime)
            .withDetail("error", e.getMessage())
            .withDetail("exceptionType", e.getClass().getSimpleName());
      }
   }



   /*******************************************************************************
    ** Check database connection by executing a simple query
    *******************************************************************************/
   private HealthCheckResult checkDatabaseConnection(RDBMSBackendMetaData backend, long startTime) throws Exception
   {
      Map<String, Object> details = new HashMap<>();
      details.put("backendName", backendName);
      details.put("vendor", backend.getVendor());

      try(Connection connection = ConnectionManager.getConnection(backend))
      {
         //////////////////////////////////////////////////////////
         // Execute simple query to verify database is reachable //
         //////////////////////////////////////////////////////////
         try(PreparedStatement ps = connection.prepareStatement("SELECT 1"))
         {
            ps.setQueryTimeout(timeoutMs / 1000); // Convert to seconds

            try(ResultSet rs = ps.executeQuery())
            {
               if(rs.next())
               {
                  Long duration = System.currentTimeMillis() - startTime;

                  //////////////////////////////////////
                  // Query succeeded - database is UP //
                  //////////////////////////////////////
                  details.put("queryResult", rs.getInt(1));

                  return new HealthCheckResult()
                     .withStatus(HealthStatus.UP)
                     .withDurationMs(duration)
                     .withDetails(details);
               }
               else
               {
                  return new HealthCheckResult()
                     .withStatus(HealthStatus.DEGRADED)
                     .withDurationMs(System.currentTimeMillis() - startTime)
                     .withDetails(details)
                     .withDetail("warning", "Query returned no results");
               }
            }
         }
      }
   }



   /*******************************************************************************
    ** Fluent setter for backendName
    *******************************************************************************/
   public DatabaseHealthIndicator withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for timeoutMs
    *******************************************************************************/
   public DatabaseHealthIndicator withTimeout(Integer timeoutMs)
   {
      this.timeoutMs = timeoutMs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for backendName
    *******************************************************************************/
   public String getBackendName()
   {
      return (this.backendName);
   }



   /*******************************************************************************
    ** Setter for backendName
    *******************************************************************************/
   public void setBackendName(String backendName)
   {
      this.backendName = backendName;
   }



   /*******************************************************************************
    ** Getter for timeoutMs
    *******************************************************************************/
   public Integer getTimeoutMs()
   {
      return (this.timeoutMs);
   }



   /*******************************************************************************
    ** Setter for timeoutMs
    *******************************************************************************/
   public void setTimeoutMs(Integer timeoutMs)
   {
      this.timeoutMs = timeoutMs;
   }
}
