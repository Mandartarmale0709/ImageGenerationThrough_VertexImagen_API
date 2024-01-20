/* Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package com.example.demo;

import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;


import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import com.google.cloud.Timestamp;
import com.google.cloud.Date;
import org.springframework.core.ParameterizedTypeReference;
import java.net.URI;
import java.util.logging.Logger;


import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.RequestEntity;
import org.springframework.http.HttpMethod;

import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.*;
import com.example.demo.Yoga;
import java.io.InputStream;
import java.io.*;
import java.nio.charset.StandardCharsets;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse; 

import org.json.JSONObject;  
import org.json.JSONArray;  

import jakarta.xml.bind.DatatypeConverter;

@RestController
public class PromptController {

   private static final String get_pose_list = "https://yoga-pose-spanner-uxu5wi2jpa-uc.a.run.app/"; 
	
/*Uncomment below line and comment out above line if you are using Firebase Database instead of Spanner*/
  //  private static final String get_pose_list = "https://yoga-poses-test-default-rtdb.firebaseio.com/Yoga_Poses.json"; 
    
    RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = Logger.getLogger(PromptController.class.getName());


        /*
            Method that is invoked to create the prompt, to return the hello HTML page 
        */
        @GetMapping("/")
        public ModelAndView extractLabels(ModelMap map, Prompt prompt) {
            List<String> promptList = getList();
            map.addAttribute("poselist", promptList);
            return new ModelAndView("index", map);
        }

         /*
            Method that is invoked to add customization to the prompt if any and invoke imagen API
        */
        @GetMapping("/getimage")
        public ModelAndView getImage(Prompt prompt, ModelMap map) {
            //Pass PROMPT Request Parameter
            String promptRequest = 
            "{'instances': [ { 'prompt': '" + prompt.getPrompt() + "' } ],'parameters': { 'sampleCount': 1} }";
            logger.warning("PROMPT REQUEST! " + promptRequest);
          
            
            //Call Imagen API that returns the image in bytesBase64Encoded format
            String image = callImagen(promptRequest);
            
            //Call Cloud Functions that converts Base64Encoded bytes into image
            //callImageBuilder(image);

            //Return image to HTML

             map.addAttribute("imagestring", image);
        return new ModelAndView("getimage", map);
        }


    /*
        Method to invoke the imagen API that takes prompt string as a request and
        generates the corresponding image.
    */
    public File callImageBuilder(String base64){
        byte[] data = DatatypeConverter.parseBase64Binary(base64);
        logger.warning("Data! " + data);
            String path = "/home/abisukumaran/genai-posegen/src/main/resources/templates/" +"pose.jpg";
            logger.warning("Path! " + path);
            File file = new File(path);
            try(OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))){
                outputStream.write(data);
            }catch(Exception e){
                logger.warning("EXCEPTION! " + e);
            }
        return file;
    }

    public String callImagen(String promptRequest){
      String str = "{\"client_id\": \"764086051850-6qr4p6gpi6hn506pt8ejuq83di341hur.apps.googleusercontent.com\",\"client_secret\": \"d-FL95Q19q7MQmFpd7hHD0Ty\",\"quota_project_id\": \"crypto-arcade-411804\",\"refresh_token\": \"1//0gaUW1LU4BcXoCgYIARAAGBASNwF-L9Irp3odPgpm8-vqo78CsbsZ4zKJroL96vX2U6zRbOxpqmZpe7-jUvpk9AwCzZSj9ze6V8U\",\"type\": \"authorized_user\",\"universe_domain\": \"googleapis.com\"}";
       
    
        try{
            InputStream stream = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
            String token = credentials.refreshAccessToken().getTokenValue();
            String paramString = "https://us-central1-aiplatform.googleapis.com/v1/projects/crypto-arcade-411804/locations/us-central1/publishers/google/models/imagegeneration:predict";
            String jsonString = promptRequest;          
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer "+token);
            headers.set("Content-Type", "application/json; charset=utf-8");
            HttpEntity<String> entity = new HttpEntity<>(jsonString, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> resultString = restTemplate.exchange(paramString, HttpMethod.POST, entity, String.class);
            
           String result = resultString.toString();
            //process response
            JSONObject jsonObject = new JSONObject(result.substring(5));
            String base64 = jsonObject.getJSONArray("predictions").getJSONObject(0).getString("bytesBase64Encoded");
     return base64;
}catch(Exception e){
    System.out.println("EXCEPTION in edit" + e);
    return null;
}
    }
    /*
        Method to invoke API that retrieves a specific pose
        GetMapping("/getList")
    */
    
    public List<String> getList(){
        
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> resultString = restTemplate.exchange(get_pose_list, HttpMethod.GET, entity, String.class);
        String responseString = resultString.getBody().toString();
        responseString = responseString.replace("[", "").replace("]", "").replace("},", "};");        
        String[] split = responseString.split(";");
        List<String> list = Arrays.asList(split);
        List<String> poseList = new ArrayList<String>();
        
        try{
            for(String pose : list){
                String poseString = pose;
                poseString = poseString.replace("{", "").replace("}", "").replace("\"", "");
                String[] splitPoseString = poseString.split(",");
		    
		//Spanner specific snippet begins
		String name = splitPoseString[1].split(":")[1];
                String breath = splitPoseString[2].split(":")[1];
                String desc = splitPoseString[3].split(":")[1];
		//Spanner specific snippet ends
		    
		/* Uncomment below and comment the above 3 lines enclosed within Spanner specific snippet if you are using Firebase database instead of Spanner */
		/*
  		String name = splitPoseString[2].split(":")[1];
                String breath = splitPoseString[0].split(":")[1];
                String desc = splitPoseString[1].split(":")[1];
		*/
                poseList.add(name);
            }
        } catch(Exception e){
            poseList = null;
            logger.warning("Exception! " + e);
        }
        logger.warning("I am a warning log ! " + poseList.get(0));
		
      return poseList;
    }
    
}
