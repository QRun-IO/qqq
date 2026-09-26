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

package com.kingsrook.qqq.backend.core.actions.dashboard.widgets;


import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.dashboard.AbstractHTMLWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetCount;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class NoCodeWidgetVelocityUtils
{
   private static final QLogger LOG = QLogger.getLogger(NoCodeWidgetVelocityUtils.class);

   private Map<String, Object> context;
   private RenderWidgetInput   input;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public NoCodeWidgetVelocityUtils(Map<String, Object> context, RenderWidgetInput input)
   {
      this.context = context;
      this.input = input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String icon(String iconName, String color)
   {
      return String.format("""
         <span class="material-icons-round notranslate MuiIcon-root MuiIcon-fontSizeInherit" style="color: %s; position: relative; top: 6px;" aria-hidden="true">%s</span>
         """, color, iconName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String helpIcon()
   {
      return (icon("help_outline", "blue"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String errorIcon()
   {
      return (icon("error_outline", "red"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String warningIcon()
   {
      return (icon("warning", "orange"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String checkIcon()
   {
      return (icon("check", "green"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String pendingIcon()
   {
      return (icon("pending", "#0062ff"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String spanColorGreen()
   {
      return ("""
         <span style="color: green;">
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String spanColorOrange()
   {
      return ("""
         <span style="color: orange;">
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String spanColorRed()
   {
      return ("""
         <span style="color: red;">
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String plural(Integer size, String ifOne, String ifNotOne)
   {
      return StringUtils.plural(size, ifOne, ifNotOne);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String formatDateTime(Instant i)
   {
      if(i == null)
      {
         return ("");
      }
      return QValueFormatter.formatDateTimeWithZone(i.atZone(ZoneId.of(QContext.getQInstance().getDefaultTimeZoneId())));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String formatSecondsAsDuration(Integer seconds)
   {
      StringBuilder rs = new StringBuilder();

      if(seconds == null)
      {
         return ("");
      }

      int secondsPerDay = 24 * 60 * 60;
      if(seconds >= secondsPerDay)
      {
         int days = seconds / (secondsPerDay);
         seconds = seconds % secondsPerDay;
         rs.append(days).append(StringUtils.plural(days, " day", " days")).append(" ");
      }

      int secondsPerHour = 60 * 60;
      if(seconds >= secondsPerHour)
      {
         int hours = seconds / (secondsPerHour);
         seconds = seconds % secondsPerHour;
         rs.append(hours).append(StringUtils.plural(hours, " hour", " hours")).append(" ");
      }

      int secondsPerMinute = 60;
      if(seconds >= secondsPerMinute)
      {
         int minutes = seconds / (secondsPerMinute);
         seconds = seconds % secondsPerMinute;
         rs.append(minutes).append(StringUtils.plural(minutes, " minute", " minutes")).append(" ");
      }

      if(seconds > 0 || rs.length() == 0)
      {
         rs.append(seconds).append(StringUtils.plural(seconds, " second", " seconds")).append(" ");
      }

      if(rs.length() > 0)
      {
         rs.deleteCharAt(rs.length() - 1);
      }

      return (rs.toString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String formatSecondsAsRoundedDuration(Integer seconds)
   {
      StringBuilder rs = new StringBuilder();

      if(seconds == null)
      {
         return ("");
      }

      int secondsPerDay = 24 * 60 * 60;
      if(seconds >= secondsPerDay)
      {
         int days = seconds / (secondsPerDay);
         return (days + StringUtils.plural(days, " day", " days"));
      }

      int secondsPerHour = 60 * 60;
      if(seconds >= secondsPerHour)
      {
         int hours = seconds / (secondsPerHour);
         return (hours + StringUtils.plural(hours, " hour", " hours"));
      }

      int secondsPerMinute = 60;
      if(seconds >= secondsPerMinute)
      {
         int minutes = seconds / (secondsPerMinute);
         return (minutes + StringUtils.plural(minutes, " minute", " minutes"));
      }

      if(seconds > 0 || rs.length() == 0)
      {
         return (seconds + StringUtils.plural(seconds, " second", " seconds"));
      }

      return ("");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String tableCountFilterLink(String countVariableName, String singular, String plural) throws QException
   {
      try
      {
         WidgetCount  widgetCount = (WidgetCount) context.get(countVariableName + ".source");
         Integer      count       = ValueUtils.getValueAsInteger(context.get(countVariableName));
         QQueryFilter filter      = widgetCount.getEffectiveFilter(input);
         return (AbstractHTMLWidgetRenderer.aHrefTableFilterNoOfRecords(widgetCount.getTableName(), filter, count, singular, plural));
      }
      catch(Exception e)
      {
         LOG.warn("Error rendering widget link", e);
         return ("");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String format(String displayFormat, Serializable value)
   {
      return (QValueFormatter.formatValue(displayFormat, value));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String round(BigDecimal input, int digits)
   {
      return String.valueOf(input.setScale(digits, RoundingMode.HALF_UP));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Object ifElse(Object ifObject, Object elseObject)
   {
      if(StringUtils.hasContent(ValueUtils.getValueAsString(ifObject)))
      {
         return (ifObject);
      }
      else if(StringUtils.hasContent(ValueUtils.getValueAsString(elseObject)))
      {
         return (elseObject);
      }

      return ("");
   }

}
