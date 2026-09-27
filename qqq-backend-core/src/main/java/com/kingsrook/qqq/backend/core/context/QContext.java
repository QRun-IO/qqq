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

package com.kingsrook.qqq.backend.core.context;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeVoidVoidMethod;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** A collection of thread-local variables, to define the current context of the
 ** QQQ code that is running.  e.g., what QInstance is being used, what QSession
 ** is active, etc.
 *******************************************************************************/
public class QContext
{
   private static final QLogger LOG = QLogger.getLogger(QContext.class);

   private static ThreadLocal<QInstance>                  qInstanceThreadLocal           = new ThreadLocal<>();
   private static ThreadLocal<QSession>                   qSessionThreadLocal            = new ThreadLocal<>();
   private static ThreadLocal<QBackendTransaction>        qBackendTransactionThreadLocal = new ThreadLocal<>();
   private static ThreadLocal<Stack<AbstractActionInput>> actionStackThreadLocal         = new ThreadLocal<>();

   private static ThreadLocal<Map<String, Serializable>> objectsThreadLocal = new ThreadLocal<>();



   /*******************************************************************************
    ** private constructor - class is not meant to be instantiated.
    *******************************************************************************/
   private QContext()
   {
   }



   /*******************************************************************************
    ** Most common method to set or init the context - e.g., set the current thread
    ** with a QInstance and QSession.
    *******************************************************************************/
   public static void init(QInstance qInstance, QSession qSession)
   {
      init(qInstance, qSession, null, null);
   }



   /*******************************************************************************
    ** Full flavor init method - also take a transaction and action input (to seed the stack).
    *******************************************************************************/
   public static void init(QInstance qInstance, QSession qSession, QBackendTransaction transaction, AbstractActionInput actionInput)
   {
      qInstanceThreadLocal.set(qInstance);
      qSessionThreadLocal.set(qSession);
      qBackendTransactionThreadLocal.set(transaction);

      actionStackThreadLocal.set(new Stack<>());
      if(actionInput != null)
      {
         actionStackThreadLocal.get().add(actionInput);
      }

      if(qInstance != null && !qInstance.getHasBeenValidated())
      {
         try
         {
            new QInstanceValidator().validate(qInstance);
         }
         catch(QInstanceValidationException e)
         {
            LOG.warn(e);
            throw (new IllegalArgumentException("QInstance failed validation" + e.getMessage(), e));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <T extends Throwable> void withTemporaryContext(CapturedContext context, UnsafeVoidVoidMethod<T> method) throws T
   {
      CapturedContext originalContext = QContext.capture();
      try
      {
         QContext.init(context);
         method.run();
      }
      finally
      {
         QContext.init(originalContext);
      }
   }



   /*******************************************************************************
    ** Init a new thread with the context captured from a different thread.  e.g.,
    ** when starting some async task.
    *******************************************************************************/
   public static void init(CapturedContext capturedContext)
   {
      init(capturedContext.qInstance(), capturedContext.qSession(), capturedContext.qBackendTransaction(), null);
      actionStackThreadLocal.set(capturedContext.actionStack());
   }



   /*******************************************************************************
    ** Capture all values from the current thread - meant to be used with the init
    ** overload that takes a CapturedContext, for setting up a child thread.
    *******************************************************************************/
   public static CapturedContext capture()
   {
      return (new CapturedContext(getQInstance(), getQSession(), getQBackendTransaction(), getActionStack()));
   }



   /*******************************************************************************
    ** Clear all values in the current thread.
    *******************************************************************************/
   public static void clear()
   {
      qInstanceThreadLocal.remove();
      qSessionThreadLocal.remove();
      qBackendTransactionThreadLocal.remove();
      actionStackThreadLocal.remove();
      objectsThreadLocal.remove();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QInstance getQInstance()
   {
      return (qInstanceThreadLocal.get());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QSession getQSession()
   {
      return (qSessionThreadLocal.get());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QBackendTransaction getQBackendTransaction()
   {
      return (qBackendTransactionThreadLocal.get());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Stack<AbstractActionInput> getActionStack()
   {
      return (actionStackThreadLocal.get());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void pushAction(AbstractActionInput action)
   {
      if(actionStackThreadLocal.get() == null)
      {
         actionStackThreadLocal.set(new Stack<>());
      }
      actionStackThreadLocal.get().push(action);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void popAction()
   {
      try
      {
         actionStackThreadLocal.get().pop();
      }
      catch(Exception e)
      {
         LOG.debug("Error popping action stack", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void setQInstance(QInstance qInstance)
   {
      qInstanceThreadLocal.set(qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void setQSession(QSession qSession)
   {
      qSessionThreadLocal.set(qSession);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void setTransaction(QBackendTransaction transaction)
   {
      qBackendTransactionThreadLocal.set(transaction);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void clearTransaction()
   {
      qBackendTransactionThreadLocal.remove();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Optional<AbstractActionInput> getFirstActionInStack()
   {
      if(actionStackThreadLocal.get() == null || actionStackThreadLocal.get().isEmpty())
      {
         return (Optional.empty());
      }

      return (Optional.of(actionStackThreadLocal.get().get(0)));
   }



   /*******************************************************************************
    ** get one named object from the Context for the current thread.  may return null.
    *******************************************************************************/
   public static Serializable getObject(String key)
   {
      if(objectsThreadLocal.get() == null)
      {
         return null;
      }
      return objectsThreadLocal.get().get(key);
   }



   /*******************************************************************************
    ** get one named object from the Context for the current thread, cast to the
    ** specified type if possible.  if not found, or wrong type, empty is returned.
    *******************************************************************************/
   public static <T extends Serializable> Optional<T> getObject(String key, Class<T> type)
   {
      Serializable object = getObject(key);

      if(type.isInstance(object))
      {
         return Optional.of(type.cast(object));
      }
      else if(object == null)
      {
         return Optional.empty();
      }
      else
      {
         LOG.warn("Unexpected type of object found in session under key [" + key + "]",
            logPair("expectedType", type.getName()),
            logPair("actualType", object.getClass().getName())
         );
         return Optional.empty();
      }
   }



   /*******************************************************************************
    ** put a named object into the Context for the current thread.
    *******************************************************************************/
   public static void setObject(String key, Serializable object)
   {
      if(objectsThreadLocal.get() == null)
      {
         objectsThreadLocal.set(new HashMap<>());
      }
      objectsThreadLocal.get().put(key, object);
   }



   /*******************************************************************************
    ** remove a named object from the Context of the current thread.
    *******************************************************************************/
   public static void removeObject(String key)
   {
      if(objectsThreadLocal.get() != null)
      {
         objectsThreadLocal.get().remove(key);
      }
   }



   /*******************************************************************************
    ** get the full map of named objects for the current thread (possibly null).
    *******************************************************************************/
   public static Map<String, Serializable> getObjects()
   {
      return objectsThreadLocal.get();
   }



   /*******************************************************************************
    ** fully replace the map of named objets for the current thread.
    *******************************************************************************/
   public static void setObjects(Map<String, Serializable> objects)
   {
      objectsThreadLocal.set(objects);
   }

}
