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


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for StringUtils
 **
 *******************************************************************************/
class StringUtilsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_hasContent()
   {
      assertFalse(StringUtils.hasContent(""));
      assertFalse(StringUtils.hasContent("   "));
      assertFalse(StringUtils.hasContent(" \n  "));
      assertFalse(StringUtils.hasContent(null));
      assertTrue(StringUtils.hasContent("a"));
      assertTrue(StringUtils.hasContent(" a "));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_nvlString()
   {
      assertEquals("foo", StringUtils.nvl("foo", "bar"));
      assertEquals("bar", StringUtils.nvl(null, "bar"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_nvlObject()
   {
      assertEquals("1701", StringUtils.nvl(1701, "bar"));
      assertEquals("bar", StringUtils.nvl((Integer) null, "bar"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_safeAppend()
   {
      assertEquals("Foo", StringUtils.safeAppend("Foo", null));
      assertEquals("Foo", StringUtils.safeAppend(null, "Foo"));
      assertEquals("FooBar", StringUtils.safeAppend("Foo", "Bar"));
      assertEquals("", StringUtils.safeAppend(null, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_allCapsToMixedCase()
   {
      assertNull(StringUtils.allCapsToMixedCase(null));
      assertEquals("Foo", StringUtils.allCapsToMixedCase("FOO"));
      assertEquals("Foo Bar", StringUtils.allCapsToMixedCase("FOO BAR"));
      assertEquals("Foo bar", StringUtils.allCapsToMixedCase("FOO bar"));
      assertEquals("Foo bar", StringUtils.allCapsToMixedCase("FOo bar"));
      assertEquals("Foo Bar", StringUtils.allCapsToMixedCase("FOo BAr"));
      assertEquals("foo bar", StringUtils.allCapsToMixedCase("foo bar"));
      assertEquals("Foo Bar", StringUtils.allCapsToMixedCase("FOO_BAR"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_safeTruncate2()
   {
      assertNull(StringUtils.safeTruncate(null, 5));
      assertEquals("123", StringUtils.safeTruncate("123", 5));
      assertEquals("12345", StringUtils.safeTruncate("1234567", 5));
      assertEquals("1234567", StringUtils.safeTruncate("1234567\uD834\uDD1EX", 8)); // Preserve the supplementary character at the UTF-16 boundary.
      assertEquals("1234567\uD834\uDD1E", StringUtils.safeTruncate("1234567\uD834\uDD1EX", 9)); // Preserve the supplementary character at the UTF-16 boundary.
      assertEquals("", StringUtils.safeTruncate("\uD834\uDD1E", 1)); // Preserve the supplementary character at the UTF-16 boundary.
      assertEquals("", StringUtils.safeTruncate("abc", 0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_safeTruncate3()
   {
      assertNull(StringUtils.safeTruncate(null, 5, "..."));
      assertEquals("123", StringUtils.safeTruncate("123", 5, "..."));
      assertEquals("12345", StringUtils.safeTruncate("12345", 5, "..."));
      assertEquals("12...", StringUtils.safeTruncate("123456", 5, "..."));
      assertEquals("12...", StringUtils.safeTruncate("1234567890", 5, "..."));
      assertEquals("\uD834\uDD1E\uD834\uDD1E...", StringUtils.safeTruncate("\uD834\uDD1E\uD834\uDD1E\uD834\uDD1E\uD834\uDD1E\uD834\uDD1E", 8, "...")); // Preserve the supplementary character at the UTF-16 boundary.
      assertEquals("", StringUtils.safeTruncate("abcd", 0, "..."));
      assertEquals(".", StringUtils.safeTruncate("abcd", 1, "..."));
      assertEquals("..", StringUtils.safeTruncate("abcd", 2, "..."));
      assertEquals("...", StringUtils.safeTruncate("abcd", 3, "..."));
      assertEquals("", StringUtils.safeTruncate("abcd", 1, "\uD834\uDD1E")); // Preserve the supplementary character at the UTF-16 boundary.
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_safeTrim()
   {
      assertNull(StringUtils.safeTrim(null));
      assertEquals("foo", StringUtils.safeTrim("foo "));
      assertEquals("foo", StringUtils.safeTrim(" foo "));
      assertEquals("foo", StringUtils.safeTrim(" foo"));
      assertEquals("foo", StringUtils.safeTrim("    foo   \n   "));
      assertEquals("foo", StringUtils.safeTrim("\nfoo  \r  \n   "));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_join()
   {
      assertNull(StringUtils.join(",", null));
      assertEquals("1", StringUtils.join(",", List.of(1)));
      assertEquals("", StringUtils.join(",", List.of()));
      assertEquals("1,2,3", StringUtils.join(",", List.of(1, 2, 3)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_joinWithCommasAndAnd()
   {
      assertNull(StringUtils.joinWithCommasAndAnd(null));
      assertEquals("", StringUtils.joinWithCommasAndAnd(List.of()));
      assertEquals("A", StringUtils.joinWithCommasAndAnd(List.of("A")));
      assertEquals("A and B", StringUtils.joinWithCommasAndAnd(List.of("A", "B")));
      assertEquals("A, B, and C", StringUtils.joinWithCommasAndAnd(List.of("A", "B", "C")));
      assertEquals("A, B, C, and D", StringUtils.joinWithCommasAndAnd(List.of("A", "B", "C", "D")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_ltrim()
   {
      assertEquals("", StringUtils.ltrim(null));
      assertEquals("123", StringUtils.ltrim("123"));
      assertEquals("123", StringUtils.ltrim(" 123"));
      assertEquals("123", StringUtils.ltrim("    123"));
      assertEquals("123", StringUtils.ltrim("  \n\n  123"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_rtrim()
   {
      assertEquals("", StringUtils.rtrim(null));
      assertEquals("123", StringUtils.rtrim("123"));
      assertEquals("123", StringUtils.rtrim("123 "));
      assertEquals("123", StringUtils.rtrim("123    "));
      assertEquals("123", StringUtils.rtrim("123  \n\n  "));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_pluralCollection()
   {
      assertEquals("s", StringUtils.plural(List.of()));
      assertEquals("", StringUtils.plural(List.of(1)));
      assertEquals("s", StringUtils.plural(List.of(1, 2)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_pluralInteger()
   {
      assertEquals("s", StringUtils.plural(0));
      assertEquals("", StringUtils.plural(1));
      assertEquals("s", StringUtils.plural(2));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_pluralCollectionStringString()
   {
      assertEquals("es", StringUtils.plural(List.of(), "", "es"));
      assertEquals("", StringUtils.plural(List.of(1), "", "es"));
      assertEquals("es", StringUtils.plural(List.of(1, 2), "", "es"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_pluralIntegerStringString()
   {
      assertEquals("es", StringUtils.plural(0, "", "es"));
      assertEquals("", StringUtils.plural(1, "", "es"));
      assertEquals("es", StringUtils.plural(2, "", "es"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLcFirst()
   {
      assertNull(StringUtils.lcFirst(null));
      assertEquals("", StringUtils.lcFirst(""));
      assertEquals(" ", StringUtils.lcFirst(" "));
      assertEquals("a", StringUtils.lcFirst("A"));
      assertEquals("1", StringUtils.lcFirst("1"));
      assertEquals("a", StringUtils.lcFirst("a"));
      assertEquals("aB", StringUtils.lcFirst("AB"));
      assertEquals("aBc", StringUtils.lcFirst("ABc"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUcFirst()
   {
      assertNull(StringUtils.ucFirst(null));
      assertEquals("", StringUtils.ucFirst(""));
      assertEquals(" ", StringUtils.ucFirst(" "));
      assertEquals("A", StringUtils.ucFirst("A"));
      assertEquals("1", StringUtils.ucFirst("1"));
      assertEquals("A", StringUtils.ucFirst("a"));
      assertEquals("Ab", StringUtils.ucFirst("ab"));
      assertEquals("Abc", StringUtils.ucFirst("abc"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPluralFormat()
   {
      assertEquals("Apple", StringUtils.pluralFormat(1, "Apple{,s}"));
      assertEquals("Apples", StringUtils.pluralFormat(0, "Apple{,s}"));
      assertEquals("Apples", StringUtils.pluralFormat(2, "Apple{,s}"));

      assertEquals("Apple and Orange", StringUtils.pluralFormat(1, "Apple{,s} and Orange{,s}"));
      assertEquals("Apples and Oranges", StringUtils.pluralFormat(2, "Apple{,s} and Orange{,s}"));

      assertEquals("Apple was eaten", StringUtils.pluralFormat(1, "Apple{,s} {was,were} eaten"));
      assertEquals("Apples were eaten", StringUtils.pluralFormat(2, "Apple{,s} {was,were} eaten"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testEmptyToNull()
   {
      assertNull(StringUtils.emptyToNull(null));
      assertNull(StringUtils.emptyToNull(""));
      assertNull(StringUtils.emptyToNull(" "));
      assertNull(StringUtils.emptyToNull("  "));
      assertEquals("a", StringUtils.emptyToNull("a"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppendIncrementingSuffix()
   {
      assertEquals("test (1)", StringUtils.appendIncrementingSuffix("test"));
      assertEquals("test (2)", StringUtils.appendIncrementingSuffix("test (1)"));
      assertEquals("test (a) (1)", StringUtils.appendIncrementingSuffix("test (a)"));
      assertEquals("test (a32) (1)", StringUtils.appendIncrementingSuffix("test (a32)"));
      assertEquals("test ((2)) (1)", StringUtils.appendIncrementingSuffix("test ((2))"));
      assertEquals("test ((2)) (101)", StringUtils.appendIncrementingSuffix("test ((2)) (100)"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSafeEqualsIgnoreCase()
   {
      assertTrue(StringUtils.safeEqualsIgnoreCase(null, null));
      assertFalse(StringUtils.safeEqualsIgnoreCase("a", null));
      assertFalse(StringUtils.safeEqualsIgnoreCase(null, "a"));
      assertTrue(StringUtils.safeEqualsIgnoreCase("a", "a"));
      assertTrue(StringUtils.safeEqualsIgnoreCase("A", "a"));
      assertFalse(StringUtils.safeEqualsIgnoreCase("a", "b"));
      assertTrue(StringUtils.safeEqualsIgnoreCase("timothy d. chamberlain", "TIMOThy d. chaMberlain"));
      assertTrue(StringUtils.safeEqualsIgnoreCase("timothy d. chamberlain", "timothy d. chamberlain"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNCopies()
   {
      assertEquals("", StringUtils.nCopies(0, "a"));
      assertEquals("a", StringUtils.nCopies(1, "a"));
      assertEquals("aa", StringUtils.nCopies(2, "a"));
      assertEquals("ab ab ab ", StringUtils.nCopies(3, "ab "));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNCopiesWithGlue()
   {
      assertEquals("", StringUtils.nCopiesWithGlue(0, "a", ""));
      assertEquals("", StringUtils.nCopiesWithGlue(0, "a", ","));
      assertEquals("a", StringUtils.nCopiesWithGlue(1, "a", ","));
      assertEquals("aa", StringUtils.nCopiesWithGlue(2, "a", ""));
      assertEquals("a,a", StringUtils.nCopiesWithGlue(2, "a", ","));
      assertEquals("ab ab ab", StringUtils.nCopiesWithGlue(3, "ab", " "));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMaskAndTruncate()
   {
      assertEquals("", StringUtils.maskAndTruncate(null));
      assertEquals("", StringUtils.maskAndTruncate(""));
      assertEquals("** MASKED **", StringUtils.maskAndTruncate("1"));
      assertEquals("** MASKED **", StringUtils.maskAndTruncate("12"));
      assertEquals("** MASKED **", StringUtils.maskAndTruncate("123"));
      assertEquals("** MASKED **", StringUtils.maskAndTruncate("1234"));
      assertEquals("** MASKED **", StringUtils.maskAndTruncate("12345"));
      assertEquals("** MASKED **", StringUtils.maskAndTruncate("123456"));
      assertEquals("** MASKED **", StringUtils.maskAndTruncate("1234567"));
      assertEquals("1234** MASKED **", StringUtils.maskAndTruncate("12345678"));
      assertEquals("1234** MASKED **", StringUtils.maskAndTruncate("123456789"));
      assertEquals("1234** MASKED **", StringUtils.maskAndTruncate("1234567890"));
      assertEquals("1234** MASKED **", StringUtils.maskAndTruncate("12345678901"));
      assertEquals("1234** MASKED **9012", StringUtils.maskAndTruncate("123456789012"));
      assertEquals("1234** MASKED **6789", StringUtils.maskAndTruncate("123456789" + StringUtils.nCopies(100, "xyz") + "123456789"));

      assertEquals("***", StringUtils.maskAndTruncate("12", "***", 3, 1));
      assertEquals("1***3", StringUtils.maskAndTruncate("123", "***", 3, 1));
      assertEquals("1***4", StringUtils.maskAndTruncate("1234", "***", 3, 1));
      assertEquals("1***5", StringUtils.maskAndTruncate("12345", "***", 3, 1));
      assertEquals("12***", StringUtils.maskAndTruncate("12345", "***", 3, 2));
      assertEquals("12***56", StringUtils.maskAndTruncate("123456", "***", 3, 2));

      assertEquals("***", StringUtils.maskAndTruncate("12", "***", 3, 4));
      assertEquals("***", StringUtils.maskAndTruncate("123", "***", 3, 4));
      assertEquals("***", StringUtils.maskAndTruncate("1234", "***", 3, 4));
      assertEquals("***", StringUtils.maskAndTruncate("12345", "***", 3, 4));
      assertEquals("***", StringUtils.maskAndTruncate("12345", "***", 3, 4));
      assertEquals("***", StringUtils.maskAndTruncate("123456", "***", 3, 4));
      assertEquals("1234***", StringUtils.maskAndTruncate("1234567890", "***", 3, 4));
      assertEquals("1234***", StringUtils.maskAndTruncate("12345678901", "***", 3, 4));
      assertEquals("1234***9012", StringUtils.maskAndTruncate("123456789012", "***", 3, 4));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCountSubstringInstances() throws Exception
   {
      assertEquals(0, StringUtils.countSubstringInstances(null, null));
      assertEquals(0, StringUtils.countSubstringInstances("haystack", null));
      assertEquals(0, StringUtils.countSubstringInstances(null, "needle"));
      assertEquals(0, StringUtils.countSubstringInstances("haystack", ""));
      assertEquals(0, StringUtils.countSubstringInstances("haystack", "needle"));
      assertEquals(1, StringUtils.countSubstringInstances("a", "a"));
      assertEquals(2, StringUtils.countSubstringInstances("aa", "a"));
      assertEquals(2, StringUtils.countSubstringInstances("aba", "a"));
      assertEquals(2, StringUtils.countSubstringInstances("abab", "a"));
      assertEquals(1, StringUtils.countSubstringInstances("ab", "ab"));
      assertEquals(1, StringUtils.countSubstringInstances("abc", "ab"));
      assertEquals(1, StringUtils.countSubstringInstances(" abc", "ab"));
      assertEquals(2, StringUtils.countSubstringInstances("abab", "ab"));
      assertEquals(2, StringUtils.countSubstringInstances("abcab", "ab"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIsUUID_validUUID_returnsTrue()
   {
      assertTrue(StringUtils.isUUID("550e8400-e29b-41d4-a716-446655440000"));
      assertTrue(StringUtils.isUUID("00000000-0000-0000-0000-000000000000"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIsUUID_invalidFormats_returnsFalse()
   {
      assertFalse(StringUtils.isUUID("not-a-uuid"));
      assertFalse(StringUtils.isUUID("550e8400-e29b-41d4-a716-44665544000"));    // too short
      assertFalse(StringUtils.isUUID("550e8400-e29b-41d4-a716-4466554400000"));  // too long
      assertFalse(StringUtils.isUUID("550e8400e29b41d4a716446655440000"));        // no dashes
      assertFalse(StringUtils.isUUID("GGGGGGGG-e29b-41d4-a716-446655440000"));   // non-hex chars
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReplaceNonAsciiCharacters_accentedChars_transliterated()
   {
      assertEquals("ae", StringUtils.replaceNonAsciiCharacters("æ"));
      // ICU4J Any-Latin;Latin-ASCII strips diacritics (ü→u) rather than using German spelling-out (ü→ue)
      assertEquals("Dusseldorf", StringUtils.replaceNonAsciiCharacters("Düsseldorf"));
      assertEquals("Resume", StringUtils.replaceNonAsciiCharacters("Résumé"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReplaceNonAsciiCharacters_pureAscii_unchanged()
   {
      assertEquals("hello world", StringUtils.replaceNonAsciiCharacters("hello world"));
      assertEquals("", StringUtils.replaceNonAsciiCharacters(""));
   }


}
