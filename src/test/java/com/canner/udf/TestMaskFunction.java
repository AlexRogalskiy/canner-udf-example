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

package com.canner.udf;

import io.airlift.slice.Slices;
import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.canner.udf.scalar.MaskFunction.decrypt;
import static com.canner.udf.scalar.MaskFunction.encrypt;
import static com.canner.udf.scalar.MaskFunction.maskColumn;
import static com.canner.udf.scalar.MaskFunction.maskEmail;
import static com.canner.udf.scalar.MaskFunction.ripeMD160;
import static io.airlift.slice.Slices.utf8Slice;
import static org.testng.AssertJUnit.assertEquals;

public class TestMaskFunction
{
    @Test
    public void testMaskColumn()
    {
        assertEquals(
                maskColumn(Slices.utf8Slice("test")),
                utf8Slice("*****"));
    }

    @Test
    public void testRipeMD160()
            throws NoSuchAlgorithmException
    {
        assertEquals(
                new String(ripeMD160(utf8Slice("canner-dev")).getBytes()),
                "2a9dc86c2b13606ffeef8b257f619edfadf7e1d6");
    }

    @Test
    public void testEmailMask()
    {
        assertEquals(
                maskEmail(utf8Slice("canner-dev@cannerdata.com"), 5),
                utf8Slice("canne*****@cannerdata.com"));
    }

    @Test
    public void testEncryptDecrypt()
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException
    {
        assertEquals(
                decrypt(encrypt(utf8Slice("canner-dev"))),
                utf8Slice("canner-dev"));
    }
}
