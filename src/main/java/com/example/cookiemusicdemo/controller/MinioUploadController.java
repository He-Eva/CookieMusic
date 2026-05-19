package com.example.cookiemusicdemo.controller;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

@Service
public class MinioUploadController {

    private static MinioClient minioClient;
    private static String bucketName;

    public static void init(){
        Properties properties = new Properties();
        try {
            // 使用类加载器获取资源文件的输入流
            InputStream inputStream = MinioUploadController.class.getClassLoader().getResourceAsStream("application-dev.properties");
            if (inputStream != null) {
                properties.load(inputStream);
                String minioEndpoint = properties.getProperty("minio.endpoint");
                String minioAccessKey = properties.getProperty("minio.access-key");
                String minioSecretKey = properties.getProperty("minio.secret-key");
                String minioBucketName = properties.getProperty("minio.bucket-name");
                bucketName = minioBucketName;
                minioClient = MinioClient.builder()
                        .endpoint(minioEndpoint)
                        .credentials(minioAccessKey, minioSecretKey)
                        .build();
            }
        }catch (Exception e){
            System.out.println(e);
        }
    }

    public static String uploadFile(MultipartFile file) {
        try {
            init();
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object("/song/music/"+file.getOriginalFilename())
                            .stream(inputStream, inputStream.available(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return "File uploaded successfully!";
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return "Error uploading file to MinIO: " + e.getMessage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static String uploadImgFile(MultipartFile file) {
        try {
            init();
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object("/singer/img/"+file.getOriginalFilename())
                            .stream(inputStream, inputStream.available(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return "File uploaded successfully!";
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return "Error uploading file to MinIO: " + e.getMessage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static String uploadSonglistImgFile(MultipartFile file) {
        try {
            init();
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object("/songlist/img/"+file.getOriginalFilename())
                            .stream(inputStream, inputStream.available(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return "File uploaded successfully!";
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return "Error uploading file to MinIO: " + e.getMessage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static String uploadSongImgFile(MultipartFile file) {
        try {
            init();
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object("/song/img/"+file.getOriginalFilename())
                            .stream(inputStream, inputStream.available(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return "File uploaded successfully!";
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return "Error uploading file to MinIO: " + e.getMessage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String uploadAtorImgFile(MultipartFile file) {
        try {
            init();
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object("/consumer/img/"+file.getOriginalFilename())
                            .stream(inputStream, inputStream.available(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return "File uploaded successfully!";
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return "Error uploading file to MinIO: " + e.getMessage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String uploadPostImgFile(MultipartFile file, String fileName) {
        try {
            init();
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object("/post/img/" + fileName)
                            .stream(inputStream, inputStream.available(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return "File uploaded successfully!";
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return "Error uploading file to MinIO: " + e.getMessage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 登录页左侧封面图，固定 key：site/img/login-cover.&lt;ext&gt;
     *
     * @return 成功时返回前端可用的相对路径，如 user01/site/img/login-cover.jpg；失败返回 null
     */
    public static String uploadSiteLoginCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            init();
            String ext = resolveLoginCoverExt(file);
            String objectName = "site/img/login-cover." + ext;
            InputStream inputStream = file.getInputStream();
            long size = file.getSize() > 0 ? file.getSize() : inputStream.available();
            String ct = file.getContentType();
            if (ct == null || ct.isEmpty()) {
                ct = "image/jpeg";
            }
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(ct)
                            .build()
            );
            return "user01/site/img/login-cover." + ext;
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String resolveLoginCoverExt(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null) {
            String lower = ct.toLowerCase();
            if (lower.contains("png")) {
                return "png";
            }
            if (lower.contains("webp")) {
                return "webp";
            }
            if (lower.contains("gif")) {
                return "gif";
            }
            if (lower.contains("jpeg") || lower.contains("jpg")) {
                return "jpg";
            }
        }
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            String e = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
            if ("jpeg".equals(e)) {
                return "jpg";
            }
            if ("jpg".equals(e) || "png".equals(e) || "webp".equals(e) || "gif".equals(e)) {
                return e;
            }
        }
        return "jpg";
    }
}
