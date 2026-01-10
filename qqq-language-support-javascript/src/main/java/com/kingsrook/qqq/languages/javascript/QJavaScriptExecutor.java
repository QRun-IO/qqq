/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.languages.javascript;


import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.scripts.QCodeExecutor;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.QCodeExecutionLoggerInterface;
import com.kingsrook.qqq.backend.core.exceptions.QCodeException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import org.openjdk.nashorn.internal.runtime.ECMAException;
import org.openjdk.nashorn.internal.runtime.ParserException;
import org.openjdk.nashorn.internal.runtime.Undefined;


/*******************************************************************************
 **
 *******************************************************************************/
public class QJavaScriptExecutor implements QCodeExecutor
{
   private static final QLogger LOG = QLogger.getLogger(QJavaScriptExecutor.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Serializable execute(QCodeReference codeReference, Map<String, Serializable> inputContext, QCodeExecutionLoggerInterface executionLogger) throws QCodeException
   {
      String       code   = getCode(codeReference);
      Serializable output = runInline(code, inputContext, executionLogger);
      return (output);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Object convertObjectToJava(Object object) throws QCodeException
   {
      try
      {
         if(object == null || object instanceof String || object instanceof Boolean || object instanceof Integer || object instanceof Long || object instanceof BigDecimal)
         {
            return (object);
         }
         else if(object instanceof Float f)
         {
            return (new BigDecimal(f));
         }
         else if(object instanceof Double d)
         {
            return (new BigDecimal(d));
         }
         else if(object instanceof Undefined)
         {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // well, we always said we wanted javascript to treat null & undefined the same way...  here's our chance //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            return (null);
         }

         if(object instanceof ScriptObjectMirror scriptObjectMirror)
         {
            try
            {
               if("Date".equals(scriptObjectMirror.getClassName()))
               {
                  ////////////////////////////////////////////////////////////////////
                  // looks like the js Date is in UTC (is that because our JVM is?) //
                  // so the instant being in UTC matches                            //
                  ////////////////////////////////////////////////////////////////////
                  Double  millis  = (Double) scriptObjectMirror.callMember("getTime");
                  Instant instant = Instant.ofEpochMilli(millis.longValue());
                  return (instant);
               }
            }
            catch(Exception e)
            {
               LOG.debug("Error unwrapping javascript date", e);
            }

            if(scriptObjectMirror.isArray())
            {
               List<Object> result = new ArrayList<>();
               for(String key : scriptObjectMirror.keySet())
               {
                  result.add(Integer.parseInt(key), convertObjectToJava(scriptObjectMirror.get(key)));
               }
               return (result);
            }
            else
            {
               ///////////////////////////////////////////////////////////////////////////////////////////////////////
               // last thing we know to try (though really, there's probably some check we should have around this) //
               ///////////////////////////////////////////////////////////////////////////////////////////////////////
               Map<String, Object> result = new HashMap<>();
               for(String key : scriptObjectMirror.keySet())
               {
                  result.put(key, convertObjectToJava(scriptObjectMirror.get(key)));
               }
               return (result);
            }
         }

         return QCodeExecutor.super.convertObjectToJava(object);
      }
      catch(Exception e)
      {
         throw (new QCodeException("Error converting java object", e));
      }
   }



   /*******************************************************************************
    ** Convert a native java object into one for the script's language/runtime.
    ** e.g., a java Instant to a Nashorn Date
    **
    *******************************************************************************/
   public Object convertJavaObject(Object object, Object requestedTypeHint) throws QCodeException
   {
      try
      {
         if("Date".equals(requestedTypeHint))
         {
            if(object instanceof Instant i)
            {
               long         millis = (i.getEpochSecond() * 1000 + i.getLong(ChronoField.MILLI_OF_SECOND));
               ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
               return engine.eval("new Date(" + millis + ")");
            }
         }

         return (QCodeExecutor.super.convertJavaObject(object, requestedTypeHint));
      }
      catch(Exception e)
      {
         throw (new QCodeException("Error converting java object", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Serializable runInline(String code, Map<String, Serializable> inputContext, QCodeExecutionLoggerInterface executionLogger) throws QCodeException
   {
      new NashornScriptEngineFactory();
      ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

      //////////////////////////////////////////////
      // setup the javascript environment/context //
      //////////////////////////////////////////////
      Bindings bindings = engine.createBindings();
      bindings.putAll(inputContext);

      if(!bindings.containsKey("logger"))
      {
         bindings.put("logger", executionLogger);
      }

      ////////////////////////////////////////////////////////////////////////
      // wrap the user's code in an immediately-invoked function expression //
      // if the user's code (%s below) returns - then our IIFE is done.     //
      // if the user's code doesn't return, but instead created a 'script'  //
      // variable, with a 'main' function on it (e.g., from a compiled      //
      // type script file), then call main function and return its result.  //
      ////////////////////////////////////////////////////////////////////////
      String codeToRun = """
         (function userDefinedFunction()
         {
            %s

            var mainFunction = null;
            try
            {
               if(script && script.main && typeof script.main == "function")
               {
                  mainFunction = script.main;
               }
            }
            catch(e) { }

            if(mainFunction != null)
            {
               return (mainFunction());
            }
         })();
         """.formatted(code);

      Serializable output;
      try
      {
         output = (Serializable) engine.eval(codeToRun, bindings);
      }
      catch(ScriptException se)
      {
         QCodeException qCodeException = getQCodeExceptionFromScriptException(se);
         throw (qCodeException);
      }

      return (output);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QCodeException getQCodeExceptionFromScriptException(ScriptException se)
   {
      boolean isParserException     = ExceptionUtils.findClassInRootChain(se, ParserException.class) != null;
      boolean isUserThrownException = ExceptionUtils.findClassInRootChain(se, ECMAException.class) != null;

      String message      = se.getMessage();
      String errorContext = null;
      if(message != null)
      {
         message = message.replaceFirst(" in <eval>.*", "");
         message = message.replaceFirst("<eval>:\\d+:\\d+", "");

         if(message.contains("\n"))
         {
            String[] parts = message.split("\n", 2);
            message = parts[0];
            errorContext = parts[1];
         }
      }

      int actualScriptLineNumber = se.getLineNumber() - 2;

      String  prefix         = "Script Exception";
      boolean includeColumn  = true;
      boolean includeContext = false;
      if(isParserException)
      {
         prefix = "Script parser exception";
         includeContext = true;
      }
      else if(isUserThrownException)
      {
         prefix = "Script threw an exception";
         includeColumn = false;
      }

      QCodeException qCodeException = new QCodeException(prefix + " at line " + actualScriptLineNumber + (includeColumn ? (" column " + se.getColumnNumber()) : "") + ": " + message);
      if(includeContext)
      {
         qCodeException.setContext(errorContext);
      }

      return (qCodeException);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getCode(QCodeReference codeReference)
   {
      if(StringUtils.hasContent(codeReference.getInlineCode()))
      {
         return (codeReference.getInlineCode());
      }
      else
      {
         throw (new NotImplementedException("Only inline code is implemented at this time."));
      }
   }

}
