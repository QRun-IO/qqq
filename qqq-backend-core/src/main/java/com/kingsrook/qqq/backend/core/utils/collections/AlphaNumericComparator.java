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

package com.kingsrook.qqq.backend.core.utils.collections;


import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*******************************************************************************
 ** Comparator for strings that are a mix of alpha + numeric, where we want to
 ** sort the numeric substrings like numbers.
 **
 ** e.g., A1, A2, A10 won't come out as A1, A10, A2
 *******************************************************************************/
public class AlphaNumericComparator implements Comparator<String>
{
   private static final int A_FIRST = -1;
   private static final int B_FIRST = 1;
   private static final int TIE     = 0;

   private static final Pattern INT_PATTERN            = Pattern.compile("^\\d+$");
   private static final Pattern LEADING_INT_PATTERN    = Pattern.compile("^\\d+");
   private static final Pattern ALPHA_THEN_INT_PATTERN = Pattern.compile("^(\\D+)\\d+");



   /*******************************************************************************
    ** compare 2 Strings
    **
    *******************************************************************************/
   public int compare(String a, String b)
   {
      try
      {
         //////////////////////////////////////
         // eliminate degenerate cases first //
         //////////////////////////////////////
         if(a == null && b == null)
         {
            return (TIE);
         }
         else if(a == null)
         {
            return (A_FIRST);
         }
         else if(b == null)
         {
            return (B_FIRST);
         }
         else if(a.equals(b))
         {
            return (TIE);
         } // also covers a == "" and b == ""
         else if(a.equals(""))
         {
            return (A_FIRST);
         }
         else if(b.equals(""))
         {
            return (B_FIRST);
         }

         ////////////////////////////////////////////////////////////////
         // if both strings are pure numeric, parse as int and compare //
         ////////////////////////////////////////////////////////////////
         if(INT_PATTERN.matcher(a).matches() && INT_PATTERN.matcher(b).matches())
         {
            int intsCompared = Integer.valueOf(a).compareTo(Integer.valueOf(b));
            if(intsCompared == TIE)
            {
               ///////////////////////////////////////////////////////////////////////////////
               // in case the integers are the same (ie, "0001" vs "1"), compare as strings //
               ///////////////////////////////////////////////////////////////////////////////
               return (a.compareTo(b));
            }
            else
            {
               ///////////////////////////////////////////////////////////////
               // else, if the ints were different, return their comparison //
               ///////////////////////////////////////////////////////////////
               return (intsCompared);
            }
         }

         /////////////////////////////////////////////////////
         // if both start as numbers, extract those numbers //
         /////////////////////////////////////////////////////
         Matcher aLeadingIntMatcher = LEADING_INT_PATTERN.matcher(a);
         Matcher bLeadingIntMatcher = LEADING_INT_PATTERN.matcher(b);
         if(aLeadingIntMatcher.lookingAt() && bLeadingIntMatcher.lookingAt())
         {
            ///////////////////////////
            // extract the int parts //
            ///////////////////////////
            String aIntPart = a.substring(0, aLeadingIntMatcher.end());
            String bIntPart = b.substring(0, bLeadingIntMatcher.end());

            /////////////////////////////////////////////////////////////
            // if the ints compare as non-zero, return that comparison //
            /////////////////////////////////////////////////////////////
            int intPartCompared = Integer.valueOf(aIntPart).compareTo(Integer.valueOf(bIntPart));
            if(intPartCompared != TIE)
            {
               return (intPartCompared);
            }
            else
            {
               //////////////////////////////////////////////////////////////////////
               // otherwise, make recursive call to compare the rest of the string //
               //////////////////////////////////////////////////////////////////////
               String aRest = a.substring(aLeadingIntMatcher.end());
               String bRest = b.substring(bLeadingIntMatcher.end());
               return (compare(aRest, bRest));
            }
         }
         //////////////////////////////////////////////////////
         // if one starts as numeric, but other doesn't      //
         // return the one that starts with the number first //
         //////////////////////////////////////////////////////
         else if(aLeadingIntMatcher.lookingAt())
         {
            return (A_FIRST);
         }
         else if(bLeadingIntMatcher.lookingAt())
         {
            return (B_FIRST);
         }

         //////////////////////////////////////////////////////////////////////////
         // now, if both parts have an alpha part, followed by digit parts, and  //
         // the alpha parts are the same, then discard the alpha parts and recur //
         //////////////////////////////////////////////////////////////////////////
         Matcher aAlphaThenIntMatcher = ALPHA_THEN_INT_PATTERN.matcher(a);
         Matcher bAlphaThenIntMatcher = ALPHA_THEN_INT_PATTERN.matcher(b);
         if(aAlphaThenIntMatcher.lookingAt() && bAlphaThenIntMatcher.lookingAt())
         {
            String aAlphaPart = aAlphaThenIntMatcher.group(1);
            String bAlphaPart = bAlphaThenIntMatcher.group(1);

            if(aAlphaPart.equals(bAlphaPart))
            {
               String aRest = a.substring(aAlphaPart.length());
               String bRest = b.substring(bAlphaPart.length());
               return (compare(aRest, bRest));
            }
         }

         /////////////////////////////////////////////////
         // as last resort, just do pure string compare //
         /////////////////////////////////////////////////
         return (a.compareTo(b));
      }
      catch(Exception e)
      {
         //////////////////////////////////////////////////////////
         // on exception, don't allow caller to catch -- rather, //
         // always return something sensible (and null-safe)     //
         //////////////////////////////////////////////////////////
         if(a == null && b == null)
         {
            return (TIE);
         }
         else if(a == null)
         {
            return (A_FIRST);
         }
         else if(b == null)
         {
            return (B_FIRST);
         }
         else
         {
            return (a.compareTo(b));
         }
      }
   }

}
