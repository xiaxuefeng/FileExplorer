/*****************************************
*版权所有©2015,深圳市铂睿智恒科技有限公司
*
*文件名称：FileExplorerService
*内容摘要：文件管理器的service类
*当前版本：1.0
*作	者：xiaxuefeng
*完成日期：2015-7-13
*修改记录：
*修改日期：
*版 本 号：
*修 改 人：
*修改内容：
...
*修改记录：
*修改日期：
*版 本 号：
*修 改 人：
*修改内容：
********************************************/
package com.android.fileexplorer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import com.android.fileexplorer.FileOperationHelper.IOperationProgressListener;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
public class FileExplorerService extends Service{
	private final IBinder binder = new MyBinder();
	public class MyBinder extends Binder{
		FileExplorerService getService() {
			return FileExplorerService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	private FilenameFilter mFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String filename) {
			return false;
		}
	};
	
    
    private void asnycExecute(Runnable r, final ArrayList<FileInfo> curFileNameList, 
    		final IOperationProgressListener operationListener) {
        final Runnable _r = r;
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object... params) {
                synchronized(curFileNameList) {
                	Log.d("xxf", "doInBackground");
                    _r.run();
                }
                if (operationListener != null) {
                    operationListener.onFinish();
                }

                return null;
            }
        }.execute();
    }
    
	private boolean CopyFile(FileInfo f, String dest) {
        if (f == null || dest == null) {
            return false;
        }

        File file = new File(f.filePath);
        if (file.isDirectory()) {
        	if (file.listFiles().length == 0) {
        		return Util.copyFile(f.filePath, dest);
        	}
            // directory exists in destination, rename it
            String destPath = Util.makePath(dest, f.fileName);
            File destFile = new File(destPath);
            int i = 1;
            while (destFile.exists()) {
                destPath = Util.makePath(dest, f.fileName + " " + i++);
                destFile = new File(destPath);
            }

            for (File child : file.listFiles()) {
                if (!child.isHidden() && Util.isNormalFile(child.getAbsolutePath())) {
                	CopyFile(Util.GetFileInfo(child, mFilter, Settings.instance().
                    		getShowDotAndHiddenFiles()), destPath);
                }
            }
            return true;
        } else {
        	return Util.copyFile(f.filePath, dest);
        }
    }
	private ArrayList<FileInfo> mCurFileNameList;
	public void Paste(final ArrayList<FileInfo> curFileNameList,
			final IOperationProgressListener operationListener, String path) {
		final String _path = path;
		this.mCurFileNameList = curFileNameList;
		Runnable runnable = new Runnable() {
	            @Override
	            public void run() {
	                for (FileInfo f : mCurFileNameList) {
	                    if(!CopyFile(f, _path)) {
	                    	break;
	                    }
	                }
	                
	                operationListener.onFileChanged(_path);
	                clear();
	            }
	        };
        asnycExecute(runnable, curFileNameList, operationListener);
	}

	public void EndMove(final ArrayList<FileInfo> curFileNameList,
			final IOperationProgressListener operationListener, String path) {
		final String _path = path;
		this.mCurFileNameList = curFileNameList;
		Runnable runnable = new Runnable() {
            @Override
            public void run() {
                    for (FileInfo f : mCurFileNameList) {
                        if (!MoveFile(f, _path)) {
                        	String newPath = Util.makePath(_path, f.fileName);
                        	
                        	if (f!=null && _path!=null 
                        			&& !f.filePath.equals(newPath)) {
                        		if(!CopyFile(f, _path)) {
        	                    	break;
        	                    }
                        		DeleteFile(f);
                        	}
                        } else {
                        	operationListener.onFileChanged(f.filePath);
                        }
                    }
                    operationListener.onFileChanged(_path);
                    clear();
                }
        };
        asnycExecute(runnable, curFileNameList, operationListener);
	}
	
    private boolean MoveFile(FileInfo f, String dest) {

        if (f == null || dest == null) {
            return false;
        }

        File file = new File(f.filePath);
        if (file.isDirectory()) {
        	return false;
        }
        String newPath = Util.makePath(dest, f.fileName);
        try {
            return file.renameTo(new File(newPath));
        } catch (SecurityException e) {
        }
        return false;
    }
    
    protected void DeleteFile(FileInfo f) {
        if (f == null) {
            return;
        }

        File file = new File(f.filePath);
        boolean directory = file.isDirectory();
        if (directory) {
            for (File child : file.listFiles()) {
                if (Util.isNormalFile(child.getAbsolutePath())) {
                    DeleteFile(Util.GetFileInfo(child, mFilter, true));
                }
            }
        }
        String volumeName = "external";
		if (MediaFile.isImageFileType(f.filePath)) {
			getContentResolver().delete(MediaStore.Images.Media.getContentUri(volumeName),
        			"_DATA=?",new String[]{f.filePath});
		}else if (MediaFile.isAudioFileType(f.filePath)){
			getContentResolver().delete(MediaStore.Audio.Media.getContentUri(volumeName),
        			"_DATA=?",new String[]{f.filePath});
		}else if (MediaFile.isVideoFileType(f.filePath)) {
			getContentResolver().delete(MediaStore.Video.Media.getContentUri(volumeName),
        			"_DATA=?",new String[]{f.filePath});
		}else {
			getContentResolver().delete(MediaStore.Files.getContentUri("external"),
        			"_DATA=?",new String[]{f.filePath});
		}
		if (file.exists()) {
			file.delete();
		}
    }
    
    public void clear() {
        synchronized(mCurFileNameList) {
            mCurFileNameList.clear();
        }
    }
}
