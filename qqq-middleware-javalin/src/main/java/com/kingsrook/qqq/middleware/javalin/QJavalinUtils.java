/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin;


import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import io.javalin.http.Context;
import org.eclipse.jetty.http.HttpStatus;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility methods shared by javalin implementations
 *******************************************************************************/
public class QJavalinUtils
{
   private static final QLogger LOG = QLogger.getLogger(QJavalinUtils.class);



   /*******************************************************************************
    ** Returns Integer if context has a valid int query parameter by the given name,
    **  Returns null if no param (or empty value).
    **  Throws QValueException for malformed numbers.
    *******************************************************************************/
   public static Integer integerQueryParam(Context context, String name) throws QValueException
   {
      String value = context.queryParam(name);
      if(StringUtils.hasContent(value))
      {
         return (ValueUtils.getValueAsInteger(value));
      }

      return (null);
   }



   /*******************************************************************************
    ** Returns true iff context has a valid query parameter by the given name, with
    ** a value of "true".
    *******************************************************************************/
   public static boolean queryParamIsTrue(Context context, String name) throws QValueException
   {
      String value = context.queryParam(name);
      if(Objects.equals(value, "true"))
      {
         return (true);
      }

      return (false);
   }



   /*******************************************************************************
    ** Returns Integer if context has a valid int form parameter by the given name,
    **  Returns null if no param (or empty value).
    **  Throws QValueException for malformed numbers.
    *******************************************************************************/
   public static Integer integerFormParam(Context context, String name) throws QValueException
   {
      String value = context.formParam(name);
      if(StringUtils.hasContent(value))
      {
         return (ValueUtils.getValueAsInteger(value));
      }

      return (null);
   }



   /*******************************************************************************
    ** Returns String if context has a valid query parameter by the given name,
    *  Returns null if no param (or empty value).
    *******************************************************************************/
   public static String stringQueryParam(Context context, String name)
   {
      String value = context.queryParam(name);
      if(StringUtils.hasContent(value))
      {
         return (value);
      }

      return (null);
   }



   /***************************************************************************
    ** get a param value from either the form-body, or query string returning
    ** the first one found, looking in that order, null if neither is found.
    ** uses try-catch on reading each of those, as they apparently can throw!
    ***************************************************************************/
   public static String getFormParamOrQueryParam(Context context, String parameterName)
   {
      String value = null;
      try
      {
         value = context.formParam(parameterName);
      }
      catch(Exception e)
      {
         ////////////////
         // leave null //
         ////////////////
      }

      if(!StringUtils.hasContent(value))
      {
         try
         {
            value = context.queryParam(parameterName);
         }
         catch(Exception e)
         {
            ////////////////
            // leave null //
            ////////////////
         }
      }

      return value;
   }



   /***************************************************************************
    ** get a param value from either the query string, or form-body, returning
    ** the first one found, looking in that order, null if neither is found.
    ** uses try-catch on reading each of those, as they apparently can throw!
    ***************************************************************************/
   static String getQueryParamOrFormParam(Context context, String parameterName)
   {
      String value = null;
      try
      {
         value = context.queryParam(parameterName);
      }
      catch(Exception e)
      {
         ////////////////
         // leave null //
         ////////////////
      }

      if(!StringUtils.hasContent(value))
      {
         try
         {
            value = context.formParam(parameterName);
         }
         catch(Exception e)
         {
            ////////////////
            // leave null //
            ////////////////
         }
      }

      return value;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void handleException(HttpStatus.Code statusCode, Context context, Exception e)
   {
      QUserFacingException userFacingException = ExceptionUtils.findClassInRootChain(e, QUserFacingException.class);
      if(userFacingException != null)
      {
         if(userFacingException instanceof QNotFoundException)
         {
            statusCode = Objects.requireNonNullElse(statusCode, HttpStatus.Code.NOT_FOUND); // 404
            respondWithError(context, statusCode, userFacingException.getMessage());
         }
         else if(userFacingException instanceof QBadRequestException)
         {
            statusCode = Objects.requireNonNullElse(statusCode, HttpStatus.Code.BAD_REQUEST); // 400
            respondWithError(context, statusCode, userFacingException.getMessage());
         }
         else
         {
            LOG.info("User-facing exception", e);
            statusCode = Objects.requireNonNullElse(statusCode, HttpStatus.Code.INTERNAL_SERVER_ERROR); // 500
            respondWithError(context, statusCode, userFacingException.getMessage());
         }
      }
      else
      {
         if(e instanceof QAuthenticationException)
         {
            respondWithError(context, HttpStatus.Code.UNAUTHORIZED, e.getMessage()); // 401
            return;
         }

         if(e instanceof QPermissionDeniedException)
         {
            respondWithError(context, HttpStatus.Code.FORBIDDEN, e.getMessage()); // 403
            return;
         }

         ////////////////////////////////
         // default exception handling //
         ////////////////////////////////
         LOG.warn("Exception in javalin request", e);
         respondWithError(context, HttpStatus.Code.INTERNAL_SERVER_ERROR, e.getClass().getSimpleName() + " (" + e.getMessage() + ")"); // 500
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void respondWithError(Context context, HttpStatus.Code statusCode, String errorMessage)
   {
      context.status(statusCode.getCode());
      context.result(JsonUtils.toJson(Map.of("error", errorMessage)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void handleQueryNullLimit(QJavalinMetaData javalinMetaData, QueryInput queryInput, Context context)
   {
      if(javalinMetaData == null)
      {
         javalinMetaData = new QJavalinMetaData();
      }

      boolean allowed = javalinMetaData.getQueryWithoutLimitAllowed();
      if(!allowed)
      {
         if(queryInput.getFilter() == null)
         {
            queryInput.setFilter(new QQueryFilter());
         }

         queryInput.getFilter().setLimit(javalinMetaData.getQueryWithoutLimitDefault());
         LOG.log(javalinMetaData.getQueryWithoutLimitLogLevel(), "Query request did not specify a limit, which is not allowed.  Using default instead", null,
            logPair("defaultLimit", javalinMetaData.getQueryWithoutLimitDefault()),
            logPair("path", context == null ? null : context.path()));
      }
   }

}
