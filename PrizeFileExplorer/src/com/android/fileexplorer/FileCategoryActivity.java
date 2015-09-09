
package com.android.fileexplorer;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.android.fileexplorer.FavoriteDatabaseHelper.FavoriteDatabaseListener;
import com.android.fileexplorer.FileCategoryHelper.CategoryInfo;
import com.android.fileexplorer.FileCategoryHelper.FileCategory;
import com.android.fileexplorer.FileExplorerTabActivity.IBackPressedListener;
import com.android.fileexplorer.FileViewInteractionHub.Mode;
import com.android.fileexplorer.Util.OutSDCardInfo;
import com.android.fileexplorer.Util.SDCardInfo;
import com.mediatek.storage.StorageManagerEx;
public class FileCategoryActivity extends Fragment implements IFileInteractionListener,
FavoriteDatabaseListener, IBackPressedListener {

	public static final String EXT_FILETER_KEY = "ext_filter";

	private static final String LOG_TAG = "FileCategoryActivity";

	private static HashMap<Integer, FileCategory> button2Category = new HashMap<Integer, FileCategory>();

	private HashMap<FileCategory, Integer> categoryIndex = new HashMap<FileCategory, Integer>();

	private FileListCursorAdapter mAdapter;

	private FileViewInteractionHub mFileViewInteractionHub;

	private FileCategoryHelper mFileCagetoryHelper;

	private FileIconHelper mFileIconHelper;

	private CategoryBar multiCategoryBar; //add memory storage Bar

	private CategoryBarSmall mCategoryBar, mSDCategoryBar;

	private ScannerReceiver mScannerReceiver;

	private FavoriteList mFavoriteList;

	private ViewPage curViewPage = ViewPage.Invalid;

	private ViewPage preViewPage = ViewPage.Invalid;

	private Activity mActivity;

	private View mRootView;

	private FileViewActivity mFileViewActivity;

	private ListView fileListView;

	private boolean mConfigurationChanged = false;

	private LinearLayout outSDField;
	public void setConfigurationChanged(boolean changed) {
		mConfigurationChanged = changed;
	}

	static {
		button2Category.put(R.id.category_music, FileCategory.Music);
		button2Category.put(R.id.category_video, FileCategory.Video);
		button2Category.put(R.id.category_picture, FileCategory.Picture);
		//        button2Category.put(R.id.category_theme, FileCategory.Theme);
		button2Category.put(R.id.category_document, FileCategory.Doc);
		//        button2Category.put(R.id.category_zip, FileCategory.Zip);
		button2Category.put(R.id.category_apk, FileCategory.Apk);
		button2Category.put(R.id.category_favorite, FileCategory.Favorite);
	}
	@Override
	public void onResume() {
		super.onResume();
		if (!Util.isInPaste) {
			updateUI();
		}
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mActivity = getActivity();
		mFileViewActivity = (FileViewActivity) ((FileExplorerTabActivity) mActivity)
				.getFragment(Util.SDCARD_TAB_INDEX);
		mRootView = inflater.inflate(R.layout.file_explorer_category, container, false);
		curViewPage = ViewPage.Invalid;
		mFileViewInteractionHub = new FileViewInteractionHub(this);
		mFileViewInteractionHub.setMode(Mode.View);
		mFileViewInteractionHub.setRootPath("/");
		mFileIconHelper = new FileIconHelper(mActivity);
		mFavoriteList = new FavoriteList(mActivity, (ListView) mRootView.findViewById(R.id.favorite_list), this, mFileIconHelper);
		mFavoriteList.initList();
		mAdapter = new FileListCursorAdapter(mActivity, null, mFileViewInteractionHub, mFileIconHelper);
		outSDField = (LinearLayout) mRootView.findViewById(R.id.phone_sd_line);
		fileListView = (ListView) mRootView.findViewById(R.id.file_path_list);
		fileListView.setAdapter(mAdapter);
		setupCategoryInfo();
		setupClick();
		registerScannerReceiver();
		return mRootView;
	}

	private void registerScannerReceiver() {
		mScannerReceiver = new ScannerReceiver();
		IntentFilter intentFilter = new IntentFilter();
		//        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		//        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		//        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		//        intentFilter.addAction("com.test.broadcast");
		intentFilter.addAction("refresh");
		//        intentFilter.addDataScheme("file");
		mActivity.registerReceiver(mScannerReceiver, intentFilter);
	}

	private void setupCategoryInfo() {
		mFileCagetoryHelper = new FileCategoryHelper(mActivity);

		mCategoryBar = (CategoryBarSmall) mRootView.findViewById(R.id.category_bar);
		mSDCategoryBar = (CategoryBarSmall) mRootView.findViewById(R.id.sd_category_bar);
		/*PRIZE-delete theme and zip items-hekeyi-2015-4-1-start*/
		/*int[] imgs = new int[] {
                R.drawable.category_bar_music, R.drawable.category_bar_video,
                R.drawable.category_bar_picture, R.drawable.category_bar_theme,
                R.drawable.category_bar_document, R.drawable.category_bar_zip,
                R.drawable.category_bar_apk, R.drawable.category_bar_other
        };*/

		int[] imgscount = new int[] {R.drawable.category_bar_occupy};
		int[] imgs = new int[]{
				R.drawable.category_bar_music, R.drawable.category_bar_video,
				R.drawable.category_bar_picture,R.drawable.category_bar_apk,
				R.drawable.category_bar_document,R.drawable.category_bar_other
		};
		/*for (int i = 0; i < imgs.length; i++) {
            mCategoryBar.addCategory(imgs[i]);
        }*/
		/*PRIZE-delete theme and zip items-hekeyi-2015-4-1-end*/
		for (int i = 0; i < imgscount.length; i++) {
			mCategoryBar.addCategory(imgscount[i]);
			mSDCategoryBar.addCategory(imgscount[i]);

		}

		multiCategoryBar =(CategoryBar)mRootView.findViewById(R.id.multi_category_bar);
		for (int i = 0; i < imgs.length; i++) {
			multiCategoryBar.addCategory(imgs[i]);
		}


		for (int i = 0; i < FileCategoryHelper.sCategories.length; i++) {
			categoryIndex.put(FileCategoryHelper.sCategories[i], i);
		}

	}



	/**
	 * 方法描述：refresh the CategoryBar
	 * @param 参数名 说明
	 * @return 返回类型 说明
	 * @see 类名/完整类名/完整类名#方法名
	 * @author hekeyi
	 */
	public void refreshCategoryInfo() {
		SDCardInfo sdCardInfo = Util.getSDCardInfo();
		if (sdCardInfo != null) {
			mCategoryBar.setFullValue(sdCardInfo.total);
			//setTextView(R.id.sd_card_capacity, getString(R.string.sd_card_size, Util.convertStorage(sdCardInfo.total)));
			setTextView(R.id.sd_card_available, Util.convertStorage(sdCardInfo.total - sdCardInfo.free) + "/"
					+ Util.convertStorage(sdCardInfo.total));            
			multiCategoryBar.setFullValue(sdCardInfo.total);
			setTotalBarValue(sdCardInfo.total - sdCardInfo.free);
		}
		OutSDCardInfo outSDCardInfo = Util.getOutSDCardInfo();
		View divider = mRootView.findViewById(R.id.phone_sd_line_divider);
		divider.setVisibility(View.GONE);
		outSDField.setVisibility(View.GONE);
		if (outSDCardInfo != null) {
			outSDField.setVisibility(View.VISIBLE);
			divider.setVisibility(View.VISIBLE);
			mSDCategoryBar.setFullValue(outSDCardInfo.total);
			//setTextView(R.id.out_sd_card_available, getString(R.string.sd_card_available, Util.convertStorage(outSDCardInfo.free)));            
			setTextView(R.id.out_sd_card_available,Util.convertStorage(outSDCardInfo.total - outSDCardInfo.free) + "/" 
					+ Util.convertStorage(outSDCardInfo.total));      
			multiCategoryBar.setFullValue(sdCardInfo.total + outSDCardInfo.total);
			setSDTotalBarValue(outSDCardInfo.total - outSDCardInfo.free);
		}
		mFileCagetoryHelper.refreshCategoryInfo();

		// the other category size should include those files didn't get scanned.
		long size = 0;
		for (FileCategory fc : FileCategoryHelper.sCategories) {
			CategoryInfo categoryInfo = mFileCagetoryHelper.getCategoryInfos().get(fc);
			setCategoryCount(fc, categoryInfo.count);

			// other category size should be set separately with calibration
			if(fc == FileCategory.Other)
				continue;

			setCategorySize(fc, categoryInfo.size);
			setCategoryBarValue(fc, categoryInfo.size);
			size += categoryInfo.size;
		}



		if (sdCardInfo != null) {
			long otherSize;
			if (outSDCardInfo != null) {
				otherSize = sdCardInfo.total + outSDCardInfo.total - outSDCardInfo.free - sdCardInfo.free - size;
			} else {
				otherSize = sdCardInfo.total - sdCardInfo.free - size;
			}           
			setCategorySize(FileCategory.Other, otherSize);
			setCategoryBarValue(FileCategory.Other, otherSize);
		}

		setCategoryCount(FileCategory.Favorite, mFavoriteList.getCount());
		if (mSDCategoryBar.getVisibility() == View.VISIBLE) {
			mSDCategoryBar.startAnimation();
		}
		if (mCategoryBar.getVisibility() == View.VISIBLE) {
			mCategoryBar.startAnimation();
		}
		if(multiCategoryBar.getVisibility() == View.VISIBLE) {
			multiCategoryBar.startAnimation();
		}

	}

	public enum ViewPage {
		Home, Favorite, Category, NoSD, Invalid
	}

	private void showPage(ViewPage p) {
		if (curViewPage == p) return;

		curViewPage = p;

		showView(R.id.file_path_list, false);
		showView(R.id.navigation_bar, false);
		showView(R.id.category_page, false);
		showView(R.id.operation_bar, false);
		showView(R.id.sd_not_available_page, false);
		mFavoriteList.show(false);
		showEmptyView(false);

		switch (p) {
		case Home:
			showView(R.id.category_page, true);
			if (mConfigurationChanged) {
				((FileExplorerTabActivity) mActivity).reInstantiateCategoryTab();
				mConfigurationChanged = false;
			}
			break;
		case Favorite:
			showView(R.id.navigation_bar, true);
			mFavoriteList.show(true);
			showEmptyView(mFavoriteList.getCount() == 0);
			break;
		case Category:
			showView(R.id.navigation_bar, true);
			showView(R.id.file_path_list, true);
			showEmptyView(mAdapter.getCount() == 0);
			break;
		case NoSD:
			showView(R.id.sd_not_available_page, true);
			break;
		}
	}

	private void showEmptyView(boolean show) {
		View emptyView = mActivity.findViewById(R.id.empty_view);
		if (emptyView != null)
			emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	private void showView(int id, boolean show) {
		View view = mRootView.findViewById(id);
		if (view != null) {
			view.setVisibility(show ? View.VISIBLE : View.GONE);
		}
	}

	View.OnClickListener onClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			/* PRIZE-add phone_capacity clickListenner-liguizeng-2015-4-7-start */
			String inSD = StorageManagerEx.getInternalStoragePath();
			String outSD = StorageManagerEx.getExternalStoragePath();

			if(v == mRootView.findViewById(R.id.phone_capacity_line)){
				((FileExplorerTabActivity)getActivity()).mViewPager.setCurrentItem(1);
				if (inSD != null)
					mFileViewActivity.setPath(inSD);
				ActionMode actionMode = ((FileExplorerTabActivity)getActivity()).getActionMode();
				if (actionMode != null) {
					actionMode.finish();
				}
				mFileViewInteractionHub.refreshFileList();
				return;
			} else if (v == mRootView.findViewById(R.id.phone_sd_line)){
				((FileExplorerTabActivity)getActivity()).mViewPager.setCurrentItem(1);
				if (outSD != null)
					mFileViewActivity.setPath(outSD);
				ActionMode actionMode = ((FileExplorerTabActivity)getActivity()).getActionMode();
				if (actionMode != null) {
					actionMode.finish();
				}
				mFileViewInteractionHub.refreshFileList();
				return;
			}
			/* PRIZE-add phone_capacity clickListenner-liguizeng-2015-4-7-end */
			FileCategory f = button2Category.get(v.getId());
			if (f != null) {
				onCategorySelected(f);
				if (f != FileCategory.Favorite) {
					setHasOptionsMenu(true);
				}
			}
		}

	};

	private void setCategoryCount(FileCategory fc, long count) {
		int id = getCategoryCountId(fc);
		if (id == 0)
			return;
		if (isAdded()) {
			setTextView(id, "" + count + " "+getResources().getString(R.string.file_count_unit));
		}
	}

	private void setTextView(int id, String t) {
		TextView text = (TextView) mRootView.findViewById(id);
		text.setText(t);
	}

	private void onCategorySelected(FileCategory f) {
		if (mFileCagetoryHelper.getCurCategory() != f) {
			mFileCagetoryHelper.setCurCategory(f);
			mFileViewInteractionHub.setCurrentPath(mFileViewInteractionHub.getRootPath()
					+ getString(mFileCagetoryHelper.getCurCategoryNameResId()));
			mFileViewInteractionHub.refreshFileList();
		} else {
			updateUI();
		}
		if (f == FileCategory.Favorite) {
			showPage(ViewPage.Favorite);
		} else {
			showPage(ViewPage.Category);
		}
	}

	private void setupClick(int id) {
		View button = mRootView.findViewById(id);
		if (button != null) {
			button.setOnClickListener(onClickListener);
		}
	}

	private void setupClick() {
		setupClick(R.id.category_music);
		setupClick(R.id.category_video);
		setupClick(R.id.category_picture);
		/*PRIZE-delete theme and zip items-hekeyi-2015-4-1-start*/
		//        setupClick(R.id.category_theme);
		setupClick(R.id.category_document);
		//        setupClick(R.id.category_zip);
		/*PRIZE-delete theme and zip items-hekeyi-2015-4-1-end*/


		// PRIZE-add phone_capacity clickListenner-liguizeng-2015-4-7
		setupClick(R.id.phone_capacity_line);
		setupClick(R.id.phone_sd_line);
		setupClick(R.id.category_apk);
		setupClick(R.id.category_favorite);
	}

	@Override
	public boolean onBack() {
		if (isHomePage() || curViewPage == ViewPage.NoSD || mFileViewInteractionHub == null) {
			return false;
		}

		return mFileViewInteractionHub.onBackPressed();
	}

	public boolean isHomePage() {
		return curViewPage == ViewPage.Home;
	}


	public boolean onRefreshFileList(String path, FileSortHelper sort) {
		FileCategory curCategory = mFileCagetoryHelper.getCurCategory();
		if (curCategory == FileCategory.Favorite || curCategory == FileCategory.All)
			return false;
		Cursor c = mFileCagetoryHelper.query(curCategory, sort.getSortMethod());
		showEmptyView(c == null || c.getCount() == 0);
		mAdapter.changeCursor(c);
		refreshCategoryInfo();
		return true;
	}

	@Override
	public View getViewById(int id) {
		return mRootView.findViewById(id);
	}

	@Override
	public Context getContext() {
		return mActivity;
	}

	@Override
	public void onDataChanged() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mAdapter.notifyDataSetChanged();
				mFavoriteList.getArrayAdapter().notifyDataSetChanged();
				showEmptyView(mAdapter.getCount() == 0);
			}

		});
	}

	@Override
	public void onPick(FileInfo f) {
		if (FileViewActivity.PRIZE_FILE_MANAGER_MAIL) {
			Log.d(LOG_TAG, "onPick com.prize.filemanager.ADD_FILE");
			Uri uri = Uri.fromFile(new File(f.filePath));
			Intent intent = new Intent();
			intent.setAction("com.prize.filemanager.ADD_FILE_OK");
			intent.setData(uri);
			mActivity.sendBroadcast(intent);
			mActivity.finish();
		} else {
			Intent intent = new Intent();
			intent.setData(Uri.fromFile(new File(f.filePath)));
			mActivity.setResult(Activity.RESULT_OK, intent);
			mActivity.finish();
		}
	}

	@Override
	public boolean shouldShowOperationPane() {
		return true;
	}

	@Override
	public boolean onOperation(int id) {
		//mFileViewInteractionHub.addContextMenuSelectedItem();
		switch (id) {
		case R.id.button_operation_copy:
		case GlobalConsts.MENU_COPY:
			copyFileInFileView(mFileViewInteractionHub.getSelectedFileList());
			mFileViewInteractionHub.clearSelection();
			break;
		case R.id.button_operation_move:
		case GlobalConsts.MENU_MOVE:
			startMoveToFileView(mFileViewInteractionHub.getSelectedFileList());
			mFileViewInteractionHub.clearSelection();
			break;
		case GlobalConsts.OPERATION_UP_LEVEL:
			setHasOptionsMenu(false);
			showPage(ViewPage.Home);
			break;
		default:
			return false;
		}
		return true;
	}

	@Override
	public String getDisplayPath(String path) {
		return getString(R.string.tab_category) + path;
	}

	@Override
	public String getRealPath(String displayPath) {
		return "";
	}

	@Override
	public boolean onNavigation(String path) {
		showPage(ViewPage.Home);
		return true;
	}

	@Override
	public boolean shouldHideMenu(int menu) {
		return (menu == GlobalConsts.MENU_NEW_FOLDER || menu == GlobalConsts.MENU_FAVORITE
				|| menu == GlobalConsts.MENU_PASTE || menu == GlobalConsts.MENU_SHOWHIDE);
	}

	@Override
	public void addSingleFile(FileInfo file) {
		refreshList();
	}

	@Override
	public Collection<FileInfo> getAllFiles() {
		return mAdapter.getAllFiles();
	}

	@Override
	public FileInfo getItem(int pos) {
		return mAdapter.getFileItem(pos);
	}

	@Override
	public int getItemCount() {
		return mAdapter.getCount();
	}

	@Override
	public void sortCurrentList(FileSortHelper sort) {
		refreshList();
	}

	private void refreshList() {
		mFileViewInteractionHub.refreshFileList();
	}

	private void copyFileInFileView(ArrayList<FileInfo> files) {
		if (files.size() == 0) return;
		mFileViewActivity.copyFile(files);
		((FileExplorerTabActivity) mActivity).setCurrentPage(Util.SDCARD_TAB_INDEX);
	}

	private void startMoveToFileView(ArrayList<FileInfo> files) {
		if (files.size() == 0) return;
		mFileViewActivity.moveToFile(files);
		((FileExplorerTabActivity) mActivity).setCurrentPage(Util.SDCARD_TAB_INDEX);
	}

	@Override
	public FileIconHelper getFileIconHelper() {
		return mFileIconHelper;
	}

	private static int getCategoryCountId(FileCategory fc) {
		switch (fc) {
		case Music:
			return R.id.category_music_count;
		case Video:
			return R.id.category_video_count;
		case Picture:
			return R.id.category_picture_count;
			/*PRIZE-delete theme and zip items-hekeyi-2015-4-1-start*/
			/*case Theme:
                return R.id.category_theme_count;*/
		case Doc:
			return R.id.category_document_count;
			/* case Zip:
                return R.id.category_zip_count;*/
			/*PRIZE-delete theme and zip items-hekeyi-2015-4-1-end*/
		case Apk:
			return R.id.category_apk_count;
		case Favorite:
			return R.id.category_favorite_count;
		}

		return 0;
	}

	private void setCategorySize(FileCategory fc, long size) {
		int txtId = 0;
		int resId = 0;
		switch (fc) {
		case Music:
			txtId = R.id.category_legend_music;
			resId = R.string.category_music;
			break;
		case Video:
			txtId = R.id.category_legend_video;
			resId = R.string.category_video;
			break;
		case Picture:
			txtId = R.id.category_legend_picture;
			resId = R.string.category_picture;
			break;
			/*PRIZE-delete theme and zip items-hekeyi-2015-4-1-start*/
			/*case Theme:
                txtId = R.id.category_legend_theme;
                resId = R.string.category_theme;
                break;*/
		case Doc:
			txtId = R.id.category_legend_document;
			resId = R.string.category_document;
			break;
			/* case Zip:
                txtId = R.id.category_legend_zip;
                resId = R.string.category_zip;
                break;*/
			/*PRIZE-delete theme and zip items-hekeyi-2015-4-1-end*/
		case Apk:
			txtId = R.id.category_legend_apk;
			resId = R.string.category_apk;
			break;
		case Other:
			txtId = R.id.category_legend_other;
			resId = R.string.category_other;
			break;
		}

		if (txtId == 0 || resId == 0)
			return;

		setTextView(txtId, getString(resId) + ":" + Util.convertStorage(size));
	}


	/** 
	 * @param set memory values for total memory 
	 * @return 
	 * @see 
	 * @author hekeyi
	 */
	private void setTotalBarValue( long size){
		if (mCategoryBar == null) {
			mCategoryBar = (CategoryBarSmall) mRootView.findViewById(R.id.category_bar);
		}
		mCategoryBar.setCategoryValue(size);
	}
	private void setSDTotalBarValue( long size){
		if (mSDCategoryBar == null) {
			mSDCategoryBar = (CategoryBarSmall) mRootView.findViewById(R.id.sd_category_bar);
		}
		mSDCategoryBar.setCategoryValue(size);
	}
	/** 
	 * @param set memory values for category memory
	 * @return 
	 * @see 
	 * @author hekeyi
	 */
	private void setCategoryBarValue(FileCategory f, long size) {
		if(multiCategoryBar == null){
			multiCategoryBar = (CategoryBar) mRootView.findViewById(R.id.multi_category_bar);
		}
		multiCategoryBar.setCategoryValue(categoryIndex.get(f), size);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mActivity != null) {
			mActivity.unregisterReceiver(mScannerReceiver);
		}
	}

	private class ScannerReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// handle intents related to external storage
			Log.d("xxf", "refresh");
			if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED) || action.equals(Intent.ACTION_MEDIA_MOUNTED)
					|| action.equals(Intent.ACTION_MEDIA_UNMOUNTED) || action.equals("com.test.broadcast")
					|| action.equals("refresh")) {
				notifyFileChanged();
			}
		}
	}

	private void updateUI() {
		boolean sdCardReady = Util.isSDCardReady();
		if (sdCardReady) {
			if (preViewPage != ViewPage.Invalid) {
				showPage(preViewPage);
				preViewPage = ViewPage.Invalid;
			} else if (curViewPage == ViewPage.Invalid || curViewPage == ViewPage.NoSD) {
				showPage(ViewPage.Home);
			}
			refreshCategoryInfo();
			// refresh file list
			mFileViewInteractionHub.refreshFileList();
			// refresh file list view in another tab
			mFileViewActivity.refresh();
		} else {
			preViewPage = curViewPage;
			showPage(ViewPage.NoSD);
		}
	}

	// process file changed notification, using a timer to avoid frequent
	// refreshing due to batch changing on file system
	synchronized public void notifyFileChanged() {
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		timer.schedule(new TimerTask() {

			public void run() {
				timer = null;
				Message message = new Message();
				message.what = MSG_FILE_CHANGED_TIMER;
				handler.sendMessage(message);
			}

		}, 1000);
	}

	private static final int MSG_FILE_CHANGED_TIMER = 100;

	private Timer timer;

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_FILE_CHANGED_TIMER:
				updateUI();
				break;
			}
			super.handleMessage(msg);
		}

	};

	// update the count of favorite
	@Override
	public void onFavoriteDatabaseChanged() {
		setCategoryCount(FileCategory.Favorite, mFavoriteList.getCount());
	}

	@Override
	public void runOnUiThread(Runnable r) {
		mActivity.runOnUiThread(r);
	}

}

