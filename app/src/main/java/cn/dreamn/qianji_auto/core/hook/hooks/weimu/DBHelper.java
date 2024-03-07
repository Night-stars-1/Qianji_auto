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

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import cn.dreamn.qianji_auto.core.hook.Utils;

public class DBHelper {

    private final Utils utils;
    boolean isObj = false;
    //SQLiteDatabase
    private SQLiteDatabase db;
    private String UserId;

    @SuppressLint("SdCardPath")
    public DBHelper(Utils utils) {
        this.utils = utils;
        openDb();
    }

    public DBHelper(SQLiteDatabase db, Utils utils) {
        this.db = db;
        this.utils = utils;
        isObj = true;
    }

    public SQLiteDatabase getDb() {
        if (db == null || !db.isOpen()) {
            openDb();
        }
        return db;
    }

    public String getUserId() {
        return UserId;
    }

    private void openDb() {
        isObj = false;
        utils.log("Qianji-Copy 开始读取配置文件", false);
        String FilesDir = utils.getContext().getFilesDir().getPath();
        String userInfoJson = readJsonFromAsset( FilesDir + "/user_info.json");
        JSONObject userInfoObject = JSONObject.parseObject(userInfoJson);
        UserId = userInfoObject.getString("id");
        this.db = SQLiteDatabase.openOrCreateDatabase(FilesDir + "/user/"+ UserId +"/data.db", null);
    }

    @SuppressLint("Range")
    public String getAllTables() {
        if (!db.isOpen()) {
            openDb();
        }
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;", null);
        StringBuilder str = new StringBuilder();
        while (cursor.moveToNext()) {
            String str1 = cursor.getString(cursor.getColumnIndex("name"));
            Cursor cursor2 = db.rawQuery(" PRAGMA TABLE_INFO (" + str1 + ")", null);
            StringBuilder str2 = new StringBuilder();
            while (cursor2.moveToNext()) {
                str2.append(" ").append(cursor.getString(cursor.getColumnIndex("name")));
            }
            cursor2.close();
            str.append("[").append(str1).append("](").append(str2).append(")\n");
        }
        cursor.close();
        return str.toString();
    }

    /**
     * 获取分类信息
     *
     * @return 分类信息
     */
    @SuppressLint("Range")
    public JSONArray getCategory() {
        if (!db.isOpen()) {
            openDb();
        }
        Cursor Cursor = db.rawQuery("select * from category", null);
        utils.log("父级分类信息数量: " + Cursor.getCount(), false);
        JSONArray jsonArray = new JSONArray();
        while (Cursor.moveToNext()) {
            String parent_id = Cursor.getString(Cursor.getColumnIndex("parent_id"));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", Cursor.getString(Cursor.getColumnIndex("name"))); // 分类名称
            jsonObject.put("type", Cursor.getInt(Cursor.getColumnIndex("type"))==1?1:0); // 类型->0:支出 1:收入
            jsonObject.put("id", Cursor.getString(Cursor.getColumnIndex("id"))); // 分类id
            jsonObject.put("book_id", Cursor.getString(Cursor.getColumnIndex("book_id"))); // 账本id
            jsonObject.put("parent", parent_id); // 父级id
            jsonObject.put("level", parent_id.isEmpty()?"1":"2"); // 层级->1:父级 2:子级
            jsonObject.put("icon", Cursor.getString(Cursor.getColumnIndex("icon"))); // 图标
            jsonObject.put("sort", "0"); // 金额
            utils.log(jsonObject.toJSONString(), false);
            jsonArray.add(jsonObject);
        }
        Cursor.close();
        return jsonArray;
    }

    /**
     * 获取资产信息
     *
     * @return 资产信息
     */
    @SuppressLint("Range")
    public JSONArray getAsset() {
        if (!db.isOpen()) {
            openDb();
        }
        Cursor cursor = db.rawQuery("select * from capital_basic", null);
        utils.log("资产信息数量: " + cursor.getCount(), false);
        JSONArray jsonArray = new JSONArray();
        while (cursor.moveToNext()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sort", cursor.getInt(cursor.getColumnIndex("init_balance"))/10); // 金额
            jsonObject.put("id", cursor.getString(cursor.getColumnIndex("id"))); // 资产id
            jsonObject.put("name", cursor.getString(cursor.getColumnIndex("name"))); // 资产名称
            jsonObject.put("icon", cursor.getString(cursor.getColumnIndex("icon"))); // 图标
            jsonObject.put("type", "0"); // 未知
            jsonObject.put("info", "1"); // 未知
            jsonArray.add(jsonObject);
        }
        cursor.close();
        return jsonArray;
    }

    @SuppressLint("Range")
    public JSONArray getUserBook() {
        if (!db.isOpen()) {
            openDb();
        }
        Cursor cursor = db.rawQuery("select * from book", null);
        JSONArray jsonArray = new JSONArray();
        boolean has = false;
        while (cursor.moveToNext()) {
            has = true;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", cursor.getString(cursor.getColumnIndex("id")));
            jsonObject.put("name", cursor.getString(cursor.getColumnIndex("name")));
            jsonObject.put("userId", UserId);
            jsonObject.put("cover", "http://res.qianjiapp.com/headerimages2/maarten-van-den-heuvel-7RyfX2BHoXU-unsplash.jpg!headerimages2");
            jsonArray.add(jsonObject);
        }
        cursor.close();
        if (!has) {//非vip并且没有默认账本会自动附加
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", "-1");
            jsonObject.put("userId", UserId);
            jsonObject.put("name", "日常账本");
            jsonObject.put("cover", "http://res.qianjiapp.com/headerimages2/maarten-van-den-heuvel-7RyfX2BHoXU-unsplash.jpg!headerimages2");
            jsonArray.add(jsonObject);
        }

        return jsonArray;
    }

    @SuppressLint("Range")
    public JSONArray getBills(String type, String UserId) {
        if (!db.isOpen()) {
            openDb();
        }
        Cursor cursor = db.rawQuery("select * from user_bill where TYPE='" + type + "' and USERID='" + UserId + "' and EXTRA IS NULL order by createtime desc", null);
        JSONArray jsonArray = new JSONArray();
        while (cursor.moveToNext()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", cursor.getString(cursor.getColumnIndex("billid")));
            jsonObject.put("remark", cursor.getString(cursor.getColumnIndex("REMARK")));
            jsonObject.put("money", cursor.getString(cursor.getColumnIndex("MONEY")));
            jsonObject.put("descinfo", cursor.getString(cursor.getColumnIndex("DESCINFO")));
            jsonObject.put("createtime", cursor.getString(cursor.getColumnIndex("createtime")));
            jsonObject.put("type", cursor.getString(cursor.getColumnIndex("TYPE")));
            jsonArray.add(jsonObject);
        }
        cursor.close();
        return jsonArray;
    }


    @Override
    public void finalize() {
        try {
            super.finalize();
            if (db != null && db.isOpen() && !isObj) {
                db.close();
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private String readJsonFromAsset(String filePath) {
        String json = "";
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            json = new String(data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            utils.log(e.toString());
            //e.printStackTrace();
        }

        return json;
    }

    private String getBookId(String bookName) {
        if (!db.isOpen()) {
            openDb();
        }
        Cursor cursor = db.rawQuery("select * from book where name= ? LIMIT 1", new String[]{bookName});
        String bookId = "";
        while (cursor.moveToNext()) {
            bookId = cursor.getString(cursor.getColumnIndex("id"));
        }
        cursor.close();
        if (bookId.isEmpty()) {
            String FilesDir = utils.getContext().getFilesDir().getPath();
            String userInfoJson = readJsonFromAsset( FilesDir + "/user/" + UserId + "/userSettings.json");
            JSONObject userInfoObject = JSONObject.parseObject(userInfoJson);
            bookId = userInfoObject.getString("bookid");
        }
        return bookId;
    }

    private String getCategoryId(String cateName, int type) {
        String categoryId = "";
        if (!db.isOpen()) {
            openDb();
        }
        // type 1:收入 2:支出
        Cursor cursor = db.rawQuery("SELECT * FROM category WHERE (name = ? OR name = ?) AND type = ? LIMIT 1", new String[]{cateName, "其它", String.valueOf(type)});
        while (cursor.moveToNext()) {
            categoryId = cursor.getString(cursor.getColumnIndex("id"));
        }
        utils.log("分类id: " + categoryId, false);
        cursor.close();
        if (type == 0) {
            categoryId = "";
        }
        return categoryId;
    }

    /**
     * 获取资产信息
     *
     * @return 资产信息
     */
    @SuppressLint("Range")
    public String getAssetId(String accountName) {
        String assetId = "";
        if (!db.isOpen()) {
            openDb();
        }
        Cursor cursor = db.rawQuery("select * from capital_basic where name = ? LIMIT 1", new String[]{accountName});
        while (cursor.moveToNext()) {
            assetId = cursor.getString(cursor.getColumnIndex("id")); // 金额
        }
        cursor.close();
        return assetId;
    }

    private static String generateRandomString(int length) {
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }

        return sb.toString();
    }

    public boolean addBill(String money, String remark, String time, String cateName, String bookName, String accountName, String accountName2, String fee, Integer type) {
        boolean status;
        if (!db.isOpen()) {
            openDb();
        }
        db.beginTransaction();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            Date date = sdf.parse(time);
            long timestamp = 0;
            if (date != null) {
                timestamp = date.getTime();
            }
            // 使用 ContentValues 存储数据
            ContentValues values = new ContentValues();
            values.put("id", generateRandomString(32));
            values.put("type", type); // 8:支出 9:收入
            values.put("money", Double.parseDouble(money)*100);
            values.put("book_id", getBookId(bookName)); // 账本id
            values.put("category_id", getCategoryId(cateName, type==8?2:type==9?1:0)); // 分类id
            values.put("to_capital_id", type == 9 ? getAssetId(accountName):getAssetId(accountName2)); // 账户id
            values.put("from_capital_id", type == 9? getAssetId(accountName2):getAssetId(accountName)); // 账户id
            values.put("fee", (int) Double.parseDouble(fee)*100);
            //values.put("seq_id", "seq_id");
            values.put("sync_status", "1");
            values.put("deleted", "0");
            values.put("created_at", timestamp);
            values.put("updated_at", timestamp);
            values.put("remark", remark);
            values.put("time", timestamp);
            values.put("end_time", "0");
            values.put("transaction_status", "2");
            values.put("record_way", "0");
            values.put("excluded", "0");
            values.put("unique_no", "");
            values.put("source", "0");
            utils.log(values.toString(), false);
            // 插入数据
            db.insert("transaction_basic", null, values);

            // 标记事务成功
            db.setTransactionSuccessful();
        } catch (ParseException e) {
            utils.log("插入数据失败"+e, false);
            throw new RuntimeException(e);
        } finally {
            status = db.inTransaction();
            // 结束事务
            db.endTransaction();
            // 关闭数据库连接
            db.close();
        }
        return status;
    }

}