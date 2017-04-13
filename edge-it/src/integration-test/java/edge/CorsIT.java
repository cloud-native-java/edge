package edge;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
public class CorsIT extends AbstractEdgeTest {

    private Log log = LogFactory.getLog(getClass());

    @Before
    public void before() throws Throwable {
        this.defaultSetup();
    }

}
