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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

public class FileOperationHelper {
    private static final String LOG_TAG = "FileOperation";

    private ArrayList<FileInfo> mCurFileNameList = new ArrayList<FileInfo>();

    private boolean mMoving;

    private IOperationProgressListener mOperationListener;

    private FilenameFilter mFilter;
    private Context mContext;
    private static FileExplorerService mFileExplorerService;
    public interface IOperationProgressListener {
        void onFinish();

        void onFileChanged(String path);
    }
    private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
				updateMediaDB(mContext,(String) msg.obj);
		}
	};
    public FileOperationHelper(IOperationProgressListener l, Context context) {
        mOperationListener = l;
        this.mContext = context;
    }

	public static void setService(FileExplorerService serviceBinder) {
		mFileExplorerService = serviceBinder;
	}
	public void setFilenameFilter(FilenameFilter f) {
        mFilter = f;
    }

    public String CreateFolder(String path, String name) {
        Log.v(LOG_TAG, "CreateFolder >>> " + path + "," + name);
        String exist = "exist";
        File f = new File(Util.makePath(path, name));
        if (f.exists())
            return exist;
        if (f.mkdir()) {
        	return "success";
        } else {
        	return "error";
        }
    }

    public void Copy(ArrayList<FileInfo> files) {
        copyFileList(files);
    }

    public boolean Paste(String path) {
        if (mCurFileNameList.size() == 0)
        return false;
        if (mFileExplorerService != null) {
        	mFileExplorerService.Paste(mCurFileNameList, mOperationListener, path);
        }
        return true;
    }

    public boolean canPaste() {
        return mCurFileNameList.size() != 0;
    }

    public void StartMove(ArrayList<FileInfo> files) {
        if (mMoving)
            return;

        mMoving = true;
        copyFileList(files);
    }

    public boolean isMoveState() {
        return mMoving;
    }

    public boolean canMove(String path) {
        for (FileInfo f : mCurFileNameList) {
            if (!f.IsDir)
                continue;

            if (Util.containsPath(f.filePath, path))
                return false;
        }

        return true;
    }

    public void clear() {
    	Log.d(LOG_TAG, "clear ........");
        synchronized(mCurFileNameList) {
            mCurFileNameList.clear();
        }
    }

    public boolean EndMove(String path) {
        if (!mMoving)
            return false;
        mMoving = false;

        if (TextUtils.isEmpty(path))
            return false;
        if (mFileExplorerService != null) {
        	mFileExplorerService.EndMove(mCurFileNameList, mOperationListener, path);
        }
        return true;
    }

    public ArrayList<FileInfo> getFileList() {
        return mCurFileNameList;
    }

    private void asnycExecute(Runnable r) {
        final Runnable _r = r;
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object... params) {
                synchronized(mCurFileNameList) {
                    _r.run();
                }
                if (mOperationListener != null) {
                    mOperationListener.onFinish();
                }

                return null;
            }
        }.execute();
    }

    public boolean isFileSelected(String path) {
        synchronized(mCurFileNameList) {
            for (FileInfo f : mCurFileNameList) {
                if (f.filePath.equalsIgnoreCase(path))
                    return true;
            }
        }
        return false;
    }

    public boolean Rename(FileInfo f, String newName) {
    	boolean isRecorderFile = false;
        if (f == null || newName == null) {
            Log.e(LOG_TAG, "Rename: null parameter");
            return false;
        }
        if (queryMediaDB(mContext,f.filePath)) {
        	isRecorderFile = true;
        }
        File file = new File(f.filePath);
        final boolean isDir = file.isDirectory();
        if (isDir) {
        	mOldPaths.clear();
        	getOldPaths(f.filePath);
        }
        final String newPath = Util.makePath(Util.getPathFromFilepath(f.filePath), newName);
        //final boolean needScan = file.isFile() && !f.filePath.equals(newPath);
        final boolean needScan = !f.filePath.equals(newPath);
        try {
        	File newFile = new File(newPath);
        	if (newFile.exists()) return false;
            boolean ret = file.renameTo(new File(newPath));
            if (ret) {
                if (needScan) {
                	/*SharedPreferences preferences = mContext.getSharedPreferences("mp4Setting", 0);
                	String oldExtension = f.fileName.contains(".") ? f.fileName.substring(
                			f.fileName.lastIndexOf(".") + 1, f.fileName.length()) : "";
					String newExtension = newName.contains(".") ? newName.substring(
							newName.lastIndexOf(".") + 1, newName.length()) : "";
					if (newExtension.equals("mp4") && 
							(MediaFile.isAudioFileType(f.filePath) || 
							preferences.getBoolean(oldExtension, false))) {
						SharedPreferences.Editor editor = preferences.edit();
						editor.putBoolean(newName, true);
						editor.commit();
					}
					if(preferences.getBoolean(oldExtension, false)) {
						deleteAudioDBFile(f.filePath);
					} else {
						deleteDBFile(f.filePath);
					}*/
					deleteDBFile(f.filePath);
                    //mOperationListener.onFileChanged(f.filePath);
                	if (isDir && mOldPaths.size() > 0) {
                		for (int i = 0; i < mOldPaths.size(); i++) {
                			deleteDBFile(mOldPaths.get(i));
						}
                	}
                	//updateInMediaStore(f.filePath, newPath);
                }
                mOperationListener.onFileChanged(newPath);
                if (isRecorderFile) {
                	new Thread(new Runnable(){    
       			     public void run(){    
       			         try {
       						Thread.sleep(3000);
       					} catch (InterruptedException e) {
       						e.printStackTrace();
       					} 
       			         Message msg = handler.obtainMessage();
       			         msg.obj = newPath;
       			         handler.sendMessage(msg);
       			     }
       			 }).start();
            	}
            }
            return ret;
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Fail to rename file," + e.toString());
        }
        return false;
    }
    
    private void deleteAudioDBFile(String filePath) {
    	String volumeName = "external";
    	mContext.getContentResolver().delete(MediaStore.Audio.Media.getContentUri(volumeName),
    			"_DATA=?",new String[]{filePath});
	}

	private void deleteDBFile(String filePath) {
    	String volumeName = "external";
    	/*if (MediaFile.isImageFileType(filePath)) {
			mContext.getContentResolver().delete(MediaStore.Images.Media.getContentUri(volumeName),
        			"_DATA=?",new String[]{filePath});
		}else if (MediaFile.isAudioFileType(filePath)){
			mContext.getContentResolver().delete(MediaStore.Audio.Media.getContentUri(volumeName),
        			"_DATA=?",new String[]{filePath});
		}else if (MediaFile.isVideoFileType(filePath)) {
			mContext.getContentResolver().delete(MediaStore.Video.Media.getContentUri(volumeName),
        			"_DATA=?",new String[]{filePath});
		}else {
			mContext.getContentResolver().delete(MediaStore.Files.getContentUri(volumeName),
        			"_DATA=?",new String[]{filePath});
		}*/
    	mContext.getContentResolver().delete(MediaStore.Files.getContentUri(volumeName),
    			"_DATA=?",new String[]{filePath});
    }
    
    List<String> mOldPaths = new ArrayList<String>();
    private void getOldPaths(String filePath) {
    	File[] oldFiles = new File(filePath).listFiles();
    	for (File file : oldFiles) {
			if (file.isDirectory()) {
				getOldPaths(file.getPath());
			} else {
				mOldPaths.add(file.getPath());
			}
		}
	}

	/*public void updateInMediaStore(String newPath, String oldPath) {
        if (mContext != null && !TextUtils.isEmpty(newPath) && !TextUtils.isEmpty(newPath)) {
            Uri uri = MediaStore.Files.getMtpObjectsUri("external");
            uri = uri.buildUpon().appendQueryParameter("need_update_media_values", "true").build();
            String where = MediaStore.Files.FileColumns.DATA + "=?";
            String[] whereArgs = new String[] { oldPath };
            ContentResolver cr = mContext.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Files.FileColumns.DATA, newPath);
            whereArgs = new String[] { oldPath };
            cr.update(uri, values, where, whereArgs);
        }
    }*/
    
    private void updateMediaDB(Context context, String newPath) {
    	ContentResolver resolver = context.getContentResolver();
    	Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    	String[] selectionArgs = new String[]{newPath};
    	ContentValues cv = new ContentValues();
		cv.put(MediaStore.Audio.Media.IS_RECORD, "1");
		String selection = MediaStore.Audio.Media.DATA + " = ?";
		try {
			resolver.update(base, cv,selection,selectionArgs);
		} catch (Exception e) {
			Log.e("xxf", "Sql Error");
		}
	}

	private boolean queryMediaDB(Context context, String path) {
		ContentResolver resolver = context.getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String[] ids = new String[] { MediaStore.Audio.Media._ID };
        final String where = MediaStore.Audio.Media.DATA + " = ? and " 
        		+ MediaStore.Audio.Media.IS_RECORD + "= ?";
        String[] selectionArgs = new String[]{path,"1"};
        Cursor cursor = null;
        cursor = resolver.query(base, ids, where, selectionArgs, null);
        if ((null != cursor) && (cursor.getCount() > 0)) {
        	cursor.close();
        	return true;
        }
        cursor.close();
        return false;
	}

	public boolean Delete(ArrayList<FileInfo> files) {
    	Log.d("hky", "FileOperationHelper  delete file ");
        copyFileList(files);
        asnycExecute(new Runnable() {
            @Override
            public void run() {
            	Log.d("hky", "int the thread of delete file");
                for (FileInfo f : mCurFileNameList) {
                    DeleteFile(f);
                    mOperationListener.onFileChanged(f.filePath);
                }

                clear();
            }
        });
        return true;
    }

    protected void DeleteFile(FileInfo f) {
    	Log.d(LOG_TAG, "DeleteFile: begin delete...");    	
        if (f == null) {
            Log.e(LOG_TAG, "DeleteFile: null parameter");
            return;
        }

        File file = new File(f.filePath);
        boolean directory = file.isDirectory();
        if (directory) {
            for (File child : file.listFiles(mFilter)) {
                if (Util.isNormalFile(child.getAbsolutePath())) {
                    DeleteFile(Util.GetFileInfo(child, mFilter, true));
                }
            }
        }
        String volumeName = "external";
		/*if (MediaFile.isImageFileType(f.filePath)) {
			mContext.getContentResolver().delete(MediaStore.Images.Media.getContentUri(volumeName),
        			"_DATA=?",new String[]{f.filePath});
		}else if (MediaFile.isAudioFileType(f.filePath)){
			mContext.getContentResolver().delete(MediaStore.Audio.Media.getContentUri(volumeName),
        			"_DATA=?",new String[]{f.filePath});
		}else if (MediaFile.isVideoFileType(f.filePath)) {
			mContext.getContentResolver().delete(MediaStore.Video.Media.getContentUri(volumeName),
        			"_DATA=?",new String[]{f.filePath});
		}else {
			mContext.getContentResolver().delete(MediaStore.Files.getContentUri("external"),
        			"_DATA=?",new String[]{f.filePath});
		}*/
        mContext.getContentResolver().delete(MediaStore.Files.getContentUri(volumeName),
    			"_DATA=?",new String[]{f.filePath});
		if (file.exists()) {
			file.delete();
		}
        Log.v(LOG_TAG, "DeleteFile >>> " + f.filePath);
    }

    private void copyFileList(ArrayList<FileInfo> files) {
        synchronized(mCurFileNameList) {
            mCurFileNameList.clear();
            for (FileInfo f : files) {
                mCurFileNameList.add(f);
            }
        }
    }



}
