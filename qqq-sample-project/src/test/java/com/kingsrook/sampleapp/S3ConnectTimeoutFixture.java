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


import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.kingsrook.qqq.backend.core.actions.interfaces.QStorageInterface;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.s3.S3BackendModule;
import com.kingsrook.qqq.backend.module.filesystem.s3.actions.S3StorageAction;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3BackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3TableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.s3.utils.S3Utils;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.apache.http.conn.ConnectTimeoutException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** A Linux-only child JVM keeps backlog behavior reproducible and all sockets in
 ** its owned loopback namespace. No host network policy or external endpoint is used.
 ******************************************************************************/
public class S3ConnectTimeoutFixture
{
   private static final String NAME = "connectTimeoutFixture";
   private static AmazonS3 client;



   /*******************************************************************************
    ** Real successful connects fill the queue before native and QQQ timeout checks.
    ******************************************************************************/
   public static void main(String[] args) throws Exception
   {
      List<Socket> held = new ArrayList<>();
      try(ServerSocket listener = new ServerSocket())
      {
         listener.bind(new InetSocketAddress("127.0.0.1", 0), 1);
         InetSocketAddress address = new InetSocketAddress("127.0.0.1", listener.getLocalPort());
         boolean saturated = false;
         for(int i = 0; i < 32; i++)
         {
            Socket socket = new Socket();
            try
            {
               socket.connect(address, 150);
               held.add(socket);
            }
            catch(SocketTimeoutException expected)
            {
               socket.close();
               saturated = true;
               break;
            }
            catch(Exception failure)
            {
               socket.close();
               throw failure;
            }
         }
         assertTrue(saturated, "queue did not saturate within the bounded connection count");
         assertTrue(held.size() > 0, "native successful connections must precede the failure");
         try(Socket probe = new Socket())
         {
            assertThrows(SocketTimeoutException.class, () -> probe.connect(address, 500));
         }

         client = AmazonS3ClientBuilder.standard().withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://127.0.0.1:" + listener.getLocalPort(), "us-east-1"))
            .withPathStyleAccessEnabled(true).withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("test", "test")))
            .withClientConfiguration(new ClientConfiguration().withConnectionTimeout(500).withSocketTimeout(5000).withMaxErrorRetry(0)).build();
         QBackendModuleDispatcher.registerBackendModule(new TimeoutBackend());
         QInstance instance = SampleMetaDataProvider.defineTestInstance();
         instance.addBackend(new S3BackendMetaData().withName(NAME).withBucketName("owned-timeout-bucket").withBackendType(TimeoutBackend.class));
         instance.addTable(new QTableMetaData().withName(NAME).withBackendName(NAME).withPrimaryKeyField("fileName")
            .withField(new QFieldMetaData("fileName", QFieldType.STRING)).withField(new QFieldMetaData("contents", QFieldType.BLOB))
            .withBackendDetails(new S3TableBackendDetails().withBasePath("files").withCardinality(Cardinality.ONE).withFileNameFieldName("fileName").withContentsFieldName("contents")));
         QContext.init(instance, new QSession());
         long started = System.nanoTime();
         QException failure = assertThrows(QException.class, () -> new StorageAction().getInputStream(new StorageInput(NAME).withReference("must-not-write")));
         Throwable cause = failure;
         while(!(cause instanceof ConnectTimeoutException) && cause.getCause() != null)
         {
            cause = cause.getCause();
         }
         assertInstanceOf(ConnectTimeoutException.class, cause, "must fail while connecting, not reading or by refusal");
         assertInstanceOf(SocketTimeoutException.class, cause.getCause());
         assertTrue(Duration.ofNanos(System.nanoTime() - started).compareTo(Duration.ofSeconds(5)) < 0);

         listener.setSoTimeout(1000);
         for(int i = 0; i < held.size(); i++)
         {
            try(Socket accepted = listener.accept())
            {
               assertEquals(listener.getLocalPort(), accepted.getLocalPort());
            }
         }
         try(Socket recovered = new Socket())
         {
            recovered.connect(address, 500);
            assertTrue(recovered.isConnected(), "draining the same listener must restore native connectivity");
         }
         System.out.println("QQQ_CONNECT_TIMEOUT_PASS native=SocketTimeoutException sdk=ConnectTimeoutException recovery=same-listener");
      }
      finally
      {
         try
         {
            for(Socket socket : held)
            {
               socket.close();
            }
         }
         finally
         {
            if(client != null)
            {
               client.shutdown();
            }
            QContext.clear();
         }
      }
   }



   /*******************************************************************************
    ** Only the endpoint and SDK fixture deadlines differ from the real S3 action.
    ******************************************************************************/
   public static class TimeoutBackend extends S3BackendModule
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getBackendType()
      {
         return NAME;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public QStorageInterface getStorageInterface()
      {
         S3Utils utils = new S3Utils();
         utils.setAmazonS3(client);
         S3StorageAction action = new S3StorageAction();
         action.setS3Utils(utils);
         return action;
      }
   }
}
