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

package cn.dreamn.qianji_auto.core.hook.hooks.weimu;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Objects;

import cn.dreamn.qianji_auto.core.hook.core.hookBase;
import cn.dreamn.qianji_auto.core.hook.hooks.weimu.hooks.DataBase;
import dalvik.system.DexFile;

public class Weimu extends hookBase {
    static final hookBase self = new Weimu();
    public static hookBase getInstance() {
        return self;
    }


    private Handler mHandler;
    private Thread mUiThread;

    private Boolean not_find = true;

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
    private void scan(Context ctx,ClassLoader loader,RunBody runnable) {
        try {
          //  utils.log("Class-dump-dex:"+ctx.getPackageResourcePath());
            DexFile dex = new DexFile(ctx.getPackageResourcePath());
            Enumeration<String> entries = dex.entries();
            while (entries.hasMoreElements() && not_find) {
                String strClazz = entries.nextElement();
                if (strClazz.startsWith("kylec.me")){
                    //utils.log("Class-dump:"+strClazz);
                    try{
                        Class<?> entryClass =loader.loadClass(strClazz);
                        runnable.run(entryClass);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    //查找需要hook的class
    private JSONObject findAllHookClass() throws ClassNotFoundException {
        String key  = "version_"+(utils.getVerName()+utils.getVerCode()).hashCode();
        String data = utils.readData(key);
        if(!Objects.equals(data, "")){
            try{
               return JSONObject.parseObject(data);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        Toast.makeText(utils.getContext(),"一羽记账版本："+utils.getVerName()+"("+utils.getVerCode()+")未适配，尝试适配！",Toast.LENGTH_LONG).show();
        utils.log("一羽记账版本："+utils.getVerName()+"("+utils.getVerCode()+")未适配，尝试适配！");
        attach();
        new Thread(){
            @Override
            public void run() {
                //新的线程适配
                JSONObject jsonObject = new JSONObject();
                // 查找timeout
                scan(utils.getContext(),utils.getClassLoader(), clz -> {
                    //遍历每一个class
                    if(clz.getName().equals("kylec.me.base.core.YiYuApp")) {
                        not_find = false;
                        // utils.log("Xposed---class" + clz);
                        Method[] m = clz.getDeclaredMethods();
                        // 打印获取到的所有的类方法的信息
                        // utils.log("XPosed" + Arrays.toString(m));
                        for (Method method : m) {
                            
                            if (method.getReturnType().getName().equals("java.lang.String")) {
                                //找到DataBasePath
                                JSONArray jsonArray = new JSONArray();
                                jsonArray.add("kylec.me.base.core.YiYuApp");
                                jsonArray.add(method.getName());
                                jsonObject.put("DataBasePath", jsonArray);
                            }
                        }
                    }

                });

                runOnUiThread(() -> {
                    if(jsonObject.containsKey("DataBasePath")){
                        Toast.makeText(utils.getContext(),"一羽记账版本："+utils.getVerName()+"("+utils.getVerCode()+")适配成功！",Toast.LENGTH_LONG).show();
                        utils.log("一羽记账版本："+utils.getVerName()+"("+utils.getVerCode()+")适配成功！");
                        utils.writeData(key,jsonObject.toString());
                        try {
                            Thread.sleep(2000);
                            utils.restart();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }else{
                        Toast.makeText(utils.getContext(),"一羽记账版本："+utils.getVerName()+"("+utils.getVerCode()+")适配失败！",Toast.LENGTH_LONG).show();
                        utils.log("一羽记账版本："+utils.getVerName()+"("+utils.getVerCode()+")适配失败！");
                        Log.e("Xposed",jsonObject.toJSONString());
                    }
                });
            }
        }.start();

return null;

    }
    
    @Override
    public void hookLoadPackage() throws ClassNotFoundException {
        try {
            DataBase.init(utils);
        } catch (Throwable e) {
            utils.log("记得记账 DataBase HookError " + e.toString());
        }
    }



    @Override
    public String getPackPageName() {
        return "com.weimu.remember.bookkeeping";
    }

    @Override
    public String getAppName() {
        return "记得记账";
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
