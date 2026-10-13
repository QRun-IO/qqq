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

package com.kingsrook.qqq.backend.core.utils.http;


import javax.net.ssl.SSLException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.http.Header;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.ssl.SSLContexts;


/*******************************************************************************
 ** An HTTP client for calling URLs that users typed in (webhook endpoints,
 ** callbacks), which must not be usable to reach the internal network (SSRF).
 **
 ** <ul>
 **   <li>HTTPS only unless the policy allows http; no user-info in the URL.</li>
 **   <li>The host is resolved once. Every resolved address is checked against
 **       {@link AddressRules} (private, loopback, link-local, metadata and
 **       reserved ranges are blocked unless on the allowlist), and the request
 **       is refused if any address is blocked. The connection is then pinned to
 **       exactly those addresses through the client's DNS resolver, so a DNS
 **       answer that changes between the check and the connect (DNS rebinding)
 **       cannot redirect it.</li>
 **   <li>Redirects are never followed: a 3xx is returned to the caller.</li>
 **   <li>TLS 1.2 or 1.3, the JVM trust store and standard host name
 **       verification.</li>
 **   <li>Connect, read and total timeouts; at most {@code maxResponseBytes} of
 **       the response body is read.</li>
 ** </ul>
 **
 ** <p>One instance holds one connection pool for one policy; share it and
 ** {@link #close()} it when done. It is thread-safe.</p>
 *******************************************************************************/
public final class SafeHttpClient implements AutoCloseable
{
   private static final ThreadLocal<Pin> PIN = new ThreadLocal<>();

   /** Context attribute holding the remote address, captured while the connection is live. */
   private static final String REMOTE_ADDRESS_ATTRIBUTE = "safeHttp.remoteAddress";

   private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(runnable ->
   {
      Thread thread = new Thread(runnable, "safe-http-watchdog");
      thread.setDaemon(true);
      return (thread);
   });

   private final SafeHttpPolicy                     policy;
   private final AddressRules                       rules;
   private final HostResolver                       resolver;
   private final PoolingHttpClientConnectionManager connectionManager;
   private final CloseableHttpClient                client;



   /*******************************************************************************
    ** Resolves a host name to addresses (replaceable in tests to simulate DNS).
    *******************************************************************************/
   @FunctionalInterface
   public interface HostResolver
   {
      /*******************************************************************************
       ** Resolve a host name.
       *******************************************************************************/
      InetAddress[] resolve(String host) throws UnknownHostException;
   }



   /*******************************************************************************
    ** The addresses checked for the host of the request on this thread.
    *******************************************************************************/
   private record Pin(String host, InetAddress[] addresses)
   {
   }



   /*******************************************************************************
    ** Client using the system resolver.
    *******************************************************************************/
   public SafeHttpClient(SafeHttpPolicy policy)
   {
      this(policy, InetAddress::getAllByName);
   }



   /*******************************************************************************
    ** Client with a resolver.
    *******************************************************************************/
   public SafeHttpClient(SafeHttpPolicy policy, HostResolver resolver)
   {
      this.policy = policy;
      this.rules = new AddressRules(policy.getInternalAllowlist());
      this.resolver = resolver;

      SSLConnectionSocketFactory ssl = new SSLConnectionSocketFactory(SSLContexts.createSystemDefault(), new String[] {"TLSv1.3", "TLSv1.2"}, null,
         SSLConnectionSocketFactory.getDefaultHostnameVerifier());
      Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
         .register("http", PlainConnectionSocketFactory.getSocketFactory())
         .register("https", ssl)
         .build();

      DnsResolver pinned = host ->
      {
         Pin pin = PIN.get();
         if(pin != null && pin.host().equalsIgnoreCase(stripBrackets(host)))
         {
            return (pin.addresses());
         }
         throw (new UnknownHostException("Refusing to resolve a host that was not checked: " + host));
      };

      this.connectionManager = new PoolingHttpClientConnectionManager(registry, pinned);
      this.connectionManager.setMaxTotal(50);
      this.connectionManager.setDefaultMaxPerRoute(5);
      this.connectionManager.setValidateAfterInactivity(2000);

      int connectMillis = (int) policy.getConnectTimeout().toMillis();
      this.client = HttpClients.custom()
         .setConnectionManager(connectionManager)
         .disableRedirectHandling()
         .disableAutomaticRetries()
         .disableCookieManagement()
         .disableAuthCaching()
         .setRequestExecutor(new HttpRequestExecutor()
         {
            /*******************************************************************************
             ** Record the connected address while the connection is live: a response
             ** without a body releases the connection inside execute().
             *******************************************************************************/
            @Override
            public HttpResponse execute(HttpRequest request, HttpClientConnection connection, HttpContext context) throws IOException, HttpException
            {
               String address = connectionAddress(connection);
               if(address != null)
               {
                  context.setAttribute(REMOTE_ADDRESS_ATTRIBUTE, address);
               }
               return (super.execute(request, connection, context));
            }
         })
         .setUserAgent(policy.getUserAgent())
         .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectTimeout(connectMillis)
            .setConnectionRequestTimeout(connectMillis)
            .setSocketTimeout((int) policy.getReadTimeout().toMillis())
            .setRedirectsEnabled(false)
            .build())
         .build();
   }



   /*******************************************************************************
    ** Check a URL against the policy without sending anything (for validating
    ** a URL when it is saved). Resolves DNS.
    **
    ** @throws SafeHttpException when it would be refused
    *******************************************************************************/
   public void checkUrl(String url) throws SafeHttpException
   {
      URI uri = parse(url);
      resolveAndCheck(host(uri));
   }



   /*******************************************************************************
    ** POST a body.
    **
    ** @param url      target
    ** @param headers  request headers (Content-Type included by the caller)
    ** @param body     bytes to send
    ** @throws SafeHttpException when refused, or on a network failure; any
    **         HTTP status (including 3xx, 4xx and 5xx) is a response, not an
    **         exception
    *******************************************************************************/
   public SafeHttpResponse post(String url, Map<String, String> headers, byte[] body) throws SafeHttpException
   {
      URI           uri       = parse(url);
      String        host      = host(uri);
      InetAddress[] addresses = resolveAndCheck(host);

      HttpPost request = new HttpPost(uri);
      if(headers != null)
      {
         headers.forEach(request::setHeader);
      }
      request.setEntity(new ByteArrayEntity(body == null ? new byte[0] : body));

      AtomicBoolean      timedOut = new AtomicBoolean(false);
      ScheduledFuture<?> watchdog = WATCHDOG.schedule(() ->
      {
         timedOut.set(true);
         request.abort();
      }, policy.getTotalTimeout().toMillis(), TimeUnit.MILLISECONDS);

      long              start   = System.nanoTime();
      HttpClientContext context = HttpClientContext.create();
      PIN.set(new Pin(host, addresses));
      try(CloseableHttpResponse response = client.execute(request, context))
      {
         String remote = remoteAddress(context);

         Map<String, String> responseHeaders = new LinkedHashMap<>();
         for(Header header : response.getAllHeaders())
         {
            responseHeaders.putIfAbsent(header.getName().toLowerCase(Locale.ROOT), header.getValue());
         }

         byte[]  responseBody = new byte[0];
         boolean truncated    = false;
         if(response.getEntity() != null)
         {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try(InputStream in = response.getEntity().getContent())
            {
               byte[] chunk = new byte[8192];
               int    read;
               while((read = in.read(chunk)) != -1)
               {
                  int room = policy.getMaxResponseBytes() - buffer.size();
                  if(read > room)
                  {
                     buffer.write(chunk, 0, Math.max(room, 0));
                     truncated = true;
                     request.abort();
                     break;
                  }
                  buffer.write(chunk, 0, read);
               }
            }
            catch(IOException e)
            {
               if(!truncated)
               {
                  throw (e);
               }
            }
            responseBody = buffer.toByteArray();
         }
         return (new SafeHttpResponse(response.getStatusLine().getStatusCode(), responseHeaders, responseBody, truncated, remote,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
      }
      catch(ConnectTimeoutException e)
      {
         throw (new SafeHttpException(SafeHttpException.Reason.CONNECT_TIMEOUT, "Connect timed out after " + policy.getConnectTimeout().toMillis() + " ms", e));
      }
      catch(SocketTimeoutException e)
      {
         throw (new SafeHttpException(SafeHttpException.Reason.READ_TIMEOUT, "No response data for " + policy.getReadTimeout().toMillis() + " ms", e));
      }
      catch(SSLException e)
      {
         throw (new SafeHttpException(SafeHttpException.Reason.TLS_FAILURE, "TLS failed: " + e.getMessage(), e));
      }
      catch(IOException e)
      {
         if(timedOut.get() || (e instanceof InterruptedIOException && timedOut.get()))
         {
            throw (new SafeHttpException(SafeHttpException.Reason.TOTAL_TIMEOUT, "Request took longer than " + policy.getTotalTimeout().toMillis() + " ms", e));
         }
         if(e instanceof HttpHostConnectException)
         {
            throw (new SafeHttpException(SafeHttpException.Reason.CONNECTION_FAILED, "Connection refused", e));
         }
         throw (new SafeHttpException(SafeHttpException.Reason.CONNECTION_FAILED, "Request failed: " + e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()), e));
      }
      finally
      {
         PIN.remove();
         watchdog.cancel(false);
      }
   }



   /*******************************************************************************
    ** Close the connection pool.
    *******************************************************************************/
   @Override
   public void close()
   {
      try
      {
         client.close();
      }
      catch(IOException e)
      {
         //////////////////////////////////////
         // nothing useful to do on close    //
         //////////////////////////////////////
      }
      connectionManager.shutdown();
   }



   /*******************************************************************************
    ** Parse and check the URL shape and scheme.
    *******************************************************************************/
   private URI parse(String url) throws SafeHttpException
   {
      if(url == null || url.isBlank())
      {
         throw (new SafeHttpException(SafeHttpException.Reason.INVALID_URL, "A URL is required"));
      }
      URI uri;
      try
      {
         uri = new URI(url.trim());
      }
      catch(URISyntaxException e)
      {
         throw (new SafeHttpException(SafeHttpException.Reason.INVALID_URL, "Not a valid URL", e));
      }
      String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
      if(!"https".equals(scheme) && !("http".equals(scheme) && policy.isAllowPlainHttp()))
      {
         throw (new SafeHttpException(SafeHttpException.Reason.BLOCKED_SCHEME, policy.isAllowPlainHttp() ? "Only http and https URLs are allowed" : "Only https URLs are allowed"));
      }
      if(uri.getRawUserInfo() != null)
      {
         throw (new SafeHttpException(SafeHttpException.Reason.INVALID_URL, "URLs with a user name or password are not allowed"));
      }
      if(uri.getHost() == null || uri.getHost().isBlank())
      {
         throw (new SafeHttpException(SafeHttpException.Reason.INVALID_URL, "The URL has no host"));
      }
      return (uri);
   }



   /*******************************************************************************
    ** Host without IPv6 brackets.
    *******************************************************************************/
   private static String host(URI uri)
   {
      return (stripBrackets(uri.getHost()));
   }



   /*******************************************************************************
    ** Remove [ ] around an IPv6 literal.
    *******************************************************************************/
   private static String stripBrackets(String host)
   {
      if(host != null && host.startsWith("[") && host.endsWith("]"))
      {
         return (host.substring(1, host.length() - 1));
      }
      return (host);
   }



   /*******************************************************************************
    ** Resolve once and check every address.
    *******************************************************************************/
   private InetAddress[] resolveAndCheck(String host) throws SafeHttpException
   {
      InetAddress[] addresses;
      try
      {
         addresses = resolver.resolve(host);
      }
      catch(UnknownHostException e)
      {
         throw (new SafeHttpException(SafeHttpException.Reason.DNS_FAILURE, "Could not resolve " + host, e));
      }
      if(addresses == null || addresses.length == 0)
      {
         throw (new SafeHttpException(SafeHttpException.Reason.DNS_FAILURE, "Could not resolve " + host));
      }
      for(InetAddress address : addresses)
      {
         String problem = rules.check(host, address);
         if(problem != null)
         {
            throw (new SafeHttpException(SafeHttpException.Reason.BLOCKED_ADDRESS, "Blocked: " + host + " resolves to " + problem));
         }
      }
      return (addresses);
   }



   /*******************************************************************************
    ** The address the request connected to, if known.
    *******************************************************************************/
   private static String remoteAddress(HttpClientContext context)
   {
      Object captured = context.getAttribute(REMOTE_ADDRESS_ATTRIBUTE);
      return (captured instanceof String text ? text : connectionAddress(context.getConnection()));
   }



   /*******************************************************************************
    ** The remote address of a connection, or null when it is unknown or the
    ** connection has already been released.
    *******************************************************************************/
   private static String connectionAddress(HttpConnection connection)
   {
      try
      {
         if(connection instanceof HttpInetConnection inet && inet.getRemoteAddress() != null)
         {
            return (inet.getRemoteAddress().getHostAddress());
         }
      }
      catch(RuntimeException e)
      {
         ///////////////////////////////////////////////////
         // released connection: the address is unknown  //
         ///////////////////////////////////////////////////
      }
      return (null);
   }
}
