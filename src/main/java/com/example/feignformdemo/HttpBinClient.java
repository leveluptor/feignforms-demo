package com.example.feignformdemo;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.io.File;

public interface HttpBinClient {

    @RequestLine("PUT /anything")
    @Headers("Content-Type: multipart/form-data")
    String updateImage(@Param("image") File image);

}
