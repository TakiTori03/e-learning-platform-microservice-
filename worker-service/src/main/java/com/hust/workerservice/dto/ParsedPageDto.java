package com.hust.workerservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class ParsedPageDto {
    private int page;
    private String rawContent;
    private List<String> chunks;
}
