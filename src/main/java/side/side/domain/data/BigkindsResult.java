package side.side.domain.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BigkindsResult {

    @JsonProperty("total_hits")
    private int count;
    @JsonProperty("documents")
    private List<BigkindsArticle> BigkindsArticles = new ArrayList<>();

}
