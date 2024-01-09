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

package cn.dreamn.qianji_auto.core.hook.hooks.wechat;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;

import cn.dreamn.qianji_auto.core.hook.core.hookBase;
import cn.dreamn.qianji_auto.core.hook.hooks.qianji.Qianji;
import cn.dreamn.qianji_auto.core.hook.hooks.wechat.hooks.LoginInfo;
import cn.dreamn.qianji_auto.core.hook.hooks.wechat.hooks.Msg;
import cn.dreamn.qianji_auto.core.hook.hooks.wechat.hooks.NickName;
import cn.dreamn.qianji_auto.core.hook.hooks.wechat.hooks.PayTools;
import cn.dreamn.qianji_auto.core.hook.hooks.wechat.hooks.RedPackage;
import cn.dreamn.qianji_auto.core.hook.hooks.wechat.hooks.Setting;
import dalvik.system.DexFile;

public class Wechat extends hookBase {
    static final hookBase self = new Wechat();
    public static hookBase getInstance() {
        return self;
    }


    private Handler mHandler;
    private Thread mUiThread;

    final void attach(){
        mHandler = new Handler();
        mUiThread = Thread.currentThread();
    }

    public final void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != mUiThread) {
            mHandler.post(action);
        } else {
            action.run();
        }
    }


    interface RunBody {
        void run(Class<?> clz) throws IllegalAccessException, InstantiationException, InvocationTargetException;
    }
    private void scan(Context ctx, ClassLoader loader, RunBody runnable) {
        try {
            //  utils.log("Class-dump-dex:"+ctx.getPackageResourcePath());
            DexFile dex = new DexFile(ctx.getPackageResourcePath());
            Enumeration<String> entries = dex.entries();
            while (entries.hasMoreElements()) {
                String strClazz = entries.nextElement();
                if (strClazz.contains("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")){
                    // utils.log("Class-dump:"+strClazz);
                    try{
                        Class<?> entryClass =loader.loadClass(strClazz);
                        runnable.run(entryClass);
                    }catch (Exception e){
                        // e.printStackTrace();
                    }
                }

            }
        } catch (Exception e) {
            utils.log(e.toString());
        }

    }

    //查找需要hook的class
    private JSONObject findAllHookClass() {
        String key  = "version_"+(utils.getVerName()+utils.getVerCode()).hashCode();
        String data = utils.readData(key);
        if(!Objects.equals(data, "")){
            try{
                return JSONObject.parseObject(data);
            }catch (Exception e){
                utils.log(e.toString());
            }
        }

        Toast.makeText(utils.getContext(),"微信版本："+utils.getVerName()+"("+utils.getVerCode()+")未适配，尝试适配！",Toast.LENGTH_LONG).show();
        utils.log("微信版本："+utils.getVerName()+"("+utils.getVerCode()+")未适配，尝试适配！");
        attach();
        new Thread(){
            @Override
            public void run() {
                //新的线程适配
                JSONObject jsonObject = new JSONObject();
                // 查找timeout
                scan(utils.getContext(),utils.getClassLoader(), clz -> {
                    Method[] m = clz.getDeclaredMethods();

                    for (Method method : m) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1 && parameterTypes[0].getName().startsWith("com.tencent.mm.plugin.luckymoney.model.a")) {
                            //utils.log("Xposed---parameterTypes", parameterTypes[0].getName());
                            JSONArray jsonArray = new JSONArray();
                            jsonArray.add("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI");
                            jsonArray.add(method.getName());
                            jsonArray.add(parameterTypes[0].getName());
                            jsonObject.put("RedPackage", jsonArray);
                        }
                    }

                });

                runOnUiThread(() -> {
                    if(jsonObject.containsKey("RedPackage")){
                        Toast.makeText(utils.getContext(),"微信版本："+utils.getVerName()+"("+utils.getVerCode()+")适配成功！",Toast.LENGTH_LONG).show();
                        utils.log("微信版本："+utils.getVerName()+"("+utils.getVerCode()+")适配成功！");
                        utils.writeData(key,jsonObject.toString());
                        try {
                            Thread.sleep(2000);
                            utils.restart();
                        } catch (InterruptedException e) {
                            utils.log(e.toString());
                        }

                    }else{
                        Toast.makeText(utils.getContext(),"微信版本："+utils.getVerName()+"("+utils.getVerCode()+")适配失败！",Toast.LENGTH_LONG).show();
                        utils.log("微信版本："+utils.getVerName()+"("+utils.getVerCode()+")适配失败！");
                        Log.e("Xposed",jsonObject.toJSONString());
                    }
                });
            }
        }.start();

        return null;
    }


    @Override
    public void hookLoadPackage() throws ClassNotFoundException {
        JSONObject jsonObject = findAllHookClass();
/*
        if (utils.isDebug()) {
            try {
                OpenLog.init(utils);
            } catch (Throwable e) {
                utils.log("微信 Log HookError " + e.toString());
            }
        }*/
       /** try {
            LoginInfo.init(utils);
        } catch (Throwable e) {
            utils.log("微信 LoginInfo HookError " + e.toString());
        }**/
        try {
           //Setting.init(utils);
        } catch (Throwable e) {
            utils.log("微信 Settings HookError " + e.toString());
        }
        try {
            RedPackage.init(utils, jsonObject.getJSONArray("RedPackage"));
        } catch (Throwable e) {
            utils.log("微信 RedPackage HookError " + e.toString());
        }
        try {
            NickName.init(utils);
        } catch (Throwable e) {
            utils.log("微信 NickName HookError " + e.toString());
        }
        try {
            PayTools.init(utils);
        } catch (Throwable e) {
            utils.log("微信 PayTools HookError " + e.toString());
        }
        try {
            Msg.init(utils);
        } catch (Throwable e) {
            utils.log("微信 Msg HookError " + e.toString());
        }

    }



    @Override
    public String getPackPageName() {
        return "com.tencent.mm";
    }

    @Override
    public String getAppName() {
        return "微信";
    }

    @Override
    public boolean needHelpFindApplication() {
        return true;
    }
    @Override
    public int hookIndex() {
        return 1;
    }

}
