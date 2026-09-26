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

package com.kingsrook.qqq.esb.connection;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Long-lived JMS connections to ESB providers (brokers): one connection per
 * provider, shared by every session opened on it.
 *
 * A provider's connection is made, from the provider's meta-data in the
 * QContext's instance, on the first openSession for that provider, and is kept
 * until closeAll.  Each openSession call opens a new session on it; callers
 * close the sessions they open.
 *
 * Connecting waits at most EsbConnectionFactoryBuilder.CONNECT_TIMEOUT_MS
 * (5 s).  When a connection can't be made, or is lost (reported to the JMS
 * ExceptionListener), the manager reconnects in the background with backoff
 * (1 s, doubling up to 30 s), then fires the provider's connection listeners,
 * so the owners of sessions and consumers (which died with the old connection)
 * can open new ones.  While it is reconnecting, openSession fails at once
 * rather than waiting - so a broker that is down holds up only the first
 * attempt (by at most the connect timeout), not every one after it.
 *******************************************************************************/
public final class EsbConnectionManager
{
   private static final QLogger LOG = QLogger.getLogger(EsbConnectionManager.class);

   private static final Long INITIAL_RECONNECT_DELAY_MS = 1000L;
   private static final Long MAX_RECONNECT_DELAY_MS     = 30_000L;

   private static final EsbConnectionManager INSTANCE = new EsbConnectionManager();

   private final Map<String, ProviderConnection> providerConnections = new ConcurrentHashMap<>();
   private final Map<String, List<Runnable>>     connectionListeners = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** Singleton - use getInstance.
    *******************************************************************************/
   private EsbConnectionManager()
   {
   }



   /*******************************************************************************
    ** Get the singleton instance.
    *******************************************************************************/
   public static EsbConnectionManager getInstance()
   {
      return (INSTANCE);
   }



   /*******************************************************************************
    ** Open a new session on a provider's connection - connecting first, if this is
    ** the provider's first session.  A transacted session sends and receives when
    ** it commits; a non-transacted one auto-acknowledges.
    **
    ** Throws QException if the provider is unknown, its broker client isn't on
    ** the classpath, or it can't be connected to (then the manager keeps trying
    ** in the background - see the class comment).
    *******************************************************************************/
   public Session openSession(String providerName, boolean transacted) throws QException
   {
      return (getOrCreateProviderConnection(providerName).openSession(transacted));
   }



   /*******************************************************************************
    ** The JMS Queue or Topic for an ESB destination, on a session from
    ** openSession for the destination's provider.
    *******************************************************************************/
   public Destination resolve(Session session, QEsbDestinationMetaData destination) throws JMSException
   {
      return (getDestinationResolver(destination.getProviderName()).resolve(session, destination));
   }



   /*******************************************************************************
    ** The JMS Queue for a broker-side queue name (e.g., a dead-letter queue), on a
    ** session from openSession for the provider.
    *******************************************************************************/
   public Queue resolveQueue(Session session, String providerName, String brokerQueueName) throws JMSException
   {
      return (getDestinationResolver(providerName).resolveQueue(session, brokerQueueName));
   }



   /*******************************************************************************
    ** Whether the provider has a live connection (false if it was never
    ** connected, or is reconnecting).
    *******************************************************************************/
   public boolean isConnected(String providerName)
   {
      ProviderConnection providerConnection = getProviderConnection(providerName);
      return (providerConnection != null && providerConnection.isConnected());
   }



   /*******************************************************************************
    ** Add a listener to run each time the manager (re)connects to the provider in
    ** the background - after a lost connection, or after a first connect that
    ** failed.  Listeners run on the reconnecting thread (which has no QContext);
    ** one that throws is logged, and doesn't stop the others.
    *******************************************************************************/
   public void addConnectionListener(String providerName, Runnable onReconnect)
   {
      connectionListeners.computeIfAbsent(providerName, name -> new CopyOnWriteArrayList<>()).add(onReconnect);
   }



   /*******************************************************************************
    ** Close every provider's connection (and so every session on them), stop any
    ** background reconnecting, and remove all connection listeners - a full
    ** reset.  The next openSession connects anew.
    *******************************************************************************/
   public void closeAll()
   {
      List<ProviderConnection> providerConnectionsToClose;
      synchronized(this)
      {
         providerConnectionsToClose = new ArrayList<>(providerConnections.values());
         providerConnections.clear();
         connectionListeners.clear();
      }

      for(ProviderConnection providerConnection : providerConnectionsToClose)
      {
         providerConnection.close();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ProviderConnection getProviderConnection(String providerName)
   {
      return (providerName == null ? null : providerConnections.get(providerName));
   }



   /*******************************************************************************
    ** Get the provider's connection state - making it (from the provider's
    ** meta-data), if this is the provider's first use.
    *******************************************************************************/
   private synchronized ProviderConnection getOrCreateProviderConnection(String providerName) throws QException
   {
      ProviderConnection providerConnection = getProviderConnection(providerName);
      if(providerConnection != null)
      {
         return (providerConnection);
      }

      QEsbProviderMetaData provider = getProviderMetaData(providerName);
      if(provider.getType() == null)
      {
         throw (new QException("ESB provider " + providerName + " has no type"));
      }

      EsbConnectionFactoryBuilder connectionFactoryBuilder = EsbConnectionFactoryBuilder.forProviderType(provider.getType(), EsbConnectionManager.class.getClassLoader());

      ConnectionFactory connectionFactory;
      try
      {
         connectionFactory = connectionFactoryBuilder.buildConnectionFactory(provider);
      }
      catch(JMSException | RuntimeException e)
      {
         throw (new QException("Could not set up a connection factory for ESB provider " + providerName, e));
      }

      providerConnection = new ProviderConnection(providerName, connectionFactoryBuilder, connectionFactory, () -> fireConnectionListeners(providerName));
      providerConnections.put(providerName, providerConnection);
      return (providerConnection);
   }



   /*******************************************************************************
    ** Look up a provider in the QContext's instance.
    *******************************************************************************/
   private static QEsbProviderMetaData getProviderMetaData(String providerName) throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      if(qInstance == null)
      {
         throw (new QException("There is no QInstance in context, to look up ESB provider " + providerName));
      }

      EsbInstanceMetaData  esbInstanceMetaData = QSupplementalInstanceMetaData.of(qInstance, EsbInstanceMetaData.NAME);
      QEsbProviderMetaData provider            = esbInstanceMetaData == null ? null : esbInstanceMetaData.getProvider(providerName);
      if(provider == null)
      {
         throw (new QException("Unknown ESB provider: " + providerName));
      }

      return (provider);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private EsbDestinationResolver getDestinationResolver(String providerName) throws JMSException
   {
      ProviderConnection providerConnection = getProviderConnection(providerName);
      if(providerConnection == null)
      {
         throw (new jakarta.jms.IllegalStateException("There is no open connection for ESB provider: " + providerName));
      }
      return (providerConnection.getDestinationResolver());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void fireConnectionListeners(String providerName)
   {
      for(Runnable listener : connectionListeners.getOrDefault(providerName, List.of()))
      {
         try
         {
            listener.run();
         }
         catch(Exception e)
         {
            LOG.warn("Error in an ESB connection listener", e, logPair("providerName", providerName));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void closeQuietly(AutoCloseable closeable)
   {
      try
      {
         closeable.close();
      }
      catch(Exception e)
      {
         LOG.debug("Error closing an ESB connection or session", e);
      }
   }



   /*******************************************************************************
    * One provider's connection, and its background reconnecting.
    *
    * connection is null while there is none: before the first connect, while
    * reconnecting (reconnectThread is then set), and after close.
    *******************************************************************************/
   private static class ProviderConnection
   {
      private final String                      providerName;
      private final EsbConnectionFactoryBuilder connectionFactoryBuilder;
      private final ConnectionFactory           connectionFactory;
      private final EsbDestinationResolver      destinationResolver;
      private final Runnable                    onReconnect;

      private Connection connection;
      private Thread     reconnectThread;
      private Boolean    closed = false;



      /*******************************************************************************
       ** Constructor
       *******************************************************************************/
      ProviderConnection(String providerName, EsbConnectionFactoryBuilder connectionFactoryBuilder, ConnectionFactory connectionFactory, Runnable onReconnect)
      {
         this.providerName = providerName;
         this.connectionFactoryBuilder = connectionFactoryBuilder;
         this.connectionFactory = connectionFactory;
         this.destinationResolver = new EsbDestinationResolver(connectionFactoryBuilder);
         this.onReconnect = onReconnect;
      }



      /*******************************************************************************
       ** Open a session on the connection (connecting first, if there is none and
       ** no reconnect is under way).
       *******************************************************************************/
      Session openSession(boolean transacted) throws QException
      {
         Connection currentConnection = getOrConnect();

         Session session;
         try
         {
            session = currentConnection.createSession(transacted, transacted ? Session.SESSION_TRANSACTED : Session.AUTO_ACKNOWLEDGE);
         }
         catch(JMSException | RuntimeException e)
         {
            ///////////////////////////////////////////////////////////////////////
            // a connection that can't make sessions is treated as lost - it may //
            // have failed before (or without) telling its ExceptionListener     //
            ///////////////////////////////////////////////////////////////////////
            onConnectionLost(currentConnection, e);
            throw (new QException("Could not open a session on ESB provider " + providerName, e));
         }

         try
         {
            connectionFactoryBuilder.configureSession(session);
            return (session);
         }
         catch(JMSException | RuntimeException e)
         {
            closeQuietly(session);
            throw (new QException("Could not configure a session on ESB provider " + providerName, e));
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      EsbDestinationResolver getDestinationResolver()
      {
         return (destinationResolver);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      synchronized Boolean isConnected()
      {
         return (connection != null);
      }



      /*******************************************************************************
       ** Close the connection, and stop any reconnecting.  Final.
       *******************************************************************************/
      void close()
      {
         Connection connectionToClose;
         Thread     threadToStop;
         synchronized(this)
         {
            closed = true;
            connectionToClose = connection;
            connection = null;
            threadToStop = reconnectThread;
            reconnectThread = null;
         }

         if(threadToStop != null)
         {
            threadToStop.interrupt();
         }

         if(connectionToClose != null)
         {
            closeQuietly(connectionToClose);
         }

         if(connectionFactory instanceof AutoCloseable closeableConnectionFactory)
         {
            closeQuietly(closeableConnectionFactory);
         }
      }



      /*******************************************************************************
       ** The live connection - or a new one, if there's none and no reconnect is
       ** under way.  If connecting fails, start reconnecting in the background.
       *******************************************************************************/
      private synchronized Connection getOrConnect() throws QException
      {
         if(closed)
         {
            throw (new QException("The connection to ESB provider " + providerName + " has been closed"));
         }

         if(connection != null)
         {
            return (connection);
         }

         if(reconnectThread != null)
         {
            throw (new QException("ESB provider " + providerName + " is not connected (reconnecting in the background)"));
         }

         try
         {
            connection = createStartedConnection();
            LOG.info("Connected to ESB provider", logPair("providerName", providerName));
            return (connection);
         }
         catch(JMSException | RuntimeException e)
         {
            LOG.warn("Could not connect to ESB provider; will keep trying in the background", e, logPair("providerName", providerName));
            startReconnecting(null);
            throw (new QException("Could not connect to ESB provider " + providerName, e));
         }
      }



      /*******************************************************************************
       ** Make, and start, a connection - whose loss is reported to
       ** onConnectionLost.
       *******************************************************************************/
      private Connection createStartedConnection() throws JMSException
      {
         Connection newConnection = connectionFactory.createConnection();
         try
         {
            newConnection.setExceptionListener(exception -> onConnectionLost(newConnection, exception));
            newConnection.start();
            return (newConnection);
         }
         catch(JMSException | RuntimeException e)
         {
            closeQuietly(newConnection);
            throw (e);
         }
      }



      /*******************************************************************************
       ** Called (e.g., by the JMS client) when a connection fails.  Only acts if
       ** that connection is the current one (not an old one, or a duplicate
       ** report).
       *******************************************************************************/
      private synchronized void onConnectionLost(Connection failedConnection, Exception exception)
      {
         if(closed || connection != failedConnection)
         {
            return;
         }

         LOG.warn("Lost connection to ESB provider; reconnecting", exception, logPair("providerName", providerName));
         connection = null;
         startReconnecting(failedConnection);
      }



      /*******************************************************************************
       ** Start the background reconnect thread (if it isn't running).  Called
       ** while holding this object's lock.
       *******************************************************************************/
      private void startReconnecting(Connection failedConnection)
      {
         if(reconnectThread != null)
         {
            return;
         }

         reconnectThread = Thread.ofPlatform()
            .daemon()
            .name("qqq-esb-reconnect-" + providerName)
            .start(() -> reconnect(failedConnection));
      }



      /*******************************************************************************
       ** Body of the reconnect thread: close the failed connection (off the JMS
       ** client's thread), then try to connect - waiting 1 s before the first try,
       ** doubling up to 30 s - until connected (then fire the listeners), or closed.
       *******************************************************************************/
      private void reconnect(Connection failedConnection)
      {
         if(failedConnection != null)
         {
            closeQuietly(failedConnection);
         }

         Long delayMs = INITIAL_RECONNECT_DELAY_MS;
         while(true)
         {
            try
            {
               Thread.sleep(delayMs);
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
               return;
            }

            if(isClosed())
            {
               return;
            }

            try
            {
               Connection newConnection = createStartedConnection();
               if(!installReconnectedConnection(newConnection))
               {
                  closeQuietly(newConnection);
                  return;
               }

               LOG.info("Reconnected to ESB provider", logPair("providerName", providerName));
               onReconnect.run();
               return;
            }
            catch(JMSException | RuntimeException e)
            {
               delayMs = Math.min(delayMs * 2, MAX_RECONNECT_DELAY_MS);
               LOG.info("Could not reconnect to ESB provider; will try again", logPair("providerName", providerName), logPair("retryInMs", delayMs), logPair("error", e.getMessage()));
            }
         }
      }



      /*******************************************************************************
       ** Make a reconnected connection the current one, and end the reconnect -
       ** unless closed meanwhile (then return false).
       *******************************************************************************/
      private synchronized Boolean installReconnectedConnection(Connection newConnection)
      {
         if(closed)
         {
            return (false);
         }

         connection = newConnection;
         reconnectThread = null;
         return (true);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private synchronized Boolean isClosed()
      {
         return (closed);
      }
   }

}
