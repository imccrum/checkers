package imccrum.com.checkers;

/**
 * Created by ian on 19/07/14.
 */
public class User {

  private String name;
  private String password;

  public User(String name, String password) {

    this.password = password;
    this.name = name;
  }

  public String getName() {
    return name;
  }
}


