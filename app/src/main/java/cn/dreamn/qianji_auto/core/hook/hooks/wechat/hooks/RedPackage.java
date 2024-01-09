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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Field;
import java.util.Objects;

import cn.dreamn.qianji_auto.core.hook.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class RedPackage {
    public static void init(Utils utils, JSONArray jsonArray) {
        ClassLoader mAppClassLoader = utils.getClassLoader();
        try {
            String cls =jsonArray.getString(0);
            String method = jsonArray.getString(1);
            String qVar = jsonArray.getString(2);
            Class<?> qVar2 = XposedHelpers.findClass(qVar, mAppClassLoader);
            XposedHelpers.findAndHookMethod(cls, mAppClassLoader, method, qVar2, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);

                    setParam(param, utils, qVar2);
                }
            });
        } catch (Error | Exception e) {
            utils.log("hook红包失败！" + e);
        }
    }

    private static void setParam(XC_MethodHook.MethodHookParam param, Utils utils, Class<?> qVar) throws IllegalAccessException, NoSuchFieldException {
        utils.log("hooked qvar");
        if (param.args[0] == null) {
            utils.log("qVar = null");
            return;
        }

        Object object = param.args[0];
        Field[] fields = qVar.getDeclaredFields();
        StringBuilder log = new StringBuilder("微信的红包相关数据\n");
        int i = 0;
        for (Field f : fields) {
            f.setAccessible(true);
            i++;
            Object obj = f.get(object);
            String str;
            if (obj == null) str = "null";
            else str = obj.toString();
            log.append("属性名").append(i).append(":").append(f.getName()).append(" 属性值:").append(str).append("\n");
        }

        utils.log(log.toString());

        String json = utils.readData("red");
        if (json.isEmpty()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("money", "AcI");
            jsonObject.put("total", "AZd");
            jsonObject.put("remark", "zXl");
            jsonObject.put("shop", "AcD");
            jsonObject.put("status", "AcB");
            jsonObject.put("group", "rid");
            json = jsonObject.toJSONString();
        }

        JSONObject jsonObject = JSONObject.parseObject(json);
        String qVarMoney = jsonObject.getString("money");
        String qVarRemark = jsonObject.getString("remark");
        String qVarShop = jsonObject.getString("shop");
        String qVarStatus = jsonObject.getString("status");
        String qVarGroup = jsonObject.getString("group");
        String qVarTotal = jsonObject.getString("total");


        Field money = qVar.getDeclaredField(qVarMoney);
        Field remark = qVar.getDeclaredField(qVarRemark);
        Field shopAccount = qVar.getDeclaredField(qVarShop);
        Field status = qVar.getDeclaredField(qVarStatus);
        Field groups = qVar.getDeclaredField(qVarGroup);
        Field total = qVar.getDeclaredField(qVarTotal);

        double m = Integer.parseInt(Objects.requireNonNull(money.get(object)).toString()) / 100.0d;
        double t = Integer.parseInt(Objects.requireNonNull(total.get(object)).toString()) / 100.0d;
        if (m == 0)//金额为0直接忽略
            return;
        String remarkStr = Objects.requireNonNull(remark.get(object)).toString();
        if (remarkStr.isEmpty()) {
            remarkStr = "大吉大利，恭喜发财";
        }
        String n = Objects.requireNonNull(groups.get(object)).toString();
        String isGroup = (n.equals("1")) ? "false" : "true";
        //增加 isGroup
        String data = "money=%s,remark=%s,status=%s,shop=%s,isGroup=%s,numbers=%s,total=%s,title=微信收到红包";

        data = String.format(data, m, remarkStr, Objects.requireNonNull(status.get(object)), Objects.requireNonNull(shopAccount.get(object)), isGroup, n, t);

        // utils.log("红包数据：" + data);

        utils.sendString(data);
    }
}
