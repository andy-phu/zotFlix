//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


import java.util.*;

import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpSession;


//Checks the session and looks for the key = "previous" if true then there is a previous,
//if false then there isn't and create a previous based on the new end point and param
@WebServlet("/previousGetter")
public class PreviousGetter extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private boolean hashMapComparison(HashMap<String, HashMap<String, String>> prev, HashMap<String, HashMap<String, String>> curr){
        if (prev.size() != curr.size()) {
            return false;
        }

        for (Map.Entry<String, HashMap<String, String>> entry : prev.entrySet()) {
            String key = entry.getKey();
            HashMap<String, String> prevInnerMap = entry.getValue();
            HashMap<String, String> currInnerMap = curr.get(key);
            if (currInnerMap == null) {
                return false;
            }

            for (Map.Entry<String, String> innerEntry : prevInnerMap.entrySet()) {
                String innerKey = innerEntry.getKey();
                String prevValue = innerEntry.getValue();
                String currValue = currInnerMap.get(innerKey);
                if (!Objects.equals(prevValue, currValue)) {
                    return false;
                }
            }
        }
        return true;
    }


    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        boolean prevSessionFlag = false;
        boolean emptyEndpoint = false;
        //System.out.println("inside of the previous getter");

        BufferedReader reader = request.getReader();

        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        //System.out.println("http session: " + session);

        HashMap<String, HashMap<String, String>> prev = (HashMap<String, HashMap<String, String>>) session.getAttribute("prev");
        System.out.println("prev map in the beginning: " + prev);

        //empty endpoint means that we use the prev session's end point
        if (request.getParameter("endpoint").isEmpty()){
            System.out.println("endpoint is empty");
            emptyEndpoint = true;
        }

        //checks what's inside of the request body
        if (reader != null) {
            //System.out.println("Request Body:");
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.print(line);
            }
        } else {
            //if the body is empty than send the prev session as a response for the frontend to use
            prevSessionFlag = true;
            //System.out.println("Request body is null");
        }


        //get the new updated current session if the request body contains content or there is no prev
        if(!prevSessionFlag){
            System.out.println("USE THE CURRENT PARAMS");
            String page = request.getParameter("page");
            String pageSize = request.getParameter("pageSize");
            String sortRule = request.getParameter("sortRule");
            System.out.println("page " + page + " pageSize " + pageSize + " sort rule " + sortRule);

            ArrayList<String> searchArr = new ArrayList<>();
            searchArr.add("title");
            searchArr.add("director");
            searchArr.add("star");
            searchArr.add("year");



            //if end point is search, browse/genre, or browse/character
            String endpoint = request.getParameter("endpoint");

            if (emptyEndpoint){
                HashMap.Entry<String,HashMap<String, String>> entry = prev.entrySet().iterator().next();
                String key = endpoint = entry.getKey();
                if (key.equals("browsegenre")){
                    endpoint = "browse/genre";
                }else if (key.equals("browsecharacter")){
                    endpoint = "browse/character";
                }else{
                    endpoint = "search";
                }
                System.out.println("new endpoint retrieved from prev: " + endpoint);
            }
            HashMap <String, HashMap<String, String>> searchMap = new HashMap<String, HashMap<String, String>>();
            System.out.println("endpoint " + endpoint);


            if (endpoint.equals("search")) {
                System.out.println("SEARCH");
                //holds title director star and year not all of them will be included though
                HashMap<String, String> searchParams = new HashMap<>();
                for (String searchElem : searchArr){
                    String temp = request.getParameter(searchElem);
                    System.out.println("temp: " + temp);
                    //if it is a valid param, put it in the hashmap
                    if (temp != null && !temp.isEmpty()){
                        searchParams.put(searchElem, temp);
                    }else if (prev != null && !prev.isEmpty() && prev.get("search") != null){ //if temp is completely empty get the previous
                        System.out.println("goes to check prev for temp: " + prev);
                        searchParams.put(searchElem, prev.get("search").get(searchElem));
                    }
                }

                if(!emptyEndpoint){
                    searchParams.put("page", page);
                    searchParams.put("pageSize", pageSize);
                    searchParams.put("sortRule", sortRule);
                }else{
                    if (prev != null && !prev.isEmpty()){
                        System.out.println("using the prev params for sorting rules");
                        searchParams.put("page", prev.get("search").get("page"));
                        searchParams.put("pageSize", prev.get("search").get("pageSize"));
                        searchParams.put("sortRule", prev.get("search").get("sortRule"));
                    }
                }
                searchMap.put(endpoint, searchParams);
            }
            else if( endpoint.equals("browse/genre")){
                System.out.println("BROWSE/GENRE");
                HashMap<String, String> browseParams = new HashMap<>();


                String temp = request.getParameter("genre");

                //if it is a valid param, put it in the hashmap
                if (temp != null && !temp.isEmpty()){
                    browseParams.put("genre", temp);
                }else{
                    System.out.println("there is no genre param: "+ prev.get("browsegenre").get("genre"));
                    browseParams.put("genre", prev.get("browsegenre").get("genre"));
                }

                if(!emptyEndpoint){
                    browseParams.put("page", page);
                    browseParams.put("pageSize", pageSize);
                    browseParams.put("sortRule", sortRule);
                }else{
                    if (prev != null && !prev.isEmpty()){
                        System.out.println("using the prev params for sorting rules");
                        browseParams.put("page", prev.get("browsegenre").get("page"));
                        browseParams.put("pageSize", prev.get("browsegenre").get("pageSize"));
                        browseParams.put("sortRule", prev.get("browsegenre").get("sortRule"));
                    }
                }

                searchMap.put("browsegenre", browseParams);
            }
            else if( endpoint.equals("browse/character")){
                System.out.println("BROWSE/CHARACTER");
                HashMap<String, String> browseParams = new HashMap<>();


                String temp = request.getParameter("character");

                //if it is a valid param, put it in the hashmap
                if (temp != null && !temp.isEmpty()){
                    browseParams.put("character", temp);
                }else{
                    System.out.println("there is no char param: "+ prev.get("browsecharacter").get("character"));
                    browseParams.put("character", prev.get("browsecharacter").get("character"));
                }

                if(!emptyEndpoint){
                    browseParams.put("page", page);
                    browseParams.put("pageSize", pageSize);
                    browseParams.put("sortRule", sortRule);
                }else{
                    if (prev != null && !prev.isEmpty()){
                        System.out.println("using the prev params for sorting rules");
                        browseParams.put("page", prev.get("browsecharacter").get("page"));
                        browseParams.put("pageSize", prev.get("browsecharacter").get("pageSize"));
                        browseParams.put("sortRule", prev.get("browsecharacter").get("sortRule"));
                    }
                }

                searchMap.put("browsecharacter", browseParams);
            }


            try {

                //session null means that there is no previous search for movielist
                //which means to store the current one as previous and return status picked up by the frontend
                //which basically means to just continue with original get request
                if (session == null) {

                    out.print("{\"message\":\"Unauthorized access\"}");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                } else {
                    //check if there is already an attribute for prev
                    System.out.println("checking if there is already an attribute for prev ");

                    //if there is a prev but the body is not null then that means a new search is activated and update the prev
                    if (prev != null && !prev.isEmpty()) {

                        //if there is always a prev one than just use the current one
                        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                        String currJson = mapper.writeValueAsString(searchMap);
                        System.out.println("using the current params" + searchMap);
                        session.setAttribute("prev", searchMap);
                        System.out.println("current json : " + currJson);
                        out.print(currJson);
                        response.setStatus(HttpServletResponse.SC_CREATED);
                    } else {
                        //session exists but there is no prev from searching just return as if session == null w/o creating new sesh
                        //THIS IS THE DEFAULT BEHAVIOR WHERE THIS IS THE FIRST SEARCH AND NO PREV EXISTS
                        System.out.println("search map: " + searchMap);

                        session.setAttribute("prev", searchMap);
                        System.out.println("no previous movie list, using current endpt and params");
                        response.setStatus(HttpServletResponse.SC_CREATED);
                    }


                }
            }
            catch (Exception e) {

                    request.getServletContext().log("Error: ", e);

                    out.print(e.getMessage());
                    out.flush();
            }
        }else{
                //use previous if when you click on list the body is null because nothing was searched before
                //body is empty when you are adjusting the sorting and page number on the prev session
                //THIS IS THE ONLY CASE WHERE YOU WOULD USE THE PREVIOUS

                ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                String prevJson = mapper.writeValueAsString(prev);
                System.out.println("using the previous params" + prev);
                out.print(prevJson);
                response.setStatus(HttpServletResponse.SC_OK);
        }







            //flush out the buffer just in case
            out.flush();







    }



}
