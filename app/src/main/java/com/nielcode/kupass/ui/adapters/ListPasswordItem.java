package com.nielcode.kupass.ui.adapters;

import com.nielcode.kupass.utils.Constant;
import java.io.Serializable;

public class ListPasswordItem implements Serializable {

  private long id = 0;
  private String password = Constant.STRING_EMPTY;
  private String note = Constant.STRING_EMPTY;
  private String userName = Constant.STRING_EMPTY;
  private String passwordName = Constant.STRING_EMPTY;

  public ListPasswordItem(long id, String username, String password) {
    this(id, "Password", username, password);
  }

  public ListPasswordItem(long id, String passwordName, String username, String password) {
    this(id, passwordName, username, password, Constant.STRING_EMPTY);
  }

  public ListPasswordItem(
      long id, String passwordName, String username, String password, String note) {
    this.id = id;
    this.passwordName = passwordName;
    this.userName = username;
    this.password = password;
    this.note = note;
  }

  public long getId() {
    return this.id;
  }

  public String getPasswordName() {
    return this.passwordName;
  }

  public String getUserName() {
    return this.userName;
  }

  public String getPassword() {
    return this.password;
  }

  public String getNote() {
    return this.note;
  }
}
