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

package cn.dreamn.qianji_auto.core.hook.hooks.wechat.hooks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.dreamn.qianji_auto.core.hook.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class PayTools {
    public static void init(Utils utils) {
        FixedSizeList<String> payList = new FixedSizeList<String>(4);
        ClassLoader mAppClassLoader = utils.getClassLoader();
        Class<?> MMKRichLabelView = XposedHelpers.findClass("com.tencent.kinda.framework.widget.base.MMKRichLabelView", mAppClassLoader);
        Class<?> KTex = XposedHelpers.findClass("com.tencent.kinda.gen.KText", mAppClassLoader);
        Class<?> MMKRichText = XposedHelpers.findClass("com.tencent.kinda.framework.widget.base.MMKRichText", mAppClassLoader);
        XposedHelpers.findAndHookMethod(MMKRichLabelView, "setRichText", KTex, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Method get = MMKRichText.getDeclaredMethod("get");
                if (param.args[0] == null) {
                    utils.log("arg1 is null");
                    return;
                }
                String data = get.invoke(param.args[0]).toString();
                utils.log("设置数据：" + data, true);

                String[] cards = new String[]{"卡(", "零钱"};

                if (data.startsWith("支付安全由")) {
                    utils.log("识别商品名称：" + payList.get(0));
                    utils.log("识别的金额：" + payList.get(1));
                    utils.log("识别的商户：" + payList.get(3));
                    utils.writeData("cache_wechat_payName", payList.get(0));
                    utils.writeData("cache_wechat_payMoney", payList.get(1));
                    utils.writeData("cache_wechat_payUser", payList.get(3));
                    utils.writeData("cache_wechat_time", System.currentTimeMillis());
                } else if (inArray(data, cards, false)) {
                    //支付账户
                    utils.writeData("cache_wechat_paytool", data);
                    utils.log("识别的账户名：" + data);
                } else {
                    payList.add(data);
                    utils.log("识别的数据：" + payList);
                }
                /*
                String[] empty = new String[]{"支付", "使用", "请", "待", "识别", "失败"};
                String[] money = new String[]{"￥", "$"};

                if (inArray(data, empty, true)) {
                    return;
                }
                //微信官方用语，删了~
                if (inArray(data, cards, false)) {
                    //支付账户
                    utils.writeData("cache_wechat_paytool", data);
                    utils.log("识别的账户名：" + data);
                } else if (inArray(data, money, true)) {
                    utils.log("识别的金额：" + data);
                    //金额
                    utils.writeData("cache_wechat_payMoney", data);
                } else if (isUser[0]) {
                    utils.log("识别的商户：" + data);
                    //转账人
                    utils.writeData("cache_wechat_payUser", data);
                    isUser[0] = false;
                } else if (data.equals("收款方")) {
                    isUser[0] = true;
                } else {
                    utils.log("识别商品名称：" + data);
                    //商品名称
                    utils.writeData("cache_wechat_payName", data);
                }
                */
            }

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
            }
        });
    }

    private static boolean inArray(String data, String[] array, boolean start) {
        for (String arr : array) {
            if (start) {
                if (data.startsWith(arr)) return true;
            } else {
                if (data.contains(arr)) return true;
            }
        }
        return false;
    }

    public static class FixedSizeList<T> extends ArrayList<T> {
        private final int capacity;

        public FixedSizeList(int capacity) {
            super(capacity);
            this.capacity = capacity;
        }

        @Override
        public boolean add(T t) {
            if (size() == capacity) {
                remove(0); // 移除最旧的元素
            }
            return super.add(t);
        }
    }
}
