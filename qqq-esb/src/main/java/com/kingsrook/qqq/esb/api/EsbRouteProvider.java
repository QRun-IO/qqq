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


import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
import com.kingsrook.qqq.middleware.javalin.QJavalinUtils;
import com.kingsrook.qqq.middleware.javalin.executors.ExecutorSessionUtils;
import io.javalin.config.JavalinConfig;
import io.javalin.http.ContentType;
import io.javalin.http.Context;


/*******************************************************************************
 * Javalin routes for the ESB endpoints, under /qqq/v1/esb:
 *
 * - GET /overview - providers, destinations, publishers, triggers
 * - GET /table/{table} - a table's publications and subscribers
 * - GET /process/{process} - a process's publications and triggers
 * - GET /deadLetters/{trigger}?offset=0&amp;limit=50 - browse dead letters
 * - GET /messages/{destination}?offset=0&amp;limit=50 - browse a queue (for a
 *   topic, add trigger=NAME to browse that trigger's subscription)
 *
 * Each request gets a session from the instance's authentication module, and
 * EsbStatusBuilder builds the body and checks permissions.  Errors: 401
 * unauthenticated, 403 not permitted, 404 unknown or without ESB meta-data,
 * 400 bad paging.  Bodies are serialized with nulls and empty lists kept, as
 * the endpoint contract has them.
 *
 * Registered with Javalin by EsbJavalinMetaDataProducer.  The overview needs
 * access to the ESB app, so it's a 403 unless the instance has the app (from
 * EsbAppMetaDataProducer).
 *******************************************************************************/
public class EsbRouteProvider implements QJavalinRouteProviderInterface
{
   public static final String BASE_PATH = "/qqq/v1/esb";

   private QInstance qInstance;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void setQInstance(QInstance qInstance)
   {
      this.qInstance = qInstance;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptJavalinConfig(JavalinConfig config)
   {
      config.routes.get(BASE_PATH + "/overview", context -> respond(context, builder -> builder.buildOverview()));
      config.routes.get(BASE_PATH + "/table/{table}", context -> respond(context, builder -> builder.buildTable(context.pathParam("table"))));
      config.routes.get(BASE_PATH + "/process/{process}", context -> respond(context, builder -> builder.buildProcess(context.pathParam("process"))));
      config.routes.get(BASE_PATH + "/deadLetters/{trigger}", context -> respond(context, builder -> builder.browseDeadLetters(context.pathParam("trigger"), getIntegerParam(context, "offset"), getIntegerParam(context, "limit"))));
      config.routes.get(BASE_PATH + "/messages/{destination}", context -> respond(context, builder -> builder.browseMessages(context.pathParam("destination"), context.queryParam("trigger"), getIntegerParam(context, "offset"), getIntegerParam(context, "limit"))));
   }



   /*******************************************************************************
    ** Set up the session, build the body, and send it as JSON - or send the
    ** error's status.
    *******************************************************************************/
   private void respond(Context context, BodyBuilder bodyBuilder)
   {
      try
      {
         ExecutorSessionUtils.setupSession(context, qInstance);
         Map<String, Object> body = bodyBuilder.build(new EsbStatusBuilder());

         context.contentType(ContentType.APPLICATION_JSON);
         context.result(JsonUtils.toJsonCustomized(body, builder -> builder.serializationInclusion(JsonInclude.Include.ALWAYS)));
      }
      catch(Exception e)
      {
         QJavalinUtils.handleException(null, context, e);
      }
      finally
      {
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** An integer query parameter - null if absent; QBadRequestException if it
    ** isn't an integer.
    *******************************************************************************/
   private static Integer getIntegerParam(Context context, String name) throws QBadRequestException
   {
      String value = context.queryParam(name);
      if(!StringUtils.hasContent(value))
      {
         return (null);
      }

      try
      {
         return (Integer.parseInt(value.trim()));
      }
      catch(NumberFormatException e)
      {
         throw (new QBadRequestException("The " + name + " parameter must be an integer."));
      }
   }



   /*******************************************************************************
    * Builds one endpoint's body.
    *******************************************************************************/
   @FunctionalInterface
   private interface BodyBuilder
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      Map<String, Object> build(EsbStatusBuilder builder) throws QException;
   }

}
