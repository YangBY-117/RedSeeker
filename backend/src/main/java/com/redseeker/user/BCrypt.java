package com.redseeker.user;

import java.security.SecureRandom;

final class BCrypt {
  private static final int GENSALT_DEFAULT_LOG2_ROUNDS = 10;
  private static final int BCRYPT_SALT_LEN = 16;

  private static final int BLOWFISH_NUM_ROUNDS = 16;

  private static final int[] P_ORIG = {
    0x243f6a88, 0x85a308d3, 0x13198a2e, 0x03707344, 0xa4093822, 0x299f31d0,
    0x082efa98, 0xec4e6c89, 0x452821e6, 0x38d01377, 0xbe5466cf, 0x34e90c6c,
    0xc0ac29b7, 0xc97c50dd, 0x3f84d5b5, 0xb5470917, 0x9216d5d9, 0x8979fb1b
  };

  private static final int[] S_ORIG = {
    0xd1310ba6, 0x98dfb5ac, 0x2ffd72db, 0xd01adfb7, 0xb8e1afed, 0x6a267e96,
    0xba7c9045, 0xf12c7f99, 0x24a19947, 0xb3916cf7, 0x0801f2e2, 0x858efc16,
    0x636920d8, 0x71574e69, 0xa458fea3, 0xf4933d7e, 0x0d95748f, 0x728eb658,
    0x718bcd58, 0x82154aee, 0x7b54a41d, 0xc25a59b5, 0x9c30d539, 0x2af26013,
    0xc5d1b023, 0x286085f0, 0xca417918, 0xb8db38ef, 0x8e79dcb0, 0x603a180e,
    0x6c9e0e8b, 0xb01e8a3e, 0xd71577c1, 0xbd314b27, 0x78af2fda, 0x55605c60,
    0xe65525f3, 0xaa55ab94, 0x57489862, 0x63e81440, 0x55ca396a, 0x2aab10b6,
    0xb4cc5c34, 0x1141e8ce, 0xa15486af, 0x7c72e993, 0xb3ee1411, 0x636fbc2a,
    0x2ba9c55d, 0x741831f6, 0xce5c3e16, 0x9b87931e, 0xafd6ba33, 0x6c24cf5c,
    0x7a325381, 0x28958677, 0x3b8f4898, 0x6b4bb9af, 0xc4bfe81b, 0x66282193,
    0x61d809cc, 0xfb21a991, 0x487cac60, 0x5dec8032, 0xef845d5d, 0xe98575b1,
    0xdc262302, 0xeb651b88, 0x23893e81, 0xd396acc5, 0x0f6d6ff3, 0x83f44239,
    0x2e0b4482, 0xa4842004, 0x69c8f04a, 0x9e1f9b5e, 0x21c66842, 0xf6e96c9a,
    0x670c9c61, 0xabd388f0, 0x6a51a0d2, 0xd8542f68, 0x960fa728, 0xab5133a3,
    0x6eef0b6c, 0x137a3be4, 0xba3bf050, 0x7efb2a98, 0xa1f1651d, 0x39af0176,
    0x66ca593e, 0x82430e88, 0x8cee8619, 0x456f9fb4, 0x7d84a5c3, 0x3b8b5ebe,
    0xe06f75d8, 0x85c12073, 0x401a449f, 0x56c16aa6, 0x4ed3aa62, 0x363f7706,
    0x1bfedf72, 0x429b023d, 0x37d0d724, 0xd00a1248, 0xdb0fead3, 0x49f1c09b,
    0x075372c9, 0x80991b7b, 0x25d479d8, 0xf6e8def7, 0xe3fe501a, 0xb6794c3b,
    0x976ce0bd, 0x04c006ba, 0xc1a94fb6, 0x409f60c4, 0x5e5c9ec2, 0x196a2463,
    0x68fb6faf, 0x3e6c53b5, 0x1339b2eb, 0x3b52ec6f, 0x6dfc511f, 0x9b30952c,
    0xcc814544, 0xaf5ebd09, 0xbee3d004, 0xde334afd, 0x660f2807, 0x192e4bb3,
    0xc0cba857, 0x45c8740f, 0xd20b5f39, 0xb9d3fbdb, 0x5579c0bd, 0x1a60320a,
    0xd6a100c6, 0x402c7279, 0x679f25fe, 0xfb1fa3cc, 0x8ea5e9f8, 0xdb3222f8,
    0x3c7516df, 0xfd616b15, 0x2f501ec8, 0xad0552ab, 0x323db5fa, 0xfd238760,
    0x53317b48, 0x3e00df82, 0x9e5c57bb, 0xca6f8ca0, 0x1a87562e, 0xdf1769db,
    0xd542a8f6, 0x287effc3, 0xac6732c6, 0x8c4f5573, 0x695b27b0, 0xbbca58c8,
    0xe1ffa35d, 0xb8f011a0, 0x10fa3d98, 0xfd2183b8, 0x4afcb56c, 0x2dd1d35b,
    0x9a53e479, 0xb6f84565, 0xd28e49bc, 0x4bfb9790, 0xe1ddf2da, 0xa4cb7e33,
    0x62fb1341, 0xcee4c6e8, 0xef20cada, 0x36774c01, 0xd07e9efe, 0x2bf11fb4,
    0x95dbda4d, 0xae909198, 0xeaad8e71, 0x6b93d5a0, 0xd08ed1d0, 0xafc725e0,
    0x8e3c5b2f, 0x8e7594b7, 0x8ff6e2fb, 0xf2122b64, 0x8888b812, 0x900df01c,
    0x4fad5ea0, 0x688fc31c, 0xd1cff191, 0xb3a8c1ad, 0x2f2f2218, 0xbe0e1777,
    0xea752dfe, 0x8b021fa1, 0xe5a0cc0f, 0xb56f74e8, 0x18acf3d6, 0xce89e299,
    0xb4a84fe0, 0xfd13e0b7, 0x7cc43b81, 0xd2ada8d9, 0x165fa266, 0x80957705,
    0x93cc7314, 0x211a1477, 0xe6ad2065, 0x77b5fa86, 0xc75442f5, 0xfb9d35cf,
    0xebcdaf0c, 0x7b3e89a0, 0xd6411bd3, 0xae1e7e49, 0x00250e2d, 0x2071b35e,
    0x226800bb, 0x57b8e0af, 0x2464369b, 0xf009b91e, 0x5563911d, 0x59dfa6aa,
    0x78c14389, 0xd95a537f, 0x207d5ba2, 0x02e5b9c5, 0x83260376, 0x6295cfa9,
    0x11c81968, 0x4e734a41, 0xb3472dca, 0x7b14a94a, 0x1b510052, 0x9a532915,
    0xd60f573f, 0xbc9bc6e4, 0x2b60a476, 0x81e67400, 0x08ba6fb5, 0x571be91f,
    0xf296ec6b, 0x2a0dd915, 0xb6636521, 0xe7b9f9b6, 0xff34052e, 0xc5855664,
    0x53b02d5d, 0xa99f8fa1, 0x08ba4799, 0x6e85076a
  };

  private static final char[] BASE64_CODE = {
    '.', '/', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b',
    'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
    'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
    '6', '7', '8', '9'
  };

  private static final int[] INDEX_64 = new int[128];

  static {
    for (int i = 0; i < INDEX_64.length; i++) {
      INDEX_64[i] = -1;
    }
    for (int i = 0; i < BASE64_CODE.length; i++) {
      INDEX_64[BASE64_CODE[i]] = i;
    }
  }

  private BCrypt() {
  }

  static String hashpw(String password, String salt) {
    byte[] passwordBytes = (password + "\u0000").getBytes();
    byte[] saltBytes = decodeBase64(salt.substring(7, 29), BCRYPT_SALT_LEN);
    int rounds = Integer.parseInt(salt.substring(4, 6));

    BCryptOpenBSD bcrypt = new BCryptOpenBSD();
    byte[] hashed = bcrypt.cryptRaw(passwordBytes, saltBytes, rounds);
    StringBuilder rs = new StringBuilder();
    rs.append(salt.substring(0, 29));
    rs.append(encodeBase64(hashed, hashed.length - 1));
    return rs.toString();
  }

  static String gensalt() {
    return gensalt(GENSALT_DEFAULT_LOG2_ROUNDS);
  }

  static String gensalt(int logRounds) {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[BCRYPT_SALT_LEN];
    random.nextBytes(salt);
    return "$2a$" + (logRounds < 10 ? "0" + logRounds : logRounds) + "$"
        + encodeBase64(salt, salt.length);
  }

  static boolean checkpw(String plaintext, String hashed) {
    if (hashed == null || !hashed.startsWith("$2")) {
      return false;
    }
    String salt = hashed.substring(0, 29);
    String hashedCandidate = hashpw(plaintext, salt);
    return slowEquals(hashedCandidate.getBytes(), hashed.getBytes());
  }

  private static boolean slowEquals(byte[] a, byte[] b) {
    int diff = a.length ^ b.length;
    for (int i = 0; i < a.length && i < b.length; i++) {
      diff |= a[i] ^ b[i];
    }
    return diff == 0;
  }

  private static String encodeBase64(byte[] d, int len) {
    int off = 0;
    StringBuilder rs = new StringBuilder();
    int c1;
    int c2;
    while (off < len) {
      c1 = d[off++] & 0xff;
      rs.append(BASE64_CODE[(c1 >> 2) & 0x3f]);
      c1 = (c1 & 0x03) << 4;
      if (off >= len) {
        rs.append(BASE64_CODE[c1 & 0x3f]);
        break;
      }
      c2 = d[off++] & 0xff;
      c1 |= (c2 >> 4) & 0x0f;
      rs.append(BASE64_CODE[c1 & 0x3f]);
      c1 = (c2 & 0x0f) << 2;
      if (off >= len) {
        rs.append(BASE64_CODE[c1 & 0x3f]);
        break;
      }
      c2 = d[off++] & 0xff;
      c1 |= (c2 >> 6) & 0x03;
      rs.append(BASE64_CODE[c1 & 0x3f]);
      rs.append(BASE64_CODE[c2 & 0x3f]);
    }
    return rs.toString();
  }

  private static byte[] decodeBase64(String s, int maxolen) {
    int off = 0;
    int slen = s.length();
    int olen = 0;
    byte[] ret = new byte[maxolen];
    int c1;
    int c2;
    int c3;
    int c4;
    int o;
    while (off < slen - 1 && olen < maxolen) {
      c1 = char64(s.charAt(off++));
      c2 = char64(s.charAt(off++));
      if (c1 == -1 || c2 == -1) {
        break;
      }
      o = (c1 << 2);
      o |= (c2 & 0x30) >> 4;
      ret[olen++] = (byte) o;
      if (olen >= maxolen || off >= slen) {
        break;
      }
      c3 = char64(s.charAt(off++));
      if (c3 == -1) {
        break;
      }
      o = (c2 & 0x0f) << 4;
      o |= (c3 & 0x3c) >> 2;
      ret[olen++] = (byte) o;
      if (olen >= maxolen || off >= slen) {
        break;
      }
      c4 = char64(s.charAt(off++));
      o = (c3 & 0x03) << 6;
      o |= c4;
      ret[olen++] = (byte) o;
    }
    return ret;
  }

  private static int char64(char x) {
    if (x < 0 || x > 127) {
      return -1;
    }
    return INDEX_64[x];
  }

  private static final class BCryptOpenBSD {
    private int[] p;
    private int[] s;

    private byte[] cryptRaw(byte[] password, byte[] salt, int logRounds) {
      int rounds = 1 << logRounds;
      if (salt.length != BCRYPT_SALT_LEN) {
        throw new IllegalArgumentException("Bad salt length");
      }
      initKey();
      eksKey(salt, password);
      for (int i = 0; i < rounds; i++) {
        key(password);
        key(salt);
      }
      int[] cdata = {
        0x4f727068, 0x65616e42, 0x65686f6c, 0x64657253,
        0x63727944, 0x6f756274
      };
      for (int i = 0; i < 64; i++) {
        encipher(cdata, 0);
        encipher(cdata, 2);
        encipher(cdata, 4);
      }
      byte[] ret = new byte[cdata.length * 4];
      int j = 0;
      for (int i = 0; i < cdata.length; i++) {
        ret[j++] = (byte) ((cdata[i] >> 24) & 0xff);
        ret[j++] = (byte) ((cdata[i] >> 16) & 0xff);
        ret[j++] = (byte) ((cdata[i] >> 8) & 0xff);
        ret[j++] = (byte) (cdata[i] & 0xff);
      }
      return ret;
    }

    private void initKey() {
      p = P_ORIG.clone();
      s = S_ORIG.clone();
    }

    private void key(byte[] key) {
      int offset = 0;
      int[] lr = {0, 0};
      for (int i = 0; i < p.length; i++) {
        p[i] = p[i] ^ streamToWord(key, offset);
        offset = (offset + 4) % key.length;
      }
      for (int i = 0; i < p.length; i += 2) {
        encipher(lr, 0);
        p[i] = lr[0];
        p[i + 1] = lr[1];
      }
      for (int i = 0; i < s.length; i += 2) {
        encipher(lr, 0);
        s[i] = lr[0];
        s[i + 1] = lr[1];
      }
    }

    private void eksKey(byte[] data, byte[] key) {
      int[] lr = {0, 0};
      int dataOffset = 0;
      int keyOffset = 0;
      for (int i = 0; i < p.length; i++) {
        p[i] = p[i] ^ streamToWord(key, keyOffset);
        keyOffset = (keyOffset + 4) % key.length;
      }
      for (int i = 0; i < p.length; i += 2) {
        lr[0] ^= streamToWord(data, dataOffset);
        dataOffset = (dataOffset + 4) % data.length;
        lr[1] ^= streamToWord(data, dataOffset);
        dataOffset = (dataOffset + 4) % data.length;
        encipher(lr, 0);
        p[i] = lr[0];
        p[i + 1] = lr[1];
      }
      for (int i = 0; i < s.length; i += 2) {
        lr[0] ^= streamToWord(data, dataOffset);
        dataOffset = (dataOffset + 4) % data.length;
        lr[1] ^= streamToWord(data, dataOffset);
        dataOffset = (dataOffset + 4) % data.length;
        encipher(lr, 0);
        s[i] = lr[0];
        s[i + 1] = lr[1];
      }
    }

    private int streamToWord(byte[] data, int offp) {
      int word = 0;
      for (int i = 0; i < 4; i++) {
        word = (word << 8) | (data[offp] & 0xff);
        offp = (offp + 1) % data.length;
      }
      return word;
    }

    private void encipher(int[] lr, int off) {
      int l = lr[off];
      int r = lr[off + 1];
      l ^= p[0];
      for (int i = 0; i <= BLOWFISH_NUM_ROUNDS - 2; ) {
        r ^= f(l) ^ p[++i];
        l ^= f(r) ^ p[++i];
      }
      lr[off] = r ^ p[BLOWFISH_NUM_ROUNDS + 1];
      lr[off + 1] = l;
    }

    private int f(int x) {
      int h = s[(x >> 24) & 0xff];
      h += s[0x100 | ((x >> 16) & 0xff)];
      h ^= s[0x200 | ((x >> 8) & 0xff)];
      h += s[0x300 | (x & 0xff)];
      return h;
    }
  }
}
