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

package com.kingsrook.qqq.backend.core.utils.http;


import java.net.InetAddress;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for AddressRules.
 *******************************************************************************/
class AddressRulesTest extends BaseTest
{

   /*******************************************************************************
    ** Every blocked range, including IPv4 embedded in IPv6.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {
      "0.0.0.0", "0.1.2.3", "10.0.0.1", "10.255.255.255", "100.64.0.1", "100.127.255.254", "127.0.0.1", "127.1.2.3",
      "169.254.0.1", "169.254.169.254", "172.16.0.1", "172.31.255.255", "192.0.0.8", "192.0.2.10", "192.88.99.1", "192.168.1.1",
      "198.18.0.1", "198.19.255.255", "198.51.100.7", "203.0.113.9", "224.0.0.1", "239.255.255.250", "240.0.0.1", "255.255.255.255",
      "::", "::1", "100::1", "2001:db8::1", "fc00::1", "fd12:3456::1", "fe80::1", "febf::1", "fec0::1", "ff02::1",
      "::ffff:127.0.0.1", "::ffff:10.0.0.1", "::ffff:169.254.169.254", "::10.0.0.1", "64:ff9b::a00:1", "64:ff9b::7f00:1",
      "2002:a00:1::", "2002:7f00:1::1"
   })
   void testBlockedRanges(String text) throws Exception
   {
      InetAddress address = InetAddress.getByName(text);
      assertThat(AddressRules.isInternal(address)).as(text).isTrue();
      assertThat(AddressRules.defaults().check("example.test", address)).as(text).isNotNull();
   }



   /*******************************************************************************
    ** Public addresses pass.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"8.8.8.8", "1.1.1.1", "93.184.216.34", "100.128.0.1", "172.32.0.1", "192.169.0.1", "2606:4700:4700::1111", "2002:808:808::1",
      "64:ff9b::808:808", "::ffff:8.8.8.8"})
   void testPublicAddressesPass(String text) throws Exception
   {
      InetAddress address = InetAddress.getByName(text);
      assertThat(AddressRules.isInternal(address)).as(text).isFalse();
      assertThat(AddressRules.defaults().check("example.test", address)).as(text).isNull();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAllowlistReopensTargets() throws Exception
   {
      AddressRules rules = new AddressRules(List.of("10.100.22.0/24", "192.168.1.5", "next.kof22.internal", ".svc.cluster.local", "fd00:1::/32", " ", ""));
      assertThat(rules.hasAllowlist()).isTrue();

      assertThat(rules.check("any", InetAddress.getByName("10.100.22.64"))).isNull();
      assertThat(rules.check("any", InetAddress.getByName("::ffff:10.100.22.64"))).isNull();
      assertThat(rules.check("any", InetAddress.getByName("10.100.23.1"))).contains("not on this endpoint's allowlist");
      assertThat(rules.check("any", InetAddress.getByName("192.168.1.5"))).isNull();
      assertThat(rules.check("any", InetAddress.getByName("192.168.1.6"))).isNotNull();
      assertThat(rules.check("NEXT.kof22.internal", InetAddress.getByName("10.1.2.3"))).isNull();
      assertThat(rules.check("other.kof22.internal", InetAddress.getByName("10.1.2.3"))).isNotNull();
      assertThat(rules.check("web.kof22.svc.cluster.local", InetAddress.getByName("172.25.0.10"))).isNull();
      assertThat(rules.check("svc.cluster.local.evil.test", InetAddress.getByName("172.25.0.10"))).isNotNull();
      assertThat(rules.check("any", InetAddress.getByName("fd00:1::5"))).isNull();
      assertThat(rules.check("any", InetAddress.getByName("fd00:2::5"))).isNotNull();
      assertThat(rules.check("any", InetAddress.getByName("8.8.8.8"))).isNull();

      assertThat(AddressRules.defaults().hasAllowlist()).isFalse();
      assertThat(new AddressRules(null).hasAllowlist()).isFalse();
   }



   /*******************************************************************************
    ** Metadata and unspecified addresses stay blocked whatever the allowlist.
    *******************************************************************************/
   @Test
   void testMetadataAlwaysBlocked() throws Exception
   {
      AddressRules rules = new AddressRules(List.of("169.254.0.0/16", "169.254.169.254", "100.64.0.0/10", "metadata.google.internal", "fd00::/8", "0.0.0.0/8", "::/0"));
      for(String text : List.of("169.254.169.254", "::ffff:169.254.169.254", "100.100.100.200", "fd00:ec2::254", "0.0.0.0", "::"))
      {
         assertThat(rules.check("metadata.google.internal", InetAddress.getByName(text))).as(text).contains("never allowed");
      }
      assertThat(rules.check("x", InetAddress.getByName("169.254.10.10"))).isNull();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadAllowlistEntries()
   {
      for(String bad : List.of("10.0.0.0/33", "10.0.0.0/x", "not a host", "-bad.example", "exa_mple.com", ".", "http://x", "999.1.1.1/8"))
      {
         assertThatThrownBy(() -> new AddressRules(List.of(bad))).as(bad).isInstanceOf(IllegalArgumentException.class);
      }
   }
}
