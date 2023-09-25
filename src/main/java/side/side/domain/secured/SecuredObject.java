package side.side.domain.secured;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class SecuredObject {

    private Map<String, String> sm = new HashMap<>();
}
