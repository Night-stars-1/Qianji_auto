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

package cn.dreamn.qianji_auto.core.hook.hooks.qq.hooks;

import android.content.ContentValues;
import android.content.Context;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.dreamn.qianji_auto.core.hook.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;


public class Msg {
    public static void init(Utils utils) throws ClassNotFoundException {
        ClassLoader mAppClassLoader = utils.getClassLoader();

        //final Class<?> msg_record_class = mAppClassLoader.loadClass("com.tencent.mobileqq.data.MessageRecord");
        XposedHelpers.findAndHookMethod(
                "com.tencent.mobileqq.app.MessageHandlerUtils", // 类名
                mAppClassLoader, // 类加载器
                "msgFilter", // 方法名
                "com.tencent.common.app.AppInterface", // 参数1: AppInterface
                "com.tencent.mobileqq.data.MessageRecord", // 参数2: MessageRecord
                boolean.class, // 参数3: boolean
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String msgtype = XposedHelpers.getObjectField(param.args[1], "msgtype").toString();
                        utils.log("QQ消息类型：" + msgtype);
                        String s = (String) XposedHelpers.callMethod(param.args[1], "toString");
                        if (msgtype.equals("-1012")) {
                            utils.log("检验点");
                            byte[] msgData = (byte[]) XposedHelpers.getObjectField(param.args[1], "msgData");
                            utils.log(Arrays.toString(msgData));
                            String data = new String(msgData).replaceAll("[^0-9a-zA-Z\u4e00-\u9fa5\\s.，,。？“”/:=+-]+", "\n");
                            Pattern p = Pattern.compile("(\r?\n(\\s*\r?\n)+)");
                            Matcher m = p.matcher(data);
                            data = m.replaceAll("\n");

                            //去重
                            String last = utils.readData("lastQQ");
                            utils.writeData("lastQQ", data);
                            if (data.equals(last)) {
                                utils.log("QQ重复账单");
                                return;
                            }
                            utils.sendString(data);
                        }
                        utils.log(s);
                        //utils.dumpFields(param.args[1], msg_record_class);
                    }
                }
        );

    }


}
