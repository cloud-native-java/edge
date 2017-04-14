package edge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
public class PasswordGrantIT extends AbstractEdgeTest {

 @Before
 public void before() throws Throwable {
  this.defaultSetup(true);
 }

 @Test
 public void testAuth() throws Throwable {
  Map<String, String> env = new HashMap<>();
  this.baselineDeploy(new String[] { "secure" }, env, new String[] { "secure",
   "sso" }, env);
  this.deployAuthService();

  // todo we should be able to call the
  // auth-service, get a token, then
  // call the edge-service/api/user
  // endpoint with that token

 }
}
