/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.support.integration.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {
  public static String readFileContents(String filePath) throws IOException {
    InputStream inputStream = new FileInputStream(filePath);
    System.out.println("Available bytes from the file :"+inputStream.available());
    byte[] bytes = new byte[inputStream.available()];

    StringBuffer sb = new StringBuffer();
    while (inputStream.read(bytes) != -1) {
      sb.append(new String(bytes, StandardCharsets.UTF_8));
    }

    return sb.toString();
  }

  public static byte[] readFileContentsInBytes(String filePath) throws IOException {

  	Path path = Paths.get(filePath);
    return Files.readAllBytes(path);

    /*
    InputStream inputStream = new FileInputStream(filePath);
    System.out.println("Available bytes from the file :"+inputStream.available());
    byte[] bytes = new byte[inputStream.available()];

    StringBuffer sb = new StringBuffer();
    while (inputStream.read(bytes) != -1) {
      
    }

    return sb.toString();
    */
  }
}