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


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.messaging.SendMessageAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Content;
import com.kingsrook.qqq.backend.core.model.actions.messaging.MultiParty;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Party;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailContentRole;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailPartyRole;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.QMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.email.EmailMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Normal configured EMAIL dispatch into an owned, non-relaying loopback receiver.
 *******************************************************************************/
@Timeout(20)
class SampleMessagingAcceptanceTest
{
   private static final String PROVIDER = "ownedMessagingAcceptance";
   private static final String SUBJECT = "Owned messaging acceptance";
   private static final String TEXT = "Hello Zoë — owned text body.";
   private static final String HTML = "<p>Hello <b>Zoë</b> — owned HTML body.</p>";
   private CapturedContext previousContext;
   private Map<String, Serializable> previousObjects;
   private QInstance instance;



   /*******************************************************************************
    ** Use sample metadata without starting its database, launcher or other services.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousContext = QContext.capture();
      previousObjects = QContext.getObjects();
      QContext.setObjects(new LinkedHashMap<>());
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
   }



   /*******************************************************************************
    ** Restore thread-owned state for later sample tests in the same JVM.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      QContext.init(previousContext);
      QContext.setObjects(previousObjects);
   }



   /*******************************************************************************
    ** SMTP envelope recipients include Bcc; MIME headers must not disclose it.
    *******************************************************************************/
   @Test
   void testConfiguredMessageEnvelopeAndBody() throws Exception
   {
      try(SmtpReceiver receiver = new SmtpReceiver(0); SmtpReceiver unrelated = new SmtpReceiver(0))
      {
         configure(PROVIDER, receiver);
         configure("unrelatedMessagingAcceptance", unrelated);
         new SendMessageAction().execute(message());

         Receipt receipt = receiver.receipt();
         assertEquals(List.of("MAIL FROM:<sender@example.invalid>", "RCPT TO:<to@example.invalid>",
            "RCPT TO:<copy@example.invalid>", "RCPT TO:<blind@example.invalid>"), receipt.envelope());
         assertTrue(receipt.accepted());
         assertEquals(1, receipt.dataCommands());
         MimeMessage mime = parse(receipt);
         assertEquals(SUBJECT, mime.getSubject());
         assertEquals("sender@example.invalid", ((InternetAddress) mime.getFrom()[0]).getAddress());
         assertEquals("Owned Sender", ((InternetAddress) mime.getFrom()[0]).getPersonal());
         assertEquals(1, mime.getRecipients(Message.RecipientType.TO).length);
         assertEquals("to@example.invalid", ((InternetAddress) mime.getRecipients(Message.RecipientType.TO)[0]).getAddress());
         assertEquals(1, mime.getRecipients(Message.RecipientType.CC).length);
         assertEquals("copy@example.invalid", ((InternetAddress) mime.getRecipients(Message.RecipientType.CC)[0]).getAddress());
         assertNull(mime.getHeader("Bcc"));
         assertFalse(receipt.data().contains("blind@example.invalid"));
         Multipart body = assertInstanceOf(Multipart.class, mime.getContent());
         assertEquals(2, body.getCount());
         assertTrue(body.getBodyPart(0).isMimeType("text/plain"));
         assertEquals(TEXT, body.getBodyPart(0).getContent());
         assertTrue(body.getBodyPart(1).isMimeType("text/html"));
         assertEquals(HTML, body.getBodyPart(1).getContent());
         unrelated.assertNoConnection();
      }
   }



   /*******************************************************************************
    ** Reconfiguration and a subsequent single recipient must not reuse old parties.
    *******************************************************************************/
   @Test
   void testSubsequentMessageDoesNotReuseRecipients() throws Exception
   {
      try(SmtpReceiver first = new SmtpReceiver(0); SmtpReceiver second = new SmtpReceiver(0); SmtpReceiver unrelated = new SmtpReceiver(0))
      {
         configure(PROVIDER, first);
         configure("unrelatedMessagingAcceptance", unrelated);
         new SendMessageAction().execute(message());
         assertTrue(first.receipt().accepted());
         ((EmailMessagingProviderMetaData) instance.getMessagingProvider(PROVIDER)).setSmtpPort(String.valueOf(second.server.getLocalPort()));
         new SendMessageAction().execute(message().withTo(new Party().withAddress("next@example.invalid")));
         Receipt receipt = second.receipt();
         assertTrue(receipt.accepted());
         assertEquals(List.of("MAIL FROM:<sender@example.invalid>", "RCPT TO:<next@example.invalid>"), receipt.envelope());
         MimeMessage mime = parse(receipt);
         assertEquals(1, mime.getAllRecipients().length);
         assertEquals("next@example.invalid", ((InternetAddress) mime.getAllRecipients()[0]).getAddress());
         assertNull(mime.getRecipients(Message.RecipientType.CC));
         assertNull(mime.getHeader("Bcc"));
         unrelated.assertNoConnection();
      }
   }



   /*******************************************************************************
    ** Invalid provider selection fails before contacting even the configured sink.
    *******************************************************************************/
   @Test
   void testMissingAndUnknownProviderConfiguration() throws Exception
   {
      try(SmtpReceiver receiver = new SmtpReceiver(0))
      {
         configure(PROVIDER, receiver);
         for(String missing : new String[] { null, "", " " })
         {
            QException failure = assertThrows(QException.class, () -> new SendMessageAction().execute(message().withMessagingProviderName(missing)));
            assertTrue(failure.getMessage().contains("provider name was not given"));
         }
         QException failure = assertThrows(QException.class, () -> new SendMessageAction().execute(message().withMessagingProviderName("unknownOwnedProvider")));
         assertTrue(failure.getMessage().contains("was not found in this QInstance"));
         receiver.assertNoConnection();
      }
   }



   /*******************************************************************************
    ** A configured but unsupported type must not fall back to a real provider.
    *******************************************************************************/
   @Test
   void testUnsupportedProviderType() throws Exception
   {
      try(SmtpReceiver receiver = new SmtpReceiver(0))
      {
         configure(PROVIDER, receiver);
         instance.addMessagingProvider(new QMessagingProviderMetaData().withName("unsupportedOwnedProvider").withType("OWNED_UNSUPPORTED"));
         QModuleDispatchException failure = assertThrows(QModuleDispatchException.class,
            () -> new SendMessageAction().execute(message().withMessagingProviderName("unsupportedOwnedProvider")));
         assertTrue(failure.getMessage().contains("Unrecognized messaging provider type [OWNED_UNSUPPORTED]"));
         receiver.assertNoConnection();
      }
   }



   /*******************************************************************************
    ** Invalid address syntax and sender-only roles cannot reach SMTP transport.
    *******************************************************************************/
   @Test
   void testInvalidReceiverRejectedBeforeTransmission() throws Exception
   {
      try(SmtpReceiver receiver = new SmtpReceiver(0))
      {
         configure(PROVIDER, receiver);
         QException roleFailure = assertThrows(QException.class, () -> new SendMessageAction().execute(message()
            .withTo(new Party().withAddress("to@example.invalid").withRole(EmailPartyRole.FROM))));
         assertInstanceOf(QException.class, roleFailure.getCause());
         assertTrue(roleFailure.getCause().getMessage().contains("Unrecognized recipient role"));
         QException addressFailure = assertThrows(QException.class, () -> new SendMessageAction().execute(message()
            .withTo(new MultiParty()
               .withParty(new Party().withAddress("to@example.invalid"))
               .withParty(new Party().withAddress("invalid@@example.invalid")))));
         assertInstanceOf(AddressException.class, addressFailure.getCause());
         receiver.assertNoConnection();
      }
   }



   /*******************************************************************************
    ** Recipient rejection must report failure without issuing DATA or falling back.
    *******************************************************************************/
   @Test
   void testReceiverRefusalDoesNotTransmitBody() throws Exception
   {
      try(SmtpReceiver receiver = new SmtpReceiver(550); SmtpReceiver unrelated = new SmtpReceiver(0))
      {
         configure(PROVIDER, receiver);
         configure("unrelatedMessagingAcceptance", unrelated);
         QException failure = assertThrows(QException.class, () -> new SendMessageAction().execute(message()
            .withTo(new Party().withAddress("rejected@example.invalid"))));
         assertEquals("Error sending email", failure.getMessage());
         assertThat(failure).hasStackTraceContaining("550 owned-recipient-rejected");
         Receipt receipt = receiver.receipt();
         assertEquals(List.of("MAIL FROM:<sender@example.invalid>", "RCPT TO:<rejected@example.invalid>"), receipt.envelope());
         assertEquals(0, receipt.dataCommands());
         assertNull(receipt.data());
         assertFalse(receipt.accepted());
         unrelated.assertNoConnection();
      }
   }



   /*******************************************************************************
    ** Even after DATA, receiver failure cannot be reported as successful delivery.
    *******************************************************************************/
   @Test
   void testDeliveryFailureAfterBodyDoesNotFallback() throws Exception
   {
      try(SmtpReceiver receiver = new SmtpReceiver(554); SmtpReceiver unrelated = new SmtpReceiver(0))
      {
         configure(PROVIDER, receiver);
         configure("unrelatedMessagingAcceptance", unrelated);
         QException failure = assertThrows(QException.class, () -> new SendMessageAction().execute(message()
            .withTo(new Party().withAddress("to@example.invalid"))));
         assertEquals("Error sending email", failure.getMessage());
         assertThat(failure).hasStackTraceContaining("554 owned-delivery-failure");
         Receipt receipt = receiver.receipt();
         assertEquals(List.of("MAIL FROM:<sender@example.invalid>", "RCPT TO:<to@example.invalid>"), receipt.envelope());
         assertEquals(1, receipt.dataCommands());
         assertEquals(SUBJECT, parse(receipt).getSubject());
         assertFalse(receipt.accepted());
         unrelated.assertNoConnection();
      }
   }



   /*******************************************************************************
    ** Host and port come only from this test's owned numeric-loopback socket.
    *******************************************************************************/
   private void configure(String name, SmtpReceiver receiver)
   {
      instance.addMessagingProvider(new EmailMessagingProviderMetaData()
         .withSmtpServer("127.0.0.1").withSmtpPort(String.valueOf(receiver.server.getLocalPort())).withName(name));
   }



   /*******************************************************************************
    ** All parties are synthetic; no credential or recipient comes from the environment.
    *******************************************************************************/
   private SendMessageInput message()
   {
      return new SendMessageInput().withMessagingProviderName(PROVIDER)
         .withFrom(new Party().withAddress("sender@example.invalid").withLabel("Owned Sender").withRole(EmailPartyRole.FROM))
         .withTo(new MultiParty()
            .withParty(new Party().withAddress("to@example.invalid").withRole(EmailPartyRole.TO))
            .withParty(new Party().withAddress("copy@example.invalid").withRole(EmailPartyRole.CC))
            .withParty(new Party().withAddress("blind@example.invalid").withRole(EmailPartyRole.BCC)))
         .withSubject(SUBJECT)
         .withContent(new Content().withContentRole(EmailContentRole.TEXT).withBody(TEXT))
         .withContent(new Content().withContentRole(EmailContentRole.HTML).withBody(HTML));
   }



   /*******************************************************************************
    ** Decode the receiver's actual wire body using the existing Jakarta Mail parser.
    *******************************************************************************/
   private MimeMessage parse(Receipt receipt) throws Exception
   {
      return new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(receipt.data().getBytes(StandardCharsets.UTF_8)));
   }



   /*******************************************************************************
    ** Accepted is separate from captured: a failed DATA request still has a body.
    *******************************************************************************/
   private record Receipt(List<String> envelope, String data, Integer dataCommands, Boolean accepted)
   {
   }



   /*******************************************************************************
    ** One bounded SMTP transaction, following the sample's existing SmtpSink pattern.
    *******************************************************************************/
   private static class SmtpReceiver implements AutoCloseable
   {
      private final ServerSocket server;
      private final CompletableFuture<Receipt> result = new CompletableFuture<>();
      private final AtomicInteger connections = new AtomicInteger();
      private final Thread worker;
      private volatile Socket connection;



      /*******************************************************************************
       ** Error codes choose deterministic local failures; nothing is forwarded.
       *******************************************************************************/
      SmtpReceiver(Integer failureCode) throws IOException
      {
         server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
         server.setSoTimeout(10000);
         worker = Thread.ofPlatform().name("owned-messaging-smtp").start(() -> receive(failureCode));
      }



      /*******************************************************************************
       ** Keep receiver failures visible to tests instead of losing worker exceptions.
       *******************************************************************************/
      private void receive(Integer failureCode)
      {
         try(Socket socket = server.accept())
         {
            connection = socket;
            connections.incrementAndGet();
            socket.setSoTimeout(5000);
            try(BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
               PrintWriter output = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8))
            {
               List<String> envelope = new ArrayList<>();
               String data = null;
               Integer dataCommands = 0;
               Boolean accepted = false;
               reply(output, "220 localhost owned acceptance");
               String line;
               while((line = input.readLine()) != null)
               {
                  if(line.equals("QUIT"))
                  {
                     reply(output, "221 closed");
                     break;
                  }
                  else if(line.startsWith("MAIL FROM:") || line.startsWith("RCPT TO:"))
                  {
                     envelope.add(line);
                     reply(output, line.startsWith("RCPT TO:") && failureCode.equals(550) ? "550 owned-recipient-rejected" : "250 accepted envelope");
                  }
                  else if(line.equals("DATA"))
                  {
                     dataCommands++;
                     reply(output, "354 send data");
                     StringBuilder body = new StringBuilder();
                     while((line = input.readLine()) != null && !line.equals("."))
                     {
                        body.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
                     }
                     if(line == null)
                     {
                        throw new IOException("Incomplete owned SMTP body");
                     }
                     data = body.toString();
                     accepted = !failureCode.equals(554);
                     reply(output, accepted ? "250 accepted message" : "554 owned-delivery-failure");
                  }
                  else if(line.startsWith("EHLO ") || line.startsWith("HELO ") || line.equals("RSET"))
                  {
                     reply(output, "250 localhost");
                  }
                  else
                  {
                     throw new IOException("Unexpected command in owned SMTP receiver");
                  }
               }
               result.complete(new Receipt(List.copyOf(envelope), data, dataCommands, accepted));
            }
         }
         catch(Exception e)
         {
            if(!server.isClosed())
            {
               result.completeExceptionally(e);
            }
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private void reply(PrintWriter output, String response)
      {
         output.print(response + "\r\n");
         output.flush();
      }



      /*******************************************************************************
       ** Wait for the complete transaction, not merely receipt of its body.
       *******************************************************************************/
      Receipt receipt() throws Exception
      {
         Receipt receipt = result.get(6, TimeUnit.SECONDS);
         assertEquals(1, connections.get());
         return receipt;
      }



      /*******************************************************************************
       ** Join before checking so an already-accepted connection cannot race the assertion.
       *******************************************************************************/
      void assertNoConnection() throws Exception
      {
         close();
         assertEquals(0, connections.get(), "Unselected or invalid message contacted a receiver");
      }



      /*******************************************************************************
       ** Close only sockets and the thread owned by this receiver.
       *******************************************************************************/
      @Override
      public void close() throws Exception
      {
         server.close();
         if(connection != null)
         {
            connection.close();
         }
         worker.join(6000);
         assertFalse(worker.isAlive(), "Owned SMTP receiver did not close");
         if(result.isCompletedExceptionally())
         {
            result.get();
         }
      }
   }
}
