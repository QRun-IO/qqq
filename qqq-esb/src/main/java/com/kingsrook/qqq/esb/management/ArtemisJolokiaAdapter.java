/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.management;


import javax.management.ObjectName;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/*******************************************************************************
 * Broker adapter for ActiveMQ Artemis: runs the queue's QueueControl MBean
 * operations through the web console's Jolokia API (POSTs under
 * /console/jolokia/ on the provider's managementUrl - e.g.,
 * http://host:8161).  Artemis supports every action.
 *
 * Each call first searches for the queue's MBean by queue name (so it needn't
 * know the broker name, address, or routing type - a subscription queue's
 * address is its topic), then reads or execs on it.  A queue name may also be
 * fully qualified, ADDRESS::QUEUE (as EsbBrokerNames names subscription
 * queues); then the search matches the address too.
 *
 * Messages are selected by Artemis core filters: by JMS message id
 * (AMQUserID), or by JMS timestamp (AMQTimestamp).  Ids are quoted literals in
 * the filter, so no id can widen it; and with no ids, nothing is sent - an
 * empty filter would select every message.
 *
 * Requests carry the managementUrl's origin as their Origin header, as the
 * console's own browser requests do: Artemis's default Jolokia access policy
 * rejects requests whose Origin isn't allowed (see jolokia-access.xml).  The
 * managementUsername needs a role the console lets manage queues.
 *******************************************************************************/
public class ArtemisJolokiaAdapter implements EsbBrokerAdapter
{
   public static final EsbBrokerCapabilities CAPABILITIES = new EsbBrokerCapabilities(true, true, true, true, true, true, true);

   static final String JOLOKIA_PATH = "/console/jolokia/";

   private static final String       MBEAN_DOMAIN          = "org.apache.activemq.artemis";
   private static final List<String> QUEUE_INFO_ATTRIBUTES = List.of("MessageCount", "ConsumerCount", "Paused");

   private final EsbManagementHttp http;



   /*******************************************************************************
    ** Constructor - for a provider with a managementUrl.
    *******************************************************************************/
   public ArtemisJolokiaAdapter(QEsbProviderMetaData provider)
   {
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
    ** Reads the queue MBean's MessageCount, ConsumerCount, and Paused attributes.
    *******************************************************************************/
   @Override
   public Optional<EsbQueueInfo> getQueueInfo(String brokerQueueName) throws QException
   {
      Optional<String> queueMBean = findQueueMBean(brokerQueueName);
      if(queueMBean.isEmpty())
      {
         return (Optional.empty());
      }

      JSONObject request = new JSONObject()
         .put("type", "read")
         .put("mbean", queueMBean.get())
         .put("attribute", new JSONArray(QUEUE_INFO_ATTRIBUTES));

      JSONObject attributes = callJolokia(request, "get info for queue " + brokerQueueName).optJSONObject("value");
      if(attributes == null)
      {
         throw (new QException("Jolokia on ESB provider " + http.getProviderName() + " gave no attributes for queue " + brokerQueueName));
      }

      return (Optional.of(new EsbQueueInfo(attributes.optLong("MessageCount"), attributes.optInt("ConsumerCount"), attributes.optBoolean("Paused"))));
   }



   /*******************************************************************************
    ** QueueControl.pause() - not persisted, so a broker restart resumes it.
    *******************************************************************************/
   @Override
   public void pauseQueue(String brokerQueueName) throws QException
   {
      execOnQueue(brokerQueueName, "pause()", "pause queue " + brokerQueueName);
   }



   /*******************************************************************************
    ** QueueControl.resume()
    *******************************************************************************/
   @Override
   public void resumeQueue(String brokerQueueName) throws QException
   {
      execOnQueue(brokerQueueName, "resume()", "resume queue " + brokerQueueName);
   }



   /*******************************************************************************
    ** QueueControl.removeAllMessages()
    *******************************************************************************/
   @Override
   public long purgeQueue(String brokerQueueName) throws QException
   {
      String action = "purge queue " + brokerQueueName;
      return (toCount(execOnQueue(brokerQueueName, "removeAllMessages()", action), action));
   }



   /*******************************************************************************
    ** QueueControl.removeMessages(filter), by message id.
    *******************************************************************************/
   @Override
   public int deleteMessages(String brokerQueueName, List<String> messageIds) throws QException
   {
      String filter = messageIdFilter(messageIds);
      if(filter == null)
      {
         return (0);
      }

      String action = "delete messages from queue " + brokerQueueName;
      return (toCount(execOnQueue(brokerQueueName, "removeMessages(java.lang.String)", action, filter), action).intValue());
   }



   /*******************************************************************************
    ** QueueControl.removeMessages(filter), by timestamp.
    *******************************************************************************/
   @Override
   public int deleteMessagesOlderThan(String brokerQueueName, Instant cutoff) throws QException
   {
      if(cutoff == null)
      {
         throw (new QException("A cutoff time is required, to delete older messages"));
      }

      String action = "delete old messages from queue " + brokerQueueName;
      return (toCount(execOnQueue(brokerQueueName, "removeMessages(java.lang.String)", action, olderThanFilter(cutoff)), action).intValue());
   }



   /*******************************************************************************
    ** QueueControl.moveMessages(filter, otherQueueName), by message id.
    *******************************************************************************/
   @Override
   public int moveMessages(String brokerQueueName, List<String> messageIds, String toBrokerQueueName) throws QException
   {
      if(!StringUtils.hasContent(toBrokerQueueName))
      {
         throw (new QException("A queue to move the messages to is required"));
      }

      String filter = messageIdFilter(messageIds);
      if(filter == null)
      {
         return (0);
      }

      String action = "move messages from queue " + brokerQueueName + " to " + toBrokerQueueName;
      return (toCount(execOnQueue(brokerQueueName, "moveMessages(java.lang.String,java.lang.String)", action, filter, queuePart(toBrokerQueueName)), action).intValue());
   }



   /*******************************************************************************
    ** The queue's own name, from a queue name that may be fully qualified
    ** (ADDRESS::QUEUE) - Artemis queue names are broker-wide, so QueueControl
    ** names queues by that alone.
    *******************************************************************************/
   private static String queuePart(String brokerQueueName)
   {
      Integer separatorIndex = brokerQueueName.indexOf(EsbBrokerNames.ARTEMIS_FQQN_SEPARATOR);
      return (separatorIndex < 0 ? brokerQueueName : brokerQueueName.substring(separatorIndex + EsbBrokerNames.ARTEMIS_FQQN_SEPARATOR.length()));
   }



   /*******************************************************************************
    ** An Artemis core filter selecting messages by JMS message id - or null if
    ** there are no (non-blank) ids.  Each id is a quoted string literal (quotes
    ** doubled), so it can only ever match an id.
    *******************************************************************************/
   static String messageIdFilter(List<String> messageIds)
   {
      if(messageIds == null)
      {
         return (null);
      }

      List<String> literals = messageIds.stream()
         .filter(StringUtils::hasContent)
         .map(messageId -> "'" + messageId.replace("'", "''") + "'")
         .toList();

      return (literals.isEmpty() ? null : "AMQUserID IN (" + String.join(", ", literals) + ")");
   }



   /*******************************************************************************
    ** An Artemis core filter selecting messages with a JMS timestamp before the
    ** cutoff.
    *******************************************************************************/
   static String olderThanFilter(Instant cutoff)
   {
      return ("AMQTimestamp < " + cutoff.toEpochMilli());
   }



   /*******************************************************************************
    ** Exec an operation (by its JMX signature, as some QueueControl operations
    ** are overloaded) on a queue's MBean; returns the operation's result.
    *******************************************************************************/
   private Object execOnQueue(String brokerQueueName, String operation, String action, Object... arguments) throws QException
   {
      String queueMBean = findQueueMBean(brokerQueueName)
         .orElseThrow(() -> new QException("Queue " + brokerQueueName + " not found on ESB provider " + http.getProviderName() + " (to " + action + ")"));

      JSONObject request = new JSONObject()
         .put("type", "exec")
         .put("mbean", queueMBean)
         .put("operation", operation)
         .put("arguments", new JSONArray(Arrays.asList(arguments)));

      return (callJolokia(request, action).opt("value"));
   }



   /*******************************************************************************
    ** The queue's MBean name (from a Jolokia search by queue name - and address,
    ** for a fully qualified ADDRESS::QUEUE name), or empty if the broker has no
    ** such queue.  More than one match (e.g., the same queue name on two brokers
    ** in one JVM) is an error, rather than act on either.
    *******************************************************************************/
   private Optional<String> findQueueMBean(String brokerQueueName) throws QException
   {
      if(!StringUtils.hasContent(brokerQueueName))
      {
         throw (new QException("A queue name is required"));
      }

      Integer separatorIndex = brokerQueueName.indexOf(EsbBrokerNames.ARTEMIS_FQQN_SEPARATOR);
      String  addressKey     = separatorIndex < 0 ? "" : "address=" + ObjectName.quote(brokerQueueName.substring(0, separatorIndex)) + ",";

      JSONObject request = new JSONObject()
         .put("type", "search")
         .put("mbean", MBEAN_DOMAIN + ":component=addresses," + addressKey + "subcomponent=queues,queue=" + ObjectName.quote(queuePart(brokerQueueName)) + ",*");

      JSONArray queueMBeans = callJolokia(request, "find queue " + brokerQueueName).optJSONArray("value");
      if(queueMBeans == null || queueMBeans.isEmpty())
      {
         return (Optional.empty());
      }

      if(queueMBeans.length() > 1)
      {
         throw (new QException("Queue name " + brokerQueueName + " matches more than one queue on ESB provider " + http.getProviderName() + ": " + EsbManagementHttp.abbreviate(queueMBeans.toString())));
      }

      return (Optional.of(queueMBeans.getString(0)));
   }



   /*******************************************************************************
    ** POST one Jolokia request (to /console/jolokia/TYPE - Jolokia reads the
    ** request from the body; the path's last part just names it in access
    ** logs); returns Jolokia's response, if its status is 200.
    *******************************************************************************/
   private JSONObject callJolokia(JSONObject request, String action) throws QException
   {
      HttpRequest httpRequest = http.newRequest(JOLOKIA_PATH + request.getString("type"))
         .header("Content-Type", "application/json")
         .header("Origin", http.getOrigin())
         .POST(HttpRequest.BodyPublishers.ofString(request.toString(), StandardCharsets.UTF_8))
         .build();

      HttpResponse<String> httpResponse = http.send(httpRequest, action);
      if(httpResponse.statusCode() != 200)
      {
         throw (new QException("Jolokia on ESB provider " + http.getProviderName() + " answered HTTP " + httpResponse.statusCode() + " (to " + action + ")"
            + (httpResponse.statusCode() == 401 || httpResponse.statusCode() == 403 ? " - check the management credentials, and that the console allows this origin" : "")));
      }

      JSONObject response;
      try
      {
         response = new JSONObject(httpResponse.body());
      }
      catch(JSONException e)
      {
         throw (new QException("Jolokia on ESB provider " + http.getProviderName() + " answered with something that is not JSON (to " + action + ")"));
      }

      //////////////////////////////////////////////////////////////////
      // Jolokia reports its errors (e.g., no such MBean) in the body //
      // with an HTTP 200, so its own status is the one to check      //
      //////////////////////////////////////////////////////////////////
      Integer status = response.optInt("status", -1);
      if(!status.equals(200))
      {
         throw (new QException("Jolokia on ESB provider " + http.getProviderName() + " could not " + action + " (status " + status + "): "
            + EsbManagementHttp.abbreviate(response.optString("error", "no error message"))));
      }

      return (response);
   }



   /*******************************************************************************
    ** A count returned by a QueueControl operation.
    *******************************************************************************/
   private Long toCount(Object value, String action) throws QException
   {
      if(value instanceof Number number)
      {
         return (number.longValue());
      }

      throw (new QException("Jolokia on ESB provider " + http.getProviderName() + " returned no count (to " + action + ")"));
   }

}
