/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * This file is part of FileExplorer.
 *
 * FileExplorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileExplorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.android.fileexplorer;

import java.io.File;

import com.mediatek.storage.StorageManagerEx;

import java.util.ArrayList;
import java.util.Arrays;

import org.swiftp.Globals;

import android.R.drawable;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteFullException;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.fileexplorer.FileListItem.ModeCallback;
import com.android.fileexplorer.FileOperationHelper.IOperationProgressListener;
import com.android.fileexplorer.FileSortHelper.SortMethod;
import com.android.fileexplorer.FileViewActivity.SelectFilesCallback;
import com.android.fileexplorer.TextInputDialog.OnFinishListener;
import com.android.fileexplorer.Util.OutSDCardInfo;
import com.android.fileexplorer.Util.SDCardInfo;

/* PRIZE-input package-liufan-2015-05-23-start */
/* PRIZE-input package-liufan-2015-05-23-end */

public class FileViewInteractionHub implements IOperationProgressListener {

	private static final String LOG_TAG = "FileViewInteractionHub";

	private IFileInteractionListener mFileViewListener;

	private ArrayList<FileInfo> mCheckedFileNameList = new ArrayList<FileInfo>();

	private FileOperationHelper mFileOperationHelper;

	private FileSortHelper mFileSortHelper;

	private View mConfirmOperationBar;

	private ProgressDialog progressDialog;

	private View mNavigationBar;

	private TextView mNavigationBarText;

	private View mDropdownNavigation;

	private ImageView mNavigationBarUpDownArrow;

	private Context mContext;

	private HorizontalScrollView pathListContain;

	private LinearLayout pathList;
	private boolean mInPaste = false;
	private String outSD = StorageManagerEx.getExternalStoragePath();
	private String inSD = StorageManagerEx.getInternalStoragePath();
	/*
	 * final Handler handler = new Handler(){
	 * 
	 * @Override public void handleMessage(Message msg) { if (msg.what == 111){
	 * Intent intent = new Intent("refresh"); mContext.sendBroadcast(intent); }
	 * } };
	 */
	public enum Mode {
		View, Pick
	};

	public FileViewInteractionHub(IFileInteractionListener fileViewListener) {
		assert (fileViewListener != null);
		mFileViewListener = fileViewListener;
		setup();
		mFileSortHelper = new FileSortHelper();
		mContext = mFileViewListener.getContext();
		Globals.setContext(mContext);
		mFileOperationHelper = new FileOperationHelper(this, mContext);

	}

	private void showProgress(String msg) {
		progressDialog = new ProgressDialog(mContext);
		// dialog.setIcon(R.drawable.icon);
		progressDialog.setMessage(msg);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.setOnKeyListener(onKeyListener);
		progressDialog.show();
	}

	/**
	 * add a keylistener for progress dialog
	 */
	private OnKeyListener onKeyListener = new OnKeyListener() {
		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_BACK
					&& event.getAction() == KeyEvent.ACTION_DOWN) {
				onBackPressed();
			}
			return false;
		}
	};

	public void sortCurrentList() {
		mFileViewListener.sortCurrentList(mFileSortHelper);
	}

	public boolean canShowCheckBox() {
		return mConfirmOperationBar.getVisibility() != View.VISIBLE;
	}

	private void showConfirmOperationBar(boolean show) {
		mConfirmOperationBar.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	public void addContextMenuSelectedItem() {
		if (mCheckedFileNameList.size() == 0) {
			int pos = mListViewContextMenuSelectedItem;
			if (pos != -1) {
				FileInfo fileInfo = mFileViewListener.getItem(pos);
				if (fileInfo != null) {
					mCheckedFileNameList.add(fileInfo);
				}
			}
		}
	}

	public ArrayList<FileInfo> getSelectedFileList() {
		return mCheckedFileNameList;
	}

	public boolean canPaste() {
		return mFileOperationHelper.canPaste();
	}

	// operation finish notification
	@Override
	public void onFinish() {
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}

		mFileViewListener.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showConfirmOperationBar(false);
				clearSelection();
				refreshFileList();
			}
		});
	}

	public FileInfo getItem(int pos) {
		return mFileViewListener.getItem(pos);
	}

	public boolean isInSelection() {
		return mCheckedFileNameList.size() > 0;
	}

	public boolean isMoveState() {
		return mFileOperationHelper.isMoveState()
				|| mFileOperationHelper.canPaste();
	}

	private void setup() {
		setupNaivgationBar();
		setupFileListView();
		setupOperationPane();
	}

	private void setupNaivgationBar() {
		mNavigationBar = mFileViewListener.getViewById(R.id.navigation_bar);
		mNavigationBarText = (TextView) mFileViewListener
				.getViewById(R.id.current_path_view);
		pathListContain = (HorizontalScrollView) mNavigationBar
				.findViewById(R.id.path_contain);
		if (pathListContain != null)
			pathList = (LinearLayout) pathListContain
			.findViewById(R.id.navigation_path_list);
	}

	// buttons
	private void setupOperationPane() {
		mConfirmOperationBar = mFileViewListener
				.getViewById(R.id.moving_operation_bar);
		setupClick(mConfirmOperationBar, R.id.button_moving_confirm);
		setupClick(mConfirmOperationBar, R.id.button_moving_cancel);
	}

	private void setupClick(View v, int id) {
		View button = (v != null ? v.findViewById(id) : mFileViewListener
				.getViewById(id));
		if (button != null)
			button.setOnClickListener(buttonClick);
	}

	private View.OnClickListener buttonClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button_operation_copy:
				onOperationCopy();
				break;
			case R.id.button_operation_move:
				onOperationMove();
				break;
			case R.id.button_operation_send:
				onOperationSend();
				break;
			case R.id.button_operation_delete:
				onOperationDelete();
				break;
			case R.id.button_operation_cancel:
				onOperationSelectAllOrCancel();
				break;
				/*
				 * case R.id.current_path_pane: onNavigationBarClick(); break;
				 */
			case R.id.button_moving_confirm:
				onOperationButtonConfirm();
				break;
			case R.id.button_moving_cancel:
				onOperationButtonCancel();
				break;
				/*
				 * case R.id.path_pane_up_level: onOperationUpLevel(); ActionMode
				 * mode = ((FileExplorerTabActivity) mContext) .getActionMode(); if
				 * (mode != null) { mode.finish(); } break;
				 */
			}
		}

	};

	private void onOperationReferesh() {
		refreshFileList();
	}

	private void onOperationFavorite() {
		String path = mCurrentPath;

		if (mListViewContextMenuSelectedItem != -1) {
			path = mFileViewListener.getItem(mListViewContextMenuSelectedItem).filePath;
		}
		/*
		 * String strs[] = path.split("/"); String outSD =
		 * StorageManagerEx.getExternalStoragePath(); String inSD =
		 * StorageManagerEx.getInternalStoragePath(); String inSD_end =
		 * inSD.substring(inSD.lastIndexOf("/") + 1, inSD.length()); String
		 * outSD_end = outSD.substring(outSD.lastIndexOf("/") + 1,
		 * outSD.length()); StringBuffer lastPath = new StringBuffer(); for (int
		 * i = 0; i < strs.length; i++) { if (i > 1) {
		 * if(strs[i].equals(inSD_end) ) { lastPath.append("0:"); }else if
		 * (strs[i].equals(outSD_end)){ lastPath.append("1:"); }else {
		 * lastPath.append("/" + strs[i]); } } }
		 * onOperationFavorite(lastPath.toString());
		 */
		onOperationFavorite(path);
		clearSelection();
	}

	private void onOperationSetting() {
		Intent intent = new Intent(mContext,
				FileExplorerPreferenceActivity.class);
		if (intent != null) {
			try {
				mContext.startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Log.e(LOG_TAG, "fail to start setting: " + e.toString());
			}
		}
	}

	private void onOperationFavorite(String path) {
		String strs[] = path.split("/");
		String inSD_end = null, outSD_end = null;
		if (inSD != null)
			inSD_end = inSD.substring(inSD.lastIndexOf("/") + 1,
					inSD.length());
		if (outSD != null)
			outSD_end = outSD.substring(outSD.lastIndexOf("/") + 1,
					outSD.length());
		StringBuffer lastPath = new StringBuffer();
		for (int i = 0; i < strs.length; i++) {
			if (i > 1) {
				if (inSD_end != null && strs[i].equals(inSD_end) || strs[i].equals("emulated")) {
					if (lastPath.length() > 0) {
						lastPath.delete(0, lastPath.length());
					}
					lastPath.append("0:");
				} else if (outSD_end != null && strs[i].equals(outSD_end)) {
					lastPath.append("1:");
				} else {
					lastPath.append("/" + strs[i]);
				}
			}
		}
		FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper
				.getInstance();
		if (databaseHelper != null) {
			int stringId = 0;
			if (databaseHelper.isFavorite(path)) {
				databaseHelper.delete(lastPath.toString());
				stringId = R.string.removed_favorite;
			} else {
				databaseHelper.insert(
						Util.getNameFromFilepath(lastPath.toString()),
						lastPath.toString());
				stringId = R.string.added_favorite;
			}

			Toast.makeText(mContext, stringId, Toast.LENGTH_SHORT).show();
		}
	}

	private void onOperationShowSysFiles() {
		Settings.instance().setShowDotAndHiddenFiles(
				!Settings.instance().getShowDotAndHiddenFiles());
		refreshFileList();
	}

	public void onOperationSelectAllOrCancel() {
		if (!isSelectedAll()) {
			onOperationSelectAll();
		} else {
			clearSelection();
		}
	}

	public void onOperationSelectAll() {
		mCheckedFileNameList.clear();
		for (FileInfo f : mFileViewListener.getAllFiles()) {
			f.Selected = true;
			mCheckedFileNameList.add(f);
		}
		FileExplorerTabActivity fileExplorerTabActivity = (FileExplorerTabActivity) mContext;
		ActionMode mode = fileExplorerTabActivity.getActionMode();
		if (mode == null) {
			mode = fileExplorerTabActivity.startActionMode(new ModeCallback(
					mContext, this));
			fileExplorerTabActivity.setActionMode(mode);
			Util.updateActionModeTitle(mode, mContext, getSelectedFileList()
					.size());
		}
		mFileViewListener.onDataChanged();
	}

	private OnClickListener navigationClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			String inSDAndOutSD = inSD+":"+outSD;
			Log.d(LOG_TAG, inSDAndOutSD);
			String path = (String) v.getTag();
			OutSDCardInfo outSDCardInfo = Util.getOutSDCardInfo();
			if(path.equals(outSD) && null == outSDCardInfo){
				return;
			}
			assert (path != null);
			if (mFileViewListener.onNavigation(path))
				return;

			if (path.isEmpty()) {
				mCurrentPath = mRoot;
			} else {
				mCurrentPath = path;
			}
			refreshFileList();
		}

	};

	/*
	 * protected void onNavigationBarClick() { if
	 * (mDropdownNavigation.getVisibility() == View.VISIBLE) {
	 * showDropdownNavigation(false); } else { LinearLayout list =
	 * (LinearLayout)
	 * mDropdownNavigation.findViewById(R.id.dropdown_navigation_list);
	 * list.removeAllViews(); int pos = 0; String displayPath =
	 * mFileViewListener.getDisplayPath(mCurrentPath); while (pos != -1 &&
	 * !displayPath.equals("/")) { int end = displayPath.indexOf("/", pos); if
	 * (end == -1) break;
	 * 
	 * View listItem =
	 * LayoutInflater.from(mContext).inflate(R.layout.textview_item, null);
	 * TextView text = (TextView) listItem.findViewById(R.id.path_name); String
	 * substring = displayPath.substring(pos, end);
	 * if(substring.isEmpty())substring = "/"; text.setText(substring);
	 * 
	 * listItem.setOnClickListener(navigationClick);
	 * listItem.setTag(mFileViewListener.getRealPath(displayPath.substring(0,
	 * end))); pos = end + 1; list.addView(listItem); } if (list.getChildCount()
	 * > 0) showDropdownNavigation(true);
	 * 
	 * } }
	 */

	public boolean onOperationUpLevel() {
		if (mFileViewListener.onOperation(GlobalConsts.OPERATION_UP_LEVEL)) {
			return true;
		}

		if (inSD != null && !"/storage".equals(mCurrentPath)) {
			if (mCurrentPath.equals(inSD)) {
				mCurrentPath = "/storage";
			} else {
				mCurrentPath = new File(mCurrentPath).getParent();
			}
			refreshFileList();
			return true;
		}

		return false;
	}

	public void onOperationCreateFolder() {
		TextInputDialog dialog = new TextInputDialog(mContext,
				mContext.getString(R.string.operation_create_folder),
				mContext.getString(R.string.operation_create_folder_message),
				mContext.getString(R.string.new_folder_name),
				new OnFinishListener() {
			@Override
			public boolean onFinish(String text) {
				return doCreateFolder(text);
			}
		});
		dialog.show();
		dialog.setEditTextFilter(20);
	}

	private boolean doCreateFolder(String text) {
		if (TextUtils.isEmpty(text))
			return false;
		String mess = mFileOperationHelper.CreateFolder(mCurrentPath, text);
		if (mess.equals("success")) {
			refreshFileList();
			onFileChanged(Util.makePath(mCurrentPath, text));
			/*
			 * mFileViewListener.addSingleFile(Util.GetFileInfo(Util.makePath(
			 * mCurrentPath, text)));
			 * mFileListView.setSelection(mFileListView.getCount() - 1);
			 */
		} else if (mess.equals("error")) {
			new AlertDialog.Builder(mContext)
			.setMessage(
					mContext.getString(R.string.fail_to_create_folder))
					.setPositiveButton(R.string.confirm, null).create().show();
			return false;
		} else {
			new AlertDialog.Builder(mContext)
			.setMessage(mContext.getString(R.string.folder_exist))
			.setPositiveButton(R.string.confirm, null).create().show();
			return false;
		}

		return true;
	}

	public void onOperationSearch() {

	}

	public void onSortChanged(SortMethod s) {
		if (mFileSortHelper.getSortMethod() != s) {
			mFileSortHelper.setSortMethog(s);
			sortCurrentList();
		}
	}

	public void onOperationCopy() {
		onOperationCopy(getSelectedFileList());
	}

	public void onOperationCopy(ArrayList<FileInfo> files) {
		mFileOperationHelper.Copy(files);
		clearSelection();

		showConfirmOperationBar(true);
		View confirmButton = mConfirmOperationBar
				.findViewById(R.id.button_moving_confirm);
		// confirmButton.setEnabled(false);
		// refresh to hide selected files
		// refreshFileList();
	}

	public void onOperationCopyPath() {
		if (getSelectedFileList().size() == 1) {
			copy(getSelectedFileList().get(0).filePath);
		}
		clearSelection();
	}

	private void copy(CharSequence text) {
		ClipboardManager cm = (ClipboardManager) mContext
				.getSystemService(Context.CLIPBOARD_SERVICE);
		cm.setText(text);
		Toast.makeText(mContext, R.string.copy_path_success, Toast.LENGTH_SHORT).show();
	}

	private void onOperationPaste() {
		ArrayList<FileInfo> pasteFiles = mFileOperationHelper.getFileList();
		long totalPaste = 0;
		long sdFree = 0;
		for (FileInfo fileInfo : pasteFiles) {
			totalPaste += fileInfo.fileSize;
		}
		if (inSD != null && mCurrentPath.startsWith(inSD)) {
			SDCardInfo sdCardInfo = Util.getSDCardInfo();
			sdFree = sdCardInfo.free;
		} else if (outSD != null && mCurrentPath.startsWith(outSD)){
			OutSDCardInfo outsdCardInfo = Util.getOutSDCardInfo();
			sdFree = outsdCardInfo.free;
		}
		if (totalPaste > sdFree) {
			Toast.makeText(mContext, R.string.no_enough_room, Toast.LENGTH_SHORT).show();
			showConfirmOperationBar(false);
			clearSelection();
			mFileOperationHelper.clear();
			return;
		}
		if (mFileOperationHelper.Paste(mCurrentPath)) {
			mInPaste = true;
			showProgress(mContext.getString(R.string.operation_pasting));
		}
	}

	public void onOperationMove() {
		boolean isFavorite = false;
		for (FileInfo fileInfo : getSelectedFileList()) {
			FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper
					.getInstance();
			if (databaseHelper != null && fileInfo != null) {
				isFavorite = databaseHelper.isFavorite(fileInfo.filePath);
				break;
			}
		}
		if (isFavorite) {
			Toast.makeText(mContext, R.string.cancleFavorite, 1000).show();
			clearSelection();
			return;
		}
		mFileOperationHelper.StartMove(getSelectedFileList());
		clearSelection();
		showConfirmOperationBar(true);
		View confirmButton = mConfirmOperationBar
				.findViewById(R.id.button_moving_confirm);
		confirmButton.setEnabled(false);
		// refresh to hide selected files
		refreshFileList();
	}

	public void refreshFileList() {
		clearSelection();
		updateNavigationPane();
		/*
		 * String sdIn = Environment.getExternalStorageDirectory().getPath();
		 * String path = sdIn. substring(0, sdIn.lastIndexOf("/"));
		 * org.swiftp.Util.newFileNotify(path);
		 */
		// MediaScannerConnection.scanFile(mContext, new String[] { path },
		// null,
		// null);
		// onRefreshFileList returns true indicates list has changed
		mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);
		// update move operation button state
		updateConfirmButtons();

	}

	private void updateConfirmButtons() {
		Button confirmButton = (Button) mConfirmOperationBar
				.findViewById(R.id.button_moving_confirm);
		if (mConfirmOperationBar.getVisibility() == View.GONE){
			confirmButton.setEnabled(true);
			confirmButton.setTextColor(Color.BLACK);
			return;
		}

		int text = R.string.operation_paste;
		if (isSelectingFiles()) {
			confirmButton.setEnabled(mCheckedFileNameList.size() != 0);
			text = R.string.operation_send;
		} else if (isMoveState()) {
			confirmButton.setEnabled(mFileOperationHelper.canMove(mCurrentPath)
					&& !mCurrentPath.equals("/storage"));
		}
		if (!confirmButton.isEnabled()) {
			confirmButton.setTextColor(Color.GRAY);
		} else {
			confirmButton.setTextColor(Color.BLACK);
		}
		confirmButton.setText(text);
	}


	private void updateNavigationPane() {
		// View upLevel =
		// mFileViewListener.getViewById(R.id.path_pane_up_level);
		/* PRIZE-delete menu-hekeyi-2015-4-1-start */
		// upLevel.setVisibility(mRoot.equals(mCurrentPath) ? View.INVISIBLE :
		// View.VISIBLE);
		/* PRIZE-delete menu-hekeyi-2015-4-1-end */
		// View arrow = mFileViewListener.getViewById(R.id.path_pane_arrow);
		/* PRIZE-delete menu-hekeyi-2015-4-1-start */
		// arrow.setVisibility(mRoot.equals(mCurrentPath) ? View.GONE :
		// View.VISIBLE);
		/* PRIZE-delete menu-hekeyi-2015-4-1-end */
		// Log.d("lgz","mFileViewListener.getDisplayPath(mCurrentPath):"+mFileViewListener.getDisplayPath(mCurrentPath)
		// +"/nmCurrentPath:"+mCurrentPath+"mNavigationBarText:"+mNavigationBarText.getText());
		// list.removeAllViews();
		pathList.removeAllViews();
		int first = 0;
		int pos = 0;
		String displayPath = mFileViewListener.getDisplayPath(mCurrentPath);
		int end = displayPath.indexOf("/", pos);
		first = end;
		if (end != -1) {
			pos = end + 1;
			LinearLayout layout = new LinearLayout(mContext);
			layout.setEnabled(false);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					Util.dip2px(mContext, 80),
					LinearLayout.LayoutParams.MATCH_PARENT);
			layout.setBackgroundColor(Color.TRANSPARENT);
			pathList.addView(layout, params);
			while (pos != -1 && !displayPath.equals("/")) {
				end = displayPath.indexOf("/", pos);
				if (end == -1) {
					String substring = displayPath.substring(pos,
							displayPath.length());
					View listItem = LayoutInflater.from(mContext).inflate(
							R.layout.textview_item, null);
					TextView text = (TextView) listItem
							.findViewById(R.id.path_name);
					text.setText(substring);
					pathList.addView(listItem);
					break;
				}
				View listItem = LayoutInflater.from(mContext).inflate(
						R.layout.textview_item, null);
				TextView text = (TextView) listItem
						.findViewById(R.id.path_name);
				String substring = displayPath.substring(pos, end);
				if (substring.isEmpty())
					substring = "/";
				text.setText(substring);

				listItem.setOnClickListener(navigationClick);
				listItem.setTag(mFileViewListener.getRealPath(displayPath
						.substring(0, end)));
				pos = end + 1;
				pathList.addView(listItem);

				ViewTreeObserver vto = pathList.getViewTreeObserver();
				vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
					boolean hasMeasured;

					@Override
					public boolean onPreDraw() {
						if (hasMeasured == false) {
							int wid = pathList.getWidth();
							pathListContain.scrollTo(wid, 0);
							hasMeasured = true;
						}
						return true;
					}
				});
			}
			String sub = displayPath.substring(0, first);
			mNavigationBarText.setText(sub);
			mNavigationBarText.setOnClickListener(navigationClick);
			mNavigationBarText.setTag(mFileViewListener.getRealPath(displayPath
					.substring(0, first)));
			mNavigationBarText.bringToFront();
		} else {
			mNavigationBarText.setText(displayPath);
		}
	}

	public void onOperationSend() {
		ArrayList<FileInfo> selectedFileList = getSelectedFileList();
		for (FileInfo f : selectedFileList) {
			String ext = f.filePath.substring(f.filePath.lastIndexOf('.') + 1,
					f.filePath.length()).toLowerCase();
			if (f.IsDir) {
				AlertDialog dialog = new AlertDialog.Builder(mContext)
				.setMessage(R.string.error_info_cant_send_folder)
				.setPositiveButton(R.string.confirm, null).create();
				dialog.show();
				clearSelection();
				return;
			} else if (ext.equals("apk")) {
				AlertDialog dialog = new AlertDialog.Builder(mContext)
				.setMessage(R.string.error_info_cant_send_apk)
				.setPositiveButton(R.string.confirm, null).create();
				dialog.show();
				clearSelection();
				return;
			}
		}

		Intent intent = IntentBuilder.buildSendFile(selectedFileList);
		if (intent != null) {
			try {
				mFileViewListener.startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Log.e(LOG_TAG, "fail to view file: " + e.toString());
			}
		}
		clearSelection();
	}

	boolean isUsualFile;
	public void onOperationRename() {
		boolean isFavorite = false;
		for (FileInfo fileInfo : getSelectedFileList()) {
			FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper
					.getInstance();
			if (databaseHelper != null && fileInfo != null) {
				isFavorite = databaseHelper.isFavorite(fileInfo.filePath);
				break;
			}
		}
		if (isFavorite) {
			Toast.makeText(mContext, R.string.cancleFavorite, Toast.LENGTH_SHORT).show();
			clearSelection();
			return;
		}
		int pos = mListViewContextMenuSelectedItem;
		if (pos == -1)
			return;

		if (getSelectedFileList().size() == 0)
			return;

		final FileInfo f = getSelectedFileList().get(0);
		clearSelection();
		isUsualFile = false;
		if (f.fileName.contains(".")) {
			isUsualFile = true;
		}

		TextInputDialog dialog = new TextInputDialog(mContext,
				mContext.getString(R.string.operation_rename),
				mContext.getString(R.string.operation_rename_message),
				f.fileName, new OnFinishListener() {
			@Override
			public boolean onFinish(final String text) {
				if (text.equals(f.fileName)) {
					return true;
				}
				String newExtension = "";
				String oldExtension = "";
				if (isUsualFile) {
					oldExtension = f.fileName.substring(
							f.fileName.lastIndexOf(".") + 1,
							f.fileName.length());
				}
				if (text.contains(".")) {
					newExtension = text.substring(
							text.lastIndexOf(".") + 1, text.length());
				}
				if (isUsualFile && !newExtension.equals(oldExtension)) {
					new AlertDialog.Builder(mContext)
					.setTitle(R.string.rename_confirm)
					.setMessage(
							R.string.rename_confirm_introduction)
							.setNegativeButton(R.string.cancel, null)
							.setPositiveButton(
									R.string.confirm,
									new android.content.DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											doRename(f, text);
										}
									}).show();
				} else {
					doRename(f, text);
				}
				return true;
			}

		});
		dialog.show();
		if (f.IsDir) {
			dialog.setEditTextFilter(20);
		} else {
			dialog.setEditTextFilter(f.fileName.length() + 10);
		}

	}

	private boolean doRename(final FileInfo f, String text) {
		if (TextUtils.isEmpty(text))
			return false;

		if (mFileOperationHelper.Rename(f, text)) {
			String newPath = Util.makePath(
					Util.getPathFromFilepath(f.filePath), text);
			/* PRIZE-update sound recorder database-liufan-2015-05-23-start */
			// updateMediaDB(new File(f.filePath),mContext,text,newPath);
			/* PRIZE-update sound recorder database-liufan-2015-05-23-end */
			f.fileName = text;
			f.filePath = newPath;
			// mFileViewListener.onDataChanged();
		} else {
			if (text.contains("/")) {
				new AlertDialog.Builder(mContext)
				.setMessage(mContext.getString(R.string.fail_to_rename_char))
				.setPositiveButton(R.string.confirm, null).create().show();
				return false;
			}
			new AlertDialog.Builder(mContext)
			.setMessage(mContext.getString(R.string.fail_to_rename))
			.setPositiveButton(R.string.confirm, null).create().show();
			return false;
		}

		return true;
	}

	/* PRIZE-update sound recorder database-liufan-2015-05-23-start */
	private static void updateMediaDB(File file, Context context, String text,
			String newPath) {
		ContentResolver resolver = context.getContentResolver();
		Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		final String[] ids = new String[] { MediaStore.Audio.Media._ID };
		final String where = MediaStore.Audio.Media.DATA + " = ?";
		String[] selectionArgs = new String[] { file.getAbsolutePath() };
		Cursor cursor = null;
		try {
			cursor = resolver.query(base, ids, where, selectionArgs, null);
			if ((null != cursor) && (cursor.getCount() > 0)) {
				ContentValues cv = new ContentValues();
				cv.put(MediaStore.Audio.Media.IS_RECORD, "1");
				cv.put(MediaStore.Audio.Media.DISPLAY_NAME, text);
				cv.put(MediaStore.Audio.Media.DATA, newPath);
				// ContentResolver resolver = context.getContentResolver();
				String selection = MediaStore.Audio.Media.DATA + " = ?";
				// String[] selectionArgs = new
				// String[]{file.getAbsolutePath()};
				try {
					// resolver.insert(base, cv);
					resolver.update(base, cv, selection, selectionArgs);
				} catch (UnsupportedOperationException e) {
				}
			} else {
				if (cursor == null) {
				} else {
				}
			}
		} catch (IllegalStateException e) {

		} catch (SQLiteFullException e) {

		} finally {
			if (null != cursor) {
				cursor.close();
			}
		}

	}

	/* PRIZE-update sound recorder database-liufan-2015-05-23-end */

	/* PRIZE-notify-xiaxuefeng-2015-4-2-start */
	private void notifyFileSystemChanged(String path) {
		if (path == null)
			return;
		// final File f = new File(path);
		// final Intent intent;
		// if (f.isDirectory()) {
		/*
		 * intent = new Intent(Intent.ACTION_MEDIA_MOUNTED);
		 * intent.setClassName("com.android.providers.media",
		 * "com.android.providers.media.MediaScannerReceiver"); intent.setData
		 * (Uri.fromFile(Environment.getExternalStorageDirectory()));
		 * Log.v(LOG_TAG, "directory changed, send broadcast:" +
		 * intent.toString());
		 */
		// String sdIn = Environment.getExternalStorageDirectory().getPath();
		// String root = sdIn.
		// substring(0, sdIn.lastIndexOf("/"));
		org.swiftp.Util.newFileNotify(path);
		// Log.d("xxf", "scan-path:"+ path);
		// refreshFileList();
		/*
		 * MediaScannerConnection.scanFile(mContext, new String[] { root },
		 * null, new MediaScannerConnection.OnScanCompletedListener() {
		 * 
		 * @Override public void onScanCompleted(String path, Uri uri) {
		 * Log.d(LOG_TAG, "onScanCompleted");
		 * 
		 * FileViewInteractionHub.this.onFinish(); } }); Log.d(LOG_TAG,
		 * "directory" + Environment.getExternalStorageDirectory().getPath());
		 */
		// } else {
		/*
		 * intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		 * intent.setData(Uri.fromFile(new File(path))); Log.v(LOG_TAG,
		 * "file changed, send broadcast:" + intent.toString());
		 */
		// org.swiftp.Util.newFileNotify(path);
		// refreshFileList();
		/*
		 * MediaScannerConnection.scanFile(mContext, new String[] { path },
		 * null, new MediaScannerConnection.OnScanCompletedListener() {
		 * 
		 * @Override public void onScanCompleted(String path, Uri uri) {
		 * Log.d(LOG_TAG, "onScanCompleted");
		 * 
		 * FileViewInteractionHub.this.onFinish(); } }); Log.d(LOG_TAG, path);
		 */
		// }
		/*
		 * new Thread(new Runnable(){ public void run(){ try {
		 * Thread.sleep(10000); } catch (InterruptedException e) {
		 * e.printStackTrace(); } handler.sendEmptyMessage(111); } }).start();
		 */
	}

	/* PRIZE-notify-xiaxuefeng-2015-4-2-end */

	public void onOperationDelete() {
		Log.d("hky", "FileViewInteractionHub onOperationDelete....");
		boolean isFavorite = false;
		for (FileInfo fileInfo : getSelectedFileList()) {
			FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper
					.getInstance();
			if (databaseHelper != null && fileInfo != null) {
				isFavorite = databaseHelper.isFavorite(fileInfo.filePath);
				break;
			}
		}
		if (!isFavorite) {
			doOperationDelete(getSelectedFileList());
		} else {
			Toast.makeText(mContext, R.string.cancleFavorite, Toast.LENGTH_SHORT).show();
			clearSelection();
		}

	}

	public void onOperationDelete(int position) {
		Log.d("hky", "FileViewInteractionHub onOperationDelete(int)....");
		FileInfo file = mFileViewListener.getItem(position);
		if (file == null)
			return;

		ArrayList<FileInfo> selectedFileList = new ArrayList<FileInfo>();
		selectedFileList.add(file);
		doOperationDelete(selectedFileList);
	}

	/* PRIZE-delete-xiaxuefeng-2015-4-2-start */
	private void doOperationDelete(final ArrayList<FileInfo> selectedFileList) {
		final ArrayList<FileInfo> selectedFiles = new ArrayList<FileInfo>(
				selectedFileList);
		Dialog dialog = new AlertDialog.Builder(mContext)
		.setMessage(
				mContext.getString(R.string.operation_delete_confirm_message))
				.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						if (mFileOperationHelper.Delete(selectedFiles)) {
							showProgress(mContext
									.getString(R.string.operation_deleting));
						}
						clearSelection();
					}
				})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						clearSelection();
					}
				}).create();
		dialog.show();
	}

	/* PRIZE-delete-xiaxuefeng-2015-4-2-end */

	public void onOperationInfo() {
		if (getSelectedFileList().size() == 0)
			return;

		FileInfo file = getSelectedFileList().get(0);
		if (file == null)
			return;

		InformationDialog dialog = new InformationDialog(mContext, file,
				mFileViewListener.getFileIconHelper());
		dialog.show();
		clearSelection();
	}

	public void onOperationButtonConfirm() {
		if (isSelectingFiles()) {
			mSelectFilesCallback.selected(mCheckedFileNameList);
			mSelectFilesCallback = null;
			clearSelection();
		} else if (mFileOperationHelper.isMoveState()) {
			if (mFileOperationHelper.EndMove(mCurrentPath)) {
				mInPaste = true;
				showProgress(mContext.getString(R.string.operation_moving));
			}
		} else {
			onOperationPaste();
		}
	}

	public void onOperationButtonCancel() {
		mFileOperationHelper.clear();
		showConfirmOperationBar(false);
		if (isSelectingFiles()) {
			mSelectFilesCallback.selected(null);
			mSelectFilesCallback = null;
			clearSelection();
		} else if (mFileOperationHelper.isMoveState()) {
			// refresh to show previously selected hidden files
			mFileOperationHelper.EndMove(null);
			refreshFileList();
		} else {
			refreshFileList();
		}
	}

	// context menu
	private OnCreateContextMenuListener mListViewContextMenuListener = new OnCreateContextMenuListener() {
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {
			boolean isFavorite = false;
			if (isInSelection() || isMoveState())
				return;
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

			FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper
					.getInstance();
			FileInfo file = mFileViewListener.getItem(info.position);
			if (file == null) return;
			if (file != null && (inSD != null && file.filePath.equals(inSD)
					|| outSD != null && file.filePath.equals(outSD)
					|| file.filePath.equals("/storage/sdcard0"))) {
				return;
			}
			if (databaseHelper != null && file != null && file.filePath != null) {
				isFavorite = databaseHelper.isFavorite(file.filePath);
				int stringId = isFavorite ? R.string.operation_unfavorite
						: R.string.operation_favorite;
				addMenuItem(menu, GlobalConsts.MENU_FAVORITE, 0, stringId);
			}
			if (mFileViewListener instanceof FileViewActivity) {
				addMenuItem(menu, GlobalConsts.MENU_COPY, 0,
						R.string.operation_copy);
				addMenuItem(menu, GlobalConsts.MENU_COPY_PATH, 0,
						R.string.operation_copy_path);
				addMenuItem(menu, GlobalConsts.MENU_MOVE, 0,
						R.string.operation_move);
			}
			// addMenuItem(menu, GlobalConsts.MENU_COPY, 0,
			// R.string.operation_copy);
			// addMenuItem(menu, GlobalConsts.MENU_COPY_PATH, 0,
			// R.string.operation_copy_path);
			// addMenuItem(menu, GlobalConsts.MENU_PASTE, 0,
			// R.string.operation_paste);
			// addMenuItem(menu, GlobalConsts.MENU_MOVE, 0,
			// R.string.operation_move);
			addMenuItem(menu, MENU_SEND, 0, R.string.operation_send);
			addMenuItem(menu, MENU_RENAME, 0, R.string.operation_rename);
			addMenuItem(menu, MENU_DELETE, 0, R.string.operation_delete);
			addMenuItem(menu, MENU_INFO, 0, R.string.operation_info);

			if (!canPaste()) {
				MenuItem menuItem = menu.findItem(GlobalConsts.MENU_PASTE);
				if (menuItem != null)
					menuItem.setEnabled(false);
			}
		}
	};

	// File List view setup
	private ListView mFileListView;

	private int mListViewContextMenuSelectedItem;

	private void setupFileListView() {
		mFileListView = (ListView) mFileViewListener
				.getViewById(R.id.file_path_list);
		mFileListView.setLongClickable(true);

		mFileListView
		.setOnCreateContextMenuListener(mListViewContextMenuListener);
		mFileListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				onListItemClick(parent, view, position, id);
			}
		});
	}

	// menu
	private static final int MENU_SEARCH = 1;

	// private static final int MENU_NEW_FOLDER = 2;
	private static final int MENU_SORT = 3;

	private static final int MENU_SEND = 7;

	private static final int MENU_RENAME = 8;

	private static final int MENU_DELETE = 9;

	private static final int MENU_INFO = 10;

	private static final int MENU_SORT_NAME = 11;

	private static final int MENU_SORT_SIZE = 12;

	private static final int MENU_SORT_DATE = 13;

	private static final int MENU_SORT_TYPE = 14;

	private static final int MENU_REFRESH = 15;

	private static final int MENU_SELECTALL = 16;

	private static final int MENU_SETTING = 17;

	private static final int MENU_EXIT = 18;

	private OnMenuItemClickListener menuItemClick = new OnMenuItemClickListener() {

		@Override
		public boolean onMenuItemClick(MenuItem item) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
					.getMenuInfo();
			mListViewContextMenuSelectedItem = info != null ? info.position
					: -1;

			int itemId = item.getItemId();
			if (mFileViewListener.onOperation(itemId)) {
				return true;
			}

			addContextMenuSelectedItem();

			switch (itemId) {
			case MENU_SEARCH:
				onOperationSearch();
				break;
			case GlobalConsts.MENU_NEW_FOLDER:
				onOperationCreateFolder();
				break;
			case MENU_REFRESH:
				onOperationReferesh();
				break;
			case MENU_SELECTALL:
				onOperationSelectAllOrCancel();
				break;
			case GlobalConsts.MENU_SHOWHIDE:
				onOperationShowSysFiles();
				break;
			case GlobalConsts.MENU_FAVORITE:
				onOperationFavorite();
				break;
			case MENU_SETTING:
				onOperationSetting();
				break;
			case MENU_EXIT:
				((FileExplorerTabActivity) mContext).finish();
				break;
				// sort
			case MENU_SORT_NAME:
				item.setChecked(true);
				onSortChanged(SortMethod.name);
				break;
			case MENU_SORT_SIZE:
				item.setChecked(true);
				onSortChanged(SortMethod.size);
				break;
			case MENU_SORT_DATE:
				item.setChecked(true);
				onSortChanged(SortMethod.date);
				break;
			case MENU_SORT_TYPE:
				item.setChecked(true);
				onSortChanged(SortMethod.type);
				break;

			case GlobalConsts.MENU_COPY:
				onOperationCopy();
				break;
			case GlobalConsts.MENU_COPY_PATH:
				onOperationCopyPath();
				break;
			case GlobalConsts.MENU_PASTE:
				onOperationPaste();
				break;
			case GlobalConsts.MENU_MOVE:
				onOperationMove();
				break;
			case MENU_SEND:
				onOperationSend();
				break;
			case MENU_RENAME:
				onOperationRename();
				break;
			case MENU_DELETE:
				onOperationDelete();
				break;
			case MENU_INFO:
				onOperationInfo();
				break;
			default:
				return false;
			}

			mListViewContextMenuSelectedItem = -1;
			return true;
		}

	};

	private com.android.fileexplorer.FileViewInteractionHub.Mode mCurrentMode;

	private String mCurrentPath;

	private String mRoot;

	private SelectFilesCallback mSelectFilesCallback;

	public boolean onCreateOptionsMenu(Menu menu) {
		clearSelection();
		// menu.add(0, MENU_SEARCH, 0,
		// R.string.menu_item_search).setOnMenuItemClickListener(
		// menuItemClick);
		// addMenuItem(menu, MENU_SELECTALL, 0, R.string.operation_selectall,
		// R.drawable.ic_menu_select_all);

		// SubMenu sortMenu = menu.addSubMenu(0, MENU_SORT, 1,
		// R.string.menu_item_sort).setIcon(
		// R.drawable.ic_menu_sort);
		// addMenuItem(sortMenu, MENU_SORT_NAME, 0,
		// R.string.menu_item_sort_name);
		// addMenuItem(sortMenu, MENU_SORT_SIZE, 1,
		// R.string.menu_item_sort_size);
		// addMenuItem(sortMenu, MENU_SORT_DATE, 2,
		// R.string.menu_item_sort_date);
		// addMenuItem(sortMenu, MENU_SORT_TYPE, 3,
		// R.string.menu_item_sort_type);
		// sortMenu.setGroupCheckable(0, true, true);
		// sortMenu.getItem(0).setChecked(true);

		// addMenuItem(menu, GlobalConsts.MENU_PASTE, 2,
		// R.string.operation_paste);
		addMenuItem(menu, GlobalConsts.MENU_NEW_FOLDER, 3,
				R.string.operation_create_folder, R.drawable.ic_menu_new_folder);
		// addMenuItem(menu, GlobalConsts.MENU_FAVORITE, 4,
		// R.string.operation_favorite,
		// R.drawable.ic_menu_delete_favorite);
		// addMenuItem(menu, GlobalConsts.MENU_SHOWHIDE, 5,
		// R.string.operation_show_sys,
		// R.drawable.ic_menu_show_sys);
		addMenuItem(menu, MENU_REFRESH, 6, R.string.operation_refresh,
				R.drawable.ic_menu_refresh);
		// addMenuItem(menu, MENU_SETTING, 7, R.string.menu_setting,
		// drawable.ic_menu_preferences);
		addMenuItem(menu, MENU_EXIT, 8, R.string.menu_exit,
				drawable.ic_menu_close_clear_cancel);
		return true;
	}

	private void addMenuItem(Menu menu, int itemId, int order, int string) {
		addMenuItem(menu, itemId, order, string, -1);
	}

	private void addMenuItem(Menu menu, int itemId, int order, int string,
			int iconRes) {
		if (!mFileViewListener.shouldHideMenu(itemId)) {
			MenuItem item = menu.add(0, itemId, order, string)
					.setOnMenuItemClickListener(menuItemClick);
			if (iconRes > 0) {
				item.setIcon(iconRes);
			}
		}
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		updateMenuItems(menu);
		return true;
	}

	private void updateMenuItems(Menu menu) {
		/*
		 * menu.findItem(MENU_SELECTALL).setTitle( isSelectedAll() ?
		 * R.string.operation_cancel_selectall : R.string.operation_selectall);
		 * menu.findItem(MENU_SELECTALL).setEnabled(mCurrentMode != Mode.Pick);
		 */
		boolean canCreateFolder = !mCurrentPath.equals("/storage");
		if (canCreateFolder) {
			if (menu.findItem(GlobalConsts.MENU_NEW_FOLDER) == null) {
				addMenuItem(menu, GlobalConsts.MENU_NEW_FOLDER, 3,
						R.string.operation_create_folder,
						R.drawable.ic_menu_new_folder);
			}
		} else {
			menu.removeItem(GlobalConsts.MENU_NEW_FOLDER);
		}

		// menu.findItem(GlobalConsts.MENU_NEW_FOLDER).setEnabled(canCreateFolder);
		/*
		 * MenuItem menuItem = menu.findItem(GlobalConsts.MENU_SHOWHIDE); if
		 * (menuItem != null) {
		 * menuItem.setTitle(Settings.instance().getShowDotAndHiddenFiles() ?
		 * R.string.operation_hide_sys : R.string.operation_show_sys); }
		 */

		/*
		 * FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper
		 * .getInstance(); if (databaseHelper != null) { MenuItem item =
		 * menu.findItem(GlobalConsts.MENU_FAVORITE); if (item != null) {
		 * item.setTitle(databaseHelper.isFavorite(mCurrentPath) ?
		 * R.string.operation_unfavorite : R.string.operation_favorite); } }
		 */

	}

	public boolean isFileSelected(String filePath) {
		return mFileOperationHelper.isFileSelected(filePath);
	}

	public void setMode(Mode m) {
		mCurrentMode = m;
	}

	public Mode getMode() {
		return mCurrentMode;
	}

	public void onListItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		FileInfo lFileInfo = mFileViewListener.getItem(position);
		if (lFileInfo == null) {
			Log.e(LOG_TAG, "file does not exist on position:" + position);
			Toast.makeText(mContext, R.string.fail_to_open, Toast.LENGTH_SHORT).show();
			return;
		}else{
			String path = lFileInfo.filePath;
			File currentFile = new File(path);
			if(!currentFile.exists()){
				return;
			}
		}

		if (isInSelection()) {
			boolean selected = lFileInfo.Selected;
			ActionMode actionMode = ((FileExplorerTabActivity) mContext)
					.getActionMode();
			ImageView checkBox = (ImageView) view
					.findViewById(R.id.file_checkbox);
			if (selected) {
				mCheckedFileNameList.remove(lFileInfo);
				checkBox.setImageResource(R.drawable.btn_check_off_holo_light);
			} else {
				mCheckedFileNameList.add(lFileInfo);
				checkBox.setImageResource(R.drawable.btn_check_on_holo_light);
			}
			if (actionMode != null) {
				if (mCheckedFileNameList.size() == 0)
					actionMode.finish();
				else
					actionMode.invalidate();
			}
			lFileInfo.Selected = !selected;

			Util.updateActionModeTitle(actionMode, mContext,
					mCheckedFileNameList.size());
			return;
		}

		if (!lFileInfo.IsDir) {
			if (mCurrentMode == Mode.Pick) {
				mFileViewListener.onPick(lFileInfo);
			} else {
				if (mFileViewListener instanceof FileViewActivity) {
					viewFile(lFileInfo);
				} else {
					categoryViewFile(lFileInfo);
				}
			}
			return;
		}
		mCurrentPath = getAbsoluteName(mCurrentPath, lFileInfo.fileName);
		ActionMode actionMode = ((FileExplorerTabActivity) mContext)
				.getActionMode();
		if (actionMode != null) {
			actionMode.finish();
		}
		refreshFileList();
	}

	public void setRootPath(String path) {
		mRoot = path;
		mCurrentPath = path;
	}

	public String getRootPath() {
		return mRoot;
	}

	public String getCurrentPath() {
		return mCurrentPath;
	}

	public void setCurrentPath(String path) {
		mCurrentPath = path;
	}

	private String getAbsoluteName(String path, String name) {
		if (inSD != null && inSD.contains("/emulated/")) {
			String[] names = inSD.split("/");
			if ("sdcard0".equals(name)) {
				name = names[2] + "/" + names[3];
			}
		}
		return path.equals(GlobalConsts.ROOT_PATH) ? path + name : path
				+ File.separator + name;
	}

	// check or uncheck
	public boolean onCheckItem(FileInfo f, View v) {
		if (isMoveState())
			return false;

		if (isSelectingFiles() && f.IsDir)
			return false;

		if (f.Selected) {
			mCheckedFileNameList.add(f);
		} else {
			mCheckedFileNameList.remove(f);
		}
		return true;
	}

	private boolean isSelectingFiles() {
		return mSelectFilesCallback != null;
	}

	public boolean isSelectedAll() {
		return mFileViewListener.getItemCount() != 0
				&& mCheckedFileNameList.size() == mFileViewListener
				.getItemCount();
	}

	public boolean isSelected() {
		return mCheckedFileNameList.size() != 0;
	}

	public void clearSelection() {
		Log.d(LOG_TAG, "clearSelection........");
		if (mCheckedFileNameList.size() > 0) {
			for (FileInfo f : mCheckedFileNameList) {
				if (f == null) {
					continue;
				}
				f.Selected = false;
			}
			mCheckedFileNameList.clear();
			mFileViewListener.onDataChanged();
		}
	}
	private void viewFile(FileInfo lFileInfo) {
		try {
			IntentBuilder.viewFile(mContext, lFileInfo.filePath);
		} catch (ActivityNotFoundException e) {
			Log.e(LOG_TAG, "fail to view file: " + e.toString());
		}
	}

	private void categoryViewFile(FileInfo lFileInfo) {
		try {
			IntentBuilder.categoryViewFile(mContext, lFileInfo.filePath);
		} catch (ActivityNotFoundException e) {
			Log.e(LOG_TAG, "fail to view file: " + e.toString());
		}
	}

	public void onResume() {
		if (progressDialog != null) {
			progressDialog.show();
		}
	}

	public void onPause() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}

	public boolean onBackPressed() {
		if (mInPaste) {
			Util.setInPaste(false);
			onFinish();
			return true;
		}
		if (isInSelection()) {
			clearSelection();
		} else if (!onOperationUpLevel()) {
			return false;
		}
		return true;
	}

	public void copyFile(ArrayList<FileInfo> files) {
		mFileOperationHelper.Copy(files);
	}

	public void moveFileFrom(ArrayList<FileInfo> files) {
		mFileOperationHelper.StartMove(files);
		showConfirmOperationBar(true);
		updateConfirmButtons();
		// refresh to hide selected files
		refreshFileList();
	}

	/*
	 * private void showDropdownNavigation(boolean show) {
	 * mDropdownNavigation.setVisibility(show ? View.VISIBLE : View.GONE);
	 * mNavigationBarUpDownArrow
	 * .setImageResource(mDropdownNavigation.getVisibility() == View.VISIBLE ?
	 * R.drawable.arrow_up : R.drawable.arrow_down); }
	 */

	@Override
	public void onFileChanged(String path) {
		mInPaste = false;
		notifyFileSystemChanged(path);
	}

	public void startSelectFiles(SelectFilesCallback callback) {
		mSelectFilesCallback = callback;
		showConfirmOperationBar(true);
		updateConfirmButtons();
	}

}
