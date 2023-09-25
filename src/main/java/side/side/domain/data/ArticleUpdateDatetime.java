package side.side.domain.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleUpdateDatetime {

    int idSeq;

    LocalDateTime articleUpdateDatetime;
}
