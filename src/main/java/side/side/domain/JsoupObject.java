package side.side.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JsoupObject {

    private String siteUrl;
    private String dataKey;
    private String dataValue;
}
