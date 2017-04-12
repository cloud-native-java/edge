package auth.accounts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Account {

 @Id
 @GeneratedValue
 private Long id;

 private String username, password; // <1>

 private boolean active; // <2>

 public Account(String username, String password, boolean active) {
  this.username = username;
  this.password = password;
  this.active = active;
 }

}
