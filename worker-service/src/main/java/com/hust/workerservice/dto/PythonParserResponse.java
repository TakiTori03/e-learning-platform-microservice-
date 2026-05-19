package com.hust.workerservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class PythonParserResponse {
    private String status;
    private String filename;
    private int totalPages;
    private List<ParsedPageDto> pages;
}
