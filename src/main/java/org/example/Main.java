package org.example;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Main {

    /**
     * This method sets up an HTTP server and maps various endpoints to the HolidayHandler class.
     *
     * @param args command-line arguments (not used)
     * @throws IOException if an I/O error occurs while setting up the server
     */
    public static void main(String[] args) throws IOException {
        //    public static Gson gson = new Gson();
        List<Holiday> holidays = loadHolidays();
        HolidayHandler holidayHandler = new HolidayHandler(holidays);

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/createHoliday", holidayHandler);
        server.createContext("/getHoliday", holidayHandler);
        server.createContext("/getHolidays", holidayHandler);
        server.createContext("/updateHoliday", holidayHandler);
        server.createContext("/deleteHoliday", holidayHandler);
        server.createContext("/resetRatings", holidayHandler);
        server.createContext("/rateHoliday", holidayHandler);
        server.setExecutor(null);
        server.start();
    }


    /**
     Loads a list of holidays from a JSON file named "holidays.json".
     * If the file does not exist, it will be created with an empty list.
     *
     * @return an ArrayList of Holiday objects
     * Example:
     * List<Holiday> holidays = loadHolidays();
     * // holidays will be an empty list if the file is new, or a list of holidays if the file already exists
     */
    public static List<Holiday> loadHolidays() {
        File file = new File("holidays.json");
        Gson gson = new Gson();
        if (!file.exists()){
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("[]");
            } catch (Exception e) {
                System.out.println("Error creating new holidays file: " + e);
                return new ArrayList<>();
            }
        }
        try (FileReader reader = new FileReader(file)) {
            Holiday[] holidaysArray = gson.fromJson(reader, Holiday[].class);
            return new ArrayList<>(List.of(holidaysArray));
        } catch (Exception e) {
            System.out.println("Error loading holidays: " + e);
            return new ArrayList<>();
        }
    }

}