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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** The one error body of the UI API (core and every extension):
 ** `{ error, code, message, fieldErrors[], traceId }`.  `error` is the
 ** original property (kept, equal to `message`).  Since UI API 1.6.0.
 *******************************************************************************/
public class BasicErrorResponseV1 implements ToSchema
{
   ////////////////////////////////////////////////////////////////////////////
   // well-known codes; clients treat an unknown code by its HTTP status     //
   ////////////////////////////////////////////////////////////////////////////
   public static final String CODE_BAD_REQUEST       = "BAD_REQUEST";
   public static final String CODE_UNAUTHENTICATED   = "UNAUTHENTICATED";
   public static final String CODE_PERMISSION_DENIED = "PERMISSION_DENIED";
   public static final String CODE_NOT_FOUND         = "NOT_FOUND";
   public static final String CODE_CONFLICT          = "CONFLICT";
   public static final String CODE_RATE_LIMITED      = "RATE_LIMITED";
   public static final String CODE_INTERNAL_ERROR    = "INTERNAL_ERROR";
   public static final String CODE_ERROR             = "ERROR";

   private String             error;
   private String             code;
   private String             message;
   private List<FieldErrorV1> fieldErrors;
   private String             traceId;



   /*******************************************************************************
    ** An error about one field of the request.
    *******************************************************************************/
   public record FieldErrorV1(String field, String message, String code)
   {
   }



   /*******************************************************************************
    ** The well-known code for an HTTP status.
    *******************************************************************************/
   public static String codeForStatus(int status)
   {
      return (switch(status)
      {
         case 400 -> CODE_BAD_REQUEST;
         case 401 -> CODE_UNAUTHENTICATED;
         case 403 -> CODE_PERMISSION_DENIED;
         case 404 -> CODE_NOT_FOUND;
         case 409 -> CODE_CONFLICT;
         case 429 -> CODE_RATE_LIMITED;
         case 500 -> CODE_INTERNAL_ERROR;
         default -> CODE_ERROR;
      });
   }



   /*******************************************************************************
    ** The body as a map (null traceId and empty field errors omitted).
    *******************************************************************************/
   public Map<String, Object> toMap()
   {
      Map<String, Object> rs = new LinkedHashMap<>();
      rs.put("error", error);
      rs.put("code", code);
      rs.put("message", message);
      if(fieldErrors != null && !fieldErrors.isEmpty())
      {
         List<Map<String, Object>> list = new ArrayList<>();
         for(FieldErrorV1 fieldError : fieldErrors)
         {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("field", fieldError.field());
            map.put("message", fieldError.message());
            if(fieldError.code() != null)
            {
               map.put("code", fieldError.code());
            }
            list.add(map);
         }
         rs.put("fieldErrors", list);
      }
      if(traceId != null)
      {
         rs.put("traceId", traceId);
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      properties.put("error", new Schema().withType(Type.STRING).withDescription("The error message (the same as message; kept for clients of UI API 1.0 to 1.5)."));
      properties.put("code", new Schema().withType(Type.STRING).withDescription("""
         A stable, machine-readable code. Well-known values: BAD_REQUEST (400), UNAUTHENTICATED (401),
         PERMISSION_DENIED (403), NOT_FOUND (404), CONFLICT (409), RATE_LIMITED (429), INTERNAL_ERROR (500),
         ERROR (other). Extensions may add codes; treat an unknown code by the HTTP status. Since 1.6.0."""));
      properties.put("message", new Schema().withType(Type.STRING).withDescription("A message to show the user. Since 1.6.0."));
      properties.put("fieldErrors", new Schema().withType(Type.ARRAY).withDescription("Errors about particular request fields, when known. Since 1.6.0.")
         .withItems(new Schema().withType(Type.OBJECT)
            .withProperty("field", new Schema().withType(Type.STRING).withDescription("The field (or parameter) name."))
            .withProperty("message", new Schema().withType(Type.STRING).withDescription("The error about that field."))
            .withProperty("code", new Schema().withType(Type.STRING).withDescription("An optional machine-readable code, e.g. REQUIRED, TOO_LONG, PATTERN."))));
      properties.put("traceId", new Schema().withType(Type.STRING).withDescription("Identifies this request in the server logs (also the X-QQQ-Trace-Id response header); quote it when reporting a problem. Since 1.6.0."));

      return (new Schema().withType(Type.OBJECT)
         .withDescription("The error body of every UI API operation (core and extensions).")
         .withProperties(properties));
   }



   /*******************************************************************************
    ** Getter for error
    *******************************************************************************/
   public String getError()
   {
      return (this.error);
   }



   /*******************************************************************************
    ** Setter for error
    *******************************************************************************/
   public void setError(String error)
   {
      this.error = error;
   }



   /*******************************************************************************
    ** Fluent setter for error (also sets message, when it is not set)
    *******************************************************************************/
   public BasicErrorResponseV1 withError(String error)
   {
      this.error = error;
      if(this.message == null)
      {
         this.message = error;
      }
      return (this);
   }



   /*******************************************************************************
    ** Getter for code
    *******************************************************************************/
   public String getCode()
   {
      return (this.code);
   }



   /*******************************************************************************
    ** Fluent setter for code
    *******************************************************************************/
   public BasicErrorResponseV1 withCode(String code)
   {
      this.code = code;
      return (this);
   }



   /*******************************************************************************
    ** Getter for message
    *******************************************************************************/
   public String getMessage()
   {
      return (this.message);
   }



   /*******************************************************************************
    ** Fluent setter for message
    *******************************************************************************/
   public BasicErrorResponseV1 withMessage(String message)
   {
      this.message = message;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldErrors
    *******************************************************************************/
   public List<FieldErrorV1> getFieldErrors()
   {
      return (this.fieldErrors);
   }



   /*******************************************************************************
    ** Fluent setter for fieldErrors
    *******************************************************************************/
   public BasicErrorResponseV1 withFieldErrors(List<FieldErrorV1> fieldErrors)
   {
      this.fieldErrors = fieldErrors;
      return (this);
   }



   /*******************************************************************************
    ** Getter for traceId
    *******************************************************************************/
   public String getTraceId()
   {
      return (this.traceId);
   }



   /*******************************************************************************
    ** Fluent setter for traceId
    *******************************************************************************/
   public BasicErrorResponseV1 withTraceId(String traceId)
   {
      this.traceId = traceId;
      return (this);
   }
}
