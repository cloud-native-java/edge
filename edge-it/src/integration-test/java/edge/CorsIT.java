package edge;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
public class CorsIT extends AbstractEdgeTest {

    private File html5Client;

    private Log log = LogFactory.getLog(getClass());

    private String edgeServiceAppId, html5AppId;

    @Before
    public void before() throws Throwable {
       this.defaultSetup(false);
        this.html5Client = new File(root, "../html5-client/manifest.yml");

        this.edgeServiceAppId = this.appNameFromManifest(this.edgeServiceManifest);
        this.html5AppId = this.appNameFromManifest(this.html5Client);
    }

    void installWebDriver() throws Throwable {
        WebDriver webDriver = new FirefoxDriver();
        String html5Url = service.urlForApplication(this.html5AppId);

        this.log.info("the html5 URL = " + html5Url);

        webDriver.get(html5Url);

    }

    @Test
    public void testCors() throws Throwable {
        this.baselineDeploy("cors,insecure".split(","));
        if (!this.service.applicationExists(html5AppId)) {
            this.service.pushApplicationUsingManifest(this.html5Client);
            this.log.info("deployed " + html5AppId);
        }

        //  installWebDriver();
    }

}
