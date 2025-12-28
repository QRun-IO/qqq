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

package com.kingsrook.qqq.middleware.javalin.routeproviders;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Comprehensive unit tests for StaticAssetDetector
 *******************************************************************************/
class StaticAssetDetectorTest
{
   private StaticAssetDetector detector;


   /*******************************************************************************
    ** Set up fresh detector for each test
    *******************************************************************************/
   @BeforeEach
   void setUp()
   {
      detector = new StaticAssetDetector();
   }


   /*******************************************************************************
    ** Test JavaScript and module extensions
    *******************************************************************************/
   @Test
   void testJavaScriptExtensions()
   {
      assertTrue(detector.isStaticAsset("/assets/main.js"));
      assertTrue(detector.isStaticAsset("/assets/module.mjs"));
      assertTrue(detector.isStaticAsset("/assets/legacy.cjs"));
      assertTrue(detector.isStaticAsset("/components/Button.jsx"));
   }


   /*******************************************************************************
    ** Test TypeScript extensions
    *******************************************************************************/
   @Test
   void testTypeScriptExtensions()
   {
      assertTrue(detector.isStaticAsset("/src/main.ts"));
      assertTrue(detector.isStaticAsset("/components/App.tsx"));
   }


   /*******************************************************************************
    ** Test CSS and stylesheet extensions
    *******************************************************************************/
   @Test
   void testCSSExtensions()
   {
      assertTrue(detector.isStaticAsset("/assets/main.css"));
      assertTrue(detector.isStaticAsset("/assets/main.scss"));
      assertTrue(detector.isStaticAsset("/assets/main.sass"));
      assertTrue(detector.isStaticAsset("/assets/main.less"));
   }


   /*******************************************************************************
    ** Test WebAssembly and source maps
    *******************************************************************************/
   @Test
   void testModernWebFormats()
   {
      assertTrue(detector.isStaticAsset("/assets/module.wasm"));
      assertTrue(detector.isStaticAsset("/assets/main.js.map"));
      assertTrue(detector.isStaticAsset("/assets/main.css.map"));
   }


   /*******************************************************************************
    ** Test common image formats
    *******************************************************************************/
   @Test
   void testImageFormats()
   {
      assertTrue(detector.isStaticAsset("/images/logo.png"));
      assertTrue(detector.isStaticAsset("/images/photo.jpg"));
      assertTrue(detector.isStaticAsset("/images/photo.jpeg"));
      assertTrue(detector.isStaticAsset("/images/animation.gif"));
      assertTrue(detector.isStaticAsset("/images/logo.svg"));
      assertTrue(detector.isStaticAsset("/favicon.ico"));
   }


   /*******************************************************************************
    ** Test modern image formats
    *******************************************************************************/
   @Test
   void testModernImageFormats()
   {
      assertTrue(detector.isStaticAsset("/images/photo.webp"));
      assertTrue(detector.isStaticAsset("/images/photo.avif"));
      assertTrue(detector.isStaticAsset("/images/legacy.bmp"));
      assertTrue(detector.isStaticAsset("/images/scan.tiff"));
   }


   /*******************************************************************************
    ** Test font formats
    *******************************************************************************/
   @Test
   void testFontFormats()
   {
      assertTrue(detector.isStaticAsset("/fonts/arial.woff"));
      assertTrue(detector.isStaticAsset("/fonts/arial.woff2"));
      assertTrue(detector.isStaticAsset("/fonts/roboto.ttf"));
      assertTrue(detector.isStaticAsset("/fonts/legacy.eot"));
      assertTrue(detector.isStaticAsset("/fonts/custom.otf"));
   }


   /*******************************************************************************
    ** Test data file formats
    *******************************************************************************/
   @Test
   void testDataFileFormats()
   {
      assertTrue(detector.isStaticAsset("/data/config.json"));
      assertTrue(detector.isStaticAsset("/data/sitemap.xml"));
      assertTrue(detector.isStaticAsset("/data/export.csv"));
      assertTrue(detector.isStaticAsset("/robots.txt"));
   }


   /*******************************************************************************
    ** Test video formats
    *******************************************************************************/
   @Test
   void testVideoFormats()
   {
      assertTrue(detector.isStaticAsset("/videos/tutorial.mp4"));
      assertTrue(detector.isStaticAsset("/videos/demo.webm"));
      assertTrue(detector.isStaticAsset("/videos/clip.ogg"));
      assertTrue(detector.isStaticAsset("/videos/legacy.avi"));
      assertTrue(detector.isStaticAsset("/videos/recording.mov"));
   }


   /*******************************************************************************
    ** Test audio formats
    *******************************************************************************/
   @Test
   void testAudioFormats()
   {
      assertTrue(detector.isStaticAsset("/audio/music.mp3"));
      assertTrue(detector.isStaticAsset("/audio/sound.wav"));
      assertTrue(detector.isStaticAsset("/audio/track.ogg"));
      assertTrue(detector.isStaticAsset("/audio/podcast.m4a"));
      assertTrue(detector.isStaticAsset("/audio/lossless.flac"));
   }


   /*******************************************************************************
    ** Test common asset directory patterns
    *******************************************************************************/
   @Test
   void testAssetDirectories()
   {
      assertTrue(detector.isStaticAsset("/assets/bundle.js"));
      assertTrue(detector.isStaticAsset("/static/main.js"));
      assertTrue(detector.isStaticAsset("/public/favicon.ico"));
      assertTrue(detector.isStaticAsset("/dist/bundle.js"));
      assertTrue(detector.isStaticAsset("/build/main.js"));
      assertTrue(detector.isStaticAsset("/_next/static/chunks/main.js"));
      assertTrue(detector.isStaticAsset("/fonts/arial.woff2"));
      assertTrue(detector.isStaticAsset("/images/logo.png"));
      assertTrue(detector.isStaticAsset("/media/video.mp4"));
   }


   /*******************************************************************************
    ** Test that SPA routes are NOT detected as assets
    *******************************************************************************/
   @Test
   void testSPARoutes()
   {
      assertFalse(detector.isStaticAsset("/"));
      assertFalse(detector.isStaticAsset("/admin"));
      assertFalse(detector.isStaticAsset("/users/123"));
      assertFalse(detector.isStaticAsset("/dashboard/settings"));
      assertFalse(detector.isStaticAsset("/products/abc-def-ghi"));
      assertFalse(detector.isStaticAsset("/api/users"));
      assertFalse(detector.isStaticAsset("/admin/users/123/edit"));
   }


   /*******************************************************************************
    ** Test query parameters are handled correctly
    *******************************************************************************/
   @Test
   void testQueryParameters()
   {
      assertTrue(detector.isStaticAsset("/assets/main.js?v=123"));
      assertTrue(detector.isStaticAsset("/images/logo.png?size=large&quality=high"));
   }


   /*******************************************************************************
    ** Test hash fragments are handled correctly
    *******************************************************************************/
   @Test
   void testHashFragments()
   {
      assertTrue(detector.isStaticAsset("/assets/main.js#section"));
      assertTrue(detector.isStaticAsset("/styles/main.css#top"));
      assertTrue(detector.isStaticAsset("/assets/main.js?v=123#section"));
   }


   /*******************************************************************************
    ** Test case insensitivity
    *******************************************************************************/
   @Test
   void testCaseInsensitivity()
   {
      assertTrue(detector.isStaticAsset("/images/LOGO.PNG"));
      assertTrue(detector.isStaticAsset("/assets/MAIN.JS"));
      assertTrue(detector.isStaticAsset("/styles/Theme.Css"));
   }


   /*******************************************************************************
    ** Test double extensions and minified files
    *******************************************************************************/
   @Test
   void testDoubleExtensionsAndMinified()
   {
      assertTrue(detector.isStaticAsset("/assets/bundle.min.js"));
      assertTrue(detector.isStaticAsset("/assets/main.prod.css"));
      assertTrue(detector.isStaticAsset("/vendor/jquery.min.js"));
   }


   /*******************************************************************************
    ** Test nested paths
    *******************************************************************************/
   @Test
   void testNestedPaths()
   {
      assertTrue(detector.isStaticAsset("/admin/portal/assets/js/main.js"));
      assertTrue(detector.isStaticAsset("/app/customer/static/css/theme.css"));
   }


   /*******************************************************************************
    ** Test extensionless files in asset directories
    *******************************************************************************/
   @Test
   void testExtensionlessInAssetDirectory()
   {
      assertTrue(detector.isStaticAsset("/assets/logo"));
      assertTrue(detector.isStaticAsset("/static/favicon"));
   }


   /*******************************************************************************
    ** Test false positive prevention for routes containing asset-like substrings
    *******************************************************************************/
   @Test
   void testFalsePositivePrevention()
   {
      assertFalse(detector.isStaticAsset("/results/json/data"));
      assertFalse(detector.isStaticAsset("/projects/javascript-tutorial"));
      assertFalse(detector.isStaticAsset("/success/item"));
      assertFalse(detector.isStaticAsset("/access/granted"));
   }


   /*******************************************************************************
    ** Test custom extensions
    *******************************************************************************/
   @Test
   void testCustomExtensions()
   {
      detector.withCustomExtensions(".custom", ".myext");

      assertTrue(detector.isStaticAsset("/assets/file.custom"));
      assertTrue(detector.isStaticAsset("/data/config.myext"));
   }


   /*******************************************************************************
    ** Test custom path patterns
    *******************************************************************************/
   @Test
   void testCustomPathPatterns()
   {
      detector.withCustomPathPatterns("/my-assets/", "/resources/");

      assertTrue(detector.isStaticAsset("/my-assets/bundle.js"));
      assertTrue(detector.isStaticAsset("/admin/resources/logo.png"));
   }


   /*******************************************************************************
    ** Test custom detector logic
    *******************************************************************************/
   @Test
   void testCustomDetector()
   {
      detector.withCustomDetector(path -> path.startsWith("/cdn/"));

      assertTrue(detector.isStaticAsset("/cdn/library.js"));
      assertTrue(detector.isStaticAsset("/cdn/styles.css"));
   }


   /*******************************************************************************
    ** Test null and empty inputs
    *******************************************************************************/
   @Test
   void testNullAndEmptyInputs()
   {
      assertFalse(detector.isStaticAsset(null));
      assertFalse(detector.isStaticAsset(""));
   }


   /*******************************************************************************
    ** Test real-world Vite output
    *******************************************************************************/
   @Test
   void testViteOutput()
   {
      assertTrue(detector.isStaticAsset("/assets/index-abc123.js"));
      assertTrue(detector.isStaticAsset("/assets/index-def456.css"));
      assertTrue(detector.isStaticAsset("/assets/vendor-ghi789.js"));
   }


   /*******************************************************************************
    ** Test real-world Create React App output
    *******************************************************************************/
   @Test
   void testCreateReactAppOutput()
   {
      assertTrue(detector.isStaticAsset("/static/js/main.abc123.js"));
      assertTrue(detector.isStaticAsset("/static/css/main.def456.css"));
      assertTrue(detector.isStaticAsset("/static/media/logo.ghi789.png"));
   }


   /*******************************************************************************
    ** Test real-world Next.js output
    *******************************************************************************/
   @Test
   void testNextJSOutput()
   {
      assertTrue(detector.isStaticAsset("/_next/static/chunks/webpack-abc123.js"));
      assertTrue(detector.isStaticAsset("/_next/static/css/def456.css"));
      assertTrue(detector.isStaticAsset("/_next/data/abc123/users/1.json"));
   }


   /*******************************************************************************
    ** Test real-world Angular output
    *******************************************************************************/
   @Test
   void testAngularOutput()
   {
      assertTrue(detector.isStaticAsset("/runtime.abc123.js"));
      assertTrue(detector.isStaticAsset("/polyfills.def456.js"));
      assertTrue(detector.isStaticAsset("/main.ghi789.js"));
      assertTrue(detector.isStaticAsset("/styles.jkl012.css"));
   }


   /*******************************************************************************
    ** Test webpack chunks
    *******************************************************************************/
   @Test
   void testWebpackChunks()
   {
      assertTrue(detector.isStaticAsset("/assets/0.abc123.js"));
      assertTrue(detector.isStaticAsset("/assets/1.def456.js"));
      assertTrue(detector.isStaticAsset("/assets/chunk-vendors.ghi789.js"));
   }


   /*******************************************************************************
    ** Test configuration getters
    *******************************************************************************/
   @Test
   void testGetters()
   {
      assertTrue(detector.getExtensions().contains(".js"));
      assertTrue(detector.getExtensions().contains(".css"));
      assertTrue(detector.getExtensions().contains(".wasm"));

      assertTrue(detector.getPathPatterns().contains("/assets/"));
      assertTrue(detector.getPathPatterns().contains("/static/"));
      assertTrue(detector.getPathPatterns().contains("/_next/"));
   }


   /*******************************************************************************
    ** Test getters after custom configuration
    *******************************************************************************/
   @Test
   void testGettersAfterCustom()
   {
      detector.withCustomExtensions(".custom");
      detector.withCustomPathPatterns("/my-assets/");

      assertTrue(detector.getExtensions().contains(".custom"));
      assertTrue(detector.getExtensions().contains(".js")); // Still has defaults

      assertTrue(detector.getPathPatterns().contains("/my-assets/"));
      assertTrue(detector.getPathPatterns().contains("/assets/")); // Still has defaults
   }
}
