/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.sampleapp;


import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.api.actions.ApiImplementation;
import com.kingsrook.qqq.api.actions.GetTableApiFieldsAction;
import com.kingsrook.qqq.api.javalin.QJavalinApiHandler;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.h2.tools.RunScript;


/*******************************************************************************
 ** Shared lifecycle for upload/download acceptance. The normal application server
 ** uses an explicitly populated private H2 database, without demo auto-priming or
 ** broker startup. Tests own their filesystem roots and request assertions.
 *******************************************************************************/
final class SampleUploadTestFixture implements AutoCloseable
{
   private final CapturedContext previousContext = QContext.capture();
   private final Map<String, Serializable> previousObjects = QContext.getObjects();
   private final QInstance previousServerInstance = QJavalinImplementation.getQInstance();
   private final Boolean previousHttpOnly = QJavalinImplementation.getSessionCookieHttpOnly();
   private Connection database;
   private final QApplicationJavalinServer server;



   /*******************************************************************************
    ** Rebind every sample RDBMS table to the same owned connection destination.
    *******************************************************************************/
   SampleUploadTestFixture(QInstance instance) throws QException
   {
      String jdbcUrl = "jdbc:h2:mem:upload_acceptance_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=0";
      try
      {
         database = DriverManager.getConnection(jdbcUrl, "sa", "");
         try(InputStreamReader reader = new InputStreamReader(SampleMetaDataProvider.class.getResourceAsStream("/prime-test-database.sql"), StandardCharsets.UTF_8))
         {
            RunScript.execute(database, reader);
         }
      }
      catch(Exception e)
      {
         if(database != null)
         {
            try
            {
               database.close();
            }
            catch(Exception closeException)
            {
               e.addSuppressed(closeException);
            }
         }
         throw new QException("Could not initialize owned upload acceptance database", e);
      }
      QContext.setObjects(new LinkedHashMap<>());
      ConnectionManager.resetConnectionProviders();
      RDBMSBackendMetaData backend = SampleMetaDataProvider.defineRdbmsBackend().withName("uploadDatabase" + UUID.randomUUID()).withJdbcUrl(jdbcUrl);
      instance.addBackend(backend);
      instance.getTables().values().forEach(table ->
      {
         if(SampleMetaDataProvider.RDBMS_BACKEND_NAME.equals(table.getBackendName()))
         {
            table.setBackendName(backend.getName());
         }
         if(table.getSupplementalMetaData() != null)
         {
            table.getSupplementalMetaData().remove(EsbTableMetaData.TYPE);
         }
      });
      instance.getSupplementalMetaData().remove(EsbInstanceMetaData.NAME);
      instance.getProcesses().values().forEach(process ->
      {
         if(process.getSupplementalMetaData() != null)
         {
            process.getSupplementalMetaData().remove(EsbProcessMetaData.TYPE);
         }
      });
      server = new QApplicationJavalinServer(new SampleMetaDataProvider()
      {
         /***************************************************************************
          ** Supply exactly the metadata whose native database this fixture owns.
          ***************************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            return instance;
         }
      });
      server.setPort(0);
      server.setServeFrontendMaterialDashboard(false);
      server.setServeFrontendNext(false);
      server.withJavalinConfigCustomizer(config -> config.jetty.host = "127.0.0.1");
   }



   /*******************************************************************************
    ** Tests configure and exercise normal server routes.
    *******************************************************************************/
   QApplicationJavalinServer server()
   {
      return server;
   }



   /*******************************************************************************
    ** Independent JDBC oracle; marker changes prove the HTTP route's data identity.
    *******************************************************************************/
   Connection database()
   {
      return database;
   }



   /*******************************************************************************
    ** Restore both the captured context and its objects map, including on failure.
    *******************************************************************************/
   @Override
   public void close() throws Exception
   {
      try
      {
         server.stop();
      }
      finally
      {
         try
         {
            try(Statement statement = database.createStatement())
            {
               statement.execute("SHUTDOWN");
            }
            finally
            {
               database.close();
            }
         }
         finally
         {
            ApiImplementation.clearCaches();
            GetTableApiFieldsAction.clearCaches();
            ConnectionManager.resetConnectionProviders();
            QJavalinImplementation.setQInstance(previousServerInstance);
            QJavalinImplementation.setSessionCookieHttpOnly(previousHttpOnly);
            new QJavalinApiHandler(previousServerInstance);
            QContext.clear();
            QContext.init(previousContext);
            QContext.setObjects(previousObjects);
         }
      }
   }
}
