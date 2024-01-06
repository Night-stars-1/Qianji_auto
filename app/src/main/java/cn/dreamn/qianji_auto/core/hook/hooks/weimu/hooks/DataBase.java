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

package cn.dreamn.qianji_auto.core.hook.hooks.weimu.hooks;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tencent.mmkv.MMKV;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import cn.dreamn.qianji_auto.bills.BillInfo;
import cn.dreamn.qianji_auto.core.broadcast.AppBroadcast;
import cn.dreamn.qianji_auto.core.hook.Utils;
import cn.dreamn.qianji_auto.core.hook.hooks.weimu.DBHelper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class DataBase {

    public static void init(Utils utils) {
        utils.log("自动记账同步：记得记账初始化", false);
        ClassLoader mAppClassLoader = utils.getClassLoader();

        final DBHelper[] dbHelper = new DBHelper[1];
        utils.log("使用文件模式获取数据库对象", false);
        dbHelper[0] = new DBHelper(utils);

        final boolean[] hooked = {false};
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {

            @SuppressLint("Range")
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (hooked[0]) return;
            hooked[0] = true;
            Activity activity = (Activity) param.thisObject;
            final String activityClzName = activity.getClass().getName();
            if (activityClzName.contains("com.weimu.remember.bookkeeping.MainActivity")) {
                Intent intent = (Intent) XposedHelpers.callMethod(activity, "getIntent");
                if (intent != null) {
                    int AutoSignal = intent.getIntExtra("AutoSignal", AppBroadcast.BROADCAST_NOTHING);
                    if (AutoSignal == AppBroadcast.BROADCAST_ASYNC) {
                        utils.log("自动记账同步：记得记账开始同步", false);
                        JSONObject jsonObject = new JSONObject();
                        JSONArray userBooks = dbHelper[0].getUserBook();
                        utils.log(userBooks.toJSONString(), false);
                        JSONArray categorys = new JSONArray();
                        JSONArray asset = new JSONArray();
                        for (int i = 0; i < userBooks.size(); i++) {
                            asset.addAll(dbHelper[0].getAsset());
                            categorys.addAll(dbHelper[0].getCategory());
                        }
                        jsonObject.put("asset", asset);
                        jsonObject.put("category", categorys);
                        jsonObject.put("userBook", userBooks);
                        jsonObject.put("AutoSignal", AutoSignal);
                        utils.log(jsonObject.toJSONString(), false);
                        utils.send2auto(jsonObject.toJSONString());
                        Toast.makeText(utils.getContext(), "记得记账数据信息获取完毕，现在返回自动记账。", Toast.LENGTH_LONG).show();
                        XposedHelpers.callMethod(activity, "finishAndRemoveTask");
                    }
                }
            }
            }
        });

        // hook 一个无界面的Activity做记账接口
        XposedHelpers.findAndHookMethod("com.alipay.sdk.app.AlipayResultActivity", mAppClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean status;
                if (hooked[0]) return;
                hooked[0] = true;
                Activity activity = (Activity) param.thisObject;
                Intent intent = (Intent) XposedHelpers.callMethod(activity, "getIntent");
                if (intent != null) {
                    String path = intent.getData().getPath();
                    Uri Data = intent.getData();

                    String type = Data.getQueryParameter("type"); // 账单类型，0支出，1收入，2转账，3信用卡还款（非债务的还款），5 报销
                    String money = Data.getQueryParameter("money"); // 账单金额
                    String remark = Data.getQueryParameter("remark"); // 账单备注信息
                    String data = Data.getQueryParameter("data"); //
                    String time = Data.getQueryParameter("time"); // 账单时间
                    String cateName = Data.getQueryParameter("catename"); // 账单分类
                    String cateChoose = Data.getQueryParameter("catechoose"); // 弹出选择分类面板
                    String cateTheme = Data.getQueryParameter("catetheme"); // 选择分类面板的主题样式
                    String bookName = Data.getQueryParameter("bookname"); // 账单所属账本名称
                    String accountName = Data.getQueryParameter("accountname"); // 账单所属资产名称(或转账的转出账户）
                    String accountName2 = Data.getQueryParameter("accountname2"); // 转账或者还款的转入账户
                    String fee = Data.getQueryParameter("fee"); // 转账或者信用卡还款的手续费
                    if (path.equals("/addbill")) {
                        switch (type) {
                            case "0":
                                utils.log("金额: "+money, false);
                                utils.log("账单分类: "+cateName, false);
                                utils.log("备注信息: "+remark, false);
                                utils.log("账单时间: "+time, false);
                                utils.log("账本名称: "+bookName, false);
                                utils.log("资产名称: "+accountName2, false);
                                status = dbHelper[0].addBill(money, remark, time, cateName, bookName, accountName, accountName2, fee, 8);
                                if (status) {
                                    Toast.makeText(utils.getContext(), "支出记账成功", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(utils.getContext(), "支出记账失败", Toast.LENGTH_LONG).show();
                                }
                                break;
                            case "1":
                                /**
                                utils.log("金额: "+money, false);
                                utils.log("账单备注信息: "+remark, false);
                                utils.log("账单时间: "+time, false);
                                utils.log("账本名称: "+bookName, false);
                                utils.log("资产名称: "+accountName, false);
                                 */
                                status = dbHelper[0].addBill(money, remark, time, cateName, bookName, accountName, accountName2, fee, 9);
                                if (status) {
                                    Toast.makeText(utils.getContext(), "收入记账成功", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(utils.getContext(), "收入记账失败", Toast.LENGTH_LONG).show();
                                }
                                break;
                            case "2":
                                /**
                                utils.log("金额: "+money, false);
                                utils.log("账单备注信息: "+remark, false);
                                utils.log("账单时间: "+time, false);
                                utils.log("账本名称: "+bookName, false);
                                utils.log("转出账户: "+accountName, false);
                                utils.log("转入账户: "+accountName2, false);
                                utils.log("手续费: "+fee, false);
                                 */
                                status = dbHelper[0].addBill(money, remark, time, cateName, bookName, accountName, accountName2, fee, 2);
                                if (status) {
                                    Toast.makeText(utils.getContext(), "转账记账成功", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(utils.getContext(), "转账记账失败", Toast.LENGTH_LONG).show();
                                }
                                break;
                            case "3":
                                utils.log("金额: "+money, false);
                                utils.log("账单备注信息: "+remark, false);
                                utils.log("账单时间: "+time, false);
                                utils.log("账本名称: "+bookName, false);
                                utils.log("转出账户: "+accountName, false);
                                utils.log("手续费: "+fee, false);
                                Toast.makeText(utils.getContext(), "暂未支持信用还款", Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                }
            }
        });

    }

}
