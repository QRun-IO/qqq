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

package com.kingsrook.qqq.backend.core.health;


import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Flat typed details. The returned map is immutable; all writes are sanitized.
 *******************************************************************************/
public class HealthDetails
{
   private static final QLogger LOG = QLogger.getLogger(HealthDetails.class);
   private static final Pattern KEY = Pattern.compile("[a-zA-Z][a-zA-Z0-9_.]*");
   private static final Pattern FORBIDDEN = Pattern.compile("(?i)password|secret|token|key|credential|authorization");
   private static final Set<String> WARNED_CHECKS = ConcurrentHashMap.newKeySet();
   private final Map<String, Object> values = new LinkedHashMap<>();



   /*******************************************************************************
    ** Add one safe scalar. Forbidden keys retain only a redaction marker.
    *******************************************************************************/
   public HealthDetails withValue(String key, Object value)
   {
      if(key == null || !KEY.matcher(key).matches())
      {
         throw new IllegalArgumentException("Invalid health detail key");
      }
      if(FORBIDDEN.matcher(key).find())
      {
         values.put(key, HealthRedactor.REDACTED);
         String checkName = String.valueOf(QContext.getObject("healthCheckName"));
         if(!HealthRedactor.REDACTED.equals(value) && WARNED_CHECKS.add(checkName))
         {
            LOG.warn("A forbidden health detail was redacted", logPair("check", HealthRedactor.redact(checkName)));
         }
         return (this);
      }
      if(!(value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Instant))
      {
         throw new IllegalArgumentException("Health details accept only strings, numbers, booleans and instants");
      }
      if(value instanceof Number number && !Double.isFinite(number.doubleValue()))
      {
         throw new IllegalArgumentException("Health detail numbers must be finite");
      }
      if(value instanceof String string)
      {
         String redacted = HealthRedactor.redact(string);
         value = redacted.substring(0, Math.min(500, redacted.length()));
      }
      values.put(key, value);
      return (this);
   }



   /*******************************************************************************
    ** Immutable view prevents bypassing type checks and redaction.
    *******************************************************************************/
   public Map<String, Object> getValues()
   {
      return (Collections.unmodifiableMap(values));
   }



   /*******************************************************************************
    ** Emit complete JSON within a 4 KiB UTF-8 budget, dropping trailing details.
    *******************************************************************************/
   public String toJson()
   {
      Map<String, Object> bounded = new LinkedHashMap<>();
      for(Map.Entry<String, Object> entry : values.entrySet())
      {
         bounded.put(entry.getKey(), entry.getValue());
         if(JsonUtils.toJson(bounded).getBytes(StandardCharsets.UTF_8).length > 4096)
         {
            bounded.remove(entry.getKey());
            break;
         }
      }
      return (JsonUtils.toJson(bounded));
   }
}
