/*Poppy La
File name: WebCrawl.java
Instructor: Dr.Dimpsey
Date: 10/15/2021
Course: CSS436
Program 1: Web Crawler
   An application will download the html from the startingURL which is provided as the first argument to the program.  
   It will parse the html finding the first <a href >reference to other absolute URLs that is not previously visited.
*/

import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.regex.*;

public class WebCrawl {

   public static Set<String> visited = new HashSet<String>();
   public static Stack<URL> stack = new Stack<>();
   public static int currentHop = 1;

   public static void main(String[] args) throws IOException, URISyntaxException {

      // notice the invalid input argument
      if(args.length != 2) {
         System.out.println("Error: Invalid input!");
         System.exit(0);
      }

      try {
         URL url = new URL(args[0]);
         int statusCode = checkStatus(url);     // check for valid initial URL
         int hopNumber = Integer.parseInt(args[1]);

         printHeadline();
         System.out.println("Inital URL: " + url + "\t Number of hops: " + hopNumber + "\n");

         //check valid hop number
         if (hopNumber < 0) {
            throw new Exception();
         } else if (hopNumber == 0) {
            System.out.println("0 hop requested. Terminated.");
            System.exit(0);
         }
         
         // process the hop requested
         for (currentHop = 1; currentHop <= hopNumber; currentHop++) {
            // initial URL
            if(currentHop == 1) {
               // status 300 or 400
               if (statusCode >= 300 && statusCode < 400) {
                  System.out.println("Status:" + statusCode + "  Hop step:[" + currentHop + "] redirecting\tURL: " + url);
                  currentHop++;
                  url = redirect(url);    // redirect to another URL
               } else if (statusCode >= 400) {
                  throw new IOException();   // invalid inital URL
               }

            // check for next URL in current page
            } else {
               boolean recoil = true;
               // check if next URL reach end of page to recoil back
               while(recoil) {
                  URL nextURL = getNextURL(url);

                  // Access the next page of previous URL to continus if end page is reached
                  if (url.equals(nextURL)) {
                     recoil = true;
                     stack.pop();
                     url = stack.peek();
                     continue;
                  }
                  
                  url = nextURL;    // not end page, don't need to recoil
                  recoil = false; 
               }
            }

            visited.add(cleanedPage(url));      // add current URL to the list
            stack.push(url);                    // store current URL to stack in case recoil is needed later
            System.out.println("Status:" + statusCode + "  Hop step:[" + currentHop + "] \t\tURL: " + url);
         }

      } catch (IOException e) {
         System.out.println("Error: Cannot access website. Program is terminated.");
      } catch (URISyntaxException e) {
         System.out.println("Error: Only http or https allowed. Program is terminated.");
      } catch (Exception e) {
      System.out.println("Error: Negative number input in second argument. Program is terminated.");
      }
   }


   public static void printHeadline() {
      System.out.println("Name: Poppy La");
      System.out.println("Prof. Robert Dimpsey");
      System.out.println("Program 1");
      System.out.println("-----------------------------------------------------------------");
      System.out.println("                           WEB CRAWLER                           ");
      System.out.println("-----------------------------------------------------------------");
      System.out.println("Process the requested number of hops starting with inital URL provided.");
   }


   // check connection status when access URL
   public static int checkStatus(URL myURL) throws URISyntaxException, IOException {
      HttpURLConnection connection = (HttpURLConnection)myURL.openConnection();
      // terminate if the connection takes longer than 15 seconds
      connection.setReadTimeout(15000);

      int statusCode = connection.getResponseCode();
      return statusCode;
   }


   // redirect to other URL
   public static URL redirect(URL myURL) throws URISyntaxException, IOException {
      HttpURLConnection connection = (HttpURLConnection)myURL.openConnection();

      // the URL that need to be directed to is stored at "location"
      String redirectUrl = connection.getHeaderField("Location");
      myURL = new URL(redirectUrl);    // access the new link
      return myURL;
   }


   // remove all the http/https and www. part for URL comparation
   // http/https should be considered the same
   // With and without wwww. shouls be considered the same
   public static String cleanedPage(URL myURL) throws URISyntaxException, IOException {
      String stringURL = myURL.toString();

      // remove back trailing
      if (Character.compare(stringURL.toString().charAt(stringURL.length() - 1), '/') == 0)
         stringURL = stringURL.substring(0, stringURL.length() - 1);

      // remove http/https/www.
      String cleaned = stringURL.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","");
      return cleaned;
   }


   // find the next URL to access
   // it should be the first valid URL in the current page
   public static URL getNextURL(URL myURL) throws URISyntaxException, IOException {
      HttpURLConnection connection = (HttpURLConnection)myURL.openConnection();
      BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line;

      // regex pattern for all <a> tag with href attribute
      Pattern pTag = Pattern.compile("<a([^>]+)>(.+?)>", Pattern.DOTALL);
      Pattern pHref = Pattern.compile("href=\"(http[s]?://(.*?))\"", Pattern.DOTALL);

      // Read every line of source page, find valid http/https link
      while ((line = reader.readLine()) != null) {
         // find any html match <a> tag
         Matcher tagMatch = pTag.matcher(line); 

         while (tagMatch.find()) {
            String link = tagMatch.group(0);       //get the whole html line that has <a> tag
            Matcher hMatch = pHref.matcher(link);  // find any <a> tag html with href attribute

            while (hMatch.find()) {
               URL newUrl = new URL(hMatch.group(1));    //get http/https link in html
               int statusCode = checkStatus(newUrl);

               // status 300 or 400
               if (statusCode >= 300 && statusCode < 400) {
                  System.out.println("Status:" + statusCode + "  Hop step:[" + currentHop + "] redirecting \tURL: " + newUrl);
                  currentHop++;
                  newUrl = redirect(newUrl);    // redirect to another URL
               } else if (statusCode >= 400) {
                  System.out.println("Status:" + statusCode + "  Hop step:[" + currentHop + "] skipping \tURL: " + newUrl);
                  currentHop++;
                  continue;      // jump to next URL
               }

               // return new url if it's not visited
               if(!visited.contains(cleanedPage(newUrl))){
                  return newUrl;
               }
            }
         }
      }
      // can not find valid/unvisited URL
      return myURL;
   }
}