package cn.dreamn.qianji_auto.app;

import static cn.dreamn.qianji_auto.core.broadcast.AppBroadcast.BROADCAST_ASYNC;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hjq.toast.Toaster;
import com.tencent.mmkv.MMKV;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.dreamn.qianji_auto.R;
import cn.dreamn.qianji_auto.bills.BillInfo;
import cn.dreamn.qianji_auto.data.database.Db;
import cn.dreamn.qianji_auto.data.database.Helper.BookNames;
import cn.dreamn.qianji_auto.data.database.Table.Category;
import cn.dreamn.qianji_auto.setting.AppStatus;
import cn.dreamn.qianji_auto.ui.utils.HandlerUtil;
import cn.dreamn.qianji_auto.utils.runUtils.Log;
import cn.dreamn.qianji_auto.utils.runUtils.RootUtils;
import cn.dreamn.qianji_auto.utils.runUtils.TaskThread;
import cn.dreamn.qianji_auto.utils.runUtils.Tool;

public class Weimu implements IApp {
    private static Weimu wangc;
    static long time = 0;

    public static Weimu getInstance(){
        if(wangc==null)
            wangc=new Weimu();
        return wangc;
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
    public int getAppIcon() {
        return R.drawable.logo_qianji;
    }

    @Override
    public void sendToApp(Context context,BillInfo billInfo) {

        Handler mHandler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 0) {
                    time = System.currentTimeMillis();
                    RootUtils.exec(new String[]{"am force-stop com.weimu.remember.bookkeeping"});
                    Intent intent = new Intent();
                    intent.setClassName("com.weimu.remember.bookkeeping", "com.alipay.sdk.app.AlipayResultActivity");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Uri uri = Uri.parse(getQianJi(billInfo));
                    intent.setData(uri);
                    context.startActivity(intent);

                    //TODO 4.0新增：多币种记账支持，此处预留修改位。
                    //Toaster.show(String.format(context.getString(R.string.book_success), billInfo.getMoney()));
                }
            }
        };
        if (AppStatus.isXposed()) {
            mHandler.sendEmptyMessage(0);
        } else {
            delay(mHandler);
        }


    }

    private void delay(Handler handler) {
        long m = System.currentTimeMillis() - time;
        if (m < 3000) {
            Toaster.show("稍后为您记账！");
            TaskThread.onMain(m, () -> delay(handler));
        } else {
            handler.sendEmptyMessage(0);
        }
    }

    @Override
    public void asyncDataBefore(Context context, int type) {
        if (AppStatus.isXposed()) {
            Log.i("记得记账:自动记账同步", "同步开始");

            RootUtils.exec(new String[]{"am force-stop com.weimu.remember.bookkeeping"});
            //杀死其他应用
            //  Tool.stopApp(context,"com.weimu.remember.bookkeeping");
            Intent intent = new Intent();
            intent.setClassName("com.weimu.remember.bookkeeping", "com.weimu.remember.bookkeeping.MainActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("AutoSignal", type);
            MMKV mmkv = MMKV.defaultMMKV();
            mmkv.encode("AutoSignal", type);
            Log.i("记得记账:自动记账同步", "正在前往记得记账");
            context.startActivity(intent);
        } else {
            Toaster.show(context.getResources().getString(R.string.not_support).replaceAll("钱迹", "记得记账"));
        }
    }


    private void doAsync(Bundle extData) {
        String json = extData.getString("data");
        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONArray asset = jsonObject.getJSONArray("asset");
        JSONArray category = jsonObject.getJSONArray("category");
        JSONArray userBook = jsonObject.getJSONArray("userBook");
        android.util.Log.i("记得记账", "doAsync: "+userBook);
        List<String> userBookList = new ArrayList<>();

        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 1) {
                    Log.i("记得记账:doAsync", (String) msg.obj);
                } else {
                    Toaster.show(R.string.async_success);
                }

            }
        };

        if (asset == null || category == null || userBook == null) {
            Log.i("记得记账:doAsync", "记得记账数据信息无效");
            return;
        }

        TaskThread.onThread(() -> {
            Db.db.CategoryDao().clean();

            for (int i = 0; i < category.size(); i++) {
                //HandlerUtil.send(mHandler, "（" + (i + 1) + "/" + category.size() + "）正在处理【分类数据】", 1);
                JSONObject jsonObject1 = category.getJSONObject(i);
                String name = jsonObject1.getString("name");
                String icon = jsonObject1.getString("icon");
                String level = jsonObject1.getString("level");
                String type = jsonObject1.getString("type");
                String self_id = jsonObject1.getString("id");
                String parent_id = jsonObject1.getString("parent");
                String book_id = jsonObject1.getString("book_id");
                String sort = jsonObject1.getString("sort");

                if (self_id == null || self_id.equals("")) {
                    self_id = String.valueOf(System.currentTimeMillis());
                }
                if (sort == null || sort.equals("")) {
                    sort = "500";
                }
                String self = self_id;

                Category[] category1 = Db.db.CategoryDao().getByName(name, type, book_id);

                if (category1 != null && category1.length > 0) continue;

                Db.db.CategoryDao().add(name, icon, level, type, self, parent_id, book_id, sort);

            }

            Log.i("记得记账:doAsync", "分类数据处理完毕");

            //资产数据处理
            Db.db.AssetDao().clean();

            for (int i = 0; i < asset.size(); i++) {
                //HandlerUtil.send(mHandler, "（" + (i + 1) + "/" + asset.size() + "）正在处理【资产数据】", 1);
                JSONObject jsonObject1 = asset.getJSONObject(i);
                if (jsonObject1.getString("type").equals("5"))
                    continue;
                Db.db.AssetDao().add(jsonObject1.getString("name"), jsonObject1.getString("icon"), jsonObject1.getInteger("sort"), jsonObject1.getString("id"));
            }

            Log.i("记得记账:doAsync", "资产数据处理完毕");

            Db.db.BookNameDao().clean();
            for (int i = 0; i < userBook.size(); i++) {
                //HandlerUtil.send(mHandler, "（" + (i + 1) + "/" + userBook.size() + "）正在处理【账本数据】", 1);
                JSONObject jsonObject1 = userBook.getJSONObject(i);
                String bookName = jsonObject1.getString("name");
                userBookList.add(bookName);
                String icon = jsonObject1.getString("cover");
                String bid = jsonObject1.getString("id");
                if (bid == null || bid.equals("")) {
                    bid = String.valueOf(System.currentTimeMillis());
                }
                Db.db.BookNameDao().add(bookName, icon, bid);
            }

            Log.i("记得记账:doAsync", "账本数据处理完毕");

            if (!userBookList.contains(BookNames.getDefault())) {
                BookNames.change(userBookList.get(0));
                Log.i("记得记账:doAsync", "默认账本未找到替换成 -> " + userBookList.get(0));
            }

            HandlerUtil.send(mHandler, 0);
        });


    }

    @Override
    public void asyncDataAfter(Context context, Bundle extData, int type) {
        // Toaster.show("收到记得记账数据！正在后台同步中...");
        if (type == BROADCAST_ASYNC) {
            doAsync(extData);
        }
        RootUtils.exec(new String[]{"am force-stop com.weimu.remember.bookkeeping"});

    }


    public String getQianJi(BillInfo billInfo) {

        String url = "qianji://publicapi/addbill?&type=" + billInfo.getType(true) + "&money=" + billInfo.getMoney();
        MMKV mmkv = MMKV.defaultMMKV();
        if (billInfo.getRemark() != null) {
            url += "&remark=" + billInfo.getRemark();
        }
        if (billInfo.getReimbursement() && billInfo.getType().equals(BillInfo.TYPE_INCOME)) {
            url += "&data=" + new String(Base64.encode(billInfo.getExtraData().getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE));
            if (billInfo.getAccountId1() != null)
                url += "&accountnameId=" + billInfo.getAccountId1();
            // String data = new String(Base64.decode(uri.getQueryParameter("data"),Base64.URL_SAFE));
        }

        //显示钱迹记账结果
        if (mmkv.getBoolean("show_qianji_result", true)) {
            url += "&showresult=1";
        } else {
            url += "&showresult=0";
        }

        //懒人模式，自动分类
        if (mmkv.getBoolean("lazy_mode", true)) {
            if (!mmkv.getBoolean("need_cate", true)) {
                return url + "&catechoose=0";
            } else {
                return url + "&catechoose=1";
            }

        }

        if (billInfo.getTime() != null) {
            url += "&time=" + billInfo.getTime();
        }

        if (billInfo.getCateName() != null) {
            url += "&catename=" + billInfo.getCateName();
        }
        url += "&catechoose=" + billInfo.getCateChoose();

        url += "&catetheme=auto";

        if (billInfo.getBookName()!= null && !billInfo.getBookName().equals("默认账本")) {
            url += "&bookname=" + billInfo.getBookName();
        }

        if (billInfo.getAccountName() != null && !billInfo.getAccountName().equals("") && !billInfo.getAccountName().equals("无账户")) {
            url += "&accountname=" + billInfo.getAccountName();
        }
        if (billInfo.getAccountName2() != null && !billInfo.getAccountName2().equals("") && !billInfo.getAccountName2().equals("无账户") && Arrays.asList("2", "3").contains(billInfo.getType(true))) {
            url += "&accountname2=" + billInfo.getAccountName2();
        } else {
            url += "&accountname2=";
        }
        if (billInfo.getFee() != null && !billInfo.getFee().equals("") && !billInfo.getFee().equals("0")) {
            url += "&fee=" + billInfo.getFee();
        } else {
            url += "&fee=0";
        }
        Log.i("记得记账:getQianJi", "记得记账URL:" + url);
        return url;
    }


    @Override
    public String getAsyncDesc(Context context) {
        if (AppStatus.xposedActive(context)) {
            return context.getResources().getString(R.string.qianji_async_desc).replaceAll("钱迹", "记得记账");
        }
        return context.getResources().getString(R.string.qianji_async_no_support).replaceAll("钱迹", "记得记账");
    }
}
