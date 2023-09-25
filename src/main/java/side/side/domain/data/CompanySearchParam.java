package side.side.domain.data;

import lombok.*;
import side.side.domain.secured.SecuredKey;
import side.side.domain.secured.SecuredObject;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CompanySearchParam extends SecuredObject {

    @SecuredKey
    private int idSeq;

    private String companyName;

    private String ceoName;

    private LocalDateTime articleUpdateDatetime;

    @Builder.Default
    private int keywordDepth = 4;
}
