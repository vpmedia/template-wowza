// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

package hu.vpmedia.media.wms.plugin.security.encryption;


public class TEA
{

    public TEA()
    {
    }

    public static String encrypt(String s, String s1)
    {
        int ai[] = charsToLongs(strToChars(s));
        int ai1[] = charsToLongs(strToChars(s1));
        int ai2[] = new int[4];
        for(int i = 0; i < ai2.length; i++)
            if(i < ai1.length)
                ai2[i] = ai1[i];
            else
                ai2[i] = 0;

        int j = ai.length;
        boolean flag = false;
        if(j == 0)
            return "";
        if(j == 1)
            ai[j++] = 0;
        int l = ai[j - 1];
        int i1 = ai[0];
        int l1 = 0x9e3779b9;
        int l2 = (int)Math.floor(6D + 52D / (double)j);
        int i3 = 0;
        while(l2-- > 0) 
        {
            i3 += l1;
            int k2 = i3 >>> 2 & 3;
            int k;
            for(k = 0; k < j - 1; k++)
            {
                int j1 = ai[k + 1];
                int i2 = (l >>> 5 ^ j1 << 2) + (j1 >>> 3 ^ l << 4) ^ (i3 ^ j1) + (ai2[k & 3 ^ k2] ^ l);
                l = ai[k] += i2;
            }

            int k1 = ai[0];
            int j2 = (l >>> 5 ^ k1 << 2) + (k1 >>> 3 ^ l << 4) ^ (i3 ^ k1) + (ai2[k & 3 ^ k2] ^ l);
            l = ai[j - 1] += j2;
        }
        return charsToHex(longsToChars(ai));
    }

    public static String decrypt(String s, String s1)
    {
        int ai[] = charsToLongs(hexToChars(s));
        int ai1[] = charsToLongs(strToChars(s1));
        int ai2[] = new int[4];
        for(int i = 0; i < ai2.length; i++)
            if(i < ai1.length)
                ai2[i] = ai1[i];
            else
                ai2[i] = 0;

        int j = ai.length;
        boolean flag = false;
        if(j == 0)
            return "";
        int l = ai[j - 1];
        int k1 = ai[0];
        int l1 = 0x9e3779b9;
        boolean flag1 = false;
        boolean flag2 = false;
        int l2 = (int)Math.floor(6D + 52D / (double)j);
        for(int i3 = l2 * l1; i3 != 0; i3 -= l1)
        {
            int k2 = i3 >>> 2 & 3;
            int k;
            for(k = j - 1; k > 0; k--)
            {
                int i1 = ai[k - 1];
                int i2 = (i1 >>> 5 ^ k1 << 2) + (k1 >>> 3 ^ i1 << 4) ^ (i3 ^ k1) + (ai2[k & 3 ^ k2] ^ i1);
                k1 = ai[k] -= i2;
            }

            int j1 = ai[j - 1];
            int j2 = (j1 >>> 5 ^ k1 << 2) + (k1 >>> 3 ^ j1 << 4) ^ (i3 ^ k1) + (ai2[k & 3 ^ k2] ^ j1);
            k1 = ai[0] -= j2;
        }

        return charsToStr(longsToChars(ai));
    }

    private static int[] charsToLongs(byte abyte0[])
    {
        int ai[] = new int[(int)Math.ceil((double)abyte0.length / 4D)];
        for(int i = 0; i < ai.length; i++)
        {
            ai[i] = 0;
            if(i * 4 + 3 < abyte0.length)
                ai[i] |= abyte0[i * 4 + 3] & 0xff;
            ai[i] <<= 8;
            if(i * 4 + 2 < abyte0.length)
                ai[i] |= abyte0[i * 4 + 2] & 0xff;
            ai[i] <<= 8;
            if(i * 4 + 1 < abyte0.length)
                ai[i] |= abyte0[i * 4 + 1] & 0xff;
            ai[i] <<= 8;
            ai[i] |= abyte0[i * 4] & 0xff;
        }

        return ai;
    }

    private static byte[] longsToChars(int ai[])
    {
        byte abyte0[] = new byte[ai.length * 4];
        for(int i = 0; i < ai.length; i++)
        {
            abyte0[i * 4 + 0] = (byte)(ai[i] & 0xff);
            abyte0[i * 4 + 1] = (byte)(ai[i] >> 8 & 0xff);
            abyte0[i * 4 + 2] = (byte)(ai[i] >> 16 & 0xff);
            abyte0[i * 4 + 3] = (byte)(ai[i] >> 24 & 0xff);
        }

        return abyte0;
    }

    private static String charsToHex(byte abyte0[])
    {
        String s = "";
        for(int i = 0; i < abyte0.length; i++)
        {
            String s1 = Integer.toHexString(abyte0[i] & 0xff);
            if(s1.length() < 2)
                s1 = (new StringBuilder()).append("0").append(s1).toString();
            s = (new StringBuilder()).append(s).append(s1).toString();
        }

        return s;
    }

    private static byte[] hexToChars(String s)
    {
        byte abyte0[] = new byte[s.length() / 2];
        for(int i = 0; i < s.length() / 2; i++)
            abyte0[i] = (byte)(Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16) & 0xff);

        return abyte0;
    }

    private static String charsToStr(byte abyte0[])
    {
        String s = null;
        try
        {
            s = new String(abyte0, "UTF-8");
            s = s.trim();
        }
        catch(Exception exception) { }
        return s;
    }

    private static byte[] strToChars(String s)
    {
        try
        {
            return s.getBytes("UTF-8");
        }
        catch(Exception exception)
        {
            return null;
        }
    }
}
