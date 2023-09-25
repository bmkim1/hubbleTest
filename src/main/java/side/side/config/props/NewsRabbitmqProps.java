package side.side.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties
public class NewsRabbitmqProps {

    private String exchange;
    private String queue;
    private String routingKey;
}
