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


import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/*******************************************************************************
 ** Which network addresses an outbound request may reach.
 **
 ** <p>Blocked by default: "this network" (0/8), private (10/8, 172.16/12,
 ** 192.168/16), carrier-grade NAT (100.64/10), loopback (127/8, ::1),
 ** link-local (169.254/16, fe80::/10, which covers cloud metadata), IETF
 ** protocol and documentation ranges, benchmarking (198.18/15), multicast,
 ** reserved and broadcast (224/4, 240/4), IPv6 unique-local (fc00::/7),
 ** site-local (fec0::/10), discard (100::/64) and documentation (2001:db8::/32).
 ** IPv4 addresses embedded in IPv6 (mapped, compatible, NAT64 64:ff9b::/96 and
 ** 6to4 2002::/16) are checked as IPv4.</p>
 **
 ** <p>An allowlist re-opens specific internal targets: CIDR blocks
 ** ({@code 10.100.22.0/24}), single addresses, exact host names, or host
 ** suffixes written with a leading dot ({@code .svc.cluster.local}). The cloud
 ** metadata addresses (169.254.169.254, 100.100.100.200, fd00:ec2::254) and the
 ** unspecified addresses stay blocked even when allowlisted.</p>
 *******************************************************************************/
public final class AddressRules
{
   /** A dotted-quad IPv4 literal with octets 0-255. */
   private static final String IPV4 = "((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";

   private static final List<Cidr> BLOCKED = List.of(
      Cidr.parse("0.0.0.0/8"),
      Cidr.parse("10.0.0.0/8"),
      Cidr.parse("100.64.0.0/10"),
      Cidr.parse("127.0.0.0/8"),
      Cidr.parse("169.254.0.0/16"),
      Cidr.parse("172.16.0.0/12"),
      Cidr.parse("192.0.0.0/24"),
      Cidr.parse("192.0.2.0/24"),
      Cidr.parse("192.88.99.0/24"),
      Cidr.parse("192.168.0.0/16"),
      Cidr.parse("198.18.0.0/15"),
      Cidr.parse("198.51.100.0/24"),
      Cidr.parse("203.0.113.0/24"),
      Cidr.parse("224.0.0.0/4"),
      Cidr.parse("240.0.0.0/4"),
      Cidr.parse("::/128"),
      Cidr.parse("::1/128"),
      Cidr.parse("100::/64"),
      Cidr.parse("2001:db8::/32"),
      Cidr.parse("fc00::/7"),
      Cidr.parse("fe80::/10"),
      Cidr.parse("fec0::/10"),
      Cidr.parse("ff00::/8"));

   private static final List<Cidr> ALWAYS_BLOCKED = List.of(
      Cidr.parse("0.0.0.0/32"),
      Cidr.parse("169.254.169.254/32"),
      Cidr.parse("100.100.100.200/32"),
      Cidr.parse("::/128"),
      Cidr.parse("fd00:ec2::254/128"));

   private final List<Cidr>   allowedCidrs     = new ArrayList<>();
   private final List<String> allowedHosts     = new ArrayList<>();
   private final List<String> allowedSuffixes  = new ArrayList<>();



   /*******************************************************************************
    ** Rules with an allowlist (null or empty: nothing internal is allowed).
    **
    ** @throws IllegalArgumentException for an entry that is not a CIDR, an
    **         address, a host name or a .suffix
    *******************************************************************************/
   public AddressRules(List<String> allowlist)
   {
      if(allowlist == null)
      {
         return;
      }
      for(String raw : allowlist)
      {
         String entry = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
         if(entry.isEmpty())
         {
            continue;
         }
         if(entry.contains("/") || looksLikeAddress(entry) || entry.matches("[0-9.]+"))
         {
            allowedCidrs.add(Cidr.parse(entry.contains("/") ? entry : entry + (entry.contains(":") ? "/128" : "/32")));
         }
         else if(entry.startsWith("."))
         {
            validateHostName(entry.substring(1), raw);
            allowedSuffixes.add(entry);
         }
         else
         {
            validateHostName(entry, raw);
            allowedHosts.add(entry);
         }
      }
   }



   /*******************************************************************************
    ** Rules with no allowlist.
    *******************************************************************************/
   public static AddressRules defaults()
   {
      return (new AddressRules(null));
   }



   /*******************************************************************************
    ** True when the address is in a blocked range (ignoring any allowlist).
    *******************************************************************************/
   public static boolean isInternal(InetAddress address)
   {
      InetAddress effective = embeddedIpv4(address);
      for(Cidr cidr : BLOCKED)
      {
         if(cidr.contains(effective) || cidr.contains(address))
         {
            return (true);
         }
      }
      return (false);
   }



   /*******************************************************************************
    ** Null when the host may be reached at this address, else the reason.
    *******************************************************************************/
   public String check(String host, InetAddress address)
   {
      InetAddress effective = embeddedIpv4(address);
      for(Cidr cidr : ALWAYS_BLOCKED)
      {
         if(cidr.contains(effective) || cidr.contains(address))
         {
            return (address.getHostAddress() + " is a cloud metadata or unspecified address and is never allowed");
         }
      }
      if(!isInternal(address))
      {
         return (null);
      }
      String lowerHost = host == null ? "" : host.toLowerCase(Locale.ROOT);
      if(allowedHosts.contains(lowerHost))
      {
         return (null);
      }
      for(String suffix : allowedSuffixes)
      {
         if(lowerHost.endsWith(suffix))
         {
            return (null);
         }
      }
      for(Cidr cidr : allowedCidrs)
      {
         if(cidr.contains(effective) || cidr.contains(address))
         {
            return (null);
         }
      }
      return (address.getHostAddress() + " is internal (private, loopback, link-local or reserved) and is not on this endpoint's allowlist");
   }



   /*******************************************************************************
    ** True when there is any allowlist entry.
    *******************************************************************************/
   public boolean hasAllowlist()
   {
      return (!allowedCidrs.isEmpty() || !allowedHosts.isEmpty() || !allowedSuffixes.isEmpty());
   }



   /*******************************************************************************
    ** The IPv4 address embedded in an IPv6 address (mapped, compatible, NAT64,
    ** 6to4), or the address itself.
    *******************************************************************************/
   static InetAddress embeddedIpv4(InetAddress address)
   {
      if(!(address instanceof Inet6Address))
      {
         return (address);
      }
      byte[] b = address.getAddress();
      try
      {
         boolean firstTenZero = true;
         for(int i = 0; i < 10; i++)
         {
            firstTenZero &= b[i] == 0;
         }
         boolean mapped     = firstTenZero && (b[10] & 0xff) == 0xff && (b[11] & 0xff) == 0xff;
         boolean compatible = firstTenZero && b[10] == 0 && b[11] == 0 && !(b[12] == 0 && b[13] == 0 && b[14] == 0 && (b[15] == 0 || b[15] == 1));
         if(mapped || compatible)
         {
            return (InetAddress.getByAddress(new byte[] {b[12], b[13], b[14], b[15]}));
         }
         boolean nat64 = (b[0] & 0xff) == 0x00 && (b[1] & 0xff) == 0x64 && (b[2] & 0xff) == 0xff && (b[3] & 0xff) == 0x9b;
         for(int i = 4; i < 12 && nat64; i++)
         {
            nat64 = b[i] == 0;
         }
         if(nat64)
         {
            return (InetAddress.getByAddress(new byte[] {b[12], b[13], b[14], b[15]}));
         }
         if((b[0] & 0xff) == 0x20 && (b[1] & 0xff) == 0x02)
         {
            return (InetAddress.getByAddress(new byte[] {b[2], b[3], b[4], b[5]}));
         }
      }
      catch(UnknownHostException e)
      {
         return (address);
      }
      return (address);
   }



   /*******************************************************************************
    ** True for a literal IPv4 address (four octets of 0-255) or IPv6 address,
   ** so parsing it never triggers a DNS lookup.
    *******************************************************************************/
   static boolean looksLikeAddress(String text)
   {
      return (text.matches(IPV4) || (text.contains(":") && text.matches("[0-9a-f:.]+")));
   }



   /*******************************************************************************
    ** A host name: letters, digits, hyphens and dots.
    *******************************************************************************/
   private static void validateHostName(String name, String raw)
   {
      boolean valid = !name.isEmpty() && name.length() <= 253;
      for(String label : name.split("\\.", -1))
      {
         valid &= isHostLabel(label);
      }
      if(!valid)
      {
         throw (new IllegalArgumentException("Not a CIDR block, address, host name or .suffix: " + raw));
      }
   }



   /*******************************************************************************
    ** One DNS label: 1-63 of a-z, 0-9 and '-', not starting or ending with '-'.
    ** (A plain loop rather than a regex, so no input can make it backtrack.)
    *******************************************************************************/
   private static boolean isHostLabel(String label)
   {
      if(label.isEmpty() || label.length() > 63 || label.charAt(0) == '-' || label.charAt(label.length() - 1) == '-')
      {
         return (false);
      }
      for(int i = 0; i < label.length(); i++)
      {
         char c = label.charAt(i);
         if(!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-'))
         {
            return (false);
         }
      }
      return (true);
   }



   /*******************************************************************************
    ** A CIDR block.
    *******************************************************************************/
   static final class Cidr
   {
      private final byte[] network;
      private final int    prefix;



      /*******************************************************************************
       ** Constructor.
       *******************************************************************************/
      private Cidr(byte[] network, int prefix)
      {
         this.network = network;
         this.prefix = prefix;
      }



      /*******************************************************************************
       ** Parse {@code address/prefix}.
       *******************************************************************************/
      static Cidr parse(String text)
      {
         int slash = text.indexOf('/');
         if(slash < 0)
         {
            throw (new IllegalArgumentException("Not a CIDR block: " + text));
         }
         String addressText = text.substring(0, slash);
         if(!looksLikeAddress(addressText))
         {
            throw (new IllegalArgumentException("Not a CIDR block: " + text));
         }
         try
         {
            byte[] bytes  = InetAddress.getByName(addressText).getAddress();
            int    prefix = Integer.parseInt(text.substring(slash + 1));
            if(prefix < 0 || prefix > bytes.length * 8)
            {
               throw (new IllegalArgumentException("Bad prefix length in " + text));
            }
            return (new Cidr(bytes, prefix));
         }
         catch(UnknownHostException | NumberFormatException e)
         {
            throw (new IllegalArgumentException("Not a CIDR block: " + text, e));
         }
      }



      /*******************************************************************************
       ** True when the address is inside the block (same family only).
       *******************************************************************************/
      boolean contains(InetAddress address)
      {
         byte[] bytes = address.getAddress();
         if(bytes.length != network.length || (address instanceof Inet4Address) != (network.length == 4))
         {
            return (false);
         }
         int fullBytes = prefix / 8;
         for(int i = 0; i < fullBytes; i++)
         {
            if(bytes[i] != network[i])
            {
               return (false);
            }
         }
         int remainder = prefix % 8;
         if(remainder == 0)
         {
            return (true);
         }
         int mask = (0xff << (8 - remainder)) & 0xff;
         return ((bytes[fullBytes] & mask) == (network[fullBytes] & mask));
      }
   }
}
