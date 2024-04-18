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
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;

import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;



// This annotation maps this Java Servlet Class to a URL
@WebServlet("/login")
public class Login extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Create a dataSource which registered in web.
    private DataSource dataSource;


    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
            System.out.println("LOGIN");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StringBuilder requestBody = new StringBuilder();

        String email = "";
        String password = "";

        try(BufferedReader reader = request.getReader()){
            String line ;
            while ((line = reader.readLine()) != null){
                requestBody.append(line);
            }
        }catch(IOException e){
            System.out.println("reading error");
            e.printStackTrace();
        }

        String requestString = requestBody.toString();
        System.out.println("request string " + requestString);
        ObjectMapper objectMapper = new ObjectMapper();
        User user = objectMapper.readValue(requestString, User.class);

        System.out.println("email " +user.getEmail());
        System.out.println("password " +user.getPassword());


        // Get the PrintWriter for writing response
        PrintWriter out = response.getWriter();

        // Set response mime type
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        //response.addHeader("Access-Control-Allow-Origin", "http://localhost:5173");

        try {

            Connection connection = dataSource.getConnection();
            // prepare query
            String query = "SELECT c.id FROM customers c WHERE c.email = ? AND c.password = ?";
            // declare statement
            PreparedStatement preparedStatement = connection.prepareStatement(query);


            preparedStatement.setString(1,user.getEmail());
            preparedStatement.setString(2,user.getPassword());


            // execute query
            ResultSet resultSet = preparedStatement.executeQuery();


            Map<String,String> jsonResponse= new HashMap<>();

            //null means failure
            if (resultSet.next()){

                jsonResponse.put("status" , "success");
                jsonResponse.put("message" , "login successful");

                request.getSession().setAttribute("user", new Email(user.getEmail()));

                response.setStatus(HttpServletResponse.SC_OK);
            }else{

                //add email into cookie
                jsonResponse.put("status" , "fail");
                jsonResponse.put("message" , "login fail");

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }

            String jsonString = objectMapper.writeValueAsString(jsonResponse);
            out.print(jsonString);
            //flush out the buffer just in case
            out.flush();
            connection.close();



        } catch (Exception e) {

            request.getServletContext().log("Error: ", e);



            out.print(e.getMessage());
            out.flush();
        }



    }



}
