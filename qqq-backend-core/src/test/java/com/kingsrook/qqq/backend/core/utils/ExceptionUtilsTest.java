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

package com.kingsrook.qqq.backend.core.utils;


import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ExceptionUtils
 **
 *******************************************************************************/
class ExceptionUtilsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFindClassInRootChain()
   {
      assertNull(ExceptionUtils.findClassInRootChain(null, QUserFacingException.class));

      QUserFacingException target = new QUserFacingException("target");
      assertSame(target, ExceptionUtils.findClassInRootChain(target, QUserFacingException.class));
      assertSame(target, ExceptionUtils.findClassInRootChain(new QException("decoy", target), QUserFacingException.class));
      assertNull(ExceptionUtils.findClassInRootChain(new QException("decoy", target), IllegalArgumentException.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetRootException()
   {
      assertNull(ExceptionUtils.getRootException(null));

      Exception root = new Exception("root");
      assertSame(root, ExceptionUtils.getRootException(root));

      Exception container = new Exception("container", root);
      assertSame(root, ExceptionUtils.getRootException(container));

      Exception middle = new Exception("middle", root);
      Exception top    = new Exception("top", middle);
      assertSame(root, ExceptionUtils.getRootException(top));

      ////////////////////////////////////////////////////////////////////////////////////////
      // without the code that checks for loops, these next two checks cause infinite loops //
      ////////////////////////////////////////////////////////////////////////////////////////
      MyException selfCaused = new MyException("selfCaused");
      selfCaused.setCause(selfCaused);
      assertSame(selfCaused, ExceptionUtils.getRootException(selfCaused));

      MyException cycle1 = new MyException("cycle1");
      MyException cycle2 = new MyException("cycle2");
      cycle1.setCause(cycle2);
      cycle2.setCause(cycle1);
      assertSame(cycle1, ExceptionUtils.getRootException(cycle1));
      assertSame(cycle2, ExceptionUtils.getRootException(cycle2));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testConcatenateMessagesFromChain()
   {
      assertNull(ExceptionUtils.concatenateMessagesFromChain(null));
      assertEquals("QException", ExceptionUtils.concatenateMessagesFromChain(new QException((String) null)));
      assertEquals("QException", ExceptionUtils.concatenateMessagesFromChain(new QException("")));
      assertEquals("foo; bar", ExceptionUtils.concatenateMessagesFromChain(new QException("foo", new QException("bar"))));
      assertEquals("foo; QException; bar", ExceptionUtils.concatenateMessagesFromChain(new QException("foo", new QException(null, new QException("bar")))));

      MyException selfCaused = new MyException("selfCaused");
      selfCaused.setCause(selfCaused);
      assertEquals("selfCaused", ExceptionUtils.concatenateMessagesFromChain(selfCaused));

      MyException cycle1 = new MyException("cycle1");
      MyException cycle2 = new MyException("cycle2");
      cycle1.setCause(cycle2);
      cycle2.setCause(cycle1);

      assertEquals("cycle1; cycle2", ExceptionUtils.concatenateMessagesFromChain(cycle1));
      assertEquals("cycle2; cycle1", ExceptionUtils.concatenateMessagesFromChain(cycle2));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetTopAndBottomMessages()
   {
      assertEquals("foo", ExceptionUtils.getTopAndBottomMessages(new Exception("foo")));
      assertEquals("foo: bar", ExceptionUtils.getTopAndBottomMessages(new Exception("foo", new Exception("bar"))));
      assertEquals("foo: baz", ExceptionUtils.getTopAndBottomMessages(new Exception("foo", new Exception("bar", new Exception("baz")))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTryIgnore()
   {
      ExceptionUtils.tryIgnore(() -> ((Integer) null).toString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTryCatch()
   {
      AtomicBoolean didCatch = new AtomicBoolean(false);
      ExceptionUtils.tryCatch(() -> ((Integer) null).toString(), e -> didCatch.set(true));
      assertTrue(didCatch.get());
   }



   /*******************************************************************************
    ** Test exception class - lets you set the cause, easier to create a loop.
    *******************************************************************************/
   public class MyException extends Exception
   {
      private Throwable myCause = null;



      /*******************************************************************************
       **
       *******************************************************************************/
      public MyException(String message)
      {
         super(message);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public MyException(Throwable cause)
      {
         super(cause);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public void setCause(Throwable cause)
      {
         myCause = cause;
      }



      @Override
      public synchronized Throwable getCause()
      {
         return (myCause);
      }
   }
}
