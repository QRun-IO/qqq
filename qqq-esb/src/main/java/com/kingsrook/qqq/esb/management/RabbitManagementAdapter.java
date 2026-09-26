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

package com.kingsrook.qqq.esb.management;


import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import org.json.JSONException;
import org.json.JSONObject;


/*******************************************************************************
 * Broker adapter for RabbitMQ: queue info and purge through the management
 * plugin's HTTP API, on the provider's managementUrl (e.g.,
 * http://host:15672).  RabbitMQ can't pause a queue, or delete or move
 * selected messages, so those throw UnsupportedOperationException (pausing
 * QQQ's own consumers, via the trigger controls, covers QQQ's side).
 *
 * The vhost is the one in the provider's AMQP url, as the JMS client reads it:
 * the url's path, less its leading slash, decoded; no path (or just a slash)
 * is the default vhost, "/".
 *******************************************************************************/
public class RabbitManagementAdapter implements EsbBrokerAdapter
{
   public static final EsbBrokerCapabilities CAPABILITIES = new EsbBrokerCapabilities(true, true, false, true, false, false, false);

   private static final String DEFAULT_VHOST = "/";

   private final String            amqpUrl;
   private final EsbManagementHttp http;



   /*******************************************************************************
    ** Constructor - for a provider with a managementUrl.
    *******************************************************************************/
   public RabbitManagementAdapter(QEsbProviderMetaData provider)
   {
      this.amqpUrl = provider.getUrl();
      this.http = new EsbManagementHttp(provider);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public EsbBrokerCapabilities capabilities()
   {
      return (CAPABILITIES);
   }



   /*******************************************************************************
    ** GET /api/queues/VHOST/NAME: its messages (ready plus unacknowledged) and
    ** consumers.  (The API's counts are sampled, so they can lag by a few
    ** seconds.)
    *******************************************************************************/
   @Override
   public Optional<EsbQueueInfo> getQueueInfo(String brokerQueueName) throws QException
   {
      String action = "get info for queue " + brokerQueueName;

      HttpResponse<String> response = http.send(http.newRequest(queuePath(brokerQueueName)).GET().build(), action);
      if(response.statusCode() == 404)
      {
         return (Optional.empty());
      }
      requireSuccess(response, action);

      JSONObject queue;
      try
      {
         queue = new JSONObject(response.body());
      }
      catch(JSONException e)
      {
         throw (new QException("The management API of ESB provider " + http.getProviderName() + " answered with something that is not JSON (to " + action + ")"));
      }

      return (Optional.of(new EsbQueueInfo(queue.optLong("messages", 0L), queue.optInt("consumers", 0), false)));
   }



   /*******************************************************************************
    ** DELETE /api/queues/VHOST/NAME/contents.  The API doesn't say how many
    ** messages it removed, so this returns how many the queue held just before
    ** (from getQueueInfo) - which, like all the API's counts, can lag a little.
    *******************************************************************************/
   @Override
   public long purgeQueue(String brokerQueueName) throws QException
   {
      String action = "purge queue " + brokerQueueName;

      EsbQueueInfo queueInfo = getQueueInfo(brokerQueueName)
         .orElseThrow(() -> new QException("Queue " + brokerQueueName + " not found on ESB provider " + http.getProviderName() + " (to " + action + ")"));

      HttpResponse<String> response = http.send(http.newRequest(queuePath(brokerQueueName) + "/contents").DELETE().build(), action);
      if(response.statusCode() == 404)
      {
         throw (new QException("Queue " + brokerQueueName + " not found on ESB provider " + http.getProviderName() + " (to " + action + ")"));
      }
      requireSuccess(response, action);

      return (queueInfo.messageCount());
   }



   /*******************************************************************************
    ** Not supported by RabbitMQ.
    *******************************************************************************/
   @Override
   public void pauseQueue(String brokerQueueName)
   {
      throw (unsupported("pausing a queue"));
   }



   /*******************************************************************************
    ** Not supported by RabbitMQ.
    *******************************************************************************/
   @Override
   public void resumeQueue(String brokerQueueName)
   {
      throw (unsupported("resuming a queue"));
   }



   /*******************************************************************************
    ** Not supported by RabbitMQ.
    *******************************************************************************/
   @Override
   public int deleteMessages(String brokerQueueName, List<String> messageIds)
   {
      throw (unsupported("deleting selected messages"));
   }



   /*******************************************************************************
    ** Not supported by RabbitMQ.
    *******************************************************************************/
   @Override
   public int deleteMessagesOlderThan(String brokerQueueName, Instant cutoff)
   {
      throw (unsupported("deleting messages older than a time"));
   }



   /*******************************************************************************
    ** Not supported by RabbitMQ.
    *******************************************************************************/
   @Override
   public int moveMessages(String brokerQueueName, List<String> messageIds, String toBrokerQueueName)
   {
      throw (unsupported("moving messages"));
   }



   /*******************************************************************************
    ** /api/queues/VHOST/NAME, with both parts encoded as path segments.
    *******************************************************************************/
   private String queuePath(String brokerQueueName) throws QException
   {
      if(!StringUtils.hasContent(brokerQueueName))
      {
         throw (new QException("A queue name is required"));
      }

      return ("/api/queues/" + EsbManagementHttp.encodePathSegment(getVhost()) + "/" + EsbManagementHttp.encodePathSegment(brokerQueueName));
   }



   /*******************************************************************************
    ** The vhost in the provider's AMQP url (see the class comment).
    *******************************************************************************/
   private String getVhost() throws QException
   {
      if(!StringUtils.hasContent(amqpUrl))
      {
         return (DEFAULT_VHOST);
      }

      try
      {
         String path = new URI(amqpUrl).getPath();
         return (path == null || path.length() <= 1 ? DEFAULT_VHOST : path.substring(1));
      }
      catch(URISyntaxException e)
      {
         throw (new QException("Could not read the vhost from the url of ESB provider " + http.getProviderName() + " (it is not a valid AMQP URI)"));
      }
   }



   /*******************************************************************************
    ** Throw unless the response is a 2xx - with the API's reason, if it gave one.
    *******************************************************************************/
   private void requireSuccess(HttpResponse<String> response, String action) throws QException
   {
      if(response.statusCode() >= 200 && response.statusCode() < 300)
      {
         return;
      }

      String reason = response.body();
      try
      {
         JSONObject error = new JSONObject(response.body());
         reason = error.optString("reason", error.optString("error", response.body()));
      }
      catch(JSONException e)
      {
         ////////////////////////////////////////////////
         // not a JSON error body - show it as it came //
         ////////////////////////////////////////////////
      }

      throw (new QException("The management API of ESB provider " + http.getProviderName() + " answered HTTP " + response.statusCode() + " (to " + action + "): " + EsbManagementHttp.abbreviate(reason)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private UnsupportedOperationException unsupported(String what)
   {
      return (new UnsupportedOperationException("RabbitMQ does not support " + what + " (ESB provider " + http.getProviderName() + ")"));
   }

}
