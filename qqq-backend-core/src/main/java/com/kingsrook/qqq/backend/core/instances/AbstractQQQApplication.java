/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.instances;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.validation.plugins.QInstanceValidatorPluginInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Base class to provide the definition of a QQQ-based application.
 **
 ** Essentially, just how to define its meta-data - in the form of a QInstance.
 **
 ** Also provides means to define the instance validation plugins to be used.
 *******************************************************************************/
public abstract class AbstractQQQApplication
{

   /***************************************************************************
    **
    ***************************************************************************/
   public abstract QInstance defineQInstance() throws QException;



   /***************************************************************************
    **
    ***************************************************************************/
   public QInstance defineValidatedQInstance() throws QException, QInstanceValidationException
   {
      QInstance qInstance = defineQInstance();

      QInstanceValidator.removeAllValidatorPlugins();
      for(QInstanceValidatorPluginInterface<?> validatorPlugin : CollectionUtils.nonNullList(getValidatorPlugins()))
      {
         QInstanceValidator.addValidatorPlugin(validatorPlugin);
      }

      QInstanceValidator qInstanceValidator = new QInstanceValidator();
      qInstanceValidator.validate(qInstance);
      return (qInstance);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected List<QInstanceValidatorPluginInterface<?>> getValidatorPlugins()
   {
      return new ArrayList<>();
   }
}
