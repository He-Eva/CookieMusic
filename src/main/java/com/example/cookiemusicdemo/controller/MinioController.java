package com.example.cookiemusicdemo.controller;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;


@Controller
public class MinioController {
    @Value("${minio.bucket-name}")
    private String bucketName;
    @Autowired
    private MinioClient minioClient;
    //获取歌曲
    //"/{fileName:.+}"
    @GetMapping("/user01/song/music/{fileName:.+}")
    public ResponseEntity<byte[]> getMusic(@PathVariable String fileName, HttpServletRequest request) {
        System.out.println(fileName);
        try {
            String objectName = "song/music/" + fileName;

            // 获取对象大小（用于 Range/Content-Range）
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucketName).object(objectName).build()
            );
            long fileSize = stat.size();

            // Range 支持：浏览器拖动进度条会发 Range: bytes=start-end
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            // 尽量给正确的音频类型（mp3/m4a 等），这里统一用 octet-stream 会影响浏览器 seek
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setCacheControl(CacheControl.noCache().getHeaderValue());
            headers.setContentDisposition(ContentDisposition.inline().filename(fileName).build());

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
                HttpRange r = ranges.isEmpty() ? null : ranges.get(0);
                if (r == null) {
                    return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
                }
                long start = r.getRangeStart(fileSize);
                long end = r.getRangeEnd(fileSize);
                if (start >= fileSize) {
                    return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
                }
                long len = end - start + 1;

                // MinIO 分段读取
                GetObjectArgs args = GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .offset(start)
                        .length(len)
                        .build();
                try (InputStream inputStream = minioClient.getObject(args);
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    IOUtils.copy(inputStream, out);
                    byte[] bytes = out.toByteArray();
                    headers.setContentLength(bytes.length);
                    headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + (start + bytes.length - 1) + "/" + fileSize);
                    return new ResponseEntity<>(bytes, headers, HttpStatus.PARTIAL_CONTENT);
                }
            }

            // 不带 Range：返回完整文件
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build();
            try (InputStream inputStream = minioClient.getObject(args)) {
                byte[] bytes = IOUtils.toByteArray(inputStream);
                headers.setContentLength(bytes.length);
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    //获取歌手图片
    @GetMapping("/user01/singer/img/{fileName:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String fileName) throws Exception {
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object("singer/img/"+fileName)
                        .build()
        );

        byte[] bytes = IOUtils.toByteArray(stream);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG); // 设置响应内容类型为图片类型，根据实际情况修改

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
    //获取歌单图片
    @GetMapping("/user01/songlist/img/{fileName:.+}")
    public ResponseEntity<byte[]> getImage1(@PathVariable String fileName) throws Exception {
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object("songlist/img/"+fileName)
                        .build()
        );

        byte[] bytes = IOUtils.toByteArray(stream);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG); // 设置响应内容类型为图片类型，根据实际情况修改

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
    //获取歌的图片
    ///user01/singer/song/98329722.jfif
    @GetMapping("/user01/song/img/{fileName:.+}")
    public ResponseEntity<byte[]> getImage2(@PathVariable String fileName) throws Exception {
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object("song/img/"+fileName)
                        .build()
        );

        byte[] bytes = IOUtils.toByteArray(stream);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG); // 设置响应内容类型为图片类型，根据实际情况修改

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
    //获取头像
    @GetMapping("/user01/consumer/img/{fileName:.+}")
    public ResponseEntity<byte[]> getImage3(@PathVariable String fileName) throws Exception {
        System.out.println("00000000000000000");
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object("consumer/img/"+fileName)
                        .build()
        );

        byte[] bytes = IOUtils.toByteArray(stream);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG); // 设置响应内容类型为图片类型，根据实际情况修改

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
    //获取轮播图
    @GetMapping("/user01/swiper/img/{fileName:.+}")
    public ResponseEntity<byte[]> getImage4(@PathVariable String fileName) throws Exception {
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object("swiper/img/"+fileName)
                        .build()
        );

        byte[] bytes = IOUtils.toByteArray(stream);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG); // 设置响应内容类型为图片类型，根据实际情况修改

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    // 获取帖子图片
    @GetMapping("/user01/post/img/{fileName:.+}")
    public ResponseEntity<byte[]> getPostImage(@PathVariable String fileName) throws Exception {
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object("post/img/" + fileName)
                        .build()
        );

        byte[] bytes = IOUtils.toByteArray(stream);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
