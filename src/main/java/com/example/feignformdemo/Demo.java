package com.example.feignformdemo;

import feign.Feign;
import feign.form.FormEncoder;
import feign.httpclient.ApacheHttpClient;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Demo {

    public static void main(String[] args) throws Exception {

        URL url = Demo.class.getResource("/generated_1_px.png");
        File image = new File(url.toURI());


        byte[] bytes = Files.readAllBytes(Paths.get(url.toURI()));

        System.out.println("--- first three bytes from source image ---");
        System.out.println(bytes[0] + " " + bytes[1] + " " + bytes[2]);


        HttpBinClient apacheClient = Feign.builder()
                .client(new ApacheHttpClient())
                .encoder(new FormEncoder())
                .target(HttpBinClient.class, "http://httpbin.org/");

        HttpBinClient apacheClientWithWorkaround = Feign.builder()
                .client(new ApacheHttpClient())
                .encoder(new FormEncoderWithWorkaround())
                .target(HttpBinClient.class, "http://httpbin.org/");


        System.out.println("--- apache client ---");
        System.out.println(apacheClient.updateImage(image));

        System.out.println("--- apache client + workaround---");
        System.out.println(apacheClientWithWorkaround.updateImage(image));
    }
}
