package com.offerwave.dto;

import lombok.Data;

@Data
public class MaterialImageUploadDto {

    private String originalName;

    private String fileName;

    private String url;

    private String contentType;

    private Long size;
}
