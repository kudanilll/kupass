package com.nielcode.kupass.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ScoopedStorage {

  public static final int REQUEST_ACTION_CREATE_FILE = 101;
  public static final int REQUEST_ACTION_CREATE_CSV = 102;

  private final Activity activity;
  private final Context context;
  private String content;

  public ScoopedStorage(Activity activity, Context context) {
    this.activity = activity;
    this.context = context;
  }

  @SuppressLint("SimpleDateFormat")
  public void createFile(String content, int extension) {
    this.content = content;
    String extensionString = Constant.STRING_EMPTY;
    switch (extension) {
      case Constant.EXPORT_JSON:
        extensionString = ".json";
        break;
      case Constant.EXPORT_TEXT:
        extensionString = ".txt";
        break;
    }

    SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
    String filename = Constant.FILES_PREFIX + formatter.format(new Date()) + extensionString;

    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("text/*");
    intent.putExtra(Intent.EXTRA_TITLE, filename);
    this.activity.startActivityForResult(intent, REQUEST_ACTION_CREATE_FILE);
  }

  public boolean create(Uri uri) {
    if (uri == null) return false;
    try {
      OutputStream fstream = this.context.getContentResolver().openOutputStream(uri);
      Objects.requireNonNull(fstream).write(this.content.getBytes(StandardCharsets.US_ASCII));
      fstream.close();
    } catch (IOException e) {
      return false;
    }
    return true;
  }

  @SuppressLint("SimpleDateFormat")
  public void createFileCsv() {
    SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
    String filename = Constant.FILES_PREFIX + formatter.format(new Date()) + ".csv";
  }

  public boolean createCsv(Intent intent) {
    String[] header = {"password_name", "username", "password", "note"};
    return true;
  }
}
