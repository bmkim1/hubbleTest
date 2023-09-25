package side.side.domain.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;

@Getter
@ToString
public class NaverResultWrapper {
    private String lastBuildDate;
    private Integer total;
    private Integer start;
    private Integer display;
    @JsonProperty("Items")
    private List<NaverArticle> articleList;

    private String errorMessage;
    private String errorCode;
}
