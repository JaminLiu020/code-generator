package com.jamin.codecube.monitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MonitorContext implements Serializable {
    private String userId;
    private String appId;
    private String requestId;
    private String chatHistoryId;

    @Serial
    private static final long serialVersionUID = 1L;
}
