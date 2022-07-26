/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.nio.file.Path;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8057113
 * @summary Verify extension methods
 * @run testng Extensions
 */
public class Extensions {
    /**
     * Returns path name string and expected extension pairs.
     *
     * @return {@code {{"pathname", "extension"},...}}
     */
    @DataProvider
    static Object[][] getProvider() {
        Object[][] pairs = {
            {".",              null},
            {"..",             null},
            {".a",             null},
            {"a.",             ""},
            {"aa",             null},
            {".a.b",           "b"},
            {"......",         ""},
            {".....a",         "a"},
            {"...a.b",         "b"},
            {"..foo",          "foo"},
            {"test.rb",        "rb"},
            {"a/b/d/test.rb" , "rb"},
            {".a/b/d/test.rb", "rb"},
            {"foo.",           ""},
            {"test",           null},
            {".profile",       null},
            {".profile.sh",    "sh"},
            {"..foo",          "foo"},
            {".....foo",       "foo"},
            {".vimrc",         null},
            {"test.",          ""},
            {"test..",         ""},
            {"test...",        ""},
            {"foo.tar.gz",     "gz"},
            {"foo.bar.",       ""},
            {"image.jpg",      "jpg"},
            {"music.mp3",      "mp3"},
            {"video.mp4",      "mp4"},
            {"document.txt",   "txt"},
            {"x.zip",          "zip"},
            {"y.gzip",         "gzip"},
            {"z.jar",          "jar"},
        };
        return pairs;
    }

    @Test(dataProvider = "getProvider")
    public static void get(String pathname, String extension) {
        Assert.assertEquals(Path.of(pathname).getExtension(), extension);
    }
}
