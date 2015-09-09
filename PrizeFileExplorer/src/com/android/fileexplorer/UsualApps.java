package com.android.fileexplorer;

import java.util.HashMap;

import android.content.Context;
import android.util.Log;

import com.mediatek.storage.StorageManagerEx;
public class UsualApps {
	private static final String outSD = StorageManagerEx.getExternalStoragePath();
	private static final String inSD = StorageManagerEx.getInternalStoragePath();
	private String inSD_end = "",outSD_end = "";
	private HashMap<String, String> usualApps;
	public HashMap<String, String> getUsualApps(Context context) {
		if (inSD != null)
		inSD_end = inSD.substring(inSD.lastIndexOf("/") + 1, inSD.length());
		if (inSD_end != null && !inSD_end.equals("sdcard0") && !inSD_end.equals("sdcard1")) {
			inSD_end = "sdcard0";
		}
		if (outSD != null)
		outSD_end = outSD.substring(outSD.lastIndexOf("/") + 1, outSD.length());
		if(usualApps == null){
			usualApps = new HashMap<String, String>();
			init(context);
		}
		return usualApps;
	}
	public void init(Context context) {
		usualApps.put(".thumbnails", "     （缩略图）");
		usualApps.put("Application", "     （应用程序）");
		usualApps.put("Bluetooth", "     （蓝牙）");
		usualApps.put("DCIM", "     （相册）");
		usualApps.put(inSD_end, context.getString(R.string.string_phone_capacity));
		usualApps.put(outSD_end, context.getString(R.string.string_phone_sd));
		usualApps.put("Download", "     （下载目录）");
		usualApps.put("msc", "     （相机相关）");
		usualApps.put("mtklog", "     （MTK日志）");
		usualApps.put("Music", "     （音乐）");
		usualApps.put("Musiclrc", "     （音乐歌词）");
		usualApps.put("Netdisk", "     （网盘）");
		usualApps.put("Notification", "     （通知）");
		usualApps.put("PhoneRecord", "     （通话录音）");
		usualApps.put("Recordings", "     （录音）");
		usualApps.put("record", "     （通话录音）");
		usualApps.put("Ringtones", "     （铃声）");
		usualApps.put("Screenshots", "     （截图）");
		usualApps.put("Sounds", "     （音频）");
		usualApps.put("tmp", "     （临时文件）");
		usualApps.put("DBank", "     （华为网盘）");
		usualApps.put("Playlists", "     （播放列表）");
		usualApps.put("360", "     （360软件）");
		usualApps.put(".360yunpan", "     （360云盘）");
		usualApps.put(".antutu", "     （安兔兔）");
		usualApps.put(".estrong", "     （ES文件浏览器）");
		usualApps.put(".ktv", "     （唱吧）");
		usualApps.put(".tmfs", "     （腾讯私密文件）");
		usualApps.put("115wangpan", "     （115网盘）");
		usualApps.put("alipay", "     （支付宝）");
		usualApps.put("antohomemain", "     （汽车之家）");
		usualApps.put("antonavi", "     （高德地图）");
		usualApps.put("baidu", "     （百度）");
		usualApps.put("BaiduMap", "     （百度地图）");
		usualApps.put("BaiduNetDisk", "     （百度云）");
		usualApps.put("baofeng", "     （暴风影音）");
		usualApps.put("Duokan", "     （多看阅读）");
		usualApps.put("DUOMI", "     （多米音乐）");
		usualApps.put("feiji", "     （全民飞机大战）");
		usualApps.put("filemaster", "     （文件全能王）");
		usualApps.put("handcent", "     （超级短信）");
		usualApps.put("iReader", "     （掌阅iReader）");
		usualApps.put("kbrowser", "     （猎豹浏览器）");
		usualApps.put("kingreader", "     （开卷有益）");
		usualApps.put("kugou", "     （酷狗音乐）");
		usualApps.put("LoveWallpaper", "     （爱壁纸）");
		usualApps.put("moji", "     （墨迹天气）");
		usualApps.put("mxbrowser", "     （遨游）");
		usualApps.put("mybook66", "     （云帆小说）");
		usualApps.put("powerword", "     （金山词霸）");
		usualApps.put("QieZi", "     （茄子快传）");
		usualApps.put("qqmusic", "     （QQ音乐）");
		usualApps.put("qqpim", "     （QQ同步助手）");
		usualApps.put("qqsecure", "     （腾讯管家）");
		usualApps.put("QQBrowser", "     （QQ浏览器）");
		usualApps.put("renren", "     （人人）");
		usualApps.put("shootme", "     （屏幕截图）");
		usualApps.put("sina", "     （新浪）");
		usualApps.put("sogou", "     （搜狗输入法）");
		usualApps.put("taobao", "     （淘宝）");
		usualApps.put("titaniumbackup", "     （钛备份）");
		usualApps.put("tunnybrowser", "     （海豚浏览器）");
		usualApps.put("tencent", "     （腾讯）");
		usualApps.put("ttpod", "     （天天动听）");
		usualApps.put("wandoujia", "     （豌豆荚）");
		usualApps.put("ZAKER", "     （ZAKER）");
		usualApps.put("zapya", "     （快牙）");
		usualApps.put("我的快盘", "     （金山快盘）");
		usualApps.put(".File_SafeBox", "     （保密柜）");
		usualApps.put("Android", "     （系统目录）");
		usualApps.put("backup", "     （系统备份）");
		usualApps.put("Picture", "     （图片）");
		usualApps.put("360contacts", "     （360通讯录）");
		usualApps.put("360Download", "     （360下载目录）");
		usualApps.put("360explorer", "     （360文件管理器）");
	    usualApps.put("UCDownloads", "     （UC浏览器）");
	}
}
