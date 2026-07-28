package com.amz.ios.launcher.config;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.amz.ios.ioslite.common.util.FileUtil;
import com.amz.ios.launcher.LauncherFiles;
import com.amz.ios.launcher.R;

import java.io.File;

public class LauncherBackupHelper {
    private static final String TAG = "LauncherBackupHelper";

    private static final String COMMAND_BACKUP = "backup";
    private static final String COMMAND_RESTORE = "restore";

    public static final int HAVE_NO_SDCARD = 0;
    public static final int BACKUP_OK = 1;
    public static final int BACKUP_FAIL = 2;
    public static final int RESTORE_OK = 3;
    public static final int RESTORE_FAIL = 4;


    private Context mContext = null;

    private File mBackupDir;
    private File mTargetDb;
    private File mBackupDb;
    private static final String BACKUP_DIR = "launcher_backup";
    private static final String BACKUP_STANDARD = "standard";
    private static final String BACKUP_DRAWER = "drawer";
    private static final String BACKUP_USAGE = "usage";

    private BackupTask mBackupTask = null;
    private OnBackupListener mBackupListener = null;

    public LauncherBackupHelper(Context context) {
        mContext = context;
        initFiles();
    }

    private void initFiles() {
        String backupFileName = BACKUP_STANDARD;
        File rootFile = new File(FileUtil.getRootFilesDir(), BACKUP_DIR);
        mBackupDir = new File(rootFile, backupFileName);

        if (!mBackupDir.exists()) {
            mBackupDir.mkdirs();
        }

        File databaseDir = new File(Environment.getDataDirectory() + "/data/" + mContext.getPackageName() + "/databases/");
        mTargetDb = new File(databaseDir, LauncherFiles.LAUNCHER_DB);

        int gridStyle = Settings.getDesktopGrid(mContext);
        mBackupDb = new File(mBackupDir, gridStyle + mTargetDb.getName());
    }

    public boolean isBackupDbExists() {
        return mBackupDb.exists();
    }

    public void setOnBackupListener(OnBackupListener listener) {
        mBackupListener = listener;
    }

    public void doRestore() {
        if (!mBackupDb.exists()) {
            return;
        }

        if (mBackupTask != null && !mBackupTask.isCancelled()) {
            mBackupTask.cancel(true);
        }
        mBackupTask = new BackupTask(mContext);
        mBackupTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, COMMAND_RESTORE);
    }

    public void doBackup() {
        if (mBackupTask != null && !mBackupTask.isCancelled()) {
            mBackupTask.cancel(true);
        }
        mBackupTask = new BackupTask(mContext);
        mBackupTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, COMMAND_BACKUP);
    }

    public class BackupTask extends AsyncTask<String, Void, Integer> {

        private Context mContext;

        public BackupTask(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(String... params) {
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                return HAVE_NO_SDCARD;
            }

            String command = params[0];
            if (command.equals(COMMAND_BACKUP)) {
                FileUtil.copyFile(new File(mTargetDb.getPath() + "-shm"), new File(mBackupDb.getPath() + "-shm"));
                FileUtil.copyFile(new File(mTargetDb.getPath() + "-wal"), new File(mBackupDb.getPath() + "-wal"));
                return FileUtil.copyFile(mTargetDb, mBackupDb) ? BACKUP_OK : BACKUP_FAIL;
            } else if (command.equals(COMMAND_RESTORE)) {
                FileUtil.copyFile(new File(mBackupDb.getPath() + "-shm"), new File(mTargetDb.getPath() + "-shm"));
                FileUtil.copyFile(new File(mBackupDb.getPath() + "-wal"), new File(mTargetDb.getPath() + "-wal"));
                return FileUtil.copyFile(mBackupDb, mTargetDb) ? RESTORE_OK : RESTORE_FAIL;
            } else {
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            int resId = -1;
            switch (result.intValue()) {
                case HAVE_NO_SDCARD:
                    resId = R.string.launcher_settings_have_no_sdcard;
                    break;
                case BACKUP_OK:
                    resId = R.string.launcher_settings_backup_sucess;
                    if (mBackupListener != null) {
                        mBackupListener.onBackup(true);
                    }
                    break;
                case BACKUP_FAIL:
                    resId = R.string.launcher_settings_backup_failed;
                    if (mBackupListener != null) {
                        mBackupListener.onBackup(false);
                    }
                    break;
                case RESTORE_OK:
                    resId = R.string.launcher_settings_restore_sucess;
                    if (mBackupListener != null) {
                        mBackupListener.onRestore(true);
                    }
                    break;
                case RESTORE_FAIL:
                    resId = R.string.launcher_settings_restore_failed;
                    if (mBackupListener != null) {
                        mBackupListener.onRestore(false);
                    }
                    break;
                default:
                    break;
            }
            if (resId > 0) {
                Toast.makeText(mContext, mContext.getString(resId), Toast.LENGTH_SHORT).show();
            }
        }

    }

    public long getLastestRestoreTime() {
        if (mBackupDb.exists()) {
            return mBackupDb.lastModified();
        }
        return -1;
    }

    interface OnBackupListener {
        void onBackup(boolean success);

        void onRestore(boolean success);
    }
}
