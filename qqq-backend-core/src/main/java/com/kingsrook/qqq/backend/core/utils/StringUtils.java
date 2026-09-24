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


import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ibm.icu.text.Transliterator;


/*******************************************************************************
 ** Utility methods for working with Strings
 **
 *******************************************************************************/
public class StringUtils
{
   /*******************************************************************************
    ** test if string is not null and is not empty (after being trimmed).
    **
    ** @param input the string to test
    ** @return Boolean
    *******************************************************************************/
   public static Boolean hasContent(String input)
   {
      if(input != null && !input.trim().equals(""))
      {
         return true;
      }
      return false;
   }



   /*******************************************************************************
    ** returns input.toString() if not null, or nullOutput if input == null (as in SQL NVL)
    **
    *******************************************************************************/
   public static String nvl(Object input, String nullOutput)
   {
      if(input == null)
      {
         return nullOutput;
      }
      return input.toString();
   }



   /*******************************************************************************
    ** returns input if not null, or nullOutput if input == null (as in SQL NVL)
    **
    *******************************************************************************/
   public static String nvl(String input, String nullOutput)
   {
      if(input == null)
      {
         return nullOutput;
      }
      return input;
   }



   /*******************************************************************************
    ** allCapsToMixedCase - ie, UNIT_CODE -> Unit Code
    **
    ** @param input
    ** @return
    *******************************************************************************/
   public static String allCapsToMixedCase(String input)
   {
      if(input == null)
      {
         return (null);
      }

      StringBuilder rs = new StringBuilder();

      ///////////////////////////////////////////////////////////////////////
      // match for 0 or more non-capitals (which will pass through as-is), //
      //    then one capital (which will be kept uppercase),               //
      //    then 0 or more capitals (which will be lowercased),            //
      //    then 0 or more non-capitals (which will pass through as-is)    //
      //                                                                   //
      // Example matches are:                                              //
      // "UNIT CODE" -> ()(U)(NIT)( ), then ()(C)(ODE)() -> (Unit )(Code)  //
      // "UNITCODE"  -> ()(U)(NITCODE)(),                -> (Unitcode)     //
      // "UnitCode"  -> ()(U)()(nit), then ()(C)()(ode)  -> (Unit)(Code)   //
      // "UNIT0CODE" -> ()(U)(NIT)(0), then ()(C)(ODE)() -> (Unit0)(Code)  //
      // "0UNITCODE" -> (0)(U)(NITCODE)()                -> (0Unitcode)    //
      ///////////////////////////////////////////////////////////////////////
      Pattern pattern = Pattern.compile("([^A-Z]*)([A-Z])([A-Z]*)([^A-Z]*)");
      Matcher matcher = pattern.matcher(input);
      while(matcher.find())
      {
         rs.append(matcher.group(1) != null ? matcher.group(1) : "");
         rs.append(matcher.group(2) != null ? matcher.group(2) : "");
         rs.append(matcher.group(3) != null ? matcher.group(3).toLowerCase() : "");
         rs.append(matcher.group(4) != null ? matcher.group(4) : "");
      }

      ///////////////////////////////////////////////////////////////////////
      // just in case no match was found, return the original input string //
      ///////////////////////////////////////////////////////////////////////
      if(rs.length() == 0)
      {
         return (input);
      }

      return (rs.toString().replace('_', ' '));
   }



   /*******************************************************************************
    ** Truncate to at most maxLength UTF-16 units without splitting a surrogate pair.
    **
    *******************************************************************************/
   public static String safeTruncate(String input, int maxLength)
   {
      if(input == null)
      {
         return (null);
      }

      if(input.length() <= maxLength)
      {
         return (input);
      }

      if(maxLength > 0 && Character.isHighSurrogate(input.charAt(maxLength - 1)) && Character.isLowSurrogate(input.charAt(maxLength)))
      {
         maxLength--;
      }
      return (input.substring(0, maxLength));
   }



   /*******************************************************************************
    ** null- and index-bounds- safely truncate a string to a max length, appending
    ** a suffix (like "...") if it did get truncated.  Note that the returned string,
    ** with the suffix added, will be at most maxLength UTF-16 units. If the suffix
    ** alone exceeds maxLength, truncate the suffix. Preserve surrogate pairs.
    **
    *******************************************************************************/
   public static String safeTruncate(String input, int maxLength, String suffix)
   {
      if(input == null)
      {
         return (null);
      }

      if(input.length() <= maxLength)
      {
         return (input);
      }

      if(suffix.length() >= maxLength)
      {
         return (safeTruncate(suffix, maxLength));
      }
      return (safeTruncate(input, maxLength - suffix.length()) + suffix);
   }



   /*******************************************************************************
    ** safely appends a string to another, changing empty string if either value is null
    **
    *******************************************************************************/
   public static String safeAppend(String input, String contentToAppend)
   {
      input = input != null ? input : "";
      contentToAppend = contentToAppend != null ? contentToAppend : "";
      return input + contentToAppend;
   }



   /*******************************************************************************
    ** returns input if not null, or nullOutput if input == null (as in SQL NVL)
    **
    *******************************************************************************/
   public static String safeTrim(String input)
   {
      if(input == null)
      {
         return null;
      }
      return input.trim();
   }



   /*******************************************************************************
    ** Join a collection of objects into 1 string
    **
    ** @param   glue - String to insert between entries
    ** @param   collection - The collection of objects to join.
    ** @return String
    *******************************************************************************/
   public static String join(String glue, Collection<? extends Object> collection)
   {
      if(collection == null)
      {
         return (null);
      }

      StringBuffer rs = new StringBuffer();

      int i = 0;
      for(Object s : collection)
      {
         if(i++ > 0)
         {
            rs.append(glue);
         }
         rs.append(String.valueOf(s));
      }

      return (rs.toString());
   }



   /*******************************************************************************
    ** joinWithCommasAndAnd
    **
    ** [one] => [one]
    ** [one, two] => [one and two]
    ** [one, two, three] => [one, two, and three]
    ** [one, two, three, four] => [one, two, three, and four]
    ** etc.
    **
    ** @param input
    ** @return
    *******************************************************************************/
   public static String joinWithCommasAndAnd(List<String> input)
   {
      if(input == null)
      {
         return (null);
      }

      StringBuilder rs   = new StringBuilder();
      int           size = input.size();

      for(int i = 0; i < size; i++)
      {
         if(i > 0 && size == 2)
         {
            rs.append(" and ");
         }
         else if(i > 0 && i < size - 1)
         {
            rs.append(", ");
         }
         else if(i > 0 && i == size - 1)
         {
            rs.append(", and ");
         }
         rs.append(input.get(i));
      }

      return (rs.toString());

   }



   /*******************************************************************************
    ** Trims leading white spaces from a String. Returns a blank ("") value if NULL
    **
    ** @param  input
    ** @return String
    *******************************************************************************/
   public static String ltrim(String input)
   {
      if(!hasContent(input))
      {
         return "";
      }

      int i = 0;
      while(i < input.length() && Character.isWhitespace(input.charAt(i)))
      {
         i++;
      }
      return (input.substring(i));
   }



   /*******************************************************************************
    ** Trims trailing white spaces from a String. Returns a blank ("") value if NULL
    **
    ** @param  input
    ** @return String
    *******************************************************************************/
   public static String rtrim(String input)
   {
      if(!hasContent(input))
      {
         return "";
      }

      int i = input.length() - 1;
      while(i > 0 && Character.isWhitespace(input.charAt(i)))
      {
         i--;
      }
      return (input.substring(0, i + 1));
   }



   /*******************************************************************************
    ** Given a "formatString" containing any number of {singular,plural} style "tokens",
    ** replace the "tokens" with the "singular" options if the 'size' parameter is 1
    ** or the "plural" options if not-1 (e.g., 0 or 2+)
    **
    ** e.g.: StringUtils.pluralFormat(n, "Apple{,s} {was,were} eaten")) // seems easier.
    ** e.g.: StringUtils.pluralFormat(n, "Apple{ was,s were} eaten")) // also works...
    *******************************************************************************/
   public static String pluralFormat(Integer size, String formatString)
   {
      int           lastIndex = 0;
      StringBuilder output    = new StringBuilder();

      Pattern pattern = Pattern.compile("\\{.*?,.*?}");
      Matcher matcher = pattern.matcher(formatString);
      while(matcher.find())
      {
         String   group       = matcher.group();
         String   groupBody   = group.substring(1, group.length() - 1);
         String[] groupParts  = groupBody.split(",", 2);
         String   replacement = (size == 1) ? groupParts[0] : groupParts[1];
         output.append(formatString, lastIndex, matcher.start()).append(replacement);

         lastIndex = matcher.end();
      }

      if(lastIndex < formatString.length())
      {
         output.append(formatString, lastIndex, formatString.length());
      }

      return (output.toString());
   }



   /*******************************************************************************
    ** Switch between strings based on if the size of the parameter collection.  If
    ** it is 1 (the singular) or not-1 (0 or 2+, the plural). Get back "" or "s"
    **
    *******************************************************************************/
   public static String plural(Collection<?> collection)
   {
      return (plural(collection == null ? 0 : collection.size()));
   }



   /*******************************************************************************
    ** Switch between strings based on if the 'size' parameter is 1 (the singular)
    ** or not-1 (0 or 2+, the plural).  Get back "" or "s"
    **
    *******************************************************************************/
   public static String plural(Integer size)
   {
      return (plural(size, "", "s"));
   }



   /*******************************************************************************
    ** Switch between strings based on if the size of the parameter collection.  If
    ** it is 1 (the singular) or not-1 (0 or 2+, the plural).  Specify/customize the
    ** values that you get back (e.g., "y", "ies")
    **
    *******************************************************************************/
   public static String plural(Collection<?> collection, String ifOne, String ifNotOne)
   {
      return (plural(collection.size(), ifOne, ifNotOne));
   }



   /*******************************************************************************
    ** Switch between strings based on if the 'size' parameter is 1 (the singular)
    ** or not-1 (0 or 2+, the plural).  Specify/customize the values that you get back
    ** (e.g., "y", "ies")
    **
    *******************************************************************************/
   public static String plural(Integer size, String ifOne, String ifNotOne)
   {
      return (size != null && size.equals(1) ? ifOne : ifNotOne);
   }



   /*******************************************************************************
    ** Lowercase the first char of a string.
    *******************************************************************************/
   public static String lcFirst(String s)
   {
      if(s == null)
      {
         return (null);
      }

      if(s.length() <= 1)
      {
         return (s.toLowerCase());
      }

      return (s.substring(0, 1).toLowerCase() + s.substring(1));
   }



   /*******************************************************************************
    ** Uppercase the first char of a string.
    *******************************************************************************/
   public static String ucFirst(String s)
   {
      if(s == null)
      {
         return (null);
      }

      if(s.length() <= 1)
      {
         return (s.toUpperCase());
      }

      return (s.substring(0, 1).toUpperCase() + s.substring(1));
   }



   /*******************************************************************************
    ** determines if a given string is a UUID
    *******************************************************************************/
   public static boolean isUUID(String s)
   {
      return (Pattern.matches("[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}", s));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static String replaceNonAsciiCharacters(String s)
   {
      Transliterator transliterator = Transliterator.getInstance("Any-Latin; Latin-ASCII");
      return (transliterator.transliterate(s));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static String emptyToNull(String s)
   {
      if(!hasContent(s))
      {
         return (null);
      }

      return (s);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static boolean safeEqualsIgnoreCase(String a, String b)
   {
      if(a == null && b == null)
      {
         return true;
      }
      if(a == null || b == null)
      {
         return false;
      }
      return (a.equalsIgnoreCase(b));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static String appendIncrementingSuffix(String input)
   {
      ////////////////////////////////
      // remove any existing suffix //
      ////////////////////////////////
      String base = input.replaceAll(" \\(\\d+\\)$", "");
      if(input.matches(".* \\(\\d+\\)$"))
      {
         //////////////////////////
         // increment if matches //
         //////////////////////////
         int current = Integer.parseInt(input.replaceAll(".* \\((\\d+)\\)$", "$1"));
         return base + " (" + (current + 1) + ")";
      }
      else
      {
         ////////////////////////////////////
         // no match so put a 1 at the end //
         ////////////////////////////////////
         return base + " (1)";
      }
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   public static String maskAndTruncate(String value)
   {
      return (maskAndTruncate(value, "** MASKED **", 6, 4));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String maskAndTruncate(String value, String mask, int minLengthToMask, int charsToShowOnEnds)
   {
      if(!hasContent(value))
      {
         return ("");
      }

      if(value.length() < minLengthToMask || value.length() < 2 * charsToShowOnEnds)
      {
         return mask;
      }

      if(value.length() < charsToShowOnEnds * 3)
      {
         return (value.substring(0, charsToShowOnEnds) + mask);
      }

      return (value.substring(0, charsToShowOnEnds) + mask + value.substring(value.length() - charsToShowOnEnds));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static String nCopies(int n, String s)
   {
      return (nCopiesWithGlue(n, s, ""));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static String nCopiesWithGlue(int n, String s, String glue)
   {
      return (StringUtils.join(glue, Collections.nCopies(n, s)));
   }



   /***************************************************************************
    * For a given string, count how many times (if any) the given substring
    * appears (non-overlapping).
    *
    * @param str the "haystack" - the string being searched in.
    * @param substring the "needle" - the substring being looked for.
    * @return int number of non-overlapping occurrences - 0 if not found.
    * also 0 if either string is null or the substring is empty.
    ***************************************************************************/
   public static int countSubstringInstances(String str, String substring)
   {
      if(str == null || substring == null || substring.isEmpty())
      {
         return 0;
      }

      int count = 0;
      int index = 0;

      while((index = str.indexOf(substring, index)) != -1)
      {
         count++;
         index += substring.length();
      }

      return count;
   }



}
