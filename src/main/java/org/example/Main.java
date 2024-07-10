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
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    public static Gson gson = new Gson();
    private static List<Holiday> holidays = new ArrayList<>();
    private static AtomicLong idCounter = new AtomicLong(0);

    public static void main(String[] args) throws IOException, IOException {
        holidays = loadHolidays();
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
     * @loadHolidays Loads holidays from a JSON file and returns them as an ArrayList.
     * @FileWriter if file not exist, create new file with empty Array
     * @FileReader reade JSON file, and return data to ArrayList
     * @return an ArrayList of Holiday objects
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

    public static long getNextId() {
        return idCounter.incrementAndGet();
    }
}