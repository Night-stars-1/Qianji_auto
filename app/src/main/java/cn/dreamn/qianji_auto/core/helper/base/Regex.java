/*
 * Copyright (C) 2021 dreamn(dream@dreamn.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package cn.dreamn.qianji_auto.core.helper.base;

import java.util.regex.Pattern;

public class Regex {
    public static boolean isMatch(String str, String pattern) {
        return Pattern.matches(".*" + pattern, str);
    }

    public static boolean isMatchEnd(String str, String pattern) {
        return Pattern.matches(".*" + pattern + "]$", str);
    }
}
