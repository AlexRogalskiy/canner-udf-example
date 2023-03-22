/*
 * Copyright 2023 Canner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.canner.udf.scalar;

import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.spi.function.Description;
import io.trino.spi.function.ScalarFunction;
import io.trino.spi.function.SqlType;
import io.trino.spi.type.StandardTypes;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;

import static io.airlift.slice.Slices.utf8Slice;

public class MaskFunction
{
    private static final Key secretKey;
    private static final IvParameterSpec ivParameterSpec;

    static {
        try {
            Security.addProvider(new BouncyCastleProvider());
            secretKey = getSecretKey();
            ivParameterSpec = getIvParameterSpec();
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private MaskFunction() {}

    @Description("RipeMD160")
    @ScalarFunction("ripemd_160")
    @SqlType(StandardTypes.VARCHAR)
    public static Slice ripeMD160(@SqlType(StandardTypes.VARCHAR) Slice value)
            throws NoSuchAlgorithmException
    {
        MessageDigest messageDigest = MessageDigest.getInstance("RipeMD160");
        messageDigest.update(value.getBytes());
        byte[] result = messageDigest.digest();
        return utf8Slice(new BigInteger(1, result).toString(16));
    }

    @Description("mask value in column")
    @ScalarFunction("mask_column")
    @SqlType(StandardTypes.VARCHAR)
    public static Slice maskColumn(@SqlType(StandardTypes.VARCHAR) Slice value)
    {
        return utf8Slice("*****");
    }

    @Description("mask email")
    @ScalarFunction("mask_email")
    @SqlType(StandardTypes.VARCHAR)
    public static Slice maskEmail(@SqlType(StandardTypes.VARCHAR) Slice value, @SqlType(StandardTypes.BIGINT) long num)
    {
        String email = value.toStringUtf8();
        return utf8Slice(email.replaceAll("(^[^@]{" + num + "}|(?!^)\\G)[^@]", "$1*"));
    }

    @Description("encrypt")
    @ScalarFunction("encrypt")
    @SqlType(StandardTypes.VARBINARY)
    public static Slice encrypt(@SqlType(StandardTypes.VARCHAR) Slice value)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        byte[] encrypted = cipher.doFinal(value.getBytes());
        return Slices.wrappedBuffer(encrypted);
    }

    @Description("decrypt")
    @ScalarFunction("decrypt")
    @SqlType(StandardTypes.VARCHAR)
    public static Slice decrypt(@SqlType(StandardTypes.VARBINARY) Slice value)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        byte[] decryptedInput = cipher.doFinal(value.getBytes());
        return Slices.wrappedBuffer(decryptedInput);
    }

    private static Key getSecretKey()
            throws NoSuchAlgorithmException
    {
        KeyGenerator aes = KeyGenerator.getInstance("AES");
        aes.init(192);
        return aes.generateKey();
    }

    private static IvParameterSpec getIvParameterSpec()
            throws NoSuchAlgorithmException
    {
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        byte[] random = new byte[16];
        secureRandom.nextBytes(random);
        return new IvParameterSpec(random);
    }
}
