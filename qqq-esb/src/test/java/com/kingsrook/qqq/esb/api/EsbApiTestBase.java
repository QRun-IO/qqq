/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.api;


import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerHelper;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.metadata.EsbAppMetaDataProducer;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessEvent;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessPublication;
import com.kingsrook.qqq.esb.model.EsbTableEvent;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import com.kingsrook.qqq.esb.model.EsbTablePublication;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.stats.EsbStats;
import io.javalin.Javalin;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;


/*******************************************************************************
 * Base for ESB endpoint tests: a Javalin server (on a free port) with the ESB
 * route provider, an instance with ESB meta-data on the embedded broker, and
 * sessions whose permissions each test sets (see setPermissions).
 *
 * The instance: table order publishes INSERT and UPDATE to topic orderEvents
 * and INSERT to queue orderFulfillment (broker name fulfillment.orders);
 * process syncOrder is triggered by orderEvents; process fulfillOrder is
 * triggered by orderFulfillment and publishes COMPLETED to orderEvents.
 * Table customer and process plainProcess have no ESB meta-data.
 *******************************************************************************/
public class EsbApiTestBase extends EsbTestBase
{
   public static final String TABLE_NAME_CUSTOMER        = "customer";
   public static final String PROCESS_NAME_FULFILL_ORDER = "fulfillOrder";
   public static final String PROCESS_NAME_PLAIN         = "plainProcess";
   public static final String DESTINATION_ORDER_EVENTS   = "orderEvents";
   public static final String DESTINATION_FULFILLMENT    = "orderFulfillment";
   public static final String BROKER_NAME_FULFILLMENT    = "fulfillment.orders";
   public static final String TRIGGER_SYNC_ORDER         = PROCESS_NAME_SYNC_ORDER + "." + DESTINATION_ORDER_EVENTS;
   public static final String TRIGGER_FULFILL_ORDER      = PROCESS_NAME_FULFILL_ORDER + "." + DESTINATION_FULFILLMENT;
   public static final String SUBSCRIPTION_SYNC_ORDER    = PROCESS_NAME_SYNC_ORDER + "." + DESTINATION_ORDER_EVENTS;
   public static final String DEAD_LETTERS_FULFILL_ORDER = BROKER_NAME_FULFILLMENT + ".dlq";
   public static final String DEAD_LETTERS_SYNC_ORDER    = DESTINATION_ORDER_EVENTS + "." + SUBSCRIPTION_SYNC_ORDER + ".dlq";

   private static EsbRouteProvider routeProvider;
   private static Javalin          service;
   private static HttpClient       httpClient;

   protected QInstance qInstance;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeAll
   static void esbApiTestBaseBeforeAll()
   {
      routeProvider = new EsbRouteProvider();
      service = Javalin.create(config -> routeProvider.acceptJavalinConfig(config)).start(0);
      httpClient = HttpClient.newHttpClient();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterAll
   static void esbApiTestBaseAfterAll()
   {
      if(service != null)
      {
         service.stop();
         service = null;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void esbApiTestBaseBeforeEach() throws Exception
   {
      qInstance = defineApiInstance();
      routeProvider.setQInstance(qInstance);
      QContext.init(qInstance, new QSession());
      EsbStats.getInstance().reset();
      setPermissions();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void esbApiTestBaseAfterEach()
   {
      EsbConnectionManager.getInstance().closeAll();
      EsbStats.getInstance().reset();
      setPermissions();
   }



   /*******************************************************************************
    ** Build the instance described in the class comment.
    *******************************************************************************/
   public static QInstance defineApiInstance() throws Exception
   {
      QInstance qInstance = defineInstance();
      qInstance.getAuthentication().setCustomizer(new QCodeReference(PermissionsCustomizer.class));

      qInstance.addTable(new QTableMetaData()
         .withName(TABLE_NAME_CUSTOMER)
         .withBackendName(BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)));

      qInstance.addProcess(new QProcessMetaData()
         .withName(PROCESS_NAME_FULFILL_ORDER)
         .withLabel("Fulfill Order")
         .withStep(new QBackendStepMetaData().withName("fulfill").withCode(new QCodeReference(SyncOrderStep.class))));

      qInstance.addProcess(new QProcessMetaData()
         .withName(PROCESS_NAME_PLAIN)
         .withStep(new QBackendStepMetaData().withName("plain").withCode(new QCodeReference(SyncOrderStep.class))));

      EsbInstanceMetaData.of(qInstance)
         .withDestination(new QEsbDestinationMetaData()
            .withName(DESTINATION_ORDER_EVENTS)
            .withType(EsbDestinationType.TOPIC)
            .withProviderName(PROVIDER_NAME))
         .withDestination(new QEsbDestinationMetaData()
            .withName(DESTINATION_FULFILLMENT)
            .withType(EsbDestinationType.QUEUE)
            .withProviderName(PROVIDER_NAME)
            .withDestinationName(BROKER_NAME_FULFILLMENT));

      EsbTableMetaData.ofOrWithNew(qInstance.getTable(TABLE_NAME_ORDER))
         .withPublication(new EsbTablePublication().withDestinationName(DESTINATION_ORDER_EVENTS).withEvents(List.of(EsbTableEvent.INSERT, EsbTableEvent.UPDATE)))
         .withPublication(new EsbTablePublication().withDestinationName(DESTINATION_FULFILLMENT).withEvents(List.of(EsbTableEvent.INSERT)));

      EsbProcessMetaData.ofOrWithNew(qInstance.getProcess(PROCESS_NAME_SYNC_ORDER))
         .withTrigger(new EsbTrigger().withDestinationName(DESTINATION_ORDER_EVENTS));

      EsbProcessMetaData.ofOrWithNew(qInstance.getProcess(PROCESS_NAME_FULFILL_ORDER))
         .withTrigger(new EsbTrigger().withDestinationName(DESTINATION_FULFILLMENT).withConcurrency(2).withMaxAttempts(5))
         .withPublication(new EsbProcessPublication().withDestinationName(DESTINATION_ORDER_EVENTS).withEvents(List.of(EsbProcessEvent.COMPLETED)));

      MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, EsbAppMetaDataProducer.class.getPackageName());
      return (qInstance);
   }



   /*******************************************************************************
    ** Set the permissions of the sessions the server makes (none, if no args).
    *******************************************************************************/
   public static void setPermissions(String... permissions)
   {
      PermissionsCustomizer.permissions = Set.of(permissions);
   }



   /*******************************************************************************
    ** GET a path from the server.
    *******************************************************************************/
   public static HttpResponse<String> get(String path) throws Exception
   {
      HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + service.port() + path)).GET().build();
      return (httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
   }



   /*******************************************************************************
    ** GET a path from the server, assert a 200, and parse the body.
    *******************************************************************************/
   public static JSONObject getJson(String path) throws Exception
   {
      HttpResponse<String> response = get(path);
      if(response.statusCode() != 200)
      {
         throw (new AssertionError("Expected 200 from " + path + " but got " + response.statusCode() + ": " + response.body()));
      }
      return (new JSONObject(response.body()));
   }



   /*******************************************************************************
    ** Send text messages (each with the given string properties) to a queue on
    ** the embedded broker, by its broker-side name.
    *******************************************************************************/
   public static void sendToQueue(String brokerQueueName, Map<String, Serializable> properties, String... bodies) throws Exception
   {
      try(Session session = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false))
      {
         MessageProducer producer = session.createProducer(EsbConnectionManager.getInstance().resolveQueue(session, PROVIDER_NAME, brokerQueueName));
         for(String body : bodies)
         {
            Message message = session.createTextMessage(body);
            for(Map.Entry<String, Serializable> entry : properties.entrySet())
            {
               message.setObjectProperty(entry.getKey(), entry.getValue());
            }
            producer.send(message);
         }
      }
   }



   /*******************************************************************************
    * Session customizer (for the mock authentication module) that gives each
    * session the permissions the test set.
    *******************************************************************************/
   public static class PermissionsCustomizer implements QAuthenticationModuleCustomizerInterface
   {
      private static volatile Set<String> permissions = Set.of();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance qInstance, QSession qSession, Map<String, Object> context)
      {
         qSession.setPermissions(new HashSet<>(permissions));
      }
   }

}
