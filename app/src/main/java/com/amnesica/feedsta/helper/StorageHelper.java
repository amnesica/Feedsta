package com.amnesica.feedsta.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.amnesica.feedsta.Account;
import com.amnesica.feedsta.Post;
import com.amnesica.feedsta.models.AccountStorage;
import com.amnesica.feedsta.models.PostStorage;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Helper class for storing and retrieving elements from internal storage
 */
@SuppressWarnings({"ResultOfMethodCallIgnored"})
public class StorageHelper {

    // filename for file storing followed accounts
    public static final String filename_accounts = "account_prefs.txt";
    // filename for file storing followed accounts (updated thumbnail urls)
    public static final String filename_accounts_updated = "account_prefs_updated.txt";

    // filename for file storing bookmarked posts
    public static final String filename_bookmarks = "bookmarked_posts.txt";
    // filename for file storing bookmarked posts (updated thumbnail urls)
    public static final String filename_bookmarks_updated = "bookmarked_posts_updated.txt";

    // filename for file storing posts in feed
    public static final String filename_posts = "feed_posts.txt";

    /**
     * Save video in internal storage
     *
     * @param inputStream InputStream
     * @param buffer      byte[]
     * @param accountName String
     * @return Boolean, if file was saved successfully
     */
    public static Boolean saveVideo(InputStream inputStream, byte[] buffer, String accountName, Context context) {
        if (inputStream != null && buffer != null && accountName != null && context != null) {
            String filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsoluteFile() + "/Feedsta/";
            File dir = new File(filepath);
            dir.mkdir();

            FileOutputStream fileOutputStream;
            File file = new File(dir, System.currentTimeMillis() + "_" + accountName + ".mp4");
            try {
                fileOutputStream = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                Log.d("StorageHelper", Log.getStackTraceString(e));
                return false;
            }

            while (true) {
                int len1;
                try {
                    if (!((len1 = inputStream.read(buffer)) > 0)) break;
                } catch (IOException e) {
                    Log.d("StorageHelper", Log.getStackTraceString(e));
                    return false;
                }
                try {
                    fileOutputStream.write(buffer, 0, len1);
                } catch (IOException e) {
                    Log.d("StorageHelper", Log.getStackTraceString(e));
                    return false;
                }
            }
            try {
                fileOutputStream.close();
                inputStream.close();

                // refresh media
                try {
                    // trigger scan of media files. Image needs to be shown in gallery app
                    MediaScannerConnection.scanFile(context, new String[]{file.getPath()}, null, null);
                } catch (NullPointerException e) {
                    Log.d("StorageHelper", Log.getStackTraceString(e));
                }

                return true;
            } catch (IOException e) {
                Log.d("StorageHelper", Log.getStackTraceString(e));
                return false;
            }
        }
        return false;
    }

    /**
     * Saves an image to internal storage
     *
     * @param finalBitmap Bitmap
     * @param accountName String
     * @return Boolean, if file was saved successfully
     */
    public static Boolean saveImage(Bitmap finalBitmap, String accountName, Context context) {
        if (finalBitmap != null && context != null && accountName != null) {
            OutputStream outputStream;
            String filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsoluteFile() + "/Feedsta/";
            File dir = new File(filepath);
            dir.mkdir();
            File file = new File(dir, System.currentTimeMillis() + "_" + accountName + ".jpg");
            try {
                outputStream = new FileOutputStream(file);
            } catch (FileNotFoundException | NullPointerException e) {
                Log.d("StorageHelper", Log.getStackTraceString(e));
                return false;
            }

            try {
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            } catch (Exception e) {
                Log.d("StorageHelper", Log.getStackTraceString(e));
            }

            try {
                // trigger scan of media files. Image needs to be shown in gallery app
                MediaScannerConnection.scanFile(context, new String[]{file.getPath()}, null, null);
            } catch (NullPointerException e) {
                Log.d("StorageHelper", Log.getStackTraceString(e));
            }

            try {
                outputStream.flush();
            } catch (IOException e) {
                Log.d("StorageHelper", Log.getStackTraceString(e));
                return false;
            }
            try {
                outputStream.close();
                return true;
            } catch (IOException e) {
                Log.d("StorageHelper", Log.getStackTraceString(e));
                return false;
            }
        }
        return false;
    }

    /**
     * Deletes the file with the specified filename
     *
     * @param context  Context
     * @param filename String
     */
    public static void deleteSpecificFile(Context context, String filename) {
        File file = new File(context.getFilesDir(), filename);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Backups an oldFile with oldFileName and renames a newFile with newFileName to oldFileName
     *
     * @param context     Context
     * @param oldFileName name of oldFile
     * @param newFileName name of newFile
     * @throws NullPointerException NullPointerException
     */
    public static void renameSpecificFileTo(Context context, String oldFileName, String newFileName) throws NullPointerException {
        // backup and rename old file
        File oldFile = new File(context.getFilesDir(), oldFileName);
        oldFile.renameTo(new File(context.getFilesDir(), oldFileName + "old" + System.currentTimeMillis()));

        // rename new file to old filename
        File newFile = new File(context.getFilesDir(), newFileName);
        newFile.renameTo(new File(context.getFilesDir(), oldFileName));
    }

    /**
     * Removes an account from filename_accounts
     *
     * @param account account to be removed
     * @param context context
     * @return true, if removing was successful
     */
    public static Boolean removeAccountFromInternalStorage(Account account, Context context) {
        boolean removed = false;
        String filename = filename_accounts;
        ArrayList<AccountStorage> readAccounts = new ArrayList<>();
        AccountStorage accountStorageToDelete = null;

        try {
            FileInputStream fis;
            fis = context.openFileInput(filename);
            ObjectInputStream oi = new ObjectInputStream(fis);
            boolean isExist = true;

            // get all accounts and save in arrayList
            while (isExist) {
                if (fis.available() != 0) {
                    readAccounts = (ArrayList<AccountStorage>) oi.readObject();
                } else {
                    isExist = false;
                    fis.close();
                }
            }
            oi.close();

            // search for specific account name
            if (readAccounts != null) {
                for (AccountStorage accountStorage : readAccounts) {
                    if (accountStorage.getId().equals(account.getId())) {
                        accountStorageToDelete = accountStorage;
                    }
                }
                if (accountStorageToDelete != null) {
                    // Delete account to delete in list
                    readAccounts.remove(accountStorageToDelete);

                    // delete file
                    deleteSpecificFile(context, filename);

                    // write account in internal storage
                    removed = storeAccountStorageListInInternalStorage(readAccounts, context);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Log.d("StorageHelper", Log.getStackTraceString(e));
            removed = false;
        }
        return removed;
    }

    /**
     * Removes an post from filename_posts or filename_bookmarks
     *
     * @param post     post to remove
     * @param context  context
     * @param filename filename_posts or filename_bookmarks
     * @return true, if removing was successful
     */
    public static Boolean removePostFromInternalStorage(Post post, Context context, String filename) {
        boolean removed = false;
        ArrayList<PostStorage> readPosts = new ArrayList<>();
        PostStorage postStorageToDelete = null;

        try {
            FileInputStream fis;
            fis = context.openFileInput(filename);
            ObjectInputStream oi = new ObjectInputStream(fis);
            boolean isExist = true;

            // get all posts and save in arrayList
            while (isExist) {
                if (fis.available() != 0) {
                    readPosts = (ArrayList<PostStorage>) oi.readObject();
                } else {
                    isExist = false;
                    fis.close();
                }
            }
            oi.close();

            // search for specific post id
            if (readPosts != null) {
                for (PostStorage postStorage : readPosts) {
                    if (postStorage.getId().equals(post.getId())) {
                        postStorageToDelete = postStorage;
                    }
                }
                if (postStorageToDelete != null) {
                    // Delete account to delete in list
                    readPosts.remove(postStorageToDelete);

                    // delete file
                    deleteSpecificFile(context, filename);

                    // write account in internal storage
                    removed = storePostStorageListInInternalStorage(readPosts, context, filename);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Log.d("StorageHelper", Log.getStackTraceString(e));
            removed = false;
        }
        return removed;
    }

    /**
     * Converts an ArrayList<Account> accounts to ArrayList<AccountStorage> and stores them in a file with filename
     *
     * @param accounts ArrayList<Account>
     * @param context  context
     * @param filename filename
     * @return true, if storing was successful
     * @throws IOException IOException
     */
    public static Boolean storeAccountListInInternalStorage(ArrayList<Account> accounts, Context context, String filename) throws IOException {
        File file = new File(context.getFilesDir(), filename);
        byte[] bytesToWrite;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArrayList<AccountStorage> accountStorages = new ArrayList<>();

        // convert to AccountStorage list
        for (Account account : accounts) {
            accountStorages.add(new AccountStorage(account.getId(),
                    account.getImageProfilePicUrl(),
                    account.getUsername(),
                    account.getFullName(),
                    account.getIs_private()));
        }

        ObjectOutputStream out;
        out = new ObjectOutputStream(bos);
        out.writeObject(accountStorages);
        out.flush();
        out.close();
        bytesToWrite = bos.toByteArray();
        bos.close();

        // store byte[] bytesToWrite in file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (bytesToWrite != null) {
                fos.write(bytesToWrite);
                fos.close();
                return true;
            }
        } catch (IOException e) {
            Log.d("StorageHelper", Log.getStackTraceString(e));
        }
        return false;
    }

    /**
     * Converts an ArrayList<Post> posts to ArrayList<PostStorage> and stores them in a file with filename
     *
     * @param posts    ArrayList<Post>
     * @param context  context
     * @param filename filename
     * @return true, if storing was successful
     * @throws IOException IOException
     */
    public static Boolean storePostListInInternalStorage(ArrayList<Post> posts, Context context, String filename) throws IOException {
        // file with path
        File file = new File(context.getFilesDir(), filename);
        byte[] bytesToWrite;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArrayList<PostStorage> postStorages = new ArrayList<>();

        // convert to PostStorage list
        for (Post post : posts) {
            postStorages.add(new PostStorage(post.getId(),
                    post.getShortcode(),
                    post.getTakenAtDate(),
                    post.getIs_video(),
                    post.getImageUrlThumbnail(),
                    post.getIs_sideCar(),
                    post.getCategory()));
        }

        ObjectOutputStream out;
        out = new ObjectOutputStream(bos);
        out.writeObject(postStorages);
        out.flush();
        out.close();
        bytesToWrite = bos.toByteArray();
        bos.close();

        // store byte[] bytesToWrite in file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (bytesToWrite != null) {
                fos.write(bytesToWrite);
                fos.close();
                return true;
            }
        } catch (IOException e) {
            Log.d("StorageHelper", Log.getStackTraceString(e));
        }
        return false;
    }

    /**
     * Stores an account in AccountStorage representation in internal storage
     *
     * @param accountToStore account to be stored
     * @param context        context
     * @return true if storing was successful
     * @throws IOException IOException
     */
    public static Boolean storeAccountInInternalStorage(Account accountToStore, Context context) throws IOException {
        String filename = filename_accounts;
        ArrayList<AccountStorage> readAccounts = null;

        File file = new File(context.getFilesDir(), filename);
        byte[] bytesToWrite = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        boolean bAccountShouldBeStored = false;

        if (file.exists()) {
            // check if account is already in file to read old accountList first
            if (!checkIfAccountOrPostIsInFile(accountToStore, filename, context)) {
                // read arrayList from storage
                readAccounts = readAccountStorageListFromInternalStorage(context);

                bAccountShouldBeStored = true;
            }
        } else {
            readAccounts = new ArrayList<>();

            bAccountShouldBeStored = true;
        }

        // if this boolean is false, bytesToWrite is null -> nothing is stored!
        if (bAccountShouldBeStored && readAccounts != null) {
            // add account to list in AccountStorage representation
            readAccounts.add(new AccountStorage(accountToStore.getId(),
                    accountToStore.getImageProfilePicUrl(),
                    accountToStore.getUsername(),
                    accountToStore.getFullName(),
                    accountToStore.getIs_private()));

            ObjectOutputStream out;
            out = new ObjectOutputStream(bos);
            out.writeObject(readAccounts);
            out.flush();
            out.close();
            bytesToWrite = bos.toByteArray();
            bos.close();
        }

        // store byte[] bytesToWrite in file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (bytesToWrite != null) {
                fos.write(bytesToWrite);
                fos.close();
                return true;
            }
        }
        return false;
    }

    /**
     * Stores an post in PostStorage representation in internal storage
     * (adds to list and does not edit specific post!!)
     *
     * @param postToStore post to be stored
     * @param context     context
     * @param filename    filename (bookmarked_posts or posts)
     * @return true if storing was successful
     * @throws IOException IOException
     */
    public static Boolean storePostInInternalStorage(Post postToStore, Context context, String filename) throws IOException {
        ArrayList<PostStorage> readPosts = null;

        File file = new File(context.getFilesDir(), filename);
        byte[] bytesToWrite = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        boolean bPostShouldBeStored = false;

        if (file.exists()) {
            // check if post is already in file to read old postList first
            if (!checkIfAccountOrPostIsInFile(postToStore, filename, context)) {
                // read arrayList from storage
                readPosts = readPostStorageListFromInternalStorage(context, filename);

                bPostShouldBeStored = true;
            }
        } else {
            readPosts = new ArrayList<>();

            bPostShouldBeStored = true;
        }

        // if this boolean is false, bytesToWrite is null -> nothing is stored!
        if (bPostShouldBeStored && readPosts != null) {
            // add post to list in PostStorage representation
            readPosts.add(new PostStorage(postToStore.getId(),
                    postToStore.getShortcode(),
                    postToStore.getTakenAtDate(),
                    postToStore.getIs_video(),
                    postToStore.getImageUrlThumbnail(),
                    postToStore.getIs_sideCar(),
                    postToStore.getCategory()));

            ObjectOutputStream out;
            out = new ObjectOutputStream(bos);
            out.writeObject(readPosts);
            out.flush();
            out.close();
            bytesToWrite = bos.toByteArray();
            bos.close();
        }

        // store byte[] bytesToWrite in file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (bytesToWrite != null) {
                fos.write(bytesToWrite);
                fos.close();
                return true;
            }
        }
        return false;
    }

    /**
     * Updates a category of an bookmarked post in the specific bookmarked post in storage
     *
     * @param postToUpdate post with new category
     * @param context      context
     * @return true, if update was successful
     * @throws IOException IOException
     */
    public static Boolean updateBookmarkCategoryInStorage(Post postToUpdate, Context context) throws IOException {
        ArrayList<PostStorage> readPosts = null;

        String filename = filename_bookmarks;
        File file = new File(context.getFilesDir(), filename);
        byte[] bytesToWrite = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        if (file.exists()) {
            // read arrayList from storage
            readPosts = readPostStorageListFromInternalStorage(context, filename);
        }

        // if this boolean is false, bytesToWrite is null -> nothing is stored!
        if (readPosts != null) {

            // update category in post from storage
            for (PostStorage postStorage : readPosts) {
                if (postStorage.getId().equals(postToUpdate.getId())) {
                    postStorage.setCategory(postToUpdate.getCategory());
                }
            }

            // write updated List to byte[]
            ObjectOutputStream out;
            out = new ObjectOutputStream(bos);
            out.writeObject(readPosts);
            out.flush();
            out.close();
            bytesToWrite = bos.toByteArray();
            bos.close();
        }

        // store updated byte[] bytesToWrite in file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (bytesToWrite != null) {
                fos.write(bytesToWrite);
                fos.close();
                return true;
            }
        }
        return false;
    }

    /**
     * Read accounts from internal storage
     *
     * @param context context
     * @return ArrayList of accounts
     */
    public static ArrayList<Account> readAccountsFromInternalStorage(Context context) {
        String filename = filename_accounts;
        ArrayList<Account> listAccounts = null;
        File file = new File(context.getFilesDir(), filename);

        if (file.exists()) {
            ArrayList<AccountStorage> readAccounts = new ArrayList<>();
            FileInputStream fis = null;

            try {
                fis = context.openFileInput(filename);
                ObjectInputStream oi = new ObjectInputStream(fis);
                boolean isExist = true;

                while (isExist) {
                    if (fis.available() != 0) {
                        readAccounts = (ArrayList<AccountStorage>) oi.readObject();
                        listAccounts = new ArrayList<>();
                    } else {
                        isExist = false;
                        fis.close();
                    }
                }
                oi.close();
            } catch (IOException | ClassNotFoundException e) {
                Log.d("StorageHelper", Log.getStackTraceString(e));
            } finally {
                // close all streams even when exception was called
                try {
                    assert fis != null;
                    fis.close();
                } catch (IOException e) {
                    Log.d("StorageHelper", Log.getStackTraceString(e));
                }
            }

            // convert AccountStorage accounts to normal Accounts
            if (readAccounts != null && listAccounts != null) {
                for (AccountStorage accountStorage : readAccounts) {
                    listAccounts.add(new Account(accountStorage.getImageProfilePicUrl(),
                            accountStorage.getUsername(),
                            accountStorage.getFullName(),
                            accountStorage.getIs_private(),
                            accountStorage.getId()));
                }

                // for normal behaviour in feedFragment to display text
                // set listAccounts to null when readAccounts is empty
                if (readAccounts.isEmpty()) {
                    listAccounts = null;
                }
            }
        }
        return listAccounts;
    }

    /**
     * Returns the ArrayList<AccountStorage> of accounts (only internal use in Storagehelper)
     *
     * @param context context
     * @return ArrayList<AccountStorage>
     */
    private static ArrayList<AccountStorage> readAccountStorageListFromInternalStorage(Context context) {
        String filename = filename_accounts;
        ArrayList<AccountStorage> readAccounts = new ArrayList<>();
        File file = new File(context.getFilesDir(), filename);

        if (file.exists()) {
            FileInputStream fis = null;

            try {
                fis = context.openFileInput(filename);
                ObjectInputStream oi = new ObjectInputStream(fis);
                boolean isExist = true;

                while (isExist) {
                    if (fis.available() != 0) {
                        readAccounts = (ArrayList<AccountStorage>) oi.readObject();
                    } else {
                        isExist = false;
                        fis.close();
                    }
                }
                oi.close();
            } catch (IOException | ClassNotFoundException e) {
                Log.d("StorageHelper", Log.getStackTraceString(e));
            } finally {
                // close all streams even when exception was called
                try {
                    assert fis != null;
                    fis.close();
                } catch (IOException e) {
                    Log.d("StorageHelper", Log.getStackTraceString(e));
                }
            }
        }
        return readAccounts;
    }

    private static ArrayList<PostStorage> readPostStorageListFromInternalStorage(Context context, String filename) {
        ArrayList<PostStorage> readPosts = new ArrayList<>();
        File file = new File(context.getFilesDir(), filename);

        if (file.exists()) {
            FileInputStream fis = null;

            try {
                fis = context.openFileInput(filename);
                ObjectInputStream oi = new ObjectInputStream(fis);
                boolean isExist = true;

                while (isExist) {
                    if (fis.available() != 0) {
                        readPosts = (ArrayList<PostStorage>) oi.readObject();
                    } else {
                        isExist = false;
                        fis.close();
                    }
                }
                oi.close();
            } catch (IOException | ClassNotFoundException e) {
                Log.d("StorageHelper", Log.getStackTraceString(e));
            } finally {
                // close all streams even when exception was called
                try {
                    assert fis != null;
                    fis.close();
                } catch (IOException e) {
                    Log.d("StorageHelper", Log.getStackTraceString(e));
                }
            }
        }
        return readPosts;
    }

    /**
     * Read posts from internal storage (for feed or bookmarked posts)
     *
     * @param context  context
     * @param filename filename of file in internal storage (filename_posts or filename_bookmarked_posts)
     * @return ArrayList of posts
     */
    public static ArrayList<Post> readPostsFromInternalStorage(Context context, String filename) {
        ArrayList<Post> listPosts = null;
        File file = new File(context.getFilesDir(), filename);

        if (file.exists()) {
            ArrayList<PostStorage> readPosts = new ArrayList<>();
            FileInputStream fis = null;

            try {
                fis = context.openFileInput(filename);
                ObjectInputStream oi = new ObjectInputStream(fis);
                boolean isExist = true;

                while (isExist) {
                    if (fis.available() != 0) {
                        readPosts = (ArrayList<PostStorage>) oi.readObject();
                        listPosts = new ArrayList<>();
                    } else {
                        isExist = false;
                        fis.close();
                    }
                }
                oi.close();
            } catch (IOException | ClassNotFoundException e) {
                Log.d("StorageHelper", Log.getStackTraceString(e));
            } finally {
                // close all streams even when exception was called
                try {
                    assert fis != null;
                    fis.close();
                } catch (IOException e) {
                    Log.d("StorageHelper", Log.getStackTraceString(e));
                }
            }

            // convert AccountStorage accounts to normal Accounts
            if (readPosts != null && listPosts != null) {
                for (PostStorage postStorage : readPosts) {
                    listPosts.add(new Post(postStorage.getId(),
                            postStorage.getShortcode(),
                            postStorage.getTakenAtDate(),
                            postStorage.getIs_video(),
                            postStorage.getImageUrlThumbnail(),
                            postStorage.getIs_sideCar(),
                            postStorage.getCategory()));
                }

                // for normal behaviour in feedFragment to display text
                // set listPosts to null when readPosts is empty
                if (readPosts.isEmpty()) {
                    listPosts = null;
                }
            }
        }
        return listPosts;
    }

    /**
     * Checks if an account or a post is in file with filename
     *
     * @param object   Object
     * @param filename String
     * @param context  Context
     * @return Boolean, if data exists in file
     */
    public static Boolean checkIfAccountOrPostIsInFile(Object object, String filename, Context context) {
        boolean dataIsInFile = false;
        if (object instanceof Account) {
            Account account = (Account) object;
            ArrayList<AccountStorage> readAccountStorages;

            try {
                FileInputStream fis;
                fis = context.openFileInput(filename);
                ObjectInputStream oi = new ObjectInputStream(fis);
                boolean isExist = true;

                while (isExist) {
                    if (fis.available() != 0) {

                        // hint: call to readAccountsFromInternalStorage(context); does not work;
                        // ends in loop because fis is always available! -> duplicated code is necessary here
                        readAccountStorages = (ArrayList<AccountStorage>) oi.readObject();

                        for (AccountStorage readAccountStorage : readAccountStorages) {
                            // check if readAccountStorage and account (inserted account) are the same
                            // note: readAccountStorage is AccountStorage and account is Account
                            // (no converting necessary here because comparision value is username)
                            if (readAccountStorage != null && (readAccountStorage.getUsername().equals(account.getUsername()))) {
                                dataIsInFile = true;
                                break;
                            }
                        }
                    } else {
                        isExist = false;
                        oi.close();
                        fis.close();
                    }
                }
                return dataIsInFile;
            } catch (IOException | NullPointerException | ClassNotFoundException e) {
                return false;
            }
        } else if (object instanceof Post) {
            Post post = (Post) object;
            ArrayList<PostStorage> readPostStorages;

            try {
                FileInputStream fis;
                fis = context.openFileInput(filename);
                ObjectInputStream oi = new ObjectInputStream(fis);
                boolean isExist = true;

                while (isExist) {
                    if (fis.available() != 0) {
                        // hint: call to readPostsFromInternalStorage(context); does not work;
                        // ends in loop because fis is always available! -> duplicated code is necessary here
                        readPostStorages = (ArrayList<PostStorage>) oi.readObject();

                        for (PostStorage readPostStorage : readPostStorages) {
                            // check if readPostStorages and post (inserted post) are the same
                            // note: readPostStorages is PostStorage and post is Post
                            // (no converting necessary here because comparision value is id)
                            if (readPostStorage != null && (readPostStorage.getId().equals(post.getId()))) {
                                dataIsInFile = true;
                                break;
                            }
                        }
                    } else {
                        isExist = false;
                        oi.close();
                        fis.close();
                    }
                }
                return dataIsInFile;
            } catch (IOException | NullPointerException | ClassNotFoundException e) {
                Log.d("StorageHelper", Log.getStackTraceString(e));
                return false;
            }
        }
        return false;
    }

    /**
     * Checks if file with filename exists in internal storage
     *
     * @param filename filename of file
     * @param context  context
     * @return true, if file exists
     */
    public static boolean checkIfFileExists(String filename, Context context) {
        File file = new File(context.getFilesDir(), filename);
        return file.exists();
    }

    /**
     * Stores a list of AccountStorage in internal storage
     *
     * @param listNewAccountStorageRep ArrayList<AccountStorage>
     * @param context                  Context
     * @return boolean
     * @throws IOException IOException
     */
    private static boolean storeAccountStorageListInInternalStorage(ArrayList<AccountStorage> listNewAccountStorageRep, Context context) throws IOException {
        byte[] bytesToWrite;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);

        out.writeObject(listNewAccountStorageRep);
        out.flush();
        out.close();
        bytesToWrite = bos.toByteArray();
        bos.close();

        // writes directly to filename_accounts
        File file = new File(context.getFilesDir(), filename_accounts);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (bytesToWrite != null) {
                fos.write(bytesToWrite);
                fos.close();
                return true;
            }
        } catch (IOException e) {
            throw new IOException();
        }

        return false;
    }

    /**
     * Stores a list of PostStorage in internal storage
     *
     * @param listNewPostStorageRep ArrayList<PostStorage>
     * @param context               Context
     * @param filenamePara          String
     * @return boolean
     * @throws IOException IOException
     */
    private static boolean storePostStorageListInInternalStorage(ArrayList<PostStorage> listNewPostStorageRep, Context context, String filenamePara) throws IOException {
        // filename for temporary new post list (bookmarks or posts) -> will be renamed in filename_posts after successful converting
        String newFilename = null;
        if (filenamePara.equals(filename_bookmarks)) {
            newFilename = filename_bookmarks;
        }
        if (filenamePara.equals(filename_posts)) {
            newFilename = filename_posts;
        }

        byte[] bytesToWrite;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);

        out.writeObject(listNewPostStorageRep);
        out.flush();
        out.close();
        bytesToWrite = bos.toByteArray();
        bos.close();

        // writes directly to filename_posts or filename_bookmarks (no renaming -> newFilename used here)
        assert newFilename != null;
        File file = new File(context.getFilesDir(), newFilename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (bytesToWrite != null) {
                fos.write(bytesToWrite);
                fos.close();
                return true;
            }
        } catch (IOException e) {
            throw new IOException();
        }

        return false;
    }

    /**
     * Returns the amount of followed accounts
     *
     * @param context Context
     * @return amount of followed accounts as integer
     */
    static int amountFollowedAccounts(Context context) {
        if (context != null) {
            ArrayList<AccountStorage> accountList = readAccountStorageListFromInternalStorage(context);
            if (accountList != null) {
                return accountList.size();
            }
        }
        return 0;
    }

    /**
     * Returns the amount of bookmarks
     *
     * @param context Context
     * @return amount of bookmarks as integer
     */
    static int amountBookmarks(Context context) {
        if (context != null) {
            ArrayList<PostStorage> postList = readPostStorageListFromInternalStorage(context, filename_bookmarks);
            if (postList != null) {
                return postList.size();
            }
        }
        return 0;
    }

    /**
     * Imports (copies) one file from stringCopyPath to stringPastePath (existing files are overwritten)
     *
     * @param fileToCopy      file to copy
     * @param stringPastePath path to copy to
     * @return true, if import was successful
     */
    public static boolean copyFileImport(File fileToCopy, String stringPastePath, Context context) {
        File destination = new File(stringPastePath);

        try {
            FileUtils.copyFile(fileToCopy, destination);
        } catch (IOException e) {
            Log.d("StorageHelper", Log.getStackTraceString(e));
            return false;
        }
        return true;
    }

    /**
     * Exports app files to picked directory treeUriForPickedDir (internal or external sd card)
     *
     * @param inputPath                  path of file to be copied (without filename and extension)
     * @param inputFilenameWithExtension filename and extension of file to be copied
     * @param treeUriForPickedDir        picked destination directory
     * @param context                    context
     * @return true, if copied successful
     */
    public static boolean copyFileExport(String inputPath, String inputFilenameWithExtension, Uri treeUriForPickedDir, Context context) {
        InputStream in = null;
        OutputStream out = null;
        boolean successful = true;
        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUriForPickedDir);
        String extension = inputFilenameWithExtension.substring(inputFilenameWithExtension.lastIndexOf(".") + 1);

        try {
            DocumentFile documentfile = pickedDir.findFile(inputFilenameWithExtension);

            // if no file with filename exists -> create new one
            // source: https://stackoverflow.com/questions/51855122/how-to-rewrite-documentfile
            if (documentfile == null) {
                documentfile = pickedDir.createFile("text/" + extension, inputFilenameWithExtension);
            }

            out = context.getContentResolver().openOutputStream(documentfile.getUri());
            in = new FileInputStream(inputPath + File.separator + inputFilenameWithExtension);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            // write the output file (You have now copied the file)
            out.flush();
            out.close();

        } catch (Exception e) {
            Log.d("StorageHelper", Log.getStackTraceString(e));
            successful = false;
        }

        return successful;
    }
}