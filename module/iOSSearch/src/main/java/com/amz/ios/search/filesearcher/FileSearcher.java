package com.amz.ios.search.filesearcher;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import androidx.annotation.NonNull;
import android.util.Log;

import com.amz.ios.search.filesearcher.filter.FileFilter;
import com.amz.ios.search.filesearcher.searchengine.FileItem;
import com.amz.ios.search.filesearcher.searchengine.SearchEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static androidx.core.content.ContextCompat.getExternalCacheDirs;

/**
 * Created by Will on 2017/10/31.
 */

public class FileSearcher  {
    private static String TAG = "FileSearcher";
    public static final String FILE_FILTER = "file_filter";
    public static final String SEARCH_PATH = "search_path";
    private final FileFilter fileFilter = new FileFilter();
    private final Context context;
    public static FileSearcherCallback callback;
    SearchEngine searchEngine;
    private List<FileItem> fileItems = new ArrayList<>();

    /**
     *
     * @param context context
     */
    public FileSearcher(@NonNull Context context){
        this.context = context;
    }

    /**
     * search with detail limit
     * @param min minimum size in byte
     * @param max max size in byte,negative value is no limit
     * @return itself
     */
    public FileSearcher withSizeLimit(long min, long max){
        fileFilter.withSizeLimit(min,max);
        return this;
    }

    /**
     * search with extension
     * @param extension  extension,such as txt,jpg.
     * @return itself
     */
    public FileSearcher withExtension(@NonNull String extension){
        fileFilter.withExtension(extension);
        return this;
    }

    /**
     * search with keyword
     * @param keyword keyword
     * @return itself
     */
    public FileSearcher withKeyword(@NonNull String keyword){
        fileFilter.withKeyword(keyword);
        return this;
    }

    /**
     * whether show hidden files or not(whether show files that prefix with '.'),default is not.
     * @param showHidden show or not
     * @return itself
     */
    public FileSearcher showHidden(boolean showHidden){
        fileFilter.showHidden(showHidden);
        return this;
    }
    public ArrayList<File> getAllStorage(Context c) {
        ArrayList<File> rootPaths = new ArrayList<File>();

        File primaryFile = Environment.getExternalStorageDirectory();
        rootPaths.add(primaryFile);

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                File[] fileDirs = getExternalCacheDirs(c);
                for (File cacheDir: fileDirs) {
                    if (cacheDir == null)
                        continue;
                    File mainStorage = cacheDir.getParentFile();
                    if (mainStorage != null) {
                        mainStorage = mainStorage.getParentFile();
                        if (mainStorage != null) {
                            mainStorage = mainStorage.getParentFile();
                            if (mainStorage != null) {
                                mainStorage = mainStorage.getParentFile();
                                if (mainStorage.exists()) {
                                    if (!mainStorage.getAbsolutePath().contentEquals(primaryFile.getAbsolutePath()))
                                        rootPaths.add(mainStorage);
                                }
                            }
                        }
                    }
                }
            }
            else {
                String storageDirPath = primaryFile.getParent();
                if (storageDirPath == null)
                    return rootPaths;

                File storageFile = new File(storageDirPath);
                if (storageFile.exists()) {
                    if (storageFile.isDirectory()) {
                        File[] subStorageFiles = storageFile.listFiles();

                        if (subStorageFiles == null)
                            return rootPaths;

                        for (File sdCard : subStorageFiles) {
                            if (sdCard.getAbsolutePath().contentEquals(primaryFile.getAbsolutePath()))
                                continue;

                            rootPaths.add(sdCard);
                        }
                    }
                }
            }
        }

        return rootPaths;
    }

    /**
     * search with specified conditions,if passed path is invalid,an IllegalStatementException will be thrown.
     * @param callback
     */
    public void search(final FileSearcherCallback callback){
        fileItems.clear();
        ArrayList<File> rootPaths = getAllStorage(context);

        this.callback = callback;

        if(fileFilter == null || rootPaths.size() <= 0) {
            Log.d("", "");
            return;
        }

        searchEngine = new SearchEngine(rootPaths, fileFilter);
        searchEngine.setCallback(new SearchEngine.SearchEngineCallback() {
            @Override
            public void onFind(List<FileItem> items) {
                fileItems.addAll(items);
                Log.d(TAG, "onFind added count = " + items.size());
            }

            @Override
            public void onSearchDirectory(File file) {
//                toolbar.setSubtitle( file.getPath().replace(Environment.getExternalStorageDirectory().getPath()+File.separator,""));
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "search finished");
                if (callback != null)
                    callback.onSelect(fileItems);
            }
        });

        searchEngine.start();
    }
    public interface FileSearcherCallback{
        void onSelect(List<FileItem> files);
    }
}
