package com.android.fileexplorer;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;

import com.android.fileexplorer.FileExplorerTabActivity.IBackPressedListener;
import com.android.fileexplorer.FileViewInteractionHub.Mode;

import android.app.ActionBar;
import android.view.ViewGroup.LayoutParams;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.mediatek.storage.StorageManagerEx;

public class FileViewActivity extends Fragment implements
IFileInteractionListener, IBackPressedListener {

	public static final String EXT_FILTER_KEY = "ext_filter";

	private static final String LOG_TAG = "FileViewActivity";

	public static final String EXT_FILE_FIRST_KEY = "ext_file_first";

	public static final String ROOT_DIRECTORY = "root_directory";

	public static final String PICK_FOLDER = "pick_folder";

	private ListView mFileListView;

	// private TextView mCurrentPathTextView;
	private ArrayAdapter<FileInfo> mAdapter;

	private FileViewInteractionHub mFileViewInteractionHub;

	private FileCategoryHelper mFileCagetoryHelper;

	private FileIconHelper mFileIconHelper;

	private ArrayList<FileInfo> mFileNameList = new ArrayList<FileInfo>();

	private Activity mActivity;
	
	private Handler mUiRefreshHandler = new Handler(){
		public void handleMessage(Message msg) {
			mAdapter.notifyDataSetChanged();
		};
	};

	private View mRootView;
	private static final String sdDir = Util.getSdDirectory();
	private static final String startSrc = sdDir.substring(0,
			sdDir.indexOf("/", 1));
	private static final String outSD = StorageManagerEx
			.getExternalStoragePath();
	// memorize the scroll positions of previous paths
	private ArrayList<PathScrollPositionItem> mScrollPositionList = new ArrayList<PathScrollPositionItem>();
	private String mPreviousPath;
	/** PRIZE-修复邮件添加附件中的文件无作用（bug 525）-使用Prize的文件管理器-2015-05-23 start */
	public static  boolean PRIZE_FILE_MANAGER_MAIL = false;
	/** PRIZE-修复邮件添加附件中的文件无作用（bug 525）-使用Prize的文件管理器-2015-05-23 end */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("xxf", "refresh");
			String action = intent.getAction();
			Log.v(LOG_TAG, "received broadcast:" + intent.toString());
			if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
					|| action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
					|| action.equals("com.test.broadcast")) {
						updateUI();
						mUiRefreshHandler.sendEmptyMessage(0);
			} else if (action.equals("refresh")) {
				Log.d(LOG_TAG, "onReceive(Context context, Intent intent) refresh");
				refresh();
				mUiRefreshHandler.sendEmptyMessage(0);
			}
		}
	};

	private boolean mBackspaceExit;

	@Override
	public void onResume() {
		if (!Util.isInPaste) {
			refresh();
		}
		mFileViewInteractionHub.onResume();
		super.onResume();
	}
	@Override
	public void onStart() {
		super.onStart();
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(LOG_TAG, "onCreateView....");
		mActivity = getActivity();
		// getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);
		mRootView = inflater.inflate(R.layout.file_explorer_list, container,
				false);
		ActivitiesManager.getInstance().registerActivity(
				ActivitiesManager.ACTIVITY_FILE_VIEW, mActivity);

		mFileCagetoryHelper = new FileCategoryHelper(mActivity);
		mFileViewInteractionHub = new FileViewInteractionHub(this);
		Intent intent = mActivity.getIntent();
		String action = intent.getAction();
		if (!TextUtils.isEmpty(action)
				&& (action.equals(Intent.ACTION_PICK)
						|| action.equals(Intent.ACTION_GET_CONTENT)
						/**
						 * PRIZE-修复邮件添加附件中的文件无作用（bug 525）-使用Prize的文件管理器- 2015-05-23
						 * start
						 */
						|| action.equals("com.prize.filemanager.ADD_FILE"))
				) {
			if (action.equals("com.prize.filemanager.ADD_FILE")) {
				PRIZE_FILE_MANAGER_MAIL = true;
			} else {
				PRIZE_FILE_MANAGER_MAIL = false;
			}
			/** PRIZE-修复邮件添加附件中的文件无作用（bug 525）-使用Prize的文件管理器- 2015-05-23 end */
			mFileViewInteractionHub.setMode(Mode.Pick);

			boolean pickFolder = intent.getBooleanExtra(PICK_FOLDER, false);
			if (!pickFolder) {
				String[] exts = intent.getStringArrayExtra(EXT_FILTER_KEY);
				if (exts != null) {
					mFileCagetoryHelper.setCustomCategory(exts);
				}
			} else {
				mFileCagetoryHelper.setCustomCategory(new String[] {} /*
				 * folder
				 * only
				 */);
				mRootView.findViewById(R.id.pick_operation_bar).setVisibility(
						View.VISIBLE);

				mRootView.findViewById(R.id.button_pick_confirm)
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						try {
							Intent intent = Intent.parseUri(
									mFileViewInteractionHub
									.getCurrentPath(), 0);
							mActivity.setResult(Activity.RESULT_OK,
									intent);
							mActivity.finish();
						} catch (URISyntaxException e) {
							e.printStackTrace();
						}
					}
				});

				mRootView.findViewById(R.id.button_pick_cancel)
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						mActivity.finish();
					}
				});
			}
		} else {
			mFileViewInteractionHub.setMode(Mode.View);
		}

		mFileListView = (ListView) mRootView.findViewById(R.id.file_path_list);
		mFileIconHelper = new FileIconHelper(mActivity);
		mAdapter = new FileListAdapter(mActivity, R.layout.file_browser_item,
				mFileNameList, mFileViewInteractionHub, mFileIconHelper);

		boolean baseSd = intent.getBooleanExtra(GlobalConsts.KEY_BASE_SD,
				!FileExplorerPreferenceActivity.isReadRoot(mActivity));
		Log.i(LOG_TAG, "baseSd = " + baseSd);

		String rootDir = intent.getStringExtra(ROOT_DIRECTORY);
		if (!TextUtils.isEmpty(rootDir)) {
			if (baseSd && this.sdDir.startsWith(rootDir)) {
				rootDir = this.sdDir;
			}
		} else {
			rootDir = baseSd ? this.sdDir : GlobalConsts.ROOT_PATH;
		}
		mFileViewInteractionHub.setRootPath(rootDir);

		String currentDir = FileExplorerPreferenceActivity
				.getPrimaryFolder(mActivity);
		Uri uri = intent.getData();
		if (uri != null) {
			if (baseSd && this.sdDir.startsWith(uri.getPath())) {
				currentDir = this.sdDir;
			} else {
				currentDir = uri.getPath();
			}
			((FileExplorerTabActivity) getActivity()).mViewPager
			.setCurrentItem(1);
		}
		File file = new File(currentDir);
		if (!file.exists()) {
			Toast.makeText(mActivity, getString(R.string.folderError), Toast.LENGTH_SHORT)
			.show();
			mFileViewInteractionHub.setCurrentPath(this.sdDir);
		} else {
			mFileViewInteractionHub.setCurrentPath(currentDir);
			mFileViewInteractionHub.refreshFileList();
		}
		Log.i(LOG_TAG, "CurrentDir = " + currentDir);

		mBackspaceExit = (uri != null)
				&& (TextUtils.isEmpty(action) || (!action
						.equals(Intent.ACTION_PICK) && !action
						.equals(Intent.ACTION_GET_CONTENT)));

		mFileListView.setAdapter(mAdapter);
		// mFileViewInteractionHub.refreshFileList();
		Intent intentrec = getActivity().getIntent();
		String recPath = intentrec.getStringExtra("soundRecorder");
		String currentItemIndex = intentrec.getStringExtra("currentItemIndex");
		if (currentItemIndex != null) {
			((FileExplorerTabActivity) getActivity()).mViewPager
			.setCurrentItem(1);
		}
		if (recPath != null) {
			File recordDir = new File(recPath);
			if (!recordDir.exists()) {
				Toast.makeText(mActivity, getString(R.string.recordError), 1000)
				.show();
				mFileViewInteractionHub.setCurrentPath(this.sdDir);
			} else {
				mFileViewInteractionHub.setCurrentPath(recPath);
				mFileViewInteractionHub.refreshFileList();
			}

		}
		IntentFilter intentFilter = new IntentFilter();
		/*
		 * intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		 * intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		 * intentFilter.addAction("com.test.broadcast");
		 */
		intentFilter.addAction("refresh");
		// intentFilter.addDataScheme("file");
		mActivity.registerReceiver(mReceiver, intentFilter);

		updateUI();
		setHasOptionsMenu(true);
		/*
		 * View view =
		 * getActivity().getWindow().getDecorView().findViewById(com.
		 * android.internal.R.id.decor_content_parent); View actionBar =
		 * view.findViewById(com.android.internal.R.id.split_action_bar); if
		 * (actionBar != null) { actionBar.setVisibility(View.GONE); }
		 */
		return mRootView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mActivity.unregisterReceiver(mReceiver);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		mFileViewInteractionHub.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		mFileViewInteractionHub.onCreateOptionsMenu(menu);
	}

	@Override
	public void onStop() {
		super.onStop();
	}
	@Override
	public void onPause() {
		super.onPause();
		mFileViewInteractionHub.onPause();
	}
	@Override
	public boolean onBack() {
		if (mBackspaceExit || !Util.isSDCardReady()
				|| mFileViewInteractionHub == null) {
			return false;
		}

		return mFileViewInteractionHub.onBackPressed();
	}

	private class PathScrollPositionItem {
		String path;
		int pos;

		PathScrollPositionItem(String s, int p) {
			path = s;
			pos = p;
		}
	}

	// execute before change, return the memorized scroll position
	private int computeScrollPosition(String path) {
		int pos = 0;
		if (mPreviousPath != null) {
			if (path.startsWith(mPreviousPath)) {
				int firstVisiblePosition = mFileListView
						.getFirstVisiblePosition();
				if (mScrollPositionList.size() != 0
						&& mPreviousPath.equals(mScrollPositionList
								.get(mScrollPositionList.size() - 1).path)) {
					mScrollPositionList.get(mScrollPositionList.size() - 1).pos = firstVisiblePosition;
					Log.i(LOG_TAG, "computeScrollPosition: update item: "
							+ mPreviousPath + " " + firstVisiblePosition
							+ " stack count:" + mScrollPositionList.size());
					pos = firstVisiblePosition;
				} else {
					mScrollPositionList.add(new PathScrollPositionItem(
							mPreviousPath, firstVisiblePosition));
					Log.i(LOG_TAG, "computeScrollPosition: add item: "
							+ mPreviousPath + " " + firstVisiblePosition
							+ " stack count:" + mScrollPositionList.size());
				}
			} else {
				int i;
				boolean isLast = false;
				for (i = 0; i < mScrollPositionList.size(); i++) {
					if (!path.startsWith(mScrollPositionList.get(i).path)) {
						break;
					}
				}
				// navigate to a totally new branch, not in current stack
				if (i > 0) {
					pos = mScrollPositionList.get(i - 1).pos;
				}

				for (int j = mScrollPositionList.size() - 1; j >= i - 1
						&& j >= 0; j--) {
					mScrollPositionList.remove(j);
				}
			}
		}

		Log.i(LOG_TAG, "computeScrollPosition: result pos: " + path + " " + pos
				+ " stack count:" + mScrollPositionList.size());
		mPreviousPath = path;
		return pos;
	}

	public boolean onRefreshFileList(String path, FileSortHelper sort) {
		Log.d(LOG_TAG, "onRefreshFileList(String path, FileSortHelper sort)");
		File file = new File(path);
		if (!file.exists() || !file.isDirectory()) {
			return false;
		}
		ArrayList<FileInfo> fileList = mFileNameList;
		fileList.clear();

		File[] listFiles = file.listFiles(mFileCagetoryHelper.getFilter());
		if (listFiles == null)
			return true;

		for (File child : listFiles) {
			Log.d(LOG_TAG, "onRefreshFileList  for (File child : listFiles)");
			// do not show selected file if in move state
			if (mFileViewInteractionHub.isMoveState()
					&& mFileViewInteractionHub.isFileSelected(child.getPath()))
				continue;
			String absolutePath = child.getAbsolutePath();
			if (Util.isNormalFile(absolutePath)
					&& Util.shouldShowFile(absolutePath)) {
				FileInfo lFileInfo = Util.GetFileInfo(child,
						mFileCagetoryHelper.getFilter(), Settings.instance()
						.getShowDotAndHiddenFiles());
				if (lFileInfo != null) {
					Log.d(LOG_TAG, "onRefreshFileList  lFileInfo != null");
					fileList.add(lFileInfo);
				}
			}
		}

		final int pos = computeScrollPosition(path);

		sortCurrentList(sort);
		showEmptyView(fileList.size() == 0);
		mFileListView.setSelection(pos);
		getActivity().invalidateOptionsMenu();
		return true;
	}

	private void updateUI() {
		boolean sdCardReady = Util.isSDCardReady();
		View noSdView = mRootView.findViewById(R.id.sd_not_available_page);
		noSdView.setVisibility(sdCardReady ? View.GONE : View.VISIBLE);

		View navigationBar = mRootView.findViewById(R.id.navigation_bar);
		navigationBar.setVisibility(sdCardReady ? View.VISIBLE : View.GONE);
		mFileListView.setVisibility(sdCardReady ? View.VISIBLE : View.GONE);

		if (sdCardReady) {
			mFileViewInteractionHub.refreshFileList();
		}
	}

	private void showEmptyView(boolean show) {
		View emptyView = mRootView.findViewById(R.id.empty_view);
		if (emptyView != null)
			emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
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
		Log.d(LOG_TAG, "onDataChanged.....");
		mUiRefreshHandler.sendEmptyMessage(0);
	}

	@Override
	public void onPick(FileInfo f) {
		/** PRIZE-修复邮件添加附件中的文件无作用（bug 525）-使用Prize的文件管理器- 2015-05-23 start */
		if (PRIZE_FILE_MANAGER_MAIL) {
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
		/** PRIZE-修复邮件添加附件中的文件无作用（bug 525）-使用Prize的文件管理器- 2015-05-23 end */
		return;
	}

	@Override
	public boolean shouldShowOperationPane() {
		return true;
	}

	@Override
	public boolean onOperation(int id) {
		return false;
	}

	// 支持显示真实路径
	@Override
	public String getDisplayPath(String path) {
		/* PRIZE-start_path-xiaxuefeng-2015-5-11-start */
		if (isAdded()) {
			if (path.startsWith(this.startSrc)
					&& !FileExplorerPreferenceActivity.showRealPath(mActivity)) {
				if (path.startsWith(this.sdDir)) {
					return getString(R.string.root_str) + "/"
							+ getString(R.string.sd_folder)
							+ path.substring(this.sdDir.length());
				} else if (path.startsWith(this.outSD)) {
					return getString(R.string.root_str) + "/"
							+ getString(R.string.out_sd)
							+ path.substring(this.outSD.length());
				} else {
					return getString(R.string.root_str)
							+ path.substring(this.startSrc.length());
				}

			} else {
				return path;
			}
		}
		return path;
		/* PRIZE-start_path-xiaxuefeng-2015-5-11-end */
	}

	@Override
	public String getRealPath(String displayPath) {
		/* PRIZE-start_path-xiaxuefeng-2015-5-11-start */
		final String perfixName = getString(R.string.root_str);
		final String perfixNameIn = perfixName + "/"
				+ getString(R.string.sd_folder);
		final String perfixNameOut = perfixName + "/"
				+ getString(R.string.out_sd);
		/* PRIZE-start_path-xiaxuefeng-2015-5-11-end */
		if (displayPath.startsWith(perfixName)) {
			if (displayPath.startsWith(perfixNameIn)) {
				return this.sdDir
						+ displayPath.substring(perfixNameIn.length());
			} else if (displayPath.startsWith(perfixNameOut)) {
				return this.outSD
						+ displayPath.substring(perfixNameOut.length());
			} else {
				return this.startSrc
						+ displayPath.substring(perfixName.length());
			}
		} else {
			return displayPath;
		}
	}

	@Override
	public boolean onNavigation(String path) {
		return false;
	}

	@Override
	public boolean shouldHideMenu(int menu) {
		return false;
	}

	public void copyFile(ArrayList<FileInfo> files) {
		mFileViewInteractionHub.onOperationCopy(files);
	}

	public void refresh() {
		Log.d(LOG_TAG, "refresh......");
		if (mFileViewInteractionHub != null) {
			mFileViewInteractionHub.refreshFileList();
		}
	}

	public void moveToFile(ArrayList<FileInfo> files) {
		mFileViewInteractionHub.moveFileFrom(files);
	}

	public interface SelectFilesCallback {
		// files equals null indicates canceled
		void selected(ArrayList<FileInfo> files);
	}

	public void startSelectFiles(SelectFilesCallback callback) {
		mFileViewInteractionHub.startSelectFiles(callback);
	}

	@Override
	public FileIconHelper getFileIconHelper() {
		return mFileIconHelper;
	}

	public boolean setPath(String location) {
		if (mFileViewInteractionHub == null
				|| !location.startsWith(mFileViewInteractionHub.getRootPath())) {
			return false;
		}
		mFileViewInteractionHub.setCurrentPath(location);
		mFileViewInteractionHub.refreshFileList();
		return true;
	}

	@Override
	public FileInfo getItem(int pos) {
		if (pos < 0 || pos > mFileNameList.size() - 1)
			return null;

		return mFileNameList.get(pos);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void sortCurrentList(FileSortHelper sort) {
		Log.d(LOG_TAG, "sortCurrentList(FileSortHelper sort)");
		Collections.sort(mFileNameList, sort.getComparator());
		mUiRefreshHandler.sendEmptyMessage(0);
	}

	@Override
	public ArrayList<FileInfo> getAllFiles() {
		return mFileNameList;
	}

	@Override
	public void addSingleFile(FileInfo file) {
		Log.d(LOG_TAG, "addSingleFile(FileInfo file)");
		mFileNameList.add(file);
		mUiRefreshHandler.sendEmptyMessage(0);
	}

	@Override
	public int getItemCount() {
		return mFileNameList.size();
	}

	@Override
	public void runOnUiThread(Runnable r) {
		mActivity.runOnUiThread(r);
	}

	@Override
	public void refreshCategoryInfo() {
		// TODO Auto-generated method stub

	}
}
