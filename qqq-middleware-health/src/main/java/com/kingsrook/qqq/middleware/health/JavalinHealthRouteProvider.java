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

package com.kingsrook.qqq.middleware.health;


import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthResponse;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;
import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
import io.javalin.config.JavalinConfig;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;
import static io.javalin.apibuilder.ApiBuilder.get;


/*******************************************************************************
 ** Javalin route provider for health check endpoints.
 **
 ** This provider:
 ** - Registers the /health endpoint (or custom path)
 ** - Executes all configured health indicators
 ** - Returns JSON response with overall status
 ** - Sets appropriate HTTP status code (200 for UP, 503 for DOWN)
 *******************************************************************************/
public class JavalinHealthRouteProvider implements QJavalinRouteProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(JavalinHealthRouteProvider.class);

   private QInstance qInstance;



   /*******************************************************************************
    ** Set the QInstance (called during initialization and hot-swap)
    *******************************************************************************/
   @Override
   public void setQInstance(QInstance qInstance)
   {
      this.qInstance = qInstance;
   }



   /*******************************************************************************
    ** Accept Javalin config to register routes
    *******************************************************************************/
   @Override
   public void acceptJavalinConfig(JavalinConfig config)
   {
      HealthCheckMetaData healthConfig = QSupplementalInstanceMetaData.of(qInstance, HealthCheckMetaData.METADATA_KEY);

      if(healthConfig == null || !healthConfig.getEnabled())
      {
         LOG.debug("Health check endpoint not enabled");
         return;
      }

      String endpointPath = healthConfig.getEndpointPath();
      LOG.info("Registering health check endpoint", logPair("path", endpointPath));

      config.router.apiBuilder(() ->
      {
         get(endpointPath, ctx ->
         {
            /////////////////////////////////////////////////////////
            // TODO: Implement authentication if configured        //
            // if(healthConfig.getAuthenticator() != null) { ... } //
            /////////////////////////////////////////////////////////

            ///////////////////////////
            // Execute health checks //
            ///////////////////////////
            HealthCheckExecutor executor = new HealthCheckExecutor(qInstance, healthConfig);
            try
            {
               HealthResponse response = executor.execute();

               /////////////////////////////////////////////////
               // Set HTTP status code based on health status //
               /////////////////////////////////////////////////
               if(response.getStatus() == HealthStatus.DOWN)
               {
                  ctx.status(503); // Service Unavailable
               }
               else
               {
                  ctx.status(200); // OK (for UP, DEGRADED, or UNKNOWN)
               }

               ctx.json(response);
            }
            finally
            {
               executor.shutdown();
            }
         });
      });
   }
}
