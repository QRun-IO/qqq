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

package com.kingsrook.qqq.backend.core.model.metadata.security;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock.NullValueBehavior;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility for working with security key, nullValueBehaviors.
 *******************************************************************************/
public class NullValueBehaviorUtil
{
   private static final QLogger LOG = QLogger.getLogger(NullValueBehaviorUtil.class);



   /*******************************************************************************
    ** Look at a RecordSecurityLock, but also the active session - and if the session
    ** has a null-value-behavior key for the lock's key-type, then allow that behavior
    ** to override the lock's default.
    *******************************************************************************/
   public static NullValueBehavior getEffectiveNullValueBehavior(RecordSecurityLock recordSecurityLock)
   {
      QSecurityKeyType securityKeyType = QContext.getQInstance().getSecurityKeyType(recordSecurityLock.getSecurityKeyType());
      if(StringUtils.hasContent(securityKeyType.getNullValueBehaviorKeyName()))
      {
         List<Serializable> nullValueSessionValueList = QContext.getQSession().getSecurityKeyValues(securityKeyType.getNullValueBehaviorKeyName());
         if(CollectionUtils.nullSafeHasContents(nullValueSessionValueList))
         {
            NullValueBehavior nullValueBehavior = NullValueBehavior.tryToGetFromString(ValueUtils.getValueAsString(nullValueSessionValueList.get(0)));
            if(nullValueBehavior != null)
            {
               return nullValueBehavior;
            }
            else
            {
               LOG.info("Unexpected value in nullValueBehavior security key.  Will use recordSecurityLock's nullValueBehavior",
                  logPair("nullValueBehaviorKeyName", securityKeyType.getNullValueBehaviorKeyName()),
                  logPair("value", nullValueSessionValueList.get(0)));
            }
         }
      }

      return (recordSecurityLock.getNullValueBehavior());
   }

}
