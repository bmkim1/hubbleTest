package side.side.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Data
@Configuration
@ConfigurationProperties("hubble2")
public class Props {

    private String profilePath;
    private String calcServerPath;
    private String serverPath;

    private String apiNaverPath;
    private String apiBigkindsPath;
    private String apiBigkindsAccessKey;

    private String apiServerPath;
    private String apiMailServerPath;
    private String email;

    @PostConstruct
    public void init() {
        this.apiMailServerPath = this.apiServerPath + "/mail";
    }
}
