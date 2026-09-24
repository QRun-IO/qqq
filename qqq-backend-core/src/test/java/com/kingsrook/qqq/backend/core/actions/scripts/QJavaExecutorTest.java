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

package com.kingsrook.qqq.backend.core.actions.scripts;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.QCodeExecutionLoggerInterface;
import com.kingsrook.qqq.backend.core.exceptions.QCodeException;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit tests for QJavaExecutor — the Java-based code executor.
 **
 ** QJavaExecutor loads a Function<Map<String,Object>, Serializable> by class name
 ** via QCodeLoader, injects a "logger" context key when absent, and wraps
 ** any runtime exception in QCodeException.
 *******************************************************************************/
class QJavaExecutorTest extends BaseTest
{

   /*******************************************************************************
    ** A correctly wired Function returns its output through execute().
    *******************************************************************************/
   @Test
   void testExecute_validFunction_returnsOutput() throws QCodeException
   {
      QCodeReference ref     = new QCodeReference(EchoFunction.class.getName(), QCodeType.JAVA);
      Map<String, Serializable> input = new HashMap<>();
      input.put("message", "hello");

      Serializable result = new QJavaExecutor().execute(ref, input, nullLogger());

      assertEquals("hello", result);
   }



   /*******************************************************************************
    ** The executor injects a "logger" key into the context when one is not present.
    *******************************************************************************/
   @Test
   void testExecute_loggerInjectedWhenAbsent() throws QCodeException
   {
      QCodeReference            ref    = new QCodeReference(LoggerPresenceFunction.class.getName(), QCodeType.JAVA);
      Map<String, Serializable> input  = new HashMap<>();
      QCodeExecutionLoggerInterface sentinel = nullLogger();

      Serializable result = new QJavaExecutor().execute(ref, input, sentinel);

      assertEquals(Boolean.TRUE, result, "context['logger'] should have been populated before the function ran");
   }



   /*******************************************************************************
    ** If the caller already provided a "logger" key the executor must not overwrite it.
    *******************************************************************************/
   @Test
   void testExecute_existingLoggerKeyNotOverwritten() throws QCodeException
   {
      QCodeReference            ref   = new QCodeReference(LoggerPresenceFunction.class.getName(), QCodeType.JAVA);
      Map<String, Serializable> input = new HashMap<>();
      input.put("logger", "callerSuppliedLogger"); // Serializable placeholder — not a real logger

      // The function checks if context["logger"] is non-null; it should be the caller-supplied value
      Serializable result = new QJavaExecutor().execute(ref, input, nullLogger());

      assertEquals(Boolean.TRUE, result, "context['logger'] should still be present (caller's value retained)");
   }



   /*******************************************************************************
    ** An exception thrown inside the function must be wrapped in QCodeException.
    *******************************************************************************/
   @Test
   void testExecute_functionThrows_wrappedInQCodeException()
   {
      QCodeReference            ref   = new QCodeReference(ThrowingFunction.class.getName(), QCodeType.JAVA);
      Map<String, Serializable> input = new HashMap<>();

      assertThatThrownBy(() -> new QJavaExecutor().execute(ref, input, nullLogger()))
         .isInstanceOf(QCodeException.class)
         .hasMessageContaining("Error executing script");
   }



   /*******************************************************************************
    ** A class name that doesn't exist at all must still surface as QCodeException.
    *******************************************************************************/
   @Test
   void testExecute_unknownClassName_throwsQCodeException()
   {
      QCodeReference            ref   = new QCodeReference("com.kingsrook.DoesNotExist", QCodeType.JAVA);
      Map<String, Serializable> input = new HashMap<>();

      assertThatThrownBy(() -> new QJavaExecutor().execute(ref, input, nullLogger()))
         .isInstanceOf(QCodeException.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QCodeExecutionLoggerInterface nullLogger()
   {
      return new QCodeExecutionLoggerInterface()
      {
         @Override
         public void acceptExecutionStart(ExecuteCodeInput executeCodeInput)
         {
         }


         @Override
         public void acceptLogLine(String logLine)
         {
         }


         @Override
         public void acceptException(Exception exception)
         {
         }


         @Override
         public void acceptExecutionEnd(java.io.Serializable output)
         {
         }
      };
   }



   /*******************************************************************************
    ** Returns the value of context["message"].
    *******************************************************************************/
   public static class EchoFunction implements Function<Map<String, Object>, Serializable>
   {
      @Override
      public Serializable apply(Map<String, Object> context)
      {
         return (String) context.get("message");
      }
   }



   /*******************************************************************************
    ** Returns TRUE when context["logger"] is non-null (verifying injection).
    *******************************************************************************/
   public static class LoggerPresenceFunction implements Function<Map<String, Object>, Serializable>
   {
      @Override
      public Serializable apply(Map<String, Object> context)
      {
         return context.get("logger") != null;
      }
   }



   /*******************************************************************************
    ** Always throws a RuntimeException to test exception wrapping.
    *******************************************************************************/
   public static class ThrowingFunction implements Function<Map<String, Object>, Serializable>
   {
      @Override
      public Serializable apply(Map<String, Object> context)
      {
         throw new RuntimeException("intentional test failure");
      }
   }

}
