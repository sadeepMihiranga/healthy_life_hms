package lk.healthylife.hms.dto;

import com.google.common.net.MediaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailAttachmentDTO {

    private String fileName;
    private byte[] file;
    private MediaType contentType;
}
