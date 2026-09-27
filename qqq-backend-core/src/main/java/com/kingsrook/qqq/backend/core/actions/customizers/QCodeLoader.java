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

package com.kingsrook.qqq.backend.core.actions.customizers;


import java.lang.reflect.Constructor;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.code.InitializableViaCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility to load code for running QQQ customizers.
 **
 ** That memoization causes 1,000,000 such calls to go from ~500ms to ~100ms.
 *******************************************************************************/
public class QCodeLoader
{
   private static final QLogger LOG = QLogger.getLogger(QCodeLoader.class);

   private static Memoization<String, Constructor<?>> constructorMemoization = new Memoization<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Optional<TableCustomizerInterface> getTableCustomizer(QTableMetaData table, String customizerName)
   {
      Optional<QCodeReference> codeReference = table.getCustomizer(customizerName);
      if(codeReference.isPresent())
      {
         return (Optional.ofNullable(QCodeLoader.getAdHoc(TableCustomizerInterface.class, codeReference.get())));
      }
      return (Optional.empty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public static <T> T getAdHoc(Class<T> expectedType, QCodeReference codeReference)
   {
      if(codeReference == null)
      {
         return (null);
      }

      if(!codeReference.getCodeType().equals(QCodeType.JAVA))
      {
         ///////////////////////////////////////////////////////////////////////////////////////
         // todo - 1) support more languages, 2) wrap them w/ java Functions here, 3) profit! //
         ///////////////////////////////////////////////////////////////////////////////////////
         throw (new IllegalArgumentException("Only JAVA code references are supported at this time."));
      }

      try
      {
         Optional<Constructor<?>> constructor = constructorMemoization.getResultThrowing(codeReference.getName(), (UnsafeFunction<String, Constructor<?>, Exception>) s ->
         {
            Class<?> customizerClass = Class.forName(codeReference.getName());
            return customizerClass.getConstructor();
         });

         if(constructor.isPresent())
         {
            T t = (T) constructor.get().newInstance();

            ////////////////////////////////////////////////////////////////
            // if the object is initializable, then, well, initialize it! //
            ////////////////////////////////////////////////////////////////
            if(t instanceof InitializableViaCodeReference initializableViaCodeReference)
            {
               initializableViaCodeReference.initialize(codeReference);
            }

            return t;
         }
         else
         {
            LOG.error("Could not get constructor for code reference", logPair("codeReference", codeReference));
            return (null);
         }
      }
      catch(Exception e)
      {
         LOG.error("Error initializing codeReference", e, logPair("codeReference", codeReference));

         //////////////////////////////////////////////////////////////////////////////////////////////////////////
         // return null here - under the assumption that during normal run-time operations, we'll never hit here //
         // as we'll want to validate all functions in the instance validator at startup time (and IT will throw //
         // if it finds an invalid code reference                                                                //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////
         return (null);
      }
   }

}
