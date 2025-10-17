/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.scheduler;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 * class to give a human-friendly descriptive string from a cron expression.
 * Note that this implementation is written specifically for quartz, without much
 * thought to other cron variants.
 *
 * <p>description strings are like, for example:</p>
 * <ul>
 *    <li>Every day, at 11:00 am</li>
 *    <li>Every day, at 12:00 am, every 5 seconds between 00 and 59</li>
 *    <li>Every week, every day between Monday and Friday, at 12:00 am</li>
 *    <li>Every month, every day between the 10th and 15th, at 12:00 am</li>
 *    <li>In January, on the 1st, at 12:00 am</li>
 * </ul>
 *******************************************************************************/
public class CronDescriber
{
   private static final Map<Integer, String> weekdayNameMap           = new HashMap<>();
   private static final Map<String, Integer> weekdayByAbbreviationMap = new HashMap<>();
   private static final Map<Integer, String> monthNameMap             = new HashMap<>();
   private static final Map<String, Integer> monthByAbbreviationMap   = new HashMap<>();

   static
   {
      weekdayNameMap.put(1, "Sunday");
      weekdayNameMap.put(2, "Monday");
      weekdayNameMap.put(3, "Tuesday");
      weekdayNameMap.put(4, "Wednesday");
      weekdayNameMap.put(5, "Thursday");
      weekdayNameMap.put(6, "Friday");
      weekdayNameMap.put(7, "Saturday");

      weekdayByAbbreviationMap.put("SUN", 1);
      weekdayByAbbreviationMap.put("MON", 2);
      weekdayByAbbreviationMap.put("TUE", 3);
      weekdayByAbbreviationMap.put("WED", 4);
      weekdayByAbbreviationMap.put("THU", 5);
      weekdayByAbbreviationMap.put("FRI", 6);
      weekdayByAbbreviationMap.put("SAT", 7);

      monthNameMap.put(1, "January");
      monthNameMap.put(2, "February");
      monthNameMap.put(3, "March");
      monthNameMap.put(4, "April");
      monthNameMap.put(5, "May");
      monthNameMap.put(6, "June");
      monthNameMap.put(7, "July");
      monthNameMap.put(8, "August");
      monthNameMap.put(9, "September");
      monthNameMap.put(10, "October");
      monthNameMap.put(11, "November");
      monthNameMap.put(12, "December");

      monthByAbbreviationMap.put("JAN", 1);
      monthByAbbreviationMap.put("FEB", 2);
      monthByAbbreviationMap.put("MAR", 3);
      monthByAbbreviationMap.put("APR", 4);
      monthByAbbreviationMap.put("MAY", 5);
      monthByAbbreviationMap.put("JUN", 6);
      monthByAbbreviationMap.put("JUL", 7);
      monthByAbbreviationMap.put("AUG", 8);
      monthByAbbreviationMap.put("SEP", 9);
      monthByAbbreviationMap.put("OCT", 10);
      monthByAbbreviationMap.put("NOV", 11);
      monthByAbbreviationMap.put("DEC", 12);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   enum Field
   {
      SECONDS(0, 59, "second"),
      MINUTES(0, 59, "minute"),
      HOURS(0, 23, "hour"),
      DAY_OF_MONTH(1, 31, "day"),
      MONTH(1, 12, "month"),
      DAY_OF_WEEK(1, 7, "day"),
      YEAR(1970, Integer.MAX_VALUE, "year");

      final int    minValue;
      final int    maxValue;
      final String singularLabel;
      final String pluralLabel;



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      Field(int minValue, int maxValue, String singularLabel)
      {
         this.minValue = minValue;
         this.maxValue = maxValue;
         this.singularLabel = singularLabel;
         this.pluralLabel = singularLabel + "s";
      }
   }



   /***************************************************************************
    * Generate a human-readable description for the given cron expression
    *
    * @param cronExpression cron string to describe.  Must be a valid (quartz) cron string.
    * @return human-readable description of the cron string
    * @throws ParseException if wrong number of parts, or invalid values, etc.
    ***************************************************************************/
   public static String getDescription(String cronExpression) throws ParseException
   {
      String[] parts = cronExpression.trim().split("\\s+");
      if(parts.length < 6 || parts.length > 7)
      {
         throw new ParseException("Invalid cron expression: " + cronExpression, 0);
      }

      String seconds    = parts[0];
      String minutes    = parts[1];
      String hours      = parts[2];
      String dayOfMonth = parts[3];
      String month      = parts[4];
      String dayOfWeek  = parts[5];
      String year       = parts.length == 7 ? parts[6] : "*";

      List<String> phrases = new ArrayList<>();

      phrases.addAll(buildDayPhrases(dayOfMonth, month, dayOfWeek, year));
      phrases.addAll(buildTimePhrases(seconds, minutes, hours));

      String description = StringUtils.join(", ", phrases);
      return StringUtils.ucFirst(description);
   }



   /***************************************************************************
    * set the specified fieldName to the description for the given cronExpression
    * in the given record - and if there's an error generating the description,
    * then set the fieldName to an error message (don't throw).
    *
    * This gives a little degree of safety in case we can't describe a valid
    * cron string (it could happen!) e.g., to prevent an error from saving a
    * record if you were just calling record.setValue(fieldName, getDescription(...))
    * directly (e.g., and it threw).
    *
    * @param record record to set the value in
    * @param fieldName field name to set in the record
    * @param cronExpression cron string to describe
    ***************************************************************************/
   public static void setDescriptionInRecord(QRecord record, String fieldName, String cronExpression)
   {
      try
      {
         if(!StringUtils.hasContent(cronExpression))
         {
            record.setValue(fieldName, null);
         }
         else
         {
            record.setValue(fieldName, CronDescriber.getDescription(cronExpression));
         }
      }
      catch(Exception e)
      {
         record.setValue(fieldName, "Error generating description: " + e.getMessage());
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static List<String> buildTimePhrases(String seconds, String minutes, String hours) throws ParseException
   {
      List<String> rs = new ArrayList<>();

      List<Token> hourTokens   = tokenize(hours, Field.HOURS);
      List<Token> minuteTokens = tokenize(minutes, Field.MINUTES);
      List<Token> secondTokens = tokenize(seconds, Field.SECONDS);
      boolean     secondsIs0   = secondTokens.equals(List.of(new Scalar(0)));

      boolean hoursIsStar   = hours.equals("*");
      boolean minutesIsStar = minutes.equals("*");
      boolean secondsIsStar = seconds.equals("*");

      boolean minutesIsOneScalar = minuteTokens.size() == 1 && minuteTokens.get(0) instanceof Scalar;
      boolean secondsIsOneScalar = secondTokens.size() == 1 && secondTokens.get(0) instanceof Scalar;

      if(hoursIsStar && minutesIsStar && secondsIsStar)
      {
         return (List.of("every second"));
      }
      else if(hoursIsStar && minutesIsStar && secondsIs0)
      {
         return (List.of("every minute"));
      }
      else if(!isPlural(hourTokens) && !isPlural(minuteTokens))
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         // at a single hour and minute (like at 12:00 am or at 11:59:59 pm, or even at 3:45 am, every second) //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         int h = ((Scalar) hourTokens.get(0)).value;
         int m = ((Scalar) minuteTokens.get(0)).value;
         if(secondsIs0)
         {
            /////////////////
            // at 12:00 am //
            /////////////////
            rs.add("at " + get12Hour(h) + ":" + getZeroPadded(m) + " " + getAmPm(h));
         }
         else
         {
            if(isPlural(secondTokens))
            {
               //////////////////////////////////////
               // at 12:00 am, at seconds 10 to 15 //
               //////////////////////////////////////
               rs.add("at " + hmToTime(h, m));
               rs.add(buildSecondPhrase(secondTokens));
            }
            else
            {
               ////////////////////
               // at 12:00:15 am //
               ////////////////////
               int s = ((Scalar) secondTokens.get(0)).value;
               rs.add("at " + hmsToTime(h, m, s));
            }
         }
      }
      else if(minutesIsStar && secondsIsStar)
      {
         rs.add(buildHourPhrase(hourTokens));
         rs.add("every second");
      }
      else if(minutesIsStar && secondsIs0)
      {
         rs.add(buildHourPhrase(hourTokens));
         rs.add("every minute");
      }
      else if(hourTokens.stream().allMatch(t -> t instanceof Scalar) && minutesIsOneScalar && secondsIsOneScalar)
      {
         List<String> parts = new ArrayList<>();
         int          m     = ((Scalar) minuteTokens.get(0)).value;
         for(Token hourToken : hourTokens)
         {
            int h = ((Scalar) hourToken).value;
            if(secondsIs0)
            {
               parts.add(hmToTime(h, m));
            }
            else
            {
               int s = ((Scalar) secondTokens.get(0)).value;
               parts.add(hmsToTime(h, m, s));
            }
         }
         rs.add("at " + StringUtils.joinWithCommasAndAnd(parts));
      }
      else
      {
         rs.add(buildHourPhrase(hourTokens));
         rs.add(buildMinutePhrase(minuteTokens));
         if(!secondsIs0)
         {
            rs.add(buildSecondPhrase(secondTokens));
         }
      }

      return (rs);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static String buildHourPhrase(List<Token> tokens)
   {
      if(tokens.size() == 1 && new Range(0, 23).equals(tokens.get(0)))
      {
         return "every hour";
      }

      return (buildPhrase(tokens, Field.HOURS, "at", "", null));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static String buildMinutePhrase(List<Token> tokens)
   {
      return (buildPhrase(tokens, Field.MINUTES, "at", null, null));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static String buildSecondPhrase(List<Token> tokens)
   {
      return (buildPhrase(tokens, Field.SECONDS, "at", null, null));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static List<String> buildDayPhrases(String dayOfMonth, String month, String dayOfWeek, String year) throws ParseException
   {
      boolean monthIsStar                = month.equals("*");
      boolean yearIsStar                 = year.equals("*");
      boolean dayOfMonthIsQuestion       = dayOfMonth.equals("?");
      boolean dayOfWeekIsQuestion        = dayOfWeek.equals("?");
      boolean dayOfMonthIsStarOrQuestion = dayOfMonth.equals("*") || dayOfMonthIsQuestion;
      boolean dayOfWeekIsStarOrQuestion  = dayOfWeek.equals("*") || dayOfWeekIsQuestion;

      if(!dayOfWeekIsQuestion && !dayOfMonthIsQuestion)
      {
         throw (new ParseException("Cannot specify both day of month and day of week (one must be \"?\")", 0));
      }
      if(dayOfWeekIsQuestion && dayOfMonthIsQuestion)
      {
         throw (new ParseException("Cannot specify both day of month and day of week (only one can be \"?\")", 0));
      }

      if(dayOfMonthIsStarOrQuestion && dayOfWeekIsStarOrQuestion && monthIsStar && yearIsStar)
      {
         return (List.of("every day"));
      }

      List<String> rs = new ArrayList<>();
      if(!yearIsStar)
      {
         rs.add(buildYearPhrase(year));
      }

      if(!monthIsStar)
      {
         rs.add(buildMonthPhrase(month));
      }

      if(!dayOfWeekIsStarOrQuestion)
      {
         if(yearIsStar && monthIsStar)
         {
            rs.add("every week");
         }
         rs.add(buildWeekdayPhrase(dayOfWeek));
      }

      if(!dayOfMonthIsStarOrQuestion)
      {
         if(yearIsStar && monthIsStar)
         {
            rs.add("every month");
         }
         rs.add(buildDayOfMonthPhrase(dayOfMonth));
      }

      return (rs);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static String buildYearPhrase(String year) throws ParseException
   {
      List<Token> tokens = tokenize(year, Field.YEAR);
      return (buildPhrase(tokens, Field.YEAR, "in the", null, null));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static String buildMonthPhrase(String month) throws ParseException
   {
      List<Token> tokens = tokenize(month, Field.MONTH);
      return (buildPhrase(tokens, Field.MONTH, "in", "", null));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static String buildWeekdayPhrase(String dayOfWeek) throws ParseException
   {
      List<Token> tokens = tokenize(dayOfWeek, Field.DAY_OF_WEEK);
      return (buildPhrase(tokens, Field.DAY_OF_WEEK, "on", "", null));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static String buildDayOfMonthPhrase(String dayOfMonth) throws ParseException
   {
      List<Token> tokens = tokenize(dayOfMonth, Field.DAY_OF_MONTH);
      return (buildPhrase(tokens, Field.DAY_OF_MONTH, "on", "the", "the"));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static String buildPhrase(List<Token> tokens, Field field, String prefixLikeAt, String overrideSingularOrPluralLabelAfterAt, String wordAfterBetween)
   {
      boolean      needAnAt = true;
      List<String> parts    = new ArrayList<>();

      for(int i = 0; i < tokens.size(); i++)
      {
         Token token     = tokens.get(i);
         Token nextToken = i < tokens.size() - 1 ? tokens.get(i + 1) : null;

         if(token instanceof Range range)
         {
            parts.add("every " + field.singularLabel + " between " + (wordAfterBetween == null ? "" : (wordAfterBetween + " ")) + range.toString(field));
            needAnAt = true;
         }
         else if(token instanceof Step step)
         {
            parts.add(token.toString(field));
            needAnAt = true;
         }
         else
         {
            ////////////////////////////////////////////////////////////////////
            // peek at next to see if we want a singular or plural label here //
            ////////////////////////////////////////////////////////////////////
            String label = field.singularLabel;
            if(nextToken != null && nextToken instanceof Scalar)
            {
               label = field.pluralLabel;
            }
            if(overrideSingularOrPluralLabelAfterAt != null)
            {
               label = overrideSingularOrPluralLabelAfterAt;
            }
            if(!"".equals(label))
            {
               label += " ";
            }

            parts.add((needAnAt ? (prefixLikeAt + " " + label) : "") + token.toString(field));
            needAnAt = false;
         }
      }

      return (StringUtils.joinWithCommasAndAnd(parts));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static boolean isPlural(List<Token> tokens)
   {
      if(tokens.size() == 1 && !tokens.get(0).isPlural())
      {
         return false;
      }

      return (true);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static List<Token> tokenize(String input, Field field) throws ParseException
   {
      List<Token> rs = new ArrayList<>();
      for(String part : input.split(","))
      {
         if(part.contains("/"))
         {
            rs.add(Step.of(part, field));
         }
         else if(part.contains("-"))
         {
            rs.add(Range.of(part, field));
         }
         else if(part.equals("*"))
         {
            rs.add(new Range(field.minValue, field.maxValue));
         }
         else
         {
            rs.add(new Scalar(Token.parseToInt(part, field)));
         }
      }
      return (rs);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static String hmToTime(int h, int m)
   {
      return get12Hour(h) + ":" + getZeroPadded(m) + " " + getAmPm(h);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static String hmsToTime(int h, int m, int s)
   {
      return get12Hour(h) + ":" + getZeroPadded(m) + ":" + getZeroPadded(s) + " " + getAmPm(h);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static int get12Hour(int value)
   {
      if(value == 0 || value == 12)
      {
         return 12;
      }
      else if(value < 12)
      {
         return value;
      }
      else
      {
         return (value - 12);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static String getAmPm(int value)
   {
      if(value < 12)
      {
         return ("am");
      }
      else
      {
         return ("pm");
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static String getZeroPadded(int value)
   {
      return value < 10 ? "0" + value : String.valueOf(value);
   }



   /***************************************************************************
    * Token:
    * - Scalar (single value)
    * - Step (1/10 or * /10 or 5-10/10)
    *    - Scalar or Range, with stepSize
    * - Range (1-10, Mon-Thu)
    ***************************************************************************/
   interface Token
   {
      /***************************************************************************
       *
       ***************************************************************************/
      static int parseToInt(String input, Field field) throws ParseException
      {
         try
         {
            return (Integer.parseInt(input));
         }
         catch(Exception e)
         {
            String inputForMaps = StringUtils.safeTruncate(input.toUpperCase(), 3);
            return switch(field)
            {
               case DAY_OF_WEEK ->
               {
                  if(weekdayByAbbreviationMap.containsKey(inputForMaps))
                  {
                     yield (weekdayByAbbreviationMap.get(inputForMaps));
                  }
                  else
                  {
                     throw new ParseException("Invalid day of week: [" + input + "]", 0);
                  }
               }
               case MONTH ->
               {
                  if(monthByAbbreviationMap.containsKey(inputForMaps))
                  {
                     yield (monthByAbbreviationMap.get(inputForMaps));
                  }
                  else
                  {
                     throw new ParseException("Invalid month: [" + input + "]", 0);
                  }
               }
               default -> throw e instanceof ParseException pe ? pe :
                  e instanceof NumberFormatException ? new ParseException("Invalid number: [" + input + "]", 0) :
                     new ParseException("Error parsing [" + input + "]: " + e, 0);
            };
         }
      }


      /***************************************************************************
       *
       ***************************************************************************/
      static boolean isInt(String input)
      {
         try
         {
            Integer.parseInt(input);
            return true;
         }
         catch(Exception e)
         {
            return false;
         }
      }

      /***************************************************************************
       *
       ***************************************************************************/
      boolean isPlural();


      /***************************************************************************
       *
       ***************************************************************************/
      String toString(Field field);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   record Scalar(int value) implements Token
   {
      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public boolean isPlural()
      {
         return false;
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public String toString(Field field)
      {
         return switch(field)
         {
            case MONTH -> monthNameMap.get(value);
            case DAY_OF_WEEK -> weekdayNameMap.get(value);
            case DAY_OF_MONTH ->
            {
               if(value % 10 == 1 && value != 11)
               {
                  yield (value + "st");
               }
               else if(value % 10 == 2 && value != 12)
               {
                  yield (value + "nd");
               }
               else if(value % 10 == 3)
               {
                  yield (value + "rd");
               }

               yield (value + "th");
            }
            case MINUTES, SECONDS -> getZeroPadded(value);
            case HOURS -> get12Hour(value) + getAmPm(value);
            default -> String.valueOf(value);
         };
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   record Range(int from, int to) implements Token
   {

      /***************************************************************************
       *
       ***************************************************************************/
      public static Token of(int from, int to)
      {
         if(from == to)
         {
            return new Scalar(from);
         }

         return new Range(from, to);
      }



      /***************************************************************************
       *
       ***************************************************************************/
      public static Token of(String input, Field field) throws ParseException
      {
         if(input.equals("*"))
         {
            return Range.of(field.minValue, field.maxValue);
         }
         else if(input.contains("-"))
         {
            String[] parts = input.split("-", 2);
            if(parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty())
            {
               throw new ParseException("Incomplete range expression: [" + input + "]", 0);
            }

            if(parts[0].equals("*"))
            {
               ////////////////////////////////////////////////////////////////////////////////////////////////
               // if quartz sees * in first part, you get the whole range - not clear if others do the same? //
               ////////////////////////////////////////////////////////////////////////////////////////////////
               return Range.of(field.minValue, field.maxValue);
            }

            if(field.equals(Field.DAY_OF_WEEK) || field.equals(Field.MONTH))
            {
               ///////////////////////////////////////////////////////////////////////////////
               // in weekday and month, it fails upon mixes of numbers (or stars) and names //
               ///////////////////////////////////////////////////////////////////////////////
               boolean isIntOrStar0      = Token.isInt(parts[0]) || parts[0].equals("*");
               boolean isIntOrStar1      = Token.isInt(parts[1]) || parts[1].equals("*");
               boolean bothIntOrStar     = isIntOrStar0 && isIntOrStar1;
               boolean neitherIntNorStar = !isIntOrStar0 && !isIntOrStar1;
               if(!bothIntOrStar && !neitherIntNorStar)
               {
                  throw new ParseException("Range may not mix words and numbers: [" + input + "]", 0);
               }

               //////////////////////////////////////////////////////////////////////////////////
               // in weekday and month, if part 0 is > 3 long, you just get that value, scalar //
               //////////////////////////////////////////////////////////////////////////////////
               if(parts[0].length() > 3)
               {
                  return new Scalar(Token.parseToInt(parts[0], field));
               }
            }

            ////////////////////////////////////////////////////
            // okay, try to parse both parts and return range //
            ////////////////////////////////////////////////////
            return Range.of(Token.parseToInt(parts[0], field), Token.parseToInt(parts[1], field));
         }
         else
         {
            return Range.of(Token.parseToInt(input, field), field.maxValue);
         }
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public boolean isPlural()
      {
         return from != to;
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public String toString(Field field)
      {
         return new Scalar(from).toString(field) + " and " + new Scalar(to).toString(field);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   record Step(Token range, int stepSize) implements Token
   {

      /***************************************************************************
       *
       ***************************************************************************/
      public static Token of(String part, Field field) throws ParseException
      {
         String[] parts = part.split("/", 2);
         if(parts.length != 2)
         {
            throw new IllegalArgumentException("Incomplete step expression");
         }

         Token range = Range.of(parts[0], field);
         if(range instanceof Scalar scalar)
         {
            return scalar;
         }

         return new Step(range, Token.parseToInt(parts[1], field));
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public boolean isPlural()
      {
         return range.isPlural();
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public String toString(Field field)
      {
         if(range instanceof Scalar)
         {
            return range.toString(field);
         }

         String stepString = stepSize + " " + field.pluralLabel;
         if(stepSize == 1)
         {
            stepString = field.singularLabel;
         }

         return "every " + stepString + " between " + (field.equals(Field.DAY_OF_MONTH) ? "the " : "") + range.toString(field);
      }
   }

}
