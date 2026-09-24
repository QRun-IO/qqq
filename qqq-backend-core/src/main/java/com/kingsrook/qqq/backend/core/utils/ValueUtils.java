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

package com.kingsrook.qqq.backend.core.utils;


import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** Utilities work values - e.g., type-cast-like operations
 *******************************************************************************/
public class ValueUtils
{
   private static final QLogger LOG = QLogger.getLogger(ValueUtils.class);

   private static final DateTimeFormatter dateTimeFormatter_yyyyMMddWithDashes = DateTimeFormatter.ofPattern("yyyy-MM-dd");
   private static final DateTimeFormatter dateTimeFormatter_MdyyyyWithSlashes  = DateTimeFormatter.ofPattern("M/d/yyyy");
   private static final DateTimeFormatter dateTimeFormatter_yyyyMMdd           = DateTimeFormatter.ofPattern("yyyyMMdd");



   /*******************************************************************************
    ** Type-safely make a String from any Object.
    *******************************************************************************/
   public static String getValueAsString(Object value)
   {
      if(value == null)
      {
         return (null);
      }
      else if(value instanceof String s)
      {
         return (s);
      }
      else if(value instanceof byte[] ba)
      {
         return (new String(ba));
      }
      else if(value instanceof PossibleValueEnum<?> pve)
      {
         return getValueAsString(pve.getPossibleValueId());
      }
      else
      {
         return (String.valueOf(value));
      }
   }



   /*******************************************************************************
    ** Returns null for null input;
    ** Returns the input object for Boolean-typed inputs.
    ** Then, follows Boolean.parseBoolean, returning true iff value is a case-insensitive
    ** match for "true", for String.valueOf the input
    *******************************************************************************/
   public static Boolean getValueAsBoolean(Object value)
   {
      if(value == null)
      {
         return (null);
      }
      else if(value instanceof Boolean b)
      {
         return (b);
      }
      else if(value instanceof String s)
      {
         return "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
      }
      else
      {
         return (Boolean.parseBoolean(String.valueOf(value)));
      }
   }



   /*******************************************************************************
    ** Type-safely make an Long from any Object.
    ** null and empty-string inputs return null.
    ** We try to strip away commas and decimals (as long as they are exactly equal to the int value)
    ** We may throw if the input can't be converted to an integer.
    *******************************************************************************/
   public static Long getValueAsLong(Object value) throws QValueException
   {
      try
      {
         if(value == null)
         {
            return (null);
         }
         else if(value instanceof Integer i)
         {
            return Long.valueOf((i));
         }
         else if(value instanceof Long l)
         {
            return (l);
         }
         else if(value instanceof BigInteger b)
         {
            return (b.longValue());
         }
         else if(value instanceof Float f)
         {
            if(f.longValue() != f)
            {
               throw (new QValueException(f + " does not have an exact integer representation."));
            }
            return (f.longValue());
         }
         else if(value instanceof Double d)
         {
            if(d.longValue() != d)
            {
               throw (new QValueException(d + " does not have an exact integer representation."));
            }
            return (d.longValue());
         }
         else if(value instanceof BigDecimal bd)
         {
            return bd.longValueExact();
         }
         else if(value instanceof PossibleValueEnum<?> pve)
         {
            return getValueAsLong(pve.getPossibleValueId());
         }
         else if(value instanceof String s)
         {
            if(!StringUtils.hasContent(s))
            {
               return (null);
            }

            try
            {
               return (Long.parseLong(s));
            }
            catch(NumberFormatException nfe)
            {
               if(s.contains(","))
               {
                  String sWithoutCommas = s.replaceAll(",", "");
                  try
                  {
                     return (getValueAsLong(sWithoutCommas));
                  }
                  catch(Exception ignore)
                  {
                     throw (nfe);
                  }
               }
               if(s.matches("[+-]?\\d+\\.0+"))
               {
                  String sWithoutDecimal = s.replaceAll("\\.0+$", "");
                  try
                  {
                     return (getValueAsLong(sWithoutDecimal));
                  }
                  catch(Exception ignore)
                  {
                     throw (nfe);
                  }
               }
               throw (nfe);
            }
         }
         else
         {
            throw (new QValueException("Unsupported class " + value.getClass().getName() + " for converting to Long."));
         }
      }
      catch(QValueException qve)
      {
         throw (qve);
      }
      catch(Exception e)
      {
         throw (new QValueException("Value [" + value + "] could not be converted to a Long.", e));
      }
   }



   /*******************************************************************************
    ** Type-safely make an Integer from any Object.
    ** null and empty-string inputs return null.
    ** We try to strip away commas and decimals (as long as they are exactly equal to the int value)
    ** We may throw if the input can't be converted to an integer.
    *******************************************************************************/
   public static Integer getValueAsInteger(Object value) throws QValueException
   {
      try
      {
         if(value == null)
         {
            return (null);
         }
         else if(value instanceof Integer i)
         {
            return (i);
         }
         else if(value instanceof BigInteger b)
         {
            return (b.intValue());
         }
         else if(value instanceof Long l)
         {
            return Math.toIntExact(l);
         }
         else if(value instanceof Float f)
         {
            if(f.intValue() != f)
            {
               throw (new QValueException(f + " does not have an exact integer representation."));
            }
            return (f.intValue());
         }
         else if(value instanceof Double d)
         {
            if(d.intValue() != d)
            {
               throw (new QValueException(d + " does not have an exact integer representation."));
            }
            return (d.intValue());
         }
         else if(value instanceof BigDecimal bd)
         {
            return bd.intValueExact();
         }
         else if(value instanceof PossibleValueEnum<?> pve)
         {
            return getValueAsInteger(pve.getPossibleValueId());
         }
         else if(value instanceof String s)
         {
            if(!StringUtils.hasContent(s))
            {
               return (null);
            }

            try
            {
               return (Integer.parseInt(s));
            }
            catch(NumberFormatException nfe)
            {
               if(s.contains(","))
               {
                  String sWithoutCommas = s.replaceAll(",", "");
                  try
                  {
                     return (getValueAsInteger(sWithoutCommas));
                  }
                  catch(Exception ignore)
                  {
                     throw (nfe);
                  }
               }
               if(s.matches("[+-]?\\d+\\.0+"))
               {
                  String sWithoutDecimal = s.replaceAll("\\.0+$", "");
                  try
                  {
                     return (getValueAsInteger(sWithoutDecimal));
                  }
                  catch(Exception ignore)
                  {
                     throw (nfe);
                  }
               }
               throw (nfe);
            }
         }
         else
         {
            throw (new QValueException("Unsupported class " + value.getClass().getName() + " for converting to Integer."));
         }
      }
      catch(QValueException qve)
      {
         throw (qve);
      }
      catch(Exception e)
      {
         throw (new QValueException("Value [" + value + "] could not be converted to an Integer.", e));
      }
   }



   /*******************************************************************************
    ** Type-safely make a LocalDateTime from any Object.
    ** null and empty-string inputs return null.
    ** We may throw if the input can't be converted to a LocalDateTime
    *******************************************************************************/
   public static LocalDateTime getValueAsLocalDateTime(Object value) throws QValueException
   {
      try
      {
         if(value == null)
         {
            return (null);
         }
         else if(value instanceof LocalDateTime ldt)
         {
            return (ldt);
         }
         else if(value instanceof java.sql.Timestamp ts)
         {
            return ts.toLocalDateTime();
         }
         else if(value instanceof Calendar c)
         {
            TimeZone tz  = c.getTimeZone();
            ZoneId   zid = (tz == null) ? ZoneId.systemDefault() : tz.toZoneId();
            return LocalDateTime.ofInstant(c.toInstant(), zid);
         }
         else if(value instanceof String s)
         {
            if(!StringUtils.hasContent(s))
            {
               return (null);
            }

            return LocalDateTime.parse(s);
         }
         else
         {
            throw (new QValueException("Unsupported class " + value.getClass().getName() + " for converting to LocalDateTime."));
         }
      }
      catch(QValueException qve)
      {
         throw (qve);
      }
      catch(Exception e)
      {
         throw (new QValueException("Value [" + value + "] could not be converted to a LocalDateTime.", e));
      }
   }



   /*******************************************************************************
    ** Type-safely make a LocalDate from any Object.
    ** null and empty-string inputs return null.
    ** We may throw if the input can't be converted to a LocalDate
    *******************************************************************************/
   public static LocalDate getValueAsLocalDate(Object value) throws QValueException
   {
      try
      {
         if(value == null)
         {
            return (null);
         }
         else if(value instanceof LocalDate ld)
         {
            return (ld);
         }
         else if(value instanceof java.sql.Date d)
         {
            return d.toLocalDate();
         }
         else if(value instanceof java.util.Date d)
         {
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
         }
         else if(value instanceof Calendar c)
         {
            TimeZone tz  = c.getTimeZone();
            ZoneId   zid = (tz == null) ? ZoneId.systemDefault() : tz.toZoneId();
            return LocalDateTime.ofInstant(c.toInstant(), zid).toLocalDate();
         }
         else if(value instanceof Instant i)
         {
            return LocalDate.ofInstant(i, ZoneId.systemDefault()); // todo - where should the zone come from?
         }
         else if(value instanceof LocalDateTime ldt)
         {
            return ldt.toLocalDate();
         }
         else if(value instanceof String s)
         {
            if(!StringUtils.hasContent(s))
            {
               return (null);
            }

            return tryLocalDateParsers(s);
         }
         else
         {
            throw (new QValueException("Unsupported class " + value.getClass().getName() + " for converting to LocalDate."));
         }
      }
      catch(QValueException qve)
      {
         throw (qve);
      }
      catch(Exception e)
      {
         throw (new QValueException("Value [" + value + "] could not be converted to a LocalDate.", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static LocalDate tryLocalDateParsers(String s)
   {
      DateTimeParseException lastException = null;
      for(DateTimeFormatter dateTimeFormatter : List.of(dateTimeFormatter_yyyyMMddWithDashes, dateTimeFormatter_MdyyyyWithSlashes, dateTimeFormatter_yyyyMMdd))
      {
         try
         {
            return LocalDate.parse(s, dateTimeFormatter);
         }
         catch(DateTimeParseException dtpe)
         {
            lastException = dtpe;
         }
      }
      throw (new QValueException("Could not parse value [" + s + "] to a local date", lastException));
   }



   /*******************************************************************************
    ** Type-safely make a BigDecimal from any Object.
    ** null and empty-string inputs return null.
    ** We may throw if the input can't be converted to a BigDecimal
    *******************************************************************************/
   public static BigDecimal getValueAsBigDecimal(Object value) throws QValueException
   {
      try
      {
         if(value == null)
         {
            return (null);
         }
         else if(value instanceof BigDecimal bd)
         {
            return (bd);
         }
         else if(value instanceof Integer i)
         {
            return new BigDecimal(i);
         }
         else if(value instanceof Long l)
         {
            return new BigDecimal(l);
         }
         else if(value instanceof Float f)
         {
            return new BigDecimal(f);
         }
         else if(value instanceof Double d)
         {
            return new BigDecimal(d);
         }
         else if(value instanceof String s)
         {
            if(!StringUtils.hasContent(s))
            {
               return (null);
            }

            try
            {
               return (new BigDecimal(s));
            }
            catch(NumberFormatException nfe)
            {
               if(s.contains(","))
               {
                  String sWithoutCommas = s.replaceAll(",", "");
                  try
                  {
                     return (getValueAsBigDecimal(sWithoutCommas));
                  }
                  catch(Exception ignore)
                  {
                     throw (nfe);
                  }
               }
               throw (nfe);
            }
         }
         else
         {
            throw (new QValueException("Unsupported class " + value.getClass().getName() + " for converting to BigDecimal."));
         }
      }
      catch(QValueException qve)
      {
         throw (qve);
      }
      catch(Exception e)
      {
         throw (new QValueException("Value [" + value + "] could not be converted to a BigDecimal.", e));
      }
   }



   /*******************************************************************************
    ** Type-safely make an Instant from any Object.
    ** null and empty-string inputs return null.
    ** We may throw if the input can't be converted to a Instant
    *******************************************************************************/
   public static Instant getValueAsInstant(Object value)
   {
      try
      {
         if(value == null)
         {
            return (null);
         }
         else if(value instanceof Instant i)
         {
            return (i);
         }
         else if(value instanceof java.sql.Date d)
         {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // note - in the jdk, this method throws UnsupportedOperationException (because of the lack of time in sql Dates) //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            return d.toInstant();
         }
         else if(value instanceof java.util.Date d)
         {
            return d.toInstant();
         }
         else if(value instanceof Calendar c)
         {
            return (c.toInstant());
         }
         else if(value instanceof LocalDateTime ldt)
         {
            ZoneId zoneId = ZoneId.systemDefault();
            return ldt.toInstant(zoneId.getRules().getOffset(ldt));
         }
         else if(value instanceof String s)
         {
            if(!StringUtils.hasContent(s))
            {
               return (null);
            }

            try
            {
               /////////////////////////////////////////////////////////////////////////////////////
               // first assume the instant is perfectly formatted, as in: 2007-12-03T10:15:30.00Z //
               /////////////////////////////////////////////////////////////////////////////////////
               return Instant.parse(s);
            }
            catch(DateTimeParseException e)
            {
               try
               {
                  /////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // if the string isn't quite the right format, try some alternates that are common and fairly un-vague //
                  /////////////////////////////////////////////////////////////////////////////////////////////////////////
                  return tryAlternativeInstantParsing(s, e);
               }
               catch(DateTimeParseException dtpe)
               {
                  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // we commonly receive date-times with only a single-digit hour after the space, which fails tryAlternativeInstantParsing. //
                  // so if we see what looks like that pattern, zero-pad the hour, and try the alternative parse patterns again.             //
                  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  if(s.matches(".* \\d:.*"))
                  {
                     return tryAlternativeInstantParsing(s.replaceFirst(" (\\d):", " 0$1:"), e);
                  }
                  else
                  {
                     throw (dtpe);
                  }
               }
            }
         }
         else
         {
            throw (new QValueException("Unsupported class " + value.getClass().getName() + " for converting to Instant."));
         }
      }
      catch(QValueException qve)
      {
         throw (qve);
      }
      catch(Exception e)
      {
         throw (new QValueException("Value [" + value + "] could not be converted to a Instant.", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Instant tryAlternativeInstantParsing(String s, DateTimeParseException e) throws DateTimeParseException
   {
      ////////////////////////////////////////////////////////////////////
      // 1999-12-31T12:59                                               //
      // missing seconds & zone - but we're happy to assume :00 and UTC //
      ////////////////////////////////////////////////////////////////////
      if(s.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$"))
      {
         //////////////////////////
         // todo ... time zone?? //
         //////////////////////////
         return Instant.parse(s + ":00Z");
      }

      ///////////////////////////////////////////////////////////////
      // 1999-12-31 12:59:59.0                                     //
      // fractional seconds and no zone - truncate, and assume UTC //
      ///////////////////////////////////////////////////////////////
      else if(s.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.0$"))
      {
         s = s.replaceAll(" ", "T").replaceAll("\\..*$", "Z");
         return Instant.parse(s);
      }

      ////////////////////////////////////////////
      // 1999-12-31 12:59:59                    //
      // Missing 'T' and 'Z', so just add those //
      ////////////////////////////////////////////
      else if(s.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$"))
      {
         s = s.replaceAll(" ", "T") + "Z";
         return Instant.parse(s);
      }

      /////////////////////////////////////////////
      // 1999-12-31 12:59                        //
      // missing T, seconds, and Z - add 'em all //
      /////////////////////////////////////////////
      else if(s.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$"))
      {
         s = s.replaceAll(" ", "T") + ":00Z";
         return Instant.parse(s);
      }

      else
      {
         try
         {
            ////////////////////////////////////////////////////////
            // such as '2011-12-03T10:15:30+01:00[Europe/Paris]'. //
            ////////////////////////////////////////////////////////
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant(ZoneOffset.UTC);
         }
         catch(DateTimeParseException e2)
         {
            try
            {
               ///////////////////////////////////////////////////////
               // also includes such as '2011-12-03T10:15:30+01:00' //
               ///////////////////////////////////////////////////////
               return LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC);
            }
            catch(Exception e3)
            {
               // just throw the original
            }
         }

         throw (e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static LocalTime getValueAsLocalTime(Object value)
   {
      try
      {
         if(value == null)
         {
            return (null);
         }
         else if(value instanceof LocalTime lt)
         {
            return (lt);
         }
         else if(value instanceof String s)
         {
            if(!StringUtils.hasContent(s))
            {
               return (null);
            }

            return LocalTime.parse(s);
         }
         else
         {
            throw (new QValueException("Unsupported class " + value.getClass().getName() + " for converting to LocalTime."));
         }
      }
      catch(QValueException qve)
      {
         throw (qve);
      }
      catch(Exception e)
      {
         throw (new QValueException("Value [" + value + "] could not be converted to a LocalTime.", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static byte[] getValueAsByteArray(Object value)
   {
      if(value == null)
      {
         return (null);
      }
      else if(value instanceof byte[] ba)
      {
         return (ba);
      }
      else if(value instanceof String s)
      {
         return (s.getBytes(StandardCharsets.UTF_8));
      }
      else
      {
         throw (new QValueException("Unsupported class " + value.getClass().getName() + " for converting to ByteArray."));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public static <T extends Serializable> T getValueAsType(Class<T> type, Object value)
   {
      if(type.equals(Integer.class))
      {
         return (T) getValueAsInteger(value);
      }
      else if(type.equals(String.class))
      {
         return (T) getValueAsString(value);
      }
      else if(type.equals(Boolean.class))
      {
         return (T) getValueAsBoolean(value);
      }
      else if(type.equals(BigDecimal.class))
      {
         return (T) getValueAsBigDecimal(value);
      }
      else if(type.equals(LocalDateTime.class))
      {
         return (T) getValueAsLocalDateTime(value);
      }
      else if(type.equals(LocalDate.class))
      {
         return (T) getValueAsLocalDate(value);
      }
      else if(type.equals(Instant.class))
      {
         return (T) getValueAsInstant(value);
      }
      else if(type.equals(byte[].class))
      {
         return (T) getValueAsByteArray(value);
      }
      else
      {
         throw new QValueException("Unsupported type [" + type.getSimpleName() + "] in getValueAsType.");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Serializable getValueAsFieldType(QFieldType type, Object value)
   {
      return switch(type)
      {
         case STRING, TEXT, HTML, PASSWORD -> getValueAsString(value);
         case INTEGER -> getValueAsInteger(value);
         case LONG -> getValueAsLong(value);
         case DECIMAL -> getValueAsBigDecimal(value);
         case BOOLEAN -> getValueAsBoolean(value);
         case DATE -> getValueAsLocalDate(value);
         case TIME -> getValueAsLocalTime(value);
         case DATE_TIME -> getValueAsInstant(value);
         case BLOB -> getValueAsByteArray(value);
      };
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Instant getStartOfTodayInZoneId(String zoneId)
   {
      return (getStartOfDayInZoneId(Instant.now(), zoneId));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Instant getStartOfDayInZoneId(Instant when, String zoneId)
   {
      ZoneId zone = ZoneId.of(zoneId);

      //////////////////////////////////////////////////////////////////////////////
      // get date time for now in given zone, truncate it and add offset from utc //
      //////////////////////////////////////////////////////////////////////////////
      LocalDateTime givenZonesNow = LocalDateTime.ofInstant(when, zone);
      LocalDateTime startOfDay    = givenZonesNow.truncatedTo(ChronoUnit.DAYS);
      return (startOfDay.toInstant(zone.getRules().getOffset(startOfDay)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Instant getStartOfMonthInZoneId(String zoneId)
   {
      return (getStartOfMonthInZoneId(Instant.now(), zoneId));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Instant getStartOfMonthInZoneId(Instant when, String zoneId)
   {
      ZoneId zone = ZoneId.of(zoneId);

      //////////////////////////////////////////////////////////////////////////////
      // get date time for now in given zone, truncate it and add offset from utc //
      //////////////////////////////////////////////////////////////////////////////
      LocalDateTime givenZonesNow = LocalDateTime.ofInstant(when, zone);
      LocalDateTime startOfMonth = givenZonesNow
         .withDayOfMonth(1)
         .with(ChronoField.HOUR_OF_DAY, 0)
         .with(ChronoField.MINUTE_OF_DAY, 0)
         .with(ChronoField.SECOND_OF_DAY, 0)
         .with(ChronoField.NANO_OF_DAY, 0);
      return (startOfMonth.toInstant(zone.getRules().getOffset(startOfMonth)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Instant getStartOfYearInZoneId(String zoneId)
   {
      return getStartOfYearInZoneId(Instant.now(), zoneId);
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   public static Instant getStartOfYearInZoneId(Instant when, String zoneId)
   {
      ZoneId zone = ZoneId.of(zoneId);

      //////////////////////////////////////////////////////////////////////////////
      // get date time for now in given zone, truncate it and add offset from utc //
      //////////////////////////////////////////////////////////////////////////////
      LocalDateTime givenZonesNow = LocalDateTime.ofInstant(when, zone);
      LocalDateTime startOfYear = givenZonesNow
         .withDayOfYear(1)
         .with(ChronoField.HOUR_OF_DAY, 0)
         .with(ChronoField.MINUTE_OF_DAY, 0)
         .with(ChronoField.SECOND_OF_DAY, 0)
         .with(ChronoField.NANO_OF_DAY, 0);
      return (startOfYear.toInstant(zone.getRules().getOffset(startOfYear)));
   }



   /*******************************************************************************
    ** Return the first argument that isn't null.
    ** If all were null, return null.
    *******************************************************************************/
   @SafeVarargs
   public static <T> T getFirstNonNull(T... ts)
   {
      if(ts == null || ts.length == 0)
      {
         return (null);
      }

      for(T t : ts)
      {
         if(t != null)
         {
            return (t);
         }
      }

      return (null);
   }



   /*******************************************************************************
    ** Get the (time) zoneId either for the current user session (based on session
    ** value UserTimezone or UserTimezoneOffsetMinutes), else the instance's
    ** defaultTimeZoneId string.
    *******************************************************************************/
   public static ZoneId getSessionOrInstanceZoneId()
   {
      String timezone = QContext.getQSession().getValue(QSession.VALUE_KEY_USER_TIMEZONE);
      if(StringUtils.hasContent(timezone))
      {
         return (ZoneId.of(timezone));
      }

      String userTimezoneOffsetMinutesString = QContext.getQSession().getValue(QSession.VALUE_KEY_USER_TIMEZONE_OFFSET_MINUTES);
      if(StringUtils.hasContent(userTimezoneOffsetMinutesString))
      {
         return (ZoneId.ofOffset("UTC", ZoneOffset.ofTotalSeconds(60 * Integer.parseInt(userTimezoneOffsetMinutesString))));
      }

      return (ZoneId.of(QContext.getQInstance().getDefaultTimeZoneId()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static QFieldType inferQFieldTypeFromValue(Serializable value, QFieldType defaultIfCannotInfer)
   {
      if(value instanceof String)
      {
         return QFieldType.STRING;
      }
      else if(value instanceof Integer)
      {
         return QFieldType.INTEGER;
      }
      else if(value instanceof Long)
      {
         return QFieldType.LONG;
      }
      else if(value instanceof BigDecimal)
      {
         return QFieldType.DECIMAL;
      }
      else if(value instanceof Boolean)
      {
         return QFieldType.BOOLEAN;
      }
      else if(value instanceof Instant)
      {
         return QFieldType.DATE_TIME;
      }
      else if(value instanceof LocalDate)
      {
         return QFieldType.DATE;
      }
      else if(value instanceof LocalTime)
      {
         return QFieldType.TIME;
      }

      LOG.debug("Could not infer QFieldType from value [" + (value == null ? "null" : value.getClass().getSimpleName()) + "]");

      return defaultIfCannotInfer;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static Map getValueAsMap(Serializable value)
   {
      if(value == null)
      {
         return (null);
      }
      else if(value instanceof Map<?, ?> map)
      {
         return (map);
      }
      else if(value instanceof String string && string.startsWith("{") && string.endsWith("}"))
      {
         try
         {
            Map map = JsonUtils.toObject(string, Map.class);
            return (map);
         }
         catch(IOException e)
         {
            throw new QValueException("Error parsing string to map", e);
         }
      }
      else
      {
         throw new QValueException("Unrecognized object type in getValueAsMap: " + value.getClass().getSimpleName());
      }
   }
}
