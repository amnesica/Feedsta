package com.amnesica.feedsta.fragments;

import static com.amnesica.feedsta.helper.StaticIdentifier.permsReadAndWrite;
import static com.amnesica.feedsta.helper.StaticIdentifier.permsRequestCode;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.amnesica.feedsta.R;
import com.amnesica.feedsta.helper.FileUtil;
import com.amnesica.feedsta.helper.FragmentHelper;
import com.amnesica.feedsta.helper.StorageHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.Objects;

/** Fragment for settings */
public class SettingsFragment extends PreferenceFragmentCompat
    implements SharedPreferences.OnSharedPreferenceChangeListener {

  // key of specific shared preference
  private String key;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.fragment_settings, rootKey);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    // show dialog when darkMode is enabled or disabled
    if (key.equals(getResources().getString(R.string.dark_mode))) {
      showRestartAppDialog();
    }
  }

  @Override
  public boolean onPreferenceTreeClick(Preference preference) {
    key = preference.getKey();

    // ask permissions first
    if (key.equals(getResources().getString(R.string.export_files))
        || key.equals(getResources().getString(R.string.import_files))) {

      // check permissions
      requestPermissions(permsReadAndWrite, permsRequestCode);
      return true;
    }
    return false;
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (permsRequestCode == 200) {
      if (grantResults.length > 0
          && grantResults[0] == PackageManager.PERMISSION_GRANTED
          && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        // Permission is granted. Continue the action or workflow in your app.
        if (key != null && key.equals(getResources().getString(R.string.export_files))) {
          chooseFolderToExport();
        }

        if (key != null && key.equals(getResources().getString(R.string.import_files))) {
          chooseFolderToImport();
        }
      } else {
        // Explain to the user that the feature is unavailable because
        // the features requires a permission that the user has denied.
        // At the same time, respect the user's decision. Don't link to
        // system settings in an effort to convince the user to change
        // their decision.
        FragmentHelper.showToast(
            getResources().getString(R.string.permission_denied),
            requireActivity(),
            requireContext());
      }
    }
  }

  /** Opens system data manager to choose directory to export to */
  private void chooseFolderToExport() {
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    i.addCategory(Intent.CATEGORY_DEFAULT);
    startActivityForResult(
        Intent.createChooser(i, getString(R.string.choose_export_directory)), 9999);
    FragmentHelper.showToast(
        getString(R.string.choose_export_directory), requireActivity(), requireContext());
  }

  /** Opens system data manager to choose directory to import from */
  private void chooseFolderToImport() {
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    i.addCategory(Intent.CATEGORY_DEFAULT);
    startActivityForResult(
        Intent.createChooser(i, getString(R.string.choose_import_directory)), 9998);
    FragmentHelper.showToast(
        getString(R.string.choose_import_directory), requireActivity(), requireContext());
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    // get path to export to
    switch (requestCode) {
      case 9999:
        if (data != null && data.getData() != null) {
          Uri treeUri = data.getData();

          // show dialog that all previous exported files are overwritten
          showExportConfirmationDialog(treeUri);
        }
        break;
      case 9998:
        if (data != null && data.getData() != null) {
          Uri treeUri = data.getData();
          String path = FileUtil.getFullPathFromTreeUri(treeUri, requireContext());

          // show dialog that all previous imported files are overwritten
          showImportConfirmationDialog(path);
        }
        break;
      default:
        FragmentHelper.showToast(
            getString(R.string.operation_cancelled), requireActivity(), requireContext());
    }
  }

  /**
   * Shows dialog that files are being overwritten if they exist
   *
   * @param path String
   */
  private void showImportConfirmationDialog(final String path) {
    MaterialAlertDialogBuilder alertDialogBuilder;

    // create alertDialog
    alertDialogBuilder =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_confirm_import_title)
            .setMessage(R.string.dialog_confirm_import_message)
            .setPositiveButton(
                R.string.dialog_confirm_import_positive_button,
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                    // do import
                    importDataFromApp(path);
                  }
                })
            .setNegativeButton(
                R.string.CANCEL,
                new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                  }
                })

            // get the click outside the dialog to set the behaviour like the negative button was
            // clicked
            .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                  @Override
                  public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                  }
                });

    final MaterialAlertDialogBuilder finalAlertDialogBuilder = alertDialogBuilder;

    // get color for button texts
    TypedValue typedValue = new TypedValue();
    Resources.Theme theme = requireContext().getTheme();
    theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
    @ColorInt final int color = typedValue.data;

    // create alertDialog
    requireActivity()
        .runOnUiThread(
            new Runnable() {
              @Override
              public void run() {
                AlertDialog alertDialog = finalAlertDialogBuilder.create();
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
              }
            });
  }

  /**
   * Files filename_accounts, filename_bookmarks and shared preferences are exported to directory
   *
   * @param pickedDir Uri
   */
  private void exportDataFromApp(final Uri pickedDir) {
    if (pickedDir == null) {
      FragmentHelper.showToast(
          getString(R.string.export_failed), requireActivity(), requireContext());
    }

    boolean successful;

    String rawApplicationName =
        requireActivity()
            .getApplicationInfo()
            .loadLabel(requireActivity().getPackageManager())
            .toString();
    String applicationName =
        Character.toLowerCase(rawApplicationName.charAt(0)) + rawApplicationName.substring(1);
    String pathToSharedPrefs =
        "/data"
            .concat("/data/com.amnesica.")
            .concat(applicationName)
            .concat("/shared_prefs/com.amnesica.")
            .concat(applicationName)
            .concat("_preferences.xml");

    // path of app files
    String inputPath = requireContext().getFilesDir().getPath();

    // copy account_prefs.txt
    if (StorageHelper.checkIfFileExists(StorageHelper.FILENAME_ACCOUNTS, requireContext())) {

      String inputFilename = StorageHelper.FILENAME_ACCOUNTS;
      successful =
          StorageHelper.copyFileExport(inputPath, inputFilename, pickedDir, requireContext());

      if (!successful) {
        FragmentHelper.showToast(
            getString(R.string.export_failed), requireActivity(), requireContext());
        return;
      }
    }

    // copy bookmark_prefs.txt
    if (StorageHelper.checkIfFileExists(StorageHelper.FILENAME_BOOKMARKS, requireContext())) {

      String inputFilename = StorageHelper.FILENAME_BOOKMARKS;
      successful =
          StorageHelper.copyFileExport(inputPath, inputFilename, pickedDir, requireContext());

      if (!successful) {
        FragmentHelper.showToast(
            getString(R.string.export_failed), requireActivity(), requireContext());
        return;
      }
    }

    // copy shared preferences
    if (new File(pathToSharedPrefs).exists()) {

      String inputPathSharedPrefs =
          "/data".concat("/data/com.amnesica.").concat(applicationName).concat("/shared_prefs");
      String inputFilename = "com.amnesica." + applicationName + "_preferences.xml";

      successful =
          StorageHelper.copyFileExport(
              inputPathSharedPrefs, inputFilename, pickedDir, requireContext());

      if (!successful) {
        FragmentHelper.showToast(
            getString(R.string.export_failed), requireActivity(), requireContext());
        return;
      }
    }

    FragmentHelper.showToast(
        getString(R.string.export_successful), requireActivity(), requireContext());
  }

  /**
   * Files filename_accounts, filename_bookmarks and shared preferences are imported from directory
   *
   * @param importPath String
   */
  private void importDataFromApp(String importPath) {
    if (importPath == null) {
      FragmentHelper.showToast(
          getString(R.string.import_failed), requireActivity(), requireContext());
    }

    boolean successful;
    boolean successfulImportAccounts = false;
    boolean successfulImportBookmarks = false;
    boolean successfulImportSharedPrefs = false;

    String rawApplicationName =
        requireActivity()
            .getApplicationInfo()
            .loadLabel(requireActivity().getPackageManager())
            .toString();
    String applicationName =
        Character.toLowerCase(rawApplicationName.charAt(0)) + rawApplicationName.substring(1);

    String nameSharedPreferences = "com.amnesica." + applicationName + "_preferences";
    String pathToSharedPrefs =
        "/data"
            .concat("/data/com.amnesica.")
            .concat(applicationName)
            .concat("/shared_prefs/com.amnesica.")
            .concat(applicationName)
            .concat("_preferences.xml");

    // source and destination paths
    String accountsPathToImportFrom = importPath + File.separator + StorageHelper.FILENAME_ACCOUNTS;
    String accountsPathDestination =
        requireContext().getFilesDir() + File.separator + StorageHelper.FILENAME_ACCOUNTS;

    String bookmarksPathToImportFrom =
        importPath + File.separator + StorageHelper.FILENAME_BOOKMARKS;
    String bookmarksPathDestination =
        requireContext().getFilesDir() + File.separator + StorageHelper.FILENAME_BOOKMARKS;

    String sharedPrefsPathToImportFrom =
        importPath + File.separator + nameSharedPreferences + ".xml";

    // copy imported accounts file to data folder and overwrite current account_prefs.txt
    if (new File(accountsPathToImportFrom).exists()) {
      successful =
          StorageHelper.copyFileImport(new File(accountsPathToImportFrom), accountsPathDestination);
      successfulImportAccounts = successful;
      if (!successful) {
        FragmentHelper.showToast(
            getString(R.string.import_failed), requireActivity(), requireContext());
        return;
      }
    }

    // copy imported bookmarks file to data folder and overwrite current bookmarks_prefs.txt
    if (new File(bookmarksPathToImportFrom).exists()) {
      successful =
          StorageHelper.copyFileImport(
              new File(bookmarksPathToImportFrom), bookmarksPathDestination);
      successfulImportBookmarks = successful;
      if (!successful) {
        FragmentHelper.showToast(
            getString(R.string.import_failed), requireActivity(), requireContext());
        return;
      }
    }

    // copy imported sharedPrefs file to data folder and overwrite current sharedPrefs
    if (new File(sharedPrefsPathToImportFrom).exists()) {
      successful =
          StorageHelper.copyFileImport(new File(sharedPrefsPathToImportFrom), pathToSharedPrefs);
      successfulImportSharedPrefs = successful;
      if (!successful) {
        FragmentHelper.showToast(
            getString(R.string.import_failed), requireActivity(), requireContext());
        return;
      }
    }

    // all imports are successful
    if (successfulImportAccounts && successfulImportBookmarks && successfulImportSharedPrefs) {
      FragmentHelper.showToast(
          getString(R.string.import_successful), requireActivity(), requireContext());

      // show dialog to restart phone (needed for changes in settings)
      showRestartPhoneDialog();
    } else {
      FragmentHelper.showToast(
          getString(R.string.some_data_may_not_be_imported), requireActivity(), requireContext());
    }
  }

  /** Show dialog to restart app */
  private void showRestartAppDialog() {
    MaterialAlertDialogBuilder alertDialogBuilder;
    // create alertDialog
    alertDialogBuilder =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_dark_mode_changed_title)
            .setMessage(R.string.dialog_dark_mode_changed_message)
            .setPositiveButton(
                R.string.dialog_dark_mode_changed_positive_button,
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                    requireActivity().finish();
                  }
                })
            .setNegativeButton(
                R.string.dialog_dark_mode_changed_negative_button,
                new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                  }
                })

            // get the click outside the dialog to set the behaviour like the negative button was
            // clicked
            .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                  @Override
                  public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                  }
                });

    final MaterialAlertDialogBuilder finalAlertDialogBuilder = alertDialogBuilder;

    // get color for button texts
    TypedValue typedValue = new TypedValue();
    Resources.Theme theme = requireContext().getTheme();
    theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
    @ColorInt final int color = typedValue.data;

    // create alertDialog
    requireActivity()
        .runOnUiThread(
            new Runnable() {
              @Override
              public void run() {
                AlertDialog alertDialog = finalAlertDialogBuilder.create();
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
              }
            });
  }

  /** Show dialog to restart phone (needed for updated shared preferences to be used by the app) */
  private void showRestartPhoneDialog() {
    MaterialAlertDialogBuilder alertDialogBuilder;
    // create alertDialog
    alertDialogBuilder =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_restart_phone_title)
            .setMessage(R.string.dialog_restart_phone_message)
            .setPositiveButton(
                R.string.dialog_restart_phone_positive_button,
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                    requireActivity().finish();
                  }
                })
            .setNegativeButton(
                R.string.dialog_restart_phone_negative_button,
                new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                  }
                })
            // get the click outside the dialog to set the behaviour like the negative button was
            // clicked
            .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                  @Override
                  public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                  }
                });

    final MaterialAlertDialogBuilder finalAlertDialogBuilder = alertDialogBuilder;

    // get color for button texts
    TypedValue typedValue = new TypedValue();
    Resources.Theme theme = requireContext().getTheme();
    theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
    @ColorInt final int color = typedValue.data;

    // create alertDialog
    requireActivity()
        .runOnUiThread(
            new Runnable() {
              @Override
              public void run() {
                AlertDialog alertDialog = finalAlertDialogBuilder.create();
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
              }
            });
  }

  @Override
  public void onResume() {
    super.onResume();
    Objects.requireNonNull(getPreferenceManager().getSharedPreferences())
        .registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onPause() {
    Objects.requireNonNull(getPreferenceManager().getSharedPreferences())
        .unregisterOnSharedPreferenceChangeListener(this);
    super.onPause();
  }

  /**
   * Shows dialog that files are being overwritten if they exist
   *
   * @param pickedDir Uri
   */
  private void showExportConfirmationDialog(final Uri pickedDir) {
    MaterialAlertDialogBuilder alertDialogBuilder;
    // create alertDialog
    alertDialogBuilder =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_confirm_export_title)
            .setMessage(R.string.dialog_confirm_export_message)
            .setPositiveButton(
                R.string.dialog_confirm_export_positive_button,
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                    // do export
                    exportDataFromApp(pickedDir);
                  }
                })
            .setNegativeButton(
                R.string.CANCEL,
                new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                  }
                })

            // get the click outside the dialog to set the behaviour like the negative button was
            // clicked
            .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                  @Override
                  public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                  }
                });

    final MaterialAlertDialogBuilder finalAlertDialogBuilder = alertDialogBuilder;

    // get color for button texts
    TypedValue typedValue = new TypedValue();
    Resources.Theme theme = requireContext().getTheme();
    theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
    @ColorInt final int color = typedValue.data;

    // create alertDialog
    requireActivity()
        .runOnUiThread(
            new Runnable() {
              @Override
              public void run() {
                AlertDialog alertDialog = finalAlertDialogBuilder.create();
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
              }
            });
  }
}
