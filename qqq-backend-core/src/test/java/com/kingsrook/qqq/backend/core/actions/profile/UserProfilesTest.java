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

package com.kingsrook.qqq.backend.core.actions.profile;


import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.preferences.UserPreferencesAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsTestSupport;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRef;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRefs;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.profile.UserProfilesMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Core profile: avatar upload, conversion, safety, replacement and session
 ** values.
 *******************************************************************************/
class UserProfilesTest extends BaseTest
{
   private static final String ALICE = "alice@example.com";
   private static final String BOB   = "bob@example.com";

   private final UserPreferencesAction action = new UserPreferencesAction();



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setupProfiles() throws Exception
   {
      QInstance instance = DurableJobsTestSupport.defineInstance(DurableJobsTestSupport.fastSettings().withStorageTableName(TestUtils.TABLE_NAME_MEMORY_STORAGE));
      new UserProfileMetaDataProvider().withGravatarOptIn(true).defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, TestUtils.TABLE_NAME_MEMORY_STORAGE);
      new QInstanceValidator().validate(instance);
      QContext.init(instance, session(ALICE));
   }



   /*******************************************************************************
    ** Upload, then read back: the URL names the content key and every size is a
    ** square PNG.
    *******************************************************************************/
   @Test
   void testUploadPng() throws Exception
   {
      setAvatar(upload("me.png", png(300, 200, Color.RED)));

      String url = (String) avatarValue();
      QRecord row = UserProfiles.find(ALICE);
      String  key = row.getValueString("avatarKey");
      assertThat(key).hasSize(32);
      assertThat(url).isEqualTo("/qqq/v1/avatars/alice%40example.com?v=" + key);
      for(int size : UserProfilesMetaData.AVATAR_SIZES)
      {
         BufferedImage image = ImageIO.read(new ByteArrayInputStream(read(key, size)));
         assertThat(image.getWidth()).isEqualTo(size);
         assertThat(image.getHeight()).isEqualTo(size);
      }

      ///////////////////////////////////////////////////////////////
      // sending the current URL back changes nothing              //
      ///////////////////////////////////////////////////////////////
      setAvatar(url);
      assertThat(UserProfiles.find(ALICE).getValueString("avatarKey")).isEqualTo(key);
   }



   /*******************************************************************************
    ** A JPEG carrying EXIF (orientation and a GPS position) comes back upright
    ** and with no metadata at all.
    *******************************************************************************/
   @Test
   void testExifStrippedAndOrientationApplied() throws Exception
   {
      BufferedImage source   = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
      Graphics2D    graphics = source.createGraphics();
      graphics.setColor(Color.RED);
      graphics.fillRect(0, 0, 100, 50);
      graphics.setColor(Color.BLUE);
      graphics.fillRect(0, 50, 100, 50);
      graphics.dispose();
      byte[] jpeg = withExif(encode(source, "jpeg"), 6);
      assertThat(new String(jpeg, StandardCharsets.ISO_8859_1)).contains("Exif").contains("GPSLatitude");
      assertThat(AvatarImageProcessor.exifOrientation(jpeg)).isEqualTo(6);

      setAvatar(upload("phone.jpg", jpeg));
      byte[] stored = read(UserProfiles.find(ALICE).getValueString("avatarKey"), 256);
      String text   = new String(stored, StandardCharsets.ISO_8859_1);
      assertThat(text).doesNotContain("Exif").doesNotContain("GPS").doesNotContain("eXIf");

      //////////////////////////////////////////////////////////////////////
      // orientation 6 is "rotate 90 clockwise": the red top is now right //
      //////////////////////////////////////////////////////////////////////
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(stored));
      Color         left  = new Color(image.getRGB(30, 128));
      Color         right = new Color(image.getRGB(226, 128));
      assertThat(right.getRed()).isGreaterThan(200);
      assertThat(left.getBlue()).isGreaterThan(200);
   }



   /*******************************************************************************
    ** SVG, HTML and a fake PNG are refused by their bytes, whatever their name.
    *******************************************************************************/
   @Test
   void testRefusedTypes() throws Exception
   {
      String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"><rect width=\"10\" height=\"10\"/></svg>";
      assertRefused(upload("evil.png", svg.getBytes(StandardCharsets.UTF_8)), "Use a PNG, JPEG, WebP or GIF image.");
      assertRefused(upload("page.jpg", "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8)), "Use a PNG, JPEG, WebP or GIF image.");

      byte[] fakePng = new byte[64];
      System.arraycopy(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A }, 0, fakePng, 0, 8);
      assertRefused(upload("broken.png", fakePng), "couldn't be read");
      assertThat(UserProfiles.find(ALICE)).isNull();
   }



   /*******************************************************************************
    ** WebP uploads decode through the ImageIO plugin, and the type sniffing
    ** recognises each accepted format.
    *******************************************************************************/
   @Test
   void testFormatsDetected() throws Exception
   {
      assertThat(ImageIO.getImageReadersByFormatName("webp").hasNext()).isTrue();
      assertThat(AvatarImageProcessor.detectFormat(png(4, 4, Color.RED))).isEqualTo("png");
      assertThat(AvatarImageProcessor.detectFormat(encode(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "jpeg"))).isEqualTo("jpeg");
      assertThat(AvatarImageProcessor.detectFormat(encode(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "gif"))).isEqualTo("gif");
      assertThat(AvatarImageProcessor.detectFormat("RIFF0000WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1))).isEqualTo("webp");
      assertThat(AvatarImageProcessor.detectFormat("<?xml version=\"1.0\"?><svg/>".getBytes(StandardCharsets.UTF_8))).isNull();
   }



   /*******************************************************************************
    ** Too many bytes, or a header claiming a huge image, is refused before any
    ** decoding.
    *******************************************************************************/
   @Test
   void testLimits() throws Exception
   {
      UserProfilesMetaData.of(QContext.getQInstance()).setMaxAvatarBytes(1000);
      assertRefused(upload("big.png", png(400, 400, Color.GREEN)), "too large");
      UserProfilesMetaData.of(QContext.getQInstance()).setMaxAvatarBytes(UserProfilesMetaData.DEFAULT_MAX_BYTES);

      ////////////////////////////////////////////////////////////////
      // patch the IHDR of a real PNG to claim 30000 x 30000 pixels //
      ////////////////////////////////////////////////////////////////
      byte[] bomb = png(10, 10, Color.GREEN);
      writeInt(bomb, 16, 30000);
      writeInt(bomb, 20, 30000);
      assertThatThrownBy(() -> setAvatar(upload("bomb.png", bomb))).isInstanceOf(QBadRequestException.class).hasMessageContaining("Photo:");
   }



   /*******************************************************************************
    ** Another user's upload can't become my photo, even for an administrator.
    *******************************************************************************/
   @Test
   void testOtherUsersUploadRefused() throws Exception
   {
      QContext.setQSession(session(BOB));
      String bobsRef = upload("bob.png", png(50, 50, Color.BLUE));
      QContext.setQSession(session(ALICE).withPermission("jobs_admin"));
      assertRefused(bobsRef, "Upload not found");
   }



   /*******************************************************************************
    ** Replacing removes the old objects; removing clears the row and objects.
    *******************************************************************************/
   @Test
   void testReplaceAndRemove() throws Exception
   {
      setAvatar(upload("a.png", png(64, 64, Color.RED)));
      String first = UserProfiles.find(ALICE).getValueString("avatarKey");
      setAvatar(upload("b.png", png(64, 64, Color.BLUE)));
      String second = UserProfiles.find(ALICE).getValueString("avatarKey");
      assertThat(second).isNotEqualTo(first);
      assertThat(exists(first, 64)).isFalse();
      assertThat(exists(second, 64)).isTrue();

      setAvatar(null);
      assertThat(avatarValue()).isNull();
      assertThat(UserProfiles.find(ALICE).getValue("avatarKey")).isNull();
      assertThat(exists(second, 64)).isFalse();
   }



   /*******************************************************************************
    ** Session values: photo URL and display name; Gravatar only when chosen,
    ** and a photo wins over it.
    *******************************************************************************/
   @Test
   void testFrontendValuesAndGravatar() throws Exception
   {
      assertThat(UserProfiles.frontendUserValues(QContext.getQInstance(), QContext.getQSession())).isEmpty();

      action.patchSection(UserProfilesMetaData.SECTION_NAME, Map.of("values", Map.of("displayName", "  Alice A.  ", "useGravatar", true)));
      QRecord row = UserProfiles.find(ALICE);
      assertThat(row.getValueString("displayName")).isEqualTo("Alice A.");
      assertThat(row.getValueString("gravatarHash")).isEqualTo(UserProfiles.gravatarHash(ALICE)).hasSize(32);
      Map<String, Serializable> values = UserProfiles.frontendUserValues(QContext.getQInstance(), QContext.getQSession());
      assertThat(values).containsEntry("displayName", "Alice A.");
      assertThat((String) values.get("avatarUrl")).endsWith("?v=g" + row.getValueString("gravatarHash"));

      setAvatar(upload("me.png", png(64, 64, Color.RED)));
      values = UserProfiles.frontendUserValues(QContext.getQInstance(), QContext.getQSession());
      assertThat((String) values.get("avatarUrl")).endsWith("?v=" + UserProfiles.find(ALICE).getValueString("avatarKey"));

      action.patchSection(UserProfilesMetaData.SECTION_NAME, Map.of("values", Map.of("displayName", " ")));
      assertThat(UserProfiles.find(ALICE).getValue("displayName")).isNull();

      ///////////////////////////////////////////////////////////////////
      // bob's profile is his own: alice's writes never touched it     //
      ///////////////////////////////////////////////////////////////////
      QContext.setQSession(session(BOB));
      assertThat(avatarValue()).isNull();
      assertThat(UserProfiles.find(BOB)).isNull();
   }



   /*******************************************************************************
    ** Gravatar can't be chosen when the app doesn't allow it.
    *******************************************************************************/
   @Test
   void testNoGravatarWithoutOptIn() throws Exception
   {
      QInstance instance = DurableJobsTestSupport.defineInstance(DurableJobsTestSupport.fastSettings().withStorageTableName(TestUtils.TABLE_NAME_MEMORY_STORAGE));
      new UserProfileMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, TestUtils.TABLE_NAME_MEMORY_STORAGE);
      QContext.init(instance, session(ALICE));
      assertThat(action.getSection(UserProfilesMetaData.SECTION_NAME).getValues().getValues()).doesNotContainKey("useGravatar");
      assertThatThrownBy(() -> action.patchSection(UserProfilesMetaData.SECTION_NAME, Map.of("values", Map.of("useGravatar", true)))).isInstanceOf(QBadRequestException.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertRefused(String ref, String message)
   {
      assertThatThrownBy(() -> setAvatar(ref)).isInstanceOf(QBadRequestException.class).hasMessageContaining("Photo:").hasMessageContaining(message);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setAvatar(String value) throws QException
   {
      Map<String, Object> values = new HashMap<>();
      values.put("avatar", value);
      action.patchSection(UserProfilesMetaData.SECTION_NAME, Map.of("values", values));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Serializable avatarValue() throws QException
   {
      return (action.getSection(UserProfilesMetaData.SECTION_NAME).getValues().getValues().get("avatar"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String upload(String name, byte[] bytes) throws QException
   {
      UploadRef ref = UploadRefs.store(name, "image/png", new ByteArrayInputStream(bytes));
      return (ref.ref());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private byte[] read(String key, int size) throws Exception
   {
      try(InputStream input = new StorageAction().getInputStream(UserProfiles.avatarObject(key, size)))
      {
         return (input.readAllBytes());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean exists(String key, int size)
   {
      try
      {
         read(key, size);
         return (true);
      }
      catch(Exception e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QSession session(String userId)
   {
      return (new QSession().withUser(new QUser().withIdReference(userId).withFullName("Test " + userId)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static byte[] png(int width, int height, Color color) throws Exception
   {
      BufferedImage image    = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      Graphics2D    graphics = image.createGraphics();
      graphics.setColor(color);
      graphics.fillRect(0, 0, width, height);
      for(int i = 0; i < width; i += 7)
      {
         graphics.setColor(new Color((i * 37) % 255, (i * 91) % 255, (i * 13) % 255));
         graphics.drawLine(i, 0, width - i, height);
      }
      graphics.dispose();
      return (encode(image, "png"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static byte[] encode(BufferedImage image, String format) throws Exception
   {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(image, format, out);
      return (out.toByteArray());
   }



   /*******************************************************************************
    ** Splice an APP1 Exif segment (orientation, GPS IFD pointer and a GPS tag
    ** name as text) in after the JPEG's SOI marker.
    *******************************************************************************/
   private static byte[] withExif(byte[] jpeg, int orientation)
   {
      ByteArrayOutputStream tiff = new ByteArrayOutputStream();
      tiff.writeBytes(new byte[] { 'M', 'M', 0, 42, 0, 0, 0, 8 });
      tiff.writeBytes(new byte[] { 0, 2 });
      tiff.writeBytes(new byte[] { 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0 });
      tiff.writeBytes(new byte[] { (byte) 0x88, 0x25, 0, 4, 0, 0, 0, 1, 0, 0, 0, 0 });
      tiff.writeBytes(new byte[] { 0, 0, 0, 0 });
      tiff.writeBytes("GPSLatitude=41.8781N;GPSLongitude=87.6298W".getBytes(StandardCharsets.US_ASCII));

      byte[]                payload = tiff.toByteArray();
      int                   length  = 2 + 6 + payload.length;
      ByteArrayOutputStream out     = new ByteArrayOutputStream();
      out.write(0xFF);
      out.write(0xD8);
      out.write(0xFF);
      out.write(0xE1);
      out.write(length >> 8);
      out.write(length & 0xFF);
      out.writeBytes(new byte[] { 'E', 'x', 'i', 'f', 0, 0 });
      out.writeBytes(payload);
      out.write(jpeg, 2, jpeg.length - 2);
      return (out.toByteArray());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void writeInt(byte[] bytes, int at, int value)
   {
      bytes[at]     = (byte) (value >> 24);
      bytes[at + 1] = (byte) (value >> 16);
      bytes[at + 2] = (byte) (value >> 8);
      bytes[at + 3] = (byte) value;
   }
}
